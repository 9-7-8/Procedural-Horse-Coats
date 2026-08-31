package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.coat.CoatData;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is the "generate layers by rule rather than shipping PNGs" piece:
 * for bay horses, we load the vanilla base coat texture and the vanilla
 * black texture, then composite them ourselves at runtime by copying leg
 * pixels from the black texture into the base texture up to a height
 * determined by CoatData.legBlackHeight(). The result is uploaded as a
 * DynamicTexture and cached by a bucketed height so we don't generate a
 * new texture for every single horse (small variations in height reuse
 * the same generated texture).
 *
 * <p><b>TODO before this looks right in-game:</b> LEG_REGIONS below is a
 * placeholder. Open horse_brown.png and horse_black.png (both 64x64) in
 * an image editor, find the pixel rectangles the four legs occupy, and
 * fill in real values. Everything else in this class is version-specific
 * plumbing that should work as-is once those coordinates are correct.
 */
public final class GeneticCoatTextureFactory {

    private static final Identifier BASE_BAY_TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/horse/horse_brown.png");
    private static final Identifier BLACK_OVERLAY_TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/horse/horse_black.png");

    // Bucket the continuous [0,1] height into 10 steps so we generate at
    // most 10 textures total for bay horses, not one per horse.
    private static final int HEIGHT_BUCKETS = 10;

    private static final Map<Integer, Identifier> CACHE = new ConcurrentHashMap<>();

    // Pixel rectangle on the 64x64 horse texture that holds the legs, as
    // {x, y, width, height}. All four legs of the vanilla equine model share
    // ONE texture region: AbstractEquineModel puts every leg at texOffs(48, 21)
    // as a 4x11x4 cube, so the four side faces unwrap to U 48..64, V 25..36
    // (the earlier placeholder pointed at U/V 0,0 - the head - which is why the
    // "black" was landing on the face). compositeLegRegion fills this bottom-up,
    // so a small legBlackHeight only darkens the hoof end.
    private static final int[][] LEG_REGIONS = {
            {48, 25, 16, 11},
    };

    public static Identifier getOrCreate(CoatData coatData) {
        int bucket = Math.round(coatData.legBlackHeight() * HEIGHT_BUCKETS);
        return CACHE.computeIfAbsent(bucket, b -> generate((float) b / HEIGHT_BUCKETS));
    }

    private static Identifier generate(float legBlackHeight) {
        // 'base' is intentionally NOT in the try-with-resources: ownership of it
        // transfers to the DynamicTexture (which closes it on its own close()),
        // so closing it here too would double-free the native image.
        NativeImage base = null;
        try (NativeImage overlay = loadVanillaTexture(BLACK_OVERLAY_TEXTURE)) {
            base = loadVanillaTexture(BASE_BAY_TEXTURE);

            for (int[] region : LEG_REGIONS) {
                compositeLegRegion(base, overlay, region, legBlackHeight);
            }

            int heightPercent = Math.round(legBlackHeight * 100);
            DynamicTexture dynamicTexture = new DynamicTexture(() -> "horsegenetics_bay_" + heightPercent, base);
            Identifier id = Identifier.fromNamespaceAndPath(
                    HorseGenetics.MOD_ID, "bay_generated_" + heightPercent);
            Minecraft.getInstance().getTextureManager().register(id, dynamicTexture);
            return id;
        } catch (IOException e) {
            if (base != null) {
                base.close();
            }
            throw new RuntimeException("Failed to composite bay horse texture", e);
        }
    }

    /**
     * Copies overlay pixels into base within the given region, starting from
     * the bottom of the region (the hoof) and covering upward proportional to
     * legBlackHeight. 0.0 = no black copied, 1.0 = whole region copied.
     */
    private static void compositeLegRegion(NativeImage base, NativeImage overlay, int[] region, float legBlackHeight) {
        int x = region[0], y = region[1], w = region[2], h = region[3];
        int blackRows = Math.round(h * legBlackHeight);
        int startY = y + (h - blackRows); // bottom-up
        for (int row = startY; row < y + h; row++) {
            for (int col = x; col < x + w; col++) {
                base.setPixel(col, row, overlay.getPixel(col, row));
            }
        }
    }

    /**
     * Release every generated bay texture and forget the cache. Called when the
     * player leaves a world so the textures don't outlive the horse database
     * they were made for.
     */
    public static void clear() {
        var textureManager = Minecraft.getInstance().getTextureManager();
        for (Identifier id : CACHE.values()) {
            textureManager.release(id);
        }
        CACHE.clear();
    }

    private static NativeImage loadVanillaTexture(Identifier location) throws IOException {
        // NativeImage.read expects an InputStream; resource manager lookup
        // omitted here for brevity - wire this to Minecraft.getInstance()
        // .getResourceManager().open(location) in the real implementation.
        var resource = Minecraft.getInstance().getResourceManager().getResourceOrThrow(location);
        try (var stream = resource.open()) {
            return NativeImage.read(stream);
        }
    }

    private GeneticCoatTextureFactory() {
    }
}
