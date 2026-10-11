package com.example.horsegenetics.neoforge.client.gear;

/**
 * <b>Which way one suit of horse armour is drawn this frame.</b> The pure half
 * of {@link BardingLayer}, kept free of Minecraft types so a test can pin it -
 * this module's test classpath carries no game.
 *
 * <p>Edged is the default (owner, 2026-10-09: all horse armour is edged). Every
 * reason to step back to vanilla's flat shell is listed here and nowhere else,
 * and the step is always <b>for the whole item</b>: a suit with three lifted
 * layers and one flat one would have the flat layer buried under the others.
 */
public final class BardingRule {

    public enum Path {
        /** Each layer as a lifted, rimmed mesh of this mod's. */
        EDGED,
        /** Vanilla's {@code EquipmentLayerRenderer}, untouched. */
        VANILLA
    }

    private BardingRule() {
    }

    /**
     * @param edgesOn       the client's {@code gear.edges} switch
     * @param modelReplaced another mod's item swapped the armour model
     *                      ({@code getGenericArmorModel}); its texture is laid
     *                      out for that model, not for the horse this cuts from
     * @param trimmed       the stack carries an armour trim, which is a sprite
     *                      on an atlas and has no mesh here
     * @param layers        how many of the asset's layers are drawn at all
     * @param flatLayers    how many of those have no mesh: an unreadable
     *                      texture, or one whose edge is over the face limit
     */
    public static Path path(boolean edgesOn, boolean modelReplaced, boolean trimmed, int layers, int flatLayers) {
        if (!edgesOn || modelReplaced || trimmed || layers <= 0 || flatLayers > 0) {
            return Path.VANILLA;
        }
        return Path.EDGED;
    }
}
