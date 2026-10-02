package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.parts.PartKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link GrownParts#listed} - the words the browser designer prints for the parts it
 * cannot draw. One line per part (a pair once), the sex gate the game obeys, and a
 * foal flag that matches {@link PartKind#showsOnFoal}.
 */
class GrownPartsListedTest {

    private static final Epigenome EPI = Epigenome.fromSeed(0x5EEDL);

    private static Genotype with(Sex sex, AllelePair... pairs) {
        Genotype g = Genotype.wildType().withSex(sex);
        for (AllelePair p : pairs) {
            g = g.with(p);
        }
        return g;
    }

    @Test
    void aWildHorseListsNothing() {
        assertTrue(GrownParts.listed(Genotype.wildType(), EPI).isEmpty());
    }

    @Test
    void aHornIsOneLineAndAFoalWearsIt() {
        Genotype g = with(Sex.FEMALE, new AllelePair(Genes.UNICORN_HORN.Horn, Genes.UNICORN_HORN.Horn));
        assertEquals(List.of(new GrownParts.Listed("Unicorn horn", false)), GrownParts.listed(g, EPI));
    }

    @Test
    void aPairIsNamedOnceAndIsAdultOnly() {
        Genotype g = with(Sex.MALE,
                new AllelePair(Genes.ANTLERS.Ant, Genes.ANTLERS.n),
                new AllelePair(Genes.DRAGON_HORNS.Drg, Genes.DRAGON_HORNS.Drg));
        assertEquals(List.of(new GrownParts.Listed("Antlers", true), new GrownParts.Listed("Dragon horns", true)),
                GrownParts.listed(g, EPI));
    }

    @Test
    void stallionAntlersListOnAStallionAndNotOnAMare() {
        AllelePair antm = new AllelePair(Genes.ANTLERS.Antm, Genes.ANTLERS.Antm);
        assertEquals(List.of(new GrownParts.Listed("Antlers", true)),
                GrownParts.listed(with(Sex.MALE, antm), EPI));
        assertTrue(GrownParts.listed(with(Sex.FEMALE, antm), EPI).isEmpty());
    }

    /**
     * The editors' rows: a part gene's outcomes are all wild types, and an antlered
     * horse's antlers row used to say "no effect". A coat gene's rule is unchanged.
     */
    @Test
    void aPartGeneRowSaysWhatItGrows() {
        Gene ant = Genes.ANTLERS;
        assertTrue(EditorRules.expressing(ant, ant.expressionOf(new AllelePair(Genes.ANTLERS.Ant, Genes.ANTLERS.Ant))));
        assertTrue(!EditorRules.expressing(ant, ant.expressionOf(new AllelePair(Genes.ANTLERS.n, Genes.ANTLERS.n))));
        Gene horn = Genes.UNICORN_HORN;
        assertTrue(EditorRules.expressing(horn, horn.expressionOf(new AllelePair(Genes.UNICORN_HORN.Horn, Genes.UNICORN_HORN.Horn))));
        assertTrue(EditorRules.grows(ant) && EditorRules.grows(Genes.HORN_COLOUR));
        assertTrue(!EditorRules.grows(Genes.EXTENSION));
    }

    /** Every kind has a label, and both sides of a pair share it. */
    @Test
    void everyKindIsNamedAndPairsShareAName() {
        for (PartKind k : PartKind.values()) {
            assertTrue(!k.label().isBlank(), k.name());
        }
        assertEquals(PartKind.ANTLER_LEFT.label(), PartKind.ANTLER_RIGHT.label());
        assertEquals(PartKind.RAM_HORN_LEFT.label(), PartKind.RAM_HORN_RIGHT.label());
        assertEquals(PartKind.DRAGON_HORN_LEFT.label(), PartKind.DRAGON_HORN_RIGHT.label());
    }
}
