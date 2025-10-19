package com.sake.mobfriends.advancements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sake.mobfriends.MobFriends;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.CriterionValidator;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;

import java.util.Optional;


public class BecomeFriendlyTrigger extends SimpleCriterionTrigger<BecomeFriendlyTrigger.TriggerInstance> {


    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MobFriends.MOD_ID, "become_friendly_with_faction");


    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, String faction) {
        this.trigger(player, (triggerInstance) -> triggerInstance.matches(faction));
    }

    // 【核心移植】TriggerInstance 现在继承 SimpleCriterionTrigger.SimpleInstance
    public static class TriggerInstance implements SimpleCriterionTrigger.SimpleInstance {
        private final Optional<ContextAwarePredicate> player;
        private final Optional<String> faction;

        public TriggerInstance(Optional<ContextAwarePredicate> player, Optional<String> faction) {
            this.player = player;
            this.faction = faction;
        }

        public boolean matches(String faction) {
            // 如果 advancement json 中没有指定 faction，则匹配所有
            // 否则，检查传入的 faction 是否与 advancement 中指定的匹配
            return this.faction.isEmpty() || this.faction.get().equals(faction);
        }

        // 【核心移植】为 TriggerInstance 实现自己的 Codec
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                                ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                                Codec.STRING.optionalFieldOf("faction").forGetter(TriggerInstance::faction)
                        )
                        .apply(instance, TriggerInstance::new)
        );

        @Override
        public Optional<ContextAwarePredicate> player() {
            return this.player;
        }

        public Optional<String> faction() {
            return this.faction;
        }

        // 【核心移植】这个验证方法在新的API中也是需要的
        @Override
        public void validate(CriterionValidator validator) {
            SimpleInstance.super.validate(validator);
        }
    }
}