package com.example.horsegenetics.web;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.coat.pattern.CoatTextureComposer;
import com.example.horsegenetics.common.coat.pattern.GradientLut;
import com.example.horsegenetics.common.coat.pattern.LutSet;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.AbilityContribution;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.EpigeneticAbilityContribution;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.GenotypeCatalog;
import com.example.horsegenetics.common.genetics.genes.CutieMarkGene;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneCodeDisplay;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.common.trait.Traits;

import org.teavm.jso.JSExport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything the horse designer page can ask of the mod, and the only thing it
 * is allowed to ask.
 *
 * <p><b>The point of this class.</b> The page is served from GitHub Pages, which
 * is static, so there is no server to run the coat pipeline on. TeaVM compiles
 * this module - and {@code common/} with it - to WebAssembly, so the browser
 * runs the mod's <i>actual</i> code rather than a JavaScript retelling of it.
 * Everything genetic lives behind this facade: the JavaScript draws pixels and
 * forwards clicks and knows nothing about alleles.
 *
 * <p><b>The boundary is numbers and strings.</b> Structured answers go out as
 * JSON, coats come back as {@code int[]} (which reaches JavaScript as an
 * {@code Int32Array}), and every edit goes in as an index. Nothing is
 * serialised that did not have to be, and no resource is read - the page
 * decodes the coat template and the gradient PNGs it already has inlined and
 * hands the pixels in, which is why TeaVM's weak spot never comes up.
 *
 * @see HorseEditor the model, and its note about staying in step with
 *      {@code CustomHorseSpawnScreen}
 */
public final class DesignerApi {

    private static HorseEditor editor;
    private static final JavaRng RNG = new JavaRng();

    private static GradientLut baseLut;
    private static final Map<String, GradientLut> altLuts = new LinkedHashMap<>();
    private static int[] adultTemplate;
    private static int[] babyTemplate;

    private DesignerApi() {
    }

    public static void main(String[] args) {
        editor = new HorseEditor(RNG);
    }

    private static HorseEditor editor() {
        if (editor == null) {
            editor = new HorseEditor(RNG);
        }
        return editor;
    }

    // ---- assets in ---------------------------------------------------------

    /** The red/black gradient, decoded by the page from the PNG the mod ships. */
    @JSExport
    public static void setGradient(int[] argb, int width, int height) {
        baseLut = new GradientLut(argb, width, height);
    }

    /**
     * An alternate palette for the LUT locus - {@code "bluepink"} today. Keyed
     * exactly as {@code LutContribution.lutResources()} keys it, so a horse
     * homozygous for a variant resolves against the right chart.
     */
    @JSExport
    public static void setAlternateGradient(String key, int[] argb, int width, int height) {
        altLuts.put(key, new GradientLut(argb, width, height));
    }

    /** The white horse template, adult or foal. */
    @JSExport
    public static void setTemplate(boolean adult, int[] argb) {
        if (adult) {
            adultTemplate = argb;
        } else {
            babyTemplate = argb;
        }
    }

    /**
     * The two name word tables, newline-separated, fetched by the page. They
     * come in rather than being read off the classpath because
     * {@code getResourceAsStream} is TeaVM's weakest spot - the same reason the
     * textures do.
     */
    @JSExport
    public static void setNameWords(String alpha, String beta) {
        editor().setNameWords(lines(alpha), lines(beta), RNG);
    }

    private static List<String> lines(String text) {
        List<String> out = new ArrayList<>();
        // Split on the newline char itself - no regex, no escape to mangle.
        for (String line : text.split(String.valueOf((char) 10))) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        return out;
    }

    /** @param halves bit 1 the first name, bit 2 the last - so 3 is both. */
    @JSExport
    public static void rerollName(int halves) {
        editor().rerollName(halves, RNG);
    }

    @JSExport
    public static boolean ready() {
        return baseLut != null && adultTemplate != null && babyTemplate != null;
    }

    // ---- the coat ----------------------------------------------------------

    /**
     * Bake the horse as it currently stands. The whole pipeline: every natural
     * gene restricting pigment, the gradient resolve, every magical gene adding
     * signed RGB, the composite onto the template, the eyes, the overlay phase.
     *
     * @return a 128x128 ARGB sheet, reaching JavaScript as an {@code Int32Array}
     */
    @JSExport
    public static int[] coat(boolean adult) {
        if (!ready()) {
            return new int[HorseSkinGeometry.SHEET_SIZE * HorseSkinGeometry.SHEET_SIZE];
        }
        return CoatTextureComposer.compose(
                editor().genotype(), editor().epigenome(),
                adult ? Skin.ADULT : Skin.BABY, adult,
                adult ? adultTemplate : babyTemplate,
                new LutSet(baseLut, altLuts));
    }

    @JSExport
    public static int sheetSize() {
        return HorseSkinGeometry.SHEET_SIZE;
    }

    // ---- the registry, once ------------------------------------------------

    /**
     * Every gene the page shows, in the order it shows them - alphabetically by
     * display name, the sex locus excluded because the Sex button owns it. Sent
     * once; it cannot change while the page is open.
     */
    @JSExport
    public static String genesJson() {
        Json j = new Json().arr();
        for (HorseEditor.Row row : editor().rows()) {
            Gene g = row.gene;
            j.obj()
                    .kv("key", g.key())
                    .kv("name", g.name())
                    .kv("natural", g.isNatural())
                    .kv("priority", g.priority())
                    .kv("paints", g.affectsCoat())
                    .kv("sexLinked", g.inheritance().sexLinked())
                    .kv("defaultIndex", HorseEditor.indexOf(g, g.defaultAllele()))
                    .kv("shows", showsAs(g))
                    .key("alleles").arr();
            for (Allele a : g.alleles()) {
                j.obj().kv("token", a.token()).kv("label", a.label()).endObj();
            }
            j.endArr().endObj();
        }
        return j.endArr().toString();
    }

    /**
     * What, if anything, this page can show of a gene - <b>derived, never
     * typed</b>.
     *
     * <p>A hand-written list of "genes you cannot see in a browser" is wrong the
     * day someone adds a gene and forgets it, and wrong <i>silently</i>: the
     * page would go on offering a row that does nothing. So this asks the gene
     * itself, the way {@code SpliceSafety} asks whether a locus can hurt a
     * horse.
     *
     * <ul>
     *   <li>{@code "coat"} - it paints. {@link Gene#affectsCoat()} is exactly
     *       "is any of my outcomes not a wild type", so this is free.</li>
     *   <li>{@code "size"} - it moves the horse's scale, which the field
     *       draws.</li>
     *   <li>{@code "condition"} - it declares a {@link Condition}, which the
     *       page raises as a toast.</li>
     *   <li>{@code "ability"} - it grants game behaviour and nothing else:
     *       particles, item icons, world blocks, mob effects. <b>Nothing about
     *       it exists outside Minecraft.</b></li>
     *   <li>{@code "stats"} - it moves speed, health or jump and no more. Real,
     *       and invisible: the numbers ride along in the JSON export.</li>
     * </ul>
     */
    private static String showsAs(Gene g) {
        if (g.affectsCoat() || readByAPainter(g)) {
            return "coat";
        }
        Epigenome epi = Epigenome.fromSeed(0x5EEDL);
        // The cutie mark paints nothing and moves nothing, but it is not a
        // stat either: the emblem is item icons out of the game's registry,
        // drawn by a client render layer. markFor is the one API in common/
        // that produces one, so asking it is still asking the model rather
        // than keeping a list.
        if (g instanceof CutieMarkGene mark) {
            for (AllelePair pair : GenotypeCatalog.allPairsOf(g)) {
                if (mark.markFor(Genotype.wildType().with(pair), epi).isPresent()) {
                    return "ability";
                }
            }
        }
        for (AllelePair pair : GenotypeCatalog.allPairsOf(g)) {
            Traits t = HorseTraits.resolve(Genotype.wildType().with(pair), epi, true);
            if (Math.abs(t.scale() - 1.0) > 0.005) {
                return "size";
            }
            if (!t.conditions().isEmpty()) {
                return "condition";
            }
        }
        return (g instanceof AbilityContribution || g instanceof EpigeneticAbilityContribution)
                ? "ability" : "stats";
    }

    /**
     * Does some other gene fold this one's alleles into the coat?
     * {@code PATN1} and {@code PATN2} paint nothing on their own and would
     * otherwise look invisible, but the leopard complex names them in
     * {@link Gene#coatDependsOn()} and a horse carrying them looks different.
     * The relationship is declared, so read it rather than special-case them.
     */
    private static boolean readByAPainter(Gene g) {
        for (Gene other : com.example.horsegenetics.common.genetics.Genes.codeOrder()) {
            if (other != g && other.affectsCoat() && other.coatDependsOn().contains(g.key())) {
                return true;
            }
        }
        return false;
    }

    /** "(none)" plus every breed, in {@link com.example.horsegenetics.common.breed.Breeds#all()} order. */
    @JSExport
    public static String breedsJson() {
        Json j = new Json().arr().val("(none)");
        for (Breed b : editor().breeds()) {
            j.val(b.name());
        }
        return j.endArr().toString();
    }

    // ---- the horse, every frame it changes ---------------------------------

    /**
     * Everything the panel draws: each row's allele slots and what it expresses,
     * the sex and age flags, the breed, the short genome line, the epigenetic
     * fingerprint, and the resolved body.
     *
     * <p>The expression name is {@link Genotype#expressionOf}, so a row says
     * what the gene does <i>on this horse</i> - a bay's agouti reads as bay, a
     * chestnut's reads as no effect, which is the whole reason the screen shows
     * it rather than the allele tokens alone.
     */
    @JSExport
    public static String stateJson() {
        HorseEditor e = editor();
        Genotype gt = e.genotype();
        Traits traits = HorseTraits.resolve(gt, e.epigenome(), true);

        Json j = new Json().obj()
                .kv("first", e.first())
                .kv("last", e.last())
                .kv("female", e.female())
                .kv("baby", e.baby())
                .kv("breedIndex", e.breedIndex())
                .kv("shortForm", GeneCodeDisplay.shortForm(gt))
                .kv("epiFingerprint", Long.toHexString(e.epigenome().visibleFingerprint(gt)))
                .kv("code", gt.toCode())
                .kv("speed", traits.speed())
                .kv("health", traits.health())
                .kv("jump", traits.jump())
                .kv("scale", traits.scale())
                .key("conditions").arr();
        for (Condition c : traits.conditions()) {
            j.obj().kv("name", c.name())
                    .kv("severity", c.severity().name())
                    .kv("description", c.description())
                    .endObj();
        }
        j.endArr().key("rows").arr();
        for (HorseEditor.Row row : e.rows()) {
            Expression x = gt.expressionOf(row.gene);
            boolean expressing = x != null && !x.wildType();
            j.obj()
                    .kv("added", row.added)
                    .kv("a", row.a)
                    .kv("b", row.b)
                    .kv("expressing", expressing)
                    .kv("expression", expressing ? x.name() : "no effect")
                    .endObj();
        }
        return j.endArr().endObj().toString();
    }

    // ---- edits -------------------------------------------------------------

    @JSExport
    public static void addGene(int row) {
        editor().add(row);
    }

    @JSExport
    public static void removeGene(int row) {
        editor().remove(row);
    }

    @JSExport
    public static void cycleAllele(int row, int slot) {
        editor().cycle(row, slot);
    }

    @JSExport
    public static void setAllele(int row, int slot, int allele) {
        editor().setAllele(row, slot, allele);
    }

    @JSExport
    public static void setSex(boolean female) {
        editor().setSex(female);
    }

    @JSExport
    public static void setBaby(boolean baby) {
        editor().setBaby(baby);
    }

    @JSExport
    public static void setBreed(int index) {
        editor().setBreed(index, RNG);
    }

    @JSExport
    public static void randomize() {
        editor().randomize(RNG);
    }

    @JSExport
    public static void rerollEpigenome() {
        editor().rerollEpigenome(RNG);
    }

    @JSExport
    public static void clearGenes() {
        editor().clearGenes();
    }

    @JSExport
    public static String genotypeCode() {
        return editor().genotype().toCode();
    }

    @JSExport
    public static String epigenomeCode() {
        return editor().epigenome().toCode();
    }

    /** @return false if the code did not parse - the page says so and changes nothing. */
    @JSExport
    public static boolean pasteCode(String code) {
        return editor().paste(code);
    }

    /**
     * The other half of an import. The page parses the JSON (that is a browser
     * format and JavaScript reads it for free) and calls these; everything
     * genetic still happens here.
     */
    @JSExport
    public static boolean setEpigenomeCode(String code) {
        return editor().setEpigenome(code);
    }

    @JSExport
    public static void setName(String first, String last) {
        editor().setName(first, last);
    }

    /** @return false if no breed by that name - the label is dropped, the horse is not. */
    @JSExport
    public static boolean setBreedByName(String name) {
        return editor().stampBreed(name);
    }

    /**
     * The horse as a file: enough to rebuild it exactly, plus enough for a
     * person to tell what it is.
     *
     * <p>The two code strings are the whole of it - a genotype code and an
     * epigenome code reconstruct the horse completely, which is the same
     * guarantee {@code HorseRecord} leans on. Everything else here is
     * derived and is written out for the reader, not for the loader: a
     * <b>loader must re-resolve traits rather than trust these</b>, or a
     * re-tuned gene would be unable to reach a saved horse.
     *
     * <p>Nothing loads this yet - see the roadmap. It exists so that designing a
     * horse here and bringing it into a world is one step away rather than a
     * retyped genotype code.
     */
    @JSExport
    public static String horseJson() {
        HorseEditor e = editor();
        Genotype gt = e.genotype();
        Traits traits = HorseTraits.resolve(gt, e.epigenome(), true);
        Json j = new Json().obj()
                .kv("format", 1)
                .kv("generator", "horse designer (wiki/horse-designer)")
                .key("name").obj().kv("first", e.first()).kv("last", e.last()).endObj()
                .kv("sex", e.female() ? "MARE" : "STALLION")
                .kv("baby", e.baby())
                .kv("breed", e.breedIndex() == 0 ? null : e.breeds().get(e.breedIndex() - 1).name())
                .kv("genotype", gt.toCode())
                .kv("epigenome", e.epigenome().toCode())
                .key("readable").obj()
                .kv("shortForm", GeneCodeDisplay.shortForm(gt))
                .kv("speed", traits.speed())
                .kv("health", traits.health())
                .kv("jump", traits.jump())
                .kv("scale", traits.scale())
                .key("conditions").arr();
        for (Condition c : traits.conditions()) {
            j.val(c.name());
        }
        return j.endArr().endObj().endObj().toString();
    }

    /** A filename for it, from the horse's own name. */
    @JSExport
    public static String horseFileName() {
        String raw = (editor().first() + "-" + editor().last()).toLowerCase();
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            out.append((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') ? c : '-');
        }
        String name = out.toString().replaceAll("-+", "-").replaceAll("^-|-$", "");
        return (name.isEmpty() ? "horse" : name) + ".json";
    }

    // ---- the live parity check ---------------------------------------------

    /**
     * A sample of the texel grid, straight out of {@link HorseSkinGeometry}.
     *
     * <p>The page still builds its 3D mesh from the JavaScript port of the
     * geometry tables (a mesh builder is view code, and that port is checked
     * against the Java by {@code check-parity.mjs}). This exists so the page can
     * confirm that on <i>every load</i> rather than trusting a script someone
     * remembered to run: it walks the same texels through both and complains if
     * they disagree. A stale port is then visible in the tool, not only in a
     * terminal nobody opened.
     */
    @JSExport
    public static String geometryProbeJson(boolean adult) {
        Skin skin = adult ? Skin.ADULT : Skin.BABY;
        int n = HorseSkinGeometry.SHEET_SIZE;
        Json j = new Json().arr();
        for (int py = 1; py < n; py += 7) {
            for (int px = 1; px < n; px += 7) {
                var sample = HorseSkinGeometry.sample(skin, px, py);
                if (sample.isEmpty()) {
                    continue;
                }
                var s = sample.get();
                j.obj().kv("px", px).kv("py", py)
                        .kv("part", s.part().name())
                        .kv("face", s.face().name())
                        .kv("x", s.point().x())
                        .kv("y", s.point().y())
                        .kv("z", s.point().z())
                        .endObj();
            }
        }
        return j.endArr().toString();
    }

    /**
     * Self-test, printed under the panel the way the gene creator prints its
     * parity verdict: bake a handful of known genotypes and report the registry
     * size. If the numbers here are wrong, nothing else on the page is worth
     * looking at.
     */
    @JSExport
    public static String selfTest() {
        int genes = 0;
        int paints = 0;
        for (Gene g : com.example.horsegenetics.common.genetics.Genes.codeOrder()) {
            genes++;
            if (g.affectsCoat()) {
                paints++;
            }
        }
        String assets = ready() ? "assets loaded" : "assets MISSING";
        return genes + " genes, " + paints + " that paint, " + assets;
    }

    /** A trivial Rng - the browser has no reproducibility contract to keep. */
    static final class JavaRng implements Rng {
        private final java.util.Random random = new java.util.Random();

        @Override
        public float nextFloat() {
            return random.nextFloat();
        }

        @Override
        public boolean nextBoolean() {
            return random.nextBoolean();
        }

        @Override
        public int nextInt(int bound) {
            return random.nextInt(bound);
        }

        @Override
        public long nextLong() {
            return random.nextLong();
        }
    }
}
