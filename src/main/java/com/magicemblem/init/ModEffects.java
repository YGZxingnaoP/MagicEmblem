package com.magicemblem.init;

import com.magicemblem.MagicEmblem;
import com.magicemblem.common.effect.EarlyClassEffect;
import com.magicemblem.common.effect.GloriousStudentEffect;
import com.magicemblem.common.effect.SeriousViolationEffect;
import com.magicemblem.common.effect.WorktimeEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 模组效果（Buff/DeBuff）注册表
 * 
 * 注册以下效果：
 * - glorious_student: 我是光荣的大学牲（不可被牛奶消除）
 * - serious_violation: 严重违纪（生成超级保安）
 * - worktime: 绝赞worktime（夜间增益）
 * - early_class: 早八时间（早晨负面）
 */
public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = 
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, MagicEmblem.MODID);

    // ===== 我是光荣的大学牲 =====
    // 认证后获得，不被牛奶消除，可配置重进存档后保留
    public static final RegistryObject<MobEffect> GLORIOUS_STUDENT = EFFECTS.register(
            "glorious_student", GloriousStudentEffect::new);

    // ===== 严重违纪 =====
    // 伤害村民/其他玩家时触发，每10秒生成超级保安
    public static final RegistryObject<MobEffect> SERIOUS_VIOLATION = EFFECTS.register(
            "serious_violation", SeriousViolationEffect::new);

    // ===== 绝赞worktime =====
    // 夜间(13800~1000)生效，给予急迫II、幸运、疾跑III、力量V
    public static final RegistryObject<MobEffect> WORKTIME = EFFECTS.register(
            "worktime", WorktimeEffect::new);

    // ===== 早八时间 =====
    // 早晨(1000~6000)生效，给予缓慢、缓降、挖掘疲劳、反胃
    public static final RegistryObject<MobEffect> EARLY_CLASS = EFFECTS.register(
            "early_class", EarlyClassEffect::new);
}
