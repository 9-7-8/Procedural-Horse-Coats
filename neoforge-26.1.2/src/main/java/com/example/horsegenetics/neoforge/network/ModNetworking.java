package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.client.ClientCoatCache;
import com.example.horsegenetics.neoforge.client.ClientHorseRecordCache;
import com.example.horsegenetics.neoforge.data.HorseAncestryData;
import com.example.horsegenetics.neoforge.server.DebugPenManager;
import com.example.horsegenetics.neoforge.server.HorseRecords;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
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
