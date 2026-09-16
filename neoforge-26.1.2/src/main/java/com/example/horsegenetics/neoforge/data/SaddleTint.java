package com.example.horsegenetics.neoforge.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

/**
 * The three colours of a piece of tack: the <b>seat</b> leather, the
 * <b>bridle</b> leather, and the <b>metal</b> hardware.
 *
 * <p><b>Why this is not {@code dyed_color}.</b> Vanilla holds one dye value per
 * stack and reads it <i>once</i>, outside the layer loop, so however many layers
 * an equipment asset declares they all take the same colour. Three zones need
 * three values, so they live here and reach the renderer through
 * {@code IClientItemExtensions.getArmorLayerTintColor}, which is handed the
 * layer index precisely for this case.
 *
 * <p><b>The indices are the layer order in the equipment asset</b>, and the two
 * must not drift: {@code assets/minecraft/equipment/saddle.json} lists seat,
 * then bridle, then metal, and {@link #forLayer(int)} answers in that order. The
 * textures are disjoint - the bake asserts that no texel appears in two of them
 * - so a wrong order would not overlap anything, it would just paint the bridle
 * with the seat's colour, which is the kind of bug that looks like a design
 * choice.
 *
 * <p>Absent means undyed. The item carries no component until somebody dyes it,
 * and the renderer then falls back to each layer's own
 * {@code color_when_undyed}, which is what keeps a plain saddle - on a horse, a
 * donkey, a mule, a skeleton or a zombie horse - pixel-identical to vanilla's.
 */
public record SaddleTint(int seat, int bridle, int metal) {

    /** Layer order in the equipment asset. Changing these means changing that file too. */
    public static final int LAYER_SEAT = 0;
    public static final int LAYER_BRIDLE = 1;
    public static final int LAYER_METAL = 2;

    public static final Codec<SaddleTint> CODEC = RecordCodecBuilder.create(i -> i.group(
            ExtraCodecs.RGB_COLOR_CODEC.fieldOf("seat").forGetter(SaddleTint::seat),
            ExtraCodecs.RGB_COLOR_CODEC.fieldOf("bridle").forGetter(SaddleTint::bridle),
            ExtraCodecs.RGB_COLOR_CODEC.fieldOf("metal").forGetter(SaddleTint::metal)
    ).apply(i, SaddleTint::new));

    public static final StreamCodec<ByteBuf, SaddleTint> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SaddleTint::seat,
            ByteBufCodecs.INT, SaddleTint::bridle,
            ByteBufCodecs.INT, SaddleTint::metal,
            SaddleTint::new);

    /**
     * The colour for one equipment layer, or {@code -1} for a layer this record
     * does not speak for.
     *
     * <p>{@code -1} rather than {@code 0} on purpose: returning zero from
     * {@code getArmorLayerTintColor} tells the renderer to <b>skip the layer
     * entirely</b>, so a fourth layer added to the asset later would silently
     * vanish instead of rendering untinted.
     */
    public int forLayer(int layerIndex) {
        return switch (layerIndex) {
            case LAYER_SEAT -> seat;
            case LAYER_BRIDLE -> bridle;
            case LAYER_METAL -> metal;
            default -> -1;
        };
    }
}
