package com.example.horsegenetics.common.genetics;

/**
 * What the <b>Randomize</b> button on a gene editor does - the split button's
 * menu, as an enum, so the custom spawn egg and the browser designer offer the
 * same things in the same order under the same names.
 *
 * <p>Every mode except {@link #EPIGENETICS} rolls alleles. What separates them
 * is <b>scope</b> - which loci the roll is allowed to touch - and whether the
 * horse's sex is re-rolled first.
 *
 * <h2>Sex is rolled first, or not at all</h2>
 * A sex-linked locus cannot be filled in without knowing the sex: a stallion
 * carries one copy of an X-linked gene and a mare two, so rolling the genes and
 * <i>then</i> the sex means immediately snapping half of what was rolled back
 * to something else. So {@link #rollsSex} modes set the sex before the first
 * allele is drawn.
 *
 * <p>Only the whole-genome modes do it. Re-rolling one family - the dilutions,
 * say - is an edit to part of the horse in front of you, and quietly changing
 * its sex underneath that is not what was asked for.
 *
 * <h2>A scoped roll leaves everything else exactly as it was</h2>
 * <i>Random natural white</i> re-rolls the white loci and does not touch the
 * dilutions, the magic or the epigenetics of anything outside the family. That
 * is what makes the menu worth having: it turns Randomize from "throw this
 * horse away" into "keep this horse and try another set of legs on it".
 */
public enum RandomizeMode {

    /**
     * The original button: a founder roll. With a breed selected it stays
     * inside that breed's pools and stat targets, otherwise it is
     * {@link Genotype#random} - every locus drawn from its own frequency table.
     */
    RANDOM("Randomize", "Randomize", Scope.ALL, null, true, 0),

    /**
     * <b>Every locus, uniformly, with no regard for what the wild produces.</b>
     * Each gene picks two of its alleles with equal weight - which is how you
     * see a homozygous dominant white, a locus whose founder table gives it a
     * rate in the thousandths, without rolling until it turns up. A pair the
     * gene refuses ({@link Gene#canOccur}, so KIT's nonviable homozygotes and
     * MET's {@code met/met}) snaps back to that gene's wild type rather than to
     * another draw: an impossible horse is not a rare horse.
     */
    TRUE_RANDOM("True random", "True random", Scope.ALL, null, true, 0),

    /** A founder roll, applied only to the {@link Gene#isNatural() natural} loci. */
    NATURAL("Random natural", "Rnd natural", Scope.NATURAL, null, false, 0),

    /** A founder roll, applied only to the magical loci. */
    MAGICAL("Random magical", "Rnd magical", Scope.MAGICAL, null, false, 0),

    /** A founder roll, applied only to {@link GeneFamily#NATURAL_DILUTION}. */
    NATURAL_DILUTION("Random natural dilution", "Rnd dilution", Scope.FAMILY, GeneFamily.NATURAL_DILUTION, false, 0),

    /** A founder roll, applied only to {@link GeneFamily#NATURAL_WHITE}. */
    NATURAL_WHITE("Random natural white", "Rnd white", Scope.FAMILY, GeneFamily.NATURAL_WHITE, false, 0),

    /**
     * Pick a breed at random, then roll a wild founder of it. The breed is a
     * real choice and not a label: it constrains every locus that follows.
     */
    BREED("Randomize breed", "Rnd breed", Scope.ALL, null, true, 0),

    /** A founder roll, then magical genes added until at least this many show. */
    PLUS_1_MAGICAL("Random +1 magical", "Rnd +1 magic", Scope.ALL, null, true, 1),
    PLUS_2_MAGICAL("Random +2 magical", "Rnd +2 magic", Scope.ALL, null, true, 2),
    PLUS_3_MAGICAL("Random +3 magical", "Rnd +3 magic", Scope.ALL, null, true, 3),

    /**
     * The epigenome and nothing else - the same thing the old <b>Reroll
     * epi.</b> button did, folded in here because it is a randomize and had no
     * business owning a button of its own.
     */
    EPIGENETICS("Randomize epigenetics", "Rnd epigen.", Scope.NONE, null, false, 0);

    /** Which loci a mode is allowed to touch. */
    public enum Scope {
        /** Every gene. */
        ALL,
        /** {@link Gene#isNatural()}. */
        NATURAL,
        /** Not {@link Gene#isNatural()}. */
        MAGICAL,
        /** One {@link GeneFamily}, named by {@link RandomizeMode#family()}. */
        FAMILY,
        /** No gene at all - the epigenome-only mode. */
        NONE
    }

    private final String label;
    private final String shortLabel;
    private final Scope scope;
    private final GeneFamily family;
    private final boolean rollsSex;
    private final int magicalFloor;

    RandomizeMode(String label, String shortLabel, Scope scope, GeneFamily family,
            boolean rollsSex, int magicalFloor) {
        this.label = label;
        this.shortLabel = shortLabel;
        this.scope = scope;
        this.family = family;
        this.rollsSex = rollsSex;
        this.magicalFloor = magicalFloor;
    }

    /** What the menu calls it. */
    public String label() {
        return label;
    }

    /**
     * What the button <i>face</i> calls it - the same thing in twelve
     * characters, because the right-hand column is 96 pixels wide on both
     * screens and "Random natural dilution" is not.
     */
    public String shortLabel() {
        return shortLabel;
    }

    public Scope scope() {
        return scope;
    }

    /** The family this mode re-rolls, or {@code null} unless {@link #scope} is {@code FAMILY}. */
    public GeneFamily family() {
        return family;
    }

    /** Whether the sex is re-rolled - <b>before</b> any allele is drawn. */
    public boolean rollsSex() {
        return rollsSex;
    }

    /** How many magical genes the horse must end up showing, or {@code 0} for no floor. */
    public int magicalFloor() {
        return magicalFloor;
    }

    /** Is this locus in scope for this mode? */
    public boolean covers(Gene gene) {
        return switch (scope) {
            case ALL -> true;
            case NATURAL -> gene.isNatural();
            case MAGICAL -> !gene.isNatural();
            case FAMILY -> GeneFamily.of(gene) == family;
            case NONE -> false;
        };
    }
}
