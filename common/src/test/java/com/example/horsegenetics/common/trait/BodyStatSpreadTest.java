package com.example.horsegenetics.common.trait;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>Do two horses actually have different bodies?</b>
 *
 * <p>Written off a report that every horse in a stable was showing the same
 * speed, health, jump and size. They were, and they were right to: the horses
 * had been made with the custom spawn egg, which starts every locus at
 * {@link com.example.horsegenetics.common.genetics.Gene#defaultAllele()}, and
 * an all-baseline genotype resolves to exactly {@link HorseTraits#baseline()}
 * every time. Not a defect - but nothing said so out loud, and the two cases
 * this pins are the two that would have answered the question in seconds.
 */
class BodyStatSpreadTest {

    private static String signature(Traits t) {
        return String.format("%.4f/%.4f/%.3f/%.4f",
                t.speed(), t.jump(), t.health(), t.scale());
    }

    /**
     * <b>The baseline is a single horse, not a range.</b> Every wild-type
     * genotype is the same body, whatever the epigenome says - epigenetic
     * numbers ride on the allele copies, and a locus sitting at its baseline
     * contributes nothing for them to modulate.
     */
    @Test
    void anAllWildTypeGenotypeIsAlwaysTheSameBody() {
        Set<String> seen = new LinkedHashSet<>();
        for (int i = 0; i < 12; i++) {
            Genome genome = new Genome(Genotype.wildType(), Epigenome.fromSeed(1000L + i));
            seen.add(signature(HorseTraits.resolve(genome)));
        }
        assertEquals(1, seen.size(),
                "a wild-type horse has one body; the epigenome cannot vary what no allele contributes");
        assertEquals(signature(HorseTraits.baseline()), seen.iterator().next());
    }

    /**
     * <b>Rolled horses do differ.</b> The body-stat loci are real genes -
     * HMGA2, LCORL, MSTN, PDK4, RYR2, CKM and the magical size/speed/health/jump
     * set - so a horse that drew any of them is off the baseline, and a
     * population of them spreads out.
     */
    @Test
    void randomlyRolledHorsesDoNotAllShareOneBody() {
        Set<String> seen = new LinkedHashSet<>();
        for (int i = 0; i < 40; i++) {
            SeededRng rng = new SeededRng(9000L + i);
            seen.add(signature(HorseTraits.resolve(Genome.random(rng))));
        }
        assertTrue(seen.size() > 5,
                "40 rolled horses collapsed to " + seen.size() + " distinct bodies - the body-stat "
                        + "loci are not reaching the trait resolver");
    }
}
