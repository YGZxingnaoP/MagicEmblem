package com.magicemblem.common.item;

import com.magicemblem.init.ModEntities;
import net.minecraftforge.common.ForgeSpawnEggItem;

/**
 * 超级保安刷怪蛋
 *
 * 使用 ForgeSpawnEggItem（接受 Supplier<EntityType>），
 * 避免注册顺序问题：Item 注册时 EntityType 可能尚未完成注册。
 *
 * 使用 SchoolGuard 目录下提供的刷怪蛋材质。
 * 右键方块生成超级保安实体。
 */
public class SchoolGuardSpawnEggItem extends ForgeSpawnEggItem {

    public SchoolGuardSpawnEggItem() {
        super(ModEntities.SCHOOL_GUARD, 0x1A1A2E, 0xFFD700,
                new Properties().stacksTo(64));
    }
}
