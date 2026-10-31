package com.miophas.singularity_iteration.common.blockentity.generator;

import com.miophas.singularity_iteration.common.block.generator.mio_icif_block_steam_tunnel;
import com.miophas.singularity_iteration.common.block.generator.mio_icif_block_steam_tunnel_part;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.core.runtime.TickMarks;
import com.miophas.singularity_iteration.core.runtime.reactor.SteamReactorCycle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.39: the controller of the Steam Tunnel, IC2 Classic's large steam turbine ({@code SteamTunnelTileEntity}). With its
 * structure formed ({@link SteamTunnelStructure}) it turns steam into EU in steps of 1,024 mB into 512 EU, up to 4 steps a tick
 * (4,096 mB/t into 2,048 EU/t, the 2 mB per EU of the small turbine). As in IC2 Classic it runs while its 10,000 EU buffer has room
 * for one step and then makes all the steps the steam allows, so the buffer may go over by up to 3 steps ({@link #MAX_ENERGY}).
 * The power ports of the shell take the EU and send it as HV packets. The steam comes back as the water it was boiled from: IC2
 * Classic boils 1 mB of water into 160 mB of steam and returns 6.4 mB per 1,024 mB, SI boils at 1:100
 * ({@link SteamReactorCycle#STEAM_PER_WATER}) and returns 1 mB per 100 mB, so a reactor loop loses no water either way. Water that
 * does not fit its 2,000 mB tank is lost, and the fluid ports push it into pipes. Steam enters through the controller or a fluid
 * port. No menu: Jade and a right-click show the state. Formed, its front shows one big fan: the controller is the hub, the 8
 * casings around it the blades ({@link #showFan}); it spins and blows steam while the tunnel runs.
 */
public class SteamTunnelBlockEntity extends BlockEntity implements IFluidHandler {
    public static final int STEAM_CAPACITY = 16_000, WATER_CAPACITY = 2_000;
    public static final int STEAM_PER_STEP = 1_024, MAX_STEPS = 4, CHECK_TICKS = 20;
    public static final long EU_PER_STEP = 512L, ENERGY_CAPACITY = 10_000L;
    /** The most the buffer can hold: room for one step lets all 4 run (IC2 Classic). */
    public static final long MAX_ENERGY = ENERGY_CAPACITY + (MAX_STEPS - 1) * EU_PER_STEP;
    /** Running (fan spinning, steam blowing) until this long after the last step: steam that comes in bursts keeps it running. */
    public static final int RUN_HOLD_TICKS = 40;
    /** Steam particles a tick out of the front of a running tunnel (client). */
    public static final int PARTICLES_PER_TICK = 3;

    private final FluidTank steam = new FluidTank(STEAM_CAPACITY, stack -> mio_icif_steam_turbine.steam(stack.getFluid())) {
        @Override protected void onContentsChanged() { TickMarks.saved(SteamTunnelBlockEntity.this); }
    };
    private final FluidTank water = new FluidTank(WATER_CAPACITY, stack -> stack.getFluid().isSame(Fluids.WATER)) {
        @Override protected void onContentsChanged() { TickMarks.saved(SteamTunnelBlockEntity.this); }
    };
    private long energy;
    /** Steam already turned into EU whose water (less than 1 mB) is still owed. */
    private int condensate;
    private boolean formed;
    private final List<BlockPos> ports = new ArrayList<>();
    private int checkIn, runHold;
    /** EU made this second so far, and in the last whole second (for the output per tick). */
    private long madeThisSecond, madeLastSecond;
    /** Why the structure is not formed (a lang key and a position), or null. */
    private @Nullable String problem;
    private @Nullable BlockPos problemAt;

    public SteamTunnelBlockEntity(BlockPos pos, BlockState state) {
        super(mio_icif_block_entities.STEAM_TUNNEL_ENTITY_TYPE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SteamTunnelBlockEntity tunnel) {
        if (level.isClientSide) return;
        if (--tunnel.checkIn <= 0) {
            tunnel.checkIn = CHECK_TICKS;
            tunnel.validate();
        }
        if (tunnel.formed && tunnel.energy + EU_PER_STEP <= ENERGY_CAPACITY && tunnel.steam.getFluidAmount() >= STEAM_PER_STEP) {
            int steps = Math.min(MAX_STEPS, tunnel.steam.getFluidAmount() / STEAM_PER_STEP);   // IC2 Classic: not capped by the room left
            tunnel.energy += EU_PER_STEP * steps;
            tunnel.steam.drain(STEAM_PER_STEP * steps, FluidAction.EXECUTE);
            tunnel.condensate += STEAM_PER_STEP * steps;
            int made = tunnel.condensate / SteamReactorCycle.STEAM_PER_WATER;
            tunnel.condensate %= SteamReactorCycle.STEAM_PER_WATER;
            tunnel.water.fill(new FluidStack(Fluids.WATER, made), FluidAction.EXECUTE);   // what does not fit is lost
            tunnel.madeThisSecond += EU_PER_STEP * steps;
            tunnel.runHold = RUN_HOLD_TICKS;
        } else if (tunnel.runHold > 0) {
            tunnel.runHold--;
        }
        if (level.getGameTime() % 20 == 0) {
            tunnel.madeLastSecond = tunnel.madeThisSecond;
            tunnel.madeThisSecond = 0;
        }
        tunnel.setActive(tunnel.formed && tunnel.runHold > 0);
    }

    /** Running or not: the controller's state, and the fan pieces spin with it. */
    private void setActive(boolean active) {
        BlockState state = getBlockState();
        if (!state.hasProperty(mio_icif_block_steam_tunnel.ACTIVE) || state.getValue(mio_icif_block_steam_tunnel.ACTIVE) == active) return;
        level.setBlock(worldPosition, state.setValue(mio_icif_block_steam_tunnel.ACTIVE, active), 3);
        showFan(level, worldPosition, state.getValue(mio_icif_block_steam_tunnel.FACING), formed, active);
    }

    /** Client: a running tunnel blows steam out of its fan, a disc 2.5 blocks across in front of the controller. */
    public static void clientTick(Level level, BlockPos pos, BlockState state) {
        if (!state.getValue(mio_icif_block_steam_tunnel.ACTIVE) || !state.getValue(mio_icif_block_steam_tunnel.FORMED)) return;
        Direction front = state.getValue(mio_icif_block_steam_tunnel.FACING), side = front.getClockWise();
        RandomSource random = level.random;
        for (int i = 0; i < PARTICLES_PER_TICK; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI, radius = Math.sqrt(random.nextDouble()) * 1.25;
            double across = Math.cos(angle) * radius, up = Math.sin(angle) * radius, out = 0.55 + random.nextDouble() * 0.2;
            double speed = 0.18 + random.nextDouble() * 0.12;
            level.addParticle(ParticleTypes.CLOUD,
                pos.getX() + 0.5 + front.getStepX() * out + side.getStepX() * across, pos.getY() + 0.5 + up,
                pos.getZ() + 0.5 + front.getStepZ() * out + side.getStepZ() * across,
                front.getStepX() * speed, 0.02, front.getStepZ() * speed);
        }
    }

    /** Checks the structure and links its ports (every second, at once after a part was removed). */
    public void validate() {
        if (level == null) return;
        Direction facing = getBlockState().getValue(mio_icif_block_steam_tunnel.FACING);
        List<BlockPos> found = new ArrayList<>();
        String why = null;
        BlockPos where = null;
        for (Map.Entry<BlockPos, SteamTunnelStructure.Role> entry : SteamTunnelStructure.layout(worldPosition, facing).entrySet()) {
            BlockPos at = entry.getKey();
            if (entry.getValue() == SteamTunnelStructure.Role.CONTROLLER) continue;
            if (!level.isLoaded(at)) { why = "unloaded"; where = at; break; }
            BlockState state = level.getBlockState(at);
            boolean casing = state.is(mio_icif_blocks.STEAM_TUNNEL_CASING.get());
            boolean port = state.is(mio_icif_blocks.STEAM_TUNNEL_FLUID_PORT.get()) || state.is(mio_icif_blocks.STEAM_TUNNEL_POWER_PORT.get());
            switch (entry.getValue()) {
                case INTERIOR -> { if (!state.isAir()) why = "air"; }
                case EDGE -> { if (!casing) why = port ? "port_on_edge" : "casing"; }
                default -> {
                    if (port) found.add(at.immutable());
                    else if (!casing) why = "casing_or_port";
                }
            }
            if (why != null) { where = at; break; }
        }
        boolean ok = why == null;
        problem = why;
        problemAt = where;
        List<BlockPos> linked = ok ? found : List.of();
        if (ok != formed || !linked.equals(ports)) {
            for (BlockPos old : ports)
                if (!linked.contains(old) && level.isLoaded(old) && level.getBlockEntity(old) instanceof SteamTunnelPart part) part.unlink(worldPosition);
            for (BlockPos now : linked) if (level.getBlockEntity(now) instanceof SteamTunnelPart part) part.link(worldPosition);
            ports.clear();
            ports.addAll(linked);
            formed = ok;
            setChanged();
        }
        BlockState own = getBlockState();
        if (own.getValue(mio_icif_block_steam_tunnel.FORMED) != ok)
            level.setBlock(worldPosition, own.setValue(mio_icif_block_steam_tunnel.FORMED, ok), Block.UPDATE_CLIENTS);
        showFan(level, worldPosition, facing, ok, ok && own.getValue(mio_icif_block_steam_tunnel.ACTIVE));
    }

    /**
     * Puts the 8 blade pieces of the big fan on the casings around the controller at {@code controller} ({@code spinning} while the
     * tunnel runs), or takes them off. A casing that already shows a piece of another tunnel keeps it; hiding takes off only this
     * tunnel's pieces.
     */
    public static void showFan(Level level, BlockPos controller, Direction front, boolean show, boolean spinning) {
        if (level.isClientSide) return;
        for (SteamTunnelStructure.Fan piece : SteamTunnelStructure.Fan.of(front)) {
            BlockPos at = piece.casing(controller);
            if (!level.isLoaded(at)) continue;
            BlockState state = level.getBlockState(at);
            if (!state.hasProperty(mio_icif_block_steam_tunnel_part.Casing.FAN)) continue;
            SteamTunnelStructure.Fan now = state.getValue(mio_icif_block_steam_tunnel_part.Casing.FAN);
            BlockState want = state;
            if (show && (now == SteamTunnelStructure.Fan.NONE || now == piece))
                want = state.setValue(mio_icif_block_steam_tunnel_part.Casing.FAN, piece).setValue(mio_icif_block_steam_tunnel_part.Casing.SPINNING, spinning);
            else if (!show && now == piece)
                want = state.setValue(mio_icif_block_steam_tunnel_part.Casing.FAN, SteamTunnelStructure.Fan.NONE).setValue(mio_icif_block_steam_tunnel_part.Casing.SPINNING, false);
            if (want != state) level.setBlock(at, want, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    /** A casing or port was removed: every controller in reach checks its structure now (it broke at once in IC2 Classic). */
    public static void partRemoved(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        for (BlockPos at : BlockPos.betweenClosed(pos.offset(-4, -1, -4), pos.offset(4, 1, 4)))
            if (level.isLoaded(at) && level.getBlockEntity(at) instanceof SteamTunnelBlockEntity tunnel
                    && SteamTunnelStructure.inReach(at, pos)) tunnel.validate();
    }

    // ------------------------------------------------------------------ state for ports, Jade and the right-click

    public boolean isFormed() { return formed; }

    public boolean serves(BlockPos port) { return formed && ports.contains(port); }

    public long energy() { return energy; }

    /** EU made per tick, over the last second (2,048 at full steam). */
    public long euPerTick() { return madeLastSecond / 20; }

    /** It converted steam in the last {@link #RUN_HOLD_TICKS} ticks. */
    public boolean running() { return formed && runHold > 0; }

    public FluidStack steam() { return steam.getFluid(); }

    public FluidStack water() { return water.getFluid(); }

    /** A power port takes up to {@code amount} EU from the buffer. */
    public long takeEnergy(long amount) {
        long taken = Math.max(0, Math.min(amount, energy));
        if (taken > 0) { energy -= taken; TickMarks.saved(this); }
        return taken;
    }

    /** EU a power port could not store comes back (never lost). */
    public void returnEnergy(long amount) {
        if (amount > 0) { energy = Math.min(MAX_ENERGY, energy + amount); TickMarks.saved(this); }
    }

    /** The state line: running, idle, or why the structure is not formed. */
    public Component status() {
        if (formed) return Component.translatable(running() ? "message.mio_icif.steam_tunnel.running" : "message.mio_icif.steam_tunnel.idle",
            euPerTick(), ports.size());
        if (problem == null) return Component.translatable("message.mio_icif.steam_tunnel.checking");
        return Component.translatable("message.mio_icif.steam_tunnel.problem." + problem,
            problemAt == null ? "" : problemAt.getX() + ", " + problemAt.getY() + ", " + problemAt.getZ());
    }

    // ------------------------------------------------------------------ fluids: steam in, water out

    @Override public int getTanks() { return 2; }

    @Override public FluidStack getFluidInTank(int tank) { return tank == 0 ? steam.getFluid() : water.getFluid(); }

    @Override public int getTankCapacity(int tank) { return tank == 0 ? STEAM_CAPACITY : WATER_CAPACITY; }

    @Override public boolean isFluidValid(int tank, FluidStack stack) { return tank == 0 && steam.isFluidValid(stack); }

    @Override public int fill(FluidStack resource, FluidAction action) { return steam.fill(resource, action); }

    @Override public FluidStack drain(FluidStack resource, FluidAction action) { return water.drain(resource, action); }

    @Override public FluidStack drain(int maxDrain, FluidAction action) { return water.drain(maxDrain, action); }

    // ------------------------------------------------------------------ save

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Steam", steam.writeToNBT(registries, new CompoundTag()));
        tag.put("Water", water.writeToNBT(registries, new CompoundTag()));
        tag.putLong("Energy", energy);
        tag.putInt("Condensate", condensate);
        tag.putBoolean("Formed", formed);
        ListTag list = new ListTag();
        for (BlockPos port : ports) list.add(NbtUtils.writeBlockPos(port));
        tag.put("Ports", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        steam.readFromNBT(registries, tag.getCompound("Steam"));
        water.readFromNBT(registries, tag.getCompound("Water"));
        energy = Math.max(0, Math.min(MAX_ENERGY, tag.getLong("Energy")));
        condensate = Math.max(0, tag.getInt("Condensate"));
        formed = tag.getBoolean("Formed");
        ports.clear();
        for (Tag t : tag.getList("Ports", Tag.TAG_INT_ARRAY))
            if (t instanceof net.minecraft.nbt.IntArrayTag a && a.size() == 3) ports.add(new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()));
    }

    /** A block entity of the shell that serves a controller (fluid port, power port). */
    public interface SteamTunnelPart {
        void link(BlockPos controller);

        void unlink(BlockPos controller);
    }
}
