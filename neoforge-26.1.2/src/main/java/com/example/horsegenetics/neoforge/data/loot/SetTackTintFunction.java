package com.example.horsegenetics.neoforge.data.loot;

import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.data.SaddleTint;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;
import java.util.Locale;

/**
 * Loot function: <b>dye the tack being produced from a named palette</b> - what
 * lets the leatherworker's five tiers sell visibly better-dyed tack without a
 * listing per colour.
 *
 * <pre>
 * { "function": "horsegenetics:set_tack_tint", "palette": "flowers" }
 * </pre>
 *
 * <h2>Why a palette and not a random colour</h2>
 * The request was a ladder a player can see: at his first tier the leatherworker
 * deals in what you could dye a thing with on the walk to his post - bone meal
 * and whatever flowers are growing - and at his last he is selling lapis-dyed
 * armour with diamond fittings. A flat random roll cannot express that, because
 * the <i>expense of the dye</i> is the whole of the progression. So each tier
 * names a {@link Palette} and the palettes are ordered by how much work their
 * colours cost to make.
 *
 * <p>Same timing as {@link SetRandomDyeFunction} and {@link SetRandomGeneFunction}:
 * a villager trade's {@code given_item_modifiers} run when the <b>offer is
 * generated</b>, so the colour is rolled once per restock and then sits in the
 * window at a fixed price rather than re-rolling under the player's cursor.
 *
 * <h2>Why not {@code set_random_dye}</h2>
 * That one writes vanilla's {@code dyed_color}, which reaches only the blanket of
 * leather horse armour and <i>nothing at all</i> on a saddle. This writes our own
 * {@code tack_tint}, which carries three zones and is what the three-layer
 * equipment assets this mod ships actually read - so it colours the saddle too,
 * and it can set the metal fittings, which no vanilla component can express.
 *
 * <p>That is also why the dyed-saddle trade is back. It was withdrawn once for
 * selling a fourteen-emerald saddle indistinguishable from the eight-emerald one;
 * with three tinted layers on the saddle it now sells something you can see.
 */
public class SetTackTintFunction extends LootItemConditionalFunction {

    /**
     * A tier's worth of dyes, and the metal that goes with them.
     *
     * <p>Ordered by <b>what the dye costs to get</b> rather than by taste. The
     * first rung is bone meal and flowers because that is the request, and every
     * rung after it is a step further from a meadow.
     */
    public enum Palette {
        /** Bone meal and whatever is growing: white, dandelion, tulips, poppy, allium. */
        FLOWERS(SaddleTint.IRON,
                DyeColor.WHITE, DyeColor.YELLOW, DyeColor.ORANGE, DyeColor.PINK,
                DyeColor.LIGHT_GRAY, DyeColor.RED, DyeColor.MAGENTA),
        /** Things you have to grow or go and find: cocoa, cactus, sea pickle, blue orchid. */
        GARDEN(SaddleTint.IRON,
                DyeColor.BROWN, DyeColor.LIME, DyeColor.GREEN, DyeColor.LIGHT_BLUE, DyeColor.GRAY),
        /** Things that take a furnace or a squid, with copper on the buckles. */
        KILN(SaddleTint.COPPER,
                DyeColor.BLACK, DyeColor.CYAN, DyeColor.BROWN, DyeColor.GREEN),
        /** The expensive end of the wheel, on brass. */
        DEEP(SaddleTint.GOLD,
                DyeColor.BLUE, DyeColor.PURPLE, DyeColor.CYAN, DyeColor.BLACK),
        /** The master's work: lapis, and diamond fittings. One colour, on purpose. */
        LAPIS(SaddleTint.DIAMOND, DyeColor.BLUE);

        private final int metal;
        private final List<DyeColor> dyes;

        Palette(int metal, DyeColor... dyes) {
            this.metal = metal;
            this.dyes = List.of(dyes);
        }

        int roll(RandomSource random) {
            // & 0xFFFFFF: the component is an RGB colour, and the diffuse colour
            // arrives with an alpha byte on it. The bench does the same.
            return dyes.get(random.nextInt(dyes.size())).getTextureDiffuseColor() & 0xFFFFFF;
        }

        static final Codec<Palette> CODEC = Codec.STRING.xmap(
                s -> valueOf(s.toUpperCase(Locale.ROOT)),
                p -> p.name().toLowerCase(Locale.ROOT));
    }

    public static final MapCodec<SetTackTintFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(
            i -> commonFields(i).and(
                    Palette.CODEC.fieldOf("palette").forGetter(f -> f.palette)
            ).apply(i, SetTackTintFunction::new));

    private final Palette palette;

    protected SetTackTintFunction(List<LootItemCondition> conditions, Palette palette) {
        super(conditions);
        this.palette = palette;
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        RandomSource random = context.getRandom();
        // Seat and bridle roll separately. Within one palette the colours are
        // near enough relations that two of them read as somebody's choice; one
        // colour on both would make every piece he ever sells look like the
        // same piece.
        stack.set(ModDataComponents.TACK_TINT.get(),
                new SaddleTint(palette.roll(random), palette.roll(random), palette.metal));
        return stack;
    }

    @Override
    public MapCodec<? extends LootItemConditionalFunction> codec() {
        return MAP_CODEC;
    }
}
