package com.example.horsegenetics.common.repro;

import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.GenomeSample;

import java.util.Objects;
import java.util.UUID;

/**
 * <b>One foal that has not been born yet.</b> Its genome was drawn at
 * conception - the only moment both parents' genomes and any carrot bias were
 * in hand - so nothing about it changes between now and birth.
 *
 * <p>It carries everything birth needs without looking anyone up, because the
 * sire may be unloaded, sold or dead by then: his id, name and generation for
 * the record, and his whole {@link GenomeSample} for anything computed from his
 * body at birth (the parent-stats comparison). Owner's call, 2026-09-13.
 *
 * @param genome      the foal's own genotype and epigenome
 * @param breedToken  its breed label, already combined (and marked spliced) at conception
 * @param lostEarly   drawn as a lethal-at-conception genotype: this embryo will be lost
 *                    in the first third of the pregnancy
 * @param sireId      the sire's entity id - the pedigree edge
 * @param sireGeneration the sire's generation, for the foal's own
 * @param sire        the sire's heritable material
 * @param bredBy      the player who arranged the mating, or {@code ""}. Breeding credit
 *                    stays with them even if the mare is sold before birth.
 */
public record Embryo(GenomeSample genome, String breedToken, boolean lostEarly,
                     UUID sireId, String sireFirstName, String sireLastName, int sireGeneration,
                     GenomeSample sire, String bredBy) {

    public Embryo {
        Objects.requireNonNull(genome, "genome");
        Objects.requireNonNull(sireId, "sireId");
        Objects.requireNonNull(sire, "sire");
        breedToken = breedToken == null ? "" : breedToken;
        sireFirstName = sireFirstName == null ? "" : sireFirstName;
        sireLastName = sireLastName == null ? "" : sireLastName;
        bredBy = bredBy == null ? "" : bredBy;
    }

    /** The foal's live genome. */
    public Genome foal() {
        return genome.genome();
    }
}
