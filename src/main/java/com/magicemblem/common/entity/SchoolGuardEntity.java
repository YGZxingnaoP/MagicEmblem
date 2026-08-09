package com.magicemblem.common.entity;

import com.magicemblem.MagicEmblem;
import com.magicemblem.client.geo.GeoAnimationPlayer;
import com.magicemblem.client.geo.GeoBone;
import com.magicemblem.client.geo.GeoModel;
import com.magicemblem.client.geo.GeoModelParser;
import com.magicemblem.init.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 超级保安实体
 *
 * 基于 Vindicator 的自定义实体：
 * - 血量200，护甲20，攻击力8，攻击周期2秒
 * - 碰撞箱2格高
 * - 自定义 geo.json 模型渲染（逐面UV，与方块渲染逻辑相同）
 * - 动画：idle/walk/run/attack
 * - 中立生物：只攻击对其造成伤害的生物和带严重违纪buff的玩家
 * - 由严重违纪buff触发生成，距玩家5方块处生成
 * - 存在90秒后自动消失
 *
 * 速度设计：
 * - 基础移动速度 = 玩家行走速度 (0.25)
 * - 追逐目标时速度略快于玩家疾跑 (玩家疾跑≈0.28，保安追逐≈0.35)
 */
public class SchoolGuardEntity extends Vindicator {

    // ===== 客户端动画（仅客户端使用） =====
    private GeoModel cachedModel;
    private final GeoAnimationPlayer animationPlayer = new GeoAnimationPlayer();
    private boolean modelLoaded = false;
    private String lastAnimName = "";

    // ===== 资源路径 =====
    public static final ResourceLocation MODEL_LOCATION =
            new ResourceLocation(MagicEmblem.MODID, "geo/school_guard.geo.json");
    public static final ResourceLocation TEXTURE_LOCATION =
            new ResourceLocation(MagicEmblem.MODID, "textures/entity/school_guard/main_texture.png");
    public static final ResourceLocation ANIMATION_LOCATION =
            new ResourceLocation(MagicEmblem.MODID, "animations/school_guard.animation.json");

    // ===== 同步数据：是否正在攻击 =====
    private static final EntityDataAccessor<Boolean> ATTACKING =
            SynchedEntityData.defineId(SchoolGuardEntity.class, EntityDataSerializers.BOOLEAN);

    // ===== 同步数据：是否正在跑步 =====
    private static final EntityDataAccessor<Boolean> SPRINTING =
            SynchedEntityData.defineId(SchoolGuardEntity.class, EntityDataSerializers.BOOLEAN);

    // ===== 攻击计时器（服务端） =====
    /** 攻击冷却：2秒=40tick间隔 */
    private int attackCooldown = 0;
    /** 攻击动画展示计时器 */
    private int attackAnimTimer = 0;
    /** 攻击停顿计时器：攻击时原地停顿1.5秒（30tick）保证attack动画播完 */
    private int attackPauseTimer = 0;

    /** 追逐速度倍率（略快于玩家疾跑） */
    private static final double CHASE_SPEED_MODIFIER = 1.4;

    public SchoolGuardEntity(EntityType<? extends Vindicator> type, Level level) {
        super(type, level);
    }

    // ===== 属性注册 =====

    /**
     * 注册实体属性
     * 血量200，护甲20，攻击力8
     * 基础移动速度0.25（同玩家行走速度），追逐时由AI倍率加速
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Vindicator.createAttributes()
                .add(Attributes.MAX_HEALTH, 200.0)
                .add(Attributes.ARMOR, 20.0)
                .add(Attributes.ATTACK_DAMAGE, 14.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25); // 同玩家行走速度
    }

    // ===== 数据同步 =====

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ATTACKING, false);
        this.entityData.define(SPRINTING, false);
    }

    public void setAttacking(boolean attacking) {
        this.entityData.set(ATTACKING, attacking);
    }

    public boolean isAttacking() {
        return this.entityData.get(ATTACKING);
    }

    public void setSprintingAI(boolean sprinting) {
        this.entityData.set(SPRINTING, sprinting);
    }

    public boolean isSprintingAI() {
        return this.entityData.get(SPRINTING);
    }

    // ===== AI 目标注册 =====

    @Override
    protected void registerGoals() {
        // 基本移动目标
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SchoolGuardMeleeAttackGoal(this, CHASE_SPEED_MODIFIER, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0)); // 普通漫步=玩家行走速度
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        // 目标选择：
        // 1. 被攻击后反击（中立生物）
        // 2. 主动攻击拥有"严重违纪"buff的玩家
        // 不攻击村民、铁傀儡等任何其他生物
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 64, true, false,
                this::isValidTarget));
        // 32格内：穿墙也能发现严重违纪玩家（优先级低于视线内检测）
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 32, false, false,
                this::isValidTarget));
    }

    /**
     * 判断实体是否为有效攻击目标：
     * - 仅拥有"严重违纪"buff的玩家 → 主动攻击
     * - 其他所有生物不会被主动攻击
     */
    private boolean isValidTarget(LivingEntity entity) {
        if (entity instanceof Player player) {
            return player.hasEffect(ModEffects.SERIOUS_VIOLATION.get());
        }
        return false;
    }

    // ===== Tick 逻辑 =====

    @Override
    public void tick() {
        super.tick();

        // 服务端逻辑
        if (!this.level().isClientSide()) {
            // 90秒后自动消失
            CompoundTag tag = this.getPersistentData();
            long spawnTime = tag.getLong("spawn_time");
            if (spawnTime > 0) {
                long aliveTime = this.level().getGameTime() - spawnTime;
                if (aliveTime > 1800L) { // 90秒 = 1800 tick
                    this.discard();
                    return;
                }
            }

            // 攻击冷却计时
            if (attackCooldown > 0) {
                attackCooldown--;
            }
            // 攻击动画计时
            if (attackAnimTimer > 0) {
                attackAnimTimer--;
                if (attackAnimTimer == 0) {
                    setAttacking(false);
                }
            }
            // 攻击停顿计时（攻击时原地不动，保证动画播完）
            if (attackPauseTimer > 0) {
                attackPauseTimer--;
                // 停顿期间禁止移动
                this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
            }

            // 同步跑步状态给客户端（用于动画切换）
            LivingEntity target = this.getTarget();
            if (target != null && this.distanceToSqr(target) > 4.0 && attackPauseTimer <= 0) {
                // 追逐目标时跑步
                if (!this.isSprintingAI()) {
                    this.setSprintingAI(true);
                }
            } else {
                if (this.isSprintingAI()) {
                    this.setSprintingAI(false);
                }
            }
        }

        // 客户端：动画更新
        if (this.level().isClientSide()) {
            updateAnimation();
        }
    }

    // ===== 客户端动画 =====

    @OnlyIn(Dist.CLIENT)
    private void loadModel() {
        var rm = Minecraft.getInstance().getResourceManager();
        cachedModel = GeoModelParser.parse(rm, MODEL_LOCATION);
        animationPlayer.load(rm, ANIMATION_LOCATION);
        modelLoaded = true;
    }

    @OnlyIn(Dist.CLIENT)
    private void updateAnimation() {
        if (!modelLoaded) {
            loadModel();
        }
        if (cachedModel == null) return;

        // 选择动画
        String targetAnim;
        if (isAttacking()) {
            targetAnim = "attack";
        } else if (isSprintingAI() && isMovingClient()) {
            targetAnim = "run";
        } else if (isMovingClient()) {
            targetAnim = "walk";
        } else {
            targetAnim = "idle";
        }

        // 切换动画
        if (!targetAnim.equals(lastAnimName)) {
            boolean loop = !"attack".equals(targetAnim);
            animationPlayer.play(targetAnim, loop);
            lastAnimName = targetAnim;
        }

        // 推进动画
        animationPlayer.tick(1.0f / 20.0f);
        animationPlayer.apply(cachedModel);

        // 非攻击状态下：head 骨骼随视线转动
        if (!isAttacking()) {
            applyHeadTracking(cachedModel);
        }
    }

    /**
     * 判断实体是否在移动（客户端）
     */
    @OnlyIn(Dist.CLIENT)
    private boolean isMovingClient() {
        Vec3 motion = this.getDeltaMovement();
        return motion.x * motion.x + motion.z * motion.z > 0.001;
    }

    /**
     * 将实体头部朝向叠加到 head 骨骼上（非攻击状态下生效）
     */
    @OnlyIn(Dist.CLIENT)
    private void applyHeadTracking(GeoModel model) {
        GeoBone headBone = findBone(model.bones, "head");
        if (headBone == null) return;

        // 头部相对身体的偏航角和俯仰角
        float headYaw = this.yHeadRot - this.yBodyRot;
        float headPitch = this.getXRot();

        // 限制角度范围，防止头部旋转过度
        headYaw = Math.max(-70, Math.min(70, headYaw));
        headPitch = Math.max(-40, Math.min(40, headPitch));

        // 叠加到动画旋转上
        // headYaw>0(右转) → rotation[1]减 → PoseStack Y轴顺时针 → 头右转
        headBone.rotation[1] -= headYaw;
        headBone.rotation[0] -= headPitch;
    }

    /**
     * 递归查找指定名称的骨骼
     */
    @OnlyIn(Dist.CLIENT)
    private GeoBone findBone(java.util.List<GeoBone> bones, String name) {
        for (GeoBone bone : bones) {
            if (name.equals(bone.name)) return bone;
            GeoBone found = findBone(bone.children, name);
            if (found != null) return found;
        }
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    public GeoModel getModel() {
        if (!modelLoaded) loadModel();
        return cachedModel;
    }

    @OnlyIn(Dist.CLIENT)
    public ResourceLocation getTextureLocation() {
        return TEXTURE_LOCATION;
    }

    // ===== NBT =====

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("magicemblem_super_guard", true);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
    }

    // ===== 不掉落经验 =====

    @Override
    public int getExperienceReward() {
        return 0;
    }

    // ===== 自定义近战攻击 AI =====

    /**
     * 自定义近战攻击 AI
     *
     * 功能：
     * - 攻击周期2秒（40tick）
     * - 攻击时原地停顿1.5秒（30tick），保证attack动画播完
     * - 追逐目标时使用 CHASE_SPEED_MODIFIER 倍率（略快于玩家疾跑）
     */
    private static class SchoolGuardMeleeAttackGoal extends MeleeAttackGoal {
        private final SchoolGuardEntity guard;

        public SchoolGuardMeleeAttackGoal(SchoolGuardEntity guard, double speedModifier, boolean followingTargetEvenIfNotSeen) {
            super(guard, speedModifier, followingTargetEvenIfNotSeen);
            this.guard = guard;
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity enemy, double distToEnemySqr) {
            double reach = this.getAttackReachSqr(enemy);
            if (distToEnemySqr <= reach && guard.attackCooldown <= 0) {
                // 执行攻击
                guard.attackCooldown = 40; // 2秒 = 40 tick 攻击间隔
                guard.setAttacking(true);
                guard.attackAnimTimer = 30; // 1.5秒攻击动画展示
                guard.attackPauseTimer = 30; // 1.5秒原地停顿，保证动画播完
                guard.doHurtTarget(enemy);
            }
        }

        @Override
        public boolean canContinueToUse() {
            // 攻击停顿期间不允许移动，但保持AI活跃
            if (guard.attackPauseTimer > 0) {
                return true;
            }
            return super.canContinueToUse();
        }

        @Override
        public void tick() {
            // 攻击停顿期间不移动
            if (guard.attackPauseTimer > 0) {
                return;
            }
            super.tick();
        }

        @Override
        public void stop() {
            super.stop();
            guard.setAttacking(false);
            guard.attackAnimTimer = 0;
            guard.attackPauseTimer = 0;
        }
    }
}
