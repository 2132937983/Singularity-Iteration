package com.miophas.singularity_iteration.common.blockentity.generator;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.api.energy.grid.*;
import com.miophas.singularity_iteration.core.runtime.energy.grid.*;
import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.api.machine.IGeneratorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;

@SuppressWarnings("null")
public class mio_icif_drop_generator extends AbstractEnergyBlockEntity implements com.miophas.singularity_iteration.core.api.machine.IComparatorSource, com.miophas.singularity_iteration.core.api.energy.grid.IGridDispatch, com.miophas.singularity_iteration.common.area.WorkAreaProvider, IGeneratorBlock {

    private boolean registered = false;

    /**
     * 0.1.11.13: the numbers of a model. The basic and the advanced generator were two copies of the same class; the
     * advanced one now extends this one with its own profile.
     */
    protected record Profile(long capacity, long maxExtract, CableTier tier, long production, int scanRange, int valueScale) {}
    protected static final Profile BASIC = new Profile(10000L, 128L, CableTier.MV, 20L, 3, 1);
    protected static final long MAX_ENERGY_RECEIVE = 0L;
    protected final Profile profile;
    private static final double CONSUME_DISTANCE_SQ = 1.0D;

    protected int fuel = 0;
    private long scexFuelCredit;
    private final com.miophas.singularity_iteration.core.runtime.energy.WorldFuelCollection scexCollector = new com.miophas.singularity_iteration.core.runtime.energy.WorldFuelCollection();
    private boolean isWorking = false;
    /** 0.1.11.19: the furnace (IncineratorPhysics): chamber °C, the ash pit, and the chart of the chamber for the screen. */
    private double chamber = com.miophas.singularity_iteration.common.energy.IncineratorPhysics.AMBIENT;
    private final com.miophas.singularity_iteration.common.energy.Recorder chart = new com.miophas.singularity_iteration.common.energy.Recorder();
    private long lastEu;
    public static final int MARK_BURNT = 1, MARK_INERT = 2;
    private final net.neoforged.neoforge.items.ItemStackHandler ash = new net.neoforged.neoforge.items.ItemStackHandler(1) {
        @Override public boolean isItemValid(int slot, ItemStack stack) { return false; }   // taken out only
        @Override public int getSlotLimit(int slot) { return com.miophas.singularity_iteration.common.energy.IncineratorPhysics.ASH_CAPACITY; }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    public double getChamber() { return chamber; }
    public com.miophas.singularity_iteration.common.energy.Recorder getChart() { return chart; }
    public int getAsh() { return ash.getStackInSlot(0).getCount(); }
    public long getLastEu() { return lastEu; }
    public int getScanRange() { return profile.scanRange(); }
    public long getProduction() { return profile.production(); }
    public net.neoforged.neoforge.items.ItemStackHandler getAshHandler() { return ash; }
    /** Sets the chamber temperature (tests and the capture scene). */
    public void setChamber(double celsius) { chamber = celsius; }

    private boolean ashRoom(int n) {
        return getAsh() + n <= com.miophas.singularity_iteration.common.energy.IncineratorPhysics.ASH_CAPACITY;
    }

    private void addAsh(int n) {
        var stack = ash.getStackInSlot(0);
        int count = Math.min(com.miophas.singularity_iteration.common.energy.IncineratorPhysics.ASH_CAPACITY, (stack.isEmpty() ? 0 : stack.getCount()) + n);
        ash.setStackInSlot(0, new ItemStack(com.miophas.singularity_iteration.common.item.resource.mio_icif_resources.ASH.get(), count));
    }

    /** An item went in: a burning one leaves an ash, one that does not burn two and cools the chamber. */
    private void eat(ItemStack stack) {
        if (getItemValue(stack) <= 0) {
            chamber = com.miophas.singularity_iteration.common.energy.IncineratorPhysics.quench(chamber);
            addAsh(com.miophas.singularity_iteration.common.energy.IncineratorPhysics.ASH_INERT);
            chart.mark(MARK_INERT);
        } else {
            addAsh(com.miophas.singularity_iteration.common.energy.IncineratorPhysics.ASH_BURNT);
            chart.mark(MARK_BURNT);
        }
    }

    /** EU of a tick of fuel at the chamber's temperature (before the output multiplier). */
    public long euPerFuelTick() {
        return Math.round(profile.production() * com.miophas.singularity_iteration.common.energy.IncineratorPhysics.efficiency(chamber));
    }

    private int collect(net.minecraft.server.level.ServerLevel server, BlockPos pos) {
        return scexCollector.items(server, pos, profile.scanRange(), fuel, this::getItemValue, fuelLimit(),
            stack -> ashRoom(com.miophas.singularity_iteration.common.energy.IncineratorPhysics.ASH_INERT), this::eat);
    }

    private void furnace(Level level, boolean burning) {
        chamber = com.miophas.singularity_iteration.common.energy.IncineratorPhysics.step(chamber, burning);
        chart.tick(level.getGameTime(), chamber);
    }

    public mio_icif_drop_generator(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.DROP_GENERATOR_ENTITY_TYPE.get(), BASIC);
    }

    protected mio_icif_drop_generator(BlockPos pos, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<?> type, Profile profile) {
        super(pos, state, type, profile.capacity(), MAX_ENERGY_RECEIVE, profile.maxExtract(), profile.tier());
        this.profile = profile;
        setAsPowerSource(profile.production());
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_drop_generator blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        if (blockEntity.energyStorage.scexNetworkControlled()) {
            blockEntity.scexTickStoredFuel(level, pos);
            return;
        }

        // 0.1.11.11: the shared collector (merged orbs counted, at most 64 entities a tick) and nothing taken while the
        // storage is full or the fuel is well stocked (it ate every drop around it for nothing before)
        if (level instanceof net.minecraft.server.level.ServerLevel server) blockEntity.fuel = blockEntity.collect(server, pos);

        boolean wasWorking = blockEntity.isWorking;
        long room = blockEntity.energyStorage.getCapacity() - blockEntity.energyStorage.getAmount();
        long perTick = blockEntity.euPerFuelTick();
        boolean burning = false;
        blockEntity.lastEu = 0;
        // 0.1.11.19: a tick of fuel heats the chamber; it makes EU by the chamber's temperature (none while it is cold)
        if (blockEntity.fuel > 0 && room > 0 && com.miophas.singularity_iteration.common.energy.GeneratorOutput.multiplier(blockEntity) > 0) {
            long tickEu = perTick > 0 ? com.miophas.singularity_iteration.common.energy.GeneratorOutput.eu(blockEntity, perTick) : 0;   // 0.1.8.4
            // 0.1.11.11: a tick of fuel only when its whole output fits (a part was lost before)
            if (room >= tickEu) {
                if (tickEu > 0) blockEntity.energyStorage.generateEnergyInternal(tickEu, false);
                blockEntity.lastEu = tickEu;
                blockEntity.fuel--;
                burning = true;
            }
        }
        blockEntity.isWorking = burning;
        blockEntity.furnace(level, burning);

        if (wasWorking != blockEntity.isWorking) {
            blockEntity.updateBlockState(blockEntity.isWorking);
        }

        // 0.1.10.10 (D1): save mark only (setChanged() every tick also updated the six neighbours)
        com.miophas.singularity_iteration.core.runtime.TickMarks.saved(blockEntity);
    }

    /** 0.1.11.11: take no more fuel while the storage is full or {@value #FUEL_STOCK} is already stocked. */
    private static final int FUEL_STOCK = 10000;
    private int fuelLimit() {
        // 0.1.11.19: nor while the ash pit is full
        return energyStorage.getAmount() >= energyStorage.getCapacity() || !ashRoom(com.miophas.singularity_iteration.common.energy.IncineratorPhysics.ASH_INERT) ? 0 : FUEL_STOCK;
    }

    private void scexTickStoredFuel(Level level, BlockPos pos) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel server) || !server.getServer().isSameThread()) return;
        int previousFuel = fuel;
        long previousCredit = scexFuelCredit;
        if (com.miophas.singularity_iteration.common.energy.GeneratorOutput.multiplier(this) == 0) return;                      // 0.1.8.4: switched off in the config
        fuel = collect(server, pos);
        // 0.1.11.19: EU per tick of fuel by the chamber's temperature; a cold chamber burns fuel to heat up
        long perTick = com.miophas.singularity_iteration.common.energy.GeneratorOutput.rate(this, euPerFuelTick());   // 0.1.8.4 output multiplier
        boolean burning = false;
        lastEu = 0;
        long room = energyStorage.scexExactAmount().roomBelow(energyStorage.getCapacity()).whole();
        if (perTick <= 0) {
            if (fuel > 0 && room > 0) { fuel--; burning = true; }
        } else {
            scexFuelCredit = Math.min(scexFuelCredit, perTick - 1);
            var step = com.miophas.singularity_iteration.core.runtime.energy.StoredFuelGeneration.tick(energyStorage, fuel, scexFuelCredit, perTick);
            burning = step.generated() > 0 || step.fuelRemaining() < fuel;
            fuel = Math.toIntExact(step.fuelRemaining());
            scexFuelCredit = step.bufferedEnergy();
            lastEu = step.generated();
        }
        furnace(level, burning);
        boolean wasWorking = isWorking;
        isWorking = burning;
        if (wasWorking != isWorking) updateBlockState(isWorking);
        if (previousFuel != fuel || previousCredit != scexFuelCredit || wasWorking != isWorking || lastEu > 0) setChanged();
    }


    /**
     * 0.1.11.12: fuel ticks of an item. Common items count by their furnace burn time (burn time / 8, so coal gives
     * 200 ticks); common items that do not burn give 2 ticks, a tenth of before (a cobblestone generator was free
     * power). Uncommon and above by rarity, as before.
     */
    protected int getItemValue(ItemStack stack) {
        Rarity rarity = stack.getRarity();
        if (rarity != Rarity.COMMON) return getItemValue(rarity);
        int burn = stack.getBurnTime(null);
        // 0.1.11.19: an item that does not burn gives nothing (it was 2): it is incinerated and cools the chamber
        return burn > 0 ? Math.max(2, burn / 8) * profile.valueScale() : 0;
    }

    protected int getItemValue(Rarity rarity) {
        int k = profile.valueScale();
        if (rarity == Rarity.COMMON) return 20 * k;
        if (rarity == Rarity.UNCOMMON) return 400 * k;
        // 0.1.11.11: epic above rare (they were the other way round)
        if (rarity == Rarity.RARE) return 5000 * k;
        if (rarity == Rarity.EPIC) return 10000 * k;
        return 20 * k;
    }

    private void updateBlockState(boolean working) {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState currentState = level.getBlockState(worldPosition);
        if (currentState.getBlock().getStateDefinition().getProperty("active") instanceof net.minecraft.world.level.block.state.properties.BooleanProperty active
                && currentState.getValue(active) != working) {
            level.setBlock(worldPosition, currentState.setValue(active, working), 3);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!energyStorage.scexNetworkControlled() && level != null && !level.isClientSide && !registered) {
            NeoForge.EVENT_BUS.post(new EnergyTileLoadEvent(this, level));
            registered = true;
        }
    }

    @Override
    public void setRemoved() {
        if (!energyStorage.scexNetworkControlled() && level != null && !level.isClientSide && registered) {
            NeoForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this, level));
            registered = false;
        }
        super.setRemoved();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (!energyStorage.scexNetworkControlled() && level != null && !level.isClientSide && !registered) {
            NeoForge.EVENT_BUS.post(new EnergyTileLoadEvent(this, level));
            registered = true;
        }
    }

    @Override
    public double getOfferedEnergy() {
        if (energyStorage.scexNetworkControlled()) return 0;
        long available = Math.min(energyStorage.getAmount(), energyStorage.getMaxExtract());
        return Math.min(available, profile.tier().powerRating);
    }

    @Override
    public void drawEnergy(double amount) {
        if (energyStorage.scexNetworkControlled()) return;
        if (amount > 0.0D) {
            long request = Math.min((long) amount, profile.tier().powerRating);
            energyStorage.extract(request, false);
        }
    }

    @Override
    public int getSourceTier() {
        if (energyStorage.scexNetworkControlled()) return 0;
        return EnergyNetGlobal.cableTierToSourceTier(profile.tier());
    }

    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor acceptor, Direction direction) {
        return !energyStorage.scexNetworkControlled();
    }

    public long getEnergyStored() {
        return energyStorage.getAmount();
    }

    public long getEnergyCapacity() {
        return energyStorage.getCapacity();
    }

    public int getFuel() {
        return fuel;
    }

    public boolean isWorking() {
        return isWorking;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Fuel", fuel);
        tag.putLong("Energy", energyStorage.getAmount());
        tag.putBoolean("IsWorking", isWorking);
        tag.putDouble("scex_chamber", chamber);
        tag.put("scex_ash", ash.serializeNBT(registries));
        if (energyStorage.scexNetworkControlled() || scexFuelCredit > 0) tag.putLong("scex_fuel_credit_eu", scexFuelCredit);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fuel = Math.max(0, tag.getInt("Fuel"));
        scexFuelCredit = com.miophas.singularity_iteration.core.api.util.BoundedUnits.clamp(tag.getLong("scex_fuel_credit_eu"), profile.production() - 1);
        chamber = tag.contains("scex_chamber") ? tag.getDouble("scex_chamber") : com.miophas.singularity_iteration.common.energy.IncineratorPhysics.AMBIENT;
        if (tag.contains("scex_ash")) ash.deserializeNBT(registries, tag.getCompound("scex_ash"));
        // The parent's lowercase energy + fraction is authoritative in the controlled format.
        if (tag.contains("Energy", net.minecraft.nbt.Tag.TAG_LONG)
                && (!energyStorage.scexNetworkControlled() || !tag.contains("energy", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC))) {
            energyStorage.setEnergy(tag.getLong("Energy"));
            energyStorage.scexLoadFraction(tag.getLong("scex_energy_fraction"));
        }
        if (tag.contains("IsWorking", net.minecraft.nbt.Tag.TAG_BYTE)) {
            isWorking = tag.getBoolean("IsWorking");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = super.getUpdateTag(registries);
        tag.putInt("Fuel", fuel);
        tag.putLong("scex_fuel_credit_eu", scexFuelCredit);
        tag.putBoolean("IsWorking", isWorking);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        fuel = Math.max(0, tag.getInt("Fuel"));
        scexFuelCredit = com.miophas.singularity_iteration.core.api.util.BoundedUnits.clamp(tag.getLong("scex_fuel_credit_eu"), profile.production() - 1);
        isWorking = tag.getBoolean("IsWorking");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.drop_generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.miophas.singularity_iteration.common.menu.generator.DropGeneratorMenu(containerId, playerInventory, this);
    }

    @Override
    public ICableTier getCableTier() {
        return profile.tier();
    }

    @Override
    public boolean isBurning() {
        return isWorking;
    }

    @Override
    public int getBurnTime() {
        return fuel;
    }

    @Override
    public int getMaxBurnTime() {
        return fuel;
    }

    @Override
    public long getPowerOutput() {
        return profile.production();
    }

    @Override
    public ItemStack getFuelSlotItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getChargeSlotItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public IItemHandler getItemHandler() {
        return ash;   // 0.1.11.19: the ash pit (taken out only)
    }

    @Override
    public int getDefaultBurnTime() {
        return fuel;
    }

    @Override
    public void setBurnTime(int ticks) {
        this.fuel = Math.max(0, ticks);
        this.setChanged();
    }

    @Override
    public java.util.List<com.miophas.singularity_iteration.common.area.WorkArea> workAreas() {
        return java.util.List.of(com.miophas.singularity_iteration.common.area.WorkArea.cube(worldPosition, profile.scanRange(), com.miophas.singularity_iteration.common.area.WorkArea.COLLECT));
    }

    /** 0.1.10.22 (G3): burns fuel only while the grid takes its energy, so the merit order uses it after the must-run sources. */
    @Override
    public com.miophas.singularity_iteration.core.api.energy.grid.DispatchClass dispatchClass() { return com.miophas.singularity_iteration.core.api.energy.grid.DispatchClass.DISPATCHABLE; }

    /** 0.1.11.13: comparator by the share of the energy storage filled. */
    @Override
    public int comparatorSignal() {
        return com.miophas.singularity_iteration.core.api.machine.IComparatorSource.fill(energyStorage.getAmount(), energyStorage.getCapacity());
    }
}
