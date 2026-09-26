package com.example.horsegenetics.neoforge.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * <b>Who this horse has stopped wanting to kill, and until when</b> - the state
 * behind {@code horsegenetics.passification}.
 *
 * <p>The gene says what a horse will accept; this says what it has <i>been</i>
 * given. It is per-player on purpose: passifying a horse is a bargain between
 * that animal and the person who fed it, and a horse calmed by one player is
 * still perfectly willing to kill their friend.
 *
 * <h2>Permanent is just a very distant deadline</h2>
 * {@link #until} holds the game time each player's calm expires, and a permanent
 * route stores {@link #FOREVER}. Folding the two into one map rather than
 * keeping a set beside it means {@link #calm} is one comparison and there is no
 * way for the two to disagree - which the two-field version would eventually
 * have found a way to do.
 *
 * <p>{@link #progress} counts part-paid offerings, keyed by player <i>and</i>
 * item, so a horse that wants four apples remembers the three it has had - and a
 * compound heterozygote being paid down two different routes at once keeps the
 * two counts apart.
 *
 * <p>Keys are UUID strings rather than a UUID codec: the maps serialise with the
 * plain string codec the rest of the attachments here use, and nothing needs to
 * sort or arithmetic them. {@code copyOnDeath}, so a re-summoned horse still
 * remembers the bargain.
 */
public record PassificationAttachment(Map<String, Long> until,
                                      Map<String, Long> lastCalm,
                                      Map<String, Integer> progress) {

    /** The deadline a permanent route writes. */
    public static final long FOREVER = Long.MAX_VALUE;

    public static final PassificationAttachment DEFAULT =
            new PassificationAttachment(Map.of(), Map.of(), Map.of());

    public PassificationAttachment {
        until = Map.copyOf(until);
        lastCalm = Map.copyOf(lastCalm);
        progress = Map.copyOf(progress);
    }

    public static final MapCodec<PassificationAttachment> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Codec.LONG)
                    .optionalFieldOf("until", Map.of())
                    .forGetter(PassificationAttachment::until),
            Codec.unboundedMap(Codec.STRING, Codec.LONG)
                    .optionalFieldOf("last_calm", Map.of())
                    .forGetter(PassificationAttachment::lastCalm),
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("progress", Map.of())
                    .forGetter(PassificationAttachment::progress)
    ).apply(i, PassificationAttachment::new));

    public static final Codec<PassificationAttachment> CODEC = MAP_CODEC.codec();

    /** Is this horse currently calm toward that player? */
    public boolean calm(UUID player, long now) {
        Long end = until.get(player.toString());
        return end != null && (end == FOREVER || now < end);
    }

    /** Has this horse ever been permanently settled toward that player? */
    public boolean permanent(UUID player) {
        return Long.valueOf(FOREVER).equals(until.get(player.toString()));
    }

    /**
     * <b>Has this horse been permanently settled by anybody at all?</b>
     *
     * <p>The one question in this class that is deliberately <i>not</i> per
     * player, and the reason is a rule rather than an optimisation: a permanent
     * calm switches a horse's temperament off outright - it stops fleeing and
     * stops being aggressive toward <b>everything</b>, not only toward whoever
     * fed it (owner's call). A temporary calm stays per player, because it is a
     * bargain rather than a change of character. See
     * {@code Passification.suppresses}.
     */
    public boolean permanentForAnyone() {
        for (Long end : until.values()) {
            if (Long.valueOf(FOREVER).equals(end)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Is a fresh temporary calm allowed yet? Always true if none has ever been
     * bought - the absent case, written out rather than inferred from a
     * subtraction, for the reason {@code HorseCooldownsAttachment.last} spells
     * out at length.
     */
    public boolean offAsCooldown(UUID player, long now, long cooldownTicks) {
        Long last = lastCalm.get(player.toString());
        return last == null || now - last >= cooldownTicks;
    }

    /** How many of {@code item} this player has already fed toward a route. */
    public int fed(UUID player, String item) {
        return progress.getOrDefault(key(player, item), 0);
    }

    /** One more of {@code item} accepted. */
    public PassificationAttachment feed(UUID player, String item) {
        Map<String, Integer> next = new HashMap<>(progress);
        next.merge(key(player, item), 1, Integer::sum);
        return new PassificationAttachment(until, lastCalm, next);
    }

    /**
     * The bargain is struck: calm until {@code end}, the part-payment cleared,
     * and the cooldown clock started.
     */
    public PassificationAttachment settle(UUID player, String item, long end, long now) {
        Map<String, Long> nextUntil = new HashMap<>(until);
        nextUntil.put(player.toString(), end);
        Map<String, Long> nextLast = new HashMap<>(lastCalm);
        nextLast.put(player.toString(), now);
        Map<String, Integer> nextProgress = new HashMap<>(progress);
        nextProgress.remove(key(player, item));
        return new PassificationAttachment(nextUntil, nextLast, nextProgress);
    }

    private static String key(UUID player, String item) {
        return player + "|" + item;
    }
}
