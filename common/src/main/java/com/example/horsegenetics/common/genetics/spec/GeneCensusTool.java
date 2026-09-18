package com.example.horsegenetics.common.genetics.spec;

import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.Breeds;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneFamily;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.GenotypeCatalog;
import com.example.horsegenetics.common.genetics.genes.AgoutiGene;
import com.example.horsegenetics.common.genetics.genes.ExtensionGene;
import com.example.horsegenetics.common.genetics.genes.MagicHealthGene;
import com.example.horsegenetics.common.genetics.genes.MagicJumpGene;
import com.example.horsegenetics.common.genetics.genes.MagicPullGene;
import com.example.horsegenetics.common.genetics.genes.MagicSizeGene;
import com.example.horsegenetics.common.genetics.genes.MagicSpeedGene;
import com.example.horsegenetics.common.genetics.genes.ShadeGene;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * <b>Every outcome every gene can produce, on one page</b>: the table inside
 * {@code wiki/gene-census.html}.
 *
 * <h2>Why it is baked and not written</h2>
 * It is one row per <i>allele presentation</i> - a distinct {@link Expression}
 * of one gene, with every canonical {@link AllelePair} that lands on it - across
 * the whole registry. That is thousands of rows over hundreds of genes, and
 * every word in them is already written down in the gene's own file, in the
 * words the game shows. A hand-written census would be a second copy of the
 * registry that goes stale the first time an outcome is reworded, and nobody
 * would notice: a census is exactly the kind of page a reader trusts without
 * checking. So the registry writes it. Re-run
 * {@code ./gradlew :common:bakeGeneCensus} after any gene changes.
 *
 * <h2>What it leaves out, and why that is not a picker's reasoning</h2>
 * {@code MarkingFactsTool} skips genes that {@link Gene#affectsCoat() paint
 * nothing} and skips {@link Expression#wildType() wild-type} combinations,
 * because it feeds the breed designer's markings filters and "a horse that looks
 * like nothing happened" is not a marking. This census keeps the loci that
 * change no pixel - sex, diet, fireproof, milk, the health genes - because they
 * are the ones a reader is least able to find any other way, having no icon to
 * recognise them by.
 *
 * <p><b>Wild-type rows are culled</b>, though - with two qualifications, one of
 * which is about what {@link Expression#wildType()} actually means here.
 *
 * <p>What goes is the row for <b>"this horse has nothing at this locus"</b>, and
 * finding it takes two tests rather than one, because
 * {@link Expression#wildType()} does not mean what its name suggests. It is also
 * the only constructor that paints in neither phase, so a gene whose effect is
 * not a coat declares its <b>real</b> outcomes with it - {@code MagicSizeGene}'s
 * "Larger" and "Smaller", every diet of {@code DietGene}, mare and stallion.
 * Culling on the flag alone deletes the diet loci, the sex locus and the
 * body-stat genes outright, which are exactly the rows the paragraph above calls
 * hardest to find any other way. So:
 * <ul>
 *   <li>on a gene that <b>paints</b> ({@link Gene#affectsCoat()}), every
 *       wild-type row is the do-nothing row, and all of them go;</li>
 *   <li>on a gene that <b>paints nothing</b>, the row whose outcome the author
 *       <b>never named</b> - {@link #UNNAMED_WILD_ID} - which is the "nothing
 *       here" row however many carrier pairs share it. Carrier pairs share it
 *       often, and a carrier row is dropped on a painting gene already, so
 *       keeping them here would be the same row treated two ways.</li>
 * </ul>
 * <p>The id is what carries that second rule, and it was earned: <b>Mare</b> is
 * the baseline row of a non-painting gene and is plainly an outcome rather than
 * an absence, so a test on the pairs alone deletes it. An {@link Expression#id()}
 * is documented as stable identity; the display name is prose and would drift.
 *
 * <p><b>No gene may end up with no rows at all.</b> The leopard modifiers
 * {@code PATN1} / {@code PATN2} answer with <i>one</i> unnamed expression for
 * every pair they have, so the rule above would take their only row and stop the
 * census mentioning the locus. Each gene's rows are therefore decided before any
 * is written, and a gene that would lose all of them keeps all of them - a dull
 * row beats a missing locus. If that rescue ever fails to save one, the tool
 * refuses to write the page, because vanishing silently was the failure mode of
 * culling on the wild-type flag.
 *
 * <p>{@link #WILD_TYPE_KEPT} is the second qualification: three genes keep their
 * wild-type row even though they do paint, because a horse's base colour
 * genuinely <i>is</i> one of those combinations. {@code n/n} goes the way of the
 * first rule - "carries nothing" is not a presentation - and it is matched as a
 * whole pair, never as text, so {@code Wun/n} and its two dozen relatives are
 * untouched.
 *
 * <h2>The breeds column</h2>
 * Which shipped breeds <b>name</b> a combination that lands on the row, read off
 * {@link Breed#genePools()} unioned with every {@link Breed.Strain}'s pools.
 * The point of it is the <i>blank</i> cells: a presentation no breed produces.
 *
 * <p>Three things it deliberately is not. It is not
 * {@link Breed#founderTable(String)} - that throws on a locus this build has not
 * got, and its fallback borrows "the first strain that names it", which would
 * corrupt per-strain truth. It is not a string comparison of pair text - a breed
 * file stores its pairs as authored ({@code "N/G3"}, {@code "ShL/Sh"}) while a
 * census row prints {@link AllelePair#toTokens()} after canonicalisation to the
 * gene's {@link Gene#alleles()} order, so tokens are resolved to
 * {@link Allele}s and compared as a pair. And it is not "everything a horse of
 * this breed could turn out to be": magical herds bolt a random magical gene
 * onto a few wild herds without ever touching {@code genePools}, and splices,
 * gene carrots and the two editors reach every locus in the mod. Those would all
 * make the column mean "possible somehow", which is nobody's question.
 *
 * <p>Two loci-groups get a marker instead of a breed list, because a blank there
 * would be a lie: the four body-stat genes are set from the breed's
 * {@code stats} block by {@code BreedFounder.bodyStatPair} rather than from a
 * pool, and a {@link Gene#feralOnly()} gene is forced wild on every breed.
 *
 * <h2>It is written to be read by a machine</h2>
 * The stated purpose is that somebody designing a breed or a gene can hand the
 * whole page to an AI and ask it questions. That is why the rows are baked into
 * the HTML rather than fetched or built by script - an assistant reads the
 * source, and a table assembled at run time is invisible to it - and why the
 * page is one flat semantic table with no tab panels to hide half of it. It is
 * also why the narrow-screen collapse of the pair column is <b>visual only</b>:
 * the wildcard summary ({@code Gld/*}) is a button label beside the full list,
 * never a replacement for it. The machine-readable <i>sources</i> are still the
 * gene files themselves,
 * {@code common/src/main/resources/horsegenetics/genes/*.json}.
 *
 * <p>Arg 0 is the wiki directory - the tool needs it for two things: the page it
 * splices, and which gene pages exist on disk, since a gene name is only
 * hyperlinked when there is somewhere for the link to go (the thirteen eye loci
 * have no page of their own).
 */
public final class GeneCensusTool {

    private static final String BEGIN =
            "<!-- BEGIN generated by :common:bakeGeneCensus - do not edit between the markers -->";
    private static final String END = "<!-- END generated -->";

    /**
     * <b>The genes whose wild-type rows survive the cull</b> - owner's call, and
     * meant to be trivially editable: add or drop a key here and re-bake. Every
     * key is checked against the registry on start-up, so a rename cannot leave
     * this silently matching nothing.
     *
     * <p>These three are in because a horse's base colour genuinely is one of
     * their combinations - a chestnut <i>is</i> {@code e/e}, and a breed sheet
     * pins agouti and extension precisely to say which baseline it sits on. On
     * every other <i>painting</i> gene the wild type is the absence of the
     * gene's effect, which a census of outcomes has nothing to say about.
     *
     * <p>Shade is belt-and-braces: it paints nothing on its own (a painter reads
     * it through {@link Gene#coatDependsOn()}), so every one of its outcomes is
     * a wild type and the {@link Gene#affectsCoat()} test would keep them
     * anyway. It is named here because the reason to keep it is the base-colour
     * one, not an accident of how it is wired.
     */
    private static final List<String> WILD_TYPE_KEPT =
            Arrays.asList(AgoutiGene.KEY, ExtensionGene.KEY, ShadeGene.KEY);

    /**
     * The five magical body-stat loci. A breed never names these in a pool -
     * {@code BreedFounder.bodyStatPair} derives the pair from the breed's
     * {@code stats} block - so the breeds column prints
     * {@link #MARKER_STATS} rather than the blank that pool-reading produces.
     * The keys come from the genes themselves rather than from a fourth copy of
     * the string list that {@code BreedFounder} and {@code BreedSpecParser} keep.
     */
    private static final List<String> BODY_STAT_KEYS = Arrays.asList(
            MagicSizeGene.KEY, MagicSpeedGene.KEY, MagicHealthGene.KEY, MagicJumpGene.KEY,
            MagicPullGene.KEY);

    /** What the breeds column says for a locus driven by the breed's stat scores. */
    private static final String MARKER_STATS = "set by stat scores";

    /** What it says for a {@link Gene#feralOnly()} locus, which no breed may carry. */
    private static final String MARKER_FERAL = "feral only";

    /** How many breed names a long cell shows before folding the rest away. */
    private static final int BREEDS_SHOWN = 5;

    /**
     * Below this share of a breed's pool for a locus, the breed is marked
     * {@code (rare)} on the row - a near-blank-spot, visible as such.
     *
     * <p>It is a <b>share</b>, because a {@link Breed.Combo}'s weight is
     * relative within its pool and only {@link Breed#founderTable(String)}
     * rescales those to 100. A pool of two entries weighted {@code 1} and
     * {@code 1} is 50/50, not one per cent.
     */
    private static final double RARE_SHARE = 0.02;

    /**
     * The id {@link Expression#wildType(String)} gives an outcome whose author
     * did not name it - "the horse has nothing here", and nothing more to say.
     *
     * <p>It is what separates the two kinds of baseline row on a gene that
     * paints nothing. {@code magic_swim_speed}'s baseline means <i>swims like
     * any other horse</i> and is as empty as a coat gene's wild type; the sex
     * locus's baseline is <b>Mare</b>, which is an outcome. Both are wild-type
     * flagged and both sit on a row whose every pair is at baseline, so neither
     * the flag nor the pairs can tell them apart - but the author reached for the
     * three-argument factory for one and not the other, and an
     * {@link Expression#id()} is documented as stable identity, unlike the
     * display name, which is prose.
     */
    private static final String UNNAMED_WILD_ID = "wild";

    private GeneCensusTool() {
    }

    public static void main(String[] args) throws IOException {
        Path wiki = Path.of(args.length > 0 ? args[0] : "wiki");
        Path page = Files.isDirectory(wiki) ? wiki.resolve("gene-census.html") : wiki;
        Path dir = page.toAbsolutePath().getParent();

        checkRegistered(WILD_TYPE_KEPT, "WILD_TYPE_KEPT");
        checkRegistered(BODY_STAT_KEYS, "BODY_STAT_KEYS");

        // geneKey -> expression id -> breed name -> what that breed's sheet says
        Map<String, Map<String, Map<String, Credit>>> credits = breedCredits();

        StringBuilder rows = new StringBuilder(1 << 20);
        int presentations = 0;
        int genes = 0;

        // Everything the summary line and the console report say. Counted, never
        // written down: a census that quotes its own size is the one page in the
        // wiki guaranteed to be caught out.
        int rowsBefore = 0;
        int coatWildDropped = 0;
        int baselineDropped = 0;
        int wildKeptBaseColour = 0;
        int wildKeptNamed = 0;
        int nnRemoved = 0;
        int withBreeds = 0;
        int withoutBreeds = 0;
        int statRows = 0;
        int feralRows = 0;
        int uppercaseNnRows = 0;
        int strainQualified = 0;
        int rareFlagged = 0;
        int wildcardRows = 0;
        int countRows = 0;
        int plainRows = 0;
        int maxBreeds = 0;
        String maxBreedsRow = "";
        List<String> nnWholeRow = new ArrayList<>();
        List<String> unjustified = new ArrayList<>();
        List<String> rescued = new ArrayList<>();
        List<String> keptLabelledWild = new ArrayList<>();
        List<String> oddDrops = new ArrayList<>();
        List<String> zeroed = new ArrayList<>();

        for (Gene gene : Genes.codeOrder()) {
            // Group the pairs by outcome, in encounter order. allPairsOf has
            // already dropped the combinations no horse could carry and put the
            // population's baseline first, so the first group is the one a wild
            // horse most likely has.
            Map<String, List<AllelePair>> byExpression = new LinkedHashMap<>();
            Map<String, Expression> expressionById = new LinkedHashMap<>();
            for (AllelePair pair : GenotypeCatalog.allPairsOf(gene)) {
                Expression e = gene.expressionOf(pair);
                if (e == null) {
                    // Not fatal: a gene whose table has a hole is a bug in that
                    // gene, not a reason for the whole census to fail to bake.
                    System.out.println("warning: " + gene.key() + " has no expression for "
                            + pair.toTokens() + " - row skipped");
                    continue;
                }
                byExpression.computeIfAbsent(e.id(), k -> new ArrayList<>()).add(pair);
                expressionById.put(e.id(), e);
            }
            if (byExpression.isEmpty()) {
                continue;
            }

            String name = esc(gene.name());
            String href = pageOf(gene);
            String geneCell = Files.exists(dir.resolve(href))
                    ? "<a href=\"" + href + "\">" + name + "</a>"
                    : name;
            String kind = gene.isNatural() ? "Natural" : "Magical";
            String family = esc(GeneFamily.of(gene).title());
            // affectsCoat() is literally "has an outcome that is not a wild
            // type", which is the question the cull turns on - see the class note.
            boolean paints = gene.affectsCoat();
            boolean keepWild = WILD_TYPE_KEPT.contains(gene.key());
            String marker = gene.feralOnly() ? MARKER_FERAL
                    : (BODY_STAT_KEYS.contains(gene.key()) ? MARKER_STATS : null);
            Map<String, Map<String, Credit>> geneCredits = credits.get(gene.key());
            boolean emittedAny = false;

            // Decided before anything is written, because one row's fate depends
            // on the others: a gene whose EVERY row is an absence row keeps them
            // all, since a census that stops mentioning a locus is worse than one
            // carrying a dull row for it. patn1 and patn2 are the two.
            Map<String, Boolean> drop = new LinkedHashMap<>();
            int survivors = 0;
            for (Map.Entry<String, List<AllelePair>> entry : byExpression.entrySet()) {
                Expression e = expressionById.get(entry.getKey());
                boolean absence = e.wildType()
                        && (paints || UNNAMED_WILD_ID.equals(e.id()));
                boolean out = absence && !keepWild;
                drop.put(entry.getKey(), out);
                if (!out) {
                    survivors++;
                }
            }
            if (survivors == 0) {
                for (Map.Entry<String, Boolean> d : drop.entrySet()) {
                    d.setValue(Boolean.FALSE);
                }
                rescued.add(gene.key());
            }

            for (Map.Entry<String, List<AllelePair>> entry : byExpression.entrySet()) {
                rowsBefore++;
                Expression e = expressionById.get(entry.getKey());
                if (drop.get(entry.getKey())) {
                    if (paints) {
                        coatWildDropped++;
                    } else {
                        baselineDropped++;
                    }
                    // A cross-check on the rule, never the rule itself. On a
                    // non-painting gene a dropped row should be one the game
                    // labels "Wild type"; anything else wants a human's eye.
                    // (Painting genes are not checked: their carrier rows are
                    // named "X carrier" and go for a different, older reason.)
                    if (!paints && !"Wild type".equals(e.name())) {
                        oddDrops.add(gene.key() + " / " + e.id()
                                + " (\"" + e.name() + "\")");
                    }
                    continue;
                }
                if (e.wildType()) {
                    if (keepWild) {
                        wildKeptBaseColour++;
                    } else {
                        wildKeptNamed++;
                        // Kept because its id says the author named it, yet it
                        // still reads as "Wild type" on the page. Only prose
                        // separates these from the rows above, so they are
                        // reported rather than guessed at.
                        if ("Wild type".equals(e.name())) {
                            keptLabelledWild.add(gene.key() + " / " + e.id());
                        }
                    }
                }

                // n/n, matched as a whole pair - both tokens equal "n" - so the
                // chips that merely contain the letter (Wun/n, Leaden/n, Brn/n)
                // are untouched. A substring replace here would eat two dozen of
                // them.
                List<AllelePair> printed = new ArrayList<>();
                int stripped = 0;
                for (AllelePair pair : entry.getValue()) {
                    if (isNn(pair)) {
                        stripped++;
                        continue;
                    }
                    printed.add(pair);
                }
                if (printed.isEmpty()) {
                    // Nothing left to name the row by. Emitting an empty cell is
                    // worse than keeping the n/n, so keep it and say so.
                    printed.addAll(entry.getValue());
                    nnWholeRow.add(gene.key() + " / " + e.id());
                    stripped = 0;
                }
                nnRemoved += stripped;

                Set<String> printedTokens = new LinkedHashSet<>();
                List<String> chips = new ArrayList<>();
                boolean showsUppercaseNn = false;
                for (AllelePair pair : printed) {
                    printedTokens.add(pair.toTokens());
                    chips.add("<code>" + esc(pair.toTokens()) + "</code>");
                    if (pair.first().token().equals("N") && pair.second().token().equals("N")) {
                        showsUppercaseNn = true;
                    }
                }
                if (showsUppercaseNn) {
                    uppercaseNnRows++;
                }

                Map<String, Credit> rowCredits = geneCredits == null
                        ? null : geneCredits.get(e.id());
                List<String> breeds = new ArrayList<>();
                if (rowCredits != null) {
                    for (Map.Entry<String, Credit> c : rowCredits.entrySet()) {
                        Credit credit = c.getValue();
                        breeds.add(credit.label());
                        if (credit.strainQualified()) {
                            strainQualified++;
                        }
                        if (credit.rare()) {
                            rareFlagged++;
                        }
                        // The invariant: a breed named beside a pair list must be
                        // justifiable by a pair actually printed there. It can only
                        // fail if a stripped n/n was the sole reason for the credit.
                        boolean justified = false;
                        for (String t : credit.pairs) {
                            if (printedTokens.contains(t)) {
                                justified = true;
                                break;
                            }
                        }
                        if (!justified) {
                            unjustified.add(gene.key() + " / " + e.id() + ": " + c.getKey()
                                    + " credited only via " + credit.pairs);
                        }
                    }
                }

                if (marker != null) {
                    if (MARKER_STATS.equals(marker)) {
                        statRows++;
                    } else {
                        feralRows++;
                    }
                } else if (breeds.isEmpty()) {
                    withoutBreeds++;
                } else {
                    withBreeds++;
                    if (breeds.size() > maxBreeds) {
                        maxBreeds = breeds.size();
                        maxBreedsRow = gene.key() + " / " + e.id();
                    }
                }

                String summary = collapsed(printed);
                if (summary == null) {
                    plainRows++;
                } else if (summary.endsWith("/*")) {
                    wildcardRows++;
                } else {
                    countRows++;
                }

                rows.append("<tr>\n")
                        .append("<td class=\"cs-pairs\">").append(pairsCell(chips, summary))
                        .append("</td>\n")
                        .append("<td class=\"cs-gene\">").append(geneCell).append("</td>\n")
                        .append("<td class=\"cs-kind\">").append(kind).append("</td>\n")
                        .append("<td class=\"cs-family\">").append(family).append("</td>\n")
                        .append(breedsCell(marker, breeds))
                        .append("<td class=\"cs-pres\">").append(esc(e.name())).append("</td>\n")
                        .append("<td class=\"cs-does\">").append(esc(e.description())).append("</td>\n")
                        .append("</tr>\n");
                presentations++;
                emittedAny = true;
            }
            if (emittedAny) {
                genes++;
            } else {
                zeroed.add(gene.key());
            }
        }

        if (!zeroed.isEmpty()) {
            // The failure mode of culling on the wild-type flag alone: a gene
            // whose every outcome is flagged loses its whole entry, and the
            // census silently stops mentioning a locus that exists.
            for (String key : zeroed) {
                System.out.println("GENE WITH NO ROWS LEFT: " + key);
            }
            throw new IllegalStateException(zeroed.size()
                    + " gene(s) lost every row to the cull - see above; the page was not written");
        }

        if (!unjustified.isEmpty()) {
            // Nothing is written: a breed name sitting beside a pair list that
            // does not contain its pair reads as a bug even when the claim under
            // it is true, so this is a stop-and-report rather than a warning.
            for (String line : unjustified) {
                System.out.println("UNJUSTIFIED CREDIT: " + line);
            }
            throw new IllegalStateException(unjustified.size()
                    + " breed credit(s) rest only on a pair the row no longer prints"
                    + " - see the lines above; the page was not written");
        }

        StringBuilder block = new StringBuilder(rows.length() + 2048);
        block.append(BEGIN).append("\n")
                .append("<p class=\"cs-summary\">").append(presentations)
                .append(" presentations across ").append(genes)
                .append(" genes. <strong>").append(withoutBreeds)
                .append("</strong> of them are carried by no built-in breed at all. ")
                .append(coatWildDropped + baselineDropped)
                .append(" rows for “this locus is doing nothing” were left out - ")
                .append(coatWildDropped).append(" wild types of genes that paint and ")
                .append(baselineDropped)
                .append(" baseline rows of genes that do not - keeping the ")
                .append(wildKeptBaseColour).append(" that are a base colour (")
                .append(keptGeneNames()).append(") and the ").append(wildKeptNamed)
                .append(" outcomes that paint nothing but are named all the same.</p>\n")
                .append("<div class=\"table-wrap\">\n")
                .append("<table id=\"census\">\n")
                .append("<thead>\n<tr>\n")
                .append("<th scope=\"col\" data-sort=\"pairs\">Allele pair(s)</th>\n")
                .append("<th scope=\"col\" data-sort=\"gene\">Gene</th>\n")
                .append("<th scope=\"col\" data-sort=\"kind\">Kind</th>\n")
                .append("<th scope=\"col\" data-sort=\"family\">Category</th>\n")
                .append("<th scope=\"col\" data-sort=\"breeds\">Breeds</th>\n")
                .append("<th scope=\"col\" data-sort=\"pres\">Presentation</th>\n")
                .append("<th scope=\"col\" data-sort=\"does\">What it does</th>\n")
                .append("</tr>\n</thead>\n")
                .append("<tbody>\n").append(rows).append("</tbody>\n")
                .append("</table>\n</div>\n")
                .append(END).append("\n");

        // Normalise on the way in: a checkout on Windows can leave the page
        // CRLF, and then a marker written with a bare newline matches nothing -
        // which looks exactly like "the markers are missing".
        String s = Files.readString(page, StandardCharsets.UTF_8).replace("\r\n", "\n");
        int i = s.indexOf(BEGIN);
        if (i < 0) {
            throw new IllegalStateException("gene-census.html: no BEGIN marker to write between");
        }
        int j = s.indexOf(END, i);
        if (j < 0) {
            throw new IllegalStateException("gene-census.html: BEGIN marker with no END");
        }
        s = s.substring(0, i) + block + s.substring(j + END.length() + 1);
        Files.writeString(page, s, StandardCharsets.UTF_8);

        System.out.println("wrote " + presentations + " presentations across " + genes
                + " genes to " + page.toAbsolutePath());
        System.out.println("rows before the cull: " + rowsBefore
                + "; dropped: " + (coatWildDropped + baselineDropped)
                + " (" + coatWildDropped + " wild types of painting genes, "
                + baselineDropped + " unnamed baselines of non-painting ones)"
                + "; kept as a base colour: " + wildKeptBaseColour
                + " (" + keptGeneNames() + ")"
                + "; kept as named non-painting outcomes: " + wildKeptNamed);
        System.out.println("genes rescued from losing every row (all rows kept): "
                + rescued.size() + " " + rescued);
        System.out.println("rows kept whose id is named but whose label still reads "
                + "\"Wild type\" (prose only separates these - your call): "
                + keptLabelledWild.size() + " " + keptLabelledWild);
        System.out.println("dropped rows the game does not label \"Wild type\" (cross-check, want 0): "
                + oddDrops.size() + " " + oddDrops);
        System.out.println("genes left with no rows at all: " + zeroed.size());
        System.out.println("n/n chips removed: " + nnRemoved
                + "; rows whose n/n was kept to avoid an empty cell: " + nnWholeRow.size()
                + (nnWholeRow.isEmpty() ? "" : " " + nnWholeRow));
        System.out.println("rows still showing an uppercase N/N pair: " + uppercaseNnRows);
        System.out.println("breeds: " + withBreeds + " rows with at least one, "
                + withoutBreeds + " with none, " + statRows + " marked \"" + MARKER_STATS
                + "\", " + feralRows + " marked \"" + MARKER_FERAL + "\"; most on one row: "
                + maxBreeds + " (" + maxBreedsRow + ") of " + Breeds.shipped().size()
                + " shipped breeds");
        System.out.println("breed credits qualified by a strain: " + strainQualified
                + "; flagged (rare), under " + (RARE_SHARE * 100) + "% of that pool: "
                + rareFlagged);
        System.out.println("pair cells: " + wildcardRows + " collapse to a wildcard, "
                + countRows + " to a count, " + plainRows + " are a single pair and never collapse");
        System.out.println("breed credits resting on a pair the row does not print: "
                + unjustified.size());
    }

    // ------------------------------------------------------------------
    // The breeds column
    // ------------------------------------------------------------------

    /**
     * <b>One breed's claim on one row</b> - and the two things that qualify it:
     * which of the breed's sheets named the locus, and how much of that
     * sheet's pool for it actually lands here.
     */
    private static final class Credit {

        private final String breedName;
        /** The pairs that earned the credit - what the printed-pairs invariant checks. */
        private final Set<String> pairs = new LinkedHashSet<>();
        /** Strains whose pool credits this row, in declaration order. */
        private final List<String> strains = new ArrayList<>();
        /** Does the breed's <i>own</i> sheet name this locus at all? */
        private boolean ownNamesGene;
        /**
         * The best share any one crediting pool gives this row. The best, not
         * the sum: "rare" should mean unlikely wherever it can happen, so one
         * pool that rolls it half the time settles the matter.
         */
        private double bestShare;

        Credit(String breedName) {
            this.breedName = breedName;
        }

        /** Strain-qualified: the locus is on a strain's sheet and not the breed's own. */
        boolean strainQualified() {
            return !ownNamesGene && !strains.isEmpty();
        }

        boolean rare() {
            return bestShare < RARE_SHARE;
        }

        /**
         * {@code Dhampir (White)}, {@code Hequ (A, C)}, {@code Friesian (rare)},
         * {@code Dhampir (White, rare)} - one parenthetical, strains first,
         * because both qualifiers narrow the same claim and two brackets in a
         * row read as a footnote.
         */
        String label() {
            List<String> qualifiers = new ArrayList<>();
            if (strainQualified()) {
                qualifiers.addAll(strains);
            }
            if (rare()) {
                qualifiers.add("rare");
            }
            if (qualifiers.isEmpty()) {
                return breedName;
            }
            return breedName + " (" + join(qualifiers, ", ") + ")";
        }
    }

    /**
     * Which shipped breeds name a combination landing on each presentation:
     * {@code gene key -> expression id -> breed name -> }{@link Credit}. Keyed
     * on the plain breed name, so the column sorts by breed rather than by
     * whatever qualifier the label ends up carrying.
     *
     * <p>Each pool is read <b>separately</b> rather than merged, because the
     * qualifiers are per-sheet: a locus only a strain names is that strain's,
     * and a share has to be normalised inside the pool it came from.
     */
    private static Map<String, Map<String, Map<String, Credit>>> breedCredits() {
        Map<String, Map<String, Map<String, Credit>>> out = new LinkedHashMap<>();
        for (Breed breed : Breeds.shipped()) {
            accumulate(out, breed, null, breed.genePools());
            for (Breed.Strain strain : breed.strains()) {
                accumulate(out, breed, strain.name(), strain.genePools());
            }
        }
        return out;
    }

    /**
     * Fold one pool - a breed's own ({@code strainName} null) or one strain's -
     * into the credits.
     */
    private static void accumulate(Map<String, Map<String, Map<String, Credit>>> out,
                                   Breed breed, String strainName,
                                   Map<String, List<Breed.Combo>> pools) {
        for (Map.Entry<String, List<Breed.Combo>> pool : pools.entrySet()) {
            Gene gene = Genes.byKeyOrNull(pool.getKey());
            if (gene == null) {
                continue;   // the breed names a locus this build has not got
            }
            // The denominator is this pool's whole weight for this locus - the
            // weights are relative, and only founderTable rescales them to 100.
            double total = 0.0;
            Map<String, Double> landed = new LinkedHashMap<>();
            Map<String, Set<String>> pairsByOutcome = new LinkedHashMap<>();
            for (Breed.Combo combo : pool.getValue()) {
                Allele a = alleleOf(gene, combo.a());
                Allele b = alleleOf(gene, combo.b());
                if (a == null || b == null) {
                    System.out.println("warning: " + breed.id() + " names "
                            + combo.a() + "/" + combo.b() + " at " + gene.key()
                            + ", which it does not declare - skipped");
                    continue;
                }
                // Built as a pair and resolved through the gene, never compared
                // as text: the file's "ShL/Sh" and the census's "Sh/ShL" are the
                // same combination.
                AllelePair pair = new AllelePair(a, b);
                Expression e = gene.expressionOf(pair);
                if (e == null) {
                    continue;
                }
                double weight = Math.max(0.0, combo.weight());
                total += weight;
                Double seen = landed.get(e.id());
                landed.put(e.id(), seen == null ? weight : seen + weight);
                pairsByOutcome.computeIfAbsent(e.id(), k -> new LinkedHashSet<>())
                        .add(pair.toTokens());
            }
            for (Map.Entry<String, Double> outcome : landed.entrySet()) {
                Credit credit = out
                        .computeIfAbsent(gene.key(), k -> new LinkedHashMap<>())
                        .computeIfAbsent(outcome.getKey(),
                                k -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER))
                        .computeIfAbsent(breed.name(), k -> new Credit(breed.name()));
                credit.ownNamesGene = breed.genePools().containsKey(gene.key());
                if (strainName != null && !credit.strains.contains(strainName)) {
                    credit.strains.add(strainName);
                }
                credit.pairs.addAll(pairsByOutcome.get(outcome.getKey()));
                // Normalised inside this pool, and NOT multiplied by the
                // strain's own weight: the strain qualifier on the label
                // already says "only some founders", and folding it in here
                // would say it twice.
                double share = total > 0.0 ? outcome.getValue() / total : 0.0;
                if (share > credit.bestShare) {
                    credit.bestShare = share;
                }
            }
        }
    }

    /**
     * One token to one of {@code gene}'s alleles, or {@code null}.
     * {@code Breed.alleleOf} is private and {@link Gene#fromToken} throws; a
     * census wants to skip and carry on.
     */
    private static Allele alleleOf(Gene gene, String token) {
        for (Allele a : gene.alleles()) {
            if (a.token().equals(token)) {
                return a;
            }
        }
        return null;
    }

    /** The breeds cell: a marker, a clearly-marked blank, a list, or a list with a folded tail. */
    private static String breedsCell(String marker, List<String> breeds) {
        // data-breeds is what the column sorts on, because 118 must not sort
        // between 11 and 12. A marker is -1: it is not a count, and the
        // "carried by no breed" filter must not catch it either.
        if (marker != null) {
            return "<td class=\"cs-breeds\" data-breeds=\"-1\"><span class=\"cs-marker\">"
                    + esc(marker) + "</span></td>\n";
        }
        if (breeds.isEmpty()) {
            return "<td class=\"cs-breeds\" data-breeds=\"0\">"
                    + "<span class=\"cs-none\">no breed</span></td>\n";
        }
        StringBuilder cell = new StringBuilder();
        cell.append("<td class=\"cs-breeds\" data-breeds=\"").append(breeds.size()).append("\">");
        int shown = Math.min(BREEDS_SHOWN, breeds.size());
        for (int i = 0; i < shown; i++) {
            cell.append(i == 0 ? "" : ", ").append(esc(breeds.get(i)));
        }
        if (breeds.size() > shown) {
            StringBuilder tail = new StringBuilder();
            for (int i = shown; i < breeds.size(); i++) {
                tail.append(", ").append(esc(breeds.get(i)));
            }
            cell.append(disclosure("+" + (breeds.size() - shown) + " more", tail.toString()));
        }
        cell.append("</td>\n");
        return cell.toString();
    }

    /** The pairs cell: the chips, plus the narrow-screen summary when there is more than one. */
    private static String pairsCell(List<String> chips, String summary) {
        String full = join(chips);
        if (summary == null) {
            return full;
        }
        return disclosure(summary, full);
    }

    /**
     * <b>The one disclosure idiom this table has</b> - a button labelled with a
     * summary, and the full text beside it in the source. Which of the two shows
     * is CSS's business (the breeds tail folds at every width, the pair list only
     * on a narrow screen), so the full text is in the HTML either way: this page
     * is meant to be handed to an AI, and content a script has to reveal is
     * content that is not there.
     */
    private static String disclosure(String summary, String full) {
        return "<span class=\"cs-disc\"><button type=\"button\" class=\"cs-disc-btn\""
                + " aria-expanded=\"false\">" + esc(summary) + "</button>"
                + "<span class=\"cs-disc-full\">" + full + "</span></span>";
    }

    /**
     * How a row's pair list reads when there is no room for it - the label on the
     * narrow-screen disclosure.
     *
     * <p>{@code null} for a single pair: {@code a/a} needs no summary. Otherwise
     * the token every pair carries, as {@code Gld/*} - which is the honest
     * shorthand for "one copy or two, with anything else". Where no token is
     * common to every pair there is no such shorthand, and a count is printed
     * instead: the night-watch locus stacks {@code Wbh/n}, {@code Wun/n},
     * {@code Wun/Wbh} and more onto one outcome, and any wildcard for that would
     * be a claim the row does not make.
     */
    private static String collapsed(List<AllelePair> pairs) {
        if (pairs.size() < 2) {
            return null;
        }
        List<String> common = new ArrayList<>();
        AllelePair first = pairs.get(0);
        for (String candidate : new String[]{first.first().token(), first.second().token()}) {
            if (common.contains(candidate)) {
                continue;
            }
            boolean inAll = true;
            for (AllelePair pair : pairs) {
                if (!pair.first().token().equals(candidate)
                        && !pair.second().token().equals(candidate)) {
                    inAll = false;
                    break;
                }
            }
            if (inAll) {
                common.add(candidate);
            }
        }
        // Two common tokens means every pair is the same heterozygote, which a
        // set of distinct pairs cannot be - so this is defensive, and a count is
        // the safe answer if it ever happens.
        return common.size() == 1 ? common.get(0) + "/*" : pairs.size() + " pairs";
    }

    // ------------------------------------------------------------------

    /** Both tokens are literally {@code n} - the pair, not any chip containing the letter. */
    private static boolean isNn(AllelePair pair) {
        return pair.first().token().equals("n") && pair.second().token().equals("n");
    }

    /** A hard-coded key that matches no registered gene is a silent no-op; refuse to bake. */
    private static void checkRegistered(List<String> keys, String what) {
        for (String key : keys) {
            if (Genes.byKeyOrNull(key) == null) {
                throw new IllegalStateException(what + " names \"" + key
                        + "\", which no registered gene answers to");
            }
        }
    }

    /** The kept genes as the registry names them, for the page's own summary line. */
    private static String keptGeneNames() {
        List<String> names = new ArrayList<>();
        for (String key : WILD_TYPE_KEPT) {
            names.add(esc(Genes.byKey(key).name()));
        }
        return join(names, ", ");
    }

    private static String join(List<String> chips) {
        return join(chips, " &middot; ");
    }

    private static String join(List<String> parts, String separator) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            out.append(i == 0 ? "" : separator).append(parts.get(i));
        }
        return out.toString();
    }

    /** The page a gene lives on, as {@code GeneWikiTool} names it. */
    private static String pageOf(Gene gene) {
        String slug = gene.key().substring(gene.key().indexOf('.') + 1);
        return "gene-" + slug.replace('_', '-') + ".html";
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
