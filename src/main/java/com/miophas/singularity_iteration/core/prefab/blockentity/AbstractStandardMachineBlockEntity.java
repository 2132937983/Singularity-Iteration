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
        maxProgress = timing.ticks();
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
            if (changed) setChanged();
            return;
        }
        if (!hasEnoughEnergy()) {
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
        return timing == null ? energyPerTick : timing.energyPerTick();
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
