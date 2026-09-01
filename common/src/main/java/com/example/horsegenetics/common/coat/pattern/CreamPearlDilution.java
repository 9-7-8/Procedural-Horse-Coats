package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;

/**
 * The shared Cream + Pearl calculation. Cream ({@code SLC45A2}) and Pearl are
 * <b>allelic</b> in real horses; here they are two separate genes, so this
 * class reads <i>both</i> and applies the combined dilution once. It only
 * restricts red / black pigment - a natural effect.
 *
 * <table>
 *   <tr><th>Cream</th><th>Pearl</th><th>effect</th></tr>
 *   <tr><td>0</td><td>0-1</td><td>none</td></tr>
 *   <tr><td>1</td><td>0</td><td>single cream - red hard, black mildly (smoky buckskin / palomino)</td></tr>
 *   <tr><td>0</td><td>2</td><td>double pearl - mild, uniform, both pigments</td></tr>
 *   <tr><td>1</td><td>1+</td><td>Cr/prl - acts as double cream</td></tr>
 *   <tr><td>2+</td><td>any</td><td>double cream - severe, both pigments (perlino)</td></tr>
 * </table>
 *
 * Red is always restricted more than black under a double dilution, which is
 * why a diluted bay body fades to cream while the points hold smoky colour.
 */
public final class CreamPearlDilution {

    // pigment kept (multiplied), per mode
    private static final float SINGLE_CREAM_RED = 0.45f;   // copper -> golden
    // Single cream keeps most - but not all - of the black. Bay never *adds*
    // black anywhere; the points / lower legs are just black it declined to
    // restrict, so a real pigment dilution has to reach them too (a smoky /
    // sooty buckskin), not leave them jet black.
    private static final float SINGLE_CREAM_BLACK = 0.7f;
    private static final float DOUBLE_PEARL_RED = 0.55f;
    private static final float DOUBLE_PEARL_BLACK = 0.60f;
    private static final float DOUBLE_DILUTE_RED = 0.08f;  // body -> pale cream
    private static final float DOUBLE_DILUTE_BLACK = 0.38f; // points -> smoky rust

    private CreamPearlDilution() {}

    public enum Mode { NONE, SINGLE_CREAM, DOUBLE_PEARL, DOUBLE_DILUTE }

    public static int creamDose(Genotype genotype) {
        return dose(genotype.pair(Genes.CREAM), Genes.CREAM.Cr);
    }

    public static int pearlDose(Genotype genotype) {
        return dose(genotype.pair(Genes.PEARL), Genes.PEARL.prl);
    }

    private static int dose(AllelePair pair, com.example.horsegenetics.common.genetics.Allele allele) {
        int n = 0;
        if (pair.first().equals(allele)) n++;
        if (pair.second().equals(allele)) n++;
        return n;
    }

    public static Mode mode(Genotype genotype) {
        int cr = creamDose(genotype);
        int prl = pearlDose(genotype);
        if (cr >= 2) return Mode.DOUBLE_DILUTE;
        if (cr == 1) return prl >= 1 ? Mode.DOUBLE_DILUTE : Mode.SINGLE_CREAM;
        return prl >= 2 ? Mode.DOUBLE_PEARL : Mode.NONE;
    }

    /** Apply the combined effect to {@code ctx.pigment()}. Safe to call when Mode is NONE. */
    public static void apply(CoatBuildContext ctx) {
        Mode m = mode(ctx.genotype());
        if (m == Mode.NONE) {
            return;
        }
        final float keepRed;
        final float keepBlack;
        switch (m) {
            case SINGLE_CREAM -> { keepRed = SINGLE_CREAM_RED; keepBlack = SINGLE_CREAM_BLACK; }
            case DOUBLE_PEARL -> { keepRed = DOUBLE_PEARL_RED; keepBlack = DOUBLE_PEARL_BLACK; }
            default -> { keepRed = DOUBLE_DILUTE_RED; keepBlack = DOUBLE_DILUTE_BLACK; }
        }
        CoatRegions.restrictAll(ctx.skin(), ctx.pigment(), (f, px, py, p) -> {
            f.setRed(px, py, f.red(px, py) * keepRed);
            f.setBlack(px, py, f.black(px, py) * keepBlack);
        });
    }
}
