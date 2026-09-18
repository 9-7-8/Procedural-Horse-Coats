package com.example.horsegenetics.neoforge.client;

import net.minecraft.client.renderer.entity.state.VillagerRenderState;

/**
 * Vanilla's villager render state plus the one thing this mod's cowboy needs on
 * the client: whether he is an arcane dealer, so {@link CowboyOverlayLayer} can
 * put a purple hat on him.
 *
 * <p>Filled in {@code CowboyRenderer.extractRenderState} off
 * {@code Cowboy.isArcane()}, which is synched for exactly this reason - every
 * other field on that entity is server state and stays there.
 */
public class CowboyRenderState extends VillagerRenderState {

    /** False on a state nobody extracted, which renders the ordinary hat. */
    public boolean arcane = false;
}
