package com.example.horsegenetics.neoforge.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * <b>The rescuing braid</b>: worked into a horse's mane or tail, and spent the
 * moment the horse would die to put it back in its stall instead.
 *
 * <p>It is the second item in the mod that saves a horse without being used, and
 * the two are deliberately different instruments. An
 * {@link EmergencyStasisChamberItem emergency stasis chamber} is <i>yours</i> -
 * carried, covering every horse you own, and it answers by taking the animal out
 * of the world. A braid is <i>the horse's</i> - worn, covering only the animal
 * wearing it, and it answers by moving it somewhere you will find it, still an
 * animal, standing in its own stall. Which is why a braid goes first when a
 * horse has both: it is the more specific instrument, and a horse that has gone
 * home is a horse the chamber no longer has to catch.
 *
 * <p>Everything it does is in {@code server/RescuingBraidHandler}. This class is
 * the item, and there is nothing to it: a braid has no use of its own, no
 * durability and no state beyond its colour. Putting one on is the ordinary gear
 * click - see {@code HorseTackSlot} - and taking it off again gets it back
 * whole.
 *
 * <h2>The colour is vanilla's, on purpose</h2>
 * A braid is in {@code #minecraft:dyeable} and carries vanilla's
 * {@code dyed_color}, which buys the whole colour story for nothing: dyeing one
 * in a crafting grid, mixing two dyes, washing it out in a cauldron, the "Dyed"
 * tooltip line and the tinted inventory icon are all vanilla's code doing what
 * it already does. The mod's own three-zone {@code tack_tint} is the wrong
 * shape here - it exists because a saddle has a seat, a bridle and its
 * fittings, and a braid is one plait of one colour.
 */
public class RescuingBraidItem extends Item {

    @SuppressWarnings("deprecation") // Item(Properties) - DeferredRegister supplies the id-carrying Properties
    public RescuingBraidItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        adder.accept(Component.literal("Worn in a horse's mane or tail.")
                .withStyle(ChatFormatting.GRAY));
        adder.accept(Component.literal("Breaks the moment the horse would die, and sends it - "
                        + "and you, if you are aboard - back to its stall.")
                .withStyle(ChatFormatting.GRAY));
        adder.accept(Component.literal("One use. The horse needs a stall, or you a holding pen.")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
