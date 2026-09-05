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

    public static final MapCodec<CarrotCombineRecipe> MAP_CODEC = MapCodec.unit(CarrotCombineRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, CarrotCombineRecipe> STREAM_CODEC =
            StreamCodec.unit(new CarrotCombineRecipe());
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
