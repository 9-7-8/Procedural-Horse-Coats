package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.common.progress.ProgressTask;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.network.ProgressSyncPayload;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * <b>Which of the checklist's tasks each player has done.</b> A server-global
 * {@link SavedData} keyed by player {@link UUID}, beside
 * {@link GeneDatabaseData} and for the same reasons: it belongs to the save
 * rather than to a client, it must survive a relog, and on a shared server two
 * players are learning the mod at different speeds.
 *
 * <h2>It gates nothing</h2>
 * Nothing in the mod asks whether a task is done. The checklist is a teaching
 * aid and a record, not a progression lock - a player who reads the wiki and
 * builds a shelf on their first day is not stopped, they just tick two boxes at
 * once. That is deliberate: the moment a checklist gates content it stops being
 * advice and starts being homework.
 *
 * <h2>Completing is idempotent and says so once</h2>
 * Handlers call {@link #complete} freely - on every tame, every foal, every
 * whistle - so it has to be cheap and silent when nothing changed. The one time
 * it is not silent is the first: a single chat line, because a checklist nobody
 * notices ticking is a checklist nobody opens.
 */
public final class HorseProgressData extends SavedData {

    private record PlayerTasks(UUID player, List<String> done) {
        static final Codec<PlayerTasks> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("player").forGetter(PlayerTasks::player),
                Codec.STRING.listOf().fieldOf("done").forGetter(PlayerTasks::done)
        ).apply(i, PlayerTasks::new));
    }

    public static final Codec<HorseProgressData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.list(PlayerTasks.CODEC).fieldOf("players").forGetter(HorseProgressData::snapshot)
    ).apply(i, HorseProgressData::new));

    public static final SavedDataType<HorseProgressData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "progress"),
            HorseProgressData::new,
            CODEC);

    private final Map<UUID, Set<String>> byPlayer = new LinkedHashMap<>();

    private HorseProgressData() {
    }

    private HorseProgressData(List<PlayerTasks> players) {
        for (PlayerTasks p : players) {
            byPlayer.put(p.player(), new LinkedHashSet<>(p.done()));
        }
    }

    public static HorseProgressData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    private List<PlayerTasks> snapshot() {
        List<PlayerTasks> out = new ArrayList<>();
        byPlayer.forEach((id, done) -> out.add(new PlayerTasks(id, List.copyOf(done))));
        return out;
    }

    /**
     * Tick one task off. Does nothing if it was already done, so a caller never
     * has to check first.
     *
     * @return true the first time, which is when the chat line is said
     */
    public boolean complete(ServerPlayer player, ProgressTask task) {
        Set<String> done = byPlayer.computeIfAbsent(player.getUUID(), k -> new LinkedHashSet<>());
        if (!done.add(task.id())) {
            return false;
        }
        setDirty();
        sync(player);
        int total = ProgressTask.values().length;
        player.sendSystemMessage(Component.literal("[✔] ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(task.title()).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("  (" + done.size() + "/" + total + ")")
                        .withStyle(ChatFormatting.DARK_GRAY)));
        return true;
    }

    public Set<String> doneBy(UUID player) {
        return byPlayer.getOrDefault(player, Set.of());
    }

    public void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player,
                new ProgressSyncPayload(List.copyOf(doneBy(player.getUUID()))));
    }
}
