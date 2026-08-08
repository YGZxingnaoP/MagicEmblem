package com.magicemblem.client.renderer;

import com.magicemblem.client.ModRenderTypes;
import com.magicemblem.client.geo.GeoModel;
import com.magicemblem.client.geo.GeoModelRenderer;
import com.magicemblem.common.block.AbstractEmblemBlock;
import com.magicemblem.common.blockentity.AbstractEmblemBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 通用校徽方块实体渲染器
 *
 * 适用于所有继承 {@link AbstractEmblemBlockEntity} 的方块实体。
 * 功能：
 * - 从方块实体获取模型和贴图（多态）
 * - 根据方块朝向（HORIZONTAL_FACING）绕Y轴旋转模型
 * - GlowingPart 骨骼使用 emissive + 全亮度渲染
 *
 * 【扩展指南】新增校徽方块时直接复用此渲染器，无需创建新渲染器。
 * 在 MagicEmblem.ClientModEvents 中注册：
 *   BlockEntityRenderers.register(XXX_BE.get(), EmblemBlockRenderer::new);
 */
public class EmblemBlockRenderer implements BlockEntityRenderer<AbstractEmblemBlockEntity> {

    public EmblemBlockRenderer(BlockEntityRendererProvider.Context context) {
        // 标准 BlockEntityRenderer 构造函数
    }

    @Override
    public void render(AbstractEmblemBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        GeoModel model = blockEntity.getModel();
        if (model == null) return;

        // 从方块实体获取贴图路径（多态）
        ResourceLocation texture = blockEntity.getTextureLocation();

        // 普通骨骼：entityCutout
        RenderType normalType = RenderType.entityCutout(texture);
        // GlowingPart 骨骼：emissive + 全亮度
        RenderType glowType = ModRenderTypes.glow(texture);

        poseStack.pushPose();

        // 根据方块朝向旋转模型（默认朝北，FACING=NORTH 时不旋转）
        BlockState state = blockEntity.getBlockState();
        if (state.hasProperty(AbstractEmblemBlock.FACING)) {
            Direction facing = state.getValue(AbstractEmblemBlock.FACING);
            float yRot = switch (facing) {
                case SOUTH -> 180f;
                case EAST -> 270f;
                case WEST -> 90f;
                default -> 0f; // NORTH
            };
            if (yRot != 0f) {
                poseStack.translate(0.5, 0, 0.5);
                poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
                poseStack.translate(-0.5, 0, -0.5);
            }
        }

        GeoModelRenderer.render(poseStack, bufferSource, model,
                texture, normalType, glowType,
                packedLight, packedOverlay);

        poseStack.popPose();
    }
}
