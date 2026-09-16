package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.menu.EquestrianBenchMenu;
import com.example.horsegenetics.neoforge.network.BenchNamePayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * <b>The Equestrian Bench's screen.</b> A saddle, three labelled material slots,
 * a name field, and one result.
 *
 * <h2>Three boxes, not a selector</h2>
 * This had a zone picker and it is gone (owner, 2026-09-16): one row per zone,
 * each labelled, so all three can be dyed in a single pass. A zone whose slot is
 * empty keeps the colour it already had, so the empty rows are not "no colour",
 * they are "leave this one alone".
 *
 * <h2>The name field is the anvil's</h2>
 * Same widget and, importantly, the same <b>keyboard guard</b>: a container
 * screen would otherwise close on the inventory key, so typing an "e" into a
 * name would shut the window. {@code canConsumeInput} is what vanilla's anvil
 * uses to decide whether the box wants the keystroke, and this copies it rather
 * than inventing a rule.
 */
public final class EquestrianBenchScreen extends AbstractContainerScreen<EquestrianBenchMenu> {

    private static final int[] ZONE_YS = {
            EquestrianBenchMenu.SEAT_Y, EquestrianBenchMenu.BRIDLE_Y, EquestrianBenchMenu.METAL_Y };

    private EditBox nameBox;

    /** Survives a resize, which rebuilds every widget from scratch. */
    private String typed = "";

    public EquestrianBenchScreen(EquestrianBenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, EquestrianBenchMenu.WIDTH, EquestrianBenchMenu.HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = EquestrianBenchMenu.MARGIN;
        this.titleLabelY = EquestrianBenchMenu.TITLE_Y;
        this.inventoryLabelX = EquestrianBenchMenu.MARGIN;
        this.inventoryLabelY = EquestrianBenchMenu.INV_LABEL_Y;

        this.nameBox = new EditBox(this.font,
                leftPos + EquestrianBenchMenu.NAME_X + 1, topPos + EquestrianBenchMenu.NAME_Y + 3,
                EquestrianBenchMenu.NAME_W - 2, EquestrianBenchMenu.NAME_H - 5,
                Component.literal("Saddle name"));
        // Unbordered, like the anvil's: the sunken well drawn behind it is the
        // frame, so the box should not draw a second one of its own.
        this.nameBox.setBordered(false);
        this.nameBox.setMaxLength(BenchNamePayload.MAX_LENGTH);
        this.nameBox.setValue(this.typed);
        this.nameBox.setResponder(text -> {
            this.typed = text;
            ClientPacketDistributor.sendToServer(new BenchNamePayload(text));
        });
        this.addRenderableWidget(this.nameBox);
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(this.nameBox);
    }

    /**
     * Let the box have the keystroke if it wants one - otherwise the container
     * screen closes on the inventory key and you cannot type an "e" into a name.
     *
     * <p><b>Escape is handled first, and that is not a detail.</b>
     * {@code EditBox.canConsumeInput()} is {@code isActive() && isFocused() &&
     * isEditable()}, which is true for <i>every</i> key once the box has focus -
     * Escape included. Guarding on it alone means {@code super.keyPressed} never
     * runs, and a player who clicks into the name field is trapped in the window
     * with no way out (reported 2026-09-16, and this screen takes focus on open,
     * so it happened immediately). Escape unfocuses the box and falls through.
     */
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE) {
            if (this.nameBox != null && this.nameBox.isFocused()) {
                this.nameBox.setFocused(false);
            }
            return super.keyPressed(event);
        }
        if (this.nameBox != null && (this.nameBox.keyPressed(event) || this.nameBox.canConsumeInput())) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);

        VanillaPanel.window(g, leftPos, topPos, EquestrianBenchMenu.WIDTH, EquestrianBenchMenu.HEIGHT);

        // The name field sits in a sunken well, so it reads as something to type in.
        VanillaPanel.well(g, leftPos + EquestrianBenchMenu.NAME_X, topPos + EquestrianBenchMenu.NAME_Y,
                EquestrianBenchMenu.NAME_W, EquestrianBenchMenu.NAME_H);

        VanillaPanel.slot(g, leftPos + EquestrianBenchMenu.SADDLE_X, topPos + EquestrianBenchMenu.SADDLE_Y);
        VanillaPanel.slot(g, leftPos + EquestrianBenchMenu.RESULT_X, topPos + EquestrianBenchMenu.RESULT_Y);

        // The labels follow whatever is in the slot: a saddle has a seat and a
        // bridle, a caparison has neither, so naming the armour's rows "Seat"
        // and "Bridle" would simply be wrong (owner, 2026-09-16). An empty slot
        // shows the saddle's words, since that is the commoner errand.
        String[] labels = com.example.horsegenetics.neoforge.data.SaddleTint.zoneNames(
                this.menu.slots.get(EquestrianBenchMenu.SLOT_SADDLE).getItem());
        for (int i = 0; i < ZONE_YS.length; i++) {
            int y = topPos + ZONE_YS[i];
            VanillaPanel.slot(g, leftPos + EquestrianBenchMenu.ZONE_X, y);
            g.text(this.font, Component.literal(labels[i]),
                    leftPos + EquestrianBenchMenu.ZONE_LABEL_X, y + 5, VanillaPanel.TEXT, false);
        }

        for (int i = 0; i < 27; i++) {
            VanillaPanel.slot(g, leftPos + EquestrianBenchMenu.MARGIN + (i % 9) * 18,
                    topPos + EquestrianBenchMenu.INV_Y + (i / 9) * 18);
        }
        for (int i = 0; i < 9; i++) {
            VanillaPanel.slot(g, leftPos + EquestrianBenchMenu.MARGIN + i * 18,
                    topPos + EquestrianBenchMenu.INV_Y + 3 * 18 + 4);
        }
    }

    /**
     * Window-relative coordinates: the caller has already translated to
     * {@code (leftPos, topPos)}, and adding them again is what threw the
     * research shelf's labels off its window.
     */
    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, this.title, EquestrianBenchMenu.MARGIN,
                EquestrianBenchMenu.TITLE_Y, VanillaPanel.TEXT, false);
        g.text(this.font, this.playerInventoryTitle, EquestrianBenchMenu.MARGIN,
                EquestrianBenchMenu.INV_LABEL_Y, VanillaPanel.TEXT, false);
    }
}
