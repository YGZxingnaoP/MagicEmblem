package com.magicemblem.common.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 上海理工大学校徽图案物品
 * 
 * 性质：
 * - 物品栏图标使用 icon.png
 * - 合成：工作台/背包，纸+2个红色染料（无序）
 * - 作为合成材料用于魔法校徽合成
 * 
 * 扩展：后续新增其他学校校徽时，创建类似的Item子类即可
 */
public class UsstEmblemPatternItem extends Item {

    public UsstEmblemPatternItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON));
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, 
                                 List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.translatable("item.magicemblem.usst_emblem_pattern.desc"));
    }
}
