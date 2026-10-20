package com.miophas.singularity_iteration.common.recipe.fermenter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/** JSON and network form of {@link FermenterRecipe}. */
public class FermenterRecipeSerializer implements RecipeSerializer<FermenterRecipe> {

    public static final MapCodec<FermenterRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING.optionalFieldOf("group", "").forGetter(FermenterRecipe::getGroup),
        SizedFluidIngredient.FLAT_CODEC.fieldOf("input").forGetter(FermenterRecipe::getInput),
        ExtraCodecs.POSITIVE_INT.fieldOf("heat").forGetter(FermenterRecipe::getHeat),
        FluidStack.CODEC.fieldOf("output").forGetter(FermenterRecipe::getOutput),
        ItemStack.OPTIONAL_CODEC.optionalFieldOf("byproduct", ItemStack.EMPTY).forGetter(FermenterRecipe::getByproduct),
        ExtraCodecs.POSITIVE_INT.optionalFieldOf("byproduct_interval", FermenterRecipe.DEFAULT_BYPRODUCT_INTERVAL)
            .forGetter(FermenterRecipe::getByproductInterval)
    ).apply(instance, FermenterRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FermenterRecipe> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, FermenterRecipe::getGroup,
        SizedFluidIngredient.STREAM_CODEC, FermenterRecipe::getInput,
        ByteBufCodecs.VAR_INT, FermenterRecipe::getHeat,
        FluidStack.STREAM_CODEC, FermenterRecipe::getOutput,
        ItemStack.OPTIONAL_STREAM_CODEC, FermenterRecipe::getByproduct,
        ByteBufCodecs.VAR_INT, FermenterRecipe::getByproductInterval,
        FermenterRecipe::new);

    @Override
    public MapCodec<FermenterRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, FermenterRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
