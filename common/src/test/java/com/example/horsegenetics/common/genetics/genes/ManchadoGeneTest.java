package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Axis;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Manchado: a rare recessive, and a shape that has to stay distinguishable from
 * every other white pattern in the mod.
 *
 * <p>The geometry claims are the ones worth pinning, because "white with holes
 * in it" is a description several genes could satisfy by accident: the white
 * must be <b>dorsal</b>, it must leave <b>islands</b> rather than flecks, and it
 * must not touch the head or the legs.
 */
class ManchadoGeneTest {

    private static final int N = HorseSkinGeometry.SHEET_SIZE;
    private static final ManchadoGene MANCHADO = Genes.MANCHADO;

    private static Genotype bay(Allele a, Allele b) {
        return Genotype.wildType()
                .with(new AllelePair(Genes.AGOUTI.A, Genes.AGOUTI.a))
                .with(new AllelePair(a, b));
    }

    // ------------------------------------------------------------------
    // A rare recessive
    // ------------------------------------------------------------------

    @Test
    void itTakesTwoCopiesAndTheCarrierIsSilent() {
        assertTrue(bay(MANCHADO.Ma, MANCHADO.Ma).expressionOf(Genes.MANCHADO).wildType());
        Expression carrier = bay(MANCHADO.ma, MANCHADO.Ma).expressionOf(Genes.MANCHADO);
        assertTrue(carrier.wildType(), "a carrier shows nothing");
        assertEquals("manchado-carrier", carrier.id(), "...but the gene browser still names it");
        assertEquals("manchado", bay(MANCHADO.ma, MANCHADO.ma).expressionOf(Genes.MANCHADO).id());
        assertTrue(MANCHADO.isManchado(new AllelePair(MANCHADO.ma, MANCHADO.ma)));
    }

    /**
     * <b>No founder is ever manchado.</b> The founder table has no affected row
     * at all, so the only way to see one is to breed two carriers - which is the
     * whole point of a pattern this rare, and the reason a pedigree is worth
     * keeping.
     */
    @Test
    void noFounderIsAffectedAndCarriersAreScarce() {
        int carriers = 0;
        for (long seed = 0; seed < 4000; seed++) {
            AllelePair pair = Genotype.random(new SeededRng(seed)).pair(Genes.MANCHADO);
            assertFalse(MANCHADO.isManchado(pair), "a founder was manchado at seed " + seed);
            if (pair.has(MANCHADO.ma)) {
                carriers++;
            }
        }
        assertTrue(carriers > 0, "4000 founders should turn up some carriers");
        assertTrue(carriers < 400, "...but it should be rare, saw " + carriers + " in 4000");
    }

    /** And its own locus - not a leopard modifier and not a KIT allele. */
    @Test
    void itIsItsOwnLocus() {
        assertFalse(Genes.LEOPARD.coatDependsOn().contains(ManchadoGene.KEY),
                "manchado is not part of the leopard complex");
        assertFalse(Genes.KIT.alleles().stream().anyMatch(a -> a.token().equals("ma")),
                "manchado is not a KIT allele");
    }

    // ------------------------------------------------------------------
    // The shape
    // ------------------------------------------------------------------

    /** Top-down. The dorsal-versus-ventral contrast is the pattern's strongest cue. */
    @Test
    void theWhiteIsDorsalAndTheUndersideStaysColoured() {
        PigmentField f = paint(bay(MANCHADO.ma, MANCHADO.ma), 4L);
        double top = bandWhite(f, 0.88);
        double belly = bandWhite(f, 0.08);
        assertTrue(top > 0.5, "the back should be broadly white, got " + top);
        assertTrue(belly < 0.10, "the belly should stay coloured, got " + belly);
    }

    /** The head and lower legs stay dark - that is what separates it from splash and sabino. */
    @Test
    void theHeadAndLegsAreUntouched() {
        PigmentField f = paint(bay(MANCHADO.ma, MANCHADO.ma), 4L);
        for (Part part : new Part[]{Part.HEAD, Part.MUZZLE,
                Part.LEFT_FRONT_LEG, Part.RIGHT_HIND_LEG}) {
            assertEquals(0.0, partWhite(f, part), 1e-9,
                    part + " must stay coloured - manchado is not splash");
        }
    }

    /**
     * A white tail is a reported manchado feature - so it must be what the gene
     * usually does, not what one lucky horse does. Averaged over a spread of
     * horses rather than pinned to a single seed: the numbers a horse carries
     * are stored now, so which horse a given seed produces is an implementation
     * detail, while "manchados tend to have white tails" is the actual claim.
     */
    @Test
    void theTailGoesMostlyWhite() {
        double total = 0;
        int horses = 20;
        for (long seed = 0; seed < horses; seed++) {
            total += partWhite(paint(bay(MANCHADO.ma, MANCHADO.ma), seed), Part.TAIL);
        }
        double mean = total / horses;
        assertTrue(mean > 0.6, "a white tail is a reported manchado feature, got mean " + mean);
    }

    /**
     * <b>Islands, not a solid sheet.</b> The white field has to keep base-colour
     * inside it - a manchado whose white came out complete would be a very
     * ordinary tobiano.
     */
    @Test
    void thereAreColouredIslandsInsideTheWhite() {
        boolean sawIslands = false;
        for (long seed = 0; seed < 20 && !sawIslands; seed++) {
            PigmentField f = paint(bay(MANCHADO.ma, MANCHADO.ma), seed);
            // In the dorsal band, count texels that are still coloured. Some is
            // islands; none at all is a plain white sheet.
            double coloured = 1.0 - bandWhite(f, 0.88);
            sawIslands = coloured > 0.06 && coloured < 0.6;
        }
        assertTrue(sawIslands, "the dorsal field should keep islands of base colour");
    }

    /** And they are islands rather than flecks: bigger than a texel or two. */
    @Test
    void theIslandsAreRoundedAndNotFlecks() {
        PigmentField f = paint(bay(MANCHADO.ma, MANCHADO.ma), 4L);
        int islandTexels = 0;
        int runs = 0;
        boolean inRun = false;
        Bounds b = HorseSkinGeometry.bounds(Skin.ADULT, Part.BODY);
        // Walk one horizontal line through the dorsal field and count how many
        // separate coloured runs it crosses, and how long they are. A leopard's
        // spot cloud gives many short runs; manchado should give few long ones.
        for (int px = 0; px < N; px++) {
            boolean coloured = false;
            for (int py = 0; py < N; py++) {
                var s = HorseSkinGeometry.sample(Skin.ADULT, px, py);
                if (s.isEmpty() || s.get().part() != Part.BODY) {
                    continue;
                }
                double fy = (s.get().point().y() - b.yMin()) / b.span(Axis.Y);
                if (fy < 0.80 || fy > 0.95) {
                    continue;
                }
                if (f.red(px, py) > 0.5f || f.black(px, py) > 0.5f) {
                    coloured = true;
                }
            }
            if (coloured) {
                islandTexels++;
                if (!inRun) {
                    runs++;
                }
            }
            inRun = coloured;
        }
        if (runs > 0) {
            assertTrue(islandTexels / (double) runs > 1.5,
                    "islands should be runs, not single texels: " + islandTexels + " over " + runs);
        }
    }

    // ------------------------------------------------------------------

    private static PigmentField paint(Genotype gt, long seed) {
        CoatBuildContext ctx = new CoatBuildContext(gt, Epigenome.fromSeed(seed), Skin.ADULT, true);
        PigmentField f = new PigmentField(N);
        for (Gene g : new Gene[]{Genes.EXTENSION, Genes.AGOUTI, Genes.MANCHADO}) {
            Expression e = gt.expressionOf(g);
            if (!e.wildType()) {
                f = e.restrict(ctx, f);
            }
        }
        return f;
    }

    /** Fraction of a horizontal band of the barrel that has been whitened. */
    private static double bandWhite(PigmentField f, double height) {
        Bounds b = HorseSkinGeometry.bounds(Skin.ADULT, Part.BODY);
        double[] acc = {0, 0};
        HorseSkinGeometry.forEachTexel(Skin.ADULT, Part.BODY, (px, py, p, face, point) -> {
            double fy = (point.y() - b.yMin()) / b.span(Axis.Y);
            if (Math.abs(fy - height) > 0.08) {
                return;
            }
            acc[1]++;
            if (f.red(px, py) < 0.5f && f.black(px, py) < 0.5f) {
                acc[0]++;
            }
        });
        return acc[0] / Math.max(1, acc[1]);
    }

    private static double partWhite(PigmentField f, Part part) {
        double[] acc = {0, 0};
        HorseSkinGeometry.forEachTexel(Skin.ADULT, part, (px, py, p, face, point) -> {
            acc[1]++;
            if (f.red(px, py) < 0.5f && f.black(px, py) < 0.5f) {
                acc[0]++;
            }
        });
        return acc[0] / Math.max(1, acc[1]);
    }
}
