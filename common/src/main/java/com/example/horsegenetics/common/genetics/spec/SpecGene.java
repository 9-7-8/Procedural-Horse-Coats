package com.example.horsegenetics.common.genetics.spec;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.ColorField;
import com.example.horsegenetics.common.coat.pattern.ColorView;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.pattern.PigmentView;
import com.example.horsegenetics.common.coat.pattern.SpecPainter;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.DominancePattern;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Gene} whose behaviour comes from a {@link GeneSpec} instead of from
 * Java. Everything the interface asks - alleles, precedence, inheritance,
 * visibility, determinism, the coat hooks - is answered from the spec, so a gene
 * that fits the format is a file rather than a class.
 *
 * <p>It is an ordinary gene in every other respect: it goes in the same
 * registry, takes the same place in the genotype code, breeds by the same
 * Mendelian draw, and its non-deterministic numbers come off the same per-allele
 * epigenetic seed. Nothing downstream knows or cares that it was loaded from a
 * file.
 *
 * <p>Randomness consumed by {@link #randomPair}: one {@code nextInt(wildOdds)}
 * per allele copy, plus one {@code nextInt} per copy to pick between variants on
 * a gene that declares more than two alleles.
 */
public final class SpecGene implements Gene {

    private final GeneSpec spec;
    private final List<Allele> alleles;
    private final Allele wildType;
    private final Allele variant;

    public SpecGene(GeneSpec spec) {
        this.spec = spec;
        List<Allele> built = new ArrayList<>();
        for (GeneSpec.AlleleSpec a : spec.alleles()) {
            built.add(new Allele(spec.key(), a.token(), a.label(), a.visible(), a.deterministic()));
        }
        this.alleles = List.copyOf(built);
        this.variant = alleles.get(0);
        this.wildType = alleles.get(alleles.size() - 1);
    }

    public GeneSpec spec() {
        return spec;
    }

    /**
     * The gene's processing priority - see {@link Gene#priority()}. A spec gene
     * sorts into the one unified {@code (priority, key)} order alongside the
     * built-ins, so a data-driven natural gene at priority 35 lands between the
     * built-in cream and champagne.
     */
    @Override
    public int priority() {
        return spec.priority();
    }

    @Override public String key() { return spec.key(); }

    @Override public List<Allele> alleles() { return alleles; }

    @Override public Allele wildType() { return wildType; }

    @Override public DominancePattern dominance() { return spec.dominance(); }

    @Override public boolean isNatural() { return spec.natural(); }

    @Override
    public AllelePair randomPair(Rng rng) {
        return new AllelePair(rollAllele(rng), rollAllele(rng));
    }

    private Allele rollAllele(Rng rng) {
        if (rng.nextInt(spec.wildOdds()) != 0) {
            return wildType;
        }
        int variants = alleles.size() - 1;
        return variants <= 1 ? variant : alleles.get(rng.nextInt(variants));
    }

    /** How many copies of the <b>variant</b> allele ({@code alleles[0]}) this horse carries. */
    public int dose(AllelePair pair) {
        int n = 0;
        if (pair.first().equals(variant)) {
            n++;
        }
        if (pair.second().equals(variant)) {
            n++;
        }
        return n;
    }

    @Override
    public boolean isVisible(AllelePair pair, Genotype genotype) {
        if (spec.dominance() == DominancePattern.RECESSIVE) {
            return dose(pair) >= 2;
        }
        return !pair.first().equals(wildType) || !pair.second().equals(wildType);
    }

    @Override
    public boolean isDeterministic(AllelePair pair, Genotype genotype) {
        return spec.isDeterministic() || !isVisible(pair, genotype);
    }

    @Override
    public PigmentField restrict(AllelePair pair, CoatBuildContext ctx, PigmentView coat) {
        if (!spec.natural() || !isVisible(pair, ctx.genotype())) {
            return null;
        }
        return SpecPainter.restrict(spec, values(pair, ctx), ctx, coat);
    }

    @Override
    public ColorField tint(AllelePair pair, CoatBuildContext ctx, PigmentView coat, ColorView colour) {
        if (spec.natural() || !isVisible(pair, ctx.genotype())) {
            return null;
        }
        return SpecPainter.tint(spec, values(pair, ctx), ctx, coat, colour);
    }

    private SpecValues values(AllelePair pair, CoatBuildContext ctx) {
        return SpecValues.draw(spec, ctx.epigeneticsFor(spec.key()), dose(pair));
    }

    @Override
    public String toString() {
        return "SpecGene[" + spec.key() + "]";
    }
}
