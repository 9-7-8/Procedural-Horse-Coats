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
 */
@EventBusSubscriber
public final class GeneBookFromHorse {

    private GeneBookFromHorse() {
    }

    @SubscribeEvent
    static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Horse horse)) {
            return;
        }
        if (!event.getItemStack().is(Items.BOOK)) {
            return;
        }
        // Cancel on both sides: right-clicking a tamed horse with an item is
        // "ride" to vanilla, and an unsuppressed client would predict a mount.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        List<ResearchTopic> carried = carriedPairs(horse);
        if (carried.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.horsegenetics.gene_book.nothing"));
            return;
        }
        ResearchTopic topic = carried.get(player.getRandom().nextInt(carried.size()));

        if (!player.getAbilities().instabuild) {
            event.getItemStack().shrink(1);
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
