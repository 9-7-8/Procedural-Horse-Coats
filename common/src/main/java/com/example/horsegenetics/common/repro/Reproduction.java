package com.example.horsegenetics.common.repro;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * <b>A horse's reproductive record</b> - the value behind the
 * {@code HORSE_REPRO} attachment. The codec lives in the NeoForge module,
 * because {@code common/} imports no DFU.
 *
 * <p>Nothing here is a current state. The state is <b>derived</b> from these
 * anchors and the time ({@link ReproRules#stateAt}), so a horse that reloads
 * after three cycles has nothing to catch up on.
 *
 * <ul>
 *   <li><b>{@code cyclePhase}</b> - where in her cycle a mare is at tick 0, as
 *       a fraction. Taken from the entity id ({@link #fresh}) so no two mares
 *       come into heat together and none needs rolling. A fraction rather than
 *       ticks, so changing the gestation setting moves every mare's cycle
 *       without making it jump.</li>
 *   <li><b>{@code pregnancy}</b> - what she is carrying, if anything.</li>
 *   <li><b>{@code foaledTick}</b> - her last birth <i>from a pregnancy</i>, or
 *       {@link #NEVER}. Instant golden-carrot foals do not set it.</li>
 *   <li><b>{@code nursing}</b> - the foals from that birth she is still feeding.
 *       Empty means she is not lactating.</li>
 *   <li><b>{@code apartSince}</b> - when her foals were last all out of reach,
 *       or {@link #NEVER} while one is near. A day apart weans them.</li>
 *   <li><b>{@code coverDay} / {@code covers}</b> - the stallion side: which day
 *       he last covered a mare or filled a jar, and how many times that day.</li>
 *   <li><b>{@code lastNaturalTry}</b> - when a stallion last covered her on his
 *       own, so that happens once per heat ({@link ReproRules#mayTryNaturally}).</li>
 * </ul>
 */
public record Reproduction(double cyclePhase, Optional<Pregnancy> pregnancy, long foaledTick,
                           List<UUID> nursing, long apartSince, long coverDay, int covers,
                           long lastNaturalTry) {

    public static final long NEVER = Long.MIN_VALUE;

    public Reproduction {
        pregnancy = pregnancy == null ? Optional.empty() : pregnancy;
        nursing = nursing == null ? List.of() : List.copyOf(nursing);
        if (!(cyclePhase >= 0.0 && cyclePhase < 1.0)) {
            cyclePhase = 0.0;
        }
    }

    /** A horse with no history, its cycle phase taken from its id. */
    public static Reproduction fresh(UUID id) {
        Objects.requireNonNull(id, "id");
        // the top 53 bits of the low half, as a double in [0, 1)
        double phase = (id.getLeastSignificantBits() >>> 11) * 0x1.0p-53;
        return new Reproduction(phase, Optional.empty(), NEVER, List.of(), NEVER, NEVER, 0, NEVER);
    }

    public boolean pregnant() {
        return pregnancy.isPresent();
    }

    public boolean lactating() {
        return !nursing.isEmpty();
    }

    public Reproduction withPregnancy(Pregnancy p) {
        return withPregnancy(Optional.of(p));
    }

    public Reproduction withPregnancy(Optional<Pregnancy> p) {
        return new Reproduction(cyclePhase, p, foaledTick, nursing, apartSince, coverDay, covers, lastNaturalTry);
    }

    /** She has given birth at {@code now} to {@code foals} (empty if none survived). */
    public Reproduction foaled(long now, List<UUID> foals) {
        return new Reproduction(cyclePhase, Optional.empty(), now, foals, NEVER, coverDay, covers, lastNaturalTry);
    }

    /** Her cycle moved - the testing clock's "into heat now". */
    public Reproduction withCyclePhase(double phase) {
        return new Reproduction(phase, pregnancy, foaledTick, nursing, apartSince, coverDay, covers, lastNaturalTry);
    }

    /** Her last birth moved - the testing clock's "skip to foal heat". */
    public Reproduction withFoaledTick(long tick) {
        return new Reproduction(cyclePhase, pregnancy, tick, nursing, apartSince, coverDay, covers, lastNaturalTry);
    }

    public Reproduction weaned() {
        return new Reproduction(cyclePhase, pregnancy, foaledTick, List.of(), NEVER, coverDay, covers,
                lastNaturalTry);
    }

    public Reproduction withApartSince(long tick) {
        return new Reproduction(cyclePhase, pregnancy, foaledTick, nursing, tick, coverDay, covers, lastNaturalTry);
    }

    /** A stallion covered her on his own at {@code tick} - her one natural try this heat. */
    public Reproduction withNaturalTry(long tick) {
        return new Reproduction(cyclePhase, pregnancy, foaledTick, nursing, apartSince, coverDay, covers, tick);
    }

    /** Covers he has already made on the day containing {@code now}. */
    public int coversOn(long now, long dayTicks) {
        return Math.floorDiv(now, dayTicks) == coverDay ? covers : 0;
    }

    /** One more cover (or jar fill) on the day containing {@code now}. */
    public Reproduction withCover(long now, long dayTicks) {
        long day = Math.floorDiv(now, dayTicks);
        int count = day == coverDay ? covers + 1 : 1;
        return new Reproduction(cyclePhase, pregnancy, foaledTick, nursing, apartSince, day, count, lastNaturalTry);
    }
}
