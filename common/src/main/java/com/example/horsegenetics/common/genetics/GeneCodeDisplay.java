package com.example.horsegenetics.common.genetics;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * Does {@code gene}'s wild-type allele mean "this trait is simply absent"
     * (so a heterozygote shows {@code nXxx} and a homozygote {@code XxxXxx})?
     * True for every gene except a {@link DominancePattern#COMPLETE_DOMINANT}
     * one (white, test - which print both real tokens) and grey (which prints
     * {@code Gg}). Derived, so a data-driven pattern gene is covered for free.
     */
    private static boolean absenceWildtype(Gene gene) {
        return gene.dominance() != DominancePattern.COMPLETE_DOMINANT && gene != Genes.GREY;
    }

    /**
     * Order the non-extension/agouti genes are listed in: a curated built-in
     * order (white patterns, then dilutions, then grey, then the magical genes,
     * then the diagnostic test gene), followed by any data-driven genes in
     * their {@code (priority, key)} order.
     */
    private static List<Gene> trailingOrder() {
        List<Gene> out = new ArrayList<>(List.of(
                Genes.SPLASH, Genes.ROAN, Genes.TOBIANO, Genes.FRAME, Genes.SABINO, Genes.WHITE,
                Genes.DUN, Genes.SILVER, Genes.MUSHROOM, Genes.CHAMPAGNE, Genes.CREAM, Genes.PEARL,
                Genes.GREY, Genes.MAGIC_ZEBRA, Genes.PINK_HAIR, Genes.TEST));
        for (Gene g : Genes.loaded()) {
            if (!out.contains(g)) {
                out.add(g);
            }
        }
        return out;
    }

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
                int eq = segment.indexOf('=');
                String body = eq < 0 ? segment : segment.substring(eq + 1);
                String[] alleles = body.split("/");
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(alleles.length == 2 && alleles[0].equals(alleles[1])
                        ? alleles[0] + alleles[1]
                        : body);
            }
            return sb.toString();
        }
    }

    public static String shortForm(Genotype genotype) {
        String head = twoTokens(genotype.pair(Genes.EXTENSION)) + twoTokens(genotype.pair(Genes.AGOUTI));

        StringBuilder rest = new StringBuilder();
        for (Gene gene : trailingOrder()) {
            AllelePair pair = genotype.pair(gene);
            Allele wild = gene.wildType();
            boolean firstWild = pair.first().equals(wild);
            boolean secondWild = pair.second().equals(wild);
            if (firstWild && secondWild) {
                continue; // no variant allele - not worth showing
            }

            String token;
            if (absenceWildtype(gene)) {
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
