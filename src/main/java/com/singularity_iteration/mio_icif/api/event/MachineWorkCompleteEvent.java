package com.singularity_iteration.mio_icif.api.event;

import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.api.machine.IMachineAPI;
import com.singularity_iteration.mio_icif.api.machine.IMachineInfo;
import com.singularity_iteration.mio_icif.api.machine.IProducerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 机器完成工作事件
 *
 * <p><b>触发时机</b>：当机器完成一次配方加工后触发（例如：压缩机完成压缩、提取机完成提取）。
 *
 * <p><b>触发频率</b>：每次配方完成触发一次，频率取决于机器加工速度和配方时间。
 *
 * <p><b>覆盖范围</b>：所有实现 IProducerBlock 接口的机器类型，包括：
 * <ul>
 *   <li>压缩机、提取机、粉碎机、离心机等基础机器</li>
 *   <li>焊接机、精密组装机等多原料机器</li>
 *   <li>核反应堆（每次循环完成时）</li>
 * </ul>
 *
 * <p><b>注意</b>：此事件不可取消。可用于修改输出物品或执行额外操作。
 *
 * <p>示例用法：
 * <pre>{@code
 * @SubscribeEvent
 * public void onMachineWorkComplete(MachineWorkCompleteEvent event) {
 *     IMachineInfo info = event.getMachineInfo();
 *     ItemStack output = event.getOutput();
 *     if (output.getItem() == Items.IRON_INGOT) {
 *         if (event.getLevel().random.nextFloat() < 0.1f) {
 *             output.grow(1);
 *         }
 *     }
 * }
 * }</pre>
 */
public class MachineWorkCompleteEvent extends Event {

    private final Level level;
    private final BlockPos pos;
    private final IProducerBlock machine;
    private final IMachineInfo machineInfo;
    private ItemStack output;

    public MachineWorkCompleteEvent(Level level, BlockPos pos, IProducerBlock machine, ItemStack output) {
        this.level = level;
        this.pos = pos;
        this.machine = machine;
        this.machineInfo = new ProducerMachineInfoAdapter(level, pos, machine);
        this.output = output;
    }

    public Level getLevel() {
        return level;
    }

    public BlockPos getPos() {
        return pos;
    }

    /**
     * 获取机器只读信息视图
     */
    public IMachineInfo getMachineInfo() {
        return machineInfo;
    }

    /**
     * @deprecated 使用 {@link #getMachineInfo()} 获取只读视图。此方法将在后续版本移除。
     */
    @Deprecated
    public IProducerBlock getMachine() {
        return machine;
    }

    public ItemStack getOutput() {
        return output;
    }

    public void setOutput(ItemStack output) {
        this.output = output;
    }

    static final class ProducerMachineInfoAdapter implements IMachineInfo {
        private final Level level;
        private final BlockPos pos;
        private final IProducerBlock machine;

        ProducerMachineInfoAdapter(Level level, BlockPos pos, IProducerBlock machine) {
            this.level = level;
            this.pos = pos;
            this.machine = machine;
        }

        @Override public Level getWorld() { return level; }
        @Override public BlockPos getPos() { return pos; }
        @Override public IMachineAPI.MachineType getMachineType() { return IMachineAPI.MachineType.PROCESSOR; }
        @Override public long getStoredEnergy() {
            return machine.getEnergyStorage() != null ? machine.getEnergyStorage().getAmount() : 0;
        }
        @Override public long getMaxEnergy() {
            return machine.getEnergyStorage() != null ? machine.getEnergyStorage().getCapacity() : 0;
        }
        @Override public int getProgress() { return machine.getProgress(); }
        @Override public int getMaxProgress() { return machine.getMaxProgress(); }
        @Override public boolean isWorking() { return machine.isWorking(); }
        @Override public long getEnergyPerTick() { return machine.getEffectiveEnergyPerTick(); }
        @Override public long getBaseEnergyPerTick() { return machine.getEnergyPerTick(); }
        @Override public ICableTier getCableTier() {
            var storage = machine.getEnergyStorage();
            if (storage != null && storage.getCableTier() != null) {
                return storage.getCableTier();
            }
            return com.singularity_iteration.mio_icif.api.MioIcifAPI.instance()
                .getEnergyNetAPI().getDefaultCableTier();
        }
        @Override public ItemStack getInputItem() {
            var handler = machine.getItemHandler();
            return handler != null ? handler.getStackInSlot(0) : ItemStack.EMPTY;
        }
        @Override public ItemStack getOutputItem() { return ItemStack.EMPTY; }
        @Override public Collection<ItemStack> getInputItems() {
            com.singularity_iteration.mio_icif.api.machine.ISlotLayout layout = machine.getSlotLayout();
            net.neoforged.neoforge.items.IItemHandler handler = machine.getItemHandler();
            if (handler == null || layout == null) return Collections.emptyList();
            int[] inputSlots = layout.getInputSlots();
            List<ItemStack> items = new ArrayList<>(inputSlots.length);
            for (int slot : inputSlots) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty()) items.add(stack);
            }
            return items;
        }
        @Override public Collection<ItemStack> getOutputItems() { return Collections.emptyList(); }
        @Override public long getRuntimeTicks() { return 0; }
        @Override public long getTotalProcessed() { return machine.getTotalProcessed(); }
        @Override public double getSpeedMultiplier() { return machine.getUpgradeStats() != null ? machine.getUpgradeStats().getProcessTimeMultiplier() : 1.0; }
        @Override public double getEnergyMultiplier() { return machine.getUpgradeStats() != null ? machine.getUpgradeStats().getEnergyUsageMultiplier() : 1.0; }
        @Override public boolean supportsUpgrades() { return machine.getUpgradeSlotCount() > 0; }
        @Override public Collection<ItemStack> getInstalledUpgrades() {
            return machine.getUpgrades() != null ? List.copyOf(machine.getUpgrades()) : Collections.emptyList();
        }
    }
}