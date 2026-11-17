package com.miophas.singularity_iteration.core.api.event;

import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.api.machine.IMachineAPI;
import com.miophas.singularity_iteration.core.api.machine.IMachineInfo;
import com.miophas.singularity_iteration.core.api.machine.IProducerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Machine work complete event.
 *
 * <p><b>When</b>: the event fires when a machine completes one recipe operation
 * (for example, when a compressor completes a compression or an extractor completes an extraction).
 *
 * <p><b>Frequency</b>: the event fires one time for each completed recipe.
 * The frequency depends on the machine speed and the recipe time.
 *
 * <p><b>Scope</b> (0.1.11.3): the prefab processing machines, after each operation that made an output (see API guide
 * §27.2 and §28.1). Machines that replace {@code tickProduction} and do not report their cycles do not post it.
 *
 * <p><b>Note</b>: you cannot cancel this event. In the prefab machines {@link #getOutput()} is an empty stack and
 * {@link #setOutput} has no effect: the output is already in the slots. For additional outputs use
 * {@code IWorkCycleUpgrade} and its {@code BonusOutputs}.
 *
 * <p>Example:
 * <pre>{@code
 * @SubscribeEvent
 * public void onMachineWorkComplete(MachineWorkCompleteEvent event) {
 *     IMachineInfo info = event.getMachineInfo();
 *     // count operations, play an effect, update statistics ...
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
     * Gets a read-only view of the machine information.
     */
    public IMachineInfo getMachineInfo() {
        return machineInfo;
    }

    /**
     * @deprecated Use {@link #getMachineInfo()} to get a read-only view. A later version will remove this method.
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
            return com.miophas.singularity_iteration.core.api.MioIcifAPI.instance()
                .getEnergyNetAPI().getDefaultCableTier();
        }
        @Override public ItemStack getInputItem() {
            var handler = machine.getItemHandler();
            return handler != null ? handler.getStackInSlot(0) : ItemStack.EMPTY;
        }
        @Override public ItemStack getOutputItem() { return ItemStack.EMPTY; }
        @Override public Collection<ItemStack> getInputItems() {
            com.miophas.singularity_iteration.core.api.machine.ISlotLayout layout = machine.getSlotLayout();
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