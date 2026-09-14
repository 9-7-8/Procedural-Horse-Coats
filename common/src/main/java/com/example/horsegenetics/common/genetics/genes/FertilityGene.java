package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneRarity;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;

import java.util.List;

/**
 * <b>Fertility</b> ({@code horsegenetics.fertility}) - how readily a mare
 * conceives, and how often she carries two.
 *
 * <p>Three alleles:
 * <ul>
 *   <li>{@code n} - ordinary fertility, the wild type.</li>
 *   <li>{@code sf} - <b>subfertile</b>, recessive. {@code sf/sf} multiplies the
 *       chance of conception by {@link #SUBFERTILE_FACTOR} on <i>every</i> path,
 *       plain golden carrots included. It is the only thing in the mod that can
 *       make vanilla breeding miss, and it takes two copies in <i>one</i>
 *       parent, so a horse nobody has bred for it breeds exactly as vanilla
 *       does. Settled with the owner, 2026-09-13.</li>
 *   <li>{@code tw} - <b>twinning</b>, additive. Raises the chance that a
 *       conception on this mod's own paths is two embryos rather than one
 *       ({@link #twinChance}). Plain golden-carrot breeding never gives twins.</li>
 * </ul>
 *
 * <h2>The number on each copy</h2>
 * Every allele copy carries a {@code fertility} value, Gaussian about 1.0. The
 * <b>expressed</b> copy's value (the ordinary epigenetic rule - see
 * {@code Epigenome.expressed}) scales a mare's conception odds on the mod's
 * own paths, and drifts a little each breeding like every other epigenetic
 * number. It never touches plain golden-carrot breeding: only the allele does.
 *
 * <h2>Declared natural, and honest about it</h2>
 * Real equine fertility and twinning are polygenic and heavily environmental;
 * there is no single horse locus like this. It is a game abstraction of a real
 * heritable trait, filed with the other naturals that paint nothing (sex,
 * diet) - see {@code wiki/gene-fertility.html}'s science tab.
 *
 * <p>The numbers that are not about the alleles - heat, gestation, the odds by
 * stage - live in {@code common.repro.ReproRules}.
 */
public final class FertilityGene implements Gene {

    public static final String KEY = "horsegenetics.fertility";

    /** Between sex (1) and diet (5); it paints nothing, so the slot only orders the code. */
    public static final int PRIORITY = 3;

    /** The one epigenetic value, on every copy. */
    public static final String FERTILITY = "fertility";

    /** What {@code sf/sf} multiplies the chance of conception by, on every path. */
    public static final double SUBFERTILE_FACTOR = 0.5;

    /** Chance a conception is twins, by how many {@code tw} copies the mare carries. */
    public static final double TWIN_CHANCE_NO_COPY = 0.02;
    public static final double TWIN_CHANCE_ONE_COPY = 0.10;
    public static final double TWIN_CHANCE_TWO_COPIES = 0.25;

    public final Allele tw;
    public final Allele n;
    public final Allele sf;

    private final List<Allele> alleles;

    private final Expression wild = Expression.wildType(
            "Ordinary fertility. A mare conceives at the usual odds and nearly always carries one foal.");
    private final Expression subfertile = Expression.wildType("subfertile", "Subfertile",
            "Two copies of sf. Every breeding is half as likely to take - even plain golden carrots "
                    + "sometimes come to nothing. One copy is silent.");
    private final Expression twinProne = Expression.wildType("twin-prone", "Twin-prone",
            "One copy of tw. A pregnancy is twins about one time in ten instead of one in fifty.");
    private final Expression twinning = Expression.wildType("twinning", "Strongly twin-prone",
            "Two copies of tw. A pregnancy is twins about one time in four.");

    private final List<Expression> expressions;
    private final FounderTable founders;

    public FertilityGene() {
        this.tw = new Allele(KEY, 0, "tw", "Twinning (tw)");
        this.n = new Allele(KEY, 1, "n", "Ordinary fertility (n)");
        this.sf = new Allele(KEY, 2, "sf", "Subfertile (sf)");
        this.alleles = List.of(tw, n, sf);
        this.expressions = List.of(wild, subfertile, twinProne, twinning);
        // Carriers only, and rarely. A subfertile wild horse would not have
        // left many foals to be caught, so sf/sf is something you breed.
        this.founders = FounderTable.builder()
                .weight(n, n, 88.0)
                .weight(sf, n, 6.0)
                .weight(tw, n, 5.0)
                .weight(tw, tw, 1.0)
                .build();
    }

    @Override public String key() { return KEY; }
    @Override public String name() { return "Fertility"; }
    @Override public int priority() { return PRIORITY; }
    @Override public GeneRarity rarity() { return GeneRarity.UNCOMMON; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        if (pair.homozygousFor(sf)) {
            return subfertile;
        }
        switch (pair.count(tw)) {
            case 2: return twinning;
            case 1: return twinProne;
            default: return wild;
        }
    }

    @Override
    public EpiSchema epiSchema() {
        // mean +/- 6 sigma is the design span, so a founder sits in 0.7..1.3
        return EpiSchema.of(EpiValue.gaussian(FERTILITY, 1.0, 0.05, 0.7));
    }

    // --- what the alleles do -------------------------------------------------

    public boolean subfertile(AllelePair pair) {
        return pair.homozygousFor(sf);
    }

    /** The allele half of the odds: {@link #SUBFERTILE_FACTOR} for {@code sf/sf}, else 1. */
    public double alleleFactor(AllelePair pair) {
        return subfertile(pair) ? SUBFERTILE_FACTOR : 1.0;
    }

    /** Chance a conception on the mod's own paths is two embryos. */
    public double twinChance(AllelePair pair) {
        switch (pair.count(tw)) {
            case 2: return TWIN_CHANCE_TWO_COPIES;
            case 1: return TWIN_CHANCE_ONE_COPY;
            default: return TWIN_CHANCE_NO_COPY;
        }
    }

    /** The expressed copy's {@code fertility} number. */
    public double fertility(Genome genome) {
        return genome.epigenome().expressedValues(this, genome.genotype()).get(FERTILITY);
    }

    /**
     * Everything this locus says about a <b>mare's</b> odds on the mod's own
     * paths: the allele factor times her expressed number.
     */
    public double mareFactor(Genome mare) {
        return alleleFactor(mare.genotype().pair(this)) * fertility(mare);
    }
}
