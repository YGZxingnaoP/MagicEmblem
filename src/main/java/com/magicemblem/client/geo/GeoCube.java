package com.magicemblem.client.geo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * geo.json 方块（cube）
 *
 * 完全模仿 BBS mod 的 ModelCube 设计：
 * - origin / size：方块坐标和尺寸（经过 Bedrock→Java 转换）
 * - inflate：膨胀量（描边/外壳效果）
 * - pivot / rotation：方块旋转（可选，渲染时应用）
 * - uv：各面 UV 映射（Map: face方向 -> FaceUV）
 * - quads：预计算的四边形列表（解析时由 generateQuads() 生成）
 *
 * 渲染时 cube 旋转通过 PoseStack 应用（BBS CubicCubeRenderer.renderCube）
 */
public class GeoCube {

    /** 方块起始点 [x, y, z]（模型坐标，已转换为Java坐标系） */
    public float[] origin = {0, 0, 0};

    /** 方块尺寸 [x, y, z]（可含负数，已转换为Java坐标系） */
    public float[] size = {1, 1, 1};

    /** 膨胀量（向外扩展，用于描边效果） */
    public float inflate = 0;

    /** 方块旋转中心 [x, y, z]（可选，null 表示无旋转） */
    public float[] pivot = null;

    /** 方块旋转角度 [x, y, z]（度，可选） */
    public float[] rotation = null;

    /**
     * UV 映射（face方向 -> UV数据）
     * face 方向: "north", "south", "east", "west", "up", "down"
     */
    public Map<String, FaceUV> uv = new HashMap<>();

    /**
     * 预计算的四边形列表
     * 在解析时由 generateQuads() 生成，渲染时直接使用
     * 完全匹配 BBS ModelCube.generateQuads() 的顶点顺序和UV映射
     */
    public List<GeoQuad> quads = new ArrayList<>();

    // ===== 坐标辅助方法 =====

    public float getMinX() { return size[0] < 0 ? origin[0] + size[0] : origin[0]; }
    public float getMinY() { return size[1] < 0 ? origin[1] + size[1] : origin[1]; }
    public float getMinZ() { return size[2] < 0 ? origin[2] + size[2] : origin[2]; }

    public float getMaxX() { return size[0] < 0 ? origin[0] : origin[0] + size[0]; }
    public float getMaxY() { return size[1] < 0 ? origin[1] : origin[1] + size[1]; }
    public float getMaxZ() { return size[2] < 0 ? origin[2] : origin[2] + size[2]; }

    /**
     * 面的 UV 数据
     */
    public static class FaceUV {
        /** UV 起始坐标 [u, v]（像素） */
        public float[] uv = {0, 0};
        /** UV 尺寸 [width, height]（像素，可为负） */
        public float[] uvSize = {1, 1};
    }

    /**
     * 预计算的四边形（4个顶点 + 法线）
     * 
     * 完全模仿 BBS ModelQuad 设计：
     * - 每个顶点包含 3D 坐标（方块单位，已除以16）和 UV 坐标（归一化）
     * - 法线方向为单位向量
     * 
     * 顶点顺序和UV角点映射（BBS createQuad 约定）：
     * - p1 = (sx, sy)   UV左上
     * - p2 = (ex, sy)   UV右上
     * - p3 = (ex, ey)   UV右下
     * - p4 = (sx, ey)   UV左下
     */
    public static class GeoQuad {
        public float[] vx = new float[4];
        public float[] vy = new float[4];
        public float[] vz = new float[4];
        public float[] u = new float[4];
        public float[] v = new float[4];
        public float nx, ny, nz;
    }
}
