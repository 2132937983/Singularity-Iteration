package com.singularity_iteration.mio_icif.api.upgrade.tile;

/**
 * 可升级属性枚举。
 * <p>
 * 对应 IC2 1.12.2 的 {@code UpgradableProperty}。
 * 定义了机器可以支持的升级属性类型。
 */
public enum UpgradableProperty {
    /**
     * 处理速度升级（ overclocker）。
     */
    PROCESSING,

    /**
     * 可安装增强模块（augment slot）。
     */
    AUGMENTABLE,

    /**
     * 红石信号控制（红石信号反转）。
     */
    REDSTONE_SENSITIVE,

    /**
     * 变压器升级（提高输入电压等级）。
     */
    TRANSFORMER,

    /**
     * 能量存储升级（增加储能容量）。
     */
    ENERGY_STORAGE,

    /**
     * 物品消耗（机器消耗物品）。
     */
    ITEM_CONSUMING,

    /**
     * 物品产出（机器产出物品）。
     */
    ITEM_PRODUCING,

    /**
     * 流体消耗（机器消耗流体）。
     */
    FLUID_CONSUMING,

    /**
     * 流体产出（机器产出流体）。
     */
    FLUID_PRODUCING,

    /**
     * 远程访问（远程工具支持）。
     */
    REMOTELY_ACCESSIBLE
}