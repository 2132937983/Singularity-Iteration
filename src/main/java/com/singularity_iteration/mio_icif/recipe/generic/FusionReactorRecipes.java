package com.singularity_iteration.mio_icif.recipe.generic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class FusionReactorRecipes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
        DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, Singularity_Iteration.MOD_ID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Singularity_Iteration.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<FusionReactorRecipe>> FUSION_REACTOR_TYPE =
        RECIPE_TYPES.register("fusion_reactor", () -> new RecipeType<FusionReactorRecipe>() {
            @Override
            public String toString() {
                return "fusion_reactor";
            }
        });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FusionReactorRecipe>> FUSION_REACTOR_SERIALIZER =
        RECIPE_SERIALIZERS.register("fusion_reactor", FusionReactorRecipeSerializer::new);

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
        RECIPE_SERIALIZERS.register(eventBus);
    }

    public static class FusionReactorRecipeSerializer implements RecipeSerializer<FusionReactorRecipe> {

        @Override
        public MapCodec<FusionReactorRecipe> codec() {
            return RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter(Recipe::getGroup),
                    Ingredient.CODEC_NONEMPTY.fieldOf("ingredient1").forGetter(FusionReactorRecipe::getIngredient1),
                    Ingredient.CODEC_NONEMPTY.fieldOf("ingredient2").forGetter(FusionReactorRecipe::getIngredient2),
                    ItemStack.CODEC.fieldOf("result").forGetter(FusionReactorRecipe::getResult),
                    Codec.INT.optionalFieldOf("processingtime", 200).forGetter(FusionReactorRecipe::getProcessingTime),
                    Codec.LONG.optionalFieldOf("energyRequired", 1000000L).forGetter(FusionReactorRecipe::getEnergyRequired)
                ).apply(instance, FusionReactorRecipe::new)
            );
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FusionReactorRecipe> streamCodec() {
            return StreamCodec.of(
                this::toNetwork,
                this::fromNetwork
            );
        }

        private FusionReactorRecipe fromNetwork(RegistryFriendlyByteBuf buf) {
            String group = buf.readUtf();
            Ingredient ingredient1 = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            Ingredient ingredient2 = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
            int processingTime = buf.readVarInt();
            long energyRequired = buf.readLong();
            return new FusionReactorRecipe(group, ingredient1, ingredient2, result, processingTime, energyRequired);
        }

        private void toNetwork(RegistryFriendlyByteBuf buf, FusionReactorRecipe recipe) {
            buf.writeUtf(recipe.getGroup());
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.getIngredient1());
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.getIngredient2());
            ItemStack.STREAM_CODEC.encode(buf, recipe.getResult());
            buf.writeVarInt(recipe.getProcessingTime());
            buf.writeLong(recipe.getEnergyRequired());
        }
    }
}
