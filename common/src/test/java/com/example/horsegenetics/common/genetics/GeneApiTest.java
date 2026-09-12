package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.pattern.PigmentView;
import com.example.horsegenetics.common.genetics.genes.AbstractNaturalGene;
import com.example.horsegenetics.common.genetics.genes.TwoAlleleGene;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>The seam another mod extends through.</b> A third-party gene is a Java
 * object somebody else wrote, registered through a public method, and from that
 * point indistinguishable from a built-in - these tests are what says so.
 *
 * <p>Everything here registers into the real registry and unregisters in
 * {@link #reset()}. A leaked registration would lengthen the genotype code for
 * every test after it in the same JVM, which is the failure
 * {@link Genes#clearLoaded()}'s note describes.
 */
class GeneApiTest {

    @AfterEach
    void reset() {
        Genes.clearLoaded();
    }

    /** The whole of a third-party natural gene: a declaration and a paint function. */
    private static final class OtherModGene extends AbstractNaturalGene {
        OtherModGene(String key, int priority) {
            super(gene(key, priority, "Someone else's gene")
                    .variant("Sm", "Something (Sm)")
                    .recessive()
                    .hardyWeinberg(0.1)
                    .wild("Nothing happens.")
                    .carrier("carried", "Carrier", "One copy, invisible.")
                    .outcome("something", "Something", "Half the red goes."));
        }

        @Override
        protected PigmentField restrict(CoatBuildContext ctx, PigmentView coat) {
            PigmentField f = coat.mutableCopy();
            f.setRed(0, 0, f.red(0, 0) * 0.5f);
            return f;
        }
    }

    // ------------------------------------------------------------------
    // The registry
    // ------------------------------------------------------------------

    @Test
    void aThirdPartyJavaGeneRegistersAndSortsByPriority() {
        int before = Genes.codeOrder().size();
        OtherModGene gene = new OtherModGene("othermod.something", 45);
        Genes.register(gene);

        assertEquals(before + 1, Genes.codeOrder().size());
        assertSame(gene, Genes.byKey("othermod.something"));
        // Sorted into the one order by priority, not appended after the
        // built-ins: 45 lands between MATP (40) and champagne (50).
        int i = Genes.codeOrder().indexOf(gene);
        assertEquals("horsegenetics.matp", Genes.codeOrder().get(i - 1).key());
        assertEquals("horsegenetics.champagne", Genes.codeOrder().get(i + 1).key());
        assertTrue(Genes.naturalOrder().contains(gene));
        assertTrue(Genes.influencesCoat(gene), "it paints, so it is in the texture key");
    }

    @Test
    void registrationOrderDoesNotDecideGeneOrder() {
        Genes.register(new OtherModGene("bmod.late", 46));
        Genes.register(new OtherModGene("amod.early", 45));
        List<String> first = keysOf();

        Genes.clearLoaded();
        Genes.register(new OtherModGene("amod.early", 45));
        Genes.register(new OtherModGene("bmod.late", 46));

        assertEquals(first, keysOf(), "two mods loading in either order must give one code layout");
    }

    @Test
    void tiesBreakAlphabeticallySoTwoModsPickingOnePriorityStillAgree() {
        Genes.register(new OtherModGene("zmod.thing", 45));
        Genes.register(new OtherModGene("amod.thing", 45));
        int a = Genes.codeOrder().indexOf(Genes.byKey("amod.thing"));
        int z = Genes.codeOrder().indexOf(Genes.byKey("zmod.thing"));
        assertTrue(a < z, "same priority, so the key decides - and decides the same way every launch");
    }

    @Test
    void aKeyWithoutANamespaceIsRefused() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Genes.register(new OtherModGene("something", 45)));
        assertTrue(e.getMessage().contains("<modid>.<gene>"), e.getMessage());
    }

    @Test
    void aKeyWithTwoDotsOrCapitalsIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> Genes.register(new OtherModGene("a.b.c", 45)));
        assertThrows(IllegalArgumentException.class,
                () -> Genes.register(new OtherModGene("OtherMod.Thing", 45)));
        assertThrows(IllegalArgumentException.class,
                () -> Genes.register(new OtherModGene("othermod.", 45)));
    }

    @Test
    void aCollidingKeyIsRefusedRatherThanSilentlyDropped() {
        Genes.register(new OtherModGene("othermod.thing", 45));
        assertThrows(IllegalArgumentException.class,
                () -> Genes.register(new OtherModGene("othermod.thing", 46)));
    }

    @Test
    void aFrozenRegistryRefusesLateGenesAndSaysWhy() {
        Genes.freeze();
        assertTrue(Genes.isFrozen());
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> Genes.register(new OtherModGene("othermod.late", 45)));
        assertTrue(e.getMessage().contains("othermod.late"), e.getMessage());
        assertTrue(e.getMessage().contains("frozen"), e.getMessage());

        // clearLoaded thaws - it is the tests' reset, and it has to be able to
        // put the shipped gene files back.
        Genes.clearLoaded();
        assertFalse(Genes.isFrozen());
    }

    // ------------------------------------------------------------------
    // The base class
    // ------------------------------------------------------------------

    @Test
    void theDeclarationAnswersEverythingTheInterfaceAsks() {
        OtherModGene gene = new OtherModGene("othermod.thing", 45);

        assertEquals("othermod.thing", gene.key());
        assertEquals(45, gene.priority());
        assertTrue(gene.isNatural());
        assertEquals(2, gene.alleles().size());
        // The variant sorts to slot 0, which is what every "copy 0 expresses"
        // read downstream depends on.
        assertEquals(0, gene.variant.order());
        assertEquals(1, gene.wild.order());
        assertSame(gene.wild, gene.defaultAllele());
        assertEquals(3, gene.expressions().size(), "wild, carrier, outcome");
        assertTrue(gene.affectsCoat());
    }

    @Test
    void aRecessiveNeedsTwoCopiesAndOneCopyReadsAsACarrier() {
        OtherModGene gene = new OtherModGene("othermod.thing", 45);
        AllelePair both = new AllelePair(gene.variant, gene.variant);
        AllelePair one = new AllelePair(gene.variant, gene.wild);
        AllelePair none = new AllelePair(gene.wild, gene.wild);

        assertTrue(gene.expresses(both));
        assertFalse(gene.expresses(one));
        assertFalse(gene.expresses(none));
        assertTrue(gene.isCarrier(one));

        assertEquals("something", gene.expressionOf(both).id());
        assertEquals("carried", gene.expressionOf(one).id());
        assertEquals("wild", gene.expressionOf(none).id());
        // Only the outcome paints; a carrier is a wild type and stays out of
        // the texture key.
        assertFalse(gene.expressionOf(both).wildType());
        assertTrue(gene.expressionOf(one).wildType());
    }

    @Test
    void aDominantExpressesOnOneCopy() {
        TwoAlleleGene gene = new AbstractNaturalGene(
                TwoAlleleGene.gene("othermod.dom", 45, "Dominant")
                        .variant("D", "Dominant (D)")
                        .dominant()
                        .hardyWeinberg(0.1)
                        .wild("Nothing.")
                        .outcome("shown", "Shown", "Something.")) {
            @Override
            protected PigmentField restrict(CoatBuildContext ctx, PigmentView coat) {
                return coat.mutableCopy();
            }
        };
        assertTrue(gene.expresses(new AllelePair(gene.variant, gene.wild)));
        assertFalse(gene.isCarrier(new AllelePair(gene.variant, gene.wild)));
        assertEquals(2, gene.expressions().size(), "no carrier declared, so wild and outcome");
    }

    @Test
    void anIncompleteDeclarationFailsAtConstructionNotAtPaintTime() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () ->
                new AbstractNaturalGene(TwoAlleleGene.gene("othermod.broken", 45, "Broken")
                        .variant("B", "Broken (B)")
                        .recessive()) {
                    @Override
                    protected PigmentField restrict(CoatBuildContext ctx, PigmentView coat) {
                        return coat.mutableCopy();
                    }
                });
        // It names what is missing rather than NPE-ing somewhere downstream.
        assertTrue(e.getMessage().contains("othermod.broken"), e.getMessage());
    }

    @Test
    void theFounderTableCoversEveryCombinationExactlyOnce() {
        OtherModGene gene = new OtherModGene("othermod.thing", 45);
        FounderTable table = gene.founderTable(new FounderContext(Map.of(), gene));
        // Hardy-Weinberg over one allele at 0.1: the three combinations, and
        // the homozygous variant is the rarest of them.
        double both = table.share(new AllelePair(gene.variant, gene.variant));
        double one = table.share(new AllelePair(gene.variant, gene.wild));
        double none = table.share(new AllelePair(gene.wild, gene.wild));
        assertTrue(both > 0 && one > both && none > one,
                "q^2 < 2pq < p^2 at q = 0.1, got " + both + " / " + one + " / " + none);
    }

    // ------------------------------------------------------------------

    @Test
    void aThirdPartyGeneIsRolledOntoAFounderLikeAnyOther() {
        OtherModGene gene = new OtherModGene("othermod.thing", 45);
        Genes.register(gene);

        // Not asserting which combination - asserting the locus is rolled at
        // all, which is the whole of "indistinguishable from a built-in".
        Genotype founder = Genotype.random(new SeededRng(1));
        AllelePair pair = founder.pair(gene);
        assertNotEquals(null, pair);
        assertTrue(pair.count(gene.variant) + pair.count(gene.wild) == 2,
                "both copies are this gene's own alleles");
    }

    private static List<String> keysOf() {
        List<String> out = new ArrayList<>();
        for (Gene g : Genes.codeOrder()) {
            out.add(g.key());
        }
        return out;
    }
}
