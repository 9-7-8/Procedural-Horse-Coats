package com.example.horsegenetics.neoforge.worldgen;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Puts the cowboy's barn into <b>plains villages, at the edge</b>, by appending
 * it to {@code minecraft:village/plains/terminators}.
 *
 * <h2>Why the terminator pool</h2>
 * A terminator is what the village generator places when a street connector has
 * nowhere left to go - both {@code village/plains/streets} and
 * {@code village/plains/houses} name it as their {@code fallback}. So the
 * terminator slots are, by construction, the ends of the village: the last
 * piece on a road that ran out of room. Putting the barn there is the closest
 * the jigsaw system comes to saying "on the outskirts" without hand-placing it
 * and owning terrain fitting ourselves.
 *
 * <p>The barn's own connector (baked into {@code cowboy_barn.nbt}) is a
 * {@code minecraft:street} jigsaw on its west face at y=1, which is the
 * relationship the vanilla terminators have to a road: their jigsaw sits one
 * above their y=0 ground block, so the barn's floor lands level with the
 * street's surface.
 *
 * <h2>Why this is code and not a datapack file</h2>
 * A datapack can only <b>replace</b> {@code terminators.json}, never add to it.
 * Replacing it would freeze today's four vanilla terminators into this mod and
 * silently delete any other mod's additions. Appending needs the live pool
 * object, which is what the access transformer on
 * {@link StructureTemplatePool} is for.
 *
 * <p>{@code rawTemplates} is what the codec would re-serialise and what other
 * mods read; {@code templates} is the weight-expanded list the generator
 * actually draws from. Both have to move together, and {@code maxSize} is a
 * lazy cache of the tallest element, so it is reset in case something has
 * already asked.
 *
 * <h2>Frequency</h2>
 * One weight against the four vanilla terminators of weight 1 each, so roughly
 * one terminator slot in five. A village has a handful of them, and the barn is
 * 15x7 where a terminator is 2x3 - so most of the draws that pick it then fail
 * the bounding-box check and fall through to a small terminator instead. The
 * net effect is "most plains villages, where there was room". A village that
 * rolls two barns gets two barns; only the first grows a cowboy (see
 * {@code CowboyHandler}).
 */
@EventBusSubscriber
public final class BarnPoolInjector {

    /** The pool this mod owns, holding exactly the barn element. */
    private static final ResourceKey<StructureTemplatePool> BARN_POOL = ResourceKey.create(
            Registries.TEMPLATE_POOL, Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "cowboy_barn"));

    /** The vanilla pool it is appended to - the ends of plains-village streets. */
    private static final ResourceKey<StructureTemplatePool> PLAINS_TERMINATORS = ResourceKey.create(
            Registries.TEMPLATE_POOL, Identifier.withDefaultNamespace("village/plains/terminators"));

    private BarnPoolInjector() {
    }

    @SubscribeEvent
    static void onServerAboutToStart(ServerAboutToStartEvent event) {
        Registry<StructureTemplatePool> pools =
                event.getServer().registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL);

        StructureTemplatePool barn = pools.getValue(BARN_POOL);
        StructureTemplatePool terminators = pools.getValue(PLAINS_TERMINATORS);
        if (barn == null || terminators == null) {
            HorseGenetics.LOGGER.warn("Cowboy barn not added to plains villages: pool missing (barn={}, terminators={})",
                    barn != null, terminators != null);
            return;
        }

        // Idempotent: a single-player session that quits to title and loads
        // again reuses the same frozen pool objects, so a blind append would
        // stack a second copy of the barn every load and slowly crowd out the
        // vanilla terminators.
        List<Pair<StructurePoolElement, Integer>> existing = terminators.rawTemplates;
        List<Pair<StructurePoolElement, Integer>> additions = new ArrayList<>();
        for (Pair<StructurePoolElement, Integer> entry : barn.rawTemplates) {
            if (!existing.contains(entry)) {
                additions.add(entry);
            }
        }
        if (additions.isEmpty()) {
            return;
        }

        List<Pair<StructurePoolElement, Integer>> merged = new ArrayList<>(existing);
        merged.addAll(additions);
        terminators.rawTemplates = merged;
        for (Pair<StructurePoolElement, Integer> entry : additions) {
            for (int i = 0; i < entry.getSecond(); i++) {
                terminators.templates.add(entry.getFirst());
            }
        }
        terminators.maxSize = Integer.MIN_VALUE; // recomputed lazily, barn included

        HorseGenetics.LOGGER.info("Cowboy barn added to {} ({} of {} draws)",
                PLAINS_TERMINATORS.identifier(), additions.size(), terminators.size());
    }
}
