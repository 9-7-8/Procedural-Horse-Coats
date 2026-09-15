package com.example.horsegenetics.common.care;

/**
 * <b>Hunger</b>: an invisible number on every horse, and the numbers behind it, free of
 * Minecraft.
 *
 * <p>Owner, 2026-09-14: <i>"horses have an invisible hunger bar, that works like this: it
 * decays over time like normal, and then decays faster to heal injuries at an increased
 * rate (but they cant heal when they're starving). It doesn't injure the horse or gate
 * anything else."</i>
 *
 * <ul>
 *   <li><b>It drains</b> at {@link #DRAIN_PER_DAY}, so a full horse is hungry after a day.</li>
 *   <li><b>Healing spends it.</b> Every health point healed costs {@link #COST_PER_HEALTH},
 *       and a horse with hunger to spend heals faster than the old gated heal did
 *       ({@link #HEAL_PER_SECOND}, doubled in a herd).</li>
 *   <li><b>A starving horse cannot heal</b> (at or below {@link #STARVING}), and healing
 *       never spends a horse down past that line.</li>
 *   <li><b>Nothing else.</b> Hunger never hurts a horse, and no other system reads it.</li>
 * </ul>
 *
 * <h2>Looking for food</h2>
 * Below {@link #HUNGRY} a horse goes looking, and eats until {@link #SATED}. Above it, a
 * horse eats only what it is already standing at. What it looks for, best first, is
 * {@link Food}'s order - the owner's, with the tag's crops and moss placed by the owner
 * on the same day. Everything a horse eats is used up. A carnivore that finds none of the
 * foods its diet allows hunts a passive animal and eats what it drops; a blood-drinker's
 * one bite a day is its meal. Which foods a diet allows is the game module's to decide.
 *
 * <p>Nothing here reads a horse or a level. The game module gathers the facts and asks.
 */
public final class Hunger {

    private Hunger() {
    }

    /** One Minecraft day, the unit the drain is written in. */
    public static final long DAY_TICKS = 24_000L;

    public static final double FULL = 100.0;

    /** Below this a horse goes looking for food. */
    public static final double HUNGRY = 50.0;

    /** A horse that went looking eats until it reaches this. */
    public static final double SATED = 90.0;

    /** At or below this a horse cannot heal, and healing never spends below it. */
    public static final double STARVING = 10.0;

    /** The resting drain: a full horse is hungry after one day, and starving after about two. */
    public static final double DRAIN_PER_DAY = 50.0;

    /** Hunger spent per health point healed. A horse brought back from one heart spends about 40. */
    public static final double COST_PER_HEALTH = 2.0;

    /**
     * How fast a fed horse heals, in health points per second. The heal it replaces was one
     * point every thirty ticks (0.67 a second); this is half again.
     */
    public static final double HEAL_PER_SECOND = 1.0;

    /** A horse in a herd heals twice as fast, as it did before hunger. */
    public static final double HERD_HEAL_FACTOR = 2.0;

    /**
     * What a horse looks for, best first, and what one mouthful is worth. The order of the
     * constants is the order a hungry horse searches in; {@link #sought()} is false for the
     * two ways of eating a horse does not go and find.
     */
    public enum Food {
        /** Its favourite food (the food-preference locus), lying on the ground. */
        FAVOURITE(40.0, true),
        /** Any dropped food its diet lets it eat. */
        DROPPED(25.0, true),
        /** A cake block, eaten whole. */
        CAKE(60.0, true),
        /** A hay bale, eaten whole. */
        HAY(80.0, true),
        /** A crop, pumpkin, melon or sugar cane. */
        CROP(20.0, true),
        /** A tuft of grass or fern; failing that, the grass of a grass block, which turns to dirt. */
        GRASS(6.0, true),
        /** A moss block. */
        MOSS(6.0, true),
        MUSHROOM(10.0, true),
        FLOWER(4.0, true),
        /** Something a player held out. Not sought. */
        HAND(25.0, false),
        /** A blood-drinker's bite. Not sought; the bite goal finds its own. */
        BITE(20.0, false);

        private final double value;
        private final boolean sought;

        Food(double value, boolean sought) {
            this.value = value;
            this.sought = sought;
        }

        /** How much hunger one mouthful restores. */
        public double value() {
            return value;
        }

        /** Does a hungry horse go looking for this? */
        public boolean sought() {
            return sought;
        }
    }

    /** Hunger after {@code ticks} of resting drain. */
    public static double drain(double hunger, long ticks) {
        return clamp(hunger - DRAIN_PER_DAY * ticks / DAY_TICKS);
    }

    /** Can a horse with this much hunger heal at all? */
    public static boolean canHeal(double hunger) {
        return hunger > STARVING;
    }

    /** Should a horse go looking for food? */
    public static boolean seeksFood(double hunger) {
        return hunger < HUNGRY;
    }

    /** Does a horse that is already eating want another mouthful? */
    public static boolean wantsMore(double hunger) {
        return hunger < SATED;
    }

    /** Health points a fed horse heals over {@code ticks}, in or out of a herd. */
    public static double healOver(long ticks, boolean inHerd) {
        return HEAL_PER_SECOND * (inHerd ? HERD_HEAL_FACTOR : 1.0) * ticks / 20.0;
    }

    /**
     * How much of {@code wanted} health a horse can afford: all of it, or as much as the
     * hunger it has above {@link #STARVING} pays for. Zero for a starving horse.
     */
    public static double affordable(double hunger, double wanted) {
        if (!canHeal(hunger) || wanted <= 0.0) {
            return 0.0;
        }
        return Math.min(wanted, (hunger - STARVING) / COST_PER_HEALTH);
    }

    /** Hunger after healing {@code healed} health points. */
    public static double afterHealing(double hunger, double healed) {
        return clamp(hunger - Math.max(0.0, healed) * COST_PER_HEALTH);
    }

    /** Hunger after one mouthful of {@code food}. */
    public static double eat(double hunger, Food food) {
        return clamp(hunger + food.value());
    }

    private static double clamp(double hunger) {
        return hunger < 0.0 ? 0.0 : Math.min(hunger, FULL);
    }
}
