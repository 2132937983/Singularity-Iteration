package com.miophas.singularity_iteration.common.blockentity.generator;

import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.common.block.generator.mio_icif_Block_Diesel_Generator;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.energy.IEnergyStorageAccess;
import com.miophas.singularity_iteration.core.prefab.blockentity.GenericGeneratorBlockEntity;
import com.miophas.singularity_iteration.core.api.item.IItemAPI;
import com.miophas.singularity_iteration.core.api.registry.IMioIcifRegistries;
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

    public static final long ENERGY_GENERATION_RATE = (long) (FUEL_ENERGY_PER_TICK_BASE * PRODUCTION_MULTIPLIER);
    /**
     * 0.1.11.12: 160 EU per mB of diesel (was 7.5, while the LV semifluid generator got 128 from the same diesel): the
     * dedicated engine is the more efficient one. Fuel is drawn 3 mB (480 EU) at a time: 0.75 mB/t at 120 EU/t.
     */
    public static final int EU_PER_MB = 160, MB_PER_UNIT = 3;
    public static final long ENERGY_PER_UNIT = (long) EU_PER_MB * MB_PER_UNIT;
    /** mB a tick at full output (for the manual). */
    public static final double FUEL_CONSUME_PER_TICK = (double) ENERGY_GENERATION_RATE / EU_PER_MB;

    public static final long ENERGY_CAPACITY = 1000000L;
    /** 0.1.11.12 (old grid): EU paid for by fuel already drawn, not yet generated (at most one draw). */
    private long oldGridCredit;
    public static final long MAX_RECEIVE = 0L;
    public static final long MAX_EXTRACT = 512L;

    private static final ResourceLocation DIESELOIL_ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "dieseloil");
    private static final ResourceLocation ENTITY_TYPE_ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "diesel_generator");

    protected final FluidTank fuelTank;
    private long scexFuelCredit;

    public mio_icif_diesel_generator(BlockPos pos, BlockState state) {
        this(pos, state, resolveEntityType());
    }

    public mio_icif_diesel_generator(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            SlotLayout.builder().extra(2).battery().build(), ENERGY_GENERATION_RATE, ENERGY_CAPACITY, MAX_RECEIVE, MAX_EXTRACT,
            MioIcifAPI.instance().getEnergyNetAPI().getCableTier("ev"));

        Fluid dieselFluid = resolveDieselFluid();
        this.fuelTank = new FluidTank(FUEL_CAPACITY, fluidStack ->
            dieselFluid != null && fluidStack.getFluid() == dieselFluid) {
            @Override protected void onContentsChanged() {
                if (energyStorage.scexNetworkControlled()) com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank.markUnsaved(mio_icif_diesel_generator.this);
                else mio_icif_diesel_generator.this.setChanged();
            }
        };
    }

    private static BlockEntityType<?> resolveEntityType() {
        BlockEntityType<?> type = MioIcifAPI.instance().getRegistries().getBlockEntityType(ENTITY_TYPE_ID);
        return type != null ? type : BlockEntityType.Builder.of(mio_icif_diesel_generator::new, net.minecraft.world.level.block.Blocks.AIR).build(null);
    }

    private static Fluid resolveDieselFluid() {
        return MioIcifAPI.instance().getRegistries().getFluid(DIESELOIL_ID);
    }

    private boolean isFuelBucket(ItemStack stack) {
        // 0.1.10.6: also partly filled cells and containers of other mods (through the fluid capability)
        Fluid diesel = resolveDieselFluid();
        return isKnownFuelBucket(stack) || (diesel != null && com.miophas.singularity_iteration.common.util.FluidContainerSlots.holds(stack, diesel));
    }

    private boolean isKnownFuelBucket(ItemStack stack) {
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
        ItemStack held = itemHandler.getStackInSlot(FUEL_BUCKET_SLOT);
        if (!held.isEmpty() && !isKnownFuelBucket(held)) {
            Fluid diesel = resolveDieselFluid();
            if (diesel != null && com.miophas.singularity_iteration.common.util.FluidContainerSlots.emptyInto(itemHandler, FUEL_BUCKET_SLOT, EMPTY_BUCKET_SLOT, fuelTank, diesel)) setChanged();
            return;
        }
        if (energyStorage.scexNetworkControlled()) { scexFillFuelContainer(); return; }
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
                emptyContainer = new ItemStack(com.miophas.singularity_iteration.common.item.cell.mio_icif_cells.CELL_EMPTY.get());
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

        // 0.1.10.6: only a whole container; before, a nearly full tank kept the part it took and the container stayed
        if (fuelTank.fill(new FluidStack(dieselFluid, FUEL_PER_BUCKET), IFluidHandler.FluidAction.SIMULATE) < FUEL_PER_BUCKET) return;
        int filled = fuelTank.fill(new FluidStack(dieselFluid, FUEL_PER_BUCKET), IFluidHandler.FluidAction.EXECUTE);
        if (filled >= FUEL_PER_BUCKET) {
            // 0.1.11.11: written through the slots (changing the live stacks bypassed the handler)
            itemHandler.setStackInSlot(FUEL_BUCKET_SLOT, fuelBucketStack.copyWithCount(fuelBucketStack.getCount() - 1));
            if (emptyBucketStack.isEmpty()) {
                itemHandler.setStackInSlot(EMPTY_BUCKET_SLOT, emptyContainer);
            } else {
                itemHandler.setStackInSlot(EMPTY_BUCKET_SLOT, emptyBucketStack.copyWithCount(emptyBucketStack.getCount() + 1));
            }
            setChanged();
            if (level != null && !level.isClientSide()) {
                com.miophas.singularity_iteration.core.runtime.sync.ClientSyncThrottle.request(this);   // 0.1.10.10 (D4): throttled
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
        if (energyStorage.scexNetworkControlled()) {
            if (com.miophas.singularity_iteration.core.runtime.energy.SolarItemCharging.chargeRange(itemHandler, BATTERY_SLOT, 1, energyStorage, getItemAPI())) setChanged();
            return;
        }
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

            // 0.1.11.11: take only what the item accepts (the rest was lost)
            long accepts = itemAPI.chargeBattery(chargeStack, energyToCharge, true);
            if (accepts <= 0) return;
            long energyExtracted = storage.useEnergy(accepts, false);
            itemAPI.chargeBattery(chargeStack, energyExtracted, false);
            setChanged();
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_diesel_generator blockEntity) {
        if (level.isClientSide()) return;
        blockEntity.engineTick(level, pos);   // 0.1.11.15: the engine model, in both energy modes
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        engine.save(tag);   // 0.1.11.15
        if (energyStorage.scexNetworkControlled() || scexFuelCredit > 0) tag.putLong("scex_fuel_credit_eu", scexFuelCredit);
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
        scexReadFuelCredit(tag);
        engine.load(tag, scexFuelCredit);   // 0.1.11.15: older saves start cold with their fuel credit
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (energyStorage.scexNetworkControlled() || scexFuelCredit > 0) tag.putLong("scex_fuel_credit_eu", scexFuelCredit);
        // 0.1.10.17 (E-2): the parent decides on the inventory (syncInventoryToClient).
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
        scexReadFuelCredit(tag);
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
        if (player == null || level == null || isRemoved()) return false;
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
        return new com.miophas.singularity_iteration.common.menu.generator.DieselGeneratorMenu(containerId, inventory, this);   // 0.1.11.15
    }

    private void scexFillFuelContainer() {
        var input = itemHandler.getStackInSlot(FUEL_BUCKET_SLOT);
        if (input.isEmpty() || !isFuelBucket(input)) return;
        var api = getItemAPI();
        boolean cell = api.isFluidCell(input);
        var fluid = resolveDieselFluid();
        if (fluid == null) return;
        var contents = cell ? api.getFluidCellContent(input.copyWithCount(1)).copy()
            : new FluidStack(fluid, FUEL_PER_BUCKET);
        var empty = cell ? api.getFluidCellEmptyContainer(input.copyWithCount(1)) : new ItemStack(Items.BUCKET);
        if (contents.isEmpty() || contents.getFluid() != fluid || empty.isEmpty()) return;
        if (com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank.transfer(itemHandler, FUEL_BUCKET_SLOT, EMPTY_BUCKET_SLOT, fuelTank, contents, empty)) setChanged();
    }

    private void scexTickFuel(Level level, BlockPos pos) {
        boolean wasBurning = burnTime > 0;
        handleFuelBucketSlot();
        chargeItems();
        if (outputScale() == 0) { burnTime = 0; setChanged(); return; }   // 0.1.8.4: switched off in the config
        var step = com.miophas.singularity_iteration.core.runtime.energy.FluidFuelGeneration.tick(fuelTank, energyStorage,
            scexFuelCredit, MB_PER_UNIT, com.miophas.singularity_iteration.common.energy.GeneratorOutput.rate(this, ENERGY_PER_UNIT), com.miophas.singularity_iteration.common.energy.GeneratorOutput.rate(this, ENERGY_GENERATION_RATE));   // 0.1.8.4, 0.1.11.12
        scexFuelCredit = step.bufferedEnergy();
        burnTime = step.generated() > 0 ? 1 : 0;
        burnDuration = 1;
        if (wasBurning != (burnTime > 0)) {
            var state = level.getBlockState(pos);
            if (state.hasProperty(mio_icif_Block_Diesel_Generator.ACTIVE)) level.setBlock(pos, state.setValue(mio_icif_Block_Diesel_Generator.ACTIVE, burnTime > 0), 3);
        }
        setChanged();
    }

    private void scexReadFuelCredit(CompoundTag tag) {
        if (tag.contains("scex_fuel_credit_eu", net.minecraft.nbt.Tag.TAG_LONG)) {
            scexFuelCredit = Math.clamp(tag.getLong("scex_fuel_credit_eu"), 0L, ENERGY_GENERATION_RATE);
        } else if (energyStorage.scexNetworkControlled()) {
            scexFuelCredit = 0;
        }
    }

    /** 0.1.11.13: comparator by the fuel in the tank. */
    @Override
    public int comparatorSignal() {
        return com.miophas.singularity_iteration.core.api.machine.IComparatorSource.fill(fuelTank.getFluidAmount(), fuelTank.getCapacity());
    }

    // ---- 0.1.11.15: the engine -----------------------------------------------------------------------------------------

    protected final com.miophas.singularity_iteration.common.energy.EngineGovernor engine = new com.miophas.singularity_iteration.common.energy.EngineGovernor(6);

    /** Full output in EU/t of the fuel in the tank (multiplier included); 0 without fuel. */
    public double engineRated() {
        return fuelTank.getFluidAmount() <= 0 ? 0 : ENERGY_GENERATION_RATE * outputScale();
    }

    /** EU per mB of the fuel in the tank at full load (multiplier included). */
    public double engineEuPerMb() {
        return fuelTank.getFluidAmount() <= 0 ? 0 : EU_PER_MB * outputScale();
    }

    public com.miophas.singularity_iteration.common.energy.EngineGovernor getEngine() { return engine; }

    private void engineTick(Level level, BlockPos pos) {
        boolean wasBurning = burnTime > 0;
        handleFuelBucketSlot();
        chargeItems();
        if (!energyStorage.scexNetworkControlled()) {
            if (shouldDirectlyDistributeEnergy()) distributeEnergy();
            else distributeEnergyToCompatSinks();
        }
        double made = engine.tick(energyStorage.getAmount(), energyStorage.getCapacity(), engineRated(), engineEuPerMb(),
            n -> fuelTank.drain(n, IFluidHandler.FluidAction.EXECUTE).getAmount(), this::produce);
        burnTime = made > 0 ? 1 : 0;
        burnDuration = 1;
        if (wasBurning != burnTime > 0) {
            BlockState now = level.getBlockState(pos);
            if (now.getBlock().getStateDefinition().getProperty("active") instanceof net.minecraft.world.level.block.state.properties.BooleanProperty active
                    && now.getValue(active) != burnTime > 0)
                level.setBlock(pos, now.setValue(active, burnTime > 0), 3);
        }
        com.miophas.singularity_iteration.core.runtime.TickMarks.saved(this);
    }

    /** Menu data of the engine screen (see {@link com.miophas.singularity_iteration.common.energy.EngineGovernor#data}). */
    public net.minecraft.world.inventory.ContainerData getEngineData() {
        return new net.minecraft.world.inventory.ContainerData() {
            @Override public int getCount() { return com.miophas.singularity_iteration.common.energy.EngineGovernor.DATA_COUNT; }
            @Override public void set(int i, int v) { }
            @Override public int get(int i) {
                return switch (i) {
                    case 9 -> fuelTank.getFluidAmount();
                    case 10 -> fuelTank.getCapacity();
                    case 11 -> fuelTank.isEmpty() ? -1 : net.minecraft.core.registries.BuiltInRegistries.FLUID.getId(fuelTank.getFluid().getFluid());
                    default -> engine.data(i, engineRated(), engineEuPerMb(), energyStorage.getAmount(), energyStorage.getCapacity());
                };
            }
        };
    }
}
