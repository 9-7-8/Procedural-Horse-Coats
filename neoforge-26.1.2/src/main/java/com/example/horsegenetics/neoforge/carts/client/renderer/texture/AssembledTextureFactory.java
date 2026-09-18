/*
 * Derived from UsefulCarts (https://github.com/Andrewwwwwwwwwwwwwww/usefulcarts-mc26.1.2),
 * itself a port of NiftyCarts by jmb19905, originally AstikorCarts by MennoMax.
 * Copyright (c) 2019 MennoMax
 * Copyright (c) 2023 jmb19905
 * Licensed under the MIT License. See LICENSES/UsefulCarts-MIT.txt.
 * Modified for Horse Genetics (NeoForge 26.1.2).
 */
package com.example.horsegenetics.neoforge.carts.client.renderer.texture;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

public class AssembledTextureFactory {
    private final Object2ObjectMap<Identifier, AssembledTexture> textures = new Object2ObjectOpenHashMap<>();

    public AssembledTextureFactory add(AssembledTexture texture) {
        this.textures.put(texture.getId(), texture);
        return this;
    }

    public void bake() {
        final Minecraft mc = Minecraft.getInstance();
        final ResourceManager resources = mc.getResourceManager();
        final TextureManager textures = mc.getTextureManager();
        final AtlasManager sprites = mc.getAtlasManager();
        Object2ObjectMaps.fastForEach(this.textures, e -> {
            if (resources.getResource(e.getKey()).isPresent()) {
                textures.release(e.getKey());
            } else {
                textures.register(e.getKey(), e.getValue().assemble(sprites));
            }
        });
    }
}
