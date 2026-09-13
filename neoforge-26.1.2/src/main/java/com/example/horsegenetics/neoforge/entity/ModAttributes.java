package com.example.horsegenetics.neoforge.entity;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

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
        modEventBus.addListener(ModAttributes::onAttributes);
    }

    /**
     * Put it on horses only, and <b>subscribe explicitly rather than by
     * annotation</b>.
     *
     * <p>26.1.2's {@code @EventBusSubscriber} has no {@code bus} element - it
     * infers the bus from the event type - and when the attribute failed to
     * reach any horse the first time, that inference was the one link in the
     * chain that could not be checked by reading. An explicit
     * {@code addListener} on the bus we were handed removes the question
     * instead of answering it.
     *
     * <p>An attribute costs a slot on every entity that carries it, and nothing
     * else in this mod swims in lava on purpose. Anything without it falls
     * through {@code getAttribute() == null} in the mixin and takes vanilla's
     * constant.
     */
    /**
     * The horse family, <b>by identity rather than by class</b>.
     *
     * <p>This list looks clumsy beside
     * {@code AbstractHorse.class.isAssignableFrom(type.getBaseClass())}, which
     * is what was here first and which attached the attribute to <b>nothing</b>.
     * {@code EntityType.getBaseClass()} is a stub:
     *
     * <pre>public Class&lt;? extends Entity&gt; getBaseClass() { return Entity.class; }</pre>
     *
     * <p>It returns {@code Entity.class} for every type in the game, so any
     * class-based filter over entity types silently matches nothing. It
     * compiles, it runs, it logs no warning, and downstream
     * {@code GeneAbilityHandler.applyAttribute} takes its own silent early-out
     * on "this entity has no such attribute" - three layers of quiet, and a
     * feature that shipped doing nothing. See {@code wiki/api-notes.html}.
     */
    private static final Set<EntityType<?>> HORSE_TYPES = Set.of(
            EntityType.HORSE, EntityType.DONKEY, EntityType.MULE,
            EntityType.SKELETON_HORSE, EntityType.ZOMBIE_HORSE);

    private static void onAttributes(EntityAttributeModificationEvent event) {
        int added = 0;
        for (var type : event.getTypes()) {
            if (HORSE_TYPES.contains(type)) {
                event.add(type, LAVA_MOVEMENT);
                added++;
            }
        }
        // Say it out loud. "The horse has no such attribute" is a SILENT
        // early-out in GeneAbilityHandler.applyAttribute - correct there, and
        // the reason a whole feature could be wired up, ship, and do nothing
        // with no line anywhere saying so.
        HorseGenetics.LOGGER.info("[Debug] lava_movement attached to {} entity type(s)", added);
    }
}
