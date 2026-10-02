package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.BreedFounder;
import com.example.horsegenetics.common.breed.BreedLineage;
import com.example.horsegenetics.common.breed.Breeds;
import com.example.horsegenetics.common.coat.CoatData;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.name.HorseNameGenerator.NameParts;
import com.example.horsegenetics.common.genetics.genes.PassificationGene;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.data.HorseCareAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.data.PassificationAttachment;
import com.example.horsegenetics.neoforge.network.CoatSyncPayload;
import com.example.horsegenetics.neoforge.network.HorseCareSyncPayload;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <b>A vanilla zombie or skeleton horse becomes one of this mod's horses</b> the
 * first time it is ticked (undead treatment section 6). A permanent adapter, not a
 * one-shot migration: old saves, skeleton traps, {@code /summon} and spawn eggs all
 * keep producing vanilla undead horses, and every one of them arrives here.
 *
 * <h2>How the swap is made</h2>
 * The undead horse is <b>saved whole</b> ({@code saveWithoutId}) and the tag loaded
 * into a fresh {@code minecraft:horse}. The two share {@link AbstractHorse}'s save
 * format, so everything that identifies the animal comes across without a line of
 * copying here: its UUID, where it stands, its age, health, custom name, tame flag
 * and owner, saddle and armour, its lead, scoreboard tags, persistence, fire, and
 * every other mod's persistent data. What came from the entity <i>type</i> - its
 * skin, stats and sounds - is re-derived from a genome rolled from the breed its
 * pool names (the resolved-not-rolled rule; D4). Only the health <b>fraction</b>
 * is kept.
 *
 * <h2>Order, and the failure policy</h2>
 * The old entity is discarded <b>before</b> the new one is added, because one UUID
 * cannot be held by two live entities (the reason {@code HorseStasisHandler} has
 * {@code alreadyLoose}). If the add is refused, a vanilla copy is rebuilt from the
 * same tag and put back, so the failure mode is "the horse is still there and still
 * vanilla", never "the horse is gone" (6.7). {@code discard} rather than
 * {@code kill}: no death, no drops, no death notice. Each conversion is seeded from
 * the UUID, so a replay after a crash makes the same horse.
 *
 * <h2>When it waits</h2>
 * Only on the entity tick, under the founding budget, like
 * {@link HorseFoundingTickHandler} - writing {@code Attributes.SCALE} anywhere else
 * has crashed the chunk system before (3.2). And not while a player is riding it
 * (D8), not while it is an unsprung skeleton trap (D5), never a dead or dying one,
 * never a modded subclass (exact types only, D7), never one a debug pen marked
 * {@link #KEEP_VANILLA}, and never at all with {@code undead.convert} off.
 */
@EventBusSubscriber
public final class UndeadHorseConverter {

    /** A persistent-data flag that keeps a vanilla undead horse vanilla - for a debug pen's control case. */
    public static final String KEEP_VANILLA = "horsegenetics:keep_vanilla";

    /** Tries per entity per level load before it is left vanilla for the session. */
    private static final int MAX_TRIES = 5;

    /**
     * <b>The undead adapters</b>: which vanilla type converts into which pool. The
     * extension point D7 asks for - a patch for another mod's undead horse is one more
     * entry here, behind its own switch. Exact types only: a subclass is somebody
     * else's horse.
     */
    private static final Map<EntityType<?>, String> POOLS = new LinkedHashMap<>();

    static {
        POOLS.put(EntityType.SKELETON_HORSE, "skeleton");
        POOLS.put(EntityType.ZOMBIE_HORSE, "zombie");
    }

    private static final IntOpenHashSet HANDLED = new IntOpenHashSet();
    private static final Int2IntOpenHashMap TRIES = new Int2IntOpenHashMap();
    private static final java.util.Set<String> EMPTY_POOLS_REPORTED = new java.util.HashSet<>();

    private UndeadHorseConverter() {
    }

    /** The pool a vanilla entity type converts into, or {@code null} for anything this does not touch. */
    public static String poolOf(EntityType<?> type) {
        return POOLS.get(type);
    }

    @SubscribeEvent
    static void tick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        String pool = POOLS.get(entity.getType());
        if (pool == null || !(entity instanceof AbstractHorse undead)
                || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (!ServerConfig.undeadConvert() || HANDLED.contains(entity.getId())) {
            return;
        }
        if (!readyToConvert(undead)) {
            return; // asked again next tick
        }
        if (!HorseFoundingTickHandler.mayFound(level)) {
            return;
        }
        long started = System.nanoTime();
        try {
            if (convert(undead, level, pool) != null) {
                HANDLED.add(entity.getId());
            } else {
                retryLater(undead, "it could not be placed");
            }
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[undead] could not convert {} at {}", undead.getType(), undead.blockPosition(), e);
            retryLater(undead, e.toString());
        }
        HorseFoundingTickHandler.charge(level, System.nanoTime() - started);
    }

    /**
     * Everything that says "not now" (6.2) or "never" (6.1). A "never" is marked
     * handled so the question is not asked of it every tick.
     */
    static boolean readyToConvert(AbstractHorse undead) {
        if (undead.isRemoved() || !undead.isAlive() || undead.deathTime > 0) {
            return false; // a corpse is not resurrected as a fresh horse
        }
        if (undead.getPersistentData().getBooleanOr(KEEP_VANILLA, false)) {
            HANDLED.add(undead.getId());
            return false;
        }
        if (undead instanceof SkeletonHorse skeleton && skeleton.isTrap()) {
            return false; // the trap is vanilla's; only the horse it leaves behind is ours (D5)
        }
        for (Entity passenger : undead.getPassengers()) {
            if (passenger instanceof Player) {
                return false; // converting under a rider desyncs them; wait for them to get off (D8)
            }
        }
        return true;
    }

    private static void retryLater(AbstractHorse undead, String why) {
        int tries = TRIES.addTo(undead.getId(), 1) + 1;
        if (tries >= MAX_TRIES) {
            HANDLED.add(undead.getId());
            HorseGenetics.LOGGER.warn("[undead] gave up converting {} at {} after {} tries ({}) - it stays vanilla "
                    + "until the level reloads", undead.getType(), undead.blockPosition(), tries, why);
        }
    }

    /**
     * The swap itself. Returns the new horse, or {@code null} when nothing changed
     * (an empty pool, a refused add that was rolled back).
     */
    static Horse convert(AbstractHorse undead, ServerLevel level, String pool) {
        UUID uuid = undead.getUUID();
        SeededRng rng = new SeededRng(uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits(),
                "undead-conversion");
        Breed breed = pickBreed(pool, rng);
        if (breed == null) {
            if (EMPTY_POOLS_REPORTED.add(pool)) {
                HorseGenetics.LOGGER.warn("[undead] no breed is in the '{}' pool (\"undead_of\") - vanilla {} horses "
                        + "are left as they are", pool, pool);
            }
            HANDLED.add(undead.getId());
            return null;
        }

        // Snapshot, read only. The health fraction now: the stats are about to change.
        float fraction = undead.getMaxHealth() > 0f ? undead.getHealth() / undead.getMaxHealth() : 1f;
        boolean tamed = undead.isTamed();
        EntityReference<LivingEntity> ownerRef = undead.getOwnerReference();
        UUID owner = ownerRef == null ? null : ownerRef.getUUID();
        String customName = undead.hasCustomName() ? undead.getCustomName().getString() : null;
        CompoundTag tag = save(undead);
        List<Entity> riders = new ArrayList<>(undead.getPassengers());

        Horse horse = EntityType.HORSE.create(level, EntitySpawnReason.CONVERSION);
        if (horse == null) {
            return null;
        }
        if (!load(horse, tag, undead)) {
            return null;
        }
        found(horse, breed, rng, customName, owner, fraction);
        if (tamed && owner != null) {
            arrivePassified(horse, owner, level.getGameTime());
        }
        HorseCareAttachment care = horse.getData(ModAttachments.HORSE_CARE.get());

        // The swap. The lead is let go without its item first: the loaded tag holds
        // the same lead, so dropping it as well would hand the player a second one.
        if (undead.isLeashed()) {
            undead.removeLeash();
        }
        undead.ejectPassengers();
        undead.discard();
        if (!level.addFreshEntity(horse)) {
            restoreVanilla(undead, tag, level, riders);
            return null;
        }
        for (Entity rider : riders) {
            // UNVERIFIED in a game: that a non-player rider remounts cleanly with force on.
            rider.startRiding(horse, true, true);
        }
        PacketDistributor.sendToPlayersTrackingEntity(horse,
                CoatSyncPayload.of(horse.getId(), new CoatData(HorseRecords.of(horse).genome())));
        PacketDistributor.sendToPlayersTrackingEntity(horse,
                new HorseCareSyncPayload(horse.getId(), care.bond(), care.inHerd()));
        DebugAnnounce.log("Undead", "a vanilla " + pool + " horse at " + horse.blockPosition().toShortString()
                + " became " + HorseRecords.of(horse).displayName() + ", a " + breed.name());
        return horse;
    }

    /** One breed of the pool, uniform and seeded, so a replay picks the same one. */
    static Breed pickBreed(String pool, SeededRng rng) {
        List<Breed> members = new ArrayList<>();
        for (Breed b : Breeds.all()) {
            if (pool.equals(b.undeadOf())) {
                members.add(b);
            }
        }
        if (members.isEmpty()) {
            return null;
        }
        return members.get(Math.min(members.size() - 1, (int) (rng.nextFloat() * members.size())));
    }

    /**
     * Give the new horse its genome, record, body and care. A player's own name for
     * the animal is kept as its barn name - what the nameplate shows - over a
     * registered name rolled like any founder's.
     */
    private static void found(Horse horse, Breed breed, SeededRng rng, String customName, UUID owner, float fraction) {
        Sex sex = rng.nextFloat() < 0.5f ? Sex.MALE : Sex.FEMALE;
        Genome genome = BreedFounder.roll(breed, rng, sex);
        BreedFounderLog.founder(breed, genome.genotype(), "converted vanilla undead horse");
        NameParts name = HorseRecords.newNameParts(rng);
        HorseRecord record = HorseRecord.founder(horse.getUUID(), name.first(), name.last(), genome,
                BreedLineage.pure(breed.id()).toToken());
        if (customName != null && !customName.isBlank()) {
            record = record.withBarnName(java.util.Optional.of(customName));
        }
        if (owner != null) {
            record = record.withOwner(owner);
        }
        HorseRecords.apply(horse, record);
        HorseRecords.applyTraitsToEntity(horse, record, false);
        horse.setHealth(Math.max(1f, fraction * horse.getMaxHealth()));
        // Bond starts at zero (D8): the attachment's own default, and a converted horse
        // never had one. Fed, as a released stasis horse is.
        horse.setData(ModAttachments.HORSE_CARE.get(), HorseCareAttachment.DEFAULT);
    }

    /**
     * A horse the player already owned does not turn on them after dark: every
     * passification route it has is settled for good, for its owner (D25).
     */
    private static void arrivePassified(Horse horse, UUID owner, long now) {
        List<PassificationGene.Route> routes = Passification.routesOf(horse);
        if (routes.isEmpty()) {
            return;
        }
        PassificationAttachment state = horse.getData(ModAttachments.PASSIFICATION.get());
        for (PassificationGene.Route route : routes) {
            state = state.settle(owner, route.item(), PassificationAttachment.FOREVER, now);
        }
        horse.setData(ModAttachments.PASSIFICATION.get(), state);
    }

    /** Put a vanilla copy back where the conversion failed - the horse is never lost. */
    private static void restoreVanilla(AbstractHorse undead, CompoundTag tag, ServerLevel level, List<Entity> riders) {
        Entity copy = undead.getType().create(level, EntitySpawnReason.LOAD);
        if (copy instanceof AbstractHorse back && load(back, tag, undead) && level.addFreshEntity(back)) {
            for (Entity rider : riders) {
                rider.startRiding(back, true, true);
            }
            HorseGenetics.LOGGER.warn("[undead] the converted horse at {} was refused by the level; the vanilla "
                    + "{} was put back", undead.blockPosition(), undead.getType());
            return;
        }
        HorseGenetics.LOGGER.error("[undead] could neither convert nor restore the {} at {} (uuid {})",
                undead.getType(), undead.blockPosition(), undead.getUUID());
    }

    private static CompoundTag save(AbstractHorse horse) {
        try (ProblemReporter.ScopedCollector reporter =
                     new ProblemReporter.ScopedCollector(horse.problemPath(), HorseGenetics.LOGGER)) {
            TagValueOutput out = TagValueOutput.createWithContext(reporter, horse.registryAccess());
            horse.saveWithoutId(out);
            return out.buildResult();
        }
    }

    private static boolean load(AbstractHorse into, CompoundTag tag, AbstractHorse from) {
        try (ProblemReporter.ScopedCollector reporter =
                     new ProblemReporter.ScopedCollector(from.problemPath(), HorseGenetics.LOGGER)) {
            // Persistence comes across in the tag like everything else: a wild
            // vanilla undead horse may still despawn as one would.
            into.load(TagValueInput.create(reporter, from.level().registryAccess(), tag));
            return true;
        } catch (RuntimeException bad) {
            HorseGenetics.LOGGER.warn("[undead] could not read the {} at {} into a horse", from.getType(),
                    from.blockPosition(), bad);
            return false;
        }
    }

    @SubscribeEvent
    static void onLeave(EntityLeaveLevelEvent event) {
        if (POOLS.containsKey(event.getEntity().getType())) {
            HANDLED.remove(event.getEntity().getId());
            TRIES.remove(event.getEntity().getId());
        }
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        HANDLED.clear();
        TRIES.clear();
        EMPTY_POOLS_REPORTED.clear();
    }
}
