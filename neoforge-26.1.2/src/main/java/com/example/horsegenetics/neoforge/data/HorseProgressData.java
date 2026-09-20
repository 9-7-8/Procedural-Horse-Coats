package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.common.progress.ProgressTask;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.advancement.ModTriggers;
import com.example.horsegenetics.neoforge.advancement.ProgressTaskTrigger;
import com.example.horsegenetics.neoforge.network.ProgressSyncPayload;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
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
 * it is not silent is the first, and what says so is <b>an advancement</b>:
 * every task is also one ({@link ProgressTaskTrigger}), so the toast and the
 * chat line are vanilla's, they can be turned off in vanilla's settings, and
 * the tick is in {@code /advancement} and the advancement screen as well as in
 * the book. This class wrote its own green chat line until that landed; two
 * announcements of one event is one too many.
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
     * @return true the first time, which is when the advancement is awarded
     */
    public boolean complete(ServerPlayer player, ProgressTask task) {
        Set<String> done = byPlayer.computeIfAbsent(player.getUUID(), k -> new LinkedHashSet<>());
        if (!done.add(task.id())) {
            return false;
        }
        setDirty();
        sync(player);
        award(player, task);
        return true;
    }

    /**
     * Fire the criterion behind this task's advancement.
     *
     * <p>Deliberately not called on a repeat completion: the trigger walks
     * every listener the player has on it, and {@link #complete} is called from
     * goals that tick. Once, on the first, is what an advancement needs.
     *
     * <p>{@link #awardEverythingDone} is the other caller, and the reason this
     * is separate: a player whose ticks predate the advancements existing has
     * the boxes and not the toasts, and nothing would ever fire for them again.
     */
    private static void award(ServerPlayer player, ProgressTask task) {
        ModTriggers.PROGRESS_TASK.get().trigger(player, task);
    }

    /**
     * Fire the criterion for everything this player has already done - on
     * login, from {@code ProgressHooks}.
     *
     * <p>It is how the advancements catch up with a save that ticked its boxes
     * before they existed, and how a revoked one comes back. Awarding an
     * advancement a player already holds does nothing, so this is safe every
     * login rather than once.
     */
    public void awardEverythingDone(ServerPlayer player) {
        for (String id : doneBy(player.getUUID())) {
            ProgressTask task = ProgressTask.byId(id);
            if (task != null) {
                award(player, task);
            }
        }
    }

    public Set<String> doneBy(UUID player) {
        return byPlayer.getOrDefault(player, Set.of());
    }

    public void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player,
                new ProgressSyncPayload(List.copyOf(doneBy(player.getUUID()))));
    }
}
