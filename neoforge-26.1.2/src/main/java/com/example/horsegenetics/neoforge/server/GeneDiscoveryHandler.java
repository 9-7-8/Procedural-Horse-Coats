package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.neoforge.data.GeneDatabaseData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Fills a player's {@link GeneDatabaseData} as they meet genes (roadmap wiki
 * &sect;16.1): <b>taming</b> a horse that carries a non-baseline allele at a
 * gene, and <b>breeding</b> a foal with one (called from
 * {@link HorseBreedingHandler#applyBredFoal}). Reading a paper is the third
 * route and lives on {@link com.example.horsegenetics.neoforge.item.ResearchPaperItem}.
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
        for (Gene gene : Genes.codeOrder()) {
            AllelePair pair = genotype.pair(gene);
            if (pair.homozygousFor(gene.defaultAllele())) {
                continue; // the ordinary horse tells you nothing new
            }
            List<String> tokens = new ArrayList<>(2);
            tokens.add(pair.first().token());
            tokens.add(pair.second().token());
            db.discover(serverPlayer, gene, tokens);
        }
    }

    private GeneDiscoveryHandler() {
    }
}
