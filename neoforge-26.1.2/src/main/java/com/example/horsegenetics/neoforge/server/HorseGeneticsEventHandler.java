package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.coat.CoatData;
import com.example.horsegenetics.common.coat.CoatGenerator;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.neoforge.NeoRng;
import com.example.horsegenetics.neoforge.data.HorseCoatAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.network.CoatSyncPayload;
import net.minecraft.world.entity.animal.equine.Horse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Wild-spawn assignment only, for this MVP: any Horse that joins the level
 * without a genuine attachment yet gets a fresh random genotype rolled and
 * a coat generated from it. Breeding-based inheritance (actually combining
 * two parents' alleles) is the natural next step once this is confirmed
 * working end to end.
 */
@EventBusSubscriber
public final class HorseGeneticsEventHandler {

    // Sentinel default from ModAttachments - if we see this exact combination,
    // treat the attachment as "never assigned" rather than a real chestnut horse.
    private static final String UNASSIGNED_CODE = "eeaa";

    @SubscribeEvent
    static void onHorseJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Horse horse)) return;

        HorseCoatAttachment existing = horse.getData(ModAttachments.HORSE_COAT.get());
        boolean needsAssignment = existing == null || isUnassignedSentinel(existing);
        if (needsAssignment) {
            assignRandomCoat(horse);
        }
    }

    // A real "eeaa" chestnut rolled legitimately also matches the sentinel string,
    // but the sentinel's leg height is always exactly 0f and phenotype CHESTNUT,
    // which is indistinguishable from a real chestnut anyway (chestnut has no
    // leg height data), so treating it as "needs assignment" is harmless - worst
    // case we re-roll a horse that would have stayed chestnut regardless.
    private static boolean isUnassignedSentinel(HorseCoatAttachment attachment) {
        return UNASSIGNED_CODE.equals(attachment.genotypeCode());
    }

    private static void assignRandomCoat(Horse horse) {
        NeoRng rng = new NeoRng(horse.getRandom());
        Genotype genotype = Genotype.random(rng);
        CoatData coatData = CoatGenerator.generate(genotype, rng);
        horse.setData(ModAttachments.HORSE_COAT.get(), HorseCoatAttachment.from(genotype, coatData));
        syncToTrackers(horse, coatData);
    }

    /** Re-send coat data whenever a player starts tracking a horse (e.g. walks into range). */
    @SubscribeEvent
    static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof Horse horse)) return;
        HorseCoatAttachment attachment = horse.getData(ModAttachments.HORSE_COAT.get());
        if (attachment == null) return;
        PacketDistributor.sendToPlayer(
                (net.minecraft.server.level.ServerPlayer) event.getEntity(),
                CoatSyncPayload.of(horse.getId(), attachment.coatData())
        );
    }

    private static void syncToTrackers(Horse horse, CoatData coatData) {
        PacketDistributor.sendToPlayersTrackingEntity(horse, CoatSyncPayload.of(horse.getId(), coatData));
    }

    private HorseGeneticsEventHandler() {
    }
}
