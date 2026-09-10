package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.List;

/**
 * <b>Music enjoyer</b> ({@code horsegenetics.music_enjoyer}) - play a record
 * near this horse and it slowly decides it likes you.
 *
 * <h2>Bond as something a gene can touch</h2>
 * Bond has so far been entirely about what the player does. This is the first
 * locus where the <i>horse</i> has an opinion about how it would like to be won
 * over, and it opens a category the mod has not used: genes that change the
 * husbandry rather than the animal.
 *
 * <h2>The daily cap is not optional</h2>
 * The translator routes this through the same path every other bond source uses,
 * so it answers to the same daily ceiling. A source that ignored the cap would
 * be an AFK exploit rather than a feature - leave a jukebox running overnight
 * and you would wake to a tier-three horse - and it would devalue every other
 * way of earning bond in one change.
 */
public final class MusicEnjoyerGene extends AbstractAbilityGene {

    public static final String KEY = "horsegenetics.music_enjoyer";
    public static final int PRIORITY = 168;

    /** Bond points per beat, and how far apart the beats are. */
    public static final int AMOUNT = 1;
    public static final int INTERVAL_TICKS = 200;

    public MusicEnjoyerGene() {
        super(KEY, PRIORITY, "Music enjoyer",
                "Msc", "Music-loving (Msc)",
                Dominance.RECESSIVE, Founders.EXPRESSING, 1.6,
                "The horse is indifferent to music.",
                "Music-loving",
                "Two copies. While a jukebox is playing within a few blocks, the horse stands there giving off hearts and its bond rises. It still answers to the same daily bond ceiling as everything else, so this is a pleasant way to spend a day rather than a shortcut past one.");
    }

    @Override
    protected List<GeneAbility> abilitiesWhenExpressed(EpiValues epi) {
        return List.of(new GeneAbility.Bond(AMOUNT, INTERVAL_TICKS, new GeneAbility.Condition.Flag("near_jukebox", false), 1));
    }
}
