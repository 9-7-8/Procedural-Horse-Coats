package com.example.horsegenetics.neoforge.mixin;

import com.example.horsegenetics.neoforge.entity.ModAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * <b>The one mixin in this project, and the argument for it.</b>
 *
 * <p>{@code LivingEntity.travelInLava} opens with
 * {@code this.moveRelative(0.02F, input)}. That constant is the entire speed of
 * lava travel, it is <b>hardcoded</b>, and the method reads no attribute -
 * verified by reading 26.1.2's source, and the same shape exists in 1.12.2's
 * {@code moveEntityWithHeading}. {@code known-gaps.html} gap 179 asked whether
 * a lava crossing that slow was worth this project's first mixin; the owner's
 * answer was yes.
 *
 * <h2>Why nothing short of this works</h2>
 * Three cheaper repairs were tried on paper and all three fail on the case that
 * matters, which is a horse with somebody <i>riding</i> it:
 * <ul>
 *   <li><b>A velocity nudge</b>, the way {@code walk_on_water} works, is
 *       overwritten - a ridden horse is simulated on the rider's client and the
 *       server's copy is moved by the position packets coming back.</li>
 *   <li><b>An existing attribute</b> - there is none. {@code MOVEMENT_SPEED} is
 *       not consulted anywhere in that method.</li>
 *   <li><b>NeoForge's {@code FluidType.move} hook</b> belongs to the fluid
 *       type, and vanilla lava's type is vanilla's.</li>
 * </ul>
 *
 * <h2>Why this is a safe place to put one</h2>
 * It is a {@link ModifyConstant} on a single float in a single private method,
 * and <b>it returns vanilla's own value unless the entity carries this mod's
 * attribute</b> - which is horses only, and only horses with the gene ever get
 * a modifier on it. Every other mob in the game, and every horse in a world
 * where nothing expresses, takes exactly the path it took before. A mixin on
 * {@code LivingEntity} sits in a hot path shared with every other mod in the
 * pack, so "does nothing unless asked" is not a nicety here; it is the whole
 * licence to be there.
 *
 * <h2>What it costs the backport</h2>
 * Nothing that matters. The interesting half - the epigenetic speed, its
 * founder floor, breeding for a faster swimmer - lives in {@code common/} and
 * is plain Java. This class is the translation, which is what the platform
 * module is for; a {@code forge-1.12.2/} would need its own fifteen lines
 * against {@code moveEntityWithHeading} and nothing else would move.
 */
@Mixin(LivingEntity.class)
public abstract class LavaTravelMixin {

    /**
     * Replace lava's hardcoded acceleration with this entity's attribute.
     *
     * <p>The constant appears once in the method. If a future version uses
     * 0.02F somewhere else in it, this starts modifying two things silently -
     * which is the standing hazard of {@code ModifyConstant} and the reason
     * this javadoc says so out loud.
     */
    @ModifyConstant(method = "travelInLava", constant = @Constant(floatValue = 0.02F))
    private float horsegenetics$lavaSpeed(float vanilla) {
        LivingEntity self = (LivingEntity) (Object) this;
        AttributeInstance instance = self.getAttribute(ModAttributes.LAVA_MOVEMENT);
        return instance == null ? vanilla : (float) instance.getValue();
    }
}
