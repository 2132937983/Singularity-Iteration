package com.miophas.singularity_iteration.core.runtime.machine;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyStorageBlockEntity;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.item.IItemAPI;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class ChargepadHelper {

    private static final EquipmentSlot[] CHARGE_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private ChargepadHelper() {}

    public static boolean chargeNearbyPlayer(AbstractEnergyStorageBlockEntity container, long maxTransferPerItem) {
        if (container.getLevel() == null || container.getLevel().isClientSide()) return false;

        if (!container.shouldEmitEnergy()) return false;

        var storage = container.getEnergyStorageInternal();
        if (storage.getAmount() <= 0) return false;

        List<Player> players = container.getLevel().getEntitiesOfClass(Player.class, container.getChargepadDetectionBox());

        boolean charged = false;
        for (Player player : players) {
            if (chargePlayerEquipment(container, player, maxTransferPerItem)) {
                charged = true;
            }
        }
        return charged;
    }

    private static boolean chargePlayerEquipment(AbstractEnergyStorageBlockEntity container, Player player, long maxTransferPerItem) {
        var storage = container.getEnergyStorageInternal();
        long availableEnergy = storage.getAmount();
        if (availableEnergy <= 0) return false;

        long totalExtracted = 0;
        IItemAPI api = MioIcifAPI.instance().getItemAPI();

        for (EquipmentSlot slot : CHARGE_SLOTS) {
            if (totalExtracted >= availableEnergy) break;
            ItemStack stack = player.getItemBySlot(slot);
            if (api.isElectricArmor(stack)) {
                totalExtracted += chargeArmor(container, stack, availableEnergy - totalExtracted, maxTransferPerItem);
            }
        }

        if (totalExtracted < availableEnergy) {
            totalExtracted += chargeTool(container, player.getMainHandItem(), availableEnergy - totalExtracted, maxTransferPerItem);
        }
        if (totalExtracted < availableEnergy) {
            totalExtracted += chargeTool(container, player.getOffhandItem(), availableEnergy - totalExtracted, maxTransferPerItem);
        }

        if (totalExtracted < availableEnergy) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (totalExtracted >= availableEnergy) break;
                ItemStack stack = player.getInventory().getItem(i);
                totalExtracted += chargeInventoryItem(container, stack, availableEnergy - totalExtracted, maxTransferPerItem);
            }
        }

        if (totalExtracted > 0) {
            container.setChanged();
            return true;
        }
        return false;
    }

    private static long chargeArmor(AbstractEnergyStorageBlockEntity container, ItemStack stack, long availableEnergy, long maxTransferPerItem) {
        IItemAPI api = MioIcifAPI.instance().getItemAPI();
        long current = api.getElectricArmorStored(stack);
        long max = api.getElectricArmorMaxEnergy(stack);
        if (current >= max) return 0;
        long spaceInItem = max - current;
        long energyToTransfer = Math.min(availableEnergy, Math.min(maxTransferPerItem, spaceInItem));
        if (energyToTransfer <= 0) return 0;
        var storage = container.getEnergyStorageInternal();
        long extracted = storage.extract(energyToTransfer, false);
        if (extracted > 0) {
            api.chargeElectricArmor(stack, extracted, false);
        }
        return extracted;
    }

    private static long chargeTool(AbstractEnergyStorageBlockEntity container, ItemStack stack, long availableEnergy, long maxTransferPerItem) {
        if (stack.isEmpty()) return 0;
        IItemAPI api = MioIcifAPI.instance().getItemAPI();
        if (!api.isElectricTool(stack)) return 0;
        long current = api.getElectricToolStored(stack);
        long max = api.getElectricToolMaxEnergy(stack);
        if (current >= max) return 0;
        long spaceInItem = max - current;
        long energyToTransfer = Math.min(availableEnergy, Math.min(maxTransferPerItem, spaceInItem));
        if (energyToTransfer <= 0) return 0;
        var storage = container.getEnergyStorageInternal();
        long extracted = storage.extract(energyToTransfer, false);
        if (extracted > 0) {
            api.chargeElectricTool(stack, extracted, false);
        }
        return extracted;
    }

    private static long chargeInventoryItem(AbstractEnergyStorageBlockEntity container, ItemStack stack, long availableEnergy, long maxTransferPerItem) {
        if (stack.isEmpty()) return 0;
        IItemAPI api = MioIcifAPI.instance().getItemAPI();
        if (api.isElectricArmor(stack)) {
            return chargeArmor(container, stack, availableEnergy, maxTransferPerItem);
        }
        if (api.isElectricTool(stack)) {
            return chargeTool(container, stack, availableEnergy, maxTransferPerItem);
        }
        if (api.isBattery(stack)) {
            return chargeBattery(container, stack, availableEnergy, maxTransferPerItem);
        }
        return 0;
    }

    private static long chargeBattery(AbstractEnergyStorageBlockEntity container, ItemStack stack, long availableEnergy, long maxTransferPerItem) {
        IItemAPI api = MioIcifAPI.instance().getItemAPI();
        if (api.isBatteryFull(stack)) return 0;
        long current = api.getBatteryStored(stack);
        long max = api.getBatteryCapacity(stack);
        long chargeRate = api.getChargeRate(stack);
        long spaceInItem = max - current;
        long maxTransfer = Math.min(maxTransferPerItem, Math.min(spaceInItem, chargeRate));
        long energyToTransfer = Math.min(availableEnergy, maxTransfer);
        if (energyToTransfer <= 0) return 0;
        var storage = container.getEnergyStorageInternal();
        long extracted = storage.extract(energyToTransfer, false);
        if (extracted > 0) {
            api.chargeBattery(stack, extracted, false);
        }
        return extracted;
    }
}
