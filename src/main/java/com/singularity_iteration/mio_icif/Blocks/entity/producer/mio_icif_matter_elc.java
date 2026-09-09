package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Block;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
import com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal;
import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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
 * UU物质合成机方块实体类 - 参考IC2 Squeezer模式
 *
 * 槽位布局(对比IC2):
 * - 0: 放大器槽位 - (72, 40)
 * - 1: 输出槽位 - (125, 59)
 * - 2: 容器槽位：空单元输入输出 - (125, 23)
 * - 3-6: 4个升级槽 - (152, 8 + i * 18)
 *
 * 工作机机制(对比IC2):
 * - 满能量的机器产1mB UU物质
 * - 满能量用于一次，5M EU（实际3M EU）才能完成
 * - 流体槽容量8000 mB（参考原版）
 * - 额外的资源来自于电池升级：5M EU才能合成
 */
@SuppressWarnings("null")
public class mio_icif_matter_elc extends mio_icif_producer {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .extra(1)   // 放大器槽
        .output(1)  // 输出槽
        .input(1)   // 容器槽位：空单元输入/输出
        .upgrade(4) // 4个升级槽
        .build();

    // 槽位布局定义（对比IC2）
    public static final int SLOT_COUNT = 7;
    public static final int AMPLIFIER_SLOT = 0;      // 放大器槽位：接受废料/废料箱
    public static final int OUTPUT_SLOT = 1;         // 输出槽
    public static final int CONTAINER_SLOT = 2;      // 容器槽位：空单元输入/输出
    public static final int UPGRADE_SLOT_START = 3;  // 升级槽起始位
    public static final int UPGRADE_SLOT_COUNT = 4;  // 升级槽的数量

    // 默认配置（对比IC2原版）
    public static final long DEFAULT_CAPACITY = 1000000L; // 1M EU（推荐使用K=1M EU）
    public static final long DEFAULT_MAX_RECEIVE = 8192L; // EV级输入电压
    public static final long DEFAULT_MAX_EXTRACT = 0L;    // 不输出能量
    public static final int DEFAULT_WORK_TIME = 1;        // 不使用工作周期
    public static final long DEFAULT_ENERGY_PER_TICK = 0L; // 不自动消耗能量

    // UU物质液体槽容量（对比IC2原版: 8000 mB）
    public static final int UUMATTER_CAPACITY = 8000;

    // EU消费配置（对比IC2）
    public static final long EU_PER_MB = 1000000L;      // 每mB需要1M EU
    public static final int SCRAP_BONUS = 5000;         // 每个废料赔偿5000 EU能量倍率
    public static final int SCRAPBOX_BONUS = 45000;     // 每个废料赔偿补偿5000 EU能量倍率
    public static final int THORIUM_SCRAP_BONUS = 360000; // 每个钍废料额外提供360000 EU能量倍率对应8倍废料赔偿倍率

    // UU物质液体槽实例
    protected final FluidTank uuMatterTank;

    // 放大器增强器计算，用于计算能量消耗倍率
    protected int scrap;
    // 上次能量计算，用于计算能量来源
    protected double lastEnergy;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) energyStorage.getAmount();           // 当前能量
                case 1 -> (int) energyStorage.getCapacity();         // 最大容量值
                case 2 -> (int) energyStorage.getAmount();           // 当前能量与储备容纳
                case 3 -> (int) energyStorage.getCapacity();         // 最大能量与储备容纳
                case 4 -> uuMatterTank.getFluidAmount();             // UU物质液体数量
                case 5 -> uuMatterTank.getCapacity();                // UU物质液体容量
                case 6 -> scrap;                                      // 放大器增强器数量
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

    public mio_icif_matter_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.MATTER_ELC_ENTITY_TYPE.get());
    }

    public mio_icif_matter_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            DEFAULT_WORK_TIME,
            LAYOUT,
            DEFAULT_ENERGY_PER_TICK,
            CableTier.EV);

        // 创建UU物质液体槽，只接受UU物质流
        this.uuMatterTank = new FluidTank(UUMATTER_CAPACITY, fluidStack -> 
            fluidStack.getFluid() == mio_icif_fluids.UUMATTER.get());

        this.scrap = 0;
        this.lastEnergy = 0;
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.matter_elc");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case AMPLIFIER_SLOT -> isScrap(stack);
            case OUTPUT_SLOT -> false; // 输出槽位不允许自动化
            case CONTAINER_SLOT -> isEmptyCell(stack);
            default -> {
                // 升级槽位（槽位3-6处）
                if (slot >= UPGRADE_SLOT_START && slot < UPGRADE_SLOT_START + UPGRADE_SLOT_COUNT) {
                    yield getItemAPI().isUpgrade(stack);
                }
                yield false;
            }
        };
    }

    private boolean isScrap(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(mio_icif_normal.SCRAP.get()) || stack.is(mio_icif_normal.SCRAPBOX.get()) || stack.is(mio_icif_resources.THORIUM_SCRAP.get()));
    }

    private boolean isEmptyCell(ItemStack stack) {
        return mio_icif_cells.isEmptyCell(stack);
    }

    @Override
    protected int[] getSlotsForDirection(Direction side) {
        return switch (side) {
            case UP -> new int[]{AMPLIFIER_SLOT}; // 上方：访问放大器
            case DOWN -> new int[]{OUTPUT_SLOT};   // 下方：访问输出
            case NORTH, SOUTH, EAST, WEST -> new int[]{CONTAINER_SLOT, AMPLIFIER_SLOT};
        };
    }

    @Override
    protected int[] getInputSlots() {
        return new int[]{CONTAINER_SLOT};
    }

    @Override
    protected int[] getOutputSlots() {
        return new int[]{OUTPUT_SLOT};
    }

    @Override
    protected boolean canInsertItem(int slot, ItemStack stack, @Nullable Direction side) {
        return switch (slot) {
            case AMPLIFIER_SLOT -> isScrap(stack);
            case CONTAINER_SLOT -> isEmptyCell(stack);
            case OUTPUT_SLOT -> false;
            default -> {
                if (slot >= UPGRADE_SLOT_START && slot < UPGRADE_SLOT_START + UPGRADE_SLOT_COUNT) {
                    yield false; // 升级槽不允许自动化访问，与IC2原版保持一致
                }
                yield false;
            }
        };
    }

    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        return slot == OUTPUT_SLOT;
    }

    /**
     * 尝试消耗增强器执行能量
     * 对比IC2算法:
     * 1. 首先应用增强器减少bonus = min(scrap, energy - lastEnergy)
     * 2. 额度能量就用力输出而不是能量消耗
     * 3. 设置内部状态StateRunningScrap而不是StateRunning
     */
    private void processAmplifier() {
        // 应用增强器减少能量
        if (scrap > 0) {
            double bonus = Math.min(scrap, energyStorage.getAmount() - lastEnergy);
            if (bonus > 0) {
                // 这里计算scrap使用能量，机器为原型而不是升级槽改
                // 能量与scrap的关系是机器matter设计
                long currentEnergy = energyStorage.getAmount();
                long newEnergy = currentEnergy + (long)(5.0 * bonus);
                energyStorage.setEnergy(Math.min(newEnergy, energyStorage.getCapacity()));
                scrap -= (int)bonus;
            }
        }

        // 尝试从放大器槽位获取更多增强剂，使用三方计提方法
        if (scrap < 10000) {
            processAmplifierSlot();
        }
    }

    /**
     * 处理增强器槽位中的物品，使用三方计提系统对齐为输入
     */
    private void processAmplifierSlot() {
        ItemStack scrapStack = itemHandler.getStackInSlot(AMPLIFIER_SLOT);
        if (scrapStack.isEmpty()) return;

        // 检查对应废料种族
        if (scrapStack.is(mio_icif_normal.SCRAP.get())) {
            scrap += SCRAP_BONUS;
            scrapStack.shrink(1);
            setChanged();
        } else if (scrapStack.is(mio_icif_normal.SCRAPBOX.get())) {
            scrap += SCRAPBOX_BONUS;
            scrapStack.shrink(1);
            setChanged();
        } else if (scrapStack.is(mio_icif_resources.THORIUM_SCRAP.get())) {
            scrap += THORIUM_SCRAP_BONUS;
            scrapStack.shrink(1);
            setChanged();
        }
    }

    // UU物质每次产出1mB（对比IC2原版：1M EU = 1mB UU物质）
    public static final int UUMATTER_OUTPUT_AMOUNT = 1;

    /**
     * 尝试生成UU物质
     * 修改：能量满的时候自动0mB UU物质质量从输出槽1mB位置放置时，需要消耗一定来保证
     * - 不使用setEnergy方法
     * - 使用 fillInternal 填充输出不触发发射事件
     */
    private boolean attemptGeneration() {
        // 检查流体槽是否有足够空间
        if (uuMatterTank.getFluidAmount() + UUMATTER_OUTPUT_AMOUNT > uuMatterTank.getCapacity()) {
            return false;
        }
        // 检查能量是否已经满
        if (energyStorage.getAmount() < energyStorage.getCapacity()) {
            return false;
        }

        // 输出槽接收产出10mB UU物质
        uuMatterTank.fill(new FluidStack(mio_icif_fluids.UUMATTER.get(), UUMATTER_OUTPUT_AMOUNT), IFluidHandler.FluidAction.EXECUTE);
        apiUseEnergy(energyStorage.getCapacity(), false);
        setChanged();
        return true;
    }

    /**
     * 填充到空气容器中 - 对比IC2
     * 从输入流中取出
     * - 检查输出槽是否为空的空容器
     * - 检查流体槽中有足够流体量1000mB
     * - 检查输出槽是否可以接收
     * - 填充到输出槽
     */
    private boolean processContainer() {
        ItemStack inputStack = itemHandler.getStackInSlot(CONTAINER_SLOT);
        ItemStack outputStack = itemHandler.getStackInSlot(OUTPUT_SLOT);
        
        // 检查输入槽是否为空容器
        if (!isEmptyCell(inputStack)) return false;
        
        // 检查流体槽中有足够流体
        if (uuMatterTank.getFluidAmount() < 1000) return false;
        
        // 构建UU物质物品
        ItemStack uuMatterCell = mio_icif_cells.getFilledCellForFluidStack(mio_icif_fluids.UUMATTER.get());
        
        // 检查输出槽是否可以接收
        if (outputStack.isEmpty()) {
            // 输出槽为空可以直接放置
            itemHandler.setStackInSlot(OUTPUT_SLOT, uuMatterCell);
            inputStack.shrink(1);
            uuMatterTank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
            setChanged();
            return true;
        } else if (ItemStack.isSameItemSameComponents(outputStack, uuMatterCell) && 
                   outputStack.getCount() < outputStack.getMaxStackSize()) {
            // 输出槽已有相同物品且未满
            outputStack.grow(1);
            inputStack.shrink(1);
            uuMatterTank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
            setChanged();
            return true;
        }
        
        return false;
    }

    @Override
    protected boolean canWork() {
        // 检查是否可以生成UU物质或填充容器
        return energyStorage.getAmount() >= energyStorage.getCapacity() || 
               (uuMatterTank.getFluidAmount() >= 1000 && isEmptyCell(itemHandler.getStackInSlot(CONTAINER_SLOT)));
    }

    @Override
    protected void doWork() {
        isWorking = true;
        
        // 处理增强器
        processAmplifier();
        
        // 尝试生成UU物质
        if (energyStorage.getAmount() >= energyStorage.getCapacity()) {
            attemptGeneration();
        }
        
        // 处理容器填充
        processContainer();
        
        lastEnergy = energyStorage.getAmount();
    }

    @Override
    protected void stopWork() {
        isWorking = false;
    }

    public FluidTank getUuMatterTank() {
        return uuMatterTank;
    }

    public int getUuMatterAmount() {
        return uuMatterTank.getFluidAmount();
    }

    public int getUuMatterCapacity() {
        return UUMATTER_CAPACITY;
    }

    public int getScrap() {
        return scrap;
    }

    public ContainerData getContainerData() {
        return dataAccess;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Producer.MatterElcMenu(
            containerId, playerInventory, this);
    }

    /**
     * 获取能量百分比进度字符串用于GUI显示
     */
    public String getProgressAsString() {
        int p = (int)Math.min(100.0 * energyStorage.getAmount() / energyStorage.getCapacity(), 100.0);
        return p + "%";
    }

    @Override
    public IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        return uuMatterTank;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Scrap", scrap);
        tag.putDouble("LastEnergy", lastEnergy);
        
        CompoundTag fluidTag = new CompoundTag();
        uuMatterTank.writeToNBT(registries, fluidTag);
        tag.put("UuMatterTank", fluidTag);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        
        if (tag.contains("Scrap")) {
            scrap = tag.getInt("Scrap");
        }
        if (tag.contains("LastEnergy")) {
            lastEnergy = tag.getDouble("LastEnergy");
        }
        
        if (tag.contains("UuMatterTank")) {
            CompoundTag fluidTag = tag.getCompound("UuMatterTank");
            uuMatterTank.readFromNBT(registries, fluidTag);
        }
    }

    /**
     * 主tick执行逻辑 - 对比IC2
     * 执行顺序如下:
     * 1. 重新计算升级统计
     * 2. 调用父类tick方法处理充放电等逻辑
     * 3. 检查红石信号情况
     * 4. 处理增强器：应用bonus + 清除增强器计数器
     * 5. 尝试生成UU物质时能量满进行
     * 6. 执行容器填充
     * 7. 执行升级流体/升级液体弹射等
     * 8. 保存lastEnergy用于计算能量来源bonus用
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_matter_elc blockEntity) {
        if (level.isClientSide()) return;

        // 先升级计挺
        blockEntity.recalculateUpgradeStats();
        
        // 调用父类tick方法处理充放电等
        mio_icif_Energy_Block.tick(level, pos, state, blockEntity);

        // 检查红石信号和工作情况
        if (!blockEntity.canWorkRedstone() || blockEntity.energyStorage.getAmount() <= 0) {
            blockEntity.setState(0); // 停止工作
            blockEntity.stopWork();
            // 即使不工作也处理流体升级
            blockEntity.handleFluidUpgrades();
            return;
        }

        // 激活工作状态
        if (blockEntity.scrap > 0) {
            blockEntity.setState(2); // 增强器工作模式
        } else {
            blockEntity.setState(1); // 正常工作模式
        }
        blockEntity.isWorking = true;
        
        // 处理增强器
        blockEntity.processAmplifier();
        
        // 尝试生成UU物质当能量满时
        if (blockEntity.energyStorage.getAmount() >= blockEntity.energyStorage.getCapacity()) {
            blockEntity.attemptGeneration();
        }
        
        // 执行容器填充
        blockEntity.processContainer();
        
        // 执行流体升级/流体弹射等方式
        blockEntity.handleFluidUpgrades();
        
        // 保存lastEnergy用于计算能量来源bonus
        blockEntity.lastEnergy = blockEntity.energyStorage.getAmount();
    }

    // 状态管理变量
    private int state = 0;
    private int prevState = -1;

    private void setState(int newState) {
        this.state = newState;
        if (this.prevState != this.state) {
            this.prevState = this.state;
            setChanged();
        }
    }

    public int getState() {
        return state;
    }
}