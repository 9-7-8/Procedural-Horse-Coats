package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.BreedFounder;
import com.example.horsegenetics.common.breed.BreedLineage;
import com.example.horsegenetics.common.breed.Breeds;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.name.HorseNameGenerator.NameParts;
import com.example.horsegenetics.common.name.PersonNameGenerator;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.NeoRng;
import com.example.horsegenetics.neoforge.data.CowboyBrand;
import com.example.horsegenetics.neoforge.data.HorseCareAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.entity.Cowboy;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Builds a cowboy out of the bare entity the barn structure placed, and wires
 * the two goals that make him and his horses behave like a working outfit.
 *
 * <h2>Founding, on the tick and not on the join</h2>
 * {@code cowboy_barn.nbt} carries a {@code horsegenetics:cowboy} with nothing in
 * it but its id - a structure template can only place an entity, not roll one.
 * So his name, his home, his mount and his herd are all made here, on his first
 * server tick.
 *
 * <p>The tick and not {@link EntityJoinLevelEvent} for exactly the reason
 * {@link HorseFoundingTickHandler} documents at length: founding spawns horses,
 * spawning a horse ends in an {@code Attributes.SCALE} write, and a scale write
 * runs a collision scan that can force-load a chunk. Doing that from the join
 * event or a {@code server.execute} task re-enters the chunk system's ticket
 * pass and crashes it. {@code EntityTickEvent.Post} is after the tick's chunk
 * work is done.
 *
 * <h2>One cowboy per village</h2>
 * The jigsaw generator has no way to say "at most one of this element per
 * structure", so a village that rolls two barns gets two barns. The second one
 * is just a barn: of the cowboys within {@link #ONE_PER_RADIUS} of each other
 * exactly one founds, and the rest discard themselves before spawning anything.
 */
@EventBusSubscriber
public final class CowboyHandler {

    /** A second cowboy this close to a founded one is surplus and removes itself. */
    private static final int ONE_PER_RADIUS = 128;

    /** Attempts to find a standable spot for one new horse before giving up. */
    private static final int PLACEMENT_TRIES = 24;

    /** How far from the barn centre a herd horse may be placed. */
    private static final int PLACEMENT_RADIUS = 5;

    private static final PersonNameGenerator NAMES = PersonNameGenerator.cowboys();

    private CowboyHandler() {
    }

    // ------------------------------------------------------------------
    // founding
    // ------------------------------------------------------------------

    @SubscribeEvent
    static void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Cowboy cowboy)) {
            return;
        }
        if (!(cowboy.level() instanceof ServerLevel level) || cowboy.isFounded()) {
            return;
        }
        found(cowboy, level);
    }

    private static void found(Cowboy cowboy, ServerLevel level) {
        if (hasNeighbouringCowboy(cowboy, level)) {
            cowboy.discard();
            return;
        }

        Rng rng = new NeoRng(cowboy.getRandom());
        cowboy.setCustomName(Component.literal(NAMES.generate(rng)));
        cowboy.setCustomNameVisible(true);
        cowboy.setHome(cowboy.blockPosition());
        cowboy.markFounded();

        // His own horse first: herd slot 0 is the one he rides and never sells.
        Horse mount = breedHorse(cowboy, level, rng);
        if (mount != null) {
            cowboy.startRiding(mount, true, false);
        }

        int herdSize = Cowboy.MIN_HERD + cowboy.getRandom().nextInt(Cowboy.MAX_HERD - Cowboy.MIN_HERD + 1);
        for (int i = 0; i < herdSize; i++) {
            breedHorse(cowboy, level, rng);
        }

        HorseGenetics.LOGGER.info("{} set up at {} with {} horses",
                cowboy.cowboyName(), cowboy.blockPosition(), cowboy.herdIds().size());
    }

    /**
     * Is this cowboy the surplus one? Two tests, because there are two ways a
     * village ends up with two barns' worth of cowboy:
     *
     * <ul>
     *   <li>an <b>already founded</b> neighbour - the ordinary case, one barn
     *       loaded and set up before the other was reached;</li>
     *   <li>an <b>unfounded</b> neighbour with a lower UUID - the case where
     *       both barns load in the same tick and neither is founded yet, so a
     *       plain "is anyone founded?" check would let both through. Electing
     *       the lowest UUID is the same tie-break {@link HerdManager} uses for
     *       a wild herd's lead, and for the same reason: every candidate has to
     *       reach the same answer without talking to the others.</li>
     * </ul>
     */
    private static boolean hasNeighbouringCowboy(Cowboy cowboy, ServerLevel level) {
        AABB box = new AABB(cowboy.blockPosition()).inflate(ONE_PER_RADIUS);
        for (Cowboy other : level.getEntitiesOfClass(Cowboy.class, box)) {
            if (other == cowboy) {
                continue;
            }
            if (other.isFounded() || other.getUUID().compareTo(cowboy.getUUID()) < 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * One horse of the cowboy's own breeding: a <b>foundation</b> horse with no
     * parents and generation 0, stamped with a real breed - he never keeps a
     * feral mixed - and with {@code bredBy} set to his name.
     *
     * <p>{@code bredBy} is the whole point of the character. It is the field
     * {@link HorseRecord#attribution()} prefers over {@code tamedBy}, so long
     * after the horse has changed hands twice the family tree still says who
     * bred it, and transferring ownership never touches it.
     */
    private static @Nullable Horse breedHorse(Cowboy cowboy, ServerLevel level, Rng rng) {
        BlockPos spot = findPlacement(cowboy, level);
        if (spot == null) {
            return null;
        }
        Horse horse = EntityType.HORSE.create(level, EntitySpawnReason.STRUCTURE);
        if (horse == null) {
            return null;
        }
        horse.snapTo(spot, level.getRandom().nextFloat() * 360.0F, 0.0F);
        horse.setAge(0);                 // an adult; he does not sell foals
        horse.setPersistenceRequired();  // his stock does not despawn

        Breed breed = pickBreed(cowboy, level);
        Genome genome = BreedFounder.roll(breed, rng);
        NameParts name = HorseRecords.newNameParts(rng);
        HorseRecord record = HorseRecord
                .founder(horse.getUUID(), name.first(), name.last(), genome,
                        BreedLineage.pure(breed.id()).toToken())
                .withBredBy(cowboy.cowboyName());

        level.addFreshEntity(horse);
        HorseRecords.apply(horse, record);
        horse.setData(ModAttachments.COWBOY_BRAND.get(), CowboyBrand.of(cowboy.getUUID()));
        cowboy.addToHerd(horse.getUUID());
        joinHerd(cowboy, horse);
        return horse;
    }

    /**
     * Put the horse in the cowboy's herd, <b>led by the horse he rides</b>. The
     * first horse made is the mount, so it becomes the lead and points at
     * itself - the same convention {@link HerdManager} uses for a wild herd's
     * lead, which is what lets the browser and the info panel treat this as an
     * ordinary herd with an ordinary lead.
     *
     * <p>Deliberately <b>not</b> {@code withWildHerd}: that also sets a herd
     * breed and band, and neither is true here. His string is a mixed set of
     * pure breeds, not one breed's band, and saying otherwise would put a
     * falsehood in the field {@code WildHerdGoal} keys off - which would then
     * fight {@link CowboyHerdGoal} for the same horses.
     */
    private static void joinHerd(Cowboy cowboy, Horse horse) {
        UUID lead = cowboy.mountId().orElse(horse.getUUID());
        HorseCareAttachment care = horse.getData(ModAttachments.HORSE_CARE.get());
        horse.setData(ModAttachments.HORSE_CARE.get(), care.withHerd(Optional.of(lead)));
    }

    /**
     * A breed that belongs where the barn is, weighted the way a wild herd is
     * weighted - so most of his string is workaday and the rare one is worth
     * the ride out. Never {@link Breeds#FERAL_MIXED}: a horse of unrecorded
     * ancestry is precisely what a breeder does not have.
     */
    private static Breed pickBreed(Cowboy cowboy, ServerLevel level) {
        Breed breed = HerdManager.pickHerdBreed(
                level.getBiome(cowboy.blockPosition()), level.getRandom());
        if (breed != Breeds.FERAL_MIXED) {
            return breed;
        }
        List<Breed> all = Breeds.all();
        double total = 0.0;
        for (Breed b : all) {
            if (b != Breeds.FERAL_MIXED) {
                total += b.spawnWeight();
            }
        }
        double roll = level.getRandom().nextDouble() * total;
        for (Breed b : all) {
            if (b == Breeds.FERAL_MIXED) {
                continue;
            }
            roll -= b.spawnWeight();
            if (roll < 0.0) {
                return b;
            }
        }
        return all.get(all.size() - 1);
    }

    /** A block near the barn with solid ground under it and room to stand. */
    private static @Nullable BlockPos findPlacement(Cowboy cowboy, ServerLevel level) {
        BlockPos origin = cowboy.blockPosition();
        for (int attempt = 0; attempt < PLACEMENT_TRIES; attempt++) {
            BlockPos candidate = origin.offset(
                    level.getRandom().nextInt(PLACEMENT_RADIUS * 2 + 1) - PLACEMENT_RADIUS,
                    0,
                    level.getRandom().nextInt(PLACEMENT_RADIUS * 2 + 1) - PLACEMENT_RADIUS);
            if (standable(level, candidate)) {
                return candidate;
            }
        }
        return standable(level, origin) ? origin : null;
    }

    private static boolean standable(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos.below()).isSolidRender()
                && level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir();
    }

    // ------------------------------------------------------------------
    // the goals
    // ------------------------------------------------------------------

    /**
     * Every horse gets both cowboy goals on join. They are cheap and they check
     * their own preconditions - a horse with no brand and no rider on its back
     * never leaves {@code canUse()} - which is a much smaller thing to get
     * wrong than trying to add a goal at the moment a horse becomes his.
     */
    @SubscribeEvent
    static void onHorseJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Horse horse)) {
            return;
        }
        for (WrappedGoal wrapped : horse.goalSelector.getAvailableGoals()) {
            if (wrapped.getGoal() instanceof CowboyMountGoal) {
                return;
            }
        }
        // Priority 0: while the cowboy is aboard, his itinerary outranks every
        // stroll, panic and follow goal the horse has.
        horse.goalSelector.addGoal(0, new CowboyMountGoal(horse));
        horse.goalSelector.addGoal(3, new CowboyHerdGoal(horse));
    }

    // ------------------------------------------------------------------
    // brands
    // ------------------------------------------------------------------

    /** The cowboy who owns this horse, if it is branded and he is still alive. */
    public static @Nullable Cowboy ownerOf(Horse horse, ServerLevel level) {
        CowboyBrand brand = horse.getData(ModAttachments.COWBOY_BRAND.get());
        if (brand == null || brand.cowboy().isEmpty()) {
            return null;
        }
        UUID id = brand.cowboy().get();
        if (level.getEntity(id) instanceof Cowboy cowboy && cowboy.isAlive()) {
            return cowboy;
        }
        // He is gone. His horses are nobody's now - clear the brand so they can
        // be tamed the ordinary way rather than becoming untameable orphans.
        horse.setData(ModAttachments.COWBOY_BRAND.get(), CowboyBrand.NONE);
        return null;
    }

    /** Redemption, and death: this horse is no longer the cowboy's. */
    public static void clearBrand(Horse horse) {
        horse.setData(ModAttachments.COWBOY_BRAND.get(), CowboyBrand.NONE);
    }

    /**
     * Kill the cowboy and his herd goes feral: the horses stop following, and
     * anyone may tame them the hard way. They keep his name as their breeder,
     * because that is a fact about where they came from and not a claim on
     * them.
     */
    public static void onCowboyDied(Cowboy cowboy, ServerLevel level) {
        for (Horse horse : cowboy.liveHerd(level)) {
            clearBrand(horse);
        }
    }
}
