package com.example.horsegenetics.web;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.BreedFounder;
import com.example.horsegenetics.common.breed.Breeds;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.Inheritance;
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
 * <h2>The deliberate divergences</h2>
 * Both are the same one difference wearing two hats: <b>a browser page has no
 * world and no inventory</b>. So the screen's two output actions have no twin
 * here and are not missing features -
 * <ul>
 *   <li><b>Spawn</b> - there is nothing to spawn into, and no server to send a
 *       packet to.</li>
 *   <li><b>Make egg</b> - which writes the horse on screen into a
 *       {@code preset_horse_spawn_egg}. There is no inventory to put an item
 *       in. The page's equivalent is already there and better suited to a
 *       browser: copy the genotype code, which is the same horse in a form you
 *       can paste anywhere, this page included.</li>
 * </ul>
 * Everything else - the row list, the sex flag, the breed index, the epigenome -
 * is the same state and must stay so.
 */
public final class HorseEditor {

    /** One gene, and whether it is on the horse. Mirrors the screen's {@code Row}. */
    static final class Row {
        final Gene gene;
        int a;
        int b;
        boolean added;

        Row(Gene gene) {
            this.gene = gene;
            this.a = indexOf(gene, gene.defaultAllele());
            this.b = this.a;
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

    /** The horse as it stands - every added row's pair, plus the sex locus. */
    public Genotype genotype() {
        Genotype gt = Genotype.wildType().withSex(female ? Sex.FEMALE : Sex.MALE);
        for (Row row : rows) {
            if (!row.added) {
                continue;
            }
            List<Allele> as = row.gene.alleles();
            gt = gt.with(new AllelePair(as.get(row.a), as.get(row.b)));
        }
        return gt;
    }

    public Genome genome() {
        return new Genome(genotype(), epigenome);
    }

    // ---- editing ---------------------------------------------------------

    /**
     * Put a gene on the horse, homozygous for the first allele that does
     * something - and if the horse cannot carry that pair, the next one it can.
     * Verbatim the screen's {@code variantPair} rule: a gene you just added
     * should <i>show</i>, or adding it taught you nothing.
     */
    public void add(int index) {
        Row row = rows.get(index);
        if (row.added) {
            return;
        }
        AllelePair pair = variantPair(row.gene);
        row.a = indexOf(row.gene, pair.first());
        row.b = indexOf(row.gene, pair.second());
        row.added = true;
        enforceSexLinkage(row);
    }

    public void remove(int index) {
        Row row = rows.get(index);
        row.added = false;
        int def = indexOf(row.gene, row.gene.defaultAllele());
        row.a = def;
        row.b = def;
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
     * Pick a breed. Index 0 is "(none)"; anything else rolls a fresh wild
     * founder of that breed - genotype, epigenome and sex - into the editor,
     * exactly as the screen's breed dropdown does. You can still hand-edit any
     * locus afterwards.
     */
    public void setBreed(int index, Rng rng) {
        this.breedIndex = Math.max(0, Math.min(index, breeds().size()));
        if (breedIndex != 0) {
            rerollName(3, rng);
            applyGenome(BreedFounder.roll(breeds().get(breedIndex - 1), rng), true);
        }
    }

    /**
     * With a breed selected this is a fresh {@link BreedFounder#roll} of it, so
     * the alleles stay inside that breed's pools and stat targets; with
     * "(none)" it is an unconstrained {@link Genome#random}. The chosen sex is
     * kept either way.
     */
    public void randomize(Rng rng) {
        rerollName(3, rng);   // a different horse deserves a different name
        Genome g = breedIndex == 0
                ? Genome.random(rng)
                : BreedFounder.roll(breeds().get(breedIndex - 1), rng, female ? Sex.FEMALE : Sex.MALE);
        applyGenome(g, false);
    }

    public void rerollEpigenome(Rng rng) {
        this.epigenome = Epigenome.random(rng);
    }

    public void clearGenes() {
        for (Row row : rows) {
            int def = indexOf(row.gene, row.gene.defaultAllele());
            row.a = def;
            row.b = def;
            row.added = false;
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
            AllelePair pair = gt.pair(row.gene);
            int def = indexOf(row.gene, row.gene.defaultAllele());
            row.a = indexOf(row.gene, pair.first());
            row.b = indexOf(row.gene, pair.second());
            row.added = !(row.a == def && row.b == def);
        }
        breedIndex = 0;
        return true;
    }

    /** Stamp a rolled genome onto the editor - every locus, the epigenome, optionally the sex. */
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
        this.epigenome = g.epigenome();
    }

    // ---- the two rules worth stating twice --------------------------------

    /**
     * The pair a gene is added at: <b>a combination that actually shows</b>.
     * Candidates in order of how obvious they are - each variant allele
     * homozygous, then two <i>different</i> variant alleles, then one variant
     * against the baseline - and the first that both {@code canOccur} and is
     * not a wild type wins; failing that, the first carryable one; failing
     * that, the baseline. {@code KIT}'s four nonviable {@code W} homozygotes
     * and {@code MET}'s {@code met/met} are what the {@code canOccur} checks
     * are for, and magic sectoral heterochromia - every one of whose
     * homozygotes is silent by design - is what the wild-type test is for.
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
