package com.example.horsegenetics.neoforge.data.loot;

import com.example.horsegenetics.neoforge.compat.ModdedArmour;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.data.SaddleTint;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Chest-loot injection of <b>horse tack</b> - one piece, of any type, dyed if it
 * is a piece that can be.
 *
 * <p>Vanilla puts horse armour in a handful of tables and a saddle in a few more,
 * at odds that suit a game where a horse is a means of transport. In a mod whose
 * whole subject is horses that is far too thin: the owner plays on a heavily
 * modded server and asked for every armour type, dyed saddles and dyed leather
 * armour to be reachable from chest loot generally, not from eight named chests.
 *
 * <h2>What can come out, and why in these proportions</h2>
 *
 * <p>The pool is weighted by what the piece is worth, not by what it is made of.
 * Leather is the commonest because a dyed leather piece is the one this mod has
 * the most to say about; diamond is the rarest because finding one in a chest
 * should still be the best thing in it.
 *
 * <p><b>Every modded armour shares one bucket.</b> {@link ModdedArmour} registers
 * one horse armour per metal any installed mod adds, and a 319-jar pack can bring
 * dozens - which, given a weight each, would make vanilla's four armours the rare
 * ones and turn the pool into a survey of the player's modlist. They are drawn
 * uniformly from inside a single share instead, so the pack's metals are
 * represented without swamping the thing they are compatible with. With no such
 * mods installed the bucket is not in the draw at all.
 *
 * <h2>The dye is ours, not vanilla's</h2>
 *
 * <p>A saddle and a leather horse armour both get a {@code tack_tint}, the mod's
 * own three-zone component, rather than vanilla's {@code dyed_color} - the same
 * choice and for the same reason as {@link SetTackTintFunction}: {@code dyed_color}
 * holds one value, reaches only the blanket, and does nothing whatsoever to a
 * saddle. Three zones is also what makes a found piece look found, since the two
 * leather zones roll separately.
 *
 * <p><b>The fittings are deliberately cheap.</b> A chest rolls iron, copper,
 * bone, coal, basalt or gold and never diamond, emerald or amethyst. The
 * leatherworker's five tiers are a ladder whose top rung is lapis-dyed tack on
 * diamond, and that ladder is worth climbing only if a dungeon chest cannot hand
 * you its last rung.
 */
public class AddHorseTackModifier extends LootModifier {

    public static final MapCodec<AddHorseTackModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            codecStart(inst).and(
                    Codec.FLOAT.optionalFieldOf("chance", 0.22F)
                            .forGetter(m -> m.chance))
                    .apply(inst, AddHorseTackModifier::new));

    private static final int W_LEATHER_ARMOUR = 10;
    private static final int W_SADDLE = 8;
    private static final int W_IRON = 6;
    private static final int W_GOLD = 4;
    private static final int W_DIAMOND = 1;
    /** One share between all of them, however many metals the pack brought. */
    private static final int W_MODDED = 5;

    /** What a found piece's hardware can be: workaday metals and stone, no gems. */
    private static final int[] FOUND_FITTINGS = {
            SaddleTint.IRON, SaddleTint.IRON, SaddleTint.COPPER,
            SaddleTint.BONE, SaddleTint.COAL, SaddleTint.BASALT, SaddleTint.GOLD };

    private final float chance;

    public AddHorseTackModifier(LootItemCondition[] conditions, int priority, float chance) {
        super(conditions, priority);
        this.chance = chance;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        RandomSource random = context.getRandom();
        if (random.nextFloat() >= this.chance) {
            return loot;
        }
        loot.add(rollPiece(random));
        return loot;
    }

    private static ItemStack rollPiece(RandomSource random) {
        List<ModdedArmour.Armour> modded = ModdedArmour.armours();
        int total = W_LEATHER_ARMOUR + W_SADDLE + W_IRON + W_GOLD + W_DIAMOND
                + (modded.isEmpty() ? 0 : W_MODDED);
        int roll = random.nextInt(total);
        if ((roll -= W_LEATHER_ARMOUR) < 0) {
            return tinted(new ItemStack(Items.LEATHER_HORSE_ARMOR), random);
        }
        if ((roll -= W_SADDLE) < 0) {
            return tinted(new ItemStack(Items.SADDLE), random);
        }
        if ((roll -= W_IRON) < 0) {
            return new ItemStack(Items.IRON_HORSE_ARMOR);
        }
        if ((roll -= W_GOLD) < 0) {
            return new ItemStack(Items.GOLDEN_HORSE_ARMOR);
        }
        // The last weighted branch, written the same way as the others rather
        // than as a bare else: the modded bucket sits after it, and a reader
        // should not have to work out which of the two is the fallthrough.
        if (roll - W_DIAMOND < 0) {
            return new ItemStack(Items.DIAMOND_HORSE_ARMOR);
        }
        return new ItemStack(modded.get(random.nextInt(modded.size())).item().get());
    }

    private static ItemStack tinted(ItemStack stack, RandomSource random) {
        // Seat and bridle roll independently out of the whole wheel. The
        // leatherworker draws his two from one palette because his tiers are a
        // progression; a piece somebody lost in a dungeon has no such excuse, and
        // an unrelated pair is exactly what makes it read as second-hand.
        stack.set(ModDataComponents.TACK_TINT.get(),
                new SaddleTint(randomDye(random), randomDye(random),
                        FOUND_FITTINGS[random.nextInt(FOUND_FITTINGS.length)]));
        return stack;
    }

    private static int randomDye(RandomSource random) {
        // & 0xFFFFFF: the component is an RGB colour and the diffuse colour
        // arrives with an alpha byte on it. The bench and SetTackTintFunction
        // both do the same.
        return DyeColor.values()[random.nextInt(DyeColor.values().length)]
                .getTextureDiffuseColor() & 0xFFFFFF;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
