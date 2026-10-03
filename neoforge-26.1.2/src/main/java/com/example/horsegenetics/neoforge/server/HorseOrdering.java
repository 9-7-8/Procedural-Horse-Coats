package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.care.HorseOrder;
import com.example.horsegenetics.common.care.HorseOrders;
import com.example.horsegenetics.common.care.HorseOrders.Refusal;
import com.example.horsegenetics.common.care.HorseOrders.Situation;
import com.example.horsegenetics.common.progress.ProgressTask;
import com.example.horsegenetics.neoforge.carts.util.CartWorld;
import com.example.horsegenetics.neoforge.data.HorseCareAttachment;
import com.example.horsegenetics.neoforge.data.HorseOrderAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * <b>The command whistle's orders, on the server</b> (wiki/item-whistles.html#command).
 * Gives them - re-checking everything the client already greyed out, because the client
 * decides nothing - reads them back, and clears them.
 *
 * <p><b>Clearing is lazy where it can be.</b> An order belongs to the player who gave it,
 * in the dimension it was given in. {@link #current} drops it the moment either stops
 * being true - the horse was sold, transferred, or changed dimension - so no hook in the
 * ownership or portal code is needed. The places that move a horse within one dimension
 * call {@link #clear} themselves: a whistle or ender-whistle recall (calling a horse
 * means you want it), Send home, and storing it in a stasis chamber or stall bank.
 *
 * <p><b>Go home is not stored at all.</b> It is the Send home trip ({@link StallRecall#goHome}),
 * run the moment the order is given, which clears whatever order the horse had.
 */
public final class HorseOrdering {

    private HorseOrdering() {
    }

    /**
     * This horse's order, after dropping one that no longer holds. Every order goal's
     * canUse and every suppressed goal asks this, so it stays one attachment read for a
     * horse with no order.
     */
    public static HorseOrderAttachment current(AbstractHorse horse) {
        // hasData first: getData on a horse without one CREATES the default, and a synced
        // attachment is then sent to every tracking client and saved - for every tamed
        // horse BondFollowGoal asks about, which is all of them.
        if (!horse.hasData(ModAttachments.HORSE_ORDER.get())) {
            return HorseOrderAttachment.NONE;
        }
        HorseOrderAttachment o = horse.getData(ModAttachments.HORSE_ORDER.get());
        if (!o.hasOrder() || horse.level().isClientSide()) {
            return o;
        }
        boolean otherDimension = !o.dimension().equals(dimensionOf(horse));
        boolean otherOwner = !(horse instanceof Horse h)
                || o.orderedBy().map(id -> !HorseOwnership.isOwner(h, id)).orElse(true);
        // A one-shot is never stored; one that somehow was (a hand-edited save) is dropped.
        if (otherDimension || otherOwner || o.order().oneShot()) {
            clear(horse);
            return HorseOrderAttachment.NONE;
        }
        return o;
    }

    /** Does an order stand on this horse? The cheap question the suppressed goals ask. */
    public static boolean hasOrder(AbstractHorse horse) {
        return current(horse).hasOrder();
    }

    /** Back to no order. A no-op (and no sync) on a horse that has none. */
    public static void clear(AbstractHorse horse) {
        if (horse.hasData(ModAttachments.HORSE_ORDER.get())
                && horse.getData(ModAttachments.HORSE_ORDER.get()).hasOrder()) {
            horse.setData(ModAttachments.HORSE_ORDER.get(), HorseOrderAttachment.NONE);
        }
    }

    /** What this horse is doing that an order has to get past, and whether it was bred to fight. */
    public static Situation situation(Horse horse) {
        HorseCareAttachment care = horse.getData(ModAttachments.HORSE_CARE.get());
        return new Situation(care == null ? 0 : care.behaviourTier(), horse.isLeashed(),
                CartWorld.get(horse.level()).isPulling(horse), fighter(HorseRecords.of(horse)));
    }

    /**
     * Was the horse with this record bred to fight ({@link HorseOrders#fighter})? Server and
     * client both ask it from the record - the client has it from HorseRecordSyncPayload -
     * so the wheel greys exactly what the server would refuse. An unreadable code is not a
     * fighter: a horse whose genes cannot be read is not sent after monsters.
     */
    public static boolean fighter(com.example.horsegenetics.common.horse.HorseRecord record) {
        if (record == null || !record.hasName()) {
            return false;
        }
        try {
            return HorseOrders.fighter(com.example.horsegenetics.common.genetics.Genotype.parse(record.geneticCode()),
                    com.example.horsegenetics.common.genetics.Epigenome.parse(record.epigenomeCode()));
        } catch (RuntimeException unreadable) {
            return false;
        }
    }

    /**
     * Give {@code order} to each horse, and return each one's answer (null = obeyed), in
     * the same order. Anything not {@code player}'s own, out of reach or in another level
     * is skipped without an answer: the client should never have offered it.
     */
    public static List<Refusal> give(ServerPlayer player, List<Horse> horses, HorseOrder order) {
        List<Refusal> answers = new ArrayList<>();
        ServerLevel level = player.level();
        double reach = HorseOrders.REACH_BLOCKS + 2; // a little slack for a horse that took a step
        for (Horse horse : horses) {
            if (horse.level() != level || !horse.isAlive() || horse.distanceToSqr(player) > reach * reach
                    || !HorseOwnership.isOwner(horse, player.getUUID())) {
                continue;
            }
            Refusal refusal = HorseOrders.refusal(order, situation(horse));
            if (refusal == null && order == HorseOrder.GO_HOME) {
                // A one-shot: the Send home trip, done now. It clears any order on arrival
                // and leaves nothing standing, so a horse never holds Go home.
                refusal = StallRecall.goHome(player, horse);
            }
            answers.add(refusal);
            if (refusal != null || order.oneShot()) {
                continue;
            }
            if (order == HorseOrder.REJOIN_HERD) {
                clear(horse);
            } else {
                horse.setData(ModAttachments.HORSE_ORDER.get(), new HorseOrderAttachment(order,
                        order.anchored() ? Optional.of(groundAt(horse.level(), horse.blockPosition())) : Optional.empty(),
                        dimensionOf(horse), Optional.of(player.getUUID())));
                horse.getNavigation().stop();
            }
        }
        if (answers.stream().anyMatch(r -> r == null)) {
            HorseProgress.complete(player, ProgressTask.COMMAND_WHISTLE);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS, 0.8F, 1.3F);
        }
        return answers;
    }

    /** The order given by the payload: one aimed horse, or every one of the player's in reach. */
    public static void handle(ServerPlayer player, List<Integer> entityIds, boolean all, HorseOrder order) {
        ServerLevel level = player.level();
        List<Horse> horses = new ArrayList<>();
        if (all) {
            level.getEntitiesOfClass(Horse.class, player.getBoundingBox().inflate(HorseOrders.REACH_BLOCKS),
                    h -> h.isAlive() && h.getControllingPassenger() != player
                            && HorseOwnership.isOwner(h, player.getUUID())).forEach(horses::add);
        } else {
            for (int id : entityIds) {
                Entity e = level.getEntity(id);
                if (e instanceof Horse h) {
                    horses.add(h);
                }
            }
        }
        List<Refusal> answers = give(player, horses, order);
        if (all) {
            player.sendSystemMessage(Component.literal(HorseOrders.summary(order, answers)));
        } else if (answers.size() == 1 && !horses.isEmpty()) {
            player.sendSystemMessage(Component.literal(HorseOrders.oneLine(
                    HorseRecords.of(horses.get(0)).displayName(), order, answers.get(0))));
        }
    }

    /** The player who gave the order, if they are here: whom Follow and Defend me stay with. */
    static ServerPlayer orderedByHere(AbstractHorse horse, HorseOrderAttachment o) {
        if (o.orderedBy().isEmpty() || !(horse.level() instanceof ServerLevel level)) {
            return null;
        }
        UUID id = o.orderedBy().get();
        return level.getPlayerByUUID(id) instanceof ServerPlayer p && p.isAlive() && !p.isSpectator() ? p : null;
    }

    /**
     * The spot a horse can stand on at or just under {@code pos}: a horse ordered mid-jump,
     * or still falling, would otherwise be anchored in the air where no path reaches
     * (found by the order_stay_walks_back gametest, whose horse was still landing).
     */
    public static net.minecraft.core.BlockPos groundAt(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        net.minecraft.core.BlockPos p = pos;
        for (int i = 0; i < 4; i++) {
            net.minecraft.core.BlockPos below = p.below();
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
                return p;
            }
            p = below;
        }
        return pos; // nothing under it within four blocks: keep where it was told
    }

    static String dimensionOf(Entity entity) {
        return entity.level().dimension().identifier().toString();
    }
}
