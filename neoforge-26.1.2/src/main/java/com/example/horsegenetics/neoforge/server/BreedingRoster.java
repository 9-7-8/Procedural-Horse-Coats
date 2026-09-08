package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.data.HorseAncestryData;
import com.example.horsegenetics.neoforge.network.BreedingRosterPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * <b>Which horses a player owns</b>, gathered for the browser's Breeding
 * preview tab.
 *
 * <h2>Two signals, because one is not enough</h2>
 * A {@link HorseRecord} remembers who <i>tamed</i> it, which is the durable
 * answer and survives the horse being unloaded on the far side of the world -
 * but it is not the same as who owns it now, because a transfer paper changes
 * the entity's owner without rewriting history. So a horse is on the roster if
 * <b>either</b> its record names this player as the tamer <b>or</b> the live
 * entity is loaded and owned by them. Neither alone is right, and the union is:
 * the record catches the horse you left at home, the entity catches the horse
 * you were given this morning.
 *
 * <p>The roster is not a permission check - the preview reads genotypes and
 * writes nothing - so the filter exists to make the list usable, not to keep a
 * secret. Nothing here is sent about a horse the player cannot already walk up
 * to and inspect.
 */
public final class BreedingRoster {

    private BreedingRoster() {
    }

    /** Gather and send. Called from the payload handler; safe on the server thread. */
    public static void sendTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new BreedingRosterPayload(gather(player)));
    }

    static List<BreedingRosterPayload.Entry> gather(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return List.of();
        }
        String username = player.getGameProfile().name();
        List<HorseRecord> mine = new ArrayList<>();
        for (HorseRecord record : HorseAncestryData.get(server).all()) {
            if (!record.hasGenome()) {
                continue; // nothing to breed from - see HorseRecord.unassigned
            }
            if (username.equals(record.tamedBy().orElse(null)) || ownedNow(server, record, player)) {
                mine.add(record);
            }
        }

        // Newest first: a breeding programme is nearly always about the horses
        // at the front of it, and this is also the sensible thing to keep when
        // the list has to be cut to the packet cap.
        mine.sort(Comparator.comparingInt(HorseRecord::generation).reversed()
                .thenComparing(HorseRecord::displayName, String.CASE_INSENSITIVE_ORDER));

        List<BreedingRosterPayload.Entry> out = new ArrayList<>();
        for (HorseRecord record : mine) {
            if (out.size() >= BreedingRosterPayload.MAX_ENTRIES) {
                break;
            }
            out.add(new BreedingRosterPayload.Entry(
                    record.id(),
                    record.displayName(),
                    record.lineage().displayName(),
                    record.generation(),
                    record.geneticCode()));
        }
        return List.copyOf(out);
    }

    /**
     * Is the live entity for this record loaded somewhere and owned by this
     * player? A UUID lookup per level, which is a map hit rather than a scan.
     */
    private static boolean ownedNow(MinecraftServer server, HorseRecord record, ServerPlayer player) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(record.id());
            if (entity instanceof AbstractHorse horse && horse.isTamed()) {
                LivingEntity owner = horse.getOwner();
                return owner != null && owner.getUUID().equals(player.getUUID());
            }
        }
        return false;
    }
}
