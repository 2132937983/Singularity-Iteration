// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory.blade;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.233 (SlashBlade collab B1): a collab blade as a NeoForge FE item energy storage (1 EU = 4 FE), so the Armory
 * (through {@code ArmoryItemEnergy}'s FE path) and any FE charger fill it. It only takes energy; the blade spends its EU
 * itself.
 */
public final class BladeEnergyStorage implements IEnergyStorage {
    public static final int FE_PER_EU = 4;

    private final ItemStack stack;
    private final CollabBlade kind;

    private BladeEnergyStorage(ItemStack stack, CollabBlade kind) {
        this.stack = stack;
        this.kind = kind;
    }

    /** The storage of a collab blade stack, or null for any other stack. */
    @Nullable
    public static BladeEnergyStorage of(ItemStack stack) {
        if (stack.getCount() != 1) return null;
        CollabBlade kind = CollabBlade.of(stack);
        return kind == null ? null : new BladeEnergyStorage(stack, kind);
    }

    private BladeCore core() {
        BladeCore c = CollabBlade.core(stack);
        return c == null ? BladeCore.fresh(kind) : c;
    }

    @Override
    public int receiveEnergy(int fe, boolean simulate) {
        if (fe <= 0) return 0;
        BladeCore c = core();
        long eu = Math.min(fe / FE_PER_EU, BladeResources.room(c, kind));
        if (eu <= 0) return 0;
        if (!simulate) CollabBlade.set(stack, c.withEu(c.eu() + eu));
        return (int) (eu * FE_PER_EU);
    }

    @Override
    public int extractEnergy(int fe, boolean simulate) { return 0; }

    @Override
    public int getEnergyStored() { return (int) Math.min(Integer.MAX_VALUE, core().eu() * FE_PER_EU); }

    @Override
    public int getMaxEnergyStored() { return (int) Math.min(Integer.MAX_VALUE, kind.euMax * FE_PER_EU); }

    @Override
    public boolean canExtract() { return false; }

    @Override
    public boolean canReceive() { return true; }
}
