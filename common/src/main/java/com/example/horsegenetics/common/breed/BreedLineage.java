package com.example.horsegenetics.common.breed;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * The <b>breed label</b> carried on a horse's {@link
 * com.example.horsegenetics.common.horse.HorseRecord record} - what the info
 * panel shows and what breeding combines. Four shapes:
 *
 * <ul>
 *   <li><b>pure</b> - one breed id ({@code "friesian"});</li>
 *   <li><b>cross</b> - exactly two breed ids ({@code "cross:arabian+friesian"},
 *       components always sorted so order never matters);</li>
 *   <li><b>spliced</b> - one pure line a gene splice carrot has been let into
 *       ({@code "spliced:friesian"}, displayed <b>Spliced (Friesian)</b>);</li>
 *   <li><b>mixed</b> - three or more lines tangled together ({@code "mixed"});</li>
 *   <li><b>feral</b> - a horse with no herd identity, i.e. a lone wild spawn,
 *       a {@code /summon}, or a spawn-egg horse ({@code "feral_mixed"},
 *       displayed <b>Feral Mixed</b>).</li>
 * </ul>
 *
 * <h2>Combination</h2>
 * {@link #combine} implements the owner's rules exactly:
 * <pre>
 *   pure A  + pure A            -> pure A
 *   pure A  + pure B            -> cross {A,B}
 *   cross S + cross S           -> cross S            (same pair, order-free)
 *   cross S + pure Z, Z in S    -> cross S
 *   cross S + pure Z, Z not in S-> mixed
 *   cross S + cross T (S != T)  -> mixed
 *   mixed   + anything          -> mixed
 *   feral   + anything          -> mixed              (feral included)
 * </pre>
 *
 * <h2>Spliced is a cross with a splice on one side</h2>
 * A gene splice carrot that actually lands its allele on the foal makes that
 * foal <b>Spliced (its breed)</b>. It is not a new rule: {@link #SPLICE} is a
 * reserved component, and a spliced Friesian is the pair
 * {@code {friesian, splice}} - so every line of the table above applies to it
 * unchanged, and {@link #combine} gained no branch. What falls out is
 * <pre>
 *   Spliced(A) + A              -> Spliced(A)     (cross S + pure Z, Z in S)
 *   Spliced(A) + Spliced(A)     -> Spliced(A)     (cross S + cross S)
 *   Spliced(A) + B              -> mixed          (cross S + pure Z, Z not in S)
 *   Spliced(A) + Spliced(B)     -> mixed          (cross S + cross T)
 *   Spliced(A) + cross {A,B}    -> mixed          (cross S + cross T)
 * </pre>
 * A spliced line stays spliced for good and there is no breeding it back out,
 * which is the whole point: it is what makes an ordinary purebred worth more
 * than a spectacular spliced one. Anybody can splice a Friesian into something
 * remarkable; only a breeder can get there without.
 *
 * <p>Splicing a <b>cross</b> gives {@link #MIXED}, and that is the same
 * consequence rather than a special case: {@code {A, B, splice}} is three lines
 * tangled together, which is what Mixed has always meant. Splicing
 * {@link #MIXED} or {@link #FERAL} is a no-op, for the reason those two absorb
 * everything else - neither names an ancestry, so "Spliced (nothing in
 * particular)" would say less than "Mixed" already does.
 *
 * <h2>Why feral is absorbing</h2>
 * A wild loner used to be labelled "Unknown" and combined as though it were an
 * ordinary distinct breed, so a Friesian bred to one produced a
 * "Friesian &times; Unknown cross". That forced the model to answer a question
 * it has no good answer to: is Unknown a breed? Does it have a stat band, a
 * gene pool, a purity? Can a line be bred back to pure Unknown? Making it
 * <b>absorbing</b> - every cross involving it is simply {@link #MIXED} -
 * deletes the question instead of answering it, and says the true thing: a horse
 * of unrecorded ancestry contributes unrecorded ancestry, and what comes out is
 * a horse of mixed breeding.
 */
public record BreedLineage(Kind kind, List<String> components) {

    public enum Kind { PURE, CROSS, SPLICED, MIXED, FERAL }

    /** What a spliced token starts with. */
    private static final String SPLICED_PREFIX = "spliced:";

    /**
     * The reserved component id a splice contributes. It is not a breed and no
     * breed file may claim it - {@code BreedSpecParser} refuses the id - which
     * is what lets {@link #combine} treat a spliced line as an ordinary
     * two-component cross and gain no branch for it.
     */
    public static final String SPLICE = "splice";

    /** The serialised id of the {@link #FERAL} label. */
    public static final String FERAL_ID = "feral_mixed";

    public static final BreedLineage MIXED = new BreedLineage(Kind.MIXED, List.of());
    public static final BreedLineage FERAL = new BreedLineage(Kind.FERAL, List.of(FERAL_ID));

    public BreedLineage {
        Objects.requireNonNull(kind, "kind");
        components = List.copyOf(components);
    }

    public static BreedLineage pure(String breedId) {
        if (breedId == null || breedId.isBlank() || breedId.equals(FERAL_ID)) {
            return FERAL;
        }
        return new BreedLineage(Kind.PURE, List.of(breedId));
    }

    /**
     * This line with a gene splice let into it.
     *
     * <p>Idempotent; a no-op on the two shapes that name no ancestry; and
     * {@link #MIXED} on a cross, because a cross plus a splice is three lines
     * and three lines have always been Mixed.
     */
    public BreedLineage spliced() {
        return switch (kind) {
            case PURE -> new BreedLineage(Kind.SPLICED, components);
            case CROSS -> MIXED;
            case SPLICED, MIXED, FERAL -> this;
        };
    }

    public boolean isSpliced() {
        return kind == Kind.SPLICED;
    }

    public static BreedLineage cross(String a, String b) {
        Set<String> s = new TreeSet<>();
        s.add(a);
        s.add(b);
        if (s.size() == 1) {
            return pure(s.iterator().next());
        }
        return new BreedLineage(Kind.CROSS, new ArrayList<>(s));
    }

    // --- parse / serialise ------------------------------------------------

    /** The empty / null token, a feral token, or an unrecognised shape all read as {@link #FERAL}. */
    public static BreedLineage parse(String token) {
        if (token == null || token.isBlank() || token.equals(FERAL_ID)) {
            return FERAL;
        }
        if (token.equals("mixed")) {
            return MIXED;
        }
        if (token.startsWith(SPLICED_PREFIX)) {
            return parse(token.substring(SPLICED_PREFIX.length())).spliced();
        }
        if (token.startsWith("cross:")) {
            String[] parts = token.substring("cross:".length()).split("\\+");
            if (parts.length == 2) {
                return cross(parts[0], parts[1]);
            }
            return MIXED;
        }
        return new BreedLineage(Kind.PURE, List.of(token));
    }

    public String toToken() {
        return switch (kind) {
            case PURE -> components.get(0);
            case CROSS -> "cross:" + String.join("+", components); // already sorted
            case SPLICED -> SPLICED_PREFIX + baseToken();
            case MIXED -> "mixed";
            case FERAL -> FERAL_ID;
        };
    }

    /** The token this would carry if it had never been spliced. */
    private String baseToken() {
        return components.get(0);
    }

    // --- combination ---------------------------------------------------

    public static BreedLineage combine(BreedLineage a, BreedLineage b) {
        // Mixed and Feral are both absorbing, and for the same reason: neither
        // names an ancestry a foal could inherit half of.
        if (a.kind == Kind.MIXED || b.kind == Kind.MIXED
                || a.kind == Kind.FERAL || b.kind == Kind.FERAL) {
            return MIXED;
        }
        Set<String> sa = a.idSet();
        Set<String> sb = b.idSet();

        boolean aPure = sa.size() == 1;
        boolean bPure = sb.size() == 1;

        if (aPure && bPure) {
            return sa.equals(sb) ? a.asPureLike(sa) : cross(one(sa), one(sb));
        }
        if (aPure) { // b names two lines
            return sb.containsAll(sa) ? b.asCrossLike(sb) : MIXED;
        }
        if (bPure) { // a names two lines
            return sa.containsAll(sb) ? a.asCrossLike(sa) : MIXED;
        }
        // both name two lines
        return sa.equals(sb) ? a.asCrossLike(sa) : MIXED;
    }

    /**
     * The lines this label names, with {@link #SPLICE} standing in for a splice.
     * A spliced Friesian is {@code {friesian, splice}} - two components - which
     * is exactly why {@link #combine} needs no case for it.
     */
    private Set<String> idSet() {
        Set<String> ids = new LinkedHashSet<>(components);
        if (kind == Kind.SPLICED) {
            ids.add(SPLICE);
        }
        return ids;
    }

    private BreedLineage asPureLike(Set<String> ids) {
        String id = one(ids);
        return id.equals(FERAL_ID) ? FERAL : new BreedLineage(Kind.PURE, List.of(id));
    }

    /** A two-component set back as a label - spliced when the splice is one of the two. */
    private BreedLineage asCrossLike(Set<String> ids) {
        if (ids.contains(SPLICE)) {
            Set<String> real = new TreeSet<>(ids);
            real.remove(SPLICE);
            return new BreedLineage(Kind.SPLICED, new ArrayList<>(real));
        }
        return new BreedLineage(Kind.CROSS, new ArrayList<>(new TreeSet<>(ids)));
    }

    private static String one(Set<String> s) {
        return s.iterator().next();
    }

    // --- display -----------------------------------------------------

    private String crossName() {
        return components.stream()
                .map(Breeds::displayName)
                .sorted(Comparator.naturalOrder())
                .reduce((x, y) -> x + " × " + y).orElse("Cross") + " cross";
    }

    /** The player-facing label, e.g. {@code "Arabian × Friesian cross"}. */
    public String displayName() {
        return switch (kind) {
            case PURE -> Breeds.displayName(components.get(0));
            case CROSS -> crossName();
            case SPLICED -> "Spliced (" + Breeds.displayName(components.get(0)) + ")";
            case MIXED -> "Mixed";
            case FERAL -> "Feral Mixed";
        };
    }
}
