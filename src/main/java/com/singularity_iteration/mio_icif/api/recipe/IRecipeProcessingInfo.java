package com.singularity_iteration.mio_icif.api.recipe;

/**
 * 配方处理信息接口
 *
 * <p>所有需要暴露处理时间和能耗信息的配方都应实现此接口。
 * 通过实现此接口，可以避免使用反射访问配方属性，确保在混淆环境下正常工作。
 *
 * <p>实现示例：
 * <pre>{@code
 * public class MyCustomRecipe implements Recipe<MyInventory>, IRecipeProcessingInfo {
 *     private final int processingTime;
 *     private final int energyPerTick;
 *
 *     @Override
 *     public int getProcessingTime() {
 *         return processingTime;
 *     }
 *
 *     @Override
 *     public int getEnergyPerTick() {
 *         return energyPerTick;
 *     }
 * }
 * }</pre>
 */
public interface IRecipeProcessingInfo {

    /**
     * 获取配方处理时间（tick）
     *
     * @return 处理时间，单位 tick
     */
    int getProcessingTime();

    /**
     * 获取配方每 tick 能耗（EU/tick）
     *
     * @return 每 tick 能耗，单位 EU/tick
     */
    int getEnergyPerTick();

    /**
     * 获取配方总能耗（EU）
     *
     * <p>默认实现为 getEnergyPerTick() * getProcessingTime()
     *
     * @return 总能耗，单位 EU
     */
    default long getTotalEnergyCost() {
        return (long) getEnergyPerTick() * getProcessingTime();
    }

    /**
     * 获取配方输入物品消耗数量
     *
     * <p>默认返回1，表示每次加工消耗1个输入物品。
     * 金属成型机等配方可能需要消耗多个输入物品。
     *
     * @return 消耗数量
     */
    default int getIngredientCount() {
        return 1;
    }
}