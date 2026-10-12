package com.example.horsegenetics.neoforge.mixin;

import com.example.horsegenetics.common.gear.HorseShoes;
import com.example.horsegenetics.neoforge.server.HorseShoeEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * <b>A horse in grip shoes slides less on ice</b> (#248).
 *
 * <p>How far a mob slides is decided in one place: {@code LivingEntity.travelInAir}
 * reads the block under it once a tick,
 * {@code getBlockState(posBelow).getFriction(level, posBelow, this)}, into the
 * local {@code blockFriction}, and both the push it gets from its own speed
 * and the drag on what it already had come from that one number. The answer is
 * the block's, and there is no event and nothing on the entity to override -
 * NeoForge's {@code getFriction} hook is for a mod's own blocks, not for
 * vanilla ice. So this changes the number where it is stored: the first
 * {@code float} local of the method, which is {@code blockFriction}.
 *
 * <p>A {@code ModifyVariable} and not a {@code Redirect} of the call, so that
 * another mod changing the same friction composes with this instead of one of
 * the two failing to apply.
 *
 * <p>It runs for every living thing every tick, so the cheap tests come
 * first: only a block slicker than ordinary ground, and only a horse, ever
 * reaches the gear lookup. Both sides, because a ridden horse is moved by its
 * rider's client; the shoes it reads are in the synced gear attachment.
 *
 * <p><b>Not verified in a played game.</b> The gametest
 * {@code grip_shoes_slide_less_on_ice} slides two horses from the same push.
 */
@Mixin(LivingEntity.class)
public abstract class ShoeGripMixin {

    @ModifyVariable(method = "travelInAir", at = @At("STORE"), ordinal = 0)
    private float horsegenetics$gripShoesHoldOnIce(float blockFriction) {
        if (blockFriction > HorseShoes.ORDINARY_FRICTION && (Object) this instanceof AbstractHorse horse) {
            return HorseShoeEffects.friction(horse, blockFriction);
        }
        return blockFriction;
    }
}
