package com.example.horsegenetics.neoforge.item;

import com.example.horsegenetics.common.gear.HorseShoes;
import com.example.horsegenetics.neoforge.HorseGenetics;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/**
 * <b>The two horse shoe enchantments</b> (#248), the mod's first: <b>Grip</b>
 * (less slide on ice) and <b>Roadster</b> (speed on a path or a road).
 *
 * <p>Both are data ({@code data/horsegenetics/enchantment/*.json}) and carry
 * no vanilla effect component: a horse's shoes are four gear slots of this
 * mod's, which no vanilla equipment slot reaches, so nothing vanilla would
 * ever run one. What this class does is the read: a worn stack's own levels,
 * as the {@link HorseShoes.Shoe} that {@code HorseShoes.bonus} takes. It is
 * the only place a stack's enchantments are read for a shoe, on both sides.
 *
 * <p>The conflict is the data's: the two name one {@code exclusive_set}, so an
 * anvil or a table puts one of them on a shoe and never both. A stack given
 * both by a command gets both; nothing here second-guesses it.
 */
public final class ShoeEnchantments {

    public static final ResourceKey<Enchantment> GRIP = key("grip");
    public static final ResourceKey<Enchantment> ROADSTER = key("roadster");

    /**
     * What counts as a path or a road for Roadster: {@code horsegenetics:shoe_roads},
     * a block tag, so a pack can add its own paving.
     */
    public static final TagKey<Block> ROADS =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "shoe_roads"));

    /** The enchantability a shoe is registered with: iron armour's, whatever its metal. */
    public static final int ENCHANTABILITY = 9;

    private ShoeEnchantments() {
    }

    private static ResourceKey<Enchantment> key(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT,
                Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, path));
    }

    /** The level of {@code enchantment} on {@code stack}, or {@code 0}. */
    public static int level(ItemStack stack, ResourceKey<Enchantment> enchantment) {
        ItemEnchantments on = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (on.isEmpty()) {
            return 0;
        }
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : on.entrySet()) {
            if (entry.getKey().is(enchantment)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }

    /** The shoe in {@code stack} with its own enchantments, or {@code null} when it is not a shoe: a bare hoof. */
    public static HorseShoes.@Nullable Shoe shoeOf(ItemStack stack) {
        HorseShoes.Metal tier = HorseShoeItem.tierOf(stack);
        return tier == null ? null : new HorseShoes.Shoe(tier, level(stack, GRIP), level(stack, ROADSTER));
    }
}
