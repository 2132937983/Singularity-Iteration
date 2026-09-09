package com.singularity_iteration.mio_icif.recipe.generic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.singularity_iteration.mio_icif.recipe.mio_icif_SingleItemRecipeInput;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * 通用单输入配方序列化器
 */
public class GenericSingleInputRecipeSerializer implements RecipeSerializer<AbstractSingleInputRecipe<mio_icif_SingleItemRecipeInput>> {

    private final RecipeFactory factory;

    public GenericSingleInputRecipeSerializer() {
        this(ElectricFurnaceRecipe::new);
    }

    public GenericSingleInputRecipeSerializer(RecipeFactory factory) {
        this.factory = factory;
    }

    @Override
    public MapCodec<AbstractSingleInputRecipe<mio_icif_SingleItemRecipeInput>> codec() {
        return RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(AbstractSingleInputRecipe::getGroup),
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(r -> r.ingredient),
                ItemStack.CODEC.fieldOf("result").forGetter(r -> r.result),
                Codec.INT.optionalFieldOf("processingtime", 200).forGetter(r -> r.processingTime),
                Codec.INT.optionalFieldOf("energyPerTick", 10).forGetter(r -> r.energyPerTick),
                Codec.FLOAT.optionalFieldOf("experience", 0.0f).forGetter(r -> r instanceof ElectricFurnaceRecipe e ? e.getExperience() : 0.0f)
            ).apply(instance, factory::create)
        );
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, AbstractSingleInputRecipe<mio_icif_SingleItemRecipeInput>> streamCodec() {
        return StreamCodec.of(
            this::toNetwork,
            this::fromNetwork
        );
    }

    private AbstractSingleInputRecipe<mio_icif_SingleItemRecipeInput> fromNetwork(RegistryFriendlyByteBuf buf) {
        String group = buf.readUtf();
        Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
        ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
        int processingTime = buf.readVarInt();
        int energyPerTick = buf.readVarInt();
        float experience = buf.readFloat();
        return factory.create(group, ingredient, result, processingTime, energyPerTick, experience);
    }

    private void toNetwork(RegistryFriendlyByteBuf buf, AbstractSingleInputRecipe<mio_icif_SingleItemRecipeInput> recipe) {
        buf.writeUtf(recipe.getGroup());
        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.ingredient);
        ItemStack.STREAM_CODEC.encode(buf, recipe.result);
        buf.writeVarInt(recipe.processingTime);
        buf.writeVarInt(recipe.energyPerTick);
        buf.writeFloat(recipe instanceof ElectricFurnaceRecipe e ? e.getExperience() : 0.0f);
    }

    @FunctionalInterface
    public interface RecipeFactory {
        AbstractSingleInputRecipe<mio_icif_SingleItemRecipeInput> create(String group, Ingredient ingredient, ItemStack result, int processingTime, int energyPerTick, float experience);
    }
}
