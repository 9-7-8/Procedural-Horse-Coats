package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.care.Hunger;
import com.example.horsegenetics.common.genetics.Diet;
import com.example.horsegenetics.common.genetics.HorseDiet;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
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
 * it and eats, one mouthful at a time, until it reaches {@link Hunger#SATED}. Above that it
 * only grazes what it is standing on, now and then, the way vanilla's animation already
 * pretends to. What it looks for, best first, is {@link Hunger.Food}'s order:
 * <ol>
 *   <li>its favourite food, lying on the ground;</li>
 *   <li>any dropped food it can eat - the same test a hand-feeding uses
 *       ({@code GeneAbilityHandler.eats});</li>
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
 * <p>UNVERIFIED: that {@code BlockTags.CROPS} and {@code BlockTags.SMALL_FLOWERS} hold what
 * their names say in 26.1.2 (both compile; contents not read), and that a partial path to an
 * unreachable target times out cleanly - an unreachable target is ignored for a minute after
 * {@link #MAX_PURSUIT}.
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
    /** A fed horse grazes where it stands about once in this many checks. */
    private static final int GRAZE_CHANCE = 200;
    private static final float HUNT_DAMAGE = 4.0F;
    private static final int HUNT_SWING = 20;

    /** Passive animals whose drops a carnivore eats: {@link DietFoods}'s raw meat. */
    private static final Set<EntityType<?>> MEAT_PREY = Set.of(
            EntityType.COW, EntityType.MOOSHROOM, EntityType.PIG, EntityType.SHEEP,
            EntityType.CHICKEN, EntityType.RABBIT);
    /** ...and a fish-eater's. */
    private static final Set<EntityType<?>> FISH_PREY = Set.of(
            EntityType.COD, EntityType.SALMON, EntityType.TROPICAL_FISH, EntityType.PUFFERFISH);

    private final Horse horse;
    private final Map<Long, Long> ignoredUntil = new HashMap<>();

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
        if (!(horse.level() instanceof ServerLevel level) || !free() || !HorseRecords.hasRealRecord(horse)) {
            return false;
        }
        HorseDiet diet = HorseDietHandler.dietOf(horse);
        if (diet.diet() == Diet.NOTHING || diet.diet() == Diet.BLOOD) {
            return false;
        }
        double hunger = hunger();
        if (!Hunger.seeksFood(hunger)) {
            if (Hunger.wantsMore(hunger) && horse.getRandom().nextInt(GRAZE_CHANCE) == 0) {
                grazeInPlace(level, diet);
            }
            return false;
        }
        if (--searchCooldown > 0) {
            return false;
        }
        searchCooldown = SEARCH_INTERVAL;
        return search(level, diet);
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
        return block != null && horse.level() instanceof ServerLevel level
                && rungOf(level, block, HorseDietHandler.dietOf(horse)) == rung;
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
            }
        }
        if (horse.distanceToSqr(at) > REACH_SQ) {
            return;
        }
        if (item != null) {
            eatItem(level, item);
            item = null;
        } else if (block != null) {
            eatBlock(level, block, rung);
            block = null;
        } else if (prey != null && --swingCooldown <= 0) {
            swingCooldown = HUNT_SWING;
            hunt(level, prey);
        }
    }

    // ------------------------------------------------------------------
    // Finding food
    // ------------------------------------------------------------------

    /** The best thing to eat in reach, in {@link Hunger.Food} order and nearest within a rung. */
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
            if (ignored(~(long) e.getId(), now) || !GeneAbilityHandler.eats(horse, e.getItem())) {
                continue;
            }
            boolean fav = favourite != null
                    && favourite.equals(BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).toString());
            double d = horse.distanceToSqr(e);
            if ((fav && !bestIsFavourite) || (fav == bestIsFavourite && d < bestItemDist)) {
                bestItem = e;
                bestIsFavourite = fav;
                bestItemDist = d;
            }
        }
        if (bestItem != null) {
            item = bestItem;
            rung = bestIsFavourite ? Hunger.Food.FAVOURITE : Hunger.Food.DROPPED;
            return true;
        }

        BlockPos origin = horse.blockPosition();
        BlockPos bestBlock = null;
        Hunger.Food bestRung = null;
        double bestBlockDist = Double.MAX_VALUE;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                for (int dy = -SEARCH_DOWN; dy <= SEARCH_UP; dy++) {
                    p.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    Hunger.Food r = rungOf(level, p, diet);
                    if (r == null || (bestRung != null && r.ordinal() > bestRung.ordinal())
                            || ignored(p.asLong(), now)) {
                        continue;
                    }
                    double d = horse.distanceToSqr(Vec3.atCenterOf(p));
                    if (bestRung == null || r.ordinal() < bestRung.ordinal() || d < bestBlockDist) {
                        bestBlock = p.immutable();
                        bestRung = r;
                        bestBlockDist = d;
                    }
                }
            }
        }
        if (bestBlock != null) {
            block = bestBlock;
            rung = bestRung;
            return true;
        }

        if (diet.diet() == Diet.RAW_MEAT || diet.diet() == Diet.FISH) {
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
                prey = best;
                rung = Hunger.Food.DROPPED;     // what it is after is the drop
                return true;
            }
        }
        return false;
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
        } else if (st.is(Blocks.HAY_BLOCK)) {
            food = Hunger.Food.HAY;
            form = Items.HAY_BLOCK;
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

    /** A fed horse, now and then: the tuft it stands in, or the grass under its feet. */
    private void grazeInPlace(ServerLevel level, HorseDiet diet) {
        BlockPos at = horse.blockPosition();
        if (rungOf(level, at, diet) == Hunger.Food.GRASS) {
            eatBlock(level, at, Hunger.Food.GRASS);
        } else if (rungOf(level, at.below(), diet) == Hunger.Food.GRASS) {
            eatBlock(level, at.below(), Hunger.Food.GRASS);
        }
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
        fed(level, rung, BuiltInRegistries.ITEM.getKey(what).toString(), e.blockPosition());
    }

    private void eatBlock(ServerLevel level, BlockPos pos, Hunger.Food food) {
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
        fed(level, food, what, pos);
    }

    private void fed(ServerLevel level, Hunger.Food food, String what, BlockPos pos) {
        double after = Hunger.eat(hunger(), food);
        horse.setData(ModAttachments.HUNGER.get(), after);
        level.playSound(null, horse.getX(), horse.getY(), horse.getZ(),
                SoundEvents.HORSE_EAT, SoundSource.NEUTRAL, 0.8F, 1.0F);
        ActionTrace.log("hunger", ActionTrace.describeShort(horse) + " ate " + what + " ("
                + food.name().toLowerCase(java.util.Locale.ROOT) + ") at " + pos.toShortString()
                + " - hunger now " + Math.round(after));
    }

    private void hunt(ServerLevel level, LivingEntity target) {
        target.hurtServer(level, level.damageSources().mobAttack(horse), HUNT_DAMAGE);
        horse.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        if (!target.isAlive()) {
            ActionTrace.log("hunger", ActionTrace.describeShort(horse) + " hunted "
                    + BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()) + " at "
                    + target.blockPosition().toShortString() + " - its drops are next");
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
