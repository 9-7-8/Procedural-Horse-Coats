package com.example.horsegenetics.neoforge.server.recipe;

import com.example.horsegenetics.neoforge.item.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * <b>The bottom rung: a Basic Horse Stasis Chamber.</b> A glass bottle, a wheat,
 * a horse hair and a container of water, in any arrangement.
 *
 * <p>It is a {@code CustomRecipe} rather than a JSON shapeless recipe for one
 * reason, and it is the water: <b>the container is not consumed.</b> A plain
 * recipe eats every input, and the owner's rule is that you keep your bucket.
 *
 * <h2>Matching the water by tag, not by item</h2>
 * The ingredient is {@link Tags.Items#BUCKETS_WATER} ({@code c:buckets/water}),
 * the convention tag every mod that adds a water container is expected to join,
 * so a modded bucket-equivalent crafts this without anyone here naming it.
 *
 * <h2>Giving the container back, and the half of it that is still open</h2>
 * {@link #getRemainingItems} returns the input's <b>own declared crafting
 * remainder</b> when it has one - which is how a vanilla water bucket comes back
 * as an empty bucket, the same as in a cake. A modded container that declares no
 * remainder is handed <b>straight back unchanged, still full</b>, because "not
 * consumed" is the rule that matters and returning nothing would break it.
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

    /** Any container of water, ours or another mod's, by the convention tag. */
    private static boolean isWater(ItemStack stack) {
        return stack.is(Tags.Items.BUCKETS_WATER);
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
     * The four ingredients, in no particular arrangement - {@code matches}
     * accepts any, so this is drawn as the shapeless recipe it is.
     *
     * <p>Built on first use and only cached once it is real: the water tag is
     * a datapack tag, so asking too early gives an empty ingredient and
     * {@code PlacementInfo.create} answers {@code NOT_PLACEABLE} - which, if it
     * were cached, would put this straight back to being the invisible recipe
     * the override above exists to fix.
     */
    @Override
    public PlacementInfo placementInfo() {
        PlacementInfo cached = placement;
        if (cached != null) {
            return cached;
        }
        PlacementInfo built = PlacementInfo.create(List.of(
                Ingredient.of(Items.GLASS_BOTTLE),
                Ingredient.of(Items.WHEAT),
                Ingredient.of(ModItems.HORSE_HAIR.get()),
                Ingredient.of(BuiltInRegistries.ITEM.getOrThrow(Tags.Items.BUCKETS_WATER))));
        if (!built.isImpossibleToPlace()) {
            placement = built;
        }
        return built;
    }

    private volatile @Nullable PlacementInfo placement;

    @Override
    public RecipeSerializer<StasisChamberRecipe> getSerializer() {
        return SERIALIZER;
    }
}
