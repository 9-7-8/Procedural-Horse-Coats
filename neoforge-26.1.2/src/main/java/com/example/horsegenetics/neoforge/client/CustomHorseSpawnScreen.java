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
import com.example.horsegenetics.common.genetics.EditorRules;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneCodeDisplay;
import com.example.horsegenetics.common.genetics.GeneFamily;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.Inheritance;
import com.example.horsegenetics.common.genetics.RandomizeMode;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.spec.HorseAbilities;
import com.example.horsegenetics.common.horse.HorseFile;
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
 * the registry's processing order), under a {@link GeneFamily} filter and each
 * with a padlock.
 * <b>Centre</b> - a live 3D horse in the coat the genome makes.
 * <b>Right</b> - age, sex, breed, the two split buttons, and spawn.
 *
 * <h2>The two split buttons</h2>
 * <b>Randomize</b> and <b>Add random</b> each have an arrow beside them. The
 * face does the thing; the arrow picks <i>which</i> thing, out of
 * {@link RandomizeMode} and {@link EditorRules.AddScope}, and the choice sticks
 * - the label always says what the next press will do. Picking from the menu
 * also runs it, because having said what Randomize means you wanted it done.
 *
 * <p>Two rules are worth stating here rather than leaving to the enum.
 * <b>The sex is rolled first</b> on the whole-genome modes, before a single
 * allele: a sex-linked locus cannot be filled in before the sex is known.
 * And <b>a scoped roll leaves everything else alone</b> - <i>Rnd dilution</i>
 * re-rolls seven loci and does not touch the rest of the horse, epigenetics
 * included.
 *
 * <h2>Locks, and the loci that change nothing you can see</h2>
 * A <b>locked</b> row is held exactly as it stands against every randomize -
 * its alleles, whether it is on the horse, and its epigenetics. That is the
 * difference between "roll me another horse" and "roll me another horse with
 * <i>this</i> tobiano".
 *
 * <p><b>Rnd health</b> is off by default, and turns on the genes that change
 * nothing visible: the disorders, the stat loci, the ability genes
 * ({@link Genes#influencesCoat} draws the line). Off, because the reason to
 * press Randomize on a gene editor is almost always to look at a coat, and
 * rolling a lethal into the horse you are looking at is a surprise nobody
 * asked for.
 *
 * <p><b>Extension, agouti and shade are always on the horse</b> and carry no
 * {@code x}: every horse has alleles at all three, and a list that hides them
 * until you click implies otherwise.
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
 * this class - {@code applyGenome}, {@code stamp}, {@code randomizable},
 * {@code topUpMagical}, {@code addRandom} and {@code enforceSexLinkage} are all
 * mirrored there under the same names, and the rules that are <i>identical</i>
 * rather than merely parallel have moved to {@code common/EditorRules} and
 * {@code common/RandomizeMode} so there is only one of each. Only the drawing is JavaScript
 * ({@code wiki/horse-designer/js/gui.js}), and it copies the layout constants
 * and colours below by value, so a moved widget is a two-line change.
 *
 * <p>The deliberate divergences: the browser has nothing to spawn, so
 * <b>Spawn</b> and <b>Cancel</b> are replaced by <b>Wander</b> and <b>Reset
 * view</b>; it cannot draw the cutie-mark item icons or the particle emitters,
 * which come from the game's own registries; and where this screen has
 * <b>Copy horse</b> / <b>Paste</b> the browser has <b>Export</b> /
 * <b>Import</b>. That last pair is the same slot on the same column carrying
 * the identical payload - {@code HorseFile}, the whole animal - to a file
 * rather than to a clipboard, because a Minecraft screen has no file picker and
 * a browser tab has no chat to paste into.
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
    /**
     * <b>Wide enough for the widest label the column actually carries.</b> It
     * was 96, and several labels were simply wider than that: "Spawn (creative
     * only)" measures 111px, and "Breed: " plus a truncated name and its arrow
     * ran past 120. A vanilla button does not shrink or wrap its label, so they
     * were drawn straight over the edges - reported, accurately, as the buttons
     * being "WAY too cramped".
     */
    private static final int RIGHT_W = 128;

    /** 20-high buttons with a 4px gutter. Was 22, i.e. a 2px gutter. */
    private static final int RIGHT_STEP = 24;

    /**
     * The tightest gutter {@link #rightStep()} will squeeze to - buttons
     * touching, no gap at all. Below this it stops giving and lets the two
     * groups meet, because there is nothing left to give.
     */
    private static final int RIGHT_STEP_MIN = 20;

    /** Rows in the top group: Age, Sex, Breed, Randomize, Add, Rnd health, Clear. */
    private static final int RIGHT_ROWS = 7;
    private static final int PANEL = 0x90000000;
    /** The padlock column down the left of the gene list. */
    private static final int LOCK_W = 10;
    /** The family filter sitting above the list. */
    private static final int FILTER_H = 14;
    /** The arrow half of a split button. */
    private static final int ARROW_W = 14;

    /** Many-allele genes (particle, KIT, ...) get a scrollable list instead of a cycle button. */
    /** The hover blurb's panel: width, its text's line height, and its margin. */
    private static final int BLURB_W = 176;
    private static final int BLURB_LINE_H = 10;
    private static final int BLURB_PAD = 5;

    private static final int DD_ROW_H = 12;
    private static final int DD_VISIBLE = 8;
    private static final int DD_W = 76;
    /** The breed picker's dropdown is wider - breed names are long. */
    private static final int BREED_DD_W = 118;
    /** So are the randomize / add-random / gene-family menus. */
    private static final int MENU_DD_W = 118;

    private boolean baby = false;
    private boolean female = true;
    private int scroll = 0;

    /** Preview view: click-drag orbits, wheel zooms; the horse always walks in place. */
    private float previewYaw = -30f;
    private float previewPitch = 8f;
    private float previewZoom = 1f;
    private boolean draggingPreview = false;
    private final long screenOpenedAt = System.currentTimeMillis();

    /**
     * Which dropdown is open, if any. There are five of them now - the allele
     * picker, the breed picker, the gene-family filter and the two split-button
     * menus - and they are all the same widget with different contents, so they
     * share one open-state rather than one boolean each.
     */
    private enum Dd { NONE, ALLELE, BREED, FAMILY, RANDOMIZE, ADD }

    private Dd dd = Dd.NONE;
    /** The row whose allele slot is being picked - {@link Dd#ALLELE} only. */
    private Row ddRow;
    private int ddSlot;
    private int ddScroll;
    private int ddX;
    private int ddY;

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
    /** {@link #rows} after {@link #filter} - what the list actually shows. */
    private final List<Row> view = new ArrayList<>();
    /** The gene family the list is narrowed to, or {@code null} for all of them. */
    private GeneFamily filter = null;
    private final List<GeneFamily> familyChoices = new ArrayList<>();

    /** What the Randomize split button will do next time it is pressed. */
    private RandomizeMode randomizeMode = RandomizeMode.RANDOM;
    /** What the Add random split button will do next time it is pressed. */
    private EditorRules.AddScope addScope = EditorRules.AddScope.ANY;
    /**
     * Whether a randomize may touch the loci that change nothing you can see -
     * the disorders, the stat genes, the ability genes. Off, because the
     * overwhelmingly common reason to press Randomize on a gene editor is to
     * look at a coat, and rolling a lethal into the horse you are looking at is
     * a surprise nobody asked for. {@link Genes#influencesCoat} draws the line.
     */
    private boolean randomizeInvisible = false;

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
        /** Held against every randomize - see {@link CustomHorseSpawnScreen#toggleLock}. */
        boolean locked;
        int a;
        int b;

        Row(Gene gene) {
            this.gene = gene;
            int def = indexOf(gene, gene.defaultAllele());
            this.a = def;
            this.b = def;
            // Extension, agouti and shade are on the horse from the start:
            // every horse has alleles at all three. EditorRules says so once,
            // for this screen and the browser one both.
            this.added = EditorRules.alwaysCarried(gene);
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
        familyChoices.addAll(GeneFamily.occupied());
        rebuildView();
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
     * Open one of the five dropdowns, anchored at a widget. They differ only in
     * what {@link #ddLabels} answers, which is what keeps the drawing, the
     * scrolling and the click handling single copies of themselves.
     */
    private void openDropdown(Dd kind, int anchorX, int anchorY) {
        this.dd = kind;
        this.ddX = anchorX;
        int h = DD_VISIBLE * DD_ROW_H;
        this.ddY = Math.max(4, Math.min(anchorY, this.height - h - 4));
        int max = Math.max(0, ddLabels().size() - DD_VISIBLE);
        this.ddScroll = Math.max(0, Math.min(ddCurrent() - DD_VISIBLE / 2, max));
    }

    private void openAlleleDropdown(Row row, int slot, int anchorX, int anchorY) {
        this.ddRow = row;
        this.ddSlot = slot;
        openDropdown(Dd.ALLELE, anchorX, anchorY);
    }

    /** What the open dropdown lists. */
    private List<String> ddLabels() {
        List<String> out = new ArrayList<>();
        switch (dd) {
            case ALLELE -> {
                for (Allele a : ddRow.gene.alleles()) {
                    out.add(a.token());
                }
            }
            case BREED -> {
                out.add("(none)");
                for (Breed b : breedChoices) {
                    out.add(b.name());
                }
            }
            case FAMILY -> {
                out.add("All genes");
                for (GeneFamily f : familyChoices) {
                    out.add(f.label());
                }
            }
            case RANDOMIZE -> {
                for (RandomizeMode m : RandomizeMode.values()) {
                    out.add(m.label());
                }
            }
            case ADD -> {
                for (EditorRules.AddScope a : EditorRules.AddScope.values()) {
                    out.add(a.label());
                }
            }
            default -> { }
        }
        return out;
    }

    /** Which entry of the open dropdown is the current one. */
    private int ddCurrent() {
        return switch (dd) {
            case ALLELE -> ddSlot == 0 ? ddRow.a : ddRow.b;
            case BREED -> breedIndex;
            case FAMILY -> filter == null ? 0 : familyChoices.indexOf(filter) + 1;
            case RANDOMIZE -> randomizeMode.ordinal();
            case ADD -> addScope.ordinal();
            default -> 0;
        };
    }

    private int ddWidth() {
        return switch (dd) {
            case ALLELE -> DD_W;
            case BREED -> BREED_DD_W;
            default -> MENU_DD_W;
        };
    }

    private void closeDropdown() {
        dd = Dd.NONE;
        ddRow = null;
    }

    /**
     * Any click while a dropdown is open resolves or dismisses it. Picking a
     * <b>mode</b> also runs it: the menu is how you say what Randomize means,
     * and having said it you wanted it done - the button face keeps the choice
     * for next time.
     */
    private void pickFromDropdown(double mx, double my) {
        Dd kind = dd;
        int h = DD_VISIBLE * DD_ROW_H;
        int w = ddWidth();
        int idx = -1;
        if (mx >= ddX && mx < ddX + w && my >= ddY && my < ddY + h) {
            int hit = ddScroll + (int) ((my - ddY) / DD_ROW_H);
            if (hit >= 0 && hit < ddLabels().size()) {
                idx = hit;
            }
        }
        Row row = ddRow;
        int slot = ddSlot;
        closeDropdown();
        if (idx >= 0) {
            switch (kind) {
                case ALLELE -> {
                    if (slot == 0) {
                        row.a = idx;
                    } else {
                        row.b = idx;
                    }
                    enforceSexLinkage(row);
                }
                case BREED -> {
                    breedIndex = idx;
                    if (idx != 0) {
                        applyBreedPreset(breedChoices.get(idx - 1));
                    }
                }
                case FAMILY -> {
                    filter = idx == 0 ? null : familyChoices.get(idx - 1);
                    scroll = 0;
                    rebuildView();
                }
                case RANDOMIZE -> {
                    randomizeMode = RandomizeMode.values()[idx];
                    randomize();
                }
                case ADD -> {
                    addScope = EditorRules.AddScope.values()[idx];
                    addRandom();
                }
                default -> { }
            }
        }
        rebuildWidgets();
    }

    /**
     * Roll a fresh wild founder of a breed straight into the editor - genotype,
     * epigenome and sex - and stamp that breed on whatever is spawned. Picking a
     * breed by hand is "here is a different horse", so it replaces the whole one,
     * locks and all; the {@code Rnd breed} mode is the one that respects them.
     */
    private void applyBreedPreset(Breed breed) {
        applyGenome(BreedFounder.roll(breed, new NeoRng(RandomSource.create())), true, null);
    }

    /**
     * Stamp a whole rolled genome onto the editor - the epigenome, optionally
     * the sex, and every locus {@code mode} is <b>allowed</b> to touch. A locus
     * the roll left at its baseline drops off the list, so a mostly-plain roll
     * does not come back as forty rows of {@code N/N}.
     *
     * <p>{@code mode} is {@code null} for the stamps that are not a randomize -
     * a breed picked by hand, a horse pasted in - which replace the horse
     * outright.
     */
    private void applyGenome(Genome g, boolean adoptSex, RandomizeMode mode) {
        Genotype gt = g.genotype();
        if (adoptSex) {
            female = gt.sex() == Sex.FEMALE;
        }
        Epigenome next = g.epigenome();
        for (Row row : rows) {
            if (mode != null && !(mode.covers(row.gene) && randomizable(row))) {
                // untouched - and its epigenetics stay untouched with it
                if (Epigenome.carries(row.gene)) {
                    next = next.with(row.gene.key(), epigenome.copies(row.gene));
                }
                continue;
            }
            stamp(row, gt);
        }
        epigenome = next;
        previewKey = "";
    }

    /**
     * One row from a genotype. A locus at its baseline is not marked added -
     * that is what stops a paste coming back as forty rows of {@code N/N} -
     * unless it is one of the three every horse visibly carries.
     */
    private void stamp(Row row, Genotype gt) {
        AllelePair pair = gt.pair(row.gene);
        int def = indexOf(row.gene, row.gene.defaultAllele());
        row.a = indexOf(row.gene, pair.first());
        row.b = indexOf(row.gene, pair.second());
        row.added = !(row.a == def && row.b == def) || EditorRules.alwaysCarried(row.gene);
    }

    /** Is this row's gene something a randomize is allowed to move? */
    private boolean randomizable(Row row) {
        return !row.locked && (randomizeInvisible || Genes.influencesCoat(row.gene));
    }

    /**
     * <b>Randomize, in whichever of {@link RandomizeMode}'s senses is selected.</b>
     *
     * <p>The order is load-bearing, and is the same on the browser twin:
     * <ol>
     *   <li><b>the sex first</b>, for the whole-genome modes - a sex-linked
     *       locus cannot be filled in before the sex is known, and rolling it
     *       afterwards means snapping half of what was rolled straight back;</li>
     *   <li>the breed, if the mode picks one, because a breed constrains every
     *       locus after it;</li>
     *   <li>the alleles;</li>
     *   <li>the magical floor, for the {@code +N} modes.</li>
     * </ol>
     *
     * <p>A locked row, a row outside the mode's scope, and - unless
     * {@link #randomizeInvisible} - a row that changes nothing you can see are
     * all left exactly as they were, epigenetics included.
     */
    private void randomize() {
        NeoRng rng = new NeoRng(RandomSource.create());
        RandomizeMode mode = randomizeMode;
        if (mode == RandomizeMode.EPIGENETICS) {
            rerollEpigenome(rng);
            closeDropdown();
            rebuildWidgets();
            return;
        }
        if (mode.rollsSex()) {
            setSex(rng.nextBoolean());
        }
        if (mode == RandomizeMode.BREED && !breedChoices.isEmpty()) {
            breedIndex = 1 + rng.nextInt(breedChoices.size());
        }
        Sex sex = female ? Sex.FEMALE : Sex.MALE;
        Genome g;
        if (mode == RandomizeMode.TRUE_RANDOM) {
            g = new Genome(EditorRules.trueRandom(rng, sex), Epigenome.random(rng));
        } else if (breedIndex == 0) {
            g = Genome.random(rng);
        } else {
            g = BreedFounder.roll(breedChoices.get(breedIndex - 1), rng, sex);
        }
        applyGenome(g, false, mode);
        if (mode.magicalFloor() > 0) {
            topUpMagical(mode.magicalFloor(), rng);
        }
        closeDropdown();
        rebuildWidgets();
    }

    /**
     * Add magical genes, at the pair that shows, until the horse shows at least
     * {@code want} of them. Genes already showing count, so <i>+1 magical</i> on
     * a horse that rolled a galaxy coat adds nothing.
     */
    private void topUpMagical(int want, NeoRng rng) {
        List<Row> candidates = new ArrayList<>();
        int showing = 0;
        Genotype gt = genotype();
        for (Row row : rows) {
            if (row.gene.isNatural()) {
                continue;
            }
            if (EditorRules.showingMagical(gt, row.gene)) {
                showing++;
            } else if (randomizable(row)) {
                candidates.add(row);
            }
        }
        while (showing < want && !candidates.isEmpty()) {
            add(candidates.remove(rng.nextInt(candidates.size())));
            showing++;
        }
    }

    /**
     * <b>Add random.</b> One gene the horse is not already carrying, put on at
     * the pair that shows - the fastest way to meet a gene you did not know
     * existed. A no-op when everything in scope is already on, or locked.
     */
    private void addRandom() {
        NeoRng rng = new NeoRng(RandomSource.create());
        List<Row> candidates = new ArrayList<>();
        for (Row row : rows) {
            if (!row.added && addScope.covers(row.gene) && randomizable(row)) {
                candidates.add(row);
            }
        }
        if (!candidates.isEmpty()) {
            add(candidates.get(rng.nextInt(candidates.size())));
        }
        closeDropdown();
        rebuildWidgets();
    }

    /**
     * A fresh epigenome, with every locked locus keeping the numbers it had. The
     * gene is what a lock is on, and its epigenetics are part of that gene on
     * this horse - a lock that let the dapple pattern re-roll underneath the
     * allele would not be a lock.
     */
    private void rerollEpigenome(NeoRng rng) {
        Epigenome fresh = Epigenome.random(rng);
        for (Row row : rows) {
            if (!randomizable(row) && Epigenome.carries(row.gene)) {
                fresh = fresh.with(row.gene.key(), epigenome.copies(row.gene));
            }
        }
        epigenome = fresh;
        previewKey = "";
    }

    private void toggleLock(Row row) {
        row.locked = !row.locked;
    }

    /** {@link #rows} narrowed by {@link #filter}. Rebuilt whenever either moves. */
    private void rebuildView() {
        view.clear();
        for (Row row : rows) {
            if (filter == null || GeneFamily.of(row.gene) == filter) {
                view.add(row);
            }
        }
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private boolean inPreview(double mx, double my) {
        return mx >= previewLeft() && mx <= previewRight()
                && my >= LIST_TOP && my <= this.height - 30;
    }

    // What a freshly added gene lands on - the first combination that actually
    // shows something - is EditorRules.variantPair in common/. It was written
    // out here and in the browser twin's HorseEditor in the same shape under
    // the same name; one copy is better than two kept in step by hand.

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

    /**
     * Top of the group pinned to the bottom of the column - Make egg, Copy /
     * Paste, Spawn, Cancel, each {@link #RIGHT_STEP} apart, with Cancel landing
     * 26 from the bottom edge.
     */
    private int bottomStackTop() {
        return this.height - 26 - 3 * RIGHT_STEP;
    }

    /**
     * The top group grows down and the bottom group is pinned up, so on a short
     * window they meet. The gutter is what gives: the roomy {@link #RIGHT_STEP}
     * whenever it fits, otherwise the largest step that does, down to
     * {@link #RIGHT_STEP_MIN}. Labels never truncate at any size - only the
     * spacing between rows changes.
     *
     * <p><b>It cannot save every size.</b> Seven 20-high buttons need 140px
     * even touching, and a 1080p screen at GUI scale 4 leaves about 120 - so
     * the two groups still overlap below roughly 290px of window, exactly as
     * they did before this existed. Fixing that means the column scrolls or
     * splits, which is a bigger change than the one asked for; it is written
     * down in {@code wiki/known-gaps.html} rather than half-done here.
     */
    private int rightStep() {
        int available = bottomStackTop() - 8 - (LIST_TOP + 4);
        int fits = (available - 20) / (RIGHT_ROWS - 1);
        return Math.max(RIGHT_STEP_MIN, Math.min(RIGHT_STEP, fits));
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
        return Math.max(0, view.size() - visibleRows());
    }

    /** Where a gene's name starts: past the padlock column. */
    private int nameX() {
        return LIST_X + LOCK_W;
    }

    /** Width available for a gene name: the whole row, less the widgets an added row carries. */
    private int nameWidth(boolean added) {
        return added ? listWidth() - LOCK_W - 2 * ALLELE_W - REMOVE_W - 10
                : listWidth() - LOCK_W - 8;
    }

    /** The <b>visible</b> row index under {@code (mouseX, mouseY)}, or {@code -1}. */
    private int rowAt(double mouseX, double mouseY) {
        if (mouseX < LIST_X - 4 || mouseX > LIST_X + listWidth()
                || mouseY < LIST_TOP || mouseY >= LIST_TOP + visibleRows() * ROW_H) {
            return -1;
        }
        int index = scroll + (int) ((mouseY - LIST_TOP) / ROW_H);
        return index < view.size() ? index : -1;
    }

    /** Is this point on a row's padlock rather than on the row itself? */
    private boolean inLockColumn(double mouseX) {
        return mouseX >= LIST_X - 4 && mouseX < LIST_X + LOCK_W;
    }

    /** Cut a label down until it fits, the way the breed button always has. */
    private String truncate(String text, int maxW) {
        if (this.font.width(text) <= maxW) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text);
        while (sb.length() > 1 && this.font.width(sb.toString() + "\u2026") > maxW) {
            sb.setLength(sb.length() - 1);
        }
        return sb + "\u2026";
    }

    // ------------------------------------------------------------------
    // Widgets
    // ------------------------------------------------------------------

    @Override
    protected void init() {
        rebuildView();

        int listW = listWidth();
        int aX = LIST_X + listW - 2 * ALLELE_W - REMOVE_W - 6;
        int bX = LIST_X + listW - ALLELE_W - REMOVE_W - 4;
        int xX = LIST_X + listW - REMOVE_W;

        // The gene-family filter, above the list. A hundred and seventy loci in
        // one alphabetical column is a list you scroll rather than read; this is
        // how you ask for the dilutions, or for the genes made of strokes.
        final int filterY = LIST_TOP - FILTER_H - 2;
        addRenderableWidget(Button.builder(
                        Component.literal(truncate(
                                (filter == null ? "All genes" : filter.label()) + " \u25be",
                                listW - 4)),
                        b -> openDropdown(Dd.FAMILY, LIST_X - 4, filterY + FILTER_H))
                .bounds(LIST_X - 4, filterY, listW + 4, FILTER_H).build());

        int visible = visibleRows();
        for (int i = scroll; i < view.size() && i < scroll + visible; i++) {
            final Row row = view.get(i);
            // Every row carries a padlock, added or not. It is drawn rather than
            // labelled (the font has no lock glyph in the plane it can reach),
            // so it takes its click in mouseClicked instead of being a Button -
            // see inLockColumn.
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
                                    openAlleleDropdown(row, 0, aX, ry);
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
                                    openAlleleDropdown(row, 1, bX, ry);
                                } else {
                                    row.b = (row.b + 1) % as.size();
                                    enforceSexLinkage(row);
                                    rebuildWidgets();
                                }
                            })
                    .bounds(bX, ry, ALLELE_W, ROW_H - 2).build());
            // Extension, agouti and shade carry no x - every horse has alleles
            // at all three, so there is no state in which taking one off is
            // honest. EditorRules.alwaysCarried, and the browser twin agrees.
            if (!EditorRules.alwaysCarried(row.gene)) {
                addRenderableWidget(Button.builder(
                                Component.literal("x"),
                                b -> {
                                    remove(row);
                                    rebuildWidgets();
                                })
                        .bounds(xX, ry, REMOVE_W, ROW_H - 2).build());
            }
        }

        int rx = rightX();
        int ry = LIST_TOP + 4;
        final int rightStep = rightStep();
        addRenderableWidget(Button.builder(
                        Component.literal(baby ? "Age: Foal" : "Age: Adult"),
                        b -> {
                            baby = !baby;
                            rebuildWidgets();
                        })
                .bounds(rx, ry, RIGHT_W, 20).build());
        ry += rightStep;
        addRenderableWidget(Button.builder(
                        Component.literal(female ? "Sex: Mare" : "Sex: Stallion"),
                        b -> {
                            setSex(!female);
                            rebuildWidgets();
                        })
                .bounds(rx, ry, RIGHT_W, 20).build());
        ry += rightStep;
        // Fitted to the button, not cut at a fixed character count. Twelve
        // characters was a guess at what fitted a 96px button and was wrong in
        // both directions - it truncated names that would have fitted and let
        // through ones that did not. truncate() measures.
        String breedName = breedIndex == 0 ? "(none)" : breedChoices.get(breedIndex - 1).name();
        breedName = truncate(breedName, RIGHT_W - this.font.width("Breed:  ▾") - 8);
        final int breedBtnX = rx;
        final int breedBtnY = ry;
        addRenderableWidget(Button.builder(
                        Component.literal("Breed: " + breedName + " ▾"),
                        b -> openDropdown(Dd.BREED, breedBtnX, breedBtnY))
                .bounds(rx, ry, RIGHT_W, 20).build());
        ry += rightStep;
        // Two split buttons. The face does the thing; the arrow picks which
        // thing it is, and the choice sticks - the label always says what will
        // happen next.
        final int randY = ry;
        addRenderableWidget(Button.builder(
                        Component.literal(randomizeMode.shortLabel()), b -> randomize())
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.literal(randomizeMode.label()
                                + ". The arrow beside it changes what this button does.")))
                .bounds(rx, ry, RIGHT_W - ARROW_W, 20).build());
        addRenderableWidget(Button.builder(Component.literal("▾"),
                        b -> openDropdown(Dd.RANDOMIZE, rx, randY + 20))
                .bounds(rx + RIGHT_W - ARROW_W, ry, ARROW_W, 20).build());
        ry += rightStep;
        final int addY = ry;
        addRenderableWidget(Button.builder(
                        Component.literal(addScope.shortLabel()), b -> addRandom())
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.literal("Put one gene the horse is not already carrying onto it, "
                                + "at a combination that shows. The arrow narrows it to the "
                                + "naturals or to the magic.")))
                .bounds(rx, ry, RIGHT_W - ARROW_W, 20).build());
        addRenderableWidget(Button.builder(Component.literal("▾"),
                        b -> openDropdown(Dd.ADD, rx, addY + 20))
                .bounds(rx + RIGHT_W - ARROW_W, ry, ARROW_W, 20).build());
        ry += rightStep;
        addRenderableWidget(Button.builder(
                        Component.literal(randomizeInvisible ? "Rnd health: on" : "Rnd health: off"),
                        b -> {
                            randomizeInvisible = !randomizeInvisible;
                            rebuildWidgets();
                        })
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.literal("Off: Randomize leaves alone every gene that changes "
                                + "nothing you can see - the disorders, the stat loci, the ability "
                                + "genes - so rolling a coat cannot quietly roll a lethal. On: it "
                                + "rolls those too.")))
                .bounds(rx, ry, RIGHT_W, 20).build());
        ry += rightStep;
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
                .bounds(rx, bottomStackTop() + 2 * RIGHT_STEP, RIGHT_W, 20)
                .tooltip(creative ? null : net.minecraft.client.gui.components.Tooltip.create(
                        Component.literal("The custom spawn egg builds a horse from scratch, so it is a "
                                + "creative-mode tool. Switch to creative to spawn what you have built; "
                                + "everything else on this screen works either way.")))
                .build();
        spawnButton.active = creative;
        addRenderableWidget(spawnButton);
        // The whole horse, to and from the clipboard - the same HorseFile the
        // browser designer's Export / Import writes to a file, in the same slot
        // on the same column. They replaced Copy code / Paste code, which moved
        // the alleles alone and so pasted back a different horse: fresh
        // epigenetics, no name, no breed.
        int halfW = (RIGHT_W - 4) / 2;
        addRenderableWidget(Button.builder(Component.literal("Copy horse"), b -> copyHorse())
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.literal("Put this whole horse on the clipboard - alleles, "
                                + "epigenetics, name, sex, age and breed. The horse designer's "
                                + "Import reads the same text.")))
                .bounds(rx, bottomStackTop() + RIGHT_STEP, halfW, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Paste"), b -> pasteHorse())
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.literal("Read a horse off the clipboard. Tolerant: a gene this "
                                + "build does not have is dropped, an epigenome that will not "
                                + "parse is re-rolled, an unknown breed loses its label.")))
                .bounds(rx + halfW + 4, bottomStackTop() + RIGHT_STEP, halfW, 20).build());
        Button eggButton = Button.builder(Component.literal("Make egg"), b -> makeEgg())
                .bounds(rx, bottomStackTop(), RIGHT_W, 20)
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
                .bounds(rx, bottomStackTop() + 3 * RIGHT_STEP, RIGHT_W, 20).build());
    }

    /**
     * Clicking a gene that is not on the horse adds it. The allele buttons and
     * the {@code x} on an added row are real widgets and take their own clicks
     * first, so this only ever sees the empty part of a row.
     */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (dd != Dd.NONE) {
            pickFromDropdown(event.x(), event.y()); // any click resolves or dismisses it
            return true;
        }
        // The padlock column first: it overlaps no widget, and on an added row
        // the rest of the line belongs to the allele buttons.
        int index = rowAt(event.x(), event.y());
        if (index >= 0 && inLockColumn(event.x())) {
            toggleLock(view.get(index));
            rebuildWidgets();
            return true;
        }
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (index >= 0 && !view.get(index).added) {
            add(view.get(index));
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
        if (dd != Dd.NONE) {
            int max = Math.max(0, ddLabels().size() - DD_VISIBLE);
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
        AllelePair pair = EditorRules.variantPair(row.gene);
        row.a = indexOf(row.gene, pair.first());
        row.b = indexOf(row.gene, pair.second());
        row.added = true;
        enforceSexLinkage(row); // variantPair doesn't know the screen's sex
    }

    /** A no-op on the three loci every horse visibly carries - they have no x. */
    private void remove(Row row) {
        if (EditorRules.alwaysCarried(row.gene)) {
            return;
        }
        row.added = false;
        row.a = indexOf(row.gene, row.gene.defaultAllele());
        row.b = row.a;
    }

    /** The Sex button, and the sex a whole-genome randomize rolls. */
    private void setSex(boolean isFemale) {
        female = isFemale;
        for (Row row : rows) {
            if (row.added) {
                enforceSexLinkage(row);
            }
        }
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

    /** Back to a plain horse - but a locked row is kept, which is the point of a lock. */
    private void reset() {
        for (Row row : rows) {
            if (row.locked) {
                continue;
            }
            row.added = EditorRules.alwaysCarried(row.gene);
            row.a = indexOf(row.gene, row.gene.defaultAllele());
            row.b = row.a;
        }
        breedIndex = 0;
        rerollEpigenome(new NeoRng(RandomSource.create()));
        rebuildWidgets();
    }

    /**
     * <b>The whole horse, to and from the clipboard.</b> {@link HorseFile} - the
     * alleles, the epigenetics riding on each copy, the name, the sex, the age
     * and the breed label - which is the same format the browser designer's
     * Export writes to a file, so a horse crosses between them intact.
     *
     * <p>It replaced Copy code / Paste code, which moved a genotype code and
     * nothing else. Paste one of those back and you got a different horse: same
     * alleles, fresh epigenetics, no name, no breed. On a mod whose premise is
     * that one genotype makes many horses, that is a lossy copy pretending to be
     * an exact one.
     */
    private void copyHorse() {
        String breedName = breedIndex == 0 ? "" : breedChoices.get(breedIndex - 1).name();
        Minecraft.getInstance().keyboardHandler.setClipboard(HorseFile.write(
                new HorseFile("", "", female, baby, breedName,
                        genotype().toCode(), epigenome.toCode()),
                "custom horse spawn egg"));
        say("Copied this horse to the clipboard.");
    }

    /**
     * Read one back. Deliberately tolerant - a gene this build does not have is
     * dropped, an epigenome that will not parse is re-rolled, an unknown breed
     * loses only its label - and it says which of those happened, because a
     * silent partial load is how you end up debugging a coat that was never the
     * one you copied.
     */
    private void pasteHorse() {
        String text = Minecraft.getInstance().keyboardHandler.getClipboard();
        HorseFile horse;
        try {
            horse = HorseFile.read(text);
        } catch (RuntimeException e) {
            say("That clipboard is not a horse: " + e.getMessage());
            return;
        }
        Genotype parsed = Genotype.parse(horse.genotype());
        female = horse.female();
        baby = horse.baby();
        for (Row row : rows) {
            stamp(row, parsed);
            enforceSexLinkage(row); // a hand-edited or stale file may not be sex-consistent
        }
        breedIndex = 0;
        for (int i = 0; i < breedChoices.size(); i++) {
            if (breedChoices.get(i).name().equalsIgnoreCase(horse.breed())) {
                breedIndex = i + 1;
                break;
            }
        }
        boolean droppedEpigenome = true;
        if (!horse.epigenome().isEmpty()) {
            try {
                epigenome = Epigenome.parse(horse.epigenome());
                droppedEpigenome = false;
            } catch (RuntimeException ignored) {
                epigenome = rollEpigenome();
            }
        } else {
            epigenome = rollEpigenome();
        }
        previewKey = "";
        if (droppedEpigenome) {
            say("Pasted the horse, but its epigenome did not parse - "
                    + "this one has a fresh set, so the coat will differ wherever a gene "
                    + "varies per horse.");
        }
        if (!horse.breed().isEmpty() && breedIndex == 0) {
            say("Pasted the horse, but there is no breed called \""
                    + horse.breed() + "\" in this build - the label was dropped.");
        }
        rebuildWidgets();
    }

    /**
     * A line in the client's own chat. The paste is tolerant by design, so it
     * has things to say that a screen this full has nowhere to print - and a
     * partial load that says nothing is how you end up debugging a coat that was
     * never the one you copied.
     */
    private static void say(String message) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal(message));
        }
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
        int shown = Math.min(view.size(), scroll + visible) - scroll;
        int hovered = rowAt(mouseX, mouseY);
        int nx = nameX();

        // Header band. It stops at 24 rather than 32: the backdrop is painted
        // after the widgets (see the drawing note on the class), and the gene
        // family filter sits at 24, so a taller band would paint over it.
        g.fill(0, 6, this.width, 24, PANEL);
        g.text(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2, 14, 0xFFFFFFFF);

        // the name column only - stop short of an added row's allele buttons
        // 0xE0 rather than 0x88: the gene names sit over the world (and over
        // grass, on the browser twin) and a half-transparent backing made them
        // hard to read. Keep this and wiki/horse-designer/js/gui.js NAME_BG the
        // same value.
        g.fill(LIST_X - 4, LIST_TOP - 2, LIST_X + nameWidth(true) + LOCK_W + 2,
                LIST_TOP + shown * ROW_H, 0xE0000000);

        for (int i = scroll; i < view.size() && i < scroll + visible; i++) {
            Row row = view.get(i);
            int ry = LIST_TOP + (i - scroll) * ROW_H;
            boolean overLock = i == hovered && inLockColumn(mouseX);
            if (overLock) {
                g.fill(LIST_X - 4, ry, LIST_X + LOCK_W, ry + ROW_H - 2, 0x33FFFFFF);
            }
            drawPadlock(g, LIST_X - 2, ry, row.locked);
            if (!row.added) {
                // off the horse: a plain name, the whole row clickable
                if (i == hovered && !overLock) {
                    g.fill(nx - 2, ry, LIST_X + listW, ry + ROW_H - 2, 0x33FFFFFF);
                }
                drawFitted(g, row.gene.name(), nx, ry + 6, nameWidth(false),
                        i == hovered ? 0xFFFFFFFF : 0xFF9AA0B0);
                continue;
            }
            // on the horse: name, what it expresses, and its two allele buttons
            Expression e = genotype.expressionOf(row.gene);
            boolean expressing = !e.wildType();
            g.fill(nx - 2, ry, nx + nameWidth(true) + 2, ry + ROW_H - 2, 0x33202838);
            drawFitted(g, row.gene.name(), nx, ry + 1, nameWidth(true),
                    expressing ? 0xFF9BE08A : 0xFFC8C8C8);
            drawFitted(g, expressing ? e.name() : "no effect", nx, ry + 10, nameWidth(true),
                    0xFF787888);
        }

        drawPreview(g, mouseX, mouseY);

        // the genome as text, under the list
        g.fill(LIST_X - 4, this.height - 42, LIST_X + listW, this.height - 14, 0xE0000000);
        drawFitted(g, GeneCodeDisplay.shortForm(genotype), LIST_X, this.height - 39, listW - 4, 0xFF88CC88);
        drawFitted(g, "epigenetics #" + Long.toHexString(epigenome.visibleFingerprint(genotype)),
                LIST_X, this.height - 26, listW - 4, 0xFF8890A8);

        // Last, so it sits over the preview and the genome line - but not over an
        // open dropdown, which is drawn after this and is the thing you are
        // actually pointing at.
        if (dd == Dd.NONE && hovered >= 0) {
            drawGeneBlurb(g, view.get(hovered).gene, hovered);
        }

        if (dd != Dd.NONE) {
            drawDropdown(g, mouseX, mouseY);
        }
    }

    /**
     * The padlock at the head of a row - drawn, not typed. Minecraft's font has
     * no lock glyph in the plane it can reach, and the mark has to be the same
     * on both screens, so both draw the same handful of rectangles. See
     * {@code wiki/horse-designer/js/gui.js} {@code padlock()}.
     */
    private void drawPadlock(GuiGraphicsExtractor g, int x, int y, boolean locked) {
        int colour = locked ? 0xFFE8C060 : 0xFF565C6C;
        int bx = x + 2;
        int by = y + 6;                              // a 5x6 icon, centred in the column
        if (locked) {
            g.fill(bx + 1, by, bx + 4, by + 1, colour);        // shackle, closed
            g.fill(bx + 1, by + 1, bx + 2, by + 2, colour);
            g.fill(bx + 3, by + 1, bx + 4, by + 2, colour);
        } else {
            g.fill(bx + 2, by, bx + 5, by + 1, colour);        // shackle, swung open
            g.fill(bx + 4, by + 1, bx + 5, by + 2, colour);
        }
        g.fill(bx, by + 2, bx + 5, by + 6, colour);            // body
    }

    /**
     * Whichever dropdown is open, drawn last so it sits over the widgets it
     * covers. One routine for all five: they differ only in what
     * {@link #ddLabels} answers, and two copies of this had already started to
     * drift before the third, fourth and fifth arrived.
     */
    private void drawDropdown(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        List<String> labels = ddLabels();
        int w = ddWidth();
        int cur = ddCurrent();
        int h = DD_VISIBLE * DD_ROW_H;
        g.fill(ddX - 1, ddY - 1, ddX + w + 1, ddY + h + 1, 0xF00E0E16);
        g.fill(ddX - 1, ddY - 1, ddX + w + 1, ddY, 0xFF5A6478);
        for (int i = 0; i < DD_VISIBLE; i++) {
            int idx = ddScroll + i;
            if (idx >= labels.size()) {
                break;
            }
            int ry = ddY + i * DD_ROW_H;
            boolean hov = mouseX >= ddX && mouseX < ddX + w && mouseY >= ry && mouseY < ry + DD_ROW_H;
            if (idx == cur) {
                g.fill(ddX, ry, ddX + w, ry + DD_ROW_H, 0x557088FF);
            } else if (hov) {
                g.fill(ddX, ry, ddX + w, ry + DD_ROW_H, 0x33FFFFFF);
            }
            drawFitted(g, labels.get(idx), ddX + 3, ry + 2, w - 10,
                    idx == cur ? 0xFFFFFFFF : 0xFFC0C4D0);
        }
        int max = Math.max(0, labels.size() - DD_VISIBLE);
        if (max > 0) {
            int barH = Math.max(6, h * DD_VISIBLE / labels.size());
            int barY = ddY + Math.round((h - barH) * (ddScroll / (float) max));
            g.fill(ddX + w - 2, ddY, ddX + w, ddY + h, 0x40FFFFFF);
            g.fill(ddX + w - 2, barY, ddX + w, barY + barH, 0xFF8890A8);
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
    /**
     * <b>What this gene does, while you are pointing at it.</b> Drawn as a panel
     * rather than a vanilla {@code Tooltip} for two reasons: a list row is not a
     * widget here (an unadded row takes its click in {@code mouseClicked}), and
     * the browser twin has no vanilla tooltip to mirror - a panel built out of
     * fills and text exists identically in both.
     *
     * <p>It sits to the right of the list, which is empty space on this screen,
     * so it never covers the thing you are reading. It is pushed back inside the
     * window on both axes rather than being allowed to run off the edge.
     *
     * <p>{@link Gene#description()} is documented as possibly empty - callers
     * are told to treat that as "no summary available" - so this draws nothing
     * at all rather than an empty box. Every registered gene has one today, and
     * {@code GeneDescriptionCoverageTest} keeps it that way; a drop-in gene with
     * no blurb is the case this guards.
     */
    private void drawGeneBlurb(GuiGraphicsExtractor g, Gene gene, int hoveredIndex) {
        String blurb = gene.description();
        if (blurb == null || blurb.isBlank()) {
            return;
        }
        List<String> lines = wrap(blurb, BLURB_W - 2 * BLURB_PAD);
        String heading = gene.name();

        int h = BLURB_PAD * 2 + BLURB_LINE_H + 2 + lines.size() * BLURB_LINE_H;
        int x = LIST_X + listWidth() + 6;
        int y = LIST_TOP + (hoveredIndex - scroll) * ROW_H - 2;
        x = Math.min(x, this.width - BLURB_W - 4);
        x = Math.max(4, x);
        y = Math.min(y, this.height - h - 4);
        y = Math.max(4, y);

        g.fill(x - 1, y - 1, x + BLURB_W + 1, y + h + 1, 0xF00E0E16);
        g.fill(x - 1, y - 1, x + BLURB_W + 1, y, 0xFF5A6478);
        drawFitted(g, heading, x + BLURB_PAD, y + BLURB_PAD, BLURB_W - 2 * BLURB_PAD, 0xFFFFFFFF);
        int ty = y + BLURB_PAD + BLURB_LINE_H + 2;
        for (String line : lines) {
            g.text(this.font, Component.literal(line), x + BLURB_PAD, ty, 0xFFC0C4D0);
            ty += BLURB_LINE_H;
        }
    }

    /**
     * Greedy word wrap to a pixel width. Vanilla's {@code font.split} would do
     * this, but it returns {@code FormattedCharSequence}s and the browser twin
     * has to wrap the same string to the same width with its own measurer - so
     * this is written out, in plain words, once per side. A single word longer
     * than the line is left long rather than broken mid-word; nothing in a gene
     * blurb is, and a hyphenated break reads worse than a slightly wide line.
     */
    private List<String> wrap(String text, int maxW) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (this.font.width(candidate) <= maxW || line.isEmpty()) {
                line.setLength(0);
                line.append(candidate);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

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
