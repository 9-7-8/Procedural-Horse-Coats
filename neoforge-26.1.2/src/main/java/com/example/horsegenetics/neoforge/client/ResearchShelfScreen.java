package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.block.EquineResearchShelfBlockEntity;
import com.example.horsegenetics.neoforge.menu.ResearchShelfMenu;
import com.example.horsegenetics.neoforge.network.ShelfActionPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.List;

/**
 * <b>The Equine Research Shelf's screen.</b> Two tabs across the top:
 * <b>Craft</b> copies a gene the shelf holds onto a blank book, <b>Store</b> is
 * the shelf's own contents.
 *
 * <h2>It is drawn as a Minecraft window</h2>
 * Face, bevel, sunken slots, dark text - see {@link VanillaPanel} for why that
 * is drawn rather than blitted from a texture. The first version of this screen
 * was a flat dark panel in the browser's own style, which looked like a
 * different mod bolted on; worse, its list and its slots were placed by eye and
 * <b>overlapped</b>. Everything here is now on one column of constants, laid out
 * top to bottom, with {@link ResearchShelfMenu}'s slot coordinates taken from
 * the same numbers.
 *
 * <h2>Which slots are live is the tab</h2>
 * The book and result slots belong to Craft and the filing slot to Store, and
 * the ones that do not belong go <b>inactive</b>: {@code Slot.x} and
 * {@code Slot.y} are final, and {@code Slot.isActive()} is what vanilla
 * consults for drawing <i>and</i> hit-testing - the pair that must never
 * disagree, because a slot you cannot see but can still click is the worst of
 * both.
 *
 * <h2>The list is drawn, not slotted</h2>
 * A shelf holds one paper per gene and there is no cap on how many, so the list
 * cannot be a grid of slots without inventing one. A row is a gene; on Craft it
 * selects, on Store it takes the paper back. Filing goes the other way through a
 * real slot, because putting an item <i>in</i> is a thing players already know
 * how to do and a list row is not.
 */
public final class ResearchShelfScreen extends AbstractContainerScreen<ResearchShelfMenu> {

    private enum Tab {
        CRAFT("Craft"),
        STORE("Store");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    private static final int TAB_H = 16;
    private static final int TITLE_Y = 6;

    private static final int ROW_H = 12;
    private static final int LIST_ROWS = 4;
    private static final int LIST_H = LIST_ROWS * ROW_H;

    private Tab tab = Tab.CRAFT;
    private int scroll;

    public ResearchShelfScreen(ResearchShelfMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ResearchShelfMenu.WIDTH, ResearchShelfMenu.HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = ResearchShelfMenu.MARGIN;
        this.titleLabelY = TITLE_Y;
        this.inventoryLabelX = ResearchShelfMenu.MARGIN;
        this.inventoryLabelY = ResearchShelfMenu.INV_LABEL_Y;
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Tab hit = tabAt(event.x(), event.y());
        if (hit != null) {
            if (hit != tab && this.menu.getCarried().isEmpty()) {
                tab = hit;
                scroll = 0;
                this.menu.setStoreTab(tab == Tab.STORE);
            }
            return true;
        }
        int row = rowAt(event.x(), event.y());
        if (row >= 0) {
            // Craft: a row is the gene the next book becomes.
            // Store: a row is a paper you can take back.
            ClientPacketDistributor.sendToServer(new ShelfActionPayload(
                    tab == Tab.STORE ? ShelfActionPayload.Action.WITHDRAW
                            : ShelfActionPayload.Action.SELECT,
                    stored().get(row)));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (sy != 0 && overList(mx, my)) {
            scroll = Math.max(0, Math.min(scroll - (int) Math.signum(sy), maxScroll()));
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    private List<String> stored() {
        return this.menu.storedGenes();
    }

    private int maxScroll() {
        return Math.max(0, stored().size() - LIST_ROWS);
    }

    private boolean overList(double mx, double my) {
        int l = leftPos + ResearchShelfMenu.MARGIN;
        int t = topPos + ResearchShelfMenu.LIST_Y;
        return mx >= l && mx < l + ResearchShelfMenu.LIST_W && my >= t && my < t + LIST_H;
    }

    private int rowAt(double mx, double my) {
        if (!overList(mx, my)) {
            return -1;
        }
        int i = scroll + (int) ((my - (topPos + ResearchShelfMenu.LIST_Y)) / ROW_H);
        return i >= 0 && i < stored().size() ? i : -1;
    }

    private Tab tabAt(double mx, double my) {
        int tx = leftPos + 4;
        int ty = topPos - TAB_H;
        for (Tab t : Tab.values()) {
            int w = this.font.width(t.label) + 18;
            if (mx >= tx && mx < tx + w && my >= ty && my < ty + TAB_H) {
                return t;
            }
            tx += w + 2;
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        this.menu.setStoreTab(tab == Tab.STORE);

        // Tabs first, so the window's own bevel draws over the active one's
        // bottom edge and the two read as joined.
        int tx = leftPos + 4;
        for (Tab t : Tab.values()) {
            int w = this.font.width(t.label) + 18;
            VanillaPanel.tab(g, tx, topPos - TAB_H, w, TAB_H + 4, t == tab);
            g.text(this.font, Component.literal(t.label), tx + 9, topPos - TAB_H + 5,
                    t == tab ? VanillaPanel.TEXT : VanillaPanel.TEXT_DIM, false);
            tx += w + 2;
        }

        VanillaPanel.window(g, leftPos, topPos, ResearchShelfMenu.WIDTH, ResearchShelfMenu.HEIGHT);
        drawList(g, mouseX, mouseY);

        if (tab == Tab.CRAFT) {
            drawCraft(g);
        } else {
            VanillaPanel.slot(g, leftPos + ResearchShelfMenu.FILE_X, topPos + ResearchShelfMenu.SLOT_Y);
            g.text(this.font, Component.literal("Put a research paper here to file it"),
                    leftPos + ResearchShelfMenu.MARGIN, topPos + ResearchShelfMenu.NOTE_Y,
                    VanillaPanel.TEXT_DIM, false);
        }

        for (int i = 0; i < 27; i++) {
            VanillaPanel.slot(g, leftPos + ResearchShelfMenu.MARGIN + (i % 9) * 18,
                    topPos + ResearchShelfMenu.INV_Y + (i / 9) * 18);
        }
        for (int i = 0; i < 9; i++) {
            VanillaPanel.slot(g, leftPos + ResearchShelfMenu.MARGIN + i * 18,
                    topPos + ResearchShelfMenu.HOTBAR_Y);
        }
    }

    private void drawCraft(GuiGraphicsExtractor g) {
        int slotY = topPos + ResearchShelfMenu.SLOT_Y;
        VanillaPanel.slot(g, leftPos + ResearchShelfMenu.BOOK_X, slotY);
        VanillaPanel.slot(g, leftPos + ResearchShelfMenu.RESULT_X, slotY);

        // The arrow between them fills as the copy is written - the furnace's
        // idea, and the only honest way to tell somebody who has just put a book
        // in that something is happening.
        int barX = leftPos + ResearchShelfMenu.BOOK_X + 24;
        int barY = slotY + 7;
        int barW = ResearchShelfMenu.RESULT_X - ResearchShelfMenu.BOOK_X - 28;
        VanillaPanel.well(g, barX, barY, barW, 4);
        int total = this.menu.copyTotal();
        int done = this.menu.copyProgress();
        if (done > 0 && total > 0) {
            g.fill(barX + 1, barY + 1,
                    barX + 1 + Math.min(barW - 2, (barW - 2) * done / total), barY + 3, 0xFF3BA55D);
        }

        String selected = this.menu.selectedGene();
        int noteX = leftPos + ResearchShelfMenu.MARGIN;
        int noteY = topPos + ResearchShelfMenu.NOTE_Y;
        if (selected.isEmpty()) {
            g.text(this.font, Component.literal("Pick a gene above, then add a book"),
                    noteX, noteY, VanillaPanel.TEXT_DIM, false);
            return;
        }
        String name = EquineResearchShelfBlockEntity.displayName(selected);
        // Seconds, not ticks - nobody thinks in ticks, and the number is the
        // whole point of the rarity rule being visible at all.
        int seconds = Math.max(1, total / 20);
        String note = done > 0
                ? name + " - " + Math.max(1, (total - done) / 20) + "s left"
                : name + " - " + seconds + "s with a book in";
        drawFitted(g, note, noteX, noteY, ResearchShelfMenu.LIST_W, VanillaPanel.TEXT);
    }

    private void drawList(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int l = leftPos + ResearchShelfMenu.MARGIN;
        int t = topPos + ResearchShelfMenu.LIST_Y;
        int w = ResearchShelfMenu.LIST_W;
        VanillaPanel.well(g, l - 1, t - 1, w + 2, LIST_H + 2);

        List<String> genes = stored();
        if (genes.isEmpty()) {
            g.text(this.font, Component.literal("This shelf is empty."), l + 3, t + 2, 0xFF3F3F3F, false);
            g.text(this.font, Component.literal(tab == Tab.STORE
                            ? "File a paper below to start it off."
                            : "Nothing filed yet - see the Store tab."),
                    l + 3, t + 2 + ROW_H, 0xFF3F3F3F, false);
            return;
        }
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        int hovered = rowAt(mouseX, mouseY);
        String selected = this.menu.selectedGene();
        for (int i = scroll; i < genes.size() && i < scroll + LIST_ROWS; i++) {
            int ry = t + (i - scroll) * ROW_H;
            boolean isSelected = tab == Tab.CRAFT && genes.get(i).equals(selected);
            if (isSelected) {
                g.fill(l, ry, l + w, ry + ROW_H, VanillaPanel.SELECTED);
            } else if (i == hovered) {
                g.fill(l, ry, l + w, ry + ROW_H, VanillaPanel.HOVER);
            }
            drawFitted(g, EquineResearchShelfBlockEntity.displayName(genes.get(i)),
                    l + 3, ry + 2, w - 8, isSelected ? 0xFF202020 : 0xFF303030);
        }
        if (maxScroll() > 0) {
            int x1 = l + w - 3;
            int thumbH = Math.max(6, LIST_H * LIST_ROWS / genes.size());
            int thumbY = t + (LIST_H - thumbH) * scroll / maxScroll();
            g.fill(x1, t, x1 + 3, t + LIST_H, VanillaPanel.SHADOW);
            g.fill(x1, thumbY, x1 + 3, thumbY + thumbH, VanillaPanel.FACE);
        }
    }

    /** Left-aligned, squeezed down (never up) so a long gene name still fits its row. */
    private void drawFitted(GuiGraphicsExtractor g, String text, int x, int y, int maxW, int colour) {
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

    /** Vanilla draws both captions in {@code #404040}; this window is vanilla-coloured. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, this.title, leftPos + ResearchShelfMenu.MARGIN, topPos + TITLE_Y,
                VanillaPanel.TEXT, false);
        g.text(this.font, this.playerInventoryTitle, leftPos + ResearchShelfMenu.MARGIN,
                topPos + ResearchShelfMenu.INV_LABEL_Y, VanillaPanel.TEXT, false);
    }
}
