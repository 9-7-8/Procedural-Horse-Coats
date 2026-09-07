package com.example.horsegenetics.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;

/**
 * Draws the cowboy's hat and coat over his villager body - the same overlay the
 * <b>horseman</b> profession wears, so the two characters read as the same
 * trade.
 *
 * <p>It is a layer rather than a baked-in composite because that keeps
 * {@code textures/entity/villager/profession/horseman.png} the <b>single
 * source</b> for that art. A villager profession can only ever be an overlay -
 * vanilla's {@code VillagerProfessionLayer} paints it over the villager-type
 * skin - so compositing a second, flattened copy for the cowboy would be one
 * picture stored twice, and the copy would go stale the first time the overlay
 * is repainted.
 *
 * <p>The cowboy cannot use {@code VillagerProfessionLayer} itself: that layer
 * reads {@code VillagerData} off the render state to decide which profession
 * and which biome variant to draw, and he is not a {@link
 * net.minecraft.world.entity.npc.villager.Villager} and has none. He is always
 * this one look, so the layer is a constant.
 *
 * <p>{@code order 2} matches what the profession layer submits its overlay at,
 * so the hat sorts over the body the same way a villager's does.
 */
public class CowboyHatLayer extends RenderLayer<VillagerRenderState, VillagerModel> {

    /** Shared with the horseman profession - see the class comment. */
    static final Identifier OVERLAY = Identifier.fromNamespaceAndPath(
            com.example.horsegenetics.neoforge.HorseGenetics.MOD_ID,
            "textures/entity/villager/profession/horseman.png");

    public CowboyHatLayer(RenderLayerParent<VillagerRenderState, VillagerModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                       VillagerRenderState state, float yRot, float xRot) {
        if (state.isInvisible) {
            return;
        }
        renderColoredCutoutModel(getParentModel(), OVERLAY, poseStack, collector, lightCoords, state, -1, 2);
    }
}
