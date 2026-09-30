package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.entity.HorseTackSlot;
import com.example.horsegenetics.neoforge.menu.HorseGearMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

/**
 * <b>The Dress window</b> - the horse on a paper doll with its nineteen gear
 * slots around it, and the player's inventory on the right. A real container
 * screen: pick a piece up, put it down, shift-click it across.
 *
 * <p>The doll is the Gear tab's doll, drawn by the same rule - each slot at
 * {@code HorseTackSlot.anchorX/anchorY} of the box - so a slot is in the same
 * place on the horse in both, and the roster is still the only layout. The
 * horse turns under a drag, as it does on the tab; the slots do not move with
 * it, and it opens on the three-quarter view they are placed for.
 *
 * <p><b>Escape goes back to the information screen</b> it was opened from, the
 * way following a link there walks back. Walking out of range or the horse
 * dying closes it outright - that close comes from the server, not through
 * {@link #onClose}, so it does not drop the player into a screen about a horse
 * they have just left.
 */
public final class HorseGearScreen extends AbstractContainerScreen<HorseGearMenu> {

    /** The information screen's opening pose - see its DOLL_YAW for why three-quarter. */
    private static final float DOLL_YAW = 52.0f;
    private static final float DOLL_PITCH = 6.0f;
    private static final float DOLL_PITCH_LIMIT = 75.0f;
    private static final float DOLL_DEGREES_PER_PIXEL = 2.0f;

    /** Over a slot this horse cannot use yet - a foal's - so it reads as closed, not missing. */
    private static final int CLOSED = 0x90C6C6C6;

    /**
     * Where Escape should go, handed over by the information screen just before
     * it asks the server to open this. Keyed by the horse so a stale one - the
     * server refused, and some other menu opened later - is never picked up.
     */
    private static @Nullable Screen pendingReturn;
    private static int pendingReturnHorse = -1;

    private final @Nullable Screen parent;
    private float dollYaw = DOLL_YAW;
    private float dollPitch = DOLL_PITCH;
    private boolean draggingDoll;

    public HorseGearScreen(HorseGearMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, HorseGearMenu.WIDTH, HorseGearMenu.HEIGHT);
        if (pendingReturnHorse == menu.horseId()) {
            this.parent = pendingReturn;
        } else {
            this.parent = null;
        }
        pendingReturn = null;
        pendingReturnHorse = -1;
    }

    /** Called by the information screen as it sends the open request. */
    static void returnTo(Screen parent, int horseId) {
        pendingReturn = parent;
        pendingReturnHorse = horseId;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = HorseGearMenu.MARGIN;
        this.titleLabelY = HorseGearMenu.TITLE_Y;
        this.inventoryLabelX = HorseGearMenu.INV_X;
        this.inventoryLabelY = HorseGearMenu.INV_LABEL_Y;
    }

    @Override
    public void onClose() {
        super.onClose();
        if (this.parent != null) {
            Minecraft.getInstance().setScreen(this.parent);
        }
    }

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        VanillaPanel.window(g, leftPos, topPos, HorseGearMenu.WIDTH, HorseGearMenu.HEIGHT);

        // The horse first, so a slot that overlaps it sits on the animal.
        AbstractHorse horse = this.menu.horse();
        int[] box = portraitBox();
        HorsePortrait.drawPosed(g, coat(), horse != null && horse.isBaby(),
                box[0], box[1], box[2] - box[0], box[3] - box[1], dollYaw, dollPitch);

        for (HorseTackSlot slot : HorseTackSlot.values()) {
            int x = leftPos + HorseGearMenu.slotX(slot);
            int y = topPos + HorseGearMenu.slotY(slot);
            VanillaPanel.slot(g, x, y);
            if (horse == null || !slot.usableOn(horse)) {
                g.fill(x, y, x + 16, y + 16, CLOSED);
            }
        }

        for (int i = 0; i < 27; i++) {
            VanillaPanel.slot(g, leftPos + HorseGearMenu.INV_X + (i % 9) * 18,
                    topPos + HorseGearMenu.INV_Y + (i / 9) * 18);
        }
        for (int i = 0; i < 9; i++) {
            VanillaPanel.slot(g, leftPos + HorseGearMenu.INV_X + i * 18,
                    topPos + HorseGearMenu.INV_Y + 3 * 18 + 4);
        }
    }

    /** Window-relative: the caller has already translated to the window. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, this.title, HorseGearMenu.MARGIN, HorseGearMenu.TITLE_Y,
                VanillaPanel.TEXT, false);
        g.text(this.font, this.playerInventoryTitle, HorseGearMenu.INV_X, HorseGearMenu.INV_LABEL_Y,
                VanillaPanel.TEXT, false);

        AbstractHorse horse = this.menu.horse();
        String note = horse != null && horse.isBaby()
                ? "A foal wears nothing. Every slot opens when it grows up."
                : "Drag the horse to turn it. Shift-click a piece to put it on, "
                        + "or to take it off.";
        int y = HorseGearMenu.NOTE_Y;
        for (String line : GuiText.wrap(this.font, note, 9 * 18)) {
            g.text(this.font, Component.literal(line), HorseGearMenu.INV_X, y,
                    VanillaPanel.TEXT_DIM, false);
            y += this.font.lineHeight + 1;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        // An empty gear slot says what goes in it, as the Gear tab's does. A full
        // one gets the item's own tooltip from the container screen.
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem()
                && this.menu.getCarried().isEmpty()) {
            HorseTackSlot tack = HorseGearMenu.tackAt(this.hoveredSlot.index);
            if (tack != null) {
                g.setTooltipForNextFrame(Component.literal(tack.hint()), mouseX, mouseY);
            }
        }
    }

    private @Nullable com.example.horsegenetics.common.coat.CoatData coat() {
        return ClientCoatCache.get(this.menu.horseId());
    }

    /** {x0, y0, x1, y1} of the horse's portrait, in screen space. */
    private int[] portraitBox() {
        int dx = leftPos + HorseGearMenu.DOLL_X;
        int dy = topPos + HorseGearMenu.DOLL_Y;
        return new int[] {
                dx + Math.round(HorseGearMenu.DOLL_W * HorseGearMenu.PORTRAIT_X0),
                dy + Math.round(HorseGearMenu.DOLL_H * HorseGearMenu.PORTRAIT_Y0),
                dx + Math.round(HorseGearMenu.DOLL_W * HorseGearMenu.PORTRAIT_X1),
                dy + Math.round(HorseGearMenu.DOLL_H * HorseGearMenu.PORTRAIT_Y1)};
    }

    // ------------------------------------------------------------------
    // Turning the horse
    // ------------------------------------------------------------------

    /**
     * A press on the horse turns it - but only after the slots have had it,
     * since several sit on the animal, and only with nothing on the cursor, so
     * a piece being carried across the doll is never mistaken for a grab.
     */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && this.menu.getCarried().isEmpty()
                && overPortrait(event.x(), event.y()) && !overTackSlot(event.x(), event.y())) {
            draggingDoll = true;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingDoll) {
            dollYaw = Mth.wrapDegrees(dollYaw + (float) dx * DOLL_DEGREES_PER_PIXEL);
            dollPitch = Mth.clamp(dollPitch + (float) dy * DOLL_DEGREES_PER_PIXEL,
                    -DOLL_PITCH_LIMIT, DOLL_PITCH_LIMIT);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingDoll) {
            draggingDoll = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    private boolean overPortrait(double mx, double my) {
        int[] box = portraitBox();
        return mx >= box[0] && mx < box[2] && my >= box[1] && my < box[3];
    }

    private boolean overTackSlot(double mx, double my) {
        for (HorseTackSlot slot : HorseTackSlot.values()) {
            int x = leftPos + HorseGearMenu.slotX(slot) - 1;
            int y = topPos + HorseGearMenu.slotY(slot) - 1;
            if (mx >= x && mx < x + HorseGearMenu.SLOT && my >= y && my < y + HorseGearMenu.SLOT) {
                return true;
            }
        }
        return false;
    }
}
