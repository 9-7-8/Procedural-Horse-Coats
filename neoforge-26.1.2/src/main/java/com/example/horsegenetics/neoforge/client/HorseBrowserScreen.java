package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.neoforge.menu.HorseBrowserMenu;
import com.example.horsegenetics.neoforge.menu.SpliceRecipeDisplay;
import com.example.horsegenetics.neoforge.network.SelectBrowserGenePayload;
import com.example.horsegenetics.neoforge.network.ViewSpliceRecipePayload;
import com.example.horsegenetics.neoforge.network.WriteResearchPaperPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The <b>Horse Browser</b> - opened with the browser key (default <kbd>H</kbd>).
 * An {@link AbstractContainerScreen} over {@link HorseBrowserMenu}, with two
 * tabs drawn from a strip at the top of the window:
 *
 * <ul>
 *   <li><b>Gene database</b> - a full-window reference: a filterable gene list on
 *       the left, a scrolling detail pane on the right. No slots (the menu's
 *       slots all go inactive here).</li>
 *   <li><b>Crafting</b> - a compact centered panel: a 3x3 grid + result slot that
 *       only makes this mod's recipes (see {@code HorseBrowserRecipes}), the
 *       gene list reused on the left to pick which gene a book becomes a paper
 *       for, and the player inventory.</li>
 * </ul>
 *
 * <p>All custom drawing is done in screen coordinates: {@code extractLabels}
 * runs inside the container's {@code leftPos/topPos} translate, so it is undone
 * first.
 */
public final class HorseBrowserScreen extends AbstractContainerScreen<HorseBrowserMenu> {

    private enum Tab {
        GENE_DATABASE("Gene database"),
        CRAFTING("Crafting");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    private static final int DIM = 0xC80A0A0E;
    private static final int PANEL = 0xF014141C;
    private static final int PANEL_SOFT = 0xE01A1A24;
    private static final int BORDER = 0xFF3C3C4A;
    private static final int SLOT_BG = 0xFF2B2B36;
    private static final int SLOT_RESULT_BG = 0xFF473C22;
    private static final int TAB_ON = 0xFF2E2E3C;
    private static final int TAB_OFF = 0xFF191921;
    private static final int TAB_TEXT_ON = 0xFFFFFFFF;
    private static final int TAB_TEXT_OFF = 0xFF888F9F;
    private static final int LABEL = 0xFF8890A8;
    private static final int ROW_HOVER = 0x22FFFFFF;
    private static final int ROW_SEL = 0x3355A0E0;
    private static final int NAME = 0xFFE4E8F0;
    private static final int NAME_DIM = 0xFF9AA0B0;
    private static final int HEADING = 0xFFF2F2F6;
    private static final int EXPR_ON = 0xFF9BE08A;
    private static final int EXPR_OFF = 0xFF6E7686;
    private static final int DESC = 0xFFB2B8C6;
    private static final int TAG = 0xFF7C84A0;
    private static final int ALLELE_TOK = 0xFFE0C070;

    private static final int IMG_W = 262; // the Crafting panel; leftPos/topPos centre it
    private static final int IMG_H = 210;
    private static final int TAB_TOP = 6;
    private static final int TAB_H = 18;
    private static final int ROW_H = 12;

    private Tab tab = Tab.GENE_DATABASE;
    private String search = "";
    private String selectedKey = "";
    private int listScroll = 0;
    private float detailScroll = 0f;
    private float detailMaxScroll = 0f;

    private EditBox searchBox;
    private Button craftPaperButton;
    private Button viewRecipeButton;
    private final List<Gene> allGenes;
    private List<Gene> filtered = List.of();
    /** Ghost stacks for the current "View splice recipe", in grid order; empty = none shown. */
    private java.util.List<net.minecraft.world.item.ItemStack> spliceGhosts = java.util.List.of();

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

    private boolean craftable(Gene g) {
        return g.hasGeneCarrot() && discovered(g);
    }

    // ------------------------------------------------------------------
    // Full-window (Gene database) geometry, in screen coords
    // ------------------------------------------------------------------

    private int contentTop() {
        return TAB_TOP + TAB_H + 8;
    }

    private int contentBottom() {
        return this.height - 12;
    }

    private int fsLeft() {
        return Math.max(16, this.width / 2 - 300);
    }

    private int fsRight() {
        return Math.min(this.width - 16, this.width / 2 + 300);
    }

    private int listX() {
        return fsLeft();
    }

    private int listW() {
        return Math.max(150, (fsRight() - fsLeft()) * 30 / 100);
    }

    private int listTop() {
        return contentTop() + 34;
    }

    private int detailX() {
        return listX() + listW() + 16;
    }

    private int detailR() {
        return fsRight();
    }

    private int visibleRows() {
        return Math.max(1, (contentBottom() - listTop()) / ROW_H);
    }

    private int maxListScroll() {
        return Math.max(0, filtered.size() - visibleRows());
    }

    // --- widgets ---

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = -9999;
        this.inventoryLabelX = -9999;

        searchBox = new EditBox(this.font, listX() + 1, contentTop(), listW() - 2, 16,
                Component.literal("Filter"));
        searchBox.setMaxLength(48);
        searchBox.setHint(Component.literal("filter by gene or allele"));
        searchBox.setValue(search);
        addRenderableWidget(searchBox);

        int bw = Math.min(190, detailR() - detailX());
        craftPaperButton = Button.builder(Component.translatable("gui.horsegenetics.craft_paper"), b -> craftPaper())
                .bounds(detailX(), contentBottom() - 42, bw, 18)
                .build();
        craftPaperButton.visible = false;
        addRenderableWidget(craftPaperButton);

        viewRecipeButton = Button.builder(Component.translatable("gui.horsegenetics.view_recipe"), b -> viewRecipe())
                .bounds(detailX(), contentBottom() - 20, bw, 18)
                .build();
        viewRecipeButton.visible = false;
        addRenderableWidget(viewRecipeButton);

        applyFilter();
    }

    private void craftPaper() {
        Gene g = selected();
        if (g != null && ClientGeneDatabase.knows(g.key())) {
            ClientPacketDistributor.sendToServer(new WriteResearchPaperPayload(g.key()));
        }
    }

    private void viewRecipe() {
        Gene g = selected();
        if (g == null || !g.hasGeneCarrot() || !discovered(g)) {
            return;
        }
        spliceGhosts = SpliceRecipeDisplay.forGene(g);
        ClientPacketDistributor.sendToServer(new ViewSpliceRecipePayload(g.key()));
        tab = Tab.CRAFTING;           // show the grid it just filled
        listScroll = 0;
        detailScroll = 0f;
        applyFilter();
    }

    /** Show / hide / position the two gene-action buttons for the current tab. */
    private void layoutGeneButtons() {
        if (craftPaperButton == null || viewRecipeButton == null) {
            return;
        }
        Gene sel = selected();
        boolean carrot = sel != null && sel.hasGeneCarrot();
        boolean known = sel != null && ClientGeneDatabase.knows(sel.key());
        boolean disc = sel != null && discovered(sel);

        craftPaperButton.visible = carrot && known;
        craftPaperButton.active = carrot && known;
        viewRecipeButton.visible = carrot && disc;
        viewRecipeButton.active = carrot && disc;

        if (tab == Tab.GENE_DATABASE) {
            int x = detailX();
            int w = Math.min(190, detailR() - detailX());
            craftPaperButton.setRectangle(w, 18, x, contentBottom() - 42);
            viewRecipeButton.setRectangle(w, 18, x, contentBottom() - 20);
        } else {
            int x = leftPos + HorseBrowserMenu.RESULT_X + 22;
            int w = leftPos + IMG_W - 8 - x;
            craftPaperButton.setRectangle(w, 16, x, topPos + 84);
            viewRecipeButton.setRectangle(w, 16, x, topPos + 102);
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
        if (filtered.stream().noneMatch(g -> g.key().equals(selectedKey)) && !filtered.isEmpty()) {
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
            spliceGhosts = java.util.List.of(); // a preview is for the gene it was raised on
        }
        ClientPacketDistributor.sendToServer(new SelectBrowserGenePayload(key));
    }

    // --- input ---

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Tab hit = tabAt(event.x(), event.y());
        if (hit != null) {
            if (hit != tab && menu.getCarried().isEmpty()) {
                tab = hit;
                listScroll = 0;
                detailScroll = 0f;
                if (hit == Tab.GENE_DATABASE) {
                    spliceGhosts = java.util.List.of();
                }
                applyFilter();
            }
            return true;
        }
        int row = rowAt(event.x(), event.y());
        if (row >= 0) {
            select(filtered.get(row).key());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private int tabStripLeft() {
        int total = 0;
        for (Tab t : Tab.values()) {
            total += this.font.width(t.label) + 24 + 4;
        }
        return this.width / 2 - total / 2;
    }

    private Tab tabAt(double mx, double my) {
        if (my < TAB_TOP || my > TAB_TOP + TAB_H) {
            return null;
        }
        int tx = tabStripLeft();
        for (Tab t : Tab.values()) {
            int w = this.font.width(t.label) + 24;
            if (mx >= tx && mx <= tx + w) {
                return t;
            }
            tx += w + 4;
        }
        return null;
    }

    private int rowAt(double mx, double my) {
        if (mx < listX() || mx > listX() + listW() || my < listTop()
                || my >= listTop() + visibleRows() * ROW_H) {
            return -1;
        }
        int i = listScroll + (int) ((my - listTop()) / ROW_H);
        return i >= 0 && i < filtered.size() ? i : -1;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (mx >= listX() && mx <= listX() + listW() && my >= listTop() && maxListScroll() > 0) {
            listScroll = Math.max(0, Math.min(maxListScroll(), listScroll - (int) Math.signum(sy)));
            return true;
        }
        if (tab == Tab.GENE_DATABASE && mx >= detailX() && my >= contentTop() && detailMaxScroll > 0) {
            detailScroll = Math.max(0f, Math.min(detailMaxScroll, detailScroll - (float) sy * 16f));
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    /** No slot interaction on the Gene database tab (its slots are inactive anyway). */
    @Override
    protected void slotClicked(Slot slot, int slotId, int buttonNum, ContainerInput input) {
        if (tab != Tab.CRAFTING && slot != null && slotId >= 0) {
            return;
        }
        super.slotClicked(slot, slotId, buttonNum, input);
    }

    // --- drawing ---

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        menu.setCraftingVisible(tab == Tab.CRAFTING);
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(0, 0, this.width, this.height, DIM);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        // Undo the container's leftPos/topPos translate so we work in screen coords.
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(-leftPos, -topPos);

        if (searchBox != null && !searchBox.getValue().equals(search)) {
            search = searchBox.getValue();
            applyFilter();
        }
        if (searchBox != null) {
            boolean showSearch = true; // both tabs filter the same list
            searchBox.visible = showSearch;
            searchBox.active = showSearch;
        }
        layoutGeneButtons();

        drawTabStrip(g);

        if (tab == Tab.GENE_DATABASE) {
            drawGeneList(g, mouseX, mouseY, contentBottom());
            drawGeneDetail(g);
        } else {
            drawCraftingPanel(g, mouseX, mouseY);
        }

        pose.popMatrix();
    }

    private void drawTabStrip(GuiGraphicsExtractor g) {
        int tx = tabStripLeft();
        for (Tab t : Tab.values()) {
            int w = this.font.width(t.label) + 24;
            boolean on = t == tab;
            g.fill(tx, TAB_TOP, tx + w, TAB_TOP + TAB_H, on ? TAB_ON : TAB_OFF);
            g.fill(tx, TAB_TOP, tx + w, TAB_TOP + 2, on ? 0xFF55A0E0 : BORDER);
            g.fill(tx, TAB_TOP + TAB_H - 1, tx + w, TAB_TOP + TAB_H, BORDER);
            g.text(this.font, Component.literal(t.label), tx + 12, TAB_TOP + 5,
                    on ? TAB_TEXT_ON : TAB_TEXT_OFF, false);
            tx += w + 4;
        }
    }

    private void drawGeneList(GuiGraphicsExtractor g, int mouseX, int mouseY, int bottom) {
        int l = listX();
        int w = listW();
        int top = listTop();

        g.text(this.font, Component.literal(filtered.size() + (filtered.size() == 1 ? " gene" : " genes")),
                l, top - 12, LABEL, false);

        g.fill(l - 2, top - 2, l + w + 2, bottom + 2, PANEL_SOFT);
        g.enableScissor(l, top, l + w, bottom);
        int hovered = rowAt(mouseX, mouseY);
        for (int i = listScroll; i < filtered.size() && i < listScroll + visibleRows(); i++) {
            Gene gene = filtered.get(i);
            int ry = top + (i - listScroll) * ROW_H;
            boolean sel = gene.key().equals(selectedKey);
            if (sel) {
                g.fill(l - 2, ry, l + w, ry + ROW_H, ROW_SEL);
            } else if (i == hovered) {
                g.fill(l - 2, ry, l + w, ry + ROW_H, ROW_HOVER);
            }
            int col = !discovered(gene) ? TAG : (sel ? NAME : NAME_DIM);
            drawFitted(g, gene.name(), l + 4, ry + 2, w - 12, col);
        }
        g.disableScissor();

        int max = maxListScroll();
        if (max > 0) {
            int trackH = bottom - top;
            int x1 = l + w;
            g.fill(x1 - 3, top, x1, bottom, 0x33FFFFFF);
            int thumbH = Math.max(16, trackH * visibleRows() / filtered.size());
            int thumbY = top + (trackH - thumbH) * listScroll / max;
            g.fill(x1 - 3, thumbY, x1, thumbY + thumbH, 0xAAFFFFFF);
        }
    }

    private void drawGeneDetail(GuiGraphicsExtractor g) {
        Gene gene = selected();
        int l = detailX();
        int r = detailR();
        int top = contentTop();
        boolean buttons = craftPaperButton != null && (craftPaperButton.visible || viewRecipeButton.visible);
        int bottom = contentBottom() - (buttons ? 48 : 0);
        int w = r - l;

        g.fill(l - 6, top - 2, r + 2, bottom + 2, PANEL_SOFT);

        if (gene == null) {
            g.text(this.font, Component.literal("No genes match \"" + search + "\"."), l, top + 4, NAME_DIM, false);
            detailMaxScroll = 0f;
            return;
        }

        g.enableScissor(l - 4, top, r, bottom);
        int lineH = this.font.lineHeight + 2;
        int y = top - (int) detailScroll;
        int startY = y;

        g.text(this.font, Component.literal(gene.name()), l, y, HEADING, false);
        y += lineH + 1;
        drawFitted(g, gene.key() + "    " + (gene.isNatural() ? "natural" : "magical")
                + "    " + gene.rarity().name().toLowerCase()
                + (gene.affectsCoat() ? "" : "    no coat effect"), l, y, w, TAG);
        y += lineH + 5;

        if (!discovered(gene)) {
            g.text(this.font, Component.literal("Not yet discovered"), l, y, EXPR_OFF, false);
            y += lineH;
            for (String line : GuiText.wrap(this.font,
                    "Tame or breed a horse carrying this gene, or read a research paper, to fill "
                            + "in its entry. Nothing about a horse is hidden - this list only tracks "
                            + "what you have met.", w)) {
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
            if (e.masks()) flags.add("masks");
            if (!e.deterministic()) flags.add("varies");
            if (!flags.isEmpty()) {
                head.append("   (").append(String.join(", ", flags)).append(')');
            }
            drawFitted(g, head.toString(), l, y, w, e.wildType() ? EXPR_OFF : EXPR_ON);
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
            y += 2;
            boolean unlocked = ClientGeneDatabase.carrotUnlocked(gene.key());
            g.text(this.font, Component.literal("Splice carrot recipe: " + (unlocked ? "unlocked" : "locked")),
                    l, y, unlocked ? EXPR_ON : EXPR_OFF, false);
            y += lineH;
            List<String> seen = ClientGeneDatabase.seenTokens(gene.key());
            if (!creative() && !seen.isEmpty()) {
                for (String line : GuiText.wrap(this.font, "Variants seen: " + String.join(", ", seen), w)) {
                    g.text(this.font, Component.literal(line), l, y, DESC, false);
                    y += lineH;
                }
            }
        }

        g.disableScissor();

        int contentH = y - startY;
        detailMaxScroll = Math.max(0f, contentH - (bottom - top));
        detailScroll = Math.max(0f, Math.min(detailScroll, detailMaxScroll));
        if (detailMaxScroll > 0f) {
            int trackH = bottom - top;
            int x1 = r + 1;
            g.fill(x1 - 3, top, x1, bottom, 0x33FFFFFF);
            int thumbH = Math.max(16, (int) ((long) trackH * trackH / contentH));
            int thumbY = top + Math.round(detailScroll * (trackH - thumbH) / detailMaxScroll);
            g.fill(x1 - 3, thumbY, x1, thumbY + thumbH, 0xAAFFFFFF);
        }
    }

    private void drawCraftingPanel(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int px = leftPos;
        int py = topPos;

        // the gene list on the left (full-window position, but a shorter run)
        int listBottom = Math.min(contentBottom(), py + IMG_H);
        drawGeneList(g, mouseX, mouseY, listBottom);

        // the centered crafting panel
        g.fill(px, py, px + IMG_W, py + IMG_H, PANEL);
        g.fill(px, py, px + IMG_W, py + 1, BORDER);
        g.fill(px, py + IMG_H - 1, px + IMG_W, py + IMG_H, BORDER);
        g.fill(px, py, px + 1, py + IMG_H, BORDER);
        g.fill(px + IMG_W - 1, py, px + IMG_W, py + IMG_H, BORDER);

        // grid + result slot backdrops (slots render on top of these)
        for (int i = 0; i < 9; i++) {
            int gx = px + HorseBrowserMenu.GRID_X + (i % 3) * 18;
            int gy = py + HorseBrowserMenu.GRID_Y + (i / 3) * 18;
            cell(g, gx, gy, SLOT_BG);
            // "View splice recipe" ghost: an ingredient the player did not have.
            // Drawn before the real slots, so a filled slot hides its ghost.
            if (i < spliceGhosts.size()) {
                net.minecraft.world.item.ItemStack ghost = spliceGhosts.get(i);
                if (!ghost.isEmpty() && menu.slots.get(HorseBrowserMenu.GRID_START + i).getItem().isEmpty()) {
                    g.fakeItem(ghost, gx, gy);
                    g.fill(gx, gy, gx + 16, gy + 16, 0xA6202028); // knock it back to a placeholder
                }
            }
        }
        cell(g, px + HorseBrowserMenu.RESULT_X, py + HorseBrowserMenu.RESULT_Y, SLOT_RESULT_BG);
        g.fill(px + HorseBrowserMenu.RESULT_X - 22, py + HorseBrowserMenu.RESULT_Y + 7,
                px + HorseBrowserMenu.RESULT_X - 6, py + HorseBrowserMenu.RESULT_Y + 9, 0xFF6A6A78);

        // player-inventory backdrops
        for (int i = 0; i < 27; i++) {
            cell(g, px + HorseBrowserMenu.INV_X + (i % 9) * 18, py + HorseBrowserMenu.INV_Y + (i / 9) * 18, SLOT_BG);
        }
        for (int i = 0; i < 9; i++) {
            cell(g, px + HorseBrowserMenu.INV_X + i * 18, py + HorseBrowserMenu.INV_Y + 58, SLOT_BG);
        }
        g.text(this.font, Component.translatable("container.inventory"),
                px + HorseBrowserMenu.INV_X, py + HorseBrowserMenu.INV_Y - 11, LABEL, false);

        // selected-gene note, right of the grid
        int tx = px + HorseBrowserMenu.RESULT_X + 22;
        int ty = py + 8;
        int tw = px + IMG_W - 8 - tx;
        Gene sel = selected();
        g.text(this.font, Component.literal("Gene paper"), tx, ty, LABEL, false);
        ty += this.font.lineHeight + 2;
        if (sel == null) {
            g.text(this.font, Component.literal("pick a gene"), tx, ty, NAME_DIM, false);
        } else {
            boolean ok = sel.hasGeneCarrot() && discovered(sel);
            drawFitted(g, sel.name(), tx, ty, tw, ok ? EXPR_ON : EXPR_OFF);
            ty += this.font.lineHeight + 2;
            String note = !sel.hasGeneCarrot() ? "no gene carrot for this gene"
                    : !discovered(sel) ? "not discovered yet"
                    : "add a book to the grid";
            for (String line : GuiText.wrap(this.font, note, tw)) {
                g.text(this.font, Component.literal(line), tx, ty, DESC, false);
                ty += this.font.lineHeight + 1;
            }
        }
    }

    private static void cell(GuiGraphicsExtractor g, int x, int y, int colour) {
        g.fill(x - 1, y - 1, x + 17, y + 17, colour);
    }

    /** Draw text at (x,y) screen; if wider than maxW, scale it to fit. */
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
