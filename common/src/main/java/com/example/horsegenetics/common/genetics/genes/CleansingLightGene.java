package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;

import java.util.List;

/**
 * <b>Cleansing light</b> ({@code horsegenetics.cleansing_light}) - an aura that
 * burns the undead.
 *
 * <h2>The lost loot is a decision, not a defect</h2>
 * A mob killed by the horse's aura was not killed by the player, so there is no
 * experience and none of the drops that only fall to a player kill. This was
 * raised in design and <b>accepted deliberately</b>: the gene is protection, and
 * crediting the rider was rejected as making it straightforwardly better than
 * fighting. It draws a real line between the horse that keeps you alive and the
 * horse that makes you rich. Recorded here so a later session does not "fix" it.
 *
 * <h2>Why the beat is slow</h2>
 * Below about a second the target's hurt-immunity frames swallow most of the
 * damage, so a faster beat costs the full entity scan and delivers almost
 * nothing extra. The cheap setting and the correct setting are the same setting.
 */
public final class CleansingLightGene extends AbstractAbilityGene {

    public static final String KEY = "horsegenetics.cleansing_light";
    public static final int PRIORITY = 164;

    public static final String RADIUS = "radius";
    public static final double MIN_RADIUS = 4.0;
    public static final double MAX_RADIUS = 8.0;

    /** Health points per beat, negative because this is a healing aura with the sign flipped. */
    public static final double DAMAGE = -2.0;

    /** At or above 20: below that, hurt-immunity frames eat the damage and the scan is wasted. */
    public static final int INTERVAL_TICKS = 40;
    public static final int MAX_TARGETS = 8;

    public CleansingLightGene() {
        super(KEY, PRIORITY, "Cleansing light",
                "Cln", "Cleansing (Cln)",
                Dominance.RECESSIVE, Founders.EXPRESSING, 1.0,
                "The undead are no more troubled near this horse than anywhere else.",
                "Cleansing light",
                "Two copies. Zombies, skeletons, drowned and anything else that counts as undead take steady damage while they stand within a radius written on the allele copy. Anything the aura kills was killed by the horse, so you get no experience and none of the drops that only fall to a player - it is a defensive gene, not a farming one.");
    }

    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(EpiValue.uniform(RADIUS, MIN_RADIUS, MAX_RADIUS));
    }

    @Override
    protected List<GeneAbility> abilitiesWhenExpressed(EpiValues epi) {
        return List.of(new GeneAbility.Healing("group", "undead", epi.get(RADIUS), DAMAGE,
                INTERVAL_TICKS, MAX_TARGETS, GeneAbility.Condition.ALWAYS, 1));
    }
}
