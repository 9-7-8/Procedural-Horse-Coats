package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.coat.CoatData;
import com.example.horsegenetics.common.coat.pattern.CoatTextureComposer;
import com.example.horsegenetics.common.coat.pattern.GradientLut;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs the common {@code CoatTextureComposer} pipeline and uploads the result
 * as a {@link DynamicTexture}, cached by {@link CoatData#textureKey()}.
 *
 * <p>Deterministic coats (black, chestnut, champagne, white) share one texture
 * per genotype code; non-deterministic coats (bay, seal, markings) key on the
 * epigenetic seed too, so every such horse gets its own. The white-horse
 * template and the red/black gradient are loaded once from the mod's assets.
 */
public final class GeneticCoatTextureFactory {

    private static final int N = HorseSkinGeometry.SHEET_SIZE;

    private static final Identifier WHITE_TEMPLATE =
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "textures/entity/horse/horse_white.png");
    private static final Identifier GRADIENT =
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "textures/coat/redblackgradient.png");

    private static final Map<String, Identifier> CACHE = new ConcurrentHashMap<>();

    private static volatile int[] template;
    private static volatile GradientLut gradient;

    private GeneticCoatTextureFactory() {
    }

    public static Identifier getOrCreate(CoatData coat) {
        return CACHE.computeIfAbsent(coat.textureKey(), key -> generate(coat, key));
    }

    private static Identifier generate(CoatData coat, String key) {
        ensureAssetsLoaded();
        int[] argb = CoatTextureComposer.compose(coat.genotype(), coat.epigeneticSeed(), template, gradient);

        NativeImage image = new NativeImage(N, N, false);
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                image.setPixel(x, y, argb[y * N + x]);
            }
        }
        DynamicTexture texture = new DynamicTexture(() -> "horsegenetics_coat_" + key, image);
        Identifier id = Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "coat/" + sanitize(key));
        Minecraft.getInstance().getTextureManager().register(id, texture);
        return id;
    }

    private static synchronized void ensureAssetsLoaded() {
        if (template != null && gradient != null) {
            return;
        }
        template = loadArgb(WHITE_TEMPLATE, N, N);

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
        template = null;
        gradient = null;
    }

    private static String sanitize(String key) {
        return key.replaceAll("[^A-Za-z0-9_]", "_").toLowerCase(Locale.ROOT);
    }
}
