package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
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

    /**
     * The breeding-carrot effects an item carries, as
     * {@link com.example.horsegenetics.common.genetics.CarrotEffect#id()}
     * tokens (roadmap &sect;14): the four base carrots carry one, a Known Gene Splice
     * carrot carries its {@code known:<gene>:het|hom}, a combination carrot
     * carries the merged list.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<String>>> CARROT_EFFECTS =
            TYPES.register("carrot_effects", () -> DataComponentType.<List<String>>builder()
                    .persistent(Codec.STRING.listOf())
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()))
                    .build());

    /** The gene a {@code research_paper} documents - a gene key ({@code <modid>.<gene>}). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> RESEARCH_GENE =
            TYPES.register("research_gene", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    /**
     * Who a {@code blank_transfer_paper} was crafted by ({@link PaperBearer}).
     * Only that player may sign it, and only against a horse they own.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PaperBearer>> PAPER_BEARER =
            TYPES.register("paper_bearer", () -> DataComponentType.<PaperBearer>builder()
                    .persistent(PaperBearer.CODEC)
                    .networkSynchronized(PaperBearer.STREAM_CODEC)
                    .build());

    /**
     * The horse a {@code signed_transfer_paper} is a claim on
     * ({@link com.example.horsegenetics.common.horse.TransferDeed}) - written
     * when a player signs a blank against their own horse, or when the cowboy
     * sells one out of his herd. Redeeming it moves that horse's ownership to
     * whoever right-clicks the animal with it.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<
            com.example.horsegenetics.common.horse.TransferDeed>> HORSE_DEED =
            TYPES.register("horse_deed", () -> DataComponentType
                    .<com.example.horsegenetics.common.horse.TransferDeed>builder()
                    .persistent(TransferDeedCodecs.CODEC)
                    .networkSynchronized(TransferDeedCodecs.STREAM_CODEC)
                    .build());

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
    }

    private ModDataComponents() {
    }
}
