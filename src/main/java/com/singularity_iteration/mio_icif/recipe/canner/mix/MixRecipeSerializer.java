package com.singularity_iteration.mio_icif.recipe.canner.mix;

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
 * 混合模式配方序列化器
 */
@SuppressWarnings("null")
public class MixRecipeSerializer implements RecipeSerializer<MixRecipe> {

    public static final int DEFAULT_PROCESSING_TIME = 300;
    public static final int DEFAULT_ENERGY_PER_TICK = 15;

    public static final MapCodec<MixRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(MixRecipe::getGroup),
            FluidStack.CODEC.fieldOf("input_fluid").forGetter(MixRecipe::getInputFluid),
            Ingredient.CODEC_NONEMPTY.fieldOf("material").forGetter(MixRecipe::getMaterialIngredient),
            Codec.INT.optionalFieldOf("material_count", 1).forGetter(MixRecipe::getMaterialCount),
            FluidStack.CODEC.fieldOf("result_fluid").forGetter(MixRecipe::getResultFluid),
            ItemStack.CODEC.fieldOf("result_cell").forGetter(MixRecipe::getResultCell),
            Codec.INT.optionalFieldOf("processingtime", DEFAULT_PROCESSING_TIME).forGetter(MixRecipe::getProcessingTime),
            Codec.INT.optionalFieldOf("energypertick", DEFAULT_ENERGY_PER_TICK).forGetter(MixRecipe::getEnergyPerTick)
        ).apply(instance, MixRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, MixRecipe> STREAM_CODEC = StreamCodec.of(
        MixRecipeSerializer::toNetwork,
        MixRecipeSerializer::fromNetwork
    );

    @Override
    public MapCodec<MixRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, MixRecipe> streamCodec() {
        return STREAM_CODEC;
    }

    private static MixRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        String group = buffer.readUtf();
        FluidStack inputFluid = FluidStack.STREAM_CODEC.decode(buffer);
        Ingredient materialIngredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
        int materialCount = buffer.readVarInt();
        FluidStack resultFluid = FluidStack.STREAM_CODEC.decode(buffer);
        ItemStack resultCell = ItemStack.STREAM_CODEC.decode(buffer);
        int processingTime = buffer.readVarInt();
        int energyPerTick = buffer.readVarInt();
        return new MixRecipe(group, inputFluid, materialIngredient, materialCount, resultFluid, resultCell, processingTime, energyPerTick);
    }

    private static void toNetwork(RegistryFriendlyByteBuf buffer, MixRecipe recipe) {
        buffer.writeUtf(recipe.getGroup());
        FluidStack.STREAM_CODEC.encode(buffer, recipe.getInputFluid());
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.getMaterialIngredient());
        buffer.writeVarInt(recipe.getMaterialCount());
        FluidStack.STREAM_CODEC.encode(buffer, recipe.getResultFluid());
        ItemStack.STREAM_CODEC.encode(buffer, recipe.getResultCell());
        buffer.writeVarInt(recipe.getProcessingTime());
        buffer.writeVarInt(recipe.getEnergyPerTick());
    }
}


