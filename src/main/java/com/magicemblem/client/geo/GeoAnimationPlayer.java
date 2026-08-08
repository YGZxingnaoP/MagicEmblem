package com.magicemblem.client.geo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.magicemblem.MagicEmblem;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 简单动画系统（替代 GeckoLib）
 * 
 * 解析 Bedrock animation.json 格式，支持：
 * - 骨骼 rotation/position/scale 关键帧插值
 * - 循环播放（loop: true）
 * - 数学表达式求值（简化版，支持 math.sin/math.cos + query.anim_time）
 * 
 * 动画 JSON 格式：
 * {
 *   "format_version": "1.8.0",
 *   "animations": {
 *     "idle": {
 *       "loop": true,
 *       "animation_length": 3,
 *       "bones": {
 *         "emblem": {
 *           "rotation": { "0.0": {"vector": [0,0,0]}, "3.0": {"vector": [0,360,0]} }
 *         }
 *       }
 *     }
 *   }
 * }
 */
public class GeoAnimationPlayer {

    /** 已解析的动画库（名称 -> 动画数据） */
    private Map<String, GeoAnimation> animations = new HashMap<>();

    /** 当前播放的动画名称 */
    private String currentAnim = null;

    /** 当前动画已播放的时间（秒） */
    private float currentTime = 0;

    /** 是否循环播放 */
    private boolean looping = true;

    // ===== 动画数据结构 =====

    public static class GeoAnimation {
        public boolean loop = false;
        public float length = 1.0f;
        public Map<String, BoneAnim> bones = new HashMap<>();
    }

    public static class BoneAnim {
        public List<KeyFrame> rotation = new ArrayList<>();
        public List<KeyFrame> position = new ArrayList<>();
        public List<KeyFrame> scale = new ArrayList<>();
    }

    public static class KeyFrame {
        public float time;
        public String[] values; // 存储原始值（支持表达式）
    }

    // ===== 加载动画 =====

    /**
     * 从资源管理器加载动画文件
     */
    public void load(ResourceManager manager, ResourceLocation location) {
        try {
            Optional<Resource> resource = manager.getResource(location);
            if (resource.isEmpty()) {
                MagicEmblem.LOGGER.warn("Animation file not found: {}", location);
                return;
            }
            try (InputStream is = resource.get().open();
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                parseAnimations(root);
            }
        } catch (Exception e) {
            MagicEmblem.LOGGER.error("Failed to load animation: {}", location, e);
        }
    }

    private void parseAnimations(JsonObject root) {
        if (!root.has("animations")) return;
        JsonObject animsObj = root.getAsJsonObject("animations");
        for (String name : animsObj.keySet()) {
            JsonObject animObj = animsObj.getAsJsonObject(name);
            GeoAnimation anim = new GeoAnimation();
            if (animObj.has("loop")) {
                JsonElement loopElem = animObj.get("loop");
                anim.loop = loopElem.isJsonPrimitive() && (loopElem.getAsBoolean() || "true".equals(loopElem.getAsString()));
            }
            if (animObj.has("animation_length")) {
                anim.length = animObj.get("animation_length").getAsFloat();
            }
            if (animObj.has("bones")) {
                JsonObject bonesObj = animObj.getAsJsonObject("bones");
                for (String boneName : bonesObj.keySet()) {
                    BoneAnim boneAnim = new BoneAnim();
                    JsonObject boneObj = bonesObj.getAsJsonObject(boneName);
                    boneAnim.rotation = parseKeyFrames(boneObj, "rotation");
                    boneAnim.position = parseKeyFrames(boneObj, "position");
                    boneAnim.scale = parseKeyFrames(boneObj, "scale");
                    anim.bones.put(boneName, boneAnim);
                }
            }
            animations.put(name, anim);
        }
    }

    private List<KeyFrame> parseKeyFrames(JsonObject boneObj, String channel) {
        List<KeyFrame> frames = new ArrayList<>();
        if (!boneObj.has(channel)) return frames;

        JsonElement channelElem = boneObj.get(channel);
        if (channelElem.isJsonArray()) {
            // 简单格式：直接向量数组
            // 不常用，忽略
        } else if (channelElem.isJsonObject()) {
            // 标准格式：时间 -> 关键帧
            JsonObject framesObj = channelElem.getAsJsonObject();
            for (String timeStr : framesObj.keySet()) {
                KeyFrame frame = new KeyFrame();
                try {
                    frame.time = Float.parseFloat(timeStr);
                } catch (NumberFormatException e) {
                    frame.time = 0;
                }
                JsonElement frameElem = framesObj.get(timeStr);
                if (frameElem.isJsonObject()) {
                    JsonObject frameObj = frameElem.getAsJsonObject();
                    if (frameObj.has("vector")) {
                        JsonArray vec = frameObj.getAsJsonArray("vector");
                        frame.values = new String[vec.size()];
                        for (int i = 0; i < vec.size(); i++) {
                            frame.values[i] = vec.get(i).getAsString();
                        }
                    }
                } else if (frameElem.isJsonArray()) {
                    JsonArray arr = frameElem.getAsJsonArray();
                    frame.values = new String[arr.size()];
                    for (int i = 0; i < arr.size(); i++) {
                        frame.values[i] = arr.get(i).getAsString();
                    }
                }
                frames.add(frame);
            }
        }
        // 按时间排序
        frames.sort(Comparator.comparingDouble(f -> f.time));
        return frames;
    }

    // ===== 播放控制 =====

    public void play(String animName, boolean loop) {
        this.currentAnim = animName;
        this.currentTime = 0;
        this.looping = loop;
    }

    /**
     * 推进动画时间
     * @param deltaSeconds 时间增量（秒）
     */
    public void tick(float deltaSeconds) {
        if (currentAnim == null) return;
        GeoAnimation anim = animations.get(currentAnim);
        if (anim == null) return;

        currentTime += deltaSeconds;
        if (looping) {
            if (anim.length > 0) {
                currentTime = currentTime % anim.length;
            }
        } else {
            if (currentTime > anim.length) {
                currentTime = anim.length;
            }
        }
    }

    /**
     * 将当前动画状态应用到模型的骨骼上
     */
    public void apply(GeoModel model) {
        if (currentAnim == null || model == null) return;
        GeoAnimation anim = animations.get(currentAnim);
        if (anim == null) return;

        // 重置所有骨骼的动画变换
        resetBones(model.bones);

        // 应用动画到匹配的骨骼
        for (Map.Entry<String, BoneAnim> entry : anim.bones.entrySet()) {
            String boneName = entry.getKey();
            BoneAnim boneAnim = entry.getValue();
            GeoBone bone = findBone(model.bones, boneName);
            if (bone == null) continue;

            bone.rotation = interpolateKeyFrames(boneAnim.rotation, currentTime, new float[]{0, 0, 0});
            bone.posOffset = interpolateKeyFrames(boneAnim.position, currentTime, new float[]{0, 0, 0});
            bone.scale = interpolateKeyFrames(boneAnim.scale, currentTime, new float[]{1, 1, 1});

            // BBS 坐标系转换：动画旋转 X/Y 取反，位移 X 取反
            bone.rotation[0] *= -1; // rotation X
            bone.rotation[1] *= -1; // rotation Y
            bone.posOffset[0] *= -1; // position X
        }
    }

    private void resetBones(List<GeoBone> bones) {
        for (GeoBone bone : bones) {
            bone.posOffset = new float[]{0, 0, 0};
            bone.rotation = new float[]{0, 0, 0};
            bone.scale = new float[]{1, 1, 1};
            resetBones(bone.children);
        }
    }

    private GeoBone findBone(List<GeoBone> bones, String name) {
        for (GeoBone bone : bones) {
            if (name.equals(bone.name)) return bone;
            GeoBone found = findBone(bone.children, name);
            if (found != null) return found;
        }
        return null;
    }

    /**
     * 关键帧插值
     */
    private float[] interpolateKeyFrames(List<KeyFrame> frames, float time, float[] defaultVal) {
        if (frames.isEmpty()) return defaultVal.clone();
        if (frames.size() == 1) {
            return evaluateValues(frames.get(0).values, time, defaultVal);
        }

        // 找到前后两个关键帧
        KeyFrame prev = frames.get(0);
        KeyFrame next = frames.get(frames.size() - 1);

        for (int i = 0; i < frames.size() - 1; i++) {
            if (time >= frames.get(i).time && time <= frames.get(i + 1).time) {
                prev = frames.get(i);
                next = frames.get(i + 1);
                break;
            }
        }

        if (prev == next || prev.time == next.time) {
            return evaluateValues(prev.values, time, defaultVal);
        }

        float t = (time - prev.time) / (next.time - prev.time);
        t = Math.max(0, Math.min(1, t));

        float[] v0 = evaluateValues(prev.values, time, defaultVal);
        float[] v1 = evaluateValues(next.values, time, defaultVal);

        float[] result = new float[Math.min(v0.length, v1.length)];
        for (int i = 0; i < result.length; i++) {
            result[i] = v0[i] + (v1[i] - v0[i]) * t;
        }
        return result;
    }

    /**
     * 求值关键帧的值数组（支持数学表达式）
     */
    private float[] evaluateValues(String[] values, float time, float[] defaultVal) {
        if (values == null) return defaultVal.clone();
        float[] result = new float[Math.min(values.length, 3)];
        for (int i = 0; i < result.length; i++) {
            result[i] = evaluateExpression(values[i], time);
        }
        // 填充缺失的分量
        for (int i = result.length; i < defaultVal.length; i++) {
            result = Arrays.copyOf(result, defaultVal.length);
            result[i] = defaultVal[i];
        }
        return result;
    }

    /**
     * 简化的数学表达式求值器
     * 支持：数字、math.sin()、math.cos()、query.anim_time
     */
    private float evaluateExpression(String expr, float time) {
        if (expr == null || expr.isEmpty()) return 0;
        try {
            // 替换 query.anim_time
            String processed = expr.replace("query.anim_time", String.valueOf(time));
            // 替换 math.sin / math.cos
            processed = processed.replace("math.sin", "Math.sin");
            processed = processed.replace("math.cos", "Math.cos");
            processed = processed.replace("math.pi", "Math.PI");
            // 简单的数值求值（处理 Math.sin/cos 和算术）
            return (float) evalSimple(processed);
        } catch (Exception e) {
            try {
                return Float.parseFloat(expr);
            } catch (NumberFormatException e2) {
                return 0;
            }
        }
    }

    /**
     * 简单表达式求值（支持 Math.sin/cos 和基本算术）
     */
    private static double evalSimple(String expr) {
        expr = expr.trim();
        // Math.sin(...)
        if (expr.startsWith("Math.sin(")) {
            String inner = expr.substring(9, expr.length() - 1);
            return Math.sin(Math.toRadians(evalSimple(inner)));
        }
        // Math.cos(...)
        if (expr.startsWith("Math.cos(")) {
            String inner = expr.substring(9, expr.length() - 1);
            return Math.cos(Math.toRadians(evalSimple(inner)));
        }
        // 括号
        if (expr.startsWith("(") && expr.endsWith(")")) {
            return evalSimple(expr.substring(1, expr.length() - 1));
        }
        // 加减法（最后优先级）
        int lastPlusMinus = -1;
        int parenDepth = 0;
        for (int i = expr.length() - 1; i >= 0; i--) {
            char c = expr.charAt(i);
            if (c == ')') parenDepth++;
            if (c == '(') parenDepth--;
            if (parenDepth == 0 && (c == '+' || c == '-') && i > 0) {
                lastPlusMinus = i;
                break;
            }
        }
        if (lastPlusMinus > 0) {
            double left = evalSimple(expr.substring(0, lastPlusMinus));
            double right = evalSimple(expr.substring(lastPlusMinus + 1));
            return expr.charAt(lastPlusMinus) == '+' ? left + right : left - right;
        }
        // 乘除法
        int lastMulDiv = -1;
        parenDepth = 0;
        for (int i = expr.length() - 1; i >= 0; i--) {
            char c = expr.charAt(i);
            if (c == ')') parenDepth++;
            if (c == '(') parenDepth--;
            if (parenDepth == 0 && (c == '*' || c == '/')) {
                lastMulDiv = i;
                break;
            }
        }
        if (lastMulDiv > 0) {
            double left = evalSimple(expr.substring(0, lastMulDiv));
            double right = evalSimple(expr.substring(lastMulDiv + 1));
            return expr.charAt(lastMulDiv) == '*' ? left * right : left / right;
        }
        // 数字
        return Double.parseDouble(expr);
    }

    public String getCurrentAnim() {
        return currentAnim;
    }

    public float getCurrentTime() {
        return currentTime;
    }
}
