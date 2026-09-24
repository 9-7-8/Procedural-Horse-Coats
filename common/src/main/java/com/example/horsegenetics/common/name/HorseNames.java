package com.example.horsegenetics.common.name;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.name.HorseNameGenerator.NameParts;

/**
 * Name combination for breeding. Pure Layer-1 logic.
 *
 * <p>A foal takes <b>one half of its name from a parent and rolls the other</b>
 * - which half, and from which parent, is the breeder's own
 * {@link NamingPolicy}. By default the surname runs down the same-sex line (a
 * filly carries her dam's last name, a colt his sire's) and the first name is
 * rolled fresh.
 *
 * <h2>Why the half is always rolled</h2>
 * This replaced a schedule that varied the name by how many foals a pairing had
 * already produced: foal 1 took the dam's first and the sire's last, foal 2 the
 * other combination, foals 3-6 one parent half plus a rolled word, and foal 7
 * onward a wholly rolled name. It read well for one pair, and the owner's
 * server showed what it does to a herd - the first two foals of every pairing
 * are built only out of words already in use, so names collided constantly and
 * a surname meant nothing. Rolling one half every time gives the alpha table's
 * several hundred words to separate two foals of the same parents, while the
 * inherited half is the thing that actually carries a family.
 */
public final class HorseNames {

    /**
     * The name for a newborn foal.
     *
     * @param dam       the dam's name halves
     * @param sire      the sire's name halves
     * @param foalSex   the foal's own sex, from the Mendelian draw - it decides
     *                  the parent under {@link NamingPolicy.ParentSource#BY_SEX}
     * @param policy    the breeder's setting; {@link NamingPolicy#DEFAULT} for a
     *                  wild birth or a player who has not chosen
     * @param generator the word tables the rolled half comes from
     */
    public static NameParts foal(NameParts dam, NameParts sire, Sex foalSex,
                                 NamingPolicy policy, HorseNameGenerator generator, Rng rng) {
        NameParts parent = policy.inheritsFromDam(foalSex) ? dam : sire;
        NameParts rolled = generator.generateParts(rng);
        return policy.half() == NamingPolicy.InheritedHalf.LAST
                ? new NameParts(rolled.first(), parent.last())
                : new NameParts(parent.first(), rolled.last());
    }

    private HorseNames() {
    }
}
