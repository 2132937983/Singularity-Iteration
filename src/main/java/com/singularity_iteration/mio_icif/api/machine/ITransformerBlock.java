package com.singularity_iteration.mio_icif.api.machine;

import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import net.minecraft.core.Direction;

/**
 * 变压器方块接口。
 *
 * <p>变压器是核心电力基础设施，用于在不同电压等级之间转换能量。
 * 实现此接口的方块可以将低压侧的能量升压到高压侧，或将高压侧的能量降压到低压侧。
 *
 * <h3>工作模式</h3>
 * <ul>
 *   <li>{@link TransformerMode#STEP_UP}   — 升压模式：低压侧输入 → 高压侧输出</li>
 *   <li>{@link TransformerMode#STEP_DOWN} — 降压模式：高压侧输入 → 低压侧输出</li>
 * </ul>
 */
public interface ITransformerBlock extends IEnergyBlock {

    /**
     * 变压器工作模式枚举
     */
    enum TransformerMode {
        /** 升压模式：低压 → 高压 */
        STEP_UP,
        /** 降压模式：高压 → 低压 */
        STEP_DOWN
    }

    /**
     * 获取当前变压器模式
     * @return 变压器模式
     */
    TransformerMode getTransformerMode();

    /**
     * 设置变压器模式
     * @param mode 目标模式
     */
    void setTransformerMode(TransformerMode mode);

    /**
     * 获取低压侧电缆等级
     * @return 低压侧等级
     */
    ICableTier getLowTier();

    /**
     * 获取高压侧电缆等级
     * @return 高压侧等级
     */
    ICableTier getHighTier();

    /**
     * 获取当前输入侧的电缆等级
     * @return 输入侧等级
     */
    ICableTier getInputTier();

    /**
     * 获取当前输出侧的电缆等级
     * @return 输出侧等级
     */
    ICableTier getOutputTier();

    /**
 * 获取内部冲区当前能量
     * @return 褰撳墠缂撳啿鑳介噺锛圗U锛�
     */
    long getInternalBuffer();

    /**
 * 获取内部冲区容量
 * @return 冲容量（EU）
     */
    long getBufferCapacity();

    /**
     * 检查指定方向是否连接到变压器
     * @param side 方向
     * @return true 如果该方向已连接
     */
    boolean isConnected(Direction side);
}
