/*
 * Derived from UsefulCarts (https://github.com/Andrewwwwwwwwwwwwwww/usefulcarts-mc26.1.2),
 * itself a port of NiftyCarts by jmb19905, originally AstikorCarts by MennoMax.
 * Copyright (c) 2019 MennoMax
 * Copyright (c) 2023 jmb19905
 * Licensed under the MIT License. See LICENSES/UsefulCarts-MIT.txt.
 * Modified for Horse Genetics (NeoForge 26.1.2).
 */
package com.example.horsegenetics.neoforge.carts.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class PreparedMaterial {
    private final ObjectList<Fill> fills;

    private final TextureAtlasSprite sprite;

    private final int resolution;

    PreparedMaterial(final ObjectList<Fill> fills, final TextureAtlasSprite sprite, final int resolution) {
        this.fills = fills;
        this.sprite = sprite;
        this.resolution = resolution;
    }

    int getResolution() {
        return this.resolution;
    }

    void draw(final NativeImage image, final int resolution) {
        for (final Fill m : this.fills) {
            m.fill(image, this.sprite, this.resolution, resolution);
        }
    }
}
