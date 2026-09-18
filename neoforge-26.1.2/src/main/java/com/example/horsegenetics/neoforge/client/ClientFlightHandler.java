package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.genetics.spec.HorseAbilities;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.server.FlightToggle;
import com.example.horsegenetics.neoforge.server.HorseFlight;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Flight for the horse you are riding.</b> A ridden horse is moved by its rider's client, so
 * {@code HorseFlightMixin} runs there and asks {@link HorseFlight#of} and {@link FlightToggle#wants}, both of which
 * call the lookups this installs. The genes come from {@link ClientHorseRecordCache} - attachments are not synced, so
 * the record cache is the only place the client can learn a horse's genome - and the mode from the same
 * {@link HorseAbilities} the server resolves, through the same {@link HorseFlight#fromAbilities} the server uses, so
 * the two readings of one genome cannot drift.
 *
 * <p>The toggle is kept per rider rather than per horse, matching the server's copy: it is a fact about what the
 * person is asking for, not about the animal.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class ClientFlightHandler {

    private ClientFlightHandler() {
    }

    /** Flight per genetic code plus epigenome, so a mount is resolved once rather than every tick. */
    private static final Map<String, HorseFlight.Flight> FLIGHT_BY_CODE = new HashMap<>();

    /** Riders on this client who have asked to fly. In practice only ever the local player. */
    private static final Set<UUID> WANTS = ConcurrentHashMap.newKeySet();

    /** Riders on this client who have asked to come down. */
    private static final Set<UUID> LANDING = ConcurrentHashMap.newKeySet();

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        HorseFlight.clientFlight = ClientFlightHandler::flightOf;
        FlightToggle.clientWants = ClientFlightHandler::wants;
        FlightToggle.clientDescending = ClientFlightInput::descending;
        FlightToggle.clientLanding = ClientFlightHandler::landing;
    }

    private static HorseFlight.Flight flightOf(Horse horse) {
        HorseRecord record = ClientHorseRecordCache.get(horse.getId());
        if (record == null || !record.hasGenome()) {
            return HorseFlight.Flight.NONE;
        }
        if (FLIGHT_BY_CODE.size() > 512) {
            FLIGHT_BY_CODE.clear();
        }
        return FLIGHT_BY_CODE.computeIfAbsent(
                record.geneticCode() + "|" + record.epigenomeCode(), key -> resolve(record));
    }

    private static HorseFlight.Flight resolve(HorseRecord record) {
        try {
            return HorseFlight.fromAbilities(
                    HorseAbilities.activeFor(record.genotype(), record.epigenome()));
        } catch (RuntimeException badCode) {
            return HorseFlight.Flight.NONE; // a record the client could not parse stays on the ground
        }
    }

    private static boolean wants(Horse horse) {
        return horse.getControllingPassenger() instanceof Player rider && WANTS.contains(rider.getUUID());
    }

    /** Flip this client's own copy the instant F is pressed, without waiting for the server to answer. */
    public static boolean toggleLocal(UUID rider) {
        if (WANTS.add(rider)) {
            return true;
        }
        WANTS.remove(rider);
        return false;
    }

    private static boolean landing(Horse horse) {
        return horse.getControllingPassenger() instanceof Player rider && LANDING.contains(rider.getUUID());
    }

    /** Ask to come down. One press of F; a second press inside the window gets off instead. */
    public static void landLocal(UUID rider) {
        LANDING.add(rider);
    }

    public static void forget(UUID rider) {
        WANTS.remove(rider);
        LANDING.remove(rider);
    }
}
