package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.progress.ProgressTask;
import com.example.horsegenetics.neoforge.block.HorseStasisBankBlockEntity;
import com.example.horsegenetics.neoforge.data.HorseWhereabouts;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.data.PenRecord;
import com.example.horsegenetics.neoforge.data.StallData;
import com.example.horsegenetics.neoforge.data.StallRecord;
import com.example.horsegenetics.neoforge.data.StasisBankIndex;
import com.example.horsegenetics.neoforge.data.StasisSnapshot;
import com.example.horsegenetics.neoforge.item.HoldingPenTicketItem;
import com.example.horsegenetics.neoforge.item.StasisChamberItem;
import com.example.horsegenetics.neoforge.item.TicketItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
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
 *
 * <h2>Horses that are not entities at all</h2>
 * A horse in a stasis chamber will never load, however long the button waits for
 * it, and its last sighting is where it went <i>in</i> rather than where the jar
 * ended up. So that case branches early into {@link #fromChamber}, which finds
 * the chamber, takes the horse out of it <b>into its stall</b> and leaves the
 * empty jar where the full one was. It is the same rules downstream - the same
 * destination, the same live stall measure, the same ticket spend - because the
 * only thing that differs is where the horse was before it travelled.
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
        // A horse in a chamber is not unloaded, it is not an entity at all, so
        // no amount of pulling chunks in will ever find one. Asked before the
        // sighting is read, because the sighting for a chambered horse is where
        // it went IN - which is a place it is emphatically not.
        if (HorseWhereabouts.get(server).inStasis(horseId)) {
            fromChamber(player, horseId);
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

        Home home = homeOf(player, horse.getUUID());
        if (home == null) {
            say(player, "That horse has no stall, and you have no holding pen. Bind a stall sign to "
                    + "it, or hang a holding pen sign, and try again.");
            return;
        }
        ServerLevel target = server.getLevel(home.dimension());
        if (target == null) {
            say(player, home.worldGoneLine());
            return;
        }

        ItemStack ticket = chooseTicket(player, from.dimension(), home.dimension(), home.pen());
        if (ticket == null) {
            say(player, noTicketReason(from.dimension(), home.dimension()));
            return;
        }

        Vec3 landing = TicketHandler.landingSpot(target, home.signPos(), horse);
        if (landing == null) {
            // Nothing spent, nothing moved - see TicketHandler, where a guessed
            // landing spot once suffocated a horse inside a wall.
            say(player, home.noRoomLine());
            return;
        }

        TicketHandler.arrive(from, target, horse, landing, player);
        if (!player.getAbilities().instabuild) {
            ticket.shrink(1);
        }
        String name = horse.hasCustomName() ? horse.getCustomName().getString() : "The horse";
        say(player, name + home.arrivedLine());

        creditTicket(player, ticket);
    }

    // ------------------------------------------------------------------
    // Where home is
    // ------------------------------------------------------------------

    /**
     * <b>Where a horse belongs, and the three sentences that go with it.</b>
     *
     * <p>Its own stall first; the holding pen only when it has none. A horse with
     * a stall is never sent to the pen, even if a pen ticket is the cheaper spend
     * - the stall is where that horse belongs.
     *
     * <p>The wording travels with the destination rather than being chosen again
     * at each refusal, because there are now two callers - a horse standing in a
     * field and a horse in a bottle - and "check the sign is still up" has to be
     * the same advice from both.
     */
    private record Home(ResourceKey<Level> dimension, BlockPos signPos, boolean pen) {

        String worldGoneLine() {
            return pen ? "Your holding pen's world is not loaded." : "That stall's world is not loaded.";
        }

        String noRoomLine() {
            return pen
                    ? "There is no room to stand in your holding pen - check the sign is still up and "
                            + "that the pen has a floor and two blocks of headroom."
                    : "There is nowhere in that stall this horse fits - check the sign is still up, "
                            + "that the stall is still closed in, and that it is big enough.";
        }

        String arrivedLine() {
            return pen ? " is in your holding pen." : " is back in its stall.";
        }
    }

    private static @Nullable Home homeOf(ServerPlayer player, UUID horseId) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return null;
        }
        StallData stalls = StallData.get(server);
        StallRecord stall = stalls.forHorse(horseId);
        if (stall != null) {
            return new Home(stall.dimension(), stall.signPos(), false);
        }
        PenRecord pen = stalls.penOf(player.getUUID());
        return pen == null ? null : new Home(pen.dimension(), pen.signPos(), true);
    }

    /** The checklist tasks a spent ticket ticks off, whichever path spent it. */
    private static void creditTicket(ServerPlayer player, ItemStack ticket) {
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
    // Out of the bottle
    // ------------------------------------------------------------------

    /**
     * <b>Send home a horse that is in a stasis chamber.</b> The chamber is found,
     * the horse comes out of it directly into its stall, and the <b>empty jar is
     * left where the full one was</b> - in the bank slot it was filed in, or in
     * the pocket it was being carried in.
     *
     * <h2>Why it is not "release, then send"</h2>
     * Because a release that is followed by a refusal is a horse standing in
     * somebody's cellar with the bottle spent. Everything that can say no is asked
     * <b>before</b> {@link HorseStasisHandler#place} is called: the chamber is
     * found, the destination resolved, the horse {@link
     * HorseStasisHandler#restore restored} but not put down, the stall measured
     * against that horse's real box, and the ticket chosen. Any of those failing
     * leaves the chamber exactly as full as it was, which is the same promise
     * {@code StasisChamberItem.useOn} makes.
     *
     * <p>The horse has to exist to be measured, which is why the restore happens
     * in the middle rather than at the end - {@code TicketHandler.landingSpot}
     * sizes the room against the animal, and a shire does not fit where a
     * Shetland does.
     */
    private static void fromChamber(ServerPlayer player, UUID horseId) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        Chamber chamber = findChamber(server, player, horseId);
        if (chamber == null) {
            say(player, "That horse is in a stasis chamber, and the chamber is not on you or in one "
                    + "of your banks. Bring it back, or let the horse out by hand.");
            return;
        }
        StasisSnapshot snapshot = StasisChamberItem.snapshotOf(chamber.stack());
        if (snapshot == null) {
            say(player, "That chamber is empty.");   // the index was stale; the container wins
            return;
        }
        if (HorseStasisHandler.alreadyLoose(server, snapshot)) {
            say(player, snapshot.horseName() + " is already out in the world - that chamber is a copy.");
            return;
        }

        Home home = homeOf(player, horseId);
        if (home == null) {
            say(player, "That horse has no stall, and you have no holding pen. Bind a stall sign to "
                    + "it, or hang a holding pen sign, and try again.");
            return;
        }
        ServerLevel target = server.getLevel(home.dimension());
        if (target == null) {
            say(player, home.worldGoneLine());
            return;
        }

        Horse horse = HorseStasisHandler.restore(target, snapshot);
        if (horse == null) {
            say(player, snapshot.horseName() + " could not be let out - the chamber is unchanged.");
            return;
        }
        if (!HorseOwnership.isOwner(horse, player.getUUID())) {
            say(player, "That horse does not answer to you.");
            return;
        }
        Vec3 landing = TicketHandler.landingSpot(target, home.signPos(), horse);
        if (landing == null) {
            say(player, home.noRoomLine());
            return;
        }
        // The trip starts where the bottle is, not where the horse went in, and
        // that is what the tier has to reach across.
        ItemStack ticket = chooseTicket(player, chamber.dimension(), home.dimension(), home.pen());
        if (ticket == null) {
            say(player, noTicketReason(chamber.dimension(), home.dimension()));
            return;
        }

        if (HorseStasisHandler.place(target, horse, landing, horse.getYRot()) == null) {
            say(player, snapshot.horseName() + " could not be let out - the chamber is unchanged.");
            return;
        }
        chamber.empty();
        if (!player.getAbilities().instabuild) {
            ticket.shrink(1);
        }
        target.sendParticles(ParticleTypes.PORTAL, landing.x, landing.y + 0.8, landing.z,
                24, 0.4, 0.6, 0.4, 0.2);
        target.playSound(null, landing.x, landing.y, landing.z, SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.NEUTRAL, 1.0F, 1.0F);
        say(player, snapshot.horseName() + home.arrivedLine() + " The chamber is empty again.");
        ActionTrace.log("stasis", snapshot.horseName() + " sent home out of a chamber "
                + chamber.where() + " -> " + (home.pen() ? "holding pen" : "stall") + " at "
                + home.signPos().toShortString());
        creditTicket(player, ticket);
    }

    /**
     * One chamber, wherever it is sitting - a slot of the player's inventory or a
     * slot of one of their banks' grids. Both are {@link Container}s, so the only
     * thing that genuinely differs is which world the jar is in and what to call
     * the place in a trace line.
     */
    private record Chamber(Container container, int slot, ResourceKey<Level> dimension, String where) {

        ItemStack stack() {
            return container.getItem(slot);
        }

        /**
         * <b>Leave the empty jar in there.</b> An empty chamber is a different
         * item from a full one, so this is a swap in the slot rather than an edit
         * - see {@code StasisChamberItem.withoutHorse}. The at-stud mark goes with
         * the horse: an empty bottle is not standing at stud.
         */
        void empty() {
            ItemStack emptied = StasisChamberItem.withoutHorse(stack());
            emptied.remove(ModDataComponents.STASIS_AT_STUD.get());
            container.setItem(slot, emptied);
        }
    }

    /**
     * <b>Find the jar.</b> The player's own pockets first, which costs a walk of
     * an inventory and touches no disk; then the one bank {@link StasisBankIndex}
     * says has that horse filed in it, which costs exactly one chunk load.
     *
     * <p>The index is re-checked against the real grid rather than trusted, for
     * the reason its own doc gives: a stale row costs a wasted look and finds
     * nothing, which is the right way round.
     */
    private static @Nullable Chamber findChamber(MinecraftServer server, ServerPlayer player,
                                                 UUID horseId) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            StasisSnapshot snapshot = StasisChamberItem.snapshotOf(inventory.getItem(slot));
            if (snapshot != null && horseId.equals(snapshot.horseId())) {
                return new Chamber(inventory, slot, player.level().dimension(), "in your pack");
            }
        }

        StasisBankIndex index = StasisBankIndex.get(server);
        StasisBankIndex.Bank record = index.bankHolding(player.getUUID(), horseId);
        if (record == null) {
            return null;
        }
        ServerLevel level = server.getLevel(record.dimension());
        if (level == null) {
            index.forget(record.dimension(), record.pos());
            return null;
        }
        // Loads the chunk - the one expensive thing here, spent only on a bank
        // that has said it is holding this exact horse.
        if (!(level.getBlockEntity(record.pos()) instanceof HorseStasisBankBlockEntity bank)) {
            index.forget(record.dimension(), record.pos());
            return null;
        }
        Container chambers = bank.chambers();
        for (int slot = 0; slot < chambers.getContainerSize(); slot++) {
            StasisSnapshot snapshot = StasisChamberItem.snapshotOf(chambers.getItem(slot));
            if (snapshot != null && horseId.equals(snapshot.horseId())) {
                return new Chamber(chambers, slot, record.dimension(),
                        "in the bank at " + record.pos().toShortString());
            }
        }
        return null;
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
