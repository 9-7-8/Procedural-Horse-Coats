package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.parts.AttachedPart;
import com.example.horsegenetics.common.parts.HornGenerator;
import com.example.horsegenetics.common.parts.HornSize;

import java.util.List;
import java.util.Optional;

/**
 * <b>Unicorn horn</b> ({@code horsegenetics.unicorn_horn}) - a <b>magical</b>,
 * <b>recessive</b> gene, and the <b>first gene in the mod that grows a piece of
 * geometry rather than painting a texel</b>. A horse with two copies wears a
 * single tapering horn on its forehead.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>{@code Horn/n}</td><td>{@code unicorn-carrier} - a wild type; nothing shows</td></tr>
 *   <tr><td>{@code Horn/Horn}</td><td>{@code unicorn} - the horn</td></tr>
 * </table>
 *
 * <h2>It paints nothing, and that is the whole novelty</h2>
 * All three outcomes are {@link Expression#wildType() wild types}: the coat
 * <i>texture</i> is untouched, so two horses that differ only here share one baked
 * coat and neither coat golden moves. What shows instead is a mesh, hung on the
 * animated head by {@code AttachedPartLayer} - so the horn follows a head-toss, a
 * graze and a rear for nothing, because it is parented to a bone that already
 * moves. The priority ({@value #PRIORITY}) is therefore only a code-order slot,
 * the same way {@link CutieMarkGene}'s is.
 *
 * <h2>Every horn is its own horn</h2>
 * The locus grants a horn; the <b>epigenome</b> decides what kind, and each number
 * is inherited with the allele copy and drifts a little at every breeding. That is
 * what makes this a gene rather than a switch, and it is the same premise the coat
 * engine runs on applied to geometry: the space of horns is not a list.
 *
 * <p>The numbers are sorted by <b>what it costs to change them</b>, which is the
 * one design decision here worth reading twice:
 * <ul>
 *   <li><b>{@code length}</b> and <b>{@code twist}</b> change the mesh, so they
 *       quantise into a {@code PartShape} - sixteen length steps and four twists,
 *       sixty-four meshes for the whole game however many unicorns exist. Length
 *       <i>also</i> feeds the free axis below, which closes the seam between steps
 *       so nothing pops as a line's horns lengthen over generations.</li>
 *   <li><b>{@code girth}</b> and <b>{@code tilt}</b> are a transform on the finished
 *       mesh: continuous, free, and shared meshes stay shared.</li>
 *   <li><b>{@code tint}</b> and <b>{@code glow}</b> are arguments to the draw. One
 *       greyscale sheet therefore covers pearl, ivory, bone and gold, and a
 *       lineage's horn colour can wander over thirty generations without any new
 *       art - the same trick a dyed braid uses.</li>
 * </ul>
 *
 * <p>{@code glow} is a <b>category</b> rather than a magnitude on purpose. Drift
 * moves a magnitude a hair at every breeding and re-rolls a category only rarely,
 * so a glowing horn stays glowing down a line and is a real event when it appears,
 * instead of a horse fading imperceptibly into luminescence over ten foals.
 */
public final class UnicornHornGene implements Gene {

    public static final String KEY = "horsegenetics.unicorn_horn";

    /**
     * Magical band, immediately after {@link CutieMarkGene}'s 196 - the two are
     * neighbours because they are the two loci whose whole effect happens after
     * the coat texture is finished. It is only a code-order slot: the layer draws
     * where the renderer puts it regardless.
     */
    public static final int PRIORITY = 197;

    /**
     * Per allele. Recessive, so roughly the square of this is homozygous: about
     * one wild horse in four hundred is a unicorn, and one in twenty carries it.
     * Rare enough to be a find, common enough that a breeder who wants one can
     * get there by crossing carriers rather than by waiting for a miracle.
     */
    public static final double WILD_HORN_FREQUENCY = 0.05;

    public final Allele Horn = new Allele(KEY, 0, "Horn", "Unicorn horn (Horn)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Horn, n);

    private final Expression WILD = Expression.wildType("An ordinary horse.");

    private final Expression CARRIER = Expression.wildType("unicorn-carrier", "Unicorn carrier",
            "One copy, which shows nothing. Two carriers bred together are the only reliable way "
                    + "a horn appears (or, rarely, a wild horse born with two).");

    private final Expression UNICORN = Expression.wildType("unicorn", "Unicorn",
            "A single tapering horn on the forehead, growing out of the skull between the ears "
                    + "and leaning forward. Its length, thickness, twist, lean and colour are all "
                    + "epigenetic and inherited with the allele - so a horn you like breeds true, "
                    + "and a line selected for length keeps getting longer. It is a piece of the "
                    + "horse rather than a marking on it: nothing about the coat changes, and the "
                    + "horn follows the head when the horse grazes.");

    private final List<Expression> expressions = List.of(WILD, CARRIER, UNICORN);

    private final FounderTable founders = FounderTable.hardyWeinberg(Horn, n, WILD_HORN_FREQUENCY);

    // ------------------------------------------------------------------
    // The epigenetic numbers. Every one of these is read by hornFor and honoured
    // by AttachedPartLayer - see AttachedPart on why that is a rule.
    // ------------------------------------------------------------------

    /** Position on the {@link HornSize} ladder: 0 is a nub, 1 a narwhal tusk. */
    public static final String LENGTH = "length";
    /** Cross-section multiplier - a needle of a horn, or a stout one. */
    public static final String GIRTH = "girth";
    /** Which of {@link HornGenerator}'s four shapes. */
    public static final String TWIST = "twist";
    /** Radians of forward lean off the skull. */
    public static final String TILT = "tilt";
    /** Draws the horn full-bright when it lands on zero. */
    public static final String GLOW = "glow";
    /** Prefix of the three colour channels. */
    public static final String TINT = "tint";

    /**
     * How many outcomes {@link #GLOW} has, of which exactly one glows. Eight, so
     * about an eighth of unicorns are luminous - rare enough to be worth breeding
     * for and common enough to have been seen.
     */
    public static final int GLOW_OUTCOMES = 8;

    @Override public String key() { return KEY; }
    @Override public String name() { return "Unicorn horn"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return switch (pair.count(Horn)) {
            case 2 -> UNICORN;
            case 1 -> CARRIER;
            default -> WILD;
        };
    }

    /** Whether this combination actually grows a horn. */
    public boolean shows(AllelePair pair) {
        return pair.count(Horn) == 2;
    }

    /**
     * <b>The design ranges, and why the tilt can be negative.</b>
     *
     * <p>{@code girth} bottoms out at 0.7 rather than lower because the generator
     * already refuses to emit a box under 0.6 model units; scaling a horn thinner
     * than that on top of it would give a part that shimmers as the camera moves
     * and no gene should be able to ask for one.
     *
     * <p>{@code tilt} runs a little past zero into the negative so a few horses
     * carry a horn raked <i>back</i> over the poll rather than forward over the
     * face. It reads as a different animal rather than as a mistake, and it costs
     * nothing: the tilt is one rotation applied to a shared mesh.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(
                        EpiValue.uniform(LENGTH, 0.0, 1.0).clampedTo(0.0, 1.0),
                        EpiValue.uniform(GIRTH, 0.75, 1.30).clampedTo(0.7, 1.6),
                        EpiValue.category(TWIST, HornGenerator.STYLES),
                        EpiValue.uniform(TILT, -0.10, 0.50).clampedTo(-0.6, 1.0),
                        EpiValue.category(GLOW, GLOW_OUTCOMES))
                // Pale and bright rather than saturated: a horn is keratin, so the
                // colour range is ivory through pearl to a warm gold, and the
                // brightest thing about it should be that it is nearly white.
                .and(EpiValue.colour(TINT, 0.04, 0.30, 0.82, 1.00));
    }

    /**
     * The horn this horse grows, or empty if it is not {@code Horn/Horn}.
     *
     * <p>Deterministic and heritable: every number comes off the expressing copy's
     * epigenetic values, so the same horse grows the same horn in every session and
     * a foal that inherits the copy inherits the horn.
     */
    public Optional<AttachedPart> hornFor(Genotype genotype, Epigenome epigenome) {
        if (!shows(genotype.pair(this))) {
            return Optional.empty();
        }
        EpiValues epi = GeneEpigenetics.forGene(this, genotype, epigenome).expressed();
        return Optional.of(AttachedPart.horn(
                epi.get(LENGTH),
                epi.get(GIRTH),
                epi.get(TILT),
                epi.category(TWIST),
                0xFF000000 | epi.rgb(TINT),
                epi.category(GLOW) == 0));
    }

    /**
     * What to call this horse's horn on the horse screen - "a standard horn",
     * "a narwhal horn". Empty for a horse that has none.
     */
    public Optional<HornSize> sizeOf(Genotype genotype, Epigenome epigenome) {
        return hornFor(genotype, epigenome).map(part -> HornSize.of(part.length()));
    }
}
