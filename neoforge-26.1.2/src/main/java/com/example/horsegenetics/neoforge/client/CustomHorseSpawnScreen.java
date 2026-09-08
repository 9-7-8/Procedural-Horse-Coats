package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.BreedFounder;
import com.example.horsegenetics.common.breed.BreedLineage;
import com.example.horsegenetics.common.breed.Breeds;
import com.example.horsegenetics.common.coat.CoatData;
import com.example.horsegenetics.common.coat.pattern.HairPattern;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneCodeDisplay;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.Inheritance;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.spec.HorseAbilities;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.common.trait.Traits;
import com.example.horsegenetics.neoforge.NeoRng;
import com.example.horsegenetics.neoforge.network.SpawnCustomHorsePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.Horse;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The editor behind the <b>custom horse spawn egg</b>: build a horse gene by
 * gene, watch it, then spawn exactly that horse.
 *
 * <h2>Layout</h2>
 * <b>Left</b> - every registered gene in <b>alphabetical order by display
 * name</b> ({@link Gene#name()}, so the list reads ACAN, Agouti, B4GALT7,
 * Champagne, EDNRB (frame overo), KIT (white spotting)&hellip; rather than in
 * the registry's processing order).
 * <b>Centre</b> - a live 3D horse in the coat the genome makes.
 * <b>Right</b> - age, sex, an epigenetics re-roll, code copy / paste, and spawn.
 *
 * <h2>The list is a catalogue you add from</h2>
 * A gene starts <b>off</b> the horse and is drawn as a plain name;
 * <b>clicking the row adds it</b>, and only then does it grow its two allele
 * buttons and an {@code x} to take it off again. Every gene carrying two
 * baseline buttons from the start was a wall of {@code N/N} that said nothing -
 * a horse carries every locus whether or not you have touched it, so what the
 * list is actually for is picking the handful you want to see.
 *
 * <p><b>A gene is added homozygous for the allele that does something</b> -
 * you clicked it to look at it, not to leave it silent. That means the first
 * allele that is not the gene's {@link Gene#defaultAllele()}, doubled; if a
 * horse cannot carry that pair ({@code KIT}'s nonviable {@code W}
 * homozygotes), the next one that it can, and failing all of them one copy
 * against the baseline. Either copy can then be cycled anywhere, baseline
 * included.
 *
 * <h2>The epigenome is part of what you are looking at</h2>
 * The screen holds a real {@link Epigenome} and previews with it.
 * <b>Reroll epi.</b> draws a new one and the preview redraws - the first place
 * per-allele epigenetics are directly visible, and how you flip through the bay
 * leg heights or grey dapplings one genotype can produce. It is then sent
 * <i>with</i> the genotype and written into the founder record, so the horse
 * that appears is the horse that was on screen; the server used to roll its
 * own, which made the preview a suggestion.
 *
 * <h2>This screen has a twin - change both</h2>
 * <b>{@code wiki/horse-designer/} is the browser version of this screen</b>, and
 * the two are meant to behave identically: same gene list, same click-to-add,
 * same allele buttons and dropdown-past-three-alleles, same right-hand column in
 * the same order, same short-form line and epigenetic fingerprint underneath.
 * It exists so a coat can be looked at without launching the game, which only
 * works if what it shows is what this shows.
 *
 * <p><b>A change here belongs there in the same commit.</b> Most of it will be
 * a change in Java either way: the designer's model is
 * {@code web/HorseEditor.java}, a deliberate twin of the state and the rules on
 * this class - {@code variantPair}, {@code enforceSexLinkage},
 * {@code applyGenome} and {@code randomizeGenes} are all mirrored there under
 * the same names. Only the drawing is JavaScript
 * ({@code wiki/horse-designer/js/gui.js}), and it copies the layout constants
 * and colours below by value, so a moved widget is a two-line change.
 *
 * <p>The two deliberate divergences: the browser has nothing to spawn, so
 * <b>Spawn</b> and <b>Cancel</b> are replaced by <b>Wander</b> and <b>Reset
 * view</b>; and it cannot draw the cutie-mark item icons or the particle
 * emitters, which come from the game's own registries.
 *
 * <p><b>The creative gate belongs to that first divergence.</b> This screen's
 * Spawn button reads <i>Spawn (creative only)</i> and is inactive outside
 * creative; the browser has no game mode and its button in that slot is
 * Wander, so there is nothing to mirror. Everything <em>above</em> that button
 * - the gene list, the preview, the epigenome, the code box - works in every
 * game mode on both, which is the property worth preserving if the gate ever
 * moves: the editor is a viewer that can also spawn, not a creative tool.
 *
 * <p><b>A third divergence, in when the gene list is taken.</b> Both this class
 * and {@code HorseEditor} snapshot {@code Genes.codeOrder()} in their
 * constructor - they have to, because both address gene rows by index. In the
 * game that is safe at any moment: {@code Genes} registers the shipped gene
 * files from its own class initialiser, so the registry is complete before
 * anything can hold a reference to it. In the browser it is not, because TeaVM
 * cannot read a classpath index and the page has to hand the genes in
 * afterwards - which is why {@code DesignerApi} builds its editor lazily and
 * drops it on every registration. Nothing to mirror here; the note exists so
 * that the next person to see the two constructors side by side knows the
 * difference is deliberate.
 *
 * <h2>Notes</h2>
 * Everything here is client-only, and the spawn is <b>creative-only, re-checked
 * on the server</b> ({@code ModNetworking.handleSpawnCustomHorse}).
 *
 * <p>The preview follows {@code FamilyTreeScreen.drawHorseModel}: a throwaway
 * client-only {@link Horse} whose render state gets the coat injected directly,
 * deliberately <b>without</b> touching {@link ClientCoatCache} - an edited
 * horse must never leak into how the world renders.
 *
 * <p>Drawing note: this is the retained-mode GUI. Widgets ({@link Button}s) are
 * rendered as part of {@code super.extractRenderState}, so anything this screen
 * paints afterwards lands <b>on top</b> of them - so the backdrop is drawn only
 * where no widget lives.
 */
public final class CustomHorseSpawnScreen extends Screen {

    private static final int ROW_H = 20;
    private static final int LIST_X = 8;
    private static final int LIST_TOP = 40;
    private static final int ALLELE_W = 38;
    private static final int REMOVE_W = 14;
    private static final int RIGHT_W = 96;
    private static final int RIGHT_STEP = 22;
    private static final int PANEL = 0x90000000;

    /** Many-allele genes (particle, KIT, ...) get a scrollable list instead of a cycle button. */
    private static final int DD_ROW_H = 12;
    private static final int DD_VISIBLE = 8;
    private static final int DD_W = 76;
    /** The breed picker's dropdown is wider - breed names are long. */
    private static final int BREED_DD_W = 118;

    private boolean baby = false;
    private boolean female = true;
    private int scroll = 0;

    /** Preview view: click-drag orbits, wheel zooms; the horse always walks in place. */
    private float previewYaw = -30f;
    private float previewPitch = 8f;
    private float previewZoom = 1f;
    private boolean draggingPreview = false;
    private final long screenOpenedAt = System.currentTimeMillis();

    /** The open many-allele dropdown, or {@code null}. Anchored at ({@link #ddX}, {@link #ddY}). */
    private Row ddRow;
    private int ddSlot;
    private int ddScroll;
    private int ddX;
    private int ddY;
    /** True while the breed picker's dropdown is open (mutually exclusive with {@link #ddRow}). */
    private boolean breedDd;

    /**
     * GUI-space "dust" motes for the particle-locus preview. The real emitter
     * is server-side ({@code GeneAbilityHandler}, {@code EntityTickEvent.Post},
     * client-guarded) and the preview horse is a throwaway entity that never
     * ticks, so nothing would show otherwise. This is a rough stand-in - a
     * cloud round the horse in the emitter's colour, not a placed-in-world
     * particle - enough to preview "this horse trails something, in this
     * colour".
     */
    private final java.util.List<Mote> motes = new java.util.ArrayList<>();
    private final java.util.Random previewRng = new java.util.Random();
    private long lastFrameNanos = System.nanoTime();
    private double emitAccumulator = 0.0;
    private static final int MOTE_CAP = 160;

    private static final class Mote {
        float x;
        float y;
        float vx;
        float vy;
        float age;
        float life;
        float size;
        int rgb;
    }

    /** 0 = no preset; 1..N = Breeds.all().get(index-1). A preset also stamps the spawned horse's breed. */
    private int breedIndex = 0;
    private final java.util.List<Breed> breedChoices = Breeds.all();

    private final List<Row> rows = new ArrayList<>();
    private Epigenome epigenome;

    /** Cached preview, rebuilt only when the genome actually changes. */
    private CoatData previewCoat;
    private String previewKey = "";
    private Horse previewHorse;
    private boolean previewHorseIsBaby;

    /**
     * One gene in the list. {@code added} is held separately rather than
     * inferred from the alleles, so a gene you added and then cycled back to
     * its baseline stays on the horse instead of silently leaving the list.
     */
    private static final class Row {
        final Gene gene;
        boolean added;
        int a;
        int b;

        Row(Gene gene) {
            this.gene = gene;
            int def = indexOf(gene, gene.defaultAllele());
            this.a = def;
            this.b = def;
        }
    }

    public CustomHorseSpawnScreen() {
        super(Component.literal("Custom Horse Spawn Egg"));
        for (Gene gene : Genes.codeOrder()) {
            if (gene == Genes.SEX) {
                continue; // the Mare / Stallion button owns that locus
            }
            rows.add(new Row(gene));
        }
        rows.sort(Comparator.comparing(r -> r.gene.name(), String.CASE_INSENSITIVE_ORDER));
        this.epigenome = rollEpigenome();
    }

    private static int indexOf(Gene gene, Allele allele) {
        List<Allele> as = gene.alleles();
        for (int i = 0; i < as.size(); i++) {
            if (as.get(i).equals(allele)) {
                return i;
            }
        }
        return 0;
    }

    private static Epigenome rollEpigenome() {
        return Epigenome.random(new NeoRng(RandomSource.create()));
    }

    /**
     * Open the breed picker - a scrollable dropdown of {@code "(none)"} plus
     * every {@link Breeds#all() breed}, anchored at the Breed button.
     * Choosing a real breed rolls a fresh wild founder of it
     * ({@link BreedFounder#roll}) straight into the editor - genotype,
     * epigenome and sex - and stamps that breed on whatever is spawned (you can
     * still hand-edit any locus afterwards). {@code "(none)"} leaves the current
     * genome alone and spawns as Feral Mixed.
     */
    private void openBreedDropdown(int anchorX, int anchorY) {
        breedDd = true;
        ddRow = null;
        int h = DD_VISIBLE * DD_ROW_H;
        ddX = anchorX;
        ddY = Math.max(LIST_TOP, Math.min(anchorY, this.height - h - 4));
        int max = Math.max(0, breedChoices.size() + 1 - DD_VISIBLE);
        ddScroll = Math.max(0, Math.min(breedIndex - DD_VISIBLE / 2, max));
    }

    private void pickBreedFromDropdown(double mx, double my) {
        int h = DD_VISIBLE * DD_ROW_H;
        if (mx >= ddX && mx < ddX + BREED_DD_W && my >= ddY && my < ddY + h) {
            int idx = ddScroll + (int) ((my - ddY) / DD_ROW_H);
            if (idx >= 0 && idx <= breedChoices.size()) {
                breedIndex = idx;
                if (idx != 0) {
                    applyBreedPreset(breedChoices.get(idx - 1));
                }
            }
        }
        closeDropdown();
        rebuildWidgets();
    }

    private void applyBreedPreset(Breed breed) {
        applyGenome(BreedFounder.roll(breed, new NeoRng(RandomSource.create())), true);
    }

    /**
     * Stamp a whole rolled genome onto the editor - every locus, the epigenome,
     * optionally the sex. A locus the roll left at its baseline drops off the
     * list, so a mostly-plain roll does not come back as 40 rows of {@code N/N}.
     */
    private void applyGenome(Genome g, boolean adoptSex) {
        Genotype gt = g.genotype();
        if (adoptSex) {
            female = gt.sex() == Sex.FEMALE;
        }
        for (Row row : rows) {
            AllelePair pair = gt.pair(row.gene);
            int def = indexOf(row.gene, row.gene.defaultAllele());
            row.a = indexOf(row.gene, pair.first());
            row.b = indexOf(row.gene, pair.second());
            row.added = !(row.a == def && row.b == def);
        }
        epigenome = g.epigenome();
        previewKey = "";
    }

    /**
     * <b>Randomize genes.</b> With a breed selected this is a fresh
     * {@link BreedFounder#roll} of it (so the alleles stay inside that breed's
     * pools and stat targets); with "(none)" it is an unconstrained
     * {@link Genome#random}. The chosen sex is kept either way.
     */
    private void randomizeGenes() {
        NeoRng rng = new NeoRng(RandomSource.create());
        Genome g = breedIndex == 0
                ? Genome.random(rng)
                : BreedFounder.roll(breedChoices.get(breedIndex - 1), rng,
                        female ? Sex.FEMALE : Sex.MALE);
        applyGenome(g, false);
        closeDropdown();
        rebuildWidgets();
    }

    private void openDropdown(Row row, int slot, int anchorX, int anchorY) {
        breedDd = false;
        ddRow = row;
        ddSlot = slot;
        ddX = anchorX;
        int h = DD_VISIBLE * DD_ROW_H;
        ddY = Math.max(LIST_TOP, Math.min(anchorY, this.height - h - 4));
        int cur = slot == 0 ? row.a : row.b;
        int max = Math.max(0, row.gene.alleles().size() - DD_VISIBLE);
        ddScroll = Math.max(0, Math.min(cur - DD_VISIBLE / 2, max));
    }

    private void closeDropdown() {
        ddRow = null;
        breedDd = false;
    }

    private void pickFromDropdown(double mx, double my) {
        List<Allele> as = ddRow.gene.alleles();
        int h = DD_VISIBLE * DD_ROW_H;
        if (mx >= ddX && mx < ddX + DD_W && my >= ddY && my < ddY + h) {
            int idx = ddScroll + (int) ((my - ddY) / DD_ROW_H);
            if (idx >= 0 && idx < as.size()) {
                if (ddSlot == 0) {
                    ddRow.a = idx;
                } else {
                    ddRow.b = idx;
                }
                enforceSexLinkage(ddRow);
            }
        }
        closeDropdown();
        rebuildWidgets();
    }

    private boolean inPreview(double mx, double my) {
        return mx >= previewLeft() && mx <= previewRight()
                && my >= LIST_TOP && my <= this.height - 30;
    }

    /**
     * What a freshly added gene lands on: <b>a combination that actually
     * shows</b> - you added the row to see the gene, not to leave it silent.
     *
     * <p>Candidates are tried in order of how obvious they are - each variant
     * allele homozygous, then two <i>different</i> variant alleles, then one
     * variant against the baseline - and the first that both
     * {@link Gene#canOccur} and is not a wild type wins. A pair the gene rules
     * out is skipped (KIT's four nonviable {@code W} homozygotes, MET's
     * {@code met/met}); if nothing in the list expresses, the first carryable
     * candidate is used anyway, and if there is no variant at all the row stays
     * on the baseline.
     *
     * <p>The wild-type test is what the two-different-alleles rung is for.
     * Magic sectoral heterochromia is the gene that needs it: every one of its
     * homozygotes is silent by design, so the old "first variant homozygote"
     * rule would have added the row and shown nothing.
     */
    private static AllelePair variantPair(Gene gene) {
        Allele base = gene.defaultAllele();
        List<Allele> alleles = gene.alleles();
        List<AllelePair> candidates = new ArrayList<>();
        for (Allele a : alleles) {
            if (!a.equals(base)) {
                candidates.add(new AllelePair(a, a));
            }
        }
        for (int i = 0; i < alleles.size(); i++) {
            for (int j = i + 1; j < alleles.size(); j++) {
                Allele a = alleles.get(i);
                Allele b = alleles.get(j);
                if (!a.equals(base) && !b.equals(base)) {
                    candidates.add(new AllelePair(a, b));
                }
            }
        }
        for (Allele a : alleles) {
            if (!a.equals(base)) {
                candidates.add(new AllelePair(a, base));
            }
        }
        AllelePair carryable = null;
        for (AllelePair pair : candidates) {
            if (!gene.canOccur(pair)) {
                continue;
            }
            if (carryable == null) {
                carryable = pair;
            }
            if (!gene.expressionOf(pair).wildType()) {
                return pair;
            }
        }
        return carryable != null ? carryable : new AllelePair(base, base);
    }

    /**
     * Keep a sex-linked row honest against the current {@link #female} flag -
     * a stallion has one {@code X} and cannot carry two real copies of an
     * {@code X}-linked gene (a {@code Brn/Brn} stallion, say), and the mirror
     * holds for a {@code Y}-linked one. Neither allele button nor the Sex
     * button knows about the other on its own, so this runs after every edit
     * to either. A no-op on an autosomal gene.
     */
    private void enforceSexLinkage(Row row) {
        Inheritance mode = row.gene.inheritance();
        if (!mode.sexLinked()) {
            return;
        }
        int placeholderIdx = indexOf(row.gene, row.gene.hemizygousPlaceholder());
        int defaultIdx = indexOf(row.gene, row.gene.defaultAllele());
        boolean aReal = row.a != placeholderIdx;
        boolean bReal = row.b != placeholderIdx;
        int allowedReal = mode.copiesIn(female ? Sex.FEMALE : Sex.MALE);
        int haveReal = (aReal ? 1 : 0) + (bReal ? 1 : 0);
        if (haveReal == allowedReal) {
            return;
        }
        switch (allowedReal) {
            case 2 -> {
                // both slots must hold a real allele
                if (!aReal) {
                    row.a = bReal ? row.b : defaultIdx;
                }
                if (!bReal) {
                    row.b = aReal ? row.a : defaultIdx;
                }
            }
            case 1 -> {
                // exactly one real allele, the other slot the placeholder
                if (haveReal == 0) {
                    row.a = defaultIdx;
                    row.b = placeholderIdx;
                } else {
                    row.b = placeholderIdx; // keep a's real allele
                }
            }
            default -> {
                // 0 real allowed (a mare at a Y-linked locus) - both placeholder
                row.a = placeholderIdx;
                row.b = placeholderIdx;
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------
    // Geometry
    // ------------------------------------------------------------------

    /** Wide enough for a gene name and its allele buttons, narrow enough to leave a preview. */
    private int listWidth() {
        return Math.max(150, Math.min(240, this.width - RIGHT_W - 130));
    }

    private int listBottom() {
        return this.height - 44;
    }

    private int rightX() {
        return this.width - RIGHT_W - 8;
    }

    private int previewLeft() {
        return LIST_X + listWidth() + 8;
    }

    private int previewRight() {
        return rightX() - 8;
    }

    private int visibleRows() {
        return Math.max(1, (listBottom() - LIST_TOP) / ROW_H);
    }

    private int maxScroll() {
        return Math.max(0, rows.size() - visibleRows());
    }

    /** Width available for a gene name: the whole row, less the widgets an added row carries. */
    private int nameWidth(boolean added) {
        return added ? listWidth() - 2 * ALLELE_W - REMOVE_W - 10 : listWidth() - 8;
    }

    /** The row under {@code (mouseX, mouseY)}, or {@code -1}. */
    private int rowAt(double mouseX, double mouseY) {
        if (mouseX < LIST_X - 4 || mouseX > LIST_X + listWidth()
                || mouseY < LIST_TOP || mouseY >= LIST_TOP + visibleRows() * ROW_H) {
            return -1;
        }
        int index = scroll + (int) ((mouseY - LIST_TOP) / ROW_H);
        return index < rows.size() ? index : -1;
    }

    // ------------------------------------------------------------------
    // Widgets
    // ------------------------------------------------------------------

    @Override
    protected void init() {
        scroll = Math.max(0, Math.min(scroll, maxScroll()));

        int listW = listWidth();
        int aX = LIST_X + listW - 2 * ALLELE_W - REMOVE_W - 6;
        int bX = LIST_X + listW - ALLELE_W - REMOVE_W - 4;
        int xX = LIST_X + listW - REMOVE_W;

        int visible = visibleRows();
        for (int i = scroll; i < rows.size() && i < scroll + visible; i++) {
            final Row row = rows.get(i);
            if (!row.added) {
                continue; // a gene not on the horse is a plain name; the row itself is the button
            }
            final List<Allele> as = row.gene.alleles();
            final int ry = LIST_TOP + (i - scroll) * ROW_H;
            final boolean dropdown = as.size() > 3;
            addRenderableWidget(Button.builder(
                            Component.literal(as.get(row.a).token()),
                            b -> {
                                if (dropdown) {
                                    openDropdown(row, 0, aX, ry);
                                } else {
                                    row.a = (row.a + 1) % as.size();
                                    enforceSexLinkage(row);
                                    rebuildWidgets();
                                }
                            })
                    .bounds(aX, ry, ALLELE_W, ROW_H - 2).build());
            addRenderableWidget(Button.builder(
                            Component.literal(as.get(row.b).token()),
                            b -> {
                                if (dropdown) {
                                    openDropdown(row, 1, bX, ry);
                                } else {
                                    row.b = (row.b + 1) % as.size();
                                    enforceSexLinkage(row);
                                    rebuildWidgets();
                                }
                            })
                    .bounds(bX, ry, ALLELE_W, ROW_H - 2).build());
            addRenderableWidget(Button.builder(
                            Component.literal("x"),
                            b -> {
                                remove(row);
                                rebuildWidgets();
                            })
                    .bounds(xX, ry, REMOVE_W, ROW_H - 2).build());
        }

        int rx = rightX();
        int ry = LIST_TOP + 4;
        addRenderableWidget(Button.builder(
                        Component.literal(baby ? "Age: Foal" : "Age: Adult"),
                        b -> {
                            baby = !baby;
                            rebuildWidgets();
                        })
                .bounds(rx, ry, RIGHT_W, 20).build());
        ry += RIGHT_STEP;
        addRenderableWidget(Button.builder(
                        Component.literal(female ? "Sex: Mare" : "Sex: Stallion"),
                        b -> {
                            female = !female;
                            for (Row row : rows) {
                                if (row.added) {
                                    enforceSexLinkage(row);
                                }
                            }
                            rebuildWidgets();
                        })
                .bounds(rx, ry, RIGHT_W, 20).build());
        ry += RIGHT_STEP;
        String breedName = breedIndex == 0 ? "(none)" : breedChoices.get(breedIndex - 1).name();
        if (breedName.length() > 12) {
            breedName = breedName.substring(0, 11) + "…";
        }
        final int breedBtnX = rx;
        final int breedBtnY = ry;
        addRenderableWidget(Button.builder(
                        Component.literal("Breed: " + breedName + " ▾"),
                        b -> openBreedDropdown(breedBtnX, breedBtnY))
                .bounds(rx, ry, RIGHT_W, 20).build());
        ry += RIGHT_STEP;
        addRenderableWidget(Button.builder(
                        Component.literal("Randomize"),
                        b -> randomizeGenes())
                .bounds(rx, ry, RIGHT_W, 20).build());
        ry += RIGHT_STEP;
        addRenderableWidget(Button.builder(
                        Component.literal("Reroll epi."),
                        b -> {
                            epigenome = rollEpigenome();
                            rebuildWidgets();
                        })
                .bounds(rx, ry, RIGHT_W, 20).build());
        ry += RIGHT_STEP;
        addRenderableWidget(Button.builder(Component.literal("Copy code"), b -> copyCode())
                .bounds(rx, ry, RIGHT_W, 20).build());
        ry += RIGHT_STEP;
        addRenderableWidget(Button.builder(Component.literal("Paste code"), b -> pasteCode())
                .bounds(rx, ry, RIGHT_W, 20).build());
        ry += RIGHT_STEP;
        addRenderableWidget(Button.builder(Component.literal("Clear genes"), b -> reset())
                .bounds(rx, ry, RIGHT_W, 20).build());

        // Creative-only, and the server re-checks it. Saying so on the button
        // rather than only in a chat line after the click: the one report of
        // this tool "not working" in a real build came with no detail, and a
        // dead-looking button that never explains itself is the shape of bug
        // that produces exactly that report.
        boolean creative = Minecraft.getInstance().player != null
                && Minecraft.getInstance().player.getAbilities().instabuild;
        Button spawnButton = Button.builder(Component.literal(creative ? "Spawn" : "Spawn (creative only)"),
                        b -> spawn())
                .bounds(rx, this.height - 48, RIGHT_W, 20)
                .tooltip(creative ? null : net.minecraft.client.gui.components.Tooltip.create(
                        Component.literal("The custom spawn egg builds a horse from scratch, so it is a "
                                + "creative-mode tool. Switch to creative to spawn what you have built; "
                                + "everything else on this screen works either way.")))
                .build();
        spawnButton.active = creative;
        addRenderableWidget(spawnButton);
        Button eggButton = Button.builder(Component.literal("Make egg"), b -> makeEgg())
                .bounds(rx, this.height - 70, RIGHT_W, 20)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.literal(creative
                                ? "Put this exact horse into a preset spawn egg instead of "
                                        + "spawning it: keep it, spawn it later, or give it to "
                                        + "somebody who is not in creative."
                                : "Creative only, the same as Spawn - it writes an arbitrary "
                                        + "horse into an item.")))
                .build();
        eggButton.active = creative;
        addRenderableWidget(eggButton);
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(rx, this.height - 26, RIGHT_W, 20).build());
    }

    /**
     * Clicking a gene that is not on the horse adds it. The allele buttons and
     * the {@code x} on an added row are real widgets and take their own clicks
     * first, so this only ever sees the empty part of a row.
     */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (breedDd) {
            pickBreedFromDropdown(event.x(), event.y()); // any click resolves or dismisses it
            return true;
        }
        if (ddRow != null) {
            pickFromDropdown(event.x(), event.y()); // any click resolves or dismisses it
            return true;
        }
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        int index = rowAt(event.x(), event.y());
        if (index >= 0 && !rows.get(index).added) {
            add(rows.get(index));
            rebuildWidgets();
            return true;
        }
        if (event.button() == 0 && inPreview(event.x(), event.y())) {
            draggingPreview = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingPreview && event.button() == 0) {
            draggingPreview = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingPreview) {
            previewYaw += (float) dx;
            previewPitch = Math.max(-80f, Math.min(80f, previewPitch + (float) dy));
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (breedDd) {
            int max = Math.max(0, breedChoices.size() + 1 - DD_VISIBLE);
            ddScroll = Math.max(0, Math.min(max, ddScroll - (int) Math.signum(scrollY)));
            return true;
        }
        if (ddRow != null) {
            int max = Math.max(0, ddRow.gene.alleles().size() - DD_VISIBLE);
            ddScroll = Math.max(0, Math.min(max, ddScroll - (int) Math.signum(scrollY)));
            return true;
        }
        if (inPreview(mouseX, mouseY)) {
            previewZoom = Math.max(0.3f, Math.min(4.0f, previewZoom * (scrollY > 0 ? 1.1f : 0.9f)));
            return true;
        }
        if (maxScroll() > 0 && mouseX < LIST_X + listWidth()) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY)));
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ------------------------------------------------------------------
    // The genome under construction
    // ------------------------------------------------------------------

    private void add(Row row) {
        AllelePair pair = variantPair(row.gene);
        row.a = indexOf(row.gene, pair.first());
        row.b = indexOf(row.gene, pair.second());
        row.added = true;
        enforceSexLinkage(row); // variantPair doesn't know the screen's sex
    }

    private void remove(Row row) {
        row.added = false;
        row.a = indexOf(row.gene, row.gene.defaultAllele());
        row.b = row.a;
    }

    /**
     * Only the added rows are named; {@link Genotype#of} fills every other gene
     * with its default allele, which is what a horse carries there anyway.
     */
    private Genotype genotype() {
        List<AllelePair> pairs = new ArrayList<>();
        for (Row row : rows) {
            if (!row.added) {
                continue;
            }
            List<Allele> as = row.gene.alleles();
            pairs.add(new AllelePair(as.get(row.a), as.get(row.b)));
        }
        return Genotype.of(pairs).withSex(female ? Sex.FEMALE : Sex.MALE);
    }

    private void reset() {
        for (Row row : rows) {
            remove(row);
        }
        epigenome = rollEpigenome();
        rebuildWidgets();
    }

    /**
     * Copy the genotype code out / paste one back in - the round trip
     * {@code wiki/roadmap.html} section 9 asks for, and what turns "look at
     * this horse" into something reproducible. The epigenome is deliberately
     * not in the string: it has its own re-roll button, and a code you can
     * paste into a chat message wants to stay one line.
     */
    private void copyCode() {
        Minecraft.getInstance().keyboardHandler.setClipboard(genotype().toCode());
    }

    private void pasteCode() {
        String code = Minecraft.getInstance().keyboardHandler.getClipboard();
        Genotype parsed;
        try {
            parsed = Genotype.parse(code.trim());
        } catch (RuntimeException e) {
            return; // a clipboard full of something else is not worth a dialog
        }
        for (Row row : rows) {
            AllelePair pair = parsed.pair(row.gene);
            row.a = indexOf(row.gene, pair.first());
            row.b = indexOf(row.gene, pair.second());
            // a locus the pasted horse leaves at its baseline is not "added" -
            // it would otherwise paste back as 30-odd rows of N/N.
            row.added = !pair.homozygousFor(row.gene.defaultAllele());
        }
        female = parsed.sex() == Sex.FEMALE;
        for (Row row : rows) {
            enforceSexLinkage(row); // a hand-edited or stale clipboard code may not be sex-consistent
        }
        rebuildWidgets();
    }

    private void spawn() {
        send(false);
    }

    /**
     * Write what is on screen into a {@code preset_horse_spawn_egg} instead of
     * spawning it - the "keep this one" the editor used to be missing. A build
     * worth ten minutes of fiddling could only ever be spent immediately, in
     * front of you, and there was nothing to hand to anybody else.
     *
     * <p>Same packet, same server-side validation, one flag different: the two
     * cannot drift into "the egg spawns a different horse from the button".
     */
    private void makeEgg() {
        send(true);
    }

    private void send(boolean asEgg) {
        String breedTok = breedIndex == 0 ? ""
                : BreedLineage.pure(breedChoices.get(breedIndex - 1).id()).toToken();
        ClientPacketDistributor.sendToServer(new SpawnCustomHorsePayload(
                genotype().toCode(), epigenome.toCode(), baby, female, breedTok, asEgg));
        onClose();
    }

    // ------------------------------------------------------------------
    // Preview
    // ------------------------------------------------------------------

    /**
     * How far the preview will grow the model before it stops. Past this a horse
     * is a wall of pixels in a panel this size and you learn nothing more from
     * it; the printed multiplier carries the rest.
     */
    private static final double PREVIEW_SCALE_CAP = 2.5;

    /**
     * The body the current genome resolves to. Cheap enough for a frame - it is
     * one walk of the gene list - and it has to be re-resolved rather than
     * cached against the coat key, because the epigenome moves the size without
     * moving a single pixel of the coat.
     */
    private Traits previewTraits() {
        return HorseTraits.resolve(genotype(), epigenome, true);
    }

    /** The coat the current genome makes, rebuilt only when the genome moves. */
    private CoatData previewCoat() {
        Genotype genotype = genotype();
        String key = genotype.toCode() + '@' + epigenome.toCode();
        if (previewCoat == null || !key.equals(previewKey)) {
            previewCoat = new CoatData(genotype, epigenome);
            previewKey = key;
        }
        return previewCoat;
    }

    private Horse previewHorse() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        if (previewHorse == null) {
            try {
                previewHorse = EntityType.HORSE.create(mc.level, EntitySpawnReason.LOAD);
            } catch (RuntimeException e) {
                return null;
            }
            previewHorseIsBaby = false;
        }
        if (previewHorse != null && previewHorseIsBaby != baby) {
            previewHorse.setBaby(baby);
            previewHorseIsBaby = baby;
        }
        return previewHorse;
    }

    private void drawPreview(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int x0 = previewLeft();
        int x1 = previewRight();
        int y0 = LIST_TOP;
        int y1 = this.height - 30;
        int w = x1 - x0;
        int h = y1 - y0;
        if (w < 40 || h < 40) {
            return;
        }
        g.fill(x0, y0, x1, y1, PANEL);

        Horse horse = previewHorse();
        CoatData coat = previewCoat();
        if (horse == null) {
            return;
        }
        drawSizeReadout(g, x0, x1, y1);
        // Same framing ratio the family tree uses (a 50x78 viewport at scale 16).
        float mScale = Math.min(w / 3.1F, h / 4.9F);

        // ...times whatever the genome says the horse's body scale is, so the
        // magical size locus is visible here and "Reroll epi." visibly resizes
        // the horse. Framing is deliberately NOT refitted to the result: fitting
        // a big horse back into the panel would cancel exactly the thing being
        // previewed. Capped only so an extreme draw is still a recognisable
        // horse rather than a wall of pixels - the readout below never caps.
        double bodyScale = previewTraits().scale();
        mScale *= (float) Math.min(bodyScale, PREVIEW_SCALE_CAP);
        mScale *= previewZoom;

        try {
            EntityRenderer<? super Horse, ?> renderer =
                    Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(horse);
            EntityRenderState state = renderer.createRenderState(horse, 1.0F);
            state.shadowPieces.clear();
            state.outlineColor = 0;
            if (state instanceof GeneticHorseRenderState gs) {
                gs.coatData = coat;
            }
            float pitch = Math.max(-80.0F, Math.min(80.0F, previewPitch));
            float t = (System.currentTimeMillis() - screenOpenedAt) / 1000.0F;
            if (state instanceof LivingEntityRenderState ls) {
                ls.bodyRot = 180.0F + previewYaw;
                ls.yRot = 180.0F + previewYaw; // head in line with the body - no mouse tracking
                ls.xRot = 0.0F;
                ls.boundingBoxWidth = ls.boundingBoxWidth / ls.scale;
                ls.boundingBoxHeight = ls.boundingBoxHeight / ls.scale;
                ls.scale = 1.0F;
                // Always mid-stride: the legs swing, and on a real (server-ticked)
                // horse this is also when the particle locus emits. setupAnim only
                // swings the legs once walkAnimationSpeed clears 0.2.
                ls.walkAnimationSpeed = 1.0F;
                ls.walkAnimationPos = t * 6.0F;
                ls.ageInTicks = t * 20.0F;
            }
            if (state instanceof EquineRenderState es) {
                es.animateTail = true;
            }
            Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
            Quaternionf xRotation = new Quaternionf().rotateX(pitch * ((float) Math.PI / 180.0F));
            rotation.mul(xRotation);
            Vector3f translation = new Vector3f(0.0F, state.boundingBoxHeight / 2.0F + 0.0625F, 0.0F);
            g.entity(state, mScale, translation, rotation, xRotation, x0 + 1, y0 + 1, x1 - 1, y1 - 1);
        } catch (RuntimeException ignored) {
            // a coat that will not bake should not take the editor down with it
        }
        drawPreviewParticles(g, x0, y0, x1, y1);
    }

    /**
     * Advance and draw the preview's particle cloud - one pass per frame,
     * wall-clock timed. Spawns from every {@code particle}-kind {@code emitter}
     * the current genome expresses (epigenetic colour resolved through the
     * screen's live epigenome), in a band matched loosely to the emitter's
     * body anchor. Motes are clipped to the preview panel.
     */
    private void drawPreviewParticles(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastFrameNanos) / 1_000_000_000f);
        lastFrameNanos = now;

        motes.removeIf(m -> {
            m.age += dt;
            m.x += m.vx * dt;
            m.y += m.vy * dt;
            m.vy += 14f * dt;            // gentle gravity (px/s^2)
            m.vx *= (1f - Math.min(1f, 0.9f * dt)); // drag
            return m.age >= m.life;
        });

        java.util.List<HorseAbilities.Active> abilities =
                HorseAbilities.activeFor(genotype(), epigenome);
        boolean anyEmitter = false;
        for (HorseAbilities.Active a : abilities) {
            if (a.ability() instanceof GeneAbility.Emitter e && "particle".equals(e.kind())) {
                anyEmitter = true;
                break;
            }
        }
        if (anyEmitter) {
            emitAccumulator += dt;
            float period = 0.10f;       // seconds between spawn ticks in the preview
            int guard = 0;
            while (emitAccumulator >= period && guard++ < 8) {
                emitAccumulator -= period;
                for (HorseAbilities.Active a : abilities) {
                    if (!(a.ability() instanceof GeneAbility.Emitter e) || !"particle".equals(e.kind())) {
                        continue;
                    }
                    if (previewRng.nextFloat() > Math.max(0.05, e.chance())) {
                        continue;
                    }
                    spawnMotes(e, x0, y0, x1, y1);
                }
            }
        }

        for (Mote m : motes) {
            float k = 1f - (m.age / m.life);
            int alpha = Math.max(0, Math.min(255, Math.round(k * 210)));
            int col = (alpha << 24) | (m.rgb & 0xFFFFFF);
            int px = Math.round(m.x);
            int py = Math.round(m.y);
            if (px < x0 || px > x1 || py < y0 || py > y1) {
                continue;
            }
            int sz = Math.max(1, Math.round(m.size * (0.5f + 0.5f * k)));
            g.fill(px, py, px + sz, py + sz, col);
        }
    }

    /**
     * The colour one puff comes out, which is <b>not</b> simply
     * {@code e.color()} for a cycling emitter: rainbow dust leaves its two
     * colour fields white and expects the translator to read the hue off the
     * clock, so a preview that trusted the record would draw a white trail for
     * the one gene whose entire point is that it is not white. The wall clock
     * stands in for the horse's {@code tickCount} here - there is no horse yet -
     * at the game's twenty ticks a second, so a cycle set on this screen turns
     * at the speed it will turn in the world.
     */
    private static int previewColor(GeneAbility.Emitter e) {
        if (e.cycleTicks() > 0) {
            double seconds = (System.nanoTime() % 1_000_000_000_000L) / 1.0e9;
            double turn = seconds * 20.0 / e.cycleTicks();
            return HairPattern.hsvToRgb(turn - Math.floor(turn), 1.0, 1.0) & 0xFFFFFF;
        }
        return (e.color() & 0xFFFFFF) == 0 ? 0xFFFFFF : (e.color() & 0xFFFFFF);
    }

    private void spawnMotes(GeneAbility.Emitter e, int x0, int y0, int x1, int y1) {
        if (motes.size() >= MOTE_CAP) {
            return;
        }
        int cx = (x0 + x1) / 2;
        int hgt = y1 - y0;
        float yFrac = switch (e.anchor()) {
            case "head", "eyes" -> 0.42f;
            case "body" -> 0.58f;
            case "spine" -> 0.50f;
            case "tail" -> 0.55f;
            default -> 0.80f;           // feet / hooves
        };
        float baseY = y0 + hgt * yFrac;
        float spreadX = 26f * previewZoom;
        float spreadY = 10f * previewZoom;
        int rgb = previewColor(e);
        int n = Math.max(1, Math.min(6, e.count()));
        for (int i = 0; i < n && motes.size() < MOTE_CAP; i++) {
            Mote m = new Mote();
            m.x = cx + (previewRng.nextFloat() - 0.5f) * 2f * spreadX;
            m.y = baseY + (previewRng.nextFloat() - 0.5f) * 2f * spreadY;
            m.vx = (previewRng.nextFloat() - 0.5f) * 20f;
            m.vy = -6f - previewRng.nextFloat() * 14f;   // drift up, then gravity pulls it down
            m.age = 0f;
            m.life = 0.7f + previewRng.nextFloat() * 0.8f;
            m.size = 1.6f + previewRng.nextFloat() * 1.4f;
            m.rgb = rgb;
            motes.add(m);
        }
    }

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        int listW = listWidth();
        Genotype genotype = genotype();
        int visible = visibleRows();
        int shown = Math.min(rows.size(), scroll + visible) - scroll;
        int hovered = rowAt(mouseX, mouseY);

        // header band - nothing lives above the list or the right column
        g.fill(0, 6, this.width, 32, PANEL);
        g.text(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2, 14, 0xFFFFFFFF);

        // the name column only - stop short of an added row's allele buttons
        // 0xE0 rather than 0x88: the gene names sit over the world (and over
        // grass, on the browser twin) and a half-transparent backing made them
        // hard to read. Keep this and wiki/horse-designer/js/gui.js NAME_BG the
        // same value.
        g.fill(LIST_X - 4, LIST_TOP - 14, LIST_X + nameWidth(true) + 2,
                LIST_TOP + shown * ROW_H, 0xE0000000);
        Component header = Component.literal(maxScroll() > 0
                ? "Genes - click to add  (scroll)"
                : "Genes - click to add");
        g.text(this.font, header, LIST_X, LIST_TOP - 12, 0xFF9098A8);

        for (int i = scroll; i < rows.size() && i < scroll + visible; i++) {
            Row row = rows.get(i);
            int ry = LIST_TOP + (i - scroll) * ROW_H;
            if (!row.added) {
                // off the horse: a plain name, the whole row clickable
                if (i == hovered) {
                    g.fill(LIST_X - 4, ry, LIST_X + listW, ry + ROW_H - 2, 0x33FFFFFF);
                }
                drawFitted(g, row.gene.name(), LIST_X, ry + 6, nameWidth(false),
                        i == hovered ? 0xFFFFFFFF : 0xFF9AA0B0);
                continue;
            }
            // on the horse: name, what it expresses, and its two allele buttons
            Expression e = genotype.expressionOf(row.gene);
            boolean expressing = !e.wildType();
            g.fill(LIST_X - 4, ry, LIST_X + nameWidth(true) + 2, ry + ROW_H - 2, 0x33202838);
            drawFitted(g, row.gene.name(), LIST_X, ry + 1, nameWidth(true),
                    expressing ? 0xFF9BE08A : 0xFFC8C8C8);
            drawFitted(g, expressing ? e.name() : "no effect", LIST_X, ry + 10, nameWidth(true),
                    0xFF787888);
        }

        drawPreview(g, mouseX, mouseY);

        // the genome as text, under the list
        g.fill(LIST_X - 4, this.height - 42, LIST_X + listW, this.height - 14, 0xE0000000);
        drawFitted(g, GeneCodeDisplay.shortForm(genotype), LIST_X, this.height - 39, listW - 4, 0xFF88CC88);
        drawFitted(g, "epigenetics #" + Long.toHexString(epigenome.visibleFingerprint(genotype)),
                LIST_X, this.height - 26, listW - 4, 0xFF8890A8);

        if (ddRow != null) {
            drawDropdown(g, mouseX, mouseY);
        } else if (breedDd) {
            drawBreedDropdown(g, mouseX, mouseY);
        }
    }

    /** The open breed picker, drawn last so it sits over the widgets it covers. */
    private void drawBreedDropdown(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int count = breedChoices.size() + 1;
        int h = DD_VISIBLE * DD_ROW_H;
        g.fill(ddX - 1, ddY - 1, ddX + BREED_DD_W + 1, ddY + h + 1, 0xF00E0E16);
        g.fill(ddX - 1, ddY - 1, ddX + BREED_DD_W + 1, ddY, 0xFF5A6478);
        for (int i = 0; i < DD_VISIBLE; i++) {
            int idx = ddScroll + i;
            if (idx >= count) {
                break;
            }
            int ry = ddY + i * DD_ROW_H;
            boolean hov = mouseX >= ddX && mouseX < ddX + BREED_DD_W && mouseY >= ry && mouseY < ry + DD_ROW_H;
            if (idx == breedIndex) {
                g.fill(ddX, ry, ddX + BREED_DD_W, ry + DD_ROW_H, 0x557088FF);
            } else if (hov) {
                g.fill(ddX, ry, ddX + BREED_DD_W, ry + DD_ROW_H, 0x33FFFFFF);
            }
            String label = idx == 0 ? "(none)" : breedChoices.get(idx - 1).name();
            drawFitted(g, label, ddX + 3, ry + 2, BREED_DD_W - 10,
                    idx == breedIndex ? 0xFFFFFFFF : 0xFFC0C4D0);
        }
        int max = Math.max(0, count - DD_VISIBLE);
        if (max > 0) {
            int barH = Math.max(6, h * DD_VISIBLE / count);
            int barY = ddY + Math.round((h - barH) * (ddScroll / (float) max));
            g.fill(ddX + BREED_DD_W - 2, ddY, ddX + BREED_DD_W, ddY + h, 0x40FFFFFF);
            g.fill(ddX + BREED_DD_W - 2, barY, ddX + BREED_DD_W, barY + barH, 0xFF8890A8);
        }
    }

    /** The open many-allele picker, drawn last so it sits over the widgets it covers. */
    private void drawDropdown(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        List<Allele> as = ddRow.gene.alleles();
        int h = DD_VISIBLE * DD_ROW_H;
        g.fill(ddX - 1, ddY - 1, ddX + DD_W + 1, ddY + h + 1, 0xF00E0E16);
        g.fill(ddX - 1, ddY - 1, ddX + DD_W + 1, ddY, 0xFF5A6478);
        int cur = ddSlot == 0 ? ddRow.a : ddRow.b;
        for (int i = 0; i < DD_VISIBLE; i++) {
            int idx = ddScroll + i;
            if (idx >= as.size()) {
                break;
            }
            int ry = ddY + i * DD_ROW_H;
            boolean hov = mouseX >= ddX && mouseX < ddX + DD_W && mouseY >= ry && mouseY < ry + DD_ROW_H;
            if (idx == cur) {
                g.fill(ddX, ry, ddX + DD_W, ry + DD_ROW_H, 0x557088FF);
            } else if (hov) {
                g.fill(ddX, ry, ddX + DD_W, ry + DD_ROW_H, 0x33FFFFFF);
            }
            drawFitted(g, as.get(idx).token(), ddX + 3, ry + 2, DD_W - 10,
                    idx == cur ? 0xFFFFFFFF : 0xFFC0C4D0);
        }
        int max = Math.max(0, as.size() - DD_VISIBLE);
        if (max > 0) {
            int barH = Math.max(6, h * DD_VISIBLE / as.size());
            int barY = ddY + Math.round((h - barH) * (ddScroll / (float) max));
            g.fill(ddX + DD_W - 2, ddY, ddX + DD_W, ddY + h, 0x40FFFFFF);
            g.fill(ddX + DD_W - 2, barY, ddX + DD_W, barY + barH, 0xFF8890A8);
        }
    }

    /**
     * The exact body scale, along the bottom of the preview panel. It is the one
     * thing the picture cannot be trusted for: the model is capped at
     * {@link #PREVIEW_SCALE_CAP}, and past that only this number is telling the
     * truth - so it says so when it is carrying the weight.
     *
     * <p>Only drawn when the horse is not ordinary size, so a screen with the
     * size locus untouched looks exactly as it did.
     */
    private void drawSizeReadout(GuiGraphicsExtractor g, int x0, int x1, int y1) {
        double scale = previewTraits().scale();
        if (Math.abs(scale - 1.0) < 0.005) {
            return;
        }
        String text = String.format(java.util.Locale.ROOT, "size %.2fx", scale);
        if (scale > PREVIEW_SCALE_CAP) {
            text += " (preview capped)";
        }
        int colour = scale > 1.0 ? 0xFFE0C070 : 0xFF80B8D0;
        Component line = Component.literal(text);
        g.text(this.font, line, (x0 + x1) / 2 - this.font.width(line) / 2, y1 - 11, colour);
    }

    /** Left-aligned at {@code (x, y)}, scaled down (never up) so the whole string fits {@code maxW}. */
    private void drawFitted(GuiGraphicsExtractor g, String text, int x, int y, int maxW, int color) {
        float w = this.font.width(text);
        if (w <= maxW || w <= 0) {
            g.text(this.font, Component.literal(text), x, y, color);
            return;
        }
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(maxW / w);
        g.text(this.font, Component.literal(text), 0, 0, color);
        pose.popMatrix();
    }
}
