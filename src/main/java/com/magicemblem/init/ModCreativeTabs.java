package com.magicemblem.init;

import com.magicemblem.MagicEmblem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 模组创造模式标签页注册表
 *
 * 包含校徽图案、魔法校徽物品和开发辅助物品。
 */
public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MagicEmblem.MODID);

    // ===== 魔法校徽创造标签页 =====
    public static final RegistryObject<CreativeModeTab> MAGIC_EMBLEM_TAB = CREATIVE_MODE_TABS.register(
            "magic_emblem_tab", () -> CreativeModeTab.builder()
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.USST_EMBLEM_PATTERN.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        // 校徽图案
                        output.accept(ModItems.USST_EMBLEM_PATTERN.get());
                        // 魔法校徽物品
                        output.accept(ModItems.MAGIC_EMBLEM.get());
                        // Example 校徽（开发辅助）
                        output.accept(ModItems.EXAMPLE_EMBLEM.get());
                    }).build());
}
