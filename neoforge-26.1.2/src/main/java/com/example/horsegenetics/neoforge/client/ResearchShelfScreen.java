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
 * <b>Store</b>, a chest of research papers, and <b>Craft</b>, which copies a
 * gene the shelf holds onto a blank book.
 *
 * <h2>Store opens first, and it is a chest</h2>
 * Six rows of nine, where a double chest's are. Put a paper in, take it out -
 * nothing takes time and nothing is consumed. It replaced a single filing slot
 * that swallowed the paper into a list, which the owner found did "nothing"
 * (2026-09-10). A paper for a gene the shelf already holds will not go in.
 *
 * <h2>It is drawn as a Minecraft window</h2>
 * Face, bevel, sunken slots, dark text - see {@link VanillaPanel}. Every
 * position comes from {@link ResearchShelfMenu}'s constants, the same numbers
 * its slots are placed with.
 *
 * <h2>Which slots are live is the tab</h2>
 * The paper grid belongs to Store and the book and result slots to Craft; the
 * ones that do not belong go <b>inactive</b>, because {@code Slot.isActive()}
 * is what vanilla consults for drawing <i>and</i> hit-testing.
 */
public final class ResearchShelfScreen extends AbstractContainerScreen<ResearchShelfMenu> {

    private enum Tab {
        STORE("Store"),
        CRAFT("Copy");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    private static final int TAB_H = 16;
    private static final int TITLE_Y = 6;
    private static final int ROW_H = 12;

    /**
     * The tab the shelf opens on: <b>whichever was showing last time</b>, for the
     * rest of the session (owner's request, 2026-09-10). Client-wide rather than
     * per shelf - the question is "what was I doing", not "what was this one".
     */
    private static Tab lastTab = Tab.STORE;

    private Tab tab = lastTab;
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
        this.menu.setStoreTab(tab == Tab.STORE);
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
                lastTab = hit;
                scroll = 0;
                this.menu.setStoreTab(tab == Tab.STORE);
            }
            return true;
        }
        if (tab == Tab.CRAFT) {
            int row = rowAt(event.x(), event.y());
            if (row >= 0) {
                String gene = stored().get(row);
                this.menu.selectGene(gene); // the highlight, client-side
                ClientPacketDistributor.sendToServer(
                        new ShelfActionPayload(ShelfActionPayload.Action.SELECT, gene));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (tab == Tab.CRAFT && sy != 0 && overList(mx, my)) {
            scroll = Math.max(0, Math.min(scroll - (int) Math.signum(sy), maxScroll()));
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    private List<String> stored() {
        return this.menu.storedGenes();
    }

    private int maxScroll() {
        return Math.max(0, stored().size() - ResearchShelfMenu.LIST_ROWS);
    }

    private boolean overList(double mx, double my) {
        int l = leftPos + ResearchShelfMenu.MARGIN;
        int t = topPos + ResearchShelfMenu.LIST_Y;
        return mx >= l && mx < l + ResearchShelfMenu.LIST_W && my >= t && my < t + ResearchShelfMenu.LIST_H;
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

        if (tab == Tab.STORE) {
            for (int i = 0; i < EquineResearchShelfBlockEntity.SLOTS; i++) {
                VanillaPanel.slot(g, leftPos + ResearchShelfMenu.MARGIN + (i % 9) * 18,
                        topPos + ResearchShelfMenu.GRID_Y + (i / 9) * 18);
            }
        } else {
            drawList(g, mouseX, mouseY);
            drawCraft(g);
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
        if (selected.isEmpty() || !stored().contains(selected)) {
            drawFitted(g, "Pick a gene, then add a blank book",
                    noteX, noteY, ResearchShelfMenu.LIST_W, VanillaPanel.TEXT_DIM);
            return;
        }
        String name = EquineResearchShelfBlockEntity.displayName(selected);
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
        int h = ResearchShelfMenu.LIST_H;
        VanillaPanel.well(g, l - 1, t - 1, w + 2, h + 2);

        List<String> genes = stored();
        if (genes.isEmpty()) {
            drawFitted(g, "No papers on this shelf yet.", l + 3, t + 2, w - 6, 0xFF3F3F3F);
            drawFitted(g, "Put some in on the Store tab.", l + 3, t + 2 + ROW_H, w - 6, 0xFF3F3F3F);
            return;
        }
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        int hovered = rowAt(mouseX, mouseY);
        String selected = this.menu.selectedGene();
        for (int i = scroll; i < genes.size() && i < scroll + ResearchShelfMenu.LIST_ROWS; i++) {
            int ry = t + (i - scroll) * ROW_H;
            boolean isSelected = genes.get(i).equals(selected);
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
            int thumbH = Math.max(6, h * ResearchShelfMenu.LIST_ROWS / genes.size());
            int thumbY = t + (h - thumbH) * scroll / maxScroll();
            g.fill(x1, t, x1 + 3, t + h, VanillaPanel.SHADOW);
            g.fill(x1, thumbY, x1 + 3, thumbY + thumbH, VanillaPanel.FACE);
        }
    }

    /**
     * Left-aligned, squeezed down (never up) so a long string still fits.
     * <b>Every string this window draws goes through here</b> - the window is
     * {@link ResearchShelfMenu#WIDTH} wide and captions drawn raw once ran out
     * through the frame.
     */
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

    /**
     * Vanilla draws both captions in {@code #404040}; this window is
     * vanilla-coloured. <b>Window-relative coordinates</b>: the caller has
     * already translated to {@code (leftPos, topPos)}, and adding them again is
     * what threw the title and the inventory label off the window (2026-09-10).
     */
    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, this.title, ResearchShelfMenu.MARGIN, TITLE_Y, VanillaPanel.TEXT, false);
        g.text(this.font, this.playerInventoryTitle, ResearchShelfMenu.MARGIN,
                ResearchShelfMenu.INV_LABEL_Y, VanillaPanel.TEXT, false);
    }
}
