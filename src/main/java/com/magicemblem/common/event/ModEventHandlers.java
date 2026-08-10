package com.magicemblem.common.event;

import com.magicemblem.MagicEmblem;
import com.magicemblem.common.block.MagicEmblemBlock;
import com.magicemblem.common.blockentity.AbstractEmblemBlockEntity;
import com.magicemblem.common.entity.SchoolGuardEntity;
import com.magicemblem.init.ModEffects;
import com.magicemblem.init.ModEntities;
import com.magicemblem.init.ModItems;
import com.magicemblem.network.CameraAnimTriggerPacket;
import com.magicemblem.network.ModNetwork;
import com.magicemblem.network.PlayAnthemPacket;
import com.magicemblem.school.SchoolRegistry;
import com.magicemblem.score.PlayerScoreManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.ItemCraftedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.ItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

/**
 * 模组事件处理器
 * 
 * 处理以下事件：
 * 1. 牛奶消除保护（"我是光荣的大学牲"不被牛奶消除）
 * 2. 伤害检测（触发"严重违纪"效果）
 * 3. 玩家登录/登出（buff持久化）
 * 4. 玩家tick（触发"严重违纪"卫道士生成、超级保安过期清除、时间buff）
 * 5. 成就触发
 */
@Mod.EventBusSubscriber(modid = MagicEmblem.MODID)
public class ModEventHandlers {

    /**
     * 牛奶消除保护
     * - "我是光荣的大学牲"：不被牛奶消除，立刻加回
     * - "绝赞worktime"/"早八时间"：允许牛奶消除，但标记 milked_xxx 防止 tick 重新添加
     */
    @SubscribeEvent
    public static void onMilkEffect(MobEffectEvent.Remove event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.level().isClientSide()) {
            if (event.getEffectInstance() == null) return;
            var effect = event.getEffectInstance().getEffect();

            if (effect == ModEffects.GLORIOUS_STUDENT.get()) {
                // 大学牲buff不被牛奶消除，立刻加回
                player.getServer().execute(() -> {
                    player.addEffect(new MobEffectInstance(
                            ModEffects.GLORIOUS_STUDENT.get(), Integer.MAX_VALUE, 0, false, false));
                });
            } else if (effect == ModEffects.WORKTIME.get()) {
                // 牛奶消除了worktime，标记为已喝奶消除
                CompoundTag modData = getOrCreateModData(player);
                modData.putBoolean("milked_worktime", true);
                MagicEmblem.LOGGER.info("[MagicEmblem] Milk removed worktime for {}", player.getName().getString());
            } else if (effect == ModEffects.EARLY_CLASS.get()) {
                // 牛奶消除了early_class，标记为已喝奶消除
                CompoundTag modData = getOrCreateModData(player);
                modData.putBoolean("milked_early_class", true);
                MagicEmblem.LOGGER.info("[MagicEmblem] Milk removed early_class for {}", player.getName().getString());
            }
        }
    }

    /**
     * 模组buff门禁
     * 没有"我是光荣的大学牲"buff时，禁止其它模组buff生效（直接拦截不让添加）
     * 大学牲buff本身不受此限制
     */
    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            if (event.getEffectInstance() == null) return;
            var effect = event.getEffectInstance().getEffect();
            // 大学牲buff本身不受限制
            if (effect == ModEffects.GLORIOUS_STUDENT.get()) return;
            // 检查是否为本模组的buff
            if (effect == ModEffects.WORKTIME.get()
                    || effect == ModEffects.EARLY_CLASS.get()
                    || effect == ModEffects.SERIOUS_VIOLATION.get()) {
                // 没有大学牲buff → 拦截添加
                if (!player.hasEffect(ModEffects.GLORIOUS_STUDENT.get())) {
                    event.setCanceled(true);
                    MagicEmblem.LOGGER.info("[MagicEmblem] Blocked buff {} for {} (no glorious student)",
                            effect.getDescriptionId(), player.getName().getString());
                }
            }
        }
    }

    /**
     * 客户端玩家Tick事件（仅在客户端执行）
     * 处理：
     * 1. 早八时间：强制游泳姿态（必须在客户端设置）
     * 2. 大学牲buff保底：客户端级别防止buff消失
     */
    @SubscribeEvent
    public static void onClientPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !event.player.level().isClientSide()) return;
        Player player = event.player;

        // === 早八时间：强制游泳姿态（客户端级别） ===
        if (player.hasEffect(ModEffects.EARLY_CLASS.get())) {
            long timeOfDay = player.level().getDayTime() % 24000;
            if (timeOfDay >= 1000 && timeOfDay < 6000) {
                player.setForcedPose(Pose.SWIMMING);
            } else {
                // 非早八时段恢复默认姿态
                if (player.getForcedPose() == Pose.SWIMMING) {
                    player.setForcedPose(null);
                }
            }
        } else {
            // 没有早八效果时，如果还是游泳姿态，清除
            if (player.getForcedPose() == Pose.SWIMMING) {
                player.setForcedPose(null);
            }
        }
    }

    /**
     * 玩家合成物品事件
     * 合成魔法校徽时触发"这不是校徽吗"成就
     */
    @SubscribeEvent
    public static void onItemCrafted(ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (event.getCrafting().is(ModItems.MAGIC_EMBLEM.get())) {
                ModAdvancementTriggers.GET_EMBLEM.trigger(player);
            }
        }
    }

    /**
     * 玩家捡起物品事件
     * 捡起魔法校徽时也触发"这不是校徽吗"成就（覆盖掉落捡起等场景）
     */
    @SubscribeEvent
    public static void onItemPickup(ItemPickupEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (event.getStack().is(ModItems.MAGIC_EMBLEM.get())) {
                ModAdvancementTriggers.GET_EMBLEM.trigger(player);
            }
        }
    }

    /**
     * 玩家伤害事件
     * 当拥有"大学牲"buff的玩家伤害村民或其他玩家时，触发"严重违纪"
     */
    @SubscribeEvent
    public static void onPlayerDamage(LivingDamageEvent event) {
        Entity source = event.getSource().getEntity();
        if (source instanceof ServerPlayer attacker && !attacker.level().isClientSide()) {
            boolean hasGlorious = attacker.hasEffect(ModEffects.GLORIOUS_STUDENT.get());
            Entity target = event.getEntity();
            boolean validTarget = target instanceof AbstractVillager || (target instanceof Player && target != attacker);
            boolean hasSeriousViolation = attacker.hasEffect(ModEffects.SERIOUS_VIOLATION.get());

            MagicEmblem.LOGGER.info("[MagicEmblem] Damage event: attacker={}, target={}, hasGlorious={}, validTarget={}, hasSeriousViolation={}",
                    attacker.getName().getString(),
                    target != null ? target.getName().getString() : "null",
                    hasGlorious, validTarget, hasSeriousViolation);

            // 检查攻击者是否有"大学牲"buff
            if (hasGlorious && validTarget && !hasSeriousViolation) {
                MagicEmblem.LOGGER.info("[MagicEmblem] Player {} triggered serious violation by attacking {}",
                        attacker.getName().getString(), target.getName().getString());
                boolean added = attacker.addEffect(new MobEffectInstance(
                        ModEffects.SERIOUS_VIOLATION.get(), Integer.MAX_VALUE, 0, false, true));
                MagicEmblem.LOGGER.info("[MagicEmblem] Serious violation effect added: {}, now hasEffect={}",
                        added, attacker.hasEffect(ModEffects.SERIOUS_VIOLATION.get()));

                // 记录严重违纪时间戳（用于 10 秒延迟生成卫道士）
                CompoundTag modData = getOrCreateModData(attacker);
                modData.putLong("serious_violation_time", attacker.level().getGameTime());

                // 发送校歌播放包（如果该玩家有对应学校）
                if (SchoolRegistry.isRegistered("USST")) {
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> attacker),
                            new PlayAnthemPacket("USST"));
                }

                // 触发成就
                onSeriousViolation(attacker);
            }
        }
    }

    /**
     * 玩家tick事件
     * 处理"严重违纪"卫道士生成、超级保安清除和时间相关的buff
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) return;
        ServerPlayer player = (ServerPlayer) event.player;

        // ===== 大学牡buff保底：每20tick检查一次 =====
        // 无论牛奶还是任何方式移除，都立刻加回
        // 条件仅检查 authenticated（配置仅控制下线后是否保留，不影响在线保底）
        if (player.tickCount % 20 == 0) {
            CompoundTag pdata = player.getPersistentData();
            boolean authenticated = pdata.contains("magicemblem")
                    && pdata.getCompound("magicemblem").getBoolean("authenticated");
            // 只要认证过，在线期间就必须有buff
            if (authenticated && !player.hasEffect(ModEffects.GLORIOUS_STUDENT.get())) {
                MagicEmblem.LOGGER.info("[MagicEmblem] Re-applying glorious student buff for {}", player.getName().getString());
                player.addEffect(new MobEffectInstance(
                        ModEffects.GLORIOUS_STUDENT.get(), Integer.MAX_VALUE, 0, false, false));
            }
        }

        // ===== 严重违纪：10秒延迟首次生成超级保安，之后每2分钟生成一次 =====
        // 等级制度：I→1名  II→3名  III→5名
        if (player.tickCount % 20 == 0) {
            boolean hasSeriousViolation = player.hasEffect(ModEffects.SERIOUS_VIOLATION.get());
            if (hasSeriousViolation) {
                CompoundTag modData = getOrCreateModData(player);
                long svTime = modData.getLong("serious_violation_time");
                long lastGuardSpawn = modData.getLong("last_guard_spawn_time");
                long currentTime = player.level().getGameTime();
                int violLevel = getViolationLevel(player);

                if (svTime > 0) {
                    // 首次：10秒（200tick）延迟后生成
                    if (lastGuardSpawn == 0 && currentTime - svTime >= 200) {
                        if (!hasNearbySchoolGuard(player)) {
                            MagicEmblem.LOGGER.info("[MagicEmblem] Spawning school guard (first, level={}) for player {}",
                                    violLevel, player.getName().getString());
                            spawnSuperGuard(player, violLevel);
                            modData.putLong("last_guard_spawn_time", currentTime);
                            // II级记录90秒窗口起始（与保安寿命对齐），用于追踪击杀升级
                            if (violLevel == 1) {
                                modData.putLong("guard_window_start", currentTime);
                                modData.putInt("guard_kill_count", 0);
                            }
                        }
                    }
                    // 后续：每2分钟（2400tick）生成一次
                    else if (lastGuardSpawn > 0 && currentTime - lastGuardSpawn >= 2400) {
                        if (!hasNearbySchoolGuard(player)) {
                            MagicEmblem.LOGGER.info("[MagicEmblem] Spawning school guard (periodic, level={}) for player {}",
                                    violLevel, player.getName().getString());
                            spawnSuperGuard(player, violLevel);
                            modData.putLong("last_guard_spawn_time", currentTime);
                            if (violLevel == 1) {
                                modData.putLong("guard_window_start", currentTime);
                                modData.putInt("guard_kill_count", 0);
                            }
                        }
                    }
                }
            } else {
                // 没有严重违纪时清除时间戳
                CompoundTag modData = getOrCreateModData(player);
                if (modData.getLong("serious_violation_time") != 0) {
                    modData.putLong("serious_violation_time", 0);
                }
                if (modData.getLong("last_guard_spawn_time") != 0) {
                    modData.putLong("last_guard_spawn_time", 0);
                }
            }
        }

        // ===== 积分系统 =====
        PlayerScoreManager.tick(player);

        // ===== 绝赞worktime和早八时间的触发 =====
        if (player.hasEffect(ModEffects.GLORIOUS_STUDENT.get())) {
            long timeOfDay = player.level().getDayTime() % 24000;
        
            CompoundTag modData = getOrCreateModData(player);
            boolean milkedWorktime = modData.getBoolean("milked_worktime");
            boolean milkedEarlyClass = modData.getBoolean("milked_early_class");
        
            // 夜间时段（13800~24000 和 0~1000）：给予绝赞worktime
            boolean isNight = timeOfDay >= 13800 || timeOfDay < 1000;
            boolean hadWorktime = player.hasEffect(ModEffects.WORKTIME.get());
        
            if (isNight && !hadWorktime && !milkedWorktime) {
                player.addEffect(new MobEffectInstance(
                        ModEffects.WORKTIME.get(), Integer.MAX_VALUE, 0, false, false));
            } else if (!isNight && hadWorktime) {
                player.removeEffect(ModEffects.WORKTIME.get());
            }
        
            // 早八时段（1000~6000）：给予早八时间
            boolean isEarlyMorning = timeOfDay >= 1000 && timeOfDay < 6000;
            boolean hadEarlyClass = player.hasEffect(ModEffects.EARLY_CLASS.get());
            if (isEarlyMorning && !hadEarlyClass && !milkedEarlyClass && hadWorktime) {
                player.addEffect(new MobEffectInstance(
                        ModEffects.EARLY_CLASS.get(), Integer.MAX_VALUE, 0, false, false));
                onNightOwl(player);
            } else if (!isEarlyMorning && hadEarlyClass) {
                player.removeEffect(ModEffects.EARLY_CLASS.get());
            }
        }
    }

    /**
     * 保安死亡事件 — 严重违纪升级检测
     * I→II：击杀任意一名保安立即升级
     * II→III：90秒窗口内击杀两名保安
     * III：最高级，不再升级
     */
    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof SchoolGuardEntity)) return;
        if (event.getEntity().level().isClientSide()) return;

        Entity killer = event.getSource().getEntity();
        if (!(killer instanceof ServerPlayer player)) return;
        if (!player.hasEffect(ModEffects.SERIOUS_VIOLATION.get())) return;

        CompoundTag modData = getOrCreateModData(player);
        int currentLevel = getViolationLevel(player);
        long now = player.level().getGameTime();

        if (currentLevel == 0) {
            // I → II：击杀任意保安立即升级
            MagicEmblem.LOGGER.info("[MagicEmblem] Player {} killed guard, upgrading to Serious Violation II",
                    player.getName().getString());
            player.removeEffect(ModEffects.SERIOUS_VIOLATION.get());
            player.addEffect(new MobEffectInstance(
                    ModEffects.SERIOUS_VIOLATION.get(), Integer.MAX_VALUE, 1, false, true));
            modData.putLong("serious_violation_time", now);
            modData.putLong("last_guard_spawn_time", 0);
            modData.putInt("guard_kill_count", 0);
        }
        else if (currentLevel == 1) {
            // II → III：90秒窗口内击杀2名保安
            long windowStart = modData.getLong("guard_window_start");
            // 如果窗口已过期，重置
            if (windowStart == 0 || now - windowStart > 1800L) {
                windowStart = now;
                modData.putLong("guard_window_start", windowStart);
                modData.putInt("guard_kill_count", 1);
                MagicEmblem.LOGGER.info("[MagicEmblem] Player {} killed guard (1/2) in new 90s window",
                        player.getName().getString());
            } else {
                int kills = modData.getInt("guard_kill_count") + 1;
                modData.putInt("guard_kill_count", kills);
                MagicEmblem.LOGGER.info("[MagicEmblem] Player {} killed guard ({}/2) in 90s window",
                        player.getName().getString(), kills);
                if (kills >= 2) {
                    // II → III
                    MagicEmblem.LOGGER.info("[MagicEmblem] Player {} upgraded to Serious Violation III!",
                            player.getName().getString());
                    player.removeEffect(ModEffects.SERIOUS_VIOLATION.get());
                    player.addEffect(new MobEffectInstance(
                            ModEffects.SERIOUS_VIOLATION.get(), Integer.MAX_VALUE, 2, false, true));
                    modData.putLong("serious_violation_time", now);
                    modData.putLong("last_guard_spawn_time", 0);
                    modData.putInt("guard_kill_count", 0);
                    // 触发"你惹怒了保安大队"成就
                    ModAdvancementTriggers.GUARD_LORD.trigger(player);
                }
            }
        }
        // currentLevel == 2 (III): 最高级，不升级
    }

    /**
     * 玩家登录事件
     * 上线时清除所有模组buff（保底），认证状态不保留，需重新认证
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 上线清除所有模组buff
            clearAllModBuffs(player);
            MagicEmblem.LOGGER.info("[MagicEmblem] Cleared all mod buffs on login for {}",
                    player.getName().getString());
        }
    }

    /**
     * 玩家登出事件
     * 清除所有模组buff + 清除认证状态（每次上线需重新认证）
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 清除所有模组buff
            clearAllModBuffs(player);
            // 清除认证状态
            CompoundTag data = player.getPersistentData();
            if (data.contains("magicemblem")) {
                data.getCompound("magicemblem").putBoolean("authenticated", false);
            }
            // 玩家登出时保存积分
            PlayerScoreManager.saveScores();
            MagicEmblem.LOGGER.info("[MagicEmblem] Cleared all mod buffs and auth on logout for {}",
                    player.getName().getString());
        }
    }

    // ===== 辅助方法 =====

    /**
     * 检查玩家附近是否已有超级保安（64格范围内）
     */
    private static boolean hasNearbySchoolGuard(ServerPlayer player) {
        List<SchoolGuardEntity> guards = player.level().getEntitiesOfClass(SchoolGuardEntity.class,
                player.getBoundingBox().inflate(64));
        return !guards.isEmpty();
    }

    /**
     * 生成超级保安（按等级决定数量：I→1名 II→3名 III→5名）
     * 在距玩家5方块处随机角度生成
     * 自带速度III和力量V，无抗性提升
     * 存在90秒后自动消失（由实体自身tick处理）
     */
    private static void spawnSuperGuard(ServerPlayer player, int violLevel) {
        Level level = player.level();
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            MagicEmblem.LOGGER.error("[MagicEmblem] Cannot spawn school guard: not a ServerLevel");
            return;
        }
        int count;
        switch (violLevel) {
            case 1: count = 3; break;
            case 2: count = 5; break;
            default: count = 1; break;
        }
        MagicEmblem.LOGGER.info("[MagicEmblem] spawnSuperGuard: level={}, count={}, player=({}, {}, {})",
                violLevel, count, player.getBlockX(), player.getBlockY(), player.getBlockZ());
        try {
            // 以玩家为中心的均匀分布角度
            double baseAngle = Math.random() * Math.PI * 2;
            for (int i = 0; i < count; i++) {
                double angle = baseAngle + (2.0 * Math.PI * i / count) + (Math.random() - 0.5) * 0.6;
                double spawnX = player.getX() + Math.cos(angle) * 5.0;
                double spawnZ = player.getZ() + Math.sin(angle) * 5.0;
                double spawnY = player.getY();

                SchoolGuardEntity guard = new SchoolGuardEntity(ModEntities.SCHOOL_GUARD.get(), serverLevel);
                guard.setPos(spawnX, spawnY, spawnZ);

                float facingYaw = (float) Math.toDegrees(Math.atan2(spawnX - player.getX(), player.getZ() - spawnZ));
                guard.setYRot(facingYaw);
                guard.yBodyRot = facingYaw;
                guard.yHeadRot = facingYaw;

                guard.setCustomName(Component.literal("超级保安"));
                guard.setCustomNameVisible(true);

                guard.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
                        Integer.MAX_VALUE, 2, false, false));
                guard.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,
                        Integer.MAX_VALUE, 4, false, false));

                CompoundTag tag = guard.getPersistentData();
                tag.putBoolean("magicemblem_super_guard", true);
                tag.putLong("spawn_time", serverLevel.getGameTime());

                guard.setPersistenceRequired();
                guard.setHealth(200.0f);

                boolean added = serverLevel.addFreshEntity(guard);
                if (!added) {
                    MagicEmblem.LOGGER.error("[MagicEmblem] FAILED to spawn school guard #{}/{}", i + 1, count);
                }
            }
        } catch (Exception e) {
            MagicEmblem.LOGGER.error("[MagicEmblem] EXCEPTION in spawnSuperGuard", e);
        }
    }

    /**
     * 获取玩家严重违纪等级（0=I, 1=II, 2=III）
     */
    private static int getViolationLevel(ServerPlayer player) {
        MobEffectInstance instance = player.getEffect(ModEffects.SERIOUS_VIOLATION.get());
        return instance != null ? instance.getAmplifier() : 0;
    }

    /**
     * 认证成功回调
     */
    public static void onAuthenticationSuccess(ServerPlayer player) {
        // 保存认证状态
        CompoundTag modData = getOrCreateModData(player);
        modData.putBoolean("authenticated", true);
        // 重新认证：清除牛奶消除标记，附属buff重新开始时间判断
        modData.putBoolean("milked_worktime", false);
        modData.putBoolean("milked_early_class", false);

        // 触发认证成就
        ModAdvancementTriggers.AUTH_SUCCESS.trigger(player);
    }

    /**
     * 严重违纪成就触发
     */
    private static void onSeriousViolation(ServerPlayer player) {
        ModAdvancementTriggers.SERIOUS_VIOLATION.trigger(player);
    }

    /**
     * 熬夜成就触发
     */
    private static void onNightOwl(ServerPlayer player) {
        ModAdvancementTriggers.NIGHT_OWL.trigger(player);
    }

    /**
     * 清除玩家身上所有模组buff
     */
    private static void clearAllModBuffs(ServerPlayer player) {
        if (player.hasEffect(ModEffects.GLORIOUS_STUDENT.get()))
            player.removeEffect(ModEffects.GLORIOUS_STUDENT.get());
        if (player.hasEffect(ModEffects.WORKTIME.get()))
            player.removeEffect(ModEffects.WORKTIME.get());
        if (player.hasEffect(ModEffects.EARLY_CLASS.get()))
            player.removeEffect(ModEffects.EARLY_CLASS.get());
        if (player.hasEffect(ModEffects.SERIOUS_VIOLATION.get()))
            player.removeEffect(ModEffects.SERIOUS_VIOLATION.get());
    }

    /**
     * 获取或创建玩家的模组数据标签
     */
    private static CompoundTag getOrCreateModData(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains("magicemblem")) {
            data.put("magicemblem", new CompoundTag());
        }
        return data.getCompound("magicemblem");
    }
}
