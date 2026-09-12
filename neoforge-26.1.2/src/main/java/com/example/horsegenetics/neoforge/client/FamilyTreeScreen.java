package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.coat.CoatData;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.ClientConfig;
import com.example.horsegenetics.neoforge.network.FamilyTreeRequestPayload;
import com.example.horsegenetics.neoforge.network.OffspringDataPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.Horse;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * <b>A family chart that runs both ways.</b> Three modes, switched by the tabs
 * along the bottom or the left / right arrow keys:
 *
 * <ul>
 *   <li><b>Ancestors</b> - the pedigree. The subject is the right-hand column
 *       and each column to the left is one generation older, doubling in size,
 *       out to great-grandparents. Within every pair the <b>sire is on top</b>
 *       and the <b>dam is on the bottom</b>. A foundation horse simply shows
 *       empty parent boxes.</li>
 *   <li><b>Descendants</b> - the same idea pointed the other way: the subject on
 *       the left, then a column per generation of foals. Unlike the pedigree
 *       this is <b>ragged</b> - a generation has as many horses as it has - and
 *       it is capped by the server, so a very prolific line says so rather than
 *       showing part of a family as the whole of it.</li>
 *   <li><b>Overview</b> - both at once, the subject in the middle with two
 *       columns of forebears to its left and two generations of foals to its
 *       right.</li>
 * </ul>
 *
 * <p>Click any horse to re-centre on it. That used to be a <b>one-way trip</b>:
 * the chart only went up, and re-rooting lost the horse you opened it on with
 * no way back. So there is a history - <b>Back</b> or backspace - and a
 * <b>Home</b> that always returns to the horse the screen was opened on, which
 * is the thing a player loses first when every click re-roots the chart.
 *
 * <p>Note: the column index here is chart depth, not the horse's `generation`
 * number (which is on the record and shown on the inventory panel / paper).
 */
public final class FamilyTreeScreen extends Screen {

    private static final int COLUMNS = 4;        // subject + 3 ancestor columns
    /** Overview: grandparents, parents, subject, foals, grand-foals. */
    private static final int OVERVIEW_COLUMNS = 5;
    // "Natural" (maximum) box size; shrunk per-window in rebuildNodes().
    private static final int BOX_W = 132;
    private static final int BOX_H = 34;
    private static final int MIN_BOX_W = 54;
    private static final int MIN_BOX_H = 15;
    private static final int SWATCH = 28;        // fallback flat coat icon
    private static final int MODEL_VIEW_W = 50;
    private static final int MODEL_VIEW_H = 78;
    private static final int MODEL_LIFT = 18;       // keep the model (esp. its feet) inside the row slot
    private static final float MODEL_SCALE = 16.0F; // eyeball - horse is a big model
    private static final int LINE = 0xFF5A5A66;

    private static final int VIEW_TOP = 30;
    private static final int VIEW_BOTTOM_MARGIN = 30;   // clear of the Done button
    private static final int LEFT_MARGIN = 6;
    private static final int COL_GAP = 8;               // horizontal gap between columns (for the elbow)
    private static final int ROW_GAP = 3;               // vertical gap between rows
    private static final int ROW_SPACING = BOX_H + 6;   // natural row pitch (scroll mode)
    private static final int HEAD_ROOM = 16;            // space above row 1 for the model head

    private UUID rootId;
    /** Where Done / Escape goes, or null to close out to the game. */
    private final @org.jetbrains.annotations.Nullable Screen parent;
    private final List<Node> nodes = new ArrayList<>();
    /** One reusable client-only horse per record, so each box can draw a live 3D model. */
    private final Map<UUID, Horse> modelHorses = new HashMap<>();

    private float scrollY = 0f;
    private float maxScroll = 0f;

    // Per-window layout, recomputed each rebuildNodes().
    private boolean useScrollbar = false;
    private int boxW = BOX_W;
    private int boxH = BOX_H;
    private float uiScale = 1f;   // text + model shrink factor

    /**
     * <b>Which way the chart runs.</b> Ancestors was the only direction for a
     * long time, and re-centring on a forebear was a one-way trip: you could
     * walk up the tree and had no way back down it, and no way back to the
     * horse you opened the screen on.
     */
    private enum Mode {
        ANCESTORS("Ancestors", "sire on top, dam below - click a horse to re-centre"),
        DESCENDANTS("Descendants", "each column is one generation of foals - click a horse to re-centre"),
        OVERVIEW("Overview", "grandparents, parents, this horse, then its foals - click any to re-centre");

        final String label;
        final String hint;

        Mode(String label, String hint) {
            this.label = label;
            this.hint = hint;
        }
    }

    private Mode mode = Mode.ANCESTORS;

    /**
     * The horse the screen was opened on. Never changes, so <b>Home</b> always
     * means the same thing however far you have wandered - which is the thing a
     * player loses first when every click re-roots the chart.
     */
    private final UUID originalId;

    /** Re-centre history, most recent last. <b>Back</b> pops it. */
    private final java.util.ArrayDeque<UUID> history = new java.util.ArrayDeque<>();

    private Button backButton;
    private Button homeButton;

    /** Have we asked the server yet? init() runs again on every resize. */
    private boolean requested = false;

    /**
     * One box. {@code linkCol}/{@code linkIdx} name the node this one connects
     * to on the side <b>nearer the subject</b>, or {@code -1} for none - which
     * is what lets ancestors (a binary tree growing left) and descendants (a
     * ragged tree growing right) share one connector routine.
     */
    private record Node(int col, int idx, UUID id, HorseRecord record, int x, int y,
                        int linkCol, int linkIdx) {
        Node(int col, int idx, UUID id, HorseRecord record, int x, int y) {
            this(col, idx, id, record, x, y, -1, -1);
        }
    }

    public FamilyTreeScreen(HorseRecord root) {
        this(root, null);
    }

    /**
     * @param parent the screen to go back to on Done / Escape, or {@code null}
     *               to close out to the game. The horse information screen's
     *               Family tree tab passes itself, so the tree reads as one of
     *               its pages rather than as a place you end up.
     */
    public FamilyTreeScreen(HorseRecord root, @org.jetbrains.annotations.Nullable Screen parent) {
        super(Component.literal("Family Tree"));
        this.rootId = root.id();
        this.originalId = root.id();
        this.parent = parent;
    }

    @Override
    public void onClose() {
        if (parent != null) {
            net.minecraft.client.Minecraft.getInstance().setScreen(parent);
            return;
        }
        super.onClose();
    }

    @Override
    protected void init() {
        super.init();

        // Mode tabs, left to right along the bottom, then Back / Home / Done.
        int y = this.height - 26;
        int x = 6;
        for (Mode m : Mode.values()) {
            int w = Math.max(52, this.font.width(m.label) + 12);
            Button b = Button.builder(Component.literal(m.label), btn -> {
                        this.mode = m;
                        this.scrollY = 0f;
                        rebuildButtons();
                    })
                    .bounds(x, y, w, 20)
                    .build();
            b.active = this.mode != m;
            addRenderableWidget(b);
            x += w + 2;
        }

        backButton = Button.builder(Component.literal("< Back"), b -> goBack())
                .bounds(x + 8, y, 52, 20)
                .build();
        backButton.active = !history.isEmpty();
        addRenderableWidget(backButton);

        homeButton = Button.builder(Component.literal("Home"), b -> goHome())
                .bounds(x + 62, y, 46, 20)
                .build();
        homeButton.active = !rootId.equals(originalId);
        addRenderableWidget(homeButton);

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(this.width - 56, y, 50, 20)
                .build());

        // Only on the way in. init() also runs on every window resize, and
        // rebuildButtons() calls it - neither is a reason to ask the server for
        // the same family again.
        if (!requested) {
            requested = true;
            fetch(rootId);
        }
    }

    /**
     * Re-lay the buttons so their enabled / disabled states follow the current
     * position: which mode tab is the active one, whether there is anywhere to
     * go back to, and whether Home would do anything.
     */
    private void rebuildButtons() {
        clearWidgets();
        init();
    }

    /**
     * Re-centre on {@code id}, remembering where we were so <b>Back</b> can
     * undo it. The subject itself is not pushed - clicking the horse you are
     * already looking at should not fill the history with itself.
     */
    private void request(UUID id) {
        if (id.equals(rootId)) {
            return;
        }
        history.push(rootId);
        fetch(id);
        rebuildButtons();
    }

    private void goBack() {
        if (history.isEmpty()) {
            return;
        }
        fetch(history.pop());
        rebuildButtons();
    }

    private void goHome() {
        if (rootId.equals(originalId)) {
            return;
        }
        history.push(rootId);
        fetch(originalId);
        rebuildButtons();
    }

    /**
     * Ask the server for everything this root needs, without touching history.
     * <b>Both</b> directions are requested every time rather than only the one
     * the current mode draws: the answers arrive asynchronously, so fetching
     * lazily on a mode switch would show an empty chart for a beat every time
     * you pressed a tab.
     */
    private void fetch(UUID id) {
        this.rootId = id;
        this.scrollY = 0f;
        ClientPacketDistributor.sendToServer(new FamilyTreeRequestPayload(id));
        ClientPacketDistributor.sendToServer(
                new com.example.horsegenetics.neoforge.network.OffspringRequestPayload(id));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.fill(0, 0, this.width, this.height, 0xD0101014);

        rebuildNodes();

        int viewBottom = this.height - VIEW_BOTTOM_MARGIN;
        g.enableScissor(0, VIEW_TOP, this.width, viewBottom);
        for (Node n : nodes) {
            if (n.linkCol == -1) {
                continue;
            }
            Node other = find(n.linkCol, n.linkIdx);
            if (other != null) {
                drawConnector(g, other, n);
            }
        }
        for (Node n : nodes) {
            drawBox(g, n, mouseX, mouseY);
        }
        g.disableScissor();

        drawScrollbar(g, viewBottom);

        HorseRecord subject = ClientHorseRecordCache.byId(rootId);
        String name = subject == null ? "Family Tree" : subject.displayName();
        g.text(this.font, Component.literal(name), 8, 8, 0xFFFFFFFF);

        String hint = mode.hint;
        if (mode != Mode.ANCESTORS && anyGenerationTruncated()) {
            hint = hint + " - some generations are too large to show in full";
        }
        g.text(this.font, Component.literal(hint), 8, 20, 0xFF808088);
    }

    /** Did the server have to cut any generation of foals it sent us? */
    private boolean anyGenerationTruncated() {
        for (OffspringDataPayload.Generation gen : ClientOffspring.of(rootId)) {
            if (gen.truncated()) {
                return true;
            }
        }
        return false;
    }

    private void drawScrollbar(GuiGraphicsExtractor g, int viewBottom) {
        if (maxScroll <= 0f) {
            return;
        }
        int viewH = viewBottom - VIEW_TOP;
        float contentH = viewH + maxScroll;
        int x1 = this.width - 2;
        int x0 = x1 - 3;
        g.fill(x0, VIEW_TOP, x1, viewBottom, 0x33FFFFFF);
        int thumbH = Math.max(20, (int) (viewH * viewH / contentH));
        int thumbY = VIEW_TOP + Math.round(scrollY * (viewH - thumbH) / maxScroll);
        g.fill(x0, thumbY, x1, thumbY + thumbH, 0xAAFFFFFF);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollDy) {
        if (maxScroll > 0f) {
            scrollY = Math.max(0f, Math.min(maxScroll, scrollY - (float) scrollDy * 22f));
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollDy);
    }

    /** Three-segment elbow from the child's right edge to this node's left edge. */
    private void drawConnector(GuiGraphicsExtractor g, Node child, Node parent) {
        int x1 = child.x + boxW;
        int x2 = parent.x;
        int y1 = child.y + boxH / 2;
        int y2 = parent.y + boxH / 2;
        int midX = (x1 + x2) / 2;
        hLine(g, x1, midX, y1);
        vLine(g, midX, y1, y2);
        hLine(g, midX, x2, y2);
    }

    private static void hLine(GuiGraphicsExtractor g, int ax, int bx, int y) {
        g.fill(Math.min(ax, bx), y, Math.max(ax, bx) + 1, y + 1, LINE);
    }

    private static void vLine(GuiGraphicsExtractor g, int x, int ay, int by) {
        g.fill(x, Math.min(ay, by), x + 1, Math.max(ay, by) + 1, LINE);
    }

    private void drawBox(GuiGraphicsExtractor g, Node n, int mouseX, int mouseY) {
        boolean present = n.record != null;
        boolean hovered = present && n.col > 0
                && mouseX >= n.x && mouseX <= n.x + boxW && mouseY >= n.y && mouseY <= n.y + boxH;

        int bg = present ? (hovered ? 0xFF3A3A48 : 0xFF26262E) : 0xFF1A1A1E;
        int border = present ? (hovered ? 0xFFFFFFFF : 0xFF505060) : 0xFF303038;
        g.fill(n.x, n.y, n.x + boxW, n.y + boxH, bg);
        g.fill(n.x, n.y, n.x + boxW, n.y + 1, border);
        g.fill(n.x, n.y + boxH - 1, n.x + boxW, n.y + boxH, border);
        g.fill(n.x, n.y, n.x + 1, n.y + boxH, border);
        g.fill(n.x + boxW - 1, n.y, n.x + boxW, n.y + boxH, border);

        if (present) {
            HorseRecord r = n.record;
            // model first so the text sits on top of it
            drawHorseModel(g, r, n.x, n.y, mouseX, mouseY);
            int textMaxW = Math.max(20, (boxW - SWATCH) - 8);
            int line2 = n.y + 3 + Math.round(11f * uiScale);
            drawFitted(g, r.displayName(), n.x + 4, n.y + 3, textMaxW, 0xFFF0F0F0);
            String by = r.attribution().map(a -> "by " + a).orElse("wild");
            drawFitted(g, by, n.x + 4, line2, textMaxW, 0xFF8088A8);
        } else {
            g.text(this.font, Component.literal(n.col == 0 ? "?" : "—"), n.x + 4, n.y + boxH / 2 - 4, 0xFF606068);
        }
    }

    /** Draw {@code text} left-aligned at {@code (x, y)}, scaled down (never up) so the WHOLE string fits {@code maxW}. */
    private void drawFitted(GuiGraphicsExtractor g, String text, int x, int y, int maxW, int color) {
        float w = this.font.width(text);
        float scale = w > 0 ? Math.min(uiScale, maxW / w) : uiScale;
        if (scale >= 0.999f) {
            g.text(this.font, Component.literal(text), x, y, color);
            return;
        }
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale);
        g.text(this.font, Component.literal(text), 0, 0, color);
        pose.popMatrix();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        double mx = event.x();
        double my = event.y();
        if (my < VIEW_TOP || my > this.height - VIEW_BOTTOM_MARGIN) {
            return false; // outside the (clipped) chart area
        }
        for (Node n : nodes) {
            // Any horse but the one already centred - which now includes a
            // foal, so the chart walks down as well as up.
            if (n.record != null && !n.id.equals(rootId)
                    && mx >= n.x && mx <= n.x + boxW && my >= n.y && my <= n.y + boxH) {
                request(n.id);
                return true;
            }
        }
        return false;
    }

    /**
     * Backspace goes back and Home goes home, so walking the tree does not
     * need the mouse to come back off it. Left / right step between
     * <b>modes</b> rather than between boxes: the three of them are ordered
     * oldest-first, so right really is "look further down the family".
     */
    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        switch (event.key()) {
            case org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE -> {
                goBack();
                return true;
            }
            case org.lwjgl.glfw.GLFW.GLFW_KEY_HOME -> {
                goHome();
                return true;
            }
            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT -> {
                int step = event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT ? -1 : 1;
                Mode[] all = Mode.values();
                int next = Math.floorMod(mode.ordinal() + step, all.length);
                this.mode = all[next];
                this.scrollY = 0f;
                rebuildButtons();
                return true;
            }
            default -> {
                return super.keyPressed(event);
            }
        }
    }

    // --- layout ---

    private void rebuildNodes() {
        nodes.clear();
        this.useScrollbar = ClientConfig.familyTreeScrollBar();
        int viewH = Math.max(40, (this.height - VIEW_BOTTOM_MARGIN) - VIEW_TOP);

        // Horizontal: every column of the CURRENT mode must fit side by side.
        // Overview is the wide one - two generations of forebears, the subject,
        // and two of foals - so sizing for COLUMNS would run it off the window.
        int columns = mode == Mode.OVERVIEW ? OVERVIEW_COLUMNS : COLUMNS;
        int rightEdge = this.width - (useScrollbar ? 10 : 4);
        int colStep = Math.max(MIN_BOX_W + COL_GAP, (rightEdge - LEFT_MARGIN) / columns);
        this.boxW = Math.max(MIN_BOX_W, Math.min(BOX_W, colStep - COL_GAP));

        // Vertical: either shrink 8 rows to fit (default) or keep pitch and scroll.
        int rowSpacing = useScrollbar
                ? ROW_SPACING
                : Math.max(MIN_BOX_H + ROW_GAP, Math.min(ROW_SPACING, (viewH - HEAD_ROOM) / 8));
        this.boxH = Math.max(MIN_BOX_H, Math.min(BOX_H, rowSpacing - ROW_GAP));
        this.uiScale = Math.max(0.35f, Math.min(1f, this.boxH / (float) BOX_H));

        int contentH = rowSpacing * 8;
        maxScroll = useScrollbar ? Math.max(0f, (HEAD_ROOM + contentH) - viewH) : 0f;
        scrollY = Math.max(0f, Math.min(scrollY, maxScroll));
        int originY = VIEW_TOP + HEAD_ROOM - Math.round(scrollY);

        switch (mode) {
            case ANCESTORS -> layoutAncestors(0, COLUMNS, rightEdge, colStep, originY, contentH);
            case DESCENDANTS -> layoutDescendants(0, COLUMNS, LEFT_MARGIN, colStep, originY, contentH);
            case OVERVIEW -> {
                // Subject in the middle: two columns of forebears to its left,
                // two generations of foals to its right.
                int subjectX = LEFT_MARGIN + 2 * colStep;
                layoutAncestors(0, 3, subjectX + this.boxW, colStep, originY, contentH);
                layoutDescendants(1, 3, subjectX, colStep, originY, contentH);
            }
        }
    }

    /**
     * The pedigree half: {@code columns} columns marching left from
     * {@code rightEdge}, each twice as tall as the last. Column 0 is the
     * subject, and {@code firstCol} lets the overview skip drawing it twice.
     */
    private void layoutAncestors(int firstCol, int columns, int rightEdge, int colStep,
                                 int originY, int contentH) {
        for (int col = firstCol; col < columns; col++) {
            int slots = 1 << col;
            int colX = rightEdge - this.boxW - col * colStep;
            for (int i = 0; i < slots; i++) {
                UUID id = ancestorId(col, i);
                HorseRecord rec = id == null ? null : ClientHorseRecordCache.byId(id);
                int centerY = originY + (int) ((i + 0.5) * contentH / slots);
                // Links toward the subject: this node's child is one column right.
                nodes.add(new Node(col, i, id, rec, colX, centerY - this.boxH / 2,
                        col == 0 ? -1 : col - 1, i / 2));
            }
        }
    }

    /**
     * The foals half. Unlike the pedigree this is <b>ragged</b> - a generation
     * has as many horses as it has, not a power of two - so each column is
     * spread evenly over the same height and columns are negative so they
     * cannot collide with an ancestor's.
     *
     * <p>A generation the server had to cut ({@link OffspringDataPayload
     * .Generation#truncated()}) is drawn as far as it goes; saying so is
     * {@link #extractRenderState}'s job, because "and more" belongs beside the
     * hint rather than inside the chart.
     */
    private void layoutDescendants(int firstGen, int generations, int leftEdge, int colStep,
                                   int originY, int contentH) {
        List<OffspringDataPayload.Generation> gens = ClientOffspring.of(rootId);
        if (firstGen == 0) {
            HorseRecord subject = ClientHorseRecordCache.byId(rootId);
            nodes.add(new Node(0, 0, rootId, subject, leftEdge,
                    originY + contentH / 2 - this.boxH / 2));
        }
        for (int g = 0; g < generations - 1 && g < gens.size(); g++) {
            List<HorseRecord> horses = gens.get(g).horses();
            if (horses.isEmpty()) {
                break;
            }
            int col = -(g + 1);
            int colX = leftEdge + (g + 1) * colStep;
            for (int i = 0; i < horses.size(); i++) {
                HorseRecord rec = horses.get(i);
                int centerY = originY + (int) ((i + 0.5) * contentH / horses.size());
                // Link back to the parent one generation nearer the subject,
                // when that horse is actually on the chart; else to the subject.
                int linkCol = g == 0 ? 0 : -g;
                int linkIdx = g == 0 ? 0 : parentIndexIn(gens.get(g - 1), rec);
                nodes.add(new Node(col, i, rec.id(), rec, colX, centerY - this.boxH / 2,
                        linkIdx < 0 ? -1 : linkCol, Math.max(0, linkIdx)));
            }
        }
    }

    /** Which horse of {@code previous} is {@code foal}'s parent, or -1 if neither is. */
    private static int parentIndexIn(OffspringDataPayload.Generation previous, HorseRecord foal) {
        List<HorseRecord> horses = previous.horses();
        for (int i = 0; i < horses.size(); i++) {
            UUID id = horses.get(i).id();
            if (foal.fatherId().filter(id::equals).isPresent()
                    || foal.motherId().filter(id::equals).isPresent()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Walk {@code col} steps up from the root. Each bit of {@code i}, most
     * significant first, chooses the parent: <b>0 = sire</b> (drawn on top),
     * <b>1 = dam</b> (drawn on the bottom).
     */
    private UUID ancestorId(int col, int i) {
        UUID cur = rootId;
        for (int bit = col - 1; bit >= 0; bit--) {
            HorseRecord rec = ClientHorseRecordCache.byId(cur);
            if (rec == null) {
                return null;
            }
            Optional<UUID> next = ((i >> bit) & 1) == 0 ? rec.fatherId() : rec.motherId();
            if (next.isEmpty()) {
                return null;
            }
            cur = next.get();
        }
        return cur;
    }

    private Node find(int col, int idx) {
        for (Node n : nodes) {
            if (n.col == col && n.idx == idx) {
                return n;
            }
        }
        return null;
    }

    /**
     * A live 3D horse model in this record's coat, for the node whose box starts
     * at {@code (boxX, boxY)}. Uses a throwaway client-only {@link Horse} (never
     * added to the world) whose render state we build directly and hand our coat
     * before submitting - so nothing touches {@link ClientCoatCache}. The
     * viewport is {@value #MODEL_VIEW_W}x{@value #MODEL_VIEW_H} and reaches above
     * the node box so the head isn't clipped; the model turns to face the
     * cursor. Falls back to the flat coat swatch if the client level or
     * renderer isn't ready.
     */
    private void drawHorseModel(GuiGraphicsExtractor g, HorseRecord r, int boxX, int boxY, int mouseX, int mouseY) {
        int sw = Math.round(SWATCH * uiScale);
        int swatchX = boxX + boxW - sw - 3;
        int swatchY = boxY + (boxH - sw) / 2;
        Horse horse = modelHorse(r);
        CoatData coat = coatFor(r);
        if (horse == null || coat == null) {
            drawCoatSwatchScaled(g, coat, swatchX, swatchY, sw);
            return;
        }
        float mScale = MODEL_SCALE * uiScale;
        int viewW = Math.round(MODEL_VIEW_W * uiScale);
        int viewH = Math.round(MODEL_VIEW_H * uiScale);
        // viewport centred so the feet sit near the box floor (minus MODEL_LIFT)
        // and the head overflows upward (drawn over the row above)
        int cx = boxX + boxW - 3 - viewW / 2;
        int cy = boxY + boxH - 4 - Math.round(MODEL_LIFT * uiScale);
        try {
            EntityRenderer<? super Horse, ?> renderer =
                    Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(horse);
            EntityRenderState state = renderer.createRenderState(horse, 1.0F);
            state.shadowPieces.clear();
            state.outlineColor = 0;
            if (state instanceof GeneticHorseRenderState gs) {
                gs.coatData = coat;
            }
            // turn toward the cursor (same idea as the vanilla inventory model,
            // just a stronger swing so it clearly "looks at" the pointer)
            float xAngle = (float) Math.atan((cx - mouseX) / 30.0F);
            float yAngle = (float) Math.atan((cy - mouseY) / 30.0F);
            if (state instanceof LivingEntityRenderState ls) {
                ls.bodyRot = 180.0F + xAngle * 42.0F;
                ls.yRot = xAngle * 42.0F;
                ls.xRot = -yAngle * 22.0F;
                ls.boundingBoxWidth = ls.boundingBoxWidth / ls.scale;
                ls.boundingBoxHeight = ls.boundingBoxHeight / ls.scale;
                ls.scale = 1.0F;
            }
            Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
            Quaternionf xRotation = new Quaternionf().rotateX(yAngle * 22.0F * ((float) Math.PI / 180.0F));
            rotation.mul(xRotation);
            Vector3f translation = new Vector3f(0.0F, state.boundingBoxHeight / 2.0F + 0.0625F, 0.0F);
            g.entity(state, mScale, translation, rotation, xRotation,
                    cx - viewW / 2, cy - viewH / 2, cx + viewW / 2, cy + viewH / 2);
        } catch (RuntimeException ignored) {
            drawCoatSwatchScaled(g, coat, swatchX, swatchY, sw);
        }
    }

    private Horse modelHorse(HorseRecord r) {
        Horse existing = modelHorses.get(r.id());
        if (existing != null) {
            return existing;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        try {
            Horse h = EntityType.HORSE.create(mc.level, EntitySpawnReason.LOAD);
            if (h != null) {
                h.setBaby(false);
                modelHorses.put(r.id(), h);
            }
            return h;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static CoatData coatFor(HorseRecord r) {
        try {
            // The record carries the epigenome, so a dead or distant ancestor
            // draws its *real* coat. There is deliberately no stand-in for a
            // record without one: inventing a plausible epigenome from the UUID
            // drew a horse that never existed, which is worse than a blank in a
            // pedigree - the whole job of this screen is to show what was there.
            return r.hasGenome() ? new CoatData(r.genome()) : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Flat-texture fallback for {@link #drawHorseModel}. */
    private void drawCoatSwatchScaled(GuiGraphicsExtractor g, CoatData coat, int x, int y, int size) {
        if (coat == null) {
            return;
        }
        Identifier texture = GeneticHorseRenderer.coatTextureFor(coat, false);
        g.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFF000000);
        g.blit(texture, x, y, x + size, y + size, 0.0f, 1.0f, 0.0f, 1.0f);
    }

    @Override
    public void removed() {
        modelHorses.clear();
        super.removed();
    }
}
