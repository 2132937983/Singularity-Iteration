package com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities;
import com.singularity_iteration.mio_icif.energy.kinetic.KineticStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import com.singularity_iteration.mio_icif.api.machine.IKineticGeneratorBlock;

/**
 * 电力动能机方块实
 * 使用电力产生动能，需要电动马达作为核心部
 * 类似电力加热机的逻辑，但是产生动能而非热能
 * 
 * 特点
 * - 从电网接EU 电力
 * - 消EU 电力产生 KU 动能
 * - 产生的动能可以传导到相邻方块
 * - 11 个槽位：1 个电池槽 + 10 个马达槽
 */
@SuppressWarnings("null")
public class mio_icif_Kinetic_Generator_elc extends mio_icif_producer implements IKineticGeneratorBlock {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .battery()
        .extra(10)
        .build();

    // 槽位定义
    public static final int BATTERY_SLOT = 0;         // 电池
    public static final int MOTOR_SLOT_START = 1;     // 马达槽起
    public static final int MOTOR_SLOT_COUNT = 10;    // 马达槽数
    public static final int TOTAL_SLOTS = 11;         // 总槽位数

    // 每个马达的动能产生速率 (KU/tick)00 KU
    private static final int KINETIC_PER_MOTOR = 100;
    // 最大动能产生速率 (KU/tick)0 个马达：1000 KU
    private static final int MAX_KINETIC_GENERATION = 1000;

    // 默认配置
    public static final long DEFAULT_CAPACITY = 40000L;
    public static final long DEFAULT_MAX_RECEIVE = 1000L;  // 最大功1000 EU/t
    public static final long DEFAULT_MAX_EXTRACT = 0L;

    // 动能存储（用于能力系统注册，实际动能直接输出到前方方块）
    private final KineticStorage kineticStorage;

    /**
     * 构造函
     */
    public mio_icif_Kinetic_Generator_elc(BlockPos pos, BlockState state) {
        // 调用父类构造函数：电力容量 40000，最大接1000，不能输出电
        // 不需要工作进度，所maxProgress = 1
        // 11 个槽位：1 个电池槽 + 10 个马达槽
        super(pos, state, mio_icif_block_entities.KINETIC_GENERATOR_ELC_ENTITY_TYPE.get(),
                DEFAULT_CAPACITY, DEFAULT_MAX_RECEIVE, DEFAULT_MAX_EXTRACT,
                1, LAYOUT,
                100L, CableTier.LV);

        // 初始化动能存储（用于能力系统，容量等于最大产生速率
        this.kineticStorage = new KineticStorage(MAX_KINETIC_GENERATION, 0, MAX_KINETIC_GENERATION, 10000, 0.0f);
    }

    /**
     * 获取当前能量消耗（根据马达数量
     * 1 个马= 100 EU/t0 个马= 1000 EU/t
     * 能量转换比例 EU = 1 KU
     */
    private long getCurrentEnergyConsumption() {
        return getMotorCount() * 100L;
    }

    @Override
    protected boolean hasEnoughEnergy() {
        // 根据马达数量检查是否有足够能量
        return energyStorage.getAmount() >= getCurrentEnergyConsumption();
    }

    @Override
    protected boolean consumeEnergy() {
        // 根据马达数量消耗能
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
        // 需要至少一个马达、有电、且前方有需要动能的机器才能工作
        return getMotorCount() > 0 && hasEnoughEnergy() && hasKineticConsumerInFront();
    }

    /**
     * 检查前方是否有需要动能的机器
     */
    private boolean hasKineticConsumerInFront() {
        if (level == null) return false;

        // 获取方块朝向
        Direction facing = getBlockState().getValue(com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block.FACING);
        BlockPos frontPos = worldPosition.relative(facing);

        // 获取前方方块的动能存储能
        IMioIcifCapabilities.IKineticStorage frontKinetic = level.getCapability(
            IMioIcifCapabilities.KINETIC_STORAGE_BLOCK, frontPos, facing.getOpposite());
        if (frontKinetic == null) {
            BlockEntity be = level.getBlockEntity(frontPos);
            frontKinetic = MioIcifAPI.instance().getCapabilities().adaptKineticStorage(be);
        }

        if (frontKinetic != null && frontKinetic.canReceiveKinetic()) {
            return frontKinetic.getKineticStored() < frontKinetic.getMaxKineticStored();
        }

        return false;
    }

    @Override
    protected void doWork() {
        // 消耗能
        if (consumeEnergy()) {
            // 根据马达数量计算动能产生
            int motorCount = getMotorCount();
            int kineticToGenerate = motorCount * KINETIC_PER_MOTOR;
            // 输出动能
            outputKinetic(kineticToGenerate);
            isWorking = true;
        } else {
            isWorking = false;
        }
    }

    /**
     * 检查物品是否适合放入指定槽位
     * - 槽位 0：电池槽，只能放入电
     * - 槽位 1-10：马达槽，只能放入马达，每个槽限 1 
     */
    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        // 电池槽（槽位 0）只能放入电
        if (slot == BATTERY_SLOT) {
            return isBattery(stack);
        }

        // 马达槽（槽位 1-10）只能放入马
        if (slot >= MOTOR_SLOT_START && slot < TOTAL_SLOTS) {
            return isMotor(stack);
        }

        return false;
    }

    /**
     * 检查物品是否是马达
     */
    private boolean isMotor(ItemStack stack) {
        if (stack.isEmpty()) {
            return true; // 空物品总是有效的（用于清空槽位
        }
        return stack.getItem() == mio_icif_resources.MOTOR.get();
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
            // 马达槽每个只能放 1 个物
            if (slot >= MOTOR_SLOT_START && slot < TOTAL_SLOTS) {
                return false; // 马达槽已有物品，不能再放
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
     * - 所有方向都可以访问电池槽和马达
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
     * - 电池槽可以提取（用于取出放完电的电池
     * - 马达槽不能提
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
     * 获取马达数量
     */
    public int getMotorCount() {
        int count = 0;
        for (int i = MOTOR_SLOT_START; i < TOTAL_SLOTS; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.getItem() == mio_icif_resources.MOTOR.get()) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * 输出动能到相邻方
     * @param kineticAmount 要输出的动能数量
     */
    protected void outputKinetic(int kineticAmount) {
        if (level == null) return;

        // 获取方块朝向
        Direction facing = getBlockState().getValue(com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block.FACING);
        BlockPos frontPos = worldPosition.relative(facing);

        // 获取前方方块的动能存储能
        IMioIcifCapabilities.IKineticStorage frontKinetic = level.getCapability(
            IMioIcifCapabilities.KINETIC_STORAGE_BLOCK, frontPos, facing.getOpposite());
        if (frontKinetic == null) {
            BlockEntity be = level.getBlockEntity(frontPos);
            frontKinetic = MioIcifAPI.instance().getCapabilities().adaptKineticStorage(be);
        }

        if (frontKinetic != null && frontKinetic.canReceiveKinetic()) {
            // 电力动能机的 RPM 基于马达数量：每个马达提1000 RPM，最10000 RPM
            int myRPM = getMotorCount() * 1000;
            int adjacentRPM = frontKinetic.getRPM();

            // 只有转速差才能传输
            if (myRPM > adjacentRPM) {
                long maxReceive = frontKinetic.getMaxKineticStored() - frontKinetic.getKineticStored();
                long kineticToTransfer = Math.min(kineticAmount, maxReceive);
                if (kineticToTransfer > 0) {
                    frontKinetic.receiveKinetic(kineticToTransfer, false);
                }
            }
        }
    }

    /**
     * tick 更新逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_Kinetic_Generator_elc blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 调用父类tick 逻辑（处理能量传输和工作逻辑，包含电池槽放电
        mio_icif_producer.tick(level, pos, state, blockEntity);
    }

    /**
     * 获取当前动能产生速率（根据马达数量）
     */
    public int getKineticGeneration() {
        return getMotorCount() * KINETIC_PER_MOTOR;
    }

    @Override
    public int getKineticOutput() {
        return isWorking() ? getKineticGeneration() : 0;
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
        return getKineticGeneration();
    }

    @Override
    public int getRotorRPM() {
        return kineticStorage != null ? kineticStorage.getRPM() : 0;
    }

    /**
     * 获取每个马达的动能产生速率
     */
    public static int getKineticPerMotor() {
        return KINETIC_PER_MOTOR;
    }

    /**
     * 获取最大动能产生速率
     */
    public static int getMaxKineticGeneration() {
        return MAX_KINETIC_GENERATION;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.kinetic_generator_elc");
    }

    @Override
    public @org.jetbrains.annotations.Nullable net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory playerInventory, net.minecraft.world.entity.player.Player player) {
        return new com.singularity_iteration.mio_icif.Menu.KUEntity.KineticGeneratorElcMenu(id, playerInventory, this.getItemHandler());
    }

    /**
     * 获取动能存储能力
     * 用于能力系统注册
     */
    @Nullable
    public IMioIcifCapabilities.IKineticStorage getKineticStorageCapability(@Nullable Direction side) {
        if (side != null) {
            Direction facing = getBlockState().getValue(com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block.FACING);
            if (side == facing) {
                return MioIcifAPI.instance().getCapabilities().adaptKineticStorage(kineticStorage);
            }
            return null;
        }
        return MioIcifAPI.instance().getCapabilities().adaptKineticStorage(kineticStorage);
    }

    /**
     * 获取物品栏能
     * 用于能力系统注册
     */
    public net.neoforged.neoforge.items.IItemHandler getItemHandlerCapability(@Nullable Direction side) {
        return itemHandler;
    }
}