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
public record JumpMaterials(String rails, String standards, int railsDye, int standardsDye,
                            int size) {

    /**
     * <b>Every height a jump can be</b>, as a multiple of a block, in the order
     * the screen steps through them.
     *
     * <p>Ten rungs rather than even tenths, and rather than five presets: fine
     * where it matters and coarse where it does not. <b>Below 1.0 the steps are
     * small</b>, because that is where a jump stops being an obstacle and
     * becomes a ground pole - the interesting part of the range - and above it
     * they widen, because nobody needs to tell 1.6 from 1.7 blocks apart.
     *
     * <p>A jump is <b>{@code size + 0.5} blocks to clear</b>: the drawn height
     * scales, and the half-block of invisible fence on top does not. That falls
     * out rather than being designed, and it is what makes the bottom of this
     * ladder work - 0.1 clears at 0.6, well under a horse's 1.0 step height, so
     * a ground pole is walked over without needing a rule of its own.
     *
     * <p>Every entry costs a scaled bake of every part, which is the only
     * reason this is a ladder and not a slider.
     */
    public static final float[] SIZES =
            {0.1F, 0.2F, 0.3F, 0.5F, 0.75F, 1.0F, 1.25F, 1.5F, 1.75F, 2.0F};

    /** One block, which is what a jump has always been and what a craft gives. */
    public static final int DEFAULT_SIZE = 5;

    /** Clamp anything arriving from disk or the wire into the ladder. */
    public static int clampSize(int size) {
        return Math.max(0, Math.min(SIZES.length - 1, size));
    }

    /** This jump's height, as a multiple of a block. */
    public float scale() {
        return SIZES[clampSize(this.size)];
    }

    /** What a jump is made of when nothing says otherwise. */
    public static final String DEFAULT_WOOD = "oak";

    /**
     * <b>Not painted.</b> A real colour, never this, means the part is dyed.
     *
     * <p>-1 rather than 0, because 0 is black and black is a dye somebody will
     * want. Every field that holds one of these is an int and not an
     * {@code Optional<Integer>}: it is read on chunk-meshing worker threads
     * once per part per block, and it is saved, synced and put on an item.
     */
    public static final int UNDYED = -1;

    public static final JumpMaterials DEFAULT =
            new JumpMaterials(DEFAULT_WOOD, DEFAULT_WOOD, UNDYED, UNDYED, DEFAULT_SIZE);

    public static final Codec<JumpMaterials> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("rails").forGetter(JumpMaterials::rails),
            Codec.STRING.fieldOf("standards").forGetter(JumpMaterials::standards),
            // Optional, defaulting to undyed: the overwhelming majority of
            // jumps are never painted, and an int written into every one of
            // hundreds of block entities in a course is worth not writing.
            Codec.INT.optionalFieldOf("rails_dye", UNDYED).forGetter(JumpMaterials::railsDye),
            Codec.INT.optionalFieldOf("standards_dye", UNDYED)
                    .forGetter(JumpMaterials::standardsDye),
            Codec.INT.optionalFieldOf("size", DEFAULT_SIZE).forGetter(JumpMaterials::size)
    ).apply(i, JumpMaterials::new));

    /** A jump of one wood throughout, unpainted - what a craft gives. */
    public static JumpMaterials of(String wood) {
        return new JumpMaterials(wood, wood, UNDYED, UNDYED, DEFAULT_SIZE);
    }

    /** The colour a part is drawn in, as a multiplier, or white if it is bare. */
    public int tint(boolean rails) {
        int dye = rails ? this.railsDye : this.standardsDye;
        return dye == UNDYED ? 0xFFFFFF : dye;
    }

    /** True if either half has been painted. */
    public boolean painted() {
        return this.railsDye != UNDYED || this.standardsDye != UNDYED;
    }

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
                    ByteBufCodecs.VAR_INT, JumpMaterials::railsDye,
                    ByteBufCodecs.VAR_INT, JumpMaterials::standardsDye,
                    ByteBufCodecs.VAR_INT, JumpMaterials::size,
                    JumpMaterials::new);

    /**
     * <b>New rails, stripped back to bare wood.</b>
     *
     * <p>Re-wooding a half <i>clears its paint</i>, and that is the only way to
     * clear it - a dye is spent for good and never comes back, so the undo is a
     * fresh plank, which pops the old one out. Owner's design: "the only way to
     * undo painting is to put in a new piece of wood, which pops out the
     * original piece again (not super realistic, but that's okay)". It is not
     * realistic and it is <i>legible</i>, which is the trade: one slot per half
     * does both jobs and the rule is one sentence.
     */
    public JumpMaterials withRails(String wood) {
        return new JumpMaterials(wood, this.standards, UNDYED, this.standardsDye, this.size);
    }

    /** New standards, stripped back to bare wood. See {@link #withRails}. */
    public JumpMaterials withStandards(String wood) {
        return new JumpMaterials(this.rails, wood, this.railsDye, UNDYED, this.size);
    }

    /** The same jump with its rails painted. */
    public JumpMaterials withRailsDye(int colour) {
        return new JumpMaterials(this.rails, this.standards, colour, this.standardsDye, this.size);
    }

    /** The same jump with its standards painted. */
    public JumpMaterials withStandardsDye(int colour) {
        return new JumpMaterials(this.rails, this.standards, this.railsDye, colour, this.size);
    }

    /** The same jump at a different height. */
    public JumpMaterials withSize(int index) {
        return new JumpMaterials(this.rails, this.standards, this.railsDye, this.standardsDye,
                clampSize(index));
    }

    /**
     * True when both halves look the same - same wood <i>and</i> same paint.
     *
     * <p>Paint counts, because it is what you see: an oak jump with red rails
     * and bare oak standards is two-toned, and naming it "Oak Horse Jump" as
     * though it were plain would be a lie told by the item.
     */
    public boolean uniform() {
        return this.rails.equals(this.standards) && this.railsDye == this.standardsDye;
    }

    // --- the item form ------------------------------------------------------
    //
    // On an ITEM the two woods are two separate string components rather than
    // one of these, because a client item definition selects a model on the
    // whole value of one component - see ModDataComponents.JUMP_RAILS. These
    // three methods are the only places that know that, so the rest of the mod
    // goes on passing a JumpMaterials around.

    /**
     * What an item says it is made of, defaulting either half that is missing.
     *
     * <p>Takes a {@link DataComponentGetter} rather than an {@code ItemStack}
     * because a block entity's implicit-component hook is handed one of those
     * and not a stack.
     */
    public static JumpMaterials fromComponents(
            net.minecraft.core.component.DataComponentGetter components) {
        return new JumpMaterials(
                components.getOrDefault(
                        com.example.horsegenetics.neoforge.data.ModDataComponents.JUMP_RAILS.get(),
                        DEFAULT_WOOD),
                components.getOrDefault(
                        com.example.horsegenetics.neoforge.data.ModDataComponents.JUMP_STANDARDS.get(),
                        DEFAULT_WOOD),
                components.getOrDefault(
                        com.example.horsegenetics.neoforge.data.ModDataComponents.JUMP_RAILS_DYE.get(),
                        UNDYED),
                components.getOrDefault(
                        com.example.horsegenetics.neoforge.data.ModDataComponents.JUMP_STANDARDS_DYE.get(),
                        UNDYED),
                clampSize(components.getOrDefault(
                        com.example.horsegenetics.neoforge.data.ModDataComponents.JUMP_SIZE.get(),
                        DEFAULT_SIZE)));
    }

    /**
     * Stamp this onto a stack. Both woods always - oak included, see the
     * component's note - and <b>each dye only if there is one</b>.
     *
     * <p>Painted is the rare case, so an undyed jump carries no dye component
     * at all rather than one saying "undyed". Two stacks that differ by a
     * component nobody set would not merge, and a chest of plain jumps that
     * will not stack is the bug this asymmetry avoids.
     */
    public void writeTo(net.minecraft.world.item.ItemStack stack) {
        stack.set(com.example.horsegenetics.neoforge.data.ModDataComponents.JUMP_RAILS.get(), this.rails);
        stack.set(com.example.horsegenetics.neoforge.data.ModDataComponents.JUMP_STANDARDS.get(), this.standards);
        setOrClear(stack, com.example.horsegenetics.neoforge.data.ModDataComponents.JUMP_RAILS_DYE.get(), this.railsDye);
        setOrClear(stack, com.example.horsegenetics.neoforge.data.ModDataComponents.JUMP_STANDARDS_DYE.get(), this.standardsDye);
        // Size is written only when it is NOT the ordinary one block, for the
        // same reason the dyes are: an item saying "size 5" and an item saying
        // nothing are the same jump, and two stacks of the same jump that will
        // not merge get reported as an inventory bug.
        if (this.size == DEFAULT_SIZE) {
            stack.remove(com.example.horsegenetics.neoforge.data.ModDataComponents.JUMP_SIZE.get());
        } else {
            stack.set(com.example.horsegenetics.neoforge.data.ModDataComponents.JUMP_SIZE.get(), this.size);
        }
    }

    private static void setOrClear(net.minecraft.world.item.ItemStack stack,
                                   net.minecraft.core.component.DataComponentType<Integer> type,
                                   int colour) {
        if (colour == UNDYED) {
            stack.remove(type);
        } else {
            stack.set(type, colour);
        }
    }

    /** The same, into the map a block entity collects its implicit components into. */
    public void writeTo(net.minecraft.core.component.DataComponentMap.Builder builder) {
        builder.set(com.example.horsegenetics.neoforge.data.ModDataComponents.JUMP_RAILS.get(), this.rails);
        builder.set(com.example.horsegenetics.neoforge.data.ModDataComponents.JUMP_STANDARDS.get(), this.standards);
        if (this.railsDye != UNDYED) {
            builder.set(com.example.horsegenetics.neoforge.data.ModDataComponents.JUMP_RAILS_DYE.get(), this.railsDye);
        }
        if (this.standardsDye != UNDYED) {
            builder.set(com.example.horsegenetics.neoforge.data.ModDataComponents.JUMP_STANDARDS_DYE.get(), this.standardsDye);
        }
        if (this.size != DEFAULT_SIZE) {
            builder.set(com.example.horsegenetics.neoforge.data.ModDataComponents.JUMP_SIZE.get(), this.size);
        }
    }
}
