package com.example.horsegenetics.neoforge.advancement;

import com.example.horsegenetics.common.progress.ProgressTask;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * <b>The one criterion every one of this mod's advancements is built on.</b>
 * It fires once, for one player, the moment a {@link ProgressTask} is ticked
 * off - see {@code HorseProgressData.complete}.
 *
 * <h2>Why one trigger and not a hundred</h2>
 * Every task already has an id and already has a single choke point that
 * completes it. A trigger per task would be a hundred registry entries and a
 * hundred classes saying the same thing; instead the <i>condition</i> names
 * which task, and the advancement JSON carries that name. Adding a task stays
 * what it has always been - a line in the enum and a hook - plus a re-bake.
 *
 * <h2>Three shapes of condition</h2>
 * <ul>
 *   <li>{@code {"task": "tame_mare"}} - that one task. Every leaf advancement.</li>
 *   <li>{@code {"chapter": "wild"}} - anything at all in that chapter. The eight
 *       chapter nodes, so a chapter lights up as soon as you have done one
 *       thing in it rather than all of it.</li>
 *   <li>{@code {}} - any task whatever. The tab's root.</li>
 * </ul>
 * Both fields are matched only when present, so the three are one codec and
 * the empty condition is the general case rather than a special one.
 */
public class ProgressTaskTrigger extends SimpleCriterionTrigger<ProgressTaskTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    /** The task {@code player} has just ticked off for the first time. */
    public void trigger(ServerPlayer player, ProgressTask task) {
        this.trigger(player, instance -> instance.matches(task));
    }

    /** The id a chapter is written under in the JSON - the {@link ProgressTask.Group} name, lower case. */
    public static String chapterId(ProgressTask.Group group) {
        return group.name().toLowerCase(java.util.Locale.ROOT);
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player,
                                  Optional<String> task,
                                  Optional<String> chapter)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(i -> i.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                Codec.STRING.optionalFieldOf("task").forGetter(TriggerInstance::task),
                Codec.STRING.optionalFieldOf("chapter").forGetter(TriggerInstance::chapter)
        ).apply(i, TriggerInstance::new));

        boolean matches(ProgressTask done) {
            if (task.isPresent() && !task.get().equals(done.id())) {
                return false;
            }
            return chapter.isEmpty() || chapter.get().equals(chapterId(done.group()));
        }
    }
}
