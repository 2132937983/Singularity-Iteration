// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids;
import com.miophas.singularity_iteration.common.integration.ae2.AE2Compat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 0.1.10.27 (X2): opens the container of a settlement slot as a {@link SettlementStorage}, in this order: an AE2 storage
 * cell (only when AE2 is loaded; the AE2 code lives in {@code common.integration.ae2} and is reached by reflection, so this
 * class loads without AE2), an item with an item handler capability (shulker boxes through NeoForge, backpacks of other
 * mods), a fluid container holding UU-matter. Any failure of another mod's storage leaves the slot unusable for that action
 * instead of breaking the menu.
 */
public final class SettlementStorages {
    private SettlementStorages() {}

    public static final int NONE = 0, ITEMS = 1, AE2_CELL = 2, FLUID = 3;

    /** The AE2 side: implemented by {@code integration.ae2.Ae2SettlementCells}. */
    public interface Opener {
        boolean handles(ItemStack stack);

        /** The storage of a cell; {@code commit} persists it and calls {@code changed}. */
        SettlementStorage open(ItemStack stack, Runnable changed);
    }

    private static Opener ae2;
    private static boolean ae2Tried;

    private static Opener ae2() {
        if (ae2Tried) return ae2;
        ae2Tried = true;
        if (!AE2Compat.isAE2Loaded()) return null;
        try {
            ae2 = (Opener) Class.forName("com.miophas.singularity_iteration.common.integration.ae2.Ae2SettlementCells")
                .getDeclaredConstructor().newInstance();
        } catch (Throwable t) {
            Singularity_Iteration.LOGGER.warn("[Exchange] AE2 storage cells are not available for settlement: {}", t.toString());
            ae2 = null;
        }
        return ae2;
    }

    private static Fluid uuFluid() { return mio_icif_fluids.UUMATTER.get(); }

    /** The kind of container a stack is for a settlement slot ({@link #NONE} when it is none). */
    public static int kind(ItemStack stack) {
        if (stack.isEmpty() || stack.getCount() != 1) return NONE;
        Opener cells = ae2();
        if (cells != null) {
            try {
                if (cells.handles(stack)) return AE2_CELL;
            } catch (LinkageError | RuntimeException e) {
                Singularity_Iteration.LOGGER.warn("[Exchange] AE2 storage cells disabled for settlement: {}", e.toString());
                ae2 = null;
            }
        }
        try {
            if (stack.getCapability(Capabilities.ItemHandler.ITEM) != null) return ITEMS;
            if (FluidUtil.getFluidContained(stack).map(f -> f.getFluid().isSame(uuFluid())).orElse(false)) return FLUID;
        } catch (RuntimeException e) {
            return NONE;
        }
        return NONE;
    }

    /** True for a stack that the settlement slot takes. */
    public static boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && kind(stack.copyWithCount(1)) != NONE;
    }

    /** The storage of the stack in {@code container} slot {@code index}, or null. */
    public static SettlementStorage open(Container container, int index) {
        ItemStack stack = container.getItem(index);
        int kind = kind(stack);
        try {
            return switch (kind) {
                case AE2_CELL -> ae2() == null ? null : ae2().open(stack, container::setChanged);
                case ITEMS -> {
                    IItemHandler h = stack.getCapability(Capabilities.ItemHandler.ITEM);
                    yield h == null ? null : new HandlerStorage(h, container);
                }
                case FLUID -> new FluidStorage(container, index);
                default -> null;
            };
        } catch (LinkageError | RuntimeException e) {
            Singularity_Iteration.LOGGER.warn("[Exchange] settlement container could not be opened: {}", e.toString());
            return null;
        }
    }

    // ------------------------------------------------------------------ item handler (shulker box, backpack)

    static final class HandlerStorage implements SettlementStorage {
        private final IItemHandler h;
        private final Container container;

        HandlerStorage(IItemHandler h, Container container) {
            this.h = h;
            this.container = container;
        }

        @Override
        public int kind() { return ITEMS; }

        @Override
        public List<Entry> items() {
            List<ItemStack> protos = new ArrayList<>();
            List<long[]> counts = new ArrayList<>();
            for (int i = 0; i < h.getSlots(); i++) {
                ItemStack s = h.getStackInSlot(i);
                if (s.isEmpty()) continue;
                int at = -1;
                for (int k = 0; k < protos.size() && at < 0; k++) if (ItemStack.isSameItemSameComponents(protos.get(k), s)) at = k;
                if (at < 0) { protos.add(s.copyWithCount(1)); counts.add(new long[] {0}); at = protos.size() - 1; }
                counts.get(at)[0] += s.getCount();
            }
            List<Entry> out = new ArrayList<>(protos.size());
            for (int k = 0; k < protos.size(); k++) out.add(new Entry(protos.get(k), counts.get(k)[0]));
            return out;
        }

        @Override
        public long extract(ItemStack proto, long amount, boolean simulate) {
            long left = amount;
            for (int i = 0; i < h.getSlots() && left > 0; i++) {
                ItemStack s = h.getStackInSlot(i);
                if (s.isEmpty() || !ItemStack.isSameItemSameComponents(s, proto)) continue;
                if (simulate) {
                    // handlers may give at most a stack per call; the slot's count is what a real extraction reaches
                    ItemStack probe = h.extractItem(i, (int) Math.min(left, s.getCount()), true);
                    if (!probe.isEmpty()) left -= Math.min(left, s.getCount());
                    continue;
                }
                while (left > 0) {
                    ItemStack got = h.extractItem(i, (int) Math.min(left, Integer.MAX_VALUE), false);
                    if (got.isEmpty()) break;
                    left -= got.getCount();
                }
            }
            return amount - left;
        }

        @Override
        public long insert(ItemStack proto, long amount, boolean simulate) {
            long left = amount;
            // first onto stacks of the same item, then into empty slots
            for (int pass = 0; pass < 2 && left > 0; pass++) {
                for (int i = 0; i < h.getSlots() && left > 0; i++) {
                    ItemStack s = h.getStackInSlot(i);
                    if (pass == 0 ? s.isEmpty() || !ItemStack.isSameItemSameComponents(s, proto) : !s.isEmpty()) continue;
                    int max = (int) Math.min(left, Math.max(1, Math.max(proto.getMaxStackSize(), h.getSlotLimit(i))));
                    ItemStack rest = h.insertItem(i, proto.copyWithCount(max), simulate);
                    left -= max - rest.getCount();
                }
            }
            return amount - left;
        }

        @Override
        public long uu() { return 0; }

        @Override
        public long extractUu(long mb, boolean simulate) { return 0; }

        @Override
        public void commit() { container.setChanged(); }
    }

    // ------------------------------------------------------------------ fluid container of UU-matter

    static final class FluidStorage implements SettlementStorage {
        private final Container container;
        private final int index;
        private ItemStack result;

        FluidStorage(Container container, int index) {
            this.container = container;
            this.index = index;
        }

        private IFluidHandlerItem handler() {
            return FluidUtil.getFluidHandler(container.getItem(index).copyWithCount(1)).orElse(null);
        }

        @Override
        public int kind() { return FLUID; }

        @Override
        public List<Entry> items() { return List.of(); }

        @Override
        public long extract(ItemStack proto, long amount, boolean simulate) { return 0; }

        @Override
        public long insert(ItemStack proto, long amount, boolean simulate) { return 0; }

        @Override
        public long uu() {
            IFluidHandlerItem fh = handler();
            if (fh == null) return 0;
            return fh.drain(new FluidStack(uuFluid(), Integer.MAX_VALUE), IFluidHandler.FluidAction.SIMULATE).getAmount();
        }

        @Override
        public long extractUu(long mb, boolean simulate) {
            IFluidHandlerItem fh = handler();
            if (fh == null || mb <= 0) return 0;
            FluidStack got = fh.drain(new FluidStack(uuFluid(), (int) Math.min(Integer.MAX_VALUE, mb)),
                simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
            if (!simulate && !got.isEmpty()) result = fh.getContainer();
            return got.getAmount();
        }

        @Override
        public void commit() {
            if (result != null) container.setItem(index, result);
            result = null;
            container.setChanged();
        }
    }
}
