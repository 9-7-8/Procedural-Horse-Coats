package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.progress.ProgressTask;
import com.example.horsegenetics.neoforge.data.HorseWhereabouts;
import com.example.horsegenetics.neoforge.data.PenRecord;
import com.example.horsegenetics.neoforge.data.StallData;
import com.example.horsegenetics.neoforge.data.StallRecord;
import com.example.horsegenetics.neoforge.item.HoldingPenTicketItem;
import com.example.horsegenetics.neoforge.item.TicketItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * <b>"Send home" from the horse browser.</b> One button per row of <i>My
 * horses</i>: spend a ticket, and the horse goes to its own stall, or to the
 * player's holding pen if it has no stall of its own.
 *
 * <h2>It is the ticket items' rules, not a second set</h2>
 * Everything about where a horse may be sent, where in a stall it lands and
 * what a landing failure means lives in {@link TicketHandler}, and this calls
 * into it rather than restating it. That matters because the two <i>look</i>
 * separable and are not: the stall is re-measured live at spend time, the tier
 * decides which worlds a ticket reaches, and a refusal has to leave the ticket
 * unspent. A second copy of any of those drifts, and the drift would be a horse
 * teleported into a wall.
 *
 * <p>The one behaviour that is genuinely new is the <b>fallback</b>. Right-click
 * a written ticket on a horse with no stall and it refuses; this sends it to the
 * holding pen instead, because "put this horse somewhere sensible" is the whole
 * point of a button on a list of horses you cannot see.
 *
 * <h2>Which ticket is spent</h2>
 * The cheapest one in the player's inventory that can actually make the trip -
 * see {@link #chooseTicket}. A pen trip prefers an actual holding-pen ticket,
 * which is the purpose-made item and is not tier-limited.
 *
 * <h2>Horses that are not loaded</h2>
 * Which is most of them, and the reason the button is worth having - a horse you
 * can see is one you can walk up to and right-click. The machinery is the ender
 * whistle's, because that problem was solved there first: put a chunk ticket on
 * the last place the horse was seen, then wait a few seconds for it to load and
 * act when it does ({@link EnderWhistleCalls#call}). Nothing is spent and
 * nothing is promised until the horse is actually in hand.
 */
@EventBusSubscriber
public final class StallRecall {

    /** How long to wait for a called horse's chunk to load before giving up. */
    private static final int WAIT_TICKS = 100;

    /** Chunk-ticket radius around the horse's last known position. */
    private static final int TICKET_RADIUS = 2;

    /** A recall waiting on its horse to load. */
    private record Pending(UUID player, UUID horse, long deadline) {
    }

    private static final List<Pending> PENDING = new CopyOnWriteArrayList<>();

    private StallRecall() {
    }

    // ------------------------------------------------------------------
    // Entry point - the browser's button, via HorseRecallPayload.
    // ------------------------------------------------------------------

    /**
     * Send one horse home. Every refusal says what to do about it and spends
     * nothing; this is reachable from a button a player can mash, so it has to
     * be safe to call with any id at any time.
     */
    public static void request(ServerPlayer player, UUID horseId) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        if (HorseWhereabouts.get(server).isDead(horseId)) {
            say(player, "That horse is dead.");
            return;
        }
        // Cheap pre-check before pulling a chunk in for nothing. It cannot be
        // the real check - the tier needed depends on which world the horse
        // turns out to be in, which is not known until it loads - so the
        // authoritative one is in send(), after it is in hand.
        if (!holdsAnyTicket(player)) {
            say(player, "You have no tickets. A written ticket sends a horse to its stall, "
                    + "a holding pen ticket sends it to your pen.");
            return;
        }

        Horse loaded = findLoaded(server, horseId);
        if (loaded != null) {
            send(player, loaded);
            return;
        }
        var seen = HorseWhereabouts.get(server).lookup(horseId);
        ServerLevel there = seen.map(s -> server.getLevel(s.dimension())).orElse(null);
        if (seen.isEmpty() || there == null) {
            say(player, "That horse has not been seen anywhere yet.");
            return;
        }
        BlockPos pos = seen.get().pos();
        there.getChunkSource().addTicketWithRadius(TicketType.PORTAL,
                new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4), TICKET_RADIUS);
        PENDING.add(new Pending(player.getUUID(), horseId, server.getTickCount() + WAIT_TICKS));
        say(player, "Reaching for that horse...");
    }

    /** Poll the waiting recalls, exactly as the whistle does. */
    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        for (Iterator<Pending> it = PENDING.iterator(); it.hasNext(); ) {
            Pending pending = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(pending.player());
            if (player == null) {
                PENDING.remove(pending);    // logged off; the chunk ticket expires on its own
                continue;
            }
            Horse horse = findLoaded(server, pending.horse());
            if (horse != null) {
                PENDING.remove(pending);
                send(player, horse);
            } else if (server.getTickCount() > pending.deadline()) {
                PENDING.remove(pending);
                say(player, "That horse was not where it was last seen.");
            }
        }
    }

    // ------------------------------------------------------------------
    // The move
    // ------------------------------------------------------------------

    /**
     * The horse is in hand. Work out where home is, find a ticket that reaches
     * it, and hand the actual move to {@link TicketHandler}.
     */
    private static void send(ServerPlayer player, Horse horse) {
        MinecraftServer server = player.level().getServer();
        if (server == null || !(horse.level() instanceof ServerLevel from)) {
            return;
        }
        if (!HorseOwnership.isOwner(horse, player.getUUID())) {
            say(player, "That horse does not answer to you.");
            return;
        }
        if (horse.isVehicle()) {
            say(player, "That horse has a rider, and stays put.");
            return;
        }

        // Its own stall first; the holding pen only when it has none. A horse
        // with a stall is never sent to the pen, even if a pen ticket is the
        // cheaper spend - the stall is where that horse belongs.
        StallData stalls = StallData.get(server);
        StallRecord stall = stalls.forHorse(horse.getUUID());
        PenRecord pen = stall == null ? stalls.penOf(player.getUUID()) : null;
        if (stall == null && pen == null) {
            say(player, "That horse has no stall, and you have no holding pen. Bind a stall sign to "
                    + "it, or hang a holding pen sign, and try again.");
            return;
        }

        ResourceKey<Level> destination = stall != null ? stall.dimension() : pen.dimension();
        BlockPos signPos = stall != null ? stall.signPos() : pen.signPos();
        ServerLevel target = server.getLevel(destination);
        if (target == null) {
            say(player, stall != null
                    ? "That stall's world is not loaded."
                    : "Your holding pen's world is not loaded.");
            return;
        }

        ItemStack ticket = chooseTicket(player, from.dimension(), destination, stall == null);
        if (ticket == null) {
            say(player, noTicketReason(from.dimension(), destination));
            return;
        }

        Vec3 landing = TicketHandler.landingSpot(target, signPos, horse);
        if (landing == null) {
            // Nothing spent, nothing moved - see TicketHandler, where a guessed
            // landing spot once suffocated a horse inside a wall.
            say(player, stall != null
                    ? "There is nowhere in that stall this horse fits - check the sign is still up, "
                            + "that the stall is still closed in, and that it is big enough."
                    : "There is no room to stand in your holding pen - check the sign is still up and "
                            + "that the pen has a floor and two blocks of headroom.");
            return;
        }

        TicketHandler.arrive(from, target, horse, landing, player);
        if (!player.getAbilities().instabuild) {
            ticket.shrink(1);
        }
        String name = horse.hasCustomName() ? horse.getCustomName().getString() : "The horse";
        say(player, stall != null
                ? name + " is back in its stall."
                : name + " is in your holding pen.");

        HorseProgress.complete(player, ProgressTask.USE_TICKET);
        if (ticket.getItem() instanceof HoldingPenTicketItem) {
            HorseProgress.complete(player, ProgressTask.USE_PEN_TICKET);
        } else if (ticket.getItem() instanceof TicketItem written) {
            switch (written.tier()) {
                case BOUND -> HorseProgress.complete(player, ProgressTask.USE_BOUND_TICKET);
                case INTERDIMENSIONAL ->
                        HorseProgress.complete(player, ProgressTask.USE_INTERDIMENSIONAL_TICKET);
                default -> {
                    // BASIC, already credited above
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Tickets
    // ------------------------------------------------------------------

    /**
     * <b>The cheapest ticket that can actually make this trip</b>, or null when
     * the player holds none that can.
     *
     * <p>A pen trip prefers a holding-pen ticket: it is the purpose-made item,
     * and it is not tier-limited - {@code TicketHandler.sendToPen} reaches a pen
     * from any world, which is deliberate, so it is always a legal spend.
     *
     * <p>Otherwise the written tickets are tried in <b>enum declaration
     * order</b>, which is cheapest-first by construction - BASIC, BOUND,
     * INTERDIMENSIONAL - and the first whose tier {@link TicketHandler#reaches}
     * the destination wins. <b>That ordering is load-bearing</b>: reordering
     * {@link TicketItem.Tier} would silently start spending the dearest ticket
     * in a player's pocket.
     */
    private static @Nullable ItemStack chooseTicket(ServerPlayer player,
                                                    ResourceKey<Level> from,
                                                    ResourceKey<Level> to,
                                                    boolean penTrip) {
        Inventory inventory = player.getInventory();
        ItemStack penTicket = null;
        ItemStack[] byTier = new ItemStack[TicketItem.Tier.values().length];
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof HoldingPenTicketItem) {
                if (penTicket == null) {
                    penTicket = stack;
                }
            } else if (stack.getItem() instanceof TicketItem written) {
                int tier = written.tier().ordinal();
                if (byTier[tier] == null) {
                    byTier[tier] = stack;
                }
            }
        }
        if (penTrip && penTicket != null) {
            return penTicket;
        }
        for (TicketItem.Tier tier : TicketItem.Tier.values()) {
            ItemStack candidate = byTier[tier.ordinal()];
            if (candidate != null && TicketHandler.reaches(tier, from, to)) {
                return candidate;
            }
        }
        return null;
    }

    /** Is it worth pulling a chunk in at all? */
    private static boolean holdsAnyTicket(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() instanceof TicketItem || stack.getItem() instanceof HoldingPenTicketItem) {
                return true;
            }
        }
        return false;
    }

    /** Says which ticket would have worked, rather than only that none did. */
    private static String noTicketReason(ResourceKey<Level> from, ResourceKey<Level> to) {
        if (!from.equals(to)) {
            return "That horse is in another world. Only an interdimensional ticket reaches home from there.";
        }
        return from.equals(Level.OVERWORLD)
                ? "You have no ticket that reaches. A basic ticket would do this."
                : "A basic ticket only works in the overworld. A bound ticket would do this.";
    }

    private static @Nullable Horse findLoaded(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity instanceof Horse horse && horse.isAlive()) {
                return horse;
            }
        }
        return null;
    }

    private static void say(ServerPlayer player, String text) {
        player.sendSystemMessage(Component.literal(text));
    }
}
