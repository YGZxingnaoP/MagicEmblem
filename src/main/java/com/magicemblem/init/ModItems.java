package com.magicemblem.init;

import com.magicemblem.MagicEmblem;
import com.magicemblem.common.item.ExampleItem;
import com.magicemblem.common.item.MagicEmblemItem;
import com.magicemblem.common.item.UsstEmblemPatternItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 模组物品注册表
 *
 * 新增物品时在此类中添加 RegistryObject 即可。
 * 扩展：新增学校校徽时，按命名规范添加新的 RegistryObject。
 */
public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MagicEmblem.MODID);

    // ===== 上海理工大学校徽图案（旗帜图案） =====
    // 合成：纸+2红色染料（无序），性质同原版BannerPatternItem
    public static final RegistryObject<Item> USST_EMBLEM_PATTERN = ITEMS.register(
            "usst_emblem_pattern", UsstEmblemPatternItem::new);

    // ===== 上海理工大学魔法校徽（物品） =====
    // 合成：8木板围绕校徽图案，手持显示 geo.json 模型
    // 特性：fireResistant, NBT提示, 右键放置方块
    public static final RegistryObject<Item> MAGIC_EMBLEM = ITEMS.register(
            "magic_emblem", MagicEmblemItem::new);

    // ===== Example 校徽物品（开发辅助） =====
    // 仅通过创造模式获取，无合成配方
    // 特性：fireResistant, 右键放置方块, 手持显示 geo.json 模型
    public static final RegistryObject<Item> EXAMPLE_EMBLEM = ITEMS.register(
            "example_emblem", ExampleItem::new);
}
