package com.example.horsegenetics.neoforge.server.recipe;

import com.example.horsegenetics.common.horse.StasisTier;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.data.StasisSnapshot;
import com.example.horsegenetics.neoforge.item.ModItems;
import com.example.horsegenetics.neoforge.item.StasisChamberItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * <b>Climbing the stasis ladder.</b> A chamber plus one ingredient becomes the
 * chamber one rung up: an eye of ender for Intermediate, a gold ingot for
 * Advanced, a diamond for Spacer. Three recipes' worth of behaviour in one
 * class, since they differ only in which item they ask for.
 *
 * <h2>Why this is a Java recipe and not three JSON files</h2>
 * <b>Because a chamber may have a horse in it.</b> A vanilla shapeless recipe
 * builds its result from scratch and carries no components across, so upgrading
 * an occupied chamber would hand back a shiny new empty one and delete the
 * animal - silently, with no message and nothing in the log. This recipe copies
 * the {@code stasis_snapshot} component onto the result, so a horse rides the
 * upgrade untouched.
 *
 * <p>That is worth being blunt about: the JSON version of this feature is not a
 * cheaper version of it, it is a data-loss bug with a crafting recipe attached.
 */
public class StasisUpgradeRecipe extends CustomRecipe {

    /**
     * One instance, shared by both codecs. See {@link CarrotCombineRecipe#INSTANCE}
     * for why a second one kicks every joining client off a dedicated server.
     */
    public static final StasisUpgradeRecipe INSTANCE = new StasisUpgradeRecipe();

    public static final MapCodec<StasisUpgradeRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, StasisUpgradeRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<StasisUpgradeRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    /**
     * What the rung <i>above</i> {@code tier} costs. Keyed by the tier being
     * upgraded <b>to</b>, so the table reads the way the wiki's recipe table
     * does, and {@code SPACER} has no entry because there is nothing above it.
     */
    private static @Nullable Item costOf(StasisTier upgradeTo) {
        switch (upgradeTo) {
            case INTERMEDIATE:
                return Items.ENDER_EYE;
            case ADVANCED:
                return Items.GOLD_INGOT;
            case SPACER:
                return Items.DIAMOND;
            default:
                return null;
        }
    }

    /** The chamber in the grid, if the grid is exactly one chamber and its upgrade cost. */
    private static @Nullable ItemStack upgradable(CraftingInput input) {
        ItemStack chamber = ItemStack.EMPTY;
        ItemStack other = ItemStack.EMPTY;
        int filled = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            filled++;
            if (s.getItem() instanceof StasisChamberItem) {
                if (!chamber.isEmpty()) {
                    return null; // two chambers - which one is being upgraded?
                }
                chamber = s;
            } else {
                if (!other.isEmpty()) {
                    return null;
                }
                other = s;
            }
        }
        if (filled != 2 || chamber.isEmpty() || other.isEmpty()) {
            return null;
        }
        StasisTier up = ((StasisChamberItem) chamber.getItem()).tier().next();
        if (up == null) {
            return null; // a Spacer is already the top rung
        }
        Item cost = costOf(up);
        return cost != null && other.is(cost) ? chamber : null;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return upgradable(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack chamber = upgradable(input);
        if (chamber == null) {
            return ItemStack.EMPTY;
        }
        StasisTier up = ((StasisChamberItem) chamber.getItem()).tier().next();
        if (up == null) {
            return ItemStack.EMPTY;
        }
        ItemStack out = new ItemStack(ModItems.stasisChamber(up));
        // The horse rides the upgrade, and nothing else does. Without this line
        // an occupied chamber upgrades into an empty one and the animal is gone
        // for good.
        //
        // STASIS_AT_STUD is deliberately NOT carried, and today it cannot
        // matter: only a SPACER can be marked, and a SPACER is the top rung, so
        // no marked chamber ever reaches this method. Add a rung above SPACER
        // and that stops being true - an upgrade would then quietly bring a mare
        // back in from stud, and this is the line to revisit.
        StasisSnapshot inside = StasisChamberItem.snapshotOf(chamber);
        if (inside != null) {
            out.set(ModDataComponents.STASIS_SNAPSHOT.get(), inside);
        }
        return out;
    }

    @Override
    public RecipeSerializer<StasisUpgradeRecipe> getSerializer() {
        return SERIALIZER;
    }
}
