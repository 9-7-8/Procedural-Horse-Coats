package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.CoatOverlay;
import com.example.horsegenetics.common.coat.pattern.CoatOverlayContribution;
import com.example.horsegenetics.common.coat.pattern.ColorField;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.Diet;
import com.example.horsegenetics.common.genetics.DietContribution;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.EyeColor;
import com.example.horsegenetics.common.genetics.EyeColorContribution;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneRarity;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.HorseDiet;
import com.example.horsegenetics.common.trait.TraitBuilder;
import com.example.horsegenetics.common.trait.TraitContribution;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.EyeSpread;

import java.util.List;
import java.util.Optional;

/**
 * <b>Dhampir</b> ({@code horsegenetics.dhampir}) - a magical recessive, and the
 * most thoroughly <i>changed</i> horse in the mod: a white, red-eyed, sun-shy
 * animal that is three times as hard to kill as an ordinary horse and can only
 * be healed by biting something.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>{@code Dhmp/n}</td><td>{@code dhampir-trace} - red eyes and glowing scleras, and <b>nothing else</b></td></tr>
 *   <tr><td>{@code Dhmp/Dhmp}</td><td>{@code dhampir} - the whole animal</td></tr>
 * </table>
 *
 * <h2>The heterozygote is the point of the gene</h2>
 * A carrier is not silent - it has the eyes - which is unusual here and
 * deliberate: it makes the locus <b>visible before you breed it</b>. Two
 * red-eyed horses in a field are a promise, not a lottery ticket, and that is
 * the difference between a recessive worth hunting and one nobody would ever
 * find. It costs the carrier nothing: no sun damage, no stats, ordinary
 * appetite.
 *
 * <h2>What the homozygote is</h2>
 * <ul>
 *   <li><b>Pure white</b>, painted absolutely in phase 3 over whatever the
 *       melanin genes made - and the coat underneath is still in the genome for
 *       a foal to inherit. It is painted <b>early</b> in phase 3 (see
 *       {@link #PRIORITY}), so it is a backdrop rather than a mask: every
 *       marking gene above it still draws on top.</li>
 *   <li><b>Red eyes and glowing scleras</b>, at
 *       {@link EyeColor#RANK_MAGICAL} - above even a depigmented blue, because
 *       this is paint rather than pigment.</li>
 *   <li><b>Three times the health, half again the speed, twice the jump.</b>
 *       Unclamped multipliers, so it is genuinely a different animal and not a
 *       nudge inside the ordinary band.</li>
 *   <li><b>{@link Diet#NOTHING}</b> - it cannot be fed, at all, by anything.
 *       That is the whole cost of the three-times health, and it is why the
 *       diet locus resolves the <i>last</i> claim in
 *       {@link com.example.horsegenetics.common.genetics.Genes#codeOrder()}:
 *       this gene sorts after the diet locus and overrides it with no special
 *       case anywhere.</li>
 * </ul>
 *
 * <h2>What is not in this file</h2>
 * Sunlight burning it, fleeing to shade or water, and hunting a mob to heal are
 * all <b>behaviour</b>, and behaviour lives in the game module - here in
 * {@code neoforge/server/DhampirHandler} and its two goals, the same way the
 * herd, aggro and lethal-foal behaviours do. They are deliberately not
 * {@code effects} verbs: each would be a verb with exactly one user, and the
 * {@code effects} vocabulary is meant to be things a <i>data-driven</i> gene
 * would reach for. See {@code wiki/gene-dhampir.html}.
 *
 * <p>Magical, so it paints in phase 3 and never touches the pigment field.
 * Rare: no wild founder is ever homozygous - a dhampir is something you breed.
 */
public final class DhampirGene implements Gene, TraitContribution, EyeColorContribution,
        CoatOverlayContribution, DietContribution {

    public static final String KEY = "horsegenetics.dhampir";

    /**
     * <b>Near the bottom of the magical band</b>, deliberately. A dhampir paints
     * the whole horse absolute white, so wherever it sits in the order is where
     * the horse's markings stop being visible - and at 135 that was every
     * data-driven gene in the mod. Moved down here so a dhampir is a
     * <i>backdrop</i> rather than a dead end: breed one carrying anything else
     * that paints and the marking still lands on top of the white.
     * (Owner's call - "so that you aren't stuck with a pure white horse if you
     * want a Dhampir horse".) Below it sit only {@code suit} and {@code hood},
     * which are lower still for the same reason.
     */
    public static final int PRIORITY = 105;

    /** Multipliers on the resolved body. Unclamped - a dhampir is off the ordinary scale. */
    public static final double HEALTH_MULTIPLIER = 3.0;
    public static final double SPEED_MULTIPLIER = 1.5;
    public static final double JUMP_MULTIPLIER = 2.0;

    /** How many founders in a hundred carry exactly one copy. None carry two. */
    private static final double CARRIER_PERCENT = 3.0;

    /**
     * The iris. Deep and slightly dark rather than a signal red: at two texels a
     * pure {@code #FF0000} reads as a bright dot rather than as an eye, the same
     * argument that keeps {@link EyeColor#BLUE} desaturated.
     */
    private static final EyeColor RED = new EyeColor("dhampir-red", "Red",
            0xB4131B, EyeColor.RANK_MAGICAL, 1.0);

    public final Allele Dhmp = new Allele(KEY, 0, "Dhmp", "Dhampir (Dhmp)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Dhmp, n);

    private final Expression WILD = Expression.wildType(
            "An ordinary horse. Whatever the rest of the genome made it, it stays.");

    /**
     * The carrier. It has no painter in either phase - the eyes and the glow are
     * both {@link CoatOverlay} work - so it is a
     * {@link Expression.Builder#marker()}, not a wild type: it genuinely changes
     * the horse, and a red-eyed bay must not share a baked texture with a plain
     * one.
     */
    private final Expression TRACE = Expression.of("dhampir-trace", "Dhampir trace")
            .describe("Red eyes with faintly glowing whites, and nothing else at all - the coat, "
                    + "the appetite and the body are an ordinary horse's. Two of these bred "
                    + "together are the only way a dhampir appears.")
            .marker();

    private final Expression DHAMPIR = Expression.of("dhampir", "Dhampir")
            .describe("White from nose to tail, red-eyed, and burned by daylight - it runs for "
                    + "shade or water and will jump a fence to get there. Three times the health "
                    + "of an ordinary horse, half again the speed and twice the jump, and it "
                    + "cannot be fed by any means: the only way it heals is to hunt something "
                    + "living and take a bite. The white is a backdrop rather than a verdict: "
                    + "it is painted near the bottom of the magical order, so anything else "
                    + "the horse carries still draws on top of it.")
            .tint((ctx, coat, colour) -> {
                ColorField delta = ColorField.deltaLike(colour);
                // Absolute, not additive: a dhampir is white over a black horse,
                // a cremello or a leopard alike, and phase 3's accumulator would
                // otherwise let a dark coat show through from underneath.
                HorseSkinGeometry.forEachTexel(ctx.skin(),
                        (px, py, part, face, point) -> delta.set(px, py, 255, 255, 255, 255));
                return delta;
            });

    private final List<Expression> expressions = List.of(WILD, TRACE, DHAMPIR);

    /**
     * Carriers only. A wild-caught horse is an adult that survived, and a
     * dhampir is an animal that burns in daylight and starves unless it hunts -
     * the population would not hold one. The same "founders never carry two"
     * rule the health loci use, for a different reason.
     */
    private final FounderTable founders = FounderTable.builder()
            .weight(Dhmp, n, CARRIER_PERCENT)
            .weight(n, n, 100.0 - CARRIER_PERCENT)
            .build();

    @Override public String key() { return KEY; }
    @Override public String name() { return "Dhampir"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public GeneRarity rarity() { return GeneRarity.LEGENDARY; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }


    /**
     * Not for the random splice. {@link com.example.horsegenetics.common.genetics.SpliceSafety}
     * derives its blacklist from what a combination does to the <i>body</i>, and
     * this one <i>raises</i> every stat it touches - so nothing there would stop
     * it handing an unborn foal a horse that cannot be fed and burns in the sun.
     * This is exactly the "harm resolution cannot see" the manual override
     * exists for.
     */
    @Override public boolean spliceable() { return false; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        int copies = pair.count(Dhmp);
        if (copies == 2) {
            return DHAMPIR;
        }
        return copies == 1 ? TRACE : WILD;
    }

    /** Is this horse the full animal - white, sunburnt, unfeedable? */
    public boolean isDhampir(AllelePair pair) {
        return pair != null && pair.count(Dhmp) == 2;
    }

    /** Does this horse show the eyes at all - a carrier or the animal itself? */
    public boolean showsEyes(AllelePair pair) {
        return pair != null && pair.has(Dhmp);
    }

    // --- the four channels ------------------------------------------------

    @Override
    public void contribute(AllelePair pair, Genotype genotype, TraitBuilder out) {
        if (!isDhampir(pair)) {
            return;     // a carrier's body is an ordinary horse's
        }
        out.multiplyHealthUnclamped(HEALTH_MULTIPLIER)
                .multiplySpeedUnclamped(SPEED_MULTIPLIER)
                .multiplyJumpUnclamped(JUMP_MULTIPLIER);
    }

    @Override
    public Optional<EyeColor> eyeColor(AllelePair pair, Genotype genotype, Epigenome epigenome,
                                       double whiteCoverage) {
        return showsEyes(pair) ? Optional.of(RED) : Optional.empty();
    }

    /**
     * The face. Two things, and the first of them is a <b>re-assertion</b>.
     *
     * <p>The eye channel has already run and put the red in - but the overlay
     * phase comes after it and is explicitly allowed to have the last word over
     * the iris, which is right for the genes it was built for (light's glowing
     * eye, the leopard complex's white sclera rim) and wrong here: a dhampir
     * that also carried the leopard complex came out with the rim's ordinary
     * black eye. This gene sorts after every gene that paints, so painting the
     * iris again here is the enforcement of a claim the eye channel makes; the
     * {@link EyeColorContribution} is still the declaration, and it is what the
     * info panel reads.
     *
     * <p>Then the whites, and only the whites. A glowing iris would read as a
     * lamp; a glowing sclera with a hole in it reads as a dhampir.
     */
    @Override
    public void overlay(AllelePair pair, CoatBuildContext ctx, CoatOverlay out) {
        out.tintIris(RED.rgb(), RED.strength());
        out.markEmissiveSclera();
    }

    @Override
    public Optional<HorseDiet> diet(AllelePair pair, Genotype genotype, GeneEpigenetics random) {
        return isDhampir(pair) ? Optional.of(HorseDiet.of(Diet.NOTHING)) : Optional.empty();
    }
    /**
     * Only the eye spread. Everything else this gene does is fixed by its
     * alleles.
     */
    @Override
    public EpiSchema epiSchema() {
        return EyeSpread.schema();
    }

}
