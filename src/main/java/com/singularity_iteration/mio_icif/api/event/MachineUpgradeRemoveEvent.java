package com.singularity_iteration.mio_icif.api.event;

import com.singularity_iteration.mio_icif.api.machine.IProducerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * 升级插件移除事件
 *
 * <p><b>触发时机</b>：当玩家或自动化系统从机器的升级槽中取出升级插件时触发。
 *
 * <p><b>触发频率</b>：每次移除操作触发一次，通常在玩家点击升级槽或管道/漏斗取出物品时。
 *
 * <p><b>覆盖范围</b>：所有实现 IProducerBlock 接口的机器类型（同 MachineUpgradeInsertEvent）。
 *
 * <p>可通过取消事件阻止移除。
 *
 * <p>示例用法：
 * <pre>{@code
 * @SubscribeEvent
 * public void onUpgradeRemove(MachineUpgradeRemoveEvent event) {
 *     // 阻止升级插件被移除（例如：机器工作时锁定升级）
 *     if (event.getMachine().isWorking()) {
 *         event.setCanceled(true);
 *     }
 * }
 * }</pre>
 */
public class MachineUpgradeRemoveEvent extends Event implements ICancellableEvent {

    private final Level level;
    private final BlockPos pos;
    private final IProducerBlock machine;
    private final int slot;
    private final ItemStack upgrade;

    public MachineUpgradeRemoveEvent(Level level, BlockPos pos, IProducerBlock machine, int slot, ItemStack upgrade) {
        this.level = level;
        this.pos = pos;
        this.machine = machine;
        this.slot = slot;
        this.upgrade = upgrade;
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

    /**
     * 获取升级槽位索引
     */
    public int getSlot() {
        return slot;
    }

    /**
     * 获取升级插件物品
     */
    public ItemStack getUpgrade() {
        return upgrade;
    }
}