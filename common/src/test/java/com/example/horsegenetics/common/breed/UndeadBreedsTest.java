package com.example.horsegenetics.common.breed;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.breed.spec.BreedSpecParser;
import com.example.horsegenetics.common.breed.spec.BreedSpecWriter;
import com.example.horsegenetics.common.coat.pattern.CoatTextureComposer;
import com.example.horsegenetics.common.coat.pattern.GradientLut;
import com.example.horsegenetics.common.coat.pattern.LutSet;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Diet;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.GrownParts;
import com.example.horsegenetics.common.genetics.Undeath;
import com.example.horsegenetics.common.genetics.eye.EyeHue;
import com.example.horsegenetics.common.genetics.eye.Eyes;
import com.example.horsegenetics.common.genetics.spec.HorseAbilities;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.parts.AttachedPart;
import com.example.horsegenetics.common.parts.PartKind;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two undead breeds and what makes them undead: the undeath loci, the pools a
 * vanilla undead horse converts into, the skeleton's horn count group, the black
 * eyes, and the two sheet passes in the composer. See wiki/undead-horses.html.
 */
class UndeadBreedsTest {

    private static final Breed SKELETON = Breeds.get("great_valley_skeleton_horse");
    private static final Breed ZOMBIE = Breeds.get("graveborn_warmblood");

    private static Genome roll(Breed breed, long seed) {
        return BreedFounder.roll(breed, new SeededRng(seed, "undead-test"));
    }

    @Test
    void eachBreedJoinsItsPoolAndEveryFounderExpressesItsUndeath() {
        assertEquals("skeleton", SKELETON.undeadOf());
        assertEquals("zombie", ZOMBIE.undeadOf());
        for (long seed = 0; seed < 60; seed++) {
            assertEquals(Undeath.Kind.SKELETON, Undeath.kindOf(roll(SKELETON, seed).genotype()));
            assertEquals(Undeath.Kind.ZOMBIE, Undeath.kindOf(roll(ZOMBIE, seed).genotype()));
        }
    }

    @Test
    void foundersAreTheSameForTheSameSeed() {
        assertEquals(roll(SKELETON, 7).genotypeCode(), roll(SKELETON, 7).genotypeCode());
        assertEquals(roll(ZOMBIE, 7).epigenomeCode(), roll(ZOMBIE, 7).epigenomeCode());
    }

    /** D6: one copy is a carrier - a living horse in every way that asks. */
    @Test
    void aCarrierIsNotUndead() {
        Genotype carrier = Genotype.wildType().with(new AllelePair(Genes.SKELETON.undead, Genes.SKELETON.n));
        assertFalse(Undeath.isUndead(carrier));
        assertTrue(HorseAbilities.activeFor(carrier).isEmpty()
                || HorseAbilities.activeFor(carrier).stream().noneMatch(a -> a.geneKey().equals(Genes.SKELETON.key())));
        assertFalse(Undeath.isUndead(Genotype.wildType()));
    }

    /** D29: black eyes, even on a skeleton whose hidden whites would ask for blue. */
    @Test
    void undeadEyesAreBlack() {
        for (long seed = 0; seed < 80; seed++) {
            for (Breed breed : List.of(SKELETON, ZOMBIE)) {
                Genome g = roll(breed, seed);
                var eyes = Eyes.resolve(g.genotype(), g.epigenome(), true);
                assertEquals(EyeHue.BLACK, eyes.right().iris().hue(), breed.id() + " " + seed);
                assertEquals(EyeHue.BLACK, eyes.left().iris().hue(), breed.id() + " " + seed);
                assertEquals(EyeHue.BLACK, eyes.left().sclera().hue(), breed.id() + " " + seed);
                assertFalse(eyes.left().sectoral() || eyes.right().sectoral(), breed.id() + " " + seed);
            }
        }
    }

    @Test
    void theSkeletonNeverDrownsAndEatsBones() {
        Genome g = roll(SKELETON, 3);
        assertTrue(HorseAbilities.activeFor(g.genotype(), g.epigenome()).stream()
                .anyMatch(a -> a.ability() instanceof GeneAbility.Traversal t
                        && t.flag().equals("underwater_breathing")));
        assertEquals(Genes.DIET.alleleFor(Diet.BONE), g.genotype().pair(Genes.DIET).first());
    }

    /**
     * The horn count group: about 84 founders in 100 wear no horn, and the share
     * with exactly k kinds follows the sheet (owner: 10, 5, 1.25, ~0.16 percent).
     * Every horn is bone and none glows.
     */
    @Test
    void hornsFollowTheCountGroup() {
        int n = 20_000;
        int[] byCount = new int[5];
        for (long seed = 0; seed < n; seed++) {
            Genome g = roll(SKELETON, seed);
            List<AttachedPart> parts = GrownParts.of(g.genotype(), g.epigenome());
            java.util.Set<String> kinds = new java.util.HashSet<>();
            for (AttachedPart p : parts) {
                assertFalse(p.emissive(), "a skeleton's horn glows");
                if (p.kind() == PartKind.SAIL) {
                    // The sail's bone version: bare rays, the membrane left off.
                    assertEquals(0f, p.opacity(), "a skeleton's sail has skin");
                } else {
                    assertFalse(p.translucent(), "a skeleton's antler is crystal");
                }
                assertFalse(p.blooms(), "a skeleton's antler has leaves");
                kinds.add(p.kind().label());
            }
            byCount[kinds.size()]++;
        }
        assertEquals(0.8359, byCount[0] / (double) n, 0.012);
        assertEquals(0.10, byCount[1] / (double) n, 0.008);
        assertEquals(0.05, byCount[2] / (double) n, 0.006);
        assertEquals(0.0125, byCount[3] / (double) n, 0.004);
        assertTrue(byCount[4] < n * 0.006, "all four kinds should be about 1 in 640");
    }

    @Test
    void bothHornColourLociArePinnedToBone() {
        Genome g = roll(SKELETON, 11);
        assertEquals("Bon/Bon", g.genotype().pair(Genes.HORN_COLOUR).toTokens());
        assertEquals("Bon/Bon", g.genotype().pair(Genes.DRAGON_HORN_COLOUR).toTokens());
        assertEquals("Bon/Bon", g.genotype().pair(Genes.DORSAL_SPINE_COLOUR).toTokens());
        assertEquals("Bon/Bon", g.genotype().pair(Genes.BACK_SAIL_COLOUR).toTokens());
        assertEquals("Bon/Bon", g.genotype().pair(Genes.BODY_PLATE_COLOUR).toTokens());
        assertEquals("Bon/Bon", g.genotype().pair(Genes.TUSK_COLOUR).toTokens());
    }

    private static final Breed BLACKENED = Breeds.get("blackened_skeleton_horse");

    /** A skeleton horse converting in the Nether is Blackened; anywhere else, Great Valley. */
    @Test
    void theConversionPoolPrefersABreedThatLivesThere() {
        for (long seed = 0; seed < 40; seed++) {
            assertEquals(BLACKENED, UndeadPools.pick("skeleton", "minecraft:soul_sand_valley",
                    new SeededRng(seed, "pool")));
            assertEquals(SKELETON, UndeadPools.pick("skeleton", "minecraft:plains", new SeededRng(seed, "pool")));
            assertEquals(ZOMBIE, UndeadPools.pick("zombie", "minecraft:crimson_forest", new SeededRng(seed, "pool")));
        }
        assertEquals(null, UndeadPools.pick("ghost", "minecraft:plains", new SeededRng(1, "pool")));
    }

    /** Rare overall, and the commonest horse in the Nether (owner, 2026-10-02). */
    @Test
    void blackenedIsTheCommonestNetherHorse() {
        assertTrue(BLACKENED.spawnWeight() < Commonness.UNCOMMON.weight);
        for (Breed b : Breeds.all()) {
            if (b != BLACKENED && BreedClimate.isNether(b.biomes())) {
                assertTrue(BLACKENED.spawnWeight() > b.spawnWeight(), b.id() + " out-spawns Blackened");
            }
        }
        assertTrue(BreedClimate.isNether(BLACKENED.biomes()));
        assertTrue(BLACKENED.allows(BreedSource.WILD));
    }

    @Test
    void blackenedIsAFireproofBlackSkeleton() {
        for (long seed = 0; seed < 40; seed++) {
            Genome g = roll(BLACKENED, seed);
            assertEquals(Undeath.Kind.SKELETON, Undeath.kindOf(g.genotype()));
            assertEquals("Frp/Frp", g.genotype().pair(Genes.byKey("horsegenetics.fireproof")).toTokens());
            assertEquals("a/a", g.genotype().pair(Genes.byKey("horsegenetics.agouti")).toTokens());
            assertEquals("n/n", g.genotype().pair(Genes.MAGIC_WHITE).toTokens());
            double coat = com.example.horsegenetics.common.genetics.GeneEpigenetics.forGene(Genes.SKELETON,
                    g.genotype(), g.epigenome()).expressed()
                    .get(com.example.horsegenetics.common.genetics.genes.SkeletonGene.COAT);
            assertTrue(coat >= 0.84, "coat_opacity " + coat);
        }
    }

    /** Every horn on a skeleton is bone - the ram's horns too, from the shade's bone end. */
    @Test
    void aSkeletonsRamHornsAreBone() {
        for (long seed = 0; seed < 300; seed++) {
            Genome g = roll(SKELETON, seed);
            for (AttachedPart p : GrownParts.of(g.genotype(), g.epigenome())) {
                if (p.kind().ramHorn()) {
                    int c = p.baseTint();
                    int r = (c >> 16) & 0xFF, gr = (c >> 8) & 0xFF, b = c & 0xFF;
                    assertTrue(r >= 0xEA && gr >= 0xE0 && b >= 0xC8,
                            "a skeleton's ram horn should be pale bone, got " + Integer.toHexString(c));
                }
            }
        }
    }

    /** No wild founder reaches the bone end: the wild roll is 0..1, as before. */
    @Test
    void wildRamHornsNeverRollBone() {
        for (long seed = 0; seed < 400; seed++) {
            Genome g = BreedFounder.roll(Breeds.FERAL_MIXED, new SeededRng(seed, "wild-ram"));
            double shade = com.example.horsegenetics.common.genetics.GeneEpigenetics.forGene(
                    Genes.RAM_HORNS, g.genotype(), g.epigenome()).expressed()
                    .get(com.example.horsegenetics.common.genetics.genes.RamHornsGene.SHADE);
            assertTrue(shade >= 0.0, "a wild ram horn rolled shade " + shade);
        }
    }

    @Test
    void aCountGroupRoundTripsThroughTheWriter() {
        String written = BreedSpecWriter.write(SKELETON);
        Breed again = BreedSpecParser.parse(written, "round-trip");
        assertEquals(SKELETON.countGroups(), again.countGroups());
        assertEquals(SKELETON.undeadOf(), again.undeadOf());
    }

    @Test
    void aGroupedLocusNamedTwiceIsRefused() {
        String json = "{\"id\": \"twice\", \"name\": \"Twice\", \"genes\": {\"horsegenetics.unicorn_horn\": "
                + "[{\"pair\": \"Horn/Horn\", \"weight\": 1}]}, \"count_groups\": [{\"counts\": [1, 1], "
                + "\"loci\": {\"horsegenetics.unicorn_horn\": \"Horn/Horn\"}}]}";
        assertThrows(IllegalArgumentException.class, () -> BreedSpecParser.parse(json, "twice"));
    }

    @Test
    void anUnknownPoolIsRefused() {
        String json = "{\"id\": \"wraith\", \"name\": \"Wraith\", \"undead_of\": \"ghost\"}";
        assertThrows(IllegalArgumentException.class, () -> BreedSpecParser.parse(json, "wraith"));
    }

    /** Section 2: a code saved before the undeath loci existed reads as a living horse. */
    @Test
    void aCodeFromBeforeTheUndeathLociReadsAsAlive() {
        Genotype old = Genotype.parse("horsegenetics.extension=E/e-horsegenetics.agouti=A/a");
        assertEquals("n/n", old.pair(Genes.SKELETON).toTokens());
        assertEquals("n/n", old.pair(Genes.ZOMBIE).toTokens());
        assertFalse(Undeath.isUndead(old));
    }

    // ---- the composer's two sheet passes ---------------------------------

    private static final int N = HorseSkinGeometry.SHEET_SIZE;

    /** A white template, a flat gradient, and sheets that are half cut away. */
    private static LutSet syntheticSet(int sheetRgb) {
        int[] grad = new int[16 * 16];
        java.util.Arrays.fill(grad, 0xFF886644);
        int[] sheet = new int[N * N];
        for (int i = 0; i < sheet.length; i++) {
            sheet[i] = (i % N) < N / 2 ? (0xFF000000 | sheetRgb) : 0;
        }
        Map<String, GradientLut> sheets = new HashMap<>();
        GradientLut s = new GradientLut(sheet, N, N);
        sheets.put("skeleton_adult", s);
        sheets.put("zombie_adult", s);
        return new LutSet(new GradientLut(grad, 16, 16), Map.of(), sheets);
    }

    private static int[] whiteTemplate() {
        int[] t = new int[N * N];
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> t[py * N + px] = 0xFFFFFFFF);
        return t;
    }

    @Test
    void theSheetsAlphaCutsTheCoat() {
        int[] template = whiteTemplate();
        Genotype skel = Genotype.wildType().with(new AllelePair(Genes.SKELETON.undead, Genes.SKELETON.undead));
        Epigenome epi = Epigenome.fromSeed(1L);
        int[] out = CoatTextureComposer.compose(skel, epi, Skin.ADULT, true, template, syntheticSet(0xE0E0D0));
        int cut = 0, kept = 0;
        for (int i = 0; i < out.length; i++) {
            if ((template[i] >>> 24) == 0) {
                continue;
            }
            if ((i % N) < N / 2) {
                kept += (out[i] >>> 24) != 0 ? 1 : 0;
            } else {
                assertEquals(0, out[i] >>> 24, "texel " + i + " should be cut away");
                cut++;
            }
        }
        assertTrue(cut > 0 && kept > 0);
    }

    @Test
    void aZombieWearsTheSheetNotItsNaturalColour() {
        int[] template = whiteTemplate();
        LutSet set = syntheticSet(0x406030);
        Epigenome epi = Epigenome.fromSeed(2L);
        Genotype zomb = Genotype.wildType().with(new AllelePair(Genes.ZOMBIE.undead, Genes.ZOMBIE.undead));
        int[] out = CoatTextureComposer.compose(zomb, epi, Skin.ADULT, true, template, set);
        int[] living = CoatTextureComposer.compose(Genotype.wildType(), epi, Skin.ADULT, true, template, set);
        boolean sawSheet = false;
        for (int i = 0; i < out.length; i++) {
            if ((template[i] >>> 24) != 0 && (i % N) < N / 2 && (out[i] & 0xFFFFFF) == 0x406030) {
                sawSheet = true;
            }
        }
        assertTrue(sawSheet, "an unmarked zombie on a white template is the sheet's own colour");
        assertNotEquals(java.util.Arrays.hashCode(living), java.util.Arrays.hashCode(out));
    }

    @Test
    void withNoSheetLoadedTheHorseBakesAsItsCoat() {
        int[] template = whiteTemplate();
        int[] grad = new int[16 * 16];
        java.util.Arrays.fill(grad, 0xFF886644);
        LutSet bare = LutSet.of(new GradientLut(grad, 16, 16));
        Epigenome epi = Epigenome.fromSeed(3L);
        Genotype skel = Genotype.wildType().with(new AllelePair(Genes.SKELETON.undead, Genes.SKELETON.undead));
        assertEquals(java.util.Arrays.hashCode(
                        CoatTextureComposer.compose(Genotype.wildType(), epi, Skin.ADULT, true, template, bare)),
                java.util.Arrays.hashCode(CoatTextureComposer.compose(skel, epi, Skin.ADULT, true, template, bare)));
    }

    @Test
    void theHornKindsInTheGroupAreEveryHornLikePart() {
        java.util.Set<String> grouped = new java.util.HashSet<>();
        for (Breed.GroupLocus l : SKELETON.countGroups().get(0).loci()) {
            grouped.add(l.gene());
        }
        // Every granting locus GrownParts knows - if another horn-like part ships,
        // the owner's rule is that the skeleton's pool takes it too.
        assertEquals(java.util.Set.of(Genes.UNICORN_HORN.key(), Genes.ANTLERS.key(), Genes.RAM_HORNS.key(),
                Genes.DRAGON_HORNS.key(), Genes.DORSAL_SPINES.key(), Genes.BACK_SAIL.key(),
                Genes.BODY_PLATES.key(), Genes.TUSKS.key()), grouped);
        assertEquals(PartKind.values().length, 12, "a new part kind: decide whether the skeleton grows it");
    }
}
