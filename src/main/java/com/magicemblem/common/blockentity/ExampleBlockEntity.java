package com.magicemblem.common.blockentity;

import com.magicemblem.MagicEmblem;
import com.magicemblem.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Example 校徽方块实体（开发辅助 / 模板示例）
 *
 * 继承 {@link AbstractEmblemBlockEntity}，仅需提供资源路径。
 * 这是添加新校徽方块实体时最精简的模板。
 *
 * 【扩展指南】
 * 1. 复制此类并重命名
 * 2. 修改下方资源路径指向你的模型/贴图/动画文件
 * 3. 在 ModBlockEntities 中注册
 */
public class ExampleBlockEntity extends AbstractEmblemBlockEntity {

    // ===== Example 资源路径（修改此处指向你的资源） =====

    /** geo.json 模型文件 */
    public static final ResourceLocation MODEL_LOCATION =
            new ResourceLocation(MagicEmblem.MODID, "geo/example.geo.json");

    /** 贴图文件 */
    public static final ResourceLocation TEXTURE_LOCATION =
            new ResourceLocation(MagicEmblem.MODID, "textures/emblem/example_texture.png");

    /** 动画文件 */
    public static final ResourceLocation ANIMATION_LOCATION =
            new ResourceLocation(MagicEmblem.MODID, "animations/example.animation.json");

    /** 默认播放的动画名称（对应动画文件中的 animations 键名） */
    private static final String DEFAULT_ANIM = "example";

    public ExampleBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.EXAMPLE_BE.get(), pPos, pBlockState);
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

    /** Example 方块不参与学校系统（返回 null） */
    @Override
    public String getSchoolId() { return null; }
}
