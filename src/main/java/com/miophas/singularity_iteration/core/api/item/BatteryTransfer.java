// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.item;

import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.ItemStack;

/** Common SI item API accounting. Rates remain a caller policy; each result is an actual item delta. */
public final class BatteryTransfer {
    private BatteryTransfer() { }

    public static boolean isEquipment(ItemStack stack) {
        return !stack.isEmpty() && stack.getCount() == 1
            && (stack.getItem() instanceof IElectricToolItem || stack.getItem() instanceof IElectricArmorItem);
    }

    /** External supply is distinct from a tool spending its own charge. Addon declarations take precedence. */
    public static boolean canProvideExternally(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getCount() != 1) return false;
        try {
            var electric = stack.getCapability(IMioIcifCapabilities.ELECTRIC_ITEM);
            return electric != null ? electric.canProvideEnergy(stack) : !isEquipment(stack);
        } catch (RuntimeException | LinkageError unavailable) {
            return false;
        }
    }

    /** Stage both owned items before a manual equipment charge; a refusing or throwing provider cannot mint energy. */
    public static long move(ItemStack source, IBatteryItem from, ItemStack target, IBatteryItem to, long amount) {
        if (source == target || source.isEmpty() || target.isEmpty() || source.getCount() != 1 || target.getCount() != 1 || amount <= 0) return 0;
        long available = from.getEnergy(source), stored = to.getEnergy(target), capacity = to.getMaxEnergy(target);
        if (available <= 0 || available > from.getMaxEnergy(source) || stored < 0 || stored > capacity) return 0;
        long offered = Math.min(amount, Math.min(available, capacity - stored));
        if (offered <= 0) return 0;
        var oldSource = source.copy();
        var oldTarget = target.copy();
        var nextSource = oldSource.copy();
        var nextTarget = oldTarget.copy();
        long accepted = transfer(nextTarget, to, offered, true, false);
        if (accepted <= 0 || from.extractEnergy(nextSource, accepted) != accepted
                || from.getEnergy(nextSource) != available - accepted || to.getEnergy(nextTarget) != stored + accepted
                || nextSource.getItem() != oldSource.getItem() || nextTarget.getItem() != oldTarget.getItem()
                || nextSource.getCount() != 1 || nextTarget.getCount() != 1
                || !ItemStack.matches(oldSource, source) || !ItemStack.matches(oldTarget, target)) return 0;
        commit(source, oldSource, nextSource);
        if (!ItemStack.matches(oldTarget, target)) throw new IllegalStateException("Electric charging destination changed during debit commit");
        commit(target, oldTarget, nextTarget);
        if (from.getEnergy(source) != available - accepted || to.getEnergy(target) != stored + accepted)
            throw new IllegalStateException("Electric charging item changed a committed balance");
        return accepted;
    }

    private static void commit(ItemStack stack, ItemStack before, ItemStack after) {
        var patch = DataComponentPatch.builder();
        for (var component : before.getComponents()) if (!after.has(component.type())) patch.remove(component.type());
        for (var component : after.getComponents()) patch.set(component);
        stack.applyComponents(patch.build());
    }

    /** Paid actions either get their full debit or leave the original item untouched. */
    public static boolean consume(ItemStack stack, IBatteryItem battery, long amount) {
        if (stack.isEmpty() || stack.getCount() != 1 || amount < 0) return false;
        if (amount == 0) return true;
        long before = battery.getEnergy(stack), capacity = battery.getMaxEnergy(stack);
        if (before < amount || before > capacity) return false;
        var snapshot = stack.copy();
        var staged = snapshot.copy();
        long removed = battery.extractEnergy(staged, amount);
        if (removed != amount || battery.getEnergy(staged) != before - amount
                || staged.getItem() != snapshot.getItem() || staged.getCount() != 1
                || battery.getMaxEnergy(staged) != capacity || !ItemStack.matches(snapshot, stack)) return false;
        commit(stack, snapshot, staged);
        if (battery.getEnergy(stack) != before - amount) throw new IllegalStateException("Electric item changed a committed action debit");
        return true;
    }

    public static long transfer(ItemStack stack, IBatteryItem battery, long amount, boolean charging, boolean simulate) {
        if (stack.isEmpty() || stack.getCount() != 1 || amount <= 0) return 0;
        long capacity = battery.getMaxEnergy(stack), before = battery.getEnergy(stack);
        if (capacity <= 0 || before < 0 || before > capacity) return 0;
        long offered = Math.min(amount, charging ? capacity - before : before);
        if (offered <= 0 || simulate) return offered;
        long reported = charging ? battery.addEnergy(stack, offered) : battery.extractEnergy(stack, offered);
        long after = battery.getEnergy(stack);
        if (after < 0 || after > capacity) throw new IllegalStateException("Electric item returned an out-of-range balance");
        long changed = charging ? after - before : before - after;
        if (changed < 0 || changed > offered || reported != changed)
            throw new IllegalStateException("Electric item transfer disagrees with its stored energy change");
        return changed;
    }
}
