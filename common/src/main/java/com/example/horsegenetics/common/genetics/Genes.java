package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.genetics.genes.AgoutiGene;
import com.example.horsegenetics.common.genetics.genes.DunGene;
import com.example.horsegenetics.common.genetics.genes.FrameGene;
import com.example.horsegenetics.common.genetics.genes.ChampagneGene;
import com.example.horsegenetics.common.genetics.genes.ExtensionGene;
import com.example.horsegenetics.common.genetics.genes.GreyGene;
import com.example.horsegenetics.common.genetics.genes.MagicZebraGene;
import com.example.horsegenetics.common.genetics.genes.MushroomGene;
import com.example.horsegenetics.common.genetics.genes.MatpGene;
import com.example.horsegenetics.common.genetics.genes.PinkHairGene;
import com.example.horsegenetics.common.genetics.genes.RoanGene;
import com.example.horsegenetics.common.genetics.genes.SabinoGene;
import com.example.horsegenetics.common.genetics.genes.SilverGene;
import com.example.horsegenetics.common.genetics.genes.SplashGene;
import com.example.horsegenetics.common.genetics.genes.TestGene;
import com.example.horsegenetics.common.genetics.genes.TobianoGene;
import com.example.horsegenetics.common.genetics.genes.WhiteGene;
import com.example.horsegenetics.common.genetics.spec.SpecGene;

import java.lang.System.Logger;
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
 * <h2>One order, derived</h2>
 * There is a single processing order: <b>every</b> registered gene - built-in
 * and data-driven alike - sorted on {@code (}{@link Gene#priority()}{@code ,
 * key)}. The three public views are all a <i>result</i> of that one sort, never
 * a source:
 * <ul>
 *   <li>{@link #codeOrder()} / {@link #all()} - the whole sorted list;
 *       position in the genotype code follows it.</li>
 *   <li>{@link #naturalOrder()} - the sorted list filtered to
 *       {@link Gene#isNatural()} - the order natural genes push pigment down in
 *       phase 1.</li>
 *   <li>{@link #magicalOrder()} - the sorted list filtered to the magical
 *       genes - the order they add signed RGB in phase 3.</li>
 * </ul>
 * The natural / magical <b>phase</b> (not the priority number) is what splits
 * the two coat passes; the {@code 0-99} / {@code 100+} bands are only a
 * convention, and {@link #register} logs a warning when a gene sits outside its
 * band. <b>Registration order is never respected</b>: two people who drop the
 * same gene files in a different order must get the same horses.
 *
 * <p>Register during startup, before anything parses a genotype - each
 * registration can move where a gene sits in the code, and a code written
 * against the old order still parses (a gene now absent from it reads as wild
 * type) but is a different genotype. Dev-only mod, no saves to keep; see the
 * "no legacy code" rule in {@code CLAUDE.md}.
 */
public final class Genes {

    public static final String NS = "horsegenetics";

    private static final Logger LOG = System.getLogger("horsegenetics.genetics");

    /** The lowest priority in the magical band - below this a gene is "natural" by convention. */
    public static final int MAGICAL_BAND_START = 100;

    public static final ExtensionGene EXTENSION = new ExtensionGene();
    public static final AgoutiGene AGOUTI = new AgoutiGene();
    public static final WhiteGene WHITE = new WhiteGene();
    public static final TestGene TEST = new TestGene();
    public static final ChampagneGene CHAMPAGNE = new ChampagneGene();
    public static final SplashGene SPLASH = new SplashGene();
    public static final GreyGene GREY = new GreyGene();
    public static final MatpGene MATP = new MatpGene();
    public static final MagicZebraGene MAGIC_ZEBRA = new MagicZebraGene();
    public static final PinkHairGene PINK_HAIR = new PinkHairGene();
    public static final DunGene DUN = new DunGene();
    public static final SilverGene SILVER = new SilverGene();
    public static final MushroomGene MUSHROOM = new MushroomGene();
    public static final RoanGene ROAN = new RoanGene();
    public static final TobianoGene TOBIANO = new TobianoGene();
    public static final FrameGene FRAME = new FrameGene();
    public static final SabinoGene SABINO = new SabinoGene();

    /** The hand-written genes. Order here is irrelevant - the registry sorts. */
    private static final List<Gene> BUILTINS = List.of(
            EXTENSION, AGOUTI, WHITE, TEST, CHAMPAGNE, SPLASH, GREY, MATP,
            MAGIC_ZEBRA, PINK_HAIR, DUN, SILVER, MUSHROOM, ROAN, TOBIANO, FRAME, SABINO);

    /** Ordering: lower priority first, ties broken alphabetically by key. */
    private static final Comparator<Gene> BY_PRIORITY_THEN_KEY =
            Comparator.comparingInt(Gene::priority).thenComparing(Gene::key);

    private static final List<SpecGene> LOADED = new ArrayList<>();

    private static volatile List<Gene> order = List.of();
    private static volatile List<Gene> naturalOrder = List.of();
    private static volatile List<Gene> magicalOrder = List.of();
    private static volatile Map<String, Gene> byKey = Map.of();
    private static volatile Map<String, Allele> alleleByKey = Map.of();

    static {
        rebuild();
    }

    private Genes() {}

    // ------------------------------------------------------------------
    // Registration
    // ------------------------------------------------------------------

    /**
     * Add a data-driven gene. Throws if its key is taken (that is genuinely
     * unrecoverable); warns, but carries on, if its priority sits outside its
     * phase's conventional band. Call during startup; the gene sorts into the
     * one {@code (priority, key)} order, so registration order does not decide
     * where it lands.
     */
    public static synchronized void register(SpecGene gene) {
        if (byKey.containsKey(gene.key())) {
            throw new IllegalArgumentException("a gene is already registered under " + gene.key());
        }
        checkBand(gene);
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
        List<SpecGene> out = new ArrayList<>(LOADED);
        out.sort(BY_PRIORITY_THEN_KEY);
        return List.copyOf(out);
    }

    /** Drop every loaded gene, back to the built-ins. Tests and reloads. */
    public static synchronized void clearLoaded() {
        LOADED.clear();
        rebuild();
    }

    private static void checkBand(Gene gene) {
        boolean magicalByNumber = gene.priority() >= MAGICAL_BAND_START;
        if (gene.isNatural() && magicalByNumber) {
            LOG.log(Logger.Level.WARNING, "gene {0} is natural but its priority {1} is in the magical band (>= {2})",
                    gene.key(), gene.priority(), MAGICAL_BAND_START);
        } else if (!gene.isNatural() && !magicalByNumber) {
            LOG.log(Logger.Level.WARNING, "gene {0} is magical but its priority {1} is in the natural band (< {2})",
                    gene.key(), gene.priority(), MAGICAL_BAND_START);
        }
    }

    private static void rebuild() {
        LOADED.sort(BY_PRIORITY_THEN_KEY);

        List<Gene> all = new ArrayList<>(BUILTINS.size() + LOADED.size());
        all.addAll(BUILTINS);
        all.addAll(LOADED);
        all.sort(BY_PRIORITY_THEN_KEY);
        order = List.copyOf(all);

        List<Gene> natural = new ArrayList<>();
        List<Gene> magical = new ArrayList<>();
        for (Gene g : order) {
            (g.isNatural() ? natural : magical).add(g);
        }
        naturalOrder = List.copyOf(natural);
        magicalOrder = List.copyOf(magical);

        Map<String, Gene> keys = new LinkedHashMap<>();
        Map<String, Allele> alleles = new LinkedHashMap<>();
        for (Gene g : order) {
            keys.put(g.key(), g);
            for (Allele a : g.alleles()) {
                alleles.put(a.key(), a);
            }
        }
        byKey = Map.copyOf(keys);
        alleleByKey = Map.copyOf(alleles);

        GenotypeCatalog.invalidate();
    }

    // ------------------------------------------------------------------
    // Lookup
    // ------------------------------------------------------------------

    public static List<Gene> codeOrder() {
        return order;
    }

    public static List<Gene> naturalOrder() {
        return naturalOrder;
    }

    public static List<Gene> magicalOrder() {
        return magicalOrder;
    }

    public static List<Gene> all() {
        return order;
    }

    public static Gene byKey(String geneKey) {
        Gene g = byKey.get(geneKey);
        if (g == null) {
            throw new IllegalArgumentException("no gene registered under " + geneKey);
        }
        return g;
    }

    /**
     * The gene registered under {@code geneKey}, or {@code null} if none - a
     * genotype-code segment naming an unregistered gene is dropped, not an
     * error, so parsing can stay tolerant across a gene being added or removed.
     */
    public static Gene byKeyOrNull(String geneKey) {
        return byKey.get(geneKey);
    }

    public static Allele allele(String alleleKey) {
        Allele a = alleleByKey.get(alleleKey);
        if (a == null) {
            throw new IllegalArgumentException("no allele registered under " + alleleKey);
        }
        return a;
    }
}
