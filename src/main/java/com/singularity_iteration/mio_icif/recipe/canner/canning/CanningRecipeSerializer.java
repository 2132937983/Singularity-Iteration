package com.singularity_iteration.mio_icif.recipe.canner.canning;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * 装罐模式配方序列化器
 */
@SuppressWarnings("null")
public class CanningRecipeSerializer implements RecipeSerializer<CanningRecipe> {

    public static final int DEFAULT_PROCESSING_TIME = 200;
    public static final int DEFAULT_ENERGY_PER_TICK = 10;

    public static final MapCodec<CanningRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(CanningRecipe::getGroup),
            Ingredient.CODEC_NONEMPTY.fieldOf("can").forGetter(CanningRecipe::getCanIngredient),
            Ingredient.CODEC_NONEMPTY.fieldOf("food").forGetter(CanningRecipe::getFoodIngredient),
            ItemStack.CODEC.fieldOf("result").forGetter(CanningRecipe::getResult),
            Codec.INT.optionalFieldOf("processingtime", DEFAULT_PROCESSING_TIME).forGetter(CanningRecipe::getProcessingTime),
            Codec.INT.optionalFieldOf("energypertick", DEFAULT_ENERGY_PER_TICK).forGetter(CanningRecipe::getEnergyPerTick)
        ).apply(instance, CanningRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, CanningRecipe> STREAM_CODEC = StreamCodec.of(
        CanningRecipeSerializer::toNetwork,
        CanningRecipeSerializer::fromNetwork
    );

    @Override
    public MapCodec<CanningRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, CanningRecipe> streamCodec() {
        return STREAM_CODEC;
    }

    private static CanningRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        String group = buffer.readUtf();
        Ingredient canIngredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
        Ingredient foodIngredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
        ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
        int processingTime = buffer.readVarInt();
        int energyPerTick = buffer.readVarInt();
        return new CanningRecipe(group, canIngredient, foodIngredient, result, processingTime, energyPerTick);
    }

    private static void toNetwork(RegistryFriendlyByteBuf buffer, CanningRecipe recipe) {
        buffer.writeUtf(recipe.getGroup());
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.getCanIngredient());
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.getFoodIngredient());
        ItemStack.STREAM_CODEC.encode(buffer, recipe.getResult());
        buffer.writeVarInt(recipe.getProcessingTime());
        buffer.writeVarInt(recipe.getEnergyPerTick());
    }
}


