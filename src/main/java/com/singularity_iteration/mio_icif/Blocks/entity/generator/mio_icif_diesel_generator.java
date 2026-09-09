package com.singularity_iteration.mio_icif.Blocks.entity.generator;

import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Diesel_Generator;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.energy.IEnergyStorageAccess;
import com.singularity_iteration.mio_icif.api.internal.energy.GenericGeneratorBlockEntity;
import com.singularity_iteration.mio_icif.api.item.IItemAPI;
import com.singularity_iteration.mio_icif.api.registry.IMioIcifRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class mio_icif_diesel_generator extends GenericGeneratorBlockEntity {

    public static final int SLOT_COUNT = 3;
    public static final int FUEL_BUCKET_SLOT = 0;
    public static final int EMPTY_BUCKET_SLOT = 1;
    public static final int BATTERY_SLOT = 2;

    public static final int FUEL_CAPACITY = 240000;
    public static final int FUEL_PER_BUCKET = 1000;

    public static final int FUEL_ENERGY_PER_MB = 5;
    public static final int FUEL_ENERGY_PER_TICK_BASE = 80;
    public static final double PRODUCTION_MULTIPLIER = 1.5;

    public static final int FUEL_CONSUME_PER_TICK = (int) Math.ceil((double) FUEL_ENERGY_PER_TICK_BASE / FUEL_ENERGY_PER_MB);
    public static final long ENERGY_GENERATION_RATE = (long) (FUEL_ENERGY_PER_TICK_BASE * PRODUCTION_MULTIPLIER);

    public static final long ENERGY_CAPACITY = 1000000L;
    public static final long MAX_RECEIVE = 0L;
    public static final long MAX_EXTRACT = 512L;

    private static final ResourceLocation DIESELOIL_ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "dieseloil");
    private static final ResourceLocation ENTITY_TYPE_ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "diesel_generator");

    protected final FluidTank fuelTank;

    public mio_icif_diesel_generator(BlockPos pos, BlockState state) {
        this(pos, state, resolveEntityType());
    }

    public mio_icif_diesel_generator(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            SlotLayout.builder().extra(2).battery().build(), ENERGY_GENERATION_RATE, ENERGY_CAPACITY, MAX_RECEIVE, MAX_EXTRACT,
            MioIcifAPI.instance().getEnergyNetAPI().getCableTier("ev"));

        Fluid dieselFluid = resolveDieselFluid();
        this.fuelTank = new FluidTank(FUEL_CAPACITY, fluidStack ->
            dieselFluid != null && fluidStack.getFluid() == dieselFluid);
    }

    private static BlockEntityType<?> resolveEntityType() {
        BlockEntityType<?> type = MioIcifAPI.instance().getRegistries().getBlockEntityType(ENTITY_TYPE_ID);
        return type != null ? type : BlockEntityType.Builder.of(mio_icif_diesel_generator::new, net.minecraft.world.level.block.Blocks.AIR).build(null);
    }

    private static Fluid resolveDieselFluid() {
        return MioIcifAPI.instance().getRegistries().getFluid(DIESELOIL_ID);
    }

    private boolean isFuelBucket(ItemStack stack) {
        IItemAPI itemAPI = MioIcifAPI.instance().getItemAPI();
        Fluid dieselFluid = resolveDieselFluid();
        if (dieselFluid == null) return false;
        if (isDieselBucket(stack)) return true;
        if (itemAPI.isFluidCell(stack)) {
            var content = itemAPI.getFluidCellContent(stack);
            return !content.isEmpty() && content.getFluid() == dieselFluid;
        }
        return false;
    }

    private boolean isDieselBucket(ItemStack stack) {
        IMioIcifRegistries registries = MioIcifAPI.instance().getRegistries();
        var dieselBucket = registries.getItem(ResourceLocation.fromNamespaceAndPath("mio_icif", "dieseloil_bucket"));
        if (dieselBucket != null && stack.is(dieselBucket)) return true;
        return false;
    }

    @Override
    public int getFuelBurnTime(ItemStack fuel) {
        if (isFuelBucket(fuel)) {
            return Integer.MAX_VALUE;
        }
        return 0;
    }

    private void handleFuelBucketSlot() {
        ItemStack fuelBucketStack = itemHandler.getStackInSlot(FUEL_BUCKET_SLOT);
        if (fuelBucketStack.isEmpty() || !isFuelBucket(fuelBucketStack)) return;

        if (fuelTank.getFluidAmount() >= fuelTank.getCapacity()) return;

        IItemAPI itemAPI = MioIcifAPI.instance().getItemAPI();
        Fluid dieselFluid = resolveDieselFluid();
        if (dieselFluid == null) return;

        boolean isCell = itemAPI.isFluidCell(fuelBucketStack);
        ItemStack emptyContainer;
        if (isCell) {
            emptyContainer = itemAPI.getFluidCellEmptyContainer(fuelBucketStack);
            if (emptyContainer.isEmpty()) {
                var emptyCell = MioIcifAPI.instance().getRegistries().getItem(ResourceLocation.fromNamespaceAndPath("mio_icif", "cell_empty"));
                emptyContainer = emptyCell != null ? new ItemStack(emptyCell) : ItemStack.EMPTY;
            }
        } else {
            emptyContainer = new ItemStack(Items.BUCKET);
        }

        ItemStack emptyBucketStack = itemHandler.getStackInSlot(EMPTY_BUCKET_SLOT);
        if (!emptyBucketStack.isEmpty()) {
            if (!ItemStack.isSameItem(emptyBucketStack, emptyContainer) || emptyBucketStack.getCount() >= emptyBucketStack.getMaxStackSize()) {
                return;
            }
        }

        int filled = fuelTank.fill(new FluidStack(dieselFluid, FUEL_PER_BUCKET), IFluidHandler.FluidAction.EXECUTE);
        if (filled >= FUEL_PER_BUCKET) {
            fuelBucketStack.shrink(1);
            if (emptyBucketStack.isEmpty()) {
                itemHandler.setStackInSlot(EMPTY_BUCKET_SLOT, emptyContainer);
            } else {
                emptyBucketStack.grow(1);
            }
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    }

    @Override
    protected void consumeFuel() {
    }

    @Override
    protected void generateEnergy() {
    }

    @Override
    protected void chargeItems() {
        ItemStack chargeStack = itemHandler.getStackInSlot(BATTERY_SLOT);
        if (chargeStack.isEmpty()) return;

        IItemAPI itemAPI = MioIcifAPI.instance().getItemAPI();
        if (itemAPI.isBattery(chargeStack)) {
            long currentEnergy = itemAPI.getBatteryStored(chargeStack);
            long batteryMaxEnergy = itemAPI.getBatteryCapacity(chargeStack);
            long batteryChargeRate = itemAPI.getChargeRate(chargeStack);

            if (currentEnergy >= batteryMaxEnergy) return;

            IEnergyStorageAccess storage = getEnergyStorage();
            long availableEnergy = storage.getAmount();
            if (availableEnergy <= 0) return;

            long energyToCharge = Math.min(batteryChargeRate, batteryMaxEnergy - currentEnergy);
            energyToCharge = Math.min(energyToCharge, availableEnergy);

            long energyExtracted = storage.useEnergy(energyToCharge, false);
            itemAPI.chargeBattery(chargeStack, energyExtracted, false);
            setChanged();
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_diesel_generator blockEntity) {
        if (level.isClientSide()) return;

        boolean wasBurning = blockEntity.burnTime > 0;

        blockEntity.handleFuelBucketSlot();

        blockEntity.chargeItems();

        if (blockEntity.shouldDirectlyDistributeEnergy()) {
            blockEntity.distributeEnergy();
        } else {
            blockEntity.distributeEnergyToCompatSinks();
        }

        IEnergyStorageAccess storage = blockEntity.getEnergyStorage();
        boolean isEnergyFull = storage.getAmount() >= storage.getCapacity();

        if (!isEnergyFull && blockEntity.fuelTank.getFluidAmount() >= FUEL_CONSUME_PER_TICK) {
            blockEntity.fuelTank.drain(FUEL_CONSUME_PER_TICK, IFluidHandler.FluidAction.EXECUTE);
            long energyToGenerate = Math.min(ENERGY_GENERATION_RATE, storage.getCapacity() - storage.getAmount());
            if (energyToGenerate > 0) {
                storage.generateEnergy(energyToGenerate, false);
            }
            blockEntity.burnTime = 1;
            blockEntity.burnDuration = 1;
        } else {
            blockEntity.burnTime = 0;
        }

        boolean isBurning = blockEntity.burnTime > 0;
        if (wasBurning != isBurning) {
            BlockState newState = level.getBlockState(pos);
            if (newState.hasProperty(mio_icif_Block_Diesel_Generator.ACTIVE)) {
                newState = newState.setValue(mio_icif_Block_Diesel_Generator.ACTIVE, isBurning);
                level.setBlock(pos, newState, 3);
            }
        }

        blockEntity.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (itemHandler != null) {
            tag.put("Items", itemHandler.serializeNBT(registries));
        }
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnDuration", burnDuration);
        if (fuelTank != null) {
            tag.put("FuelTank", fuelTank.writeToNBT(registries, new CompoundTag()));
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (itemHandler != null && tag.contains("Items")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
        }
        burnTime = tag.getInt("BurnTime");
        burnDuration = tag.getInt("BurnDuration");
        if (fuelTank != null && tag.contains("FuelTank")) {
            fuelTank.readFromNBT(registries, tag.getCompound("FuelTank"));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (itemHandler != null) {
            tag.put("Items", itemHandler.serializeNBT(registries));
        }
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnDuration", burnDuration);
        if (fuelTank != null) {
            tag.put("FuelTank", fuelTank.writeToNBT(registries, new CompoundTag()));
        }
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
        if (fuelTank != null && tag.contains("FuelTank")) {
            fuelTank.readFromNBT(registries, tag.getCompound("FuelTank"));
        }
    }

    public FluidTank getFuelTank() {
        return fuelTank;
    }

    public int getFuelAmount() {
        return fuelTank != null ? fuelTank.getFluidAmount() : 0;
    }

    public int getFuelCapacity() {
        return fuelTank != null ? fuelTank.getCapacity() : 0;
    }

    @Override
    public int getContainerSize() {
        return itemHandler != null ? itemHandler.getSlots() : 0;
    }

    @Override
    public boolean isEmpty() {
        if (itemHandler == null) return true;
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) return false;
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
        ItemStack result = stack.split(amount);
        if (stack.isEmpty()) {
            itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        }
        setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (itemHandler == null) return ItemStack.EMPTY;
        ItemStack stack = itemHandler.getStackInSlot(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        setChanged();
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (itemHandler != null) {
            itemHandler.setStackInSlot(slot, stack);
            setChanged();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null) return true;
        return level.getBlockEntity(getBlockPos()) == this
            && player.distanceToSqr(worldPosition.getX() + 0.5,
                                   worldPosition.getY() + 0.5,
                                   worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[]{FUEL_BUCKET_SLOT, EMPTY_BUCKET_SLOT, BATTERY_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        if (slot == FUEL_BUCKET_SLOT) return isFuelBucket(stack);
        if (slot == BATTERY_SLOT) return MioIcifAPI.instance().getItemAPI().isBattery(stack);
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot == EMPTY_BUCKET_SLOT || slot == BATTERY_SLOT;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.diesel_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Generator.DieselGeneratorMenu(containerId, inventory, this, this.getItemHandler(), null);
    }
}