package com.example.horsegenetics.common.repro;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * <b>A mare carrying one or two {@link Embryo}s.</b> Every time is an absolute
 * game tick, so a mare whose chunk was unloaded for a week is exactly as far on
 * as one that was watched the whole time - nothing counts down.
 *
 * @param lossTick when the embryos marked {@link Embryo#lostEarly()} are lost, or
 *                 {@link #NO_LOSS}. Scheduled at conception by
 *                 {@link ReproRules#earlyLossTick}.
 */
public record Pregnancy(List<Embryo> embryos, long conceivedTick, long dueTick, long lossTick) {

    public static final long NO_LOSS = Long.MIN_VALUE;

    public Pregnancy {
        embryos = List.copyOf(embryos);
        if (embryos.isEmpty()) {
            throw new IllegalArgumentException("a pregnancy needs at least one embryo");
        }
        if (dueTick <= conceivedTick) {
            throw new IllegalArgumentException("due " + dueTick + " is not after conception " + conceivedTick);
        }
    }

    public boolean twins() {
        return embryos.size() > 1;
    }

    public boolean due(long now) {
        return now >= dueTick;
    }

    public boolean hasEarlyLoss() {
        return lossTick != NO_LOSS;
    }

    public boolean earlyLossDue(long now) {
        return hasEarlyLoss() && now >= lossTick;
    }

    public int lostEarlyCount() {
        int n = 0;
        for (Embryo e : embryos) {
            if (e.lostEarly()) {
                n++;
            }
        }
        return n;
    }

    /**
     * What is left once the early loss has happened: the survivors carried on
     * to the same due date, or empty if nothing survived.
     */
    public Optional<Pregnancy> afterEarlyLoss() {
        List<Embryo> kept = new ArrayList<>();
        for (Embryo e : embryos) {
            if (!e.lostEarly()) {
                kept.add(e);
            }
        }
        if (kept.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Pregnancy(kept, conceivedTick, dueTick, NO_LOSS));
    }

    /** How far through, 0 at conception to 1 at the due tick (clamped). */
    public double progress(long now) {
        double p = (now - conceivedTick) / (double) (dueTick - conceivedTick);
        return Math.max(0.0, Math.min(1.0, p));
    }

    public long ticksLeft(long now) {
        return Math.max(0L, dueTick - now);
    }
}
