package com.example.horsegenetics.neoforge.compat;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neoforged.fml.ModList;
import net.neoforged.fml.jarcontents.JarContents;
import org.jspecify.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * <b>What the other mods in this pack have brought with them</b> - the woods we
 * can make a double gate for, and the metals we can make horse armour and tack
 * fittings out of.
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
 * a wood is a mod that ships a {@code <name>_fence_gate} blockstate, and a metal
 * is an item a mod puts in the {@code c:ingots/*} or {@code c:gems/*} tag files.
 * Both are declarations the mod author wrote down on purpose, which makes them a
 * better source of truth than a heuristic over the registry would be anyway.
 *
 * <p><b>This works on a dedicated server</b>, which is the part worth checking
 * rather than assuming. Mod jars are universal - they carry their {@code assets/}
 * whether or not the process will ever draw anything - so the ingot <i>texture</i>
 * a colour is averaged from is readable on a server with no resource manager and
 * no client at all.
 *
 * <h2>Load order can never matter</h2>
 * <a href="../../../../../../../../wiki/philosophy.html">Philosophy §7</a> is
 * explicit that nothing may depend on which mod loaded first, and a scan over a
 * list FML hands us in its own order would break that quietly - two players with
 * the same mods would get the same content in a different order, and anything
 * derived from the order would disagree. So every result here is <b>sorted by
 * id</b> before it leaves this class, and the {@link #fingerprint()} is taken
 * over the sorted form.
 *
 * <h2>The fingerprint</h2>
 * A hash of exactly what the generated pack is built from - the woods, the
 * metals and their colours. It is what makes "generate once, but check on every
 * launch" a real check rather than a file-exists test: add a mod, the
 * fingerprint moves and the pack is rebuilt; change nothing, and the second
 * launch does no work. See {@link GeneratedPack}.
 */
public final class ModdedMaterials {

    /**
     * A wood another mod added, and the plank texture its double gate should
     * wear.
     *
     * @param gateId       the mod's own fence gate, which the recipe consumes two of
     * @param plankTexture the texture id the generated models point at
     */
    public record Wood(String namespace, String name, String gateId, String plankTexture) {
        /** The id our double gate takes in <i>our</i> namespace. */
        public String doubleGateId() {
            return namespace + "_" + name + "_double_fence_gate";
        }
    }

    /**
     * A metal or crystal another mod added.
     *
     * @param itemId the ingot or gem itself
     * @param colour averaged from its own item texture - see {@link #averageColour}
     * @param gem    true for {@code c:gems/*}, false for {@code c:ingots/*}
     */
    public record Metal(String itemId, int colour, boolean gem) {
        /** The bare material name, e.g. {@code tin} from {@code mymod:tin_ingot}. */
        public String material() {
            String path = itemId.substring(itemId.indexOf(':') + 1);
            for (String suffix : new String[] {"_ingot", "_gem", "_crystal", "_shard"}) {
                if (path.endsWith(suffix)) {
                    return path.substring(0, path.length() - suffix.length());
                }
            }
            return path;
        }

        public String armourId() {
            return material() + "_horse_armor";
        }
    }

    private static final String OURS = HorseGenetics.MOD_ID;

    private static List<Wood> woods = List.of();
    private static List<Metal> metals = List.of();
    private static Map<String, Integer> dyes = Map.of();
    private static String fingerprint = "";
    private static boolean scanned;

    private ModdedMaterials() {
    }

    public static List<Wood> woods() {
        return woods;
    }

    public static List<Metal> metals() {
        return metals;
    }

    public static String fingerprint() {
        return fingerprint;
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
        return dyes;
    }

    /** That item's fitting colour, or null if it is not a metal we found. */
    public static @Nullable Integer metalColour(String itemId) {
        for (Metal metal : metals) {
            if (metal.itemId().equals(itemId)) {
                return metal.colour();
            }
        }
        return null;
    }

    /**
     * Read every loaded mod's jar. Call once, from the mod constructor, before
     * anything registers.
     *
     * <p>Fails soft by design. A mod with an unreadable jar, a malformed tag
     * file or a texture we cannot decode costs us <i>that</i> wood or metal and
     * nothing else - it must never stop the game loading, because the thing that
     * went wrong is in somebody else's file and the player cannot fix it.
     */
    public static void scan() {
        if (scanned) {
            return;
        }
        scanned = true;

        Map<String, Wood> foundWoods = new TreeMap<>();
        Map<String, Metal> foundMetals = new TreeMap<>();
        // Textures are looked up across every jar rather than only the one that
        // declared the tag: a mod may perfectly well put another mod's item in a
        // common tag, and the art lives with whoever registered the item.
        List<JarContents> jars = new ArrayList<>();

        for (var info : ModList.get().getModFiles()) {
            JarContents contents;
            try {
                contents = info.getFile().getContents();
            } catch (RuntimeException unreadable) {
                continue;
            }
            jars.add(contents);
        }

        Map<String, Metal> foundDyes = new TreeMap<>();
        for (JarContents jar : jars) {
            try {
                collectWoods(jar, foundWoods);
                collectMetalTags(jar, foundMetals);
                collectDyeTags(jar, foundDyes);
            } catch (RuntimeException broken) {
                HorseGenetics.LOGGER.warn("compat: skipped a mod jar we could not read", broken);
            }
        }

        // Colours in a second pass, once every tag file has been read, so a
        // metal declared in one jar and drawn in another still gets its colour.
        List<Metal> coloured = new ArrayList<>();
        for (Metal metal : foundMetals.values()) {
            coloured.add(new Metal(metal.itemId(), colourOf(jars, metal.itemId()), metal.gem()));
        }

        Map<String, Integer> colouredDyes = new TreeMap<>();
        for (String id : foundDyes.keySet()) {
            colouredDyes.put(id, colourOf(jars, id));
        }

        woods = List.copyOf(foundWoods.values());
        coloured.sort(Comparator.comparing(Metal::itemId));
        metals = List.copyOf(coloured);
        dyes = Map.copyOf(colouredDyes);
        fingerprint = hash();

        HorseGenetics.LOGGER.info("compat: {} modded woods, {} modded metals, {} modded dyes, fingerprint {}",
                woods.size(), metals.size(), dyes.size(), fingerprint);
    }

    // ------------------------------------------------------------------
    // Woods
    // ------------------------------------------------------------------

    /**
     * A wood is a mod that ships a {@code <name>_fence_gate} blockstate. That is
     * the narrowest declaration that means what we need it to mean: something
     * the player can already place a gate of, which is what our recipe takes two
     * of.
     */
    private static void collectWoods(JarContents jar, Map<String, Wood> out) {
        jar.visitContent("assets", (path, resource) -> {
            // assets/<ns>/blockstates/<name>_fence_gate.json
            if (!path.endsWith("_fence_gate.json") || !path.contains("/blockstates/")) {
                return;
            }
            String[] parts = path.split("/");
            if (parts.length != 4 || !"assets".equals(parts[0]) || !"blockstates".equals(parts[2])) {
                return;
            }
            String namespace = parts[1];
            // Vanilla's twelve are already hand-listed in DoubleGates, and our
            // own namespace is where the generated ones will land - taking
            // either would make a double gate of a double gate.
            if ("minecraft".equals(namespace) || OURS.equals(namespace)) {
                return;
            }
            String name = parts[3].substring(0, parts[3].length() - "_fence_gate.json".length());
            if (name.isEmpty()) {
                return;
            }
            String texture = plankTexture(jar, namespace, name);
            if (texture == null) {
                return;
            }
            out.put(namespace + ":" + name,
                    new Wood(namespace, name, namespace + ":" + name + "_fence_gate", texture));
        });
    }

    /**
     * The texture the generated models wear.
     *
     * <p><b>Planks first, the gate's own sheet second</b>, and that order is the
     * same call the shipped twelve make. Vanilla's bamboo gate has a bespoke
     * sheet drawn for <i>its</i> rail positions - two short rails - while ours
     * runs one rail the full width, so sampling it scrambled. Planks are drawn
     * for no rail positions at all and therefore fit any. A mod whose gate
     * sheet is equally bespoke would scramble the same way, so planks win
     * wherever there are planks to win with.
     */
    private static @Nullable String plankTexture(JarContents jar, String namespace, String name) {
        if (jar.containsFile("assets/" + namespace + "/textures/block/" + name + "_planks.png")) {
            return namespace + ":block/" + name + "_planks";
        }
        JsonObject model = readJson(jar, "assets/" + namespace + "/models/block/" + name + "_fence_gate.json");
        if (model != null && model.has("textures")) {
            JsonObject textures = model.getAsJsonObject("textures");
            for (String key : new String[] {"texture", "wood", "all", "particle"}) {
                if (textures.has(key) && textures.get(key).isJsonPrimitive()) {
                    String value = textures.get(key).getAsString();
                    if (!value.startsWith("#")) {
                        return value;
                    }
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Metals
    // ------------------------------------------------------------------

    /**
     * A metal is an item a mod puts in {@code c:ingots/<material>} or
     * {@code c:gems/<material>} - the NeoForge common tags, which is where a mod
     * says "this is an ingot" in the one place every other mod reads.
     *
     * <p>Only the per-material child tags are read, not the {@code c:ingots}
     * parent: the parent is a list of {@code #c:ingots/*} references, so reading
     * it would find tag names rather than items and reading both would find
     * everything twice.
     */
    private static void collectMetalTags(JarContents jar, Map<String, Metal> out) {
        for (String kind : new String[] {"ingots", "gems"}) {
            boolean gem = "gems".equals(kind);
            jar.visitContent("data/c/tags/item/" + kind, (path, resource) -> {
                if (!path.endsWith(".json")) {
                    return;
                }
                JsonObject json = readJson(jar, path);
                if (json == null || !json.has("values")) {
                    return;
                }
                for (JsonElement value : json.getAsJsonArray("values")) {
                    String id = itemIdOf(value);
                    // A '#' entry is another tag, not an item, and an entry in
                    // the minecraft namespace is something we already handle by
                    // name - see SaddleTint's fifteen.
                    if (id == null || id.startsWith("#") || id.startsWith("minecraft:")) {
                        continue;
                    }
                    out.putIfAbsent(id, new Metal(id, 0, gem));
                }
            });
        }
    }

    /**
     * Dyes another mod added, from the {@code c:dyes} common tags.
     *
     * <p>Kept separate from the metals because they answer a different question
     * and are used in a different slot, but read the same way and coloured by
     * the same averaging - a dye's own item art is the only place its colour is
     * written down either.
     *
     * <p>Whether one of these is actually <i>needed</i> is decided at runtime,
     * not here: an item with a vanilla {@code minecraft:dye} component already
     * worked and is passed over. See {@code EquestrianBenchMenu.dyeColour}.
     */
    private static void collectDyeTags(JarContents jar, Map<String, Metal> out) {
        for (String folder : new String[] {"data/c/tags/item/dyes", "data/c/tags/item/dye"}) {
            jar.visitContent(folder, (path, resource) -> {
                if (!path.endsWith(".json")) {
                    return;
                }
                JsonObject json = readJson(jar, path);
                if (json == null || !json.has("values")) {
                    return;
                }
                for (JsonElement value : json.getAsJsonArray("values")) {
                    String id = itemIdOf(value);
                    if (id == null || id.startsWith("#") || id.startsWith("minecraft:")) {
                        continue;
                    }
                    out.putIfAbsent(id, new Metal(id, 0, false));
                }
            });
        }
    }

    /** A tag entry is either a plain string or {@code {"id": ..., "required": false}}. */
    private static @Nullable String itemIdOf(JsonElement value) {
        if (value.isJsonPrimitive()) {
            return value.getAsString();
        }
        if (value.isJsonObject() && value.getAsJsonObject().has("id")) {
            return value.getAsJsonObject().get("id").getAsString();
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Colour
    // ------------------------------------------------------------------

    /**
     * <b>An ingot's colour, averaged off its own item texture.</b>
     *
     * <p>There is no other source for it. Nothing in a mod's files says "tin is
     * silver-grey"; the only place that fact exists is the art. So the fittings
     * colour and the generated armour's tint are both read out of the sixteen-
     * odd opaque texels somebody drew, which is the same thing a player's eye
     * does when they decide two ingots look different.
     *
     * <p>Averaged in <b>linear</b> light rather than in sRGB. Averaging gamma-
     * encoded bytes darkens and desaturates - the classic wrong-looking blend -
     * and an ingot is mostly highlight and shadow of one hue, which is exactly
     * the case it gets most wrong.
     *
     * <p>Returns a mid grey when there is no art to read, which keeps a metal we
     * cannot see usable rather than dropping it.
     */
    private static int colourOf(List<JarContents> jars, String itemId) {
        int colon = itemId.indexOf(':');
        String namespace = itemId.substring(0, colon);
        String path = itemId.substring(colon + 1);

        for (JarContents jar : jars) {
            String texture = itemTexturePath(jar, namespace, path);
            if (texture == null) {
                continue;
            }
            try {
                byte[] png = jar.readFile(texture);
                if (png == null) {
                    continue;
                }
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
                if (image != null) {
                    return averageColour(image);
                }
            } catch (Exception unreadable) {
                // Somebody else's PNG. Try the next jar, then give up quietly.
            }
        }
        return 0x9C9C9C;
    }

    /**
     * Where the item's art is. The model is asked first because that is what
     * actually decides, and only mods that follow the usual naming are caught by
     * the fallback.
     */
    private static @Nullable String itemTexturePath(JarContents jar, String namespace, String path) {
        JsonObject model = readJson(jar, "assets/" + namespace + "/models/item/" + path + ".json");
        if (model != null && model.has("textures")) {
            JsonObject textures = model.getAsJsonObject("textures");
            if (textures.has("layer0")) {
                String layer = textures.get("layer0").getAsString();
                int colon = layer.indexOf(':');
                String ns = colon < 0 ? "minecraft" : layer.substring(0, colon);
                String file = colon < 0 ? layer : layer.substring(colon + 1);
                String candidate = "assets/" + ns + "/textures/" + file + ".png";
                if (jar.containsFile(candidate)) {
                    return candidate;
                }
            }
        }
        String guess = "assets/" + namespace + "/textures/item/" + path + ".png";
        return jar.containsFile(guess) ? guess : null;
    }

    /** Mean of the opaque texels, in linear light. Package-visible so a test can reach it. */
    static int averageColour(BufferedImage image) {
        double r = 0;
        double g = 0;
        double b = 0;
        long n = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                // Half-transparent texels are edge antialiasing, not material.
                if (((argb >>> 24) & 0xFF) < 128) {
                    continue;
                }
                r += toLinear((argb >> 16) & 0xFF);
                g += toLinear((argb >> 8) & 0xFF);
                b += toLinear(argb & 0xFF);
                n++;
            }
        }
        if (n == 0) {
            return 0x9C9C9C;
        }
        return (toSrgb(r / n) << 16) | (toSrgb(g / n) << 8) | toSrgb(b / n);
    }

    private static double toLinear(int channel) {
        double v = channel / 255.0;
        return v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }

    private static int toSrgb(double linear) {
        double v = linear <= 0.0031308 ? linear * 12.92 : 1.055 * Math.pow(linear, 1 / 2.4) - 0.055;
        return Math.max(0, Math.min(255, (int) Math.round(v * 255.0)));
    }

    // ------------------------------------------------------------------
    // Plumbing
    // ------------------------------------------------------------------

    private static @Nullable JsonObject readJson(JarContents jar, String path) {
        try {
            byte[] bytes = jar.readFile(path);
            if (bytes == null) {
                return null;
            }
            JsonElement parsed = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (Exception malformed) {
            return null;
        }
    }

    /**
     * Over the sorted results, and over everything the pack is built from -
     * including each metal's colour, so a mod that only repaints its ingot still
     * moves the fingerprint and still gets a redrawn armour.
     */
    private static String hash() {
        StringBuilder sb = new StringBuilder();
        for (Wood wood : woods) {
            sb.append("w|").append(wood.namespace()).append(':').append(wood.name())
                    .append('|').append(wood.plankTexture()).append('\n');
        }
        for (Metal metal : metals) {
            sb.append("m|").append(metal.itemId()).append('|')
                    .append(Integer.toHexString(metal.colour())).append('|').append(metal.gem()).append('\n');
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                hex.append(String.format("%02x", digest[i]));
            }
            return hex.toString();
        } catch (Exception impossible) {
            // SHA-256 is mandated by the platform; if it is gone, a length is
            // still a fingerprint that changes when the content does.
            return Integer.toHexString(sb.toString().hashCode());
        }
    }

    /**
     * <b>Whether an item a mod DECLARED actually exists.</b>
     *
     * <p>Everything here is read out of files - a blockstate for a gate, a tag
     * entry for an ingot - and a file is a declaration rather than a promise.
     * A mod can ship a tag naming an item it only registers behind a config
     * flag, or name another mod's item in a common tag whether or not that mod
     * is installed. Both are legitimate and both leave us holding an id nothing
     * answers to.
     *
     * <p>It cost three walls of red text to find that out, from a recipe whose
     * ingredient did not exist: the recipe simply fails to parse, loudly, and
     * the player sees a page of somebody else's mod id. So anything that names
     * a third-party item in a way the game will later resolve - which is
     * recipes, and only recipes - asks this first.
     *
     * <p><b>Only callable after registration</b>, which is why the generated
     * pack is built at pack-finder time rather than in the mod constructor.
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

    /** Reset between tests. Not called in the game. */
    static void resetForTest() {
        scanned = false;
        woods = List.of();
        metals = List.of();
        dyes = Map.of();
        fingerprint = "";
    }
}
