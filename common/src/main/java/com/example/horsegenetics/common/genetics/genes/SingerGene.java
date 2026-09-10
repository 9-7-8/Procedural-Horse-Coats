package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.List;

/**
 * <b>Singer</b> ({@code horsegenetics.singer}) - occasionally, and without
 * warning, the horse plays music.
 *
 * <h2>The opening, because there is no middle</h2>
 * The original specification said "plays sections of a disc", which is not
 * possible: <b>a Minecraft sound plays from its beginning or not at all</b>.
 * There is no way to start partway through, so "a section" can only ever mean
 * "the opening, then stopped". Note-block phrases were considered as an
 * alternative and rejected - referencing music people already know is most of
 * the charm. The limit became the shape of the gene rather than something worked
 * around, and it is recorded here so the impossible version is not attempted
 * again.
 *
 * <h2>Deliberately frivolous</h2>
 * It does nothing useful and is not meant to. The magical side is allowed a few
 * loci whose entire return is that they are funny to breed for, and a horse that
 * will not stop playing <i>Cat</i> is one of them.
 *
 * <p>How long it plays and how loudly are written on the allele copy. The
 * cooldown is the safety: several singers in one pasture would otherwise overlap
 * constantly, and no volume setting fixes that.
 */
public final class SingerGene extends AbstractMatchedPairGene {

    public static final String KEY = "horsegenetics.singer";
    public static final int PRIORITY = 180;

    /** The two values a copy carries. */
    public static final String DURATION = "duration";
    public static final String VOLUME = "volume";

    /** Ticks. The recognisable opening of a record, and no more. */
    public static final double MIN_DURATION = 60;
    public static final double MAX_DURATION = 220;

    public static final double MIN_VOLUME = 0.5;
    public static final double MAX_VOLUME = 1.6;

    /** Ticks between attempts, and the cooldown under it. A herd of singers is the case to design for. */
    public static final int INTERVAL_TICKS = 1200;
    public static final int COOLDOWN_TICKS = 900;

    public static final double WILD_CARRIER_PERCENT = 12.0;

    public SingerGene() {
        super(KEY, PRIORITY, "Singer",
                List.of(
                        new Variant0("Cat", "minecraft:music_disc.cat", "Cat"),
                        new Variant0("Blks", "minecraft:music_disc.blocks", "Blocks"),
                        new Variant0("Chrp", "minecraft:music_disc.chirp", "Chirp"),
                        new Variant0("Far", "minecraft:music_disc.far", "Far"),
                        new Variant0("Mall", "minecraft:music_disc.mall", "Mall"),
                        new Variant0("Mlhi", "minecraft:music_disc.mellohi", "Mellohi"),
                        new Variant0("Stal", "minecraft:music_disc.stal", "Stal"),
                        new Variant0("Strd", "minecraft:music_disc.strad", "Strad"),
                        new Variant0("Wrd", "minecraft:music_disc.ward", "Ward"),
                        new Variant0("Wait", "minecraft:music_disc.wait", "Wait"),
                        new Variant0("Otsd", "minecraft:music_disc.otherside", "Otherside"),
                        new Variant0("Pgst", "minecraft:music_disc.pigstep", "Pigstep"),
                        new Variant0("Rlic", "minecraft:music_disc.relic", "Relic"),
                        new Variant0("Crtr", "minecraft:music_disc.creator", "Creator"),
                        new Variant0("Prcp", "minecraft:music_disc.precipice", "Precipice")),
                WILD_CARRIER_PERCENT,
                "The horse makes horse noises.",
                "One copy, and the horse is silent. It carries a song and never sings it.",
                "Two different songs. The horse settles on neither and sings nothing at all.",
                new MatchedText() {
                    @Override public String name(Variant v) {
                        return "Sings " + v.label();
                    }

                    @Override public String description(Variant v) {
                        return "Two matching copies. Every so often the horse plays the opening of "
                                + v.label() + " - not the whole record, because a Minecraft sound "
                                + "cannot be started partway through and so cannot be excerpted "
                                + "from the middle. How long it plays and how loudly are written "
                                + "on the allele copy.";
                    }
                });
    }

    @Override
    protected String idPrefix() {
        return "sings";
    }

    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(
                EpiValue.uniform(DURATION, MIN_DURATION, MAX_DURATION),
                EpiValue.uniform(VOLUME, MIN_VOLUME, MAX_VOLUME));
    }

    @Override
    protected List<GeneAbility> abilitiesFor(Variant v, EpiValues epi) {
        return List.of(new GeneAbility.Sound(v.subject(),
                new GeneAbility.Trigger.Interval(INTERVAL_TICKS),
                epi.get(VOLUME), 1.0, (int) Math.round(epi.get(DURATION)), COOLDOWN_TICKS,
                GeneAbility.Condition.ALWAYS, 1));
    }
}
