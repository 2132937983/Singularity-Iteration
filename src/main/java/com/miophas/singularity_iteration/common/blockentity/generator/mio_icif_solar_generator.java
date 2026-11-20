package com.miophas.singularity_iteration.common.blockentity.generator;

import com.miophas.singularity_iteration.common.energy.SolarPhysics;
import com.miophas.singularity_iteration.common.energy.SolarProfile;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractGeneratorBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
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
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Every solar panel. 0.1.11.14: one implementation for the basic solar generator, the four advanced solar panels and the
 * three METS generators (they were three code paths, each with an old-grid and an independent-network branch, and they
 * disagreed on night, rain and shade). The others are subclasses that give their numbers.
 *
 * <p>The output follows {@link SolarPhysics}: the sun's elevation and the air mass, cloud cover from the weather, shade
 * (straight up open: everything; under cover with sky light around: the diffuse part), the cell temperature from the
 * biome, and inverter clipping at 700 W/m². A panel with a night core (the hybrid panels; the METS generators' night
 * share) gives its core output at night while it sees the sky. It is written through the framework's sink, so both energy
 * modes behave alike.
 *
 * <p>For the screen (a pyranometer record) it keeps today's irradiance and output in 96 points (every 250 ticks).
 */
@SuppressWarnings("null")
public class mio_icif_solar_generator extends AbstractGeneratorBlockEntity {

    public static final int SLOT_COUNT = 1;
    public static final int BATTERY_SLOT = 0;
    /** The basic panel: 1 EU/t, LV. */
    public static final long ENERGY_GENERATION_RATE = 1L;
    public static final long ENERGY_CAPACITY = 100000L;
    public static final long MAX_RECEIVE = 0L;
    public static final long MAX_EXTRACT = 32L;

    /** The environment is read every second; the record keeps a point every 250 ticks (96 a day). */
    public static final int REFRESH = 20, SAMPLE = 250, SAMPLES = 24000 / SAMPLE;
    /** Shade states for the screen. */
    public static final int OPEN = 0, DIFFUSE = 1, DARK = 2, NO_SKY = 3;
    /** 0.1.11.101: after the record, the profile for the screen: full-output level W/m², flags (1 direct light only, 2 held cell), cooling x1000. */
    public static final int DATA_PROFILE = 16 + SAMPLES / 2;
    /** 0.1.11.102: flag 4 marks a photon resonance panel (route B); then the night sky factor x1000. */
    public static final int DATA_COUNT = DATA_PROFILE + 4;

    protected final int dayPower, corePower, chargeSlots;
    /** 0.1.11.100: how this panel uses the sky (light, weather, temperature, night, packets). */
    protected final SolarProfile profile;
    private final boolean clampBasicBuffer;

    // the environment (server)
    private int refreshIn;
    private double irradiance, clearSky, cloud = 1, cellTemp = 25, derate = 1, elevation;
    private int exposure = DARK;
    private boolean coreActive;
    /** 0.1.11.102: the night output factor of the weather (1 clear). */
    private double nightSky = 1;
    private double output;
    private boolean isGenerating;
    // today's record
    private long recordDay = Long.MIN_VALUE;
    private final byte[] recordG = new byte[SAMPLES], recordOut = new byte[SAMPLES];
    private int recordIndex = -1;
    private double sumG, sumOut;
    private int sumCount;
    private double today;

    public mio_icif_solar_generator(BlockPos pos, BlockState state) {
        this(pos, state, null);
    }

    public mio_icif_solar_generator(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        this(pos, state, type, CableTier.LV);
    }

    public mio_icif_solar_generator(BlockPos pos, BlockState state, BlockEntityType<?> type, CableTier cableTier) {
        // the basic panel keeps the independent network's 2 EU buffer (its fractional output is taken every tick)
        this(pos, state, type, cableTier, (int) ENERGY_GENERATION_RATE, 0,
            com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy.controls(state) ? 2L : ENERGY_CAPACITY, MAX_EXTRACT, 1, 1, true);
    }

    /**
     * @param dayPower    rated output (EU/t) in full sun
     * @param corePower   output (EU/t) of the night core, 0 for none
     * @param slots       inventory slots (the advanced and METS panels keep 4, as saved)
     * @param chargeSlots slots that charge items; the rest are hidden and emptied
     */
    protected mio_icif_solar_generator(BlockPos pos, BlockState state, BlockEntityType<?> type, CableTier cableTier,
                                       int dayPower, int corePower, long capacity, long maxExtract, int slots, int chargeSlots, boolean clampBasicBuffer) {
        this(pos, state, type, cableTier, dayPower, corePower, capacity, maxExtract, slots, chargeSlots, clampBasicBuffer, SolarProfile.STANDARD);
    }

    /**
     * @param maxExtract EU a tick it may send; a profile with several packets needs the tier's packet times that many
     * @param profile    how the panel uses the sky (0.1.11.100)
     */
    protected mio_icif_solar_generator(BlockPos pos, BlockState state, BlockEntityType<?> type, CableTier cableTier,
                                       int dayPower, int corePower, long capacity, long maxExtract, int slots, int chargeSlots,
                                       boolean clampBasicBuffer, SolarProfile profile) {
        super(pos, state, type != null ? type : mio_icif_block_entities.SOLAR_GENERATOR_ENTITY_TYPE.get(),
              slots == 1 ? SlotLayout.builder().battery().build() : SlotLayout.builder().extra(slots).build(),
              dayPower, capacity, MAX_RECEIVE, maxExtract, cableTier);
        this.dayPower = dayPower;
        this.corePower = corePower;
        this.chargeSlots = Math.max(1, Math.min(slots, chargeSlots));
        this.clampBasicBuffer = clampBasicBuffer;
        this.profile = java.util.Objects.requireNonNull(profile, "solar profile");
        setAsPowerSource(maxExtract);
    }

    public static CableTier tierOf(int tier) {
        return switch (tier) {
            case 2 -> CableTier.MV;
            case 3 -> CableTier.HV;
            case 4 -> CableTier.EV;
            case 5 -> CableTier.IV;
            case 6 -> CableTier.LuV;
            case 7 -> CableTier.ZPMV;
            default -> CableTier.LV;
        };
    }

    // ---- the environment ---------------------------------------------------------------------------------------------

    /** Reads the sun, the weather, the shade and the temperature, and sets the output for the next second. */
    protected void sense(Level level) {
        long dayTime = level.getDayTime();
        elevation = SolarPhysics.elevation(dayTime);
        BlockPos up = worldPosition.above();
        int skyLight = 0;
        if (!level.dimensionType().hasSkyLight()) exposure = NO_SKY;
        else if (level.canSeeSky(up)) exposure = OPEN;
        else exposure = (skyLight = level.getBrightness(LightLayer.SKY, up)) > 0 ? DIFFUSE : DARK;
        var biome = level.getBiome(worldPosition).value();
        boolean precipitation = biome.hasPrecipitation();
        boolean wet = precipitation && level.isRaining();
        float rain = precipitation ? level.getRainLevel(1) : 0, thunder = precipitation ? level.getThunderLevel(1) : 0;
        clearSky = SolarPhysics.clearSky(elevation);
        cloud = precipitation ? profile.weather(SolarPhysics.cloudIndex(rain, thunder)) : 1;
        double shade = switch (exposure) {
            case OPEN -> profile.shade(true, 15);
            case DIFFUSE -> profile.shade(false, skyLight);
            default -> 0;
        };
        irradiance = clearSky * cloud * shade;
        cellTemp = profile.cellTemperature(SolarPhysics.ambient(biome.getBaseTemperature(), wet), irradiance);
        derate = profile.derate(cellTemp);
        coreActive = corePower > 0 && elevation <= 0 && exposure == OPEN;
        double pv = dayPower * profile.output(irradiance, derate);
        nightSky = profile.night(rain, thunder);
        double core = coreActive ? corePower * nightSky : 0;
        output = Math.max(pv, core) * outputScale();
    }

    // ---- tick --------------------------------------------------------------------------------------------------------

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_solar_generator be) {
        if (level.isClientSide()) return;
        boolean wasGenerating = be.isGenerating;
        be.chargeItems();
        be.ejectHiddenSlots();
        boolean network = be.energyStorage.scexNetworkControlled();
        if (!network && be.shouldDirectlyDistributeEnergy()) be.distributeEnergy();
        else if (!network) be.distributeEnergyToCompatSinks();
        if (be.refreshIn-- <= 0) {
            be.sense(level);
            be.refreshIn = REFRESH - 1;
        }
        if (network && be.clampBasicBuffer && be.output > 0
                && be.energyStorage.scexExactAmount().compareTo(com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyAmount.of(2)) > 0)
            be.energyStorage.setEnergy(2);
        boolean full = be.energyStorage.getAmount() >= be.energyStorage.getCapacity();
        double made = be.output > 0 && !full ? be.produce(be.output) : 0;
        be.isGenerating = made > 0;
        be.record(level.getDayTime(), made);
        if (wasGenerating != be.isGenerating) {
            BlockState now = level.getBlockState(pos);
            for (String name : new String[]{"active", "lit"}) {
                if (now.getBlock().getStateDefinition().getProperty(name) instanceof net.minecraft.world.level.block.state.properties.BooleanProperty flag
                        && now.getValue(flag) != be.isGenerating) {
                    level.setBlock(pos, now.setValue(flag, be.isGenerating), 3);
                    break;
                }
            }
        }
        com.miophas.singularity_iteration.core.runtime.TickMarks.saved(be);
    }

    /** One point of today's record every 250 ticks (the mean irradiance and output); a new day starts a clean sheet. */
    private void record(long dayTime, double made) {
        long day = Math.floorDiv(dayTime, 24000L);
        if (day != recordDay) {
            java.util.Arrays.fill(recordG, (byte) 0);
            java.util.Arrays.fill(recordOut, (byte) 0);
            recordDay = day;
            recordIndex = -1;
            sumG = sumOut = 0;
            sumCount = 0;
            today = 0;
        }
        today += made;
        sumG += irradiance;
        sumOut += dayPower > 0 ? made / (dayPower * Math.max(1e-9, outputScale())) : 0;
        sumCount++;
        int index = (int) (Math.floorMod(dayTime, 24000L) / SAMPLE);
        if (index != recordIndex) {
            if (recordIndex >= 0 && sumCount > 0) {
                recordG[recordIndex] = ink(sumG / sumCount / 1016.0);
                recordOut[recordIndex] = ink(sumOut / sumCount);
            }
            recordIndex = index;
            sumG = sumOut = 0;
            sumCount = 0;
        }
    }

    /** 0 is no record; 1..255 is 0..1. */
    private static byte ink(double v) {
        return (byte) (1 + (int) Math.round(Math.max(0, Math.min(1, v)) * 254));
    }

    // ---- items ---------------------------------------------------------------------------------------------------------

    @Override
    protected void chargeItems() {
        if (energyStorage.scexNetworkControlled()) {
            if (com.miophas.singularity_iteration.core.runtime.energy.SolarItemCharging.charge(itemHandler, chargeSlots, energyStorage, getItemAPI())) setChanged();
            return;
        }
        var api = getItemAPI();
        for (int i = 0; i < chargeSlots; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty() || !api.isBattery(stack)) continue;
            long stored = api.getBatteryStored(stack), max = api.getBatteryCapacity(stack);
            long available = energyStorage.getAmount();
            if (stored >= max || available <= 0) continue;
            long offer = Math.min(Math.min(api.getChargeRate(stack), max - stored), available);
            offer = Math.min(offer, Math.max(0, api.chargeBattery(stack, offer, true)));
            long taken = offer > 0 ? getEnergyStorageInternal().extract(offer, false) : 0;
            long accepted = taken > 0 ? api.chargeBattery(stack, taken, false) : 0;
            if (accepted < taken) getEnergyStorageInternal().generateEnergyInternal(taken - accepted, false);
            setChanged();
        }
    }

    /** Items left in hidden slots (the METS generators' old discharge slot) are dropped so nothing is lost. */
    private void ejectHiddenSlots() {
        if (level == null) return;
        for (int i = chargeSlots; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
            net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, stack);
            setChanged();
        }
    }

    @Override
    public int getFuelBurnTime(ItemStack fuel) { return 0; }

    @Override
    public int[] getSlotsForFace(Direction side) {
        int[] slots = new int[chargeSlots];
        for (int i = 0; i < chargeSlots; i++) slots[i] = i;
        return slots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot >= 0 && slot < chargeSlots && isBattery(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= 0 && slot < chargeSlots;
    }

    // ---- state -------------------------------------------------------------------------------------------------------

    @Override
    public boolean isBurning() { return isGenerating; }

    @Override
    public boolean isGenerating() { return isGenerating; }

    /** EU/t now (the configured multiplier included). */
    @Override
    public long getPowerOutput() { return Math.round(output); }

    public int getDayPower() { return dayPower; }
    public SolarProfile getProfile() { return profile; }

    /** 0.1.11.100: a profile may send several packets of its tier a tick (the network reads this). */
    @Override
    public int getPacketCount() { return profile.packets(); }
    public int getCorePower() { return corePower; }
    public int getChargeSlots() { return chargeSlots; }
    public double getIrradiance() { return irradiance; }
    public double getOutput() { return output; }
    public int getExposure() { return exposure; }

    /**
     * Menu data: 0 irradiance W/m², 1 clear-sky W/m², 2 output EU/t x100, 3 rated EU/t x100, 4 night core EU/t x100,
     * 5 cell °C x10, 6 temperature factor x1000, 7 cloud index x1000, 8 shade state, 9 time of day, 10 energy,
     * 11 capacity, 12 today's EU, 13 elevation x10, 14 charge slots, 15 core on, 16.. today's record (two points an int),
     * then {@link #DATA_PROFILE}: full-output level W/m², flags (1 direct light only, 2 held cell, 4 photon resonance),
     * cooling x1000, night sky factor x1000.
     */
    public ContainerData getContainerData() {
        return new ContainerData() {
            @Override public int getCount() { return DATA_COUNT; }
            @Override public void set(int i, int v) { }
            @Override public int get(int i) {
                double scale = outputScale();
                return switch (i) {
                    case 0 -> (int) Math.round(irradiance);
                    case 1 -> (int) Math.round(clearSky);
                    case 2 -> (int) Math.round(output * 100);
                    case 3 -> (int) Math.round(dayPower * scale * 100);
                    case 4 -> (int) Math.round(corePower * scale * 100);
                    case 5 -> (int) Math.round(cellTemp * 10);
                    case 6 -> (int) Math.round(derate * 1000);
                    case 7 -> (int) Math.round(cloud * 1000);
                    case 8 -> exposure;
                    case 9 -> level == null ? 0 : (int) Math.floorMod(level.getDayTime(), 24000L);
                    case 10 -> (int) Math.min(Integer.MAX_VALUE, energyStorage.getAmount());
                    case 11 -> (int) Math.min(Integer.MAX_VALUE, energyStorage.getCapacity());
                    case 12 -> (int) Math.min(Integer.MAX_VALUE, Math.round(today));
                    case 13 -> (int) Math.round(elevation * 10);
                    case 14 -> chargeSlots;
                    case 15 -> coreActive ? 1 : 0;
                    case DATA_PROFILE -> (int) Math.round(profile.clip());
                    case DATA_PROFILE + 1 -> (profile.directOnly() ? 1 : 0) | (profile.fixedCell() ? 2 : 0)
                        | (com.miophas.singularity_iteration.common.energy.SolarRoutes.photonResonance(profile) ? 4 : 0);
                    case DATA_PROFILE + 2 -> (int) Math.round(com.miophas.singularity_iteration.common.energy.SolarRoutes.cooling(profile) * 1000);
                    case DATA_PROFILE + 3 -> (int) Math.round(nightSky * 1000);
                    default -> {
                        int k = (i - 16) * 2;
                        if (k < 0 || k + 1 >= SAMPLES) yield 0;
                        yield (recordG[k] & 0xFF) | (recordOut[k] & 0xFF) << 8 | (recordG[k + 1] & 0xFF) << 16 | (recordOut[k + 1] & 0xFF) << 24;
                    }
                };
            }
        };
    }

    // ---- save ----------------------------------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("IsGenerating", isGenerating);
        tag.putLong("SolarDay", recordDay);
        tag.putByteArray("SolarRecordG", recordG);
        tag.putByteArray("SolarRecordOut", recordOut);
        tag.putDouble("SolarToday", today);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        isGenerating = tag.getBoolean("IsGenerating");
        recordDay = tag.contains("SolarDay") ? tag.getLong("SolarDay") : Long.MIN_VALUE;
        byte[] g = tag.getByteArray("SolarRecordG"), o = tag.getByteArray("SolarRecordOut");
        if (g.length == SAMPLES && o.length == SAMPLES) {
            System.arraycopy(g, 0, recordG, 0, SAMPLES);
            System.arraycopy(o, 0, recordOut, 0, SAMPLES);
        }
        today = tag.getDouble("SolarToday");
        refreshIn = 0;   // read the world on the first tick, never a saved value
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("IsGenerating", isGenerating);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        isGenerating = tag.getBoolean("IsGenerating");
    }

    @Override
    public Component getDisplayName() {
        // 0.1.11.14: the block's own name (the advanced panels showed "Solar Generator")
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.miophas.singularity_iteration.common.menu.generator.SolarGeneratorMenu(containerId, playerInventory, this);
    }
}
