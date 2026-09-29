package com.miophas.singularity_iteration.common.recipe.charge_carrying;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.List;

public class ChargeCarryingShapelessRecipeSerializer implements RecipeSerializer<ChargeCarryingShapelessRecipe> {

    public static final MapCodec<ChargeCarryingShapelessRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(ChargeCarryingShapelessRecipe::getGroup),
            Ingredient.LIST_CODEC.fieldOf("ingredients").forGetter(ChargeCarryingShapelessRecipe::getIngredientList),
            ItemStack.CODEC.fieldOf("result").forGetter(ChargeCarryingShapelessRecipe::getResult),
            Codec.BOOL.optionalFieldOf("transfer_charge", true).forGetter(ChargeCarryingShapelessRecipe::shouldTransferCharge)
        ).apply(instance, ChargeCarryingShapelessRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ChargeCarryingShapelessRecipe> STREAM_CODEC = StreamCodec.of(
        ChargeCarryingShapelessRecipeSerializer::toNetwork,
        ChargeCarryingShapelessRecipeSerializer::fromNetwork
    );

    @Override
    public MapCodec<ChargeCarryingShapelessRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, ChargeCarryingShapelessRecipe> streamCodec() {
        return STREAM_CODEC;
    }

    private static ChargeCarryingShapelessRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        String group = buffer.readUtf();
        int count = buffer.readVarInt();
        java.util.List<Ingredient> ingredients = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            ingredients.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
        }
        ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
        boolean transferCharge = buffer.readBoolean();
        return new ChargeCarryingShapelessRecipe(group, ingredients, result, transferCharge);
    }

    private static void toNetwork(RegistryFriendlyByteBuf buffer, ChargeCarryingShapelessRecipe recipe) {
        buffer.writeUtf(recipe.getGroup());
        java.util.List<Ingredient> ingredients = recipe.getIngredientList();
        buffer.writeVarInt(ingredients.size());
        for (Ingredient ingredient : ingredients) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
        }
        ItemStack.STREAM_CODEC.encode(buffer, recipe.getResult());
        buffer.writeBoolean(recipe.shouldTransferCharge());
    }
}