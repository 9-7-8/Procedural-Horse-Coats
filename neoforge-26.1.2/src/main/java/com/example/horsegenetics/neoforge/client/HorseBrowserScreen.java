package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.neoforge.menu.HorseBrowserMenu;
import com.example.horsegenetics.neoforge.network.SelectBrowserGenePayload;
import com.example.horsegenetics.neoforge.network.WriteResearchPaperPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The <b>Horse Browser</b> - a tabbed reference/crafting window, opened with the
 * browser key (default <kbd>H</kbd>). It is an {@link AbstractContainerScreen}
 * over {@link HorseBrowserMenu} so its Crafting tab can carry a live 3x3 grid
 * and show the player inventory.
 *
 * <h2>Gene Database tab</h2>
 * Every registered gene, filterable, with full detail for a gene the player has
 * met (creative sees all) and a nudge for one they have not. A
 * <b>Write research paper</b> button spends one book for a paper on the selected
 * discovered gene.
 *
 * <h2>Crafting tab</h2>
 * A private 3x3 grid that only makes this mod's outputs (see
 * {@code HorseBrowserRecipes}): drop a book in and pick a discovered gene on the
 * left to craft its <b>research paper</b>, then combine that paper with a golden
 * carrot, hair, the rarity item and flavours into a <b>Known Gene Splice
 * carrot</b>; two or more carrots combine into one.
 *
 * <p>The player inventory is shown on both tabs. On the Gene Database tab the
 * grid + result slots go inactive ({@link HorseBrowserMenu#setCraftingVisible})
 * so they neither render nor take clicks.
 */
public final class HorseBrowserScreen extends AbstractContainerScreen<HorseBrowserMenu> {

    private enum Tab {
        GENE_DATABASE("Gene DB"),
        CRAFTING("Crafting");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    // --- palette (matches the old flat look) ---
    private static final int PANEL = 0xF00E0E12;
    private static final int BORDER = 0xFF3A3A48;
    private static final int LIST_BG = 0xFF141419;
    private static final int SLOT_BG = 0xFF262630;
    private static final int SLOT_RESULT_BG = 0xFF3A3320;
    private static final int TAB_ON = 0xFF2A2A38;
    private static final int TAB_OFF = 0xFF17171E;
    private static final int TAB_TEXT_ON = 0xFFFFFFFF;
    private static final int TAB_TEXT_OFF = 0xFF9098A8;
    private static final int LABEL = 0xFF8890A8;
    private static final int ROW_HOVER = 0x22FFFFFF;
    private static final int ROW_SEL = 0x3355A0E0;
    private static final int NAME = 0xFFDDE2EC;
    private static final int NAME_DIM = 0xFF9AA0B0;
    private static final int HEADING = 0xFFF0F0F0;
    private static final int EXPR_ON = 0xFF9BE08A;
    private static final int EXPR_OFF = 0xFF6E7686;
    private static final int DESC = 0xFFB2B8C6;
    private static final int TAG = 0xFF7C84A0;
    private static final int ALLELE_TOK = 0xFFE0C070;

    // --- geometry, LOCAL to leftPos / topPos ---
    private static final int IMG_W = 316;
    private static final int IMG_H = 244;
    private static final int TAB_H = 15;
    private static final int LIST_X = 8;
    private static final int LIST_W = 116;
    private static final int LIST_TOP = 40;
    private static final int LIST_BOTTOM = 150;
    private static final int ROW_H = 13;
    private static final int DETAIL_X = 130;
    private static final int DETAIL_TOP = 21;
    private static final int DETAIL_BOTTOM = 150;
    private static final int SEARCH_Y = 21;
    private static final int SEARCH_H = 14;

    private Tab tab = Tab.GENE_DATABASE;
    private String search = "";
    private String selectedKey = "";
    private int listScroll = 0;
    private float detailScroll = 0f;
    private float detailMaxScroll = 0f;

    private EditBox searchBox;
    private Button writeButton;
    private final List<Gene> allGenes;
    private List<Gene> filtered = List.of();

    public HorseBrowserScreen(HorseBrowserMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, IMG_W, IMG_H);
        List<Gene> genes = new ArrayList<>(Genes.codeOrder());
        genes.sort(Comparator.comparing(Gene::name, String.CASE_INSENSITIVE_ORDER));
        this.allGenes = List.copyOf(genes);
        if (!allGenes.isEmpty()) {
            this.selectedKey = allGenes.get(0).key();
        }
    }

    private boolean creative() {
        return minecraft != null && minecraft.player != null && minecraft.player.getAbilities().instabuild;
    }

    private boolean discovered(Gene g) {
        return g != null && (creative() || ClientGeneDatabase.knows(g.key()));
    }

    /** Genes eligible for the Crafting tab list: carrot-bearing and discovered (creative: all). */
    private boolean craftable(Gene g) {
        return g.hasGeneCarrot() && discovered(g);
    }

    // --- widgets ---

    @Override
    protected void init() {
        super.init();
        this.titleLabelY = -100; // we draw our own title in the tab strip
        this.inventoryLabelX = LIST_X;
        this.inventoryLabelY = IMG_H - 84;

        searchBox = new EditBox(this.font, leftPos + LIST_X, topPos + SEARCH_Y, LIST_W, SEARCH_H,
                Component.literal("Filter"));
        searchBox.setMaxLength(48);
        searchBox.setHint(Component.literal("filter genes"));
        searchBox.setValue(search);
        addRenderableWidget(searchBox);

        int bw = IMG_W - DETAIL_X - 10;
        writeButton = Button.builder(Component.translatable("gui.horsegenetics.write_paper"), b -> writePaper())
                .bounds(leftPos + DETAIL_X, topPos + DETAIL_BOTTOM - 18, bw, 16)
                .build();
        writeButton.visible = false;
        addRenderableWidget(writeButton);

        applyFilter();
    }

    private void writePaper() {
        Gene g = selected();
        if (g != null && ClientGeneDatabase.knows(g.key())) {
            ClientPacketDistributor.sendToServer(new WriteResearchPaperPayload(g.key()));
        }
    }

    private void applyFilter() {
        String q = search.trim().toLowerCase(Locale.ROOT);
        List<Gene> out = new ArrayList<>();
        for (Gene g : allGenes) {
            if (tab == Tab.CRAFTING && !craftable(g)) {
                continue;
            }
            if (q.isEmpty() || matches(g, q)) {
                out.add(g);
            }
        }
        filtered = out;
        listScroll = Math.max(0, Math.min(listScroll, maxListScroll()));
        boolean stillThere = filtered.stream().anyMatch(g -> g.key().equals(selectedKey));
        if (!stillThere && !filtered.isEmpty()) {
            select(filtered.get(0).key());
        }
    }

    private static boolean matches(Gene g, String q) {
        if (g.name().toLowerCase(Locale.ROOT).contains(q) || g.key().toLowerCase(Locale.ROOT).contains(q)) {
            return true;
        }
        for (Allele a : g.alleles()) {
            if (a.token().toLowerCase(Locale.ROOT).contains(q) || a.label().toLowerCase(Locale.ROOT).contains(q)) {
                return true;
            }
        }
        return false;
    }

    private Gene selected() {
        for (Gene g : allGenes) {
            if (g.key().equals(selectedKey)) {
                return g;
            }
        }
        return null;
    }

    private void select(String key) {
        if (!key.equals(selectedKey)) {
            selectedKey = key;
            detailScroll = 0f;
        }
        // keep the server's gene selection in sync so the book -> paper craft works
        ClientPacketDistributor.sendToServer(new SelectBrowserGenePayload(key));
    }

    private int visibleRows() {
        return Math.max(1, (LIST_BOTTOM - LIST_TOP) / ROW_H);
    }

    private int maxListScroll() {
        return Math.max(0, filtered.size() - visibleRows());
    }

    // --- input ---

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();

        Tab hit = tabAt(mx, my);
        if (hit != null) {
            if (hit != tab && menu.getCarried().isEmpty()) {
                tab = hit;
                applyFilter();
            }
            return true;
        }
        int row = rowAt(mx, my);
        if (row >= 0) {
            select(filtered.get(row).key());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private Tab tabAt(double mx, double my) {
        if (my < topPos || my > topPos + TAB_H) {
            return null;
        }
        int tx = leftPos;
        for (Tab t : Tab.values()) {
            int w = this.font.width(t.label) + 16;
            if (mx >= tx && mx <= tx + w) {
                return t;
            }
            tx += w + 2;
        }
        return null;
    }

    private int rowAt(double mx, double my) {
        double lx = mx - leftPos;
        double ly = my - topPos;
        if (lx < LIST_X || lx > LIST_X + LIST_W || ly < LIST_TOP || ly >= LIST_TOP + visibleRows() * ROW_H) {
            return -1;
        }
        int i = listScroll + (int) ((ly - LIST_TOP) / ROW_H);
        return i >= 0 && i < filtered.size() ? i : -1;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        double lx = mx - leftPos;
        double ly = my - topPos;
        if (lx >= LIST_X && lx <= LIST_X + LIST_W && ly >= LIST_TOP && ly <= LIST_BOTTOM && maxListScroll() > 0) {
            listScroll = Math.max(0, Math.min(maxListScroll(), listScroll - (int) Math.signum(sy)));
            return true;
        }
        if (tab == Tab.GENE_DATABASE && lx >= DETAIL_X && ly >= DETAIL_TOP && ly <= DETAIL_BOTTOM && detailMaxScroll > 0) {
            detailScroll = Math.max(0f, Math.min(detailMaxScroll, detailScroll - (float) sy * 14f));
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    /** No slot interaction while the grid is hidden. */
    @Override
    protected void slotClicked(Slot slot, int slotId, int buttonNum, net.minecraft.world.inventory.ContainerInput input) {
        if (tab != Tab.CRAFTING && slot != null && slotId >= 0 && slotId < HorseBrowserMenu.INV_START) {
            return;
        }
        super.slotClicked(slot, slotId, buttonNum, input);
    }

    // --- drawing ---

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        menu.setCraftingVisible(tab == Tab.CRAFTING);
        super.extractBackground(g, mouseX, mouseY, partialTick);

        if (searchBox != null && !searchBox.getValue().equals(search)) {
            search = searchBox.getValue();
            applyFilter();
        }

        int l = leftPos;
        int t = topPos;
        int r = l + IMG_W;
        int b = t + IMG_H;

        g.fill(l, t + TAB_H, r, b, PANEL);
        g.fill(l, t + TAB_H, r, t + TAB_H + 1, BORDER);
        g.fill(l, b - 1, r, b, BORDER);
        g.fill(l, t + TAB_H, l + 1, b, BORDER);
        g.fill(r - 1, t + TAB_H, r, b, BORDER);

        int tx = l;
        for (Tab tb : Tab.values()) {
            int w = this.font.width(tb.label) + 16;
            boolean on = tb == tab;
            g.fill(tx, t, tx + w, t + TAB_H, on ? TAB_ON : TAB_OFF);
            g.fill(tx, t, tx + w, t + 1, BORDER);
            tx += w + 2;
        }

        g.fill(l + LIST_X - 2, t + LIST_TOP - 2, l + LIST_X + LIST_W + 2, t + LIST_BOTTOM + 2, LIST_BG);

        if (tab == Tab.CRAFTING) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    int sx = l + HorseBrowserMenu.GRID_X + col * 18;
                    int sy = t + HorseBrowserMenu.GRID_Y + row * 18;
                    g.fill(sx - 1, sy - 1, sx + 17, sy + 17, SLOT_BG);
                }
            }
            int rx = l + HorseBrowserMenu.RESULT_X;
            int ry = t + HorseBrowserMenu.RESULT_Y;
            g.fill(rx - 1, ry - 1, rx + 17, ry + 17, SLOT_RESULT_BG);
            // arrow
            g.fill(rx - 22, ry + 7, rx - 6, ry + 9, 0xFF6A6A78);
        }

        // player inventory cells (always visible)
        for (int i = 0; i < 27; i++) {
            int sx = l + HorseBrowserMenu.INV_X + (i % 9) * 18;
            int sy = t + HorseBrowserMenu.INV_Y + (i / 9) * 18;
            g.fill(sx - 1, sy - 1, sx + 17, sy + 17, SLOT_BG);
        }
        for (int i = 0; i < 9; i++) {
            int sx = l + HorseBrowserMenu.INV_X + i * 18;
            int sy = t + HorseBrowserMenu.INV_Y + 58;
            g.fill(sx - 1, sy - 1, sx + 17, sy + 17, SLOT_BG);
        }
        g.fill(l + 6, t + IMG_H - 88, r - 6, t + IMG_H - 87, BORDER);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        // (matrix is translated by leftPos/topPos here; mouse coords are absolute)
        int tx = 0;
        for (Tab tb : Tab.values()) {
            int w = this.font.width(tb.label) + 16;
            g.text(this.font, Component.literal(tb.label), tx + 8, 4, tb == tab ? TAB_TEXT_ON : TAB_TEXT_OFF, false);
            tx += w + 2;
        }

        if (writeButton != null) {
            Gene sel = selected();
            boolean canWrite = tab == Tab.GENE_DATABASE && sel != null
                    && ClientGeneDatabase.knows(sel.key()) && sel.hasGeneCarrot();
            writeButton.visible = canWrite;
            writeButton.active = canWrite;
        }

        drawGeneList(g, mouseX, mouseY);
        if (tab == Tab.GENE_DATABASE) {
            drawGeneDetail(g);
        } else {
            drawCraftingInfo(g);
        }

        g.text(this.font, Component.translatable("container.inventory"), LIST_X, IMG_H - 84, LABEL, false);
    }

    private void drawGeneList(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, Component.literal(filtered.size() + (filtered.size() == 1 ? " gene" : " genes")),
                LIST_X, LIST_TOP - 11, LABEL, false);

        int sx0 = leftPos + LIST_X;
        int sy0 = topPos + LIST_TOP;
        int sx1 = leftPos + LIST_X + LIST_W;
        int sy1 = topPos + LIST_BOTTOM;
        g.enableScissor(sx0, sy0, sx1, sy1);
        int hovered = rowAt(mouseX, mouseY);
        for (int i = listScroll; i < filtered.size() && i < listScroll + visibleRows(); i++) {
            Gene gene = filtered.get(i);
            int ry = LIST_TOP + (i - listScroll) * ROW_H;
            boolean sel = gene.key().equals(selectedKey);
            if (sel) {
                g.fill(LIST_X - 2, ry, LIST_X + LIST_W, ry + ROW_H, ROW_SEL);
            } else if (i == hovered) {
                g.fill(LIST_X - 2, ry, LIST_X + LIST_W, ry + ROW_H, ROW_HOVER);
            }
            int col = !discovered(gene) ? TAG : (sel ? NAME : NAME_DIM);
            drawFitted(g, gene.name(), LIST_X, ry + 3, LIST_W - 6, col);
        }
        g.disableScissor();
        drawListScrollbar(g);
    }

    private void drawListScrollbar(GuiGraphicsExtractor g) {
        int max = maxListScroll();
        if (max <= 0) {
            return;
        }
        int trackH = LIST_BOTTOM - LIST_TOP;
        int x1 = LIST_X + LIST_W;
        int x0 = x1 - 3;
        g.fill(x0, LIST_TOP, x1, LIST_BOTTOM, 0x33FFFFFF);
        int thumbH = Math.max(16, trackH * visibleRows() / filtered.size());
        int thumbY = LIST_TOP + (trackH - thumbH) * listScroll / max;
        g.fill(x0, thumbY, x1, thumbY + thumbH, 0xAAFFFFFF);
    }

    private void drawCraftingInfo(GuiGraphicsExtractor g) {
        int x = HorseBrowserMenu.GRID_X;
        int y = HorseBrowserMenu.GRID_Y + 60;
        Gene sel = selected();

        g.text(this.font, Component.literal("Selected gene"), x, y, LABEL, false);
        y += 11;
        if (sel == null) {
            g.text(this.font, Component.literal("- none -"), x, y, NAME_DIM, false);
        } else {
            boolean ok = sel.hasGeneCarrot() && discovered(sel);
            g.text(this.font, Component.literal(sel.name()), x, y, ok ? EXPR_ON : EXPR_OFF, false);
            y += 10;
            String note = !sel.hasGeneCarrot() ? "no gene carrot for this gene"
                    : !discovered(sel) ? "not discovered yet - read a paper or meet a horse"
                    : "put a book in the grid to write its paper";
            for (String line : GuiText.wrap(this.font, note, IMG_W - x - 8)) {
                g.text(this.font, Component.literal(line), x, y, DESC, false);
                y += 10;
            }
        }

        y = HorseBrowserMenu.RESULT_Y + 24;
        for (String line : GuiText.wrap(this.font,
                "This grid only makes Horse Genetics recipes: gene papers, Known Gene Splice "
                        + "carrots, and combined carrots.", IMG_W - HorseBrowserMenu.GRID_X - 8)) {
            g.text(this.font, Component.literal(line), HorseBrowserMenu.GRID_X, y, TAG, false);
            y += 10;
        }
    }

    private void drawGeneDetail(GuiGraphicsExtractor g) {
        Gene gene = selected();
        int l = DETAIL_X;
        int r = IMG_W - 10;
        int top = DETAIL_TOP;
        int bottom = DETAIL_BOTTOM - (writeButton != null && writeButton.visible ? 22 : 0);
        int w = r - l;

        if (gene == null) {
            g.text(this.font, Component.literal("No genes match \"" + search + "\"."), l, top + 4, NAME_DIM, false);
            detailMaxScroll = 0f;
            return;
        }

        g.enableScissor(leftPos + l, topPos + top, leftPos + r, topPos + bottom);
        int lineH = this.font.lineHeight + 2;
        int y = top - (int) detailScroll;
        int startY = y;

        g.text(this.font, Component.literal(gene.name()), l, y, HEADING, false);
        y += lineH + 1;

        String tags = gene.key() + "   " + (gene.isNatural() ? "natural" : "magical")
                + "   " + gene.rarity().name().toLowerCase()
                + (gene.affectsCoat() ? "" : "   no coat effect");
        g.text(this.font, Component.literal(tags), l, y, TAG, false);
        y += lineH + 4;

        if (!discovered(gene)) {
            g.text(this.font, Component.literal("Not yet discovered"), l, y, EXPR_OFF, false);
            y += lineH;
            for (String line : GuiText.wrap(this.font,
                    "Tame or breed a horse carrying this gene, or read a research paper, to fill in "
                            + "its entry. Nothing about a horse is hidden - this list only tracks what "
                            + "you have met.", w)) {
                g.text(this.font, Component.literal(line), l, y, DESC, false);
                y += lineH;
            }
            g.disableScissor();
            detailMaxScroll = 0f;
            return;
        }

        String summary = gene.description();
        if (summary == null || summary.isBlank()) {
            summary = "No summary available yet - see the wiki gene page.";
        }
        for (String line : GuiText.wrap(this.font, summary, w)) {
            g.text(this.font, Component.literal(line), l, y, DESC, false);
            y += lineH;
        }
        y += 6;

        List<Allele> alleles = gene.alleles();
        g.text(this.font, Component.literal("Alleles (" + alleles.size() + ")"), l, y, LABEL, false);
        y += lineH + 2;
        for (Allele a : alleles) {
            g.text(this.font, Component.literal(a.token()), l, y, ALLELE_TOK, false);
            int tw = this.font.width(a.token());
            drawFitted(g, "- " + a.label(), l + tw + 6, y, w - tw - 6, DESC);
            y += lineH;
        }
        y += 6;

        List<Expression> exprs = gene.expressions();
        g.text(this.font, Component.literal("Phenotypes (" + exprs.size() + ")"), l, y, LABEL, false);
        y += lineH + 2;
        for (Expression e : exprs) {
            StringBuilder head = new StringBuilder(e.name());
            List<String> flags = new ArrayList<>();
            if (e.wildType()) flags.add("wild type");
            if (e.masks()) flags.add("masks other genes");
            if (!e.deterministic()) flags.add("varies per horse");
            if (!flags.isEmpty()) {
                head.append("   (").append(String.join(", ", flags)).append(')');
            }
            g.text(this.font, Component.literal(head.toString()), l, y, e.wildType() ? EXPR_OFF : EXPR_ON, false);
            y += lineH;
            String d = e.description();
            if (d != null && !d.isBlank()) {
                for (String line : GuiText.wrap(this.font, d, w - 8)) {
                    g.text(this.font, Component.literal(line), l + 8, y, DESC, false);
                    y += lineH;
                }
            }
            y += 3;
        }

        if (gene.hasGeneCarrot()) {
            y += 4;
            boolean unlocked = ClientGeneDatabase.carrotUnlocked(gene.key());
            g.text(this.font, Component.literal("Splice carrot recipe: " + (unlocked ? "unlocked" : "locked")),
                    l, y, unlocked ? EXPR_ON : EXPR_OFF, false);
            y += lineH;
            List<String> seen = ClientGeneDatabase.seenTokens(gene.key());
            if (!creative() && !seen.isEmpty()) {
                drawFitted(g, "Variants seen: " + String.join(", ", seen), l, y, w, DESC);
                y += lineH;
            }
        }

        g.disableScissor();

        int contentH = y - startY;
        detailMaxScroll = Math.max(0f, contentH - (bottom - top));
        detailScroll = Math.max(0f, Math.min(detailScroll, detailMaxScroll));

        if (detailMaxScroll > 0f) {
            int trackH = bottom - top;
            int x1 = r + 4;
            int x0 = x1 - 3;
            g.fill(x0, top, x1, bottom, 0x33FFFFFF);
            int thumbH = Math.max(16, (int) ((long) trackH * trackH / contentH));
            int thumbY = top + Math.round(detailScroll * (trackH - thumbH) / detailMaxScroll);
            g.fill(x0, thumbY, x1, thumbY + thumbH, 0xAAFFFFFF);
        }
    }

    private void drawFitted(GuiGraphicsExtractor g, String text, int x, int y, int maxW, int color) {
        float fw = this.font.width(text);
        if (fw <= maxW || fw <= 0) {
            g.text(this.font, Component.literal(text), x, y, color, false);
            return;
        }
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(maxW / fw);
        g.text(this.font, Component.literal(text), 0, 0, color, false);
        pose.popMatrix();
    }
}
