package com.singularity_iteration.mio_icif.recipe.canner.fill_from_tank;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 水槽灌满单元模式配方序列化器
 */
@SuppressWarnings("null")
public class FillFromTankRecipeSerializer implements RecipeSerializer<FillFromTankRecipe> {

    public static final int DEFAULT_PROCESSING_TIME = 100;
    public static final int DEFAULT_ENERGY_PER_TICK = 5;

    public static final MapCodec<FillFromTankRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(FillFromTankRecipe::getGroup),
            Ingredient.CODEC_NONEMPTY.fieldOf("empty_cell").forGetter(FillFromTankRecipe::getEmptyCellIngredient),
            FluidStack.CODEC.fieldOf("fluid_input").forGetter(FillFromTankRecipe::getFluidInput),
            ItemStack.CODEC.fieldOf("filled_cell").forGetter(FillFromTankRecipe::getFilledCellResult),
            Codec.INT.optionalFieldOf("processingtime", DEFAULT_PROCESSING_TIME).forGetter(FillFromTankRecipe::getProcessingTime),
            Codec.INT.optionalFieldOf("energypertick", DEFAULT_ENERGY_PER_TICK).forGetter(FillFromTankRecipe::getEnergyPerTick)
        ).apply(instance, FillFromTankRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, FillFromTankRecipe> STREAM_CODEC = StreamCodec.of(
        FillFromTankRecipeSerializer::toNetwork,
        FillFromTankRecipeSerializer::fromNetwork
    );

    @Override
    public MapCodec<FillFromTankRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, FillFromTankRecipe> streamCodec() {
        return STREAM_CODEC;
    }

    private static FillFromTankRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        String group = buffer.readUtf();
        Ingredient emptyCellIngredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
        FluidStack fluidInput = FluidStack.STREAM_CODEC.decode(buffer);
        ItemStack filledCellResult = ItemStack.STREAM_CODEC.decode(buffer);
        int processingTime = buffer.readVarInt();
        int energyPerTick = buffer.readVarInt();
        return new FillFromTankRecipe(group, emptyCellIngredient, fluidInput, filledCellResult, processingTime, energyPerTick);
    }

    private static void toNetwork(RegistryFriendlyByteBuf buffer, FillFromTankRecipe recipe) {
        buffer.writeUtf(recipe.getGroup());
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.getEmptyCellIngredient());
        FluidStack.STREAM_CODEC.encode(buffer, recipe.getFluidInput());
        ItemStack.STREAM_CODEC.encode(buffer, recipe.getFilledCellResult());
        buffer.writeVarInt(recipe.getProcessingTime());
        buffer.writeVarInt(recipe.getEnergyPerTick());
    }
}


