package com.example.horsegenetics.neoforge.item;

import com.example.horsegenetics.common.pack.HarnessTier;
import com.example.horsegenetics.neoforge.ServerConfig;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * <b>A storage harness</b> - the strap over the rump and the metal frame on
 * each flank that a chest hangs in. A horse wears one before it can carry a
 * chest at all ({@code HorseTackSlot.HARNESS}), and it takes a share of the
 * load off the horse; how large a share is the metal of its fittings
 * ({@link HarnessTier}) and the server's {@code packs.harness_best_reduction}.
 *
 * <p>One class, an item per tier, because a recipe cannot tell "with a
 * component" from "without" - the house rule in {@code bake-recipe-reference}.
 * The leather is dyeable ({@code minecraft:dyeable}); the fittings are not.
 *
 * <p>The tooltip reads the live setting rather than a lang string with a
 * number in it, so a server that has made harnesses matter says so on the
 * item.
 */
public class StorageHarnessItem extends Item {

    private final HarnessTier tier;

    @SuppressWarnings("deprecation") // Item(Properties) - DeferredRegister supplies the id-carrying Properties
    public StorageHarnessItem(HarnessTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public HarnessTier tier() {
        return tier;
    }

    /** The share of the load this harness takes off, on this server's setting. */
    public double reduction() {
        return tier.reduction(ServerConfig.packHarnessBestReduction());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        adder.accept(Component.literal("Worn in a horse's harness slot. A chest hangs from it.")
                .withStyle(ChatFormatting.GRAY));
        long percent = Math.round(100.0 * reduction());
        adder.accept(Component.literal(percent > 0
                        ? "What the horse carries weighs " + percent + "% less."
                        : "It carries the chests and takes nothing off the load.")
                .withStyle(ChatFormatting.GRAY));
        adder.accept(Component.literal("The leather takes dye.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
