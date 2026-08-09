package com.magicemblem.client.renderer.entity;

import com.magicemblem.client.ModRenderTypes;
import com.magicemblem.client.geo.GeoModel;
import com.magicemblem.client.geo.GeoModelRenderer;
import com.magicemblem.common.entity.SchoolGuardEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 超级保安实体渲染器
 *
 * 使用与方块完全相同的 GeoModelRenderer 进行逐面UV渲染。
 *
 * 坐标轴处理：
 * - GeoModelRenderer.render() 内部会 translate(0.5, 0, 0.5)（方块居中用）
 *   实体渲染时需要预先 translate(-0.5, 0, -0.5) 抵消
 * - Bedrock 模型默认朝南（+Z），Minecraft 实体 yaw=0 时也朝南
 *   GeoModelParser 已处理 Bedrock→Java 的 X 轴翻转
 *   需要绕 Y 轴旋转 180° - entityYaw 使模型朝向实体朝向
 * - 模型坐标为像素（16px=1格），GeoModelRenderer 内部 SCALE=1/16 处理
 * - 模型原点在脚底（Y=0），与实体渲染原点一致
 *
 * 光照参数：
 * - packedLight: 使用渲染器传入的实际光照值，使实体受环境光照影响
 * - packedOverlay: 使用 OverlayTexture.NO_OVERLAY，避免错误的overlay坐标导致材质全黑
 */
@OnlyIn(Dist.CLIENT)
public class SchoolGuardRenderer extends EntityRenderer<SchoolGuardEntity> {

    public SchoolGuardRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5f;
    }

    @Override
    public void render(SchoolGuardEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight) {
        GeoModel model = entity.getModel();
        if (model == null) {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            return;
        }

        ResourceLocation texture = entity.getTextureLocation();
        RenderType normalType = RenderType.entityCutout(texture);
        RenderType glowType = ModRenderTypes.glow(texture);

        poseStack.pushPose();

        // 绕 Y 轴旋转，使模型朝向实体朝向
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - entityYaw));

        // 抵消 GeoModelRenderer.render() 内部的 translate(0.5, 0, 0.5)
        poseStack.translate(-0.5, 0, -0.5);

        // 渲染模型
        // packedLight: 使用实际环境光照
        // packedOverlay: OverlayTexture.NO_OVERLAY (正确值，避免材质全黑)
        GeoModelRenderer.render(poseStack, bufferSource, model, texture,
                normalType, glowType, packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();

        // 渲染名称标签（如果可见）
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SchoolGuardEntity entity) {
        return SchoolGuardEntity.TEXTURE_LOCATION;
    }
}
