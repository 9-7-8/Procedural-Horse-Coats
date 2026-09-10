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

    /**
     * <b>Marks a saddle the mod put on a horse, not the player.</b>
     *
     * <p>Bareback riding at the top bond tier works by quietly equipping a real
     * saddle, because vanilla decides who may steer inside
     * {@code AbstractHorse.getControllingPassenger()} and that check is
     * {@code isSaddled()} - there is no hook on it and no mixins here. A real
     * saddle therefore buys the real thing: client-predicted movement, jumping,
     * the correct feel. This component is what keeps that from becoming a lie -
     * the renderer skips drawing it, and
     * {@code server/BarebackSteeringHandler} takes it back off the moment the
     * rider is gone, so it never becomes a saddle the player owns.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> PHANTOM_SADDLE =
            TYPES.register("phantom_saddle", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
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
     * sells one out of their herd. Redeeming it moves that horse's ownership to
     * whoever right-clicks the animal with it.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<
            com.example.horsegenetics.common.horse.TransferDeed>> HORSE_DEED =
            TYPES.register("horse_deed", () -> DataComponentType
                    .<com.example.horsegenetics.common.horse.TransferDeed>builder()
                    .persistent(TransferDeedCodecs.CODEC)
                    .networkSynchronized(TransferDeedCodecs.STREAM_CODEC)
                    .build());

    /**
     * The breed a {@code breed_spawn_egg} spawns - a breed id. It is a component
     * rather than one item per breed because a breed added by a player after the
     * jar was built has no registry entry to be an item, and must still get an
     * egg. See {@code item/BreedSpawnEggItem}.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> BREED_ID =
            TYPES.register("breed", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    /**
     * Whether the horse a {@code preset_horse_spawn_egg} records was a foal.
     * Beside the genome rather than in it, because age is not genetic - the same
     * reason {@link StoredGenome} does not store one either.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> PRESET_BABY =
            TYPES.register("preset_baby", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
    }

    private ModDataComponents() {
    }
}
