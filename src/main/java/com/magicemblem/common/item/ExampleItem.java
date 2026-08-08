package com.magicemblem.common.item;

import com.magicemblem.client.geo.GeoAnimationPlayer;
import com.magicemblem.client.geo.GeoModel;
import com.magicemblem.client.geo.GeoModelParser;
import com.magicemblem.client.geo.GeoModelRenderer;
import com.magicemblem.client.ModRenderTypes;
import com.magicemblem.common.block.AbstractEmblemBlock;
import com.magicemblem.common.blockentity.ExampleBlockEntity;
import com.magicemblem.init.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * Example 校徽物品（开发辅助 / 模板示例）
 *
 * 继承 {@link AbstractEmblemItem}，仅通过创造模式获取。
 * 手持渲染使用 example 动画（所有场景统一动画）。
 *
 * 【扩展指南】复制此类，修改 getBlock() 和 BEWLR 中的资源引用即可。
 */
public class ExampleItem extends AbstractEmblemItem {

    @Override
    public AbstractEmblemBlock getBlock() {
        return (AbstractEmblemBlock) ModBlocks.EXAMPLE_BLOCK.get();
    }

    // ===== 悬浮提示 =====

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel,
                                 List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.translatable("item.magicemblem.example_emblem.desc"));
    }

    // ===== 自定义手持渲染器 =====

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private ExampleItemBEWLR renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new ExampleItemBEWLR();
                }
                return this.renderer;
            }
        });
    }

    /**
     * Example 物品手持渲染器
     * - 物品栏/GUI/地面 → 静止（不播放动画）
     * - 手持（第一/三人称）→ on_hand 动画
     */
    private static class ExampleItemBEWLR extends BlockEntityWithoutLevelRenderer {

        private GeoModel cachedModel;
        private final GeoAnimationPlayer animationPlayer = new GeoAnimationPlayer();
        private boolean modelLoaded = false;

        public ExampleItemBEWLR() {
            super(null, null);
        }

        @Override
        public void renderByItem(ItemStack pStack, ItemDisplayContext pTransformType,
                                  PoseStack pPoseStack, MultiBufferSource pBuffer,
                                  int pPackedLight, int pPackedOverlay) {
            if (!modelLoaded) {
                ResourceManager rm = Minecraft.getInstance().getResourceManager();
                cachedModel = GeoModelParser.parse(rm, ExampleBlockEntity.MODEL_LOCATION);
                animationPlayer.load(rm, ExampleBlockEntity.ANIMATION_LOCATION);
                modelLoaded = true;
            }
            if (cachedModel == null) return;

            // 根据显示上下文区分动画
            boolean isHeld = pTransformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                    || pTransformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                    || pTransformType == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                    || pTransformType == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;

            // 仅手持时播放 on_hand 动画，其他场景保持静止
            if (isHeld) {
                if (!"on_hand".equals(animationPlayer.getCurrentAnim())) {
                    animationPlayer.play("on_hand", true);
                }
                animationPlayer.tick(1.0f / 20.0f);
                animationPlayer.apply(cachedModel);
            }
            // 非手持场景不推进动画，模型保持原始 T-pose 静止

            ResourceLocation texture = ExampleBlockEntity.TEXTURE_LOCATION;
            RenderType normalType = RenderType.entityCutout(texture);
            RenderType glowType = ModRenderTypes.glow(texture);

            pPoseStack.pushPose();

            // 渲染缩放（根据场景微调）
            if (pTransformType == ItemDisplayContext.GUI) {
                pPoseStack.scale(0.6f, 0.6f, 0.6f);
            } else if (pTransformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                    || pTransformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                    || pTransformType == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                    || pTransformType == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
                pPoseStack.scale(0.4f, 0.4f, 0.4f);
                pPoseStack.translate(0, 0.5, 0);
            } else {
                pPoseStack.scale(0.5f, 0.5f, 0.5f);
                pPoseStack.translate(0, 0.3, 0);
            }

            GeoModelRenderer.render(pPoseStack, pBuffer, cachedModel, texture,
                    normalType, glowType, pPackedLight, pPackedOverlay);

            pPoseStack.popPose();
        }
    }
}
