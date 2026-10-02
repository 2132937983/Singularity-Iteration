package com.miophas.singularity_iteration.core.runtime.machine;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyStorageBlockEntity;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.item.IItemAPI;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

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

        // Equipment in priority order (stable: armor head-to-feet, then main and off hand).
        java.util.List<ItemStack> equipment = new java.util.ArrayList<>(6);
        for (EquipmentSlot slot : CHARGE_SLOTS) equipment.add(player.getItemBySlot(slot));
        equipment.add(player.getMainHandItem());
        equipment.add(player.getOffhandItem());
        equipment.sort(java.util.Comparator.comparingInt(
            (ItemStack stack) -> -com.miophas.singularity_iteration.core.api.item.ChargePriority.of(stack).ordinal()));
        for (ItemStack stack : equipment) {
            if (totalExtracted >= availableEnergy) break;
            if (stack.isEmpty() || com.miophas.singularity_iteration.core.api.item.ChargePriority.of(stack)
                    == com.miophas.singularity_iteration.core.api.item.ChargePriority.OFF) continue;
            if (api.isElectricArmor(stack)) {
                totalExtracted += chargeArmor(container, stack, availableEnergy - totalExtracted, maxTransferPerItem);
            } else {
                totalExtracted += chargeTool(container, stack, availableEnergy - totalExtracted, maxTransferPerItem);
            }
        }

        if (totalExtracted < availableEnergy) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (totalExtracted >= availableEnergy) break;
                ItemStack stack = player.getInventory().getItem(i);
                if (com.miophas.singularity_iteration.core.api.item.ChargePriority.of(stack)
                        == com.miophas.singularity_iteration.core.api.item.ChargePriority.OFF) continue;
                totalExtracted += chargeInventoryItem(container, stack, availableEnergy - totalExtracted, maxTransferPerItem);
            }
        }

        if (totalExtracted > 0) {
            container.setChanged();
            return true;
        }
        return false;
    }

    /**
     * Moves energy only after the item has said how much it will take. The old order
     * (extract first, then charge and ignore the result) silently destroyed whatever the
     * item refused - e.g. its per-tick charge limit - which showed up as very low
     * charging efficiency.
     */
    private static long transfer(AbstractEnergyStorageBlockEntity container, long offer,
                                 java.util.function.LongUnaryOperator simulate, java.util.function.LongUnaryOperator charge) {
        long accepted = Math.min(offer, Math.max(0, simulate.applyAsLong(offer)));
        if (accepted <= 0) return 0;
        var storage = container.getEnergyStorageInternal();
        long extracted = storage.extract(accepted, false);
        if (extracted <= 0) return 0;
        long stored = Math.max(0, charge.applyAsLong(extracted));
        if (stored < extracted) storage.generateEnergyInternal(extracted - stored, false);   // refund, never lose energy
        return stored;
    }

    private static long chargeArmor(AbstractEnergyStorageBlockEntity container, ItemStack stack, long availableEnergy, long maxTransferPerItem) {
        IItemAPI api = MioIcifAPI.instance().getItemAPI();
        long current = api.getElectricArmorStored(stack);
        long max = api.getElectricArmorMaxEnergy(stack);
        if (current >= max) return 0;
        long spaceInItem = max - current;
        long energyToTransfer = Math.min(availableEnergy, Math.min(maxTransferPerItem, spaceInItem));
        if (energyToTransfer <= 0) return 0;
        return transfer(container, energyToTransfer, amount -> api.chargeElectricArmor(stack, amount, true),
            amount -> api.chargeElectricArmor(stack, amount, false));
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
        return transfer(container, energyToTransfer, amount -> api.chargeElectricTool(stack, amount, true),
            amount -> api.chargeElectricTool(stack, amount, false));
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
        return transfer(container, energyToTransfer, amount -> api.chargeBattery(stack, amount, true),
            amount -> api.chargeBattery(stack, amount, false));
    }
}
