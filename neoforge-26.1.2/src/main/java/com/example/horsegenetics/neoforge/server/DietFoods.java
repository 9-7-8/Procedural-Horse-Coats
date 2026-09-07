package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.Diet;
import com.example.horsegenetics.common.genetics.HorseDiet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Map;

/**
 * <b>The {@link Diet} &rarr; item table</b>, and deliberately on this side of
 * the split: {@code common/} names categories, the game module names items -
 * the same division {@code RarityItems} makes for the gene-carrot tiers, and
 * for the same reason (a backport re-points this file and touches nothing
 * else).
 *
 * <p>Two things it owes {@code common/}:
 * <ul>
 *   <li>a diet with {@link Diet#variants()} above zero must have <b>exactly</b>
 *       that many items listed, because the roll that picks one is an index
 *       into this list. {@link #verify()} checks it at startup rather than
 *       letting an out-of-range horse quietly eat nothing;</li>
 *   <li>the lists are the whole definition of a diet. There is no tag and no
 *       predicate hiding elsewhere.</li>
 * </ul>
 *
 * <p><b>Unverified against a running game.</b> Every item id here is read off
 * the 26.1.2 sources, not off a play session.
 */
public final class DietFoods {

    private DietFoods() {
    }

    /** Raw flesh. Fish are their own diet, so they are not here. */
    private static final List<Item> RAW_MEAT = List.of(
            Items.BEEF, Items.PORKCHOP, Items.CHICKEN, Items.MUTTON, Items.RABBIT);

    private static final List<Item> FISH = List.of(
            Items.COD, Items.SALMON, Items.TROPICAL_FISH, Items.PUFFERFISH,
            Items.COOKED_COD, Items.COOKED_SALMON);

    /** Things that grow and are eaten as they come out of the ground. */
    private static final List<Item> RAW_VEGETABLES = List.of(
            Items.CARROT, Items.POTATO, Items.BEETROOT, Items.MELON_SLICE, Items.APPLE,
            Items.SWEET_BERRIES, Items.GLOW_BERRIES, Items.PUMPKIN);

    private static final List<Item> WHEAT = List.of(Items.WHEAT, Items.HAY_BLOCK);

    /** Cooked and prepared - what a person would sit down to. */
    private static final List<Item> HUMAN_FOOD = List.of(
            Items.BREAD, Items.COOKED_BEEF, Items.COOKED_PORKCHOP, Items.COOKED_CHICKEN,
            Items.COOKED_MUTTON, Items.COOKED_RABBIT, Items.BAKED_POTATO,
            Items.MUSHROOM_STEW, Items.RABBIT_STEW, Items.BEETROOT_SOUP,
            Items.COOKIE, Items.PUMPKIN_PIE, Items.CAKE,
            Items.GOLDEN_CARROT, Items.GOLDEN_APPLE);

    private static final List<Item> CAKE = List.of(Items.CAKE);

    private static final List<Item> POTION = List.of(
            Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION);

    private static final List<Item> LAVA = List.of(Items.LAVA_BUCKET);
    private static final List<Item> WATER = List.of(Items.WATER_BUCKET);

    /** Order is the contract: the epigenetic roll is an index into this list. */
    private static final List<Item> INGOTS = List.of(
            Items.IRON_INGOT, Items.GOLD_INGOT, Items.COPPER_INGOT, Items.NETHERITE_INGOT);

    /** Order is the contract - see {@link #INGOTS}. */
    private static final List<Item> GEMS = List.of(
            Items.DIAMOND, Items.EMERALD, Items.AMETHYST_SHARD, Items.QUARTZ, Items.LAPIS_LAZULI);

    private static final Map<Diet, List<Item>> BY_DIET = Map.of(
            Diet.RAW_MEAT, RAW_MEAT,
            Diet.FISH, FISH,
            Diet.RAW_VEGETABLES, RAW_VEGETABLES,
            Diet.WHEAT, WHEAT,
            Diet.HUMAN_FOOD, HUMAN_FOOD,
            Diet.CAKE, CAKE,
            Diet.POTION, POTION,
            Diet.LAVA, LAVA,
            Diet.WATER, WATER);

    /**
     * What is left in the player's hand after the horse has eaten - a bucket
     * back from a bucket, a bottle back from a potion. {@code null} for
     * everything else, which is simply consumed.
     */
    private static final Map<Item, Item> REMAINDERS = Map.of(
            Items.LAVA_BUCKET, Items.BUCKET,
            Items.WATER_BUCKET, Items.BUCKET,
            Items.POTION, Items.GLASS_BOTTLE,
            Items.SPLASH_POTION, Items.GLASS_BOTTLE,
            Items.LINGERING_POTION, Items.GLASS_BOTTLE);

    /**
     * Does this horse eat this? The one question the handler asks.
     *
     * <p>{@link Diet#NORMAL} answers false for everything - an ordinary horse
     * is vanilla's business and the handler never gets this far for one - and
     * so does {@link Diet#NOTHING}, which is the point of it.
     */
    public static boolean accepts(HorseDiet diet, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        switch (diet.diet()) {
            case NORMAL:
            case NOTHING:
                return false;
            case ANYTHING:
                return isEdible(stack);
            case INGOT:
                return stack.is(INGOTS.get(clamp(diet.variant(), INGOTS.size())));
            case GEM:
                return stack.is(GEMS.get(clamp(diet.variant(), GEMS.size())));
            default: {
                List<Item> items = BY_DIET.get(diet.diet());
                return items != null && items.contains(stack.getItem());
            }
        }
    }

    /**
     * Is this something a player could plausibly be <b>trying</b> to feed the
     * horse? It is the difference between refusing an apple - which has to be
     * refused, or vanilla heals the horse behind us - and ignoring a saddle,
     * which must go through untouched.
     */
    public static boolean isFeedAttempt(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (isEdible(stack)) {
            return true;
        }
        for (List<Item> items : BY_DIET.values()) {
            if (items.contains(stack.getItem())) {
                return true;
            }
        }
        return INGOTS.contains(stack.getItem()) || GEMS.contains(stack.getItem());
    }

    /**
     * Would vanilla put a tamed adult horse into love mode for this? The diet
     * locus governs <b>healing</b>, not breeding, and a special-diet horse that
     * could never be bred would be a dead end rather than a curiosity - so
     * these two go through untouched whatever the horse eats.
     */
    public static boolean isVanillaLoveItem(ItemStack stack) {
        return stack.is(Items.GOLDEN_APPLE) || stack.is(Items.GOLDEN_CARROT)
                || stack.is(Items.ENCHANTED_GOLDEN_APPLE);
    }

    /** The container left behind, or {@code null}. */
    public static Item remainderOf(ItemStack stack) {
        return REMAINDERS.get(stack.getItem());
    }

    /** The item a variant diet is currently asking for - for a message or a tooltip. */
    public static Item wantedBy(HorseDiet diet) {
        if (diet.diet() == Diet.INGOT) {
            return INGOTS.get(clamp(diet.variant(), INGOTS.size()));
        }
        if (diet.diet() == Diet.GEM) {
            return GEMS.get(clamp(diet.variant(), GEMS.size()));
        }
        return null;
    }

    /**
     * Every variant diet has exactly as many items here as {@code common/} says
     * it has variants. Called once at startup; a mismatch is a programming
     * error, and a silent one - the horse would simply never accept anything.
     */
    public static void verify() {
        require(Diet.INGOT, INGOTS.size());
        require(Diet.GEM, GEMS.size());
        for (Diet diet : Diet.values()) {
            if (diet.variants() == 0 && diet != Diet.NORMAL && diet != Diet.NOTHING
                    && diet != Diet.ANYTHING && !BY_DIET.containsKey(diet)) {
                throw new IllegalStateException("DietFoods has no items for " + diet);
            }
        }
    }

    private static void require(Diet diet, int listed) {
        if (diet.variants() != listed) {
            throw new IllegalStateException("DietFoods lists " + listed + " items for " + diet
                    + " but common says it has " + diet.variants() + " variants");
        }
    }

    private static boolean isEdible(ItemStack stack) {
        return stack.has(DataComponents.FOOD);
    }

    private static int clamp(int i, int size) {
        return i < 0 ? 0 : (i >= size ? size - 1 : i);
    }
}
