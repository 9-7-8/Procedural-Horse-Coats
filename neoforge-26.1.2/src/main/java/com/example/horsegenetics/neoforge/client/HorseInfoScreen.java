package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.breed.BreedStatCurve;
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
import com.example.horsegenetics.common.trait.HorseUnits;
import com.example.horsegenetics.common.trait.StatAxis;
import com.example.horsegenetics.common.trait.TraitBreakdown;
import com.example.horsegenetics.common.cart.CartDraft;
import com.example.horsegenetics.common.cart.CartKind;
import com.example.horsegenetics.common.trait.Traits;
import com.example.horsegenetics.common.coat.CoatData;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.entity.HorseTackSlot;
import com.example.horsegenetics.neoforge.network.FamilyTreeRequestPayload;
import com.example.horsegenetics.neoforge.network.HorseSocialSyncPayload;
import com.example.horsegenetics.neoforge.network.MountHorsePayload;
import com.example.horsegenetics.neoforge.network.TackSlotPayload;
import com.example.horsegenetics.neoforge.network.InspectHorsePayload;
import com.example.horsegenetics.neoforge.network.OffspringDataPayload;
import com.example.horsegenetics.neoforge.network.OffspringRequestPayload;
import com.example.horsegenetics.neoforge.network.SetBarnNamePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
 * <p><b>Every other horse named on it is a link.</b> A companion, a rival, a
 * descendant: clicking one opens that horse's own copy of this screen, with this
 * one as the parent, so Escape walks back the way you came. See
 * {@link #openHorse}.
 *
 * <p>Escape / Done goes back to the horse inventory screen it was opened from,
 * so the button is a detour rather than an exit.
 */
public final class HorseInfoScreen extends Screen {

    private enum Tab {
        OVERVIEW("Overview"),
        GEAR("Gear"),
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
    /** A horse's name you can click through to. The tab accent, doing its one job. */
    private static final int LINK = TAB_ACCENT;
    private static final int LINK_HOVER = 0xFF9ACCF2;

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
    /**
     * A tack slot, and the gap between two of them. Eighteen is vanilla's slot
     * - 16 of item with a pixel of well on each side - because these hold the
     * same saddle and the same barding, and a slot that is not slot-sized reads
     * as a picture of one rather than somewhere to click.
     */
    private static final int SLOT = 18;
    private static final int SLOT_GAP = 8;

    private static final int FOAL_W = 40;
    private static final int FOAL_H = 40;
    private static final int FOAL_GAP = 4;

    /**
     * The Gear tab's paper doll: the box the horse and its nineteen slots share.
     * Every slot's place inside it comes from {@code HorseTackSlot.anchorX} and
     * {@code anchorY} as a fraction of this box, so these are the only two
     * numbers the layout has and the roster carries the rest.
     */
    private static final int DOLL_W = 420;
    private static final int DOLL_H = 264;

    /** Where the horse itself sits inside the doll, leaving gutters for slots. */
    private static final float PORTRAIT_X0 = 0.27f;
    private static final float PORTRAIT_X1 = 0.73f;
    private static final float PORTRAIT_Y0 = 0.13f;
    private static final float PORTRAIT_Y1 = 0.64f;

    /**
     * The three-quarter view the doll opens on. Face-on would overlap the near
     * and off legs, and the four boot slots would then point at one visible
     * leg - which is why this, and not a side view, is where it starts.
     */
    private static final float DOLL_YAW = 52.0f;
    private static final float DOLL_PITCH = 6.0f;

    /**
     * How far the doll may be tipped. Past vertical the horse is being looked
     * at from directly above or below, where no slot anchor means anything and
     * the model reads as a shape rather than an animal.
     */
    private static final float DOLL_PITCH_LIMIT = 75.0f;

    /** Degrees turned per pixel dragged - a full turn in a little under the doll's width. */
    private static final float DOLL_DEGREES_PER_PIXEL = 2.0f;

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

    /** Set once the opening ancestry walk has been asked for; see {@link #init()}. */
    private boolean offspringAsked;

    /** Ticks since the screen opened, for the once-a-second hold heartbeat. */
    private int heldTicks;

    /** Where the tack slots landed this frame - see {@code Cursor.tack}. */
    private record TackHit(HorseTackSlot slot, int x, int y) {
    }

    private final List<TackHit> tackHits = new ArrayList<>();

    /**
     * <b>Which way the paper doll's horse is facing.</b> Seeded from
     * {@link #DOLL_YAW} / {@link #DOLL_PITCH} and then dragged: a slot is
     * anchored to a part of the animal, so the off-side boots and the tail slot
     * sit behind a horse the opening three-quarter view only half shows, and
     * being able to turn it is how you see what you are clicking. Per screen,
     * not static - reopening a horse starts from the canonical pose rather than
     * from however the last one was left.
     */
    private float dollYaw = DOLL_YAW;
    private float dollPitch = DOLL_PITCH;

    /**
     * The doll portrait's box as it was last drawn, or {@code null} on a frame
     * that drew no doll. Recorded on the way past exactly as {@link TackHit} is,
     * and for the same reason: it is where the drag has to start.
     */
    private @Nullable DollBox dollBox;

    private record DollBox(int x0, int y0, int x1, int y1) {
        boolean holds(double mx, double my) {
            return mx >= x0 && mx < x1 && my >= y0 && my < y1;
        }
    }

    /** True between pressing inside the doll and letting go. */
    private boolean draggingDoll;

    /**
     * Where a <b>link to another horse</b> landed this frame - a companion or a
     * rival on Overview, a descendant on Offspring. Recorded on the way past for
     * the same reason the tack slots are: the y it was drawn at is the scrolled
     * one, so working the geometry out a second time on the click is a second
     * chance to get it wrong.
     */
    private record LinkHit(int x0, int y0, int x1, int y1, UUID id) {
    }

    private final List<LinkHit> linkHits = new ArrayList<>();

    /**
     * A horse we have asked the server about and mean to open when it arrives -
     * see {@link #openHorse}. Null the rest of the time.
     */
    private @Nullable UUID pendingOpen;

    /** Ticks spent waiting on {@link #pendingOpen}, so a silent server is given up on. */
    private int pendingTicks;

    /** Three seconds. Long enough for a round trip, short enough not to fire later. */
    private static final int PENDING_GIVE_UP = 60;

    private Button rideButton;

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

        // Ask for the descendants as the screen opens, so the Offspring tab is
        // already filled the first time it is looked at. It used to be the
        // Refresh button alone, on the grounds that the walk is expensive - but
        // the cost lands on the server once per screen, the button was a step
        // every player had to be told about, and an empty tab reads as "no
        // foals" rather than "not asked". (Owner's call.) Refresh stays for a
        // second look at a horse whose page is already open.
        //
        // Guarded because init() runs again on every resize, and a window being
        // dragged must not become a stream of ancestry walks.
        if (!offspringAsked) {
            offspringAsked = true;
            requestOffspring();
        }

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
        live().barnName().ifPresent(barnBox::setValue);
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

        // Ride. It is on Overview with the tack because that is the tab about
        // this horse as an animal rather than as a genotype. The plain
        // right-click mounts again (HorseInfoInteraction takes only sneak), so
        // this is no longer the only way into the saddle - it stays because the
        // screen is where you decide you want *this* horse, and a wild one you
        // have just read about is one click from being tamed. See
        // MountHorsePayload.
        rideButton = Button.builder(Component.literal("Ride"), b -> mount())
                .bounds(contentRight() - buttonW("Ride"), contentTop() - 2, buttonW("Ride"), 16)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(
                        "Get on. A horse that is not yet tamed will try to throw you off - "
                                + "which is how it is tamed.")))
                .build();
        addRenderableWidget(rideButton);

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

    /** What {@link #ownsHorse()} said when the widgets were last applied. */
    private boolean lastOwned;

    private void applyTabWidgets() {
        lastOwned = ownsHorse();
        boolean owned = lastOwned;
        // Naming is for horses you own. The screen opens on anything now (sneak
        // and use - HorseInfoInteraction), so a stranger's horse and the cowboy's
        // stock both reach this, and a box you can type in but not submit is
        // worse than no box. The server refuses it as well; this is the half that
        // stops a player being told "no" only after they have typed a name.
        boolean overview = tab == Tab.OVERVIEW && owned;
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
        if (rideButton != null) {
            // A foal, a horse somebody is already on, and a cowboy's branded
            // string are all refused by the server - so the button is not
            // offered for them either. Taming is riding, so an untamed horse
            // keeps it.
            boolean rideable = tab == Tab.OVERVIEW && horse != null && !horse.isBaby()
                    && !horse.isVehicle() && !brandedStock();
            rideButton.visible = rideable;
            rideButton.active = rideable;
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

    /**
     * Whether the viewing player owns this horse, matching
     * {@code HorseOwnership.isOwner} on the server.
     *
     * <p><b>Read off the record, not the entity.</b> The obvious
     * {@code horse.getOwnerReference()} is always {@code null} here:
     * {@code AbstractHorse.defineSynchedData} registers one value, the flags
     * byte behind {@code isTamed()}, and keeps its owner in a plain field that
     * only ever goes to NBT. So the client sees that a horse is tamed and never
     * by whom, and asking the entity made this return {@code false} for
     * everybody - which is why the barn-name box below was invisible to the
     * owner of the horse as well as to a stranger. The record carries the owner
     * precisely so that this question has an answer on the client;
     * {@code HorseOwnerTrackingHandler} keeps it level with vanilla's.
     */
    private boolean ownsHorse() {
        if (horse == null || !horse.isTamed()) {
            return false;
        }
        var viewer = Minecraft.getInstance().player;
        return viewer != null && live().ownedBy(viewer.getUUID());
    }

    /**
     * Is this one of a cowboy's string? The brand is a synced attachment, so
     * this is the same answer the server's {@code TransferPaperHandler} gives -
     * the screen hides what that would refuse rather than offering a button
     * that does nothing.
     */
    private boolean brandedStock() {
        if (horse == null) {
            return false;
        }
        var brand = horse.getData(ModAttachments.COWBOY_BRAND.get());
        return brand != null && brand.isBranded();
    }

    /** The Ride button. The screen closes on the way - you cannot read it from the saddle. */
    private void mount() {
        if (horse == null) {
            return;
        }
        ClientPacketDistributor.sendToServer(new MountHorsePayload(horse.getId()));
        Minecraft.getInstance().setScreen(null);
    }

    /**
     * One tack slot: the well, the item in it, and the hover.
     *
     * <p>A slot the player cannot use is drawn flat and dim rather than hidden.
     * A foal has no saddle slot and never will until it grows, and a stranger's
     * horse has tack you may look at and not take - in both cases an empty gap
     * where a slot should be reads as a bug, and a greyed slot reads as the
     * rule it is.
     */
    private void drawTackSlot(GuiGraphicsExtractor g, HorseTackSlot slot, int x, int y,
                              int mouseX, int mouseY) {
        boolean usable = ownsHorse() && horse != null && slot.usableOn(horse);
        ItemStack worn = horse == null ? ItemStack.EMPTY : slot.on(horse);

        g.fill(x, y, x + SLOT, y + SLOT, FIELD_WELL);
        int edge = usable ? FIELD_EDGE : RULE;
        g.fill(x, y, x + SLOT, y + 1, edge);
        g.fill(x, y + SLOT - 1, x + SLOT, y + SLOT, edge);
        g.fill(x, y, x + 1, y + SLOT, edge);
        g.fill(x + SLOT - 1, y, x + SLOT, y + SLOT, edge);

        if (!worn.isEmpty()) {
            g.item(worn, x + 1, y + 1);
            g.itemDecorations(this.font, worn, x + 1, y + 1);
        }

        boolean over = mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT
                && mouseY >= pageTop() - 2 && mouseY <= panelBottom() - PAD;
        if (!over) {
            return;
        }
        g.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, 0x30FFFFFF);
        if (!worn.isEmpty()) {
            g.setTooltipForNextFrame(this.font, worn, mouseX, mouseY);
        } else {
            g.setTooltipForNextFrame(Component.literal(slot.hint()), mouseX, mouseY);
        }
    }

    /**
     * A click on a tack slot. One click swaps that slot with what is in your
     * hand - the server decides which direction and refuses anything that is
     * not yours; see {@code ModNetworking.handleTackSlot}.
     */
    private boolean clickTack(double mx, double my) {
        if (horse == null || !ownsHorse() || my < pageTop() - 2 || my > panelBottom() - PAD) {
            return false;
        }
        for (TackHit hit : tackHits) {
            if (mx >= hit.x() && mx < hit.x() + SLOT && my >= hit.y() && my < hit.y() + SLOT) {
                if (!hit.slot().usableOn(horse)) {
                    return true; // a foal's saddle slot: a real slot, and not yet usable
                }
                ClientPacketDistributor.sendToServer(
                        new TackSlotPayload(horse.getId(), hit.slot().name()));
                return true;
            }
        }
        return false;
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
        // Ownership arrives with the record, and the record is re-synced while
        // the screen is open - so a horse tamed or handed over with this screen
        // up would otherwise keep the naming box hidden until a tab switch.
        if (ownsHorse() != lastOwned) {
            applyTabWidgets();
        }
        if (pendingOpen != null) {
            HorseRecord arrived = ClientHorseRecordCache.byId(pendingOpen);
            if (arrived != null) {
                pendingOpen = null;
                show(arrived);
            } else if (++pendingTicks > PENDING_GIVE_UP) {
                // The server had nothing for it - a horse whose record has been
                // forgotten, or one this player may not read. Nothing happens,
                // which is what a click on a dead link should do.
                pendingOpen = null;
            }
        }
    }

    // ------------------------------------------------------------------
    // Going to another horse
    // ------------------------------------------------------------------

    /**
     * <b>Follow a name to the horse it names.</b> Clicking a companion, a rival
     * or a descendant opens that horse's own copy of this screen, with this one
     * as the parent - so Escape walks back the way you came, however far you
     * wandered.
     *
     * <p>The record usually comes straight out of
     * {@link ClientHorseRecordCache}: a horse being tracked nearby is put there
     * by its sync payload, and a descendant arrives with the Offspring answer.
     * When it is not there - a band-mate two hundred blocks away, a foal sold
     * years ago - the family-tree request is what fetches it, since that is
     * already the packet that means "the record for this id, please", and
     * {@link #tick()} opens the screen when the answer lands. It is the same
     * round trip the Family tree tab makes on every click, so this is a
     * heartbeat's wait at worst.
     */
    private void openHorse(@Nullable UUID id) {
        if (id == null || id.equals(record.id())) {
            return; // the horse whose screen this already is
        }
        HorseRecord known = ClientHorseRecordCache.byId(id);
        if (known != null) {
            show(known);
            return;
        }
        pendingOpen = id;
        pendingTicks = 0;
        ClientPacketDistributor.sendToServer(new FamilyTreeRequestPayload(id));
    }

    private void show(HorseRecord other) {
        Minecraft.getInstance().setScreen(new HorseInfoScreen(other, liveEntity(other.id()), this));
    }

    /**
     * The entity for a record, if that horse happens to be loaded. With one, the
     * screen shows live health, live attributes and the tack slots work; without
     * one it falls back to what the genotype says, which is what the whole screen
     * already does for a horse read off a family tree.
     */
    private static @Nullable AbstractHorse liveEntity(UUID id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        for (var entity : mc.level.entitiesForRendering()) {
            if (entity instanceof AbstractHorse found && found.getUUID().equals(id)) {
                return found;
            }
        }
        return null;
    }

    /** A click on a horse name or portrait. */
    private boolean clickLink(double mx, double my) {
        if (my < pageTop() - 2 || my > panelBottom() - PAD) {
            return false; // scrolled out from under the page - see the scissor
        }
        for (LinkHit hit : linkHits) {
            if (mx >= hit.x0() && mx < hit.x1() && my >= hit.y0() && my < hit.y1()) {
                openHorse(hit.id());
                return true;
            }
        }
        return false;
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

    /**
     * <b>The inventory key closes it, the way it closes vanilla's.</b> This is a
     * plain {@link Screen} rather than an {@code AbstractContainerScreen}, so it
     * gets none of that class's key handling for free - and since
     * {@link HorseInfoKeyHandler} is now what <kbd>E</kbd> opens from the saddle,
     * a screen that would not close on the same key is a dead end.
     *
     * <p>A focused text box owns the keyboard first: without the swallow, typing
     * a barn name with an "e" in it shuts the window. See
     * {@code HorseBrowserScreen.keyPressed} for the vanilla trap behind that -
     * {@code EditBox} returns false for an ordinary letter, because letters
     * arrive separately through {@code charTyped}, so swallowing the key here
     * does not cost the letter.
     */
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (super.keyPressed(event)) {
            return true; // includes Escape, which Screen turns into onClose()
        }
        if (barnBox != null && barnBox.isFocused() && barnBox.isActive()) {
            return true;
        }
        if (Minecraft.getInstance().options.keyInventory.matches(event)) {
            onClose();
            return true;
        }
        return false;
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
        if (tab == Tab.GEAR && clickTack(event.x(), event.y())) {
            return true;
        }
        // After the slots, deliberately: several of them overlap the animal, and
        // a click on a boot is a click on a boot even though the leg is under it.
        // What is left is the horse itself, which turns.
        if (tab == Tab.GEAR && event.button() == 0
                && dollBox != null && dollBox.holds(event.x(), event.y())) {
            draggingDoll = true;
            return true;
        }
        if (clickLink(event.x(), event.y())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    /**
     * <b>Drag the doll's horse to turn it.</b> Horizontal is yaw and wraps all
     * the way round; vertical is pitch and stops short of overhead
     * ({@link #DOLL_PITCH_LIMIT}), because past that the slot anchors stop
     * corresponding to anything a player can see.
     *
     * <p>The drag is not confined to the box it started in - once you have hold
     * of the horse you keep it until you let go, which is what every model
     * viewer does and what the hand expects.
     */
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingDoll) {
            dollYaw = Mth.wrapDegrees(dollYaw + (float) dx * DOLL_DEGREES_PER_PIXEL);
            dollPitch = Mth.clamp(dollPitch + (float) dy * DOLL_DEGREES_PER_PIXEL,
                    -DOLL_PITCH_LIMIT, DOLL_PITCH_LIMIT);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingDoll) {
            draggingDoll = false;
            return true;
        }
        return super.mouseReleased(event);
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
        tackHits.clear();
        linkHits.clear();
        dollBox = null;
        switch (tab) {
            case OVERVIEW -> drawOverview(c, mouseX, mouseY);
            case GEAR -> drawGear(c, mouseX, mouseY);
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
        g.text(this.font, Component.literal(live().barnName().isPresent()
                        ? "known around the yard as \"" + live().barnName().get() + "\""
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

    private void drawOverview(Cursor c, int mouseX, int mouseY) {
        boolean adult = horse == null || !horse.isBaby();

        // Tack used to be a row here. It moved to the Gear tab when the roster
        // went from two slots to nineteen - a row that wide would have pushed
        // the rest of Overview off the page, and nineteen slots want to be
        // arranged on a horse rather than in a line. What stays is a count, so
        // a player reading Overview still learns the horse is wearing
        // something and where to go for it. It sits at the very top because it
        // is the line most likely to be wrong at a glance - what the horse has
        // on is the thing you came to check, and everything below it is a fact
        // about the animal that will not have changed since you last looked.
        if (horse != null) {
            c.label("Tack");
            int worn = wornCount();
            c.wrapped(worn == 0
                    ? "Wearing nothing. The Gear tab is where you dress it."
                    : "Wearing " + worn + (worn == 1 ? " piece" : " pieces")
                            + ". The Gear tab has the slots.", DESC, 0);
            c.rule();
        }

        c.pair("Sex", live().sexLabel(adult));
        c.pair("Generation", Integer.toString(live().generation()));
        c.pair("Breed", live().lineage().displayName());
        c.rule();

        c.label("Body");
        if (horse != null) {
            double cur = horse.getHealth();
            double max = horse.getAttributeValue(Attributes.MAX_HEALTH);
            c.pair("Health", String.format("%.1f / %.1f  (%.0f hearts)", cur, max, Math.ceil(max / 2.0)),
                    cur < max * 0.4 ? BAD : cur >= max ? GOOD : VALUE);
            // Both in units a player can picture: how fast it runs, and how
            // high a block it can get onto. The raw attributes are what the
            // simulation uses and what the designer shows, but neither number
            // means anything on sight - 0.1875 is not a speed to anybody.
            c.pair("Speed", String.format("%.1f m/s",
                    HorseUnits.metresPerSecond(horse.getAttributeValue(Attributes.MOVEMENT_SPEED))));
            c.pair("Jump", String.format("%.1f m",
                    HorseUnits.jumpMetres(horse.getAttributeValue(Attributes.JUMP_STRENGTH))));
            // Pull sits with the other four body numbers because it is one of
            // them - the fifth body stat, not a cart footnote. It reads off the
            // genotype rather than an attribute because there is no pull
            // attribute: nothing in the game engine needs one. The Draught
            // block further down says what the score is worth.
            c.pair("Pull", String.format("%.1f / 10  (%s)", traits().pull(), pullWord(traits().pull())));
            double scale = horse.getAttributeValue(Attributes.SCALE);
            c.pair("Size", sizeWord(scale) + "  " + heightText(scale, horse.isBaby())
                    + String.format("  (%.2f)", scale));
            // A mare's heat / pregnancy / nursing, in words from the server. It
            // rides the social summary, so it appears a second after opening.
            ClientHorseSocialCache.Social summary = ClientHorseSocialCache.get(horse.getId());
            if (summary != null && !summary.breeding().isEmpty()) {
                c.pair("Breeding", summary.breeding());
            }
        } else {
            Traits t = traits();
            c.pair("Health", String.format("%.1f", t.health()) + "  (from the genotype)");
            c.pair("Speed", String.format("%.3f", t.speed()));
            c.pair("Jump", String.format("%.2f", t.jump()));
            c.pair("Pull", String.format("%.1f / 10  (%s)", t.pull(), pullWord(t.pull())));
            c.pair("Size", sizeWord(t.scale()) + "  " + heightText(t.scale(), false));
        }

        drawDraught(c);

        // Bond used to hang off the bottom of the tack block; with that block
        // gone to the top it needs its own rule, or it reads as a Draught row.
        ClientHorseCareCache.Care care = horse == null ? null : ClientHorseCareCache.get(horse.getId());
        if (care != null) {
            c.rule();
            c.pair("Bond", care.bond() + "  " + bondTierLabel(care.bond())
                    + (care.inHerd() ? "   • in a herd" : ""));
        }

        ClientHorseSocialCache.Social social = horse == null ? null : ClientHorseSocialCache.get(horse.getId());
        if (social != null) {
            c.rule();
            c.label("Social");
            c.pair("Role", social.role());
            c.wrapped(social.roleDescription(), DESC, 8);
            if (!social.standing().isEmpty()) {
                c.pair("Standing", social.standing());
            }
            // Each name is a way to that horse. A band-mate is the one thing on
            // this page that is another animal you could be reading instead.
            if (social.companions().isEmpty()) {
                c.pair("Companions", "none yet");
            } else {
                c.links("Companions", social.companions(), mouseX, mouseY);
            }
            social.rival().ifPresent(r -> c.links("Rival", List.of(r), mouseX, mouseY));
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
        c.pair("Bred by", live().bredBy().orElse("— (not bred in captivity)"));
        c.pair("Tamed by", live().tamedBy().orElse("— (never tamed)"));
        String owner = currentOwnerName();
        if (owner != null && !owner.equals(live().tamedBy().orElse(null))) {
            c.pair("Owner now", owner);
        }
    }

    /**
     * <b>What this horse is worth in harness</b>, on the overview page.
     *
     * <p>Pulling ability is a 1-10 score and, on its own, means nothing to a
     * player: five is "ordinary" only if you already know that. So the score is
     * shown with the two things it actually decides - how much of its speed
     * this horse would keep pulling the heaviest vehicle in the game, and
     * whether its back takes a second rider - and then with the one number that
     * is about this animal's history rather than its genotype: how far it has
     * hauled.
     *
     * <p>The wagon is the yardstick because it is the extreme. A figure for the
     * animal cart would flatter every horse alive and separate none of them.
     * Empty and full, because since cargo started counting towards the load
     * those are two quite different animals' worth of work.
     *
     * <p>Haulage comes off the synced {@code CART_METRES} attachment rather
     * than any player statistic: it is a fact about the horse, and it is the
     * line that makes a working animal's screen different from a fresh one's.
     */
    private void drawDraught(Cursor c) {
        Traits t = traits();
        c.rule();
        c.label("Draught");

        // The live entity's speed if we have one, so tack and effects count;
        // the genotype's otherwise.
        double speed = horse != null ? horse.getAttributeValue(Attributes.MOVEMENT_SPEED) : t.speed();
        int keptPercent = (int) Math.round(100.0
                * CartDraft.retention(t.pull(), speed, CartKind.WAGON.load()));
        c.pair("Hauling a wagon", keptPercent + "% of its speed",
                keptPercent >= 70 ? GOOD : keptPercent < 45 ? BAD : VALUE);
        // The empty figure alone stopped being the whole story once cargo
        // counted: the second line is the one that decides whether this horse
        // can take the trade run, and it is where a weak horse falls apart.
        int ladenPercent = (int) Math.round(100.0 * CartDraft.retention(t.pull(), speed,
                CartDraft.loaded(CartKind.WAGON.load(), CartKind.WAGON.cargoShare(), 1.0)));
        c.pair("...loaded to the roof", ladenPercent + "% of its speed",
                ladenPercent >= 70 ? GOOD : ladenPercent < 45 ? BAD : VALUE);
        c.pair("Carries", CartDraft.carriesTwoRiders(t.pull()) ? "two riders" : "one rider");

        if (horse != null) {
            double metres = horse.getData(ModAttachments.CART_METRES.get());
            c.pair("Hauled", metres < 1.0
                    ? "never been in harness"
                    : metres < 1000.0
                        ? String.format("%.0f m", metres)
                        : String.format("%.2f km", metres / 1000.0));
        }
    }

    /** The 1-10 pull score in words, so the number means something on sight. */
    private static String pullWord(double pull) {
        if (pull < 2.0) {
            return "feeble";
        }
        if (pull < 4.0) {
            return "light";
        }
        if (pull < 6.0) {
            return "ordinary";
        }
        if (pull < 7.5) {
            return "strong";
        }
        return pull < 9.0 ? "powerful" : "a true draft horse";
    }

    /**
     * <b>The record as it is now, not as it was when the screen opened.</b>
     *
     * <p>{@link #record} is the snapshot the screen was constructed with, and
     * it never changes. That is fine for the genotype - which cannot - but the
     * barn name can, from this very screen: pressing <i>Set</i> sends
     * {@code SetBarnNamePayload}, the server applies it and syncs the new
     * record back, and every reader here carried on printing the old one. The
     * horse's floating nameplate changed and the screen did not, which reads
     * exactly like a button that does nothing.
     *
     * <p>So anything a player can change is read through here. The identity
     * check is not paranoia: entity ids are recycled within a session, and
     * drawing another horse's name onto this screen would be a worse bug than
     * the one this fixes.
     */
    private HorseRecord live() {
        if (horse != null) {
            HorseRecord fresh = ClientHorseRecordCache.get(horse.getId());
            if (fresh != null && fresh.id().equals(record.id())) {
                return fresh;
            }
        }
        return record;
    }

    private String fullName() {
        return (live().firstName() + " " + live().lastName()).strip();
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
     * Height at the withers in hands, off {@link BreedStatCurve#handsFor} - the
     * inverse of how a breed's hands range becomes a scale, so the two agree. A
     * foal's scale attribute is its adult one (age shrinks it separately), so
     * for a foal this is the height it will grow to, and says so.
     */
    private static String heightText(double scale, boolean baby) {
        String hh = BreedStatCurve.formatHands(BreedStatCurve.handsFor(scale));
        return baby ? hh + " grown" : hh;
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
    // Gear
    // ------------------------------------------------------------------

    /** How many of the nineteen slots are filled. Overview reports this. */
    private int wornCount() {
        if (horse == null) {
            return 0;
        }
        int worn = 0;
        for (HorseTackSlot slot : HorseTackSlot.values()) {
            if (!slot.on(horse).isEmpty()) {
                worn++;
            }
        }
        return worn;
    }

    /**
     * <b>The Gear tab.</b> Nineteen slots arranged on the horse itself - see
     * {@code Cursor.gear} for the doll, and {@code HorseTackSlot} for why only
     * two of them are real equipment slots.
     *
     * <p>Like the tack row it replaces, it is drawn only for a horse that is
     * actually here: this screen also opens on a record with no entity behind
     * it (from the browser), and there is nothing to dress.
     */
    private void drawGear(Cursor c, int mouseX, int mouseY) {
        if (horse == null) {
            c.wrapped("This horse is a record rather than an animal standing in front of you, "
                    + "so there is nothing here to dress.", DESC, 0);
            return;
        }

        c.gear(mouseX, mouseY);

        if (!ownsHorse()) {
            c.wrapped("Not your horse - you can see what it is wearing and no more.", DESC, 0);
        } else if (horse.isBaby()) {
            c.wrapped("A foal wears nothing. Tack sized for an adult is the one thing a growing "
                    + "horse should not be carrying, so every slot is closed until it grows up.",
                    DESC, 0);
        } else {
            c.wrapped("Click a slot to put on what you are holding, or to take off what is there.",
                    DESC, 0);
        }

        // Honesty about the state of the roster. Seventeen of these nineteen
        // slots have no item in the world that fits them yet - the slots, the
        // storage and the screen landed first, deliberately, and each piece
        // joins its slot's tag as it is made. Without this line the tab reads
        // as broken rather than as unfinished.
        c.gap(4);
        c.wrapped("Only the saddle and the barding have anything to put in them so far. "
                + "The other seventeen slots are built and empty - the gear that fills them "
                + "is still to be made.", DIM_TEXT, 0);
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
        c.pair("Size", String.format("%.2f", t.scale()) + "   " + sizeWord(t.scale())
                + "   " + heightText(t.scale(), false));
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
            case PULL -> signed(term.pull(), 1);   // a score, and nothing multiplies it
        };
        boolean helps = switch (axis) {
            case SPEED -> term.speed() > 0 || term.speedFactor() > 1.0;
            case HEALTH -> term.health() > 0 || term.healthFactor() > 1.0;
            case JUMP -> term.jump() > 0 || term.jumpFactor() > 1.0;
            case PULL -> term.pull() > 0;
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
            case PULL -> "Pulling ability";
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
                        ? "as of when this page was opened"
                        : "asking the server..."),
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
                            : "Waiting on the server. Opening this page asks it to walk the whole "
                                    + "ancestry table and send a full record for every descendant, "
                                    + "so give it a moment - and Refresh asks again.",
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

        /**
         * A labelled row of <b>horse names you can click</b>, comma-separated in
         * the value column where {@link #pair} would have put one string. Each
         * name is measured as it is drawn, so the box recorded for the click is
         * the box the name is in.
         *
         * <p><b>The value column wraps.</b> A name that would not fit before the
         * right edge starts a fresh line indented to the same column, so a horse
         * with a full band of companions reads as a short list rather than one
         * run of names off the side of the panel - where the overflow was not
         * only unreadable but unclickable, the hit boxes being recorded outside
         * the panel with it. The comma stays on the line it ends, never leading
         * the next one.
         */
        void links(String label, List<HorseSocialSyncPayload.Companion> items, int mouseX, int mouseY) {
            g.text(font, Component.literal(label), x, y, LABEL, false);
            int left = x + 96;
            int right = x + width;
            int lx = left;
            for (int i = 0; i < items.size(); i++) {
                HorseSocialSyncPayload.Companion item = items.get(i);
                int w = font.width(item.label());
                // Wrap before drawing, never mid-name; the first name on a line
                // is drawn even when it alone overruns, since there is nowhere
                // narrower to put it.
                if (lx > left && lx + w > right) {
                    y += lineH();
                    lx = left;
                }
                boolean hover = mouseX >= lx && mouseX < lx + w
                        && mouseY >= y && mouseY < y + lineH();
                g.text(font, Component.literal(item.label()), lx, y,
                        hover ? LINK_HOVER : LINK, false);
                if (hover) {
                    g.fill(lx, y + font.lineHeight, lx + w, y + font.lineHeight + 1, LINK_HOVER);
                }
                linkHits.add(new LinkHit(lx, y, lx + w, y + lineH(), item.id()));
                lx += w;
                if (i < items.size() - 1) {
                    g.text(font, Component.literal(","), lx, y, DIM_TEXT, false);
                    lx += font.width(", ");
                }
            }
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
         * <b>The paper doll.</b> The horse's own portrait in the middle and the
         * nineteen slots arranged around it where the gear actually sits - so
         * the near fore boot is under the near fore leg and the tail slot is
         * behind the tail, and a player finds a slot by looking at the horse
         * rather than by reading a list.
         *
         * <p>Every position comes from {@code HorseTackSlot.anchorX/anchorY} as
         * a fraction of the doll's box, so the layout is the roster and adding
         * a slot moves nothing here.
         *
         * <p>Like the row it replaced, it records where each slot landed on the
         * way past ({@link #tackHits}) rather than working the geometry out
         * twice - which is what keeps a scrolled page's slots clickable in the
         * right place, since the y it is drawn at is already the scrolled one.
         */
        void gear(int mouseX, int mouseY) {
            int dollW = Math.min(DOLL_W, width);
            int dollH = DOLL_H;
            int dx = x + (width - dollW) / 2;

            // The horse first, so a slot that overlaps it reads as sitting on
            // the animal rather than behind it.
            int px = dx + Math.round(dollW * PORTRAIT_X0);
            int pw = Math.round(dollW * (PORTRAIT_X1 - PORTRAIT_X0));
            int py = y + Math.round(dollH * PORTRAIT_Y0);
            int ph = Math.round(dollH * (PORTRAIT_Y1 - PORTRAIT_Y0));
            HorsePortrait.drawPosed(g, coatOf(record), horse != null && horse.isBaby(),
                    px, py, pw, ph, dollYaw, dollPitch);
            dollBox = new DollBox(px, py, px + pw, py + ph);

            for (HorseTackSlot slot : HorseTackSlot.values()) {
                int sx = dx + Math.round((dollW - SLOT) * slot.anchorX());
                int sy = y + Math.round((dollH - SLOT) * slot.anchorY());
                drawTackSlot(g, slot, sx, sy, mouseX, mouseY);
                tackHits.add(new TackHit(slot, sx, sy));
            }
            y += dollH + 4;
        }

        /**
         * A wrapped row of little horses with their names under them. Each is
         * drawn from its own record's coat, so the row is what those horses
         * actually look like rather than a set of icons.
         *
         * <p><b>Each one is a link to that horse's own page.</b> The whole
         * portrait-and-name block is the target rather than the name alone: at
         * forty pixels the picture is the thing the eye is on, and a player who
         * has just spotted a foal they like should not have to find the caption.
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
                int block = FOAL_H + nameH;
                boolean hover = mouseX >= hx && mouseX < hx + FOAL_W
                        && mouseY >= y && mouseY < y + block;
                HorsePortrait.draw(g, coatOf(horse), false, hx, y, FOAL_W, FOAL_H, mouseX, mouseY);
                HorseInfoScreen.this.drawFitted(g, horse.displayName(), hx, y + FOAL_H + 1, FOAL_W,
                        hover ? LINK_HOVER
                                : horse.sex() == com.example.horsegenetics.common.horse.Sex.FEMALE
                                        ? EXPR_CARRIER : VALUE);
                linkHits.add(new LinkHit(hx, y, hx + FOAL_W, y + block, horse.id()));
            }
            y += FOAL_H + nameH + 4;
        }

    }
}
