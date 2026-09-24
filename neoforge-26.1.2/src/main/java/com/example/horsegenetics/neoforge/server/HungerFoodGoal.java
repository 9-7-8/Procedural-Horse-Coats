package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.care.Hunger;
import com.example.horsegenetics.common.genetics.Diet;
import com.example.horsegenetics.common.genetics.HorseDiet;
import com.example.horsegenetics.common.progress.ProgressTask;
import com.example.horsegenetics.neoforge.compat.HayBales;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <b>A hungry horse goes and eats</b> ({@link Hunger}, owner 2026-09-14).
 *
 * <p>Below {@link Hunger#HUNGRY} it looks round for the best food its diet allows, walks to
 * it and eats, one mouthful at a time, until it reaches {@link Hunger#SATED}. Between that and
 * {@link Hunger#GRAZE_BELOW} it only grazes what it is standing on, now and then. What it looks
 * for, best first, is {@link Hunger.Food}'s order (and a grazing horse takes a dropped
 * mouthful in reach before the grass, which is the only rung it does not walk to):
 * <ol>
 *   <li>its favourite food, lying on the ground;</li>
 *   <li>any dropped food it can eat ({@link DietFoods#acceptsFromGround}) - which for an
 *       ordinary horse is <b>wider</b> than what it takes from a hand, and is the item
 *       form of the rungs below it, so it does not cross a pen for a planted potato and
 *       then ignore one lying at its feet;</li>
 *   <li>cake, then hay, then crops, pumpkins, melons and sugar cane;</li>
 *   <li>grass, then moss, then mushrooms, then flowers.</li>
 * </ol>
 * The nearest thing on the best rung wins. <b>Everything eaten is used up</b> (owner): a cake,
 * a hay bale, a crop, a mushroom or a flower is gone, and grass and moss turn to dirt the way
 * a sheep leaves them. Block eating asks {@code EventHooks.canEntityGrief} first, as the
 * sheep's {@code EatBlockGoal} does, and a horse still gets its mouthful when mob griefing is
 * off - the block just stays.
 *
 * <h2>Diets</h2>
 * An ordinary horse and an eat-anything horse take every rung. A narrow diet takes a block
 * rung only if {@link DietFoods} accepts that block's item (a wheat-eater's hay and wheat, a
 * vegetable-eater's carrots and pumpkins, a cake-eater's cake), and never grass, moss,
 * mushrooms or flowers. <b>A carnivore</b> (owner: "It's carnivores that hunt") that finds
 * nothing it can eat hunts a passive animal whose drops it eats, and the drops are then just
 * dropped food. A fish-eater hunts fish. A blood-drinker's meal is its bite
 * ({@link BloodHuntGoal}), and a horse that eats nothing never looks.
 *
 * <h2>Cost</h2>
 * THE FIRST BUILD SLOWED THE SERVER BY A THIRD (2026-09-14, 22:12): 50 ms a tick became 64-71
 * from the first census, before any horse was hungry. {@code canUse} runs for every horse every
 * tick or two, and it asked {@code HorseDietHandler.dietOf} first - which parses the horse's whole
 * genotype and epigenome - so a yard of 280 horses parsed a genome tens of thousands of times a
 * second to decide to do nothing. Now hunger is read first, and a fed horse leaves {@code canUse}
 * on one attachment read; the diet is resolved once per horse, as {@link BloodHuntGoal} does.
 * The same build also grazed every horse below 90, which kept every horse topped up at 96, never
 * hungry, and turned 1,547 grass blocks to dirt in 26 minutes with a log line each. Grazing now
 * starts below {@link Hunger#GRAZE_BELOW}, about once every two minutes a horse, and is counted
 * into one line a minute.
 *
 * <h2>Over the fence</h2>
 * A fence is two separate questions, and the horse has to fail both to be stopped by one.
 * <ul>
 *   <li><b>It only goes after food it can walk to.</b> A pasture beside a farm used to pin
 *       every horse against the fence: the crops were the best rung in sight, the path to
 *       them ended at the rail, and the horse leaned on it. {@link #search} asks
 *       {@code canReach} before it commits, keeps the nearest candidate of <i>each</i> rung
 *       so an unreachable farm falls through to the grass in the pen, and {@link #tick}
 *       drops a pursuit the moment a gate shuts on it.</li>
 *   <li><b>It only eats what it can get its mouth to.</b> That check passes a farm the long
 *       way round through a gate - as it should - but {@link #REACH_SQ} is a plain distance,
 *       so a horse stood at the rail was already within it and ate the crop through the
 *       fence instead of walking. {@link #exposed} is the second half: a horse a barrier
 *       away from its target keeps walking rather than eating where it stands.</li>
 * </ul>
 *
 * <p>UNVERIFIED: that {@code BlockTags.CROPS} and {@code BlockTags.SMALL_FLOWERS} hold what
 * their names say in 26.1.2 (both compile; contents not read); that {@link #REACH_ACCURACY}
 * of 1 separates a hay bale the horse stands beside from a crop one fence away - the manhattan
 * reading of vanilla's own {@code moveTo} accuracy, not measured in game; and that a fence's
 * collision shape stands 1.5 blocks in 26.1.2, which is what puts it above a grazing horse's
 * eye line and is the whole of why {@link #exposed} sees it.
 */
public final class HungerFoodGoal extends Goal {

    private static final int SEARCH_RADIUS = 10;
    private static final int SEARCH_DOWN = 2;
    private static final int SEARCH_UP = 2;
    private static final int SEARCH_INTERVAL = 40;
    private static final double REACH_SQ = 6.25;        // 2.5 blocks, centre to centre
    private static final double SPEED = 1.0;
    private static final int REPATH_INTERVAL = 20;
    private static final int MAX_PURSUIT = 400;
    private static final int IGNORE_TICKS = 1_200;
    /**
     * How close the path has to get to count as reached - the same accuracy vanilla's own
     * {@code moveTo(x, y, z, speed)} passes, so the check below and the move that follows it
     * ask one question. Manhattan distance in blocks: 1 lets the horse stand beside a hay
     * bale or on a grass block, and is still 2 short of a crop on the far side of a fence.
     */
    private static final int REACH_ACCURACY = 1;
    /** Path tests one search may spend before giving up and waiting for the next. */
    private static final int MAX_REACH_TESTS = 3;
    private static final int RUNGS = Hunger.Food.values().length;
    /** A grazing horse takes a mouthful about once in this many checks - roughly every two minutes. */
    private static final int GRAZE_CHANCE = 1_200;
    /** How far a grazing horse takes a dropped mouthful from without walking to it. */
    private static final double GRAZE_REACH = 2.0;
    private static final float HUNT_DAMAGE = 4.0F;
    private static final int HUNT_SWING = 20;

    /** Passive animals whose drops a carnivore eats: {@link DietFoods}'s raw meat. */
    private static final Set<EntityType<?>> MEAT_PREY = Set.of(
            EntityType.COW, EntityType.MOOSHROOM, EntityType.PIG, EntityType.SHEEP,
            EntityType.CHICKEN, EntityType.RABBIT);
    /** ...and a fish-eater's. */
    private static final Set<EntityType<?>> FISH_PREY = Set.of(
            EntityType.COD, EntityType.SALMON, EntityType.TROPICAL_FISH, EntityType.PUFFERFISH);

    /** Grazing mouthfuls since the last summary line, and when that line was written. */
    private static int grazed;
    private static long grazeLogged = Long.MIN_VALUE / 2;

    private final Horse horse;
    private final Map<Long, Long> ignoredUntil = new HashMap<>();

    /** Resolved once, the first time the horse has a real record - see {@link #diet}. */
    private @Nullable HorseDiet diet;
    private @Nullable ItemEntity item;
    private @Nullable BlockPos block;
    private @Nullable LivingEntity prey;
    private Hunger.Food rung;
    private int searchCooldown;
    private int repathCooldown;
    private int pursuitTicks;
    private int swingCooldown;

    public HungerFoodGoal(Horse horse) {
        this.horse = horse;
        this.searchCooldown = horse.getRandom().nextInt(SEARCH_INTERVAL);
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    // ------------------------------------------------------------------
    // Goal
    // ------------------------------------------------------------------

    @Override
    public boolean canUse() {
        // Cheapest first: one attachment read decides it for a fed horse.
        double hunger = hunger();
        if (!Hunger.seeksFood(hunger)) {
            if (Hunger.grazes(hunger) && horse.getRandom().nextInt(GRAZE_CHANCE) == 0
                    && horse.level() instanceof ServerLevel level && free()) {
                HorseDiet d = diet();
                if (d != null && eatsAtAll(d)) {
                    grazeInPlace(level, d);
                }
            }
            return false;
        }
        if (--searchCooldown > 0) {
            return false;
        }
        searchCooldown = SEARCH_INTERVAL;
        if (!(horse.level() instanceof ServerLevel level) || !free()) {
            return false;
        }
        HorseDiet d = diet();
        return d != null && eatsAtAll(d) && search(level, d);
    }

    @Override
    public boolean canContinueToUse() {
        if (!free() || pursuitTicks >= MAX_PURSUIT || !Hunger.wantsMore(hunger())) {
            return false;
        }
        if (item != null) {
            return item.isAlive();
        }
        if (prey != null) {
            return prey.isAlive();
        }
        HorseDiet d = diet();
        return block != null && d != null && horse.level() instanceof ServerLevel level
                && rungOf(level, block, d) == rung;
    }

    @Override
    public void start() {
        pursuitTicks = 0;
        repathCooldown = 0;
        swingCooldown = 0;
    }

    @Override
    public void stop() {
        if (pursuitTicks >= MAX_PURSUIT) {
            ignore(targetKey());
        }
        item = null;
        block = null;
        prey = null;
        horse.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (!(horse.level() instanceof ServerLevel level)) {
            return;
        }
        pursuitTicks++;
        Vec3 at = targetPos();
        if (at == null) {
            return;
        }
        horse.getLookControl().setLookAt(at.x, at.y, at.z);
        if (--repathCooldown <= 0) {
            repathCooldown = REPATH_INTERVAL;
            if (prey != null) {
                horse.getNavigation().moveTo(prey, SPEED);
            } else if (!horse.getNavigation().moveTo(at.x, at.y, at.z, SPEED) && pursuitTicks > REPATH_INTERVAL) {
                pursuitTicks = MAX_PURSUIT;     // no path at all: give up now and ignore it
                return;
            } else {
                // A gate can shut behind a horse that set off while it was open. moveTo takes
                // the partial path and would walk the horse into the fence and hold it there
                // until MAX_PURSUIT; the path it just built already knows it stops short.
                Path path = horse.getNavigation().getPath();
                if (path != null && !path.canReach()) {
                    pursuitTicks = MAX_PURSUIT;
                    return;
                }
            }
        }
        if (horse.distanceToSqr(at) > REACH_SQ) {
            return;
        }
        // ...and can get its mouth to it. Within 2.5 blocks is not the same as beside it:
        // see #exposed. A blocked target is not given up on - the horse keeps walking, and
        // the way round is what search already proved exists.
        if (item != null) {
            if (!horse.hasLineOfSight(item)) {
                return;
            }
            eatItem(level, item);
            item = null;
        } else if (block != null) {
            if (!exposed(block)) {
                return;
            }
            eatBlock(level, block, rung, false);
            block = null;
        } else if (prey != null && horse.hasLineOfSight(prey) && --swingCooldown <= 0) {
            swingCooldown = HUNT_SWING;
            hunt(level, prey);
        }
    }

    // ------------------------------------------------------------------
    // Finding food
    // ------------------------------------------------------------------

    /** The diet, resolved once; null until the horse has a real record. */
    private @Nullable HorseDiet diet() {
        if (diet == null && HorseRecords.hasRealRecord(horse)) {
            diet = HorseDietHandler.dietOf(horse);
        }
        return diet;
    }

    private static boolean eatsAtAll(HorseDiet d) {
        return d.diet() != Diet.NOTHING && d.diet() != Diet.BLOOD;
    }

    /**
     * The best thing to eat in reach, in {@link Hunger.Food} order and nearest within a rung.
     *
     * <p><b>In reach means walkable to</b> (owner, 2026-09-23: "all the horses are pushing
     * against the pasture edge where the fence separates them from the farm/crops"). A farm
     * over the fence is a hundred crop blocks ten blocks away, every one of them the best rung
     * on offer and none of them reachable; the horse took the nearest, got a partial path that
     * ended at the fence, leaned on it for {@link #MAX_PURSUIT}, ignored that one block and
     * started again on the block beside it. So each candidate is now asked
     * {@link #reachable(BlockPos)} before the horse commits to it, the way
     * {@code BondFollowGoal.ReachCheck} asks it of an owner behind a door.
     *
     * <p>That alone would starve the horse instead: the crop rung beats grass, so a pen of
     * grass beside a fenced farm would spend every search rejecting crops and never fall
     * through to the grass under the horse's feet. The scan therefore keeps <b>the nearest
     * candidate of every rung</b>, not one global best, and walks them best-first until one
     * is reachable - so an unreachable farm quietly demotes the horse to the grass it is
     * standing in. A rejected candidate is ignored for {@link #IGNORE_TICKS} like any other
     * dead end, which is what stops the next search picking its neighbour.
     */
    private boolean search(ServerLevel level, HorseDiet diet) {
        long now = level.getGameTime();
        if (ignoredUntil.size() > 64) {
            ignoredUntil.values().removeIf(until -> until <= now);
        }
        String favourite = horse.isBaby() ? null : FoodPreferenceHandler.favouriteOf(horse);

        ItemEntity bestItem = null;
        boolean bestIsFavourite = false;
        double bestItemDist = Double.MAX_VALUE;
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class,
                horse.getBoundingBox().inflate(SEARCH_RADIUS, SEARCH_UP, SEARCH_RADIUS),
                e -> e.isAlive() && !e.getItem().isEmpty())) {
            if (ignored(~(long) e.getId(), now)) {
                continue;
            }
            boolean fav = favourite != null
                    && favourite.equals(BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).toString());
            // The diet resolved once for this horse, not GeneAbilityHandler.eats: that
            // asks HorseDietHandler.dietOf per call, so a pen with a pile of dropped
            // items parsed one horse's genotype once an item - the cost this class was
            // rebuilt to avoid - and answered the narrower hand-feeding question anyway.
            if (!fav && !DietFoods.acceptsFromGround(diet, e.getItem())) {
                continue;
            }
            double d = horse.distanceToSqr(e);
            if ((fav && !bestIsFavourite) || (fav == bestIsFavourite && d < bestItemDist)) {
                bestItem = e;
                bestIsFavourite = fav;
                bestItemDist = d;
            }
        }
        int tests = 0;
        if (bestItem != null) {
            if (reachable(bestItem)) {
                item = bestItem;
                rung = bestIsFavourite ? Hunger.Food.FAVOURITE : Hunger.Food.DROPPED;
                return true;
            }
            ignore(~(long) bestItem.getId());
            tests++;
        }

        // The nearest of every rung, so a rung the horse cannot walk to demotes it to the
        // next one rather than ending the search - see this method's note.
        BlockPos[] nearest = new BlockPos[RUNGS];
        double[] nearestDist = new double[RUNGS];
        java.util.Arrays.fill(nearestDist, Double.MAX_VALUE);
        BlockPos origin = horse.blockPosition();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                for (int dy = -SEARCH_DOWN; dy <= SEARCH_UP; dy++) {
                    p.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    Hunger.Food r = rungOf(level, p, diet);
                    if (r == null || ignored(p.asLong(), now)) {
                        continue;
                    }
                    double d = horse.distanceToSqr(Vec3.atCenterOf(p));
                    if (d < nearestDist[r.ordinal()]) {
                        nearest[r.ordinal()] = p.immutable();
                        nearestDist[r.ordinal()] = d;
                    }
                }
            }
        }
        for (Hunger.Food r : Hunger.Food.values()) {
            BlockPos at = nearest[r.ordinal()];
            if (at == null) {
                continue;
            }
            if (tests++ >= MAX_REACH_TESTS) {
                return false;       // the rest keep until the next search, a rung poorer
            }
            if (reachable(at)) {
                block = at;
                rung = r;
                return true;
            }
            ignore(at.asLong());
        }

        if (tests < MAX_REACH_TESTS && (diet.diet() == Diet.RAW_MEAT || diet.diet() == Diet.FISH)) {
            Set<EntityType<?>> kinds = diet.diet() == Diet.FISH ? FISH_PREY : MEAT_PREY;
            LivingEntity best = null;
            double bestDist = Double.MAX_VALUE;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                    horse.getBoundingBox().inflate(SEARCH_RADIUS, SEARCH_UP, SEARCH_RADIUS),
                    e -> e.isAlive() && kinds.contains(e.getType()) && !e.isBaby()
                            && !(e instanceof AbstractHorse)
                            && !(e instanceof TamableAnimal t && t.isTame()))) {
                double d = horse.distanceToSqr(e);
                if (!ignored(~(long) e.getId(), now) && d < bestDist) {
                    best = e;
                    bestDist = d;
                }
            }
            if (best != null) {
                if (reachable(best)) {
                    prey = best;
                    rung = Hunger.Food.DROPPED;     // what it is after is the drop
                    return true;
                }
                ignore(~(long) best.getId());
            }
        }
        return false;
    }

    /**
     * "Could this horse actually walk to it?" - one real {@code createPath} call, and a path
     * that exists but stops short ({@code !canReach()}) is a fence. The same question
     * {@code BondFollowGoal.ReachCheck} asks of an owner; it needs no cache here because only
     * a hungry horse asks it, at most {@link #MAX_REACH_TESTS} times per
     * {@link #SEARCH_INTERVAL}, where the pursuit it replaces repathed every
     * {@link #REPATH_INTERVAL} ticks for up to {@link #MAX_PURSUIT}.
     */
    private boolean reachable(BlockPos pos) {
        Path path = horse.getNavigation().createPath(pos, REACH_ACCURACY);
        return path != null && path.canReach();
    }

    private boolean reachable(Entity target) {
        Path path = horse.getNavigation().createPath(target, REACH_ACCURACY);
        return path != null && path.canReach();
    }

    /**
     * <b>"Can the horse get its mouth to it?"</b> - {@link #reachable} answers the question
     * for the horse's feet, and that is not the same question (owner, 2026-09-23: "horses are
     * eating crops through a fence or wall"). A farm on the other side of a rail is normally
     * reachable: there is a gate somewhere, so {@code canReach} says yes and the horse sets
     * off. But the eating radius is {@link #REACH_SQ}, a plain distance, so a horse that
     * happens to be stood at the fence is already inside it and eats the crop where it
     * stands rather than walking the long way round. The same held for a carcass or a dropped
     * carrot over the rail, and for a carnivore's swing at a cow behind one.
     *
     * <p>This is the same clip vanilla's own {@code hasLineOfSight} makes - collider shapes,
     * fluids ignored, {@code MISS} means nothing is in the way - which is why the item and
     * prey rungs just call that. The difference is only <b>where to aim</b>: at the nearest
     * point of the block, not its centre. Centre is wrong for exactly the rung a horse eats
     * most, because a grass block sits <i>below</i> the ground the horse walks on, and a ray
     * to the middle of one five blocks away dives through the turf in between and reports a
     * barrier every time. The nearest point of the box is the top edge facing the horse - the
     * part it actually puts its head on - and the ray to it skims the surface.
     *
     * <p>A fence and a cobble wall both collide to 1.5 blocks, above a grazing horse's eye
     * line, so both stop the ray. <b>A one-block wall deliberately does not</b>: a horse can
     * put its head over that, and can jump it, so the pathfinder was going to let it walk
     * there anyway.
     */
    private boolean exposed(BlockPos pos) {
        Vec3 eye = horse.getEyePosition();
        // The closest point of the block's box to the eye, clamped axis by axis.
        Vec3 aim = new Vec3(
                Mth.clamp(eye.x, pos.getX(), pos.getX() + 1.0),
                Mth.clamp(eye.y, pos.getY(), pos.getY() + 1.0),
                Mth.clamp(eye.z, pos.getZ(), pos.getZ() + 1.0));
        HitResult hit = horse.level().clip(new ClipContext(
                eye, aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, horse));
        return hit.getType() == HitResult.Type.MISS
                || (hit instanceof net.minecraft.world.phys.BlockHitResult b
                        && b.getBlockPos().equals(pos));
    }

    /**
     * What this block is to this horse, or null if it is not food it may eat. Grass and moss
     * blocks count only with air above, which is where a horse eats them from.
     */
    private static @Nullable Hunger.Food rungOf(ServerLevel level, BlockPos pos, HorseDiet diet) {
        BlockState st = level.getBlockState(pos);
        Hunger.Food food;
        Item form = null;
        if (st.getBlock() instanceof CakeBlock || st.getBlock() instanceof CandleCakeBlock) {
            food = Hunger.Food.CAKE;
            form = Items.CAKE;
        } else if (HayBales.isBale(st)) {
            // Vanilla's hay block and any other mod's bale - HayBales.BALE_BLOCKS.
            // The item form is read off the block rather than named, so a narrow
            // diet judges a modded bale by the same item DietFoods was asked about.
            food = Hunger.Food.HAY;
            form = st.getBlock().asItem();
        } else if (st.is(BlockTags.CROPS) || st.is(Blocks.PUMPKIN) || st.is(Blocks.MELON) || st.is(Blocks.SUGAR_CANE)) {
            food = Hunger.Food.CROP;
            form = cropItem(st);
        } else if (st.is(Blocks.SHORT_GRASS) || st.is(Blocks.TALL_GRASS) || st.is(Blocks.FERN)
                || st.is(Blocks.LARGE_FERN)) {
            food = Hunger.Food.GRASS;
        } else if (st.is(Blocks.GRASS_BLOCK)) {
            if (!level.getBlockState(pos.above()).isAir()) {
                return null;
            }
            food = Hunger.Food.GRASS;
        } else if (st.is(Blocks.MOSS_BLOCK)) {
            if (!level.getBlockState(pos.above()).isAir()) {
                return null;
            }
            food = Hunger.Food.MOSS;
        } else if (st.is(Blocks.RED_MUSHROOM) || st.is(Blocks.BROWN_MUSHROOM)) {
            food = Hunger.Food.MUSHROOM;
        } else if (st.is(BlockTags.SMALL_FLOWERS)) {
            food = Hunger.Food.FLOWER;
        } else {
            return null;
        }
        Diet d = diet.diet();
        if (d == Diet.NORMAL || d == Diet.ANYTHING) {
            return food;
        }
        return form != null && DietFoods.accepts(diet, new ItemStack(form)) ? food : null;
    }

    /** The item a crop block stands for, for a narrow diet to accept; null for stems and the rest. */
    private static @Nullable Item cropItem(BlockState st) {
        if (st.is(Blocks.WHEAT)) return Items.WHEAT;
        if (st.is(Blocks.CARROTS)) return Items.CARROT;
        if (st.is(Blocks.POTATOES)) return Items.POTATO;
        if (st.is(Blocks.BEETROOTS)) return Items.BEETROOT;
        if (st.is(Blocks.PUMPKIN)) return Items.PUMPKIN;
        if (st.is(Blocks.MELON)) return Items.MELON_SLICE;
        if (st.is(Blocks.SUGAR_CANE)) return Items.SUGAR_CANE;
        return null;
    }

    // ------------------------------------------------------------------
    // Eating
    // ------------------------------------------------------------------

    /**
     * A horse that is not hungry but could eat: food lying at its feet, the tuft it stands
     * in, or the grass under it. The dropped mouthful is here as well as in {@link #search}
     * because the owner's ask was "near them", not "when starving" (2026-09-23) - a horse
     * that has eaten into its reserve and is standing on a dropped carrot should take it
     * rather than wait to fall below {@link Hunger#HUNGRY}. It costs one entity query on the
     * same one-in-{@link #GRAZE_CHANCE} roll the grass mouthful already pays for.
     */
    private void grazeInPlace(ServerLevel level, HorseDiet diet) {
        if (grazeDropped(level, diet)) {
            return;
        }
        BlockPos at = horse.blockPosition();
        if (rungOf(level, at, diet) == Hunger.Food.GRASS) {
            eatBlock(level, at, Hunger.Food.GRASS, true);
        } else if (rungOf(level, at.below(), diet) == Hunger.Food.GRASS) {
            eatBlock(level, at.below(), Hunger.Food.GRASS, true);
        }
    }

    /** Food within {@link #GRAZE_REACH}, eaten where the horse stands; false if there is none. */
    private boolean grazeDropped(ServerLevel level, HorseDiet diet) {
        String favourite = horse.isBaby() ? null : FoodPreferenceHandler.favouriteOf(horse);
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class,
                horse.getBoundingBox().inflate(GRAZE_REACH, 1.0, GRAZE_REACH),
                e -> e.isAlive() && !e.getItem().isEmpty())) {
            boolean fav = favourite != null
                    && favourite.equals(BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).toString());
            if (!fav && !DietFoods.acceptsFromGround(diet, e.getItem())) {
                continue;
            }
            if (!horse.hasLineOfSight(e)) {
                continue;       // two blocks away through a fence is not "at its feet"
            }
            rung = fav ? Hunger.Food.FAVOURITE : Hunger.Food.DROPPED;
            eatItem(level, e);
            return true;
        }
        return false;
    }

    private void eatItem(ServerLevel level, ItemEntity e) {
        ItemStack left = e.getItem().copy();
        Item what = left.getItem();
        left.shrink(1);
        if (left.isEmpty()) {
            e.discard();
        } else {
            e.setItem(left);
        }
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, what),
                horse.getX(), horse.getY() + horse.getBbHeight() * 0.75, horse.getZ(),
                8, 0.2, 0.15, 0.2, 0.05);
        fed(level, rung, BuiltInRegistries.ITEM.getKey(what).toString(), e.blockPosition(), false);
    }

    private void eatBlock(ServerLevel level, BlockPos pos, Hunger.Food food, boolean grazing) {
        BlockState st = level.getBlockState(pos);
        String what = BuiltInRegistries.BLOCK.getKey(st.getBlock()).toString();
        if (net.neoforged.neoforge.event.EventHooks.canEntityGrief(level, horse)) {
            if (st.is(Blocks.GRASS_BLOCK) || st.is(Blocks.MOSS_BLOCK)) {
                // As a sheep leaves it: the grass is eaten and the dirt stays.
                level.levelEvent(2001, pos, Block.getId(st));
                level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 2);
            } else {
                level.destroyBlock(pos, false);
            }
        }
        fed(level, food, what, pos, grazing);
    }

    private void fed(ServerLevel level, Hunger.Food food, String what, BlockPos pos, boolean grazing) {
        double before = hunger();
        double after = Hunger.eat(before, food);
        horse.setData(ModAttachments.HUNGER.get(), after);
        // A horse feeding itself - watched, since nobody did it.
        HorseProgress.completeForWatcher(horse, ProgressTask.WILD_GRAZE);
        // The husbandry half of the same mouthful: food into a horse that
        // wanted it. Credited to the owner, who arranged what it found.
        if (Hunger.seeksFood(before) && horse.getOwner() instanceof Player keeper) {
            HorseProgress.complete(keeper, ProgressTask.FEED_HUNGRY_HORSE);
        }
        level.playSound(null, horse.getX(), horse.getY(), horse.getZ(),
                SoundEvents.HORSE_EAT, SoundSource.NEUTRAL, 0.8F, 1.0F);
        if (!grazing) {
            ActionTrace.log("hunger", ActionTrace.describeShort(horse) + " ate " + what + " ("
                    + food.name().toLowerCase(java.util.Locale.ROOT) + ") at " + pos.toShortString()
                    + " - hunger now " + Math.round(after));
            return;
        }
        grazed++;
        long now = level.getGameTime();
        if (now - grazeLogged >= 1_200L) {
            ActionTrace.log("hunger", "grazing: " + grazed + " mouthful(s) of grass by horses that were not"
                    + " hungry, in the last minute");
            grazed = 0;
            grazeLogged = now;
        }
    }

    private void hunt(ServerLevel level, LivingEntity target) {
        target.hurtServer(level, level.damageSources().mobAttack(horse), HUNT_DAMAGE);
        horse.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        if (!target.isAlive()) {
            ActionTrace.log("hunger", ActionTrace.describeShort(horse) + " hunted "
                    + BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()) + " at "
                    + target.blockPosition().toShortString() + " - its drops are next");
            // The kill, rather than each of the swings on the way to it.
            HorseProgress.completeForWatcher(horse, ProgressTask.WILD_HUNT);
            prey = null;
            searchCooldown = 0;
        }
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private double hunger() {
        return horse.getData(ModAttachments.HUNGER.get());
    }

    /** Not ridden, led, fighting, targeting something, or held by a reader - as HerdGoals asks. */
    private boolean free() {
        LivingEntity target = horse.getTarget();
        return horse.isAlive() && !horse.isVehicle() && !horse.isLeashed()
                && (target == null || !target.isAlive())
                && !BandLife.inFight(horse) && !HorseInspectHold.isHeld(horse);
    }

    private @Nullable Vec3 targetPos() {
        if (item != null) {
            return item.position();
        }
        if (prey != null) {
            return prey.position();
        }
        return block == null ? null : Vec3.atCenterOf(block);
    }

    private long targetKey() {
        if (item != null) {
            return ~(long) item.getId();
        }
        if (prey != null) {
            return ~(long) prey.getId();
        }
        return block == null ? Long.MIN_VALUE : block.asLong();
    }

    private void ignore(long key) {
        if (key != Long.MIN_VALUE) {
            ignoredUntil.put(key, horse.level().getGameTime() + IGNORE_TICKS);
        }
    }

    private boolean ignored(long key, long now) {
        Long until = ignoredUntil.get(key);
        return until != null && until > now;
    }
}
