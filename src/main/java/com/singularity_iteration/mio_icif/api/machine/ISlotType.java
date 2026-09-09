package com.singularity_iteration.mio_icif.api.machine;

/**
 * API 级槽位类型接口
 * 使附属模组开发者与内部 SlotType 实现解耦
 */
public interface ISlotType {
    
    /**
     * 获取槽位类型名称
     * @return 类型名称
     */
    String getName();
    
    /**
     * 检查是否为输入槽
     * @return 如果是输入槽返回 true
     */
    boolean isInput();
    
    /**
     * 检查是否为输出槽
     * @return 如果是输出槽返回 true
     */
    boolean isOutput();
    
    /**
     * 检查是否为电池槽
     * @return 如果是电池槽返回 true
     */
    boolean isBattery();
    
    /**
     * 检查是否为升级槽
     * @return 如果是升级槽返回 true
     */
    boolean isUpgrade();
    
    /**
     * 检查是否为额外槽
     * @return 如果是额外槽返回 true
     */
    boolean isExtra();
}