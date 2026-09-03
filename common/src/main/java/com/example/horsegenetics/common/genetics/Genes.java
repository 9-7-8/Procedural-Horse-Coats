package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.genetics.genes.AgoutiGene;
import com.example.horsegenetics.common.genetics.genes.DunGene;
import com.example.horsegenetics.common.genetics.genes.FrameGene;
import com.example.horsegenetics.common.genetics.genes.ChampagneGene;
import com.example.horsegenetics.common.genetics.genes.CreamGene;
import com.example.horsegenetics.common.genetics.genes.ExtensionGene;
import com.example.horsegenetics.common.genetics.genes.GreyGene;
import com.example.horsegenetics.common.genetics.genes.MagicZebraGene;
import com.example.horsegenetics.common.genetics.genes.MushroomGene;
import com.example.horsegenetics.common.genetics.genes.PearlGene;
import com.example.horsegenetics.common.genetics.genes.PinkHairGene;
import com.example.horsegenetics.common.genetics.genes.RoanGene;
import com.example.horsegenetics.common.genetics.genes.SabinoGene;
import com.example.horsegenetics.common.genetics.genes.SilverGene;
import com.example.horsegenetics.common.genetics.genes.SplashGene;
import com.example.horsegenetics.common.genetics.genes.TestGene;
import com.example.horsegenetics.common.genetics.genes.TobianoGene;
import com.example.horsegenetics.common.genetics.genes.WhiteGene;
import com.example.horsegenetics.common.genetics.spec.SpecGene;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The gene registry. Genes are addressed by {@code <modauthor>.<gene>}
 * ({@link #NS} for the built-ins). See <b>wiki/gene-*.html</b> for the full
 * description of every gene.
 *
 * <p>Three orderings, deliberately independent:
 * <ul>
 *   <li>{@link #codeOrder()} - position in the genotype code string.</li>
 *   <li>{@link #naturalOrder()} - the order the <b>natural</b> genes push
 *       pigment down in phase 1.</li>
 *   <li>{@link #magicalOrder()} - the order the <b>magical</b> genes add
 *       signed RGB in phase 3. Ordinary magical genes accumulate by integer
 *       addition and so are order-independent; the order still matters for a
 *       gene that paints flat.</li>
 * </ul>
 *
 * <h2>Built-ins, then data-driven genes</h2>
 * The eleven hand-written genes keep their fixed order, first, exactly as they
 * were - so adding a gene never changes an existing horse's coat. Genes loaded
 * from JSON ({@link SpecGene}, see {@code genetics.spec.GeneSpecLoader}) are
 * appended after them, sorted by their declared {@code priority} and then by
 * key. <b>Registration order is deliberately not respected</b>: two people who
 * drop the same two gene files in a different order must get the same horses,
 * which is the same argument as the hard-coded gene priority in
 * {@code wiki/roadmap.html} §2.
 *
 * <p>Register during startup, before anything parses a genotype - each
 * registration lengthens the genotype code by one segment, and codes written
 * before it will no longer parse. (Dev-only mod, no saves to keep; see the "no
 * legacy code" rule in {@code CLAUDE.md}.)
 */
public final class Genes {

    public static final String NS = "horsegenetics";

    public static final ExtensionGene EXTENSION = new ExtensionGene();
    public static final AgoutiGene AGOUTI = new AgoutiGene();
    public static final WhiteGene WHITE = new WhiteGene();
    public static final TestGene TEST = new TestGene();
    public static final ChampagneGene CHAMPAGNE = new ChampagneGene();
    public static final SplashGene SPLASH = new SplashGene();
    public static final GreyGene GREY = new GreyGene();
    public static final CreamGene CREAM = new CreamGene();
    public static final PearlGene PEARL = new PearlGene();
    public static final MagicZebraGene MAGIC_ZEBRA = new MagicZebraGene();
    public static final PinkHairGene PINK_HAIR = new PinkHairGene();
    public static final DunGene DUN = new DunGene();
    public static final SilverGene SILVER = new SilverGene();
    public static final MushroomGene MUSHROOM = new MushroomGene();
    public static final RoanGene ROAN = new RoanGene();
    public static final TobianoGene TOBIANO = new TobianoGene();
    public static final FrameGene FRAME = new FrameGene();
    public static final SabinoGene SABINO = new SabinoGene();

    private static final List<Gene> BUILTIN_CODE_ORDER =
            List.of(EXTENSION, AGOUTI, WHITE, TEST, CHAMPAGNE, SPLASH, GREY, CREAM, PEARL,
                    MAGIC_ZEBRA, PINK_HAIR, DUN, SILVER, MUSHROOM, ROAN, TOBIANO, FRAME, SABINO);

    private static final List<Gene> BUILTIN_NATURAL_ORDER =
            List.of(EXTENSION, AGOUTI, SILVER, MUSHROOM, DUN, CREAM, PEARL, CHAMPAGNE, GREY, WHITE,
                    ROAN, TOBIANO, FRAME, SABINO, SPLASH);

    /**
     * Pink hair and magic zebra both <i>add</i>, so their order between
     * themselves doesn't matter - but Test paints <b>flat</b>, and it is
     * {@code COMPLETE_DOMINANT} ("while it shows, nothing else is"), so it has
     * to run <b>last</b>. Loaded magical genes slot in between the two halves,
     * for the same reason.
     */
    private static final List<Gene> BUILTIN_MAGICAL_HEAD = List.of(PINK_HAIR, MAGIC_ZEBRA);
    private static final List<Gene> BUILTIN_MAGICAL_TAIL = List.of(TEST);

    private static final List<SpecGene> LOADED = new ArrayList<>();

    private static volatile List<Gene> codeOrder = BUILTIN_CODE_ORDER;
    private static volatile List<Gene> naturalOrder = BUILTIN_NATURAL_ORDER;
    private static volatile List<Gene> magicalOrder = concat(BUILTIN_MAGICAL_HEAD, BUILTIN_MAGICAL_TAIL);
    private static volatile Map<String, Gene> byKey = Map.of();
    private static volatile Map<String, Allele> alleleByKey = Map.of();

    static {
        rebuildIndexes();
    }

    private Genes() {}

    // ------------------------------------------------------------------
    // Registration
    // ------------------------------------------------------------------

    /**
     * Add a data-driven gene. Throws if its key is taken. Call during startup;
     * see the class note on why registration order does not decide gene order.
     */
    public static synchronized void register(SpecGene gene) {
        if (byKey.containsKey(gene.key())) {
            throw new IllegalArgumentException("a gene is already registered under " + gene.key());
        }
        LOADED.add(gene);
        rebuild();
    }

    public static synchronized void registerAll(Collection<SpecGene> genes) {
        for (SpecGene g : genes) {
            register(g);
        }
    }

    /** Every data-driven gene currently registered, in gene order. */
    public static List<SpecGene> loaded() {
        return List.copyOf(LOADED);
    }

    /** Drop every loaded gene, back to the eleven built-ins. Tests and reloads. */
    public static synchronized void clearLoaded() {
        LOADED.clear();
        rebuild();
    }

    private static void rebuild() {
        LOADED.sort(Comparator.comparingInt(SpecGene::priority).thenComparing(SpecGene::key));

        List<Gene> loadedNatural = new ArrayList<>();
        List<Gene> loadedMagical = new ArrayList<>();
        for (SpecGene g : LOADED) {
            (g.isNatural() ? loadedNatural : loadedMagical).add(g);
        }

        codeOrder = concat(BUILTIN_CODE_ORDER, LOADED);
        naturalOrder = concat(BUILTIN_NATURAL_ORDER, loadedNatural);
        List<Gene> magical = new ArrayList<>(BUILTIN_MAGICAL_HEAD);
        magical.addAll(loadedMagical);
        magical.addAll(BUILTIN_MAGICAL_TAIL);
        magicalOrder = List.copyOf(magical);

        rebuildIndexes();
        GenotypeCatalog.invalidate();
    }

    private static void rebuildIndexes() {
        Map<String, Gene> genes = new LinkedHashMap<>();
        Map<String, Allele> alleles = new LinkedHashMap<>();
        for (Gene g : codeOrder) {
            genes.put(g.key(), g);
            for (Allele a : g.alleles()) {
                alleles.put(a.key(), a);
            }
        }
        byKey = Map.copyOf(genes);
        alleleByKey = Map.copyOf(alleles);
    }

    private static List<Gene> concat(List<? extends Gene> a, List<? extends Gene> b) {
        List<Gene> out = new ArrayList<>(a);
        out.addAll(b);
        return List.copyOf(out);
    }

    // ------------------------------------------------------------------
    // Lookup
    // ------------------------------------------------------------------

    public static List<Gene> codeOrder() {
        return codeOrder;
    }

    public static List<Gene> naturalOrder() {
        return naturalOrder;
    }

    public static List<Gene> magicalOrder() {
        return magicalOrder;
    }

    public static List<Gene> all() {
        return codeOrder;
    }

    public static Gene byKey(String geneKey) {
        Gene g = byKey.get(geneKey);
        if (g == null) {
            throw new IllegalArgumentException("no gene registered under " + geneKey);
        }
        return g;
    }

    public static Allele allele(String alleleKey) {
        Allele a = alleleByKey.get(alleleKey);
        if (a == null) {
            throw new IllegalArgumentException("no allele registered under " + alleleKey);
        }
        return a;
    }
}
