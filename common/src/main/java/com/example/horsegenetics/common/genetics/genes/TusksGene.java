package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.GeneRarity;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.parts.AttachedPart;
import com.example.horsegenetics.common.parts.HornGenerator;
import com.example.horsegenetics.common.parts.NarwhalSize;

import java.util.List;
import java.util.Optional;

/**
 * <b>Tusks</b> ({@code horsegenetics.tusks}) - a <b>magical, dominant</b> locus with
 * one allele per mouth-and-muzzle growth. One gene, a form per allele, and the forms
 * combine (owner, tusks treatment): a horse with two different forms grows both.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>{@code Nar/n}, {@code Nar/Nar}</td><td>{@code narwhal} - one long spiral horn from the front of the muzzle</td></tr>
 * </table>
 *
 * <h2>Slice A: the narwhal horn only</h2>
 * The treatment's other two forms - {@code Tsk} boar tusks and {@code Sab} sabre fangs -
 * need generators of their own and are the next slice. They are <b>not declared
 * yet</b>, so no allele exists that grows nothing. When they come they are appended
 * after {@code n}: alleles are saved by token, and appending never changes the
 * canonical order of a pair that already exists ({@link AllelePair}), so no horse's
 * copies move to the other allele.
 *
 * <h2>Each form reads its own copy</h2>
 * Unlike every other part gene, which reads the {@link GeneEpigenetics#expressed()
 * expressed} copy, this one reads <b>the copy that carries the form</b>
 * ({@link #copyFor}). A horse with two different forms grows each from its own copy's
 * numbers - a {@code Sab/Nar} horse's fangs and horn are bred separately though they
 * share a locus. Two copies of one form grow it once, from the first copy (the
 * antlers' rule: symmetric, no second set). The copy that carries an allele is the
 * one in its slot: {@code Epigenome}'s alignment invariant, which {@code TusksGeneTest}
 * holds with the pair written both ways round.
 *
 * <h2>The numbers are generic</h2>
 * Every copy carries the same five numbers whatever its form, and each form reads the
 * ones it uses (treatment): {@code length} (a position on the form's own ladder),
 * {@code girth}, {@code curve} (a bend or lean, {@code [0,1]}, each form its own
 * angle), {@code style} and a {@code seed}. The narwhal horn uses the first four. The
 * seed is for the fangs' serration and the tusks' hook, and is carried from the start
 * so the schema does not change under a save when they arrive.
 *
 * <p>It does nothing but show: no stat, no ability, no hitbox. Not sex-limited - no
 * counterpart to the antlers' stallion-only allele - and not on a foal (a hard part).
 * Its colour is another locus, {@link TuskColourGene}, put on by {@code GrownParts.of}.
 * It paints nothing, so every outcome is a wild type and neither coat golden reads it.
 */
public final class TusksGene implements Gene {

    public static final String KEY = "horsegenetics.tusks";

    /**
     * Magical band. The part loci fill 227-246 and 247 is the only slot left beside
     * them, so this and its colour take the next free run of two; a {@code MAGIC_PARTS}
     * override files them with the parts whatever the band. Only a code-order slot.
     */
    public static final int PRIORITY = 334;

    /**
     * Founders, in percent, as {@code Nar/n} - about one horse in a thousand, under the
     * antlers' shares, because the narwhal horn is to be the rarest of the three forms
     * (treatment). Dominant, so every carrier shows: no invisible carriers in the wild.
     */
    public static final double WILD_NARWHAL_PERCENT = 0.1;

    /**
     * How many styles the {@code style} category holds. Six, so each form's own count
     * divides it - the narwhal's two twists now, the tusks' and fangs' three each next
     * - and a form reads {@code style % its count}: every style equally likely, and a
     * {@code Sab/Nar} horse's two forms independent of each other. Fixed now so it never
     * changes under a save.
     */
    public static final int STYLES = 6;

    /** How far the {@code curve} number lifts a narwhal horn above the head's own axis, in radians. */
    public static final double NARWHAL_LIFT = 0.45;

    public final Allele Nar = new Allele(KEY, 0, "Nar", "Narwhal horn (Nar)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Nar, n);

    private final Expression WILD = Expression.wildType("An ordinary horse.");

    private final Expression NARWHAL = Expression.wildType("narwhal", "Narwhal horn",
            "One long spiral horn growing straight forward out of the front of the muzzle, "
                    + "like a narwhal's tusk - on a mare or a stallion alike. Its length, "
                    + "thickness, twist and how far it lifts are epigenetic and inherited with "
                    + "the allele. It sits on the nose, not the forehead, so a unicorn can grow "
                    + "one too. Its colour is a gene of its own. Grown at maturity - a foal has "
                    + "none.");

    private final List<Expression> expressions = List.of(WILD, NARWHAL);

    private final FounderTable founders = FounderTable.builder()
            .weight(Nar, n, WILD_NARWHAL_PERCENT)
            .weight(n, 100.0 - WILD_NARWHAL_PERCENT)
            .build();

    /** Position on the form's own ladder - {@link NarwhalSize} for the narwhal horn. */
    public static final String LENGTH = "length";
    /** Cross-section multiplier. */
    public static final String GIRTH = "girth";
    /** A bend or lean, {@code [0,1]}: the narwhal horn's lift above the head's axis. */
    public static final String CURVE = "curve";
    /** Which style of its form, {@code style % count} - see {@link #STYLES}. */
    public static final String STYLE = "style";
    /** Variation for the forms that need it: a serration pattern, a hook. */
    public static final String SEED = "seed";

    @Override public String key() { return KEY; }
    @Override public String name() { return "Tusks"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public GeneRarity rarity() { return GeneRarity.RARE; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return pair.count(Nar) > 0 ? NARWHAL : WILD;
    }

    /** Whether this combination grows a narwhal horn - one copy is enough. */
    public boolean narwhal(AllelePair pair) {
        return pair.count(Nar) > 0;
    }

    /**
     * Length wild in the lower part of the ladder and bred toward the ceiling, as the
     * horn's; girth the antlers' range; the curve and the style anywhere.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(
                EpiValue.uniform(LENGTH, 0.0, 0.6).clampedTo(0.0, 1.0),
                EpiValue.uniform(GIRTH, 0.85, 1.15).clampedTo(0.7, 1.6),
                EpiValue.uniform(CURVE, 0.0, 1.0).clampedTo(0.0, 1.0),
                EpiValue.category(STYLE, STYLES),
                EpiValue.seed(SEED));
    }

    /**
     * The numbers on the copy that carries {@code form} - the first copy when both do.
     * The one place a form finds its copy; {@code form} must be in the pair.
     */
    EpiValues copyFor(Allele form, Genotype genotype, Epigenome epigenome) {
        AllelePair pair = genotype.pair(this);
        int slot = pair.first() == form ? 0 : 1;
        return GeneEpigenetics.forGene(this, genotype, epigenome).copy(slot);
    }

    /**
     * Every part this horse's tusks locus grows, white and dark, or an empty list.
     * Ask {@code GrownParts.of} for the coloured parts.
     */
    public List<AttachedPart> partsFor(Genotype genotype, Epigenome epigenome) {
        return narwhalFor(genotype, epigenome).map(List::of).orElse(List.of());
    }

    /** The narwhal horn, from the {@code Nar} copy's numbers - or empty without one. */
    public Optional<AttachedPart> narwhalFor(Genotype genotype, Epigenome epigenome) {
        if (!narwhal(genotype.pair(this))) {
            return Optional.empty();
        }
        EpiValues epi = copyFor(Nar, genotype, epigenome);
        return Optional.of(AttachedPart.narwhal(
                epi.get(LENGTH),
                epi.get(GIRTH),
                -NARWHAL_LIFT * clamp01(epi.get(CURVE)),
                epi.category(STYLE) % HornGenerator.NARWHAL_STYLES));
    }

    /** What to call this horse's narwhal horn - "a long narwhal horn". Empty for a horse that has none. */
    public Optional<NarwhalSize> narwhalSizeOf(Genotype genotype, Epigenome epigenome) {
        return narwhalFor(genotype, epigenome).map(part -> NarwhalSize.of(part.length()));
    }

    private static double clamp01(double v) {
        return v < 0.0 ? 0.0 : (v > 1.0 ? 1.0 : v);
    }
}
