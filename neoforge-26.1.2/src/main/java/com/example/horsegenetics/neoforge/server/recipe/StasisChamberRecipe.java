package com.example.horsegenetics.neoforge.server.recipe;

import com.example.horsegenetics.neoforge.item.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.crafting.DifferenceIngredient;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * <b>The bottom rung: a Basic Horse Stasis Chamber - the modded half.</b> A glass
 * bottle, a wheat, a horse hair and a container of water, in any arrangement.
 *
 * <h2>This is the second of two recipes, and it takes only the modded water</h2>
 * The vanilla grid - the same four items with a {@code minecraft:water_bucket} -
 * is an ordinary shapeless JSON recipe, {@code recipe/basic_stasis_chamber.json}.
 * <b>This class deliberately refuses the vanilla bucket</b> ({@link #water()}
 * subtracts it) so that exactly one recipe ever claims a given grid: two that
 * matched the same one would be resolved by registry order, and the loser would
 * be a recipe nothing can reach. See {@code check-recipes.mjs} for the shape of
 * that bug.
 *
 * <p>The split exists because <b>a plain JSON recipe is the only shape proven to
 * be drawable on a modded server</b> - it is why the stasis bank was the one
 * thing in this family a player could find in JEI - while a tag ingredient is
 * the only way to accept a water container this mod has never heard of. One
 * recipe could not be both.
 *
 * <p>A previous version of this comment claimed the custom recipe was needed for
 * the vanilla bucket too, on the grounds that "a plain recipe eats every input".
 * <b>That was simply wrong</b>: {@code Items.WATER_BUCKET} is declared
 * {@code craftRemainder(BUCKET)}, so any JSON recipe hands the empty bucket back
 * by itself - it is how cake returns three of them.
 *
 * <h2>Matching the water by tag, not by item</h2>
 * The base ingredient is {@link Tags.Items#BUCKETS_WATER} ({@code c:buckets/water}),
 * the convention tag every mod that adds a water container is expected to join,
 * so a modded bucket-equivalent crafts this without anyone here naming it.
 *
 * <h2>Giving the container back, and the half of it that is still open</h2>
 * {@link #getRemainingItems} returns the input's <b>own declared crafting
 * remainder</b> when it has one. A modded container that declares no remainder is
 * handed <b>straight back unchanged, still full</b>, because "not consumed" is
 * the rule that matters and returning nothing would break it.
 *
 * <p>That second branch is deliberately the blunt answer. The correct one is to
 * drain the container through its fluid capability and return whatever that
 * leaves, which is the same match-by-tag-return-drained-container problem the
 * water troughs on {@code wiki/compatibility.html} will have to solve. That work
 * does not exist yet, and {@code wiki/horse-stasis.html} says to reuse it rather
 * than invent a second remainder rule here - so this waits for it, and the
 * meantime costs a bucket of water, not a bucket. See that page's Verification
 * tab.
 */
public class StasisChamberRecipe extends CustomRecipe {

    /**
     * One instance, shared by both codecs. See {@link CarrotCombineRecipe#INSTANCE}
     * for why a second one is a login kick on every dedicated server rather than
     * a recipe that merely does not work.
     */
    public static final StasisChamberRecipe INSTANCE = new StasisChamberRecipe();

    public static final MapCodec<StasisChamberRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, StasisChamberRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<StasisChamberRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    /** The grid holds exactly one of each of the four ingredients and nothing else. */
    private static boolean complete(CraftingInput input) {
        int bottle = 0;
        int wheat = 0;
        int hair = 0;
        int water = 0;
        int filled = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            filled++;
            if (s.is(Items.GLASS_BOTTLE)) {
                bottle++;
            } else if (s.is(Items.WHEAT)) {
                wheat++;
            } else if (s.is(ModItems.HORSE_HAIR.get())) {
                hair++;
            } else if (isWater(s)) {
                water++;
            }
        }
        return bottle == 1 && wheat == 1 && hair == 1 && water == 1 && filled == 4;
    }

    /**
     * <b>Every water container except the vanilla bucket</b> - {@code c:buckets/water}
     * minus {@code minecraft:water_bucket}, the one the JSON twin claims.
     *
     * <p>One definition, used by {@link #isWater}, {@link #placementInfo()} and
     * {@link #display()} alike, so this recipe cannot advertise a grid it would
     * then refuse.
     *
     * <p><b>{@code null} when nothing but the vanilla bucket is in the tag</b>,
     * which is the ordinary case on a pack with no water-adding mod. That is the
     * honest answer rather than an edge case: with the bucket subtracted there is
     * no item left this recipe would accept, so it must not match, and it must
     * not be drawn either - an ingredient over an empty set renders as a blank
     * slot, and a recipe with a hole in it is worse than no second recipe at all.
     * The vanilla grid is still fully craftable; it is the JSON twin's.
     *
     * <p>Built on first use and cached only once it is real. The tag is datapack
     * content, so asking before the first reload gives an empty one, and caching
     * that would leave this permanently inert even on a pack that does add a
     * container. A {@code null} return is therefore always retried.
     */
    private static volatile @Nullable Ingredient water;

    private static @Nullable Ingredient water() {
        Ingredient cached = water;
        if (cached != null) {
            return cached;
        }
        HolderSet.Named<Item> tagged = BuiltInRegistries.ITEM.getOrThrow(Tags.Items.BUCKETS_WATER);
        boolean anyModded = false;
        for (Holder<Item> holder : tagged) {
            if (!holder.value().equals(Items.WATER_BUCKET)) {
                anyModded = true;
                break;
            }
        }
        if (!anyModded) {
            return null;
        }
        Ingredient built = DifferenceIngredient.of(Ingredient.of(tagged), Ingredient.of(Items.WATER_BUCKET));
        water = built;
        return built;
    }

    /** A container of water some other mod added. The vanilla bucket is the JSON recipe's. */
    private static boolean isWater(ItemStack stack) {
        Ingredient accepted = water();
        return accepted != null && accepted.test(stack);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return complete(input);
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        if (!complete(input)) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(ModItems.BASIC_STASIS_CHAMBER.get());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack s = input.getItem(slot);
            if (s.isEmpty()) {
                continue;
            }
            ItemStackTemplate declared = s.getCraftingRemainder();
            if (isWater(s)) {
                // Never empty-handed: the declared remainder if there is one
                // (vanilla's empty bucket), otherwise the container itself.
                remaining.set(slot, declared != null ? declared.create() : s.copyWithCount(1));
            } else if (declared != null) {
                remaining.set(slot, declared.create());
            }
        }
        return remaining;
    }

    /**
     * <b>Not special, so the recipe is findable.</b> {@code CustomRecipe} answers
     * {@code true} here and {@code PlacementInfo.NOT_PLACEABLE} below, and
     * between them that is a recipe <i>no tool can show you</i>: no recipe-book
     * entry, no place-into-grid button, and - the way this was actually found -
     * <b>nothing at all when you click the chamber in JEI</b>, since JEI skips
     * special recipes on the grounds that they have no ingredients to draw. The
     * item existed, the recipe worked if you already knew it, and the game
     * offered no way to learn it. See {@code wiki/horse-stasis.html}.
     *
     * <p>Overriding these two costs nothing that {@code matches} was doing:
     * matching is still this class's, so the water is still any container in
     * {@code c:buckets/water} and the container still comes back. The placement
     * is only what the book and JEI <i>draw</i>.
     */
    @Override
    public boolean isSpecial() {
        return false;
    }

    /**
     * Where the recipe book may place this into the grid for you. Cached only
     * once it is real, for the reason {@link #water()} gives.
     */
    @Override
    public PlacementInfo placementInfo() {
        PlacementInfo cached = placement;
        if (cached != null) {
            return cached;
        }
        List<Ingredient> ingredients = ingredients();
        if (ingredients == null) {
            return PlacementInfo.NOT_PLACEABLE;
        }
        PlacementInfo built = PlacementInfo.create(ingredients);
        if (!built.isImpossibleToPlace()) {
            placement = built;
        }
        return built;
    }

    private volatile @Nullable PlacementInfo placement;

    /**
     * <b>What a recipe viewer actually draws, and the piece that was missing.</b>
     *
     * <p>{@link #isSpecial()} and {@link #placementInfo()} were overridden first,
     * and were not enough on their own: {@code Recipe.display()} defaults to
     * {@code List.of()} and {@code CustomRecipe} does not override it, so this
     * recipe advertised <b>no displays at all</b> - and a display is the object
     * the recipe book and JEI draw. The item stayed undiscoverable through a
     * whole release with the other two flags correct.
     *
     * <p>So all three are needed, and they answer three different questions: is
     * this recipe listed, can it be placed into the grid for you, and what does
     * it look like. See {@code wiki/horse-stasis.html}.
     *
     * <p>The water slot draws from the tag, so on a modded server it cycles
     * through every container that mod added - which is the whole point of this
     * being the second recipe.
     */
    @Override
    public List<RecipeDisplay> display() {
        List<Ingredient> ingredients = ingredients();
        if (ingredients == null) {
            return List.of();
        }
        return List.of(new ShapelessCraftingRecipeDisplay(
                ingredients.stream().map(Ingredient::display).toList(),
                new SlotDisplay.ItemSlotDisplay(ModItems.BASIC_STASIS_CHAMBER.get()),
                new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
    }

    /**
     * The four ingredients, in no particular arrangement - {@code matches}
     * accepts any, so this is drawn as the shapeless recipe it is. {@code null}
     * while the water tag is still empty, which is the one state in which this
     * recipe can neither match nor be drawn honestly.
     */
    private static @Nullable List<Ingredient> ingredients() {
        Ingredient accepted = water();
        if (accepted == null) {
            return null;
        }
        return List.of(
                Ingredient.of(Items.GLASS_BOTTLE),
                Ingredient.of(Items.WHEAT),
                Ingredient.of(ModItems.HORSE_HAIR.get()),
                accepted);
    }

    @Override
    public RecipeSerializer<StasisChamberRecipe> getSerializer() {
        return SERIALIZER;
    }
}
