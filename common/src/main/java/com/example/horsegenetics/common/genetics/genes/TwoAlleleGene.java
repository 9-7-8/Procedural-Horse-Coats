package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneRarity;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>One variant allele, one wild type, one outcome.</b> The shape most
 * hand-written genes turn out to have, and everything about it that is not the
 * gene's own idea.
 *
 * <p>{@link Gene} is an eight-method interface, and a two-allele locus answers
 * seven of those the same way every time: build the two {@link Allele}s with
 * the right {@link Allele#order() slot order}, list them, name a default, list
 * the expressions, map a pair to one of them by counting copies, and turn a
 * declared frequency into a {@link FounderTable}. This base answers all seven
 * from a {@link Setup}, so a subclass writes the eighth - what the gene
 * actually <i>does</i>.
 *
 * <h2>Which subclass</h2>
 * <ul>
 *   <li>{@link AbstractNaturalGene} - the outcome restricts red / black pigment
 *       in phase 1. A real-life coat gene.</li>
 *   <li>{@link AbstractMagicalGene} - the outcome adds signed RGB in phase 3.</li>
 *   <li>{@link AbstractAbilityGene} - the outcome paints nothing and grants
 *       abilities instead.</li>
 *   <li>Raw {@link Gene} for anything else. <b>Nothing here is required</b>;
 *       these tiers only mean the common case is short.</li>
 * </ul>
 *
 * <h2>What it deliberately does not do</h2>
 * There is no dominance <i>property</i> on the gene - see {@link Gene} for why.
 * {@link Setup#dominant()} / {@link Setup#recessive()} is a statement about
 * this locus's two alleles only, and all it does is decide which of the three
 * combinations {@link #expressionOf} sends to the outcome. A gene with three
 * alleles, or one whose heterozygote is its own outcome, does not fit this base
 * and should not be bent into it.
 *
 * @see AbstractAbilityGene the first of these, and the evidence the shape is real
 */
public abstract class TwoAlleleGene implements Gene {

    /** Whether one copy is enough. */
    public enum Dominance { DOMINANT, RECESSIVE }

    /**
     * What the wild population carries - a design decision rather than a knob.
     *
     * <p>{@link #EXPRESSING} puts the expressing combination in the wild, so a
     * player can <i>catch</i> one. {@link #CARRIERS_ONLY} puts only the carrier
     * there, so the gene exists in the population and is never seen until
     * somebody breeds two together - which is what makes a pedigree, and the
     * gene database, worth anything.
     */
    public enum Founders {
        /** The expressing combination is born wild, so the gene can be caught. */
        EXPRESSING,
        /** Only carriers are born wild, so the gene is never seen until somebody breeds it. */
        CARRIERS_ONLY
    }

    // ------------------------------------------------------------------
    // Setup
    // ------------------------------------------------------------------

    /**
     * Everything a two-allele locus has to declare, as one fluent value.
     *
     * <p>A builder rather than a fifteen-argument constructor because that is
     * what the first of these ({@link AbstractAbilityGene}) grew into: ten
     * positional arguments, six of them strings, and no way to read a call site
     * without counting commas.
     */
    public static final class Setup {

        final String key;
        final int priority;
        final String displayName;

        String variantToken;
        String variantLabel;
        String wildToken = "n";
        String wildLabel = "Wild-type (n)";

        Dominance dominance;
        Founders foundersPolicy;
        double wildPercent = -1;
        double alleleFrequency = -1;
        FounderTable explicitFounders;

        String wildText = "";
        String carrierId;
        String carrierName;
        String carrierText;

        String outcomeId = "active";
        String outcomeName;
        String outcomeText = "";
        boolean varies;
        boolean masks;

        GeneRarity rarity = GeneRarity.DEFAULT;
        boolean feralOnly;
        boolean hasGeneCarrot = true;

        private Setup(String key, int priority, String displayName) {
            this.key = key;
            this.priority = priority;
            this.displayName = displayName;
        }

        /** The variant allele's code token and its display label. */
        public Setup variant(String token, String label) {
            this.variantToken = token;
            this.variantLabel = label;
            return this;
        }

        /** Rename the wild type, for a locus whose baseline has a real name of its own. */
        public Setup wildAllele(String token, String label) {
            this.wildToken = token;
            this.wildLabel = label;
            return this;
        }

        /** One copy expresses. */
        public Setup dominant() {
            this.dominance = Dominance.DOMINANT;
            return this;
        }

        /** Two copies express; one is a silent carrier. */
        public Setup recessive() {
            this.dominance = Dominance.RECESSIVE;
            return this;
        }

        /**
         * Founder frequency as <b>one allele's</b> frequency, spread over the
         * three combinations by Hardy-Weinberg - so carriers and homozygotes
         * both appear in the proportions a real population would have them.
         * The usual choice for a natural gene.
         */
        public Setup hardyWeinberg(double alleleFrequency) {
            this.alleleFrequency = alleleFrequency;
            return this;
        }

        /**
         * Founder frequency as a <b>percentage of founders</b> carrying the one
         * combination {@code policy} names, with every other founder wild type.
         * Blunter than {@link #hardyWeinberg}, and the right shape for a gene
         * whose point is that it is never seen in the wild.
         */
        public Setup founders(Founders policy, double percentOfHorses) {
            this.foundersPolicy = policy;
            this.wildPercent = percentOfHorses;
            return this;
        }

        /** A founder distribution this base could not have derived. */
        public Setup founderTable(FounderTable table) {
            this.explicitFounders = table;
            return this;
        }

        /** The sentence a player reads for a horse carrying nothing here. */
        public Setup wild(String description) {
            this.wildText = description;
            return this;
        }

        /**
         * Give the single-copy carrier of a <b>recessive</b> gene its own
         * wording. Optional: without it a carrier reads as the plain wild type,
         * which is honest but says less. Meaningless on a dominant, where one
         * copy is the outcome.
         */
        public Setup carrier(String id, String name, String description) {
            this.carrierId = id;
            this.carrierName = name;
            this.carrierText = description;
            return this;
        }

        /** The outcome: its stable id, its name, and the sentence describing it. */
        public Setup outcome(String id, String name, String description) {
            this.outcomeId = id;
            this.outcomeName = name;
            this.outcomeText = description;
            return this;
        }

        /** The outcome differs per horse - it reads the expressing copy's epigenetics. */
        public Setup varies() {
            this.varies = true;
            return this;
        }

        /** While the outcome shows, no other gene is visible. */
        public Setup masking() {
            this.masks = true;
            return this;
        }

        public Setup rarity(GeneRarity value) {
            this.rarity = value;
            return this;
        }

        /** No named breed carries this - see {@link Gene#feralOnly()}. */
        public Setup feralOnly() {
            this.feralOnly = true;
            return this;
        }

        /** No Known Gene Splice carrot for this locus - see {@link Gene#hasGeneCarrot()}. */
        public Setup noGeneCarrot() {
            this.hasGeneCarrot = false;
            return this;
        }

        private void validate() {
            require(key != null && !key.isEmpty(), "a key");
            require(displayName != null && !displayName.isEmpty(), "a name");
            require(variantToken != null, "variant(token, label)");
            require(dominance != null, "dominant() or recessive()");
            require(outcomeName != null, "outcome(id, name, description)");
            require(explicitFounders != null || alleleFrequency >= 0 || wildPercent >= 0,
                    "hardyWeinberg(f), founders(policy, percent) or founderTable(t)");
        }

        private void require(boolean ok, String what) {
            if (!ok) {
                throw new IllegalStateException("gene " + key + " declares no " + what);
            }
        }
    }

    /** Start describing a two-allele gene. */
    public static Setup gene(String key, int priority, String displayName) {
        return new Setup(key, priority, displayName);
    }

    // ------------------------------------------------------------------

    private final String key;
    private final int priority;
    private final String displayName;
    private final Dominance dominance;
    private final GeneRarity rarity;
    private final boolean feralOnly;
    private final boolean hasGeneCarrot;

    /** The variant allele. {@code order() == 0}, so it lands in slot 0 of a pair. */
    public final Allele variant;
    /** The wild type. */
    public final Allele wild;

    private final List<Allele> alleles;
    private final Expression wildExpression;
    private final Expression carrierExpression;
    private final FounderTable founders;

    /** Built on first read: the outcome is the subclass's, so it does not exist yet here. */
    private volatile List<Expression> expressions;

    protected TwoAlleleGene(Setup setup) {
        setup.validate();
        this.key = setup.key;
        this.priority = setup.priority;
        this.displayName = setup.displayName;
        this.dominance = setup.dominance;
        this.rarity = setup.rarity;
        this.feralOnly = setup.feralOnly;
        this.hasGeneCarrot = setup.hasGeneCarrot;

        this.variant = new Allele(setup.key, 0, setup.variantToken, setup.variantLabel);
        this.wild = new Allele(setup.key, 1, setup.wildToken, setup.wildLabel);
        this.alleles = List.of(variant, wild);

        this.wildExpression = Expression.wildType(setup.wildText);
        this.carrierExpression = setup.carrierId == null ? null
                : Expression.wildType(setup.carrierId, setup.carrierName, setup.carrierText);
        this.founders = buildFounders(setup);
    }

    private FounderTable buildFounders(Setup setup) {
        if (setup.explicitFounders != null) {
            return setup.explicitFounders;
        }
        if (setup.alleleFrequency >= 0) {
            return FounderTable.hardyWeinberg(variant, wild, setup.alleleFrequency);
        }
        FounderTable.Builder b = FounderTable.builder();
        if (setup.foundersPolicy == Founders.CARRIERS_ONLY || dominance == Dominance.DOMINANT) {
            // A dominant gene's carrier IS its expressing form, so the two
            // policies coincide there and the heterozygote is what goes in.
            b.weight(variant, wild, setup.wildPercent);
        } else {
            b.weight(variant, variant, setup.wildPercent);
        }
        return b.weight(wild, wild, 100.0 - setup.wildPercent).build();
    }

    /**
     * The outcome. A subclass builds it in its own constructor, because only it
     * knows what the gene paints - which is why {@link #expressions()} is lazy
     * rather than a field assigned here.
     */
    protected abstract Expression outcome();

    /** Does this combination express? */
    public final boolean expresses(AllelePair pair) {
        int copies = pair.count(variant);
        return dominance == Dominance.DOMINANT ? copies >= 1 : copies == 2;
    }

    /** Is this a single copy of a recessive - carried, and showing nothing? */
    public final boolean isCarrier(AllelePair pair) {
        return dominance == Dominance.RECESSIVE && pair.count(variant) == 1;
    }

    @Override public final String key() { return key; }
    @Override public String name() { return displayName; }
    @Override public final int priority() { return priority; }
    @Override public final List<Allele> alleles() { return alleles; }
    @Override public final Allele defaultAllele() { return wild; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }
    @Override public GeneRarity rarity() { return rarity; }
    @Override public boolean feralOnly() { return feralOnly; }
    @Override public boolean hasGeneCarrot() { return hasGeneCarrot; }

    @Override
    public final List<Expression> expressions() {
        List<Expression> cached = expressions;
        if (cached == null) {
            List<Expression> built = new ArrayList<>(3);
            built.add(wildExpression);
            if (carrierExpression != null) {
                built.add(carrierExpression);
            }
            built.add(outcome());
            cached = List.copyOf(built);
            expressions = cached;
        }
        return cached;
    }

    @Override
    public Expression expressionOf(AllelePair pair) {
        if (expresses(pair)) {
            return outcome();
        }
        return carrierExpression != null && isCarrier(pair) ? carrierExpression : wildExpression;
    }
}
