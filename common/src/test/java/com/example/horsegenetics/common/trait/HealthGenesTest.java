package com.example.horsegenetics.common.trait;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.GenotypeCatalog;
import com.example.horsegenetics.common.genetics.genes.RecessiveDisorderGene;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.example.horsegenetics.common.trait.HorseTraitsTest.with;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The health loci: that a wild horse is never affected, that two carriers throw
 * an affected foal at the Mendelian rate, and that the two kinds of lethal stay
 * apart.
 */
class HealthGenesTest {

    /** The six simple recessive disorders, in code order. */
    private static final List<RecessiveDisorderGene> DISORDERS = List.of(
            Genes.B4GALT7, Genes.PLOD1, Genes.RAPGEF5, Genes.ST14, Genes.SHOX, Genes.MET);

    /**
     * <b>A wild-caught horse is an adult that survived.</b> No founder table
     * lists an affected combination, so no founder is ever affected - and the
     * only way to see any of these disorders is to have bred for it by
     * accident.
     */
    @Test
    void noFounderIsEverAffected() {
        SeededRng rng = new SeededRng(4242L);
        int carriers = 0;
        for (int i = 0; i < 40_000; i++) {
            Genotype g = Genotype.random(rng);
            Traits t = HorseTraits.resolve(g);
            assertSame(Viability.VIABLE, t.viability(),
                    "a founder was born lethal: " + g.toCode());
            for (Condition c : t.conditions()) {
                assertSame(Severity.INFORMATIONAL, worstOf(c),
                        "a founder was born impaired by something other than a colour gene: " + c);
            }
            for (RecessiveDisorderGene gene : DISORDERS) {
                assertFalse(gene.isAffected(g.pair(gene)), gene.key() + " affected a founder");
                if (gene.isCarrier(g.pair(gene))) {
                    carriers++;
                }
            }
            assertFalse(Genes.ACAN.isAffected(g.pair(Genes.ACAN)), "ACAN affected a founder");
            assertFalse(g.pair(Genes.EDNRB).homozygousFor(Genes.EDNRB.O), "lethal white in a founder");
        }
        assertTrue(carriers > 0, "carriers have to exist in the wild or nothing can ever surface");
    }

    /**
     * Only the colour genes carry anything a founder can express, and only
     * deafness and MCOA - the first costs nothing and the second is one of
     * silver's own consequences.
     */
    private static Severity worstOf(Condition c) {
        return c.severity() == Severity.IMPAIRING && c.id().equals("mcoa")
                ? Severity.INFORMATIONAL
                : c.severity();
    }

    /** Two carriers, one in four foals affected - the ordinary Mendelian rate. */
    @Test
    void twoCarriersThrowAnAffectedFoalOneTimeInFour() {
        for (RecessiveDisorderGene gene : DISORDERS) {
            Genotype carrier = with(new AllelePair(gene.variant, gene.baseline));
            SeededRng rng = new SeededRng(7L);
            int affected = 0;
            int n = 20_000;
            for (int i = 0; i < n; i++) {
                if (gene.isAffected(carrier.breedWith(carrier, rng).pair(gene))) {
                    affected++;
                }
            }
            double rate = affected / (double) n;
            assertTrue(Math.abs(rate - 0.25) < 0.02,
                    gene.key() + " threw affected foals at " + rate + ", expected about 0.25");
        }
    }

    /** A carrier bred to a clear horse never produces an affected foal. */
    @Test
    void aCarrierBredToAClearHorseIsSafe() {
        for (RecessiveDisorderGene gene : DISORDERS) {
            Genotype carrier = with(new AllelePair(gene.variant, gene.baseline));
            Genotype clear = Genotype.wildType();
            SeededRng rng = new SeededRng(11L);
            for (int i = 0; i < 5_000; i++) {
                assertFalse(gene.isAffected(carrier.breedWith(clear, rng).pair(gene)),
                        gene.key() + " produced an affected foal from a clear parent");
            }
        }
    }

    /**
     * The two kinds of lethal are genuinely different code paths, and the
     * catalogue is where the difference shows: overo lethal white is
     * <b>born</b>, so it occurs and gets a gallery pen; MET never implants, so
     * it does not occur at all.
     */
    @Test
    void theTwoKindsOfLethalAreDistinguished() {
        Traits lethalWhite = HorseTraits.resolve(
                with(new AllelePair(Genes.EDNRB.O, Genes.EDNRB.O)));
        assertSame(Viability.LETHAL_AT_BIRTH, lethalWhite.viability());
        assertTrue(Genes.EDNRB.canOccur(new AllelePair(Genes.EDNRB.O, Genes.EDNRB.O)));

        Traits embryonic = HorseTraits.resolve(
                with(new AllelePair(Genes.MET.variant, Genes.MET.variant)));
        assertSame(Viability.LETHAL_AT_CONCEPTION, embryonic.viability());
        assertFalse(Genes.MET.canOccur(new AllelePair(Genes.MET.variant, Genes.MET.variant)));
        assertEquals(2, GenotypeCatalog.allPairsOf(Genes.MET).size(),
                "met/met must not be enumerated - no horse is ever born with it");
    }

    /** Every lethal names what killed it, so the chat line is never a blank. */
    @Test
    void everyLethalNamesItsCause() {
        List<Genotype> lethals = List.of(
                with(new AllelePair(Genes.EDNRB.O, Genes.EDNRB.O)),
                with(new AllelePair(Genes.MET.variant, Genes.MET.variant)),
                with(new AllelePair(Genes.PLOD1.variant, Genes.PLOD1.variant)),
                with(new AllelePair(Genes.RAPGEF5.variant, Genes.RAPGEF5.variant)),
                with(new AllelePair(Genes.ST14.variant, Genes.ST14.variant)),
                with(new AllelePair(Genes.SHOX.variant, Genes.SHOX.variant)),
                with(new AllelePair(Genes.ACAN.D1, Genes.ACAN.D1)));
        for (Genotype g : lethals) {
            Traits t = HorseTraits.resolve(g);
            assertTrue(t.lethal(), g.toCode() + " should be lethal");
            assertTrue(t.lethalCondition().isPresent(), "a lethal must say what killed it");
            assertFalse(t.lethalCondition().get().name().isBlank());
            assertFalse(t.lethalCondition().get().description().isBlank());
        }
    }

    /**
     * ACAN is the locus where "affected" is not "homozygous": a compound
     * heterozygote has no working copy either, so two carriers of
     * <i>different</i> variants are exactly as dangerous as two of the same.
     */
    @Test
    void acanAffectsEveryCombinationWithNoWorkingCopy() {
        List<Allele> broken = List.of(Genes.ACAN.D1, Genes.ACAN.D2, Genes.ACAN.D3, Genes.ACAN.D4);
        for (Allele a : broken) {
            for (Allele b : broken) {
                AllelePair pair = new AllelePair(a, b);
                assertTrue(Genes.ACAN.isAffected(pair), pair.toTokens() + " has no working copy");
                Traits t = HorseTraits.resolve(with(pair));
                assertTrue(t.scale() < HorseTraits.BASE_SCALE, "a dwarf is smaller");
                assertTrue(t.health() < HorseTraits.BASE_HEALTH, "a dwarf has fewer hearts");
            }
            AllelePair carrier = new AllelePair(a, Genes.ACAN.N);
            assertFalse(Genes.ACAN.isAffected(carrier));
            assertTrue(Genes.ACAN.isCarrier(carrier));
            assertEquals(HorseTraits.baseline(), HorseTraits.resolve(with(carrier)),
                    "a carrier is an ordinary horse");
        }
        // ...and only D1/D1 is the lethal form.
        assertSame(Viability.LETHAL_AT_BIRTH,
                HorseTraits.resolve(with(new AllelePair(Genes.ACAN.D1, Genes.ACAN.D1))).viability());
        assertSame(Viability.VIABLE,
                HorseTraits.resolve(with(new AllelePair(Genes.ACAN.D1, Genes.ACAN.D4))).viability());
    }

    /** Deafness is one condition with two causes, and is reported once. */
    @Test
    void bothSplashLociReportTheSameDeafnessOnce() {
        Traits both = HorseTraits.resolve(with(
                new AllelePair(Genes.MITF.SW1, Genes.MITF.SW1),
                new AllelePair(Genes.PAX3.SW2, Genes.PAX3.SW2)));
        long deafness = both.conditions().stream()
                .filter(c -> c.id().equals("splash-deafness")).count();
        assertEquals(1, deafness);
        assertSame(Viability.VIABLE, both.viability(), "deafness costs the horse nothing");
    }

    /** Silver's ocular defect is homozygous-only - the coat allele hides it. */
    @Test
    void mcoaIsHomozygousSilverOnly() {
        assertTrue(HorseTraits.resolve(with(new AllelePair(Genes.SILVER.Z, Genes.SILVER.z)))
                .conditions().isEmpty());
        Traits homo = HorseTraits.resolve(with(new AllelePair(Genes.SILVER.Z, Genes.SILVER.Z)));
        assertEquals(1, homo.conditions().size());
        assertTrue(homo.health() < HorseTraits.BASE_HEALTH);
    }

    /** Every disorder gene declares a carrier outcome with real wording to show. */
    @Test
    void everyDisorderHasCarrierWordingWorthShowing() {
        List<Gene> health = List.of(
                Genes.ACAN, Genes.B4GALT7, Genes.PLOD1, Genes.RAPGEF5,
                Genes.ST14, Genes.SHOX, Genes.MET);
        for (Gene gene : health) {
            boolean carrier = gene.expressions().stream()
                    .anyMatch(e -> e.id().endsWith("-carrier") && !e.description().isBlank());
            assertTrue(carrier, gene.key() + " needs carrier wording - it is the whole point");
        }
    }
}
