package com.singularity_iteration.mio_icif.api.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 车床可加工物品接口
 * 实现此接口的物品可以在车床上进行加工，通过状态变化产出不同产物。
 * 参考 IC2 的 ILatheItem
 */
public interface ILatheItem {
    /**
     * 获取物品宽度（车床加工参数）
     * 如果物品在车床中加工需要旋转多个面
     */
    int getWidth(ItemStack stack);

    /**
     * 获取当前车床加工状态
     * 返回数组表示每个面的加工状态（0=未加工，1=已加工）
     * 索引范围 0~getWidth()-1
     */
    int[] getCurrentState(ItemStack stack);

    /**
     * 设置指定位置的车床加工状态
     * @param position 面索引 (0-4)
     * @param value 状态值
     */
    void setState(ItemStack stack, int position, int value);

    /**
     * 获取指定位置车床加工后的输出物品
     * @param position 面索引 (0-4)
     */
    ItemStack getOutputItem(ItemStack stack, int position);

    /**
     * 获取指定位置车床加工后的输出概率 (0.0~1.0)
     * @param position 面索引 (0-4)
     */
    float getOutputChance(ItemStack stack, int position);

    /**
     * 获取物品纹理路径
     */
    ResourceLocation getTexture(ItemStack stack);

    /**
     * 获取物品硬度（车床加工参数）
     */
    int getHardness(ItemStack stack);

    /**
     * 车床工具接口
     */
    interface ILatheTool extends ICustomDamageItem {
        /**
         * 获取工具硬度
         */
        int getHardness(ItemStack stack);
    }
}