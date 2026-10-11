package com.miophas.singularity_iteration.core.prefab.blockentity;

import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.api.energy.grid.IEnergyAcceptor;
import com.miophas.singularity_iteration.core.api.energy.grid.IEnergyEmitter;
import com.miophas.singularity_iteration.core.api.item.IItemAPI;
import com.miophas.singularity_iteration.core.runtime.energy.CustomEUEnergyStorage;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.runtime.energy.grid.EnergyNetGlobal;
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
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;


/**
 * Base class for energy container block entities.
 * It extends AbstractEnergyBlockEntity. It is the base class for energy storage block entities.
 * It gives a larger energy capacity and higher energy transfer rates.
 * It uses the EU (Energy Unit) energy system.
 * 
 * <p>Charge and discharge operations go through IItemAPI. Add-ons can register custom battery and armor types.
 * 
 * Redstone modes (based on IC2):
 * 0 - Do not emit a redstone signal.
 * 1 - Emit a signal when the energy is almost full (energy >= capacity - output*20).
 * 2 - Emit a signal when the energy is in the middle range (output < energy < capacity - output).
 * 3 - Emit a signal when the energy is not full (energy < capacity - output).
 * 4 - Emit a signal when the energy is almost empty (energy < output).
 * 5 - Stop the energy output when a redstone signal is received.
 * 6 - Stop the output when a redstone signal is received, but continue the output when the energy is full.
 * 7 - Output energy only when a redstone signal is received.
 */
@SuppressWarnings("null")
public class AbstractEnergyStorageBlockEntity extends AbstractEnergyBlockEntity implements Container, com.miophas.singularity_iteration.core.api.machine.IEnergyContainerBlock {

    // ==================== API method layer ====================
    // Charge and discharge operations go through IItemAPI. Subclasses can override them to support custom battery or armor types.

    /**
     * Gets the item API instance.
     */
    protected IItemAPI getItemAPI() {
        return MioIcifAPI.instance().getItemAPI();
    }

    /**
     * Uses the API to check if the item is a battery.
     */
    protected boolean apiIsBattery(ItemStack stack) {
        return getItemAPI().isBattery(stack);
    }

    /**
     * Uses the API to check if the item is electric armor.
     */
    protected boolean apiIsElectricArmor(ItemStack stack) {
        return getItemAPI().isElectricArmor(stack);
    }

    /**
     * Uses the API to check if the item is an electric tool.
     */
    protected boolean apiIsElectricTool(ItemStack stack) {
        return getItemAPI().isElectricTool(stack);
    }

    /**
     * Uses the API to get the battery capacity (EU).
     */
    protected long apiGetBatteryCapacity(ItemStack stack) {
        return getItemAPI().getBatteryCapacity(stack);
    }

    /**
     * Uses the API to get the current battery energy (EU).
     */
    protected long apiGetBatteryStored(ItemStack stack) {
        return getItemAPI().getBatteryStored(stack);
    }

    /**
     * Uses the API to get the battery charge rate (EU/t).
     */
    protected long apiGetChargeRate(ItemStack stack) {
        return getItemAPI().getChargeRate(stack);
    }

    /**
     * Uses the API to charge a battery.
     */
    protected long apiChargeBattery(ItemStack stack, long amount, boolean simulate) {
        return getItemAPI().chargeBattery(stack, amount, simulate);
    }

    /**
     * Uses the API to discharge a battery.
     */
    protected long apiDischargeBattery(ItemStack stack, long amount, boolean simulate) {
        return getItemAPI().dischargeBattery(stack, amount, simulate);
    }

    /**
     * Uses the API to charge electric armor.
     */
    protected long apiChargeElectricArmor(ItemStack stack, long amount, boolean simulate) {
        return getItemAPI().chargeElectricArmor(stack, amount, simulate);
    }

    /**
     * Uses the API to check if the battery is full.
     */
    protected boolean apiIsBatteryFull(ItemStack stack) {
        return getItemAPI().isBatteryFull(stack);
    }

    /**
     * Uses the API to check if the electric armor is full.
     */
    protected boolean apiIsArmorFull(ItemStack stack) {
        return getItemAPI().getElectricArmorStored(stack) >= getItemAPI().getElectricArmorMaxEnergy(stack);
    }

    /**
     * Uses the API to get the maximum energy of the electric armor (EU).
     */
    protected long apiGetArmorMaxEnergy(ItemStack stack) {
        return getItemAPI().getElectricArmorMaxEnergy(stack);
    }

    /**
     * Uses the API to get the current energy of the electric armor (EU).
     */
    protected long apiGetArmorStored(ItemStack stack) {
        return getItemAPI().getElectricArmorStored(stack);
    }

    // Slot layout: charge slot + discharge slot + 4 extra slots.
    protected static final SlotLayout LAYOUT = SlotLayout.builder()
        .battery()
        .battery()
        .extra(4)
        .build();

    // Item handler.
    protected MachineItemHandler itemHandler;

    // Slot indexes (in builder order: BATTERY=0, BATTERY=1, EXTRA=2-5).
    protected static final int CHARGE_SLOT = 0;
    protected static final int BATTERY_SLOT = 1;
    
    // Redstone mode (0-7).
    protected byte redstoneMode = 0;
    public static final byte REDSTONE_MODES = 8;
    
    protected boolean hasRedstoneInput = false;

    private boolean lastRedstoneOutput = false;
    private final AABB chargepadDetectionBox;

    /**
     * Constructor.
     * @param pos block position
     * @param state block state
     * @param type block entity type
     * @param capacity energy capacity (EU)
     * @param maxReceive maximum input rate (EU/tick)
     * @param maxExtract maximum output rate (EU/tick)
     * @param cableTier cable tier
     */
    public AbstractEnergyStorageBlockEntity(BlockPos pos, BlockState state, BlockEntityType<?> type,
                                     long capacity, long maxReceive, long maxExtract, CableTier cableTier) {
        super(pos, state, type, capacity, maxReceive, maxExtract, cableTier);
        this.itemHandler = createItemHandler();
        this.chargepadDetectionBox = createChargepadDetectionBox(pos);
    }

    /**
     * Simplified constructor (uses default values).
     */
    public AbstractEnergyStorageBlockEntity(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type, 100000, 1000, 1000, CableTier.MV); // Default: 100000 EU capacity, 1000 EU/t input and output, MV tier.
        this.itemHandler = createItemHandler();
        this.chargepadDetectionBox = createChargepadDetectionBox(pos);
    }

    private static AABB createChargepadDetectionBox(BlockPos pos) {
        return new AABB(pos.getX(), pos.getY(), pos.getZ(),
            pos.getX() + 1, pos.getY() + 2.5, pos.getZ() + 1);
    }

    /** Reused spatial query bounds for chargepad ticks. */
    public final AABB getChargepadDetectionBox() {
        return chargepadDetectionBox;
    }

    protected MachineItemHandler createItemHandler() {
        return new MachineItemHandler(LAYOUT) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (slot == CHARGE_SLOT && com.miophas.singularity_iteration.core.runtime.energy.FeMachineBridge.chargeable(stack)) return true;
                return super.isItemValid(slot, stack);
            }
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
    }
    
    @Override
    public Component getDisplayName() {
        // the block's own name: CESU, chargers and the LESU family had no override and showed the
        // generic "Energy Container" key, which zh_cn translates with the BatBox name.
        return getBlockState().getBlock().getName();
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        // Create the BatBox GUI menu. Pass this so that the menu can read the energy data.
        return MachinePresentation.storageMenu(containerId, playerInventory, this);
    }
    
    /**
     * Update logic for each tick (server side only).
     * The IC2 energy grid system handles the energy output automatically through the IEnergySource interface.
     * A manual distributeEnergy() call is not necessary.
     * This method also uses scexFeBridge to push energy to adjacent FE energy storages (for example MI machines).
     */
    public static void tick(Level level, BlockPos pos, BlockState state, AbstractEnergyStorageBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        AbstractEnergyBlockEntity.tick(level, pos, state, blockEntity);

        // every tick: the redstone output must follow the stored energy without a delay
        blockEntity.updateRedstoneInput(level, pos);
        blockEntity.updateEnergyOutput();
        blockEntity.checkRedstoneOutputChange();

        blockEntity.processBattery();

        blockEntity.chargeItems();

        // Only legacy boxes push here. Controlled boxes are routed by IndependentSiEnergy;
        // FeMachineBridge enforces that ownership and shares the item/FE output ledger.
        blockEntity.scexFeBridge().push();
    }
    
    /**
     * Checks if the redstone output state changed. If it changed, this method notifies the neighbors.
     */
    protected void checkRedstoneOutputChange() {
        boolean currentOutput = shouldEmitRedstone();
        if (currentOutput != this.lastRedstoneOutput) {
            this.lastRedstoneOutput = currentOutput;
            notifyNeighborsRedstoneChange();
        }
    }
    
    /**
     * Reads the redstone input signal.
     */
    protected void updateRedstoneInput(Level level, BlockPos pos) {
        this.hasRedstoneInput = level.hasNeighborSignal(pos);
    }
    
    /**
     * Controls the energy output from the redstone mode.
     */
    protected void updateEnergyOutput() {
        // Redstone mode 5: stop the output when a redstone signal is received.
        // Redstone mode 6: stop the output when a redstone signal is received, but continue the output when the energy is full.
        // Redstone mode 7: output energy only when a redstone signal is received.
        boolean shouldEmit = shouldEmitEnergy();
        this.energyStorage.setOutputEnabled(shouldEmit);
    }

    /** Only the four measured classic storage blocks opt into these thresholds. */
    protected boolean usesClassicRedstoneThresholds() {
        return false;
    }
    
    /**
     * Checks if the block must output energy (from the redstone mode).
     */
    public boolean shouldEmitEnergy() {
        switch (this.redstoneMode) {
            case 5: // Stop the output when a redstone signal is received.
                return !this.hasRedstoneInput;
            case 6: // With redstone input, release only the surplus near full charge.
                if (this.hasRedstoneInput) {
                    if (usesClassicRedstoneThresholds()) {
                        return this.energyStorage.getAmount() > this.energyStorage.getCapacity()
                                - 20L * this.energyStorage.getMaxExtract();
                    }
                    return this.energyStorage.getAmount() >= this.energyStorage.getCapacity();
                }
                return true;
            case 7: // Output energy only when a redstone signal is received.
                return this.hasRedstoneInput;
            default:
                return true;
        }
    }
    
    /**
     * Checks if the block must emit a redstone signal (from the redstone mode).
     */
    public boolean shouldEmitRedstone() {
        switch (this.redstoneMode) {
            case 1: // Classic-range storage blocks emit a signal when 20 packets or less of free space remain.
                if (usesClassicRedstoneThresholds()) {
                    return this.energyStorage.getAmount() >= this.energyStorage.getCapacity()
                            - 20L * this.energyStorage.getMaxExtract();
                }
                return this.energyStorage.getAmount() >= this.energyStorage.getCapacity();
            case 2: // Emit a signal when the energy is in the middle range.
                return this.energyStorage.getAmount() > this.energyStorage.getMaxExtract() && 
                       this.energyStorage.getAmount() < this.energyStorage.getCapacity() - this.energyStorage.getMaxExtract();
            case 3: // Emit a signal when the energy is not full.
                if (usesClassicRedstoneThresholds()) {
                    return this.energyStorage.getAmount() < this.energyStorage.getCapacity()
                            - this.energyStorage.getMaxExtract();
                }
                return this.energyStorage.getAmount() < this.energyStorage.getCapacity();
            case 4: // Classic-range storage blocks emit a signal when less than one packet remains.
                if (usesClassicRedstoneThresholds()) {
                    return this.energyStorage.getAmount() < this.energyStorage.getMaxExtract();
                }
                return this.energyStorage.getAmount() <= 0;
            default:
                return false;
        }
    }
    
    /**
     * Gets the current redstone signal strength (0-15).
     */
    public int getRedstoneSignalStrength() {
        return shouldEmitRedstone() ? 15 : 0;
    }
    
    /**
     * Gets the redstone mode.
     */
    public byte getRedstoneMode() {
        return this.redstoneMode;
    }
    
    /**
     * Returns true if there is a redstone input signal.
     */
    public boolean hasRedstoneInput() {
        return this.hasRedstoneInput;
    }
    
    /**
     * Sets the redstone mode. Values outside 0 to REDSTONE_MODES - 1 are ignored.
     */
    public void setRedstoneMode(byte mode) {
        if (mode >= 0 && mode < REDSTONE_MODES && mode != this.redstoneMode) {
            this.redstoneMode = mode;
            setChanged();
            // Tell the neighbor blocks that the redstone signal can change.
            notifyNeighborsRedstoneChange();
        }
    }
    
    /**
     * Changes to the next redstone mode.
     */
    public void cycleRedstoneMode() {
        byte oldMode = this.redstoneMode;
        this.redstoneMode = (byte) ((this.redstoneMode + 1) % REDSTONE_MODES);
        setChanged();
        // Tell the neighbor blocks that the redstone signal can change.
        if (this.redstoneMode != oldMode) {
            notifyNeighborsRedstoneChange();
        }
    }
    
    /**
     * Tells the neighbor blocks that the redstone signal changed (server side only).
     */
    protected void notifyNeighborsRedstoneChange() {
        if (this.level != null && !this.level.isClientSide()) {
            this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
            // Also update this block state.
            this.level.blockUpdated(this.worldPosition, this.getBlockState().getBlock());
        }
    }
    
    /**
     * Gets the redstone mode name (translation key for the GUI).
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

    // ==================== IC2 energy grid overrides ====================
    // A storage block is both a sink (it receives energy from the sides that are not the front) and a source (it outputs energy from the front).
    // The parent class AbstractEnergyBlockEntity outputs energy only when isPowerSource=true.
    // A storage block has isPowerSource=false. Thus these methods must be overridden.

    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor acceptor, Direction direction) {
        return !energyStorage.scexNetworkControlled() && direction == getOutputSide();
    }

    @Override
    public boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction direction) {
        return !energyStorage.scexNetworkControlled() && direction != getOutputSide();
    }

    @Override
    public double getOfferedEnergy() {
        if (!canTransferGridEnergy()) return 0.0D;
        if (!energyStorage.isOutputEnabled()) return 0.0D;
        long available = energyStorage.getAmount();
        long packet = energyStorage.getMaxExtract();
        if (available <= 0 || packet <= 0) return 0.0D;
        if (requiresFullOutputPacket() && available < packet) return 0.0D;
        return Math.min(available, packet);
    }

    /** Storage variants opt in after their packet threshold has been measured. */
    protected boolean requiresFullOutputPacket() {
        return false;
    }

    @Override
    public void drawEnergy(double amount) {
        if (!canTransferGridEnergy()) return;
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
        // Check if the energy output is allowed (redstone mode control).
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
     * <p>Battery discharge goes through IItemAPI. Add-ons can register custom battery types.
     */
    protected void processBattery() {
        if (!itemHandler.getStackInSlot(BATTERY_SLOT).isEmpty()) scexFeBridge().discharge(itemHandler, BATTERY_SLOT);
    }

    /**
     * Charges the item in the charge slot.
     * Batteries and electric armor are supported.
     * 
     * <p>Charge operations go through IItemAPI. Add-ons can register custom battery and armor types.
     */
    protected void chargeItems() {
        scexFeBridge().charge(itemHandler, CHARGE_SLOT);
    }

    // Container interface implementation
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
        if (slot < 0 || slot >= itemHandler.getSlots() || amount <= 0) {
            return ItemStack.EMPTY;
        }
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
        return player != null && Container.stillValidBlockEntity(this, player);
    }
    
    @Override
    public void clearContent() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
        setChanged();
    }
    
    /**
     * Gives the energy storage capability.
     * Storage block: the front outputs, the other sides input.
     */
    @Override
    @Deprecated(since = "0.1.7.14", forRemoval = false)
    @org.jetbrains.annotations.ApiStatus.Internal
    public CustomEUEnergyStorage getEnergyStorageCapability(@Nullable Direction side) {
        return super.getEnergyStorageCapability(side);
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // Save the inventory. The parent class saves the energy.
        if (itemHandler != null) {
            tag.put("Items", itemHandler.serializeNBT(registries));
        }
        // Save the redstone mode.
        tag.putByte("RedstoneMode", this.redstoneMode);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // Load the inventory. The parent class loads the energy.
        if (itemHandler != null && tag.contains("Items")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
        }
        // Load the redstone mode.
        if (tag.contains("RedstoneMode")) {
            this.redstoneMode = tag.getByte("RedstoneMode");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        // Send the inventory to the client.
        if (itemHandler != null) {
            tag.put("Items", itemHandler.serializeNBT(registries));
        }
        // Send the redstone mode to the client.
        tag.putByte("RedstoneMode", this.redstoneMode);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        // Get the item stacks from the server.
        if (itemHandler != null && tag.contains("Items")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
        }
        // Get the redstone mode from the server.
        if (tag.contains("RedstoneMode")) {
            this.redstoneMode = tag.getByte("RedstoneMode");
        }
    }

    // ==================== IEnergyContainerBlock interface implementation ====================

    @Override
    public net.neoforged.neoforge.items.IItemHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public int getBatterySlotCount() {
        return LAYOUT.getCount(com.miophas.singularity_iteration.core.prefab.inventory.SlotType.BATTERY);
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
        return 0; // Energy containers do not track the total charged energy.
    }

    @Override
    public long getTotalDischarged() {
        return 0; // Energy containers do not track the total discharged energy.
    }

    @Override
    public long getRunningTime() {
        return 0; // Energy containers do not track the running time.
    }

    @Override
    public String getRunningState() {
        long energy = energyStorage.getAmount();
        long capacity = energyStorage.getCapacity();
        if (energy > 0 && energy < capacity) {
            return "Working";              // plain English API text (0.1.7.36)
        } else if (energy >= capacity) {
            return "Full";
        } else {
            return "Idle";
        }
    }
}
