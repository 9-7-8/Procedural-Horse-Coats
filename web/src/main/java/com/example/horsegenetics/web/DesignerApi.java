package com.example.horsegenetics.web;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.BreedFounder;
import com.example.horsegenetics.common.breed.BreedSource;
import com.example.horsegenetics.common.breed.Breeds;
import com.example.horsegenetics.common.breed.Commonness;
import com.example.horsegenetics.common.breed.spec.BreedSpecParser;
import com.example.horsegenetics.common.breed.spec.BreedSpecWriter;
import com.example.horsegenetics.common.coat.pattern.CoatTextureComposer;
import com.example.horsegenetics.common.coat.pattern.GradientLut;
import com.example.horsegenetics.common.coat.pattern.LutSet;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.AbilityContribution;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.BaseCoats;
import com.example.horsegenetics.common.genetics.EpigeneticAbilityContribution;
import com.example.horsegenetics.common.genetics.CarrotEffect;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.GenotypeCatalog;
import com.example.horsegenetics.common.genetics.genes.CutieMarkGene;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneCodeDisplay;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.SpliceSafety;
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

    /**
     * The pigment level below which phase 2 leaves a texel transparent instead of
     * sampling. Mirrors {@code CoatTextureComposer.TRANSPARENT_EPS}, which is
     * private; the footprint has to apply the same test or it would report the
     * whole sheet as coverage on a horse that is mostly unpigmented.
     */
    private static final float PIGMENT_EPS = 0.001f;

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
        for (Gene other : Genes.codeOrder()) {
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

    // ---- gene previews -----------------------------------------------------
    //
    // What the wiki's per-gene preview window (wiki/gene-preview/) asks for.
    // Everything here is STATELESS - it takes a genotype and hands back a coat,
    // and never touches the editor above. A wiki page is not editing a horse;
    // it is showing one gene on three known backgrounds, and two callers
    // sharing one mutable editor is exactly how a page ends up displaying the
    // designer's last horse.

    /** Black, bay and chestnut, each as a genotype code. */
    @JSExport
    public static String baseCoatsJson() {
        Json j = new Json().arr();
        for (BaseCoats.BaseCoat b : BaseCoats.all()) {
            j.obj().kv("key", b.key()).kv("name", b.name()).kv("code", b.genotype().toCode()).endObj();
        }
        return j.endArr().toString();
    }

    /**
     * Every genotype at one locus and the phenotype each one gives, for the
     * inheritance table on a gene page's gameplay tab.
     *
     * <p><b>Why this is not {@link #genePreviewJson}.</b> That one answers
     * "what is worth a button" - distinct-looking combinations, wild type
     * dropped, because the base coat already shows it. An inheritance table is
     * the opposite question: a player reading one wants to see that
     * {@code f/f} gives a plain horse just as much as they want the rest, and
     * they want {@code Fl1/Fl1} and {@code Fl2/f} listed separately even though
     * both land on {@code flaxen}, because those are different breeding
     * outcomes. So this reports <b>all</b> pairs, wild type included, without
     * collapsing.
     *
     * <p>The axis is {@link Gene#alleles()} in registry order. Not every cell
     * in the square necessarily exists - a sex-linked locus has no pair for
     * some combinations - so cells are listed rather than assumed, and the
     * page leaves a gap where there is none.
     *
     * <p>Nothing here is a number the wiki could have written down. The point
     * is that a gene which gains an allele gains a row and a column with no
     * page edited.
     */
    @JSExport
    public static String geneInheritanceJson(String geneKey) {
        Gene g = Genes.byKeyOrNull(geneKey);
        if (g == null) {
            return new Json().obj().kv("missing", true).kv("key", geneKey).endObj().toString();
        }
        Json j = new Json().obj()
                .kv("missing", false)
                .kv("key", g.key())
                .kv("name", g.name())
                .kv("natural", g.isNatural())
                .kv("paints", g.affectsCoat())
                .key("alleles").arr();
        for (Allele a : g.alleles()) {
            j.obj().kv("token", a.token())
                    .kv("label", a.label())
                    .kv("isDefault", a.equals(g.defaultAllele()))
                    .endObj();
        }
        j.endArr().key("cells").arr();
        for (AllelePair pair : GenotypeCatalog.allPairsOf(g)) {
            Expression x = g.expressionOf(pair);
            j.obj().kv("tokens", pair.toTokens())
                    .kv("a", pair.first().token())
                    .kv("b", pair.second().token())
                    .kv("name", x.name())
                    .kv("description", x.description())
                    .kv("wild", x.wildType())
                    .kv("varies", !x.deterministic())
                    .endObj();
        }
        return j.endArr().endObj().toString();
    }

    /**
     * What a preview of one gene can offer: its display name, and one entry per
     * combination that <b>looks different</b> -
     * {@link GenotypeCatalog#distinctPairsOf}, minus the wild type, which is
     * what the base coat on its own already shows. So tobiano yields one
     * ({@code To/to} and {@code To/To} paint the same), KIT yields one per
     * white outcome, and a page needs to know none of that.
     *
     * @return {@code {"missing":true}} if no gene is registered under that key -
     *         a wiki page carrying a stale key says so rather than drawing a
     *         plain horse and calling it the gene
     */
    @JSExport
    public static String genePreviewJson(String geneKey) {
        Gene g = Genes.byKeyOrNull(geneKey);
        if (g == null) {
            return new Json().obj().kv("missing", true).kv("key", geneKey).endObj().toString();
        }
        Json j = new Json().obj()
                .kv("missing", false)
                .kv("key", g.key())
                .kv("name", g.name())
                .kv("natural", g.isNatural())
                .kv("paints", g.affectsCoat())
                .key("outcomes").arr();
        // A gene that reads modifier loci says so, and its own expressionOf -
        // which sees one pair and nothing else - is coarse by its own admission:
        // the leopard complex answers "varnish roan" for every LP horse and
        // leaves the real one of eight to expressionIn. Collapsing by expression
        // would then throw away LP/lp, half the table, so such a gene lists its
        // combinations instead and labels them by their tokens.
        boolean coarse = !g.coatDependsOn().isEmpty();
        for (AllelePair pair : coarse ? GenotypeCatalog.allPairsOf(g)
                : GenotypeCatalog.distinctPairsOf(g)) {
            Expression x = g.expressionOf(pair);
            if (coarse ? pair.count(g.defaultAllele()) == 2 : x.wildType()) {
                continue;
            }
            j.obj().kv("tokens", pair.toTokens())
                    .kv("name", coarse ? pair.toTokens() : x.name())
                    .kv("description", x.description())
                    .kv("varies", !x.deterministic())
                    .endObj();
        }
        j.endArr();
        modifiers(j, g);
        return j.endObj().toString();
    }

    /**
     * The other loci this gene <b>reads</b> when it paints, each with every
     * combination a preview could set it to.
     *
     * <p>The leopard complex is the case this exists for. Its own
     * {@link Gene#expressionOf} sees one pair and can only answer
     * {@code varnish-roan} for any {@code LP} horse; the real outcome is one of
     * eight and turns on {@code PATN1} and {@code PATN2}, which
     * {@link Gene#coatDependsOn} names. A preview offering the leopard locus
     * alone would show one corner of that table and call it the gene.
     *
     * <p>Combinations come from {@link GenotypeCatalog#allPairsOf}, <b>not</b>
     * the distinct-by-expression list: a modifier paints nothing on its own, so
     * its whole expression table is wild type and collapsing by expression
     * would leave one entry and hide the thing being modified. The baseline
     * pair is flagged - that is what "off" means.
     *
     * <p>The relationship is <b>declared</b>, so this reads the declaration
     * back; nothing here knows what an appaloosa is.
     */
    private static void modifiers(Json j, Gene g) {
        j.key("modifiers").arr();
        for (String key : g.coatDependsOn()) {
            Gene mod = Genes.byKeyOrNull(key);
            if (mod == null) {
                continue;   // a modifier no longer registered simply drops
            }
            j.obj().kv("key", mod.key()).kv("name", mod.name()).key("options").arr();
            for (AllelePair pair : GenotypeCatalog.allPairsOf(mod)) {
                j.obj().kv("tokens", pair.toTokens())
                        .kv("baseline", pair.count(mod.defaultAllele()) == 2)
                        .endObj();
            }
            j.endArr().endObj();
        }
        j.endArr();
    }

    /**
     * One base coat carrying one combination of one gene.
     *
     * <p>The page hands back the strings this class gave it and gets a genotype
     * code; it never assembles one itself. The code format is a documented
     * string and JavaScript could concatenate it, but then the wiki would hold a
     * second opinion about what a black horse is, and hard rule 3 exists
     * because second opinions drift.
     *
     * @param tokens {@code <a>/<b>} from {@link #genePreviewJson}, or empty for
     *               the bare base coat
     * @return the code, or {@code ""} if anything did not resolve
     */
    @JSExport
    public static String previewGenotypeCode(String baseKey, String geneKey, String tokens) {
        BaseCoats.BaseCoat base = BaseCoats.byKey(baseKey);
        if (base == null) {
            return "";
        }
        Genotype gt = base.genotype();
        Gene g = Genes.byKeyOrNull(geneKey);
        if (g != null && tokens != null && !tokens.isEmpty()) {
            int slash = tokens.indexOf('/');
            if (slash < 0) {
                return "";
            }
            try {
                gt = gt.with(new AllelePair(
                        g.fromToken(tokens.substring(0, slash)),
                        g.fromToken(tokens.substring(slash + 1))));
            } catch (RuntimeException bad) {
                return "";
            }
        }
        return gt.toCode();
    }

    /**
     * One more gene set on an existing genotype code - the composable half of
     * {@link #previewGenotypeCode}, so a page that needs two loci at once (a
     * painter plus a modifier it reads) still never assembles a code itself.
     *
     * @return the new code, or {@code ""} if anything did not resolve
     */
    @JSExport
    public static String withGene(String genotypeCode, String geneKey, String tokens) {
        Gene g = Genes.byKeyOrNull(geneKey);
        if (g == null || tokens == null) {
            return "";
        }
        int slash = tokens.indexOf('/');
        if (slash < 0) {
            return "";
        }
        try {
            return Genotype.parse(genotypeCode)
                    .with(new AllelePair(
                            g.fromToken(tokens.substring(0, slash)),
                            g.fromToken(tokens.substring(slash + 1))))
                    .toCode();
        } catch (RuntimeException bad) {
            return "";
        }
    }

    /**
     * Bake any horse at all - the same pipeline {@link #coat} runs, with the
     * genotype and epigenome given rather than taken from the editor.
     *
     * @return the 128x128 ARGB sheet, or a transparent one if either code failed
     *         to parse
     */
    @JSExport
    public static int[] coatOf(String genotypeCode, String epigenomeCode, boolean adult) {
        int blank = HorseSkinGeometry.SHEET_SIZE * HorseSkinGeometry.SHEET_SIZE;
        if (!ready()) {
            return new int[blank];
        }
        try {
            return CoatTextureComposer.compose(
                    Genotype.parse(genotypeCode), Epigenome.parse(epigenomeCode),
                    adult ? Skin.ADULT : Skin.BABY, adult,
                    adult ? adultTemplate : babyTemplate,
                    new LutSet(baseLut, altLuts));
        } catch (RuntimeException bad) {
            return new int[blank];
        }
    }

    /**
     * <b>Where on the chart this coat reads from.</b> Runs phase 1 for the given
     * horse and reports, in normalised chart coordinates, every position the
     * gradient lookup is about to sample - as a coarse occupancy histogram plus
     * the exact bounds and the texel-weighted centroid.
     *
     * <p>This is the LUT lab's overlay, and it is the reason the lab can claim to
     * show what a dilution <em>does</em> rather than only what it looks like: a
     * cream is a horse whose pigment pair moved, and the pair is the thing to
     * draw. It is computed from {@link CoatTextureComposer#pigmentField}, the
     * same phase-1 loop {@code bake} runs, so it cannot disagree with the horse
     * beside it.
     *
     * <p>Coordinates are {@link GradientLut#chartX}/{@link GradientLut#chartY} -
     * 0,0 is the top-left of the artwork - so a caller draws them directly and
     * never restates the axis convention. Only texels this skin actually maps and
     * that carry some pigment are counted, which is the same test phase 2 applies
     * before it samples; a fully white horse therefore reports no coverage at all
     * rather than a phantom point in the corner.
     *
     * @param bins histogram resolution per axis, clamped to [8, 128]
     */
    @JSExport
    public static String lutFootprintJson(String genotypeCode, String epigenomeCode,
                                          boolean adult, int bins) {
        int b = bins < 8 ? 8 : (bins > 128 ? 128 : bins);
        Json j = new Json().obj();
        try {
            Genotype gt = Genotype.parse(genotypeCode);
            Epigenome epi = Epigenome.parse(epigenomeCode);
            Skin skin = adult ? Skin.ADULT : Skin.BABY;
            PigmentField pigment = CoatTextureComposer.pigmentField(gt, epi, skin, adult);

            int[] counts = new int[b * b];
            // Arrays rather than locals because forEachTexel takes a lambda and a
            // captured local must be effectively final. acc is double: a 128x128
            // sheet is 16k texels and the centroid is the one number here a
            // reader will read to three places.
            double[] acc = new double[2];
            float[] bnd = { 1f, 0f, 1f, 0f };   // minX, maxX, minY, maxY (chart space)
            float[] lvl = { 1f, 0f, 1f, 0f };   // minRed, maxRed, minBlack, maxBlack
            int[] total = new int[1];

            HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
                float r = pigment.red(px, py);
                float bl = pigment.black(px, py);
                if (r <= PIGMENT_EPS && bl <= PIGMENT_EPS) {
                    return;   // phase 2 leaves these transparent and never samples
                }
                float x = GradientLut.chartX(r);
                float y = GradientLut.chartY(bl);
                int bx = (int) (x * (b - 1) + 0.5f);
                int by = (int) (y * (b - 1) + 0.5f);
                counts[by * b + bx]++;
                total[0]++;
                acc[0] += x;
                acc[1] += y;
                if (x < bnd[0]) bnd[0] = x;
                if (x > bnd[1]) bnd[1] = x;
                if (y < bnd[2]) bnd[2] = y;
                if (y > bnd[3]) bnd[3] = y;
                // The same span said in pigment levels, which is what a gene
                // author tunes. Emitted from here so no caller has to invert
                // chartX/chartY and get the direction wrong.
                if (r < lvl[0]) lvl[0] = r;
                if (r > lvl[1]) lvl[1] = r;
                if (bl < lvl[2]) lvl[2] = bl;
                if (bl > lvl[3]) lvl[3] = bl;
            });

            j.kv("texels", total[0]).kv("bins", b);
            if (total[0] == 0) {
                // Every texel unpigmented - a fully white horse. Say so rather
                // than reporting a bounding box round nothing.
                return j.kv("empty", true).key("cells").arr().endArr().endObj().toString();
            }
            j.kv("empty", false)
                    .kv("minX", bnd[0]).kv("maxX", bnd[1])
                    .kv("minY", bnd[2]).kv("maxY", bnd[3])
                    .kv("cx", acc[0] / total[0]).kv("cy", acc[1] / total[0])
                    .kv("redMin", lvl[0]).kv("redMax", lvl[1])
                    .kv("blackMin", lvl[2]).kv("blackMax", lvl[3]);

            int peak = 0;
            for (int i = 0; i < counts.length; i++) {
                if (counts[i] > peak) peak = counts[i];
            }
            j.kv("peak", peak).key("cells").arr();
            for (int i = 0; i < counts.length; i++) {
                if (counts[i] == 0) continue;
                j.obj().kv("x", i % b).kv("y", i / b).kv("n", counts[i]).endObj();
            }
            return j.endArr().endObj().toString();
        } catch (RuntimeException bad) {
            return new Json().obj().kv("texels", 0).kv("empty", true)
                    .key("cells").arr().endArr().endObj().toString();
        }
    }

    /**
     * <b>What pigment resolves at this point on the chart?</b> The probe behind the
     * LUT lab's click-to-inspect: hand it a chart position and it answers with the
     * red and black levels that land there and the colour they resolve to.
     *
     * <p>The inversion is {@link GradientLut#redAtChartX} /
     * {@link GradientLut#blackAtChartY} rather than arithmetic here, so the axis
     * convention stays stated once. A probe that mirrored the chart would return
     * plausible numbers in range and be wrong in the least visible way possible.
     */
    @JSExport
    public static String pigmentAtChartJson(double chartX, double chartY) {
        float red = GradientLut.redAtChartX((float) chartX);
        float black = GradientLut.blackAtChartY((float) chartY);
        return probe(clamp01((float) chartX), clamp01((float) chartY), red, black, -1);
    }

    /**
     * <b>Where on the chart is this colour?</b> The nearest position whose colour
     * matches the one given, with the pigment levels that produce it - so a colour
     * lifted off reference art can be turned back into the pair a gene would have
     * to leave behind to hit it.
     *
     * <p>Searches the <b>current</b> base chart, so in the lab it follows whatever
     * gradient is loaded rather than the one the mod ships. {@code distance} comes
     * back with the answer because the match may be poor: a chart simply may not
     * contain the colour asked for, and a position returned for a distant match is
     * not a meaningful answer to anything.
     *
     * @param hex {@code #RRGGBB}, {@code RRGGBB} or {@code #RGB}
     * @return {@code {"ok":false}} if that is not a colour, or if no chart is loaded
     */
    @JSExport
    public static String nearestOnChartJson(String hex) {
        int rgb = parseHex(hex);
        if (rgb < 0 || baseLut == null) {
            return new Json().obj().kv("ok", false).endObj().toString();
        }
        GradientLut.Nearest n = baseLut.nearest(rgb);
        return probe(n.chartX(), n.chartY(),
                GradientLut.redAtChartX(n.chartX()), GradientLut.blackAtChartY(n.chartY()),
                n.distance());
    }

    /** The one shape both probes answer in, so the page handles them identically. */
    private static String probe(float x, float y, float red, float black, double distance) {
        Json j = new Json().obj()
                .kv("ok", true)
                .kv("x", x).kv("y", y)
                .kv("red", red).kv("black", black)
                // How much of each pigment a gene had to take away to land here.
                .kv("redRestricted", 1.0f - red)
                .kv("blackRestricted", 1.0f - black);
        if (baseLut != null) {
            j.kv("rgb", hex6(baseLut.sample(red, black)));
        }
        if (distance >= 0) {
            j.kv("distance", distance);
        }
        return j.endObj().toString();
    }

    private static String hex6(int argb) {
        String s = Integer.toHexString(argb & 0xFFFFFF);
        StringBuilder b = new StringBuilder("#");
        for (int i = s.length(); i < 6; i++) {
            b.append('0');
        }
        return b.append(s).toString().toUpperCase();
    }

    /** @return the RGB, or -1 if the string is not a colour this accepts */
    private static int parseHex(String hex) {
        if (hex == null) {
            return -1;
        }
        String t = hex.trim();
        if (t.startsWith("#")) {
            t = t.substring(1);
        }
        if (t.length() == 3) {   // #RGB - each digit doubled, as CSS does it
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < 3; i++) {
                b.append(t.charAt(i)).append(t.charAt(i));
            }
            t = b.toString();
        }
        if (t.length() != 6) {
            return -1;
        }
        int v = 0;
        for (int i = 0; i < 6; i++) {
            int d = Character.digit(t.charAt(i), 16);
            if (d < 0) {
                return -1;
            }
            v = (v << 4) | d;
        }
        return v;
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    /**
     * The resolved body of any horse, so a preview can size the model it draws.
     * A coat gene leaves {@code scale} at 1, but the preview window is meant for
     * every gene that shows, and a size locus that did not visibly resize the
     * horse would be a window showing nothing.
     */
    @JSExport
    public static String traitsOfJson(String genotypeCode, String epigenomeCode) {
        try {
            Traits t = HorseTraits.resolve(
                    Genotype.parse(genotypeCode), Epigenome.parse(epigenomeCode), true);
            Json j = new Json().obj()
                    .kv("speed", t.speed()).kv("health", t.health())
                    .kv("jump", t.jump()).kv("scale", t.scale())
                    .key("conditions").arr();
            for (Condition c : t.conditions()) {
                j.obj().kv("name", c.name()).kv("severity", c.severity().name()).endObj();
            }
            return j.endArr().endObj().toString();
        } catch (RuntimeException bad) {
            return new Json().obj().kv("scale", 1.0).key("conditions").arr().endArr().endObj().toString();
        }
    }

    /**
     * A fresh epigenome - the dice in the corner of the preview window. Every
     * copy of every allele gets a new priority and seed, which is what moves a
     * tobiano's patches without touching what the horse is.
     */
    @JSExport
    public static String newEpigenomeCode() {
        return Epigenome.random(RNG).toCode();
    }

    // ---- the gene carrot ---------------------------------------------------

    /**
     * A gene's <b>Known Gene Splice carrot</b>, as the wiki's per-gene recipe
     * card needs it (wiki/gene-carrot/).
     *
     * <p>Every field here is the gene's own answer. The recipe is one
     * parameterised {@code CustomRecipe} rather than N generated ones, so what
     * varies from gene to gene is exactly this: whether a carrot exists at all,
     * which rarity tier pays for it, whether feeding it makes the parent
     * heterozygous or homozygous for that gamete, and whether the <i>Unknown</i>
     * splice may land on the locus. A page that wrote any of those down would be
     * wrong the next time a gene was re-tiered, and wrong silently across fifty
     * pages at once.
     *
     * <p>The tier&rarr;item mapping is deliberately <b>not</b> here: it lives on
     * the recipe side ({@code server/recipe/RarityItems}) so a third-party gene
     * cannot invent its own currency, and {@code common/} must not know what an
     * iron ingot is. The card names the tier and renders the item.
     */
    @JSExport
    public static String geneCarrotJson(String geneKey) {
        Gene g = Genes.byKeyOrNull(geneKey);
        if (g == null) {
            return new Json().obj().kv("missing", true).kv("key", geneKey).endObj().toString();
        }
        boolean hom = g.geneCarrotHomozygous();
        return new Json().obj()
                .kv("missing", false)
                .kv("key", g.key())
                .kv("name", g.name())
                .kv("natural", g.isNatural())
                .kv("hasCarrot", g.hasGeneCarrot())
                .kv("rarity", g.rarity().name())
                // How often a research paper for this gene turns up, relative to
                // the other tiers - the same weight the loot modifier uses.
                .kv("lootWeight", g.rarity().lootWeight())
                .kv("homozygous", hom)
                .kv("effect", new CarrotEffect.KnownGeneSplice(g.key(), hom).id())
                // Whether the Unknown Gene Splice may roll this locus. Derived
                // from what the gene does to the horse, never listed.
                .kv("unknownSpliceable", SpliceSafety.isSafe(g))
                .endObj().toString();
    }

    // ---- breeds, for wiki/breed-designer/ ----------------------------------

    /**
     * Register the breed bundle the page fetched -
     * {@code wiki/horse-designer/assets/breeds.json}, which is
     * {@code common/src/main/resources/horsegenetics/breeds/} rewritten as one
     * array by {@code :common:bakeBreedFiles}.
     *
     * <p>It has to come in rather than be read off the classpath for the same
     * reason the gradient and the name tables do: {@code getResourceAsStream} is
     * the weakest thing TeaVM does. Call it <b>before</b> anything asks for a
     * breed - the registry is lazy and will otherwise conclude there are none.
     *
     * @return a JSON array of anything that would not load, empty when all is well
     */
    /**
     * Register the gene bundle the page fetched -
     * {@code wiki/horse-designer/assets/genes.json}, which is
     * {@code common/src/main/resources/horsegenetics/genes/} rewritten as one
     * array by {@code :common:bakeGeneBundle}.
     *
     * <p>Same reason as {@link #registerBreeds}: {@code getResourceAsStream} is
     * the weakest thing TeaVM does. Call it <b>first</b>, ahead of the breeds
     * and ahead of anything that parses a genotype - a gene arriving late moves
     * every gene after it in the code order, so a code read before this call
     * and a code read after it are two different codes.
     *
     * @return a JSON array of anything that would not load, empty when all is well
     */
    @JSExport
    public static String registerGenes(String bundleJson) {
        Json j = new Json().arr();
        for (String message : Genes.registerBundle(bundleJson, "genes.json")) {
            j.val(message);
        }
        return j.endArr().toString();
    }

    @JSExport
    public static String registerBreeds(String bundleJson) {
        Json j = new Json().arr();
        for (String message : Breeds.registerBundle(bundleJson, "breeds.json")) {
            j.val(message);
        }
        return j.endArr().toString();
    }

    /**
     * The <b>base-coat presets</b> the breed designer's first step offers, each
     * as the gene pools it would write.
     *
     * <p>They are built here, by calling {@code Breed.Builder}'s own
     * {@code extensionAny} / {@code agoutiBayBias} / {@code shadeAny} helpers
     * and reading back what they produced, rather than being a table of weights
     * in the page. The weights in a Friesian's file and the weights the tool
     * offers for "mostly black" are then the same numbers by construction, and
     * re-tuning one re-tunes the other.
     *
     * <p>Every preset includes the <b>shade</b> locus at its wild spread, for
     * the reason {@code Breed.Builder.shadeAny} documents: shade is a modifier a
     * coat gene reads, so a breed that does not name it gets it forced wild -
     * and a world where only feral horses vary in shade would be a bug nobody
     * would think to look for.
     */
    @JSExport
    public static String basePresetsJson() {
        Json j = new Json().arr();
        preset(j, "any", "Any colour",
                "The wild spread at both loci - a breed with no colour rule of its own.",
                Breed.of("x", "x").extensionAny().agoutiAny());
        preset(j, "bay", "Mostly bay",
                "Black-biased extension with a strong bay agouti. Chestnuts are rare and blacks uncommon.",
                Breed.of("x", "x").extensionBlackBias().agoutiBayBias());
        preset(j, "black", "Mostly black",
                "Black-biased extension, agouti fixed recessive. The Friesian shape.",
                Breed.of("x", "x").extensionBlackBias().agoutiBlack());
        preset(j, "black_only", "Black only",
                "Fixed at both loci - every founder is black, and the line cannot throw anything else.",
                Breed.of("x", "x").fixed("horsegenetics.extension", "E").agoutiBlack());
        preset(j, "bay_only", "Bay only",
                "Fixed at both loci - every founder is bay.",
                Breed.of("x", "x").fixed("horsegenetics.extension", "E")
                        .fixed("horsegenetics.agouti", "A").shadeAny());
        preset(j, "chestnut", "Chestnut only",
                "Extension fixed recessive - the Suffolk Punch / Haflinger shape. Agouti still varies but cannot show.",
                Breed.of("x", "x").extensionChestnut().agoutiAny());
        return j.endArr().toString();
    }

    private static void preset(Json j, String key, String name, String blurb, Breed.Builder builder) {
        Breed breed = builder.build();
        j.obj().kv("key", key).kv("name", name).kv("blurb", blurb).key("genes").obj();
        for (Map.Entry<String, List<Breed.Combo>> e : breed.genePools().entrySet()) {
            j.key(e.getKey()).arr();
            for (Breed.Combo c : e.getValue()) {
                j.obj().kv("pair", c.a() + "/" + c.b()).kv("weight", c.weight()).endObj();
            }
            j.endArr();
        }
        j.endObj().endObj();
    }

    /**
     * One registered breed as the <b>file it would be</b> - {@code BreedSpecWriter}
     * output, byte for byte what {@code :common:bakeBreedFiles} writes.
     *
     * <p>This is what "open a built-in breed" in the designer loads, and the
     * reason the tool cannot drift from the mod: the starting point for editing
     * a Friesian is the game's own Friesian file, produced by the game's own
     * writer, not a JavaScript reconstruction of it.
     */
    @JSExport
    public static String breedFileJson(String breedId) {
        Breed breed = Breeds.get(breedId);
        if (breed == Breeds.FERAL_MIXED && !"feral_mixed".equals(breedId)) {
            return "";
        }
        return BreedSpecWriter.write(breed);
    }

    /** Every registered breed, with enough of each to fill a picker. */
    @JSExport
    public static String breedCatalogJson() {
        Json j = new Json().arr();
        for (Breed b : Breeds.all()) {
            j.obj()
                    .kv("id", b.id())
                    .kv("name", b.name())
                    .kv("magical", b.magical())
                    .kv("commonness", Commonness.forWeight(b.spawnWeight())
                            .name().toLowerCase(java.util.Locale.ROOT))
                    .kv("genes", b.genePools().size())
                    .kv("biomes", b.biomes().size())
                    .key("spawn").arr();
            for (BreedSource source : BreedSource.values()) {
                if (b.allows(source)) {
                    j.val(source.id());
                }
            }
            j.endArr().endObj();
        }
        return j.endArr().toString();
    }

    /**
     * Every biome id any registered breed mentions, sorted.
     *
     * <p>The breed designer offers these as suggestions. It is a union of what
     * the breeds already say rather than a list of Minecraft's biomes, and that
     * is the right shape for two reasons: the tool does not have Minecraft's
     * registry to ask, and the biomes worth suggesting are the ones horses
     * already live in. A modded biome typed into one breed file turns up as a
     * suggestion in the next, which a fixed list could never do.
     */
    @JSExport
    public static String knownBiomesJson() {
        java.util.TreeSet<String> ids = new java.util.TreeSet<>();
        for (Breed b : Breeds.all()) {
            ids.addAll(b.biomes());
        }
        Json j = new Json().arr();
        for (String id : ids) {
            j.val(id);
        }
        return j.endArr().toString();
    }

    /**
     * A gene's <b>epigenetic value schema</b> - the named numbers a breed may
     * band, with the range founders are rolled in and the hard clamp beyond it.
     *
     * <p>Only {@code SCALAR}s are listed. A seed is the long behind a noise
     * field and a category is an index into a list the gene owns; neither has a
     * "slightly more", so neither can be banded, and offering a slider for one
     * would be offering a control that does nothing.
     */
    @JSExport
    public static String epiSchemaJson(String geneKey) {
        Gene g = Genes.byKeyOrNull(geneKey);
        Json j = new Json().arr();
        if (g == null) {
            return j.endArr().toString();
        }
        for (EpiValue v : g.epiSchema().values()) {
            if (v.kind() != EpiValue.Kind.SCALAR) {
                continue;
            }
            j.obj()
                    .kv("name", v.name())
                    .kv("min", v.min())
                    .kv("max", v.max())
                    .kv("clampLo", v.clampLo())
                    .kv("clampHi", v.clampHi())
                    .kv("arity", v.arity())
                    .endObj();
        }
        return j.endArr().toString();
    }

    /**
     * Run a candidate breed file through the <b>real parser</b> and report what
     * it said.
     *
     * <p>This is the whole reason the breed designer is a wasm page rather than
     * a form that writes JSON. The tool's validation and the game's are the same
     * code, so a file the tool calls good is a file the game will load, and a
     * warning the tool shows is the warning the log would print. There is no
     * parity to keep because there is no second implementation.
     */
    @JSExport
    public static String checkBreedJson(String json) {
        List<String> warnings = new ArrayList<>();
        try {
            Breed breed = BreedSpecParser.parse(json, "the editor", warnings::add);
            Json j = new Json().obj()
                    .kv("ok", true)
                    .kv("id", breed.id())
                    .kv("name", breed.name())
                    .kv("genes", breed.genePools().size())
                    .key("warnings").arr();
            for (String w : warnings) {
                j.val(w);
            }
            return j.endArr().endObj().toString();
        } catch (RuntimeException e) {
            Json j = new Json().obj()
                    .kv("ok", false)
                    .kv("error", String.valueOf(e.getMessage()))
                    .key("warnings").arr();
            for (String w : warnings) {
                j.val(w);
            }
            return j.endArr().endObj().toString();
        }
    }

    /**
     * Roll one <b>founder</b> of a candidate breed file and hand back its codes,
     * so the page can render it through the same pipeline it renders everything
     * else with.
     *
     * <p>It is a real {@code BreedFounder.roll}: the pool draw, the wild-type
     * forcing, the geometric magic draw, the stat targets and the epigenetic
     * bands. So the horses the designer shows are the horses a herd of this
     * breed would actually be made of, and "my pool looks right but every horse
     * comes out black" is answerable in the tool.
     */
    @JSExport
    public static String breedFounderJson(String json, int seed) {
        Breed breed;
        try {
            breed = BreedSpecParser.parse(json, "the editor", m -> { });
        } catch (RuntimeException e) {
            return new Json().obj().kv("ok", false)
                    .kv("error", String.valueOf(e.getMessage())).endObj().toString();
        }
        Genome genome = BreedFounder.roll(breed, new SeededRng(seed));
        return new Json().obj()
                .kv("ok", true)
                .kv("genotype", genome.genotypeCode())
                .kv("epigenome", genome.epigenome().toCode())
                .endObj().toString();
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
        for (Gene g : Genes.codeOrder()) {
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
