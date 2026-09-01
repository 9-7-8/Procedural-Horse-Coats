package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.genetics.GeneCodeDisplay;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.ParentStats;
import com.example.horsegenetics.neoforge.network.SetBarnNamePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Client hooks on the vanilla horse inventory screen (E while riding a tamed
 * horse). Everything this mod adds - a metadata panel, an editable barn-name
 * field, and a <b>"View Family Tree"</b> button - sits in a <b>grey
 * vanilla-style panel to the left of the horse GUI</b>, behind a collapsible
 * tab (the small button on its edge). Nothing here replaces the screen or
 * removes other listeners, so a second horse-inventory mod keeps working;
 * toggle our tab off to get it fully out of the way.
 *
 * <p>The panel fill is drawn in {@link ScreenEvent.Render.Background} (behind
 * the widgets, in front of the backdrop); {@link #onKeyPressed} keeps
 * <kbd>E</kbd> from closing the screen while the barn-name box is focused.
 *
 * <p>Layout constants below are eyeballed, not visually verified.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class HorseScreenHooks {

    /** Session-wide: remembered across screen re-inits (resize, reopen). */
    private static boolean panelOpen = true;

    private static Button tabButton;
    private static Button familyTreeButton;
    private static Button setBarnButton;
    private static EditBox barnBox;

    private static final int PANEL_W = 128;
    private static final int PANEL_H = 164;

    // vanilla container-panel palette. Text is drawn WITHOUT a drop shadow
    // (see the `false` arg on every g.text call below) - on the light-grey
    // FACE a shadowed dark string turns into unreadable grey-on-grey mush,
    // exactly like a vanilla inventory label would.
    private static final int FACE = 0xFFC6C6C6;
    private static final int HILIGHT = 0xFFFFFFFF;
    private static final int SHADOW = 0xFF555555;
    private static final int LABEL = 0xFF404040;   // vanilla container-label grey
    private static final int VALUE = 0xFF1B1B1B;   // near-black for the primary values

    private static final int STAT_UP = 0xFF1A6B1A;   // above both parents
    private static final int STAT_MID = 0xFF8A5E00;  // above one
    private static final int STAT_DOWN = 0xFFA61B1B; // below both

    @SubscribeEvent
    static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof HorseInventoryScreen screen)) {
            return;
        }
        HorseRecord record = currentRecord();

        int px = panelLeft(screen);
        int py = panelTop(screen);

        // tab toggle - sits on the panel's right edge, between it and the GUI
        tabButton = Button.builder(Component.literal(panelOpen ? "<" : "i"), b -> togglePanel())
                .bounds(px + PANEL_W, py, 12, 20).build();
        event.addListener(tabButton);

        familyTreeButton = Button.builder(Component.literal("View Family Tree"), b -> openFamilyTree())
                .bounds(px + 6, py + PANEL_H - 48, PANEL_W - 12, 18).build();
        event.addListener(familyTreeButton);

        barnBox = new EditBox(Minecraft.getInstance().font, px + 6, py + PANEL_H - 26, PANEL_W - 44, 16,
                Component.literal("Barn name"));
        barnBox.setMaxLength(HorseRecord.MAX_BARN_NAME);
        barnBox.setHint(Component.literal("barn name"));
        if (record != null) {
            record.barnName().ifPresent(barnBox::setValue);
        }
        event.addListener(barnBox);

        setBarnButton = Button.builder(Component.literal("Set"), b -> submitBarn(barnBox.getValue()))
                .bounds(px + PANEL_W - 34, py + PANEL_H - 26, 28, 16).build();
        event.addListener(setBarnButton);

        applyVisibility();
    }

    /**
     * Drawn in {@link ScreenEvent.Render.Background} - the hook that fires
     * <em>after</em> the screen's dimmed backdrop but <em>before</em> the widget
     * layer. That's the only place a filled rect ends up behind our buttons /
     * edit box (so they stay visible and clickable) yet in front of the dark
     * backdrop (so the panel isn't a murky grey rectangle). Text goes in the
     * same pass, after the fill, so it sits on top of the grey.
     */
    @SubscribeEvent
    static void onPanelRender(ScreenEvent.Render.Background event) {
        if (!(event.getScreen() instanceof HorseInventoryScreen screen)) {
            return;
        }
        if (tabButton != null) {
            tabButton.setMessage(Component.literal(panelOpen ? "<" : "i"));
        }
        if (!panelOpen) {
            return;
        }
        HorseRecord r = currentRecord();
        if (r == null) {
            return;
        }

        var g = event.getGuiGraphics();
        var font = Minecraft.getInstance().font;
        int px = panelLeft(screen);
        int py = panelTop(screen);

        // grey bevelled panel
        g.fill(px, py, px + PANEL_W, py + PANEL_H, FACE);
        g.fill(px, py, px + PANEL_W, py + 1, HILIGHT);
        g.fill(px, py, px + 1, py + PANEL_H, HILIGHT);
        g.fill(px, py + PANEL_H - 1, px + PANEL_W, py + PANEL_H, SHADOW);
        g.fill(px + PANEL_W - 1, py, px + PANEL_W, py + PANEL_H, SHADOW);

        AbstractHorse horse = ridingHorse();
        boolean adult = horse == null || !horse.isBaby();
        int tx = px + 7;
        int ty = py + 7;

        g.text(font, Component.literal(clip(r.displayName(), 21)), tx, ty, VALUE, false);
        ty += 11;
        if (r.barnName().isPresent()) {
            g.text(font, Component.literal(clip("(" + (r.firstName() + " " + r.lastName()).strip() + ")", 22)),
                    tx, ty, LABEL, false);
            ty += 10;
        }
        g.text(font, Component.literal(r.sex().label(adult) + "   gen " + r.generation()), tx, ty, LABEL, false);
        ty += 11;
        for (String line : wrap(font, shortGenes(r), PANEL_W - 14)) {
            g.text(font, Component.literal(line), tx, ty, VALUE, false);
            ty += 10;
        }
        ty += 1;

        String speedValue = r.hasStats() ? String.format("%.3f", r.speed()) : "-";
        g.text(font, Component.literal("speed "), tx, ty, LABEL, false);
        g.text(font, Component.literal(speedValue), tx + font.width("speed "), ty, statColor(r, true), false);
        ty += 10;
        String healthValue = r.hasStats() ? String.format("%.0f", r.health()) : "-";
        g.text(font, Component.literal("health "), tx, ty, LABEL, false);
        g.text(font, Component.literal(healthValue), tx + font.width("health "), ty, statColor(r, false), false);
        ty += 11;

        g.text(font, Component.literal(clip(attributionLine(r), 22)), tx, ty, LABEL, false);
    }

    /**
     * While the barn-name box has focus, keep the vanilla inventory keybind (and
     * any other key) from reaching the screen - otherwise pressing <kbd>E</kbd>
     * mid-word closes the whole GUI and the edit is lost. Escape still closes;
     * Enter submits and unfocuses; every other key is forwarded to the box so
     * backspace / arrows keep working. (Character input rides a separate event,
     * so letters still type.)
     */
    @SubscribeEvent
    static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof HorseInventoryScreen)) {
            return;
        }
        if (barnBox == null || !panelOpen || !barnBox.isFocused()) {
            return;
        }
        int key = event.getKeyCode();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            return; // let escape close the screen as usual
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            submitBarn(barnBox.getValue());
            barnBox.setFocused(false);
        } else {
            barnBox.keyPressed(event.getKeyEvent()); // backspace / delete / arrows / home / end
        }
        event.setCanceled(true);
    }

    private static int statColor(HorseRecord r, boolean speed) {
        if (!r.hasStats() || r.parentStats().isEmpty()) {
            return VALUE;
        }
        ParentStats ps = r.parentStats().get();
        int rank = speed ? ps.rankSpeed(r.speed()) : ps.rankHealth(r.health());
        return rank > 0 ? STAT_UP : rank < 0 ? STAT_DOWN : STAT_MID;
    }

    // --- helpers ---

    private static int panelLeft(HorseInventoryScreen screen) {
        int guiLeft = ((AbstractContainerScreen<?>) screen).getGuiLeft();
        return Math.max(2, guiLeft - PANEL_W - 8);
    }

    private static int panelTop(HorseInventoryScreen screen) {
        return ((AbstractContainerScreen<?>) screen).getGuiTop();
    }

    private static void togglePanel() {
        panelOpen = !panelOpen;
        applyVisibility();
    }

    private static void applyVisibility() {
        if (familyTreeButton != null) familyTreeButton.visible = panelOpen;
        if (setBarnButton != null) setBarnButton.visible = panelOpen;
        if (barnBox != null) barnBox.visible = panelOpen;
    }

    private static String attributionLine(HorseRecord r) {
        if (r.bredBy().isPresent()) {
            return "bred by " + r.bredBy().get();
        }
        return "tamed by " + r.tamedBy().orElse("(untamed)");
    }

    private static String clip(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    /** The compact gene string (degrades gracefully if the stored code won't parse). */
    private static String shortGenes(HorseRecord r) {
        return GeneCodeDisplay.shortForm(r.geneticCode());
    }

    /** Greedy word-wrap to {@code maxWidth} pixels, so the gene line never spills out of the panel. */
    private static java.util.List<String> wrap(Font font, String s, int maxWidth) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : s.split(" ")) {
            String candidate = cur.length() == 0 ? word : cur + " " + word;
            if (cur.length() > 0 && font.width(candidate) > maxWidth) {
                lines.add(cur.toString());
                cur = new StringBuilder(word);
            } else {
                cur = new StringBuilder(candidate);
            }
        }
        if (cur.length() > 0) {
            lines.add(cur.toString());
        }
        return lines;
    }

    private static HorseRecord currentRecord() {
        AbstractHorse horse = ridingHorse();
        return horse == null ? null : ClientHorseRecordCache.get(horse.getId());
    }

    private static void submitBarn(String value) {
        AbstractHorse horse = ridingHorse();
        if (horse != null) {
            ClientPacketDistributor.sendToServer(new SetBarnNamePayload(horse.getId(), value));
        }
    }

    private static void openFamilyTree() {
        HorseRecord record = currentRecord();
        if (record != null) {
            Minecraft.getInstance().setScreen(new FamilyTreeScreen(record));
        }
    }

    private static AbstractHorse ridingHorse() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getVehicle() instanceof AbstractHorse horse ? horse : null;
    }

    private HorseScreenHooks() {
    }
}
