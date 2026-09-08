package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.BreedingPreview;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.horse.HorseListing;
import com.example.horsegenetics.common.horse.HorseQuery;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.neoforge.menu.HorseBrowserMenu;
import com.example.horsegenetics.neoforge.menu.SpliceRecipeDisplay;
import com.example.horsegenetics.neoforge.network.HorseRosterRequestPayload;
import com.example.horsegenetics.neoforge.network.SelectBrowserGenePayload;
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
import java.util.UUID;

/**
 * The <b>Horse Browser</b> - opened with the browser key (default <kbd>H</kbd>).
 * An {@link AbstractContainerScreen} over {@link HorseBrowserMenu}, with four
 * tabs drawn from a strip at the top of the window:
 *
 * <ul>
 *   <li><b>My horses</b> - every horse the player owns as one sortable,
 *       filterable table. The columns are clickable headings; the filter box
 *       takes the query language in {@link HorseQuery}, which reaches every
 *       piece of metadata a horse has, <b>genes included</b> ({@code gene:SB1}
 *       finds carriers, not just horses that show it). Rows are
 *       {@link HorseListing}s built once when the roster lands
 *       ({@link ClientHorseRoster}), not per frame.</li>
 *   <li><b>Gene database</b> - a full-window reference: a filterable gene list on
 *       the left, a scrolling detail pane on the right. No slots (the menu's
 *       slots all go inactive here).</li>
 *   <li><b>Breeding preview</b> - pick one of your mares and one of your
 *       stallions and read what they could produce at every locus, and how
 *       often. It is a <b>Punnett square per gene</b>, not a foal and not a
 *       list of genotypes: {@link BreedingPreview} says why, and it is grouped
 *       so the loci that pull on one trait arrive together. The roster comes
 *       from the server on opening the tab ({@code HorseRosterPayload}), and is
 *       the same roster the My horses table reads.</li>
 *   <li><b>Crafting</b> - a compact centered panel: a 3x3 grid + result slot that
 *       only makes this mod's recipes (see {@code HorseBrowserRecipes}), the
 *       gene list reused on the left to pick which gene a book becomes a paper
 *       for, and the player inventory.</li>
 * </ul>
 *
 * <h2>Drawing order</h2>
 * All custom drawing is done in screen coordinates, from
 * {@link #extractContents} and <b>before</b> its {@code super} call - which is
 * what draws the widgets and then the slots. It used to run from
 * {@code extractLabels}, after both, and this screen paints near-opaque panels
 * across the window: anything of ours that overlapped a widget washed it out,
 * which is what had happened to the Crafting tab's own button. Chrome under,
 * widgets and slots over.
 */
public final class HorseBrowserScreen extends AbstractContainerScreen<HorseBrowserMenu> {

    private enum Tab {
        MY_HORSES("My horses"),
        GENE_DATABASE("Gene database"),
        BREEDING_PREVIEW("Breeding preview"),
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
    private static final int GOOD = 0xFF9BE08A;
    private static final int BAD = 0xFFF08C8C;
    private static final int HEAD_BG = 0xFF23232E;
    private static final int DIVIDER = 0x18FFFFFF;

    private static final int IMG_W = 262; // the Crafting panel; leftPos/topPos centre it
    private static final int IMG_H = 210;
    private static final int TAB_TOP = 6;
    private static final int TAB_H = 18;
    private static final int ROW_H = 12;

    private Tab tab = Tab.MY_HORSES;
    private String search = "";
    private String selectedKey = "";
    private int listScroll = 0;
    private float detailScroll = 0f;
    private float detailMaxScroll = 0f;

    private EditBox searchBox;
    private EditBox horseFilterBox;
    private Button craftPaperButton;
    private Button refreshRosterButton;
    private Button settledToggle;

    // --- My horses tab ---
    private String horseFilter = "";
    private HorseQuery.Sort sort = HorseQuery.Sort.NAME;
    private boolean sortDescending = false;
    private int horseScroll = 0;
    private UUID selectedHorseId;
    /** Recomputed when the filter, the sort or the roster changes - never per frame. */
    private List<HorseListing> horseRows = List.of();
    private int horseRowsVersion = -1;
    private String horseRowsQuery = null;

    // --- Breeding preview tab ---
    private UUID damId;
    private UUID sireId;
    private boolean showSettled = false;
    private int mareScroll = 0;
    private int stallionScroll = 0;
    /** Recomputed only when the pair or the toggle changes - the walk is not free. */
    private List<BreedingPreview.Group> preview = List.of();
    private int previewRosterVersion = -1;
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
                .bounds(detailX(), contentBottom() - 22, bw, 18)
                .build();
        craftPaperButton.visible = false;
        addRenderableWidget(craftPaperButton);

        horseFilterBox = new EditBox(this.font, fsLeft() + 1, contentTop(),
                Math.max(160, (fsRight() - fsLeft()) / 2), 16, Component.literal("Filter"));
        horseFilterBox.setMaxLength(96);
        horseFilterBox.setHint(Component.literal("mare  gen>2  gene:SB1  -lethal"));
        horseFilterBox.setValue(horseFilter);
        addRenderableWidget(horseFilterBox);

        refreshRosterButton = Button.builder(Component.literal("Refresh"), b -> requestRoster())
                .bounds(listX(), contentTop() - 1, 60, 18).build();
        refreshRosterButton.visible = false;
        addRenderableWidget(refreshRosterButton);

        settledToggle = Button.builder(settledLabel(), b -> {
                    showSettled = !showSettled;
                    b.setMessage(settledLabel());
                    rebuildPreview();
                })
                .bounds(listX() + 64, contentTop() - 1, 130, 18).build();
        settledToggle.visible = false;
        addRenderableWidget(settledToggle);

        applyFilter();
        if (tab == Tab.MY_HORSES && !ClientHorseRoster.received()) {
            requestRoster();
        }
    }

    private Component settledLabel() {
        return Component.literal(showSettled ? "All loci" : "Only what can vary");
    }

    private void requestRoster() {
        ClientPacketDistributor.sendToServer(HorseRosterRequestPayload.INSTANCE);
    }

    private void craftPaper() {
        Gene g = selected();
        if (g != null && g.hasGeneCarrot()) {
            // The server re-checks discovery and that a book is in the inventory,
            // and messages the player if not - better than a dead button.
            ClientPacketDistributor.sendToServer(new WriteResearchPaperPayload(g.key()));
        }
    }

    /** The Known Gene Splice recipe stacks shown as ghosts for the selected gene, or empty. */
    private java.util.List<net.minecraft.world.item.ItemStack> spliceGhosts() {
        Gene sel = selected();
        return sel != null && sel.hasGeneCarrot() ? SpliceRecipeDisplay.forGene(sel) : java.util.List.of();
    }

    /**
     * Show, hide and place the per-tab buttons.
     *
     * <p><b>"Craft research paper" is on the Crafting tab and nowhere else.</b>
     * It was on the Gene database tab too, doing exactly the same thing from a
     * screen with no grid and no result slot to show for it - and it was eating
     * the bottom 26 pixels of the detail pane, which is the one part of that tab
     * that is short of room. One button, on the tab that is about crafting.
     */
    private void layoutGeneButtons() {
        boolean breeding = tab == Tab.BREEDING_PREVIEW;
        boolean mine = tab == Tab.MY_HORSES;
        if (refreshRosterButton != null) {
            // Both roster tabs want it; it sits in the same place on each.
            boolean show = breeding || mine;
            refreshRosterButton.visible = show;
            refreshRosterButton.active = show;
            if (mine) {
                refreshRosterButton.setRectangle(60, 18, fsRight() - 60, contentTop() - 1);
            } else {
                refreshRosterButton.setRectangle(60, 18, listX(), contentTop() - 1);
            }
        }
        if (settledToggle != null) {
            settledToggle.visible = breeding;
            settledToggle.active = breeding;
        }
        if (horseFilterBox != null) {
            horseFilterBox.visible = mine;
            horseFilterBox.active = mine;
            if (!mine) {
                horseFilterBox.setFocused(false);
            }
        }
        if (craftPaperButton == null) {
            return;
        }
        Gene sel = selected();
        boolean carrot = tab == Tab.CRAFTING && sel != null && sel.hasGeneCarrot();
        craftPaperButton.visible = carrot;
        craftPaperButton.active = carrot;
        if (carrot) {
            int x = leftPos + HorseBrowserMenu.RESULT_X + 22;
            int w = leftPos + IMG_W - 8 - x;
            craftPaperButton.setRectangle(w, 16, x, topPos + 90);
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
                applyFilter();
                if ((tab == Tab.BREEDING_PREVIEW || tab == Tab.MY_HORSES)
                        && !ClientHorseRoster.received()) {
                    requestRoster();
                }
            }
            return true;
        }
        if (tab == Tab.MY_HORSES) {
            HorseQuery.Sort column = columnAt(event.x(), event.y());
            if (column != null) {
                // Click a heading to sort by it; click the one you are already
                // sorted by to turn it round. Same as every table anywhere.
                if (column == sort) {
                    sortDescending = !sortDescending;
                } else {
                    sort = column;
                    sortDescending = defaultDescending(column);
                }
                rebuildHorseRows();
                return true;
            }
            HorseListing row = horseRowAt(event.x(), event.y());
            if (row != null) {
                selectedHorseId = row.id().equals(selectedHorseId) ? null : row.id();
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }
        if (tab == Tab.BREEDING_PREVIEW) {
            HorseListing mare = horseAt(event.x(), event.y(), Sex.FEMALE);
            if (mare != null) {
                damId = mare.id().equals(damId) ? null : mare.id();
                rebuildPreview();
                return true;
            }
            HorseListing stallion = horseAt(event.x(), event.y(), Sex.MALE);
            if (stallion != null) {
                sireId = stallion.id().equals(sireId) ? null : stallion.id();
                rebuildPreview();
                return true;
            }
            return super.mouseClicked(event, doubleClick);
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
        if (tab == Tab.MY_HORSES) {
            int max = Math.max(0, horseRows.size() - horseVisibleRows());
            horseScroll = Math.max(0, Math.min(max, horseScroll - (int) Math.signum(sy) * 3));
            return true;
        }
        if (tab == Tab.BREEDING_PREVIEW) {
            if (mx >= listX() && mx <= listX() + listW()) {
                boolean upper = my < pickerSplit();
                List<HorseListing> list = ClientHorseRoster.of(upper ? Sex.FEMALE : Sex.MALE);
                int visible = pickerRows(upper);
                int max = Math.max(0, list.size() - visible);
                if (upper) {
                    mareScroll = Math.max(0, Math.min(max, mareScroll - (int) Math.signum(sy)));
                } else {
                    stallionScroll = Math.max(0, Math.min(max, stallionScroll - (int) Math.signum(sy)));
                }
                return true;
            }
            if (detailMaxScroll > 0) {
                detailScroll = Math.max(0f, Math.min(detailMaxScroll, detailScroll - (float) sy * 16f));
            }
            return true;
        }
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

    /** Real slot tooltips, plus a real-item tooltip for a hovered recipe ghost. */
    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        if (tab != Tab.CRAFTING || (hoveredSlot != null && hoveredSlot.hasItem())) {
            return;
        }
        java.util.List<net.minecraft.world.item.ItemStack> ghosts = spliceGhosts();
        for (int i = 0; i < 9 && i < ghosts.size(); i++) {
            net.minecraft.world.item.ItemStack ghost = ghosts.get(i);
            if (ghost.isEmpty() || !menu.slots.get(HorseBrowserMenu.GRID_START + i).getItem().isEmpty()) {
                continue;
            }
            int gx = leftPos + HorseBrowserMenu.GRID_X + (i % 3) * 18;
            int gy = topPos + HorseBrowserMenu.GRID_Y + (i / 3) * 18;
            if (mouseX >= gx && mouseX < gx + 16 && mouseY >= gy && mouseY < gy + 16) {
                g.setTooltipForNextFrame(this.font, ghost, mouseX, mouseY);
                return;
            }
        }
    }

    // --- drawing ---

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        menu.setCraftingVisible(tab == Tab.CRAFTING);
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(0, 0, this.width, this.height, DIM);
    }

    /**
     * Everything this screen draws, in screen coordinates and <b>under</b> the
     * widgets and slots that {@code super} goes on to draw. See the class note
     * on drawing order.
     */
    @Override
    public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        drawChrome(g, mouseX, mouseY);
        super.extractContents(g, mouseX, mouseY, partialTick);
    }

    /** Vanilla's "Horse Browser" / "Inventory" captions; this screen draws its own. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
    }

    private void drawChrome(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (searchBox != null && !searchBox.getValue().equals(search)) {
            search = searchBox.getValue();
            applyFilter();
        }
        if (searchBox != null) {
            // The gene database and Crafting filter the same gene list; the two
            // roster tabs are about horses, and reusing one box for two
            // different lists reads as a bug the first time it clears itself.
            // My horses has its own box, with its own query language.
            boolean showSearch = tab == Tab.GENE_DATABASE || tab == Tab.CRAFTING;
            searchBox.visible = showSearch;
            searchBox.active = showSearch;
        }
        if (horseFilterBox != null && !horseFilterBox.getValue().equals(horseFilter)) {
            horseFilter = horseFilterBox.getValue();
            horseScroll = 0;
        }
        layoutGeneButtons();

        drawTabStrip(g);

        switch (tab) {
            case MY_HORSES -> drawMyHorses(g, mouseX, mouseY);
            case GENE_DATABASE -> {
                drawGeneList(g, mouseX, mouseY, contentBottom());
                drawGeneDetail(g);
            }
            case BREEDING_PREVIEW -> drawBreedingPreview(g, mouseX, mouseY);
            case CRAFTING -> drawCraftingPanel(g, mouseX, mouseY);
        }
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
        int bottom = contentBottom() - (craftPaperButton != null && craftPaperButton.visible ? 26 : 0);
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
        java.util.List<net.minecraft.world.item.ItemStack> ghosts = spliceGhosts();
        for (int i = 0; i < 9; i++) {
            int gx = px + HorseBrowserMenu.GRID_X + (i % 3) * 18;
            int gy = py + HorseBrowserMenu.GRID_Y + (i / 3) * 18;
            cell(g, gx, gy, SLOT_BG);
            // The selected gene's splice recipe, ghosted into any empty grid slot.
            // Drawn before the real slots, so a slot the player fills hides its ghost.
            if (i < ghosts.size()) {
                net.minecraft.world.item.ItemStack ghost = ghosts.get(i);
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

    // ------------------------------------------------------------------
    // My horses - the whole stable as one sortable, filterable table
    // ------------------------------------------------------------------
    //
    // The roadmap (wiki/roadmap.html#browser) is right that the expensive part
    // of this tab is the index behind it, and that sorting and filtering
    // "belong server-side, paginated". They are not there yet: this sorts and
    // filters the roster the client already has, capped at
    // HorseRosterPayload.MAX_ENTRIES. That cap is the whole of the difference,
    // it is said out loud in the footer when it bites, and nothing here has to
    // change when the real index arrives - the table draws HorseListings and
    // does not care who filtered them.
    //
    // The query language, the sort comparators and the coat description live in
    // common/horse (HorseQuery, HorseListing), where they are unit-tested
    // without a game and will survive the backport. This class draws.

    /** One column of the table: a sort key, and its share of the width. */
    private record Column(HorseQuery.Sort sort, int weight) {
    }

    private static final List<Column> COLUMNS = List.of(
            new Column(HorseQuery.Sort.NAME, 112),
            new Column(HorseQuery.Sort.BARN, 54),
            new Column(HorseQuery.Sort.SEX, 44),
            new Column(HorseQuery.Sort.AGE, 32),
            new Column(HorseQuery.Sort.BREED, 88),
            new Column(HorseQuery.Sort.GENERATION, 24),
            new Column(HorseQuery.Sort.COAT, 104),
            new Column(HorseQuery.Sort.SPEED, 40),
            new Column(HorseQuery.Sort.HEALTH, 40),
            new Column(HorseQuery.Sort.JUMP, 34),
            new Column(HorseQuery.Sort.SIZE, 34),
            new Column(HorseQuery.Sort.BOND, 30),
            new Column(HorseQuery.Sort.WHERE, 64));

    /** The weights above, summed. Columns are laid out as shares of this. */
    private static final int TOTAL_WEIGHT = 700;

    /** The heading strip, one row tall, above the rows themselves. */
    private int tableHeadY() {
        return contentTop() + 30;
    }

    private int tableTop() {
        return tableHeadY() + ROW_H + 2;
    }

    /** The footer holds the selected horse; the rows stop above it. */
    private int tableBottom() {
        return contentBottom() - 34;
    }

    private int horseVisibleRows() {
        return Math.max(1, (tableBottom() - tableTop()) / ROW_H);
    }

    private int columnX(int index) {
        int x = fsLeft() + 2;
        int avail = fsRight() - fsLeft() - 8;
        for (int i = 0; i < index; i++) {
            x += COLUMNS.get(i).weight() * avail / TOTAL_WEIGHT;
        }
        return x;
    }

    private int columnW(int index) {
        int avail = fsRight() - fsLeft() - 8;
        return Math.max(12, COLUMNS.get(index).weight() * avail / TOTAL_WEIGHT - 4);
    }

    /**
     * A number reads better biggest-first and a name reads better A-Z, so a
     * fresh click on a column starts it the way that column is usually wanted.
     */
    private static boolean defaultDescending(HorseQuery.Sort sort) {
        return switch (sort) {
            case GENERATION, SPEED, HEALTH, JUMP, SIZE, BOND -> true;
            default -> false;
        };
    }

    private HorseQuery.Sort columnAt(double mx, double my) {
        if (my < tableHeadY() || my >= tableHeadY() + ROW_H) {
            return null;
        }
        for (int i = 0; i < COLUMNS.size(); i++) {
            if (mx >= columnX(i) - 2 && mx < columnX(i) + columnW(i) + 2) {
                return COLUMNS.get(i).sort();
            }
        }
        return null;
    }

    private HorseListing horseRowAt(double mx, double my) {
        if (mx < fsLeft() || mx > fsRight() || my < tableTop()
                || my >= tableTop() + horseVisibleRows() * ROW_H) {
            return null;
        }
        int i = horseScroll + (int) ((my - tableTop()) / ROW_H);
        return i >= 0 && i < horseRows.size() ? horseRows.get(i) : null;
    }

    /**
     * Re-filter and re-sort. Called when something actually changed - a new
     * roster, a new query, a new column - and never per frame: a {@code gene:}
     * term walks every locus of every horse, which is nothing once and far too
     * much sixty times a second.
     */
    private void rebuildHorseRows() {
        horseRows = HorseQuery.apply(ClientHorseRoster.all(), horseFilter, sort, sortDescending);
        horseRowsVersion = ClientHorseRoster.version();
        horseRowsQuery = horseFilter;
        horseScroll = Math.max(0, Math.min(horseScroll,
                Math.max(0, horseRows.size() - horseVisibleRows())));
    }

    private void drawMyHorses(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (horseRowsVersion != ClientHorseRoster.version() || !horseFilter.equals(horseRowsQuery)) {
            rebuildHorseRows();
        }

        int l = fsLeft();
        int r = fsRight();
        int top = tableTop();
        int bottom = tableBottom();

        int total = ClientHorseRoster.all().size();
        String count = horseRows.size() == total
                ? total + (total == 1 ? " horse" : " horses")
                : horseRows.size() + " of " + total + " horses";
        int countW = this.font.width(count);
        g.text(this.font, Component.literal(count), l + 2, contentTop() + 20, LABEL, false);
        drawFitted(g, "keys: " + String.join(" ", HorseQuery.keys()),
                l + countW + 14, contentTop() + 20, r - l - countW - 80, TAG);

        // Headings - clickable, and the sorted one carries the direction.
        g.fill(l - 2, tableHeadY() - 2, r + 2, tableHeadY() + ROW_H, HEAD_BG);
        for (int i = 0; i < COLUMNS.size(); i++) {
            Column column = COLUMNS.get(i);
            boolean on = column.sort() == sort;
            String head = column.sort().label() + (on ? (sortDescending ? " v" : " ^") : "");
            drawFitted(g, head, columnX(i), tableHeadY() + 1, columnW(i), on ? NAME : LABEL);
        }

        g.fill(l - 2, top - 2, r + 2, bottom + 2, PANEL_SOFT);
        g.enableScissor(l - 2, top, r + 2, bottom);
        for (int i = horseScroll; i < horseRows.size() && i < horseScroll + horseVisibleRows(); i++) {
            HorseListing row = horseRows.get(i);
            int ry = top + (i - horseScroll) * ROW_H;
            boolean sel = row.id().equals(selectedHorseId);
            boolean hover = mouseX >= l && mouseX <= r && mouseY >= ry && mouseY < ry + ROW_H;
            if (sel) {
                g.fill(l - 2, ry, r, ry + ROW_H, ROW_SEL);
            } else if (hover) {
                g.fill(l - 2, ry, r, ry + ROW_H, ROW_HOVER);
            } else if ((i & 1) == 1) {
                // Zebra striping: thirteen columns of small text need the eye
                // held on one row, and a per-row rule would be heavier than
                // the rows themselves.
                g.fill(l - 2, ry, r, ry + ROW_H, DIVIDER);
            }
            drawHorseRow(g, row, ry + 2, sel);
        }
        g.disableScissor();

        if (horseRows.isEmpty()) {
            String note = !ClientHorseRoster.received() ? "asking the server..."
                    : total == 0 ? "no horses on record yet - tame or breed one"
                    : "nothing matches \"" + horseFilter + "\"";
            g.text(this.font, Component.literal(note), l + 4, top + 4, EXPR_OFF, false);
        }

        int max = Math.max(0, horseRows.size() - horseVisibleRows());
        if (max > 0) {
            int trackH = bottom - top;
            g.fill(r - 1, top, r + 2, bottom, 0x33FFFFFF);
            int thumbH = Math.max(16, trackH * horseVisibleRows() / horseRows.size());
            int thumbY = top + (trackH - thumbH) * horseScroll / max;
            g.fill(r - 1, thumbY, r + 2, thumbY + thumbH, 0xAAFFFFFF);
        }

        drawHorseFooter(g, l, r, bottom + 6);
    }

    private void drawHorseRow(GuiGraphicsExtractor g, HorseListing row, int y, boolean sel) {
        int plain = sel ? NAME : NAME_DIM;
        String[] cells = {
                row.displayName(),
                row.barnName().isEmpty() ? "-" : row.barnName(),
                row.sexLabel(),
                row.ageLabel(),
                row.breed(),
                Integer.toString(row.generation()),
                row.coat(),
                String.format("%.3f", row.speed()),
                String.format("%.1f", row.health()),
                String.format("%.2f", row.jump()),
                String.format("%.2f", row.scale()),
                row.bond() < 0 ? "?" : Integer.toString(row.bond()),
                row.loaded() ? row.where() : "not loaded"
        };
        int[] colours = {
                row.lethal() ? BAD : plain,
                plain,
                plain,
                plain,
                plain,
                plain,
                plain,
                statColour(row.speed(), HorseTraits.BASE_SPEED, plain),
                statColour(row.health(), HorseTraits.BASE_HEALTH, plain),
                statColour(row.jump(), HorseTraits.BASE_JUMP, plain),
                plain,
                row.bond() < 0 ? EXPR_OFF : plain,
                row.loaded() ? plain : EXPR_OFF
        };
        for (int i = 0; i < COLUMNS.size(); i++) {
            drawFitted(g, cells[i], columnX(i), y, columnW(i), colours[i]);
        }
    }

    /** Green above the baseline horse, red below - the same rule as the info screen. */
    private static int statColour(double actual, double baseline, int plain) {
        if (actual > baseline + 1e-6) {
            return GOOD;
        }
        return actual < baseline - 1e-6 ? BAD : plain;
    }

    /**
     * Two lines under the table: what the selected horse is, in the words the
     * columns had no room for, or how to drive the tab when nothing is picked.
     */
    private void drawHorseFooter(GuiGraphicsExtractor g, int l, int r, int y) {
        HorseListing row = ClientHorseRoster.byId(selectedHorseId);
        int w = r - l;
        int second = y + this.font.lineHeight + 2;
        if (row == null) {
            drawFitted(g, "click a heading to sort, a row to read it; terms are ANDed and "
                            + "-term excludes, e.g. \"mare gen>2 gene:SB1 -lethal\"",
                    l + 2, y, w, TAG);
            if (ClientHorseRoster.truncated()) {
                drawFitted(g, "You own more horses than the roster holds - it is showing the "
                        + "most recent generations.", l + 2, second, w, EXPR_OFF);
            }
            return;
        }
        StringBuilder head = new StringBuilder(row.displayName());
        if (!row.barnName().isEmpty()) {
            head.append(" (\"").append(row.barnName()).append("\")");
        }
        head.append("  -  ").append(row.sexLabel().toLowerCase(Locale.ROOT))
                .append(", ").append(row.breed())
                .append(", generation ").append(row.generation())
                .append("  -  ").append(row.coat());
        drawFitted(g, head.toString(), l + 2, y, w, NAME);

        StringBuilder tail = new StringBuilder();
        tail.append("bond ").append(row.bond() < 0 ? "unknown" : row.bond());
        if (row.inHerd()) {
            tail.append("  - in a herd");
        }
        if (!row.tamedBy().isEmpty()) {
            tail.append("  - tamed by ").append(row.tamedBy());
        }
        if (!row.bredBy().isEmpty()) {
            tail.append("  - bred by ").append(row.bredBy());
        }
        tail.append("  - ").append(row.loaded() ? row.where() : "not loaded right now");
        if (!row.conditions().isEmpty()) {
            tail.append("  - ").append(String.join(", ", row.conditions()));
        }
        drawFitted(g, tail.toString(), l + 2, second, w, row.lethal() ? BAD : DESC);
    }

    // ------------------------------------------------------------------
    // Breeding preview
    // ------------------------------------------------------------------
    //
    // Two pickers stacked down the left - mares above, stallions below - and
    // the Punnett squares down the right, grouped by the trait the loci pull
    // on (BreedingPreview.groups). It answers "can this pair throw that allele,
    // and how often", which is the question a beginner actually has, and
    // deliberately not "what will the foal look like": a predicted coat is a
    // promise the draw does not make.

    /** The y at which the mare list ends and the stallion list begins. */
    private int pickerSplit() {
        return listTop() + (contentBottom() - listTop()) / 2;
    }

    private int pickerTop(boolean mares) {
        return (mares ? listTop() : pickerSplit()) + 12;
    }

    private int pickerBottom(boolean mares) {
        return mares ? pickerSplit() - 6 : contentBottom();
    }

    private int pickerRows(boolean mares) {
        return Math.max(1, (pickerBottom(mares) - pickerTop(mares)) / ROW_H);
    }

    private int pickerScroll(boolean mares) {
        return mares ? mareScroll : stallionScroll;
    }

    /** The roster row under the cursor in one of the two pickers, or null. */
    private HorseListing horseAt(double mx, double my, Sex sex) {
        boolean mares = sex == Sex.FEMALE;
        if (mx < listX() || mx > listX() + listW()
                || my < pickerTop(mares) || my >= pickerTop(mares) + pickerRows(mares) * ROW_H) {
            return null;
        }
        List<HorseListing> list = ClientHorseRoster.of(sex);
        int i = pickerScroll(mares) + (int) ((my - pickerTop(mares)) / ROW_H);
        return i >= 0 && i < list.size() ? list.get(i) : null;
    }

    /**
     * Recompute the squares. Only on a selection or toggle change: the walk
     * touches every registered gene and probes the trait system once per
     * possible outcome, which is nothing at all once and far too much per frame.
     */
    private void rebuildPreview() {
        detailScroll = 0f;
        HorseListing dam = ClientHorseRoster.byId(damId);
        HorseListing sire = ClientHorseRoster.byId(sireId);
        previewRosterVersion = ClientHorseRoster.version();
        if (dam == null || sire == null) {
            preview = List.of();
            return;
        }
        try {
            // Which one is the dam is read off the genotypes rather than off
            // the list the player clicked: the sex loci are the authority, and
            // a sex-linked square comes out wrong rather than mirrored if the
            // two disagree.
            boolean firstIsDam = BreedingPreview.isDam(dam.genotype());
            preview = BreedingPreview.groups(
                    firstIsDam ? dam.genotype() : sire.genotype(),
                    firstIsDam ? sire.genotype() : dam.genotype(),
                    showSettled);
        } catch (RuntimeException unusable) {
            preview = List.of();
        }
    }

    private void drawBreedingPreview(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (previewRosterVersion != ClientHorseRoster.version()) {
            rebuildPreview();
        }
        drawPicker(g, mouseX, mouseY, Sex.FEMALE);
        drawPicker(g, mouseX, mouseY, Sex.MALE);
        drawPreviewPane(g);
    }

    private void drawPicker(GuiGraphicsExtractor g, int mouseX, int mouseY, Sex sex) {
        boolean mares = sex == Sex.FEMALE;
        List<HorseListing> list = ClientHorseRoster.of(sex);
        int l = listX();
        int w = listW();
        int headY = mares ? listTop() : pickerSplit();
        int top = pickerTop(mares);
        int bottom = pickerBottom(mares);
        UUID chosen = mares ? damId : sireId;

        g.text(this.font, Component.literal((mares ? "Mares" : "Stallions") + "  (" + list.size() + ")"),
                l, headY, LABEL, false);

        g.fill(l - 2, top - 2, l + w + 2, bottom + 2, PANEL_SOFT);
        g.enableScissor(l, top, l + w, bottom);
        int visible = pickerRows(mares);
        int scroll = pickerScroll(mares);
        for (int i = scroll; i < list.size() && i < scroll + visible; i++) {
            HorseListing horse = list.get(i);
            int ry = top + (i - scroll) * ROW_H;
            boolean sel = horse.id().equals(chosen);
            boolean hover = mouseX >= l && mouseX <= l + w && mouseY >= ry && mouseY < ry + ROW_H;
            if (sel) {
                g.fill(l - 2, ry, l + w, ry + ROW_H, ROW_SEL);
            } else if (hover) {
                g.fill(l - 2, ry, l + w, ry + ROW_H, ROW_HOVER);
            }
            drawFitted(g, horse.displayName() + "  g" + horse.generation(), l + 4, ry + 2, w - 12,
                    sel ? NAME : NAME_DIM);
        }
        g.disableScissor();

        if (list.isEmpty()) {
            String note = !ClientHorseRoster.received()
                    ? "asking the server..."
                    : "no tamed " + (mares ? "mares" : "stallions") + " on record";
            g.text(this.font, Component.literal(note), l + 4, top + 2, EXPR_OFF, false);
        }

        int max = Math.max(0, list.size() - visible);
        if (max > 0) {
            int trackH = bottom - top;
            int x1 = l + w;
            g.fill(x1 - 3, top, x1, bottom, 0x33FFFFFF);
            int thumbH = Math.max(16, trackH * visible / list.size());
            int thumbY = top + (trackH - thumbH) * scroll / max;
            g.fill(x1 - 3, thumbY, x1, thumbY + thumbH, 0xAAFFFFFF);
        }
    }

    private void drawPreviewPane(GuiGraphicsExtractor g) {
        int l = detailX();
        int r = detailR();
        int top = contentTop();
        int bottom = contentBottom();
        int w = r - l;
        g.fill(l - 6, top - 2, r + 2, bottom + 2, PANEL_SOFT);

        HorseListing dam = ClientHorseRoster.byId(damId);
        HorseListing sire = ClientHorseRoster.byId(sireId);
        int lineH = this.font.lineHeight + 2;

        if (dam == null || sire == null) {
            int y = top + 4;
            g.text(this.font, Component.literal("Pick a mare and a stallion"), l, y, HEADING, false);
            y += lineH + 4;
            for (String line : GuiText.wrap(this.font,
                    "This shows what the pair could produce at every locus, and how often - not a "
                            + "foal, and not a genotype. It answers whether two horses could throw a "
                            + "given allele, which is the only question a Punnett square can honestly "
                            + "answer about a whole horse.", w)) {
                g.text(this.font, Component.literal(line), l, y, DESC, false);
                y += lineH;
            }
            if (ClientHorseRoster.truncated()) {
                y += lineH;
                for (String line : GuiText.wrap(this.font,
                        "You own more horses than this list holds - it is showing the most recent "
                                + "generations.", w)) {
                    g.text(this.font, Component.literal(line), l, y, TAG, false);
                    y += lineH;
                }
            }
            detailMaxScroll = 0f;
            return;
        }

        g.enableScissor(l - 4, top, r, bottom);
        int y = top - (int) detailScroll;
        int startY = y;

        drawFitted(g, dam.displayName() + "   x   " + sire.displayName(), l, y, w, HEADING);
        y += lineH;
        drawFitted(g, dam.breed() + " mare  x  " + sire.breed() + " stallion", l, y, w, TAG);
        y += lineH + 4;

        if (preview.isEmpty()) {
            g.text(this.font, Component.literal(showSettled
                            ? "Nothing to show - those genotypes would not resolve."
                            : "Every locus is settled: this pair can only produce one thing."),
                    l, y, EXPR_OFF, false);
            y += lineH;
        }

        for (BreedingPreview.Group group : preview) {
            g.text(this.font, Component.literal(group.label()), l, y, HEADING, false);
            y += lineH;
            for (String line : GuiText.wrap(this.font, group.note(), w)) {
                g.text(this.font, Component.literal(line), l, y, TAG, false);
                y += lineH;
            }
            y += 2;
            for (BreedingPreview.Locus locus : group.loci()) {
                int indent = BreedingPreview.isModifier(locus.gene()) ? 10 : 0;
                drawFitted(g, locus.gene().name(), l + indent, y, w - indent - 130, NAME);
                drawFitted(g, locus.dam().toTokens() + "  x  " + locus.sire().toTokens(),
                        l + w - 126, y, 126, ALLELE_TOK);
                y += lineH;
                for (BreedingPreview.Outcome outcome : locus.outcomes()) {
                    g.text(this.font, Component.literal(percent(outcome.chance())),
                            l + indent + 12, y, DESC, false);
                    g.text(this.font, Component.literal(outcome.pair().toTokens()),
                            l + indent + 48, y, ALLELE_TOK, false);
                    drawFitted(g, outcome.expression().name(), l + indent + 120, y,
                            w - indent - 120, outcome.expressing() ? EXPR_ON : EXPR_OFF);
                    y += lineH;
                }
                y += 3;
            }
            y += 4;
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

    /**
     * A probability as the thing a breeder says out loud. Quarters and halves
     * keep their fraction, because one in four is what a carrier pairing
     * literally is and 25% is a translation of it; everything else rounds to a
     * percent.
     */
    private static String percent(double chance) {
        if (Math.abs(chance - 0.25) < 1e-9) return "1 in 4";
        if (Math.abs(chance - 0.5) < 1e-9) return "1 in 2";
        if (Math.abs(chance - 0.75) < 1e-9) return "3 in 4";
        if (Math.abs(chance - 1.0) < 1e-9) return "always";
        return Math.round(chance * 100) + "%";
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
