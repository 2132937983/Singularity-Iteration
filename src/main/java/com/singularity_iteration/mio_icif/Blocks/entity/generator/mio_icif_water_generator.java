package com.singularity_iteration.mio_icif.Blocks.entity.generator;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Generator;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 水力发电机方块实体类
 * 通过消耗水桶发电，每个水桶4000EU
 * 当水力发电机面前有水时也会产生微量电力
 *
 * 槽位结构：1个水桶槽 + 1个电池充电槽
 */
@SuppressWarnings("null")
public class mio_icif_water_generator extends mio_icif_Energy_Generator {

    // 槽位数量：2个水桶槽 + 1个电池槽
    public static final int SLOT_COUNT = 2;
    // 水桶槽索引
    public static final int BUCKET_SLOT = 0;
    // 电池充电槽索引
    public static final int BATTERY_SLOT = 1;

    // 能量容量（LV 级）
    public static final long ENERGY_CAPACITY = 64000L;
    public static final long MAX_RECEIVE = 0L; // 水力发电机不接受外部能量输入
    public static final long MAX_EXTRACT = 32L; // LV 级最大输出速率 32 EU/tick

    // 水槽容量：25000 mb
    public static final int WATER_CAPACITY = 25000;
    // 每个水桶的发电量
    public static final long ENERGY_PER_BUCKET = 4000L;
    // 每个水桶的水量
    public static final int WATER_PER_BUCKET = 1000;
    // 面前有水时的微量发电量(EU/tick)
    public static final long WATER_FACE_GENERATION = 2L;

    // 当前水槽中的水量 (mb)
    private int waterAmount = 0;
    // 当前是否正在发电
    private boolean isGenerating = false;
    // 当前计算的发电量
    private long currentEnergyOutput = 0L;
    // 面前是否有水
    private boolean hasWaterInFront = false;

    /**
     * 构造函数（用于 BlockEntityType.Builder）
     */
    public mio_icif_water_generator(BlockPos pos, BlockState state) {
        this(pos, state, null);
    }

    /**
     * 构造函数
     */
    public mio_icif_water_generator(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type != null ? type : mio_icif_block_entities.WATER_GENERATOR_ENTITY_TYPE.get(),
              SlotLayout.builder().extra(1).battery().build(), 0L, ENERGY_CAPACITY, MAX_RECEIVE, MAX_EXTRACT, CableTier.LV);
    }

    /**
     * 检查面前是否有水
     */
    private void checkWaterInFront(Level level, BlockPos pos, BlockState state) {
        // 获取方块朝向
        Direction facing = state.getValue(com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Water_Generator.FACING);
        // 检查朝向的方块是否是水
        BlockPos frontPos = pos.relative(facing);
        BlockState frontState = level.getBlockState(frontPos);
        this.hasWaterInFront = frontState.is(Blocks.WATER);
    }

    /**
     * 尝试消耗水桶
     * 返回是否成功消耗
     */
    private boolean tryConsumeBucket() {
        ItemStack bucketStack = itemHandler.getStackInSlot(BUCKET_SLOT);
        if (bucketStack.isEmpty()) {
            return false;
        }

        // 检查是否是水桶
        if (bucketStack.is(Items.WATER_BUCKET)) {
            // 检查水槽是否有足够空间
            int spaceAvailable = WATER_CAPACITY - waterAmount;
            if (spaceAvailable < WATER_PER_BUCKET) {
                return false;
            }

            // 消耗水桶，添加水量
            bucketStack.shrink(1);
            waterAmount += WATER_PER_BUCKET;

            // 如果槽位为空，放入空桶
            if (bucketStack.isEmpty()) {
                itemHandler.setStackInSlot(BUCKET_SLOT, new ItemStack(Items.BUCKET));
            } else {
                // 尝试将空桶放入槽位或玩家背包
                // 这里简化处理，直接替换（实际应该尝试合并）
                // 由于槽位只能放一个物品，这里需要处理溢出
                // 暂时简单处理：如果槽位满了，就不给空桶（或者可以掉落）
                // 更好的做法是在tick中处理空桶输出
            }

            setChanged();
            return true;
        }

        return false;
    }

    /**
     * 计算发电量
     */
    private long calculateEnergyOutput(Level level, BlockPos pos, BlockState state) {
        long output = 0;

        // 检查面前是否有水，产生微量电力
        checkWaterInFront(level, pos, state);
        if (hasWaterInFront) {
            output += WATER_FACE_GENERATION;
        }

        // 如果有水在水槽中，根据水量发电
        if (waterAmount > 0) {
            // 计算可以发多少电（每1000mb水发4000EU）
            // 每tick消耗1mb水，产生4EU
            output += 4;
        }

        return output;
    }

    /**
     * 生成能量
     */
    @Override
    protected void generateEnergy() {
        if (currentEnergyOutput <= 0) {
            return;
        }

        // 使用 CustomEUEnergyStorage 的 generateEnergyInternal 方法
        long energyGenerated = Math.min(currentEnergyOutput,
            energyStorage.getCapacity() - energyStorage.getAmount());
        if (energyGenerated > 0) {
            apiGenerateEnergy(energyGenerated, false);

            // 消耗水（如果是从水槽发电）
            if (waterAmount > 0 && currentEnergyOutput > WATER_FACE_GENERATION) {
                waterAmount = Math.max(0, waterAmount - 1);
            }
        }
    }

    /**
     * 给充电槽中的物品充电
     * 重写父类方法，使用 BATTERY_SLOT
     */
    @Override
    protected void chargeItems() {
        ItemStack chargeStack = itemHandler.getStackInSlot(BATTERY_SLOT);
        if (chargeStack.isEmpty()) {
            return;
        }

        // 检查物品是否是电池类
        if (getItemAPI().isBattery(chargeStack)) {
            var api = getItemAPI();
            long currentEnergy = api.getBatteryStored(chargeStack);
            long batteryMaxEnergy = api.getBatteryCapacity(chargeStack);
            long batteryChargeRate = api.getChargeRate(chargeStack);

            if (currentEnergy >= batteryMaxEnergy) {
                return;
            }

            long availableEnergy = getEnergyStorage().getAmount();
            if (availableEnergy <= 0) {
                return;
            }

            long energyToCharge = Math.min(batteryChargeRate, batteryMaxEnergy - currentEnergy);
            energyToCharge = Math.min(energyToCharge, availableEnergy);

            long energyExtracted = getEnergyStorageInternal().extract(energyToCharge, false);

            api.chargeBattery(chargeStack, energyExtracted, false);
            setChanged();
        }
    }

    /**
     * 每tick更新逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_water_generator blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 记录之前的发电状态
        boolean wasGenerating = blockEntity.isGenerating;

        // 尝试消耗水桶（如果槽位有水桶且水槽有空间）
        blockEntity.tryConsumeBucket();

        // 计算发电量
        blockEntity.currentEnergyOutput = blockEntity.calculateEnergyOutput(level, pos, state);

        // 检查能量存储是否已满
        boolean isEnergyFull = blockEntity.getEnergyStorage().getAmount() >=
                              blockEntity.getEnergyStorage().getCapacity();

        // 更新发电状态
        blockEntity.isGenerating = blockEntity.currentEnergyOutput > 0 && !isEnergyFull;

        // 如果可以发电且能量未满，则生成能量
        if (blockEntity.isGenerating) {
            blockEntity.generateEnergy();
        }

        // 先给充电槽中的物品充电，再分配能量到相邻方块
        blockEntity.chargeItems();
        blockEntity.distributeEnergy();

        // 检查发电状态是否改变
        if (wasGenerating != blockEntity.isGenerating) {
            // 更新方块状态
            BlockState newState = level.getBlockState(pos);
            if (newState.hasProperty(com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Water_Generator.ACTIVE)) {
                newState = newState.setValue(com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Water_Generator.ACTIVE, blockEntity.isGenerating);
                level.setBlock(pos, newState, 3);
            }
        }

        // 标记方块实体已更新
        blockEntity.setChanged();
    }

    /**
     * 获取燃料的燃烧时间（水力发电机不需要传统燃料）
     * @param fuel 燃料物品
     * @return 始终返回0，因为水力发电机使用水桶而非燃料
     */
    @Override
    public int getFuelBurnTime(ItemStack fuel) {
        // 水力发电机不使用传统燃料机制
        return 0;
    }

    /**
     * 检查是否正在燃烧/发电
     * 对于水力发电机，返回当前是否正在发电
     */
    @Override
    public boolean isBurning() {
        return isGenerating;
    }

    /**
     * 获取当前发电状态
     */
    public boolean isGenerating() {
        return isGenerating;
    }

    /**
     * 获取当前水量
     */
    public int getWaterAmount() {
        return waterAmount;
    }

    /**
     * 获取最大水量
     */
    public int getMaxWaterAmount() {
        return WATER_CAPACITY;
    }

    /**
     * 获取当前发电量
     */
    public long getCurrentEnergyOutput() {
        return currentEnergyOutput;
    }

    /**
     * 获取面前是否有水
     */
    public boolean hasWaterInFront() {
        return hasWaterInFront;
    }

    /**
     * 获取水桶槽中的物品
     */
    public ItemStack getBucketSlotItem() {
        return itemHandler.getStackInSlot(BUCKET_SLOT);
    }

    // ==================== WorldlyContainer 接口实现 ====================

    @Override
    public int[] getSlotsForFace(Direction side) {
        // 所有方向都可以访问水桶槽和电池槽
        return new int[]{BUCKET_SLOT, BATTERY_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        // 水桶槽：只接受水桶
        if (slot == BUCKET_SLOT) {
            return stack.is(Items.WATER_BUCKET);
        }
        // 电池槽：只接受电池类物品
        if (slot == BATTERY_SLOT) {
            return isBattery(stack);
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        // 水桶槽可以提取（空桶）
        if (slot == BUCKET_SLOT) {
            return stack.is(Items.BUCKET);
        }
        // 电池槽可以提取
        if (slot == BATTERY_SLOT) {
            return true;
        }
        return false;
    }

    // ==================== NBT 数据保存 ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("IsGenerating", isGenerating);
        tag.putInt("WaterAmount", waterAmount);
        tag.putLong("CurrentEnergyOutput", currentEnergyOutput);
        tag.putBoolean("HasWaterInFront", hasWaterInFront);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        isGenerating = tag.getBoolean("IsGenerating");
        waterAmount = tag.getInt("WaterAmount");
        currentEnergyOutput = tag.getLong("CurrentEnergyOutput");
        hasWaterInFront = tag.getBoolean("HasWaterInFront");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("IsGenerating", isGenerating);
        tag.putInt("WaterAmount", waterAmount);
        tag.putLong("CurrentEnergyOutput", currentEnergyOutput);
        tag.putBoolean("HasWaterInFront", hasWaterInFront);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        isGenerating = tag.getBoolean("IsGenerating");
        waterAmount = tag.getInt("WaterAmount");
        currentEnergyOutput = tag.getLong("CurrentEnergyOutput");
        hasWaterInFront = tag.getBoolean("HasWaterInFront");
    }

    // ==================== MenuProvider 接口实现 ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.water_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Generator.WaterGeneratorMenu(containerId, playerInventory, this);
    }
}