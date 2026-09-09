package com.singularity_iteration.mio_icif.recipe.washer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * 洗矿配方序列化器
 * 用于序列化和反序列化洗矿配方
 * 支持多输出（3个输出槽位? */
@SuppressWarnings("null")
public class mio_icif_WasherRecipeSerializer implements RecipeSerializer<mio_icif_WasherRecipe> {

    // 默认处理时间
    public static final int DEFAULT_PROCESSING_TIME = 200;
    // 默认每tick能量消
public static final int DEFAULT_ENERGY_PER_TICK = 10;
    // 默认消耗水量（mb
public static final int DEFAULT_WATER_AMOUNT = 200;
    // 默认输出数量
    public static final int DEFAULT_RESULT_COUNT = 1;

    // Codec 用于 JSON 序列
public static final MapCodec<mio_icif_WasherRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(mio_icif_WasherRecipe::getGroup),
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(mio_icif_WasherRecipe::getIngredient),
            // 主输
        ItemStack.CODEC.fieldOf("primaryResult").forGetter(mio_icif_WasherRecipe::getPrimaryResult),
            Codec.INT.optionalFieldOf("primaryCount", DEFAULT_RESULT_COUNT).forGetter(mio_icif_WasherRecipe::getPrimaryCount),
            // 副输?
            ItemStack.CODEC.optionalFieldOf("secondaryResult", ItemStack.EMPTY).forGetter(mio_icif_WasherRecipe::getSecondaryResult),
            Codec.INT.optionalFieldOf("secondaryCount", 0).forGetter(mio_icif_WasherRecipe::getSecondaryCount),
            // 副输?
            ItemStack.CODEC.optionalFieldOf("tertiaryResult", ItemStack.EMPTY).forGetter(mio_icif_WasherRecipe::getTertiaryResult),
            Codec.INT.optionalFieldOf("tertiaryCount", 0).forGetter(mio_icif_WasherRecipe::getTertiaryCount),
            // 其他参数
            Codec.INT.optionalFieldOf("processingtime", DEFAULT_PROCESSING_TIME).forGetter(mio_icif_WasherRecipe::getProcessingTime),
            Codec.INT.optionalFieldOf("energypertick", DEFAULT_ENERGY_PER_TICK).forGetter(mio_icif_WasherRecipe::getEnergyPerTick),
            Codec.INT.optionalFieldOf("wateramount", DEFAULT_WATER_AMOUNT).forGetter(mio_icif_WasherRecipe::getWaterAmount)
        ).apply(instance, mio_icif_WasherRecipe::new)
    );

    // StreamCodec 用于网络同步
    public static final StreamCodec<RegistryFriendlyByteBuf, mio_icif_WasherRecipe> STREAM_CODEC = StreamCodec.of(
        mio_icif_WasherRecipeSerializer::toNetwork,
        mio_icif_WasherRecipeSerializer::fromNetwork
    );

    // 可选的ItemStack StreamCodec
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
    public MapCodec<mio_icif_WasherRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, mio_icif_WasherRecipe> streamCodec() {
        return STREAM_CODEC;
    }

    /**
 * 从网络冲区读取配方
     */
    private static mio_icif_WasherRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        String group = buffer.readUtf();
        Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
        // 主输
    ItemStack primaryResult = ItemStack.STREAM_CODEC.decode(buffer);
        int primaryCount = buffer.readVarInt();
        // 副输?
        ItemStack secondaryResult = OPTIONAL_ITEM_STREAM_CODEC.decode(buffer);
        int secondaryCount = buffer.readVarInt();
        // 副输?
        ItemStack tertiaryResult = OPTIONAL_ITEM_STREAM_CODEC.decode(buffer);
        int tertiaryCount = buffer.readVarInt();
        // 其他参数
        int processingTime = buffer.readVarInt();
        int energyPerTick = buffer.readVarInt();
        int waterAmount = buffer.readVarInt();
        
        return new mio_icif_WasherRecipe(group, ingredient, 
            primaryResult, primaryCount,
            secondaryResult, secondaryCount,
            tertiaryResult, tertiaryCount,
            processingTime, energyPerTick, waterAmount);
    }

    /**
 * 写入配方到网络冲区
     */
    private static void toNetwork(RegistryFriendlyByteBuf buffer, mio_icif_WasherRecipe recipe) {
        buffer.writeUtf(recipe.getGroup());
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.getIngredient());
        // 主输
    ItemStack.STREAM_CODEC.encode(buffer, recipe.getPrimaryResult());
        buffer.writeVarInt(recipe.getPrimaryCount());
        // 副输?
        OPTIONAL_ITEM_STREAM_CODEC.encode(buffer, recipe.getSecondaryResult());
        buffer.writeVarInt(recipe.getSecondaryCount());
        // 副输?
        OPTIONAL_ITEM_STREAM_CODEC.encode(buffer, recipe.getTertiaryResult());
        buffer.writeVarInt(recipe.getTertiaryCount());
        // 其他参数
        buffer.writeVarInt(recipe.getProcessingTime());
        buffer.writeVarInt(recipe.getEnergyPerTick());
        buffer.writeVarInt(recipe.getWaterAmount());
    }
}


