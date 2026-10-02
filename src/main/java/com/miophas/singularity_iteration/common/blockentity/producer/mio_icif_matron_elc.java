package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.api.upgrade.tile.UpgradableProperty;
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

import java.util.EnumSet;
import java.util.Set;

/** IC2 Experimental agriculture machine; cursor and resource tanks survive reloads. */
@SuppressWarnings("null")
public class mio_icif_matron_elc extends AbstractProcessingMachineBlockEntity implements com.miophas.singularity_iteration.common.area.WorkAreaProvider {

    // 对齐 1.7.10 TileEntityCropmatron：肥料 7 格 + 除草剂容器 7 格 + 水容器输入/输出各 1 格。
    // 1.7.10 原版不支持升级；此处升级槽 4 格与电池槽 1 格均为本项目有意保留的扩展。
    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .battery()
        .extra(1)
        .extra(1)
        .extra(7)
        .extra(7)
        .upgrade(4)
        .build();
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_WATER_CELL_INPUT = 1;
    public static final int SLOT_WATER_CELL_OUTPUT = 2;
    public static final int SLOT_FERTILIZER_START = 3;
    public static final int SLOT_FERTILIZER_COUNT = 7;
    public static final int SLOT_WEEDEX_START = 10;
    public static final int SLOT_WEEDEX_COUNT = 7;
    public static final int SLOT_UPGRADE_START = 17;
    public static final int TOTAL_SLOTS = 21;
    public static final long DEFAULT_CAPACITY = 10000L; // IC2 2.8 Cropmatron: 10 000 EU, tier 1
    public static final long DEFAULT_MAX_RECEIVE = 32L;
    public static final long DEFAULT_MAX_EXTRACT = 0L;
    public static final int DEFAULT_WORK_TIME = 10;
    public static final long DEFAULT_ENERGY_PER_TICK = 1L;
    public static final int WATER_CAPACITY = 2000;
    // 对齐 IC2 2.8 自动施用 applyWeedEx：一次补到 150（手动施用上限为 100）
    public static final int WEED_CONTROL_LIMIT = 150;
    public static final int HERBICIDE_VALUE = 50;
    public static final long MIN_START_ENERGY = 31L;
    public static final int SCAN_RADIUS = com.miophas.singularity_iteration.core.runtime.crop.CropScanCursor.RADIUS;
    public static final int SCAN_VOLUME = com.miophas.singularity_iteration.core.runtime.crop.CropScanCursor.VOLUME;
    protected final FluidTank waterTank;
    protected final FluidTank weedExTank;
    private final IFluidHandler fluidPorts;
    private int scanIndex;
    public FluidTank getWeedExTank() { return weedExTank; }


    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) energyStorage.getAmount();
                case 1 -> (int) energyStorage.getCapacity();
                case 2 -> waterTank.getFluidAmount();
                case 3 -> waterTank.getCapacity();
                case 4 -> weedExTank.getFluidAmount();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return 5;
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
            CableTier.LV);

        this.waterTank = new FluidTank(WATER_CAPACITY, fluidStack -> 
            fluidStack.getFluid().isSame(net.minecraft.world.level.material.Fluids.WATER)) {
                @Override protected void onContentsChanged() { setChanged(); }
            };
        this.weedExTank = new FluidTank(2000, fluid -> fluid.getFluid().isSame(
            com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids.WEED_EX.get())) {
                @Override protected void onContentsChanged() { setChanged(); }
            };
        this.fluidPorts = new com.miophas.singularity_iteration.core.prefab.fluid.FluidTankGroup(java.util.List.of(
            new com.miophas.singularity_iteration.core.prefab.fluid.FluidTankGroup.Tank(waterTank, com.miophas.singularity_iteration.core.prefab.fluid.FluidTankGroup.Role.INPUT_ONLY),
            new com.miophas.singularity_iteration.core.prefab.fluid.FluidTankGroup.Tank(weedExTank, com.miophas.singularity_iteration.core.prefab.fluid.FluidTankGroup.Role.INPUT_ONLY)));
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
        return new com.miophas.singularity_iteration.common.menu.producer.MatronElcMenu(
            containerId, playerInventory, this);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return isBattery(stack);
        if (slot == SLOT_WATER_CELL_INPUT) return isWaterCell(stack);
        if (slot == SLOT_WATER_CELL_OUTPUT) return false;
        if (slot >= SLOT_FERTILIZER_START && slot < SLOT_FERTILIZER_START + SLOT_FERTILIZER_COUNT) {
            return isFertilizerItem(stack);
        }
        if (slot >= SLOT_WEEDEX_START && slot < SLOT_WEEDEX_START + SLOT_WEEDEX_COUNT) {
            return isWeedExCell(stack);
        }
        if (slot >= SLOT_UPGRADE_START && slot < TOTAL_SLOTS) {
            return isSuitableUpgrade(stack);
        }
        return false;
    }

    /** 扩展：升级插件必须声明适用于本机属性（1.7.10 原版监护机无升级槽）。 */
    private boolean isSuitableUpgrade(ItemStack stack) {
        return stack.getItem() instanceof com.miophas.singularity_iteration.common.item.upgrade.mio_icif_upgrade upgrade
            && upgrade.isSuitableFor(stack, getUpgradableProperties());
    }

    /** 扩展：监护机可用升级类型（沿用 1.12.2 原版属性集）。 */
    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.TRANSFORMER,
            UpgradableProperty.ENERGY_STORAGE,
            UpgradableProperty.ITEM_CONSUMING,
            UpgradableProperty.FLUID_CONSUMING);
    }

    /** 对齐原版：仅接受专用作物肥料物品，不接受骨粉/通用肥料标签。 */
    private boolean isFertilizerItem(ItemStack stack) {
        return com.miophas.singularity_iteration.common.item.resource.MatronFertilizerItem.isFertilizer(stack);
    }

    @Override
    protected int[] getSlotsForDirection(Direction side) {
        // 对齐原版：升级槽不对自动化开放
        int[] slots = new int[SLOT_UPGRADE_START];
        for (int i = 0; i < SLOT_UPGRADE_START; i++) slots[i] = i;
        return slots;
    }

    @Override
    protected boolean canInsertItem(int slot, ItemStack stack, @Nullable Direction side) {
        boolean topOnly = slot == SLOT_WATER_CELL_INPUT
            || (slot >= SLOT_FERTILIZER_START && slot < SLOT_FERTILIZER_START + SLOT_FERTILIZER_COUNT)
            || (slot >= SLOT_WEEDEX_START && slot < SLOT_WEEDEX_START + SLOT_WEEDEX_COUNT);
        if (topOnly) {
            // 对齐原版 InvSlot.InvSide.TOP：肥料/除草剂/水容器仅可从顶面自动化输入
            return side == Direction.UP && isItemValidForSlot(slot, stack);
        }
        return super.canInsertItem(slot, stack, side);
    }

    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        return slot == SLOT_WATER_CELL_OUTPUT;
    }

    @Override
    protected int getBatterySlot() {
        return SLOT_BATTERY;
    }

    @Override
    protected boolean canWork() {
        return energyStorage.getAmount() >= 31;
    }

    @Override
    protected void doWork() {
    }

    private boolean isWaterCell(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(net.minecraft.world.item.Items.WATER_BUCKET)) return true;
        return mio_icif_cells.isCellContainingFluid(stack, net.minecraft.world.level.material.Fluids.WATER);
    }

    private boolean isWeedExCell(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var handler = stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM);
        return handler != null && !handler.drain(new FluidStack(
            com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids.WEED_EX.get(),
            Integer.MAX_VALUE), IFluidHandler.FluidAction.SIMULATE).isEmpty();
    }

    @SuppressWarnings("unused")
    private boolean isEmptyCell(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(net.minecraft.world.item.Items.BUCKET)) return true;
        return mio_icif_cells.isEmptyCell(stack);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_matron_elc blockEntity) {
        if (level.isClientSide()) {
            return;
        }
        AbstractProcessingMachineBlockEntity.tick(level, pos, state, blockEntity);
        blockEntity.processWaterCell();
        blockEntity.processWeedExCell();
        // 对齐 1.7.10：每 tick 扫描一次，门槛 31 EU
        if (!blockEntity.canWork()) {
            return;
        }
        blockEntity.processCrops();
    }

    private void processWaterCell() {
        processFluidContainer(SLOT_WATER_CELL_INPUT, waterTank, SLOT_WATER_CELL_OUTPUT);
    }

    private void processWeedExCell() {
        // 除草剂为耐久型容器，耗尽后直接消失，无需空容器输出槽
        for (int i = 0; i < SLOT_WEEDEX_COUNT; i++) {
            if (processFluidContainer(SLOT_WEEDEX_START + i, weedExTank, -1)) break;
        }
    }

    private boolean processFluidContainer(int slot, FluidTank tank, int outputSlot) {
        ItemStack input = itemHandler.getStackInSlot(slot);
        if (input.isEmpty()) return false;
        ItemStack working = input.copyWithCount(1);
        var handler = working.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM);
        if (handler == null) return false;
        FluidStack offered = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        int amount = tank.fill(offered, IFluidHandler.FluidAction.SIMULATE);
        if (amount <= 0) return false;
        FluidStack actual = handler.drain(offered.copyWithAmount(amount), IFluidHandler.FluidAction.EXECUTE);
        if (actual.isEmpty() || actual.getAmount() > amount) return false;
        ItemStack result = handler.getContainer();
        boolean retain = !result.isEmpty() && result.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM) != null
            && !result.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM).drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE).isEmpty();
        ItemStack output = outputSlot >= 0 ? itemHandler.getStackInSlot(outputSlot) : ItemStack.EMPTY;
        if (!retain && !result.isEmpty() && !output.isEmpty()
                && (!ItemStack.isSameItemSameComponents(output, result) || output.getCount() + result.getCount() > output.getMaxStackSize())) return false;
        if (retain && input.getCount() > 1) return false;
        tank.fill(actual, IFluidHandler.FluidAction.EXECUTE);
        if (retain) itemHandler.setStackInSlot(slot, result);
        else {
            input.shrink(1);
            if (!result.isEmpty() && outputSlot >= 0) {
                if (output.isEmpty()) itemHandler.setStackInSlot(outputSlot, result);
                else output.grow(result.getCount());
            }
        }
        setChanged();
        return true;
    }

    private void processCrops() {
        Level world = getLevel();
        if (world == null || energyStorage.getAmount() < MIN_START_ENERGY) return;
        scanIndex = com.miophas.singularity_iteration.core.runtime.crop.CropScanCursor.next(scanIndex);
        apiUseEnergy(1, false);
        setChanged();
        // 水平 ±5（11 格）、垂直 -1..1（3 层）：与原版 scanX/scanY/scanZ 相同的推进顺序
        BlockPos target = com.miophas.singularity_iteration.core.runtime.crop.CropScanCursor.target(worldPosition, scanIndex);
        boolean worked = false;
        if (world.hasChunkAt(target)) {
            if (world.getBlockEntity(target) instanceof com.miophas.singularity_iteration.core.api.crop.IPlanter crop) {
                if (tryFertilize(crop)) { apiUseEnergy(10, false); worked = true; }
                if (tryHydrate(crop)) { apiUseEnergy(10, false); worked = true; }
                if (tryRemoveWeed(crop)) { apiUseEnergy(10, false); worked = true; }
            } else if (tryHydrateFarmland(world, target)) { apiUseEnergy(10, false); worked = true; }
        }
        updateWorkingState(world, worldPosition, worked);
    }

    private void updateWorkingState(Level level, BlockPos pos, boolean working) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof com.miophas.singularity_iteration.common.block.producer.mio_icif_block_matron) {
            boolean currentWorking = state.getValue(com.miophas.singularity_iteration.common.block.producer.mio_icif_block_matron.WORKING);
            if (currentWorking != working) {
                level.setBlock(pos, state.setValue(com.miophas.singularity_iteration.common.block.producer.mio_icif_block_matron.WORKING, working), 3);
            }
        }
    }

    private boolean tryHydrateFarmland(Level level, BlockPos center) {
        BlockState state = level.getBlockState(center);
        if (!state.is(net.minecraft.world.level.block.Blocks.FARMLAND)) return false;
        int moisture = state.getValue(net.minecraft.world.level.block.FarmBlock.MOISTURE);
        int amount = Math.min(waterTank.getFluidAmount(), 7 - moisture);
        if (amount <= 0 || !level.setBlock(center, state.setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE, moisture + amount), 2)) return false;
        waterTank.drain(amount, IFluidHandler.FluidAction.EXECUTE);
        return true;
    }

    private boolean tryRemoveWeed(com.miophas.singularity_iteration.core.api.crop.IPlanter cropEntity) {
        // IC2 2.8 automated application: top the crop's Weed-EX storage up to the
        // automated cap in one operation (manual application caps lower). The
        // previous fixed +50 step followed 1.7.10 and needed three scans.
        int missing = WEED_CONTROL_LIMIT - cropEntity.getWeedControl();
        if (missing <= 0) return false;
        FluidStack drained = weedExTank.drain(missing, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) return false;
        cropEntity.setWeedControl(cropEntity.getWeedControl() + drained.getAmount());
        cropEntity.updateState();
        return true;
    }

    private boolean tryFertilize(com.miophas.singularity_iteration.core.api.crop.IPlanter cropEntity) {
        if (cropEntity.getNutrients() >= 100) {
            return false;
        }
        for (int i = 0; i < SLOT_FERTILIZER_COUNT; i++) {
            int slot = SLOT_FERTILIZER_START + i;
            ItemStack fertilizerStack = itemHandler.getStackInSlot(slot);
            if (!fertilizerStack.isEmpty() && isFertilizerItem(fertilizerStack)) {
                cropEntity.setNutrients(cropEntity.getNutrients() + 90);
                cropEntity.updateState();
                fertilizerStack.shrink(1);
                if (fertilizerStack.isEmpty()) {
                    itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
                }
                return true;
            }
        }
        return false;
    }

    private boolean tryHydrate(com.miophas.singularity_iteration.core.api.crop.IPlanter cropEntity) {
        if (cropEntity.getWater() >= 200) {
            return false;
        }
        int waterNeeded = 200 - cropEntity.getWater();
        FluidStack drained = waterTank.drain(waterNeeded, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty() || drained.getAmount() <= 0) {
            return false;
        }
        cropEntity.setWater(cropEntity.getWater() + drained.getAmount());
        cropEntity.updateState();
        return true;
    }

    @Override
    public net.neoforged.neoforge.fluids.capability.IFluidHandler getFluidHandlerCapability(@Nullable Direction direction) {
        return fluidPorts;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("WaterTank", waterTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("CropScanIndex", scanIndex);
        tag.put("WeedExTank", weedExTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt(SLOT_LAYOUT_KEY, SLOT_LAYOUT_VERSION);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("WaterTank")) {
            waterTank.readFromNBT(registries, tag.getCompound("WaterTank"));
        }
        scanIndex = com.miophas.singularity_iteration.core.runtime.crop.CropScanCursor.restore(tag.getInt("CropScanIndex"));
        if (tag.contains("WeedExTank")) weedExTank.readFromNBT(registries, tag.getCompound("WeedExTank"));
        if (tag.getInt(SLOT_LAYOUT_KEY) < SLOT_LAYOUT_VERSION) migrateLegacySlotLayout();
    }

    /*
     * Saves up to 0.1.7.14 kept Weed-EX containers in slots 3-9 and fertilizer in
     * 10-16; 0.1.7.15 swapped the groups (IC2 order: fertilizer first) without a
     * migration, so upgraded machines silently ignored both. An unmarked save
     * whose contents only fit the old arrangement has the two groups swapped back.
     */
    private static final String SLOT_LAYOUT_KEY = "MatronSlotLayout";
    private static final int SLOT_LAYOUT_VERSION = 2;

    private void migrateLegacySlotLayout() {
        boolean misplaced = false;
        for (int i = 0; i < SLOT_FERTILIZER_COUNT && !misplaced; i++) {
            ItemStack inFertilizerSlot = itemHandler.getStackInSlot(SLOT_FERTILIZER_START + i);
            ItemStack inWeedExSlot = itemHandler.getStackInSlot(SLOT_WEEDEX_START + i);
            misplaced = (!inFertilizerSlot.isEmpty() && !isFertilizerItem(inFertilizerSlot))
                || (!inWeedExSlot.isEmpty() && !isWeedExCell(inWeedExSlot));
        }
        if (!misplaced) return;
        for (int i = 0; i < SLOT_FERTILIZER_COUNT; i++) {
            ItemStack a = itemHandler.getStackInSlot(SLOT_FERTILIZER_START + i).copy();
            ItemStack b = itemHandler.getStackInSlot(SLOT_WEEDEX_START + i).copy();
            itemHandler.setStackInSlot(SLOT_FERTILIZER_START + i, b);
            itemHandler.setStackInSlot(SLOT_WEEDEX_START + i, a);
        }
    }

    @Override
    public java.util.List<com.miophas.singularity_iteration.common.area.WorkArea> workAreas() {
        int r = com.miophas.singularity_iteration.core.runtime.crop.CropScanCursor.RADIUS;
        return java.util.List.of(com.miophas.singularity_iteration.common.area.WorkArea.box(worldPosition, r, 1, 1, r, com.miophas.singularity_iteration.common.area.WorkArea.FARM));
    }
}
