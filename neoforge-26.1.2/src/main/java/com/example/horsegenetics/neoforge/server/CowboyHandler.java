package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.BreedFounder;
import com.example.horsegenetics.common.breed.BreedLineage;
import com.example.horsegenetics.common.breed.BreedSource;
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
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
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
 * <p>He is also <b>restocked</b> from here, on the same tick handler and for
 * the same reason - see {@link #restock}. The <b>horseman</b> next door is not
 * built here at all: his post is a block in {@code cowboy_barn.nbt}, in the
 * middle of the road-facing end, and the villager who takes the job off it is a
 * structure entity beside it. A shop front is architecture.
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

    /** How far from the cowboy a herd horse may be placed. */
    private static final int PLACEMENT_RADIUS = 5;

    /** Nearest and furthest the founding paddock may be from the barn. */
    private static final int PADDOCK_MIN = 12;
    private static final int PADDOCK_MAX = 24;

    /** How wide a fan of directions out of town the paddock search samples, in radians. */
    private static final double PADDOCK_ARC = Math.PI;

    /** Attempts to find an open paddock before giving up and founding at the barn. */
    private static final int PADDOCK_TRIES = 60;

    /**
     * Fraction of {@link #PADDOCK_TRIES} spent in the out-of-town fan before the
     * search gives up on the bearing and samples the whole circle. A barn on the
     * shore of a lake has no open ground on its outward side and would otherwise
     * fall all the way through to founding in the building.
     */
    private static final double PADDOCK_FAN_SHARE = 0.7;

    /**
     * How far the paddock may sit above or below the barn's own floor.
     *
     * <p>The <b>roof rule</b>, half of it. The surface heightmap answers "what is
     * the highest solid block in this column", and in a village that is very
     * often somebody's roof - which is where the first cowboy to try this ended
     * up standing, on a house that was not even his. A paddock is meant to be the
     * field the barn stands in, so it has to be at about the barn's own level.
     */
    private static final int PADDOCK_RISE = 3;

    /**
     * How deep the ground under a paddock has to be solid.
     *
     * <p>The other half of the roof rule, and the half that catches a roof the
     * height test cannot - a porch, a wall top, a haystack. Standing on a roof
     * means one solid block underfoot and then the room; standing on ground means
     * solid all the way down. Three blocks is enough to tell them apart and cheap
     * enough to run sixty times.
     */
    private static final int PADDOCK_GROUND_DEPTH = 3;

    /** Ticks between two looks over his string - about a minute. */
    private static final int RESTOCK_INTERVAL = 1200;

    /**
     * Spread on {@link #RESTOCK_INTERVAL}, so two cowboys loaded in the same
     * tick do not go on restocking on the same tick for the rest of the world's
     * life. Costs nothing and keeps the work smeared out.
     */
    private static final int RESTOCK_JITTER = 600;

    /** Chance, per look, that one horse is retired to make room for a new one. */
    private static final float ROTATE_CHANCE = 0.15F;

    /**
     * A horse with a player this close is never quietly removed.
     *
     * <p>The rotation is meant to be something you notice by coming back to a
     * different string, not something you watch happen. A horse vanishing in
     * front of you reads as a bug however well it is documented.
     */
    private static final int ROTATE_PRIVACY = 48;

    /**
     * At most one new horse per look.
     *
     * <p>A cowboy who has just sold his whole string gets it back over several
     * minutes rather than in one tick - which paces better in play, and keeps
     * the spawn work per tick where founding already proved it is safe.
     */
    private static final int RESTOCK_PER_LOOK = 1;

    /** Ticks between the cowboy's own state line, and between attempts to get him back on. */
    private static final int COWBOY_REPORT_INTERVAL = 20;

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
        if (!(cowboy.level() instanceof ServerLevel level)) {
            return;
        }
        if (!cowboy.isFounded()) {
            found(cowboy, level);
            return;
        }
        if (cowboy.tickCount % COWBOY_REPORT_INTERVAL == 0) {
            reportCowboy(cowboy, level);
        }
        if (cowboy.tickRestockClock()) {
            cowboy.setRestockCooldown(RESTOCK_INTERVAL + cowboy.getRandom().nextInt(RESTOCK_JITTER));
            restock(cowboy, level);
        }
    }

    /**
     * One line a second about the man himself, whether or not he is on a horse.
     *
     * <p>{@code CowboyMountGoal}'s own report only runs while that goal is
     * running, so it is silent in precisely the case that needs explaining. This
     * one runs off the cowboy's tick, which is the same tick that founds him, so
     * it cannot be silent for a cowboy that exists at all.
     */
    private static void reportCowboy(Cowboy cowboy, ServerLevel level) {
        if (!DebugAnnounce.enabled()) {
            return;
        }
        DebugAnnounce.log("Cowboy", String.format(
                "%s founded=%s pos=%d,%d,%d home=%s herd=%d live=%d stock=%d target=%d trading=%s",
                cowboy.cowboyName(), cowboy.isFounded(),
                cowboy.blockPosition().getX(), cowboy.blockPosition().getY(), cowboy.blockPosition().getZ(),
                cowboy.home().map(Object::toString).orElse("none"),
                cowboy.herdIds().size(), cowboy.liveHerd(level).size(),
                sellableStock(cowboy, level).size(), cowboy.stockTarget(),
                cowboy.isTrading()));
    }

    private static void found(Cowboy cowboy, ServerLevel level) {
        Rng rng = new NeoRng(cowboy.getRandom());
        // The village's horse family, if it has one already - the hitch and the
        // table hand out their jobs in whatever order, so whoever is hired first
        // coins the surname and the other joins it. See server/HorsemanHandler.
        String surname = HorsemanHandler.familySurname(level, cowboy.blockPosition(), cowboy, rng);
        cowboy.setCustomName(Component.literal(NAMES.generateParts(rng).first() + " " + surname));
        cowboy.setCustomNameVisible(true);

        // Home is where the structure put him - the middle of the barn - and it
        // stays that whatever happens next. Nothing walks him back to it any
        // more: the dusk ride and the door sweep that used to read this both
        // went with the mounted cowboy. It is a remembered address now, not a
        // routine. (CowboyDoorGoal opens doors he bumps into; it does not know
        // about this.)
        BlockPos barn = cowboy.blockPosition();
        cowboy.setHome(barn);
        cowboy.markFounded();

        // But he does not found *in* it. The barn interior is eleven blocks by
        // five with walls on every side, and a horse is nearly a block and a half
        // wide: placing a string of them in there put half of them in the walls.
        // He walks out to open ground first and the herd is made around him.
        BlockPos paddock = findPaddock(level, barn);
        if (paddock != null) {
            cowboy.snapTo(paddock, cowboy.getYRot(), 0.0F);
        }

        // The breed he is known for, settled before the first horse is made so
        // that horse can be one.
        Breed favourite = pickBreed(cowboy, level);
        cowboy.setPreferredBreed(favourite.id());

        int herdSize = Cowboy.MIN_HERD + cowboy.getRandom().nextInt(Cowboy.MAX_HERD - Cowboy.MIN_HERD + 1);
        cowboy.setStockTarget(herdSize);

        breedHorse(cowboy, level, rng, favourite);
        for (int i = 1; i < herdSize; i++) {
            breedHorse(cowboy, level, rng, nextBreedFor(cowboy, level));
        }

        HorseGenetics.LOGGER.info("{} set up at {} with {} horses",
                cowboy.cowboyName(), cowboy.blockPosition(), cowboy.herdIds().size());
        announce(cowboy, level);
    }

    /**
     * In a dev build, say in chat that a cowboy just founded and where.
     *
     * <p>Founding happens the moment his chunk starts ticking, which is usually
     * before the player is close enough to see the barn - so without this, "did
     * one generate?" is a question you answer by reading the server log, and the
     * answer scrolls past. See {@link DebugAnnounce} for the rest of the reasoning
     * and for why it costs nothing in a real build.
     */
    private static void announce(Cowboy cowboy, ServerLevel level) {
        DebugAnnounce.sayAt(level, "Cowboy",
                cowboy.cowboyName() + " set up with "
                        + (cowboy.herdIds().size() - 1) + " horses for sale",
                cowboy.blockPosition(), ChatFormatting.YELLOW);
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
    private static @Nullable Horse breedHorse(Cowboy cowboy, ServerLevel level, Rng rng, Breed breed) {
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
        // The herd's lead id is the man himself. Every other herd in this mod
        // leads with a horse, but this one has never had a horse to lead with
        // since he stopped riding - and the browser only needs the ids to agree.
        HorseCareAttachment care = horse.getData(ModAttachments.HORSE_CARE.get());
        horse.setData(ModAttachments.HORSE_CARE.get(), care.withHerd(Optional.of(cowboy.getUUID())));
        return horse;
    }

    /**
     * A breed that belongs where the barn is, weighted the way a wild herd is
     * weighted - so most of his string is workaday and the rare one is worth
     * the ride out. Never {@link Breeds#FERAL_MIXED}: a horse of unrecorded
     * ancestry is precisely what a breeder does not have.
     *
     * <p>This is the <b>random half</b> of his string. Which half a given horse
     * falls in is {@link #nextBreedFor}'s call.
     */
    private static Breed pickBreed(Cowboy cowboy, ServerLevel level) {
        Breed breed = HerdManager.pickHerdBreed(
                level.getBiome(cowboy.blockPosition()), level.getRandom(), BreedSource.COWBOY);
        if (breed != Breeds.FERAL_MIXED) {
            return breed;
        }
        // Nothing local he is allowed to deal in - fall back to the whole set he
        // is, still weighted by commonness. A breed that has switched the cowboy
        // source off is not in this list either, so "no dealer has ever had one"
        // is a thing a breed can actually say.
        List<Breed> sellable = Breeds.from(BreedSource.COWBOY);
        if (sellable.isEmpty()) {
            return Breeds.FERAL_MIXED;
        }
        double total = 0.0;
        for (Breed b : sellable) {
            total += b.spawnWeight();
        }
        double roll = level.getRandom().nextDouble() * total;
        for (Breed b : sellable) {
            roll -= b.spawnWeight();
            if (roll < 0.0) {
                return b;
            }
        }
        return sellable.get(sellable.size() - 1);
    }

    /**
     * An open patch of ground for the outfit to start on: near the barn, out of
     * it, and on the far side of it from the village.
     *
     * <p>The bearing is taken from the village bell to the barn, and candidates
     * are sampled in a {@link #PADDOCK_ARC} fan around it - so "out of town" is
     * literal, and he sets up on the grass past the last house rather than in
     * somebody's turnip field. Past {@link #PADDOCK_FAN_SHARE} of the attempts the
     * search widens to the whole circle rather than fail, and a village with no
     * findable bell starts from a random bearing.
     *
     * <p>Each candidate is dropped onto the surface heightmap and then has to pass
     * {@link #standingOnGround}, which is what keeps him off the rooftops - see
     * {@link #PADDOCK_RISE} and {@link #PADDOCK_GROUND_DEPTH} for why a heightmap
     * position on its own is not enough in a village.
     */
    private static @Nullable BlockPos findPaddock(ServerLevel level, BlockPos barn) {
        double outward = outwardHeading(level, barn);
        int fanTries = (int) (PADDOCK_TRIES * PADDOCK_FAN_SHARE);
        for (int attempt = 0; attempt < PADDOCK_TRIES; attempt++) {
            double arc = attempt < fanTries ? PADDOCK_ARC : Math.PI * 2.0;
            double angle = outward + (level.getRandom().nextDouble() - 0.5) * arc;
            int distance = PADDOCK_MIN + level.getRandom().nextInt(PADDOCK_MAX - PADDOCK_MIN + 1);
            BlockPos ground = level.getHeightmapPos(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    barn.offset((int) Math.round(Math.cos(angle) * distance),
                            0,
                            (int) Math.round(Math.sin(angle) * distance)));
            if (nearHome(barn, ground)) {
                continue;
            }
            if (standingOnGround(level, barn, ground) && roomForAHorse(level, ground)) {
                return ground;
            }
        }
        return null;
    }

    /**
     * Is this the field, or is it a roof?
     *
     * <p>Two tests, because either alone is fooled. The height test alone passes
     * a low porch or a wall top; the depth test alone passes the flat top of a
     * hill fifteen blocks above the village. Together they say "ground, at about
     * the height the barn is at", which is what a paddock means.
     */
    private static boolean standingOnGround(ServerLevel level, BlockPos barn, BlockPos pos) {
        if (Math.abs(pos.getY() - barn.getY()) > PADDOCK_RISE) {
            return false;
        }
        for (int depth = 1; depth <= PADDOCK_GROUND_DEPTH; depth++) {
            if (!level.getBlockState(pos.below(depth)).isSolidRender()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Half-span of the box that counts as "at the barn", around his home block.
     *
     * <p>An approximation, and knowingly so: the barn is 15x7 on the ground and
     * the jigsaw can rotate it any of four ways, so the only shape that is
     * rotation-independent is a square.
     */
    private static final int HOME_RADIUS = 5;

    /** How far above and below home "at the barn" reaches. */
    private static final int HOME_HEIGHT = 3;

    /** How far to look for the village bell from the barn. */
    private static final int BELL_SEARCH = 96;

    private static final java.util.function.Predicate<net.minecraft.core.Holder<
            net.minecraft.world.entity.ai.village.poi.PoiType>> MEETING =
            holder -> holder.is(net.minecraft.world.entity.ai.village.poi.PoiTypes.MEETING);

    /** Near enough to his home block to count as "at the barn". */
    private static boolean nearHome(BlockPos barn, BlockPos pos) {
        return Math.abs(pos.getX() - barn.getX()) <= HOME_RADIUS
                && Math.abs(pos.getZ() - barn.getZ()) <= HOME_RADIUS
                && Math.abs(pos.getY() - barn.getY()) <= HOME_HEIGHT;
    }

    /**
     * The village's bell - the one part of a generated village reliably at its
     * middle and reliably findable at runtime, which beats trying to recover the
     * structure's bounding box after the fact.
     */
    private static Optional<BlockPos> villageCentre(ServerLevel level, BlockPos barn) {
        return level.getPoiManager().findClosest(
                MEETING, barn, BELL_SEARCH,
                net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY);
    }

    /** Which way is out of town: the bearing from the village bell to the barn. */
    private static double outwardHeading(ServerLevel level, BlockPos barn) {
        Optional<BlockPos> centre = villageCentre(level, barn);
        if (centre.isPresent()) {
            double dx = barn.getX() - centre.get().getX();
            double dz = barn.getZ() - centre.get().getZ();
            if (dx != 0.0 || dz != 0.0) {
                return Math.atan2(dz, dx);
            }
        }
        return level.getRandom().nextDouble() * Math.PI * 2.0;
    }

    /** A block near the cowboy with solid ground under it and room for a horse. */
    private static @Nullable BlockPos findPlacement(Cowboy cowboy, ServerLevel level) {
        BlockPos origin = cowboy.blockPosition();
        for (int attempt = 0; attempt < PLACEMENT_TRIES; attempt++) {
            BlockPos candidate = origin.offset(
                    level.getRandom().nextInt(PLACEMENT_RADIUS * 2 + 1) - PLACEMENT_RADIUS,
                    0,
                    level.getRandom().nextInt(PLACEMENT_RADIUS * 2 + 1) - PLACEMENT_RADIUS);
            if (roomForAHorse(level, candidate)) {
                return candidate;
            }
        }
        return roomForAHorse(level, origin) ? origin : null;
    }

    /**
     * Ground to stand on, and <b>a horse's worth of room above it</b>.
     *
     * <p>This used to test one column - air here, air above, solid below - which
     * is the right test for a villager and the wrong one for a horse. A horse is
     * about a block and a half wide, so a column that is clear can still put its
     * shoulders through the wall beside it; that is exactly how a barn full of
     * horses ended up with half of them embedded in the walls. Asking the level
     * whether the horse's own spawn box collides with anything is the same
     * question the game asks, and it cannot disagree with the game.
     */
    private static boolean roomForAHorse(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos.below()).isSolidRender()) {
            return false;
        }
        if (level.getBlockState(pos).liquid()) {
            return false;
        }
        return level.noCollision(EntityType.HORSE.getSpawnAABB(
                pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
    }

    // ------------------------------------------------------------------
    // restocking
    // ------------------------------------------------------------------

    /**
     * Keep his string worth walking out to see.
     *
     * <p>Two things happen here, both slowly and both on the same clock:
     *
     * <ul>
     *   <li><b>Topping up.</b> Horses leave - bought, tamed, eaten by a wolf -
     *       and without this a cowboy the player has traded with once is a man
     *       standing next to an empty field for the rest of the world's life.
     *       He breeds back up to {@link Cowboy#stockTarget()}, the number he had
     *       the day he set up, one horse per look.</li>
     *   <li><b>Rotation.</b> A <i>full</i> string turns over now and then: one
     *       horse retires and the top-up replaces it next look. Without it a
     *       player who did not like the six horses on offer never has a reason
     *       to come back, because they would be the same six horses forever.</li>
     * </ul>
     *
     * <p>Only ever the horses he can still sell. His mount is not stock, a horse
     * somebody has already bought the papers for is spoken for, and one that has
     * been tamed has left his hands entirely - {@link #forgetTamed} drops those
     * from the herd so they stop counting against the target they no longer fill.
     */
    private static void restock(Cowboy cowboy, ServerLevel level) {
        if (cowboy.stockTarget() <= 0) {
            return; // never founded a string, or founded before there was a target
        }
        forgetTamed(cowboy, level);

        List<Horse> stock = sellableStock(cowboy, level);
        if (stock.size() >= cowboy.stockTarget()
                && !stock.isEmpty()
                && level.getRandom().nextFloat() < ROTATE_CHANCE) {
            retireOne(cowboy, level, stock);
            stock = sellableStock(cowboy, level);
        }

        Rng rng = new NeoRng(cowboy.getRandom());
        for (int bred = 0; bred < RESTOCK_PER_LOOK && stock.size() < cowboy.stockTarget(); bred++) {
            Horse horse = breedHorse(cowboy, level, rng, nextBreedFor(cowboy, level));
            if (horse == null) {
                break; // nowhere to stand it - try again next look
            }
            stock = sellableStock(cowboy, level);
        }
    }

    /**
     * The horses he could write a paper for right now: alive, loaded, untamed,
     * not his mount, not already sold.
     *
     * <p>The same test {@code Cowboy.updateTrades} applies when it builds the
     * offer list, and deliberately so - "how many has he got?" and "how many can
     * you buy?" have to be the same number, or he restocks to fill a shelf the
     * merchant screen cannot see.
     *
     * <p><b>Loaded only.</b> An unloaded horse is indistinguishable from a dead
     * one through {@code getEntity}, so it does not count - which is safe in the
     * direction that matters, because this only ever runs on a cowboy whose own
     * chunk is ticking, and his string is by construction standing around him.
     */
    private static List<Horse> sellableStock(Cowboy cowboy, ServerLevel level) {
        List<UUID> ids = cowboy.herdIds();
        List<Horse> out = new ArrayList<>();
        for (UUID id : ids) {
            if (cowboy.hasSold(id)) {
                continue;
            }
            if (level.getEntity(id) instanceof Horse horse && horse.isAlive() && !horse.isTamed()) {
                out.add(horse);
            }
        }
        return out;
    }

    /**
     * Drop tamed horses out of the herd for good. A redeemed paper tames the
     * horse and clears its brand, so it has stopped following him and stopped
     * being his - leaving the id in the list would only make {@code herd} grow
     * without bound over a long world.
     */
    private static void forgetTamed(Cowboy cowboy, ServerLevel level) {
        for (UUID id : cowboy.herdIds()) {
            if (level.getEntity(id) instanceof Horse horse && horse.isTamed()) {
                cowboy.removeFromHerd(id);
            }
        }
    }

    /**
     * Retire one horse at random - unless somebody is stood near enough to watch
     * it happen ({@link #ROTATE_PRIVACY}), in which case nothing happens and the
     * next look tries again.
     */
    private static void retireOne(Cowboy cowboy, ServerLevel level, List<Horse> stock) {
        Horse horse = stock.get(level.getRandom().nextInt(stock.size()));
        if (level.getNearestPlayer(horse, ROTATE_PRIVACY) != null) {
            return;
        }
        cowboy.removeFromHerd(horse.getUUID());
        clearBrand(horse);
        horse.discard();
    }

    /**
     * Which breed the next horse he breeds should be: <b>his own until half the
     * string is it</b>, and something the country round him produces after that.
     *
     * <p>Counted rather than coin-flipped, because a coin flip on a string of
     * six lands on eight-out-of-eight often enough to be seen, and "the man who
     * only has Fjords" is a different character from "the Fjord man". Counting
     * also self-corrects: sell three of his Fjords and the next three he breeds
     * are Fjords.
     */
    private static Breed nextBreedFor(Cowboy cowboy, ServerLevel level) {
        Optional<String> preferred = cowboy.preferredBreed();
        if (preferred.isEmpty()) {
            return pickBreed(cowboy, level);
        }
        Breed favourite = Breeds.get(preferred.get());
        if (favourite == Breeds.FERAL_MIXED) {
            return pickBreed(cowboy, level);
        }
        int wanted = (cowboy.stockTarget() + 1) / 2;
        int have = 0;
        for (Horse horse : sellableStock(cowboy, level)) {
            if (favourite.id().equals(pureBreedOf(horse))) {
                have++;
            }
        }
        return have < wanted ? favourite : pickBreed(cowboy, level);
    }

    /** The breed id of a horse of one pure breed, or {@code null} for anything else. */
    private static @Nullable String pureBreedOf(Horse horse) {
        BreedLineage lineage = HorseRecords.of(horse).lineage();
        return lineage.kind() == BreedLineage.Kind.PURE ? lineage.components().get(0) : null;
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
            if (wrapped.getGoal() instanceof CowboyHerdGoal) {
                return;
            }
        }
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
