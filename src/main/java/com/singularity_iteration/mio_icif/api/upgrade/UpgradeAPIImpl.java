package com.singularity_iteration.mio_icif.api.upgrade;

import com.singularity_iteration.mio_icif.api.machine.IProducerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 升级插件 API 实现
 */
@SuppressWarnings("null")
public class UpgradeAPIImpl implements IUpgradeAPI {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeAPIImpl.class);

    @Override
    public boolean supportsUpgrades(Level world, BlockPos pos) {
        if (world.isClientSide) return false;

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IProducerBlock producer) {
            return producer.getUpgradeSlotCount() > 0;
        }
        return false;
    }

    @Override
    public int getMaxUpgradeSlots(Level world, BlockPos pos) {
        if (world.isClientSide) return 0;

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IProducerBlock producer) {
            return producer.getUpgradeSlotCount();
        }
        return 0;
    }

    @Override
    public Collection<ItemStack> getInstalledUpgrades(Level world, BlockPos pos) {
        if (world.isClientSide) return Collections.emptyList();

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IProducerBlock producer) {
            List<ItemStack> upgrades = new ArrayList<>();
            int start = producer.getUpgradeSlotStart();
            int count = producer.getUpgradeSlotCount();
            IItemHandler handler = producer.getItemHandler();
            for (int i = 0; i < count; i++) {
                ItemStack stack = handler.getStackInSlot(start + i);
                if (!stack.isEmpty()) {
                    upgrades.add(stack);
                }
            }
            return upgrades;
        }
        return Collections.emptyList();
    }

    @Override
    public UpgradeType getUpgradeType(ItemStack upgrade) {
        if (upgrade.getItem() instanceof com.singularity_iteration.mio_icif.api.upgrade.tile.IUpgradeItem upgradeItem) {
            return upgradeItem.getApiUpgradeType();
        }
        return UpgradeType.CUSTOM;
    }

    @Override
    public IUpgradeEffect getUpgradeEffect(ItemStack upgrade) {
        if (upgrade.getItem() instanceof com.singularity_iteration.mio_icif.api.upgrade.tile.IUpgradeItem upgradeItem) {
            return new UpgradeEffectImpl(
                upgradeItem.getSpeedBonus(),
                upgradeItem.getEnergyBonus(),
                upgradeItem.getExtraCapacity(),
                upgradeItem.getExtraOutputSlots(),
                upgradeItem.hasAutoEject(),
                upgradeItem.hasAutoImport(),
                upgradeItem.hasRedstoneControl(),
                upgradeItem.getTierUpgrade());
        }
        return new UpgradeEffectImpl(1.0, 1.0, 0, 0, false, false, false, 0);
    }

    @Override
    public IUpgradeEffect calculateCombinedEffect(Collection<ItemStack> upgrades) {
        double speedMultiplier = 1.0;
        double energyMultiplier = 1.0;
        long extraCapacity = 0;
        int extraOutputSlots = 0;
        boolean hasAutoEject = false;
        boolean hasAutoImport = false;
        boolean hasRedstoneControl = false;
        int tierUpgrade = 0;

        for (ItemStack upgrade : upgrades) {
            IUpgradeEffect effect = getUpgradeEffect(upgrade);
            speedMultiplier *= effect.getSpeedMultiplier();
            energyMultiplier *= effect.getEnergyMultiplier();
            extraCapacity += effect.getExtraCapacity();
            extraOutputSlots += effect.getExtraOutputSlots();
            hasAutoEject |= effect.hasAutoEject();
            hasAutoImport |= effect.hasAutoImport();
            hasRedstoneControl |= effect.hasRedstoneControl();
            tierUpgrade += effect.getTierUpgrade();
        }

        return new UpgradeEffectImpl(
            speedMultiplier,
            energyMultiplier,
            extraCapacity,
            extraOutputSlots,
            hasAutoEject,
            hasAutoImport,
            hasRedstoneControl,
            tierUpgrade
        );
    }

    @Override
    public boolean installUpgrade(Level world, BlockPos pos, ItemStack upgrade) {
        if (world.isClientSide) return false;

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IProducerBlock) {
            if (!canInstallUpgrade(world, pos, upgrade)) {
                return false;
            }

            IItemHandler handler = ((IProducerBlock) be).getItemHandler();
            int start = ((IProducerBlock) be).getUpgradeSlotStart();
            int count = ((IProducerBlock) be).getUpgradeSlotCount();
            for (int i = 0; i < count; i++) {
                int slot = start + i;
                if (handler.getStackInSlot(slot).isEmpty()) {
                    ItemStack remainder = handler.insertItem(slot, upgrade, false);
                    return remainder.isEmpty() || remainder.getCount() < upgrade.getCount();
                }
            }
        }
        return false;
    }

    @Override
    public ItemStack removeUpgrade(Level world, BlockPos pos, int slot) {
        if (world.isClientSide) return ItemStack.EMPTY;

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IProducerBlock prod) {
            int start = prod.getUpgradeSlotStart();
            int count = prod.getUpgradeSlotCount();
            if (slot >= 0 && slot < count) {
                int actualSlot = start + slot;
                IItemHandler handler = prod.getItemHandler();
                return handler.extractItem(actualSlot, handler.getStackInSlot(actualSlot).getCount(), false);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canInstallUpgrade(Level world, BlockPos pos, ItemStack upgrade) {
        if (world.isClientSide) return false;

        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof IProducerBlock producer)) {
            return false;
        }

        int start = producer.getUpgradeSlotStart();
        int count = producer.getUpgradeSlotCount();
        IItemHandler handler = producer.getItemHandler();
        boolean hasEmptySlot = false;
        for (int i = 0; i < count; i++) {
            if (handler.getStackInSlot(start + i).isEmpty()) {
                hasEmptySlot = true;
                break;
            }
        }
        if (!hasEmptySlot) return false;

        UpgradeType type = getUpgradeType(upgrade);
        int currentCount = 0;
        for (int i = 0; i < count; i++) {
            ItemStack installed = handler.getStackInSlot(start + i);
            if (!installed.isEmpty() && getUpgradeType(installed) == type) {
                currentCount++;
            }
        }

        return currentCount < getUpgradeLimit(type);
    }

    @Override
    public int getUpgradeLimit(UpgradeType type) {
        return switch (type) {
            case OVERCLOCKER -> 16;
            case TRANSFORMER -> 4;
            case ENERGY_STORAGE -> 16;
            case EJECTOR -> 1;
            case IMPORT -> 1;
            case FLUID_EJECTOR -> 1;
            case FLUID_IMPORT -> 1;
            case REDSTONE_SIGNAL -> 1;
            case ADVANCED_CIRCUIT -> 4;
            case MOLECULAR_TRANSFORM -> 1;
            case QUANTUM_CORE -> 1;
            // CUSTOM 类型没有硬性限制，实际限制由机器的升级槽数量决定
            // 返回 4 作为合理的默认上限，避免调用方误以为可以装几十亿个升级
            case CUSTOM -> 4;
        };
    }

    /**
     * 升级效果实现
     */
    private static class UpgradeEffectImpl implements IUpgradeEffect {
        private final double speedMultiplier;
        private final double energyMultiplier;
        private final long extraCapacity;
        private final int extraOutputSlots;
        private final boolean hasAutoEject;
        private final boolean hasAutoImport;
        private final boolean hasRedstoneControl;
        private final int tierUpgrade;

        public UpgradeEffectImpl(double speedMultiplier, double energyMultiplier, long extraCapacity,
                                 int extraOutputSlots, boolean hasAutoEject, boolean hasAutoImport,
                                 boolean hasRedstoneControl, int tierUpgrade) {
            this.speedMultiplier = speedMultiplier;
            this.energyMultiplier = energyMultiplier;
            this.extraCapacity = extraCapacity;
            this.extraOutputSlots = extraOutputSlots;
            this.hasAutoEject = hasAutoEject;
            this.hasAutoImport = hasAutoImport;
            this.hasRedstoneControl = hasRedstoneControl;
            this.tierUpgrade = tierUpgrade;
        }

        @Override
        public double getSpeedMultiplier() { return speedMultiplier; }

        @Override
        public double getEnergyMultiplier() { return energyMultiplier; }

        @Override
        public long getExtraCapacity() { return extraCapacity; }

        @Override
        public int getExtraOutputSlots() { return extraOutputSlots; }

        @Override
        public boolean hasAutoEject() { return hasAutoEject; }

        @Override
        public boolean hasAutoImport() { return hasAutoImport; }

        @Override
        public boolean hasRedstoneControl() { return hasRedstoneControl; }

        @Override
        public int getTierUpgrade() { return tierUpgrade; }
    }
}