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
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.resources.Identifier;

public class AssembledTexture {
    private final Identifier id;
    private final int width;
    private final int height;

    private final ObjectList<CartMaterial> materials = new ObjectArrayList<>();

    public AssembledTexture(Identifier id, final int width, final int height) {
        this.id = id;
        this.width = width;
        this.height = height;
    }

    public AssembledTexture add(final CartMaterial material) {
        this.materials.add(material);
        return this;
    }

    public Identifier getId() {
        return id;
    }

    AbstractTexture assemble(final AtlasManager sprites) {
        final PreparedMaterial[] prepared = new PreparedMaterial[this.materials.size()];
        int resolution = 1;
        for (final ObjectListIterator<CartMaterial> it = this.materials.iterator(); it.hasNext(); ) {
            final int i = it.nextIndex();
            final PreparedMaterial p = it.next().prepare(sprites);
            prepared[i] = p;
            resolution = Math.max(resolution, p.getResolution());
        }
        final NativeImage image = new NativeImage(this.width * resolution, this.height * resolution, true);
        for (final PreparedMaterial p : prepared) p.draw(image, resolution);
        return new DynamicTexture(id::toString, image);
    }
}
