package com.singularity_iteration.mio_icif.api.energy;

/**
 * 单个 Tile 的能量流统计
 * 
 * <p>这是 API 暴露的 NodeStats 版本，供附属模组开发者安全使用。
 * 
 * @param energyIn 输入能量 (EU)
 * @param energyOut 输出能量 (EU)
 * @param voltage 电压 (EU/packet)
 */
public record NodeStats(long energyIn, long energyOut, double voltage) {
}