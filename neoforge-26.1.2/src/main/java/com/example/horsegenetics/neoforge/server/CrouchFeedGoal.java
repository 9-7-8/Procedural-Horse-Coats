package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.HorseDiet;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * <b>Taming by hand.</b> Crouch, hold out something the horse eats, and keep
 * still and looking at it: it comes over, eats out of your hand, and each
 * mouthful is one roll at bonding - the same roll as being thrown off its back.
 *
 * <h2>Why it is worth having beside riding</h2>
 * Vanilla taming is "climb on, get thrown off, climb on again", which is a fine
 * mechanic and a strange first impression of an animal this mod otherwise asks
 * you to look after. This is the patient route to the same place: no faster on
 * average - the roll is vanilla's, and each feed costs an item the way each
 * mount costs a fall - just quieter, and it reads as approaching an animal
 * rather than wrestling one.
 *
 * <h2>The four conditions, and why each is checked every tick</h2>
 * <ol>
 *   <li><b>Crouched.</b> The gesture, and the thing that keeps this from firing
 *       every time somebody walks past a wild herd holding wheat.</li>
 *   <li><b>Holding food this horse eats.</b> Which is a genetic question, not a
 *       vanilla one - a carnivore is not coming over for a carrot, and a
 *       dhampir is not coming over at all. See {@link #tempting}.</li>
 *   <li><b>Looking at it.</b> Picks <i>which</i> horse out of a herd, without
 *       needing a click.</li>
 *   <li><b>Still.</b> Checked against where the player was standing when the
 *       horse set off ({@link #anchor}), because "it approaches while you hold
 *       your nerve" is the whole of the interaction. Walking at it ends the
 *       approach; a little shuffling does not.</li>
 * </ol>
 *
 * <p>All four are re-tested in {@link #canContinueToUse()} rather than only at
 * the start, so breaking the pose stops the horse mid-walk, which is the
 * feedback that teaches the mechanic.
 */
public final class CrouchFeedGoal extends Goal {

    /** How far off a player can start tempting a horse. */
    private static final double SEARCH_RADIUS = 12.0;

    /** Close enough to eat out of a hand. */
    private static final double EAT_RANGE_SQR = 5.0;

    /** Ticks at the hand before a mouthful is taken - long enough to be a moment, not a wait. */
    private static final int EAT_TICKS = 45;

    /** Deliberately below a walk: an animal deciding to trust you does not trot. */
    private static final double APPROACH_SPEED = 0.75;

    /** How far off centre the player's gaze may be, as a dot product - about 25 degrees. */
    private static final double LOOK_DOT = 0.90;

    /** How far the player may drift from where they stood and still count as still. */
    private static final double STILL_TOLERANCE_SQR = 4.0;

    /** Vanilla's consolation for a failed taming attempt, and the same here. */
    private static final int TEMPER_PER_FEED = 5;

    private final Horse horse;
    private Player player;
    private Vec3 anchor;
    private int chewing;

    public CrouchFeedGoal(Horse horse) {
        this.horse = horse;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (horse.isTamed() || horse.isBaby() || horse.isVehicle()) {
            // A foal is left out for the same reason vanilla leaves it out of
            // riding: it is too young to be anybody's, and letting food do what
            // a saddle cannot would be a quiet inconsistency.
            return false;
        }
        Player found = horse.level().getNearestPlayer(horse, SEARCH_RADIUS);
        if (found == null || !qualifies(found)) {
            return false;
        }
        player = found;
        anchor = found.position();
        chewing = 0;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return !horse.isTamed() && !horse.isVehicle()
                && player != null && qualifies(player)
                && player.distanceToSqr(anchor) <= STILL_TOLERANCE_SQR;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        horse.getNavigation().stop();
        player = null;
        anchor = null;
        chewing = 0;
    }

    @Override
    public void tick() {
        horse.getLookControl().setLookAt(player, 30.0F, 30.0F);
        if (horse.distanceToSqr(player) > EAT_RANGE_SQR) {
            chewing = 0;
            horse.getNavigation().moveTo(player, APPROACH_SPEED);
            return;
        }

        // At the hand. Stand, and take a mouthful on the beat.
        horse.getNavigation().stop();
        if (++chewing < EAT_TICKS) {
            return;
        }
        chewing = 0;
        eat();
    }

    /** One mouthful: the item goes, the particles fly, and the horse rolls for it. */
    private void eat() {
        ItemStack food = heldFood(player);
        if (food.isEmpty() || !(horse.level() instanceof ServerLevel level)) {
            return;
        }

        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, food.getItem()),
                horse.getX(), horse.getY() + horse.getBbHeight() * 0.75, horse.getZ(),
                8, 0.2, 0.15, 0.2, 0.05);
        level.playSound(null, horse.getX(), horse.getY(), horse.getZ(),
                SoundEvents.HORSE_EAT, SoundSource.NEUTRAL, 0.8F, 1.0F);
        if (!player.getAbilities().instabuild) {
            food.shrink(1);
        }

        // Vanilla's own roll, unchanged: nextInt(maxTemper) < temper, and a
        // failure nudges the temper up so patience still pays. Matching it
        // rather than inventing a curve is the point - this is a second door
        // into taming, not an easier one.
        if (horse.getRandom().nextInt(horse.getMaxTemper()) < horse.getTemper()) {
            horse.tameWithName(player);
            level.broadcastEntityEvent(horse, (byte) 7); // hearts
        } else {
            horse.modifyTemper(TEMPER_PER_FEED);
            level.broadcastEntityEvent(horse, (byte) 6); // smoke
        }
    }

    /** Crouched, looking this way, and holding something this horse would eat. */
    private boolean qualifies(Player candidate) {
        if (!candidate.isAlive() || !candidate.isCrouching() || candidate.isSpectator()) {
            return false;
        }
        if (heldFood(candidate).isEmpty()) {
            return false;
        }
        if (candidate.distanceToSqr(horse) > SEARCH_RADIUS * SEARCH_RADIUS) {
            return false;
        }
        return looksAt(candidate);
    }

    /** Whichever hand has food in it, so the off-hand works too. */
    private ItemStack heldFood(Player candidate) {
        ItemStack main = candidate.getMainHandItem();
        if (tempting(main)) {
            return main;
        }
        ItemStack off = candidate.getOffhandItem();
        return tempting(off) ? off : ItemStack.EMPTY;
    }

    /**
     * Food <i>this</i> horse eats.
     *
     * <p>Split the way {@link HorseDietHandler} splits it, and for the same
     * reason: an ordinary horse is vanilla's business, and a horse with a diet
     * locus has its own list. A dhampir eats nothing, which falls out of this
     * rather than needing saying - it is special, and nothing is accepted.
     */
    private boolean tempting(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        HorseDiet diet = HorseDietHandler.dietOf(horse);
        return diet.isSpecial() ? DietFoods.accepts(diet, stack) : horse.isFood(stack);
    }

    /** Is the player's gaze inside a cone pointing at this horse? */
    private boolean looksAt(Player candidate) {
        Vec3 look = candidate.getViewVector(1.0F).normalize();
        Vec3 toHorse = horse.position()
                .add(0.0, horse.getBbHeight() * 0.5, 0.0)
                .subtract(candidate.getEyePosition())
                .normalize();
        return look.dot(toHorse) >= LOOK_DOT;
    }
}
