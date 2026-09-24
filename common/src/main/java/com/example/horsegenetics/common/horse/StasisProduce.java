package com.example.horsegenetics.common.horse;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.spec.HorseAbilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <b>What a shelved horse makes while nobody is looking</b> - the Horse Stasis
 * Bank's drop buffer, as arithmetic.
 *
 * <h2>It is the egg-layer's own rule, with the field taken away</h2>
 * A horse standing in a paddock produces on exactly one verb,
 * {@link GeneAbility.Produce} - an item id, an interval, and a count between
 * {@code min} and {@code max} - and nothing else about the horse decides
 * whether it happens. The bank changes none of that. It supplies the clock and
 * the basket; {@link HorseAbilities#activeFor} decides what and how often, off
 * the same genome the live horse would be read from.
 *
 * <p><b>Milk is not here, and cannot be.</b> The Roadmap tab used to call this a
 * "milk/drops buffer", and that half was never buildable: milk, potion milk and
 * their volume locus are all {@code Yield} abilities, which fire on a player
 * holding a bucket out to a horse. There is no passive milking path in the mod
 * to copy, so a bank that filled itself with milk would be inventing a mechanic
 * rather than shelving one. The buffer collects what a horse <i>drops on its
 * own</i>: eggs, feathers, wool, ink, string, bone, leather, and whatever a
 * future gene adds to the same verb.
 *
 * <h2>Two things it deliberately does not do</h2>
 * <ul>
 *   <li><b>{@link GeneAbility.Produce#nearbyCap()} is ignored.</b> That cap
 *       counts loose item entities around a live horse so a paddock does not
 *       silt up, and a chamber has no ground to drop on. The buffer's own nine
 *       slots are the cap instead - see {@code HorseStasisBankBlockEntity},
 *       which refuses the turn rather than destroying a yield it cannot hold.</li>
 *   <li><b>A condition it cannot evaluate refuses.</b> Most of the ability
 *       vocabulary's condition flags are questions about a living animal in a
 *       world - is it raining, is it in water, is a jukebox playing - and a
 *       horse in a chamber is in none of those. {@link #holdsInStasis} answers
 *       only the handful that are facts about the horse itself, and says no to
 *       everything else. A gene that wants to produce in a bank must say
 *       {@code always} or gate on sex or age.</li>
 * </ul>
 *
 * <p>Pure Java and unit-tested - {@code StasisProduceTest}. The game module's
 * job is to hand over the genome, the clock and the last-fired stamps, and to
 * turn an item id into an item.
 */
public final class StasisProduce {

    private StasisProduce() {
    }

    /**
     * The key a gene's last firing is stamped under, in the horse's cooldowns
     * attachment.
     *
     * <p><b>Durable, unlike the live path.</b> {@code GeneAbilityHandler} keeps
     * its produce cooldowns in a static map that is lost on restart and cleared
     * wholesale past eight thousand entries; a shelved horse has no tick to
     * carry one, so the bank writes its stamp into the horse's own tag. The
     * consequence is the pleasant one: a horse shelved with an egg due lays it
     * on its next turn and then keeps a real clock, rather than laying one every
     * time the server comes up.
     */
    public static String key(String geneKey) {
        return "produce:" + geneKey;
    }

    /** One thing a stored horse has made: which gene made it, what, and how many. */
    public record Yield(String geneKey, String item, int count) {

        public Yield {
            count = Math.max(1, count);
        }
    }

    /**
     * Everything this horse owes the buffer now.
     *
     * @param sex        read off the horse's record - the one condition flag a
     *                   stored horse can answer for certain
     * @param adult      {@code false} for a foal; a shelved foal does not grow
     *                   up, so this is whatever it was captured as
     * @param now        the game tick
     * @param lastByKey  the horse's cooldown stamps. A key that is absent has
     *                   never fired and is due - {@code HorseCooldownsAttachment}'s
     *                   own rule, and the same one a live horse gets after a
     *                   restart
     * @return one entry per gene that is due, in gene order; empty is the common
     *         case and costs one walk of the ability list
     */
    public static List<Yield> due(Genotype genotype, Epigenome epigenome, Sex sex, boolean adult,
                                  long now, Map<String, Long> lastByKey, Rng rng) {
        if (genotype == null || !HorseAbilities.anyLoaded()) {
            return List.of();
        }
        List<Yield> out = new ArrayList<>();
        for (HorseAbilities.Active active : HorseAbilities.activeFor(genotype, epigenome)) {
            if (!(active.ability() instanceof GeneAbility.Produce produce)) {
                continue;
            }
            if (!holdsInStasis(produce.when(), sex, adult)) {
                continue;
            }
            if (!ready(lastByKey, key(active.geneKey()), now, produce.intervalTicks())) {
                continue;
            }
            out.add(new Yield(active.geneKey(), produce.item(), roll(produce, rng)));
        }
        return List.copyOf(out);
    }

    /**
     * Has {@code interval} passed since this key last fired? An absent stamp is
     * ready; so is a stamp from the future, which is what a world whose time
     * command has been run backwards leaves behind.
     */
    public static boolean ready(Map<String, Long> lastByKey, String key, long now, int interval) {
        Long last = lastByKey == null ? null : lastByKey.get(key);
        if (last == null) {
            return true;
        }
        long since = now - last;
        return since < 0 || since >= Math.max(1, interval);
    }

    private static int roll(GeneAbility.Produce produce, Rng rng) {
        int min = Math.max(1, produce.min());
        int max = Math.max(min, produce.max());
        return max == min ? min : min + rng.nextInt(max - min + 1);
    }

    /**
     * <b>Can this ability's condition be said to hold for a horse that is not in
     * the world?</b>
     *
     * <p>Answered for the flags that are facts about the horse itself -
     * {@code adult}, {@code baby}, {@code sex_male}, {@code sex_female} - and
     * refused for every other one, because every other one is a question about a
     * place. {@code in_water}, {@code night}, {@code raining}, {@code has_rider}
     * and the rest are not false about a chamber so much as meaningless, and
     * guessing either way would be a gene quietly behaving differently in a bank
     * than in a field. Refusing is the answer that cannot surprise anybody: the
     * horse simply produces nothing until it is let out.
     *
     * <p>{@code Not} of an unanswerable flag is unanswerable too, which is why
     * this is not a boolean over a three-valued question written in two.
     */
    public static boolean holdsInStasis(GeneAbility.Condition when, Sex sex, boolean adult) {
        if (when == null || when instanceof GeneAbility.Condition.Always) {
            return true;
        }
        if (when instanceof GeneAbility.Condition.Flag flag) {
            Boolean known = flagInStasis(flag.name(), sex, adult);
            return known != null && (flag.negate() ? !known : known);
        }
        if (when instanceof GeneAbility.Condition.Not not) {
            // Unanswerable stays unanswerable: "not raining" is no more a fact
            // about a chamber than "raining" is.
            return answerable(not.term(), sex, adult) && !holdsInStasis(not.term(), sex, adult);
        }
        if (when instanceof GeneAbility.Condition.All all) {
            for (GeneAbility.Condition term : all.terms()) {
                if (!holdsInStasis(term, sex, adult)) {
                    return false;
                }
            }
            return true;
        }
        if (when instanceof GeneAbility.Condition.Any any) {
            for (GeneAbility.Condition term : any.terms()) {
                if (holdsInStasis(term, sex, adult)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    /** Whether {@link #holdsInStasis}'s answer means anything for this condition. */
    private static boolean answerable(GeneAbility.Condition when, Sex sex, boolean adult) {
        if (when == null || when instanceof GeneAbility.Condition.Always) {
            return true;
        }
        if (when instanceof GeneAbility.Condition.Flag flag) {
            return flagInStasis(flag.name(), sex, adult) != null;
        }
        if (when instanceof GeneAbility.Condition.Not not) {
            return answerable(not.term(), sex, adult);
        }
        if (when instanceof GeneAbility.Condition.All all) {
            for (GeneAbility.Condition term : all.terms()) {
                if (!answerable(term, sex, adult)) {
                    return false;
                }
            }
            return true;
        }
        if (when instanceof GeneAbility.Condition.Any any) {
            for (GeneAbility.Condition term : any.terms()) {
                if (!answerable(term, sex, adult)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /** {@code null} for a flag a chamber cannot answer. */
    private static Boolean flagInStasis(String name, Sex sex, boolean adult) {
        if (name == null) {
            return null;
        }
        switch (name) {
            case "adult":
                return adult;
            case "baby":
                return !adult;
            case "sex_female":
                return sex == Sex.FEMALE;
            case "sex_male":
                return sex == Sex.MALE;
            default:
                return null;
        }
    }
}
