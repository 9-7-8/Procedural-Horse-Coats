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
 * horse, a {@code /summon} or a spawn-egg horse is {@code "feral_mixed"}; a foal's
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
 * <p>{@code ownerId} mirrors vanilla's owner so the <b>client</b> can know it.
 * Vanilla syncs a horse's tamed flag but never its owner, so ownership is a
 * server-only fact unless the mod carries it - see {@link #ownedBy}.
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
        Optional<ParentStats> parentStats,
        boolean gelded,
        Optional<UUID> ownerId) {

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
        ownerId = ownerId == null ? Optional.empty() : ownerId;
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
                Optional.empty(), false, Optional.empty());
    }

    /**
     * A foundation horse - no recorded parents, generation 0. {@code breedToken}
     * is a {@link BreedLineage} token ({@code null} / blank reads as
     * {@code "feral_mixed"}).
     */
    public static HorseRecord founder(UUID id, String firstName, String lastName, Genome genome,
                                      String breedToken) {
        return new HorseRecord(id, firstName, lastName, Optional.empty(),
                genome.genotypeCode(), genome.epigenomeCode(), breedToken(breedToken),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), 0, Optional.empty(),
                false, Optional.empty());
    }

    /** A foundation horse with no breed identity ({@code "feral_mixed"}). */
    public static HorseRecord founder(UUID id, String firstName, String lastName, Genome genome) {
        return founder(id, firstName, lastName, genome, BreedLineage.FERAL.toToken());
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
                Optional.empty(), false, Optional.empty());
    }

    /** A bred foal with no breed identity carried through - test / legacy convenience. */
    public static HorseRecord bred(UUID id, String firstName, String lastName, Genome genome,
                                   UUID motherId, UUID fatherId, int generation) {
        return bred(id, firstName, lastName, genome, BreedLineage.FERAL.toToken(),
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

    /** The horse's breed as a value object ({@code "feral_mixed"} when unset). */
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
                motherId, fatherId, tamedBy, bredBy, generation, parentStats, gelded, ownerId);
    }

    /**
     * The same horse with a recorded dam and sire. Breeding sets these when it
     * builds a foal's record; this is for the cases that make a horse without
     * going through breeding and still need it to have a pedigree - the test
     * yard's own horses, and an imported or spawned horse whose pedigree is known
     * to the caller but was never bred here.
     */
    public HorseRecord withParents(UUID mother, UUID father) {
        return new HorseRecord(id, firstName, lastName, barnName, geneticCode, epigenomeCode, breed,
                Optional.ofNullable(mother), Optional.ofNullable(father),
                tamedBy, bredBy, generation, parentStats, gelded, ownerId);
    }

    public HorseRecord withBreed(String breedToken) {
        return new HorseRecord(id, firstName, lastName, barnName, geneticCode, epigenomeCode,
                breedToken(breedToken), motherId, fatherId, tamedBy, bredBy, generation, parentStats, gelded, ownerId);
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
                ? HorseTraits.resolve(genotype(), epigenome(), true)
                : HorseTraits.resolve(genotype());
    }

    public HorseRecord withNames(String newFirst, String newLast) {
        return new HorseRecord(id, newFirst, newLast, barnName, geneticCode, epigenomeCode, breed,
                motherId, fatherId, tamedBy, bredBy, generation, parentStats, gelded, ownerId);
    }

    public HorseRecord withBarnName(Optional<String> newBarnName) {
        return new HorseRecord(id, firstName, lastName, newBarnName, geneticCode, epigenomeCode, breed,
                motherId, fatherId, tamedBy, bredBy, generation, parentStats, gelded, ownerId);
    }

    public HorseRecord withTamedBy(String username) {
        return new HorseRecord(id, firstName, lastName, barnName, geneticCode, epigenomeCode, breed,
                motherId, fatherId, Optional.of(username), bredBy, generation, parentStats, gelded, ownerId);
    }

    public HorseRecord withBredBy(String username) {
        return new HorseRecord(id, firstName, lastName, barnName, geneticCode, epigenomeCode, breed,
                motherId, fatherId, tamedBy, Optional.of(username), generation, parentStats, gelded, ownerId);
    }

    public HorseRecord withParentStats(ParentStats newParentStats) {
        return new HorseRecord(id, firstName, lastName, barnName, geneticCode, epigenomeCode, breed,
                motherId, fatherId, tamedBy, bredBy, generation, Optional.ofNullable(newParentStats), gelded,
                ownerId);
    }

    public boolean hasKnownParents() {
        return motherId.isPresent() || fatherId.isPresent();
    }

    // --- ownership ------------------------------------------------------

    /**
     * The same horse owned by {@code owner} ({@code null} for a horse that has
     * gone wild again). Mirrors vanilla's owner rather than replacing it: the
     * server decides who owns a horse, and this carries that answer to the
     * client, which cannot otherwise know it - see {@link #ownedBy}.
     */
    public HorseRecord withOwner(UUID owner) {
        return new HorseRecord(id, firstName, lastName, barnName, geneticCode, epigenomeCode, breed,
                motherId, fatherId, tamedBy, bredBy, generation, parentStats, gelded,
                Optional.ofNullable(owner));
    }

    /**
     * Whether {@code viewer} owns this horse.
     *
     * <p><b>This exists because vanilla's answer is server-only.</b>
     * {@code AbstractHorse} keeps its owner in a plain field written to NBT, and
     * defines exactly one synched value - the flags byte behind
     * {@code isTamed()}. So a client sees that a horse is tamed and never by
     * whom, and {@code getOwnerReference()} is always {@code null} there. Any
     * client-side "is this mine" test has to read this field instead; the server
     * still decides, and {@code HorseOwnership.isOwner} remains the authority
     * that actually refuses an action.
     */
    public boolean ownedBy(UUID viewer) {
        return viewer != null && ownerId.isPresent() && ownerId.get().equals(viewer);
    }

    /**
     * <b>Somebody else's horse</b> - the test the family tree greys a box on.
     *
     * <p>Deliberately <b>not</b> {@code !ownedBy(viewer)}: a wild horse, a
     * founder nobody ever tamed and an ancestor whose owner was never mirrored
     * are owned by no one, and reading those as another player's would grey out
     * most of a pedigree. Only a horse with an owner who is not the viewer
     * counts.
     */
    public boolean ownedByAnother(UUID viewer) {
        return viewer != null && ownerId.isPresent() && !ownerId.get().equals(viewer);
    }

    /**
     * <b>Sold on</b>: this horse is credited to {@code viewerName} - they bred
     * it, or tamed it - and somebody else owns it now.
     *
     * <p>Selling never removes a horse from anybody's pedigree, so this is only
     * ever about how the box is drawn. It is a narrower question than
     * {@link #ownedByAnother}, which is true of any horse another player owns:
     * a stallion you borrowed for one covering was never yours to sell.
     */
    public boolean soldOnBy(String viewerName, UUID viewer) {
        return ownedByAnother(viewer) && viewerName != null
                && attribution().map(viewerName::equals).orElse(false);
    }

    // --- gelding --------------------------------------------------------

    /**
     * Gelded with a vet's kit. Permanent, and a fact about the animal rather
     * than about its genes: the sex locus still reads {@code X/Y}, a jar filled
     * before still holds his genome, and the flag goes wherever the record goes
     * - death, a transfer paper, the family tree.
     */
    public HorseRecord withGelded(boolean value) {
        return new HorseRecord(id, firstName, lastName, barnName, geneticCode, epigenomeCode, breed,
                motherId, fatherId, tamedBy, bredBy, generation, parentStats, value, ownerId);
    }

    /**
     * An ungelded male - the only horse that covers a mare, fills a seed jar,
     * spars, guards a band or answers a mare in heat. A gelding is none of those.
     */
    public boolean entire() {
        return sex() == Sex.MALE && !gelded;
    }

    /** Stallion, colt, mare, filly - or gelding. */
    public String sexLabel(boolean adult) {
        if (gelded && sex() == Sex.MALE) {
            return adult ? "Gelding" : "Gelded colt";
        }
        return sex().label(adult);
    }
}
