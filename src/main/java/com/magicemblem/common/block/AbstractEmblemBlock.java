package com.magicemblem.common.block;

import com.magicemblem.common.blockentity.AbstractEmblemBlockEntity;
import com.magicemblem.network.CameraAnimTriggerPacket;
import com.magicemblem.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * 魔法校徽方块抽象基类
 *
 * 所有校徽方块的通用逻辑，包括：
 * - HORIZONTAL_FACING 朝向属性（放置时北面朝向玩家）
 * - ENTITYBLOCK_ANIMATED 渲染模式（由 BlockEntityRenderer 渲染）
 * - 首次放置触发运镜动画（仅对放置者生效，不影响其他玩家）
 * - 合理的碰撞箱
 *
 * 子类需要实现：
 * - {@link #newBlockEntity} — 创建对应的方块实体
 * - {@link #getTicker} — 绑定方块实体类型
 * - {@link #use} — 右键交互行为
 * - {@link #getBlockEntityType} — 返回方块实体注册类型
 *
 * 【扩展指南】新增校徽方块时继承此类，只需实现上述抽象方法即可。
 */
public abstract class AbstractEmblemBlock extends BaseEntityBlock {

    /** 方块朝向属性（水平方向） */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** 方块的视觉/选择碰撞箱（中心区域） */
    private static final VoxelShape SHAPE = box(2, 0, 2, 14, 14, 14);

    public AbstractEmblemBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.0f, 3.0f)
                .sound(SoundType.METAL)
                .noOcclusion()
                .lightLevel(state -> 7));
        // 默认朝向为北
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    // ===== BlockState 定义 =====

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING);
    }

    /** 放置时北面朝向玩家 */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
    }

    // ===== 碰撞体积 =====

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos,
                                CollisionContext pContext) {
        return SHAPE;
    }

    @Override
    public VoxelShape getInteractionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return SHAPE;
    }

    // ===== 渲染类型：由 BlockEntityRenderer 渲染 =====

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    // ===== 抽象方法（子类实现） =====

    /** 获取此方块对应的 BlockEntityType（用于 getTicker 类型匹配） */
    protected abstract BlockEntityType<? extends AbstractEmblemBlockEntity> getBlockEntityType();

    // ===== 首次放置运镜动画 =====

    /**
     * 方块放置后触发运镜动画（仅影响放置者）
     *
     * 实现方式：
     * 1. 设置方块实体的 firstPlace = true
     * 2. 通过 PacketDistributor.PLAYER 仅向放置者发送 CameraAnimTriggerPacket
     * 3. 多人联机时其他玩家不受影响
     */
    protected void triggerPlacementCameraAnim(Level level, BlockPos placePos,
                                                Player player, BlockState placeState) {
        if (level.isClientSide()) return;

        // 设置 firstPlace 标记
        if (level.getBlockEntity(placePos) instanceof AbstractEmblemBlockEntity be) {
            be.setFirstPlace(true);
            level.sendBlockUpdated(placePos, placeState, placeState, 3);
        }

        // 仅向放置者发送运镜触发包（不影响其他玩家）
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getPersistentData().putBoolean("saw_camera_anim", true);
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new CameraAnimTriggerPacket(placePos));
        }
    }
}
