package com.magicemblem.common.block;

import com.magicemblem.client.gui.AuthScreen;
import com.magicemblem.common.blockentity.AbstractEmblemBlockEntity;
import com.magicemblem.common.blockentity.MagicEmblemBlockEntity;
import com.magicemblem.init.ModBlockEntities;
import com.magicemblem.init.ModItems;
import com.magicemblem.network.ModNetwork;
import com.magicemblem.network.PlayAnthemPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraftforge.network.PacketDistributor;
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
        if (!pLevel.isClientSide()) {
            // 服务端：发送校歌播放包（统一通过 PlayAnthemPacket 播放，含掐停背景音乐和防重复）
            if (pLevel.getBlockEntity(pPos) instanceof MagicEmblemBlockEntity be
                    && be.getSchoolId() != null
                    && pPlayer instanceof ServerPlayer serverPlayer) {
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new PlayAnthemPacket(be.getSchoolId()));
            }
        } else {
            // 客户端：打开认证界面
            String schoolId = "USST";
            if (pLevel.getBlockEntity(pPos) instanceof MagicEmblemBlockEntity be) {
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
            if (pLevel.isClientSide()) {
                // 停止方块实体级别的校歌
                if (pLevel.getBlockEntity(pPos) instanceof MagicEmblemBlockEntity be) {
                    be.stopAnthem();
                }
                // 同时停止 PlayAnthemPacket 级别的校歌
                PlayAnthemPacket.stopCurrentAnthem();
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
