package com.singularity_iteration.mio_icif.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * 发电机启动事件
 *
 * <p><b>触发时机</b>：当发电机从停止状态切换到发电状态时触发（例如：核反应堆放入燃料棒后启动）。
 *
 * <p><b>触发频率</b>：每次发电机启动触发一次，通常在放入燃料、能量条件满足、红石信号允许时。
 *
 * <p><b>覆盖范围</b>：所有继承自 mio_icif_Energy_Block 的发电设备，包括：
 * <ul>
 *   <li>核反应堆、流体反应堆</li>
 *   <li>太阳能板（光照条件满足时）</li>
 *   <li>风力发电机（有风时）</li>
 *   <li>地热发电机、RTG 发电机等</li>
 * </ul>
 *
 * <p>可通过取消事件阻止发电机启动。
 *
 * <p>示例用法：
 * <pre>{@code
 * @SubscribeEvent
 * public void onGeneratorStart(GeneratorStartEvent event) {
 *     // 检查是否是白天
 *     if (event.getWorld().isDay()) {
 *         // 允许发电
 *     } else {
 *         // 取消发电
 *         event.setCanceled(true);
 *     }
 * }
 * }</pre>
 */

public class GeneratorStartEvent extends Event implements ICancellableEvent {

    private final Level world;
    private final BlockPos pos;
    private final long generationRate;

    public GeneratorStartEvent(Level world, BlockPos pos, long generationRate) {
        this.world = world;
        this.pos = pos;
        this.generationRate = generationRate;
    }

    /**
     * 获取世界
     *
     * @return 世界对象
     */
    public Level getWorld() {
        return world;
    }

    /**
     * 获取位置
     *
     * @return 发电机位置
     */
    public BlockPos getPos() {
        return pos;
    }

    /**
     * 获取发电速率
     *
     * @return 发电速率 (EU/t)
     */
    public long getGenerationRate() {
        return generationRate;
    }
}