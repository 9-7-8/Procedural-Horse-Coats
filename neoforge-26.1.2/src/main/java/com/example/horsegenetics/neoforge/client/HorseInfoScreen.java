package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.EpigenomeReadout;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneCodeDisplay;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.GeneCategory;
import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.common.trait.StatAxis;
import com.example.horsegenetics.common.trait.TraitBreakdown;
import com.example.horsegenetics.common.trait.Traits;
import com.example.horsegenetics.common.coat.CoatData;
import com.example.horsegenetics.neoforge.network.InspectHorsePayload;
import com.example.horsegenetics.neoforge.network.OffspringDataPayload;
import com.example.horsegenetics.neoforge.network.OffspringRequestPayload;
import com.example.horsegenetics.neoforge.network.SetBarnNamePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>The horse information screen</b> - the "i" button at the top left of the
 * horse inventory ({@link HorseScreenHooks}) opens this, and everything this
 * mod has to say about one horse is on it.
 *
 * <p>Six tabs, drawn from a strip at the top in the same dark chrome as the
 * horse browser ({@code HorseBrowserScreen}), which is the mod's established
 * full-window look:
 *
 * <ul>
 *   <li><b>Overview</b> - who this horse is. Name, barn name (editable here and
 *       nowhere else now), sex, generation, breed, its four live body numbers,
 *       its bond, its disorders, and who bred / tamed / owns it.</li>
 *   <li><b>Genes</b> - the genotype, one row per locus. Deliberately the only
 *       tab with <b>no</b> epigenetic values: it is the index, and a wall of
 *       numbers under every row is what the other three tabs are for. A filter
 *       button, <b>on by default</b>, hides the loci the horse is plain
 *       baseline at, keeping the three that decide its base colour; the genetic
 *       code itself is not here at all - it is on the spawn egg and the
 *       designer, and a horse's own screen is the wrong place to read a
 *       thousand characters of it.</li>
 *   <li><b>Health</b> - the genes behind speed, max health, jump and size, each
 *       showing <i>its own</i> contribution rather than the total, plus every
 *       disorder. The numbers come from {@link TraitBreakdown}, so they are the
 *       genes' own arithmetic and not a second implementation of it.</li>
 *   <li><b>Coat</b> - the genes that decide what colour this horse is.</li>
 *   <li><b>Other genes</b> - everything that reaches the horse through neither
 *       its coat nor its body: abilities, the diet locus, eye colour, cutie
 *       marks. A gene that does one of those <i>and</i> paints - dhampir - is
 *       here rather than on Coat, because the coat is the least of what it
 *       does. {@link GeneCategory} decides, from what the gene implements.</li>
 *   <li><b>Offspring</b> - the other direction: this horse's foals, then
 *       their foals, each generation drawn as a row of little horses under a
 *       divider. It is the <b>only</b> tab that does not redraw from what the
 *       client already has - the answer is a walk of the whole ancestry table
 *       and a full record per descendant, so it is asked for on a
 *       <b>Refresh</b> press and not before.</li>
 *   <li><b>Family tree</b> - hands straight off to {@link FamilyTreeScreen},
 *       which comes back here when it closes.</li>
 * </ul>
 *
 * <p>Escape / Done goes back to the horse inventory screen it was opened from,
 * so the button is a detour rather than an exit.
 */
public final class HorseInfoScreen extends Screen {

    private enum Tab {
        OVERVIEW("Overview"),
        GENES("Genes"),
        HEALTH("Health"),
        COAT("Coat"),
        OTHER("Other genes"),
        OFFSPRING("Offspring"),
        FAMILY("Family tree");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    // The browser's palette, by value - the two screens are meant to read as
    // one tool. A change here belongs there.
    private static final int DIM = 0xC80A0A0E;
    private static final int PANEL = 0xF014141C;
    private static final int PANEL_SOFT = 0xE01A1A24;
    private static final int BORDER = 0xFF3C3C4A;
    private static final int TAB_ON = 0xFF2E2E3C;
    private static final int TAB_OFF = 0xFF191921;
    private static final int TAB_ACCENT = 0xFF55A0E0;
    private static final int TAB_TEXT_ON = 0xFFFFFFFF;
    private static final int TAB_TEXT_OFF = 0xFF888F9F;
    private static final int HEADING = 0xFFF2F2F6;
    private static final int LABEL = 0xFF8890A8;
    private static final int VALUE = 0xFFE4E8F0;
    private static final int DESC = 0xFFB2B8C6;
    private static final int DIM_TEXT = 0xFF6E7686;
    private static final int ALLELE_TOK = 0xFFE0C070;
    private static final int EXPR_ON = 0xFF9BE08A;
    private static final int EXPR_CARRIER = 0xFFC9B27A;
    private static final int GOOD = 0xFF9BE08A;
    // Brighter than it was: this red now carries a number a player reads
    // ("speed 0.164"), not just a word, so it has to be legible rather than
    // merely alarming. ~7:1 against the panel.
    private static final int BAD = 0xFFF08C8C;
    private static final int RULE = 0x22FFFFFF;
    /** Fainter than {@link #RULE} - it separates rows, it does not end a section. */
    private static final int DIVIDER = 0x18FFFFFF;
    private static final int FIELD_WELL = 0xFF0B0B10;
    private static final int FIELD_EDGE = 0xFF5A6274;

    private static final int TAB_TOP = 6;
    private static final int TAB_H = 18;
    private static final int PAD = 10;

    /** Overview's fixed header: the horse's name, then the barn-name editor. */
    private static final int HEADER_H = 42;

    /** Genes' fixed header: the locus count, and the filter button beside it. */
    private static final int GENES_HEADER_H = 24;

    /** Offspring's fixed header: the Refresh button and what it last found. */
    private static final int OFFSPRING_HEADER_H = 24;

    /** One descendant's portrait, and the pitch of a row of them. */
    private static final int FOAL_W = 40;
    private static final int FOAL_H = 40;
    private static final int FOAL_GAP = 4;

    /** Remembered across openings - reopening on the tab you were reading. */
    private static Tab lastTab = Tab.OVERVIEW;

    /**
     * Remembered across openings, and <b>on</b> by default: most horses are
     * baseline at most of their loci, and a list of eighty rows saying "nothing
     * here" is what made the Genes tab unreadable. Off shows every locus.
     */
    private static boolean hideBaseline = true;

    private final HorseRecord record;
    private final @Nullable AbstractHorse horse;
    private final @Nullable Screen parent;

    private final Genotype genotype;
    private final @Nullable Epigenome epigenome;

    private Tab tab = lastTab;
    private float scroll = 0f;
    private float maxScroll = 0f;

    private EditBox barnBox;
    private Button setBarnButton;
    private Button baselineFilterButton;
    private Button offspringRefreshButton;

    /** Ticks since the screen opened, for the once-a-second hold heartbeat. */
    private int heldTicks;

    public HorseInfoScreen(HorseRecord record, @Nullable AbstractHorse horse, @Nullable Screen parent) {
        super(Component.literal(record.displayName()));
        this.record = record;
        this.horse = horse;
        this.parent = parent;

        Genotype g;
        try {
            g = record.genotype();
        } catch (RuntimeException unparseable) {
            g = Genotype.parse("");
        }
        this.genotype = g;

        Epigenome e;
        try {
            e = record.hasGenome() ? record.epigenome() : null;
        } catch (RuntimeException unparseable) {
            e = null;
        }
        this.epigenome = e;
    }

    // ------------------------------------------------------------------
    // Geometry
    // ------------------------------------------------------------------

    private int panelLeft() {
        return Math.max(8, this.width / 2 - 270);
    }

    private int panelRight() {
        return Math.min(this.width - 8, this.width / 2 + 270);
    }

    private int panelTop() {
        return TAB_TOP + TAB_H;
    }

    private int panelBottom() {
        return this.height - 32;
    }

    private int contentLeft() {
        return panelLeft() + PAD;
    }

    private int contentRight() {
        return panelRight() - PAD - 4;
    }

    private int contentTop() {
        return panelTop() + PAD;
    }

    /**
     * Where the scrolling page starts. Overview keeps a fixed header above it
     * holding the name and the barn-name editor: those are real widgets drawn
     * by the widget layer at a fixed position, so they cannot be allowed to
     * scroll out from under the page they belong to.
     */
    private int pageTop() {
        return switch (tab) {
            case OVERVIEW -> contentTop() + HEADER_H;
            case GENES -> contentTop() + GENES_HEADER_H;
            case OFFSPRING -> contentTop() + OFFSPRING_HEADER_H;
            default -> contentTop();
        };
    }

    private int contentWidth() {
        return contentRight() - contentLeft();
    }

    private int lineH() {
        return this.font.lineHeight + 2;
    }

    // ------------------------------------------------------------------
    // Widgets
    // ------------------------------------------------------------------

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(this.width / 2 - 50, this.height - 26, 100, 20)
                .build());

        // The barn-name box lives on Overview only. It is created regardless so
        // its value survives a tab switch, and hidden by applyTabWidgets().
        int barnY = contentTop() + HEADER_H - 20;
        barnBox = new EditBox(this.font, contentLeft() + 74, barnY, 116, 16,
                Component.literal("Barn name"));
        barnBox.setMaxLength(HorseRecord.MAX_BARN_NAME);
        barnBox.setHint(Component.literal("barn name"));
        record.barnName().ifPresent(barnBox::setValue);
        addRenderableWidget(barnBox);

        setBarnButton = Button.builder(Component.literal("Set"), b -> submitBarnName())
                .bounds(contentLeft() + 194, barnY, 30, 16)
                .build();
        addRenderableWidget(setBarnButton);

        // Genes' filter. Same shape as the browser's "Only what can vary"
        // toggle - the label says what you are looking at, not what the button
        // would do, which is the one that reads right on a screen you glance at.
        int filterW = 150;
        baselineFilterButton = Button.builder(baselineFilterLabel(), b -> {
                    hideBaseline = !hideBaseline;
                    b.setMessage(baselineFilterLabel());
                    scroll = 0f;
                })
                .bounds(contentRight() - filterW, contentTop() - 2, filterW, 16)
                .build();
        addRenderableWidget(baselineFilterButton);

        offspringRefreshButton = Button.builder(Component.literal("Refresh"), b -> requestOffspring())
                .bounds(contentRight() - buttonW("Refresh"), contentTop() - 2, buttonW("Refresh"), 16)
                .build();
        addRenderableWidget(offspringRefreshButton);

        applyTabWidgets();
        sendHold(true);
    }

    /** A button exactly as wide as what is written on it - see the browser's twin. */
    private int buttonW(String label) {
        return this.font.width(label) + 14;
    }

    private void requestOffspring() {
        ClientPacketDistributor.sendToServer(new OffspringRequestPayload(record.id()));
    }

    private Component baselineFilterLabel() {
        return Component.literal(hideBaseline ? "Only what it carries" : "Every locus");
    }

    private void applyTabWidgets() {
        boolean overview = tab == Tab.OVERVIEW;
        if (barnBox != null) {
            barnBox.visible = overview;
            barnBox.active = overview;
            if (!overview) {
                barnBox.setFocused(false);
            }
        }
        if (setBarnButton != null) {
            setBarnButton.visible = overview;
            setBarnButton.active = overview;
        }
        boolean genes = tab == Tab.GENES;
        if (baselineFilterButton != null) {
            baselineFilterButton.visible = genes;
            baselineFilterButton.active = genes;
        }
        boolean offspring = tab == Tab.OFFSPRING;
        if (offspringRefreshButton != null) {
            offspringRefreshButton.visible = offspring;
            offspringRefreshButton.active = offspring;
        }
    }

    private void submitBarnName() {
        if (horse != null) {
            ClientPacketDistributor.sendToServer(new SetBarnNamePayload(horse.getId(), barnBox.getValue()));
        }
    }

    /**
     * Re-assert the hold once a second. It is a lease rather than a flag on
     * purpose - see {@code server/HorseInspectHold} - so a client that dies with
     * this screen open leaves the horse walking again a moment later instead of
     * frozen forever.
     */
    @Override
    public void tick() {
        super.tick();
        if (heldTicks++ % 20 == 0) {
            sendHold(true);
        }
    }

    @Override
    public void removed() {
        super.removed();
        sendHold(false);
    }

    /** Ask the server to stop this horse wandering off while it is being read about. */
    private void sendHold(boolean watching) {
        if (horse != null) {
            ClientPacketDistributor.sendToServer(new InspectHorsePayload(horse.getId(), watching));
        }
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

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Tab hit = tabAt(event.x(), event.y());
        if (hit != null) {
            select(hit);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void select(Tab hit) {
        if (hit == Tab.FAMILY) {
            // Not a page - the family tree is its own screen, and this is the
            // way in to it. It comes back here rather than to the game.
            Minecraft.getInstance().setScreen(new FamilyTreeScreen(record, this));
            return;
        }
        if (hit != tab) {
            tab = hit;
            lastTab = hit;
            scroll = 0f;
            applyTabWidgets();
        }
    }

    private int tabStripLeft() {
        int total = 0;
        for (Tab t : Tab.values()) {
            total += this.font.width(t.label) + 20 + 3;
        }
        return this.width / 2 - total / 2;
    }

    private @Nullable Tab tabAt(double mx, double my) {
        if (my < TAB_TOP || my > TAB_TOP + TAB_H) {
            return null;
        }
        int tx = tabStripLeft();
        for (Tab t : Tab.values()) {
            int w = this.font.width(t.label) + 20;
            if (mx >= tx && mx <= tx + w) {
                return t;
            }
            tx += w + 3;
        }
        return null;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (maxScroll > 0f) {
            scroll = Math.max(0f, Math.min(maxScroll, scroll - (float) sy * 18f));
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------

    /**
     * <b>The widgets are drawn last, not first.</b> {@code Screen}'s default is
     * to paint them and then hand over, which is right for a screen that draws
     * a background - and wrong for this one, which paints a near-opaque panel
     * across the whole window. Drawn in the default order, the barn-name box
     * came out under 94% of that panel: still there, still clickable, and
     * looking like nothing at all. So the panel goes down first and
     * {@code super} runs at the end.
     */
    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, DIM);

        int pl = panelLeft();
        int pr = panelRight();
        int pt = panelTop();
        int pb = panelBottom();
        g.fill(pl, pt, pr, pb, PANEL);
        g.fill(pl, pt, pr, pt + 1, BORDER);
        g.fill(pl, pb - 1, pr, pb, BORDER);
        g.fill(pl, pt, pl + 1, pb, BORDER);
        g.fill(pr - 1, pt, pr, pb, BORDER);

        drawTabStrip(g);

        if (tab == Tab.OVERVIEW) {
            drawOverviewHeader(g);
        } else if (tab == Tab.GENES) {
            drawGenesHeader(g);
        } else if (tab == Tab.OFFSPRING) {
            drawOffspringHeader(g);
        }

        int top = pageTop();
        int bottom = pb - PAD;
        int y0 = top - (int) scroll;

        g.enableScissor(pl + 1, top - 2, pr - 1, bottom + 4);
        Cursor c = new Cursor(g, contentLeft(), y0, contentWidth());
        switch (tab) {
            case OVERVIEW -> drawOverview(c);
            case GENES -> drawGenes(c);
            case HEALTH -> drawHealth(c);
            case COAT -> drawGeneList(c, GeneCategory.COAT, "Nothing but the baseline colour genes.");
            case OTHER -> drawGeneList(c, GeneCategory.OTHER, "This horse carries no ability, diet or eye genes.");
            case OFFSPRING -> drawOffspring(c, mouseX, mouseY);
            case FAMILY -> { /* never rendered - selecting it pushes a screen */ }
        }
        g.disableScissor();

        int contentH = c.y - y0;
        maxScroll = Math.max(0f, contentH - (bottom - top));
        scroll = Math.max(0f, Math.min(scroll, maxScroll));
        drawScrollbar(g, pr, top, bottom, contentH);

        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    private void drawTabStrip(GuiGraphicsExtractor g) {
        int tx = tabStripLeft();
        for (Tab t : Tab.values()) {
            int w = this.font.width(t.label) + 20;
            boolean on = t == tab;
            g.fill(tx, TAB_TOP, tx + w, TAB_TOP + TAB_H, on ? TAB_ON : TAB_OFF);
            g.fill(tx, TAB_TOP, tx + w, TAB_TOP + 2, on ? TAB_ACCENT : BORDER);
            g.text(this.font, Component.literal(t.label), tx + 10, TAB_TOP + 5,
                    on ? TAB_TEXT_ON : TAB_TEXT_OFF, false);
            tx += w + 3;
        }
    }

    private void drawScrollbar(GuiGraphicsExtractor g, int pr, int top, int bottom, int contentH) {
        if (maxScroll <= 0f || contentH <= 0) {
            return;
        }
        int trackH = bottom - top;
        int x1 = pr - 4;
        g.fill(x1 - 3, top, x1, bottom, 0x33FFFFFF);
        int thumbH = Math.max(20, (int) ((long) trackH * trackH / contentH));
        int thumbY = top + Math.round(scroll * (trackH - thumbH) / maxScroll);
        g.fill(x1 - 3, thumbY, x1, thumbY + thumbH, 0xAAFFFFFF);
    }

    // ------------------------------------------------------------------
    // Overview
    // ------------------------------------------------------------------

    /**
     * The part of Overview that does not scroll: the horse's full name, and the
     * barn-name editor whose two widgets are painted by the widget layer at a
     * fixed position. Everything else about the horse is on the page below.
     */
    private void drawOverviewHeader(GuiGraphicsExtractor g) {
        int x = contentLeft();
        int y = contentTop();
        g.text(this.font, Component.literal(GuiText.clip(fullName(), 48)), x, y, HEADING, false);
        y += lineH() + 2;
        g.text(this.font, Component.literal(record.barnName().isPresent()
                        ? "known around the yard as \"" + record.barnName().get() + "\""
                        : "no barn name yet"),
                x, y, DIM_TEXT, false);

        g.text(this.font, Component.literal("Barn name"), x, contentTop() + HEADER_H - 16, LABEL, false);
        drawFieldFrame(g, barnBox);
        g.fill(x, contentTop() + HEADER_H - 4, contentRight(), contentTop() + HEADER_H - 3, RULE);
    }

    /**
     * A well and a lit edge around a text field. The vanilla {@code EditBox}
     * sprite is a dark grey box that all but vanishes on this screen's dark
     * panel - it looked like a caption, not somewhere to type - so the frame is
     * drawn underneath it, one pixel proud on every side, and brightens to the
     * tab accent while the field has focus.
     */
    private void drawFieldFrame(GuiGraphicsExtractor g, EditBox box) {
        if (box == null || !box.visible) {
            return;
        }
        int x0 = box.getX() - 2;
        int y0 = box.getY() - 2;
        int x1 = box.getX() + box.getWidth() + 2;
        int y1 = box.getY() + box.getHeight() + 2;
        int edge = box.isFocused() ? TAB_ACCENT : FIELD_EDGE;
        g.fill(x0, y0, x1, y1, FIELD_WELL);
        g.fill(x0, y0, x1, y0 + 1, edge);
        g.fill(x0, y1 - 1, x1, y1, edge);
        g.fill(x0, y0, x0 + 1, y1, edge);
        g.fill(x1 - 1, y0, x1, y1, edge);
    }

    private void drawOverview(Cursor c) {
        boolean adult = horse == null || !horse.isBaby();

        c.pair("Sex", record.sex().label(adult));
        c.pair("Generation", Integer.toString(record.generation()));
        c.pair("Breed", record.lineage().displayName());
        c.rule();

        c.label("Body");
        if (horse != null) {
            double cur = horse.getHealth();
            double max = horse.getAttributeValue(Attributes.MAX_HEALTH);
            c.pair("Health", String.format("%.1f / %.1f  (%.0f hearts)", cur, max, Math.ceil(max / 2.0)),
                    cur < max * 0.4 ? BAD : cur >= max ? GOOD : VALUE);
            c.pair("Speed", String.format("%.3f", horse.getAttributeValue(Attributes.MOVEMENT_SPEED)));
            c.pair("Jump", String.format("%.2f", horse.getAttributeValue(Attributes.JUMP_STRENGTH)));
            c.pair("Size", sizeWord(horse.getAttributeValue(Attributes.SCALE))
                    + String.format("  (%.2f)", horse.getAttributeValue(Attributes.SCALE)));
        } else {
            Traits t = traits();
            c.pair("Health", String.format("%.1f", t.health()) + "  (from the genotype)");
            c.pair("Speed", String.format("%.3f", t.speed()));
            c.pair("Jump", String.format("%.2f", t.jump()));
            c.pair("Size", sizeWord(t.scale()));
        }

        ClientHorseCareCache.Care care = horse == null ? null : ClientHorseCareCache.get(horse.getId());
        if (care != null) {
            c.pair("Bond", care.bond() + "  " + bondTierLabel(care.bond())
                    + (care.inHerd() ? "   • in a herd" : ""));
        }

        List<Condition> conditions = traits().conditions();
        if (!conditions.isEmpty()) {
            c.rule();
            c.label("Conditions");
            for (Condition condition : conditions) {
                c.line(condition.name(), condition.severity().lethal() ? BAD : EXPR_CARRIER);
                c.wrapped(condition.description(), DESC, 8);
            }
        }
        c.rule();

        c.label("Provenance");
        c.pair("Bred by", record.bredBy().orElse("— (not bred in captivity)"));
        c.pair("Tamed by", record.tamedBy().orElse("— (never tamed)"));
        String owner = currentOwnerName();
        if (owner != null && !owner.equals(record.tamedBy().orElse(null))) {
            c.pair("Owner now", owner);
        }
    }

    private String fullName() {
        return (record.firstName() + " " + record.lastName()).strip();
    }

    /**
     * The horse's owner <i>right now</i>, as opposed to whoever first tamed it.
     * Only resolvable when the owner entity is loaded on this client - which,
     * for the player standing in front of the horse reading this screen, it
     * nearly always is.
     */
    private @Nullable String currentOwnerName() {
        if (horse == null) {
            return null;
        }
        LivingEntity owner = horse.getOwner();
        return owner == null ? null : owner.getName().getString();
    }

    /** Matches HorseCareAttachment.behaviourTier() - kept here to avoid a server import. */
    private static String bondTierLabel(int bond) {
        if (bond >= 81) return "follows";
        if (bond >= 61) return "approaches";
        if (bond >= 31) return "attentive";
        return "wary";
    }

    /** Body scale as a word - a number between 0.88 and 1.10 means nothing to a player. */
    private static String sizeWord(double scale) {
        if (scale < 0.80) return "dwarf";
        if (scale < 0.94) return "small";
        if (scale <= 1.03) return "average";
        if (scale <= 1.12) return "large";
        return "draught";
    }

    /**
     * This horse's body as the alleles describe it. Resolved with the health
     * genetics on regardless of the server setting: carrying a disorder is a
     * fact about the alleles whatever the server does with it, and a breeder
     * needs to see it either way.
     */
    private Traits traits() {
        try {
            return HorseTraits.resolve(genotype, epigenome, true);
        } catch (RuntimeException unresolvable) {
            return HorseTraits.baseline();
        }
    }

    // ------------------------------------------------------------------
    // Genes - the index
    // ------------------------------------------------------------------

    /**
     * The fixed strip above the locus list: the short form of the genotype, and
     * how much of the list the filter is showing. The filter itself is a real
     * widget drawn by the widget layer at a fixed position on the right, so
     * like Overview's barn box it cannot be allowed to scroll away from the
     * count it belongs to.
     */
    private void drawGenesHeader(GuiGraphicsExtractor g) {
        int x = contentLeft();
        int y = contentTop();
        int total = Genes.codeOrder().size();
        int shown = countShownLoci();
        g.text(this.font, Component.literal(GuiText.clip(GeneCodeDisplay.shortForm(genotype), 56)),
                x, y, ALLELE_TOK, false);
        y += lineH() + 1;
        g.text(this.font, Component.literal(shown == total
                        ? "all " + total + " loci, in code order"
                        : shown + " of " + total + " loci - the rest are plain baseline"),
                x, y, DIM_TEXT, false);
        g.fill(x, contentTop() + GENES_HEADER_H - 5, contentRight(),
                contentTop() + GENES_HEADER_H - 4, RULE);
    }

    private int countShownLoci() {
        int shown = 0;
        for (Gene gene : Genes.codeOrder()) {
            if (showsLocus(gene)) {
                shown++;
            }
        }
        return shown;
    }

    /**
     * Is this locus on the list right now? With the filter on, a locus is worth
     * a row when the horse is <b>not</b> plain baseline at it - which keeps
     * silent carriers, deliberately: a carrier is the single most interesting
     * thing a breeder can be told, and it is exactly the row a "hide the wild
     * type" filter would throw away if it read the phenotype instead of the
     * alleles. The three loci that decide the base colour stay whatever the
     * filter says, because "what colour is this horse" has no answer without
     * them.
     */
    private boolean showsLocus(Gene gene) {
        if (!hideBaseline || isBaseCoatLocus(gene)) {
            return true;
        }
        return !gene.atBaseline(genotype.pair(gene));
    }

    /** Extension, agouti and shade - the base coat, and never filtered out. */
    private static boolean isBaseCoatLocus(Gene gene) {
        return gene == Genes.EXTENSION || gene == Genes.AGOUTI || gene == Genes.SHADE;
    }

    private void drawGenes(Cursor c) {
        int shown = 0;
        for (Gene gene : Genes.codeOrder()) {
            if (!showsLocus(gene)) {
                continue;
            }
            AllelePair pair = genotype.pair(gene);
            Expression expr = genotype.expressionOf(gene);
            boolean baseline = gene.atBaseline(pair);
            boolean on = !expr.wildType();
            shown++;
            c.row(gene.name(), pair.toTokens(), expr.name(),
                    on ? EXPR_ON : baseline ? DIM_TEXT : EXPR_CARRIER);
        }
        if (shown == 0) {
            c.line("This horse is plain baseline at every locus but its base colour.", DIM_TEXT);
        }
    }

    // ------------------------------------------------------------------
    // Health
    // ------------------------------------------------------------------

    private void drawHealth(Cursor c) {
        Traits t = traits();
        c.heading("What this horse's body came out at");
        // Green above the baseline, red below, plain when it is exactly on it.
        // Size is left plain on purpose: a draught horse is not a worse horse
        // than a pony, and colouring it would say it was.
        c.pair("Max health", String.format("%.1f", t.health())
                        + "   (baseline " + String.format("%.1f", HorseTraits.BASE_HEALTH) + ")",
                versusBaseline(t.health(), HorseTraits.BASE_HEALTH));
        c.pair("Speed", String.format("%.3f", t.speed())
                        + "   (baseline " + String.format("%.3f", HorseTraits.BASE_SPEED) + ")",
                versusBaseline(t.speed(), HorseTraits.BASE_SPEED));
        c.pair("Jump", String.format("%.2f", t.jump())
                        + "   (baseline " + String.format("%.2f", HorseTraits.BASE_JUMP) + ")",
                versusBaseline(t.jump(), HorseTraits.BASE_JUMP));
        c.pair("Size", String.format("%.2f", t.scale()) + "   " + sizeWord(t.scale()));
        c.gap(3);
        c.wrapped("Every number above is the baseline plus what the genes below add. Nothing is "
                + "rolled - two horses with the same alleles are the same horse.", DIM_TEXT, 0);
        c.rule();

        List<TraitBreakdown.Term> terms;
        try {
            terms = TraitBreakdown.of(genotype, epigenome, true);
        } catch (RuntimeException unresolvable) {
            terms = List.of();
        }
        if (terms.isEmpty()) {
            c.line("This horse carries nothing that moves its body off the baseline.", DIM_TEXT);
            return;
        }

        for (StatAxis axis : StatAxis.values()) {
            List<TraitBreakdown.Term> on = TraitBreakdown.on(terms, axis);
            if (on.isEmpty()) {
                continue;
            }
            c.label(axisLabel(axis));
            for (int i = 0; i < on.size(); i++) {
                if (i > 0) {
                    c.divider();
                }
                drawTerm(c, on.get(i), axis);
            }
            c.gap(3);
        }

        List<TraitBreakdown.Term> withConditions = new ArrayList<>();
        for (TraitBreakdown.Term term : terms) {
            if (!term.conditions().isEmpty()) {
                withConditions.add(term);
            }
        }
        if (!withConditions.isEmpty()) {
            c.rule();
            c.label("Disorders this horse expresses");
            for (int i = 0; i < withConditions.size(); i++) {
                if (i > 0) {
                    c.divider();
                }
                TraitBreakdown.Term term = withConditions.get(i);
                c.row(term.gene().name(), term.pair().toTokens(), "", VALUE);
                for (Condition condition : term.conditions()) {
                    c.line(condition.name(), condition.severity().lethal() ? BAD : EXPR_CARRIER, 8);
                    c.wrapped(condition.description(), DESC, 16);
                }
                epigenetics(c, term.gene());
            }
        }
    }

    private void drawTerm(Cursor c, TraitBreakdown.Term term, StatAxis axis) {
        String delta = switch (axis) {
            case SPEED -> signed(term.speed(), 3) + factor(term.speedFactor());
            case HEALTH -> signed(term.health(), 1) + factor(term.healthFactor());
            case JUMP -> signed(term.jump(), 2) + factor(term.jumpFactor());
            case SCALE -> signed(term.scale(), 2) + factor(term.scaleFactor());
        };
        boolean helps = switch (axis) {
            case SPEED -> term.speed() > 0 || term.speedFactor() > 1.0;
            case HEALTH -> term.health() > 0 || term.healthFactor() > 1.0;
            case JUMP -> term.jump() > 0 || term.jumpFactor() > 1.0;
            case SCALE -> true; // bigger is not better; size is just size
        };
        c.row(term.gene().name(), term.pair().toTokens(), delta.strip(),
                axis == StatAxis.SCALE ? VALUE : helps ? GOOD : BAD);
        epigenetics(c, term.gene());
    }

    /**
     * Green if this number is above the baseline horse, red if below, the plain
     * value colour if it is exactly on it.
     */
    private static int versusBaseline(double actual, double baseline) {
        if (actual > baseline + 1e-6) {
            return GOOD;
        }
        return actual < baseline - 1e-6 ? BAD : VALUE;
    }

    private static String axisLabel(StatAxis axis) {
        return switch (axis) {
            case SPEED -> "Speed";
            case HEALTH -> "Max health";
            case JUMP -> "Jump strength";
            case SCALE -> "Size";
        };
    }

    /** {@code +0.020} / {@code -1.5}, or empty when the gene adds nothing on this axis. */
    private static String signed(double delta, int decimals) {
        if (delta == 0.0) {
            return "";
        }
        return String.format("%+." + decimals + "f", delta);
    }

    /** {@code x1.42}, or empty when the gene does not multiply this axis. */
    private static String factor(double factor) {
        return factor == 1.0 ? "" : String.format("  ×%.2f", factor);
    }

    // ------------------------------------------------------------------
    // Coat / Other - one gene list, filtered by category
    // ------------------------------------------------------------------

    /**
     * Every gene in {@code category} this horse carries something at, with the
     * outcome it actually produces and the numbers that gene wrote on the two
     * allele copies. A gene sitting at its plain baseline is left out - it is on
     * the Genes tab, where the whole list lives.
     */
    private void drawGeneList(Cursor c, GeneCategory category, String emptyLine) {
        c.heading(category == GeneCategory.COAT
                ? "The genes that decide what colour this horse is"
                : "Genes that do something other than colour or body");
        if (category == GeneCategory.OTHER) {
            c.wrapped("A gene that changes the coat and something else is here rather than on Coat "
                    + "- the coat is the least of what it does.", DIM_TEXT, 0);
        }
        c.rule();

        int shown = 0;
        for (Gene gene : Genes.codeOrder()) {
            AllelePair pair = genotype.pair(gene);
            // The per-horse category, not the per-gene one: KIT declares an eye
            // channel, but only its broad white outcomes ever claim an iris, so
            // an ordinary sabino belongs on Coat and every horse alive was
            // getting a KIT row under Other genes instead.
            if (GeneCategory.of(gene, pair, genotype, epigenome) != category) {
                continue;
            }
            Expression expr = genotype.expressionOf(gene);
            boolean baseline = gene.atBaseline(pair);
            boolean on = !expr.wildType();
            // Always show the three loci that define the base horse, and
            // otherwise only a locus the horse carries something at - including
            // a silent carrier, which is exactly what a breeder is looking for.
            boolean always = isBaseCoatLocus(gene);
            if (!always && baseline) {
                continue;
            }
            if (shown > 0) {
                c.divider();
            }
            shown++;
            c.row(gene.name(), pair.toTokens(), expr.name(),
                    on ? EXPR_ON : baseline ? DIM_TEXT : EXPR_CARRIER);
            String d = expr.description();
            if (d != null && !d.isBlank()) {
                c.wrapped(d, on || !baseline ? DESC : DIM_TEXT, 8);
            }
            epigenetics(c, gene);
            c.gap(2);
        }
        if (shown == 0) {
            c.line(emptyLine, DIM_TEXT);
        }
    }

    /**
     * The numbers this gene wrote on the horse's two allele copies, with the
     * expressed one marked. Empty for most genes - the shared formatter says so
     * and this draws nothing.
     */
    /**
     * A descendant's coat, straight off its record. Null when the record has no
     * genome - the portrait draws an empty well and says nothing it cannot
     * know, which is the same rule the family tree follows for an ancestor
     * whose coat is genuinely gone.
     */
    private static @Nullable CoatData coatOf(HorseRecord horse) {
        try {
            return horse.hasGenome() ? new CoatData(horse.genome()) : null;
        } catch (RuntimeException unreadable) {
            return null;
        }
    }

    /** Text at {@code (x, y)}, squeezed rather than clipped when it will not fit. */
    private void drawFitted(GuiGraphicsExtractor g, String text, int x, int y, int maxW, int colour) {
        float w = this.font.width(text);
        if (w <= maxW || w <= 0) {
            g.text(this.font, Component.literal(text), x, y, colour, false);
            return;
        }
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(maxW / w);
        g.text(this.font, Component.literal(text), 0, 0, colour, false);
        pose.popMatrix();
    }

    private void epigenetics(Cursor c, Gene gene) {
        if (epigenome == null) {
            return;
        }
        List<String> lines;
        try {
            lines = EpigenomeReadout.lines(gene, genotype, epigenome);
        } catch (RuntimeException unreadable) {
            return;
        }
        for (String line : lines) {
            c.line(line, ALLELE_TOK, 16);
        }
    }

    // ------------------------------------------------------------------
    // Offspring - the pedigree read downward
    // ------------------------------------------------------------------

    /**
     * The strip above the generations: what the last answer was, and the button
     * that asks for another. Fixed rather than scrolling, for the same reason
     * Overview's barn box is - it is a real widget at a real position and it
     * cannot be allowed to slide away from the text it belongs to.
     */
    private void drawOffspringHeader(GuiGraphicsExtractor g) {
        int x = contentLeft();
        int y = contentTop();
        g.text(this.font, Component.literal("Descendants of " + GuiText.clip(fullName(), 40)),
                x, y, HEADING, false);
        y += lineH() + 1;
        boolean asked = record.id().equals(ClientOffspring.rootId());
        g.text(this.font, Component.literal(asked
                        ? "as of the last refresh"
                        : "press Refresh - this tab is the only one that asks the server"),
                x, y, DIM_TEXT, false);
        g.fill(x, contentTop() + OFFSPRING_HEADER_H - 5, contentRight(),
                contentTop() + OFFSPRING_HEADER_H - 4, RULE);
    }

    /**
     * One block per generation - foals, grandfoals, and so on - each a wrapped
     * row of little horses in their real coats, separated by a rule. A
     * descendant's coat comes straight off its record (which carries the
     * epigenome), so nothing here needs a second round trip.
     */
    private void drawOffspring(Cursor c, int mouseX, int mouseY) {
        List<OffspringDataPayload.Generation> generations = ClientOffspring.of(record.id());
        if (generations.isEmpty()) {
            c.wrapped(record.id().equals(ClientOffspring.rootId())
                            ? "This horse has no recorded descendants."
                            : "Nothing asked for yet. Refresh walks the whole ancestry table and "
                                    + "sends a full record for every descendant, which is why it is a "
                                    + "button and not something that happens when you open the tab.",
                    DIM_TEXT, 0);
            return;
        }
        for (int i = 0; i < generations.size(); i++) {
            if (i > 0) {
                c.rule();
            }
            OffspringDataPayload.Generation generation = generations.get(i);
            c.label(generationLabel(i) + "  (" + generation.horses().size()
                    + (generation.truncated() ? "+" : "") + ")");
            c.horses(generation.horses(), mouseX, mouseY);
            if (generation.truncated()) {
                c.line("...and more than this tab will fetch at once.", DIM_TEXT, 4);
            }
            c.gap(2);
        }
    }

    /** Foals, grandfoals, great-grandfoals - and then plain numbers. */
    private static String generationLabel(int index) {
        return switch (index) {
            case 0 -> "Foals";
            case 1 -> "Grandfoals";
            case 2 -> "Great-grandfoals";
            default -> (index + 1) + " generations down";
        };
    }

    // ------------------------------------------------------------------
    // A y cursor, so a tab reads as the page it draws
    // ------------------------------------------------------------------

    private final class Cursor {

        private final GuiGraphicsExtractor g;
        private final int x;
        private final int width;
        private int y;

        Cursor(GuiGraphicsExtractor g, int x, int y, int width) {
            this.g = g;
            this.x = x;
            this.y = y;
            this.width = width;
        }

        void heading(String text) {
            g.text(font, Component.literal(GuiText.clip(text, 64)), x, y, HEADING, false);
            y += lineH() + 3;
        }

        void label(String text) {
            g.text(font, Component.literal(text.toUpperCase(java.util.Locale.ROOT)), x, y, LABEL, false);
            y += lineH() + 1;
        }

        void line(String text, int colour) {
            line(text, colour, 0);
        }

        void line(String text, int colour, int indent) {
            g.text(font, Component.literal(text), x + indent, y, colour, false);
            y += lineH();
        }

        void wrapped(String text, int colour, int indent) {
            for (String l : GuiText.wrap(font, text, width - indent)) {
                g.text(font, Component.literal(l), x + indent, y, colour, false);
                y += lineH();
            }
        }

        void pair(String label, String value) {
            pair(label, value, VALUE);
        }

        void pair(String label, String value, int colour) {
            g.text(font, Component.literal(label), x, y, LABEL, false);
            g.text(font, Component.literal(value), x + 96, y, colour, false);
            y += lineH();
        }

        /** {@code gene name | tokens | outcome} - the three-column gene row. */
        void row(String name, String tokens, String outcome, int outcomeColour) {
            g.text(font, Component.literal(GuiText.clip(name, 28)), x, y, VALUE, false);
            g.text(font, Component.literal(tokens), x + 150, y, ALLELE_TOK, false);
            if (!outcome.isEmpty()) {
                g.text(font, Component.literal(GuiText.clip(outcome, 46)), x + 250, y, outcomeColour, false);
            }
            y += lineH();
        }

        void rule() {
            y += 3;
            g.fill(x, y, x + width, y + 1, RULE);
            y += 6;
        }

        /**
         * The hairline between two entries of a list. Fainter and tighter than
         * {@link #rule()}, which ends a section: several lines of description
         * and epigenetic numbers under every gene ran together into one block
         * without it, and a heavier rule would have read as a new section per
         * gene.
         */
        void divider() {
            y += 2;
            g.fill(x, y, x + width, y + 1, DIVIDER);
            y += 5;
        }

        void gap(int px) {
            y += px;
        }

        /**
         * A wrapped row of little horses with their names under them. Each is
         * drawn from its own record's coat, so the row is what those horses
         * actually look like rather than a set of icons.
         */
        void horses(List<HorseRecord> horses, int mouseX, int mouseY) {
            int perRow = Math.max(1, width / (FOAL_W + FOAL_GAP));
            int nameH = font.lineHeight + 1;
            for (int i = 0; i < horses.size(); i++) {
                HorseRecord horse = horses.get(i);
                int col = i % perRow;
                if (col == 0 && i > 0) {
                    y += FOAL_H + nameH + FOAL_GAP;
                }
                int hx = x + col * (FOAL_W + FOAL_GAP);
                HorsePortrait.draw(g, coatOf(horse), false, hx, y, FOAL_W, FOAL_H, mouseX, mouseY);
                HorseInfoScreen.this.drawFitted(g, horse.displayName(), hx, y + FOAL_H + 1, FOAL_W,
                        horse.sex() == com.example.horsegenetics.common.horse.Sex.FEMALE
                                ? EXPR_CARRIER : VALUE);
            }
            y += FOAL_H + nameH + 4;
        }

    }
}
