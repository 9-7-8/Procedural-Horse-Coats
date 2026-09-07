package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Diet;
import com.example.horsegenetics.common.genetics.EyeColor;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.HorseDiet;
import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.common.trait.Traits;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The dhampir: that the carrier is exactly the eyes and nothing more, that the
 * homozygote is the whole animal, and - the one that would be silently wrong -
 * that it beats the diet locus rather than merely disagreeing with it.
 */
class DhampirGeneTest {

    private static final DhampirGene DHAMPIR = Genes.DHAMPIR;

    private static Genotype with(Allele a, Allele b) {
        return Genotype.wildType().with(new AllelePair(a, b));
    }

    private static Genotype carrier() {
        return with(DHAMPIR.Dhmp, DHAMPIR.n);
    }

    private static Genotype dhampir() {
        return with(DHAMPIR.Dhmp, DHAMPIR.Dhmp);
    }

    // ------------------------------------------------------------------
    // Three combinations, three different horses
    // ------------------------------------------------------------------

    @Test
    void theCarrierIsTheEyesAndNothingElse() {
        Genotype gt = carrier();
        // it is not a wild type - a red-eyed bay is a different horse from a bay
        assertFalse(gt.expressionOf(DHAMPIR).wildType());
        assertTrue(DHAMPIR.showsEyes(gt.pair(DHAMPIR)));
        assertFalse(DHAMPIR.isDhampir(gt.pair(DHAMPIR)));

        // ...and it is an ordinary horse in every other way
        assertEquals(HorseTraits.resolve(Genotype.wildType()), HorseTraits.resolve(gt));
        assertEquals(Diet.NORMAL, HorseDiet.resolve(gt, null).diet());
    }

    @Test
    void theHomozygoteIsWhiteAndMasksEverything() {
        assertTrue(dhampir().expressionOf(DHAMPIR).masks(),
                "a dhampir hides every other coat gene, like dominant white");
        assertTrue(DHAMPIR.affectsCoat());
        assertFalse(DHAMPIR.isNatural(), "it paints in phase 3, over the melanin genes");
    }

    @Test
    void theBodyMultipliersLandOnlyOnTheHomozygote() {
        Traits plain = HorseTraits.resolve(Genotype.wildType());
        Traits full = HorseTraits.resolve(dhampir());

        assertEquals(plain.health() * DhampirGene.HEALTH_MULTIPLIER, full.health(), 1e-9);
        assertEquals(plain.speed() * DhampirGene.SPEED_MULTIPLIER, full.speed(), 1e-9);
        assertEquals(plain.jump() * DhampirGene.JUMP_MULTIPLIER, full.jump(), 1e-9);
        assertEquals(plain.scale(), full.scale(), 1e-9, "size is not part of it");

        assertEquals(plain, HorseTraits.resolve(carrier()));
    }

    // ------------------------------------------------------------------
    // The diet channel - the ordering claim
    // ------------------------------------------------------------------

    @Test
    void aDhampirCannotBeFed() {
        assertEquals(Diet.NOTHING, HorseDiet.resolve(dhampir(), null).diet());
        assertTrue(HorseDiet.resolve(dhampir(), null).isSpecial());
    }

    /**
     * <b>The reason the diet locus sits early and the channel keeps the last
     * claim.</b> A horse that is homozygous for a diet allele <i>and</i> a
     * dhampir has two genes with an opinion, and the dhampir has to win - if
     * the channel kept the first claim instead, this horse would happily eat
     * lava and the whole cost of the gene would be gone.
     */
    @Test
    void theDhampirOverridesTheDietLocus() {
        Allele lava = Genes.DIET.alleleFor(Diet.LAVA);
        Genotype both = dhampir().with(new AllelePair(lava, lava));

        assertEquals(Diet.LAVA, HorseDiet.resolve(
                        Genotype.wildType().with(new AllelePair(lava, lava)), null).diet(),
                "the diet locus alone still says lava");
        assertEquals(Diet.NOTHING, HorseDiet.resolve(both, null).diet(),
                "the dhampir must win - it sorts after the diet locus");
        assertTrue(Genes.DIET.priority() < DHAMPIR.priority(),
                "the override only works because of the order");
    }

    // ------------------------------------------------------------------
    // The eyes
    // ------------------------------------------------------------------

    @Test
    void theRedIrisOutranksEveryPigmentClaimAndTheDepigmentedBlue() {
        Optional<EyeColor> claim = DHAMPIR.eyeColor(
                carrier().pair(DHAMPIR), carrier(), null, 0.0);
        assertTrue(claim.isPresent());
        EyeColor red = claim.get();
        assertEquals(EyeColor.RANK_MAGICAL, red.rank());
        assertTrue(EyeColor.BLUE.losesTo(red), "blue must lose to the dhampir's red");
        assertFalse(red.depigmented(), "it is paint, not an absence of pigment");

        assertTrue(DHAMPIR.eyeColor(Genotype.wildType().pair(DHAMPIR),
                Genotype.wildType(), null, 0.0).isEmpty());
    }

    // ------------------------------------------------------------------
    // Where it comes from
    // ------------------------------------------------------------------

    /** A wild-caught horse is an adult that survived; a dhampir would not have. */
    @Test
    void noFounderIsEverADhampir() {
        boolean sawCarrier = false;
        for (long seed = 0; seed < 3000; seed++) {
            AllelePair pair = Genotype.random(new SeededRng(seed)).pair(DHAMPIR);
            assertFalse(DHAMPIR.isDhampir(pair), "a founder was a dhampir at seed " + seed);
            sawCarrier |= DHAMPIR.showsEyes(pair);
        }
        assertTrue(sawCarrier, "3000 founders should turn up at least one carrier");
    }

    /**
     * The derived splice blacklist reads the <i>body</i>, and every stat this
     * gene touches goes up - so nothing there would stop a random splice
     * handing an unborn foal a horse that burns in daylight and cannot be fed.
     * That is what the manual override is for.
     */
    @Test
    void theRandomSpliceCannotRollIt() {
        assertFalse(DHAMPIR.spliceable());
        assertFalse(DHAMPIR.geneCarrotHomozygous(),
                "the known carrot hands over one copy - the animal has to be bred");
    }
}
