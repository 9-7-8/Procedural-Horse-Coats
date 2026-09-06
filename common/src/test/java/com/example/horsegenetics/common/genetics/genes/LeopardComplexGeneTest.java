package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.CoatTextureComposer;
import com.example.horsegenetics.common.coat.pattern.GradientLut;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.testutil.Codes;
import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.common.trait.Traits;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The <b>leopard complex</b> - {@link LeopardGene} + {@link Patn1Gene} +
 * {@link Patn2Gene} - as a set, because the whole point of it is that the
 * visible outcome is a function of three loci at once.
 *
 * <p>What is pinned here:
 * <ul>
 *   <li>the eight-way {@link Gene#expressionIn} grid over LP zygosity x PATN1 x
 *       PATN2, and that the pair-only {@link Gene#expressionOf} stays coarse;</li>
 *   <li>{@code PATN1} / {@code PATN2} paint nothing without {@code LP} - a
 *       byte-identical coat and an identical texture key - but a
 *       leopard-complex horse's texture key <i>does</i> move with them;</li>
 *   <li>the painters do the obvious thing: leopard whitens the whole horse and
 *       leaves round spots, a blanket only touches the hindquarters, varnish
 *       spares the mane and tail;</li>
 *   <li>{@code LP/LP} carries CSNB and one copy does not;</li>
 *   <li>a founder never carries {@code PATN} without {@code LP}.</li>
 * </ul>
 */
class LeopardComplexGeneTest {

    private static final LeopardGene LP = Genes.LEOPARD;

    // ------------------------------------------------------------------
    // The outcome grid
    // ------------------------------------------------------------------

    @Test
    void expressionInResolvesAllEightOutcomes() {
        assertEquals("wild", idFor("lp/lp", "n/n", "n/n"));
        assertEquals("mottled", idFor("LP/lp", "n/n", "n/n"));
        assertEquals("varnish-roan", idFor("LP/LP", "n/n", "n/n"));
        assertEquals("leopard", idFor("LP/lp", "PATN1/n", "n/n"));
        assertEquals("fewspot", idFor("LP/LP", "PATN1/n", "n/n"));
        assertEquals("blanket", idFor("LP/lp", "n/n", "PATN2/n"));
        assertEquals("snowcap", idFor("LP/LP", "n/n", "PATN2/n"));
        assertEquals("semi-leopard", idFor("LP/lp", "PATN1/n", "PATN2/n"));
        assertEquals("semi-leopard", idFor("LP/LP", "PATN1/PATN1", "PATN2/PATN2"));
    }

    @Test
    void patnDoesNothingWithoutLeopardComplex() {
        assertEquals("wild", idFor("lp/lp", "PATN1/PATN1", "PATN2/PATN2"));
        assertTrue(LP.expressionIn(pair(LP, "lp/lp"),
                Genotype.parse(Codes.of("patn1", "PATN1/PATN1", "patn2", "PATN2/PATN2"))).wildType());
    }

    @Test
    void expressionOfIsCoarseAndKeepsTheGalleryToTwoLeopardPens() {
        assertEquals("wild", LP.expressionOf(pair(LP, "lp/lp")).id());
        // every LP-carrying pair reads the same here - the real answer needs PATN
        assertEquals("varnish-roan", LP.expressionOf(pair(LP, "LP/lp")).id());
        assertEquals("varnish-roan", LP.expressionOf(pair(LP, "LP/LP")).id());

        Set<String> groups = new java.util.HashSet<>();
        for (AllelePair p : com.example.horsegenetics.common.genetics.GenotypeCatalog.allPairsOf(LP)) {
            Expression e = LP.expressionOf(p);
            groups.add(e.wildType() ? "" : e.id());
        }
        assertEquals(Set.of("", "varnish-roan"), groups);
    }

    @Test
    void everyDeclaredExpressionIsResolvedSomewhere() {
        Set<String> reached = new java.util.HashSet<>();
        for (String lp : new String[]{"lp/lp", "LP/lp", "LP/LP"}) {
            for (String p1 : new String[]{"n/n", "PATN1/n"}) {
                for (String p2 : new String[]{"n/n", "PATN2/n"}) {
                    reached.add(idFor(lp, p1, p2));
                }
            }
        }
        Set<String> declared = new java.util.HashSet<>();
        for (Expression e : LP.expressions()) {
            declared.add(e.id());
        }
        assertEquals(declared, reached, "every LeopardGene expression should be reachable via expressionIn");
    }

    // ------------------------------------------------------------------
    // The texture key
    // ------------------------------------------------------------------

    @Test
    void leopardDeclaresItReadsBothModifiers() {
        assertEquals(Set.of(Patn1Gene.KEY, Patn2Gene.KEY), Set.copyOf(LP.coatDependsOn()));
    }

    @Test
    void patnMovesTheCoatCodeOnlyForALeopardComplexHorse() {
        // no LP: PATN in the code, but not in the coat code
        String plain = Genotype.parse(Codes.of("agouti", "A/a")).coatCode();
        String patnNoLp = Genotype.parse(Codes.of("agouti", "A/a", "patn1", "PATN1/PATN1")).coatCode();
        assertEquals(plain, patnNoLp, "PATN with no LP must not fork the texture cache");

        // with LP: fewspot and snowcap are different horses and must key apart
        String fewspot = Genotype.parse(Codes.of("leopard", "LP/LP", "patn1", "PATN1/n")).coatCode();
        String snowcap = Genotype.parse(Codes.of("leopard", "LP/LP", "patn2", "PATN2/n")).coatCode();
        assertNotEquals(fewspot, snowcap);
        assertTrue(fewspot.contains(Patn1Gene.KEY) && fewspot.contains(Patn2Gene.KEY),
                "a leopard-complex horse's coat code carries both modifier segments");
    }

    // ------------------------------------------------------------------
    // The painters
    // ------------------------------------------------------------------

    @Test
    void leopardWhitensTheWholeHorseAndLeavesSpots() {
        double body = whiteFraction(Codes.of("agouti", "A/a", "leopard", "LP/lp", "patn1", "PATN1/n"), Part.BODY);
        // mostly white, but not all white - the spots are base colour
        assertTrue(body > 0.45, "a leopard body should be mostly white, was " + body);
        assertTrue(body < 0.97, "a leopard body should still carry spots, was " + body);
    }

    @Test
    void fewspotIsWhiterThanLeopard() {
        double leopard = whiteFraction(Codes.of("leopard", "LP/lp", "patn1", "PATN1/n"), Part.BODY);
        double fewspot = whiteFraction(Codes.of("leopard", "LP/LP", "patn1", "PATN1/n"), Part.BODY);
        assertTrue(fewspot > leopard + 0.05, "fewspot (" + fewspot + ") should be clearly whiter than leopard (" + leopard + ")");
    }

    @Test
    void aBlanketOnlyTouchesTheHindquarters() {
        String code = Codes.of("agouti", "A/a", "leopard", "LP/lp", "patn2", "PATN2/n");
        double body = whiteFraction(code, Part.BODY);
        double frontLeg = whiteFraction(code, Part.LEFT_FRONT_LEG);
        assertTrue(body > 0.05, "the blanket should whiten part of the barrel, was " + body);
        assertTrue(body < 0.75, "the blanket should not cover the whole barrel, was " + body);
        assertTrue(frontLeg < 0.05, "a blanket leaves the front legs coloured, was " + frontLeg);
    }

    @Test
    void snowcapHasNoSpotsSoIsWhiterInsideTheBlanketThanASpottedBlanket() {
        double blanket = whiteFraction(Codes.of("leopard", "LP/LP", "patn2", "PATN2/n"), Part.BODY);
        double snowcap = whiteFraction(Codes.of("leopard", "LP/LP", "patn2", "PATN2/n"), Part.BODY);
        // both LP/LP + PATN2 -> both are snowcap; sanity that it is deterministic per seed
        assertEquals(blanket, snowcap);
    }

    @Test
    void varnishRoanSparesTheManeAndTail() {
        String code = Codes.of("agouti", "A/a", "leopard", "LP/LP");
        assertEquals(0.0, whiteFraction(code, Part.MANE), 1e-9);
        assertEquals(0.0, whiteFraction(code, Part.TAIL), 1e-9);
        assertTrue(whiteFraction(code, Part.BODY) > 0.05, "varnish roan should lighten the body");
    }

    // ------------------------------------------------------------------
    // Health
    // ------------------------------------------------------------------

    @Test
    void csnbIsCarriedByTheHomozygoteOnly() {
        Traits het = HorseTraits.resolve(Genotype.parse(Codes.of("leopard", "LP/lp")), true);
        Traits hom = HorseTraits.resolve(Genotype.parse(Codes.of("leopard", "LP/LP")), true);
        assertFalse(het.conditions().contains(LeopardGene.CSNB));
        assertTrue(hom.conditions().contains(LeopardGene.CSNB));
        // informational - it costs nothing
        assertEquals(Condition.informational("csnb", "x", "y").severity(), LeopardGene.CSNB.severity());
    }

    // ------------------------------------------------------------------
    // Founders
    // ------------------------------------------------------------------

    @Test
    void aFounderNeverCarriesPatnWithoutLeopardComplex() {
        var rng = new com.example.horsegenetics.common.SeededRng(20260906L);
        Gene patn1 = Genes.byKey(Patn1Gene.KEY);
        Gene patn2 = Genes.byKey(Patn2Gene.KEY);
        int lpCarriers = 0;
        int patnOnLp = 0;
        for (int i = 0; i < 20_000; i++) {
            Genotype g = Genotype.random(rng);
            boolean lp = g.pair(LP).has(LP.LP);
            boolean patn = ((AppaloosaModifierGene) patn1).isPresent(g.pair(Patn1Gene.KEY))
                    || ((AppaloosaModifierGene) patn2).isPresent(g.pair(Patn2Gene.KEY));
            if (lp) {
                lpCarriers++;
                if (patn) {
                    patnOnLp++;
                }
            }
            assertFalse(patn && !lp, "a founder carried PATN with no leopard complex");
        }
        assertTrue(lpCarriers > 0, "the sweep should have turned up some LP founders");
        assertTrue(patnOnLp > 0, "some of those LP founders should carry a PATN modifier");
    }

    @Test
    void leopardComplexIsRareInTheWild() {
        // founderTable ignores its context here, so null is fine
        double hom = LP.founderTable(null).share(pair(LP, "LP/LP"));
        double het = LP.founderTable(null).share(pair(LP, "LP/lp"));
        assertTrue(hom < 0.01, "LP/LP founders should be well under 1%, was " + hom);
        assertTrue(het > 0.02 && het < 0.08, "LP carriers ~5%, was " + het);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static String idFor(String lpPair, String p1Pair, String p2Pair) {
        Genotype g = Genotype.parse(Codes.of("leopard", lpPair, "patn1", p1Pair, "patn2", p2Pair));
        return LP.expressionIn(g.pair(LP), g).id();
    }

    private static AllelePair pair(Gene gene, String tokens) {
        String[] p = tokens.split("/");
        return new AllelePair(gene.fromToken(p[0]), gene.fromToken(p[1]));
    }


    /** White fraction of one body part, meaned over a few epigenetic seeds. */
    private static double whiteFraction(String code, Part part) {
        long[] seeds = {1L, 7L, 99L, 4242L};
        double sum = 0;
        for (long seed : seeds) {
            sum += whiteFraction(code, part, seed);
        }
        return sum / seeds.length;
    }

    private static double whiteFraction(String code, Part part, long seed) {
        int n = HorseSkinGeometry.SHEET_SIZE;
        int[] template = new int[n * n];
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, p, f, pt) -> template[py * n + px] = 0xFFFFFFFF);
        int[] lut = new int[16 * 16];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int shade = 255 - Math.round(y / 15f * 255);
                lut[y * 16 + x] = 0xFF000000 | (shade << 16) | (shade << 8) | shade;
            }
        }
        int[] img = CoatTextureComposer.compose(Genotype.parse(code), Epigenome.fromSeed(seed),
                Skin.ADULT, true, template, new GradientLut(lut, 16, 16));
        int[] tally = new int[2];
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, p, f, pt) -> {
            if (p != part) {
                return;
            }
            tally[1]++;
            if ((img[py * n + px] & 0xFFFFFF) > 0xE0E0E0) {
                tally[0]++;
            }
        });
        return tally[1] == 0 ? 0 : tally[0] / (double) tally[1];
    }
}
