package com.example.horsegenetics.common.horse;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.genes.EggLayerGene;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.testutil.FakeRng;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Horse Stasis Bank's drop buffer, minus the bank.
 *
 * <p>Two things are worth pinning here. The first is that a shelved horse
 * produces on <b>the same rule a live one does</b> - the gene's own
 * {@code produce} ability, its own interval, its own item - so the bank can
 * never become a second, better place to keep a hen-gened horse. The second is
 * the refusal: a condition a chamber cannot answer must stop production rather
 * than be guessed at, because a gene that behaved one way in a field and
 * another in a bank would be a bug nobody could see.
 */
class StasisProduceTest {

    /** Ticks an egg-layer waits, with no epigenome - the midpoint of its schema. */
    private static final long MIDPOINT_INTERVAL =
            Math.round((EggLayerGene.MIN_INTERVAL + EggLayerGene.MAX_INTERVAL) / 2.0);

    private static Genotype layer(int variant) {
        Allele allele = Genes.EGG_LAYER.variants().get(variant).allele();
        return Genotype.wildType().withSex(Sex.FEMALE).with(new AllelePair(allele, allele));
    }

    private static Map<String, Long> stamps(String geneKey, long at) {
        Map<String, Long> out = new HashMap<>();
        out.put(StasisProduce.key(geneKey), at);
        return out;
    }

    /** No epigenome, so the interval is the midpoint and the test is not a dice roll. */
    private static List<StasisProduce.Yield> due(Genotype genotype, long now, Map<String, Long> stamps) {
        return StasisProduce.due(genotype, null, Sex.FEMALE, true, now, stamps, new FakeRng());
    }

    @Test
    void anOrdinaryHorseOwesNothing() {
        assertTrue(due(Genotype.wildType(), 100_000L, Map.of()).isEmpty());
    }

    /**
     * <b>A chamber that has never been opened is due at once</b>, which is
     * deliberate and is what a live horse does too: its cooldown lives in a map
     * that is empty after every restart. A horse shelved with an egg pending
     * does not lose it.
     */
    @Test
    void aLayerWithNoStampIsDueStraightAway() {
        List<StasisProduce.Yield> owed = due(layer(0), 0L, Map.of());
        assertEquals(1, owed.size());
        assertEquals("minecraft:egg", owed.get(0).item());
        assertEquals(1, owed.get(0).count());
        assertEquals(EggLayerGene.KEY, owed.get(0).geneKey());
    }

    /** ...and is not due again until its own interval has passed. */
    @Test
    void aStampedLayerWaitsItsOwnInterval() {
        Map<String, Long> stamped = stamps(EggLayerGene.KEY, 500_000L);
        assertTrue(due(layer(0), 500_000L, stamped).isEmpty());
        assertTrue(due(layer(0), 500_000L + MIDPOINT_INTERVAL - 1, stamped).isEmpty());
        assertEquals(1, due(layer(0), 500_000L + MIDPOINT_INTERVAL, stamped).size());
    }

    /**
     * A stamp from the future is ready. {@code /time set} runs the clock
     * backwards, and the alternative is a horse that never produces again for
     * the rest of that world's life with nothing anywhere saying why.
     */
    @Test
    void aStampFromTheFutureIsReady() {
        assertEquals(1, due(layer(0), 1_000L, stamps(EggLayerGene.KEY, 9_000_000L)).size());
    }

    /** The eight laying alleles are eight different items, and the right one comes out. */
    @Test
    void eachLayingAlleleProducesItsOwnItem() {
        assertEquals("minecraft:feather", due(layer(1), 0L, Map.of()).get(0).item());
        assertEquals("minecraft:leather", due(layer(7), 0L, Map.of()).get(0).item());
    }

    /** One laying copy and one wild-type is a carrier, and a carrier lays nothing. */
    @Test
    void aCarrierLaysNothing() {
        Allele egg = Genes.EGG_LAYER.variants().get(0).allele();
        Genotype carrier = Genotype.wildType()
                .with(new AllelePair(egg, Genes.EGG_LAYER.defaultAllele()));
        assertTrue(due(carrier, 0L, Map.of()).isEmpty());
    }

    // ------------------------------------------------------------------
    // Conditions
    // ------------------------------------------------------------------

    @Test
    void theConditionsAChamberCanAnswer() {
        assertTrue(StasisProduce.holdsInStasis(GeneAbility.Condition.ALWAYS, Sex.MALE, true));
        assertTrue(StasisProduce.holdsInStasis(flag("adult"), Sex.MALE, true));
        assertFalse(StasisProduce.holdsInStasis(flag("adult"), Sex.MALE, false));
        assertTrue(StasisProduce.holdsInStasis(flag("baby"), Sex.MALE, false));
        assertTrue(StasisProduce.holdsInStasis(flag("sex_female"), Sex.FEMALE, true));
        assertFalse(StasisProduce.holdsInStasis(flag("sex_female"), Sex.MALE, true));
    }

    /**
     * <b>Everything about a place is refused, negation included.</b> "Not
     * raining" is no more a fact about a bottle than "raining" is, and a bank
     * that answered one of them would have a gene behaving differently indoors.
     */
    @Test
    void aConditionAboutTheWorldRefuses() {
        assertFalse(StasisProduce.holdsInStasis(flag("raining"), Sex.FEMALE, true));
        assertFalse(StasisProduce.holdsInStasis(
                new GeneAbility.Condition.Not(flag("raining")), Sex.FEMALE, true));
        assertFalse(StasisProduce.holdsInStasis(
                new GeneAbility.Condition.All(List.of(flag("adult"), flag("in_water"))), Sex.FEMALE, true));
        assertFalse(StasisProduce.holdsInStasis(
                new GeneAbility.Condition.Any(List.of(flag("night"), flag("day"))), Sex.FEMALE, true));
    }

    /** A negated flag it <i>can</i> answer still works both ways round. */
    @Test
    void aNegatedFlagAChamberKnowsStillWorks() {
        assertTrue(StasisProduce.holdsInStasis(
                new GeneAbility.Condition.Not(flag("baby")), Sex.FEMALE, true));
        assertTrue(StasisProduce.holdsInStasis(new GeneAbility.Condition.Flag("baby", true),
                Sex.FEMALE, true));
        assertTrue(StasisProduce.holdsInStasis(
                new GeneAbility.Condition.Any(List.of(flag("sex_male"), flag("adult"))), Sex.FEMALE, true));
    }

    private static GeneAbility.Condition flag(String name) {
        return new GeneAbility.Condition.Flag(name, false);
    }
}
