package com.magicemblem.init;

import com.magicemblem.MagicEmblem;
import com.magicemblem.common.entity.SchoolGuardEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 模组实体注册表
 *
 * 注册超级保安实体。
 */
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MagicEmblem.MODID);

    /** 超级保安实体类型 */
    public static final RegistryObject<EntityType<SchoolGuardEntity>> SCHOOL_GUARD = ENTITIES.register(
            "school_guard", () -> EntityType.Builder
                    .of(SchoolGuardEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 2.0f) // 碰撞箱：宽0.6格，高2格
                    .clientTrackingRange(10)
                    .build("school_guard"));

    /**
     * 注册实体属性（在 FMLCommonSetupEvent 之后由 EntityAttributeCreationEvent 触发）
     */
    @Mod.EventBusSubscriber(modid = MagicEmblem.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEntityAttributes {
        @SubscribeEvent
        public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
            event.put(ModEntities.SCHOOL_GUARD.get(), SchoolGuardEntity.createAttributes().build());
        }
    }
}
