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
 *   <li>{@code GeneWikiTool}, which writes one page per magical gene, groups
 *       the wiki sidebar and the landing page's gene cards by family, and takes
 *       every title and lede from here.</li>
 * </ul>
 *
 * <p>That last one is why every family carries a {@link #title} and a
 * {@link #lede}: the sidebar section, the landing-page heading and the
 * paragraph under it are all this table, so the wiki cannot group genes one way
 * and the two editors another.
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

    NATURAL_COAT("Natural coat genes", true, "Natural coat genes",
            "The pigment every other gene then works on: whether the horse can make black at "
                    + "all, where that black is allowed to go, and the countershading over the "
                    + "top of it."),
    NATURAL_DILUTION("Natural dilution genes", true, "Natural dilution genes",
            "Genes that lighten what is already there. Each takes red, or black, or both down "
                    + "by some amount - which is why the same dilution reads completely "
                    + "differently on a chestnut and on a black."),
    NATURAL_WHITE("Natural white genes", true, "Natural white genes",
            "Genes that take the coat away entirely in places. Only alleles at the same locus "
                    + "compete for a slot, which is why a horse can be tobiano and splashed and "
                    + "framed at once but never dominant white and sabino."),
    NATURAL_HEALTH("Natural health genes", true, "Natural health genes",
            "The loci you cannot see: speed, size, jump, and the disorders. Almost every one "
                    + "is recessive and absent from its own founder table as a homozygote - a "
                    + "wild horse can carry a disorder but never have one.", true),
    NATURAL_OTHER("Other natural genes", true, "Other natural genes",
            "The naturals that are neither pigment nor pattern nor disorder - what the horse "
                    + "will eat, and what colour its eyes are."),

    /** The hand-written magical genes that <b>paint</b>. */
    MAGIC_CORE("Magical coat genes", false, "Magical coat genes",
            "Where the magic starts. These are the hand-written genes that put colour on the "
                    + "horse - Java classes rather than gene files, because each does something "
                    + "no colour op can: swap the whole gradient chart, colour the hair on its "
                    + "own axis, draw an emblem over the finished texture, take the front third "
                    + "of the animal away. The data-driven markings are in the seven families "
                    + "below this one, sorted by what shape they are made of."),

    /** Non-painting: what the horse gives you, and what it leaves behind. */
    MAGIC_YIELD("Magical yield genes", false, "Yield and death",
            "Genes about what you get out of the horse rather than what it looks like - what "
                    + "fills a bucket, how often, how much meat it leaves, what it drops, and "
                    + "what happens to the ground it died on. None of them paints anything, and "
                    + "most of them cannot be seen on a living animal at all, which is what "
                    + "makes them worth a gene database."),

    /** Non-painting: how the horse acts. */
    MAGIC_BEHAVIOUR("Magical behaviour genes", false, "Behaviour",
            "Genes that change what the horse <i>does</i> - how mobs feel about it, what it "
                    + "becomes after dark, what it eats and what it turns into. The night loci "
                    + "are the reason this family exists: a horse that stands in the corner "
                    + "watching you is not a coat pattern and never belonged beside one."),

    /** Non-painting: what the horse gives off. */
    MAGIC_EMISSION("Magical emission genes", false, "Trails and emissions",
            "Genes that put something in the air around the horse rather than on it - the "
                    + "particle loci, whose colour, body site and density are all written on the "
                    + "allele copy and inherited with it."),
    MAGIC_BODY("Magical body-stat genes", false, "Magical body-stat genes",
            "Loci that move what a horse's body can do and paint nothing at all - the magical "
                    + "mirror of the performance genes, with far wider ranges and no real-world "
                    + "claim behind them. Four of them (speed, health, jump and size) resolve "
                    + "into the horse's stats and are the ones a breed pins from its stat bands; "
                    + "the rest reach the game as effects instead, because swimming and fighting "
                    + "mean nothing without a running game around them."),

    // The seven data-driven magical families. Title and lede are the wiki's;
    // GeneWikiTool reads them from here so the section it writes and the menu
    // the editors draw cannot drift apart.
    MAGIC_GROUND("Magic: ground and strong white", false, "Ground and strong white",
            "Genes that replace the base coat rather than mark it - a white horse with the "
                    + "colour breaking through, a coat confined to a handful of regions, a "
                    + "field split down the middle."),
    MAGIC_FIELDS("Magic: fields and regions", false, "Fields and regions",
            "Genes that divide the horse into areas - a blanket off the topline, a band round "
                    + "the neck, the underside against the back, a wash with no edge anywhere."),
    MAGIC_SPOTS("Magic: spots and rings", false, "Spots and rings",
            "Genes made of countable marks: spots, rosettes, annuli, crescents, chains of "
                    + "disks, and haloes with something dark inside them."),
    MAGIC_SPECKLE("Magic: speckle and dust", false, "Speckle and dust",
            "Genes made of stipple - fields of fine points that drift into drifts and thin out "
                    + "rather than ending, and the markings built on top of them."),
    MAGIC_LINES("Magic: lines and strokes", false, "Lines and strokes",
            "Genes made of strokes: scratches, contour lines, riblines, brindle nets, "
                    + "filigree, and the two that are stripes but insist they are not."),
    MAGIC_HAIR("Magic: mane and tail", false, "Mane and tail",
            "Genes that touch only the hair - gradients down its length, bands across it, "
                    + "and the spectrum run root to tip."),
    MAGIC_MODIFIERS("Magic: colour modifiers", false, "Colour modifiers",
            "Genes with no shape of their own. Each reads what the horse already is and "
                    + "changes it - which is why they paint last, and why a plain horse shows "
                    + "some of them not at all. The far end of the band is where the few genes "
                    + "that must have the LAST word live: a neon outline over everything, a "
                    + "head blacked out under whatever else was drawn on it.");

    private final String label;
    private final boolean natural;
    private final String title;
    private final String lede;
    private final boolean collapsed;

    GeneFamily(String label, boolean natural, String title, String lede) {
        this(label, natural, title, lede, false);
    }

    GeneFamily(String label, boolean natural, String title, String lede, boolean collapsed) {
        this.label = label;
        this.natural = natural;
        this.title = title;
        this.lede = lede;
        this.collapsed = collapsed;
    }

    /** What the menus call it. */
    public String label() {
        return label;
    }

    /** Whether this family holds {@link Gene#isNatural() natural} genes. */
    public boolean natural() {
        return natural;
    }

    /** The family's heading, on its own page or over a section of one. */
    public String title() {
        return title;
    }

    /** The paragraph under that heading. */
    public String lede() {
        return lede;
    }

    /**
     * Does the landing page open this family's section <b>shut</b>?
     *
     * <p>One family does. The health loci are the largest family in the mod and
     * the least browsable - forty-odd cards that all say the same thing, because
     * a disorder locus has nothing to show and its interest is entirely in the
     * sentence rather than the picture - and they sat between the coat genes and
     * the magic, which is what people come to the page for. Collapsed by
     * default, one click away. (Owner's call.)
     */
    public boolean collapsed() {
        return collapsed;
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

    /**
     * The magical genes that <b>do not paint</b>, and what each is actually
     * about.
     *
     * <p>This table exists because <b>priority is the wrong axis for these
     * genes and always was</b>. For a gene that paints, its priority is a real
     * fact with a real meaning - where in the stack it lands - and the bands
     * below were laid out as families precisely because the paint order and the
     * taxonomy want the same grouping. A gene that paints nothing has a priority
     * only because every gene needs a slot in the genotype code, and banding on
     * it grouped genes by an accident of when they were written.
     *
     * <p>The result was one family holding a hood marking, a milk bucket, a
     * spawn egg and a horse that stares at you through walls, which is not a
     * category anybody could use. So the non-painting magicals are named here
     * instead, by what they do - and the bands go back to meaning what they
     * were built to mean.
     *
     * <p>It is the same split the naturals already make: {@link #NATURAL_HEALTH}
     * is "the natural loci you cannot see", asked of {@link Genes#influencesCoat}.
     * This is that idea applied to the other half of the registry, with the
     * groups spelled out because "invisible" covers more ground here.
     */
    private static final Map<String, GeneFamily> MAGICAL_OVERRIDES = new LinkedHashMap<>();

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
        NATURAL_BANDS.put(60, NATURAL_OTHER);     // tiger eye, then the eight natural eye loci
        NATURAL_BANDS.put(69, NATURAL_WHITE);     // zebra striping .. PAX3, and the PATN modifiers

        // What you get out of the horse.
        MAGICAL_OVERRIDES.put("horsegenetics.milk", MAGIC_YIELD);
        MAGICAL_OVERRIDES.put("horsegenetics.magic_milk_volume", MAGIC_YIELD);
        MAGICAL_OVERRIDES.put("horsegenetics.magic_meat", MAGIC_YIELD);
        MAGICAL_OVERRIDES.put("horsegenetics.magic_item_drop", MAGIC_YIELD);
        MAGICAL_OVERRIDES.put("horsegenetics.magic_on_death", MAGIC_YIELD);

        // How it acts.
        MAGICAL_OVERRIDES.put("horsegenetics.magic_mob_aura", MAGIC_BEHAVIOUR);
        MAGICAL_OVERRIDES.put("horsegenetics.magic_night_temper", MAGIC_BEHAVIOUR);
        MAGICAL_OVERRIDES.put("horsegenetics.magic_night_watch", MAGIC_BEHAVIOUR);
        MAGICAL_OVERRIDES.put("horsegenetics.lycan", MAGIC_BEHAVIOUR);
        MAGICAL_OVERRIDES.put("horsegenetics.verdant", MAGIC_BEHAVIOUR);

        // What it gives off.
        MAGICAL_OVERRIDES.put("horsegenetics.particle", MAGIC_EMISSION);
        MAGICAL_OVERRIDES.put("horsegenetics.rainbow_dust", MAGIC_EMISSION);
        MAGICAL_OVERRIDES.put("horsegenetics.molten_hooves", MAGIC_EMISSION);

        // The behaviour family. Priority is meaningless for a gene that paints
        // nothing, so every one of these is named rather than banded.
        MAGICAL_OVERRIDES.put("horsegenetics.fireproof", MAGIC_BEHAVIOUR);
        MAGICAL_OVERRIDES.put("horsegenetics.bird_boned", MAGIC_BEHAVIOUR);
        MAGICAL_OVERRIDES.put("horsegenetics.ocean_born", MAGIC_BEHAVIOUR);
        MAGICAL_OVERRIDES.put("horsegenetics.hydrophobic", MAGIC_BEHAVIOUR);
        MAGICAL_OVERRIDES.put("horsegenetics.hot_blooded", MAGIC_BEHAVIOUR);
        MAGICAL_OVERRIDES.put("horsegenetics.dryad", MAGIC_BEHAVIOUR);
        MAGICAL_OVERRIDES.put("horsegenetics.intimidating", MAGIC_BEHAVIOUR);
        MAGICAL_OVERRIDES.put("horsegenetics.meowing", MAGIC_BEHAVIOUR);
        MAGICAL_OVERRIDES.put("horsegenetics.cleansing_light", MAGIC_BEHAVIOUR);
        MAGICAL_OVERRIDES.put("horsegenetics.holy_ward", MAGIC_BEHAVIOUR);
        MAGICAL_OVERRIDES.put("horsegenetics.echolocate", MAGIC_BEHAVIOUR);
        MAGICAL_OVERRIDES.put("horsegenetics.base_alarm", MAGIC_BEHAVIOUR);
        MAGICAL_OVERRIDES.put("horsegenetics.music_enjoyer", MAGIC_BEHAVIOUR);
        MAGICAL_OVERRIDES.put("horsegenetics.gladiator", MAGIC_BEHAVIOUR);
        MAGICAL_OVERRIDES.put("horsegenetics.guardian", MAGIC_BEHAVIOUR);
        MAGICAL_OVERRIDES.put("horsegenetics.ender_echo", MAGIC_BEHAVIOUR);
        MAGICAL_OVERRIDES.put("horsegenetics.spontaneous_breeding", MAGIC_BEHAVIOUR);
        MAGICAL_OVERRIDES.put("horsegenetics.eyesight", MAGIC_BODY);
        MAGICAL_OVERRIDES.put("horsegenetics.weather_speed", MAGIC_BODY);
        MAGICAL_OVERRIDES.put("horsegenetics.weather_jump", MAGIC_BODY);
        MAGICAL_OVERRIDES.put("horsegenetics.food_preference", MAGIC_BEHAVIOUR);
        MAGICAL_OVERRIDES.put("horsegenetics.potion_milk", MAGIC_YIELD);
        MAGICAL_OVERRIDES.put("horsegenetics.egg_layer", MAGIC_YIELD);
        MAGICAL_OVERRIDES.put("horsegenetics.singer", MAGIC_EMISSION);
        MAGICAL_OVERRIDES.put("horsegenetics.pack_leader", MAGIC_BEHAVIOUR);
        MAGICAL_OVERRIDES.put("horsegenetics.spawner", MAGIC_BEHAVIOUR);

        // Cutie mark paints nothing in phase 3 - it draws its emblem in the
        // overlay pass, over the finished texture - but it is unambiguously a
        // marking and belongs with the genes that put colour on a horse.
        MAGICAL_OVERRIDES.put("horsegenetics.cutie_mark", MAGIC_CORE);

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
            GeneFamily named = MAGICAL_OVERRIDES.get(gene.key());
            if (named != null) {
                return named;
            }
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
     * and so cannot ask {@link #of}. A data-driven gene always paints, so the
     * bands are the whole answer for one.
     */
    public static GeneFamily ofMagicalPriority(int priority) {
        Map.Entry<Integer, GeneFamily> e = MAGICAL_BANDS.floorEntry(priority);
        return e == null ? MAGIC_CORE : e.getValue();
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
