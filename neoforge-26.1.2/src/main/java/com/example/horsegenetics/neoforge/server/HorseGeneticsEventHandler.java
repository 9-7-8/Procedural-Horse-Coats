package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.coat.CoatData;
import com.example.horsegenetics.common.coat.CoatGenerator;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.NeoRng;
import com.example.horsegenetics.neoforge.data.HorseAncestryData;
import com.example.horsegenetics.neoforge.data.HorseCoatAttachment;
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

        if (!HorseRecords.hasRealRecord(horse)) {
            // wild / imported horse - found a new line
            HorseRecords.apply(horse, HorseRecords.newFounder(horse, rng));
        } else if (horse.level() instanceof ServerLevel level) {
            // bred (BabyEntitySpawnEvent already set the attachment) or reloaded:
            // make sure the global DB knows it, the floating name is showing, and
            // the speed/health stats are filled in (older records predate them).
            HorseRecords.backfillStatsIfMissing(horse);
            HorseRecord record = HorseRecords.of(horse);
            HorseAncestryData.get(level.getServer()).record(record);
            HorseRecords.applyStatsToEntity(horse, record, false);
            if (horse.getCustomName() == null) {
                horse.setCustomName(Component.literal(record.displayName()));
                horse.setCustomNameVisible(true);
            }
        }

        HorseCoatAttachment coat = horse.getData(ModAttachments.HORSE_COAT.get());
        if (coat == null || coat.isUnassigned()) {
            Genotype genotype = Genotype.parse(HorseRecords.of(horse).geneticCode());
            CoatData coatData = CoatGenerator.generate(genotype, rng); // rolls the epigenetic seed once
            horse.setData(ModAttachments.HORSE_COAT.get(), HorseCoatAttachment.from(coatData));
            syncToTrackers(horse, coatData);
        }
    }

    /** Re-send coat + record data whenever a player starts tracking a horse (e.g. walks into range). */
    @SubscribeEvent
    static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof Horse horse)) return;
        ServerPlayer player = (ServerPlayer) event.getEntity();

        HorseCoatAttachment coat = horse.getData(ModAttachments.HORSE_COAT.get());
        if (coat != null) {
            PacketDistributor.sendToPlayer(player, CoatSyncPayload.of(horse.getId(), coat.coatData()));
        }
        if (HorseRecords.hasRealRecord(horse)) {
            PacketDistributor.sendToPlayer(player,
                    new com.example.horsegenetics.neoforge.network.HorseRecordSyncPayload(horse.getId(), HorseRecords.of(horse)));
        }
    }

    private static void syncToTrackers(Horse horse, CoatData coatData) {
        PacketDistributor.sendToPlayersTrackingEntity(horse, CoatSyncPayload.of(horse.getId(), coatData));
    }

    private HorseGeneticsEventHandler() {
    }
}
