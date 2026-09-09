package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.mio_icif_HeatU_Block;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 蒸汽机方块实
 * 用水 + HU 热量产生蒸汽或过热蒸
 *
 * 工作原理
 * - 从除正面外的其他面接
HU 热量
 * - 消耗水/蒸馏水产生蒸
 * - 1 mB 
= 100 mB 蒸汽，消
100 HU
 * - 使用普通水会钙化，蒸馏水不
 */
@SuppressWarnings("null")
public class mio_icif_steam_generator extends mio_icif_HeatU_Block {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .extra(2)
        .upgrade(2)
        .build();

    // 槽位定义
    public static final int WATER_BUCKET_SLOT = 0;   // 水桶输入
public static final int EMPTY_BUCKET_SLOT = 1;   // 空桶输出
public static final int UPGRADE_SLOT_START = 2;  // 升级槽起
public static final int UPGRADE_SLOT_COUNT = 2;  // 升级槽数
public static final int TOTAL_SLOTS = 4;

    // 流体容量
    public static final int WATER_TANK_CAPACITY = 10000;  // 水槽 10 
public static final int STEAM_TANK_CAPACITY = 100000; // 蒸汽

    // 工作参数（原
public static final float HEAT_PER_HU = 5.0E-4F;   // 
    public static final float COOLING_PER_TICK = 0.01F; // 不工作时
    public static final float MAX_SYSTEM_HEAT = 500.0F; // 系统热量上限（超过爆炸）
    public static final int MAX_HU_PER_TICK = 1200;     // 
    public static final int STEAM_EXPANSION = 100;      // 1mB 

    public static final int MAX_CALCIFICATION = 100000; // 钙化上限（原版）
    public static final float EPSILON = 1.0E-4F;

 // 热量配置（HU 输入冲
public static final int HEAT_CAPACITY = 50000;
    public static final int MAX_HEAT_RECEIVE = 1000;
    public static final int MAX_HEAT_EXTRACT = 0;
    public static final int MAX_TEMP = 500;
    public static final float HEAT_LOSS_FACTOR = 0.0f;

    // 流体存储
    protected final FluidTank waterTank;
    protected final FluidTank steamTank;

    // 状态
private int calcification = 0;
    private boolean isWorking = false;

    // 原版 IC2 核心参数
    private float systemHeat = 20.0F; // 系统热量/温度（从环境温度开始）
    private int pressure = 0;         // 压力阀 0-300
    private int inputMB = 0;          // 水流量设

    // 输出类型记录（供 GUI 显示
@SuppressWarnings("null")
public enum OutputType { NONE, WATER, DISTILLEDWATER, STEAM, SUPERHEATEDSTEAM }
    private OutputType outputFluid = OutputType.NONE;
    private int outputMB = 0;

    // 上一 tick 的输入输出速率，供 GUI 显示（IC2 风格
private int lastWaterInput = 0;   // mB/t
    private int lastSteamOutput = 0;  // mB/t
    private long lastHeatInput = 0;

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> waterTank.getFluidAmount();
                case 1 -> waterTank.getCapacity();
                case 2 -> calcification;
                case 3 -> MAX_CALCIFICATION;
                case 4 -> (int) Math.min(lastHeatInput, Integer.MAX_VALUE);
                case 5 -> Math.round(systemHeat * 10.0F);
                case 6 -> pressure;
                case 7 -> inputMB;
                case 8 -> outputMB;
                case 9 -> gaugeHeatScaled(76);
                case 10 -> gaugeCalcificationScaled(58);
                case 11 -> gaugeLiquidScaled(47, 0);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() { return 12; }
    };

    public mio_icif_steam_generator(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.STEAM_GENERATOR_ENTITY_TYPE.get());
    }

    public mio_icif_steam_generator(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(type, pos, state, HEAT_CAPACITY, MAX_HEAT_RECEIVE, MAX_HEAT_EXTRACT,
              20, MAX_TEMP, HEAT_LOSS_FACTOR);

        this.waterTank = new FluidTank(WATER_TANK_CAPACITY, fluidStack -> {
            if (fluidStack.isEmpty()) return true;
            return fluidStack.getFluid() == Fluids.WATER || fluidStack.getFluid() == mio_icif_fluids.DISTILLEDWATER.get();
        });

        this.steamTank = new FluidTank(STEAM_TANK_CAPACITY, fluidStack -> {
            if (fluidStack.isEmpty()) return true;
            return fluidStack.getFluid() == mio_icif_fluids.STEAM.get()
                || fluidStack.getFluid() == mio_icif_fluids.SUPERHEATEDSTEAM.get();
        });

        // 初始化槽位布局和物品处理器
        this.slotLayout = LAYOUT;
        this.itemHandler = createItemHandler(LAYOUT);
        this.itemHandler.setValidator((slot, stack, slotType) -> mio_icif_steam_generator.this.isItemValidForSlot(slot, stack));
    }

    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (isUpgradeSlot(slot)) {
            return stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Upgrade.mio_icif_upgrade;
        }
        return switch (slot) {
            case WATER_BUCKET_SLOT -> stack.is(Items.WATER_BUCKET) || stack.is(mio_icif_fluids.DISTILLEDWATER_BUCKET.get());
            case EMPTY_BUCKET_SLOT -> false;
            default -> false;
        };
    }

    /**
     * 
tick 更新逻辑（原
IC2 风格
 */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_steam_generator blockEntity) {
        if (level.isClientSide()) return;

        blockEntity.receiveHeatFromSides();
        blockEntity.handleWaterBucketSlot();
        blockEntity.outputStoredSteamToNeighbors(); // 将内部蒸汽槽的蒸汽推送给相邻容器

        boolean wasWorking = blockEntity.isWorking;

        if (blockEntity.isCalcified()) {
            blockEntity.isWorking = false;
            blockEntity.cooldown(COOLING_PER_TICK);
        } else {
            blockEntity.isWorking = blockEntity.work();
        }

        if (!blockEntity.isWorking) {
            blockEntity.cooldown(COOLING_PER_TICK);
        }

        if (wasWorking != blockEntity.isWorking) {
            blockEntity.updateBlockState(blockEntity.isWorking);
        }

        blockEntity.handleAutomationUpgrades();
        blockEntity.setChanged();
    }

    /**
     * 从除正面外的其他面接收热
 */
    private void receiveHeatFromSides() {
        if (level == null || level.isClientSide()) return;

        lastHeatInput = 0;
        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        long heatNeeded = HEAT_CAPACITY - heatStorage.getHeatStored();
        if (heatNeeded <= 0) return;

        for (Direction direction : Direction.values()) {
            if (direction == facing) continue;
            if (heatNeeded <= 0) return;

            BlockPos neighborPos = worldPosition.relative(direction);
            IMioIcifCapabilities.IHeatStorage adjacentHeat = level.getCapability(IMioIcifCapabilities.HEAT_STORAGE_BLOCK, neighborPos, direction.getOpposite());

            if (adjacentHeat != null && adjacentHeat.canExtractHeat()) {
                long heatToExtract = Math.min(heatNeeded, adjacentHeat.getHeatStored());
                long extracted = adjacentHeat.extractHeat(heatToExtract, false);
                if (extracted > 0) {
                    heatStorage.receiveHeat(extracted, false);
                    lastHeatInput += extracted;
                    heatNeeded -= extracted;
                }
            }
        }
    }

    /**
     * 处理水桶输入
 */
    private void handleWaterBucketSlot() {
        ItemStack bucketStack = itemHandler.getStackInSlot(WATER_BUCKET_SLOT);
        if (bucketStack.isEmpty()) return;

        boolean isWaterBucket = bucketStack.is(Items.WATER_BUCKET);
        boolean isDistilledBucket = bucketStack.is(mio_icif_fluids.DISTILLEDWATER_BUCKET.get());
        if (!isWaterBucket && !isDistilledBucket) return;

        Fluid fluidToFill = isDistilledBucket ? mio_icif_fluids.DISTILLEDWATER.get() : Fluids.WATER;
        if (waterTank.getFluidAmount() + FluidType.BUCKET_VOLUME > waterTank.getCapacity()) return;

        ItemStack emptyStack = itemHandler.getStackInSlot(EMPTY_BUCKET_SLOT);
        if (!emptyStack.isEmpty() && (!emptyStack.is(Items.BUCKET) || emptyStack.getCount() >= emptyStack.getMaxStackSize())) {
            return;
        }

        int filled = waterTank.fill(new FluidStack(fluidToFill, FluidType.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE);
        if (filled >= FluidType.BUCKET_VOLUME) {
            itemHandler.extractItem(WATER_BUCKET_SLOT, 1, false);
            if (emptyStack.isEmpty()) {
                itemHandler.setStackInSlot(EMPTY_BUCKET_SLOT, new ItemStack(Items.BUCKET));
            } else {
                emptyStack.grow(1);
            }
            setChanged();
        }
    }

    /**
     * 原版 IC2 风格的核心工作逻辑（修复：按需提取 HU，避免多余热量注
systemHeat
 */
    private boolean work() {
        lastWaterInput = 0;
        lastSteamOutput = 0;
        outputMB = 0;
        outputFluid = OutputType.NONE;

        // 没有水或流量
    if (waterTank.getFluidAmount() <= 0 || inputMB <= 0) {
            if (systemHeat > 25.0F) {
                cooldown(COOLING_PER_TICK);
                lastHeatInput = 0;
            } else {
                // 温度已经接近环境温度，什么都不做
                lastHeatInput = 0;
            }
            return false;
        }

        Fluid inputFluid = waterTank.getFluid().getFluid();
        boolean hasDistilledWater = inputFluid == mio_icif_fluids.DISTILLEDWATER.get();
        int maxAmount = Math.min(inputMB, waterTank.getFluidAmount());

        // 压力=0 且温
        if (pressure == 0 && systemHeat < 99.9999F) {
            lastWaterInput = maxAmount;
            outputMB = maxAmount;
            outputFluid = hasDistilledWater ? OutputType.DISTILLEDWATER : OutputType.WATER;
            int transferred = outputFluidToNeighbors(inputFluid, maxAmount);
            if (transferred > 0) {
                waterTank.drain(transferred, IFluidHandler.FluidAction.EXECUTE);
            }
            // 仍需要加
        long heatingHu = Math.min(heatStorage.getHeatStored(), 500);
            if (heatingHu > 0 && systemHeat < 90.0F) {
                heatStorage.extractHeat(heatingHu, false);
                lastHeatInput = heatingHu;
                heatup(heatingHu);
            } else {
                lastHeatInput = 0;
            }
            return true;
        }

        float hUneeded = 100.0F + pressure / 220.0F * 100.0F;
        float targetTemp = 100.0F + pressure / 220.0F * 100.0F * 2.74F;
        float reqHeat = targetTemp - systemHeat;

        // === 先计算实际需要的 HU ===
        int huForHeating = 0;
        if (reqHeat > EPSILON) {
            huForHeating = (int) Math.ceil(reqHeat / HEAT_PER_HU);
        }

        int maxHuForSteam = (int) Math.ceil(maxAmount * hUneeded);
        int totalHuNeeded = huForHeating + maxHuForSteam;

        // 按需提取 HU（不超过缓冲和上限）
        long heatInput = Math.min(heatStorage.getHeatStored(), Math.min(totalHuNeeded, MAX_HU_PER_TICK));

        if (heatInput <= 0 && reqHeat > EPSILON) {
            // 需要加热但
        lastHeatInput = 0;
            return false;
        }

        lastHeatInput = heatInput;

        long remainingHu = heatInput;

        if (huForHeating > 0 && remainingHu > 0) {
            long actualHeatingHu = Math.min(huForHeating, remainingHu);
            heatStorage.extractHeat(actualHeatingHu, false);
            heatup((int) actualHeatingHu);
            remainingHu -= actualHeatingHu;
            reqHeat = targetTemp - systemHeat;
        }

        // === 用剩
        int activeAmount = 0;
        if (remainingHu > 0) {
            activeAmount = Math.min(maxAmount, (int) (remainingHu / hUneeded));
            if (activeAmount > 0) {
                long huUsed = (long) Math.ceil(activeAmount * hUneeded);
                heatStorage.extractHeat(huUsed, false);
                remainingHu -= huUsed;
            }
        }

        // === 如果温度高于目标，用冷却水生产蒸汽来降温 ===
        int coolingAmount = 0;
        if (reqHeat <= -0.1001F) {
            coolingAmount = Math.min(maxAmount, (int) (-reqHeat / 0.1F));
            coolingAmount = Math.min(coolingAmount, 20);
        }

        int totalAmount = Math.max(activeAmount, coolingAmount);
        if (totalAmount <= 0) {
            // 没有消耗水，如果温度高于目标就冷却
            if (reqHeat < -EPSILON) {
                cooldown(Math.min(-reqHeat, 0.2F));
            }
            return true;
        }

        // 钙化（普通水
    if (!hasDistilledWater) {
            calcification += totalAmount;
        }

        waterTank.drain(totalAmount, IFluidHandler.FluidAction.EXECUTE);
        lastWaterInput = totalAmount;

        // 温度高于目标时，这部分降温消耗的水不计入蒸汽产量
        if (activeAmount <= 0) {
            cooldown(Math.min(-reqHeat, 0.2F));
            return true;
        }

        outputMB = activeAmount * STEAM_EXPANSION;

        Fluid outputFluidType;
        if (systemHeat >= 373.9999F) {
            outputFluidType = mio_icif_fluids.SUPERHEATEDSTEAM.get();
            outputFluid = OutputType.SUPERHEATEDSTEAM;
        } else {
            outputFluidType = mio_icif_fluids.STEAM.get();
            outputFluid = OutputType.STEAM;
        }

        // 1. 先填内部蒸汽
    FluidStack steamStack = new FluidStack(outputFluidType, outputMB);
        int filledInternal = steamTank.fill(steamStack, IFluidHandler.FluidAction.EXECUTE);
        int remainingOutput = outputMB - filledInternal;

        // 2. 内部满了才往外推
        int transferredToNeighbors = 0;
        if (remainingOutput > 0) {
            transferredToNeighbors = outputSteamToNeighbors(outputFluidType, remainingOutput);
            remainingOutput -= transferredToNeighbors;
        }

        // 3. 内外都满了才处理溢出
        if (remainingOutput > 0) {
            if (level.random.nextInt(10) == 0) {
                // 蒸汽溢出爆炸：产生破坏性爆
                level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 2.0F, Level.ExplosionInteraction.BLOCK);
            } else if (remainingOutput >= STEAM_EXPANSION) {
                waterTank.fill(new FluidStack(inputFluid, remainingOutput / STEAM_EXPANSION), IFluidHandler.FluidAction.EXECUTE);
            }
        }

        lastSteamOutput = filledInternal + transferredToNeighbors;
        return true;
    }

    private void heatup(float huInput) {
        if (huInput < 0) return;
        systemHeat += huInput * HEAT_PER_HU;
        if (systemHeat > MAX_SYSTEM_HEAT) {
            if (level != null) {
                level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 2.0F, Level.ExplosionInteraction.BLOCK);
                level.removeBlock(worldPosition, false);
            }
        }
    }

    private void cooldown(float cool) {
        if (cool < 0) return;
        // 简化：冷却到环境温度（假设 20°C），原版使用 BiomeUtil.getBiomeTemperature
        systemHeat = Math.max(systemHeat - cool, 20.0F);
    }

    private boolean isCalcified() {
        return calcification >= MAX_CALCIFICATION;
    }

    /**
     * 将指定流体输出到相邻容器（尝试所
6 个方向，原版 IC2 风格），返回实际传输
 */
    private int outputFluidToNeighbors(Fluid fluid, int amount) {
        if (level == null || amount <= 0) return 0;
        int totalTransferred = 0;
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(dir);
            IFluidHandler neighborHandler = level.getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                neighborPos, dir.getOpposite());
            if (neighborHandler != null) {
                FluidStack toDrain = new FluidStack(fluid, Math.min(amount - totalTransferred, 1000));
                int filled = neighborHandler.fill(toDrain, IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    totalTransferred += filled;
                    if (totalTransferred >= amount) return totalTransferred;
                }
            }
        }
        return totalTransferred;
    }

    /**
     * 将蒸汽输出到相邻流体容器，返回实际传输量
     */
    private int outputSteamToNeighbors(Fluid fluid, int amount) {
        return outputFluidToNeighbors(fluid, amount);
    }

    /**
     * 
tick 将内部蒸汽槽的蒸汽推送给相邻容器
     */
    private void outputStoredSteamToNeighbors() {
        if (level == null || level.isClientSide()) return;
        if (steamTank.getFluidAmount() <= 0) return;

        FluidStack available = steamTank.getFluid();
        if (available.isEmpty()) return;

        int toPush = Math.min(available.getAmount(), 1000); // 
        if (toPush <= 0) return;

        int transferred = outputSteamToNeighbors(available.getFluid(), toPush);
        if (transferred > 0) {
            steamTank.drain(transferred, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    private void updateBlockState(boolean working) {
        if (level == null || level.isClientSide()) return;
        BlockState state = getBlockState();
        if (state.hasProperty(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_steam_generator.LIT)) {
            level.setBlock(worldPosition, state.setValue(
                com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_steam_generator.LIT, working), 3);
        }
    }

    // ==================== Capability ====================

    public IItemHandler getItemHandlerCapability(@Nullable Direction side) {
        return itemHandler;
    }

    @Override
    public IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        return new CombinedFluidHandler(waterTank, steamTank);
    }

    @Override
    public IMioIcifCapabilities.IHeatStorage getHeatStorageCapability(@Nullable Direction side) {
        if (side == null) return heatStorage;
        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        if (side == facing) return null;
        return heatStorage;
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("waterTank", waterTank.writeToNBT(registries, new CompoundTag()));
        tag.put("steamTank", steamTank.writeToNBT(registries, new CompoundTag()));
        tag.put("items", itemHandler.serializeNBT(registries));
        tag.putInt("calcification", calcification);
        tag.putBoolean("isWorking", isWorking);
        tag.putFloat("systemHeat", systemHeat);
        tag.putInt("pressure", pressure);
        tag.putInt("inputMB", inputMB);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("waterTank")) waterTank.readFromNBT(registries, tag.getCompound("waterTank"));
        if (tag.contains("steamTank")) steamTank.readFromNBT(registries, tag.getCompound("steamTank"));
        if (tag.contains("items")) itemHandler.deserializeNBT(registries, tag.getCompound("items"));
        calcification = tag.getInt("calcification");
        isWorking = tag.getBoolean("isWorking");
        systemHeat = tag.getFloat("systemHeat");
        pressure = tag.getInt("pressure");
        inputMB = tag.getInt("inputMB");
    }

    // ==================== MenuProvider ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.steam_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Producer.SteamGeneratorMenu(containerId, playerInventory, this);
    }

    // ==================== Getters & Setters ====================

    public FluidTank getWaterTank() { return waterTank; }
    public FluidTank getSteamTank() { return steamTank; }
    public ItemStackHandler getItemHandler() { return itemHandler; }
    public ContainerData getContainerData() { return containerData; }
    public int getCalcification() { return calcification; }
    public int getMaxCalcification() { return MAX_CALCIFICATION; }
    public boolean isWorking() { return isWorking; }
    public int getPressure() { return pressure; }
    public int getInputMB() { return inputMB; }
    public int getOutputMB() { return outputMB; }

    // 上一 tick 输入输出速率（供 GUI 显示
public int getLastWaterInput() { return lastWaterInput; }
    public int getLastSteamOutput() { return lastSteamOutput; }
    public long getLastHeatInput() { return lastHeatInput; }

    /**
     * 系统热量温度（供 GUI 显示，IC2 风格，保
1 位小数）
     */
    public float getSystemHeat() { return Math.round(systemHeat * 10.0F) / 10.0F; }

    /**
     * 钙化百分比（
GUI 显示，保
2 位小数）
     */
    public float getCalcificationPercent() {
        return Math.round(calcification * 100.0F / MAX_CALCIFICATION * 100.0F) / 100.0F;
    }

    /**
     * 热量条缩放（原版 IC2 风格
 */
    public int gaugeHeatScaled(int i) {
        return (int)(i * systemHeat / MAX_SYSTEM_HEAT);
    }

    /**
     * 钙化条缩放（原版 IC2 风格
 */
    public int gaugeCalcificationScaled(int i) {
        return i * calcification / MAX_CALCIFICATION;
    }

    /**
     * 液体条缩放（原版 IC2 风格
 */
    public int gaugeLiquidScaled(int i, int tank) {
        if (tank == 0) {
            if (waterTank.getFluidAmount() <= 0) return 0;
            return waterTank.getFluidAmount() * i / waterTank.getCapacity();
        }
        return 0;
    }

    /**
     * 处理 GUI 按钮事件（原
IC2 压力/流量调节
 */
    public void onButtonEvent(int eventId) {
        if (eventId >= 2000) {
            // 左侧：压力调
        int delta = eventId - 2000;
            pressure = Math.min(300, Math.max(0, pressure + delta));
        } else if (eventId <= -2000) {
            // 左侧：压力减
        int delta = -(eventId + 2000);
            pressure = Math.min(300, Math.max(0, pressure - delta));
        } else if (eventId > 0) {
            // 右侧：流量增
        inputMB = Math.min(1000, inputMB + eventId);
        } else if (eventId < 0) {
            // 右侧：流量减
        inputMB = Math.max(0, inputMB + eventId);
        }
        setChanged();
    }

    /**
     * 当前输出流体名称的翻译键（原
IC2 风格，根
outputtyp
 */
    public String getOutputFluidTranslationKey() {
        switch (outputFluid) {
            case WATER: return "gui.mio_icif.steam_generator.output.water";
            case DISTILLEDWATER: return "gui.mio_icif.steam_generator.output.destiwater";
            case STEAM: return "gui.mio_icif.steam_generator.output.steam";
            case SUPERHEATEDSTEAM: return "gui.mio_icif.steam_generator.output.hotsteam";
            default: return "";
        }
    }

    /**
     * 组合流体处理
 */
    private static class CombinedFluidHandler implements IFluidHandler {
        private final FluidTank waterTank;
        private final FluidTank steamTank;

        public CombinedFluidHandler(FluidTank waterTank, FluidTank steamTank) {
            this.waterTank = waterTank;
            this.steamTank = steamTank;
        }

        @Override
        public int getTanks() { return 2; }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return tank == 0 ? waterTank.getFluid() : steamTank.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? waterTank.getCapacity() : steamTank.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return tank == 0 ? waterTank.isFluidValid(stack) : steamTank.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (waterTank.isFluidValid(resource)) {
                return waterTank.fill(resource, action);
            }
            return 0;
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            return steamTank.drain(resource, action);
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            return steamTank.drain(maxDrain, action);
        }
    }
}