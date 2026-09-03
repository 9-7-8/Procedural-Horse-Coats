package com.example.horsegenetics.neoforge.item;

import com.example.horsegenetics.common.genetics.GeneCodeDisplay;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.data.StoredGenome;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * The seed jar. An <b>empty</b> jar carries nothing; a <b>filled</b>
 * ({@code stallion_seed_jar}) one carries a {@link StoredGenome} data
 * component. The only behaviour on the item itself is the tooltip - collecting
 * from a stallion and impregnating a mare are handled in
 * {@code server/StallionSeedJarHandler}, alongside the other
 * item-on-horse interactions.
 */
public class SeedJarItem extends Item {

    // Item(Properties) is @Deprecated to push modders toward the id-carrying
    // Properties that DeferredRegister.Items#registerItem already supplies here.
    @SuppressWarnings("deprecation")
    public SeedJarItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        StoredGenome stored = stack.get(ModDataComponents.STORED_GENOME.get());
        if (stored == null) {
            adder.accept(Component.literal("Empty").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        String sire = stored.sourceName().isBlank() ? "unknown stallion" : stored.sourceName();
        adder.accept(Component.literal("Sire: " + sire).withStyle(ChatFormatting.GRAY));
        try {
            adder.accept(Component.literal(GeneCodeDisplay.shortForm(Genotype.parse(stored.genotypeCode())))
                    .withStyle(ChatFormatting.DARK_GRAY));
        } catch (RuntimeException ignored) {
            // A genotype code from an older/other registry won't parse here; the
            // jar is still usable, just show no summary line.
        }
    }
}
