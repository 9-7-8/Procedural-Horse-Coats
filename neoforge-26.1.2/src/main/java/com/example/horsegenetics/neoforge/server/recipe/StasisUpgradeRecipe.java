package com.example.horsegenetics.neoforge.server.recipe;

import com.example.horsegenetics.common.horse.StasisTier;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.data.StasisSnapshot;
import com.example.horsegenetics.neoforge.item.ModItems;
import com.example.horsegenetics.neoforge.item.StasisChamberItem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * <b>Climbing the stasis ladder.</b> A chamber plus one ingredient becomes the
 * chamber one rung up: an eye of ender for Intermediate, a gold ingot for
 * Advanced, a diamond for Spacer.
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
 *
 * <h2>But it <i>is</i> three recipes, one per rung</h2>
 * It used to be one instance matching all three steps, which is tidier Java and
 * was wrong for the only thing that matters here: <b>a recipe can only advertise
 * one set of ingredients</b>. {@code placementInfo()} has no way to say "a
 * chamber and whichever item its own tier calls for", so one instance could
 * never be drawn, and an undrawable recipe is one JEI and the recipe book both
 * skip - which is how the whole stasis family ended up craftable only by a
 * player who already knew the answer. See {@link StasisChamberRecipe#isSpecial()}.
 *
 * <p>So the tier being upgraded <b>to</b> is a field, read from the recipe JSON's
 * {@code "to"}, and {@code data/horsegenetics/recipe/} carries one file per
 * rung. The cost table below is still the single place the prices live.
 */
public class StasisUpgradeRecipe extends CustomRecipe {

    /**
     * The tier's stable string form, which is what the recipe JSON writes. Kept
     * here rather than on {@link StasisTier} because {@code common/} imports no
     * codecs at all - see the hard rules.
     */
    private static final Codec<StasisTier> TIER_CODEC = Codec.STRING.comapFlatMap(
            id -> {
                StasisTier tier = StasisTier.byId(id);
                return tier == null
                        ? DataResult.error(() -> "unknown stasis tier '" + id + "'")
                        : DataResult.success(tier);
            },
            StasisTier::id);

    private static final StreamCodec<ByteBuf, StasisTier> TIER_STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(id -> {
                StasisTier tier = StasisTier.byId(id);
                if (tier == null) {
                    throw new IllegalArgumentException("unknown stasis tier '" + id + "'");
                }
                return tier;
            }, StasisTier::id);

    /**
     * A real codec with a real field, so unlike the mod's other special recipes
     * there is no shared-instance trap here: {@code StreamCodec.unit} is what
     * makes decoding a fresh object unencodable, and this encodes its tier like
     * any ordinary payload. See {@link CarrotCombineRecipe#INSTANCE} for the
     * shape of that bug, and {@code ModGameTests.EVERY_RECIPE_ENCODES} for the
     * test that catches it.
     */
    public static final MapCodec<StasisUpgradeRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            TIER_CODEC.fieldOf("to").forGetter(StasisUpgradeRecipe::upgradeTo)
    ).apply(i, StasisUpgradeRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, StasisUpgradeRecipe> STREAM_CODEC =
            StreamCodec.composite(TIER_STREAM_CODEC, StasisUpgradeRecipe::upgradeTo, StasisUpgradeRecipe::new);

    public static final RecipeSerializer<StasisUpgradeRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    /** Which rung this file makes. The rung below it is what goes in the grid. */
    private final StasisTier upgradeTo;

    private volatile @Nullable PlacementInfo placement;

    public StasisUpgradeRecipe(StasisTier upgradeTo) {
        this.upgradeTo = upgradeTo;
    }

    public StasisTier upgradeTo() {
        return upgradeTo;
    }

    /**
     * What the rung <i>above</i> {@code tier} costs. Keyed by the tier being
     * upgraded <b>to</b>, so the table reads the way the wiki's recipe table
     * does, and {@code BASIC} has no entry because nothing upgrades into the
     * bottom rung.
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

    /** The rung below {@code upgradeTo}, or {@code null} if it is the bottom one. */
    private static @Nullable StasisTier below(StasisTier upgradeTo) {
        for (StasisTier tier : StasisTier.values()) {
            if (tier.next() == upgradeTo) {
                return tier;
            }
        }
        return null;
    }

    /**
     * The chamber in the grid, if the grid is exactly <b>this recipe's</b>
     * chamber and its cost. A chamber of any other tier is another file's
     * business, which is what keeps the three from matching each other's grids.
     */
    private @Nullable ItemStack upgradable(CraftingInput input) {
        Item cost = costOf(upgradeTo);
        StasisTier from = below(upgradeTo);
        if (cost == null || from == null) {
            return null;
        }
        ItemStack chamber = ItemStack.EMPTY;
        ItemStack other = ItemStack.EMPTY;
        int filled = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            filled++;
            if (s.getItem() instanceof StasisChamberItem held && held.tier() == from) {
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
        return other.is(cost) ? chamber : null;
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
        ItemStack out = new ItemStack(ModItems.stasisChamber(upgradeTo));
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

    /**
     * <b>Not special, so the recipe is findable.</b> See
     * {@link StasisChamberRecipe#isSpecial()}.
     */
    @Override
    public boolean isSpecial() {
        return false;
    }

    /** The rung below, and the one item it costs to climb. */
    @Override
    public PlacementInfo placementInfo() {
        PlacementInfo cached = placement;
        if (cached != null) {
            return cached;
        }
        Item cost = costOf(upgradeTo);
        StasisTier from = below(upgradeTo);
        if (cost == null || from == null) {
            return PlacementInfo.NOT_PLACEABLE;
        }
        PlacementInfo built = PlacementInfo.create(List.of(
                Ingredient.of(ModItems.stasisChamber(from)),
                Ingredient.of(cost)));
        if (!built.isImpossibleToPlace()) {
            placement = built;
        }
        return built;
    }

    @Override
    public RecipeSerializer<StasisUpgradeRecipe> getSerializer() {
        return SERIALIZER;
    }
}
