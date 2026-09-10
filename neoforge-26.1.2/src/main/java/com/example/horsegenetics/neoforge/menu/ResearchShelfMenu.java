package com.example.horsegenetics.neoforge.menu;

import com.example.horsegenetics.neoforge.block.EquineResearchShelfBlockEntity;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.item.ModItems;
import com.example.horsegenetics.neoforge.network.ShelfSyncPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * <b>The Equine Research Shelf's menu.</b> Two tabs' worth of function behind
 * three slots.
 *
 * <ul>
 *   <li><b>Craft</b> - pick a gene the shelf holds, put a blank book in
 *       {@link #BOOK_SLOT}, take its copy out of {@link #RESULT_SLOT}. The book
 *       is spent; the shelf's own paper is not, and never is.</li>
 *   <li><b>Store</b> - drop a research paper into {@link #FILE_SLOT} and the
 *       shelf files it, or click a row to take one back.</li>
 * </ul>
 *
 * <h2>The shelf is the authority, always</h2>
 * The client is told what the shelf holds ({@link ShelfSyncPayload}) so it can
 * draw the list, and it is told again after every change. It is never asked.
 * Every operation here re-reads the block entity, so a forged packet can at
 * worst ask for a gene the shelf does not hold, and get nothing.
 *
 * <h2>Why filing consumes the paper immediately</h2>
 * {@link #FILE_SLOT} is an input that empties itself: put a paper in, the gene
 * is recorded and the item is gone the same tick. It could have been a slot you
 * leave papers sitting in, but then "what is in the shelf" would have two
 * answers - the filed set and the slot - and a player would have to know that
 * one of them does not count. A paper for a gene the shelf <i>already</i> holds
 * is left alone rather than eaten, so nothing is ever destroyed for nothing.
 */
public final class ResearchShelfMenu extends AbstractContainerMenu {

    public static final int BOOK_SLOT = 0;
    public static final int RESULT_SLOT = 1;
    public static final int FILE_SLOT = 2;
    private static final int SLOT_COUNT = 3;

    private final Player player;
    private final @Nullable EquineResearchShelfBlockEntity shelf;

    private final Container input = new SimpleContainer(1);
    private final Container filing = new SimpleContainer(1);
    private final ResultContainer result = new ResultContainer();

    private String selectedGene = "";

    /**
     * <b>Which tab the screen is showing</b>, so the slots that do not belong to
     * it can go inactive. Client-only state living on the menu, the way a
     * container screen's own toggles do: {@code Slot.x/y} are final, so a slot
     * cannot be moved out of the way - but {@link Slot#isActive()} is consulted
     * for both drawing <i>and</i> hit-testing, which is exactly the pair that
     * has to agree. Always true server-side, where there is no tab.
     */
    private boolean storeTab;

    /** Client-side mirror of the shelf's contents; the server reads the block entity. */
    private List<String> clientStored = List.of();

    /** Client constructor - {@code MenuType} hands us no block entity. */
    public ResearchShelfMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null);
    }

    public ResearchShelfMenu(int containerId, Inventory inventory,
                             @Nullable EquineResearchShelfBlockEntity shelf) {
        super(ModMenus.RESEARCH_SHELF.get(), containerId);
        this.player = inventory.player;
        this.shelf = shelf;

        addSlot(new Slot(input, 0, 44, 40) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.BOOK);
            }

            @Override
            public boolean isActive() {
                return !storeTab;
            }
        });
        addSlot(new Slot(result, 0, 116, 40) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean isActive() {
                return !storeTab;
            }

            @Override
            public void onTake(Player taker, ItemStack taken) {
                input.removeItem(0, 1);
                recomputeResult();
                super.onTake(taker, taken);
            }
        });
        addSlot(new Slot(filing, 0, 80, 64) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.RESEARCH_PAPER.get());
            }

            @Override
            public boolean isActive() {
                return storeTab;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 102 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 160));
        }
    }

    // ------------------------------------------------------------------
    // What the shelf holds
    // ------------------------------------------------------------------

    /** Server: the block entity. Client: whatever it was last told. */
    public List<String> storedGenes() {
        return shelf != null ? shelf.storedGenes() : clientStored;
    }

    public void acceptStored(List<String> genes) {
        this.clientStored = List.copyOf(genes);
    }

    public String selectedGene() {
        return selectedGene;
    }

    /** Client-side, from the screen, every frame. */
    public void setStoreTab(boolean store) {
        this.storeTab = store;
    }

    /** From the client's list click, and re-checked here against the shelf. */
    public void selectGene(String geneKey) {
        this.selectedGene = geneKey == null ? "" : geneKey;
        recomputeResult();
    }

    /** Take one filed paper back out. Server-side; ignores a gene not held. */
    public void withdraw(String geneKey) {
        if (shelf == null || !shelf.withdraw(geneKey)) {
            return;
        }
        ItemStack paper = new ItemStack(ModItems.RESEARCH_PAPER.get());
        paper.set(ModDataComponents.RESEARCH_GENE.get(), geneKey);
        if (!player.getInventory().add(paper)) {
            player.drop(paper, false);
        }
        if (geneKey.equals(selectedGene)) {
            selectedGene = "";
        }
        recomputeResult();
        sync();
    }

    private void sync() {
        if (shelf != null && player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new ShelfSyncPayload(shelf.storedGenes()));
        }
    }

    /** Push the current contents at the client - called once when the screen opens. */
    public void syncOnOpen() {
        sync();
    }

    // ------------------------------------------------------------------
    // The two operations
    // ------------------------------------------------------------------

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == filing) {
            fileWhateverIsThere();
        }
        recomputeResult();
    }

    /** A paper in the filing slot is recorded and consumed; a duplicate is left alone. */
    private void fileWhateverIsThere() {
        if (shelf == null) {
            return;
        }
        ItemStack stack = filing.getItem(0);
        if (stack.isEmpty()) {
            return;
        }
        String geneKey = stack.get(ModDataComponents.RESEARCH_GENE.get());
        if (geneKey == null || geneKey.isEmpty()) {
            return; // a blank paper - nothing to file
        }
        if (shelf.file(geneKey)) {
            stack.shrink(1);
            filing.setChanged();
            sync();
        }
    }

    /**
     * The result is a copy of the selected gene's paper, and exists only while a
     * book is in and the shelf still holds that gene. Recomputed rather than
     * remembered, so removing the book or withdrawing the original clears it.
     */
    private void recomputeResult() {
        if (shelf == null) {
            return; // client-side; the server sends the answer
        }
        boolean ready = !selectedGene.isEmpty()
                && shelf.stores(selectedGene)
                && input.getItem(0).is(Items.BOOK);
        if (!ready) {
            result.setItem(0, ItemStack.EMPTY);
            broadcastChanges();
            return;
        }
        ItemStack paper = new ItemStack(ModItems.RESEARCH_PAPER.get());
        paper.set(ModDataComponents.RESEARCH_GENE.get(), selectedGene);
        result.setItem(0, paper);
        broadcastChanges();
    }

    // ------------------------------------------------------------------
    // Plumbing
    // ------------------------------------------------------------------

    @Override
    public ItemStack quickMoveStack(Player who, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < SLOT_COUNT) {
            // out of the machine and into the player
            if (!moveItemStackTo(stack, SLOT_COUNT, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else if (!moveItemStackTo(stack, 0, SLOT_COUNT, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    /**
     * Close if the shelf is gone or the player walked off. Distance rather than
     * {@code stillValid(ContainerLevelAccess, ...)} because this menu is built
     * from a block entity, not from a level access - and squared, because
     * {@code distanceToSqr} is what there is. 8 blocks, comfortably past
     * vanilla's reach, so it never closes on somebody standing at the shelf.
     */
    @Override
    public boolean stillValid(Player who) {
        if (shelf == null) {
            return true; // client copy - the server's answer is the one that counts
        }
        return !shelf.isRemoved()
                && who.distanceToSqr(shelf.getBlockPos().getCenter()) <= 64.0;
    }

    /** The book and any un-filed paper go back to the player, as a workbench does. */
    @Override
    public void removed(Player who) {
        super.removed(who);
        result.setItem(0, ItemStack.EMPTY);
        if (!who.level().isClientSide()) {
            clearContainer(who, input);
            clearContainer(who, filing);
        }
    }
}
