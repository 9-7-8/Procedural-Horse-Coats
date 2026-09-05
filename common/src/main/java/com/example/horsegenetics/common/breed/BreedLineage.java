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
 *   <li><b>mixed</b> - three or more lines tangled together ({@code "mixed"});</li>
 *   <li><b>unknown</b> - a horse with no herd identity, i.e. a lone wild spawn,
 *       a {@code /summon}, or a spawn-egg horse ({@code "unknown"}). For the
 *       combination rules Unknown behaves as an ordinary distinct breed.</li>
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
 * </pre>
 */
public record BreedLineage(Kind kind, List<String> components) {

    public enum Kind { PURE, CROSS, MIXED, UNKNOWN }

    public static final BreedLineage MIXED = new BreedLineage(Kind.MIXED, List.of());
    public static final BreedLineage UNKNOWN = new BreedLineage(Kind.UNKNOWN, List.of("unknown"));

    public BreedLineage {
        Objects.requireNonNull(kind, "kind");
        components = List.copyOf(components);
    }

    public static BreedLineage pure(String breedId) {
        if (breedId == null || breedId.isBlank() || breedId.equals("unknown")) {
            return UNKNOWN;
        }
        return new BreedLineage(Kind.PURE, List.of(breedId));
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

    /** The empty / null token, an unknown token, or an unrecognised shape all read as {@link #UNKNOWN}. */
    public static BreedLineage parse(String token) {
        if (token == null || token.isBlank() || token.equals("unknown")) {
            return UNKNOWN;
        }
        if (token.equals("mixed")) {
            return MIXED;
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
            case MIXED -> "mixed";
            case UNKNOWN -> "unknown";
        };
    }

    // --- combination ---------------------------------------------------

    public static BreedLineage combine(BreedLineage a, BreedLineage b) {
        if (a.kind == Kind.MIXED || b.kind == Kind.MIXED) {
            return MIXED;
        }
        Set<String> sa = a.idSet();
        Set<String> sb = b.idSet();

        boolean aPure = sa.size() == 1;
        boolean bPure = sb.size() == 1;

        if (aPure && bPure) {
            return sa.equals(sb) ? a.asPureLike(sa) : cross(one(sa), one(sb));
        }
        if (aPure) { // b is a cross
            return sb.containsAll(sa) ? b.asCrossLike(sb) : MIXED;
        }
        if (bPure) { // a is a cross
            return sa.containsAll(sb) ? a.asCrossLike(sa) : MIXED;
        }
        // both crosses
        return sa.equals(sb) ? a.asCrossLike(sa) : MIXED;
    }

    private Set<String> idSet() {
        return new LinkedHashSet<>(components);
    }

    private BreedLineage asPureLike(Set<String> ids) {
        String id = one(ids);
        return id.equals("unknown") ? UNKNOWN : new BreedLineage(Kind.PURE, List.of(id));
    }

    private BreedLineage asCrossLike(Set<String> ids) {
        return new BreedLineage(Kind.CROSS, new ArrayList<>(new TreeSet<>(ids)));
    }

    private static String one(Set<String> s) {
        return s.iterator().next();
    }

    // --- display -----------------------------------------------------

    /**
     * The magical body-stat targets this label pins. A <b>pure</b> breed uses
     * its own; a <b>cross</b> uses the per-axis average of its two components
     * ({@link com.example.horsegenetics.common.trait.BreedStatTargets#average});
     * <b>mixed</b> and <b>unknown</b> pin nothing, so their body stats fall back
     * to the ordinary bounded-Gaussian draw.
     */
    public com.example.horsegenetics.common.trait.BreedStatTargets statTargets() {
        return switch (kind) {
            case PURE -> Breeds.get(components.get(0)).statTargets();
            case CROSS -> com.example.horsegenetics.common.trait.BreedStatTargets.average(
                    Breeds.get(components.get(0)).statTargets(),
                    Breeds.get(components.get(1)).statTargets());
            case MIXED, UNKNOWN -> com.example.horsegenetics.common.trait.BreedStatTargets.NONE;
        };
    }

    /** The player-facing label, e.g. {@code "Arabian × Friesian cross"}. */
    public String displayName() {
        return switch (kind) {
            case PURE -> Breeds.displayName(components.get(0));
            case CROSS -> components.stream()
                    .map(Breeds::displayName)
                    .sorted(Comparator.naturalOrder())
                    .reduce((x, y) -> x + " × " + y).orElse("Cross") + " cross";
            case MIXED -> "Mixed";
            case UNKNOWN -> "Unknown";
        };
    }
}
