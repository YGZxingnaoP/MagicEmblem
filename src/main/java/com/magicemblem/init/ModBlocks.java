package com.magicemblem.init;

import com.magicemblem.MagicEmblem;
import com.magicemblem.common.block.ExampleBlock;
import com.magicemblem.common.block.MagicEmblemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 模组方块注册表
 *
 * 所有校徽方块在此注册。
 * 新增校徽方块时添加新的 RegistryObject 即可。
 */
public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MagicEmblem.MODID);

    // ===== 上海理工大学魔法校徽方块 =====
    // 通过 MagicEmblemItem 右键放置，右键打开认证界面 + 播放校歌
    public static final RegistryObject<Block> MAGIC_EMBLEM_BLOCK = BLOCKS.register(
            "magic_emblem_block", MagicEmblemBlock::new);

    // ===== Example 校徽方块（开发辅助） =====
    // 仅通过创造模式获取，右键打开模型添加指南界面
    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register(
            "example_block", ExampleBlock::new);
}
