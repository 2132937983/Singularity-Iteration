package com.singularity_iteration.mio_icif.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * 能量生成事件
 *
 * <p><b>触发时机</b>：当发电机（包括核反应堆、太阳能板、风力发电机等）生成能量时触发。
 *
 * <p><b>触发频率</b>：每个游戏 tick 触发一次（如果发电机正在工作），频率高。
 *
 * <p><b>覆盖范围</b>：所有继承自 mio_icif_Energy_Block 的发电设备，包括：
 * <ul>
 *   <li>核反应堆、流体反应堆</li>
 *   <li>太阳能板、风力发电机</li>
 *   <li>地热发电机、RTG 发电机</li>
 *   <li>其他所有发电机器</li>
 * </ul>
 *
 * <p>可通过取消事件阻止能量生成，或修改生成量。
 *
 * <p>示例用法：
 * <pre>{@code
 * @SubscribeEvent
 * public void onEnergyGenerated(EnergyGenerationEvent event) {
 *     // 记录发电量
 *     stats.recordGeneration(event.getPos(), event.getAmount());
 *
 *     // 修改发电量（例如：升级插件效果）
 *     event.setAmount(event.getAmount() * 2);
 * }
 * }</pre>
 */
public class EnergyGenerationEvent extends Event implements ICancellableEvent {

    private final Level world;
    private final BlockPos pos;
    private long amount;
    private final long maxCapacity;
    private final long currentStored;

    public EnergyGenerationEvent(Level world, BlockPos pos, long amount, long maxCapacity, long currentStored) {
        this.world = world;
        this.pos = pos;
        this.amount = amount;
        this.maxCapacity = maxCapacity;
        this.currentStored = currentStored;
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
     * @return 方块位置
     */
    public BlockPos getPos() {
        return pos;
    }

    /**
     * 获取发电量
     *
     * @return 生成的能量值
     */
    public long getAmount() {
        return amount;
    }

    /**
     * 设置发电量
     *
     * @param amount 新的发电量
     */
    public void setAmount(long amount) {
        // 确保发电量不会超过储能上限
        long space = maxCapacity - currentStored;
        this.amount = Math.min(amount, Math.max(0, space));
    }

    /**
     * 获取最大容量
     *
     * @return 最大能量容量
     */
    public long getMaxCapacity() {
        return maxCapacity;
    }

    /**
     * 获取当前存储量
     *
     * @return 当前存储的能量
     */
    public long getCurrentStored() {
        return currentStored;
    }
}