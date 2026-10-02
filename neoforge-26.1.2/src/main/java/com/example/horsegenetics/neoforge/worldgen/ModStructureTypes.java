package com.example.horsegenetics.neoforge.worldgen;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers the mod's structure types - see {@link LevelJigsawStructure}. */
public final class ModStructureTypes {

    public static final DeferredRegister<StructureType<?>> TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, HorseGenetics.MOD_ID);

    public static final DeferredHolder<StructureType<?>, StructureType<LevelJigsawStructure>> LEVEL_JIGSAW =
            TYPES.register("level_jigsaw", () -> () -> LevelJigsawStructure.CODEC);

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
    }

    private ModStructureTypes() {
    }
}
