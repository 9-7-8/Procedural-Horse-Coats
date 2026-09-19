package com.example.horsegenetics.neoforge.compat;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>A dummy wood and a dummy ingot, and does the scan find them?</b>
 *
 * <h2>Why this test exists</h2>
 * {@link MaterialScan} is the piece of the compatibility layer most likely to be
 * quietly wrong, and its failure mode is the worst kind: a path pattern that
 * matches one mod's layout and not another's <b>finds nothing</b>, which is
 * indistinguishable from there being nothing to find. Nothing goes red, nothing
 * logs, and a player just never gets a gate.
 *
 * <p>There is a second, coarser check - {@code tools/compat/build-test-mod.mjs}
 * writes a fake mod jar and you boot the game - and it is worth keeping, because
 * it is the only thing that exercises NeoForge's real {@code JarContents} and the
 * generated pack end to end. But it costs a two-minute server boot per question,
 * so it can only ever ask two or three. This asks the awkward ones: the
 * fallbacks, the things that must <i>not</i> be picked up, and determinism.
 *
 * <p>The scan imports nothing from Minecraft or NeoForge precisely so this can
 * run as a plain unit test. If a future change makes it need a registry, that
 * change belongs in {@code ModdedMaterials} instead - or this test stops being
 * possible.
 */
class MaterialScanTest {

    private static final String OURS = "horsegenetics";

    // ------------------------------------------------------------------
    // A jar, in memory
    // ------------------------------------------------------------------

    /** Files in, {@link MaterialScan.Source} out. Insertion-ordered, so a test can shuffle it. */
    private static final class FakeJar implements MaterialScan.Source {
        private final Map<String, byte[]> files = new LinkedHashMap<>();

        FakeJar put(String path, String text) {
            files.put(path, text.getBytes(StandardCharsets.UTF_8));
            return this;
        }

        FakeJar put(String path, byte[] bytes) {
            files.put(path, bytes);
            return this;
        }

        @Override
        public byte[] read(String path) {
            return files.get(path);
        }

        @Override
        public boolean has(String path) {
            return files.containsKey(path);
        }

        @Override
        public void list(String folder, Consumer<String> paths) {
            // NeoForge's visitContent hands over the FULL jar-relative path, not
            // one relative to the folder asked for. Getting that wrong in the
            // real adapter would break every pattern in the scan, so the fake
            // has to behave the same way or the test would pass on a lie.
            for (String path : files.keySet()) {
                if (path.startsWith(folder + "/") || path.equals(folder)) {
                    paths.accept(path);
                }
            }
        }
    }

    /** A solid square, encoded as a real PNG so the scan's decoder is what reads it. */
    private static byte[] png(int size, int rgb, int alpha) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                image.setRGB(x, y, (alpha << 24) | rgb);
            }
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (Exception impossible) {
            throw new AssertionError(impossible);
        }
    }

    /** A mod with one wood (maple), one ingot (tin) and one gem (ruby). */
    private static FakeJar aModWithAWoodAndAMetal() {
        return new FakeJar()
                .put("assets/testmod/blockstates/maple_fence_gate.json",
                        "{\"variants\":{\"\":{\"model\":\"testmod:block/maple_fence_gate\"}}}")
                .put("assets/testmod/textures/block/maple_planks.png", png(16, 0xC88E5A, 255))
                // The planks ITEM, which is what a cart recipe spends and what
                // confirmWoods demands before this counts as a wood at all.
                .put("assets/testmod/items/maple_planks.json", "{}")
                .put("data/c/tags/item/ingots/tin.json",
                        "{\"values\":[\"testmod:tin_ingot\"]}")
                .put("assets/testmod/textures/item/tin_ingot.png", png(16, 0xD6D8DE, 255))
                .put("data/c/tags/item/gems/ruby.json",
                        "{\"values\":[\"testmod:ruby_gem\"]}")
                .put("assets/testmod/textures/item/ruby_gem.png", png(16, 0xC01A2B, 255));
    }

    private static MaterialScan.Result scan(MaterialScan.Source... sources) {
        return MaterialScan.of(List.of(sources), OURS);
    }

    // ------------------------------------------------------------------
    // The thing itself
    // ------------------------------------------------------------------

    @Test
    void findsADummyWoodAndPointsItAtThatModsPlanks() {
        MaterialScan.Result result = scan(aModWithAWoodAndAMetal());

        assertEquals(1, result.woods().size(), "one blockstate, one wood");
        ModdedMaterials.Wood maple = result.woods().get(0);
        assertEquals("testmod", maple.namespace());
        assertEquals("maple", maple.name());
        // Two of THEIR gate is what our recipe consumes.
        assertEquals("testmod:maple_fence_gate", maple.gateId());
        // Their planks, not ours and not vanilla's: the whole reason a modded
        // gate needs no generated art.
        assertEquals("testmod:block/maple_planks", maple.plankTexture());
        // Namespaced, so two mods adding a maple do not collide.
        assertEquals("testmod_maple_double_fence_gate", maple.doubleGateId());
    }

    @Test
    void findsADummyIngotAndADummyGemAndTellsThemApart() {
        MaterialScan.Result result = scan(aModWithAWoodAndAMetal());

        assertEquals(2, result.metals().size());
        // Sorted by id, so ruby comes before tin whatever order they were read.
        ModdedMaterials.Metal ruby = result.metals().get(0);
        ModdedMaterials.Metal tin = result.metals().get(1);

        assertEquals("testmod:ruby_gem", ruby.itemId());
        assertTrue(ruby.gem(), "c:gems/* is a gem");
        assertEquals("ruby", ruby.material(), "the _gem suffix comes off");
        assertEquals("ruby_horse_armor", ruby.armourId());

        assertEquals("testmod:tin_ingot", tin.itemId());
        assertFalse(tin.gem(), "c:ingots/* is not a gem");
        assertEquals("tin", tin.material(), "the _ingot suffix comes off");
    }

    @Test
    void readsEachMetalsColourOffItsOwnArt() {
        MaterialScan.Result result = scan(aModWithAWoodAndAMetal());

        // A solid square averages to itself, whatever the averaging does in
        // between - which is the point: it pins the round trip through
        // linear light without pinning a number nobody could check.
        assertEquals(0xC01A2B, result.metals().get(0).colour(), "ruby is its own red");
        assertEquals(0xD6D8DE, result.metals().get(1).colour(), "tin is its own grey");
    }

    @Test
    void averagesInLinearLightRatherThanOverBytes() {
        // Half black, half white. Averaged over sRGB bytes this is 0x7F7F7F;
        // averaged in linear light - correctly - it is much brighter, because
        // half the LIGHT is twice as bright as half the byte value.
        BufferedImage half = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
        half.setRGB(0, 0, 0xFF000000);
        half.setRGB(1, 0, 0xFFFFFFFF);

        int mean = MaterialScan.averageColour(half) & 0xFF;
        assertTrue(mean > 0xB0,
                "linear mean of black and white should be near 0xBC, got 0x" + Integer.toHexString(mean));
    }

    @Test
    void ignoresTransparentTexelsSoAnIconsPaddingIsNotItsColour() {
        BufferedImage padded = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                padded.setRGB(x, y, 0x00000000); // fully transparent black
            }
        }
        padded.setRGB(1, 1, 0xFFC01A2B);

        assertEquals(0xC01A2B, MaterialScan.averageColour(padded),
                "one opaque texel among transparent padding is the whole colour");
    }

    // ------------------------------------------------------------------
    // What must NOT be picked up
    // ------------------------------------------------------------------

    @Test
    void ignoresVanillaAndOurOwnNamespace() {
        MaterialScan.Result result = scan(new FakeJar()
                // Vanilla's twelve are already hand-listed in DoubleGates...
                .put("assets/minecraft/blockstates/oak_fence_gate.json", "{}")
                .put("assets/minecraft/textures/block/oak_planks.png", png(4, 0x111111, 255))
                // ...and ours is where the generated ones land, so taking it
                // would make a double gate of a double gate.
                .put("assets/" + OURS + "/blockstates/oak_double_fence_gate.json", "{}")
                // Vanilla metals are handled by name - see SaddleTint's fifteen.
                .put("data/c/tags/item/ingots/iron.json", "{\"values\":[\"minecraft:iron_ingot\"]}"));

        assertTrue(result.woods().isEmpty(), "vanilla and our own namespace are not modded woods");
        assertTrue(result.metals().isEmpty(), "vanilla metals are not modded metals");
    }

    @Test
    void ignoresTagReferencesAndAWoodWithNoTextureToWear() {
        MaterialScan.Result result = scan(new FakeJar()
                // A '#' entry is another tag, not an item.
                .put("data/c/tags/item/ingots/tin.json", "{\"values\":[\"#c:ingots/other\"]}")
                // A gate declared with no planks and no readable model: there is
                // nothing for the generated models to point at, so it is skipped
                // rather than generated as a purple cube.
                .put("assets/testmod/blockstates/ghost_fence_gate.json", "{}"));

        assertTrue(result.metals().isEmpty(), "a tag reference is not an item");
        assertTrue(result.woods().isEmpty(), "a wood with no texture cannot be drawn");
    }

    @Test
    void doesNotMistakeAModelOrARecipeForABlockstate() {
        // The path pattern is four segments with `blockstates` third. These two
        // both end in _fence_gate.json and neither is a blockstate.
        MaterialScan.Result result = scan(new FakeJar()
                .put("assets/testmod/models/block/maple_fence_gate.json", "{}")
                .put("assets/testmod/textures/block/maple_planks.png", png(4, 0x112233, 255))
                .put("data/testmod/recipe/maple_fence_gate.json", "{}"));

        assertTrue(result.woods().isEmpty(), "only a blockstate declares a wood");
    }

    @Test
    void survivesAMalformedTagFileAndKeepsTheRest() {
        MaterialScan.Result result = scan(new FakeJar()
                .put("data/c/tags/item/ingots/broken.json", "{ this is not json")
                .put("data/c/tags/item/ingots/tin.json", "{\"values\":[\"testmod:tin_ingot\"]}")
                .put("assets/testmod/textures/item/tin_ingot.png", png(4, 0xD6D8DE, 255)));

        assertEquals(1, result.metals().size(), "one bad file costs one material, not all of them");
        assertEquals("testmod:tin_ingot", result.metals().get(0).itemId());
    }

    @Test
    void givesAMetalWithNoArtTheFallbackRatherThanDroppingIt() {
        MaterialScan.Result result = scan(new FakeJar()
                .put("data/c/tags/item/ingots/tin.json", "{\"values\":[\"testmod:tin_ingot\"]}"));

        assertEquals(1, result.metals().size(), "a metal we cannot see is still a metal");
        assertEquals(MaterialScan.FALLBACK_COLOUR, result.metals().get(0).colour());
    }

    // ------------------------------------------------------------------
    // The fallbacks
    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // A gate is not a promise of a plank
    // ------------------------------------------------------------------

    /**
     * <b>The Every Slab case, in miniature.</b> That mod generates a fence and a
     * gate for every block in the game, so a 332-jar pack adopted 247 pseudo-woods
     * - {@code andesite}, {@code black_wool}, {@code coal_ore} - and registered
     * 1,488 cart items nobody could ever craft. The gate is real and its texture
     * resolves; what does not exist is {@code andesite_planks}, which is the item
     * the cart recipes go on to spend.
     */
    @Test
    void aGateWithNoPlanksItemIsNotAWood() {
        MaterialScan.Result result = scan(new FakeJar()
                .put("assets/everyslab/blockstates/andesite_fence_gate.json", "{}")
                .put("assets/everyslab/items/andesite_fence_gate.json", "{}")
                .put("assets/everyslab/models/block/andesite_fence_gate.json",
                        "{\"textures\":{\"texture\":\"minecraft:block/andesite\"}}"));

        assertTrue(result.woods().isEmpty(),
                "a fence gate says a gate can be placed, and nothing more");
    }

    /** The planks may perfectly well be in another jar, exactly as a metal's art may. */
    @Test
    void thePlanksMayBeInADifferentJarFromTheGate() {
        MaterialScan.Result result = scan(
                new FakeJar()
                        .put("assets/testmod/blockstates/maple_fence_gate.json", "{}")
                        .put("assets/testmod/textures/block/maple_planks.png", png(4, 0x112233, 255)),
                new FakeJar()
                        .put("assets/testmod/items/maple_planks.json", "{}"));

        assertEquals(1, result.woods().size(), "one jar declares the gate, another the planks");
    }

    /**
     * Only the wagon spends a stripped log. Nine real modded woods in the pack
     * shipped none, and each registered a wagon that could not be crafted.
     */
    @Test
    void aWoodWithoutAStrippedLogIsStillAWood() {
        FakeJar noLog = new FakeJar()
                .put("assets/testmod/blockstates/edelwood_fence_gate.json", "{}")
                .put("assets/testmod/items/edelwood_planks.json", "{}")
                .put("assets/testmod/textures/block/edelwood_planks.png", png(4, 0x223344, 255));

        MaterialScan.Result result = scan(noLog);
        assertEquals(1, result.woods().size(), "no stripped log is not a disqualification");
        assertFalse(result.woods().get(0).strippedLog(), "and it is recorded as absent");

        MaterialScan.Result withLog = scan(
                noLog.put("assets/testmod/items/stripped_edelwood_log.json", "{}"));
        assertTrue(withLog.woods().get(0).strippedLog(), "shipped, so the wagon can be crafted");
    }

    @Test
    void fallsBackToTheGatesOwnModelWhenThereAreNoPlanks() {
        MaterialScan.Result result = scan(new FakeJar()
                .put("assets/testmod/blockstates/ash_fence_gate.json", "{}")
                .put("assets/testmod/items/ash_planks.json", "{}")
                .put("assets/testmod/models/block/ash_fence_gate.json",
                        "{\"textures\":{\"texture\":\"testmod:block/ash_bespoke\"}}"));

        assertEquals(1, result.woods().size());
        assertEquals("testmod:block/ash_bespoke", result.woods().get(0).plankTexture());
    }

    @Test
    void prefersPlanksOverTheGatesOwnSheet() {
        // Vanilla's bamboo gate has a bespoke sheet drawn for ITS rail positions,
        // and sampling it scrambled - planks are drawn for no rail positions at
        // all and therefore fit any. The shipped twelve make the same call.
        MaterialScan.Result result = scan(new FakeJar()
                .put("assets/testmod/blockstates/bamboo_fence_gate.json", "{}")
                .put("assets/testmod/items/bamboo_planks.json", "{}")
                .put("assets/testmod/textures/block/bamboo_planks.png", png(4, 0x445566, 255))
                .put("assets/testmod/models/block/bamboo_fence_gate.json",
                        "{\"textures\":{\"texture\":\"testmod:block/bamboo_bespoke\"}}"));

        assertEquals("testmod:block/bamboo_planks", result.woods().get(0).plankTexture());
    }

    @Test
    void readsAnItemsArtThroughItsModelWhenTheNameDoesNotMatch() {
        MaterialScan.Result result = scan(new FakeJar()
                .put("data/c/tags/item/ingots/tin.json", "{\"values\":[\"testmod:tin_ingot\"]}")
                // The item is tin_ingot but the art is called something else -
                // only the model knows, which is why the model is asked first.
                .put("assets/testmod/models/item/tin_ingot.json",
                        "{\"textures\":{\"layer0\":\"testmod:item/metals/tin\"}}")
                .put("assets/testmod/textures/item/metals/tin.png", png(4, 0xD6D8DE, 255)));

        assertEquals(0xD6D8DE, result.metals().get(0).colour());
    }

    @Test
    void findsAMetalDeclaredInOneJarAndDrawnInAnother() {
        // A mod may perfectly well put another mod's item in a common tag, and
        // the art lives with whoever registered the item.
        MaterialScan.Source declarer = new FakeJar()
                .put("data/c/tags/item/ingots/tin.json", "{\"values\":[\"other:tin_ingot\"]}");
        MaterialScan.Source artist = new FakeJar()
                .put("assets/other/textures/item/tin_ingot.png", png(4, 0xD6D8DE, 255));

        assertEquals(0xD6D8DE, scan(declarer, artist).metals().get(0).colour());
        // ...and the other way round, because the tag pass completes before any
        // colour is read.
        assertEquals(0xD6D8DE, scan(artist, declarer).metals().get(0).colour());
    }

    @Test
    void findsAModdedDyeSeparatelyFromTheMetals() {
        MaterialScan.Result result = scan(new FakeJar()
                .put("data/c/tags/item/dyes/mauve.json", "{\"values\":[\"testmod:mauve_dye\"]}")
                .put("assets/testmod/textures/item/mauve_dye.png", png(4, 0x915F6D, 255)));

        assertTrue(result.metals().isEmpty(), "a dye is not a fitting material");
        assertEquals(Map.of("testmod:mauve_dye", 0x915F6D), result.dyes());
    }

    // ------------------------------------------------------------------
    // Load order can never matter - philosophy 7
    // ------------------------------------------------------------------

    @Test
    void givesTheSameAnswerWhateverOrderTheJarsCameIn() {
        List<MaterialScan.Source> jars = new ArrayList<>(List.of(
                aModWithAWoodAndAMetal(),
                new FakeJar()
                        .put("assets/aaa/blockstates/ash_fence_gate.json", "{}")
                        .put("assets/aaa/textures/block/ash_planks.png", png(4, 0x111111, 255))
                        .put("data/c/tags/item/ingots/zinc.json", "{\"values\":[\"aaa:zinc_ingot\"]}")
                        .put("assets/aaa/textures/item/zinc_ingot.png", png(4, 0x222222, 255))));

        MaterialScan.Result forwards = MaterialScan.of(jars, OURS);
        List<MaterialScan.Source> backwards = new ArrayList<>(jars);
        java.util.Collections.reverse(backwards);
        MaterialScan.Result reversed = MaterialScan.of(backwards, OURS);

        assertEquals(forwards.woods(), reversed.woods());
        assertEquals(forwards.metals(), reversed.metals());
        // The fingerprint is the thing that actually has to be stable: it is what
        // decides whether the generated pack is rebuilt, so an order-dependent
        // one would rewrite the folder on every launch.
        assertEquals(forwards.fingerprint(), reversed.fingerprint());
    }

    @Test
    void theFingerprintMovesWhenTheContentDoesAndNotOtherwise() {
        String before = scan(aModWithAWoodAndAMetal()).fingerprint();
        assertEquals(before, scan(aModWithAWoodAndAMetal()).fingerprint(),
                "same mods, same answer - this is what stops the pack being rewritten every launch");

        // A mod removed.
        assertNotEquals(before, scan(new FakeJar()).fingerprint());

        // Only the ART changed - same ids, different colour. The armour is
        // redrawn for this, so the fingerprint has to notice it.
        FakeJar repainted = aModWithAWoodAndAMetal();
        repainted.put("assets/testmod/textures/item/tin_ingot.png", png(16, 0x3A5F2B, 255));
        assertNotEquals(before, scan(repainted).fingerprint(),
                "a repainted ingot needs a repainted armour");
    }

    @Test
    void anEmptyPackOfModsFindsNothingAndSaysSoStably() {
        MaterialScan.Result empty = scan();
        assertTrue(empty.woods().isEmpty());
        assertTrue(empty.metals().isEmpty());
        assertTrue(empty.dyes().isEmpty());
        // The overwhelmingly common case, and it must be stable: GeneratedPack
        // compares against it to decide there is nothing to write.
        assertEquals(empty.fingerprint(), scan().fingerprint());
    }

    // ------------------------------------------------------------------
    // The record's own naming rules
    // ------------------------------------------------------------------

    @Test
    void stripsTheMaterialSuffixItActuallyHas() {
        assertEquals("tin", new ModdedMaterials.Metal("m:tin_ingot", 0, false).material());
        assertEquals("ruby", new ModdedMaterials.Metal("m:ruby_gem", 0, true).material());
        assertEquals("amethyst", new ModdedMaterials.Metal("m:amethyst_shard", 0, true).material());
        assertEquals("aether", new ModdedMaterials.Metal("m:aether_crystal", 0, true).material());
        // No suffix at all: the whole path is the material.
        assertEquals("obsidian", new ModdedMaterials.Metal("m:obsidian", 0, false).material());
    }

    @Test
    void stripsAnAffixAtTheFrontToo() {
        // bloodmagic:ingot_hellforged, from the pack that took v0.5.012 down.
        // Only the suffix was stripped, so this was sold as "Ingot Hellforged
        // Horse Armor".
        assertEquals("hellforged", new ModdedMaterials.Metal("m:ingot_hellforged", 0, false).material());
        assertEquals("tin", new ModdedMaterials.Metal("m:ingot_tin", 0, false).material());
        assertEquals("ruby", new ModdedMaterials.Metal("m:gem_ruby", 0, true).material());
        // Stripping either end leaves nothing, so neither end is stripped: an
        // empty material is the id "_horse_armor".
        assertEquals("ingot", new ModdedMaterials.Metal("m:ingot", 0, false).material());
        assertEquals("ingot_", new ModdedMaterials.Metal("m:ingot_", 0, false).material());
    }

    // ------------------------------------------------------------------
    // One armour per material name
    // ------------------------------------------------------------------

    private static ModdedMaterials.Metal metal(String id) {
        return new ModdedMaterials.Metal(id, 0, false);
    }

    @Test
    void collapsesTwoModsSameMetalOntoOneArmour() {
        // The crash, in four lines. v0.5.012 registered one armour per metal and
        // these two both answer "redstone_alloy_horse_armor", which is a
        // Duplicate registration and an ExceptionInInitializerError out of the
        // mod constructor - the whole mod gone, on somebody else's server.
        List<ModdedMaterials.Metal> metals = List.of(
                metal("enderio:redstone_alloy_ingot"),
                metal("energizedpower:redstone_alloy_ingot"));

        List<ModdedMaterials.Metal> armours = MaterialScan.armourMetals(metals);

        assertEquals(1, armours.size(), "one steel, one armour");
        assertEquals("enderio:redstone_alloy_ingot", armours.get(0).itemId(),
                "the lowest id wins, so it does not depend on load order");
        // ...and neither mod's ingot becomes a dead end: both forge it.
        assertEquals(2, MaterialScan.metalsFor(metals, "redstone_alloy_horse_armor").size());
    }

    @Test
    void collapsesOneModsOwnCrystalAndIngotOntoOneArmour() {
        // ExtendedAE ships entro_crystal AND entro_ingot, and both strip to
        // "entro". Worth its own test because it shows the bug needs no
        // 300-mod pack to reproduce - and because namespacing the id, which is
        // the obvious other fix, would NOT have caught this one.
        List<ModdedMaterials.Metal> metals = List.of(
                new ModdedMaterials.Metal("extendedae:entro_crystal", 0, true),
                new ModdedMaterials.Metal("extendedae:entro_ingot", 0, false));

        assertEquals(1, MaterialScan.armourMetals(metals).size(),
                "one material name, one armour, even from a single mod");
    }

    @Test
    void leavesUncollidingMetalsAloneAndKeepsThemInOrder() {
        List<ModdedMaterials.Metal> metals = List.of(
                metal("a:cobalt_ingot"), metal("b:steel_ingot"), metal("c:steel_ingot"),
                metal("d:zinc_ingot"));

        List<ModdedMaterials.Metal> armours = MaterialScan.armourMetals(metals);

        assertEquals(List.of("cobalt_horse_armor", "steel_horse_armor", "zinc_horse_armor"),
                armours.stream().map(ModdedMaterials.Metal::armourId).toList());
    }

    @Test
    void theArmourCollapseDoesNotTouchTheMetalsThemselves() {
        // The bench colours a fitting from whichever ingot the player put in it,
        // so metals() has to keep every one of them even though only one of each
        // name gets an armour. Collapsing the wrong list would silently stop
        // five of six steels working at the bench.
        MaterialScan.Result result = scan(aModWithAWoodAndAMetal(), new FakeJar()
                .put("data/c/tags/item/ingots/tin.json", "{\"values\":[\"othermod:tin_ingot\"]}")
                .put("assets/othermod/textures/item/tin_ingot.png", png(16, 0x8899AA, 255)));

        assertEquals(3, result.metals().size(), "two tins and a ruby all survive the scan");
        assertEquals(2, MaterialScan.armourMetals(result.metals()).size(),
                "but the two tins share one armour");
        assertEquals(0x8899AA,
                result.metals().stream()
                        .filter(m -> m.itemId().equals("othermod:tin_ingot"))
                        .findFirst().orElseThrow().colour(),
                "the losing tin still carries its own colour for the bench");
    }

    @Test
    void metalColourLooksUpOnlyWhatWasFound() {
        assertNull(ModdedMaterials.metalColour("testmod:never_scanned"),
                "an id we never saw has no colour, and must not be given one");
    }
}
