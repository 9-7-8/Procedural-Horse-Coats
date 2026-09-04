package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.TraitBuilder;

/**
 * <b>B4GALT7</b> ({@code horsegenetics.b4galt7}) - <b>Friesian dwarfism</b>, and
 * the one disorder in the mod a horse lives a full life with.
 *
 * <p>An affected horse is a normal-headed animal on short, splayed limbs with a
 * weak ribcage: the growth defect hits the long bones and the chest and leaves
 * the skull almost alone. The mod cannot draw that yet - {@code Attributes.SCALE}
 * is one number for the whole entity, and per-part scaling would mean owning the
 * horse model rather than borrowing the vanilla one - so it renders as an
 * <b>overall</b> three-quarter-size horse, with the ribcage weakness paid for in
 * hearts. See {@code wiki/roadmap.html} 4.4 for the per-part follow-up.
 *
 * <p><b>Not lethal.</b> Affected foals survive, which is what makes this the one
 * place in the model where a player ends up with a live animal and a decision to
 * make about a carrier line, rather than a corpse and a chat message.
 */
public final class B4galt7Gene extends RecessiveDisorderGene {

    public static final String KEY = "horsegenetics.b4galt7";
    public static final int PRIORITY = 87;

    /** How many wild horses in a hundred carry one copy. */
    public static final double WILD_CARRIER_PERCENT = 4.0;

    public static final Condition DWARFISM = Condition.impairing(
            "friesian-dwarfism", "Friesian dwarfism",
            "Short, splayed limbs and a weak ribcage on a normal-sized head. The horse lives, "
                    + "but it is small, slow, jumps badly and has noticeably fewer hearts.");

    public B4galt7Gene() {
        super(KEY, "B4GALT7", PRIORITY,
                "d", "Dwarfism (d)", "N", "Wild-type (N)",
                WILD_CARRIER_PERCENT, DWARFISM);
    }

    @Override
    protected void affect(TraitBuilder out) {
        out.multiplyScale(0.75)
                .addHealth(-5.0)
                .addSpeed(-0.020)
                .addJump(-0.10);
    }
}
