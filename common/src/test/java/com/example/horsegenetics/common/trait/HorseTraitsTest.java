package com.example.horsegenetics.common.trait;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.GenotypeCatalog;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The trait resolver: that a horse's body really is a function of its genotype,
 * that the function is the one the genes describe, and that the non-coat genes
 * cost the coat pipeline nothing.
 */
class HorseTraitsTest {

    /** A wild-type genotype with the given pairs substituted in. */
    static Genotype with(AllelePair... pairs) {
        Genotype g = Genotype.wildType();
        for (AllelePair replacement : pairs) {
            List<AllelePair> out = new ArrayList<>();
            for (AllelePair p : g.pairs()) {
                out.add(p.geneKey().equals(replacement.geneKey()) ? replacement : p);
            }
            g = Genotype.of(out);
        }
        return g;
    }

    @Test
    void anAllWildTypeHorseIsTheBaseline() {
        Traits t = HorseTraits.resolve(Genotype.wildType());
        assertEquals(HorseTraits.BASE_SPEED, t.speed(), 1e-12);
        assertEquals(HorseTraits.BASE_HEALTH, t.health(), 1e-12);
        assertEquals(HorseTraits.BASE_JUMP, t.jump(), 1e-12);
        assertEquals(HorseTraits.BASE_SCALE, t.scale(), 1e-12);
        assertTrue(t.conditions().isEmpty());
        assertSame(Viability.VIABLE, t.viability());
    }

    /** The same alleles always give the same horse - the whole point of the change. */
    @Test
    void resolutionIsDeterministic() {
        SeededRng rng = new SeededRng(99L);
        for (int i = 0; i < 200; i++) {
            Genotype g = Genotype.random(rng);
            assertEquals(HorseTraits.resolve(g), HorseTraits.resolve(g));
            assertEquals(HorseTraits.resolve(Genotype.parse(g.toCode())), HorseTraits.resolve(g));
        }
    }

    @Test
    void speedIsTheSumOfTheThreeSpeedLoci() {
        Genotype fastest = with(
                new AllelePair(Genes.MSTN.C, Genes.MSTN.C),
                new AllelePair(Genes.PDK4.A, Genes.PDK4.A),
                new AllelePair(Genes.CKM.T, Genes.CKM.T));
        double expected = HorseTraits.BASE_SPEED
                + 2 * Genes.MSTN.SPEED_PER_C
                + 2 * Genes.PDK4.SPEED_PER_A
                + 2 * Genes.CKM.SPEED_PER_T;
        assertEquals(expected, HorseTraits.resolve(fastest).speed(), 1e-12);
    }

    /**
     * MSTN is codominant, so the heterozygote is genuinely the midpoint of the
     * two homozygotes rather than a copy of either.
     */
    @Test
    void mstnHeterozygoteIsTheMidpoint() {
        double sprint = HorseTraits.resolve(with(new AllelePair(Genes.MSTN.C, Genes.MSTN.C))).speed();
        double stay = HorseTraits.resolve(with(new AllelePair(Genes.MSTN.T, Genes.MSTN.T))).speed();
        double mid = HorseTraits.resolve(with(new AllelePair(Genes.MSTN.C, Genes.MSTN.T))).speed();
        assertEquals((sprint + stay) / 2.0, mid, 1e-12);

        double sprintHp = HorseTraits.resolve(with(new AllelePair(Genes.MSTN.C, Genes.MSTN.C))).health();
        double midHp = HorseTraits.resolve(with(new AllelePair(Genes.MSTN.C, Genes.MSTN.T))).health();
        double stayHp = HorseTraits.resolve(with(new AllelePair(Genes.MSTN.T, Genes.MSTN.T))).health();
        assertTrue(stayHp > midHp && midHp > sprintHp, "speed is bought with hearts");
        assertEquals(HorseTraits.BASE_HEALTH, stayHp, 1e-12, "the baseline allele contributes zero");
    }

    /** The two size loci pull opposite ways and compose additively. */
    @Test
    void theSizeLociAddAndCancel() {
        double tall = HorseTraits.resolve(with(new AllelePair(Genes.LCORL.L, Genes.LCORL.L))).scale();
        double pony = HorseTraits.resolve(with(new AllelePair(Genes.HMGA2.p, Genes.HMGA2.p))).scale();
        assertTrue(tall > HorseTraits.BASE_SCALE);
        assertTrue(pony < HorseTraits.BASE_SCALE);

        double both = HorseTraits.resolve(with(
                new AllelePair(Genes.LCORL.L, Genes.LCORL.L),
                new AllelePair(Genes.HMGA2.p, Genes.HMGA2.p))).scale();
        assertEquals(tall + pony - HorseTraits.BASE_SCALE, both, 1e-12);
    }

    /**
     * Dwarfism multiplies where the height loci add, so a dwarf pony really is
     * smaller than either alone and a dwarf draught horse is still a dwarf.
     */
    @Test
    void dwarfismMultipliesOnTopOfHeight() {
        double ponyScale = HorseTraits.resolve(with(new AllelePair(Genes.HMGA2.p, Genes.HMGA2.p))).scale();
        double dwarfScale = HorseTraits.resolve(with(new AllelePair(Genes.ACAN.D2, Genes.ACAN.D3))).scale();
        double both = HorseTraits.resolve(with(
                new AllelePair(Genes.HMGA2.p, Genes.HMGA2.p),
                new AllelePair(Genes.ACAN.D2, Genes.ACAN.D3))).scale();
        assertTrue(both < ponyScale && both < dwarfScale);
        assertEquals(ponyScale * (dwarfScale / HorseTraits.BASE_SCALE), both, 1e-12);
    }

    /**
     * A genetic health value must never resolve to zero - a degenerate
     * max-health attribute is a crash, not a very sick horse. Killing one is
     * the damage path's job.
     */
    @Test
    void healthNeverResolvesToZero() {
        Genotype worst = with(
                new AllelePair(Genes.ACAN.D1, Genes.ACAN.D1),
                new AllelePair(Genes.B4GALT7.variant, Genes.B4GALT7.baseline),
                new AllelePair(Genes.PLOD1.variant, Genes.PLOD1.variant),
                new AllelePair(Genes.RAPGEF5.variant, Genes.RAPGEF5.variant),
                new AllelePair(Genes.ST14.variant, Genes.ST14.variant),
                new AllelePair(Genes.SHOX.variant, Genes.SHOX.variant));
        Traits t = HorseTraits.resolve(worst);
        assertEquals(HorseTraits.MIN_HEALTH, t.health(), 1e-12);
        assertTrue(t.speed() >= HorseTraits.MIN_SPEED);
        assertTrue(t.jump() >= HorseTraits.MIN_JUMP);
        assertTrue(t.scale() >= HorseTraits.MIN_SCALE);
    }

    /**
     * The config off position stops the disorders affecting the horse without
     * touching anything else - the performance and size loci are not health
     * genetics and keep working.
     */
    @Test
    void switchingHealthGeneticsOffSuppressesOnlyTheDisorders() {
        Genotype g = with(
                new AllelePair(Genes.PLOD1.variant, Genes.PLOD1.variant),
                new AllelePair(Genes.LCORL.L, Genes.LCORL.L),
                new AllelePair(Genes.MSTN.C, Genes.MSTN.C));

        Traits on = HorseTraits.resolve(g, true);
        assertSame(Viability.LETHAL_AT_BIRTH, on.viability());

        Traits off = HorseTraits.resolve(g, false);
        assertSame(Viability.VIABLE, off.viability());
        assertTrue(off.conditions().isEmpty());
        assertEquals(HorseTraits.resolve(with(
                new AllelePair(Genes.LCORL.L, Genes.LCORL.L),
                new AllelePair(Genes.MSTN.C, Genes.MSTN.C))), off);
    }

    /**
     * None of the thirteen non-coat genes paints, so none of them widens the
     * genotype gallery or forks the texture cache - the same free ride the sex
     * locus gets, for the same reason.
     */
    @Test
    void theNonCoatGenesAreInvisibleToTheCoat() {
        for (Gene gene : List.of(
                Genes.MSTN, Genes.PDK4, Genes.CKM, Genes.RYR2, Genes.LCORL, Genes.HMGA2,
                Genes.ACAN, Genes.B4GALT7, Genes.PLOD1, Genes.RAPGEF5, Genes.ST14,
                Genes.SHOX, Genes.MET)) {
            assertFalse(gene.affectsCoat(), gene.key() + " must not paint");
            assertEquals(1, GenotypeCatalog.distinctPairsOf(gene).size(),
                    gene.key() + " must collapse to one gallery entry");
        }

        Genotype plain = Genotype.wildType();
        Genotype loaded = with(
                new AllelePair(Genes.MSTN.C, Genes.MSTN.C),
                new AllelePair(Genes.ACAN.D1, Genes.ACAN.N));
        assertNotEquals(plain.toCode(), loaded.toCode());
        assertEquals(plain.coatCode(), loaded.coatCode());
    }

    /**
     * Every gene that contributes a trait is reachable from the one walk.
     *
     * <p><b>Named, not counted.</b> This used to assert a total, which is a
     * derived number written into a test - it says nothing about <i>which</i>
     * gene went missing, and it goes red every time a gene is added for reasons
     * that have nothing to do with traits. What it is actually protecting is
     * that a gene which contributes to the body is still declaring it, so it
     * names them: the performance, size and health loci, the colour loci that
     * carry a disorder, milk's embryonic lethal, the leopard complex's CSNB, and
     * the dhampir's multipliers.
     */
    @Test
    void everyTraitGeneIsVisitedInCodeOrder() {
        List<Gene> expected = List.of(
                // performance and size
                Genes.MSTN, Genes.PDK4, Genes.CKM, Genes.RYR2, Genes.LCORL, Genes.HMGA2,
                // the health loci
                Genes.ACAN, Genes.B4GALT7, Genes.PLOD1, Genes.RAPGEF5, Genes.ST14,
                Genes.SHOX, Genes.MET,
                // colour loci that carry a disorder, milk's embryonic lethal,
                // the leopard complex's CSNB
                Genes.EDNRB, Genes.MITF, Genes.PAX3, Genes.SILVER, Genes.MILK, Genes.LEOPARD,
                // the dhampir's multiplier set. The four magical body-stat loci
                // are NOT here - they go through EpigeneticTraitContribution,
                // because their magnitude is on the allele copy rather than on
                // the allele.
                Genes.DHAMPIR);

        List<Gene> found = new ArrayList<>();
        for (Gene g : Genes.codeOrder()) {
            if (g instanceof TraitContribution) {
                found.add(g);
            }
        }
        for (Gene g : expected) {
            assertTrue(found.contains(g), g.key() + " should contribute a trait and does not");
        }
        List<String> unexpected = new ArrayList<>();
        for (Gene g : found) {
            if (!expected.contains(g)) {
                unexpected.add(g.key());
            }
        }
        assertTrue(unexpected.isEmpty(),
                "these genes contribute a trait and are not named here: " + unexpected);
    }
}
