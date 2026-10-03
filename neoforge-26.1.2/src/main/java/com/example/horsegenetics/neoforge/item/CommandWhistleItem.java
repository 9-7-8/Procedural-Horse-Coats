package com.example.horsegenetics.neoforge.item;

import com.example.horsegenetics.common.care.HorseOrders;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * <b>The command whistle</b> (wiki/item-whistles.html#command): hold use on a horse of
 * yours and a wheel of orders opens; release over one to give it. Sneak and hold to order
 * every horse of yours within {@link HorseOrders#REACH_BLOCKS} blocks.
 *
 * <p>The item does nothing by itself: the client's {@code CommandWhistleClient} catches
 * the use key before vanilla turns it into an interaction (so the horse is not mounted
 * and its inventory does not open), opens {@code CommandWheelScreen}, and the pick goes
 * to the server as a {@code CommandHorsePayload}, where {@code HorseOrdering} re-checks
 * everything. A plain right-click at nothing, not sneaking, does nothing.
 */
public class CommandWhistleItem extends Item {

    // Item(Properties) is @Deprecated to nudge modders toward the id-carrying
    // Properties that DeferredRegister.Items#registerItem already supplies here.
    @SuppressWarnings("deprecation")
    public CommandWhistleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        boolean first = true;
        for (String line : HorseOrders.tooltip()) {
            adder.accept(Component.literal(line).withStyle(first ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY));
            first = false;
        }
    }
}
