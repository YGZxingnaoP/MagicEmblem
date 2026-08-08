package com.magicemblem.common.blockentity;

import com.magicemblem.MagicEmblem;
import com.magicemblem.init.ModBlockEntities;
import com.magicemblem.init.ModSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 上海理工大学魔法校徽方块实体
 *
 * 继承 {@link AbstractEmblemBlockEntity}，额外提供：
 * - 校歌播放（SimpleSoundInstance 可靠控制）
 * - USST 专属模型/贴图/动画路径
 */
public class MagicEmblemBlockEntity extends AbstractEmblemBlockEntity {

    // ===== USST 资源路径 =====

    public static final ResourceLocation MODEL_LOCATION =
            new ResourceLocation(MagicEmblem.MODID, "geo/usst_emblem.geo.json");
    public static final ResourceLocation TEXTURE_LOCATION =
            new ResourceLocation(MagicEmblem.MODID, "textures/emblem/usst_main_texture.png");
    public static final ResourceLocation ANIMATION_LOCATION =
            new ResourceLocation(MagicEmblem.MODID, "animations/usst_emblem.animation.json");
    private static final String DEFAULT_ANIM = "idle";

    /** USST 学校标识 */
    public static final String SCHOOL_ID = "USST";

    public MagicEmblemBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.MAGIC_EMBLEM_BE.get(), pPos, pBlockState);
    }

    // ===== 资源路径实现 =====

    @Override
    public ResourceLocation getModelLocation() { return MODEL_LOCATION; }

    @Override
    public ResourceLocation getTextureLocation() { return TEXTURE_LOCATION; }

    @Override
    public ResourceLocation getAnimationLocation() { return ANIMATION_LOCATION; }

    @Override
    public String getDefaultAnimationName() { return DEFAULT_ANIM; }

    @Override
    public String getSchoolId() { return SCHOOL_ID; }

    // ===== 校歌播放（客户端 SimpleSoundInstance） =====

    /** 当前校歌音效实例（客户端） */
    private SimpleSoundInstance anthemSoundInstance;

    /**
     * 播放校歌（客户端）
     * 使用 SimpleSoundInstance 存储引用，确保可以可靠停止
     */
    public void playAnthem() {
        if (level == null || !level.isClientSide()) return;
        if (anthemSoundInstance != null
                && Minecraft.getInstance().getSoundManager().isActive(anthemSoundInstance)) {
            return;
        }
        anthemSoundInstance = SimpleSoundInstance.forRecord(
                ModSoundEvents.USST_ANTHEM.get(),
                Vec3.atCenterOf(worldPosition));
        Minecraft.getInstance().getSoundManager().play(anthemSoundInstance);
    }

    /** 停止校歌（客户端） */
    public void stopAnthem() {
        if (level == null || !level.isClientSide()) return;
        if (anthemSoundInstance != null) {
            Minecraft.getInstance().getSoundManager().stop(anthemSoundInstance);
            anthemSoundInstance = null;
        }
    }

    /** 校歌是否正在播放 */
    public boolean isAnthemPlaying() {
        if (anthemSoundInstance == null) return false;
        return Minecraft.getInstance().getSoundManager().isActive(anthemSoundInstance);
    }
}
