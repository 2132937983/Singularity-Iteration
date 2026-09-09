package com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.HuGenerator;

import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.MachineItemHandler;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities;
import com.singularity_iteration.mio_icif.api.machine.IHeatGeneratorBlock;
import com.singularity_iteration.mio_icif.api.machine.IBurnControl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * ??�交?��?��?��??��?��??
 * ??��????�交?��?���???��?�却?????�岩�?转�?�为??��??
 *
 * ?��??��??
 * - 两个液�?�槽：�?��?�槽位???��?�却???/岩�??）�?��?�出槽位????�却�???�岩岩�??�?
 * - 10个�?�交?��?��槽位?��?�个���??0HU/t??��??，�??�?00HU/t
 * - �?000mB??��?�却???�?0kHU+??�却�?
 * - �?000mB岩�??�?0kHU+??�岩岩�??
 * - ??��?�只??��?��??�??��输出
 * - ??��?��??�?上�?��?��??�?
 */
@SuppressWarnings("null")
public class mio_icif_heat_source_fluid extends com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.mio_icif_HeatU_Block implements WorldlyContainer, IHeatGeneratorBlock, IBurnControl {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .extra(2)
        .output(2)
        .heatConductor(10)
        .extra(3)
        .build();

    // 槽位?��?��?��????? builder �??��顺�?��?�EXTRA=0-1, OUTPUT=2-3, HEAT_CONDUCTOR=4-13, EXTRA=14-16�?
    public static final int INPUT_FLUID_BUCKET_SLOT = 0;    // 输�?�液体�????��??�?(EXTRA)
    public static final int INPUT_EMPTY_BUCKET_SLOT = 1;    // 输�?�空气?/空气?��??输出�? (EXTRA)
    public static final int OUTPUT_FLUID_BUCKET_SLOT = 2;   // 输出液�?��????��??�?(OUTPUT)
    public static final int OUTPUT_FULL_BUCKET_SLOT = 3;    // 输出满桶/充满?��??�? (OUTPUT)
    public static final int HEAT_CONDUCTOR_START = 4;       // ??�交?��?��槽起�?(EXTRA)
    public static final int HEAT_CONDUCTOR_COUNT = 10;      // ??�交?��?��槽数据?
    public static final int EXTRA_SLOT_START = 14;          // 额�?��?��??槽起�?(EXTRA)
    public static final int EXTRA_SLOT_COUNT = 3;           // 额�?��?��??槽数据?
    public static final int TOTAL_SLOTS = 17;               // ??�槽位数

    // ??��?��?�置
    public static final int HEAT_PER_CONDUCTOR = 10;        // 每个??�交?��?��增�??10HU/t
    public static final int MAX_HEAT_OUTPUT = 100;          // ???�?00HU/t
    public static final int HEAT_CAPACITY = 50000;          // ??��?��?��?��?��??50kHU
    public static final int HEAT_WARNING_THRESHOLD = 40000; // ??��?�警??��??�??0kHU
    public static final int HU_PER_BUCKET = 20000;          // 每桶产�??20kHU
    public static final int FLUID_PER_OPERATION = 1000;     // 每次??��?��??�??000mB
    public static final int EXPLOSION_WARNING_TICKS = 1200; // 1?????? = 1200 ticks

    // �?体�?�置
    public static final int FLUID_CAPACITY = 2000;          // 每个�?000mB容纳??

    // ??��??存�??
    protected MachineItemHandler itemHandler;

    // 输�?��??体�??- ?��??�岩�???��?��?�却�?
    protected final FluidTank inputTank;

    // 输出�?体�??- 存�?��?�却液�?��?�岩岩�??
    protected final FluidTank outputTank;

    // ??��?��?��?��??使用??��??heatStorage�?

 // 褰擄??戒氦???????缂擄???
    private int conductorCount = 0;

    // 当�?��?�出??��??
    private int currentHeatOutput = 0;

    // ?��?���??��工�??
    private boolean isWorking = false;

    // �???��????�计?��?���?当�?��?��??�?警�?��????�时间?始计?���?
    private int overheatTimer = 0;

    /**
     * ?????�函�?
     */
    public mio_icif_heat_source_fluid(BlockPos pos, BlockState state) {
        super(mio_icif_block_entities.HEAT_SOURCE_FLUID.get(), pos, state, HEAT_CAPACITY, 0, MAX_HEAT_OUTPUT, 20, 1000, 0);

        // ??��?��?�槽位置??�???��?��??�?????��
        this.slotLayout = LAYOUT;
        this.itemHandler = createItemHandler(LAYOUT);
        this.itemHandler.setValidator((slot, stack, slotType) -> mio_icif_heat_source_fluid.this.isItemValidForSlot(slot, stack));

        // ??��?��?��?��?��??体槽 - ?��??�岩�???��?��?�却�?
        this.inputTank = new FluidTank(FLUID_CAPACITY, fluidStack ->
            fluidStack.getFluid() == Fluids.LAVA ||
            fluidStack.getFluid() == mio_icif_fluids.HOTCOOLANT.get());

        // ??��?��?��?�出�?体槽 - ?��??��?�却液�?��?�岩岩�??
        this.outputTank = new FluidTank(FLUID_CAPACITY, fluidStack ->
            fluidStack.getFluid() == mio_icif_fluids.COOLANT.get() ||
            fluidStack.getFluid() == mio_icif_fluids.PAHOEHOELAVA.get());

        // ??��?��?��?�交?��?��?���?
        updateConductorCount();
    }

    /**
     * ?��?��??�交?��?��?��???
     */
    private void updateConductorCount() {
        int count = 0;
        for (int i = 0; i < HEAT_CONDUCTOR_COUNT; i++) {
            if (!itemHandler.getStackInSlot(HEAT_CONDUCTOR_START + i).isEmpty()) {
                count++;
            }
        }
        this.conductorCount = count;
        this.currentHeatOutput = Math.min(count * HEAT_PER_CONDUCTOR, MAX_HEAT_OUTPUT);
    }

    /**
     * 每tick?��?��??��??
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_heat_source_fluid blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // ?��?��??�交?��?��?��??��???��家可??�动????��?????�出�?
        blockEntity.updateConductorCount();

        // �????输�?�液体�??
        blockEntity.handleInputFluidSlot();

        // �????输出液�?��??
        blockEntity.handleOutputFluidSlot();

        // �??????�交换?
        blockEntity.processHeatExchange();

        // 输出??��??
        blockEntity.outputHeat();

        // �??��?��?���???��??�?
        blockEntity.checkOverheat();

        // ?��?��工�?�状态??
        boolean wasWorking = blockEntity.isWorking;
        blockEntity.isWorking = blockEntity.heatStorage.getHeatStored() > 0 || blockEntity.isProcessingFluid();

        // ?��?��?��??�状态??
        if (wasWorking != blockEntity.isWorking) {
            BlockState newState = state.setValue(
                com.singularity_iteration.mio_icif.Blocks.HUGenerator.mio_icif_block_heat_source_fluid.ACTIVE,
                blockEntity.isWorking);
            level.setBlock(pos, newState, 3);
        }

        blockEntity.setChanged();
    }

    /**
     * �??��?��?���??���????�?�?
     */
    private boolean isProcessingFluid() {
        return inputTank.getFluidAmount() >= FLUID_PER_OPERATION &&
               outputTank.getFluidAmount() + FLUID_PER_OPERATION <= outputTank.getCapacity() &&
               conductorCount > 0;
    }

    /**
     * �??��?��?��??��?��?�请�?�?�??��??�设置????�???��?��??
     */
    @SuppressWarnings("unused")
    private boolean hasHeatRequest() {
        Direction facing = getBlockState().getValue(
            net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        BlockPos adjacentPos = worldPosition.relative(facing);

        IMioIcifCapabilities.IHeatStorage adjacentHeat = level.getCapability(
                IMioIcifCapabilities.HEAT_STORAGE_BLOCK, adjacentPos, facing.getOpposite());
        if (adjacentHeat == null) {
            adjacentHeat = MioIcifAPI.instance().getCapabilities().adaptHeatStorage(
                level.getBlockEntity(adjacentPos));
        }

        return adjacentHeat != null && adjacentHeat.canReceiveHeat();
    }

    /**
     * �??????�交换?- ?���???��?��?��??体�?��?�交?��?��就产??��?��??
     */
    private void processHeatExchange() {
        com.singularity_iteration.mio_icif.Singularity_Iteration.LOGGER.info(
            "[HeatSourceFluid] processHeatExchange tick=" + level.getGameTime() + " pos=" + worldPosition +
            " conductorCount=" + conductorCount + " inputAmount=" + inputTank.getFluidAmount() +
            " outputAmount=" + outputTank.getFluidAmount() + " heatStored=" + heatStorage.getHeatStored());
        // �??��?��?��??��?�交?��?��
        if (conductorCount <= 0) {
            return;
        }

        // �??��输�?�槽?��?��??�足够�??�?�?
        if (inputTank.getFluidAmount() < FLUID_PER_OPERATION) {
            return;
        }

        // �??��输出槽是?��??�足够空气?
        if (outputTank.getFluidAmount() + FLUID_PER_OPERATION > outputTank.getCapacity()) {
            return;
        }

        // �??��??��?�是?��已充满?达�?��?��?��?��?�产???�?
        if (heatStorage.getHeatStored() >= HEAT_CAPACITY) {
            return;
        }

        // ?��??��?��?��??体类???
        FluidStack inputFluid = inputTank.getFluid();
        if (inputFluid.isEmpty()) {
            return;
        }

        // ?��?��输�?��??体类??�并�?�?
        boolean isLava = inputFluid.getFluid() == Fluids.LAVA;
        boolean isHotCoolant = inputFluid.getFluid() == mio_icif_fluids.HOTCOOLANT.get();

        if (!isLava && !isHotCoolant) {
            return;
        }

        // �???��?��?��??�?
        FluidStack drained = inputTank.drain(FLUID_PER_OPERATION, IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() < FLUID_PER_OPERATION) {
            return;
        }

        // 产�?��?��?��??不�??�?容纳?��?��?��??
        long heatToAdd = Math.min(HU_PER_BUCKET, HEAT_CAPACITY - heatStorage.getHeatStored());
        heatStorage.generateHeatInternal(heatToAdd, false);

        // 产�?��?�出�?�?
        if (isLava) {
            outputTank.fill(new FluidStack(mio_icif_fluids.PAHOEHOELAVA.get(), FLUID_PER_OPERATION), IFluidHandler.FluidAction.EXECUTE);
        } else {
            outputTank.fill(new FluidStack(mio_icif_fluids.COOLANT.get(), FLUID_PER_OPERATION), IFluidHandler.FluidAction.EXECUTE);
        }

        setChanged();
    }

    /**
     * 输出??��??
     */
    private void outputHeat() {
        if (heatStorage.getHeatStored() <= 0 || currentHeatOutput <= 0) {
            return;
        }

        Direction facing = getBlockState().getValue(
            net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        BlockPos adjacentPos = worldPosition.relative(facing);

        IMioIcifCapabilities.IHeatStorage adjacentHeat = level.getCapability(
                IMioIcifCapabilities.HEAT_STORAGE_BLOCK, adjacentPos, facing.getOpposite());
        if (adjacentHeat == null) {
            adjacentHeat = MioIcifAPI.instance().getCapabilities().adaptHeatStorage(
                level.getBlockEntity(adjacentPos));
        }

        com.singularity_iteration.mio_icif.Singularity_Iteration.LOGGER.info(
            "[HeatSourceFluid] outputHeat tick=" + level.getGameTime() + " pos=" + worldPosition + " facing=" + facing +
            " heatStored=" + heatStorage.getHeatStored() + " currentHeatOutput=" + currentHeatOutput +
            " adjacentPos=" + adjacentPos + " adjacentHeat=" + (adjacentHeat != null ? "found" : "null"));

        if (adjacentHeat != null && adjacentHeat.canReceiveHeat()) {
            long heatToOutput = Math.min(currentHeatOutput,
                    Math.min(heatStorage.getHeatStored(),
                    adjacentHeat.getMaxHeatStored() - adjacentHeat.getHeatStored()));

            if (heatToOutput > 0) {
                com.singularity_iteration.mio_icif.Singularity_Iteration.LOGGER.info(
                    "[HeatSourceFluid] Sending " + heatToOutput + " HU to " + adjacentPos +
                    " (adjStored=" + adjacentHeat.getHeatStored() + "/" + adjacentHeat.getMaxHeatStored() + ")");
                adjacentHeat.receiveHeat(heatToOutput, false);
                heatStorage.consumeHeatInternal(heatToOutput, false);
                setChanged();
            } else {
                com.singularity_iteration.mio_icif.Singularity_Iteration.LOGGER.info(
                    "[HeatSourceFluid] heatToOutput=0 at " + adjacentPos +
                    " (stored=" + heatStorage.getHeatStored() + "/" + heatStorage.getMaxHeatStored() +
                    ", adjStored=" + adjacentHeat.getHeatStored() + "/" + adjacentHeat.getMaxHeatStored() + ")");
            }
        } else if (adjacentHeat != null) {
            com.singularity_iteration.mio_icif.Singularity_Iteration.LOGGER.info(
                "[HeatSourceFluid] adjacent.canReceiveHeat()=false at " + adjacentPos);
        } else {
            com.singularity_iteration.mio_icif.Singularity_Iteration.LOGGER.info(
                "[HeatSourceFluid] NO adjacent heat storage at " + adjacentPos + " facing=" + facing);
        }
    }

    /**
     * �??��?��?���???��??�?
     * ??��?��??�?40000HU?���?�??????��?�计?��，�???????��?��??�?
     */
    private void checkOverheat() {
        // �??��?��?���?�?警�?��??�?
        if (heatStorage.getHeatStored() >= HEAT_WARNING_THRESHOLD) {
            overheatTimer++;

            // �???��??�?1??????，�??�?
            if (overheatTimer >= EXPLOSION_WARNING_TICKS) {
                // ?????��??
                level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                        1.0f, Level.ExplosionInteraction.BLOCK);

                // 移除?��???
                level.removeBlock(worldPosition, false);
            }
        } else {
            // ??��?��?��?��?��?��???��，�?�置计时?��
            overheatTimer = 0;
        }
    }

    /**
     * �????输�?�液体�??
     */
    private void handleInputFluidSlot() {
        ItemStack bucketStack = itemHandler.getStackInSlot(INPUT_FLUID_BUCKET_SLOT);
        if (bucketStack.isEmpty()) {
            return;
        }

        // �??��?��?��?��岩�??桶�??岩�????��????��?��?�却?????��??
        boolean isLavaBucket = bucketStack.is(Items.LAVA_BUCKET);
        boolean isLavaCell = mio_icif_cells.isCellContainingFluid(bucketStack, Fluids.LAVA);
        boolean isHotCoolantCell = mio_icif_cells.isCellContainingFluid(bucketStack, mio_icif_fluids.HOTCOOLANT.get());

        if (!isLavaBucket && !isLavaCell && !isHotCoolantCell) {
            return;
        }

        // �??���?体�?��?�是?��还�?�足够空?��容纳�?个�?�整?????��??�?000mB�?
        int remainingSpace = inputTank.getCapacity() - inputTank.getFluidAmount();
        if (remainingSpace < FLUID_PER_OPERATION) {
            return;
        }

        // �??��当�?��?��?�槽中�??�?体类???
        FluidStack currentFluid = inputTank.getFluid();
        if (!currentFluid.isEmpty()) {
            boolean currentIsLava = currentFluid.getFluid() == Fluids.LAVA;
            boolean currentIsHotCoolant = currentFluid.getFluid() == mio_icif_fluids.HOTCOOLANT.get();
            
            // �???��?��?�是岩�??�??��??�继续添??�岩�?
            if (currentIsLava && isHotCoolantCell) {
                return;
            }
            // �???��?��?�是??��?�却???�??��??�继续添??��?��?�却???
            if (currentIsHotCoolant && (isLavaBucket || isLavaCell)) {
                return;
            }
        }
        
        // �??��输出槽中???�?体类??��???��止�?�出槽混???�?
        FluidStack outputFluid = outputTank.getFluid();
        if (!outputFluid.isEmpty()) {
            boolean outputIsPahoehoeLava = outputFluid.getFluid() == mio_icif_fluids.PAHOEHOELAVA.get();
            boolean outputIsCoolant = outputFluid.getFluid() == mio_icif_fluids.COOLANT.get();
            
            // �???��?�出槽是??�岩岩�??�??��??�添??�岩�?
            if (outputIsPahoehoeLava && isHotCoolantCell) {
                return;
            }
            // �???��?�出槽是??�却液�???��??�添??��?��?�却???
            if (outputIsCoolant && (isLavaBucket || isLavaCell)) {
                return;
            }
        }

        // ?��?��输�?�类???
        boolean isCell = isLavaCell || isHotCoolantCell;
        ItemStack emptyContainer = isCell ? mio_icif_cells.getEmptyCellForStack(bucketStack) : new ItemStack(Items.BUCKET);
        if (isCell && emptyContainer.isEmpty()) emptyContainer = new ItemStack(mio_icif_cells.CELL_EMPTY.get());

        // �??��空容?��输出�?
        ItemStack emptyStack = itemHandler.getStackInSlot(INPUT_EMPTY_BUCKET_SLOT);
        if (!emptyStack.isEmpty()) {
            if (!ItemStack.isSameItem(emptyStack, emptyContainer) ||
                emptyStack.getCount() >= emptyStack.getMaxStackSize()) {
                return;
            }
        }

        // 填充??输�?��??
        if (isLavaBucket || isLavaCell) {
            inputTank.fill(new FluidStack(Fluids.LAVA, FLUID_PER_OPERATION), IFluidHandler.FluidAction.EXECUTE);
        } else {
            inputTank.fill(new FluidStack(mio_icif_fluids.HOTCOOLANT.get(), FLUID_PER_OPERATION), IFluidHandler.FluidAction.EXECUTE);
        }

        // �???��?��?��?��??
        bucketStack.shrink(1);

        // 添�?�空容纳??
        if (emptyStack.isEmpty()) {
            itemHandler.setStackInSlot(INPUT_EMPTY_BUCKET_SLOT, emptyContainer);
        } else {
            emptyStack.grow(1);
        }

        setChanged();
    }

    /**
     * �????输出液�?��??
     */
    private void handleOutputFluidSlot() {
        // �??��输出槽是?��??�空??��??/桶可以填充?
        ItemStack emptyStack = itemHandler.getStackInSlot(OUTPUT_FLUID_BUCKET_SLOT);
        if (emptyStack.isEmpty()) {
            return;
        }

        // �??��输出�?体槽?��?��??��??�?
        if (outputTank.getFluidAmount() < FLUID_PER_OPERATION) {
            return;
        }

        // ?��?��输出�?体类???
        FluidStack outputFluid = outputTank.getFluid();
        if (outputFluid.isEmpty()) {
            return;
        }

        boolean isPahoehoeLava = outputFluid.getFluid() == mio_icif_fluids.PAHOEHOELAVA.get();
        boolean isCoolant = outputFluid.getFluid() == mio_icif_fluids.COOLANT.get();

        // �??��输�?��??空容?��类�?�是?��?���?
        boolean isEmptyCell = mio_icif_cells.isEmptyCell(emptyStack);

        ItemStack fullContainer = null;

        if (isPahoehoeLava && isEmptyCell) {
            fullContainer = mio_icif_cells.getFilledCellForFluidStack(mio_icif_fluids.PAHOEHOELAVA.get());
        } else if (isCoolant && isEmptyCell) {
            fullContainer = mio_icif_cells.getFilledCellForFluidStack(mio_icif_fluids.COOLANT.get());
        }

        if (fullContainer == null) {
            return;
        }

        // �??��满容?��输出�?
        ItemStack fullStack = itemHandler.getStackInSlot(OUTPUT_FULL_BUCKET_SLOT);
        if (!fullStack.isEmpty()) {
            if (!ItemStack.isSameItem(fullStack, fullContainer) ||
                fullStack.getCount() >= fullStack.getMaxStackSize()) {
                return;
            }
        }

        // �???��?�出�?�?
        FluidStack drained = outputTank.drain(FLUID_PER_OPERATION, IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() < FLUID_PER_OPERATION) {
            return;
        }

        // �???�空容器
        emptyStack.shrink(1);

        // 添�?�满容纳??
        if (fullStack.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_FULL_BUCKET_SLOT, fullContainer);
        } else {
            fullStack.grow(1);
        }

        setChanged();
    }

    // ==================== ??��??槽位?��??====================

    /**
     * �??��??��???��?��???????��??��??定槽位?
     */
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == INPUT_FLUID_BUCKET_SLOT) {
            // 输�?�槽：接??�岩�?桶�?��?��?�却?????��??
            return stack.is(Items.LAVA_BUCKET) || mio_icif_cells.isCellContainingFluid(stack, mio_icif_fluids.HOTCOOLANT.get());
        } else if (slot == INPUT_EMPTY_BUCKET_SLOT) {
            // 空容?��输出槽位?��?��??许�?�动?��???
            return false;
        } else if (slot == OUTPUT_FLUID_BUCKET_SLOT) {
            // 输出槽位?��?��?�接??�空??��??
            return mio_icif_cells.isEmptyCell(stack);
        } else if (slot == OUTPUT_FULL_BUCKET_SLOT) {
            // 满容?��输出槽位?��?��??许�?�动?��???
            return false;
        } else if (slot >= HEAT_CONDUCTOR_START && slot < HEAT_CONDUCTOR_START + HEAT_CONDUCTOR_COUNT) {
            // ??�交?��?��槽位?�只?��??��?�交?���?
            return stack.is(mio_icif_resources.HEATCONDUCTOR.get());
        } else if (slot >= EXTRA_SLOT_START && slot < EXTRA_SLOT_START + EXTRA_SLOT_COUNT) {
            // 额�?��?��??槽位?��???��?��??�任何�?��??�?�???��??
            return true;
        }
        return false;
    }

    // ==================== NBT 序�?��??====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", itemHandler.serializeNBT(registries));
        tag.put("InputTank", inputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("OutputTank", outputTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("ConductorCount", conductorCount);
        tag.putInt("OverheatTimer", overheatTimer);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Items")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
        }
        if (tag.contains("InputTank")) {
            inputTank.readFromNBT(registries, tag.getCompound("InputTank"));
        }
        if (tag.contains("OutputTank")) {
            outputTank.readFromNBT(registries, tag.getCompound("OutputTank"));
        }
        if (tag.contains("HeatStored")) {
            heatStorage.setHeat(tag.getInt("HeatStored"));
        }
        conductorCount = tag.getInt("ConductorCount");
        overheatTimer = tag.getInt("OverheatTimer");
        currentHeatOutput = Math.min(conductorCount * HEAT_PER_CONDUCTOR, MAX_HEAT_OUTPUT);
    }

    // ==================== ?��??��??====================

    public long getHeatStored() {
        return heatStorage.getHeatStored();
    }

    public long getHeatCapacity() {
        return HEAT_CAPACITY;
    }

    public int getConductorCount() {
        return conductorCount;
    }

    public int getCurrentHeatOutput() {
        return currentHeatOutput;
    }

    public void consumeHeat(int amount) {
        heatStorage.consumeHeatInternal(amount, false);
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int getInputFluidAmount() {
        return inputTank.getFluidAmount();
    }

    public int getInputFluidCapacity() {
        return inputTank.getCapacity();
    }

    public int getOutputFluidAmount() {
        return outputTank.getFluidAmount();
    }

    public int getOutputFluidCapacity() {
        return outputTank.getCapacity();
    }

    public int getInputFluidTypeId() {
        FluidStack fluid = inputTank.getFluid();
        if (fluid.isEmpty()) return -1;
        return net.minecraft.core.registries.BuiltInRegistries.FLUID.getId(fluid.getFluid());
    }

    public int getOutputFluidTypeId() {
        FluidStack fluid = outputTank.getFluid();
        if (fluid.isEmpty()) return -1;
        return net.minecraft.core.registries.BuiltInRegistries.FLUID.getId(fluid.getFluid());
    }

    public boolean isWorking() {
        return isWorking;
    }

    @Nullable
    @Override
    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    @Nullable
    @Override
    protected IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        return inputTank;
    }

    public IFluidHandler getInputTankCapability(@Nullable Direction side) {
        return inputTank;
    }

    public IFluidHandler getOutputTankCapability(@Nullable Direction side) {
        return outputTank;
    }

    // ?��?��?��??��??体槽位??��于Jade等显示�??
    public FluidTank getInputTank() {
        return inputTank;
    }

    public FluidTank getOutputTank() {
        return outputTank;
    }

    // ?��??��?????�?体�??????���??��于Jade?��示�?�槽位?
    public IFluidHandler getCombinedFluidHandler() {
        return new CombinedFluidHandler(inputTank, outputTank);
    }

    /**
     * �????�?体�?????�?- �?输�?�槽??��?�出槽位??并为�?个�?�两个槽???�????�?
     */
    private static class CombinedFluidHandler implements IFluidHandler {
        private final FluidTank inputTank;
        private final FluidTank outputTank;

        public CombinedFluidHandler(FluidTank inputTank, FluidTank outputTank) {
            this.inputTank = inputTank;
            this.outputTank = outputTank;
        }

        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank == 0) {
                return inputTank.getFluid();
            } else if (tank == 1) {
                return outputTank.getFluid();
            }
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            if (tank == 0) {
                return inputTank.getCapacity();
            } else if (tank == 1) {
                return outputTank.getCapacity();
            }
            return 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (tank == 0) {
                return inputTank.isFluidValid(stack);
            }
            return false; // 输出槽位?�接??��?��??
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            // ?��???许填?????��?��?��??
            return inputTank.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            // 优�??从�?�出槽抽???
            FluidStack result = outputTank.drain(resource, action);
            if (result.isEmpty()) {
                result = inputTank.drain(resource, action);
            }
            return result;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            // 优�??从�?�出槽抽???
            FluidStack result = outputTank.drain(maxDrain, action);
            if (result.isEmpty()) {
                result = inputTank.drain(maxDrain, action);
            }
            return result;
        }
    }

    // ==================== WorldlyContainer ?��?��实现 ====================

    @Override
    public int[] getSlotsForFace(Direction side) {
        // ?????�面??�可以访?��?????�槽位?
        int[] slots = new int[TOTAL_SLOTS];
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            slots[i] = i;
        }
        return slots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        // ?��??��?��?�空容器输出槽位??满容?��输出�?
        return slot == INPUT_EMPTY_BUCKET_SLOT || slot == OUTPUT_FULL_BUCKET_SLOT;
    }

    // ==================== Container ?��?��实现 ====================

    @Override
    public int getContainerSize() {
        // 返回?��?��???��?��???槽位?�数??��?��?��?�NBT??�载??�槽位数??��?�匹???
        return itemHandler != null ? itemHandler.getSlots() : TOTAL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        int slots = getContainerSize();
        for (int i = 0; i < slots; i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return itemHandler.getStackInSlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = itemHandler.getStackInSlot(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = stack.split(amount);
        if (stack.isEmpty()) {
            itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        }
        setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = itemHandler.getStackInSlot(slot);
        itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        itemHandler.setStackInSlot(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        int slots = getContainerSize();
        for (int i = 0; i < slots; i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    // ==================== MenuProvider ?��?��实现 ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.heat_source_fluid");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.HUEntity.HeatSourceFluidMenu(
                containerId, playerInventory, this);
    }

    // ==================== IHeatGeneratorBlock ?��?��实现 ====================

    @Override
    public int getHeatOutput() {
        return currentHeatOutput;
    }

    @Override
    public boolean isGenerating() {
        return isWorking;
    }

    @Override
    public int getBurnTime() {
        return 0;
    }

    @Override
    public int getBurnDuration() {
        return 0;
    }

    @Override
    public int getHeatGenerationRate() {
        return currentHeatOutput;
    }

    // ==================== IBurnControl ?��?��实现 ====================

    @Override
    public int getDefaultBurnTime() {
        return 0;
    }

    @Override
    public void setBurnTime(int ticks) {
        // Not applicable for heat source fluid
    }
}