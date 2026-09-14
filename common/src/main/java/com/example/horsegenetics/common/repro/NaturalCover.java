package com.example.horsegenetics.common.repro;

import java.util.List;

/**
 * <b>Whether a stallion left with a mare covers her now, and which one</b> - the
 * whole decision behind {@code NaturalBreedingHandler}, free of Minecraft so it
 * can be tested. The game module gathers the facts and asks once per scan.
 *
 * <p>Owner, 2026-09-13: every horse breeds on its own, tamed or wild, once per
 * heat when they meet. Both at full health, neither ridden nor leashed, cowboy
 * stock exempt, a gelding never covers, three covers a day is a hard stop, and
 * {@link ReproRules#NATURAL_CAP} other horses near her stops it.
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
    public record Party(boolean adult, boolean female, boolean gelded, boolean fullHealth,
                        boolean ridden, boolean leashed, boolean cowboyStock) {

        /** Adult, whole, free, and not a dealer's stock. */
        public boolean ableToBreed() {
            return adult && fullHealth && !ridden && !leashed && !cowboyStock;
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
            if (!s.party().entireStallion() || s.coversToday() >= ReproRules.FREE_COVERS_PER_DAY
                    || s.distanceSq() >= reachSq) {
                continue;
            }
            if (best < 0 || s.distanceSq() < candidates.get(best).distanceSq()) {
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
}
