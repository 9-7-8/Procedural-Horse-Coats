package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.data.PenRecord;
import com.example.horsegenetics.neoforge.data.StallData;
import com.example.horsegenetics.neoforge.data.StallRecord;
import com.example.horsegenetics.neoforge.item.HoldingPenTicketItem;
import com.example.horsegenetics.neoforge.item.ModItems;
import com.example.horsegenetics.neoforge.item.TicketItem;
import com.example.horsegenetics.neoforge.item.TurnoutTicketItem;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import com.example.horsegenetics.common.progress.ProgressTask;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * <b>Spending a ticket</b> - right-click your horse with one and it goes to its
 * stall. See {@link TicketItem} for what the tiers mean.
 *
 * <p>Same trap as every other horse interaction: {@code EntityInteract} fires on
 * both sides and vanilla turns any item used on a tamed horse into a mount, so
 * the event is cancelled on <b>both</b> sides and everything happens server-only
 * (see {@link HorseInteractionHandler}).
 *
 * <p>Every refusal says why. A ticket is a consumable a player crafted on
 * purpose, and one that silently does nothing is indistinguishable from a bug -
 * so nothing is spent unless the horse actually moved.
 */
@EventBusSubscriber
public final class TicketHandler {

    private TicketHandler() {
    }

    @SubscribeEvent
    static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Horse horse)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof HoldingPenTicketItem) {
            if (!event.getLevel().isClientSide() && horse.level() instanceof ServerLevel level) {
                sendToPen(level, horse, event.getEntity(), stack);
            }
            consume(event);
            return;
        }
        if (stack.getItem() instanceof TurnoutTicketItem) {
            if (!event.getLevel().isClientSide() && horse.level() instanceof ServerLevel level) {
                turnOut(level, horse, event.getEntity(), stack);
            }
            consume(event);
            return;
        }
        if (!(stack.getItem() instanceof TicketItem ticket)) {
            // The blank is the crafting base and does nothing, but it should say
            // so rather than mounting the horse the player just poked.
            if (stack.is(ModItems.BLANK_TICKET.get())) {
                if (!event.getLevel().isClientSide()) {
                    say(event.getEntity(), "A blank ticket is not written on yet - craft it up first.");
                }
                consume(event);
            }
            return;
        }

        Player player = event.getEntity();
        if (!event.getLevel().isClientSide() && horse.level() instanceof ServerLevel level) {
            send(level, horse, player, stack, ticket.tier());
        }
        consume(event);
    }

    private static void send(ServerLevel level, Horse horse, Player player, ItemStack stack,
                             TicketItem.Tier tier) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }
        if (!ownedBy(horse, player.getUUID())) {
            say(player, notYours(horse));
            return;
        }
        StallRecord stall = StallData.get(server).forHorse(horse.getUUID());
        if (stall == null) {
            say(player, "This horse has no stall. Bind a stall sign to it and hang the sign up first.");
            return;
        }
        ServerLevel target = server.getLevel(stall.dimension());
        if (target == null) {
            say(player, "That stall's world is not loaded.");
            return;
        }
        if (!reaches(tier, level.dimension(), stall.dimension())) {
            say(player, refusal(tier, level.dimension(), stall.dimension()));
            return;
        }
        if (horse.isVehicle()) {
            say(player, "Get off first - a horse cannot travel with a rider.");
            return;
        }

        Vec3 landing = landingSpot(target, stall.signPos(), horse);
        if (landing == null) {
            // Nothing is spent and nothing moves. A horse that quietly fails to
            // arrive is indistinguishable from a horse that was deleted, and
            // the version that guessed a spot instead of refusing put one
            // inside a wall, where it suffocated.
            say(player, "There is nowhere in that stall this horse fits - check the sign is still up, "
                    + "that the stall is still closed in, and that it is big enough for this horse.");
            return;
        }
        arrive(level, target, horse, landing, player);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        say(player, (stall.horseName().isBlank() ? "The horse" : stall.horseName()) + " is back in its stall.");
        HorseProgress.complete(player, ProgressTask.USE_TICKET);
        // The tier as well as the act: the three written tickets are three
        // separate crafts and three separate reaches, and a player who has only
        // ever used the basic one has not met the other two.
        switch (tier) {
            case BOUND -> HorseProgress.complete(player, ProgressTask.USE_BOUND_TICKET);
            case INTERDIMENSIONAL ->
                    HorseProgress.complete(player, ProgressTask.USE_INTERDIMENSIONAL_TICKET);
            default -> {
                // BASIC, already credited
            }
        }
    }

    /**
     * <b>A holding pen ticket</b>: any horse the player owns, to the player's one
     * holding pen, from any world. The same checks and the same live landing as
     * a stall ticket, because a pen is a stall that belongs to a player instead
     * of a horse - see {@code HoldingPenSignItem}.
     */
    private static void sendToPen(ServerLevel level, Horse horse, Player player, ItemStack stack) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }
        if (!ownedBy(horse, player.getUUID())) {
            say(player, notYours(horse));
            return;
        }
        PenRecord pen = StallData.get(server).penOf(player.getUUID());
        if (pen == null) {
            say(player, "You have no holding pen yet. Hang a holding pen sign on a pen's wall first.");
            return;
        }
        ServerLevel target = server.getLevel(pen.dimension());
        if (target == null) {
            say(player, "Your holding pen's world is not loaded.");
            return;
        }
        if (horse.isVehicle()) {
            say(player, "Get off first - a horse cannot travel with a rider.");
            return;
        }
        Vec3 landing = landingSpot(target, pen.signPos(), horse);
        if (landing == null) {
            say(player, "There is no room to stand in your holding pen - check the sign is still up and "
                    + "that the pen has a floor and two blocks of headroom.");
            return;
        }
        arrive(level, target, horse, landing, player);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        String name = horse.hasCustomName() ? horse.getCustomName().getString() : "The horse";
        say(player, name + " is in your holding pen.");
        HorseProgress.complete(player, ProgressTask.USE_PEN_TICKET);
    }

    /**
     * <b>A turnout ticket</b>: any horse the player owns, to the horse realm's
     * arrival field, <b>and given up there</b> - see {@link TurnoutTicketItem}.
     *
     * <p>It is the only ticket with no destination of its own to find. Every
     * other one lands on a stall or a pen the player built, and refuses when
     * that is gone; the realm has exactly one entrance
     * ({@link HorseRealm#arrivalSpot}) and it is flat ground the dimension
     * guarantees, so there is nothing here that can fail late.
     *
     * <p><b>Order matters.</b> The horse is moved first and released second, so
     * {@code HorseRelease.makeWild} hands the tack back to a player who is
     * standing in the world the horse just left - rather than dropping a saddle
     * on the realm floor where they cannot reach it. Everything else about what
     * "wild" means is that method's business and deliberately not repeated here.
     */
    private static void turnOut(ServerLevel level, Horse horse, Player player, ItemStack stack) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }
        if (!ownedBy(horse, player.getUUID())) {
            say(player, notYours(horse));
            return;
        }
        ServerLevel realm = server.getLevel(HorseRealm.REALM_LEVEL);
        if (realm == null) {
            say(player, "The horse realm is not loaded on this server.");
            return;
        }
        if (horse.isVehicle()) {
            say(player, "Get off first - a horse cannot travel with a rider.");
            return;
        }

        String name = HorseRecords.hasRealRecord(horse)
                ? HorseRecords.of(horse).displayName()
                : (horse.hasCustomName() ? horse.getCustomName().getString() : "The horse");

        arrive(level, realm, horse, HorseRealm.arrivalSpot(), player);
        HorseRelease.makeWild(realm, horse, player);
        // A stall bound to a horse that is no longer yours is a sign nobody can
        // rebind. Neither existing release path clears one, because neither can
        // reach a horse that HAS one: the freedom stick only works inside the
        // realm and HorseRealmFeral only fires on a horse already there. This
        // one starts wherever the horse lives, so it is the first release that
        // has to tidy up after itself.
        StallData.get(server).removeHorse(horse.getUUID());

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        say(player, name + " is loose in the horse realm, and is nobody's now.");
        ActionTrace.log("ticket", player.getGameProfile().name() + " turned out "
                + ActionTrace.describeShort(horse) + " into the realm - unowned, stall binding cleared");
    }

    /**
     * Move the horse, with a puff and a sound at both ends so the player sees it go.
     *
     * <p>{@code player} is here only to be handed the lead: an interdimensional
     * ticket on a leashed horse used to leave the lead in the dimension it
     * started in. See {@link HorseLeads}.
     */
    // Package-private, not private: StallRecall spends tickets too, from the
    // horse browser's Send home button, and it must use THESE rules rather
    // than a second copy of them. See that class.
    static void arrive(ServerLevel from, ServerLevel target, Horse horse, Vec3 landing,
                               Player player) {
        from.sendParticles(ParticleTypes.PORTAL, horse.getX(), horse.getY() + 0.8, horse.getZ(),
                24, 0.4, 0.6, 0.4, 0.2);
        from.playSound(null, horse.getX(), horse.getY(), horse.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0F, 1.0F);

        HorseLeads.untieFor(horse, player);
        horse.teleportTo(target, landing.x, landing.y, landing.z,
                Set.of(), horse.getYRot(), horse.getXRot(), false);

        target.sendParticles(ParticleTypes.PORTAL, landing.x, landing.y + 0.8, landing.z,
                24, 0.4, 0.6, 0.4, 0.2);
        target.playSound(null, landing.x, landing.y, landing.z, SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    /** Does a ticket of this tier reach from {@code from} to {@code to}? */
    // Package-private, not private: StallRecall spends tickets too, from the
    // horse browser's Send home button, and it must use THESE rules rather
    // than a second copy of them. See that class.
    static boolean reaches(TicketItem.Tier tier, net.minecraft.resources.ResourceKey<Level> from,
                                   net.minecraft.resources.ResourceKey<Level> to) {
        return switch (tier) {
            case BASIC -> from.equals(Level.OVERWORLD) && to.equals(Level.OVERWORLD);
            case BOUND -> from.equals(to);
            case INTERDIMENSIONAL -> true;
        };
    }

    private static String refusal(TicketItem.Tier tier, net.minecraft.resources.ResourceKey<Level> from,
                                  net.minecraft.resources.ResourceKey<Level> to) {
        if (tier == TicketItem.Tier.BASIC && from.equals(to)) {
            // Same world, but not the overworld - the next ticket up covers it.
            return "A basic ticket only works in the overworld. A bound ticket would do this.";
        }
        return "This horse's stall is in another world. Only an interdimensional ticket reaches it.";
    }

    /**
     * <b>Where in the stall to put the horse, or {@code null} if nowhere.</b>
     *
     * <p>The stall is measured <b>now</b>, not read back from the record. Two
     * reasons, and the first one cost a horse: the stored span is a bounding
     * box, and a box around any room that is not a plain cuboid contains the
     * walls inside it. The second is that a player rebuilds stalls - re-running
     * the detector means a stall that has been widened, floored or re-fenced
     * since the sign went up is the stall the horse arrives in.
     *
     * <p>The chunk is pulled in first. A horse teleported into unloaded terrain
     * is the failure that looks exactly like a horse that was deleted.
     */
    // Package-private, not private: StallRecall spends tickets too, from the
    // horse browser's Send home button, and it must use THESE rules rather
    // than a second copy of them. See that class.
    static Vec3 landingSpot(ServerLevel level, BlockPos signPos, Horse horse) {
        level.getChunk(signPos); // load it, so what we read is real and the horse arrives somewhere
        BlockState sign = level.getBlockState(signPos);
        if (!(sign.getBlock() instanceof WallSignBlock)) {
            return null; // the sign is gone - so is the stall it was naming
        }
        Direction facing = sign.getValue(WallSignBlock.FACING);
        StallDetector.Result live =
                StallDetector.forSign(level, signPos.relative(facing.getOpposite()), facing);
        // NULL-SAFE: forSign refuses now instead of inventing a box, so a stall
        // whose gate has been taken out since it was bound comes back null. This
        // passed that straight into landingSpot, which would have thrown.
        Vec3 spot = live == null ? null : StallDetector.landingSpot(level, live, horse);
        // ONE LINE PER TICKET, because "the horse landed in the roof" has two
        // explanations the code alone cannot tell apart - the wrong room, or the
        // right room and the wrong spot in it - and the owner's report that it
        // arrived "right above the sign" matches neither reading of the code.
        ActionTrace.log("ticket", ActionTrace.describeShort(horse) + " -> stall sign at "
                + signPos.toShortString() + " facing " + facing.getName() + ": "
                + (live == null
                        ? "no enclosed room there any more"
                        : "room " + live.min().toShortString() + " to " + live.max().toShortString()
                                + " (" + live.blockCount() + " tiles)")
                + ", horse box " + String.format("%.2f x %.2f", horse.getBbWidth(), horse.getBbHeight())
                + " -> " + (spot == null
                        ? "NOTHING FITS - refused"
                        : String.format("landing at %.2f, %.2f, %.2f", spot.x, spot.y, spot.z)));
        return spot;
    }

    private static boolean ownedBy(Horse horse, UUID playerId) {
        if (!horse.isTamed()) {
            return false;
        }
        EntityReference<LivingEntity> owner = horse.getOwnerReference();
        return owner != null && playerId.equals(owner.getUUID());
    }

    /**
     * <b>Why a ticket will not take this horse</b>, in the words that say what
     * to do about it. The two tickets used to answer the same check with two
     * different sentences, and the stall ticket's was only "That is not your
     * horse." - said to the owner standing beside an untamed test mare labelled
     * TAME ME (2026-09-13). True, and no help at all. The pen ticket had the
     * opposite fault: "tame it first" to a horse somebody else had tamed.
     */
    private static String notYours(Horse horse) {
        return horse.isTamed()
                ? "That horse belongs to someone else."
                : "That horse is not tamed yet - tame it first, then use the ticket.";
    }

    private static void say(Player player, String text) {
        player.sendSystemMessage(Component.literal(text));
    }

    private static void consume(PlayerInteractEvent.EntityInteract event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
