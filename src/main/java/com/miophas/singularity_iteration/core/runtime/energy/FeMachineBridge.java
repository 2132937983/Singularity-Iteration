// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyStorageBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/** Standard NeoForge capability integration. No optional mod classes or global world caches. */
public final class FeMachineBridge {
    private final AbstractEnergyBlockEntity owner;
    private final FeLedger ledger;
    private final IEnergyStorage[] ports = new IEnergyStorage[7];
    private BlockCapabilityCache<IEnergyStorage, Direction> outputCache;
    private int idle;
    public FeMachineBridge(AbstractEnergyBlockEntity owner) {
        this.owner = owner;
        ledger = new FeLedger(owner.getEnergyStorageInternal(), () -> owner.getLevel() instanceof ServerLevel level ? level.getServer().getTickCount() : 0, this::active);
    }
    private boolean active() {
        if (owner.isRemoved() || !(owner.getLevel() instanceof ServerLevel level) || !level.getServer().isSameThread()) return false;
        var pos = owner.getBlockPos();
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk != null && level.shouldTickBlocksAt(net.minecraft.world.level.ChunkPos.asLong(pos))
            && chunk.getBlockEntity(pos, net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK) == owner;
    }
    private boolean input(@Nullable Direction side) {
        if (owner instanceof AbstractEnergyStorageBlockEntity box) return side != null && box.canConsumePowerFromSide(side);
        return !owner.isPowerSource() && owner.canConnect(side);
    }
    private boolean output(@Nullable Direction side) {
        if (side == null) return false;
        if (owner instanceof AbstractEnergyStorageBlockEntity box) return box.canProvidePowerFromSide(side);
        // Custom source prefabs may explicitly publish FE through supportsFeCapability().
        if (owner instanceof DemandEnergySource demand && (demand.outputFaces() & (1 << side.ordinal())) == 0) return false;
        return owner.isPowerSource() && owner.canConnect(side);
    }
    public IEnergyStorage port(@Nullable Direction side) {
        int index = side == null ? 6 : side.ordinal();
        if (ports[index] == null) ports[index] = ledger.port(() -> input(side), () -> output(side));
        return ports[index];
    }
    public FeLedger.OutputQuote quoteNativeOutput() { return ledger.quoteNativeOutput(); }
    /** Routed output shares exactly the native/item/FE allowance and persisted uncertain debit. */
    public int pushRouted(IEnergyStorage target, java.util.function.BooleanSupplier currentRoute) {
        return ledger.push(target, currentRoute);
    }
    public int pullRouted(IEnergyStorage source, Direction side, int lossFe, java.util.function.BooleanSupplier currentRoute) {
        return ledger.pull(source, lossFe, () -> input(side) && currentRoute.getAsBoolean());
    }
    public int uncertainOutput() { return ledger.uncertainOutput(); }
    public void loadUncertainOutput(int amount) { ledger.loadUncertainOutput(amount); }
    /** Shared whole-EU input used by optional energy-unit adapters. */
    public long receiveWholeEu(long amount, boolean simulate) {
        return ledger.receiveWholeEu(amount, simulate);
    }
    /** Shared whole-EU output used by optional energy-unit adapters. */
    public long extractWholeEu(long amount, boolean simulate) {
        return ledger.extractWholeEu(amount, simulate);
    }
    public void clear() { outputCache = null; idle = 0; }
    public void push() {
        // Controlled boxes use pushRouted after topology and packet-budget checks.
        if (owner.getEnergyStorageInternal().scexNetworkControlled()) return;
        if (!active() || !(owner instanceof AbstractEnergyStorageBlockEntity box)) return;
        if (idle > 0) { idle--; return; }
        Direction side = null;
        for (Direction candidate : Direction.values()) if (box.canProvidePowerFromSide(candidate)) { side = candidate; break; }
        if (side == null) return;
        if (!output(side) || !owner.getEnergyStorageInternal().isOutputEnabled()) return;
        var level = (ServerLevel)owner.getLevel(); var target = owner.getBlockPos().relative(side);
        if (outputCache == null || outputCache.level() != level || !outputCache.pos().equals(target)) {
            outputCache = BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, level, target, side.getOpposite(),
                () -> !owner.isRemoved() && owner.getLevel() == level, () -> idle = 0);
        }
        try {
            Direction outputSide = side;
            int amount = ledger.push(outputCache.getCapability(), () -> output(outputSide));
            if (amount == 0) idle = 7 + Math.floorMod(owner.getBlockPos().hashCode(), 5);
        } catch (RuntimeException failure) {
            // Unknown external commit is held in the persisted ledger, never sent a second time.
            idle = 20;
        }
    }
    public static boolean chargeable(ItemStack stack) {
        if (stack.isEmpty()) return false;
        IEnergyStorage cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return cap != null && cap.canReceive();
    }
    public static boolean dischargeable(ItemStack stack) {
        if (stack.isEmpty()) return false;
        IEnergyStorage cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return cap != null && cap.canExtract();
    }
    public void discharge(MachineItemHandler inventory, int slot) {
        if (slot < 0 || slot >= inventory.getSlots() || !active()) return;
        var kind = owner instanceof AbstractEnergyStorageBlockEntity ? MachineItemDischarging.InputKind.STORAGE : MachineItemDischarging.InputKind.MACHINE;
        if (MachineItemDischarging.dischargeSi(ledger, owner.getEnergyStorageInternal(), inventory, slot, kind)) return;
        ItemStack current = inventory.getStackInSlot(slot);
        if (current.isEmpty() || current.getCount() != 1 || ledger.receive(Integer.MAX_VALUE, true) == 0) return;
        ItemStack before = current.copy(), after = before.copy();
        IEnergyStorage cap = after.getCapability(Capabilities.EnergyStorage.ITEM);
        MachineItemDischarging.dischargeFe(ledger, owner.getEnergyStorageInternal(), inventory, slot, before, after, cap);
    }
    public boolean charge(MachineItemHandler inventory, int slot) {
        if (!active()) return false;
        if (StorageItemCharging.charge(ledger, owner.getEnergyStorageInternal(), inventory, slot)) return true;
        ItemStack before = inventory.getStackInSlot(slot).copy();
        if (before.isEmpty() || before.getCount() != 1) return false;
        ItemStack after = before.copy();
        IEnergyStorage cap = after.getCapability(Capabilities.EnergyStorage.ITEM);
        if (cap == null || !cap.canReceive()) return false;
        int offer = ledger.extractItemFe(Integer.MAX_VALUE, true);
        if (offer <= 0) return true;
        var balance = owner.getEnergyStorageInternal().scexNetworkQuote();
        var exactBalance = owner.getEnergyStorageInternal().scexExactAmount();
        int accepted = cap.receiveEnergy(offer, false);
        if (accepted < 0 || accepted > offer || after.getCount() != 1 || after.getItem() != before.getItem()
                || !ItemStack.matches(before, inventory.getStackInSlot(slot))
                || !balance.equals(owner.getEnergyStorageInternal().scexNetworkQuote())
                || !exactBalance.equals(owner.getEnergyStorageInternal().scexExactAmount())) return true;
        if (accepted == 0) return true;
        if (ledger.extractItemFe(accepted, true) != accepted) return true;
        if (ledger.extractItemFe(accepted, false) != accepted) return true;
        if (!inventory.scexCommitSlots(new int[]{slot}, new ItemStack[]{before}, new ItemStack[]{after}))
            throw new IllegalStateException("Owned charging slot changed during debit");
        return true;
    }
}
