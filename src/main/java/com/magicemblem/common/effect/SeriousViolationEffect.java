package com.magicemblem.common.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * "严重违纪" 效果
 * 
 * 仅在玩家拥有"我是光荣的大学牲"时触发。
 * 当玩家对村民或其他玩家造成伤害时获得。
 * 效果期间每10秒生成"超级保安"卫道士。
 * 
 * 卫道士特性：
 * - 力量255、疾跑V、抗性提升255
 * - 命名为"超级保安"
 * - 带有自定义NBT标签，生存时间仅1分钟
 * 
 * 可被牛奶消除，时长无限。
 */
public class SeriousViolationEffect extends MobEffect {

    public SeriousViolationEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF0000); // 红色
    }

    @Override
    public boolean isDurationEffectTick(int pDuration, int pAmplifier) {
        return true;
    }
}
