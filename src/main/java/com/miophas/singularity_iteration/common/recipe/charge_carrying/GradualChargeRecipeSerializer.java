package com.miophas.singularity_iteration.common.recipe.charge_carrying;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class GradualChargeRecipeSerializer implements RecipeSerializer<GradualChargeRecipe> {

    public static final MapCodec<GradualChargeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(GradualChargeRecipe::getGroup),
            ItemStack.CODEC.fieldOf("target").forGetter(r -> new ItemStack(r.getTargetItem())),
            Ingredient.CODEC_NONEMPTY.fieldOf("charge_material").forGetter(GradualChargeRecipe::getChargeMaterial),
            Codec.LONG.fieldOf("charge_per_material").forGetter(GradualChargeRecipe::getChargePerMaterial)
        ).apply(instance, (group, target, chargeMaterial, chargePerMaterial) ->
            new GradualChargeRecipe(group, target.getItem(), chargeMaterial, chargePerMaterial))
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, GradualChargeRecipe> STREAM_CODEC = StreamCodec.of(
        GradualChargeRecipeSerializer::toNetwork,
        GradualChargeRecipeSerializer::fromNetwork
    );

    @Override
    public MapCodec<GradualChargeRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, GradualChargeRecipe> streamCodec() {
        return STREAM_CODEC;
    }

    private static GradualChargeRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        String group = buffer.readUtf();
        ItemStack target = ItemStack.STREAM_CODEC.decode(buffer);
        Ingredient chargeMaterial = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
        long chargePerMaterial = buffer.readLong();
        return new GradualChargeRecipe(group, target.getItem(), chargeMaterial, chargePerMaterial);
    }

    private static void toNetwork(RegistryFriendlyByteBuf buffer, GradualChargeRecipe recipe) {
        buffer.writeUtf(recipe.getGroup());
        ItemStack.STREAM_CODEC.encode(buffer, new ItemStack(recipe.getTargetItem()));
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.getChargeMaterial());
        buffer.writeLong(recipe.getChargePerMaterial());
    }
}