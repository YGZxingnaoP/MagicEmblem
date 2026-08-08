package com.magicemblem.common.block;

import com.magicemblem.client.gui.AuthScreen;
import com.magicemblem.common.blockentity.AbstractEmblemBlockEntity;
import com.magicemblem.common.blockentity.MagicEmblemBlockEntity;
import com.magicemblem.init.ModBlockEntities;
import com.magicemblem.init.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 上海理工大学魔法校徽方块
 *
 * 继承 {@link AbstractEmblemBlock}，额外提供：
 * - 右键打开认证界面 + 播放校歌
 * - 破坏时停止校歌
 * - 掉落校徽图案物品
 */
public class MagicEmblemBlock extends AbstractEmblemBlock {

    @Override
    protected BlockEntityType<? extends AbstractEmblemBlockEntity> getBlockEntityType() {
        return ModBlockEntities.MAGIC_EMBLEM_BE.get();
    }

    // ===== 方块实体 =====

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new MagicEmblemBlockEntity(pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState,
                                                                    BlockEntityType<T> pBlockEntityType) {
        return pLevel.isClientSide() ?
                createTickerHelper(pBlockEntityType, ModBlockEntities.MAGIC_EMBLEM_BE.get(),
                        (level, pos, state, blockEntity) -> blockEntity.tick(level, pos, state))
                : null;
    }

    // ===== 右键交互：打开认证界面 + 播放校歌 =====

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos,
                                  Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pLevel.isClientSide()) {
            // 读取学校ID（用于认证界面显示）
            String schoolId = "USST";
            if (pLevel.getBlockEntity(pPos) instanceof MagicEmblemBlockEntity be) {
                // 播放校歌（如果未播放）
                if (!be.isAnthemPlaying()) {
                    be.playAnthem();
                }
                schoolId = be.getSchoolId() != null ? be.getSchoolId() : "USST";
            }
            Minecraft.getInstance().setScreen(new AuthScreen(pPos, schoolId));
        }
        return InteractionResult.SUCCESS;
    }

    // ===== 方块被破坏时停止校歌 =====

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos,
                          BlockState pNewState, boolean pMovedByPiston) {
        if (!pState.is(pNewState.getBlock())) {
            if (pLevel.isClientSide() && pLevel.getBlockEntity(pPos) instanceof MagicEmblemBlockEntity be) {
                be.stopAnthem();
            }
            super.onRemove(pState, pLevel, pPos, pNewState, pMovedByPiston);
        }
    }

    // ===== 破坏后掉落校徽图案 =====

    @Override
    public List<ItemStack> getDrops(BlockState pState, LootParams.Builder pBuilder) {
        List<ItemStack> drops = new ArrayList<>(super.getDrops(pState, pBuilder));
        drops.add(new ItemStack(ModItems.USST_EMBLEM_PATTERN.get()));
        return drops;
    }
}
