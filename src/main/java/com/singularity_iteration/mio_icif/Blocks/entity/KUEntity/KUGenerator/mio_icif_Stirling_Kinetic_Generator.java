package com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator;

import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.mio_icif_KineticU_Block;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.MachineItemHandler;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities;
import com.singularity_iteration.mio_icif.api.machine.IKineticGeneratorBlock;
import com.singularity_iteration.mio_icif.api.machine.IBurnControl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * ?��?��??�动??��?��???��?��??��?��??
 * 使用??��?��?��?��??体产??�动??��?��??�?水�?�坴保�??违转
 *
 * 工�?��?��??�?
 * - 从�?�方?���?HU ??��??
 * - �???�水??��?��?�产??�动??��?��?�水
 * - 4 HU + 1mB �?= 1mB ??�水 + 12 KU ?��???
 *
 * 槽位?��?��??�????8个�?��??
 * - 0: 水桶输�?��??
 * - 1: 空桶输出�?
 * - 2-4: ??�件槽位??3个�??
 * - 5-6: ??��??槽位??2个�?��????��??
 */
@SuppressWarnings("null")
public class mio_icif_Stirling_Kinetic_Generator extends mio_icif_KineticU_Block implements IKineticGeneratorBlock, IBurnControl {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .extra(2)
        .upgrade(3)
        .extra(2)
        .build();

    // 槽位?��?��??
    public static final int WATER_BUCKET_SLOT = 0;      // 水桶输�?��??
    public static final int EMPTY_BUCKET_SLOT = 1;      // 空桶输出�?
    public static final int UPGRADE_SLOT_START = 2;     // ??�件槽起�?
    public static final int UPGRADE_SLOT_COUNT = 3;     // ??�件槽数据?
    public static final int ITEM_SLOT_START = 5;        // ??��??槽起�?
    public static final int ITEM_SLOT_COUNT = 2;        // ??��??槽数据?
    public static final int TOTAL_SLOTS = 8;            // ??�槽佝数

    // �?体槽容纳??(2000 mB = 2�?
    public static final int FLUID_CAPACITY = 2000;

    // ??��?�转?��比�?��?? HU + 1mB �?= 1mB ??�水 + 12 KU
    public static final int HU_PER_OPERATION = 4;       // 毝次??��?��??�??4 HU
    public static final int MB_PER_OPERATION = 1;       // 毝次??��?��??�??1 mB �?
    public static final int KU_PER_OPERATION = 12;      // 毝次??��?�产??? 12 KU
    public static final int MB_WATER_TO_HOTWATER = 1;   // 1mB 水�?��??1mB ??�水

    // ??��?��?��?��?��?? (10次�?��?��??????????��??
    public static final int MAX_BUFFERED_HEAT = HU_PER_OPERATION * 10;

    // ?��??��?��?��?�置
    public static final int KINETIC_CAPACITY = 1000;    // ?��??�容???
    public static final int KINETIC_MAX_EXTRACT = 100;  // ???大�?�出??��??

    // �?体�?��??
    protected final FluidTank waterTank;    // 水槽
    protected final FluidTank hotWaterTank; // ??�水�?

    // ??��??�?
    protected final MachineItemHandler itemHandler;

    // 当�?��?��?��????��?��???��于工作�??
    private int bufferedHeat = 0;

    // 工�?��?�度
    public int progress = 0;
    public int maxProgress = 1; // �?tick�????�?�?

    /**
     * ?????�函�?
     */
    public mio_icif_Stirling_Kinetic_Generator(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.STIRLING_KINETIC_GENERATOR_ENTITY_TYPE.get());
    }

    /**
     * ?????�函?���??��????��定�?�类??��??
     */
    public mio_icif_Stirling_Kinetic_Generator(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(type, pos, state, KINETIC_CAPACITY, 0, KINETIC_MAX_EXTRACT, 100, 0.005f);

        // ??��?��?�水槽位???��??�水??�蒸馝水�?
        this.waterTank = new FluidTank(FLUID_CAPACITY, fluidStack ->
            fluidStack.getFluid() == Fluids.WATER || 
            fluidStack.getFluid() == mio_icif_fluids.DISTILLEDWATER.get());

        // ??��?��?��?�水槽位???��?��??��?�水�?
        this.hotWaterTank = new FluidTank(FLUID_CAPACITY, fluidStack ->
            fluidStack.getFluid() == mio_icif_fluids.HOTWATER.get());

        // ??��?��?��?��??�?????��
        this.itemHandler = new MachineItemHandler(LAYOUT) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
        this.itemHandler.setValidator((slot, stack, slotType) -> mio_icif_Stirling_Kinetic_Generator.this.isItemValidForSlot(slot, stack));
    }

    /**
     * �??��??��???��?��???????��??��??定槽位?
     */
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case WATER_BUCKET_SLOT -> isValidWaterContainer(stack);
            case EMPTY_BUCKET_SLOT -> isValidEmptyContainer(stack); // 空桶槽坯以接??�空桶�?�空??��??
            case UPGRADE_SLOT_START, UPGRADE_SLOT_START + 1, UPGRADE_SLOT_START + 2 -> true; // ??�件槽接??�任何�?��??
            case ITEM_SLOT_START, ITEM_SLOT_START + 1 -> true; // ??��??槽接??�任何�?��??
            default -> false;
        };
    }

    /**
     * �??��??��???��?��?��??��?????空容?���?空桶???空气?��??�?
     */
    private boolean isValidEmptyContainer(ItemStack stack) {
        if (stack.is(Items.BUCKET)) {
            return true;
        }
        if (mio_icif_cells.isEmptyCell(stack)) {
            return true;
        }
        return false;
    }

    private boolean isValidWaterContainer(ItemStack stack) {
        if (stack.is(Items.WATER_BUCKET)) {
            return true;
        }
        if (mio_icif_cells.isCellContainingAnyFluid(stack, Fluids.WATER, mio_icif_fluids.DISTILLEDWATER.get())) {
            return true;
        }
        return false;
    }

    /**
     * �????水容?��输�?��??水桶???水�?��??????��馝水??��??�?
     * �?容器中�??水转移�?��??体�?��?��?�空容器?��??�空桶�?�出�?
     */
    private void handleWaterBucketSlot() {
        ItemStack waterContainerStack = itemHandler.getStackInSlot(WATER_BUCKET_SLOT);
        if (waterContainerStack.isEmpty()) {
            return;
        }

        // �??��?��?��?��??��?????水容?��
        if (!isValidWaterContainer(waterContainerStack)) {
            return;
        }

        // �??��水槽?��?��还�?�空???
        if (waterTank.getFluidAmount() + FluidType.BUCKET_VOLUME > waterTank.getCapacity()) {
            return;
        }

        // 确定?�空容器类�??
        ItemStack emptyContainerStack = itemHandler.getStackInSlot(EMPTY_BUCKET_SLOT);
        ItemStack emptyContainerType;
        
        if (waterContainerStack.is(Items.WATER_BUCKET)) {
            emptyContainerType = new ItemStack(Items.BUCKET);
        } else if (mio_icif_cells.isCellContainingAnyFluid(waterContainerStack, Fluids.WATER, mio_icif_fluids.DISTILLEDWATER.get())) {
            emptyContainerType = mio_icif_cells.getEmptyCellForStack(waterContainerStack);
            if (emptyContainerType.isEmpty()) emptyContainerType = new ItemStack(mio_icif_cells.CELL_EMPTY.get());
        } else {
            return; // ?��?��容器类�??
        }

        // �??��空桶输出槽是?��?��以容纳?
        if (!emptyContainerStack.isEmpty()) {
            if (!ItemStack.isSameItem(emptyContainerStack, emptyContainerType) || 
                emptyContainerStack.getCount() >= emptyContainerStack.getMaxStackSize()) {
                return;
            }
        }

        // 转移水�??1000mb = 1�???��??�?
        int filled = waterTank.fill(new FluidStack(Fluids.WATER, FluidType.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE);
        if (filled >= FluidType.BUCKET_VOLUME) {
            // �???�水容器
            itemHandler.extractItem(WATER_BUCKET_SLOT, 1, false);
            // 添�?�空容器??�空桶�?�出�?
            if (emptyContainerStack.isEmpty()) {
                itemHandler.setStackInSlot(EMPTY_BUCKET_SLOT, emptyContainerType);
            } else {
                emptyContainerStack.grow(1);
            }
            setChanged();
        }
    }

    /**
     * �??????�水输出??�容?���?桶�?��?��??�?
     * �???��?�空容器且�?�水槽位?�足够�????�水，�????��?�水容纳??
     */
    private void handleHotWaterOutput() {
        // �??��??�水槽是?��??�足够�????�水
        if (hotWaterTank.getFluidAmount() < FluidType.BUCKET_VOLUME) {
            return;
        }

        // �??��空容?���?
        ItemStack emptyContainerStack = itemHandler.getStackInSlot(EMPTY_BUCKET_SLOT);
        if (emptyContainerStack.isEmpty()) {
            return;
        }

        // 确定?��?�出容器类�??
        ItemStack filledContainerType;
        
        if (emptyContainerStack.is(Items.BUCKET)) {
            filledContainerType = new ItemStack(mio_icif_fluids.HOTWATER_BUCKET.get());
        } else if (mio_icif_cells.isEmptyCell(emptyContainerStack)) {
            filledContainerType = mio_icif_cells.getFilledCellForFluidStack(mio_icif_fluids.HOTWATER.get());
        } else {
            return; // 丝是??��?????空容?��
        }

        // �??��输�?�槽?��?��?��以容纳�?�出
        ItemStack inputSlotStack = itemHandler.getStackInSlot(WATER_BUCKET_SLOT);
        if (!inputSlotStack.isEmpty()) {
            // �???��?��?�槽已�?��?��??，�???��?��?��?��?��??�类??��????�水容纳??
            if (!ItemStack.isSameItem(inputSlotStack, filledContainerType) ||
                inputSlotStack.getCount() >= inputSlotStack.getMaxStackSize()) {
                return; // ??��?�容纳�?�出
            }
        }

        // �???��?�水??�空容器，�????��?�水容器
        hotWaterTank.drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.EXECUTE);
        emptyContainerStack.shrink(1);
        
        if (inputSlotStack.isEmpty()) {
            itemHandler.setStackInSlot(WATER_BUCKET_SLOT, filledContainerType);
        } else {
            inputSlotStack.grow(1);
        }
        setChanged();
    }

    /**
     * 从除去??��外�????��?��?�个?��?��?��??��??
     */
    private void receiveHeatFromSides() {
        if (level == null || level.isClientSide()) return;

        Direction facing = getBlockState().getValue(com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block.FACING);

        // ??��???????�方??��?�除去?�??��
        for (Direction direction : Direction.values()) {
            if (direction == facing) continue; // 跳�??�??��

 // �??缂擄?芥槸?宸诧??
            int heatNeeded = MAX_BUFFERED_HEAT - bufferedHeat;
            if (heatNeeded <= 0) return; // 缓�?�已充满?��?��?��?��?��?��?�方法?

            BlockPos neighborPos = worldPosition.relative(direction);
            // 从相??�方??�获??��?��?��?��?��?�相??�方??��?�为?��询方???
            IMioIcifCapabilities.IHeatStorage heatStorage = level.getCapability(IMioIcifCapabilities.HEAT_STORAGE_BLOCK, neighborPos, direction.getOpposite());
            if (heatStorage == null) {
                BlockEntity neighborBe = level.getBlockEntity(neighborPos);
                heatStorage = MioIcifAPI.instance().getCapabilities().adaptHeatStorage(neighborBe);
            }

            if (heatStorage != null && heatStorage.canExtractHeat()) {
                // 尝�?��?�相??�方??��?��?��?��??
                long heatToExtract = Math.min(heatNeeded, heatStorage.getHeatStored());
                long extracted = heatStorage.extractHeat(heatToExtract, false);
                if (extracted > 0) {
                    bufferedHeat += extracted;
                    setChanged();
                }
            }
        }
    }

    private boolean doWork() {
        if (bufferedHeat < HU_PER_OPERATION) return false;
        if (waterTank.getFluidAmount() < MB_PER_OPERATION) return false;

        int hotWaterToAdd = MB_WATER_TO_HOTWATER;
        if (hotWaterTank.getFluidAmount() + hotWaterToAdd > hotWaterTank.getCapacity()) {
            return false;
        }

        int operations = bufferedHeat / HU_PER_OPERATION;
        int waterAvailable = waterTank.getFluidAmount() / MB_PER_OPERATION;
        int hotWaterSpace = (hotWaterTank.getCapacity() - hotWaterTank.getFluidAmount()) / MB_WATER_TO_HOTWATER;
        operations = Math.min(operations, waterAvailable);
        operations = Math.min(operations, hotWaterSpace);

        if (operations <= 0) return false;

        bufferedHeat -= operations * HU_PER_OPERATION;
        waterTank.drain(operations * MB_PER_OPERATION, IFluidHandler.FluidAction.EXECUTE);
        hotWaterTank.fill(new FluidStack(mio_icif_fluids.HOTWATER.get(), operations * MB_WATER_TO_HOTWATER), IFluidHandler.FluidAction.EXECUTE);

        int totalKU = operations * KU_PER_OPERATION;

        Direction facing = getBlockState().getValue(com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block.FACING);
        BlockPos frontPos = worldPosition.relative(facing);

        IMioIcifCapabilities.IKineticStorage frontKinetic = level.getCapability(
            IMioIcifCapabilities.KINETIC_STORAGE_BLOCK, frontPos, facing.getOpposite());
        if (frontKinetic == null) {
            BlockEntity be = level.getBlockEntity(frontPos);
            frontKinetic = MioIcifAPI.instance().getCapabilities().adaptKineticStorage(be);
        }
        if (frontKinetic != null && frontKinetic.canReceiveKinetic()) {
            frontKinetic.receiveKinetic(totalKU, false);
        }

        setChanged();
        return true;
    }

    /**
     * �?tick ?��?��??��??
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_Stirling_Kinetic_Generator blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // �????水桶输�??
        blockEntity.handleWaterBucketSlot();

        // �??????�水输出??�桶
        blockEntity.handleHotWaterOutput();

        // 从除去??��外�????��?��?�个?��?��?��??��??
        blockEntity.receiveHeatFromSides();

        boolean didWork = blockEntity.doWork();

        // �??��工�?�状???�?�???��?�次??��?��??工�?��?��?��????��?��?�工作�?��?��?�工作中�?
        boolean isWorking = didWork || blockEntity.isWorking();
        // ?��?��?��??�状???�?�?tick ??�更?��，确保状???�?确定??
        blockEntity.updateBlockState(isWorking, state, level, pos);

        blockEntity.setChanged();
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        bufferedHeat = tag.getInt("BufferedHeat");
        progress = tag.getInt("Progress");
        waterTank.readFromNBT(registries, tag.getCompound("WaterTank"));
        hotWaterTank.readFromNBT(registries, tag.getCompound("HotWaterTank"));
        itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("BufferedHeat", bufferedHeat);
        tag.putInt("Progress", progress);
        tag.put("WaterTank", waterTank.writeToNBT(registries, new CompoundTag()));
        tag.put("HotWaterTank", hotWaterTank.writeToNBT(registries, new CompoundTag()));
        tag.put("Items", itemHandler.serializeNBT(registries));
    }

    // Getters for GUI
    public int getWaterAmount() {
        return waterTank.getFluidAmount();
    }

    public int getHotWaterAmount() {
        return hotWaterTank.getFluidAmount();
    }

    public int getMaxFluidAmount() {
        return FLUID_CAPACITY;
    }

    public int getBufferedHeat() {
        return bufferedHeat;
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    /**
     * �??��?��?��?��?���??��工�??
     * ?��事件?��?�足够�????��?��??水�?��?�水槽空?��
     */
    public boolean isWorking() {
        return bufferedHeat >= HU_PER_OPERATION &&
               waterTank.getFluidAmount() >= MB_PER_OPERATION &&
               hotWaterTank.getFluidAmount() + MB_WATER_TO_HOTWATER <= hotWaterTank.getCapacity();
    }

    /**
     * ?��?��?��??�状态??
     */
    public void updateBlockState(boolean working, net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }

        // ?��??��?��?��??active ?���??
        Boolean currentActive = state.getValue(com.singularity_iteration.mio_icif.Blocks.KUGenerator.mio_icif_Block_Stirling_Kinetic_Generator.ACTIVE);
        // ?��??��?�状????��??�时??�更新?
        if (currentActive != working) {
            level.setBlock(pos, state.setValue(
                    com.singularity_iteration.mio_icif.Blocks.KUGenerator.mio_icif_Block_Stirling_Kinetic_Generator.ACTIVE, working),
                    3);
        }
    }

    public FluidTank getWaterTank() {
        return waterTank;
    }

    public FluidTank getHotWaterTank() {
        return hotWaterTank;
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    // ?��??��?????�?体�??????���??��于Jade?��示�?�槽位?
    public IFluidHandler getCombinedFluidHandler() {
        return new CombinedFluidHandler(waterTank, hotWaterTank);
    }

    /**
     * �????�?体�?????�?- �?水槽??��?�水槽位??并为�?个�?�两个槽???�????�?
     */
    private static class CombinedFluidHandler implements IFluidHandler {
        private final FluidTank waterTank;
        private final FluidTank hotWaterTank;

        public CombinedFluidHandler(FluidTank waterTank, FluidTank hotWaterTank) {
            this.waterTank = waterTank;
            this.hotWaterTank = hotWaterTank;
        }

        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank == 0) {
                return waterTank.getFluid();
            } else if (tank == 1) {
                return hotWaterTank.getFluid();
            }
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            if (tank == 0) {
                return waterTank.getCapacity();
            } else if (tank == 1) {
                return hotWaterTank.getCapacity();
            }
            return 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (tank == 0) {
                return waterTank.isFluidValid(stack);
            }
            return false; // ??�水槽位?�接??��?��??
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            // ?��???许填?????�水�?
            return waterTank.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            // 优�??从�?�水槽抽???
            FluidStack result = hotWaterTank.drain(resource, action);
            if (result.isEmpty()) {
                result = waterTank.drain(resource, action);
            }
            return result;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            // 优�??从�?�水槽抽???
            FluidStack result = hotWaterTank.drain(maxDrain, action);
            if (result.isEmpty()) {
                result = waterTank.drain(maxDrain, action);
            }
            return result;
        }
    }

    /**
     * ?��??��?��?��?��?��?��?��???��于�?��?�查询�??
     * 返回?��??个�?��?��?? IHeatStorage�??��?��?????��?��?��?��??表示?��?��?��以接?��??��?��??
     */
    public IMioIcifCapabilities.IHeatStorage getHeatStorageCapability(@Nullable Direction side) {
        return new IMioIcifCapabilities.IHeatStorage() {
            @Override
            public long receiveHeat(long maxReceive, boolean simulate) {
                long heatNeeded = MAX_BUFFERED_HEAT - bufferedHeat;
                long heatToReceive = Math.min(maxReceive, heatNeeded);
                if (!simulate && heatToReceive > 0) {
                    bufferedHeat += heatToReceive;
                    setChanged();
                }
                return heatToReceive;
            }

            @Override
            public long extractHeat(long maxExtract, boolean simulate) {
                return 0;
            }

            @Override
            public long getHeatStored() {
                return bufferedHeat;
            }

            @Override
            public long getMaxHeatStored() {
                return MAX_BUFFERED_HEAT;
            }

            @Override
            public boolean canReceiveHeat() {
                return bufferedHeat < MAX_BUFFERED_HEAT;
            }

            @Override
            public boolean canExtractHeat() {
                return false;
            }

            @Override
            public int getTemperature() {
                return 20;
            }

            @Override
            public boolean isOverheated() {
                return false;
            }

            @Override
            public long getHeatLossPerTick() {
                return 0;
            }

            @Override
            public long getMaxReceive() {
                return MAX_BUFFERED_HEAT - bufferedHeat;
            }

            @Override
            public long getMaxExtract() {
                return 0;
            }
        };
    }

    // MenuProvider 实现
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.stirling_kinetic_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Generator.StirlingKineticGeneratorMenu(containerId, playerInventory, this);
    }

    // ==================== IKineticGeneratorBlock ???????? ====================

    @Override
    public int getKineticOutput() {
        return isWorking() ? KU_PER_OPERATION : 0;
    }

    @Override
    public boolean isGenerating() {
        return isWorking();
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
    public int getKineticGenerationRate() {
        return KU_PER_OPERATION;
    }

    @Override
    public int getRotorRPM() {
        return kineticStorage != null ? kineticStorage.getRPM() : 0;
    }

    // ==================== IBurnControl ???????? ====================

    @Override
    public int getDefaultBurnTime() {
        return 0;
    }

    @Override
    public void setBurnTime(int ticks) {
        // Not applicable for Stirling kinetic generator
    }
}