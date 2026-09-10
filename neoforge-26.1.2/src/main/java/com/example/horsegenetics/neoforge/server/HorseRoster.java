package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.data.HorseAncestryData;
import com.example.horsegenetics.neoforge.data.HorseCareAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.network.HorseRosterPayload;
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
 * <b>Which horses a player owns</b>, gathered for the browser's <i>My horses</i>
 * table and its <i>Breeding preview</i> pickers.
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
 * <p>The roster is not a permission check - it reads genotypes and writes
 * nothing - so the filter exists to make the list usable, not to keep a secret.
 * Nothing here is sent about a horse the player cannot already walk up to and
 * inspect.
 *
 * <h2>The live half</h2>
 * Bond, age, herd membership and whereabouts are entity facts, so they are only
 * knowable for a horse in a loaded chunk. {@link #live} does <b>one</b> UUID
 * lookup per level - a map hit, not a scan - and every field it could not
 * answer goes out marked unknown rather than defaulted; see
 * {@link HorseRosterPayload}.
 */
public final class HorseRoster {

    private HorseRoster() {
    }

    /** Gather and send. Called from the payload handler; safe on the server thread. */
    public static void sendTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new HorseRosterPayload(gather(player)));
    }

    static List<HorseRosterPayload.Entry> gather(ServerPlayer player) {
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

        // Every horse you own contributes its alleles to the collection.
        //
        // The only hooks used to be taming and breeding, so a horse bought from
        // the cowboy, handed over on a transfer paper, or tamed before the
        // collection existed at all counted for nothing - and the Alleles tab
        // read empty for a player with a full stable, which is what it was
        // reported as (known gap 149). Swept here rather than at the moment of
        // acquisition because there is no single such moment, and because a
        // sweep is retroactive: it repairs a save that predates the feature
        // instead of only being right from now on. It runs on the uncapped
        // list, before MAX_ENTRIES cuts the packet down - what you can see in
        // the table and what you have met are different questions.
        collectAlleles(server, player, mine);

        // Newest first: a breeding programme is nearly always about the horses
        // at the front of it, and this is also the sensible thing to keep when
        // the list has to be cut to the packet cap. The table re-sorts on the
        // client, so this order only decides who survives the cap.
        mine.sort(Comparator.comparingInt(HorseRecord::generation).reversed()
                .thenComparing(HorseRecord::displayName, String.CASE_INSENSITIVE_ORDER));

        List<HorseRosterPayload.Entry> out = new ArrayList<>();
        for (HorseRecord record : mine) {
            if (out.size() >= HorseRosterPayload.MAX_ENTRIES) {
                break;
            }
            out.add(entry(server, record));
        }
        return List.copyOf(out);
    }

    /** Hand every owned genotype to the collection in one batch. */
    private static void collectAlleles(MinecraftServer server, ServerPlayer player,
                                       List<HorseRecord> mine) {
        List<com.example.horsegenetics.common.genetics.Genotype> genotypes =
                new ArrayList<>(mine.size());
        for (HorseRecord record : mine) {
            try {
                genotypes.add(com.example.horsegenetics.common.genetics.Genotype.parse(
                        record.geneticCode()));
            } catch (RuntimeException unparseable) {
                // a code written against a different registry - nothing to learn
            }
        }
        if (!genotypes.isEmpty()) {
            com.example.horsegenetics.neoforge.data.GeneDatabaseData.get(server)
                    .collect(player, genotypes);
        }
    }

    private static HorseRosterPayload.Entry entry(MinecraftServer server, HorseRecord record) {
        AbstractHorse horse = live(server, record);
        boolean loaded = horse != null;
        HorseCareAttachment care = loaded ? horse.getData(ModAttachments.HORSE_CARE.get()) : null;
        return new HorseRosterPayload.Entry(
                record.id(),
                record.firstName(),
                record.lastName(),
                record.barnName().orElse(""),
                record.lineage().displayName(),
                record.generation(),
                record.geneticCode(),
                loaded ? horse.isTamed() : record.tamedBy().isPresent(),
                // A horse nobody can see is assumed grown: a foal that has been
                // out of the world long enough to unload has almost certainly
                // aged up, and "foal" on a five-year-old mare reads as a bug.
                !loaded || !horse.isBaby(),
                loaded,
                care == null ? HorseRosterPayload.BOND_UNKNOWN : care.bond(),
                care != null && care.inHerd(),
                loaded ? where(horse) : "",
                record.tamedBy().orElse(""),
                record.bredBy().orElse(""),
                record.hasKnownParents());
    }

    /** {@code "overworld 118, 71, -204"} - the dimension path, then the block it is standing on. */
    private static String where(AbstractHorse horse) {
        return horse.level().dimension().identifier().getPath()
                + " " + horse.getBlockX() + ", " + horse.getBlockY() + ", " + horse.getBlockZ();
    }

    /**
     * The live entity for this record, if it is loaded anywhere. One UUID lookup
     * per level, which is a map hit rather than a scan.
     */
    private static AbstractHorse live(MinecraftServer server, HorseRecord record) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(record.id());
            if (entity instanceof AbstractHorse horse) {
                return horse;
            }
        }
        return null;
    }

    /**
     * Is the live entity for this record loaded somewhere and owned by this
     * player?
     */
    private static boolean ownedNow(MinecraftServer server, HorseRecord record, ServerPlayer player) {
        AbstractHorse horse = live(server, record);
        if (horse == null || !horse.isTamed()) {
            return false;
        }
        LivingEntity owner = horse.getOwner();
        return owner != null && owner.getUUID().equals(player.getUUID());
    }
}
