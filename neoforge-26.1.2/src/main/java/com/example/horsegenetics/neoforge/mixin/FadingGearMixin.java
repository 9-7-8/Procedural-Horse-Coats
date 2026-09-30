package com.example.horsegenetics.neoforge.mixin;

import com.example.horsegenetics.neoforge.client.GeneticHorseRenderState;
import com.example.horsegenetics.neoforge.client.RiderFade;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * <b>The saddle and the barding fade with the horse.</b> Without this the
 * horse you are riding goes see-through and its tack does not, so what you are
 * looking down at is a saddle and a suit of armour floating in the shape of a
 * horse. That is worse than no fade at all, and it is the failure the whole
 * piece was specified to avoid.
 *
 * <h2>Why a mixin, when the rest of the fade is four overrides</h2>
 * The coat, the emissive glow and a braid are all drawn by this mod, so each
 * of them just takes the alpha off {@link GeneticHorseRenderState}. The gear
 * is not: it goes through vanilla's
 * {@code EquipmentLayerRenderer.renderLayers}, which closes <b>both</b> of the
 * doors a fade needs.
 *
 * <ul>
 *   <li><b>The render type is hard-coded</b> to
 *       {@code RenderTypes.armorCutoutNoCull(texture)}, and
 *       {@code RenderPipelines.ARMOR_CUTOUT_NO_CULL} carries no
 *       {@code BlendFunction} on its colour target at all. A cutout pipeline
 *       discards a fragment below an alpha of 0.1 and draws everything above
 *       it fully opaque, so tinting it half-transparent gives an ordinary
 *       solid saddle - and below 0.1, one that vanishes outright rather than
 *       fading.</li>
 *   <li><b>The colour is computed inside</b>, from the item's dye through
 *       {@code IClientItemExtensions.getArmorLayerTintColor}, and
 *       {@code SimpleEquipmentLayer} forwards no tint of its own. So there is
 *       nothing to pass in even where the pipeline would blend.</li>
 * </ul>
 *
 * <p>Hence two hooks rather than one: the type is swapped where it is built,
 * and the alpha is applied where the model is submitted. They are separate
 * because <b>a {@link RenderType} cannot be asked what texture it draws</b> -
 * that lives in a private {@code RenderSetup} with no accessor - so the
 * translucent replacement has to be made at the one point the
 * {@link Identifier} is still in hand.
 *
 * <h2>It guards on the render state, so it costs everything else nothing</h2>
 * <b>No flag, no thread-local, no static.</b> {@code renderLayers} is handed
 * the entity's render state, and this mod's is the only one carrying a
 * {@link GeneticHorseRenderState#fadeAlpha}. Anything that is not one of our
 * horses - and any of our horses that is not currently fading - goes straight
 * to the original call. A zombie in golden armour never reaches the second
 * line of either handler.
 *
 * <p>It also leaves other mods' work intact in the direction that matters: a
 * mod that swapped the armour model or texture has already done so by the time
 * either hook runs, since the model is an argument and the texture is the very
 * value being wrapped.
 *
 * <p><b>UNVERIFIED at runtime, twice over.</b> This is the mod's first use of
 * <b>MixinExtras</b> - every other mixin here is plain Sponge {@code @Inject}
 * - and it is bundled with NeoForge rather than declared by this project, so
 * check it resolves before trusting the annotations. And the descriptors are
 * long. That is the good kind of brittle: {@code defaultRequire} is 1, so a
 * signature that stops matching refuses to boot the game rather than quietly
 * leaving a floating saddle.
 */
// No @OnlyIn. NeoForge 26.1.2 logs an ERROR for a mod class carrying one -
// "the runtime member-stripping behaviour of this annotation is no longer
// present" - and it was never what kept this off a server anyway: the
// "client" list in horsegenetics.mixins.json is, and that is where this is
// registered. Seen in a real client boot on 2026-09-29.
@Mixin(EquipmentLayerRenderer.class)
public abstract class FadingGearMixin {

    private static final String RENDER_LAYERS =
            "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;"
                    + "Lnet/minecraft/resources/ResourceKey;"
                    + "Lnet/minecraft/client/model/Model;"
                    + "Ljava/lang/Object;"
                    + "Lnet/minecraft/world/item/ItemStack;"
                    + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/SubmitNodeCollector;"
                    + "I"
                    + "Lnet/minecraft/resources/Identifier;"
                    + "II)V";

    /**
     * Swap the cutout pipeline for a blending one, at the single point the
     * texture is still an {@link Identifier} rather than sealed inside a
     * {@link RenderType}.
     *
     * <p>{@code state} is captured with {@code argsOnly} because it is a
     * parameter of the wrapped method rather than a local, and it is the only
     * {@code Object}-typed one - {@code renderLayers} is generic in {@code S},
     * so erasure leaves exactly one candidate.
     */
    @WrapOperation(
            method = RENDER_LAYERS,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCull("
                            + "Lnet/minecraft/resources/Identifier;)"
                            + "Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private RenderType horsegenetics$blendGear(Identifier texture, Operation<RenderType> original,
                                               @Local(argsOnly = true) Object state) {
        if (state instanceof GeneticHorseRenderState genetic && genetic.isFading()) {
            // Already no-cull, like the type it replaces, which is what a
            // see-through animal wants anyway.
            return RenderTypes.entityTranslucent(texture);
        }
        return original.call(texture);
    }

    /**
     * And the alpha, at every submission in the method - the layer, the
     * enchantment glint and an armour trim. All three deliberately: a trim
     * left solid over a faded barding is the same bug in miniature.
     *
     * <p>Here {@code state} needs no capturing; it is the submission's own
     * second argument.
     */
    @WrapOperation(
            method = RENDER_LAYERS,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel("
                            + "Lnet/minecraft/client/model/Model;"
                            + "Ljava/lang/Object;"
                            + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                            + "Lnet/minecraft/client/renderer/rendertype/RenderType;"
                            + "III"
                            + "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;"
                            + "I"
                            + "Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"))
    private void horsegenetics$fadeGear(
            OrderedSubmitNodeCollector collector,
            Model<?> model,
            Object state,
            PoseStack poseStack,
            RenderType renderType,
            int lightCoords,
            int overlayCoords,
            int tintedColor,
            @Nullable TextureAtlasSprite sprite,
            int outlineColor,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay,
            Operation<Void> original) {
        int tint = state instanceof GeneticHorseRenderState genetic && genetic.isFading()
                ? RiderFade.fade(tintedColor, genetic.fadeAlpha)
                : tintedColor;
        original.call(collector, model, state, poseStack, renderType, lightCoords, overlayCoords,
                tint, sprite, outlineColor, crumblingOverlay);
    }
}
