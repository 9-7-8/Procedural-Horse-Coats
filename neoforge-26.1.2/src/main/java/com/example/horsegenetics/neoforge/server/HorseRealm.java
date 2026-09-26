package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.data.HorseRealmSize;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.Set;

/**
 * <b>The horse realm - a public commons, not the debug corridor.</b>
 *
 * <p>A bounded, flat, eternally-lit field where a player can leave surplus
 * horses without fear of harming them, and another player can find one and tame
 * it. It is a <i>second</i> dimension: {@link DebugPenManager#DEBUG_LEVEL} stays
 * exactly what it was - a private, disposable, per-player test corridor reached
 * by F6 - and <b>none</b> of this page's rules are applied to it. The ordinary
 * hay-bale portal now arrives here instead.
 *
 * <p>Everything about the place is a fixed function of a block coordinate, and
 * that is the whole design. There is no saved layout, no per-player plot, no
 * allocator: the terrain, the water and the hundred exits are worked out from
 * the chunk position when a chunk loads ({@link HorseRealmTerrain}), so the
 * realm needs no state at all beyond each player's own way home.
 *
 * <h2>The shape of it</h2>
 * <ul>
 *   <li><b>{@value #CHUNKS} by {@value #CHUNKS} chunks</b>, numbered 0..999 on
 *       both axes, so the field runs from block 0 to block {@code SIZE - 1}.
 *       Finite on purpose - an endless field cannot be walked, shared or
 *       searched, and a horse released into one is gone.</li>
 *   <li><b>A flat plane at Y=0</b>: bedrock at Y={@value #BEDROCK_Y}, one dirt
 *       layer at Y={@value #GROUND_Y}. Both come from the {@code minecraft:flat}
 *       generator in {@code dimension/horse_realm.json}; nothing here builds
 *       them.</li>
 *   <li><b>A {@value #POOL_SIZE}x{@value #POOL_SIZE} water pool</b> at the
 *       origin of every {@value #POOL_GRID_CHUNKS}-chunk cell. Not decoration:
 *       {@code HorseCareHandler}'s healing is gated on standing near water, so a
 *       dry field is a field where a hurt horse never recovers - the mistake the
 *       test yard made for weeks (gap 258).</li>
 *   <li><b>A portal</b> at the origin of every
 *       <b>one exit, at the origin</b>, which every Overworld portal arrives
 *       at - so the horses are all within walking distance of one place.</li>
 *   <li><b>An invisible barrier</b> one block outside each edge. Barrier blocks
 *       for the walking case, and {@link HorseRealmRules} clamps anything that
 *       gets over them.</li>
 * </ul>
 *
 * <h2>Where a portal comes out</h2>
 * <b>Every portal arrives at the middle exit</b>, whoever built it and wherever
 * it is - see {@link #arrivalCell}. The field is shared and the horses in it are
 * the point, so everyone landing in one place is what puts the horses in one
 * place. Going home is the reverse - any of the hundred returns you to the portal
 * you came in by ({@link ModAttachments#REALM_RETURN}), which is why the grid
 * needs no destination wiring of its own.
 *
 * <h2>What is deliberately not here</h2>
 * A horse standing in a realm portal on its own does <b>not</b> travel. Entry is
 * open to horses - that is how you bring them in, led through an Overworld
 * portal - but <b>the exit is players only, and nothing else</b>. A released
 * horse that wandered into an exit and surfaced in a stranger's back garden
 * would be the one outcome this whole feature exists to prevent.
 *
 * <p>It used to be "players, plus the tamed horses they own standing near them",
 * which quietly made the field a stable you emptied on the way out instead of a
 * place horses live. See the note where {@code EVACUATE_RADIUS} used to be.
 *
 * <p><b>Not verified in-game.</b> Nothing on this page has been seen running.
 */
public final class HorseRealm {

    public static final ResourceKey<Level> REALM_LEVEL = ResourceKey.create(
            Registries.DIMENSION, Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "horse_realm"));

    /**
     * <b>The field is a circle centred on the origin, and it grows.</b> How big
     * it is right now lives in {@link HorseRealmSize}, because it is a fact about
     * one world rather than a constant - see that class for the density rule and
     * for why it may never shrink.
     *
     * <p>It used to be a {@code 1000}-chunk square with its origin corner at
     * {@code (0, 0)}, which is why anything that has to read a saved position
     * from before the reshape is reading coordinates that are now a long way
     * outside the field. {@code HorseRealmRules} is what carries those home.
     */
    public static int radius(MinecraftServer server) {
        return HorseRealmSize.get(server).radius();
    }

    /** The old square field, kept only so the reshape knows what to clean up. */
    public static final int OLD_CHUNKS = 1000;
    public static final int OLD_SIZE = OLD_CHUNKS * 16;

    /**
     * <b>The surface.</b> Everything the realm builds is measured from here, and
     * <b>{@code data/horsegenetics/dimension/horse_realm.json} has to agree</b>:
     * its flat layers are air up to {@code BEDROCK_Y - 1}, then one bedrock, then
     * one grass block. Nothing checks that at build time, and the tell that they
     * have drifted is a realm that generates with its floor in the wrong place -
     * which {@code ModGameTests.horse_realm_terrain} does catch, since it asserts
     * the two blocks by name at these coordinates.
     *
     * <p>It was {@code 0} until the lift; see {@link HorseRealmLift}, which is
     * what carries a world that was generated before this moved. If it moves
     * again, that class's own recorded offset is what makes a second lift safe.
     */
    public static final int GROUND_Y = 64;
    public static final int BEDROCK_Y = GROUND_Y - 1;
    /** Where a standing entity's feet go. */
    public static final int STAND_Y = GROUND_Y + 1;

    public static final int POOL_GRID_CHUNKS = 5;
    public static final int POOL_SIZE = 2;

    /** How tall the perimeter wall is built. Above it, {@link HorseRealmRules} clamps. */
    public static final int WALL_HEIGHT = 16;

    // Portal geometry, chunk-local, all inside the grid cell's origin chunk so a
    // portal is never split across two chunks and can be built from one decoration
    // pass. The plane runs along X at local z = PORTAL_DZ, so you walk into it
    // from north or south.
    public static final int PORTAL_DX = 6;          // west edge of the frame
    public static final int PORTAL_DZ = 8;          // the plane
    static final int PORTAL_INNER_W = 2;
    static final int PORTAL_INNER_H = 3;
    /** How far south of the plane an arriving traveller is put down. */
    static final int ARRIVE_DZ = PORTAL_DZ + 3;

    // A LEAVING PLAYER TAKES NO HORSES. There was an EVACUATE_RADIUS here - 48
    // blocks, within which a leaving player's own tamed horses were gathered up
    // and dropped at their overworld portal. It is gone, and so is the radius:
    // the point of the realm is to be somewhere you LEAVE horses. Walking out
    // with a herd in tow made it a stable you had to empty every time rather
    // than a place they live, and the radius was only ever an attempt to guess
    // which of them you meant. A horse comes home the way it came: led or
    // ridden through the frame. (Owner's call.)

    public static boolean isRealm(Level level) {
        return level != null && REALM_LEVEL.equals(level.dimension());
    }

    public static boolean isRealm(ResourceKey<Level> dimension) {
        return REALM_LEVEL.equals(dimension);
    }

    /** True for a horse, a player or anything else currently standing in the realm. */
    public static boolean isInRealm(Entity entity) {
        return entity != null && isRealm(entity.level());
    }

    /** Inside the field: within {@code radius} of the origin. */
    public static boolean inBounds(double x, double z, int radius) {
        return x * x + z * z < (double) radius * radius;
    }

    /** The same, for a whole block column. */
    public static boolean inBounds(int x, int z, int radius) {
        return (double) x * x + (double) z * z < (double) radius * radius;
    }

    /**
     * <b>Is this column the wall?</b> Outside the field, but touching it - the
     * eight-neighbour boundary, so the ring is watertight against a diagonal.
     * A four-neighbour ring has corner gaps an entity moving continuously can
     * slip through, and {@code HorseRealmRules}' clamp should be the second line
     * of defence rather than the first.
     */
    public static boolean isWall(int x, int z, int radius) {
        if (inBounds(x, z, radius)) {
            return false;
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if ((dx != 0 || dz != 0) && inBounds(x + dx, z + dz, radius)) {
                    return true;
                }
            }
        }
        return false;
    }

    // --- the exit grid ---

    /** The one exit. Chunk {@code (0, 0)}, so the frame is at the origin. */
    public static final ChunkPos PORTAL_CELL = new ChunkPos(0, 0);

    /**
     * <b>Every portal arrives at the same exit - the one in the middle of the
     * field.</b>
     *
     * <p>This used to hash the Overworld portal's own block position, so each
     * portal had its own one of the hundred and two players' portals could not
     * land on top of each other. <b>That was the wrong answer to the wrong
     * question</b> (owner's call). Landing apart does not keep two players out of
     * each other's way in a field this size; it keeps their <i>horses</i> apart,
     * scattered across sixteen thousand blocks in hundred-chunk pockets that never
     * meet. A shared field whose whole point is that horses live in it wants them
     * <b>in one place</b> - grazing together, banding together
     * ({@link HorseRealmHerds}), and findable by whoever comes looking.
     *
     * <p>The other ninety-nine exits are still built and still work. They are a
     * way home for somebody who has walked a long way, which is what the grid was
     * always genuinely for; they are simply no longer where anyone <i>starts</i>.
     * Going home is unaffected either way - any exit returns you to the portal you
     * came in by, from {@link ModAttachments#REALM_RETURN}.
     *
     * @param from kept in the signature because the caller has it and a future
     *             change of heart is then one method deep, not a call-site sweep
     */
    public static ChunkPos arrivalCell(BlockPos from) {
        return PORTAL_CELL;
    }

    /** True if this chunk carries the exit. There is exactly one. */
    public static boolean isPortalChunk(int cx, int cz) {
        return cx == PORTAL_CELL.x() && cz == PORTAL_CELL.z();
    }

    /**
     * True if this chunk carries a watering hole. The same grid as before, now
     * clipped to the circle - and to the whole circle, so a chunk that only
     * becomes field when the realm grows gets its pool the first time it loads
     * after that.
     */
    static boolean isPoolChunk(int cx, int cz, int radius) {
        if (Math.floorMod(cx, POOL_GRID_CHUNKS) != 0 || Math.floorMod(cz, POOL_GRID_CHUNKS) != 0) {
            return false;
        }
        return inBounds(cx * 16, cz * 16, radius);
    }

    /** The block a portal chunk's frame is measured from (its north-west ground corner). */
    static BlockPos portalAnchor(ChunkPos cell) {
        return new BlockPos(cell.getMinBlockX(), GROUND_Y, cell.getMinBlockZ());
    }

    /** The block a traveller is put down on, just south of the one exit. */
    public static BlockPos arrivalBlock() {
        return portalAnchor(PORTAL_CELL).offset(PORTAL_DX + 1, STAND_Y - GROUND_Y, ARRIVE_DZ);
    }

    /**
     * <b>Where the field's one entrance puts you</b>, as a position rather than a
     * block - what {@code HorseRealmRules} carries a stranded animal to. Named
     * once here so the arrival, the reshape and the wall all agree on where the
     * middle of this dimension is.
     */
    public static Vec3 arrivalSpot() {
        BlockPos at = arrivalBlock();
        return new Vec3(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
    }

    // --- travel ---

    /**
     * Send {@code entity} into the realm through the Overworld portal at
     * {@code portalPos}. Players have their way home remembered first; horses
     * simply arrive, which is how a led string of them follows their owner in.
     */
    static void enter(Entity entity, ServerLevel from, BlockPos portalPos) {
        MinecraftServer server = from.getServer();
        if (server == null) {
            return;
        }
        ServerLevel realm = server.getLevel(REALM_LEVEL);
        if (realm == null) {
            HorseGenetics.LOGGER.error(
                    "Horse realm not found - is data/horsegenetics/dimension/horse_realm.json present?");
            return;
        }
        ChunkPos cell = arrivalCell(portalPos);
        // Build the exit before anybody stands next to where it should be. The
        // chunk decoration would get to it a tick later anyway, but arriving
        // beside a portal that is not there yet reads as a broken realm.
        HorseRealmTerrain.decorateNow(realm, cell);

        if (entity instanceof ServerPlayer player) {
            player.setData(ModAttachments.REALM_RETURN,
                    Optional.of(GlobalPos.of(from.dimension(), portalPos)));
        }
        HorsePortalManager.placeAt(entity, realm, arrivalBlock());
    }

    /**
     * Take {@code player} home through any of the realm's exits - <b>and nothing
     * else</b>. Returns silently for anything that is not a player, which is
     * also what stops a horse walking into an exit frame from following: see the
     * class note, and the note where {@code EVACUATE_RADIUS} used to be.
     *
     * <p>A horse only comes home if you lead or ride it through, the same way it
     * got here. Nothing standing near the frame is collected.
     */
    static void leave(Entity entity, ServerLevel realm, BlockPos portalPos) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        MinecraftServer server = realm.getServer();
        if (server == null) {
            return;
        }
        GlobalPos home = player.getData(ModAttachments.REALM_RETURN).orElse(null);
        ServerLevel target = home == null ? null : server.getLevel(home.dimension());
        if (target == null) {
            target = server.getLevel(Level.OVERWORLD);
            home = null;
        }
        if (target == null) {
            return;
        }
        BlockPos to = home == null ? target.getRespawnData().pos() : safeReturn(target, home.pos());

        if (entity instanceof Mob mob && mob.isLeashed()) {
            mob.dropLeash();
        }
        HorsePortalManager.placeAt(player, target, to);
    }

    /**
     * The remembered Overworld portal, put back if it has gone. A portal is a
     * player-built thing and a player can mine it, so the way home is the one
     * piece of this feature that can be taken away while you are standing in
     * another dimension. If the frame is still there, this is a no-op and
     * you step out where you stepped in.
     */
    private static BlockPos safeReturn(ServerLevel target, BlockPos remembered) {
        if (target.getBlockState(remembered).is(com.example.horsegenetics.neoforge.block.ModBlocks.HAY_PORTAL.get())) {
            return remembered;
        }
        if (HorseRealmTerrain.rebuildReturnPortal(target, remembered)) {
            return remembered;
        }
        // Nowhere safe to put it back - put the player down on the surface
        // rather than inside whatever was built over the portal.
        return target.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                remembered);
    }

    private HorseRealm() {
    }
}
