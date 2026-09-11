package com.example.horsegenetics.neoforge.block;

import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneRarity;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
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
 * <b>An Equine Research Shelf: a chest of research papers, and a copier that
 * works like a furnace.</b>
 *
 * <h2>Slots, not a list</h2>
 * {@link #SLOTS} slots of research papers, put in and taken out by hand,
 * instantly - the owner asked for it to "just be a UI like a chest"
 * (2026-09-10). <b>One paper per gene</b>, enforced by {@link #holdsElsewhere}
 * from the menu's slots.
 *
 * <h2>Copying runs on the block, like a furnace</h2>
 * The book slot, the result slot, the gene picked and the progress all live
 * here and {@link #tick} advances them whether or not anyone has the screen
 * open. They used to live on the menu, which exists only while a player is
 * looking - so a copy stopped the moment the screen closed (owner-reported the
 * same day). A finished copy spends one book and lands in the result slot,
 * where copies of the same gene stack; with more books in, the next one starts
 * as soon as there is room. It needs the original still on the shelf, and
 * changing the pick or taking the original away starts the clock again.
 */
public class EquineResearchShelfBlockEntity extends BlockEntity {

    /** A double chest's worth - six rows of nine. */
    public static final int SLOTS = 54;

    /**
     * <b>Copying takes time, and how much is the gene's rarity.</b> One iron
     * ingot's smelt - 200 ticks, ten seconds - per rarity tier, so a common gene
     * is ten seconds and a mythic one a minute.
     */
    public static final int TICKS_PER_RARITY_TIER = 200;

    /** {@link ContainerData} indices - the furnace pattern. */
    public static final int DATA_PROGRESS = 0;
    public static final int DATA_TOTAL = 1;
    /** Index of the picked gene in {@link #genesIn}'s order, or -1 - how the client learns the pick. */
    public static final int DATA_SELECTED = 2;
    public static final int DATA_COUNT = 3;

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

    private final SimpleContainer book = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            EquineResearchShelfBlockEntity.this.setChanged();
        }
    };

    private final SimpleContainer result = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            EquineResearchShelfBlockEntity.this.setChanged();
        }
    };

    private String selectedGene = "";
    private int progress;

    /** The furnace's dataAccess: what the menu syncs to the client every tick. */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_TOTAL -> selectedGene.isEmpty() ? 0 : copyTicks(selectedGene);
                case DATA_SELECTED -> genesIn(papers).indexOf(selectedGene);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == DATA_PROGRESS) {
                progress = value;
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public EquineResearchShelfBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESEARCH_SHELF.get(), pos, state);
    }

    public SimpleContainer papers() {
        return papers;
    }

    public SimpleContainer book() {
        return book;
    }

    public SimpleContainer result() {
        return result;
    }

    public ContainerData data() {
        return data;
    }

    public String selectedGene() {
        return selectedGene;
    }

    /** Pick the gene to copy. A new pick restarts the clock; the same pick is a no-op. */
    public void select(String geneKey) {
        String next = geneKey == null ? "" : geneKey;
        if (!next.equals(selectedGene)) {
            selectedGene = next;
            progress = 0;
            setChanged();
        }
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
     * Copy tab's list. Static and container-based so the client's copy of the
     * menu, which has the synced slots but no block entity, reads the same list.
     */
    public static List<String> genesIn(Container container) {
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
    public static boolean holdsElsewhere(Container container, String geneKey, int exceptSlot) {
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

    /** How long this gene takes to copy - see {@link #TICKS_PER_RARITY_TIER}. */
    public static int copyTicks(String geneKey) {
        Gene gene = Genes.byKeyOrNull(geneKey);
        GeneRarity rarity = gene == null ? GeneRarity.DEFAULT : gene.rarity();
        return (rarity.ordinal() + 1) * TICKS_PER_RARITY_TIER;
    }

    /** The copy the current job would produce. */
    private ItemStack copyOf(String geneKey) {
        ItemStack paper = new ItemStack(ModItems.RESEARCH_PAPER.get());
        paper.set(ModDataComponents.RESEARCH_GENE.get(), geneKey);
        return paper;
    }

    /** A gene picked and still on the shelf, a book in, and room for the copy. */
    private boolean canCopy() {
        if (selectedGene.isEmpty() || !stores(selectedGene) || !book.getItem(0).is(Items.BOOK)) {
            return false;
        }
        ItemStack out = result.getItem(0);
        return out.isEmpty()
                || (ItemStack.isSameItemSameComponents(out, copyOf(selectedGene))
                        && out.getCount() < out.getMaxStackSize());
    }

    /**
     * <b>One tick of copying, on the block</b> - so it carries on with nobody
     * looking, exactly as a furnace smelts with its screen shut. A job that stops
     * being possible (the book gone, the original taken off the shelf, the
     * result slot full) drops back to zero rather than pausing.
     */
    public static void tick(Level level, BlockPos pos, BlockState state, EquineResearchShelfBlockEntity shelf) {
        if (level.isClientSide()) {
            return;
        }
        if (!shelf.canCopy()) {
            if (shelf.progress != 0) {
                shelf.progress = 0;
                shelf.setChanged();
            }
            return;
        }
        shelf.progress++;
        if (shelf.progress >= copyTicks(shelf.selectedGene)) {
            shelf.progress = 0;
            ItemStack out = shelf.result.getItem(0);
            if (out.isEmpty()) {
                shelf.result.setItem(0, shelf.copyOf(shelf.selectedGene));
            } else {
                out.grow(1);
                shelf.result.setChanged();
            }
            shelf.book.removeItem(0, 1);
        }
        shelf.setChanged();
    }

    /**
     * <b>Give everything back when the shelf is broken</b> - papers, books and
     * finished copies - here, where vanilla's containers do it, because this is
     * the last moment the block entity still exists.
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null) {
            Containers.dropContents(this.level, pos, papers);
            Containers.dropContents(this.level, pos, book);
            Containers.dropContents(this.level, pos, result);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, papers.getItems());
        output.store("book", ItemStack.OPTIONAL_CODEC, book.getItem(0));
        output.store("result", ItemStack.OPTIONAL_CODEC, result.getItem(0));
        output.putString("selected", selectedGene);
        output.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        papers.getItems().replaceAll(s -> ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, papers.getItems());
        book.getItems().set(0, input.read("book", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        result.getItems().set(0, input.read("result", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        selectedGene = input.getStringOr("selected", "");
        progress = input.getIntOr("progress", 0);
    }
}
