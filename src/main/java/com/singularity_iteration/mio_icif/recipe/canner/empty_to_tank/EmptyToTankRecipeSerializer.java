package com.singularity_iteration.mio_icif.recipe.canner.empty_to_tank;

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
 * 单元灌入水槽模式配方序列化器
 */
@SuppressWarnings("null")
public class EmptyToTankRecipeSerializer implements RecipeSerializer<EmptyToTankRecipe> {

    public static final int DEFAULT_PROCESSING_TIME = 100;
    public static final int DEFAULT_ENERGY_PER_TICK = 5;

    public static final MapCodec<EmptyToTankRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(EmptyToTankRecipe::getGroup),
            Ingredient.CODEC_NONEMPTY.fieldOf("cell").forGetter(EmptyToTankRecipe::getCellIngredient),
            FluidStack.CODEC.fieldOf("fluid_output").forGetter(EmptyToTankRecipe::getFluidOutput),
            ItemStack.CODEC.fieldOf("empty_cell").forGetter(EmptyToTankRecipe::getEmptyCellResult),
            Codec.INT.optionalFieldOf("processingtime", DEFAULT_PROCESSING_TIME).forGetter(EmptyToTankRecipe::getProcessingTime),
            Codec.INT.optionalFieldOf("energypertick", DEFAULT_ENERGY_PER_TICK).forGetter(EmptyToTankRecipe::getEnergyPerTick)
        ).apply(instance, EmptyToTankRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, EmptyToTankRecipe> STREAM_CODEC = StreamCodec.of(
        EmptyToTankRecipeSerializer::toNetwork,
        EmptyToTankRecipeSerializer::fromNetwork
    );

    @Override
    public MapCodec<EmptyToTankRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, EmptyToTankRecipe> streamCodec() {
        return STREAM_CODEC;
    }

    private static EmptyToTankRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        String group = buffer.readUtf();
        Ingredient cellIngredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
        FluidStack fluidOutput = FluidStack.STREAM_CODEC.decode(buffer);
        ItemStack emptyCellResult = ItemStack.STREAM_CODEC.decode(buffer);
        int processingTime = buffer.readVarInt();
        int energyPerTick = buffer.readVarInt();
        return new EmptyToTankRecipe(group, cellIngredient, fluidOutput, emptyCellResult, processingTime, energyPerTick);
    }

    private static void toNetwork(RegistryFriendlyByteBuf buffer, EmptyToTankRecipe recipe) {
        buffer.writeUtf(recipe.getGroup());
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.getCellIngredient());
        FluidStack.STREAM_CODEC.encode(buffer, recipe.getFluidOutput());
        ItemStack.STREAM_CODEC.encode(buffer, recipe.getEmptyCellResult());
        buffer.writeVarInt(recipe.getProcessingTime());
        buffer.writeVarInt(recipe.getEnergyPerTick());
    }
}


