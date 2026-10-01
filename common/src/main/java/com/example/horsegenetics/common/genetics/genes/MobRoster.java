package com.example.horsegenetics.common.genetics.genes;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>The mobs a per-mob locus may name</b>, in one table.
 *
 * <p>Three genes have an allele per creature - {@link LycanGene} turns the horse
 * into one, {@link PackLeaderGene} makes one follow it, {@link SpawnerGene}
 * makes it produce them - and the alternative to this class is three lists that
 * agree today and drift the first time the game adds a mob. Ids are plain
 * strings because {@code common/} may not import Minecraft; the translator
 * resolves them against the live registry, and an id this game version has never
 * heard of simply does nothing.
 *
 * <h2>Two rosters, and the rule for each</h2>
 * {@link #peaceful()} is every vanilla mob in a <b>non-hostile spawn
 * category</b> minus the horse family - a mechanical rule rather than a taste,
 * which is what lets the list contain a pufferfish and a wandering trader
 * without anybody having to defend either. {@link #hostile()} is the monsters,
 * and exists only because {@link SpawnerGene} deliberately reaches further than
 * the other two.
 *
 * <p><b>Token stability matters.</b> A token is part of the genotype code, so
 * renaming one or reordering the list changes every saved horse. Add to the end
 * of a roster; never reorder it.
 *
 * <h2>Where the mob lives, and why a gene may care</h2>
 * Every entry carries a {@link Habitat}. Two of the three genes do not look at
 * it - a pack of bees or a squid spawner is a fine thing to breed for, because
 * the mob is a <i>companion</i> and the horse stays a horse. {@link LycanGene}
 * is the one that <i>becomes</i> the mob, and there the habitat decides whether
 * the allele may exist at all: {@link #ground()} is its whole allele set, so a
 * horse can never be shut in a body that drowns on land or flies out of reach
 * of its owner. The tag is the blacklist - a mob added to this table as
 * {@link Habitat#WATER} or {@link Habitat#AIR} cannot reach the lycan locus by
 * being forgotten about.
 *
 * <h2>Modded mobs</h2>
 * Not listed here, and never will be: each locus has one more allele,
 * {@link #CHAOS_TOKEN Chaos}, that stands in for every modded mob at once - see
 * the section at the bottom of this class, and {@code neoforge/server/ChaosRoster}.
 */
public final class MobRoster {

    private MobRoster() {
    }

    /**
     * <b>Where the mob lives.</b> Not Minecraft's {@code MobCategory} - that is
     * a spawn rule and {@code common/} cannot see it anyway - but the coarser
     * question a gene actually asks: could a horse wear this body and still be
     * a horse standing in a field?
     */
    public enum Habitat {
        /** Walks on land. Everything a horse could be swapped for safely. */
        GROUND,
        /** Drowns, or suffocates, out of water: the fish, the squids, the axolotl. */
        WATER,
        /** Flies: it would leave the ground, and its owner, behind. */
        AIR
    }

    /** One entry: the allele token, the mob id, the label a player reads, and where it lives. */
    public record Entry(String token, String mob, String label, Habitat habitat) {
    }

    private static final List<Entry> PEACEFUL = new ArrayList<>();
    private static final List<Entry> HOSTILE = new ArrayList<>();

    private static void peaceful(String token, String mob, String label, Habitat habitat) {
        PEACEFUL.add(new Entry(token, mob, label, habitat));
    }

    private static void hostile(String token, String mob, String label, Habitat habitat) {
        HOSTILE.add(new Entry(token, mob, label, habitat));
    }

    static {
        // Alphabetical by mob id.
        peaceful("Aly", "minecraft:allay", "Allay", Habitat.AIR);
        peaceful("Arma", "minecraft:armadillo", "Armadillo", Habitat.GROUND);
        peaceful("Axo", "minecraft:axolotl", "Axolotl", Habitat.WATER);
        peaceful("Bat", "minecraft:bat", "Bat", Habitat.AIR);
        peaceful("Bee", "minecraft:bee", "Bee", Habitat.AIR);
        peaceful("Cml", "minecraft:camel", "Camel", Habitat.GROUND);
        peaceful("Cat", "minecraft:cat", "Cat", Habitat.GROUND);
        peaceful("Chk", "minecraft:chicken", "Chicken", Habitat.GROUND);
        peaceful("Cod", "minecraft:cod", "Cod", Habitat.WATER);
        peaceful("Cow", "minecraft:cow", "Cow", Habitat.GROUND);
        peaceful("Dol", "minecraft:dolphin", "Dolphin", Habitat.WATER);
        peaceful("Fox", "minecraft:fox", "Fox", Habitat.GROUND);
        // Amphibious, and it walks: a frog out of water is a frog, not a
        // suffocating one. Same for the turtle, and not for the tadpole.
        peaceful("Frg", "minecraft:frog", "Frog", Habitat.GROUND);
        peaceful("Glsq", "minecraft:glow_squid", "Glow squid", Habitat.WATER);
        peaceful("Gt", "minecraft:goat", "Goat", Habitat.GROUND);
        peaceful("Ghst", "minecraft:happy_ghast", "Happy ghast", Habitat.AIR);
        peaceful("Lma", "minecraft:llama", "Llama", Habitat.GROUND);
        peaceful("Mshr", "minecraft:mooshroom", "Mooshroom", Habitat.GROUND);
        peaceful("Ntls", "minecraft:nautilus", "Nautilus", Habitat.WATER);
        peaceful("Oce", "minecraft:ocelot", "Ocelot", Habitat.GROUND);
        peaceful("Pnd", "minecraft:panda", "Panda", Habitat.GROUND);
        peaceful("Prt", "minecraft:parrot", "Parrot", Habitat.AIR);
        peaceful("Pig", "minecraft:pig", "Pig", Habitat.GROUND);
        peaceful("Plr", "minecraft:polar_bear", "Polar bear", Habitat.GROUND);
        peaceful("Pff", "minecraft:pufferfish", "Pufferfish", Habitat.WATER);
        peaceful("Rbt", "minecraft:rabbit", "Rabbit", Habitat.GROUND);
        peaceful("Slm", "minecraft:salmon", "Salmon", Habitat.WATER);
        peaceful("Shp", "minecraft:sheep", "Sheep", Habitat.GROUND);
        peaceful("Snf", "minecraft:sniffer", "Sniffer", Habitat.GROUND);
        peaceful("Sqd", "minecraft:squid", "Squid", Habitat.WATER);
        // Lava, not water, and it walks on land perfectly well - shivering.
        peaceful("Strd", "minecraft:strider", "Strider", Habitat.GROUND);
        peaceful("Tdp", "minecraft:tadpole", "Tadpole", Habitat.WATER);
        peaceful("Tlma", "minecraft:trader_llama", "Trader llama", Habitat.GROUND);
        peaceful("Trpf", "minecraft:tropical_fish", "Tropical fish", Habitat.WATER);
        peaceful("Trt", "minecraft:turtle", "Turtle", Habitat.GROUND);
        peaceful("Wtr", "minecraft:wandering_trader", "Wandering trader", Habitat.GROUND);
        peaceful("Wlf", "minecraft:wolf", "Wolf", Habitat.GROUND);

        // The monsters. Only SpawnerGene uses these, and only ever as something
        // somebody engineered on purpose - no founder table contains one.
        hostile("Blz", "minecraft:blaze", "Blaze", Habitat.AIR);
        hostile("Bog", "minecraft:bogged", "Bogged", Habitat.GROUND);
        hostile("Brz", "minecraft:breeze", "Breeze", Habitat.GROUND);
        hostile("Cvsp", "minecraft:cave_spider", "Cave spider", Habitat.GROUND);
        hostile("Crp", "minecraft:creeper", "Creeper", Habitat.GROUND);
        hostile("Drw", "minecraft:drowned", "Drowned", Habitat.GROUND);
        hostile("End", "minecraft:enderman", "Enderman", Habitat.GROUND);
        hostile("Endm", "minecraft:endermite", "Endermite", Habitat.GROUND);
        hostile("Evk", "minecraft:evoker", "Evoker", Habitat.GROUND);
        hostile("Ghs", "minecraft:ghast", "Ghast", Habitat.AIR);
        hostile("Gua", "minecraft:guardian", "Guardian", Habitat.WATER);
        hostile("Hgl", "minecraft:hoglin", "Hoglin", Habitat.GROUND);
        hostile("Hsk", "minecraft:husk", "Husk", Habitat.GROUND);
        hostile("Mag", "minecraft:magma_cube", "Magma cube", Habitat.GROUND);
        hostile("Phn", "minecraft:phantom", "Phantom", Habitat.AIR);
        hostile("Pig2", "minecraft:piglin", "Piglin", Habitat.GROUND);
        hostile("Plg", "minecraft:pillager", "Pillager", Habitat.GROUND);
        hostile("Rav", "minecraft:ravager", "Ravager", Habitat.GROUND);
        hostile("Shk", "minecraft:shulker", "Shulker", Habitat.GROUND);
        hostile("Slv", "minecraft:silverfish", "Silverfish", Habitat.GROUND);
        hostile("Skl", "minecraft:skeleton", "Skeleton", Habitat.GROUND);
        hostile("Slme", "minecraft:slime", "Slime", Habitat.GROUND);
        hostile("Spd", "minecraft:spider", "Spider", Habitat.GROUND);
        hostile("Stry", "minecraft:stray", "Stray", Habitat.GROUND);
        hostile("Vex", "minecraft:vex", "Vex", Habitat.AIR);
        hostile("Vin", "minecraft:vindicator", "Vindicator", Habitat.GROUND);
        hostile("Wtch", "minecraft:witch", "Witch", Habitat.GROUND);
        hostile("Wth", "minecraft:wither_skeleton", "Wither skeleton", Habitat.GROUND);
        hostile("Zmb", "minecraft:zombie", "Zombie", Habitat.GROUND);
        hostile("Zvil", "minecraft:zombie_villager", "Zombie villager", Habitat.GROUND);
    }

    /** Every non-hostile vanilla mob, minus the horse family. */
    public static List<Entry> peaceful() {
        return List.copyOf(PEACEFUL);
    }

    /**
     * The peaceful mobs that walk on land - {@link LycanGene}'s allele set, and
     * the reason {@link Habitat} exists. A body a horse can be put into and
     * later found again.
     */
    public static List<Entry> ground() {
        List<Entry> out = new ArrayList<>();
        for (Entry e : PEACEFUL) {
            if (e.habitat() == Habitat.GROUND) {
                out.add(e);
            }
        }
        return List.copyOf(out);
    }

    /** The monsters. Only the spawner locus reaches these. */
    public static List<Entry> hostile() {
        return List.copyOf(HOSTILE);
    }

    /** Both rosters, peaceful first. The spawner locus's allele set. */
    public static List<Entry> all() {
        List<Entry> out = new ArrayList<>(PEACEFUL);
        out.addAll(HOSTILE);
        return List.copyOf(out);
    }

    // ------------------------------------------------------------------
    // Chaos: every modded mob, through one allele
    // ------------------------------------------------------------------

    /*
     * The roster above is vanilla and hand-written, and it stays that way. A
     * modpack's mobs reach the three loci through ONE extra allele each, Chaos,
     * whose meaning is a seed on the allele copy rather than a mob on the allele
     * - the eye locus's chaos colour, pointed at a registry instead of at a
     * colour wheel. The game module builds the list of eligible modded mobs when
     * the server starts and the seed picks one by position.
     *
     * Why not an allele per modded mob (recorded so it is not reopened): a token
     * is text in the genotype code and registry ids collide with its separators;
     * a live-registry allele list changes with the modpack, so two servers would
     * read one code differently; and the designer, the census and the wiki are
     * baked from here, where no registry exists. One permanent allele answers all
     * three - a removed mod can never leave a horse naming an allele that is gone.
     */

    /** The Chaos allele's token at all three mob loci. Free at every one of them; keep it so. */
    public static final String CHAOS_TOKEN = "Cha";

    /** The Chaos allele's label. */
    public static final String CHAOS_LABEL = "Chaos";

    /** The epigenetic seed a Chaos copy carries - one {@code SEED} value, same name at all three loci. */
    public static final String CHAOS_SEED = "chaos";

    /**
     * The subject a Chaos variant names before a seed is attached. Not a mob id:
     * nothing in the game can resolve it, so a path that forgets to swap it for
     * {@link #chaosSubject(long)} finds no mob and does nothing.
     */
    public static final String CHAOS_SUBJECT = "chaos";

    private static final String CHAOS_PREFIX = "chaos:";

    /**
     * The subject an ability carries for a Chaos pair with this seed -
     * {@code "chaos:<seed>"}, in the field that otherwise holds a mob id, so no
     * ability record needed a new field. Decimal, signed, because the
     * game module parses it back and {@code Long.parseUnsignedLong} is not
     * available to {@code common/}'s browser build.
     */
    public static String chaosSubject(long seed) {
        return CHAOS_PREFIX + seed;
    }

    /** Is {@code subject} a chaos subject rather than a mob id? */
    public static boolean isChaos(String subject) {
        return subject != null && (subject.equals(CHAOS_SUBJECT) || subject.startsWith(CHAOS_PREFIX));
    }

    /** The seed in a {@link #chaosSubject}, or {@code 0} for the bare {@link #CHAOS_SUBJECT} or a malformed one. */
    public static long chaosSeed(String subject) {
        if (subject == null || !subject.startsWith(CHAOS_PREFIX)) {
            return 0L;
        }
        try {
            return Long.parseLong(subject.substring(CHAOS_PREFIX.length()));
        } catch (NumberFormatException bad) {
            return 0L;
        }
    }

    /**
     * <b>Which entry a seed picks</b> from a list of {@code size}: the seed modulo
     * the size, never negative. By position, so adding or removing a mod moves
     * what an existing seed means - the owner's call: it is chaos. {@code -1}
     * when the list is empty, which is "no modded animal available".
     */
    public static int chaosPick(long seed, int size) {
        return size <= 0 ? -1 : (int) Math.floorMod(seed, (long) size);
    }
}
