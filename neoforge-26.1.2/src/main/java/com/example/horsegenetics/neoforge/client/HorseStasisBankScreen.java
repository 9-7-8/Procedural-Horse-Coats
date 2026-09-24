package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.block.HorseStasisBankBlockEntity;
import com.example.horsegenetics.neoforge.menu.HorseStasisBankMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * <b>The Horse Stasis Bank's screen.</b> A chest of stasis chambers, and a count
 * of how many of them have a horse in.
 *
 * <h2>One tab, and no tab bar</h2>
 * The research shelf draws two tabs above its window; this draws none, because a
 * single-tab tab bar is a control that does nothing. The <b>Browse</b> tab is
 * stage three of the build order on {@code wiki/horse-stasis.html} and brings the
 * bar with it - {@link ResearchShelfScreen}'s {@code Tab} enum, {@code tabAt} and
 * {@code VanillaPanel.tab} are the three pieces to copy at that point, along with
 * the menu's {@code activeOnTab} half.
 *
 * <h2>It is drawn as a Minecraft window</h2>
 * Face, bevel, sunken slots, dark text - see {@link VanillaPanel}, no texture.
 * Every position comes from {@link HorseStasisBankMenu}'s constants, the same
 * numbers its slots are placed with.
 *
 * <h2>The count is read, not synced</h2>
 * Each chamber carries its whole horse in a data component, so the client already
 * knows which slots are occupied and {@code occupied()} is a loop over the slots
 * rather than anything on the wire.
 */
public final class HorseStasisBankScreen extends AbstractContainerScreen<HorseStasisBankMenu> {

    private static final int TITLE_Y = 6;

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
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        VanillaPanel.window(g, leftPos, topPos, HorseStasisBankMenu.WIDTH, HorseStasisBankMenu.HEIGHT);

        for (int i = 0; i < HorseStasisBankBlockEntity.SLOTS; i++) {
            VanillaPanel.slot(g, leftPos + HorseStasisBankMenu.MARGIN + (i % 9) * 18,
                    topPos + HorseStasisBankMenu.GRID_Y + (i / 9) * 18);
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
     * The two captions vanilla draws, plus the horse count on the title's right.
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
     * since every tier shares one item model.
     */
    private String countLine() {
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
