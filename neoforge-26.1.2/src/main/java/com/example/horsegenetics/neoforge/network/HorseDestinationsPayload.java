package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Server -&gt; client: <b>where each nearby horse is trying to walk</b>, for the
 * F8 highlight's destination lines. Pushed only to players who have the
 * highlight on, and an empty list is pushed once when they turn it off so the
 * lines go out immediately rather than fading on the client's staleness timer.
 *
 * <h2>Two points, because the interesting case is when they differ</h2>
 * {@link Entry#tx}/{@code ty}/{@code tz} is the <b>intent</b> - what the goal
 * decided it wants, read off {@link com.example.horsegenetics.neoforge.server.DebugDestination}.
 * {@link Entry#sx}/{@code sy}/{@code sz} is the <b>stop</b> - the last node of
 * the path the horse is actually walking. For a reachable target they are the
 * same place. For a horse pressed against a wall with a crop behind it they are
 * not, and the gap between them is the bug drawn to scale.
 *
 * <p>{@link Entry#state} says which of three cases produced that pair, so the
 * client can colour the line without re-deriving it:
 * {@link #STATE_REACHES}, {@link #STATE_BLOCKED}, {@link #STATE_NO_PATH}.
 *
 * <h2>It is a debug overlay, so it is capped and lossy</h2>
 * Capped at {@link #MAX_ENTRIES} horses; past that the nearest are sent and the
 * rest are dropped silently, because a hard failure in a diagnostic is worse
 * than a short list. Positions are sent as full doubles rather than packed -
 * this rides a 500 ms cadence for one player at a time, so the bytes are not
 * worth a precision bug in the one tool used to find precision bugs.
 */
public record HorseDestinationsPayload(List<Entry> entries) implements CustomPacketPayload {

    /** A packet ceiling for a debug overlay, not a design statement. */
    public static final int MAX_ENTRIES = 128;

    /** Labels are truncated to this on the way out; see {@code DebugDestination}. */
    public static final int MAX_LABEL = 48;

    /** The horse has a path and that path reaches the target. */
    public static final byte STATE_REACHES = 0;

    /** The horse has a path but it stops short - {@code Path.canReach()} is false. */
    public static final byte STATE_BLOCKED = 1;

    /** The horse wants the target and has no path to it at all. */
    public static final byte STATE_NO_PATH = 2;

    /**
     * One horse's destination.
     *
     * @param entityId the <b>network</b> id, not the UUID - the client resolves
     *                 it with {@code ClientLevel.getEntity(int)}, and a horse
     *                 near enough to draw a line for is near enough to be
     *                 tracked, so the id is always resolvable
     * @param hasStop  false when the horse has no current path, in which case
     *                 the stop coordinates are not written at all
     */
    public record Entry(int entityId,
                        double tx, double ty, double tz,
                        boolean hasStop,
                        double sx, double sy, double sz,
                        byte state,
                        String label) {
    }

    public static final Type<HorseDestinationsPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "horse_destinations"));

    /**
     * Hand-written for the same reason {@link HorseRosterPayload}'s is - past
     * {@code StreamCodec.composite}'s arity, and with one conditional field.
     * Straight-line reads and writes in the same order; the one rule is that
     * they stay in the same order, <b>including the conditional</b>, which is
     * only present when {@code hasStop} was true.
     */
    private static final StreamCodec<ByteBuf, Entry> ENTRY_STREAM_CODEC =
            new StreamCodec<ByteBuf, Entry>() {
                @Override
                public Entry decode(ByteBuf buf) {
                    int entityId = ByteBufCodecs.VAR_INT.decode(buf);
                    double tx = ByteBufCodecs.DOUBLE.decode(buf);
                    double ty = ByteBufCodecs.DOUBLE.decode(buf);
                    double tz = ByteBufCodecs.DOUBLE.decode(buf);
                    boolean hasStop = ByteBufCodecs.BOOL.decode(buf);
                    double sx = 0.0;
                    double sy = 0.0;
                    double sz = 0.0;
                    if (hasStop) {
                        sx = ByteBufCodecs.DOUBLE.decode(buf);
                        sy = ByteBufCodecs.DOUBLE.decode(buf);
                        sz = ByteBufCodecs.DOUBLE.decode(buf);
                    }
                    byte state = ByteBufCodecs.BYTE.decode(buf);
                    String label = ByteBufCodecs.stringUtf8(MAX_LABEL).decode(buf);
                    return new Entry(entityId, tx, ty, tz, hasStop, sx, sy, sz, state, label);
                }

                @Override
                public void encode(ByteBuf buf, Entry entry) {
                    ByteBufCodecs.VAR_INT.encode(buf, entry.entityId());
                    ByteBufCodecs.DOUBLE.encode(buf, entry.tx());
                    ByteBufCodecs.DOUBLE.encode(buf, entry.ty());
                    ByteBufCodecs.DOUBLE.encode(buf, entry.tz());
                    ByteBufCodecs.BOOL.encode(buf, entry.hasStop());
                    if (entry.hasStop()) {
                        ByteBufCodecs.DOUBLE.encode(buf, entry.sx());
                        ByteBufCodecs.DOUBLE.encode(buf, entry.sy());
                        ByteBufCodecs.DOUBLE.encode(buf, entry.sz());
                    }
                    ByteBufCodecs.BYTE.encode(buf, entry.state());
                    ByteBufCodecs.stringUtf8(MAX_LABEL).encode(buf, entry.label());
                }
            };

    public static final StreamCodec<ByteBuf, HorseDestinationsPayload> STREAM_CODEC = StreamCodec.composite(
            ENTRY_STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ENTRIES)), HorseDestinationsPayload::entries,
            HorseDestinationsPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
