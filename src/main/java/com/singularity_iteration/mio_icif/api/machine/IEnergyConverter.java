package com.singularity_iteration.mio_icif.api.machine;

import com.singularity_iteration.mio_icif.api.energy.ICableTier;

/**
 * 能量转换器接口。
 *
 * <p>用于描述在不同能量类型之间进行转换的方块，
 * 如 EU ↔ FE（Forge Energy）、EU ↔ KU（Kinetic Units）等。
 *
 * <h3>能量类型</h3>
 * <ul>
 *   <li>{@link EnergyType#EU}  — IndustrialCraft 能量单位</li>
 *   <li>{@link EnergyType#FE}  — Forge Energy（通用能量）</li>
 *   <li>{@link EnergyType#KU}  — 动能单位（Kinetic Units）</li>
 *   <li>{@link EnergyType#HE}  — 热能单位（Heat Energy）</li>
 * </ul>
 */
public interface IEnergyConverter extends IEnergyBlock {

    /**
     * 能量类型枚举
     */
    enum EnergyType {
        /** EU — IndustrialCraft 能量 */
        EU,
        /** FE — Forge Energy */
        FE,
        /** KU — Kinetic Units */
        KU,
        /** HE — Heat Energy */
        HE,
        /** 自定义类型 */
        CUSTOM
    }

    /**
     * 获取输入侧的能量类型
     * @return 输入能量类型
     */
    EnergyType getInputType();

    /**
     * 获取输出侧的能量类型
     * @return 输出能量类型
     */
    EnergyType getOutputType();

    /**
     * 获取转换比率
     * <p>例如：EU → FE 的比率为 4.0 表示 1 EU = 4 FE
     * @return 转换比率（输出/输入）
     */
    double getConversionRatio();

    /**
     * 获取输入侧电缆等级
     * @return 输入侧等级
     */
    ICableTier getInputCableTier();

    /**
     * 获取输出侧电缆等级
     * @return 输出侧等级
     */
    ICableTier getOutputCableTier();

    /**
     * 获取转换效率（0.0 - 1.0）
     * @return 效率
     */
    default double getEfficiency() {
        return 1.0;
    }

    /**
 * 获取内部冲区当前能量（以输出类型计）
     * @return 褰撳墠缂撳啿鑳介噺
     */
    long getBufferAmount();

    /**
 * 获取内部冲区容量（以输出类型计）
 * @return 冲容量
     */
    long getBufferCapacity();
}
