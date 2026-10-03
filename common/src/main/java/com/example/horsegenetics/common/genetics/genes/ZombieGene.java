package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genotype;

/**
 * <b>Zombie</b> ({@code horsegenetics.zombie}) - two copies and the horse is dead
 * flesh: vanilla's zombie horse sheet is its base coat, stamped into the colour
 * field before the magical phase, so its green-grey replaces whatever the natural
 * genes made of it and any magical gene still paints over it - the Long-buried
 * strain's dust is grave dirt on a rotted hide (undead treatment D38, D41).
 *
 * <p>The sheet's own handful of holes - the eighty-odd texels vanilla knocks out
 * of its zombie - are cut at the very end, as the skeleton's are.
 */
public final class ZombieGene extends AbstractUndeathGene {

    public static final String KEY = "horsegenetics.zombie";

    /** Beside the skeleton locus. */
    public static final int PRIORITY = 192;

    public ZombieGene() {
        super(KEY, "Zombie", PRIORITY, "Zomb", "Zombie", "zombie",
                "One zombie copy. The horse is alive in every way that shows; the copy is "
                        + "there for its foals.",
                "Zombie",
                "Two zombie copies. The horse is dead flesh - the rotted green-grey hide of a "
                        + "zombie horse, whatever colour its other genes would have made it, though "
                        + "magical markings still show over it. It counts as undead.");
    }

    @Override
    protected SheetUse use(String sheet, Genotype genotype, Epigenome epigenome) {
        return new SheetUse(sheet, Pass.SEED, 1.0, 0.0);
    }
}
