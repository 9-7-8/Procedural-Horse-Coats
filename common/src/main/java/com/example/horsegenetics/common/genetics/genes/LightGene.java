package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.CoatOverlay;
import com.example.horsegenetics.common.coat.pattern.CoatOverlayContribution;
import com.example.horsegenetics.common.coat.pattern.CoatRegions;
import com.example.horsegenetics.common.coat.pattern.ColorField;
import com.example.horsegenetics.common.coat.pattern.ColorView;
import com.example.horsegenetics.common.coat.pattern.HairPattern;
import com.example.horsegenetics.common.coat.pattern.PigmentView;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.genetics.AbilityContribution;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <b>Light</b> ({@code horsegenetics.light}) - a <b>magical</b> gene, and the
 * model's first genuinely <b>codominant</b> locus with more than two variants:
 * a horse shows <i>everything</i> it carries.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>{@code Lthf/Lthf}, {@code Lthf/n}</td><td>{@code hooves} - gold, glowing hooves</td></tr>
 *   <tr><td>{@code Ltmn/Ltmn}, {@code Ltmn/n}</td><td>{@code mane} - a gold, glowing mane</td></tr>
 *   <tr><td>{@code Lteye/Lteye}, {@code Lteye/n}</td><td>{@code eyes} - gold, glowing eyes</td></tr>
 *   <tr><td>{@code Lthf/Ltmn}</td><td>{@code hooves-mane} - both</td></tr>
 *   <tr><td>{@code Lthf/Lteye}</td><td>{@code hooves-eyes} - both</td></tr>
 *   <tr><td>{@code Ltmn/Lteye}</td><td>{@code mane-eyes} - both</td></tr>
 * </table>
 *
 * <p>Ten combinations, seven outcomes, <b>no dominance order anywhere</b> - and
 * that is the clearest demonstration yet of why this model has no dominance
 * property. Three alleles that are each dominant to the wild type and to
 * <i>none of each other</i> cannot be described by ranking them; the table says
 * it in one line per row. The one thing the locus cannot make is all three at
 * once, for the honest reason that a horse has two copies of the chromosome and
 * not three.
 *
 * <p><b>Any variant copy at all lights the horse</b> - torch-strength
 * ({@value #LIGHT_LEVEL}), the same whether the glow is in the hooves, the mane,
 * the eyes or two of them. What the alleles decide is where it <i>shows</i>, not
 * how much of it there is: a horse with one gold ear-to-withers stripe of mane
 * lights a stable exactly as well as one with four gold hooves, because in both
 * cases the horse is the light source.
 *
 * <h2>Two halves, in two phases</h2>
 * The gold on the mane and hooves is ordinary phase-3 paint, walked toward the
 * colour so the strands and the hoof shading survive. The <b>eyes</b> cannot be:
 * {@link CoatRegions#redrawEyes} restores them from the template as the last act
 * of the bake, precisely so a wide white pattern can never blind a horse, so a
 * gene that wants to colour them has to run after that - which is what the
 * overlay phase is for. The emissive mask is written in the same pass, since
 * "this texel glows" has no channel in either accumulator.
 *
 * <p>Deterministic: one gold, no per-horse variation. The obvious extension is
 * an epigenetic colour, the way {@link HairColorGene} does it - at which point
 * the light block's colour becomes a question the game cannot answer, since
 * vanilla light has no hue.
 */
public final class LightGene implements Gene, CoatOverlayContribution, AbilityContribution {

    public static final String KEY = "horsegenetics.light";
    public static final int PRIORITY = 160;

    /** Torch-strength. A horse is a light source, not a beacon. */
    public static final int LIGHT_LEVEL = 14;

    /** The gold everything here is painted in - the same one Suntouched uses. */
    public static final int GOLD = 0xFFCF47;

    /** How far of the way to the gold, per region. Short of 100 so the shading survives. */
    private static final double MANE_STRENGTH = 0.90;
    private static final double HOOF_STRENGTH = 0.88;
    private static final double EYE_STRENGTH = 0.92;

    /** Fraction of a leg that is hoof. A touch more than {@code BayCoat.HOOF_FRACTION} so it reads. */
    public static final double HOOF_FRACTION = 0.16;

    public static final double WILD_HOOF_FREQUENCY = 0.005;
    public static final double WILD_MANE_FREQUENCY = 0.005;
    public static final double WILD_EYE_FREQUENCY = 0.005;

    /** Which part of the horse a variant copy lights up. */
    public enum Region { HOOVES, MANE, EYES }

    public final Allele Lthf = new Allele(KEY, 0, "Lthf", "Glowing hooves (Lthf)");
    public final Allele Ltmn = new Allele(KEY, 1, "Ltmn", "Glowing mane (Ltmn)");
    public final Allele Lteye = new Allele(KEY, 2, "Lteye", "Glowing eyes (Lteye)");
    public final Allele n = new Allele(KEY, 3, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Lthf, Ltmn, Lteye, n);

    private final Expression WILD = Expression.wildType("The horse gives off no light.");

    private final Expression HOOVES = glowing("hooves", "Glowing hooves",
            "All four hooves are gold and burn bright in the dark.", Region.HOOVES);

    private final Expression MANE = glowing("mane", "Glowing mane",
            "The mane is gold and burns bright in the dark. A foal has no mane yet, so it lights "
                    + "its surroundings without anything visibly glowing.", Region.MANE);

    private final Expression EYES = glowing("eyes", "Glowing eyes",
            "The eyes are gold and burn bright in the dark - by far the smallest of the three, "
                    + "and the one that carries furthest at night.", Region.EYES);

    private final Expression HOOVES_MANE = glowing("hooves-mane", "Glowing hooves and mane",
            "One copy of each. Both show, in full - the alleles do not compete.",
            Region.HOOVES, Region.MANE);

    private final Expression HOOVES_EYES = glowing("hooves-eyes", "Glowing hooves and eyes",
            "One copy of each. Both show, in full.", Region.HOOVES, Region.EYES);

    private final Expression MANE_EYES = glowing("mane-eyes", "Glowing mane and eyes",
            "One copy of each. Both show, in full.", Region.MANE, Region.EYES);

    private final List<Expression> expressions =
            List.of(WILD, HOOVES, MANE, EYES, HOOVES_MANE, HOOVES_EYES, MANE_EYES);

    private final Map<Expression, Set<Region>> regions = Map.of(
            HOOVES, EnumSet.of(Region.HOOVES),
            MANE, EnumSet.of(Region.MANE),
            EYES, EnumSet.of(Region.EYES),
            HOOVES_MANE, EnumSet.of(Region.HOOVES, Region.MANE),
            HOOVES_EYES, EnumSet.of(Region.HOOVES, Region.EYES),
            MANE_EYES, EnumSet.of(Region.MANE, Region.EYES));

    private final FounderTable founders = FounderTable.hardyWeinberg(frequencies(), pair -> true);

    /** Baseline last, and a {@link LinkedHashMap} - see {@link MilkGene#frequencies()}. */
    private Map<Allele, Double> frequencies() {
        Map<Allele, Double> p = new LinkedHashMap<>();
        p.put(Lthf, WILD_HOOF_FREQUENCY);
        p.put(Ltmn, WILD_MANE_FREQUENCY);
        p.put(Lteye, WILD_EYE_FREQUENCY);
        p.put(n, 1.0 - WILD_HOOF_FREQUENCY - WILD_MANE_FREQUENCY - WILD_EYE_FREQUENCY);
        return p;
    }

    private final List<GeneAbility> glow = List.of(
            new GeneAbility.Glow(LIGHT_LEVEL, List.of(), GeneAbility.Condition.ALWAYS, 1));

    @Override public String key() { return KEY; }
    @Override public String name() { return "Light"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        boolean hooves = pair.has(Lthf);
        boolean mane = pair.has(Ltmn);
        boolean eyes = pair.has(Lteye);
        if (hooves && mane) {
            return HOOVES_MANE;
        }
        if (hooves && eyes) {
            return HOOVES_EYES;
        }
        if (mane && eyes) {
            return MANE_EYES;
        }
        if (hooves) {
            return HOOVES;
        }
        if (mane) {
            return MANE;
        }
        return eyes ? EYES : WILD;
    }

    /** Which parts of the horse glow, for this combination. Empty for a wild type. */
    public Set<Region> regionsOf(AllelePair pair) {
        return regions.getOrDefault(expressionOf(pair), Set.of());
    }

    // ------------------------------------------------------------------
    // Phase 3 - the gold on the mane and the hooves
    // ------------------------------------------------------------------

    private Expression glowing(String id, String name, String description, Region... which) {
        Set<Region> set = EnumSet.noneOf(Region.class);
        set.addAll(List.of(which));
        return Expression.of(id, name)
                .describe(description + " The horse itself gives off light at level "
                        + LIGHT_LEVEL + ", the same as a torch.")
                .tint((ctx, coat, accumulated) -> paint(ctx, accumulated, set));
    }

    private static ColorField paint(CoatBuildContext ctx, ColorView accumulated, Set<Region> which) {
        boolean mane = which.contains(Region.MANE) && HorseSkinGeometry.hasPart(ctx.skin(), Part.MANE);
        boolean hooves = which.contains(Region.HOOVES);
        if (!mane && !hooves) {
            return null; // eyes only - nothing for this phase to do
        }
        ColorField delta = ColorField.deltaLike(accumulated);
        if (mane) {
            HairPattern.paint(ctx, accumulated, delta, Part.MANE,
                    (px, py, point) -> new HairPattern.Tone(GOLD, MANE_STRENGTH));
        }
        if (hooves) {
            for (Part leg : CoatRegions.LEGS) {
                paintHoof(ctx, accumulated, delta, leg);
            }
        }
        return delta;
    }

    private static void paintHoof(CoatBuildContext ctx, ColorView accumulated, ColorField delta, Part leg) {
        var bounds = HorseSkinGeometry.bounds(ctx.skin(), leg);
        double cutoff = bounds.yMin() + bounds.span(HorseSkinGeometry.Axis.Y) * HOOF_FRACTION;
        HorseSkinGeometry.forEachTexel(ctx.skin(), leg, (px, py, part, face, point) -> {
            if (point.y() > cutoff) {
                return;
            }
            delta.add(px, py,
                    toward(accumulated, px, py, 0, (GOLD >> 16) & 0xFF),
                    toward(accumulated, px, py, 1, (GOLD >> 8) & 0xFF),
                    toward(accumulated, px, py, 2, GOLD & 0xFF));
            delta.addOpacity(px, py, 255 - accumulated.opacity(px, py));
        });
    }

    private static int toward(ColorView colour, int px, int py, int channel, int target) {
        int seen = colour.visible(px, py, channel);
        int wanted = (int) Math.round(seen + (target - seen) * HOOF_STRENGTH);
        return wanted - switch (channel) {
            case 0 -> colour.red(px, py);
            case 1 -> colour.green(px, py);
            default -> colour.blue(px, py);
        };
    }

    // ------------------------------------------------------------------
    // Overlay - the eyes, and every emissive texel
    // ------------------------------------------------------------------

    @Override
    public void overlay(AllelePair pair, CoatBuildContext ctx, CoatOverlay out) {
        Set<Region> which = regionsOf(pair);
        if (which.contains(Region.EYES)) {
            out.shadeEyes(GOLD, EYE_STRENGTH);
            out.markEmissiveEyes();
        }
        if (which.contains(Region.MANE)) {
            out.markEmissivePart(Part.MANE);
        }
        if (which.contains(Region.HOOVES)) {
            for (Part leg : CoatRegions.LEGS) {
                out.markEmissiveLowerLeg(leg, HOOF_FRACTION);
            }
        }
    }

    // ------------------------------------------------------------------
    // Behaviour
    // ------------------------------------------------------------------

    /**
     * The world light, which is the same for every glowing combination. The
     * emissive <i>texels</i> are not declared here - they come out of the coat
     * bake, because which of them glow is a fact about pixels and the overlay
     * phase already knows it exactly.
     */
    @Override
    public List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype) {
        return regionsOf(pair).isEmpty() ? List.of() : glow;
    }
}
