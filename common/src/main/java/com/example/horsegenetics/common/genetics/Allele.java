package com.example.horsegenetics.common.genetics;

import java.util.Objects;

/**
 * One allele of one {@link Gene}. Horses carry these two-at-a-time (an
 * {@link AllelePair}) per gene, and the {@link Genotype} is the full set.
 *
 * <p>Identified by a <b>primary key</b> {@code <modauthor>.<gene>.<allele>}
 * (e.g. {@code "horsegenetics.agouti.At"}); {@link #geneKey()} is the
 * {@code <modauthor>.<gene>} prefix, and {@link #gene()} resolves it through
 * {@link Genes}. A third-party add-on would register genes under its own
 * {@code modauthor} namespace.
 *
 * <p>{@link #symbol()} is the single character this allele takes in the compact
 * genotype code string (persistence / sync / pedigree). {@link #visible()} /
 * {@link #deterministic()} are population-level hints - the authoritative
 * per-horse answers come from {@link Gene#isVisible}/{@link Gene#isDeterministic}
 * for the actual pair - used as defaults and for quick "does any allele need
 * per-horse generation" checks.
 */
public final class Allele {

    private final String key;
    private final String geneKey;
    private final char symbol;
    private final String label;
    private final boolean visible;
    private final boolean deterministic;

    /** Created by a {@link Gene} implementation for each of its alleles. */
    public Allele(String geneKey, String localId, char symbol, String label, boolean visible, boolean deterministic) {
        this.geneKey = Objects.requireNonNull(geneKey);
        this.key = geneKey + "." + localId;
        this.symbol = symbol;
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

    public char symbol() {
        return symbol;
    }

    public String label() {
        return label;
    }

    /** Does expressing this allele change the coat at all? (Wild-type alleles: false.) */
    public boolean visible() {
        return visible;
    }

    /** Is this allele's visible effect identical on every horse, or does it need epigenetics / per-horse RNG? */
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
