package com.singularity_iteration.mio_icif.Blocks.entity;

import com.singularity_iteration.mio_icif.Blocks.entity.slot.ISlotValidator;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.MachineItemHandler;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotType;
import com.singularity_iteration.mio_icif.Items.Upgrade.MachineUpgradeStats;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.api.energy.IEnergyTileAccess;
import com.singularity_iteration.mio_icif.api.item.IItemAPI;
import com.singularity_iteration.mio_icif.api.machine.IMachineUpgradeStats;
import com.singularity_iteration.mio_icif.api.machine.ISlotLayout;
import com.singularity_iteration.mio_icif.api.machine.IWorkCompleteCallback;
import com.singularity_iteration.mio_icif.api.recipe.IRecipeAPI;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.mio_icif_sounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * 生产者机器基类
 * 所有生产用机器的基类，提供工作进度等基础功能
 * 电器类型，使用电力而非燃料
 * 实现 Container 和 WorldlyContainer 接口以支持漏斗交互
 * 使用 EU (Energy Unit) 能量系统
 * 
 * <p>配方查找和能量操作通过 API 层进行，子类可以覆盖受保护的方法
 * 来改变行为，Addon 可以安全地扩展机器行为。
 */
@SuppressWarnings("null")
public abstract class mio_icif_producer extends mio_icif_Energy_Block implements WorldlyContainer, ISlotValidator, com.singularity_iteration.mio_icif.api.machine.IProducerBlock {

    protected final SlotLayout slotLayout;
    protected MachineItemHandler itemHandler;

    protected int progress;
    protected int maxProgress;
    protected final int baseMaxProgress;

    protected final long baseCapacity;

    protected boolean isWorking;

    // 输入槽物品变化检测（用于重置进度）
    protected ItemStack[] lastInputStacks;

    public static final int WORK_SOUND_INTERVAL = 40;
    protected int workSoundTick;

    protected final long energyPerTick;
    protected final int slotCount;

    protected IMachineUpgradeStats upgradeStats = MachineUpgradeStats.empty();

    // ==================== API 方法层 ====================
    // 所有配方和能量操作通过这些受保护的方法进行，子类可以覆盖以改变行为

    /**
     * 获取配方 API 实例
     */
    protected IRecipeAPI getRecipeAPI() {
        return MioIcifAPI.instance().getRecipeAPI();
    }

    /**
     * 获取物品 API 实例
     */
    protected IItemAPI getItemAPI() {
        return MioIcifAPI.instance().getItemAPI();
    }

    /**
     * 获取能量 API 访问接口（通过父类）
     */
    @Override
    protected IEnergyTileAccess getEnergyAPI() {
        return super.getEnergyAPI();
    }

    /**
     * 通过 API 查找配方
     * 子类可以覆盖此方法以提供自定义配方查找逻辑
     */
    @SuppressWarnings("deprecation")
    protected <T extends net.minecraft.world.item.crafting.Recipe<?>> java.util.Optional<net.minecraft.world.item.crafting.RecipeHolder<T>> apiFindRecipe(
            net.minecraft.world.item.crafting.RecipeType<T> type, ItemStack input, Level level) {
        return getRecipeAPI().findRecipe(type, input, level);
    }

    /**
     * 通过 API 获取配方输出
     */
    protected ItemStack apiGetRecipeOutput(net.minecraft.world.item.crafting.RecipeHolder<?> recipe) {
        return getRecipeAPI().getRecipeOutput(recipe);
    }

    /**
     * 通过 API 获取配方处理时间
     */
    protected int apiGetRecipeProcessTime(net.minecraft.world.item.crafting.RecipeHolder<?> recipe) {
        return getRecipeAPI().getRecipeProcessTime(recipe);
    }

    /**
     * 通过 API 获取配方能耗
     */
    protected long apiGetRecipeEnergyCost(net.minecraft.world.item.crafting.RecipeHolder<?> recipe) {
        return getRecipeAPI().getRecipeEnergyCost(recipe);
    }

    /**
     * 通过 API 检查物品是否为电池
     * 子类可以覆盖此方法以支持自定义电池类型
     */
    protected boolean apiIsBattery(ItemStack stack) {
        return getItemAPI().isBattery(stack);
    }

    /**
     * 通过 API 为电池充电
     */
    protected long apiChargeBattery(ItemStack stack, long amount, boolean simulate) {
        return getItemAPI().chargeBattery(stack, amount, simulate);
    }

    /**
     * 通过 API 从电池放电
     */
    protected long apiDischargeBattery(ItemStack stack, long amount, boolean simulate) {
        return getItemAPI().dischargeBattery(stack, amount, simulate);
    }

    /**
     * 通过 API 获取电池容量
     */
    protected long apiGetBatteryCapacity(ItemStack stack) {
        return getItemAPI().getBatteryCapacity(stack);
    }

    /**
     * 通过 API 获取电池当前能量
     */
    protected long apiGetBatteryStored(ItemStack stack) {
        return getItemAPI().getBatteryStored(stack);
    }

    /**
     * 通过 API 获取电池充电速率
     */
    protected long apiGetChargeRate(ItemStack stack) {
        return getItemAPI().getChargeRate(stack);
    }

    /**
     * 通过 API 为电力护甲充电
     */
    protected long apiChargeElectricArmor(ItemStack stack, long amount, boolean simulate) {
        return getItemAPI().chargeElectricArmor(stack, amount, simulate);
    }

    /**
     * 通过 API 从电力护甲放电
     */
    protected long apiDischargeElectricArmor(ItemStack stack, long amount, boolean simulate) {
        return getItemAPI().dischargeElectricArmor(stack, amount, simulate);
    }

    /**
     * 通过 API 检查是否为电力护甲
     */
    protected boolean apiIsElectricArmor(ItemStack stack) {
        return getItemAPI().isElectricArmor(stack);
    }

    /**
     * 通过 API 检查是否为电动工具
     */
    protected boolean apiIsElectricTool(ItemStack stack) {
        return getItemAPI().isElectricTool(stack);
    }

    /**
     * 通过 API 消耗能量（内部做功）
     * 使用 apiUseEnergy 绕过 maxExtract 限制
     */
    protected boolean apiConsumeEnergy(long amount) {
        long consumed = apiUseEnergy(amount, false);
        return consumed >= amount;
    }

    /**
     * 通过 API 获取当前存储能量
     * 直接访问 energyStorage，避免通过 getEnergyAPI() 造成无限递归
     */
    @Override
    protected long apiGetStoredEnergy() {
        return energyStorage.getAmount();
    }

    /**
     * 通过 API 接收能量
     * 直接访问 energyStorage，避免通过 getEnergyAPI() 造成无限递归
     */
    @Override
    protected long apiReceiveEnergy(long amount, boolean simulate) {
        return energyStorage.receive(amount, simulate);
    }

    /**
     * 简化构造函数 - 供 NeoForge BlockEntityType.Builder.of() 使用
     * 
     * <p>此构造函数使用默认配置，适用于 GenericMachineBlockEntity 等需要通过
     * BlockEntityType 创建的机器。实际配置应在 onLoad() 时从 MachineRegistry 获取。
     */
    public mio_icif_producer(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        this(pos, state, type, 10000, 100, 100, 200, 
             SlotLayout.builder().input(1).output(1).build(), 10, CableTier.LV);
    }

    public mio_icif_producer(BlockPos pos, BlockState state, BlockEntityType<?> type,
                             long capacity, long maxReceive, long maxExtract,
                              int maxProgress, SlotLayout slotLayout, long energyPerTick, CableTier cableTier) {
        super(pos, state, type, capacity, maxReceive, maxExtract, cableTier);
        this.baseMaxProgress = maxProgress;
        this.maxProgress = maxProgress;
        this.baseCapacity = capacity;
        this.energyPerTick = energyPerTick;
        this.slotLayout = slotLayout;
        this.slotCount = slotLayout.getTotalSlots();
        this.progress = 0;
        this.isWorking = false;
        this.workSoundTick = 0;
        this.lastInputStacks = null;

        this.setAsConsumer();

        this.itemHandler = createItemHandler(slotLayout);
    }

    public mio_icif_producer(BlockPos pos, BlockState state, BlockEntityType<?> type,
                             long capacity, long maxReceive, long maxExtract,
                             int maxProgress, ISlotLayout slotLayout, long energyPerTick, ICableTier cableTier) {
        this(pos, state, type, capacity, maxReceive, maxExtract, maxProgress,
             SlotLayout.fromISlotLayout(slotLayout), energyPerTick, CableTier.fromICableTier(cableTier));
    }



    protected MachineItemHandler createItemHandler(SlotLayout layout) {
        MachineItemHandler handler = new MachineItemHandler(layout);
        handler.setValidator(this);
        return handler;
    }

    @Deprecated
    protected void initUpgradeSlots() {
    }

    @Deprecated
    protected void setUpgradeSlots(int start, int count) {
    }

    @Override
    public ISlotLayout getSlotLayout() {
        return slotLayout;
    }

    @Override
    public boolean isValidForSlot(int slot, ItemStack stack, SlotType type) {
        return isItemValidForSlot(slot, stack);
    }

    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    protected int getSlotLimit(int slot) {
        SlotType type = slotLayout.getInternalSlotType(slot);
        // 扫描槽、记忆槽、工具槽等限制只能放1个物品
        if (type == SlotType.SCANNER || type == SlotType.MEMORY || type == SlotType.TOOL) {
            return 1;
        }
        return 64;
    }

    /**
     * 获取指定方向可访问的槽位
     * 子类必须实现此方法以定义各方向可访问的槽位
     * @param side 方向
     * @return 可访问的槽位数组
     */
    protected abstract int[] getSlotsForDirection(Direction side);

    /**
     * 检查指定槽位是否可以从指定方向插入物品
     * 基于 SlotLayout 自动判断槽位类型
     */
    protected boolean canInsertItem(int slot, ItemStack stack, @Nullable Direction side) {
        SlotType type = slotLayout.getInternalSlotType(slot);
        if (type == null) return false;

        if (!itemHandler.isItemValid(slot, stack)) {
            return false;
        }

        ItemStack currentStack = itemHandler.getStackInSlot(slot);
        if (!currentStack.isEmpty()) {
            if (!ItemStack.isSameItem(currentStack, stack)) {
                return false;
            }
            if (currentStack.getCount() >= currentStack.getMaxStackSize()) {
                return false;
            }
        }

        return switch (type) {
            case INPUT -> !isBattery(stack);
            case BATTERY -> isBattery(stack);
            case UPGRADE -> false; // 升级槽不允许自动化插入（与IC2原版一致）
            case OUTPUT -> false;
            case FILTER -> isItemValidForSlot(slot, stack);
            default -> isItemValidForSlot(slot, stack);
        };
    }

    protected boolean isUpgradeSlot(int slot) {
        return slotLayout.isType(slot, SlotType.UPGRADE);
    }

    public static final long REDSTONE_ENERGY_VALUE = 800L;

    protected boolean isBattery(ItemStack stack) {
        return getItemAPI().isBattery(stack)
            || stack.getItem() == Items.REDSTONE;
    }

    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        SlotType type = slotLayout.getInternalSlotType(slot);
        return type == SlotType.OUTPUT;
    }

    protected int getBatterySlot() {
        return slotLayout.getStart(SlotType.BATTERY);
    }

    /**
     * 每tick更新逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_producer blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 刷新升级插件统计信息并应用相关效果?
        blockEntity.recalculateUpgradeStats();

        // 调用父类的能量传输逻辑
        mio_icif_Energy_Block.tick(level, pos, state, blockEntity);

        // 检测输入槽物品变化，如果变化则重置进度
        blockEntity.checkInputChanged();

        // 子类每 tick 钩子（例如 GenericMachineBlockEntity 的 tick 回调）
        blockEntity.onTick();

        // 红石信号反转升级：控制机器是否被红石信号阻止
        if (!blockEntity.canWorkRedstone()) {
            blockEntity.stopWork();
        } else {
            // 执行生产逻辑
            if (blockEntity.canWork()) {
                blockEntity.doWork();
            } else {
                // 检查是否需要重置进度（解决"一格通病"）
                if (blockEntity.shouldResetProgress()) {
                    blockEntity.progress = 0;
                }
                blockEntity.stopWork();
            }

            // 更新工作进度（在doWork之后，确保isWorking状态正确）
            blockEntity.updateProgress();
        }

        // 处理电池槽放电（仅从电池获取能量，不给电池充电）
        blockEntity.handleBatterySlot();

        // 处理自动化升级（弹出/抽取等）
        // 注意：即使机器因红石信号停止工作，自动化升级仍应继续运行
        blockEntity.handleAutomationUpgrades();

        // 工作音效：机器工作时周期性播放
        if (blockEntity.isWorking) {
            blockEntity.workSoundTick++;
            if (blockEntity.workSoundTick >= WORK_SOUND_INTERVAL) {
                blockEntity.workSoundTick = 0;
                level.playSound(null, pos, mio_icif_sounds.MACHINE_WORK.get(),
                    SoundSource.BLOCKS, 0.5F, 1.0F);
            }
        } else {
            blockEntity.workSoundTick = 0;
        }
    }

    /**
     * 刷新升级插件统计信息
     * 超频升级：每 tick 处理更多进度，消耗更多EU，电压等级不变
     */
    protected void recalculateUpgradeStats() {
        int upgradeStart = slotLayout.getStart(SlotType.UPGRADE);
        int upgradeCount = slotLayout.getCount(SlotType.UPGRADE);
        this.upgradeStats = MachineUpgradeStats.fromInventory(itemHandler, upgradeStart, upgradeCount);
        // 超频升级改为1tick 增加更多进度，而不是减少最大进度
        // 因此这里只需要把旧存档可能被改过的maxProgress 恢复成基础值
        if (this.maxProgress != this.baseMaxProgress) {
            this.maxProgress = this.baseMaxProgress;
            if (this.progress > this.maxProgress) {
                this.progress = this.maxProgress;
            }
        }
        // 应用能量储存升级对容量的影响（基于基础容量计算，避免重复叠加）
        long newCapacity = baseCapacity + upgradeStats.getEnergyCapacityBonus();
        IEnergyTileAccess api = getEnergyAPI();
        if (newCapacity != api.getMaxEnergy()) {
            api.setCapacity(newCapacity);
        }

        // 应用变压器升级对最大接收速率的影响
        long effectiveMaxReceive = getEffectiveMaxReceive();
        if (effectiveMaxReceive != api.getMaxReceive()) {
            energyStorage.setMaxReceive(effectiveMaxReceive);
        }
    }

    /**
     * 获取每tick 增加的工作进度（受超频升级影响）
     * 超频升级使机器在 1 tick 内处理多个进度，同时消耗更多EU，但电压等级不变
     */
    protected int getProgressPerTick() {
        return Math.max(1, (int) Math.ceil(1.0 / upgradeStats.getProcessTimeMultiplier()));
    }

    /**
     * 获取当前升级统计信息
     */
    @Override
    public IMachineUpgradeStats getUpgradeStats() {
        return upgradeStats;
    }

    /**
     * 获取升级插件槽位起始索引
     */
    public int getUpgradeSlotStart() {
        return slotLayout.getStart(SlotType.UPGRADE);
    }

    /**
     * 获取升级插件槽位数量
     */
    public int getUpgradeSlotCount() {
        return slotLayout.getCount(SlotType.UPGRADE);
    }

    /**
     * 检查机器在红石控制下是否可以工作
     * 默认：无红石信号时工作，有红石信号时停止
     * 红石信号反转升级：有红石信号时工作，无信号时停止
     */
    protected boolean canWorkRedstone() {
        if (level == null) {
            return true;
        }
        boolean powered = level.hasNeighborSignal(worldPosition);
        if (upgradeStats.isRedstoneInverted()) {
            return powered;
        }
        return !powered;
    }

    /**
     * 获取输入槽位索引数组，子类可覆盖
     */
    protected int[] getInputSlots() {
        return slotLayout.getSlotsOfType(SlotType.INPUT);
    }

    /**
     * 获取输出槽位索引数组，子类可覆盖
     */
    protected int[] getOutputSlots() {
        return slotLayout.getSlotsOfType(SlotType.OUTPUT);
    }

    /**
     * 处理自动化升级插件
     * 子类可以覆盖以添加更具体的流体/物品交互
     */
    protected void handleAutomationUpgrades() {
        if (level == null || level.isClientSide) {
            return;
        }
        // 确保升级统计是最新的（子类可能未调用父类 tick）
        recalculateUpgradeStats();
        if (upgradeStats.getEjectorCount() > 0) {
            ejectItems(upgradeStats.getEjectorCount());
        }
        if (upgradeStats.getPullingCount() > 0) {
            pullItems(upgradeStats.getPullingCount());
        }
        handleFluidUpgrades();
    }

    /**
     * 弹出升级：将输出槽物品推送到相邻容器
     * @param upgradeCount 弹出升级数量
     */
    protected void ejectItems(int upgradeCount) {
        int maxPerTick = Math.max(1, upgradeCount);
        List<Direction> dirs = upgradeStats.getEjectorDirections();
        Iterable<Direction> targetDirs = !dirs.isEmpty() ? dirs : Arrays.asList(Direction.values());
        for (int outputSlot : getOutputSlots()) {
            ItemStack stack = itemHandler.getStackInSlot(outputSlot);
            if (stack.isEmpty()) {
                continue;
            }
            for (Direction dir : targetDirs) {
                IItemHandler target = getAdjacentItemHandler(worldPosition.relative(dir), dir.getOpposite());
                if (target == null) {
                    continue;
                }
                int moveCount = Math.min(stack.getCount(), maxPerTick);
                ItemStack toMove = stack.copyWithCount(moveCount);
                ItemStack remaining = ItemHandlerHelper.insertItemStacked(target, toMove, false);
                int moved = moveCount - remaining.getCount();
                if (moved > 0) {
                    itemHandler.extractItem(outputSlot, moved, false);
                    stack = itemHandler.getStackInSlot(outputSlot);
                    if (stack.isEmpty()) {
                        break;
                    }
                }
            }
        }
    }

    /**
     * 抽取升级：从相邻容器拉取物品到输入槽
     * @param upgradeCount 抽取升级数量
     */
    protected void pullItems(int upgradeCount) {
        int maxPerTick = Math.max(1, upgradeCount);
        List<Direction> dirs = upgradeStats.getPullingDirections();
        Iterable<Direction> targetDirs = !dirs.isEmpty() ? dirs : Arrays.asList(Direction.values());
        for (int inputSlot : getInputSlots()) {
            ItemStack current = itemHandler.getStackInSlot(inputSlot);
            if (current.getCount() >= itemHandler.getSlotLimit(inputSlot)) {
                continue;
            }
            for (Direction dir : targetDirs) {
                IItemHandler source = getAdjacentItemHandler(worldPosition.relative(dir), dir.getOpposite());
                if (source == null) {
                    continue;
                }
                boolean pulled = false;
                for (int i = 0; i < source.getSlots(); i++) {
                    ItemStack sourceStack = source.getStackInSlot(i);
                    if (sourceStack.isEmpty()) {
                        continue;
                    }
                    if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, sourceStack)) {
                        continue;
                    }
                    if (!isItemValidForSlot(inputSlot, sourceStack)) {
                        continue;
                    }
                    int moveCount = Math.min(sourceStack.getCount(), maxPerTick);
                    ItemStack simulatedExtract = source.extractItem(i, moveCount, true);
                    if (simulatedExtract.isEmpty()) {
                        continue;
                    }
                    ItemStack remainder = itemHandler.insertItem(inputSlot, simulatedExtract, true);
                    int canMove = simulatedExtract.getCount() - remainder.getCount();
                    if (canMove <= 0) {
                        continue;
                    }
                    ItemStack extracted = source.extractItem(i, canMove, false);
                    if (!extracted.isEmpty()) {
                        itemHandler.insertItem(inputSlot, extracted, false);
                        pulled = true;
                        break;
                    }
                }
                if (pulled) {
                    break;
                }
            }
        }
    }

    /**
     * 获取相邻位置的物品处理器
     */
    @Nullable
    protected IItemHandler getAdjacentItemHandler(BlockPos pos, @Nullable Direction side) {
        if (level == null) {
            return null;
        }
        BlockEntity target = level.getBlockEntity(pos);
        if (target == null) {
            return null;
        }
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
    }

    /**
     * 获取相邻位置的流体处理器
     */
    @Nullable
    protected IFluidHandler getAdjacentFluidHandler(BlockPos pos, @Nullable Direction side) {
        if (level == null) {
            return null;
        }
        BlockEntity target = level.getBlockEntity(pos);
        if (target == null) {
            return null;
        }
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
    }

    /**
     * 处理流体自动化升级
     * 子类如有多个流体槽或特殊输出方向可覆盖
     */
    protected void handleFluidUpgrades() {
        if (level == null || level.isClientSide) {
            return;
        }
        IFluidHandler own = getFluidHandlerCapability(null);
        if (own == null || own.getTanks() == 0) {
            return;
        }

        if (upgradeStats.getFluidEjectorCount() > 0) {
            ejectFluids(own, upgradeStats.getFluidEjectorCount());
        }
        if (upgradeStats.getFluidPullingCount() > 0) {
            pullFluids(own, upgradeStats.getFluidPullingCount());
        }
    }

    /**
     * 流体弹出升级：将输出槽流体推送到相邻储罐
     * 默认把最后一个tank 当作输出 tank
     * @param own 本机器的流体处理器
     * @param upgradeCount 流体弹出升级数量
     */
    protected void ejectFluids(IFluidHandler own, int upgradeCount) {
        int maxPerTick = Math.max(1, upgradeCount * 1000);
        List<Direction> dirs = upgradeStats.getFluidEjectorDirections();
        Iterable<Direction> targetDirs = !dirs.isEmpty() ? dirs : Arrays.asList(Direction.values());
        for (int tank = own.getTanks() - 1; tank >= 0; tank--) {
            FluidStack fluid = own.getFluidInTank(tank);
            if (fluid.isEmpty()) {
                continue;
            }
            FluidStack toMove = fluid.copyWithAmount(Math.min(fluid.getAmount(), maxPerTick));
            if (toMove.isEmpty()) {
                continue;
            }
            for (Direction dir : targetDirs) {
                IFluidHandler target = getAdjacentFluidHandler(worldPosition.relative(dir), dir.getOpposite());
                if (target == null) {
                    continue;
                }
                int filled = target.fill(toMove, IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    own.drain(fluid.copyWithAmount(filled), IFluidHandler.FluidAction.EXECUTE);
                    toMove.setAmount(toMove.getAmount() - filled);
                    if (toMove.isEmpty()) {
                        break;
                    }
                }
            }
        }
    }

    /**
     * 流体抽入升级：从相邻储罐抽取流体到输入tank
     * 默认把第一个tank 当作输入 tank
     * @param own 本机器的流体处理器
     * @param upgradeCount 流体抽入升级数量
     */
    protected void pullFluids(IFluidHandler own, int upgradeCount) {
        int maxPerTick = Math.max(1, upgradeCount * 1000);
        List<Direction> dirs = upgradeStats.getFluidPullingDirections();
        Iterable<Direction> targetDirs = !dirs.isEmpty() ? dirs : Arrays.asList(Direction.values());
        for (int tank = 0; tank < own.getTanks(); tank++) {
            int capacity = own.getTankCapacity(tank);
            FluidStack current = own.getFluidInTank(tank);
            if (current.getAmount() >= capacity) {
                continue;
            }
            boolean inputTank = tank == 0 || current.isEmpty();
            if (!inputTank) {
                continue;
            }
            for (Direction dir : targetDirs) {
                IFluidHandler source = getAdjacentFluidHandler(worldPosition.relative(dir), dir.getOpposite());
                if (source == null) {
                    continue;
                }
                int space = capacity - current.getAmount();
                int maxDrain = Math.min(space, maxPerTick);
                FluidStack available = source.drain(maxDrain, IFluidHandler.FluidAction.SIMULATE);
                if (available.isEmpty()) {
                    continue;
                }
                if (!current.isEmpty() && !FluidStack.isSameFluid(available, current)) {
                    continue;
                }
                if (!own.isFluidValid(tank, available)) {
                    continue;
                }
                int filled = own.fill(available, IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    source.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                    break;
                }
            }
        }
    }

    /**
     * 子类返回本机器的流体处理器，若不支持流体则返回null
     */
    @Nullable
    protected IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        return null;
    }

    /**
     * 更新工作进度
     * 超频升级会提高每 tick 增加的进度值
     */
    protected void updateProgress() {
        if (isWorking) {
            progress = Math.min(maxProgress, progress + getProgressPerTick());
        }
        // 进度不回退，保持当前进度直到完成
    }

    /**
     * 检查机器是否可以工作
     * @return 是否可以工作
     */
    protected abstract boolean canWork();

    /**
     * 检查是否有有效的配方（用于判断进度是否应该重置）
     * 子类应该重写此方法来判断输入物品是否有匹配的配方
     * 默认返回true，表示不重置进度（保守策略）
     * @return 是否有有效配方
     */
    protected boolean hasValidRecipe() {
        return true;
    }

    /**
     * 执行生产工作
     */
    protected abstract void doWork();

    /**
     * 停止工作
     */
    protected void stopWork() {
        isWorking = false;
    }

    /**
     * 检测输入槽物品是否发生变化
     * 只在物品类型/NBT变化时重置工作进度，数量变化不重置
     * 这样自动化添加相同物品时不会打断工作进度
     */
    protected void checkInputChanged() {
        int[] inputSlots = getInputSlots();
        if (inputSlots.length == 0) return;

        // 初始化 lastInputStacks
        if (lastInputStacks == null || lastInputStacks.length != inputSlots.length) {
            lastInputStacks = new ItemStack[inputSlots.length];
            for (int i = 0; i < inputSlots.length; i++) {
                lastInputStacks[i] = itemHandler.getStackInSlot(inputSlots[i]).copy();
            }
            return;
        }

        // 检查每个输入槽是否发生变化
        boolean changed = false;
        for (int i = 0; i < inputSlots.length; i++) {
            ItemStack current = itemHandler.getStackInSlot(inputSlots[i]);
            ItemStack last = lastInputStacks[i];

            // 只检测物品类型/NBT变化，不检测数量变化
            // 这样自动化添加相同物品时不会重置进度
            if (!ItemStack.isSameItemSameComponents(current, last)) {
                changed = true;
                break;
            }
        }

        if (changed) {
            // 输入物品发生变化，重置进度
            progress = 0;
            isWorking = false;
            // 鏇存柊缂撳瓨
            for (int i = 0; i < inputSlots.length; i++) {
                lastInputStacks[i] = itemHandler.getStackInSlot(inputSlots[i]).copy();
            }
        }
    }

    /**
     * 检查是否应该重置进度（输入槽物品变化时）
     * 默认实现：只在输入槽为空或配方不匹配时重置，断电时保持进度
     * 子类可以覆盖此方法来自定义重置逻辑
     * @return 是否应该重置进度
     */
    protected boolean shouldResetProgress() {
        int[] inputSlots = getInputSlots();
        if (inputSlots.length == 0) return false;
        ItemStack input = itemHandler.getStackInSlot(inputSlots[0]);
        if (input.isEmpty()) {
            return true;
        }
        return !hasValidRecipe();
    }

    /**
     * 完成生产
     */
    protected void finishWork() {
        progress = 0;
        isWorking = false;
    }

    /**
     * 每 tick 子类钩子（默认无操作）
     */
    protected void onTick() {
        // hook for subclasses
    }

    /**
     * 强制开始工作（供 API 调用）
     */
    public void forceStartWork() {
        this.isWorking = true;
        this.progress = Math.min(this.progress + 1, this.maxProgress);
    }

    /**
     * 强制停止工作（供 API 调用）
     */
    public void forceStopWork() {
        this.isWorking = false;
        this.progress = 0;
    }

    @Override
    public void setLit(boolean lit) {
        if (getLevel() != null && !getLevel().isClientSide) {
            BlockState state = getBlockState();
            for (var property : state.getProperties()) {
                if (property instanceof net.minecraft.world.level.block.state.properties.BooleanProperty boolProp
                    && "lit".equals(boolProp.getName())) {
                    if (state.getValue(boolProp) != lit) {
                        getLevel().setBlock(getBlockPos(), state.setValue(boolProp, lit), 3);
                    }
                    return;
                }
            }
        }
    }

    @Override
    public void serverTick() {
        if (getLevel() == null || getLevel().isClientSide) return;
        tick(getLevel(), getBlockPos(), getBlockState(), this);
    }

    /**
     * 获取总处理物品数量
     * 子类应重写此方法以返回实际的总处理量
     */
    public long getTotalProcessed() {
        return 0;
    }

    /**
     * 处理电池槽的放电逻辑（仅从电池提取能量给机器，不给电池充电）
     * 生产机器只能消耗电池能量，不能给电池充电
     * 
     * <p>通过 IItemAPI 进行电池操作，Addon 可以注册自定义电池类型。
     */
    protected void handleBatterySlot() {
        int batterySlot = getBatterySlot();
        if (batterySlot < 0) {
            return;
        }

        ItemStack batteryStack = itemHandler.getStackInSlot(batterySlot);
        if (batteryStack.isEmpty()) {
            return;
        }

        if (apiGetStoredEnergy() >= getEffectiveCapacity()) {
            return;
        }

        long energyNeeded = getEffectiveCapacity() - apiGetStoredEnergy();
        long maxTransfer = Math.min(energyNeeded, getEffectiveMaxReceive());

        if (batteryStack.getItem() == Items.REDSTONE) {
            // 对齐IC2：有任何空间即消耗红石，不要求满800EU空缺
            long energyToAdd = Math.min(REDSTONE_ENERGY_VALUE, maxTransfer);
            if (energyToAdd > 0) {
                batteryStack.shrink(1);
                apiReceiveEnergy(energyToAdd, false);
                setChanged();
            }
            return;
        }

        // 通过 API 检查是否为电池并进行放电
        if (apiIsBattery(batteryStack)) {
            long batteryEnergy = apiGetBatteryStored(batteryStack);
            if (batteryEnergy > 0) {
                long batteryChargeRate = apiGetChargeRate(batteryStack);
                long energyToExtract = Math.min(batteryEnergy, Math.min(batteryChargeRate, maxTransfer));
                long extractedEnergy = apiDischargeBattery(batteryStack, energyToExtract, false);
                if (extractedEnergy > 0) {
                    apiReceiveEnergy(extractedEnergy, false);
                    setChanged();
                }
            }
        }
    }

    /**
     * 获取物品处理器
     *
     * @return 物品处理器
     */
    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    /**
     * 获取指定方向的物品处理器（带方向限制）
     * 用于 capability 系统，限制提取行为
     * @param side 方向
     * @return 带方向限制的 IItemHandler
     */
    public IItemHandler getItemHandlerCapability(@Nullable Direction side) {
        return new SidedItemHandler(side);
    }

    /**
     * 获取当前工作进度
     * @return 工作进度
     */
    public int getProgress() {
        return progress;
    }

    /**
     * 获取最大工作进度
     * @return 最大工作进度
     */
    public int getMaxProgress() {
        return maxProgress;
    }

    /**
     * 获取基础最大进度（不受升级影响）
     * @return 基础最大进度
     */
    public int getBaseMaxProgress() {
        return baseMaxProgress;
    }

    /**
     * 检查机器是否正在工作
     * @return 是否正在工作
     */
    public boolean isWorking() {
        return isWorking;
    }

    /**
     * 获取每次工作消耗的能量
     * @return 能量消耗
     */
    public long getEnergyPerTick() {
        return energyPerTick;
    }

    /**
     * 获取工作进度百分比（用于GUI显示）
     * @return 0-100之间的值
     */
    public int getWorkProgress() {
        if (maxProgress <= 0) {
            return 0;
        }
        return (progress * 100) / maxProgress;
    }

    /**
     * 获取实际每tick 能耗（含超频升级影响）
     */
    public long getEffectiveEnergyPerTick() {
        return upgradeStats.getEnergyPerTick(energyPerTick);
    }

    /**
     * 检查是否有足够能量工作
     * @return 是否有足够能量
     */
    protected boolean hasEnoughEnergy() {
        return apiGetStoredEnergy() >= getEffectiveEnergyPerTick();
    }

    /**
     * 消耗能量
     * @return 是否成功消耗
     */
    protected boolean consumeEnergy() {
        long effectiveEnergy = getEffectiveEnergyPerTick();
        long used = getEnergyAPI().useEnergy(effectiveEnergy, false);
        return used >= effectiveEnergy;
    }

    @Override
    public ICableTier getEffectiveCableTier() {
        ICableTier baseTier = (ICableTier) energyStorage.getCableTier();
        return upgradeStats.getEffectiveCableTier(baseTier);
    }

    @Override
    public long getEffectiveCapacity() {
        return baseCapacity + upgradeStats.getEnergyCapacityBonus();
    }

    @Override
    public long getEffectiveMaxReceive() {
        long base = energyStorage.getMaxReceive();
        // 变压器升级提升最大接收速率到有效等级的电压等级
        return Math.max(base, getEffectiveCableTier().getPowerRating());
    }

    /**
     * 获取本tick需求的能量量
 * 对齐 IC2 原版行为：返回内部冲区的全部剩余空间(capacity - storage)
     * IC2 原版机器的容纳= energyPerTick × operationLength，因此需求量自然受限于合理范围
 * 例如：电容纳=100 EU，打粉机容量600 EU，不会出现需求过大的问题
     */
    @Override
    public double getDemandedEnergy() {
        if (isPowerSource) return 0.0D;
        long spaceAvailable = getEffectiveCapacity() - apiGetStoredEnergy();
        if (spaceAvailable <= 0) return 0.0D;
        return spaceAvailable;
    }

    // ==================== Container 接口实现 ====================

    @Override
    public int getContainerSize() {
        return itemHandler.getSlots();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return itemHandler.getStackInSlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return itemHandler.extractItem(slot, amount, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = itemHandler.getStackInSlot(slot);
        itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        itemHandler.setStackInSlot(slot, stack);
    }

    @Override
    public void setChanged() {
        super.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(
            this.worldPosition.getX() + 0.5,
            this.worldPosition.getY() + 0.5,
            this.worldPosition.getZ() + 0.5
        ) <= 64.0;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    // ==================== WorldlyContainer 接口实现 ====================

    @Override
    public int[] getSlotsForFace(Direction side) {
        return getSlotsForDirection(side);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canInsertItem(slot, stack, side);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canExtractItem(slot, side);
    }

    // ==================== SidedItemHandler 内部类 ====================

    /**
     * 带方向限制的 IItemHandler 包装类
     * 用于 capability 系统，限制物品的插入和提取行为
     */
    protected class SidedItemHandler implements IItemHandler {
        protected final Direction side;

        public SidedItemHandler(@Nullable Direction side) {
            this.side = (side != null) ? side.getOpposite() : null;
        }

        @Override
        public int getSlots() {
            return itemHandler.getSlots();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return itemHandler.getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            // 检查是否可以从指定方向插入
            if (!canInsertItem(slot, stack, side)) {
                return stack;
            }
            return itemHandler.insertItem(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            // 检查是否可以从指定方向提取
            if (!canExtractItem(slot, side)) {
                return ItemStack.EMPTY;
            }
            return itemHandler.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemHandler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return mio_icif_producer.this.isItemValidForSlot(slot, stack);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("progress", progress);
        tag.putBoolean("is_working", isWorking);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
            // 自动迁移旧存档的槽位数量（例如冷凝机从3/7 槽扩容到 8 槽）
            if (itemHandler.getSlots() != slotCount) {
                MachineItemHandler resized = createItemHandler(slotLayout);
                int copyCount = Math.min(itemHandler.getSlots(), slotCount);
                for (int i = 0; i < copyCount; i++) {
                    resized.setStackInSlot(i, itemHandler.getStackInSlot(i));
                }
                itemHandler = resized;
            }
        }
        if (tag.contains("progress")) {
            progress = tag.getInt("progress");
        }
        if (tag.contains("is_working")) {
            isWorking = tag.getBoolean("is_working");
        }
 // 重置输入槽存，下次tick会重新初始化
        lastInputStacks = null;

        recalculateUpgradeStats();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        // 同步物品栏到客户端
        tag.put("inventory", itemHandler.serializeNBT(registries));
        // 同步工作进度
        tag.putInt("progress", progress);
        // 同步工作状态
        tag.putBoolean("is_working", isWorking);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        // 从服务端同步物品栈
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        // 同步工作进度
        if (tag.contains("progress")) {
            progress = tag.getInt("progress");
        }
        // 同步工作状态
        if (tag.contains("is_working")) {
            isWorking = tag.getBoolean("is_working");
        }
 // 重置输入槽存，下次tick会重新初始化
        lastInputStacks = null;
    }

    // ==================== IProducerBlock 接口实现 ====================

    @Override
    public java.util.List<ItemStack> getUpgrades() {
        java.util.List<ItemStack> upgrades = new java.util.ArrayList<>();
        int start = getUpgradeSlotStart();
        int count = getUpgradeSlotCount();
        for (int i = 0; i < count; i++) {
            ItemStack stack = itemHandler.getStackInSlot(start + i);
            if (!stack.isEmpty()) {
                upgrades.add(stack);
            }
        }
        return upgrades;
    }

    @Override
    public IWorkCompleteCallback getWorkCompleteCallback() {
        return null; // 基类没有回调，子类可以覆盖
    }
}