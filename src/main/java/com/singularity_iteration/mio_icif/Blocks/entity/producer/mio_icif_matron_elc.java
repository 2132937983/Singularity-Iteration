package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
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
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * 作物管理器方块实体类
 * 实现自动化农田管理框架
 * 工作范围：水平方向4格（9*9区域）/ 垂直方向3格（9*3*9区域）
 * 功能包括：
 * - 使用水单元补充水量
 * - 使用肥料物品加速成长
 * - 使用水仓库/水流补充
 *
 * 槽位布局说明：
 * 0: 电池槽
 * 1: 水单元输入槽位：接受水桶/水单元
 * 2: 空单元输出槽位
 * 3-9: 除草剂槽位：7个槽位
 * 10-16: 肥料槽位：7个槽位
 *
 * 流体配置：
 * - 水电容器接受所有流体
 *
 * 崩溃能量消费最大2EU/t（LV等级），不够能量不会强制工作
 */
@SuppressWarnings("null")
public class mio_icif_matron_elc extends mio_icif_producer {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .battery()
        .extra(2)
        .extra(14)
        .build();

    // 槽位布局定义
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_WATER_CELL_INPUT = 1;
    public static final int SLOT_EMPTY_CELL_OUTPUT = 2;
    public static final int SLOT_HERBICIDE_START = 3;
    public static final int SLOT_HERBICIDE_COUNT = 7;
    public static final int SLOT_FERTILIZER_START = 10;
    public static final int SLOT_FERTILIZER_COUNT = 7;
    public static final int TOTAL_SLOTS = 17;

    // 默认配置
    // 默认配置对比IC2原版设置
    public static final long DEFAULT_CAPACITY = 1000L;       // 对比IC2作物管理器
    public static final long DEFAULT_MAX_RECEIVE = 32L;      // 最大输入能量2EU/t (LV等级)
    public static final long DEFAULT_MAX_EXTRACT = 0L;
    public static final int DEFAULT_WORK_TIME = 20;          // 1秒工作周期
    public static final long DEFAULT_ENERGY_PER_TICK = 1L;   // 每次工作消耗1EU

    // 流体配置
    public static final int WATER_CAPACITY = 16000;          // 水箱容量容纳16000mB

    // 工作范围参数
    public static final int HORIZONTAL_RANGE = 4;            // 水平扩散范围4格
    public static final int VERTICAL_RANGE = 1;              // 垂直扩散范围1格

    // 搜索标签用于快速查询物品
    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> FERTILIZERS_TAG = 
        net.minecraft.tags.ItemTags.create(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "fertilizers"));

    // 水体流水槽
    protected final FluidTank waterTank;

    // 工作计数计时
    private int workTimer = 0;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) energyStorage.getAmount();
                case 1 -> (int) energyStorage.getCapacity();
                case 2 -> waterTank.getFluidAmount();
                case 3 -> waterTank.getCapacity();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return 4;
        }
    };

    public mio_icif_matron_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.MATRON_ELC_ENTITY_TYPE.get());
    }

    public mio_icif_matron_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            DEFAULT_WORK_TIME,
            LAYOUT,
            DEFAULT_ENERGY_PER_TICK,
            CableTier.LV); // LV等级，最大输入2EU/t

        this.waterTank = new FluidTank(WATER_CAPACITY, fluidStack -> 
            fluidStack.getFluid().isSame(net.minecraft.world.level.material.Fluids.WATER));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.matron");
    }

    public ContainerData getContainerData() {
        return dataAccess;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Producer.MatronElcMenu(
            containerId, playerInventory, this);
    }

    /**
     * 检查槽位是否存适合特定槽位
     */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_BATTERY -> isBattery(stack);
            case SLOT_WATER_CELL_INPUT -> isWaterCell(stack);
            case SLOT_EMPTY_CELL_OUTPUT -> false; // 输出槽位不允许自动化
            default -> {
                // 除草剂槽位（3-8）：只允许除草剂
                if (slot >= SLOT_HERBICIDE_START && slot < SLOT_HERBICIDE_START + SLOT_HERBICIDE_COUNT) {
                    yield com.singularity_iteration.mio_icif.Items.Normal.MatronHerbicideItem.isHerbicide(stack);
                }
                // 肥料槽位（9-14）：只允许带#c:fertilizers 标签的肥料
                if (slot >= SLOT_FERTILIZER_START && slot < SLOT_FERTILIZER_START + SLOT_FERTILIZER_COUNT) {
                    yield isFertilizerItem(stack);
                }
                yield false;
            }
        };
    }

    /**
     * 检查物品否带有#c:fertilizers 标签或者是骨粉
     */
    private boolean isFertilizerItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        // 检查是否是骨粉
        if (stack.is(net.minecraft.world.item.Items.BONE_MEAL)) {
            return true;
        }
        // 检查是否带有#c:fertilizers 标签
        return stack.is(FERTILIZERS_TAG);
    }

    /**
     * 获取特定方向可访问的槽位
     */
    @Override
    protected int[] getSlotsForDirection(Direction side) {
        // 任何方向所有槽位都可以访问
        int[] slots = new int[TOTAL_SLOTS];
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            slots[i] = i;
        }
        return slots;
    }

    @Override
    protected int getBatterySlot() {
        return SLOT_BATTERY;
    }

    /**
     * 检查是否可以工作
     */
    @Override
    protected boolean canWork() {
        // 检查是否有足够能量
        if (energyStorage.getAmount() < energyPerTick) {
            return false;
        }
        return true;
    }

    /**
     * 不执行工作（作物管理逻辑在tick中处理）
     */
    @Override
    protected void doWork() {
        // 作物管理器相关工作在tick()中处理
    }

    /**
     * 检查物品是否为水单位
     */
    private boolean isWaterCell(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(net.minecraft.world.item.Items.WATER_BUCKET)) return true;
        return mio_icif_cells.isCellContainingFluid(stack, net.minecraft.world.level.material.Fluids.WATER);
    }

    @SuppressWarnings("unused")
    private boolean isEmptyCell(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(net.minecraft.world.item.Items.BUCKET)) return true;
        return mio_icif_cells.isEmptyCell(stack);
    }

    /**
     * 每tick执行作物管理逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_matron_elc blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 调用父类tick方法处理能量相关逻辑
        mio_icif_producer.tick(level, pos, state, blockEntity);

        // 素材水单元转换为水槽，不考虑工作周期
        blockEntity.processWaterCell();

        // 工作计时
        blockEntity.workTimer++;
        if (blockEntity.workTimer < DEFAULT_WORK_TIME) {
            return;
        }
        blockEntity.workTimer = 0;

        // 检查是否可以工作
        if (!blockEntity.canWork()) {
            return;
        }

        // 执行作物管理
        blockEntity.processCrops();
    }

    /**
     * 处理水单元转换操作
     * 水桶/水单元转换为水并放入空单元输出槽
     */
    private void processWaterCell() {
        ItemStack waterCellStack = itemHandler.getStackInSlot(SLOT_WATER_CELL_INPUT);
        if (waterCellStack.isEmpty() || !isWaterCell(waterCellStack)) {
            return;
        }

        // 确定水量是水桶还是空单元
        int waterToAdd = 1000; // 1水桶单位 = 1000mB
        if (waterTank.getFluidAmount() + waterToAdd > waterTank.getCapacity()) {
            return; // 水箱已经满了
        }

        // 检查空气单元的输出槽是否可以接受
        ItemStack emptyCellOutput = itemHandler.getStackInSlot(SLOT_EMPTY_CELL_OUTPUT);
        ItemStack emptyCellToProduce;
        if (waterCellStack.is(net.minecraft.world.item.Items.WATER_BUCKET)) {
            emptyCellToProduce = new ItemStack(net.minecraft.world.item.Items.BUCKET);
        } else if (mio_icif_cells.isFluidCell(waterCellStack)) {
            emptyCellToProduce = mio_icif_cells.getEmptyCellForStack(waterCellStack);
            if (emptyCellToProduce.isEmpty()) emptyCellToProduce = new ItemStack(mio_icif_cells.CELL_EMPTY.get());
        } else {
            return;
        }

        if (emptyCellOutput.isEmpty()) {
            // 空槽位可以直接放置
            itemHandler.setStackInSlot(SLOT_EMPTY_CELL_OUTPUT, emptyCellToProduce);
        } else if (ItemStack.isSameItemSameComponents(emptyCellOutput, emptyCellToProduce) && 
                   emptyCellOutput.getCount() < emptyCellOutput.getMaxStackSize()) {
            // 已有相同物品且可以叠加
            emptyCellOutput.grow(1);
        } else {
            return; // 输出槽无法放置
        }

        // 添加水到水箱
        waterTank.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, waterToAdd), 
                       net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);

        // 消耗水单元格
        waterCellStack.shrink(1);
        if (waterCellStack.isEmpty()) {
            itemHandler.setStackInSlot(SLOT_WATER_CELL_INPUT, ItemStack.EMPTY);
        }
    }

    /**
     * 执行作物管理功能
     * 搜索工作范围内的作物方块并执行管理动作
     */
    private void processCrops() {
        Level level = getLevel();
        if (level == null) return;

        BlockPos center = getBlockPos();
        boolean worked = false;

        // 首先检查中心位置周围土地湿润
        if (tryHydrateFarmland(level, center)) {
            worked = true;
        }

        // 循环工作范围内的方块
        for (int x = -HORIZONTAL_RANGE; x <= HORIZONTAL_RANGE; x++) {
            for (int y = -VERTICAL_RANGE; y <= VERTICAL_RANGE; y++) {
                for (int z = -HORIZONTAL_RANGE; z <= HORIZONTAL_RANGE; z++) {
                    BlockPos checkPos = center.offset(x, y, z);
                    if (level.getBlockEntity(checkPos) instanceof com.singularity_iteration.mio_icif.Blocks.entity.crop.mio_icif_crop_entity cropEntity) {
                        // 依次尝试三级动作：除草 -> 施肥 -> 补水
                        if (tryRemoveWeed(cropEntity)) {
                            worked = true;
                        } else if (tryFertilize(cropEntity)) {
                            worked = true;
                        } else if (tryHydrate(cropEntity)) {
                            worked = true;
                        }

                        // 如果已经有工作了，本次tick不再继续搜索
                        if (worked) {
                            // 消耗能量
                            apiUseEnergy(energyPerTick, false);
                            // 将方块状态设置为工作状态
                            updateWorkingState(level, center, true);
                            return;
                        }
                    }
                }
            }
        }

        // 没有工作需要被执行，设置状态为未工作
        if (worked) {
            apiUseEnergy(energyPerTick, false);
            updateWorkingState(level, center, true);
        } else {
            // 没有工作项，更新状态为未工作
            updateWorkingState(level, center, false);
        }
    }

    /**
     * 更新方块工作状态
     */
    private void updateWorkingState(Level level, BlockPos pos, boolean working) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_matron) {
            boolean currentWorking = state.getValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_matron.WORKING);
            if (currentWorking != working) {
                level.setBlock(pos, state.setValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_matron.WORKING, working), 3);
            }
        }
    }

    /**
     * 尝试灌溉周围所有未湿润的土地
     * 每次湿润消耗10mB水
     * @return 是否成功灌溉任何土地
     */
    private boolean tryHydrateFarmland(Level level, BlockPos center) {
        boolean hydratedAny = false;
        int waterConsumed = 0;
        final int WATER_PER_FARMLAND = 10; // 每块耕地消耗10mB水

        for (int x = -HORIZONTAL_RANGE; x <= HORIZONTAL_RANGE; x++) {
            for (int y = -VERTICAL_RANGE - 1; y <= VERTICAL_RANGE; y++) { // 包含工作架下方的耕地
                for (int z = -HORIZONTAL_RANGE; z <= HORIZONTAL_RANGE; z++) {
                    BlockPos checkPos = center.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);

                    // 检查是否是耕地且未湿润
                    if (state.getBlock() instanceof net.minecraft.world.level.block.FarmBlock) {
                        int moisture = state.getValue(net.minecraft.world.level.block.FarmBlock.MOISTURE);
                        if (moisture < 7) {
                            // 检查是否有足够水源
                            if (waterTank.getFluidAmount() >= WATER_PER_FARMLAND) {
                                // 设置耕地为湿润状态7
                                level.setBlock(checkPos, state.setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE, 7), 3);
                                waterConsumed += WATER_PER_FARMLAND;
                                hydratedAny = true;
                            }
                        }
                    }
                }
            }
        }

        // 消耗水
        if (waterConsumed > 0) {
            waterTank.drain(waterConsumed, IFluidHandler.FluidAction.EXECUTE);
            setChanged();
        }

        return hydratedAny;
    }

    /**
     * 尝试除草操作
     * @return 是否成功除草
     */
    private boolean tryRemoveWeed(com.singularity_iteration.mio_icif.Blocks.entity.crop.mio_icif_crop_entity cropEntity) {
        // 检查植物是否是杂草
        if (cropEntity.getPlant() == null || !cropEntity.getPlant().getTypeId().equals("weed")) {
            return false;
        }

        // 查找除草剂
        for (int i = 0; i < SLOT_HERBICIDE_COUNT; i++) {
            int slot = SLOT_HERBICIDE_START + i;
            ItemStack herbicideStack = itemHandler.getStackInSlot(slot);
            if (!herbicideStack.isEmpty() && com.singularity_iteration.mio_icif.Items.Normal.MatronHerbicideItem.isHerbicide(herbicideStack)) {
                // 重置作物状态
                cropEntity.reset();
                // 设置作物防控为150天
                cropEntity.setWeedControl(150);
                cropEntity.updateState();
                // 消耗除草剂剩余伤害
                herbicideStack.setDamageValue(herbicideStack.getDamageValue() + 1);
                if (herbicideStack.getDamageValue() >= herbicideStack.getMaxDamage()) {
                    itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 尝试施肥操作
     * 参考IC2的applyFertilizer方法，对每个作物补充100%的penalty，但重置到99
     * @return 是否成功施肥作物
     */
    private boolean tryFertilize(com.singularity_iteration.mio_icif.Blocks.entity.crop.mio_icif_crop_entity cropEntity) {
        // 检查作物是否是已存在
        if (cropEntity.getPlant() == null) {
            return false;
        }

        // 检查作物养分是否已满100
        if (cropEntity.getNutrients() >= 100) {
            return false;
        }

        // 查找肥料
        for (int i = 0; i < SLOT_FERTILIZER_COUNT; i++) {
            int slot = SLOT_FERTILIZER_START + i;
            ItemStack fertilizerStack = itemHandler.getStackInSlot(slot);
            if (!fertilizerStack.isEmpty() && isFertilizerItem(fertilizerStack)) {
                // 补充养分重置到100，参考IC2：机械补充90，整体重置100
                cropEntity.setNutrients(Math.min(100, cropEntity.getNutrients() + 90));
                cropEntity.updateState();
                // 消耗肥料物品
                fertilizerStack.shrink(1);
                if (fertilizerStack.isEmpty()) {
                    itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 尝试给水操作
     * 参考IC2的applyHydration方法，自动补充作物生长需水量，作物值从水中存储中补水
     * @return 是否成功补水
     */
    private boolean tryHydrate(com.singularity_iteration.mio_icif.Blocks.entity.crop.mio_icif_crop_entity cropEntity) {
        // 检查作物是否是已存在
        if (cropEntity.getPlant() == null) {
            return false;
        }

        // 检查作物水份水是否已满100
        if (cropEntity.getWater() >= 100) {
            return false;
        }

        // 计算需补充水份消耗水量，1mB = 1水份点
        int waterNeeded = 100 - cropEntity.getWater();
        // 从水存储中抽取水量mB
        FluidStack drained = waterTank.drain(waterNeeded, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty() || drained.getAmount() <= 0) {
            return false;
        }

        // 增加作物水值
        cropEntity.setWater(Math.min(100, cropEntity.getWater() + drained.getAmount()));
        cropEntity.updateState();
        return true;
    }

    /**
     * 获取流体处理器实例
     */
    @Override
    public net.neoforged.neoforge.fluids.capability.IFluidHandler getFluidHandlerCapability(@Nullable Direction direction) {
        // 只返回水槽处理
        return waterTank;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("WaterTank", waterTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("WorkTimer", workTimer);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("WaterTank")) {
            waterTank.readFromNBT(registries, tag.getCompound("WaterTank"));
        }
        workTimer = tag.getInt("WorkTimer");
    }
}