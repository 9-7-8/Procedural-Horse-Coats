package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.ColorField;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The contract both coat hooks owe the composer: an {@link Expression} is handed
 * the state so far and <b>returns</b> its contribution. It never writes through
 * its input, and it is a function - same inputs, same output, every time.
 *
 * <p>This is the property that makes a gene testable on its own; the pipeline
 * as a whole is checked by {@code CoatPipelineGoldenTest}.
 */
class GeneCoatHookTest {

    private static final int N = HorseSkinGeometry.SHEET_SIZE;

    /**
     * A genotype whose every gene sits at its default allele, except
     * {@code gene}, which is homozygous for its first-declared (non-default)
     * allele.
     */
    private static Genotype homozygousVariant(Gene gene) {
        Allele variant = gene.alleles().stream()
                .filter(a -> !a.equals(gene.defaultAllele())).findFirst().orElseThrow();
        StringBuilder code = new StringBuilder();
        for (Gene g : Genes.codeOrder()) {
            String token = (g == gene ? variant : g.defaultAllele()).token();
            if (code.length() > 0) {
                code.append('-');
            }
            code.append(g.key()).append('=').append(token).append('/').append(token);
        }
        return Genotype.parse(code.toString());
    }

    /**
     * The genes whose homozygous-variant expression is still a wild type - the
     * combination genuinely does nothing on its own, so there is no
     * contribution to assert. Agouti is the case: it paints black points, and
     * this test's base genotype is chestnut, which has no black.
     */
    private static boolean expressesAlone(Gene gene) {
        Genotype gt = homozygousVariant(gene);
        return !gt.expressionOf(gene).wildType();
    }

    private static CoatBuildContext ctx(Genotype gt) {
        return new CoatBuildContext(gt, Epigenome.fromSeed(4242L), Skin.ADULT, true);
    }

    private static boolean sameField(PigmentField a, PigmentField b) {
        for (int py = 0; py < N; py++) {
            for (int px = 0; px < N; px++) {
                if (a.red(px, py) != b.red(px, py) || a.black(px, py) != b.black(px, py)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Test
    void noNaturalExpressionWritesThroughTheFieldItWasHanded() {
        for (Gene gene : Genes.naturalOrder()) {
            if (!expressesAlone(gene)) {
                continue;
            }
            Genotype gt = homozygousVariant(gene);
            PigmentField input = new PigmentField(N);
            PigmentField untouched = input.mutableCopy();

            PigmentField out = gt.expressionOf(gene).restrict(ctx(gt), input);

            assertTrue(sameField(input, untouched), gene.key() + " mutated its input field");
            assertNotNull(out, gene.key() + " should contribute when homozygous for its variant");
            assertFalse(sameField(out, input), gene.key() + " returned a field identical to its input");
        }
    }

    @Test
    void everyNaturalExpressionIsAFunctionOfItsInputs() {
        for (Gene gene : Genes.naturalOrder()) {
            if (!expressesAlone(gene)) {
                continue;
            }
            Genotype gt = homozygousVariant(gene);
            PigmentField a = gt.expressionOf(gene).restrict(ctx(gt), new PigmentField(N));
            PigmentField b = gt.expressionOf(gene).restrict(ctx(gt), new PigmentField(N));
            assertTrue(sameField(a, b), gene.key() + " is not reproducible from the same inputs");
        }
    }

    /**
     * The wild-type genotype expresses nothing anywhere. Under the combination
     * table this is structural rather than a per-gene early return: a
     * {@code wildType} expression carries no painter at all, so the composer
     * skips it without asking the gene anything.
     */
    @Test
    void everyGeneIsWildTypeOnTheAllDefaultGenotype() {
        Genotype wild = Genotype.wildType();
        for (Gene gene : Genes.codeOrder()) {
            Expression e = wild.expressionOf(gene);
            assertTrue(e.wildType(),
                    gene.key() + " should be a wild type when every gene sits at its default allele,"
                            + " but produced '" + e.id() + "'");
        }
    }

    @Test
    void aMagicalExpressionReturnsADeltaAndLeavesTheAccumulatorAlone() {
        for (Gene gene : Genes.magicalOrder()) {
            if (!expressesAlone(gene)) {
                continue;
            }
            Genotype gt = homozygousVariant(gene);
            ColorField accumulator = new ColorField(N);
            accumulator.setArgb(0, 0, 0xFF123456);

            ColorField delta = gt.expressionOf(gene).tint(ctx(gt), new PigmentField(N), accumulator);

            assertNotNull(delta, gene.key() + " should contribute when homozygous for its variant");
            assertEquals(N, delta.size());
            assertEquals(0xFF123456, accumulator.argb(0, 0), gene.key() + " mutated the accumulator");
        }
    }

    /** Every expression a gene can return has to be one it declared. */
    @Test
    void everyResolvedExpressionIsDeclared() {
        for (Gene gene : Genes.codeOrder()) {
            for (AllelePair pair : GenotypeCatalog.allPairsOf(gene)) {
                Expression e = gene.expressionOf(pair);
                assertTrue(gene.expressions().contains(e),
                        gene.key() + " resolves " + pair.toTokens() + " to '" + e.id()
                                + "', which is not in its expressions() list");
            }
        }
    }

    /** A wild-type outcome paints nothing, in either phase. */
    @Test
    void aWildTypeExpressionHasNoPainter() {
        for (Gene gene : Genes.codeOrder()) {
            for (Expression e : gene.expressions()) {
                if (!e.wildType()) {
                    continue;
                }
                Genotype wild = Genotype.wildType();
                assertEquals(null, e.restrict(ctx(wild), new PigmentField(N)),
                        gene.key() + " wild type '" + e.id() + "' painted pigment");
                assertEquals(null, e.tint(ctx(wild), new PigmentField(N), new ColorField(N)),
                        gene.key() + " wild type '" + e.id() + "' painted colour");
            }
        }
    }

    @Test
    void magicalAndNaturalAreMutuallyExclusiveAndTheOrderingsAgree() {
        for (Gene gene : Genes.naturalOrder()) {
            assertTrue(gene.isNatural(), gene.key() + " is in naturalOrder but declares itself magical");
        }
        for (Gene gene : Genes.magicalOrder()) {
            assertFalse(gene.isNatural(), gene.key() + " is in magicalOrder but declares itself natural");
        }
        assertEquals(Genes.codeOrder().size(), Genes.naturalOrder().size() + Genes.magicalOrder().size(),
                "every registered gene belongs to exactly one phase");
    }
}
