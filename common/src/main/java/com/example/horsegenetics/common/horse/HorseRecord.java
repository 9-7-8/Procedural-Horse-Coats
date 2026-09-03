package com.example.horsegenetics.common.horse;

import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;

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
 * <p>{@code geneticCode} and {@code epigenomeCode} together are the horse's
 * {@link Genome} - which alleles it carries, and the priority + epigenetic seed
 * riding on each of those allele copies. <b>Both live here</b>, because both are
 * heritable facts about the animal in exactly the same sense: assigned once at
 * birth (rolled for a founder, inherited for a foal) and never re-rolled. Keeping
 * the epigenome on the entity instead meant a horse's record could describe an
 * ancestor's alleles but not its coat, so the family tree had to invent a
 * plausible stand-in from the record UUID.
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
        String epigenomeCode,
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
        Objects.requireNonNull(epigenomeCode, "epigenomeCode");
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

    /**
     * The "nothing assigned yet" sentinel: no name, the all-default genotype and
     * an <b>empty</b> epigenome code, which is what {@link #hasGenome()} tests.
     * A spawn or breeding handler replaces it the moment a horse joins the
     * world, so no real horse is ever seen or saved holding one.
     */
    public static HorseRecord unassigned(UUID id) {
        return new HorseRecord(id, Sex.FEMALE, "", "", Optional.empty(),
                Genotype.wildType().toCode(), "",
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), 0, 0.0, 0.0,
                Optional.empty());
    }

    /** A foundation horse - no recorded parents, generation 0, stats unrecorded. */
    public static HorseRecord founder(UUID id, Sex sex, String firstName, String lastName, Genome genome) {
        return new HorseRecord(id, sex, firstName, lastName, Optional.empty(),
                genome.genotypeCode(), genome.epigenomeCode(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), 0, 0.0, 0.0, Optional.empty());
    }

    /** A horse bred from two known parents; {@code generation} is the caller's {@code 1 + max(parent gens)}. */
    public static HorseRecord bred(UUID id, Sex sex, String firstName, String lastName, Genome genome,
                                   UUID motherId, UUID fatherId, int generation) {
        return new HorseRecord(id, sex, firstName, lastName, Optional.empty(),
                genome.genotypeCode(), genome.epigenomeCode(),
                Optional.of(motherId), Optional.of(fatherId), Optional.empty(), Optional.empty(), generation,
                0.0, 0.0, Optional.empty());
    }

    // --- the genome ---------------------------------------------------

    /** The alleles this horse carries. */
    public Genotype genotype() {
        return Genotype.parse(geneticCode);
    }

    /** The priority + epigenetic seed on each of those allele copies. */
    public Epigenome epigenome() {
        return Epigenome.parse(epigenomeCode);
    }

    /** Both together - what the coat pipeline needs. */
    public Genome genome() {
        return Genome.parse(geneticCode, epigenomeCode);
    }

    /** Has a real genome been assigned yet, or is this still the blank sentinel? */
    public boolean hasGenome() {
        return !epigenomeCode.isEmpty();
    }

    public HorseRecord withGenome(Genome genome) {
        return new HorseRecord(id, sex, firstName, lastName, barnName,
                genome.genotypeCode(), genome.epigenomeCode(),
                motherId, fatherId, tamedBy, bredBy, generation, speed, health, parentStats);
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
        return new HorseRecord(id, sex, newFirst, newLast, barnName, geneticCode, epigenomeCode,
                motherId, fatherId, tamedBy, bredBy, generation, speed, health, parentStats);
    }

    public HorseRecord withBarnName(Optional<String> newBarnName) {
        return new HorseRecord(id, sex, firstName, lastName, newBarnName, geneticCode, epigenomeCode,
                motherId, fatherId, tamedBy, bredBy, generation, speed, health, parentStats);
    }

    public HorseRecord withTamedBy(String username) {
        return new HorseRecord(id, sex, firstName, lastName, barnName, geneticCode, epigenomeCode,
                motherId, fatherId, Optional.of(username), bredBy, generation, speed, health, parentStats);
    }

    public HorseRecord withBredBy(String username) {
        return new HorseRecord(id, sex, firstName, lastName, barnName, geneticCode, epigenomeCode,
                motherId, fatherId, tamedBy, Optional.of(username), generation, speed, health, parentStats);
    }

    public HorseRecord withStats(double newSpeed, double newHealth) {
        return new HorseRecord(id, sex, firstName, lastName, barnName, geneticCode, epigenomeCode,
                motherId, fatherId, tamedBy, bredBy, generation, newSpeed, newHealth, parentStats);
    }

    public HorseRecord withParentStats(ParentStats newParentStats) {
        return new HorseRecord(id, sex, firstName, lastName, barnName, geneticCode, epigenomeCode,
                motherId, fatherId, tamedBy, bredBy, generation, speed, health, Optional.ofNullable(newParentStats));
    }

    public boolean hasKnownParents() {
        return motherId.isPresent() || fatherId.isPresent();
    }
}
