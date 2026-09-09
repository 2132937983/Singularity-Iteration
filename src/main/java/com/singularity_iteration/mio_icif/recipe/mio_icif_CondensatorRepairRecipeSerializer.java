package com.singularity_iteration.mio_icif.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * 冷凝模块修复配方序列化器
 */
@SuppressWarnings("null")
public class mio_icif_CondensatorRepairRecipeSerializer implements RecipeSerializer<mio_icif_CondensatorRepairRecipe> {

    // 单例实例
    public static final mio_icif_CondensatorRepairRecipeSerializer INSTANCE = new mio_icif_CondensatorRepairRecipeSerializer();

    // JSON编解码器（不需要额外数据）
    private static final MapCodec<mio_icif_CondensatorRepairRecipe> CODEC = MapCodec.unit(mio_icif_CondensatorRepairRecipe::new);

    // 网络流传输编解码�?- 使用正确的实现方法
private static final StreamCodec<RegistryFriendlyByteBuf, mio_icif_CondensatorRepairRecipe> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public mio_icif_CondensatorRepairRecipe decode(RegistryFriendlyByteBuf buf) {
                    return new mio_icif_CondensatorRepairRecipe();
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, mio_icif_CondensatorRepairRecipe recipe) {
                    // 不需要写入任何数据
            }
            };

    @Override
    public MapCodec<mio_icif_CondensatorRepairRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, mio_icif_CondensatorRepairRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}


