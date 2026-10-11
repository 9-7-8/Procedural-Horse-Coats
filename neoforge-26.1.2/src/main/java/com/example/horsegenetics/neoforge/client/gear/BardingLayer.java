package com.example.horsegenetics.neoforge.client.gear;

import com.example.horsegenetics.common.gear.WornRule;
import com.example.horsegenetics.neoforge.ClientConfig;
import com.example.horsegenetics.neoforge.client.GeneticHorseRenderState;
import com.example.horsegenetics.neoforge.client.RiderFade;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.animal.equine.AbstractEquineModel;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.HorseRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>Horse armour with an edge.</b> Piece 2 of "worn gear is 3D" (owner,
 * 2026-10-09): every suit of horse armour on a horse this mod draws - vanilla's,
 * another mod's, this mod's generated metals - stands {@link #LIFT} off the coat
 * with a wall where the painted plate ends, instead of being vanilla's shell a
 * tenth of a unit proud with nothing at its edge.
 *
 * <p>This is a looks-only exception to "vanilla mechanics stay intact"
 * (wiki/decisions.html). Nothing about the armour's behaviour is touched, and
 * the client's {@code gear.edges} switch hands the drawing back to vanilla.
 *
 * <h2>It reads the armour exactly as vanilla does</h2>
 * The layers come from the item's equipment asset, each texture from
 * {@code Layer.getTextureLocation} through {@code ClientHooks.getArmorTexture},
 * each colour from {@code IClientItemExtensions.getArmorLayerTintColor} with the
 * item's dye as the fallback, and a colour of zero skips the layer - the body of
 * {@code EquipmentLayerRenderer.renderLayers}, read in the 26.1.2 sources. So
 * {@code TackClientExtensions}' three-colour leather and a generated metal's
 * {@code color_when_undyed} arrive here unchanged. What differs is the last
 * step: each layer is a {@link WornMeshes} mesh rather than a {@code submitModel}.
 *
 * <h2>The mesh is cut from the bare horse, not from vanilla's armour model</h2>
 * Vanilla's {@code HORSE_ARMOR} layer is the horse's body mesh inflated by 0.1.
 * A rim built on that would stop a tenth of a unit short of the coat, and its
 * lift would have to be 0.15, under the floor {@link WornRule} enforces. So this
 * bakes the same mesh with no inflation ({@link #skinModel}) and lifts it the
 * whole quarter unit; the walls then reach the skin. Inflation moves no UV, so
 * every armour texture fits it as it fits vanilla's.
 *
 * <p>That model belongs to this layer alone, so it is posed here
 * ({@code setupAnim}) before its bones are walked. A {@code submitModel} poses
 * its model later, at draw time; custom geometry is transformed at submit.
 *
 * <h2>When it steps aside</h2>
 * {@link BardingRule}. In every such case the vanilla layer this replaced is
 * called as it always was - including while a horse fades, where
 * {@code FadingGearMixin} still does its work on that path. On the edged path
 * the fade is done here, as {@code HarnessLayer} does it: a blending render type
 * and the alpha on the tint.
 *
 * <p><b>UNVERIFIED in a running game</b> beyond the photo shoot recorded on
 * wiki/horse-gear.html's Verification tab. Known differences from vanilla's
 * draw: custom geometry carries no outline colour, so edged armour adds nothing
 * to a glowing horse's outline; and a mod or pack that replaces the
 * {@code HORSE_ARMOR} layer definition itself is not followed (one that swaps
 * the model per item is, by stepping aside).
 */
public class BardingLayer extends RenderLayer<HorseRenderState, HorseModel> {

    /** How far armour stands off the coat: the worn floor, not vanilla's 0.1 (owner). */
    static final float LIFT = WornRule.lift(WornRule.FLOOR);

    private static final EquipmentClientInfo.LayerType TYPE = EquipmentClientInfo.LayerType.HORSE_BODY;

    /** Vanilla's first submit order for this layer; kept, so the saddle still sorts after it. */
    private static final int ORDER = 2;

    private record Drawn(WornMeshes.WornMesh mesh, Identifier texture, int colour) {
    }

    private final SimpleEquipmentLayer<HorseRenderState, HorseModel, HorseModel> vanilla;
    private final HorseModel vanillaModel;
    private final HorseModel skin;
    private final EquipmentAssetManager assets;

    /** {@code getTextureLocation} builds an Identifier each call; vanilla memoises it too. */
    private final Map<EquipmentClientInfo.Layer, Identifier> textures = new HashMap<>();

    /** Reused every frame. Render thread only. */
    private final List<Drawn> drawn = new ArrayList<>();

    public BardingLayer(RenderLayerParent<HorseRenderState, HorseModel> renderer,
                        EntityRendererProvider.Context context) {
        super(renderer);
        this.vanillaModel = new HorseModel(context.bakeLayer(ModelLayers.HORSE_ARMOR));
        this.vanilla = new SimpleEquipmentLayer<>(renderer, context.getEquipmentRenderer(), TYPE,
                state -> state.bodyArmorItem, this.vanillaModel, null, ORDER);
        this.skin = skinModel();
        this.assets = context.getEquipmentAssets();
    }

    /**
     * {@code LayerDefinitions}' own line for {@code HORSE_ARMOR} with the 0.1
     * taken out: the body mesh, 64 by 64, at the living horse's 1.1.
     */
    private static HorseModel skinModel() {
        return new HorseModel(LayerDefinition
                .create(AbstractEquineModel.createBodyMesh(CubeDeformation.NONE), 64, 64)
                .apply(MeshTransformer.scaling(1.1F))
                .bakeRoot());
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                       HorseRenderState state, float yRot, float xRot) {
        if (!this.edged(poseStack, collector, lightCoords, state)) {
            this.vanilla.submit(poseStack, collector, lightCoords, state, yRot, xRot);
        }
    }

    /** Draw the armour edged and answer true, or draw nothing and answer false. */
    private boolean edged(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                          HorseRenderState state) {
        // A foal wears no armour in vanilla (the layer has no baby model), and
        // the vanilla layer is what says so.
        if (!ClientConfig.gearEdges() || state.isBaby) {
            return false;
        }
        ItemStack stack = state.bodyArmorItem;
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null || equippable.assetId().isEmpty()) {
            return false;
        }
        IClientItemExtensions extensions = IClientItemExtensions.of(stack);
        Model<?> offered = extensions.getGenericArmorModel(stack, TYPE, this.vanillaModel);
        List<EquipmentClientInfo.Layer> layers = this.assets.get(equippable.assetId().get()).getLayers(TYPE);
        int dye = extensions.getDefaultDyeColor(stack);

        this.drawn.clear();
        int flat = 0;
        int index = 0;
        for (EquipmentClientInfo.Layer layer : layers) {
            int colour = extensions.getArmorLayerTintColor(stack, layer, index++, dye);
            if (colour == 0) {
                continue;
            }
            Identifier texture = ClientHooks.getArmorTexture(stack, TYPE, layer,
                    this.textures.computeIfAbsent(layer, l -> l.getTextureLocation(TYPE)));
            WornMeshes.WornMesh mesh = WornMeshes.get(texture, this.skin.root(), LIFT);
            if (mesh.flat()) {
                flat++;
            }
            this.drawn.add(new Drawn(mesh, texture, colour));
        }
        if (BardingRule.path(true, offered != this.vanillaModel, stack.has(DataComponents.TRIM),
                this.drawn.size(), flat) != BardingRule.Path.EDGED) {
            return false;
        }

        this.skin.setupAnim(state);
        boolean fading = state instanceof GeneticHorseRenderState genetic && genetic.isFading();
        float alpha = fading ? ((GeneticHorseRenderState) state).fadeAlpha : 1.0F;
        boolean foil = stack.hasFoil();
        int order = ORDER;
        for (Drawn piece : this.drawn) {
            // The cutout armour pipeline has no blend function, so a fading
            // suit needs the translucent one - FadingGearMixin's reasoning.
            RenderType type = fading
                    ? RenderTypes.entityTranslucent(piece.texture())
                    : RenderTypes.armorCutoutNoCull(piece.texture());
            int colour = fading ? RiderFade.fade(piece.colour(), alpha) : piece.colour();
            WornMeshes.submit(piece.mesh(), poseStack, collector.order(order++), type, lightCoords, colour);
            if (foil) {
                // The glint pipeline draws where depth is EQUAL, so the same
                // quads a second time put it on the faces and the walls alike.
                // Once only, on the first layer drawn, as vanilla does.
                WornMeshes.submit(piece.mesh(), poseStack, collector.order(order++),
                        RenderTypes.armorEntityGlint(), lightCoords, colour);
                foil = false;
            }
        }
        return true;
    }
}
