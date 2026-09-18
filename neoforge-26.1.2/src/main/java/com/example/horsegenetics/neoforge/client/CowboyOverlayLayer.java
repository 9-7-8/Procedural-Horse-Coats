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
 * coat the four <b>equestrian</b> professions wear, so they all read as the same
 * trade.
 *
 * <h2>Why the clothes are layers and not one flattened texture</h2>
 * A villager is drawn in three passes - the bare skin
 * ({@code textures/entity/villager/villager.png}, which is the one that has a
 * <b>face</b> on it), then the biome type's robe, then the profession's
 * overlay. Baking those into a single cowboy texture would store the hat art
 * twice and the copy would go stale, so this reproduces vanilla's order instead.
 *
 * <h2>He has his own copy of the hat, and it is generated</h2>
 * He used to read the horseman's profession texture directly, which was the
 * cheapest way to guarantee the two could not drift. That stopped being possible
 * when the horseman became four villagers in four hat colours: there is no file
 * at that path any more, and picking one of the four would hand him somebody
 * else's hat. So {@code tools/villagers/bake-profession-hats.ps1} writes his
 * copy - the same source art with the hat left its own brown - at the same time
 * it writes the four. The no-drift guarantee is now held by the bake rather than
 * by a shared path, which is the stronger of the two.
 *
 * <p>The cowboy cannot use vanilla's {@code VillagerProfessionLayer} to do it:
 * that layer reads {@code VillagerData} off the render state to decide which
 * profession and which biome variant to draw, and they are not a {@link
 * net.minecraft.world.entity.npc.villager.Villager} and has none. They are always
 * this one look, so both textures here are constants.
 *
 * <h2>The no-hat model</h2>
 * {@code cowboy.png.mcmeta} declares {@code hat: full}, which in vanilla means
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

    /** Generated beside the four equestrians' - see the class comment. */
    static final Identifier HAT = Identifier.fromNamespaceAndPath(
            com.example.horsegenetics.neoforge.HorseGenetics.MOD_ID,
            "textures/entity/villager/cowboy.png");

    /**
     * The arcane dealer's hat colour, multiplied over the ordinary cowboy hat
     * rather than drawn from its own texture - one tint against a second PNG to
     * keep in step with the first.
     *
     * <p>Light on purpose. The hat texture is already dark, and a multiply by a
     * saturated purple takes it to nearly black; this is picked bright enough
     * that the result reads as purple at riding distance. It is one constant, so
     * it is the thing to move if it looks wrong in the light.
     */
    private static final int ARCANE_HAT = 0xFFC9A0FF;

    private final VillagerModel noHatModel;

    /** See {@code CowboyRenderer.createRenderState} for why this is an instanceof. */
    private static boolean arcaneHat(VillagerRenderState state) {
        return state instanceof CowboyRenderState cowboyState && cowboyState.arcane;
    }

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
        renderColoredCutoutModel(getParentModel(), HAT, poseStack, collector, lightCoords, state,
            arcaneHat(state) ? ARCANE_HAT : -1, 2);
    }
}
