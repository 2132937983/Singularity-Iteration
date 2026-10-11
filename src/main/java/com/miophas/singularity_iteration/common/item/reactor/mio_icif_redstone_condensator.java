package com.miophas.singularity_iteration.common.item.reactor;

/**
 * Redstone condensator.
 *
 * Properties:
 * - It absorbs up to 20,000 heat from nearby components.
 * - Other components, such as heat vents, cannot cool it.
 * - Craft it with redstone in a crafting table to repair it.
 * - Each redstone repairs 10000 points of durability.
 * - It shows its durability while it works.
 * - When it is full of heat, it stops absorbing heat. It is not destroyed.
 */
@SuppressWarnings("null")
public class mio_icif_redstone_condensator extends mio_icif_condensator {

    // Maximum heat storage
public static final int MAX_HEAT = 20000;

    // Heat absorbed per tick
public static final int ABSORPTION_RATE = 2; // 每tick吸收2点（已翻倍）

    // Durability that one redstone repairs
public static final int REDSTONE_REPAIR = 10000;

    // 青金石修复量（红石冷凝模块不使用青金石修复）
    public static final int LAPIS_REPAIR = 0;

    /**
     * 构造函数
 * @param properties 物品属性
 */
    public mio_icif_redstone_condensator(Properties properties) {
        super(properties, MAX_HEAT, ABSORPTION_RATE, REDSTONE_REPAIR, LAPIS_REPAIR);
    }
}


