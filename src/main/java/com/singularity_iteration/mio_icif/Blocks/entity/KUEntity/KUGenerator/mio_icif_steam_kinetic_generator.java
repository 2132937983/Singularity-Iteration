package com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator;

import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.mio_icif_KineticU_Block;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.MachineItemHandler;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
import com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities;
import com.singularity_iteration.mio_icif.api.machine.IKineticGeneratorBlock;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.singularity_iteration.mio_icif.api.machine.ISlotLayout;

/**
 * 蒸汽动能发生机方块实
 * 消耗蒸汽产
KU 动能
 *
 * 工作原理
 * - 输入普通蒸汽：1 mB 
2 KU，副产物 1 mB 蒸馏
 * - 输入过热蒸汽
 mB 
4 KU，无副产
 * - 需要安装蒸汽涡轮叶
 * - 涡轮叶片工作时会损耗耐久
 */
@SuppressWarnings("null")
public class mio_icif_steam_kinetic_generator extends mio_icif_KineticU_Block implements com.singularity_iteration.mio_icif.api.machine.IProducerBlock, IKineticGeneratorBlock {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .extra(2)
        .turbine()
        .upgrade(2)
        .build();

    // 槽位定义
    public static final int STEAM_CELL_SLOT = 0;     // 蒸汽单元输入
public static final int WATER_CELL_SLOT = 1;     // 蒸馏水单元输出槽
    public static final int TURBINE_SLOT = 2;        // 涡轮叶片
public static final int UPGRADE_SLOT_START = 3;  // 升级槽起
public static final int UPGRADE_SLOT_COUNT = 2;  // 升级槽数
public static final int TOTAL_SLOTS = 5;

    // 流体容量
    public static final int STEAM_TANK_CAPACITY = 21000;   // 蒸汽槽（原版 IC2 21000 mB
public static final int WATER_TANK_CAPACITY = 1000;    // 蒸馏水槽（原

    // 工作参数（原
public static final int KU_PER_MB_STEAM = 2;           // 
    public static final int KU_PER_MB_SUPERHEATED = 4;     // 
    public static final int TURBINE_DURABILITY_COST_NORMAL = 2;   // 普通蒸汽每 20 tick 损坏
public static final int TURBINE_DURABILITY_COST_SUPERHEATED = 1; // 过热蒸汽
public static final int MAX_TURBINE_DAMAGE = 200;      // 涡轮叶片最大耐久
    public static final int CONDENSATION_THRESHOLD = 100;  // 冷凝进度阈�
public static final int CONDENSATION_DIVISOR = 10;     // 冷凝进度 = actualAmount / CONDENSATION_DIVISOR
    public static final int UPDATE_TICK_INTERVAL = 20;     // 涡轮耐久/冷凝处理间隔

    // 动能配置
    public static final int KINETIC_CAPACITY = 10000;
    public static final int KINETIC_MAX_RECEIVE = 0;
    public static final int KINETIC_MAX_EXTRACT = 1000;
    public static final int MAX_RPM = 10000;
    public static final float FRICTION_FACTOR = 0.005f;

    // 流体存储
    protected final FluidTank steamTank;
    protected final FluidTank waterTank;

    // 物品
protected final MachineItemHandler itemHandler;

    // 状
    private boolean isWorking = false;
    private boolean isTurbineFilledWithWater = false;
    private int lastKineticOutput = 0; // 上一 tick 实际产出
    private int condensationProgress = 0; // 冷凝进度（原版）
    private int updateTicker = 0; // 20 tick 计数


    // 数据同步访问
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> isHotSteam() ? 1 : 0;
                case 1 -> hasTurbine() ? 1 : 0;
                case 2 -> isTurbineFilledWithWater ? 1 : 0;
                case 3 -> isWorking ? 1 : 0;
                case 4 -> lastKineticOutput;
                case 5 -> gaugeLiquidScaled(26, 0);
                case 6 -> waterTank.getFluidAmount();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // 客户端只读，不需要实
        }

        @Override
        public int getCount() {
            return 7;
        }
    };
    public mio_icif_steam_kinetic_generator(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.STEAM_KINETIC_GENERATOR_ENTITY_TYPE.get());
    }

    public mio_icif_steam_kinetic_generator(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(type, pos, state, KINETIC_CAPACITY, KINETIC_MAX_RECEIVE, KINETIC_MAX_EXTRACT, MAX_RPM, FRICTION_FACTOR);

        this.steamTank = new FluidTank(STEAM_TANK_CAPACITY, fluidStack -> {
            if (fluidStack.isEmpty()) return true;
            return fluidStack.getFluid() == mio_icif_fluids.STEAM.get()
                || fluidStack.getFluid() == mio_icif_fluids.SUPERHEATEDSTEAM.get();
        });

        this.waterTank = new FluidTank(WATER_TANK_CAPACITY, fluidStack -> {
            if (fluidStack.isEmpty()) return true;
            return fluidStack.getFluid() == mio_icif_fluids.DISTILLEDWATER.get();
        });

        this.itemHandler = new MachineItemHandler(LAYOUT) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
        this.itemHandler.setValidator((slot, stack, slotType) -> mio_icif_steam_kinetic_generator.this.isItemValidForSlot(slot, stack));
    }

    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case STEAM_CELL_SLOT -> mio_icif_cells.isCellContainingAnyFluid(stack, mio_icif_fluids.STEAM.get(), mio_icif_fluids.SUPERHEATEDSTEAM.get());
            case WATER_CELL_SLOT -> false;
            case TURBINE_SLOT -> stack.is(mio_icif_normal.STEAM_TURBINE_BLADE.get());
            default -> true; // 升级
    };
    }

    /**
     * 
tick 更新逻辑（原
IC2 风格
 */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_steam_kinetic_generator blockEntity) {
        if (level.isClientSide()) return;

        blockEntity.handleSteamCellSlot();
        blockEntity.generateKinetic();
        blockEntity.outputWaterToNeighbors();
        blockEntity.outputSteamToNeighbors();

        // 调用基类 tick 处理摩擦损失和动能传输（distributeKinetic
    mio_icif_KineticU_Block.tick(level, pos, state, blockEntity);

        // 额外：主动将 KU 输出到前方的方块（正面输出，类似 Stirling
    blockEntity.outputKineticToFront();

        boolean wasWorking = blockEntity.isWorking;
        blockEntity.isWorking = blockEntity.canWork();
        if (wasWorking != blockEntity.isWorking) {
            blockEntity.updateBlockState(blockEntity.isWorking);
        }

        blockEntity.setChanged();
    }

    /**
     * 
KU 动能输出到前方的方块（正面输出接口）
     */
    private void outputKineticToFront() {
        if (level == null || level.isClientSide()) return;
        if (kineticStorage.getKineticStored() <= 0) return;

        Direction facing = getBlockState().getValue(
            com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block.FACING);
        BlockPos frontPos = worldPosition.relative(facing);

        IMioIcifCapabilities.IKineticStorage frontKinetic = level.getCapability(
            IMioIcifCapabilities.KINETIC_STORAGE_BLOCK,
            frontPos, facing.getOpposite());
        if (frontKinetic == null) {
            BlockEntity be = level.getBlockEntity(frontPos);
            frontKinetic = MioIcifAPI.instance().getCapabilities().adaptKineticStorage(be);
        }

        if (frontKinetic != null && frontKinetic.canReceiveKinetic()) {
            long toTransfer = Math.min(kineticStorage.getKineticStored(),
                                      Math.min(kineticStorage.getMaxExtract(),
                                               frontKinetic.getMaxKineticStored() - frontKinetic.getKineticStored()));
            if (toTransfer > 0) {
                long extracted = kineticStorage.extractKinetic(toTransfer, false);
                if (extracted > 0) {
                    frontKinetic.receiveKinetic(extracted, false);
                }
            }
        }
    }

    /**
     * 处理蒸汽单元输入
 */
    private void handleSteamCellSlot() {
        ItemStack cellStack = itemHandler.getStackInSlot(STEAM_CELL_SLOT);
        if (cellStack.isEmpty()) return;

        boolean isSteamCell = mio_icif_cells.isCellContainingFluid(cellStack, mio_icif_fluids.STEAM.get());
        boolean isSuperheatedCell = mio_icif_cells.isCellContainingFluid(cellStack, mio_icif_fluids.SUPERHEATEDSTEAM.get());
        if (!isSteamCell && !isSuperheatedCell) return;

        Fluid fluidToFill = isSuperheatedCell ? mio_icif_fluids.SUPERHEATEDSTEAM.get() : mio_icif_fluids.STEAM.get();
        if (steamTank.getFluidAmount() + FluidType.BUCKET_VOLUME > steamTank.getCapacity()) return;

        ItemStack waterStack = itemHandler.getStackInSlot(WATER_CELL_SLOT);
        if (!waterStack.isEmpty() && (!mio_icif_cells.isCellContainingFluid(waterStack, mio_icif_fluids.DISTILLEDWATER.get()) || waterStack.getCount() >= waterStack.getMaxStackSize())) {
        if (!mio_icif_cells.isEmptyCell(waterStack) || waterStack.getCount() >= waterStack.getMaxStackSize()) {
                return;
            }
        }

        int filled = steamTank.fill(new FluidStack(fluidToFill, FluidType.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE);
        if (filled >= FluidType.BUCKET_VOLUME) {
            itemHandler.extractItem(STEAM_CELL_SLOT, 1, false);
            if (waterStack.isEmpty()) {
                itemHandler.setStackInSlot(WATER_CELL_SLOT, mio_icif_cells.getEmptyCellForStack(cellStack));
            } else {
                waterStack.grow(1);
            }
            setChanged();
        }
    }

    /**
     * 产生动能（原
IC2 风格：每次处理全部蒸汽）
     */
    private void generateKinetic() {
        lastKineticOutput = 0;
        ItemStack turbineStack = itemHandler.getStackInSlot(TURBINE_SLOT);
        if (turbineStack.isEmpty() || !turbineStack.is(mio_icif_normal.STEAM_TURBINE_BLADE.get())) return;
        if (steamTank.getFluidAmount() <= 0) return;

        FluidStack steamFluid = steamTank.getFluid();
        if (steamFluid.isEmpty()) return;

        boolean isSuperheated = steamFluid.getFluid() == mio_icif_fluids.SUPERHEATEDSTEAM.get();
        int amount = steamFluid.getAmount();
        int kuPerMb = isSuperheated ? KU_PER_MB_SUPERHEATED : KU_PER_MB_STEAM;

        // 蒸馏水节流（普通蒸汽，原版 IC2：蒸馏水槽满则停转）
        float throttle = 1.0F;
        if (!isSuperheated) {
            if (waterTank.getFluidAmount() >= waterTank.getCapacity()) {
                isTurbineFilledWithWater = true;
                return;
            }
            // 原版 IC2 无渐进节流，只有
        isTurbineFilledWithWater = false;
        }

        // 计算可产出的总动
    long kineticProduced = (long) (amount * kuPerMb * throttle);

        long availableSpace = kineticStorage.getMaxKineticStored() - kineticStorage.getKineticStored();
        if (availableSpace <= 0) return;

        long actualKinetic = Math.min(kineticProduced, availableSpace);
        if (actualKinetic <= 0) return;

        // 计算实际消耗的蒸汽?
        long actualAmount = actualKinetic / kuPerMb;
        if (actualAmount <= 0) return;

        // 检查涡轮耐久
        int turbineDamage = turbineStack.getDamageValue();
        if (turbineDamage >= MAX_TURBINE_DAMAGE) {
            itemHandler.setStackInSlot(TURBINE_SLOT, ItemStack.EMPTY);
            return;
        }

        // 消耗蒸
    FluidStack drained = steamTank.drain((int) actualAmount, IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() < actualAmount) return;

        // 产生 KU（使
        long actualKineticProduced = actualAmount * kuPerMb;
        long actuallyReceived = kineticStorage.generateKineticInternal(actualKineticProduced, false);
        lastKineticOutput = (int) actuallyReceived;

        // 产生蒸馏水（普通蒸汽，原版冷凝进度机制
    if (!isSuperheated) {
            int condensation = (int) (actualAmount / CONDENSATION_DIVISOR);
            condensationProgress += condensation;
            if (condensationProgress >= CONDENSATION_THRESHOLD) {
                int waterAmount = condensationProgress / CONDENSATION_THRESHOLD;
                condensationProgress %= CONDENSATION_THRESHOLD;
                waterTank.fill(new FluidStack(mio_icif_fluids.DISTILLEDWATER.get(), waterAmount), IFluidHandler.FluidAction.EXECUTE);
            }
        }

        // 涡轮耐久损耗（
    updateTicker++;
        if (updateTicker >= UPDATE_TICK_INTERVAL) {
            updateTicker = 0;
            int durabilityCost = isSuperheated ? TURBINE_DURABILITY_COST_SUPERHEATED : TURBINE_DURABILITY_COST_NORMAL;
            turbineStack.setDamageValue(turbineDamage + durabilityCost);
            if (turbineStack.getDamageValue() >= MAX_TURBINE_DAMAGE) {
                itemHandler.setStackInSlot(TURBINE_SLOT, ItemStack.EMPTY);
            }
        }
    }

    /**
     * 将蒸馏水输出到相邻流体容
 */
    private void outputWaterToNeighbors() {
        if (level == null || waterTank.getFluidAmount() <= 0) return;

        FluidStack available = waterTank.getFluid();
        if (available.isEmpty()) return;

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(dir);
            IFluidHandler neighborHandler = level.getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                neighborPos, dir.getOpposite());

            if (neighborHandler != null) {
                FluidStack toDrain = new FluidStack(available.getFluid(), Math.min(available.getAmount(), 1000));
                int filled = neighborHandler.fill(toDrain, IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    waterTank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                    available = waterTank.getFluid();
                    if (available.isEmpty()) return;
                }
            }
        }
    }

    /**
     * 将过热蒸汽输出到相邻的冷凝器或蒸汽动能机
     */
    private void outputSteamToNeighbors() {
        if (level == null || steamTank.getFluidAmount() <= 0) return;

        FluidStack available = steamTank.getFluid();
        if (available.isEmpty() || available.getFluid() != mio_icif_fluids.SUPERHEATEDSTEAM.get()) return;

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(dir);
            IFluidHandler neighborHandler = level.getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                neighborPos, dir.getOpposite());

            if (neighborHandler != null) {
                FluidStack toDrain = new FluidStack(available.getFluid(), Math.min(available.getAmount(), 1000));
                int filled = neighborHandler.fill(toDrain, IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    steamTank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                    available = steamTank.getFluid();
                    if (available.isEmpty()) return;
                }
            }
        }
    }

    private boolean canWork() {
        ItemStack turbineStack = itemHandler.getStackInSlot(TURBINE_SLOT);
        return !turbineStack.isEmpty()
            && turbineStack.is(mio_icif_normal.STEAM_TURBINE_BLADE.get())
            && turbineStack.getDamageValue() < MAX_TURBINE_DAMAGE
            && steamTank.getFluidAmount() > 0
            && kineticStorage.getKineticStored() < kineticStorage.getMaxKineticStored()
            && !isTurbineFilledWithWater;
    }

    private void updateBlockState(boolean working) {
        if (level == null || level.isClientSide()) return;
        BlockState state = getBlockState();
        if (state.hasProperty(com.singularity_iteration.mio_icif.Blocks.KUGenerator.mio_icif_block_steam_kinetic_generator.LIT)) {
            level.setBlock(worldPosition, state.setValue(
                com.singularity_iteration.mio_icif.Blocks.KUGenerator.mio_icif_block_steam_kinetic_generator.LIT, working), 3);
        }
    }

    // ==================== IProducerBlock API ====================

    @Override
    public void forceStartWork() {
        this.isWorking = true;
    }

    @Override
    public void forceStopWork() {
        this.isWorking = false;
    }

    @Override
    public com.singularity_iteration.mio_icif.api.machine.IMachineUpgradeStats getUpgradeStats() {
        int start = UPGRADE_SLOT_START;
        int count = UPGRADE_SLOT_COUNT;
        return com.singularity_iteration.mio_icif.Items.Upgrade.MachineUpgradeStats.fromInventory(itemHandler, start, count);
    }

    @Override
    public int getUpgradeSlotStart() {
        return UPGRADE_SLOT_START;
    }

    @Override
    public int getUpgradeSlotCount() {
        return UPGRADE_SLOT_COUNT;
    }

    @Override
    public java.util.List<ItemStack> getUpgrades() {
        java.util.List<ItemStack> upgrades = new java.util.ArrayList<>();
        for (int i = UPGRADE_SLOT_START; i < UPGRADE_SLOT_START + UPGRADE_SLOT_COUNT; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                upgrades.add(stack);
            }
        }
        return upgrades;
    }

    @Override
    public int getProgress() {
        return 0;
    }

    @Override
    public int getMaxProgress() {
        return 1;
    }

    @Override
    public int getBaseMaxProgress() {
        return 1;
    }

    @Override
    public long getEnergyPerTick() {
        return 0;
    }

    @Override
    public ISlotLayout getSlotLayout() {
        return LAYOUT;
    }

    @Override
    public int getContainerSize() {
        return itemHandler.getSlots();
    }

    @Override
    public com.singularity_iteration.mio_icif.api.energy.IEnergyStorageAccess getEnergyStorage() {
        return null;
    }

    @Override
    public com.singularity_iteration.mio_icif.api.machine.IWorkCompleteCallback getWorkCompleteCallback() {
        return null;
    }

    @Override
    public boolean isWorking() {
        return isWorking;
    }

    @Override
    public int getKineticOutput() {
        return isWorking ? lastKineticOutput : 0;
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
    public int getKineticGenerationRate() {
        return lastKineticOutput;
    }

    @Override
    public int getRotorRPM() {
        return kineticStorage != null ? kineticStorage.getRPM() : 0;
    }

    @Override
    public long getEffectiveEnergyPerTick() {
        return 0;
    }

    @Override
    public long getTotalProcessed() {
        return 0;
    }

    // ==================== Capability ====================

    public IItemHandler getItemHandlerCapability(@Nullable Direction side) {
        return itemHandler;
    }

    public IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        return new CombinedFluidHandler(steamTank, waterTank);
    }

    @Nullable
    public net.neoforged.neoforge.energy.IEnergyStorage getEnergyStorageCapability(@Nullable Direction side) {
        return null;
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("steamTank", steamTank.writeToNBT(registries, new CompoundTag()));
        tag.put("waterTank", waterTank.writeToNBT(registries, new CompoundTag()));
        tag.put("Items", itemHandler.serializeNBT(registries));
        tag.putBoolean("isWorking", isWorking);
        tag.putBoolean("isTurbineFilledWithWater", isTurbineFilledWithWater);
        tag.putInt("condensationProgress", condensationProgress);
        tag.putInt("updateTicker", updateTicker);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("steamTank")) steamTank.readFromNBT(registries, tag.getCompound("steamTank"));
        if (tag.contains("waterTank")) waterTank.readFromNBT(registries, tag.getCompound("waterTank"));
        if (tag.contains("Items")) itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
        isWorking = tag.getBoolean("isWorking");
        isTurbineFilledWithWater = tag.getBoolean("isTurbineFilledWithWater");
        condensationProgress = tag.getInt("condensationProgress");
        updateTicker = tag.getInt("updateTicker");
    }

    // ==================== MenuProvider ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.steam_kinetic_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Generator.SteamKineticGeneratorMenu(containerId, playerInventory, this);
    }

    /**
     * 获取数据同步访问
     */
    public ContainerData getContainerData() {
        return dataAccess;
    }

    // ==================== Getters ====================

    public FluidTank getSteamTank() { return steamTank; }
    public FluidTank getWaterTank() { return waterTank; }
    public ItemStackHandler getItemHandler() { return itemHandler; }

    /**
     * 是否已安装涡轮叶片（原版 IC2 风格
 */
    public boolean hasTurbine() {
        ItemStack stack = itemHandler.getStackInSlot(TURBINE_SLOT);
        return !stack.isEmpty() && stack.is(mio_icif_normal.STEAM_TURBINE_BLADE.get());
    }

    /**
     * 涡轮是否被水填满（原
IC2 风格
 */
    public boolean isTurbineFilledWithWater() {
        return isTurbineFilledWithWater;
    }

    /**
     * 当前输入的是否为过热蒸汽（原
IC2 风格
 */
    public boolean isHotSteam() {
        FluidStack fluid = steamTank.getFluid();
        return !fluid.isEmpty() && fluid.getFluid() == mio_icif_fluids.SUPERHEATEDSTEAM.get();
    }

    /**
     * 获取上一 tick 实际产出
KU（供 GUI 显示输出，原版叫 gethUoutput
 */
    public int getLastKineticOutput() { return lastKineticOutput; }

    /**
     * 获取蒸馏水槽（原
getTank() 返回 distilledwaterTank
 */
    public FluidTank getTank() {
        return waterTank;
    }

    /**
     * 液体条缩放（原版 IC2 风格
 */
    public int gaugeLiquidScaled(int i, int tank) {
        switch (tank) {
            case 0:
                if (waterTank.getFluidAmount() <= 0) return 0;
                return waterTank.getFluidAmount() * i / waterTank.getCapacity();
        }
        return 0;
    }

    /**
     * 组合流体处理
 */
    private static class CombinedFluidHandler implements IFluidHandler {
        private final FluidTank steamTank;
        private final FluidTank waterTank;

        public CombinedFluidHandler(FluidTank steamTank, FluidTank waterTank) {
            this.steamTank = steamTank;
            this.waterTank = waterTank;
        }

        @Override
        public int getTanks() { return 2; }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return tank == 0 ? steamTank.getFluid() : waterTank.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? steamTank.getCapacity() : waterTank.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return tank == 0 ? steamTank.isFluidValid(stack) : waterTank.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (steamTank.isFluidValid(resource)) {
                return steamTank.fill(resource, action);
            }
            return 0;
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            FluidStack fromWater = waterTank.drain(resource, action);
            if (!fromWater.isEmpty()) return fromWater;
            return steamTank.drain(resource, action);
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack fromWater = waterTank.drain(maxDrain, action);
            if (!fromWater.isEmpty()) return fromWater;
            return steamTank.drain(maxDrain, action);
        }
    }
}