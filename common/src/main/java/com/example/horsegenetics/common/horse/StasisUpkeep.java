package com.example.horsegenetics.common.horse;

import com.example.horsegenetics.common.care.Hunger;

/**
 * <b>What one turn of the Horse Stasis Bank's upkeep does to one stored
 * horse</b> - fed from the bank's supply, healed a little, and the hunger and
 * water that paid for it.
 *
 * <h2>It is the world's healing rule, with the pond in a bucket</h2>
 * A horse standing in a field heals under three conditions, and
 * {@code HorseCareHandler} applies all three: it is hurt, its diet is one that
 * items can feed at all ({@link com.example.horsegenetics.common.genetics.Diet#fedByItems()}),
 * and there is water within a few blocks. Hunger is the currency - healing is
 * bought at {@link Hunger#COST_PER_HEALTH} a point and never spends a horse
 * below {@link Hunger#STARVING}, and eating is what puts hunger back.
 *
 * <p>The bank changes none of that. It only <b>supplies</b> the two things a
 * shelved horse cannot go and find: a mouthful out of the feed slot, and the
 * pond, as a meter topped up from a water bucket. Everything else is
 * {@link Hunger} doing exactly what it does in the field, which is why this
 * class is arithmetic and not policy.
 *
 * <h2>An idle bank eats nothing</h2>
 * {@link #upkeep} refuses at the first line for a horse that is not hurt, so a
 * bank of healthy horses never touches its feed or its water however long it
 * sits there. Supplies are spent on injuries and on nothing else - a bank that
 * quietly ate a hay bale a minute to keep fifty full horses full would be a
 * tax on using the feature at all.
 *
 * <p><b>Stasis does not make a horse hungry.</b> Nothing here drains hunger and
 * nothing ever will: a horse in a chamber does not tick, which is the whole
 * point of the chamber. The number only ever goes up here (eating) or down in
 * payment for health the horse actually got back.
 *
 * <p>Pure Java, and deliberately: the whole of the bank's economy is testable
 * without a game - see {@code StasisUpkeepTest}. The game module's job is only
 * to read these four numbers out of a stored horse's tag and write them back.
 */
public final class StasisUpkeep {

    private StasisUpkeep() {
    }

    /**
     * Ticks between one chamber's turn and the next. The same
     * {@code SCAN_INTERVAL} {@code HorseCareHandler} gives a live horse, so a
     * horse alone in a bank heals at very nearly the rate it would in a field.
     *
     * <p><b>The bank takes one chamber per turn</b>, in rotation. That is the
     * answer to "fifty-four horses is not free": the cost of a bank is one
     * horse's worth of work every {@value #HEAL_INTERVAL} ticks whether it
     * holds one chamber or every slot, and what a fuller bank costs is not
     * server time but <i>speed</i> - each horse's turn comes round less often.
     * A bank is a slow hospital, and it gets slower the more patients it has.
     */
    public static final int HEAL_INTERVAL = 30;

    /**
     * Health points one bucket of water is worth. A horse hurt to half of a
     * thirty-point bar costs fifteen of these, so a bucket sees off about seven
     * of them before the slot wants another.
     */
    public static final int WATER_PER_BUCKET = 100;

    /**
     * One horse after one turn of upkeep, and what it cost.
     *
     * @param hunger the horse's hunger afterwards
     * @param health the horse's health afterwards
     * @param water  water units left in the bank's meter
     * @param ate    a mouthful was taken out of the feed slot
     * @param healed health points given back, which is also the water spent
     */
    public record Result(double hunger, float health, int water, boolean ate, double healed) {

        /** Did anything at all happen - is there something to write back? */
        public boolean changed() {
            return ate || healed > 0.0;
        }
    }

    /**
     * Take one turn on one stored horse.
     *
     * @param hunger      its hunger, as the chamber holds it
     * @param health      its health, as the chamber holds it
     * @param maxHealth   the most it can have
     * @param water       water units the bank has left
     * @param fedByItems  whether anything a slot can hold feeds this horse at
     *                    all - a blood-drinker heals by biting and a
     *                    {@code NOTHING} diet by neither, exactly as in the field
     * @param mouthful    what the feed slot is worth to <i>this</i> horse, or
     *                    {@code null} if the slot is empty or holds something it
     *                    will not eat
     */
    public static Result upkeep(double hunger, float health, float maxHealth, int water,
                                boolean fedByItems, Hunger.Food mouthful) {
        Result unchanged = new Result(hunger, health, water, false, 0.0);
        if (health >= maxHealth || health <= 0.0F || maxHealth <= 0.0F || !fedByItems) {
            return unchanged;
        }

        // Eat first, then heal on what that bought - the order a horse in a
        // field does it in, and the order that lets one turn feed a starving
        // horse and get some health back out of the same turn.
        double fedHunger = hunger;
        boolean ate = false;
        if (mouthful != null && Hunger.wantsMore(fedHunger)) {
            fedHunger = Hunger.eat(fedHunger, mouthful);
            ate = true;
        }

        if (water <= 0) {
            // Fed but dry. The mouthful still counts: it is banked against the
            // turn after somebody fills the water slot.
            return new Result(fedHunger, health, water, ate, 0.0);
        }

        double wanted = Math.min(Math.min(maxHealth - health, Hunger.healOver(HEAL_INTERVAL, false)), water);
        double healed = Hunger.affordable(fedHunger, wanted);
        if (healed <= 0.0) {
            return new Result(fedHunger, health, water, ate, 0.0);
        }

        float grown = Math.min(maxHealth, health + (float) healed);
        // Water is charged on the health that actually landed, rounded up, so a
        // bucket can never be drained by a horse that got nothing back.
        int spent = (int) Math.ceil(grown - health);
        return new Result(Hunger.afterHealing(fedHunger, healed), grown,
                Math.max(0, water - spent), ate, grown - health);
    }
}
