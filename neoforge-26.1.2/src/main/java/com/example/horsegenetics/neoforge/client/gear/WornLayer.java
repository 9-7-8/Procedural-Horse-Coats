package com.example.horsegenetics.neoforge.client.gear;

import com.example.horsegenetics.common.gear.WornPieces;
import com.example.horsegenetics.neoforge.ClientConfig;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.client.DebugPhotoShootClient;
import com.example.horsegenetics.neoforge.client.GeneticHorseRenderState;
import com.example.horsegenetics.neoforge.client.RiderFade;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HorseRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * <b>Draws every lifted row of {@link WornPieces}</b> - each soft thing a horse
 * wears that is a region of its own mesh: the rescuing braid in the mane and
 * the tail first, and whatever rows come after.
 *
 * <p>One layer for the whole table, so a new soft piece is a row, a baked mask
 * and one line in {@link #WORN} saying when a horse wears it and in what
 * colour; it is never a layer of its own (owner, 2026-10-09). Hard pieces are
 * box-built and are not drawn here.
 *
 * <h2>How a row is drawn</h2>
 * Its mask is a sheet that is flat white where the piece is and transparent
 * everywhere else, baked from the row's region ({@code HairMaskTool}). With
 * {@code gear.edges} on, {@link WornMeshes} cuts that mask into a mesh of its
 * own, the row's lift proud of the horse with a wall round the painted outline,
 * on the bones the region lies on - so a boot is on the leg's bone and swings
 * with the leg. The colour is the vertex colour, so one mask covers every dye.
 *
 * <p>Off, or when no mesh can be built, the row is the flat submit it always
 * was: the whole parent model through the mask, tinted. That path's argument
 * order was wrong until 2026-09-29 and is spelled out where it is called.
 *
 * <p>A horse wearing nothing costs one int read per row and returns.
 *
 * <p>Seen in stills only (a photo shoot); the Verification tab of
 * wiki/horse-gear.html has what is still to be looked at in play.
 */
public class WornLayer extends RenderLayer<HorseRenderState, HorseModel> {

    /**
     * When a horse wears a row, and in what colour: opaque ARGB, or 0 for "not
     * wearing it". A lifted row with no line here is never drawn, and the
     * constructor says so in the log.
     */
    private static final Map<String, ToIntFunction<GeneticHorseRenderState>> WORN = Map.of(
            WornPieces.BRAID_MANE.name(), state -> state.braidMane,
            WornPieces.BRAID_TAIL.name(), state -> state.braidTail,
            WornPieces.TEST_BOOT_FORE.name(), state -> DebugPhotoShootClient.testBoot,
            WornPieces.TEST_BOOT_HIND.name(), state -> DebugPhotoShootClient.testBoot);

    private record Row(WornPieces.Piece piece, Identifier mask, ToIntFunction<GeneticHorseRenderState> worn) {
    }

    private final List<Row> rows = new ArrayList<>();

    public WornLayer(RenderLayerParent<HorseRenderState, HorseModel> renderer) {
        super(renderer);
        // A debug row is not something a player can wear: outside a photo shoot
        // it is not in the layer at all.
        boolean debug = Boolean.getBoolean(DebugPhotoShootClient.PROPERTY);
        for (WornPieces.Piece piece : WornPieces.lifted()) {
            if (piece.debug() && !debug) {
                continue;
            }
            ToIntFunction<GeneticHorseRenderState> worn = WORN.get(piece.name());
            if (worn == null) {
                HorseGenetics.LOGGER.error(
                        "worn piece {} has no line in WornLayer.WORN, so no horse will ever be drawn wearing it",
                        piece.name());
                continue;
            }
            this.rows.add(new Row(piece, Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID,
                    "textures/entity/horse/" + piece.mask() + ".png"), worn));
        }
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
                       HorseRenderState state, float yRot, float xRot) {
        if (!(state instanceof GeneticHorseRenderState genetic) || state.isInvisible) {
            return;
        }
        for (Row row : this.rows) {
            int colour = row.worn().applyAsInt(genetic);
            if (colour == 0) {
                continue;   // not wearing it - see GeneticHorseRenderState
            }
            // What a horse wears fades with the horse: a plait left hanging at
            // full colour over a see-through horse would read as a bug rather
            // than as a braid. RiderFade.fade keeps the dye and moves only the
            // alpha.
            draw(poseStack, submitNodeCollector, lightCoords, state, row,
                    RiderFade.fade(colour, genetic.fadeAlpha));
        }
    }

    private void draw(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                      HorseRenderState state, Row row, int colour) {
        Identifier mask = row.mask();
        if (ClientConfig.gearEdges()) {
            WornMeshes.WornMesh mesh = WornMeshes.get(mask, this.getParentModel().root(), row.piece().lift());
            if (!mesh.flat()) {
                // The tint is the vertex colour here, so there is no argument
                // order to get wrong.
                WornMeshes.submit(mesh, poseStack, collector.order(1),
                        RenderTypes.entityTranslucent(mask), lightCoords, colour);
                return;
            }
        }
        // ARGUMENT ORDER, and it was wrong here until 2026-09-29. The ten-arg
        // submitModel is
        //   (model, state, pose, renderType, light, overlay,
        //    tintedColor, sprite, outlineColor, crumblingOverlay)
        // and this call had the last two ints the other way round: the braid's
        // dye was being passed as the OUTLINE colour and the tint was
        // state.outlineColor, which is 0 for any horse that is not glowing.
        // A tint of zero is transparent black, so no braid has ever been drawn
        // - and since EntityRenderState.appearsGlowing() is
        // "outlineColor != 0", a dyed braid was quietly asking for a glowing
        // outline instead. Both halves are silent: nothing logs, and an
        // undrawn overlay looks exactly like a horse that is not wearing one.
        collector.order(1).submitModel(
                this.getParentModel(),
                state,
                poseStack,
                RenderTypes.entityTranslucent(mask),
                lightCoords,
                OverlayTexture.NO_OVERLAY,
                colour,
                null,
                state.outlineColor,
                null);
    }
}
