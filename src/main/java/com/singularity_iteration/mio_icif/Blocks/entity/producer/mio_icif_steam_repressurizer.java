package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.mio_icif_HeatU_Block;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 蒸汽再加压机方块实体
 * 
IC2 蒸汽/过热蒸汽通过加热再加压，提高输出蒸汽
 *
 * 转换比例
 * - 普通蒸汽：1 mB 
1.6 mB
 * - 过热蒸汽
 mB 
3.2 mB
 */
@SuppressWarnings("null")
public class mio_icif_steam_repressurizer extends mio_icif_HeatU_Block {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .extra(2)
        .upgrade(2)
        .build();

    // 槽位定义
    public static final int INPUT_CELL_SLOT = 0;     // 蒸汽单元输入槽（可选）
    public static final int OUTPUT_CELL_SLOT = 1;    // 再加压蒸汽单元输出槽（可选）
    public static final int UPGRADE_SLOT_START = 2;  // 升级槽起
public static final int UPGRADE_SLOT_COUNT = 2;  // 升级槽数
public static final int TOTAL_SLOTS = 4;

    // 流体容量
    public static final int INPUT_TANK_CAPACITY = 10000;  // 输入蒸汽
public static final int OUTPUT_TANK_CAPACITY = 16000; // 输出蒸汽

    // 工作参数（原
public static final int INPUT_BATCH = 10;            // 每批次处
    public static final int NORMAL_OUTPUT_BATCH = 16;    // 10 mB 普通蒸

    public static final int SUPERHEATED_OUTPUT_BATCH = 32; // 10 mB 过热蒸汽 
    public static final int HU_PER_HEAT_UNIT = 1;        // 每积
    public static final int MAX_HEAT_PER_TICK = 4;       // 

    // 热量配置
    public static final int HEAT_CAPACITY = 20000;
    public static final int MAX_HEAT_RECEIVE = 500;
    public static final int MAX_HEAT_EXTRACT = 0;
    public static final int MAX_TEMP = 500;
    public static final float HEAT_LOSS_FACTOR = 0.0f;

    // 流体存储
    protected final FluidTank inputTank;
    protected final FluidTank outputTank;

    // 状态
private boolean isWorking = false;
    private int currentHeat = 0; // 当前积累的热量单位（原版

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> inputTank.getFluidAmount();
                case 1 -> inputTank.getCapacity();
                case 2 -> outputTank.getFluidAmount();
                case 3 -> outputTank.getCapacity();
                case 4 -> (int) Math.min(heatStorage.getHeatStored(), Integer.MAX_VALUE);
                case 5 -> (int) Math.min(heatStorage.getMaxHeatStored(), Integer.MAX_VALUE);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() { return 6; }
    };

    public mio_icif_steam_repressurizer(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.STEAM_REPRESSURIZER_ENTITY_TYPE.get());
    }

    public mio_icif_steam_repressurizer(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(type, pos, state, HEAT_CAPACITY, MAX_HEAT_RECEIVE, MAX_HEAT_EXTRACT,
              20, MAX_TEMP, HEAT_LOSS_FACTOR);

        this.inputTank = new FluidTank(INPUT_TANK_CAPACITY, fluidStack -> {
            if (fluidStack.isEmpty()) return true;
            return fluidStack.getFluid() == mio_icif_fluids.STEAM.get()
                || fluidStack.getFluid() == mio_icif_fluids.SUPERHEATEDSTEAM.get();
        });

        this.outputTank = new FluidTank(OUTPUT_TANK_CAPACITY, fluidStack -> {
            if (fluidStack.isEmpty()) return true;
            return fluidStack.getFluid() == mio_icif_fluids.STEAM.get()
                || fluidStack.getFluid() == mio_icif_fluids.SUPERHEATEDSTEAM.get();
        });

        // 初始化槽位布局和物品处理器
        this.slotLayout = LAYOUT;
        this.itemHandler = createItemHandler(LAYOUT);
        this.itemHandler.setValidator((slot, stack, slotType) -> mio_icif_steam_repressurizer.this.isItemValidForSlot(slot, stack));
    }

    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (isUpgradeSlot(slot)) {
            return stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Upgrade.mio_icif_upgrade;
        }
        return switch (slot) {
            case INPUT_CELL_SLOT -> !(stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Upgrade.mio_icif_upgrade);
            case OUTPUT_CELL_SLOT -> false;
            default -> false;
        };
    }

    /**
     * 
tick 更新逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_steam_repressurizer blockEntity) {
        if (level.isClientSide()) return;

        blockEntity.receiveHeatFromSides();
        blockEntity.handleInputCellSlot();
        blockEntity.repressurizeSteam();
        blockEntity.outputSteamToNeighbors();

        boolean wasWorking = blockEntity.isWorking;
        blockEntity.isWorking = blockEntity.canWork();
        if (wasWorking != blockEntity.isWorking) {
            blockEntity.updateBlockState(blockEntity.isWorking);
        }

        blockEntity.handleAutomationUpgrades();

        blockEntity.setChanged();
    }

    /**
     * 从除正面外的其他面接收热
 */
    private void receiveHeatFromSides() {
        if (level == null || level.isClientSide()) return;

        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        long heatNeeded = HEAT_CAPACITY - heatStorage.getHeatStored();
        if (heatNeeded <= 0) return;

        for (Direction direction : Direction.values()) {
            if (direction == facing) continue;
            if (heatNeeded <= 0) return;

            BlockPos neighborPos = worldPosition.relative(direction);
            IMioIcifCapabilities.IHeatStorage adjacentHeat = level.getCapability(IMioIcifCapabilities.HEAT_STORAGE_BLOCK, neighborPos, direction.getOpposite());

            if (adjacentHeat != null && adjacentHeat.canExtractHeat()) {
                long heatToExtract = Math.min(heatNeeded, adjacentHeat.getHeatStored());
                long extracted = adjacentHeat.extractHeat(heatToExtract, false);
                if (extracted > 0) {
                    heatStorage.receiveHeat(extracted, false);
                    heatNeeded -= extracted;
                }
            }
        }
    }

    /**
     * 处理输入单元槽（简化版：接受装有蒸汽的单元，填入输入槽
 */
    private void handleInputCellSlot() {
        // 暂不实现单元处理，留给后续扩
}

    /**
     * 蒸汽再加压（原版 IC2 风格
 */
    private void repressurizeSteam() {
        FluidStack inputFluid = inputTank.getFluid();
        if (inputFluid.isEmpty() || inputTank.getFluidAmount() < INPUT_BATCH) return;

        boolean isSuperheated = inputFluid.getFluid() == mio_icif_fluids.SUPERHEATEDSTEAM.get();
        int outputPerBatch = isSuperheated ? SUPERHEATED_OUTPUT_BATCH : NORMAL_OUTPUT_BATCH;

        // 检查输出槽是否有足够空
    if (outputTank.getFluidAmount() > 0 && outputTank.getFluid().getFluid() != inputFluid.getFluid()) return;

        // 目标热量 = 输入
        int targetHeat = inputTank.getFluidAmount() / 10;

    if (currentHeat < targetHeat && heatStorage.getHeatStored() >= HU_PER_HEAT_UNIT) {
            heatStorage.extractHeat(HU_PER_HEAT_UNIT, false);
            currentHeat++;
        }

        if (currentHeat <= 0) return;

        // 计算可处理量
        int availableInput = inputTank.getFluidAmount();
        int availableOutputSpace = outputTank.getCapacity() - outputTank.getFluidAmount();

        int maxProcessByHeat = currentHeat * 10;
        int maxProcessByOutput = availableOutputSpace * INPUT_BATCH / outputPerBatch;

        int amountToProcess = Math.min(availableInput, Math.min(maxProcessByHeat, maxProcessByOutput));
        if (amountToProcess < INPUT_BATCH) return;

        // 向下取整
        int batches = amountToProcess / INPUT_BATCH;
        amountToProcess = batches * INPUT_BATCH;

        int usedHeat = amountToProcess / 10;
        int outputAmount = batches * outputPerBatch;

        // 执行转换
        FluidStack drained = inputTank.drain(amountToProcess, IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() < amountToProcess) return;

        outputTank.fill(new FluidStack(inputFluid.getFluid(), outputAmount), IFluidHandler.FluidAction.EXECUTE);
        currentHeat -= usedHeat;
    }

    /**
     * 将再加压蒸汽输出到相邻流体容
 */
    private void outputSteamToNeighbors() {
        if (level == null || outputTank.getFluidAmount() <= 0) return;

        FluidStack available = outputTank.getFluid();
        if (available.isEmpty()) return;

        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);

        Direction[] directions = new Direction[]{facing, facing.getOpposite()};
        for (Direction dir : directions) {
            BlockPos neighborPos = worldPosition.relative(dir);
            IFluidHandler neighborHandler = level.getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                neighborPos, dir.getOpposite());

            if (neighborHandler != null) {
                FluidStack toDrain = new FluidStack(available.getFluid(), Math.min(available.getAmount(), 1000));
                int filled = neighborHandler.fill(toDrain, IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    outputTank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                    available = outputTank.getFluid();
                    if (available.isEmpty()) return;
                }
            }
        }
    }

    private boolean canWork() {
        return inputTank.getFluidAmount() >= INPUT_BATCH
            && outputTank.getFluidAmount() + NORMAL_OUTPUT_BATCH <= outputTank.getCapacity();
    }

    private void updateBlockState(boolean working) {
        if (level == null || level.isClientSide()) return;
        BlockState state = getBlockState();
        if (state.hasProperty(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_steam_repressurizer.LIT)) {
            level.setBlock(worldPosition, state.setValue(
                com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_steam_repressurizer.LIT, working), 3);
        }
    }

    // ==================== Capability ====================

    public IItemHandler getItemHandlerCapability(@Nullable Direction side) {
        return itemHandler;
    }

    @Override
    public IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        return new CombinedFluidHandler(inputTank, outputTank);
    }

    @Override
    public IMioIcifCapabilities.IHeatStorage getHeatStorageCapability(@Nullable Direction side) {
        if (side == null) return heatStorage;
        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        if (side == facing) return null;
        return heatStorage;
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inputTank", inputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("outputTank", outputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("items", itemHandler.serializeNBT(registries));
        tag.putBoolean("isWorking", isWorking);
        tag.putInt("currentHeat", currentHeat);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inputTank")) inputTank.readFromNBT(registries, tag.getCompound("inputTank"));
        if (tag.contains("outputTank")) outputTank.readFromNBT(registries, tag.getCompound("outputTank"));
        if (tag.contains("items")) itemHandler.deserializeNBT(registries, tag.getCompound("items"));
        isWorking = tag.getBoolean("isWorking");
        currentHeat = tag.getInt("currentHeat");
    }

    // ==================== MenuProvider ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.steam_repressurizer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Producer.SteamRepressurizerMenu(containerId, playerInventory, this);
    }

    // ==================== Getters ====================

    public FluidTank getInputTank() { return inputTank; }
    public FluidTank getOutputTank() { return outputTank; }
    public ItemStackHandler getItemHandler() { return itemHandler; }
    public ContainerData getContainerData() { return containerData; }
    public boolean isWorking() { return isWorking; }

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

    /**
     * 组合流体处理
 */
    private static class CombinedFluidHandler implements IFluidHandler {
        private final FluidTank inputTank;
        private final FluidTank outputTank;

        public CombinedFluidHandler(FluidTank inputTank, FluidTank outputTank) {
            this.inputTank = inputTank;
            this.outputTank = outputTank;
        }

        @Override
        public int getTanks() { return 2; }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return tank == 0 ? inputTank.getFluid() : outputTank.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? inputTank.getCapacity() : outputTank.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return tank == 0 ? inputTank.isFluidValid(stack) : outputTank.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (inputTank.isFluidValid(resource)) {
                return inputTank.fill(resource, action);
            }
            return 0;
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            return outputTank.drain(resource, action);
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            return outputTank.drain(maxDrain, action);
        }
    }
}