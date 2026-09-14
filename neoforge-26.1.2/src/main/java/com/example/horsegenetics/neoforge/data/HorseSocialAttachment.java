package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.common.herd.Relationship;
import com.example.horsegenetics.common.herd.SocialLedger;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A horse's <b>social life</b>: when it was born, who its dam is, which band it was
 * born into and when it will leave, and its {@link SocialLedger} of relationships.
 * The rules that move all of this are {@code common.herd.HerdRules}; this is only
 * the storage and its codec, kept here because {@code common/} imports no DFU.
 *
 * <ul>
 *   <li><b>{@code bornTick}</b> - the game tick it was born, or {@link #UNKNOWN}
 *       until the social tick first sees it and estimates one. Game time, not day
 *       time, so sleeping does not age a horse.</li>
 *   <li><b>{@code dam}</b> - its mother, where known: the record's for a bred
 *       foal, the band mare it attached to for a wild one.</li>
 *   <li><b>{@code natalHerd}</b> - the band it was born into, while it is still
 *       there. Cleared when it disperses.</li>
 *   <li><b>{@code disperseAfterDays}</b> - adult days it stays before leaving,
 *       rolled once.</li>
 *   <li><b>{@code lastDecay}</b> - when the ledger last faded, so decay does not
 *       compound faster for a horse scanned more often.</li>
 * </ul>
 *
 * <p>Dev only: no legacy handling.
 */
public record HorseSocialAttachment(long bornTick, Optional<UUID> dam, Optional<UUID> natalHerd,
                                    double disperseAfterDays, List<Relationship> relationships,
                                    long lastDecay) {

    public static final long UNKNOWN = Long.MIN_VALUE;

    public static final HorseSocialAttachment DEFAULT =
            new HorseSocialAttachment(UNKNOWN, Optional.empty(), Optional.empty(), 0.0, List.of(), 0L);

    public HorseSocialAttachment {
        dam = dam == null ? Optional.empty() : dam;
        natalHerd = natalHerd == null ? Optional.empty() : natalHerd;
        relationships = relationships == null ? List.of() : List.copyOf(relationships);
    }

    private static final Codec<Relationship> RELATIONSHIP = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("other").forGetter(Relationship::other),
            Codec.DOUBLE.optionalFieldOf("familiarity", 0.0).forGetter(Relationship::familiarity),
            Codec.DOUBLE.optionalFieldOf("rank", 0.0).forGetter(Relationship::rank),
            Codec.DOUBLE.optionalFieldOf("grooming", 0.0).forGetter(Relationship::grooming),
            Codec.DOUBLE.optionalFieldOf("rivalry", 0.0).forGetter(Relationship::rivalry),
            Codec.LONG.optionalFieldOf("last", 0L).forGetter(Relationship::lastTogether)
    ).apply(i, Relationship::new));

    public static final MapCodec<HorseSocialAttachment> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.LONG.optionalFieldOf("born", UNKNOWN).forGetter(HorseSocialAttachment::bornTick),
            UUIDUtil.CODEC.optionalFieldOf("dam").forGetter(HorseSocialAttachment::dam),
            UUIDUtil.CODEC.optionalFieldOf("natal_herd").forGetter(HorseSocialAttachment::natalHerd),
            Codec.DOUBLE.optionalFieldOf("disperse_after", 0.0).forGetter(HorseSocialAttachment::disperseAfterDays),
            RELATIONSHIP.listOf().optionalFieldOf("relationships", List.of())
                    .forGetter(HorseSocialAttachment::relationships),
            Codec.LONG.optionalFieldOf("last_decay", 0L).forGetter(HorseSocialAttachment::lastDecay)
    ).apply(i, HorseSocialAttachment::new));

    public boolean bornKnown() {
        return bornTick != UNKNOWN;
    }

    public SocialLedger ledger() {
        return SocialLedger.of(relationships);
    }

    public HorseSocialAttachment withLedger(SocialLedger ledger, long decayedAt) {
        return new HorseSocialAttachment(bornTick, dam, natalHerd, disperseAfterDays, ledger.entries(), decayedAt);
    }

    public HorseSocialAttachment withBirth(long born, Optional<UUID> newDam, Optional<UUID> natal, double disperseAfter) {
        return new HorseSocialAttachment(born, newDam, natal, disperseAfter, relationships, lastDecay);
    }

    public HorseSocialAttachment withDam(Optional<UUID> newDam) {
        return new HorseSocialAttachment(bornTick, newDam, natalHerd, disperseAfterDays, relationships, lastDecay);
    }

    /** It has left the band it was born into. */
    public HorseSocialAttachment dispersed() {
        return new HorseSocialAttachment(bornTick, dam, Optional.empty(), disperseAfterDays, relationships, lastDecay);
    }
}
