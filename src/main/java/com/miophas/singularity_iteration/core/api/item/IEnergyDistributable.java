package com.miophas.singularity_iteration.core.api.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;

/**
 * Portable battery that can feed the electric equipment in the player inventory. Right click
 * switches the inventory auto-charge mode ({@link BatteryAutoCharge}).
 *
 * <p>This function belongs only to batteries. Electric tools must not implement this interface.</p>
 *
 * <h3>How to use</h3>
 * <ul>
 *   <li>Batteries: implement this interface to get the right-click discharge function automatically</li>
 *   <li>Addon batteries: implement {@link IBatteryItem} and this interface</li>
 *   <li>Electric tools: do not implement this interface</li>
 * </ul>
 */
public interface IEnergyDistributable extends IBatteryItem {

    /**
     * Distributes the battery energy to the other electric equipment in the player inventory.
     * The energy is divided equally among the targets that are not full. The battery stack must have a count of 1.
     *
     * @param player        the player
     * @param batteryStack  the battery item stack
     * @param totalEnergy   the total energy that can be distributed (EU); it is limited to the stored energy
     */
    default void distributeEnergyToItems(Player player, ItemStack batteryStack, long totalEnergy) {
        if (batteryStack.isEmpty() || batteryStack.getCount() != 1) return;
        totalEnergy = Math.min(Math.max(0, totalEnergy), getEnergy(batteryStack));
        if (totalEnergy == 0) return;
        List<ItemStack> targets = new ArrayList<>();
        Inventory inv = player.getInventory();

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty() || s == batteryStack) continue;
            if (BatteryTransfer.isEquipment(s) && s.getItem() instanceof IBatteryItem item) {
                if (item.getEnergy(s) < item.getMaxEnergy(s)) targets.add(s);
            }
        }

        if (ModList.get().isLoaded("curios")) {
            BatteryTargetProviders.appendTargets(player, targets, batteryStack);
        }

        if (targets.isEmpty()) return;

        long energyPerTarget = totalEnergy / targets.size();
        if (energyPerTarget <= 0) return;

        for (ItemStack target : targets) {
            BatteryTransfer.move(batteryStack, this, target, (IBatteryItem) target.getItem(), energyPerTarget);
        }
    }

    /** False for tools built on the battery base (detectors, managers): they keep their own right click. */
    default boolean supportsAutoCharge() {
        return true;
    }

    /**
     * Right click: switches the inventory auto-charge mode on or off (0.1.7.35). The battery no
     * longer empties itself into the inventory on each click; while the mode is on it feeds the
     * electric tools and armor of the inventory at its charge rate ({@link BatteryAutoCharge}).
     *
     * @return the interaction result, or null when the stack is not a single battery
     */
    default InteractionResultHolder<ItemStack> handleDistributeUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getCount() != 1 || !supportsAutoCharge()) return null;
        if (!level.isClientSide) {
            boolean on = BatteryAutoCharge.toggle(stack);
            player.displayClientMessage(Component.translatable(on ? "message.mio_icif.bat.auto_on" : "message.mio_icif.bat.auto_off",
                getEnergy(stack), getMaxEnergy(stack)), true);
            level.playSound(null, player.blockPosition(), on ? net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE
                : net.minecraft.sounds.SoundEvents.BEACON_DEACTIVATE, net.minecraft.sounds.SoundSource.PLAYERS, 0.35F, 1.8F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}