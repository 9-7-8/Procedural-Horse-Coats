package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.coat.CoatData;
import com.example.horsegenetics.common.genetics.CoatPhenotype;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.ClientConfig;
import com.example.horsegenetics.neoforge.network.FamilyTreeRequestPayload;
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
 * A pedigree chart. The subject is the right-hand column; each column to the
 * left is one generation older, doubling in size, out to great-grandparents
 * (column 3). Within every pair the <b>sire is on top</b> and the <b>dam is
 * on the bottom</b>. Click any known ancestor box to re-centre the tree on
 * that horse (the server is asked for its ancestors). A foundation horse
 * simply shows empty parent boxes.
 *
 * <p>Note: the column index here is chart depth, not the horse's `generation`
 * number (which is on the record and shown on the inventory panel / paper).
 */
public final class FamilyTreeScreen extends Screen {

    private static final int COLUMNS = 4;        // subject + 3 ancestor columns
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

    private record Node(int col, int idx, UUID id, HorseRecord record, int x, int y) {}

    public FamilyTreeScreen(HorseRecord root) {
        super(Component.literal("Family Tree"));
        this.rootId = root.id();
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(this.width / 2 - 50, this.height - 26, 100, 20)
                .build());
        request(rootId);
    }

    private void request(UUID id) {
        this.rootId = id;
        this.scrollY = 0f;
        ClientPacketDistributor.sendToServer(new FamilyTreeRequestPayload(id));
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
            if (n.col == 0) {
                continue;
            }
            Node child = find(n.col - 1, n.idx / 2);
            if (child != null) {
                drawConnector(g, child, n);
            }
        }
        for (Node n : nodes) {
            drawBox(g, n, mouseX, mouseY);
        }
        g.disableScissor();

        drawScrollbar(g, viewBottom);

        g.text(this.font, this.title, 8, 8, 0xFFFFFFFF);
        g.text(this.font, Component.literal("sire on top, dam below - click an ancestor to re-centre"),
                8, 20, 0xFF808088);
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
            if (n.col > 0 && n.record != null
                    && mx >= n.x && mx <= n.x + boxW && my >= n.y && my <= n.y + boxH) {
                request(n.id);
                return true;
            }
        }
        return false;
    }

    // --- layout ---

    private void rebuildNodes() {
        nodes.clear();
        this.useScrollbar = ClientConfig.familyTreeScrollBar();
        int viewH = Math.max(40, (this.height - VIEW_BOTTOM_MARGIN) - VIEW_TOP);

        // Horizontal: 4 columns must always fit side by side.
        int rightEdge = this.width - (useScrollbar ? 10 : 4);
        int colStep = Math.max(MIN_BOX_W + COL_GAP, (rightEdge - LEFT_MARGIN) / COLUMNS);
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

        for (int col = 0; col < COLUMNS; col++) {
            int slots = 1 << col;
            int colX = rightEdge - this.boxW - col * colStep;
            for (int i = 0; i < slots; i++) {
                UUID id = ancestorId(col, i);
                HorseRecord rec = id == null ? null : ClientHorseRecordCache.byId(id);
                int centerY = originY + (int) ((i + 0.5) * contentH / slots);
                nodes.add(new Node(col, i, id, rec, colX, centerY - this.boxH / 2));
            }
        }
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
            CoatPhenotype phenotype = Genotype.parse(r.geneticCode()).phenotype();
            return phenotype == CoatPhenotype.BAY ? CoatData.bay(0.5f) : CoatData.solid(phenotype);
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
