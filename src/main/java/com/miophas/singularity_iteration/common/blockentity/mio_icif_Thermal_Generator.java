package com.miophas.singularity_iteration.common.blockentity;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractGeneratorBlockEntity;

import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Thermal generator block entity (LV tier).
 * It burns coal and other fuels to make energy.
 * It uses the EU (Energy Unit) energy system.
 * LV tier settings: 128 EU/tick output, 4000 EU capacity, generation rate 10 EU/tick.
 * 
 * This generator sends energy through the power grid (PowerGrid). It does not send energy directly to adjacent blocks.
 * The grid uses extractPowerForConsumer to take energy from the energyStorage of the generator.
 */
@SuppressWarnings("null")
public class mio_icif_Thermal_Generator extends AbstractGeneratorBlockEntity implements com.miophas.singularity_iteration.core.api.energy.grid.IGridDispatch {
    
    // LV tier: matches original IC2. Capacity is 4000 EU. Generation rate is 10 EU/tick. Maximum output is 32 EU/tick.
    public mio_icif_Thermal_Generator(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.THERMAL_GENERATOR_ENTITY_TYPE.get(),
            SlotLayout.builder().input(1).battery().build(), 10L, 4000L, 0L, 32L, CableTier.LV);
    }

    public mio_icif_Thermal_Generator(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            SlotLayout.builder().input(1).battery().build(), 10L, 4000L, 0L, 32L, CableTier.LV);
    }
    
    /**
     * The block entity registers to the grid when it loads.
     * The base class AbstractEnergyBlockEntity does the registration automatically through EnergyTileLoadEvent.
     */

    /**
     * The thermal generator sends energy through the grid. It does not send energy directly to adjacent blocks.
     * The grid uses extractPowerForConsumer to take energy from the storage of the generator.
     */
    @Override
    protected boolean shouldDirectlyDistributeEnergy() {
        return false;
    }
    
    /**
     * Gets the burn time of the fuel.
 * Like the vanilla furnace, it burns the item to make energy if the item is a fuel.
     * @param fuel the fuel item
     * @return the burn time in ticks. 0 means that the item is not a fuel.
     */
    @Override
    public int getFuelBurnTime(ItemStack fuel) {
        return fuelBurnTime(fuel, energyStorage.scexNetworkControlled());
    }

    /** Shared fuel rule for the machine and its client menu prediction. */
    public static int fuelBurnTime(ItemStack fuel, boolean independentNetwork) {
        if (fuel.isEmpty()) {
            return 0;
        }

        if (independentNetwork && fuel.is(net.minecraft.world.item.Items.LAVA_BUCKET)) {
            return 0;
        }
        int burnTime = fuel.getBurnTime(null);
        // R12 public game observations: coal/charcoal 400, stick 25, planks 75,
        // carpet 16 and slab 37 ticks. New-platform/addon fuels remain extensions.
        if (independentNetwork) burnTime /= 4;
        return burnTime > 0 ? burnTime : 0;
    }
    
    /**
     * 获取发电机的显示名称
     * @return 显示名称
     */
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.thermal_generator");
    }
    
    /**
     * Creates the menu.
     * @param id the menu ID
     * @param playerInventory the player inventory
     * @param player the player
     * @return the menu instance
     */
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new com.miophas.singularity_iteration.common.menu.generator.ThermalGeneratorMenu(id, playerInventory, this, this.getItemHandler(), getContainerData());
    }

    /** 0.1.10.22 (G3): burns fuel only while the grid takes its energy, so the merit order uses it after the must-run sources. */
    @Override
    public com.miophas.singularity_iteration.core.api.energy.grid.DispatchClass dispatchClass() { return com.miophas.singularity_iteration.core.api.energy.grid.DispatchClass.DISPATCHABLE; }

    // ---- 0.1.11.10: furnace temperature, the damper, the chart recorder, the comparator ----------------------------

    /** Ticks from cold to full temperature while firing, and from full to cold without fuel. */
    public static final int WARM_UP = 200, COOL_DOWN = 600;
    /** A banked fire (damper closed, fuel left) cools down to this and holds. */
    public static final double BANKED_HEAT = 0.35;
    /** The damper opens again when the storage falls below this share. */
    public static final double REOPEN = 0.85;
    /** Chart: one revolution is one game day; a point every 300 ticks (15 s), 80 points. */
    public static final int REVOLUTION = 24000, SAMPLE = 300, SAMPLES = REVOLUTION / SAMPLE;

    public static final int DATA_COUNT = 12 + SAMPLES / 2;

    /** Furnace temperature 0..1 (0 cold, 1 full output). */
    private double heat;
    /** Burn time left of the current lump; it goes down by {@link #heat} a tick, so a lump gives the same EU at any temperature. */
    private double burnLeft;
    private boolean banked;
    private double euCarry;
    private double lastOutput;
    private final byte[] chartHeat = new byte[SAMPLES], chartOutput = new byte[SAMPLES];
    private long chartIndex = Long.MIN_VALUE;   // no point yet
    private double sumHeat, sumOutput;
    private int sumCount;
    private int lastSignal = -1;

    public double getHeat() { return heat; }
    public boolean isBanked() { return banked; }
    public double getBurnLeft() { return burnLeft; }
    public double getLastOutput() { return lastOutput; }

    /** Full output in EU/t (the configured output multiplier included). */
    public double maxOutput() { return energyGenerationRate * outputScale(); }

    /** 300 + 1000 x heat kelvin: a cold furnace at room temperature, a full one at 1300 K (a coal bed burns at 1200..1500 K). */
    public static int kelvin(double heat) { return (int) Math.round(300 + 1000 * heat); }

    /** Fuel ticks left at full temperature: the current lump plus the fuel slot. */
    public double fuelTicksLeft() {
        ItemStack fuel = itemHandler != null ? itemHandler.getStackInSlot(FUEL_SLOT) : ItemStack.EMPTY;
        int per = getFuelBurnTime(fuel);
        return Math.max(0, burnLeft) + (per > 0 ? (double) per * fuel.getCount() : 0);
    }

    /** 0 without fuel; 1..15 with the fuel left (a full stack of coal gives 15). */
    public int comparatorSignal() {
        double t = fuelTicksLeft();
        if (t <= 0) return 0;
        // a full stack of coal (under this energy mode's fuel rule) gives 15
        double full = 64.0 * Math.max(1, fuelBurnTime(new ItemStack(net.minecraft.world.item.Items.COAL), energyStorage.scexNetworkControlled()));
        return 1 + (int) Math.min(14, Math.floor(14 * t / full));
    }

    /** Ticker of the block (replaces the shared generator tick). */
    public static void tickThermal(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, mio_icif_Thermal_Generator be) {
        if (level.isClientSide()) return;
        be.tickFurnace(level, pos);
        be.record(level.getGameTime());
        int signal = be.comparatorSignal();
        if (signal != be.lastSignal) {
            be.lastSignal = signal;
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
        com.miophas.singularity_iteration.core.runtime.TickMarks.saved(be);
    }

    /**
     * One tick of the furnace, in both energy modes. The independent network keeps its fuel rule (a quarter of the
     * furnace burn time) and does all wire transfers itself; the old grid also gets the compatible neighbours here.
     */
    private void tickFurnace(net.minecraft.world.level.Level level, BlockPos pos) {
        boolean network = energyStorage.scexNetworkControlled();
        chargeItems();
        if (!network) distributeEnergyToCompatSinks();
        long stored = energyStorage.getAmount(), cap = energyStorage.getCapacity();
        long room = network ? energyStorage.scexExactAmount().roomBelow(cap).whole() : cap - stored;
        // the damper: shut when the storage is full (the fuel waits instead of burning for nothing), open again below 85 %
        if (room <= 0) banked = true;
        else if (banked && stored < cap * REOPEN) banked = false;
        if (!banked && burnLeft <= 0) {
            burnTime = 0;
            consumeFuel();
            burnLeft = burnTime;
        }
        boolean firing = burnLeft > 0 && !banked;
        if (firing) {
            heat = Math.min(1, heat + 1.0 / WARM_UP);
            lastOutput = maxOutput() * heat;
            if (network) {
                energyStorage.scexGenerateEnergy(com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyAmount.fromDouble(lastOutput), false);
            } else {
                double eu = lastOutput + euCarry;
                long whole = (long) Math.floor(eu);
                euCarry = eu - whole;
                whole = Math.min(whole, room);
                if (whole > 0) apiGenerateEnergy(whole);
            }
            burnLeft = Math.max(0, burnLeft - heat);
        } else {
            lastOutput = 0;
            double floor = burnLeft > 0 ? BANKED_HEAT : 0;
            if (heat > floor) heat = Math.max(floor, heat - 1.0 / COOL_DOWN);
        }
        burnTime = (int) Math.ceil(burnLeft);
        updateActiveState(level, pos, firing);
    }

    /** One point of the chart every {@link #SAMPLE} ticks: the mean temperature and output; a gap (unloaded) is blank. */
    private void record(long gameTime) {
        sumHeat += heat;
        double max = maxOutput();
        sumOutput += max > 0 ? lastOutput / max : 0;
        sumCount++;
        long index = Math.floorDiv(gameTime, (long) SAMPLE);
        if (index == chartIndex) return;
        if (chartIndex != Long.MIN_VALUE && index > chartIndex) {
            int i = (int) Math.floorMod(chartIndex, (long) SAMPLES);
            chartHeat[i] = ink(sumHeat / sumCount);
            chartOutput[i] = ink(sumOutput / sumCount);
            for (long k = chartIndex + 1; k < index && k <= chartIndex + SAMPLES; k++) {
                int j = (int) Math.floorMod(k, (long) SAMPLES);
                chartHeat[j] = 0;
                chartOutput[j] = 0;
            }
        }
        chartIndex = index;
        sumHeat = sumOutput = 0;
        sumCount = 0;
    }

    /** 0 is blank paper; 1..255 is ink at 0..1. */
    private static byte ink(double v) {
        return (byte) (1 + (int) Math.round(Math.max(0, Math.min(1, v)) * 254));
    }

    /**
     * Menu data: 0 firing, 1 heat x1000, 2 output EU/t x100, 3 energy, 4 capacity, 5 banked, 6 burn left (ticks),
     * 7 burn duration, 8 fuel in the slot, 9 burn ticks of one, 10 time in the revolution, 11 full output EU/t x100,
     * 12.. the chart, two points (heat, output, heat, output bytes) an int.
     */
    public net.minecraft.world.inventory.ContainerData getContainerData() {
        return new net.minecraft.world.inventory.ContainerData() {
            @Override public int getCount() { return DATA_COUNT; }
            @Override public void set(int i, int v) { }
            @Override public int get(int i) {
                ItemStack fuel = itemHandler.getStackInSlot(FUEL_SLOT);
                return switch (i) {
                    case 0 -> burnLeft > 0 && !banked && lastOutput > 0 ? 1 : 0;
                    case 1 -> (int) Math.round(heat * 1000);
                    case 2 -> (int) Math.round(lastOutput * 100);
                    case 3 -> (int) Math.min(Integer.MAX_VALUE, energyStorage.getAmount());
                    case 4 -> (int) Math.min(Integer.MAX_VALUE, energyStorage.getCapacity());
                    case 5 -> banked ? 1 : 0;
                    case 6 -> (int) Math.ceil(Math.max(0, burnLeft));
                    case 7 -> burnDuration;
                    case 8 -> fuel.getCount();
                    case 9 -> getFuelBurnTime(fuel);
                    case 10 -> level == null ? 0 : (int) (level.getGameTime() % REVOLUTION);
                    case 11 -> (int) Math.round(maxOutput() * 100);
                    default -> {
                        int k = (i - 12) * 2;
                        if (k < 0 || k + 1 >= SAMPLES) yield 0;
                        yield (chartHeat[k] & 0xFF) | (chartOutput[k] & 0xFF) << 8 | (chartHeat[k + 1] & 0xFF) << 16 | (chartOutput[k + 1] & 0xFF) << 24;
                    }
                };
            }
        };
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("Heat", heat);
        tag.putDouble("BurnLeft", burnLeft);
        tag.putBoolean("Banked", banked);
        tag.putByteArray("ChartHeat", chartHeat);
        tag.putByteArray("ChartOutput", chartOutput);
        tag.putLong("ChartIndex", chartIndex);
    }

    @Override
    public void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // saves before 0.1.11.10: a lump burning keeps burning at full temperature
        burnLeft = tag.contains("BurnLeft") ? tag.getDouble("BurnLeft") : burnTime;
        heat = tag.contains("Heat") ? tag.getDouble("Heat") : (burnTime > 0 ? 1 : 0);
        banked = tag.getBoolean("Banked");
        byte[] h = tag.getByteArray("ChartHeat"), o = tag.getByteArray("ChartOutput");
        if (h.length == SAMPLES && o.length == SAMPLES) {
            System.arraycopy(h, 0, chartHeat, 0, SAMPLES);
            System.arraycopy(o, 0, chartOutput, 0, SAMPLES);
        }
        chartIndex = tag.contains("ChartIndex") ? tag.getLong("ChartIndex") : Long.MIN_VALUE;
    }
}
