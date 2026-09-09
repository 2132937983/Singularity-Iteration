package com.singularity_iteration.mio_icif.Blocks.entity;

import com.singularity_iteration.mio_icif.Blocks.entity.slot.MachineItemHandler;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.energy.grid.IEnergyAcceptor;
import com.singularity_iteration.mio_icif.energy.grid.IEnergyEmitter;
import com.singularity_iteration.mio_icif.api.item.IItemAPI;
import com.singularity_iteration.mio_icif.energy.CustomEUEnergyStorage;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.energy.grid.EnergyNetGlobal;
import com.singularity_iteration.mio_icif.energy.grid.FECompatTile;
import com.singularity_iteration.mio_icif.energy.grid.IEnergyTile;
import com.singularity_iteration.mio_icif.integration.mi.EnergyBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 能量容器方块实体基类
 * 继承自 mio_icif_Energy_Block，作为储电器方块实体的基类
 * 提供更大的能量容量和更高的能量传输速率
 * 使用 EU (Energy Unit) 能量系统
 * 
 * <p>充放电操作通过 IItemAPI 进行，Addon 可以注册自定义电池和护甲类型。
 * 
 * 红石模式说明（参考IC2）：
 * 0 - 不发出红石信号
 * 1 - 能量接近满时发出信号 (能量 >= 容量 - 输出*20)
 * 2 - 能量在中间范围时发出信号 (输出 < 能量 < 容量-输出)
 * 3 - 能量未满时发出信号 (能量 < 容量-输出)
 * 4 - 能量接近空时发出信号 (能量 < 输出)
 * 5 - 接收到红石信号时停止输出能量
 * 6 - 接收到红石信号时停止输出，或能量满时继续输出
 * 7 - 接收到红石信号时才输出能量
 */
@SuppressWarnings("null")
public class mio_icif_Energy_Container extends mio_icif_Energy_Block implements Container, com.singularity_iteration.mio_icif.api.machine.IEnergyContainerBlock {

    // ==================== API 方法层 ====================
    // 充放电操作通过 IItemAPI 进行，子类可以覆盖以支持自定义电池/护甲类型

    /**
     * 获取物品 API 实例
     */
    protected IItemAPI getItemAPI() {
        return MioIcifAPI.instance().getItemAPI();
    }

    /**
     * 通过 API 检查物品是否为电池
     */
    protected boolean apiIsBattery(ItemStack stack) {
        return getItemAPI().isBattery(stack);
    }

    /**
     * 通过 API 检查物品是否为电力护甲
     */
    protected boolean apiIsElectricArmor(ItemStack stack) {
        return getItemAPI().isElectricArmor(stack);
    }

    /**
     * 通过 API 检查物品是否为电动工具
     */
    protected boolean apiIsElectricTool(ItemStack stack) {
        return getItemAPI().isElectricTool(stack);
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
     * 通过 API 为电力护甲充电
     */
    protected long apiChargeElectricArmor(ItemStack stack, long amount, boolean simulate) {
        return getItemAPI().chargeElectricArmor(stack, amount, simulate);
    }

    /**
     * 通过 API 检查电池是否已充满
     */
    protected boolean apiIsBatteryFull(ItemStack stack) {
        return getItemAPI().isBatteryFull(stack);
    }

    /**
     * 通过 API 检查电力护甲是否已充满
     */
    protected boolean apiIsArmorFull(ItemStack stack) {
        return getItemAPI().getElectricArmorStored(stack) >= getItemAPI().getElectricArmorMaxEnergy(stack);
    }

    /**
     * 通过 API 获取电力护甲最大能量
     */
    protected long apiGetArmorMaxEnergy(ItemStack stack) {
        return getItemAPI().getElectricArmorMaxEnergy(stack);
    }

    /**
     * 通过 API 获取电力护甲当前能量
     */
    protected long apiGetArmorStored(ItemStack stack) {
        return getItemAPI().getElectricArmorStored(stack);
    }

    // 槽位布局：充电槽 + 放电槽 + 4个额外槽
    protected static final SlotLayout LAYOUT = SlotLayout.builder()
        .battery()
        .battery()
        .extra(4)
        .build();

    // 物品栏处理器
    protected MachineItemHandler itemHandler;

    // 槽位索引（按 builder 顺序：BATTERY=0, BATTERY=1, EXTRA=2-5）
    protected static final int CHARGE_SLOT = 0;
    protected static final int BATTERY_SLOT = 1;
    
    // 红石模式（0-7）
    protected byte redstoneMode = 0;
    public static final byte REDSTONE_MODES = 8;
    
    protected boolean hasRedstoneInput = false;

    private static final int FE_COMPAT_SCAN_INTERVAL = 20;
    private static final int REDSTONE_CHECK_INTERVAL = 1;
    private int tickCounter = 0;
    private boolean lastRedstoneOutput = false;

    /** 相邻FE机器的电网代理，key为储能方块指向邻居的方向 */
    private final Map<Direction, FECompatTile> feCompatTiles = new HashMap<>();

    /**
     * 构造函数
     * @param pos 方块位置
     * @param state 方块状态
     * @param type 方块实体类型
     * @param capacity 能量容量（EU）
     * @param maxReceive 最大输入速率（EU/tick）
     * @param maxExtract 最大输出速率（EU/tick）
     * @param cableTier 电缆等级
     */
    public mio_icif_Energy_Container(BlockPos pos, BlockState state, BlockEntityType<?> type,
                                     long capacity, long maxReceive, long maxExtract, CableTier cableTier) {
        super(pos, state, type, capacity, maxReceive, maxExtract, cableTier);
        this.itemHandler = createItemHandler();
    }

    /**
     * 简化构造函数（使用默认参数）
     */
    public mio_icif_Energy_Container(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type, 100000, 1000, 1000, CableTier.MV); // 默认100000 EU 容量，1000 EU/t 输入/输出，MV等级
        this.itemHandler = createItemHandler();
    }

    protected MachineItemHandler createItemHandler() {
        return new MachineItemHandler(LAYOUT) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
    }
    
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.energy_container");
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        // 创建 BatBox 的 GUI 菜单，传入this 以便 Menu 可以访问能量数据
        return new com.singularity_iteration.mio_icif.Menu.Storage.BatBoxMenu(containerId, playerInventory, this);
    }
    
    /**
     * 每tick 更新逻辑
     * 能量输出由IC2 电网系统通过 IEnergySource 接口自动处理
     * 不再需要手动distributeEnergy()
     * 额外通过 EnergyBridge 向相邻的非IEnergyTile 能量存储（如 MI 机器）推送能量
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_Energy_Container blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        mio_icif_Energy_Block.tick(level, pos, state, blockEntity);

        blockEntity.tickCounter++;

        if ((blockEntity.tickCounter % REDSTONE_CHECK_INTERVAL) == 0) {
            blockEntity.updateRedstoneInput(level, pos);
            blockEntity.updateEnergyOutput();
            blockEntity.checkRedstoneOutputChange();
        }

        blockEntity.processBattery();

        blockEntity.chargeItems();

        if (blockEntity.shouldEmitEnergy()) {
            blockEntity.pushEnergyToCompatSinks();
        }

        if ((blockEntity.tickCounter % FE_COMPAT_SCAN_INTERVAL) == 0) {
            blockEntity.updateFECompatTiles(level, pos);
        }
    }
    
    /**
     * 检查红石信号输出状态是否改变，如果改变则通知邻居
     */
    protected void checkRedstoneOutputChange() {
        boolean currentOutput = shouldEmitRedstone();
        if (currentOutput != this.lastRedstoneOutput) {
            this.lastRedstoneOutput = currentOutput;
            notifyNeighborsRedstoneChange();
        }
    }
    
    /**
     * 检测红石信号输入
     */
    protected void updateRedstoneInput(Level level, BlockPos pos) {
        this.hasRedstoneInput = level.hasNeighborSignal(pos);
    }
    
    /**
     * 根据红石模式控制能量输出
     */
    protected void updateEnergyOutput() {
        // 红石模式 5: 接收到红石信号时停止输出
        // 红石模式 6: 接收到红石信号时停止输出，或能量满时继续输出
        // 红石模式 7: 接收到红石信号时才输出能量
        boolean shouldEmit = shouldEmitEnergy();
        this.energyStorage.setOutputEnabled(shouldEmit);
    }
    
    /**
     * 检查是否应该输出能量（根据红石模式）
     */
    public boolean shouldEmitEnergy() {
        switch (this.redstoneMode) {
            case 5: // 接收到红石信号时停止输出
                return !this.hasRedstoneInput;
            case 6: // 接收到红石信号时停止输出，或严格满电时继续输出
                if (this.hasRedstoneInput) {
                    return this.energyStorage.getAmount() >= this.energyStorage.getCapacity();
                }
                return true;
            case 7: // 接收到红石信号时才输出能量
                return this.hasRedstoneInput;
            default:
                return true;
        }
    }
    
    /**
     * 检查是否应该发出红石信号（根据红石模式）
     */
    public boolean shouldEmitRedstone() {
        switch (this.redstoneMode) {
            case 1: // 严格满电时发出信号 (100%)
                return this.energyStorage.getAmount() >= this.energyStorage.getCapacity();
            case 2: // 能量在中间范围时发出信号
                return this.energyStorage.getAmount() > this.energyStorage.getMaxExtract() && 
                       this.energyStorage.getAmount() < this.energyStorage.getCapacity() - this.energyStorage.getMaxExtract();
            case 3: // 能量未满时发出信号
                return this.energyStorage.getAmount() < this.energyStorage.getCapacity();
            case 4: // 严格空电时发出信号 (0%)
                return this.energyStorage.getAmount() <= 0;
            default:
                return false;
        }
    }
    
    /**
     * 获取当前红石信号强度（0-15）
     */
    public int getRedstoneSignalStrength() {
        return shouldEmitRedstone() ? 15 : 0;
    }
    
    /**
     * 获取红石模式
     */
    public byte getRedstoneMode() {
        return this.redstoneMode;
    }
    
    /**
     * 是否有红石输入信号
     */
    public boolean hasRedstoneInput() {
        return this.hasRedstoneInput;
    }
    
    /**
     * 设置红石模式
     */
    public void setRedstoneMode(byte mode) {
        if (mode >= 0 && mode < REDSTONE_MODES && mode != this.redstoneMode) {
            this.redstoneMode = mode;
            setChanged();
            // 通知邻居方块红石信号可能改变
            notifyNeighborsRedstoneChange();
        }
    }
    
    /**
     * 切换到下一个红石模式
     */
    public void cycleRedstoneMode() {
        byte oldMode = this.redstoneMode;
        this.redstoneMode = (byte) ((this.redstoneMode + 1) % REDSTONE_MODES);
        setChanged();
        // 通知邻居方块红石信号可能改变
        if (this.redstoneMode != oldMode) {
            notifyNeighborsRedstoneChange();
        }
    }
    
    /**
     * 通知邻居方块红石信号改变
     */
    protected void notifyNeighborsRedstoneChange() {
        if (this.level != null && !this.level.isClientSide()) {
            this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
            // 也更新自身方块状态
            this.level.blockUpdated(this.worldPosition, this.getBlockState().getBlock());
        }
    }
    
    /**
     * 获取红石模式名称（用于GUI显示）
     */
    public String getRedstoneModeName() {
        return switch (this.redstoneMode) {
            case 0 -> "mode.redstone.none";
            case 1 -> "mode.redstone.full";
            case 2 -> "mode.redstone.partial";
            case 3 -> "mode.redstone.not_full";
            case 4 -> "mode.redstone.empty";
            case 5 -> "mode.redstone.inverted";
            case 6 -> "mode.redstone.conditional";
            case 7 -> "mode.redstone.redstone_on";
            default -> "mode.redstone.unknown";
        };
    }

    /**
     * 从输出方向向相邻的非 IEnergyTile 能量存储推送能量
     * 遵循储能方块的输出方向规则（仅正面输出）
     */
    
    // ============ FE兼容代理管理 ============

    /**
     * 扫描相邻方块，管理FE机器的电网代理tile。
     */
    private void updateFECompatTiles(Level level, BlockPos pos) {
        Direction outputSide = getOutputSide();
        for (Direction dir : Direction.values()) {
            if (dir == outputSide) continue;

            BlockPos neighborPos = pos.relative(dir);
            if (!level.isLoaded(neighborPos)) continue;

            FECompatTile existing = feCompatTiles.get(dir);
            if (existing != null) {
                if (existing.isValid()) continue;
                feCompatTiles.remove(dir);
                if (existing.isRegistered()) {
                    EnergyNetGlobal.removeTile(existing);
                }
                continue;
            }

            boolean isIETile = level.getBlockEntity(neighborPos) instanceof IEnergyTile;
            if (isIETile) continue;

            if (EnergyBridge.hasCompatEnergyStorage(level, neighborPos, dir)) {
                FECompatTile compat = new FECompatTile(level, neighborPos, dir);
                feCompatTiles.put(dir, compat);
                EnergyNetGlobal.addTile(compat, level, neighborPos);
            }
        }
    }
private void pushEnergyToCompatSinks() {
        if (level == null) return;
        // 检查是否允许输出能量（红石模式控制）
        if (!energyStorage.isOutputEnabled()) return;
        try {
            long available = energyStorage.getAmount();
            if (available <= 0) return;

            Direction outputSide = getOutputSide();
            BlockPos neighborPos = worldPosition.relative(outputSide);

            if (level.getBlockEntity(neighborPos) instanceof IEnergyTile) return;

            if (!EnergyBridge.hasCompatEnergyStorage(level, neighborPos, outputSide)) return;

            long maxPush = Math.min(available, energyStorage.getMaxExtract());
            long pushed = EnergyBridge.pushToStorage(level, neighborPos, outputSide, maxPush);
            if (pushed > 0) {
                energyStorage.extract(pushed, false);
            }
        } catch (Exception e) {
            // 安全降级：不崩溃，只跳过本次推送
        }
    }

    // ==================== IC2 电网系统覆盖 ====================
    // 储能方块既是 Sink（从非正面接收能量）又是 Source（从正面输出能量）
    // 父类 mio_icif_Energy_Block 的实现仅在isPowerSource=true 时才输出能量
    // 但储能方块isPowerSource=false，因此必须覆盖这些方法

    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor acceptor, Direction direction) {
        return direction == getOutputSide();
    }

    @Override
    public boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction direction) {
        return direction != getOutputSide();
    }

    @Override
    public double getOfferedEnergy() {
        if (!energyStorage.isOutputEnabled()) return 0.0D;
        if (energyStorage.getAmount() <= 0) return 0.0D;
        return Math.min(energyStorage.getAmount(), energyStorage.getMaxExtract());
    }

    @Override
    public void drawEnergy(double amount) {
        if (!energyStorage.isOutputEnabled()) return;
        if (amount > 0.0D) {
            energyStorage.extract((long) amount, false);
        }
    }

    @Override
    public int getSourceTier() {
        ICableTier tier = getEffectiveCableTier();
        if (tier instanceof CableTier cableTier) {
            return EnergyNetGlobal.cableTierToSourceTier(cableTier);
        }
        return 0;
    }
    
    /**
     * Front face is output; other sides are input.
     */
    protected Direction getOutputSide() {
        BlockState state = getBlockState();
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
            return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        }
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
        }
        return Direction.NORTH;
    }

    public boolean canProvidePowerFromSide(Direction side) {
        return side == getOutputSide();
    }

    public boolean canConsumePowerFromSide(Direction side) {
        return side != getOutputSide();
    }

    public long getPowerOutput() {
        return Math.min(energyStorage.getAmount(), energyStorage.getMaxExtract());
    }

    public long extractPowerForConsumer(long amount, boolean simulate) {
        // 检查是否允许输出能量（红石模式控制）
        if (!energyStorage.isOutputEnabled()) return 0L;
        if (amount <= 0L) {
            return 0L;
        }
        long toExtract = Math.min(amount, energyStorage.getMaxExtract());
        return energyStorage.extract(toExtract, simulate);
    }

    /**
     * New IC2 grid system pushes energy via IEnergySink.injectEnergy().
     * This old method is no longer called; kept as no-op for compatibility.
     */
    protected void requestEnergyFromGrid() {
        // Energy is now distributed automatically by EnergyCalculator
    }

    /**
     * Drain charge-slot items into the internal buffer.
     * 
     * <p>通过 IItemAPI 进行电池放电，Addon 可以注册自定义电池类型。
     */
    protected void processBattery() {
        ItemStack batteryStack = itemHandler.getStackInSlot(BATTERY_SLOT);
        if (batteryStack.isEmpty()) {
            return;
        }

        // 检查储能方块是否已满
        long energyStored = apiGetStoredEnergy();
        long maxEnergy = apiGetMaxEnergy();
        if (energyStored >= maxEnergy) {
            return;
        }

        long spaceAvailable = maxEnergy - energyStored;
        long maxTransfer = Math.min(apiGetMaxReceive(), spaceAvailable);

        if (batteryStack.getItem() == Items.REDSTONE) {
            // 对齐IC2：有任何空间即消耗红石，不要求满800EU空缺
            long energyToAdd = Math.min(mio_icif_producer.REDSTONE_ENERGY_VALUE, spaceAvailable);
            if (energyToAdd > 0) {
                batteryStack.shrink(1);
                apiReceiveEnergy(energyToAdd, false);
                setChanged();
            }
            return;
        }

        // 处理电池放电（排除电力工具和电力护甲）
        if (apiIsBattery(batteryStack) && !apiIsElectricTool(batteryStack) && !apiIsElectricArmor(batteryStack)) {
            long batteryEnergy = apiGetBatteryStored(batteryStack);
            if (batteryEnergy <= 0) {
                return;
            }

            long energyToTransfer = Math.min(batteryEnergy, maxTransfer);
            long extractedEnergy = apiDischargeBattery(batteryStack, energyToTransfer, false);

            if (extractedEnergy > 0) {
                apiReceiveEnergy(extractedEnergy, false);
                setChanged();
            }
        }
    }
    
    /**
     * 给充电槽中的物品充电
     * 支持电池和电力护甲
     * 
     * <p>通过 IItemAPI 进行充电操作，Addon 可以注册自定义电池和护甲类型。
     */
    protected void chargeItems() {
        ItemStack chargeStack = itemHandler.getStackInSlot(CHARGE_SLOT);
        if (chargeStack.isEmpty()) {
            return;
        }
        
        // 检查储能方块是否有足够能量
        long availableEnergy = apiGetStoredEnergy();
        if (availableEnergy <= 0) {
            return;
        }
        
        // 处理电力护甲充电（优先检查，因为 IElectricArmorItem extends IBatteryItem）
        if (apiIsElectricArmor(chargeStack)) {
            if (apiIsArmorFull(chargeStack)) {
                return;
            }
            
            long armorMaxEnergy = apiGetArmorMaxEnergy(chargeStack);
            long armorCurrentEnergy = apiGetArmorStored(chargeStack);
            long spaceInArmor = armorMaxEnergy - armorCurrentEnergy;
            long maxTransfer = Math.min(apiGetMaxExtract(), spaceInArmor);
            long energyToTransfer = Math.min(availableEnergy, maxTransfer);
            
            long extractedEnergy = apiExtractEnergy(energyToTransfer, false);
            
            if (extractedEnergy > 0) {
                apiChargeElectricArmor(chargeStack, extractedEnergy, false);
                setChanged();
            }
        }
        // 处理电池充电（排除电力护甲，避免类型冲突）
        else if (apiIsBattery(chargeStack)) {
            if (apiIsBatteryFull(chargeStack)) {
                return;
            }
            
            long batteryMaxEnergy = apiGetBatteryCapacity(chargeStack);
            long batteryCurrentEnergy = apiGetBatteryStored(chargeStack);
            long spaceInBattery = batteryMaxEnergy - batteryCurrentEnergy;
            long batteryChargeRate = apiGetChargeRate(chargeStack);
            long maxTransfer = Math.min(apiGetMaxExtract(), Math.min(spaceInBattery, batteryChargeRate));
            long energyToTransfer = Math.min(availableEnergy, maxTransfer);
            
            long extractedEnergy = apiExtractEnergy(energyToTransfer, false);
            
            if (extractedEnergy > 0) {
                apiChargeBattery(chargeStack, extractedEnergy, false);
                setChanged();
            }
        }
    }
    
    // Container 接口实现
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
        ItemStack stack = itemHandler.getStackInSlot(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int toRemove = Math.min(amount, stack.getCount());
        ItemStack result = stack.copyWithCount(toRemove);
        if (toRemove >= stack.getCount()) {
            itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        } else {
            itemHandler.setStackInSlot(slot, stack.copyWithCount(stack.getCount() - toRemove));
        }
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
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
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }
    
    @Override
    public void setChanged() {
        super.setChanged();
    }
    
    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }
    
    @Override
    public void clearContent() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
        setChanged();
    }
    
    /**
     * 提供能量存储能力
     * 储能方块：正面输出，其他方向输入
     */
    @Override
    public CustomEUEnergyStorage getEnergyStorageCapability(@Nullable Direction side) {
        // 简化实现，直接返回能量存储
        return energyStorage;
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // 保存物品栏（能量由父类自动保存）
        if (itemHandler != null) {
            tag.put("Items", itemHandler.serializeNBT(registries));
        }
        // 保存红石模式
        tag.putByte("RedstoneMode", this.redstoneMode);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // 加载物品栏（能量由父类自动加载）
        if (itemHandler != null && tag.contains("Items")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
        }
        // 加载红石模式
        if (tag.contains("RedstoneMode")) {
            this.redstoneMode = tag.getByte("RedstoneMode");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        // 同步物品栏到客户端
        if (itemHandler != null) {
            tag.put("Items", itemHandler.serializeNBT(registries));
        }
        // 同步红石模式到客户端
        tag.putByte("RedstoneMode", this.redstoneMode);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        // 从服务端同步物品栈
        if (itemHandler != null && tag.contains("Items")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
        }
        // 从服务端同步红石模式
        if (tag.contains("RedstoneMode")) {
            this.redstoneMode = tag.getByte("RedstoneMode");
        }
    }

    // ==================== IEnergyContainerBlock 接口实现 ====================

    @Override
    public net.neoforged.neoforge.items.IItemHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public int getBatterySlotCount() {
        return LAYOUT.getCount(com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotType.BATTERY);
    }

    @Override
    public long getChargeRate() {
        return energyStorage.getMaxReceive();
    }

    @Override
    public long getDischargeRate() {
        return energyStorage.getMaxExtract();
    }

    @Override
    public long getTotalCharged() {
        return 0; // 储能容器不跟踪总充电量
    }

    @Override
    public long getTotalDischarged() {
        return 0; // 储能容器不跟踪总放电量
    }

    @Override
    public long getRunningTime() {
        return 0; // 储能容器不跟踪运行时间
    }

    @Override
    public String getRunningState() {
        long energy = energyStorage.getAmount();
        long capacity = energyStorage.getCapacity();
        if (energy > 0 && energy < capacity) {
            return "工作中";
        } else if (energy >= capacity) {
            return "已满";
        } else {
            return "空闲";
        }
    }
}