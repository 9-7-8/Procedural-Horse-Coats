package com.example.horsegenetics.common.genetics;

/**
 * The two alleles a horse carries at one {@link Gene}. Order is canonicalized
 * on construction to "more dominant first" (per {@link Gene#precedence}), so a
 * pair equals its reverse and the genotype code is stable.
 */
public record AllelePair(Allele first, Allele second) {

    public AllelePair {
        if (!first.geneKey().equals(second.geneKey())) {
            throw new IllegalArgumentException(
                    "alleles from different genes: " + first + " / " + second);
        }
        Gene gene = first.gene();
        if (gene.precedence(second) < gene.precedence(first)) {
            Allele tmp = first;
            first = second;
            second = tmp;
        }
    }

    public String geneKey() {
        return first.geneKey();
    }

    public Gene gene() {
        return first.gene();
    }

    public boolean has(Allele allele) {
        return first.equals(allele) || second.equals(allele);
    }

    public boolean homozygous() {
        return first.equals(second);
    }

    /** The dominant (expressed) allele - {@code first} after canonicalization. */
    public Allele dominant() {
        return first;
    }

    public boolean anyVisible() {
        return first.visible() || second.visible();
    }

    public boolean allDeterministic() {
        return first.deterministic() && second.deterministic();
    }
}
