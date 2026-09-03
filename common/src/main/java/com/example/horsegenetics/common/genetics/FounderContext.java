package com.example.horsegenetics.common.genetics;

import java.util.Map;

/**
 * The genotype built <b>so far</b> while a founder horse is being rolled, handed
 * to {@link Gene#founderTable} so a gene's founder distribution can depend on
 * what the horse already is - "champagne is twice as likely on a chestnut".
 *
 * <p>Founders roll their genes in {@link Genes#codeOrder()}, so a gene can only
 * ever read genes with a <b>lower priority</b> than its own. Asking about a gene
 * that has not been drawn yet is a programming error, not a value, so
 * {@link #pair} throws rather than quietly handing back a wild type that would
 * make the bug invisible.
 *
 * <p>Most genes ignore this entirely and return a constant table.
 */
public final class FounderContext {

    private final Map<String, AllelePair> drawn;
    private final Gene rolling;

    /** Built by {@link Genotype#random}; {@code drawn} is read live as the roll proceeds. */
    FounderContext(Map<String, AllelePair> drawn, Gene rolling) {
        this.drawn = drawn;
        this.rolling = rolling;
    }

    /** The gene currently being rolled - the one whose {@link Gene#founderTable} was asked for. */
    public Gene gene() {
        return rolling;
    }

    /**
     * What this founder drew at {@code gene}.
     *
     * @throws IllegalStateException if {@code gene} has not been rolled yet -
     *         i.e. it sorts at or after {@link #gene()} in
     *         {@link Genes#codeOrder()}.
     */
    public AllelePair pair(Gene gene) {
        AllelePair p = drawn.get(gene.key());
        if (p == null) {
            throw new IllegalStateException(rolling.key() + " asked for " + gene.key()
                    + ", which has not been rolled yet - a founder distribution can only read genes with a"
                    + " lower (priority, key) than its own");
        }
        return p;
    }

    /** {@link #pair(Gene)}'s answer as an {@link Expression} of that gene. */
    public Expression expressionOf(Gene gene) {
        return gene.expressionOf(pair(gene));
    }

    /** Has {@code gene} been rolled yet? For a gene that wants to degrade rather than throw. */
    public boolean isRolled(Gene gene) {
        return drawn.containsKey(gene.key());
    }
}
