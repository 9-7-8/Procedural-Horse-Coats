package com.example.horsegenetics.common.horse;

import com.example.horsegenetics.common.genetics.GameteBias;
import com.example.horsegenetics.common.repro.Conception;
import com.example.horsegenetics.common.repro.ReproRules;
import com.example.horsegenetics.common.repro.ReproTiming;
import com.example.horsegenetics.common.repro.Reproduction;

/**
 * <b>Breeding two horses that are both in chambers</b> - whether the Horse
 * Stasis Bank may put a shelved stallion to a shelved mare, and the mating it
 * hands to {@link Conception#attempt}.
 *
 * <h2>It is a paddock with a lid on it</h2>
 * Nothing about the rules is new. A Spacer chamber marked <b>at stud</b> is a
 * horse turned out into the bank's paddock, and the bank then applies the
 * ordinary once-per-heat rule a stallion left with a mare obeys
 * ({@link ReproRules#mayTryNaturally}): she must be in heat, she must not
 * already have been covered this heat, and both of them must be well enough
 * ({@link ReproRules#COVER_HEALTH}). Owner, 2026-09-24: <i>"only creates a new
 * foal once per the mare's heat, same cycle as on land"</i> - which is not a cap
 * this had to invent, but the rule it had to reuse.
 *
 * <p>Her cycle needs no ticking to reach her. Every time in {@link Reproduction}
 * is an absolute game tick and the state is <b>derived</b> from those anchors,
 * so a mare who has spent three cycles on a shelf is in exactly the phase she
 * would have been in standing in a field. That is the one property that makes
 * this feature possible at all, and it was there before stasis was.
 *
 * <h2>What the bank does not do</h2>
 * <ul>
 *   <li><b>Carrot effects do not reach a chamber.</b> An armed carrot lives on
 *       the {@code ARMED_CARROTS} attachment of a live horse and is spent by a
 *       breeding it can see; a bank breeding happens with nobody present. Both
 *       biases are {@link GameteBias#NONE} here, which is also what a wild
 *       cover draws.</li>
 *   <li><b>No crowd check.</b> {@link ReproRules#DEFAULT_NATURAL_CAP} counts
 *       animals in a chunk so a packed pen stops breeding; a bank holds no
 *       animals at all, and the number of chambers marked at stud is already
 *       the player's own cap.</li>
 *   <li><b>It does not deliver.</b> A pregnancy is carried in the chamber and
 *       the mare comes <i>out</i> to foal - see
 *       {@code HorseStasisBankBlockEntity}. Birth is the one moment a horse has
 *       to be an animal again, and routing it through the ordinary
 *       {@code ReproHandler} birth is what keeps a bank-bred foal identical to
 *       every other one: same naming policy, same pedigree, same ancestry row,
 *       same chat line.</li>
 * </ul>
 *
 * <p>Pure Java and unit-tested - {@code StasisMatingTest}.
 */
public final class StasisMating {

    private StasisMating() {
    }

    /** Why the bank is not breeding this mare right now. {@link #READY} is the one that acts. */
    public enum Verdict {

        /** Everything holds: draw. */
        READY("ready"),
        /** Not a mare, no papers, or a record with no genome to draw from. */
        UNFIT("cannot be bred in a bank"),
        /** Already carrying - the bank is waiting for her due date. */
        PREGNANT("in foal"),
        /** Out of heat, just foaled, or in the postpartum rest. */
        NOT_IN_HEAT("not in heat"),
        /** In heat, and a stallion has already had his one try at it. */
        COVERED_THIS_HEAT("already covered this heat"),
        /** Hurt. The bank's own feed and water is how that gets fixed. */
        TOO_HURT("too hurt to breed"),
        /** Nothing in this bank that could sire a foal on her. */
        NO_STALLION("no stallion at stud here");

        private final String said;

        Verdict(String said) {
            this.said = said;
        }

        /** The words the Browse tab puts on her row. */
        public String said() {
            return said;
        }
    }

    /**
     * May the bank cover this mare now, leaving the stallion out of it?
     *
     * <p>Split from {@link #verdict} so the expensive half - finding a stallion
     * among fifty-four chambers - is only paid for by a mare who is otherwise
     * ready.
     *
     * @param health    hers, out of the chamber
     * @param maxHealth hers, out of her record
     */
    public static Verdict mareVerdict(HorseRecord mare, Reproduction repro, float health, float maxHealth,
                                      long now, ReproTiming timing) {
        if (mare == null || mare.sex() != Sex.FEMALE || !mare.hasGenome()) {
            return Verdict.UNFIT;
        }
        if (repro.pregnant()) {
            return Verdict.PREGNANT;
        }
        if (!ReproRules.stateAt(repro, now, timing).receptive()) {
            return Verdict.NOT_IN_HEAT;
        }
        if (!ReproRules.mayTryNaturally(repro, now, timing)) {
            return Verdict.COVERED_THIS_HEAT;
        }
        if (maxHealth > 0.0F && !ReproRules.healthyEnoughToBreed(health, maxHealth)) {
            return Verdict.TOO_HURT;
        }
        return Verdict.READY;
    }

    /**
     * Could this horse sire a foal in a bank? Not gelded, male, and carrying a
     * genome to draw from - and well enough, by the same rule the mare is held
     * to, since a natural cover asks it of both.
     */
    public static boolean canSire(HorseRecord sire, float health, float maxHealth) {
        if (sire == null || sire.sex() != Sex.MALE || sire.gelded() || !sire.hasGenome()) {
            return false;
        }
        return maxHealth <= 0.0F || ReproRules.healthyEnoughToBreed(health, maxHealth);
    }

    /**
     * The mating itself. No biases: see the class note on carrots.
     *
     * @param bredBy credited to nobody - a bank breeding has no breeder at the
     *               moment it happens, exactly as a wild cover does not
     */
    public static Conception.Mating mating(HorseRecord dam, HorseRecord sire, String bredBy) {
        return new Conception.Mating(
                dam.genome(), dam.lineage(),
                sire.genome(), sire.lineage(), sire.id(),
                sire.firstName(), sire.lastName(), sire.generation(),
                GameteBias.NONE, GameteBias.NONE,
                bredBy == null ? "" : bredBy);
    }
}
