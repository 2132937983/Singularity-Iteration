// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.prefab.item;

import com.miophas.singularity_iteration.core.api.item.IBatteryItem;

import com.miophas.singularity_iteration.core.api.item.electric.IElectricItem;
import com.miophas.singularity_iteration.core.api.item.electric.IElectricItemManager;
import com.miophas.singularity_iteration.core.api.item.BatteryTransfer;
import java.util.IdentityHashMap;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Independent SI compatibility boundary from R133/R135 public-call observations.
 * Storage belongs to the supplied provider; this adapter has no separate balance.
 * Boolean routing preserves measured SI behavior, not an inferred external API.
 */
public final class BatteryElectricAdapter {
    private BatteryElectricAdapter() { }

    public static ElectricItemWrapper asElectricItem(IBatteryItem battery) {
        return new ElectricItemWrapper(battery);
    }

    public static IBatteryItem asBatteryItem(IElectricItem item, IElectricItemManager manager) {
        return new BatteryItemWrapper(item, manager);
    }

    private static final ThreadLocal<IdentityHashMap<ItemStack, Boolean>> ACTIVE =
        ThreadLocal.withInitial(IdentityHashMap::new);

    /** Caller callbacks see staged items. Recursive public mutations are refused. */
    private static final class Scope implements AutoCloseable {
        private final ItemStack original;
        private final ItemStack before;
        private final ItemStack staged;
        private final IdentityHashMap<ItemStack, Boolean> owned = new IdentityHashMap<>();

        private Scope(ItemStack stack) {
            original = stack;
            track(original);
            before = original.copy();
            staged = before.copy();
            track(staged);
        }

        private void track(ItemStack stack) {
            if (owned.containsKey(stack)) return;
            if (ACTIVE.get().containsKey(stack))
                throw new IllegalStateException("Electric adapter callback reentry");
            owned.put(stack, Boolean.TRUE);
            ACTIVE.get().put(stack, Boolean.TRUE);
        }

        private <T> T query(ItemStack stack, Supplier<T> callback) {
            track(stack);
            var snapshot = stack.copy();
            T value = callback.get();
            if (!ItemStack.matches(snapshot, stack))
                throw new IllegalStateException("Electric provider mutated an item during a query");
            return value;
        }

        private void originalUnchanged() {
            if (!ItemStack.matches(before, original))
                throw new IllegalStateException("Electric provider changed the original item outside its staged call");
        }

        private void commit() {
            originalUnchanged();
            if (staged.getItem() != before.getItem() || staged.getCount() != before.getCount())
                throw new IllegalStateException("Electric provider changed the item or count");
            var patch = DataComponentPatch.builder();
            for (var component : before.getComponents())
                if (!staged.has(component.type())) patch.remove(component.type());
            for (var component : staged.getComponents()) patch.set(component);
            original.applyComponents(patch.build());
            if (!ItemStack.matches(original, staged))
                throw new IllegalStateException("Electric component commit did not preserve the staged item");
        }

        @Override public void close() {
            var active = ACTIVE.get();
            for (var stack : owned.keySet()) active.remove(stack);
            if (active.isEmpty()) ACTIVE.remove();
        }
    }

    private static boolean single(ItemStack stack) {
        return !stack.isEmpty() && stack.getCount() == 1;
    }

    private static <T> T query(ItemStack stack, java.util.function.Function<ItemStack, T> callback) {
        try (var scope = new Scope(stack)) {
            T value = scope.query(scope.staged, () -> callback.apply(scope.staged));
            scope.originalUnchanged();
            return value;
        }
    }

    /** Only the observed provider calls; policy and transaction checks live outside. */
    private interface Access {
        long capacity(ItemStack stack);
        long energy(ItemStack stack);
        long rate(ItemStack stack);
        long add(ItemStack stack, long amount);
        long remove(ItemStack stack, long amount);
    }

    private static Access nativeAccess(IBatteryItem battery) {
        return new Access() {
            // Public getMaxCharge keeps its measured no-argument query. Actual
            // transactions validate the native per-stack capacity and its stability.
            @Override public long capacity(ItemStack stack) { return battery.getMaxEnergy(stack); }
            @Override public long energy(ItemStack stack) { return battery.getEnergy(stack); }
            @Override public long rate(ItemStack stack) { return battery.getChargeRate(stack); }
            @Override public long add(ItemStack stack, long amount) { return battery.addEnergy(stack, amount); }
            @Override public long remove(ItemStack stack, long amount) { return battery.extractEnergy(stack, amount); }
        };
    }

    private static Access legacyAccess(IElectricItem item, IElectricItemManager manager) {
        return new Access() {
            @Override public long capacity(ItemStack stack) { return item.getMaxCharge(stack); }
            @Override public long energy(ItemStack stack) { return manager.getCharge(stack); }
            @Override public long rate(ItemStack stack) { return item.getTransferLimit(stack); }
            @Override public long add(ItemStack stack, long amount) {
                return manager.charge(stack, amount, unchangedTier(item, stack), false, false);
            }
            @Override public long remove(ItemStack stack, long amount) {
                return manager.discharge(stack, amount, unchangedTier(item, stack), false, false, true);
            }
        };
    }

    private static int unchangedTier(IElectricItem item, ItemStack stack) {
        var before = stack.copy();
        int tier = item.getTier(stack);
        if (!ItemStack.matches(before, stack))
            throw new IllegalStateException("Electric tier query changed the item");
        return tier;
    }

    /** Adds query purity and copy ownership to the independent transfer helper. */
    private static final class CheckedBattery implements IBatteryItem {
        private final Access access;
        private final Scope scope;
        private CheckedBattery(Access access, Scope scope) { this.access = access; this.scope = scope; }
        @Override public long getMaxEnergy() { throw new UnsupportedOperationException("A stack is required"); }
        @Override public long getMaxEnergy(ItemStack stack) { return scope.query(stack, () -> access.capacity(stack)); }
        @Override public long getEnergy(ItemStack stack) { return scope.query(stack, () -> access.energy(stack)); }
        @Override public long getChargeRate(ItemStack stack) { return scope.query(stack, () -> access.rate(stack)); }
        @Override public long addEnergy(ItemStack stack, long amount) { scope.track(stack); return access.add(stack, amount); }
        @Override public long extractEnergy(ItemStack stack, long amount) { scope.track(stack); return access.remove(stack, amount); }
        @Override public boolean isFull(ItemStack stack) { return getEnergy(stack) >= getMaxEnergy(stack); }
        @Override public boolean isEmpty(ItemStack stack) { return getEnergy(stack) <= 0; }
    }

    private static long transfer(ItemStack stack, Access access, long amount,
                                 boolean charging, boolean simulate, boolean rateLimited, boolean exact) {
        if (!single(stack) || amount <= 0 || ACTIVE.get().containsKey(stack)) return 0;
        try (var scope = new Scope(stack)) {
            var battery = new CheckedBattery(access, scope);
            long capacity = battery.getMaxEnergy(scope.staged);
            long before = battery.getEnergy(scope.staged);
            if (capacity <= 0 || before < 0 || before > capacity) {
                scope.originalUnchanged();
                return 0;
            }
            long offered = amount;
            if (rateLimited) offered = Math.min(offered, Math.max(0, battery.getChargeRate(scope.staged)));
            long moved;
            if (exact) {
                moved = BatteryTransfer.consume(scope.staged, battery, offered) ? offered : 0;
            } else {
                moved = BatteryTransfer.transfer(scope.staged, battery, offered, charging, simulate);
            }
            scope.originalUnchanged();
            if (battery.getMaxEnergy(scope.staged) != capacity || scope.staged.getItem() != scope.before.getItem()
                    || scope.staged.getCount() != 1)
                throw new IllegalStateException("Electric provider changed transfer capacity or item identity");
            long after = battery.getEnergy(scope.staged);
            scope.originalUnchanged();
            if (simulate) {
                if (!ItemStack.matches(scope.before, scope.staged))
                    throw new IllegalStateException("Electric simulation changed the staged item");
                return moved;
            }
            long delta = charging ? after - before : before - after;
            if (moved < 0 || moved > offered || delta != moved || after < 0 || after > capacity)
                throw new IllegalStateException("Electric transfer did not conserve its actual balance");
            if (moved == 0) return 0;
            scope.commit();
            return moved;
        }
    }

    public static class ElectricItemWrapper implements IElectricItem, IElectricItemManager {
        private final IBatteryItem battery;
        private final Access access;

        public ElectricItemWrapper(IBatteryItem battery) {
            this.battery = battery;
            this.access = nativeAccess(battery);
        }

        @Override public boolean canProvideEnergy(ItemStack stack) {
            return query(stack, item -> !battery.isEmpty(item));
        }
        @Override public long getMaxCharge(ItemStack stack) {
            return query(stack, item -> battery.getMaxEnergy());
        }
        @Override public int getTier(ItemStack stack) { return 1; }
        @Override public long getTransferLimit(ItemStack stack) {
            return query(stack, battery::getChargeRate);
        }
        @Override public long getCharge(ItemStack stack) { return query(stack, battery::getEnergy); }
        @Override public String getToolTip(ItemStack stack) {
            return query(stack, item -> battery.getEnergy(item) + " / " + battery.getMaxEnergy());
        }

        @Override public long charge(ItemStack stack, long amount, int tier, boolean b0, boolean b1) {
            return transfer(stack, access, amount, true, b1, b1 && !b0, false);
        }

        @Override public long discharge(ItemStack stack, long amount, int tier, boolean b0, boolean b1, boolean b2) {
            return transfer(stack, access, amount, false, b1 || !b2, b1 && !b0, false);
        }

        @Override public boolean canUse(ItemStack stack, long amount) {
            if (!single(stack) || amount < 0 || ACTIVE.get().containsKey(stack)) return false;
            return query(stack, item -> {
                long stored = battery.getEnergy(item), capacity = battery.getMaxEnergy(item);
                return stored >= amount && stored >= 0 && stored <= capacity;
            });
        }

        @Override public boolean use(ItemStack stack, long amount, LivingEntity entity) {
            if (!single(stack) || amount < 0 || ACTIVE.get().containsKey(stack)) return false;
            return amount == 0 || transfer(stack, access, amount, false, false, false, true) == amount;
        }

        @Override public void chargeFromArmor(ItemStack stack, LivingEntity entity) {
            // Measured compatibility: R133 native/vanilla equipment and R135's
            // 808 legacy/dual donor rows performed no callbacks or state writes.
            // This method deliberately owns no donor selection or automatic transfer.
            // Connected-client and arbitrary add-on behavior are outside that evidence.
        }
    }

    public static class BatteryItemWrapper implements IBatteryItem {
        private final IElectricItem item;
        private final IElectricItemManager manager;
        private final Access access;

        public BatteryItemWrapper(IElectricItem item, IElectricItemManager manager) {
            this.item = item;
            this.manager = manager;
            this.access = legacyAccess(item, manager);
        }

        @Override public long getMaxEnergy() {
            throw new UnsupportedOperationException("IBatteryItem.getMaxEnergy() requires an ItemStack for this legacy provider; use getMaxEnergy(stack).");
        }
        @Override public long getMaxEnergy(ItemStack stack) { return query(stack, item::getMaxCharge); }
        @Override public long getEnergy(ItemStack stack) { return query(stack, manager::getCharge); }
        @Override public long getChargeRate(ItemStack stack) { return query(stack, item::getTransferLimit); }
        @Override public boolean isFull(ItemStack stack) {
            return query(stack, value -> manager.getCharge(value) >= item.getMaxCharge(value));
        }
        @Override public boolean isEmpty(ItemStack stack) { return query(stack, value -> manager.getCharge(value) <= 0); }
        @Override public long addEnergy(ItemStack stack, long amount) {
            return transfer(stack, access, amount, true, false, false, false);
        }
        @Override public long extractEnergy(ItemStack stack, long amount) {
            return transfer(stack, access, amount, false, false, false, false);
        }
    }
}
