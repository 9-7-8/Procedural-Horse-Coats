package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.common.log.HorseEvent;
import com.example.horsegenetics.common.log.HorseEventLog;
import com.example.horsegenetics.common.repro.CoverNotice;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;
import java.util.UUID;

/**
 * Serialization for {@link HorseEvent} - the disk codec the world save uses and
 * the stream codec the Log tab's packet uses.
 *
 * <p>Here rather than on the record itself for the usual reason: {@link HorseEvent}
 * lives in {@code common/}, which imports nothing from Mojang or Minecraft, and
 * this is all three.
 *
 * <h2>Enums go to disk by name</h2>
 * Not by ordinal. An ordinal is smaller and is wrong the first time somebody
 * inserts a kind into the middle of the enum, which silently reinterprets every
 * saved row - a birth becomes a death and nothing complains. Names cost a few
 * bytes in a file that holds at most a couple of hundred rows per player.
 *
 * <p>An unrecognised name is a hard error rather than a quiet default. There is
 * no back-compat path in this repo, so the only way to read one is a genuine
 * corruption, and a log that turns an unreadable row into a plausible one is
 * worse than a log that says it could not read it.
 */
public final class HorseEventCodecs {

    private HorseEventCodecs() {
    }

    private static final Codec<HorseEvent.Kind> KIND = Codec.STRING.comapFlatMap(
            name -> {
                for (HorseEvent.Kind kind : HorseEvent.Kind.values()) {
                    if (kind.name().equals(name)) {
                        return DataResult.success(kind);
                    }
                }
                return DataResult.error(() -> "unknown horse event kind: " + name);
            },
            HorseEvent.Kind::name);

    private static final Codec<CoverNotice.Reason> REASON = Codec.STRING.comapFlatMap(
            name -> {
                for (CoverNotice.Reason reason : CoverNotice.Reason.values()) {
                    if (reason.name().equals(name)) {
                        return DataResult.success(reason);
                    }
                }
                return DataResult.error(() -> "unknown cover reason: " + name);
            },
            CoverNotice.Reason::name);

    public static final Codec<HorseEvent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            KIND.fieldOf("kind").forGetter(HorseEvent::kind),
            Codec.LONG.fieldOf("at").forGetter(HorseEvent::at),
            UUIDUtil.CODEC.fieldOf("horse").forGetter(HorseEvent::horseId),
            Codec.STRING.optionalFieldOf("name", "").forGetter(HorseEvent::horseName),
            Codec.STRING.optionalFieldOf("other", "").forGetter(HorseEvent::other),
            REASON.optionalFieldOf("reason").forGetter(e -> Optional.ofNullable(e.reason())),
            Codec.INT.optionalFieldOf("nearby", 0).forGetter(HorseEvent::nearby),
            Codec.INT.optionalFieldOf("cap", 0).forGetter(HorseEvent::cap)
    ).apply(instance, (kind, at, horse, name, other, reason, nearby, cap) ->
            new HorseEvent(kind, at, horse, name, other, reason.orElse(null), nearby, cap)));

    /** One row with the player it belongs to - what the save file is a list of. */
    public static final Codec<HorseEventLog.Owned> OWNED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(HorseEventLog.Owned::owner),
            CODEC.fieldOf("event").forGetter(HorseEventLog.Owned::event)
    ).apply(instance, HorseEventLog.Owned::new));

    /**
     * Wire form. Straight-line reads and writes in the same order - the one rule
     * is that they stay in the same order, as {@code HorseRosterPayload} says.
     *
     * <p>The reason is sent as an ordinal-plus-one so that zero can mean absent,
     * which is what it is on six of the seven kinds. On the wire, unlike on
     * disk, an ordinal is safe: both ends are the same build.
     */
    public static final StreamCodec<ByteBuf, HorseEvent> STREAM_CODEC = new StreamCodec<ByteBuf, HorseEvent>() {
        @Override
        public HorseEvent decode(ByteBuf buf) {
            HorseEvent.Kind kind = HorseEvent.Kind.values()[ByteBufCodecs.VAR_INT.decode(buf)];
            long at = ByteBufCodecs.VAR_LONG.decode(buf);
            UUID horse = UUIDUtil.STREAM_CODEC.decode(buf);
            String name = ByteBufCodecs.stringUtf8(64).decode(buf);
            String other = ByteBufCodecs.stringUtf8(96).decode(buf);
            int reason = ByteBufCodecs.VAR_INT.decode(buf);
            int nearby = ByteBufCodecs.VAR_INT.decode(buf);
            int cap = ByteBufCodecs.VAR_INT.decode(buf);
            return new HorseEvent(kind, at, horse, name, other,
                    reason == 0 ? null : CoverNotice.Reason.values()[reason - 1], nearby, cap);
        }

        @Override
        public void encode(ByteBuf buf, HorseEvent event) {
            ByteBufCodecs.VAR_INT.encode(buf, event.kind().ordinal());
            ByteBufCodecs.VAR_LONG.encode(buf, event.at());
            UUIDUtil.STREAM_CODEC.encode(buf, event.horseId());
            ByteBufCodecs.stringUtf8(64).encode(buf, event.horseName());
            ByteBufCodecs.stringUtf8(96).encode(buf, event.other());
            ByteBufCodecs.VAR_INT.encode(buf, event.reason() == null ? 0 : event.reason().ordinal() + 1);
            ByteBufCodecs.VAR_INT.encode(buf, event.nearby());
            ByteBufCodecs.VAR_INT.encode(buf, event.cap());
        }
    };
}
