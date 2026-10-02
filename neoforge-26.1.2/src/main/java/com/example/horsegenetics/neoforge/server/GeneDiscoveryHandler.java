package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.breed.BreedLineage;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.ResearchTopic;
import com.example.horsegenetics.neoforge.data.GeneDatabaseData;
import java.util.ArrayList;
import java.util.List;
import com.example.horsegenetics.common.progress.ProgressTask;
import com.example.horsegenetics.neoforge.server.HorseProgress;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Fills a player's {@link GeneDatabaseData} as they meet genes (roadmap wiki
 * &sect;16.1): <b>taming</b> a horse that carries a non-baseline allele at a
 * gene, and <b>breeding</b> a foal with one (called from
 * {@link HorseBreedingHandler#applyBredFoal}). Filing a paper on the research
 * shelf is the third route, {@link #discoverFromPaper}.
 *
 * <p>The database gates nothing but the gene-carrot recipe - every gene a
 * horse carries still shows on the info panel and the genotype code regardless.
 */
@EventBusSubscriber
public final class GeneDiscoveryHandler {

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GeneDatabaseData.get(((net.minecraft.server.level.ServerLevel) player.level()).getServer()).sync(player);
        }
    }

    @SubscribeEvent
    static void onTame(AnimalTameEvent event) {
        if (event.getAnimal() instanceof Horse horse && event.getTamer() instanceof ServerPlayer player) {
            HorseProgress.complete(player, HorseRecords.of(horse).sex() == com.example.horsegenetics.common.horse.Sex.FEMALE
                    ? ProgressTask.TAME_MARE : ProgressTask.TAME_STALLION);
            HorseRecords.of(horse).breed().ifPresent(id -> {
                GeneDatabaseData.get(((net.minecraft.server.level.ServerLevel) player.level())
                        .getServer()).discoverBreeds(player, java.util.List.of(id));
                HorseProgress.complete(player, ProgressTask.DISCOVER_BREED);
            });
            // The two magical ways in, both facts about the label this horse
            // already carries. "dhampir" is the id in
            // horsegenetics/breeds/dhampir.json, the one breed file whose kind
            // is magical; isMagical() is the Magical (Breed) variant label.
            BreedLineage line = HorseRecords.of(horse).lineage();
            if (line.components().contains("dhampir")) {
                HorseProgress.complete(player, ProgressTask.TAME_DHAMPIR);
            }
            if (line.isMagical()) {
                HorseProgress.complete(player, ProgressTask.TAME_MAGICAL_HORSE);
            }
            try {
                discoverFrom(player, Genotype.parse(HorseRecords.of(horse).geneticCode()));
            } catch (RuntimeException ignored) {
                // an unparseable code from another registry - nothing to learn
            }
        }
    }

    /** Discover every gene {@code genotype} carries a non-baseline allele at, for {@code player}. */
    public static void discoverFrom(Player player, Genotype genotype) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        GeneDatabaseData db = GeneDatabaseData.get(((net.minecraft.server.level.ServerLevel) serverPlayer.level()).getServer());
        // Every allele on the animal joins the collection, baseline included -
        // which is a different question from whether the gene is discovered.
        db.collect(serverPlayer, genotype);
        for (Gene gene : Genes.codeOrder()) {
            AllelePair pair = genotype.pair(gene);
            if (pair.homozygousFor(gene.defaultAllele())) {
                continue; // the ordinary horse tells you nothing new
            }
            List<String> tokens = new ArrayList<>(2);
            tokens.add(pair.first().token());
            tokens.add(pair.second().token());
            db.discover(serverPlayer, gene, tokens);
            HorseProgress.complete(serverPlayer, ProgressTask.DISCOVER_GENE);
            if (!gene.isNatural()) {
                HorseProgress.complete(serverPlayer, ProgressTask.MAGICAL_GENE_DISCOVERED);
            }
        }
    }

    /**
     * <b>Filing a paper on the research shelf discovers its gene</b> for the
     * player who filed it (owner, 2026-10-01). Called from the shelf menu's
     * paper slot, so only a player's hand reaches it: automation writing to the
     * container never passes here, and a {@link FakePlayer} - another mod's
     * automation, or the yard's own - is not a player who learnt anything.
     * Told the way taming and breeding tell: the gene database updates and the
     * progress tasks tick, with no chat line of its own.
     */
    public static void discoverFromPaper(Player player, ResearchTopic topic) {
        if (!(player instanceof ServerPlayer serverPlayer) || player instanceof FakePlayer || topic == null) {
            return;
        }
        List<String> tokens = topic.discoveryTokens();
        if (tokens.isEmpty()) {
            return;
        }
        Gene gene = topic.gene();
        GeneDatabaseData.get(((net.minecraft.server.level.ServerLevel) serverPlayer.level()).getServer())
                .discover(serverPlayer, gene, tokens);
        HorseProgress.complete(serverPlayer, ProgressTask.DISCOVER_GENE);
        if (!gene.isNatural()) {
            HorseProgress.complete(serverPlayer, ProgressTask.MAGICAL_GENE_DISCOVERED);
        }
    }

    private GeneDiscoveryHandler() {
    }
}
