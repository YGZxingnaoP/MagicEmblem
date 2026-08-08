package com.magicemblem.school;

import com.magicemblem.MagicEmblem;
import com.magicemblem.init.ModSoundEvents;
import net.minecraft.sounds.SoundEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * 学校注册表
 *
 * 管理学校ID与对应资源的映射关系：
 * - schoolId → 校歌音效（SoundEvent）
 * - schoolId → 密码（由 {@link SchoolPasswordManager} 管理）
 *
 * 【扩展指南】添加新学校时：
 * 1. 在 {@link ModSoundEvents} 中注册新校歌音效
 * 2. 在 {@link #registerSchools()} 中调用 register() 注册映射
 * 3. 在 school_passwords.json 中添加对应密码
 */
public class SchoolRegistry {

    /** schoolId → 校歌音效 */
    private static final Map<String, SoundEvent> ANTHEMS = new HashMap<>();

    /**
     * 注册所有学校资源映射
     * 在 FMLCommonSetupEvent 中调用（此时 SoundEvent 已注册完成）
     */
    public static void registerSchools() {
        // USST 上海理工大学
        register("USST", ModSoundEvents.USST_ANTHEM.get());

        // 【扩展】在此添加更多学校：
        // register("PKU", ModSoundEvents.PKU_ANTHEM.get());
    }

    /**
     * 注册单个学校
     * @param schoolId 学校标识（如 "USST"）
     * @param anthem 校歌 SoundEvent
     */
    public static void register(String schoolId, SoundEvent anthem) {
        ANTHEMS.put(schoolId, anthem);
        MagicEmblem.LOGGER.info("[SchoolRegistry] Registered school: {}", schoolId);
    }

    /**
     * 获取学校校歌音效
     * @param schoolId 学校标识
     * @return 校歌 SoundEvent，未注册返回 null
     */
    public static SoundEvent getAnthem(String schoolId) {
        return ANTHEMS.get(schoolId);
    }

    /**
     * 检查学校是否已注册
     */
    public static boolean isRegistered(String schoolId) {
        return ANTHEMS.containsKey(schoolId);
    }
}
