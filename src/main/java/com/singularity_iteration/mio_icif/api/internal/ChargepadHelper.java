package com.singularity_iteration.mio_icif.api.internal;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Container;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.item.IItemAPI;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class ChargepadHelper {

    private ChargepadHelper() {}

    public static boolean chargeNearbyPlayer(mio_icif_Energy_Container container, long maxTransferPerItem) {
        if (container.getLevel() == null || container.getLevel().isClientSide()) return false;

        if (!container.shouldEmitEnergy()) return false;

        var storage = container.getEnergyStorageInternal();
        if (storage.getAmount() <= 0) return false;

        AABB detectionBox = new AABB(
            container.getBlockPos().getX(), container.getBlockPos().getY(), container.getBlockPos().getZ(),
            container.getBlockPos().getX() + 1, container.getBlockPos().getY() + 2.5, container.getBlockPos().getZ() + 1
        );
        List<Player> players = container.getLevel().getEntitiesOfClass(Player.class, detectionBox);

        boolean charged = false;
        for (Player player : players) {
            if (chargePlayerEquipment(container, player, maxTransferPerItem)) {
                charged = true;
            }
        }
        return charged;
    }

    private static boolean chargePlayerEquipment(mio_icif_Energy_Container container, Player player, long maxTransferPerItem) {
        var storage = container.getEnergyStorageInternal();
        long availableEnergy = storage.getAmount();
        if (availableEnergy <= 0) return false;

        long totalExtracted = 0;
        IItemAPI api = MioIcifAPI.instance().getItemAPI();

        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
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

    private static long chargeArmor(mio_icif_Energy_Container container, ItemStack stack, long availableEnergy, long maxTransferPerItem) {
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

    private static long chargeTool(mio_icif_Energy_Container container, ItemStack stack, long availableEnergy, long maxTransferPerItem) {
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

    private static long chargeInventoryItem(mio_icif_Energy_Container container, ItemStack stack, long availableEnergy, long maxTransferPerItem) {
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

    private static long chargeBattery(mio_icif_Energy_Container container, ItemStack stack, long availableEnergy, long maxTransferPerItem) {
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