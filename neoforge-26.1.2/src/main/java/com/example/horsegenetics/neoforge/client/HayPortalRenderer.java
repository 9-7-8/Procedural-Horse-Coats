package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.block.HayPortalBlock;
import com.example.horsegenetics.neoforge.block.HayPortalBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Renders the hay-bale portal with the mod's own animated texture,
 * {@code assets/horsegenetics/textures/block/hay_portal.png} - a vertical strip
 * of {@value #FRAME_COUNT} 64x64 frames (see the {@code .png.mcmeta}). We drive
 * the animation ourselves rather than via the vanilla tiled-strip system so it
 * can <b>ramp with the teleport dwell</b>: {@link HayPortalClientAnim} plays it
 * slow when idle and faster the longer the player stands in the portal.
 *
 * <p>Geometry: only the two faces on the portal's plane axis are drawn, as a
 * <b>half-block slab centred in the block</b> (0.25..0.75 along that axis) - so
 * it reads like a thin portal, not a solid cube. Each is drawn twice: an
 * <b>opaque black</b> pass ({@link RenderTypes#entitySolid}, writes depth) so
 * the sky / clouds / water can't show through, then the <b>animated swirl</b>
 * ({@link RenderTypes#entityTranslucentEmissive}, full-bright) on top.
 *
 * <p>Still extends {@code AbstractEndPortalRenderer} only for the plumbing
 * ({@code facesToShow} population + BER registration); none of the End-portal
 * shader is used.
 *
 * <h2>Changing the look later</h2>
 * <ul>
 *   <li><b>New frames:</b> replace {@code hay_portal.png} (keep it 64 wide,
 *       N*64 tall) and set {@link #FRAME_COUNT} to N. A GIF becomes this by
 *       exporting its frames to one tall PNG strip. The {@code .mcmeta} is not
 *       read here.</li>
 *   <li><b>Speed / ramp:</b> constants in {@link HayPortalClientAnim}.</li>
 *   <li><b>Slab thickness:</b> {@link #SLAB_MIN} / {@link #SLAB_MAX}.</li>
 *   <li><b>Back to the End-portal starfield:</b> restore
 *       {@code submitCube(state.facesToShow, RenderTypes.endPortal(), ...)} in
 *       {@link #submit} (see git history).</li>
 * </ul>
 */
public class HayPortalRenderer extends AbstractEndPortalRenderer<HayPortalBlockEntity, HayPortalRenderState> {

    static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("horsegenetics", "textures/block/hay_portal.png");
    /** Number of 64x64 frames stacked vertically in {@link #TEXTURE}. */
    static final int FRAME_COUNT = 36;

    /** Slab extent along the portal's thin axis (centred: 0.25..0.75 = half a block). */
    private static final float SLAB_MIN = 0.25f;
    private static final float SLAB_MAX = 0.75f;
    private static final int FULL_BRIGHT = 0x00F000F0;

    @Override
    public HayPortalRenderState createRenderState() {
        return new HayPortalRenderState();
    }

    @Override
    public void extractRenderState(HayPortalBlockEntity blockEntity, HayPortalRenderState state, float partialTicks,
                                   Vec3 cameraPosition, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.axis = blockEntity.getBlockState().getValue(HayPortalBlock.AXIS);
    }

    @Override
    public void submit(HayPortalRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       CameraRenderState camera) {
        if (state.facesToShow.isEmpty()) {
            return;
        }
        // AXIS is the horizontal axis the portal *plane* runs along (vanilla
        // nether-portal convention); the slab is thin along the OTHER axis, and
        // the faces we draw are the ones perpendicular to it - i.e. the ones
        // that face the player walking up to the frame.
        Direction.Axis thinAxis = switch (state.axis) {
            case X -> Direction.Axis.Z;
            case Z -> Direction.Axis.X;
            case Y -> Direction.Axis.Y;
        };
        Vector3f from = new Vector3f(0f, 0f, 0f);
        Vector3f to = new Vector3f(1f, 1f, 1f);
        switch (thinAxis) {
            case X -> { from.x = SLAB_MIN; to.x = SLAB_MAX; }
            case Y -> { from.y = SLAB_MIN; to.y = SLAB_MAX; }
            case Z -> { from.z = SLAB_MIN; to.z = SLAB_MAX; }
        }

        int frame = HayPortalClientAnim.currentFrame(FRAME_COUNT);
        float fv0 = (float) frame / FRAME_COUNT;
        float fv1 = (float) (frame + 1) / FRAME_COUNT;

        // opaque backing so nothing behind the portal (clouds / water / sky) shows through
        RenderType backing = RenderTypes.entitySolid(TEXTURE);
        submitNodeCollector.submitCustomGeometry(poseStack, backing, (pose, buffer) -> {
            for (Direction dir : state.facesToShow) {
                if (dir.getAxis() == thinAxis) {
                    emitFace(pose, buffer, dir, from, to, 0f, 1f, 0, 0, 0);
                }
            }
        });

        // the glowing animated swirl, one strip frame
        RenderType swirl = RenderTypes.entityTranslucentEmissive(TEXTURE, false);
        submitNodeCollector.submitCustomGeometry(poseStack, swirl, (pose, buffer) -> {
            for (Direction dir : state.facesToShow) {
                if (dir.getAxis() == thinAxis) {
                    emitFace(pose, buffer, dir, from, to, fv0, fv1, 255, 255, 255);
                }
            }
        });
    }

    /** One face of the box [{@code from}, {@code to}], wound CCW as seen from outside. */
    private static void emitFace(PoseStack.Pose pose, VertexConsumer buf, Direction dir, Vector3f from, Vector3f to,
                                 float fv0, float fv1, int r, int gr, int b) {
        Vector3f n = dir.step();
        Vector3f up = dir.getAxis().isVertical() ? new Vector3f(0f, 0f, 1f) : new Vector3f(0f, 1f, 0f);
        Vector3f right = new Vector3f(up).cross(n); // right = up x normal  =>  right x up == normal
        Vector3f half = new Vector3f((to.x - from.x) * 0.5f, (to.y - from.y) * 0.5f, (to.z - from.z) * 0.5f);
        Vector3f centre = new Vector3f((from.x + to.x) * 0.5f, (from.y + to.y) * 0.5f, (from.z + to.z) * 0.5f);
        float nExt = extent(n, half);
        centre.add(n.x * nExt, n.y * nExt, n.z * nExt);
        float rExt = extent(right, half);
        float uExt = extent(up, half);
        vert(pose, buf, centre, right, up, -rExt, -uExt, n, 0f, fv1, r, gr, b);
        vert(pose, buf, centre, right, up, rExt, -uExt, n, 1f, fv1, r, gr, b);
        vert(pose, buf, centre, right, up, rExt, uExt, n, 1f, fv0, r, gr, b);
        vert(pose, buf, centre, right, up, -rExt, uExt, n, 0f, fv0, r, gr, b);
    }

    private static float extent(Vector3f axisVec, Vector3f half) {
        return Math.abs(axisVec.x) * half.x + Math.abs(axisVec.y) * half.y + Math.abs(axisVec.z) * half.z;
    }

    private static void vert(PoseStack.Pose pose, VertexConsumer buf, Vector3f centre, Vector3f right, Vector3f up,
                             float a, float b, Vector3f n, float u, float v, int cr, int cg, int cb) {
        float x = centre.x + right.x * a + up.x * b;
        float y = centre.y + right.y * a + up.y * b;
        float z = centre.z + right.z * a + up.z * b;
        buf.addVertex(pose, x, y, z)
                .setColor(cr, cg, cb, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, n.x, n.y, n.z);
    }
}
