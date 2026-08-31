package com.example.horsegenetics.common.horse;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Everything the mod tracks about one horse, as plain data. Layer-1: no game
 * dependency - the integration layer converts between this and Minecraft's
 * attachment / SavedData systems at the boundary.
 *
 * <p>A horse has a two-part generated name ({@code firstName} / {@code lastName},
 * from the alpha / beta word tables) that can only be changed with a name tag,
 * and an optional {@code barnName} the owner can change freely. The name shown
 * in-game is {@link #displayName()}: the barn name if set, otherwise
 * "{@code firstName lastName}".
 *
 * <p>{@code speed} and {@code health} mirror the entity's movement-speed and
 * max-health attribute values. A foundation horse copies whatever the entity
 * spawned with; a foal's are rolled from its parents by
 * {@link HorseStats#rollFoalStat}. Both are <b>rounded up</b> by the
 * constructor - health to a whole number, speed to 3 decimals - and are
 * uncapped. {@code 0.0} means "not recorded yet". {@code parentStats}, when
 * present, is the low/high of the two parents' values at birth, for the UI to
 * colour this horse's stats against.
 */
public record HorseRecord(
        UUID id,
        Sex sex,
        String firstName,
        String lastName,
        Optional<String> barnName,
        String geneticCode,
        Optional<UUID> motherId,
        Optional<UUID> fatherId,
        Optional<String> tamedBy,
        Optional<String> bredBy,
        int generation,
        double speed,
        double health,
        Optional<ParentStats> parentStats) {

    public static final int MAX_BARN_NAME = 16;

    public HorseRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(sex, "sex");
        Objects.requireNonNull(firstName, "firstName");
        Objects.requireNonNull(lastName, "lastName");
        Objects.requireNonNull(geneticCode, "geneticCode");
        barnName = clampBarnName(barnName);
        motherId = motherId == null ? Optional.empty() : motherId;
        fatherId = fatherId == null ? Optional.empty() : fatherId;
        tamedBy = tamedBy == null ? Optional.empty() : tamedBy;
        bredBy = bredBy == null ? Optional.empty() : bredBy;
        parentStats = parentStats == null ? Optional.empty() : parentStats;
        generation = Math.max(0, generation);
        speed = ceilSpeed(speed);
        health = ceilHealth(health);
    }

    /** Speed rounds up to 3 decimals (a horse's speed attribute is a small fraction). */
    public static double ceilSpeed(double v) {
        return v <= 0.0 ? 0.0 : Math.ceil(v * 1000.0) / 1000.0;
    }

    /** Health rounds up to a whole number. No cap. */
    public static double ceilHealth(double v) {
        return v <= 0.0 ? 0.0 : Math.ceil(v);
    }

    private static Optional<String> clampBarnName(Optional<String> barnName) {
        if (barnName == null || barnName.isEmpty()) {
            return Optional.empty();
        }
        String s = barnName.get().strip();
        if (s.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(s.length() > MAX_BARN_NAME ? s.substring(0, MAX_BARN_NAME) : s);
    }

    /** A foundation horse - no recorded parents, generation 0, stats unrecorded. */
    public static HorseRecord founder(UUID id, Sex sex, String firstName, String lastName, String geneticCode) {
        return new HorseRecord(id, sex, firstName, lastName, Optional.empty(), geneticCode,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), 0, 0.0, 0.0, Optional.empty());
    }

    /** A horse bred from two known parents; {@code generation} is the caller's {@code 1 + max(parent gens)}. */
    public static HorseRecord bred(UUID id, Sex sex, String firstName, String lastName, String geneticCode,
                                   UUID motherId, UUID fatherId, int generation) {
        return new HorseRecord(id, sex, firstName, lastName, Optional.empty(), geneticCode,
                Optional.of(motherId), Optional.of(fatherId), Optional.empty(), Optional.empty(), generation,
                0.0, 0.0, Optional.empty());
    }

    /** What to show in-game: the barn name if set, otherwise "first last". */
    public String displayName() {
        return barnName.orElseGet(() -> (firstName + " " + lastName).strip());
    }

    /** Who this horse is attributed to for the family tree: its breeder, else its tamer. */
    public Optional<String> attribution() {
        return bredBy.or(() -> tamedBy);
    }

    /** True once this horse has a registered name or a barn name (i.e. it isn't the blank sentinel). */
    public boolean hasName() {
        return !firstName.isEmpty() || !lastName.isEmpty() || barnName.isPresent();
    }

    /** True once {@link #speed} and {@link #health} have been filled in. */
    public boolean hasStats() {
        return speed > 0.0 && health > 0.0;
    }

    public HorseRecord withNames(String newFirst, String newLast) {
        return new HorseRecord(id, sex, newFirst, newLast, barnName, geneticCode,
                motherId, fatherId, tamedBy, bredBy, generation, speed, health, parentStats);
    }

    public HorseRecord withBarnName(Optional<String> newBarnName) {
        return new HorseRecord(id, sex, firstName, lastName, newBarnName, geneticCode,
                motherId, fatherId, tamedBy, bredBy, generation, speed, health, parentStats);
    }

    public HorseRecord withTamedBy(String username) {
        return new HorseRecord(id, sex, firstName, lastName, barnName, geneticCode,
                motherId, fatherId, Optional.of(username), bredBy, generation, speed, health, parentStats);
    }

    public HorseRecord withBredBy(String username) {
        return new HorseRecord(id, sex, firstName, lastName, barnName, geneticCode,
                motherId, fatherId, tamedBy, Optional.of(username), generation, speed, health, parentStats);
    }

    public HorseRecord withStats(double newSpeed, double newHealth) {
        return new HorseRecord(id, sex, firstName, lastName, barnName, geneticCode,
                motherId, fatherId, tamedBy, bredBy, generation, newSpeed, newHealth, parentStats);
    }

    public HorseRecord withParentStats(ParentStats newParentStats) {
        return new HorseRecord(id, sex, firstName, lastName, barnName, geneticCode,
                motherId, fatherId, tamedBy, bredBy, generation, speed, health, Optional.ofNullable(newParentStats));
    }

    public boolean hasKnownParents() {
        return motherId.isPresent() || fatherId.isPresent();
    }
}
