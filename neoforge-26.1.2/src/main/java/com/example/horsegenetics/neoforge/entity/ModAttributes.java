package com.example.horsegenetics.neoforge.entity;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * <b>Attributes this mod adds, and the first one is the reason the mod has a
 * mixin at all.</b>
 *
 * <h2>Why lava travel needed a new attribute rather than a new number</h2>
 * {@code LivingEntity.travelInLava} is, in 26.1.2 and in 1.12.2 alike,
 * {@code moveRelative(0.02F, input)} with a hardcoded drag - <b>it reads no
 * attribute at all</b>, which is what {@code known-gaps.html} gap 179 recorded
 * and what was re-verified by reading the method before this was written. So a
 * fireproof horse crossed lava at a fixed crawl and nothing in the format could
 * change it.
 *
 * <p>The obvious repair - nudge the horse's velocity every tick, the way
 * {@code walk_on_water} does - <b>does not work for the case that matters</b>. A
 * ridden horse is simulated on the rider's client and the server's velocity is
 * overwritten by the position packets coming back. The comment on
 * {@code lava_swim} already says so, which is why buoyancy there is a
 * {@code GRAVITY} modifier and not a nudge: <b>attributes sync to the
 * client</b>. So the speed has to be an attribute, and vanilla has none, so
 * this is one.
 *
 * <p>{@link #LAVA_MOVEMENT}'s default is <b>exactly vanilla's constant</b>. An
 * entity that never gets a modifier - every mob in the game, and every horse
 * without the gene - therefore moves through lava exactly as it always did, and
 * the mixin is a no-op for it. That is the property that makes patching a
 * method on {@code LivingEntity} defensible at all.
 */
public final class ModAttributes {

    /** Vanilla's hardcoded lava acceleration, and therefore this attribute's default. */
    public static final double VANILLA_LAVA_SPEED = 0.02;

    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, HorseGenetics.MOD_ID);

    /**
     * How fast a mob accelerates while swimming in lava.
     *
     * <p>The terminal speed is roughly twice this, because the same method
     * halves the remaining velocity every tick - so vanilla's 0.02 settles at
     * about 0.04 blocks a tick, which is the crawl. A value of 0.1 lands near
     * an ordinary walk.
     */
    public static final DeferredHolder<Attribute, Attribute> LAVA_MOVEMENT =
            ATTRIBUTES.register("lava_movement", () -> new RangedAttribute(
                    "attribute.name.horsegenetics.lava_movement",
                    VANILLA_LAVA_SPEED, 0.0, 1.0).setSyncable(true));

    private ModAttributes() {
    }

    public static void register(IEventBus modEventBus) {
        ATTRIBUTES.register(modEventBus);
    }

    /**
     * Put it on horses only.
     *
     * <p>An attribute costs a slot on every entity that carries it, and nothing
     * else in this mod is going to swim in lava on purpose. Anything without it
     * falls through {@code getAttribute() == null} in the mixin and takes
     * vanilla's constant.
     */
    @EventBusSubscriber(modid = HorseGenetics.MOD_ID)
    public static final class Attach {
        private Attach() {
        }

        @SubscribeEvent
        static void onAttributes(EntityAttributeModificationEvent event) {
            for (var type : event.getTypes()) {
                if (AbstractHorse.class.isAssignableFrom(type.getBaseClass())) {
                    event.add(type, LAVA_MOVEMENT);
                }
            }
        }
    }
}
