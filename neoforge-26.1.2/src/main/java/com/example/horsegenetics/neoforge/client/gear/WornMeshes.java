package com.example.horsegenetics.neoforge.client.gear;

import com.example.horsegenetics.common.gear.RimMesh;
import com.example.horsegenetics.common.gear.WornCache;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <b>Builds a worn piece's lifted, rimmed mesh from any baked model and any
 * texture, once, and draws it on the model's own bones.</b>
 *
 * <p>The soft builder of "worn gear is 3D" (owner, 2026-10-09). A piece that
 * wraps the horse used to be the horse's own mesh drawn a second time through a
 * mask, exactly coincident with the coat. Here the same mesh and the same mask
 * become geometry: every box the texture paints is handed to
 * {@link RimMesh#box}, which lifts the painted faces and walls the painted
 * outline, and the result is kept per bone so it rears, grazes and swings with
 * the bone it was cut from.
 *
 * <h2>Why the quads are drawn by hand</h2>
 * {@code ModelPart.Cube.polygons} is a public array of public records, and a
 * rim could be smuggled into a baked cube through it (read in the 26.1.2
 * sources). It is not done that way: a cube with foreign polygons is something
 * no other code expects, and a renderer that replaces {@code ModelPart}'s draw
 * loop reads a cube's box rather than its polygons. So the mesh is plain floats
 * and goes through {@code submitCustomGeometry}, with the pose stack walked
 * down the bone chain first - {@code LivingEntityRenderer.submit} calls
 * {@code setupAnim} before it runs the layers, so the chain holds this frame's
 * pose, which is what {@code AttachedPartLayer} already relies on.
 *
 * <p><b>UNVERIFIED in a running game on a moving horse</b>; the Verification
 * tab of wiki/model-parts.html has the checks. One known difference from a
 * {@code submitModel}: custom geometry carries no outline colour, so a lifted
 * piece adds nothing to a glowing horse's outline.
 *
 * <h2>The cache</h2>
 * {@link WornCache}, capped at {@link #CAP} and least-recently-used, keyed on
 * the texture, the model root it was built from (by identity - the adult and
 * the foal are different trees) and the lift. Separate from {@code PartMeshes}
 * on purpose: grown meshes are bounded by the genes, worn ones by however many
 * textures a pack brings. Emptied on a resource reload ({@code CoatAssetReload}).
 * Render thread only.
 */
public final class WornMeshes {

    /** How many worn meshes are held. A constant, not a config key: a mesh is a few kilobytes. */
    public static final int CAP = 64;

    /** One bone's share of a mesh: the chain of parts from the root down to it, and its quads in blocks. */
    public record Piece(ModelPart[] chain, float[] quads) {
    }

    /**
     * A built mesh. {@link #flat()} means there is none - the texture could not
     * be read, paints nothing on this model, or would need more than
     * {@link RimMesh#QUAD_LIMIT} quads - and the caller draws its piece the flat
     * way instead.
     */
    public record WornMesh(List<Piece> pieces, int quads) {
        public boolean flat() {
            return this.pieces.isEmpty();
        }
    }

    private record Key(Identifier texture, ModelPart root, float lift) {
    }

    private static final WornMesh FLAT = new WornMesh(List.of(), 0);

    private static final WornCache<Key, WornMesh> CACHE = new WornCache<>(CAP);

    private WornMeshes() {
    }

    /** The mesh {@code texture} paints on the model under {@code root}, standing {@code lift} model units off it. */
    public static WornMesh get(Identifier texture, ModelPart root, float lift) {
        return CACHE.get(new Key(texture, root, lift), WornMeshes::build);
    }

    /** Throw every mesh away: the textures they were cut from may have changed. */
    public static void clear() {
        CACHE.clear();
    }

    public static int cached() {
        return CACHE.size();
    }

    private static WornMesh build(Key key) {
        RimMesh.Texels texels = read(key.texture());
        if (texels == null) {
            return FLAT;
        }
        Map<String, List<float[]>> byBone = new LinkedHashMap<>();
        int[] quads = {0};
        // visit() is the one public walk over a baked tree that hands out its
        // cubes. The pose it computes is thrown away: only the path is wanted.
        key.root().visit(new PoseStack(), (pose, path, index, cube) -> {
            RimMesh.Built built = RimMesh.box(faces(cube), texels, key.lift());
            if (built.count() > 0) {
                byBone.computeIfAbsent(path, p -> new ArrayList<>()).add(built.quads());
                quads[0] += built.count();
            }
        });
        if (quads[0] > RimMesh.QUAD_LIMIT) {
            HorseGenetics.LOGGER.info("worn piece {} would need {} faces for its edge (limit {}), so it is drawn flat",
                    key.texture(), quads[0], RimMesh.QUAD_LIMIT);
            return FLAT;
        }
        List<Piece> pieces = new ArrayList<>();
        byBone.forEach((path, parts) -> pieces.add(new Piece(chain(key.root(), path), blocks(parts))));
        return new WornMesh(List.copyOf(pieces), quads[0]);
    }

    private static List<RimMesh.Face> faces(ModelPart.Cube cube) {
        List<RimMesh.Face> faces = new ArrayList<>();
        for (ModelPart.Polygon polygon : cube.polygons) {
            ModelPart.Vertex[] vertices = polygon.vertices();
            if (vertices.length != 4) {
                continue;
            }
            float[] corners = new float[12];
            float[] uv = new float[8];
            for (int i = 0; i < 4; i++) {
                corners[i * 3] = vertices[i].x();
                corners[i * 3 + 1] = vertices[i].y();
                corners[i * 3 + 2] = vertices[i].z();
                uv[i * 2] = vertices[i].u();
                uv[i * 2 + 1] = vertices[i].v();
            }
            faces.add(new RimMesh.Face(corners, uv,
                    polygon.normal().x(), polygon.normal().y(), polygon.normal().z()));
        }
        return faces;
    }

    /** A visit path is "" for the root and "/a/b" for a part two levels down. */
    private static ModelPart[] chain(ModelPart root, String path) {
        List<ModelPart> chain = new ArrayList<>();
        chain.add(root);
        ModelPart at = root;
        for (String name : path.split("/")) {
            if (!name.isEmpty()) {
                at = at.getChild(name);
                chain.add(at);
            }
        }
        return chain.toArray(new ModelPart[0]);
    }

    /** One array for the bone, with positions taken from model units to blocks as a cube's own draw does. */
    private static float[] blocks(List<float[]> parts) {
        int length = 0;
        for (float[] part : parts) {
            length += part.length;
        }
        float[] all = new float[length];
        int at = 0;
        for (float[] part : parts) {
            System.arraycopy(part, 0, all, at, part.length);
            at += part.length;
        }
        for (int v = 0; v < all.length; v += RimMesh.FLOATS_PER_VERTEX) {
            all[v] /= 16f;
            all[v + 1] /= 16f;
            all[v + 2] /= 16f;
        }
        return all;
    }

    /** The texture's painted texels, or null if it is not a readable pack resource. */
    private static RimMesh.Texels read(Identifier texture) {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
        if (resource.isEmpty()) {
            HorseGenetics.LOGGER.info("worn piece {} is not a pack texture, so it is drawn flat", texture);
            return null;
        }
        try (InputStream in = resource.get().open();
             NativeImage image = NativeImage.read(in)) {
            int width = image.getWidth();
            int height = image.getHeight();
            boolean[] painted = new boolean[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    painted[y * width + x] = (image.getPixel(x, y) >>> 24) >= RimMesh.PAINTED_ALPHA;
                }
            }
            return new RimMesh.Texels() {
                @Override
                public int width() {
                    return width;
                }

                @Override
                public int height() {
                    return height;
                }

                @Override
                public boolean painted(int u, int v) {
                    return u >= 0 && v >= 0 && u < width && v < height && painted[v * width + u];
                }
            };
        } catch (IOException | RuntimeException e) {
            HorseGenetics.LOGGER.warn("could not read worn piece {}, so it is drawn flat", texture, e);
            return null;
        }
    }

    /**
     * Draw {@code mesh} on the bones it was built from, in {@code colour}.
     *
     * <p>The pose stack must be where a {@code submitModel} of the same model
     * would start: this applies the root and every part below it itself. A bone
     * that is hidden this frame hides its share, as it would in the model.
     */
    public static void submit(WornMesh mesh, PoseStack poseStack, OrderedSubmitNodeCollector collector,
                              RenderType renderType, int lightCoords, int colour) {
        for (Piece piece : mesh.pieces()) {
            if (hidden(piece.chain())) {
                continue;
            }
            poseStack.pushPose();
            for (ModelPart part : piece.chain()) {
                part.translateAndRotate(poseStack);
            }
            float[] quads = piece.quads();
            collector.submitCustomGeometry(poseStack, renderType,
                    (pose, buffer) -> emit(pose, buffer, quads, lightCoords, colour));
            poseStack.popPose();
        }
    }

    private static boolean hidden(ModelPart[] chain) {
        for (ModelPart part : chain) {
            if (!part.visible) {
                return true;
            }
        }
        return chain[chain.length - 1].skipDraw;
    }

    /** The same per-vertex call a baked cube makes, in the same argument order. */
    private static void emit(PoseStack.Pose pose, VertexConsumer buffer, float[] quads, int lightCoords, int colour) {
        Matrix4f matrix = pose.pose();
        Vector3f position = new Vector3f();
        Vector3f normal = new Vector3f();
        for (int q = 0; q < quads.length; q += RimMesh.FLOATS_PER_QUAD) {
            pose.transformNormal(quads[q + 5], quads[q + 6], quads[q + 7], normal);
            for (int v = q; v < q + RimMesh.FLOATS_PER_QUAD; v += RimMesh.FLOATS_PER_VERTEX) {
                matrix.transformPosition(quads[v], quads[v + 1], quads[v + 2], position);
                buffer.addVertex(position.x(), position.y(), position.z(), colour, quads[v + 3], quads[v + 4],
                        OverlayTexture.NO_OVERLAY, lightCoords, normal.x(), normal.y(), normal.z());
            }
        }
    }
}
