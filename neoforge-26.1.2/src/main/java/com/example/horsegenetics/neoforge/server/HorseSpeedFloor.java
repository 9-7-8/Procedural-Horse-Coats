package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.trait.HorseTraits;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

/**
 * <b>No horse in this mod is too slow to get away from a zombie.</b>
 *
 * <h2>What it does, and what it deliberately does not</h2>
 * This is a clamp on the <b>attribute</b>, applied by
 * {@link HorseRecords#applyTraitsToEntity} at the moment the resolved body is
 * pushed onto the live entity - and nowhere else. It is the last thing that
 * happens to the number.
 *
 * <p>The genetics are <b>untouched</b>. {@link HorseTraits#MIN_SPEED} is still
 * {@code 0.02}, a horse that resolves to it still resolves to it, the family
 * tree still shows what its parents passed on, and breeding a fast line is
 * still work. What changes is only what the animal in the world can be made to
 * do with the result. That separation is the whole design: the genome is a
 * model of inheritance and should not be bent to make the game playable, and
 * the attribute is the game and should not be allowed to become unplayable.
 *
 * <h2>Why vanilla's slowest horse</h2>
 * The brief was "just barely fast enough to outrun a zombie", and the honest
 * way to hit that is not to read the zombie. A zombie's
 * {@code MOVEMENT_SPEED} is {@code 0.23} and a horse's is
 * {@code 0.1125}-{@code 0.3375}, and those two numbers are <b>not on the same
 * scale</b>: a ridden horse travels on the player's movement model (its
 * {@code 0.1125} is about 4.8 blocks a second, next to a walking player's
 * {@code 0.1} at 4.3), while a chasing zombie travels on the mob model, where
 * {@code 0.23} is a shamble. Clamping a horse to a zombie's raw {@code 0.23}
 * would floor it <i>above</i> the average vanilla horse, which is the opposite
 * of a floor.
 *
 * <p>So the reference is the slowest horse <b>Minecraft itself</b> will ever
 * hand you: {@code AbstractHorse.generateSpeed} with every roll at zero. It is
 * a number with a meaning - the bottom of what the game itself calls a
 * horse - it sits below this mod's own wild-type
 * ({@link HorseTraits#BASE_SPEED}) so the whole natural range still varies, and
 * a horse at it comfortably leaves a zombie behind. Everything the mod's
 * genetics can do <i>below</i> that point is the part that was never playable.
 *
 * <p>Read rather than written down, so it cannot go stale against a change to
 * vanilla's own formula - which is exactly the kind of number that moves. That
 * is what the access transformer on {@code generateSpeed} is for.
 *
 * <p><b>Retuning</b> is one expression in {@link #floor()}. If a floored horse
 * turns out not to actually escape a zombie, multiply it up; the in-game check
 * is on {@code wiki/verification.html} and it is literally "race one".
 *
 * <p>Kept on the Minecraft side and off {@code common/} for the same reason
 * {@link HorsePrices} and {@code RarityItems} are: it is a playability decision
 * about this game, not a fact about a horse's biology. The browser designer
 * shows the genetic number, and should - it is showing genetics.
 */
public final class HorseSpeedFloor {

    private HorseSpeedFloor() {
    }

    /**
     * The slowest a horse is allowed to actually move: vanilla's own slowest
     * horse, {@code generateSpeed} with all three rolls at zero.
     *
     * <p>Computed rather than cached. It is asked once per horse per trait
     * application - a spawn, a birth, a reload - and never in a tick loop, so
     * three multiplications are cheaper than a staleness bug.
     */
    public static double floor() {
        return AbstractHorse.generateSpeed(() -> 0.0);
    }

    /** The genetic speed, or the floor, whichever is faster. */
    public static double clamp(double geneticSpeed) {
        return Math.max(geneticSpeed, floor());
    }
}
