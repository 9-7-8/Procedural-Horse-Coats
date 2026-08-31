package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.Rng;

import java.util.Objects;

/**
 * A horse's genotype at the loci modeled so far:
 *
 * <ul>
 *   <li><b>E (extension)</b>: 'E' dominant (black pigment producible), 'e' recessive
 *       (no black pigment at all - chestnut, regardless of agouti).</li>
 *   <li><b>A (agouti)</b>: 'A' dominant (restricts black to points - bay), 'a' recessive
 *       (black unrestricted - solid black). Only expressed if at least one 'E' is present.</li>
 *   <li><b>W (white)</b>: 'W' dominant (solid white horse, no markings - masks
 *       everything else), 'w' recessive (no effect). Rare in wild horses.</li>
 * </ul>
 *
 * <p>Stored and transmitted as a <b>6-character</b> code: two E alleles, then two A
 * alleles, then two W alleles, e.g. {@code "EeAaww"}, {@code "eeaaWw"}. Within each
 * locus the dominant allele is always written first (canonical form), so "eE" is
 * normalized to "Ee" on parse. Legacy <b>4-character</b> codes (before the W locus
 * existed) are still accepted and read as {@code ww}. Pure data + logic, no Minecraft
 * dependency - ports to any version unchanged.
 */
public final class Genotype {

    /** One in this many for each W allele of a wild horse to be dominant 'W'. Tune for rarity. */
    public static final int WILD_WHITE_ALLELE_ODDS = 50;

    private final char e1, e2, a1, a2, w1, w2;

    private Genotype(char e1, char e2, char a1, char a2, char w1, char w2) {
        this.e1 = e1;
        this.e2 = e2;
        this.a1 = a1;
        this.a2 = a2;
        this.w1 = w1;
        this.w2 = w2;
    }

    /**
     * Parses a genotype code. Accepts the canonical 6-character form
     * (EE/Ee/ee, AA/Aa/aa, WW/Ww/ww) or a legacy 4-character code (no W locus,
     * read as {@code ww}). Throws {@link IllegalArgumentException} on malformed
     * input. Allele order within a locus does not matter.
     */
    public static Genotype parse(String code) {
        Objects.requireNonNull(code, "code");
        String c = code.length() == 4 ? code + "ww" : code; // legacy 2-locus code
        if (c.length() != 6) {
            throw new IllegalArgumentException(
                    "Genotype code must be 6 characters (EE/Ee/ee then AA/Aa/aa then WW/Ww/ww), "
                            + "or a legacy 4-character code; got: " + code);
        }
        char[] e = sortLocus(c.charAt(0), c.charAt(1), 'E', 'e');
        char[] a = sortLocus(c.charAt(2), c.charAt(3), 'A', 'a');
        char[] w = sortLocus(c.charAt(4), c.charAt(5), 'W', 'w');
        return new Genotype(e[0], e[1], a[0], a[1], w[0], w[1]);
    }

    /** Builds a Genotype from six alleles, two per locus, in any order. */
    public static Genotype of(char e1, char e2, char a1, char a2, char w1, char w2) {
        char[] e = sortLocus(e1, e2, 'E', 'e');
        char[] a = sortLocus(a1, a2, 'A', 'a');
        char[] w = sortLocus(w1, w2, 'W', 'w');
        return new Genotype(e[0], e[1], a[0], a[1], w[0], w[1]);
    }

    /** Convenience: E and A alleles only, W locus defaulted to {@code ww}. */
    public static Genotype of(char e1, char e2, char a1, char a2) {
        return of(e1, e2, a1, a2, 'w', 'w');
    }

    /**
     * Generates a random genotype: 50/50 per allele at the E and A loci, and a
     * rare 'W' at the white locus (1 in {@value #WILD_WHITE_ALLELE_ODDS} per
     * allele). Placeholder for real population genetics.
     */
    public static Genotype random(Rng rng) {
        char e1 = rng.nextBoolean() ? 'E' : 'e';
        char e2 = rng.nextBoolean() ? 'E' : 'e';
        char a1 = rng.nextBoolean() ? 'A' : 'a';
        char a2 = rng.nextBoolean() ? 'A' : 'a';
        char w1 = rng.nextInt(WILD_WHITE_ALLELE_ODDS) == 0 ? 'W' : 'w';
        char w2 = rng.nextInt(WILD_WHITE_ALLELE_ODDS) == 0 ? 'W' : 'w';
        return of(e1, e2, a1, a2, w1, w2);
    }

    /**
     * Mendelian breeding: the child inherits one randomly chosen allele from
     * this parent and one from {@code other} at each locus, independently.
     * Draw order: E from this, E from other, A from this, A from other, W from
     * this, W from other. Allele order within a locus is canonicalized by
     * {@link #of}, so the result does not depend on which parent is "this".
     */
    public Genotype breedWith(Genotype other, Rng rng) {
        char childE1 = rng.nextBoolean() ? this.e1 : this.e2;
        char childE2 = rng.nextBoolean() ? other.e1 : other.e2;
        char childA1 = rng.nextBoolean() ? this.a1 : this.a2;
        char childA2 = rng.nextBoolean() ? other.a1 : other.a2;
        char childW1 = rng.nextBoolean() ? this.w1 : this.w2;
        char childW2 = rng.nextBoolean() ? other.w1 : other.w2;
        return of(childE1, childE2, childA1, childA2, childW1, childW2);
    }

    private static char[] sortLocus(char x, char y, char dominant, char recessive) {
        boolean xOk = (x == dominant || x == recessive);
        boolean yOk = (y == dominant || y == recessive);
        if (!xOk || !yOk) {
            throw new IllegalArgumentException(
                    "Expected alleles '" + dominant + "'/'" + recessive + "', got '" + x + "' and '" + y + "'");
        }
        if (x == dominant || y == dominant) {
            return x == dominant ? new char[] {x, y} : new char[] {y, x};
        }
        return new char[] {recessive, recessive};
    }

    /** True if at least one dominant 'W' allele is present - a solid white horse. */
    public boolean isWhite() {
        return w1 == 'W' || w2 == 'W';
    }

    /** True if at least one dominant 'E' allele is present. */
    public boolean hasBlackPigment() {
        return e1 == 'E' || e2 == 'E';
    }

    /** True if at least one dominant 'A' allele is present. Only meaningful when hasBlackPigment(). */
    public boolean isAgouti() {
        return a1 == 'A' || a2 == 'A';
    }

    public CoatPhenotype phenotype() {
        if (isWhite()) {
            return CoatPhenotype.WHITE;
        }
        if (!hasBlackPigment()) {
            return CoatPhenotype.CHESTNUT;
        }
        return isAgouti() ? CoatPhenotype.BAY : CoatPhenotype.BLACK;
    }

    /** Serializes back to the canonical 6-character code, e.g. "EeAaww". */
    public String toCode() {
        return "" + e1 + e2 + a1 + a2 + w1 + w2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Genotype other)) return false;
        return e1 == other.e1 && e2 == other.e2 && a1 == other.a1 && a2 == other.a2
                && w1 == other.w1 && w2 == other.w2;
    }

    @Override
    public int hashCode() {
        return Objects.hash(e1, e2, a1, a2, w1, w2);
    }

    @Override
    public String toString() {
        return "Genotype[" + toCode() + " -> " + phenotype() + "]";
    }
}
