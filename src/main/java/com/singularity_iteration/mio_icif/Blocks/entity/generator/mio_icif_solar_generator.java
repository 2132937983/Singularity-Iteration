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
 * 太阳能发电机方块实体类
 * 仅在白天工作，只能在主世界使用
 * 正上方任何高度都不能有非透明方块阻挡
 * 发电时间段为6:20~17:45，平均每天能发电13050EU，输出电压为1EU/t
 * 下雨或下雪时不发电
 * 
 * 槽位结构：1个电池充电槽
 */
@SuppressWarnings("null")
public class mio_icif_solar_generator extends mio_icif_Energy_Generator {

    // 槽位数量：1个电池槽
    public static final int SLOT_COUNT = 1;
    // 电池充电槽索引
    public static final int BATTERY_SLOT = 0;

    // 发电速率 (EU/tick) - LV，1 EU/t
    public static final long ENERGY_GENERATION_RATE = 1L;

    // 能量容量（LV 级）
    public static final long ENERGY_CAPACITY = 100000L;
    public static final long MAX_RECEIVE = 0L; // 太阳能发电机不接受外部能量输入
    public static final long MAX_EXTRACT = 32L; // LV 级最大输出速率 32 EU/tick

    // 发电时间段（以游戏刻计算，一天24000刻）
    // Minecraft 时间：0 = 6:00 (日出), 6000 = 12:00 (正午), 12000 = 18:00 (日落), 18000 = 0:00 (午夜)
    // 6:20 = 0 + 20 * 1000/60 ≈ 333 刻
    // 17:45 = 6000 + 5700 = 11700 刻（从中午往前推）
    // 实际上：18:00 = 12000, 所以 17:45 = 12000 - 15*1000/60 = 12000 - 250 = 11750
    public static final int GENERATION_START_TIME = 333;   // 6:20 (日出+20分钟)
    public static final int GENERATION_END_TIME = 11750;   // 17:45 (日落-15分钟)

    // 一天的总发电时间刻数
    public static final int TOTAL_GENERATION_TICKS = GENERATION_END_TIME - GENERATION_START_TIME; // 11583刻
    // 平均每天发电量13050EU，所以实际发电速率需要调整
    // 13050 EU / 11583 ticks ≈ 1.126 EU/t，但设定为1 EU/t

    // 当前是否正在发电（用于客户端同步和方块状态更新）
    private boolean isGenerating = false;

    /**
     * 构造函数（用于 BlockEntityType.Builder）
     */
    public mio_icif_solar_generator(BlockPos pos, BlockState state) {
        this(pos, state, null);
    }

    /**
     * 构造函数
     */
    public mio_icif_solar_generator(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        this(pos, state, type, CableTier.LV);
    }

    /**
     * 构造函数（带 CableTier）
     */
    public mio_icif_solar_generator(BlockPos pos, BlockState state, BlockEntityType<?> type, CableTier cableTier) {
        super(pos, state, type != null ? type : mio_icif_block_entities.SOLAR_GENERATOR_ENTITY_TYPE.get(),
              SlotLayout.builder().battery().build(), ENERGY_GENERATION_RATE, ENERGY_CAPACITY, MAX_RECEIVE, MAX_EXTRACT, cableTier);
    }

    /**
     * 检查是否可以发电
     * 条件：
     * 1. 在主世界
     * 2. 时间在6:20~17:45之间
     * 3. 天气晴朗（不下雨/下雪）
     * 4. 正上方没有非透明方块阻挡
     */
    public boolean canGenerate(Level level, BlockPos pos) {
        // 检查是否在主世界
        if (!isOverworld(level)) {
            return false;
        }

        // 检查天气（下雨或下雪时不发电）
        if (level.isRaining() || level.isThundering()) {
            return false;
        }

        // 检查时间
        long timeOfDay = level.getDayTime() % 24000;
        if (timeOfDay < GENERATION_START_TIME || timeOfDay > GENERATION_END_TIME) {
            return false;
        }

        // 检查上方是否有非透明方块阻挡
        if (!hasClearSky(level, pos)) {
            return false;
        }

        return true;
    }

    /**
     * 检查是否在主世界
     */
    protected boolean isOverworld(Level level) {
        return level.dimension() == Level.OVERWORLD;
    }

    /**
     * 检查正上方是否有非透明方块阻挡
     * 使用 Minecraft 内置的 canSeeSky 方法
     */
    protected boolean hasClearSky(Level level, BlockPos pos) {
        // 使用 Minecraft 内置方法检查该位置是否能看到天空
        return level.canSeeSky(pos.above());
    }

    /**
     * 生成能量
     * 太阳能发电机在条件满足时持续发电
     */
    @Override
    protected void generateEnergy() {
        // 使用 CustomEUEnergyStorage 的 generateEnergyInternal 方法
        // 这样可以绕过 maxReceive=0 的限制
        long energyGenerated = Math.min(energyGenerationRate, 
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
     * 
     * 注意：先分配能量，再检查是否应该停止发电
     * 这样可以避免因内部存储满就停止发电，而实际上能量可以通过电网输出
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_solar_generator blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 记录之前的发电状态
        boolean wasGenerating = blockEntity.isGenerating;

        // 先给充电槽中的物品充电，再分配能量到相邻方块
        // 这样可以确保电池和外部电器同时得到能量
        blockEntity.chargeItems();
        
        // 只有当发电机需要直接向相邻方块输出能量时，才调用distributeEnergy()
        if (blockEntity.shouldDirectlyDistributeEnergy()) {
            blockEntity.distributeEnergy();
        }

        // 分配完能量后再检查是否可以发电
        // 检查是否可以发电
        boolean canGenerate = blockEntity.canGenerate(level, pos);

        // 检查能量存储是否已满
        boolean isEnergyFull = blockEntity.getEnergyStorage().getAmount() >=
                              blockEntity.getEnergyStorage().getCapacity();

        // 更新发电状态
        blockEntity.isGenerating = canGenerate && !isEnergyFull;

        // 如果可以发电且能量未满，则生成能量
        if (blockEntity.isGenerating) {
            blockEntity.generateEnergy();
        }

        // 检查发电状态是否改变
        if (wasGenerating != blockEntity.isGenerating) {
            // 更新方块状态
            BlockState newState = level.getBlockState(pos);
            if (newState.hasProperty(com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Solar_Generator.ACTIVE)) {
                newState = newState.setValue(com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Solar_Generator.ACTIVE, blockEntity.isGenerating);
                level.setBlock(pos, newState, 3);
            }
        }

        // 标记方块实体已更新
        blockEntity.setChanged();
    }

    /**
     * 获取燃料的燃烧时间（太阳能发电机不需要燃料）
     * @param fuel 燃料物品
     * @return 始终返回0，因为太阳能不需要燃料
     */
    @Override
    public int getFuelBurnTime(ItemStack fuel) {
        // 太阳能发电机不需要燃料，始终返回0
        return 0;
    }

    /**
     * 检查是否正在燃烧/发电
     * 对于太阳能发电机，返回当前是否正在发电
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

    // ==================== WorldlyContainer 接口实现 ====================

    @Override
    public int[] getSlotsForFace(Direction side) {
        // 所有方向都可以访问电池槽
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
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        isGenerating = tag.getBoolean("IsGenerating");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("IsGenerating", isGenerating);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        isGenerating = tag.getBoolean("IsGenerating");
    }

    // ==================== MenuProvider 接口实现 ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.solar_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Generator.SolarGeneratorMenu(containerId, playerInventory, this);
    }
}