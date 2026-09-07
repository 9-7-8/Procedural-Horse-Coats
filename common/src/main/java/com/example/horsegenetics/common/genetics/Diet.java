package com.example.horsegenetics.common.genetics;

/**
 * <b>What a horse will eat</b>, and how much good it does it.
 *
 * <p>{@link #NORMAL} is the whole population minus a handful: an ordinary horse
 * eats what an ordinary Minecraft horse eats and heals the way vanilla heals
 * it, and this enum has nothing to say about it. Every other value is a
 * <b>narrow</b> diet - the horse refuses everything else - and pays for that
 * narrowness with efficiency, which is the entire trade the
 * {@link com.example.horsegenetics.common.genetics.genes.DietGene} locus
 * offers: <i>fewer things will feed this horse, and the ones that do feed it
 * feed it far better</i>.
 *
 * <h2>Which items count is not decided here</h2>
 * This is {@code common/}, so it names no item. Each value is a
 * <b>category</b>, and the NeoForge module holds the category&rarr;item table
 * ({@code server.DietFoods}) exactly the way {@code RarityItems} holds the
 * rarity&rarr;item one. {@link #variants()} is the one number the two sides have
 * to agree on: a diet with {@code n} variants is really {@code n} diets - the
 * horse eats <i>one</i> metal, not metal in general - and which one is a
 * per-horse roll off the allele copy, so the game module must offer exactly
 * {@code n} items for it.
 *
 * <h2>Healing</h2>
 * {@link #healPoints()} is in health points (two to a heart), and
 * {@link #healsFully()} means what it says: the horse goes to full health from
 * one item however hurt it was. The numbers grade by <b>breadth</b>, because
 * that is the only axis that makes the trade legible - a horse that eats
 * anything gains the least per item and a horse that eats one metal gains
 * everything.
 */
public enum Diet {

    /**
     * An ordinary horse. Vanilla decides what it eats and vanilla heals it;
     * nothing in this mod intervenes. This is the value every horse without a
     * variant diet locus resolves to.
     */
    NORMAL("normal", "Ordinary feed", 0.0, 0),

    /**
     * <b>Nothing at all.</b> Not a diet allele - no combination of the diet
     * locus produces it - but a value another gene can claim, for a horse that
     * cannot be fed by any means. It exists so "cannot be fed" is a diet rather
     * than a special case bolted beside one.
     */
    NOTHING("nothing", "Cannot be fed", 0.0, 0),

    /** Every edible thing there is, and the least out of each. */
    ANYTHING("anything", "Anything edible", 4.0, 0),

    RAW_MEAT("raw-meat", "Raw meat only", 20.0, 0),
    FISH("fish", "Fish only", 20.0, 0),
    RAW_VEGETABLES("raw-vegetables", "Raw vegetables only", 20.0, 0),
    WHEAT("wheat", "Wheat only", 20.0, 0),

    /** Cooked and prepared food - the broadest of the narrow diets, so the weakest. */
    HUMAN_FOOD("human-food", "Cooked food only", 10.0, 0),

    CAKE("cake", "Cake only", -1.0, 0),   // FULL
    POTION("potion", "Potions only", -1.0, 0),   // FULL
    LAVA("lava", "Lava only", -1.0, 0),   // FULL
    WATER("water", "Water only", 20.0, 0),

    /** One metal, chosen per horse - see {@link #variants()}. */
    INGOT("ingot", "One metal only", -1.0, 4),   // FULL
    /** One gem, chosen per horse - see {@link #variants()}. */
    GEM("gem", "One gem only", -1.0, 5);   // FULL

    /**
     * {@link #healPoints()} sentinel: this diet heals the horse completely.
     * Written literally in the constants above rather than by name - an enum
     * constant's arguments cannot forward-reference a field of the enum.
     */
    public static final double FULL = -1.0;

    private final String id;
    private final String label;
    private final double heal;
    private final int variants;

    Diet(String id, String label, double heal, int variants) {
        this.id = id;
        this.label = label;
        this.heal = heal;
        this.variants = variants;
    }

    /** Stable slug - the {@link Expression#id()} the diet locus uses for it. */
    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    /** Health points one item restores. Meaningless when {@link #healsFully()}. */
    public double healPoints() {
        return heal;
    }

    /** Does one item take the horse to full health however hurt it is? */
    public boolean healsFully() {
        return heal == FULL;
    }

    /**
     * How many <b>different</b> diets this value really is. {@code 0} for all
     * but two: an {@link #INGOT} horse eats one metal and a {@link #GEM} horse
     * one gem, rolled per horse off the expressing allele copy's epigenetic
     * seed, so two ingot-eaters out of the same dam may want different bars.
     * The game module must offer exactly this many items for the category.
     */
    public int variants() {
        return variants;
    }

    /** Is this a diet the mod actually intervenes for? False only for {@link #NORMAL}. */
    public boolean isSpecial() {
        return this != NORMAL;
    }
}
