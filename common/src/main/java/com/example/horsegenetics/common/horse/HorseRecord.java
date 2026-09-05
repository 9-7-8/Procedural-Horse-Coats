package com.example.horsegenetics.common.horse;

import com.example.horsegenetics.common.breed.BreedLineage;
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
 * birth (rolled for a founder, inherited for a foal) and never re-rolled.
 *
 * <p>{@code breed} is the horse's <b>breed label</b> - a {@link BreedLineage}
 * token. A wild herd is stamped with the breed it was rolled from; a lone wild
 * horse, a {@code /summon} or a spawn-egg horse is {@code "unknown"}; a foal's
 * label is {@link BreedLineage#combine} of its parents'. It is a fact about
 * ancestry, not a derived value, so it is stored rather than resolved. It does,
 * though, feed {@link #traits()}: a pure breed's horses are pinned to that
 * breed's stat bands.
 *
 * <p>There is <b>no {@code sex} field</b>. Sex is a gene like any other
 * ({@code horsegenetics.sex}, {@code X}/{@code Y}), so it is already in
 * {@code geneticCode} and {@link #sex()} reads it from there.
 *
 * <p>There are <b>no {@code speed} or {@code health} fields</b> either. A
 * horse's speed, max health, jump strength and body size are a pure function of
 * its genome and its breed ({@link Traits}); {@link #traits()} resolves them on
 * demand.
 *
 * <p>{@code parentStats} is the low/high of the two parents' resolved speed and
 * health at the moment of birth, so the UI can say whether this foal came out
 * above both its parents, between them, or below.
 */
public record HorseRecord(
        UUID id,
        String firstName,
        String lastName,
        Optional<String> barnName,
        String geneticCode,
        String epigenomeCode,
        Optional<String> breed,
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
        breed = breed == null ? Optional.empty() : breed;
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
     */
    public static HorseRecord unassigned(UUID id) {
        return new HorseRecord(id, "", "", Optional.empty(),
                Genotype.wildType().toCode(), "", Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), 0,
                Optional.empty());
    }

    /**
     * A foundation horse - no recorded parents, generation 0. {@code breedToken}
     * is a {@link BreedLineage} token ({@code null} / blank reads as
     * {@code "unknown"}).
     */
    public static HorseRecord founder(UUID id, String firstName, String lastName, Genome genome,
                                      String breedToken) {
        return new HorseRecord(id, firstName, lastName, Optional.empty(),
                genome.genotypeCode(), genome.epigenomeCode(), breedToken(breedToken),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), 0, Optional.empty());
    }

    /** A foundation horse with no breed identity ({@code "unknown"}). */
    public static HorseRecord founder(UUID id, String firstName, String lastName, Genome genome) {
        return founder(id, firstName, lastName, genome, BreedLineage.UNKNOWN.toToken());
    }

    /**
     * A horse bred from two known parents; {@code generation} is the caller's
     * {@code 1 + max(parent gens)}, and {@code breedToken} the
     * {@link BreedLineage#combine} of the two parents' labels.
     */
    public static HorseRecord bred(UUID id, String firstName, String lastName, Genome genome,
                                   String breedToken, UUID motherId, UUID fatherId, int generation) {
        return new HorseRecord(id, firstName, lastName, Optional.empty(),
                genome.genotypeCode(), genome.epigenomeCode(), breedToken(breedToken),
                Optional.of(motherId), Optional.of(fatherId), Optional.empty(), Optional.empty(), generation,
                Optional.empty());
    }

    /** A bred foal with no breed identity carried through - test / legacy convenience. */
    public static HorseRecord bred(UUID id, String firstName, String lastName, Genome genome,
                                   UUID motherId, UUID fatherId, int generation) {
        return bred(id, firstName, lastName, genome, BreedLineage.UNKNOWN.toToken(),
                motherId, fatherId, generation);
    }

    private static Optional<String> breedToken(String token) {
        return token == null || token.isBlank() ? Optional.empty() : Optional.of(token);
    }

    // --- the genome ---------------------------------------------------

    /**
     * This horse's {@link Sex}, read off the sex locus in {@link #geneticCode}.
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

    /** The horse's breed as a value object ({@code "unknown"} when unset). */
    public BreedLineage lineage() {
        return BreedLineage.parse(breed.orElse(null));
    }

    /** Has a real genome been assigned yet, or is this still the blank sentinel? */
    public boolean hasGenome() {
        return !epigenomeCode.isEmpty();
    }

    public HorseRecord withGenome(Genome genome) {
        return new HorseRecord(id, firstName, lastName, barnName,
                genome.genotypeCode(), genome.epigenomeCode(), breed,
                motherId, fatherId, tamedBy, bredBy, generation, parentStats);
    }

    public HorseRecord withBreed(String breedToken) {
        return new HorseRecord(id, firstName, lastName, barnName, geneticCode, epigenomeCode,
                breedToken(breedToken), motherId, fatherId, tamedBy, bredBy, generation, parentStats);
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
     * <b>The body this genome describes</b> - speed, max health, jump strength,
     * body scale and the disorders it expresses - resolved fresh every time.
     *
     * <p>Resolved from the whole genome <b>and the breed</b>: a pure breed pins
     * one or more body axes to a target band, and the magical body-stat genes
     * land the horse inside it from its epigenetic seeds. A cross uses the
     * average of its two components' bands; a mixed or unknown horse pins
     * nothing and its body stats take the ordinary bounded-Gaussian draw.
     */
    public Traits traits() {
        return hasGenome()
                ? HorseTraits.resolve(genotype(), epigenome(), lineage().statTargets(), true)
                : HorseTraits.resolve(genotype());
    }

    public HorseRecord withNames(String newFirst, String newLast) {
        return new HorseRecord(id, newFirst, newLast, barnName, geneticCode, epigenomeCode, breed,
                motherId, fatherId, tamedBy, bredBy, generation, parentStats);
    }

    public HorseRecord withBarnName(Optional<String> newBarnName) {
        return new HorseRecord(id, firstName, lastName, newBarnName, geneticCode, epigenomeCode, breed,
                motherId, fatherId, tamedBy, bredBy, generation, parentStats);
    }

    public HorseRecord withTamedBy(String username) {
        return new HorseRecord(id, firstName, lastName, barnName, geneticCode, epigenomeCode, breed,
                motherId, fatherId, Optional.of(username), bredBy, generation, parentStats);
    }

    public HorseRecord withBredBy(String username) {
        return new HorseRecord(id, firstName, lastName, barnName, geneticCode, epigenomeCode, breed,
                motherId, fatherId, tamedBy, Optional.of(username), generation, parentStats);
    }

    public HorseRecord withParentStats(ParentStats newParentStats) {
        return new HorseRecord(id, firstName, lastName, barnName, geneticCode, epigenomeCode, breed,
                motherId, fatherId, tamedBy, bredBy, generation, Optional.ofNullable(newParentStats));
    }

    public boolean hasKnownParents() {
        return motherId.isPresent() || fatherId.isPresent();
    }
}
