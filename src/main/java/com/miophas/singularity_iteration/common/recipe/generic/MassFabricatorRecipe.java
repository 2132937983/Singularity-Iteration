package com.miophas.singularity_iteration.common.recipe.generic;

import com.miophas.singularity_iteration.common.recipe.mio_icif_SingleItemRecipeInput;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * A Mass Fabricator amplifier (IC2 matter amplifier, like scrap): one item that matches the ingredient
 * adds {@code credit} EU of amplifier credit, and received EU covered by the credit counts
 * {@code mio_icif_matter_elc.AMPLIFIER_MULTIPLIER} times. It makes nothing by itself.
 * <pre>{"type": "mio_icif:mass_fabricator", "ingredient": {"item": "..."}, "credit": 5000}</pre>
 */
public class MassFabricatorRecipe implements Recipe<mio_icif_SingleItemRecipeInput> {

    private final String group;
    private final Ingredient ingredient;
    private final int credit;

    public MassFabricatorRecipe(String group, Ingredient ingredient, int credit) {
        this.group = group;
        this.ingredient = ingredient;
        this.credit = credit;
    }

    public Ingredient getIngredient() { return ingredient; }

    /** The EU of amplifier credit one item adds. */
    public int getCredit() { return credit; }

    @Override public String getGroup() { return group; }

    @Override
    public boolean matches(mio_icif_SingleItemRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(mio_icif_SingleItemRecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, ingredient);
    }

    /** No recipe book entry. */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return MassFabricatorRecipes.MASS_FABRICATOR_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return MassFabricatorRecipes.MASS_FABRICATOR_TYPE.get();
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(net.minecraft.world.level.block.Blocks.IRON_BLOCK);
    }

    public static final class Serializer implements RecipeSerializer<MassFabricatorRecipe> {
        public static final MapCodec<MassFabricatorRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(MassFabricatorRecipe::getGroup),
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(MassFabricatorRecipe::getIngredient),
            ExtraCodecs.POSITIVE_INT.fieldOf("credit").forGetter(MassFabricatorRecipe::getCredit)
        ).apply(instance, MassFabricatorRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, MassFabricatorRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, MassFabricatorRecipe::getGroup,
            Ingredient.CONTENTS_STREAM_CODEC, MassFabricatorRecipe::getIngredient,
            ByteBufCodecs.VAR_INT, MassFabricatorRecipe::getCredit,
            MassFabricatorRecipe::new);

        @Override public MapCodec<MassFabricatorRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, MassFabricatorRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
