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
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

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
 * <h2>Baking is rationed, and a horse is baked once</h2>
 * On 2026-09-13 walking toward a herd froze the owner's whole machine badly
 * enough to need a reboot. A thread dump put it on the render thread (173 s of
 * CPU against the server thread's 32 s), and the cause was here: the renderer
 * asks for a horse's textures <i>while extracting a frame</i>, and every horse
 * seen for the first time was composed and uploaded right then - <b>twice</b>,
 * once for its glow mask and again for its coat, even though most horses glow
 * with nothing. A herd coming into view was dozens of full bakes inside single
 * frames.
 *
 * <p>Three things fix that, and all three live here or in the renderer:
 * <ul>
 *   <li><b>One bake per horse.</b> {@link #resolve} runs the composer once and
 *       fills the coat and the glow cache from the same result.</li>
 *   <li><b>A time budget.</b> {@link #mayBake} lets a new bake start only
 *       while less than {@code coats.bakeBudgetMs} has been spent in the
 *       current {@value #WINDOW_MS} ms window - always at least one, so a coat
 *       is never starved. A horse that has to wait shows {@link #placeholder}
 *       for a frame or two.</li>
 *   <li><b>Distance.</b> The renderer only allows a new bake for a horse inside
 *       {@code coats.detailDistance}; one further out wears the stand-in until
 *       you come closer. A coat that already exists is used at any range, so
 *       nothing flickers at the boundary.</li>
 * </ul>
 * Whether a horse is on screen needs no check of ours: vanilla's
 * {@code LevelRenderer.extractVisibleEntities} culls against the frustum
 * before it extracts, so an off-screen horse never reaches the renderer.
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

    /**
     * <b>Which keys each cache holds right now</b>, as membership only. This is
     * what lets a caller ask "is this coat already made?" without the cache's
     * loader running - the question the budget and the distance check both
     * need, and one {@link TexelBudgetCache} cannot answer on its own because
     * {@code get} always loads on a miss. Kept in step through the eviction
     * hooks, so an evicted coat stops counting as made the moment it is freed.
     */
    private static final Set<String> LIVE_COATS = ConcurrentHashMap.newKeySet();
    private static final Set<String> LIVE_GLOWS = ConcurrentHashMap.newKeySet();

    private static final TexelBudgetCache<String, Identifier> CACHE =
            new TexelBudgetCache<>(BUDGET, SHEET_COST, (key, id) -> {
                LIVE_COATS.remove(key);
                release(id, KEY_BY_ID);
            });

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
            new TexelBudgetCache<>(BUDGET, SHEET_COST, (key, id) -> {
                LIVE_GLOWS.remove(key);
                release(id, EMISSIVE_KEY_BY_ID);
            });

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

    // ------------------------------------------------------------------
    // The bake budget. Render thread only, like every caller of this class.
    // ------------------------------------------------------------------

    /** The window the time budget is counted over. */
    private static final int WINDOW_MS = 50;
    private static final long WINDOW_NANOS = WINDOW_MS * 1_000_000L;

    /**
     * Started one window in the past, so the first call opens a fresh one.
     * Deliberately a difference of two {@code nanoTime} readings and never a
     * sentinel: {@code nanoTime} may be negative, and a subtraction against a
     * made-up extreme overflows - which is exactly how the world-flag cache
     * switched itself off earlier the same day.
     */
    private static long windowStart = System.nanoTime() - WINDOW_NANOS;
    private static long spentInWindow;
    private static boolean bakedInWindow;

    /** Session diagnostics, reported by {@link #clear()} and then reset. */
    private static long bakes;
    private static long bakeNanos;
    private static long slowestBakeNanos;
    private static long deferrals;

    private GeneticCoatTextureFactory() {
    }

    /** A horse's two textures for one frame: its coat, and its glow mask or {@code null}. */
    public record Resolved(Identifier coat, @Nullable Identifier glow) {
    }

    /**
     * <b>The renderer's one call.</b> The coat and the glow mask for a horse,
     * baked together if either is missing and the budget allows - or, when it
     * does not, whatever already exists plus the {@linkplain #placeholder
     * stand-in} for whatever does not.
     *
     * @param parts        body parts a {@code glow} effect lights outright
     * @param breedLabel   dev-build cosmetic only - the {@code [coat]} log line
     * @param mayStartBake {@code false} for a horse beyond the detail distance:
     *                     it may use a coat that exists, never cause a new one
     */
    public static Resolved resolve(CoatData coat, boolean baby, @Nullable Set<Part> parts,
                                   @Nullable String breedLabel, boolean mayStartBake) {
        Set<Part> wanted = parts == null ? Set.of() : parts;
        String coatKey = coatKey(coat, baby);
        String glowKey = glowKey(coatKey, wanted);
        boolean haveCoat = LIVE_COATS.contains(coatKey);
        boolean haveGlow = LIVE_GLOWS.contains(glowKey);

        if (!(haveCoat && haveGlow) && mayStartBake && mayBake()) {
            // ONE composer run feeds both caches. Whichever of the two already
            // exists is a cache hit and ignores the loader.
            return timed(() -> {
                CoatTextureComposer.Baked baked = bake(coat, baby);
                Identifier coatId = CACHE.get(coatKey,
                        k -> registerCoat(coat, baby, k, baked.argb(), breedLabel));
                Identifier glowId = EMISSIVE_CACHE.get(glowKey,
                        k -> registerGlow(baby, wanted, k, baked));
                return new Resolved(coatId, glowId);
            });
        }
        // Counted only when the BUDGET said no. A horse beyond the detail
        // distance is not waiting its turn, it is not asking - counting it would
        // add every distant horse on every frame and bury the one number the
        // shutdown line exists to report.
        if (!(haveCoat && haveGlow) && mayStartBake) {
            deferrals++;
        }
        // The loaders below only run if an entry vanished between the membership
        // check and the lookup, which a single render thread cannot do - they
        // are there so a surprise costs a bake rather than a missing texture.
        Identifier coatId = haveCoat
                ? CACHE.get(coatKey, coatLoader(coat, baby, breedLabel))
                : placeholder(baby);
        Identifier glowId = haveGlow
                ? EMISSIVE_CACHE.get(glowKey, glowLoader(coat, baby, wanted))
                : null;
        return new Resolved(coatId, glowId);
    }

    public static Identifier getOrCreate(CoatData coat, boolean baby) {
        return getOrCreate(coat, baby, null);
    }

    /**
     * The coat alone, for a screen or an item rather than a horse in the world:
     * the family tree, the portrait well, a transfer paper. Budgeted like
     * everything else - a family tree of thirty strangers used to bake thirty
     * coats in one frame - but with no distance, because nothing on a screen is
     * far away. A coat that has to wait shows the stand-in and is asked for
     * again next frame.
     */
    public static Identifier getOrCreate(CoatData coat, boolean baby, @Nullable String breedLabel) {
        String key = coatKey(coat, baby);
        if (LIVE_COATS.contains(key) || mayBake()) {
            return CACHE.get(key, coatLoader(coat, baby, breedLabel));
        }
        deferrals++;
        return placeholder(baby);
    }

    /**
     * <b>What a horse wears while its own coat waits.</b> The wild-type coat,
     * baked through the ordinary pipeline and shared by every horse still in
     * the queue - so it costs one bake per mesh for a whole session, not one
     * per horse.
     *
     * <p>Not vanilla's horse texture, although that was the obvious idea: the
     * adult HD model moves the legs and ears onto patches of their own, so a
     * vanilla sheet would put the wrong texels on exactly the parts a moving
     * horse shows most. Exempt from the budget, because the fallback cannot
     * itself be something that waits.
     */
    private static Identifier placeholder(boolean baby) {
        return CACHE.get(coatKey(CoatData.DEFAULT, baby), coatLoader(CoatData.DEFAULT, baby, null));
    }

    /**
     * May a new bake start now? Always the first one in a window, so a coat can
     * never be starved; after that only while the window's spend is under
     * {@code coats.bakeBudgetMs}. A budget of zero is therefore "one bake per
     * {@value #WINDOW_MS} ms, whatever it costs".
     */
    private static boolean mayBake() {
        long now = System.nanoTime();
        if (now - windowStart >= WINDOW_NANOS) {
            windowStart = now;
            spentInWindow = 0L;
            bakedInWindow = false;
        }
        return !bakedInWindow || spentInWindow < ClientConfig.coatBakeBudgetMs() * 1_000_000L;
    }

    /** Run a bake-and-upload and put what it cost against the window and the session. */
    private static <T> T timed(Supplier<T> work) {
        long started = System.nanoTime();
        try {
            return work.get();
        } finally {
            long took = System.nanoTime() - started;
            bakedInWindow = true;
            spentInWindow += took;
            bakes++;
            bakeNanos += took;
            slowestBakeNanos = Math.max(slowestBakeNanos, took);
        }
    }

    private static String coatKey(CoatData coat, boolean baby) {
        return coat.textureKey() + (baby ? ":foal" : ":adult");
    }

    /** The gene-written half of a mask is a function of the texture key already, so only spec parts join it. */
    private static String glowKey(String coatKey, Set<Part> parts) {
        StringBuilder tag = new StringBuilder();
        for (Part part : new TreeSet<>(parts)) {
            tag.append(tag.isEmpty() ? "" : "+").append(part.name());
        }
        return coatKey + ":glow:" + tag;
    }

    private static TexelBudgetCache.Loader<String, Identifier> coatLoader(CoatData coat, boolean baby,
                                                                         @Nullable String breedLabel) {
        return k -> timed(() -> registerCoat(coat, baby, k, bake(coat, baby).argb(), breedLabel));
    }

    private static TexelBudgetCache.Loader<String, Identifier> glowLoader(CoatData coat, boolean baby,
                                                                         Set<Part> parts) {
        return k -> timed(() -> registerGlow(baby, parts, k, bake(coat, baby)));
    }

    private static CoatTextureComposer.Baked bake(CoatData coat, boolean baby) {
        ensureAssetsLoaded();
        return CoatTextureComposer.bake(coat.genotype(), coat.epigenome(),
                baby ? Skin.BABY : Skin.ADULT, !baby, baby ? babyTemplate : adultTemplate, lutSet);
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
    private static @Nullable Identifier registerGlow(boolean baby, Set<Part> parts, String key,
                                                     CoatTextureComposer.Baked baked) {
        Identifier id = buildGlow(baby, parts, key, baked);
        LIVE_GLOWS.add(key); // a null answer is an answer too - see EMISSIVE_CACHE
        return id;
    }

    private static @Nullable Identifier buildGlow(boolean baby, Set<Part> parts, String key,
                                                  CoatTextureComposer.Baked baked) {
        Skin skin = baby ? Skin.BABY : Skin.ADULT;
        int[] argb = baked.argb();
        float[] byGene = baked.emissive();

        if (parts.isEmpty() && byGene == null) {
            return null; // nothing on this horse glows - cached as a real, free entry
        }

        int[] mask = new int[N * N];
        boolean[] any = {false};
        if (byGene != null) {
            for (int i = 0; i < mask.length; i++) {
                int c = argb[i];
                // The gene's intensity becomes this texel's ALPHA. The emissive
                // pass blends (BlendFunction.TRANSLUCENT) over the coat as the
                // world lit it, so alpha 0.4 is four tenths of the full-bright
                // colour over six tenths of the ordinary one - a dimmer, not a
                // darker colour. Scaling the RGB instead would blend toward
                // black and a dim glow would read as a smudge.
                int alpha = Math.round(byGene[i] * 255f);
                if (alpha > 0 && (c >>> 24) != 0) {
                    mask[i] = (alpha << 24) | (c & 0xFFFFFF);
                    any[0] = true;
                }
            }
        }
        for (Part part : parts) {
            HorseSkinGeometry.forEachTexel(skin, part, (px, py, p2, face, point) -> {
                int i = py * N + px;
                int c = argb[i];
                // A whole part named by a `glow` effect is lit outright - the
                // effect has no intensity of its own, and full bright is what
                // "this part glows" has always meant.
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

    private static Identifier registerCoat(CoatData coat, boolean baby, String key, int[] argb,
                                           @Nullable String breedLabel) {
        if (ClientConfig.debugTools()) {
            debugLogCoat(coat, baby, argb, baby ? babyTemplate : adultTemplate, breedLabel);
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
        LIVE_COATS.add(key);
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
    private static void debugLogCoat(CoatData coat, boolean baby, int[] argb, int[] template,
                                     @Nullable String breedLabel) {
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
        // The session's only report on the LRU and on the bake budget, and the
        // one place the owner can see from a log whether either ever had to
        // act. Written before the clear, because after it every number is zero.
        HorseGenetics.LOGGER.info(
                "releasing {} coat textures and {} glow masks; {} coats and {} masks were recycled "
                        + "for space during the session (budget {} texels each)",
                CACHE.size(), EMISSIVE_CACHE.size(),
                CACHE.evictionCount(), EMISSIVE_CACHE.evictionCount(), BUDGET);
        HorseGenetics.LOGGER.info(
                "coat baking: {} bakes, {} ms in total, slowest {} ms; {} times a horse close enough "
                        + "for its own coat drew the stand-in because the budget was spent "
                        + "(budget {} ms per {} ms window)",
                bakes, bakeNanos / 1_000_000L, String.format("%.1f", slowestBakeNanos / 1_000_000.0),
                deferrals, ClientConfig.coatBakeBudgetMs(), WINDOW_MS);
        CACHE.clear();
        EMISSIVE_CACHE.clear();
        KEY_BY_ID.clear();
        EMISSIVE_KEY_BY_ID.clear();
        LIVE_COATS.clear();
        LIVE_GLOWS.clear();
        bakes = 0L;
        bakeNanos = 0L;
        slowestBakeNanos = 0L;
        deferrals = 0L;
        adultTemplate = null;
        babyTemplate = null;
        lutSet = null;
    }
}
