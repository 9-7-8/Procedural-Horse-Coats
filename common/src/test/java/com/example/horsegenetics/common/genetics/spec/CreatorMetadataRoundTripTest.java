package com.example.horsegenetics.common.genetics.spec;

import com.example.horsegenetics.common.genetics.GeneRarity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The gene creator ({@code wiki/gene-creator/}) writes the file the game reads,
 * and it now has form fields for the format-3 gameplay metadata - the blurb,
 * the rarity, the gene carrot and the splice table. That metadata does not
 * paint, so {@code check-parity.mjs} says nothing about it: the parity net
 * covers the paint engine only.
 *
 * <p>This is the net for the other half. Every fixture here is literal creator
 * output, and the assertions are the values that were typed into the forms.
 *
 * <p>Regenerate the fixtures with
 * {@code node wiki/gene-creator/tools/bake-export-fixtures.mjs} after changing
 * the creator's {@code tidy()}, its metadata forms, or its effects mirror.
 */
class CreatorMetadataRoundTripTest {

    private static String fixture() throws IOException {
        try (InputStream in = CreatorMetadataRoundTripTest.class.getClassLoader()
                .getResourceAsStream("creator-metadata-roundtrip.json")) {
            assertNotNull(in, "creator-metadata-roundtrip.json is missing from the test resources");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("the parser accepts a creator export that uses every metadata block")
    void theParserAcceptsACreatorExportThatUsesEveryMetadataBlock() throws IOException {
        GeneSpec spec = GeneSpecParser.parse(fixture(), "creator-metadata-roundtrip.json");

        assertEquals("mymod.my_gene", spec.key());
        assertEquals("A gene that does a thing.", spec.blurb());
        assertEquals(GeneRarity.LEGENDARY, spec.rarity());

        assertTrue(spec.carrot().enabled());
        assertTrue(spec.carrot().homozygous(), "behaviour: homozygous should read as a two-copy carrot");
        assertEquals(List.of("minecraft:sugar"), spec.carrot().flavour());

        // The splice table is keyed by combination, exactly like founders.
        assertEquals(3, spec.splice().size());
        assertEquals(10.0, weightOf(spec, "My/My"), 1e-9);
        assertEquals(40.0, weightOf(spec, "My/my"), 1e-9);
        assertEquals(50.0, weightOf(spec, "my/my"), 1e-9);
    }

    @Test
    @DisplayName("re-exporting a shipped gene through the creator does not change what it does")
    void reExportingAShippedGeneThroughTheCreatorDoesNotChangeWhatItDoes() throws IOException {
        // The creator drops every parameter equal to its default, so its export
        // is textually shorter than the file it read. That is only safe if the
        // game reads the two as the same gene - which is what this asserts,
        // rather than a string comparison that would fail on the tidying alone.
        GeneSpec original = GeneSpecParser.parse(
                resource("creator-effects-waterborn-original.json"), "original");
        GeneSpec tidied = GeneSpecParser.parse(
                resource("creator-effects-waterborn-tidied.json"), "tidied");

        assertEquals(original.expressions().size(), tidied.expressions().size());
        for (int i = 0; i < original.expressions().size(); i++) {
            assertEquals(original.expressions().get(i).abilities(),
                    tidied.expressions().get(i).abilities(),
                    "expression " + i + " grants different abilities after a creator round-trip");
        }
    }

    @Test
    @DisplayName("the parser accepts a creator export using every effect verb")
    void theParserAcceptsACreatorExportUsingEveryEffectVerb() throws IOException {
        GeneSpec spec = GeneSpecParser.parse(resource("creator-effects-all-verbs.json"),
                "creator-effects-all-verbs.json");

        List<GeneAbility> effects = spec.expressions().get(0).abilities();
        // One per verb the creator's effects form offers - if AbilityType grows
        // one and the creator's mirror does not, parity fails; if the mirror
        // grows one the game cannot parse, this fails.
        assertEquals(AbilityType.all().size(), effects.size(),
                "the creator should be able to emit every verb the game declares");

        Set<String> verbs = new LinkedHashSet<>();
        for (GeneAbility a : effects) {
            verbs.add(a.getClass().getSimpleName());
        }
        assertEquals(effects.size(), verbs.size(), "every verb should map to a distinct ability type");

        // Spot-check the two shapes the form has to build rather than copy: a
        // trigger with a value, and a multi-flag condition.
        GeneAbility.Yield milk = effects.stream()
                .filter(GeneAbility.Yield.class::isInstance)
                .map(GeneAbility.Yield.class::cast)
                .findFirst().orElseThrow();
        assertEquals("minecraft:milk_bucket", milk.produces());
        assertEquals(24000, milk.cooldownTicks());
        assertEquals("He kicks you.", milk.deniedMessage());
        assertInstanceOf(GeneAbility.Condition.All.class, milk.when());
        assertEquals(3, ((GeneAbility.Condition.All) milk.when()).terms().size());
    }

    private static String resource(String name) throws IOException {
        try (InputStream in = CreatorMetadataRoundTripTest.class.getClassLoader()
                .getResourceAsStream(name)) {
            assertNotNull(in, name + " is missing from the test resources");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static double weightOf(GeneSpec spec, String combination) {
        return spec.splice().stream()
                .filter(w -> w.combination().equals(combination))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no splice weight for " + combination))
                .percent();
    }
}
