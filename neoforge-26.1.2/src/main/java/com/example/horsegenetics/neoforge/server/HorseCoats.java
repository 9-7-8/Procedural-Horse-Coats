package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.data.HorseAncestryData;
import com.example.horsegenetics.neoforge.network.HorseCoatBatchPayload;
import com.example.horsegenetics.neoforge.network.HorseCoatRequestPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Answers a {@link HorseCoatRequestPayload} - the epigenome behind each of a
 * handful of horses, so the browser can draw them.
 *
 * <p>It is a straight record lookup and it sends nothing a player could not
 * already learn by walking up to the horse: an epigenome is the reason two
 * horses with the same alleles look slightly different, not a secret. The
 * request is capped by its own codec at {@link HorseCoatRequestPayload#MAX_IDS},
 * and an id with no record is quietly left out of the reply.
 */
public final class HorseCoats {

    private HorseCoats() {
    }

    public static void sendTo(ServerPlayer player, List<UUID> ids) {
        MinecraftServer server = player.level().getServer();
        if (server == null || ids.isEmpty()) {
            return;
        }
        HorseAncestryData db = HorseAncestryData.get(server);
        List<HorseCoatBatchPayload.Entry> out = new ArrayList<>();
        for (UUID id : ids) {
            if (out.size() >= HorseCoatBatchPayload.MAX_ENTRIES) {
                break;
            }
            Optional<HorseRecord> record = db.lookup(id);
            if (record.isPresent() && record.get().hasGenome()) {
                out.add(new HorseCoatBatchPayload.Entry(id, record.get().epigenomeCode()));
            }
        }
        if (!out.isEmpty()) {
            PacketDistributor.sendToPlayer(player, new HorseCoatBatchPayload(List.copyOf(out)));
        }
    }
}
