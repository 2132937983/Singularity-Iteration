package com.singularity_iteration.mio_icif.api.machine;

/**
 * KU 动能发电机接口。
 * <p>
 * 为 KU 动能发电机提供统一的运行时 API。
 * 实现此接口的方块可以通过动能系统向相邻机器提供动能。
 * <p>
 * 注意：此接口独立于 {@link IEnergyBlock}（EU 系统），
 * 因为 KU 动能是与 EU 电力并行的独立能量系统。
 */
public interface IKineticGeneratorBlock {

    /**
     * 获取当前动能输出速率（KU/tick）。
     *
     * @return 当前动能输出速率，0 表示未发电
     */
    int getKineticOutput();

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
     * 获取动能生成速率（KU/tick）。
     *
     * @return 动能生成速率
     */
    int getKineticGenerationRate();

    /**
     * 获取当前转子转速（RPM）。
     *
     * @return 当前转速
     */
    int getRotorRPM();
}
