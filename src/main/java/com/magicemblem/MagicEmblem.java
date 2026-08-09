package com.magicemblem;

import com.magicemblem.client.renderer.EmblemBlockRenderer;
import com.magicemblem.client.renderer.entity.SchoolGuardRenderer;
import com.magicemblem.common.event.ModAdvancementTriggers;
import com.magicemblem.common.event.ModEventHandlers;
import com.magicemblem.init.ModBlocks;
import com.magicemblem.init.ModBlockEntities;
import com.magicemblem.init.ModCreativeTabs;
import com.magicemblem.init.ModEffects;
import com.magicemblem.init.ModEntities;
import com.magicemblem.init.ModItems;
import com.magicemblem.init.ModSoundEvents;
import com.magicemblem.network.ModNetwork;
import com.magicemblem.school.SchoolPasswordManager;
import com.magicemblem.school.SchoolRegistry;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Magic Emblem 主模组类
 * 
 * 基于上海理工大学校徽的趣味模组：
 * - 自研 geo.json 渲染系统（模仿 BBS mod，不依赖 GeckoLib）
 * - 学生身份验证系统
 * - 多种趣味 Buff 和成就
 * 
 * @author MagicEmblem Team
 */
@Mod(MagicEmblem.MODID)
public class MagicEmblem {

    /** 模组ID */
    public static final String MODID = "magicemblem";

    /** 模组日志记录器 */
    public static final Logger LOGGER = LogUtils.getLogger();

    public MagicEmblem(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // 注册模组通用设置事件
        modEventBus.addListener(this::commonSetup);

        // 注册所有注册项
        ModItems.ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModEffects.EFFECTS.register(modEventBus);
        ModSoundEvents.SOUND_EVENTS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);

        // 注册 Forge 事件总线（ModEventHandlers 的 @SubscribeEvent 方法）
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(ModEventHandlers.class);

        // 注册成就触发器（在mod构造器中尽早注册，确保在成就加载前完成）
        ModAdvancementTriggers.register();

        // 注册配置文件
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // 加载学校密码
        SchoolPasswordManager.loadPasswords();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Magic Emblem mod loading - common setup");
        ModNetwork.register();
        SchoolRegistry.registerSchools();
    }

    /**
     * 客户端事件（渲染器注册）
     */
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("Magic Emblem mod loading - client setup");
            event.enqueueWork(() -> {
                // 注册通用校徽渲染器（适用于所有 AbstractEmblemBlockEntity 子类）
                BlockEntityRenderers.register(
                        ModBlockEntities.MAGIC_EMBLEM_BE.get(),
                        EmblemBlockRenderer::new);
                BlockEntityRenderers.register(
                        ModBlockEntities.EXAMPLE_BE.get(),
                        EmblemBlockRenderer::new);
                // 注册超级保安实体渲染器
                EntityRenderers.register(ModEntities.SCHOOL_GUARD.get(), SchoolGuardRenderer::new);
            });
        }
    }
}
