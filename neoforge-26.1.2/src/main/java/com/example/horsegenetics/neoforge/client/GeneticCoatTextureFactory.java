package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.ClientConfig;
import com.example.horsegenetics.common.coat.CoatData;
import com.example.horsegenetics.common.coat.CoatTextureId;
import com.example.horsegenetics.common.coat.TexelBudgetCache;
import com.example.horsegenetics.common.coat.pattern.CoatTextureComposer;
import com.example.horsegenetics.common.coat.pattern.GradientLut;
import com.example.horsegenetics.common.coat.pattern.LutSet;
import com.example.horsegenetics.common.genetics.GeneCodeDisplay;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.LutContribution;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs the {@code CoatTextureComposer} pipeline for a horse and uploads the
 * result as a {@link DynamicTexture}, cached by {@link CoatData#textureKey()}
 * plus adult/foal (grey greys only adults, and the foal uses a different mesh /
 * template). The two white templates + the red/black gradient load once.
 *
 * <p>Both caches are {@linkplain TexelBudgetCache texel-budgeted LRUs}: an
 * entry is a whole {@code N x N} sheet held both as CPU pixels and as a
 * registered GPU texture, so the client used to accumulate one of each per
 * distinct genome it had ever drawn and free them only on logging out. The
 * budget is in texels rather than entries so it still means the same amount of
 * memory if {@link HorseSkinGeometry#SHEET_SIZE} ever changes.
 *
 * <p>Everything here is loaded lazily and dropped by {@link #clear()}, which
 * runs on logging out <i>and</i> on a client resource reload
 * ({@link CoatAssetReload}) - the templates and the LUTs are pack resources,
 * so a reload has to be able to take them back.
 */
public final class GeneticCoatTextureFactory {

    private static final int N = HorseSkinGeometry.SHEET_SIZE;

    private static final Identifier ADULT_TEMPLATE =
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "textures/entity/horse/horse_white.png");
    private static final Identifier BABY_TEMPLATE =
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "textures/entity/horse/horse_white_baby.png");
    private static final Identifier GRADIENT =
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "textures/coat/redblackgradient.png");

    /**
     * How much a cache may hold, in texels - 8Mi, which is 32MiB of ARGB pixels
     * plus the same again on the GPU, per cache. Chosen as a memory figure
     * rather than as a horse count on purpose: it is the number that has to stay
     * true at any sheet size, and at the current one it is comfortably more
     * coats than a client can have on screen.
     */
    private static final long MAX_TEXELS_PER_CACHE = 8L * 1024 * 1024;

    /**
     * ...but never fewer than this many coats, whatever the sheet size. The
     * budget alone would allow only 32 entries at a 512px sheet, and a cache
     * smaller than the number of horses in view does not merely thrash - it can
     * release a texture that an already-submitted draw call is still going to
     * read. This floor keeps the LRU comfortably larger than any plausible
     * frame.
     */
    private static final int MIN_ENTRIES = 64;

    private static final long BUDGET = Math.max(MAX_TEXELS_PER_CACHE, (long) MIN_ENTRIES * N * N);

    /** One composed sheet's worth of texels; a {@code null} value holds no texture at all. */
    private static final TexelBudgetCache.Cost<Identifier> SHEET_COST =
            id -> id == null ? 0L : (long) N * N;

    /**
     * Reverse of the coat cache, kept purely as a tripwire: two texture keys
     * landing on one {@link Identifier} is the bug that made chestnuts and bays
     * render as the bare white template (see {@link CoatTextureId}), and it is
     * silent - {@code TextureManager#register} just overwrites and closes the
     * loser. {@code CoatTextureId} is injective so this can't fire; it's here so
     * a future change to the id scheme can't reintroduce the bug quietly.
     *
     * <p>An evicted key is removed from here too, so the tripwire only ever
     * covers ids that are <i>live at the same time</i> - which is exactly when
     * the collision would do damage, since a released id has no texture left to
     * overwrite.
     */
    private static final Map<Identifier, String> KEY_BY_ID = new ConcurrentHashMap<>();

    private static final TexelBudgetCache<String, Identifier> CACHE =
            new TexelBudgetCache<>(BUDGET, SHEET_COST, (key, id) -> release(id, KEY_BY_ID));

    /**
     * Emissive-mask textures, one per (coat, foal/adult, emissive part set). The
     * mask carries the composed coat's own colour on the named parts and is
     * transparent everywhere else; {@code EmissiveCoatLayer} draws it full-bright
     * over the base coat. Keyed off the same texture key so it invalidates with
     * the coat.
     *
     * <p>A horse with nothing emissive caches a <b>null</b>, which is a real
     * entry costing no texels: without it every ordinary horse would recompose
     * its whole coat every frame to rediscover that nothing glows.
     */
    private static final Map<Identifier, String> EMISSIVE_KEY_BY_ID = new ConcurrentHashMap<>();

    private static final TexelBudgetCache<String, Identifier> EMISSIVE_CACHE =
            new TexelBudgetCache<>(BUDGET, SHEET_COST, (key, id) -> release(id, EMISSIVE_KEY_BY_ID));

    /** The one place a generated texture is handed back to the game. */
    private static void release(Identifier id, Map<Identifier, String> reverse) {
        if (id == null) {
            return; // the "nothing glows" marker - never registered
        }
        reverse.remove(id);
        Minecraft.getInstance().getTextureManager().release(id);
    }

    private static volatile int[] adultTemplate;
    private static volatile int[] babyTemplate;
    /** The natural red/black gradient plus every alternate LUT the LUT locus can select. */
    private static volatile LutSet lutSet;

    private GeneticCoatTextureFactory() {
    }

    public static Identifier getOrCreate(CoatData coat, boolean baby) {
        return getOrCreate(coat, baby, null);
    }

    /** {@code breedLabel} is dev-build cosmetic only - the [coat] chat line. */
    public static Identifier getOrCreate(CoatData coat, boolean baby, String breedLabel) {
        String key = coat.textureKey() + (baby ? ":foal" : ":adult");
        return CACHE.get(key, k -> generate(coat, baby, k, breedLabel));
    }

    /**
     * The full-bright mask for one horse, or {@code null} when nothing on it
     * glows. Two sources are folded together, because both kinds of gene can ask
     * for one:
     * <ul>
     *   <li>a data-driven {@code glow} effect's {@code parts} list, passed in by
     *       the renderer as whole body parts;</li>
     *   <li>the <b>texel mask the coat bake itself produced</b>
     *       ({@link CoatTextureComposer.Baked#emissive()}) - a built-in gene
     *       writing it in the overlay phase, which is how the light locus lights
     *       four hooves and two eyes rather than four whole legs and a head.</li>
     * </ul>
     * Whatever is emissive is painted with the coat colour the composer produced
     * there; the rest is transparent.
     */
    public static Identifier getOrCreateEmissive(CoatData coat, boolean baby, Set<Part> parts) {
        Set<Part> wanted = parts == null ? Set.of() : parts;
        StringBuilder tag = new StringBuilder();
        for (Part part : new TreeSet<>(wanted)) {
            tag.append(tag.isEmpty() ? "" : "+").append(part.name());
        }
        // The gene-written half of the mask is a function of the texture key
        // already, so only the spec parts need to appear in the cache key.
        String key = coat.textureKey() + (baby ? ":foal" : ":adult") + ":glow:" + tag;
        return EMISSIVE_CACHE.get(key, k -> generateEmissive(coat, baby, wanted, k));
    }

    private static Identifier generateEmissive(CoatData coat, boolean baby, Set<Part> parts, String key) {
        ensureAssetsLoaded();
        Skin skin = baby ? Skin.BABY : Skin.ADULT;
        int[] template = baby ? babyTemplate : adultTemplate;
        CoatTextureComposer.Baked baked =
                CoatTextureComposer.bake(coat.genotype(), coat.epigenome(), skin, !baby, template, lutSet);
        int[] argb = baked.argb();
        boolean[] byGene = baked.emissive();

        if (parts.isEmpty() && byGene == null) {
            return null; // nothing on this horse glows - cached as a real, free entry
        }

        int[] mask = new int[N * N];
        boolean[] any = {false};
        if (byGene != null) {
            for (int i = 0; i < mask.length; i++) {
                int c = argb[i];
                if (byGene[i] && (c >>> 24) != 0) {
                    mask[i] = 0xFF000000 | (c & 0xFFFFFF);
                    any[0] = true;
                }
            }
        }
        for (Part part : parts) {
            HorseSkinGeometry.forEachTexel(skin, part, (px, py, p2, face, point) -> {
                int i = py * N + px;
                int c = argb[i];
                if ((c >>> 24) != 0) {
                    mask[i] = 0xFF000000 | (c & 0xFFFFFF);
                    any[0] = true;
                }
            });
        }
        if (!any[0]) {
            return null; // e.g. a mane glow on a foal, which has no mane
        }

        NativeImage image = new NativeImage(N, N, false);
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                image.setPixel(x, y, mask[y * N + x]);
            }
        }
        DynamicTexture texture = new DynamicTexture(() -> "horsegenetics_glow_" + key, image);
        // The composed key can be long at 13 genotype segments; a hash keeps the
        // Identifier path well inside the 256-char limit. A tripwire guards the
        // (astronomically unlikely) collision rather than letting it be silent.
        Identifier id = Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID,
                "coat_glow/" + Integer.toUnsignedString(key.hashCode(), 16));
        String previous = EMISSIVE_KEY_BY_ID.put(id, key);
        if (previous != null && !previous.equals(key)) {
            throw new IllegalStateException("emissive coat id collision on " + id
                    + ": '" + previous + "' vs '" + key + "'");
        }
        Minecraft.getInstance().getTextureManager().register(id, texture);
        return id;
    }

    private static Identifier generate(CoatData coat, boolean baby, String key, String breedLabel) {
        ensureAssetsLoaded();
        Skin skin = baby ? Skin.BABY : Skin.ADULT;
        int[] template = baby ? babyTemplate : adultTemplate;
        int[] argb = CoatTextureComposer.compose(coat.genotype(), coat.epigenome(), skin, !baby, template, lutSet);

        if (ClientConfig.debugTools()) {
            debugLogCoat(coat, baby, argb, template, breedLabel);
        }

        NativeImage image = new NativeImage(N, N, false);
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                image.setPixel(x, y, argb[y * N + x]);
            }
        }
        DynamicTexture texture = new DynamicTexture(() -> "horsegenetics_coat_" + key, image);
        Identifier id = Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "coat/" + CoatTextureId.encode(key));
        String previous = KEY_BY_ID.put(id, key);
        if (previous != null && !previous.equals(key)) {
            throw new IllegalStateException("coat texture id collision on " + id
                    + ": '" + previous + "' vs '" + key + "' - CoatTextureId is no longer injective");
        }
        Minecraft.getInstance().getTextureManager().register(id, texture);
        return id;
    }

    /**
     * One line per baked coat, <b>to the log</b> - it used to go to chat as well,
     * and every horse loading in printed its genotype there, which drowned
     * everything else (owner's request, 2026-09-10). Chat still gets the one
     * line that is a problem: the failure where the whole overlay came out
     * transparent and the horse renders as the bare white template
     * ("FLAT WHITE").
     */
    private static void debugLogCoat(CoatData coat, boolean baby, int[] argb, int[] template, String breedLabel) {
        Minecraft mc = Minecraft.getInstance();
        boolean bareTemplate = Arrays.equals(argb, template);
        String msg = "[coat] " + (baby ? "foal  " : "adult ")
                + GeneCodeDisplay.shortForm(coat.genotype())
                + " @" + Long.toUnsignedString(coat.epigenome().visibleFingerprint(coat.genotype()), 16)
                + (coat.isDeterministic() ? "  det" : "  per-horse")
                + (breedLabel == null ? "" : "  [" + breedLabel + "]")
                + (bareTemplate ? "  >> FLAT WHITE (overlay fully transparent)" : "");
        HorseGenetics.LOGGER.info(msg);
        if (bareTemplate && mc.player != null) {
            mc.player.sendSystemMessage(Component.literal(msg));
        }
    }

    private static synchronized void ensureAssetsLoaded() {
        if (adultTemplate != null && babyTemplate != null && lutSet != null) {
            return;
        }
        adultTemplate = loadArgb(ADULT_TEMPLATE, N, N);
        babyTemplate = loadArgb(BABY_TEMPLATE, N, N);

        GradientLut base = loadLut(GRADIENT);

        // The LUT locus (horsegenetics.lut) names one texture per variant
        // allele; a horse homozygous for that allele resolves against it in
        // phase 2 instead of the red/black gradient. Any gene may in principle
        // implement LutContribution, so we walk them all.
        Map<String, GradientLut> alternates = new HashMap<>();
        for (Gene gene : Genes.codeOrder()) {
            if (!(gene instanceof LutContribution lut)) {
                continue;
            }
            lut.lutResources().forEach((key, path) -> {
                if (alternates.containsKey(key)) {
                    return;
                }
                Identifier id = Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, path);
                try {
                    alternates.put(key, loadLut(id));
                } catch (RuntimeException e) {
                    // A missing alternate LUT falls back to the base gradient
                    // (LutSet.resolve does), so a bad path is a warning, not a crash.
                    HorseGenetics.LOGGER.warn("could not load alternate LUT '{}' ({}) - "
                            + "horses homozygous for it will render with the natural gradient", key, id, e);
                }
            });
        }
        lutSet = new LutSet(base, alternates);
    }

    private static GradientLut loadLut(Identifier location) {
        try (InputStream in = Minecraft.getInstance().getResourceManager().getResourceOrThrow(location).open();
             NativeImage img = NativeImage.read(in)) {
            int w = img.getWidth();
            int h = img.getHeight();
            int[] px = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    px[y * w + x] = img.getPixel(x, y);
                }
            }
            return new GradientLut(px, w, h);
        } catch (IOException e) {
            throw new RuntimeException("failed to load coat LUT " + location, e);
        }
    }

    private static int[] loadArgb(Identifier location, int w, int h) {
        try (InputStream in = Minecraft.getInstance().getResourceManager().getResourceOrThrow(location).open();
             NativeImage img = NativeImage.read(in)) {
            if (img.getWidth() != w || img.getHeight() != h) {
                throw new IllegalStateException(location + " must be " + w + "x" + h
                        + ", got " + img.getWidth() + "x" + img.getHeight());
            }
            int[] px = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    px[y * w + x] = img.getPixel(x, y);
                }
            }
            return px;
        } catch (IOException e) {
            throw new RuntimeException("failed to load " + location, e);
        }
    }

    /**
     * Release every generated coat texture and drop the pack resources behind
     * them. Called on world exit and on a client resource reload; the caches
     * free each entry through the same eviction hook the LRU uses, so there is
     * one release path rather than two.
     */
    public static void clear() {
        // The session's only report on the LRU, and the one place the owner can
        // see from a log whether it ever had to evict anything. Written before
        // the clear, because after it every number is zero.
        HorseGenetics.LOGGER.info(
                "releasing {} coat textures and {} glow masks; {} coats and {} masks were recycled "
                        + "for space during the session (budget {} texels each)",
                CACHE.size(), EMISSIVE_CACHE.size(),
                CACHE.evictionCount(), EMISSIVE_CACHE.evictionCount(), BUDGET);
        CACHE.clear();
        EMISSIVE_CACHE.clear();
        KEY_BY_ID.clear();
        EMISSIVE_KEY_BY_ID.clear();
        adultTemplate = null;
        babyTemplate = null;
        lutSet = null;
    }
}
