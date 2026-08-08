package com.magicemblem.init;

import com.magicemblem.MagicEmblem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 模组音效注册表
 */
public class ModSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MagicEmblem.MODID);

    /** 校歌音效 */
    public static final RegistryObject<SoundEvent> USST_ANTHEM = SOUND_EVENTS.register(
            "usst_anthem",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MagicEmblem.MODID, "usst_anthem")));
}
