package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.data.HorseAfterlife;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.equine.Horse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.UUID;

/**
 * <b>Keeping a dead horse long enough for somebody to ask for it back</b>, and
 * throwing it away once they have had their chance. The writing half of
 * {@link HorseAfterlife}; {@link HorseResurrectCommand} is the reading half.
 *
 * <h2>This is the last resort, deliberately</h2>
 * Three things already stand between a horse and dying, and all three are
 * better than this one because the player keeps playing through them:
 * {@code HorseLastStandHandler} refuses to let a single blow kill, the
 * {@code RescuingBraidHandler} sends a braided horse home, and
 * {@link EmergencyStasisHandler} bottles any horse its owner has insured. A
 * horse only reaches this file when all of those were absent or spent. So a
 * resurrection is an <b>operator's</b> undo and not a mechanic: nothing a
 * player holds or crafts reaches it, and it is asked for in chat, not clicked.
 *
 * <h2>Where the snapshot is taken</h2>
 * On {@link LivingDeathEvent}, at default priority. NeoForge does not deliver a
 * cancelled event to an ordinary subscriber, so the last stand's save - which
 * cancels the damage rather than the death, but is the shape of thing that
 * could - cannot leave a snapshot of a horse that did not die. The animal is at
 * zero health at this point and its tag says so; {@link HorseResurrectCommand}
 * heals it on the way back out, which it would have to do anyway.
 *
 * <p>It is taken <b>before</b> vanilla drops the horse's tack, so the saddle
 * and the armour are inside the snapshot as well as on the ground. That would
 * be a duplication bug, and the resurrect command strips the gear rather than
 * this file editing NBT keys by hand - see {@code HorseResurrectCommand.bare}.
 *
 * <h2>Owned horses only</h2>
 * A wild horse has nobody to ask for it back, and between the horse realm, the
 * debug dimension and ordinary worldgen they die in numbers that would make
 * this store the largest thing in the save. The gate is the record's
 * {@code ownerId}, which {@code HorseRecords.setOwner} mirrors off vanilla's
 * own owner - so it is exactly "somebody tamed this and still has it".
 *
 * <p><b>Not verified in-game.</b>
 */
@EventBusSubscriber
public final class HorseAfterlifeHandler {

    /**
     * How often the expiry sweep runs, in ticks. Ten seconds: the window it is
     * spending is measured in tens of minutes, so the resolution is irrelevant
     * and the cost should be. The sweep is one pass over the online players,
     * each of which is a map walk over a store that holds a handful of entries
     * on any server where the setting is doing its job.
     */
    private static final int SWEEP_INTERVAL = 200;

    private HorseAfterlifeHandler() {
    }

    // ------------------------------------------------------------------
    // Death
    // ------------------------------------------------------------------

    @SubscribeEvent
    static void onHorseDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Horse horse)) {
            return;
        }
        if (!(horse.level() instanceof ServerLevel level)) {
            return;
        }
        if (!HorseRecords.hasRealRecord(horse)) {
            return;
        }
        HorseRecord record = HorseRecords.of(horse);
        UUID owner = record.ownerId().orElse(null);
        if (owner == null) {
            return;
        }

        MinecraftServer server = level.getServer();
        String name = record.displayName();
        HorseAfterlife.get(server).died(new HorseAfterlife.Wake(
                HorseStasisHandler.snapshot(horse, name),
                owner,
                ownerName(server, owner),
                level.dimension(),
                horse.blockPosition(),
                level.getGameTime(),
                0));

        ActionTrace.log("afterlife", ActionTrace.describeShort(horse)
                + " kept for resurrection (owner " + owner + ")");
        tellOwner(server, owner, name);
    }

    /**
     * The owner's name, cached into the wake so the operator's listing can say
     * whose horse it was without a lookup per row.
     *
     * <p>Read off the online player rather than a profile cache, and blank when
     * they are not here. That is not a compromise: the owner is nearly always
     * standing next to a horse when it dies, the UUID underneath is the
     * authority either way, and the listing already has to handle a blank for
     * the case where the cache has never heard of them.
     */
    private static String ownerName(MinecraftServer server, UUID owner) {
        ServerPlayer player = server.getPlayerList().getPlayer(owner);
        return player == null ? "" : player.getGameProfile().name();
    }

    /**
     * Tell the owner the horse can still be brought back, if they are here to
     * read it. Without this line the feature is invisible: nothing in the game
     * tells a player that asking an operator is a thing that would work, and a
     * rescue nobody knows to ask for is not a rescue.
     *
     * <p>Silent when the window is off, because then there is nothing true to
     * say, and silent for an offline owner, whose window has not started.
     */
    private static void tellOwner(MinecraftServer server, UUID owner, String horseName) {
        if (ServerConfig.resurrectGraceTicks() == 0) {
            // "For ever" - true, but not something to promise in chat on every
            // death. The operator knows; the player is told the ordinary notice.
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(owner);
        if (player == null) {
            return;
        }
        player.sendSystemMessage(Component.literal(
                        horseName + " can still be brought back. Ask an operator soon - "
                                + "the chance runs out after a while of you being online.")
                .withStyle(ChatFormatting.GRAY));
    }

    // ------------------------------------------------------------------
    // Expiry
    // ------------------------------------------------------------------

    /**
     * Spend each online player's time against their own dead horses.
     *
     * <p>{@link HorseAfterlife#dropExpired} runs in the same sweep and is not
     * redundant with it: {@link HorseAfterlife#spend} only ever visits an owner
     * who is logged in, so lowering the setting would otherwise leave a horse
     * belonging to somebody who never comes back sitting in the save for ever
     * under a window it is already past.
     */
    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % SWEEP_INTERVAL != 0) {
            return;
        }
        HorseAfterlife afterlife = HorseAfterlife.get(server);
        if (afterlife.size() == 0) {
            return;
        }
        int grace = ServerConfig.resurrectGraceTicks();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            afterlife.spend(player.getUUID(), SWEEP_INTERVAL, grace);
        }
        afterlife.dropExpired(grace);
    }
}
