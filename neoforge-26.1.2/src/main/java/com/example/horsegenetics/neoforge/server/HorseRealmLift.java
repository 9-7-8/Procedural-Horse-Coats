package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * <b>The realm's floor moved up, and this carries the worlds that were built on
 * the old one.</b> Chunk by chunk, as they load: everything below the new
 * surface is picked up and put down {@value #OFFSET} blocks higher, and the
 * chunk it came out of is left empty.
 *
 * <h2>This is back-compat code, which this repo does not allow</h2>
 * <b>The owner asked for it by name</b> ("write a handler for worlds that
 * already generated the dimension, that losslessly just moves all blocks
 * upwards"), and the reason the standing rule does not apply is that the rule's
 * own premise has expired: <i>no saves worth keeping</i> was true when there was
 * one tester and a dev world. There is now a shared server with other people on
 * it and a field of horses living in this dimension, and regenerating it would
 * delete them. Written down here rather than argued each time somebody reads
 * the hard rules and finds this class.
 *
 * <h2>How it knows which chunks still need it</h2>
 * <b>It asks the chunk, and stores nothing.</b> A chunk generated before the
 * move has bedrock at {@link #BUILT_AT_Y}{@code  - 1}; one generated after it
 * has air there, because the flat layers put the bedrock at
 * {@link HorseRealm#BEDROCK_Y} now. So the test is one block read, it is exact,
 * and it answers <i>no</i> for every chunk in every world that never saw the old
 * layout - including every world created from here on.
 *
 * <p>That also makes it <b>idempotent by construction</b>, which a saved flag
 * would not be: a chunk that has been lifted no longer has bedrock down there,
 * so a second pass over it does nothing. No migration marker to get out of step
 * with the blocks it describes, nothing to write on a world that does not need
 * it, and a chunk that was somehow missed is caught the next time anybody walks
 * into it. It is {@link HorseRealmTerrain}'s own argument for having no
 * "decorated" flag, and it is the same argument.
 *
 * <h2>Why the copy needs no care about order</h2>
 * The source range ends at {@link HorseRealm#GROUND_Y}{@code  - 1} and the
 * destination starts one block above that, so <b>the two do not overlap at
 * all</b> and a single ascending pass cannot overwrite something it has not
 * read yet. That is not luck - it is what picking an offset bigger than the
 * height of anything the realm builds buys, and it is worth checking again if
 * the surface ever moves by a smaller step.
 *
 * <p>Empty sections are skipped whole ({@link LevelChunkSection#hasOnlyAir}),
 * which is what keeps this from being twenty thousand block reads a chunk: an
 * old realm chunk has content in two sections and air in the rest.
 *
 * <h2>The horses are not moved here</h2>
 * They are caught by {@link HorseRealmRules}, which now floors anything in the
 * realm that is below the surface. An entity can be in an unloaded entity
 * section when its blocks load, so moving them from here would miss exactly the
 * ones that matter; a floor catches them whenever they turn up, and closes the
 * older gap that nothing stopped a player falling into the void either.
 *
 * <p><b>Not verified in-game.</b> Nothing about this has been watched happen,
 * and it rewrites terrain - see the page's Verification tab before trusting it
 * with a world anybody cares about.
 */
@EventBusSubscriber
public final class HorseRealmLift {

    /**
     * <b>Where the realm's surface used to be.</b> A frozen historical number,
     * not a setting: it is what {@link HorseRealm#GROUND_Y} was before the lift,
     * and the only thing it is for is recognising a chunk built back then. If the
     * surface moves again, this stays at 0 only if every world has already been
     * carried past it - otherwise a second constant and a second pass are needed,
     * because a chunk can only be recognised by the floor it actually has.
     */
    public static final int BUILT_AT_Y = 0;

    /** How far up everything goes. */
    public static final int OFFSET = HorseRealm.GROUND_Y - BUILT_AT_Y;

    /** Chunks waiting to be lifted, packed by {@link ChunkPos#pack()}. */
    private static final Deque<Long> PENDING = new ArrayDeque<>();

    /**
     * Fewer per tick than {@link HorseRealmTerrain}'s eight. A lift is two full
     * chunk sections of block writes where a decoration is a handful, and this
     * runs while somebody is walking into the country it is rewriting.
     */
    private static final int PER_TICK = 2;

    /** Logged once, so a world being migrated says so in the log rather than silently. */
    private static boolean announced;

    private HorseRealmLift() {
    }

    @SubscribeEvent
    static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !HorseRealm.isRealm(level)) {
            return;
        }
        PENDING.add(event.getChunk().getPos().pack());
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) {
            return;
        }
        ServerLevel realm = event.getServer().getLevel(HorseRealm.REALM_LEVEL);
        if (realm == null) {
            PENDING.clear();
            return;
        }
        for (int done = 0; done < PER_TICK && !PENDING.isEmpty(); done++) {
            ChunkPos at = ChunkPos.unpack(PENDING.poll());
            // It may have unloaded again while it sat in the queue; touching it
            // would load it back - HorseRealmTerrain's rule, for its reason.
            if (realm.hasChunk(at.x(), at.z())) {
                liftNow(realm, at);
            }
        }
    }

    /**
     * Lift this chunk <b>right now</b> if it still needs it. Called from the tick
     * queue, and directly by {@link HorseRealmTerrain} before it decorates -
     * because a decorated chunk that has not been lifted yet would get a new
     * portal at the new surface and then have the old one dropped on top of it.
     */
    public static void liftNow(ServerLevel realm, ChunkPos at) {
        if (OFFSET == 0 || !realm.hasChunk(at.x(), at.z())) {
            return;
        }
        LevelChunk chunk = realm.getChunk(at.x(), at.z());
        if (!builtOnTheOldFloor(realm, at)) {
            return;
        }
        if (!announced) {
            announced = true;
            HorseGenetics.LOGGER.info(
                    "[realm] this world's horse realm was generated with its floor at y={} - "
                            + "lifting it to y={}, one chunk at a time as they load",
                    BUILT_AT_Y, HorseRealm.GROUND_Y);
        }
        lift(realm, chunk, at);
    }

    /**
     * Bedrock where the old floor's bedrock was. Read at the chunk's own north-west
     * column, which the flat generator filled along with every other one.
     */
    private static boolean builtOnTheOldFloor(ServerLevel realm, ChunkPos at) {
        BlockPos probe = new BlockPos(at.getMinBlockX(), BUILT_AT_Y - 1, at.getMinBlockZ());
        return realm.getBlockState(probe).is(Blocks.BEDROCK);
    }

    private static void lift(ServerLevel realm, LevelChunk chunk, ChunkPos at) {
        BlockState air = Blocks.AIR.defaultBlockState();
        int srcTop = HorseRealm.GROUND_Y - 1;
        BlockPos.MutableBlockPos src = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos dst = new BlockPos.MutableBlockPos();

        for (int y = realm.getMinY(); y <= srcTop; y += 16) {
            int index = chunk.getSectionIndex(y);
            if (index < 0 || index >= chunk.getSections().length) {
                continue;
            }
            LevelChunkSection section = chunk.getSections()[index];
            if (section == null || section.hasOnlyAir()) {
                continue;       // the whole point - most of the column is nothing
            }
            int sectionBottom = chunk.getSectionYFromSectionIndex(index) << 4;
            for (int dy = 0; dy < 16; dy++) {
                int fromY = sectionBottom + dy;
                if (fromY < realm.getMinY() || fromY > srcTop) {
                    continue;
                }
                for (int dx = 0; dx < 16; dx++) {
                    for (int dz = 0; dz < 16; dz++) {
                        src.set(at.getMinBlockX() + dx, fromY, at.getMinBlockZ() + dz);
                        BlockState was = realm.getBlockState(src);
                        if (was.isAir()) {
                            continue;
                        }
                        dst.set(src.getX(), fromY + OFFSET, src.getZ());
                        realm.setBlock(dst, resurface(was, fromY), 2);
                        realm.setBlock(src, air, 2);
                    }
                }
            }
        }
    }

    /**
     * <b>The surface course comes up as grass.</b> The old realm's floor was
     * {@code minecraft:dirt} and the new one is {@code minecraft:grass_block}, so
     * a lifted world that kept its dirt would be the one world where the field is
     * brown - a difference nobody would ever be able to explain by looking at it.
     *
     * <p>Only the surface course, and only dirt: a dirt block that is part of
     * something else at another height is left exactly as it was, which is what
     * "losslessly" has to mean.
     */
    private static BlockState resurface(BlockState was, int fromY) {
        if (fromY == BUILT_AT_Y && was.is(Blocks.DIRT)) {
            return Blocks.GRASS_BLOCK.defaultBlockState();
        }
        return was;
    }
}
