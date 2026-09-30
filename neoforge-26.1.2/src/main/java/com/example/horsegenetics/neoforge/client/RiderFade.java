package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * <b>How see-through the horse under you is, this frame.</b> Looking at the
 * ground from the saddle means looking through the horse, and this is the one
 * angle a mount is genuinely in the way at.
 *
 * <p>One number comes out of here - {@link #alphaFor} - and
 * {@link GeneticHorseRenderState#fadeAlpha} carries it to everything that draws
 * a part of the horse. Every one of those draws fades <i>together</i>: the
 * coat, the glow of an emissive gene, a braid, the saddle and the barding. A
 * saddle left hanging in the air over a ghost is the failure this is written to
 * avoid, and it is the reason the gear needed a mixin of its own - see
 * {@code mixin/FadingGearMixin}.
 *
 * <h2>What it is not</h2>
 * <b>Only the horse you are riding, and only on your screen.</b> Nobody else
 * sees your mount fade; this is a drawing setting in {@link ClientConfig} and
 * touches no rule, no packet and no server. Other horses - including one
 * somebody else is riding beside you - stay solid.
 *
 * <p><b>It never reaches zero.</b> The floor is
 * {@code visual.rideFadeMinOpacity}, 0.25 by default, and the config will not
 * take less than 0.05. A mount you cannot see at all is a mount you forget you
 * are on. (Owner's call.)
 */
public final class RiderFade {

    /** Fully solid. The overwhelmingly common answer, and the cheap path. */
    public static final float OPAQUE = 1.0F;

    private RiderFade() {
    }

    /**
     * The alpha to draw {@code horse} at this frame, {@code 1.0} for every
     * horse that is not the one under the camera's own player.
     *
     * <p>Ordered so that the common case - a horse nobody is riding - costs an
     * identity comparison and nothing else. It is called once per horse per
     * frame.
     */
    public static float alphaFor(Entity horse) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.getVehicle() != horse) {
            return OPAQUE;
        }
        if (!ClientConfig.rideFade()) {
            return OPAQUE;
        }
        float floor = (float) ClientConfig.rideFadeMinOpacity();
        if (floor >= OPAQUE) {
            return OPAQUE;      // a floor of 1 is the switch off, said differently
        }
        // xRot() is degrees below the horizon: 0 ahead, 90 straight down. The
        // camera's, not the player's, so it is right in third person too - what
        // matters is where the view is aimed, not where the rider's head is.
        return ramp(mc.gameRenderer.getMainCamera().xRot(),
                ClientConfig.rideFadeStartPitch(), ClientConfig.rideFadeFullPitch(), floor);
    }

    /**
     * The ramp itself: fully solid at or above {@code start}, at {@code floor}
     * from {@code full} down, and linear between.
     *
     * <p><b>Pure, and deliberately written without {@code Mth}.</b> It is the
     * only part of the fade that is arithmetic rather than rendering, and
     * keeping it free of Minecraft is what lets {@code RiderFadeRampTest} run
     * it at all - this module's test classpath carries no Minecraft on
     * purpose. Everything else here needs a client on a screen and is a
     * Verification-tab matter.
     *
     * <p>A {@code full} at or below {@code start} would divide by zero or run
     * the ramp backwards. The config allows that pair deliberately - it is a
     * legal way to ask for a hard switch rather than a ramp - so the
     * degenerate case is answered here rather than refused there.
     */
    public static float ramp(float pitchDegrees, float start, float full, float floor) {
        if (pitchDegrees <= start) {
            return OPAQUE;
        }
        if (full <= start) {
            return floor;       // a hard switch at start, not a divide by zero
        }
        float through = (pitchDegrees - start) / (full - start);
        if (through >= 1.0F) {
            return floor;
        }
        return OPAQUE + through * (floor - OPAQUE);
    }

    /**
     * {@code alpha} as a white ARGB tint, which is what every draw call this
     * touches wants: vanilla multiplies the tint into the texture, so white
     * with a reduced alpha means "the same colours, fainter".
     */
    public static int tint(float alpha) {
        return ARGB.color(Mth.clamp(Math.round(alpha * 255.0F), 0, 255), 255, 255, 255);
    }

    /**
     * Fade an existing colour - a dyed braid, a dyed piece of tack - by
     * {@code alpha}, keeping whatever alpha it already carried.
     */
    public static int fade(int argb, float alpha) {
        return ARGB.multiply(argb, tint(alpha));
    }
}
