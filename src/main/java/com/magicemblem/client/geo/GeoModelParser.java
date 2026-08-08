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
 * geo.json 解析器（Bedrock 格式）
 *
 * 解析流程：
 * 1. 读取 geo.json 文件
 * 2. 解析 minecraft:geometry[0].description 获取 textureWidth/Height
 * 3. 解析 bones 列表，构建骨骼层级关系（parent -> children）
 * 4. 解析每个骨骼的 cubes（方块）和 UV 映射
 * 5. generateQuads() 预计算四边形（cube 旋转在渲染时应用）
 *
 * 模仿 BBS mod 的 GeoModelParser 设计
 */
public class GeoModelParser {

    /**
     * 从资源管理器加载并解析 geo.json
     *
     * @param manager  Minecraft 资源管理器
     * @param location geo.json 的资源路径
     * @return 解析后的 GeoModel，失败返回 null
     */
    public static GeoModel parse(ResourceManager manager, ResourceLocation location) {
        try {
            Optional<Resource> resource = manager.getResource(location);
            if (resource.isEmpty()) {
                MagicEmblem.LOGGER.error("geo.json not found: {}", location);
                return null;
            }
            try (InputStream is = resource.get().open();
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                return parseFromJson(root);
            }
        } catch (Exception e) {
            MagicEmblem.LOGGER.error("Failed to parse geo.json: {}", location, e);
            return null;
        }
    }

    /**
     * 从 JsonObject 解析 geo 模型
     */
    public static GeoModel parseFromJson(JsonObject root) {
        GeoModel model = new GeoModel();

        // 解析 minecraft:geometry 数组
        JsonArray geometryArray = root.getAsJsonArray("minecraft:geometry");
        if (geometryArray == null || geometryArray.size() == 0) return model;

        JsonObject geometry = geometryArray.get(0).getAsJsonObject();

        // 解析 description
        if (geometry.has("description")) {
            JsonObject desc = geometry.getAsJsonObject("description");
            if (desc.has("texture_width")) model.textureWidth = desc.get("texture_width").getAsInt();
            if (desc.has("texture_height")) model.textureHeight = desc.get("texture_height").getAsInt();
        }

        // 解析 bones
        if (geometry.has("bones")) {
            JsonArray bonesArray = geometry.getAsJsonArray("bones");
            Map<String, GeoBone> boneMap = new LinkedHashMap<>();

            for (JsonElement boneElement : bonesArray) {
                JsonObject boneJson = boneElement.getAsJsonObject();
                GeoBone bone = parseBone(boneJson, model.textureWidth, model.textureHeight);
                boneMap.put(bone.name, bone);
            }

            // 构建父子关系
            for (GeoBone bone : boneMap.values()) {
                if (bone.parent != null && boneMap.containsKey(bone.parent)) {
                    boneMap.get(bone.parent).children.add(bone);
                } else {
                    // 顶层骨骼
                    model.bones.add(bone);
                }
            }
        }

        return model;
    }

    /**
     * 解析单个骨骼
     * 
     * BBS 坐标系转换（Bedrock → Java）：
     * - pivot.x 取反
     * - rotation.x 和 rotation.y 取反
     */
    private static GeoBone parseBone(JsonObject json, int texW, int texH) {
        GeoBone bone = new GeoBone();
        bone.name = json.has("name") ? json.get("name").getAsString() : "";
        bone.parent = json.has("parent") ? json.get("parent").getAsString() : null;
        bone.pivot = parseFloatArray(json, "pivot", 3);

        // BBS 坐标系转换：X轴翻转（Bedrock 左手 → Java 右手）
        bone.pivot[0] *= -1;

        // 解析 cubes
        if (json.has("cubes")) {
            JsonArray cubesArray = json.getAsJsonArray("cubes");
            for (JsonElement cubeElement : cubesArray) {
                GeoCube cube = parseCube(cubeElement.getAsJsonObject(), texW, texH);
                if (cube != null) {
                    bone.cubes.add(cube);
                }
            }
        }

        return bone;
    }

    /**
     * 解析单个方块（cube）
     * 
     * BBS 坐标系转换（Bedrock → Java）：
     * - origin.x = -origin.x - size.x （翻转X + 偏移尺寸）
     * - pivot.x 取反
     * - rotation.x 和 rotation.y 取反
     */
    private static GeoCube parseCube(JsonObject json, int texW, int texH) {
        GeoCube cube = new GeoCube();
        cube.origin = parseFloatArray(json, "origin", 3);
        cube.size = parseFloatArray(json, "size", 3);

        // BBS 坐标系转换：X轴翻转
        cube.origin[0] *= -1;
        cube.origin[0] -= cube.size[0]; // 关键：减去尺寸X

        if (json.has("inflate")) {
            JsonElement inflateElement = json.get("inflate");
            if (inflateElement.isJsonPrimitive()) {
                cube.inflate = inflateElement.getAsFloat();
            }
        }

        if (json.has("pivot")) {
            cube.pivot = parseFloatArray(json, "pivot", 3);
            cube.pivot[0] *= -1; // BBS: pivot X 取反
        }
        if (json.has("rotation")) {
            cube.rotation = parseFloatArray(json, "rotation", 3);
            cube.rotation[0] *= -1; // BBS: rotation X 取反
            cube.rotation[1] *= -1; // BBS: rotation Y 取反
        }

        // 解析 UV 映射
        if (json.has("uv")) {
            JsonElement uvElement = json.get("uv");
            if (uvElement.isJsonObject()) {
                // 新格式：{ "north": {"uv": [u,v], "uv_size": [w,h]}, ... }
                JsonObject uvObj = uvElement.getAsJsonObject();
                for (String face : new String[]{"north", "south", "east", "west", "up", "down"}) {
                    if (uvObj.has(face)) {
                        JsonObject faceObj = uvObj.getAsJsonObject(face);
                        GeoCube.FaceUV faceUV = new GeoCube.FaceUV();
                        faceUV.uv = parseFloatArray(faceObj, "uv", 2);
                        faceUV.uvSize = parseFloatArray(faceObj, "uv_size", 2);

                        // BBS UV转换：up/down面的size取反，origin减去新size
                        // 参考 BBS GeoModelParser.parseUV()
                        if (face.equals("up") || face.equals("down")) {
                            faceUV.uvSize[0] *= -1;
                            faceUV.uvSize[1] *= -1;
                            faceUV.uv[0] -= faceUV.uvSize[0];
                            faceUV.uv[1] -= faceUV.uvSize[1];
                        }

                        cube.uv.put(face, faceUV);
                    }
                }
            } else if (uvElement.isJsonArray()) {
                // 旧格式（Box UV）：[u, v]（简单 UV 映射）
                // 简化处理：使用 box UV 映射
                float[] boxUV = parseFloatArray(json, "uv", 2);
                createBoxUV(cube, boxUV);
            }
        }

        // BBS: 解析完UV后立即生成预计算的四边形
        generateQuads(cube, texW, texH);

        return cube;
    }

    /**
     * 生成预计算的四边形（完全复制 BBS ModelCube.generateQuads()）
     *
     * 关键设计：
     * - 直接用 origin 和 origin+size 计算顶点坐标（已除以16转为方块单位）
     * - 每个面的UV角点映射完全匹配 BBS ModelUV.createQuad()
     * - cube 旋转在渲染时通过 PoseStack 应用（BBS CubicCubeRenderer.renderCube）
     */
    private static void generateQuads(GeoCube cube, int texW, int texH) {
        float tw = 1.0f / texW;
        float th = 1.0f / texH;

        float inflate = cube.inflate;
        // BBS 坐标计算方式：直接用 origin 和 origin+size
        float minX = (cube.origin[0] - inflate) / 16.0f;
        float minY = (cube.origin[1] - inflate) / 16.0f;
        float minZ = (cube.origin[2] - inflate) / 16.0f;
        float maxX = (cube.origin[0] + cube.size[0] + inflate) / 16.0f;
        float maxY = (cube.origin[1] + cube.size[1] + inflate) / 16.0f;
        float maxZ = (cube.origin[2] + cube.size[2] + inflate) / 16.0f;

        cube.quads.clear();

        // === front = north ===
        // BBS: vertex(maxX,minY,minZ,p4) vertex(minX,minY,minZ,p3)
        //      vertex(minX,maxY,minZ,p2) vertex(maxX,maxY,minZ,p1)
        if (cube.uv.containsKey("north")) {
            float[] c = getUV(cube.uv.get("north"), tw, th);
            cube.quads.add(makeQuad(
                maxX,minY,minZ, c[6],c[7],  // p4: (sx, ey)
                minX,minY,minZ, c[4],c[5],  // p3: (ex, ey)
                minX,maxY,minZ, c[2],c[3],  // p2: (ex, sy)
                maxX,maxY,minZ, c[0],c[1],  // p1: (sx, sy)
                0, 0, -1));
        }

        // === right = east ===
        // BBS: vertex(maxX,minY,maxZ,p4) vertex(maxX,minY,minZ,p3)
        //      vertex(maxX,maxY,minZ,p2) vertex(maxX,maxY,maxZ,p1)
        if (cube.uv.containsKey("east")) {
            float[] c = getUV(cube.uv.get("east"), tw, th);
            cube.quads.add(makeQuad(
                maxX,minY,maxZ, c[6],c[7],  // p4
                maxX,minY,minZ, c[4],c[5],  // p3
                maxX,maxY,minZ, c[2],c[3],  // p2
                maxX,maxY,maxZ, c[0],c[1],  // p1
                1, 0, 0));
        }

        // === back = south ===
        // BBS: vertex(minX,minY,maxZ,p4) vertex(maxX,minY,maxZ,p3)
        //      vertex(maxX,maxY,maxZ,p2) vertex(minX,maxY,maxZ,p1)
        if (cube.uv.containsKey("south")) {
            float[] c = getUV(cube.uv.get("south"), tw, th);
            cube.quads.add(makeQuad(
                minX,minY,maxZ, c[6],c[7],  // p4
                maxX,minY,maxZ, c[4],c[5],  // p3
                maxX,maxY,maxZ, c[2],c[3],  // p2
                minX,maxY,maxZ, c[0],c[1],  // p1
                0, 0, 1));
        }

        // === left = west ===
        // BBS: vertex(minX,minY,minZ,p4) vertex(minX,minY,maxZ,p3)
        //      vertex(minX,maxY,maxZ,p2) vertex(minX,maxY,minZ,p1)
        if (cube.uv.containsKey("west")) {
            float[] c = getUV(cube.uv.get("west"), tw, th);
            cube.quads.add(makeQuad(
                minX,minY,minZ, c[6],c[7],  // p4
                minX,minY,maxZ, c[4],c[5],  // p3
                minX,maxY,maxZ, c[2],c[3],  // p2
                minX,maxY,minZ, c[0],c[1],  // p1
                -1, 0, 0));
        }

        // === top = up ===
        // BBS: vertex(maxX,maxY,minZ,p2) vertex(minX,maxY,minZ,p1)
        //      vertex(minX,maxY,maxZ,p4) vertex(maxX,maxY,maxZ,p3)
        if (cube.uv.containsKey("up")) {
            float[] c = getUV(cube.uv.get("up"), tw, th);
            cube.quads.add(makeQuad(
                maxX,maxY,minZ, c[2],c[3],  // p2
                minX,maxY,minZ, c[0],c[1],  // p1
                minX,maxY,maxZ, c[6],c[7],  // p4
                maxX,maxY,maxZ, c[4],c[5],  // p3
                0, 1, 0));
        }

        // === bottom = down ===
        // BBS: vertex(minX,minY,minZ,p4) vertex(maxX,minY,minZ,p3)
        //      vertex(maxX,minY,maxZ,p2) vertex(minX,minY,maxZ,p1)
        if (cube.uv.containsKey("down")) {
            float[] c = getUV(cube.uv.get("down"), tw, th);
            cube.quads.add(makeQuad(
                minX,minY,minZ, c[6],c[7],  // p4
                maxX,minY,minZ, c[4],c[5],  // p3
                maxX,minY,maxZ, c[2],c[3],  // p2
                minX,minY,maxZ, c[0],c[1],  // p1
                0, -1, 0));
        }
    }

    /**
     * 计算 UV 四角坐标
     * @return [p1x, p1y, p2x, p2y, p3x, p3y, p4x, p4y]
     * 其中 p1=(sx,sy), p2=(ex,sy), p3=(ex,ey), p4=(sx,ey)
     */
    private static float[] getUV(GeoCube.FaceUV faceUV, float tw, float th) {
        float sx = faceUV.uv[0] * tw;
        float sy = faceUV.uv[1] * th;
        float ex = (faceUV.uv[0] + faceUV.uvSize[0]) * tw;
        float ey = (faceUV.uv[1] + faceUV.uvSize[1]) * th;
        return new float[]{sx, sy, ex, sy, ex, ey, sx, ey};
    }

    /**
     * 创建一个四边形
     */
    private static GeoCube.GeoQuad makeQuad(
            float x0, float y0, float z0, float u0, float v0,
            float x1, float y1, float z1, float u1, float v1,
            float x2, float y2, float z2, float u2, float v2,
            float x3, float y3, float z3, float u3, float v3,
            float nx, float ny, float nz) {
        GeoCube.GeoQuad q = new GeoCube.GeoQuad();
        q.vx[0] = x0; q.vy[0] = y0; q.vz[0] = z0; q.u[0] = u0; q.v[0] = v0;
        q.vx[1] = x1; q.vy[1] = y1; q.vz[1] = z1; q.u[1] = u1; q.v[1] = v1;
        q.vx[2] = x2; q.vy[2] = y2; q.vz[2] = z2; q.u[2] = u2; q.v[2] = v2;
        q.vx[3] = x3; q.vy[3] = y3; q.vz[3] = z3; q.u[3] = u3; q.v[3] = v3;
        q.nx = nx; q.ny = ny; q.nz = nz;
        return q;
    }

    /**
     * 创建 Box UV 映射（旧格式）
     * 
     * 完全复制 BBS ModelCube.setupBoxUV() 的布局：
     * 
     *  |  top  | front | right | back  | left  |
     *  |       |       |       |       |       |
     *  +-------+-------+-------+-------+-------+
     *  |       | bottom|       |       |       |
     * 
     * BBS 面名称映射：front=north, right=east, back=south, left=west
     */
    private static void createBoxUV(GeoCube cube, float[] boxUV) {
        float u = boxUV[0];
        float v = boxUV[1];
        float w = (float) Math.floor(Math.abs(cube.size[0]));
        float h = (float) Math.floor(Math.abs(cube.size[1]));
        float d = (float) Math.floor(Math.abs(cube.size[2]));

        // north/front: [u+d, v+d] -> [u+d+w, v+d+h]
        addFaceUV(cube, "north", u + d, v + d, w, h);
        // east/right: [u, v+d] -> [u+d, v+d+h]
        addFaceUV(cube, "east", u, v + d, d, h);
        // south/back: [u+d*2+w, v+d] -> [u+d*2+w+w, v+d+h]
        addFaceUV(cube, "south", u + d * 2 + w, v + d, w, h);
        // west/left: [u+d+w, v+d] -> [u+d+w+d, v+d+h]
        addFaceUV(cube, "west", u + d + w, v + d, d, h);
        // up/top: [u+d, v] -> [u+d+w, v+d] (UV flipped by BBS: swap corners)
        addFaceUV(cube, "up", u + d, v, w, d);
        // down/bottom: [u+d+w, v] -> [u+d+w+w, v+d] (UV flipped by BBS: swap corners)
        addFaceUV(cube, "down", u + d + w, v, w, -d);
    }

    private static void addFaceUV(GeoCube cube, String face, float u, float v, float w, float h) {
        GeoCube.FaceUV faceUV = new GeoCube.FaceUV();
        faceUV.uv = new float[]{u, v};
        faceUV.uvSize = new float[]{w, h};
        cube.uv.put(face, faceUV);
    }

    /**
     * 从 JsonObject 中读取 float 数组
     */
    private static float[] parseFloatArray(JsonObject json, String key, int size) {
        if (!json.has(key)) return new float[size];
        JsonArray arr = json.getAsJsonArray(key);
        float[] result = new float[size];
        for (int i = 0; i < Math.min(arr.size(), size); i++) {
            JsonElement e = arr.get(i);
            if (e.isJsonPrimitive()) {
                try {
                    result[i] = e.getAsFloat();
                } catch (Exception ex) {
                    result[i] = 0;
                }
            }
        }
        return result;
    }
}
