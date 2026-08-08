package com.magicemblem.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * 模组自定义渲染类型
 *
 * 提供 GlowingPart 骨骼专用的发光渲染类型：
 * - 使用 rendertype_entity_translucent_emissive 着色器（自发光，不受光照影响）
 * - 标准面剔除（CullStateShard(true)）：只渲染外面
 * - 使用实际贴图纹理
 */
public class ModRenderTypes extends RenderStateShard {

    /** 缓存已创建的 glow RenderType（按贴图路径） */
    private static final Map<ResourceLocation, RenderType> GLOW_CACHE = new HashMap<>();

    /**
     * 获取指定贴图的发光渲染类型
     *
     * GlowingPart 骨骼使用此类型：
     * - emissive 着色器：自发光
     * - 标准面剔除：只渲染外面（和 BBS 相同）
     * - 使用实际纹理贴图
     */
    public static RenderType glow(ResourceLocation texture) {
        return GLOW_CACHE.computeIfAbsent(texture, tex ->
                RenderType.create(
                        "magicemblem_glow_" + tex.getPath(),
                        DefaultVertexFormat.NEW_ENTITY,
                        VertexFormat.Mode.QUADS,
                        256,
                        false,
                        false,
                        RenderType.CompositeState.builder()
                                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                                .setTextureState(new TextureStateShard(tex, false, false))
                                .setCullState(new CullStateShard(true))
                                .setLightmapState(LIGHTMAP)
                                .setOverlayState(OVERLAY)
                                .createCompositeState(false)));
    }

    private ModRenderTypes() {
        super("", () -> {}, () -> {});
    }
}
