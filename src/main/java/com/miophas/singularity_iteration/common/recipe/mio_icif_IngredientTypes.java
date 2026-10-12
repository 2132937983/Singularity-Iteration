package com.miophas.singularity_iteration.common.recipe;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class mio_icif_IngredientTypes {
    public static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES = DeferredRegister.create(NeoForgeRegistries.INGREDIENT_TYPES, Singularity_Iteration.MOD_ID);

    public static final net.neoforged.neoforge.registries.DeferredHolder<IngredientType<?>, IngredientType<FluidCellIngredient>> FLUID_CELL =
            INGREDIENT_TYPES.register("fluid_cell", () -> FluidCellIngredient.TYPE);

    /** EN: Seed bag of one plant and Gain range (0.1.8.2). ZH: 指定植物与产量范围的种子袋（0.1.8.2）。JA: 指定した植物と収量範囲の種子袋（0.1.8.2）。 */
    public static final net.neoforged.neoforge.registries.DeferredHolder<IngredientType<?>, IngredientType<CropSeedIngredient>> CROP_SEED =
            INGREDIENT_TYPES.register("crop_seed", () -> CropSeedIngredient.TYPE);

    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        INGREDIENT_TYPES.register(modEventBus);
    }
}