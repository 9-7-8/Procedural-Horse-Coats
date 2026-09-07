package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.coat.CoatData;
import com.example.horsegenetics.common.horse.TransferDeed;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HorseRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Draws a <b>signed transfer paper as the horse it names</b> - the mod's own HD
 * horse mesh, in that horse's own generated coat, shrunk into an item slot.
 *
 * <h2>Why a horse and not a picture of a paper</h2>
 * The paper is a claim on a specific animal that the buyer has to walk out and
 * collect. A cowboy's stall is six of them and they are identical except for a
 * name in the tooltip, so matching the one in your hand to an animal in a field
 * meant reading a name off a paper and then reading names off horses. Drawing
 * the horse instead makes it one glance: the dun in the slot is the dun in the
 * paddock. The same model is what the item is in the merchant screen, in the
 * inventory, in the hand and on the ground, because a paper that turned back
 * into a paper the moment you bought it would be the same problem one step
 * later.
 *
 * <h2>Where the coat comes from</h2>
 * {@link TransferDeed} snapshots the horse's genetic and epigenome codes when
 * the paper is signed, so this needs neither the horse nor the server: the
 * argument extracted from the stack is simply the {@link Identifier} of the
 * generated coat texture, and {@link GeneticCoatTextureFactory} caches those by
 * coat key across every horse and paper in the world. A paper for a horse that
 * has been unloaded, sold on twice, or shot still draws the animal it was
 * written for.
 *
 * <p>Always the <b>adult</b> sheet: the cowboy does not sell foals, and a paper
 * is a picture of the horse you are buying, not of how old it was.
 *
 * <h2>Fitting a horse in a 16px slot</h2>
 * The three constants below are eyeball numbers and want looking at in game -
 * see {@code wiki/verification.html}. The transform is vanilla's
 * entity-into-the-world sequence ({@code scale(-1, -1, 1)} then the
 * {@link EntityModel#MODEL_Y_OFFSET} lift) with a fit scale in front of it, so
 * the model stands on the item cube's floor the way an entity stands on the
 * ground, and is then raised to sit in the middle of the slot.
 */
public class TransferDeedRenderer implements SpecialModelRenderer<Identifier> {

    /** The id this renderer answers to in an item model's {@code "model"} block. */
    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "transfer_deed_horse");

    /**
     * Shrink applied before the entity transform. A horse is about 2.8 blocks
     * nose to tail, so this is roughly "fill four fifths of the slot".
     */
    private static final float FIT = 0.30F;

    /** Lift off the item cube's floor, so it is centred in the slot rather than standing on the bottom of it. */
    private static final float LIFT = 0.22F;

    /** The mesh sits slightly nose-forward of its own origin; nudge it back to centre. */
    private static final float CENTRE_Z = 0.12F;

    private final HorseModel model;

    /**
     * One reusable state. Item models are only ever built on the render thread,
     * and every paper poses the horse identically - standing square, not
     * walking, not eating - so there is nothing per-stack to keep.
     */
    private final HorseRenderState pose = new HorseRenderState();

    public TransferDeedRenderer(HorseModel model) {
        this.model = model;
    }

    /**
     * The coat texture for the horse this paper names, or {@code null} for a
     * stack carrying no deed - which is what an unsigned paper, or one hacked
     * in with {@code /give}, looks like. A null argument draws nothing, so the
     * base model's own sprite is what shows.
     */
    @Override
    public @Nullable Identifier extractArgument(ItemStack stack) {
        TransferDeed deed = stack.get(ModDataComponents.HORSE_DEED.get());
        if (deed == null) {
            return null;
        }
        return GeneticCoatTextureFactory.getOrCreate(new CoatData(deed.genome()), false);
    }

    @Override
    public void submit(@Nullable Identifier coat, PoseStack poseStack, SubmitNodeCollector collector,
                       int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        if (coat == null) {
            return;
        }
        poseStack.pushPose();
        standInTheSlot(poseStack);
        collector.submitModel(model, pose, poseStack, coat, lightCoords, overlayCoords, outlineColor, null);
        poseStack.popPose();
    }

    /**
     * Bounds for the item pipeline, which needs to know how much of the slot
     * this fills. Measured through the same transform the drawing uses, because
     * the two disagreeing is how an item ends up culled at the edge of the
     * screen while still visibly on it.
     */
    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        PoseStack poseStack = new PoseStack();
        standInTheSlot(poseStack);
        model.setupAnim(pose);
        model.root().getExtentsForGui(poseStack, output);
    }

    private static void standInTheSlot(PoseStack poseStack) {
        poseStack.translate(0.5F, LIFT, 0.5F + CENTRE_Z);
        poseStack.scale(FIT, FIT, FIT);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, EntityModel.MODEL_Y_OFFSET, 0.0F);
    }

    /**
     * The data-driven half: what an item model file names to get this renderer.
     * It takes no arguments - the horse is entirely a property of the stack, not
     * of the model - so the codec is a constant.
     */
    public record Unbaked() implements SpecialModelRenderer.Unbaked<Identifier> {

        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public TransferDeedRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new TransferDeedRenderer(new HdHorseModel(context.entityModelSet().bakeLayer(ClientSetup.HD_HORSE)));
        }
    }
}
