package com.example.horsegenetics.neoforge.compat;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.neoforged.fml.ModList;
import net.neoforged.fml.jarcontents.JarContents;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * <b>What the other mods in this pack have brought with them</b> - the woods we
 * can make a double gate for, and the metals we can make horse armour and tack
 * fittings out of.
 *
 * <p>This is the <b>adapter</b>: it hands NeoForge's jars to {@link MaterialScan},
 * keeps the answers, and answers the one question that needs a registry. The
 * scanning itself is next door and has no Minecraft in it, so that it can be
 * tested without one - see {@code MaterialScanTest}.
 *
 * <h2>Why this reads jar files and not the registries</h2>
 * Everything here has to be known <b>in the mod constructor</b>, because that is
 * the last moment a {@code DeferredRegister} can still be handed an entry. At
 * that moment:
 * <ul>
 *   <li>the block and item registries are <b>not populated</b> - every mod's
 *       {@code RegisterEvent} is still to come, and which mod's runs first is
 *       not ours to decide;</li>
 *   <li>tags do not exist at all. They are datapack content and are not loaded
 *       until a world does.</li>
 * </ul>
 * What <i>is</i> available is every mod's jar, through {@link ModList}. So this
 * reads the files a mod ships rather than the objects it will later register:
 * both are declarations the mod author wrote down on purpose, which makes them a
 * better source of truth than a heuristic over the registry would have been
 * anyway.
 *
 * <p><b>This works on a dedicated server</b>, which is the part worth checking
 * rather than assuming. Mod jars are universal - they carry their {@code assets/}
 * whether or not the process will ever draw anything - so the ingot <i>texture</i>
 * a colour is averaged from is readable on a server with no resource manager and
 * no client at all.
 */
public final class ModdedMaterials {

    /**
     * A wood another mod added, and the plank texture its double gate should
     * wear.
     *
     * @param gateId       the mod's own fence gate, which the recipe consumes two of
     * @param plankTexture the texture id the generated models point at
     * @param strippedLog  whether some jar ships {@code stripped_<name>_log} as an
     *                     item. Only the wagon spends one, so a wood without it is
     *                     a wood with five carts instead of six - see
     *                     {@link MaterialScan#itemShipped}
     */
    public record Wood(String namespace, String name, String gateId, String plankTexture,
                       boolean strippedLog) {
        /** The id our double gate takes in <i>our</i> namespace. */
        public String doubleGateId() {
            return namespace + "_" + name + "_double_fence_gate";
        }
    }

    /**
     * A metal or crystal another mod added.
     *
     * @param itemId the ingot or gem itself
     * @param colour averaged from its own item texture - {@link MaterialScan#averageColour}
     * @param gem    true for {@code c:gems/*}, false for {@code c:ingots/*}
     */
    public record Metal(String itemId, int colour, boolean gem) {

        /** What a mod tacks onto a material name, at either end. */
        private static final String[] AFFIXES = {"ingot", "gem", "crystal", "shard"};

        /**
         * The bare material name: {@code tin} from {@code mymod:tin_ingot}, and
         * also from {@code mymod:ingot_tin}.
         *
         * <p><b>Both orders are in the wild.</b> The suffix was the only one
         * stripped until a 319-jar pack turned up {@code bloodmagic:ingot_hellforged},
         * which came through untouched and would have been sold to players as
         * "Ingot Hellforged Horse Armor". The namespace is deliberately not part
         * of this - see {@link #armourId()}.
         */
        public String material() {
            String path = itemId.substring(itemId.indexOf(':') + 1);
            for (String affix : AFFIXES) {
                String stripped = null;
                if (path.endsWith("_" + affix)) {
                    stripped = path.substring(0, path.length() - affix.length() - 1);
                } else if (path.startsWith(affix + "_")) {
                    stripped = path.substring(affix.length() + 1);
                }
                // An item called exactly "ingot_" leaves nothing behind, and a
                // material with no name is an id of "_horse_armor".
                if (stripped != null && !stripped.isEmpty()) {
                    return stripped;
                }
            }
            return path;
        }

        /**
         * <b>Not unique, on purpose</b> - see {@link ModdedMaterials#armourMetals()}.
         * Six mods adding a steel all answer {@code steel_horse_armor} here, and
         * exactly one item by that name is registered.
         */
        public String armourId() {
            return material() + "_horse_armor";
        }
    }

    private static MaterialScan.Result result =
            new MaterialScan.Result(List.of(), List.of(), Map.of(), "");
    private static boolean scanned;

    private ModdedMaterials() {
    }

    public static List<Wood> woods() {
        return result.woods();
    }

    public static List<Metal> metals() {
        return result.metals();
    }

    /**
     * <b>The metals that get a horse armour of their own</b> - one per
     * {@link Metal#armourId()}, where {@link #metals()} is one per item.
     *
     * <h2>Why these are not the same list</h2>
     * A horse armour is named after the material and not after the mod, because
     * "Steel Horse Armor" is what a player is looking for and
     * "Alltheores Steel Horse Armor" is not. That was written when a material
     * meant an item, and it does not: a 319-jar pack had <b>six</b> mods filing a
     * steel ingot under {@code c:ingots/steel}, and one of them was enough to
     * take down mod loading with a duplicate registration. ExtendedAE manages it
     * inside a single jar, shipping {@code entro_crystal} and {@code entro_ingot}
     * both - so namespacing the id would not have been a fix either.
     *
     * <p>So the collapse happens here, and it is the honest answer rather than
     * only the safe one: those six steels share a {@code c:ingots/steel} repair
     * tag, which means six separately-registered armours would have been six
     * identical items. Every one of the six ingots still <i>forges</i> the
     * armour - {@link #metalsFor} is how, and {@code GeneratedArmour} writes one
     * recipe per ingot - so no mod's metal becomes a dead end.
     *
     * <p>The winner is the lowest item id, so it does not depend on which mod
     * loaded first. {@link #metals()} keeps all of them, because the equestrian
     * bench colours a fitting from whichever ingot the player actually put in it
     * and must still know all 167.
     */
    public static List<Metal> armourMetals() {
        return MaterialScan.armourMetals(metals());
    }

    /** Every metal that forges this armour, in {@link #metals()} order. */
    public static List<Metal> metalsFor(String armourId) {
        return MaterialScan.metalsFor(metals(), armourId);
    }

    /**
     * Modded dyes that carry no vanilla {@code minecraft:dye} component, and the
     * colour each one's art averages to.
     *
     * <p>Most modded dyes are <b>not</b> in here and do not need to be: a dye
     * that is one of vanilla's sixteen carries the vanilla component, and the
     * bench has always accepted anything that does. This is only the ones with a
     * colour vanilla has no name for.
     */
    public static Map<String, Integer> dyes() {
        return result.dyes();
    }

    /**
     * A hash of exactly what the generated pack is built from. What makes
     * "generate once, but check on every launch" a real check rather than a
     * file-exists test. See {@link GeneratedPack}.
     */
    public static String fingerprint() {
        return result.fingerprint();
    }

    /** That item's fitting colour, or null if it is not a metal we found. */
    public static @Nullable Integer metalColour(String itemId) {
        for (Metal metal : metals()) {
            if (metal.itemId().equals(itemId)) {
                return metal.colour();
            }
        }
        return null;
    }

    /** Read every loaded mod's jar. Call once, from the mod constructor. */
    public static void scan() {
        if (scanned) {
            return;
        }
        scanned = true;

        List<MaterialScan.Source> sources = new ArrayList<>();
        for (var info : ModList.get().getModFiles()) {
            try {
                sources.add(wrap(info.getFile().getContents()));
            } catch (RuntimeException unreadable) {
                // A mod file we cannot open is a mod file we cannot read.
            }
        }
        result = MaterialScan.of(sources, HorseGenetics.MOD_ID);

        HorseGenetics.LOGGER.info("compat: {} modded woods, {} modded metals, {} modded dyes, fingerprint {}",
                woods().size(), metals().size(), dyes().size(), fingerprint());
    }

    /**
     * NeoForge's jar, as the three questions a scan asks.
     *
     * <p>{@code visitContent} hands the visitor the <b>full</b> jar-relative
     * path rather than one relative to the folder asked for, which is what the
     * scan's path patterns are written against.
     */
    private static MaterialScan.Source wrap(JarContents contents) {
        return new MaterialScan.Source() {
            @Override
            public byte @Nullable [] read(String path) {
                try {
                    return contents.readFile(path);
                } catch (Exception unreadable) {
                    return null;
                }
            }

            @Override
            public boolean has(String path) {
                return contents.containsFile(path);
            }

            @Override
            public void list(String folder, Consumer<String> paths) {
                contents.visitContent(folder, (path, resource) -> paths.accept(path));
            }
        };
    }

    /**
     * <b>Whether an item a mod DECLARED actually exists.</b>
     *
     * <p>Everything the scan finds is read out of files - a blockstate for a
     * gate, a tag entry for an ingot - and a file is a declaration rather than a
     * promise. A mod can ship a tag naming an item it only registers behind a
     * config flag, or name another mod's item in a common tag whether or not
     * that mod is installed. Both are legitimate and both leave us holding an id
     * nothing answers to.
     *
     * <p>It cost three walls of red text to find that out, from a recipe whose
     * ingredient did not exist: the recipe simply fails to parse, loudly, and
     * the player sees a page of somebody else's mod id. So anything that names a
     * third-party item in a way the game will later resolve - which is recipes,
     * and only recipes - asks this first.
     *
     * <p><b>Only callable after registration</b>, which is why the generated pack
     * is built at pack-finder time rather than in the mod constructor.
     */
    public static boolean itemExists(String id) {
        try {
            net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .getValue(net.minecraft.resources.Identifier.parse(id));
            return item != null && item != net.minecraft.world.item.Items.AIR;
        } catch (RuntimeException malformedId) {
            return false;
        }
    }
}
