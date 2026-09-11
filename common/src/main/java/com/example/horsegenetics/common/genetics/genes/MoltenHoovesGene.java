package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.EpigeneticAbilityContribution;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.List;

/**
 * <b>Molten hooves</b> ({@code horsegenetics.molten_hooves}) - hoofprints left on
 * the ground behind the horse as it moves, fading as they cool.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code MltW/*}</td><td>{@code white} - glowing white prints; <b>dominant</b></td></tr>
 *   <tr><td>{@code MltB/MltB}</td><td>{@code black} - black prints that do not glow</td></tr>
 *   <tr><td>{@code MltC/MltC}</td><td>{@code colour} - glowing prints in one colour, the copy's</td></tr>
 *   <tr><td>{@code MltM/MltM}</td><td>{@code multicolour} - glowing prints running through both copies' colours</td></tr>
 *   <tr><td>any other mix</td><td>nothing - a carrier</td></tr>
 * </table>
 *
 * <h2>Four alleles, one dominant</h2>
 * The owner's design (2026-09-10), replacing a single dominant {@code Mlt} whose
 * copies each carried two colours. <b>White</b> is dominant over everything,
 * including the wild type, so it is the one a player can simply catch. The
 * other three are <b>recessive</b>, each expressing only as its own
 * homozygote: {@code MltB/MltC} shows nothing, which is what makes a black- or a
 * multicolour-hooved line something you have to breed for.
 *
 * <p><b>Colour lives on the allele copy</b> - {@code color} and {@code color2},
 * drawn per copy and drifted with it - but only the colour alleles read it.
 * White is white and black is black whatever the numbers say.
 *
 * <h2>Black does not glow</h2>
 * The one print the world lights like anything else: a scorch rather than an
 * ember. The emitter's spare {@code data} number carries it - {@code 1} glows,
 * {@code 0} does not - and the translator hands it to the print particle.
 *
 * <h2>It sets nothing on fire</h2>
 * The first specification placed real fire blocks; on a dominant locus that
 * burns down the stable, the forest and the horse.
 *
 * <h2>Paints nothing</h2>
 * Every outcome is a {@link Expression#wildType() wild type} as far as the coat
 * is concerned, so the locus is out of the texture key.
 */
public final class MoltenHoovesGene implements Gene, EpigeneticAbilityContribution {

    public static final String KEY = "horsegenetics.molten_hooves";

    /** Beside the other emission loci - particle at 150, rainbow dust after it. It paints nothing, so the number is a slot. */
    public static final int PRIORITY = 154;

    /**
     * The particle - the mod's own flat print, registered on the NeoForge side
     * ({@code particle/ModParticles}) and laid by stride rather than by chance
     * ({@code GeneAbilityHandler.layHoofprint}). No vanilla particle is
     * coloured, emissive and flat at once.
     */
    public static final String PARTICLE = "horsegenetics:hoofprint";

    /** Particles per firing, for the ordinary emitter path; the print path lays one per stride. */
    public static final int COUNT = 2;

    /** Probability per moving tick, for the ordinary emitter path; the print path is by distance. */
    public static final double EMIT_CHANCE = 0.15;

    /** The emitter's {@code data}: {@link #GLOWS} lights the print at full brightness, {@link #UNLIT} lets the world light it. */
    public static final double GLOWS = 1.0;
    public static final double UNLIT = 0.0;

    /** White prints, cooling to a pale blue-white. */
    public static final int WHITE = 0xFFFFFF;
    public static final int WHITE_COOL = 0xCCDDFF;
    /** Black prints - a scorch, not an ember. */
    public static final int BLACK = 0x161616;
    public static final int BLACK_COOL = 0x2C2A28;

    /** Wild founders showing white: one copy, and two. Dominant, so both are visible. */
    public static final double WILD_WHITE_PERCENT = 2.0;
    public static final double WILD_DOUBLE_WHITE_PERCENT = 0.2;
    /** Wild founders showing each recessive colour - as homozygotes only, never as invisible carriers. */
    public static final double WILD_BLACK_PERCENT = 0.3;
    public static final double WILD_COLOUR_PERCENT = 0.5;
    public static final double WILD_MULTICOLOUR_PERCENT = 0.2;

    private final Allele white = new Allele(KEY, 0, "MltW", "Molten white (MltW)");
    private final Allele black = new Allele(KEY, 1, "MltB", "Molten black (MltB)");
    private final Allele colour = new Allele(KEY, 2, "MltC", "Molten colour (MltC)");
    private final Allele multicolour = new Allele(KEY, 3, "MltM", "Molten multicolour (MltM)");
    private final Allele n = new Allele(KEY, 4, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(white, black, colour, multicolour, n);

    private final Expression wild = Expression.wildType(
            "The horse leaves the ground behind it exactly as it found it.");

    private final Expression carrier = Expression.wildType("carrier", "Carries molten hooves",
            "No prints. This horse carries a recessive molten allele - black, colour or "
                    + "multicolour - that only shows in a horse with two copies of the same one.");

    private final Expression whitePrints = Expression.wildType("white", "Molten hooves: white",
            "Glowing white hoofprints on the ground behind the horse, fading as they cool. White "
                    + "is dominant: one copy is all of it, whatever the other copy is. Nothing "
                    + "catches fire.");

    private final Expression blackPrints = Expression.wildType("black", "Molten hooves: black",
            "Black hoofprints that do not glow - a scorch rather than an ember. Two copies of the "
                    + "black allele; a single one shows nothing.");

    private final Expression colourPrints = Expression.wildType("colour", "Molten hooves: colour",
            "Glowing hoofprints in one colour, written on the allele and inherited with it, so a "
                    + "line breeds true to its own fire. Two copies of the colour allele.");

    private final Expression multicolourPrints = Expression.wildType("multicolour",
            "Molten hooves: multicolour",
            "Glowing hoofprints that run through several colours - each copy's two, one print "
                    + "after another - all of them written on the alleles. Two copies of the "
                    + "multicolour allele.");

    private final List<Expression> expressions = List.of(wild, carrier, whitePrints, blackPrints,
            colourPrints, multicolourPrints);

    private final FounderTable founders = FounderTable.builder()
            .weight(white, white, WILD_DOUBLE_WHITE_PERCENT)
            .weight(white, n, WILD_WHITE_PERCENT)
            .weight(black, black, WILD_BLACK_PERCENT)
            .weight(colour, colour, WILD_COLOUR_PERCENT)
            .weight(multicolour, multicolour, WILD_MULTICOLOUR_PERCENT)
            .weight(n, n, 100.0 - WILD_WHITE_PERCENT - WILD_DOUBLE_WHITE_PERCENT
                    - WILD_BLACK_PERCENT - WILD_COLOUR_PERCENT - WILD_MULTICOLOUR_PERCENT)
            .build();

    @Override public String key() { return KEY; }
    @Override public String name() { return "Molten hooves"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        if (pair.has(white)) {
            return whitePrints;
        }
        if (pair.homozygousFor(black)) {
            return blackPrints;
        }
        if (pair.homozygousFor(colour)) {
            return colourPrints;
        }
        if (pair.homozygousFor(multicolour)) {
            return multicolourPrints;
        }
        return pair.count(n) == 2 ? wild : carrier;
    }

    /**
     * One emitter for white, black and colour; <b>two</b> for multicolour, one
     * per copy, which the print path takes in turn - so the prints run through
     * copy one's two colours and copy two's. The translator alternates a
     * horse's emitters print by print.
     */
    @Override
    public List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype,
                                          GeneEpigenetics epigenetics) {
        if (pair.has(white)) {
            return List.of(emitter(WHITE, WHITE_COOL, GLOWS));
        }
        if (pair.homozygousFor(black)) {
            return List.of(emitter(BLACK, BLACK_COOL, UNLIT));
        }
        if (pair.homozygousFor(colour)) {
            int c = epigenetics.copy(0).rgb("color");
            return List.of(emitter(c, c, GLOWS));
        }
        if (pair.homozygousFor(multicolour)) {
            EpiValues a = epigenetics.copy(0);
            EpiValues b = epigenetics.copy(1);
            return List.of(emitter(a.rgb("color"), a.rgb("color2"), GLOWS),
                    emitter(b.rgb("color"), b.rgb("color2"), GLOWS));
        }
        return List.of();
    }

    /**
     * The two colours one copy carries. Only the colour and multicolour alleles
     * read them; stored on every copy so that a white horse's hidden recessive
     * copy still has its colours when a foal inherits it.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of()
                .and(EpiValue.colour("color", 0.60, 1.00, 0.62, 1.00))
                .and(EpiValue.colour("color2", 0.60, 1.00, 0.62, 1.00));
    }

    private GeneAbility.Emitter emitter(int from, int to, double light) {
        return new GeneAbility.Emitter("particle", "trail", "hooves",
                new GeneAbility.Trigger.OnMove(),
                from, to, COUNT, light,
                PARTICLE, EMIT_CHANCE, 0,
                GeneAbility.Condition.ALWAYS, 1);
    }
}
