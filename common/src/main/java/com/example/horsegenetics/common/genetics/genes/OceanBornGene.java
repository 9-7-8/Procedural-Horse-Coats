package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.List;

/**
 * <b>Ocean-born</b> ({@code horsegenetics.ocean_born}) - neither the horse nor
 * its rider can drown. That is all it does, and it is deliberately all it does.
 *
 * <h2>Why this locus is so small</h2>
 * Everything else a water horse might want already ships:
 * {@link com.example.horsegenetics.common.genetics.Genes#MAGIC_SWIM_SPEED}
 * is how fast it crosses, {@link MagicWaterBreathingGene} is the graded dial on
 * how long it lasts under, and waterborn walks on the surface. Rider drowning
 * immunity was the one genuinely missing piece, so that is the whole gene.
 *
 * <h2>The four water loci stay split</h2>
 * The original proposal folded all of them into one "ocean-born" locus. It was
 * rejected on two counts: merging changes the genotype of every horse already
 * bred, and it destroys the ability to have a horse that swims fast and still
 * drowns. One combined gene would be far easier to breed for - which is the
 * objection, not the argument for it. Four loci mean four partial successes and
 * a horse that is good at water in a way that is specifically yours.
 */
public final class OceanBornGene extends AbstractAbilityGene {

    public static final String KEY = "horsegenetics.ocean_born";
    public static final int PRIORITY = 157;

    public OceanBornGene() {
        super(KEY, PRIORITY, "Ocean-born",
                "Ocn", "Ocean-born (Ocn)",
                Dominance.RECESSIVE, Founders.CARRIERS_ONLY, 6.0,
                "The horse drowns on the usual schedule, and so do you.",
                "Ocean-born",
                "Two copies. Neither the horse nor its rider ever drowns, however long you stay under. It says nothing about how fast the horse swims or how it behaves in water - those are other loci, on purpose.");
    }

    @Override
    protected List<GeneAbility> abilitiesWhenExpressed(EpiValues epi) {
        return List.of(new GeneAbility.Traversal("underwater_breathing", "both", GeneAbility.Condition.ALWAYS, 1));
    }
}
