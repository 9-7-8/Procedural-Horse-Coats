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
 *       {@code BondFollowGoal} reads to decide whether the horse looks at,
 *       walks toward, or follows its owner.</li>
 *   <li><b>{@code bondToday} / {@code dayStamp}</b> - the {@code +15 per day}
 *       cap. {@code dayStamp} is {@code level.getDayTime() / 24000}; when it
 *       rolls over, {@code bondToday} resets.</li>
 *   <li><b>{@code bondTicks}</b> - fractional-bond accumulator. Proximity and
 *       riding add ticks here; every {@link #TICKS_PER_BOND_POINT} it converts
 *       to one bond point (subject to the daily cap).</li>
 *   <li><b>{@code herd}</b> - the id of the herd this horse belongs to, if
 *       any. Minted by {@code HorseCareHandler} when two or more horses have
 *       been together long enough.</li>
 *   <li><b>{@code togetherTicks}</b> - the herd-formation counter: accumulates
 *       while another horse is nearby, decays otherwise. Crosses
 *       {@link #TICKS_TO_FORM_HERD} to join/form a herd; decays to 0 to leave
 *       one.</li>
 * </ul>
 *
 * <p>Synced to the client (bond + in-herd flag only) via
 * {@code HorseCareSyncPayload} for the inventory-screen panel. Dev only: no
 * legacy handling, {@link #DEFAULT} is the attachment default.
 */
public record HorseCareAttachment(
        int bond,
        Optional<UUID> herd,
        int bondToday,
        long dayStamp,
        long bondTicks,
        long togetherTicks) {

    public static final int MAX_BOND = 100;
    public static final int DAILY_CAP = 15;

    /** A Minecraft day. Also the unit for the daily bond cap (roadmap &sect;21). */
    public static final long DAY_TICKS = 24_000L;

    /** One (real) minute of proximity / riding = this many ticks. */
    public static final long TICKS_PER_BOND_POINT = 1_200L;

    /** Ten minutes of company forms a herd; ten minutes alone dissolves it. */
    public static final long TICKS_TO_FORM_HERD = 12_000L;

    public static final HorseCareAttachment DEFAULT =
            new HorseCareAttachment(0, Optional.empty(), 0, 0L, 0L, 0L);

    public static final MapCodec<HorseCareAttachment> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("bond", 0).forGetter(HorseCareAttachment::bond),
            UUIDUtil.CODEC.optionalFieldOf("herd").forGetter(HorseCareAttachment::herd),
            Codec.INT.optionalFieldOf("bond_today", 0).forGetter(HorseCareAttachment::bondToday),
            Codec.LONG.optionalFieldOf("day_stamp", 0L).forGetter(HorseCareAttachment::dayStamp),
            Codec.LONG.optionalFieldOf("bond_ticks", 0L).forGetter(HorseCareAttachment::bondTicks),
            Codec.LONG.optionalFieldOf("together_ticks", 0L).forGetter(HorseCareAttachment::togetherTicks)
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

    public HorseCareAttachment withBond(int newBond) {
        return new HorseCareAttachment(clampBond(newBond), herd, bondToday, dayStamp, bondTicks, togetherTicks);
    }

    public HorseCareAttachment withHerd(Optional<UUID> newHerd) {
        return new HorseCareAttachment(bond, newHerd, bondToday, dayStamp, bondTicks, togetherTicks);
    }

    public HorseCareAttachment with(int newBond, Optional<UUID> newHerd, int newBondToday,
                                    long newDayStamp, long newBondTicks, long newTogetherTicks) {
        return new HorseCareAttachment(clampBond(newBond), newHerd, Math.max(0, newBondToday),
                newDayStamp, Math.max(0L, newBondTicks), Math.max(0L, newTogetherTicks));
    }

    private static int clampBond(int b) {
        return b < 0 ? 0 : Math.min(b, MAX_BOND);
    }
}
