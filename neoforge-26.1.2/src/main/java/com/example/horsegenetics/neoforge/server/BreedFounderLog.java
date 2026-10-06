package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.BreedFounder;
import com.example.horsegenetics.common.breed.Breeds;
import com.example.horsegenetics.common.breed.MagicalVariant;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.trait.HealthContribution;
import com.example.horsegenetics.neoforge.HorseGenetics;

import java.util.ArrayList;
import java.util.List;

/**
 * One {@code DEBUG} line per <b>breed founder</b> the server rolls: which disorders it
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
 * string and generated stables - a magical herd's member is checked without its herd's one pair, and a stable logs its founder as rolled, before
 * the magic the stable adds on purpose ({@code StableSpawn.Rolled#founder}).
 * The custom spawn egg does not - its genome is
 * whatever the player built, so an unlisted gene there is not a bug.
 * Feral Mixed is skipped: it is the one population allowed anything.
 *
 * <p>The per-founder line is {@code DEBUG}, not {@code INFO} (#203): every wild
 * herd, egg and stable horse wrote one to a production server's log. The
 * {@code NOT ON ITS SHEET} warning is the line that matters and stays a WARN; set
 * the mod's logger to DEBUG to get the full census back.
 */
public final class BreedFounderLog {

    private BreedFounderLog() {
    }

    public static void founder(Breed breed, Genotype genotype, String source) {
        founder(breed, genotype, source, null);
    }

    /**
     * A member of a magical herd: the herd's one pair is the herd's spec, not a founder bug, so it is not
     * reported as off the sheet (issue #33). {@code herd} may be null.
     */
    public static void founder(Breed breed, Genotype genotype, String source, MagicalVariant herd) {
        if (breed == null || breed == Breeds.FERAL_MIXED) {
            return;
        }
        if (HorseGenetics.LOGGER.isDebugEnabled()) {
            debugLine(breed, genotype, source);
        }
        // The stray rule is common/'s, so a test can hold it (issue #12).
        List<String> stray = new ArrayList<>();
        for (Gene gene : BreedFounder.offSheet(breed, genotype, herd)) {
            stray.add(shown(gene, genotype));
        }
        if (!stray.isEmpty()) {
            HorseGenetics.LOGGER.warn("[breed-health] {} founder ({}) carries {} - NOT ON ITS SHEET",
                    breed.name(), source, String.join(", ", stray));
        }
    }

    /** The census line - built only when it will be written, since it walks every gene. */
    private static void debugLine(Breed breed, Genotype genotype, String source) {
        List<String> carried = new ArrayList<>();
        List<String> listed = new ArrayList<>();
        for (Gene gene : Genes.codeOrder()) {
            if (!(gene instanceof HealthContribution) || !breed.constrains(gene.key())) {
                continue;
            }
            listed.add(gene.name());
            AllelePair pair = genotype.pair(gene);
            if (!pair.homozygousFor(gene.defaultAllele())) {
                carried.add(shown(gene, genotype));
            }
        }
        HorseGenetics.LOGGER.debug("[breed-health] {} founder ({}): {} | sheet lists: {}",
                breed.name(), source,
                carried.isEmpty() ? "no disorder" : String.join(", ", carried),
                listed.isEmpty() ? "no disorders" : String.join(", ", listed));
    }

    private static String shown(Gene gene, Genotype genotype) {
        AllelePair pair = genotype.pair(gene);
        return gene.name() + " " + pair.first().token() + "/" + pair.second().token();
    }
}
