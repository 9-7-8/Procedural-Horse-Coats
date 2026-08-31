package com.example.horsegenetics.neoforge.client;

import net.minecraft.client.renderer.blockentity.state.EndPortalRenderState;
import net.minecraft.core.Direction;

/**
 * Render state for {@link HayPortalRenderer}. Adds the portal's plane axis so
 * the renderer can draw a thin centred slab instead of a full cube.
 */
public class HayPortalRenderState extends EndPortalRenderState {

    /** Axis the portal is thin along (from the block's {@code AXIS} state). */
    public Direction.Axis axis = Direction.Axis.Z;
}
