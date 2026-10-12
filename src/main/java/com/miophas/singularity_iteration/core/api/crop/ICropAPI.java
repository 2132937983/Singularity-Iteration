package com.miophas.singularity_iteration.core.api.crop;

import net.minecraft.world.item.ItemStack;

import java.util.Collection;

/**
 * <p><b>EN:</b> Crop API: plant types and base seeds of the crop-stick breeding system. Register
 * during mod construction or a setup event, on both sides.
 * <p><b>ZH:</b> 作物 API：作物架杂交系统的植物类型与基础种子。请在模组构造或初始化事件期间、在两端注册。
 * <p><b>JA:</b> 作物 API：作物棒の交配システムの植物タイプと基本種子。MOD の構築中または初期化イベントで、両側で登録する。
 */
public interface ICropAPI {

    /**
     * <p><b>EN:</b> Registers a plant type. A second plant with the same "modId:typeId" replaces
     * the first one and logs a warning.
     * <p><b>ZH:</b> 注册植物类型。相同 “modId:typeId” 的第二个植物会替换第一个，并记录警告。
     * <p><b>JA:</b> 植物タイプを登録する。同じ「modId:typeId」の 2 つ目は 1 つ目を置き換え、警告を記録する。
     *
     * @param plantType the plant type
     */
    void registerPlant(PlantType plantType);

    /**
     * <p><b>EN:</b> Returns the plant type with this id, or null.
     * <p><b>ZH:</b> 返回该 ID 的植物类型，不存在时返回 null。
     * <p><b>JA:</b> この ID の植物タイプを返す。なければ null。
     *
     * @param modId the mod ID
     * @param typeId the type ID
     * @return the plant type, or null if it does not exist
     */
    PlantType getPlant(String modId, String typeId);

    /**
     * <p><b>EN:</b> Returns all registered plant types (read-only view).
     * <p><b>ZH:</b> 返回所有已注册的植物类型（只读视图）。
     * <p><b>JA:</b> 登録済みのすべての植物タイプ（読み取り専用ビュー）を返す。
     */
    Collection<PlantType> getAllPlants();

    /**
     * <p><b>EN:</b> True when the item is a registered base seed (it can be planted on crop sticks).
     * The stack count is ignored.
     * <p><b>ZH:</b> 物品是已注册的基础种子（可种在作物架上）时为真。忽略物品数量。
     * <p><b>JA:</b> アイテムが登録済みの基本種子（作物棒に植えられる）なら真。スタック数は無視する。
     *
     * @param stack the item stack
     * @return true if the item is a base seed
     */
    boolean isBaseSeed(ItemStack stack);

    /**
     * <p><b>EN:</b> Registers a base seed with the traits 0 / 0 / 0. The plant starts at stage 1.
     * The stack count of {@code seed} is the number of items that one planting uses.
     * <p><b>ZH:</b> 以属性 0 / 0 / 0 注册基础种子，植物从阶段 1 开始。{@code seed} 的数量为每次种植消耗的物品数。
     * <p><b>JA:</b> 特性値 0 / 0 / 0 で基本種子を登録する。植物は段階 1 から始まる。{@code seed} のスタック数が 1 回の植え付けで使う個数になる。
     *
     * @param seed the seed item
     * @param plantType the related plant type
     */
    void registerBaseSeed(ItemStack seed, PlantType plantType);

    /**
     * <p><b>EN:</b> Registers a base seed with set traits (each 0..31).
     * <p><b>ZH:</b> 以指定属性（各 0..31）注册基础种子。
     * <p><b>JA:</b> 指定した特性値（各 0..31）で基本種子を登録する。
     *
     * @param seed the seed item
     * @param plantType the related plant type
     * @param growthRate the growth rate
     * @param gain the yield
     * @param resistance the resistance
     * @param weedEx the weed resistance (not used in this version; kept for future extension)
     */
    void registerBaseSeed(ItemStack seed, PlantType plantType, int growthRate, int gain, int resistance, int weedEx);
}
