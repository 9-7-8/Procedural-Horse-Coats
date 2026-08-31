package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.genetics.genes.AgoutiGene;
import com.example.horsegenetics.common.genetics.genes.ChampagneGene;
import com.example.horsegenetics.common.genetics.genes.ExtensionGene;
import com.example.horsegenetics.common.genetics.genes.TestGene;
import com.example.horsegenetics.common.genetics.genes.WhiteGene;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The built-in gene registry. Genes are addressed by
 * {@code <modauthor>.<gene>} ({@link #NS} for these); a third-party add-on
 * registers under its own namespace.
 *
 * <p>Three orderings matter and are deliberately independent:
 * <ul>
 *   <li>{@link #codeOrder()} - position in the compact genotype code string.
 *       <b>Append</b> new genes here so legacy shorter codes just pad with
 *       wild-type for the newer loci.</li>
 *   <li>{@link #restrictionOrder()} - the order genes get to push pigment down
 *       while a coat is built.</li>
 *   <li>{@link #paintOrder()} - the order direct-paint genes draw, after the
 *       pigment field is resolved to colour.</li>
 * </ul>
 */
public final class Genes {

    /** Namespace for the built-in genes ({@code <modauthor>}). */
    public static final String NS = "horsegenetics";

    public static final ExtensionGene EXTENSION = new ExtensionGene();
    public static final AgoutiGene AGOUTI = new AgoutiGene();
    public static final WhiteGene WHITE = new WhiteGene();
    public static final TestGene TEST = new TestGene();
    public static final ChampagneGene CHAMPAGNE = new ChampagneGene();

    private static final List<Gene> CODE_ORDER = List.of(EXTENSION, AGOUTI, WHITE, TEST, CHAMPAGNE);
    private static final List<Gene> RESTRICTION_ORDER = List.of(EXTENSION, AGOUTI, CHAMPAGNE, WHITE);
    private static final List<Gene> PAINT_ORDER = List.of(TEST);

    private static final Map<String, Gene> BY_KEY = new LinkedHashMap<>();
    private static final Map<String, Allele> ALLELE_BY_KEY = new LinkedHashMap<>();

    static {
        for (Gene g : CODE_ORDER) {
            BY_KEY.put(g.key(), g);
            for (Allele a : g.alleles()) {
                ALLELE_BY_KEY.put(a.key(), a);
            }
        }
    }

    private Genes() {}

    public static List<Gene> codeOrder() {
        return CODE_ORDER;
    }

    public static List<Gene> restrictionOrder() {
        return RESTRICTION_ORDER;
    }

    public static List<Gene> paintOrder() {
        return PAINT_ORDER;
    }

    public static List<Gene> all() {
        return CODE_ORDER;
    }

    public static Gene byKey(String geneKey) {
        Gene g = BY_KEY.get(geneKey);
        if (g == null) {
            throw new IllegalArgumentException("no gene registered under " + geneKey);
        }
        return g;
    }

    /** Look up an allele by its full primary key, e.g. {@code "horsegenetics.agouti.At"}. */
    public static Allele allele(String alleleKey) {
        Allele a = ALLELE_BY_KEY.get(alleleKey);
        if (a == null) {
            throw new IllegalArgumentException("no allele registered under " + alleleKey);
        }
        return a;
    }

    /** Length of a full canonical genotype code (2 chars per gene). */
    public static int codeLength() {
        return CODE_ORDER.size() * 2;
    }
}
