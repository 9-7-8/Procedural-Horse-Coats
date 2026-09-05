package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.neoforge.data.GeneDatabaseData;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.item.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * "Write research paper" (roadmap wiki &sect;16.2): the player spends one book
 * to write a {@code research_paper} for a gene they have discovered - what makes
 * knowledge shareable rather than hoardable. Triggered by
 * {@code WriteResearchPaperPayload} from the browser; re-checked here.
 */
public final class ResearchPaperWriter {

    public static void write(ServerPlayer player, String geneKey) {
        Gene gene = Genes.byKeyOrNull(geneKey);
        if (gene == null) {
            return;
        }
        if (!GeneDatabaseData.get(((net.minecraft.server.level.ServerLevel) player.level()).getServer()).knows(player.getUUID(), geneKey)) {
            player.sendSystemMessage(Component.translatable("message.horsegenetics.paper.unknown"));
            return;
        }
        int bookSlot = findBook(player);
        if (bookSlot < 0) {
            player.sendSystemMessage(Component.translatable("message.horsegenetics.paper.need_book"));
            return;
        }
        player.getInventory().getItem(bookSlot).shrink(1);

        ItemStack paper = new ItemStack(ModItems.RESEARCH_PAPER.get());
        paper.set(ModDataComponents.RESEARCH_GENE.get(), geneKey);
        if (!player.getInventory().add(paper)) {
            player.drop(paper, false);
        }
        player.sendSystemMessage(Component.translatable(
                "message.horsegenetics.paper.written", Component.literal(gene.name())));
    }

    private static int findBook(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(Items.BOOK)) {
                return i;
            }
        }
        return -1;
    }

    private ResearchPaperWriter() {
    }
}
