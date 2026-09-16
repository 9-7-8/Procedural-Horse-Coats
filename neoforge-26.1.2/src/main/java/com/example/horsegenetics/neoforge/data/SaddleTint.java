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
public record SaddleTint(int seat, int bridle, int metal)
        implements net.minecraft.world.item.component.TooltipProvider {

    /**
     * What the saddle says when you hover it. Vanilla's own
     * {@code DyedItemColor} is the precedent for a component describing itself
     * this way, and it is the only place a player can read three colours off an
     * item that can only <i>show</i> one.
     */
    @Override
    public void addToTooltip(net.minecraft.world.item.Item.TooltipContext context,
                             java.util.function.Consumer<net.minecraft.network.chat.Component> lines,
                             net.minecraft.world.item.TooltipFlag flag,
                             net.minecraft.core.component.DataComponentGetter components) {
        lines.accept(line("Seat", seat));
        lines.accept(line("Bridle", bridle));
        lines.accept(line("Fittings", metal));
    }

    private static net.minecraft.network.chat.Component line(String zone, int colour) {
        return net.minecraft.network.chat.Component
                .literal(zone + ": " + name(colour))
                .withStyle(net.minecraft.ChatFormatting.GRAY);
    }

    /**
     * A dye's name where the colour is one, and a hex code where it is not.
     * &ldquo;Seat: red&rdquo; is worth far more than &ldquo;Seat: #B02E26&rdquo;
     * to somebody deciding what to dye next, and the metals are not dyes at all.
     */
    private static String name(int rgb) {
        for (net.minecraft.world.item.DyeColor dye : net.minecraft.world.item.DyeColor.values()) {
            if ((dye.getTextureDiffuseColor() & 0xFFFFFF) == (rgb & 0xFFFFFF)) {
                return dye.getSerializedName().replace('_', ' ');
            }
        }
        for (java.util.Map.Entry<String, Integer> metal : METAL_NAMES.entrySet()) {
            if (metal.getValue() == (rgb & 0xFFFFFF)) {
                return metal.getKey();
            }
        }
        return String.format(java.util.Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }

    /** Kept beside the colours it names so the two cannot drift apart silently. */
    private static final java.util.Map<String, Integer> METAL_NAMES = java.util.Map.of(
            "iron", 0x717171,
            "gold", 0xE0B94A,
            "copper", 0xC06A44,
            "netherite", 0x4A4248,
            "diamond", 0xB8E8E4,
            "emerald", 0x3FBF6F,
            "amethyst", 0xA079D8);

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
