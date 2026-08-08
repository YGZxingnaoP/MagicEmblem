package com.magicemblem.common.effect;

import com.magicemblem.init.ModEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

/**
 * "早八时间" 效果
 * 
 * 仅在玩家拥有"我是光荣的大学牲"时触发。
 * 仅在"绝赞worktime"之后生效（夜间→早晨过渡），游戏刻1000~6000生效。
 * 
 * 效果期间：
 * - 强制玩家保持游泳姿态（setForcedPose）
 * - 缓慢 (MOVEMENT_SLOWDOWN)
 * - 缓降 (SLOW_FALLING)
 * - 挖掘疲劳 (DIG_SLOWDOWN)
 * - 反胃 (CONFUSION)
 * - 海豚的恩惠 (DOLPHINS_GRACE)
 * 
 * 可被牛奶消除，时长无限。
 */
public class EarlyClassEffect extends MobEffect {

    public EarlyClassEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B4513); // 棕色
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
            // 早八时段：1000 ~ 6000
            if (timeOfDay >= 1000 && timeOfDay < 6000) {
                // 强制游泳姿态
                player.setForcedPose(Pose.SWIMMING);
                // 施加负面buff
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 200, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 200, 0, false, false));
            }
        }
    }

    /**
     * 效果移除时恢复默认姿态
     */
    @Override
    public void removeAttributeModifiers(LivingEntity pLivingEntity, net.minecraft.world.entity.ai.attributes.AttributeMap pAttributeMap, int pAmplifier) {
        super.removeAttributeModifiers(pLivingEntity, pAttributeMap, pAmplifier);
        if (pLivingEntity instanceof Player player) {
            player.setForcedPose(null);
        }
    }
}
