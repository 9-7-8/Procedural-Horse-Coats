package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.menu.EquestrianBenchMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * <b>The Equestrian Bench's screen.</b> Put a saddle in, pick a zone, add a dye
 * or an ingot, take the result.
 *
 * <h2>The zone picker is vanilla plumbing</h2>
 * Clicking a zone calls {@code handleInventoryButtonClick}, the same route the
 * loom's pattern picker uses, so there is <b>no custom packet</b> - unlike the
 * {@link ResearchShelfScreen research shelf}, whose selection is a gene key and
 * needed one. A zone is a small int, and vanilla already carries small ints.
 *
 * <h2>Drawn as a Minecraft window</h2>
 * Face, bevel, sunken slots - see {@link VanillaPanel} - and every position
 * comes from {@link EquestrianBenchMenu}'s constants, the same numbers its slots
 * are placed with. There is one source of truth for the layout and it is the
 * menu.
 */
public final class EquestrianBenchScreen extends AbstractContainerScreen<EquestrianBenchMenu> {

    private static final int TITLE_Y = 6;
    private static final int INV_LABEL_Y = EquestrianBenchMenu.INV_Y - 12;
    private static final int ZONE_W = 40;
    private static final int ZONE_H = 16;
    private static final int ZONE_GAP = 2;

    private static final String[] ZONE_LABELS = { "Seat", "Bridle", "Metal" };

    public EquestrianBenchScreen(EquestrianBenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, EquestrianBenchMenu.WIDTH, EquestrianBenchMenu.HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = EquestrianBenchMenu.MARGIN;
        this.titleLabelY = TITLE_Y;
        this.inventoryLabelX = EquestrianBenchMenu.MARGIN;
        this.inventoryLabelY = INV_LABEL_Y;
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int zone = zoneAt(event.x(), event.y());
        if (zone >= 0) {
            if (zone != this.menu.zone() && this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, zone);
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** Which zone button is under the cursor, or {@code -1}. */
    private int zoneAt(double mx, double my) {
        int y = topPos + EquestrianBenchMenu.ZONE_BUTTON_Y;
        if (my < y || my >= y + ZONE_H) {
            return -1;
        }
        for (int i = 0; i < ZONE_LABELS.length; i++) {
            int x = leftPos + EquestrianBenchMenu.ZONE_BUTTON_X + i * (ZONE_W + ZONE_GAP);
            if (mx >= x && mx < x + ZONE_W) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);

        VanillaPanel.window(g, leftPos, topPos, EquestrianBenchMenu.WIDTH, EquestrianBenchMenu.HEIGHT);

        VanillaPanel.slot(g, leftPos + EquestrianBenchMenu.SADDLE_X, topPos + EquestrianBenchMenu.SADDLE_Y);
        VanillaPanel.slot(g, leftPos + EquestrianBenchMenu.MATERIAL_X, topPos + EquestrianBenchMenu.MATERIAL_Y);
        VanillaPanel.slot(g, leftPos + EquestrianBenchMenu.RESULT_X, topPos + EquestrianBenchMenu.RESULT_Y);

        drawZones(g, mouseX, mouseY);

        for (int i = 0; i < 27; i++) {
            VanillaPanel.slot(g, leftPos + EquestrianBenchMenu.MARGIN + (i % 9) * 18,
                    topPos + EquestrianBenchMenu.INV_Y + (i / 9) * 18);
        }
        for (int i = 0; i < 9; i++) {
            VanillaPanel.slot(g, leftPos + EquestrianBenchMenu.MARGIN + i * 18,
                    topPos + EquestrianBenchMenu.INV_Y + 3 * 18 + 4);
        }
    }

    private void drawZones(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int selected = this.menu.zone();
        int hovered = zoneAt(mouseX, mouseY);
        int y = topPos + EquestrianBenchMenu.ZONE_BUTTON_Y;
        for (int i = 0; i < ZONE_LABELS.length; i++) {
            int x = leftPos + EquestrianBenchMenu.ZONE_BUTTON_X + i * (ZONE_W + ZONE_GAP);
            VanillaPanel.tab(g, x, y, ZONE_W, ZONE_H, i == selected);
            if (i != selected && i == hovered) {
                g.fill(x + 1, y + 1, x + ZONE_W - 1, y + ZONE_H - 1, VanillaPanel.HOVER);
            }
            int textW = this.font.width(ZONE_LABELS[i]);
            g.text(this.font, Component.literal(ZONE_LABELS[i]),
                    x + (ZONE_W - textW) / 2, y + 4,
                    i == selected ? VanillaPanel.TEXT : VanillaPanel.TEXT_DIM, false);
        }
    }

    /**
     * Window-relative coordinates: the caller has already translated to
     * {@code (leftPos, topPos)}, and adding them again is what threw the
     * shelf's labels off its window.
     */
    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, this.title, EquestrianBenchMenu.MARGIN, TITLE_Y, VanillaPanel.TEXT, false);
        g.text(this.font, this.playerInventoryTitle, EquestrianBenchMenu.MARGIN,
                INV_LABEL_Y, VanillaPanel.TEXT, false);
    }
}
