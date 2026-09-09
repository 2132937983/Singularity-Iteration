package com.singularity_iteration.mio_icif.recipe.molecular_transformer;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record mio_icif_MolecularTransformerRecipeInput(ItemStack input) implements RecipeInput {

    public static final MapCodec<mio_icif_MolecularTransformerRecipeInput> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            ItemStack.CODEC.fieldOf("input").forGetter(mio_icif_MolecularTransformerRecipeInput::input)
        ).apply(instance, mio_icif_MolecularTransformerRecipeInput::new)
    );

    @Override
    public ItemStack getItem(int slot) {
        return input;
    }

    @Override
    public int size() {
        return 1;
    }
}