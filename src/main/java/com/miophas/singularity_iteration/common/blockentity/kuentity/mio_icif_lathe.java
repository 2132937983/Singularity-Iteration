package com.miophas.singularity_iteration.common.blockentity.kuentity;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import com.miophas.singularity_iteration.core.api.item.ILatheItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import com.miophas.singularity_iteration.core.api.machine.ISlotLayout;

/**
 * Lathe block entity, aligned with IC2 TileEntityLathe (2.2.828 experimental).
 * <ul>
 *   <li>3 slots: tool (0), workpiece (1), output (2). Automation inserts tools and workpieces from any
 *       side and extracts only the output (IC2 InvSlotConsumableClass / InvSlotOutput).</li>
 *   <li>KU buffer of 10000. Every tick: the lathe is active while the buffer is above 500, the buffer
 *       loses 1% (integer part, rounded), then the lathe requests KU from the first adjacent kinetic
 *       source that offers any. Sources that push KU into neighbours fill the buffer too.</li>
 *   <li>A cut (GUI button, position 0-4) needs 1000 KU and a tool harder than the workpiece. It thins
 *       the position by 1 (never below 1), gives the workpiece's by-product with its chance, costs the
 *       tool 1 durability (the tool is destroyed at its maximum) and 1000 KU.</li>
 * </ul>
 */
@SuppressWarnings("null")
public class mio_icif_lathe extends BlockEntity implements MenuProvider, Container, com.miophas.singularity_iteration.core.api.machine.IProducerBlock {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .tool()
        .input(1)
        .output(1)
        .build();

    // Slots, in builder order: TOOL=0, INPUT=1, OUTPUT=2
    public static final int TOTAL_SLOTS = 3;
    public static final int TOOL_SLOT = 0;
    public static final int LATHE_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;

    // IC2 TileEntityLathe constants
    public static final int MAX_KU_BUFFER = 10000;
    public static final int KU_COST_PER_OPERATION = 1000;
    public static final int ACTIVE_THRESHOLD = 500;
    public static final int POSITION_COUNT = 5;

    protected MachineItemHandler itemHandler;

    /** Automation view: inserts into the tool and workpiece slots, extracts only from the output. */
    private final IItemHandler automationHandler = new IItemHandler() {
        @Override public int getSlots() { return itemHandler.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == OUTPUT_SLOT ? stack : itemHandler.insertItem(slot, stack, simulate);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == OUTPUT_SLOT ? itemHandler.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return slot != OUTPUT_SLOT && itemHandler.isItemValid(slot, stack);
        }
    };

    /** Receive-only KU input on every side, for kinetic sources that push into their neighbours. */
    private final IMioIcifCapabilities.IKineticStorage kineticInput = new IMioIcifCapabilities.IKineticStorage() {
        @Override public long getKineticStored() { return kUBuffer; }
        @Override public long getMaxKineticStored() { return MAX_KU_BUFFER; }
        @Override public long receiveKinetic(long maxReceive, boolean simulate) {
            long accepted = Math.min(Math.max(0, maxReceive), MAX_KU_BUFFER - kUBuffer);
            if (accepted <= 0) return 0;
            if (!simulate) {
                kUBuffer += (int) accepted;
                com.miophas.singularity_iteration.core.runtime.TickMarks.saved(mio_icif_lathe.this);
            }
            return accepted;
        }
        @Override public long extractKinetic(long maxExtract, boolean simulate) { return 0; }
        @Override public boolean canExtractKinetic() { return false; }
        @Override public boolean canReceiveKinetic() { return true; }
        @Override public int getRPM() { return 0; }
        @Override public boolean isOverspeed() { return false; }
        @Override public long getKineticLossPerTick() { return 0; }
        @Override public long getMaxReceive() { return MAX_KU_BUFFER; }
        @Override public long getMaxExtract() { return 0; }
    };

    public int kUBuffer = 0;

    protected boolean active = false;

    public mio_icif_lathe(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.LATHE_ENTITY_TYPE.get());
    }

    public mio_icif_lathe(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(type, pos, state);
        this.itemHandler = new MachineItemHandler(LAYOUT) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
        this.itemHandler.setValidator((slot, stack, slotType) -> mio_icif_lathe.this.isItemValidForSlot(slot, stack));
    }

    /** IC2 updateEntityServer: active state, 1% decay, then the KU request. */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_lathe blockEntity) {
        if (level.isClientSide()) return;

        boolean shouldBeActive = blockEntity.kUBuffer > ACTIVE_THRESHOLD;
        if (blockEntity.active != shouldBeActive) {
            blockEntity.active = shouldBeActive;
            blockEntity.updateBlockState();
        }

        // IC2: (int)(kUBuffer - (kUBuffer / 100) + 0.5F), integer division (no decay below 100 KU)
        blockEntity.kUBuffer = (int) (blockEntity.kUBuffer - (blockEntity.kUBuffer / 100) + 0.5F);

        blockEntity.requestKU();

        // 0.1.10.10 (D1): save mark only (setChanged() every tick also updated the six neighbours)
        com.miophas.singularity_iteration.core.runtime.TickMarks.saved(blockEntity);
    }

    /**
     * IC2 getKU: asks the adjacent sides in order and takes KU from the first source that offers any,
     * at most what the source offers this tick and what the buffer has room for.
     */
    private void requestKU() {
        if (level == null) return;
        for (Direction dir : Direction.values()) {
            IMioIcifCapabilities.IKineticStorage source = kineticNeighbour(dir);
            if (source == null || !source.canExtractKinetic()) continue;
            long bandwidth = Math.min(source.getMaxExtract(), source.extractKinetic(MAX_KU_BUFFER, true));
            if (bandwidth <= 0) continue;
            long diff = Math.min(bandwidth, MAX_KU_BUFFER - kUBuffer);
            if (diff > 0) {
                kUBuffer += (int) Math.max(0, Math.min(diff, source.extractKinetic(diff, false)));
            }
            break;
        }
    }

    @Nullable
    private IMioIcifCapabilities.IKineticStorage kineticNeighbour(Direction dir) {
        IMioIcifCapabilities.IKineticStorage storage = com.miophas.singularity_iteration.core.runtime.NeighbourCaps.get(
            this, IMioIcifCapabilities.KINETIC_STORAGE_BLOCK, dir);
        if (storage != null) return storage;
        BlockPos adjacentPos = worldPosition.relative(dir);
        if (!level.isLoaded(adjacentPos)) return null;
        return MioIcifAPI.instance().getCapabilities().adaptKineticStorage(level.getBlockEntity(adjacentPos));
    }

    /**
     * IC2 process: cuts the workpiece at {@code position} (0-4). Sent by the GUI buttons.
     * @return true if the cut was made
     */
    public boolean process(int position) {
        if (position < 0 || position >= POSITION_COUNT) return false;
        if (!canWork(false)) return false;

        ItemStack latheStack = itemHandler.getStackInSlot(LATHE_SLOT);
        ItemStack toolStack = itemHandler.getStackInSlot(TOOL_SLOT);
        ILatheItem l = (ILatheItem) latheStack.getItem();
        ILatheItem.ILatheTool t = (ILatheItem.ILatheTool) toolStack.getItem();

        if (!canAddOutput(l.getOutputItem(latheStack, position))) return false;
        int[] currentState = l.getCurrentState(latheStack);
        if (currentState[position] <= 1) return false;
        l.setState(latheStack, position, currentState[position] - 1);
        if (level != null && level.random.nextFloat() < l.getOutputChance(latheStack, position)) {
            addOutput(l.getOutputItem(latheStack, position));
        }
        t.setCustomDamage(toolStack, t.getCustomDamage(toolStack) + 1);
        if (t.getCustomDamage(toolStack) >= t.getMaxCustomDamage(toolStack)) {
            itemHandler.setStackInSlot(TOOL_SLOT, ItemStack.EMPTY);
        }
        kUBuffer -= KU_COST_PER_OPERATION;

        setChanged();
        return true;
    }

    /**
     * IC2 canWork: a lathe tool and a workpiece are present, the tool is harder than the workpiece,
     * and (unless {@code power}) the buffer holds the KU of one cut.
     */
    public boolean canWork(boolean power) {
        ItemStack toolStack = itemHandler.getStackInSlot(TOOL_SLOT);
        ItemStack latheStack = itemHandler.getStackInSlot(LATHE_SLOT);

        if (toolStack.isEmpty() || !(toolStack.getItem() instanceof ILatheItem.ILatheTool t)) return false;
        if (latheStack.isEmpty() || !(latheStack.getItem() instanceof ILatheItem l)) return false;
        if (kUBuffer < KU_COST_PER_OPERATION && !power) return false;

        return t.getHardness(toolStack) > l.getHardness(latheStack);
    }

    /** IC2 InvSlotOutput.canAdd: the whole stack fits into the output slot. */
    private boolean canAddOutput(ItemStack stack) {
        if (stack.isEmpty()) return true;
        ItemStack current = itemHandler.getStackInSlot(OUTPUT_SLOT);
        int limit = Math.min(stack.getMaxStackSize(), itemHandler.getSlotLimit(OUTPUT_SLOT));
        if (current.isEmpty()) return stack.getCount() <= limit;
        return ItemStack.isSameItemSameComponents(current, stack) && current.getCount() + stack.getCount() <= limit;
    }

    private void addOutput(ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemStack current = itemHandler.getStackInSlot(OUTPUT_SLOT);
        itemHandler.setStackInSlot(OUTPUT_SLOT,
            current.isEmpty() ? stack.copy() : current.copyWithCount(current.getCount() + stack.getCount()));
    }

    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case TOOL_SLOT -> stack.getItem() instanceof ILatheItem.ILatheTool;
            case LATHE_SLOT -> stack.getItem() instanceof ILatheItem;
            default -> false;
        };
    }

    protected void updateBlockState() {
        if (level == null || level.isClientSide()) return;
        BlockState state = getBlockState();
        if (state.hasProperty(com.miophas.singularity_iteration.common.block.producer.mio_icif_block_lathe.LIT)) {
            level.setBlock(worldPosition, state.setValue(
                com.miophas.singularity_iteration.common.block.producer.mio_icif_block_lathe.LIT, active), 3);
        }
    }

    // ========== Capabilities ==========

    public IItemHandler getItemHandlerCapability(@Nullable Direction side) {
        return automationHandler;
    }

    public IMioIcifCapabilities.IKineticStorage getKineticStorageCapability(@Nullable Direction side) {
        return kineticInput;
    }

    public MachineItemHandler getItemHandler() {
        return itemHandler;
    }

    public boolean isActive() { return active; }

    public int getKUBuffer() { return kUBuffer; }

    public int getMaxKUBuffer() { return MAX_KU_BUFFER; }

    // ==================== Container (spilled on removal, carried by the wrench) ====================

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) return false;
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
        if (stack.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        ItemStack removed = stack.split(amount);
        itemHandler.setStackInSlot(slot, stack);
        return removed;
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
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    // ==================== IProducerBlock API ====================

    @Override
    public void forceStartWork() {
        this.active = true;
    }

    @Override
    public void forceStopWork() {
        this.active = false;
    }

    @Override
    public com.miophas.singularity_iteration.core.api.machine.IMachineUpgradeStats getUpgradeStats() {
        return com.miophas.singularity_iteration.core.runtime.upgrade.MachineUpgradeStats.empty();
    }

    @Override
    public int getUpgradeSlotStart() {
        return -1;
    }

    @Override
    public int getUpgradeSlotCount() {
        return 0;
    }

    @Override
    public java.util.List<ItemStack> getUpgrades() {
        return java.util.Collections.emptyList();
    }

    @Override
    public int getProgress() {
        return 0;
    }

    @Override
    public int getMaxProgress() {
        return 1;
    }

    @Override
    public int getBaseMaxProgress() {
        return 1;
    }

    @Override
    public long getEnergyPerTick() {
        return 0;
    }

    @Override
    public ISlotLayout getSlotLayout() {
        return LAYOUT;
    }

    @Override
    public int getContainerSize() {
        return itemHandler.getSlots();
    }

    @Override
    public com.miophas.singularity_iteration.core.api.energy.IEnergyStorageAccess getEnergyStorage() {
        return null;
    }

    @Override
    public com.miophas.singularity_iteration.core.api.machine.IWorkCompleteCallback getWorkCompleteCallback() {
        return null;
    }

    @Override
    public boolean isWorking() {
        return active;
    }

    @Override
    public long getEffectiveEnergyPerTick() {
        return 0;
    }

    @Override
    public long getTotalProcessed() {
        return 0;
    }

    // ========== NBT ==========

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", itemHandler.serializeNBT(registries));
        tag.putInt("kUBuffer", kUBuffer);
        tag.putBoolean("active", active);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("items")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("items"));
        }
        kUBuffer = tag.getInt("kUBuffer");
        active = tag.getBoolean("active");
    }

    // ========== Menu ==========

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.lathe");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new com.miophas.singularity_iteration.common.menu.producer.LatheMenu(id, playerInventory, this);
    }
}
