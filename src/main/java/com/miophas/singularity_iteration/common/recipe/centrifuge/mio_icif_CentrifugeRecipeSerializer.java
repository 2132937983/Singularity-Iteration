package com.miophas.singularity_iteration.common.recipe.centrifuge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Recipe serializer for the thermal centrifuge.
 * It serializes and deserializes thermal centrifuge recipes.
 * It supports multiple outputs (3 output slots).
 */
@SuppressWarnings("null")
public class mio_icif_CentrifugeRecipeSerializer implements RecipeSerializer<mio_icif_CentrifugeRecipe> {

    // Default processing time.
    public static final int DEFAULT_PROCESSING_TIME = 500;
    // Default energy consumption per tick.
public static final int DEFAULT_ENERGY_PER_TICK = 40;
    // Default minimum heat that the recipe needs.
public static final int DEFAULT_MIN_HEAT = 5000;
    // Default output count.
    public static final int DEFAULT_RESULT_COUNT = 1;

    // Codec for JSON serialization.
public static final MapCodec<mio_icif_CentrifugeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(mio_icif_CentrifugeRecipe::getGroup),
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(mio_icif_CentrifugeRecipe::getIngredient),
            // Primary output.
        ItemStack.CODEC.fieldOf("primaryResult").forGetter(mio_icif_CentrifugeRecipe::getPrimaryResult),
            Codec.INT.optionalFieldOf("primaryCount", DEFAULT_RESULT_COUNT).forGetter(mio_icif_CentrifugeRecipe::getPrimaryCount),
            // Secondary output.
            ItemStack.CODEC.optionalFieldOf("secondaryResult", ItemStack.EMPTY).forGetter(mio_icif_CentrifugeRecipe::getSecondaryResult),
            Codec.INT.optionalFieldOf("secondaryCount", 0).forGetter(mio_icif_CentrifugeRecipe::getSecondaryCount),
            // Tertiary output.
            ItemStack.CODEC.optionalFieldOf("tertiaryResult", ItemStack.EMPTY).forGetter(mio_icif_CentrifugeRecipe::getTertiaryResult),
            Codec.INT.optionalFieldOf("tertiaryCount", 0).forGetter(mio_icif_CentrifugeRecipe::getTertiaryCount),
            // Other parameters.
            Codec.INT.optionalFieldOf("processingtime", DEFAULT_PROCESSING_TIME).forGetter(mio_icif_CentrifugeRecipe::getProcessingTime),
            Codec.INT.optionalFieldOf("energypertick", DEFAULT_ENERGY_PER_TICK).forGetter(mio_icif_CentrifugeRecipe::getEnergyPerTick),
            Codec.INT.optionalFieldOf("minheat", DEFAULT_MIN_HEAT).forGetter(mio_icif_CentrifugeRecipe::getMinHeatRequired)
        ).apply(instance, mio_icif_CentrifugeRecipe::new)
    );

    // StreamCodec for network synchronization.
    public static final StreamCodec<RegistryFriendlyByteBuf, mio_icif_CentrifugeRecipe> STREAM_CODEC = StreamCodec.of(
        mio_icif_CentrifugeRecipeSerializer::toNetwork,
        mio_icif_CentrifugeRecipeSerializer::fromNetwork
    );

    // Optional ItemStack StreamCodec.
    private static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> OPTIONAL_ITEM_STREAM_CODEC = StreamCodec.of(
        (buf, stack) -> {
            buf.writeBoolean(!stack.isEmpty());
            if (!stack.isEmpty()) {
                ItemStack.STREAM_CODEC.encode(buf, stack);
            }
        },
        buf -> {
            if (buf.readBoolean()) {
                return ItemStack.STREAM_CODEC.decode(buf);
            }
            return ItemStack.EMPTY;
        }
    );

    @Override
    public MapCodec<mio_icif_CentrifugeRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, mio_icif_CentrifugeRecipe> streamCodec() {
        return STREAM_CODEC;
    }

    /**
 * Reads a recipe from the network buffer.
     */
    private static mio_icif_CentrifugeRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        String group = buffer.readUtf();
        Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
        // Primary output.
    ItemStack primaryResult = ItemStack.STREAM_CODEC.decode(buffer);
        int primaryCount = buffer.readVarInt();
        // Secondary output.
        ItemStack secondaryResult = OPTIONAL_ITEM_STREAM_CODEC.decode(buffer);
        int secondaryCount = buffer.readVarInt();
        // Tertiary output.
        ItemStack tertiaryResult = OPTIONAL_ITEM_STREAM_CODEC.decode(buffer);
        int tertiaryCount = buffer.readVarInt();
        // Other parameters.
        int processingTime = buffer.readVarInt();
        int energyPerTick = buffer.readVarInt();
        int minHeatRequired = buffer.readVarInt();
        
        return new mio_icif_CentrifugeRecipe(group, ingredient, 
            primaryResult, primaryCount,
            secondaryResult, secondaryCount,
            tertiaryResult, tertiaryCount,
            processingTime, energyPerTick, minHeatRequired);
    }

    /**
 * Writes a recipe to the network buffer.
     */
    private static void toNetwork(RegistryFriendlyByteBuf buffer, mio_icif_CentrifugeRecipe recipe) {
        buffer.writeUtf(recipe.getGroup());
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.getIngredient());
        // Primary output.
    ItemStack.STREAM_CODEC.encode(buffer, recipe.getPrimaryResult());
        buffer.writeVarInt(recipe.getPrimaryCount());
        // Secondary output.
        OPTIONAL_ITEM_STREAM_CODEC.encode(buffer, recipe.getSecondaryResult());
        buffer.writeVarInt(recipe.getSecondaryCount());
        // Tertiary output.
        OPTIONAL_ITEM_STREAM_CODEC.encode(buffer, recipe.getTertiaryResult());
        buffer.writeVarInt(recipe.getTertiaryCount());
        // Other parameters.
        buffer.writeVarInt(recipe.getProcessingTime());
        buffer.writeVarInt(recipe.getEnergyPerTick());
        buffer.writeVarInt(recipe.getMinHeatRequired());
    }
}


