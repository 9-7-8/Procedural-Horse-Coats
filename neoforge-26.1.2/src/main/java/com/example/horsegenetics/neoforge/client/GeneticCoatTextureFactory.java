package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.coat.CoatData;
import com.example.horsegenetics.common.coat.CoatTextureId;
import com.example.horsegenetics.common.coat.pattern.CoatTextureComposer;
import com.example.horsegenetics.common.coat.pattern.GradientLut;
import com.example.horsegenetics.common.genetics.GeneCodeDisplay;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.loading.FMLEnvironment;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs the {@code CoatTextureComposer} pipeline for a horse and uploads the
 * result as a {@link DynamicTexture}, cached by {@link CoatData#textureKey()}
 * plus adult/foal (grey greys only adults, and the foal uses a different mesh /
 * template). The two white templates + the red/black gradient load once.
 */
public final class GeneticCoatTextureFactory {

    private static final int N = HorseSkinGeometry.SHEET_SIZE;

    private static final Identifier ADULT_TEMPLATE =
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "textures/entity/horse/horse_white.png");
    private static final Identifier BABY_TEMPLATE =
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "textures/entity/horse/horse_white_baby.png");
    private static final Identifier GRADIENT =
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "textures/coat/redblackgradient.png");

    private static final Map<String, Identifier> CACHE = new ConcurrentHashMap<>();

    /**
     * Reverse of {@link #CACHE}, kept purely as a tripwire: two texture keys
     * landing on one {@link Identifier} is the bug that made chestnuts and bays
     * render as the bare white template (see {@link CoatTextureId}), and it is
     * silent - {@code TextureManager#register} just overwrites and closes the
     * loser. {@code CoatTextureId} is injective so this can't fire; it's here so
     * a future change to the id scheme can't reintroduce the bug quietly.
     */
    private static final Map<Identifier, String> KEY_BY_ID = new ConcurrentHashMap<>();

    private static volatile int[] adultTemplate;
    private static volatile int[] babyTemplate;
    private static volatile GradientLut gradient;

    private GeneticCoatTextureFactory() {
    }

    public static Identifier getOrCreate(CoatData coat, boolean baby) {
        String key = coat.textureKey() + (baby ? ":foal" : ":adult");
        return CACHE.computeIfAbsent(key, k -> generate(coat, baby, k));
    }

    private static Identifier generate(CoatData coat, boolean baby, String key) {
        ensureAssetsLoaded();
        Skin skin = baby ? Skin.BABY : Skin.ADULT;
        int[] template = baby ? babyTemplate : adultTemplate;
        int[] argb = CoatTextureComposer.compose(coat.genotype(), coat.epigenome(), skin, !baby, template, gradient);

        if (!FMLEnvironment.isProduction()) {
            debugLogCoat(coat, baby, argb, template);
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
     * Dev-build only: drop a line in chat every time a coat texture is baked, so
     * the compose pipeline can be watched live. Flags the failure mode where the
     * whole overlay came out transparent and the horse will render as the bare
     * white template ("FLAT WHITE").
     */
    private static void debugLogCoat(CoatData coat, boolean baby, int[] argb, int[] template) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        boolean bareTemplate = Arrays.equals(argb, template);
        String msg = "[coat] " + (baby ? "foal  " : "adult ")
                + GeneCodeDisplay.shortForm(coat.genotype())
                + " @" + Long.toUnsignedString(coat.epigenome().visibleFingerprint(coat.genotype()), 16)
                + (coat.isDeterministic() ? "  det" : "  per-horse")
                + (bareTemplate ? "  >> FLAT WHITE (overlay fully transparent)" : "");
        mc.player.sendSystemMessage(Component.literal(msg));
    }

    private static synchronized void ensureAssetsLoaded() {
        if (adultTemplate != null && babyTemplate != null && gradient != null) {
            return;
        }
        adultTemplate = loadArgb(ADULT_TEMPLATE, N, N);
        babyTemplate = loadArgb(BABY_TEMPLATE, N, N);

        try (InputStream in = Minecraft.getInstance().getResourceManager().getResourceOrThrow(GRADIENT).open();
             NativeImage img = NativeImage.read(in)) {
            int w = img.getWidth();
            int h = img.getHeight();
            int[] px = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    px[y * w + x] = img.getPixel(x, y);
                }
            }
            gradient = new GradientLut(px, w, h);
        } catch (IOException e) {
            throw new RuntimeException("failed to load coat gradient " + GRADIENT, e);
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

    /** Release every generated coat texture (called on world exit). */
    public static void clear() {
        var textureManager = Minecraft.getInstance().getTextureManager();
        for (Identifier id : CACHE.values()) {
            textureManager.release(id);
        }
        CACHE.clear();
        KEY_BY_ID.clear();
        adultTemplate = null;
        babyTemplate = null;
        gradient = null;
    }
}
