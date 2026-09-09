package com.singularity_iteration.mio_icif.Blocks.entity;

import com.singularity_iteration.mio_icif.Blocks.entity.slot.MachineItemHandler;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.item.IItemAPI;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.EUApi;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.IEUEnergyStorage;
import com.singularity_iteration.mio_icif.energy.grid.IEnergyTile;
import com.singularity_iteration.mio_icif.integration.mi.EnergyBridge;
import com.singularity_iteration.mio_icif.integration.mi.MICompat;
import com.singularity_iteration.mio_icif.integration.gt.GTCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 能量发电机方块实体，继承自能量方块实体
 * 作为所有发电机的父类，提供接受材料生成能量的基础功能
 * 实现 WorldlyContainer 接口以支持漏斗和管道交互
 * 使用 EU (Energy Unit) 能量系统
 * 
 * <p>燃料处理通过 IItemAPI 进行，Addon 可以注册自定义燃料类型。
 */
@SuppressWarnings("null")
public abstract class mio_icif_Energy_Generator extends mio_icif_Energy_Block implements WorldlyContainer, com.singularity_iteration.mio_icif.api.machine.IGeneratorBlock {
    
    // ==================== API 方法层 ====================
    // 燃料和能量操作通过受保护的方法进行，子类可以覆盖以改变行为

    /**
     * 获取物品 API 实例
     */
    protected IItemAPI getItemAPI() {
        return MioIcifAPI.instance().getItemAPI();
    }

    /**
     * 通过 API 获取燃料燃烧时间
     * 子类可以覆盖此方法以支持自定义燃料
     */
    protected int apiGetFuelBurnTime(ItemStack fuel) {
        // 首先尝试通过 API 查询（如果燃料是模组电池）
        if (getItemAPI().isBattery(fuel)) {
            long energy = getItemAPI().getBatteryStored(fuel);
            // 每 100 EU = 1 tick 燃烧时间（可被子类调整）
            return (int) (energy / 100);
        }
        // 回退到子类的抽象方法
        return getFuelBurnTime(fuel);
    }

    /**
     * 通过 API 检查物品是否为电池
     */
    protected boolean apiIsBattery(ItemStack stack) {
        return getItemAPI().isBattery(stack);
    }

    /**
     * 通过 API 从电池提取能量
     */
    protected long apiDischargeBattery(ItemStack stack, long amount, boolean simulate) {
        return getItemAPI().dischargeBattery(stack, amount, simulate);
    }

    /**
     * 通过 API 获取电池当前能量
     */
    protected long apiGetBatteryStored(ItemStack stack) {
        return getItemAPI().getBatteryStored(stack);
    }

    /**
     * 通过 API 为电池充电
     */
    protected long apiChargeBattery(ItemStack stack, long amount, boolean simulate) {
        return getItemAPI().chargeBattery(stack, amount, simulate);
    }

    /**
     * 通过 API 获取电池容量
     */
    protected long apiGetBatteryCapacity(ItemStack stack) {
        return getItemAPI().getBatteryCapacity(stack);
    }

    /**
     * 通过 API 获取电池充电速率
     */
    protected long apiGetChargeRate(ItemStack stack) {
        return getItemAPI().getChargeRate(stack);
    }

    /**
     * 通过 API 检查电池是否已充满
     */
    protected boolean apiIsBatteryFull(ItemStack stack) {
        return getItemAPI().isBatteryFull(stack);
    }

    /**
     * 通过 API 生成能量（内部发电）
     * 使用 apiGenerateEnergy 绕过 maxReceive 限制
     */
    protected long apiGenerateEnergy(long amount) {
        return apiGenerateEnergy(amount, false);
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
     * 通过 API 获取最大能量容量
     * 直接访问 energyStorage，避免通过 getEnergyAPI() 造成无限递归
     */
    @Override
    protected long apiGetMaxEnergy() {
        return energyStorage.getCapacity();
    }

    /**
     * 通过 API 提取能量
     * 直接访问 energyStorage，避免通过 getEnergyAPI() 造成无限递归
     */
    @Override
    protected long apiExtractEnergy(long amount, boolean simulate) {
        return energyStorage.extract(amount, simulate);
    }
    
    // 槽位布局
    protected SlotLayout slotLayout;
    // 物品处理器
protected MachineItemHandler itemHandler;

    // 当前燃烧时间
    public int burnTime = 0;

    // 当前物品的总燃烧时间
public int burnDuration = 0;

    // 能量生成速率 (EU/tick)
    protected final long energyGenerationRate;

    // 燃料槽索引
protected static final int FUEL_SLOT = 0;

    // 充电槽索引
protected static final int CHARGE_SLOT = 1;
    
    /**
     * 构造函数
 * @param pos 方块位置
     * @param state 方块状态
 * @param type 方块实体类型
     * @param layout 槽位布局
     * @param energyGenerationRate 能量生成速率 (EU/tick)
     */
    public mio_icif_Energy_Generator(BlockPos pos, BlockState state, BlockEntityType<?> type,
                                     SlotLayout layout, long energyGenerationRate) {
        super(pos, state, type);
        this.slotLayout = layout;
        this.itemHandler = createItemHandler(layout);
        this.energyGenerationRate = energyGenerationRate;
        // 发电机是电源
        setAsPowerSource(energyGenerationRate);
    }

    /**
     * 构造函数（支持自定义能量上限）
     * @param pos 方块位置
     * @param state 方块状态
 * @param type 方块实体类型
     * @param layout 槽位布局
     * @param energyGenerationRate 能量生成速率 (EU/tick)
     * @param capacity 能量容量上限 (EU)
     * @param maxReceive 最大输入速率 (EU/tick)
     * @param maxExtract 最大输出速率 (EU/tick)
     * @param cableTier 电缆等级
     */
    public mio_icif_Energy_Generator(BlockPos pos, BlockState state, BlockEntityType<?> type,
                                     SlotLayout layout, long energyGenerationRate,
                                     long capacity, long maxReceive, long maxExtract, CableTier cableTier) {
        super(pos, state, type, capacity, maxReceive, maxExtract, cableTier);
        this.slotLayout = layout;
        this.itemHandler = createItemHandler(layout);
        this.energyGenerationRate = energyGenerationRate;
        // 发电机是电源
        setAsPowerSource(energyGenerationRate);
    }

    /**
     * 创建物品处理器
 */
    protected MachineItemHandler createItemHandler(SlotLayout layout) {
        MachineItemHandler handler = new MachineItemHandler(layout) {
            @Override
            protected void onContentsChanged(int slot) {
                mio_icif_Energy_Generator.this.setChanged();
            }
        };
        return handler;
    }
    
    /**
     * 每tick更新逻辑，处理能量生成
 * 
     * 重要：能量分配必须在燃烧检查之前执行！
     * 这样可以确保发电机在决定是否停止燃烧之前，先尝试输出能量
 * 避免因内部存储满就停止燃烧，而实际上能量可以通过电网输出
 * 
     * 注意：如果发电机通过电网分配能量（shouldDirectlyDistributeEnergy返回false），
     * 则跳过直接能量分配，由电网通过extractPowerForConsumer提取能量
 * 
     * 燃烧停止逻辑：当能量存储已满且电网也无需求时
 * 等待当前燃烧进度自然结束（burnTime递减到0），而不是立即停止
 */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_Energy_Generator blockEntity) {
        if (level.isClientSide()) {
            return;
        }
        
        // 记录之前的燃烧状态
    boolean wasBurning = blockEntity.isBurning();
        
        // 先给充电槽中的物品充电
    blockEntity.chargeItems();
        
        if (blockEntity.shouldDirectlyDistributeEnergy()) {
            blockEntity.distributeEnergy();
        } else {
            blockEntity.distributeEnergyToCompatSinks();
        }
        
        // 分配完能量后再检查能量存储是否已满
    // 只有在能量真正无法输出时（存储已满且无法分配），才停止燃烧
    boolean isEnergyFull = blockEntity.apiGetStoredEnergy() >= 
                              blockEntity.apiGetMaxEnergy();
        
        // 如果正在燃烧，让当前燃烧自然结束
        if (blockEntity.isBurning()) {
            if (isEnergyFull) {
                // 存储已满且电网无需求：让当前燃烧进度自然递减到0，不生成能量也不消耗新燃料
                blockEntity.burnTime--;
                // 当前燃料烧完后，不尝试消耗新燃料（因为存储已满无法输出）
                if (blockEntity.burnTime <= 0) {
                    blockEntity.burnTime = 0;
                }
            } else {
                // 能量未满，正常生成能量并递减燃烧时间
                blockEntity.generateEnergy();
                blockEntity.burnTime--;
                
                // 如果燃烧完毕，尝试消耗新的燃料
            if (blockEntity.burnTime <= 0) {
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
            // 使用 mio_icif_Block_Thermal_Generator.ACTIVE 属性
        newState = newState.setValue(com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Thermal_Generator.ACTIVE, isBurning);
            level.setBlock(pos, newState, 3);
        }
        
        // 标记方块实体已更新
    blockEntity.setChanged();
    }
    
    /**
     * 向相邻方块实体分配能量
 * 发电机从六面输出能量
     */
    protected void distributeEnergy() {        if (apiGetStoredEnergy() <= 0) {
            return;
        }
        
        // 向六个方向输出能量
    for (Direction direction : Direction.values()) {
            outputEnergyToDirection(direction);
        }
    }
    
    /**
     * 向相邻的非IEnergyTile兼容方块分配能量
     * 当发电机通过电网分配能量时（shouldDirectlyDistributeEnergy返回false），
     * 电网只能向IEnergySink节点输送能量，无法识别AE2等模组的能量接收器。
     * 此方法作为补充，将能量推送到相邻的非IEnergyTile但具有兼容能量存储的方块。
     */
    protected void distributeEnergyToCompatSinks() {
        if (apiGetStoredEnergy() <= 0) {
            return;
        }
        for (Direction direction : Direction.values()) {
            if (apiGetStoredEnergy() <= 0) break;
            outputEnergyToCompatSink(direction);
        }
    }

    /**
     * 向指定方向的非IEnergyTile兼容方块输出能量
     * 跳过IEnergyTile方块（由电网处理），仅向AE2等外部模组的能量存储推送能量
     */
    private void outputEnergyToCompatSink(Direction direction) {
        BlockPos adjacentPos = worldPosition.relative(direction);

        if (level.getBlockEntity(adjacentPos) instanceof IEnergyTile) {
            return;
        }

        try {
            long energyToTransfer = Math.min(energyStorage.getAmount(), energyStorage.getMaxExtract());
            long pushed = EnergyBridge.pushToStorage(level, adjacentPos, direction, energyToTransfer);
            if (pushed > 0) {
                energyStorage.extract(pushed, false);
            }
        } catch (Exception e) {
        }
    }

    /**
     * 判断发电机是否应该直接向相邻方块分配能量
     * 
     * 默认返回 true，表示发电机直接向相邻方块输出能量
 * 对于使用电网分配能量的发电机（如火力发电机），应重写此方法返回false
 * 由电网通过 extractPowerForConsumer 从发电机的存储中提取能量
 * 
     * @return true = 直接分配能量（默认），false = 通过电网分配能量
     */
    protected boolean shouldDirectlyDistributeEnergy() {
        return true;
    }
    
    /**
     * 向指定方向输出能量
 */
    private void outputEnergyToDirection(Direction direction) {
        BlockPos adjacentPos = worldPosition.relative(direction);

        try {
            IEUEnergyStorage adjacentStorage = level.getCapability(EUApi.SIDED, adjacentPos, direction.getOpposite());

            if (adjacentStorage == null && MICompat.isMILoaded()) {
                Object miStorage = MICompat.getMIStorage(level, adjacentPos, direction.getOpposite());
                if (miStorage != null) {
                    adjacentStorage = MICompat.wrapMIStorage(miStorage);
                }
            }

            if (adjacentStorage != null) {
                long energyToTransfer = Math.min(energyStorage.getAmount(), energyStorage.getMaxExtract());
                long energyReceived = adjacentStorage.receive(energyToTransfer, false);
                if (energyReceived > 0) {
                    energyStorage.extract(energyReceived, false);
                }
                return;
            }

            if (GTCompat.isGTLoaded()) {
                Object gtContainer = GTCompat.getGTEnergyContainer(level, adjacentPos, direction.getOpposite());
                if (gtContainer != null && GTCompat.inputsEnergy(gtContainer, direction.getOpposite())) {
                    long energyToTransfer = Math.min(energyStorage.getAmount(), energyStorage.getMaxExtract());

                    long[] voltageAndAmperage = GTCompat.calculateGTVoltageAndAmperage(energyToTransfer, energyStorage.getCableTier());
                    long voltage = voltageAndAmperage[0];
                    long amperage = voltageAndAmperage[1];

                    long usedAmperage = GTCompat.acceptEnergyFromNetwork(gtContainer, direction.getOpposite(), voltage, amperage);
                    long energyTransferred = usedAmperage * voltage;

                    if (energyTransferred > 0) {
                        energyStorage.extract(energyTransferred, false);
                    }
                    return;
                }
            }

            long energyToTransfer = Math.min(energyStorage.getAmount(), energyStorage.getMaxExtract());
            long pushed = EnergyBridge.pushToStorage(level, adjacentPos, direction, energyToTransfer);
            if (pushed > 0) {
                energyStorage.extract(pushed, false);
            }
        } catch (Exception e) {
            // 安全降级：不崩溃，只跳过本次输出
        }
    }
    
    /**
     * 生成能量
     */
    protected void generateEnergy() {
        long energyGenerated = Math.min(energyGenerationRate, 
            apiGetMaxEnergy() - apiGetStoredEnergy());
        // 通过 API 写入能量
        apiGenerateEnergy(energyGenerated);
    }
    
    /**
     * 给充电槽中的物品充电
     */
    protected void chargeItems() {
        // 检查是否有充电槽
        if (itemHandler == null || itemHandler.getSlots() <= CHARGE_SLOT) {
            return;
        }

        ItemStack chargeStack = itemHandler.getStackInSlot(CHARGE_SLOT);
        if (chargeStack.isEmpty()) {
            return;
        }
        
        // 检查物品是否是电池
        if (apiIsBattery(chargeStack)) {
            long currentEnergy = apiGetBatteryStored(chargeStack);
            long batteryMaxEnergy = apiGetBatteryCapacity(chargeStack);
            long batteryChargeRate = apiGetChargeRate(chargeStack);
            
            if (currentEnergy >= batteryMaxEnergy) {
                return;
            }
            
            long availableEnergy = apiGetStoredEnergy();
            if (availableEnergy <= 0) {
                return;
            }
            
            long effectiveChargeRate = Math.min(batteryChargeRate, apiGetMaxExtract());
            long energyToCharge = Math.min(effectiveChargeRate, batteryMaxEnergy - currentEnergy);
            energyToCharge = Math.min(energyToCharge, availableEnergy);
            
            long energyExtracted = apiExtractEnergy(energyToCharge, false);
            
            if (energyExtracted > 0) {
                apiChargeBattery(chargeStack, energyExtracted, false);
            }
        }
    }
    
    /**
     * 检查是否正在燃烧
     */
    public boolean isBurning() {
        return burnTime > 0;
    }

    /**
     * 获取当前燃烧时间
     */
    public int getBurnTime() {
        return burnTime;
    }

    /**
     * 获取最大燃烧时间（当前燃料的总燃烧时间）
     */
    public int getMaxBurnTime() {
        return burnDuration;
    }

    @Override
    public int getDefaultBurnTime() {
        return getMaxBurnTime();
    }

    @Override
    public void setBurnTime(int ticks) {
        this.burnTime = ticks;
        this.setChanged();
    }

    /**
     * 获取能量生成速率 (EU/tick)
     */
    public long getEnergyGenerationRate() {
        return energyGenerationRate;
    }

    // ==================== IGeneratorBlock 接口实现 ====================

    @Override
    public long getPowerOutput() {
        return energyGenerationRate;
    }

    @Override
    public net.minecraft.world.item.ItemStack getFuelSlotItem() {
        return itemHandler != null ? itemHandler.getStackInSlot(FUEL_SLOT) : net.minecraft.world.item.ItemStack.EMPTY;
    }

    @Override
    public net.minecraft.world.item.ItemStack getChargeSlotItem() {
        return itemHandler != null ? itemHandler.getStackInSlot(CHARGE_SLOT) : net.minecraft.world.item.ItemStack.EMPTY;
    }

    @Override
    public CableTier getCableTier() {
        return energyStorage.getCableTier();
    }
    
    /**
     * 消耗燃料
     */
    protected void consumeFuel() {
        // 检查能量存储是否已满
    boolean isEnergyFull = getEnergyStorage().getAmount() >=
                              getEnergyStorage().getCapacity();

        // 如果能量已满，不消耗燃料
    if (isEnergyFull) {
            return;
        }

        ItemStack fuelStack = itemHandler.getStackInSlot(FUEL_SLOT);
        if (!fuelStack.isEmpty()) {
            int burnTime = getFuelBurnTime(fuelStack);
            if (burnTime > 0) {
                this.burnDuration = burnTime;
                this.burnTime = burnTime;

                // 在消耗前获取容器物品（如岩浆桶返回空桶）
                ItemStack remainingItem = fuelStack.getCraftingRemainingItem();

                // 消耗燃料
            fuelStack.shrink(1);

                // 如果燃料堆为空，放入容器物品（如果有的话）
            if (fuelStack.isEmpty()) {
                    itemHandler.setStackInSlot(FUEL_SLOT, remainingItem);
                }
            }
        }
    }
    
    /**
     * 获取燃料的燃烧时间，子类可以重写此方法来定义可用的燃料
 * @param fuel 燃料物品
     * @return 燃烧时间 (tick)
     */
    public abstract int getFuelBurnTime(ItemStack fuel);
    
    /**
     * 获取燃烧进度，用于GUI显示
     * @return 燃烧进度 (0-1)
     */
    public float getBurnProgress() {
        if (burnDuration == 0) {
            return 0;
        }
        return (float) burnTime / burnDuration;
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (itemHandler != null) {
            tag.put("Items", itemHandler.serializeNBT(registries));
        }
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnDuration", burnDuration);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (itemHandler != null && tag.contains("Items")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
        }
        burnTime = tag.getInt("BurnTime");
        burnDuration = tag.getInt("BurnDuration");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (itemHandler != null) {
            tag.put("Items", itemHandler.serializeNBT(registries));
        }
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnDuration", burnDuration);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (itemHandler != null && tag.contains("Items")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
        }
        burnTime = tag.getInt("BurnTime");
        burnDuration = tag.getInt("BurnDuration");
    }
    
    // Container接口实现
    @Override
    public int getContainerSize() {
        return itemHandler != null ? itemHandler.getSlots() : 0;
    }

    @Override
    public boolean isEmpty() {
        if (itemHandler == null) return true;
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return itemHandler != null ? itemHandler.getStackInSlot(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (itemHandler == null) return ItemStack.EMPTY;
        ItemStack stack = itemHandler.getStackInSlot(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        int toRemove = Math.min(amount, stack.getCount());
        ItemStack result = stack.copyWithCount(toRemove);
        stack.shrink(toRemove);
        if (stack.isEmpty()) {
            itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (itemHandler == null) return ItemStack.EMPTY;
        ItemStack stack = itemHandler.getStackInSlot(slot);
        itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (itemHandler != null) {
            itemHandler.setStackInSlot(slot, stack);
            if (stack.getCount() > getMaxStackSize()) {
                stack.setCount(getMaxStackSize());
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return level.getBlockEntity(worldPosition) == this &&
               player.distanceToSqr(worldPosition.getX() + 0.5,
                                   worldPosition.getY() + 0.5,
                                   worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        if (itemHandler != null) {
            for (int i = 0; i < itemHandler.getSlots(); i++) {
                itemHandler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    // ==================== WorldlyContainer 接口实现 ====================

    @Override
    public int[] getSlotsForFace(Direction side) {
        // 所有方向都可以访问燃料槽和充电槽
    return new int[]{FUEL_SLOT, CHARGE_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        // 燃料槽：只接受燃料
    if (slot == FUEL_SLOT) {
            return getFuelBurnTime(stack) > 0;
        }
        // 充电槽：只接受电池类物品
        if (slot == CHARGE_SLOT) {
            return isBattery(stack);
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        // 充电槽可以提取（电池充满后可以取出）
        if (slot == CHARGE_SLOT) {
            return true;
        }
        // 燃料槽不能提取（防止漏斗吸走燃料）
    return false;
    }

    /**
     * 检查物品是否是电池
     * @param stack 物品栈
 * @return 是否是电池
 */
    protected boolean isBattery(ItemStack stack) {
        return getItemAPI().isBattery(stack);
    }

    /**
     * 获取物品处理器（用于 capability 系统）
 * @param side 方向
     * @return 物品处理器
 */
    public IItemHandler getItemHandlerCapability(@Nullable Direction side) {
        return new GeneratorItemHandler(side);
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    // ==================== GeneratorItemHandler 内部类 ====================

    /**
     * 发电机的物品处理器
 * 用于 capability 系统，限制物品的插入和提取行为
 */
    protected class GeneratorItemHandler implements IItemHandlerModifiable {
        private final Direction side;

        public GeneratorItemHandler(@Nullable Direction side) {
            this.side = side;
        }

        @Override
        public int getSlots() {
            return itemHandler != null ? itemHandler.getSlots() : 0;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return itemHandler != null ? itemHandler.getStackInSlot(slot) : ItemStack.EMPTY;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (itemHandler == null) return stack;
            if (!canPlaceItemThroughFace(slot, stack, side)) {
                return stack;
            }
            return itemHandler.insertItem(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (itemHandler == null) return ItemStack.EMPTY;
            if (!canTakeItemThroughFace(slot, itemHandler.getStackInSlot(slot), side)) {
                return ItemStack.EMPTY;
            }
            return itemHandler.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemHandler != null ? itemHandler.getSlotLimit(slot) : getMaxStackSize();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return canPlaceItemThroughFace(slot, stack, side);
        }

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            if (itemHandler != null) {
                itemHandler.setStackInSlot(slot, stack);
            }
        }
    }
}