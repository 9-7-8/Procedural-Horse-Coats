package com.example.horsegenetics.neoforge.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * <b>A turnout ticket sends a horse to the horse realm and gives it up.</b>
 * Right-click one of your horses and it goes to the realm's arrival field and
 * stops being yours - untamed, unowned, healed, tack handed back, and marked as
 * a wild spawn so a band will take it in. The ticket is spent.
 *
 * <h2>It is the two existing halves in one item</h2>
 * The <b>interdimensional ticket</b> already moves a horse to another world, and
 * the <b>freedom stick</b> already releases one - but only while you are
 * standing in the realm with it, which means the honest way to retire a horse
 * today is to lead it through a portal first. This is that trip as one
 * right-click, and it is the only route that can release a horse you are not
 * standing next to in the realm.
 *
 * <p>Both halves are reused rather than reimplemented: the travel is
 * {@code TicketHandler.arrive} (the same particles, sound and lead handling as
 * every other ticket) and the release is {@code HorseRelease.makeWild}, which is
 * the single definition of what "wild" means and is the only thing that
 * remembers the temper reset and the wild-spawn mark.
 *
 * <h2>It is not reversible, and the tooltip says so</h2>
 * The horse keeps its genotype, name, breed, pedigree and bond - releasing one
 * is a gift to whoever tames it next, not a deletion - but <i>you</i> lose it,
 * its stall binding is cleared, and an ender whistle bound to it stops
 * answering. There is no ticket that fetches it back.
 */
public class TurnoutTicketItem extends Item {

    @SuppressWarnings("deprecation") // Item(Properties) - DeferredRegister supplies the id-carrying Properties
    public TurnoutTicketItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        adder.accept(Component.literal("Sends a horse to the horse realm and sets it free there.")
                .withStyle(ChatFormatting.GRAY));
        adder.accept(Component.literal("It stops being yours. Its tack comes back to you, "
                        + "its stall is freed, and nothing brings it home.")
                .withStyle(ChatFormatting.DARK_GRAY));
        adder.accept(Component.literal("Single use. Works from any world.")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
