package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.common.trait.Severity;
import com.example.horsegenetics.common.trait.Traits;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The <b>Unknown Gene Splice carrot's blacklist</b>. A player feeding a random
 * splice has chosen a surprise, not a dead foal, so the carrot may never roll a
 * locus that kills or costs hearts.
 *
 * <p>The interesting assertion is the last one: the guarantee is checked by
 * <b>resolving every genotype the carrot can actually produce</b> rather than by
 * comparing against a list of gene keys, so it keeps holding when someone adds a
 * gene.
 */
class SpliceSafetyTest {

    @Test
    void theSexLocusIsNeverSpliced() {
        assertFalse(SpliceSafety.pool().contains(Genes.SEX),
                "a carrot must never flip a foal's sex");
    }

    /**
     * Every locus that can kill a horse or take a heart off it is out. Named
     * here as a readable statement of intent - the implementation derives them.
     */
    @Test
    void nothingLethalOrHealthDamagingIsInThePool() {
        List<Gene> pool = SpliceSafety.pool();
        for (Gene g : List.of(Genes.MET, Genes.MILK, Genes.EDNRB, Genes.SILVER, Genes.MSTN,
                Genes.ACAN, Genes.B4GALT7, Genes.PLOD1, Genes.RAPGEF5, Genes.ST14, Genes.SHOX,
                Genes.MAGIC_HEALTH)) {
            assertFalse(pool.contains(g), g.key() + " can hurt a horse and must not be spliceable");
        }
    }

    /**
     * The blacklist is about <b>harm</b>, not about outcomes a player might not
     * have wanted. A pony allele and a {@code Sluggish} copy both make a slower
     * horse and neither makes a sicker one, so both stay in - a surprise is the
     * whole point of the carrot.
     */
    @Test
    void merelyUnwantedIsNotBlacklisted() {
        List<Gene> pool = SpliceSafety.pool();
        assertTrue(pool.contains(Genes.HMGA2), "a pony is slower, not damaged");
        assertTrue(pool.contains(Genes.MAGIC_SPEED), "Sluggish is a surprise, not an injury");
        assertTrue(pool.contains(Genes.MAGIC_JUMP));
    }

    /**
     * Informational conditions are kept. Splash deafness and the leopard
     * complex's night blindness cost the horse nothing, and excluding them would
     * take most of the white-pattern loci out of the carrot for no protection.
     */
    @Test
    void informationalConditionsDoNotBlacklistALocus() {
        List<Gene> pool = SpliceSafety.pool();
        assertTrue(pool.contains(Genes.MITF), "splash deafness is informational");
        assertTrue(pool.contains(Genes.PAX3));
        assertTrue(pool.contains(Genes.LEOPARD), "CSNB is informational");
    }

    /**
     * {@code KIT} stays in even though four of its homozygotes cannot occur.
     * That is the line the derivation has to draw: {@code MET} declares a lethal
     * {@code Condition} and is dropped, while {@code KIT}'s nonviable
     * combinations declare nothing and simply never exist - so sabino and
     * dominant white remain reachable.
     */
    @Test
    void kitStaysInDespiteItsNonviableHomozygotes() {
        assertTrue(SpliceSafety.pool().contains(Genes.KIT));
        assertFalse(Genes.KIT.canOccur(new AllelePair(Genes.KIT.W22, Genes.KIT.W22)));
    }

    /**
     * <b>The guarantee itself</b>, and the reason the blacklist is derived: for
     * every locus in the pool, <i>every</i> combination of it resolves to a
     * horse that is not lethal, carries nothing worse than an informational
     * condition, and has at least baseline health. This keeps holding when a
     * gene is added.
     */
    @Test
    void everyPooledLocusIsHarmlessInEveryCombination() {
        Traits baseline = HorseTraits.baseline();
        for (Gene gene : SpliceSafety.pool()) {
            for (Allele a : gene.alleles()) {
                for (Allele b : gene.alleles()) {
                    if (a.order() > b.order()) {
                        continue;
                    }
                    Traits t = HorseTraits.resolve(Genotype.wildType().with(new AllelePair(a, b)));
                    String who = gene.key() + " " + a.token() + "/" + b.token();
                    assertFalse(t.lethal(), who + " is lethal");
                    t.conditions().forEach(c -> assertTrue(c.severity() == Severity.INFORMATIONAL,
                            who + " carries " + c.severity() + " " + c.id()));
                    assertTrue(t.health() >= baseline.health(), who + " costs hearts");
                }
            }
        }
    }

    /**
     * And end to end: a thousand random splices never produce a substitution
     * that would hurt the foal.
     */
    @Test
    void aThousandRandomSplicesNeverHandOverSomethingHarmful() {
        Traits baseline = HorseTraits.baseline();
        List<CarrotEffect> splice = List.of(new CarrotEffect.GeneSplice());
        for (long seed = 0; seed < 1000; seed++) {
            GameteBias bias = CarrotEffect.fold(splice, Genotype.wildType(), new SeededRng(seed));
            bias.substitutePairs().forEach((key, pair) -> {
                Traits t = HorseTraits.resolve(Genotype.wildType().with(pair));
                assertFalse(t.lethal(), key + " -> lethal");
                assertTrue(t.health() >= baseline.health(), key + " -> fewer hearts");
            });
        }
    }
}
