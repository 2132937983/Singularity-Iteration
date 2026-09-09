package com.singularity_iteration.mio_icif.api.event;

import com.singularity_iteration.mio_icif.api.machine.IProducerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * 升级插件插入事件
 *
 * <p><b>触发时机</b>：当玩家或自动化系统将升级插件插入机器的升级槽时触发。
 *
 * <p><b>触发频率</b>：每次插入操作触发一次，通常在玩家点击升级槽或管道/漏斗插入物品时。
 *
 * <p><b>覆盖范围</b>：所有实现 IProducerBlock 接口的机器类型，包括：
 * <ul>
 *   <li>压缩机、提取机、粉碎机、离心机等基础机器</li>
 *   <li>焊接机、精密组装机等高级机器</li>
 *   <li>核反应堆、流体反应堆等发电设备</li>
 * </ul>
 *
 * <p>可通过取消事件阻止插入。
 *
 * <p>示例用法：
 * <pre>{@code
 * @SubscribeEvent
 * public void onUpgradeInsert(MachineUpgradeInsertEvent event) {
 *     // 阻止特定升级插件插入
 *     if (event.getUpgrade().getItem() == MyModItems.FORBIDDEN_UPGRADE.get()) {
 *         event.setCanceled(true);
 *     }
 * }
 * }</pre>
 */
public class MachineUpgradeInsertEvent extends Event implements ICancellableEvent {

    private final Level level;
    private final BlockPos pos;
    private final IProducerBlock machine;
    private final int slot;
    private ItemStack upgrade;

    public MachineUpgradeInsertEvent(Level level, BlockPos pos, IProducerBlock machine, int slot, ItemStack upgrade) {
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

    /**
     * 替换升级插件物品
     */
    public void setUpgrade(ItemStack upgrade) {
        this.upgrade = upgrade;
    }
}