package com.example.horsegenetics.common.horse;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * What a <b>signed transfer paper</b> records: one named horse, and who wrote
 * the paper out. Redeeming it moves that horse's ownership to whoever is
 * holding the paper when they right-click the animal.
 *
 * <p>The paper is a <b>bearer instrument</b>. Signing does not move the horse
 * and buying the paper does not move the horse; the only thing that does is
 * walking up to the animal named on it. That is what makes a paper worth
 * trading between players - it is a claim on a specific horse that its holder
 * has not collected yet.
 *
 * <p>Everything past {@link #horseId} is a <b>snapshot for display</b>, taken
 * when the paper was signed. The horse itself is the source of truth and may
 * have been renamed, re-bred or killed since; the paper is only allowed to
 * decide <i>which</i> horse, never what that horse is. In particular
 * {@link #bredBy} is shown on the paper so a buyer can see whose line the
 * animal comes from, and is <b>never written back</b> on redemption - a
 * transfer changes who owns a horse, not who bred it.
 *
 * <h2>Why it carries the horse's genome</h2>
 * A paper does not draw as a paper. It draws as a <b>small model of the horse
 * it names, in that horse's own coat</b> ({@code client/TransferDeedRenderer}),
 * which is the whole point: a cowboy's stall is six papers that look alike, and
 * a buyer walking out to collect one has to be able to match the paper in his
 * hand to an animal in a field. So the deed snapshots
 * {@link #geneticCode} and {@link #epigenomeCode} - between them everything the
 * coat pipeline needs - and the client can paint the horse without the animal
 * being loaded, or even alive.
 *
 * <p>A snapshot like the rest, and with the same rule: it decides what the
 * paper <i>shows</i>, never what the horse <i>is</i>. Nothing is ever written
 * back from here.
 *
 * <p>Layer-1: no game dependency. The item component codec that carries this on
 * an {@code ItemStack} lives in the NeoForge module next to the other item
 * component types, the same way {@code HorseRecordCodecs} sits beside
 * {@link HorseRecord}.
 */
public record TransferDeed(
        UUID horseId,
        String horseName,
        Optional<String> breed,
        Optional<String> bredBy,
        String issuedBy,
        String geneticCode,
        String epigenomeCode) {

    public TransferDeed {
        Objects.requireNonNull(horseId, "horseId");
        Objects.requireNonNull(horseName, "horseName");
        Objects.requireNonNull(issuedBy, "issuedBy");
        Objects.requireNonNull(geneticCode, "geneticCode");
        Objects.requireNonNull(epigenomeCode, "epigenomeCode");
        breed = breed == null ? Optional.empty() : breed;
        bredBy = bredBy == null ? Optional.empty() : bredBy;
    }

    /**
     * The deed a paper gets when {@code issuer} signs it against {@code horse}.
     * Everything but the issuer is copied off the horse's own record.
     */
    public static TransferDeed forHorse(HorseRecord horse, String issuer) {
        return new TransferDeed(horse.id(), horse.displayName(), horse.breed(), horse.bredBy(),
                issuer, horse.geneticCode(), horse.epigenomeCode());
    }

    /** The coat the paper draws: the horse as it was when the paper was signed. */
    public com.example.horsegenetics.common.genetics.Genome genome() {
        return com.example.horsegenetics.common.genetics.Genome.parse(geneticCode, epigenomeCode);
    }

    /** Does this deed name {@code horse}? The one question redemption asks. */
    public boolean names(HorseRecord horse) {
        return horseId.equals(horse.id());
    }

    /** The horse's breed label as a value object ({@code "feral_mixed"} when unset). */
    public com.example.horsegenetics.common.breed.BreedLineage lineage() {
        return com.example.horsegenetics.common.breed.BreedLineage.parse(breed.orElse(null));
    }
}
