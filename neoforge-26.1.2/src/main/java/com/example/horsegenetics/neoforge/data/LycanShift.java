package com.example.horsegenetics.neoforge.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;

/**
 * <b>The horse a shifted animal used to be, and will be again at dawn.</b>
 *
 * <p>The LYCAN locus does not decorate a horse - it <i>replaces</i> it. At dusk
 * the {@code Horse} entity is discarded and a real wolf (or cat, or chicken)
 * takes its place, so that everything the game lets you do to a wolf works
 * without a single special case: it breeds with wolves, it can be tamed like a
 * wolf, it is sheared like a sheep, it is bucketed like a fish. The price of
 * that is that the horse has to be kept <b>somewhere</b> in the meantime, and
 * this is where.
 *
 * <p>{@link #horse} is the whole entity: the complete
 * {@code Entity.saveWithoutId} tag, which on NeoForge carries the mod's own data
 * attachments as well - so the record, the pedigree, the bond, the cooldowns and
 * the cowboy's brand all cross the night untouched, along with the horse's
 * {@code UUID}. That is what makes the animal the <i>same horse</i> rather than
 * a lookalike: at dawn the tag is loaded back into a fresh {@code Horse} and it
 * keeps its identity down to the entity id every other system keys on.
 *
 * <p>Storing an entity inside another entity's attachment is unusual and worth
 * being explicit about. The alternative was to keep the horse alive somewhere
 * off-map and teleport it back, which needs a holding dimension, a reaper for
 * the ones whose animal died, and a rule for what happens when the chunk
 * unloads. A tag that travels with the animal has none of those problems: unload
 * the chunk and both go to disk together, kill the animal and the horse is gone
 * with it, which is exactly what should happen.
 *
 * <p>{@link #cloudColor} is the {@code 0xRRGGBB} the animal trails as it walks,
 * copied off the expressing allele copy at dusk so the tick loop does not have
 * to re-parse a genome it no longer has an easy handle on.
 * {@link #mob} is the id it shifted into - carried for diagnostics and for the
 * "is this thing one of ours" check to read without touching the tag.
 *
 * <p><b>Not</b> {@code copyOnDeath}: an animal that dies stays dead, and the
 * horse inside it dies with it. See {@code wiki/gene-lycan.html}.
 */
public record LycanShift(String mob, int cloudColor, CompoundTag horse) {

    /** The "this is an ordinary animal" sentinel - a blank mob id. */
    public static final LycanShift NONE = new LycanShift("", 0xFFFFFF, new CompoundTag());

    public static final MapCodec<LycanShift> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("mob", "").forGetter(LycanShift::mob),
            Codec.INT.optionalFieldOf("cloud_color", 0xFFFFFF).forGetter(LycanShift::cloudColor),
            CompoundTag.CODEC.optionalFieldOf("horse", new CompoundTag()).forGetter(LycanShift::horse)
    ).apply(i, LycanShift::new));

    public static final Codec<LycanShift> CODEC = MAP_CODEC.codec();

    /** Is this entity a horse wearing an animal, rather than an animal? */
    public boolean active() {
        return !mob.isEmpty() && !horse.isEmpty();
    }
}
