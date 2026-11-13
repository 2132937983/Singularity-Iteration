// SPDX-License-Identifier: Apache-2.0
// SCEX: discrete processing for basic machines, validated against ordinary IC2 game observations.
package com.miophas.singularity_iteration.core.prefab.blockentity;

import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.runtime.upgrade.StandardProcessingTiming;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractStandardMachineBlockEntity extends AbstractProcessingMachineBlockEntity {
    private StandardProcessingTiming timing;
    private int previousOverclockers = -1;
    private boolean paidForThisTick;

    protected AbstractStandardMachineBlockEntity(BlockPos pos, BlockState state, BlockEntityType<?> type,
            long capacity, long maxReceive, long maxExtract, int duration, SlotLayout layout,
            long energyPerTick, CableTier tier) {
        super(pos, state, type, capacity, maxReceive, maxExtract, duration, layout, energyPerTick, tier);
    }

    @Override
    protected void updateProcessingParameters() {
        int count = upgradeStats.getOverclockerCount();
        if (timing != null && previousOverclockers == count) return;
        timing = StandardProcessingTiming.calculate(baseMaxProgress, energyPerTick,
            upgradeStats.getProcessTimeMultiplier(), upgradeStats.getEnergyUsageMultiplier());
        previousOverclockers = count;
        int previousLength = maxProgress;
        maxProgress = cycleScaledTicks(timing.ticks());   // 0.1.11.2: with the duration factor of an open work cycle
        if (previousLength > 0 && previousLength != maxProgress)
            progress = (int) Math.min(maxProgress - 1L, (long) ((double) progress * maxProgress / previousLength));
    }

    @Override
    protected void tickProduction() {
        // Validate the input/output even during a power outage. A blocked recipe
        // clears progress, while a valid recipe waiting for power keeps progress.
        boolean available;
        paidForThisTick = true;
        try { available = canWork(); } finally { paidForThisTick = false; }
        if (!available) {
            boolean changed = progress != 0 || isWorking;
            progress = 0;
            stopWork();
            closeWorkCycle();   // 0.1.11.2
            if (changed) setChanged();
            return;
        }
        if (!hasEnoughEnergy()) {
            boolean changed = isWorking;
            stopWork();
            if (changed) setChanged();
            return;
        }
        // 0.1.11.2: work cycle upgrades and MachineWorkStartEvent; a hold is the same as the wait for energy above.
        if (progress == 0 && !isWorkCycleOpen() && !beginWorkCycle()) {
            boolean changed = isWorking;
            stopWork();
            if (changed) setChanged();
            return;
        }
        if (!consumeEnergy()) { stopWork(); return; }
        isWorking = true;
        if (++progress < maxProgress) return;
        // Pay once per active world tick. Each operation still rechecks the real recipe,
        // input quantity and output space through the existing machine implementation.
        paidForThisTick = true;
        int finished = finishedOperations();
        try {
            for (int i=0; i<timing.operations() && canWork(); i++) {
                progress = maxProgress;
                doWork();
            }
        } finally {
            paidForThisTick = false;
            progress = 0;
            isWorking = true;
            setChanged();
        }
        completeWorkCycles(finishedOperations() - finished);   // 0.1.11.2: also closes the cycle
    }

    @Override
    protected int workCycleBaseTicks() {
        return timing == null ? Math.max(1, maxProgress) : timing.ticks();
    }

    @Override
    protected boolean consumeEnergy() { return paidForThisTick || super.consumeEnergy(); }

    @Override
    protected boolean hasEnoughEnergy() { return paidForThisTick || super.hasEnoughEnergy(); }

    @Override
    protected void checkInputChanged() {
        // A replacement that still has a valid recipe retains the elapsed progress.
        // Invalid input and a blocked output are handled by tickProduction above.
    }

    @Override
    public long getEffectiveEnergyPerTick() {
        return cycleScaledEnergy(timing == null ? energyPerTick : timing.energyPerTick());   // 0.1.11.2: cycle energy factor
    }

    @Override
    protected long getProcessingCapacity() {
        return timing == null ? baseCapacity : timing.bufferWithStorage(baseCapacity, upgradeStats.getEnergyStorageCount());
    }

    @Override
    public long getEffectiveCapacity() { return getProcessingCapacity(); }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("scex_operation_ticks", maxProgress);
    }

    private void restoreTaggedProgress(CompoundTag tag) {
        if (tag.contains("scex_operation_ticks", Tag.TAG_INT)) {
            int savedDuration = Math.max(1, tag.getInt("scex_operation_ticks"));
            long scaled = (long) Math.max(0, tag.getInt("progress")) * maxProgress / savedDuration;
            progress = (int) Math.min(maxProgress - 1L, scaled);
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // Unmarked upstream saves use the old base-duration progress scale.
        // New saves explicitly retain the duration that their progress belongs to.
        restoreTaggedProgress(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("scex_operation_ticks", maxProgress);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        recalculateUpgradeStats();
        restoreTaggedProgress(tag);
        if (tag.contains("energy", Tag.TAG_ANY_NUMERIC)) apiSetEnergy(tag.getLong("energy"));
    }
}
