package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.ResearchTopic;
import com.example.horsegenetics.neoforge.item.ResearchPaperItem;
import com.example.horsegenetics.common.progress.ProgressTask;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>A book on a horse writes down one of its allele pairs, at random.</b>
 *
 * <h2>Why random, and why this exists</h2>
 * Research papers used to be written from the Horse Browser: pick the gene you
 * wanted, spend a book, get exactly that paper. That went with the browser's
 * crafting tab, and it was not a loss worth mourning - <i>choosing</i> the gene
 * meant the paper was never a discovery, only a formality once you already knew.
 *
 * <p>This is deliberately the opposite. You get <b>a</b> pair the horse in front
 * of you carries, not the one you wanted, so a paper is something the horse told
 * you rather than something you filled in. It is the collecting half of the
 * loop; the <b>Equine Research Shelf</b> is the other half, where a paper you
 * own becomes a paper you can copy.
 *
 * <h2>What counts as "a gene the horse has"</h2>
 * The same test discovery uses: a locus where the horse carries something other
 * than two copies of the plain baseline. An ordinary horse tells you nothing, so
 * an ordinary horse gives no paper and says so - and the book is not spent.
 *
 * <p><b>Discovery is not required</b>, and a paper does not grant it either: the
 * gene database is earned by <i>owning</i> a living example
 * ({@code server/GeneDiscoveryHandler}), never by reading. Gating this on the
 * database would only stop a horse teaching you what it plainly carries.
 *
 * <h2>Whose horse</h2>
 * <b>Anybody's.</b> Your own, a stranger's, one of the cowboy's branded string,
 * or one released into the horse realm for anyone to find - all four read the
 * same, and none of them needs taming first. Ownership is what gates
 * <i>keeping</i> a horse, not looking at one. See {@link #claimsBook} for the
 * test and {@link #onInteract} for why the priority that makes the cowboy's case
 * work is not optional.
 */
@EventBusSubscriber
public final class GeneBookFromHorse {

    private GeneBookFromHorse() {
    }

    /**
     * <b>A click aimed at the horse.</b> Vanilla fires
     * {@code EntityInteractSpecific} first and {@code EntityInteract} only if
     * nothing consumed it, so a listener on one alone can be cut off upstream by
     * any other mod that claims the specific event. Both, for the same reason
     * {@code HorseInteractionHandler} pairs its two.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!claimsBook(event.getTarget(), event.getItemStack())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        write(event.getTarget(), event.getItemStack(), event.getEntity(),
                event.getLevel().isClientSide());
    }

    /**
     * <b>HIGHEST, and that is load-bearing rather than tidy.</b> This used to be
     * plain {@code @SubscribeEvent}, which put it in the NORMAL tier alongside
     * {@code TransferPaperHandler.onEntityInteract} - and that one cancels
     * <i>any</i> held item on a cowboy-branded horse. A book worked on the
     * cowboy's string only because {@code GeneBookFromHorse} happens to sort
     * earlier in the jar than {@code TransferPaperHandler}, and within one
     * priority NeoForge's bus dispatches in plain registration order, which is
     * jar entry order. Nothing in the API promises that. So "you may read any
     * horse, the cowboy's included" was true by accident and one class rename
     * away from silently ending - and a listener with the default
     * {@code receiveCanceled} is skipped outright once the event is cancelled,
     * so the failure would be the book doing nothing at all.
     *
     * <p>HIGHEST is safe here because this claims strictly less than the two
     * listeners already at that priority: both of those need an <i>empty</i>
     * hand, and this needs a {@code Horse} and a {@code minecraft:book}.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!claimsBook(event.getTarget(), event.getItemStack())) {
            return;
        }
        // Cancel on both sides: right-clicking a tamed horse with an item is
        // "ride" to vanilla, and an unsuppressed client would predict a mount.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        write(event.getTarget(), event.getItemStack(), event.getEntity(),
                event.getLevel().isClientSide());
    }

    /**
     * <b>A book, on a horse - and that is the whole test.</b>
     *
     * <p><b>Any horse, deliberately.</b> No ownership test and no brand test: a
     * horse you own, a horse a stranger owns, one of the cowboy's branded string,
     * and one released into the horse realm all read the same. What a book copies
     * down is what the animal plainly carries, so refusing on ownership would only
     * stop a horse teaching you something you could learn by looking at it.
     * (Owner's call, and the reason for the priority above.)
     */
    private static boolean claimsBook(net.minecraft.world.entity.Entity target, ItemStack held) {
        return target instanceof Horse && held.is(Items.BOOK);
    }

    /** The work, once a listener has claimed the click. */
    private static void write(net.minecraft.world.entity.Entity target, ItemStack held,
                              net.minecraft.world.entity.player.Player clicker, boolean clientSide) {
        if (clientSide || !(target instanceof Horse horse)) {
            return;
        }
        if (!(clicker instanceof ServerPlayer player)) {
            return;
        }

        // A horse whose record has not been founded yet parses to a baseline
        // genotype, which is indistinguishable from an ordinary horse - so it
        // would have said "this horse has nothing to teach you" about an animal
        // that will have plenty to teach a moment later. Told apart on purpose:
        // the two look identical to a player and only one is worth retrying.
        if (!HorseRecords.hasRealRecord(horse)) {
            player.sendSystemMessage(Component.translatable("message.horsegenetics.gene_book.unknown"));
            return;
        }
        List<ResearchTopic> carried = carriedPairs(horse);
        if (carried.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.horsegenetics.gene_book.nothing"));
            return;
        }
        ResearchTopic topic = carried.get(player.getRandom().nextInt(carried.size()));

        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        ItemStack paper = ResearchPaperItem.of(topic);
        if (!player.getInventory().add(paper)) {
            player.drop(paper, false);
        }
        player.sendSystemMessage(Component.translatable("message.horsegenetics.gene_book.written",
                Component.literal(topic.label())));
        HorseProgress.complete(player, ProgressTask.GENE_BOOK);
    }

    /**
     * Every locus this horse carries off-baseline, <b>as the pair it actually
     * carries</b> - what it has to teach, down to which two alleles.
     *
     * <p>This is the only route in the mod that can produce a compound pair such
     * as {@code Sflm/Gflm}: chest loot and the supplier deal in carrier and
     * true-breeding pairs only, so a horse in front of you is the one place an
     * unusual combination comes from.
     */
    private static List<ResearchTopic> carriedPairs(Horse horse) {
        Genotype genotype;
        try {
            genotype = Genotype.parse(HorseRecords.of(horse).geneticCode());
        } catch (RuntimeException unparseable) {
            return List.of(); // a code from another registry - nothing to read
        }
        List<ResearchTopic> out = new ArrayList<>();
        for (Gene gene : Genes.codeOrder()) {
            AllelePair pair = genotype.pair(gene);
            if (!pair.homozygousFor(gene.defaultAllele())) {
                out.add(ResearchTopic.of(gene, pair));
            }
        }
        return out;
    }
}
