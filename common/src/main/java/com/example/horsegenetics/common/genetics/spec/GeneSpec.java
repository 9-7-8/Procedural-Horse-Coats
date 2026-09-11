package com.example.horsegenetics.common.genetics.spec;

import com.example.horsegenetics.common.CommonMaps;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.genetics.GeneRarity;

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
 * {@code wiki/making-a-gene.html}).
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
        List<FounderWeight> founders,
        String blurb,
        GeneRarity rarity,
        Carrot carrot,
        List<FounderWeight> splice,
        Preview preview,
        List<String> notes) {

    /**
     * The current format version. <b>3</b> adds the gameplay-economy metadata
     * (roadmap wiki &sect;19): an optional {@code blurb} (a gene-level summary),
     * {@code rarity} (a tier enum), a {@code carrot} block (opt-out + behaviour
     * + flavour ingredients) and an optional {@code splice} table (what the
     * Unknown Gene Splice carrot rolls on this gene). All four are optional - a
     * format-2 file that only bumps its version number still loads.
     *
     * <p><b>2</b> was the combination-table rewrite: {@code dominance} and
     * {@code wildOdds} gone, {@code layers} and {@code effects} moved inside an
     * {@code expressions} entry, {@code founders} a weight per combination.
     */
    public static final int FORMAT = 3;

    /**
     * The gene-carrot block (roadmap &sect;14.2). {@code enabled} is the
     * opt-out flag surfaced as {@link com.example.horsegenetics.common.genetics.Gene#hasGeneCarrot()};
     * {@code homozygous} chooses whether feeding the carrot makes the game treat
     * the parent as {@code <Gene><Gene>} rather than {@code n<Gene>} for that
     * gamete; {@code flavour} is the extra recipe ingredients (item ids).
     */
    public record Carrot(boolean enabled, boolean homozygous, List<String> flavour) {
        public static final Carrot DEFAULT = new Carrot(true, false, List.of());

        public Carrot {
            flavour = List.copyOf(flavour);
        }
    }

    /**
     * <b>Prose about the gene, written by whoever wrote the gene.</b>
     *
     * <p>The format already had two pieces of human-readable text and neither
     * is this one. {@code blurb} is a <i>summary</i> - one to three sentences,
     * sized for a tooltip and an in-game browser entry, and it has to stay that
     * size. An expression's {@code description} says what one outcome looks
     * like. Between them there was nowhere to write down the thing an author
     * actually knows and nobody else does: where the idea came from, why the
     * numbers are the numbers, what it looks like on a horse that carries
     * something else, what was tried and abandoned.
     *
     * <p>That used to go in a comment - except JSON has no comments - or on the
     * wiki page, where it drifts from the gene the moment either moves. So it
     * lives here, beside the layers it is about, and {@code GeneWikiTool} prints
     * it on the generated page: <b>write it once, in the file</b>.
     *
     * <p>One entry per paragraph. Empty for most genes, and that is fine - this
     * is for what is worth saying, not a field to fill in.
     * {@link ExpressionSpec#notes} is the same thing for a single outcome.
     */
    public List<String> notes() {
        return notes;
    }

    /**
     * <b>What the gene's one photograph should be of</b>, when measuring it
     * comes out wrong.
     *
     * <p>Which allele combination and which base coat a gene is illustrated on
     * is normally <i>detected</i> - {@code CoatVisibility} bakes the candidates
     * and keeps the loudest, so no list anywhere goes stale when a gene starts
     * or stops painting. That is the right default and stays the default. But
     * "the loudest" and "the one worth looking at" are not always the same
     * thing: Flametouched's homozygote is a whole-horse ember gradient and its
     * heterozygote is the flames the gene is named for, and Patina reads more
     * clearly on a plain bay than on the tobiano that gives it more texels to
     * move.
     *
     * <p>So a gene may <b>declare</b> either half and leave the other measured.
     * {@code base} is a {@code BaseCoats} key ({@code "bay"},
     * {@code "chestnut"}, {@code "tobiano"}, ...); {@code expression} is one of
     * this gene's own expression ids, and the loudest combination landing on
     * that expression is the one photographed. Both {@code null} - the usual
     * case - means measure everything, as before.
     */
    public record Preview(String base, String expression) {
        public static final Preview AUTO = new Preview(null, null);
    }

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
            List<LocusCondition> needs,
            List<Layer> layers,
            List<GeneAbility> abilities,
            List<String> notes) {

        /**
         * <b>Prose about this one outcome</b> - the same idea as
         * {@link GeneSpec#notes()}, one level down. {@link #description} says
         * what the outcome looks like and is written for a player;
         * {@code notes} is for everything a reader of the <i>file</i> wants:
         * why this outcome is drawn the way it is, which layer is doing the
         * work, what it collides with. One entry per paragraph, usually empty.
         */
        public List<String> notes() {
            return notes;
        }

        /** Is this the catch-all that takes whatever no other expression claimed? */
        public boolean isCatchAll() {
            return combinations.isEmpty();
        }

        /** Does this entry only apply when a <i>second</i> locus says so? */
        public boolean conditional() {
            return !needs.isEmpty();
        }
    }

    /**
     * A requirement on <b>another gene</b> - what makes a data-driven gene
     * polygenic.
     *
     * <p>{@code gene} is another gene's {@code key()}, and {@code copies} maps
     * one of <i>its</i> allele tokens to how many copies the horse must carry
     * for this expression to be the one. Every entry has to hold.
     *
     * <p>The mechanism it drives is not new - {@code Gene.expressionIn} and
     * {@code Gene.coatDependsOn} have existed for the leopard complex reading
     * PATN1 and PATN2 since long before the file format did. All this adds is a
     * way to say the same thing without writing a class, and it is deliberately
     * the <i>narrow</i> version of that: a count at another locus, and nothing
     * conditional on the phenotype there. Accretion is the gene built on it -
     * one locus decides whether the pale field exists and what colour it is,
     * and a second decides whether it is the topline or the underside.
     */
    public record LocusCondition(String gene, Map<String, Integer> copies) {}

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

    /**
     * Where (masks) crossed with what (an op), plus whether what it paints
     * <b>glows</b>.
     *
     * <p>{@code emissive} is a property of the layer rather than of the op
     * because glowing is orthogonal to colour: the same {@code TOWARD} that
     * paints a teal spot paints a <i>lit</i> teal spot with one flag flipped,
     * and a gene that wants both a lit core and an unlit bloom writes two
     * layers rather than two ops. Texels the layer covers past
     * {@link #EMISSIVE_THRESHOLD} are handed to
     * {@code CoatOverlay.markEmissive}; the coat colour is unaffected either
     * way, so a spec that sets it on a natural gene is a load error.
     */
    public record Layer(String name, List<Mask> masks, Op op, boolean emissive) {}

    /**
     * How much of a texel an emissive layer has to cover before that texel is
     * marked full-bright. Emissiveness is a boolean per texel - there is no
     * half-lit - so a soft-edged glow needs a cut somewhere, and taking it at
     * the halfway point keeps the lit region the shape the mask drew rather
     * than a bloom two body units wider than it.
     */
    public static final double EMISSIVE_THRESHOLD = 0.5;

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
        /** {@code BodyStripes} - parallel bands with a chevron slant. */
        STRIPES,
        /** {@code BodyNoise.cellDistance} - the dapple / rosette field. */
        DAPPLES,
        /** Thresholded value noise - big irregular blobs (pinto, roan patches). */
        PATCHES,
        /** Smooth value noise as a soft shading field (sooty, countershading). */
        NOISE,
        /**
         * <b>Value noise summed over several octaves</b> - detail at more than
         * one scale, which is the whole difference between a blob and a
         * pattern.
         *
         * <p>{@link #PATCHES} and {@link #NOISE} sample the lattice once, so
         * every feature they draw is the same size. Real markings are not like
         * that: a laced patch has a coarse outline, a finer scallop riding on
         * it, and a filigree finer still, and no single frequency can be all
         * three. This sums {@code octaves} of the same field at rising
         * frequency and falling amplitude, so one mask carries all of them.
         *
         * <p>Two things about it are deliberate and are not what a textbook
         * fractal-noise routine does:
         * <ul>
         *   <li><b>The octaves are normalised to keep their spread, not their
         *       sum.</b> Dividing by the total amplitude - what
         *       {@code PatchNoise.field} does, and what every fbm snippet does -
         *       averages independent samples, so the field concentrates harder
         *       round 0.5 with every octave added. A {@code threshold} tuned at
         *       three octaves then covers a different amount of horse at five,
         *       silently. Dividing by the <i>root</i> of the summed squares
         *       instead leaves the spread where one octave had it, so
         *       {@code octaves} buys detail and nothing else, and one octave is
         *       exactly {@link #PATCHES}. This is the same class of defect as
         *       the {@code cover} knobs that were written against a field
         *       nobody had measured.</li>
         *   <li><b>{@code shape} is applied to the summed field, not per
         *       octave.</b> Ridging each octave and then summing gives a
         *       thicket; ridging once gives continuous lines that fork and
         *       taper - the lace. It is the same move
         *       {@code BodyNoise.ridge} already makes on a single octave, and
         *       {@link #STROKES} is built on it.</li>
         * </ul>
         */
        FRACTAL,
        /**
         * <b>A choice made once per horse</b> - the same everywhere on the body,
         * and either fully on or fully off.
         *
         * <p>Every other mask is a function of <i>where</i> you are on the
         * horse. This one is not: it draws an integer from a seed knob and asks
         * whether it came out equal to {@code is}. That is the one thing the
         * mask fold could not express, because a knob can move a boundary but
         * cannot choose between two of them.
         *
         * <p>It replaces a trick. {@code quarter} has to pick which quadrant
         * goes pale, and did it by sampling {@link #NOISE} at a scale of 4000
         * body units - a hundred times the length of the horse, so the whole
         * animal sat inside one lattice cell - and amplifying the result onto
         * [-200, 201] so the clamp turned it into a hard yes or no. It worked,
         * and it was <i>nearly</i> constant rather than constant in two ways
         * that traded off against each other: the field still drifted a little
         * across the horse, and about one horse in four hundred drew inside the
         * strip that clamps to neither end and got a half-strength quadrant.
         * See known-gaps gap 102.
         */
        CHOICE,
        /**
         * <b>Something poured along a line and running off it</b> - a band whose
         * edge sags into separate drips of different lengths, each with a heavy
         * tip, joined to the band by a concave fillet.
         *
         * <p>It exists because {@link #WAVES} is the wrong shape for a liquid
         * and cannot be talked into it. A wave is one periodic function: every
         * lobe is the same length, the same width and the same distance from
         * the next, and the only edge shapes on offer are a scallop, a tooth and
         * a ramp. Goo is not periodic - the whole read of a drip is that one ran
         * further than its neighbour - and it is not a displaced line either,
         * because a drip is fatter at the bottom than where it left the band.
         * The owner's report on the first ooze drip was "it looks like
         * triangles", which is what a sine at this scale is.
         *
         * <p>So this is a <b>distance field</b>, not a displacement: a half-plane
         * for the band, a capsule per drip, a disc for each drip's tip, unioned
         * with a polynomial smooth-minimum so the joins fillet the way a liquid's
         * meniscus does. Everything about a drip - whether it is there at all,
         * where along the band it hangs, how far it runs and how thick it is -
         * is drawn per cell from the seed, so no two are alike and a horse keeps
         * its own.
         */
        GOO,
        /** Coverage read off the pigment the earlier genes left - "find the black". */
        PIGMENT,
        /**
         * Coverage read off what the coat actually <b>looks like</b> - the
         * colour phase 2 resolved through the {@code GradientLut}, plus whatever
         * the magical genes painted before this one.
         *
         * <p>{@link #PIGMENT} and this are not two spellings of one idea. Pigment
         * is <i>melanin</i>: a pair of levels that mean nothing until a gradient
         * chart turns them into a colour. "Black" and "white" are facts about
         * that colour, not about the levels - the LUT locus can swap the chart
         * for one whose black corner is violet, and a horse resolved through it
         * has exactly the same pigment and is no longer black anywhere. A gene
         * that says "cover the black parts" has to ask the question the viewer
         * asks, which is this one.
         *
         * <p>It reads {@link com.example.horsegenetics.common.coat.pattern.ColorView#visible}
         * - the composited appearance - so a texel the natural phase left bare
         * reads as the white template rather than as the transparent black
         * underneath it. Magical phase only: phase 1 has no colour yet, and the
         * overlay pass has no accumulator left, so the parser refuses it in both.
         */
        LUMA,
        /**
         * Discrete round or oval <b>elements</b> on a jittered lattice - a spot
         * field. Unlike {@link #DAPPLES}, which fills the horse with cells and
         * leaves a web between them, this leaves most of the horse bare and puts
         * countable marks on it: a leopard spot, a star, a freckle.
         */
        SPOTS,
        /** The same lattice drawn as <b>annuli</b> - a ring, a crescent, a rosette. */
        RINGS,
        /** Fine stipple with no discrete boundary - dust, ticking, speckling. */
        SPECKLE,
        /**
         * Tapering, curving <b>strokes</b> - {@code BodyNoise.ridge} sampled in a
         * frame stretched along one axis. The shape behind every streak,
         * scratch, ribline and brindle bar.
         */
        STROKES,
        /** One closed <b>spiral</b> per named part - the filigree / circuit figure. */
        SPIRAL,
        /**
         * <b>A shape somebody drew</b> - control points in a plane, stroked as
         * a line or filled as an outline, extruded through the horse.
         *
         * <p>It is the odd one out here and is meant to be. Every other mask
         * says what a shape is <i>made of</i> - a lattice of spots, a band of
         * this width, noise over that scale - and lets the geometry and the
         * horse's own numbers decide where it lands. Those are the right tool
         * for a marking that has a rule behind it, which is nearly all of
         * them. But a lightning bolt, a crescent, a brand, a specific curl on
         * a specific shoulder has no rule behind it: it is a shape, and the
         * only honest way to describe it is to give its outline.
         *
         * <p><b>Points, not pixels.</b> A drawn shape could have been stored as
         * a small bitmap stencil, and that would have been easier to draw with
         * and wrong in three ways: it fixes the resolution, it cannot be
         * measured in body units so it does not follow a resized horse, and it
         * turns a gene file into a picture file. Control points in body space
         * stay small, stay legible in the JSON, and are the same maths in Java
         * and in the browser.
         *
         * <p>It is still a <b>mask</b>, so everything else composes with it as
         * usual: multiply a {@code PATH} by a {@code NOISE} to break its edge
         * up, or by a {@code CHOICE} so only some horses carry it. And because
         * the points can be knob-free constants while the width is a knob, a
         * drawn shape can still vary per horse without ceasing to be that
         * shape.
         */
        PATH,
        /**
         * A band whose edges are displaced by a <b>sine</b> running along
         * another axis - the one shape in the vocabulary that is smooth on
         * purpose.
         *
         * <p>Everything else that curves here curves because noise bent it, and
         * noise never repeats. A wave does: a lobed boundary of three to six
         * even scallops, or a string of lights that rises and falls the same
         * way twice, is a periodic function and reads wrong when it is faked
         * with a ridge. Set {@code spacing} to repeat the band and it draws
         * parallel ribbons instead of one edge.
         */
        WAVES,
        /**
         * The <b>rim of each body part's box</b>, on the face this texel sits
         * on - a wireframe of the horse.
         *
         * <p>It is the one mask that asks about the <i>model</i> rather than
         * about body space. Every other shape here is a field sampled at a
         * point, and a field has no idea where the horse stops; a Minecraft
         * horse is a handful of boxes, and the line where two of a box's faces
         * meet is the only thing in this geometry that reads as an <b>edge</b>
         * to a viewer. So this measures, in the two axes that span the face,
         * how far the texel is from the nearest of that face's four
         * boundaries.
         *
         * <p>The consequence worth knowing before using it: it outlines
         * <b>every</b> box, not the silhouette. A leg gets a rectangle round
         * each of its faces, including the one buried in the barrel. That is
         * the honest thing for a mask with no visibility test, and on a boxy
         * model it is also what a wireframe is supposed to look like.
         */
        EDGE,
        /**
         * <b>Polygons that tile</b>, each filled solid, separated by a channel
         * of even width - {@code BodyNoise.cellEdge}. A giraffe, a cracked
         * glaze, a dry lake bed.
         *
         * <p>{@link #DAPPLES} and {@link #SPOTS} both draw round marks, however
         * they are pushed, because both measure to a cell's centre. This
         * measures to the wall between two cells, which is what makes the edges
         * straight and the corners meet three at a time.
         */
        CRACKLE,
        /**
         * <b>An SVG path, drawn.</b> {@link #PATH} with the whole of the
         * grammar behind it instead of a polyline.
         *
         * <p>{@code PATH} was the admission that some shapes are not made of a
         * rule - that a marking somebody <i>drew</i> has to arrive as points.
         * This is the admission that follows from it: markings that get drawn
         * get drawn in a drawing program, and what comes out is a {@code d}
         * string of cubics, arcs and subpaths inside a {@code viewBox}. Turning
         * one of those into sixty-four normalised control points by hand is
         * where the drawing stops being the drawing - the holes close up, the
         * arcs become chords, and the author spends an evening on arithmetic
         * that {@link com.example.horsegenetics.common.coat.pattern.SvgPath}
         * does exactly.
         *
         * <p>So this mask takes the file's own vocabulary and keeps the parts of
         * it that carry meaning: subpaths (so a letter O has a hole), the fill
         * rule (so the author says <i>which</i> hole), the transform list, the
         * {@code viewBox} and its aspect-ratio behaviour, and a stroke with
         * real caps, joins and a dash pattern. Everything is flattened once at
         * load; per texel it costs what {@code PATH} costs.
         *
         * <p>Like {@code PATH} it carries a shape rather than a rule for one,
         * so it is the same on every horse - which is the point, and is also
         * the reason to reach for a field mask first if what you want is
         * variety.
         */
        SVG,
        /**
         * <b>Bars that radiate from a point</b> - a periodic angular field, the
         * polar twin of {@link #WAVES}.
         *
         * <p>{@code WAVES} takes its phase from a linear coordinate, so its
         * bars are parallel, evenly spaced and all pointed the same way
         * everywhere in the region: that is the measurement it makes, and no
         * setting of it recovers a fan. A sunburst, the gills under a mushroom
         * cap and the black bars on a butterfly's wing all share one origin,
         * and their angle <i>and</i> their gap both grow with distance from it,
         * which is an angle around a pivot rather than a distance along a line.
         *
         * <p>{@code twist} rotates the fan's zero with distance, which is the
         * difference between a sunburst and a pinwheel.
         */
        FAN,
        /**
         * <b>Which way the surface faces</b> - the model's own normal, dotted
         * with a body axis.
         *
         * <p>The second mask, after {@link #EDGE}, that asks about the model
         * rather than about body space, and it asks the other question: not
         * where a box stops but which way its face is pointed. A shell's sheen,
         * a rim light along the topline, a wash that only takes on the
         * upward-facing planes - all of them are functions of facing, and every
         * other mask here would have to fake them with a position band that
         * gets it wrong the moment the geometry turns.
         *
         * <p><b>It is not iridescence.</b> The coat is a baked texture and this
         * class never learns where a camera is, so a colour run through this
         * mask is fixed on the horse: it shifts with the <i>surface</i>, not
         * with the viewer. That is a structural sheen, and it is the honest
         * half of the effect.
         */
        NORMAL
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
        /** Natural: move red / black toward explicit levels (blacken, body colour). */
        SET_PIGMENT,
        /**
         * Natural: {@code PigmentField.whiten} - mix white hair in. The op every
         * white marking wants; {@code SET_PIGMENT} to {@code (0, 0)} reaches the
         * same place but browns the soft edge on a black horse getting there.
         */
        WHITEN,
        /** Magical: add signed RGB (and opacity) - the zebra move. */
        TINT,
        /** Magical: walk what the texel <i>looks</i> like toward a colour - the pink-hair move. */
        TOWARD,
        /** Magical: flat opaque paint that replaces the accumulator. Masking genes only. */
        FLAT,
        /**
         * Magical: {@link #TOWARD} whose target colour <b>runs along an axis</b>
         * - a hue ramp. The spectral tail, the aurora band, the mane that fades
         * from root to tip.
         */
        RAMP,
        /**
         * Magical: {@link #TOWARD} whose target colour is <b>drawn per cell</b>
         * from a palette - the opal, the nebula, the galaxy. Adjacent cells take
         * different colours and the boundary between them is a cell wall, which
         * is what separates "iridescent" from "gradient".
         */
        PALETTE,
        /**
         * Magical: replace the texel with its <b>photographic negative</b>.
         * White goes black, orange goes blue. The one move the other colour ops
         * cannot make between them - every one of those walks <i>toward</i>
         * something, and a negative is a function of what is already there.
         */
        INVERT;

        public boolean isNatural() {
            return this == DILUTE || this == RESTRICT || this == SET_PIGMENT || this == WHITEN;
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

        public static final Params EMPTY = new Params(CommonMaps.<String, Object>empty());

        private static final double[] EMPTY_POINTS = new double[0];

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

        /** A list-of-colours parameter, as 0xRRGGBB each; empty when absent. */
        @SuppressWarnings("unchecked")
        public List<Integer> colors(String name) {
            Object o = raw.get(name);
            return o == null ? List.of() : (List<Integer>) o;
        }

        /** A {@code "#rrggbb"} parameter, as 0xRRGGBB. */
        public int color(String name, int fallback) {
            Object o = raw.get(name);
            return o == null ? fallback : (Integer) o;
        }

        /**
         * A {@code PATH}'s control points, flat as {@code [u0, v0, u1, v1,
         * ...]}; empty when absent.
         *
         * <p>Handed back as the array the parser built rather than a copy,
         * because the painter asks for it once per texel and copying sixty-four
         * doubles seventy thousand times to protect a value nobody writes to is
         * not a trade worth making. Nothing in the pipeline writes to it; the
         * parser is the only thing that ever fills it in.
         */
        public double[] points(String name) {
            Object o = raw.get(name);
            return o == null ? EMPTY_POINTS : (double[]) o;
        }

        /**
         * An {@code SVG} mask's path data, <b>already flattened</b>; null when
         * absent.
         *
         * <p>The only parameter whose parsed form is not the thing the file
         * wrote. Flattening a {@code d} string means walking a grammar and
         * sampling every curve in it, and doing that per texel would cost more
         * than painting the horse; doing it at load costs it once. The source
         * string rides along on {@link com.example.horsegenetics.common.coat.pattern.SvgPath.Shape#d()}
         * so nothing that wants to write the file back out has to reconstruct
         * it.
         */
        public com.example.horsegenetics.common.coat.pattern.SvgPath.Shape svg(String name) {
            Object o = raw.get(name);
            return o == null ? null : (com.example.horsegenetics.common.coat.pattern.SvgPath.Shape) o;
        }
    }
}
