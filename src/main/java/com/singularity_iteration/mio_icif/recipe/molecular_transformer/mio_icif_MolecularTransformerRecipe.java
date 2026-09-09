package com.singularity_iteration.mio_icif.recipe.molecular_transformer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import com.singularity_iteration.mio_icif.api.recipe.IRecipeProcessingInfo;

public class mio_icif_MolecularTransformerRecipe implements Recipe<mio_icif_MolecularTransformerRecipeInput>, IRecipeProcessingInfo {

    private final Ingredient input;
    private final ItemStack result;
    private final long euCost;
    private final int duration;

    public mio_icif_MolecularTransformerRecipe(Ingredient input, ItemStack result, long euCost, int duration) {
        this.input = input;
        this.result = result;
        this.euCost = euCost;
        this.duration = duration;
    }

    @Override
    public boolean matches(mio_icif_MolecularTransformerRecipeInput input, Level level) {
        return this.input.test(input.input());
    }

    @Override
    public ItemStack assemble(mio_icif_MolecularTransformerRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return mio_icif_MolecularTransformerRecipes.MOLECULAR_TRANSFORMER_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return mio_icif_MolecularTransformerRecipes.MOLECULAR_TRANSFORMER_TYPE.get();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(this.input);
        return ingredients;
    }

    public ItemStack getResult() {
        return result;
    }

    public long getEuCost() {
        return euCost;
    }

    public int getDuration() {
        return duration;
    }

    @Override
    public int getProcessingTime() {
        return duration;
    }

    @Override
    public int getEnergyPerTick() {
        return duration > 0 ? (int) Math.min(euCost / duration, Integer.MAX_VALUE) : 0;
    }

    public static class Serializer implements RecipeSerializer<mio_icif_MolecularTransformerRecipe> {

        public static final MapCodec<mio_icif_MolecularTransformerRecipe> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                Ingredient.CODEC.fieldOf("input").forGetter(r -> r.input),
                ItemStack.CODEC.fieldOf("result").forGetter(r -> r.result),
                Codec.LONG.fieldOf("eu_cost").forGetter(r -> r.euCost),
                Codec.INT.fieldOf("duration").forGetter(r -> r.duration)
            ).apply(instance, mio_icif_MolecularTransformerRecipe::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, mio_icif_MolecularTransformerRecipe> STREAM_CODEC = StreamCodec.of(
            mio_icif_MolecularTransformerRecipe.Serializer::toNetwork,
            mio_icif_MolecularTransformerRecipe.Serializer::fromNetwork
        );

        @Override
        public MapCodec<mio_icif_MolecularTransformerRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, mio_icif_MolecularTransformerRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static mio_icif_MolecularTransformerRecipe fromNetwork(RegistryFriendlyByteBuf buf) {
            Ingredient input = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
            long euCost = buf.readLong();
            int duration = buf.readVarInt();
            return new mio_icif_MolecularTransformerRecipe(input, result, euCost, duration);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buf, mio_icif_MolecularTransformerRecipe recipe) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.input);
            ItemStack.STREAM_CODEC.encode(buf, recipe.result);
            buf.writeLong(recipe.euCost);
            buf.writeVarInt(recipe.duration);
        }
    }
}