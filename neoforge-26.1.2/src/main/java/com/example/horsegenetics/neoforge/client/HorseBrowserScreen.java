package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The <b>Horse Browser</b> - opened with the configurable key (default
 * <kbd>H</kbd>). A tabbed reference window; this is the first slice, with a
 * single tab.
 *
 * <h2>Gene Database tab</h2>
 * In <b>creative</b>, every registered gene, alphabetically by display name.
 * Filter by typing part of a gene name <i>or</i> an allele (token or label);
 * click a gene to see its summary, every allele it defines, and the
 * plain-English description of every phenotype it can produce. Outside creative
 * the tab is a short note - it is a reference, not something the survival player
 * is meant to have memorised for them.
 *
 * <p>Pure client screen: all of this is in {@code common/} and already on the
 * client, so there is nothing to ask the server for.
 */
public final class HorseBrowserScreen extends Screen {

    /** Tabs of the browser. Only one is built; the strip is drawn so more slot in cleanly. */
    private enum Tab {
        GENE_DATABASE("Gene Database");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    private static final int DIM = 0xD0101014;
    private static final int PANEL = 0xF00E0E12;
    private static final int BORDER = 0xFF3A3A48;
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

    private static final int ROW_H = 14;

    // Preserved across init() (resize / reopen within a session is not expected,
    // but resize is) - selection, filter text, scroll offsets.
    private Tab tab = Tab.GENE_DATABASE;
    private String search = "";
    private String selectedKey = "";
    private int listScroll = 0;
    private float detailScroll = 0f;
    private float detailMaxScroll = 0f;

    private EditBox searchBox;
    private final List<Gene> allGenes;
    private List<Gene> filtered = List.of();

    public HorseBrowserScreen() {
        super(Component.literal("Horse Browser"));
        List<Gene> genes = new ArrayList<>(Genes.codeOrder());
        genes.sort(Comparator.comparing(Gene::name, String.CASE_INSENSITIVE_ORDER));
        this.allGenes = List.copyOf(genes);
        if (!allGenes.isEmpty()) {
            this.selectedKey = allGenes.get(0).key();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean creative() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getAbilities().instabuild;
    }

    // --- geometry ---

    private int panelLeft() {
        return Math.max(8, this.width / 2 - 300);
    }

    private int panelRight() {
        return Math.min(this.width - 8, this.width / 2 + 300);
    }

    private int panelTop() {
        return 22;
    }

    private int panelBottom() {
        return this.height - 30;
    }

    private int contentTop() {
        return panelTop() + 22;
    }

    private int listLeft() {
        return panelLeft() + 8;
    }

    private int listWidth() {
        return Math.max(140, (panelRight() - panelLeft()) * 30 / 100);
    }

    private int listRight() {
        return listLeft() + listWidth();
    }

    private int searchBoxTop() {
        return contentTop() + 2;
    }

    private int searchBoxBottom() {
        return searchBoxTop() + 16;
    }

    private int listTop() {
        return contentTop() + 34;
    }

    private int detailLeft() {
        return listRight() + 12;
    }

    private int detailRight() {
        return panelRight() - 10;
    }

    private int visibleRows() {
        return Math.max(1, (panelBottom() - 8 - listTop()) / ROW_H);
    }

    private int maxListScroll() {
        return Math.max(0, filtered.size() - visibleRows());
    }

    // --- widgets ---

    @Override
    protected void init() {
        super.init();

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(this.width / 2 - 50, this.height - 26, 100, 20).build());

        searchBox = new EditBox(this.font, listLeft(), searchBoxTop(), listWidth(), 16,
                Component.literal("Filter"));
        searchBox.setMaxLength(48);
        searchBox.setHint(Component.literal("filter: gene or allele"));
        searchBox.setValue(search);
        addRenderableWidget(searchBox);

        applyFilter();
    }

    private void applyFilter() {
        String q = search.trim().toLowerCase(Locale.ROOT);
        List<Gene> out = new ArrayList<>();
        for (Gene g : allGenes) {
            if (q.isEmpty() || matches(g, q)) {
                out.add(g);
            }
        }
        filtered = out;
        listScroll = Math.max(0, Math.min(listScroll, maxListScroll()));
        // keep the selection if it survived the filter; otherwise take the first hit
        boolean stillThere = filtered.stream().anyMatch(g -> g.key().equals(selectedKey));
        if (!stillThere && !filtered.isEmpty()) {
            selectedKey = filtered.get(0).key();
            detailScroll = 0f;
        }
    }

    private static boolean matches(Gene g, String q) {
        if (g.name().toLowerCase(Locale.ROOT).contains(q)) {
            return true;
        }
        if (g.key().toLowerCase(Locale.ROOT).contains(q)) {
            return true;
        }
        for (Allele a : g.alleles()) {
            if (a.token().toLowerCase(Locale.ROOT).contains(q)
                    || a.label().toLowerCase(Locale.ROOT).contains(q)) {
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

    // --- input ---

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        Tab hitTab = tabAt(event.x(), event.y());
        if (hitTab != null) {
            if (hitTab != tab) {
                tab = hitTab;
            }
            return true;
        }
        if (tab != Tab.GENE_DATABASE || !creative()) {
            return false;
        }
        int idx = rowAt(event.x(), event.y());
        if (idx >= 0) {
            Gene g = filtered.get(idx);
            if (!g.key().equals(selectedKey)) {
                selectedKey = g.key();
                detailScroll = 0f;
            }
            return true;
        }
        return false;
    }

    /** The tab whose strip button is under {@code (mx, my)}, or {@code null}. */
    private Tab tabAt(double mx, double my) {
        int pt = panelTop();
        if (my < pt || my > pt + 18) {
            return null;
        }
        int tx = panelLeft();
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
        if (mx < listLeft() || mx > listRight() || my < listTop()
                || my >= listTop() + visibleRows() * ROW_H) {
            return -1;
        }
        int i = listScroll + (int) ((my - listTop()) / ROW_H);
        return i >= 0 && i < filtered.size() ? i : -1;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (tab == Tab.GENE_DATABASE && creative()) {
            if (mx >= listLeft() && mx <= listRight() && my >= listTop() && maxListScroll() > 0) {
                listScroll = Math.max(0, Math.min(maxListScroll(), listScroll - (int) Math.signum(sy)));
                return true;
            }
            if (mx >= detailLeft() && mx <= detailRight() && detailMaxScroll > 0) {
                detailScroll = Math.max(0f, Math.min(detailMaxScroll, detailScroll - (float) sy * 16f));
                return true;
            }
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    // --- drawing ---

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.fill(0, 0, this.width, this.height, DIM);

        if (searchBox != null && !searchBox.getValue().equals(search)) {
            search = searchBox.getValue();
            applyFilter();
        }
        boolean showSearch = tab == Tab.GENE_DATABASE && creative();
        if (searchBox != null) {
            searchBox.visible = showSearch;
            searchBox.active = showSearch;
        }

        int pl = panelLeft();
        int pr = panelRight();
        int pt = panelTop();
        int pb = panelBottom();

        g.text(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2, 8, HEADING);

        // tab strip
        int tx = pl;
        for (Tab t : Tab.values()) {
            int w = this.font.width(t.label) + 16;
            boolean on = t == tab;
            g.fill(tx, pt, tx + w, pt + 18, on ? TAB_ON : TAB_OFF);
            g.fill(tx, pt, tx + w, pt + 1, BORDER);
            g.text(this.font, Component.literal(t.label), tx + 8, pt + 5, on ? TAB_TEXT_ON : TAB_TEXT_OFF);
            tx += w + 2;
        }

        // panel body. Widgets (the search box) are drawn by super BEFORE this
        // method's body, so the fill is carved around the box rather than drawn
        // over it - the same lesson CustomHorseSpawnScreen records.
        if (showSearch) {
            int sx0 = listLeft();
            int sx1 = listLeft() + listWidth();
            int sy0 = searchBoxTop();
            int sy1 = searchBoxBottom();
            g.fill(pl, pt + 18, pr, sy0, PANEL);
            g.fill(pl, sy0, sx0, sy1, PANEL);
            g.fill(sx1, sy0, pr, sy1, PANEL);
            g.fill(pl, sy1, pr, pb, PANEL);
        } else {
            g.fill(pl, pt + 18, pr, pb, PANEL);
        }
        g.fill(pl, pb - 1, pr, pb, BORDER);
        g.fill(pl, pt + 18, pl + 1, pb, BORDER);
        g.fill(pr - 1, pt + 18, pr, pb, BORDER);

        if (tab == Tab.GENE_DATABASE) {
            if (creative()) {
                drawGeneList(g, mouseX, mouseY);
                drawGeneDetail(g);
            } else {
                drawNotCreative(g, pl, pr, pt, pb);
            }
        }
    }

    private void drawNotCreative(GuiGraphicsExtractor g, int pl, int pr, int pt, int pb) {
        String[] lines = {
                "The gene database is a creative-mode reference.",
                "Switch to Creative to browse every gene, allele and phenotype."
        };
        int cy = (pt + 18 + pb) / 2 - lines.length * 6;
        for (String line : lines) {
            g.text(this.font, Component.literal(line),
                    (pl + pr) / 2 - this.font.width(line) / 2, cy, NAME_DIM);
            cy += 12;
        }
    }

    private void drawGeneList(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int l = listLeft();
        int r = listRight();
        int top = listTop();
        int bottom = panelBottom() - 8;

        g.text(this.font, Component.literal(filtered.size()
                + (filtered.size() == 1 ? " gene" : " genes")), l, top - 11, LABEL);

        g.enableScissor(l, top, r, bottom);
        int hovered = rowAt(mouseX, mouseY);
        for (int i = listScroll; i < filtered.size() && i < listScroll + visibleRows(); i++) {
            Gene gene = filtered.get(i);
            int ry = top + (i - listScroll) * ROW_H;
            boolean sel = gene.key().equals(selectedKey);
            if (sel) {
                g.fill(l - 2, ry, r, ry + ROW_H, ROW_SEL);
            } else if (i == hovered) {
                g.fill(l - 2, ry, r, ry + ROW_H, ROW_HOVER);
            }
            drawFitted(g, gene.name(), l, ry + 3, r - l - 6, sel ? NAME : NAME_DIM);
        }
        g.disableScissor();

        drawListScrollbar(g, r, top, bottom);
    }

    private void drawListScrollbar(GuiGraphicsExtractor g, int r, int top, int bottom) {
        int max = maxListScroll();
        if (max <= 0) {
            return;
        }
        int trackH = bottom - top;
        int rows = filtered.size();
        int x1 = r - 1;
        int x0 = x1 - 3;
        g.fill(x0, top, x1, bottom, 0x33FFFFFF);
        int thumbH = Math.max(16, trackH * visibleRows() / rows);
        int thumbY = top + (trackH - thumbH) * listScroll / max;
        g.fill(x0, thumbY, x1, thumbY + thumbH, 0xAAFFFFFF);
    }

    private void drawGeneDetail(GuiGraphicsExtractor g) {
        Gene gene = selected();
        int l = detailLeft();
        int r = detailRight();
        int top = contentTop();
        int bottom = panelBottom() - 8;
        int w = r - l;

        if (gene == null) {
            g.text(this.font, Component.literal("No genes match \"" + search + "\"."), l, top + 4, NAME_DIM);
            detailMaxScroll = 0f;
            return;
        }

        g.enableScissor(l, top, r, bottom);
        int lineH = this.font.lineHeight + 2;
        int y = top - (int) detailScroll;
        int startY = y;

        g.text(this.font, Component.literal(gene.name()), l, y, HEADING);
        y += lineH + 1;

        String tags = gene.key() + "   ·   " + (gene.isNatural() ? "natural" : "magical")
                + "   ·   priority " + gene.priority()
                + (gene.affectsCoat() ? "" : "   ·   no coat effect");
        g.text(this.font, Component.literal(tags), l, y, TAG);
        y += lineH + 4;

        String summary = gene.description();
        if (summary == null || summary.isBlank()) {
            summary = "No summary available yet — see the wiki gene page.";
        }
        for (String line : GuiText.wrap(this.font, summary, w)) {
            g.text(this.font, Component.literal(line), l, y, DESC);
            y += lineH;
        }
        y += 6;

        List<Allele> alleles = gene.alleles();
        g.text(this.font, Component.literal("Alleles (" + alleles.size() + ")"), l, y, LABEL);
        y += lineH + 2;
        for (Allele a : alleles) {
            g.text(this.font, Component.literal(a.token()), l, y, ALLELE_TOK);
            int tw = this.font.width(a.token());
            drawFitted(g, "— " + a.label(), l + tw + 6, y, w - tw - 6, DESC);
            y += lineH;
        }
        y += 6;

        List<Expression> exprs = gene.expressions();
        g.text(this.font, Component.literal("Phenotypes (" + exprs.size() + ")"), l, y, LABEL);
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
            g.text(this.font, Component.literal(head.toString()), l, y,
                    e.wildType() ? EXPR_OFF : EXPR_ON);
            y += lineH;
            String d = e.description();
            if (d != null && !d.isBlank()) {
                for (String line : GuiText.wrap(this.font, d, w - 8)) {
                    g.text(this.font, Component.literal(line), l + 8, y, DESC);
                    y += lineH;
                }
            }
            y += 3;
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
            g.text(this.font, Component.literal(text), x, y, color);
            return;
        }
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(maxW / fw);
        g.text(this.font, Component.literal(text), 0, 0, color);
        pose.popMatrix();
    }
}
