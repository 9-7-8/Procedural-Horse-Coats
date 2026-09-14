package com.example.horsegenetics.common.herd;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.horse.Sex;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * <b>The numbers behind band life</b>, in one place and free of Minecraft.
 *
 * <p>Modelled on free-roaming mustang bands - see the science tab of
 * {@code wiki/horse-care.html}. Every rate is written <b>per Minecraft day</b>
 * ({@link #DAY_TICKS}) because that is the pace the owner chose ("colts leave
 * 1-3 days after growing up"), and turned into a per-scan chance by
 * {@link #perScan} so the game module can scan as often or as rarely as its tick
 * budget allows without changing how often anything happens.
 *
 * <p>Nothing here reads a horse or a level. The game module gathers the facts -
 * ages, sexes, who is near whom, health - and asks.
 */
public final class HerdRules {

    private HerdRules() {
    }

    /** One Minecraft day, and the unit every rate below is written in. */
    public static final long DAY_TICKS = 24_000L;

    /** Vanilla's growing-up time: a foal is an adult one day after birth. */
    public static final long GROW_UP_TICKS = 24_000L;

    // ------------------------------------------------------------------
    // Relationships
    // ------------------------------------------------------------------

    /** Familiarity grows about this fast per day spent together (a rate, not a step). */
    public static final double FAMILIARITY_PER_DAY = 3.0;
    /** ...and fades this fast per day apart. A friend is mostly forgotten after a week away. */
    public static final double FAMILIARITY_DECAY_PER_DAY = 0.35;

    /** Grooming bonds grow slower than familiarity... */
    public static final double GROOMING_PER_DAY = 1.2;
    /** ...and fade far slower, which is what lets them outlast a stallion's tenure. */
    public static final double GROOMING_DECAY_PER_DAY = 0.06;

    public static final double RIVALRY_PER_DAY = 0.8;
    public static final double RIVALRY_DECAY_PER_DAY = 0.25;

    /** Familiarity at which another horse counts as a companion. */
    public static final double COMPANION_FAMILIARITY = 0.55;
    /** Grooming at which two horses are grooming partners. */
    public static final double GROOMING_PARTNER = 0.35;
    /** Rivalry at which a horse has a rival worth naming - and worth challenging. */
    public static final double RIVAL_THRESHOLD = 0.35;

    /** How much one outcome moves the rank toward its end. */
    public static final double STAKES_DISPLACEMENT = 0.08;
    public static final double STAKES_SPAR = 0.2;
    public static final double STAKES_FIGHT = 0.6;

    /** A horse displaces another at food or water only if it outranks it by this much. */
    public static final double DISPLACE_MARGIN = 0.15;

    // ------------------------------------------------------------------
    // How often things happen, per day
    // ------------------------------------------------------------------

    /** A familiar pair of bachelors or colts spars about this often. */
    public static final double SPARS_PER_PAIR_PER_DAY = 2.0;
    /** Grooming partners settle down together about this often. */
    public static final double GROOMS_PER_PAIR_PER_DAY = 3.0;
    /** A bachelor near a band challenges its stallion about this often... */
    public static final double CHALLENGES_PER_BAND_PER_DAY = 0.35;
    /** ...and a mare drifts to a band nearby about this often. */
    public static final double TRANSFERS_PER_MARE_PER_DAY = 0.05;

    /** A takeover fight ends when either horse drops below this share of its health. Nobody dies of one. */
    public static final double FIGHT_YIELD_HEALTH = 0.4;

    // ------------------------------------------------------------------
    // Arithmetic
    // ------------------------------------------------------------------

    /**
     * A value moving toward 1 at {@code ratePerDay} for {@code days}: exponential
     * approach, so it slows as it nears the top and can never overshoot, however
     * the time is sliced.
     */
    public static double approach(double current, double ratePerDay, double days) {
        return current + (1.0 - current) * (1.0 - Math.exp(-ratePerDay * days));
    }

    /**
     * The chance something that happens {@code perDay} times a day happens in
     * one scan of {@code scanTicks} - a Poisson "at least once". Keeps the daily
     * rate true whatever the scan interval.
     */
    public static double perScan(double perDay, long scanTicks) {
        if (perDay <= 0.0 || scanTicks <= 0) {
            return 0.0;
        }
        return 1.0 - Math.exp(-perDay * (scanTicks / (double) DAY_TICKS));
    }

    /** How old a horse born at {@code bornTick} is, in days. */
    public static double ageDays(long bornTick, long now) {
        return Math.max(0L, now - bornTick) / (double) DAY_TICKS;
    }

    /** How many days it has been an adult - negative while it is still a foal. */
    public static double adultDays(long bornTick, long now) {
        return (now - bornTick - GROW_UP_TICKS) / (double) DAY_TICKS;
    }

    // ------------------------------------------------------------------
    // Dispersal
    // ------------------------------------------------------------------

    /**
     * How many adult days a young horse stays in the band it was born into.
     * Colts leave early - one to three days - and fillies later, three to five:
     * the stallion drives a growing son out, a daughter drifts away nearer
     * maturity. Rolled once, at birth.
     */
    public static double disperseAfterDays(Sex sex, Rng rng) {
        return sex == Sex.MALE ? 1.0 + 2.0 * rng.nextFloat() : 3.0 + 2.0 * rng.nextFloat();
    }

    /** Has a young horse stayed long enough in its natal band to leave it? */
    public static boolean dueToDisperse(long bornTick, long now, double disperseAfterDays) {
        return adultDays(bornTick, now) >= disperseAfterDays;
    }

    // ------------------------------------------------------------------
    // Contests
    // ------------------------------------------------------------------

    /**
     * What one horse brings to a contest. Condition is hearts actually left, not
     * the ceiling - a tired or wounded stallion is exactly the one who gets
     * displaced - plus size, some experience, and the order the two already have.
     */
    public record Contestant(double health, double maxHealth, double scale, double ageDays, double rankOverOther) {
    }

    /**
     * The chance {@code a} comes out on top against {@code b}. A logistic on the
     * difference of their scores, so an even match is a coin, a large horse in
     * good condition usually wins, and an upset stays possible.
     */
    public static double winChance(Contestant a, Contestant b) {
        double diff = score(a) - score(b);
        return 1.0 / (1.0 + Math.exp(-diff * 1.6));
    }

    private static double score(Contestant c) {
        double condition = c.health() / 10.0;                 // five hearts is one point
        double size = (c.scale() - 1.0) * 3.0;
        double experience = Math.min(c.ageDays(), 10.0) * 0.08;
        return condition + size + experience + c.rankOverOther() * 0.9;
    }

    /**
     * Settle a contest on both ledgers at once, so the two views can never
     * disagree about who won.
     *
     * @return {@code [winner's ledger, loser's ledger]}
     */
    public static SocialLedger[] settle(SocialLedger winner, UUID winnerId, SocialLedger loser, UUID loserId,
                                        double stakes, long now) {
        return new SocialLedger[]{
                winner.contest(loserId, true, stakes, now),
                loser.contest(winnerId, false, stakes, now)};
    }

    // ------------------------------------------------------------------
    // Band roles
    // ------------------------------------------------------------------

    /** What a candidate lead mare brings: how long she has lived and how the others rank her. */
    public record MareFacts(UUID id, double ageDays, double standing) {
    }

    /**
     * The mare the band follows: the oldest adult mare, her standing among the
     * others breaking a tie, and her id breaking that so the answer never
     * flickers between two equally old mares.
     */
    public static Optional<UUID> leadMare(List<MareFacts> mares) {
        MareFacts best = null;
        for (MareFacts m : mares) {
            if (best == null || better(m, best)) {
                best = m;
            }
        }
        return best == null ? Optional.empty() : Optional.of(best.id());
    }

    private static boolean better(MareFacts a, MareFacts b) {
        if (Math.abs(a.ageDays() - b.ageDays()) > 0.25) {
            return a.ageDays() > b.ageDays();
        }
        if (a.standing() != b.standing()) {
            return a.standing() > b.standing();
        }
        return a.id().compareTo(b.id()) < 0;
    }
}
