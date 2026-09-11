package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.eye.EyeLocus;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>The shape all thirteen eye loci share</b>: one dominant wild type, a
 * handful of variants that are recessive to it <i>and to each other</i>, and -
 * for the twelve that come in pairs - a founder roll on the second eye that
 * usually matches the first.
 *
 * <h2>Recessive to each other, not just to the wild type</h2>
 * The rule {@link LycanGene} established and {@link AbstractMatchedPairGene}
 * generalised: a horse showing anything but the wild type carries the
 * <b>same</b> variant twice. Green beside gold is a brown-eyed horse carrying
 * two things, not a compromise between them, and it is why the eye loci are a
 * breeding programme rather than a slot machine - two odd-eyed horses caught in
 * different biomes are probably carrying different variants and their foals are
 * ordinary.
 *
 * <h2>Why the second eye usually matches the first</h2>
 * Left and right are separate loci, which is the point - heterochromia has to be
 * <i>reachable</i>. But they are separate loci inheriting independently, and a
 * naive founder roll on each would make a mismatched pair as common as the
 * square of the variant rate on each side, which for any rate worth having
 * means most odd-eyed horses in the world are wild-caught accidents rather than
 * anything a breeder did.
 *
 * <p>So the second eye's founder table is <b>conditioned on the first</b>
 * through {@link FounderContext}: {@value #TWIN_MATCH_PERCENT}% of founders draw
 * the same combination they drew on the other side, and the remainder rolls
 * freely. Heterochromia is then rare in the wild and completely ordinary to
 * breed for, which is the right way round. Nothing else in the model changes -
 * the two loci still inherit independently, so a matched founder's foals can
 * still come out odd-eyed.
 *
 * <p>That is only available to the locus that rolls <b>second</b>, which is why
 * every right-eye locus sorts one priority ahead of its left-eye twin.
 */
public abstract class AbstractEyeGene implements Gene {

    /** How often a founder's second eye simply copies the combination the first drew. */
    public static final double TWIN_MATCH_PERCENT = 92.0;

    /**
     * How many carriers there are for each horse that expresses. Three, which is
     * roughly what a recessive at these frequencies produces naturally and is
     * generous enough that a player testing a herd finds something.
     */
    public static final double CARRIERS_PER_EXPRESSER = 3.0;

    /** One variant: its allele, the outcome it produces, and how common that outcome is. */
    protected record Variant(Allele allele, Expression expression, double expressPercent) {}

    private final EyeLocus locus;
    private final int priority;
    private final String displayName;
    private final boolean natural;

    private final Allele wild;
    private final List<Variant> variants;
    private final List<Allele> alleles;
    private final List<Expression> expressions;

    private final Expression wildExpression;
    private final Expression carrier;
    /** Two <i>different</i> variants - a wild type, and only present when there are two to differ. */
    private final Expression mixed;

    private final FounderTable base;

    protected AbstractEyeGene(EyeLocus locus, int priority, String displayName, boolean natural,
                              Allele wild, Expression wildExpression,
                              Expression carrier, Expression mixed,
                              List<Variant> variants) {
        this.locus = locus;
        this.priority = priority;
        this.displayName = displayName;
        this.natural = natural;
        this.wild = wild;
        this.wildExpression = wildExpression;
        this.carrier = carrier;
        this.mixed = variants.size() > 1 ? mixed : null;
        this.variants = List.copyOf(variants);

        List<Allele> a = new ArrayList<>();
        a.add(wild);
        for (Variant v : variants) {
            a.add(v.allele());
        }
        this.alleles = List.copyOf(a);

        List<Expression> e = new ArrayList<>();
        e.add(wildExpression);
        e.add(carrier);
        if (this.mixed != null) {
            e.add(this.mixed);
        }
        for (Variant v : variants) {
            e.add(v.expression());
        }
        this.expressions = List.copyOf(e);

        this.base = buildBase();
    }

    private FounderTable buildBase() {
        FounderTable.Builder b = FounderTable.builder();
        double spent = 0.0;
        for (Variant v : variants) {
            b.weight(v.allele(), v.expressPercent());
            b.weight(wild, v.allele(), v.expressPercent() * CARRIERS_PER_EXPRESSER);
            spent += v.expressPercent() * (1.0 + CARRIERS_PER_EXPRESSER);
        }
        b.weight(wild, Math.max(0.0, 100.0 - spent));
        return b.build();
    }

    // ------------------------------------------------------------------
    // Gene
    // ------------------------------------------------------------------

    @Override public String key() { return locus.key(); }
    @Override public String name() { return displayName; }
    @Override public int priority() { return priority; }
    @Override public boolean isNatural() { return natural; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return wild; }
    @Override public List<Expression> expressions() { return expressions; }

    /** Which of the thirteen this is. */
    public final EyeLocus locus() {
        return locus;
    }

    /** The allele every ordinary horse carries two of. */
    public final Allele wild() {
        return wild;
    }

    /**
     * Recessive to the wild type and to each other: a horse shows a variant only
     * by carrying the same one twice.
     */
    @Override
    public Expression expressionOf(AllelePair pair) {
        if (pair.has(wild)) {
            return pair.homozygousFor(wild) ? wildExpression : carrier;
        }
        for (Variant v : variants) {
            if (pair.homozygousFor(v.allele())) {
                return v.expression();
            }
        }
        return mixed != null ? mixed : wildExpression;
    }

    /**
     * Which variant this horse <b>shows</b>, or {@code -1} for the wild type -
     * the index into the list the subclass passed in, which is how each locus
     * turns a pair into its own vocabulary.
     */
    protected final int shownIndex(AllelePair pair) {
        if (pair.has(wild)) {
            return -1;
        }
        for (int i = 0; i < variants.size(); i++) {
            if (pair.homozygousFor(variants.get(i).allele())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * The free roll, or - on a left-eye locus whose right-eye twin has already
     * been drawn - mostly a copy of it. See the class note.
     */
    @Override
    public FounderTable founderTable(FounderContext context) {
        EyeLocus twinLocus = locus.twin();
        if (twinLocus == locus) {
            return base;
        }
        Gene twin = Genes.byKeyOrNull(twinLocus.key());
        if (twin == null || !context.isRolled(twin)) {
            return base;   // this locus is the one that rolls first
        }
        AllelePair theirs = context.pair(twin);
        Allele a = translate(theirs.first());
        Allele b = translate(theirs.second());
        if (a == null || b == null) {
            return base;
        }
        // Built by walking the BASE table in its own order and folding the twin
        // bonus into whichever row matches, rather than declaring the twin row
        // first. FounderTable.Builder merges a repeated combination into the
        // entry that is already there, so declaring it first would move the
        // wild-type row off the end of the table - and "the last bucket is the
        // baseline" is an invariant GenotypeTest leans on to prove a founder is
        // reproducible from one float per gene.
        AllelePair twinPair = new AllelePair(a, b);
        FounderTable.Builder builder = FounderTable.builder();
        if (!base.pairs().contains(twinPair)) {
            builder.weight(a, b, TWIN_MATCH_PERCENT);   // defensive; a twin always drew from base
        }
        for (AllelePair p : base.pairs()) {
            double weight = base.share(p) * (100.0 - TWIN_MATCH_PERCENT);
            if (p.equals(twinPair)) {
                weight += TWIN_MATCH_PERCENT;
            }
            builder.weight(p.first(), p.second(), weight);
        }
        return builder.build();
    }

    /** The twin locus's allele, as <i>this</i> locus's allele of the same token. */
    private Allele translate(Allele theirs) {
        for (Allele mine : alleles) {
            if (mine.token().equals(theirs.token())) {
                return mine;
            }
        }
        return null;
    }
}
