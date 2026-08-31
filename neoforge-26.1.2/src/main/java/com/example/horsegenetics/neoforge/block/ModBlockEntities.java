package com.example.horsegenetics.neoforge.block;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, HorseGenetics.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HayPortalBlockEntity>> HAY_PORTAL =
            BLOCK_ENTITIES.register("hay_portal",
                    () -> new BlockEntityType<>(HayPortalBlockEntity::new, ModBlocks.HAY_PORTAL.get()));

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }

    private ModBlockEntities() {
    }
}
