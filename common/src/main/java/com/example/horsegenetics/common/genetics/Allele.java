package com.example.horsegenetics.common.genetics;

import java.util.Objects;

/**
 * One allele of one {@link Gene}. Horses carry these two-at-a-time (an
 * {@link AllelePair}) per gene, and the {@link Genotype} is the full set.
 *
 * <p>Identified by a <b>primary key</b> {@code <modauthor>.<gene>.<allele>}
 * (e.g. {@code "horsegenetics.splash.Spl"}); {@link #geneKey()} is the
 * {@code <modauthor>.<gene>} prefix, {@link #gene()} resolves it through
 * {@link Genes}.
 *
 * <p>{@link #token()} is the allele's text in a genotype code string. It can be
 * <b>any run of characters</b>, not a single letter - the code puts a {@code /}
 * between the two alleles of a gene and a {@code -} between genes, so
 * {@code "Spl/spl"} is unambiguous.
 *
 * <p>{@link #visible()} / {@link #deterministic()} are population-level hints;
 * the authoritative per-horse answers come from {@link Gene#isVisible} /
 * {@link Gene#isDeterministic} for the actual pair + genotype.
 */
public final class Allele {

    private final String key;
    private final String geneKey;
    private final String token;
    private final String label;
    private final boolean visible;
    private final boolean deterministic;

    /** Created by a {@link Gene} implementation for each of its alleles. */
    public Allele(String geneKey, String token, String label, boolean visible, boolean deterministic) {
        this.geneKey = Objects.requireNonNull(geneKey);
        this.token = Objects.requireNonNull(token);
        this.key = geneKey + "." + token;
        this.label = label;
        this.visible = visible;
        this.deterministic = deterministic;
    }

    public String key() {
        return key;
    }

    public String geneKey() {
        return geneKey;
    }

    public Gene gene() {
        return Genes.byKey(geneKey);
    }

    public String token() {
        return token;
    }

    public String label() {
        return label;
    }

    public boolean visible() {
        return visible;
    }

    public boolean deterministic() {
        return deterministic;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Allele a && a.key.equals(key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return key;
    }
}
