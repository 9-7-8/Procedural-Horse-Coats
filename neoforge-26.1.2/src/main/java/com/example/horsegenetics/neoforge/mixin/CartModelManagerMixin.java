/*
 * Derived from UsefulCarts (https://github.com/Andrewwwwwwwwwwwwwww/usefulcarts-mc26.1.2),
 * itself a port of NiftyCarts by jmb19905, originally AstikorCarts by MennoMax.
 * Copyright (c) 2019 MennoMax
 * Copyright (c) 2023 jmb19905
 * Licensed under the MIT License. See LICENSES/UsefulCarts-MIT.txt.
 * Modified for Horse Genetics (NeoForge 26.1.2).
 */
package com.example.horsegenetics.neoforge.mixin;

import com.example.horsegenetics.neoforge.carts.CartWood;
import com.example.horsegenetics.neoforge.carts.HorseCarts;
import com.example.horsegenetics.neoforge.carts.client.renderer.texture.AssembledTexture;
import com.example.horsegenetics.neoforge.carts.client.renderer.texture.AssembledTextureFactory;
import com.example.horsegenetics.neoforge.carts.client.renderer.texture.CartMaterial;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * <b>Builds every cart's entity texture at load, out of the wood's own block
 * sprites.</b>
 *
 * <p>This is upstream's cleverest idea and the reason carts can exist in a wood
 * nobody shipped art for: no cart entity texture is distributed at all. Each is
 * stitched here from {@code <wood>_planks}, {@code <wood>_log} and
 * {@code stripped_<wood>_log} as they sit on the block atlas, so a cart always
 * matches the wood it was built from, no Mojang art is redistributed, and a
 * resource pack that retextures oak planks retextures oak carts for free.
 *
 * <h2>What changed here</h2>
 * Upstream walked a hard-coded list of vanilla's twelve {@code WoodType}s and
 * kept a three-entry map of the woods whose log is called something else. This
 * walks {@link CartWood#all()} instead - vanilla's twelve plus one for every
 * wood any loaded mod adds - and asks the atlas whether each sprite is real
 * before using it.
 *
 * <p>That last part is the whole modded-wood story. A modded wood's plank
 * sprite is known for certain, because {@code ModdedMaterials} read it out of
 * that mod's own files; its <i>log</i> sprite is a guess from vanilla's naming
 * convention. So the guess is checked: a mod that names its logs the usual way
 * gets a cart with proper log shafts and a stripped-log hub, and one that does
 * not gets planks in those places rather than a purple cart. A plain cart is a
 * shrug; a purple one is a bug report.
 *
 * <p><b>Unverified API usage:</b> {@link #hasSprite} decides "is this sprite
 * real" by comparing the resolved sprite's own name against the id asked for,
 * on the assumption that {@link AtlasManager#get} answers an unknown id with
 * the missing-texture sprite rather than by throwing. That is vanilla's
 * long-standing behaviour but has not been confirmed on 26.1.2 with a mod
 * actually installed. If it throws instead, every modded cart is a crash at
 * resource reload and this is the first place to look.
 */
@Mixin(ModelManager.class)
public abstract class CartModelManagerMixin {

    @Inject(method = "apply", at = @At(value = "TAIL"))
    private void horsegenetics$assembleCartTextures(final ModelManager.ReloadState reloadState, final CallbackInfo ci) {
        final AtlasManager sprites = Minecraft.getInstance().getAtlasManager();
        final AssembledTextureFactory factory = new AssembledTextureFactory();

        // Shared, wood-independent pieces.
        final CartMaterial composterSide = new CartMaterial(Identifier.withDefaultNamespace("block/composter_side"), 16)
                .fill(16, 47, 44, 5, CartMaterial.R0, -2, 1)
                .fill(16, 54, 38, 5, CartMaterial.R0, -2, -6);
        final CartMaterial composterTop = new CartMaterial(Identifier.withDefaultNamespace("block/composter_top"), 16)
                .fill(18, 45, 10, 2, CartMaterial.R0, -2, 3)
                .fill(28, 45, 10, 2, CartMaterial.R0, 10, 3)
                .fill(18, 52, 8, 2, CartMaterial.R0, 0, -4)
                .fill(26, 52, 9, 2, CartMaterial.R0, 11, -4);
        final CartMaterial stone = new CartMaterial(Identifier.withDefaultNamespace("block/stone"), 16)
                .fill(62, 55, 2, 9);
        final CartMaterial dirt = new CartMaterial(Identifier.withDefaultNamespace("block/dirt"), 16)
                .fill(0, 45, 16, 17);

        for (final CartWood wood : CartWood.all()) {
            final Identifier planks = wood.planks();
            final Identifier log = wood.logOrPlanks(hasSprite(sprites, wood.log()));
            final Identifier stripped = wood.strippedOrPlanks(hasSprite(sprites, wood.strippedLog()));

            factory.add(new AssembledTexture(entityTexture(wood, "animal_cart"), 64, 64)
                    .add(new CartMaterial(planks, 16)
                            .fill(0, 0, 60, 38, CartMaterial.R0, 0, 2)
                            .fill(0, 28, 20, 33, CartMaterial.R90, 4, -2)
                            .fill(12, 30, 8, 31, CartMaterial.R270, 0, 4))
                    .add(new CartMaterial(stripped, 16)
                            .fill(54, 54, 10, 10, CartMaterial.R0, 0, 2))
                    .add(new CartMaterial(log, 16)
                            .fill(0, 21, 60, 4, CartMaterial.R90)
                            .fill(46, 60, 8, 4, CartMaterial.R90))
                    .add(stone));

            factory.add(new AssembledTexture(entityTexture(wood, "plow"), 64, 64)
                    .add(new CartMaterial(planks, 16)
                            .fill(0, 0, 64, 32, CartMaterial.R90)
                            .fill(0, 8, 42, 3, CartMaterial.R0, 0, 1)
                            .fill(0, 27, 34, 3, CartMaterial.R0, 0, 2))
                    .add(new CartMaterial(stripped, 16)
                            .fill(54, 54, 10, 10, CartMaterial.R0, 2, 0))
                    .add(new CartMaterial(log, 16)
                            .fill(0, 0, 54, 4, CartMaterial.R90)
                            .fill(46, 60, 8, 4, CartMaterial.R90))
                    .add(stone));

            factory.add(new AssembledTexture(entityTexture(wood, "wagon"), 64, 64)
                    .add(new CartMaterial(planks, 16)
                            .fill(0, 0, 64, 48))
                    .add(new CartMaterial(stripped, 16)
                            .fill(54, 53, 10, 11, CartMaterial.R0, 0, 2)
                            .fill(0, 32, 13, 19, CartMaterial.R0, 1, 0))
                    .add(new CartMaterial(log, 16)
                            .fill(0, 60, 54, 4, CartMaterial.R90))
                    .add(new CartMaterial(Identifier.withDefaultNamespace("block/stone"), 16)
                            .fill(62, 54, 2, 10)));

            factory.add(new AssembledTexture(entityTexture(wood, "seed_drill"), 64, 64)
                    .add(new CartMaterial(planks, 16)
                            .fill(0, 0, 64, 32, CartMaterial.R90)
                            .fill(0, 8, 64, 3, CartMaterial.R0, 0, 1)
                            .fill(0, 27, 34, 3, CartMaterial.R0, 0, 2))
                    .add(new CartMaterial(stripped, 16)
                            .fill(54, 54, 10, 10, CartMaterial.R0, 2, 0))
                    .add(new CartMaterial(log, 16)
                            .fill(0, 0, 64, 4, CartMaterial.R90)
                            .fill(46, 60, 8, 4, CartMaterial.R90))
                    .add(new CartMaterial(Identifier.withDefaultNamespace("block/stone"), 16)
                            .fill(62, 55, 2, 9)
                            .fill(0, 57, 8, 7, CartMaterial.R0)));

            factory.add(new AssembledTexture(entityTexture(wood, "reaper"), 64, 64)
                    .add(new CartMaterial(planks, 16)
                            .fill(0, 0, 64, 32, CartMaterial.R90)
                            .fill(0, 8, 46, 4, CartMaterial.R0, 0, 1)
                            .fill(0, 27, 34, 3, CartMaterial.R0, 0, 2))
                    .add(new CartMaterial(stripped, 16)
                            .fill(54, 54, 10, 10, CartMaterial.R0, 2, 0))
                    .add(new CartMaterial(log, 16)
                            .fill(0, 0, 64, 4, CartMaterial.R90)
                            .fill(32, 12, 8, 17, CartMaterial.R0)
                            .fill(46, 60, 8, 4, CartMaterial.R90))
                    .add(new CartMaterial(Identifier.withDefaultNamespace("block/stone"), 16)
                            .fill(62, 55, 2, 9)
                            .fill(0, 32, 64, 16)));

            factory.add(new AssembledTexture(entityTexture(wood, "supply_cart"), 64, 64)
                    .add(new CartMaterial(planks, 16)
                            .fill(0, 0, 60, 45, CartMaterial.R0, 0, 2)
                            .fill(0, 27, 60, 8, CartMaterial.R0, 0, 1))
                    .add(new CartMaterial(stripped, 16)
                            .fill(54, 54, 10, 10, CartMaterial.R0, 0, 2))
                    .add(new CartMaterial(log, 16)
                            .fill(0, 23, 54, 4, CartMaterial.R90)
                            .fill(46, 60, 8, 4, CartMaterial.R90))
                    .add(stone)
                    .add(composterSide)
                    .add(composterTop)
                    .add(dirt));
        }

        // The wagon roof, one per dye. Outside the wood loop: upstream had it
        // inside, so it rebuilt all sixteen twelve times over.
        for (final DyeColor color : DyeColor.values()) {
            factory.add(new AssembledTexture(HorseCarts.resLoc("textures/entity/wagon_roof_" + color.getName() + ".png"), 16, 16)
                    .add(new CartMaterial(Identifier.withDefaultNamespace("block/" + color.getName() + "_wool"), 16)
                            .fill(0, 0, 16, 16)));
        }

        factory.bake();
    }

    private static Identifier entityTexture(final CartWood wood, final String cart) {
        return HorseCarts.resLoc("textures/entity/" + wood.id() + "_" + cart + ".png");
    }

    /**
     * Whether the block atlas actually carries this sprite. See the class note
     * on why this is unverified.
     */
    private static boolean hasSprite(final AtlasManager sprites, final Identifier id) {
        try {
            return sprites.get(new SpriteId(TextureAtlas.LOCATION_BLOCKS, id)).contents().name().equals(id);
        } catch (final RuntimeException absent) {
            return false;
        }
    }
}
