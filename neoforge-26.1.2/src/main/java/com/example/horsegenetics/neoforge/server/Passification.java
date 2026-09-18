package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.genes.PassificationGene;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.data.PassificationAttachment;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;
import java.util.UUID;

/**
 * The translator for {@code horsegenetics.passification} - <b>the way in to a
 * horse that would otherwise kill you</b>.
 *
 * <p>The gene ({@link PassificationGene}) owns what a horse will accept and what
 * that buys; this owns the offering itself, the clock, and the <b>veto</b>.
 * Same split as lycanthropy: the locus would have been an {@code effects} verb
 * with exactly one user.
 *
 * <h2>The veto is the whole point, and it lives at the chokepoints</h2>
 * {@link #suppresses} is asked at <b>every</b> place a horse acquires a target -
 * the temper verb, the wild aggro-and-alert path, the herd alarm, and guardian's
 * retaliation. "Will not select the player as a target for any reason" is the
 * requirement, and the only way to honour "any reason" is to ask at each reason
 * rather than to unpick them one at a time afterwards. A gene added later that
 * targets a player and does not ask here is a bug in that gene.
 *
 * <p><b>Targeting only.</b> A damaging aura goes on damaging; a player who walks
 * into one has not been betrayed by this gene. That is the owner's line and it
 * keeps the veto to one question.
 *
 * <h2>Per player, not per horse</h2>
 * Passifying is a bargain between an animal and the person who fed it. A horse
 * calmed by one player is still perfectly willing to kill their friend, which is
 * also what stops one tamed horse defusing an aggressive line for a whole
 * server.
 *
 * <p><b>Not verified in-game.</b> Written against 26.1.2 sources; the interaction
 * event and {@code broadcastEntityEvent} feedback in particular are the shapes
 * the rest of this package uses, not ones observed for this handler.
 */
@EventBusSubscriber
public final class Passification {

    private Passification() {
    }

    /**
     * <b>Is this horse forbidden from targeting that creature?</b> Only ever
     * true of a player - nothing else can be passified, because nothing else can
     * offer anything.
     */
    public static boolean suppresses(Horse horse, LivingEntity target) {
        if (!(target instanceof Player player)) {
            return false;
        }
        return horse.getData(ModAttachments.PASSIFICATION.get())
                .calm(player.getUUID(), horse.level().getGameTime());
    }

    /**
     * Drop a target the horse is no longer allowed to have. Called when a calm
     * is granted, so the animal stops mid-charge rather than at its next scan.
     */
    private static void forget(Horse horse, Player player) {
        if (horse.getTarget() == player) {
            horse.setTarget(null);
        }
        if (horse.getLastHurtByMob() == player) {
            horse.setLastHurtByMob(null);
        }
    }

    // ------------------------------------------------------------------
    // The offering
    // ------------------------------------------------------------------

    @SubscribeEvent
    static void onOffer(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Horse horse)) {
            return;
        }
        List<PassificationGene.Route> routes = routesOf(horse);
        if (routes.isEmpty()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }
        String offered = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        Player player = event.getEntity();
        long now = horse.level().getGameTime();

        for (PassificationGene.Route route : routes) {
            if (!route.item().equals(offered) || !windowOpen(route, horse)) {
                continue;
            }
            // Cancel on BOTH sides, as every other claimed interaction here does:
            // a client that predicts a mount it then has to take back is worse
            // than a click that does nothing.
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (!event.getLevel().isClientSide()) {
                accept(horse, player, route, stack, now);
            }
            return;
        }
    }

    /**
     * One mouthful. Part-payment is remembered, and the calm is granted on the
     * mouthful that completes it.
     */
    private static void accept(Horse horse, Player player, PassificationGene.Route route,
                               ItemStack stack, long now) {
        PassificationAttachment state = horse.getData(ModAttachments.PASSIFICATION.get());
        UUID id = player.getUUID();

        // A horse already permanently settled toward this player wants nothing
        // more; taking the food anyway would be a quiet theft.
        if (state.permanent(id)) {
            return;
        }
        if (route.kind() == PassificationGene.Kind.TEMPORARY
                && state.calm(id, now)) {
            return; // already calm - do not spend food topping it up
        }
        if (route.kind() == PassificationGene.Kind.TEMPORARY
                && !state.offAsCooldown(id, now, route.cooldownTicks())) {
            return; // too soon since the last one
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        state = state.feed(id, route.item());

        if (state.fed(id, route.item()) >= route.amount()) {
            long end = route.kind() == PassificationGene.Kind.PERMANENT
                    ? PassificationAttachment.FOREVER
                    : now + route.durationTicks();
            state = state.settle(id, route.item(), end, now);
            forget(horse, player);
            horse.level().broadcastEntityEvent(horse, (byte) 7); // hearts
            ActionTrace.log("passified", ActionTrace.describeShort(horse) + " by "
                    + player.getName().getString() + " with " + route.amount() + "x "
                    + route.item()
                    + (route.kind() == PassificationGene.Kind.PERMANENT
                            ? " - permanently"
                            : " - for " + route.durationTicks() + " ticks"));
        } else {
            horse.level().broadcastEntityEvent(horse, (byte) 6); // smoke: not yet
        }
        horse.setData(ModAttachments.PASSIFICATION.get(), state);
    }

    /** Is this route open at the horse's present age? */
    private static boolean windowOpen(PassificationGene.Route route, Horse horse) {
        return switch (route.window()) {
            case ANY -> true;
            case FOAL -> horse.isBaby();
            case ADULT -> !horse.isBaby();
        };
    }

    /**
     * Every way into this horse, or an empty list. A horse with no record yet -
     * one that has not been through the spawn handler - offers nothing rather
     * than throwing.
     */
    private static List<PassificationGene.Route> routesOf(Horse horse) {
        HorseRecord record = HorseRecords.of(horse);
        if (!record.hasName()) {
            return List.of();
        }
        try {
            Genotype genotype = Genotype.parse(record.geneticCode());
            Epigenome epigenome = Epigenome.parse(record.epigenomeCode());
            return Genes.PASSIFICATION.routesOf(genotype.pair(Genes.PASSIFICATION),
                    GeneEpigenetics.forGene(Genes.PASSIFICATION, genotype, epigenome));
        } catch (RuntimeException e) {
            return List.of();
        }
    }
}
