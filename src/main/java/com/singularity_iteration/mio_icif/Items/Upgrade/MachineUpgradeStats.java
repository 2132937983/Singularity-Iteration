package com.singularity_iteration.mio_icif.Items.Upgrade;

import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.api.machine.IMachineUpgradeStats;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
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
    public static final double OVERCLOCKER_ENERGY_MULTIPLIER = 1.3;
    public static final long ENERGY_STORAGE_BONUS = 10000L;
    public static final long OVERCLOCKER_ENERGY_BONUS = 1000L;

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

    public MachineUpgradeStats(int overclockerCount, int energyStorageCount, int transformerCount,
                               int ejectorCount, int pullingCount, int fluidEjectorCount,
                               int fluidPullingCount, boolean redstoneInverted,
                               List<DirectionalUpgrade> ejectorDirections,
                               List<DirectionalUpgrade> pullingDirections,
                               List<DirectionalUpgrade> fluidEjectorDirections,
                               List<DirectionalUpgrade> fluidPullingDirections) {
        this.overclockerCount = overclockerCount;
        this.energyStorageCount = energyStorageCount;
        this.transformerCount = transformerCount;
        this.ejectorCount = ejectorCount;
        this.pullingCount = pullingCount;
        this.fluidEjectorCount = fluidEjectorCount;
        this.fluidPullingCount = fluidPullingCount;
        this.redstoneInverted = redstoneInverted;
        this.ejectorDirections = ejectorDirections;
        this.pullingDirections = pullingDirections;
        this.fluidEjectorDirections = fluidEjectorDirections;
        this.fluidPullingDirections = fluidPullingDirections;
    }

    public static MachineUpgradeStats fromInventory(IItemHandler itemHandler, int startSlot, int count) {
        if (itemHandler == null || startSlot < 0 || count <= 0) {
            return empty();
        }

        int overclocker = 0;
        int energyStorage = 0;
        int transformer = 0;
        int ejector = 0;
        int pulling = 0;
        int fluidEjector = 0;
        int fluidPulling = 0;
        boolean redstoneInverted = false;

        List<DirectionalUpgrade> ejectorDirs = new ArrayList<>();
        List<DirectionalUpgrade> pullingDirs = new ArrayList<>();
        List<DirectionalUpgrade> fluidEjectorDirs = new ArrayList<>();
        List<DirectionalUpgrade> fluidPullingDirs = new ArrayList<>();

        int endSlot = Math.min(startSlot + count, itemHandler.getSlots());
        for (int i = startSlot; i < endSlot; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            int amount = stack.getCount();

            String upgradeType = MioIcifAPI.instance().getItemAPI().getUpgradeType(stack);
            if (upgradeType != null) {
                Direction dir = MioIcifAPI.instance().getItemAPI().getUpgradeDirection(stack);

                switch (upgradeType) {
                    case "overclocker" -> overclocker += amount;
                    case "energy_storage" -> energyStorage += amount;
                    case "transformer" -> transformer += amount;
                    case "ejector" -> {
                        ejector += amount;
                        ejectorDirs.add(new DirectionalUpgrade(dir, amount));
                    }
                    case "pulling" -> {
                        pulling += amount;
                        pullingDirs.add(new DirectionalUpgrade(dir, amount));
                    }
                    case "fluid_ejector" -> {
                        fluidEjector += amount;
                        fluidEjectorDirs.add(new DirectionalUpgrade(dir, amount));
                    }
                    case "fluid_pulling" -> {
                        fluidPulling += amount;
                        fluidPullingDirs.add(new DirectionalUpgrade(dir, amount));
                    }
                    case "redstone_inverter" -> redstoneInverted = true;
                }
            } else if (stack.getItem() instanceof com.singularity_iteration.mio_icif.api.upgrade.tile.IAugmentationUpgrade) {
                overclocker += amount;
            }
        }

        return new MachineUpgradeStats(overclocker, energyStorage, transformer, ejector,
            pulling, fluidEjector, fluidPulling, redstoneInverted,
            ejectorDirs, pullingDirs, fluidEjectorDirs, fluidPullingDirs);
    }

    public static MachineUpgradeStats empty() {
        return new MachineUpgradeStats(0, 0, 0, 0, 0, 0, 0, false,
            Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList());
    }

    @Override
    public double getProcessTimeMultiplier() {
        return Math.pow(OVERCLOCKER_SPEED_MULTIPLIER, overclockerCount);
    }

    @Override
    public double getEnergyUsageMultiplier() {
        return Math.pow(OVERCLOCKER_ENERGY_MULTIPLIER, overclockerCount);
    }

    @Override
    public int getProcessTicks(int baseTicks) {
        return Math.max(1, (int) Math.ceil(baseTicks * getProcessTimeMultiplier()));
    }

    @Override
    public int getMaxProgress(int baseMaxProgress) {
        return getProcessTicks(baseMaxProgress);
    }

    @Override
    public long getEnergyPerTick(long baseEnergyPerTick) {
        return Math.max(1, (long) Math.ceil(baseEnergyPerTick * getEnergyUsageMultiplier()));
    }

    @Override
    public long getEnergyCapacityBonus() {
        return energyStorageCount * ENERGY_STORAGE_BONUS + overclockerCount * OVERCLOCKER_ENERGY_BONUS;
    }

    @Override
    public ICableTier getEffectiveCableTier(ICableTier baseTier) {
        var allTiers = CableTier.allTiers();
        int maxTierIndex = allTiers.size() - 1;
        int targetIndex = Math.min(baseTier.getTier() + transformerCount, maxTierIndex);
        if (targetIndex < 0 || targetIndex >= allTiers.size()) {
            return allTiers.get(allTiers.size() - 1);
        }
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
        return convertToDirections(ejectorDirections);
    }

    @Override
    public List<Direction> getPullingDirections() {
        return convertToDirections(pullingDirections);
    }

    @Override
    public List<Direction> getFluidEjectorDirections() {
        return convertToDirections(fluidEjectorDirections);
    }

    @Override
    public List<Direction> getFluidPullingDirections() {
        return convertToDirections(fluidPullingDirections);
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
        return result;
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