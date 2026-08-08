package com.magicemblem.client.geo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * geo.json 模型渲染器（照搬 BBS CubicCubeRenderer + ICubicRenderer）
 *
 * 渲染管线（完全匹配 BBS）：
 * 1. ICubicRenderer.applyGroupTransformations → 骨骼级 pivot + rotation + scale
 * 2. CubicCubeRenderer.renderCube → cube级 pivot + rotation（push/pop）
 * 3. CubicCubeRenderer 遍历 cube.quads 输出顶点
 *
 * 外面渲染为原版默认机制（面剔除启用），GlowingPart 用 emissive 着色器
 */
public class GeoModelRenderer {

    private static final float SCALE = 1.0f / 16.0f;

    /**
     * 渲染整个 geo 模型
     */
    public static void render(PoseStack poseStack, MultiBufferSource bufferSource,
                               GeoModel model, ResourceLocation texture,
                               RenderType renderType, RenderType glowType,
                               int light, int overlay) {
        if (model == null) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);

        for (GeoBone bone : model.bones) {
            renderBone(poseStack, bufferSource, bone,
                    renderType, glowType, light, overlay);
        }

        poseStack.popPose();
    }

    /**
     * 渲染单个骨骼及其子骨骼（递归）
     */
    private static void renderBone(PoseStack poseStack, MultiBufferSource bufferSource,
                                    GeoBone bone,
                                    RenderType renderType, RenderType glowType,
                                    int light, int overlay) {
        poseStack.pushPose();

        // 1. 移动到骨骼 pivot 点
        float px = bone.pivot[0] * SCALE;
        float py = bone.pivot[1] * SCALE;
        float pz = bone.pivot[2] * SCALE;
        poseStack.translate(px, py, pz);

        // 2. 应用动画旋转
        if (bone.rotation[0] != 0 || bone.rotation[1] != 0 || bone.rotation[2] != 0) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(bone.rotation[2]));
            poseStack.mulPose(Axis.YP.rotationDegrees(bone.rotation[1]));
            poseStack.mulPose(Axis.XP.rotationDegrees(bone.rotation[0]));
        }

        // 3. 应用动画位移
        if (bone.posOffset[0] != 0 || bone.posOffset[1] != 0 || bone.posOffset[2] != 0) {
            poseStack.translate(
                    bone.posOffset[0] * SCALE,
                    bone.posOffset[1] * SCALE,
                    bone.posOffset[2] * SCALE);
        }

        // 4. 应用动画缩放
        if (bone.scale[0] != 1 || bone.scale[1] != 1 || bone.scale[2] != 1) {
            poseStack.scale(bone.scale[0], bone.scale[1], bone.scale[2]);
        }

        // 5. 移回 pivot（反方向）
        poseStack.translate(-px, -py, -pz);

        // 判断是否为 GlowingPart 骨骼
        boolean isGlowing = bone.name != null && bone.name.startsWith("GlowingPart");
        RenderType currentRenderType = isGlowing ? glowType : renderType;
        int currentLight = isGlowing ? 0xF000F0 : light;

        // 6. 渲染骨骼上的所有方块
        // BBS CubicCubeRenderer.renderCube(): push → cube pivot/rotate → render → pop
        for (GeoCube cube : bone.cubes) {
            renderCube(poseStack, bufferSource, cube, currentRenderType, currentLight, overlay);
        }

        // 7. 递归渲染子骨骼
        for (GeoBone child : bone.children) {
            renderBone(poseStack, bufferSource, child,
                    renderType, glowType, light, overlay);
        }

        poseStack.popPose();
    }

    /**
     * 渲染单个 cube（照搬 BBS CubicCubeRenderer.renderCube 行133-157）
     *
     * BBS 在渲染时应用 cube 级别的 pivot + rotation 变换：
     *   stack.push();
     *   moveToPivot(stack, cube.pivot);   // translate by pivot/16
     *   rotate(stack, cube.rotate);       // ZYX rotation
     *   moveBackFromPivot(stack, cube.pivot);
     *   // ... render quads (vertices transformed by matrix stack) ...
     *   stack.pop();
     *
     * generateQuads() 生成的顶点在 cube 局部坐标系中，
     * cube 旋转在渲染时通过 PoseStack 矩阵变换完成
     */
    private static void renderCube(PoseStack poseStack, MultiBufferSource bufferSource,
                                    GeoCube cube, RenderType renderType,
                                    int light, int overlay) {
        if (cube.quads.isEmpty()) return;

        poseStack.pushPose();

        // BBS: moveToPivot → rotate → moveBackFromPivot
        if (cube.pivot != null && cube.rotation != null) {
            float px = cube.pivot[0] * SCALE;
            float py = cube.pivot[1] * SCALE;
            float pz = cube.pivot[2] * SCALE;
            poseStack.translate(px, py, pz);
            // BBS 旋转顺序: Z → Y → X（见 CubicCubeRenderer.rotate 行69-80）
            poseStack.mulPose(Axis.ZP.rotationDegrees(cube.rotation[2]));
            poseStack.mulPose(Axis.YP.rotationDegrees(cube.rotation[1]));
            poseStack.mulPose(Axis.XP.rotationDegrees(cube.rotation[0]));
            poseStack.translate(-px, -py, -pz);
        }

        VertexConsumer buffer = bufferSource.getBuffer(renderType);
        PoseStack.Pose last = poseStack.last();
        Matrix4f matrix = last.pose();
        Matrix3f normalMatrix = last.normal();

        for (GeoCube.GeoQuad quad : cube.quads) {
            // BBS: stack.peek().getNormalMatrix().transform(this.normal)
            Vector3f n = normalMatrix.transform(new Vector3f(quad.nx, quad.ny, quad.nz));

            for (int i = 0; i < 4; i++) {
                // BBS: stack.peek().getPositionMatrix().transform(this.vertex)
                buffer.vertex(matrix, quad.vx[i], quad.vy[i], quad.vz[i])
                        .color(1.0f, 1.0f, 1.0f, 1.0f)
                        .uv(quad.u[i], quad.v[i])
                        .overlayCoords(overlay)
                        .uv2(light)
                        .normal(n.x, n.y, n.z)
                        .endVertex();
            }
        }

        poseStack.popPose();
    }
}
