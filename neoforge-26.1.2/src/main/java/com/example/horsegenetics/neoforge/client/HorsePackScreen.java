package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.menu.HorsePackMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The screen for {@link HorsePackMenu}: a chest on a horse whose slot count
 * vanilla has no chest screen for. Drawn from {@link VanillaPanel} rather than
 * from a texture, because the window is whatever size the count makes it.
 *
 * <p><b>Not seen in a running game.</b>
 */
public final class HorsePackScreen extends AbstractContainerScreen<HorsePackMenu> {

    public HorsePackScreen(HorsePackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, menu.width(), menu.height());
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = HorsePackMenu.MARGIN;
        this.titleLabelY = 6;
        this.inventoryLabelX = this.menu.inventoryX();
        this.inventoryLabelY = this.menu.inventoryY() - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        VanillaPanel.window(g, leftPos, topPos, this.menu.width(), this.menu.height());
        // Slot positions are the item's corner; the frame sits a pixel outside it.
        for (int i = 0; i < this.menu.size(); i++) {
            VanillaPanel.slot(g,
                    leftPos + this.menu.gridX() + (i % this.menu.columns()) * HorsePackMenu.SLOT,
                    topPos + HorsePackMenu.GRID_Y + (i / this.menu.columns()) * HorsePackMenu.SLOT);
        }
        int invX = leftPos + this.menu.inventoryX();
        int invY = topPos + this.menu.inventoryY();
        for (int i = 0; i < 27; i++) {
            VanillaPanel.slot(g, invX + (i % 9) * 18, invY + (i / 9) * 18);
        }
        for (int i = 0; i < 9; i++) {
            VanillaPanel.slot(g, invX + i * 18, invY + 3 * 18 + 4);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, this.title, this.titleLabelX, this.titleLabelY, VanillaPanel.TEXT, false);
        g.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                VanillaPanel.TEXT, false);
    }
}
