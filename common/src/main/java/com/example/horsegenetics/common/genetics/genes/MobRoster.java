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
 * <h2>Lycan has not been migrated</h2>
 * It predates this table and keeps its own copy, because its allele order is
 * baked into the genotype code and into two checked-in goldens - moving it is a
 * real change with a real diff, not a tidy-up. The lists are identical today.
 * Recorded on {@code wiki/known-gaps.html} so the drift this class exists to
 * prevent does not happen to the one gene it does not yet cover.
 */
public final class MobRoster {

    private MobRoster() {
    }

    /** One entry: the allele token, the mob id, and the label a player reads. */
    public record Entry(String token, String mob, String label) {
    }

    private static final List<Entry> PEACEFUL = new ArrayList<>();
    private static final List<Entry> HOSTILE = new ArrayList<>();

    private static void peaceful(String token, String mob, String label) {
        PEACEFUL.add(new Entry(token, mob, label));
    }

    private static void hostile(String token, String mob, String label) {
        HOSTILE.add(new Entry(token, mob, label));
    }

    static {
        // Alphabetical by mob id, and the same set and order LycanGene uses.
        peaceful("Aly", "minecraft:allay", "Allay");
        peaceful("Arma", "minecraft:armadillo", "Armadillo");
        peaceful("Axo", "minecraft:axolotl", "Axolotl");
        peaceful("Bat", "minecraft:bat", "Bat");
        peaceful("Bee", "minecraft:bee", "Bee");
        peaceful("Cml", "minecraft:camel", "Camel");
        peaceful("Cat", "minecraft:cat", "Cat");
        peaceful("Chk", "minecraft:chicken", "Chicken");
        peaceful("Cod", "minecraft:cod", "Cod");
        peaceful("Cow", "minecraft:cow", "Cow");
        peaceful("Dol", "minecraft:dolphin", "Dolphin");
        peaceful("Fox", "minecraft:fox", "Fox");
        peaceful("Frg", "minecraft:frog", "Frog");
        peaceful("Glsq", "minecraft:glow_squid", "Glow squid");
        peaceful("Gt", "minecraft:goat", "Goat");
        peaceful("Ghst", "minecraft:happy_ghast", "Happy ghast");
        peaceful("Lma", "minecraft:llama", "Llama");
        peaceful("Mshr", "minecraft:mooshroom", "Mooshroom");
        peaceful("Ntls", "minecraft:nautilus", "Nautilus");
        peaceful("Oce", "minecraft:ocelot", "Ocelot");
        peaceful("Pnd", "minecraft:panda", "Panda");
        peaceful("Prt", "minecraft:parrot", "Parrot");
        peaceful("Pig", "minecraft:pig", "Pig");
        peaceful("Plr", "minecraft:polar_bear", "Polar bear");
        peaceful("Pff", "minecraft:pufferfish", "Pufferfish");
        peaceful("Rbt", "minecraft:rabbit", "Rabbit");
        peaceful("Slm", "minecraft:salmon", "Salmon");
        peaceful("Shp", "minecraft:sheep", "Sheep");
        peaceful("Snf", "minecraft:sniffer", "Sniffer");
        peaceful("Sqd", "minecraft:squid", "Squid");
        peaceful("Strd", "minecraft:strider", "Strider");
        peaceful("Tdp", "minecraft:tadpole", "Tadpole");
        peaceful("Tlma", "minecraft:trader_llama", "Trader llama");
        peaceful("Trpf", "minecraft:tropical_fish", "Tropical fish");
        peaceful("Trt", "minecraft:turtle", "Turtle");
        peaceful("Wtr", "minecraft:wandering_trader", "Wandering trader");
        peaceful("Wlf", "minecraft:wolf", "Wolf");

        // The monsters. Only SpawnerGene uses these, and only ever as something
        // somebody engineered on purpose - no founder table contains one.
        hostile("Blz", "minecraft:blaze", "Blaze");
        hostile("Bog", "minecraft:bogged", "Bogged");
        hostile("Brz", "minecraft:breeze", "Breeze");
        hostile("Cvsp", "minecraft:cave_spider", "Cave spider");
        hostile("Crp", "minecraft:creeper", "Creeper");
        hostile("Drw", "minecraft:drowned", "Drowned");
        hostile("End", "minecraft:enderman", "Enderman");
        hostile("Endm", "minecraft:endermite", "Endermite");
        hostile("Evk", "minecraft:evoker", "Evoker");
        hostile("Ghs", "minecraft:ghast", "Ghast");
        hostile("Gua", "minecraft:guardian", "Guardian");
        hostile("Hgl", "minecraft:hoglin", "Hoglin");
        hostile("Hsk", "minecraft:husk", "Husk");
        hostile("Mag", "minecraft:magma_cube", "Magma cube");
        hostile("Phn", "minecraft:phantom", "Phantom");
        hostile("Pig2", "minecraft:piglin", "Piglin");
        hostile("Plg", "minecraft:pillager", "Pillager");
        hostile("Rav", "minecraft:ravager", "Ravager");
        hostile("Shk", "minecraft:shulker", "Shulker");
        hostile("Slv", "minecraft:silverfish", "Silverfish");
        hostile("Skl", "minecraft:skeleton", "Skeleton");
        hostile("Slme", "minecraft:slime", "Slime");
        hostile("Spd", "minecraft:spider", "Spider");
        hostile("Stry", "minecraft:stray", "Stray");
        hostile("Vex", "minecraft:vex", "Vex");
        hostile("Vin", "minecraft:vindicator", "Vindicator");
        hostile("Wtch", "minecraft:witch", "Witch");
        hostile("Wth", "minecraft:wither_skeleton", "Wither skeleton");
        hostile("Zmb", "minecraft:zombie", "Zombie");
        hostile("Zvil", "minecraft:zombie_villager", "Zombie villager");
    }

    /** Every non-hostile vanilla mob, minus the horse family. */
    public static List<Entry> peaceful() {
        return List.copyOf(PEACEFUL);
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
