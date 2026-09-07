package com.example.horsegenetics.neoforge.item;

import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.data.PaperBearer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * A <b>blank transfer paper</b>: eight paper around one horse hair, bound at
 * the bench to whoever crafted it ({@code TransferPaperHandler.onCrafted}).
 *
 * <p>Right-click one of your own horses with it and it becomes a
 * {@link SignedTransferPaperItem signed} paper naming that animal. The binding
 * is what makes that safe - a blank is not a blank cheque anyone can fill in,
 * it is <i>your</i> stationery, and it will only write out a horse you own.
 *
 * <p>The signing itself is in {@code TransferPaperHandler} rather than here,
 * because it is a right-click on an <b>entity</b>, and the mod already routes
 * every horse interaction through a {@code PlayerInteractEvent.EntityInteract}
 * handler so that vanilla's "climb on and ride" prediction can be cancelled on
 * both sides at once.
 */
public class TransferPaperItem extends Item {

    @SuppressWarnings("deprecation")
    public TransferPaperItem(Properties properties) {
        super(properties);
    }

    /** Who this blank belongs to, or {@code null} if it was never bound. */
    public static PaperBearer bearerOf(ItemStack stack) {
        return stack.get(ModDataComponents.PAPER_BEARER.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        PaperBearer bearer = bearerOf(stack);
        if (bearer == null) {
            adder.accept(Component.translatable("tooltip.horsegenetics.transfer_paper.unbound")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        adder.accept(Component.translatable("tooltip.horsegenetics.transfer_paper.bound",
                Component.literal(bearer.name()).withStyle(ChatFormatting.WHITE))
                .withStyle(ChatFormatting.GRAY));
        adder.accept(Component.translatable("tooltip.horsegenetics.transfer_paper.hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
