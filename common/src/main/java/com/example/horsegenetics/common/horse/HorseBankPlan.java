package com.example.horsegenetics.common.horse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <b>Which horse goes into which chamber</b> when {@code /horsebank} files a player's
 * horses into their stasis bank in one go (issue #272, owner 2026-10-24).
 *
 * <p>Oldest first - the horse born longest ago - until the chambers run out. The
 * chambers are used in the player's own order (a client config list; cheapest first
 * by default). With {@code makeBasics} (the op's {@code force}), every horse past the
 * player's own empties gets a newly made Basic chamber, so all of them are filed.
 *
 * <p>Pure: the game module counts the empties, reads each horse's birth tick, and
 * carries the plan out.
 */
public final class HorseBankPlan {

    private HorseBankPlan() {
    }

    /** The word for an Emergency chamber in the order list - it is not a {@link StasisTier}. */
    public static final String EMERGENCY = "emergency";

    /**
     * The default order: cheapest first. Emergency chambers are left out - they are
     * what saves a dying horse - but a player may add {@code emergency} to their list.
     */
    public static final List<String> DEFAULT_ORDER = List.of("basic", "intermediate", "advanced", "spacer");

    /** Every word the order list understands. */
    public static final List<String> KNOWN = List.of("basic", "intermediate", "advanced", "spacer", EMERGENCY);

    /** {@code horsebank.allow_non_op}'s default: anyone may file their own horses. */
    public static final boolean DEFAULT_ALLOW_NON_OP = true;
    /** {@code horsebank.search_chunks}'s default: the owner's "one chunk". */
    public static final int DEFAULT_SEARCH_CHUNKS = 1;
    /** The most it may be: past this the bank is not "nearby" in any sense. */
    public static final int MAX_SEARCH_CHUNKS = 8;

    /** {@code horsebank.place_radius}'s default: how far {@code /horsebank new} looks for a spot. */
    public static final int DEFAULT_PLACE_RADIUS = 10;
    /** The most it may be - every block in the sphere is looked at, so it is kept small. */
    public static final int MAX_PLACE_RADIUS = 16;

    /** A horse's birth tick when it is not known; such a horse sorts first, as the oldest. */
    public static final long UNKNOWN_BIRTH = Long.MIN_VALUE;

    /** One horse that may be filed. */
    public record Candidate(UUID horse, long bornTick) {
    }

    /**
     * One filing: this horse, into an empty chamber of this kind from the player's
     * inventory - or, when {@code kind} is null, into a newly made Basic chamber.
     */
    public record Filing(UUID horse, String kind) {
        public boolean madeNew() {
            return kind == null;
        }
    }

    /** The plan, and how many horses it could not file for want of a chamber. */
    public record Plan(List<Filing> filings, int leftOut) {
    }

    /**
     * The player's order with unknown words and repeats removed. An empty or
     * unreadable list means the default, so a player who clears it is not left
     * unable to file anything.
     */
    public static List<String> cleanOrder(List<String> asked) {
        List<String> out = new ArrayList<>();
        if (asked != null) {
            for (String word : asked) {
                String w = word == null ? "" : word.trim().toLowerCase(java.util.Locale.ROOT);
                if (KNOWN.contains(w) && !out.contains(w)) {
                    out.add(w);
                }
            }
        }
        return out.isEmpty() ? new ArrayList<>(DEFAULT_ORDER) : out;
    }

    /** Oldest first; an unknown birth first of all; ties by id, so the order is stable. */
    public static List<Candidate> oldestFirst(List<Candidate> horses) {
        List<Candidate> out = new ArrayList<>(horses);
        out.sort(Comparator.comparingLong(Candidate::bornTick).thenComparing(Candidate::horse));
        return out;
    }

    /**
     * @param horses     the horses to file, in any order
     * @param empties    how many empty chambers of each kind the player holds
     * @param order      the player's order (cleaned here)
     * @param makeBasics make a Basic chamber for each horse the empties do not cover
     */
    public static Plan plan(List<Candidate> horses, Map<String, Integer> empties, List<String> order,
                            boolean makeBasics) {
        Map<String, Integer> left = new LinkedHashMap<>();
        for (String kind : cleanOrder(order)) {
            Integer n = empties.get(kind);
            if (n != null && n > 0) {
                left.put(kind, n);
            }
        }
        List<Filing> filings = new ArrayList<>();
        int leftOut = 0;
        for (Candidate c : oldestFirst(horses)) {
            String kind = null;
            for (Map.Entry<String, Integer> e : left.entrySet()) {
                if (e.getValue() > 0) {
                    kind = e.getKey();
                    e.setValue(e.getValue() - 1);
                    break;
                }
            }
            if (kind != null || makeBasics) {
                filings.add(new Filing(c.horse(), kind));
            } else {
                leftOut++;
            }
        }
        return new Plan(Collections.unmodifiableList(filings), leftOut);
    }

    /**
     * <b>Where to try putting a new bank</b>, best first (#275): every offset within
     * {@code radius} of the player's feet, ordered by how close it is to the spot two
     * blocks in front of them (along {@code lookX, lookZ}, the horizontal look), then
     * by height difference, then by distance from the player. The game module takes
     * the first one where a bank fits; none fitting means the command fails.
     *
     * @return offsets as {@code {dx, dy, dz}}
     */
    public static List<int[]> placeOrder(int radius, double lookX, double lookZ) {
        double len = Math.sqrt(lookX * lookX + lookZ * lookZ);
        double fx = len < 1e-6 ? 0 : 2 * lookX / len;
        double fz = len < 1e-6 ? 0 : 2 * lookZ / len;
        List<int[]> out = new ArrayList<>();
        int r2 = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz <= r2 && !(dx == 0 && dz == 0 && (dy == 0 || dy == 1))) {
                        out.add(new int[] {dx, dy, dz});
                    }
                }
            }
        }
        final double ffx = fx;
        final double ffz = fz;
        out.sort(Comparator.<int[]>comparingDouble(o -> (o[0] - ffx) * (o[0] - ffx) + (o[2] - ffz) * (o[2] - ffz))
                .thenComparingInt(o -> Math.abs(o[1]))
                .thenComparingInt(o -> o[0] * o[0] + o[1] * o[1] + o[2] * o[2])
                .thenComparingInt(o -> o[0]).thenComparingInt(o -> o[1]).thenComparingInt(o -> o[2]));
        return out;
    }

    /** For a chat line: "basic, intermediate, ...". */
    public static String describe(List<String> order) {
        return String.join(", ", cleanOrder(order));
    }
}
