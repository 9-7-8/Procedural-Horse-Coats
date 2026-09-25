package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.ServerConfig;
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
                CowboyFilterPayload.TYPE,
                CowboyFilterPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> handleCowboyFilter(payload, context.player()))
        );

        registrar.playToServer(
                NamingPolicyPayload.TYPE,
                NamingPolicyPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> handleNamingPolicy(payload, context.player()))
        );

        registrar.playToServer(
                RenameHorsePayload.TYPE,
                RenameHorsePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> handleRenameHorse(payload, context.player()))
        );

        registrar.playToServer(
                BenchNamePayload.TYPE,
                BenchNamePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    // The menu is the authority: a forged packet can only rename
                    // a saddle in a bench the sender actually has open, and the
                    // menu re-derives its result from its own slots afterwards.
                    if (context.player() instanceof ServerPlayer serverPlayer
                            && serverPlayer.containerMenu
                                    instanceof com.example.horsegenetics.neoforge.menu.EquestrianBenchMenu bench) {
                        bench.setSaddleName(payload.name());
                    }
                })
        );

        registrar.playToServer(
                RequestDebugPensPayload.TYPE,
                RequestDebugPensPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    // Re-checked here independently of the client-side keybind gate -
                    // a forged packet against a server that has not switched the
                    // tools on should still no-op. The client's debug.tools is the
                    // client's business; this one is the server's.
                    if (!ServerConfig.debugTools()) return;
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
                    if (!ServerConfig.debugTools()) return; // debug overlay, server's call
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        com.example.horsegenetics.neoforge.server.StallDebug.highlight(serverPlayer);
                    }
                })
        );

        registrar.playToServer(
                ToggleDivePayload.TYPE,
                ToggleDivePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    // Gameplay, not a debug tool: ocean-born is unusable without
                    // it, so this ships in a real jar like the highlight key.
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        com.example.horsegenetics.neoforge.server.GeneAbilityHandler
                                .toggleDive(serverPlayer);
                    }
                })
        );

        registrar.playToServer(
                LandPayload.TYPE,
                LandPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        com.example.horsegenetics.neoforge.server.FlightToggle.land(serverPlayer);
                    }
                })
        );

        registrar.playToServer(
                DismountPayload.TYPE,
                DismountPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    // F while flying. Stop the flight first, so the horse is not
                    // left holding isFlyingVehicle() with nobody aboard.
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        com.example.horsegenetics.neoforge.server.FlightToggle.stop(serverPlayer);
                        serverPlayer.stopRiding();
                    }
                })
        );

        registrar.playToServer(
                ToggleFlightPayload.TYPE,
                ToggleFlightPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    // The server has to agree that this horse is flying, even
                    // though the rider's client is what moves it: the floating
                    // vehicle check that would DISCONNECT the rider after 80
                    // ticks asks isFlyingVehicle() server-side.
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        com.example.horsegenetics.neoforge.server.FlightToggle.toggle(serverPlayer);
                    }
                })
        );

        registrar.playToServer(
                RequestHighlightHorsesPayload.TYPE,
                RequestHighlightHorsesPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    // Not production-gated, unlike the debug pens and the stall
                    // overlay: this only makes horses the player can already see
                    // glow, and the bug testers run real jars.
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

        registrar.playToServer(
                ShelfActionPayload.TYPE,
                ShelfActionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    // The menu is the authority on which shelf, and it re-checks
                    // the pair against the block entity - so a forged packet can
                    // only ask for something absent, and get nothing.
                    if (context.player() instanceof ServerPlayer serverPlayer
                            && serverPlayer.containerMenu
                                    instanceof com.example.horsegenetics.neoforge.menu.ResearchShelfMenu menu) {
                        menu.selectTopic(payload.topicToken());
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
                HorseLogPayload.TYPE,
                HorseLogPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        com.example.horsegenetics.neoforge.client.ClientHorseLog.accept(payload.events()))
        );

        registrar.playToServer(
                HorseLogRequestPayload.TYPE,
                HorseLogRequestPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        com.example.horsegenetics.neoforge.server.HorseLog.sendTo(serverPlayer);
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
                PopulationDataPayload.TYPE,
                PopulationDataPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        com.example.horsegenetics.neoforge.client.ClientPopulation.accept(payload))
        );

        registrar.playToServer(
                PopulationRequestPayload.TYPE,
                PopulationRequestPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        handlePopulationRequest(context.player()))
        );

        registrar.playToServer(
                TackSlotPayload.TYPE,
                TackSlotPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> handleTackSlot(payload, context.player()))
        );

        registrar.playToServer(
                MountHorsePayload.TYPE,
                MountHorsePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> handleMountHorse(payload, context.player()))
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
                        if (payload.watching()) {
                            // The Social section rides on the same once-a-second lease.
                            com.example.horsegenetics.neoforge.server.HerdSocialHandler.sendSummary(
                                    serverPlayer, payload.entityId());
                        }
                    }
                })
        );

        registrar.playToClient(
                HorseSocialSyncPayload.TYPE,
                HorseSocialSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        com.example.horsegenetics.neoforge.client.ClientHorseSocialCache.put(payload.entityId(),
                                new com.example.horsegenetics.neoforge.client.ClientHorseSocialCache.Social(
                                        payload.role(), payload.roleDescription(), payload.standing(),
                                        payload.companions(), payload.rival(), payload.breeding())))
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

    /**
     * One click on a tack slot: swap what the horse is wearing there with what
     * the player is holding.
     *
     * <p><b>A swap, not a drag.</b> The information screen is a plain screen
     * with no container behind it and no carried stack, so the hand is the only
     * cursor there is - holding a saddle and clicking the saddle slot puts it
     * on, clicking it empty-handed takes it off. That also makes the whole
     * interaction one packet the server can check in one place, which a set of
     * container clicks would not be.
     *
     * <p>Refused unless the horse is the player's and within reach. The screen
     * opens on <i>any</i> horse - a stranger's, a cowboy's string - so "near
     * enough to read" stopped being "yours to tack up" the day that landed; see
     * {@code handleSetBarnName}, which guards the same gap.
     */
    private static void handleTackSlot(TackSlotPayload payload, net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        com.example.horsegenetics.neoforge.entity.HorseTackSlot tack =
                com.example.horsegenetics.neoforge.entity.HorseTackSlot.byName(payload.slot());
        if (tack == null) {
            return;
        }
        Entity target = serverPlayer.level().getEntity(payload.entityId());
        if (!(target instanceof Horse horse) || !horse.closerThan(serverPlayer, 8.0)
                || !com.example.horsegenetics.neoforge.server.HorseOwnership.isOwner(
                        horse, serverPlayer.getUUID())) {
            return;
        }

        ItemStack held = serverPlayer.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack worn = tack.on(horse);
        boolean putting = tack.accepts(horse, held);
        if (!putting && worn.isEmpty()) {
            return; // nothing to take off and nothing that would go on
        }

        // One call for both backings: the saddle and the barding are real
        // equipment slots, the other seventeen are keys in the HORSE_GEAR
        // attachment, and HorseTackSlot.set is the only place that knows which.
        tack.set(horse, putting ? held.split(1) : ItemStack.EMPTY);
        if (!worn.isEmpty()) {
            // Back to the player, and on the floor at their feet if there is no
            // room - never deleted.
            if (!serverPlayer.getInventory().add(worn)) {
                serverPlayer.drop(worn, false);
            }
        }
        serverPlayer.containerMenu.broadcastChanges();
    }

    /**
     * The Ride button on the information screen. See {@link MountHorsePayload}
     * for why it exists at all.
     *
     * <p>Deliberately <b>not</b> restricted to a horse the player owns: getting
     * on an untamed one and being thrown off is how a horse is tamed, and that
     * is the half of riding this button most has to keep. A branded horse - a
     * cowboy's string - is refused, exactly as every other interaction with one
     * is.
     */
    private static void handleMountHorse(MountHorsePayload payload, net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        Entity target = serverPlayer.level().getEntity(payload.entityId());
        if (!(target instanceof Horse horse) || !horse.closerThan(serverPlayer, 8.0)) {
            return;
        }
        if (horse.isVehicle() || horse.isBaby() || serverPlayer.isPassenger()
                || com.example.horsegenetics.neoforge.server.TransferPaperHandler.isBranded(horse)) {
            return;
        }
        serverPlayer.closeContainer();
        // AbstractHorse.doPlayerRide, by hand. Widening it with an access
        // transformer was the first go and cost the whole build: AT widens the
        // declaration and not the overrides, so TraderLlama's protected one
        // became "weaker access" and Minecraft itself stopped recompiling.
        // These four lines are what it does, and they are all public.
        horse.setEating(false);
        horse.clearStanding();
        serverPlayer.setYRot(horse.getYRot());
        serverPlayer.setXRot(horse.getXRot());
        serverPlayer.startRiding(horse);
    }

    private static void handleSetBarnName(SetBarnNamePayload payload, net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        Entity target = serverPlayer.level().getEntity(payload.entityId());
        // Ownership, and not just range: the info screen opens on any horse now
        // (sneak and use - HorseInfoInteraction), so "close enough to read" and
        // "yours to name" have stopped being the same thing. The screen hides the
        // box on a horse you do not own; this is the half that a crafted packet
        // cannot talk its way past.
        if (target instanceof Horse horse && horse.closerThan(serverPlayer, 8.0)
                && com.example.horsegenetics.neoforge.server.HorseOwnership.isOwner(
                        horse, serverPlayer.getUUID())) {
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
    /**
     * Filter the cowboy's offers to what the player typed, and resend them.
     *
     * <p>Rebuilt on the server and pushed back rather than hidden by the screen,
     * because a trade is an index into this very list - see
     * {@link CowboyFilterPayload}. The player must already have the window open,
     * which is what keeps this from being a way to poke at a merchant across the
     * map.
     */
    /**
     * File this player's foal-naming preference. Server-authoritative by
     * construction: the client only ever says what it would like, and the
     * policy is stored against <i>this</i> player's UUID, so it reaches no
     * horse but their own.
     */
    private static void handleNamingPolicy(NamingPolicyPayload payload,
                                           net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || serverPlayer.level().getServer() == null) {
            return;
        }
        com.example.horsegenetics.neoforge.data.HorseNamingData
                .get(serverPlayer.level().getServer())
                .set(serverPlayer.getUUID(), payload.policy());
    }

    private static void handleCowboyFilter(CowboyFilterPayload payload,
                                           net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!(serverPlayer.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu menu)
                || !(serverPlayer.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }
        // Found through the cowboy rather than through the menu: the merchant a
        // MerchantMenu holds is not exposed, and a cowboy already knows who is at
        // his counter. Only the man this player actually opened is touched.
        com.example.horsegenetics.neoforge.entity.Cowboy cowboy = null;
        for (com.example.horsegenetics.neoforge.entity.Cowboy candidate : level.getEntitiesOfClass(
                com.example.horsegenetics.neoforge.entity.Cowboy.class,
                serverPlayer.getBoundingBox().inflate(16.0))) {
            if (candidate.getTradingPlayer() == serverPlayer) {
                cowboy = candidate;
                break;
            }
        }
        if (cowboy == null || !cowboy.setOfferFilter(payload.query())) {
            return; // not at a counter, or a keystroke that did not alter the text
        }
        cowboy.rebuildOffers(level);
        serverPlayer.sendMerchantOffers(menu.containerId, cowboy.getOffers(), 1, 0, false, false);
    }

    private static void handleRenameHorse(RenameHorsePayload payload, net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        Entity target = serverPlayer.level().getEntity(payload.entityId());
        if (!(target instanceof Horse horse) || !horse.closerThan(serverPlayer, 8.0)
                || !HorseRecords.hasRealRecord(horse)) {
            return;
        }
        // You may read a stranger's horse; you may not rename it. See
        // handleSetBarnName for why this became a live question.
        if (!com.example.horsegenetics.neoforge.server.HorseOwnership.isOwner(horse, serverPlayer.getUUID())) {
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

    /**
     * <b>Every horse in the world, as a graph.</b> The whole ancestry table,
     * stripped to what the family overview draws - see
     * {@link PopulationDataPayload} for why it is not a list of records.
     *
     * <p>Sorted by generation before the cap is applied, so a world too big for
     * one payload loses its newest horses rather than a scatter from
     * everywhere, and what arrives is a map that is complete as far as it goes.
     * The blank sentinels {@code HorseRecord.unassigned} leaves behind are
     * dropped the way the roster drops them: a horse with no genome is a row
     * waiting to be filled in, not an animal.
     *
     * <p>It reads {@code sex()} per horse, which parses that horse's genetic
     * code. That is the one non-trivial cost here and it is why this is a
     * button rather than something the screen asks for as you pan around.
     */
    private static void handlePopulationRequest(net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        MinecraftServer server = serverPlayer.level().getServer();
        if (server == null) {
            return;
        }
        List<HorseRecord> found = new ArrayList<>();
        for (HorseRecord record : HorseAncestryData.get(server).all()) {
            if (record.hasGenome()) {
                found.add(record);
            }
        }
        found.sort(java.util.Comparator.comparingInt(HorseRecord::generation)
                .thenComparing(HorseRecord::displayName, String.CASE_INSENSITIVE_ORDER));
        boolean truncated = found.size() > PopulationDataPayload.MAX_ENTRIES;
        List<HorseRecord> sent = truncated
                ? found.subList(0, PopulationDataPayload.MAX_ENTRIES) : found;
        List<PopulationDataPayload.Entry> entries = new ArrayList<>(sent.size());
        for (HorseRecord record : sent) {
            entries.add(new PopulationDataPayload.Entry(
                    record.id(),
                    record.displayName(),
                    record.sex() == com.example.horsegenetics.common.horse.Sex.FEMALE,
                    record.generation(),
                    record.motherId(),
                    record.fatherId()));
        }
        PacketDistributor.sendToPlayer(serverPlayer,
                new PopulationDataPayload(List.copyOf(entries), truncated));
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
