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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The contract both coat hooks owe the composer: a gene is handed the state so
 * far and <b>returns</b> its contribution. It never writes through its input,
 * and it is a function - same inputs, same output, every time.
 *
 * <p>This is the property that makes a gene testable on its own; the pipeline
 * as a whole is checked by {@code CoatPipelineGoldenTest}.
 */
class GeneCoatHookTest {

    private static final int N = HorseSkinGeometry.SHEET_SIZE;

    /** The wild-type genotype with {@code gene} homozygous for its variant allele. */
    private static Genotype homozygousVariant(Gene gene) {
        Allele variant = gene.alleles().stream().filter(a -> !a.equals(gene.wildType())).findFirst().orElseThrow();
        StringBuilder code = new StringBuilder();
        for (Gene g : Genes.codeOrder()) {
            String token = (g == gene ? variant : g.wildType()).token();
            if (code.length() > 0) {
                code.append('-');
            }
            code.append(token).append('/').append(token);
        }
        return Genotype.parse(code.toString());
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
    void noNaturalGeneWritesThroughTheFieldItWasHanded() {
        for (Gene gene : Genes.naturalOrder()) {
            Genotype gt = homozygousVariant(gene);
            PigmentField input = new PigmentField(N);
            PigmentField untouched = input.mutableCopy();

            PigmentField out = gene.restrict(gt.pair(gene), ctx(gt), input);

            assertTrue(sameField(input, untouched), gene.key() + " mutated its input field");
            assertNotNull(out, gene.key() + " should contribute when homozygous for its variant");
            assertFalse(sameField(out, input), gene.key() + " returned a field identical to its input");
        }
    }

    @Test
    void everyNaturalGeneIsAFunctionOfItsInputs() {
        for (Gene gene : Genes.naturalOrder()) {
            Genotype gt = homozygousVariant(gene);
            PigmentField a = gene.restrict(gt.pair(gene), ctx(gt), new PigmentField(N));
            PigmentField b = gene.restrict(gt.pair(gene), ctx(gt), new PigmentField(N));
            assertTrue(sameField(a, b), gene.key() + " is not reproducible from the same inputs");
        }
    }

    @Test
    void aGeneThatDoesNotExpressContributesNothing() {
        Genotype wild = Genotype.wildType();
        for (Gene gene : Genes.naturalOrder()) {
            if (gene == Genes.CREAM || gene == Genes.PEARL) {
                continue; // the two dilutions share one function and are gated by isVisible
            }
            assertNull(gene.restrict(wild.pair(gene), ctx(wild), new PigmentField(N)),
                    gene.key() + " should return null on a wild-type pair");
        }
        for (Gene gene : Genes.magicalOrder()) {
            assertNull(gene.tint(wild.pair(gene), ctx(wild), new PigmentField(N), new ColorField(N)),
                    gene.key() + " should return null on a wild-type pair");
        }
    }

    @Test
    void aMagicalGeneReturnsADeltaAndLeavesTheAccumulatorAlone() {
        for (Gene gene : Genes.magicalOrder()) {
            Genotype gt = homozygousVariant(gene);
            ColorField accumulator = new ColorField(N);
            accumulator.setArgb(0, 0, 0xFF123456);

            ColorField delta = gene.tint(gt.pair(gene), ctx(gt), new PigmentField(N), accumulator);

            assertNotNull(delta, gene.key() + " should contribute when homozygous for its variant");
            assertEquals(N, delta.size());
            assertEquals(0xFF123456, accumulator.argb(0, 0), gene.key() + " mutated the accumulator");
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
