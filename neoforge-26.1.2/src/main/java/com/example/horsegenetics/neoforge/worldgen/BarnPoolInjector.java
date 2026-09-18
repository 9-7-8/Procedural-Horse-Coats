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
 * <p>The barn's own connectors (baked into {@code cowboy_barn.nbt}) are
 * <b>three</b> {@code minecraft:street} jigsaws, all at <b>y=0</b> - its own
 * foundation course. A jigsaw pair lands both blocks at the same world height,
 * and the street connector it meets is one above the road block, so the layer
 * carrying the jigsaw is the layer that rests on the ground. That is the
 * relationship vanilla <i>houses</i> have to a street, and it is what puts the
 * barn one block proud of the road with steps up to its doors. See
 * {@code tools/barn/bake-barn.py} for what it looked like when this was y=1.
 *
 * <p><b>Three rather than one, because one fixed the piece's rotation.</b> The
 * generator turns a candidate until its connector faces back at the street, and
 * with a single connector exactly one of the four rotations does that - so the
 * homestead always landed the same way round, 16 blocks out from the road with
 * 2 of its width on one side of the road's line and 15 on the other. If those
 * 15 blocks held a village house the piece was thrown out and the slot fell
 * through to a vanilla terminator, with no second arrangement to try. The three
 * connectors run down the walk - head-on at the barn's north doors, and one at
 * each end of the path - so three rotations attach and the generator keeps
 * whichever fits the ground it has, arriving at the walk either way.
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
 * <h2>The weight is not the only lever any more</h2>
 * It was, while the piece had one connector. The three connectors it carries
 * now are the other one, and they are the cheaper of the two: weight buys more
 * <i>draws</i> of a piece that may not fit, orientation buys more <i>ways for
 * the same draw to fit</i>. Reach for a bake change before reaching for the
 * number here - and before either, run the census, because the last time this
 * was reasoned about rather than counted the reasoning was out by a factor of
 * five.
 *
 * <h2>Frequency - measured, not reasoned</h2>
 * Weight 3 against the four vanilla terminators of weight 1 each, so roughly
 * three terminator slots in seven, and a village has several slots.
 *
 * <p>Everything this paragraph used to say after that was wrong, and wrong in
 * the pessimistic direction: it argued that a piece the better part of a chunk
 * square, squeezed into a slot vanilla sizes for a 2x3 stub, must fail the
 * bounding-box check on most of the draws that pick it, and that the homestead
 * was therefore in danger of being rare or gone. <b>It never was.</b> Counted
 * over 265 plains villages (see {@code gametest/ModGameTests} and
 * {@link HomesteadCensus}), the homestead reached <b>93.2%</b> of them on the
 * single-connector piece and <b>98.9%</b> on the three-connector one. Several
 * slots at three-sevenths each is simply a lot of chances, and losing one to a
 * neighbouring house costs the village nothing as long as another slot takes
 * it.
 *
 * <p>So the weight is not doing the work it was raised to do, and it is not
 * the number to reach for. It is also not free to raise: the pool is shared
 * with vanilla's four terminators, and crowding them out changes the look of
 * every plains village and not just ours.
 *
 * <p>A village that rolls two barns gets two barns; only the first grows a
 * cowboy (see {@code CowboyHandler}).
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
