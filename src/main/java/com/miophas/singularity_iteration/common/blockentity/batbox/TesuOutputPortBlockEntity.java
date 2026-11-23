package com.miophas.singularity_iteration.common.blockentity.batbox;

import com.miophas.singularity_iteration.common.menu.wiring.TesuOutputMenu;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.api.energy.grid.IEnergyAcceptor;
import com.miophas.singularity_iteration.core.api.energy.grid.IEnergyEmitter;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.prefab.blockentity.MultiblockEnergyPart;
import com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy;
import com.miophas.singularity_iteration.core.runtime.energy.grid.EnergyNetGlobal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.157: the TESU output port. As a GESU output module it is a relay buffer of two packets, refilled from its controller
 * every tick and drained by the network, one packet a tick; unlike them its voltage is set on the port, from IV to MAX. The
 * packet the network sends is the voltage's: the port reports it as its cable tier.
 *
 * <p>0.1.11.191: a screen sets the output in EU/t ({@link TesuOutputMenu}, like the Creative Generator's), from 0 to
 * {@link #MAX_RATE}, and switches it on and off. The voltage follows the output: the lowest tier from IV up whose packet holds
 * it, so a small output does not come at a needlessly high voltage. One packet of the output a tick; the buffer holds two.
 * A port saved before (a voltage, no output) sends the full packet of its voltage, as it did. The right click opens the
 * screen (it stepped the voltage).
 */
@SuppressWarnings("null")
public class TesuOutputPortBlockEntity extends MultiblockEnergyPart implements com.miophas.singularity_iteration.core.api.machine.IStorageMember {

    /** The voltages the port can be set to, IV first. */
    public static final String[] TIERS = {"iv", "luv", "zpmv", "uv", "uhv", "uev", "uiv", "uxv", "opv", "max"};
    private static final ICableTier MAX_TIER = MioIcifAPI.instance().getEnergyNetAPI().getCableTier("max");
    /** 0.1.11.191: the highest output, one packet of MAX a tick. */
    public static final long MAX_RATE = tierOf(TIERS.length - 1).getPowerRating();
    /** 0.1.11.191: data slots of the screen: output (4 x 16 bits), switch, EU sent in the last tick (4), voltage, formed, TESU stored (4), capacity (4). */
    public static final int DATA_COUNT = 19;

    private int tierIndex;
    private long rate = tierOf(0).getPowerRating();
    private boolean enabled = true;
    private boolean sourceRole;
    /** Balance after the last refill, and the EU that left the buffer since (shown in the screen). */
    private long refilledTo, lastSent;

    public TesuOutputPortBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.TESU_OUTPUT.get(), tierOf(0).getPowerRating() * 2, 0, 0, MAX_TIER);
    }

    public static ICableTier tierOf(int index) {
        return MioIcifAPI.instance().getEnergyNetAPI().getCableTier(TIERS[Math.max(0, Math.min(TIERS.length - 1, index))]);
    }

    /** 0.1.11.191: the lowest voltage (from IV) whose packet holds {@code output}. */
    public static int tierFor(long output) {
        for (int i = 0; i < TIERS.length; i++) if (tierOf(i).getPowerRating() >= output) return i;
        return TIERS.length - 1;
    }

    public ICableTier tier() { return tierOf(tierIndex); }
    public int tierIndex() { return tierIndex; }
    /** EU a tick: the set output (0.1.11.191; it was the voltage's full packet). */
    public long rate() { return rate; }
    public boolean enabled() { return enabled; }
    public long lastSent() { return lastSent; }
    /** What leaves a tick: the output while switched on, else nothing. */
    private long output() { return enabled ? rate : 0L; }

    /** Steps the voltage up (or down), wrapping round, the output its full packet; returns the new voltage. */
    public ICableTier step(boolean down) {
        setTierIndex(Math.floorMod(tierIndex + (down ? -1 : 1), TIERS.length));
        return tier();
    }

    /** The voltage {@code index} with its full packet as the output. */
    public void setTierIndex(int index) {
        configure(tierOf(index).getPowerRating(), enabled);
    }

    /** 0.1.11.191: sets the output (clamped to 0 .. {@link #MAX_RATE}) and the switch; the voltage follows. Server side. */
    public void configure(long newRate, boolean on) {
        long clamped = Math.max(0L, Math.min(MAX_RATE, newRate));
        if (clamped == rate && on == enabled) return;
        rate = clamped;
        enabled = on;
        tierIndex = tierFor(rate);
        applyRate();
        setChanged();
        if (level != null && !level.isClientSide()) {
            IndependentSiEnergy.changed(this);   // a new packet size: republish
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    /** The buffer holds two packets of the output; what no longer fits goes back to the controller. */
    private void applyRate() {
        long packet = Math.max(1L, rate);
        energyStorage.setCapacity(packet * 2);
        if (sourceRole) {
            setAsPowerSource(packet);
            energyStorage.setMaxExtract(output());
        }
        long over = getStoredEnergy() - (enabled ? packet * 2 : 0L);
        TesuControllerBlockEntity core = controller();
        if (over > 0 && core != null) {
            long back = core.generateEnergyInternal(over, false);
            if (back > 0) consumeEnergy(back, false);
        }
    }

    @Override
    public ICableTier getEffectiveCableTier() { return tier(); }

    @Override
    public BlockEntity storageOwner() { return controller(); }

    @Nullable
    TesuControllerBlockEntity controller() {
        Level level = getLevel();
        if (level == null || !isStructureCompleted() || getCorePosition() == null || !level.isLoaded(getCorePosition())) return null;
        return level.getBlockEntity(getCorePosition()) instanceof TesuControllerBlockEntity core && core.isStructureComplete() ? core : null;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TesuOutputPortBlockEntity port) {
        if (level.isClientSide()) return;
        port.lastSent = Math.max(0L, port.refilledTo - port.getStoredEnergy());
        TesuControllerBlockEntity core = port.controller();
        boolean formed = core != null;
        if (formed != port.sourceRole) {
            port.sourceRole = formed;
            if (formed) port.setAsPowerSource(Math.max(1L, port.rate));
            else port.setAsConsumer();
            port.energyStorage.setMaxExtract(formed ? port.output() : 0);
            port.setChanged();
            IndependentSiEnergy.changed(port);      // republish faces: source <-> consumer
        } else if (formed && !port.isPowerSource()) {
            port.setAsPowerSource(Math.max(1L, port.rate));     // restored from a save
            port.energyStorage.setMaxExtract(port.output());
        }
        if (formed && port.enabled) port.refill(core);
        port.refilledTo = port.getStoredEnergy();
    }

    /** Tops the relay buffer up from the controller (at most two packets). */
    private void refill(TesuControllerBlockEntity core) {
        long space = getEnergyCapacity() - getStoredEnergy();
        if (space <= 0) return;
        long want = Math.min(space, Math.min(core.getStoredEnergy(), Math.max(1L, rate) * 2));
        if (want <= 0) return;
        long taken = core.consumeEnergy(want, false);
        if (taken > 0) {
            long stored = generateEnergyInternal(taken, false);
            if (stored < taken) core.generateEnergyInternal(taken - stored, false);   // never lose energy
            core.setChanged();
            setChanged();
        }
    }

    // ------------------------------------------------------------------ screen (0.1.11.191)

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            if (index < 4) return part(rate, index);
            if (index == 4) return enabled ? 1 : 0;
            if (index < 9) return part(lastSent, index - 5);
            if (index == 9) return tierIndex;
            TesuControllerBlockEntity core = controller();
            if (index == 10) return core != null ? 1 : 0;
            if (index < 15) return part(core == null ? 0L : core.getStoredEnergy(), index - 11);
            return part(core == null ? 0L : core.getEnergyCapacity(), index - 15);
        }
        @Override public void set(int index, int value) { }
        @Override public int getCount() { return DATA_COUNT; }
    };

    private static int part(long value, int i) { return (int) ((value >>> (16 * i)) & 0xFFFF); }

    /** Joins four 16-bit data slots (from {@code first}) into a long. */
    public static long join(ContainerData data, int first) {
        long value = 0;
        for (int i = 0; i < 4; i++) value |= (long) (data.get(first + i) & 0xFFFF) << (16 * i);
        return value;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new TesuOutputMenu(id, playerInventory, this, data);
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.tesu_output");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("TesuTier", TIERS[tierIndex]);
        tag.putLong("TesuRate", rate);       // 0.1.11.191
        tag.putBoolean("TesuOn", enabled);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int index = Math.max(0, java.util.Arrays.asList(TIERS).indexOf(tag.getString("TesuTier")));
        // 0.1.11.191: the set output; a port saved before has its voltage's full packet
        rate = tag.contains("TesuRate") ? Math.max(0L, Math.min(MAX_RATE, tag.getLong("TesuRate"))) : tierOf(index).getPowerRating();
        enabled = !tag.contains("TesuOn") || tag.getBoolean("TesuOn");
        tierIndex = tierFor(rate);
        energyStorage.setCapacity(Math.max(1L, rate) * 2);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putString("TesuTier", TIERS[tierIndex]);
        tag.putLong("TesuRate", rate);
        tag.putBoolean("TesuOn", enabled);
        return tag;
    }

    // ---- legacy grid API (used only when the independent engine is disabled)
    @Override public double getOfferedEnergy() { return isPowerSource() ? Math.min(output(), getStoredEnergy()) : 0.0D; }
    @Override public void drawEnergy(double amount) { if (isPowerSource() && amount > 0) consumeEnergy((long) amount, false); }
    @Override public int getSourceTier() { return isPowerSource() ? EnergyNetGlobal.cableTierToSourceTier((CableTier) tier()) : -1; }
    @Override public boolean emitsEnergyTo(IEnergyAcceptor acceptor, Direction direction) { return isPowerSource(); }
    @Override public double getDemandedEnergy() { return 0.0D; }
    @Override public double injectEnergy(Direction direction, double amount, double voltage) { return amount; }
    @Override public boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction direction) { return false; }
}
