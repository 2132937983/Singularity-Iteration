// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.blockentity.reactor;

import com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids;
import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_nuclear_reactor_generator;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.36: the steam reactor (IC2 Classic's steam reactor, re-done on SI's reactor). The core, its fuel, its heat, its
 * accidents and its blackbox are those of the nuclear reactor; the differences are hooks of {@link mio_icif_nuclear_reactor_generator}:
 * <ul>
 *   <li>its columns come from steam reactor chambers only (an electric chamber never joins it, nor a steam chamber an electric
 *       reactor), and it never forms the 5x5x5 fluid reactor;</li>
 *   <li>it makes no EU: steam vents turn their heat into steam ({@code SteamReactorCycle}: 1 HU = 1 mB of steam from 1/100 mB of
 *       water); ordinary vents still vent; heat the steam vents cannot turn into steam (no water, no steam room) stays in them;</li>
 *   <li>water (or distilled water) goes in and steam comes out through the reactor and its chambers; it pushes steam into
 *       adjacent fluid handlers every tick. It opens the fluid reactor screen (water / steam tanks).</li>
 * </ul>
 */
public class mio_icif_steam_reactor extends mio_icif_nuclear_reactor_generator {
    public static final int WATER_CAPACITY = 16_000, STEAM_CAPACITY = 128_000;
    /** mB of steam pushed into each adjacent fluid handler per tick. */
    public static final int PUSH_PER_SIDE = 8_000;

    private final FluidTank water = new FluidTank(WATER_CAPACITY, stack -> water(stack.getFluid())) {
        @Override protected void onContentsChanged() { setChanged(); }
    };
    private final FluidTank steam = new FluidTank(STEAM_CAPACITY, stack -> stack.getFluid().isSame(mio_icif_fluids.STEAM.get())) {
        @Override protected void onContentsChanged() { setChanged(); }
    };
    private int lastSteam;

    /** Water in (water, distilled water), steam out. */
    private final IFluidHandler tanks = new IFluidHandler() {
        @Override public int getTanks() { return 2; }
        @Override public FluidStack getFluidInTank(int tank) { return tank == 0 ? water.getFluid() : steam.getFluid(); }
        @Override public int getTankCapacity(int tank) { return tank == 0 ? WATER_CAPACITY : STEAM_CAPACITY; }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return tank == 0 ? water(stack.getFluid()) : stack.getFluid().isSame(mio_icif_fluids.STEAM.get()); }
        @Override public int fill(FluidStack resource, FluidAction action) { return water(resource.getFluid()) ? water.fill(resource, action) : 0; }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            return resource.getFluid().isSame(mio_icif_fluids.STEAM.get()) ? steam.drain(resource, action) : FluidStack.EMPTY;
        }
        @Override public FluidStack drain(int maxDrain, FluidAction action) { return steam.drain(maxDrain, action); }
    };

    public mio_icif_steam_reactor(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.STEAM_REACTOR_ENTITY_TYPE.get());
    }

    private static boolean water(Fluid fluid) {
        return fluid.isSame(Fluids.WATER) || fluid.isSame(mio_icif_fluids.DISTILLEDWATER.get());
    }

    // ---- the hooks of the nuclear reactor

    @Override protected boolean steamMode() { return true; }
    @Override protected int steamWater() { return water.getFluidAmount(); }
    @Override protected int steamRoom() { return STEAM_CAPACITY - steam.getFluidAmount(); }
    @Override protected void commitSteam(int waterUsed, int steamMade) {
        if (waterUsed > 0) water.drain(waterUsed, IFluidHandler.FluidAction.EXECUTE);
        if (steamMade > 0) steam.fill(new FluidStack(mio_icif_fluids.STEAM.get(), steamMade), IFluidHandler.FluidAction.EXECUTE);
        lastSteam = steamMade;
        setChanged();
    }
    @Override protected @Nullable IFluidHandler steamFluids() { return tanks; }
    @Override protected int inputFluidKind() { return water.isEmpty() ? -1 : 2; }
    @Override protected int outputFluidKind() { return steam.isEmpty() ? -1 : 2; }
    @Override protected int inputFluidCapacity() { return WATER_CAPACITY; }
    @Override protected int outputFluidCapacity() { return STEAM_CAPACITY; }
    /** mB of steam made by the last cycle (20 ticks). */
    @Override protected int fluidOutputRate() { return lastSteam; }
    @Override public int getInputFluidAmount() { return water.getFluidAmount(); }
    @Override public int getOutputFluidAmount() { return steam.getFluidAmount(); }
    @Override public Component getDisplayName() { return Component.translatable("container.mio_icif.steam_reactor"); }

    public FluidTank waterTank() { return water; }
    public FluidTank steamTank() { return steam; }

    /** The reactor cycle, then steam into the adjacent fluid handlers (pipes, turbines, kinetic generators). */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_steam_reactor reactor) {
        mio_icif_nuclear_reactor_generator.tick(level, pos, state, reactor);
        if (!(level instanceof ServerLevel server) || reactor.isRemoved() || reactor.steam.isEmpty()) return;
        for (Direction side : Direction.values()) {
            if (reactor.steam.isEmpty()) break;
            BlockPos at = pos.relative(side);
            if (!server.isLoaded(at)) continue;
            var be = server.getBlockEntity(at);
            if (be instanceof mio_icif_reactor_chamber || be instanceof mio_icif_nuclear_reactor_generator) continue;
            IFluidHandler target = server.getCapability(Capabilities.FluidHandler.BLOCK, at, side.getOpposite());
            if (target == null) continue;
            var offer = reactor.steam.drain(PUSH_PER_SIDE, IFluidHandler.FluidAction.SIMULATE);
            int accepted = target.fill(offer, IFluidHandler.FluidAction.EXECUTE);
            if (accepted > 0) reactor.steam.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        var tanksTag = new CompoundTag();
        tanksTag.put("water", water.writeToNBT(provider, new CompoundTag()));
        tanksTag.put("steam", steam.writeToNBT(provider, new CompoundTag()));
        tanksTag.putInt("last_steam", lastSteam);
        tag.put("SteamTanks", tanksTag);
    }

    @Override public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        var tanksTag = tag.getCompound("SteamTanks");
        water.readFromNBT(provider, tanksTag.getCompound("water"));
        steam.readFromNBT(provider, tanksTag.getCompound("steam"));
        lastSteam = Math.max(0, tanksTag.getInt("last_steam"));
    }
}
