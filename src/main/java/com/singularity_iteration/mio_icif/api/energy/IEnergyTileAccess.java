package com.singularity_iteration.mio_icif.api.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * 能源方块访问接口
 *
 * <p>提供对电网中方块的访问，包括只读查询和写入操作。
 */
public interface IEnergyTileAccess {

    /**
     * 获取方块所在世界
     *
     * @return 世界对象
     */
    Level getWorld();

    /**
     * 获取方块位置
     *
     * @return 方块位置
     */
    BlockPos getPos();

    /**
     * 获取当前存储的能量
     *
     * @return 当前能量值
     */
    long getStoredEnergy();

    /**
     * 获取最大能量容量
     *
     * @return 最大容量
     */
    long getMaxEnergy();

    /**
     * 获取当前存储的能量（与 {@link #getStoredEnergy()} 等价）
     *
     * @return 当前能量值 (EU)
     */
    default long getStored() {
        return getStoredEnergy();
    }

    /**
     * 获取最大能量容量（与 {@link #getMaxEnergy()} 等价）
     *
     * @return 容量 (EU)
     */
    default long getCapacity() {
        return getMaxEnergy();
    }

    /**
     * 获取当前提供的能量（仅电源）
     *
     * @return 提供的能量值 (EU)
     */
    long getOfferedEnergy();

    /**
     * 获取当前需求的能量（仅用电器）
     *
     * @return 需求的能量值 (EU)
     */
    long getDemandedEnergy();

    /**
     * 获取源等级（仅电源）
     *
     * @return 源等级
     */
    int getSourceTier();

    /**
     * 获取接收等级（仅用电器）
     *
     * @return 接收等级
     */
    int getSinkTier();

    /**
     * 获取电缆等级
     *
     * <p>对于非电缆方块（如电源、用电器），此方法返回空 Optional。
     *
     * @return 电缆等级（如果该方块没有电缆等级概念则返回空）
     */
    Optional<ICableTier> getCableTier();

    /**
     * 检查是否接受来自指定方向的能量
     *
     * @param direction 方向
     * @return true 如果接受该方向的能量
     */
    boolean acceptsEnergyFrom(Direction direction);

    /**
     * 检查是否向指定方向发射能量
     *
     * @param direction 方向
     * @return true 如果向该方向发射能量
     */
    boolean emitsEnergyTo(Direction direction);

    /**
     * 检查是否是电源
     *
     * @return true 如果是电源
     */
    boolean isSource();

    /**
     * 检查是否是用电器
     *
     * @return true 如果是用电器
     */
    boolean isSink();

    /**
     * 检查是否是导体（电缆）
     *
     * @return true 如果是导体
     */
    boolean isConductor();

    // ========== 能源写入 API ==========

    /**
     * 向能源方块充能
     *
     * @param amount 充能量 (EU)
     * @param simulate 如果为true，仅模拟而不实际充能
     * @return 实际充入的能量量
     */
    long chargeEnergy(long amount, boolean simulate);

    /**
     * 从能源方块取能（对外输出）
     *
     * <p>受 {@code maxExtract} 限制，适用于电网输出、外部取能等场景。
     * 对于 {@code maxExtract=0} 的方块（如加工机器），此方法始终返回 0。
     *
     * @param amount 取能量 (EU)
     * @param simulate 如果为true，仅模拟而不实际取能
     * @return 实际取出的能量量
     */
    long dischargeEnergy(long amount, boolean simulate);

    /**
     * 消耗能源方块的能量（内部做功）
     *
     * <p>不受 {@code maxExtract} 限制，适用于机器内部加工、运转等场景。
     * 与 {@link #dischargeEnergy} 的区别：
     * <ul>
     *   <li>{@code dischargeEnergy} — 对外输出，受 {@code maxExtract} 限制</li>
     *   <li>{@code useEnergy} — 内部消耗，不受 {@code maxExtract} 限制</li>
     * </ul>
     *
     * @param amount 消耗量 (EU)
     * @param simulate 如果为true，仅模拟而不实际消耗
     * @return 实际消耗的能量量
     */
    long useEnergy(long amount, boolean simulate);

    /**
     * 向能源方块内部生成能量（内部发电）
     *
     * <p>不受 {@code maxReceive} 限制，适用于发电机内部产电等场景。
     * 与 {@link #chargeEnergy} 的区别：
     * <ul>
     *   <li>{@code chargeEnergy} — 从外部充入，受 {@code maxReceive} 限制</li>
     *   <li>{@code generateEnergy} — 内部生成，不受 {@code maxReceive} 限制</li>
     * </ul>
     *
     * @param amount 生成量 (EU)
     * @param simulate 如果为true，仅模拟而不实际生成
     * @return 实际生成的能量量
     */
    long generateEnergy(long amount, boolean simulate);

    /**
     * 设置能源方块的能量（创造模式/命令）
     *
     * @param amount 要设置的能量值
     * @return true 如果设置成功，false 如果此方块不支持设置能量
     */
    boolean setEnergy(long amount);

    /**
     * 修改能源方块的容量（升级系统）
     *
     * @param capacity 新的容量值
     * @return true 如果设置成功，false 如果此方块不支持修改容量
     */
    boolean setCapacity(long capacity);

    /**
     * 获取最大输入速率
     *
     * @return 最大输入速率 (EU/tick)
     */
    long getMaxReceive();

    /**
     * 获取最大输出速率
     *
     * @return 最大输出速率 (EU/tick)
     */
    long getMaxExtract();
}