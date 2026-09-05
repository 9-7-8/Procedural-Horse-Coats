package com.example.horsegenetics.common.horse;

import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.common.trait.Traits;

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
 * <p>There is <b>no {@code sex} field</b>. Sex is a gene like any other
 * ({@code horsegenetics.sex}, {@code X}/{@code Y}), so it is already in
 * {@code geneticCode} and {@link #sex()} reads it from there. Storing it twice
 * would let a record disagree with the genome it carries, and would mean a
 * foal's sex was invented rather than inherited.
 *
 * <p>There are <b>no {@code speed} or {@code health} fields</b> either, for the
 * same reason. A horse's speed, max health, jump strength and body size are a
 * pure function of its genotype ({@link Traits}), so storing them would be
 * storing a derived value that could disagree with the alleles beside it - and
 * did, back when they were a uniform random roll off the two parents' numbers
 * with no genetics in it. {@link #traits()} resolves them on demand.
 *
 * <p>{@code parentStats} survives that change and is worth more after it: it is
 * the low/high of the two parents' resolved speed and health at the moment of
 * birth, so the UI can say whether this foal came out above both its parents,
 * between them, or below - which is now a statement about which alleles it drew
 * rather than about how a die fell.
 */
public record HorseRecord(
        UUID id,
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
        Optional<ParentStats> parentStats) {

    public static final int MAX_BARN_NAME = 16;

    public HorseRecord {
        Objects.requireNonNull(id, "id");
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
        return new HorseRecord(id, "", "", Optional.empty(),
                Genotype.wildType().toCode(), "",
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), 0,
                Optional.empty());
    }

    /**
     * A foundation horse - no recorded parents, generation 0.
     * Its sex comes from {@code genome}; a caller that wants to <i>choose</i>
     * one hands in {@link Genome#withSex}.
     */
    public static HorseRecord founder(UUID id, String firstName, String lastName, Genome genome) {
        return new HorseRecord(id, firstName, lastName, Optional.empty(),
                genome.genotypeCode(), genome.epigenomeCode(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), 0, Optional.empty());
    }

    /**
     * A horse bred from two known parents; {@code generation} is the caller's
     * {@code 1 + max(parent gens)}. Its sex is <b>inherited</b> - whichever sex
     * chromosome the Mendelian draw handed it - so there is nothing to pass.
     */
    public static HorseRecord bred(UUID id, String firstName, String lastName, Genome genome,
                                   UUID motherId, UUID fatherId, int generation) {
        return new HorseRecord(id, firstName, lastName, Optional.empty(),
                genome.genotypeCode(), genome.epigenomeCode(),
                Optional.of(motherId), Optional.of(fatherId), Optional.empty(), Optional.empty(), generation,
                Optional.empty());
    }

    // --- the genome ---------------------------------------------------

    /**
     * This horse's {@link Sex}, read off the sex locus in {@link #geneticCode}
     * - the single source of truth. A code with no sex segment (the blank
     * sentinel) reads as a mare, which is what this record's hard-coded default
     * always was.
     */
    public Sex sex() {
        return Genotype.sexOf(geneticCode);
    }

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
        return new HorseRecord(id, firstName, lastName, barnName,
                genome.genotypeCode(), genome.epigenomeCode(),
                motherId, fatherId, tamedBy, bredBy, generation, parentStats);
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

    /**
     * <b>The body this genotype describes</b> - speed, max health, jump
     * strength, body scale and the disorders it expresses - resolved fresh from
     * {@link #geneticCode} every time it is asked for.
     *
     * <p>Resolved from the <b>whole genome</b>, genotype and epigenome together,
     * because a trait can be epigenetic - the magical size locus is "big by
     * <i>this much</i>", and the amount is written on the allele copy. A record
     * with no epigenome yet ({@link #hasGenome()} false) falls back to the
     * genotype alone, where such a trait reports its midpoint.
     *
     * <p>Cheap enough to call per frame is <i>not</i> the claim: it parses the
     * code. Callers that need it in a hot loop should hold on to the result.
     */
    public Traits traits() {
        return hasGenome()
                ? HorseTraits.resolve(genotype(), epigenome(), true)
                : HorseTraits.resolve(genotype());
    }

    public HorseRecord withNames(String newFirst, String newLast) {
        return new HorseRecord(id, newFirst, newLast, barnName, geneticCode, epigenomeCode,
                motherId, fatherId, tamedBy, bredBy, generation, parentStats);
    }

    public HorseRecord withBarnName(Optional<String> newBarnName) {
        return new HorseRecord(id, firstName, lastName, newBarnName, geneticCode, epigenomeCode,
                motherId, fatherId, tamedBy, bredBy, generation, parentStats);
    }

    public HorseRecord withTamedBy(String username) {
        return new HorseRecord(id, firstName, lastName, barnName, geneticCode, epigenomeCode,
                motherId, fatherId, Optional.of(username), bredBy, generation, parentStats);
    }

    public HorseRecord withBredBy(String username) {
        return new HorseRecord(id, firstName, lastName, barnName, geneticCode, epigenomeCode,
                motherId, fatherId, tamedBy, Optional.of(username), generation, parentStats);
    }

    public HorseRecord withParentStats(ParentStats newParentStats) {
        return new HorseRecord(id, firstName, lastName, barnName, geneticCode, epigenomeCode,
                motherId, fatherId, tamedBy, bredBy, generation, Optional.ofNullable(newParentStats));
    }

    public boolean hasKnownParents() {
        return motherId.isPresent() || fatherId.isPresent();
    }
}
