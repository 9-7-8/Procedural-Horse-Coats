package com.example.horsegenetics.common.horse;

import com.example.horsegenetics.common.breed.BreedStatCurve;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * <b>Filtering and sorting a stable</b> - the query half of the browser's
 * <i>My horses</i> tab, over {@link HorseListing} rows.
 *
 * <h2>The filter language</h2>
 * Whitespace-separated <b>terms</b>, all of which must match (AND). A term
 * prefixed with {@code -} is negated. Three shapes:
 *
 * <ul>
 *   <li><b>A flag</b> - a bare word from {@link #flags()}: {@code mare},
 *       {@code stallion}, {@code foal}, {@code adult}, {@code tamed},
 *       {@code wild}, {@code herd}, {@code loaded}, {@code lost},
 *       {@code lethal}, {@code sick}, {@code healthy}, {@code bred},
 *       {@code founder}.</li>
 *   <li><b>A comparison</b> - {@code key:value}, or {@code key>value},
 *       {@code key<value}, {@code key>=value}, {@code key<=value} on a numeric
 *       key. Keys are in {@link #keys()}; a text key matches a substring,
 *       case-insensitively.</li>
 *   <li><b>A bare word</b> - matched as a substring against everything a row
 *       says in words: both names, the barn name, the breed, the coat, the
 *       people, the whereabouts and the disorders.</li>
 * </ul>
 *
 * So {@code mare gen>2 gene:SB1 -lethal} reads as "my mares from generation 3
 * on that carry a sabino allele and are not carrying a lethal", which is the
 * question the roadmap says this tab exists to answer.
 *
 * <h2>Genes are a first-class key, not a search over the code string</h2>
 * {@code gene:} and {@code carries:} match an <b>allele token</b> ({@code SB1}),
 * a gene key or a gene name, by walking the genotype's pairs - so a carrier is
 * found as readily as an expressing horse, which is the whole point of asking.
 * {@code expresses:} is the narrower question: it matches the gene's resolved
 * {@link Expression}, so it finds the horses that actually <i>show</i> it.
 *
 * <p>{@code genotype:} (alias {@code pair:}) is the third question - an
 * <b>exact combination</b>, {@code genotype:E/e}, written the way
 * {@link AllelePair#toTokens()} writes it and matched either way round. The
 * three are a ladder: {@code gene:e} is every horse with a red copy,
 * {@code genotype:E/e} is the carriers only, {@code genotype:e/e} the chestnuts
 * only, and {@code expresses:chestnut} the same chestnuts by their outcome.
 *
 * <h2>Allele tokens are case-sensitive; everything else is not</h2>
 * Extension's alleles are {@code E} and {@code e}, and agouti's {@code A} and
 * {@code a} - at these loci case <i>is</i> the allele. So {@code gene:},
 * {@code carries:} and {@code genotype:} compare tokens exactly, while names,
 * breeds, coats, flags and the keys themselves still fold case. {@code Mare},
 * {@code mare} and {@code MARE} are one flag; {@code gene:E} and {@code gene:e}
 * are two different questions.
 *
 * <p>Nothing here throws on a malformed term. A number that will not parse
 * makes the term match nothing, which is what a half-typed {@code speed>} in a
 * live-filtering box should do - an exception into a render loop is not.
 */
public final class HorseQuery {

    /** The columns a table can be ordered by. */
    public enum Sort {
        NAME("Name"),
        SEX("Sex"),
        AGE("Age"),
        BREED("Breed"),
        GENERATION("Gen"),
        COAT("Coat"),
        SPEED("Speed"),
        HEALTH("Health"),
        JUMP("Jump"),
        SIZE("Hands"),
        BOND("Bond"),
        WHERE("Where");

        private final String label;

        Sort(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private HorseQuery() {
    }

    /** Every {@code key:} a filter term understands, for the tab's help line. */
    public static List<String> keys() {
        return List.of("name", "barn", "breed", "coat", "sex", "age", "by", "where",
                "gen", "bond", "speed", "health", "jump", "hands", "size",
                "gene", "carries", "genotype", "expresses", "condition");
    }

    /** Every bare word that is a flag rather than a substring search. */
    public static List<String> flags() {
        return List.of("mare", "stallion", "filly", "colt", "gelding", "foal", "adult",
                "tamed", "wild", "herd", "loaded", "lost",
                "lethal", "sick", "healthy", "bred", "founder");
    }

    /** Filter, then sort. The two halves are public separately for testing. */
    public static List<HorseListing> apply(List<HorseListing> rows, String query,
                                           Sort sort, boolean descending) {
        List<HorseListing> out = filter(rows, query);
        out.sort(comparator(sort, descending));
        return out;
    }

    public static List<HorseListing> filter(List<HorseListing> rows, String query) {
        List<String> terms = terms(query);
        List<HorseListing> out = new ArrayList<>();
        for (HorseListing row : rows) {
            if (matches(row, terms)) {
                out.add(row);
            }
        }
        return out;
    }

    /**
     * Split on whitespace, dropping empties; an empty query means "everything".
     *
     * <p><b>Case is kept.</b> Everything that reads words - names, breeds,
     * flags, keys - folds case where it compares, but an allele token cannot:
     * {@code E} and {@code e} are two different alleles at extension, and
     * lower-casing here made {@code gene:e} match every black horse alive.
     */
    public static List<String> terms(String query) {
        List<String> out = new ArrayList<>();
        if (query == null) {
            return out;
        }
        for (String raw : query.trim().split("\\s+")) {
            if (!raw.isEmpty()) {
                out.add(raw);
            }
        }
        return out;
    }

    public static boolean matches(HorseListing row, List<String> terms) {
        String haystack = row.haystack();
        for (String term : terms) {
            boolean negate = term.startsWith("-") && term.length() > 1;
            String body = negate ? term.substring(1) : term;
            if (term(row, haystack, body) == negate) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    // One term
    // ------------------------------------------------------------------

    private static boolean term(HorseListing row, String haystack, String term) {
        int cut = splitAt(term);
        if (cut < 0) {
            return flag(row, term, haystack);
        }
        String key = term.substring(0, cut).toLowerCase(Locale.ROOT);
        char op = term.charAt(cut);
        int valueStart = cut + (term.startsWith(">=", cut) || term.startsWith("<=", cut) ? 2 : 1);
        String value = term.substring(valueStart);
        boolean orEqual = valueStart == cut + 2;
        return keyed(row, key, op, orEqual, value);
    }

    /**
     * The first {@code :}, {@code &gt;} or {@code &lt;} that separates a key
     * from a value, or -1 when the term is a bare word. Position 0 does not
     * count - a term that <i>starts</i> with an operator has no key.
     */
    private static int splitAt(String term) {
        for (int i = 1; i < term.length(); i++) {
            char c = term.charAt(i);
            if (c == ':' || c == '>' || c == '<' || c == '=') {
                return i;
            }
        }
        return -1;
    }

    private static boolean keyed(HorseListing row, String key, char op, boolean orEqual, String value) {
        switch (key) {
            case "name":
                return contains(row.displayName(), value);
            case "barn":
                return contains(row.barnName(), value);
            case "breed":
                return contains(row.breed(), value);
            case "coat":
                return contains(row.coat(), value);
            case "sex":
                return contains(row.sexLabel(), value) || contains(row.sex().name(), value);
            case "age":
                return contains(row.ageLabel(), value);
            case "by":
                return contains(row.tamedBy(), value) || contains(row.bredBy(), value);
            case "where":
                return contains(row.where(), value);
            case "condition":
            case "disorder":
                for (String condition : row.conditions()) {
                    if (contains(condition, value)) {
                        return true;
                    }
                }
                return false;
            case "gene":
            case "carries":
                return carries(row, value);
            case "genotype":
            case "pair":
                return genotypeIs(row, value);
            case "expresses":
                return expresses(row, value);
            case "gen":
            case "generation":
                return compare(row.generation(), op, orEqual, value);
            case "bond":
                return row.bond() != HorseListing.BOND_UNKNOWN
                        && compare(row.bond(), op, orEqual, value);
            case "speed":
                return compare(row.speed(), op, orEqual, value);
            case "health":
                return compare(row.health(), op, orEqual, value);
            case "jump":
                return compare(row.jump(), op, orEqual, value);
            case "hands":
            case "height":
                // Decimal hands, the way the breed files write them (16.5, not 16.2 hh).
                return compare(BreedStatCurve.handsFor(row.scale()), op, orEqual, value);
            case "size":
            case "scale":
                return compare(row.scale(), op, orEqual, value);
            default:
                // Not a key at all - a colon in a horse's name, say. Fall back
                // to the substring search rather than silently matching nothing.
                return contains(row.haystack(), key + value);
        }
    }

    private static boolean flag(HorseListing row, String rawTerm, String haystack) {
        String term = rawTerm.toLowerCase(Locale.ROOT);
        switch (term) {
            case "mare":
                return row.sex() == Sex.FEMALE && row.adult();
            case "stallion":
                return row.entire() && row.adult();
            case "filly":
                return row.sex() == Sex.FEMALE && !row.adult();
            case "colt":
                return row.entire() && !row.adult();
            case "gelding":
                return row.gelded() && row.sex() == Sex.MALE;
            case "foal":
                return !row.adult();
            case "adult":
                return row.adult();
            case "tamed":
                return row.tamed();
            case "wild":
                return !row.tamed();
            case "herd":
                return row.inHerd();
            case "loaded":
                return row.loaded();
            case "lost":
                return !row.loaded();
            case "lethal":
                return row.lethal();
            case "sick":
                return !row.conditions().isEmpty();
            case "healthy":
                return row.conditions().isEmpty();
            case "bred":
                return row.hasParents();
            case "founder":
                return !row.hasParents();
            default:
                return haystack.contains(term);
        }
    }

    // ------------------------------------------------------------------
    // Gene terms
    // ------------------------------------------------------------------

    /**
     * Does the horse hold this allele token anywhere - expressing or not?
     *
     * <p>The token compares <b>case-sensitively</b>; the gene name and key
     * around it do not. {@code E} and {@code e} are extension's two alleles,
     * so folding case here answered a different question than the one asked.
     */
    private static boolean carries(HorseListing row, String value) {
        if (value.isEmpty()) {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        for (Gene gene : Genes.codeOrder()) {
            AllelePair pair = row.genotype().pair(gene);
            if (pair == null) {
                continue;
            }
            boolean namedGene = equalsIgnoreCase(gene.name(), value)
                    || gene.key().toLowerCase(Locale.ROOT).endsWith("." + lower);
            for (Allele allele : new Allele[]{pair.first(), pair.second()}) {
                if (allele == null) {
                    continue;
                }
                if (allele.token().equals(value)) {
                    return true;
                }
                if (namedGene && allele != gene.defaultAllele() && !gene.isPlaceholder(allele)) {
                    // The reserved Y in a stallion's X-linked pair is the slot
                    // he does not have, not an allele he is carrying - without
                    // this, gene:brindle matches every stallion alive.
                    return true;
                }
            }
        }
        return false;
    }

    /** Does the horse <i>show</i> something at a gene named, or an outcome named? */
    private static boolean expresses(HorseListing row, String value) {
        if (value.isEmpty()) {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        for (Gene gene : Genes.codeOrder()) {
            Expression expression = row.genotype().expressionOf(gene);
            if (expression == null || expression.wildType()) {
                continue;
            }
            if (contains(expression.name(), value) || contains(expression.id(), value)
                    || equalsIgnoreCase(gene.name(), value)
                    || gene.key().toLowerCase(Locale.ROOT).endsWith("." + lower)) {
                return true;
            }
        }
        return false;
    }

    /**
     * <b>An exact pair</b> - {@code genotype:E/e}, the two tokens either way
     * round, matched against one locus's {@link AllelePair} in the spelling
     * {@link AllelePair#toTokens()} writes.
     *
     * <p>This is the question {@code gene:} cannot ask. {@code gene:e} finds a
     * horse with <i>a</i> red copy, carrier and chestnut alike; {@code
     * genotype:E/e} finds only the carrier, and {@code genotype:e/e} only the
     * chestnut. Tokens are case-sensitive for the same reason they are in
     * {@link #carries}; the separator is the {@code /} of a genotype code.
     *
     * <p>A value that is not exactly two {@code /}-separated non-empty tokens
     * matches nothing rather than throwing - {@code genotype:E/} is a half-typed
     * term in a live-filtering box, not an error.
     */
    private static boolean genotypeIs(HorseListing row, String value) {
        int cut = value.indexOf('/');
        if (cut <= 0 || cut == value.length() - 1) {
            return false;
        }
        String wantFirst = value.substring(0, cut);
        String wantSecond = value.substring(cut + 1);
        if (wantSecond.indexOf('/') >= 0) {
            return false;
        }
        for (Gene gene : Genes.codeOrder()) {
            AllelePair pair = row.genotype().pair(gene);
            if (pair == null || pair.first() == null || pair.second() == null) {
                continue;
            }
            String a = pair.first().token();
            String b = pair.second().token();
            if ((a.equals(wantFirst) && b.equals(wantSecond))
                    || (a.equals(wantSecond) && b.equals(wantFirst))) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Comparison helpers
    // ------------------------------------------------------------------

    private static boolean compare(double actual, char op, boolean orEqual, String value) {
        double wanted;
        try {
            wanted = Double.parseDouble(value);
        } catch (NumberFormatException notANumber) {
            return false;
        }
        switch (op) {
            case '>':
                return orEqual ? actual >= wanted : actual > wanted;
            case '<':
                return orEqual ? actual <= wanted : actual < wanted;
            default:
                // ":" and "=" both mean equality; the numbers a player types are
                // rounded to what the column shows, so this is a near-equality.
                return Math.abs(actual - wanted) < 5e-4;
        }
    }

    private static boolean contains(String haystack, String needle) {
        return haystack != null
                && haystack.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private static boolean equalsIgnoreCase(String a, String b) {
        return a != null && a.equalsIgnoreCase(b);
    }

    // ------------------------------------------------------------------
    // Sorting
    // ------------------------------------------------------------------

    /**
     * The comparator for a column. Every one falls back to the display name, so
     * a re-sort of equal rows is stable and does not reshuffle the table under
     * the cursor.
     */
    public static Comparator<HorseListing> comparator(Sort sort, boolean descending) {
        Comparator<HorseListing> c;
        switch (sort) {
            case SEX -> c = Comparator.comparing(HorseListing::sexLabel, String.CASE_INSENSITIVE_ORDER);
            case AGE -> c = Comparator.comparing(HorseListing::ageLabel, String.CASE_INSENSITIVE_ORDER);
            case BREED -> c = Comparator.comparing(HorseListing::breed, String.CASE_INSENSITIVE_ORDER);
            case GENERATION -> c = Comparator.comparingInt(HorseListing::generation);
            case COAT -> c = Comparator.comparing(HorseListing::coat, String.CASE_INSENSITIVE_ORDER);
            case SPEED -> c = Comparator.comparingDouble(HorseListing::speed);
            case HEALTH -> c = Comparator.comparingDouble(HorseListing::health);
            case JUMP -> c = Comparator.comparingDouble(HorseListing::jump);
            case SIZE -> c = Comparator.comparingDouble(HorseListing::scale);
            case BOND -> c = Comparator.comparingInt(HorseListing::bond);
            case WHERE -> c = Comparator.comparing(HorseListing::where, String.CASE_INSENSITIVE_ORDER);
            default -> c = Comparator.comparing(HorseListing::displayName, String.CASE_INSENSITIVE_ORDER);
        }
        if (descending) {
            c = c.reversed();
        }
        return c.thenComparing(HorseListing::displayName, String.CASE_INSENSITIVE_ORDER);
    }
}
