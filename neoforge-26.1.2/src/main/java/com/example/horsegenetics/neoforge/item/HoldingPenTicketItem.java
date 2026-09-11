package com.example.horsegenetics.neoforge.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * <b>The holding pen ticket</b>: right-click any horse you own and it goes to
 * your holding pen ({@link HoldingPenSignItem}) - no stall of its own needed,
 * which is the point: the horse you tamed a minute ago has none. One use. It
 * reaches across worlds, since a pen you are sending strays to is somewhere you
 * are usually not. The mechanics live with the other tickets in
 * {@code server/TicketHandler}.
 */
public class HoldingPenTicketItem extends Item {

    @SuppressWarnings("deprecation") // Item(Properties) - DeferredRegister supplies the id-carrying Properties
    public HoldingPenTicketItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        adder.accept(Component.literal("Right-click any horse you own to send it to your holding pen.")
                .withStyle(ChatFormatting.GRAY));
        adder.accept(Component.literal("One use. Needs a holding pen sign hung first.")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
