package com.example.horsegenetics.common.genetics.spec;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;

import java.util.List;
import java.util.Map;

/**
 * A gene described as <b>data</b> rather than as a Java class - the format the
 * gene creator ({@code wiki/gene-creator/}) writes and {@link SpecGene}
 * executes.
 *
 * <p>This is the whole point of the data-driven path: a gene that fits the
 * shapes below needs no code at all. Drop the JSON in the genes folder, restart,
 * and the horse population carries it. A gene that <i>doesn't</i> fit still
 * writes a class against {@code Gene} - nothing here takes that away (see
 * {@code wiki/modding.html}).
 *
 * <h2>Shape</h2>
 * A spec is a header (key, alleles, priority) plus two tables:
 * <ul>
 *   <li><b>{@code expressions}</b> - one entry per distinct outcome, each
 *       naming the allele combinations that land on it, a human-readable
 *       description, and what it paints. A wild-type entry paints nothing.
 *       This is the data form of {@code genetics.Expression}, and it is why
 *       the format has no {@code dominance} field: which combinations share an
 *       outcome <i>is</i> the whole of what dominance used to say, and saying
 *       it directly works for any number of alleles.</li>
 *   <li><b>{@code founders}</b> - the share of wild horses carrying each
 *       combination, replacing the old per-allele {@code wildOdds}.</li>
 * </ul>
 *
 * <p>An expression paints with a list of {@link Layer}s. Each layer is
 * <b>where</b> ({@link Mask}s, folded into one coverage value per texel)
 * crossed with <b>what</b> ({@link Op} - a pigment move for a natural gene, a
 * colour move for a magical one). Coverage scales the effect, so every edge is
 * soft by construction rather than by each author remembering to fade it.
 *
 * <p>An expression may also carry an <b>{@code effects}</b> list -
 * {@link GeneAbility}s, the Minecraft-specific things a gene does beyond the
 * coat (walk on water, trail particles, be milked for a fluid). Those are inert
 * in {@code common/}; the NeoForge module executes them.
 *
 * <h2>Numbers that vary per horse</h2>
 * Any numeric parameter is a {@link Value}: a constant, a {@link Knob} the horse
 * draws once from its epigenetics, or a per-dose triple. Knobs are drawn in
 * declaration order from the expressing allele copy's seed, so a foal that
 * inherits the copy inherits the look - the determinism contract in
 * {@code wiki/philosophy.html} holds for spec genes exactly as it does for
 * hand-written ones.
 *
 * <p>Parsing (and every error message) lives in {@link GeneSpecParser}; the
 * painting lives in {@code coat.pattern.SpecPainter}.
 */
public record GeneSpec(
        String key,
        String name,
        boolean natural,
        int priority,
        List<AlleleSpec> alleles,
        List<Knob> knobs,
        List<ExpressionSpec> expressions,
        List<FounderWeight> founders) {

    /**
     * The current format version. <b>2</b> since the combination-table rewrite:
     * {@code dominance} and {@code wildOdds} are gone, {@code layers} and
     * {@code effects} moved inside an {@code expressions} entry, and
     * {@code founders} declares a weight per allele combination.
     */
    public static final int FORMAT = 2;

    /** The first-declared allele - the one {@code perDose} counts. */
    public AlleleSpec variant() {
        return alleles.get(0);
    }

    /** The last-declared allele - the population's baseline, by convention of the format. */
    public AlleleSpec baseline() {
        return alleles.get(alleles.size() - 1);
    }

    /** Does any value on this gene vary per horse? If not, one bake serves every carrier. */
    public boolean isDeterministic() {
        return knobs.isEmpty();
    }

    /**
     * Does this gene carry any Minecraft-specific effects (traversal flags,
     * emitters, yields, ...) on <i>any</i> of its expressions? The coat pipeline
     * never asks; the NeoForge translator does. See {@link GeneAbility}.
     */
    public boolean hasAbilities() {
        for (ExpressionSpec e : expressions) {
            if (!e.abilities().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** The expression declared under {@code id}, or {@code null}. */
    public ExpressionSpec expression(String id) {
        for (ExpressionSpec e : expressions) {
            if (e.id().equals(id)) {
                return e;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Expressions - one per distinct outcome
    // ------------------------------------------------------------------

    /**
     * One outcome the gene can produce, and which allele combinations land on
     * it - the data form of {@code genetics.Expression}.
     *
     * <p>{@code combinations} holds canonical {@code "<a>/<b>"} tokens; an
     * entry with an <b>empty</b> list is the gene's single <b>catch-all</b>,
     * taking every combination no other expression claimed. The parser checks
     * that the two together cover each of the {@code n(n+1)/2} combinations
     * exactly once, so an unreachable expression or an unclaimed combination is
     * a load error rather than a horse nobody can explain.
     *
     * <p>{@code wildType} means "changes nothing" - such an entry carries no
     * layers and no effects. {@code masks} means "hides every other gene".
     */
    public record ExpressionSpec(
            String id,
            String name,
            String description,
            boolean wildType,
            boolean masks,
            boolean deterministic,
            List<String> combinations,
            List<Layer> layers,
            List<GeneAbility> abilities) {

        /** Is this the catch-all that takes whatever no other expression claimed? */
        public boolean isCatchAll() {
            return combinations.isEmpty();
        }
    }

    /**
     * How common one allele combination is among founder horses, as a
     * percentage. {@code combination} is a canonical {@code "<a>/<b>"} token
     * pair; the weights are normalised to 100 by
     * {@code genetics.FounderTable}.
     */
    public record FounderWeight(String combination, double percent) {}

    // ------------------------------------------------------------------
    // Header
    // ------------------------------------------------------------------

    /**
     * One allele - a token and a label, and nothing else. Whether carrying it
     * shows, and whether that looks the same on every horse, are properties of
     * a <i>combination</i> and live on {@link ExpressionSpec}.
     */
    public record AlleleSpec(String token, String label) {}

    /**
     * A number this horse draws once, off the expressing allele copy.
     *
     * <p>A {@code seed} knob takes a {@code nextLong()} instead of a
     * {@code nextFloat()} and feeds the noise fields.
     *
     * <p>{@code perLeg} + {@code spread} is the shape {@code BayCoat} hand-rolls
     * and every leg-marking gene wants: one value drawn for the <b>horse</b>,
     * then each leg scaled by an independent {@code 1 ± spread}. A horse's four
     * socks come out near each other but never exactly level, which is what a
     * real one looks like - and it is one line of JSON instead of a loop.
     * {@code spread} 0 means all four legs share the horse's value exactly.
     *
     * <p>Draw order off the expressing copy: the base value, then one extra draw
     * per leg when {@code perLeg} is set.
     */
    public record Knob(String name, double min, double max, boolean perLeg, double spread, boolean seed) {

        public static Knob range(String name, double min, double max) {
            return new Knob(name, min, max, false, 0, false);
        }

        public static Knob perLeg(String name, double min, double max, double spread) {
            return new Knob(name, min, max, true, spread, false);
        }

        public static Knob seed(String name) {
            return new Knob(name, 0, 0, false, 0, true);
        }
    }

    // ------------------------------------------------------------------
    // Values
    // ------------------------------------------------------------------

    /** A number in a spec: fixed, drawn per horse, or chosen by allele dose. */
    public sealed interface Value {

        /** A literal. */
        record Const(double v) implements Value {}

        /** The value of {@link GeneSpec#knobs()} at {@code index} for this horse. */
        record FromKnob(int index) implements Value {}

        /**
         * One value per number of copies of the gene's <b>first-declared</b>
         * allele: {@code [0 copies, 1, 2]}. A convenience for scaling one
         * expression by dose; a genuinely different outcome should be its own
         * {@link ExpressionSpec} instead.
         */
        record PerDose(double zero, double one, double two) implements Value {}
    }

    // ------------------------------------------------------------------
    // Layers
    // ------------------------------------------------------------------

    /** Where (masks) crossed with what (an op). */
    public record Layer(String name, List<Mask> masks, Op op) {}

    /** How a mask term folds into the coverage the terms before it produced. */
    public enum Combine { MULTIPLY, MAX, MIN, ADD, SUBTRACT }

    /**
     * One term of a layer's region. Every mask returns a coverage in
     * {@code [0, 1]} per texel; the terms fold together by their
     * {@link Combine}, the first one folding into a starting coverage of 1.
     */
    public record Mask(MaskType type, Params params, Combine combine, boolean invert) {}

    public enum MaskType {
        /** Everything this skin maps. The default region. */
        ALL,
        /** Named {@link Part}s, or one of {@link PartGroups}' aliases. */
        PARTS,
        /** A soft band along body X / Y / Z, measured in a chosen space. */
        AXIS,
        /** A stripe down the centreline - the blaze shape. */
        CENTERLINE,
        /** {@code BodyStripes} - zebra bars, dun leg barring, brindle. */
        STRIPES,
        /** {@code BodyNoise.cellDistance} - the dapple / rosette field. */
        DAPPLES,
        /** Thresholded value noise - big irregular blobs (pinto, roan patches). */
        PATCHES,
        /** Smooth value noise as a soft shading field (sooty, countershading). */
        NOISE,
        /** Coverage read off the pigment the earlier genes left - "find the black". */
        PIGMENT
    }

    // ------------------------------------------------------------------
    // Ops
    // ------------------------------------------------------------------

    /** What the layer does where its masks say. */
    public record Op(OpType type, Params params) {}

    public enum OpType {
        /** Natural: {@code PigmentField.dilute} - the dilution move. */
        DILUTE,
        /** Natural: multiply red / black down by an amount. */
        RESTRICT,
        /** Natural: move red / black toward explicit levels (whiten, blacken, body colour). */
        SET_PIGMENT,
        /** Magical: add signed RGB (and opacity) - the zebra move. */
        TINT,
        /** Magical: walk what the texel <i>looks</i> like toward a colour - the pink-hair move. */
        TOWARD,
        /** Magical: flat opaque paint that replaces the accumulator. Masking genes only. */
        FLAT;

        public boolean isNatural() {
            return this == DILUTE || this == RESTRICT || this == SET_PIGMENT;
        }
    }

    // ------------------------------------------------------------------
    // Params
    // ------------------------------------------------------------------

    /**
     * A mask's or an op's parameters, already checked against that type's
     * declared parameter list by {@link GeneSpecParser} - so a typo is a load
     * error naming the offending key, not a setting that silently does nothing.
     */
    public record Params(Map<String, Object> raw) {

        public static final Params EMPTY = new Params(Map.of());

        public Value value(String name, double fallback) {
            Object o = raw.get(name);
            return o == null ? new Value.Const(fallback) : (Value) o;
        }

        public boolean has(String name) {
            return raw.containsKey(name);
        }

        @SuppressWarnings("unchecked")
        public List<Part> parts(String name) {
            Object o = raw.get(name);
            return o == null ? List.of() : (List<Part>) o;
        }

        public String text(String name, String fallback) {
            Object o = raw.get(name);
            return o == null ? fallback : (String) o;
        }

        public boolean flag(String name, boolean fallback) {
            Object o = raw.get(name);
            return o == null ? fallback : (Boolean) o;
        }

        /** A {@code "#rrggbb"} parameter, as 0xRRGGBB. */
        public int color(String name, int fallback) {
            Object o = raw.get(name);
            return o == null ? fallback : (Integer) o;
        }
    }
}
