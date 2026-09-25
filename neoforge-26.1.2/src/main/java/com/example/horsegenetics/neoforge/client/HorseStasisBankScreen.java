package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.horse.StasisBrowseRow;
import com.example.horsegenetics.common.horse.StasisTier;
import com.example.horsegenetics.neoforge.block.HorseStasisBankBlockEntity;
import com.example.horsegenetics.neoforge.menu.HorseStasisBankMenu;
import com.example.horsegenetics.neoforge.network.HorseRosterRequestPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * <b>The Horse Stasis Bank's screen.</b> Three tabs: <b>Chambers</b>, a chest of
 * stasis chambers; <b>Browse</b>, the same chambers read as the horses inside
 * them; and <b>Supply</b>, the feed and water the bank mends them from.
 *
 * <h2>Two scrolling lists, one idiom</h2>
 * The Chambers grid is {@link HorseStasisBankBlockEntity#ROWS} rows deep behind
 * a {@link HorseStasisBankMenu#VISIBLE_ROWS}-row window, and Browse is a list
 * longer than its four rows. Both are moved by the wheel and both draw the same
 * three-colour thumb; neither can be dragged, on purpose, because the thumb is
 * there to say <i>there is more below</i> and the wheel is how a player would
 * reach it anyway. <b>Where they differ is who holds the offset</b>: Browse's is
 * a field here, because the rows are drawn by this class; the grid's lives on
 * the menu, because scrolling it means moving real slots.
 *
 * <h2>Browse is a reading, not a second inventory</h2>
 * Every row is a chamber the bank already holds, and the whole tab is one
 * {@link StasisBrowseRow} list rebuilt when the slots, the filter or the stable's
 * records change - never per frame. The filter box takes the browser's own query
 * language ({@code HorseQuery}), so {@code mare gen>2 gene:SB1 -lethal} means
 * here what it means there; teaching the bank a second, smaller search language
 * would have been a worse feature <i>and</i> more code.
 *
 * <h2>The tier is the gate, and it gates per chamber</h2>
 * A row is searchable only if its own chamber says so
 * ({@link StasisTier#searchable()}, never a tier compared by name). A Basic
 * chamber still lists its horse - the item's tooltip names it at every tier, so
 * hiding it here would make the bank contradict the bottle in your hand - but no
 * query can reach it, and the list says how many rows a query had to leave out
 * rather than letting a horse silently vanish from the list.
 *
 * <h2>Where the details come from</h2>
 * A chamber carries the horse's whole entity tag, and decoding a bankful of
 * those to draw a list would be the expensive way round. The details come from
 * the stable's own papers instead - {@link ClientHorseRoster}, the same rows the
 * browser's <i>My horses</i> table sorts, matched on the {@code UUID} the
 * chamber remembers - which is free, already written, and always agrees with the
 * browser. A horse with no row there (somebody else's chamber, or a stable past
 * the roster's cap) keeps its name and says it has no papers.
 *
 * <h2>It is drawn as a Minecraft window</h2>
 * Face, bevel, sunken slots, dark text - see {@link VanillaPanel}, no texture.
 * Every position comes from {@link HorseStasisBankMenu}'s constants, the same
 * numbers its slots are placed with.
 */
public final class HorseStasisBankScreen extends AbstractContainerScreen<HorseStasisBankMenu> {

    private static final HorseStasisBankMenu.Tab[] TABS = HorseStasisBankMenu.Tab.values();

    private static final int TITLE_Y = 6;
    private static final int TAB_H = 16;
    private static final int LINE_H = 10;

    /**
     * The tab the bank opens on: whichever was showing last time, for the rest
     * of the session - the research shelf's rule, and for its reason. The
     * question is "what was I doing", not "what was this bank".
     */
    private static HorseStasisBankMenu.Tab lastTab = HorseStasisBankMenu.Tab.CHAMBERS;

    private HorseStasisBankMenu.Tab tab = lastTab;
    private EditBox filterBox;
    private int scroll;

    /**
     * What the last click on a row had to say - "a Basic chamber cannot be put
     * to stud" and the like. Cleared by the next thing the player does, because
     * a refusal that stays up outlives the question that caused it.
     */
    private String note = "";

    /** Every horse in the bank, and the subset the filter leaves showing. */
    private List<StasisBrowseRow> rows = List.of();
    private List<StasisBrowseRow> shown = List.of();

    /** What {@link #rows} was built from, so it is rebuilt only when one moves. */
    private int builtFrom = Integer.MIN_VALUE;
    private int builtVersion = -1;
    private String builtQuery = "";

    public HorseStasisBankScreen(HorseStasisBankMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, HorseStasisBankMenu.WIDTH, HorseStasisBankMenu.HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = HorseStasisBankMenu.MARGIN;
        this.titleLabelY = TITLE_Y;
        this.inventoryLabelX = HorseStasisBankMenu.MARGIN;
        this.inventoryLabelY = HorseStasisBankMenu.INV_LABEL_Y;
        this.menu.setTab(tab);

        // Unbordered, like the equestrian bench's name field: the sunken well
        // drawn behind it is the frame, so the box draws no second one.
        this.filterBox = new EditBox(this.font,
                leftPos + HorseStasisBankMenu.MARGIN + 2, topPos + HorseStasisBankMenu.FILTER_Y + 3,
                HorseStasisBankMenu.LIST_W - 4, HorseStasisBankMenu.FILTER_H - 5,
                Component.literal("Filter"));
        this.filterBox.setBordered(false);
        // Dark text, because this panel is light. An EditBox defaults to the
        // near-white a dark screen wants, which on a sunken grey well is a
        // query the player cannot read back.
        this.filterBox.setTextColor(VanillaPanel.TEXT);
        this.filterBox.setMaxLength(96);
        this.filterBox.setHint(Component.literal("mare  gen>2  gene:SB1"));
        this.addRenderableWidget(this.filterBox);
        applyTab();

        // The rows are the stable's papers, which this client may never have
        // asked for - the browser is where they usually arrive. One request on
        // open, the same one its Refresh button sends.
        requestRoster();
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        HorseStasisBankMenu.Tab hit = tabAt(event.x(), event.y());
        if (hit != null) {
            if (hit != tab && this.menu.getCarried().isEmpty()) {
                tab = hit;
                lastTab = hit;
                scroll = 0;
                note = "";
                applyTab();
                if (hit == HorseStasisBankMenu.Tab.BROWSE) {
                    requestRoster();
                }
            }
            return true;
        }
        if (tab == HorseStasisBankMenu.Tab.BROWSE) {
            int row = rowAt(event.x(), event.y());
            if (row >= 0) {
                toggleStud(shown.get(row));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    /**
     * <b>A click on a row turns that horse out at stud, or brings it back in.</b>
     *
     * <p>The server is the one that decides - it re-checks the tier and the horse
     * on the other end of {@code clickMenuButton} - so this only refuses the
     * cases it can see, and says which, rather than sending a click it knows will
     * be dropped. A row that does nothing and explains nothing is the failure
     * this whole tab was written to avoid.
     */
    private void toggleStud(StasisBrowseRow row) {
        if (!row.mayStud()) {
            note = row.tier() == null
                    ? "That chamber cannot be put to stud."
                    : "A " + row.tier().id() + " chamber cannot be put to stud - a spacer can.";
            return;
        }
        note = "";
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, row.slot());
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (tab == HorseStasisBankMenu.Tab.BROWSE && sy != 0 && overList(mx, my)) {
            scroll = Math.max(0, Math.min(scroll - (int) Math.signum(sy), maxScroll()));
            return true;
        }
        // The chamber grid, a row a notch. Anywhere over the grid or its bar -
        // the player is scrolling the list of horses, not one slot of it.
        if (tab == HorseStasisBankMenu.Tab.CHAMBERS && sy != 0 && overGrid(mx, my)) {
            this.menu.setChamberRow(this.menu.chamberRow() - (int) Math.signum(sy));
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    /** Over the chamber grid, scrollbar included. */
    private boolean overGrid(double mx, double my) {
        int l = leftPos + HorseStasisBankMenu.MARGIN;
        int t = topPos + HorseStasisBankMenu.GRID_Y;
        int r = leftPos + HorseStasisBankMenu.SCROLL_X + HorseStasisBankMenu.SCROLL_W;
        return mx >= l && mx < r && my >= t && my < t + HorseStasisBankMenu.GRID_H;
    }

    /**
     * Let the filter box have the keystroke if it wants one - otherwise the
     * container screen closes on the inventory key and an "e" cannot be typed
     * into a query.
     *
     * <p><b>Escape is handled first</b>, for the reason the equestrian bench's
     * own override spells out: {@code canConsumeInput()} is true for every key
     * once the box has focus, Escape included, so guarding on it alone traps the
     * player in the window.
     */
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE) {
            if (this.filterBox != null && this.filterBox.isFocused()) {
                this.filterBox.setFocused(false);
            }
            return super.keyPressed(event);
        }
        if (this.filterBox != null && this.filterBox.isActive()
                && (this.filterBox.keyPressed(event) || this.filterBox.canConsumeInput())) {
            return true;
        }
        return super.keyPressed(event);
    }

    /** The box belongs to one tab, and an invisible box must not eat keystrokes. */
    private void applyTab() {
        this.menu.setTab(tab);
        if (this.filterBox != null) {
            boolean browsing = tab == HorseStasisBankMenu.Tab.BROWSE;
            this.filterBox.visible = browsing;
            this.filterBox.active = browsing;
            if (!browsing) {
                this.filterBox.setFocused(false);
            }
        }
    }

    private void requestRoster() {
        ClientPacketDistributor.sendToServer(HorseRosterRequestPayload.INSTANCE);
    }

    private HorseStasisBankMenu.Tab tabAt(double mx, double my) {
        int tx = leftPos + 4;
        int ty = topPos - TAB_H;
        for (HorseStasisBankMenu.Tab t : TABS) {
            int w = this.font.width(t.label()) + 18;
            if (mx >= tx && mx < tx + w && my >= ty && my < ty + TAB_H) {
                return t;
            }
            tx += w + 2;
        }
        return null;
    }

    // ------------------------------------------------------------------
    // The rows
    // ------------------------------------------------------------------

    /**
     * Rebuild only when something moved: a chamber in or out of the bank, a
     * roster landing, or a keystroke in the filter. A list redraws every frame
     * and resolving a horse's coat and disorders is not free, which is what
     * {@link ClientHorseRoster} already says about doing it per row per frame.
     */
    private void refreshRows() {
        String query = this.filterBox == null ? "" : this.filterBox.getValue();
        int contents = contentSignature();
        int version = ClientHorseRoster.version();
        if (contents == builtFrom && version == builtVersion && query.equals(builtQuery)) {
            return;
        }
        if (contents != builtFrom || version != builtVersion) {
            List<StasisBrowseRow> built = new ArrayList<>();
            for (HorseStasisBankMenu.Stored stored : this.menu.stored()) {
                built.add(new StasisBrowseRow(stored.slot(), stored.snapshot().horseName(), stored.tier(),
                        ClientHorseRoster.byId(stored.snapshot().horseId()), stored.atStud()));
            }
            rows = List.copyOf(built);
        }
        shown = StasisBrowseRow.filter(rows, query);
        builtFrom = contents;
        builtVersion = version;
        builtQuery = query;
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    /**
     * Cheap enough to take every frame: which horse is in which slot, and in
     * what. No tag is decoded - the id and the tier are all this asks for.
     */
    private int contentSignature() {
        int hash = 1;
        for (HorseStasisBankMenu.Stored stored : this.menu.stored()) {
            hash = hash * 31 + stored.snapshot().horseId().hashCode();
            hash = hash * 31 + stored.tier().ordinal();
            hash = hash * 31 + stored.slot();
            hash = hash * 31 + (stored.atStud() ? 1 : 0);
        }
        return hash;
    }

    private int maxScroll() {
        return Math.max(0, shown.size() - HorseStasisBankMenu.LIST_ROWS);
    }

    private boolean overList(double mx, double my) {
        int l = leftPos + HorseStasisBankMenu.MARGIN;
        int t = topPos + HorseStasisBankMenu.LIST_Y;
        return mx >= l && mx < l + HorseStasisBankMenu.LIST_W
                && my >= t && my < t + HorseStasisBankMenu.LIST_H;
    }

    private int rowAt(double mx, double my) {
        if (!overList(mx, my)) {
            return -1;
        }
        int i = scroll + (int) ((my - (topPos + HorseStasisBankMenu.LIST_Y)) / HorseStasisBankMenu.ROW_H);
        return i >= 0 && i < shown.size() ? i : -1;
    }

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        applyTab();
        refreshRows();

        // Tabs first, so the window's own bevel draws over the active one's
        // bottom edge and the two read as joined.
        int tx = leftPos + 4;
        for (HorseStasisBankMenu.Tab t : TABS) {
            int w = this.font.width(t.label()) + 18;
            VanillaPanel.tab(g, tx, topPos - TAB_H, w, TAB_H + 4, t == tab);
            g.text(this.font, Component.literal(t.label()), tx + 9, topPos - TAB_H + 5,
                    t == tab ? VanillaPanel.TEXT : VanillaPanel.TEXT_DIM, false);
            tx += w + 2;
        }

        VanillaPanel.window(g, leftPos, topPos, HorseStasisBankMenu.WIDTH, HorseStasisBankMenu.HEIGHT);

        if (tab == HorseStasisBankMenu.Tab.CHAMBERS) {
            drawChambers(g);
        } else if (tab == HorseStasisBankMenu.Tab.BROWSE) {
            VanillaPanel.well(g, leftPos + HorseStasisBankMenu.MARGIN,
                    topPos + HorseStasisBankMenu.FILTER_Y,
                    HorseStasisBankMenu.LIST_W, HorseStasisBankMenu.FILTER_H);
            drawList(g, mouseX, mouseY);
        } else {
            drawSupply(g);
        }

        for (int i = 0; i < 27; i++) {
            VanillaPanel.slot(g, leftPos + HorseStasisBankMenu.MARGIN + (i % 9) * 18,
                    topPos + HorseStasisBankMenu.INV_Y + (i / 9) * 18);
        }
        for (int i = 0; i < 9; i++) {
            VanillaPanel.slot(g, leftPos + HorseStasisBankMenu.MARGIN + i * 18,
                    topPos + HorseStasisBankMenu.HOTBAR_Y);
        }
    }

    /**
     * <b>The Chambers tab</b>: six rows of a grid that is
     * {@link HorseStasisBankBlockEntity#ROWS} deep, and the bar that says so.
     *
     * <p>Only the wells actually on screen are drawn - the slots for the rows
     * above and below have been moved out of the window by
     * {@link HorseStasisBankMenu#setChamberRow} and report
     * {@code isActive() == false}, so vanilla neither draws nor hit-tests them,
     * and a well drawn under one would be a hole with nothing behind it.
     */
    private void drawChambers(GuiGraphicsExtractor g) {
        int top = this.menu.chamberRow();
        int first = top * HorseStasisBankBlockEntity.COLS;
        int last = Math.min(HorseStasisBankBlockEntity.SLOTS,
                first + HorseStasisBankMenu.VISIBLE_ROWS * HorseStasisBankBlockEntity.COLS);
        for (int i = first; i < last; i++) {
            VanillaPanel.slot(g, leftPos + HorseStasisBankMenu.chamberX(i),
                    topPos + HorseStasisBankMenu.chamberY(i, top));
        }
        drawGridScrollbar(g, top);
    }

    /**
     * The grid's scrollbar - the Browse list's, in the four pixels between the
     * last column and the window's shadow. Drawn rather than dragged, like that
     * one: the wheel is how this list is moved, and a thumb nobody can grab is
     * still the only thing that says there are rows below.
     */
    private void drawGridScrollbar(GuiGraphicsExtractor g, int top) {
        int x = leftPos + HorseStasisBankMenu.SCROLL_X;
        int y = topPos + HorseStasisBankMenu.GRID_Y;
        int h = HorseStasisBankMenu.GRID_H;
        int w = HorseStasisBankMenu.SCROLL_W;
        int thumbH = Math.max(6, h * HorseStasisBankMenu.VISIBLE_ROWS / HorseStasisBankBlockEntity.ROWS);
        int thumbY = y + (h - thumbH) * top / HorseStasisBankMenu.maxChamberRow();
        g.fill(x, y, x + w, y + h, VanillaPanel.SHADOW);
        g.fill(x, thumbY, x + w, thumbY + thumbH, VanillaPanel.FACE);
    }

    /**
     * <b>The Supply tab</b>: three slots, and a sentence saying what the bank is
     * actually doing with them.
     *
     * <p>That sentence is the whole point of the tab. A bank that quietly heals
     * nothing has four possible reasons - no chamber it can see inside, no feed,
     * no water, or nothing hurt - and three of them look identical from outside.
     * {@link #supplyLine} says which, in the order a player would fix them.
     */
    private void drawSupply(GuiGraphicsExtractor g) {
        int l = leftPos + HorseStasisBankMenu.MARGIN;
        int t = topPos + HorseStasisBankMenu.FILTER_Y;

        drawFitted(g, "Feed and water the horses filed here.", l + 1, t, HorseStasisBankMenu.LIST_W,
                0xFF3F3F3F);

        String[] captions = {"Feed", "Water", "Empties"};
        for (int i = 0; i < HorseStasisBankBlockEntity.FIRST_DROP_SLOT; i++) {
            int sx = leftPos + HorseStasisBankMenu.supplyX(i);
            VanillaPanel.slot(g, sx, topPos + HorseStasisBankMenu.supplyY(i));
            int w = this.font.width(captions[i]);
            g.text(this.font, Component.literal(captions[i]), sx + 8 - w / 2,
                    topPos + HorseStasisBankMenu.SUPPLY_Y + 20, 0xFF4A4A4A, false);
        }

        drawFitted(g, waterLine(), l + 1, topPos + HorseStasisBankMenu.SUPPLY_Y + 34,
                HorseStasisBankMenu.LIST_W, 0xFF202020);
        drawFitted(g, supplyLine(), l + 1, topPos + HorseStasisBankMenu.SUPPLY_Y + 34 + LINE_H,
                HorseStasisBankMenu.LIST_W, 0xFF3F3F3F);

        // The drop buffer, on the chamber grid's own pitch so the two rows of
        // the window line up. Captioned with what is in it rather than what it
        // is for: an empty buffer under a bank of Basic chambers is not broken,
        // and dropsLine() is what says so.
        int caption = topPos + HorseStasisBankMenu.DROPS_Y - LINE_H;
        drawFitted(g, dropsLine(), l + 1, caption, HorseStasisBankMenu.LIST_W - 26, 0xFF3F3F3F);
        drawRight(g, this.menu.dropsHeld() + "/" + HorseStasisBankBlockEntity.DROP_SLOTS,
                l + HorseStasisBankMenu.LIST_W - 1, caption, 0xFF4A4A4A);
        for (int i = HorseStasisBankBlockEntity.FIRST_DROP_SLOT;
                i < HorseStasisBankBlockEntity.SUPPLY_SLOTS; i++) {
            VanillaPanel.slot(g, leftPos + HorseStasisBankMenu.supplyX(i),
                    topPos + HorseStasisBankMenu.supplyY(i));
        }
    }

    /** What the drop buffer is doing, in the order a player would fix it. */
    private String dropsLine() {
        if (this.menu.dropsHeld() >= HorseStasisBankBlockEntity.DROP_SLOTS) {
            return "Collected - full, so nothing more is made.";
        }
        if (!this.menu.anyCollecting()) {
            return "Collected - an advanced chamber fills this.";
        }
        return "Collected from the horses filed here.";
    }

    /**
     * The meter in the unit a player can act on - hearts, not the internal
     * points, since a horse's bar is what they are watching.
     */
    private String waterLine() {
        int water = this.menu.water();
        if (water <= 0) {
            return "No water. A bucket fills the meter.";
        }
        return "Water: enough for " + (water / 2) + (water / 2 == 1 ? " heart" : " hearts") + ".";
    }

    /** Why nothing is happening, or what is - in the order a player would fix it. */
    private String supplyLine() {
        // A mare who cannot be put down is the loudest of these: it is the one
        // thing here that is about the room rather than about the slots, and it
        // is the one a player would never work out from looking at the bank.
        if (this.menu.foalingBlocked()) {
            return "A mare is due - clear a space beside the bank.";
        }
        if (this.menu.occupied() == 0) {
            return "No horses filed here to look after.";
        }
        if (!this.menu.anyHealable()) {
            return "Basic chambers only - the bank cannot look inside one.";
        }
        if (this.menu.feed().isEmpty()) {
            return "Nothing in the feed slot.";
        }
        if (this.menu.water() <= 0) {
            return "Fed, but dry - healing needs both.";
        }
        return "Mending one horse at a time, slowly.";
    }

    private void drawList(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int l = leftPos + HorseStasisBankMenu.MARGIN;
        int t = topPos + HorseStasisBankMenu.LIST_Y;
        int w = HorseStasisBankMenu.LIST_W;
        int h = HorseStasisBankMenu.LIST_H;
        VanillaPanel.well(g, l - 1, t - 1, w + 2, h + 2);

        if (rows.isEmpty()) {
            drawFitted(g, "No horses filed here yet.", l + 3, t + 3, w - 6, 0xFF3F3F3F);
            drawFitted(g, "Chambers with a horse in show up here.",
                    l + 3, t + 3 + LINE_H, w - 6, 0xFF3F3F3F);
            return;
        }
        if (!ClientHorseRoster.received()) {
            // Said rather than guessed at: with no roster every row would read
            // "no stable record", which is a different and much worse claim
            // than "the papers have not arrived yet".
            drawFitted(g, "Reading your stable's papers...", l + 3, t + 3, w - 6, 0xFF3F3F3F);
            return;
        }
        if (shown.isEmpty()) {
            drawFitted(g, "Nothing matches that.", l + 3, t + 3, w - 6, 0xFF3F3F3F);
            drawFitted(g, lockedLine(), l + 3, t + 3 + LINE_H, w - 6, 0xFF3F3F3F);
            return;
        }

        int hovered = rowAt(mouseX, mouseY);
        for (int i = scroll; i < shown.size() && i < scroll + HorseStasisBankMenu.LIST_ROWS; i++) {
            int ry = t + (i - scroll) * HorseStasisBankMenu.ROW_H;
            if (i == hovered) {
                g.fill(l, ry, l + w, ry + HorseStasisBankMenu.ROW_H, VanillaPanel.HOVER);
            }
            StasisBrowseRow row = shown.get(i);
            // A row at stud is necessarily a Spacer, so saying so costs no
            // information: the one word the corner had is implied by the one it
            // has instead.
            boolean stud = row.atStud() && row.mayStud();
            int rightW = drawRight(g, stud ? "at stud" : capitalise(row.tier().id()),
                    l + w - 4, ry + 3, stud ? 0xFF2F5F2F : 0xFF4A4A4A);
            drawFitted(g, row.displayName(), l + 3, ry + 3, w - 10 - rightW, 0xFF202020);
            rightW = drawRight(g, row.origin(), l + w - 4, ry + 3 + LINE_H, 0xFF4A4A4A);
            drawFitted(g, row.detail(), l + 3, ry + 3 + LINE_H, w - 10 - rightW, 0xFF3F3F3F);
        }

        // One spare row's worth of explanation, in the first empty row rather
        // than in a line of its own - a horse dropping out of the list
        // unexplained is the thing this tab must not do, and a click that
        // appears to do nothing is the second.
        int drawn = Math.min(shown.size() - scroll, HorseStasisBankMenu.LIST_ROWS);
        if (drawn < HorseStasisBankMenu.LIST_ROWS) {
            String spare = spareLine();
            if (!spare.isEmpty()) {
                drawFitted(g, spare, l + 3, t + drawn * HorseStasisBankMenu.ROW_H + 3, w - 6, 0xFF4A4A4A);
            }
        }

        if (maxScroll() > 0) {
            int x1 = l + w - 3;
            int thumbH = Math.max(6, h * HorseStasisBankMenu.LIST_ROWS / shown.size());
            int thumbY = t + (h - thumbH) * scroll / maxScroll();
            g.fill(x1, t, x1 + 3, t + h, VanillaPanel.SHADOW);
            g.fill(x1, thumbY, x1 + 3, thumbY + thumbH, VanillaPanel.FACE);
        }
    }

    /**
     * <b>The one line the list has room for</b>, and what wins it: the last
     * refusal, then the rows a query could not reach, then what putting a horse
     * to stud is for.
     *
     * <p>The order is the order of what the player just did. A refusal answers a
     * click they made a second ago; the locked count answers a query they typed;
     * the stud line answers neither and is there to be read when nothing else is
     * going on, which is exactly when a player discovers that these rows are
     * clickable at all.
     */
    private String spareLine() {
        if (!note.isEmpty()) {
            return note;
        }
        if (StasisBrowseRow.locked(rows) > 0 && !builtQuery.trim().isEmpty()) {
            return lockedLine();
        }
        return studLine();
    }

    /** What the bank's own paddock is doing, or how to start one. */
    private String studLine() {
        int stud = StasisBrowseRow.atStud(rows);
        if (stud == 0) {
            return anySpacer() ? "Click a spacer row to put that horse to stud." : "";
        }
        if (stud == 1) {
            return "1 at stud - a mare and a stallion both, to breed.";
        }
        return stud + " at stud - covered once a heat, as in a field.";
    }

    private boolean anySpacer() {
        for (StasisBrowseRow row : rows) {
            if (row.mayStud()) {
                return true;
            }
        }
        return false;
    }

    /** Why the list is shorter than the bank: the rows no query can reach. */
    private String lockedLine() {
        int locked = StasisBrowseRow.locked(rows);
        if (locked == 0) {
            return "Every chamber here was searched.";
        }
        return locked + (locked == 1 ? " chamber a search" : " chambers a search")
                + " cannot reach - Basic, or no papers.";
    }

    /**
     * Right-aligned, never squeezed: these are short words. Returns the width it
     * took, so the left-hand string knows how much room it has left.
     */
    private int drawRight(GuiGraphicsExtractor g, String text, int right, int y, int colour) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int w = this.font.width(text);
        g.text(this.font, Component.literal(text), right - w, y, colour, false);
        return w;
    }

    /**
     * Left-aligned, squeezed down (never up) so a long name still fits. The
     * research shelf's helper, for its reason: this window is
     * {@link HorseStasisBankMenu#WIDTH} wide and a long name drawn raw runs out
     * through the frame.
     */
    private void drawFitted(GuiGraphicsExtractor g, String text, int x, int y, int maxW, int colour) {
        if (text == null || text.isEmpty() || maxW <= 0) {
            return;
        }
        float w = this.font.width(text);
        if (w <= maxW || w <= 0) {
            g.text(this.font, Component.literal(text), x, y, colour, false);
            return;
        }
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(maxW / w);
        g.text(this.font, Component.literal(text), 0, 0, colour, false);
        pose.popMatrix();
    }

    private static String capitalise(String word) {
        if (word == null || word.isEmpty()) {
            return "";
        }
        return word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1);
    }

    /**
     * The two captions vanilla draws, plus the count on the title's right.
     *
     * <p><b>Window-relative coordinates</b>: the caller has already translated to
     * {@code (leftPos, topPos)}, and adding them again is what threw the research
     * shelf's title off its window.
     */
    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, this.title, HorseStasisBankMenu.MARGIN, TITLE_Y, VanillaPanel.TEXT, false);
        g.text(this.font, this.playerInventoryTitle, HorseStasisBankMenu.MARGIN,
                HorseStasisBankMenu.INV_LABEL_Y, VanillaPanel.TEXT, false);

        Component count = Component.literal(countLine());
        int x = HorseStasisBankMenu.WIDTH - HorseStasisBankMenu.MARGIN - this.font.width(count);
        g.text(this.font, count, x, TITLE_Y, VanillaPanel.TEXT_DIM, false);
    }

    /**
     * <b>What a glance at the bank should tell you</b>: how many animals are in
     * there. The empties are worth counting separately - a bank with forty spare
     * chambers in it and a bank with forty horses in it look identical otherwise,
     * since every tier shares one item model. With a filter typed, the same
     * corner answers the question the player is actually asking: how many of them
     * came back.
     */
    private String countLine() {
        if (tab == HorseStasisBankMenu.Tab.BROWSE && !builtQuery.trim().isEmpty()) {
            return shown.size() + " of " + rows.size();
        }
        int filed = this.menu.filed();
        if (filed == 0) {
            return "empty";
        }
        int horses = this.menu.occupied();
        if (horses == 0) {
            return filed + (filed == 1 ? " chamber" : " chambers");
        }
        return horses + (horses == 1 ? " horse" : " horses") + " of " + filed;
    }
}
