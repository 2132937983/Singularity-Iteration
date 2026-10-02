package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.item.IItemAPI;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ItemCapability;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * How the Armory charges stored equipment, whatever energy system the item speaks.
 *
 * <p>Order: Singularity Iteration EU items, GregTech Modern electric items (EU), then the
 * NeoForge FE item capability - which also covers Mekanism (Joules are exposed as FE),
 * Modern Industrialization, Powah, Thermal and most other mods. All amounts are in EU;
 * FE is converted at the mod-wide 1 EU = 4 FE.
 */
public final class ArmoryItemEnergy {
    private ArmoryItemEnergy() {}

    public static final int FE_PER_EU = 4;

    /** EU the item can still take. */
    public static long room(ItemStack stack) {
        if (stack.isEmpty() || stack.getCount() != 1) return 0;
        IItemAPI api = MioIcifAPI.instance().getItemAPI();
        if (isSi(api, stack)) return Math.max(0, api.getBatteryCapacity(stack) - api.getBatteryStored(stack));
        Gt gt = Gt.of(stack);
        if (gt != null) return gt.room();
        var fe = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (fe != null && fe.canReceive()) return Math.max(0, (long) fe.getMaxEnergyStored() - fe.getEnergyStored()) / FE_PER_EU;
        return 0;
    }

    /** Charges up to {@code eu}; returns the EU actually taken (never more than offered). */
    public static long charge(ItemStack stack, long eu, boolean simulate) {
        if (eu <= 0 || stack.isEmpty() || stack.getCount() != 1) return 0;
        IItemAPI api = MioIcifAPI.instance().getItemAPI();
        if (isSi(api, stack)) {
            long took;
            if (api.isElectricArmor(stack)) took = api.chargeElectricArmor(stack, eu, simulate);
            else if (api.isElectricTool(stack)) took = api.chargeElectricTool(stack, eu, simulate);
            else took = api.chargeBattery(stack, eu, simulate);
            return clamp(took, eu);
        }
        Gt gt = Gt.of(stack);
        if (gt != null) return clamp(gt.charge(eu, simulate), eu);
        var fe = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (fe == null || !fe.canReceive()) return 0;
        int offer = (int) Math.min(Integer.MAX_VALUE / FE_PER_EU, eu) * FE_PER_EU;
        // Quote first, then move whole EU only, so no fraction of an EU is ever lost.
        int quoted = fe.receiveEnergy(offer, true) / FE_PER_EU * FE_PER_EU;
        if (quoted <= 0) return 0;
        if (simulate) return quoted / FE_PER_EU;
        int moved = fe.receiveEnergy(quoted, false);
        return clamp((moved + FE_PER_EU - 1) / FE_PER_EU, eu);
    }

    private static long clamp(long value, long max) {
        return Math.max(0, Math.min(value, max));
    }

    private static boolean isSi(IItemAPI api, ItemStack stack) {
        return api.isElectricArmor(stack) || api.isElectricTool(stack) || api.isBattery(stack);
    }

    /** GregTech Modern IElectricItem, reached by reflection (optional dependency). */
    private record Gt(Object item) {
        private static boolean resolved;
        @Nullable private static ItemCapability<Object, Void> capability;
        private static Method charge, getCharge, getMaxCharge, chargeable;

        @SuppressWarnings("unchecked")
        @Nullable
        static Gt of(ItemStack stack) {
            if (!resolved) {
                resolved = true;
                try {
                    Class<?> holder = Class.forName("com.gregtechceu.gtceu.api.capability.GTCapability");
                    Class<?> api = Class.forName("com.gregtechceu.gtceu.api.capability.IElectricItem");
                    capability = (ItemCapability<Object, Void>) holder.getField("CAPABILITY_ELECTRIC_ITEM").get(null);
                    charge = api.getMethod("charge", long.class, int.class, boolean.class, boolean.class);
                    getCharge = api.getMethod("getCharge");
                    getMaxCharge = api.getMethod("getMaxCharge");
                    chargeable = api.getMethod("chargeable");
                } catch (ReflectiveOperationException | LinkageError | ClassCastException absent) {
                    capability = null;
                }
            }
            if (capability == null) return null;
            Object item = stack.getCapability(capability);
            return item == null ? null : new Gt(item);
        }

        long room() {
            try {
                if (!(boolean) chargeable.invoke(item)) return 0;
                return Math.max(0, (long) getMaxCharge.invoke(item) - (long) getCharge.invoke(item));
            } catch (ReflectiveOperationException e) {
                return 0;
            }
        }

        long charge(long eu, boolean simulate) {
            try {
                // Armory acts as a top-tier charger and ignores the per-tick transfer limit;
                // its own maintenance budget already paces the charge.
                return (long) charge.invoke(item, eu, Integer.MAX_VALUE, true, simulate);
            } catch (ReflectiveOperationException e) {
                Singularity_Iteration.LOGGER.debug("GT item charge failed", e);
                return 0;
            }
        }
    }
}
