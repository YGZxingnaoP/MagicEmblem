package com.magicemblem.common.block;

import com.magicemblem.client.gui.EmblemGuideScreen;
import com.magicemblem.common.blockentity.AbstractEmblemBlockEntity;
import com.magicemblem.common.blockentity.ExampleBlockEntity;
import com.magicemblem.init.ModBlockEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Example 校徽方块（开发辅助 / 模板示例）
 *
 * 继承 {@link AbstractEmblemBlock}，作为新增校徽方块的参考模板。
 * 右键打开引导界面，展示如何添加自定义模型。
 *
 * 【扩展指南】复制此类，修改方块实体引用和右键行为即可创建新校徽方块。
 */
public class ExampleBlock extends AbstractEmblemBlock {

    @Override
    protected BlockEntityType<? extends AbstractEmblemBlockEntity> getBlockEntityType() {
        return ModBlockEntities.EXAMPLE_BE.get();
    }

    // ===== 方块实体 =====

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new ExampleBlockEntity(pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState,
                                                                    BlockEntityType<T> pBlockEntityType) {
        return pLevel.isClientSide() ?
                createTickerHelper(pBlockEntityType, ModBlockEntities.EXAMPLE_BE.get(),
                        (level, pos, state, blockEntity) -> blockEntity.tick(level, pos, state))
                : null;
    }

    // ===== 右键交互：打开引导界面（无音乐） =====

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos,
                                  Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pLevel.isClientSide()) {
            Minecraft.getInstance().setScreen(new EmblemGuideScreen());
        }
        return InteractionResult.SUCCESS;
    }
}
