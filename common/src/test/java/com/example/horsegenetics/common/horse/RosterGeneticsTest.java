package com.example.horsegenetics.common.horse;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>The gene and allele pickers must offer what the player has, and nothing
 * else.</b>
 *
 * <p>The failure this guards against is not a crash, it is a list nobody can
 * use: every horse carries every registered gene, so "which genes are in this
 * roster" naively answers <i>all two hundred</i>, almost all of them wild type
 * on every horse the player owns. Wild type is the floor, not a finding.
 */
class RosterGeneticsTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static HorseListing row(String name, Genotype genotype) {
        return HorseListing.of(ID, name, "Testcase", "", "Arabian", 1, genotype,
                true, true, 0, false, true, "overworld", "owner", "", false);
    }

    private static Genotype with(AllelePair... pairs) {
        Genotype g = Genotype.wildType();
        for (AllelePair pair : pairs) {
            g = g.with(pair);
        }
        return g;
    }

    private static List<String> tokens(List<Allele> alleles) {
        List<String> out = new ArrayList<>();
        for (Allele a : alleles) {
            out.add(a.token());
        }
        return out;
    }

    /** A roster of plain wild-type horses offers nothing, rather than everything. */
    @Test
    void anAllWildTypeRosterHasNoGenesToOffer() {
        List<HorseListing> rows = List.of(row("Plain", Genotype.wildType()));
        assertTrue(RosterGenetics.genesPresent(rows).isEmpty(),
                "wild type is the floor, not something to filter on");
    }

    /** An empty roster is not an error. */
    @Test
    void anEmptyRosterIsEmpty() {
        assertTrue(RosterGenetics.genesPresent(List.of()).isEmpty());
        assertTrue(RosterGenetics.allelesPresent(List.of(), Genes.EXTENSION).isEmpty());
    }

    @Test
    void onlyTheGenesSomebodyActuallyCarriesAreOffered() {
        List<HorseListing> rows = List.of(
                row("Chestnut", with(new AllelePair(Genes.EXTENSION.e, Genes.EXTENSION.e))),
                row("Bay", with(new AllelePair(Genes.AGOUTI.A, Genes.AGOUTI.A))));

        List<Gene> genes = RosterGenetics.genesPresent(rows);
        assertTrue(genes.contains(Genes.EXTENSION), "somebody is chestnut");
        assertTrue(genes.contains(Genes.AGOUTI), "somebody is bay");
        assertTrue(genes.size() < 10,
                "only the carried genes, not the whole registry - got " + genes.size());
    }

    /**
     * The sharper half: a player who owns one variant of a wide locus is offered
     * that one, not every allele the locus defines.
     */
    @Test
    void onlyTheAllelesSomebodyActuallyCarriesAreOffered() {
        List<HorseListing> rows = List.of(
                row("Chestnut", with(new AllelePair(Genes.EXTENSION.e, Genes.EXTENSION.e))));

        List<String> offered = tokens(RosterGenetics.allelesPresent(rows, Genes.EXTENSION));
        assertEquals(List.of("e"), offered,
                "one horse carrying e should offer e and nothing else");
        assertFalse(offered.contains(Genes.EXTENSION.defaultAllele().token()),
                "the wild type is never worth offering");
    }

    /** A carrier counts: the point of the picker is finding what is hidden. */
    @Test
    void aHeterozygoteCountsAsCarryingIt() {
        Genotype carrier = with(new AllelePair(Genes.EXTENSION.e, Genes.EXTENSION.defaultAllele()));
        List<HorseListing> rows = List.of(row("Carrier", carrier));

        assertTrue(RosterGenetics.genesPresent(rows).contains(Genes.EXTENSION));
        assertEquals(List.of("e"), tokens(RosterGenetics.allelesPresent(rows, Genes.EXTENSION)));
    }

    /** Two horses, two different alleles of one locus: both are offered, once each. */
    @Test
    void allelesAreGatheredAcrossTheRosterAndDeduplicated() {
        List<HorseListing> rows = List.of(
                row("A", with(new AllelePair(Genes.EXTENSION.e, Genes.EXTENSION.e))),
                row("B", with(new AllelePair(Genes.EXTENSION.e, Genes.EXTENSION.defaultAllele()))));

        assertEquals(List.of("e"), tokens(RosterGenetics.allelesPresent(rows, Genes.EXTENSION)),
                "the same allele twice is still one entry");
    }

    /** Stable order, because the list is rebuilt whenever the roster changes. */
    @Test
    void theOrderIsTheGenotypeCodeOrderAndDoesNotShuffle() {
        List<HorseListing> rows = List.of(
                row("Bay", with(new AllelePair(Genes.AGOUTI.A, Genes.AGOUTI.A))),
                row("Chestnut", with(new AllelePair(Genes.EXTENSION.e, Genes.EXTENSION.e))));

        List<Gene> first = RosterGenetics.genesPresent(rows);
        assertEquals(first, RosterGenetics.genesPresent(rows), "same input, same order");

        List<Gene> order = Genes.codeOrder();
        int previous = -1;
        for (Gene gene : first) {
            int at = order.indexOf(gene);
            assertTrue(at > previous, gene.key() + " is out of code order");
            previous = at;
        }
    }
}
