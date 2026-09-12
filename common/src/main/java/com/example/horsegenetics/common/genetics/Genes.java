package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.CommonMaps;
import com.example.horsegenetics.common.genetics.genes.AcanGene;
import com.example.horsegenetics.common.genetics.genes.AgoutiGene;
import com.example.horsegenetics.common.genetics.genes.B4galt7Gene;
import com.example.horsegenetics.common.genetics.genes.BrindleGene;
import com.example.horsegenetics.common.genetics.genes.ChampagneGene;
import com.example.horsegenetics.common.genetics.genes.CkmGene;
import com.example.horsegenetics.common.genetics.genes.CutieMarkGene;
import com.example.horsegenetics.common.genetics.genes.CvmGene;
import com.example.horsegenetics.common.genetics.genes.DhampirGene;
import com.example.horsegenetics.common.genetics.genes.DietGene;
import com.example.horsegenetics.common.genetics.genes.DunGene;
import com.example.horsegenetics.common.genetics.genes.EdnrbGene;
import com.example.horsegenetics.common.genetics.genes.ExtensionGene;
import com.example.horsegenetics.common.genetics.genes.ExtremeWhiteDominantGene;
import com.example.horsegenetics.common.genetics.genes.EyeColourGene;
import com.example.horsegenetics.common.genetics.genes.EyeGlowGene;
import com.example.horsegenetics.common.genetics.genes.EyeScleraGene;
import com.example.horsegenetics.common.genetics.genes.EyeSectorColourGene;
import com.example.horsegenetics.common.genetics.genes.EyeSectorGene;
import com.example.horsegenetics.common.genetics.genes.ThirdEyeGene;
import com.example.horsegenetics.common.genetics.eye.EyeLocus;
import com.example.horsegenetics.common.genetics.genes.FlaxenGene;
import com.example.horsegenetics.common.genetics.genes.Gbe1Gene;
import com.example.horsegenetics.common.genetics.genes.GreyGene;
import com.example.horsegenetics.common.genetics.genes.Gys1Gene;
import com.example.horsegenetics.common.genetics.genes.HealerGene;
import com.example.horsegenetics.common.genetics.genes.Hmga2Gene;
import com.example.horsegenetics.common.genetics.genes.HuedPangareGene;
import com.example.horsegenetics.common.genetics.genes.KitGene;
import com.example.horsegenetics.common.genetics.genes.LcorlGene;
import com.example.horsegenetics.common.genetics.genes.LeopardGene;
import com.example.horsegenetics.common.genetics.genes.LightGene;
import com.example.horsegenetics.common.genetics.genes.LutGene;
import com.example.horsegenetics.common.genetics.genes.LycanGene;
import com.example.horsegenetics.common.genetics.genes.MagicHealthGene;
import com.example.horsegenetics.common.genetics.genes.MagicJumpGene;
import com.example.horsegenetics.common.genetics.genes.MagicSectoralHeterochromiaGene;
import com.example.horsegenetics.common.genetics.genes.MagicSizeGene;
import com.example.horsegenetics.common.genetics.genes.MagicFighterGene;
import com.example.horsegenetics.common.genetics.genes.MagicItemDropGene;
import com.example.horsegenetics.common.genetics.genes.MagicMeatGene;
import com.example.horsegenetics.common.genetics.genes.MagicMilkVolumeGene;
import com.example.horsegenetics.common.genetics.genes.MagicMobAuraGene;
import com.example.horsegenetics.common.genetics.genes.MagicNightTemperGene;
import com.example.horsegenetics.common.genetics.genes.MagicNightWatchGene;
import com.example.horsegenetics.common.genetics.genes.MagicOnDeathGene;
import com.example.horsegenetics.common.genetics.genes.MagicSwimSpeedGene;
import com.example.horsegenetics.common.genetics.genes.MagicWaterBreathingGene;
import com.example.horsegenetics.common.genetics.genes.MagicSpeedGene;
import com.example.horsegenetics.common.genetics.genes.MagicZebraGene;
import com.example.horsegenetics.common.genetics.genes.MoltenHoovesGene;
import com.example.horsegenetics.common.genetics.genes.FireproofGene;
import com.example.horsegenetics.common.genetics.genes.BirdBonedGene;
import com.example.horsegenetics.common.genetics.genes.OceanBornGene;
import com.example.horsegenetics.common.genetics.genes.HydrophobicGene;
import com.example.horsegenetics.common.genetics.genes.HotBloodedGene;
import com.example.horsegenetics.common.genetics.genes.DryadGene;
import com.example.horsegenetics.common.genetics.genes.IntimidatingGene;
import com.example.horsegenetics.common.genetics.genes.MeowingGene;
import com.example.horsegenetics.common.genetics.genes.CleansingLightGene;
import com.example.horsegenetics.common.genetics.genes.HolyWardGene;
import com.example.horsegenetics.common.genetics.genes.EcholocateGene;
import com.example.horsegenetics.common.genetics.genes.BaseAlarmGene;
import com.example.horsegenetics.common.genetics.genes.MusicEnjoyerGene;
import com.example.horsegenetics.common.genetics.genes.GladiatorGene;
import com.example.horsegenetics.common.genetics.genes.GuardianGene;
import com.example.horsegenetics.common.genetics.genes.EnderEchoGene;
import com.example.horsegenetics.common.genetics.genes.SpontaneousBreedingGene;
import com.example.horsegenetics.common.genetics.genes.EyesightGene;
import com.example.horsegenetics.common.genetics.genes.WeatherSpeedGene;
import com.example.horsegenetics.common.genetics.genes.WeatherJumpGene;
import com.example.horsegenetics.common.genetics.genes.FoodPreferenceGene;
import com.example.horsegenetics.common.genetics.genes.PotionMilkGene;
import com.example.horsegenetics.common.genetics.genes.EggLayerGene;
import com.example.horsegenetics.common.genetics.genes.SingerGene;
import com.example.horsegenetics.common.genetics.genes.PackLeaderGene;
import com.example.horsegenetics.common.genetics.genes.SpawnerGene;
import com.example.horsegenetics.common.genetics.genes.ManchadoGene;
import com.example.horsegenetics.common.genetics.genes.ManeColorGene;
import com.example.horsegenetics.common.genetics.genes.MatpGene;
import com.example.horsegenetics.common.genetics.genes.MegaesophagusGene;
import com.example.horsegenetics.common.genetics.genes.MetGene;
import com.example.horsegenetics.common.genetics.genes.MilkGene;
import com.example.horsegenetics.common.genetics.genes.MitfGene;
import com.example.horsegenetics.common.genetics.genes.MstnGene;
import com.example.horsegenetics.common.genetics.genes.MushroomGene;
import com.example.horsegenetics.common.genetics.genes.Myo5aGene;
import com.example.horsegenetics.common.genetics.genes.NaturalZebraGene;
import com.example.horsegenetics.common.genetics.genes.PangareGene;
import com.example.horsegenetics.common.genetics.genes.ParticleGene;
import com.example.horsegenetics.common.genetics.genes.Patn1Gene;
import com.example.horsegenetics.common.genetics.genes.Patn2Gene;
import com.example.horsegenetics.common.genetics.genes.Pax3Gene;
import com.example.horsegenetics.common.genetics.genes.Pdk4Gene;
import com.example.horsegenetics.common.genetics.genes.Plod1Gene;
import com.example.horsegenetics.common.genetics.genes.PpibGene;
import com.example.horsegenetics.common.genetics.genes.PrkdcGene;
import com.example.horsegenetics.common.genetics.genes.RabicanoGene;
import com.example.horsegenetics.common.genetics.genes.RainbowDustGene;
import com.example.horsegenetics.common.genetics.genes.Rapgef5Gene;
import com.example.horsegenetics.common.genetics.genes.RoanGene;
import com.example.horsegenetics.common.genetics.genes.Ryr2Gene;
import com.example.horsegenetics.common.genetics.genes.Scn4aGene;
import com.example.horsegenetics.common.genetics.genes.SexGene;
import com.example.horsegenetics.common.genetics.genes.ShadeGene;
import com.example.horsegenetics.common.genetics.genes.ShadowcreatureGene;
import com.example.horsegenetics.common.genetics.genes.ShoxGene;
import com.example.horsegenetics.common.genetics.genes.SilverGene;
import com.example.horsegenetics.common.genetics.genes.SootyGene;
import com.example.horsegenetics.common.genetics.genes.St14Gene;
import com.example.horsegenetics.common.genetics.genes.TailColorGene;
import com.example.horsegenetics.common.genetics.genes.TigerEyeGene;
import com.example.horsegenetics.common.genetics.genes.TobianoGene;
import com.example.horsegenetics.common.genetics.genes.Toe1Gene;
import com.example.horsegenetics.common.genetics.genes.VerdantGene;
import com.example.horsegenetics.common.genetics.spec.GeneSpec;
import com.example.horsegenetics.common.genetics.spec.GeneSpecLoader;
import com.example.horsegenetics.common.genetics.spec.GeneSpecParser;
import com.example.horsegenetics.common.genetics.spec.SpecGene;

import com.example.horsegenetics.common.CommonLog;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The gene registry. Genes are addressed by {@code <modauthor>.<gene>}
 * ({@link #NS} for the built-ins). See <b>wiki/gene-*.html</b> for the full
 * description of every gene.
 *
 * <h2>One order, derived</h2>
 * There is a single processing order: <b>every</b> registered gene - built-in
 * and data-driven alike - sorted on {@code (}{@link Gene#priority()}{@code ,
 * key)}. The three public views are all a <i>result</i> of that one sort, never
 * a source:
 * <ul>
 *   <li>{@link #codeOrder()} / {@link #all()} - the whole sorted list;
 *       position in the genotype code follows it.</li>
 *   <li>{@link #naturalOrder()} - the sorted list filtered to
 *       {@link Gene#isNatural()} - the order natural genes push pigment down in
 *       phase 1.</li>
 *   <li>{@link #magicalOrder()} - the sorted list filtered to the magical
 *       genes - the order they add signed RGB in phase 3.</li>
 * </ul>
 * The natural / magical <b>phase</b> (not the priority number) is what splits
 * the two coat passes; the {@code 0-99} / {@code 100+} bands are only a
 * convention, and {@link #register} logs a warning when a gene sits outside its
 * band. <b>Registration order is never respected</b>: two people who drop the
 * same gene files in a different order must get the same horses.
 *
 * <p>Register during startup, before anything parses a genotype - each
 * registration can move where a gene sits in the code, and a code written
 * against the old order still parses (a gene now absent from it reads as wild
 * type) but is a different genotype. Dev-only mod, no saves to keep; see the
 * "no legacy code" rule in {@code CLAUDE.md}.
 */
public final class Genes {

    public static final String NS = "horsegenetics";

    /** The lowest priority in the magical band - below this a gene is "natural" by convention. */
    public static final int MAGICAL_BAND_START = 100;

    /** Sex, at priority 1 - the first gene resolved, and the only one that paints nothing. */
    public static final SexGene SEX = new SexGene();
    /**
     * <b>Diet</b> - what the horse will eat, at priority 5, ahead of every gene
     * that paints. The order is the point: {@link HorseDiet#resolve} keeps the
     * <b>last</b> {@link DietContribution} claim in {@link #codeOrder()}, so a
     * later gene that says "this one cannot be fed at all" overrides the locus
     * with no special case anywhere. {@link Gene#feralOnly()} keeps all twelve
     * of its narrow diets out of every named breed.
     */
    public static final DietGene DIET = new DietGene();
    public static final ExtensionGene EXTENSION = new ExtensionGene();
    public static final AgoutiGene AGOUTI = new AgoutiGene();
    /**
     * <b>Shade</b> - the <i>ASIP</i>/<i>RALY</i> regulatory locus that decides
     * how far black spreads over a bay, and so which of blood / ordinary /
     * liver / seal that bay is. It paints nothing itself: {@link AgoutiGene}
     * reads it through {@link BayShade}, and names it in
     * {@link Gene#coatDependsOn()} so a bay's texture key folds its alleles in.
     * Unlike the appaloosa modifiers it is unconditional - every horse carries
     * and transmits it, chestnuts included.
     */
    public static final ShadeGene SHADE = new ShadeGene();
    public static final ChampagneGene CHAMPAGNE = new ChampagneGene();
    public static final GreyGene GREY = new GreyGene();
    public static final MatpGene MATP = new MatpGene();
    public static final MagicZebraGene MAGIC_ZEBRA = new MagicZebraGene();
    /**
     * <b>Extreme white dominant</b> - the only gene that changes the coat by
     * changing a <i>rule</i> rather than by painting. See
     * {@link WhiteLockContribution}; its priority is a code-order slot only.
     */
    public static final ExtremeWhiteDominantGene EXTREME_WHITE_DOMINANT = new ExtremeWhiteDominantGene();
    public static final DunGene DUN = new DunGene();
    public static final SilverGene SILVER = new SilverGene();
    /**
     * <b>Flaxen</b> - the pale mane and tail of a chestnut, as a simulated
     * polygenic dosage rather than the unconfirmed recessive folklore models it
     * as. Sits immediately after {@link SilverGene}: the two are constantly
     * mistaken for each other, they do the same job on the long hair, and they
     * work on opposite pigments - which is why a silver is invisible on a
     * chestnut and this is invisible on everything else.
     */
    public static final FlaxenGene FLAXEN = new FlaxenGene();
    /**
     * <b>Sooty</b> - the dark countershading over a horse's topline, and the
     * one gene here that works by <i>declining to remove</i> pigment rather
     * than by removing it. It sits after {@link DunGene} so a dun's primitive
     * markings are already drawn, and before {@link MatpGene} so a smutty
     * buckskin's cape dilutes with the rest of it.
     *
     * <p>It models the same chromosome-22 signal {@link ShadeGene} does and is
     * deliberately a second gene: shade is scoped to bays, where the evidence
     * is, and this is scoped to the phenotype, which turns up on palominos and
     * buckskins the evidence says nothing about.
     */
    public static final SootyGene SOOTY = new SootyGene();
    /**
     * <b>Pangare</b> (mealy) - the pale muzzle, belly and inner legs of an
     * Exmoor or a Fjord, and {@link SootyGene}'s exact mirror: sooty keeps
     * black on the topline, this takes red off the underside. Between them they
     * are the two halves of countershading, and they sit next to each other.
     */
    public static final PangareGene PANGARE = new PangareGene();
    /**
     * <b>Hued pangare</b> - {@link PangareGene}'s own region map painted in a
     * colour rather than washed out to cream. Registered next to it because it
     * shares its painter: see {@link HuedPangareGene}.
     */
    public static final HuedPangareGene HUED_PANGARE = new HuedPangareGene();
    public static final MushroomGene MUSHROOM = new MushroomGene();
    /**
     * Natural zebra striping - the real-world half of the zebra pair, and
     * the last natural gene to touch the base coat before the white-spotting
     * loci. It <b>whitens the gaps</b> between the bands rather than painting
     * black ones, which is how a real zebra is made; {@link MagicZebraGene}
     * paints the same body map in the opposite direction.
     */
    public static final NaturalZebraGene NATURAL_ZEBRA = new NaturalZebraGene();
    public static final RoanGene ROAN = new RoanGene();
    /**
     * <b>Rabicano</b> - the other white-hair pattern, and nothing like the first
     * one: it starts at the tail dock and the flank rather than covering the
     * trunk evenly, and it has a coon tail, which roan never does. Its allele is
     * dominant and what it <i>shows</i> is a roll that can come out at nothing,
     * which is the only way to model a trait whose parents are routinely
     * recorded as solid.
     */
    public static final RabicanoGene RABICANO = new RabicanoGene();
    public static final TobianoGene TOBIANO = new TobianoGene();

    /**
     * The four <b>white-pattern loci</b>, named for the real genes they model.
     * Keeping them apart is not pedantry: only alleles at the <i>same</i> locus
     * compete for a slot, so {@code KIT} can hold dominant white or sabino but
     * never both, while {@code MITF} and {@code PAX3} splash stack freely and
     * frame stacks with everything. Tobiano and roan sit near {@code KIT} on
     * chromosome 3 but are not {@code KIT} variants, so they stay their own
     * genes above.
     */
    public static final EdnrbGene EDNRB = new EdnrbGene();
    public static final KitGene KIT = new KitGene();
    /**
     * <b>Manchado</b> - the rare Argentine pattern, and its own locus on
     * purpose. It is not a leopard-complex modifier and not a {@code KIT}
     * allele: it is not one of the mapped white-spotting pathways at all, and
     * filing it under either would have claimed something nobody has earned. A
     * modelled recessive, carriers only in the wild.
     */
    public static final ManchadoGene MANCHADO = new ManchadoGene();
    public static final MitfGene MITF = new MitfGene();
    public static final Pax3Gene PAX3 = new Pax3Gene();

    /**
     * <b>The leopard complex</b> - the appaloosa spotting family. Three loci:
     * {@link LeopardGene} ({@code LP}, the one that patterns and the one a coat
     * depends on) plus the two silent modifiers {@link Patn1Gene} and
     * {@link Patn2Gene} that only mean anything on an {@code LP} horse. The
     * modifiers paint nothing themselves - {@code LeopardGene.expressionIn}
     * reads them, and {@code Gene.coatDependsOn()} folds them into the texture
     * key. It is deliberately the model's <b>only</b> cross-locus-reading gene
     * (see {@code wiki/roadmap.html} §5.4).
     */
    public static final LeopardGene LEOPARD = new LeopardGene();
    public static final Patn1Gene PATN1 = new Patn1Gene();
    public static final Patn2Gene PATN2 = new Patn2Gene();

    /**
     * The <b>magical utility genes</b> - the second wave of magic, and the first
     * genes whose point is what the horse <i>does</i> rather than what it looks
     * like. Several of them ({@link MilkGene}, {@link MagicSizeGene},
     * {@link MagicSpeedGene}, {@link MagicHealthGene}, {@link MagicJumpGene},
     * {@link LightGene}, {@link VerdantGene}, {@link HealerGene}) reach the game
     * through {@link AbilityContribution} or
     * {@link com.example.horsegenetics.common.trait.EpigeneticTraitContribution}
     * rather than through the coat.
     *
     * <p>They were designed as a set rather than one at a time, with broad
     * epigenetic ranges, so that they <b>combine</b>: a ten-times healer with a
     * striped mane that spreads moss is a horse nobody wrote a line of code for.
     * The mane and tail loci are separate on purpose, and light is codominant on
     * purpose, for the same reason - each doubling of the outcome space is a
     * doubling of what a breeder can aim at.
     */
    public static final MilkGene MILK = new MilkGene();
    /**
     * Brindle - the model's one <b>X-linked</b> gene, and the proof of the
     * sex-linked scaffolding. A stallion carries one copy and can never be a
     * carrier; a mare needs two.
     */
    public static final BrindleGene BRINDLE = new BrindleGene();
    /**
     * Tiger eye - amber irises and nothing else. The first gene in the mod that
     * changes only the eyes, and the reason the eye-colour channel exists.
     */
    public static final TigerEyeGene TIGER_EYE = new TigerEyeGene();
    /**
     * <b>The eye loci</b> - thirteen of them, and the reason no gene in the mod
     * paints an iris on its own account any more.
     *
     * <p>Eight are natural and sit in the {@code 61}-{@code 68} slots
     * immediately after {@link #TIGER_EYE}, which is the gene that made the case
     * for them: iris colour, sclera colour, and the two halves of sectoral
     * heterochromia, each once per eye. Five are magical - the two glows per eye
     * and the third eye - and sit at {@code 121}-{@code 125}.
     *
     * <p><b>The right eye of each pair sorts one ahead of the left</b>, and that
     * is load-bearing rather than tidy: the second locus to roll reads the first
     * through {@link FounderContext} and usually copies it, which is what keeps
     * wild heterochromia rare while leaving it completely ordinary to breed for.
     * See {@link com.example.horsegenetics.common.genetics.genes.AbstractEyeGene}.
     *
     * <p>What a <i>white</i> or a <i>cream</i> horse's eyes are is not decided
     * here and not decided by those genes either: they
     * {@linkplain com.example.horsegenetics.common.genetics.eye.EyeRequest request}
     * an allele at these loci and
     * {@link com.example.horsegenetics.common.genetics.eye.Eyes#force} writes it
     * onto the horse when the horse is made. See {@code wiki/eye-colour.html}.
     */
    public static final EyeColourGene EYE_COLOUR_RIGHT = new EyeColourGene(EyeLocus.IRIS_RIGHT, 61);
    public static final EyeColourGene EYE_COLOUR_LEFT = new EyeColourGene(EyeLocus.IRIS_LEFT, 62);
    public static final EyeSectorGene EYE_SECTOR_RIGHT = new EyeSectorGene(EyeLocus.SECTOR_RIGHT, 63);
    public static final EyeSectorGene EYE_SECTOR_LEFT = new EyeSectorGene(EyeLocus.SECTOR_LEFT, 64);
    public static final EyeSectorColourGene EYE_SECTOR_COLOUR_RIGHT =
            new EyeSectorColourGene(EyeLocus.SECTOR_COLOUR_RIGHT, 65);
    public static final EyeSectorColourGene EYE_SECTOR_COLOUR_LEFT =
            new EyeSectorColourGene(EyeLocus.SECTOR_COLOUR_LEFT, 66);
    public static final EyeScleraGene EYE_SCLERA_RIGHT = new EyeScleraGene(EyeLocus.SCLERA_RIGHT, 67);
    public static final EyeScleraGene EYE_SCLERA_LEFT = new EyeScleraGene(EyeLocus.SCLERA_LEFT, 68);
    public static final EyeGlowGene EYE_GLOW_IRIS_RIGHT = new EyeGlowGene(EyeLocus.GLOW_IRIS_RIGHT, 121);
    public static final EyeGlowGene EYE_GLOW_IRIS_LEFT = new EyeGlowGene(EyeLocus.GLOW_IRIS_LEFT, 122);
    public static final EyeGlowGene EYE_GLOW_SCLERA_RIGHT =
            new EyeGlowGene(EyeLocus.GLOW_SCLERA_RIGHT, 123);
    public static final EyeGlowGene EYE_GLOW_SCLERA_LEFT =
            new EyeGlowGene(EyeLocus.GLOW_SCLERA_LEFT, 124);
    public static final ThirdEyeGene THIRD_EYE = new ThirdEyeGene(125);

    public static final MagicSizeGene BODY_SIZE = new MagicSizeGene();
    public static final MagicSpeedGene MAGIC_SPEED = new MagicSpeedGene();
    public static final MagicHealthGene MAGIC_HEALTH = new MagicHealthGene();
    public static final MagicJumpGene MAGIC_JUMP = new MagicJumpGene();
    /**
     * The <b>mechanical magical loci</b> - eight genes that paint nothing and
     * exist so that a breed can be described by what its horses <i>do</i> and
     * not only by what they look like.
     *
     * <p>They divide in two. {@link #MAGIC_SWIM_SPEED},
     * {@link #MAGIC_WATER_BREATHING} and {@link #MAGIC_FIGHTER} sit in the
     * body-stat band and are the same codominant, per-copy-percentage shape as
     * the four above them - both copies add, and a "down" copy counts against
     * an "up" one. The rest sit beside {@link #MILK} in the utility band and are
     * recessive switches: what happens where the horse died, what it left
     * behind, how mobs feel about it, how much meat, how many fillings a day.
     *
     * <p>None of them reaches the game through
     * {@link com.example.horsegenetics.common.trait.Traits}. That record is four
     * numbers and a list of conditions, and it is meant to survive a version
     * port unchanged; "swims faster" and "explodes" are Minecraft-shaped facts
     * and belong on the {@link com.example.horsegenetics.common.genetics.spec.GeneAbility}
     * side of the line, which is exactly what that side is for.
     */
    public static final MagicSwimSpeedGene MAGIC_SWIM_SPEED = new MagicSwimSpeedGene();
    public static final MagicWaterBreathingGene MAGIC_WATER_BREATHING = new MagicWaterBreathingGene();
    public static final MagicFighterGene MAGIC_FIGHTER = new MagicFighterGene();
    /**
     * Volume governs a yield <b>kind</b> rather than a gene, so it reaches
     * {@link #MILK}'s milk, water and lava at once without either locus
     * knowing about the other - which is the only shape that works, since a
     * gene is handed its own epigenetic values and nobody else's.
     */
    public static final MagicMilkVolumeGene MAGIC_MILK_VOLUME = new MagicMilkVolumeGene();
    public static final MagicMeatGene MAGIC_MEAT = new MagicMeatGene();
    public static final MagicItemDropGene MAGIC_ITEM_DROP = new MagicItemDropGene();
    /**
     * The one locus in the mod whose founder table is <b>carriers only</b> on
     * purpose. Every other magical gene with an invisible carrier puts the wild
     * population on the expressing combinations so that a player can see what
     * they are catching; this one is a hazard rather than a prize, so a feral
     * horse is never an exploding one. (Owner's call.)
     */
    public static final MagicOnDeathGene MAGIC_ON_DEATH = new MagicOnDeathGene();
    public static final MagicMobAuraGene MAGIC_MOB_AURA = new MagicMobAuraGene();
    /**
     * The <b>night loci</b> - what a horse becomes after dark. Both are unlike
     * anything else in the registry in one respect: their founder tables are
     * <b>entirely heterozygous</b>, so every feral horse carries exactly one
     * variant and none of them expresses it. The plainest outcome each locus has
     * is the one that cannot be caught. (Owner's call.)
     *
     * <p>{@link #MAGIC_NIGHT_TEMPER} supersedes {@link #MAGIC_NIGHT_WATCH}
     * whenever it has something to act on - a horse cannot both stalk you and
     * flee from you - and that rule lives in the translator, because which mobs
     * are nearby is a question only the running game can answer.
     */
    public static final MagicNightTemperGene MAGIC_NIGHT_TEMPER = new MagicNightTemperGene();
    public static final MagicNightWatchGene MAGIC_NIGHT_WATCH = new MagicNightWatchGene();
    public static final ManeColorGene MANE_COLOR = new ManeColorGene();
    public static final TailColorGene TAIL_COLOR = new TailColorGene();
    public static final ParticleGene PARTICLE = new ParticleGene();
    /**
     * <b>Rainbow dust</b> - its own locus rather than a forty-first
     * {@link ParticleGene} allele, because the particle locus's premise is that
     * its alleles <i>compete</i> for two slots and this one competes with
     * nothing: a horse may trail flames and rainbow dust at once. It sits
     * immediately after it all the same, since it is the gene that locus is most
     * often confused with.
     */
    public static final RainbowDustGene RAINBOW_DUST = new RainbowDustGene();
    /**
     * <b>Molten hooves</b> - burning hoofprints that fade behind the horse, in
     * a colour written on the allele copy. It sits with the other emission loci
     * and is the only <b>dominant</b> one among them, deliberately: every other
     * trail is a reward for breeding, and this one spreads through a herd on its
     * own so that catching a single molten horse is immediately worth something.
     * It sets nothing alight - see the class note, which is where the argument
     * against real fire is kept.
     */
    public static final MoltenHoovesGene MOLTEN_HOOVES = new MoltenHoovesGene();

    // ------------------------------------------------------------------
    // The behaviour family
    //
    // Twenty-six loci that change what a horse DOES rather than how it looks.
    // None of them paints; most are recessive and several are never expressed
    // in the wild at all, so they are the clearest argument the gene database
    // has for existing. Their hazards - what each one can do to a world if it
    // is built carelessly - are on the coding tab of each gene's page.
    // ------------------------------------------------------------------
    public static final FireproofGene FIREPROOF = new FireproofGene();
    public static final BirdBonedGene BIRD_BONED = new BirdBonedGene();
    public static final OceanBornGene OCEAN_BORN = new OceanBornGene();
    public static final HydrophobicGene HYDROPHOBIC = new HydrophobicGene();
    public static final HotBloodedGene HOT_BLOODED = new HotBloodedGene();
    public static final DryadGene DRYAD = new DryadGene();
    public static final IntimidatingGene INTIMIDATING = new IntimidatingGene();
    public static final MeowingGene MEOWING = new MeowingGene();
    public static final CleansingLightGene CLEANSING_LIGHT = new CleansingLightGene();
    public static final HolyWardGene HOLY_WARD = new HolyWardGene();
    public static final EcholocateGene ECHOLOCATE = new EcholocateGene();
    public static final BaseAlarmGene BASE_ALARM = new BaseAlarmGene();
    public static final MusicEnjoyerGene MUSIC_ENJOYER = new MusicEnjoyerGene();
    public static final GladiatorGene GLADIATOR = new GladiatorGene();
    public static final GuardianGene GUARDIAN = new GuardianGene();
    public static final EnderEchoGene ENDER_ECHO = new EnderEchoGene();
    public static final SpontaneousBreedingGene SPONTANEOUS_BREEDING = new SpontaneousBreedingGene();
    public static final EyesightGene EYESIGHT = new EyesightGene();
    public static final WeatherSpeedGene WEATHER_SPEED = new WeatherSpeedGene();
    public static final WeatherJumpGene WEATHER_JUMP = new WeatherJumpGene();
    public static final FoodPreferenceGene FOOD_PREFERENCE = new FoodPreferenceGene();
    public static final PotionMilkGene POTION_MILK = new PotionMilkGene();
    public static final EggLayerGene EGG_LAYER = new EggLayerGene();
    public static final SingerGene SINGER = new SingerGene();
    public static final PackLeaderGene PACK_LEADER = new PackLeaderGene();
    public static final SpawnerGene SPAWNER = new SpawnerGene();
    /**
     * <b>LYCAN</b> - the werewolf locus, thirty-seven shapes, every one of them
     * recessive to the wild type <i>and</i> to each other: only two copies of
     * the same allele shift. It paints nothing and grants no
     * {@link AbilityContribution} - the whole effect is a night-time entity swap
     * in {@code neoforge/server/LycanthropyHandler}, the same split
     * {@link #DHAMPIR} makes.
     */
    public static final LycanGene LYCAN = new LycanGene();
    public static final LightGene LIGHT = new LightGene();
    /** LUT - swaps the natural red/black gradient for an unnatural palette when homozygous for a variant. */
    public static final LutGene LUT = new LutGene();
    /** Cutie mark - a recessive epigenetic emblem of 1-3 items on both flanks, drawn over everything. */
    public static final CutieMarkGene CUTIE_MARK = new CutieMarkGene();
    public static final HealerGene HEALER = new HealerGene();
    /**
     * Magic sectoral heterochromia - two different colour alleles and the horse
     * shows both, one wedge of each iris apiece. The only gene in the mod whose
     * <b>heterozygote</b> is the expressing combination: matched pairs and
     * anything carrying the wild type show nothing.
     */
    public static final MagicSectoralHeterochromiaGene SECTORAL_EYES = new MagicSectoralHeterochromiaGene();
    public static final VerdantGene VERDANT = new VerdantGene();
    /**
     * <b>Dhampir</b> - the magical recessive whose <i>carrier</i> is visible:
     * one copy gives red eyes and glowing scleras and nothing else, two gives a
     * white, sun-shy animal with triple health that cannot be fed and heals
     * only by hunting. It sorts after {@link #DIET} on purpose - the diet
     * channel keeps the last claim, so this one's {@code NOTHING} wins.
     */
    public static final DhampirGene DHAMPIR = new DhampirGene();
    /**
     * <b>Shadowcreature</b> - blacks out the head and neck outright, burns the
     * eyes gold with no white round them, and runs tentacles out of the join
     * into the barrel. A Java gene because those three land in three different
     * phases, and the eyes in particular can only be reached from the overlay
     * pass.
     */
    public static final ShadowcreatureGene SHADOWCREATURE = new ShadowcreatureGene();

    /**
     * The <b>non-coat genes</b> - performance, size and health. They occupy the
     * top of the natural band ({@code 80}-{@code 99}), after every gene that
     * paints, because <b>none of them paints anything</b>: every combination
     * they can produce is a {@link Expression#wildType() wild type}, so
     * {@link Gene#affectsCoat()} is false for all of them, they are left out of
     * a horse's texture key, and the genotype gallery collapses each of them to
     * a single entry however many alleles it has. What they do instead travels
     * through {@link com.example.horsegenetics.common.trait.HorseTraits}: speed,
     * max health, jump strength, body size, and the disorders a horse expresses.
     *
     * <p>Their position among <i>themselves</i> is arbitrary - trait
     * contributions are additive and order-independent by construction (see
     * {@link com.example.horsegenetics.common.trait.TraitBuilder}) - so the
     * numbers here only fix a stable slot in the genotype code.
     */
    public static final MstnGene MSTN = new MstnGene();
    public static final Pdk4Gene PDK4 = new Pdk4Gene();
    public static final CkmGene CKM = new CkmGene();
    public static final Ryr2Gene RYR2 = new Ryr2Gene();
    public static final LcorlGene LCORL = new LcorlGene();
    public static final Hmga2Gene HMGA2 = new Hmga2Gene();

    /**
     * The <b>recessive health</b> loci. Every one of them is absent from its own
     * founder table as a homozygote - a wild-caught horse is an adult that
     * survived, so it can carry a disorder but never have one. The only way to
     * see any of these is to breed two carriers, which is the whole design: it
     * makes a pedigree worth keeping.
     */
    public static final AcanGene ACAN = new AcanGene();
    public static final B4galt7Gene B4GALT7 = new B4galt7Gene();
    public static final Plod1Gene PLOD1 = new Plod1Gene();
    public static final Rapgef5Gene RAPGEF5 = new Rapgef5Gene();
    public static final St14Gene ST14 = new St14Gene();
    public static final ShoxGene SHOX = new ShoxGene();
    public static final MetGene MET = new MetGene();
    public static final PpibGene PPIB = new PpibGene();
    public static final PrkdcGene PRKDC = new PrkdcGene();
    public static final Myo5aGene MYO5A = new Myo5aGene();
    public static final Toe1Gene TOE1 = new Toe1Gene();
    public static final CvmGene CVM = new CvmGene();
    public static final Gbe1Gene GBE1 = new Gbe1Gene();
    public static final MegaesophagusGene MEGAESOPHAGUS = new MegaesophagusGene();

    /**
     * The <b>dominant</b> disorders, and the two exceptions to the paragraph
     * above. A dominant has no silent carrier, so if founders could never be
     * affected the allele could never enter the world at all - which means a
     * wild-caught horse really can be a sick one here. The homozygote is still
     * excluded, so the worst outcome is still something somebody bred. See
     * {@link com.example.horsegenetics.common.genetics.genes.DominantDisorderGene}.
     */
    public static final Scn4aGene SCN4A = new Scn4aGene();
    public static final Gys1Gene GYS1 = new Gys1Gene();

    /** The hand-written genes. Order here is irrelevant - the registry sorts. */
    private static final List<Gene> BUILTINS = List.of(
            SEX, DIET, EXTENSION, AGOUTI, SHADE, CHAMPAGNE, GREY, MATP,
            MAGIC_ZEBRA, EXTREME_WHITE_DOMINANT, DUN, SILVER, FLAXEN, SOOTY, PANGARE, HUED_PANGARE,
            MUSHROOM, BRINDLE, TIGER_EYE,
            EYE_COLOUR_RIGHT, EYE_COLOUR_LEFT, EYE_SECTOR_RIGHT, EYE_SECTOR_LEFT,
            EYE_SECTOR_COLOUR_RIGHT, EYE_SECTOR_COLOUR_LEFT, EYE_SCLERA_RIGHT, EYE_SCLERA_LEFT,
            EYE_GLOW_IRIS_RIGHT, EYE_GLOW_IRIS_LEFT, EYE_GLOW_SCLERA_RIGHT, EYE_GLOW_SCLERA_LEFT,
            THIRD_EYE,
            NATURAL_ZEBRA, ROAN, RABICANO, TOBIANO,
            LEOPARD, EDNRB, KIT, MANCHADO, MITF, PAX3,
            MILK, BODY_SIZE, MAGIC_SPEED, MAGIC_HEALTH, MAGIC_JUMP,
            MAGIC_SWIM_SPEED, MAGIC_WATER_BREATHING, MAGIC_FIGHTER,
            MAGIC_MILK_VOLUME, MAGIC_MEAT, MAGIC_ITEM_DROP, MAGIC_ON_DEATH, MAGIC_MOB_AURA,
            MAGIC_NIGHT_TEMPER, MAGIC_NIGHT_WATCH,
            MANE_COLOR, TAIL_COLOR, PARTICLE, RAINBOW_DUST, MOLTEN_HOOVES, LYCAN,
            FIREPROOF, BIRD_BONED, OCEAN_BORN, HYDROPHOBIC,
            HOT_BLOODED, DRYAD, INTIMIDATING, MEOWING,
            CLEANSING_LIGHT, HOLY_WARD, ECHOLOCATE, BASE_ALARM,
            MUSIC_ENJOYER, GLADIATOR, GUARDIAN, ENDER_ECHO,
            SPONTANEOUS_BREEDING, EYESIGHT, WEATHER_SPEED, WEATHER_JUMP,
            FOOD_PREFERENCE, POTION_MILK, EGG_LAYER, SINGER,
            PACK_LEADER, SPAWNER,
            LIGHT, HEALER, SECTORAL_EYES, VERDANT, LUT, CUTIE_MARK,
            DHAMPIR, SHADOWCREATURE,
            MSTN, PDK4, CKM, RYR2, LCORL, HMGA2, PATN1, PATN2,
            ACAN, B4GALT7, PLOD1, RAPGEF5, ST14, SHOX, MET,
            PPIB, PRKDC, MYO5A, TOE1, CVM, GBE1, MEGAESOPHAGUS, SCN4A, GYS1);

    /** Ordering: lower priority first, ties broken alphabetically by key. */
    private static final Comparator<Gene> BY_PRIORITY_THEN_KEY =
            Comparator.comparingInt(Gene::priority).thenComparing(Gene::key);

    private static final List<SpecGene> LOADED = new ArrayList<>();

    /**
     * Hand-written genes registered from outside this class - another mod's, or
     * a test's. Separate from {@link #LOADED} only because
     * {@link #clearLoaded()} puts the drop-in population back and cannot put
     * these back: a Java gene is an object somebody handed us, not a file we
     * can re-read.
     */
    private static final List<Gene> EXTRA = new ArrayList<>();

    /** @see #freeze() */
    private static volatile boolean frozen;

    private static volatile List<Gene> order = List.of();
    private static volatile List<Gene> naturalOrder = List.of();
    private static volatile List<Gene> magicalOrder = List.of();
    private static volatile Map<String, Gene> byKey = CommonMaps.empty();
    private static volatile Map<String, Allele> alleleByKey = CommonMaps.empty();
    private static volatile Set<String> coatInfluencing = Set.of();

    static {
        rebuild();
        loadBuiltinSpecs();
    }

    /**
     * Register the gene files shipped inside the jar -
     * {@code horsegenetics/genes/} and its {@code index.json}.
     *
     * <p>It runs from the class initialiser rather than from a mod entry point
     * because the registry decides the genotype code's layout, and a code
     * written before these arrived is a different code. Doing it here means
     * there is no window in which some caller has already read
     * {@link #codeOrder()} and got a shorter answer.
     *
     * <p>In the browser this finds nothing and says nothing:
     * {@code getResourceAsStream} is the weakest thing TeaVM does, so the
     * designer hands the same genes in as a bundle instead
     * ({@link #registerBundle}). A missing index is not an error anywhere - a
     * build with no data-driven genes is a legitimate build.
     */
    private static void loadBuiltinSpecs() {
        GeneSpecLoader.Result result = GeneSpecLoader.fromClasspath();
        List<String> problems = new ArrayList<>(result.errors());
        for (GeneSpec spec : result.specs()) {
            if (byKey.containsKey(spec.key())) {
                continue;   // already registered - this is a re-entry, not a collision
            }
            try {
                register(new SpecGene(spec));
            } catch (RuntimeException e) {
                problems.add("could not register " + spec.key() + ": " + e.getMessage());
            }
        }
        for (String problem : problems) {
            CommonLog.warn("built-in gene: " + problem);
        }
    }

    /**
     * Register a bundle - every gene file as one JSON array, which is what
     * {@code wiki/horse-designer/assets/genes.json} is. The browser's way in;
     * see {@link #loadBuiltinSpecs}.
     *
     * @return everything worth telling the page about, empty when all is well
     */
    public static synchronized List<String> registerBundle(String json, String source) {
        List<String> problems = new ArrayList<>();
        List<GeneSpec> parsed;
        try {
            parsed = GeneSpecParser.parseAll(json, source, problems::add);
        } catch (RuntimeException e) {
            String message = String.valueOf(e.getMessage());
            CommonLog.warn(message);
            return List.of(message);
        }
        for (GeneSpec spec : parsed) {
            if (byKey.containsKey(spec.key())) {
                // The jar's own index did load - nothing to do, and re-registering
                // would throw. Harmless, and worth not being loud about.
                continue;
            }
            try {
                register(new SpecGene(spec));
            } catch (RuntimeException e) {
                problems.add("could not register " + spec.key() + ": " + e.getMessage());
            }
        }
        return List.copyOf(problems);
    }

    private Genes() {}

    // ------------------------------------------------------------------
    // Registration
    // ------------------------------------------------------------------

    /**
     * <b>Add a gene.</b> Data-driven ({@link SpecGene}) or hand-written Java,
     * this mod's or another's - the registry does not distinguish, and nothing
     * downstream may either.
     *
     * <p>Throws if its key is taken or malformed (both are genuinely
     * unrecoverable); warns, but carries on, if its priority sits outside its
     * phase's conventional band. Call during startup, before
     * {@link #freeze()}; the gene sorts into the one {@code (priority, key)}
     * order, so registration order does not decide where it lands.
     *
     * <p>Third-party genes reach here through
     * {@code neoforge.api.RegisterHorseGenesEvent}, which is the supported way
     * in - it fires at the one moment when every mod has been constructed and
     * nothing has parsed a genotype code yet. Calling this directly from a mod
     * constructor also works, and is subject to mod load order, which the event
     * exists to take off the caller.
     */
    public static synchronized void register(Gene gene) {
        requireOpen(gene);
        validateKey(gene.key());
        if (byKey.containsKey(gene.key())) {
            throw new IllegalArgumentException("a gene is already registered under " + gene.key());
        }
        checkBand(gene);
        // SpecGenes are the drop-in population clearLoaded() puts back; a Java
        // gene from another mod is not reloadable and is kept separately.
        if (gene instanceof SpecGene spec) {
            LOADED.add(spec);
        } else {
            EXTRA.add(gene);
        }
        rebuild();
    }

    public static synchronized void registerAll(Collection<? extends Gene> genes) {
        for (Gene g : genes) {
            register(g);
        }
    }

    /**
     * A gene key is {@code <namespace>.<gene>} - the mod that owns it, then the
     * locus, both in {@code [a-z0-9_]}.
     *
     * <p>Checked rather than trusted because the namespace is the <b>only</b>
     * thing keeping two mods that both ship a "dun" from colliding, and the
     * collision is not a crash: the second registration throws, is logged, and
     * that mod's gene is silently missing from every horse in the world.
     * Refusing a bare key at the point of registration turns that into a
     * sentence naming the mod that got it wrong.
     */
    private static void validateKey(String key) {
        int dot = key.indexOf('.');
        if (dot <= 0 || dot != key.lastIndexOf('.') || dot == key.length() - 1) {
            throw new IllegalArgumentException("gene key '" + key
                    + "' must be <modid>.<gene> - exactly one dot, neither half empty");
        }
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '.';
            if (!ok) {
                throw new IllegalArgumentException("gene key '" + key + "' has '" + c
                        + "' in it - keys are lower-case [a-z0-9_] either side of the dot");
            }
        }
    }

    private static void requireOpen(Gene gene) {
        if (frozen) {
            throw new IllegalStateException("the gene registry is frozen - " + gene.key()
                    + " is too late. Every registration moves where a gene sits in the genotype"
                    + " code, so a gene added after the code has been read would make every code"
                    + " already parsed mean something else. Register from"
                    + " RegisterHorseGenesEvent.");
        }
    }

    /**
     * <b>No more genes.</b> Called once, when every mod has had its turn, and
     * what it buys is a <i>sentence</i> instead of a mystery.
     *
     * <p>The three orderings and the two key maps are already computed once and
     * cached - {@link #rebuild()} does that on every registration, and they are
     * on the hot path for every genotype-code parse - so freezing does not make
     * anything faster. What it does is make late registration <b>loud</b>: a
     * gene registered after a code has been parsed silently changes what every
     * code already read means, and the symptom is horses whose genes have all
     * shifted by one segment, which reads as a breeding bug and is not one.
     *
     * <p>Idempotent. {@link #clearLoaded()} thaws, for tests.
     */
    public static synchronized void freeze() {
        frozen = true;
    }

    /** Is the registry closed to new genes? */
    public static boolean isFrozen() {
        return frozen;
    }

    /** Every data-driven gene currently registered, in gene order. */
    public static List<SpecGene> loaded() {
        List<SpecGene> out = new ArrayList<>(LOADED);
        out.sort(BY_PRIORITY_THEN_KEY);
        return List.copyOf(out);
    }

    /**
     * Drop every <b>drop-in</b> gene, back to the built-ins - which now includes
     * the gene files shipped in the jar, so those are re-registered on the way
     * out.
     *
     * <p>Putting them back is the whole of the method. It used to be
     * {@code LOADED.clear()} and nothing else, which was correct while the jar
     * shipped no gene files: there was nothing to put back. Now there are
     * dozens, and a test that cleared without restoring left every test after
     * it in the same JVM looking at a shorter genotype code - which surfaced as
     * an unrelated gene inheriting the wrong particle trail, and took a while to
     * recognise as an ordering problem rather than a breeding one.
     */
    public static synchronized void clearLoaded() {
        LOADED.clear();
        EXTRA.clear();
        // Thaw: this is the tests' reset, and a frozen registry could not put
        // the shipped gene files back. Nothing in the game calls it.
        frozen = false;
        rebuild();
        loadBuiltinSpecs();
    }

    private static void checkBand(Gene gene) {
        boolean magicalByNumber = gene.priority() >= MAGICAL_BAND_START;
        if (gene.isNatural() && magicalByNumber) {
            CommonLog.warn("gene {0} is natural but its priority {1} is in the magical band (>= {2})",
                    gene.key(), gene.priority(), MAGICAL_BAND_START);
        } else if (!gene.isNatural() && !magicalByNumber) {
            CommonLog.warn("gene {0} is magical but its priority {1} is in the natural band (< {2})",
                    gene.key(), gene.priority(), MAGICAL_BAND_START);
        }
    }

    private static void rebuild() {
        LOADED.sort(BY_PRIORITY_THEN_KEY);

        List<Gene> all = new ArrayList<>(BUILTINS.size() + LOADED.size() + EXTRA.size());
        all.addAll(BUILTINS);
        all.addAll(LOADED);
        all.addAll(EXTRA);
        all.sort(BY_PRIORITY_THEN_KEY);
        order = List.copyOf(all);

        List<Gene> natural = new ArrayList<>();
        List<Gene> magical = new ArrayList<>();
        for (Gene g : order) {
            (g.isNatural() ? natural : magical).add(g);
        }
        naturalOrder = List.copyOf(natural);
        magicalOrder = List.copyOf(magical);

        Map<String, Gene> keys = new LinkedHashMap<>();
        Map<String, Allele> alleles = new LinkedHashMap<>();
        for (Gene g : order) {
            keys.put(g.key(), g);
            for (Allele a : g.alleles()) {
                alleles.put(a.key(), a);
            }
        }
        byKey = CommonMaps.copyOf(keys);
        alleleByKey = CommonMaps.copyOf(alleles);

        // "Does this gene change how the horse looks?" - asked often enough
        // (every editor row, every frame) to be worth a set rather than a walk.
        Set<String> influencing = new LinkedHashSet<>();
        for (Gene g : order) {
            if (g.affectsCoat()) {
                influencing.add(g.key());
                influencing.addAll(g.coatDependsOn());
            }
        }
        coatInfluencing = Set.copyOf(influencing);

        // Built-ins never pass through register(SpecGene), so the sex-linked
        // declaration check has to live here to cover both.
        for (Gene g : order) {
            validateInheritance(g);
        }

        GenotypeCatalog.invalidate();
        SpliceSafety.invalidate();
        SpliceCategory.invalidate();
    }

    // ------------------------------------------------------------------
    // Lookup
    // ------------------------------------------------------------------

    public static List<Gene> codeOrder() {
        return order;
    }

    public static List<Gene> naturalOrder() {
        return naturalOrder;
    }

    public static List<Gene> magicalOrder() {
        return magicalOrder;
    }

    public static List<Gene> all() {
        return order;
    }

    /**
     * Does this gene change how a horse <b>looks</b> - directly or through
     * another gene that reads it?
     *
     * <p>{@link Gene#affectsCoat()} alone is not the question. {@code PATN1},
     * {@code PATN2} and {@link #SHADE} paint nothing on their own and every one
     * of their expressions is a wild type, yet a horse carrying them looks
     * different, because a painter names them in {@link Gene#coatDependsOn()}.
     * The relationship is declared, so this reads the declaration rather than
     * keeping a list of exceptions beside it.
     *
     * <p>This is the line the editors' <i>"randomize the health genes too?"</i>
     * switch is drawn on, and the line {@code DesignerApi.showsAs} starts from:
     * everything it returns {@code false} for is real, heritable and completely
     * invisible.
     */
    public static boolean influencesCoat(Gene gene) {
        return coatInfluencing.contains(gene.key());
    }

    public static Gene byKey(String geneKey) {
        Gene g = byKey.get(geneKey);
        if (g == null) {
            throw new IllegalArgumentException("no gene registered under " + geneKey);
        }
        return g;
    }

    /**
     * The gene registered under {@code geneKey}, or {@code null} if none - a
     * genotype-code segment naming an unregistered gene is dropped, not an
     * error, so parsing can stay tolerant across a gene being added or removed.
     */
    public static Gene byKeyOrNull(String geneKey) {
        return byKey.get(geneKey);
    }

    public static Allele allele(String alleleKey) {
        Allele a = alleleByKey.get(alleleKey);
        if (a == null) {
            throw new IllegalArgumentException("no allele registered under " + alleleKey);
        }
        return a;
    }

    /**
     * A sex-linked gene has to declare its reserved placeholder allele, and
     * declare it at the end of {@link Gene#alleles()} that the placeholder slot
     * sorts to - the {@code Y} <b>last</b> on an {@code X}-linked gene, the
     * {@code X} <b>first</b> on a {@code Y}-linked one, because
     * {@link AllelePair} canonicalises on declaration order.
     *
     * <p>Checked at registration rather than left to the author, because the
     * failure mode is not an exception: a placeholder declared in the wrong
     * place produces pairs that sort the wrong way round, so a stallion's
     * hemizygous allele lands in the slot the epigenome and every
     * {@code expressionOf} treat as the second copy, and the gene quietly
     * misbehaves for exactly one sex.
     */
    private static void validateInheritance(Gene gene) {
        Inheritance mode = gene.inheritance();
        if (!mode.sexLinked()) {
            return;
        }
        Allele placeholder = gene.hemizygousPlaceholder(); // throws if undeclared
        List<Allele> alleles = gene.alleles();
        int expected = mode == Inheritance.X_LINKED ? alleles.size() - 1 : 0;
        if (placeholder.order() != expected) {
            throw new IllegalArgumentException(gene.key() + " is " + mode + " so its reserved '"
                    + placeholder.token() + "' allele must be declared "
                    + (expected == 0 ? "first" : "last") + " in alleles(), not at index "
                    + placeholder.order());
        }
    }

}
