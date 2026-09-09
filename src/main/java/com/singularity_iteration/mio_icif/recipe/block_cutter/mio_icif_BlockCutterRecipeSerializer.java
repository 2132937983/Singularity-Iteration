package com.singularity_iteration.mio_icif.recipe.block_cutter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * 方块切割机配方序列化学? */
@SuppressWarnings("null")
public class mio_icif_BlockCutterRecipeSerializer implements RecipeSerializer<mio_icif_BlockCutterRecipe> {

    public static final int DEFAULT_PROCESSING_TIME = 450;
    public static final int DEFAULT_ENERGY_PER_TICK = 4;
    public static final int DEFAULT_INGREDIENT_COUNT = 1;
    public static final int DEFAULT_HARDNESS = 2;

    public static final MapCodec<mio_icif_BlockCutterRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(mio_icif_BlockCutterRecipe::getGroup),
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(mio_icif_BlockCutterRecipe::getIngredient),
            ItemStack.CODEC.fieldOf("result").forGetter(mio_icif_BlockCutterRecipe::getResult),
            Codec.INT.optionalFieldOf("processingtime", DEFAULT_PROCESSING_TIME).forGetter(mio_icif_BlockCutterRecipe::getProcessingTime),
            Codec.INT.optionalFieldOf("energypertick", DEFAULT_ENERGY_PER_TICK).forGetter(mio_icif_BlockCutterRecipe::getEnergyPerTick),
            Codec.INT.optionalFieldOf("count", DEFAULT_INGREDIENT_COUNT).forGetter(mio_icif_BlockCutterRecipe::getIngredientCount),
            Codec.INT.optionalFieldOf("hardness", DEFAULT_HARDNESS).forGetter(mio_icif_BlockCutterRecipe::getHardness)
        ).apply(instance, mio_icif_BlockCutterRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, mio_icif_BlockCutterRecipe> STREAM_CODEC = StreamCodec.of(
        mio_icif_BlockCutterRecipeSerializer::toNetwork,
        mio_icif_BlockCutterRecipeSerializer::fromNetwork
    );

    @Override
    public MapCodec<mio_icif_BlockCutterRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, mio_icif_BlockCutterRecipe> streamCodec() {
        return STREAM_CODEC;
    }

    private static mio_icif_BlockCutterRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        String group = buffer.readUtf();
        Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
        ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
        int processingTime = buffer.readVarInt();
        int energyPerTick = buffer.readVarInt();
        int ingredientCount = buffer.readVarInt();
        int hardness = buffer.readVarInt();
        return new mio_icif_BlockCutterRecipe(group, ingredient, result, processingTime, energyPerTick, ingredientCount, hardness);
    }

    private static void toNetwork(RegistryFriendlyByteBuf buffer, mio_icif_BlockCutterRecipe recipe) {
        buffer.writeUtf(recipe.getGroup());
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.getIngredient());
        ItemStack.STREAM_CODEC.encode(buffer, recipe.getResult());
        buffer.writeVarInt(recipe.getProcessingTime());
        buffer.writeVarInt(recipe.getEnergyPerTick());
        buffer.writeVarInt(recipe.getIngredientCount());
        buffer.writeVarInt(recipe.getHardness());
    }
}


