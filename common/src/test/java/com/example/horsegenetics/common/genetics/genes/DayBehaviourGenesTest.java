package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.spec.AbilityType;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The daylight twins: that they mirror the night loci allele for allele, grant
 * the day verbs rather than the night ones, and are rarer in the wild - most wild
 * horses carry nothing at either, where every wild horse carries a night allele.
 */
class DayBehaviourGenesTest {

    @Test
    void theTwinsMirrorTheNightLociTokenForToken() {
        assertEquals(tokens(Genes.MAGIC_NIGHT_TEMPER), tokens(Genes.MAGIC_DAY_TEMPER));
        assertEquals(tokens(Genes.MAGIC_NIGHT_WATCH), tokens(Genes.MAGIC_DAY_WATCH));
    }

    @Test
    void aMatchedPairGrantsTheDayVerbAndNothingElse() {
        var dayTemper = Genes.MAGIC_DAY_TEMPER;
        var agh = dayTemper.alleles().get(2);
        List<GeneAbility> a = dayTemper.abilitiesFor(new AllelePair(agh, agh), Genotype.wildType());
        assertEquals(1, a.size());
        GeneAbility.DayTemper t = assertInstanceOf(GeneAbility.DayTemper.class, a.get(0));
        assertEquals("aggressive", t.mood());
        assertEquals("hostile", t.towards());

        var dayWatch = Genes.MAGIC_DAY_WATCH;
        var wbh = dayWatch.alleles().get(4);
        assertInstanceOf(GeneAbility.DayWatch.class,
                dayWatch.abilitiesFor(new AllelePair(wbh, wbh), Genotype.wildType()).get(0));

        // two different variants show neither
        var wst = dayWatch.alleles().get(0);
        assertTrue(dayWatch.abilitiesFor(new AllelePair(wst, wbh), Genotype.wildType()).isEmpty());
    }

    @Test
    void theyAreRarerInTheWildThanTheNightLoci() {
        int dayCarriers = 0;
        int nightCarriers = 0;
        int n = 4000;
        for (long seed = 0; seed < n; seed++) {
            Genotype g = Genotype.random(new SeededRng(seed));
            AllelePair day = g.pair(Genes.MAGIC_DAY_TEMPER);
            assertFalse(!day.has(Genes.MAGIC_DAY_TEMPER.defaultAllele()),
                    "a wild horse expressed (or doubled) a day temper at seed " + seed);
            if (!day.homozygousFor(Genes.MAGIC_DAY_TEMPER.defaultAllele())) {
                dayCarriers++;
            }
            if (!g.pair(Genes.MAGIC_NIGHT_TEMPER).homozygousFor(Genes.MAGIC_NIGHT_TEMPER.defaultAllele())) {
                nightCarriers++;
            }
        }
        assertEquals(n, nightCarriers, "every wild horse carries a night temper - that is its design");
        assertEquals(MagicDayTemperGene.WILD_EACH_PERCENT * 8 / 100.0, dayCarriers / (double) n, 0.02);
    }

    @Test
    void theDayVerbsParseFromAGeneFile() {
        assertTrue(AbilityType.byName("day_temper") != null, "day_temper should be a registered verb");
        assertTrue(AbilityType.byName("day_watch") != null, "day_watch should be a registered verb");
    }

    private static List<String> tokens(Gene g) {
        return g.alleles().stream().map(a -> a.token()).toList();
    }
}
