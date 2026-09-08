package com.example.horsegenetics.common.horse;

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
 * <p>Nothing here throws on a malformed term. A number that will not parse
 * makes the term match nothing, which is what a half-typed {@code speed>} in a
 * live-filtering box should do - an exception into a render loop is not.
 */
public final class HorseQuery {

    /** The columns a table can be ordered by. */
    public enum Sort {
        NAME("Name"),
        BARN("Barn"),
        SEX("Sex"),
        AGE("Age"),
        BREED("Breed"),
        GENERATION("Gen"),
        COAT("Coat"),
        SPEED("Speed"),
        HEALTH("Health"),
        JUMP("Jump"),
        SIZE("Size"),
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
                "gen", "bond", "speed", "health", "jump", "size",
                "gene", "carries", "expresses", "condition");
    }

    /** Every bare word that is a flag rather than a substring search. */
    public static List<String> flags() {
        return List.of("mare", "stallion", "filly", "colt", "foal", "adult",
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

    /** Split on whitespace, dropping empties; an empty query means "everything". */
    public static List<String> terms(String query) {
        List<String> out = new ArrayList<>();
        if (query == null) {
            return out;
        }
        for (String raw : query.trim().split("\\s+")) {
            if (!raw.isEmpty()) {
                out.add(raw.toLowerCase(Locale.ROOT));
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
        String key = term.substring(0, cut);
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
            case "size":
            case "scale":
                return compare(row.scale(), op, orEqual, value);
            default:
                // Not a key at all - a colon in a horse's name, say. Fall back
                // to the substring search rather than silently matching nothing.
                return contains(row.haystack(), key + value);
        }
    }

    private static boolean flag(HorseListing row, String term, String haystack) {
        switch (term) {
            case "mare":
                return row.sex() == Sex.FEMALE && row.adult();
            case "stallion":
                return row.sex() == Sex.MALE && row.adult();
            case "filly":
                return row.sex() == Sex.FEMALE && !row.adult();
            case "colt":
                return row.sex() == Sex.MALE && !row.adult();
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

    /** Does the horse hold this allele token anywhere - expressing or not? */
    private static boolean carries(HorseListing row, String value) {
        if (value.isEmpty()) {
            return false;
        }
        for (Gene gene : Genes.codeOrder()) {
            AllelePair pair = row.genotype().pair(gene);
            if (pair == null) {
                continue;
            }
            boolean namedGene = equalsIgnoreCase(gene.name(), value)
                    || gene.key().toLowerCase(Locale.ROOT).endsWith("." + value);
            for (Allele allele : new Allele[]{pair.first(), pair.second()}) {
                if (allele == null) {
                    continue;
                }
                if (equalsIgnoreCase(allele.token(), value)) {
                    return true;
                }
                if (namedGene && allele != gene.defaultAllele()) {
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
        for (Gene gene : Genes.codeOrder()) {
            Expression expression = row.genotype().expressionOf(gene);
            if (expression == null || expression.wildType()) {
                continue;
            }
            if (contains(expression.name(), value) || contains(expression.id(), value)
                    || equalsIgnoreCase(gene.name(), value)
                    || gene.key().toLowerCase(Locale.ROOT).endsWith("." + value)) {
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
        return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static boolean equalsIgnoreCase(String a, String b) {
        return a != null && a.toLowerCase(Locale.ROOT).equals(b);
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
            case BARN -> c = Comparator.comparing(HorseListing::barnName, String.CASE_INSENSITIVE_ORDER);
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
