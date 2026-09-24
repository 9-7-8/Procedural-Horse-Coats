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
}
