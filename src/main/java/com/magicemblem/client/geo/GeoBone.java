package com.magicemblem.client.geo;

import java.util.ArrayList;
import java.util.List;

/**
 * geo.json 骨骼节点
 * 
 * 每个骨骼包含：
 * - name：骨骼名称（用于动画和查找）
 * - parent：父骨骼名称（null 表示顶层骨骼）
 * - pivot：旋转中心点（模型坐标，像素）
 * - cubes：骨骼上的方块列表
 * - children：子骨骼列表（解析后构建）
 * 
 * 骨骼坐标系：
 * - X 轴：右（正方向）
 * - Y 轴：上（正方向）
 * - Z 轴：南（正方向）
 * - pivot 和 origin 均使用模型坐标（16像素 = 1格）
 */
public class GeoBone {

    /** 骨骼名称 */
    public String name = "";

    /** 父骨骼名称（null 表示顶层） */
    public String parent = null;

    /** 旋转中心点 [x, y, z]（模型坐标） */
    public float[] pivot = {0, 0, 0};

    /** 骨骼上的方块列表 */
    public List<GeoCube> cubes = new ArrayList<>();

    /** 子骨骼列表（解析后由 GeoModelParser 构建） */
    public List<GeoBone> children = new ArrayList<>();

    // ===== 动画变换（运行时由动画系统设置） =====

    /** 位移偏移 [x, y, z] */
    public float[] posOffset = {0, 0, 0};

    /** 旋转角度 [x, y, z]（度） */
    public float[] rotation = {0, 0, 0};

    /** 缩放 [x, y, z]（默认 1.0） */
    public float[] scale = {1, 1, 1};
}
