package com.example.horsegenetics.neoforge.item;

import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.neoforge.data.GeneDatabaseData;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * A <b>research paper</b> (roadmap wiki &sect;16.2). Carries a
 * {@code research_gene} component naming one gene. Right-clicking it adds that
 * gene to the reader's {@link GeneDatabaseData gene database} and unlocks the
 * gene's gene-carrot recipe; the paper is consumed. It is also the ingredient
 * the paper-parameterised {@code KnownGeneSpliceRecipe} matches on.
 *
 * <p>Three sources: chest loot ({@code AddResearchPaperModifier}), other
 * players, and writing one from your own database in the Horse Browser at the
 * cost of a book.
 */
public class ResearchPaperItem extends Item {

    @SuppressWarnings("deprecation")
    public ResearchPaperItem(Properties properties) {
        super(properties);
    }

    /** The gene a stack documents, or {@code null} if the component is missing / unknown. */
    public static Gene geneOf(ItemStack stack) {
        String key = stack.get(ModDataComponents.RESEARCH_GENE.get());
        return key == null ? null : Genes.byKeyOrNull(key);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Gene gene = geneOf(stack);
        if (gene == null) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            boolean added = GeneDatabaseData.get(serverLevel.getServer())
                    .read(serverPlayer, gene);
            serverPlayer.sendSystemMessage(Component.translatable(
                    added ? "message.horsegenetics.paper.read" : "message.horsegenetics.paper.known",
                    Component.literal(gene.name())));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        Gene gene = geneOf(stack);
        if (gene == null) {
            adder.accept(Component.literal("Blank").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        adder.accept(Component.literal(gene.name() + "  ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal(gene.rarity().name().toLowerCase())
                        .withStyle(ChatFormatting.GRAY)));
        String blurb = gene.description();
        if (!blurb.isBlank()) {
            adder.accept(Component.literal(blurb).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
