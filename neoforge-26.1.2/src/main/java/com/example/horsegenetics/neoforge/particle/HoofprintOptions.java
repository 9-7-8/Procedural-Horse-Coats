package com.example.horsegenetics.neoforge.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

/**
 * <b>One glowing hoofprint</b> - what {@code horsegenetics:hoofprint} carries
 * from the server to the client.
 *
 * <p>The reason this mod has its own particle at all: no vanilla particle is
 * both <i>coloured</i> and <i>emissive</i> and <i>lies flat</i>. Dust takes a
 * colour but is lit by the world and always faces the camera; flame glows but
 * ignores colour. Molten hooves needs all three - the colour is written on each
 * allele copy - so the print carries its own two colours (the one it lands in
 * and the one it cools to), the heading it was made on, and the horse's size.
 *
 * @param fromColor RGB the print is laid down in
 * @param toColor   RGB it fades to as it cools
 * @param yaw       the horse's heading in degrees, so the toe points the way it went
 * @param scale     the horse's scale, so a draught horse leaves a bigger print
 * @param glow      full brightness, or lit by the world like anything else -
 *                  molten hooves' black allele is the one print that does not glow
 */
public record HoofprintOptions(int fromColor, int toColor, float yaw, float scale, boolean glow)
        implements ParticleOptions {

    public static final MapCodec<HoofprintOptions> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ExtraCodecs.RGB_COLOR_CODEC.fieldOf("from_color").forGetter(HoofprintOptions::fromColor),
            ExtraCodecs.RGB_COLOR_CODEC.fieldOf("to_color").forGetter(HoofprintOptions::toColor),
            Codec.FLOAT.fieldOf("yaw").forGetter(HoofprintOptions::yaw),
            Codec.FLOAT.fieldOf("scale").forGetter(HoofprintOptions::scale),
            Codec.BOOL.optionalFieldOf("glow", true).forGetter(HoofprintOptions::glow)
    ).apply(i, HoofprintOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, HoofprintOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, HoofprintOptions::fromColor,
            ByteBufCodecs.INT, HoofprintOptions::toColor,
            ByteBufCodecs.FLOAT, HoofprintOptions::yaw,
            ByteBufCodecs.FLOAT, HoofprintOptions::scale,
            ByteBufCodecs.BOOL, HoofprintOptions::glow,
            HoofprintOptions::new);

    @Override
    public ParticleType<HoofprintOptions> getType() {
        return ModParticles.HOOFPRINT.get();
    }
}
