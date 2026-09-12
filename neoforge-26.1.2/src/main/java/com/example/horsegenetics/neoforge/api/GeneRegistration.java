package com.example.horsegenetics.neoforge.api;

import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.neoforge.HorseGenetics;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;

/**
 * Fires {@link RegisterHorseGenesEvent}, then closes the registry.
 *
 * <h2>The one moment this can happen</h2>
 * A gene registration moves where every later gene sits in the genotype code,
 * so all of them have to be in before anything reads one. That rules out the
 * obvious hooks:
 *
 * <ul>
 *   <li><b>This mod's constructor</b> is too early - mod constructors run in
 *       dependency order, and a mod that has not been constructed yet cannot
 *       have added a listener.</li>
 *   <li><b>{@code FMLCommonSetupEvent}</b> is too late to be comfortable and is
 *       dispatched in parallel.</li>
 *   <li><b>{@code FMLConstructModEvent} itself</b> is per-mod and interleaved
 *       with construction, so firing from the handler body has the same problem
 *       as the constructor.</li>
 * </ul>
 *
 * <p>What works is that event's <b>deferred work queue</b>.
 * {@code ModLoader.constructMods} constructs every mod, dispatches
 * {@code FMLConstructModEvent} to each, and only then drains the queue on the
 * sync executor. So a task enqueued from the handler runs exactly once, single
 * threaded, after the last mod has been constructed and before any registry
 * event - which is the seam this needs.
 *
 * <p><b>Unverified against 26.1.2 in a running game.</b> The sequence above is
 * read off {@code ModLoader.constructMods} in the loader sources
 * ({@code fancymodloader 11.0.15}), and {@code ModLoader.postEvent} is public
 * and takes exactly this shape of event, but no third-party mod has actually
 * registered a gene through it yet. What to check is on
 * {@code wiki/verification.html}; the log line below is the evidence, and its
 * count should match the one {@link com.example.horsegenetics.neoforge.ModGeneSpecs}
 * prints just before it.
 */
public final class GeneRegistration {

    private GeneRegistration() {}

    /** Called from the mod constructor; the work itself is deferred. See the class note. */
    public static void listen(net.neoforged.bus.api.IEventBus modEventBus) {
        modEventBus.addListener(GeneRegistration::onConstruct);
    }

    private static void onConstruct(FMLConstructModEvent event) {
        event.enqueueWork(GeneRegistration::fire);
    }

    private static void fire() {
        int before = Genes.codeOrder().size();
        try {
            ModLoader.postEvent(new RegisterHorseGenesEvent());
        } catch (RuntimeException e) {
            // A third-party gene that throws must not take the game down with
            // it: the rest of the mod is fine, and the player gets a horse
            // world without that mod's locus rather than no world at all.
            HorseGenetics.LOGGER.error("[genes] a mod threw while registering genes", e);
        }
        int added = Genes.codeOrder().size() - before;
        Genes.freeze();
        HorseGenetics.LOGGER.info("[genes] {} gene(s) registered by other mods; the registry is"
                + " frozen at {} segments", added, Genes.codeOrder().size());
    }
}
