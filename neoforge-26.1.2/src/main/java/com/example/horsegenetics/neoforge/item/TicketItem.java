package com.example.horsegenetics.neoforge.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * <b>A ticket sends a horse home.</b> Right-click one of your horses with a
 * ticket and it goes to its stall; the ticket is spent. The reverse of a
 * whistle, which brings a horse to <i>you</i> - settled that way on the roadmap
 * so the two items never overlap.
 *
 * <p><b>It is not bound to a horse.</b> The horse you click is the horse that
 * travels, so one ticket in a pocket serves whichever animal has got itself
 * stuck. What the tier buys is <b>reach</b> - how far apart the horse and its
 * stall are allowed to be - which is also the order the recipes climb in
 * (a ticket, then an ender pearl, then an eye and blaze powder).
 *
 * <p>Every tier is one use. See {@code server/TicketHandler} for the interaction
 * itself; the item is only the tier and the words.
 */
public class TicketItem extends Item {

    /** How far a ticket reaches. */
    public enum Tier {
        /** Overworld to overworld, and nowhere else. */
        BASIC("Sends a horse to its stall, in the overworld.",
                "Both the horse and its stall have to be in the overworld."),
        /** Any one dimension to itself - the horse dimension included. */
        BOUND("Sends a horse to its stall, in any world.",
                "The horse and its stall must still be in the same world."),
        /** Anywhere to anywhere. */
        INTERDIMENSIONAL("Sends a horse to its stall from any world.",
                "Crosses between worlds - the only ticket that does.");

        private final String what;
        private final String limit;

        Tier(String what, String limit) {
            this.what = what;
            this.limit = limit;
        }

        public String what() {
            return what;
        }

        public String limit() {
            return limit;
        }
    }

    private final Tier tier;

    @SuppressWarnings("deprecation") // Item(Properties) - DeferredRegister supplies the id-carrying Properties
    public TicketItem(Properties properties, Tier tier) {
        super(properties);
        this.tier = tier;
    }

    public Tier tier() {
        return tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        adder.accept(Component.literal(tier.what()).withStyle(ChatFormatting.GRAY));
        adder.accept(Component.literal(tier.limit()).withStyle(ChatFormatting.DARK_GRAY));
        adder.accept(Component.literal("Single use. The horse needs a stall sign.")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
