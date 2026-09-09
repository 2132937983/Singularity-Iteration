package com.singularity_iteration.mio_icif.api.event;

import com.singularity_iteration.mio_icif.api.machine.IProducerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * 机器开始工作事件
 *
 * <p><b>触发时机</b>：当机器从空闲状态切换到工作状态时触发（例如：放入原料后机器开始加工）。
 *
 * <p><b>触发频率</b>：每次机器启动触发一次，通常在放入原料、能量充足、红石条件满足时。
 *
 * <p><b>覆盖范围</b>：所有实现 IProducerBlock 接口的机器类型，包括：
 * <ul>
 *   <li>压缩机、提取机、粉碎机、离心机等基础机器</li>
 *   <li>焊接机、精密组装机等多原料机器</li>
 *   <li>核反应堆（每次循环开始时）</li>
 * </ul>
 *
 * <p>可通过取消事件阻止机器开始工作。
 *
 * <p>示例用法：
 * <pre>{@code
 * @SubscribeEvent
 * public void onMachineWorkStart(MachineWorkStartEvent event) {
 *     // 记录机器启动
 *     logger.info("Machine started working at " + event.getPos());
 *
 *     // 阻止特定条件下的工作
 *     if (event.getLevel().isRainingAt(event.getPos().above())) {
 *         event.setCanceled(true);
 *     }
 * }
 * }</pre>
 */
public class MachineWorkStartEvent extends Event implements ICancellableEvent {

    private final Level level;
    private final BlockPos pos;
    private final IProducerBlock machine;

    public MachineWorkStartEvent(Level level, BlockPos pos, IProducerBlock machine) {
        this.level = level;
        this.pos = pos;
        this.machine = machine;
    }

    /**
     * 获取世界
     */
    public Level getLevel() {
        return level;
    }

    /**
     * 获取机器位置
     */
    public BlockPos getPos() {
        return pos;
    }

    /**
     * 获取机器实例
     */
    public IProducerBlock getMachine() {
        return machine;
    }
}