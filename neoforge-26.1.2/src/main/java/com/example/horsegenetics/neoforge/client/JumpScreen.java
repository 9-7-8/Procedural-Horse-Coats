package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.block.JumpBlock;
import com.example.horsegenetics.neoforge.block.JumpMaterials;
import com.example.horsegenetics.neoforge.block.JumpWoods;
import com.example.horsegenetics.neoforge.menu.JumpMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * <b>A jump's screen.</b> Two plank slots and three style buttons.
 *
 * <h2>Nothing here decides anything</h2>
 * Every control sends and then waits. A plank or a dye goes into a slot and the
 * <i>server</i> performs the exchange; a style button sends its id through
 * {@code handleInventoryButtonClick} and the <i>server</i> sets the blockstate.
 * What this draws - the two wood names, the two paint swatches, which button is
 * pressed in - is read straight off the block itself, so the window is always
 * showing the block rather than showing what the player just asked for. That
 * matters for the one case a local prediction would get wrong: two players with
 * the same jump open.
 *
 * <h2>The current style is the disabled button</h2>
 * Rather than a tick, a highlight or a fourth widget. A greyed-out button reads
 * as "already this" to anyone who has used a vanilla screen, and it also stops
 * the pointless round trip of setting the style it already has.
 *
 * @see JumpMenu for the exchange, and for why the woods travel as indices
 */
public final class JumpScreen extends AbstractContainerScreen<JumpMenu> {

    private static final Component RAILS = Component.translatable("horsegenetics.jump.rails");
    private static final Component STANDARDS = Component.translatable("horsegenetics.jump.standards");
    private static final Component STYLE = Component.translatable("horsegenetics.jump.style");
    private static final Component SIZE = Component.translatable("horsegenetics.jump.size");
    private static final Component SHORTER = Component.literal("\u2212");
    private static final Component TALLER = Component.literal("+");

    /** The paint swatch: where it sits in a row, and how big. */
    private static final int SWATCH_X = 150;
    private static final int SWATCH_SIZE = 10;

    private final Button[] styleButtons = new Button[JumpMenu.STYLE_BUTTONS];
    private Button shorter;
    private Button taller;

    public JumpScreen(JumpMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, JumpMenu.WIDTH, JumpMenu.HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = JumpMenu.MARGIN;
        this.titleLabelY = JumpMenu.TITLE_Y;
        this.inventoryLabelX = JumpMenu.MARGIN;
        this.inventoryLabelY = JumpMenu.INV_LABEL_Y;

        JumpBlock.Style[] styles = JumpBlock.Style.values();
        for (int i = 0; i < this.styleButtons.length; i++) {
            int id = i;
            Component label = Component.translatable(
                    "horsegenetics.jump.style." + styles[i].getSerializedName());
            this.styleButtons[i] = Button.builder(label, button -> press(id))
                    .bounds(this.leftPos + JumpMenu.BUTTON_X[i], this.topPos + JumpMenu.BUTTON_Y,
                            JumpMenu.BUTTON_W, JumpMenu.BUTTON_H)
                    .build();
            this.addRenderableWidget(this.styleButtons[i]);
        }
        this.shorter = Button.builder(SHORTER, button -> press(JumpMenu.BUTTON_SHORTER))
                .bounds(this.leftPos + JumpMenu.SIZE_MINUS_X, this.topPos + JumpMenu.SIZE_BUTTON_Y,
                        JumpMenu.SIZE_BUTTON_W, JumpMenu.SIZE_BUTTON_H)
                .build();
        this.taller = Button.builder(TALLER, button -> press(JumpMenu.BUTTON_TALLER))
                .bounds(this.leftPos + JumpMenu.SIZE_PLUS_X, this.topPos + JumpMenu.SIZE_BUTTON_Y,
                        JumpMenu.SIZE_BUTTON_W, JumpMenu.SIZE_BUTTON_H)
                .build();
        this.addRenderableWidget(this.shorter);
        this.addRenderableWidget(this.taller);

        syncButtons();
    }

    private void press(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    /**
     * The block can change under an open screen - a second player's window, a
     * command - so the pressed-in button is re-read every tick rather than set
     * when this one clicked.
     */
    @Override
    protected void containerTick() {
        super.containerTick();
        syncButtons();
    }

    private void syncButtons() {
        int current = this.menu.style().ordinal();
        for (int i = 0; i < this.styleButtons.length; i++) {
            if (this.styleButtons[i] != null) {
                this.styleButtons[i].active = i != current;
            }
        }
        // The ends of the ladder grey out, so the range is visible rather than
        // something you discover by clicking into nothing.
        int size = JumpMaterials.clampSize(this.menu.materials().size());
        if (this.shorter != null) {
            this.shorter.active = size > 0;
        }
        if (this.taller != null) {
            this.taller.active = size < JumpMaterials.SIZES.length - 1;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);

        VanillaPanel.window(g, leftPos, topPos, JumpMenu.WIDTH, JumpMenu.HEIGHT);
        VanillaPanel.slot(g, leftPos + JumpMenu.SLOT_X, topPos + JumpMenu.RAILS_Y);
        VanillaPanel.slot(g, leftPos + JumpMenu.SLOT_X, topPos + JumpMenu.STANDARDS_Y);

        for (int i = 0; i < 27; i++) {
            VanillaPanel.slot(g, leftPos + JumpMenu.MARGIN + (i % 9) * 18,
                    topPos + JumpMenu.INV_Y + (i / 9) * 18);
        }
        for (int i = 0; i < 9; i++) {
            VanillaPanel.slot(g, leftPos + JumpMenu.MARGIN + i * 18,
                    topPos + JumpMenu.INV_Y + 3 * 18 + 4);
        }
    }

    /**
     * Window-relative coordinates: the caller has already translated to
     * {@code (leftPos, topPos)}, and adding them again is what threw the
     * research shelf's labels off its window.
     */
    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, this.title, JumpMenu.MARGIN, JumpMenu.TITLE_Y, VanillaPanel.TEXT, false);
        g.text(this.font, this.playerInventoryTitle, JumpMenu.MARGIN, JumpMenu.INV_LABEL_Y,
                VanillaPanel.TEXT, false);

        // Two lines to a row: what the slot is for, then what is in the block
        // right now. The wood is the answer, so it gets the darker ink.
        JumpMaterials materials = this.menu.materials();
        row(g, RAILS, materials.rails(), materials.railsDye(), JumpMenu.RAILS_Y);
        row(g, STANDARDS, materials.standards(), materials.standardsDye(),
                JumpMenu.STANDARDS_Y);

        g.text(this.font, STYLE, JumpMenu.MARGIN, JumpMenu.STYLE_LABEL_Y,
                VanillaPanel.TEXT_DIM, false);

        // THE HEIGHT, AND WHAT IT MEANS. The multiple on its own is a number
        // nobody can act on; the blocks-to-clear is the thing a player is
        // actually asking about when they set one, and it is the number the
        // horse has to beat.
        g.text(this.font, SIZE, JumpMenu.MARGIN, JumpMenu.SIZE_LABEL_Y,
                VanillaPanel.TEXT_DIM, false);
        float scale = materials.scale();
        Component reading = Component.translatable("horsegenetics.jump.size.value",
                trim(scale), trim(scale + 0.5F));
        g.text(this.font, reading,
                JumpMenu.SIZE_VALUE_X - this.font.width(reading) / 2, JumpMenu.SIZE_LABEL_Y,
                VanillaPanel.TEXT, false);
    }

    /** "1" rather than "1.0", but "0.75" in full. */
    private static String trim(float value) {
        String text = String.format(java.util.Locale.ROOT, "%.2f", value);
        text = text.replaceAll("0+$", "");
        return text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
    }

    /**
     * One half's row: what the slot is for, what wood is in it, and - only if
     * it has been painted - a swatch of the colour.
     *
     * <p>A swatch rather than colouring the wood's name, which was the cheaper
     * idea: a jump painted black or dark blue would have had an unreadable
     * label, and the one thing this row has to do is say what the half is made
     * of. The paint is extra information and gets its own square.
     */
    private void row(GuiGraphicsExtractor g, Component caption, String wood, int dye, int y) {
        g.text(this.font, caption, JumpMenu.LABEL_X, y + 1, VanillaPanel.TEXT_DIM, false);
        g.text(this.font, JumpWoods.label(wood), JumpMenu.LABEL_X, y + 10,
                VanillaPanel.TEXT, false);
        if (dye != JumpMaterials.UNDYED) {
            int top = y + (18 - SWATCH_SIZE) / 2;
            // Bordered, so a swatch the colour of the panel is still a square.
            g.fill(SWATCH_X - 1, top - 1, SWATCH_X + SWATCH_SIZE + 1, top + SWATCH_SIZE + 1,
                    VanillaPanel.BORDER);
            g.fill(SWATCH_X, top, SWATCH_X + SWATCH_SIZE, top + SWATCH_SIZE,
                    0xFF000000 | dye);
        }
    }
}
