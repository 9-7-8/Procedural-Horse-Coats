package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.genetics.genes.AcanGene;
import com.example.horsegenetics.common.genetics.genes.AgoutiGene;
import com.example.horsegenetics.common.genetics.genes.B4galt7Gene;
import com.example.horsegenetics.common.genetics.genes.CkmGene;
import com.example.horsegenetics.common.genetics.genes.DunGene;
import com.example.horsegenetics.common.genetics.genes.EdnrbGene;
import com.example.horsegenetics.common.genetics.genes.ChampagneGene;
import com.example.horsegenetics.common.genetics.genes.ExtensionGene;
import com.example.horsegenetics.common.genetics.genes.GreyGene;
import com.example.horsegenetics.common.genetics.genes.HealerGene;
import com.example.horsegenetics.common.genetics.genes.Hmga2Gene;
import com.example.horsegenetics.common.genetics.genes.LcorlGene;
import com.example.horsegenetics.common.genetics.genes.LightGene;
import com.example.horsegenetics.common.genetics.genes.MagicHealthGene;
import com.example.horsegenetics.common.genetics.genes.MagicJumpGene;
import com.example.horsegenetics.common.genetics.genes.MagicSizeGene;
import com.example.horsegenetics.common.genetics.genes.MagicSpeedGene;
import com.example.horsegenetics.common.genetics.genes.MagicZebraGene;
import com.example.horsegenetics.common.genetics.genes.ManeColorGene;
import com.example.horsegenetics.common.genetics.genes.MilkGene;
import com.example.horsegenetics.common.genetics.genes.MushroomGene;
import com.example.horsegenetics.common.genetics.genes.MatpGene;
import com.example.horsegenetics.common.genetics.genes.MetGene;
import com.example.horsegenetics.common.genetics.genes.MstnGene;
import com.example.horsegenetics.common.genetics.genes.Pdk4Gene;
import com.example.horsegenetics.common.genetics.genes.PinkHairGene;
import com.example.horsegenetics.common.genetics.genes.Plod1Gene;
import com.example.horsegenetics.common.genetics.genes.Rapgef5Gene;
import com.example.horsegenetics.common.genetics.genes.RoanGene;
import com.example.horsegenetics.common.genetics.genes.KitGene;
import com.example.horsegenetics.common.genetics.genes.SexGene;
import com.example.horsegenetics.common.genetics.genes.Ryr2Gene;
import com.example.horsegenetics.common.genetics.genes.ShoxGene;
import com.example.horsegenetics.common.genetics.genes.SilverGene;
import com.example.horsegenetics.common.genetics.genes.St14Gene;
import com.example.horsegenetics.common.genetics.genes.MitfGene;
import com.example.horsegenetics.common.genetics.genes.TestGene;
import com.example.horsegenetics.common.genetics.genes.TailColorGene;
import com.example.horsegenetics.common.genetics.genes.TobianoGene;
import com.example.horsegenetics.common.genetics.genes.VerdantGene;
import com.example.horsegenetics.common.genetics.genes.ParticleGene;
import com.example.horsegenetics.common.genetics.genes.Pax3Gene;
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

    /** Sex, at priority 1 - the first gene resolved, and the only one that paints nothing. */
    public static final SexGene SEX = new SexGene();
    public static final ExtensionGene EXTENSION = new ExtensionGene();
    public static final AgoutiGene AGOUTI = new AgoutiGene();
    public static final TestGene TEST = new TestGene();
    public static final ChampagneGene CHAMPAGNE = new ChampagneGene();
    public static final GreyGene GREY = new GreyGene();
    public static final MatpGene MATP = new MatpGene();
    public static final MagicZebraGene MAGIC_ZEBRA = new MagicZebraGene();
    public static final PinkHairGene PINK_HAIR = new PinkHairGene();
    public static final DunGene DUN = new DunGene();
    public static final SilverGene SILVER = new SilverGene();
    public static final MushroomGene MUSHROOM = new MushroomGene();
    public static final RoanGene ROAN = new RoanGene();
    public static final TobianoGene TOBIANO = new TobianoGene();

    /**
     * The four <b>white-pattern loci</b>, named for the real genes they model.
     * Keeping them apart is not pedantry: only alleles at the <i>same</i> locus
     * compete for a slot, so {@code KIT} can hold dominant white or sabino but
     * never both, while {@code MITF} and {@code PAX3} splash stack freely and
     * frame stacks with everything. Tobiano and roan sit near {@code KIT} on
     * chromosome 3 but are not {@code KIT} variants, so they stay their own
     * genes above.
     */
    public static final EdnrbGene EDNRB = new EdnrbGene();
    public static final KitGene KIT = new KitGene();
    public static final MitfGene MITF = new MitfGene();
    public static final Pax3Gene PAX3 = new Pax3Gene();

    /**
     * The <b>magical utility genes</b> - the second wave of magic, and the first
     * genes whose point is what the horse <i>does</i> rather than what it looks
     * like. Several of them ({@link MilkGene}, {@link MagicSizeGene},
     * {@link MagicSpeedGene}, {@link MagicHealthGene}, {@link MagicJumpGene},
     * {@link LightGene}, {@link VerdantGene}, {@link HealerGene}) reach the game
     * through {@link AbilityContribution} or
     * {@link com.example.horsegenetics.common.trait.EpigeneticTraitContribution}
     * rather than through the coat.
     *
     * <p>They were designed as a set rather than one at a time, with broad
     * epigenetic ranges, so that they <b>combine</b>: a ten-times healer with a
     * striped mane that spreads moss is a horse nobody wrote a line of code for.
     * The mane and tail loci are separate on purpose, and light is codominant on
     * purpose, for the same reason - each doubling of the outcome space is a
     * doubling of what a breeder can aim at.
     */
    public static final MilkGene MILK = new MilkGene();
    public static final MagicSizeGene BODY_SIZE = new MagicSizeGene();
    public static final MagicSpeedGene MAGIC_SPEED = new MagicSpeedGene();
    public static final MagicHealthGene MAGIC_HEALTH = new MagicHealthGene();
    public static final MagicJumpGene MAGIC_JUMP = new MagicJumpGene();
    public static final ManeColorGene MANE_COLOR = new ManeColorGene();
    public static final TailColorGene TAIL_COLOR = new TailColorGene();
    public static final ParticleGene PARTICLE = new ParticleGene();
    public static final LightGene LIGHT = new LightGene();
    public static final HealerGene HEALER = new HealerGene();
    public static final VerdantGene VERDANT = new VerdantGene();

    /**
     * The <b>non-coat genes</b> - performance, size and health. They occupy the
     * top of the natural band ({@code 80}-{@code 99}), after every gene that
     * paints, because <b>none of them paints anything</b>: every combination
     * they can produce is a {@link Expression#wildType() wild type}, so
     * {@link Gene#affectsCoat()} is false for all of them, they are left out of
     * a horse's texture key, and the genotype gallery collapses each of them to
     * a single entry however many alleles it has. What they do instead travels
     * through {@link com.example.horsegenetics.common.trait.HorseTraits}: speed,
     * max health, jump strength, body size, and the disorders a horse expresses.
     *
     * <p>Their position among <i>themselves</i> is arbitrary - trait
     * contributions are additive and order-independent by construction (see
     * {@link com.example.horsegenetics.common.trait.TraitBuilder}) - so the
     * numbers here only fix a stable slot in the genotype code.
     */
    public static final MstnGene MSTN = new MstnGene();
    public static final Pdk4Gene PDK4 = new Pdk4Gene();
    public static final CkmGene CKM = new CkmGene();
    public static final Ryr2Gene RYR2 = new Ryr2Gene();
    public static final LcorlGene LCORL = new LcorlGene();
    public static final Hmga2Gene HMGA2 = new Hmga2Gene();

    /**
     * The <b>health</b> loci. Every one of them is recessive and every one of
     * them is absent from its own founder table as a homozygote - a wild-caught
     * horse is an adult that survived, so it can carry a disorder but never have
     * one. The only way to see any of these is to breed two carriers, which is
     * the whole design: it makes a pedigree worth keeping.
     */
    public static final AcanGene ACAN = new AcanGene();
    public static final B4galt7Gene B4GALT7 = new B4galt7Gene();
    public static final Plod1Gene PLOD1 = new Plod1Gene();
    public static final Rapgef5Gene RAPGEF5 = new Rapgef5Gene();
    public static final St14Gene ST14 = new St14Gene();
    public static final ShoxGene SHOX = new ShoxGene();
    public static final MetGene MET = new MetGene();

    /** The hand-written genes. Order here is irrelevant - the registry sorts. */
    private static final List<Gene> BUILTINS = List.of(
            SEX, EXTENSION, AGOUTI, TEST, CHAMPAGNE, GREY, MATP,
            MAGIC_ZEBRA, PINK_HAIR, DUN, SILVER, MUSHROOM, ROAN, TOBIANO,
            EDNRB, KIT, MITF, PAX3,
            MILK, BODY_SIZE, MAGIC_SPEED, MAGIC_HEALTH, MAGIC_JUMP,
            MANE_COLOR, TAIL_COLOR, PARTICLE, LIGHT, HEALER, VERDANT,
            MSTN, PDK4, CKM, RYR2, LCORL, HMGA2,
            ACAN, B4GALT7, PLOD1, RAPGEF5, ST14, SHOX, MET);

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
