package com.singularity_iteration.mio_icif.recipe.blast_furnace;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * 楂樼倝閰嶆柟忓垪鍖栧櫒
 * 用于序列化和反序列化高配方
 */
@SuppressWarnings("null")
public class mio_icif_BlastFurnaceRecipeSerializer implements RecipeSerializer<mio_icif_BlastFurnaceRecipe> {

    // 默认值
public static final int DEFAULT_DURATION = 2400;
    public static final int DEFAULT_AIR_COST = 1;
    public static final int DEFAULT_HEAT_COST = 128;
    public static final int DEFAULT_INGREDIENT_COUNT = 1;

    // Codec 用于 JSON 序列化
public static final MapCodec<mio_icif_BlastFurnaceRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(mio_icif_BlastFurnaceRecipe::getGroup),
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(mio_icif_BlastFurnaceRecipe::getIngredient),
            ItemStack.CODEC.fieldOf("result").forGetter(mio_icif_BlastFurnaceRecipe::getResult),
            ItemStack.CODEC.fieldOf("secondary_result").forGetter(mio_icif_BlastFurnaceRecipe::getSecondaryResult),
            Codec.INT.optionalFieldOf("duration", DEFAULT_DURATION).forGetter(mio_icif_BlastFurnaceRecipe::getDuration),
            Codec.INT.optionalFieldOf("air_cost", DEFAULT_AIR_COST).forGetter(mio_icif_BlastFurnaceRecipe::getAirCostPerTick),
            Codec.INT.optionalFieldOf("heat_cost", DEFAULT_HEAT_COST).forGetter(mio_icif_BlastFurnaceRecipe::getHeatCostPerTick),
            Codec.INT.optionalFieldOf("count", DEFAULT_INGREDIENT_COUNT).forGetter(mio_icif_BlastFurnaceRecipe::getIngredientCount)
        ).apply(instance, mio_icif_BlastFurnaceRecipe::new)
    );

    // StreamCodec 用于网络同步
    public static final StreamCodec<RegistryFriendlyByteBuf, mio_icif_BlastFurnaceRecipe> STREAM_CODEC = StreamCodec.of(
        mio_icif_BlastFurnaceRecipeSerializer::toNetwork,
        mio_icif_BlastFurnaceRecipeSerializer::fromNetwork
    );

    @Override
    public MapCodec<mio_icif_BlastFurnaceRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, mio_icif_BlastFurnaceRecipe> streamCodec() {
        return STREAM_CODEC;
    }

    private static mio_icif_BlastFurnaceRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        String group = buffer.readUtf();
        Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
        ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
        ItemStack secondaryResult = ItemStack.STREAM_CODEC.decode(buffer);
        int duration = buffer.readVarInt();
        int airCost = buffer.readVarInt();
        int heatCost = buffer.readVarInt();
        int ingredientCount = buffer.readVarInt();
        return new mio_icif_BlastFurnaceRecipe(group, ingredient, result, secondaryResult, duration, airCost, heatCost, ingredientCount);
    }

    private static void toNetwork(RegistryFriendlyByteBuf buffer, mio_icif_BlastFurnaceRecipe recipe) {
        buffer.writeUtf(recipe.getGroup());
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.getIngredient());
        ItemStack.STREAM_CODEC.encode(buffer, recipe.getResult());
        ItemStack.STREAM_CODEC.encode(buffer, recipe.getSecondaryResult());
        buffer.writeVarInt(recipe.getDuration());
        buffer.writeVarInt(recipe.getAirCostPerTick());
        buffer.writeVarInt(recipe.getHeatCostPerTick());
        buffer.writeVarInt(recipe.getIngredientCount());
    }
}