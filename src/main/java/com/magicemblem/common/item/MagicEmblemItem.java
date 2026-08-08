package com.magicemblem.common.item;

import com.magicemblem.client.geo.GeoAnimationPlayer;
import com.magicemblem.client.geo.GeoModel;
import com.magicemblem.client.geo.GeoModelParser;
import com.magicemblem.client.geo.GeoModelRenderer;
import com.magicemblem.client.ModRenderTypes;
import com.magicemblem.common.block.AbstractEmblemBlock;
import com.magicemblem.common.blockentity.MagicEmblemBlockEntity;
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
 * 上海理工大学魔法校徽物品
 *
 * 继承 {@link AbstractEmblemItem}，额外提供：
 * - 放置 MagicEmblemBlock
 * - NBT 悬浮提示（学校名称）
 * - 手持渲染：GUI/地面→idle，手持→on_hand
 */
public class MagicEmblemItem extends AbstractEmblemItem {

    @Override
    public AbstractEmblemBlock getBlock() {
        return (AbstractEmblemBlock) ModBlocks.MAGIC_EMBLEM_BLOCK.get();
    }

    // ===== NBT 悬浮提示 =====

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel,
                                 List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.translatable("item.magicemblem.magic_emblem.school",
                Component.literal("上海理工大学")));
        pTooltipComponents.add(Component.translatable("item.magicemblem.magic_emblem.desc"));
    }

    // ===== 自定义手持渲染器（BEWLR + GeoModelRenderer） =====

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private MagicEmblemItemBEWLR renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new MagicEmblemItemBEWLR();
                }
                return this.renderer;
            }
        });
    }

    /**
     * 物品手持渲染器
     * - GUI/地面/展示框 → idle 动画
     * - 手持（第一/三人称）→ on_hand 动画
     */
    private static class MagicEmblemItemBEWLR extends BlockEntityWithoutLevelRenderer {

        private GeoModel cachedModel;
        private final GeoAnimationPlayer animationPlayer = new GeoAnimationPlayer();
        private boolean modelLoaded = false;

        public MagicEmblemItemBEWLR() {
            super(null, null);
        }

        @Override
        public void renderByItem(ItemStack pStack, ItemDisplayContext pTransformType,
                                  PoseStack pPoseStack, MultiBufferSource pBuffer,
                                  int pPackedLight, int pPackedOverlay) {
            if (!modelLoaded) {
                ResourceManager rm = Minecraft.getInstance().getResourceManager();
                cachedModel = GeoModelParser.parse(rm, MagicEmblemBlockEntity.MODEL_LOCATION);
                animationPlayer.load(rm, MagicEmblemBlockEntity.ANIMATION_LOCATION);
                modelLoaded = true;
            }
            if (cachedModel == null) return;

            // 根据显示上下文选择动画
            boolean isHeld = pTransformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                    || pTransformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                    || pTransformType == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                    || pTransformType == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;

            // 仅手持时播放 on_hand 动画，其他场景（物品栏/GUI/地面）保持静止
            if (isHeld) {
                if (!"on_hand".equals(animationPlayer.getCurrentAnim())) {
                    animationPlayer.play("on_hand", true);
                }
                animationPlayer.tick(1.0f / 20.0f);
                animationPlayer.apply(cachedModel);
            }
            // 非手持场景不推进动画，模型保持原始 T-pose 静止

            ResourceLocation texture = MagicEmblemBlockEntity.TEXTURE_LOCATION;
            RenderType normalType = RenderType.entityCutout(texture);
            RenderType glowType = ModRenderTypes.glow(texture);

            pPoseStack.pushPose();

            // 渲染缩放和调整
            if (pTransformType == ItemDisplayContext.GUI) {
                pPoseStack.scale(0.6f, 0.6f, 0.6f);
            } else if (isHeld) {
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
