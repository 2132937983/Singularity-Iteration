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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;



/**
 * 风力发电机方块实体类
 * 通过风力发电，发电量取决于风力强度、高度和周围障碍物
 * 
 * 槽位结构：1个电池充电槽
 */
@SuppressWarnings("null")
public class mio_icif_wind_generator extends mio_icif_Energy_Generator {

    // 槽位数量：1个电池槽
    public static final int SLOT_COUNT = 1;
    // 电池充电槽索引
    public static final int BATTERY_SLOT = 0;

    // 电能容量（LV 级）
    public static final long ENERGY_CAPACITY = 64000L;
    public static final long MAX_RECEIVE = 0L; // 风力发电机不接受外部能量输入
    public static final long MAX_EXTRACT = 32L; // LV 级最大输出速率 32 EU/tick

    // 刷新间隔：6.4秒 = 128刻
    public static final int UPDATE_INTERVAL = 128;

    // 风力强度范围：0~30
    public static final int MIN_WIND_STRENGTH = 0;
    public static final int MAX_WIND_STRENGTH = 30;

    // 障碍检测范围：向下2格、向上4格、向四周延伸4格（9x9x7空间）
    public static final int OBSTACLE_RANGE_HORIZONTAL = 4;
    public static final int OBSTACLE_RANGE_DOWN = 2;
    public static final int OBSTACLE_RANGE_UP = 4;

    // 高度阈值
    public static final int MIN_Y_FOR_GENERATION = 64;



    // 当前风力强度 (0-30)
    private int windStrength = 15;
    // 距离下次刷新的刻数
    private int ticksUntilUpdate = UPDATE_INTERVAL;
    // 当前是否正在发电
    private boolean isGenerating = false;
    // 当前计算的发电量
    private long currentEnergyOutput = 0L;
    // 当前检测到的障碍数量
    private int obstacleCount = 0;
    // 有效高度
    private int effectiveHeight = 0;

    /**
     * 构造函数（用于 BlockEntityType.Builder）
     */
    public mio_icif_wind_generator(BlockPos pos, BlockState state) {
        this(pos, state, null);
    }

    /**
     * 构造函数（LV 级基础风力发电机）
     */
    public mio_icif_wind_generator(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type != null ? type : mio_icif_block_entities.WIND_GENERATOR_ENTITY_TYPE.get(),
              SlotLayout.builder().battery().build(), 0L, ENERGY_CAPACITY, MAX_RECEIVE, MAX_EXTRACT, CableTier.LV);
    }

    /**
     * 更新风力强度
     * 每6.4秒（128刻）刷新一次
     * 
     * 风力强度变化规则：
     * - 当0≤s≤20时，有10%的概率使强度+1
     * - 当20≤s≤30时，有10%的概率使强度-1
     * - 当0≤s≤9时，有s%的概率使强度-1
     * - 当21≤s≤30时，有(30-s)%的概率使强度+1
     */
    private void updateWindStrength() {
        int s = windStrength;
        
        // 计算概率（使用1~1000的整数表示0.0%~100.0%，精确0.1%）
        // 使用level的随机数生成器（线程安全）
        int rand = level != null ? level.random.nextInt(1000) : 0;
        
        // 当0≤s≤20时，有10%的概率使强度+1
        if (s >= 0 && s <= 20) {
            if (rand < 100) { // 10% = 100/1000
                windStrength = Math.min(MAX_WIND_STRENGTH, s + 1);
                return;
            }
        }
        
        // 当20≤s≤30时，有10%的概率使强度-1
        if (s >= 20 && s <= 30) {
            if (rand < 100) { // 10% = 100/1000
                windStrength = Math.max(MIN_WIND_STRENGTH, s - 1);
                return;
            }
        }
        
        // 当0≤s≤9时，有s%的概率使强度-1
        if (s >= 0 && s <= 9) {
            if (rand < s * 10) { // s% = s*10/1000
                windStrength = Math.max(MIN_WIND_STRENGTH, s - 1);
                return;
            }
        }
        
        // 当21≤s≤30时，有(30-s)%的概率使强度+1
        if (s >= 21 && s <= 30) {
            int prob = (30 - s) * 10; // (30-s)% = (30-s)*10/1000
            if (rand < prob) {
                windStrength = Math.min(MAX_WIND_STRENGTH, s + 1);
                return;
            }
        }
    }

    /**
     * 检测周围障碍物数量
     * 以发电机为中心，向下2格、向上下各4格所形成9x9x7空间
      * 任何非空气方块（除去发电机本身）都会被视作障碍物
     */
    private void checkObstacles(Level level, BlockPos pos) {
        int count = 0;
        
        // 遍历9x9x7空间
        for (int dx = -OBSTACLE_RANGE_HORIZONTAL; dx <= OBSTACLE_RANGE_HORIZONTAL; dx++) {
            for (int dz = -OBSTACLE_RANGE_HORIZONTAL; dz <= OBSTACLE_RANGE_HORIZONTAL; dz++) {
                for (int dy = -OBSTACLE_RANGE_DOWN; dy <= OBSTACLE_RANGE_UP; dy++) {
                    // 跳过发电机本身的位置
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    
                    BlockPos checkPos = pos.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(checkPos);
                    
                    // 如果不是空气方块，算作障碍物
                    if (!state.isAir()) {
                        count++;
                    }
                }
            }
        }
        
        this.obstacleCount = count;
    }

    /**
     * 计算发电量
      * 公式：p = w * s * (h - 64) / 750
      * 其中
      * - w为天气因素系数：晴天时w=1，雨天时w=1.2，雷雨天时w=1.5
      * - s为风力强度
      * - h为有效高度 = y - c（y轴坐标减去障碍数量）
      * - 若计算结果为负数则视为0
      * - y<64时无论如何都不可能产出任何能量
     */
    private int calculateEnergyOutput(Level level, BlockPos pos) {
        int y = pos.getY();
        
        // y<64时无法发电
        if (y < MIN_Y_FOR_GENERATION) {
            this.effectiveHeight = 0;
            return 0;
        }
        
        // 检测障碍物
        checkObstacles(level, pos);
        
        // 计算有效高度
        this.effectiveHeight = y - obstacleCount;
        
        // 有效高度小于64时无法发电
        if (effectiveHeight < MIN_Y_FOR_GENERATION) {
            return 0;
        }
        
        // 获取天气系数
        double weatherFactor;
        if (level.isThundering()) {
            weatherFactor = 1.5; // 雷雨天
        } else if (level.isRaining()) {
            weatherFactor = 1.2; // 雨天
        } else {
            weatherFactor = 1.0; // 晴天
        }
        
        // 计算发电量：p = w * s * (h - 64) / 750
        double output = weatherFactor * windStrength * (effectiveHeight - MIN_Y_FOR_GENERATION) / 750.0;
        
        // 返回整数发电量，负数视为0
        return Math.max(0, (int) Math.round(output));
    }

    /**
     * 生成能量
     * 根据当前计算的发电量生成能量
     */
    @Override
    protected void generateEnergy() {
        if (currentEnergyOutput <= 0) {
            return;
        }
        
         // 使用 CustomEUEnergyStorage 的 generateEnergyInternal 方法
         // 这样可以绕过 maxReceive=0 的限制
        long energyGenerated = Math.min(currentEnergyOutput, 
            energyStorage.getCapacity() - energyStorage.getAmount());
        if (energyGenerated > 0) {
            apiGenerateEnergy(energyGenerated, false);
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
      * 注意：先分配能量，再检查是否应该停止发电
      * 这样可以避免因内部存储满就停止发电，而实际上能量可以通过电网输出
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_wind_generator blockEntity) {
        if (level.isClientSide()) {
            return;
        }

         // 记录之前的发电状态
        boolean wasGenerating = blockEntity.isGenerating;

         // 更新倒计时
        blockEntity.ticksUntilUpdate--;
        
        // 每128刻更新一次风力强度和发电量
        if (blockEntity.ticksUntilUpdate <= 0) {
            blockEntity.ticksUntilUpdate = UPDATE_INTERVAL;
            blockEntity.updateWindStrength();
            blockEntity.currentEnergyOutput = blockEntity.calculateEnergyOutput(level, pos);
        }

        // 先给充电槽中的物品充电，再分配能量到相邻方块
         // 这样可以确保电池和外部电器同时得到能量
        blockEntity.chargeItems();
        
        // 只有当发电机需要直接向相邻方块输出能量时，才调用distributeEnergy()
        if (blockEntity.shouldDirectlyDistributeEnergy()) {
            blockEntity.distributeEnergy();
        }

        // 分配完能量后再检查能量存储是否已满
        // 只有在能量真正无法输出时（存储已满且无法分配），才停止发电
        boolean isEnergyFull = blockEntity.getEnergyStorage().getAmount() >=
                              blockEntity.getEnergyStorage().getCapacity();

        // 更新发电状态
        blockEntity.isGenerating = blockEntity.currentEnergyOutput > 0 && !isEnergyFull;

        // 如果可以发电且能量未满，则生成能量
        if (blockEntity.isGenerating) {
            blockEntity.generateEnergy();
        }

        // 检查发电状态是否改变
        if (wasGenerating != blockEntity.isGenerating) {
            // 更新方块状态
            BlockState newState = level.getBlockState(pos);
            if (newState.hasProperty(com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Wind_Generator.ACTIVE)) {
                newState = newState.setValue(com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Wind_Generator.ACTIVE, blockEntity.isGenerating);
                level.setBlock(pos, newState, 3);
            }
        }

        // 标记方块实体已更新
        blockEntity.setChanged();
    }

    /**
     * 获取燃料的燃烧时间（风力发电机不需要燃料）
     * @param fuel 燃料物品
      * @return 始终返回0，因为风力不需要燃料
     */
    @Override
    public int getFuelBurnTime(ItemStack fuel) {
        // 风力发电机不需要燃料，始终返回0
        return 0;
    }

    /**
      * 检查是否正在燃烧发电
     * 对于风力发电机，返回当前是否正在发电
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
     * 获取当前风力强度
     */
    public int getWindStrength() {
        return windStrength;
    }

    /**
      * 获取当前发电量
     */
    public long getCurrentEnergyOutput() {
        return currentEnergyOutput;
    }

    /**
     * 获取当前障碍数量
     */
    public int getObstacleCount() {
        return obstacleCount;
    }

    /**
     * 获取有效高度
     */
    public int getEffectiveHeight() {
        return effectiveHeight;
    }

    // ==================== WorldlyContainer 接口实现 ====================

    @Override
    public int[] getSlotsForFace(Direction side) {
         // 所有方向都可以访问电池
        return new int[]{BATTERY_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        // 电池槽：只接受电池类物品
        if (slot == BATTERY_SLOT) {
            return isBattery(stack);
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
         // 电池槽可以提取
        return slot == BATTERY_SLOT;
    }

    // ==================== NBT 数据保存 ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("IsGenerating", isGenerating);
        tag.putInt("WindStrength", windStrength);
        tag.putInt("TicksUntilUpdate", ticksUntilUpdate);
        tag.putLong("CurrentEnergyOutput", currentEnergyOutput);
        tag.putInt("ObstacleCount", obstacleCount);
        tag.putInt("EffectiveHeight", effectiveHeight);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        isGenerating = tag.getBoolean("IsGenerating");
        windStrength = tag.getInt("WindStrength");
        ticksUntilUpdate = tag.getInt("TicksUntilUpdate");
        currentEnergyOutput = tag.getLong("CurrentEnergyOutput");
        obstacleCount = tag.getInt("ObstacleCount");
        effectiveHeight = tag.getInt("EffectiveHeight");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("IsGenerating", isGenerating);
        tag.putInt("WindStrength", windStrength);
        tag.putLong("CurrentEnergyOutput", currentEnergyOutput);
        tag.putInt("ObstacleCount", obstacleCount);
        tag.putInt("EffectiveHeight", effectiveHeight);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        isGenerating = tag.getBoolean("IsGenerating");
        windStrength = tag.getInt("WindStrength");
        currentEnergyOutput = tag.getLong("CurrentEnergyOutput");
        obstacleCount = tag.getInt("ObstacleCount");
        effectiveHeight = tag.getInt("EffectiveHeight");
    }

    // ==================== MenuProvider 接口实现 ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.wind_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Generator.WindGeneratorMenu(containerId, playerInventory, this);
    }
}