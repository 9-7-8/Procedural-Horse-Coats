package com.example.horsegenetics.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;

/**
 * The cowboy's clothes: a plains villager's robe, and over it the same hat and
 * coat the <b>horseman</b> profession wears, so the two characters read as the
 * same trade.
 *
 * <h2>Why the clothes are layers and not one flattened texture</h2>
 * A villager is drawn in three passes - the bare skin
 * ({@code textures/entity/villager/villager.png}, which is the one that has a
 * <b>face</b> on it), then the biome type's robe, then the profession's
 * overlay. Baking those into a single cowboy texture would store the horseman
 * art twice, and the copy would go stale the first time
 * {@code profession/horseman.png} is repainted. So this reproduces vanilla's
 * order instead, and that file stays the single source for the art.
 *
 * <p>The cowboy cannot use vanilla's {@code VillagerProfessionLayer} to do it:
 * that layer reads {@code VillagerData} off the render state to decide which
 * profession and which biome variant to draw, and they are not a {@link
 * net.minecraft.world.entity.npc.villager.Villager} and has none. They are always
 * this one look, so both textures here are constants.
 *
 * <h2>The no-hat model</h2>
 * {@code horseman.png.mcmeta} declares {@code hat: full}, which in vanilla means
 * "this profession's hat replaces the type's". Vanilla honours that by drawing
 * the type pass on a villager mesh whose head has been cleared
 * ({@link net.minecraft.client.model.geom.ModelLayers#VILLAGER_NO_HAT}), and so
 * does this - otherwise the plains robe's hat band would poke out through the
 * brim of the cowboy hat.
 *
 * <p>The {@code order} arguments - 1 for the robe, 2 for the hat - are the ones
 * vanilla's profession layer submits at, so the passes sort over the body the
 * same way a villager's do.
 */
public class CowboyOverlayLayer extends RenderLayer<VillagerRenderState, VillagerModel> {

    /** The plains villager's brown robe. Drawn on the head-less mesh - see above. */
    private static final Identifier ROBE =
            Identifier.withDefaultNamespace("textures/entity/villager/type/plains.png");

    /** Shared with the horseman profession - see the class comment. */
    static final Identifier HAT = Identifier.fromNamespaceAndPath(
            com.example.horsegenetics.neoforge.HorseGenetics.MOD_ID,
            "textures/entity/villager/profession/horseman.png");

    private final VillagerModel noHatModel;

    public CowboyOverlayLayer(RenderLayerParent<VillagerRenderState, VillagerModel> renderer,
                              VillagerModel noHatModel) {
        super(renderer);
        this.noHatModel = noHatModel;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                       VillagerRenderState state, float yRot, float xRot) {
        if (state.isInvisible) {
            return;
        }
        renderColoredCutoutModel(noHatModel, ROBE, poseStack, collector, lightCoords, state, -1, 1);
        renderColoredCutoutModel(getParentModel(), HAT, poseStack, collector, lightCoords, state, -1, 2);
    }
}
