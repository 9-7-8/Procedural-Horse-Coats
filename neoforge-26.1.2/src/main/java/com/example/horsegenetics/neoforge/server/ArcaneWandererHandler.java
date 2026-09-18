package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.entity.Cowboy;
import com.example.horsegenetics.neoforge.entity.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * <b>The arcane dealer out in the world</b>, away from any village (owner,
 * 2026-09-18).
 *
 * <p>Ordinary cowboys come with a barn, because a man with a paddock and a
 * restock clock is a fixture of somewhere. This one is not from anywhere - he
 * keeps no breed and no region - so a village was always the odd place to meet
 * him. He turns up on the road as well: a periodic roll near a player, a string
 * of magical horses, and after a day or two he is gone again.
 *
 * <h2>Why he leaves</h2>
 * A {@link Cowboy} calls {@code setPersistenceRequired()} in its constructor,
 * which is right for one that came with a structure and wrong for one that
 * spawned on a hillside: every dealer the world ever rolled would still be
 * standing in it, each with up to ten horses that also never despawn. So a
 * wanderer carries an expiry, and when it passes he and his unsold string are
 * removed together, out of sight - the way {@code CowboyHandler.retireOne}
 * rotates stock. A horse a player has bought and redeemed is tamed, has lost its
 * brand and has left the herd, so it is never touched by this.
 */
@EventBusSubscriber
public final class ArcaneWandererHandler {

    /** How often a spawn is even considered. Ten seconds; this is not a hot path. */
    private static final int TRY_INTERVAL = 200;

    /**
     * The chance per player per attempt. With {@link #TRY_INTERVAL} at ten
     * seconds this is roughly one dealer every two or three hours of play - rare
     * enough to be a find, while the village barn (one in twelve) stays the way
     * to go looking for one on purpose.
     */
    private static final float SPAWN_CHANCE = 0.0015F;

    /** Far enough not to appear in front of somebody, near enough to be worth finding. */
    private static final int MIN_DISTANCE = 48;
    private static final int MAX_DISTANCE = 96;

    /** Attempts at a standing spot before giving up until the next roll. */
    private static final int PLACEMENT_TRIES = 12;

    /** Never a second one within this of a player - one dealer is an event, three is a shop. */
    private static final int CROWDING_RADIUS = 256;

    /** A day and a half of game time on the road before he moves on. */
    public static final long LIFETIME_TICKS = 36_000L;

    /** He does not vanish in front of anybody; past his expiry he waits for privacy. */
    private static final int DESPAWN_PRIVACY = 64;

    private ArcaneWandererHandler() {
    }

    /**
     * Hostile mobs ignore every cowboy, wandering or not.
     *
     * <p>{@link Cowboy} extends {@code AbstractVillager}, which vanilla zombies
     * target by class, and {@code Cowboy.HEALTH} is two hundred precisely
     * because of it - "a villager on foot in a field at night is a zombie's
     * supper". In a village that is survivable: golems, walls, lit ground. A man
     * stood alone on a hillside with ten horses has none of those, and the
     * failure is not that he dies - he usually does not - but that <b>his string
     * does</b>, and a dealer found alive beside his dead stock reads as the
     * feature being broken rather than as a bad night.
     *
     * <p>Owner's call, and it deliberately covers the village cowboy too rather
     * than only the wanderer: two kinds of cowboy differing in one more
     * invisible way is worse than both being left alone. His two hundred health
     * stays, because nothing here stops a creeper, a fall or a player.
     */
    @SubscribeEvent
    static void doNotHunt(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof Cowboy) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % TRY_INTERVAL != 0) {
            return;
        }
        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                if (level.getRandom().nextFloat() < SPAWN_CHANCE) {
                    trySpawnNear(level, player);
                }
            }
        }
    }

    /** One dealer, on open ground, out of sight of the player whose roll found him. */
    private static void trySpawnNear(ServerLevel level, ServerPlayer player) {
        if (alreadyOneNear(level, player)) {
            return;
        }
        BlockPos spot = findGround(level, player.blockPosition());
        if (spot == null) {
            return;
        }
        Cowboy cowboy = ModEntities.COWBOY.get().create(level, EntitySpawnReason.EVENT);
        if (cowboy == null) {
            return;
        }
        cowboy.snapTo(spot, level.getRandom().nextFloat() * 360.0F, 0.0F);
        cowboy.setWanderExpiry(level.getGameTime() + LIFETIME_TICKS);
        level.addFreshEntity(cowboy);

        // The ordinary founding path, minus the paddock walk: he has no barn to
        // walk out of, and is already standing on the open ground that walk
        // exists to go and find.
        CowboyHandler.foundArcaneInPlace(cowboy, level);

        HorseGenetics.LOGGER.info("[Cowboy] wandering arcane dealer {} at {} with {} horses",
                cowboy.cowboyName(), spot, cowboy.herdIds().size());
        DebugAnnounce.sayAt(level, "Cowboy",
                cowboy.cowboyName() + " is passing through with "
                        + cowboy.herdIds().size() + " magical horses",
                spot, ChatFormatting.LIGHT_PURPLE);
    }

    private static boolean alreadyOneNear(ServerLevel level, ServerPlayer player) {
        return !level.getEntitiesOfClass(Cowboy.class,
                player.getBoundingBox().inflate(CROWDING_RADIUS)).isEmpty();
    }

    private static @Nullable BlockPos findGround(ServerLevel level, BlockPos near) {
        for (int attempt = 0; attempt < PLACEMENT_TRIES; attempt++) {
            double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
            int distance = MIN_DISTANCE + level.getRandom().nextInt(MAX_DISTANCE - MIN_DISTANCE + 1);
            BlockPos candidate = near.offset(
                    (int) Math.round(Math.cos(angle) * distance), 0,
                    (int) Math.round(Math.sin(angle) * distance));
            if (!level.isLoaded(candidate)) {
                continue; // never spawn into an unloaded chunk - that would force it to load
            }
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, candidate);
            if (ground.getY() <= level.getMinY() + 1) {
                continue;
            }
            if (!level.getFluidState(ground.below()).isEmpty()) {
                continue; // not standing on water, and not on lava
            }
            return ground;
        }
        return null;
    }

    /**
     * His time is up: take him and the stock he did not sell, once nobody is
     * near enough to watch it happen. Driven from {@code CowboyHandler.tick},
     * which already runs per cowboy - a sweep of the level would be a world scan
     * to find at most a handful of entities.
     *
     * @return whether he actually left; false means he waits for privacy
     */
    static boolean moveOnIfDue(Cowboy cowboy, ServerLevel level) {
        if (!cowboy.isWanderer() || level.getGameTime() < cowboy.wanderExpiry()) {
            return false;
        }
        if (level.getNearestPlayer(cowboy, DESPAWN_PRIVACY) != null) {
            return false;
        }
        List<UUID> herd = new ArrayList<>(cowboy.herdIds());
        int taken = 0;
        for (UUID id : herd) {
            if (level.getEntity(id) instanceof Horse horse && horse.isAlive() && !horse.isTamed()) {
                CowboyHandler.clearBrand(horse);
                horse.discard();
                taken++;
            }
        }
        HorseGenetics.LOGGER.info("[Cowboy] wandering dealer {} moved on, taking {} horses",
                cowboy.cowboyName(), taken);
        cowboy.discard();
        return true;
    }
}
