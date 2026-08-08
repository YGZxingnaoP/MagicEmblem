package com.magicemblem;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * 模组配置文件
 * 使用Forge配置系统，在游戏中可通过Mod列表界面修改
 */
@Mod.EventBusSubscriber(modid = MagicEmblem.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // ===== 配置项1：校徽图案是否可以在宝箱内找到 =====
    public static final ForgeConfigSpec.BooleanValue EMBLEM_IN_CHESTS = BUILDER
            .comment("Whether the USST emblem pattern can be found in loot chests",
                     "上海理工大学校徽图案是否可以在宝箱内找到")
            .define("emblemInChests", false);

    // ===== 配置项2：校徽图案在宝箱内找到的概率 =====
    public static final ForgeConfigSpec.DoubleValue EMBLEM_CHEST_CHANCE = BUILDER
            .comment("The chance of finding the USST emblem pattern in loot chests (0.0 ~ 1.0)",
                     "上海理工大学校徽图案在宝箱内找到的概率")
            .defineInRange("emblemChestChance", 0.1, 0.0, 1.0);

    // ===== 配置项3："我是光荣的大学牲"buff是否在重进存档后保留 =====
    public static final ForgeConfigSpec.BooleanValue GLORIOUS_STUDENT_PERSIST = BUILDER
            .comment("Whether the 'Glorious Student' effect persists after re-logging",
                     "我是光荣的大学牲buff是否在重进存档后保留")
            .define("gloriousStudentPersist", false);

    /** 构建完成的配置规格 */
    static final ForgeConfigSpec SPEC = BUILDER.build();

    // 缓存的配置值
    public static boolean emblemInChests;
    public static double emblemChestChance;
    public static boolean gloriousStudentPersist;

    /**
     * 配置加载/重载时的回调
     * 将ForgeConfigSpec中的值同步到静态字段
     */
    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        emblemInChests = EMBLEM_IN_CHESTS.get();
        emblemChestChance = EMBLEM_CHEST_CHANCE.get();
        gloriousStudentPersist = GLORIOUS_STUDENT_PERSIST.get();
    }
}
