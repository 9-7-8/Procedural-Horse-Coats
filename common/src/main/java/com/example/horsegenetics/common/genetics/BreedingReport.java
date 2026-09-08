package com.example.horsegenetics.common.genetics;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>What the draw actually did, locus by locus.</b> The dev build's breeding
 * diagnostic: one line per gene saying what each parent held and what the foal
 * came out with.
 *
 * <p>It exists because a Mendelian draw is invisible. Everything about breeding
 * in this mod - the odds, the epigenetic copy that came along with an allele,
 * the carrot bias, sex linkage - resolves inside one call and then the only
 * evidence is a foal, which is a single sample of a distribution. A run of
 * foals that all look wrong and a run of foals that happen to look wrong are
 * the same picture. These lines are the other half.
 *
 * <p><b>Nothing here recomputes the draw.</b> It reads the three genotypes that
 * already exist, so it cannot disagree with what was bred - and it deliberately
 * does not claim which parent a copy came from unless that is unambiguous,
 * because when both parents could have supplied an allele, nothing in the
 * result records which one did.
 */
public final class BreedingReport {

    private BreedingReport() {
    }

    /** One line per registered gene, in {@link Genes#codeOrder()}. */
    public static List<String> full(Genotype dam, Genotype sire, Genotype foal) {
        List<String> out = new ArrayList<>();
        for (Gene gene : Genes.codeOrder()) {
            out.add(line(gene, dam, sire, foal));
        }
        return List.copyOf(out);
    }

    /**
     * Only the loci worth a second look: the foal expresses something, or it
     * landed on a combination neither parent has. Everything else is a locus
     * quietly passing the baseline down, which is most of them.
     */
    public static List<String> notable(Genotype dam, Genotype sire, Genotype foal) {
        List<String> out = new ArrayList<>();
        for (Gene gene : Genes.codeOrder()) {
            AllelePair f = foal.pair(gene);
            boolean expressing = !foal.expressionOf(gene).wildType();
            boolean baseline = f.homozygousFor(gene.defaultAllele());
            boolean newCombination = !f.equals(dam.pair(gene)) && !f.equals(sire.pair(gene));
            if (expressing || (!baseline && newCombination)) {
                out.add(line(gene, dam, sire, foal));
            }
        }
        return List.copyOf(out);
    }

    /**
     * {@code KIT (white spotting)  dam Sb1/n  x  sire W20/n  ->  Sb1/W20   [new]  Sabino}
     * - the parents' pairs, the foal's, and what it means, in that order because
     * that is the order the question is asked in.
     */
    public static String line(Gene gene, Genotype dam, Genotype sire, Genotype foal) {
        AllelePair d = dam.pair(gene);
        AllelePair s = sire.pair(gene);
        AllelePair f = foal.pair(gene);
        StringBuilder sb = new StringBuilder();
        sb.append(gene.name())
                .append("  dam ").append(d.toTokens())
                .append("  x  sire ").append(s.toTokens())
                .append("  ->  ").append(f.toTokens());
        if (!f.equals(d) && !f.equals(s)) {
            sb.append("   [new]");
        }
        Expression expr = foal.expressionOf(gene);
        if (!expr.wildType()) {
            sb.append("   ").append(expr.name());
        } else if (!f.homozygousFor(gene.defaultAllele())) {
            sb.append("   carrier");
        }
        String origin = origin(gene, d, s, f);
        if (origin != null) {
            sb.append("   ").append(origin);
        }
        return sb.toString();
    }

    /**
     * Which parent supplied which copy, when that can be said at all. It can
     * only be said when exactly one parent could have supplied a given allele;
     * a copy both parents carry is left unattributed rather than guessed, since
     * the draw records nothing about it.
     */
    private static String origin(Gene gene, AllelePair dam, AllelePair sire, AllelePair foal) {
        String first = attribute(foal.first(), dam, sire);
        String second = attribute(foal.second(), dam, sire);
        if (first == null && second == null) {
            return null;
        }
        return "(" + foal.first().token() + " " + (first == null ? "?" : first)
                + ", " + foal.second().token() + " " + (second == null ? "?" : second) + ")";
    }

    private static String attribute(Allele allele, AllelePair dam, AllelePair sire) {
        boolean fromDam = dam.has(allele);
        boolean fromSire = sire.has(allele);
        if (fromDam && !fromSire) {
            return "dam";
        }
        if (fromSire && !fromDam) {
            return "sire";
        }
        return null;
    }
}
