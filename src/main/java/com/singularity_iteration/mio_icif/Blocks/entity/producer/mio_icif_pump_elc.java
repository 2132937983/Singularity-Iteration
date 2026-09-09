package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * 电动泵方块实体类
 * 用于抽取水源、岩浆等流体方块
 * 通过 facing 属性可以朝向任意方向
 * 每抽1000mb流体消耗20EU能量
 *
 * 槽位布局说明：
 * 0: 电池槽
 * 1-4: 4个升级槽
 * 5: 空容器槽：空桶/空单元输入
 * 6: 输出槽位：填充后的流体容器/桶
 */
@SuppressWarnings("null")
public class mio_icif_pump_elc extends mio_icif_producer {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .battery()
        .upgrade(4)
        .input(1)
        .output(1)
        .build();

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_UPGRADE_START = 1;
    public static final int SLOT_UPGRADE_COUNT = 4;
    public static final int SLOT_UPGRADE_END = SLOT_UPGRADE_START + SLOT_UPGRADE_COUNT;
    public static final int SLOT_EMPTY_CONTAINER = 5;
    public static final int SLOT_OUTPUT = 6;
    public static final int TOTAL_SLOTS = 7;

    // 默认配置对比IC2原版
    public static final long DEFAULT_CAPACITY = 20L;      // 对比IC2泵
    public static final long DEFAULT_MAX_RECEIVE = 32L;  // LV级最大输入
    public static final long DEFAULT_MAX_EXTRACT = 0L;
    public static final int DEFAULT_WORK_TIME = 20; // 1秒（20 ticks）抽取1000mb
    public static final long DEFAULT_ENERGY_PER_TICK = 1L; // 实际0.95EU/t取整为1EU/t

    // 流体配置
    public static final int FLUID_CAPACITY = 16000; // 16000 mb = 16桶
    public static final int FLUID_PER_OPERATION = 1000; // 每次操作抽取1000mb
    public static final long ENERGY_PER_1000MB = 20L; // 每1000mb消耗20EU

    // 流体容器
    protected final FluidTank fluidTank;

    // 当前抽取的流体类型
    @SuppressWarnings("unused")
    private net.minecraft.world.level.material.Fluid currentFluidType = Fluids.EMPTY;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> (int) energyStorage.getAmount();
                case 3 -> (int) energyStorage.getCapacity();
                case 4 -> fluidTank.getFluidAmount();
                case 5 -> fluidTank.getCapacity();
                case 6 -> fluidTank.isEmpty() ? -1 : net.minecraft.core.registries.BuiltInRegistries.FLUID.getId(fluidTank.getFluid().getFluid());
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return 7;
        }
    };

    /**
     * 构造 BlockEntityType.Builder 注册用参数构造函数
     */
    public mio_icif_pump_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.PUMP_ELC_ENTITY_TYPE.get());
    }

    public mio_icif_pump_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            DEFAULT_WORK_TIME,
            LAYOUT,
            DEFAULT_ENERGY_PER_TICK,
            CableTier.LV);

        // 流体容器初始接受任何流体
        this.fluidTank = new FluidTank(FLUID_CAPACITY, fluidStack -> true);
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.pump_elc");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_BATTERY -> isBattery(stack);
            case SLOT_EMPTY_CONTAINER -> isEmptyContainer(stack);
            case SLOT_OUTPUT -> false; // 输出槽位不允许自动化
            default -> {
                if (slot >= SLOT_UPGRADE_START && slot < SLOT_UPGRADE_END) {
                    yield getItemAPI().isUpgrade(stack);
                }
                yield false;
            }
        };
    }

    /**
     * 检查槽位是否为空容器（空桶或空单元）
     */
    private boolean isEmptyContainer(ItemStack stack) {
        if (stack.is(Items.BUCKET)) return true;
        return mio_icif_cells.isEmptyCell(stack);
    }

    /**
     * 检查是否为桶类物品，用于判断输出类别
     */
    private boolean isBucket(ItemStack stack) {
        return stack.is(Items.BUCKET);
    }

    /**
     * 获取方块朝向
     */
    private Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block.FACING)) {
            return state.getValue(com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block.FACING);
        }
        return Direction.NORTH;
    }

    /**
     * 获取面对方向的前方位置
     */
    private BlockPos getFrontPos() {
        return worldPosition.relative(getFacing());
    }

    /**
     * 参考IC2泵的流体搜索算法
     * 从startPos开始垂直索引流体，使用类似PumpUtil的算法逻辑
     */
    @Nullable
    private BlockPos searchFluidSource(BlockPos startPos) {
        if (level == null) return null;
        
        BlockPos.MutableBlockPos pos = startPos.mutable();
        int decay = getFlowDecay(pos);
        
        // 第一阶段：向上下两侧扩展检查，搜索流体
        for (int i = 0; i < 64; i++) {
            int newDecay = moveUp(pos);
            
            if (newDecay < 0) {
                newDecay = moveSideways(pos, decay);
                if (newDecay < 0) break;
            }
            decay = newDecay;
        }
        
        // 第二阶段：在同一高度水平扫描流体
        java.util.Set<BlockPos> visited = new java.util.HashSet<>(64);
        
        for (int j = 0; j < 64; j++) {
            visited.add(pos.immutable());
            
            // 尝试向西移动
            pos.move(Direction.WEST);
            if (!visited.contains(pos)) {
                int newDecay = getFlowDecay(pos);
                if (newDecay >= 0) {
                    if (newDecay == 0) return pos.immutable();
                    continue;
                }
            }
            
            // 尝试向东移动
            pos.move(Direction.EAST, 2);
            if (!visited.contains(pos)) {
                int newDecay = getFlowDecay(pos);
                if (newDecay >= 0) {
                    if (newDecay == 0) return pos.immutable();
                    continue;
                }
            }
            
            // 尝试向北移动
            pos.move(Direction.WEST).move(Direction.NORTH);
            if (!visited.contains(pos)) {
                int newDecay = getFlowDecay(pos);
                if (newDecay >= 0) {
                    if (newDecay == 0) return pos.immutable();
                    continue;
                }
            }
            
            // 尝试向南移动
            pos.move(Direction.SOUTH, 2);
            if (!visited.contains(pos)) {
                int newDecay = getFlowDecay(pos);
                if (newDecay >= 0) {
                    if (newDecay == 0) return pos.immutable();
                    continue;
                }
            }
            
            // 居中并向下移动到下次迭代起点
            pos.move(Direction.NORTH).move(Direction.WEST);
        }
        
        // 第三阶段：在5x5区域内搜索流体
        BlockPos.MutableBlockPos cPos = pos.mutable();
        for (int ix = -2; ix <= 2; ix++) {
            for (int iz = -2; iz <= 2; iz++) {
                cPos.set(pos.getX() + ix, pos.getY(), pos.getZ() + iz);
                decay = getFlowDecay(cPos);
                
                if (decay >= 0) {
                    if (decay == 0) {
                        return cPos.immutable();
                    }
                    // 如果是水源方块，尝试提升为无限水
                    if (decay >= 1 && decay < 7) {
                        BlockState state = level.getBlockState(cPos);
                        if (state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) {
                            // 增加流体等级以使水成为永久源
                            level.setBlock(cPos, state.setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, decay + 1), 3);
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * 获取方块流体等级
     * 0 = 源方块, 1-7 = 流动等级, -1 = 不是流体
     */
    @SuppressWarnings("unused")
    private int getFlowDecay(BlockPos pos) {
        return getFlowDecay(pos.mutable());
    }
    
    private int getFlowDecay(BlockPos.MutableBlockPos pos) {
        if (level == null) return -1;
        
        BlockState state = level.getBlockState(pos);
        
        // 检查是否为液体方块
        if (state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) {
            int level = state.getValue(net.minecraft.world.level.block.LiquidBlock.LEVEL);
            return level;
        }
        
        // 检查是否为可交互流体（水源、岩浆等）
        FluidState fluidState = level.getFluidState(pos);
        if (!fluidState.isEmpty()) {
            if (fluidState.isSource()) return 0;
            return fluidState.getAmount();
        }
        
        return -1;
    }
    
    /**
     * 向上移动寻找流体
     */
    private int moveUp(BlockPos.MutableBlockPos pos) {
        // 向上移动
        pos.move(Direction.UP);
        int newDecay = getFlowDecay(pos);
        if (newDecay >= 0) return newDecay;
        
        // 尝试一个水平扫描
        pos.move(Direction.DOWN).move(Direction.EAST);
        newDecay = getFlowDecay(pos);
        if (newDecay >= 0) return newDecay;
        
        pos.move(Direction.WEST, 2);
        newDecay = getFlowDecay(pos);
        if (newDecay >= 0) return newDecay;
        
        pos.move(Direction.EAST).move(Direction.SOUTH);
        newDecay = getFlowDecay(pos);
        if (newDecay >= 0) return newDecay;
        
        pos.move(Direction.NORTH, 2);
        newDecay = getFlowDecay(pos);
        if (newDecay >= 0) return newDecay;
        
        // 重置位置并向下移动
        pos.move(Direction.SOUTH).move(Direction.DOWN);
        return -1;
    }
    
    /**
     * 东西横向移动寻找流体
     */
    private int moveSideways(BlockPos.MutableBlockPos pos, int decay) {
        // 向西
        pos.move(Direction.WEST);
        int newDecay = getFlowDecay(pos);
        if (newDecay >= 0 && newDecay < decay) return newDecay;
        
        // 向东
        pos.move(Direction.EAST, 2);
        newDecay = getFlowDecay(pos);
        if (newDecay >= 0 && newDecay < decay) return newDecay;
        
        // 向北
        pos.move(Direction.WEST).move(Direction.NORTH);
        newDecay = getFlowDecay(pos);
        if (newDecay >= 0 && newDecay < decay) return newDecay;
        
        // 向南
        pos.move(Direction.SOUTH, 2);
        newDecay = getFlowDecay(pos);
        if (newDecay >= 0 && newDecay < decay) return newDecay;
        
        // 居中
        pos.move(Direction.NORTH).move(Direction.WEST);
        return -1;
    }

    /**
     * 检查前面位置是否可用的流体
     */
    private boolean hasFluidSource() {
        if (level == null) return false;
        
        BlockPos startPos = getFrontPos();
        BlockPos fluidSource = searchFluidSource(startPos);
        return fluidSource != null;
    }

    /**
     * 获取前方处流体类型
     */
    @SuppressWarnings("unused")
    private net.minecraft.world.level.material.Fluid getFrontFluid() {
        if (level == null) return Fluids.EMPTY;
        
        BlockPos startPos = getFrontPos();
        BlockPos fluidSource = searchFluidSource(startPos);
        
        if (fluidSource != null) {
            FluidState state = level.getFluidState(fluidSource);
            if (!state.isEmpty()) {
                return state.getType();
            }
        }
        
        return Fluids.EMPTY;
    }

    /**
     * 获取特定方向可访问的槽位
     */
    @Override
    protected int[] getSlotsForDirection(Direction side) {
        // 升级槽不可被自动化访问，与IC2原版泵一致
        int[] slots = new int[TOTAL_SLOTS - SLOT_UPGRADE_COUNT];
        slots[0] = SLOT_BATTERY;
        slots[1] = SLOT_EMPTY_CONTAINER;
        slots[2] = SLOT_OUTPUT;
        return slots;
    }


    @Override
    protected int getBatterySlot() {
        return SLOT_BATTERY;
    }

    /**
     * 检查指定槽位是否可以特定方向提取
     */
    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        // 只有输出槽可以提取
        return slot == SLOT_OUTPUT;
    }

    /**
     * 默认返回true，所有槽位可
     */
    @Override
    protected boolean canInsertItem(int slot, ItemStack stack, @Nullable Direction side) {
        // 输出槽位不允许插入
        if (slot == SLOT_OUTPUT) {
            return false;
        }

        // 电池槽：只允许电池类物品
        if (slot == SLOT_BATTERY) {
            return isBattery(stack);
        }

        // 空容器槽位：只允许空桶或空单元
        if (slot == SLOT_EMPTY_CONTAINER) {
            return isEmptyContainer(stack);
        }

        if (slot >= SLOT_UPGRADE_START && slot < SLOT_UPGRADE_END) {
            return false; // 升级槽不允许自动化访问，与IC2泵一致
        }

        return false;
    }

    /**
     * 检查是否有足够流体空间存放
     */
    protected boolean hasEnoughFluidSpace() {
        return fluidTank.getFluidAmount() + FLUID_PER_OPERATION <= fluidTank.getCapacity();
    }

    /**
     * 执行工作 - 参考IC2算法
     */
    private boolean extractFluid() {
        if (level == null) return false;

        // 获取流体源
        BlockPos startPos = getFrontPos();
        BlockPos fluidSource = searchFluidSource(startPos);
        
        if (fluidSource == null) {
            return false;
        }
        
        // 获取源流体类型
        FluidState state = level.getFluidState(fluidSource);
        if (state.isEmpty()) {
            return false;
        }
        
        net.minecraft.world.level.material.Fluid fluid = state.getType();

        // 填充到流体容器槽
        int filled = fluidTank.fill(new FluidStack(fluid, FLUID_PER_OPERATION), IFluidHandler.FluidAction.EXECUTE);

        if (filled >= FLUID_PER_OPERATION) {
            // 移除源体方块
            level.setBlock(fluidSource, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            return true;
        }

        return false;
    }

    /**
     * 处理空容器槽位填充流体容器
     */
    private void handleEmptyContainerSlot() {
        ItemStack emptyContainerStack = itemHandler.getStackInSlot(SLOT_EMPTY_CONTAINER);
        if (emptyContainerStack.isEmpty() || !isEmptyContainer(emptyContainerStack)) {
            return;
        }

        // 流体槽中有足够流体
        if (fluidTank.getFluidAmount() < 1000) {
            return;
        }

        FluidStack currentFluid = fluidTank.getFluid();
        if (currentFluid.isEmpty()) {
            return;
        }

        // 检查输出槽是否可以容纳
        ItemStack outputStack = itemHandler.getStackInSlot(SLOT_OUTPUT);

        // 检查输出容器类型与流体类型确定输出
        ItemStack filledContainer;
        boolean isBucketInput = isBucket(emptyContainerStack);

        if (isBucketInput) {
            // 桶类物品输出
            filledContainer = getFilledBucket(currentFluid.getFluid());
        } else {
            // 单元物品输出
            filledContainer = getFilledCell(currentFluid.getFluid(), emptyContainerStack);
        }

        if (filledContainer.isEmpty()) {
            return;
        }

        // 检查输出栈
        if (!outputStack.isEmpty()) {
            // 检查槽位已有相同类别已填充容器，且未达最大堆叠数
            if (!ItemStack.isSameItem(outputStack, filledContainer) ||
                !ItemStack.isSameItemSameComponents(outputStack, filledContainer) ||
                outputStack.getCount() >= outputStack.getMaxStackSize()) {
                return;
            }
        }

        // 排出流体
        FluidStack drained = fluidTank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() < 1000) {
            return;
        }

        // 消耗空容器
        emptyContainerStack.shrink(1);

        // 添加填充后容器到输出槽
        if (outputStack.isEmpty()) {
            itemHandler.setStackInSlot(SLOT_OUTPUT, filledContainer);
        } else {
            outputStack.grow(1);
        }

        setChanged();
    }

    /**
     * 根据流体类型获取填充桶
     */
    private ItemStack getFilledBucket(net.minecraft.world.level.material.Fluid fluid) {
        if (fluid == Fluids.WATER) {
            return new ItemStack(Items.WATER_BUCKET);
        } else if (fluid == Fluids.LAVA) {
            return new ItemStack(Items.LAVA_BUCKET);
        }
        // 其他流体暂不支持桶
        return ItemStack.EMPTY;
    }

    /**
     * 根据流体类型获取填充单元
     */
    private ItemStack getFilledCell(net.minecraft.world.level.material.Fluid fluid, ItemStack inputContainer) {
        Item staticCell = mio_icif_cells.getFilledCellForFluid(fluid);
        if (staticCell != null) {
            return new ItemStack(staticCell);
        }
        if (inputContainer.getItem() instanceof com.singularity_iteration.mio_icif.Items.Cell.mio_icif_dynamic_cell) {
            ItemStack singleEmpty = inputContainer.copyWithCount(1);
            var handlerOpt = net.neoforged.neoforge.fluids.FluidUtil.getFluidHandler(singleEmpty);
            if (handlerOpt.isPresent()) {
                var handler = handlerOpt.get();
                int filled = handler.fill(new FluidStack(fluid, 1000), IFluidHandler.FluidAction.EXECUTE);
                if (filled >= 1000) {
                    return handler.getContainer();
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected boolean canWork() {
        // 检查是否有足够能量
        if (!hasEnoughEnergy()) {
            return false;
        }

        // 检查前方位置是否有可用的流体
        if (!hasFluidSource()) {
            return false;
        }

        // 检查是否有足够流体空间存放
        if (!hasEnoughFluidSpace()) {
            return false;
        }

        return true;
    }

    @Override
    protected void doWork() {
        // 消耗能量
        if (!consumeEnergy()) {
            stopWork();
            return;
        }

        isWorking = true;

        // 工作进度完成执行工作周期
        if (progress >= maxProgress) {
            // 尝试流体提取
            if (extractFluid()) {
                finishWork();
                // 如果可以则继续工作
                if (canWork()) {
                    isWorking = true;
                }
            } else {
                stopWork();
            }
        }
    }

    /**
     * 每tick执行工作核心逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_pump_elc blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 调用父类tick方法处理工作逻辑和电池槽升级槽
        mio_icif_producer.tick(level, pos, state, blockEntity);

        // 处理空容器槽位填充流体容器
        blockEntity.handleEmptyContainerSlot();

        // 同步方块状态亮灭
        boolean isLit = state.getValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_pump_elc.LIT);
        if (blockEntity.isWorking() != isLit) {
            level.setBlock(pos, state.setValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_pump_elc.LIT, blockEntity.isWorking()), 3);
        }
    }

    /**
     * 获取流体处理器
     */
    public IFluidHandler getFluidHandler() {
        return fluidTank;
    }

    /**
     * 获取流体处理器能力
     */
    @Override
    public IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        return fluidTank;
    }

    /**
     * 检查是否可以接受来自侧的特定流体
     * @param fluid 流体类型
     * @return 是否可以接受
     */
    public boolean canAcceptFluid(net.minecraft.world.level.material.Fluid fluid) {
        // 如果流体槽为空气则可以接受任何流体
        if (fluidTank.isEmpty()) {
            return true;
        }

        // 流体槽中已存在流体则不接受不同类型的流体
        FluidStack currentFluid = fluidTank.getFluid();
        return currentFluid.getFluid() == fluid;
    }

    /**
     * 注册流体到泵的流体槽
     * @param fluid 流体类型
     * @param amount 流体数量，单位mb
     * @return 是否可以成功注册
     */
    public boolean injectFluid(net.minecraft.world.level.material.Fluid fluid, int amount) {
        // 检查是否可以接受此类型流体
        if (!canAcceptFluid(fluid)) {
            return false;
        }

        // 检查是否有足够空气槽
        int space = fluidTank.getCapacity() - fluidTank.getFluidAmount();
        if (space < amount) {
            return false;
        }

        // 注册流体
        int filled = fluidTank.fill(new FluidStack(fluid, amount), IFluidHandler.FluidAction.EXECUTE);
        if (filled >= amount) {
            setChanged();
            return true;
        }

        return false;
    }

    /**
     * 获取存储量上限，单位mb
     */
    public int getFluidAmount() {
        return fluidTank.getFluidAmount();
    }

    public ContainerData getContainerData() {
        return dataAccess;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Producer.PumpElcMenu(
            containerId, playerInventory, this);
    }

    /**
     * 获取最大流体容量，单位mb
     */
    public int getFluidCapacity() {
        return fluidTank.getCapacity();
    }

    /**
     * 获取流体类型
     */
    public FluidStack getFluid() {
        return fluidTank.getFluid();
    }

    /**
     * 获取流体填充百分比用于GUI显示
     */
    public int getFluidProgress() {
        if (fluidTank.getCapacity() <= 0) {
            return 0;
        }
        return (fluidTank.getFluidAmount() * 100) / fluidTank.getCapacity();
    }

    /**
     * 获取流体类型名称用于GUI显示
     */
    public String getFluidTypeName() {
        FluidStack fluid = fluidTank.getFluid();
        if (fluid.isEmpty()) {
            return "Empty";
        }
        return net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid.getFluid()).getPath();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // 保存流体数据
        tag.put("fluid", fluidTank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // 读取流体数据
        if (tag.contains("fluid")) {
            fluidTank.readFromNBT(registries, tag.getCompound("fluid"));
        }
    }
}