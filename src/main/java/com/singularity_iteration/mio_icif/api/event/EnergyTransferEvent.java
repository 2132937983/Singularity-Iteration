package com.singularity_iteration.mio_icif.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * 能量传输事件
 *
 * <p><b>触发时机</b>：当能量通过电缆网络从一个方块传输到另一个方块时触发。
 *
 * <p><b>触发频率</b>：每个游戏 tick 触发一次（如果有能量传输），频率极高。建议在事件处理中避免复杂计算。
 *
 * <p><b>覆盖范围</b>：所有通过 mio_icif 能量网络传输的能量，包括：
 * <ul>
 *   <li>发电机 → 电缆 → 储能设备</li>
 *   <li>储能设备 → 电缆 → 机器</li>
 *   <li>电缆 → 电缆（网络中转）</li>
 * </ul>
 *
 * <p>可通过取消事件阻止能量传输，或修改传输量。
 *
 * <p>示例用法：
 * <pre>{@code
 * @SubscribeEvent
 * public void onEnergyTransfer(EnergyTransferEvent event) {
 *     // 记录能量传输
 *     stats.recordTransfer(event.getFrom(), event.getTo(), event.getAmount());
 *
 *     // 减少传输损耗（例如：使用更好的电缆）
 *     long loss = event.getAmount() / 10; // 10% 损耗
 *     event.setAmount(event.getAmount() - loss);
 * }
 * }</pre>
 */
public class EnergyTransferEvent extends Event implements ICancellableEvent {

    private final Level world;
    private final BlockPos from;
    private final BlockPos to;
    private final Direction direction;
    private long amount;
    private final long maxExtract;

    public EnergyTransferEvent(Level world, BlockPos from, BlockPos to, Direction direction, long amount, long maxExtract) {
        this.world = world;
        this.from = from;
        this.to = to;
        this.direction = direction;
        this.amount = amount;
        this.maxExtract = maxExtract;
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
     * 获取源位置
     *
     * @return 能量来源位置
     */
    public BlockPos getFrom() {
        return from;
    }

    /**
     * 获取目标位置
     *
     * @return 能量目标位置
     */
    public BlockPos getTo() {
        return to;
    }

    /**
     * 获取传输方向
     *
     * @return 传输方向
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * 获取传输量
     *
     * @return 传输的能量值
     */
    public long getAmount() {
        return amount;
    }

    /**
     * 设置传输量
     *
     * @param amount 新的传输量
     */
    public void setAmount(long amount) {
        this.amount = Math.max(0, Math.min(amount, maxExtract));
    }

    /**
     * 获取最大可提取量
     *
     * @return 最大提取限制
     */
    public long getMaxExtract() {
        return maxExtract;
    }
}