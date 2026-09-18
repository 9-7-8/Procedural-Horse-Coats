package com.example.horsegenetics.neoforge.mixin;

import com.example.horsegenetics.neoforge.server.FlightToggle;
import com.example.horsegenetics.neoforge.server.HorseFlight;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * <b>A horse that leaves the ground, using vanilla's own two ways of doing it.</b>
 *
 * <p>This mixin writes no physics. 26.1.2 already carries both flight models on {@code LivingEntity}, and neither is
 * gated on being a player:
 * <ul>
 *   <li>{@code travelFlying(Vec3, float, float, float)} is creative/ghast flight - {@code moveRelative}, {@code move},
 *       and a flat drag, with <b>no gravity term at all</b>. Choosing it <i>is</i> the gravity decision; that is why
 *       nothing here calls {@code setNoGravity}. {@code HappyGhast.travel} is exactly this call.</li>
 *   <li>{@code travelFallFlying} is the real elytra glide - lift {@code cos squared} of pitch, sink traded for forward
 *       speed, speed traded back for height, {@code 0.99/0.98/0.99} drag. {@code travel} dispatches into it whenever
 *       {@code isFallFlying()}, which is shared entity flag 7.</li>
 * </ul>
 * Routing into those rather than inventing constants is what keeps flight deterministic and the backport cheap: a
 * {@code forge-1.12.2} reimplements "which vanilla path", not a table of tuning numbers.
 *
 * <h2>Why a mixin at all</h2>
 * The same reason {@link LavaTravelMixin} gives, now verified at source. A ridden horse is simulated on the rider's
 * client: {@code LivingEntity.travelRidden} hands the server the {@code setDeltaMovement(Vec3.ZERO)} branch, because
 * {@code Player.isClientAuthoritative()} returns a hardcoded true, and {@code handleMoveVehicle} then {@code absSnapTo}s
 * the server's copy onto the position the client reports. Nothing written from a server tick handler survives, which
 * is why {@code GeneAbilityHandler}'s three flight cases deliberately do nothing.
 *
 * <h2>Why it is safe here</h2>
 * Both injections return immediately unless the entity is a {@link Horse} that expresses the gene <i>and</i> whose
 * rider has asked for it. Every other mob in the game, and every horse in a world where nothing expresses, takes
 * exactly the path it took before - the same licence {@code LavaTravelMixin} argues for sitting on {@code LivingEntity},
 * which is a hot path shared with every other mod in the pack.
 *
 * <p><b>UNVERIFIED at runtime.</b> Written against the 26.1.2 sources; nothing here has been watched happening to a
 * horse. {@code defaultRequire: 1} makes a missed target fail loudly at load rather than silently.
 */
@Mixin(LivingEntity.class)
public abstract class HorseFlightMixin {

    @Shadow
    protected abstract void travelFlying(Vec3 input, float waterSpeed, float lavaSpeed, float airSpeed);

    // Shared flag 7 - isFallFlying() - is set through horse.setSharedFlag, which
    // an access transformer widens. It is NOT shadowed here: the method is
    // declared on Entity, not LivingEntity, and @Shadow resolves against the
    // target class, so a shadow of it threw InvalidMixinException at class load.

    /**
     * Creative flight: vanilla's {@code travelFlying}, and skip the normal dispatcher entirely.
     *
     * <h2>How fast, and why it is the horse that decides</h2>
     * <b>A horse flies at its own speed.</b> The cruise is derived from {@code Attributes.MOVEMENT_SPEED} - the
     * genetic ground speed this mod already resolves from the genome - rather than from a constant, so breeding a
     * fast horse breeds a fast flyer and the speed loci keep meaning something once the animal leaves the ground.
     * Owner's call, 2026-09-17. It also keeps flight deterministic: airspeed is a pure function of the genotype,
     * which is what {@code philosophy.html} requires of everything else.
     *
     * <p>{@code Attributes.FLYING_SPEED} is deliberately NOT used: a vanilla {@link Horse} has no such attribute
     * registered, and asking for one it does not have throws.
     *
     * <p><b>The arithmetic.</b> {@code travelFlying} accelerates by {@code |input| * speed} each tick then applies
     * 0.91 drag, so velocity settles at {@code |input| * speed / 0.09}. Working backwards, a wanted cruise in blocks
     * per tick needs {@code speed = cruise * 0.09}, which is what {@link #horsegenetics$flightSpeed} computes.
     *
     * <p>The first version was a flat 0.08 with a happy ghast's 0.195 input scale, and the owner flew it: <i>"SUPER
     * slow, like going through molasses"</i>. It was - {@code 0.195 * 0.10 / 0.09} is 0.22 blocks a tick, walking
     * pace, slower than the horse runs on the ground. A happy ghast was the right thing to copy the SHAPE from and
     * the wrong thing to copy the NUMBERS from.
     */
    @Unique
    private float horsegenetics$ramp;

    /**
     * Blocks per tick per point of {@code MOVEMENT_SPEED}. Empirical, and the number to turn first if flight feels
     * wrong against a gallop: a horse of attribute 0.225 runs somewhere near 0.45 blocks a tick.
     */
    private static final float GROUND_BLOCKS_PER_TICK = 2.0F;

    /** Cruise the moment flight engages, as a share of ground speed. "Slightly faster", per the owner. */
    private static final float START_MULTIPLE = 1.1F;

    /** How much more than {@link #START_MULTIPLE} a horse reaches with forward held. */
    private static final float RAMP_RANGE = 1.4F;

    /** Per tick, held forward. {@code RAMP_RANGE / 0.0175} is about four seconds to top speed - "slowly". */
    private static final float RAMP_UP = 0.0175F;

    /** Per tick, forward released. Roughly three times as fast as it built - "quickly, not instantly". */
    private static final float RAMP_DOWN = 0.055F;

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void horsegenetics$flight(Vec3 input, CallbackInfo ci) {
        if (!((Object) this instanceof Horse horse)) {
            return;
        }
        HorseFlight.Mode mode = HorseFlight.of(horse).mode();
        if (mode == HorseFlight.Mode.NONE || mode == HorseFlight.Mode.GLIDE) {
            // GLIDING IS ENTIRELY VANILLA'S and is not handled here at all.
            // Shared flag 7 is set SERVER-SIDE by GeneAbilityHandler, because
            // it is synced entity data the server owns - setting it from this
            // mixin was the bug that made gliding do nothing, since for a ridden
            // horse this method only ever runs on the rider's client. With the
            // flag lit, travel's own dispatcher sends the horse into
            // travelFallFlying, which is the real lift/drag/pitch maths.
            return;
        }
        if (!FlightToggle.wants(horse)) {
            return;
        }

        // RESET BEFORE THE MOVE, NOT AFTER. travelFlying moves through
        // Entity.move, which tallies fallDistance on every downward tick and
        // spends it the instant the horse touches ground - so a long gentle
        // descent under power banked the whole drop and hurt on landing, which
        // is what the owner saw ("injured by landing even though we weren't
        // moving very fast", 2026-09-17). Resetting afterwards was too late by
        // one call, the same ordering trap gene-bird-boned.html documents.
        //
        // This is NOT fall immunity: the gene grants none, by the owner's call.
        // Cut the flight and the horse falls like anything else without
        // bird_boned. It only means that flying down is not falling.
        horse.resetFallDistance();

        if (FlightToggle.landing(horse)) {
            // COMING DOWN ON PURPOSE. A steady powered descent rather than a
            // drop: fall distance is already zeroed above, so the horse arrives
            // unhurt however far up it was. It stops by itself - FlightToggle
            // .landing goes false the moment the hooves touch, on both sides,
            // by the same onGround test.
            this.travelFlying(DOWN, LANDING_SPEED, LANDING_SPEED, LANDING_SPEED);
            ci.cancel();
            return;
        }

        float speed = this.horsegenetics$flightSpeed(horse, input);
        this.travelFlying(input, speed, speed, speed);
        ci.cancel();
    }

    /** Straight down, in the horse's own frame. {@code moveRelative} leaves the y component unrotated. */
    @Unique
    private static final Vec3 DOWN = new Vec3(0.0, -1.0, 0.0);

    /** Descent while landing: {@code 0.032 / 0.09} is about 0.36 blocks a tick, brisk but not a drop. */
    private static final float LANDING_SPEED = 0.032F;

    /**
     * The horse's own airspeed this tick, as {@code travelFlying} wants it.
     *
     * <p>Holding forward walks {@link #horsegenetics$ramp} up and letting go walks it down, so the animal gathers
     * pace over a few seconds and loses it over about one - it never stops dead, because the ramp only sets the
     * TARGET and vanilla's 0.91 drag still has to carry the velocity down to it.
     */
    @Unique
    private float horsegenetics$flightSpeed(Horse horse, Vec3 input) {
        if (input.z > 0.0) {
            this.horsegenetics$ramp = Math.min(this.horsegenetics$ramp + RAMP_UP, RAMP_RANGE);
        } else {
            this.horsegenetics$ramp = Math.max(this.horsegenetics$ramp - RAMP_DOWN, 0.0F);
        }
        float ground = (float) horse.getAttributeValue(Attributes.MOVEMENT_SPEED) * GROUND_BLOCKS_PER_TICK;
        float cruise = ground * (START_MULTIPLE + this.horsegenetics$ramp);
        return cruise * 0.09F;
    }

    /**
     * <b>Keep flag 7 lit for a gliding horse, by stepping around the method that would put it out.</b>
     *
     * <p>{@code updateFallFlying} runs every server tick while an entity is fall-flying and does two things: it
     * clears the flag unless {@code canGlide()} passes, and <b>every twenty ticks it picks a random glider item out
     * of the entity's equipment and damages it</b>.
     *
     * <p>The first version overrode {@code canGlide()} to return true for a horse with the gene - a horse's wings
     * are a gene, not a chestplate. That kept the flag lit and gliding worked, for about a second: on the twentieth
     * tick vanilla went looking for the glider item it had just been promised, {@code Util.getRandom} was handed an
     * empty list, and {@code nextInt(0)} threw <i>"Bound must be positive"</i> and took the server down mid-flight
     * (2026-09-17). Answering {@code canGlide} truthfully-but-wrongly is a lie vanilla is entitled to act on.
     *
     * <p>Cancelling the whole method instead is honest: a gene-winged horse has no glider to wear out, so there is
     * nothing to damage and nothing to clear. What is given up is the {@code ELYTRA_GLIDE} game event - sculk
     * sensors will not hear a gliding horse - which is a fair trade and worth writing down.
     *
     * <p>{@code canGlide} is deliberately left alone now. Nothing else asks it about a horse.
     */
    @Inject(method = "updateFallFlying", at = @At("HEAD"), cancellable = true)
    private void horsegenetics$genesAreWings(CallbackInfo ci) {
        if (!((Object) this instanceof Horse horse)) {
            return;
        }
        // No toggle in this condition - a glider glides whenever it is airborne -
        // but it does have to be carrying somebody, matching HorseFlight.active.
        if (HorseFlight.active(horse) && HorseFlight.of(horse).mode() == HorseFlight.Mode.GLIDE) {
            ci.cancel();
        }
    }
}
