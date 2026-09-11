package com.example.horsegenetics.neoforge.block;

import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * <b>The papers an Equine Research Shelf is holding</b> - in real slots, like a
 * chest.
 *
 * <h2>Slots, not a list</h2>
 * It used to keep a set of gene keys and turn papers into and out of items
 * through a single filing slot and a clickable list. The owner put a book in,
 * saw nothing happen, and asked for it to "just be a UI like a chest"
 * (2026-09-10) - so it is: {@link #SLOTS} slots of research papers, put in and
 * taken out by hand, instantly. The one rule the set used to make structural
 * is now a slot rule instead - <b>one paper per gene</b> - enforced by
 * {@link #holdsElsewhere} from the menu's slots.
 *
 * <p>Copying a gene onto a blank book is still here, still takes time by
 * rarity, and reads the gene list from these slots: take the original out and
 * the shelf stops being able to copy it.
 */
public class EquineResearchShelfBlockEntity extends BlockEntity {

    /** A double chest's worth - six rows of nine. */
    public static final int SLOTS = 54;

    private final SimpleContainer papers = new SimpleContainer(SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            EquineResearchShelfBlockEntity.this.setChanged();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    };

    public EquineResearchShelfBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESEARCH_SHELF.get(), pos, state);
    }

    /** The slots themselves - the menu wraps these, and breaking the block drops them. */
    public SimpleContainer papers() {
        return papers;
    }

    /** Is this a research paper with a gene written on it - the only thing the shelf takes? */
    public static boolean isFiledPaper(ItemStack stack) {
        return stack.is(ModItems.RESEARCH_PAPER.get()) && !geneOf(stack).isEmpty();
    }

    /** The gene a paper names, or {@code ""}. */
    public static String geneOf(ItemStack stack) {
        String key = stack.get(ModDataComponents.RESEARCH_GENE.get());
        return key == null ? "" : key;
    }

    /**
     * Every gene the papers in {@code container} name, sorted by gene name - the
     * Craft tab's list. Static and container-based so the client's copy of the
     * menu, which has the synced slots but no block entity, reads the same list.
     */
    public static List<String> genesIn(net.minecraft.world.Container container) {
        Set<String> seen = new LinkedHashSet<>();
        for (int i = 0; i < container.getContainerSize(); i++) {
            String key = geneOf(container.getItem(i));
            if (!key.isEmpty()) {
                seen.add(key);
            }
        }
        List<String> out = new ArrayList<>(seen);
        out.sort(Comparator.comparing(EquineResearchShelfBlockEntity::displayName,
                String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(out);
    }

    /** Does any slot other than {@code exceptSlot} already hold a paper for {@code geneKey}? */
    public static boolean holdsElsewhere(net.minecraft.world.Container container, String geneKey, int exceptSlot) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (i != exceptSlot && geneKey.equals(geneOf(container.getItem(i)))) {
                return true;
            }
        }
        return false;
    }

    public boolean stores(String geneKey) {
        return holdsElsewhere(papers, geneKey, -1);
    }

    /** The name a row shows: the gene's, or its raw key if this build has no such gene. */
    public static String displayName(String geneKey) {
        Gene gene = Genes.byKeyOrNull(geneKey);
        return gene == null ? geneKey : gene.name();
    }

    /**
     * <b>Drive the copy for whoever has this shelf open.</b> The work lives on
     * the menu (it needs the selected gene and the book slot, neither of which
     * is block state), so the ticker's whole job is to find the menus looking at
     * this block and tick them - which is also what lets two players at one
     * shelf each copy their own gene onto their own book.
     */
    public static void tick(net.minecraft.world.level.Level level, BlockPos pos,
                            BlockState state, EquineResearchShelfBlockEntity shelf) {
        if (level.isClientSide()) {
            return;
        }
        for (net.minecraft.world.entity.player.Player player : level.players()) {
            if (player.containerMenu instanceof com.example.horsegenetics.neoforge.menu.ResearchShelfMenu menu
                    && menu.isFor(shelf)) {
                menu.tickCopy();
            }
        }
    }

    /**
     * <b>Give the papers back when the shelf is broken.</b> Here, where vanilla's
     * {@code BlockEntity.preRemoveSideEffects} drops a container's contents,
     * because this is the last moment the block entity still exists. The old
     * shelf did it from the block's {@code affectNeighborsAfterRemoval}, which
     * runs after the block entity is gone - so, as far as the code shows,
     * breaking a shelf silently lost its whole collection.
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null) {
            net.minecraft.world.Containers.dropContents(this.level, pos, papers);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, papers.getItems());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        papers.getItems().replaceAll(s -> ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, papers.getItems());
    }
}
