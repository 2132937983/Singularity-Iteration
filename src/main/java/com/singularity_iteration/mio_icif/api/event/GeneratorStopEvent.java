package com.singularity_iteration.mio_icif.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;

/**
 * 发电机停止事件
 *
 * <p><b>触发时机</b>：当发电机从发电状态切换到停止状态时触发（例如：燃料耗尽、红石信号断开）。
 *
 * <p><b>触发频率</b>：每次发电机停止触发一次，通常在燃料耗尽、能量存储满、红石条件不满足时。
 *
 * <p><b>覆盖范围</b>：所有继承自 mio_icif_Energy_Block 的发电设备（同 GeneratorStartEvent）。
 *
 * <p><b>注意</b>：此事件不可取消。可用于记录统计数据或执行清理操作。
 *
 * <p>示例用法：
 * <pre>{@code
 * @SubscribeEvent
 * public void onGeneratorStop(GeneratorStopEvent event) {
 *     // 记录停止时间
 *     stats.recordStop(event.getPos());
 * }
 * }</pre>
 */

public class GeneratorStopEvent extends Event {

    private final Level world;
    private final BlockPos pos;
    private final long totalGenerated;

    public GeneratorStopEvent(Level world, BlockPos pos, long totalGenerated) {
        this.world = world;
        this.pos = pos;
        this.totalGenerated = totalGenerated;
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
     * 获取总发电量
     *
     * @return 本次运行的总发电量
     */
    public long getTotalGenerated() {
        return totalGenerated;
    }
}