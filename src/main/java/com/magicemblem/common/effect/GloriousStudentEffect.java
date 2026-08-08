package com.magicemblem.common.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * "我是光荣的大学牲" 效果
 * 
 * 玩家完成身份认证后获得，持续存在直到下线刷新（可配置）。
 * 重要特性：不会被牛奶消除（通过事件监听器检测并恢复）。
 * 效果类型：增益（BENEFICIAL）
 * 时长：无限（Integer.MAX_VALUE）
 */
public class GloriousStudentEffect extends MobEffect {

    public GloriousStudentEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFD700); // 金色
        // 添加幸运属性加成（象征大学生的幸运）
        this.addAttributeModifier(Attributes.LUCK, 
                "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                1.0, AttributeModifier.Operation.ADDITION);
    }

    /**
     * 该效果应持续存在（由tick逻辑控制）
     */
    @Override
    public boolean isDurationEffectTick(int pDuration, int pAmplifier) {
        return true;
    }
}
