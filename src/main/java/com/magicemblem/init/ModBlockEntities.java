package com.magicemblem.init;

import com.magicemblem.MagicEmblem;
import com.magicemblem.common.blockentity.ExampleBlockEntity;
import com.magicemblem.common.blockentity.MagicEmblemBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 模组方块实体注册表
 *
 * 新增校徽方块实体时添加新的 RegistryObject 即可。
 */
public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MagicEmblem.MODID);

    // ===== 上海理工大学魔法校徽方块实体 =====
    // 支持 idle 动画、认证 GUI、首次放置相机动画、校歌播放
    public static final RegistryObject<BlockEntityType<MagicEmblemBlockEntity>> MAGIC_EMBLEM_BE =
            BLOCK_ENTITIES.register("magic_emblem_be",
                    () -> BlockEntityType.Builder.of(MagicEmblemBlockEntity::new,
                            ModBlocks.MAGIC_EMBLEM_BLOCK.get()).build(null));

    // ===== Example 校徽方块实体（开发辅助） =====
    // 支持 example 动画、引导界面、首次放置相机动画
    public static final RegistryObject<BlockEntityType<ExampleBlockEntity>> EXAMPLE_BE =
            BLOCK_ENTITIES.register("example_be",
                    () -> BlockEntityType.Builder.of(ExampleBlockEntity::new,
                            ModBlocks.EXAMPLE_BLOCK.get()).build(null));
}
