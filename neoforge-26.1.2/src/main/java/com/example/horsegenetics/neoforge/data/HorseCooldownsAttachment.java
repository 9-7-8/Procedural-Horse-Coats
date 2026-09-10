package com.example.horsegenetics.neoforge.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-horse <b>timed-interaction stamps</b> - the game time of the last
 * shearing, the last per-gene {@code yield}, and anything else that is gated
 * "once per Minecraft day" (24&nbsp;000 ticks, the mod's standard time gate,
 * roadmap &sect;21).
 *
 * <p>Keys are short strings the caller owns: {@code "shear"} for
 * {@link com.example.horsegenetics.neoforge.server.HorseShearHandler}, and
 * {@code "yield:<geneKey>"} for
 * {@link com.example.horsegenetics.neoforge.server.GeneYieldHandler} - which is
 * why this exists at all: the old yield cooldown lived in a {@code static Map}
 * in the translator, so it did not survive a restart and no one could inspect
 * it (roadmap &sect;7).
 *
 * <p>{@code copyOnDeath} so a re-summoned horse keeps its cooldowns. Dev only:
 * no legacy handling.
 */
public record HorseCooldownsAttachment(Map<String, Long> lastByKey) {

    public static final long DAY_TICKS = 24_000L;

    public static final HorseCooldownsAttachment DEFAULT =
            new HorseCooldownsAttachment(Map.of());

    public HorseCooldownsAttachment {
        lastByKey = Map.copyOf(lastByKey);
    }

    public static final MapCodec<HorseCooldownsAttachment> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Codec.LONG)
                    .optionalFieldOf("last_by_key", Map.of())
                    .forGetter(HorseCooldownsAttachment::lastByKey)
    ).apply(i, HorseCooldownsAttachment::new));

    public static final Codec<HorseCooldownsAttachment> CODEC = MAP_CODEC.codec();

    /**
     * The game time this key last fired, or {@code Long.MIN_VALUE} if never.
     *
     * <p><b>Do not subtract this from anything.</b> {@code now - Long.MIN_VALUE}
     * overflows to a large <i>negative</i> number, so an elapsed-time test
     * written the obvious way says "not ready" for a key that has never fired -
     * which is the wrong answer for every gate here, and it is the whole of what
     * {@link #ready} exists to get right. It is for display and debugging.
     */
    public long last(String key) {
        return lastByKey.getOrDefault(key, Long.MIN_VALUE);
    }

    /** True if {@code key} has not fired within the last {@link #DAY_TICKS}. */
    public boolean ready(String key, long now) {
        return ready(key, now, DAY_TICKS);
    }

    /**
     * True if {@code key} has not fired within {@code cooldownTicks} - and
     * always true if it has never fired at all, which is the case the absent
     * check is here for. See {@link #last}.
     */
    public boolean ready(String key, long now, long cooldownTicks) {
        Long fired = lastByKey.get(key);
        return fired == null || now - fired >= cooldownTicks;
    }

    public HorseCooldownsAttachment stamp(String key, long now) {
        Map<String, Long> next = new HashMap<>(lastByKey);
        next.put(key, now);
        return new HorseCooldownsAttachment(next);
    }
}
