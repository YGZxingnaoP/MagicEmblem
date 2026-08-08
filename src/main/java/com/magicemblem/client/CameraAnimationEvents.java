package com.magicemblem.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.magicemblem.MagicEmblem;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 相机动画客户端事件处理器
 * 
 * 处理（无需 Mixin，使用 Forge 事件）：
 * - 相机位置覆盖（通过 Method 反射调用 Camera.setPosition(Vec3)）
 * - 相机朝向覆盖（通过 event.setYaw/setPitch）
 * - FOV覆盖（动画期间强制FOV=40）
 * - 黑场淡出渲染
 * 
 * MC 1.20.1 SRG 环境说明：
 * - Camera.setPosition(Vec3) 的 SRG 名是 m_90581_（protected 方法）
 * - Camera.position 字段的 SRG 名是 f_90552_（private Vec3）
 * - 使用三级 fallback：SRG方法 → 映射名方法 → Field反射
 */
@Mod.EventBusSubscriber(modid = MagicEmblem.MODID, value = Dist.CLIENT)
public class CameraAnimationEvents {

    // ===== 相机位置反射缓存 =====
    private static Method setPositionMethod = null;
    private static Field positionField = null;
    private static boolean resolved = false;

    /**
     * 覆盖相机角度和位置（动画期间接管相机控制）
     * 每帧调用一次（camera.setup 之后）
     */
    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!CameraAnimationManager.isActive()) return;

        // 每帧更新时间（基于 System.currentTimeMillis，不受帧率影响）
        CameraAnimationManager.update();

        Vec3 camPos = CameraAnimationManager.getCameraPosition();
        Vec3 lookTarget = CameraAnimationManager.getLookTarget();

        if (camPos != null && lookTarget != null) {
            Camera camera = event.getCamera();

            // 通过反射设置相机位置
            setCameraPosition(camera, camPos.x, camPos.y, camPos.z);

            // 计算朝向角度（相机始终看向方块中心）
            double dx = lookTarget.x - camPos.x;
            double dy = lookTarget.y - camPos.y;
            double dz = lookTarget.z - camPos.z;
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);

            // Minecraft 标准朝向公式
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            // pitch：抬头为负，低头为正
            float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontalDist));

            // 通过事件设置旋转角度
            event.setYaw(yaw);
            event.setPitch(pitch);
            event.setRoll(0);
        }
    }

    /**
     * 覆盖视口FOV（动画期间强制FOV=40）
     */
    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (CameraAnimationManager.isActive()) {
            event.setFOV(CameraAnimationManager.getCurrentFov());
        }
    }

    /**
     * 渲染黑场淡出叠加层
     */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        float alpha = CameraAnimationManager.getFadeAlpha();
        if (alpha > 0.0f) {
            Minecraft mc = Minecraft.getInstance();
            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();
            int color = ((int) (alpha * 255) << 24); // 黑色，alpha渐变
            event.getGuiGraphics().fill(0, 0, screenWidth, screenHeight, color);
        }
    }

    /**
     * 世界卸载时重置动画状态（防止静态状态跨世界残留）
     */
    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            CameraAnimationManager.reset();
        }
    }

    // ===== 反射工具方法 =====

    /**
     * 通过反射设置 Camera 位置
     * 
     * 三级 fallback 策略（MC 1.20.1 SRG 环境）：
     * 1. Method "m_90581_" (SRG名) - protected void setPosition(Vec3)
     * 2. Method "setPosition" (映射名) - 开发环境可能使用
     * 3. Field "f_90552_" (SRG名) 或遍历 Vec3 字段
     */
    private static void setCameraPosition(Camera camera, double x, double y, double z) {
        Vec3 pos = new Vec3(x, y, z);

        // 尝试 1：使用 Method 反射调用 setPosition(Vec3)
        if (setPositionMethod != null) {
            try {
                setPositionMethod.invoke(camera, pos);
                return;
            } catch (Exception e) {
                MagicEmblem.LOGGER.error("Failed to invoke camera setPosition method", e);
                return;
            }
        }

        // 尝试 2：使用 Field 反射设置 position 字段
        if (positionField != null) {
            try {
                positionField.set(camera, pos);
                return;
            } catch (Exception e) {
                MagicEmblem.LOGGER.error("Failed to set camera position field", e);
                return;
            }
        }

        // 首次调用：解析反射目标
        if (!resolved) {
            resolved = true;
            resolveCameraPosition(camera, pos);
        }
    }

    /**
     * 解析 Camera.setPosition 的反射目标（仅首次调用）
     */
    private static void resolveCameraPosition(Camera camera, Vec3 pos) {
        // 策略1：尝试 SRG 方法名 m_90581_
        try {
            Method m = Camera.class.getDeclaredMethod("m_90581_", Vec3.class);
            m.setAccessible(true);
            setPositionMethod = m;
            m.invoke(camera, pos);
            MagicEmblem.LOGGER.info("[CameraAnim] Using SRG method m_90581_ for camera position");
            return;
        } catch (Exception ignored) {}

        // 策略2：尝试映射方法名 setPosition
        try {
            Method m = Camera.class.getDeclaredMethod("setPosition", Vec3.class);
            m.setAccessible(true);
            setPositionMethod = m;
            m.invoke(camera, pos);
            MagicEmblem.LOGGER.info("[CameraAnim] Using mapped method setPosition for camera position");
            return;
        } catch (Exception ignored) {}

        // 策略3：遍历所有方法，找 protected void xxx(Vec3) 的 setPosition
        try {
            for (Method m : Camera.class.getDeclaredMethods()) {
                Class<?>[] params = m.getParameterTypes();
                if (params.length == 1 && params[0] == Vec3.class
                        && m.getReturnType() == void.class) {
                    m.setAccessible(true);
                    setPositionMethod = m;
                    m.invoke(camera, pos);
                    MagicEmblem.LOGGER.info("[CameraAnim] Found setPosition-like method: {} for camera position", m.getName());
                    return;
                }
            }
        } catch (Exception ignored) {}

        // 策略4：Field 反射 - SRG 字段名 f_90552_
        try {
            Field f = Camera.class.getDeclaredField("f_90552_");
            f.setAccessible(true);
            positionField = f;
            f.set(camera, pos);
            MagicEmblem.LOGGER.info("[CameraAnim] Using SRG field f_90552_ for camera position");
            return;
        } catch (Exception ignored) {}

        // 策略5：Field 反射 - 映射字段名 position
        try {
            Field f = Camera.class.getDeclaredField("position");
            f.setAccessible(true);
            positionField = f;
            f.set(camera, pos);
            MagicEmblem.LOGGER.info("[CameraAnim] Using mapped field position for camera position");
            return;
        } catch (Exception ignored) {}

        // 策略6：遍历所有 Vec3 类型的字段
        try {
            for (Field f : Camera.class.getDeclaredFields()) {
                if (f.getType() == Vec3.class) {
                    f.setAccessible(true);
                    positionField = f;
                    f.set(camera, pos);
                    MagicEmblem.LOGGER.info("[CameraAnim] Found Vec3 field: {} for camera position", f.getName());
                    return;
                }
            }
        } catch (Exception ignored) {}

        MagicEmblem.LOGGER.error("[CameraAnim] FAILED to find any way to set camera position! " +
                "Available methods: {}, Available fields: {}",
                java.util.Arrays.toString(Camera.class.getDeclaredMethods()),
                java.util.Arrays.toString(Camera.class.getDeclaredFields()));
    }
}
