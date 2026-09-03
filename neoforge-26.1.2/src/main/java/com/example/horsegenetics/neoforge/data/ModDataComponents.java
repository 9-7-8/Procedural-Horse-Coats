package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Item data components for the gameplay layer. First one: {@code stored_genome}
 * ({@link StoredGenome}), the genotype + epigenome an item can carry - the
 * <b>stallion seed jar</b> uses it, and later the embryo / clone-source items
 * and any "export a horse" output will too.
 *
 * <p>This is the registration path the roadmap flagged as unverified against
 * 26.1.2: {@link DataComponentType#builder()} + {@code persistent(Codec)} +
 * {@code networkSynchronized(StreamCodec)}, on {@link Registries#DATA_COMPONENT_TYPE}.
 */
public final class ModDataComponents {

    public static final DeferredRegister<DataComponentType<?>> TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, HorseGenetics.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<StoredGenome>> STORED_GENOME =
            TYPES.register("stored_genome", () -> DataComponentType.<StoredGenome>builder()
                    .persistent(StoredGenome.CODEC)
                    .networkSynchronized(StoredGenome.STREAM_CODEC)
                    .build());

    /** Which horse an item is bound to - a {@code bound_stall_sign} uses it. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BoundHorse>> BOUND_HORSE =
            TYPES.register("bound_horse", () -> DataComponentType.<BoundHorse>builder()
                    .persistent(BoundHorse.CODEC)
                    .networkSynchronized(BoundHorse.STREAM_CODEC)
                    .build());

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
    }

    private ModDataComponents() {
    }
}
