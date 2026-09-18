package com.example.horsegenetics.neoforge.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

/**
 * The three colours of a piece of tack: the <b>seat</b> leather, the
 * <b>bridle</b> leather, and the <b>metal</b> hardware.
 *
 * <p><b>Why this is not {@code dyed_color}.</b> Vanilla holds one dye value per
 * stack and reads it <i>once</i>, outside the layer loop, so however many layers
 * an equipment asset declares they all take the same colour. Three zones need
 * three values, so they live here and reach the renderer through
 * {@code IClientItemExtensions.getArmorLayerTintColor}, which is handed the
 * layer index precisely for this case.
 *
 * <p><b>The indices are the layer order in the equipment asset</b>, and the two
 * must not drift: {@code assets/minecraft/equipment/saddle.json} lists seat,
 * then bridle, then metal, and {@link #forLayer(int)} answers in that order. The
 * textures are disjoint - the bake asserts that no texel appears in two of them
 * - so a wrong order would not overlap anything, it would just paint the bridle
 * with the seat's colour, which is the kind of bug that looks like a design
 * choice.
 *
 * <p>Absent means undyed. The item carries no component until somebody dyes it,
 * and the renderer then falls back to each layer's own
 * {@code color_when_undyed}, which is what keeps a plain saddle - on a horse, a
 * donkey, a mule, a skeleton or a zombie horse - pixel-identical to vanilla's.
 */
public record SaddleTint(int seat, int bridle, int metal)
        implements net.minecraft.world.item.component.TooltipProvider {

    /**
     * What the saddle says when you hover it. Vanilla's own
     * {@code DyedItemColor} is the precedent for a component describing itself
     * this way, and it is the only place a player can read three colours off an
     * item that can only <i>show</i> one.
     */
    @Override
    public void addToTooltip(net.minecraft.world.item.Item.TooltipContext context,
                             java.util.function.Consumer<net.minecraft.network.chat.Component> lines,
                             net.minecraft.world.item.TooltipFlag flag,
                             net.minecraft.core.component.DataComponentGetter components) {
        describeInto(lines, SADDLE_ZONES);
    }

    /**
     * The same three colours under whatever the piece calls its zones. The
     * record is deliberately ignorant of which item it is on - it is three ints
     * and a layer order, and that is what lets one component serve both the
     * saddle and the armour.
     */
    public void describeInto(java.util.function.Consumer<net.minecraft.network.chat.Component> lines,
                             String[] zones) {
        lines.accept(line(zones[LAYER_SEAT], seat, false));
        lines.accept(line(zones[LAYER_BRIDLE], bridle, false));
        lines.accept(line(zones[LAYER_METAL], metal, true));
    }

    /** A saddle's three zones, in layer order. */
    public static final String[] SADDLE_ZONES = { "Seat", "Bridle", "Fittings" };

    /**
     * Leather horse armour's three zones, in the same layer order. The words
     * differ because the parts do: there is no seat on a caparison and no
     * bridle on a body piece, so the first two rows would be lying.
     */
    public static final String[] ARMOUR_ZONES = { "Blanket", "Trim", "Fittings" };

    public static String[] zoneNames(net.minecraft.world.item.ItemStack stack) {
        return stack.is(net.minecraft.world.item.Items.LEATHER_HORSE_ARMOR) ? ARMOUR_ZONES : SADDLE_ZONES;
    }

    private static net.minecraft.network.chat.Component line(String zone, int colour, boolean isMetal) {
        return net.minecraft.network.chat.Component
                .literal(zone + ": " + name(colour, isMetal))
                .withStyle(net.minecraft.ChatFormatting.GRAY);
    }

    /**
     * A dye's name where the colour is one, a material's where it is not, and a
     * hex code as a last resort. &ldquo;Seat: red&rdquo; is worth far more than
     * &ldquo;Seat: #B02E26&rdquo; to somebody deciding what to dye next.
     *
     * <p><b>The zone's own table is consulted first, and that matters.</b> The
     * two namespaces are not disjoint - a fitting colour can sit close to, or
     * land exactly on, one of the sixteen dye values. Asking the dyes first
     * would let a redstone fitting report itself as &ldquo;red&rdquo;, naming a
     * material the bench will not accept in that slot. A leather zone can only
     * hold a dye and a fitting can only hold a material, so each asks its own
     * list before the other's, and the fallthrough only ever fires for a colour
     * this build no longer offers.
     */
    private static String name(int rgb, boolean isMetal) {
        int key = rgb & 0xFFFFFF;
        String own = isMetal ? metalName(key) : dyeName(key);
        if (own != null) {
            return own;
        }
        String other = isMetal ? dyeName(key) : metalName(key);
        if (other != null) {
            return other;
        }
        return String.format(java.util.Locale.ROOT, "#%06X", key);
    }

    private static String dyeName(int key) {
        for (net.minecraft.world.item.DyeColor dye : net.minecraft.world.item.DyeColor.values()) {
            if ((dye.getTextureDiffuseColor() & 0xFFFFFF) == key) {
                return dye.getSerializedName().replace('_', ' ');
            }
        }
        return null;
    }

    private static String metalName(int key) {
        for (java.util.Map.Entry<String, Integer> metal : METAL_NAMES.entrySet()) {
            if (metal.getValue() == key) {
                return metal.getKey();
            }
        }
        // ...then anything another mod brought. Named from the material rather
        // than from the item - "tin", not "Tin Ingot" - so a modded fitting
        // reads in the tooltip the way the fifteen above do.
        for (com.example.horsegenetics.neoforge.compat.ModdedMaterials.Metal metal
                : com.example.horsegenetics.neoforge.compat.ModdedMaterials.metals()) {
            if ((metal.colour() & 0xFFFFFF) == key) {
                return metal.material().replace('_', ' ');
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // The fitting colours, defined ONCE here. The bench reads these rather than
    // keeping a second copy, which it used to and which is a drift waiting to
    // happen - two lists of the same seven numbers, and a tooltip that names a
    // colour the bench never applies.
    //
    // Worth knowing when picking one: the metal layer's base peaks at pure
    // white, because it was divided by its own brightest grey. Tinting
    // multiplies, so the colour chosen here is very nearly what the brightest
    // fitting pixel becomes - these are not hints, they are the result.
    // ------------------------------------------------------------------

    /** Plain steel: the metal layer's own undyed constant, so iron reads as never-dyed. */
    public static final int IRON = 0x717171;
    public static final int GOLD = 0xE0B94A;
    public static final int COPPER = 0xC06A44;
    public static final int NETHERITE = 0x4A4248;
    /**
     * Deliberately loud (owner, 2026-09-16). It was a pale blue-white and read as
     * washed-out pewter; spending diamonds on tack that does nothing but look
     * expensive should <i>look</i> expensive.
     */
    public static final int DIAMOND = 0x4DF0FF;
    public static final int EMERALD = 0x3FBF6F;
    public static final int AMETHYST = 0xA079D8;
    /** Nether quartz: the white option, warm rather than clinical. */
    public static final int QUARTZ = 0xF4EFE6;

    // Five more, owner 2026-09-16. Each was pulled AWAY from its nearest
    // neighbour rather than set to the material's average colour, because two
    // fittings a player cannot tell apart are worth one option, not two:
    //   basalt vs netherite   - basalt made lighter and cooler, netherite is a
    //                           warm plum-black, basalt a blue-grey stone
    //   bone vs quartz        - bone made creamier and a step darker, so quartz
    //                           stays the clean white and bone the aged one
    //   prismarine vs pearl   - pearl is the deep teal, prismarine the pale
    //                           sea-green; same hue family, opposite lightness

    /** Red. The one bright primary the wheel was missing entirely. */
    public static final int REDSTONE = 0xD82A22;
    /** Deep teal - the End's colour, darker than prismarine on purpose. */
    public static final int ENDER_PEARL = 0x1E9C94;
    /** Cool blue-grey stone, lighter than netherite so the two do not merge. */
    public static final int BASALT = 0x51535E;
    /** Aged cream, warmer and a shade darker than quartz. */
    public static final int BONE = 0xE3DCC0;
    /** Pale sea-green. */
    public static final int PRISMARINE = 0x8FD8C4;
    /**
     * The true blue the wheel was missing - it ran straight from diamond's cyan
     * to amethyst's purple with nothing between (owner, 2026-09-16).
     */
    public static final int LAPIS = 0x3355D8;
    /**
     * True black, which netherite is not - netherite is a warm plum.
     *
     * <p><b>Deliberately not {@code 0x000000}, and this is a trap rather than a
     * taste call.</b> {@link #forLayer(int)} feeds
     * {@code getArmorLayerTintColor}, and returning <i>zero</i> from that hook
     * tells the renderer to skip the layer altogether. Pure black would
     * therefore not render black hardware; it would render no hardware at all.
     * A near-black with a slight cool cast reads the same and cannot trip it.
     */
    public static final int COAL = 0x1A1A1E;

    /**
     * Kept beside the colours it names so the two cannot drift apart silently.
     * {@code Map.ofEntries} rather than {@code Map.of}, which stops at ten pairs.
     */
    private static final java.util.Map<String, Integer> METAL_NAMES = java.util.Map.ofEntries(
            java.util.Map.entry("iron", IRON),
            java.util.Map.entry("gold", GOLD),
            java.util.Map.entry("copper", COPPER),
            java.util.Map.entry("netherite", NETHERITE),
            java.util.Map.entry("diamond", DIAMOND),
            java.util.Map.entry("emerald", EMERALD),
            java.util.Map.entry("amethyst", AMETHYST),
            java.util.Map.entry("quartz", QUARTZ),
            java.util.Map.entry("redstone", REDSTONE),
            java.util.Map.entry("ender pearl", ENDER_PEARL),
            java.util.Map.entry("basalt", BASALT),
            java.util.Map.entry("bone", BONE),
            java.util.Map.entry("prismarine", PRISMARINE),
            java.util.Map.entry("lapis lazuli", LAPIS),
            java.util.Map.entry("coal", COAL));

    // ------------------------------------------------------------------
    // What each piece looks like when nobody has dyed it. These must match the
    // color_when_undyed values in the equipment assets, because "this zone is
    // already that colour" is how the bench decides not to charge you for a
    // dye that would change nothing.
    // ------------------------------------------------------------------

    /** Vanilla's saddle: brown leather twice over, plain steel hardware. */
    public static final SaddleTint SADDLE_UNDYED = new SaddleTint(0x8A552E, 0x8A552E, IRON);

    /**
     * Vanilla's leather horse armour. The blanket is vanilla's own
     * {@code -6265536}; the other two are the per-channel maxima of the trim and
     * the hardware, measured out of {@code leather_overlay.png} by
     * {@code tools/tack/bake-leather-armor-textures.ps1}.
     */
    public static final SaddleTint ARMOUR_UNDYED = new SaddleTint(0xA06540, 0x95652B, 0x9C9C9C);

    /**
     * What the bench should believe a piece already looks like.
     *
     * <p><b>An armour can arrive already dyed, and not by us.</b> Leather horse
     * armour still has vanilla's crafting-grid recipe, which sets
     * {@code dyed_color} and no component of ours - so a player can walk up with
     * a blue armour that carries no {@code tack_tint} at all. Answering "plain
     * brown" for that piece would be quietly destructive: recolouring only its
     * trim would write the blanket back to brown and throw their dye away
     * without ever saying so. So the blanket is seeded from {@code dyed_color}
     * where there is one, and only the trim and the fittings - which the grid
     * cannot touch - fall back to their own constants.
     */
    public static SaddleTint undyedFor(net.minecraft.world.item.ItemStack stack) {
        if (!stack.is(net.minecraft.world.item.Items.LEATHER_HORSE_ARMOR)) {
            return SADDLE_UNDYED;
        }
        int blanket = net.minecraft.world.item.component.DyedItemColor
                .getOrDefault(stack, ARMOUR_UNDYED.seat());
        return new SaddleTint(blanket, ARMOUR_UNDYED.bridle(), ARMOUR_UNDYED.metal());
    }

    /** Layer order in the equipment asset. Changing these means changing that file too. */
    public static final int LAYER_SEAT = 0;
    public static final int LAYER_BRIDLE = 1;
    public static final int LAYER_METAL = 2;

    public static final Codec<SaddleTint> CODEC = RecordCodecBuilder.create(i -> i.group(
            ExtraCodecs.RGB_COLOR_CODEC.fieldOf("seat").forGetter(SaddleTint::seat),
            ExtraCodecs.RGB_COLOR_CODEC.fieldOf("bridle").forGetter(SaddleTint::bridle),
            ExtraCodecs.RGB_COLOR_CODEC.fieldOf("metal").forGetter(SaddleTint::metal)
    ).apply(i, SaddleTint::new));

    public static final StreamCodec<ByteBuf, SaddleTint> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SaddleTint::seat,
            ByteBufCodecs.INT, SaddleTint::bridle,
            ByteBufCodecs.INT, SaddleTint::metal,
            SaddleTint::new);

    /**
     * The colour for one equipment layer, or {@code -1} for a layer this record
     * does not speak for.
     *
     * <p>{@code -1} rather than {@code 0} on purpose: returning zero from
     * {@code getArmorLayerTintColor} tells the renderer to <b>skip the layer
     * entirely</b>, so a fourth layer added to the asset later would silently
     * vanish instead of rendering untinted.
     */
    public int forLayer(int layerIndex) {
        return switch (layerIndex) {
            case LAYER_SEAT -> seat;
            case LAYER_BRIDLE -> bridle;
            case LAYER_METAL -> metal;
            default -> -1;
        };
    }
}
