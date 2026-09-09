package com.singularity_iteration.mio_icif.Blocks.entity.generator;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Generator;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Geo_Generator;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
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
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * 地热发电机方块实体类
 * 通过燃烧岩浆发电，每桶岩浆（1000mb）发电量10,000EU
 * 槽位结构：1岩浆桶输入槽 + 1空桶输出槽 + 1电池充电槽 = 3
 * 岩浆槽容量：8000mb
 * 输出等级：LV，2 EU/t
 * 周围岩浆方块额外发电：1 EU/t/面
 */
@SuppressWarnings("null")
public class mio_icif_geo_generator extends mio_icif_Energy_Generator {

    // 槽位数量：3个槽位
    public static final int SLOT_COUNT = 3;
    // 岩浆桶输入槽索引
    public static final int LAVA_BUCKET_SLOT = 0;
    // 空桶输出槽索引
    public static final int EMPTY_BUCKET_SLOT = 1;
    // 电池充电槽索引
    public static final int BATTERY_SLOT = 2;

    // 流体配置
    public static final int LAVA_CAPACITY = 8000; // 8000 mb = 8桶岩浆
    public static final int LAVA_PER_BUCKET = 1000; // 每桶岩浆1000mb
    public static final int ENERGY_PER_BUCKET = 10000; // 每桶岩浆发电10000EU

    // 发电速率 (EU/tick) - LV 级 20 EU/t
    public static final long ENERGY_GENERATION_RATE = 20L; // 20 EU/tick

 // 电能容量 - LV 级
    public static final long ENERGY_CAPACITY = 2400L; // 2,400 EU
    public static final long MAX_RECEIVE = 0L; // 发电机不接受外部能量输入
    public static final long MAX_EXTRACT = 32L; // LV 级最大输出速率 32 EU/tick

    // 流体存储
    protected final FluidTank lavaTank;

    // 当前正在发电的岩浆量（mb）
    private int currentLavaBurning = 0;
    // 当前岩浆已发电量
    private int currentLavaEnergyGenerated = 0;

    /**
     * 构造函数（用于 BlockEntityType.Builder）
     */
    public mio_icif_geo_generator(BlockPos pos, BlockState state) {
        this(pos, state, null);
    }

    /**
     * 构造函数
     */
    public mio_icif_geo_generator(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type != null ? type : mio_icif_block_entities.GEO_GENERATOR_ENTITY_TYPE.get(),
            SlotLayout.builder().extra(2).battery().build(), ENERGY_GENERATION_RATE, ENERGY_CAPACITY, MAX_RECEIVE, MAX_EXTRACT, CableTier.LV);

        // 初始化岩浆存储，只接受岩浆
        this.lavaTank = new FluidTank(LAVA_CAPACITY, fluidStack ->
            fluidStack.getFluid() == Fluids.LAVA);
    }

    /**
     * 检查物品是否是岩浆桶或岩浆单元
     */
    private boolean isLavaBucket(ItemStack stack) {
        return stack.is(Items.LAVA_BUCKET) || mio_icif_cells.isCellContainingFluid(stack, Fluids.LAVA);
    }

    /**
     * 获取燃料的燃烧时间（地热发电机使用岩浆）
     * @param fuel 燃料物品
     * @return 燃烧时间 (tick)，如果不是岩浆桶返回0
     */
    @Override
    public int getFuelBurnTime(ItemStack fuel) {
        if (isLavaBucket(fuel)) {
            // 返回一个较大的值，实际燃烧时间由岩浆量控制
            return Integer.MAX_VALUE;
        }
        return 0;
    }

    /**
     * 处理岩浆桶输入槽
     * 将岩浆桶/单元中的岩浆转移到流体存储，空桶/空单元放入空桶输出槽
     */
    private void handleLavaBucketSlot() {
        ItemStack lavaBucketStack = itemHandler.getStackInSlot(LAVA_BUCKET_SLOT);
        if (lavaBucketStack.isEmpty() || !isLavaBucket(lavaBucketStack)) {
            return;
        }

        // 检查流体存储是否还有空位
        if (lavaTank.getFluidAmount() >= lavaTank.getCapacity()) {
            return;
        }

        // 判断输入是桶还是单元
        boolean isCell = mio_icif_cells.isFluidCell(lavaBucketStack);
        ItemStack emptyContainer = isCell ? mio_icif_cells.getEmptyCellForStack(lavaBucketStack) : new ItemStack(Items.BUCKET);
        if (isCell && emptyContainer.isEmpty()) emptyContainer = new ItemStack(mio_icif_cells.CELL_EMPTY.get());

        // 检查空桶输出槽是否可以容纳
        ItemStack emptyBucketStack = itemHandler.getStackInSlot(EMPTY_BUCKET_SLOT);
        if (!emptyBucketStack.isEmpty()) {
            // 检查槽位中是否已有相同类型的空容器，且未达到最大堆叠数
            if (!ItemStack.isSameItem(emptyBucketStack, emptyContainer) || emptyBucketStack.getCount() >= emptyBucketStack.getMaxStackSize()) {
                return;
            }
        }

        // 转移岩浆（1000mb = 1桶）
        int filled = lavaTank.fill(new FluidStack(Fluids.LAVA, LAVA_PER_BUCKET), IFluidHandler.FluidAction.EXECUTE);
        if (filled >= LAVA_PER_BUCKET) {
            // 消耗岩浆桶/单元
            lavaBucketStack.shrink(1);
            // 添加空桶/空单元到空桶输出槽
            if (emptyBucketStack.isEmpty()) {
                itemHandler.setStackInSlot(EMPTY_BUCKET_SLOT, emptyContainer);
            } else {
                emptyBucketStack.grow(1);
            }
            setChanged();
        }
    }

    /**
     * 消耗岩浆进行发电
     * 重写父类方法，使用岩浆而不是物品燃烧
     */
    @Override
    protected void consumeFuel() {
        // 检查能量存储是否已满
        boolean isEnergyFull = getEnergyStorage().getAmount() >=
                              getEnergyStorage().getCapacity();

        // 如果能量已满，不消耗岩浆
        if (isEnergyFull) {
            return;
        }

        // 检查是否有足够的岩浆（至少1000mb = 1桶）
        if (lavaTank.getFluidAmount() >= LAVA_PER_BUCKET) {
            // 消耗1000mb岩浆
            FluidStack drained = lavaTank.drain(LAVA_PER_BUCKET, IFluidHandler.FluidAction.EXECUTE);
            if (drained.getAmount() >= LAVA_PER_BUCKET) {
                // 开始燃烧这桶岩浆
                this.currentLavaBurning = LAVA_PER_BUCKET;
                this.currentLavaEnergyGenerated = 0;
                this.burnDuration = (int) (ENERGY_PER_BUCKET / energyGenerationRate); // 燃烧持续的tick数
                this.burnTime = this.burnDuration;
                setChanged();
            }
        }
    }

    // 基于周围岩浆方块的发电速率（每面每tick）
    private static final int LAVA_BLOCK_ENERGY_RATE = 1; // 每面每tick 1 EU

    // 当前接触的岩浆方块数量
    private int nearbyLavaBlocks = 0;

    /**
     * 生成能量
     * 重写父类方法，根据岩浆消耗计算发电量
     */
    @Override
    protected void generateEnergy() {
        int totalEnergyGenerated = 0;
        
        // 1. 基于岩浆桶的发电
        if (currentLavaBurning > 0) {
            // 生成能量（每tick 20 EU）
            long lavaBucketEnergy = Math.min(energyGenerationRate,
                getEnergyStorage().getCapacity() - getEnergyStorage().getAmount());
            
            if (lavaBucketEnergy > 0) {
                totalEnergyGenerated += lavaBucketEnergy;
                currentLavaEnergyGenerated += lavaBucketEnergy;
                
                // 每生成10EU，减1mb岩浆（10000EU对应1000mb）
                int mbToRemove = currentLavaEnergyGenerated / (ENERGY_PER_BUCKET / LAVA_PER_BUCKET);
                if (mbToRemove > 0) {
                    currentLavaBurning -= mbToRemove;
                    currentLavaEnergyGenerated = currentLavaEnergyGenerated % (ENERGY_PER_BUCKET / LAVA_PER_BUCKET);
                    
                    if (currentLavaBurning < 0) {
                        currentLavaBurning = 0;
                    }
                }
            }
        }
        
        // 2. 基于周围岩浆方块的发电
        if (nearbyLavaBlocks > 0) {
            int lavaBlockEnergy = Math.min(nearbyLavaBlocks * LAVA_BLOCK_ENERGY_RATE,
                (int)(getEnergyStorage().getCapacity() - getEnergyStorage().getAmount() - totalEnergyGenerated));
            
            if (lavaBlockEnergy > 0) {
                totalEnergyGenerated += lavaBlockEnergy;
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
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_geo_generator blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 检测周围的岩浆方块数量
        blockEntity.checkNearbyLavaBlocks(level, pos);

        // 记录之前的燃烧状态
        boolean wasBurning = blockEntity.isBurning();

        // 处理岩浆桶输入槽
        blockEntity.handleLavaBucketSlot();

        // 先给充电槽中的物品充电，再分配能量到相邻方块
        // 这样可以确保电池和外部电器同时得到能量
        blockEntity.chargeItems();
        
        // 只有当发电机需要直接向相邻方块输出能量时，才调用distributeEnergy()
        if (blockEntity.shouldDirectlyDistributeEnergy()) {
            blockEntity.distributeEnergy();
        }

        // 分配完能量后再检查能量存储是否已满
        // 只有在能量真正无法输出时（存储已满且无法分配），才停止燃烧
        boolean isEnergyFull = blockEntity.getEnergyStorage().getAmount() >=
                              blockEntity.getEnergyStorage().getCapacity();

        // 生成能量（无论是否在燃烧，只要有岩浆方块就发电）
        if (!isEnergyFull) {
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
                    blockEntity.currentLavaBurning = 0;
                    blockEntity.currentLavaEnergyGenerated = 0;
                }
            } else {
                blockEntity.burnTime--;

                // 如果燃烧完毕，尝试消耗新的岩浆
                if (blockEntity.burnTime <= 0) {
                    blockEntity.currentLavaBurning = 0;
                    blockEntity.currentLavaEnergyGenerated = 0;
                    blockEntity.consumeFuel();
                }
            }
        } else {
            // 如果没有在燃烧且能量未满，尝试消耗新的岩浆
            if (!isEnergyFull) {
                blockEntity.consumeFuel();
            }
        }

        // 检查燃烧状态是否改变（包括周围岩浆方块发电的情况）
        boolean isBurning = blockEntity.isBurning() || blockEntity.nearbyLavaBlocks > 0;
        if (wasBurning != isBurning) {
            // 更新方块状态
            BlockState newState = level.getBlockState(pos);
            // 使用 ACTIVE 属性更新状态
            if (newState.hasProperty(mio_icif_Block_Geo_Generator.ACTIVE)) {
                newState = newState.setValue(mio_icif_Block_Geo_Generator.ACTIVE, isBurning);
                level.setBlock(pos, newState, 3);
            }
        }

        // 标记方块实体已更新
        blockEntity.setChanged();
    }

    /**
     * 检测周围的岩浆方块数量
     */
    private void checkNearbyLavaBlocks(Level level, BlockPos pos) {
        int count = 0;
        // 检查六个方向的方块
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = pos.relative(direction);
            BlockState adjacentState = level.getBlockState(adjacentPos);
            // 检查是否是岩浆方块（包括流动岩浆）
            if (adjacentState.getBlock() == Blocks.LAVA) {
                count++;
            }
        }
        this.nearbyLavaBlocks = count;
    }

    // ==================== WorldlyContainer 接口实现 ====================

    @Override
    public int[] getSlotsForFace(Direction side) {
        // 所有方向都可以访问所有槽位
        return new int[]{LAVA_BUCKET_SLOT, EMPTY_BUCKET_SLOT, BATTERY_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        // 岩浆桶输入槽：只接受岩浆桶
        if (slot == LAVA_BUCKET_SLOT) {
            return isLavaBucket(stack);
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
        // 岩浆桶输入槽可以提取（允许玩家取回未使用的岩浆桶）
        if (slot == LAVA_BUCKET_SLOT) {
            return true;
        }
        return false;
    }

    // ==================== 流体处理 ====================

    /**
     * 获取岩浆处理器
     */
    public IFluidHandler getLavaHandler() {
        return lavaTank;
    }

    /**
     * 获取岩浆处理器（用于特定方向）
     */
    @Nullable
    public IFluidHandler getLavaHandlerCapability(@Nullable Direction side) {
        return lavaTank;
    }

    /**
     * 获取当前岩浆储量（mb）
     */
    public int getLavaAmount() {
        return lavaTank.getFluidAmount();
    }

    /**
     * 获取最大岩浆容量（mb）
     */
    public int getLavaCapacity() {
        return lavaTank.getCapacity();
    }

    /**
     * 获取当前岩浆
     */
    public FluidStack getLava() {
        return lavaTank.getFluid();
    }

    /**
     * 获取岩浆储量百分比（用于GUI显示）
     */
    public int getLavaProgress() {
        if (lavaTank.getCapacity() <= 0) {
            return 0;
        }
        return (lavaTank.getFluidAmount() * 100) / lavaTank.getCapacity();
    }

    /**
     * 获取岩浆桶数量（用于GUI显示）
     */
    public int getLavaBuckets() {
        return lavaTank.getFluidAmount() / LAVA_PER_BUCKET;
    }

    // ==================== NBT 数据保存 ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // 保存岩浆数据
        tag.put("LavaTank", lavaTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("CurrentLavaBurning", currentLavaBurning);
        tag.putInt("CurrentLavaEnergyGenerated", currentLavaEnergyGenerated);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // 加载岩浆数据
        if (tag.contains("LavaTank")) {
            lavaTank.readFromNBT(registries, tag.getCompound("LavaTank"));
        }
        currentLavaBurning = tag.getInt("CurrentLavaBurning");
        currentLavaEnergyGenerated = tag.getInt("CurrentLavaEnergyGenerated");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("LavaTank", lavaTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("CurrentLavaBurning", currentLavaBurning);
        tag.putInt("CurrentLavaEnergyGenerated", currentLavaEnergyGenerated);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (tag.contains("LavaTank")) {
            lavaTank.readFromNBT(registries, tag.getCompound("LavaTank"));
        }
        currentLavaBurning = tag.getInt("CurrentLavaBurning");
        currentLavaEnergyGenerated = tag.getInt("CurrentLavaEnergyGenerated");
    }

    // ==================== MenuProvider 接口实现 ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.geo_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Generator.GeoGeneratorMenu(containerId, playerInventory, this, this.getItemHandler(), null);
    }
}