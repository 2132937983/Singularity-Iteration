package com.singularity_iteration.mio_icif.recipe.fluid_refining;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Optional;

public class FluidRefiningRecipeSerializer implements RecipeSerializer<FluidRefiningRecipe> {

    public static final MapCodec<FluidRefiningRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(FluidRefiningRecipe::getGroup),
            Ingredient.CODEC.optionalFieldOf("ingredient", Ingredient.EMPTY).forGetter(FluidRefiningRecipe::getInputIngredient),
            BuiltInRegistries.FLUID.byNameCodec().optionalFieldOf("input_fluid").forGetter(r -> Optional.ofNullable(r.getInputFluidTag() == null ? r.getInputFluid() : null)),
            ResourceLocation.CODEC.optionalFieldOf("input_fluid_tag").forGetter(r -> Optional.ofNullable(r.getInputFluidTag() != null ? r.getInputFluidTag().location() : null)),
            FluidStack.CODEC.fieldOf("output_fluid").forGetter(FluidRefiningRecipe::getOutputFluid),
            Codec.INT.optionalFieldOf("processingtime", FluidRefiningRecipes.DEFAULT_PROCESSING_TIME).forGetter(FluidRefiningRecipe::getProcessingTime),
            Codec.INT.optionalFieldOf("energypertick", FluidRefiningRecipes.DEFAULT_ENERGY_PER_TICK).forGetter(FluidRefiningRecipe::getEnergyPerTick)
        ).apply(instance, FluidRefiningRecipeSerializer::createRecipe)
    );

    private static FluidRefiningRecipe createRecipe(String group, Ingredient ingredient,
                                                     Optional<Fluid> inputFluid,
                                                     Optional<ResourceLocation> inputFluidTagLoc,
                                                     FluidStack outputFluid, int processingTime, int energyPerTick) {
        TagKey<Fluid> tag = inputFluidTagLoc.map(loc -> TagKey.create(BuiltInRegistries.FLUID.key(), loc)).orElse(null);
        Fluid fluid = inputFluid.orElse(tag != null ? Fluids.EMPTY : Fluids.EMPTY);
        return new FluidRefiningRecipe(group, ingredient, fluid, tag, outputFluid, processingTime, energyPerTick);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidRefiningRecipe> STREAM_CODEC = StreamCodec.of(
        FluidRefiningRecipeSerializer::toNetwork,
        FluidRefiningRecipeSerializer::fromNetwork
    );

    @Override
    public MapCodec<FluidRefiningRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, FluidRefiningRecipe> streamCodec() {
        return STREAM_CODEC;
    }

    private static FluidRefiningRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        String group = buffer.readUtf();
        Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
        boolean hasTag = buffer.readBoolean();
        TagKey<Fluid> tag = null;
        Fluid inputFluid = Fluids.EMPTY;
        if (hasTag) {
            tag = TagKey.create(BuiltInRegistries.FLUID.key(), buffer.readResourceLocation());
        } else {
            inputFluid = BuiltInRegistries.FLUID.byId(buffer.readVarInt());
        }
        FluidStack outputFluid = FluidStack.STREAM_CODEC.decode(buffer);
        int processingTime = buffer.readVarInt();
        int energyPerTick = buffer.readVarInt();
        return new FluidRefiningRecipe(group, ingredient, inputFluid, tag, outputFluid, processingTime, energyPerTick);
    }

    private static void toNetwork(RegistryFriendlyByteBuf buffer, FluidRefiningRecipe recipe) {
        buffer.writeUtf(recipe.getGroup());
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.getInputIngredient());
        if (recipe.getInputFluidTag() != null) {
            buffer.writeBoolean(true);
            buffer.writeResourceLocation(recipe.getInputFluidTag().location());
        } else {
            buffer.writeBoolean(false);
            buffer.writeVarInt(BuiltInRegistries.FLUID.getId(recipe.getInputFluid()));
        }
        FluidStack.STREAM_CODEC.encode(buffer, recipe.getOutputFluid());
        buffer.writeVarInt(recipe.getProcessingTime());
        buffer.writeVarInt(recipe.getEnergyPerTick());
    }
}