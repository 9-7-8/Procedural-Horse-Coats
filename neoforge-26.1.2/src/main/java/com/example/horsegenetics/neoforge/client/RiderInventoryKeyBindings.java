package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/**
 * The key that opens <i>your own</i> inventory. Default
 * <kbd>ALT</kbd>+<kbd>E</kbd>, rebindable in Controls.
 *
 * <p><b>It exists because this mod took the obvious key away.</b>
 * {@link HorseInfoKeyHandler} claims <kbd>E</kbd> while you are riding a horse
 * this mod knows, and opens {@link HorseInfoScreen} instead of the vanilla
 * horse screen. That is the right trade - the horse screen it replaces held a
 * saddle slot, a barding slot and nothing else - but it leaves a rider with no
 * gesture at all for the bag on their own back. This is that gesture.
 *
 * <p><b>Why not simply share <kbd>E</kbd>.</b> It cannot: the vanilla
 * {@code keyInventory} mapping is the one {@code HorseInfoKeyHandler} drains,
 * and a second listener on the same mapping would be a race between two
 * handlers over the same click. A separate mapping is also what lets a player
 * who dislikes the arrangement swap the two in Controls.
 *
 * <p><b>Not restricted to riding</b> (owner's call). The context is the whole
 * of {@code IN_GAME}, so it works on foot as well. A riding-only key buys
 * nothing and makes "it didn't open" ambiguous - you would not know whether the
 * key was unbound or the guard had rejected you.
 *
 * <p><b>No config switch.</b> Unlike the rest of the rider-comfort pieces, this
 * one gets none: an unbound key <i>is</i> the off switch, and Controls is where
 * a player already looks for it.
 *
 * <h2>The chord is new API surface here</h2>
 * <b>UNVERIFIED:</b> this is the first {@link KeyModifier} in the mod - every
 * other mapping ({@link HorseBrowserKeyBindings}, {@link WhistleKeyBindings},
 * {@link DebugKeyBindings}) is a bare key, and {@code WhistleKeyBindings}
 * avoided a chord on purpose. Reading NeoForge 26.1.2's
 * {@code KeyMappingLookup.getAll} says it should work: with ALT held, the
 * lookup collects the bindings registered under ALT first and only falls back
 * to the unmodified ones {@code if (matchingBindings.isEmpty())}, so a live
 * ALT+E match suppresses bare <kbd>E</kbd> and vanilla's {@code keyInventory}
 * is never clicked. <b>That needs confirming in a real client</b> - both that
 * the chord fires, and that the Controls screen draws it as "Alt + E" rather
 * than a bare E that looks like a conflict. If either fails, the fallback the
 * owner agreed is a bare key that clashes with nothing in this repo (the taken
 * ones are H, apostrophe, F, F6, F7, F8) - pick one and say which.
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = HorseGenetics.MOD_ID)
public final class RiderInventoryKeyBindings {

    public static final String RIDER_INVENTORY_KEY = "key.horsegenetics.rider_inventory";

    public static KeyMapping riderInventory;

    @SubscribeEvent
    static void register(RegisterKeyMappingsEvent event) {
        riderInventory = new KeyMapping(
                RIDER_INVENTORY_KEY,
                KeyConflictContext.IN_GAME,
                KeyModifier.ALT,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_E,
                // INVENTORY, not the MISC the other mappings use. Still a
                // built-in category with nothing to register or localise, and
                // it puts this key directly beneath vanilla's own "Open/Close
                // Inventory" - which is the row a player is reading when they
                // go looking for why E stopped doing that.
                KeyMapping.Category.INVENTORY);
        event.register(riderInventory);
    }

    private RiderInventoryKeyBindings() {
    }
}
