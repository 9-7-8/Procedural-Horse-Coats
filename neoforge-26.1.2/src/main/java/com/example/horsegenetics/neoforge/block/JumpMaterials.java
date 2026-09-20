package com.example.horsegenetics.neoforge.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * <b>The two woods a jump is made of</b> - the rails you jump, and the
 * standards holding them up.
 *
 * <p>Two woods, not one, is the whole reason jumps stopped being twelve blocks
 * and became one block with data. The owner's ask was that "the supporting
 * poles and the jumping poles must be able to be made of different woods",
 * which as blockstate properties would have been 12 x 12 x every other
 * property - <b>13,824 block states</b>, all of them allocated at registry
 * bootstrap on the server as well as the client. See
 * {@code wiki/item-jumps.html} for the arithmetic.
 *
 * <h2>These are wood KEYS, not texture ids</h2>
 * {@code "oak"}, {@code "biomesoplenty:fir"} - the same strings
 * {@link Jumps} registers under. The texture is looked up from the key at bake
 * time, because a texture id is a client-side fact and this record is saved to
 * disk and sent over the wire. A key that no longer resolves - a wood whose mod
 * was removed - falls back rather than crashing; see {@code JumpModel}.
 *
 * @param rails     the wood of the poles a horse jumps
 * @param standards the wood of the uprights at the ends of a run
 */
public record JumpMaterials(String rails, String standards) {

    /** What a jump is made of when nothing says otherwise. */
    public static final String DEFAULT_WOOD = "oak";

    public static final JumpMaterials DEFAULT = new JumpMaterials(DEFAULT_WOOD, DEFAULT_WOOD);

    public static final Codec<JumpMaterials> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("rails").forGetter(JumpMaterials::rails),
            Codec.STRING.fieldOf("standards").forGetter(JumpMaterials::standards)
    ).apply(i, JumpMaterials::new));

    /**
     * For the block entity's update packet and for the item component.
     *
     * <p>Plain strings rather than registry ids on purpose: a wood key is not a
     * registry entry of ours, and half of them belong to other mods that may
     * not be installed on both ends.
     */
    public static final StreamCodec<io.netty.buffer.ByteBuf, JumpMaterials> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, JumpMaterials::rails,
                    ByteBufCodecs.STRING_UTF8, JumpMaterials::standards,
                    JumpMaterials::new);

    /** The same jump with different rails. */
    public JumpMaterials withRails(String wood) {
        return new JumpMaterials(wood, this.standards);
    }

    /** The same jump with different standards. */
    public JumpMaterials withStandards(String wood) {
        return new JumpMaterials(this.rails, wood);
    }

    /** True when both parts are the same wood, which is what a fresh craft gives. */
    public boolean uniform() {
        return this.rails.equals(this.standards);
    }
}
