package com.miophas.singularity_iteration.core.runtime.machine;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyStorageBlockEntity;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.item.IItemAPI;
import com.miophas.singularity_iteration.core.api.energy.storage.ILongEnergyStorage;
import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import com.miophas.singularity_iteration.core.runtime.energy.DirectItemCharging;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class ChargepadHelper {

    /**
     * 0.1.11.206: where the charge comes from: a charge pad's own storage, a GESU core, a TESU controller (the wireless induction
     * ports). {@code take} moves EU out and returns what it moved; {@code refund} puts back what an item refused.
     */
    public interface Source {
        long available();
        long take(long amount);
        void refund(long amount);
    }

    /** A charge pad's own storage as the source. */
    private static Source of(AbstractEnergyStorageBlockEntity container) {
        var storage = container.getEnergyStorageInternal();
        return new Source() {
            @Override public long available() { return storage.getAmount(); }
            @Override public long take(long amount) { return storage.extract(amount, false); }
            @Override public void refund(long amount) { storage.generateEnergyInternal(amount, false); }
        };
    }

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
        Source source = of(container);
        for (Player player : players) {
            if (chargePlayer(player, source, maxTransferPerItem) > 0) {
                charged = true;
            }
        }
        if (charged) container.setChanged();
        return charged;
    }

    /**
     * Charges the electric items {@code player} carries (armor and hands by charge priority, then the inventory) from
     * {@code source}, at most {@code maxTransferPerItem} EU an item; returns the EU moved. 0.1.11.206: public, from any source.
     */
    public static long chargePlayer(Player player, Source source, long maxTransferPerItem) {
        long availableEnergy = source.available();
        if (availableEnergy <= 0) return 0;
        Source container = source;

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
            } else if (api.isElectricTool(stack)) {
                totalExtracted += chargeTool(container, stack, availableEnergy - totalExtracted, maxTransferPerItem);
            } else {
                totalExtracted += chargeFeItem(container, stack, availableEnergy - totalExtracted, maxTransferPerItem);
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

        return totalExtracted;
    }

    /**
     * Moves energy only after the item has said how much it will take. The old order
     * (extract first, then charge and ignore the result) silently destroyed whatever the
     * item refused - e.g. its per-tick charge limit - which showed up as very low
     * charging efficiency.
     */
    private static long transfer(Source container, long offer,
                                 java.util.function.LongUnaryOperator simulate, java.util.function.LongUnaryOperator charge) {
        long accepted = Math.min(offer, Math.max(0, simulate.applyAsLong(offer)));
        if (accepted <= 0) return 0;
        long extracted = container.take(accepted);
        if (extracted <= 0) return 0;
        long stored = Math.max(0, charge.applyAsLong(extracted));
        if (stored < extracted) container.refund(extracted - stored);   // refund, never lose energy
        return stored;
    }

    private static long chargeArmor(Source container, ItemStack stack, long availableEnergy, long maxTransferPerItem) {
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

    private static long chargeTool(Source container, ItemStack stack, long availableEnergy, long maxTransferPerItem) {
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

    private static long chargeInventoryItem(Source container, ItemStack stack, long availableEnergy, long maxTransferPerItem) {
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
        return chargeFeItem(container, stack, availableEnergy, maxTransferPerItem);
    }

    /**
     * 0.1.10.39: another mod's FE item (an AE2 wireless terminal, a Mekanism tool) on a charge pad. It takes whole EU at
     * {@link ILongEnergyStorage#FE_PER_EU} FE each; what it refuses goes back to the pad. SI items keep their own paths above.
     */
    private static long chargeFeItem(Source container, ItemStack stack, long availableEnergy, long maxTransferPerItem) {
        if (stack.isEmpty() || stack.getCount() != 1 || stack.getItem() instanceof IBatteryItem) return 0;
        var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (cap == null || !cap.canReceive()) return 0;
        long offer = Math.min(availableEnergy, maxTransferPerItem);
        if (offer <= 0) return 0;
        return transfer(container, offer,
            amount -> cap.receiveEnergy((int) Math.min(Integer.MAX_VALUE / ILongEnergyStorage.FE_PER_EU, amount) * ILongEnergyStorage.FE_PER_EU, true)
                / ILongEnergyStorage.FE_PER_EU,
            amount -> DirectItemCharging.charge(stack, amount));
    }

    private static long chargeBattery(Source container, ItemStack stack, long availableEnergy, long maxTransferPerItem) {
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
