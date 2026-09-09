package com.singularity_iteration.mio_icif.recipe.canner.canning;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * 动态装罐配方序列化学? */
@SuppressWarnings("null")
public class DynamicCanningRecipeSerializer implements RecipeSerializer<DynamicCanningRecipe> {

    public static final int DEFAULT_PROCESSING_TIME = 200;
    public static final int DEFAULT_ENERGY_PER_TICK = 10;
    public static final int DEFAULT_NUTRITION_MULTIPLIER = 1;

    public static final MapCodec<DynamicCanningRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(DynamicCanningRecipe::getGroup),
            Ingredient.CODEC_NONEMPTY.fieldOf("can").forGetter(DynamicCanningRecipe::getCanIngredient),
            Ingredient.CODEC_NONEMPTY.fieldOf("food").forGetter(DynamicCanningRecipe::getFoodIngredient),
            Codec.INT.optionalFieldOf("processingtime", DEFAULT_PROCESSING_TIME).forGetter(DynamicCanningRecipe::getProcessingTime),
            Codec.INT.optionalFieldOf("energypertick", DEFAULT_ENERGY_PER_TICK).forGetter(DynamicCanningRecipe::getEnergyPerTick),
            Codec.INT.optionalFieldOf("multiplier", DEFAULT_NUTRITION_MULTIPLIER).forGetter(DynamicCanningRecipe::getNutritionMultiplier)
        ).apply(instance, DynamicCanningRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, DynamicCanningRecipe> STREAM_CODEC = StreamCodec.of(
        DynamicCanningRecipeSerializer::toNetwork,
        DynamicCanningRecipeSerializer::fromNetwork
    );

    @Override
    public MapCodec<DynamicCanningRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, DynamicCanningRecipe> streamCodec() {
        return STREAM_CODEC;
    }

    private static DynamicCanningRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        String group = buffer.readUtf();
        Ingredient canIngredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
        Ingredient foodIngredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
        int processingTime = buffer.readVarInt();
        int energyPerTick = buffer.readVarInt();
        int nutritionMultiplier = buffer.readVarInt();
        return new DynamicCanningRecipe(group, canIngredient, foodIngredient, processingTime, energyPerTick, nutritionMultiplier);
    }

    private static void toNetwork(RegistryFriendlyByteBuf buffer, DynamicCanningRecipe recipe) {
        buffer.writeUtf(recipe.getGroup());
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.getCanIngredient());
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.getFoodIngredient());
        buffer.writeVarInt(recipe.getProcessingTime());
        buffer.writeVarInt(recipe.getEnergyPerTick());
        buffer.writeVarInt(recipe.getNutritionMultiplier());
    }
}


