package com.magicemblem.common.event;

import com.magicemblem.MagicEmblem;
import com.magicemblem.common.block.MagicEmblemBlock;
import com.magicemblem.common.blockentity.AbstractEmblemBlockEntity;
import com.magicemblem.init.ModEffects;
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
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.ItemCraftedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.ItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

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

        // ===== 严重违纪：10秒延迟生成超级保安（防止跳脸） =====
        if (player.tickCount % 20 == 0) {
            boolean hasSeriousViolation = player.hasEffect(ModEffects.SERIOUS_VIOLATION.get());
            if (hasSeriousViolation) {
                CompoundTag modData = getOrCreateModData(player);
                long svTime = modData.getLong("serious_violation_time");
                long currentTime = player.level().getGameTime();
                // 10秒（200tick）延迟后生成卫道士
                if (svTime > 0 && currentTime - svTime >= 200) {
                    MagicEmblem.LOGGER.info("[MagicEmblem] Spawning super guard for player {} at ({}, {}, {})",
                            player.getName().getString(),
                            player.getBlockX(), player.getBlockY(), player.getBlockZ());
                    spawnSuperGuard(player);
                    // 重置时间戳，防止重复生成（下次需要重新获得严重违纪才会再生成）
                    modData.putLong("serious_violation_time", 0);
                }
            } else {
                // 没有严重违纪时清除时间戳
                CompoundTag modData = getOrCreateModData(player);
                if (modData.getLong("serious_violation_time") != 0) {
                    modData.putLong("serious_violation_time", 0);
                }
            }
        }

        // ===== 超级保安120秒后消失（15秒保护期，防止刚生成就被删除） =====
        if (player.tickCount % 100 == 0) { // 每5秒检查一次
            long currentTime = player.level().getGameTime();
            player.level().getEntitiesOfClass(Vindicator.class,
                    player.getBoundingBox().inflate(64),
                    v -> {
                        CompoundTag tag = v.getPersistentData();
                        if (!tag.getBoolean("magicemblem_super_guard")) return false;
                        long aliveTime = currentTime - tag.getLong("spawn_time");
                        // 15秒保护期（300tick）内不删除
                        if (aliveTime < 300L) return false;
                        // 120秒（2400tick）后删除
                        return aliveTime > 2400L;
                    }).forEach(Entity::discard);
        }

        // ===== 首次放置检测：服务端检查附近是否有firstPlace方块 =====
        // 玩家生涯中仅触发一次（玩家级别flag）
        if (player.tickCount % 5 == 0) {
            CompoundTag pdata = player.getPersistentData();
            if (!pdata.getBoolean("saw_camera_anim")) {
                // 玩家还没看过运镜，检查附近是否有firstPlace方块
                BlockPos playerPos = player.blockPosition();
                BlockPos.MutableBlockPos searchPos = new BlockPos.MutableBlockPos();
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dy = -3; dy <= 3; dy++) {
                        for (int dz = -3; dz <= 3; dz++) {
                            searchPos.set(playerPos.getX() + dx, playerPos.getY() + dy, playerPos.getZ() + dz);
                            if (player.level().getBlockEntity(searchPos) instanceof AbstractEmblemBlockEntity be
                                    && be.isFirstPlace()) {
                                // 找到firstPlace方块！发送运镜触发包
                                BlockPos targetPos = searchPos.immutable();
                                MagicEmblem.LOGGER.info("[MagicEmblem] First place detected for player {} at {}, triggering camera anim",
                                        player.getName().getString(), targetPos);
                                // 标记玩家已看过
                                pdata.putBoolean("saw_camera_anim", true);
                                // 清除方块的firstPlace标记
                                be.setFirstPlace(false);
                                // 发送运镜触发包到客户端
                                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                                        new CameraAnimTriggerPacket(targetPos));
                                break;
                            }
                        }
                    }
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
     * 生成超级保安（卫道士）
     * 力量255、疾跑V、抗性提升255，命名"超级保安"，2分钟后消失
     * 
     * 关键：不调用 finalizeSpawn()，它会设置 PersistenceRequired=false
     * 导致和平难度下敌对生物被游戏引擎立即清除
     */
    private static void spawnSuperGuard(ServerPlayer player) {
        Level level = player.level();
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            MagicEmblem.LOGGER.error("[MagicEmblem] Cannot spawn super guard: not a ServerLevel");
            return;
        }
        try {
            double spawnX = player.getX() + Math.sin(Math.toRadians(player.getYRot())) * 3.0;
            double spawnZ = player.getZ() - Math.cos(Math.toRadians(player.getYRot())) * 3.0;
            double spawnY = player.getY();

            MagicEmblem.LOGGER.info("[MagicEmblem] spawnSuperGuard START: player=({}, {}, {}), spawn=({}, {}, {}), difficulty={}",
                    player.getBlockX(), player.getBlockY(), player.getBlockZ(),
                    spawnX, spawnY, spawnZ, serverLevel.getDifficulty());

            // Step 1: 直接构造实体（不用 NBT load 或 finalizeSpawn）
            Vindicator vindicator = new Vindicator(
                    net.minecraft.world.entity.EntityType.VINDICATOR, serverLevel);
            vindicator.setPos(spawnX, spawnY, spawnZ);
            vindicator.setCustomName(Component.literal("超级保安"));
            vindicator.setCustomNameVisible(true);

            // Step 2: 超级buff
            vindicator.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,
                    Integer.MAX_VALUE, 254, false, false));
            vindicator.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
                    Integer.MAX_VALUE, 4, false, false));
            vindicator.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
                    Integer.MAX_VALUE, 254, false, false));

            // Step 3: 标记2分钟清理计时器
            CompoundTag tag = vindicator.getPersistentData();
            tag.putBoolean("magicemblem_super_guard", true);
            tag.putLong("spawn_time", serverLevel.getGameTime());

            // Step 4: 持久化（必须最后设置！finalizeSpawn会覆盖为false）
            vindicator.setPersistenceRequired();

            // Step 5: 加入世界
            boolean added = serverLevel.addFreshEntity(vindicator);
            MagicEmblem.LOGGER.info("[MagicEmblem] addFreshEntity: result={}, id={}, alive={}, removed={}, persistent={}, pos=({}, {}, {})",
                    added, vindicator.getId(), vindicator.isAlive(), vindicator.isRemoved(),
                    vindicator.isPersistenceRequired(),
                    vindicator.getX(), vindicator.getY(), vindicator.getZ());

            if (!added) {
                MagicEmblem.LOGGER.error("[MagicEmblem] FAILED: addFreshEntity=false! difficulty={}, peaceful={}",
                        serverLevel.getDifficulty(),
                        serverLevel.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL);
            }
        } catch (Exception e) {
            MagicEmblem.LOGGER.error("[MagicEmblem] EXCEPTION in spawnSuperGuard", e);
        }
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
