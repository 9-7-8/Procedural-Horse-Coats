package com.example.horsegenetics.common.genetics.spec;

import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code effects} block - the Minecraft-specific abilities a data-driven
 * gene grants. {@code common/} owns the vocabulary and the parse; these tests
 * pin the shape the NeoForge translator reads.
 */
class GeneAbilitySpecTest {

    @AfterEach
    void unregister() {
        Genes.clearLoaded();
    }

    @Test
    void waterbornCarriesACoatLayerAndThreeEffects() {
        GeneSpec spec = GeneSpecParser.parse(GeneSpecParserTest.example("waterborn.json"), "waterborn.json");

        assertEquals("example.waterborn", spec.key());
        assertTrue(spec.hasAbilities());
        assertEquals(1, spec.layers().size(), "the neon stripes are an ordinary magical coat layer");
        assertEquals(0x1ec8ff, spec.layers().get(0).op().params().color("color", 0));

        List<GeneAbility> effects = spec.abilities();
        assertEquals(3, effects.size());

        GeneAbility.Traversal walk = assertInstanceOf(GeneAbility.Traversal.class, effects.get(0));
        assertEquals("walk_on_water", walk.flag());
        assertEquals(new GeneAbility.Condition.Flag("adult", false), walk.when());
        assertEquals(1, walk.minDose());

        GeneAbility.Emitter trail = assertInstanceOf(GeneAbility.Emitter.class, effects.get(1));
        assertEquals("particle", trail.kind());
        assertEquals("trail", trail.shape());
        assertEquals("feet", trail.anchor());
        assertInstanceOf(GeneAbility.Trigger.OnMove.class, trail.trigger());
        assertEquals("minecraft:dust", trail.particle());
        assertEquals(0x1ec8ff, trail.color());
        assertEquals(0.7, trail.chance(), 1e-9);

        GeneAbility.Yield milk = assertInstanceOf(GeneAbility.Yield.class, effects.get(2));
        assertEquals("minecraft:bucket", milk.trigger().item());
        assertEquals("minecraft:bucket", milk.consumes());
        assertEquals("minecraft:water_bucket", milk.produces());
        assertEquals(2400, milk.cooldownTicks());
        assertEquals(
                new GeneAbility.Condition.All(List.of(
                        new GeneAbility.Condition.Flag("sex_female", false),
                        new GeneAbility.Condition.Flag("tamed", false))),
                milk.when());
    }

    @Test
    void activeForListsEffectsOnlyWhileTheGeneExpresses() {
        SpecGene gene = new SpecGene(
                GeneSpecParser.parse(GeneSpecParserTest.example("waterborn.json"), "waterborn.json"));
        Genes.register(gene);

        AllelePair homo = new AllelePair(gene.alleles().get(0), gene.alleles().get(0));
        AllelePair carrier = new AllelePair(gene.alleles().get(0), gene.wildType());
        AllelePair none = new AllelePair(gene.wildType(), gene.wildType());

        assertEquals(3, SpecAbilities.activeFor(Genotype.of(homo)).size());
        assertEquals(3, SpecAbilities.activeFor(Genotype.of(carrier)).size(),
                "Waterborn is dominant - one copy expresses every minDose-1 effect");
        assertEquals(0, SpecAbilities.activeFor(Genotype.of(none)).size());
        assertTrue(SpecAbilities.anyLoaded());
    }

    @Test
    void minDoseTwoGatesAnEffectOnTheHomozygote() {
        String json = """
                { "key": "example.gilled", "phase": "magical", "dominance": "INCOMPLETE_DOMINANT",
                  "alleles": [ {"token":"G"}, {"token":"n"} ],
                  "layers": [],
                  "effects": [
                    { "type": "traversal", "flag": "underwater_breathing" },
                    { "type": "traversal", "flag": "walk_on_water", "minDose": 2 } ] }
                """;
        SpecGene gene = new SpecGene(GeneSpecParser.parse(json, "gilled.json"));
        Genes.register(gene);

        AllelePair carrier = new AllelePair(gene.alleles().get(0), gene.wildType());
        AllelePair homo = new AllelePair(gene.alleles().get(0), gene.alleles().get(0));

        assertEquals(1, SpecAbilities.activeFor(Genotype.of(carrier)).size());
        assertEquals(2, SpecAbilities.activeFor(Genotype.of(homo)).size());
    }

    // --- what it refuses ----------------------------------------------

    @Test
    void rejectsAnUnknownTraversalFlag() {
        String json = base("""
                "effects": [ { "type": "traversal", "flag": "walk_on_watr" } ]
                """);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> GeneSpecParser.parse(json, "bad.json"));
        assertTrue(e.getMessage().contains("walk_on_watr"), e.getMessage());
        assertTrue(e.getMessage().contains("walk_on_water"), "lists the legal flags: " + e.getMessage());
    }

    @Test
    void rejectsAnUnknownConditionFlag() {
        String json = base("""
                "effects": [ { "type": "traversal", "flag": "fire_immune",
                               "when": { "flag": "is_hungry" } } ]
                """);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> GeneSpecParser.parse(json, "bad.json"));
        assertTrue(e.getMessage().contains("is_hungry"), e.getMessage());
    }

    @Test
    void rejectsAnUnknownEffectType() {
        String json = base("""
                "effects": [ { "type": "teleport", "to": "home" } ]
                """);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> GeneSpecParser.parse(json, "bad.json"));
        assertTrue(e.getMessage().contains("teleport"), e.getMessage());
    }

    @Test
    void rejectsAYieldThatDoesNotFireOnInteract() {
        String json = base("""
                "effects": [ { "type": "yield", "trigger": { "interval": 100 },
                               "produces": "minecraft:milk_bucket" } ]
                """);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> GeneSpecParser.parse(json, "bad.json"));
        assertTrue(e.getMessage().contains("on_interact"), e.getMessage());
    }

    @Test
    void rejectsAnUnknownKeyInsideAnEffect() {
        String json = base("""
                "effects": [ { "type": "traversal", "flag": "fall_immune", "wehn": {} } ]
                """);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> GeneSpecParser.parse(json, "bad.json"));
        assertTrue(e.getMessage().contains("wehn"), e.getMessage());
    }

    private static String base(String effectsLine) {
        return "{ \"key\": \"example.t\", \"phase\": \"magical\", "
                + "\"alleles\": [ {\"token\":\"A\"}, {\"token\":\"a\"} ], \"layers\": [], "
                + effectsLine + " }";
    }
}
