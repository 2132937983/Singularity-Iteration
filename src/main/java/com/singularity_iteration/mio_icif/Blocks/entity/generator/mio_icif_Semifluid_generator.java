package com.singularity_iteration.mio_icif.Blocks.entity.generator;

import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Generator;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Semifluid_Generator;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * 半流质发电机方块实体
 * 通过燃烧半流质燃料发电
 * 槽位结构：1燃料桶输入槽 + 1空桶输出槽 + 1电池充电槽 = 3
 * 燃料槽容量：10000mb
 * 输出等级：LV，2 EU/t
 */
@SuppressWarnings("null")
public class mio_icif_Semifluid_generator extends mio_icif_Energy_Generator {

    // 槽位数量：3个槽位
    public static final int SLOT_COUNT = 3;
    // 燃料桶输入槽索引
    public static final int FUEL_BUCKET_SLOT = 0;
    // 空桶输出槽索引
    public static final int EMPTY_BUCKET_SLOT = 1;
    // 电池充电槽索引
    public static final int BATTERY_SLOT = 2;

    // 流体配置
    public static final int FUEL_CAPACITY = 10000; // 10000 mb = 10桶燃料
    public static final int FUEL_PER_BUCKET = 1000; // 每桶燃料1000mb
    public static final long ENERGY_PER_BUCKET = 16000L; // 每桶沼气发电16000EU (16 EU/t × 1000ticks)

    // 发电速率 (EU/tick) - LV 级 16 EU/t
    public static final long ENERGY_GENERATION_RATE = 16L; // 16 EU/tick

 // 电能容量 - LV 级
    public static final long ENERGY_CAPACITY = 500000L;
    public static final long MAX_RECEIVE = 0L; // 发电机不接受外部能量输入
    public static final long MAX_EXTRACT = 16L; // LV 级最大输出速率 16 EU/tick

    // 流体存储
    protected final FluidTank fuelTank;

    // 当前正在发电的燃料量（mb）
    private int currentFuelBurning = 0;
    // 当前燃料已发电量
    private int currentFuelEnergyGenerated = 0;

    /**
     * 构造函数（用于 BlockEntityType.Builder）
     */
    public mio_icif_Semifluid_generator(BlockPos pos, BlockState state) {
        this(pos, state, null);
    }

    /**
     * 构造函数
     */
    public mio_icif_Semifluid_generator(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type != null ? type : mio_icif_block_entities.SEMIFLUID_GENERATOR_ENTITY_TYPE.get(),
            SlotLayout.builder().extra(2).battery().build(), ENERGY_GENERATION_RATE, ENERGY_CAPACITY, MAX_RECEIVE, MAX_EXTRACT, CableTier.LV);

        // 初始化燃料存储，只接受沼气
        this.fuelTank = new FluidTank(FUEL_CAPACITY, fluidStack ->
            fluidStack.getFluid() == mio_icif_fluids.BIOGAS.get());
    }

    /**
     * 检查物品是否是沼气燃料桶或沼气单元
     */
    private boolean isFuelBucket(ItemStack stack) {
        return stack.is(mio_icif_fluids.BIOGAS_BUCKET.get()) || mio_icif_cells.isCellContainingFluid(stack, mio_icif_fluids.BIOGAS.get());
    }

    /**
     * 获取燃料的燃烧时间
     * @param fuel 燃料物品
     * @return 燃烧时间 (tick)，如果不是燃料桶返回0
     */
    @Override
    public int getFuelBurnTime(ItemStack fuel) {
        if (isFuelBucket(fuel)) {
            // 返回一个较大的值，实际燃烧时间由燃料量控制
            return Integer.MAX_VALUE;
        }
        return 0;
    }

    /**
     * 处理燃料桶输入槽
     * 将燃料桶/单元中的燃料转移到流体存储，空桶/空单元放入空桶输出槽
     */
    private void handleFuelBucketSlot() {
        ItemStack fuelBucketStack = itemHandler.getStackInSlot(FUEL_BUCKET_SLOT);
        if (fuelBucketStack.isEmpty() || !isFuelBucket(fuelBucketStack)) {
            return;
        }

        // 检查流体存储是否还有空位
        if (fuelTank.getFluidAmount() >= fuelTank.getCapacity()) {
            return;
        }

        // 判断输入是桶还是单元
        boolean isCell = mio_icif_cells.isFluidCell(fuelBucketStack);
        ItemStack emptyContainer = isCell ? mio_icif_cells.getEmptyCellForStack(fuelBucketStack) : new ItemStack(Items.BUCKET);
        if (isCell && emptyContainer.isEmpty()) emptyContainer = new ItemStack(mio_icif_cells.CELL_EMPTY.get());

        // 检查空桶输出槽是否可以容纳
        ItemStack emptyBucketStack = itemHandler.getStackInSlot(EMPTY_BUCKET_SLOT);
        if (!emptyBucketStack.isEmpty()) {
            // 检查槽位中是否已有相同类型的空容器，且未达到最大堆叠数
            if (!ItemStack.isSameItem(emptyBucketStack, emptyContainer) || emptyBucketStack.getCount() >= emptyBucketStack.getMaxStackSize()) {
                return;
            }
        }

        // 转移燃料（1000mb = 1桶）
        int filled = fuelTank.fill(new FluidStack(mio_icif_fluids.BIOGAS.get(), FUEL_PER_BUCKET), IFluidHandler.FluidAction.EXECUTE);
        if (filled >= FUEL_PER_BUCKET) {
            // 消耗燃料桶/单元
            fuelBucketStack.shrink(1);
            // 添加空桶/空单元到空桶输出槽
            if (emptyBucketStack.isEmpty()) {
                itemHandler.setStackInSlot(EMPTY_BUCKET_SLOT, emptyContainer);
            } else {
                emptyBucketStack.grow(1);
            }
            setChanged();
            // 通知客户端更新方块实体数据
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    }

    /**
     * 消耗燃料进行发电
     * 重写父类方法，使用燃料而不是物品燃烧
     */
    @Override
    protected void consumeFuel() {
        // 检查能量存储是否已满
        boolean isEnergyFull = getEnergyStorage().getAmount() >=
                              getEnergyStorage().getCapacity();

        // 如果能量已满，不消耗燃料
        if (isEnergyFull) {
            return;
        }

        // 检查是否有足够的燃料（至少1000mb = 1桶）
        if (fuelTank.getFluidAmount() >= FUEL_PER_BUCKET) {
            // 消耗1000mb燃料
            FluidStack drained = fuelTank.drain(FUEL_PER_BUCKET, IFluidHandler.FluidAction.EXECUTE);
            if (drained.getAmount() >= FUEL_PER_BUCKET) {
                // 开始燃烧这桶燃料
                this.currentFuelBurning = FUEL_PER_BUCKET;
                this.currentFuelEnergyGenerated = 0;
                // 计算燃烧持续的tick数：每桶燃料发电32000EU，发电速率32EU/tick
                this.burnDuration = (int) (ENERGY_PER_BUCKET / ENERGY_GENERATION_RATE); // 32000 / 32 = 1000 ticks
                this.burnTime = this.burnDuration;
                setChanged();
            }
        }
    }

    /**
     * 生成能量
     * 重写父类方法，根据燃料消耗计算发电量
     */
    @Override
    protected void generateEnergy() {
        long totalEnergyGenerated = 0;
        
        // 基于燃料桶的发电
        if (currentFuelBurning > 0) {
            // 生成能量（每tick 20 EU）
            long fuelBucketEnergy = Math.min(energyGenerationRate,
                getEnergyStorage().getCapacity() - getEnergyStorage().getAmount());
            
            if (fuelBucketEnergy > 0) {
                totalEnergyGenerated += fuelBucketEnergy;
                currentFuelEnergyGenerated += (int) fuelBucketEnergy;
                
                // 每生成2EU，减1mb燃料（2000EU对应1000mb）
                int mbToRemove = currentFuelEnergyGenerated / (int)(ENERGY_PER_BUCKET / FUEL_PER_BUCKET);
                if (mbToRemove > 0) {
                    currentFuelBurning -= mbToRemove;
                    currentFuelEnergyGenerated = currentFuelEnergyGenerated % (int)(ENERGY_PER_BUCKET / FUEL_PER_BUCKET);
                    
                    if (currentFuelBurning < 0) {
                        currentFuelBurning = 0;
                    }
                }
            }
        }
        
        // 实际存储生成的能量（使用generateEnergyInternal绕过maxReceive限制）
        if (totalEnergyGenerated > 0) {
            apiGenerateEnergy(totalEnergyGenerated, false);
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

        // 检查物品是否是电池
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
     * 
     * 注意：先分配能量，再检查是否应该停止燃烧
     * 这样可以避免因内部存储满就停止燃烧，而实际上能量可以通过电网输出
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_Semifluid_generator blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 记录之前的燃烧状态
        boolean wasBurning = blockEntity.isBurning();

        // 处理燃料桶输入槽
        blockEntity.handleFuelBucketSlot();

        // 先给充电槽中的物品充电，再分配能量到相邻方块
        blockEntity.chargeItems();
        
        // 只有当发电机需要直接向相邻方块输出能量时，才调用distributeEnergy()
        if (blockEntity.shouldDirectlyDistributeEnergy()) {
            blockEntity.distributeEnergy();
        }

        // 分配完能量后再检查能量存储是否已满
        // 只有在能量真正无法输出时（存储已满且无法分配），才停止燃烧
        boolean isEnergyFull = blockEntity.getEnergyStorage().getAmount() >=
                              blockEntity.getEnergyStorage().getCapacity();

        // 如果能量未满且正在燃烧，生成能量
        if (!isEnergyFull && blockEntity.isBurning()) {
            blockEntity.generateEnergy();
        }

        // 如果正在燃烧，处理燃烧逻辑
        if (blockEntity.isBurning()) {
            if (isEnergyFull) {
                // 存储已满且电网无需求：让当前燃烧进度自然递减，不生成能量也不消耗新燃料
                blockEntity.burnTime--;
                // 当前燃料烧完后，不尝试消耗新燃料
                if (blockEntity.burnTime <= 0) {
                    blockEntity.burnTime = 0;
                    blockEntity.currentFuelBurning = 0;
                    blockEntity.currentFuelEnergyGenerated = 0;
                }
            } else {
                blockEntity.burnTime--;

                // 如果燃烧完毕，尝试消耗新的燃料
                if (blockEntity.burnTime <= 0) {
                    blockEntity.currentFuelBurning = 0;
                    blockEntity.currentFuelEnergyGenerated = 0;
                    blockEntity.consumeFuel();
                }
            }
        } else {
            // 如果没有在燃烧且能量未满，尝试消耗新的燃料
            if (!isEnergyFull) {
                blockEntity.consumeFuel();
            }
        }

        // 检查燃烧状态是否改变
        boolean isBurning = blockEntity.isBurning();
        if (wasBurning != isBurning) {
            // 更新方块状态
            BlockState newState = level.getBlockState(pos);
            // 使用 ACTIVE 属性更新状态
            if (newState.hasProperty(mio_icif_Block_Semifluid_Generator.ACTIVE)) {
                newState = newState.setValue(mio_icif_Block_Semifluid_Generator.ACTIVE, isBurning);
                level.setBlock(pos, newState, 3);
            }
        }

        // 标记方块实体已更新
        blockEntity.setChanged();
    }

    // ==================== WorldlyContainer 接口实现 ====================

    @Override
    public int[] getSlotsForFace(Direction side) {
        // 所有方向都可以访问所有槽位
        return new int[]{FUEL_BUCKET_SLOT, EMPTY_BUCKET_SLOT, BATTERY_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        // 燃料桶输入槽：只接受燃料桶
        if (slot == FUEL_BUCKET_SLOT) {
            return isFuelBucket(stack);
        }
        // 空桶输出槽：不允许放入
        if (slot == EMPTY_BUCKET_SLOT) {
            return false;
        }
        // 电池槽：只接受电池类物品
        if (slot == BATTERY_SLOT) {
            return isBattery(stack);
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        // 空桶输出槽可以提取
        if (slot == EMPTY_BUCKET_SLOT) {
            return true;
        }
        // 电池槽可以提取（电池充满后可以取出）
        if (slot == BATTERY_SLOT) {
            return true;
        }
        // 燃料桶输入槽可以提取（允许玩家取回未使用的燃料桶）
        if (slot == FUEL_BUCKET_SLOT) {
            return true;
        }
        return false;
    }

    // ==================== 流体处理 ====================

    /**
     * 获取燃料处理器
     */
    public IFluidHandler getFuelHandler() {
        return fuelTank;
    }

    /**
     * 获取燃料处理器（用于特定方向）
     */
    @Nullable
    public IFluidHandler getFuelHandlerCapability(@Nullable Direction side) {
        return fuelTank;
    }

    /**
     * 获取当前燃料储量（mb）
     */
    public int getFuelAmount() {
        return fuelTank.getFluidAmount();
    }

    /**
     * 获取最大燃料容量（mb）
     */
    public int getFuelCapacity() {
        return fuelTank.getCapacity();
    }

    /**
     * 获取当前燃料
     */
    public FluidStack getFuel() {
        return fuelTank.getFluid();
    }

    /**
     * 获取燃料储量百分比（用于GUI显示）
     */
    public int getFuelProgress() {
        if (fuelTank.getCapacity() <= 0) {
            return 0;
        }
        return (fuelTank.getFluidAmount() * 100) / fuelTank.getCapacity();
    }

    /**
     * 获取燃料桶数量（用于GUI显示)
     */
    public int getFuelBuckets() {
        return fuelTank.getFluidAmount() / FUEL_PER_BUCKET;
    }

    // ==================== NBT 数据保存 ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // 保存燃料数据
        tag.put("FuelTank", fuelTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("CurrentFuelBurning", currentFuelBurning);
        tag.putInt("CurrentFuelEnergyGenerated", currentFuelEnergyGenerated);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // 加载燃料数据
        if (tag.contains("FuelTank")) {
            fuelTank.readFromNBT(registries, tag.getCompound("FuelTank"));
        }
        currentFuelBurning = tag.getInt("CurrentFuelBurning");
        currentFuelEnergyGenerated = tag.getInt("CurrentFuelEnergyGenerated");
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("FuelTank", fuelTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("CurrentFuelBurning", currentFuelBurning);
        tag.putInt("CurrentFuelEnergyGenerated", currentFuelEnergyGenerated);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (tag.contains("FuelTank")) {
            fuelTank.readFromNBT(registries, tag.getCompound("FuelTank"));
        }
        currentFuelBurning = tag.getInt("CurrentFuelBurning");
        currentFuelEnergyGenerated = tag.getInt("CurrentFuelEnergyGenerated");
    }

    // ==================== MenuProvider 接口实现 ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.semifluid_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Generator.SemifluidGeneratorMenu(containerId, playerInventory, this, this.getItemHandler(), null);
    }

    // ==================== 流体能力注册 ====================

    /**
     * 注册流体能力
     */
    public static void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.SEMIFLUID_GENERATOR_ENTITY_TYPE.get(),
            (be, side) -> be.getFuelHandlerCapability(side)
        );
    }
}