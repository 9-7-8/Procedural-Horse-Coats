package com.example.horsegenetics.common.genetics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * What <b>kind</b> of gene this is - the grouping a person reading a list of a
 * hundred and seventy loci needs before the list means anything.
 *
 * <p>It is a view, not a mechanic. Nothing in the coat pipeline, the founder
 * roll or breeding asks a gene what family it is in; the pipeline splits on
 * {@link Gene#isNatural()} and orders on {@link Gene#priority()}, and that has
 * not changed. This exists because two screens and one wiki generator all
 * needed the same grouping and were about to grow three different ones.
 *
 * <h2>Who reads it</h2>
 * <ul>
 *   <li>the gene-list filter on the custom spawn egg and on the browser
 *       designer - the two are the same screen, so the two menus must offer the
 *       same groups in the same order;</li>
 *   <li>those screens' <i>Random natural dilution</i> / <i>Random natural
 *       white</i> / <i>Random magical</i> rolls, which are exactly "re-roll one
 *       family";</li>
 *   <li>{@code GeneWikiTool}, which lays the magical genes out one page per
 *       family, groups the wiki sidebar and the landing page's gene cards by
 *       family, and takes every slug, title and lede from here.</li>
 * </ul>
 *
 * <p>That last one is why every family carries a {@link #title} and a
 * {@link #lede} and not only the seven with a page of their own: the sidebar
 * section, the landing-page heading and the paragraph under it are all this
 * table, so the wiki cannot group genes one way and the two editors another.
 *
 * <h2>How a gene lands in one</h2>
 * <b>A gene declares no family.</b> It does not need to, and asking eighty-odd
 * data-driven gene files to name one would be eighty-odd chances to disagree
 * with where the gene actually paints. Instead:
 *
 * <ul>
 *   <li><b>Magical genes</b> band on {@link Gene#priority()}. The magical
 *       priority bands were laid out as families in the first place - spots
 *       together, strokes together, the things that read the coat beneath them
 *       last - because the paint order and the taxonomy want the same grouping
 *       for the same reason.</li>
 *   <li><b>Natural genes</b> that change nothing visible are
 *       {@link #NATURAL_HEALTH}, asked of {@link Genes#influencesCoat}. The
 *       rest band on priority as well, with the short {@link #NATURAL_OVERRIDES}
 *       table below for the handful whose slot in the paint order fights their
 *       family - which is a real thing and not an oversight: {@link Genes#SHADE}
 *       paints nothing itself, so it is parked at 95 among the health loci, and
 *       it is nonetheless one of the three genes that decide what colour a horse
 *       is.</li>
 * </ul>
 *
 * <p>A gene nobody thought about still gets a family, and gets the least wrong
 * one - which is the property worth having, because the alternative is a gene
 * that quietly stops appearing in the list.
 */
public enum GeneFamily {

    NATURAL_COAT("Natural coat genes", true, null, "Natural coat genes",
            "The pigment every other gene then works on: whether the horse can make black at "
                    + "all, where that black is allowed to go, and the countershading over the "
                    + "top of it."),
    NATURAL_DILUTION("Natural dilution genes", true, null, "Natural dilution genes",
            "Genes that lighten what is already there. Each takes red, or black, or both down "
                    + "by some amount - which is why the same dilution reads completely "
                    + "differently on a chestnut and on a black."),
    NATURAL_WHITE("Natural white genes", true, null, "Natural white genes",
            "Genes that take the coat away entirely in places. Only alleles at the same locus "
                    + "compete for a slot, which is why a horse can be tobiano and splashed and "
                    + "framed at once but never dominant white and sabino."),
    NATURAL_HEALTH("Natural health genes", true, null, "Natural health genes",
            "The loci you cannot see: speed, size, jump, and the disorders. Almost every one "
                    + "is recessive and absent from its own founder table as a homozygote - a "
                    + "wild horse can carry a disorder but never have one."),
    NATURAL_OTHER("Other natural genes", true, null, "Other natural genes",
            "The naturals that are neither pigment nor pattern nor disorder - what the horse "
                    + "will eat, and what colour its eyes are."),

    /** The hand-written magical genes, the way {@code wiki/pages.js} groups them. */
    MAGIC_CORE("Magical genes", false, null, "Magical genes",
            "Where the magic starts. Every gene from here down was invented for this mod and "
                    + "adds signed colour in phase 3, on top of the pigment the natural genes "
                    + "have already resolved - or changes what the horse does instead. These "
                    + "particular ones are Java classes rather than gene files, because most of "
                    + "them do something no colour op can: swap the whole palette, trail "
                    + "particles, turn into something else at night."),
    MAGIC_BODY("Magical body-stat genes", false, null, "Magical body-stat genes",
            "Four loci that move speed, health, jump and size and paint nothing at all. They "
                    + "are the magical mirror of the performance genes, with far wider ranges "
                    + "and no real-world claim behind them."),

    // The seven data-driven magical families. Slug / title / lede are the
    // wiki's; GeneWikiTool reads them from here so the page it writes and the
    // menu the editors draw cannot drift apart.
    MAGIC_GROUND("Magic: ground and strong white", false,
            "genes-magic-ground", "Ground and strong white",
            "Genes that replace the base coat rather than mark it - a white horse with the "
                    + "colour breaking through, a coat confined to a handful of regions, a "
                    + "field split down the middle."),
    MAGIC_FIELDS("Magic: fields and regions", false,
            "genes-magic-fields", "Fields and regions",
            "Genes that divide the horse into areas - a blanket off the topline, a band round "
                    + "the neck, the underside against the back, a wash with no edge anywhere."),
    MAGIC_SPOTS("Magic: spots and rings", false,
            "genes-magic-spots", "Spots and rings",
            "Genes made of countable marks: spots, rosettes, annuli, crescents, chains of "
                    + "disks, and haloes with something dark inside them."),
    MAGIC_SPECKLE("Magic: speckle and dust", false,
            "genes-magic-speckle", "Speckle and dust",
            "Genes made of stipple - fields of fine points that drift into drifts and thin out "
                    + "rather than ending, and the markings built on top of them."),
    MAGIC_LINES("Magic: lines and strokes", false,
            "genes-magic-lines", "Lines and strokes",
            "Genes made of strokes: scratches, contour lines, riblines, brindle nets, "
                    + "filigree, and the two that are stripes but insist they are not."),
    MAGIC_HAIR("Magic: mane and tail", false,
            "genes-magic-hair", "Mane and tail",
            "Genes that touch only the hair - gradients down its length, bands across it, "
                    + "and the spectrum run root to tip."),
    MAGIC_MODIFIERS("Magic: colour modifiers", false,
            "genes-magic-modifiers", "Colour modifiers",
            "Genes with no shape of their own. Each reads what the horse already is and "
                    + "changes it - which is why they paint last, and why a plain horse shows "
                    + "some of them not at all.");

    private final String label;
    private final boolean natural;
    private final String slug;
    private final String title;
    private final String lede;

    GeneFamily(String label, boolean natural, String slug, String title, String lede) {
        this.label = label;
        this.natural = natural;
        this.slug = slug;
        this.title = title;
        this.lede = lede;
    }

    /** What the menus call it. */
    public String label() {
        return label;
    }

    /** Whether this family holds {@link Gene#isNatural() natural} genes. */
    public boolean natural() {
        return natural;
    }

    /** The wiki page's file stem, or {@code null} for a family with no index page. */
    public String slug() {
        return slug;
    }

    /** The family's heading, on its own page or over a section of one. */
    public String title() {
        return title;
    }

    /** The paragraph under that heading. */
    public String lede() {
        return lede;
    }

    // ------------------------------------------------------------------
    // Classification
    // ------------------------------------------------------------------

    /** Lowest priority in the band -> family. Magical genes only. */
    private static final TreeMap<Integer, GeneFamily> MAGICAL_BANDS = new TreeMap<>();

    /** Lowest priority in the band -> family. Natural genes that touch the coat. */
    private static final TreeMap<Integer, GeneFamily> NATURAL_BANDS = new TreeMap<>();

    /**
     * The naturals whose priority slot fights their family, and why.
     *
     * <p>Every one is a gene that paints <i>through</i> something else, or
     * paints in a band it does not belong to, or paints nothing at all without
     * being a disorder. There is a handful of them against fifty naturals;
     * listing them is honest, and bending the bands until they all fit would
     * move genes that are exactly where they should be.
     */
    private static final Map<String, GeneFamily> NATURAL_OVERRIDES = new LinkedHashMap<>();

    static {
        MAGICAL_BANDS.put(100, MAGIC_CORE);
        MAGICAL_BANDS.put(140, MAGIC_BODY);      // size, speed, health, jump
        MAGICAL_BANDS.put(150, MAGIC_CORE);
        MAGICAL_BANDS.put(200, MAGIC_GROUND);
        MAGICAL_BANDS.put(250, MAGIC_FIELDS);
        MAGICAL_BANDS.put(300, MAGIC_SPOTS);
        MAGICAL_BANDS.put(330, MAGIC_SPECKLE);
        MAGICAL_BANDS.put(360, MAGIC_LINES);
        MAGICAL_BANDS.put(400, MAGIC_HAIR);
        MAGICAL_BANDS.put(430, MAGIC_MODIFIERS);

        NATURAL_BANDS.put(0, NATURAL_OTHER);      // sex, diet
        NATURAL_BANDS.put(10, NATURAL_COAT);      // extension, agouti
        NATURAL_BANDS.put(30, NATURAL_DILUTION);  // silver .. grey
        NATURAL_BANDS.put(60, NATURAL_OTHER);     // tiger eye - the eyes and nothing else
        NATURAL_BANDS.put(65, NATURAL_WHITE);     // zebra striping .. PAX3, and the PATN modifiers

        // The sex locus paints nothing and is not a disorder. The editors keep
        // it off their lists entirely - the Sex button owns it - but it is a
        // registered gene and anything walking the registry will ask.
        NATURAL_OVERRIDES.put("horsegenetics.sex", NATURAL_OTHER);
        // Diet decides what a horse eats. It paints nothing, so the "invisible
        // is health" rule would file it under the disorders.
        NATURAL_OVERRIDES.put("horsegenetics.diet", NATURAL_OTHER);
        // Shade paints through agouti and sits at 95 for it - see the class note.
        NATURAL_OVERRIDES.put("horsegenetics.shade", NATURAL_COAT);
        // Countershading and brindle sit in the dilution band because they run
        // between the dilutions in the paint order. Neither dilutes anything:
        // sooty keeps black on the topline, pangare takes red off the
        // underside, brindle is a texture.
        NATURAL_OVERRIDES.put("horsegenetics.sooty", NATURAL_COAT);
        NATURAL_OVERRIDES.put("horsegenetics.pangare", NATURAL_COAT);
        NATURAL_OVERRIDES.put("horsegenetics.brindle", NATURAL_COAT);
    }

    /** The family a gene belongs to. Never null. */
    public static GeneFamily of(Gene gene) {
        if (!gene.isNatural()) {
            return ofMagicalPriority(gene.priority());
        }
        GeneFamily override = NATURAL_OVERRIDES.get(gene.key());
        if (override != null) {
            return override;
        }
        if (!Genes.influencesCoat(gene)) {
            return NATURAL_HEALTH;
        }
        Map.Entry<Integer, GeneFamily> e = NATURAL_BANDS.floorEntry(gene.priority());
        return e == null ? NATURAL_OTHER : e.getValue();
    }

    /**
     * The magical family a priority falls in. Public because
     * {@code GeneWikiTool} groups {@code SpecGene}s before they are registered
     * and so cannot ask {@link #of}.
     */
    public static GeneFamily ofMagicalPriority(int priority) {
        Map.Entry<Integer, GeneFamily> e = MAGICAL_BANDS.floorEntry(priority);
        return e == null ? MAGIC_CORE : e.getValue();
    }

    /**
     * The seven families the data-driven genes land in, in paint order - the
     * ones with a wiki page of their own.
     */
    public static List<GeneFamily> magicalSpecFamilies() {
        List<GeneFamily> out = new ArrayList<>();
        for (GeneFamily f : MAGICAL_BANDS.tailMap(200).values()) {
            out.add(f);
        }
        return out;
    }

    /**
     * Every family that currently holds at least one registered gene, in menu
     * order: the naturals as declared above, then the magicals in paint order.
     * Derived rather than listed, so a family whose genes were all removed stops
     * being offered instead of showing an empty list.
     */
    public static List<GeneFamily> occupied() {
        List<GeneFamily> out = new ArrayList<>();
        for (GeneFamily f : values()) {
            for (Gene g : Genes.codeOrder()) {
                if (of(g) == f) {
                    out.add(f);
                    break;
                }
            }
        }
        return out;
    }

    /** Every registered gene in this family, in {@link Genes#codeOrder() paint order}. */
    public List<Gene> members() {
        List<Gene> out = new ArrayList<>();
        for (Gene g : Genes.codeOrder()) {
            if (of(g) == this) {
                out.add(g);
            }
        }
        return out;
    }
}
