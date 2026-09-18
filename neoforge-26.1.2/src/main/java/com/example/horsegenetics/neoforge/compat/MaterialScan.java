package com.example.horsegenetics.neoforge.compat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.util.function.Consumer;

/**
 * <b>The whole of "what did the other mods bring", with no Minecraft in it.</b>
 *
 * <p>Everything here is file reading, JSON parsing and pixel averaging over a
 * {@link Source}, which is a jar reduced to the three questions this needs to
 * ask of one. {@link ModdedMaterials} is the adapter that hands it NeoForge's
 * jars and keeps the answers; this is the part that decides.
 *
 * <h2>Why it is split out at all</h2>
 * So it can be tested. The scan is the piece of this feature most likely to be
 * quietly wrong - a path pattern that matches one mod's layout and not another's
 * fails by finding nothing, which looks exactly like there being nothing to
 * find. Behind {@code ModList} that can only be tested by installing a mod and
 * booting the game; behind {@link Source} a test can hand it a wood and an ingot
 * in four lines and assert what came back. See {@code MaterialScanTest}.
 *
 * <p>The split is real rather than cosmetic: <b>this file imports nothing from
 * Minecraft or NeoForge</b>, which is what lets the test run without either.
 * Keep it that way - the moment something here needs a registry, it belongs in
 * the adapter instead.
 */
public final class MaterialScan {

    /**
     * A jar, reduced to what a scan asks of one.
     *
     * <p>Modelled on NeoForge's {@code JarContents} because that is what it
     * wraps, but with ten methods dropped: a scan reads a file, checks a file
     * exists, and walks a folder, and nothing it does needs more than that.
     */
    public interface Source {
        /** File contents, or null when the path is absent or is a directory. */
        byte @Nullable [] read(String path);

        boolean has(String path);

        /** Every file path under {@code folder}, recursively. Absent folder: no calls. */
        void list(String folder, Consumer<String> paths);
    }

    /** What a scan found. Every list is sorted; see {@link #of}. */
    public record Result(List<ModdedMaterials.Wood> woods,
                         List<ModdedMaterials.Metal> metals,
                         Map<String, Integer> dyes,
                         String fingerprint) {
    }

    /** Namespaces that are never a source of modded material. */
    private static final String MINECRAFT = "minecraft";

    private MaterialScan() {
    }

    /**
     * Read every source and decide.
     *
     * <p>Fails soft throughout. A malformed tag file, an unreadable jar or a PNG
     * we cannot decode costs <i>that</i> material and nothing else - it must
     * never stop the game loading, because whatever went wrong is in somebody
     * else's file and the player cannot fix it.
     *
     * @param ours our own mod id, which is skipped: the generated gates land in
     *             that namespace, and a double gate of a double gate is not a
     *             thing anybody wants
     */
    public static Result of(List<Source> sources, String ours) {
        Map<String, ModdedMaterials.Wood> woods = new TreeMap<>();
        Map<String, Boolean> metalIds = new TreeMap<>();
        Map<String, Boolean> dyeIds = new TreeMap<>();

        for (Source source : sources) {
            try {
                collectWoods(source, woods, ours);
                collectTagged(source, "data/c/tags/item/ingots", metalIds, false);
                collectTagged(source, "data/c/tags/item/gems", metalIds, true);
                collectTagged(source, "data/c/tags/item/dyes", dyeIds, false);
            } catch (RuntimeException broken) {
                // One bad jar is one bad jar.
            }
        }

        // Colours in a second pass, once every tag file has been read: a metal
        // declared in one jar may perfectly well be drawn in another.
        List<ModdedMaterials.Metal> metals = new ArrayList<>();
        for (Map.Entry<String, Boolean> metal : metalIds.entrySet()) {
            metals.add(new ModdedMaterials.Metal(
                    metal.getKey(), colourOf(sources, metal.getKey()), metal.getValue()));
        }
        metals.sort(Comparator.comparing(ModdedMaterials.Metal::itemId));

        Map<String, Integer> dyes = new TreeMap<>();
        for (String id : dyeIds.keySet()) {
            dyes.put(id, colourOf(sources, id));
        }

        List<ModdedMaterials.Wood> sortedWoods = List.copyOf(woods.values());
        List<ModdedMaterials.Metal> sortedMetals = List.copyOf(metals);
        Map<String, Integer> sortedDyes = Map.copyOf(dyes);
        return new Result(sortedWoods, sortedMetals, sortedDyes,
                fingerprint(sortedWoods, metals, dyes));
    }

    // ------------------------------------------------------------------
    // Woods
    // ------------------------------------------------------------------

    /**
     * A wood is a mod that ships a {@code <name>_fence_gate} blockstate - the
     * narrowest declaration that means what we need it to mean: something the
     * player can already place a gate of, which is what our recipe takes two of.
     */
    private static void collectWoods(Source source, Map<String, ModdedMaterials.Wood> out, String ours) {
        source.list("assets", path -> {
            if (!path.endsWith("_fence_gate.json")) {
                return;
            }
            // assets/<ns>/blockstates/<name>_fence_gate.json, and nothing else
            // that happens to end the same way.
            String[] parts = path.split("/");
            if (parts.length != 4 || !"assets".equals(parts[0]) || !"blockstates".equals(parts[2])) {
                return;
            }
            String namespace = parts[1];
            if (MINECRAFT.equals(namespace) || namespace.equals(ours)) {
                return;
            }
            String name = parts[3].substring(0, parts[3].length() - "_fence_gate.json".length());
            if (name.isEmpty()) {
                return;
            }
            String texture = plankTexture(source, namespace, name);
            if (texture != null) {
                out.put(namespace + ":" + name, new ModdedMaterials.Wood(
                        namespace, name, namespace + ":" + name + "_fence_gate", texture));
            }
        });
    }

    /**
     * The texture the generated models wear.
     *
     * <p><b>Planks first, the gate's own sheet second</b>, which is the same call
     * the shipped twelve make. Vanilla's bamboo gate has a bespoke sheet drawn
     * for <i>its</i> rail positions - two short rails - while ours runs one rail
     * the full width, so sampling it scrambled. Planks are drawn for no rail
     * positions at all and therefore fit any.
     */
    static @Nullable String plankTexture(Source source, String namespace, String name) {
        if (source.has("assets/" + namespace + "/textures/block/" + name + "_planks.png")) {
            return namespace + ":block/" + name + "_planks";
        }
        JsonObject model = readJson(source,
                "assets/" + namespace + "/models/block/" + name + "_fence_gate.json");
        if (model != null && model.has("textures") && model.get("textures").isJsonObject()) {
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
    // Metals and dyes
    // ------------------------------------------------------------------

    /**
     * Items another mod filed under a {@code c:} common tag - where a mod says
     * "this is an ingot" in the one place every other mod reads.
     *
     * <p>Only the per-material child tags are read, never the {@code c:ingots}
     * parent: the parent is a list of {@code #c:ingots/*} references, so reading
     * it would find tag names rather than items, and reading both would find
     * every item twice.
     */
    private static void collectTagged(Source source, String folder, Map<String, Boolean> out, boolean gem) {
        source.list(folder, path -> {
            if (!path.endsWith(".json")) {
                return;
            }
            JsonObject json = readJson(source, path);
            if (json == null || !json.has("values") || !json.get("values").isJsonArray()) {
                return;
            }
            for (JsonElement value : json.getAsJsonArray("values")) {
                String id = itemIdOf(value);
                // A '#' entry is another tag, not an item. A minecraft: one is
                // something we already handle by name - see SaddleTint's fifteen.
                if (id == null || id.startsWith("#") || id.startsWith(MINECRAFT + ":")
                        || id.indexOf(':') < 0) {
                    continue;
                }
                out.putIfAbsent(id, gem);
            }
        });
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

    /** What a material with no readable art is given. A plain mid grey. */
    public static final int FALLBACK_COLOUR = 0x9C9C9C;

    /**
     * <b>A material's colour, averaged off its own item texture.</b>
     *
     * <p>There is no other source for it. Nothing in a mod's files says "tin is
     * silver-grey"; the only place that fact exists is the art. So the fittings
     * colour and the generated armour's tint are both read out of the texels
     * somebody drew, which is the same thing a player's eye does when it decides
     * two ingots look different.
     */
    static int colourOf(List<Source> sources, String itemId) {
        int colon = itemId.indexOf(':');
        String namespace = itemId.substring(0, colon);
        String path = itemId.substring(colon + 1);

        for (Source source : sources) {
            String texture = itemTexturePath(source, namespace, path);
            if (texture == null) {
                continue;
            }
            try {
                byte[] png = source.read(texture);
                if (png == null) {
                    continue;
                }
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
                if (image != null) {
                    return averageColour(image);
                }
            } catch (Exception unreadable) {
                // Somebody else's PNG. Try the next source, then give up quietly.
            }
        }
        return FALLBACK_COLOUR;
    }

    /** Where the item's art is: the model decides, the naming convention is a fallback. */
    static @Nullable String itemTexturePath(Source source, String namespace, String path) {
        JsonObject model = readJson(source, "assets/" + namespace + "/models/item/" + path + ".json");
        if (model != null && model.has("textures") && model.get("textures").isJsonObject()) {
            JsonObject textures = model.getAsJsonObject("textures");
            if (textures.has("layer0") && textures.get("layer0").isJsonPrimitive()) {
                String layer = textures.get("layer0").getAsString();
                int colon = layer.indexOf(':');
                String ns = colon < 0 ? MINECRAFT : layer.substring(0, colon);
                String file = colon < 0 ? layer : layer.substring(colon + 1);
                String candidate = "assets/" + ns + "/textures/" + file + ".png";
                if (source.has(candidate)) {
                    return candidate;
                }
            }
        }
        String guess = "assets/" + namespace + "/textures/item/" + path + ".png";
        return source.has(guess) ? guess : null;
    }

    /**
     * Mean of the opaque texels, <b>in linear light</b>.
     *
     * <p>Averaging gamma-encoded bytes darkens and desaturates - the classic
     * wrong-looking blend - and an ingot is mostly highlight and shadow of one
     * hue, which is exactly the case it gets most wrong.
     */
    public static int averageColour(BufferedImage image) {
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
            return FALLBACK_COLOUR;
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

    static @Nullable JsonObject readJson(Source source, String path) {
        try {
            byte[] bytes = source.read(path);
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
     * Over everything the generated pack is built from, in sorted order.
     *
     * <p>The colours are in it as well as the ids, so a mod that only repaints
     * its ingot still moves the fingerprint and still gets a redrawn armour.
     * Sorted, because <a href="../../../../../../../../wiki/philosophy.html">philosophy
     * §7</a> says load order can never matter and a hash over a list FML handed
     * us in its own order would break that quietly.
     */
    static String fingerprint(List<ModdedMaterials.Wood> woods,
                              List<ModdedMaterials.Metal> metals,
                              Map<String, Integer> dyes) {
        StringBuilder sb = new StringBuilder();
        for (ModdedMaterials.Wood wood : woods) {
            sb.append("w|").append(wood.namespace()).append(':').append(wood.name())
                    .append('|').append(wood.plankTexture()).append('\n');
        }
        for (ModdedMaterials.Metal metal : metals) {
            sb.append("m|").append(metal.itemId()).append('|')
                    .append(Integer.toHexString(metal.colour())).append('|').append(metal.gem()).append('\n');
        }
        for (Map.Entry<String, Integer> dye : dyes.entrySet()) {
            sb.append("d|").append(dye.getKey()).append('|')
                    .append(Integer.toHexString(dye.getValue())).append('\n');
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
            // SHA-256 is mandated by the platform; if it is somehow gone, a hash
            // that still changes when the content does beats throwing.
            return Integer.toHexString(sb.toString().hashCode());
        }
    }
}
