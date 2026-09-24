package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.network.PopulationDataPayload;
import com.example.horsegenetics.neoforge.network.PopulationRequestPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * <b>Every horse in the world on one chart.</b> A family tree that is not about
 * one horse: each row is a generation, the founders along the top, and a line
 * runs from each horse down to every foal it got. Drag it about, scroll to
 * zoom, and click a horse to see only the ones it actually bred with.
 *
 * <p>Not to be confused with {@code FamilyTreeScreen.Mode.OVERVIEW}, which is
 * that screen's both-directions-at-once view of a <i>single</i> horse. This one
 * has no subject at all.
 *
 * <h2>Where a horse sits</h2>
 * The row is the horse's own {@code generation}, which the breeding handler
 * sets to <b>one more than the higher of its two parents</b> - so a foal out of
 * a founder mare by a fourth-generation stallion is a fifth-generation horse
 * and sits five rows down, with one long line reaching up past four rows to its
 * dam. That is the honest picture of a line-bred family and it is why this
 * chart is a thicket rather than a pyramid. Generations nobody has a horse on
 * are skipped rather than drawn as empty bands, and the gutter down the left
 * says which row is which, so the vertical axis stays readable however far you
 * have panned sideways.
 *
 * <p>Within a row, horses are in name order. Nothing deeper: it is a stable
 * place to put them and it means a horse you are looking for can be found by
 * reading along rather than by hunting.
 *
 * <h2>The focus</h2>
 * Clicking a horse hides every horse it has nothing to do with, leaving that
 * horse, everything it produced, and whoever it produced them with. On a chart
 * this crowded that is the only way to answer "who did this mare actually
 * breed with" - which is the question a player has after the third foal comes
 * out the same colour. <b>Show everyone</b>, on the right, puts the world back.
 * Nothing moves when the focus changes: the horses keep their places, so the
 * map does not jump under the cursor.
 *
 * <p>The whole population arrives in one {@link PopulationDataPayload} of light
 * entries - a name, a sex, a generation, two parent ids - and is asked for
 * once, on opening, and again on <b>Refresh</b>.
 */
public final class FamilyOverviewScreen extends Screen {

    // The family tree screen's chrome, by value - the two are meant to read as
    // one tool, and this one is reached from it.
    private static final int VIEW_TOP = 30;
    private static final int VIEW_BOTTOM_MARGIN = 30;
    private static final int DIM = 0xD0101014;
    private static final int GUTTER_BG = 0xE0121218;
    private static final int GUTTER_W = 46;
    private static final int HEADING = 0xFFFFFFFF;
    private static final int HINT = 0xFF808088;

    /** A box, at zoom 1. Wide enough for a first name and not much else. */
    private static final int BOX_W = 84;
    private static final int BOX_H = 18;
    private static final int COL_GAP = 12;
    /** The gap under a row, which is where that row's connectors run. */
    private static final int ROW_GAP = 46;

    private static final int BOX_BG = 0xFF23232C;
    private static final int BOX_BG_HOVER = 0xFF3A3A48;
    private static final int BOX_BG_FOCUS = 0xFF4A4436;
    /** Mare and stallion, in the two colours a pedigree chart always uses. */
    private static final int MARE = 0xFFD79CC4;
    private static final int STALLION = 0xFF8FB8E8;
    private static final int NAME = 0xFFF0F0F4;

    /** Below this the names are a smear, so they are not drawn at all. */
    private static final float NAME_ZOOM = 0.55f;
    /** As much of a name as {@link #BOX_W} holds at zoom 1. */
    private static final int NAME_CHARS = 14;
    private static final float MIN_ZOOM = 0.25f;
    private static final float MAX_ZOOM = 2.0f;

    /** A press that moves less than this is a click, not a drag. */
    private static final double DRAG_SLOP = 3.0;

    /** Where Done / Escape goes, or null to close out to the game. */
    private final @Nullable Screen parent;

    /** One horse, placed. {@code x}/{@code y} are map coordinates, not screen ones. */
    private record Node(PopulationDataPayload.Entry entry, int x, int y) {
    }

    private final List<Node> nodes = new ArrayList<>();
    private final Map<UUID, Node> byId = new HashMap<>();
    /** Each drawn row's map y, against the generation number it holds. */
    private final Map<Integer, Integer> rowY = new TreeMap<>();

    /** Which {@link ClientPopulation#version()} {@link #nodes} was built from. */
    private int builtVersion = -1;

    private float panX;
    private float panY;
    private float zoom = 1f;
    private boolean placed = false;

    private boolean dragging = false;
    private double pressX;
    private double pressY;
    private boolean moved = false;

    /** The horse whose family is being looked at, or null for the whole world. */
    private @Nullable UUID focusId;
    /** The focus, its mates and its foals - everything drawn while a focus is set. */
    private final Set<UUID> shown = new HashSet<>();

    private Button showEveryoneButton;

    /** Have we asked the server yet? init() runs again on every resize. */
    private boolean requested = false;

    public FamilyOverviewScreen(@Nullable Screen parent) {
        super(Component.literal("Family overview"));
        this.parent = parent;
    }

    @Override
    public void onClose() {
        if (parent != null) {
            Minecraft.getInstance().setScreen(parent);
            return;
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        int y = this.height - 26;

        addRenderableWidget(Button.builder(Component.literal("Refresh"), b -> fetch())
                .bounds(6, y, 60, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Fit"), b -> {
                    placed = false;
                })
                .bounds(70, y, 40, 20)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(
                        "Put the whole chart back on the screen.")))
                .build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(this.width - 56, y, 50, 20)
                .build());

        // The way out of a focus, on the right, where it is out of the chart's
        // way and in the same place every time.
        int w = Math.max(90, this.font.width("Show everyone") + 12);
        showEveryoneButton = Button.builder(Component.literal("Show everyone"), b -> showEveryone())
                .bounds(this.width - w - 6, VIEW_TOP + 6, w, 20)
                .build();
        showEveryoneButton.visible = focusId != null;
        addRenderableWidget(showEveryoneButton);

        if (!requested) {
            requested = true;
            fetch();
        }
    }

    private void fetch() {
        ClientPacketDistributor.sendToServer(PopulationRequestPayload.INSTANCE);
        builtVersion = -1;
        placed = false;
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    /**
     * Place every horse: one row per generation that has any, name order along
     * the row, each row centred on the map's spine so a row of two and a row of
     * two hundred hang from the same middle.
     */
    private void rebuild() {
        nodes.clear();
        byId.clear();
        rowY.clear();

        Map<Integer, List<PopulationDataPayload.Entry>> rows = new TreeMap<>();
        for (PopulationDataPayload.Entry entry : ClientPopulation.entries()) {
            rows.computeIfAbsent(entry.generation(), k -> new ArrayList<>()).add(entry);
        }

        int pitch = BOX_W + COL_GAP;
        int row = 0;
        for (Map.Entry<Integer, List<PopulationDataPayload.Entry>> generation : rows.entrySet()) {
            List<PopulationDataPayload.Entry> list = generation.getValue();
            list.sort((a, b) -> {
                int byName = a.name().compareToIgnoreCase(b.name());
                // The id breaks a tie, so two horses of the same name do not
                // swap places between one rebuild and the next.
                return byName != 0 ? byName : a.id().compareTo(b.id());
            });
            int y = row * (BOX_H + ROW_GAP);
            rowY.put(generation.getKey(), y);
            int left = -((list.size() - 1) * pitch) / 2;
            for (int i = 0; i < list.size(); i++) {
                Node node = new Node(list.get(i), left + i * pitch, y);
                nodes.add(node);
                byId.put(node.entry().id(), node);
            }
            row++;
        }
        builtVersion = ClientPopulation.version();
    }

    /** Sit the whole chart in the middle of the window, zoomed to fit. */
    private void fit() {
        placed = true;
        panX = this.width / 2f;
        panY = VIEW_TOP + 20;
        zoom = 1f;
        if (nodes.isEmpty()) {
            return;
        }
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = 0;
        for (Node n : nodes) {
            minX = Math.min(minX, n.x());
            maxX = Math.max(maxX, n.x() + BOX_W);
            maxY = Math.max(maxY, n.y() + BOX_H);
        }
        int viewW = this.width - GUTTER_W - 8;
        int viewH = (this.height - VIEW_BOTTOM_MARGIN) - VIEW_TOP - 8;
        float need = Math.min(viewW / (float) Math.max(1, maxX - minX),
                viewH / (float) Math.max(1, maxY));
        zoom = Math.max(MIN_ZOOM, Math.min(1f, need));
        panX = GUTTER_W + 4 + (viewW - (maxX - minX) * zoom) / 2f - minX * zoom;
        panY = VIEW_TOP + 6;
    }

    private int screenX(int mapX) {
        return Math.round(panX + mapX * zoom);
    }

    private int screenY(int mapY) {
        return Math.round(panY + mapY * zoom);
    }

    private int viewBottom() {
        return this.height - VIEW_BOTTOM_MARGIN;
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.y() < VIEW_TOP || event.y() > viewBottom()) {
            return false;
        }
        dragging = true;
        moved = false;
        pressX = event.x();
        pressY = event.y();
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging) {
            if (Math.abs(event.x() - pressX) > DRAG_SLOP || Math.abs(event.y() - pressY) > DRAG_SLOP) {
                moved = true;
            }
            panX += (float) dx;
            panY += (float) dy;
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    /**
     * A press that went nowhere is a click on whatever is under it. Doing this
     * on release rather than on press is what lets the same button both drag
     * the map and pick a horse off it.
     */
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging) {
            dragging = false;
            if (!moved) {
                Node hit = nodeAt(event.x(), event.y());
                if (hit != null) {
                    focus(hit.entry().id());
                } else {
                    showEveryone();
                }
            }
            return true;
        }
        return super.mouseReleased(event);
    }

    /** Zoom about the cursor, so the horse you are pointing at stays put. */
    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (my < VIEW_TOP || my > viewBottom()) {
            return super.mouseScrolled(mx, my, sx, sy);
        }
        float before = zoom;
        float next = zoom * (sy > 0 ? 1.1f : 1 / 1.1f);
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, next));
        if (zoom != before) {
            panX = (float) (mx - (mx - panX) * (zoom / before));
            panY = (float) (my - (my - panY) * (zoom / before));
        }
        return true;
    }

    private @Nullable Node nodeAt(double mx, double my) {
        int w = Math.max(3, Math.round(BOX_W * zoom));
        int h = Math.max(3, Math.round(BOX_H * zoom));
        for (Node n : nodes) {
            if (focusId != null && !shown.contains(n.entry().id())) {
                continue;
            }
            int sx = screenX(n.x());
            int sy = screenY(n.y());
            if (mx >= sx && mx < sx + w && my >= sy && my < sy + h) {
                return n;
            }
        }
        return null;
    }

    /**
     * Show this horse, everything it produced, and whoever it produced them
     * with - and nothing else. A horse with no foals shows alone, which is the
     * true answer and reads as one.
     */
    private void focus(UUID id) {
        focusId = id;
        shown.clear();
        shown.add(id);
        for (Node n : nodes) {
            Optional<UUID> mother = n.entry().mother();
            Optional<UUID> father = n.entry().father();
            boolean mine = mother.filter(id::equals).isPresent() || father.filter(id::equals).isPresent();
            if (!mine) {
                continue;
            }
            shown.add(n.entry().id());
            mother.filter(m -> !m.equals(id)).ifPresent(shown::add);
            father.filter(f -> !f.equals(id)).ifPresent(shown::add);
        }
        if (showEveryoneButton != null) {
            showEveryoneButton.visible = true;
        }
    }

    private void showEveryone() {
        focusId = null;
        shown.clear();
        if (showEveryoneButton != null) {
            showEveryoneButton.visible = false;
        }
    }

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.fill(0, 0, this.width, this.height, DIM);

        if (builtVersion != ClientPopulation.version()) {
            rebuild();
        }
        if (!placed) {
            fit();
        }

        int bottom = viewBottom();
        g.enableScissor(0, VIEW_TOP, this.width, bottom);
        drawLines(g);
        Node hover = nodeAt(mouseX, mouseY);
        for (Node n : nodes) {
            if (focusId == null || shown.contains(n.entry().id())) {
                drawBox(g, n, n == hover);
            }
        }
        g.disableScissor();

        drawGutter(g, bottom);
        drawHeader(g);
    }

    /**
     * One elbow per parent: down out of the parent, across the gap under its
     * row, and down into the child. Axis-aligned because {@code fill} is the
     * only line there is - and it reads as a pedigree chart, which a thicket of
     * diagonals would not. The dam's line is drawn in her colour and the sire's
     * in his, which is what makes a crowded patch of chart legible at all.
     */
    private void drawLines(GuiGraphicsExtractor g) {
        for (Node child : nodes) {
            if (focusId != null && !shown.contains(child.entry().id())) {
                continue;
            }
            drawLink(g, child, child.entry().mother().orElse(null), MARE);
            drawLink(g, child, child.entry().father().orElse(null), STALLION);
        }
    }

    private void drawLink(GuiGraphicsExtractor g, Node child, @Nullable UUID parentId, int colour) {
        if (parentId == null) {
            return;
        }
        if (focusId != null && !shown.contains(parentId)) {
            return;
        }
        Node parent = byId.get(parentId);
        if (parent == null) {
            return; // cut off by the payload cap, or forgotten by the world
        }
        int px = screenX(parent.x() + BOX_W / 2);
        int py = screenY(parent.y() + BOX_H);
        int cx = screenX(child.x() + BOX_W / 2);
        int cy = screenY(child.y());
        if (Math.max(py, cy) < VIEW_TOP || Math.min(py, cy) > viewBottom()) {
            return;
        }
        int mid = (py + cy) / 2;
        vLine(g, px, py, mid, colour);
        hLine(g, px, cx, mid, colour);
        vLine(g, cx, mid, cy, colour);
    }

    private static void hLine(GuiGraphicsExtractor g, int ax, int bx, int y, int colour) {
        g.fill(Math.min(ax, bx), y, Math.max(ax, bx) + 1, y + 1, colour & 0x80FFFFFF);
    }

    private static void vLine(GuiGraphicsExtractor g, int x, int ay, int by, int colour) {
        g.fill(x, Math.min(ay, by), x + 1, Math.max(ay, by) + 1, colour & 0x80FFFFFF);
    }

    private void drawBox(GuiGraphicsExtractor g, Node n, boolean hovered) {
        int w = Math.max(3, Math.round(BOX_W * zoom));
        int h = Math.max(3, Math.round(BOX_H * zoom));
        int x = screenX(n.x());
        int y = screenY(n.y());
        if (x + w < 0 || x > this.width || y + h < VIEW_TOP || y > viewBottom()) {
            return;
        }
        boolean isFocus = n.entry().id().equals(focusId);
        int edge = n.entry().female() ? MARE : STALLION;
        g.fill(x, y, x + w, y + h, isFocus ? BOX_BG_FOCUS : hovered ? BOX_BG_HOVER : BOX_BG);
        g.fill(x, y, x + w, y + 1, edge);
        g.fill(x, y + h - 1, x + w, y + h, edge);
        g.fill(x, y, x + 1, y + h, edge);
        g.fill(x + w - 1, y, x + w, y + h, edge);

        if (zoom >= NAME_ZOOM) {
            var pose = g.pose();
            pose.pushMatrix();
            pose.translate(x + 3, y + Math.round(5 * zoom));
            pose.scale(zoom);
            g.text(this.font, Component.literal(GuiText.clip(n.entry().name(), NAME_CHARS)),
                    0, 0, NAME, false);
            pose.popMatrix();
        }
    }

    /**
     * The generation axis, pinned to the left edge rather than drawn on the map
     * - pan two thousand pixels sideways and you still want to know which row
     * you are reading.
     */
    private void drawGutter(GuiGraphicsExtractor g, int bottom) {
        g.fill(0, VIEW_TOP, GUTTER_W, bottom, GUTTER_BG);
        for (Map.Entry<Integer, Integer> row : rowY.entrySet()) {
            int y = screenY(row.getValue());
            if (y < VIEW_TOP - 4 || y > bottom - 4) {
                continue;
            }
            String label = row.getKey() == 0 ? "founders" : "gen " + row.getKey();
            g.text(this.font, Component.literal(label), 4, y, HINT, false);
        }
        g.fill(GUTTER_W, VIEW_TOP, GUTTER_W + 1, bottom, 0x33FFFFFF);
    }

    private void drawHeader(GuiGraphicsExtractor g) {
        String title;
        String hint;
        if (!ClientPopulation.received()) {
            title = "Family overview";
            hint = "asking the server for every horse this world has bred...";
        } else if (nodes.isEmpty()) {
            title = "Family overview";
            hint = "no horses recorded yet - breed one, or meet one";
        } else if (focusId != null) {
            Node focus = byId.get(focusId);
            int foals = 0;
            for (UUID id : shown) {
                Node n = byId.get(id);
                if (n != null && !id.equals(focusId)
                        && (n.entry().mother().filter(focusId::equals).isPresent()
                        || n.entry().father().filter(focusId::equals).isPresent())) {
                    foals++;
                }
            }
            int mates = Math.max(0, shown.size() - foals - 1);
            title = focus == null ? "Family overview" : focus.entry().name();
            hint = foals == 0
                    ? "no recorded foals - Show everyone, on the right, puts the world back"
                    : foals + (foals == 1 ? " foal" : " foals") + " with "
                            + mates + (mates == 1 ? " other horse" : " other horses");
        } else {
            title = "Family overview";
            hint = nodes.size() + " horses across " + rowY.size()
                    + (rowY.size() == 1 ? " generation" : " generations")
                    + " - drag to move, scroll to zoom, click a horse for its own family"
                    + (ClientPopulation.truncated()
                            ? " (the newest horses are past what one answer can carry)" : "");
        }
        g.text(this.font, Component.literal(title), 8, 8, HEADING, false);
        g.text(this.font, Component.literal(hint), 8, 20, HINT, false);
    }
}
