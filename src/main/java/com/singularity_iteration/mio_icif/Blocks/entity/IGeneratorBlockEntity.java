package com.singularity_iteration.mio_icif.Blocks.entity;

import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;

/**
 * 发电机方块实体接口
 * 所有类型的发电机（EU/HU/KU）都应实现此接口
 *
 * <p>此接口扩展 {@link com.singularity_iteration.mio_icif.api.machine.IGeneratorBlock}，
 * 以 {@link CableTier} 返回类型提供电缆等级查询。
 * 新代码应优先使用 {@link com.singularity_iteration.mio_icif.api.machine.IGeneratorBlock}。
 */
public interface IGeneratorBlockEntity extends com.singularity_iteration.mio_icif.api.machine.IGeneratorBlock {

    /**
     * 获取电缆等级（内部类型返回）
     *
     * @return 电缆等级
     */
    @Override
    CableTier getCableTier();

    /**
     * 检查发电机是否正在工作
     * @return true 如果正在发电
     */
    @Override
    boolean isGenerating();

    /**
     * 获取发电速率
     * @return 每tick发电量
     */
    @Override
    long getGenerationRate();

    /**
     * 获取发电机类型ID
     * @return 类型ID
     */
    @Override
    String getGeneratorTypeId();

    /**
     * 获取发电机累计总发电量 (EU)
     * @return 总发电量
     */
    @Override
    long getTotalGenerated();
}
