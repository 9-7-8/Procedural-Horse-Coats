package com.example.horsegenetics.common.parts;

/**
 * The 64x64 part sheet is a 4x4 grid of 16x16 regions; a box samples one of them.
 * Region ids are shared by the baker ({@code tools/bake-part-sheet}), the generators
 * here and the client, so they live in one class.
 */
public final class PartSheet {
    public static final int SIZE = 64;
    public static final int REGION = 16;
    public static final int HORN = 0;      // ivory / keratin grain, opaque
    public static final int HORN_TIP = 1;  // darker, denser tip
    public static final int BONE = 2;      // antler beam, ridged
    public static final int BONE_TIP = 3;  // antler tine tip, paler
    public static final int CRYSTAL = 4;   // facet gradient, alpha ~0.55-0.85
    public static final int CRYSTAL_CORE = 5; // brighter core, alpha ~0.85

    private PartSheet() {
    }

    public static int u(int region) {
        return (region % (SIZE / REGION)) * REGION;
    }

    public static int v(int region) {
        return (region / (SIZE / REGION)) * REGION;
    }
}
