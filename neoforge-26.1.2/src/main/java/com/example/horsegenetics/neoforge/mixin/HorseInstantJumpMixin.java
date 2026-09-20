package com.example.horsegenetics.neoforge.mixin;

import com.example.horsegenetics.neoforge.ServerConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * <b>Press to jump, at full height, then wait.</b> Replaces vanilla's
 * hold-to-charge leap with an instant one and a cooldown.
 *
 * <h2>Why, and it is not only feel</h2>
 * Vanilla charges: you hold jump, a meter fills over ten ticks, and the horse
 * leaps on <i>release</i> at whatever the meter reached. So how high a horse
 * jumps is partly a fact about the rider's thumb - a well-bred horse ridden
 * badly clears less than a mediocre one ridden well. In a mod whose whole
 * argument is that the horse's <em>genetics</em> decide what it can do, that is
 * the player's hand on the scale. Owner's ask, and it makes the
 * {@code wiki/item-jumps.html} height ladder an honest instrument: what a horse
 * clears is now a fact about the horse.
 *
 * <p>The owner's words for the thing being fixed on the feel side: a jump
 * &ldquo;just kind of feels like a hop&rdquo;.
 *
 * <h2>Mojang already shipped this, on the camel</h2>
 * {@code PlayerRideableJumping.getJumpCooldown()} is <b>already in the
 * interface</b> in 26.1.2, and two vanilla systems already honour it without
 * being asked:
 * <ul>
 *   <li>{@code LocalPlayer.aiStep} skips its entire charge block when the
 *       cooldown is nonzero, and forces {@code jumpRidingScale} to zero. That
 *       is what stops vanilla's release-triggered jump firing a second time
 *       after ours - <b>we do not have to suppress it, the cooldown does</b>.</li>
 *   <li>{@code Gui.willPrioritizeJumpInfo} already treats a nonzero cooldown as
 *       "show the jump bar", so the meter appears while it drains with no work
 *       here. Only the <i>drawing</i> of the drain needed a mixin, and that is
 *       {@code JumpBarCooldownMixin}.</li>
 * </ul>
 * {@code Camel} is the same feature already built: an instant dash on press,
 * its own {@code dashCooldown} counted down on both sides, and a much heavier
 * forward throw. Read it before changing anything here.
 *
 * <h2>How the press is caught without touching LocalPlayer</h2>
 * {@code LocalPlayer.applyInput} sets {@code LivingEntity.jumping} straight from
 * the jump key, and {@code isJumping()} is public - so on the controlling client
 * the horse can simply ask its rider whether the key is down. No mixin on
 * {@code LocalPlayer}, no access transformer, and no packet: vanilla's
 * {@code tickRidden} already executes a pending jump on the authoritative side,
 * so setting the pending scale at its HEAD makes the jump happen that same tick.
 *
 * <p><b>That also sidesteps a trap.</b> Firing on key-press through vanilla's
 * own path would send {@code START_RIDING_JUMP} with a charge of zero, and
 * {@code ServerGamePacketListenerImpl} drops that packet on a {@code data > 0}
 * guard - costing the jump sound and the rear, silently. We never send it;
 * {@link #horsegenetics$instantJump} plays the sound itself.
 *
 * <h2>UNVERIFIED</h2>
 * Nothing here has been ridden. Every claim about vanilla above was read out of
 * {@code minecraft-patched-26.1.2.100-sources.jar}, but the sources have lied
 * about modifiers before (see {@code accesstransformer.cfg}), and mixin targets
 * only fail at load.
 */
@Mixin(AbstractHorse.class)
public abstract class HorseInstantJumpMixin {

    @Shadow
    protected float playerJumpPendingScale;

    @Shadow
    protected abstract void playJumpSound();

    /**
     * Ticks remaining before this horse may jump again.
     *
     * <p>A plain field per side rather than an attachment or a payload, which
     * is how {@code Camel} does it: the side that runs the jump starts its own
     * counter, and neither waits for the other. For a player-ridden horse the
     * controlling client is the authoritative one and is also the only side
     * whose answer the jump bar reads, so there is nothing to sync.
     */
    @Unique
    private int horsegenetics$jumpCooldown;

    /**
     * <b>The one method the vanilla interface already asks for.</b> Returning
     * nonzero here is what makes the client refuse a second jump and what makes
     * the HUD show the bar at all.
     *
     * <p>Declared as a plain override rather than an {@code @Inject}, because
     * {@code AbstractHorse} does not declare it - it inherits the interface
     * default. {@code HorseTwoRidersMixin} hit exactly this and has the same
     * shape; an inject on an inherited method finds no target and, with
     * {@code defaultRequire: 1}, fails the whole mixin at load.
     */
    public int getJumpCooldown() {
        return this.horsegenetics$jumpCooldown;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void horsegenetics$tickJumpCooldown(CallbackInfo ci) {
        if (this.horsegenetics$jumpCooldown > 0) {
            this.horsegenetics$jumpCooldown--;
        }
    }

    /**
     * Turn "the rider is holding jump" into a jump, this tick, at full power.
     *
     * <p>Vanilla's own body then does the work: {@code tickRidden} executes a
     * pending scale the moment it sees one on the ground. Setting it at HEAD
     * rather than calling {@code executeRidersJump} ourselves keeps us inside
     * the vanilla mounted path, which is this mod's standing rule - see gap 156
     * and {@code BarebackSteeringHandler}, whose first version drove the horse
     * from outside the path and "felt wrong: server-driven, so a beat behind
     * the input, with no jump and none of vanilla's acceleration curve."
     */
    @Inject(method = "tickRidden", at = @At("HEAD"))
    private void horsegenetics$instantJump(Player controller, Vec3 riddenInput, CallbackInfo ci) {
        AbstractHorse self = (AbstractHorse) (Object) this;
        if (!ServerConfig.instantJump()
                || !self.isLocalInstanceAuthoritative()
                || !self.onGround()
                || this.horsegenetics$jumpCooldown > 0
                || this.playerJumpPendingScale > 0.0F
                || !self.canJump()
                || !controller.isJumping()) {
            return;
        }
        // Full power, every time. getPlayerJumpPendingScale's 0.4-to-1.0 ramp
        // is the charge curve and this is what replaces it.
        this.playerJumpPendingScale = 1.0F;
        this.playJumpSound();
    }

    /**
     * Throw the horse further forward, and start the cooldown.
     *
     * <p>TAIL rather than a rewrite: vanilla has already set the vertical
     * impulse and added its own forward nudge, and this adds more of the same
     * on top. <b>Height is deliberately untouched</b> - it is
     * {@code Attributes.JUMP_STRENGTH} and therefore genetics, and it is what
     * {@code HorseUnits.jumpMetres} prints on the horse screen. Changing the
     * forward throw cannot make that figure a lie; changing the impulse would.
     *
     * <p>Gated on {@code input.z > 0} exactly as vanilla's own term is, so a
     * horse jumping from a standstill still goes straight up rather than
     * lurching forward at a fence it was not approaching.
     */
    @Inject(method = "executeRidersJump", at = @At("TAIL"))
    private void horsegenetics$forwardThrow(float amount, Vec3 input, CallbackInfo ci) {
        this.horsegenetics$jumpCooldown = ServerConfig.instantJump()
                ? ServerConfig.jumpCooldownTicks()
                : 0;

        double boost = ServerConfig.jumpForwardBoost();
        if (boost <= 0.0 || input.z <= 0.0) {
            return;
        }
        AbstractHorse self = (AbstractHorse) (Object) this;
        float sin = Mth.sin(self.getYRot() * (float) (Math.PI / 180.0));
        float cos = Mth.cos(self.getYRot() * (float) (Math.PI / 180.0));
        // The same 0.4F term vanilla just applied, once more over, scaled.
        self.setDeltaMovement(self.getDeltaMovement()
                .add(-0.4F * sin * amount * boost, 0.0, 0.4F * cos * amount * boost));
    }
}
