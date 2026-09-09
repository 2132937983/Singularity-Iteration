package com.singularity_iteration.mio_icif.api.machine;

/**
 * HU 热能发电机接口。
 * <p>
 * 为 HU 热能发电机提供统一的运行时 API。
 * 实现此接口的方块可以通过热能系统向相邻机器提供热能。
 * <p>
 * 注意：此接口独立于 {@link IEnergyBlock}（EU 系统），
 * 因为 HU 热能是与 EU 电力并行的独立能量系统。
 */
public interface IHeatGeneratorBlock {

    /**
     * 获取当前热能输出速率（HU/tick）。
     *
     * @return 当前热能输出速率，0 表示未发电
     */
    int getHeatOutput();

    /**
     * 检查发电机是否正在发电（燃烧中）。
     *
     * @return true 如果正在发电
     */
    boolean isGenerating();

    /**
     * 获取燃料剩余燃烧时间（tick）。
     *
     * @return 剩余燃烧时间
     */
    int getBurnTime();

    /**
     * 获取当前燃料的总燃烧时间（tick）。
     *
     * @return 总燃烧时间
     */
    int getBurnDuration();

    /**
     * 获取热能生成速率（HU/tick）。
     *
     * @return 热能生成速率
     */
    int getHeatGenerationRate();
}
