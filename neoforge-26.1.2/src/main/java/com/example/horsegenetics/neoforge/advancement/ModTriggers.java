package com.example.horsegenetics.neoforge.advancement;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * <b>This mod's criterion triggers.</b> There is one, and there is unlikely
 * ever to be a second - {@link ProgressTaskTrigger} says why.
 *
 * <p>The advancements it drives are not written by hand: they are baked off
 * {@code ProgressTask} by {@code neoforge-26.1.2/tools/bake-advancements.mjs}
 * into {@code data/horsegenetics/advancement/}. Change the enum, re-run the
 * bake.
 */
public final class ModTriggers {

    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, HorseGenetics.MOD_ID);

    public static final DeferredHolder<CriterionTrigger<?>, ProgressTaskTrigger> PROGRESS_TASK =
            TRIGGERS.register("progress_task", ProgressTaskTrigger::new);

    public static void register(IEventBus modEventBus) {
        TRIGGERS.register(modEventBus);
    }

    private ModTriggers() {
    }
}
