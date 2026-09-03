package com.example.horsegenetics.common.genetics;

/**
 * The two alleles a horse carries at one {@link Gene} - an <b>unordered</b>
 * combination. The two slots are put in a canonical order on construction
 * (lower {@link Allele#order()} first, i.e. the gene's own
 * {@link Gene#alleles()} declaration order) so a pair equals its reverse and a
 * genotype code round-trips byte-identically.
 *
 * <p>That order is <b>bookkeeping, not biology</b> - there is no dominance in
 * this model. What the combination does to the horse is
 * {@link Gene#expressionOf(AllelePair)}, which sees both alleles and never
 * consults which one landed in which slot.
 */
public record AllelePair(Allele first, Allele second) {

    public AllelePair {
        if (!first.geneKey().equals(second.geneKey())) {
            throw new IllegalArgumentException(
                    "alleles from different genes: " + first + " / " + second);
        }
        if (second.order() < first.order()) {
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

    /** What this combination does to the horse. */
    public Expression expression() {
        return gene().expressionOf(this);
    }

    public boolean has(Allele allele) {
        return first.equals(allele) || second.equals(allele);
    }

    /**
     * How many copies of {@code allele} this horse carries: 0, 1 or 2. The
     * "dose" nearly every combination table branches on.
     */
    public int count(Allele allele) {
        int n = 0;
        if (first.equals(allele)) {
            n++;
        }
        if (second.equals(allele)) {
            n++;
        }
        return n;
    }

    public boolean homozygous() {
        return first.equals(second);
    }

    /** Both slots are {@code allele}. */
    public boolean homozygousFor(Allele allele) {
        return first.equals(allele) && second.equals(allele);
    }

    /** {@code <a>/<b>} - the pair as it appears inside a genotype code segment. */
    public String toTokens() {
        return first.token() + "/" + second.token();
    }
}
