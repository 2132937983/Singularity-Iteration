// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.recipe.generic;

import com.miophas.singularity_iteration.common.recipe.mio_icif_SingleItemRecipeInput;
import com.miophas.singularity_iteration.core.prefab.recipe.AbstractSingleInputRecipe;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * 0.1.11.8: the Particle Polymerizer's recipe serializer: the generic single-input fields plus the optional
 * {@code process} ({@link PolymerizerProcess}). The generic {@code experience} field is still read (and ignored).
 */
public final class NeutronPolymerizerSerializer implements RecipeSerializer<AbstractSingleInputRecipe<mio_icif_SingleItemRecipeInput>> {

    private static final MapCodec<AbstractSingleInputRecipe<mio_icif_SingleItemRecipeInput>> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.STRING.optionalFieldOf("group", "").forGetter(AbstractSingleInputRecipe::getGroup),
        Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(AbstractSingleInputRecipe::getIngredient),
        ItemStack.CODEC.fieldOf("result").forGetter(AbstractSingleInputRecipe::getResult),
        Codec.INT.optionalFieldOf("processingtime", 200).forGetter(AbstractSingleInputRecipe::getProcessingTime),
        Codec.INT.optionalFieldOf("energyPerTick", 10).forGetter(AbstractSingleInputRecipe::getEnergyPerTick),
        Codec.FLOAT.optionalFieldOf("experience", 0F).forGetter(r -> 0F),
        PolymerizerProcess.CODEC.optionalFieldOf("process", PolymerizerProcess.UNKNOWN).forGetter(NeutronPolymerizerSerializer::processOf)
    ).apply(i, (group, ingredient, result, time, eut, xp, process) -> new NeutronPolymerizerRecipe(group, ingredient, result, time, eut, xp, process)));

    private static PolymerizerProcess processOf(AbstractSingleInputRecipe<mio_icif_SingleItemRecipeInput> r) {
        return r instanceof NeutronPolymerizerRecipe n ? n.getProcess() : PolymerizerProcess.UNKNOWN;
    }

    @Override
    public MapCodec<AbstractSingleInputRecipe<mio_icif_SingleItemRecipeInput>> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, AbstractSingleInputRecipe<mio_icif_SingleItemRecipeInput>> streamCodec() {
        return StreamCodec.of(NeutronPolymerizerSerializer::toNetwork, NeutronPolymerizerSerializer::fromNetwork);
    }

    private static AbstractSingleInputRecipe<mio_icif_SingleItemRecipeInput> fromNetwork(RegistryFriendlyByteBuf buf) {
        String group = buf.readUtf();
        Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
        ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
        int time = buf.readVarInt();
        int eut = buf.readVarInt();
        return new NeutronPolymerizerRecipe(group, ingredient, result, time, eut, 0F, PolymerizerProcess.read(buf));
    }

    private static void toNetwork(RegistryFriendlyByteBuf buf, AbstractSingleInputRecipe<mio_icif_SingleItemRecipeInput> recipe) {
        buf.writeUtf(recipe.getGroup());
        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.getIngredient());
        ItemStack.STREAM_CODEC.encode(buf, recipe.getResult());
        buf.writeVarInt(recipe.getProcessingTime());
        buf.writeVarInt(recipe.getEnergyPerTick());
        processOf(recipe).write(buf);
    }
}
