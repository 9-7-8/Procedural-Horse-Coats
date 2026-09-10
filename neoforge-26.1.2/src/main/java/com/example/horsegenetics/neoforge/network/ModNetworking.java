package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.common.genetics.GeneCodeDisplay;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genome;
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
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
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

        registrar.playToClient(
                GeneDatabaseSyncPayload.TYPE,
                GeneDatabaseSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        com.example.horsegenetics.neoforge.client.ClientGeneDatabase.accept(
                                payload.seenByGene(), payload.carrotUnlocked(), payload.collected(),
                                payload.breeds()))
        );

        registrar.playToClient(
                OpenHorseRenamePayload.TYPE,
                OpenHorseRenamePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        com.example.horsegenetics.neoforge.client.HorseRenameScreen.open(payload.entityId()))
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
                RenameHorsePayload.TYPE,
                RenameHorsePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> handleRenameHorse(payload, context.player()))
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

        registrar.playToServer(
                RequestHighlightHorsesPayload.TYPE,
                RequestHighlightHorsesPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (FMLEnvironment.isProduction()) return; // dev-only find-my-horses toggle
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        com.example.horsegenetics.neoforge.server.DebugHighlightHandler.toggle(serverPlayer);
                    }
                })
        );

        registrar.playToClient(
                ProgressSyncPayload.TYPE,
                ProgressSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        com.example.horsegenetics.neoforge.client.ClientProgress.accept(payload.done()))
        );

        registrar.playToClient(
                ShelfSyncPayload.TYPE,
                ShelfSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (net.minecraft.client.Minecraft.getInstance().player != null
                            && net.minecraft.client.Minecraft.getInstance().player.containerMenu
                                    instanceof com.example.horsegenetics.neoforge.menu.ResearchShelfMenu menu) {
                        menu.acceptStored(payload.geneKeys());
                    }
                })
        );

        registrar.playToServer(
                ShelfActionPayload.TYPE,
                ShelfActionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    // The menu is the authority on which shelf, and it re-checks
                    // the gene against the block entity - so a forged packet can
                    // only ask for something absent, and get nothing.
                    if (context.player() instanceof ServerPlayer serverPlayer
                            && serverPlayer.containerMenu
                                    instanceof com.example.horsegenetics.neoforge.menu.ResearchShelfMenu menu) {
                        switch (payload.action()) {
                            case SELECT -> menu.selectGene(payload.geneKey());
                            case WITHDRAW -> menu.withdraw(payload.geneKey());
                        }
                    }
                })
        );

        registrar.playToClient(
                HorseRosterPayload.TYPE,
                HorseRosterPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        com.example.horsegenetics.neoforge.client.ClientHorseRoster.accept(payload.entries()))
        );

        registrar.playToServer(
                HorseRosterRequestPayload.TYPE,
                HorseRosterRequestPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        com.example.horsegenetics.neoforge.server.HorseRoster.sendTo(serverPlayer);
                    }
                })
        );

        registrar.playToClient(
                OffspringDataPayload.TYPE,
                OffspringDataPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        com.example.horsegenetics.neoforge.client.ClientOffspring.accept(payload))
        );

        registrar.playToServer(
                OffspringRequestPayload.TYPE,
                OffspringRequestPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        handleOffspringRequest(payload, context.player()))
        );

        registrar.playToClient(
                HorseCoatBatchPayload.TYPE,
                HorseCoatBatchPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        com.example.horsegenetics.neoforge.client.ClientHorseCoats.accept(payload.entries()))
        );

        registrar.playToServer(
                HorseCoatRequestPayload.TYPE,
                HorseCoatRequestPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        com.example.horsegenetics.neoforge.server.HorseCoats.sendTo(
                                serverPlayer, payload.ids());
                    }
                })
        );

        registrar.playToServer(
                InspectHorsePayload.TYPE,
                InspectHorsePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        com.example.horsegenetics.neoforge.server.HorseInspectHold.set(
                                serverPlayer, payload.entityId(), payload.watching());
                    }
                })
        );

    }

    /**
     * <b>Creative only, and checked here.</b> The editor screen opens on the
     * client, but this payload spawns an arbitrary entity carrying an arbitrary
     * genome, so the client-side gate is worth nothing on its own; the sender
     * must be in creative <i>and</i> holding the egg.
     *
     * <p>The epigenome the screen was previewing arrives with the genotype and
     * is written straight into the founder record - so the horse that appears
     * is the horse that was on screen. An empty epigenome code (an older
     * client, or a hand-sent packet) falls back to rolling one.
     */
    private static void handleSpawnCustomHorse(SpawnCustomHorsePayload payload,
                                               net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!(serverPlayer.level() instanceof ServerLevel level)) {
            return;
        }
        // Every rejection below says so, in chat and in the log. They were
        // silent `return`s, which is how "the custom egg spawner also does not
        // seem to be working" arrives with nothing to act on: a click, no
        // horse, no message, no line anywhere. A refusal a player can read is
        // a bug report; a refusal they cannot is a mystery.
        if (!serverPlayer.getAbilities().instabuild) {
            refuseSpawn(serverPlayer, "the custom spawn egg is a creative-mode tool - "
                    + "switch to creative and try again.");
            return;
        }
        if (!holdsSpawnEgg(serverPlayer)) {
            refuseSpawn(serverPlayer, "you are not holding the custom spawn egg any more.");
            return;
        }

        Sex sex = payload.female() ? Sex.FEMALE : Sex.MALE;
        Genome genome;
        try {
            Genotype genotype = Genotype.parse(payload.genotypeCode()).withSex(sex);
            Epigenome epigenome = payload.epigenomeCode().isEmpty()
                    ? Epigenome.random(new NeoRng(serverPlayer.getRandom()))
                    : Epigenome.parse(payload.epigenomeCode());
            genome = new Genome(genotype, epigenome);
        } catch (RuntimeException e) {
            refuseSpawn(serverPlayer, "rejected genome: " + e.getMessage());
            HorseGenetics.LOGGER.warn("[Custom Horse] genome rejected", e);
            return;
        }

        // "Make an egg" instead of "spawn now". Everything above is identical -
        // the same genome is validated the same way - so the two paths cannot
        // drift apart into "the egg spawns a different horse from the button".
        if (payload.asEgg()) {
            ItemStack egg = com.example.horsegenetics.neoforge.item.PresetHorseSpawnEggItem.of(
                    new com.example.horsegenetics.neoforge.data.StoredGenome(
                            genome.genotypeCode(), genome.epigenome().toCode(),
                            serverPlayer.getUUID(), serverPlayer.getGameProfile().name(),
                            payload.breed()),
                    payload.baby());
            if (!serverPlayer.getInventory().add(egg)) {
                serverPlayer.drop(egg, false);
            }
            serverPlayer.sendSystemMessage(
                    Component.translatable("message.horsegenetics.custom_horse.egg_made"));
            return;
        }

        HitResult hit = serverPlayer.pick(6.0, 1.0F, false);
        Vec3 pos = hit.getType() != HitResult.Type.MISS
                ? hit.getLocation()
                : serverPlayer.position().add(serverPlayer.getLookAngle().scale(2.0));

        Horse horse = EntityType.HORSE.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (horse == null) {
            refuseSpawn(serverPlayer, "the game refused to create a horse entity here.");
            return;
        }
        horse.snapTo(pos.x, pos.y, pos.z, serverPlayer.getYRot(), 0.0F);
        if (payload.baby()) {
            horse.setBaby(true);
        }
        horse.setPersistenceRequired();

        // Record applied before the entity joins, so HorseGeneticsEventHandler
        // sees a real record and keeps this genome instead of rolling a random one.
        HorseRecord record = HorseRecords.newFounder(horse, new NeoRng(horse.getRandom()), genome);
        if (!payload.breed().isEmpty()) {
            record = record.withBreed(payload.breed());
        }
        HorseRecords.apply(horse, record);
        level.addFreshEntity(horse);

        serverPlayer.sendSystemMessage(Component.literal("[Custom Horse] spawned "
                + (payload.baby() ? "foal " : "") + sex.label(!payload.baby()) + " "
                + record.lineage().displayName() + " - "
                + GeneCodeDisplay.shortForm(genome.genotype())));
    }

    /** One refusal, said in chat and written to the log. */
    private static void refuseSpawn(ServerPlayer player, String reason) {
        player.sendSystemMessage(Component.literal("[Custom Horse] " + reason));
        HorseGenetics.LOGGER.info("[Custom Horse] refused spawn for {}: {}",
                player.getGameProfile().name(), reason);
    }

    private static boolean holdsSpawnEgg(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).is(ModItems.CUSTOM_HORSE_SPAWN_EGG.get())) {
                return true;
            }
        }
        return false;
    }

    private static void handleSetBarnName(SetBarnNamePayload payload, net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        Entity target = serverPlayer.level().getEntity(payload.entityId());
        if (target instanceof Horse horse && horse.closerThan(serverPlayer, 8.0)) {
            HorseRecords.setBarnName(horse, payload.barnName());
            // Clearing the box sends a blank through here too, and unnaming a
            // horse is not the task - so tick on a name, not on the packet.
            if (!payload.barnName().strip().isEmpty()) {
                com.example.horsegenetics.neoforge.server.HorseProgress.complete(serverPlayer,
                        com.example.horsegenetics.common.progress.ProgressTask.BARN_NAME);
            }
        }
    }

    /**
     * Apply a first / last name from the rename window and consume one name tag.
     * Re-checks range, that a real record exists, that a name tag is still in
     * hand, and that the two parts are not both blank.
     */
    private static void handleRenameHorse(RenameHorsePayload payload, net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        Entity target = serverPlayer.level().getEntity(payload.entityId());
        if (!(target instanceof Horse horse) || !horse.closerThan(serverPlayer, 8.0)
                || !HorseRecords.hasRealRecord(horse)) {
            return;
        }
        String first = payload.firstName().strip();
        String last = payload.lastName().strip();
        if (first.isEmpty() && last.isEmpty()) {
            return;
        }
        InteractionHand tagHand = null;
        for (InteractionHand hand : InteractionHand.values()) {
            if (serverPlayer.getItemInHand(hand).is(net.minecraft.world.item.Items.NAME_TAG)) {
                tagHand = hand;
                break;
            }
        }
        if (tagHand == null) {
            return;
        }
        HorseRecords.rename(horse, first, last);
        com.example.horsegenetics.neoforge.server.HorseProgress.complete(serverPlayer,
                com.example.horsegenetics.common.progress.ProgressTask.NAME_HORSE);
        if (!serverPlayer.getAbilities().instabuild) {
            serverPlayer.getItemInHand(tagHand).shrink(1);
        }
    }

    /**
     * A horse's descendants, generation by generation. Capped in both
     * directions by {@link OffspringDataPayload} - a full record per horse is
     * not cheap - and each generation reports whether it was cut, so the screen
     * can say "and more" rather than showing part of a family as the whole of
     * it.
     */
    private static void handleOffspringRequest(OffspringRequestPayload payload,
                                               net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        MinecraftServer server = serverPlayer.level().getServer();
        if (server == null) {
            return;
        }
        List<List<HorseRecord>> found = HorseAncestryData.get(server)
                .descendantsOf(payload.rootId(), OffspringDataPayload.MAX_GENERATIONS);
        List<OffspringDataPayload.Generation> generations = new ArrayList<>();
        for (List<HorseRecord> generation : found) {
            boolean truncated = generation.size() > OffspringDataPayload.MAX_PER_GENERATION;
            generations.add(new OffspringDataPayload.Generation(
                    truncated
                            ? List.copyOf(generation.subList(0, OffspringDataPayload.MAX_PER_GENERATION))
                            : generation,
                    truncated));
        }
        PacketDistributor.sendToPlayer(serverPlayer,
                new OffspringDataPayload(payload.rootId(), List.copyOf(generations)));
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
