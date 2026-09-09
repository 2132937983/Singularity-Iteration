package com.singularity_iteration.mio_icif.recipe;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class mio_icif_IngredientTypes {
    public static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES = DeferredRegister.create(NeoForgeRegistries.INGREDIENT_TYPES, Singularity_Iteration.MOD_ID);

    public static final net.neoforged.neoforge.registries.DeferredHolder<IngredientType<?>, IngredientType<FluidCellIngredient>> FLUID_CELL =
            INGREDIENT_TYPES.register("fluid_cell", () -> FluidCellIngredient.TYPE);

    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        INGREDIENT_TYPES.register(modEventBus);
    }
}