package com.miophas.singularity_iteration.common.blockentity.huentity.hugenerator;

import com.miophas.singularity_iteration.common.blockentity.generator.SemifluidFuels;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 流体加热机
 * 通过燃烧沼气液体来产生热能
 */
@SuppressWarnings("null")
public class mio_icif_fluid_heat_generator extends com.miophas.singularity_iteration.core.prefab.blockentity.AbstractHeatBlockEntity implements WorldlyContainer {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .extra(1)
        .output(1)
        .build();

    public static final int FUEL_BUCKET_SLOT = 0;
    public static final int EMPTY_BUCKET_SLOT = 1;
    public static final int TOTAL_SLOTS = 2;


    public static final int FUEL_CAPACITY = 10000; // 对齐原版 IC2 流体加热机储罐容量 10000 mB
    public static final int FUEL_PER_BUCKET = 1000;
    public static final int HU_PER_BUCKET = 64000;

    // Uses the inherited inventory; no shadow copy.
    protected final FluidTank fuelTank;

    private int burnTime = 0;
    private long scexHeatCredit;
    private int maxBurnTime = 0;
    private boolean isWorking = false;

    /**
     * 最近一次**实际输出**的 HU/t。对齐 IC2 {@code TileEntityHeatSourceInventory#transmitHeat}：
     * 只在真的输出成功时更新，没有输出时保持上一次读数，原版 GUI 显示的正是它。
     */
    private int transmitHeat = 0;
    /** 当前燃料的产热能力（HU/t），无燃料为 0；供 GUI 显示输出上限。 */
    private int currentHeatOutput = 0;

    private int currentFuelBurning = 0;
    private int currentFuelHeatGenerated = 0;

    public mio_icif_fluid_heat_generator(BlockPos pos, BlockState state) {
        super(mio_icif_block_entities.FLUID_HEAT_GENERATOR.get(), pos, state, 1, 0, 0, 20, 1000, 0);

        this.slotLayout = LAYOUT;
        this.itemHandler = createItemHandler(LAYOUT);
        this.itemHandler.setValidator((slot, stack, slotType) -> mio_icif_fluid_heat_generator.this.isItemValidForSlot(slot, stack));

        this.fuelTank = new FluidTank(FUEL_CAPACITY, fluidStack ->
            SemifluidFuels.accepts(fluidStack.getFluid())) {
                @Override protected void onContentsChanged() { com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank.markUnsaved(mio_icif_fluid_heat_generator.this); }
            };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_fluid_heat_generator blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        blockEntity.handleFuelBucketSlot();
        blockEntity.burnFluid();

        boolean wasActive = state.getValue(com.miophas.singularity_iteration.common.block.hugenerator.mio_icif_block_fluid_heat_generator.ACTIVE);
        boolean shouldBeActive = blockEntity.isWorking;

        if (wasActive != shouldBeActive) {
            BlockState newState = state.setValue(com.miophas.singularity_iteration.common.block.hugenerator.mio_icif_block_fluid_heat_generator.ACTIVE, shouldBeActive);
            level.setBlock(pos, newState, 3);
        }
    }

    private void scexFillFuel() {
        var input = itemHandler.getStackInSlot(FUEL_BUCKET_SLOT);
        FluidStack content = fuelContentOf(input);
        if (content == null) return;
        ItemStack empty = emptyContainerFor(input);
        if (empty.isEmpty()) return;
        if (com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank.transfer(itemHandler, FUEL_BUCKET_SLOT, EMPTY_BUCKET_SLOT, fuelTank, content, empty)) setChanged();
    }

    private void burnFluid() {
        long previousCredit = scexHeatCredit;
        boolean previousWorking = isWorking;
        FluidStack fuel = fuelTank.getFluid();
        SemifluidFuels.HeatValue value = fuel.isEmpty() ? null : SemifluidFuels.heatValue(fuel.getFluid());
        if (value == null) {
            isWorking = false;
            burnTime = 0;
            transmitHeat = 0;
            currentHeatOutput = 0;
            if (previousWorking || previousCredit != 0) {
                scexHeatCredit = 0;
                setChanged();
            }
            return;
        }
        // 对齐原版 IC2：燃烧值取自燃料表，速率 = heatPerTick；燃烧缓冲达到速率时不再耗液。
        long rate = value.heatPerTick();
        if (scexHeatCredit < rate && fuelTank.getFluidAmount() > 0) {
            if (fuelTank.drain(1, IFluidHandler.FluidAction.EXECUTE).getAmount() == 1) {
                scexHeatCredit += value.heatPerMb();
            }
        }
        long accepted = com.miophas.singularity_iteration.core.runtime.energy.ThermalOutput.enabled()
            ? com.miophas.singularity_iteration.core.runtime.energy.ThermalOutput.offer(
                com.miophas.singularity_iteration.core.runtime.energy.ThermalOutput.front(this),
                Math.min(rate, scexHeatCredit))
            : pushHeatToFront(rate);
        scexHeatCredit -= accepted;
        // 输出读数用粘滞的“最近一次实际输出”，不要用实时 credit：
        // 1 mB = heatPerMb(=2×rate) HU，credit 每 tick 就在 0 与 rate 之间交替，直接显示会变成 0/32 方波。
        if (accepted > 0) transmitHeat = (int) Math.min(Integer.MAX_VALUE, accepted);
        currentHeatOutput = (int) Math.min(Integer.MAX_VALUE, rate);
        // 火焰进度按“燃料罐还能烧多久”折算（credit 每 2 tick 归零，不能拿来驱动火焰，否则火焰隔 tick 闪烁）。
        long heatPerMb = value.heatPerMb();
        long tickRate = Math.max(1L, rate);
        maxBurnTime = (int) Math.min(Integer.MAX_VALUE, Math.max(1L, (long) FUEL_CAPACITY * heatPerMb / tickRate));
        long remainingHu = scexHeatCredit + (long) fuelTank.getFluidAmount() * heatPerMb;
        burnTime = (int) Math.min(Integer.MAX_VALUE, (remainingHu + tickRate - 1) / tickRate);
        currentFuelBurning = 0;
        currentFuelHeatGenerated = 0;
        // 原版激活状态 = “还在燃烧”（有合法燃料即在产热），而不是“本 tick 真的推出去”：否则隔 tick 抖动方块状态。
        isWorking = true;
        if (previousCredit != scexHeatCredit || previousWorking != isWorking || accepted > 0) setChanged();
    }

    /** 非独立电网模式：把热直接推给正面的用热方。 */
    private long pushHeatToFront(long rate) {
        if (level == null) return 0;
        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        BlockPos adjacentPos = worldPosition.relative(facing);
        IMioIcifCapabilities.IHeatStorage adjacentHeat = level.getCapability(
            IMioIcifCapabilities.HEAT_STORAGE_BLOCK, adjacentPos, facing.getOpposite());
        if (adjacentHeat == null) {
            adjacentHeat = MioIcifAPI.instance().getCapabilities().adaptHeatStorage(level.getBlockEntity(adjacentPos));
        }
        if (adjacentHeat == null || !adjacentHeat.canReceiveHeat()) return 0;
        long room = Math.max(0, adjacentHeat.getMaxHeatStored() - adjacentHeat.getHeatStored());
        long toOutput = Math.min(Math.min(rate, scexHeatCredit), room);
        if (toOutput <= 0) return 0;
        return adjacentHeat.receiveHeat(toOutput, false);
    }

    private void handleFuelBucketSlot() {
        if (com.miophas.singularity_iteration.core.runtime.energy.ThermalOutput.enabled()) { scexFillFuel(); return; }
        ItemStack fuelBucketStack = itemHandler.getStackInSlot(FUEL_BUCKET_SLOT);
        FluidStack content = fuelContentOf(fuelBucketStack);
        if (content == null) {
            return;
        }

        if (fuelTank.getFluidAmount() >= fuelTank.getCapacity()) {
            return;
        }

        ItemStack emptyContainer = emptyContainerFor(fuelBucketStack);

        ItemStack emptyBucketStack = itemHandler.getStackInSlot(EMPTY_BUCKET_SLOT);
        if (!emptyBucketStack.isEmpty()) {
            if (!ItemStack.isSameItem(emptyBucketStack, emptyContainer) || emptyBucketStack.getCount() >= emptyBucketStack.getMaxStackSize()) {
                return;
            }
        }

        int filled = fuelTank.fill(content, IFluidHandler.FluidAction.EXECUTE);
        if (filled >= content.getAmount()) {
            fuelBucketStack.shrink(1);
            if (fuelBucketStack.isEmpty()) {
                itemHandler.setStackInSlot(FUEL_BUCKET_SLOT, ItemStack.EMPTY);
            }
            if (emptyBucketStack.isEmpty()) {
                itemHandler.setStackInSlot(EMPTY_BUCKET_SLOT, emptyContainer);
            } else {
                emptyBucketStack.grow(1);
            }
            setChanged();
        }
    }

    private boolean isFuelBucket(ItemStack stack) {
        return fuelContentOf(stack) != null;
    }

    /** 取出容器内的半流质燃料内容（整桶/整单元），非燃料容器返回 null。 */
    @Nullable
    private FluidStack fuelContentOf(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (mio_icif_cells.isFluidCell(stack)) {
            FluidStack content = mio_icif_cells.getCellFluid(stack);
            int capacity = mio_icif_cells.getCellCapacity(stack);
            if (content.isEmpty() || capacity <= 0 || content.getAmount() < capacity
                    || !SemifluidFuels.accepts(content.getFluid())) return null;
            return content.copyWithAmount(capacity);
        }
        if (stack.getItem() instanceof BucketItem bucket && SemifluidFuels.accepts(bucket.content)) {
            return new FluidStack(bucket.content, FUEL_PER_BUCKET);
        }
        return null;
    }

    /** 排空后返回的空容器物品。 */
    private static ItemStack emptyContainerFor(ItemStack stack) {
        if (mio_icif_cells.isFluidCell(stack)) {
            ItemStack empty = mio_icif_cells.getEmptyCellForStack(stack);
            return empty.isEmpty() ? new ItemStack(mio_icif_cells.CELL_EMPTY.get()) : empty;
        }
        return new ItemStack(Items.BUCKET);
    }

    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == FUEL_BUCKET_SLOT) {
            return isFuelBucket(stack);
        }
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", itemHandler.serializeNBT(registries));
        tag.putInt("burnTime", burnTime);
        tag.putInt("maxBurnTime", maxBurnTime);
        tag.putBoolean("isWorking", isWorking);
        if (com.miophas.singularity_iteration.core.runtime.energy.ThermalOutput.enabled() || scexHeatCredit > 0) tag.putLong("scex_heat_credit_hu", scexHeatCredit);
        tag.put("fuelTank", fuelTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("currentFuelBurning", currentFuelBurning);
        tag.putInt("currentFuelHeatGenerated", currentFuelHeatGenerated);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Items")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
        }
        burnTime = Math.max(0, tag.getInt("burnTime"));
        // credit 标签只在有余额时写入；缺标签说明旧存档本来就没有余额。
        // （旧版 burnTime 是“credit 折算 tick 数”，现在改成“燃料罐剩余 tick 数”，不能再反推 credit。）
        scexHeatCredit = tag.contains("scex_heat_credit_hu", net.minecraft.nbt.Tag.TAG_LONG)
            ? com.miophas.singularity_iteration.core.api.util.BoundedUnits.clamp(tag.getLong("scex_heat_credit_hu"), HU_PER_BUCKET)
            : 0;
        maxBurnTime = tag.getInt("maxBurnTime");
        isWorking = tag.getBoolean("isWorking");
        if (tag.contains("fuelTank")) {
            fuelTank.readFromNBT(registries, tag.getCompound("fuelTank"));
        }
        currentFuelBurning = tag.getInt("currentFuelBurning");
        currentFuelHeatGenerated = tag.getInt("currentFuelHeatGenerated");
    }

    @Nullable
    @Override
    public IMioIcifCapabilities.IHeatStorage getHeatStorageCapability(@Nullable Direction side) {
        return null;
    }

    @Nullable
    @Override
    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    @Nullable
    public IItemHandler getItemHandlerCapability(@Nullable Direction side) {
        return itemHandler;
    }

    public IFluidHandler getFuelHandler() {
        return fuelTank;
    }

    @Nullable
    @Override
    protected IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        return fuelTank;
    }

    @Nullable
    public IFluidHandler getFuelHandlerCapability(@Nullable Direction side) {
        return fuelTank;
    }

    public int getFuelAmount() {
        return fuelTank.getFluidAmount();
    }

    public int getFuelCapacity() {
        return fuelTank.getCapacity();
    }

    public int getBurnTime() {
        return burnTime;
    }

    public int getMaxBurnTime() {
        return maxBurnTime;
    }

    public int getCurrentFuelBurning() {
        return currentFuelBurning;
    }

    public boolean isWorking() {
        return isWorking;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[]{FUEL_BUCKET_SLOT, EMPTY_BUCKET_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        if (slot == FUEL_BUCKET_SLOT) {
            return isFuelBucket(stack);
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot == EMPTY_BUCKET_SLOT) {
            return true;
        }
        if (slot == FUEL_BUCKET_SLOT) {
            return true;
        }
        return false;
    }

    @Override
    public int getContainerSize() {
        return TOTAL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
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
        if (amount <= 0 || slot < 0 || slot >= itemHandler.getSlots()) return ItemStack.EMPTY;
        ItemStack stack = itemHandler.getStackInSlot(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = stack.split(amount);
        if (stack.isEmpty()) {
            itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        }
        setChanged();
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
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return player != null && Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.fluid_heat_generator");
    }

    public ContainerData createContainerData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> burnTime;
                    case 1 -> maxBurnTime;
                    case 2 -> fuelTank.getFluidAmount();
                    case 3 -> FUEL_CAPACITY;
                    case 4 -> transmitHeat;
                    case 5 -> currentHeatOutput;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> burnTime = value;
                    case 1 -> maxBurnTime = value;
                }
            }

            @Override
            public int getCount() {
                return 6;
            }
        };
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.miophas.singularity_iteration.common.menu.huentity.FluidHeatGeneratorMenu(containerId, playerInventory, this);
    }
}
