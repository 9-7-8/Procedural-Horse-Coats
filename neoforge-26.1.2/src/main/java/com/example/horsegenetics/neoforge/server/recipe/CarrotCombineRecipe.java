package com.example.horsegenetics.neoforge.server.recipe;

import com.example.horsegenetics.common.genetics.CarrotEffect;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.item.ModItems;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * <b>Combining carrots</b> (roadmap wiki &sect;14.3): two or more breeding
 * carrots crafted together become one carrot carrying all their effects, as a
 * merged {@code carrot_effects} list. No visual change, no cap on count.
 *
 * <p><b>Contradictions are rejected at craft time</b> - a stabilizer with a
 * magnifier, or two gene carrots for the same gene that disagree on
 * het/hom - so the player sees the recipe simply not resolve rather than a
 * silent last-one-wins.
 */
public class CarrotCombineRecipe extends CustomRecipe {

    /**
     * <b>One instance, shared by both codecs, and it has to be the same one.</b>
     *
     * <p>{@code StreamCodec.unit(v)} does not encode anything - it checks that
     * what you handed it {@code equals} the value it captured and writes zero
     * bytes. This recipe has no fields and so no {@code equals}, which makes
     * that check an identity check. Build the map codec from a <i>supplier</i>
     * and the {@code RecipeManager} decodes a fresh object that is by
     * definition not the one the stream codec is holding, so the first attempt
     * to encode it throws:
     *
     * <pre>Can't encode 'CarrotCombineRecipe@3b51', expected 'CarrotCombineRecipe@15b2'</pre>
     *
     * <p><b>That is a kick, not a broken recipe.</b> NeoForge sends the whole
     * recipe set to every joining client as one {@code neoforge:recipe_content}
     * payload, and one unencodable recipe fails the payload, the packet and the
     * login. It shipped in v0.5.014 because <b>singleplayer never encodes it</b>
     * - there is no packet - so every test short of a real client joining a real
     * dedicated server passes. {@code RecipeStreamCodecTest} is that test
     * without the server.
     */
    public static final CarrotCombineRecipe INSTANCE = new CarrotCombineRecipe();

    // MapCodec.unit(INSTANCE), not MapCodec.unit(INSTANCE::new) - the value
    // overload, so decoding hands back the very object STREAM_CODEC compares to.
    public static final MapCodec<CarrotCombineRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, CarrotCombineRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<CarrotCombineRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private static List<String> merged(CraftingInput input) {
        List<String> tokens = new ArrayList<>();
        int carrots = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            List<String> t = com.example.horsegenetics.neoforge.server.BreedingCarrotHandler.tokensOf(s);
            if (t.isEmpty()) {
                return null; // a non-carrot in the grid
            }
            tokens.addAll(t);
            carrots++;
        }
        if (carrots < 2) {
            return null;
        }
        if (CarrotEffect.isContradictory(CarrotEffect.parseList(tokens))) {
            return null;
        }
        return tokens;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return merged(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        List<String> tokens = merged(input);
        if (tokens == null) {
            return ItemStack.EMPTY;
        }
        // The combined carrot keeps the gene-carrot skin if it carries one, else the epigenetic-splice one.
        boolean magic = tokens.stream().anyMatch(t -> t.startsWith("known:"));
        ItemStack out = new ItemStack(magic
                ? ModItems.KNOWN_GENE_SPLICE_CARROT.get()
                : ModItems.UNKNOWN_EPIGENETIC_SPLICE_CARROT.get());
        out.set(ModDataComponents.CARROT_EFFECTS.get(), List.copyOf(tokens));
        return out;
    }

    @Override
    public RecipeSerializer<CarrotCombineRecipe> getSerializer() {
        return SERIALIZER;
    }
}
