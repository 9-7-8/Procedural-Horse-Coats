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

    public static final DeferredHolder<BlockEntityType<?>,
            BlockEntityType<EquineResearchShelfBlockEntity>> RESEARCH_SHELF =
            BLOCK_ENTITIES.register("equine_research_shelf",
                    () -> new BlockEntityType<>(EquineResearchShelfBlockEntity::new,
                            ModBlocks.RESEARCH_SHELF.get()));

    public static final DeferredHolder<BlockEntityType<?>,
            BlockEntityType<HorseStasisBankBlockEntity>> HORSE_STASIS_BANK =
            BLOCK_ENTITIES.register("horse_stasis_bank",
                    () -> new BlockEntityType<>(HorseStasisBankBlockEntity::new,
                            ModBlocks.HORSE_STASIS_BANK.get()));

    /**
     * <b>Every jump has one</b>, and it holds nothing but the two woods it is
     * made of. Data only - no ticker, no inventory. See {@link JumpBlockEntity}
     * for why that matters when a course is hundreds of blocks.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<JumpBlockEntity>> JUMP =
            BLOCK_ENTITIES.register("jump",
                    () -> new BlockEntityType<>(JumpBlockEntity::new, ModBlocks.JUMP.get()));

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }

    private ModBlockEntities() {
    }
}
