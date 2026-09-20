package com.example.horsegenetics.neoforge.mixin;

import com.example.horsegenetics.neoforge.server.FlightToggle;
import com.example.horsegenetics.neoforge.server.HorseFlight;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * <b>The two things a flying mount needs that live on the horse rather than on {@code LivingEntity}.</b>
 *
 * <h2>1. The rider must not be kicked</h2>
 * {@code ServerGamePacketListenerImpl} watches for a vehicle that is not descending, is not resting on anything, and
 * has only air around it, and <b>disconnects the rider after 80 ticks</b> of it -
 * {@code multiplayer.disconnect.flying}. Four seconds. The escape the field exists for is
 * {@code Entity.isFlyingVehicle()}, which vanilla's own flying mount overrides
 * ({@code HappyGhast: return !this.isBaby();}). Without this override a flying horse works beautifully and then throws
 * its rider out of the server, which would read as anything but a networking rule.
 *
 * <h2>2. Vertical control</h2>
 * A ridden mount climbs through the {@code y} component of the vector {@code getRiddenInput} returns - there is no
 * separate vertical channel anywhere in vanilla. {@code HappyGhast.getRiddenInput} is the whole pattern: decompose the
 * rider's pitch into forward/up while they hold a movement key, and add a flat amount while they hold jump. Copied
 * here rather than invented, so a flying horse handles like the one flying mount players already know.
 *
 * <p>Only creative flight reads this. A gliding horse is steered by <i>look direction alone</i>, the way an elytra is -
 * {@code travelFallFlying} never consults rider input at all - so adding a climb term for it would quietly turn the
 * glide into powered flight, which is the distinction between the two alleles.
 *
 * <p><b>UNVERIFIED at runtime.</b> Written against the 26.1.2 sources.
 */
@Mixin(AbstractHorse.class)
public abstract class HorseFlightRiddenMixin {

    /**
     * Exempt a flying horse from the floating-vehicle kick.
     *
     * <p>A plain override rather than an injection: {@code isFlyingVehicle} is declared on {@code Entity}, which
     * {@code AbstractHorse} does not override, so there is no method here to inject into - and putting an injection on
     * {@code Entity} itself would sit this mod in the hottest path in the game for the sake of one boolean. Vanilla's
     * answer is {@code false}, so returning the gene's answer directly is complete.
     */
    public boolean isFlyingVehicle() {
        // HorseFlight.active, not wants: a glider is not toggled on, it is simply
        // off the ground, and a glider whose rider was kicked after eighty ticks
        // for "floating" would be a very confusing bug.
        return (Object) this instanceof Horse horse && HorseFlight.active(horse);
    }

    /**
     * <b>A flying horse does not rear.</b> Flight is toggled by double-tapping jump, and jump on a saddled horse is
     * also the charged rear-and-leap - both handlers read the same key on the same tick, so without this the gesture
     * that puts a horse in the air also makes it buck twice on the way up.
     */
    @Inject(method = "onPlayerJump", at = @At("HEAD"), cancellable = true)
    private void horsegenetics$noRearingInFlight(int jumpAmount, CallbackInfo ci) {
        // CREATIVE ONLY. A gliding horse needs its jump: the leap is how
        // it gets off the ground, and HorseFlightMixin only lights the elytra
        // flag once the horse is airborne. Suppressing the rear for every flying
        // mode would have left a glider standing in a field unable to launch -
        // caught while fixing the owner's "glide horses do not work AT ALL"
        // (2026-09-17), which was a different bug in the same feature.
        if (horsegenetics$creativeFlying()) {
            ci.cancel();
        }
    }

    /**
     * <b>A creative-flying horse cannot jump, which is what stops it bucking.</b>
     *
     * <p>Suppressing {@code onPlayerJump} alone was not enough and the owner saw it at once: <i>"the creative-style
     * horse was definitely bucking while being ridden"</i> (2026-09-17). {@code onPlayerJump} is called on the
     * CLIENT only; the server reaches the rear by its own road, {@code handleStartJump}, which is
     * {@code allowStandSliding = true; standIfPossible(); playJumpSound();} - and {@code standIfPossible} is the
     * rear. Holding jump to climb therefore reared the horse on every press.
     *
     * <p>{@code canJump()} is the one gate above both: the client asks it before it will treat a mount as jumpable
     * at all, so answering false here removes the rear, the leap and the sound together. The climb is unaffected -
     * {@code getRiddenInput} reads {@code controller.isJumping()}, which is the key itself, not the horse's opinion
     * of it.
     *
     * <p>Gliders keep their jump: it is how they launch.
     */
    @Inject(method = "canJump", at = @At("RETURN"), cancellable = true)
    private void horsegenetics$noJumpingInFlight(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && horsegenetics$creativeFlying()) {
            cir.setReturnValue(false);
        }
    }

    /** The server's own road to the rear, which {@code canJump} does not stand in front of. */
    @Inject(method = "handleStartJump", at = @At("HEAD"), cancellable = true)
    private void horsegenetics$noRearOnStartJump(int jumpScale, CallbackInfo ci) {
        if (horsegenetics$creativeFlying()) {
            ci.cancel();
        }
    }

    @Unique
    private boolean horsegenetics$creativeFlying() {
        return (Object) this instanceof Horse horse
                && HorseFlight.of(horse).mode() == HorseFlight.Mode.TRUE_FLIGHT
                && FlightToggle.wants(horse);
    }

    /**
     * Flat climb rate while the rider holds jump, as a share of the full input. Vanilla's happy ghast uses half;
     * a horse that flies by ignoring physics may as well go up as fast as it goes along.
     */
    private static final float CLIMB = 1.0F;

    /**
     * <b>Full magnitude, deliberately.</b> This used to be 0.195, copied from a happy ghast, which shrank the input
     * before {@code CREATIVE_SPEED} ever saw it and made the two constants fight - the product was walking pace.
     * Speed belongs to exactly one number, and that number is {@code HorseFlightMixin.CREATIVE_SPEED}; this one's
     * only job is direction.
     *
     * <p>Leaving it at 1 also means {@code Entity.getInputVector} normalises anything longer than 1 - so climbing
     * while flying forward splits the rate between the two rather than adding a diagonal speed bonus, which is
     * vanilla's own behaviour and worth keeping.
     */
    private static final float INPUT_SCALE = 1.0F;

    @Inject(method = "getRiddenInput", at = @At("RETURN"), cancellable = true)
    private void horsegenetics$flightInput(Player controller, Vec3 selfInput,
                                           CallbackInfoReturnable<Vec3> cir) {
        if (!((Object) this instanceof Horse horse)) {
            return;
        }
        if (HorseFlight.of(horse).mode() != HorseFlight.Mode.TRUE_FLIGHT || !FlightToggle.wants(horse)) {
            return;
        }
        float strafe = controller.xxa;
        float forward = 0.0F;
        float up = 0.0F;
        if (controller.zza != 0.0F) {
            float pitch = controller.getXRot() * ((float) Math.PI / 180.0F);
            forward = Mth.cos(pitch);
            up = -Mth.sin(pitch);
            if (controller.zza < 0.0F) {
                // Reversing is half speed and inverts the climb, as vanilla has it.
                forward *= -0.5F;
                up *= -0.5F;
            }
        }
        if (controller.isJumping()) {
            up += CLIMB;
        }
        if (FlightToggle.descending(horse)) {
            // Sneak comes down. Vanilla's happy ghast has no descend key at all -
            // you point its nose down - so there is nothing to copy here; this is
            // the mod's own, and it is why ClientFlightInput has to take sneak
            // away from the server before it reads it as "get off".
            up -= CLIMB;
        }
        cir.setReturnValue(new Vec3(strafe, up, forward).scale(INPUT_SCALE));
    }
}
