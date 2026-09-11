package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * <b>The baked picture of a gene</b> - the same one-horse icon its wiki page and
 * the horse designer's hover card show - as a GUI texture.
 *
 * <p>The icons are baked by {@code :common:bakeGeneIcons} into
 * {@code wiki/assets/gene-icons/}, and the NeoForge build copies them into the
 * jar under {@code assets/horsegenetics/textures/gui/gene_icons/} rather than the
 * repository carrying them twice. So a gene that has an icon on the wiki has
 * one in game, and one baked since the last build does not - rebuild.
 *
 * <p>A gene with <b>no</b> icon - a drop-in gene, or one that paints nothing and
 * so was never photographed - answers {@code null}, and the caller draws no
 * picture. Asking the texture manager for a missing texture would draw the
 * purple-and-black checkerboard instead, which reads as a bug rather than as
 * "nothing to show". The answer is cached per gene: the resource pack stack
 * does not change while a screen is open, and a hover card is drawn every frame.
 */
public final class GeneIcons {

    private static final Map<String, Optional<Identifier>> CACHE = new HashMap<>();

    private GeneIcons() {
    }

    /** The icon texture for {@code gene}, or {@code null} if this build has none. */
    public static Identifier iconFor(Gene gene) {
        return CACHE.computeIfAbsent(gene.key(), GeneIcons::find).orElse(null);
    }

    private static Optional<Identifier> find(String key) {
        String slug = key.substring(key.indexOf('.') + 1);
        Identifier id;
        try {
            id = Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "textures/gui/gene_icons/" + slug + ".png");
        } catch (RuntimeException invalid) {
            return Optional.empty();   // a drop-in gene whose key is not a valid path
        }
        return Minecraft.getInstance().getResourceManager().getResource(id).isPresent()
                ? Optional.of(id) : Optional.empty();
    }
}
