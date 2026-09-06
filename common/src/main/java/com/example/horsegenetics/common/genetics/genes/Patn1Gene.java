package com.example.horsegenetics.common.genetics.genes;

/**
 * <b>{@code PATN1}</b> ({@code horsegenetics.patn1}) - the leopard-complex
 * <b>pattern-1</b> modifier. On an {@code LP} horse it pushes the pattern
 * toward the <b>spotted</b> end: a single {@code LP} copy plus {@code PATN1}
 * gives a full-body <b>leopard</b> (a white horse with round dark spots), and
 * {@code LP/LP} plus {@code PATN1} gives a <b>fewspot</b> - nearly solid white
 * with only a handful of spots left.
 *
 * <p>It has no phenotype of its own - a {@code PATN1} carrier with no leopard
 * complex looks like any other horse - so it is a pure
 * {@link AppaloosaModifierGene}: two alleles, one wild-type outcome, and a
 * founder frequency that is zero unless the founder already rolled {@code LP}
 * at {@link LeopardGene}. The pattern itself is chosen and drawn by
 * {@link LeopardGene#expressionIn}, which reads this locus.
 *
 * <p>See {@code wiki/gene-leopard.html}.
 */
public final class Patn1Gene extends AppaloosaModifierGene {

    public static final String KEY = "horsegenetics.patn1";

    public Patn1Gene() {
        // Given LP: PATN1 is common in appaloosa lines - a bit over a third carry one copy.
        super(KEY, 93, "PATN1", "Leopard pattern-1 (PATN1)", 34.0, 4.0);
    }

    @Override
    public String name() {
        return "PATN1 (leopard pattern)";
    }
}
