package com.example.horsegenetics.neoforge.mixin;

import com.example.horsegenetics.neoforge.server.HorseShoeEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.level.block.PowderSnowBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * <b>A shod horse stands on powder snow</b> (#247); an unshod one still sinks.
 *
 * <p>Vanilla asks one static question, {@code PowderSnowBlock.canEntityWalkOnPowderSnow},
 * from the block's collision shape and from {@code LivingEntity}'s climb out of
 * it. Its two answers are both wrong for a shoe: the
 * {@code powder_snow_walkable_mobs} tag is per entity <i>type</i>, so tagging
 * the horse would float every horse, shod or not; and the other is the item in
 * the {@code FEET} slot, which a horse does not have - its shoes are four gear
 * slots of this mod's. There is no method on the entity to override (the
 * treatment expected {@code Entity.canStandOnPowderSnow()}; 26.1.2 has none),
 * so this adds a third answer to the question itself.
 *
 * <p>At {@code RETURN} and only when vanilla said no, so anything vanilla or
 * another mod already lets walk is untouched. The cost is one gear lookup for
 * a horse that is touching powder snow, and nothing for anything else.
 *
 * <p><b>Not verified in a played game.</b> The gametest
 * {@code a_shod_horse_stands_on_powder_snow} asks the same question the block does.
 */
@Mixin(PowderSnowBlock.class)
public abstract class PowderSnowShoesMixin {

    @Inject(method = "canEntityWalkOnPowderSnow", at = @At("RETURN"), cancellable = true)
    private static void horsegenetics$shodHorsesStand(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && entity instanceof AbstractHorse horse && HorseShoeEffects.shod(horse)) {
            cir.setReturnValue(true);
        }
    }
}
