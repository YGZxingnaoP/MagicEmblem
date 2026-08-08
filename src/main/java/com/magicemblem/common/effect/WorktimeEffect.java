package com.magicemblem.common.effect;

import com.magicemblem.init.ModEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * "绝赞worktime" 效果
 * 
 * 仅在玩家拥有"我是光荣的大学牲"时触发。
 * 仅在游戏刻13800至第二天游戏刻1000时生效（夜间时段）。
 * 
 * 生效期间给予玩家：
 * - 急迫II (DIG_SPEED +1)
 * - 幸运 (LUCK)
 * - 疾跑III (MOVEMENT_SPEED +2)
 * - 力量V (DAMAGE_BOOST +4)
 * 
 * 可被牛奶消除，时长无限。
 */
public class WorktimeEffect extends MobEffect {

    public WorktimeEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x00FF88); // 绿色
    }

    @Override
    public boolean isDurationEffectTick(int pDuration, int pAmplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity pLivingEntity, int pAmplifier) {
        if (pLivingEntity instanceof Player player && !player.level().isClientSide()) {
            // 必须拥有"我是光荣的大学牲"才生效
            if (!player.hasEffect(ModEffects.GLORIOUS_STUDENT.get())) return;
            long timeOfDay = player.level().getDayTime() % 24000;
            // 夜间时段：13800 ~ 24000 和 0 ~ 1000
            if (timeOfDay >= 13800 || timeOfDay < 1000) {
                // 给予夜间增益buff
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 200, 1, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 200, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 2, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 4, false, false));
            }
        }
    }
}
