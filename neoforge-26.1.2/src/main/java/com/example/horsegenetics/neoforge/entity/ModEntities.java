package com.example.horsegenetics.neoforge.entity;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The mod's own entities. One so far: the {@link Cowboy}.
 *
 * <p>He is <b>never spawned naturally</b> - there is no spawn egg and no biome
 * spawner entry. The only thing that makes one is the barn structure, which
 * carries him in its {@code entities} list the same way vanilla's
 * {@code village/plains/villagers/*.nbt} carry villagers. Everything about him
 * is then built on his first server tick by {@code CowboyHandler}.
 *
 * <p>Sized and tracked like the wandering trader, whose model and texture he
 * borrows for now ({@code client/CowboyRenderer}).
 */
@EventBusSubscriber(modid = HorseGenetics.MOD_ID)
public final class ModEntities {

    public static final DeferredRegister.Entities ENTITY_TYPES =
            DeferredRegister.createEntities(HorseGenetics.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<Cowboy>> COWBOY =
            ENTITY_TYPES.registerEntityType("cowboy", Cowboy::new, MobCategory.MISC,
                    builder -> builder.sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10));

    @SubscribeEvent
    static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(COWBOY.get(), Cowboy.createAttributes().build());
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }

    private ModEntities() {
    }
}
