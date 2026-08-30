package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.Rng;

import java.util.Objects;

/**
 * A horse's genotype at the two loci modeled so far:
 *
 * <ul>
 *   <li><b>E (extension)</b>: 'E' dominant (black pigment producible), 'e' recessive
 *       (no black pigment at all - chestnut, regardless of agouti).</li>
 *   <li><b>A (agouti)</b>: 'A' dominant (restricts black to points - bay), 'a' recessive
 *       (black unrestricted - solid black). Only expressed if at least one 'E' is present.</li>
 * </ul>
 *
 * <p>Stored and transmitted as a 4-character code: the two E alleles followed by the
 * two A alleles, e.g. {@code "EeAa"}, {@code "eeaa"}, {@code "EEAA"}. Within each locus
 * the dominant allele is always written first (canonical form), so "eE" is normalized
 * to "Ee" on parse. This class is pure data + logic - no Minecraft dependency, so it
 * ports to any Minecraft version unchanged.
 */
public final class Genotype {

    private final char e1, e2, a1, a2;

    private Genotype(char e1, char e2, char a1, char a2) {
        this.e1 = e1;
        this.e2 = e2;
        this.a1 = a1;
        this.a2 = a2;
    }

    /**
     * Parses a 4-character genotype code. Throws IllegalArgumentException on malformed input.
     * Allele order within a locus does not matter - "eE" and "Ee" parse to the same Genotype.
     */
    public static Genotype parse(String code) {
        Objects.requireNonNull(code, "code");
        if (code.length() != 4) {
            throw new IllegalArgumentException(
                    "Genotype code must be exactly 4 characters (EE/Ee/ee then AA/Aa/aa), got: " + code);
        }
        char[] eAlleles = sortLocus(code.charAt(0), code.charAt(1), 'E', 'e');
        char[] aAlleles = sortLocus(code.charAt(2), code.charAt(3), 'A', 'a');
        return new Genotype(eAlleles[0], eAlleles[1], aAlleles[0], aAlleles[1]);
    }

    /** Builds a Genotype directly from four alleles, two per locus, in any order. */
    public static Genotype of(char e1, char e2, char a1, char a2) {
        char[] eAlleles = sortLocus(e1, e2, 'E', 'e');
        char[] aAlleles = sortLocus(a1, a2, 'A', 'a');
        return new Genotype(eAlleles[0], eAlleles[1], aAlleles[0], aAlleles[1]);
    }

    /**
     * Generates a random genotype with a 50/50 chance of each allele at each locus,
     * independently. This is a placeholder for real population genetics / breeding -
     * for now every wild-spawned horse just rolls independently.
     */
    public static Genotype random(Rng rng) {
        char e1 = rng.nextBoolean() ? 'E' : 'e';
        char e2 = rng.nextBoolean() ? 'E' : 'e';
        char a1 = rng.nextBoolean() ? 'A' : 'a';
        char a2 = rng.nextBoolean() ? 'A' : 'a';
        return of(e1, e2, a1, a2);
    }

    private static char[] sortLocus(char x, char y, char dominant, char recessive) {
        boolean xOk = (x == dominant || x == recessive);
        boolean yOk = (y == dominant || y == recessive);
        if (!xOk || !yOk) {
            throw new IllegalArgumentException(
                    "Expected alleles '" + dominant + "'/'" + recessive + "', got '" + x + "' and '" + y + "'");
        }
        if (x == dominant || y == dominant) {
            // At least one dominant allele - put it first. Covers homozygous dominant too.
            return x == dominant ? new char[] {x, y} : new char[] {y, x};
        }
        return new char[] {recessive, recessive};
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
        if (!hasBlackPigment()) {
            return CoatPhenotype.CHESTNUT;
        }
        return isAgouti() ? CoatPhenotype.BAY : CoatPhenotype.BLACK;
    }

    /** Serializes back to the canonical 4-character code, e.g. "EeAa". */
    public String toCode() {
        return "" + e1 + e2 + a1 + a2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Genotype other)) return false;
        return e1 == other.e1 && e2 == other.e2 && a1 == other.a1 && a2 == other.a2;
    }

    @Override
    public int hashCode() {
        return Objects.hash(e1, e2, a1, a2);
    }

    @Override
    public String toString() {
        return "Genotype[" + toCode() + " -> " + phenotype() + "]";
    }
}
