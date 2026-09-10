package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.List;

/**
 * <b>Hydrophobic</b> ({@code horsegenetics.hydrophobic}) - it will not be ridden
 * into deep water. Take one in past its knees and it throws you and makes for
 * the nearest shore.
 *
 * <h2>The wild type here is the improved behaviour</h2>
 * Worth stating plainly, because a reader who knows Minecraft will assume the
 * opposite and conclude the gene does nothing. In this mod a tamed ridden horse
 * gets a swimming assist and will carry you across a river - a deliberate
 * improvement on vanilla. This allele takes it back off, so it is a defect to
 * breed <i>out</i> rather than a trait to breed for.
 *
 * <p>It is <b>recessive</b>, and that was the whole decision. The original
 * specification made it dominant, which would have meant most horses reverting
 * to vanilla water behaviour - silently changing how every horse already bred
 * behaves and quietly retiring the swimming assist. Recessive keeps the assist
 * universal and makes this a rare fault.
 */
public final class HydrophobicGene extends AbstractAbilityGene {

    public static final String KEY = "horsegenetics.hydrophobic";
    public static final int PRIORITY = 158;

    public HydrophobicGene() {
        super(KEY, PRIORITY, "Hydrophobic",
                "Hyd", "Hydrophobic (Hyd)",
                Dominance.RECESSIVE, Founders.EXPRESSING, 1.5,
                "The horse will carry you across a river like any other horse here.",
                "Hydrophobic",
                "Two copies. In water deep enough to swim in, the horse throws its rider and heads for land. It is exactly vanilla horse behaviour, which is what makes it a fault rather than a feature in a mod that fixed it.");
    }

    @Override
    protected List<GeneAbility> abilitiesWhenExpressed(EpiValues epi) {
        return List.of(new GeneAbility.Traversal("water_averse", "self", GeneAbility.Condition.ALWAYS, 1));
    }
}
