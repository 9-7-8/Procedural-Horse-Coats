package com.example.horsegenetics.neoforge.server.recipe;

import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.data.StasisSnapshot;
import com.example.horsegenetics.neoforge.item.ModItems;
import com.example.horsegenetics.neoforge.item.StasisChamberItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * <b>Surrounding a chamber with ender pearls.</b> A Basic Horse Stasis Chamber
 * in the middle of the grid and eight ender pearls around it makes the
 * <b>Emergency Horse Stasis Chamber</b> - the one that captures a horse of its
 * owner's by itself when that horse is about to die. Owner's recipe, on the
 * page: "a basic stasis chamber surrounded by ender pearls".
 *
 * <p>It is built from the <em>cheapest</em> rung rather than the dearest,
 * deliberately. What the eight pearls buy is the self-triggering, not a bank
 * capability: an emergency chamber is a Basic chamber in every other respect,
 * and the bank cannot look inside one. A player who wants both takes the horse
 * out and files it, or spends a book upgrading the chamber, which trades the
 * emergency away for the window.
 *
 * <h2>This is the second of two recipes, and it takes only an occupied chamber</h2>
 * An <b>empty</b> Basic chamber is converted by an ordinary shaped JSON recipe,
 * {@code recipe/emergency_stasis_chamber.json}, whose centre key is a
 * {@code neoforge:data_component} ingredient carrying the removal patch
 * {@code "!horsegenetics:stasis_snapshot": {}} - so it matches a chamber with no
 * horse in it and nothing else. {@link #middle} here <b>requires</b> the snapshot
 * to be present, so the two never claim the same grid.
 *
 * <h2>Why the occupied half cannot be a shaped JSON file</h2>
 * The same reason {@link StasisUpgradeRecipe} gives, and it is worth repeating
 * because the failure is silent: <b>the chamber in the middle may have a horse
 * in it</b>. A JSON recipe builds its result from scratch and copies no
 * components, so surrounding an occupied chamber with pearls would hand back an
 * empty emergency chamber and delete a pedigreed animal, with no message and
 * nothing in the log. This copies {@code stasis_snapshot} across, so the horse
 * rides the conversion the way it rides an upgrade.
 *
 * <p>Like the upgrades, this half keeps {@code CustomRecipe}'s defaults and is
 * deliberately <b>not</b> drawn: the JSON twin draws the identical ring, and
 * listing both would show the recipe twice.
 *
 * <p>Shaped rather than shapeless because the arrangement is the point of the
 * name - the pearls go <i>around</i> it - and because a shapeless nine-item
 * match would also accept the chamber in a corner, which reads like a different
 * recipe that happens to work.
 */
public class EmergencyChamberRecipe extends CustomRecipe {

    /**
     * One instance, shared by both codecs. See {@link CarrotCombineRecipe#INSTANCE}
     * for why a second one kicks every joining client off a dedicated server.
     */
    public static final EmergencyChamberRecipe INSTANCE = new EmergencyChamberRecipe();

    public static final MapCodec<EmergencyChamberRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, EmergencyChamberRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<EmergencyChamberRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    /**
     * The chamber in the middle, if the grid is a full three by three with a
     * Basic chamber at its centre and an ender pearl in all eight other slots.
     *
     * <p>{@code CraftingInput} is cropped to the smallest box holding anything,
     * so asking for three by three is asking for a grid with no empty slot in
     * it - and a crafting table, since a player's two-by-two cannot make one.
     */
    private static @Nullable ItemStack middle(CraftingInput input) {
        if (input.width() != 3 || input.height() != 3) {
            return null;
        }
        ItemStack centre = ItemStack.EMPTY;
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                ItemStack slot = input.getItem(x, y);
                if (x == 1 && y == 1) {
                    // Only the bottom rung. An Intermediate or better would be
                    // trading a capability away for this one, and an emergency
                    // chamber is already one of these.
                    if (!slot.is(ModItems.BASIC_STASIS_CHAMBER.get())) {
                        return null;
                    }
                    centre = slot;
                } else if (!slot.is(Items.ENDER_PEARL)) {
                    return null;
                }
            }
        }
        // Occupied chambers only. An empty one is the JSON twin's ring, and two
        // recipes claiming one grid would be resolved by registry order, leaving
        // the loser unreachable. This is the line that keeps them apart.
        if (centre.isEmpty() || StasisChamberItem.snapshotOf(centre) == null) {
            return null;
        }
        return centre;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return middle(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack chamber = middle(input);
        if (chamber == null) {
            return ItemStack.EMPTY;
        }
        ItemStack out = new ItemStack(ModItems.EMERGENCY_STASIS_CHAMBER.get());
        // The horse rides the conversion. Without this line an occupied chamber
        // comes out empty and the animal is gone for good.
        StasisSnapshot inside = StasisChamberItem.snapshotOf(chamber);
        if (inside != null) {
            out.set(ModDataComponents.STASIS_SNAPSHOT.get(), inside);
        }
        return out;
    }

    // No isSpecial(), placementInfo() or display() override: CustomRecipe's
    // defaults are wanted here. The JSON twin draws this ring; see the class
    // comment for why drawing it twice would be worse than not at all.

    @Override
    public RecipeSerializer<EmergencyChamberRecipe> getSerializer() {
        return SERIALIZER;
    }
}
