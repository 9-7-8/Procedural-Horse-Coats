package com.example.horsegenetics.common.herd;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * <b>Every horse one horse knows</b>, and what it makes of each - an immutable,
 * capped list of {@link Relationship}s.
 *
 * <p>Pure bookkeeping. It decides nothing about behaviour: it is told that two
 * horses were together, or that one yielded to the other, and it answers who a
 * horse's companions are and whether it has a rival. The rates live in
 * {@link HerdRules} so the numbers are in one place and tested.
 *
 * <h2>Capped, and what goes first</h2>
 * A horse in a busy paddock meets dozens of others, and storing all of them
 * forever is a save-file leak. The ledger keeps at most {@link #MAX} and, when
 * full, drops the relationship with the lowest {@link Relationship#importance()} -
 * the passing acquaintance, never the grooming partner or the rival.
 */
public final class SocialLedger {

    /** Most relationships one horse remembers. */
    public static final int MAX = 12;

    /** Below every one of these a relationship is forgotten outright. */
    static final double FORGET_BELOW = 0.02;

    public static final SocialLedger EMPTY = new SocialLedger(List.of());

    private final List<Relationship> entries;

    private SocialLedger(List<Relationship> entries) {
        this.entries = List.copyOf(entries);
    }

    public static SocialLedger of(List<Relationship> entries) {
        return entries == null || entries.isEmpty() ? EMPTY : new SocialLedger(capped(new ArrayList<>(entries)));
    }

    public List<Relationship> entries() {
        return entries;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public Optional<Relationship> with(UUID other) {
        for (Relationship r : entries) {
            if (r.other().equals(other)) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    /** How far {@code other} yields to this horse, -1..1; 0 for a horse it does not know. */
    public double rankOver(UUID other) {
        return with(other).map(Relationship::rank).orElse(0.0);
    }

    /**
     * The two were near each other for {@code scanTicks}. Familiarity grows
     * toward 1; grooming grows only by {@code groomWeight} (0 for a pair that are
     * not grooming partners) and rivalry by {@code rivalWeight} (0 unless both
     * are males who have reason to be rivals).
     */
    public SocialLedger together(UUID other, long now, long scanTicks, double groomWeight, double rivalWeight) {
        Relationship r = with(other).orElseGet(() -> Relationship.stranger(other, now));
        double days = scanTicks / (double) HerdRules.DAY_TICKS;
        r = r.withFamiliarity(HerdRules.approach(r.familiarity(), HerdRules.FAMILIARITY_PER_DAY, days))
                .withGrooming(groomWeight > 0.0
                        ? HerdRules.approach(r.grooming(), HerdRules.GROOMING_PER_DAY * groomWeight, days)
                        : r.grooming())
                .withRivalry(rivalWeight > 0.0
                        ? HerdRules.approach(r.rivalry(), HerdRules.RIVALRY_PER_DAY * rivalWeight, days)
                        : r.rivalry())
                .seen(now);
        return put(r);
    }

    /**
     * Something was settled between the two: {@code won} is whether <i>this</i>
     * horse came out on top, {@code stakes} how much it moves the rank - a
     * displacement a little, a spar more, a real fight most of all. Rank moves
     * by the remaining distance to the end it is heading for, so an established
     * order takes several reversals to overturn rather than one lucky bout.
     *
     * <p>The other horse's ledger must be told the mirror image; see
     * {@link HerdRules#settle}.
     */
    public SocialLedger contest(UUID other, boolean won, double stakes, long now) {
        Relationship r = with(other).orElseGet(() -> Relationship.stranger(other, now));
        double target = won ? 1.0 : -1.0;
        double rank = r.rank() + (target - r.rank()) * clampStakes(stakes);
        double rivalry = won ? r.rivalry() : Math.min(1.0, r.rivalry() + stakes * 0.5);
        return put(r.withRank(rank).withRivalry(rivalry).seen(now));
    }

    /**
     * Time passing. Each relationship fades by how long it has been since the two
     * were last together - familiarity fastest, rivalry next, grooming slowest,
     * and rank not at all (an old order stays remembered until it is re-tested).
     * A relationship that has faded to nothing is dropped.
     *
     * @param elapsedTicks ticks since the ledger last decayed, so decay does not
     *                     compound faster when a horse is scanned more often
     */
    public SocialLedger decayed(long now, long elapsedTicks) {
        if (entries.isEmpty() || elapsedTicks <= 0) {
            return this;
        }
        double days = elapsedTicks / (double) HerdRules.DAY_TICKS;
        List<Relationship> out = new ArrayList<>(entries.size());
        for (Relationship r : entries) {
            if (now - r.lastTogether() <= elapsedTicks) {
                out.add(r);     // together this interval - nothing faded
                continue;
            }
            Relationship faded = r
                    .withFamiliarity(r.familiarity() * Math.exp(-HerdRules.FAMILIARITY_DECAY_PER_DAY * days))
                    .withGrooming(r.grooming() * Math.exp(-HerdRules.GROOMING_DECAY_PER_DAY * days))
                    .withRivalry(r.rivalry() * Math.exp(-HerdRules.RIVALRY_DECAY_PER_DAY * days));
            if (faded.familiarity() >= FORGET_BELOW || faded.grooming() >= FORGET_BELOW
                    || faded.rivalry() >= FORGET_BELOW) {
                out.add(faded);
            }
        }
        return new SocialLedger(out);
    }

    /**
     * This horse's closest companions, strongest first: grooming counts double,
     * and a horse it barely knows is not a companion however long the list is.
     */
    public List<Relationship> companions(int max) {
        List<Relationship> out = new ArrayList<>();
        for (Relationship r : entries) {
            if (r.familiarity() >= HerdRules.COMPANION_FAMILIARITY || r.grooming() >= HerdRules.GROOMING_PARTNER) {
                out.add(r);
            }
        }
        out.sort(Comparator.comparingDouble((Relationship r) -> r.grooming() * 2 + r.familiarity()).reversed());
        return out.size() > max ? out.subList(0, max) : out;
    }

    /** The horse it is most at odds with, if the rivalry is strong enough to name. */
    public Optional<Relationship> rival() {
        Relationship best = null;
        for (Relationship r : entries) {
            if (r.rivalry() >= HerdRules.RIVAL_THRESHOLD && (best == null || r.rivalry() > best.rivalry())) {
                best = r;
            }
        }
        return Optional.ofNullable(best);
    }

    /** The strongest grooming partner, if there is one. */
    public Optional<Relationship> groomingPartner() {
        Relationship best = null;
        for (Relationship r : entries) {
            if (r.grooming() >= HerdRules.GROOMING_PARTNER && (best == null || r.grooming() > best.grooming())) {
                best = r;
            }
        }
        return Optional.ofNullable(best);
    }

    private SocialLedger put(Relationship r) {
        List<Relationship> out = new ArrayList<>(entries.size() + 1);
        boolean replaced = false;
        for (Relationship e : entries) {
            if (e.other().equals(r.other())) {
                out.add(r);
                replaced = true;
            } else {
                out.add(e);
            }
        }
        if (!replaced) {
            out.add(r);
        }
        return new SocialLedger(capped(out));
    }

    private static List<Relationship> capped(List<Relationship> list) {
        while (list.size() > MAX) {
            Relationship weakest = list.get(0);
            for (Relationship r : list) {
                if (r.importance() < weakest.importance()) {
                    weakest = r;
                }
            }
            list.remove(weakest);
        }
        return list;
    }

    private static double clampStakes(double s) {
        return s < 0.0 ? 0.0 : Math.min(1.0, s);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SocialLedger l && l.entries.equals(entries);
    }

    @Override
    public int hashCode() {
        return entries.hashCode();
    }
}
