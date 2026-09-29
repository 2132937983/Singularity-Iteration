package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.api.energy.storage.EUApi;
import com.miophas.singularity_iteration.core.api.energy.storage.IEUEnergyStorage;
import com.miophas.singularity_iteration.core.runtime.processing.RecipeSlots;
import com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank;
import java.util.Optional;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * ?���??��?��??��?��?�类
 * 继承�?AbstractProcessingMachineBlockEntity
 * 
 * ??��?��?? * 1. ?��??�电??�来?���?水�?��??，�?�出空气?��??
 * 2. ?????��?��?�学??��?��?�系�?�?0000EU）? * 3. �??��与侧?��??�可以�?�出??��?��??2EU/t�? * 4. 当接触面????��?��不是满电?��，�?��?��?��?�出给该?��?��
 */
@SuppressWarnings("null")
public class mio_icif_electrolyzer_elc extends AbstractProcessingMachineBlockEntity {

    // 槽位?��?��?��??�?须�?�GUI中�??槽位?�索引�???���?
    public static final int INPUT_SLOT = 0;   // 输�?��??- 水�?��??    
    public static final int OUTPUT_SLOT = 1;  // 输出�?- 空气?��??
    // ??�置
    // ??�置�?对�??IC2 ??��??�?    
    public static final long DEFAULT_CAPACITY = 400L;        // 10 EU/t ?? 20 ticks = 200 EU）??????��?��??400
    public static final long DEFAULT_MAX_RECEIVE = 32L;      // ?��?��?��??��?��?? 32EU/t
    public static final long DEFAULT_MAX_EXTRACT = 32L;      // 输出?��??��?��?? 32EU/t
    public static final long ENERGY_PER_WATER_CELL = 200L;   // ?���?�?个水??��?????�?00EU
    public static final long DEFAULT_ENERGY_PER_TICK = 10L; // 每tick�?�??0EU (??�IC2?���??��????????��??

    // ??�学??��?��??    
    protected long chemicalEnergy = 0;

    // ??��?�累积器�??��于追踪�?�时间???�水??��??�?    
    protected long energyAccumulator = 0;
    // Legacy accumulator energy was already in ChemicalEnergy or had already been emitted.
    // It still owes cell consumption but must not be published as chemical energy a second time.
    protected long legacyCellCredit;
    private long uncertainOutput;
    private boolean scexProcessing, scexOutputting;

    public mio_icif_electrolyzer_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.ELECTROLYZER.get());
    }

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .input(1)
        .output(1)
        .build();

    public mio_icif_electrolyzer_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            20,
            LAYOUT,
            DEFAULT_ENERGY_PER_TICK,
            CableTier.LV);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.electrolyzer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.miophas.singularity_iteration.common.menu.producer.ElectrolyzerElcMenu(containerId, playerInventory, this);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT_SLOT -> mio_icif_cells.isCellContainingFluid(stack, net.minecraft.world.level.material.Fluids.WATER);
            case OUTPUT_SLOT -> false; // 输出槽位?��??许�?�动?��???
            default -> false;
        };
    }

    @Override
    protected int[] getSlotsForDirection(Direction side) {
        // ?????�方??��?�可以访?��?????�槽位?        
        return new int[]{INPUT_SLOT, OUTPUT_SLOT};
    }

    @Override
    protected int[] getInputSlots() {
        return new int[]{INPUT_SLOT};
    }

    @Override
    protected int[] getOutputSlots() {
        return new int[]{OUTPUT_SLOT};
    }

    @Override
    protected boolean canInsertItem(int slot, ItemStack stack, @Nullable Direction side) {
        if (!isItemValidForSlot(slot, stack)) {
            return false;
        }
        // ?��??��?��?�槽?��以�?��??        
        return slot == INPUT_SLOT;
    }

    @Override
    protected int getBatterySlot() {
        return -1; // ?���??��没�?�电池槽
    }

    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        // ?��??��?�出槽可以�?��??        
        return slot == OUTPUT_SLOT;
    }

    /**
     * �??��?��?��?��?��?��以工�?     * ?��事件??     * 1. ??�电力??????��?��?��?�电池�??     * 2. 输�?�槽??�水??��??     * 3. 输出槽可以放??�空??��??
     * 4. ??�学??�未�?     */
    @Override
    protected boolean canWork() {
        if (scexProcessing || scexOutputting || uncertainOutput > 0 || prepareCell().isEmpty()) return false;
        long freshNeeded = ENERGY_PER_WATER_CELL - Math.min(ENERGY_PER_WATER_CELL, legacyCellCredit);
        if (freshNeeded > Math.max(0, DEFAULT_CAPACITY - chemicalEnergy)) return false;
        return energyAccumulator >= freshNeeded || apiGetStoredEnergy() > 0;
    }

    protected Optional<RecipeSlots.Prepared> prepareCell() {
        var input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) return Optional.empty();
        var single = input.copyWithCount(1);
        if (!mio_icif_cells.isCellContainingFluid(single, net.minecraft.world.level.material.Fluids.WATER)) return Optional.empty();
        var contents = mio_icif_cells.getCellFluid(single);
        if (contents.isEmpty()) return Optional.empty();
        var empty = mio_icif_cells.getEmptyCellForStack(single);
        if (empty.isEmpty() || empty.getCount() != 1) return Optional.empty();
        return RecipeSlots.prepare(itemHandler, INPUT_SLOT, 1, new int[]{OUTPUT_SLOT}, List.of(empty));
    }

    /**
     * ??��?�电力?工�??
     * �???�电??��?�累积�?��?��??当达???200EU?���???�水??��??输出空气?��??     
     * */
    @Override
    protected void doWork() {
        if (!canWork()) { stopWork(); return; }
        var prepared = prepareCell();
        if (prepared.isEmpty()) { stopWork(); return; }
        scexProcessing = true;
        try {
            long legacyUsed = Math.min(ENERGY_PER_WATER_CELL, legacyCellCredit);
            long freshNeeded = ENERGY_PER_WATER_CELL - legacyUsed;
            if (energyAccumulator < freshNeeded) {
                long debit = Math.min(freshNeeded - energyAccumulator,
                    Math.min(DEFAULT_ENERGY_PER_TICK, Math.max(0, apiGetStoredEnergy())));
                if (debit <= 0 || apiUseEnergy(debit, true) != debit) { stopWork(); return; }
                long paid = apiUseEnergy(debit, false);
                if (paid < 0 || paid > debit) throw new IllegalStateException("Invalid owned electrolyzer debit");
                energyAccumulator += paid;
                ContainerToTank.markUnsaved(this);
            }
            isWorking = true;
            if (energyAccumulator < freshNeeded) return;
            long oldChemical = chemicalEnergy, oldCredit = energyAccumulator, oldLegacy = legacyCellCredit;
            chemicalEnergy += freshNeeded; energyAccumulator -= freshNeeded; legacyCellCredit -= legacyUsed;
            progress = 0;
            // All account state is visible before owned inventory notifications.
            if (!prepared.get().commit()) {
                chemicalEnergy = oldChemical; energyAccumulator = oldCredit; legacyCellCredit = oldLegacy;
                stopWork();
            }
            ContainerToTank.markUnsaved(this);
        } finally { scexProcessing = false; }
    }

    @Override
    protected void updateProgress() {
        // The paid credit, rather than the generic scheduler, defines progress.
        long legacy = Math.min(ENERGY_PER_WATER_CELL, legacyCellCredit);
        long fresh = Math.min(ENERGY_PER_WATER_CELL - legacy, energyAccumulator);
        progress = (int) ((legacy + fresh) * 20 / ENERGY_PER_WATER_CELL);
    }

    /**
     * 每tick?��?��??��??
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_electrolyzer_elc blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // �??��??�类???tick?��法�??�??????��?�接?��???工�?��?��?��??        
        AbstractProcessingMachineBlockEntity.tick(level, pos, state, blockEntity);

        // 输出??�学??��?�相??�机?��
        blockEntity.outputChemicalEnergy();


    }

    /**
     * 输出??�学??��?�相??�机?��
     * ??��????��?�满?��????��??�机?��输出??��??     */
    private void outputChemicalEnergy() {
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread()
                || scexOutputting || scexProcessing || uncertainOutput > 0 || chemicalEnergy <= 0) return;
        long budget = DEFAULT_MAX_EXTRACT;
        for (Direction direction : Direction.values()) {
            if (budget <= 0 || chemicalEnergy <= 0) break;
            var adjacent = worldPosition.relative(direction);
            if (!server.getChunkSource().hasChunk(adjacent.getX() >> 4, adjacent.getZ() >> 4)) continue;
            var target = server.getCapability(EUApi.SIDED, adjacent, direction.getOpposite());
            if (target == null || target == energyStorage) continue;
            long capacity = target.getCapacity(), stored = target.getAmount();
            if (stored < 0 || capacity <= stored) continue;
            long delivered = offerChemical(target, Math.min(budget, capacity - stored));
            budget -= delivered;
            if (delivered > 0) {
                var neighbor = server.getBlockEntity(adjacent);
                if (neighbor != null) ContainerToTank.markUnsaved(neighbor);
            }
        }
    }

    protected long offerChemical(IEUEnergyStorage target, long limit) {
        if (scexOutputting || scexProcessing || uncertainOutput > 0 || limit <= 0 || chemicalEnergy <= 0) return 0;
        long offered = Math.min(chemicalEnergy, limit);
        scexOutputting = true;
        chemicalEnergy -= offered; uncertainOutput = offered;
        ContainerToTank.markUnsaved(this);
        try {
            long accepted = target.receive(offered, false);
            if (accepted < 0 || accepted > offered) throw new IllegalStateException("Invalid electrolyzer target acceptance");
            chemicalEnergy += offered - accepted; uncertainOutput = 0;
            ContainerToTank.markUnsaved(this);
            return accepted;
        } finally { scexOutputting = false; }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("ChemicalEnergy", chemicalEnergy);
        tag.putLong("EnergyAccumulator", energyAccumulator);
        tag.putInt("scex_electrolysis_version", 1);
        tag.putLong("scex_legacy_cell_credit", legacyCellCredit);
        tag.putLong("scex_uncertain_output", uncertainOutput);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        chemicalEnergy = Math.max(0, tag.getLong("ChemicalEnergy"));
        if (tag.contains("scex_electrolysis_version")) {
            energyAccumulator = Math.max(0, tag.getLong("EnergyAccumulator"));
            legacyCellCredit = Math.max(0, tag.getLong("scex_legacy_cell_credit"));
        } else {
            energyAccumulator = 0;
            legacyCellCredit = Math.max(0, tag.getLong("EnergyAccumulator"));
        }
        uncertainOutput = Math.max(0, tag.getLong("scex_uncertain_output"));
        updateProgress();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = super.getUpdateTag(registries); saveAdditional(tag, registries); return tag;
    }

    // ==================== Getter ?���? ====================

    public long getChemicalEnergy() {
        return chemicalEnergy;
    }

    public long getMaxChemicalEnergy() {
        return DEFAULT_CAPACITY;
    }

    public boolean isCharging() {
        return isWorking;
    }

    public boolean isDischarging() {
        return chemicalEnergy > 0;
    }
}