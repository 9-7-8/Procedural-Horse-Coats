package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.horse.Sex;

/**
 * <b>Which chromosome a gene sits on</b> - the one declaration that turns
 * ordinary Mendelian segregation into sex-linked segregation.
 *
 * <p>Everything else in the model is autosomal and always was; this exists
 * because {@link com.example.horsegenetics.common.genetics.genes.BrindleGene
 * brindle} is genuinely {@code X}-linked and the asymmetry <i>is</i> the
 * appeal: a stallion is never a carrier, he passes the locus to every daughter
 * and no son, and a trait runs visibly down one side of a pedigree.
 *
 * <h2>The reserved slot</h2>
 * A horse's genotype is one {@link AllelePair} per gene, two slots, always -
 * and that does not change here. A hemizygous horse fills the slot it does not
 * really have with a <b>reserved placeholder allele</b> the gene declares:
 *
 * <table>
 *   <tr><th>mode</th><th>mare {@code X/X}</th><th>stallion {@code X/Y}</th></tr>
 *   <tr><td>{@link #AUTOSOMAL}</td><td>two real alleles</td><td>two real alleles</td></tr>
 *   <tr><td>{@link #X_LINKED}</td><td>two real alleles</td><td>one real allele + reserved {@code Y}</td></tr>
 *   <tr><td>{@link #Y_LINKED}</td><td>two reserved {@code X}</td><td>reserved {@code X} + one real allele</td></tr>
 * </table>
 *
 * <p>That is what keeps this change small. {@link Genotype} is unchanged,
 * {@link Epigenome} alignment is unchanged (the reserved slot simply carries
 * epigenetics nothing reads), the code string is unchanged in shape, and
 * parsing needs no special case because the placeholder is a declared allele of
 * the gene like any other.
 *
 * <h2>Where the placeholder has to sit in {@code alleles()}</h2>
 * {@link AllelePair} canonicalises on {@link Allele#order()}, which is a
 * gene's declaration index. So the placeholder must be declared where it will
 * land in the slot it stands for:
 * <ul>
 *   <li><b>{@code X}-linked</b> - declare the reserved {@code Y} <b>last</b>, so
 *       a stallion reads {@code Brn/Y} with his real allele first;</li>
 *   <li><b>{@code Y}-linked</b> - declare the reserved {@code X} <b>first</b>.</li>
 * </ul>
 * {@link Genes#register} checks this and refuses a gene that gets it wrong,
 * because the failure mode otherwise is a silently mis-sorted pair rather than
 * an error.
 */
public enum Inheritance {

    /** On an ordinary chromosome. Every gene in the mod but brindle. */
    AUTOSOMAL(null),

    /**
     * On the {@code X}. A mare carries two copies and a stallion one, so a
     * recessive allele shows in <b>every</b> stallion that carries it and only
     * in a homozygous mare.
     */
    X_LINKED("Y"),

    /**
     * On the {@code Y}. Only stallions carry it at all, and every son of a
     * carrier stallion is a carrier - the locus travels down the male line
     * untouched by any mare.
     */
    Y_LINKED("X");

    private final String placeholderToken;

    Inheritance(String placeholderToken) {
        this.placeholderToken = placeholderToken;
    }

    /**
     * The token of the reserved allele a hemizygous horse's spare slot holds, or
     * {@code null} for {@link #AUTOSOMAL}. {@code "Y"} on an {@code X}-linked
     * gene means "this locus is not on the Y".
     */
    public String placeholderToken() {
        return placeholderToken;
    }

    public boolean sexLinked() {
        return this != AUTOSOMAL;
    }

    /** How many <b>real</b> alleles a horse of this sex carries at such a locus: 2, 1 or 0. */
    public int copiesIn(Sex sex) {
        return switch (this) {
            case AUTOSOMAL -> 2;
            case X_LINKED -> sex == Sex.FEMALE ? 2 : 1;
            case Y_LINKED -> sex == Sex.FEMALE ? 0 : 1;
        };
    }

    /** Does a horse of this sex carry exactly one real allele here? */
    public boolean hemizygousIn(Sex sex) {
        return copiesIn(sex) == 1;
    }

    /**
     * The display prefix for a hemizygous horse's single allele - {@code "X-"}
     * on an {@code X}-linked gene, {@code "Y-"} on a {@code Y}-linked one, so a
     * stallion's brindle locus reads {@code X-Brn} rather than as a fake
     * homozygote. Unambiguous on sight because {@code -} is the <i>gene</i>
     * separator in a code string and can never appear inside an allele token.
     */
    public String displayPrefix() {
        return switch (this) {
            case AUTOSOMAL -> "";
            case X_LINKED -> "X-";
            case Y_LINKED -> "Y-";
        };
    }
}
