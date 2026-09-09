package com.singularity_iteration.mio_icif.api.machine;

/**
 * API 级槽位布局接口
 * 使附属模组开发者与内部 SlotLayout 实现解耦
 */
public interface ISlotLayout {

    /**
     * 获取指定类型的槽位数量
     * @param type 槽位类型
     * @return 槽位数量
     */
    int getCount(ISlotType type);
    
    /**
     * 获取指定类型的槽位索引
     * @param type 槽位类型
     * @return 槽位索引数组
     */
    int[] getSlotsOfType(ISlotType type);
    
    /**
     * 获取指定槽位的类型
     * @param slotIndex 槽位索引
     * @return 槽位类型
     */
    ISlotType getType(int slotIndex);
    
    /**
     * 获取总槽位数量
     * @return 总槽位数
     */
    int getTotalCount();
    
    /**
     * 获取输入槽索引
     * @return 输入槽索引数组
     */
    int[] getInputSlots();
    
    /**
     * 获取输出槽索引
     * @return 输出槽索引数组
     */
    int[] getOutputSlots();
    
    /**
     * 获取电池槽索引
     * @return 电池槽索引数组
     */
    int[] getBatterySlots();
    
    /**
     * 获取升级槽索引
     * @return 升级槽索引数组
     */
    int[] getUpgradeSlots();
    
    /**
     * 获取输入槽数量
     * @return 输入槽数量
     */
    int getInputCount();
    
    /**
     * 获取输出槽数量
     * @return 输出槽数量
     */
    int getOutputCount();
    
    /**
     * 获取电池槽数量
     * @return 电池槽数量
     */
    int getBatteryCount();
    
    /**
     * 获取升级槽数量
     * @return 升级槽数量
     */
    int getUpgradeCount();

    /**
     * 检查指定槽位是否为给定类型
     */
    default boolean isType(int slot, ISlotType type) {
        for (int s : getSlotsOfType(type)) {
            if (s == slot) return true;
        }
        return false;
    }

    /**
     * 获取指定类型的第一个槽位起始索引
     * @return 起始索引，如果没有该类型的槽位返回 -1
     */
    default int getStart(ISlotType type) {
        int[] slots = getSlotsOfType(type);
        return slots.length > 0 ? slots[0] : -1;
    }

    /**
     * 获取指定槽位的 API 级类型（等价于内部 getInternalSlotType）
     */
    default ISlotType getSlotType(int slotIndex) {
        return getType(slotIndex);
    }

    /**
     * 槽位布局构建器
     */
    interface Builder {
        Builder input(int count);
        Builder output(int count);
        Builder battery();
        Builder fluidInput(int count);
        Builder fluidOutput(int count);
        Builder upgrade(int count);
        Builder extra(int count);
        Builder rotor();
        Builder turbine();
        Builder reactor(int count);
        Builder coil();
        Builder coil(int count);
        Builder heating();
        Builder fuel();
        Builder fuel(int count);
        Builder memory();
        Builder tool();
        Builder nuclear(int count);
        Builder drill();
        Builder scanner();
        Builder miningPipe();
        Builder filter(int count);
        Builder heatConductor();
        Builder heatConductor(int count);
        Builder rtgPellet();
        Builder rtgPellet(int count);
        ISlotLayout build();
    }
}