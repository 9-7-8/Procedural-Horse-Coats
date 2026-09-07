package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.trait.HorseTraits;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

/**
 * <b>No horse in this mod moves slower than a cow.</b>
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
 * <h2>Why a cow</h2>
 * Owner's call, and a good reference: a cow is the animal everybody has an
 * intuition for the pace of, and "slower than the cow standing next to it" is
 * the point at which a horse stops reading as a slow horse and starts reading
 * as a broken one. A horse that resolves to {@code 0.02} is roughly a tenth of
 * the slowest vanilla horse and is, in play, a statue you can sit on.
 *
 * <p>The number is <b>read from the cow</b> rather than written down here, so
 * it cannot go stale against a Minecraft change - or against another mod that
 * retunes cows, which is a case where following along is the right answer.
 *
 * <p>Worth knowing what that number is in context, because it is not a small
 * floor: a cow is {@code 0.2}, this mod's all-wild-type horse is
 * {@link HorseTraits#BASE_SPEED} ({@code 0.1875}), and vanilla rolls horses
 * between {@code 0.1125} and {@code 0.3375}. So the floor sits <i>above</i> the
 * average horse, and every horse in roughly the slower half of the range is
 * pulled up onto it. That is a deliberate trade of variety at the bottom of the
 * range for never breeding an animal you cannot ride; retuning it is one
 * expression in {@link #floor()} - a fraction of a cow, or vanilla's own
 * {@code 0.1125} minimum, if the flattening turns out to matter more than the
 * snails did.
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
     * The slowest a horse is allowed to actually move: a cow's walking speed.
     *
     * <p>Looked up rather than cached. It is asked once per horse per trait
     * application - a spawn, a birth, a reload - and never in a tick loop, so a
     * map lookup is cheaper than a staleness bug.
     */
    public static double floor() {
        AttributeSupplier cow = DefaultAttributes.getSupplier(EntityType.COW);
        return cow != null && cow.hasAttribute(Attributes.MOVEMENT_SPEED)
                ? cow.getValue(Attributes.MOVEMENT_SPEED)
                : Double.NEGATIVE_INFINITY; // no cow to measure: clamp nothing
    }

    /** The genetic speed, or the floor, whichever is faster. */
    public static double clamp(double geneticSpeed) {
        return Math.max(geneticSpeed, floor());
    }
}
