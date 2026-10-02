package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.CoatSheetContribution;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneRarity;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.eye.EyeHue;
import com.example.horsegenetics.common.genetics.eye.EyeLocus;
import com.example.horsegenetics.common.genetics.eye.EyeRequest;
import com.example.horsegenetics.common.genetics.eye.EyeRequestContribution;
import com.example.horsegenetics.common.genetics.eye.EyeSector;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <b>An undeath locus</b> - the shape the skeleton and zombie genes share. Undeath
 * is not an entity type in this mod: a skeleton horse is a horse homozygous for
 * the skeleton allele, of whatever breed (owner's design, wiki/undead-horses.html).
 *
 * <ul>
 *   <li><b>Recessive.</b> Two copies make an undead horse; one is a carrier that
 *       looks and acts like any other horse, and is not undead for anything that
 *       asks (undead treatment D6).</li>
 *   <li><b>Not in the wild.</b> No feral founder rolls a copy. Only a breed that
 *       pins it carries it - the two undead breeds, which a vanilla undead horse
 *       converts into (D3b).</li>
 *   <li><b>A sheet, not paint.</b> The look is vanilla's own undead horse sheet,
 *       laid into the bake by {@link CoatSheetContribution}: the expression is a
 *       marker, so the composer's painting loops skip it and the sheet pass runs.</li>
 *   <li><b>Black eyes.</b> An undead horse's irises and sclerae are vanilla's flat
 *       black, requested at birth like any other eye colour a gene asks for
 *       ({@link EyeRequestContribution}). The undeath loci sit after every white
 *       locus in code order, so a skeleton carrying a white that would ask for a
 *       blue eye still has a black one.</li>
 * </ul>
 */
public abstract class AbstractUndeathGene implements Gene, CoatSheetContribution, EyeRequestContribution {

    private final String key;
    private final String name;
    private final int priority;
    private final String sheetStem;
    public final Allele undead;
    public final Allele n;
    private final List<Allele> alleles;
    private final Expression wild;
    private final Expression carrier;
    private final Expression expressed;
    private final List<Expression> expressions;
    private final FounderTable founders;

    protected AbstractUndeathGene(String key, String name, int priority, String token, String label,
                                  String sheetStem, String carrierText, String expressedName,
                                  String expressedText) {
        this.key = key;
        this.name = name;
        this.priority = priority;
        this.sheetStem = sheetStem;
        this.undead = new Allele(key, 0, token, label + " (" + token + ")");
        this.n = new Allele(key, 1, "n", "Wild-type (n)");
        this.alleles = List.of(undead, n);
        this.wild = Expression.wildType("A living horse.");
        this.carrier = Expression.wildType("carrier", name + " carrier", carrierText);
        this.expressed = Expression.of(sheetStem, expressedName).describe(expressedText).marker();
        this.expressions = List.of(wild, carrier, expressed);
        this.founders = FounderTable.always(n, n);
    }

    @Override public final String key() { return key; }
    @Override public final String name() { return name; }
    @Override public final int priority() { return priority; }
    @Override public final boolean isNatural() { return false; }
    @Override public GeneRarity rarity() { return GeneRarity.RARE; }
    @Override public final List<Allele> alleles() { return alleles; }
    @Override public final Allele defaultAllele() { return n; }
    @Override public final List<Expression> expressions() { return expressions; }
    @Override public final FounderTable founderTable(FounderContext context) { return founders; }

    /** No gene carrot: undeath is bred, or converted into - never fed. */
    @Override public boolean hasGeneCarrot() { return false; }

    /** Never spliced: there is no undead horse in the wild to have taken the copy from. */
    @Override public boolean spliceable() { return false; }

    /** Two copies - the only combination that is undead. */
    public final boolean expresses(AllelePair pair) {
        return pair.homozygousFor(undead);
    }

    /** Is this horse undead at this locus? */
    public final boolean expresses(Genotype genotype) {
        return genotype != null && expresses(genotype.pair(this));
    }

    @Override
    public final Expression expressionOf(AllelePair pair) {
        if (expresses(pair)) {
            return expressed;
        }
        return pair.has(undead) ? carrier : wild;
    }

    /** The sheet key for a skin: {@code skeleton_adult}, {@code zombie_baby}. */
    public final String sheetKey(Skin skin) {
        return sheetStem + (skin == Skin.BABY ? "_baby" : "_adult");
    }

    @Override
    public final Map<String, String> sheetResources() {
        Map<String, String> out = new LinkedHashMap<>();
        out.put(sheetKey(Skin.ADULT), "textures/entity/horse/horse_" + sheetStem + ".png");
        out.put(sheetKey(Skin.BABY), "textures/entity/horse/horse_" + sheetStem + "_baby.png");
        return out;
    }

    @Override
    public final Optional<SheetUse> sheetUse(AllelePair pair, Genotype genotype, Epigenome epigenome, Skin skin) {
        if (!expresses(pair)) {
            return Optional.empty();
        }
        return Optional.of(use(sheetKey(skin), genotype, epigenome));
    }

    @Override
    public final EyeRequest requestEyes(AllelePair pair, Genotype genotype, Epigenome epigenome) {
        if (!expresses(pair)) {
            return EyeRequest.none();
        }
        return EyeRequest.none().bothIrises(EyeHue.BLACK)
                .sclera(EyeLocus.EyeSideRef.RIGHT, EyeHue.BLACK)
                .sclera(EyeLocus.EyeSideRef.LEFT, EyeHue.BLACK)
                // No wedge of another colour either: a white's heterochromia sector
                // would otherwise put a blue slice in a dead horse's eye.
                .with(EyeLocus.sector(EyeLocus.EyeSideRef.RIGHT), EyeSector.WILD.token())
                .with(EyeLocus.sector(EyeLocus.EyeSideRef.LEFT), EyeSector.WILD.token());
    }

    /** This gene's pass and its numbers for one horse. */
    protected abstract SheetUse use(String sheet, Genotype genotype, Epigenome epigenome);
}
