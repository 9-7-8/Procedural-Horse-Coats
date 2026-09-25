package com.example.horsegenetics.neoforge.server.recipe;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The mod's custom crafting recipe serializers (roadmap wiki &sect;14): the
 * paper-parameterised {@link KnownGeneSpliceRecipe} and the effect-merging
 * {@link CarrotCombineRecipe}. Each is a {@code CustomRecipe} with no
 * configurable fields, so its serializer is a {@code unit} codec and one
 * datapack JSON ({@code data/horsegenetics/recipe/*.json}) declares the single
 * instance.
 */
public final class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, HorseGenetics.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<KnownGeneSpliceRecipe>> KNOWN_GENE_SPLICE =
            SERIALIZERS.register("known_gene_splice", () -> KnownGeneSpliceRecipe.SERIALIZER);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CarrotCombineRecipe>> CARROT_COMBINE =
            SERIALIZERS.register("carrot_combine", () -> CarrotCombineRecipe.SERIALIZER);

    /** The Basic chamber - custom so the water container is handed back, not eaten. */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StasisChamberRecipe>> STASIS_CHAMBER =
            SERIALIZERS.register("stasis_chamber", () -> StasisChamberRecipe.SERIALIZER);

    /** The three tier upgrades - custom so a horse already inside rides across. */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StasisUpgradeRecipe>> STASIS_UPGRADE =
            SERIALIZERS.register("stasis_upgrade", () -> StasisUpgradeRecipe.SERIALIZER);

    /** The Emergency chamber - custom for the same reason as the upgrades. */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EmergencyChamberRecipe>> EMERGENCY_CHAMBER =
            SERIALIZERS.register("emergency_chamber", () -> EmergencyChamberRecipe.SERIALIZER);

    public static void register(IEventBus modEventBus) {
        SERIALIZERS.register(modEventBus);
    }

    private ModRecipes() {
    }
}
