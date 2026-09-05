package com.example.horsegenetics.common.trait;

import java.util.ArrayList;
import java.util.List;

/**
 * The sink a gene pushes its non-coat contribution into. One of these is passed
 * down {@link com.example.horsegenetics.common.genetics.Genes#codeOrder()} by
 * {@link HorseTraits#resolve}, and every gene that implements
 * {@link TraitContribution} gets a turn at it.
 *
 * <h2>Order independence</h2>
 * A contribution is either <b>additive</b> ({@link #addSpeed} and friends) or a
 * <b>scale multiplier</b> ({@link #multiplyScale}), and {@link #build()}
 * applies every addition before every multiplication. Addition is associative
 * and so is multiplication, so the result does not depend on the order genes
 * are visited in - the same argument that keeps the coat's phase-3 accumulator
 * drift-free. Gene priority still decides the <i>code</i> order; it
 * deliberately buys nothing here, so nobody is tempted to encode a dependency
 * between two genes as a priority number.
 *
 * <h2>Why multipliers exist at all</h2>
 * Among the <b>natural</b> loci, only body scale multiplies, and only because
 * dwarfism is a <i>proportional</i> change: a pony with chondrodysplasia should
 * end up small twice over, not be dragged to the same absolute height as a
 * draught horse with it. The natural speed, health and jump loci are purely
 * additive, which keeps a gene's weight readable as "this allele is worth two
 * hearts" rather than "it depends".
 *
 * <p>The four <b>magical body-stat</b> genes are the exception, and they are
 * built to be one: each carries a per-copy percentage that has to <i>scale</i>
 * whatever the natural loci settled on, so a magically fast pony is still slower
 * than a magically fast racehorse. They go through {@link #multiplyScaleUnclamped}
 * and the three siblings added beside it, every one applied after all additions
 * and bounded only by the {@code MAGICAL_*} guards - the exact counterpart of
 * the coat's uncapped phase-3 accumulator.
 */
public final class TraitBuilder {

    private double speed = HorseTraits.BASE_SPEED;
    private double health = HorseTraits.BASE_HEALTH;
    private double jump = HorseTraits.BASE_JUMP;
    private double scale = HorseTraits.BASE_SCALE;
    private double scaleFactor = 1.0;
    private double magicalScaleFactor = 1.0;
    private double magicalSpeedFactor = 1.0;
    private double magicalHealthFactor = 1.0;
    private double magicalJumpFactor = 1.0;
    private final List<Condition> conditions = new ArrayList<>();

    /**
     * The breed's per-axis {@link TargetBand}s, or {@link BreedStatTargets#NONE}
     * for an Unknown / breedless horse. The four magical body-stat genes read
     * this through {@link #breedBand}: when a band is present they land the
     * horse inside it from its epigenetic seeds instead of taking their usual
     * bounded-Gaussian draw.
     */
    private final BreedStatTargets breedTargets;

    TraitBuilder() {
        this(BreedStatTargets.NONE);
    }

    TraitBuilder(BreedStatTargets breedTargets) {
        this.breedTargets = breedTargets == null ? BreedStatTargets.NONE : breedTargets;
    }

    /** The breed target band for {@code axis}, or {@code null} if this horse's breed does not pin it. */
    public TargetBand breedBand(StatAxis axis) {
        return breedTargets.band(axis);
    }

    /** Movement speed, in attribute units (a vanilla horse is 0.1125 - 0.3375). */
    public TraitBuilder addSpeed(double delta) {
        speed += delta;
        return this;
    }

    /** Max health, in health points - two per heart. */
    public TraitBuilder addHealth(double delta) {
        health += delta;
        return this;
    }

    /** Jump strength, in attribute units (a vanilla horse is 0.4 - 0.8). */
    public TraitBuilder addJump(double delta) {
        jump += delta;
        return this;
    }

    /** Body scale, added before any {@link #multiplyScale} - the height loci use this. */
    public TraitBuilder addScale(double delta) {
        scale += delta;
        return this;
    }

    /** Body scale, applied after every addition - dwarfism, which is proportional. */
    public TraitBuilder multiplyScale(double factor) {
        scaleFactor *= factor;
        return this;
    }

    /**
     * Body scale <b>outside the natural bounds</b> - the magical escape hatch,
     * and the exact counterpart of the coat's uncapped phase-3 accumulator.
     *
     * <p>{@link HorseTraits#MIN_SCALE} / {@link HorseTraits#MAX_SCALE} exist so
     * that no amount of stacking real size and dwarfism loci can produce a horse
     * that is not a horse. A magical gene is allowed to produce exactly that, so
     * its factor is applied <b>after</b> that clamp and is bounded only by
     * {@link HorseTraits#MAGICAL_MIN_SCALE} / {@link HorseTraits#MAGICAL_MAX_SCALE}
     * - ten times either way, which is a limit on absurdity rather than a limit
     * on size.
     *
     * <p>The two stages compose the way you would want: a magically enormous
     * pony is still smaller than a magically enormous draught horse, because the
     * natural loci settle the horse's own size first and the magic multiplies
     * whatever that turned out to be.
     */
    public TraitBuilder multiplyScaleUnclamped(double factor) {
        magicalScaleFactor *= factor;
        return this;
    }

    /**
     * Movement speed <b>outside the natural range</b> - the magical escape
     * hatch for speed, the exact shape of {@link #multiplyScaleUnclamped}. The
     * natural speed loci add; a magical one multiplies whatever they produced,
     * so it composes the way you would want and stays bounded only by
     * {@link HorseTraits#MAGICAL_MIN_FACTOR} / {@link HorseTraits#MAGICAL_MAX_FACTOR}.
     */
    public TraitBuilder multiplySpeedUnclamped(double factor) {
        magicalSpeedFactor *= factor;
        return this;
    }

    /** Max health outside the natural range - see {@link #multiplySpeedUnclamped}. */
    public TraitBuilder multiplyHealthUnclamped(double factor) {
        magicalHealthFactor *= factor;
        return this;
    }

    /** Jump strength outside the natural range - see {@link #multiplySpeedUnclamped}. */
    public TraitBuilder multiplyJumpUnclamped(double factor) {
        magicalJumpFactor *= factor;
        return this;
    }

    /** Report a disorder this horse expresses. Duplicates are ignored. */
    public TraitBuilder condition(Condition condition) {
        if (!conditions.contains(condition)) {
            conditions.add(condition);
        }
        return this;
    }

    /**
     * Clamp and freeze. <b>Health never resolves to zero</b> - it bottoms out
     * at {@link HorseTraits#MIN_HEALTH}, because a zero max-health attribute is
     * not "a very sick horse", it is a crash; killing a horse is the damage
     * path's job, not the attribute's.
     */
    Traits build() {
        double natural = Math.min(HorseTraits.MAX_SCALE,
                Math.max(HorseTraits.MIN_SCALE, scale * scaleFactor));
        double magical = Math.min(HorseTraits.MAGICAL_MAX_SCALE,
                Math.max(HorseTraits.MAGICAL_MIN_SCALE, natural * magicalScaleFactor));
        return new Traits(
                Math.max(HorseTraits.MIN_SPEED, speed * magicFactor(magicalSpeedFactor)),
                Math.max(HorseTraits.MIN_HEALTH, health * magicFactor(magicalHealthFactor)),
                Math.max(HorseTraits.MIN_JUMP, jump * magicFactor(magicalJumpFactor)),
                magical,
                conditions);
    }

    /**
     * A magical body-stat multiplier, clamped to the {@code MAGICAL_*_FACTOR}
     * guard. The bounded Gaussian on the genes that call it keeps the real
     * range near {@code 2x}, so this only ever catches a genuinely broken
     * accumulation.
     */
    private static double magicFactor(double factor) {
        return Math.min(HorseTraits.MAGICAL_MAX_FACTOR,
                Math.max(HorseTraits.MAGICAL_MIN_FACTOR, factor));
    }
}
