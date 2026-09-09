package com.singularity_iteration.mio_icif.api.energy;

/**
 * 能源存储访问接口
 *
 * <p>提供对能源方块存储的查询和内部能量操作访问。
 * EU (Energy Units) 是 mio_icif 的能量单位。
 * <p>
 * 此接口将附属模组开发者与内部 {@code CustomEUEnergyStorage} 实现解耦，
 * 通过 {@link IEnergyNetAPI} 获取实例。
 * <p>
 * <b>与 {@code IMioIcifCapabilities.IEUStorage} 的关系</b>:
 * 两者表达相同的 EU 存储概念，但用途不同：
 * <ul>
 *   <li>{@code IEnergyStorageAccess} — 查询 + 内部能量操作（通过 API 获取，方法名 {@code getAmount()/getCapacity()}）</li>
 *   <li>{@code IMioIcifCapabilities.IEUStorage} — 完整读写能力（NeoForge Capability 系统，方法名 {@code getStored()/getCapacity()}）</li>
 * </ul>
 * <p>
 * <b>内部操作 vs 外部操作</b>:
 * <ul>
 *   <li>{@link #useEnergy} — 内部消耗，不受 maxExtract 限制</li>
 *   <li>{@link #generateEnergy} — 内部生成，不受 maxReceive 限制</li>
 * </ul>
 */
public interface IEnergyStorageAccess {
    
    /**
     * 获取当前存储的能量
     * @return 当前能量值（EU）
     */
    long getAmount();
    
    /**
     * 获取最大能量容量
     * @return 容量（EU）
     */
    long getCapacity();
    
    /**
     * 获取单次操作最大可接收能量
     * @return 最大接收速率（EU）
     */
    long getMaxReceive();
    
    /**
     * 获取单次操作最大可提取能量
     * @return 最大提取速率（EU）
     */
    long getMaxExtract();
    
    /**
     * 获取此存储运行的线缆等级
     * @return 线缆等级
     */
    ICableTier getCableTier();
    
    /**
     * 检查是否是电源
     * @return 如果是电源则返回 true
     */
    boolean isPowerSource();
    
    /**
     * 获取功率输出
     * @return 功率输出（EU/t）
     */
    long getPowerOutput();
    
    /**
     * 检查输出是否启用
     * @return 如果输出已启用则返回 true
     */
    boolean isOutputEnabled();
    
    /**
     * 获取功率等级
     * @return 功率等级（EU）
     */
    long getPowerRating();
    
    /**
     * 检查是否过载
     * @param gridPower 电网功率等级
     * @return 如果过载则返回 true
     */
    boolean isOverloaded(long gridPower);
    
    /**
     * 消耗能量（内部做功），不受 maxExtract 限制
     *
     * <p>适用于机器内部加工、运转等场景。
     * 与 {@code extract} 的区别：
     * <ul>
     *   <li>{@code extract} — 对外输出，受 maxExtract 限制</li>
     *   <li>{@code useEnergy} — 内部消耗，不受 maxExtract 限制</li>
     * </ul>
     *
     * @param amount 消耗量 (EU)
     * @param simulate 如果为true，仅模拟而不实际消耗
     * @return 实际消耗的能量量
     */
    long useEnergy(long amount, boolean simulate);
    
    /**
     * 生成能量（内部发电），不受 maxReceive 限制
     *
     * <p>适用于发电机内部产电等场景。
     * 与 {@code receive} 的区别：
     * <ul>
     *   <li>{@code receive} — 从外部充入，受 maxReceive 限制</li>
     *   <li>{@code generateEnergy} — 内部生成，不受 maxReceive 限制</li>
     * </ul>
     *
     * @param amount 生成量 (EU)
     * @param simulate 如果为true，仅模拟而不实际生成
     * @return 实际生成的能量量
     */
    long generateEnergy(long amount, boolean simulate);
    
    // ========== 旧版兼容方法 ==========
    
    /**
     * 旧版兼容 — 委托给 {@link #getAmount()}
     * 
     * <p><b>⚠️ 警告：</b>此方法会将 long 值截断为 int，高容量储能（如 MFE/MFS）会溢出。
     * 当能量值超过 2,147,483,647 EU 时会被静默截断为 Integer.MAX_VALUE。
     * 
     * @deprecated 请使用 {@link #getAmount()} 代替，它返回完整的 long 值。
     */
    @Deprecated
    default int getEnergyStored() {
        return (int) Math.min(getAmount(), Integer.MAX_VALUE);
    }
    
    /**
     * 旧版兼容 — 委托给 {@link #getCapacity()}
     * 
     * <p><b>⚠️ 警告：</b>此方法会将 long 值截断为 int，高容量储能（如 MFE/MFS）会溢出。
     * 当容量值超过 2,147,483,647 EU 时会被静默截断为 Integer.MAX_VALUE。
     * 
     * @deprecated 请使用 {@link #getCapacity()} 代替，它返回完整的 long 值。
     */
    @Deprecated
    default int getMaxEnergyStored() {
        return (int) Math.min(getCapacity(), Integer.MAX_VALUE);
    }
}