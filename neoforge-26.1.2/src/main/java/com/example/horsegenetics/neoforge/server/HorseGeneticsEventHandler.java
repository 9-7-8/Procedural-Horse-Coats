package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.coat.CoatData;
import com.example.horsegenetics.common.coat.CoatGenerator;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.NeoRng;
import com.example.horsegenetics.neoforge.data.HorseAncestryData;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.network.CoatSyncPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Natural-spawn side of horse genetics. When a Horse joins a level without a
 * real {@link HorseRecord} yet (wild spawn, {@code /summon}, debug pens, an
 * old save), it gets a founder record: a fresh random genotype, a random
 * {@link com.example.horsegenetics.common.horse.Sex}, and a generated name.
 * The coat attachment is then always derived from that record's genetic code,
 * so wild and bred horses stay consistent. Breeding is handled separately in
 * {@link HorseBreedingHandler}.
 */
@EventBusSubscriber
public final class HorseGeneticsEventHandler {

    /**
     * The debug-pen dimension is horses-only: cancel any other mob trying to
     * join it (villagers, monsters, other animals). Natural monster spawns are
     * already suppressed by {@code monster_spawn_light_level: 0} in the
     * dimension type and {@code structure_overrides: []} in the dimension, but
     * this also catches spawn eggs, {@code /summon}, breeding, etc.
     */
    @SubscribeEvent
    static void keepDebugDimensionHorsesOnly(EntityJoinLevelEvent event) {
        // RESTORE BEFORE 1.0.
        //
        // This cancelled every non-horse Mob joining the horse dimension, which
        // is what kept the gallery a gallery. It also made two of the test
        // yard's tests impossible: the holy ward's whole claim is that a mob
        // farm KEEPS PRODUCING near a warded horse, and the pack leader's is
        // about other creatures following one - and both were being judged
        // somewhere that deleted zombies and cows on arrival.
        //
        // Off entirely by the owner's call (2026-09-12) while the behaviour
        // genes are being tested. What it wants on the way back is a rule that
        // distinguishes the corridor from the yard rather than one that treats
        // the whole dimension as a display case; the geometry for that is
        // DebugPenManager.corridorWallZ(). Tracked in wiki/known-gaps.html.
        if (event.getLevel().isClientSide()) {
            return;
        }
    }

    /**
     * <b>The horse dimension is a fixed set - unless you are the one testing
     * it.</b>
     *
     * <p>These two rules treated the whole dimension as a display case, for
     * everybody, with no exception. Owner, 2026-09-13: <i>"can't place
     * anything, so can't test stalls"</i> - and that is not a corner case, it
     * is <b>two of the open questions</b>. The ticket stalls in row C are built
     * by hand by definition (tight, L-shaped, packed with hay, roofed), and the
     * dark-oak control for gap 218 is "plant four saplings in a 2x2 yourself".
     * A yard you cannot build in cannot host either.
     *
     * <p><b>Creative only</b>, which is the whole of the fix. The rule exists so
     * that a horse, a mob, a piece of dispenser plumbing or a survival-mode
     * accident cannot rewrite a gallery that is generated rather than saved -
     * and every one of those is still stopped. The tester is in creative in
     * this world always, and is the one person who is supposed to be able to
     * change it.
     *
     * <p>This is the narrower rule the sibling comment above was already asking
     * for, arrived at from the other direction: distinguish <i>who</i> rather
     * than <i>where</i>, which needs no geometry and cannot drift out of step
     * with {@code corridorWallZ()}.
     */
    @SubscribeEvent
    static void noBlockBreakInDebugDimension(BreakBlockEvent event) {
        if (event.getPlayer() != null
                && !event.getPlayer().getAbilities().instabuild
                && event.getPlayer().level().dimension().equals(DebugPenManager.DEBUG_LEVEL)) {
            event.setNotifyClient(true);
            event.setCanceled(true);
        }
    }

    /** ...and the same for placing (covers EntityMultiPlaceEvent via inheritance). */
    @SubscribeEvent
    static void noBlockPlaceInDebugDimension(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player
                && player.getAbilities().instabuild) {
            return;     // the tester, in creative - see noBlockBreakInDebugDimension
        }
        if (event.getLevel() instanceof Level level
                && level.dimension().equals(DebugPenManager.DEBUG_LEVEL)) {
            event.setCanceled(true);
        }
    }

    /**
     * A reloaded or freshly-bred horse that already has a record: register it in
     * the ancestry DB, show its name, fill an epigenome if it predates one, and
     * <b>re-resolve its body</b> onto the entity's attributes.
     *
     * <p>Called from {@link HorseFoundingTickHandler} on the entity tick, never
     * from the join event or a {@code server.execute} task - the
     * {@code Attributes.SCALE} write inside {@link HorseRecords#applyTraitsToEntity}
     * re-enters the chunk system from those contexts and crashes
     * {@code DistanceManager.runAllUpdates}. See that handler.
     */
    static void ensureExistingRecordResolved(Horse horse) {
        NeoRng rng = new NeoRng(horse.getRandom());
        if (horse.level() instanceof ServerLevel level) {
            // bred (BabyEntitySpawnEvent already set the attachment) or reloaded:
            // make sure the global DB knows it and the floating name is showing
            HorseRecord known = HorseRecords.of(horse);
            HorseAncestryData.get(level.getServer()).record(known);
            if (horse.getCustomName() == null) {
                horse.setCustomName(Component.literal(known.displayName()));
                horse.setCustomNameVisible(true);
            }
        }

        // A foal already has its epigenome: HorseBreedingHandler built one from
        // the parents' genomes, so the alleles it inherited keep their epigenetics.
        // Anything else (wild spawn, /summon, gallery horse) founds its own.
        HorseRecord record = HorseRecords.of(horse);
        if (!record.hasGenome()) {
            // rolls the epigenome once, against the genotype the record already carries
            record = record.withGenome(CoatGenerator.generate(record.genotype(), rng).genome());
            HorseRecords.apply(horse, record);
        }

        // Re-resolve the body from the genotype (a reloaded horse, or one whose
        // gene weights / health.mode changed). Only clamp HP down, never heal.
        HorseRecords.applyTraitsToEntity(horse, record, false);

        syncToTrackers(horse, new CoatData(record.genome()));
    }


    /** Re-send coat + record data whenever a player starts tracking a horse (e.g. walks into range). */
    @SubscribeEvent
    static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof Horse horse)) return;
        ServerPlayer player = (ServerPlayer) event.getEntity();

        HorseRecord record = HorseRecords.of(horse);
        if (record.hasGenome()) {
            PacketDistributor.sendToPlayer(player,
                    CoatSyncPayload.of(horse.getId(), new CoatData(record.genome())));
        }
        if (HorseRecords.hasRealRecord(horse)) {
            PacketDistributor.sendToPlayer(player,
                    new com.example.horsegenetics.neoforge.network.HorseRecordSyncPayload(horse.getId(), HorseRecords.of(horse)));
        }
        com.example.horsegenetics.neoforge.data.HorseCareAttachment care =
                horse.getData(ModAttachments.HORSE_CARE.get());
        if (care != null) {
            PacketDistributor.sendToPlayer(player,
                    new com.example.horsegenetics.neoforge.network.HorseCareSyncPayload(
                            horse.getId(), care.bond(), care.inHerd()));
        }
    }

    private static void syncToTrackers(Horse horse, CoatData coatData) {
        PacketDistributor.sendToPlayersTrackingEntity(horse, CoatSyncPayload.of(horse.getId(), coatData));
    }

    private HorseGeneticsEventHandler() {
    }
}
