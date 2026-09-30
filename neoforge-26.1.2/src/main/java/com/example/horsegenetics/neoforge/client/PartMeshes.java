package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.parts.PartGenerators;
import com.example.horsegenetics.common.parts.PartNode;
import com.example.horsegenetics.common.parts.PartShape;
import com.example.horsegenetics.common.parts.PartSheet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>Turns a {@link PartShape} into a baked mesh, once, and remembers it.</b>
 *
 * <h2>A layer definition can be baked at run time, and that was worth proving</h2>
 * Every other model in the mod is registered during
 * {@code EntityRenderersEvent.RegisterLayerDefinitions} and baked by the renderer
 * context, which would have forced attached parts into a fixed set declared at
 * startup. They are not so constrained: {@code LayerDefinition.bakeRoot()} is
 * public and pure - it is {@code mesh.getRoot().bake(xTexSize, yTexSize)} and
 * touches no registry, no event and no resource manager - so a mesh can be built
 * the first time a horse needs one. (Read out of the 26.1.2 sources, not assumed.)
 *
 * <p>That is what lets a horn's length be a real number rather than one of five
 * presets.
 *
 * <h2>No eviction, and no bake budget</h2>
 * Both are deliberate departures from how coats are handled, for the same reason:
 * there is nothing here to ration.
 * <ul>
 *   <li><b>No eviction.</b> The whole set of meshes this mod can bake is bounded by
 *       {@code kinds x styles x }{@link PartShape#SIZE_BUCKETS} - sixty-four for
 *       the horn, and {@code PartGeneratorTest} pins that. A cache that cannot
 *       exceed sixty-four entries of ninety quads each does not need a policy.</li>
 *   <li><b>No budget.</b> {@code GeneticCoatTextureFactory} rations bakes because a
 *       coat is a per-pixel loop over a 128x128 sheet and there are unboundedly
 *       many of them - walking toward a herd froze the owner's machine outright.
 *       A part mesh is fifteen boxes of allocation and there are at most sixty-four
 *       in the life of the process, so a budget would be machinery with nothing to
 *       spend.</li>
 * </ul>
 * If a later kind changes either of those facts - a crystal cluster whose count
 * belongs in the key, say - this is the class that grows the budget, and the
 * <i>reason</i> to grow it will be a measurement rather than a precaution.
 *
 * <p>Render-thread only, like every other model cache on this renderer. Nothing
 * synchronises, and nothing should: a second thread baking horse geometry would be
 * a bug well before it was a race.
 */
public final class PartMeshes {

    private static final Map<PartShape, PartModel> CACHE = new HashMap<>();

    private PartMeshes() {
    }

    /** The mesh for {@code shape}, baking it if this is the first horse to want one. */
    public static PartModel get(PartShape shape) {
        return CACHE.computeIfAbsent(shape, PartMeshes::bake);
    }

    /** How many meshes have been built. For the debug overlay and the tests. */
    public static int cached() {
        return CACHE.size();
    }

    /**
     * Build one {@code ModelPart} tree from a generated box list.
     *
     * <h2>The two coordinate conventions this reconciles</h2>
     * A {@link PartNode} hangs along <b>-y</b> from its own origin and sits at a
     * fraction {@code t} along its parent. A {@code PartPose} is an offset from the
     * parent's origin. So a child's offset is {@code -parentLength * t} on y, and
     * nothing else has to be computed: the parent's pose composes for us, exactly
     * as it does for the horse's ears on its head.
     *
     * <h2>Why there is no texture scale</h2>
     * A cube's UV rectangle is its own unwrap - {@code 2*(girth+girth)} texels wide
     * and {@code girth+length} tall - laid down at {@code texOffs}. The largest box
     * any part generates is about 10 by 4, which fits inside a 16-texel region with
     * room to spare, so every box can read its region at 1:1 and the sheet needs no
     * scaling trick of the kind {@code HdHorseModel} uses. The consequence is that a
     * box samples the <i>corner</i> of its region rather than all of it, which is
     * why {@code PartSheet} regions are a uniform grain rather than a picture.
     */
    private static PartModel bake(PartShape shape) {
        List<PartNode> nodes = PartGenerators.build(shape);
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition[] built = new PartDefinition[nodes.size()];

        for (int i = 0; i < nodes.size(); i++) {
            PartNode node = nodes.get(i);
            float half = node.girth() / 2f;
            CubeListBuilder box = CubeListBuilder.create()
                    .texOffs(PartSheet.u(node.tex()),
                            PartSheet.v(node.tex()))
                    .addBox(-half, -node.len(), -half,
                            node.girth(), node.len(), node.girth(), CubeDeformation.NONE);

            PartDefinition parent;
            float along;
            if (node.isRoot()) {
                parent = root;
                along = 0f;
            } else {
                parent = built[node.parent()];
                // Up the parent's own axis, which is -y. PartGeneratorTest
                // guarantees the parent was built before this node.
                along = -nodes.get(node.parent()).len() * node.t();
            }
            built[i] = parent.addOrReplaceChild("n" + i, box,
                    PartPose.offsetAndRotation(node.ox(), node.oy() + along, node.oz(),
                            node.rx(), node.ry(), node.rz()));
        }

        ModelPart baked = LayerDefinition
                .create(mesh, PartSheet.SIZE,
                        PartSheet.SIZE)
                .bakeRoot();
        // The same walk again, on the baked parts, so segment i is node i - the
        // order a two-tone part is coloured in, root to tip.
        ModelPart[] parts = new ModelPart[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            PartNode node = nodes.get(i);
            parts[i] = (node.isRoot() ? baked : parts[node.parent()]).getChild("n" + i);
        }
        return new PartModel(baked, List.of(parts));
    }
}
