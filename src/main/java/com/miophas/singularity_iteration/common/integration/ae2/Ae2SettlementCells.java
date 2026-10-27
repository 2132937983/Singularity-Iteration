// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.integration.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.StorageCell;
import com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids;
import com.miophas.singularity_iteration.common.exchange.SettlementStorage;
import com.miophas.singularity_iteration.common.exchange.SettlementStorages;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * 0.1.10.27 (X2): AE2 storage cells in a settlement slot (proposal Q4: cells in a slot, no ME network link). Item cells
 * give their items, fluid cells their UU-matter (AE2 counts fluids in mB). Created by reflection from
 * {@link SettlementStorages} only when AE2 is loaded; nothing else refers to this class.
 */
public final class Ae2SettlementCells implements SettlementStorages.Opener {
    @Override
    public boolean handles(ItemStack stack) {
        return StorageCells.isCellHandled(stack);
    }

    @Override
    public SettlementStorage open(ItemStack stack, Runnable changed) {
        StorageCell cell = StorageCells.getCellInventory(stack, null);
        return cell == null ? null : new CellStorage(cell, changed);
    }

    private static final class CellStorage implements SettlementStorage {
        private final StorageCell cell;
        private final Runnable changed;

        CellStorage(StorageCell cell, Runnable changed) {
            this.cell = cell;
            this.changed = changed;
        }

        private static AEFluidKey uuKey() { return AEFluidKey.of(mio_icif_fluids.UUMATTER.get()); }

        @Override
        public int kind() { return SettlementStorages.AE2_CELL; }

        @Override
        public List<Entry> items() {
            List<Entry> out = new ArrayList<>();
            for (Object2LongMap.Entry<AEKey> e : cell.getAvailableStacks()) {
                if (e.getKey() instanceof AEItemKey key && e.getLongValue() > 0) out.add(new Entry(key.toStack(1), e.getLongValue()));
            }
            return out;
        }

        @Override
        public long extract(ItemStack proto, long amount, boolean simulate) {
            if (proto.isEmpty() || amount <= 0) return 0;
            return cell.extract(AEItemKey.of(proto), amount, simulate ? Actionable.SIMULATE : Actionable.MODULATE, IActionSource.empty());
        }

        @Override
        public long insert(ItemStack proto, long amount, boolean simulate) {
            if (proto.isEmpty() || amount <= 0) return 0;
            return cell.insert(AEItemKey.of(proto), amount, simulate ? Actionable.SIMULATE : Actionable.MODULATE, IActionSource.empty());
        }

        @Override
        public long uu() {
            return cell.getAvailableStacks().get(uuKey());
        }

        @Override
        public long extractUu(long mb, boolean simulate) {
            if (mb <= 0) return 0;
            return cell.extract(uuKey(), mb, simulate ? Actionable.SIMULATE : Actionable.MODULATE, IActionSource.empty());
        }

        @Override
        public void commit() {
            cell.persist();
            changed.run();
        }
    }
}
