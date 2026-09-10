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
import com.example.horsegenetics.common.progress.ProgressTask;
import com.example.horsegenetics.neoforge.ClientConfig;
import com.example.horsegenetics.neoforge.item.ModItems;
import com.example.horsegenetics.neoforge.menu.SpliceRecipeDisplay;
import com.example.horsegenetics.neoforge.network.HorseRosterRequestPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.DoubleConsumer;

/**
 * The <b>Horse Browser</b> - opened with the browser key (default <kbd>H</kbd>).
 * A plain {@link Screen} with four tabs drawn from a strip at the top of the
 * window. <b>It holds nothing and crafts nothing</b> - see the Recipes note
 * below.
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
 *   <li><b>Recipes</b> - a <b>reference</b>: every recipe this mod adds, drawn
 *       as its grid and its result, plus one entry per gene for the
 *       gene-parameterised splice carrot. You read it here and you craft it at
 *       a crafting table.</li>
 * </ul>
 *
 * <h2>Why there is no crafting here any more</h2>
 * There used to be a real 3x3 grid, a result slot and the player's inventory on
 * that last tab, which made this an {@code AbstractContainerScreen} over a
 * server-synced menu. It was removed on the owner's call, and the reason is
 * worth keeping: <i>"putting crafting in the H menu breaks the flow of normal
 * Minecraft so much it's confusing people"</i>. A window bound to a key, that
 * is not a block, that nonetheless crafts, is a fifth thing to learn for no
 * gain - every one of those recipes already works at a crafting table.
 *
 * <p>Losing the container is the point rather than a side effect: with no slots
 * there is nothing to drag, nothing to lose on close, and no server menu to keep
 * in step. What the grid could uniquely do - turn a book into a research paper
 * for a chosen gene - moved to the <b>Equine Research Shelf</b>, a real block in
 * the world.
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
public final class HorseBrowserScreen extends Screen {

    private enum Tab {
        GETTING_STARTED("Getting started"),
        MY_HORSES("My horses"),
        GENE_DATABASE("Gene database"),
        ALLELES("Alleles"),
        BREEDING_PREVIEW("Breeding preview"),
        RECIPES("Recipes");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    /**
     * <b>Which half of the Recipes tab you are looking at.</b> One row per
     * discovered gene means the splice carrots outnumber every other recipe in
     * the mod several times over, and they buried the 27 that are not gene
     * carrots. Two categories is all it needs, and the default is the small
     * half - the one you are looking for when you open the tab at all.
     */
    private enum RecipeCategory {
        OTHER("Everything else"),
        GENE_CARROTS("Gene carrots");

        final String label;

        RecipeCategory(String label) {
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
    private static final int TRACK = 0x33FFFFFF;
    private static final int THUMB = 0xAAFFFFFF;
    private static final int THUMB_HOT = 0xFFFFFFFF;

    /**
     * How far outside the drawn bar a click still counts as grabbing it. The
     * bars are three pixels wide because that is what looks right; three pixels
     * is not something anybody can reliably hit with a mouse, so the target is
     * quietly wider than the paint.
     */
    private static final int GRAB_PAD = 6;

    /**
     * ...and how far <i>inward</i>, which is much less. Every vertical bar here
     * sits on the right edge of a list, so the pad going right is over a gutter
     * and the pad going left is over the rows - and a list whose last few pixels
     * scroll instead of selecting is its own small bug.
     */
    private static final int GRAB_PAD_IN = 2;

    /** The Getting Started contents column, when there is room to pin one. */
    private static final int TOC_W = 150;

    /** The Recipes tab's detail panel, centred by {@link #panelLeft()}. */
    private static final int IMG_W = 262;
    private static final int IMG_H = 210;

    // Slot geometry inside that panel. These were HorseBrowserMenu's, back when
    // they positioned real slots; they now position drawn ones.
    private static final int GRID_X = 30;
    private static final int GRID_Y = 34;
    private static final int RESULT_X = 108;
    private static final int RESULT_Y = 52;
    private static final int TAB_TOP = 6;
    private static final int TAB_H = 18;
    private static final int ROW_H = 12;
    /**
     * The roster tables' row height. Taller than {@link #ROW_H} because these
     * rows carry a <b>horse</b> - see {@link HorsePortrait} - and a coat is not
     * readable in twelve pixels. The gene list keeps the short pitch: it is a
     * list of names and a taller row would only show fewer of them.
     */
    private static final int HORSE_ROW_H = 26;
    /** Square, at the left of a roster row. */
    private static final int PORTRAIT_W = 30;

    /**
     * <b>Everything below is static, and that is the feature.</b> The browser is
     * a fresh {@code Screen} every time the key is pressed, so anything held on
     * the instance is forgotten the moment you close it - you came back to the
     * top of the first tab, every time, however deep you had been reading.
     *
     * <p>Static means "for this session": the tab, the scroll positions, the
     * search boxes, the sort, the selections. {@link #forgetWorld()} drops the
     * parts that are about <i>this</i> world when the client disconnects, so a
     * horse UUID from one save never selects something in another.
     *
     * <p>The first tab is the exception, and only once - see {@link #initialTab}.
     */
    private static Tab tab;
    private static String search = "";
    private static String selectedKey = "";
    private static int listScroll = 0;
    private static float detailScroll = 0f;
    private static float tutorialScroll = 0f;
    private static float tutorialMaxScroll = 0f;
    /** The pinned contents column's own scroll - it can be taller than the window. */
    private static float tocScroll = 0f;
    private float tocMaxScroll = 0f;
    /** Right edge of the pinned contents column, or 0 when it is inline. */
    private int tocColumnRight = 0;
    /**
     * <b>Which section of Getting Started is open.</b> A step index, or
     * {@code steps + g} for the checklist's group {@code g} - see
     * {@link #sectionCount()}.
     *
     * <p>The tab used to be one article with a contents list that scrolled you
     * into it. It is a section at a time now, on the owner's call: fifteen
     * chapters and a thirty-item checklist end to end is a page you can only be
     * <i>somewhere in</i>, and picking a heading should answer that heading
     * rather than aim at it.
     */
    private static int tutorialSection = 0;
    private static float alleleScroll = 0f;
    private static float alleleMaxScroll = 0f;
    /** How far the tab strip is pushed left, in pixels. Only used when it overflows. */
    private static int tabScroll = 0;
    private float detailMaxScroll = 0f;

    // ------------------------------------------------------------------
    // Scrollbars
    // ------------------------------------------------------------------

    /**
     * <b>A scrollbar drawn this frame, and how to move it.</b>
     *
     * <p>Every bar on this screen is drawn where its own panel happens to be,
     * from numbers that panel already had - the track is a couple of
     * {@code fill} calls at the end of a draw method. That made them pictures of
     * the scroll position rather than a control of it: the wheel worked and the
     * bar did not, which is the one thing every player tries first.
     *
     * <p>Rather than hoist nine different layouts into a widget, each bar
     * <i>registers</i> itself as it paints - where its track is, how long its
     * thumb came out, and a setter for whichever field it is a picture of. The
     * list is rebuilt every frame and input reads the last frame's, which is
     * exactly the geometry the player is looking at when they click.
     */
    private record ScrollBar(boolean horizontal, int hitL, int hitT, int hitR, int hitB,
                             int trackStart, int trackLen, int thumbLen, float value, float max,
                             DoubleConsumer set) {

        boolean contains(double mx, double my) {
            return mx >= hitL && mx <= hitR && my >= hitT && my <= hitB;
        }

        /** Where the thumb starts, along the scroll axis. */
        int thumbAt() {
            return trackStart + (max <= 0f ? 0 : Math.round(value * (trackLen - thumbLen) / max));
        }

        /** The scroll value that would put the thumb's near edge at {@code pos}. */
        float valueFor(double pos) {
            int span = trackLen - thumbLen;
            if (span <= 0) {
                return 0f;
            }
            return (float) Math.max(0d, Math.min(max, (pos - trackStart) * max / span));
        }
    }

    /**
     * One clickable line of the Getting Started contents list. {@code index} is
     * into {@link TutorialPage#headings()}; one past the end is the checklist,
     * which is part of the same article but not part of the same page object.
     *
     * <p>The scroll target is worked out when it is clicked rather than when it
     * is drawn: the offsets it needs are measured by the draw that happens
     * <i>after</i> this one, and a target stored here would always be one
     * layout behind.
     */
    private record TocEntry(int x0, int y0, int x1, int y1, int index) {
    }

    private final List<TocEntry> tocEntries = new ArrayList<>();

    /** Rebuilt every frame by the draw methods; read by the mouse handlers. */
    private final List<ScrollBar> bars = new ArrayList<>();
    /** The bar being dragged, and where inside its thumb the pointer took hold. */
    private ScrollBar dragging;
    private int dragGrab;
    /** The last mouse position the screen drew with - the draw methods that hover a bar do not all take it. */
    private int lastMouseX;
    private int lastMouseY;

    /**
     * Draw a vertical bar between {@code xL} and {@code xR} and register it.
     * {@code thumbH} is the caller's, because a row list sizes its thumb by rows
     * and a scrolling article sizes it by pixels.
     */
    private void scrollBarV(GuiGraphicsExtractor g, int xL, int xR, int top, int bottom,
                            float value, float max, int thumbH, DoubleConsumer set) {
        if (max <= 0f) {
            return;
        }
        ScrollBar bar = new ScrollBar(false, xL - GRAB_PAD_IN, top, xR + GRAB_PAD, bottom,
                top, bottom - top, thumbH, Math.max(0f, Math.min(value, max)), max, set);
        g.fill(xL, top, xR, bottom, TRACK);
        int thumbY = bar.thumbAt();
        boolean hot = dragging != null ? sameBar(dragging, bar) : bar.contains(lastMouseX, lastMouseY);
        g.fill(xL, thumbY, xR, thumbY + thumbH, hot ? THUMB_HOT : THUMB);
        bars.add(bar);
    }

    /** The tab strip's bar - the only horizontal one. */
    private void scrollBarH(GuiGraphicsExtractor g, int left, int right, int y,
                            float value, float max, int thumbW, DoubleConsumer set) {
        if (max <= 0f) {
            return;
        }
        // No upward padding: the tabs themselves end where this begins, and a
        // grab zone reaching into them would eat the bottom of every tab.
        ScrollBar bar = new ScrollBar(true, left, y, right, y + 8,
                left, right - left, thumbW, Math.max(0f, Math.min(value, max)), max, set);
        g.fill(left, y, right, y + 2, TRACK);
        int thumbX = bar.thumbAt();
        boolean hot = dragging != null ? sameBar(dragging, bar) : bar.contains(lastMouseX, lastMouseY);
        g.fill(thumbX, y, thumbX + thumbW, y + 2, hot ? THUMB_HOT : THUMB);
        bars.add(bar);
    }

    /**
     * Is this the same bar as the one being dragged? The list is rebuilt every
     * frame, so identity is no use - the track is what identifies a bar, and a
     * drag that outlives a relayout is a drag on whatever is in that place now.
     */
    private static boolean sameBar(ScrollBar a, ScrollBar b) {
        return a.horizontal() == b.horizontal() && a.hitL() == b.hitL() && a.hitT() == b.hitT()
                && a.trackLen() == b.trackLen();
    }

    /** A press on a bar: grab the thumb, or jump to where the track was clicked. */
    private boolean grabScrollBar(double mx, double my) {
        for (ScrollBar bar : bars) {
            if (!bar.contains(mx, my)) {
                continue;
            }
            double pos = bar.horizontal() ? mx : my;
            int thumb = bar.thumbAt();
            if (pos < thumb || pos > thumb + bar.thumbLen()) {
                // Clicked the bare track: put the thumb under the pointer and
                // carry on as if it had been grabbed in the middle.
                dragGrab = bar.thumbLen() / 2;
                bar.set().accept(bar.valueFor(pos - dragGrab));
            } else {
                dragGrab = (int) Math.round(pos - thumb);
            }
            dragging = bar;
            return true;
        }
        return false;
    }

    private EditBox searchBox;
    private EditBox horseFilterBox;
    private Button recipeCategoryButton;
    private static RecipeCategory recipeCategory = RecipeCategory.OTHER;
    private boolean recipeMenuOpen;
    private Button refreshRosterButton;
    private Button settledToggle;

    // --- My horses tab ---
    private static String horseFilter = "";
    private static HorseQuery.Sort sort = HorseQuery.Sort.NAME;
    private static boolean sortDescending = false;
    private static int horseScroll = 0;
    private static UUID selectedHorseId;
    /** Recomputed when the filter, the sort or the roster changes - never per frame. */
    private List<HorseListing> horseRows = List.of();
    private int horseRowsVersion = -1;
    private String horseRowsQuery = null;

    // --- Breeding preview tab ---
    private static UUID damId;
    private static UUID sireId;
    private static boolean showSettled = false;
    private int mareScroll = 0;
    private int stallionScroll = 0;
    /** Recomputed only when the pair or the toggle changes - the walk is not free. */
    private List<BreedingPreview.Group> preview = List.of();
    private int previewRosterVersion = -1;
    private final List<Gene> allGenes;
    private List<Gene> filtered = List.of();

    public HorseBrowserScreen() {
        super(Component.translatable("gui.horsegenetics.horse_browser"));
        List<Gene> genes = new ArrayList<>(Genes.codeOrder());
        genes.sort(Comparator.comparing(Gene::name, String.CASE_INSENSITIVE_ORDER));
        this.allGenes = List.copyOf(genes);
        if (!allGenes.isEmpty()) {
            this.selectedKey = allGenes.get(0).key();
        }
    }

    /**
     * <b>Reading the browser does not pause the world.</b> A plain
     * {@link Screen} pauses singleplayer by default and an
     * {@code AbstractContainerScreen} does not - so dropping the container
     * would have quietly changed this, and a horse you were watching would stop
     * moving whenever you checked its gene. Kept as it was.
     */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * <b>Getting Started, the first time and only the first time.</b>
     * {@code tutorial.seen} is a client config, so "first" means the first world
     * on this installation rather than the first world ever - which is the
     * honest reading of "when you first start a new world" for a page that is
     * about the mod rather than about the save.
     *
     * <p>It is marked seen the moment the player <i>leaves</i> the tab, not the
     * moment they arrive: opening the browser and immediately closing it again
     * should not count as having read it.
     */
    private static Tab initialTab() {
        return ClientConfig.tutorialSeen() ? Tab.MY_HORSES : Tab.GETTING_STARTED;
    }

    /** Drop the parts of the remembered position that belong to one world. */
    public static void forgetWorld() {
        selectedHorseId = null;
        damId = null;
        sireId = null;
        horseScroll = 0;
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

    /** The Recipes tab's detail panel, pinned right of the list and centred vertically. */
    private int panelLeft() {
        return Math.max(listX() + listW() + 12, (this.width - IMG_W) / 2);
    }

    private int panelTop() {
        return contentTop();
    }

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

    /**
     * <b>Where the left-hand list actually stops.</b> On Recipes it is clipped
     * to the detail panel's bottom edge rather than the window's, and
     * {@link #visibleRows()} has to agree - counting rows against the window
     * while scissoring against the panel is what left the last row half drawn.
     */
    private int listBottomForTab() {
        return tab == Tab.RECIPES
                ? Math.min(contentBottom(), panelTop() + IMG_H)
                : contentBottom();
    }

    private int visibleRows() {
        return Math.max(1, (listBottomForTab() - listTop()) / ROW_H);
    }

    /** How many rows the active tab's left-hand list has. */
    private int listSize() {
        return tab == Tab.RECIPES ? recipeRows.size() : filtered.size();
    }

    private int maxListScroll() {
        return Math.max(0, listSize() - visibleRows());
    }

    // --- widgets ---

    @Override
    protected void init() {
        super.init();
        if (tab == null) {
            tab = initialTab();
        }

        searchBox = new EditBox(this.font, listX() + 1, contentTop(), listW() - 2, 16,
                Component.literal("Filter"));
        searchBox.setMaxLength(48);
        searchBox.setHint(Component.literal("filter by gene or allele"));
        searchBox.setValue(search);
        addRenderableWidget(searchBox);

        recipeCategoryButton = Button.builder(recipeCategoryLabel(), b -> recipeMenuOpen = !recipeMenuOpen)
                .bounds(listX(), contentTop() + 17, listW() - 2, 16)
                .build();
        recipeCategoryButton.visible = false;
        addRenderableWidget(recipeCategoryButton);

        horseFilterBox = new EditBox(this.font, fsLeft() + 1, contentTop(),
                Math.max(160, (fsRight() - fsLeft()) / 2), 16, Component.literal("Filter"));
        horseFilterBox.setMaxLength(96);
        horseFilterBox.setHint(Component.literal("mare  gen>2  gene:SB1  -lethal"));
        horseFilterBox.setValue(horseFilter);
        addRenderableWidget(horseFilterBox);

        refreshRosterButton = Button.builder(Component.literal("Refresh"), b -> requestRoster())
                .bounds(listX(), contentTop() - 1, buttonW("Refresh"), 18).build();
        refreshRosterButton.visible = false;
        addRenderableWidget(refreshRosterButton);

        settledToggle = Button.builder(settledLabel(), b -> {
                    showSettled = !showSettled;
                    b.setMessage(settledLabel());
                    rebuildPreview();
                })
                .bounds(listX() + 64, contentTop() - 1, buttonW(settledLabel().getString()), 18).build();
        settledToggle.visible = false;
        addRenderableWidget(settledToggle);

        applyFilter();
        if (tab == Tab.MY_HORSES && !ClientHorseRoster.received()) {
            requestRoster();
        }
    }

    private Component settledLabel() {
        return Component.literal(showSettled ? "All loci" : "Only what varies");
    }

    /**
     * A button exactly as wide as what is written on it. Every button on this
     * screen used to carry a hand-picked width, and every one of them was too
     * big for its label - which is invisible while the panels are painted
     * <em>over</em> the widgets and becomes a button sitting on top of the text
     * beside it the moment that is fixed. Sizing from the font means a
     * relabelled or translated button cannot go back to overlapping.
     */
    private int buttonW(String label) {
        return this.font.width(label) + 14;
    }

    private void requestRoster() {
        ClientPacketDistributor.sendToServer(HorseRosterRequestPayload.INSTANCE);
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
    private Component recipeCategoryLabel() {
        return Component.literal(recipeCategory.label + "  \u25be");
    }

    private void layoutGeneButtons() {
        if (recipeCategoryButton != null) {
            boolean show = tab == Tab.RECIPES;
            recipeCategoryButton.visible = show;
            recipeCategoryButton.active = show;
            recipeCategoryButton.setMessage(recipeCategoryLabel());
            recipeCategoryButton.setRectangle(listW() - 2, 16, listX(), contentTop() + 17);
            if (!show) {
                recipeMenuOpen = false;
            }
        }
        boolean breeding = tab == Tab.BREEDING_PREVIEW;
        boolean mine = tab == Tab.MY_HORSES;
        int refreshW = buttonW("Refresh");
        if (refreshRosterButton != null) {
            // Both roster tabs want it; it sits in the same place on each.
            boolean show = breeding || mine;
            refreshRosterButton.visible = show;
            refreshRosterButton.active = show;
            refreshRosterButton.setRectangle(refreshW, 18,
                    mine ? fsRight() - refreshW : listX(), contentTop() - 1);
        }
        if (settledToggle != null) {
            settledToggle.visible = breeding;
            settledToggle.active = breeding;
            // Clamped to the left column. Unclamped it ran past the picker and
            // over the Punnett pane's first line at narrow window widths, which
            // is the overlap that started all of this.
            int x = listX() + refreshW + 4;
            int w = Math.min(buttonW(settledLabel().getString()), listX() + listW() - x);
            settledToggle.setRectangle(Math.max(20, w), 18, x, contentTop() - 1);
            settledToggle.setMessage(settledLabel());
        }
        if (horseFilterBox != null) {
            horseFilterBox.visible = mine;
            horseFilterBox.active = mine;
            if (!mine) {
                horseFilterBox.setFocused(false);
            }
        }
    }

    /**
     * <b>A focused text box owns the keyboard.</b> Without this, typing
     * &ldquo;speed&rdquo; into the gene search closed the window and opened the
     * player's inventory on the <kbd>e</kbd>.
     *
     * <p>It is a vanilla trap rather than a mistake here.
     * {@link net.minecraft.client.gui.components.EditBox#keyPressed} handles the
     * <i>control</i> keys - backspace, the arrows, ctrl+A/C/V - and returns
     * {@code false} for an ordinary letter, because letters arrive separately
     * through {@code charTyped}. So the letter falls out of
     * {@code super.keyPressed}, and the next thing
     * {@link AbstractContainerScreen#keyPressed} does is test it against
     * {@code keyInventory} and call {@code onClose()}. Any screen with a text
     * field and a container behind it has this bug until it says otherwise.
     *
     * <p>The fix is to <b>swallow</b> every key that is not Escape while a box
     * has focus, after giving the box its go. Swallowing {@code keyPressed} does
     * not cost the letter: GLFW's character callback is a separate one, so
     * {@code charTyped} still delivers it and the text still types.
     */
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (super.keyPressed(event)) {
            return true;
        }
        if (event.key() != InputConstants.KEY_ESCAPE && typingInABox()) {
            return true;
        }
        return false;
    }

    /** Is a text field focused, i.e. is the player mid-word? */
    private boolean typingInABox() {
        return (searchBox != null && searchBox.isFocused() && searchBox.isActive())
                || (horseFilterBox != null && horseFilterBox.isFocused() && horseFilterBox.isActive());
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
        rebuildRecipes();
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
    }

    // --- input ---

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (recipeMenuClicked(event.x(), event.y())) {
            return true;
        }
        if (event.button() == 0) {
            // Before the lists and the tabs: a bar drawn over a panel takes the
            // click, which is what makes its couple of pixels of overhang
            // grabbable at all.
            if (grabScrollBar(event.x(), event.y())) {
                return true;
            }
            if (tab == Tab.GETTING_STARTED && contentsClicked(event.x(), event.y())) {
                return true;
            }
        }
        Tab hit = tabAt(event.x(), event.y());
        if (hit != null) {
            if (hit != tab) {
                if (tab == Tab.GETTING_STARTED) {
                    ClientConfig.markTutorialSeen();
                }
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
        if (tab == Tab.GETTING_STARTED || tab == Tab.ALLELES) {
            return super.mouseClicked(event, doubleClick); // neither has a list
        }
        int row = rowAt(event.x(), event.y());
        if (row >= 0) {
            if (tab == Tab.RECIPES) {
                selectedRecipe = row;
            } else {
                select(filtered.get(row).key());
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging != null) {
            double pos = dragging.horizontal() ? event.x() : event.y();
            dragging.set().accept(dragging.valueFor(pos - dragGrab));
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null && event.button() == 0) {
            dragging = null;
            return true;
        }
        return super.mouseReleased(event);
    }

    /** Every tab laid end to end, including the gap after the last one. */
    private int tabStripWidth() {
        int total = 0;
        for (Tab t : Tab.values()) {
            total += this.font.width(t.label) + 24 + 4;
        }
        return total;
    }

    /** The window the strip is drawn inside, with a margin at each end. */
    private int tabViewportLeft() {
        return 8;
    }

    private int tabViewportWidth() {
        return Math.max(40, this.width - 16);
    }

    /** How far the strip can be pushed left before its end is on screen. */
    private int maxTabScroll() {
        return Math.max(0, tabStripWidth() - tabViewportWidth());
    }

    /**
     * <b>Where the strip starts drawing.</b> Centred while it fits, which is how
     * it has always looked; scrolled once it does not.
     *
     * <p>There are six tabs now and they ran off the edge of the window - and a
     * tab you cannot see is a tab that does not exist, which is a bad way to
     * lose a feature.
     */
    private int tabStripLeft() {
        int total = tabStripWidth();
        if (total <= tabViewportWidth()) {
            tabScroll = 0;
            return this.width / 2 - total / 2;
        }
        tabScroll = Math.max(0, Math.min(tabScroll, maxTabScroll()));
        return tabViewportLeft() - tabScroll;
    }

    /** Is the pointer over the strip? Wheel there scrolls it. */
    private boolean overTabStrip(double mx, double my) {
        return my >= TAB_TOP && my <= TAB_TOP + TAB_H + 4
                && mx >= tabViewportLeft() && mx <= tabViewportLeft() + tabViewportWidth();
    }

    private Tab tabAt(double mx, double my) {
        if (my < TAB_TOP || my > TAB_TOP + TAB_H) {
            return null;
        }
        // Outside the viewport a tab is scrolled off, not clickable - otherwise
        // the half of a tab hanging past the edge would still take clicks.
        if (mx < tabViewportLeft() || mx > tabViewportLeft() + tabViewportWidth()) {
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
        return i >= 0 && i < listSize() ? i : -1;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (sy != 0 && overTabStrip(mx, my) && maxTabScroll() > 0) {
            tabScroll = Math.max(0, Math.min(tabScroll - (int) (sy * 24), maxTabScroll()));
            return true;
        }
        if (tab == Tab.GETTING_STARTED) {
            if (tocMaxScroll > 0f && tocColumnRight > 0 && mx <= tocColumnRight) {
                tocScroll = Math.max(0f, Math.min(tocScroll - (float) sy * 18f, tocMaxScroll));
                return true;
            }
            tutorialScroll = Math.max(0f, Math.min(tutorialScroll - (float) sy * 18f, tutorialMaxScroll));
            return true;
        }
        if (tab == Tab.ALLELES) {
            alleleScroll = Math.max(0f, Math.min(alleleScroll - (float) sy * 18f, alleleMaxScroll));
            return true;
        }
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

    // --- drawing ---

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(0, 0, this.width, this.height, DIM);
    }

    /**
     * Everything this screen draws, in screen coordinates and <b>before</b> the
     * {@code super} call that draws the widgets. See the class note on drawing
     * order: this screen paints near-opaque panels across the window, so
     * anything of ours that overlapped a widget would wash it out.
     */
    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        // The bars and the contents list are rebuilt from scratch every frame;
        // the mouse handlers read the last frame's, which is the geometry the
        // player is actually pointing at.
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        bars.clear();
        tocEntries.clear();
        drawChrome(g, mouseX, mouseY);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        if (tab == Tab.RECIPES) {
            drawRecipeMenu(g, mouseX, mouseY);
            if (!recipeMenuOpen) {
                recipeCellTooltip(g, mouseX, mouseY);
            }
        }
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
            boolean showSearch = tab == Tab.GENE_DATABASE || tab == Tab.RECIPES
                    || tab == Tab.ALLELES;
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
            case GETTING_STARTED -> drawGettingStarted(g, mouseX, mouseY);
            case ALLELES -> drawAlleles(g, mouseX, mouseY);
            case RECIPES -> drawRecipes(g, mouseX, mouseY);
        }
    }

    private void drawTabStrip(GuiGraphicsExtractor g) {
        int viewL = tabViewportLeft();
        int viewW = tabViewportWidth();
        int tx = tabStripLeft();

        // Scissored, so a tab scrolled half off is cut cleanly at the margin
        // rather than running under the window's edge.
        g.enableScissor(viewL, TAB_TOP, viewL + viewW, TAB_TOP + TAB_H);
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
        g.disableScissor();

        int max = maxTabScroll();
        if (max <= 0) {
            return;
        }
        // A bar under the strip, and only when it means something. It is the
        // only thing telling a player there are tabs off the side.
        int barY = TAB_TOP + TAB_H;
        int total = tabStripWidth();
        scrollBarH(g, viewL, viewL + viewW, barY, tabScroll, max,
                Math.max(20, viewW * viewW / total), v -> tabScroll = (int) Math.round(v));
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
            scrollBarV(g, l + w - 3, l + w, top, bottom, listScroll, max,
                    Math.max(16, (bottom - top) * visibleRows() / filtered.size()),
                    v -> listScroll = (int) Math.round(v));
        }
    }

    private void drawGeneDetail(GuiGraphicsExtractor g) {
        Gene gene = selected();
        int l = detailX();
        int r = detailR();
        int top = contentTop();
        int bottom = contentBottom();
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
            scrollBarV(g, r - 2, r + 1, top, bottom, detailScroll, detailMaxScroll,
                    Math.max(16, (int) ((long) trackH * trackH / contentH)),
                    v -> detailScroll = (float) v);
        }
    }

    // ------------------------------------------------------------------
    // Recipes - a reference, not a workbench
    // ------------------------------------------------------------------
    //
    // Every recipe this mod adds, drawn as its grid and its result. You read it
    // here and you craft it at a crafting table; see the class note for why
    // there is no grid on this screen any more.
    //
    // The static recipes come from RecipeReference, which is generated from the
    // real recipe files. The gene-splice carrot is the one recipe that cannot
    // be: its ingredients depend on which gene, so it contributes one row per
    // gene the player has discovered, built by SpliceRecipeDisplay - the same
    // class the old ghost grid used.

    /** One row of the reference: what it makes, and what it takes. */
    private record RecipeRow(String label, List<ItemStack> grid, ItemStack result,
                             boolean shapeless, String note, boolean geneCarrot, String blurb) {
    }

    /**
     * <b>What the thing you are about to make is for.</b> One line per recipe,
     * out of the lang file under {@code recipe.horsegenetics.&lt;id&gt;.desc} -
     * so it is translatable, and so a recipe that converts <i>between</i> two
     * items can say which direction it goes (unpacking a bundle and making one
     * share an output but not a purpose).
     *
     * <p>A recipe with no entry gets an empty line rather than a raw key on
     * screen, which is what an untranslated {@code Component.translatable}
     * would show.
     */
    private String recipeBlurb(String recipeId) {
        String key = "recipe.horsegenetics." + recipeId + ".desc";
        String text = Component.translatable(key).getString();
        return text.equals(key) ? "" : text;
    }

    private List<RecipeRow> recipeRows = List.of();

    private static int selectedRecipe = 0;

    /**
     * Rebuild the reference list against the current search and the player's
     * discoveries. Called from {@link #applyFilter()}, so it tracks the search
     * box; item stacks are resolved here rather than cached across screens
     * because the registry can change between worlds.
     */
    private void rebuildRecipes() {
        List<RecipeRow> rows = new ArrayList<>();
        for (RecipeReference.Entry e : RecipeReference.all()) {
            if (e.kind() == RecipeReference.Kind.CUSTOM) {
                continue; // described below, where their inputs are known
            }
            ItemStack result = e.resultStack();
            if (result.isEmpty()) {
                continue; // an id this build does not have - say nothing rather than draw a hole
            }
            rows.add(new RecipeRow(result.getHoverName().getString(), e.gridStacks(), result,
                    e.kind() == RecipeReference.Kind.SHAPELESS,
                    e.kind() == RecipeReference.Kind.SHAPELESS
                            ? "Shapeless - the arrangement does not matter."
                            : "Shaped - lay it out exactly like this.",
                    false, recipeBlurb(e.id())));
        }
        // Combining two breeding carrots: a CustomRecipe, so it has no fixed
        // ingredients to draw. Said in words instead of drawn wrongly.
        rows.add(new RecipeRow("Combine breeding carrots", blankGrid(),
                new ItemStack(ModItems.MAGNIFIER_CARROT.get()), true,
                "Shapeless - the arrangement does not matter.",
                false,
                "Any two breeding carrots together make one carrot carrying both their "
                        + "effects. What comes out depends on what you put in."));
        // One row per gene, for the parameterised splice.
        for (Gene gene : allGenes) {
            if (!craftable(gene)) {
                continue;
            }
            List<ItemStack> grid = SpliceRecipeDisplay.forGene(gene);
            // Formulaic on purpose: there are as many of these as there are
            // genes, and the only thing that changes between them is the gene.
            rows.add(new RecipeRow(gene.name() + " splice carrot", grid,
                    new ItemStack(ModItems.KNOWN_GENE_SPLICE_CARROT.get()), true,
                    "Needs that gene's research paper. The rarity ingot is set by "
                            + "the gene's own rarity, so a rarer gene costs more.",
                    true,
                    "Feed a parent to aim its contribution at " + gene.name()
                            + ", instead of the coin flip it would otherwise be."));
        }
        // Category first, then the search box - so searching inside "Gene
        // carrots" stays inside it rather than quietly showing you the rest.
        boolean wantCarrots = recipeCategory == RecipeCategory.GENE_CARROTS;
        List<RecipeRow> inCategory = new ArrayList<>();
        for (RecipeRow row : rows) {
            if (row.geneCarrot() == wantCarrots) {
                inCategory.add(row);
            }
        }
        rows = inCategory;

        String q = search.trim().toLowerCase(Locale.ROOT);
        if (!q.isEmpty()) {
            List<RecipeRow> hits = new ArrayList<>();
            for (RecipeRow row : rows) {
                if (row.label().toLowerCase(Locale.ROOT).contains(q)) {
                    hits.add(row);
                }
            }
            rows = hits;
        }
        recipeRows = List.copyOf(rows);
        selectedRecipe = Math.max(0, Math.min(selectedRecipe, recipeRows.size() - 1));
    }

    private static List<ItemStack> blankGrid() {
        List<ItemStack> out = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            out.add(ItemStack.EMPTY);
        }
        return out;
    }

    private void drawRecipes(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        drawRecipeList(g, mouseX, mouseY);

        int px = panelLeft();
        int py = panelTop();
        g.fill(px, py, px + IMG_W, py + IMG_H, PANEL);
        g.fill(px, py, px + IMG_W, py + 1, BORDER);
        g.fill(px, py + IMG_H - 1, px + IMG_W, py + IMG_H, BORDER);
        g.fill(px, py, px + 1, py + IMG_H, BORDER);
        g.fill(px + IMG_W - 1, py, px + IMG_W, py + IMG_H, BORDER);

        if (recipeRows.isEmpty()) {
            g.text(this.font, Component.literal("No recipes match \"" + search + "\"."),
                    px + 10, py + 12, NAME_DIM, false);
            return;
        }
        RecipeRow row = recipeRows.get(selectedRecipe);

        drawFitted(g, row.label(), px + 10, py + 10, IMG_W - 20, HEADING);

        // the grid, its arrow, and the result
        for (int i = 0; i < 9; i++) {
            int gx = px + GRID_X + (i % 3) * 18;
            int gy = py + GRID_Y + (i / 3) * 18;
            cell(g, gx, gy, SLOT_BG);
            ItemStack stack = i < row.grid().size() ? row.grid().get(i) : ItemStack.EMPTY;
            if (!stack.isEmpty()) {
                g.fakeItem(stack, gx, gy);
            }
        }
        cell(g, px + RESULT_X, py + RESULT_Y, SLOT_RESULT_BG);
        if (!row.result().isEmpty()) {
            g.fakeItem(row.result(), px + RESULT_X, py + RESULT_Y);
        }
        g.fill(px + RESULT_X - 22, py + RESULT_Y + 7, px + RESULT_X - 6, py + RESULT_Y + 9, 0xFF6A6A78);

        int ty = py + GRID_Y + 3 * 18 + 10;
        int tw = IMG_W - 20;
        // What it is for, first - it is the thing you came to the tab to learn.
        if (!row.blurb().isEmpty()) {
            for (String line : GuiText.wrap(this.font, row.blurb(), tw)) {
                g.text(this.font, Component.literal(line), px + 10, ty, NAME, false);
                ty += this.font.lineHeight + 1;
            }
            ty += 4;
        }
        for (String line : GuiText.wrap(this.font, row.note(), tw)) {
            g.text(this.font, Component.literal(line), px + 10, ty, DESC, false);
            ty += this.font.lineHeight + 1;
        }
        ty += 4;
        // The one line this whole tab exists to say.
        for (String line : GuiText.wrap(this.font,
                "Craft this at a crafting table - this window is a reference.", tw)) {
            g.text(this.font, Component.literal(line), px + 10, ty, LABEL, false);
            ty += this.font.lineHeight + 1;
        }
    }

    /**
     * The whole window, as one scrolling article. It is the only tab with no
     * list down the left: it is meant to be read, and a 30% column to read it in
     * would be a worse page for no gain.
     */
    private void drawGettingStarted(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int l = listX();
        int r = fsRight();
        int top = contentTop();
        int bottom = contentBottom();

        g.fill(l - 6, top - 2, r + 2, bottom + 2, PANEL_SOFT);

        // The article is a long read and the prose is capped at a readable
        // measure, so on any normal window there is room beside it for the
        // contents to stay put. Pinned is the better list - a place to come back
        // to, rather than something you have to scroll to the top to use - but
        // it is only worth having if the prose is still readable once it has
        // taken its column, so a narrow window gets the list inline instead.
        boolean pinned = r - l >= TOC_W + 260;
        int textL = pinned ? l + TOC_W + 14 : l;
        int w = Math.min(r - textL, 420); // a readable measure, not the whole monitor
        tocColumnRight = pinned ? l + TOC_W : 0;

        if (pinned) {
            drawContents(g, l, top, TOC_W - 8, bottom, mouseX, mouseY, true);
            g.fill(l + TOC_W + 8, top, l + TOC_W + 9, bottom, DIVIDER);
        }

        g.enableScissor(textL - 4, top, r, bottom);
        int y = top - (int) tutorialScroll;
        int height = pinned ? 0 : drawContents(g, textL, y, w, bottom, mouseX, mouseY, false);
        height += drawSection(g, textL, y + height, w, mouseX, mouseY);
        g.disableScissor();

        tutorialMaxScroll = Math.max(0f, height - (bottom - top));
        tutorialScroll = Math.max(0f, Math.min(tutorialScroll, tutorialMaxScroll));
        if (tutorialMaxScroll > 0) {
            int trackH = bottom - top;
            scrollBarV(g, r - 2, r + 1, top, bottom, tutorialScroll, tutorialMaxScroll,
                    Math.max(16, (int) ((long) trackH * trackH / height)),
                    v -> tutorialScroll = (float) v);
        }
    }

    /** Every section the contents list offers: the prose steps, then the checklist by group. */
    private static int sectionCount() {
        return TutorialPage.headings().size() + ProgressTask.Group.values().length;
    }

    /**
     * <b>The open section, and nothing else.</b> One chapter of the tutorial, or
     * one group of the checklist under the running total.
     */
    private int drawSection(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY) {
        List<TutorialPage.Step> steps = TutorialPage.steps();
        int section = Math.max(0, Math.min(tutorialSection, sectionCount() - 1));
        if (section < steps.size()) {
            int h = TutorialPage.drawStep(g, this.font, steps.get(section), x, y, w,
                    mouseX, mouseY, HEADING, DESC);
            h += drawSectionFooter(g, x, y + h + 8, w, mouseX, mouseY, section) + 8;
            return h;
        }
        ProgressTask.Group group = ProgressTask.Group.values()[section - steps.size()];
        int h = drawChecklist(g, x, y, w, group);
        h += drawSectionFooter(g, x, y + h + 8, w, mouseX, mouseY, section) + 8;
        return h;
    }

    /**
     * <b>The way on.</b> A sectioned page still has to be readable straight
     * through - somebody meeting the mod reads it in order, and hunting the next
     * chapter in the contents list every time would be a worse walkthrough than
     * the scroll it replaced. So the last line of every section is the next one,
     * and the last section of all signs off instead.
     */
    private int drawSectionFooter(GuiGraphicsExtractor g, int x, int y, int w,
                                  int mouseX, int mouseY, int section) {
        if (section >= sectionCount() - 1) {
            // A last line, so the end of the page does not look like a cut.
            g.text(this.font, Component.literal("\u2014 good luck."), x, y, TAG, false);
            return this.font.lineHeight;
        }
        String label = "Next:  " + sectionLabel(section + 1) + "  \u203a";
        int wLine = Math.min(w, this.font.width(label) + 8);
        boolean hover = mouseX >= x - 3 && mouseX < x + wLine && mouseY >= y - 2
                && mouseY < y + this.font.lineHeight + 2;
        g.fill(x - 3, y - 3, x + wLine, y + this.font.lineHeight + 2, hover ? ROW_HOVER : DIVIDER);
        g.text(this.font, Component.literal(label), x, y, hover ? HEADING : NAME_DIM, false);
        // Registered with the contents list, so one hit test serves both.
        tocEntries.add(new TocEntry(x - 3, y - 3, x + wLine, y + this.font.lineHeight + 2, section + 1));
        return this.font.lineHeight + 4;
    }

    /** What the contents list calls a section. */
    private static String sectionLabel(int section) {
        List<String> headings = TutorialPage.headings();
        if (section < headings.size()) {
            return headings.get(section);
        }
        int g = section - headings.size();
        ProgressTask.Group[] groups = ProgressTask.Group.values();
        return g >= 0 && g < groups.length ? groups[g].title() : "";
    }

    /**
     * <b>The contents list.</b> Fifteen-odd sections of prose and then a
     * checklist is a lot of page to arrive at with only a mouse wheel, and the
     * tab reopens where you left it - so the one thing it was missing was a way
     * to say "the bit about the horseman" and be taken there.
     *
     * <p>Every line is the step's own heading, so the list needs no maintaining
     * as the page changes, and the section you are in is lit - which makes it a
     * position indicator as well as a control.
     *
     * @param pinned pinned in its own column, fixed and scrolled separately, or
     *               laid inline at the top of the article
     * @return the height it used - what the inline layout has to advance past
     */
    private int drawContents(GuiGraphicsExtractor g, int x, int y, int w, int bottom,
                             int mouseX, int mouseY, boolean pinned) {
        int steps = TutorialPage.headings().size();
        int lineH = this.font.lineHeight + 3;
        int active = Math.max(0, Math.min(tutorialSection, sectionCount() - 1));

        if (pinned) {
            g.enableScissor(x - 3, y, x + w + 6, bottom);
        }
        int cy = pinned ? y - Math.round(tocScroll) : y;
        int start = cy;
        g.text(this.font, Component.literal("Contents"), x, cy, HEADING, false);
        cy += this.font.lineHeight + 4;

        for (int i = 0; i < sectionCount(); i++) {
            // The checklist's groups are sections in their own right - thirty
            // tasks under one heading is the same wall the prose was.
            boolean checklist = i >= steps;
            if (i == steps) {
                cy += 4;
                if (!pinned || (cy + lineH > y && cy < bottom)) {
                    g.text(this.font, Component.literal("Checklist"), x, cy, HEADING, false);
                }
                cy += this.font.lineHeight + 4;
            }
            int lx = checklist ? x + 8 : x;
            if (!pinned || (cy + lineH > y && cy < bottom)) {
                boolean hover = mouseX >= x - 3 && mouseX < x + w + 2
                        && mouseY >= cy - 1 && mouseY < cy + lineH - 2
                        && (!pinned || (mouseY >= y && mouseY < bottom));
                if (i == active) {
                    g.fill(x - 3, cy - 1, x + w + 2, cy + lineH - 2, ROW_SEL);
                } else if (hover) {
                    g.fill(x - 3, cy - 1, x + w + 2, cy + lineH - 2, ROW_HOVER);
                }
                drawFitted(g, sectionLabel(i), lx, cy, w - (lx - x),
                        i == active ? NAME : (hover ? HEADING : NAME_DIM));
                tocEntries.add(new TocEntry(x - 3, cy - 1, x + w + 2, cy + lineH - 2, i));
            }
            cy += lineH;
        }
        cy += 6;

        if (pinned) {
            g.disableScissor();
            int used = cy - start;
            tocMaxScroll = Math.max(0f, used - (bottom - y));
            tocScroll = Math.max(0f, Math.min(tocScroll, tocMaxScroll));
            int trackH = bottom - y;
            scrollBarV(g, x + w + 3, x + w + 6, y, bottom, tocScroll, tocMaxScroll,
                    Math.max(16, (int) ((long) trackH * trackH / Math.max(1, used))),
                    v -> tocScroll = (float) v);
        } else {
            tocMaxScroll = 0f;
            g.fill(x, cy - 5, x + w, cy - 4, DIVIDER);
        }
        return cy - start;
    }

    /** A click on a contents line, or on the next-section link: open that section. */
    private boolean contentsClicked(double mx, double my) {
        for (TocEntry e : tocEntries) {
            if (mx < e.x0() || mx > e.x1() || my < e.y0() || my > e.y1()) {
                continue;
            }
            if (e.index() != tutorialSection) {
                tutorialSection = e.index();
                tutorialScroll = 0f; // a new section is read from its own top
            }
            return true;
        }
        return false;
    }

    /**
     * <b>The checklist, under the prose it explains.</b> One page rather than
     * two tabs on purpose: somebody who has just read "take a gene off a horse"
     * should find the box for it directly underneath, not on a tab they have to
     * know to look at.
     *
     * <p>It gates nothing. A player who read the wiki and built a shelf on their
     * first day ticks two boxes at once and is not stopped - the moment a
     * checklist gates content it stops being advice and starts being homework.
     */
    private int drawChecklist(GuiGraphicsExtractor g, int x, int y, int w, ProgressTask.Group group) {
        int start = y;
        int total = ProgressTask.values().length;
        int done = ClientProgress.count();

        // The running total stays on every group's page: it is the one number
        // somebody opening the checklist wants, and it is not this group's.
        g.text(this.font, Component.literal("Checklist  " + done + " / " + total),
                x, y, LABEL, false);
        y += this.font.lineHeight + 2;
        // A bar, because "17 of 30" is a number and a bar is a feeling.
        int barW = Math.min(w, 260);
        g.fill(x, y, x + barW, y + 3, 0xFF2B2B36);
        if (total > 0 && done > 0) {
            g.fill(x, y, x + Math.max(1, barW * done / total), y + 3, GOOD);
        }
        y += 14;

        g.text(this.font, Component.literal(group.title()), x, y, HEADING, false);
        y += this.font.lineHeight + 6;
        for (ProgressTask task : ProgressTask.inGroup(group)) {
            boolean ticked = ClientProgress.isDone(task);
            g.text(this.font, Component.literal(ticked ? "\u2714" : "\u2610"),
                    x + 2, y, ticked ? GOOD : TAG, false);
            drawFitted(g, task.title(), x + 14, y, w - 16, ticked ? EXPR_ON : NAME_DIM);
            y += this.font.lineHeight + 1;
            // The hint is what a player looking at an empty box actually
            // wants; a ticked one no longer needs telling.
            if (!ticked) {
                for (String line : GuiText.wrap(this.font, task.hint(), w - 16)) {
                    g.text(this.font, Component.literal(line), x + 14, y, TAG, false);
                    y += this.font.lineHeight;
                }
            }
            y += 4;
        }
        return y - start;
    }

    /**
     * <b>Every allele in the mod, and whether you have met it.</b> A collection,
     * and the fog is the point: an allele you have never seen shows as its gene
     * and a row of question marks, so the tab is a map of what is still out
     * there rather than a spoiler for it.
     *
     * <p>It reads {@code ClientGeneDatabase.hasAllele}, which is fed by a set
     * that records <em>both copies at every locus of every horse you tame or
     * breed</em> - baseline alleles included. Discovering a <i>gene</i> is a
     * gameplay gate and deliberately harder; collecting an <i>allele</i> is just
     * a record of what you have laid eyes on.
     */
    private void drawAlleles(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int l = listX();
        int r = fsRight();
        // listTop(), not contentTop(): the search box lives at contentTop() and
        // was drawn straight over the first row.
        int top = listTop();
        int bottom = contentBottom();
        int w = r - l - 8;

        String q = search.trim().toLowerCase(Locale.ROOT);
        int have = 0;
        int total = 0;
        for (Gene gene : allGenes) {
            for (Allele a : gene.alleles()) {
                total++;
                if (ClientGeneDatabase.hasAllele(gene.key(), a.token())) {
                    have++;
                }
            }
        }

        g.text(this.font, Component.literal("Alleles collected  " + have + " / " + total),
                l, top - 12, LABEL, false);
        g.fill(l - 6, top - 2, r + 2, bottom + 2, PANEL_SOFT);
        g.enableScissor(l - 4, top, r, bottom);

        int y = top - (int) alleleScroll;
        int start = y;
        for (Gene gene : allGenes) {
            if (!q.isEmpty() && !gene.name().toLowerCase(Locale.ROOT).contains(q)
                    && !gene.key().toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            int mine = 0;
            for (Allele a : gene.alleles()) {
                if (ClientGeneDatabase.hasAllele(gene.key(), a.token())) {
                    mine++;
                }
            }
            drawFitted(g, gene.name() + "   " + mine + "/" + gene.alleles().size(),
                    l, y, w, mine == gene.alleles().size() ? EXPR_ON : NAME);
            y += this.font.lineHeight + 2;

            int cx = l + 10;
            for (Allele a : gene.alleles()) {
                boolean got = ClientGeneDatabase.hasAllele(gene.key(), a.token());
                String label = got ? a.label() : "??? (" + a.token().replaceAll(".", "?") + ")";
                int tw = this.font.width(label) + 8;
                if (cx + tw > l + w) {
                    cx = l + 10;
                    y += this.font.lineHeight + 3;
                }
                g.fill(cx - 3, y - 1, cx + tw - 5, y + this.font.lineHeight, got ? 0x3355A0E0 : 0x18FFFFFF);
                g.text(this.font, Component.literal(label), cx, y, got ? ALLELE_TOK : TAG, false);
                cx += tw;
            }
            y += this.font.lineHeight + 8;
        }
        g.disableScissor();

        int height = y - start;
        alleleMaxScroll = Math.max(0f, height - (bottom - top));
        alleleScroll = Math.max(0f, Math.min(alleleScroll, alleleMaxScroll));
        if (alleleMaxScroll > 0) {
            int trackH = bottom - top;
            scrollBarV(g, r - 2, r + 1, top, bottom, alleleScroll, alleleMaxScroll,
                    Math.max(16, (int) ((long) trackH * trackH / Math.max(1, height))),
                    v -> alleleScroll = (float) v);
        }
    }

    private void drawRecipeList(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int l = listX();
        int w = listW();
        int top = listTop();
        int bottom = listBottomForTab();

        g.text(this.font, Component.literal(recipeRows.size()
                        + (recipeRows.size() == 1 ? " recipe" : " recipes")),
                l, top - 12, LABEL, false);
        g.fill(l - 2, top - 2, l + w + 2, bottom + 2, PANEL_SOFT);
        g.enableScissor(l, top, l + w, bottom);
        int hovered = rowAt(mouseX, mouseY);
        for (int i = listScroll; i < recipeRows.size() && i < listScroll + visibleRows(); i++) {
            int ry = top + (i - listScroll) * ROW_H;
            boolean sel = i == selectedRecipe;
            if (sel) {
                g.fill(l - 2, ry, l + w, ry + ROW_H, ROW_SEL);
            } else if (i == hovered) {
                g.fill(l - 2, ry, l + w, ry + ROW_H, ROW_HOVER);
            }
            drawFitted(g, recipeRows.get(i).label(), l + 4, ry + 2, w - 12, sel ? NAME : NAME_DIM);
        }
        g.disableScissor();

        int max = maxListScroll();
        if (max > 0) {
            scrollBarV(g, l + w - 3, l + w, top, bottom, listScroll, max,
                    Math.max(16, (bottom - top) * visibleRows() / recipeRows.size()),
                    v -> listScroll = (int) Math.round(v));
        }
    }

    /**
     * The category menu, drawn <b>after</b> the widgets rather than with the
     * rest of the chrome - it has to sit over the button that opened it, and
     * chrome goes under.
     */
    private void drawRecipeMenu(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (!recipeMenuOpen) {
            return;
        }
        int x = listX();
        int w = listW() - 2;
        int y = contentTop() + 17 + 16;
        RecipeCategory[] all = RecipeCategory.values();
        g.fill(x - 1, y - 1, x + w + 1, y + all.length * 14 + 1, 0xF00E0E16);
        g.fill(x - 1, y - 1, x + w + 1, y, BORDER);
        for (int i = 0; i < all.length; i++) {
            int ry = y + i * 14;
            boolean hov = mouseX >= x && mouseX < x + w && mouseY >= ry && mouseY < ry + 14;
            if (all[i] == recipeCategory) {
                g.fill(x, ry, x + w, ry + 14, ROW_SEL);
            } else if (hov) {
                g.fill(x, ry, x + w, ry + 14, ROW_HOVER);
            }
            drawFitted(g, all[i].label, x + 5, ry + 3, w - 10,
                    all[i] == recipeCategory ? NAME : NAME_DIM);
        }
    }

    /** The menu eats the next click wherever it lands - open menus always do. */
    private boolean recipeMenuClicked(double mx, double my) {
        if (!recipeMenuOpen) {
            return false;
        }
        int x = listX();
        int w = listW() - 2;
        int y = contentTop() + 17 + 16;
        RecipeCategory[] all = RecipeCategory.values();
        recipeMenuOpen = false;
        if (mx >= x && mx < x + w && my >= y && my < y + all.length * 14) {
            RecipeCategory picked = all[(int) ((my - y) / 14)];
            if (picked != recipeCategory) {
                recipeCategory = picked;
                listScroll = 0;
                selectedRecipe = 0;
                applyFilter();
            }
        }
        return true;
    }

    /** A real item tooltip for whichever reference cell the mouse is over. */
    private void recipeCellTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (recipeRows.isEmpty()) {
            return;
        }
        RecipeRow row = recipeRows.get(selectedRecipe);
        int px = panelLeft();
        int py = panelTop();
        for (int i = 0; i < 9 && i < row.grid().size(); i++) {
            ItemStack stack = row.grid().get(i);
            if (stack.isEmpty()) {
                continue;
            }
            int gx = px + GRID_X + (i % 3) * 18;
            int gy = py + GRID_Y + (i / 3) * 18;
            if (mouseX >= gx && mouseX < gx + 16 && mouseY >= gy && mouseY < gy + 16) {
                g.setTooltipForNextFrame(this.font, stack, mouseX, mouseY);
                return;
            }
        }
        if (!row.result().isEmpty() && mouseX >= px + RESULT_X && mouseX < px + RESULT_X + 16
                && mouseY >= py + RESULT_Y && mouseY < py + RESULT_Y + 16) {
            g.setTooltipForNextFrame(this.font, row.result(), mouseX, mouseY);
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
            new Column(HorseQuery.Sort.NAME, 124),
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
    private static final int TOTAL_WEIGHT = 658;

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
        return Math.max(1, (tableBottom() - tableTop()) / HORSE_ROW_H);
    }

    private int columnX(int index) {
        int x = fsLeft() + PORTRAIT_W + 4;
        int avail = fsRight() - fsLeft() - PORTRAIT_W - 10;
        for (int i = 0; i < index; i++) {
            x += COLUMNS.get(i).weight() * avail / TOTAL_WEIGHT;
        }
        return x;
    }

    private int columnW(int index) {
        int avail = fsRight() - fsLeft() - PORTRAIT_W - 10;
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
                || my >= tableTop() + horseVisibleRows() * HORSE_ROW_H) {
            return null;
        }
        int i = horseScroll + (int) ((my - tableTop()) / HORSE_ROW_H);
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
        List<HorseListing> onScreen = new ArrayList<>();
        for (int i = horseScroll; i < horseRows.size() && i < horseScroll + horseVisibleRows(); i++) {
            HorseListing row = horseRows.get(i);
            onScreen.add(row);
            int ry = top + (i - horseScroll) * HORSE_ROW_H;
            boolean sel = row.id().equals(selectedHorseId);
            boolean hover = mouseX >= l && mouseX <= r && mouseY >= ry && mouseY < ry + HORSE_ROW_H;
            if (sel) {
                g.fill(l - 2, ry, r, ry + HORSE_ROW_H, ROW_SEL);
            } else if (hover) {
                g.fill(l - 2, ry, r, ry + HORSE_ROW_H, ROW_HOVER);
            } else if ((i & 1) == 1) {
                // Zebra striping: twelve columns of small text need the eye
                // held on one row, and a per-row rule would be heavier than
                // the rows themselves.
                g.fill(l - 2, ry, r, ry + HORSE_ROW_H, DIVIDER);
            }
            HorsePortrait.draw(g, ClientHorseCoats.get(row.id()), !row.adult(),
                    l, ry + 1, PORTRAIT_W, HORSE_ROW_H - 2, mouseX, mouseY);
            drawHorseRow(g, row, ry + (HORSE_ROW_H - this.font.lineHeight) / 2, sel);
        }
        g.disableScissor();
        // Only the rows actually on screen, and only once each - see
        // ClientHorseCoats. Asking for the whole stable up front is what the
        // roster deliberately does not do.
        ClientHorseCoats.request(onScreen);

        if (horseRows.isEmpty()) {
            String note = !ClientHorseRoster.received() ? "asking the server..."
                    : total == 0 ? "no horses on record yet - tame or breed one"
                    : "nothing matches \"" + horseFilter + "\"";
            g.text(this.font, Component.literal(note), l + 4, top + 4, EXPR_OFF, false);
        }

        int max = Math.max(0, horseRows.size() - horseVisibleRows());
        if (max > 0) {
            scrollBarV(g, r - 1, r + 2, top, bottom, horseScroll, max,
                    Math.max(16, (bottom - top) * horseVisibleRows() / horseRows.size()),
                    v -> horseScroll = (int) Math.round(v));
        }

        drawHorseFooter(g, l, r, bottom + 6);
    }

    private void drawHorseRow(GuiGraphicsExtractor g, HorseListing row, int y, boolean sel) {
        int plain = sel ? NAME : NAME_DIM;
        String[] cells = {
                // The barn name is deliberately not a column: it is blank on
                // nearly every horse, so it was a column of dashes. It is on
                // the footer line and on the information screen, and `barn:`
                // still filters on it.
                row.displayName(),
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
        return Math.max(1, (pickerBottom(mares) - pickerTop(mares)) / HORSE_ROW_H);
    }

    private int pickerScroll(boolean mares) {
        return mares ? mareScroll : stallionScroll;
    }

    /** The roster row under the cursor in one of the two pickers, or null. */
    private HorseListing horseAt(double mx, double my, Sex sex) {
        boolean mares = sex == Sex.FEMALE;
        if (mx < listX() || mx > listX() + listW()
                || my < pickerTop(mares) || my >= pickerTop(mares) + pickerRows(mares) * HORSE_ROW_H) {
            return null;
        }
        List<HorseListing> list = ClientHorseRoster.of(sex);
        int i = pickerScroll(mares) + (int) ((my - pickerTop(mares)) / HORSE_ROW_H);
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
        drawPreviewPane(g, mouseX, mouseY);
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
        List<HorseListing> onScreen = new ArrayList<>();
        for (int i = scroll; i < list.size() && i < scroll + visible; i++) {
            HorseListing horse = list.get(i);
            onScreen.add(horse);
            int ry = top + (i - scroll) * HORSE_ROW_H;
            boolean sel = horse.id().equals(chosen);
            boolean hover = mouseX >= l && mouseX <= l + w && mouseY >= ry && mouseY < ry + HORSE_ROW_H;
            if (sel) {
                g.fill(l - 2, ry, l + w, ry + HORSE_ROW_H, ROW_SEL);
            } else if (hover) {
                g.fill(l - 2, ry, l + w, ry + HORSE_ROW_H, ROW_HOVER);
            }
            HorsePortrait.draw(g, ClientHorseCoats.get(horse.id()), !horse.adult(),
                    l, ry + 1, PORTRAIT_W, HORSE_ROW_H - 2, mouseX, mouseY);
            int tx = l + PORTRAIT_W + 4;
            int tw = l + w - 6 - tx;
            drawFitted(g, horse.displayName(), tx, ry + 4, tw, sel ? NAME : NAME_DIM);
            drawFitted(g, "g" + horse.generation() + "  " + horse.coat(),
                    tx, ry + 5 + this.font.lineHeight, tw, TAG);
        }
        g.disableScissor();
        ClientHorseCoats.request(onScreen);

        if (list.isEmpty()) {
            String note = !ClientHorseRoster.received()
                    ? "asking the server..."
                    : "no tamed " + (mares ? "mares" : "stallions") + " on record";
            g.text(this.font, Component.literal(note), l + 4, top + 2, EXPR_OFF, false);
        }

        int max = Math.max(0, list.size() - visible);
        if (max > 0) {
            scrollBarV(g, l + w - 3, l + w, top, bottom, scroll, max,
                    Math.max(16, (bottom - top) * visible / list.size()),
                    v -> {
                        int to = (int) Math.round(v);
                        if (mares) {
                            mareScroll = to;
                        } else {
                            stallionScroll = to;
                        }
                    });
        }
    }

    /** The chosen pair, drawn large above their Punnett squares. */
    private void drawPairPortraits(GuiGraphicsExtractor g, HorseListing dam, HorseListing sire,
                                   int l, int y, int mouseX, int mouseY) {
        int size = 34;
        HorsePortrait.draw(g, ClientHorseCoats.get(dam.id()), !dam.adult(),
                l, y, size, size, mouseX, mouseY);
        HorsePortrait.draw(g, ClientHorseCoats.get(sire.id()), !sire.adult(),
                l + size + 6, y, size, size, mouseX, mouseY);
    }

    private void drawPreviewPane(GuiGraphicsExtractor g, int mouseX, int mouseY) {
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

        drawPairPortraits(g, dam, sire, l, y, mouseX, mouseY);
        int textX = l + 80;
        drawFitted(g, dam.displayName() + "   x   " + sire.displayName(), textX, y + 4, w - 80, HEADING);
        drawFitted(g, dam.breed() + " mare  x  " + sire.breed() + " stallion",
                textX, y + 4 + lineH, w - 80, TAG);
        drawFitted(g, dam.coat() + "   x   " + sire.coat(),
                textX, y + 4 + lineH * 2, w - 80, DESC);
        y += 38;

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
            scrollBarV(g, r - 2, r + 1, top, bottom, detailScroll, detailMaxScroll,
                    Math.max(16, (int) ((long) trackH * trackH / contentH)),
                    v -> detailScroll = (float) v);
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
