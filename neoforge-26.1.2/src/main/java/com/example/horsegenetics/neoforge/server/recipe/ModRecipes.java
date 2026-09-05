package com.example.horsegenetics.neoforge.server.recipe;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The mod's custom crafting recipe serializers (roadmap wiki &sect;14): the
 * paper-parameterised {@link MagicCarrotRecipe} and the effect-merging
 * {@link CarrotCombineRecipe}. Each is a {@code CustomRecipe} with no
 * configurable fields, so its serializer is a {@code unit} codec and one
 * datapack JSON ({@code data/horsegenetics/recipe/*.json}) declares the single
 * instance.
 */
public final class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, HorseGenetics.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MagicCarrotRecipe>> MAGIC_CARROT =
            SERIALIZERS.register("magic_carrot", () -> MagicCarrotRecipe.SERIALIZER);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CarrotCombineRecipe>> CARROT_COMBINE =
            SERIALIZERS.register("carrot_combine", () -> CarrotCombineRecipe.SERIALIZER);

    public static void register(IEventBus modEventBus) {
        SERIALIZERS.register(modEventBus);
    }

    private ModRecipes() {
    }
}
