package com.example.horsegenetics.neoforge.item;

import com.example.horsegenetics.common.breed.BreedLineage;
import com.example.horsegenetics.common.horse.TransferDeed;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * A <b>signed transfer paper</b>: a claim on one named horse, redeemable by
 * whoever is holding it. Right-click the horse it names and that horse becomes
 * yours; the paper is spent.
 *
 * <p>Two sources, and they behave identically once written - an owner signing a
 * blank against their own horse, and the cowboy selling one out of his herd.
 * That is the point of the item: a horse can be sold without either party ever
 * standing next to it, and the buyer can sell the claim on again before they
 * collect.
 *
 * <p>The stack's <b>name</b> is the horse's name, so a merchant screen full of
 * these reads as a list of horses rather than a list of identical papers. The
 * tooltip carries the rest of the provenance, including who <i>bred</i> the
 * animal - which redemption never changes.
 */
public class SignedTransferPaperItem extends Item {

    @SuppressWarnings("deprecation")
    public SignedTransferPaperItem(Properties properties) {
        super(properties);
    }

    /** The horse this paper is a claim on, or {@code null} if it is somehow unwritten. */
    public static TransferDeed deedOf(ItemStack stack) {
        return stack.get(ModDataComponents.HORSE_DEED.get());
    }

    @Override
    public Component getName(ItemStack stack) {
        TransferDeed deed = deedOf(stack);
        if (deed == null) {
            return super.getName(stack);
        }
        return Component.translatable("item.horsegenetics.signed_transfer_paper.named",
                Component.literal(deed.horseName()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        TransferDeed deed = deedOf(stack);
        if (deed == null) {
            adder.accept(Component.translatable("tooltip.horsegenetics.transfer_paper.unwritten")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        // The deed carries a BreedLineage *token*, not a breed id, so it is parsed
        // rather than looked up: a cross used to render as "Cross:arabian+friesian"
        // here, and a spliced line would have been worse.
        adder.accept(Component.literal(
                        BreedLineage.parse(deed.breed().orElse(null)).displayName())
                .withStyle(ChatFormatting.AQUA));
        deed.bredBy().ifPresent(breeder -> adder.accept(
                Component.translatable("tooltip.horsegenetics.transfer_paper.bred_by",
                        Component.literal(breeder).withStyle(ChatFormatting.WHITE))
                        .withStyle(ChatFormatting.GRAY)));
        if (!deed.issuedBy().isBlank() && !deed.issuedBy().equals(deed.bredBy().orElse(null))) {
            adder.accept(Component.translatable("tooltip.horsegenetics.transfer_paper.issued_by",
                    Component.literal(deed.issuedBy()).withStyle(ChatFormatting.WHITE))
                    .withStyle(ChatFormatting.GRAY));
        }
        adder.accept(Component.translatable("tooltip.horsegenetics.transfer_paper.redeem")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
