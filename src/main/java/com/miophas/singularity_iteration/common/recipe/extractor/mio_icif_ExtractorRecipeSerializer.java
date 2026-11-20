package com.miophas.singularity_iteration.common.recipe.extractor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Extractor recipe serializer.
 * It serializes and deserializes extractor recipes. */
@SuppressWarnings("null")
public class mio_icif_ExtractorRecipeSerializer implements RecipeSerializer<mio_icif_ExtractorRecipe> {

    // Default processing time.
    public static final int DEFAULT_PROCESSING_TIME = 200;
    // Default energy consumption per tick.
public static final int DEFAULT_ENERGY_PER_TICK = 15;

    // Codec for JSON serialization.
public static final MapCodec<mio_icif_ExtractorRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(mio_icif_ExtractorRecipe::getGroup),
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(mio_icif_ExtractorRecipe::getIngredient),
            ItemStack.CODEC.fieldOf("result").forGetter(mio_icif_ExtractorRecipe::getResult),
            // 0.1.11.116: absent is -1 (the extractor then runs its stock 300 t at 2 EU/t)
            Codec.INT.optionalFieldOf("processingtime", -1).forGetter(mio_icif_ExtractorRecipe::getProcessingTimeRaw),
            Codec.INT.optionalFieldOf("energypertick", -1).forGetter(mio_icif_ExtractorRecipe::getEnergyPerTickRaw)
        ).apply(instance, mio_icif_ExtractorRecipe::new)
    );

    // StreamCodec for network synchronization.
    public static final StreamCodec<RegistryFriendlyByteBuf, mio_icif_ExtractorRecipe> STREAM_CODEC = StreamCodec.of(
        mio_icif_ExtractorRecipeSerializer::toNetwork,
        mio_icif_ExtractorRecipeSerializer::fromNetwork
    );

    @Override
    public MapCodec<mio_icif_ExtractorRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, mio_icif_ExtractorRecipe> streamCodec() {
        return STREAM_CODEC;
    }

    /**
 * Reads a recipe from the network buffer.
     */
    private static mio_icif_ExtractorRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        String group = buffer.readUtf();
        Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
        ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
        int processingTime = buffer.readVarInt() - 1;   // 0.1.11.116: -1 (none) travels as 0
        int energyPerTick = buffer.readVarInt() - 1;
        return new mio_icif_ExtractorRecipe(group, ingredient, result, processingTime, energyPerTick);
    }

    /**
 * Writes a recipe to the network buffer.
     */
    private static void toNetwork(RegistryFriendlyByteBuf buffer, mio_icif_ExtractorRecipe recipe) {
        buffer.writeUtf(recipe.getGroup());
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.getIngredient());
        ItemStack.STREAM_CODEC.encode(buffer, recipe.getResult());
        buffer.writeVarInt(recipe.getProcessingTimeRaw() + 1);
        buffer.writeVarInt(recipe.getEnergyPerTickRaw() + 1);
    }
}


