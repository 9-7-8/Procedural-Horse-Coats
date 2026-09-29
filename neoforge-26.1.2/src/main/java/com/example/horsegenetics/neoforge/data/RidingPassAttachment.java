package com.example.horsegenetics.neoforge.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * <b>Who else may ride this horse, and until when</b> - the state behind a
 * jockey pass and {@code /horsejockey}.
 *
 * <p>{@code server/HorseRiding} answers "may this player ride" from ownership
 * and teams, both of which are <i>standing</i> relationships. This is the
 * temporary one: a named person, a deadline, and nothing else. It exists because
 * a race needs a jockey for an afternoon and neither of the other two answers
 * can say that - putting somebody on your team to lend them a horse for one
 * race gives them every horse you own, for ever.
 *
 * <h2>Why a map of deadlines and not a set</h2>
 * The same call {@link PassificationAttachment} makes, for the same reason: one
 * map with a time in it cannot disagree with itself, whereas a set of names
 * beside a map of expiries eventually will. Reading it is one comparison.
 *
 * <p>Deadlines are <b>game time</b> ({@code ServerLevel.getGameTime}), which is
 * the clock a Minecraft day is a unit of and the one every other duration in
 * this mod uses. It does not advance while the server is down, so a pass bought
 * for a race is not spent overnight while nobody is playing.
 *
 * <h2>Extending, not replacing</h2>
 * {@link #grant} adds to whatever is left rather than overwriting it - "one
 * Minecraft day per item" (owner, 2026-09-29), so three passes fed to one horse
 * are three days. Adding from <i>now</i> when the current pass has already
 * lapsed, and from the existing deadline when it has not, is what stops a second
 * pass being worth less than the first.
 *
 * <p>Keys are UUID strings, matching every other map-bearing attachment here.
 * {@code copyOnDeath}, so a horse brought back by {@code /horseresurrect} does
 * not quietly strand the jockey who was riding it.
 */
public record RidingPassAttachment(Map<String, Long> until) {

    public static final RidingPassAttachment DEFAULT = new RidingPassAttachment(Map.of());

    public RidingPassAttachment {
        until = Map.copyOf(until);
    }

    public static final MapCodec<RidingPassAttachment> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Codec.LONG)
                    .optionalFieldOf("until", Map.of())
                    .forGetter(RidingPassAttachment::until)
    ).apply(i, RidingPassAttachment::new));

    public static final Codec<RidingPassAttachment> CODEC = MAP_CODEC.codec();

    /** Does this player hold a pass that has not run out? */
    public boolean allows(UUID player, long now) {
        Long end = until.get(player.toString());
        return end != null && now < end;
    }

    /** Ticks left on this player's pass; {@code 0} when there is none or it has lapsed. */
    public long remaining(UUID player, long now) {
        Long end = until.get(player.toString());
        return end == null || now >= end ? 0L : end - now;
    }

    /**
     * <b>Add {@code ticks} to this player's pass.</b> From the existing deadline
     * when one is still running, and from {@code now} when it is not - so a
     * second pass fed during the first is never worth less than the first was.
     */
    public RidingPassAttachment grant(UUID player, long now, long ticks) {
        Map<String, Long> next = new HashMap<>(until);
        long from = Math.max(now, next.getOrDefault(player.toString(), 0L));
        next.put(player.toString(), from + ticks);
        return new RidingPassAttachment(prune(next, now));
    }

    /** Take this player's pass away, whatever was left on it. */
    public RidingPassAttachment revoke(UUID player) {
        if (!until.containsKey(player.toString())) {
            return this;
        }
        Map<String, Long> next = new HashMap<>(until);
        next.remove(player.toString());
        return new RidingPassAttachment(next);
    }

    /**
     * <b>Every pass gone.</b> Used when the horse changes hands: a pass is
     * permission the <i>previous</i> owner gave, and carrying it across a sale
     * would hand the new owner's horse to strangers they never agreed to. See
     * {@code server/HorseGiveCommand} and {@code server/TransferPaperHandler}.
     */
    public RidingPassAttachment cleared() {
        return until.isEmpty() ? this : DEFAULT;
    }

    /** Is there nothing left to store? Lets a caller drop the attachment entirely. */
    public boolean isEmpty() {
        return until.isEmpty();
    }

    /**
     * The passes that are still running, newest deadline last. For the command's
     * listing and for nothing else - riding asks {@link #allows}.
     */
    public Map<String, Long> live(long now) {
        return Map.copyOf(prune(new HashMap<>(until), now));
    }

    /**
     * Drop what has run out. Called on every write rather than on a tick: the
     * map is only ever touched when somebody grants a pass, and a horse whose
     * jockeys have all lapsed should not carry their names in the save for ever.
     */
    private static Map<String, Long> prune(Map<String, Long> map, long now) {
        map.values().removeIf(end -> end <= now);
        return map;
    }
}
