package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.BodyNoise;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.PatchNoise;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.pattern.PigmentView;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Axis;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.BodyPoint;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneRarity;

import java.util.List;

/**
 * <b>Manchado</b> ({@code horsegenetics.manchado}) - the rare Argentine pattern:
 * broad, clean white fields over the <b>topline</b>, with smooth rounded
 * <b>islands of the base colour left inside them</b>, a mostly white tail, and a
 * head, belly and lower legs that stay dark.
 *
 * <p>It is close to the inverse of a leopard: a leopard puts dark spots on a
 * white horse and this leaves dark islands in a white field, and the difference
 * is where the white comes from and what shape the leftovers are.
 *
 * <h2>Its own locus, deliberately</h2>
 * Manchado is <b>not</b> a leopard-complex modifier and <b>not</b> a
 * {@code KIT} allele, and putting it in either would have been a claim nobody
 * has earned. It is not one of the mapped, testable white-spotting patterns at
 * all: the reviews catalogue {@code KIT} (tobiano, sabino, the dominant whites),
 * {@code MITF} and {@code PAX3} (splash), {@code EDNRB} (frame) and
 * {@code TRPM1} plus {@code RFWD3} (the leopard complex), and manchado is in
 * none of them. A horse cannot be confirmed manchado by a white-pattern panel;
 * it is identified by its shape.
 *
 * <p>The long-standing proposal is a rare autosomal <b>recessive</b>, which is
 * what this models - it explains why the pattern turns up sporadically out of
 * solid-looking parents across several Argentine populations (Criollo, polo
 * ponies, Arabians, Hackneys) without spreading like a dominant pinto gene, and
 * it makes a founder effect the obvious story. The competing ideas are somatic
 * mosaicism and a transposable element; none of the three is proven, and this is
 * a <b>modelled</b> allele.
 *
 * <h2>What tells it apart</h2>
 * <ul>
 *   <li><b>Not sabino</b>: sabino's margins are jagged and roaned, and it puts
 *       white on the face and legs. Manchado's white is clean and opaque.</li>
 *   <li><b>Not tobiano</b>: tobiano crosses the spine in large smooth patches
 *       and whitens the legs, but leaves no rounded islands inside its white.</li>
 *   <li><b>Not splash</b>: splash comes up from below - white belly, white legs,
 *       a bald face, blue eyes. Manchado is top-down and its underside stays
 *       coloured.</li>
 *   <li><b>Not the leopard complex</b>: no mottled skin, no striped hooves, no
 *       white sclera, and the islands are smooth ovals rather than spot
 *       clouds.</li>
 *   <li><b>Not roan</b>: patch white, not hair-by-hair mixing.</li>
 * </ul>
 *
 * <p><b>No health association.</b> The one white pattern here of which almost
 * nothing is known, and inventing a cost for it would be inventing a fact.
 */
public final class ManchadoGene implements Gene {

    public static final String KEY = "horsegenetics.manchado";

    /** With the white-spotting loci, after {@code KIT}. */
    public static final int PRIORITY = 77;

    /** How many founders in a hundred carry exactly one copy. No founder is affected. */
    private static final double CARRIER_PERCENT = 1.2;

    // --- the white field -------------------------------------------------

    /** Height up the barrel where the dorsal field starts, and where it is total. */
    private static final double VENTRAL = 0.30;
    private static final double DORSAL = 0.62;
    /** How far the field's margin wanders, and at what body-space frequency. */
    private static final double MARGIN_WANDER = 0.30;
    private static final double MARGIN_FREQ = 0.28;
    /**
     * How crisp the boundary is. Small: manchado's white does <b>not</b>
     * dissolve into roaning, which is the first thing that separates it from
     * sabino, and a soft edge here would draw a sabino.
     */
    private static final double MARGIN_EDGE = 0.045;

    // --- the islands -----------------------------------------------------

    /** Cell size of the island field, in body units. Large: islands, not flecks. */
    private static final double ISLAND_SCALE = 0.30;
    /** Cell distance below which a texel is inside an island, and the softness of its rim. */
    private static final double ISLAND_CORE = 0.58;
    private static final double ISLAND_EDGE = 0.09;
    /**
     * Which parts of the field carry islands at all. Sampled at a <b>much lower
     * frequency than the cells</b> - at the cells own frequency it varied inside
     * each one and cut it in half, so the field came out as slivers instead of
     * ovals. Low-frequency, it picks island-rich and island-poor regions and
     * leaves the ovals whole.
     */
    private static final double ISLAND_WHERE_SCALE = 0.09;
    private static final double ISLAND_SHARE = 0.56;

    /** How much of the tail goes white. Reported as a notable manchado feature. */
    private static final double TAIL_WHITE = 0.88;

    public final Allele ma = new Allele(KEY, 0, "ma", "Manchado (ma)");
    public final Allele Ma = new Allele(KEY, 1, "Ma", "Wild-type (Ma)");
    private final List<Allele> alleles = List.of(ma, Ma);

    private final Expression WILD = Expression.wildType(
            "Two ordinary copies. Nothing shows and nothing is passed on.");

    /**
     * The silent heterozygote, worded separately. It is the whole player-facing
     * value of a rare recessive: the only way to a manchado foal is two carriers,
     * and a pedigree is the only way to find them.
     */
    private final Expression CARRIER = Expression.wildType("manchado-carrier", "Manchado carrier",
            "One copy of ma. The horse is completely ordinary and there is no way to see it - but "
                    + "half its foals inherit the copy, and two carriers bred together are the "
                    + "only way manchado appears at all.");

    private final Expression MANCHADO = Expression.of("manchado", "Manchado")
            .describe("Broad, clean white over the back, crest, shoulder and croup, with smooth "
                    + "rounded islands of the base colour left inside it and a mostly white tail. "
                    + "The head, the underside of the neck, the belly and the lower legs stay "
                    + "coloured - the pattern is top-down, which is what separates it from splash.")
            .varies()
            .restrict(ManchadoGene::paint);

    private final List<Expression> expressions = List.of(WILD, CARRIER, MANCHADO);

    /**
     * <b>Carriers only.</b> Manchado is described as extremely rare wherever it
     * turns up, and it is meant to be a find: the founder table has no affected
     * row at all, so the only way to see one is to breed two carriers. The same
     * shape the health loci use, for a much happier reason.
     */
    private final FounderTable founders = FounderTable.builder()
            .weight(ma, Ma, CARRIER_PERCENT)
            .weight(Ma, Ma, 100.0 - CARRIER_PERCENT)
            .build();

    @Override public String key() { return KEY; }
    @Override public String name() { return "Manchado"; }
    @Override public int priority() { return PRIORITY; }
    @Override public GeneRarity rarity() { return GeneRarity.EPIC; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return Ma; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    /** Homozygous, so the known carrot hands over a carrier and the pattern still has to be bred. */
    @Override public boolean geneCarrotHomozygous() { return false; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        if (pair.homozygousFor(ma)) {
            return MANCHADO;
        }
        return pair.has(ma) ? CARRIER : WILD;
    }

    public boolean isManchado(AllelePair pair) {
        return pair.homozygousFor(ma);
    }

    // ------------------------------------------------------------------
    // Painting
    // ------------------------------------------------------------------

    /**
     * <b>Two draws</b>: the field seed and the island seed. They are separate so
     * the white's outline and the islands inside it vary independently - two
     * manchados with similar white can carry quite different islands, which is
     * what makes each one an individual rather than a stamp.
     */
    private static PigmentField paint(CoatBuildContext ctx, PigmentView coat) {
        Rng epi = ctx.epigeneticsFor(KEY);
        long fieldSeed = epi.nextLong();
        long islandSeed = epi.nextLong();

        Skin skin = ctx.skin();
        PigmentField f = coat.mutableCopy();
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            double white = fieldWeight(skin, part, point, fieldSeed);
            if (white <= 0) {
                return;
            }
            // The island field is subtracted from the white, not added over it:
            // an island is base coat the white never reached.
            white *= 1.0 - island(point, islandSeed);
            if (white > 0) {
                f.whiten(px, py, (float) white);
            }
        });
        return f;
    }

    /**
     * The white field: a <b>dorsal sheet</b> with a crisp, wandering margin.
     *
     * <p>Top-down is the whole geometry. The back, crest, upper shoulder, upper
     * barrel and croup go white; the belly, the underside of the neck, the head
     * and the lower legs do not. That dorsal-versus-ventral contrast is the
     * pattern's strongest cue and the thing that separates it from splash, which
     * comes up from underneath.
     */
    private static double fieldWeight(Skin skin, Part part, BodyPoint point, long seed) {
        if (part == Part.TAIL) {
            return TAIL_WHITE;      // reported as a notable manchado feature
        }
        if (part != Part.BODY && part != Part.NECK) {
            return 0;               // head, mane, ears, legs - all stay coloured
        }
        Bounds b = HorseSkinGeometry.bounds(skin, part);
        double fy = (point.y() - b.yMin()) / b.span(Axis.Y);
        // The margin wanders, at a low frequency, so the sheet is not a level
        // waterline - but it stays crisp, because manchado's white does not roan.
        double wander = (PatchNoise.field(seed, point.x(), point.y(), point.z(), MARGIN_FREQ) - 0.5)
                * 2.0 * MARGIN_WANDER;
        double onset = VENTRAL + wander;
        double full = DORSAL + wander;
        double t = (fy - onset) / Math.max(1e-6, full - onset);
        return PatchNoise.smoothstep(0.5 - MARGIN_EDGE, 0.5 + MARGIN_EDGE, t);
    }

    /**
     * The islands: <b>round cells of base colour</b> left standing inside the
     * white, from the same cellular field grey's dapples use. Big enough to read
     * individually at a distance, and only about a third of the cells are
     * islands at all - a dense blanket of small spots would be a leopard, which
     * is the mistake this shape exists to avoid.
     */
    private static double island(BodyPoint point, long seed) {
        double d = BodyNoise.cellDistance(seed, point.x() * ISLAND_SCALE,
                point.y() * ISLAND_SCALE, point.z() * ISLAND_SCALE);
        // Which cells are islands: a second, cell-scale value field thresholded
        // so the field is islands-in-white rather than white-between-islands.
        double which = BodyNoise.value(seed ^ 0x4D414E43_4841444FL,
                point.x() * ISLAND_WHERE_SCALE, point.y() * ISLAND_WHERE_SCALE,
                point.z() * ISLAND_WHERE_SCALE);
        if (which > ISLAND_SHARE) {
            return 0;
        }
        return 1.0 - PatchNoise.smoothstep(ISLAND_CORE - ISLAND_EDGE, ISLAND_CORE + ISLAND_EDGE, d);
    }
}
