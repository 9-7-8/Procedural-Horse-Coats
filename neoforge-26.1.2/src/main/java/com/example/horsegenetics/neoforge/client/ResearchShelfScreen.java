package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.block.EquineResearchShelfBlockEntity;
import com.example.horsegenetics.neoforge.menu.ResearchShelfMenu;
import com.example.horsegenetics.neoforge.network.ShelfActionPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.List;

/**
 * <b>The Equine Research Shelf's screen.</b> Two tabs across the top, as asked
 * for: <b>Craft</b> copies a gene the shelf holds onto a blank book,
 * <b>Store</b> is the shelf's own contents.
 *
 * <h2>Which slots are live is the tab</h2>
 * The book and result slots belong to Craft and the filing slot to Store, and
 * the ones that do not belong to the tab you are on go <b>inactive</b>. That is
 * the mechanism rather than moving them: {@code Slot.x} and {@code Slot.y} are
 * final, and {@link net.minecraft.world.inventory.Slot#isActive()} is what
 * vanilla consults for drawing <i>and</i> for hit-testing - which is exactly the
 * pair that must never disagree, because a slot you cannot see but can still
 * click is the worst of both.
 *
 * <h2>The list is drawn, not slotted</h2>
 * A shelf holds one paper per gene and there is no cap on how many, so the Store
 * tab cannot be a grid of slots without inventing one. It is a scrolling list of
 * rows: a row is a gene, and clicking it takes that paper back. Filing goes the
 * other way through a single slot, because putting an item <i>in</i> is a thing
 * players already know how to do and a list row is not.
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

    private static final int PANEL = 0xF014141C;
    private static final int PANEL_SOFT = 0xE01A1A24;
    private static final int BORDER = 0xFF3C3C4A;
    private static final int TAB_ON = 0xFF2E2E3C;
    private static final int TAB_OFF = 0xFF191921;
    private static final int TAB_TEXT_ON = 0xFFFFFFFF;
    private static final int TAB_TEXT_OFF = 0xFF888F9F;
    private static final int LABEL = 0xFF8890A8;
    private static final int NAME = 0xFFE4E8F0;
    private static final int NAME_DIM = 0xFF9AA0B0;
    private static final int ROW_HOVER = 0x22FFFFFF;
    private static final int ROW_SEL = 0x3355A0E0;
    private static final int SLOT_BG = 0xFF2B2B36;
    private static final int SLOT_RESULT_BG = 0xFF473C22;

    private static final int TAB_H = 16;
    private static final int ROW_H = 12;
    private static final int LIST_X = 8;
    private static final int LIST_Y = 36;
    private static final int LIST_W = 160;
    private static final int LIST_ROWS = 5;

    private Tab tab = Tab.CRAFT;
    private int scroll;

    public ResearchShelfScreen(ResearchShelfMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 184);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    // ------------------------------------------------------------------
    // Which slots belong to which tab
    // ------------------------------------------------------------------

    /**
     * Tell the menu which tab is showing. Its slots read that from
     * {@link net.minecraft.world.inventory.Slot#isActive()}, which is what
     * decides both whether a slot draws and whether it can be clicked - the two
     * that must never disagree.
     */
    private void placeSlots() {
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
                scroll = 0;
                placeSlots();
            }
            return true;
        }
        if (tab == Tab.STORE) {
            int row = rowAt(event.x(), event.y());
            if (row >= 0) {
                // Store: a row is a paper you can take back.
                ClientPacketDistributor.sendToServer(new ShelfActionPayload(
                        ShelfActionPayload.Action.WITHDRAW, stored().get(row)));
                return true;
            }
        } else {
            int row = rowAt(event.x(), event.y());
            if (row >= 0) {
                // Craft: a row is the gene the next book becomes.
                ClientPacketDistributor.sendToServer(new ShelfActionPayload(
                        ShelfActionPayload.Action.SELECT, stored().get(row)));
                return true;
            }
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

    /**
     * The inventory key must not close a container screen out from under a
     * player who is only scrolling - and more to the point, this screen has no
     * text field, so the base class's behaviour is right. Kept explicit because
     * the browser had the opposite bug and it is the same family of trap.
     */
    @Override
    public boolean keyPressed(KeyEvent event) {
        return super.keyPressed(event);
    }

    private List<String> stored() {
        return this.menu.storedGenes();
    }

    private int maxScroll() {
        return Math.max(0, stored().size() - LIST_ROWS);
    }

    private boolean overList(double mx, double my) {
        int l = leftPos + LIST_X;
        int t = topPos + LIST_Y;
        return mx >= l && mx < l + LIST_W && my >= t && my < t + LIST_ROWS * ROW_H;
    }

    private int rowAt(double mx, double my) {
        if (!overList(mx, my)) {
            return -1;
        }
        int i = scroll + (int) ((my - (topPos + LIST_Y)) / ROW_H);
        return i >= 0 && i < stored().size() ? i : -1;
    }

    private Tab tabAt(double mx, double my) {
        int tx = leftPos + 6;
        int ty = topPos - TAB_H;
        for (Tab t : Tab.values()) {
            int w = this.font.width(t.label) + 20;
            if (mx >= tx && mx < tx + w && my >= ty && my < ty + TAB_H) {
                return t;
            }
            tx += w + 3;
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        placeSlots();

        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + 1, BORDER);
        g.fill(leftPos, topPos + imageHeight - 1, leftPos + imageWidth, topPos + imageHeight, BORDER);
        g.fill(leftPos, topPos, leftPos + 1, topPos + imageHeight, BORDER);
        g.fill(leftPos + imageWidth - 1, topPos, leftPos + imageWidth, topPos + imageHeight, BORDER);

        drawTabs(g);

        if (tab == Tab.CRAFT) {
            drawCraft(g, mouseX, mouseY);
        } else {
            drawStore(g, mouseX, mouseY);
        }

        // the player inventory's slot backdrops, on both tabs
        for (int i = 0; i < 27; i++) {
            cell(g, leftPos + 8 + (i % 9) * 18, topPos + 102 + (i / 9) * 18, SLOT_BG);
        }
        for (int i = 0; i < 9; i++) {
            cell(g, leftPos + 8 + i * 18, topPos + 160, SLOT_BG);
        }
    }

    private void drawTabs(GuiGraphicsExtractor g) {
        int tx = leftPos + 6;
        int ty = topPos - TAB_H;
        for (Tab t : Tab.values()) {
            int w = this.font.width(t.label) + 20;
            boolean on = t == tab;
            g.fill(tx, ty, tx + w, ty + TAB_H, on ? TAB_ON : TAB_OFF);
            g.fill(tx, ty, tx + w, ty + 2, on ? 0xFF55A0E0 : BORDER);
            g.text(this.font, Component.literal(t.label), tx + 10, ty + 4,
                    on ? TAB_TEXT_ON : TAB_TEXT_OFF, false);
            tx += w + 3;
        }
    }

    private void drawCraft(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        String selected = this.menu.selectedGene();
        drawList(g, mouseX, mouseY, selected, "Pick a gene, then add a book");

        cell(g, leftPos + 44, topPos + 40, SLOT_BG);
        cell(g, leftPos + 116, topPos + 40, SLOT_RESULT_BG);

        // The arrow between them fills up as the copy is written - the furnace's
        // idea, and the only honest way to say "this takes a while" to somebody
        // who has just put a book in and is waiting for something to happen.
        int barX = leftPos + 74;
        int barY = topPos + 47;
        int barW = 34;
        g.fill(barX, barY, barX + barW, barY + 2, 0xFF3A3A46);
        int total = this.menu.copyTotal();
        int done = this.menu.copyProgress();
        if (done > 0 && total > 0) {
            g.fill(barX, barY, barX + Math.min(barW, barW * done / total), barY + 2, 0xFF9BE08A);
        }

        String note = selected.isEmpty()
                ? "No gene picked."
                : EquineResearchShelfBlockEntity.displayName(selected);
        g.text(this.font, Component.literal(note), leftPos + 8, topPos + 88,
                selected.isEmpty() ? NAME_DIM : NAME, false);
        if (!selected.isEmpty()) {
            // Seconds, not ticks: nobody thinks in ticks, and the number is the
            // whole point of the rarity rule being visible at all.
            int seconds = Math.max(1, total / 20);
            String time = done > 0
                    ? "Copying - " + Math.max(1, (total - done) / 20) + "s left"
                    : "Takes " + seconds + "s with a book in";
            g.text(this.font, Component.literal(time), leftPos + 8, topPos + 88 + this.font.lineHeight + 1,
                    LABEL, false);
        }
    }

    private void drawStore(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        drawList(g, mouseX, mouseY, "", "Click a book to take it back");
        cell(g, leftPos + 80, topPos + 64, SLOT_BG);
        g.text(this.font, Component.literal("File a paper here"), leftPos + 8, topPos + 88, LABEL, false);
    }

    /** The shelf's contents, five rows at a time. Shared by both tabs. */
    private void drawList(GuiGraphicsExtractor g, int mouseX, int mouseY,
                          String selected, String emptyHint) {
        int l = leftPos + LIST_X;
        int t = topPos + LIST_Y;
        List<String> genes = stored();

        g.fill(l - 2, t - 2, l + LIST_W + 2, t + LIST_ROWS * ROW_H + 2, PANEL_SOFT);
        if (genes.isEmpty()) {
            g.text(this.font, Component.literal("This shelf is empty."), l + 2, t + 2, NAME_DIM, false);
            g.text(this.font, Component.literal(emptyHint), l + 2, t + 2 + ROW_H, LABEL, false);
            return;
        }
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        int hovered = rowAt(mouseX, mouseY);
        for (int i = scroll; i < genes.size() && i < scroll + LIST_ROWS; i++) {
            int ry = t + (i - scroll) * ROW_H;
            if (genes.get(i).equals(selected)) {
                g.fill(l - 2, ry, l + LIST_W, ry + ROW_H, ROW_SEL);
            } else if (i == hovered) {
                g.fill(l - 2, ry, l + LIST_W, ry + ROW_H, ROW_HOVER);
            }
            g.text(this.font, Component.literal(
                            EquineResearchShelfBlockEntity.displayName(genes.get(i))),
                    l + 2, ry + 2, genes.get(i).equals(selected) ? NAME : NAME_DIM, false);
        }
        if (maxScroll() > 0) {
            int trackH = LIST_ROWS * ROW_H;
            int x1 = l + LIST_W;
            g.fill(x1 - 3, t, x1, t + trackH, 0x33FFFFFF);
            int thumbH = Math.max(8, trackH * LIST_ROWS / genes.size());
            int thumbY = t + (trackH - thumbH) * scroll / maxScroll();
            g.fill(x1 - 3, thumbY, x1, thumbY + thumbH, 0xAAFFFFFF);
        }
    }

    private static void cell(GuiGraphicsExtractor g, int x, int y, int colour) {
        g.fill(x - 1, y - 1, x + 17, y + 17, colour);
    }

    /** This screen paints its own window, so vanilla's caption pair is not wanted. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, this.title, leftPos + 8, topPos + 6, NAME, false);
        g.text(this.font, this.playerInventoryTitle, leftPos + 8, topPos + imageHeight - 94, LABEL, false);
    }
}
