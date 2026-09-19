package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.cart.CartDraft;
import com.example.horsegenetics.common.cart.CartKind;
import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;

/**
 * <b>The bridge between a live animal and the draught model.</b>
 *
 * <p>{@link CartDraft} is pure and lives in {@code common/}: it takes two
 * doubles and a load and knows nothing about entities. This class is the other
 * half - it finds those two doubles on a real animal, keeps the per-horse
 * haulage tally, and is the <b>public surface other mods integrate against</b>.
 *
 * <h2>Animals that are not our horses</h2>
 * Every method here takes any {@link LivingEntity}, because a cart does not
 * care what is hitched to it and a pack of other mods' mounts may be. Anything
 * without one of this mod's genomes is treated as an ordinary horse -
 * {@link HorseTraits#BASE_PULL} - rather than as a weakling. A donkey is not
 * feeble for want of a genotype, and a mod that adds a mount should not have it
 * silently crippled by ours.
 *
 * @see CartDraft the model itself, and the reasoning behind its shape
 */
public final class HorseDraft {

    private HorseDraft() {
    }

    /**
     * This animal's pulling ability on the 1-10 score, or
     * {@link HorseTraits#BASE_PULL} if it has no genome of ours.
     */
    public static double pullOf(final LivingEntity entity) {
        if (entity instanceof Horse horse) {
            final var record = HorseRecords.of(horse);
            if (record != null && record.hasGenome()) {
                return HorseRecords.traitsOf(horse).pull();
            }
        }
        return HorseTraits.BASE_PULL;
    }

    /**
     * This animal's <b>unhitched</b> movement speed - the attribute's base
     * value, deliberately not its current one.
     *
     * <p>A hitched horse already carries the draught modifier, so reading the
     * current value and feeding it back into the model would compound the
     * penalty every time anything asked.
     */
    public static double speedOf(final LivingEntity entity) {
        final AttributeInstance attr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        return attr == null ? HorseTraits.BASE_SPEED : attr.getBaseValue();
    }

    /**
     * The fraction of its own speed this animal keeps while pulling that
     * vehicle <b>empty</b>: {@code 1.0} unhitched, and never zero.
     *
     * <p>Empty because a {@link CartKind} is a kind, not a cart - there is
     * nothing here to ask how full it is. A caller holding a real vehicle should
     * pass {@code AbstractDrawnEntity.currentLoad()} to
     * {@link CartDraft#retention} instead; see {@link CartDraft#loaded}.
     */
    public static double retention(final LivingEntity entity, final CartKind kind) {
        return CartDraft.retention(pullOf(entity), speedOf(entity), kind.load());
    }

    /**
     * <b>How hard this animal drives a machine</b>, as a multiple of an ordinary
     * horse: {@code 1.0} is the baseline, above it is better.
     *
     * <p>This is the number a mod that makes a horse turn something should
     * multiply its work rate by. It blends pulling ability with speed rather
     * than reading pull alone, so that breeding a draft horse does not mean
     * abandoning every other stat - see {@link CartDraft#workRate}.
     *
     * <p>Not clamped, and deliberately so: a caller knows its own machine's
     * sensible bounds better than this does.
     */
    public static double workRate(final LivingEntity entity) {
        return CartDraft.workRate(pullOf(entity), speedOf(entity));
    }

    /** Whether this animal's back takes a second rider. */
    public static boolean carriesTwoRiders(final LivingEntity entity) {
        return CartDraft.carriesTwoRiders(pullOf(entity));
    }

    // ------------------------------------------------------------------
    // The haulage tally
    // ------------------------------------------------------------------

    /**
     * Add to this animal's lifetime haulage, in metres. Server side; a no-op for
     * anything that is not one of this mod's horses.
     *
     * <p>Called once per cart tick with a fraction of a metre, so it is written
     * back on every call rather than batched - the attachment is a single
     * double and the sync is a byte codec, which is cheaper than the
     * bookkeeping a batch would need.
     */
    public static void addHauled(final Entity entity, final double metres) {
        if (metres <= 0.0 || !(entity instanceof Horse horse) || horse.level().isClientSide()) {
            return;
        }
        horse.setData(ModAttachments.CART_METRES.get(), hauled(horse) + metres);
    }

    /**
     * How far this horse has hauled a cart in its life, in metres. Zero for
     * anything that has never been hitched, and for anything that is not a
     * horse.
     */
    public static double hauled(final Entity entity) {
        if (!(entity instanceof Horse horse)) {
            return 0.0;
        }
        return horse.getData(ModAttachments.CART_METRES.get());
    }
}
