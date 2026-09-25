package com.example.horsegenetics.neoforge.menu;

import com.example.horsegenetics.common.horse.StasisTier;
import com.example.horsegenetics.neoforge.block.HorseStasisBankBlockEntity;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.data.StasisSnapshot;
import com.example.horsegenetics.neoforge.item.StasisChamberItem;
import com.example.horsegenetics.neoforge.server.StasisStud;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>The Horse Stasis Bank's menu.</b> Three tabs: a grid of
 * {@link HorseStasisBankBlockEntity#SLOTS} slots that takes stasis chambers and
 * refuses everything else, <b>Browse</b>, which is the same chambers read as
 * horses rather than as items, and <b>Supply</b>, the three slots the bank feeds
 * and waters them from.
 *
 * <h2>It holds no state of its own</h2>
 * Every slot wraps the block entity's containers, exactly as
 * {@link ResearchShelfMenu}'s do, so closing the screen changes nothing. The
 * client's copy wraps empty containers of the same shape, which the ordinary
 * slot sync fills in.
 *
 * <p><b>One {@code ContainerData} value, and only one.</b> The water meter is a
 * number on the block that moves with nobody looking, which is the research
 * shelf's case and is why the shelf has one; everything else on these three tabs
 * - the horse count, every Browse row, what is in the feed slot - is a reading
 * of slots the client already holds. The tab state is the shelf's
 * {@code setStoreTab}/{@code activeOnTab} pair under another name - a
 * client-side field and an {@code isActive()} override, which is why every slot
 * stays live on the server and only the client hides the ones it is not
 * drawing.
 *
 * <h2>Every chamber's whole horse is on the wire</h2>
 * A chamber's {@code stasis_snapshot} component carries the entire entity tag,
 * for the creative pick-block reason {@code StasisSnapshot} explains, and this
 * menu can hold {@link HorseStasisBankBlockEntity#SLOTS} of them. That is the
 * one thing about the bank that is genuinely more expensive than the item it
 * stores, and it is on the page's Verification tab as a thing to measure with a
 * full bank rather than a thing to fix by sending less - sending less is how a
 * creative player silently erases a horse.
 *
 * <p><b>The deep grid made that check four times more urgent, not less.</b> The
 * whole grid still arrives in one {@code ClientboundContainerSetContentPacket}
 * on open, scrolled or not: what the scrollbar changes is which rows are drawn,
 * never which are sent. Nobody has yet opened a full bank over a real
 * connection.
 */
public final class HorseStasisBankMenu extends AbstractContainerMenu {

    /** The chamber grid is the first block of slots; the supply slots and the player's follow it. */
    public static final int FIRST_CHAMBER_SLOT = 0;
    public static final int FIRST_SUPPLY_SLOT = FIRST_CHAMBER_SLOT + HorseStasisBankBlockEntity.SLOTS;
    private static final int SLOT_COUNT =
            FIRST_SUPPLY_SLOT + HorseStasisBankBlockEntity.SUPPLY_SLOTS;

    // ------------------------------------------------------------------
    // The window's layout, in one place - addSlot needs it here and the screen
    // reads every one of them. Deliberately the research shelf's numbers: it is
    // the same double-chest grid over the same player inventory, and two windows
    // in one mod that are almost the same size look like a mistake.
    // ------------------------------------------------------------------

    public static final int WIDTH = 176;
    public static final int HEIGHT = 222;
    public static final int MARGIN = 8;
    public static final int GRID_Y = 18;

    /**
     * <b>How many of the grid's {@link HorseStasisBankBlockEntity#ROWS} rows are
     * on screen at once</b> - a double chest's six, so the window is the size a
     * player already knows and the rows below it are reached by scrolling.
     */
    public static final int VISIBLE_ROWS = 6;
    public static final int GRID_H = VISIBLE_ROWS * 18;

    /**
     * The chamber grid's scrollbar, in the gutter between the last column's well
     * (which ends at {@code MARGIN + 8 * 18 + 17}) and the window's own shadow
     * (which starts at {@code WIDTH - 3}). Four pixels, and they are exactly the
     * four that were going spare - the window does not grow for this.
     */
    public static final int SCROLL_X = MARGIN + 8 * 18 + 17;
    public static final int SCROLL_W = WIDTH - 3 - SCROLL_X;

    /** Browse tab: the filter field, then the list of horses under it. */
    public static final int FILTER_Y = 18;
    public static final int FILTER_H = 14;
    public static final int LIST_Y = FILTER_Y + FILTER_H + 4;
    public static final int LIST_W = WIDTH - 2 * MARGIN;
    /** Two lines a row - a name, and what the horse is - so four fit. */
    public static final int ROW_H = 22;
    public static final int LIST_ROWS = 4;
    public static final int LIST_H = LIST_ROWS * ROW_H;

    /** Supply tab: three slots in a row, with room above for a line of text. */
    public static final int SUPPLY_Y = 32;
    public static final int SUPPLY_GAP = 32;
    public static final int SUPPLY_X = MARGIN + 16;

    /**
     * The drop buffer, under the supply row: a row of nine, on the chamber
     * grid's own column pitch so the two read as the same window.
     */
    public static final int DROPS_Y = 96;

    public static final int INV_LABEL_Y = 128;
    public static final int INV_Y = 140;
    public static final int HOTBAR_Y = INV_Y + 3 * 18 + 4;

    private final Player player;
    private final @Nullable HorseStasisBankBlockEntity bank;
    private final Container chambers;
    private final Container supplies;
    private final ContainerData data;

    /**
     * <b>Which tab the screen is showing</b>, so the slots of the other two go
     * inactive. Only ever set on the client - see {@link #activeOnTab}.
     */
    private Tab tab = Tab.CHAMBERS;

    /**
     * <b>The top row of the chamber grid that is on screen.</b> Client-only, for
     * the tab's own reason: a slot's position and whether it is drawn are both
     * questions only the client has, and the server keeps every slot live so a
     * click on any of them lands whatever this says.
     */
    private int chamberRow;

    /** The three tabs, in the order the screen draws them. */
    public enum Tab {
        CHAMBERS("Chambers"),
        BROWSE("Browse"),
        SUPPLY("Supply");

        private final String label;

        Tab(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /** Client constructor - {@code MenuType} hands us no block entity. */
    public HorseStasisBankMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null);
    }

    public HorseStasisBankMenu(int containerId, Inventory inventory,
                               @Nullable HorseStasisBankBlockEntity bank) {
        super(ModMenus.HORSE_STASIS_BANK.get(), containerId);
        this.player = inventory.player;
        this.bank = bank;
        this.chambers = bank != null
                ? bank.chambers()
                : new SimpleContainer(HorseStasisBankBlockEntity.SLOTS);
        this.supplies = bank != null
                ? bank.supplies()
                : new SimpleContainer(HorseStasisBankBlockEntity.SUPPLY_SLOTS);
        this.data = bank != null
                ? bank.data()
                : new SimpleContainerData(HorseStasisBankBlockEntity.DATA_COUNT);
        addDataSlots(this.data);

        // Every row is placed where it would be if the whole grid fitted; the
        // rows past the sixth land below the window, and setChamberRow slides
        // them all up when the player scrolls. On the server these numbers are
        // written once and never read.
        for (int i = 0; i < HorseStasisBankBlockEntity.SLOTS; i++) {
            addSlot(new ChamberSlot(chambers, i, chamberX(i), GRID_Y + (i / HorseStasisBankBlockEntity.COLS) * 18));
        }
        for (int i = 0; i < HorseStasisBankBlockEntity.SUPPLY_SLOTS; i++) {
            addSlot(new SupplySlot(supplies, i, supplyX(i), supplyY(i)));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, MARGIN + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, MARGIN + col * 18, HOTBAR_Y));
        }
    }

    /**
     * A bank slot: a stasis chamber of any tier, empty or with a horse in it, one
     * to a slot.
     *
     * <p>Unlike the shelf's paper slot there is no second condition - no "one per
     * gene" equivalent - because two chambers are never the same chamber and a
     * rack of spare empties is a reasonable thing to keep beside the full ones.
     */
    private final class ChamberSlot extends Slot {

        ChamberSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return HorseStasisBankBlockEntity.isChamber(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean isActive() {
            return activeOnTab(Tab.CHAMBERS) && onScreen(getContainerSlot());
        }
    }

    // ------------------------------------------------------------------
    // Scrolling the chamber grid
    // ------------------------------------------------------------------

    /** Which column of the grid slot {@code i} sits in. The screen reads this too. */
    public static int chamberX(int i) {
        return MARGIN + (i % HorseStasisBankBlockEntity.COLS) * 18;
    }

    /** Where slot {@code i} is drawn with row {@code top} at the top of the window. */
    public static int chamberY(int i, int top) {
        return GRID_Y + (i / HorseStasisBankBlockEntity.COLS - top) * 18;
    }

    /** The top row on screen. */
    public int chamberRow() {
        return chamberRow;
    }

    /** How far down the grid can be scrolled - the rows that do not fit. */
    public static int maxChamberRow() {
        return HorseStasisBankBlockEntity.ROWS - VISIBLE_ROWS;
    }

    /**
     * <b>Scroll the grid to put {@code row} at the top</b>, moving every chamber
     * slot rather than remapping which container slot each one shows.
     *
     * <p>That is the choice worth being explicit about. Scrolling a <i>view</i> -
     * fixed slots reading a moving window of the container - would mean the
     * client and the server had to agree on the offset before every click, and
     * a scroll packet overtaking a click packet would file the player's horse in
     * the wrong chamber or take out the wrong one. Moving the slots instead
     * keeps slot index to container index fixed and permanent on both sides;
     * only where the client <i>draws</i> them changes, and the server neither
     * knows nor needs to. See the note on {@code Slot.x} in
     * {@code accesstransformer.cfg}.
     */
    public void setChamberRow(int row) {
        int clamped = Math.max(0, Math.min(row, maxChamberRow()));
        if (clamped == chamberRow) {
            return;
        }
        chamberRow = clamped;
        for (int i = 0; i < HorseStasisBankBlockEntity.SLOTS; i++) {
            Slot slot = slots.get(FIRST_CHAMBER_SLOT + i);
            slot.x = chamberX(i);
            slot.y = chamberY(i, chamberRow);
        }
    }

    /**
     * Is this chamber slot in the six rows the player can see?
     *
     * <p><b>Only asked on the client</b> - on the server every slot is live, for
     * the reason {@link #activeOnTab} states: the server cannot know where the
     * window is scrolled to, and it does not have to, because the client will
     * not send a click on a slot it is not drawing.
     */
    private boolean onScreen(int index) {
        if (!player.level().isClientSide()) {
            return true;
        }
        int row = index / HorseStasisBankBlockEntity.COLS;
        return row >= chamberRow && row < chamberRow + VISIBLE_ROWS;
    }

    /**
     * Where the goods container's slot {@code i} sits - the supply row, then
     * the drop buffer under it. <b>The screen reads these too</b>, so the sunken
     * wells it draws cannot drift from the slots they are drawn behind.
     */
    public static int supplyX(int i) {
        return i < HorseStasisBankBlockEntity.FIRST_DROP_SLOT
                ? SUPPLY_X + i * SUPPLY_GAP
                : MARGIN + (i - HorseStasisBankBlockEntity.FIRST_DROP_SLOT) * 18;
    }

    public static int supplyY(int i) {
        return i < HorseStasisBankBlockEntity.FIRST_DROP_SLOT ? SUPPLY_Y : DROPS_Y;
    }

    /**
     * A supply slot: feed, water, or the empties the water leaves behind.
     *
     * <p>{@code mayPlace} defers to the container's own
     * {@code canPlaceItem} rather than repeating the rules, so the slot a player
     * clicks and the slot a hopper pushes into agree by construction - see
     * {@link com.example.horsegenetics.neoforge.block.StasisBankCapability}.
     */
    private final class SupplySlot extends Slot {

        SupplySlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return container.canPlaceItem(getContainerSlot(), stack);
        }

        @Override
        public boolean isActive() {
            return activeOnTab(Tab.SUPPLY);
        }
    }

    /**
     * <b>Every slot is live on the server; the tab only hides them on the
     * client.</b> The shelf's rule, for the shelf's reason: the server cannot
     * know which tab the player is looking at, and the client will not send a
     * click on a slot it is not drawing.
     */
    private boolean activeOnTab(Tab which) {
        return !player.level().isClientSide() || tab == which;
    }

    public void setTab(Tab tab) {
        this.tab = tab;
    }

    // ------------------------------------------------------------------
    // What the Supply tab reads
    // ------------------------------------------------------------------

    /**
     * Water left, in health points it can pay for - out of
     * {@link com.example.horsegenetics.common.horse.StasisUpkeep#WATER_PER_BUCKET}
     * to the bucket.
     *
     * <p><b>The bank's first {@code ContainerData}</b>, and the first thing here
     * that genuinely has to be synced: the meter is a number on the block that
     * moves with nobody looking, which is exactly the case the Browse tab did
     * <i>not</i> have. Everything else on this screen is still read off slots
     * the client already holds.
     */
    public int water() {
        return data.get(HorseStasisBankBlockEntity.DATA_WATER);
    }

    /** Is there a horse in here the bank could be mending? */
    public boolean anyHealable() {
        return HorseStasisBankBlockEntity.anyHealable(chambers);
    }

    /** Is there a horse in here whose drops the bank collects? */
    public boolean anyCollecting() {
        return HorseStasisBankBlockEntity.anyCollecting(chambers);
    }

    /** What is in the feed slot, for the line that says whether it is enough. */
    public ItemStack feed() {
        return supplies.getItem(HorseStasisBankBlockEntity.FEED_SLOT);
    }

    /**
     * <b>Is a mare waiting to foal with nowhere to stand?</b> The bank's second
     * {@code ContainerData} value, and the second thing that genuinely has to be
     * synced: it is a fact about the room round the block, which the client
     * cannot read off a slot.
     */
    public boolean foalingBlocked() {
        return data.get(HorseStasisBankBlockEntity.DATA_FOALING) != 0;
    }

    /** How many of the nine buffer slots have something in them. */
    public int dropsHeld() {
        int n = 0;
        for (int i = 0; i < HorseStasisBankBlockEntity.DROP_SLOTS; i++) {
            if (!supplies.getItem(HorseStasisBankBlockEntity.FIRST_DROP_SLOT + i).isEmpty()) {
                n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------------
    // What the Browse tab reads
    // ------------------------------------------------------------------

    /**
     * One occupied chamber: the tier of the chamber, and the horse inside it.
     *
     * <p>The tier travels with the row because it is the gate on what the Browse
     * tab may say about that horse - {@link StasisTier#searchable()} and the two
     * flags above it, never a tier compared by name.
     */
    public record Stored(int slot, StasisTier tier, StasisSnapshot snapshot, boolean atStud) {
    }

    /**
     * Every horse filed here, in slot order.
     *
     * <p>Read off the slots rather than sent, like {@link #occupied()}: each
     * chamber carries its whole horse in a data component for the creative
     * pick-block reason {@code StasisSnapshot} spells out, so the client already
     * has all of this. Empty chambers are not rows - the Browse tab lists
     * horses, and the count on the title row is where empties are already
     * accounted for.
     */
    public List<Stored> stored() {
        List<Stored> out = new ArrayList<>();
        for (int i = 0; i < chambers.getContainerSize(); i++) {
            ItemStack stack = chambers.getItem(i);
            if (stack.getItem() instanceof StasisChamberItem chamber) {
                StasisSnapshot snapshot = StasisChamberItem.snapshotOf(stack);
                if (snapshot != null) {
                    out.add(new Stored(i, chamber.tier(), snapshot, StasisStud.atStud(stack)));
                }
            }
        }
        return out;
    }

    // ------------------------------------------------------------------
    // What the bank holds - read off the slots, so both sides agree
    // ------------------------------------------------------------------

    /** How many chambers are filed here, full or empty. */
    public int filed() {
        return HorseStasisBankBlockEntity.filed(chambers);
    }

    /** How many of them have a horse in them. */
    public int occupied() {
        return HorseStasisBankBlockEntity.occupied(chambers);
    }

    // ------------------------------------------------------------------
    // Plumbing
    // ------------------------------------------------------------------

    /**
     * Shift-click. Out of the bank into the player; from the player, a chamber
     * goes to the grid and feed or water to its own supply slot -
     * {@code moveItemStackTo} asks each slot's {@code mayPlace}, so the empties
     * slot refuses everything and a water bucket cannot land in the feed slot.
     * Anything else stays where it is.
     */
    @Override
    public ItemStack quickMoveStack(Player who, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < SLOT_COUNT) {
            if (!moveItemStackTo(stack, SLOT_COUNT, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else if (HorseStasisBankBlockEntity.isChamber(stack)) {
            if (!moveItemStackTo(stack, FIRST_CHAMBER_SLOT, FIRST_SUPPLY_SLOT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (HorseStasisBankBlockEntity.isWater(stack)
                || HorseStasisBankBlockEntity.isFeed(stack)) {
            if (!moveItemStackTo(stack, FIRST_SUPPLY_SLOT, SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
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
     * <b>Turn a chamber out at stud, or bring it back in.</b> The button id is
     * the chamber slot the player clicked on the Browse tab - nothing else needs
     * to travel, because the mark itself lives on the item and syncs with the
     * slot.
     *
     * <p>{@code clickMenuButton} rather than a payload of its own, which is the
     * rule {@code BenchNamePayload}'s comment states from the other side: an
     * int fits here and a string does not. The screen only offers the click on a
     * row it can see, and the server checks the tier and the horse again anyway
     * - a menu button is a packet, and a packet is whatever somebody sent.
     */
    @Override
    public boolean clickMenuButton(Player who, int id) {
        if (who.level().isClientSide()) {
            return true;
        }
        if (id < 0 || id >= HorseStasisBankBlockEntity.SLOTS) {
            return false;
        }
        ItemStack chamber = chambers.getItem(id);
        StasisTier tier = HorseStasisBankBlockEntity.tierOf(chamber);
        if (tier == null || !tier.breedsInBank() || StasisChamberItem.snapshotOf(chamber) == null) {
            // An empty chamber, or a rung that did not buy this. Refused rather
            // than marked: a mark that does nothing is worse than no mark.
            return false;
        }
        if (StasisStud.atStud(chamber)) {
            chamber.remove(ModDataComponents.STASIS_AT_STUD.get());
        } else {
            chamber.set(ModDataComponents.STASIS_AT_STUD.get(), Boolean.TRUE);
        }
        // Through the container, so the block's tick gate is recomputed: a bank
        // whose only work is a pair at stud must start ticking when they are
        // marked and stop when they are not.
        chambers.setChanged();
        return true;
    }

    /** Close if the bank is gone or the player walked off - 8 blocks, the shelf's reach. */
    @Override
    public boolean stillValid(Player who) {
        if (bank == null) {
            return true; // client copy - the server's answer is the one that counts
        }
        return !bank.isRemoved()
                && who.distanceToSqr(bank.getBlockPos().getCenter()) <= 64.0;
    }
}
