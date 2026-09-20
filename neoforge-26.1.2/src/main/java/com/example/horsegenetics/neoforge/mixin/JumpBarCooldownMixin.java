package com.example.horsegenetics.neoforge.mixin;

import com.example.horsegenetics.neoforge.ServerConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ContextualBarRenderer;
import net.minecraft.client.gui.contextualbar.JumpableVehicleBarRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.PlayerRideableJumping;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * <b>Make the jump meter drain</b> while the horse is on its jump cooldown,
 * instead of sitting full.
 *
 * <p>Vanilla already shows the bar during a cooldown - {@code Gui
 * .willPrioritizeJumpInfo} counts a nonzero {@code getJumpCooldown()} as reason
 * enough - but it blits one static full-width "cooldown" sprite, which says
 * <i>that</i> you are waiting and not <i>how long</i>. With
 * {@link HorseInstantJumpMixin} the wait is the entire mechanic, so the bar has
 * to answer the second question. Owner's ask: "you see the jump meter go down
 * over about a second or so until the horse can jump again."
 *
 * <p><b>The bar reads the rider's own client</b>, which is the side that holds
 * the authoritative cooldown for a ridden horse, so there is nothing to sync
 * and nothing to predict.
 *
 * <h2>Why HEAD-and-cancel rather than something smaller</h2>
 * The fill and the choice of sprite are decided inside one {@code if} in
 * {@code extractBackground}, so redirecting the {@code getJumpCooldown()} call
 * would still leave the full-width sprite, and modifying the {@code progress}
 * local would still be in the wrong branch. Taking the whole method is honest
 * about the fact that we are replacing its behaviour. <b>It also means a
 * vanilla change to that method is silently ignored rather than merged</b> -
 * if the bar ever looks wrong after an update, look here first.
 *
 * <p>Falls through to vanilla untouched when {@code ride.instant_jump} is off,
 * so the config really does restore vanilla rather than half of it.
 *
 * <h2>UNVERIFIED</h2>
 * Never drawn. The sprite names and the 182x5 geometry are copied from
 * {@code JumpableVehicleBarRenderer} in the 26.1.2 sources jar.
 */
@Mixin(JumpableVehicleBarRenderer.class)
public abstract class JumpBarCooldownMixin implements ContextualBarRenderer {

    @Unique
    private static final Identifier horsegenetics$BACKGROUND =
            Identifier.withDefaultNamespace("hud/jump_bar_background");

    @Unique
    private static final Identifier horsegenetics$PROGRESS =
            Identifier.withDefaultNamespace("hud/jump_bar_progress");

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private PlayerRideableJumping playerJumpableVehicle;

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void horsegenetics$drainingBar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker,
                                           CallbackInfo ci) {
        int cooldown = this.playerJumpableVehicle.getJumpCooldown();
        if (!ServerConfig.instantJump() || cooldown <= 0) {
            return;
        }
        int total = ServerConfig.jumpCooldownTicks();
        if (total <= 0) {
            return;
        }
        int left = this.left(this.minecraft.getWindow());
        int top = this.top(this.minecraft.getWindow());
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, horsegenetics$BACKGROUND, left, top, 182, 5);
        // Remaining, not elapsed: the bar empties as the horse recovers, so
        // "full" means ready in both modes rather than meaning opposite things.
        int width = Math.min(182, 182 * Math.min(cooldown, total) / total);
        if (width > 0) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, horsegenetics$PROGRESS,
                    182, 5, 0, 0, left, top, width, 5);
        }
        ci.cancel();
    }
}
