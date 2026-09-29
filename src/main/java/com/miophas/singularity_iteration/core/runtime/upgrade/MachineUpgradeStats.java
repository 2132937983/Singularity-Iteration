// SCEX 2026-09-12: cache immutable numeric statistics and fix registered-tier selection.
package com.miophas.singularity_iteration.core.runtime.upgrade;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.item.IItemAPI;
import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.api.machine.IMachineUpgradeStats;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("null")
public class MachineUpgradeStats implements IMachineUpgradeStats {

    public static final double OVERCLOCKER_SPEED_MULTIPLIER = 0.7;
    // SCEX alignment: user selected original Experimental behavior over the author's 1.3 design.
    public static final double OVERCLOCKER_ENERGY_MULTIPLIER = 1.6;
    public static final long ENERGY_STORAGE_BONUS = 10000L;
    public static final long OVERCLOCKER_ENERGY_BONUS = 1000L;

    private static final MachineUpgradeStats EMPTY = new MachineUpgradeStats(
        0, 0, 0, 0, 0, 0, 0, false,
        Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

    // Bounded shared tables avoid both repeated pow() calls and per-machine cache fields.
    // Unusual counts retain the original calculation without growing a global cache.
    private static final double[] PROCESS_MULTIPLIERS = multiplierTable(OVERCLOCKER_SPEED_MULTIPLIER);
    private static final double[] ENERGY_MULTIPLIERS = multiplierTable(OVERCLOCKER_ENERGY_MULTIPLIER);

    private static double[] multiplierTable(double base) {
        double[] values = new double[257];
        for (int count = 0; count < values.length; count++) values[count] = Math.pow(base, count);
        return values;
    }

    public final int overclockerCount;
    public final int energyStorageCount;
    public final int transformerCount;
    public final int ejectorCount;
    public final int pullingCount;
    public final int fluidEjectorCount;
    public final int fluidPullingCount;
    public final boolean redstoneInverted;

    private final List<DirectionalUpgrade> ejectorDirections;
    private final List<DirectionalUpgrade> pullingDirections;
    private final List<DirectionalUpgrade> fluidEjectorDirections;
    private final List<DirectionalUpgrade> fluidPullingDirections;
    // Cached immutable projections avoid per-tick list allocations in automation paths.
    private final List<Direction> ejectorDirectionView;
    private final List<Direction> pullingDirectionView;
    private final List<Direction> fluidEjectorDirectionView;
    private final List<Direction> fluidPullingDirectionView;

    public MachineUpgradeStats(int overclockerCount, int energyStorageCount, int transformerCount,
                               int ejectorCount, int pullingCount, int fluidEjectorCount,
                               int fluidPullingCount, boolean redstoneInverted,
                               List<DirectionalUpgrade> ejectorDirections,
                               List<DirectionalUpgrade> pullingDirections,
                               List<DirectionalUpgrade> fluidEjectorDirections,
                               List<DirectionalUpgrade> fluidPullingDirections) {
        this.overclockerCount = Math.max(0, overclockerCount);
        this.energyStorageCount = Math.max(0, energyStorageCount);
        this.transformerCount = Math.max(0, transformerCount);
        this.ejectorCount = Math.max(0, ejectorCount);
        this.pullingCount = Math.max(0, pullingCount);
        this.fluidEjectorCount = Math.max(0, fluidEjectorCount);
        this.fluidPullingCount = Math.max(0, fluidPullingCount);
        this.redstoneInverted = redstoneInverted;
        this.ejectorDirections = directionSnapshot(ejectorDirections);
        this.pullingDirections = directionSnapshot(pullingDirections);
        this.fluidEjectorDirections = directionSnapshot(fluidEjectorDirections);
        this.fluidPullingDirections = directionSnapshot(fluidPullingDirections);
        this.ejectorDirectionView = convertToDirections(this.ejectorDirections);
        this.pullingDirectionView = convertToDirections(this.pullingDirections);
        this.fluidEjectorDirectionView = convertToDirections(this.fluidEjectorDirections);
        this.fluidPullingDirectionView = convertToDirections(this.fluidPullingDirections);
    }

    private static List<DirectionalUpgrade> directionSnapshot(List<DirectionalUpgrade> directions) {
        if (directions == null || directions.isEmpty()) return List.of();
        return directions.stream().filter(java.util.Objects::nonNull).filter(upgrade -> upgrade.count > 0).toList();
    }

    private static int addCount(int before, int amount) {
        // The public statistics API uses ints. Saturate its summary without changing any inventory stack.
        return (int) Math.min(Integer.MAX_VALUE, (long) before + Math.max(0, amount));
    }

    public static MachineUpgradeStats fromInventory(IItemHandler itemHandler, int startSlot, int count) {
        if (itemHandler == null || startSlot < 0 || count <= 0) {
            return empty();
        }
        return fromInventory(itemHandler, startSlot, count, MioIcifAPI.instance().getItemAPI());
    }

    public static MachineUpgradeStats fromInventory(IItemHandler itemHandler, int startSlot, int count, IItemAPI itemApi) {
        if (itemHandler == null || itemApi == null || startSlot < 0 || count <= 0) return empty();

        int overclocker = 0;
        int energyStorage = 0;
        int transformer = 0;
        int ejector = 0;
        int pulling = 0;
        int fluidEjector = 0;
        int fluidPulling = 0;
        boolean redstoneInverted = false;

        List<DirectionalUpgrade> ejectorDirs = Collections.emptyList();
        List<DirectionalUpgrade> pullingDirs = Collections.emptyList();
        List<DirectionalUpgrade> fluidEjectorDirs = Collections.emptyList();
        List<DirectionalUpgrade> fluidPullingDirs = Collections.emptyList();

        int endSlot = (int) Math.min((long) startSlot + count, itemHandler.getSlots());
        for (int i = startSlot; i < endSlot; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            int amount = stack.getCount();

            String upgradeType = itemApi.getUpgradeType(stack);
            if (upgradeType != null) {
                Direction dir = itemApi.getUpgradeDirection(stack);

                switch (upgradeType) {
                    case "overclocker" -> overclocker = addCount(overclocker, amount);
                    case "energy_storage" -> energyStorage = addCount(energyStorage, amount);
                    case "transformer" -> transformer = addCount(transformer, amount);
                    case "ejector" -> {
                        ejector = addCount(ejector, amount);
                        if (ejectorDirs.isEmpty()) ejectorDirs = new ArrayList<>();
                        ejectorDirs.add(new DirectionalUpgrade(dir, amount));
                    }
                    case "pulling" -> {
                        pulling = addCount(pulling, amount);
                        if (pullingDirs.isEmpty()) pullingDirs = new ArrayList<>();
                        pullingDirs.add(new DirectionalUpgrade(dir, amount));
                    }
                    case "fluid_ejector" -> {
                        fluidEjector = addCount(fluidEjector, amount);
                        if (fluidEjectorDirs.isEmpty()) fluidEjectorDirs = new ArrayList<>();
                        fluidEjectorDirs.add(new DirectionalUpgrade(dir, amount));
                    }
                    case "fluid_pulling" -> {
                        fluidPulling = addCount(fluidPulling, amount);
                        if (fluidPullingDirs.isEmpty()) fluidPullingDirs = new ArrayList<>();
                        fluidPullingDirs.add(new DirectionalUpgrade(dir, amount));
                    }
                    case "redstone_inverter" -> redstoneInverted = true;
                }
            } else if (stack.getItem() instanceof com.miophas.singularity_iteration.core.api.upgrade.tile.IAugmentationUpgrade) {
                overclocker = addCount(overclocker, amount);
            }
        }

        if (overclocker == 0 && energyStorage == 0 && transformer == 0 && ejector == 0 && pulling == 0
                && fluidEjector == 0 && fluidPulling == 0 && !redstoneInverted) return EMPTY;
        return new MachineUpgradeStats(overclocker, energyStorage, transformer, ejector,
            pulling, fluidEjector, fluidPulling, redstoneInverted,
            ejectorDirs, pullingDirs, fluidEjectorDirs, fluidPullingDirs);
    }

    public static MachineUpgradeStats empty() {
        return EMPTY;
    }

    @Override
    public double getProcessTimeMultiplier() {
        return overclockerCount >= 0 && overclockerCount < PROCESS_MULTIPLIERS.length
            ? PROCESS_MULTIPLIERS[overclockerCount] : Math.pow(OVERCLOCKER_SPEED_MULTIPLIER, overclockerCount);
    }

    @Override
    public double getEnergyUsageMultiplier() {
        return overclockerCount >= 0 && overclockerCount < ENERGY_MULTIPLIERS.length
            ? ENERGY_MULTIPLIERS[overclockerCount] : Math.pow(OVERCLOCKER_ENERGY_MULTIPLIER, overclockerCount);
    }

    @Override
    public int getProcessTicks(int baseTicks) {
        return StandardProcessingTiming.cycleTicks(baseTicks, getProcessTimeMultiplier());
    }

    @Override
    public int getMaxProgress(int baseMaxProgress) {
        return getProcessTicks(baseMaxProgress);
    }

    @Override
    public long getEnergyPerTick(long baseEnergyPerTick) {
        return Math.max(1, Math.round(baseEnergyPerTick * getEnergyUsageMultiplier()));
    }

    @Override
    public long getEnergyCapacityBonus() {
        return energyStorageCount * ENERGY_STORAGE_BONUS + overclockerCount * OVERCLOCKER_ENERGY_BONUS;
    }

    public long getHeatCapacity(long baseCapacity) {
        long base = Math.max(0, baseCapacity), bonus = energyStorageCount * ENERGY_STORAGE_BONUS;
        return base + Math.min(Long.MAX_VALUE - base, bonus);
    }

    @Override
    public ICableTier getEffectiveCableTier(ICableTier baseTier) {
        if (transformerCount <= 0) return baseTier;
        var allTiers = CableTier.allTiers();
        // Find the first registered voltage above the actual base voltage.
        // An addon's ordinal is not an index into the sorted registry snapshot.
        int low = 0;
        int high = allTiers.size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (allTiers.get(middle).powerRating <= baseTier.getPowerRating()) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        if (low == allTiers.size()) return baseTier;
        int targetIndex = (int) Math.min((long) low + transformerCount - 1, allTiers.size() - 1L);
        return allTiers.get(targetIndex);
    }

    @Override
    public int getOverclockerCount() {
        return overclockerCount;
    }

    @Override
    public int getEnergyStorageCount() {
        return energyStorageCount;
    }

    @Override
    public int getTransformerCount() {
        return transformerCount;
    }

    @Override
    public int getEjectorCount() {
        return ejectorCount;
    }

    @Override
    public int getPullingCount() {
        return pullingCount;
    }

    @Override
    public int getFluidEjectorCount() {
        return fluidEjectorCount;
    }

    @Override
    public int getFluidPullingCount() {
        return fluidPullingCount;
    }

    @Override
    public boolean isRedstoneInverted() {
        return redstoneInverted;
    }

    @Override
    public List<Direction> getEjectorDirections() {
        return ejectorDirectionView;
    }

    @Override
    public List<Direction> getPullingDirections() {
        return pullingDirectionView;
    }

    @Override
    public List<Direction> getFluidEjectorDirections() {
        return fluidEjectorDirectionView;
    }

    @Override
    public List<Direction> getFluidPullingDirections() {
        return fluidPullingDirectionView;
    }

    @Override
    public int getFluidEjectorTransferLimit(Direction side) {
        return fluidTransferLimit(fluidEjectorDirections, fluidEjectorCount, side);
    }

    @Override
    public int getFluidPullingTransferLimit(Direction side) {
        return fluidTransferLimit(fluidPullingDirections, fluidPullingCount, side);
    }

    private static int fluidTransferLimit(List<DirectionalUpgrade> upgrades, int count, Direction side) {
        if (upgrades.isEmpty()) return IMachineUpgradeStats.fluidTransferPerStack(count);
        long limit = 0;
        for (var upgrade : upgrades) {
            if (upgrade.direction == null || upgrade.direction == side) {
                limit += IMachineUpgradeStats.fluidTransferPerStack(upgrade.count);
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, limit);
    }

    private List<Direction> convertToDirections(List<DirectionalUpgrade> upgrades) {
        if (upgrades == null || upgrades.isEmpty()) {
            return Collections.emptyList();
        }
        List<Direction> result = new ArrayList<>();
        boolean hasAnySide = false;
        for (DirectionalUpgrade upgrade : upgrades) {
            if (upgrade.direction == null) {
                hasAnySide = true;
            } else if (!result.contains(upgrade.direction)) {
                result.add(upgrade.direction);
            }
        }
        if (hasAnySide) {
            for (Direction dir : Direction.values()) {
                if (!result.contains(dir)) {
                    result.add(dir);
                }
            }
        }
        return List.copyOf(result);
    }

    public static class DirectionalUpgrade {
        @Nullable
        public final Direction direction;
        public final int count;

        public DirectionalUpgrade(@Nullable Direction direction, int count) {
            this.direction = direction;
            this.count = count;
        }
    }
}
