package com.singularity_iteration.mio_icif.recipe.extractor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * 提取机配方序列化? * 用于序列化和反序列化提取机配? */
@SuppressWarnings("null")
public class mio_icif_ExtractorRecipeSerializer implements RecipeSerializer<mio_icif_ExtractorRecipe> {

    // 默认处理时间
    public static final int DEFAULT_PROCESSING_TIME = 200;
    // 默认每tick能量消
public static final int DEFAULT_ENERGY_PER_TICK = 15;

    // Codec 用于 JSON 序列
public static final MapCodec<mio_icif_ExtractorRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(mio_icif_ExtractorRecipe::getGroup),
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(mio_icif_ExtractorRecipe::getIngredient),
            ItemStack.CODEC.fieldOf("result").forGetter(mio_icif_ExtractorRecipe::getResult),
            Codec.INT.optionalFieldOf("processingtime", DEFAULT_PROCESSING_TIME).forGetter(mio_icif_ExtractorRecipe::getProcessingTime),
            Codec.INT.optionalFieldOf("energypertick", DEFAULT_ENERGY_PER_TICK).forGetter(mio_icif_ExtractorRecipe::getEnergyPerTick)
        ).apply(instance, mio_icif_ExtractorRecipe::new)
    );

    // StreamCodec 用于网络同步
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
 * 从网络冲区读取配方
     */
    private static mio_icif_ExtractorRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        String group = buffer.readUtf();
        Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
        ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
        int processingTime = buffer.readVarInt();
        int energyPerTick = buffer.readVarInt();
        return new mio_icif_ExtractorRecipe(group, ingredient, result, processingTime, energyPerTick);
    }

    /**
 * 写入配方到网络冲区
     */
    private static void toNetwork(RegistryFriendlyByteBuf buffer, mio_icif_ExtractorRecipe recipe) {
        buffer.writeUtf(recipe.getGroup());
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.getIngredient());
        ItemStack.STREAM_CODEC.encode(buffer, recipe.getResult());
        buffer.writeVarInt(recipe.getProcessingTime());
        buffer.writeVarInt(recipe.getEnergyPerTick());
    }
}


