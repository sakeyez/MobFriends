package com.sake.mobfriends.advancements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import java.util.Optional;

public class BecomeFriendlyTrigger extends SimpleCriterionTrigger<BecomeFriendlyTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, String factionName) {
        this.trigger(player, (triggerInstance) -> triggerInstance.matches(factionName));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player,
                                  Optional<String> faction) implements SimpleCriterionTrigger.SimpleInstance {

        // 【核心移植】根据教程文档 version-1.21.1.zip/resources/server/advancements.md
        // 将旧的 Codec.optionalField(codec, name) API 修改为新的 codec.optionalFieldOf(name)
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                        Codec.STRING.optionalFieldOf("faction").forGetter(TriggerInstance::faction)
                ).apply(instance, TriggerInstance::new)
        );

        public boolean matches(String factionName) {
            // 如果faction字段不存在，则匹配所有情况；如果存在，则要求传入的factionName与之相等
            return this.faction.isEmpty() || this.faction.get().equals(factionName);
        }
    }
}