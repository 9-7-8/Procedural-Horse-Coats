package com.example.horsegenetics.common.horse;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * <b>What genetics a particular set of horses actually contains.</b> Given the
 * rows behind the browser's <i>My horses</i> tab, which genes appear in them
 * and which alleles of each - so a picker can offer the player <b>their own</b>
 * genetics rather than the whole registry.
 *
 * <h2>Why "present" means "not wild type"</h2>
 * Every horse carries every registered gene, so a naive "which genes are here"
 * answers <i>all of them</i> - two hundred-odd entries, of which the overwhelming
 * majority are wild type on every horse the player owns. That is a list nobody
 * can use. A gene counts as present only when some horse carries an allele that
 * is <b>not</b> the gene's default, which is the same thing as "there is
 * something here worth filtering on".
 *
 * <p>The alleles offered for a gene are the non-default ones actually seen, for
 * the same reason and with a sharper edge: a player who owns one splash horse
 * should be offered {@code SW1} and not the other nine splash alleles they have
 * never met. The point of the picker is to answer "what do I have", and a list
 * padded with everything they might one day have answers a different question.
 *
 * <h2>Order</h2>
 * Genes come in {@link Genes#codeOrder()} and alleles in their gene's declared
 * order, so the picker agrees with the genotype code, the info panel and the
 * wiki rather than inventing a third arrangement. Both are stable across calls,
 * which matters because the list is rebuilt every time the roster changes and a
 * list that reshuffles under the cursor is unusable.
 *
 * <p>Game-free, like {@link HorseQuery} beside it, so the same answers can be
 * computed on the server the day the owned-horse index exists.
 */
public final class RosterGenetics {

    private RosterGenetics() {
    }

    /**
     * Every gene at least one of these horses carries a non-wild-type allele of,
     * in {@link Genes#codeOrder()}.
     */
    public static List<Gene> genesPresent(List<HorseListing> rows) {
        Set<String> keys = new LinkedHashSet<>();
        for (HorseListing row : rows) {
            Genotype genotype = row.genotype();
            if (genotype == null) {
                continue;
            }
            for (Gene gene : Genes.codeOrder()) {
                if (!keys.contains(gene.key()) && carriesVariant(genotype, gene)) {
                    keys.add(gene.key());
                }
            }
        }
        List<Gene> out = new ArrayList<>();
        for (Gene gene : Genes.codeOrder()) {
            if (keys.contains(gene.key())) {
                out.add(gene);
            }
        }
        return List.copyOf(out);
    }

    /**
     * Every non-wild-type allele of {@code gene} that appears in these horses,
     * in the gene's declared order. Empty when the gene is not present, which is
     * what lets a caller treat "no alleles" and "not present" as one case.
     */
    public static List<Allele> allelesPresent(List<HorseListing> rows, Gene gene) {
        Set<String> tokens = new LinkedHashSet<>();
        for (HorseListing row : rows) {
            Genotype genotype = row.genotype();
            if (genotype == null) {
                continue;
            }
            AllelePair pair = genotype.pair(gene);
            if (pair == null) {
                continue;
            }
            addIfVariant(tokens, gene, pair.first());
            addIfVariant(tokens, gene, pair.second());
        }
        List<Allele> out = new ArrayList<>();
        for (Allele allele : gene.alleles()) {
            if (tokens.contains(allele.token())) {
                out.add(allele);
            }
        }
        return List.copyOf(out);
    }

    private static boolean carriesVariant(Genotype genotype, Gene gene) {
        AllelePair pair = genotype.pair(gene);
        if (pair == null) {
            return false;
        }
        return isVariant(gene, pair.first()) || isVariant(gene, pair.second());
    }

    private static void addIfVariant(Set<String> into, Gene gene, Allele allele) {
        if (isVariant(gene, allele)) {
            into.add(allele.token());
        }
    }

    /**
     * Not the gene's default. A null copy is the reserved slot a hemizygous
     * locus leaves on a stallion's Y, which is an absence rather than an allele.
     */
    private static boolean isVariant(Gene gene, Allele allele) {
        return allele != null && !allele.equals(gene.defaultAllele());
    }
}
