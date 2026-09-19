package com.example.horsegenetics.neoforge.carts;

import com.example.horsegenetics.neoforge.compat.ModdedMaterials;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.WoodType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * <b>A wood a cart can be built out of</b> - vanilla's twelve and one for every
 * wood any other loaded mod adds, on the same footing.
 *
 * <h2>Why this is not {@link WoodType}</h2>
 * Upstream keyed every cart on vanilla's {@code WoodType} and listed the twelve
 * by hand. That list is the reason a modded wood could never have a cart: the
 * carts are built from a table, and the table stopped at bamboo.
 *
 * <p>{@code WoodType} is also the wrong key even where it exists. Its
 * {@link WoodType#name()} is the <i>sound</i> family, a modded one may register
 * as {@code modid:maple} or {@code modid/maple} depending on how its author
 * built it, and nothing in it names the three block textures a cart is
 * assembled from. So a cart's wood is this record instead: an id, a label, and
 * the three sprites, resolved once and read by everything.
 *
 * <p>The id is what appears in registry paths ({@code oak_wagon},
 * {@code biomesoplenty_maple_plow}) and what is written into the entity's synced
 * data and its save tag, so <b>an id is permanent</b> once a world has used it.
 *
 * @param id          registry path prefix: {@code oak}, or {@code <namespace>_<name>} for a modded wood
 * @param label       the English name for the lang file: "Oak", "Maple"
 * @param planks      block-atlas sprite for the body of the cart
 * @param log         block-atlas sprite for the axle and shafts; may not exist, see {@link #logOrPlanks}
 * @param strippedLog block-atlas sprite for the hub; may not exist
 * @param plankItem   the item id a recipe spends, or null if this wood has no plank item
 * @param shippedIcon whether {@code textures/item/<id>_<cart>.png} is a painted icon in this jar
 */
public record CartWood(String id, String label, Identifier planks, Identifier log,
                       Identifier strippedLog, String plankItem, boolean shippedIcon,
                       boolean hasStrippedLog) {

    /**
     * <b>Does this wood get a wagon?</b> Only the wagon spends a stripped log, so
     * a wood whose mod never shipped one gets the other five carts and no sixth.
     *
     * <p>Before this, nine real modded woods in a 332-jar pack registered a wagon
     * that could not be crafted and said so once a boot -
     * {@code evilcraft:undead}, {@code forbidden_arcanus:edelwood},
     * {@code integrateddynamics:menril}, four {@code regions_unexplored}
     * bioshrooms and {@code undergarden:ancient_root}. Vanilla's twelve all have
     * one.
     */
    public boolean has(final com.example.horsegenetics.common.cart.CartKind kind) {
        return this.hasStrippedLog
                || kind != com.example.horsegenetics.common.cart.CartKind.WAGON;
    }

    /**
     * Vanilla's twelve, as {@code {name, log suffix, label}}.
     *
     * <p>The suffix is the irregular column, and the reason this is a table
     * rather than {@code name + "_log"}: the nether woods are stems and bamboo
     * is a block. Getting one wrong costs a purple cart, and nothing logs it.
     */
    private static final String[][] VANILLA = {
            {"oak", "log", "Oak"},
            {"spruce", "log", "Spruce"},
            {"birch", "log", "Birch"},
            {"jungle", "log", "Jungle"},
            {"acacia", "log", "Acacia"},
            {"dark_oak", "log", "Dark Oak"},
            {"pale_oak", "log", "Pale Oak"},
            {"mangrove", "log", "Mangrove"},
            {"cherry", "log", "Cherry"},
            {"bamboo", "block", "Bamboo"},
            {"crimson", "stem", "Crimson"},
            {"warped", "stem", "Warped"},
    };

    private static List<CartWood> all;

    /**
     * Every wood a cart exists in, vanilla first and then the modded ones in
     * {@link ModdedMaterials}' own sorted order, so registration is
     * deterministic across launches.
     *
     * <p>Built once and cached. Safe to call from
     * {@link HorseCarts}'s registration pass, which runs well after
     * {@code ModdedMaterials.scan()} in the mod constructor; calling it before
     * the scan would silently produce vanilla only.
     */
    public static List<CartWood> all() {
        if (all == null) {
            final List<CartWood> built = new ArrayList<>();
            for (final String[] row : VANILLA) {
                final String name = row[0];
                built.add(new CartWood(
                        name,
                        row[2],
                        vanillaSprite(name + "_planks"),
                        vanillaSprite(name + "_" + row[1]),
                        vanillaSprite("stripped_" + name + "_" + row[1]),
                        "minecraft:" + name + "_planks",
                        true,
                        true));
            }
            final java.util.Set<String> ids = new java.util.HashSet<>();
            for (final CartWood wood : built) {
                ids.add(wood.id);
            }
            for (final ModdedMaterials.Wood wood : ModdedMaterials.woods()) {
                final CartWood cart = of(wood);
                // A duplicate id here is six duplicate registry keys, and
                // Registry.register throws on one - which in this mod's history
                // means a pack that cannot start. The ids are
                // <namespace>_<name> and a clash needs two mods whose namespace
                // and wood name run together the same way, so this is guarding
                // against the unlikely rather than the expected; the armours
                // were the expected case, and they took v0.5.012 down.
                if (!ids.add(cart.id)) {
                    continue;
                }
                built.add(cart);
            }
            all = List.copyOf(built);
        }
        return all;
    }

    /** The wood with this {@link #id()}, or null - used when reading a save tag. */
    public static CartWood byId(final String id) {
        for (final CartWood wood : all()) {
            if (wood.id.equals(id)) {
                return wood;
            }
        }
        return null;
    }

    /** Oak. The fallback for a save that names a wood whose mod has been removed. */
    public static CartWood fallback() {
        return all().get(0);
    }

    /**
     * A modded wood, guessed from its name by vanilla's own convention.
     *
     * <p>Only the plank sprite is known for certain - {@code ModdedMaterials}
     * resolved it from the mod's own files. The log and stripped-log sprites are
     * <b>guesses</b>: {@code <ns>:block/<name>_log} and
     * {@code stripped_<name>_log}, which is what a mod that follows vanilla's
     * naming will have. A mod that does not gets planks everywhere instead of a
     * missing texture - see {@link #logOrPlanks}. That is a deliberate trade: a
     * slightly plain cart beats a purple one, and there is no way to be sure
     * from a jar listing alone which of a mod's dozen log variants is "the" log.
     */
    private static CartWood of(final ModdedMaterials.Wood wood) {
        final String prefix = wood.namespace() + "_" + wood.name();
        return new CartWood(
                prefix,
                label(wood.name()),
                Identifier.parse(wood.plankTexture()),
                Identifier.fromNamespaceAndPath(wood.namespace(), "block/" + wood.name() + "_log"),
                Identifier.fromNamespaceAndPath(wood.namespace(), "block/stripped_" + wood.name() + "_log"),
                wood.namespace() + ":" + wood.name() + "_planks",
                false,
                wood.strippedLog());
    }

    private static Identifier vanillaSprite(final String name) {
        return Identifier.withDefaultNamespace("block/" + name);
    }

    /** {@code dark_oak} to {@code Dark Oak}; the same shape {@code GeneratedGates.label} uses. */
    private static String label(final String name) {
        final StringBuilder out = new StringBuilder();
        for (final String word : name.split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0)))
               .append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return out.length() == 0 ? name : out.toString();
    }

    /**
     * The log sprite if the atlas actually has one, and the plank sprite if it
     * does not.
     *
     * <p>Client-side, and it has to be: whether a sprite exists is a question
     * only the stitched atlas can answer, and it cannot be asked at scan time
     * when the answer is needed here. {@code missing} is how the atlas reports
     * a sprite it has never heard of.
     *
     * @param present whether the atlas resolved {@link #log()} to a real sprite
     */
    public Identifier logOrPlanks(final boolean present) {
        return present ? this.log : this.planks;
    }

    /** As {@link #logOrPlanks}, for the stripped variant. */
    public Identifier strippedOrPlanks(final boolean present) {
        return present ? this.strippedLog : this.planks;
    }

    /** {@code oak_wagon}, {@code biomesoplenty_maple_plow} - this wood's item and entity path for a cart. */
    public String itemId(final String cartId) {
        return this.id + "_" + cartId;
    }

    /**
     * The lang entries this wood's carts need, {@code item.horsegenetics.<id>_<cart>}
     * to "Oak Wagon". Only for woods with no shipped translation, i.e. the
     * modded ones; vanilla's are in the shipped {@code en_us.json}.
     */
    public void lang(final Map<String, String> out) {
        for (final var kind : com.example.horsegenetics.common.cart.CartKind.values()) {
            if (!has(kind)) {
                continue;   // no item registered, so a name for it would name nothing
            }
            out.put("item.horsegenetics." + itemId(kind.id()),
                    this.label + " " + kindLabel(kind.id()));
        }
    }

    private static String kindLabel(final String cartId) {
        return label(cartId);
    }
}
