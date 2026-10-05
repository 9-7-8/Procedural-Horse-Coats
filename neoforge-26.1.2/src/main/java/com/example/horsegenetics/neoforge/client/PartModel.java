package com.example.horsegenetics.neoforge.client;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.example.horsegenetics.common.parts.PartNode;
import com.example.horsegenetics.common.parts.PartSheet;
import com.example.horsegenetics.common.parts.SaddleZone;

import java.util.List;

/**
 * <b>A baked attached-part mesh, ready to submit.</b> A tree of boxes with
 * <b>no animation of its own</b> - it is posed entirely by the pose stack the layer
 * hands it, because the bone it hangs from is already animated by the horse model.
 *
 * <h2>Why its state is a {@link Slice} and not the horse</h2>
 * {@code submitModel} is generic in the state it carries, and the state it
 * carries is the thing {@link #setupAnim} is called with <b>at draw time</b>. So
 * the type parameter is a free choice, and choosing something about the <i>part</i>
 * rather than the horse's render state is what makes a shared mesh safe.
 *
 * <p>That matters because <b>submits are deferred</b>. {@code submitModel} stores
 * a copy of the pose and a <i>reference</i> to this model, and the batch renderer
 * calls {@code setupAnim(state)} immediately before drawing each one. The obvious
 * way to vary a shared mesh per horse - set {@code visible} or {@code skipDraw} on
 * its parts and then submit - is therefore a trap: the second horse in a frame
 * overwrites the first horse's mesh before either is drawn, and the bug is one
 * horse wearing another's horn. Anything that must be applied by mutation is
 * applied in {@link #setupAnim}, where it runs per submit with that submit's own
 * state.
 *
 * <h2>The first thing that needed it: a two-tone horn</h2>
 * A submit carries one tint, and a two-tone horn needs a different colour on every
 * segment. So such a horn is submitted once per segment, each carrying a
 * {@link Slice} naming the one segment to draw, and {@link #setupAnim} sets
 * {@code skipDraw} on every other box. {@code skipDraw} rather than
 * {@code visible}: a segment is its predecessor's child, and {@code visible=false}
 * would hide everything above it too, where {@code skipDraw} skips the box and
 * still walks its children. A one-colour horn is submitted once with
 * {@link Slice#ALL}. <i>Unverified in game: that setupAnim runs per submit and
 * immediately before its draw is read from the 26.1.2 sources, not watched.</i>
 */
public final class PartModel extends Model<PartModel.Slice> {

    /**
     * What one submit draws of the mesh.
     *
     * <h2>The second thing that needed it: a count</h2>
     * An antler is baked with every tine its class can hold, and a horse shows only
     * {@code shown} of them - four whole and a fifth at 60% for {@code 4.6}. That is
     * a per-horse visibility and a per-horse scale on a <b>shared</b> mesh, which is
     * the exact trap the class note describes, so it rides in the slice and is
     * applied here per submit like the segment is.
     *
     * <h2>And a region mask</h2>
     * One mesh is drawn in up to four passes - the solid bone, see-through shafts on
     * a crystal antler, the bloom clumps in their own colour, the glowing points -
     * and each pass draws only the sheet regions it names. {@code skipDraw} again,
     * for the same reason as the segment: it skips a box and still walks its
     * children.
     *
     * <h2>And, for a row along the back, a hidden set and a per-element scale</h2>
     * A row of dorsal spines hides the spines under a saddle - a per-horse mask of
     * groups on a shared mesh, so the same trap again - and scales each spine about its
     * own root rather than the whole row on the pose stack, so the row keeps its length
     * ({@code PartKind#scalesPerElement}). Both land on each group's first box, the way
     * the count's shrink does.
     *
     * @param segment one box to draw, or {@code -1} for all of them
     * @param shown   how many countable groups show - see {@code AttachedPart#shown}
     * @param regions a {@code PartSheet} region mask: only boxes in these regions draw
     * @param hidden  groups not drawn whatever the count says - the saddle zone; {@code 0} for none
     * @param stretch each group's scale along its own axis; {@code 1} for a part scaled as a whole
     * @param girth   each group's scale across it; {@code 1} for a part scaled as a whole
     */
    public record Slice(int segment, float shown, int regions, int hidden, float stretch, float girth) {
        /** Every box of the solid body - a one-colour horn. */
        public static final Slice ALL = new Slice(-1, Float.POSITIVE_INFINITY, PartSheet.SOLID);

        /** A slice that hides no group and scales none. */
        public Slice(int segment, float shown, int regions) {
            this(segment, shown, regions, 0, 1f, 1f);
        }

        /** This slice with {@code hidden} groups skipped and each group scaled in its own frame. */
        public Slice perGroup(int hidden, float stretch, float girth) {
            return new Slice(segment, shown, regions, hidden, stretch, girth);
        }

        /** One segment of a two-tone part, every group, the solid body only. */
        public static Slice segment(int i, float shown) {
            return new Slice(i, shown, PartSheet.SOLID);
        }

        /** The whole of a part's {@code regions}, its first {@code shown} groups. */
        public static Slice regions(int regions, float shown) {
            return new Slice(-1, shown, regions);
        }
    }

    /**
     * The smallest a fractional group is drawn. A tine at 1% of its size is a
     * speck that flickers; under this it is simply not drawn yet.
     */
    private static final float MIN_GROUP_SCALE = 0.08f;

    private final List<ModelPart> segments;
    /** Per box: its {@code PartNode.group}. */
    private final int[] groups;
    /** Per box: does a group start here - is its parent outside the group? */
    private final boolean[] groupRoot;
    /** Per box: its sheet region. */
    private final int[] regions;
    /** Per box: how far it is from its root to the end of its own branch, 0 to 1. */
    private final float[] along;
    /** @see #saddleGroups() */
    private final int saddleGroups;

    /**
     * @param segments     the mesh's boxes in generator order, so segment {@code i} is
     *                     the {@code i}th {@code PartNode} - root to tip for a horn
     * @param nodes        the nodes they were baked from, for their groups and regions
     * @param saddleGroups the groups a saddle hides, or {@code 0}
     */
    PartModel(ModelPart root, List<ModelPart> segments, List<PartNode> nodes, int saddleGroups) {
        // The render type is a fallback only: every submit names its own, because
        // the same mesh is drawn opaque, translucent (a fading ridden horse) or
        // full-bright (a glowing horn) depending on the horse, not the mesh.
        super(root, RenderTypes::entityCutout);
        this.segments = List.copyOf(segments);
        int n = nodes.size();
        this.groups = new int[n];
        this.groupRoot = new boolean[n];
        this.regions = new int[n];
        this.along = alongOf(nodes);
        this.saddleGroups = saddleGroups;
        for (int i = 0; i < n; i++) {
            PartNode node = nodes.get(i);
            groups[i] = node.group();
            regions[i] = node.tex();
            groupRoot[i] = node.group() != PartNode.NO_GROUP
                    && (node.isRoot() || nodes.get(node.parent()).group() != node.group());
        }
    }

    /**
     * Where box {@code i} sits root ({@code 0}) to tip ({@code 1}) <b>of its own
     * branch</b> - its depth over its depth plus the longest run of boxes still
     * below it. On a single chain that is {@code i / (n - 1)}, which is what a
     * two-tone horn has always been coloured by; on a branched part - a four-horned
     * ram, whose second horn hangs off the first one's root - each horn runs root to
     * tip on its own, where the index would have coloured the second horn as if it
     * continued the first.
     */
    public float along(int i) {
        return along[i];
    }

    private static float[] alongOf(List<PartNode> nodes) {
        int n = nodes.size();
        int[] depth = new int[n];
        int[] height = new int[n];
        for (int i = 0; i < n; i++) {
            PartNode node = nodes.get(i);
            depth[i] = node.isRoot() ? 0 : depth[node.parent()] + 1;
        }
        // Parents precede children, so walking backwards finishes every child first.
        for (int i = n - 1; i >= 0; i--) {
            PartNode node = nodes.get(i);
            if (!node.isRoot()) {
                height[node.parent()] = Math.max(height[node.parent()], height[i] + 1);
            }
        }
        float[] out = new float[n];
        for (int i = 0; i < n; i++) {
            int span = depth[i] + height[i];
            out[i] = span == 0 ? 0f : (float) depth[i] / span;
        }
        return out;
    }

    /** How many segments there are to slice - the layer's loop bound. */
    public int segmentCount() {
        return segments.size();
    }

    /**
     * Every box the slice names drawn, the rest skipped; groups past the count
     * hidden and the fractional one shrunk. Runs per submit, at draw time, and
     * <b>sets every field on every box every time</b> - the previous submit of this
     * shared mesh may have been another horse's.
     */
    @Override
    public void setupAnim(Slice slice) {
        float shown = slice.shown();
        int whole = shown >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) shown;
        float fraction = shown - whole;
        for (int i = 0; i < segments.size(); i++) {
            ModelPart part = segments.get(i);
            part.skipDraw = (slice.segment() >= 0 && slice.segment() != i)
                    || !PartSheet.in(slice.regions(), regions[i]);
            float scale = 1f;
            float along = 1f;
            float across = 1f;
            boolean visible = true;
            if (groupRoot[i]) {
                int g = groups[i];
                if (g > whole || (g == whole && fraction < MIN_GROUP_SCALE)
                        || SaddleZone.hides(slice.hidden(), g)) {
                    visible = false;
                } else if (g == whole) {
                    scale = fraction;
                }
                along = slice.stretch();
                across = slice.girth();
            }
            // visible, not skipDraw: a hidden tine takes its own children - its
            // second segment and its bloom clump - with it.
            part.visible = visible;
            part.xScale = scale * across;
            part.yScale = scale * along;
            part.zScale = scale * across;
        }
    }

    /**
     * The groups whose root sits under the saddle - {@code 0} for a kind no saddle
     * hides. Worked out once, when the mesh is baked, from the vanilla saddle's range
     * in {@code AttachedPartLayer}; which horse it applies to is the layer's call.
     */
    public int saddleGroups() {
        return saddleGroups;
    }
}
