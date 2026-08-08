package com.magicemblem.common.item;

import com.magicemblem.common.block.AbstractEmblemBlock;
import com.magicemblem.common.blockentity.AbstractEmblemBlockEntity;
import com.magicemblem.network.CameraAnimTriggerPacket;
import com.magicemblem.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraftforge.network.PacketDistributor;

/**
 * 魔法校徽物品抽象基类
 *
 * 所有校徽物品的通用逻辑，包括：
 * - 右键放置对应方块
 * - 放置时触发运镜动画（仅对放置者生效）
 * - 不会被火焰/仙人掌破坏
 * - RARE 稀有度、不可堆叠
 *
 * 子类需要实现：
 * - {@link #getBlock()} — 返回对应的方块
 * - {@link #appendHoverText} — 自定义悬浮提示信息
 *
 * 【扩展指南】新增校徽物品时继承此类，提供对应方块引用即可。
 */
public abstract class AbstractEmblemItem extends Item {

    public AbstractEmblemItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.RARE)
                .fireResistant());
    }

    // ===== 抽象方法 =====

    /** 获取此物品对应的方块 */
    public abstract AbstractEmblemBlock getBlock();

    // ===== 右键放置方块 =====

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        Level level = pContext.getLevel();
        BlockPos clickedPos = pContext.getClickedPos();
        BlockPos placePos = clickedPos.relative(pContext.getClickedFace());

        if (!level.isClientSide()) {
            // 使用 getStateForPlacement 获取正确的朝向（正面朝向玩家）
            BlockPlaceContext placeContext = new BlockPlaceContext(pContext);
            BlockState placeState = getBlock().getStateForPlacement(placeContext);
            if (placeState == null) return InteractionResult.FAIL;
            if (level.setBlock(placePos, placeState, 3)) {
                pContext.getItemInHand().shrink(1);

                // 触发运镜动画（仅对放置者生效）
                triggerPlacementCameraAnim(level, placePos, pContext, placeState);

                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    /**
     * 触发放置运镜动画
     * 子类可覆盖此方法自定义放置行为
     */
    protected void triggerPlacementCameraAnim(Level level, BlockPos placePos,
                                                UseOnContext pContext, BlockState placeState) {
        if (level.getBlockEntity(placePos) instanceof AbstractEmblemBlockEntity be) {
            be.setFirstPlace(true);
            level.sendBlockUpdated(placePos, placeState, placeState, 3);
        }

        if (pContext.getPlayer() instanceof ServerPlayer serverPlayer) {
            serverPlayer.getPersistentData().putBoolean("saw_camera_anim", true);
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new CameraAnimTriggerPacket(placePos));
        }
    }

    // ===== 不会被仙人掌/火焰破坏 =====

    @Override
    public boolean canBeHurtBy(DamageSource damageSource) {
        String msgId = damageSource.type().msgId();
        if ("cactus".equals(msgId)) {
            return false;
        }
        return super.canBeHurtBy(damageSource);
    }
}
