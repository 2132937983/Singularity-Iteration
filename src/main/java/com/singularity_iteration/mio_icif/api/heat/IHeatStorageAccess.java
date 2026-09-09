package com.singularity_iteration.mio_icif.api.heat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * 热能存储访问接口
 *
 * <p>提供对热能方块的读写访问，用于查询和修改热能状态。
 * HU (Heat Units) 是 mio_icif 的热能单位。
 * <p>
 * 写入方法与 {@code IEnergyTileAccess} 的 {@code chargeEnergy/dischargeEnergy} 保持一致的设计风格，
 * 附属模组可通过这些方法直接向热能系统注入或抽取热量。
 */
public interface IHeatStorageAccess {

    /**
     * 获取方块所在世界
     */
    Level getWorld();

    /**
     * 获取方块位置
     */
    BlockPos getPos();

    /**
     * 获取当前存储的热能值(HU)
     */
    long getHeatStored();

    /**
     * 获取最大热能容量(HU)
     */
    long getMaxHeatStored();

    /**
     * 获取热能温度（摄氏度）
     */
    int getTemperature();

    /**
     * 检查是否是热源
     */
    boolean isHeatSource();

    /**
     * 检查是否是热导体
     */
    boolean isHeatConductor();

    /**
     * 检查是否可以提取热能
     */
    boolean canExtractHeat();

    /**
     * 检查是否可以接收热能
     */
    boolean canReceiveHeat();

    /**
     * 向存储中注入热能。
     * <p>
     * 不会超过最大容量。如果 {@link #canReceiveHeat()} 返回 false，则始终返回 0。
     *
     * @param amount    要注入的热量（HU），必须 ≥ 0
     * @param simulate  如果为 true，只模拟操作不实际改变存储
     * @return 实际注入的热量（HU）
     */
    long insertHeat(long amount, boolean simulate);

    /**
     * 从存储中提取热能。
     * <p>
     * 不会超过当前存储量。如果 {@link #canExtractHeat()} 返回 false，则始终返回 0。
     *
     * @param amount    要提取的热量（HU），必须 ≥ 0
     * @param simulate  如果为 true，只模拟操作不实际改变存储
     * @return 实际提取的热量（HU）
     */
    long extractHeat(long amount, boolean simulate);

    /**
     * 检查是否过热。
     * 当温度 ≥ 800°C 时视为过热状态。
     */
    boolean isOverheated();

    /**
     * 获取每 tick 的热能损耗量（HU）。
     * 温度越高损耗越快。
     */
    long getHeatLossPerTick();

    /**
     * 直接设置热能存储量。
     * <p>
     * 值会被限制在 [0, {@link #getMaxHeatStored()}] 范围内。
     * <p>
     * <b>注意：</b>此方法通过 receiveHeat/extractHeat 间接实现。
     * 如果方块当前拒绝接收或提取热能（如 canReceiveHeat() 返回 false），
     * 操作可能静默失败。调用者应在调用后通过 getHeatStored() 验证实际值。
     *
     * @param amount 新的热量值（HU）
     */
    void setHeatStored(long amount);
}