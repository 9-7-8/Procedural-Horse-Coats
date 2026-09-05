package com.example.horsegenetics.neoforge.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.Optional;
import java.util.UUID;

/**
 * Per-horse <b>care and social</b> state - roadmap wiki &sect;7.2 (gated
 * healing) and &sect;13 (bond &amp; herds). None of it is genetic.
 *
 * <ul>
 *   <li><b>{@code bond}</b> - 0..100, the player-relationship number that
 *       {@code BondFollowGoal} reads.</li>
 *   <li><b>{@code bondToday} / {@code dayStamp} / {@code bondTicks}</b> - the
 *       {@code +15 per day} cap and its fractional accumulator.</li>
 *   <li><b>{@code herd}</b> - the id of the herd this horse belongs to. For a
 *       <b>natural wild herd</b> ({@code HerdManager}) this is the <b>lead
 *       horse's UUID</b> - the lead points at itself. For a herd formed by
 *       {@code HorseCareHandler}'s together-timer it is a minted id and
 *       {@code herdBreed} / {@code herdBand} are absent.</li>
 *   <li><b>{@code herdBreed}</b> - the breed id every member of a natural wild
 *       herd shares.</li>
 *   <li><b>{@code herdBand}</b> - {@code "TRADITIONAL"} or {@code "BACHELOR"}.</li>
 *   <li><b>{@code togetherTicks}</b> - the together-timer for tamed-horse herd
 *       formation.</li>
 * </ul>
 *
 * <p>Synced to the client (bond + in-herd flag only) via
 * {@code HorseCareSyncPayload}. Dev only: no legacy handling.
 */
public record HorseCareAttachment(
        int bond,
        Optional<UUID> herd,
        int bondToday,
        long dayStamp,
        long bondTicks,
        long togetherTicks,
        Optional<String> herdBreed,
        Optional<String> herdBand) {

    public static final int MAX_BOND = 100;
    public static final int DAILY_CAP = 15;

    public static final long DAY_TICKS = 24_000L;
    public static final long TICKS_PER_BOND_POINT = 1_200L;
    public static final long TICKS_TO_FORM_HERD = 12_000L;

    public static final HorseCareAttachment DEFAULT =
            new HorseCareAttachment(0, Optional.empty(), 0, 0L, 0L, 0L, Optional.empty(), Optional.empty());

    public HorseCareAttachment {
        herd = herd == null ? Optional.empty() : herd;
        herdBreed = herdBreed == null ? Optional.empty() : herdBreed;
        herdBand = herdBand == null ? Optional.empty() : herdBand;
    }

    public static final MapCodec<HorseCareAttachment> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("bond", 0).forGetter(HorseCareAttachment::bond),
            UUIDUtil.CODEC.optionalFieldOf("herd").forGetter(HorseCareAttachment::herd),
            Codec.INT.optionalFieldOf("bond_today", 0).forGetter(HorseCareAttachment::bondToday),
            Codec.LONG.optionalFieldOf("day_stamp", 0L).forGetter(HorseCareAttachment::dayStamp),
            Codec.LONG.optionalFieldOf("bond_ticks", 0L).forGetter(HorseCareAttachment::bondTicks),
            Codec.LONG.optionalFieldOf("together_ticks", 0L).forGetter(HorseCareAttachment::togetherTicks),
            Codec.STRING.optionalFieldOf("herd_breed").forGetter(HorseCareAttachment::herdBreed),
            Codec.STRING.optionalFieldOf("herd_band").forGetter(HorseCareAttachment::herdBand)
    ).apply(i, HorseCareAttachment::new));

    public static final Codec<HorseCareAttachment> CODEC = MAP_CODEC.codec();

    /** 0 vanilla, 1 faces the player, 2 wanders toward, 3 follows. */
    public int behaviourTier() {
        if (bond >= 81) return 3;
        if (bond >= 61) return 2;
        if (bond >= 31) return 1;
        return 0;
    }

    public boolean inHerd() {
        return herd.isPresent();
    }

    /** This is a natural wild herd managed by {@code HerdManager} (has a breed + band). */
    public boolean inWildHerd() {
        return herd.isPresent() && herdBreed.isPresent();
    }

    public HorseCareAttachment withBond(int newBond) {
        return new HorseCareAttachment(clampBond(newBond), herd, bondToday, dayStamp, bondTicks,
                togetherTicks, herdBreed, herdBand);
    }

    public HorseCareAttachment withHerd(Optional<UUID> newHerd) {
        return new HorseCareAttachment(bond, newHerd, bondToday, dayStamp, bondTicks, togetherTicks,
                herdBreed, herdBand);
    }

    /** Join (or found) a natural wild herd: {@code lead} is the lead horse's UUID. */
    public HorseCareAttachment withWildHerd(UUID lead, String breedId, String band) {
        return new HorseCareAttachment(bond, Optional.of(lead), bondToday, dayStamp, bondTicks,
                togetherTicks, Optional.of(breedId), Optional.of(band));
    }

    public HorseCareAttachment with(int newBond, Optional<UUID> newHerd, int newBondToday,
                                    long newDayStamp, long newBondTicks, long newTogetherTicks) {
        return new HorseCareAttachment(clampBond(newBond), newHerd, Math.max(0, newBondToday),
                newDayStamp, Math.max(0L, newBondTicks), Math.max(0L, newTogetherTicks), herdBreed, herdBand);
    }

    private static int clampBond(int b) {
        return b < 0 ? 0 : Math.min(b, MAX_BOND);
    }
}
