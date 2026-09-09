package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.EUApi;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.IEUEnergyStorage;
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
 * 继承�?mio_icif_producer
 * 
 * ??��?��?? * 1. ?��??�电??�来?���?水�?��??，�?�出空气?��??
 * 2. ?????��?��?�学??��?��?�系�?�?0000EU）? * 3. �??��与侧?��??�可以�?�出??��?��??2EU/t�? * 4. 当接触面????��?��不是满电?��，�?��?��?��?�出给该?��?��
 */
@SuppressWarnings("null")
public class mio_icif_electrolyzer_elc extends mio_icif_producer {

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
        return new com.singularity_iteration.mio_icif.Menu.Producer.ElectrolyzerElcMenu(containerId, playerInventory, this);
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
        // �??��?��?��??��??        
        if (energyStorage.getAmount() < energyPerTick) {
            return false;
        }

        // �??��??�学??�是?��已满
        if (chemicalEnergy >= DEFAULT_CAPACITY) {
            return false;
        }

        // �??��输�?�槽?��?��??�水??��??
        ItemStack inputStack = itemHandler.getStackInSlot(INPUT_SLOT);
        if (inputStack.isEmpty() || !mio_icif_cells.isCellContainingFluid(inputStack, net.minecraft.world.level.material.Fluids.WATER)) {
            return false;
        }

        // �??��输出槽是?��?��以放??�空??��??        
        ItemStack outputStack = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (!outputStack.isEmpty()) {
            if (!mio_icif_cells.isEmptyCell(outputStack)) {
                return false;
            }
            if (outputStack.getCount() >= outputStack.getMaxStackSize()) {
                return false;
            }
        }

        return true;
    }

    /**
     * ??��?�电力?工�??
     * �???�电??��?�累积�?��?��??当达???200EU?���???�水??��??输出空气?��??     
     * */
    @Override
    protected void doWork() {
        // �???�电力?        
        long extracted = energyStorage.extract(energyPerTick, false);
        if (extracted <= 0) {
            isWorking = false;
            return;
        }

        isWorking = true;

        // 累积??��?��?��?�学??��?��??
        chemicalEnergy = Math.min(chemicalEnergy + extracted, DEFAULT_CAPACITY);
        energyAccumulator += extracted;

        // �??��?��?��达�?�电力?�?个水??��?????????????��??        
        while (energyAccumulator >= ENERGY_PER_WATER_CELL) {
            energyAccumulator -= ENERGY_PER_WATER_CELL;

            // �???��??个水??��??
            ItemStack inputStack = itemHandler.getStackInSlot(INPUT_SLOT);
            inputStack.shrink(1);
            if (inputStack.isEmpty()) {
                itemHandler.setStackInSlot(INPUT_SLOT, ItemStack.EMPTY);
            }

            ItemStack emptyCell = mio_icif_cells.getEmptyCellForStack(inputStack);
            if (emptyCell.isEmpty()) emptyCell = new ItemStack(mio_icif_cells.CELL_EMPTY.get());

            ItemStack outputStack = itemHandler.getStackInSlot(OUTPUT_SLOT);
            if (outputStack.isEmpty()) {
                itemHandler.setStackInSlot(OUTPUT_SLOT, emptyCell);
            } else {
                outputStack.grow(1);
            }

            // �???��?��?�槽已空气???�止循环
            if (itemHandler.getStackInSlot(INPUT_SLOT).isEmpty()) {
                break;
            }
        }
    }

    /**
     * 每tick?��?��??��??
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_electrolyzer_elc blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // �??��??�类???tick?��法�??�??????��?�接?��???工�?��?��?��??        
        mio_icif_producer.tick(level, pos, state, blockEntity);

        // 输出??�学??��?�相??�机?��
        blockEntity.outputChemicalEnergy();

        blockEntity.setChanged();
    }

    /**
     * 输出??�学??��?�相??�机?��
     * ??��????��?�满?��????��??�机?��输出??��??     */
    private void outputChemicalEnergy() {
        if (chemicalEnergy <= 0) {
            return;
        }

        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = worldPosition.relative(direction);
            IEUEnergyStorage adjacentStorage = level.getCapability(EUApi.SIDED, adjacentPos, direction.getOpposite());

            if (adjacentStorage != null && adjacentStorage.getCapacity() - adjacentStorage.getAmount() > 0) {
                // �??��?��??�机?��?��?��已�??        
                long containerSpace = adjacentStorage.getCapacity() - adjacentStorage.getAmount();
                if (containerSpace > 0) {
                    // 计算?�可以�?��?��????��??        
                    long energyToTransfer = Math.min(Math.min(chemicalEnergy, DEFAULT_MAX_EXTRACT), containerSpace);

                    if (energyToTransfer > 0) {
                        // 输出??��??
                        long received = adjacentStorage.receive(energyToTransfer, false);
                        if (received > 0) {
                            chemicalEnergy -= received;
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("ChemicalEnergy", chemicalEnergy);
        tag.putLong("EnergyAccumulator", energyAccumulator);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("ChemicalEnergy")) {
            chemicalEnergy = tag.getLong("ChemicalEnergy");
        }
        if (tag.contains("EnergyAccumulator")) {
            energyAccumulator = tag.getLong("EnergyAccumulator");
        }
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