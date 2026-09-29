package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.ServerConfig;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * <b>Mining from horseback at full speed.</b>
 *
 * <p>Vanilla makes a player who is not standing on the ground mine at a fifth of
 * their usual speed - the rule that stops you tunnelling while you fall. A rider
 * is not standing on anything, so the same fifth applies to a player on a horse,
 * and clearing a sapling from the path means getting off. Nothing about that is
 * a deliberate rule for riders; it is a rule about falling that a rider happens
 * to satisfy. This class exempts them.
 *
 * <h2>Where the fifth comes from, and why the event can undo it exactly</h2>
 *
 * <p>{@code Player.getDestroySpeed} ends with {@code if (!this.onGround())
 * speed /= 5.0F;} and <i>then</i> fires {@code PlayerEvent.BreakSpeed} with the
 * divided figure. Verified by reading the decompiled method in 26.1.2, because
 * the ordering is the whole design: the penalty is applied before any listener
 * sees the number, so multiplying by the same five here restores the undivided
 * speed precisely rather than approximating it. Were the event fired first, this
 * would have to be a multiply of 5 that vanilla then divided straight back.
 *
 * <h2>The guard is on the penalty, not on the posture</h2>
 *
 * <p>The obvious shape - "mounted, so multiply" - is a 5x mining <i>boost</i> the
 * moment the premise is wrong, and the premise ("a mounted player's
 * {@code onGround} is false") is a reading of vanilla's movement code rather
 * than something this mod controls. So the condition asks
 * {@code !player.onGround()} directly: the multiply happens only when the
 * division provably just happened, and if a future version leaves a rider
 * standing on their horse this quietly does nothing instead of handing out five
 * times the mining speed.
 *
 * <p><b>The horse must be on the ground too.</b> Gating on the vehicle as well
 * keeps the exemption to the case that was asked for. A horse in mid-jump, or a
 * flying one, leaves its rider genuinely airborne, and mining at full speed out
 * of the air is not a comfort fix - it is a different game.
 *
 * <h2>Both sides, deliberately</h2>
 *
 * <p>Registered with no {@code Dist}, so it runs on the client as well as the
 * server. The client predicts break progress from its own call to the same
 * method, so a server-only handler would draw a crack that crept while the block
 * broke early. {@code behaviour.mounted_mining_penalty_removed} lives in
 * {@link ServerConfig}, which is {@code ModConfig.Type.SERVER} and therefore
 * "synced to clients during connection" (FML's own words on {@code ModConfig.Type}) -
 * so both sides read the server's value, not two independent defaults.
 */
@EventBusSubscriber(modid = HorseGenetics.MOD_ID)
public final class MountedMiningHandler {

    private MountedMiningHandler() {
    }

    /**
     * Vanilla's airborne divisor, and so exactly what undoes it. Not a tunable:
     * a different number here would not "remove the penalty", it would invent a
     * mounted mining speed.
     */
    private static final float AIRBORNE_DIVISOR = 5.0F;

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!ServerConfig.mountedMiningPenaltyRemoved()) {
            return;
        }
        Player player = event.getEntity();
        if (player.onGround()) {
            return;     // vanilla charged nothing, so there is nothing to give back
        }
        if (!(player.getVehicle() instanceof AbstractHorse horse) || !horse.onGround()) {
            return;     // falling, jumping or flying - the penalty is meant for them
        }
        event.setNewSpeed(event.getNewSpeed() * AIRBORNE_DIVISOR);
    }
}
