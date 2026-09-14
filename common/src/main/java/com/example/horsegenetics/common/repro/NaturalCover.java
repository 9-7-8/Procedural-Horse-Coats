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
            if (!s.party().entireStallion() || s.coversToday() >= ReproRules.FREE_COVERS_PER_DAY
                    || s.distanceSq() > reachSq) {
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
