package com.example.horsegenetics.common.repro;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Every sentence a player reads about heat, pregnancy and covers</b>: the
 * action-bar refusals, the info screen's Breeding line, and the vet's kit.
 * Here rather than in the game module so the wording can be tested - above all
 * that the info line never gives away twins and the kit always does.
 */
public final class ReproText {

    private ReproText() {
    }

    /** Ticks as real time a player can plan around: minutes, then hours. */
    public static String duration(long ticks) {
        long minutes = Math.max(1L, (ticks + 1_199L) / 1_200L);
        if (minutes < 60L) {
            return "about " + minutes + " min";
        }
        long hours = (minutes + 59L) / 60L;
        return "about " + hours + (hours == 1L ? " hour" : " hours");
    }

    /** Why a mare cannot be bred right now, and how long until she can. */
    public static String notReceptive(String name, Reproduction r, long now, ReproTiming t) {
        long wait = ReproRules.ticksUntilReceptive(r, now, t);
        switch (ReproRules.stateAt(r, now, t)) {
            case PREGNANT:
                return name + " is pregnant - " + duration(r.pregnancy().get().ticksLeft(now)) + " to go.";
            case POSTPARTUM:
                return name + " foaled recently - foal heat in " + duration(wait) + ".";
            case ESTRUS:
            case FOAL_HEAT:
                return name + " is in heat.";
            default:
                return name + " is not in heat - " + duration(wait) + " to go.";
        }
    }

    /**
     * The info screen's one line for an adult mare (owner, 2026-09-13). Twins are
     * <b>not</b> revealed here - that is what the vet's kit is for.
     */
    public static String breedingLine(Reproduction r, long now, ReproTiming t) {
        String line;
        switch (ReproRules.stateAt(r, now, t)) {
            case PREGNANT: {
                Pregnancy p = r.pregnancy().get();
                line = "Pregnant - " + duration(p.ticksLeft(now)) + " to go"
                        + (ReproRules.late(p, now) ? ", heavy and slow" : "");
                break;
            }
            case POSTPARTUM:
                line = "Recently foaled - foal heat in " + duration(ReproRules.ticksUntilReceptive(r, now, t));
                break;
            case FOAL_HEAT:
                line = "In foal heat";
                break;
            case ESTRUS:
                line = ReproRules.inPeak(r, now, t) ? "In heat - best time now" : "In heat";
                break;
            default:
                line = "Not in heat - " + duration(ReproRules.ticksUntilReceptive(r, now, t)) + " to go";
        }
        return r.lactating() ? line + " • nursing" : line;
    }

    /** What the vet's kit says about a mare or filly. */
    public static List<String> vetMare(String name, boolean adult, Reproduction r, long now, ReproTiming t) {
        List<String> lines = new ArrayList<>();
        if (!adult) {
            lines.add(name + " is a filly, too young to breed.");
            return lines;
        }
        ReproState state = ReproRules.stateAt(r, now, t);
        switch (state) {
            case PREGNANT: {
                Pregnancy p = r.pregnancy().get();
                lines.add(name + " is pregnant with " + (p.twins() ? "twins" : "one foal") + " - "
                        + duration(p.ticksLeft(now)) + " to go.");
                break;
            }
            case POSTPARTUM:
                lines.add(name + " foaled recently. Foal heat begins in "
                        + duration(ReproRules.ticksUntilReceptive(r, now, t)) + ".");
                break;
            case FOAL_HEAT:
                lines.add(name + " is in foal heat. It ends in "
                        + duration(ReproRules.heatEndsAt(r, now, t) - now) + ".");
                break;
            case ESTRUS: {
                long start = ReproRules.heatStartAt(r, now, t);
                String half = ReproRules.inPeak(r, now, t)
                        ? "in the better half"
                        : "in the first half - the better half starts in "
                                + duration(start + t.estrusTicks() / 2 - now);
                lines.add(name + " is in heat, " + half + ". It ends in "
                        + duration(ReproRules.heatEndsAt(r, now, t) - now) + ".");
                break;
            }
            default:
                lines.add(name + " is not in heat. Her next heat begins in "
                        + duration(ReproRules.ticksUntilReceptive(r, now, t)) + ".");
        }
        if (state.receptive() && !ReproRules.mayTryNaturally(r, now, t)) {
            lines.add("A stallion has already covered her this heat.");
        }
        if (r.lactating()) {
            lines.add("She is nursing.");
        }
        return lines;
    }

    /** What the vet's kit says about a stallion, colt or gelding. */
    public static List<String> vetMale(String name, boolean adult, boolean gelded, int coversToday) {
        List<String> lines = new ArrayList<>();
        if (gelded) {
            lines.add(name + " is a gelding.");
        } else if (!adult) {
            lines.add(name + " is a colt, too young to breed.");
        } else {
            lines.add(name + " is an entire stallion: " + coversToday + " of "
                    + ReproRules.FREE_COVERS_PER_DAY + " covers made today.");
        }
        return lines;
    }
}
