package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.care.CommandWheel;
import com.example.horsegenetics.common.care.HorseOrder;
import com.example.horsegenetics.common.care.HorseOrders;
import com.example.horsegenetics.common.care.HorseOrders.Refusal;
import com.example.horsegenetics.common.care.HorseOrders.Situation;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.network.CommandHorsePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * <b>The command whistle's wheel</b>: opened while the use button is held, picked on its
 * release. Slice geometry is {@link CommandWheel}; which slices a horse may take is
 * {@link HorseOrders}. The client decides nothing it sends - the server re-checks it all.
 *
 * <p><b>Release is read two ways</b> (26.1.2, read from the sources): opening a screen
 * calls {@code KeyMapping.releaseAll()}, so {@code options.keyUse.isDown()} is false from
 * then on and useless; the screen does receive the right button's release through
 * {@link #mouseReleased}. And {@link #tick} polls GLFW, which catches a release that
 * happened before the screen opened (a tap) or went to another window. A tap therefore
 * opens and closes the wheel within a tick, with the cursor in the dead centre: nothing
 * is picked, which is the "opens on hold, not on a tap" rule.
 *
 * <p>Does not pause single-player: the horse is right there, and the world keeps going.
 */
public final class CommandWheelScreen extends Screen {

    private static final int RING = 62;
    private static final int DEAD = 18;
    private static final int SLICE_W = 78;
    private static final int SLICE_H = 18;
    private static final int PANEL = 0xD0121218;
    private static final int HOVER = 0xF0355E3B;
    private static final int GREY = 0xA0303036;
    private static final int BORDER = 0xFF3C3C4A;
    private static final int TEXT = 0xFFF2F2F6;
    private static final int DIM = 0xFF8890A8;

    private final int entityId;
    private final boolean all;
    private final List<HorseOrder> slices = HorseOrders.wheel();
    private boolean done;
    private int mouseX;
    private int mouseY;

    private CommandWheelScreen(Component title, int entityId, boolean all) {
        super(title);
        this.entityId = entityId;
        this.all = all;
    }

    /** The wheel for one aimed horse. */
    public static void openFor(Horse horse, String name) {
        Minecraft.getInstance().setScreen(new CommandWheelScreen(Component.literal(name), horse.getId(), false));
    }

    /** The wheel for every horse of the player's in reach (sneak-hold). */
    public static void openForAll() {
        Minecraft.getInstance().setScreen(new CommandWheelScreen(Component.literal(
                "Every horse of yours within " + HorseOrders.REACH_BLOCKS + " blocks"), -1, true));
    }

    @Override
    protected void init() {
        // The dead centre until the mouse says otherwise. A tap is picked in the very first
        // tick, possibly before any render or mouse move has set these: left at (0, 0) they
        // would read as the top-left slice and send an order nobody chose.
        mouseX = width / 2;
        mouseY = height / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Horse horse() {
        if (all || minecraft == null || minecraft.level == null) {
            return null;
        }
        Entity e = minecraft.level.getEntity(entityId);
        return e instanceof Horse h ? h : null;
    }

    /**
     * Why the aimed horse would refuse this slice, as far as the client can tell (bond
     * and lead; the cart only the server knows). Null when it may take it, or for the
     * all-horses wheel, where each horse answers for itself in the summary line.
     */
    private Refusal refusal(HorseOrder order) {
        Horse h = horse();
        if (h == null) {
            return null;
        }
        ClientHorseCareCache.Care care = ClientHorseCareCache.get(h.getId());
        int bond = care == null ? 0 : care.bond();
        int tier = bond >= 81 ? 3 : bond >= 61 ? 2 : bond >= 31 ? 1 : 0;
        return HorseOrders.refusal(order, new Situation(tier, h.isLeashed(), false));
    }

    private int hovered() {
        return CommandWheel.sliceAt(mouseX - width / 2.0, mouseY - height / 2.0, slices.size(), DEAD);
    }

    @Override
    public void mouseMoved(double x, double y) {
        mouseX = (int) x;
        mouseY = (int) y;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            mouseX = (int) event.x();
            mouseY = (int) event.y();
            pick();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void tick() {
        if (minecraft != null && GLFW.glfwGetMouseButton(minecraft.getWindow().handle(),
                GLFW.GLFW_MOUSE_BUTTON_RIGHT) != GLFW.GLFW_PRESS) {
            pick();
        }
        if (!all && horse() == null) {
            onClose(); // the horse unloaded or died under the wheel
        }
    }

    /** Release: send the hovered slice if it may be taken, and close either way. */
    private void pick() {
        if (done) {
            return;
        }
        done = true;
        int i = hovered();
        if (i >= 0 && refusal(slices.get(i)) == null) {
            ClientPacketDistributor.sendToServer(new CommandHorsePayload(
                    all ? List.of() : List.of(entityId), all, slices.get(i).name()));
        }
        onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partialTick) {
        mouseX = mx;
        mouseY = my;
        int cx = width / 2;
        int cy = height / 2;
        int hover = hovered();

        g.centeredText(font, title, cx, cy - RING - SLICE_H - 14, TEXT);
        Horse h = horse();
        if (h != null) {
            HorseOrder now = h.hasData(ModAttachments.HORSE_ORDER.get())
                    ? h.getData(ModAttachments.HORSE_ORDER.get()).order() : HorseOrder.REJOIN_HERD;
            g.centeredText(font, Component.literal("Now: " + now.label()), cx, cy - RING - SLICE_H - 3, DIM);
        }

        for (int i = 0; i < slices.size(); i++) {
            double a = CommandWheel.centreAngle(i, slices.size());
            int sx = cx + (int) Math.round(Math.sin(a) * RING);
            int sy = cy - (int) Math.round(Math.cos(a) * RING);
            Refusal r = refusal(slices.get(i));
            int l = sx - SLICE_W / 2;
            int t = sy - SLICE_H / 2;
            g.fill(l - 1, t - 1, l + SLICE_W + 1, t + SLICE_H + 1, BORDER);
            g.fill(l, t, l + SLICE_W, t + SLICE_H, r != null ? GREY : i == hover ? HOVER : PANEL);
            g.centeredText(font, Component.literal(slices.get(i).label()), sx, sy - 4, r != null ? DIM : TEXT);
            if (i == hover && r != null) {
                String why = r == Refusal.NOT_BONDED ? HorseOrders.bondNeeded(slices.get(i))
                        : "Not while " + r.summary();
                g.centeredText(font, Component.literal(why), cx, cy + RING + SLICE_H, DIM);
            }
        }
        g.fill(cx - 3, cy - 3, cx + 4, cy + 4, hover < 0 ? TEXT : BORDER);
        g.centeredText(font, Component.literal("Release to choose"), cx, cy + RING + SLICE_H + 12, DIM);
    }
}
