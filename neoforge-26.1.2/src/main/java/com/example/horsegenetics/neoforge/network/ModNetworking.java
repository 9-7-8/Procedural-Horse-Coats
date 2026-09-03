package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.common.genetics.GeneCodeDisplay;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.NeoRng;
import com.example.horsegenetics.neoforge.client.ClientCoatCache;
import com.example.horsegenetics.neoforge.client.ClientHorseRecordCache;
import com.example.horsegenetics.neoforge.data.HorseAncestryData;
import com.example.horsegenetics.neoforge.item.ModItems;
import com.example.horsegenetics.neoforge.server.DebugPenManager;
import com.example.horsegenetics.neoforge.server.HorseRecords;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber
public final class ModNetworking {

    private static final int FAMILY_TREE_DEPTH = 3; // great-grandparents

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(
                CoatSyncPayload.TYPE,
                CoatSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        ClientCoatCache.put(payload.entityId(), payload.coatData()))
        );

        registrar.playToClient(
                HorseRecordSyncPayload.TYPE,
                HorseRecordSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        ClientHorseRecordCache.put(payload.entityId(), payload.record()))
        );

        registrar.playToClient(
                com.example.horsegenetics.neoforge.network.HorseCareSyncPayload.TYPE,
                com.example.horsegenetics.neoforge.network.HorseCareSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        com.example.horsegenetics.neoforge.client.ClientHorseCareCache.put(
                                payload.entityId(), payload.bond(), payload.inHerd()))
        );

        registrar.playToClient(
                FamilyTreeDataPayload.TYPE,
                FamilyTreeDataPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        ClientHorseRecordCache.acceptTreeData(payload.records()))
        );

        registrar.playToServer(
                FamilyTreeRequestPayload.TYPE,
                FamilyTreeRequestPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> handleFamilyTreeRequest(payload, context.player()))
        );

        registrar.playToServer(
                SetBarnNamePayload.TYPE,
                SetBarnNamePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> handleSetBarnName(payload, context.player()))
        );

        registrar.playToServer(
                RequestDebugPensPayload.TYPE,
                RequestDebugPensPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    // Re-checked here independently of the client-side keybind gate -
                    // a forged packet against a production server should still no-op.
                    if (FMLEnvironment.isProduction()) return;
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        DebugPenManager.teleportAndGenerate(serverPlayer);
                    }
                })
        );

        registrar.playToServer(
                SpawnCustomHorsePayload.TYPE,
                SpawnCustomHorsePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> handleSpawnCustomHorse(payload, context.player()))
        );

        registrar.playToServer(
                RequestStallHighlightPayload.TYPE,
                RequestStallHighlightPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (FMLEnvironment.isProduction()) return; // dev-only debug overlay
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        com.example.horsegenetics.neoforge.server.StallDebug.highlight(serverPlayer);
                    }
                })
        );
    }

    private static void handleSpawnCustomHorse(SpawnCustomHorsePayload payload,
                                               net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!(serverPlayer.level() instanceof ServerLevel level)) {
            return;
        }

        Genotype genotype;
        try {
            genotype = Genotype.parse(payload.genotypeCode());
        } catch (RuntimeException e) {
            serverPlayer.sendSystemMessage(Component.literal("[Custom Horse] rejected genome: " + e.getMessage()));
            return;
        }
        String code = genotype.toCode();

        HitResult hit = serverPlayer.pick(6.0, 1.0F, false);
        Vec3 pos = hit.getType() != HitResult.Type.MISS
                ? hit.getLocation()
                : serverPlayer.position().add(serverPlayer.getLookAngle().scale(2.0));

        Horse horse = EntityType.HORSE.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (horse == null) {
            return;
        }
        horse.snapTo(pos.x, pos.y, pos.z, serverPlayer.getYRot(), 0.0F);
        if (payload.baby()) {
            horse.setBaby(true);
        }
        horse.setPersistenceRequired();

        Sex sex = payload.female() ? Sex.FEMALE : Sex.MALE;
        // Record applied before the entity joins, so HorseGeneticsEventHandler
        // sees a real record and keeps this genome instead of rolling a random
        // one. The epigenome is still rolled fresh, like any founder.
        HorseRecords.apply(horse,
                HorseRecords.newFounder(horse, new NeoRng(horse.getRandom()), sex, Genotype.parse(code)));
        level.addFreshEntity(horse);

        if (!serverPlayer.getAbilities().instabuild) {
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack held = serverPlayer.getItemInHand(hand);
                if (held.is(ModItems.CUSTOM_HORSE_SPAWN_EGG.get())) {
                    held.shrink(1);
                    break;
                }
            }
        }

        serverPlayer.sendSystemMessage(Component.literal("[Custom Horse] spawned "
                + (payload.baby() ? "foal " : "") + sex.label(!payload.baby()) + " - "
                + GeneCodeDisplay.shortForm(genotype)));
    }

    private static void handleSetBarnName(SetBarnNamePayload payload, net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        Entity target = serverPlayer.level().getEntity(payload.entityId());
        if (target instanceof Horse horse && horse.closerThan(serverPlayer, 8.0)) {
            HorseRecords.setBarnName(horse, payload.barnName());
        }
    }

    private static void handleFamilyTreeRequest(FamilyTreeRequestPayload payload, net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        MinecraftServer server = serverPlayer.level().getServer();
        if (server == null) {
            return;
        }
        HorseAncestryData db = HorseAncestryData.get(server);
        List<HorseRecord> tree = new ArrayList<>();
        db.lookup(payload.rootId()).ifPresent(tree::add);
        tree.addAll(db.ancestorsOf(payload.rootId(), FAMILY_TREE_DEPTH));
        PacketDistributor.sendToPlayer(serverPlayer, new FamilyTreeDataPayload(tree));
    }

    private ModNetworking() {
    }
}
