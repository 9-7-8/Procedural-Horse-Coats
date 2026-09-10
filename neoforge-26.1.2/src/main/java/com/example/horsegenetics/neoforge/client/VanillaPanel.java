package com.example.horsegenetics.neoforge.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * <b>Minecraft's own window, drawn rather than blitted.</b>
 *
 * <h2>Why not a texture</h2>
 * A vanilla container window is a nine-slice PNG, and shipping one means
 * drawing art, guessing at a size, and re-drawing it the next time the layout
 * moves. But the thing that <i>reads</i> as Minecraft is not the texture, it is
 * the <b>bevel</b>: a flat mid-grey face, a white highlight along the top and
 * left, a dark grey shadow along the bottom and right, and slots with that
 * bevel inverted so they look punched in rather than raised.
 *
 * <p>Those are four fills and two colours, and drawing them means the window is
 * whatever size the layout needs with no art to keep in step. It is the same
 * palette vanilla uses, so a screen built out of this sits beside a chest
 * without looking like a different mod.
 *
 * <h2>The palette</h2>
 * Straight from vanilla's GUI sprite sheet: {@code #C6C6C6} face,
 * {@code #FFFFFF} highlight, {@code #555555} shadow, {@code #8B8B8B} slot well.
 * Text on a panel this light has to be <b>dark</b> - {@link #TEXT} is the
 * {@code #404040} vanilla uses for container labels, and white text on it is
 * unreadable.
 */
public final class VanillaPanel {

    public static final int FACE = 0xFFC6C6C6;
    public static final int HIGHLIGHT = 0xFFFFFFFF;
    public static final int SHADOW = 0xFF555555;
    public static final int SLOT = 0xFF8B8B8B;
    public static final int BORDER = 0xFF000000;

    /** Vanilla's container label colour. Anything lighter is unreadable on {@link #FACE}. */
    public static final int TEXT = 0xFF404040;
    /** The same, softened - for a hint rather than a label. */
    public static final int TEXT_DIM = 0xFF6E6E6E;
    /** A row the player has selected. Vanilla's slot highlight. */
    public static final int SELECTED = 0x80FFFFFF;
    public static final int HOVER = 0x40FFFFFF;

    private static final int BEVEL = 3;

    private VanillaPanel() {
    }

    /** The window itself: face, highlight up and left, shadow down and right. */
    public static void window(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, FACE);
        // Highlight - top and left, inset by the bevel so the corner reads round.
        g.fill(x, y, x + w - BEVEL, y + BEVEL, HIGHLIGHT);
        g.fill(x, y, x + BEVEL, y + h - BEVEL, HIGHLIGHT);
        // Shadow - bottom and right.
        g.fill(x + BEVEL, y + h - BEVEL, x + w, y + h, SHADOW);
        g.fill(x + w - BEVEL, y + BEVEL, x + w, y + h, SHADOW);
    }

    /**
     * One 16x16 slot, at the coordinates the <em>item</em> is drawn at - so a
     * caller passes the same numbers it gave the {@code Slot}, and the well is
     * drawn one pixel out on every side, the way vanilla's is.
     */
    public static void slot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, SLOT);
        g.fill(x - 1, y - 1, x + 17, y, SHADOW);
        g.fill(x - 1, y - 1, x, y + 17, SHADOW);
        g.fill(x, y + 16, x + 17, y + 17, HIGHLIGHT);
        g.fill(x + 16, y, x + 17, y + 17, HIGHLIGHT);
    }

    /** A sunken well for a list or a text area - a slot's bevel at any size. */
    public static void well(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, SLOT);
        g.fill(x, y, x + w, y + 1, SHADOW);
        g.fill(x, y, x + 1, y + h, SHADOW);
        g.fill(x + 1, y + h - 1, x + w, y + h, HIGHLIGHT);
        g.fill(x + w - 1, y + 1, x + w, y + h, HIGHLIGHT);
    }

    /**
     * A tab above the window. {@code active} draws it in the window's own face
     * so it reads as part of it; an inactive one is darker and set down a pixel,
     * which is how vanilla's creative-inventory tabs say the same thing.
     */
    public static void tab(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean active) {
        int top = active ? y : y + 1;
        g.fill(x, top, x + w, y + h, active ? FACE : 0xFF9A9A9A);
        g.fill(x, top, x + w - BEVEL, top + BEVEL, active ? HIGHLIGHT : 0xFFB8B8B8);
        g.fill(x, top, x + BEVEL, y + h, active ? HIGHLIGHT : 0xFFB8B8B8);
        g.fill(x + w - BEVEL, top + BEVEL, x + w, y + h, SHADOW);
    }
}
