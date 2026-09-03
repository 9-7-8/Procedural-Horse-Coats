package com.example.horsegenetics.common.testutil;

import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;

/**
 * Test helper: build a genotype code string from the wild type with a few genes
 * overridden, so a test never has to hard-code all {@code Genes.codeOrder()}
 * segments (which change whenever a gene is added).
 *
 * <pre>{@code
 *   Codes.of("agouti", "A/a", "grey", "G/g")
 * }</pre>
 *
 * matches a gene by the suffix after the last {@code .} of its key.
 */
public final class Codes {

    private Codes() {}

    public static String wildType() {
        return Genotype.wildType().toCode();
    }

    public static String of(String... geneThenPair) {
        if (geneThenPair.length % 2 != 0) {
            throw new IllegalArgumentException("expected (geneName, pair) pairs");
        }
        String[] segs = wildType().split("-");
        var order = Genes.codeOrder();
        for (int k = 0; k < geneThenPair.length; k += 2) {
            String gene = geneThenPair[k];
            String pair = geneThenPair[k + 1];
            boolean hit = false;
            for (int i = 0; i < order.size(); i++) {
                if (order.get(i).key().endsWith("." + gene)) {
                    segs[i] = pair;
                    hit = true;
                    break;
                }
            }
            if (!hit) {
                throw new IllegalArgumentException("no gene named '" + gene + "'");
            }
        }
        return String.join("-", segs);
    }
}
