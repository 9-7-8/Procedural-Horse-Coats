package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.Breeds;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.trait.HealthContribution;
import com.example.horsegenetics.neoforge.HorseGenetics;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * One log line per <b>breed founder</b> the server rolls: which disorders it
 * carries, what its sheet lists, and a {@code WARN} for any disorder or magical
 * gene it carries that the sheet does <i>not</i> list.
 *
 * <p>It exists because "a breed is exactly its sheet" ({@code BreedFounder})
 * cannot be checked by eye at any useful rate: a Quarter Horse carrying HYPP
 * looks like one that does not, and a Thoroughbred's clean record is an absence.
 * Owner's request, 2026-09-11 - spawn a stack of eggs, then read the log
 * ({@code grep "[breed-health]"}). A {@code NOT ON ITS SHEET} line is a
 * {@code BreedFounder} bug.
 *
 * <p>Every server-side founder path calls it: breed eggs
 * ({@link HorseRecords#newFounder(net.minecraft.world.entity.animal.equine.Horse,
 * com.example.horsegenetics.common.Rng, Breed)}), wild herds, the cowboy's
 * string and generated stables. The custom spawn egg does not - its genome is
 * whatever the player built, so an unlisted gene there is not a bug.
 * Feral Mixed is skipped: it is the one population allowed anything.
 */
public final class BreedFounderLog {

    /** Set from the breed's stat scores, not its gene list - see {@code BreedFounder}. */
    private static final Set<String> BODY_STAT_KEYS = Set.of(
            "horsegenetics.body_size",
            "horsegenetics.magic_speed",
            "horsegenetics.magic_health",
            "horsegenetics.magic_jump");

    private BreedFounderLog() {
    }

    public static void founder(Breed breed, Genotype genotype, String source) {
        if (breed == null || breed == Breeds.FERAL_MIXED) {
            return;
        }
        List<String> carried = new ArrayList<>();
        List<String> listed = new ArrayList<>();
        List<String> stray = new ArrayList<>();
        for (Gene gene : Genes.codeOrder()) {
            boolean health = gene instanceof HealthContribution;
            boolean magic = !BODY_STAT_KEYS.contains(gene.key()) && Genes.magicalOrder().contains(gene);
            if (!health && !magic) {
                continue;
            }
            boolean onSheet = breed.constrains(gene.key());
            if (health && onSheet) {
                listed.add(gene.name());
            }
            AllelePair pair = genotype.pair(gene);
            if (pair.homozygousFor(gene.defaultAllele())) {
                continue;
            }
            String shown = gene.name() + " " + pair.first().token() + "/" + pair.second().token();
            if (!onSheet) {
                stray.add(shown);
            } else if (health) {
                carried.add(shown);
            }
        }
        HorseGenetics.LOGGER.info("[breed-health] {} founder ({}): {} | sheet lists: {}",
                breed.name(), source,
                carried.isEmpty() ? "no disorder" : String.join(", ", carried),
                listed.isEmpty() ? "no disorders" : String.join(", ", listed));
        if (!stray.isEmpty()) {
            HorseGenetics.LOGGER.warn("[breed-health] {} founder ({}) carries {} - NOT ON ITS SHEET",
                    breed.name(), source, String.join(", ", stray));
        }
    }
}
