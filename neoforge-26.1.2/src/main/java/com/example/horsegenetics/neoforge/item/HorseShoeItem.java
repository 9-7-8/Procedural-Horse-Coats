package com.example.horsegenetics.neoforge.item;

import com.example.horsegenetics.common.gear.HorseShoes;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * <b>A horse shoe</b> - one to a hoof, worn in one of the four
 * {@code HorseTackSlot.SHOE_*} slots, each its own stack in its own metal.
 *
 * <p>One class and an item per metal, the five that ship
 * ({@code ModItems}, from {@link HorseShoes.Metal}) and one for every metal
 * another mod brings ({@code compat/ModdedShoes}). The metal is the item, not a
 * component, so the colour is a constant of the item: the inventory icon
 * ({@code client/MetalTintSource}) and the shoe drawn on the hoof
 * ({@code GeneticHorseRenderer}) both ask this for it, and cannot disagree.
 *
 * <p>A shoe does nothing to the horse yet. The stat bonus, powder snow and the
 * enchantments are the rest of #164, so the tooltip promises none of them.
 */
public class HorseShoeItem extends Item {

    private final int rgb;

    /** @param rgb the metal's colour, {@code 0xRRGGBB} */
    @SuppressWarnings("deprecation") // Item(Properties) - DeferredRegister supplies the id-carrying Properties
    public HorseShoeItem(int rgb, Properties properties) {
        super(properties);
        this.rgb = rgb & 0x00FFFFFF;
    }

    /** The colour this shoe is drawn in, as opaque ARGB - never {@code 0}, which means a bare hoof. */
    public int tint() {
        return HorseShoes.tint(rgb);
    }

    /** The tint of the shoe in {@code stack}, or {@code 0} when it is not a shoe. */
    public static int tintOf(ItemStack stack) {
        return stack.getItem() instanceof HorseShoeItem shoe ? shoe.tint() : 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        adder.accept(Component.literal("Worn on a hoof, in a horse's Gear tab.")
                .withStyle(ChatFormatting.GRAY));
        adder.accept(Component.literal("One shoe to a foot: a horse takes four.")
                .withStyle(ChatFormatting.GRAY));
    }
}
