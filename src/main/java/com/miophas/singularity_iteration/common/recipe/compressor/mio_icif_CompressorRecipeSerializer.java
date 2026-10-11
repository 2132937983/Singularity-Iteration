package com.miophas.singularity_iteration.common.recipe.compressor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Recipe serializer for the compressor.
 * It serializes and deserializes compressor recipes.
 */
@SuppressWarnings("null")
public class mio_icif_CompressorRecipeSerializer implements RecipeSerializer<mio_icif_CompressorRecipe> {

    // Default processing time.
    public static final int DEFAULT_PROCESSING_TIME = 200;
    // Default energy consumption per tick.
public static final int DEFAULT_ENERGY_PER_TICK = 10;

    // Default input count.
    public static final int DEFAULT_INGREDIENT_COUNT = 1;

    // Codec for JSON serialization.
public static final MapCodec<mio_icif_CompressorRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(mio_icif_CompressorRecipe::getGroup),
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(mio_icif_CompressorRecipe::getIngredient),
            ItemStack.CODEC.fieldOf("result").forGetter(mio_icif_CompressorRecipe::getResult),
            Codec.INT.optionalFieldOf("processingtime", DEFAULT_PROCESSING_TIME).forGetter(mio_icif_CompressorRecipe::getProcessingTime),
            Codec.INT.optionalFieldOf("energypertick", DEFAULT_ENERGY_PER_TICK).forGetter(mio_icif_CompressorRecipe::getEnergyPerTick),
            Codec.INT.optionalFieldOf("count", DEFAULT_INGREDIENT_COUNT).forGetter(mio_icif_CompressorRecipe::getIngredientCount)
        ).apply(instance, mio_icif_CompressorRecipe::new)
    );

    // StreamCodec for network synchronization.
    public static final StreamCodec<RegistryFriendlyByteBuf, mio_icif_CompressorRecipe> STREAM_CODEC = StreamCodec.of(
        mio_icif_CompressorRecipeSerializer::toNetwork,
        mio_icif_CompressorRecipeSerializer::fromNetwork
    );

    @Override
    public MapCodec<mio_icif_CompressorRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, mio_icif_CompressorRecipe> streamCodec() {
        return STREAM_CODEC;
    }

    /**
 * Reads a recipe from the network buffer.
     */
    private static mio_icif_CompressorRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        String group = buffer.readUtf();
        Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
        ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
        int processingTime = buffer.readVarInt();
        int energyPerTick = buffer.readVarInt();
        int ingredientCount = buffer.readVarInt();
        return new mio_icif_CompressorRecipe(group, ingredient, result, processingTime, energyPerTick, ingredientCount);
    }

    /**
 * Writes a recipe to the network buffer.
     */
    private static void toNetwork(RegistryFriendlyByteBuf buffer, mio_icif_CompressorRecipe recipe) {
        buffer.writeUtf(recipe.getGroup());
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.getIngredient());
        ItemStack.STREAM_CODEC.encode(buffer, recipe.getResult());
        buffer.writeVarInt(recipe.getProcessingTime());
        buffer.writeVarInt(recipe.getEnergyPerTick());
        buffer.writeVarInt(recipe.getIngredientCount());
    }
}


