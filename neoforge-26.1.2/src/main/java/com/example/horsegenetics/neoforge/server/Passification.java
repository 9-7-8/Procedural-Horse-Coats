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
 * <p><b>Fleeing asks too.</b> The temper verb's {@code flee} mood used to pick
 * its nearest scary thing without asking this at all, so a horse could be
 * calmed toward a player and still bolt from them - the same class of bug the
 * paragraph above warns about, just not caught until a breed needed it.
 *
 * <p><b>Targeting only.</b> A damaging aura goes on damaging; a player who walks
 * into one has not been betrayed by this gene. That is the owner's line and it
 * keeps the veto to one question.
 *
 * <h2>Per player - unless it is permanent</h2>
 * A <b>temporary</b> calm is a bargain between an animal and the person who fed
 * it. A horse calmed by one player is still perfectly willing to kill their
 * friend, which is also what stops one tamed horse defusing an aggressive line
 * for a whole server.
 *
 * <p>A <b>permanent</b> calm ({@code Kind.PERMANENT} - {@code Prm}, {@code FPr},
 * {@code APr}) is not a bargain, it is a change of character, and it shuts the
 * temperament off <b>completely</b>: no fleeing and no aggression toward
 * anything at all, player or mob or another horse, whoever struck the deal.
 * (Owner's call, 2026-09-25: <i>"passification (when the permanent allele)
 * should permanently shut off all fleeing or aggression genes (not remove them,
 * but disable them)"</i>.) That is the only way {@link #suppresses} is ever true
 * of a non-player.
 *
 * <p><b>Disabled, not removed</b>, and the distinction is load-bearing. A
 * permanently calmed Netherhorse still carries {@code Aaa/Aaa}, still shows it in
 * the browser's Alleles tab, still writes it onto a research paper, and still
 * passes it to every foal - which then has to be calmed itself. Nothing about the
 * genome changes; the veto is asked at the moment of acting and the answer is no.
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
     * <b>Is this horse forbidden from targeting that creature?</b>
     *
     * <p>True of a <b>player</b> who holds a calm with it, and true of
     * <b>anything at all</b> once the horse has been permanently calmed by
     * somebody - see the class note on why those two are different rules.
     */
    public static boolean suppresses(Horse horse, LivingEntity target) {
        PassificationAttachment state = horse.getData(ModAttachments.PASSIFICATION.get());
        // A PERMANENT calm is not a bargain with one person, it is a change of
        // character: the horse stops fleeing and stops picking fights with
        // ANYTHING, player or mob or another horse (owner's call). So it is
        // answered before the player test, and it is the only branch that can be
        // true of a non-player target.
        //
        // The genes are not removed, only vetoed. A permanently calmed Netherhorse
        // still carries Aaa/Aaa and still passes it to its foals; it simply never
        // acts on it. That matters for breeding, for the browser's Alleles tab,
        // and for what a research paper written off it says.
        if (state.permanentForAnyone()) {
            return true;
        }
        if (!(target instanceof Player player)) {
            return false;
        }
        return state.calm(player.getUUID(), horse.level().getGameTime());
    }

    /** Is this route currently worth approaching this player for? */
    static boolean offerAvailable(Horse horse, Player player, PassificationGene.Route route, long now) {
        if (!windowOpen(route, horse)) {
            return false;
        }
        PassificationAttachment state = horse.getData(ModAttachments.PASSIFICATION.get());
        UUID id = player.getUUID();
        if (state.permanent(id) || (route.kind() == PassificationGene.Kind.TEMPORARY
                && (state.calm(id, now) || !state.offAsCooldown(id, now, route.cooldownTicks())))) {
            return false;
        }
        return true;
    }

    /**
     * Drop a target the horse is no longer allowed to have. Called when a calm
     * is granted, so the animal stops mid-charge rather than at its next scan.
     *
     * <p>A flee leg counts as something to drop too, and it is the harder half:
     * a target is re-read every tick, but a flee is a path already issued and
     * runs itself out regardless. Both go here so "it stops the moment you pay"
     * means the same thing whichever way the horse was treating you.
     */
    private static void forget(Horse horse, Player player) {
        // A PERMANENT calm has just switched the temperament off entirely, so
        // there is nothing left it is allowed to be chasing or running from -
        // not only this player. Dropping just the payer would leave a
        // permanently calmed horse still mid-charge at the mob or the horse it
        // had picked a moment earlier, and nothing would ever re-select it, so
        // the stale leg would simply run to its end looking like the veto had
        // not worked.
        if (horse.getData(ModAttachments.PASSIFICATION.get()).permanentForAnyone()) {
            horse.setTarget(null);
            horse.setLastHurtByMob(null);
            GeneAbilityHandler.stopFleeing(horse);
            return;
        }
        if (horse.getTarget() == player) {
            horse.setTarget(null);
        }
        if (horse.getLastHurtByMob() == player) {
            horse.setLastHurtByMob(null);
        }
        GeneAbilityHandler.stopFleeingFrom(horse, player);
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
    static List<PassificationGene.Route> routesOf(Horse horse) {
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
