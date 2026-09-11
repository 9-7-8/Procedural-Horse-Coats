package com.example.horsegenetics.neoforge.particle;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The mod's own particle types. One so far - see {@link HoofprintOptions} for
 * why it exists. The client half (what it looks like) is
 * {@code client/HoofprintParticle}, registered in {@code ClientSetup}.
 */
public final class ModParticles {

    /** The id a gene names it by - {@code MoltenHoovesGene.PARTICLE}. */
    public static final String HOOFPRINT_ID = HorseGenetics.MOD_ID + ":hoofprint";

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, HorseGenetics.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, ParticleType<HoofprintOptions>> HOOFPRINT =
            PARTICLE_TYPES.register("hoofprint", () -> new ParticleType<HoofprintOptions>(false) {
                @Override
                public MapCodec<HoofprintOptions> codec() {
                    return HoofprintOptions.CODEC;
                }

                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, HoofprintOptions> streamCodec() {
                    return HoofprintOptions.STREAM_CODEC;
                }
            });

    public static void register(IEventBus modEventBus) {
        PARTICLE_TYPES.register(modEventBus);
    }

    private ModParticles() {
    }
}
