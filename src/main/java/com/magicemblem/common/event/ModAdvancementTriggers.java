package com.magicemblem.common.event;

import com.google.gson.JsonObject;
import com.magicemblem.MagicEmblem;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * 模组成就触发器
 * 
 * 提供以下成就触发：
 * - auth_success: 首次认证成功
 * - serious_violation: 首次获得严重违纪
 * - get_emblem: 获得魔法校徽
 * - night_owl: 熬夜获得早八
 */
public class ModAdvancementTriggers {

    public static final AuthSuccessTrigger AUTH_SUCCESS = new AuthSuccessTrigger();
    public static final SeriousViolationTrigger SERIOUS_VIOLATION = new SeriousViolationTrigger();
    public static final GetEmblemTrigger GET_EMBLEM = new GetEmblemTrigger();
    public static final NightOwlTrigger NIGHT_OWL = new NightOwlTrigger();
    public static final GuardLordTrigger GUARD_LORD = new GuardLordTrigger();

    /**
     * 注册所有触发器到Forge的CriteriaTriggers
     */
    public static void register() {
        try {
            net.minecraft.advancements.CriteriaTriggers.register(AUTH_SUCCESS);
            MagicEmblem.LOGGER.info("[MagicEmblem] Registered advancement trigger: auth_success");
            net.minecraft.advancements.CriteriaTriggers.register(SERIOUS_VIOLATION);
            MagicEmblem.LOGGER.info("[MagicEmblem] Registered advancement trigger: serious_violation");
            net.minecraft.advancements.CriteriaTriggers.register(GET_EMBLEM);
            MagicEmblem.LOGGER.info("[MagicEmblem] Registered advancement trigger: get_emblem");
            net.minecraft.advancements.CriteriaTriggers.register(NIGHT_OWL);
            MagicEmblem.LOGGER.info("[MagicEmblem] Registered advancement trigger: night_owl");
            net.minecraft.advancements.CriteriaTriggers.register(GUARD_LORD);
            MagicEmblem.LOGGER.info("[MagicEmblem] Registered advancement trigger: guard_lord");
        } catch (Exception e) {
            MagicEmblem.LOGGER.error("[MagicEmblem] FAILED to register advancement triggers!", e);
        }
    }

    // ===== 认证成功触发器 =====
    public static class AuthSuccessTrigger extends SimpleCriterionTrigger<AuthSuccessTrigger.Instance> {
        private static final ResourceLocation ID = new ResourceLocation(MagicEmblem.MODID, "auth_success");

        @Override
        protected Instance createInstance(JsonObject pJson, ContextAwarePredicate pPredicate,
                                           DeserializationContext pContext) {
            return new Instance(pPredicate);
        }

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        public void trigger(ServerPlayer player) {
            this.trigger(player, instance -> true);
        }

        public static class Instance extends AbstractCriterionTriggerInstance {
            public Instance(ContextAwarePredicate pPlayer) {
                super(ID, pPlayer);
            }
        }
    }

    // ===== 严重违纪触发器 =====
    public static class SeriousViolationTrigger extends SimpleCriterionTrigger<SeriousViolationTrigger.Instance> {
        private static final ResourceLocation ID = new ResourceLocation(MagicEmblem.MODID, "serious_violation");

        @Override
        protected Instance createInstance(JsonObject pJson, ContextAwarePredicate pPredicate,
                                           DeserializationContext pContext) {
            return new Instance(pPredicate);
        }

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        public void trigger(ServerPlayer player) {
            this.trigger(player, instance -> true);
        }

        public static class Instance extends AbstractCriterionTriggerInstance {
            public Instance(ContextAwarePredicate pPlayer) {
                super(ID, pPlayer);
            }
        }
    }

    // ===== 获得魔法校徽触发器 =====
    public static class GetEmblemTrigger extends SimpleCriterionTrigger<GetEmblemTrigger.Instance> {
        private static final ResourceLocation ID = new ResourceLocation(MagicEmblem.MODID, "get_emblem");

        @Override
        protected Instance createInstance(JsonObject pJson, ContextAwarePredicate pPredicate,
                                           DeserializationContext pContext) {
            return new Instance(pPredicate);
        }

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        public void trigger(ServerPlayer player) {
            this.trigger(player, instance -> true);
        }

        public static class Instance extends AbstractCriterionTriggerInstance {
            public Instance(ContextAwarePredicate pPlayer) {
                super(ID, pPlayer);
            }
        }
    }

    // ===== 熬夜成就触发器 =====
    public static class NightOwlTrigger extends SimpleCriterionTrigger<NightOwlTrigger.Instance> {
        private static final ResourceLocation ID = new ResourceLocation(MagicEmblem.MODID, "night_owl");

        @Override
        protected Instance createInstance(JsonObject pJson, ContextAwarePredicate pPredicate,
                                           DeserializationContext pContext) {
            return new Instance(pPredicate);
        }

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        public void trigger(ServerPlayer player) {
            this.trigger(player, instance -> true);
        }

        public static class Instance extends AbstractCriterionTriggerInstance {
            public Instance(ContextAwarePredicate pPlayer) {
                super(ID, pPlayer);
            }
        }
    }

    // ===== 你惹怒了保安大队触发器 =====
    public static class GuardLordTrigger extends SimpleCriterionTrigger<GuardLordTrigger.Instance> {
        private static final ResourceLocation ID = new ResourceLocation(MagicEmblem.MODID, "guard_lord");

        @Override
        protected Instance createInstance(JsonObject pJson, ContextAwarePredicate pPredicate,
                                           DeserializationContext pContext) {
            return new Instance(pPredicate);
        }

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        public void trigger(ServerPlayer player) {
            this.trigger(player, instance -> true);
        }

        public static class Instance extends AbstractCriterionTriggerInstance {
            public Instance(ContextAwarePredicate pPlayer) {
                super(ID, pPlayer);
            }
        }
    }
}
