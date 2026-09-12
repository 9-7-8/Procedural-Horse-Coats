package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.HorseDiet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.List;

/**
 * <b>A horse drifts toward anybody holding something it can eat - and breaks
 * into a run for the one thing it loves.</b>
 *
 * <h2>Two speeds, and the gap between them is the whole point</h2>
 * Ordinary food it will eat gets {@value #WALK_SPEED}, which is a slow amble:
 * the horse notices you, comes over in its own time, and is easy to walk away
 * from. Its <b>favourite</b> food - the one item its
 * {@link FoodPreferenceHandler#favouriteOf food-preference locus} names - gets
 * {@value #RUN_SPEED} and a wider notice radius, so it picks its head up from
 * across the paddock and <i>runs</i>.
 *
 * <p>That contrast is the feature. A player holding wheat learns that horses
 * are mildly interested in wheat; a player holding the right thing in front of
 * the right horse gets an unmistakable reaction, and that reaction is the only
 * way the favourite locus is visible before you have fed it. The gene has been
 * in the mod without any way to <i>discover</i> it: you had to already know
 * which item to try. Now the horse tells you.
 *
 * <h2>Deliberately less insistent than tempting with a breeding carrot</h2>
 * This is a drift, not a tractor beam. It gives up at
 * {@value #STOP_DISTANCE} blocks rather than pressing into the player, it
 * re-checks the held item every tick so lowering your hand ends it immediately,
 * and at walking speed a player who keeps moving simply leaves. A horse should
 * be interested in food, not glued to it.
 *
 * <h2>What counts as edible is a genetic question</h2>
 * The same test {@link CrouchFeedGoal} uses, and for the same reason: a
 * carnivore is not coming over for a carrot, and a special-diet horse answers
 * only to what its diet locus accepts. A favourite overrides that - the
 * preference locus beats the diet locus everywhere else
 * ({@link FoodPreferenceHandler}), so it does here too, and a meat-eater with a
 * sweet tooth for apples will still run at one.
 *
 * <h2>Who it does not apply to</h2>
 * A ridden, leashed or led horse ignores food, because all three mean somebody
 * is already deciding where it goes. Foals are <i>included</i>, unlike
 * {@link CrouchFeedGoal} - that goal excludes them because taming a foal would
 * be a mechanical inconsistency, whereas a foal trotting over to food is just a
 * foal.
 */
public final class FoodTemptGoal extends Goal {

    /** How far off a horse notices ordinary food. */
    private static final double NOTICE_RADIUS = 10.0;

    /** How far off it notices its favourite - it can smell that one coming. */
    private static final double FAVOURITE_NOTICE_RADIUS = 20.0;

    /** An amble. Well under a walk, and easy to walk away from. */
    private static final double WALK_SPEED = 0.65;

    /** A run, and the only time a horse does this for food. */
    private static final double RUN_SPEED = 1.45;

    /** Close enough. It stops here rather than pressing into the player. */
    private static final double STOP_DISTANCE = 2.5;

    private final Horse horse;
    private Player player;

    /** Set when the goal starts, so the speed cannot change mid-approach. */
    private boolean running;

    /**
     * <b>The horse's diet and its favourite, resolved once and kept.</b>
     *
     * <p>This is not an optimisation, it is the difference between the goal
     * being usable and not. {@link #canUse()} runs <i>every tick, for every
     * horse</i>, and both of the underlying lookups parse the horse's genetic
     * code out of its record - {@code HorseDietHandler.dietOf} parses the
     * <b>epigenome</b> as well, which is some eight thousand characters
     * (<a href="known-gaps.html#gap-66">gap 66</a>). A paddock of thirty horses
     * would have been doing sixty full genome parses a tick, twice over for two
     * hands, for as long as anybody stood near them holding anything.
     *
     * <p>Both facts are fixed for the life of the horse - the genotype and the
     * epigenome are assigned at birth and never move - so one resolve is
     * correct as well as cheap. It is <b>lazy</b> rather than done in the
     * constructor because the goal is added on entity join and the record is
     * filled on the founding tick, so at construction there is often nothing to
     * read yet; until there is, {@link #resolved} stays false and it tries
     * again next tick.
     */
    private boolean resolved;
    private String favourite;
    private HorseDiet diet = HorseDiet.NORMAL;

    public FoodTemptGoal(Horse horse) {
        this.horse = horse;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (busy()) {
            return false;
        }
        // The favourite is noticed from further, so look out to the wider radius
        // and let the per-player test decide which range actually applies.
        Player found = horse.level().getNearestPlayer(horse, FAVOURITE_NOTICE_RADIUS);
        if (found == null) {
            return false;
        }
        Held held = heldBy(found);
        if (held == Held.NOTHING) {
            return false;
        }
        boolean favourite = held == Held.FAVOURITE;
        double radius = favourite ? FAVOURITE_NOTICE_RADIUS : NOTICE_RADIUS;
        if (horse.distanceToSqr(found) > radius * radius) {
            return false;
        }
        player = found;
        running = favourite;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (player == null || busy() || !player.isAlive()) {
            return false;
        }
        // Re-tested every tick, so putting the food away stops the horse where
        // it stands rather than letting it finish the walk.
        Held held = heldBy(player);
        if (held == Held.NOTHING) {
            return false;
        }
        double radius = running ? FAVOURITE_NOTICE_RADIUS : NOTICE_RADIUS;
        return horse.distanceToSqr(player) <= radius * radius;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        horse.getNavigation().stop();
        player = null;
        running = false;
    }

    @Override
    public void tick() {
        horse.getLookControl().setLookAt(player, 30.0F, 30.0F);
        if (horse.distanceToSqr(player) <= STOP_DISTANCE * STOP_DISTANCE) {
            horse.getNavigation().stop();
            return;
        }
        horse.getNavigation().moveTo(player, running ? RUN_SPEED : WALK_SPEED);
    }

    /** Somebody else is already deciding where this horse goes. */
    private boolean busy() {
        return horse.isVehicle() || horse.isLeashed() || horse.isPassenger();
    }

    private enum Held { NOTHING, EDIBLE, FAVOURITE }

    /**
     * What this player is offering, from this horse's point of view. Both hands
     * count, and the favourite wins over the off hand holding mere wheat.
     */
    private Held heldBy(Player candidate) {
        Held best = Held.NOTHING;
        for (ItemStack stack : List.of(candidate.getMainHandItem(), candidate.getOffhandItem())) {
            Held held = offering(stack);
            if (held == Held.FAVOURITE) {
                return Held.FAVOURITE;
            }
            if (held == Held.EDIBLE) {
                best = Held.EDIBLE;
            }
        }
        return best;
    }

    private Held offering(ItemStack stack) {
        if (stack.isEmpty()) {
            return Held.NOTHING;
        }
        resolveGenetics();
        if (favourite != null
                && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(favourite)) {
            // The preference locus beats the diet locus here exactly as it does
            // on the interaction, so a carnivore with a taste for apples runs.
            return Held.FAVOURITE;
        }
        boolean edible = diet.isSpecial() ? DietFoods.accepts(diet, stack) : horse.isFood(stack);
        return edible ? Held.EDIBLE : Held.NOTHING;
    }

    /** Read the diet and the favourite once, as soon as this horse has a record. */
    private void resolveGenetics() {
        if (resolved || !HorseRecords.hasRealRecord(horse)) {
            return;
        }
        favourite = FoodPreferenceHandler.favouriteOf(horse);
        diet = HorseDietHandler.dietOf(horse);
        resolved = true;
    }
}
