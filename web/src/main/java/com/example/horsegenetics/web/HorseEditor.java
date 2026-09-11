package com.example.horsegenetics.web;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.BreedFounder;
import com.example.horsegenetics.common.breed.Breeds;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.EditorRules;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.eye.Eyes;
import com.example.horsegenetics.common.genetics.Inheritance;
import com.example.horsegenetics.common.genetics.RandomizeMode;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.name.HorseNameGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * The horse designer's editor model - <b>a deliberate twin of
 * {@code CustomHorseSpawnScreen}</b>.
 *
 * <p><b>Keep these two in step.</b> The spawn egg is the in-game gene tester and
 * this is the browser one; they are meant to behave identically, so a change to
 * one is a change to both. That is why this is Java rather than JavaScript:
 * the two files can be read side by side in the same language, and every rule
 * that matters - which allele a gene is added at, how a sex-linked locus snaps,
 * what "Randomize" means with a breed selected - is the same code shape in
 * both. See {@code wiki/horse-designer/} and the note at the top of
 * {@code CustomHorseSpawnScreen}.
 *
 * <p>What this class is <i>not</i> is a view. It holds no pixels and no layout;
 * it answers questions and takes edits. The browser draws it.
 *
 * <h2>Why the gene rows are a snapshot, and why that is dangerous here</h2>
 * The constructor takes {@code Genes.codeOrder()} once, exactly as
 * {@code CustomHorseSpawnScreen} does, because both address gene rows by index.
 * On the screen's side that is safe at any moment - {@code Genes} registers the
 * shipped gene files from its own class initialiser, so the registry is
 * complete before anything can hold a reference to it.
 *
 * <p><b>In the browser it is not.</b> TeaVM cannot read a classpath index, so
 * the page fetches the gene bundle and hands it in after start-up, and an editor
 * built before that call holds a list with none of the data-driven genes in it.
 * That is not hypothetical: {@code DesignerApi.main} used to build one, the page
 * calls {@code main} first, and eighty-four genes registered cleanly and none of
 * them appeared. The editor is built lazily now and dropped on every
 * registration. <b>If you add a boot-time call that touches the editor, put it
 * after {@code registerGenes}.</b>
 *
 * <h2>The deliberate divergences</h2>
 * Both are the same one difference wearing two hats: <b>a browser page has no
 * world and no inventory</b>. So the screen's two output actions have no twin
 * here and are not missing features -
 * <ul>
 *   <li><b>Spawn</b> - there is nothing to spawn into, and no server to send a
 *       packet to.</li>
 *   <li><b>Make egg</b> - which writes the horse on screen into a
 *       {@code preset_horse_spawn_egg}. There is no inventory to put an item
 *       in.</li>
 * </ul>
 *
 * <p><b>And one in how a horse leaves the screen.</b> Both carry the same
 * payload - {@code HorseFile}, the whole horse rather than its alleles - but the
 * page writes it to a file you can keep and the screen writes it to the
 * clipboard, because a Minecraft screen has no file picker and a browser tab
 * has no chat to paste into. Same format, so a horse exported here loads in
 * game and back.
 *
 * <p>Everything else - the row list, the locks, the sex flag, the breed index,
 * the randomize mode, the epigenome - is the same state and must stay so.
 */
public final class HorseEditor {

    /** One gene, and whether it is on the horse. Mirrors the screen's {@code Row}. */
    static final class Row {
        final Gene gene;
        int a;
        int b;
        boolean added;
        /** Held against every randomize - see {@link HorseEditor#setLocked}. */
        boolean locked;

        Row(Gene gene) {
            this.gene = gene;
            this.a = indexOf(gene, gene.defaultAllele());
            this.b = this.a;
            this.added = EditorRules.alwaysCarried(gene);
        }
    }

    private final List<Row> rows = new ArrayList<>();
    /**
     * <b>Not</b> captured in a field. In the browser the breed registry is
     * filled from a fetched bundle (TeaVM cannot read the classpath the game
     * reads breed files off), so a list snapshotted when this object was built
     * would be the empty one - and the breed dropdown would be empty on a page
     * that had loaded every breed correctly.
     */
    List<Breed> breeds() {
        return Breeds.all();
    }

    private boolean female = true;
    private boolean baby = false;
    private int breedIndex = 0;
    private Epigenome epigenome;

    /** What the Randomize split button will do next time it is pressed. */
    private RandomizeMode randomizeMode = RandomizeMode.RANDOM;
    /** What the Add random split button will do next time it is pressed. */
    private EditorRules.AddScope addScope = EditorRules.AddScope.ANY;
    /**
     * Whether a randomize is allowed to touch the loci that change nothing you
     * can see - the disorders, the stat genes, the ability genes. Off, because
     * the overwhelmingly common reason to press Randomize on a gene editor is
     * to look at a coat, and rolling a lethal into the horse you are looking at
     * is a surprise nobody asked for. {@link Genes#influencesCoat} draws the
     * line.
     */
    private boolean randomizeInvisible = false;

    /**
     * Every horse has a name, the way every horse in game does - it is rolled
     * once here rather than left blank, because an unnamed horse reads as a
     * placeholder and this page is meant to show you a horse.
     */
    private HorseNameGenerator names;
    private String first = "";
    private String last = "";

    public HorseEditor(Rng rng) {
        // Every registered gene is a row, alphabetically by display name - and
        // the sex locus is excluded, because the Sex button owns it. Same rule,
        // same reason, as the spawn screen.
        List<Gene> all = new ArrayList<>(Genes.codeOrder());
        all.removeIf(g -> g == Genes.SEX);
        all.sort((x, y) -> x.name().compareToIgnoreCase(y.name()));
        for (Gene g : all) {
            rows.add(new Row(g));
        }
        this.epigenome = Epigenome.random(rng);
    }

    /**
     * Hand in the two word tables. They arrive from the page rather than from
     * the classpath because {@code getResourceAsStream} is the one thing TeaVM
     * is weakest at - and the same reason the coat textures arrive as pixels.
     * {@link HorseNameGenerator} has a public constructor for exactly this.
     */
    void setNameWords(List<String> alpha, List<String> beta, Rng rng) {
        this.names = new HorseNameGenerator(alpha, beta);
        rerollName(3, rng);
    }

    public String first() {
        return first;
    }

    public String last() {
        return last;
    }

    void setName(String firstName, String lastName) {
        this.first = firstName == null ? "" : firstName;
        this.last = lastName == null ? "" : lastName;
    }

    /**
     * Read an epigenome code back. Separate from {@link #paste} because a
     * genotype code and an epigenome code are two independent facts - a pasted
     * genotype keeps the epigenome you were looking at, while an imported horse
     * brings its own.
     */
    boolean setEpigenome(String code) {
        try {
            this.epigenome = Epigenome.parse(code.trim());
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Set the breed <b>label</b> without rolling a horse of it. Picking a breed
     * from the dropdown is a request for a new horse; importing one is not -
     * the genotype in the file already is that horse, and re-rolling would
     * throw away the thing being imported.
     */
    boolean stampBreed(String name) {
        if (name == null || name.isEmpty()) {
            breedIndex = 0;
            return true;
        }
        List<Breed> breeds = breeds();
        for (int i = 0; i < breeds.size(); i++) {
            if (breeds.get(i).name().equalsIgnoreCase(name)) {
                breedIndex = i + 1;
                return true;
            }
        }
        breedIndex = 0;
        return false;
    }

    /** @param halves bit 1 the first name, bit 2 the last - so 3 is both. */
    void rerollName(int halves, Rng rng) {
        if (names == null) {
            return;
        }
        HorseNameGenerator.NameParts parts = names.generateParts(rng);
        if ((halves & 1) != 0) {
            first = parts.first();
        }
        if ((halves & 2) != 0) {
            last = parts.last();
        }
    }

    // ---- reading ---------------------------------------------------------

    List<Row> rows() {
        return rows;
    }



    public boolean female() {
        return female;
    }

    public boolean baby() {
        return baby;
    }

    public int breedIndex() {
        return breedIndex;
    }

    public Epigenome epigenome() {
        return epigenome;
    }

    /**
     * The horse as it stands - every added row's pair, plus the sex locus.
     *
     * <p><b>Forced.</b> What comes back is the horse as it would be <i>born</i>:
     * {@link Eyes#force} writes the eye alleles that the rest of the genotype
     * asks for, exactly as {@code Genome.random} and {@code Genome.breedWith}
     * do. Without it a splashed white horse built by hand here would preview
     * with brown eyes and spawn with brown eyes, while every splashed white
     * horse in the world had blue ones - and the whole point of this screen is
     * that it is the game. A player who sets an eye row on a horse whose coat
     * genes ask for an eye colour is therefore overruled, which is also what
     * happens to the foal.
     *
     * <p>The twin does the same thing in the same place - see the other file.
     */
    public Genotype genotype() {
        Genotype gt = Genotype.wildType().withSex(female ? Sex.FEMALE : Sex.MALE);
        for (Row row : rows) {
            if (!row.added) {
                continue;
            }
            List<Allele> as = row.gene.alleles();
            gt = gt.with(new AllelePair(as.get(row.a), as.get(row.b)));
        }
        return Eyes.force(gt, epigenome);
    }

    public Genome genome() {
        return new Genome(genotype(), epigenome);
    }

    // ---- editing ---------------------------------------------------------

    /**
     * Put a gene on the horse at {@link EditorRules#variantPair} - the first
     * combination that actually does something. A gene you just added should
     * <i>show</i>, or adding it taught you nothing.
     */
    public void add(int index) {
        Row row = rows.get(index);
        if (row.added) {
            return;
        }
        showVariant(row);
    }

    /** Put a row on the horse at the pair that shows. Shared by add and Add random. */
    private void showVariant(Row row) {
        AllelePair pair = EditorRules.variantPair(row.gene);
        row.a = indexOf(row.gene, pair.first());
        row.b = indexOf(row.gene, pair.second());
        row.added = true;
        enforceSexLinkage(row);
    }

    /**
     * Take a gene off the horse - except the three every horse visibly has, at
     * which no {@code x} is drawn and this is a no-op
     * ({@link EditorRules#alwaysCarried}).
     */
    public void remove(int index) {
        Row row = rows.get(index);
        if (EditorRules.alwaysCarried(row.gene)) {
            return;
        }
        row.added = false;
        int def = indexOf(row.gene, row.gene.defaultAllele());
        row.a = def;
        row.b = def;
    }

    /**
     * <b>Lock a row.</b> A locked gene is held exactly as it stands against
     * every randomize - its alleles, whether it is on the horse at all, and its
     * epigenetics. It is the difference between "roll me another horse" and
     * "roll me another horse with <i>this</i> tobiano", and the second is what
     * a gene editor is for.
     */
    public void setLocked(int index, boolean locked) {
        rows.get(index).locked = locked;
    }

    public void setRandomizeMode(RandomizeMode mode) {
        this.randomizeMode = mode;
    }

    public RandomizeMode randomizeMode() {
        return randomizeMode;
    }

    public void setAddScope(EditorRules.AddScope scope) {
        this.addScope = scope;
    }

    public EditorRules.AddScope addScope() {
        return addScope;
    }

    public void setRandomizeInvisible(boolean on) {
        this.randomizeInvisible = on;
    }

    public boolean randomizeInvisible() {
        return randomizeInvisible;
    }

    /** Step one allele slot to the next allele - the &le; 3 allele case. */
    public void cycle(int index, int slot) {
        Row row = rows.get(index);
        int n = row.gene.alleles().size();
        if (slot == 0) {
            row.a = (row.a + 1) % n;
        } else {
            row.b = (row.b + 1) % n;
        }
        enforceSexLinkage(row);
    }

    /** Set one allele slot outright - what the dropdown does past three alleles. */
    public void setAllele(int index, int slot, int allele) {
        Row row = rows.get(index);
        int n = row.gene.alleles().size();
        int a = Math.max(0, Math.min(allele, n - 1));
        if (slot == 0) {
            row.a = a;
        } else {
            row.b = a;
        }
        enforceSexLinkage(row);
    }

    public void setSex(boolean isFemale) {
        this.female = isFemale;
        for (Row row : rows) {
            if (row.added) {
                enforceSexLinkage(row);
            }
        }
    }

    public void setBaby(boolean isBaby) {
        this.baby = isBaby;
    }

    /**
     * Pick a breed. Index 0 is Feral Mixed (no preset); anything else rolls a fresh wild
     * founder of that breed - genotype, epigenome and sex - into the editor,
     * exactly as the screen's breed dropdown does. You can still hand-edit any
     * locus afterwards.
     */
    public void setBreed(int index, Rng rng) {
        this.breedIndex = Math.max(0, Math.min(index, breeds().size()));
        if (breedIndex != 0) {
            rerollName(3, rng);
            applyGenome(BreedFounder.roll(breeds().get(breedIndex - 1), rng), true, null);
        }
    }

    /**
     * <b>Randomize, in whichever of {@link RandomizeMode}'s senses is selected.</b>
     *
     * <p>The order is load-bearing, and is the same on both screens:
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
     * {@link #randomizeInvisible} is on - a row that changes nothing you can see
     * are all left exactly as they were, epigenetics included.
     */
    public void randomize(Rng rng) {
        RandomizeMode mode = randomizeMode;
        if (mode == RandomizeMode.EPIGENETICS) {
            rerollEpigenome(rng);
            return;
        }
        rerollName(3, rng);   // a different horse deserves a different name
        if (mode.rollsSex()) {
            setSex(rng.nextBoolean());
        }
        if (mode == RandomizeMode.BREED && !breeds().isEmpty()) {
            breedIndex = 1 + rng.nextInt(breeds().size());
        }
        Sex sex = female ? Sex.FEMALE : Sex.MALE;
        Genome g;
        if (mode == RandomizeMode.TRUE_RANDOM) {
            g = new Genome(EditorRules.trueRandom(rng, sex), Epigenome.random(rng));
        } else if (breedIndex == 0) {
            g = Genome.random(rng);
        } else {
            g = BreedFounder.roll(breeds().get(breedIndex - 1), rng, sex);
        }
        applyGenome(g, false, mode);
        if (mode.magicalFloor() > 0) {
            topUpMagical(mode.magicalFloor(), rng);
        }
    }

    /**
     * Add magical genes, at the pair that shows, until the horse shows at least
     * {@code want} of them. Genes already showing count, so <i>+1 magical</i> on
     * a horse that rolled a galaxy coat adds nothing.
     */
    private void topUpMagical(int want, Rng rng) {
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
            showVariant(candidates.remove(rng.nextInt(candidates.size())));
            showing++;
        }
    }

    /**
     * <b>Add random.</b> One gene the horse is not already carrying, put on at
     * the pair that shows - the fastest way to meet a gene you did not know
     * existed. {@link EditorRules.AddScope} narrows it to the naturals or the
     * magic. A no-op when everything in scope is already on, or locked.
     */
    public void addRandom(Rng rng) {
        List<Row> candidates = new ArrayList<>();
        for (Row row : rows) {
            if (!row.added && addScope.covers(row.gene) && randomizable(row)) {
                candidates.add(row);
            }
        }
        if (!candidates.isEmpty()) {
            showVariant(candidates.get(rng.nextInt(candidates.size())));
        }
    }

    /**
     * A fresh epigenome, with every locked locus keeping the numbers it had.
     * The gene is what a lock is on, and its epigenetics are part of that gene
     * on this horse - a lock that let the dapple pattern re-roll underneath the
     * allele would not be a lock.
     */
    public void rerollEpigenome(Rng rng) {
        Epigenome fresh = Epigenome.random(rng);
        for (Row row : rows) {
            if (!randomizable(row) && Epigenome.carries(row.gene)) {
                fresh = fresh.with(row.gene.key(), epigenome.copies(row.gene));
            }
        }
        this.epigenome = fresh;
    }

    /** Is this row's gene something a randomize is allowed to move? */
    private boolean randomizable(Row row) {
        return !row.locked && (randomizeInvisible || Genes.influencesCoat(row.gene));
    }

    public void clearGenes() {
        for (Row row : rows) {
            if (row.locked) {
                continue;
            }
            int def = indexOf(row.gene, row.gene.defaultAllele());
            row.a = def;
            row.b = def;
            row.added = EditorRules.alwaysCarried(row.gene);
        }
        breedIndex = 0;
    }

    /**
     * Read a genotype code back into the editor. A locus sitting at its
     * baseline is <b>not</b> marked added, or a paste would come back as forty
     * rows of {@code N/N} - the screen's rule, and the reason paste is worth
     * having at all.
     */
    public boolean paste(String code) {
        Genotype gt;
        try {
            gt = Genotype.parse(code.trim());
        } catch (RuntimeException e) {
            return false;
        }
        female = gt.sex() == Sex.FEMALE;
        for (Row row : rows) {
            stamp(row, gt);
        }
        breedIndex = 0;
        return true;
    }

    /**
     * Stamp a rolled genome onto the editor - the epigenome, optionally the
     * sex, and every locus the mode is <b>allowed</b> to touch.
     *
     * <p>{@code mode} is {@code null} for the stamps that are not a randomize at
     * all (a breed picked by hand, a horse imported from a file): those replace
     * the horse outright, locks and all, because they are not "roll me another
     * one", they are "here is a different horse".
     */
    private void applyGenome(Genome g, boolean adoptSex, RandomizeMode mode) {
        Genotype gt = g.genotype();
        if (adoptSex) {
            female = gt.sex() == Sex.FEMALE;
        }
        Epigenome next = g.epigenome();
        for (Row row : rows) {
            if (mode != null && !(mode.covers(row.gene) && randomizable(row))) {
                // Untouched - and its epigenetics stay untouched with it.
                if (Epigenome.carries(row.gene)) {
                    next = next.with(row.gene.key(), epigenome.copies(row.gene));
                }
                continue;
            }
            stamp(row, gt);
        }
        this.epigenome = next;
    }

    /**
     * One row from a genotype. A locus sitting at its baseline is <b>not</b>
     * marked added - or a paste would come back as forty rows of {@code N/N} -
     * unless it is one of the three every horse visibly carries.
     */
    private void stamp(Row row, Genotype gt) {
        AllelePair pair = gt.pair(row.gene);
        int def = indexOf(row.gene, row.gene.defaultAllele());
        row.a = indexOf(row.gene, pair.first());
        row.b = indexOf(row.gene, pair.second());
        row.added = !(row.a == def && row.b == def) || EditorRules.alwaysCarried(row.gene);
    }

    // ---- the one rule still written out here -------------------------------
    //
    // variantPair moved to EditorRules in common/ - it is the same rule on both
    // screens and had been written out twice. This one has not: it works on the
    // row's two allele *indices* rather than on an AllelePair, because a pair
    // canonicalises its slots on construction and the placeholder would not
    // stay in the slot the screen draws it in.

    /**
     * Keep a sex-linked row honest against the current sex - a stallion has one
     * {@code X} and cannot carry two real copies of an {@code X}-linked gene (a
     * {@code Brn/Brn} stallion), and the mirror holds for a {@code Y}-linked
     * one. Neither an allele edit nor the Sex button knows about the other on
     * its own, so this runs after both. A no-op on an autosomal gene.
     */
    private void enforceSexLinkage(Row row) {
        Inheritance mode = row.gene.inheritance();
        if (!mode.sexLinked()) {
            return;
        }
        int placeholder = indexOf(row.gene, row.gene.hemizygousPlaceholder());
        int def = indexOf(row.gene, row.gene.defaultAllele());
        boolean aReal = row.a != placeholder;
        boolean bReal = row.b != placeholder;
        int allowed = mode.copiesIn(female ? Sex.FEMALE : Sex.MALE);
        int have = (aReal ? 1 : 0) + (bReal ? 1 : 0);
        if (have == allowed) {
            return;
        }
        switch (allowed) {
            case 2 -> {
                if (!aReal) {
                    row.a = bReal ? row.b : def;
                }
                if (!bReal) {
                    row.b = aReal ? row.a : def;
                }
            }
            case 1 -> {
                if (have == 0) {
                    row.a = def;
                    row.b = placeholder;
                } else {
                    row.b = placeholder;
                }
            }
            default -> {
                row.a = placeholder;
                row.b = placeholder;
            }
        }
    }

    static int indexOf(Gene gene, Allele allele) {
        List<Allele> as = gene.alleles();
        for (int i = 0; i < as.size(); i++) {
            if (as.get(i).equals(allele)) {
                return i;
            }
        }
        return 0;
    }
}
