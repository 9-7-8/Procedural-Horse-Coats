package com.example.horsegenetics.neoforge.block;

import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneRarity;
import com.example.horsegenetics.common.genetics.ResearchTopic;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.item.ModItems;
import com.example.horsegenetics.neoforge.item.ResearchPaperItem;
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
 * (2026-09-10). <b>One paper per allele pair</b>, enforced by {@link #holdsElsewhere}
 * from the menu's slots.
 *
 * <h2>Copying runs on the block, like a furnace</h2>
 * The book slot, the result slot, the pair picked and the progress all live
 * here and {@link #tick} advances them whether or not anyone has the screen
 * open. They used to live on the menu, which exists only while a player is
 * looking - so a copy stopped the moment the screen closed (owner-reported the
 * same day). A finished copy spends one book and lands in the result slot,
 * where copies of the same pair stack; with more books in, the next one starts
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
    /** Index of the picked pair in {@link #topicsIn}'s order, or -1 - how the client learns the pick. */
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

    private String selectedTopic = "";
    private int progress;

    /** The furnace's dataAccess: what the menu syncs to the client every tick. */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_TOTAL -> selectedTopic.isEmpty() ? 0 : copyTicks(selectedTopic);
                case DATA_SELECTED -> topicsIn(papers).indexOf(selectedTopic);
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

    public String selectedTopic() {
        return selectedTopic;
    }

    /** Pick the pair to copy. A new pick restarts the clock; the same pick is a no-op. */
    public void select(String token) {
        String next = token == null ? "" : token;
        if (!next.equals(selectedTopic)) {
            selectedTopic = next;
            progress = 0;
            setChanged();
        }
    }

    /** Is this a research paper with a pair written on it - the only thing the shelf takes? */
    public static boolean isFiledPaper(ItemStack stack) {
        return stack.is(ModItems.RESEARCH_PAPER.get()) && !topicOf(stack).isEmpty();
    }

    /**
     * The pair a paper names, as a {@link ResearchTopic#token()}, or {@code ""}.
     *
     * <p><b>A pair, not a gene.</b> The shelf files one paper per pair now, so a
     * breeder can keep {@code Agouti: A/A} and {@code Agouti: A/a} side by side -
     * which is the whole reason to file them, since those two craft different
     * carrots.
     */
    public static String topicOf(ItemStack stack) {
        ResearchTopic topic = stack.get(ModDataComponents.RESEARCH_TOPIC.get());
        return topic == null ? "" : topic.token();
    }

    /**
     * Every pair the papers in {@code container} name, sorted by gene name then
     * pair - the Copy tab's list. Static and container-based so the client's copy
     * of the menu, which has the synced slots but no block entity, reads the same
     * list.
     */
    public static List<String> topicsIn(Container container) {
        Set<String> seen = new LinkedHashSet<>();
        for (int i = 0; i < container.getContainerSize(); i++) {
            String token = topicOf(container.getItem(i));
            if (!token.isEmpty()) {
                seen.add(token);
            }
        }
        List<String> out = new ArrayList<>(seen);
        out.sort(Comparator.comparing(EquineResearchShelfBlockEntity::displayName,
                String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(out);
    }

    /** Does any slot other than {@code exceptSlot} already hold a paper for this pair? */
    public static boolean holdsElsewhere(Container container, String token, int exceptSlot) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (i != exceptSlot && token.equals(topicOf(container.getItem(i)))) {
                return true;
            }
        }
        return false;
    }

    public boolean stores(String token) {
        return holdsElsewhere(papers, token, -1);
    }

    /** The name a row shows - {@code Agouti: A/a}, or the raw token if nothing resolves. */
    public static String displayName(String token) {
        ResearchTopic topic = ResearchTopic.parse(token);
        return topic == null ? token : topic.label();
    }

    /**
     * How long this paper takes to copy - see {@link #TICKS_PER_RARITY_TIER}. The
     * gene's rarity, not the pair's: what is rare is the locus, and a carrier
     * paper is no easier to transcribe than a true-breeding one.
     */
    public static int copyTicks(String token) {
        ResearchTopic topic = ResearchTopic.parse(token);
        Gene gene = topic == null ? null : topic.gene();
        GeneRarity rarity = gene == null ? GeneRarity.DEFAULT : gene.rarity();
        return (rarity.ordinal() + 1) * TICKS_PER_RARITY_TIER;
    }

    /** The copy the current job would produce. */
    private ItemStack copyOf(String token) {
        ResearchTopic topic = ResearchTopic.parse(token);
        return topic == null ? ItemStack.EMPTY : ResearchPaperItem.of(topic);
    }

    /** A pair picked and still on the shelf, a book in, and room for the copy. */
    private boolean canCopy() {
        if (selectedTopic.isEmpty() || !stores(selectedTopic) || !book.getItem(0).is(Items.BOOK)) {
            return false;
        }
        ItemStack copy = copyOf(selectedTopic);
        if (copy.isEmpty()) {
            return false; // a token that no longer parses - never spend a book on it
        }
        ItemStack out = result.getItem(0);
        return out.isEmpty()
                || (ItemStack.isSameItemSameComponents(out, copy)
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
        if (shelf.progress >= copyTicks(shelf.selectedTopic)) {
            shelf.progress = 0;
            ItemStack out = shelf.result.getItem(0);
            if (out.isEmpty()) {
                shelf.result.setItem(0, shelf.copyOf(shelf.selectedTopic));
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
        output.putString("selected", selectedTopic);
        output.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        papers.getItems().replaceAll(s -> ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, papers.getItems());
        book.getItems().set(0, input.read("book", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        result.getItems().set(0, input.read("result", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        selectedTopic = input.getStringOr("selected", "");
        progress = input.getIntOr("progress", 0);
    }
}
