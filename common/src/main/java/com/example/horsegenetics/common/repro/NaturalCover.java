package com.example.horsegenetics.common.repro;

import java.util.List;

/**
 * <b>Whether a stallion left with a mare covers her now, and which one</b> - the
 * whole decision behind {@code NaturalBreedingHandler}, free of Minecraft so it
 * can be tested. The game module gathers the facts and asks once per scan.
 *
 * <p>Owner, 2026-09-13: every horse breeds on its own, tamed or wild, once per
 * heat when they meet. Both at full health, neither ridden nor leashed, cowboy
 * stock exempt, a gelding never covers, and {@link ReproRules#NATURAL_CAP}
 * other horses near her stops it.
 *
 * <p><b>A stallion's day is not capped</b> (owner, 2026-09-24): he attempts
 * every mare in heat he is left with, and past
 * {@link ReproRules#FREE_COVERS_PER_DAY} his <i>odds</i> halve, exactly as they
 * already did on the carrot and seed-jar paths. It used to be a hard stop here
 * and nowhere else, so the fourth mare in a paddock saw no stallion at all -
 * and {@link Verdict#NO_STALLION} is the one refusal her owner is never told
 * about, which is what made it look like nothing was happening.
 *
 * <p><b>"Full health" is a threshold, not exact equality</b> - see
 * {@link ReproRules#COVER_HEALTH}. Demanding the maximum exactly meant a single
 * point of damage stopped a horse breeding until it found water to heal at, and a
 * miscarriage costs the dam half a heart: one lost pregnancy ended her breeding
 * life, silently. Gap 258.
 */
public final class NaturalCover {

    private NaturalCover() {
    }

    /**
     * How long a stallion must stay within reach of her before he covers her -
     * vanilla's own number. {@code BreedGoal.tick} walks an animal to its partner and
     * breeds only once {@code loveTime >= 60} <i>and</i> the two are closer than three
     * blocks.
     */
    public static final long COURTSHIP_TICKS = 60L;

    /**
     * <b>A courtship in progress</b> - which stallion, since when, last seen when.
     *
     * <p>This is how known gap 230 was settled (owner, 2026-09-14: "figure out how
     * minecraft normally handles that for vanilla, then do the same thing").
     * Vanilla does <b>not</b> check line of sight for breeding: {@code BreedGoal}
     * finds its partner with {@code TargetingConditions...ignoreLineOfSight()}, so
     * two cows three blocks apart through a fence breed. What vanilla does require is
     * the approach, three seconds of it inside three blocks, and that is what a
     * natural cover lacked - it covered the first scan a stallion was in reach. Now
     * the same stallion must still be in reach {@link #COURTSHIP_TICKS} later, and a
     * pair that drifts apart starts again.
     */
    public record Courtship(java.util.UUID stallion, long since, long lastSeen) {

        /** A courtship survives one missed look this long; any longer and it starts again. */
        public static final long MAY_LAPSE = 100L;

        public static Courtship start(java.util.UUID stallion, long now) {
            return new Courtship(stallion, now, now);
        }

        /** He is in reach again at {@code now}: carry on, or start over if it is someone else or too long. */
        public Courtship seen(java.util.UUID who, long now) {
            if (!who.equals(stallion) || now - lastSeen > MAY_LAPSE) {
                return start(who, now);
            }
            return new Courtship(stallion, since, now);
        }

        public boolean complete(long now) {
            return now - since >= COURTSHIP_TICKS;
        }
    }

    /** One horse, as far as a natural cover cares. */
    public record Party(boolean adult, boolean female, boolean gelded, boolean healthyEnough,
                        boolean ridden, boolean leashed, boolean cowboyStock) {

        /** Adult, whole, free, and not a dealer's stock. */
        public boolean ableToBreed() {
            return adult && healthyEnough && !ridden && !leashed && !cowboyStock;
        }

        public boolean entireStallion() {
            return !female && !gelded && ableToBreed();
        }
    }

    /** A stallion near the mare. */
    public record Stallion(Party party, double distanceSq, int coversToday) {
    }

    public enum Verdict {
        /** Cover her, with {@link Decision#stallion()}. */
        COVER,
        /** Not an adult mare able to breed. */
        NOT_A_BREEDING_MARE,
        /** Not in heat, or already covered this heat. */
        NOT_NOW,
        /** No entire, untired stallion within reach. */
        NO_STALLION,
        /** Too many horses around her. */
        CROWDED
    }

    /** @param stallion index into the candidates when {@link Verdict#COVER}, else -1 */
    public record Decision(Verdict verdict, int stallion) {

        static Decision refuse(Verdict verdict) {
            return new Decision(verdict, -1);
        }
    }

    /**
     * @param candidates         the horses near her; anything that is not an able,
     *                           entire, untired stallion within
     *                           {@link ReproRules#NATURAL_REACH} is ignored
     * @param othersNearby       horses of any age within
     *                           {@link ReproRules#NATURAL_CAP_RADIUS}, not counting her
     */
    public static Decision decide(Party mare, Reproduction record, long now, ReproTiming timing,
                                  List<Stallion> candidates, int othersNearby) {
        if (!mare.female() || !mare.ableToBreed()) {
            return Decision.refuse(Verdict.NOT_A_BREEDING_MARE);
        }
        if (!ReproRules.mayTryNaturally(record, now, timing)) {
            return Decision.refuse(Verdict.NOT_NOW);
        }
        double reachSq = ReproRules.NATURAL_REACH * ReproRules.NATURAL_REACH;
        int best = -1;
        for (int i = 0; i < candidates.size(); i++) {
            Stallion s = candidates.get(i);
            // Strictly inside the reach, as vanilla's BreedGoal is (distanceToSqr < 9.0).
            if (!s.party().entireStallion() || s.distanceSq() >= reachSq) {
                continue;
            }
            if (best < 0 || better(s, candidates.get(best))) {
                best = i;
            }
        }
        if (best < 0) {
            return Decision.refuse(Verdict.NO_STALLION);
        }
        if (othersNearby >= ReproRules.NATURAL_CAP) {
            return Decision.refuse(Verdict.CROWDED);
        }
        return new Decision(Verdict.COVER, best);
    }

    /**
     * <b>Which of two stallions in reach she takes:</b> the one whose odds are
     * still whole, and the nearer of the two otherwise.
     *
     * <p>Owner, 2026-09-24: <i>"mares should always prefer stallions who have 3
     * covers or less, and a stallion with 3+ covers should only cover mares if
     * there's no other, more virile, stallion around"</i>. Distance decides only
     * within a group, so a rested stallion four blocks off beats a tired one at
     * her shoulder. It never refuses anybody - a paddock of tired stallions
     * still covers, which is the point of the cap going away.
     *
     * <p><b>The split is where the odds change</b>, {@code >= }
     * {@link ReproRules#FREE_COVERS_PER_DAY}, not above it. A stallion who has
     * made his third cover is already on {@link ReproRules#TIRED_STALLION_FACTOR}
     * for his fourth, so counting him as fresh would have her prefer the halved
     * one - which is the opposite of what the preference is for.
     */
    private static boolean better(Stallion candidate, Stallion best) {
        boolean tired = candidate.coversToday() >= ReproRules.FREE_COVERS_PER_DAY;
        boolean bestTired = best.coversToday() >= ReproRules.FREE_COVERS_PER_DAY;
        if (tired != bestTired) {
            return bestTired;
        }
        return candidate.distanceSq() < best.distanceSq();
    }
}
