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

    /**
     * <b>The state word alone</b>, for a look-at tooltip that has one line and
     * is on screen whenever the player glances at a horse.
     *
     * <p>Deliberately the shortest of the three readouts, and not merely
     * {@link #breedingLine} with the numbers filed off:
     *
     * <ul>
     *   <li><b>No countdowns.</b> A tooltip redraws constantly, so "about 4 min
     *       to go" would tick down in the corner of a player's eye all the way
     *       across a paddock - and in a world with {@code debug.tools} on, where
     *       a whole heat lasts a minute, it would churn.</li>
     *   <li><b>Nothing for a mare who is simply between heats.</b> The screen
     *       says "Not in heat" because a player asked it a question; a glance
     *       asked nothing, and a line on every horse you look at is noise. Empty
     *       means nothing worth saying, not nothing known.</li>
     *   <li><b>Foal heat reads as "In heat".</b> Which heat she is in is a
     *       distinction the vet's kit draws, and the whole point of this being
     *       the short readout is that it does not.</li>
     * </ul>
     *
     * <p>Twins are not here for the same reason they are not on the info line:
     * that is the kit's job, and it has to keep one.
     *
     * @return the words, or {@code ""} when there is nothing worth a line
     */
    public static String glanceLine(Reproduction r, long now, ReproTiming t) {
        List<String> words = new ArrayList<>();
        ReproState state = ReproRules.stateAt(r, now, t);
        if (state == ReproState.PREGNANT) {
            words.add("Pregnant");
        } else if (state.receptive()) {
            words.add("In heat");
        }
        if (r.lactating()) {
            words.add("Nursing");
        }
        return String.join(" • ", words);
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
