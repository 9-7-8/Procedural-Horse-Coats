package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.ColorField;
import com.example.horsegenetics.common.coat.pattern.ColorView;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.pattern.PigmentView;

import java.util.Objects;

/**
 * <b>One outcome a {@link Gene} can produce</b> - what a horse actually looks
 * like when it carries a particular combination of that gene's alleles, and the
 * paint function that produces it.
 *
 * <h2>Why this replaces "dominant" and "recessive"</h2>
 * A gene has alleles. For every <i>unordered</i> combination of two of them
 * ({@code n(n+1)/2} of them for {@code n} alleles) the horse gets some result.
 * With two alleles that is three combinations - {@code EE}, {@code Ee},
 * {@code ee} - and the classical vocabulary is a shorthand for which of them
 * happen to land on the same result. Add a third allele and it is six, and the
 * shorthand stops describing anything: {@code E}/{@code e}/{@code x} has no
 * single "dominant" allele, it has a table.
 *
 * <p>So there is no dominance property anywhere in this model. A gene declares
 * its distinct outcomes as {@code Expression} constants and one function
 * ({@link Gene#expressionOf}) mapping any pair to one of them. Several pairs
 * mapping to the same outcome <i>is</i> what "dominant" used to mean; only the
 * double-variant pair mapping to a non-{@link #wildType()} outcome <i>is</i>
 * what "recessive" used to mean; two variant alleles each with their own
 * outcome and a third for the pair of them <i>is</i> codominance. None of them
 * needs a name, and none of them needs a special case.
 *
 * <h2>Wild type</h2>
 * {@link #wildType()} marks a combination that <b>changes nothing about the
 * horse</b>. It has no painter, contributes nothing to the coat, is skipped by
 * the composer, is excluded from a horse's texture key, and reads as "absent"
 * in the genome display. It is a property of the <i>combination</i>, not of an
 * allele: on a gene where only {@code ee}, {@code ex} and {@code xx} do
 * anything, {@code EE}, {@code Ee} and {@code Ex} are all wild type.
 *
 * <h2>Masking</h2>
 * {@link #masks()} marks a combination that hides every other gene's
 * contribution - dominant white's {@code W_}, the diagnostic test overlay. Also
 * per-combination: {@code w/w} does not mask, {@code W/w} does.
 *
 * <h2>Determinism</h2>
 * {@link #deterministic()} is "every horse with this combination is painted
 * byte-for-byte identically". A {@code false} anywhere forces per-horse texture
 * generation - see {@code wiki/philosophy.html}'s determinism contract, which
 * this does not weaken: a non-deterministic expression still draws every number
 * it needs from the expressing allele copy's epigenetic seed, so the same horse
 * always regenerates the same coat.
 *
 * <h2>The paint function</h2>
 * An expression carries <b>either</b> a {@link Pigment} painter (phase 1, for a
 * gene on the natural pigment layer) <b>or</b> a {@link Colour} painter (phase
 * 3, for a gene on the magical RGB layer) - never both, because a gene is
 * natural or magical and never both. Both are pure: handed read-only views of
 * the coat so far, they <i>return</i> a contribution rather than drawing into
 * shared state.
 *
 * <h2>Identity</h2>
 * Two expressions are equal when their {@link #id()}s match. The id is the
 * gene's own slug for the outcome ({@code "sabino-white"}), unique within the
 * gene, and stable - the gallery dedups allele pairs by it, the gene dictionary
 * and the wiki key their tables on it.
 */
public final class Expression {

    /** A phase-1 paint function: push pigment down, given the coat so far. */
    @FunctionalInterface
    public interface Pigment {
        /**
         * Take {@code coat}{@link PigmentView#mutableCopy() .mutableCopy()},
         * paint into that, and return it. Returning {@code null} means "no
         * contribution after all". <b>Never write through {@code coat}</b> - it
         * is the previous gene's output and the next gene's input.
         */
        PigmentField restrict(CoatBuildContext ctx, PigmentView coat);
    }

    /** A phase-3 paint function: add signed RGB over the resolved coat. */
    @FunctionalInterface
    public interface Colour {
        /**
         * Return a delta ({@link ColorField#deltaLike(ColorView)} filled with
         * {@link ColorField#add}), or {@code null}. {@code coat} is the
         * resolved natural pigment, so an expression can <i>find</i> the black
         * or the white areas before painting them; {@code colour} is what the
         * magical genes before it accumulated.
         */
        ColorField tint(CoatBuildContext ctx, PigmentView coat, ColorView colour);
    }

    private final String id;
    private final String name;
    private final String description;
    private final boolean wildType;
    private final boolean masks;
    private final boolean deterministic;
    private final Pigment pigment;
    private final Colour colour;

    private Expression(String id, String name, String description, boolean wildType, boolean masks,
                       boolean deterministic, Pigment pigment, Colour colour) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.description = Objects.requireNonNull(description, "description");
        this.wildType = wildType;
        this.masks = masks;
        this.deterministic = deterministic;
        this.pigment = pigment;
        this.colour = colour;
    }

    // ------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------

    /**
     * The outcome for a combination that <b>changes nothing</b>. A gene needs
     * at least one; most have exactly one, but a multi-allele gene may want
     * several if different silent combinations deserve different wording.
     */
    public static Expression wildType(String id, String name, String description) {
        return new Expression(id, name, description, true, false, true, null, null);
    }

    /** {@link #wildType(String, String, String)} under the conventional id {@code "wild"}. */
    public static Expression wildType(String description) {
        return wildType("wild", "Wild type", description);
    }

    /**
     * Start describing a visible outcome. Finish with {@link Builder#restrict}
     * (a natural gene) or {@link Builder#tint} (a magical one).
     */
    public static Builder of(String id, String name) {
        return new Builder(id, name);
    }

    /** Fluent construction - see {@link Expression#of}. */
    public static final class Builder {

        private final String id;
        private final String name;
        private String description = "";
        private boolean masks;
        private boolean deterministic = true;

        private Builder(String id, String name) {
            this.id = id;
            this.name = name;
        }

        /** The human-readable sentence the gene dictionary, tooltips and the wiki show. */
        public Builder describe(String text) {
            this.description = text;
            return this;
        }

        /** This outcome varies per horse (drawn from the expressing copy's epigenetic seed). */
        public Builder varies() {
            this.deterministic = false;
            return this;
        }

        /** While this outcome shows, no other gene is visible (dominant white, the test overlay). */
        public Builder masking() {
            this.masks = true;
            return this;
        }

        /** Finish a <b>natural</b> gene's outcome: it restricts red / black pigment in phase 1. */
        public Expression restrict(Pigment painter) {
            return new Expression(id, name, description, false, masks, deterministic,
                    Objects.requireNonNull(painter, "painter"), null);
        }

        /** Finish a <b>magical</b> gene's outcome: it adds signed RGB in phase 3. */
        public Expression tint(Colour painter) {
            return new Expression(id, name, description, false, masks, deterministic,
                    null, Objects.requireNonNull(painter, "painter"));
        }
    }

    // ------------------------------------------------------------------
    // Access
    // ------------------------------------------------------------------

    /** The gene's slug for this outcome - unique within the gene, stable, used for identity. */
    public String id() {
        return id;
    }

    /** Short display name, e.g. {@code "Sabino-white"}. */
    public String name() {
        return name;
    }

    /** One human-readable sentence: what this combination does to the horse. */
    public String description() {
        return description;
    }

    /** Does this combination change nothing about the horse? */
    public boolean wildType() {
        return wildType;
    }

    /** Does this combination hide every other gene's contribution? */
    public boolean masks() {
        return masks;
    }

    /** Is every horse with this combination painted identically? */
    public boolean deterministic() {
        return deterministic;
    }

    /** Does this outcome paint in phase 1 (natural) rather than phase 3 (magical)? */
    public boolean isNatural() {
        return pigment != null;
    }

    /** Phase 1. {@code null} when this outcome does not paint pigment. */
    public PigmentField restrict(CoatBuildContext ctx, PigmentView coat) {
        return pigment == null ? null : pigment.restrict(ctx, coat);
    }

    /** Phase 3. {@code null} when this outcome does not paint colour. */
    public ColorField tint(CoatBuildContext ctx, PigmentView coat, ColorView accumulated) {
        return colour == null ? null : colour.tint(ctx, coat, accumulated);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Expression e && e.id.equals(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }
}
