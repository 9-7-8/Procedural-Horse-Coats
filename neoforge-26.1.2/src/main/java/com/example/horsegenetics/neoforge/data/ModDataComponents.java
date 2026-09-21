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

    /**
     * The three colours of a dyed saddle ({@link SaddleTint}) - seat leather,
     * bridle leather and metal hardware, dyed independently at the
     * <a href="https://9-7-8.github.io/Procedural-Horse-Coats/wiki/horse-gear.html#equestrian-bench">Equestrian
     * Bench</a>. Not {@code dyed_color}, because vanilla holds one value per
     * stack and reads it once for every layer; see that record for the rest.
     *
     * <p>Absent means undyed, and an undyed saddle renders exactly as vanilla's
     * does - which is what lets this ride on {@code minecraft:saddle} rather
     * than needing an item of our own.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SaddleTint>> TACK_TINT =
            TYPES.register("tack_tint", () -> DataComponentType.<SaddleTint>builder()
                    .persistent(SaddleTint.CODEC)
                    .networkSynchronized(SaddleTint.STREAM_CODEC)
                    .build());

    /**
     * <b>The wood of a jump's rails</b>, as a {@code JumpWoods} key - and
     * {@link #JUMP_STANDARDS} the wood of its uprights.
     *
     * <h2>Two components rather than one record, and the reason is the icon</h2>
     * A jump's block entity holds a {@code JumpMaterials} - one immutable record
     * of both woods - and that is the right shape for it. The <i>item</i> splits
     * it in two because of how a client item definition picks a model:
     * {@code minecraft:select} on {@code minecraft:component} matches the
     * <b>whole value</b> of one component, so a single pair-valued component
     * would need a case per pair - a hundred and forty-four of them per style -
     * to draw a mixed jump correctly. Split, the icon selects on the rails alone
     * in twelve cases and is never wrong about the half it shows.
     *
     * <p><b>Both are always present</b>, oak included, on every jump this mod
     * hands out - the recipes write them, the creative tab writes them, and the
     * loot table copies them off the block entity. That is deliberate: an item
     * with no components and an item saying "oak" are the same jump but do not
     * stack, and a creative-tab jump that will not stack with a crafted one is
     * the kind of thing that gets reported as an inventory bug.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> JUMP_RAILS =
            TYPES.register("jump_rails", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    /** The wood of a jump's uprights. See {@link #JUMP_RAILS}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> JUMP_STANDARDS =
            TYPES.register("jump_standards", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    /**
     * <b>The paint on a jump's rails</b> - an RGB multiplier - and
     * {@link #JUMP_STANDARDS_DYE} the paint on its uprights.
     *
     * <p><b>Absent means bare wood</b>, which is the opposite convention to
     * {@link #JUMP_RAILS} and is deliberate: almost every jump is unpainted, so
     * the common case carries no component and two plain jumps merge in a
     * chest. The wood components are always written because a jump is
     * <i>always</i> made of something; paint it usually is not.
     *
     * <p>A dye is <b>spent for good</b> - it never comes back out of the
     * block - and the only way to undo a coat is to put a fresh plank in that
     * half, which pops the old plank out and strips the paint with it. Owner's
     * design, and not realistic; it is legible, which is what one slot per half
     * doing two jobs needs to be.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> JUMP_RAILS_DYE =
            TYPES.register("jump_rails_dye", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /** The paint on a jump's uprights. See {@link #JUMP_RAILS_DYE}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> JUMP_STANDARDS_DYE =
            TYPES.register("jump_standards_dye", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /**
     * <b>How tall a jump is</b>, as an index into {@code JumpMaterials.SIZES} -
     * a ladder from a ground pole at a tenth of a block to double height.
     *
     * <p>An index rather than the number itself, because the model bakes one
     * scaled copy of every part per rung and the index <i>is</i> which copy.
     *
     * <p><b>Absent means one block</b>, the ordinary jump, the same convention
     * the dyes use and for the same reason: the common case carries no
     * component, so two plain jumps merge in a chest.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> JUMP_SIZE =
            TYPES.register("jump_size", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
    }

    private ModDataComponents() {
    }
}
