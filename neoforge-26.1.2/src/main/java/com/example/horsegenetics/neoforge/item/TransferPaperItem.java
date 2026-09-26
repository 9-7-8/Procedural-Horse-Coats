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
 * {@link SignedTransferPaperItem signed} paper naming that animal. What makes
 * that safe is the <b>ownership check on the horse</b> - a paper will only ever
 * write out an animal you own. The binding is a smaller thing on top: it stops
 * someone lifting <i>your</i> blank out of a chest and using it.
 *
 * <p><b>An unbound blank binds to whoever first signs with it.</b> Crafting was
 * once the only thing that bound one, so a blank from anywhere else - a creative
 * tab, {@code /give}, a loot table, a test kit - arrived with no bearer and was
 * refused outright, with a message about it not being your paper when it was
 * nobody's. It is not a hole: there was no owner to protect, and the horse still
 * has to be yours.
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
