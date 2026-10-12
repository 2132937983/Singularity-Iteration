// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.recipe;

import com.miophas.singularity_iteration.common.item.crop.CropSeedItem;
import com.miophas.singularity_iteration.common.item.normal.mio_icif_normal;
import com.miophas.singularity_iteration.core.api.item.ICropSeedItem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

/**
 * <p><b>EN:</b> Ingredient {@code mio_icif:crop_seed} (0.1.8.2): a seed bag of one plant with Gain in
 * {@code [min_gain, max_gain]}. External machines that test recipes with {@link Ingredient#test}
 * (for example the Immersive Engineering Garden Cloche) can then give more output for a higher
 * Gain. JSON: {@code {"type": "mio_icif:crop_seed", "plant": "mio_icif:wheat", "min_gain": 8, "max_gain": 15}}.
 * The example stack (JEI, recipe books) is an analyzed bag with Growth 1, Gain {@code min_gain}, Resistance 1.
 * <p>Limit: a machine that flattens an ingredient into its example stacks and looks recipes up by
 * item (Thermal Series) cannot tell bags apart; such machines use the seed bag as a catalyst instead.
 * <p><b>ZH:</b> 配方材料 {@code mio_icif:crop_seed}（0.1.8.2）：某一植物、产量在 {@code [min_gain, max_gain]} 内的种子袋。
 * 使用 {@link Ingredient#test} 检查配方的外部机器（例如沉浸工程的园艺玻璃罩）因此可以按产量给出更多产物。
 * 示例物品（JEI、配方书）为已分析的种子袋：生长 1、产量 {@code min_gain}、抗性 1。
 * 限制：把材料展开为示例物品并按物品查找配方的机器（热力系列）无法区分种子袋；这类机器改为把种子袋作为催化剂。
 * <p><b>JA:</b> 材料 {@code mio_icif:crop_seed}（0.1.8.2）：ある植物で、収量が {@code [min_gain, max_gain]} の種子袋。
 * {@link Ingredient#test} でレシピを確かめる外部機械（例：Immersive Engineering の Garden Cloche）は、収量が高いほど多く出せる。
 * 例示スタック（JEI、レシピ本）は分析済みの種子袋：成長 1、収量 {@code min_gain}、耐性 1。
 * 制限：材料を例示スタックに展開してアイテムでレシピを引く機械（Thermal シリーズ）は種子袋を区別できない。そのような機械では種子袋を触媒として使う。
 */
public record CropSeedIngredient(String plant, int minGain, int maxGain) implements ICustomIngredient {
    public static final MapCodec<CropSeedIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING.fieldOf("plant").forGetter(CropSeedIngredient::plant),
        Codec.intRange(0, 31).optionalFieldOf("min_gain", 0).forGetter(CropSeedIngredient::minGain),
        Codec.intRange(0, 31).optionalFieldOf("max_gain", 31).forGetter(CropSeedIngredient::maxGain)
    ).apply(instance, CropSeedIngredient::new));

    public static final IngredientType<CropSeedIngredient> TYPE = new IngredientType<>(CODEC);

    @Override
    public boolean test(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ICropSeedItem seed)) return false;
        String id = seed.cropModId(stack) + ":" + seed.cropId(stack);
        if (!id.equals(plant)) return false;
        int gain = seed.cropGain(stack);
        return gain >= minGain && gain <= maxGain;
    }

    @Override
    public Stream<ItemStack> getItems() {
        int split = plant.indexOf(':');
        if (split <= 0) return Stream.empty();
        ItemStack bag = CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), plant.substring(0, split), plant.substring(split + 1),
            1, minGain, 1);
        CropSeedItem.setScanLevel(bag, 4);
        return Stream.of(bag);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return TYPE;
    }

    public Ingredient toVanilla() {
        return new Ingredient(this);
    }
}
