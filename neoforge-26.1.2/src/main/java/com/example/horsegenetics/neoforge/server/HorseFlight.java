package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.spec.HorseAbilities;
import net.minecraft.world.entity.animal.equine.Horse;

import java.util.function.Function;

/**
 * <b>What kind of flight a horse has, if any.</b>
 *
 * <p>{@code mixin.HorseFlightMixin} runs on whichever side is moving the horse and asks {@link #of}. For a ridden
 * horse that is the <b>rider's client</b>: {@code LivingEntity.travelRidden} gives the server the
 * {@code setDeltaMovement(Vec3.ZERO)} branch, because {@code Player.isClientAuthoritative()} is hardcoded true, and
 * {@code handleMoveVehicle} then snaps the server's copy onto whatever position the client reports. This is the same
 * shape as {@link SwimScaling}, and for the same reason.
 *
 * <p><b>The three flags are separate on purpose.</b> {@code flight_creative} and {@code flight_glide} are two ways of
 * leaving the ground; {@code cloud_walk} is neither, and is composed onto the gliding allele rather than folded into
 * it. A later gene may hand cloud-treading to a horse that cannot fly at all, and none of the flight code has to
 * learn about it.
 *
 * <p><b>Conditions are not evaluated here</b>, exactly as {@code SwimScaling} does not evaluate them: the flying
 * gene's effects carry no {@code when}, so every one of them is {@code Condition.ALWAYS}. A gene that ever gates a
 * flight flag on a condition will need this class - and its client mirror - to learn how.
 */
public final class HorseFlight {

    private HorseFlight() {
    }

    /** How the horse leaves the ground. {@link #GLIDE} keeps its momentum; {@link #CREATIVE} ignores physics. */
    public enum Mode {
        NONE,
        GLIDE,
        TRUE_FLIGHT
    }

    /**
     * <b>Is this horse flying right now?</b> The one place that question is answered, because the two modes answer it
     * differently and every caller would otherwise re-derive it.
     *
     * <ul>
     *   <li><b>True flight</b> is a toggle: double-tap jump, and it stays until asked to stop.</li>
     *   <li><b>Gliding is not a toggle at all</b> - a glider glides whenever it is off the ground, the way an elytra
     *       does (owner, 2026-09-17: <i>"that should start gliding any time the horse is off the ground"</i>).</li>
     * </ul>
     */
    public static boolean active(Horse horse) {
        return switch (of(horse).mode()) {
            case NONE -> false;
            case TRUE_FLIGHT -> FlightToggle.wants(horse);
            // Ridden as well as airborne: a loose glider just falls, and its
            // onGround flickers while it stands about, which churned the flag.
            case GLIDE -> !horse.onGround() && horse.getControllingPassenger() != null;
        };
    }

    /**
     * A horse's flight, resolved. {@code cloudWalk} is independent of {@code mode} - see the class note - so a horse
     * may in principle tread cloud without flying, and {@link #NONE} is the answer for most horses in the world.
     */
    public record Flight(Mode mode, boolean cloudWalk) {

        public static final Flight NONE = new Flight(Mode.NONE, false);

        /** Whether this horse flies at all. A cloud-treader that cannot fly still answers false. */
        public boolean flies() {
            return mode != Mode.NONE;
        }
    }

    /**
     * The client's lookup, installed by {@code client.ClientFlightHandler} at client setup. A hook rather than a
     * direct call, so this class - which the mixin loads on a dedicated server too - never names a client-only class.
     */
    public static volatile Function<Horse, Flight> clientFlight = horse -> Flight.NONE;

    /** {@link Flight#NONE} for a horse without the gene. The server side reads {@code GeneAbilityHandler}'s cache. */
    public static Flight of(Horse horse) {
        if (horse.level().isClientSide()) {
            return clientFlight.apply(horse);
        }
        return fromAbilities(GeneAbilityHandler.abilitiesOf(horse));
    }

    /**
     * The shared resolution, so the client mirror cannot drift from the server's reading of the same genome.
     *
     * <p>Creative beats gliding if a horse somehow expresses both. One locus with one dominance series means it
     * cannot today - but {@code AbilityType} lets any gene grant any traversal flag, and "the higher one wins" is a
     * better answer than "whichever the iterator reached last".
     */
    public static Flight fromAbilities(Iterable<HorseAbilities.Active> abilities) {
        Mode mode = Mode.NONE;
        boolean cloudWalk = false;
        for (HorseAbilities.Active active : abilities) {
            if (!(active.ability() instanceof GeneAbility.Traversal traversal)) {
                continue;
            }
            switch (traversal.flag()) {
                case "flight_true" -> mode = Mode.TRUE_FLIGHT;
                case "flight_glide" -> {
                    if (mode != Mode.TRUE_FLIGHT) {
                        mode = Mode.GLIDE;
                    }
                }
                case "cloud_walk" -> cloudWalk = true;
                default -> { }
            }
        }
        return mode == Mode.NONE && !cloudWalk ? Flight.NONE : new Flight(mode, cloudWalk);
    }
}
