package com.singularity_iteration.mio_icif.api.crop;

import net.minecraft.world.item.ItemStack;

import java.util.Collection;

/**
 * 作物 API
 *
 * <p>提供与 mio_icif 作物杂交系统的访问，包括：
 * <ul>
 *   <li>查询植物类型</li>
 *   <li>注册自定义植物</li>
 *   <li>获取植物属性</li>
 * </ul>
 */
public interface ICropAPI {

    /**
     * 注册自定义植物类型
     *
     * @param plantType 植物类型
     */
    void registerPlant(PlantType plantType);

    /**
     * 根据 ID 获取植物类型
     *
     * @param modId 模组 ID
     * @param typeId 类型 ID
     * @return 植物类型，如果不存在则返回 null
     */
    PlantType getPlant(String modId, String typeId);

    /**
     * 获取所有注册的植物类型
     */
    Collection<PlantType> getAllPlants();

    /**
     * 检查物品是否是基础种子（可用于作物架种植）
     *
     * @param stack 物品栈
     * @return true 如果是基础种子
     */
    boolean isBaseSeed(ItemStack stack);

    /**
     * 注册基础种子
     *
     * @param seed 种子物品
     * @param plantType 对应的植物类型
     */
    void registerBaseSeed(ItemStack seed, PlantType plantType);

    /**
     * 注册基础种子（带自定义属性）
     *
     * @param seed 种子物品
     * @param plantType 对应的植物类型
     * @param growthRate 生长速度
     * @param gain 产量
     * @param resistance 抗性
     * @param weedEx 杂草抗性（当前版本未使用，保留参数用于未来扩展）
     */
    void registerBaseSeed(ItemStack seed, PlantType plantType, int growthRate, int gain, int resistance, int weedEx);
}