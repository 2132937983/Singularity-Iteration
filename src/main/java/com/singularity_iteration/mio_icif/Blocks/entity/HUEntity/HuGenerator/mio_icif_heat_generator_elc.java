package com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.HuGenerator;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities;
import com.singularity_iteration.mio_icif.api.machine.IHeatGeneratorBlock;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 电力发热机方块实
 * 继承自生产者基类，消
EU 电力产生 HU 热能
 *
 * 特点
 * - 从电网接
EU 电力（像普通用电设备）
 * - 消
EU 电力产生 HU 热能
 * - 产生的热能可以传导到相邻方块
 * - 电能转热能效率：1 EU = 2 HU
 * - 11个槽位：1个电池槽 + 10个线圈槽
 */
@SuppressWarnings("null")
public class mio_icif_heat_generator_elc extends mio_icif_producer implements IHeatGeneratorBlock, IMioIcifCapabilities.IHeatStorage {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .battery()
        .coil(10)  // 10个线圈槽
        .build();

    // 槽位定义
    public static final int BATTERY_SLOT = 0;      // 电池
    public static final int COIL_SLOT_START = 1;   // 线圈槽起
    public static final int COIL_SLOT_COUNT = 10;  // 线圈槽数
    public static final int TOTAL_SLOTS = 11;      // 总槽位数

    // 每个线圈的热能产生速率 (HU/tick)，对
    private static final int HEAT_PER_COIL = 10;
    // 最大热能产生速率 (HU/tick)
    private static final int MAX_HEAT_GENERATION = 100;

    // 热能容量，对
    private static final int HEAT_CAPACITY = 50000;

    // 最大热能输出速率，对
    private static final int MAX_HEAT_EXTRACT = 100;

    // 最高温
    private static final int MAX_TEMP = 6000;

    // 热损失系数（关闭，保证产出热量稳定输出到前方机器
    private static final float LOSS_FACTOR = 0.0f;

    // 内部热能存储
    private final IMioIcifCapabilities.IHeatStorage heatStorage;

    // 电力容量，对
    private static final long ENERGY_CAPACITY = 10000L;
    // 最大输入功率，对齐 IC2 原版 2048 EU/t (EV等级)
    private static final long MAX_INPUT = 2048L;

    /**
     * 构造函
     */
    public mio_icif_heat_generator_elc(BlockPos pos, BlockState state) {
        // 调用父类构造函数：电力容量 10000，最大接
        // 不需要工作进度，所
        // 11个槽位：1个电池槽 + 10个线圈槽
        super(pos, state, mio_icif_block_entities.HEAT_GENERATOR_ELC.get(),
                ENERGY_CAPACITY, MAX_INPUT, 0L,  // 电力参数：容
                1, LAYOUT, // maxProgress=1, slotCount=11
                100L, CableTier.EV);           // 电压等级 EV (2048 EU/t)

        this.heatStorage = MioIcifAPI.instance().getCapabilities().createHeatStorage(
                HEAT_CAPACITY, 0, MAX_HEAT_EXTRACT, 20, MAX_TEMP, LOSS_FACTOR);
    }

    /**
     * 
tick 更新逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_heat_generator_elc blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 调用父类
        mio_icif_producer.tick(level, pos, state, blockEntity);

        // 应用热损
        blockEntity.applyHeatLossInternal();

        // 向相邻方块传导热
        blockEntity.distributeHeat();

        // 更新方块状态（运行/停止
        boolean isActive = state.getValue(com.singularity_iteration.mio_icif.Blocks.HUGenerator.mio_icif_block_heat_generator_elc.ACTIVE);
        if (blockEntity.isWorking() != isActive) {
            level.setBlock(pos, state.setValue(com.singularity_iteration.mio_icif.Blocks.HUGenerator.mio_icif_block_heat_generator_elc.ACTIVE, blockEntity.isWorking()), 3);
        }
    }

    /**
     * 获取当前能量消耗（根据线圈数量
     * 1个线
= 10 EU/t（对
IC2 原版 1 EU = 1 HU
     */
    private long getCurrentEnergyConsumption() {
        return getCoilCount() * 10L;
    }

    @Override
    protected boolean hasEnoughEnergy() {
        // 根据线圈数量检查是否有足够能量
        return energyStorage.getAmount() >= getCurrentEnergyConsumption();
    }

    @Override
    protected boolean consumeEnergy() {
        // 根据线圈数量消耗能
        long energyNeeded = getCurrentEnergyConsumption();
        if (energyStorage.getAmount() >= energyNeeded) {
            // 使用内部消耗方法，不受 maxExtract 限制
            apiUseEnergy(energyNeeded, false);
            return true;
        }
        return false;
    }

    @Override
    protected boolean canWork() {
        // 只要有线圈、有电且自身热能未满即可工作
        // 热量输出交给 distributeHeat() 按温度差自动传导，不需要前方必须有消费
        return getCoilCount() > 0 && hasEnoughEnergy()
                && heatStorage.getHeatStored() < heatStorage.getMaxHeatStored();
    }

    /**
     * 检查前方是否有需要热量的机器
     * 只要有可以接收热能的能力即可，不需要判断是否已经满热，
 * 避免高在预热阈值附近小幅波动时导致工作状
100/0 闪烁
     */
    @SuppressWarnings("unused")
    private boolean hasHeatConsumerInFront() {
        if (level == null) return false;

        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        BlockPos frontPos = worldPosition.relative(facing);

        IMioIcifCapabilities.IHeatStorage frontHeat = level.getCapability(
                IMioIcifCapabilities.HEAT_STORAGE_BLOCK, frontPos, facing.getOpposite());
        if (frontHeat == null) {
            frontHeat = MioIcifAPI.instance().getCapabilities().adaptHeatStorage(
                level.getBlockEntity(frontPos));
        }

        return frontHeat != null && frontHeat.canReceiveHeat();
    }

    @Override
    protected void doWork() {
        // 消耗能
        if (consumeEnergy()) {
            // 根据线圈数量计算发热
            int coilCount = getCoilCount();
            int heatToGenerate = coilCount * HEAT_PER_COIL;
            // 产生热能
            heatStorage.generateHeatInternal(heatToGenerate, false);
            isWorking = true;
        } else {
            isWorking = false;
        }
    }

    /**
     * 检查物品是否适合放入指定槽位
     * - 槽位0：电池槽，只能放入电
     * - 槽位1-10：线圈槽，只能放入线圈，每个槽限1
     */
    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        // 电池槽（槽位0）只能放入电
        if (slot == BATTERY_SLOT) {
            return isBattery(stack);
        }

        // 线圈槽（槽位1-10）只能放入线
        if (slot >= COIL_SLOT_START && slot < TOTAL_SLOTS) {
            return isCoil(stack);
        }

        return false;
    }

    /**
     * 检查物品是否是线圈
     */
    private boolean isCoil(ItemStack stack) {
        if (stack.isEmpty()) {
            return true; // 空物品总是有效的（用于清空槽位
        }
        return stack.is(mio_icif_resources.COIL.get());
    }

    @Override
    protected boolean canInsertItem(int slot, ItemStack stack, @Nullable Direction side) {
        // 首先检查槽位是否允许放入该物品
        if (!isItemValidForSlot(slot, stack)) {
            return false;
        }

        // 检查槽位是否已
        ItemStack currentStack = itemHandler.getStackInSlot(slot);
        if (!currentStack.isEmpty()) {
            // 线圈槽每个只能放1个物
            if (slot >= COIL_SLOT_START && slot < TOTAL_SLOTS) {
                return false; // 线圈槽已有物品，不能再放
            }
            // 电池槽的检
            if (!ItemStack.isSameItem(currentStack, stack)) {
                return false;
            }
            if (currentStack.getCount() >= currentStack.getMaxStackSize()) {
                return false;
            }
            if (currentStack.getCount() >= itemHandler.getSlotLimit(slot)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 获取指定方向可访问的槽位
     * - 所有方向都可以访问电池槽和线圈
     */
    @Override
    protected int[] getSlotsForDirection(Direction side) {
        // 所有槽位都可以从任何方向访
        int[] slots = new int[TOTAL_SLOTS];
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            slots[i] = i;
        }
        return slots;
    }

    /**
     * 检查指定槽位是否可以从指定方向提取物品
     * - 电池槽可以提取（用于取出充好电的电池
     * - 线圈槽不能提
     */
    @Override
    protected int getBatterySlot() {
        return BATTERY_SLOT;
    }

    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        // 只有电池槽可以提
        return slot == BATTERY_SLOT;
    }

    /**
     * 获取线圈数量
     */
    public int getCoilCount() {
        int count = 0;
        for (int i = COIL_SLOT_START; i < TOTAL_SLOTS; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.is(mio_icif_resources.COIL.get())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * 应用热损
     */
    protected void applyHeatLossInternal() {
        long loss = heatStorage.applyHeatLoss();
        if (loss > 0) {
            setChanged();
        }
    }

    /**
     * 向相邻方块传导热
     * 对齐 IC2 原版 TileEntityHeatSourceInventory 模型
     * - 发热机将产生的热量存入内
HeatBuffer
 * - 消费者（如高）通过 extractHeat() 主动拉取热量
     * - 这里只做简单的温度差传导作为备用（兼容不主动拉取的机器
     */
    protected void distributeHeat() {
        if (level == null || heatStorage.getHeatStored() <= 0) {
            return;
        }

        int myTemp = heatStorage.getTemperature();

        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = worldPosition.relative(direction);

            IMioIcifCapabilities.IHeatStorage adjacentHeat = level.getCapability(
                    IMioIcifCapabilities.HEAT_STORAGE_BLOCK, adjacentPos, direction.getOpposite());
            if (adjacentHeat == null) {
                adjacentHeat = MioIcifAPI.instance().getCapabilities().adaptHeatStorage(
                    level.getBlockEntity(adjacentPos));
            }

            if (adjacentHeat != null && adjacentHeat.canReceiveHeat()) {
                int adjacentTemp = adjacentHeat.getTemperature();

                if (myTemp > adjacentTemp) {
                    int tempDiff = myTemp - adjacentTemp;

                    long maxTransfer = Math.min(heatStorage.getMaxExtract(),
                            adjacentHeat.getMaxHeatStored() - adjacentHeat.getHeatStored());
                    long heatToTransfer = Math.min(maxTransfer, tempDiff / 5);

                    if (heatToTransfer > 0) {
                        long extracted = heatStorage.extractHeat(heatToTransfer, false);
                        if (extracted > 0) {
                            long received = adjacentHeat.receiveHeat(extracted, false);
                            if (received < extracted) {
                                heatStorage.receiveHeat(extracted - received, false);
                            }
                            setChanged();
                        }
                    }
                }
            }
        }
    }

    public IMioIcifCapabilities.IHeatStorage getHeatStorage() {
        return heatStorage;
    }

    public IMioIcifCapabilities.IHeatStorage getHeatStorageCapability(@Nullable Direction side) {
        return this;
    }

    @Override
    public long getHeatStored() {
        return heatStorage.getHeatStored();
    }

    @Override
    public long getMaxHeatStored() {
        return heatStorage.getMaxHeatStored();
    }

    @Override
    public long receiveHeat(long maxReceive, boolean simulate) {
        return heatStorage.receiveHeat(maxReceive, simulate);
    }

    @Override
    public long extractHeat(long maxExtract, boolean simulate) {
        return heatStorage.extractHeat(maxExtract, simulate);
    }

    @Override
    public boolean canExtractHeat() {
        return heatStorage.canExtractHeat();
    }

    @Override
    public boolean canReceiveHeat() {
        return heatStorage.canReceiveHeat();
    }

    @Override
    public int getTemperature() {
        return heatStorage.getTemperature();
    }

    @Override
    public boolean isOverheated() {
        return heatStorage.isOverheated();
    }

    @Override
    public long getHeatLossPerTick() {
        return heatStorage.getHeatLossPerTick();
    }

    @Override
    public long getMaxReceive() {
        return heatStorage.getMaxReceive();
    }

    @Override
    public long getMaxExtract() {
        return heatStorage.getMaxExtract();
    }

    @Override
    public void setHeat(long heat) {
        heatStorage.setHeat(heat);
    }

    @Override
    public void setCapacity(long capacity) {
        heatStorage.setCapacity(capacity);
    }

    @Override
    public long applyHeatLoss() {
        return heatStorage.applyHeatLoss();
    }

    @Override
    public long consumeHeatInternal(long amount, boolean simulate) {
        return heatStorage.consumeHeatInternal(amount, simulate);
    }

    @Override
    public long generateHeatInternal(long amount, boolean simulate) {
        return heatStorage.generateHeatInternal(amount, simulate);
    }

    /**
     * 获取当前热能产生速率（根据线圈数量）
     */
    public int getHeatGeneration() {
        return getCoilCount() * HEAT_PER_COIL;
    }

    /**
     * 获取每个线圈的热能产生速率
     */
    public static int getHeatPerCoil() {
        return HEAT_PER_COIL;
    }

    /**
     * 获取最大热能产生速率
     */
    public static int getMaxHeatGeneration() {
        return MAX_HEAT_GENERATION;
    }

    /**
     * 获取效率（FE 
HU 的倍率
     */
    public float getEfficiency() {
        return (float) getHeatGeneration() / energyPerTick;
    }

    /**
     * 获取热能容量
     */
    public long getHeatCapacity() {
        return heatStorage.getMaxHeatStored();
    }

    /**
     * 获取热能存储百分
     */
    public float getHeatProgress() {
        return (float) heatStorage.getHeatStored() / heatStorage.getMaxHeatStored();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.heat_generator_elc");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.HUEntity.HeatGeneratorElcMenu(containerId, playerInventory, this);
    }

    // ==================== IHeatGeneratorBlock 接口实现 ====================

    @Override
    public int getHeatOutput() {
        return isWorking() ? getHeatGeneration() : 0;
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
    public int getHeatGenerationRate() {
        return getHeatGeneration();
    }
}