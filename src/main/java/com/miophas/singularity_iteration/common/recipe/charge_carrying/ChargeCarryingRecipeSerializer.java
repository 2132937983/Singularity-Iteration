package com.miophas.singularity_iteration.common.recipe.charge_carrying;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

public class ChargeCarryingRecipeSerializer implements RecipeSerializer<ChargeCarryingRecipe> {

    public static final MapCodec<ChargeCarryingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(ChargeCarryingRecipe::getGroup),
            ShapedRecipePattern.MAP_CODEC.forGetter(ChargeCarryingRecipe::getPattern),
            ItemStack.CODEC.fieldOf("result").forGetter(ChargeCarryingRecipe::getResult),
            Codec.BOOL.optionalFieldOf("transfer_charge", true).forGetter(ChargeCarryingRecipe::shouldTransferCharge)
        ).apply(instance, ChargeCarryingRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ChargeCarryingRecipe> STREAM_CODEC = StreamCodec.of(
        ChargeCarryingRecipeSerializer::toNetwork,
        ChargeCarryingRecipeSerializer::fromNetwork
    );

    @Override
    public MapCodec<ChargeCarryingRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, ChargeCarryingRecipe> streamCodec() {
        return STREAM_CODEC;
    }

    private static ChargeCarryingRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        String group = buffer.readUtf();
        ShapedRecipePattern pattern = ShapedRecipePattern.STREAM_CODEC.decode(buffer);
        ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
        boolean transferCharge = buffer.readBoolean();
        return new ChargeCarryingRecipe(group, pattern, result, transferCharge);
    }

    private static void toNetwork(RegistryFriendlyByteBuf buffer, ChargeCarryingRecipe recipe) {
        buffer.writeUtf(recipe.getGroup());
        ShapedRecipePattern.STREAM_CODEC.encode(buffer, recipe.getPattern());
        ItemStack.STREAM_CODEC.encode(buffer, recipe.getResult());
        buffer.writeBoolean(recipe.shouldTransferCharge());
    }
}