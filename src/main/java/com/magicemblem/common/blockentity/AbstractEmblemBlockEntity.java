package com.magicemblem.common.blockentity;

import com.magicemblem.MagicEmblem;
import com.magicemblem.client.geo.GeoAnimationPlayer;
import com.magicemblem.client.geo.GeoModel;
import com.magicemblem.client.geo.GeoModelParser;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 魔法校徽方块实体抽象基类
 *
 * 所有校徽方块实体的通用逻辑，包括：
 * - geo.json 模型加载与缓存（客户端）
 * - GeoAnimationPlayer 动画播放与推进
 * - firstPlace 标记管理（首次放置运镜动画）
 * - NBT 序列化与网络同步
 *
 * 子类需要实现：
 * - {@link #getModelLocation()} — geo.json 模型文件路径
 * - {@link #getTextureLocation()} — 贴图路径
 * - {@link #getAnimationLocation()} — 动画文件路径
 * - {@link #getDefaultAnimationName()} — 默认播放的动画名称
 *
 * 【扩展指南】新增校徽方块实体时继承此类，只需提供资源路径即可。
 * 渲染器通过 {@link #getModel()} 和 {@link #getTextureLocation()} 获取渲染数据。
 */
public abstract class AbstractEmblemBlockEntity extends BlockEntity {

    // ===== 模型和动画（客户端） =====

    /** 解析后的 geo 模型（客户端缓存） */
    private GeoModel cachedModel;

    /** 动画播放器 */
    protected final GeoAnimationPlayer animationPlayer = new GeoAnimationPlayer();

    /** 模型是否已加载 */
    private boolean modelLoaded = false;

    // ===== 放置状态 =====

    /** 是否为首次放置（服务端→客户端同步） */
    private boolean firstPlace = false;

    /** 客户端：是否已经播放过放置动画（防止重进世界重复触发） */
    private boolean hasAnimated = false;

    public AbstractEmblemBlockEntity(BlockEntityType<?> type, BlockPos pPos, BlockState pBlockState) {
        super(type, pPos, pBlockState);
    }

    // ===== 抽象方法：子类提供资源路径 =====

    /** geo.json 模型文件路径 */
    public abstract ResourceLocation getModelLocation();

    /** 贴图路径 */
    public abstract ResourceLocation getTextureLocation();

    /** 动画文件路径 */
    public abstract ResourceLocation getAnimationLocation();

    /** 默认播放的动画名称 */
    public abstract String getDefaultAnimationName();

    /**
     * 获取此方块实体对应的学校ID
     * 用于认证、校歌播放等学校相关逻辑
     *
     * @return 学校ID（如 "USST"），返回 null 表示此方块不参与学校系统
     */
    public abstract String getSchoolId();

    // ===== Tick 和模型管理 =====

    /**
     * 方块实体 tick 方法
     * 由子类的 Block 在 getTicker 中调用
     * 仅在客户端执行：加载模型、推进动画、应用到骨骼
     */
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            if (!modelLoaded) {
                loadModel();
            }
            // 推进动画（每 tick = 1/20 秒）
            animationPlayer.tick(1.0f / 20.0f);
            if (cachedModel != null) {
                animationPlayer.apply(cachedModel);
            }
        }
    }

    /**
     * 加载模型和动画资源（客户端）
     * 首次 tick 时自动调用
     */
    private void loadModel() {
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        cachedModel = GeoModelParser.parse(rm, getModelLocation());
        animationPlayer.load(rm, getAnimationLocation());
        animationPlayer.play(getDefaultAnimationName(), true);
        modelLoaded = true;
    }

    /**
     * 获取已解析的 geo 模型（供渲染器使用）
     * 懒加载：首次调用时加载模型
     */
    public GeoModel getModel() {
        if (!modelLoaded) loadModel();
        return cachedModel;
    }

    // ===== firstPlace 管理 =====

    public boolean isFirstPlace() {
        return firstPlace;
    }

    /**
     * 设置首次放置标记
     * 服务端调用时自动通过网络同步到附近客户端
     */
    public void setFirstPlace(boolean firstPlace) {
        this.firstPlace = firstPlace;
        setChanged();
        // 立即通过数据包同步到客户端
        if (level != null && !level.isClientSide()) {
            MagicEmblem.LOGGER.info("[{}] setFirstPlace({}) at {}, syncing to clients",
                    getClass().getSimpleName(), firstPlace, worldPosition);
            ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(this);
            for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                if (player.distanceToSqr(
                        worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5) < 64 * 64) {
                    player.connection.send(packet);
                }
            }
        }
    }

    /** 清除 firstPlace 标记（动画播放后调用） */
    public void clearFirstPlace() {
        this.firstPlace = false;
        setChanged();
    }

    public boolean hasAnimated() {
        return hasAnimated;
    }

    // ===== NBT 序列化/反序列化 =====

    @Override
    public void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        pTag.putBoolean("firstPlace", firstPlace);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        firstPlace = pTag.getBoolean("firstPlace");
    }

    // ===== 网络同步（确保 firstPlace 等字段从服务端同步到客户端） =====

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }
}
