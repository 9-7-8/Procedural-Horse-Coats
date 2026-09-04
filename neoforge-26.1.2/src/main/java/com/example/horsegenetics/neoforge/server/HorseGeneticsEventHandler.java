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
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
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
        if (event.getLevel().isClientSide()) return;
        if (!event.getLevel().dimension().equals(DebugPenManager.DEBUG_LEVEL)) return;
        if (event.getEntity() instanceof Mob && !(event.getEntity() instanceof Horse)) {
            event.setCanceled(true);
        }
    }

    /** Horses can't be injured in the debug-pen dimension - it's a viewing gallery, not a fight. */
    @SubscribeEvent
    static void noHorseDamageInDebugDimension(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof AbstractHorse
                && event.getEntity().level().dimension().equals(DebugPenManager.DEBUG_LEVEL)) {
            event.setCanceled(true);
        }
    }

    /** The horse dimension is a fixed set - no breaking blocks there. */
    @SubscribeEvent
    static void noBlockBreakInDebugDimension(BreakBlockEvent event) {
        if (event.getPlayer() != null
                && event.getPlayer().level().dimension().equals(DebugPenManager.DEBUG_LEVEL)) {
            event.setNotifyClient(true);
            event.setCanceled(true);
        }
    }

    /** ...and no placing blocks there either (covers EntityMultiPlaceEvent via inheritance). */
    @SubscribeEvent
    static void noBlockPlaceInDebugDimension(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof Level level
                && level.dimension().equals(DebugPenManager.DEBUG_LEVEL)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onHorseJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Horse horse)) return;
        ensureRecordAndCoat(horse);
    }

    private static void ensureRecordAndCoat(Horse horse) {
        NeoRng rng = new NeoRng(horse.getRandom());

        boolean founded = !HorseRecords.hasRealRecord(horse);
        if (founded) {
            // wild / imported horse - found a new line
            HorseRecords.apply(horse, HorseRecords.newFounder(horse, rng));
        } else if (horse.level() instanceof ServerLevel level) {
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

        // Resolve the body from the genotype, every join, for every horse -
        // vanilla has just randomised this horse's speed, health and jump, and
        // this is where that gets overwritten with what its alleles actually
        // say. Doing it on every join rather than storing it is what lets a
        // re-tuned gene, or a change to the server's health.mode, reach horses
        // that already exist. A newly founded horse is healed to its new max;
        // a reloaded one is only clamped down, so nothing is healed for free.
        HorseRecords.applyTraitsToEntity(horse, record, founded);

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
