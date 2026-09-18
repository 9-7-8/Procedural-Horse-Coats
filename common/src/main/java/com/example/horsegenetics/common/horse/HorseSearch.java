package com.example.horsegenetics.common.horse;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * <b>Free-text search over one horse</b> - breed, gene or allele - for the
 * filter box above a dealer's string.
 *
 * <p>It exists because the cowboy's window is a list of names and prices, and a
 * name tells a player nothing about what they are buying. That was survivable
 * when a cowboy sold six horses of one breed; it is not survivable for the
 * arcane dealer, whose ten horses carry a hundred-odd magical loci between them
 * and whose whole product <i>is</i> the genes. "Which of these has galaxy" has
 * to be answerable without opening ten horses one at a time.
 *
 * <h2>What a term matches</h2>
 * Case-insensitive substrings, against everything a player might reasonably
 * type:
 * <ul>
 *   <li>the breed label - {@code Friesian}, {@code Mixed}, {@code Magical
 *       (Friesian)};</li>
 *   <li>a gene's display name and its registry key - {@code Galaxy},
 *       {@code galaxy}, {@code horsegenetics.galaxy};</li>
 *   <li>an allele's token and its label - {@code Gxy}, and whatever that copy is
 *       called in the gene file;</li>
 *   <li>the name of the expression the pair actually produces - so
 *       {@code dapple} finds the horses showing a dapple, whatever gene put it
 *       there.</li>
 * </ul>
 *
 * <p><b>Only genes the horse is carrying non-baseline copies of are matched</b>
 * on allele and expression. Every horse carries every locus, so matching wild
 * types would make {@code n} return the entire string and tell a player
 * nothing.
 *
 * <p>Several words mean <b>all of them</b>, each against any field:
 * {@code galaxy mixed} is the Mixed horses showing galaxy, not the union. That
 * is the behaviour every search box a player has ever used has, and the union
 * is not a useful question here.
 */
public final class HorseSearch {

    private HorseSearch() {
    }

    /**
     * Whether {@code record} answers {@code query}. A blank query matches
     * everything, which is what an empty box should do.
     */
    public static boolean matches(HorseRecord record, String query) {
        if (query == null || query.strip().isEmpty()) {
            return true;
        }
        if (record == null) {
            return false;
        }
        List<String> haystack = haystack(record);
        for (String term : query.toLowerCase(Locale.ROOT).strip().split("\\s+")) {
            if (!anyContains(haystack, term)) {
                return false;
            }
        }
        return true;
    }

    private static boolean anyContains(List<String> haystack, String term) {
        for (String straw : haystack) {
            if (straw.contains(term)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Every lower-cased string this horse can be found by. Built per horse per
     * query rather than cached: a dealer's string is ten animals and this runs
     * when somebody types, not on a tick.
     */
    private static List<String> haystack(HorseRecord record) {
        List<String> out = new ArrayList<>();
        add(out, record.lineage() == null ? null : record.lineage().displayName());
        add(out, record.firstName());
        add(out, record.lastName());
        record.barnName().ifPresent(name -> add(out, name));

        Genotype genotype = record.genome() == null ? null : record.genome().genotype();
        if (genotype == null) {
            return out;
        }
        for (Gene gene : Genes.codeOrder()) {
            AllelePair pair = genotype.pair(gene);
            if (pair == null || isBaseline(gene, pair)) {
                continue; // see the class note: every horse has every locus
            }
            add(out, gene.name());
            add(out, gene.key());
            add(out, pair.first().token());
            add(out, pair.second().token());
            add(out, pair.first().label());
            add(out, pair.second().label());
            Expression expression = gene.expressionOf(pair);
            if (expression != null) {
                add(out, expression.name());
            }
        }
        return out;
    }

    /** Two copies of the gene's default allele - the locus the horse may as well not have. */
    private static boolean isBaseline(Gene gene, AllelePair pair) {
        Allele baseline = gene.defaultAllele();
        return baseline != null && baseline.equals(pair.first()) && baseline.equals(pair.second());
    }

    private static void add(List<String> out, String value) {
        if (value != null && !value.isEmpty()) {
            out.add(value.toLowerCase(Locale.ROOT));
        }
    }
}
