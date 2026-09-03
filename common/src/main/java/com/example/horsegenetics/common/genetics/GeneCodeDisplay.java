package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.genetics.genes.ChampagneGene;
import com.example.horsegenetics.common.genetics.genes.CreamGene;
import com.example.horsegenetics.common.genetics.genes.MagicZebraGene;
import com.example.horsegenetics.common.genetics.genes.PearlGene;
import com.example.horsegenetics.common.genetics.genes.PinkHairGene;
import com.example.horsegenetics.common.genetics.genes.SplashGene;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A short, human-readable rendering of a {@link Genotype} for UI surfaces - much
 * terser than the full {@link Genotype#toCode()} round-trip string.
 *
 * <p>Rules:
 * <ul>
 *   <li><b>Extension then agouti</b> come first, always, their two allele
 *       tokens run together with no separator ({@code E/e} + {@code A/a} →
 *       {@code "EeAa"}).</li>
 *   <li>Every other gene is shown <b>only if it carries a non-wild-type
 *       allele</b>, space-separated, in {@link #TRAILING_ORDER} (splash, white,
 *       champagne, cream, pearl, grey, test).</li>
 *   <li>Genes that aren't present in every horse - <b>splash, champagne, cream,
 *       pearl, magic zebra, pink hair</b> - write their wild-type slot as a
 *       lowercase {@code n} ("none")
 *       when heterozygous: {@code "nSpl"}, {@code "nCh"}, {@code "nCr"},
 *       {@code "nprl"}. Homozygous is the token doubled ({@code "SplSpl"}).</li>
 *   <li>The rest (white, grey, test) print both real tokens, dominant first:
 *       {@code "Ww"}, {@code "Gg"}, {@code "Tt"}.</li>
 * </ul>
 *
 * Example: a bay with one splash, one champagne and two cream alleles →
 * {@code "EeAa nSpl nCh CrCr"}.
 */
public final class GeneCodeDisplay {

    /** Genes whose wild-type allele means "this trait is simply absent". */
    private static final Set<String> ABSENCE_WILDTYPE = Set.of(
            ChampagneGene.KEY, SplashGene.KEY, CreamGene.KEY, PearlGene.KEY,
            MagicZebraGene.KEY, PinkHairGene.KEY,
            Genes.DUN.key(), Genes.SILVER.key(), Genes.MUSHROOM.key(), Genes.ROAN.key(),
            Genes.TOBIANO.key(), Genes.FRAME.key(), Genes.SABINO.key());

    /**
     * Order the non-extension/agouti genes are listed in: white patterns first
     * (splash, white), then the dilutions (champagne, cream, pearl), then grey,
     * then the magical genes, then the diagnostic test gene.
     */
    private static final List<Gene> TRAILING_ORDER = List.of(
            Genes.SPLASH, Genes.ROAN, Genes.TOBIANO, Genes.FRAME, Genes.SABINO, Genes.WHITE,
            Genes.DUN, Genes.SILVER, Genes.MUSHROOM, Genes.CHAMPAGNE, Genes.CREAM, Genes.PEARL,
            Genes.GREY, Genes.MAGIC_ZEBRA, Genes.PINK_HAIR, Genes.TEST);

    private GeneCodeDisplay() {}

    /**
     * {@link #shortForm(Genotype)} for a code string, degrading gracefully: if
     * the string won't parse (e.g. a horse saved under an older, shorter gene
     * set) it still collapses each {@code x/y} segment to {@code xy} when
     * homozygous and joins with spaces, so the caller never has to show the raw
     * slash-and-dash form.
     */
    public static String shortForm(String code) {
        try {
            return shortForm(Genotype.parse(code));
        } catch (RuntimeException parseFailed) {
            StringBuilder sb = new StringBuilder();
            for (String segment : code.split("-")) {
                String[] alleles = segment.split("/");
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(alleles.length == 2 && alleles[0].equals(alleles[1])
                        ? alleles[0] + alleles[1]
                        : segment);
            }
            return sb.toString();
        }
    }

    public static String shortForm(Genotype genotype) {
        String head = twoTokens(genotype.pair(Genes.EXTENSION)) + twoTokens(genotype.pair(Genes.AGOUTI));

        StringBuilder rest = new StringBuilder();
        for (Gene gene : TRAILING_ORDER) {
            AllelePair pair = genotype.pair(gene);
            Allele wild = gene.wildType();
            boolean firstWild = pair.first().equals(wild);
            boolean secondWild = pair.second().equals(wild);
            if (firstWild && secondWild) {
                continue; // no variant allele - not worth showing
            }

            String token;
            if (ABSENCE_WILDTYPE.contains(gene.key())) {
                Allele variant = firstWild ? pair.second() : pair.first();
                token = (firstWild || secondWild)
                        ? "n" + variant.token()                  // heterozygous
                        : variant.token() + variant.token();     // homozygous variant
            } else {
                token = pair.first().token() + pair.second().token();
            }

            if (rest.length() > 0) {
                rest.append(' ');
            }
            rest.append(token);
        }

        return rest.length() == 0 ? head : head + " " + rest;
    }

    /**
     * {@link #shortForm(Genotype)} greedily wrapped onto at most {@code lines}
     * lines of at most {@code maxChars} characters, breaking only between
     * whole gene tokens. For the horse dimension's pen signs, where a vanilla
     * sign line holds roughly 15 characters.
     *
     * <p>Best effort: a single token longer than {@code maxChars}, or more
     * tokens than will fit, overflows the last line rather than being dropped -
     * losing part of the genotype would be worse than a sign that runs wide.
     */
    public static List<String> wrap(Genotype genotype, int lines, int maxChars) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String token : shortForm(genotype).split(" ")) {
            boolean lastLine = out.size() == lines - 1;
            if (current.length() > 0 && !lastLine && current.length() + 1 + token.length() > maxChars) {
                out.add(current.toString());
                current.setLength(0);
            }
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(token);
        }
        if (current.length() > 0) {
            out.add(current.toString());
        }
        return out;
    }

    private static String twoTokens(AllelePair pair) {
        return pair.first().token() + pair.second().token();
    }
}
