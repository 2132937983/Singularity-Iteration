package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Block;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
import com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_vent;
import com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_reactors;
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
 * 冷凝机方块实
 * 将多余的蒸汽冷却为蒸馏水，防止蒸汽引发爆
 *
 * 基础逻辑
 * - 接收蒸汽（IC2 蒸汽或过热蒸汽）
 * - 
tick 将一定量的蒸汽冷凝为蒸馏
 * - 可安装散热片加速（TODO），此时需
EU 供电
 */
@SuppressWarnings("null")
public class mio_icif_condenser extends mio_icif_producer {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .extra(4)
        .input(1)
        .output(1)
        .battery()
        .upgrade(1)
        .build();

    // 槽位定义（与 IC2 ContainerCondenser 一致，并补充左侧电池槽
    
    public static final int TOTAL_SLOTS = 8;
    public static final int VENT_SLOT_1 = 0;       // 散热片槽 1 (26, 26)
    public static final int VENT_SLOT_2 = 1;       // 散热片槽 2 (26, 44)
    public static final int VENT_SLOT_3 = 2;       // 散热片槽 3 (134, 26)
    public static final int VENT_SLOT_4 = 3;       // 散热片槽 4 (134, 44)
    public static final int INPUT_BUCKET_SLOT = 4; // 蒸汽桶输入槽 (26, 73)
    public static final int EMPTY_BUCKET_SLOT = 5; // 空容器返还槽 (134, 73)
    public static final int BATTERY_SLOT = 6;      // 电池
    public static final int UPGRADE_SLOT = 7;      // 升级

    // 流体容量
    public static final int STEAM_TANK_CAPACITY = 10000;      // 蒸汽
    public static final int DISTILLED_TANK_CAPACITY = 10000;  // 蒸馏水槽 10 L
    // 工作参数
    public static final int BASE_CONDENSE_RATE = 10; // 基础冷凝速率 mB/tick
    public static final long ENERGY_PER_TICK = 1; // 每个散热片每 tick 消
    public static final int MAX_PROGRESS = 20; // 进度条循环周期（仅视觉效果，不影响实际产量）

    // 散热片加成（mB/tick
    // 参考原版IC2：每个散热片提供相同的加
    public static final int VENT_BONUS = 10;

    // 流体存储
    protected final FluidTank steamTank;
    protected final FluidTank distilledTank;

    private int progress = 0;

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> isWorking ? 1 : 0;
                case 1 -> (int) energyStorage.getAmount();
                case 2 -> (int) energyStorage.getCapacity();
                case 3 -> progress;
                case 4 -> steamTank.getFluidAmount();
                case 5 -> distilledTank.getFluidAmount();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() { return 6; }
    };

    public mio_icif_condenser(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.CONDENSER_ENTITY_TYPE.get());
    }

    public mio_icif_condenser(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type, 100000, 512, 0, 1, LAYOUT, ENERGY_PER_TICK, CableTier.HV);

        this.steamTank = new FluidTank(STEAM_TANK_CAPACITY, fluidStack -> {
            if (fluidStack.isEmpty()) return true;
            return fluidStack.getFluid() == mio_icif_fluids.STEAM.get()
                || fluidStack.getFluid() == mio_icif_fluids.SUPERHEATEDSTEAM.get();
        });

        this.distilledTank = new FluidTank(DISTILLED_TANK_CAPACITY, fluidStack -> {
            if (fluidStack.isEmpty()) return true;
            return fluidStack.getFluid() == mio_icif_fluids.DISTILLEDWATER.get();
        });
    }



    /**
     * 
tick 更新逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_condenser blockEntity) {
        if (level.isClientSide()) return;

        blockEntity.recalculateUpgradeStats();

        mio_icif_Energy_Block.tick(level, pos, state, blockEntity);

        blockEntity.handleSteamBucketSlot();

        if (!blockEntity.canWorkRedstone()) {
            blockEntity.isWorking = false;
        } else {
            boolean wasWorking = blockEntity.isWorking;
            blockEntity.isWorking = blockEntity.canCondense();

            if (blockEntity.isWorking) {
                blockEntity.condenseSteam();
                blockEntity.consumeEnergy();
                blockEntity.addHeatToVents();
                blockEntity.progress = (blockEntity.progress + 1) % MAX_PROGRESS;
            } else {
                blockEntity.progress = 0;
            }

            if (wasWorking != blockEntity.isWorking) {
                blockEntity.updateBlockState(blockEntity.isWorking);
            }
        }

        blockEntity.handleBatterySlot();
        blockEntity.handleAutomationUpgrades();

        blockEntity.setChanged();
    }

    /**
     * 检查是否可以工
     */
    private boolean canCondense() {
        int rate = getCondenseRate();
        long energyCost = getEnergyCost();
        
        // 没有散热片时不消耗能量，有散热片时需要足够的EU
        if (energyCost > 0 && energyStorage.getAmount() < energyCost) {
            return false;
        }
        
        return rate > 0
            && steamTank.getFluidAmount() >= rate
            && distilledTank.getFluidAmount() + rate <= distilledTank.getCapacity();
    }

    /**
     * 冷凝蒸汽
     */
    private void condenseSteam() {
        int rate = getCondenseRate();
        if (rate <= 0) return;

        FluidStack drained = steamTank.drain(rate, IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() >= rate) {
            distilledTank.fill(new FluidStack(mio_icif_fluids.DISTILLEDWATER.get(), rate), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    /**
     * 计算当前冷凝速率（基础 + 散热片加成）
     * 参考原版IC2冷凝机逻辑
     * - 基础冷凝速率
0 mB/tick
     * - 每个散热片额外提
10 mB/tick 加成
     * - 有散热片时才消耗EU（每个散热片 1 EU/tick
     */
    private int getCondenseRate() {
        int ventCount = getVentCount();
        return BASE_CONDENSE_RATE + ventCount * VENT_BONUS;
    }

    /**
     * 获取散热片数量（统计4个散热片槽位中有物品的数量）
     * 参考原版IC2的getVents()方法
     */
    private int getVentCount() {
        int count = 0;
        if (!itemHandler.getStackInSlot(VENT_SLOT_1).isEmpty()) count++;
        if (!itemHandler.getStackInSlot(VENT_SLOT_2).isEmpty()) count++;
        if (!itemHandler.getStackInSlot(VENT_SLOT_3).isEmpty()) count++;
        if (!itemHandler.getStackInSlot(VENT_SLOT_4).isEmpty()) count++;
        return count;
    }

    /**
     * 获取当前tick应消耗的EU数量
     * 参考原版IC2：每个散热片消
2 EU（这里调整为 1 EU以适配平衡性）
     */
    private long getEnergyCost() {
        int ventCount = getVentCount();
        return ventCount > 0 ? ventCount * ENERGY_PER_TICK : 0;
    }

    /**
     * 消耗能量（仅在有散热片时）
     * 覆盖父类方法，使用冷凝机特有的能量消耗逻辑
     */
    @Override
    protected boolean consumeEnergy() {
        long energyCost = getEnergyCost();
        if (energyCost > 0 && energyStorage.getAmount() >= energyCost) {
            apiUseEnergy(energyCost, false);
            return true;
        }
        return energyCost == 0; // 没有散热片时不需要消耗能
   
         }

    /**
     * 机器正常工作时，对散热片增加热量
     * 每个散热片每 tick 增加 1 点热
     */
    private void addHeatToVents() {
        for (int i = 0; i < 4; i++) {
            int slot = VENT_SLOT_1 + i;
            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof mio_icif_heat_vent heatVent) {
                heatVent.addHeat(stack, 1);
            }
        }
    }


    /**
     * 处理蒸汽输入槽：将蒸汽桶/蒸汽单元中的蒸汽转入蒸汽槽，空容器返还到空容器槽
     */
    private void handleSteamBucketSlot() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_BUCKET_SLOT);
        if (input.isEmpty()) return;

        int fillAmount = 1000;

        // 蒸汽
        
        if (input.is(mio_icif_fluids.STEAM_BUCKET.get())) {
            if (!canFillSteam(fillAmount)) return;
            if (!canInsertOrMerge(EMPTY_BUCKET_SLOT, new ItemStack(net.minecraft.world.item.Items.BUCKET))) return;

            fillSteam(fillAmount);
            input.shrink(1);
            insertOrMerge(EMPTY_BUCKET_SLOT, new ItemStack(net.minecraft.world.item.Items.BUCKET));
            setChanged();
        }
        // 过热蒸汽
        
        else if (input.is(mio_icif_fluids.SUPERHEATEDSTEAM_BUCKET.get())) {
            if (!canFillSteam(fillAmount)) return;
            if (!canInsertOrMerge(EMPTY_BUCKET_SLOT, new ItemStack(net.minecraft.world.item.Items.BUCKET))) return;

            fillSteam(fillAmount);
            input.shrink(1);
            insertOrMerge(EMPTY_BUCKET_SLOT, new ItemStack(net.minecraft.world.item.Items.BUCKET));
            setChanged();
        }
        // 蒸汽单元
        else if (mio_icif_cells.isCellContainingFluid(input, mio_icif_fluids.STEAM.get())) {
            if (!canFillSteam(fillAmount)) return;
            ItemStack emptyCell = mio_icif_cells.getEmptyCellForStack(input);
            if (emptyCell.isEmpty()) emptyCell = new ItemStack(mio_icif_cells.CELL_EMPTY.get());
            if (!canInsertOrMerge(EMPTY_BUCKET_SLOT, emptyCell)) return;

            fillSteam(fillAmount);
            input.shrink(1);
            insertOrMerge(EMPTY_BUCKET_SLOT, emptyCell);
            setChanged();
        }
        else if (mio_icif_cells.isCellContainingFluid(input, mio_icif_fluids.SUPERHEATEDSTEAM.get())) {
            if (!canFillSteam(fillAmount)) return;
            ItemStack emptyCell = mio_icif_cells.getEmptyCellForStack(input);
            if (emptyCell.isEmpty()) emptyCell = new ItemStack(mio_icif_cells.CELL_EMPTY.get());
            if (!canInsertOrMerge(EMPTY_BUCKET_SLOT, emptyCell)) return;

            fillSteam(fillAmount);
            input.shrink(1);
            insertOrMerge(EMPTY_BUCKET_SLOT, emptyCell);
            setChanged();
        }
    }

    private boolean canFillSteam(int amount) {
        return steamTank.getFluidAmount() + amount <= steamTank.getCapacity();
    }

    private void fillSteam(int amount) {
        steamTank.fill(new FluidStack(mio_icif_fluids.STEAM.get(), amount), IFluidHandler.FluidAction.EXECUTE);
    }

    private boolean canInsertOrMerge(int slot, ItemStack stack) {
        ItemStack current = itemHandler.getStackInSlot(slot);
        if (current.isEmpty()) return true;
        if (ItemStack.isSameItemSameComponents(current, stack)) {
            return current.getCount() + stack.getCount() <= itemHandler.getSlotLimit(slot);
        }
        return false;
    }

    private void insertOrMerge(int slot, ItemStack stack) {
        ItemStack current = itemHandler.getStackInSlot(slot);
        if (current.isEmpty()) {
            itemHandler.setStackInSlot(slot, stack);
        } else if (ItemStack.isSameItemSameComponents(current, stack)) {
            current.grow(stack.getCount());
        }
    }

    private void updateBlockState(boolean working) {
        if (level == null || level.isClientSide()) return;
        BlockState state = getBlockState();
        if (state.hasProperty(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_condenser.LIT)) {
            level.setBlock(worldPosition, state.setValue(
                com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_condenser.LIT, working), 3);
        }
    }

    @Override
    public boolean isItemValidForSlot(int slot, net.minecraft.world.item.ItemStack stack) {
        return switch (slot) {
            case INPUT_BUCKET_SLOT -> isSteamContainer(stack);
            case EMPTY_BUCKET_SLOT -> false;
            case BATTERY_SLOT -> isBattery(stack);
            case VENT_SLOT_1, VENT_SLOT_2, VENT_SLOT_3, VENT_SLOT_4 -> isHeatVent(stack);
            case UPGRADE_SLOT -> stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Upgrade.mio_icif_upgrade;
            default -> true;
        };
    }

    private boolean isSteamContainer(ItemStack stack) {
        return stack.is(mio_icif_fluids.STEAM_BUCKET.get())
            || stack.is(mio_icif_fluids.SUPERHEATEDSTEAM_BUCKET.get())
            || mio_icif_cells.isCellContainingAnyFluid(stack, mio_icif_fluids.STEAM.get(), mio_icif_fluids.SUPERHEATEDSTEAM.get());
    }

    private boolean isHeatVent(ItemStack stack) {
        return stack.is(mio_icif_reactors.VENT.get())
            || stack.is(mio_icif_reactors.DIAMOND_VENT.get())
            || stack.is(mio_icif_reactors.VENT_SPREAD.get())
            || stack.is(mio_icif_reactors.VENT_CORE.get())
            || stack.is(mio_icif_reactors.OVERCLOCKED_HEAT_VENT.get());
    }

    @Override
    protected int[] getSlotsForDirection(Direction side) {
        return new int[]{VENT_SLOT_1, VENT_SLOT_2, VENT_SLOT_3, VENT_SLOT_4, INPUT_BUCKET_SLOT, EMPTY_BUCKET_SLOT, BATTERY_SLOT};
    }

    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        return slot == EMPTY_BUCKET_SLOT;
    }

    @Override
    protected int getBatterySlot() {
        return BATTERY_SLOT;
    }

    @Override
    protected boolean canInsertItem(int slot, net.minecraft.world.item.ItemStack stack, @Nullable Direction side) {
        if (!isItemValidForSlot(slot, stack)) return false;
        if (slot == BATTERY_SLOT) return isBattery(stack);
        if (slot == UPGRADE_SLOT) return false; // 升级槽不允许自动化插入（与IC2原版一致）
        return true;
    }

    @Override
    protected void doWork() {
        // 工作逻辑
    
    }

    @Override
    protected boolean canWork() {
        return canCondense();
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("steamTank", steamTank.writeToNBT(registries, new CompoundTag()));
        tag.put("distilledTank", distilledTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("progress", progress);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("steamTank")) steamTank.readFromNBT(registries, tag.getCompound("steamTank"));
        if (tag.contains("distilledTank")) distilledTank.readFromNBT(registries, tag.getCompound("distilledTank"));
        progress = tag.getInt("progress");
    }

    // ==================== Capability ====================

    @Override
    public IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        return new CombinedFluidHandler(steamTank, distilledTank);
    }

    public FluidTank getSteamTank() { return steamTank; }
    public FluidTank getDistilledTank() { return distilledTank; }
    public int getProgress() { return progress; }
    public int getMaxProgress() { return MAX_PROGRESS; }

    private static class CombinedFluidHandler implements IFluidHandler {
        private final FluidTank steamTank;
        private final FluidTank distilledTank;

        public CombinedFluidHandler(FluidTank steamTank, FluidTank distilledTank) {
            this.steamTank = steamTank;
            this.distilledTank = distilledTank;
        }

        @Override
        public int getTanks() { return 2; }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? steamTank.getFluid() : distilledTank.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? steamTank.getCapacity() : distilledTank.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 ? steamTank.isFluidValid(stack) : distilledTank.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (steamTank.isFluidValid(resource)) {
                return steamTank.fill(resource, action);
            }
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return distilledTank.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return distilledTank.drain(maxDrain, action);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.condenser");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Producer.CondenserMenu(id, playerInventory, this);
    }

    public ContainerData getContainerData() { return containerData; }
}