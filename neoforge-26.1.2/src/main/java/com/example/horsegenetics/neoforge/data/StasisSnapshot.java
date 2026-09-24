package com.example.horsegenetics.neoforge.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * <b>A whole horse, held in an item.</b> The payload of a stasis chamber: what
 * the horse was at the moment it was captured, kept as data so it costs the
 * server nothing until somebody lets it out again.
 *
 * <h2>The tag is the entity, and that is deliberate</h2>
 * {@link #horse} is the horse's complete {@code Entity.saveWithoutId} tag, which
 * on NeoForge carries <b>the data attachments too</b> - so the record, the
 * pedigree, the bond, the gear, a pregnancy in progress, the cooldowns, the
 * armed carrots and the cowboy's brand all cross into stasis untouched, along
 * with the horse's own {@code UUID}. That last part is what makes a released
 * horse the <i>same horse</i> rather than a convincing copy: the ancestry
 * database, every pedigree, every stall sign and every transfer deed keys on
 * that id.
 *
 * <p><b>This is the same mechanism {@link LycanShift} already uses</b> to carry
 * a horse through a night spent as a wolf, and it is here for the same reason it
 * is there. The alternative - and the one {@code wiki/horse-stasis.html}
 * originally sketched - was a record enumerating each attachment by hand. That
 * shape loses the UUID, loses vanilla state nobody listed (age, custom name,
 * leash, the saddle in the vanilla slot), and rots silently the day somebody
 * adds a fifteenth attachment and does not think of this file. A tag needs no
 * maintenance and cannot be half-written.
 *
 * <p>The cost of the tag is that a snapshot <b>cannot live in {@code common/}</b>
 * the way the wiki page wanted, because {@code CompoundTag} is a Minecraft type
 * and {@code common/} imports none. {@link com.example.horsegenetics.common.horse.StasisTier}
 * is the piece that did go there.
 *
 * <h2>Name and id ride alongside, unpacked</h2>
 * {@link #horseName} and {@link #horseId} are copied out at capture time purely
 * so a tooltip, and later the bank's Browse tab, can say which horse is in which
 * chamber without decoding a whole entity tag per rendered slot. They are a
 * cache of what is already inside {@link #horse}, never the authority.
 *
 * <p><b>The stream codec sends the whole tag</b>, lossily-syncing only the two
 * cheap fields would be tempting and is wrong: a creative-mode pick-block sends
 * the client's copy of a stack back to the server, so anything the client was
 * not told is anything a creative player can quietly erase. Bandwidth is
 * therefore an open question for the bank, not for the item - see this page's
 * Verification tab.
 */
public record StasisSnapshot(String horseName, UUID horseId, CompoundTag horse) {

    public static final Codec<StasisSnapshot> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("name", "").forGetter(StasisSnapshot::horseName),
            UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(StasisSnapshot::horseId),
            CompoundTag.CODEC.fieldOf("horse").forGetter(StasisSnapshot::horse)
    ).apply(i, StasisSnapshot::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, StasisSnapshot> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, StasisSnapshot::horseName,
                    UUIDUtil.STREAM_CODEC, StasisSnapshot::horseId,
                    ByteBufCodecs.COMPOUND_TAG, StasisSnapshot::horse,
                    StasisSnapshot::new);
}
