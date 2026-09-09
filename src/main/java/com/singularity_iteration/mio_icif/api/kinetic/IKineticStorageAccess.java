package com.singularity_iteration.mio_icif.api.kinetic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * 动能存储访问接口
 *
 * <p>提供对动能方块的读写访问，用于查询和修改动能状态。
 * KU (Kinetic Units) 是 mio_icif 的动能单位。
 * <p>
 * 写入方法与 {@code IEnergyTileAccess} 的 {@code chargeEnergy/dischargeEnergy} 保持一致的设计风格，
 * 附属模组可通过这些方法直接向动能系统注入或抽取动能。
 */
public interface IKineticStorageAccess {

    /**
     * 获取方块所在世界
     */
    Level getWorld();

    /**
     * 获取方块位置
     */
    BlockPos getPos();

    /**
     * 获取当前存储的动能值(KU)
     */
    long getKineticStored();

    /**
     * 获取最大动能容量(KU)
     */
    long getMaxKineticStored();

    /**
     * 获取当前转速(RPM)
     */
    int getRPM();

    /**
     * 检查是否是动力源
     */
    boolean isKineticSource();

    /**
     * 检查是否是动能导体
     */
    boolean isKineticConductor();

    /**
     * 检查是否可以提取动能
     */
    boolean canExtractKinetic();

    /**
     * 检查是否可以接收动能
     */
    boolean canReceiveKinetic();

    /**
     * 向存储中注入动能。
     * <p>
     * 不会超过最大容量。如果 {@link #canReceiveKinetic()} 返回 false，则始终返回 0。
     *
     * @param amount    要注入的动能（KU），必须 ≥ 0
     * @param simulate  如果为 true，只模拟操作不实际改变存储
     * @return 实际注入的动能（KU）
     */
    long insertKinetic(long amount, boolean simulate);

    /**
     * 从存储中提取动能。
     * <p>
     * 不会超过当前存储量。如果 {@link #canExtractKinetic()} 返回 false，则始终返回 0。
     *
     * @param amount    要提取的动能（KU），必须 ≥ 0
     * @param simulate  如果为 true，只模拟操作不实际改变存储
     * @return 实际提取的动能（KU）
     */
    long extractKinetic(long amount, boolean simulate);

    /**
     * 检查是否超速。
     * 当转速 ≥ 8000 RPM 时视为超速状态。
     */
    boolean isOverspeed();

    /**
     * 获取每 tick 的动能损耗量（KU）。
     * 转速越高损耗越快。
     */
    long getKineticLossPerTick();

    /**
     * 获取最大单 tick 可接收动能速率（KU/t）。
     */
    long getMaxReceive();

    /**
     * 获取最大单 tick 可提取动能速率（KU/t）。
     */
    long getMaxExtract();

    /**
     * 直接设置动能存储量。
     * <p>
     * 值会被限制在 [0, {@link #getMaxKineticStored()}] 范围内。
     * <p>
     * <b>注意：</b>此方法通过 receiveKinetic/extractKinetic 间接实现。
     * 如果方块当前拒绝接收或提取动能（如 canReceiveKinetic() 返回 false），
     * 操作可能静默失败。调用者应在调用后通过 getKineticStored() 验证实际值。
     *
     * @param amount 新的动能值（KU）
     */
    void setKineticStored(long amount);
}