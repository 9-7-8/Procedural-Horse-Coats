package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.HorseGenetics;
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
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
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
 *   <li><b>A hay portal</b> at the origin of every
 *       {@value #PORTAL_GRID_CHUNKS}-chunk cell: {@value #PORTAL_CELLS} by
 *       {@value #PORTAL_CELLS} of them, so wherever you are, an exit is within
 *       about 1 130 blocks.</li>
 *   <li><b>An invisible barrier</b> one block outside each edge. Barrier blocks
 *       for the walking case, and {@link HorseRealmRules} clamps anything that
 *       gets over them.</li>
 * </ul>
 *
 * <h2>Where a portal comes out</h2>
 * An Overworld portal always arrives at the <i>same</i> one of the hundred, hashed
 * from its own block position. That is what makes the realm usable rather than
 * merely large: the herd you released is where you left it, every time, and two
 * players who built their portals in different places do not land on top of each
 * other. Going home is the reverse - any of the hundred returns you to the portal
 * you came in by ({@link ModAttachments#REALM_RETURN}), which is why the grid
 * needs no destination wiring of its own.
 *
 * <h2>What is deliberately not here</h2>
 * A horse standing in a realm portal on its own does <b>not</b> travel. Entry is
 * open to horses - that is how you bring them in, led through an Overworld
 * portal - but the exit is players only, plus the tamed horses they own standing
 * near them. A released horse that wandered into an exit and surfaced in a
 * stranger's back garden would be the one outcome this whole feature exists to
 * prevent.
 *
 * <p><b>Not verified in-game.</b> Nothing on this page has been seen running.
 */
public final class HorseRealm {

    public static final ResourceKey<Level> REALM_LEVEL = ResourceKey.create(
            Registries.DIMENSION, Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "horse_realm"));

    /** Chunks on a side. Chunk coordinates run 0..{@code CHUNKS - 1}. */
    public static final int CHUNKS = 1000;
    /** Blocks on a side. The field is {@code [0, SIZE)} on both X and Z. */
    public static final int SIZE = CHUNKS * 16;

    public static final int BEDROCK_Y = -1;
    public static final int GROUND_Y = 0;
    /** Where a standing entity's feet go. */
    public static final int STAND_Y = GROUND_Y + 1;

    public static final int PORTAL_GRID_CHUNKS = 100;
    public static final int PORTAL_CELLS = CHUNKS / PORTAL_GRID_CHUNKS;
    public static final int POOL_GRID_CHUNKS = 5;
    public static final int POOL_SIZE = 2;

    /** How tall the perimeter wall is built. Above it, {@link HorseRealmRules} clamps. */
    public static final int WALL_HEIGHT = 16;

    // Portal geometry, chunk-local, all inside the grid cell's origin chunk so a
    // portal is never split across two chunks and can be built from one decoration
    // pass. The plane runs along X at local z = PORTAL_DZ, so you walk into it
    // from north or south.
    public static final int PORTAL_DX = 6;          // west edge of the hay frame
    public static final int PORTAL_DZ = 8;          // the plane
    static final int PORTAL_INNER_W = 2;
    static final int PORTAL_INNER_H = 3;
    /** How far south of the plane an arriving traveller is put down. */
    static final int ARRIVE_DZ = PORTAL_DZ + 3;

    /**
     * How far from the portal a leaving player's own horses are gathered. The
     * debug dimension takes every tamed horse in the plot, because a plot is one
     * visit's worth of corridor. This realm is 16 000 blocks across and shared,
     * so "yours" has to mean "the ones standing with you", not "every horse you
     * own in the dimension" - otherwise walking out drags a herd you left here
     * on purpose back home with you.
     */
    static final double EVACUATE_RADIUS = 48.0;

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

    public static boolean inBounds(double x, double z) {
        return x >= 0.0 && x < SIZE && z >= 0.0 && z < SIZE;
    }

    // --- the exit grid ---

    /**
     * Which of the {@value #PORTAL_CELLS}x{@value #PORTAL_CELLS} exits an
     * Overworld portal at {@code from} arrives at. A hash rather than a scaled
     * coordinate: the Overworld is millions of blocks wide and the realm is
     * sixteen thousand, so any linear mapping puts every portal anyone will ever
     * build into one cell. Stable for the life of a world, because it reads only
     * the block position.
     */
    public static ChunkPos arrivalCell(BlockPos from) {
        long h = mix(BlockPos.asLong(from.getX(), 0, from.getZ()));
        int gx = (int) Math.floorMod(h, (long) PORTAL_CELLS);
        int gz = (int) Math.floorMod(mix(h ^ 0x5DEECE66DL), (long) PORTAL_CELLS);
        return new ChunkPos(gx * PORTAL_GRID_CHUNKS, gz * PORTAL_GRID_CHUNKS);
    }

    /** MurmurHash3's finalizer - even in the low bits, which {@code floorMod} takes. */
    private static long mix(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return z ^ (z >>> 33);
    }

    /** True if this chunk carries one of the grid's hay portals. */
    public static boolean isPortalChunk(int cx, int cz) {
        return cx >= 0 && cz >= 0 && cx < CHUNKS && cz < CHUNKS
                && cx % PORTAL_GRID_CHUNKS == 0 && cz % PORTAL_GRID_CHUNKS == 0;
    }

    /** True if this chunk carries one of the grid's watering holes. */
    static boolean isPoolChunk(int cx, int cz) {
        return cx >= 0 && cz >= 0 && cx < CHUNKS && cz < CHUNKS
                && cx % POOL_GRID_CHUNKS == 0 && cz % POOL_GRID_CHUNKS == 0;
    }

    /** The block a portal chunk's hay frame is measured from (its north-west ground corner). */
    static BlockPos portalAnchor(ChunkPos cell) {
        return new BlockPos(cell.getMinBlockX(), GROUND_Y, cell.getMinBlockZ());
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
        BlockPos anchor = portalAnchor(cell);
        HorsePortalManager.placeAt(entity, realm,
                anchor.offset(PORTAL_DX + 1, STAND_Y - GROUND_Y, ARRIVE_DZ));
    }

    /**
     * Take {@code player} home through any of the realm's exits, and their own
     * tamed horses within {@value #EVACUATE_RADIUS} blocks with them. Returns
     * silently for anything that is not a player - see the class note.
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

        // Their horses first, so the player lands last and sees them already there.
        List<BlockPos> used = new ArrayList<>();
        for (AbstractHorse horse : ownedHorsesNear(realm, player, portalPos)) {
            if (horse.isLeashed()) {
                horse.dropLeash();
            }
            HorsePortalManager.placeReturningHorse(horse, target, to, used);
        }
        if (entity instanceof Mob mob && mob.isLeashed()) {
            mob.dropLeash();
        }
        HorsePortalManager.placeAt(player, target, to);
    }

    private static List<AbstractHorse> ownedHorsesNear(ServerLevel realm, ServerPlayer player, BlockPos portalPos) {
        AABB box = new AABB(portalPos).inflate(EVACUATE_RADIUS);
        return realm.getEntitiesOfClass(AbstractHorse.class, box, horse -> {
            if (!horse.isAlive() || !horse.isTamed()) {
                return false;
            }
            EntityReference<LivingEntity> owner = horse.getOwnerReference();
            return owner != null && player.getUUID().equals(owner.getUUID());
        });
    }

    /**
     * The remembered Overworld portal, put back if it has gone. A portal is a
     * player-built thing and a player can mine it, so the way home is the one
     * piece of this feature that can be taken away while you are standing in
     * another dimension. If the hay frame is still there, this is a no-op and
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
