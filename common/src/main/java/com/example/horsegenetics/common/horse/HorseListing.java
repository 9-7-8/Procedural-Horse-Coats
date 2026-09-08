package com.example.horsegenetics.common.horse;

import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.GeneCategory;
import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.common.trait.Traits;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * <b>One horse as a row of a table</b> - the flattened, already-answered form of
 * a horse that the browser's <i>My horses</i> tab sorts and filters over.
 *
 * <h2>Why a second horse type</h2>
 * A {@link HorseRecord} is the durable pedigree fact and knows nothing about the
 * live entity; the entity knows nothing about the pedigree; and a {@link Traits}
 * resolve costs a walk of every gene. A table redraws every frame and a sort
 * compares thousands of times, so a row that re-derived any of that per
 * comparison would be a frame-rate bug waiting for a big stable. Everything a
 * column or a filter can ask about is therefore <b>resolved once</b>, in
 * {@link #of}, and read as a field afterwards.
 *
 * <p>The genotype is kept, because {@link HorseQuery} filters on genes and there
 * is no flattening of "carries {@code SB1}" that is not just the genotype.
 *
 * <h2>Fields that only a loaded entity can answer</h2>
 * Bond, herd membership, foal-or-adult and whereabouts come off the entity, and
 * the entity may be in an unloaded chunk on the far side of the world. Those
 * fields carry a defined "not known right now" value - {@code loaded == false},
 * bond {@code -1} - rather than a plausible-looking zero, so the table can say
 * <i>unknown</i> instead of <i>none</i>. {@link #adult} defaults to true for an
 * unloaded horse: a foal that has been out of sight for a while has almost
 * certainly grown up.
 *
 * <p>Pure Java on purpose - it is the model the tab draws, and the sorting and
 * filtering over it ({@link HorseQuery}) is unit-tested without a game.
 */
public record HorseListing(
        UUID id,
        String firstName,
        String lastName,
        String barnName,
        String breed,
        int generation,
        Sex sex,
        boolean adult,
        boolean tamed,
        double speed,
        double health,
        double jump,
        double scale,
        int bond,
        boolean inHerd,
        boolean loaded,
        String where,
        String tamedBy,
        String bredBy,
        boolean hasParents,
        String coat,
        List<String> conditions,
        boolean lethal,
        Genotype genotype) {

    /** Bond is unknown until the entity is loaded; this is that, not "no bond". */
    public static final int BOND_UNKNOWN = -1;

    public HorseListing {
        firstName = firstName == null ? "" : firstName;
        lastName = lastName == null ? "" : lastName;
        barnName = barnName == null ? "" : barnName;
        breed = breed == null ? "" : breed;
        where = where == null ? "" : where;
        tamedBy = tamedBy == null ? "" : tamedBy;
        bredBy = bredBy == null ? "" : bredBy;
        coat = coat == null ? "" : coat;
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
    }

    /**
     * Build a row, resolving the coat description, the four body numbers and the
     * disorder list from {@code genotype} once. Health genetics are resolved
     * <b>on</b> regardless of the server setting, for the same reason the
     * information screen does it: carrying a lethal is a fact about the alleles,
     * and a breeder filtering for it needs the answer either way.
     */
    public static HorseListing of(UUID id, String firstName, String lastName, String barnName,
                                  String breed, int generation, Genotype genotype,
                                  boolean adult, boolean tamed, int bond, boolean inHerd,
                                  boolean loaded, String where,
                                  String tamedBy, String bredBy, boolean hasParents) {
        Traits traits;
        try {
            traits = HorseTraits.resolve(genotype, null, true);
        } catch (RuntimeException unresolvable) {
            traits = HorseTraits.baseline();
        }
        List<String> conditions = new ArrayList<>();
        boolean lethal = false;
        for (Condition condition : traits.conditions()) {
            conditions.add(condition.name());
            lethal |= condition.severity().lethal();
        }
        return new HorseListing(id, firstName, lastName, barnName, breed, generation,
                genotype.sex(), adult, tamed,
                traits.speed(), traits.health(), traits.jump(), traits.scale(),
                bond, inHerd, loaded, where, tamedBy, bredBy, hasParents,
                describeCoat(genotype), conditions, lethal, genotype);
    }

    /** {@code "Amber Duskrunner"}, the way the horse is written down. */
    public String displayName() {
        return (firstName + " " + lastName).trim();
    }

    /** Mare / stallion / filly / colt - the word a table column wants. */
    public String sexLabel() {
        return sex.label(adult);
    }

    public String ageLabel() {
        return adult ? "adult" : "foal";
    }

    /** Everything a bare filter word is matched against, lower-cased, once. */
    public String haystack() {
        StringBuilder sb = new StringBuilder();
        sb.append(firstName).append(' ').append(lastName).append(' ').append(barnName)
                .append(' ').append(breed).append(' ').append(coat)
                .append(' ').append(tamedBy).append(' ').append(bredBy)
                .append(' ').append(where).append(' ').append(sexLabel());
        for (String condition : conditions) {
            sb.append(' ').append(condition);
        }
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    /**
     * <b>What colour this horse is, in words.</b> The base coat comes off
     * extension and agouti - the two loci that decide whether there is black
     * pigment and whether it is restricted - and every other coat gene that is
     * doing something adds its outcome after it.
     *
     * <p>Deliberately not a lookup table of genotypes: a data-driven pattern
     * gene dropped in by the gene creator names itself here for free, because
     * the string is built out of the {@link Expression}s the genes declare.
     */
    public static String describeCoat(Genotype genotype) {
        StringBuilder sb = new StringBuilder(baseCoat(genotype));
        for (Gene gene : Genes.codeOrder()) {
            if (gene == Genes.EXTENSION || gene == Genes.AGOUTI
                    || GeneCategory.of(gene) != GeneCategory.COAT) {
                continue;
            }
            Expression expression = genotype.expressionOf(gene);
            if (expression == null || expression.wildType()) {
                continue;
            }
            sb.append(", ").append(expression.name().toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }

    /**
     * Black, one of the four bays, or chestnut. Agouti reports its own bay band
     * ({@code BayShade} reads the shade locus to pick between them), so the
     * expression name is already the right word and nothing is re-derived here.
     */
    private static String baseCoat(Genotype genotype) {
        if (!Genes.EXTENSION.producesBlack(genotype.pair(Genes.EXTENSION))) {
            return "Chestnut";
        }
        Expression agouti = genotype.expressionOf(Genes.AGOUTI);
        return agouti == null || agouti.wildType() ? "Black" : agouti.name();
    }
}
