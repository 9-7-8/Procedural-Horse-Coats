package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.breed.BandType;
import com.example.horsegenetics.common.breed.BreedHerd;
import com.example.horsegenetics.common.breed.BreedLineage;
import com.example.horsegenetics.neoforge.data.HorseCareAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.equine.Horse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;
import java.util.UUID;

/**
 * <b>Released horses find each other.</b> A band is what makes the realm look
 * like a field of horses rather than a field of furniture - they graze
 * together, follow a lead, and the whole social layer (grooming, ranks, bachelor
 * bands, a colt leaving home) is already built and already runs on
 * {@code HorseCareAttachment.inWildHerd()}.
 *
 * <h2>Why this exists at all</h2>
 * {@code HerdManager} founds herds, but it founds them for a horse that has
 * <i>just spawned and has no record yet</i>: {@code assignFounder} returns
 * immediately for anything already founded, and the whole path is gated on the
 * wild-spawn mark that {@code HorseFoundingTickHandler} strips from every horse
 * that arrives with a record. A released horse is the opposite case in every
 * respect - a fully-recorded animal with a name, a pedigree and an owner it no
 * longer has - so it fell through every one of those gates and stood alone
 * forever in a field full of bands. This is the missing path, and it is scoped
 * to the realm because the realm is the only place a horse ever stops being
 * owned.
 *
 * <h2>The band is Feral Mixed, on purpose</h2>
 * Not the lead's own breed. A band that gathers here is whoever happened to be
 * released near whom - a Thoroughbred, a splice and somebody's crossbred filly -
 * and {@code BreedLineage.FERAL} is precisely the mod's word for that: <i>no
 * herd identity</i>. Stamping the lead's breed on it would claim a shared
 * ancestry that is not there, and {@code combine} already absorbs feral into
 * mixed for exactly this reason. Each horse keeps its own breed on its own
 * record either way; this only names the group.
 *
 * <p><b>Not verified in-game.</b>
 */
@EventBusSubscriber
public final class HorseRealmHerds {

    /**
     * Once every ten seconds per horse. Nothing here is urgent - a horse that
     * joins a band a few seconds after it is released looks exactly like one
     * that joined instantly - and the scan is an entity query per unherded
     * horse, which is worth keeping rare in a field that may hold hundreds.
     */
    private static final int SCAN = 200;

    /**
     * How far a released horse looks for a band to join. Matched to
     * {@code HerdManager}'s own clump radius so that a band formed here is the
     * same size and shape as a band formed by a wild spawn, and the social
     * handlers that read both cannot tell them apart.
     */
    private static final double JOIN_RADIUS = 32.0;

    @SubscribeEvent
    static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Horse horse) || !horse.isAlive() || horse.isBaby()) {
            return;
        }
        if (!(horse.level() instanceof ServerLevel level) || !HorseRealm.isRealm(level)) {
            return;
        }
        if ((horse.tickCount + horse.getId()) % SCAN != 0) {
            return;
        }
        if (horse.isTamed() || !HorseRecords.hasRealRecord(horse)) {
            return;     // somebody's horse, or not one of ours yet
        }
        HorseCareAttachment care = horse.getData(ModAttachments.HORSE_CARE.get());
        if (care.inWildHerd()) {
            return;
        }

        Horse neighbour = bandNearby(level, horse);
        UUID lead;
        String band;
        if (neighbour == null) {
            // Nobody to join: it founds its own, pointing at itself, exactly as a
            // wild lead does. A band of one is the ordinary way a band starts.
            lead = horse.getUUID();
            band = BandType.TRADITIONAL.name();
        } else {
            HorseCareAttachment theirs = neighbour.getData(ModAttachments.HORSE_CARE.get());
            lead = theirs.herd().orElse(neighbour.getUUID());
            band = theirs.herdBand().orElse(BandType.TRADITIONAL.name());
        }
        horse.setData(ModAttachments.HORSE_CARE.get(),
                care.withWildHerd(lead, BreedLineage.FERAL_ID, band));
    }

    /**
     * <b>How many horses one band will take in before the next arrival founds
     * its own instead.</b> The stallion and his harem
     * ({@link BreedHerd#MAX_HAREM_MARES}), plus the same again for the foals and
     * the hangers-on a band of that size accumulates - so the number lives with
     * the harem rule rather than beside it.
     *
     * <p><b>This is the whole reason the realm was one ball of horses.</b> Every
     * portal lands on the same block by design (see {@code HorseRealm}), so every
     * new arrival had a banded horse within {@link #JOIN_RADIUS} and joined it -
     * the same band, every time, for ever. Cohesion then held that band together
     * as one mass, and no amount of spacing between bands could help, because
     * there was only ever one band. A band that can be full is what makes the
     * second one exist.
     */
    private static final int BAND_FULL = (BreedHerd.MAX_HAREM_MARES + 1) * 2;

    /**
     * The nearest already-banded wild horse within {@value #JOIN_RADIUS} blocks
     * <b>whose band has room</b>, or null - in which case the caller founds a new
     * band.
     *
     * <p>Room is counted over the loaded members of that band near the candidate,
     * which is the same reach {@code HerdSocialHandler} uses for everything else
     * about a band. A band whose far half is unloaded therefore reads as smaller
     * than it is; the consequence is a band that grows a little past
     * {@link #BAND_FULL} rather than one that never fills, and a slightly large
     * band is a much smaller problem than a single planetary one.
     */
    private static Horse bandNearby(ServerLevel level, Horse horse) {
        List<Horse> near = level.getEntitiesOfClass(Horse.class,
                horse.getBoundingBox().inflate(JOIN_RADIUS),
                other -> other != horse && other.isAlive() && !other.isTamed()
                        && other.getData(ModAttachments.HORSE_CARE.get()).inWildHerd());
        Horse best = null;
        double bestDist = Double.MAX_VALUE;
        java.util.Map<UUID, Boolean> hasRoom = new java.util.HashMap<>();
        for (Horse other : near) {
            double d = other.distanceToSqr(horse);
            if (d >= bestDist) {
                continue;
            }
            UUID theirs = other.getData(ModAttachments.HORSE_CARE.get()).herd().orElse(other.getUUID());
            // Counted once per band rather than once per candidate: a dozen
            // horses of one band in range is one entity query, not a dozen.
            if (!hasRoom.computeIfAbsent(theirs,
                    id -> HerdSocialHandler.bandMembers(level, id, other).size() < BAND_FULL)) {
                continue;
            }
            bestDist = d;
            best = other;
        }
        return best;
    }

    private HorseRealmHerds() {
    }
}
