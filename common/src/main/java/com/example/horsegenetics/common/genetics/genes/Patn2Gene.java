package com.example.horsegenetics.common.genetics.genes;

/**
 * <b>{@code PATN2}</b> ({@code horsegenetics.patn2}) - the leopard-complex
 * <b>pattern-2</b> modifier. On an {@code LP} horse it pushes the pattern
 * toward the <b>blanket</b> end: a single {@code LP} copy plus {@code PATN2}
 * gives a <b>spotted blanket</b> (a white sheet over the hips with dark spots
 * inside it), and {@code LP/LP} plus {@code PATN2} gives a <b>snowcap</b> - the
 * same white blanket with no spots at all.
 *
 * <p>A horse carrying <i>both</i> {@code PATN1} and {@code PATN2} lands on a
 * <b>semi-leopard</b> - a spotted blanket that reaches forward over the barrel,
 * between the blanket and full-leopard looks.
 *
 * <p>Like {@link Patn1Gene} it is a silent {@link AppaloosaModifierGene}: no
 * phenotype of its own, and a founder only carries a variant copy if it also
 * rolled {@code LP}. {@code PATN2} is rarer than {@code PATN1}.
 *
 * <p>See {@code wiki/gene-leopard.html}.
 */
public final class Patn2Gene extends AppaloosaModifierGene {

    public static final String KEY = "horsegenetics.patn2";

    public Patn2Gene() {
        // Given LP: PATN2 is the rarer of the two modifiers.
        super(KEY, 94, "PATN2", "Leopard pattern-2 (PATN2)", 15.0, 1.0);
    }

    @Override
    public String name() {
        return "PATN2 (blanket pattern)";
    }
}
