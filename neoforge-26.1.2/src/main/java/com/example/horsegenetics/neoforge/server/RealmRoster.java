package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.data.HorseCareAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.network.HorseRosterPayload;
import com.example.horsegenetics.neoforge.network.RealmRosterPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.equine.Horse;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * <b>Every horse standing in the horse realm</b>, gathered for the browser's
 * <i>Horse realm</i> tab (owner, 2026-09-25). {@link HorseRoster} is the same
 * table asked the other question - <i>which of these are mine</i> - and this one
 * deliberately asks nothing about ownership at all.
 *
 * <h2>Why it is a different list rather than a filter</h2>
 * The roster is built from the <b>ancestry database</b>, every record the server
 * has ever written, and then narrowed to the ones this player owns. That is the
 * right shape for a stable: your horses exist whether or not anyone has loaded
 * the chunk they are in. It is the wrong shape here twice over. The realm's
 * horses are mostly <b>nobody's</b>, so there is no owner to filter on; and the
 * question "what is in the field right now" is about <i>entities</i>, not
 * records - a horse that has wandered out through a portal is not in the realm
 * any more, and no record would say so.
 *
 * <p>So this walks the realm level's live horses. It is a scan, which the roster
 * carefully is not, and that is affordable for the same reason: it runs when a
 * player opens a screen, over one dimension, capped like every other roster.
 *
 * <h2>Loaded is always true here, and that is the point</h2>
 * Every row is an entity that exists this tick, so bond, age and herd membership
 * are all real rather than "not known". A row in this table is a horse you can go
 * and stand next to.
 */
public final class RealmRoster {

    private RealmRoster() {
    }

    public static void sendTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new RealmRosterPayload(gather(player)));
    }

    static List<HorseRosterPayload.Entry> gather(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return List.of();
        }
        ServerLevel realm = server.getLevel(HorseRealm.REALM_LEVEL);
        if (realm == null) {
            return List.of();
        }

        List<Horse> horses = new ArrayList<>();
        for (Horse horse : realm.getEntities(net.minecraft.world.entity.EntityType.HORSE,
                h -> h.isAlive() && HorseRecords.hasRealRecord(h))) {
            horses.add(horse);
        }

        // Nearest first. The realm is one flat field and the player is standing
        // in it, so "which of these can I walk to" is the question the order
        // should answer - and it is also the sensible thing to keep when the
        // list is cut to the packet cap. A stable sorts by generation because
        // nobody is standing in it.
        boolean here = player.level().dimension().equals(HorseRealm.REALM_LEVEL);
        if (here) {
            horses.sort(Comparator.comparingDouble(h -> h.distanceToSqr(player)));
        } else {
            horses.sort(Comparator.comparing(h -> HorseRecords.of(h).displayName(),
                    String.CASE_INSENSITIVE_ORDER));
        }

        List<HorseRosterPayload.Entry> out = new ArrayList<>();
        for (Horse horse : horses) {
            if (out.size() >= HorseRosterPayload.MAX_ENTRIES) {
                break;
            }
            out.add(entry(horse));
        }
        return List.copyOf(out);
    }

    /**
     * One live horse as a table row. The same {@link HorseRosterPayload.Entry}
     * the stable uses, so the client draws both tables with one renderer and a
     * column added here is a column in both.
     */
    private static HorseRosterPayload.Entry entry(Horse horse) {
        HorseRecord record = HorseRecords.of(horse);
        HorseCareAttachment care = horse.getData(ModAttachments.HORSE_CARE.get());
        return new HorseRosterPayload.Entry(
                record.id(),
                record.firstName(),
                record.lastName(),
                record.barnName().orElse(""),
                record.lineage().displayName(),
                record.generation(),
                record.geneticCode(),
                horse.isTamed(),
                !horse.isBaby(),
                true,
                care.bond(),
                care.inHerd(),
                "realm " + horse.getBlockX() + ", " + horse.getBlockY() + ", " + horse.getBlockZ(),
                record.tamedBy().orElse(""),
                record.bredBy().orElse(""),
                record.hasKnownParents(),
                record.gelded());
    }
}
