package com.example.horsegenetics.common.repro;

import com.example.horsegenetics.common.Rng;

/**
 * <b>The numbers behind heat, conception, pregnancy and nursing</b>, free of
 * Minecraft. The game module gathers the facts - the time, the record, who is
 * near whom - and asks.
 *
 * <p>Every value here was settled with the owner on 2026-09-13; see
 * {@code wiki/roadmap.html#settled} and {@code wiki/fertility.html}.
 */
public final class ReproRules {

    private ReproRules() {
    }

    // ------------------------------------------------------------------
    // Conception odds
    // ------------------------------------------------------------------

    /** Second half of a heat - around ovulation. */
    public static final double PEAK_CHANCE = 0.80;
    /** First half of a heat. */
    public static final double EARLY_CHANCE = 0.50;
    /** Foal heat, the first heat after birth. */
    public static final double FOAL_HEAT_CHANCE = 0.60;
    /** However fertile the pair, a breeding is never a certainty. */
    public static final double MAX_CHANCE = 0.95;

    /** Covers (or jar fills) a stallion makes in one day before his odds drop. */
    public static final int FREE_COVERS_PER_DAY = 3;
    /**
     * What his odds are multiplied by once he is past them - on the carrot and
     * jar paths. A natural cover is a hard stop at the same number instead.
     */
    public static final double TIRED_STALLION_FACTOR = 0.5;

    // ------------------------------------------------------------------
    // Natural covers - a stallion left with mares
    // ------------------------------------------------------------------

    /** How close a stallion must be to a mare in heat to cover her. */
    public static final double NATURAL_REACH = 3.0;
    /** With this many other horses, foals included, within {@link #NATURAL_CAP_RADIUS}, nobody covers. */
    public static final int NATURAL_CAP = 8;
    public static final double NATURAL_CAP_RADIUS = 16.0;

    // ------------------------------------------------------------------
    // Pregnancy
    // ------------------------------------------------------------------

    /** Twins at birth: both live this often... */
    public static final double TWINS_BOTH_LIVE = 0.75;
    /** ...one is lost this often, and both the rest of the time. */
    public static final double TWINS_ONE_LOST = 0.20;

    /** An early loss happens between these fractions of the way through. */
    public static final double EARLY_LOSS_FROM = 0.10;
    public static final double EARLY_LOSS_TO = 1.0 / 3.0;

    /** From this far through, she is slower. */
    public static final double LATE_FRACTION = 2.0 / 3.0;
    /** How much slower - a multiply-total movement-speed modifier. */
    public static final double LATE_SPEED_PENALTY = 0.15;

    // ------------------------------------------------------------------
    // Nursing
    // ------------------------------------------------------------------

    /** A foal further than this from its dam counts as apart. */
    public static final double NURSING_RADIUS = 32.0;

    // ------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------

    /** Where a mare is, now. See {@link ReproState} for the priority order. */
    public static ReproState stateAt(Reproduction r, long now, ReproTiming t) {
        if (r.pregnant()) {
            return ReproState.PREGNANT;
        }
        if (r.foaledTick() != Reproduction.NEVER) {
            long since = now - r.foaledTick();
            if (since >= 0 && since < t.postpartumTicks()) {
                return ReproState.POSTPARTUM;
            }
            if (since >= 0 && since < t.postpartumTicks() + t.foalHeatTicks()) {
                return ReproState.FOAL_HEAT;
            }
        }
        return cyclePosition(r, now, t) < t.estrusTicks() ? ReproState.ESTRUS : ReproState.DIESTRUS;
    }

    /** Ticks into the current cycle, {@code 0 .. cycleTicks-1}. Estrus is the start of it. */
    static long cyclePosition(Reproduction r, long now, ReproTiming t) {
        long cycle = t.cycleTicks();
        long offset = Math.round(r.cyclePhase() * cycle);
        return Math.floorMod(now + offset, cycle);
    }

    /**
     * The cycle phase that puts a mare at {@code position} ticks into her cycle at
     * {@code now}. Only the testing clock wants this; the inverse of
     * {@link #cyclePosition}.
     */
    public static double phaseFor(long now, long position, ReproTiming t) {
        long cycle = t.cycleTicks();
        return Math.floorMod(position - now, cycle) / (double) cycle;
    }

    /** When the heat she is in now began, or {@link Reproduction#NEVER} when she is not in heat. */
    public static long heatStartAt(Reproduction r, long now, ReproTiming t) {
        switch (stateAt(r, now, t)) {
            case FOAL_HEAT:
                return r.foaledTick() + t.postpartumTicks();
            case ESTRUS:
                return now - cyclePosition(r, now, t);
            default:
                return Reproduction.NEVER;
        }
    }

    /** When the heat she is in now ends, or {@link Reproduction#NEVER} when she is not in heat. */
    public static long heatEndsAt(Reproduction r, long now, ReproTiming t) {
        switch (stateAt(r, now, t)) {
            case FOAL_HEAT:
                return r.foaledTick() + t.postpartumTicks() + t.foalHeatTicks();
            case ESTRUS:
                return heatStartAt(r, now, t) + t.estrusTicks();
            default:
                return Reproduction.NEVER;
        }
    }

    /**
     * May a stallion cover her on his own now? In heat, and not yet this heat
     * (owner, 2026-09-13: once per heat, when they meet). A failed cover waits
     * for her next heat, which is what keeps a paddock from breeding flat out.
     */
    public static boolean mayTryNaturally(Reproduction r, long now, ReproTiming t) {
        long start = heatStartAt(r, now, t);
        return start != Reproduction.NEVER && r.lastNaturalTry() < start;
    }

    /** In the second half of an ordinary heat. Foal heat has one flat chance and no peak. */
    public static boolean inPeak(Reproduction r, long now, ReproTiming t) {
        return stateAt(r, now, t) == ReproState.ESTRUS
                && cyclePosition(r, now, t) * 2 >= t.estrusTicks();
    }

    /** The stage's chance before either horse's fertility is applied. 0 when not receptive. */
    public static double baseChance(Reproduction r, long now, ReproTiming t) {
        switch (stateAt(r, now, t)) {
            case FOAL_HEAT:
                return FOAL_HEAT_CHANCE;
            case ESTRUS:
                return inPeak(r, now, t) ? PEAK_CHANCE : EARLY_CHANCE;
            default:
                return 0.0;
        }
    }

    /**
     * How long until she is receptive again - for "not in heat, about N
     * minutes". 0 if she is receptive now. A pregnant mare answers with the time
     * to her due date plus postpartum, which is when foal heat begins.
     */
    public static long ticksUntilReceptive(Reproduction r, long now, ReproTiming t) {
        ReproState state = stateAt(r, now, t);
        switch (state) {
            case FOAL_HEAT:
            case ESTRUS:
                return 0L;
            case PREGNANT:
                return r.pregnancy().get().ticksLeft(now) + t.postpartumTicks();
            case POSTPARTUM:
                return r.foaledTick() + t.postpartumTicks() - now;
            default:
                return t.cycleTicks() - cyclePosition(r, now, t);
        }
    }

    // ------------------------------------------------------------------
    // Conception
    // ------------------------------------------------------------------

    /** A stallion's multiplier, given the covers he has already made today. */
    public static double stallionFactor(int coversAlreadyToday) {
        return coversAlreadyToday >= FREE_COVERS_PER_DAY ? TIRED_STALLION_FACTOR : 1.0;
    }

    /**
     * The chance one breeding takes: the stage's base chance times everything
     * else, capped at {@link #MAX_CHANCE}. Zero stays zero - no factor can make
     * a mare out of heat conceive.
     */
    public static double conceptionChance(double base, double mareFactor, double sireFactor) {
        if (base <= 0.0) {
            return 0.0;
        }
        double chance = base * Math.max(0.0, mareFactor) * Math.max(0.0, sireFactor);
        return Math.min(MAX_CHANCE, chance);
    }

    /** When a pregnancy carrying a lethal embryo loses it: early, in the first third. */
    public static long earlyLossTick(long conceivedTick, long dueTick, Rng rng) {
        double at = EARLY_LOSS_FROM + (EARLY_LOSS_TO - EARLY_LOSS_FROM) * rng.nextFloat();
        return conceivedTick + Math.max(1L, Math.round((dueTick - conceivedTick) * at));
    }

    // ------------------------------------------------------------------
    // Birth
    // ------------------------------------------------------------------

    /** How many of a pair of twins are born alive: 2, 1 or 0. */
    public static int twinSurvivors(Rng rng) {
        double roll = rng.nextFloat();
        if (roll < TWINS_BOTH_LIVE) {
            return 2;
        }
        return roll < TWINS_BOTH_LIVE + TWINS_ONE_LOST ? 1 : 0;
    }

    /** In the last third - slower, and the info screen says so. */
    public static boolean late(Pregnancy p, long now) {
        return p.progress(now) >= LATE_FRACTION;
    }

    /** Whole days to the due date, rounded up - "about N days left". */
    public static long daysLeft(Pregnancy p, long now, ReproTiming t) {
        long left = p.ticksLeft(now);
        return (left + t.dayTicks() - 1) / t.dayTicks();
    }

    // ------------------------------------------------------------------
    // Nursing
    // ------------------------------------------------------------------

    /** Her foals have been out of reach for a whole day: they are weaned. */
    public static boolean weanedByAbsence(long apartSince, long now, ReproTiming t) {
        return apartSince != Reproduction.NEVER && now - apartSince >= t.dayTicks();
    }
}
