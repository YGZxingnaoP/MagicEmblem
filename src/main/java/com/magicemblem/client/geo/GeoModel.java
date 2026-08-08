package com.magicemblem.client.geo;

import java.util.ArrayList;
import java.util.List;

/**
 * geo.json 模型数据（Bedrock 格式）
 * 
 * 对应 geo.json 的 minecraft:geometry[0]，包含：
 * - textureWidth / textureHeight：UV 贴图的像素尺寸
 * - bones：骨骼列表，每个骨骼包含 cubes（方块）和子骨骼
 * 
 * 模仿 BBS mod 的 cubic model 系统设计，
 * 不使用 GeckoLib，完全自研 geo.json 解析和渲染。
 */
public class GeoModel {

    /** UV 贴图宽度（像素） */
    public int textureWidth = 64;

    /** UV 贴图高度（像素） */
    public int textureHeight = 64;

    /** 骨骼列表（顶层骨骼，子骨骼通过 parent 关系构建） */
    public List<GeoBone> bones = new ArrayList<>();
}
