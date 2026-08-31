package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.genetics.genes.AgoutiGene;
import com.example.horsegenetics.common.genetics.genes.ChampagneGene;
import com.example.horsegenetics.common.genetics.genes.ExtensionGene;
import com.example.horsegenetics.common.genetics.genes.SealGene;
import com.example.horsegenetics.common.genetics.genes.SplashGene;
import com.example.horsegenetics.common.genetics.genes.TestGene;
import com.example.horsegenetics.common.genetics.genes.WhiteGene;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The built-in gene registry. Genes are addressed by {@code <modauthor>.<gene>}
 * ({@link #NS} for these). See <b>Gene Dict.md</b> for the full description of
 * every gene.
 *
 * <p>Three orderings, deliberately independent:
 * <ul>
 *   <li>{@link #codeOrder()} - position in the genotype code string.</li>
 *   <li>{@link #naturalOrder()} - the order the <b>natural</b> genes push
 *       pigment down (all but Test).</li>
 *   <li>{@link #multiplyOrder()} - the order <b>non-natural</b> genes multiply
 *       their layer onto the resolved coat (Test only).</li>
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
    public static final SealGene SEAL = new SealGene();
    public static final SplashGene SPLASH = new SplashGene();

    private static final List<Gene> CODE_ORDER =
            List.of(EXTENSION, AGOUTI, WHITE, TEST, CHAMPAGNE, SEAL, SPLASH);

    // natural pigment-restriction pass, in effect order
    private static final List<Gene> NATURAL_ORDER =
            List.of(EXTENSION, AGOUTI, SEAL, CHAMPAGNE, WHITE, SPLASH);

    // non-natural multiply pass
    private static final List<Gene> MULTIPLY_ORDER = List.of(TEST);

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

    public static List<Gene> naturalOrder() {
        return NATURAL_ORDER;
    }

    public static List<Gene> multiplyOrder() {
        return MULTIPLY_ORDER;
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

    public static Allele allele(String alleleKey) {
        Allele a = ALLELE_BY_KEY.get(alleleKey);
        if (a == null) {
            throw new IllegalArgumentException("no allele registered under " + alleleKey);
        }
        return a;
    }
}
