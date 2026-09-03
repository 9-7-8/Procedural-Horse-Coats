package com.example.horsegenetics.common.genetics;

import java.util.Objects;

/**
 * One allele of one {@link Gene}. Horses carry these two-at-a-time (an
 * {@link AllelePair}) per gene, and the {@link Genotype} is the full set.
 *
 * <p>Identified by a <b>primary key</b> {@code <modauthor>.<gene>.<allele>}
 * (e.g. {@code "horsegenetics.kit.SB1"}); {@link #geneKey()} is the
 * {@code <modauthor>.<gene>} prefix, {@link #gene()} resolves it through
 * {@link Genes}.
 *
 * <p>{@link #token()} is the allele's text in a genotype code string. It can be
 * <b>any run of characters</b>, not a single letter - the code puts a {@code /}
 * between the two alleles of a gene and a {@code -} between genes, so
 * {@code "SB1/sb1"} is unambiguous.
 *
 * <p>{@link #order()} is the allele's index in its gene's
 * {@link Gene#alleles()} list. It exists for <b>one reason</b>: to give
 * {@link AllelePair} a canonical, stable slot order so a pair equals its
 * reverse and a genotype code round-trips. <b>It is not dominance</b> - there
 * is no such property here. Which alleles a horse carries decides its
 * {@link Expression}, and nothing about that consults this number. Carrying the
 * order on the allele (rather than looking it up through the registry) also
 * keeps {@link AllelePair} construction free of a {@link Genes} lookup, so a
 * gene can build its own tables in its constructor.
 *
 * <p>An allele carries <b>no</b> "visible" or "deterministic" hint any more -
 * those are properties of an allele <i>combination</i>, and live on
 * {@link Expression}.
 */
public final class Allele {

    private final String key;
    private final String geneKey;
    private final int order;
    private final String token;
    private final String label;

    /**
     * Created by a {@link Gene} implementation for each of its alleles.
     *
     * @param order this allele's index in {@link Gene#alleles()} - see
     *              {@link #order()}. Must match, or {@link AllelePair}'s
     *              canonical order and the gene's declared list disagree.
     */
    public Allele(String geneKey, int order, String token, String label) {
        this.geneKey = Objects.requireNonNull(geneKey, "geneKey");
        this.token = Objects.requireNonNull(token, "token");
        this.label = Objects.requireNonNull(label, "label");
        if (order < 0) {
            throw new IllegalArgumentException("allele order must be >= 0, got " + order);
        }
        this.order = order;
        this.key = geneKey + "." + token;
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

    /** Index in {@link Gene#alleles()} - a slot order for {@link AllelePair}, not dominance. */
    public int order() {
        return order;
    }

    public String token() {
        return token;
    }

    public String label() {
        return label;
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
