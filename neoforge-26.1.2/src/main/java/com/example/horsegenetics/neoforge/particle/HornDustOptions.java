package com.example.horsegenetics.neoforge.particle;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

/**
 * <b>A mote of a unicorn's horn dust</b> - {@code HornDustGene}. The same two
 * colours vanilla's {@code dust_color_transition} carries, under a type of the
 * mod's own, because vanilla's coloured dust hangs in the air where it appears and
 * this has to fall. The client half is {@code client/HornDustParticle}.
 *
 * @param fromColor RGB it starts as - the horn's base colour
 * @param toColor   RGB it fades to as it falls - the horn's tip colour, the same
 *                  as {@code fromColor} on a one-colour horn
 */
public record HornDustOptions(int fromColor, int toColor) implements ParticleOptions {

    public static final MapCodec<HornDustOptions> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ExtraCodecs.RGB_COLOR_CODEC.fieldOf("from_color").forGetter(HornDustOptions::fromColor),
            ExtraCodecs.RGB_COLOR_CODEC.fieldOf("to_color").forGetter(HornDustOptions::toColor)
    ).apply(i, HornDustOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, HornDustOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, HornDustOptions::fromColor,
            ByteBufCodecs.INT, HornDustOptions::toColor,
            HornDustOptions::new);

    @Override
    public ParticleType<HornDustOptions> getType() {
        return ModParticles.HORN_DUST.get();
    }
}
