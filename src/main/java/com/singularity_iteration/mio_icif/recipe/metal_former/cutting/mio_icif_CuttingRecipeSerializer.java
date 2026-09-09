package com.singularity_iteration.mio_icif.recipe.metal_former.cutting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

@SuppressWarnings("null")
public class mio_icif_CuttingRecipeSerializer implements RecipeSerializer<mio_icif_CuttingRecipe> {

    public static final int DEFAULT_PROCESSING_TIME = 100;
    public static final int DEFAULT_ENERGY_PER_TICK = 10;
    public static final int DEFAULT_INGREDIENT_COUNT = 1;

    public static final MapCodec<mio_icif_CuttingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(mio_icif_CuttingRecipe::getGroup),
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(mio_icif_CuttingRecipe::getIngredient),
            ItemStack.CODEC.fieldOf("result").forGetter(mio_icif_CuttingRecipe::getResult),
            Codec.INT.optionalFieldOf("processingtime", DEFAULT_PROCESSING_TIME).forGetter(mio_icif_CuttingRecipe::getProcessingTime),
            Codec.INT.optionalFieldOf("energypertick", DEFAULT_ENERGY_PER_TICK).forGetter(mio_icif_CuttingRecipe::getEnergyPerTick),
            Codec.INT.optionalFieldOf("count", DEFAULT_INGREDIENT_COUNT).forGetter(mio_icif_CuttingRecipe::getIngredientCount)
        ).apply(instance, mio_icif_CuttingRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, mio_icif_CuttingRecipe> STREAM_CODEC = StreamCodec.of(
        mio_icif_CuttingRecipeSerializer::toNetwork,
        mio_icif_CuttingRecipeSerializer::fromNetwork
    );

    @Override
    public MapCodec<mio_icif_CuttingRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, mio_icif_CuttingRecipe> streamCodec() {
        return STREAM_CODEC;
    }

    private static mio_icif_CuttingRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        String group = buffer.readUtf();
        Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
        ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
        int processingTime = buffer.readVarInt();
        int energyPerTick = buffer.readVarInt();
        int ingredientCount = buffer.readVarInt();
        return new mio_icif_CuttingRecipe(group, ingredient, result, processingTime, energyPerTick, ingredientCount);
    }

    private static void toNetwork(RegistryFriendlyByteBuf buffer, mio_icif_CuttingRecipe recipe) {
        buffer.writeUtf(recipe.getGroup());
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.getIngredient());
        ItemStack.STREAM_CODEC.encode(buffer, recipe.getResult());
        buffer.writeVarInt(recipe.getProcessingTime());
        buffer.writeVarInt(recipe.getEnergyPerTick());
        buffer.writeVarInt(recipe.getIngredientCount());
    }
}


