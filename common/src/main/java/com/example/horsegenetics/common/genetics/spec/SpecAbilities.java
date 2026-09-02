package com.example.horsegenetics.common.genetics.spec;

import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects the {@link GeneAbility}s a horse's {@link Genotype} actually
 * expresses - the list the NeoForge translator walks every tick.
 *
 * <p>Pure {@code common/}: it decides <i>which</i> abilities are in play
 * (the gene is a loaded {@link SpecGene}, its pair is visible, and the horse
 * carries enough copies for the ability's {@code minDose}). It does <b>not</b>
 * evaluate {@link GeneAbility.Condition}s or {@link GeneAbility.Trigger}s -
 * those need a live horse and belong to the translator.
 *
 * <p>Built-in Java genes never contribute here; only data-driven genes carry an
 * {@code effects} block. If the built-ins ever want game-side behaviour they can
 * implement a parallel hook on {@code Gene} - this class is deliberately about
 * the spec path.
 */
public final class SpecAbilities {

    private SpecAbilities() {}

    /** One expressed ability, tagged with the gene it came from (for logging / attribute-modifier ids). */
    public record Active(String geneKey, GeneAbility ability) {}

    /** Every ability the {@code genotype} expresses, across all loaded spec genes. */
    public static List<Active> activeFor(Genotype genotype) {
        List<Active> out = new ArrayList<>();
        for (SpecGene gene : Genes.loaded()) {
            if (!gene.spec().hasAbilities()) {
                continue;
            }
            AllelePair pair = genotype.pair(gene.key());
            if (!gene.isVisible(pair, genotype)) {
                continue;
            }
            int dose = gene.dose(pair);
            for (GeneAbility ability : gene.spec().abilities()) {
                if (dose >= ability.minDose()) {
                    out.add(new Active(gene.key(), ability));
                }
            }
        }
        return List.copyOf(out);
    }

    /** {@code true} if any loaded spec gene defines at least one ability - lets the translator skip work cheaply. */
    public static boolean anyLoaded() {
        for (SpecGene gene : Genes.loaded()) {
            if (gene.spec().hasAbilities()) {
                return true;
            }
        }
        return false;
    }
}
