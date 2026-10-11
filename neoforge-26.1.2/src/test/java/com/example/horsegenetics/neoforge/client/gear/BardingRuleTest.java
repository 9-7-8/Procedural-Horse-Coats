package com.example.horsegenetics.neoforge.client.gear;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.horsegenetics.neoforge.client.gear.BardingRule.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * <b>When horse armour is drawn edged and when vanilla draws it.</b>
 *
 * <p>No test can see a draw, so what is pinned is the decision in front of it:
 * {@link BardingRule} is the one place that sends a suit of armour back to
 * vanilla's path, and a rule that quietly stopped doing so would show as armour
 * missing a layer, or laid out for the wrong model, on somebody else's horse.
 */
class BardingRuleTest {

    @Test
    @DisplayName("readable armour with the switch on is edged")
    void edgedByDefault() {
        assertEquals(Path.EDGED, BardingRule.path(true, false, false, 1, 0));
        assertEquals(Path.EDGED, BardingRule.path(true, false, false, 4, 0), "leather's four layers");
    }

    @Test
    @DisplayName("the client switch hands every suit back to vanilla")
    void switchOff() {
        assertEquals(Path.VANILLA, BardingRule.path(false, false, false, 1, 0));
    }

    @Test
    @DisplayName("one layer with no mesh sends the whole item to vanilla")
    void anyFlatLayer() {
        assertEquals(Path.VANILLA, BardingRule.path(true, false, false, 1, 1), "an unreadable texture");
        assertEquals(Path.VANILLA, BardingRule.path(true, false, false, 4, 1),
                "three lifted layers must not bury the flat fourth");
    }

    @Test
    @DisplayName("a model another mod swapped in, and a trim, are vanilla's to draw")
    void foreignModelAndTrim() {
        assertEquals(Path.VANILLA, BardingRule.path(true, true, false, 1, 0));
        assertEquals(Path.VANILLA, BardingRule.path(true, false, true, 1, 0));
    }

    @Test
    @DisplayName("an asset with no drawn layer is left to vanilla, which draws nothing")
    void nothingToDraw() {
        assertEquals(Path.VANILLA, BardingRule.path(true, false, false, 0, 0));
    }
}
