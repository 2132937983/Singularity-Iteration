// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Ejector Pipe (0.1.8.0): a pipe end that throws items and pours fluids out into the world.
 *
 * <h2>Specification</h2>
 * <ul>
 *   <li>Front: the side of {@link EjectorPipeBlock#FACING} (the nozzle). Back: the opposite side.</li>
 *   <li>Input: pipes and machines can insert into a 1-slot item buffer and a
 *       {@link #TANK} mB tank (from every side except the front). When the buffer is empty, the
 *       ejector pulls up to {@link #ITEMS_PER_SHOT} items or one bucket from the item handler or
 *       fluid handler at its back.</li>
 *   <li>Items: every {@link #ITEM_PERIOD} ticks the buffer contents leave the nozzle as an item
 *       entity with a speed of {@link #ITEM_SPEED} blocks per tick along the front.</li>
 *   <li>Fluids: every {@link #FLUID_PERIOD} ticks, with one full bucket in the tank, the ejector
 *       places a source block of the fluid in front when that position is replaceable and the
 *       fluid has a block. Otherwise it waits (it never deletes fluid).</li>
 *   <li>A redstone signal stops the ejector. Protection: the block place check of the level
 *       ({@code mayInteract} is not available without a player, so spawn protection and claim
 *       mods that cancel neighbour fluid placement apply as for a dispenser).</li>
 * </ul>
 */
public class EjectorPipeBlockEntity extends BlockEntity {
    public static final int ITEMS_PER_SHOT = 16, ITEM_PERIOD = 8, FLUID_PERIOD = 20, TANK = FluidType.BUCKET_VOLUME;
    public static final double ITEM_SPEED = 0.55;

    private final ItemStackHandler buffer = new ItemStackHandler(1) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private final FluidTank tank = new FluidTank(TANK) {
        @Override protected void onContentsChanged() { setChanged(); }
    };

    /** Insert-only views for the capabilities (nothing can be taken back out of an ejector). */
    private final IItemHandler input = new IItemHandler() {
        @Override public int getSlots() { return 1; }
        @Override public ItemStack getStackInSlot(int slot) { return buffer.getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return buffer.insertItem(slot, stack, simulate); }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return true; }
    };
    private final IFluidHandler fluidInput = new IFluidHandler() {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int t) { return tank.getFluid(); }
        @Override public int getTankCapacity(int t) { return TANK; }
        @Override public boolean isFluidValid(int t, FluidStack stack) { return stack.getFluid().defaultFluidState().createLegacyBlock().getBlock()
            != net.minecraft.world.level.block.Blocks.AIR; }
        @Override public int fill(FluidStack resource, FluidAction action) { return isFluidValid(0, resource) ? tank.fill(resource, action) : 0; }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    };

    public EjectorPipeBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsRegistry.EJECTOR_ENTITY.get(), pos, state);
    }

    private Direction front() {
        return getBlockState().hasProperty(EjectorPipeBlock.FACING) ? getBlockState().getValue(EjectorPipeBlock.FACING) : Direction.NORTH;
    }

    @Nullable public IItemHandler itemCapability(@Nullable Direction side) { return side == front() ? null : input; }
    @Nullable public IFluidHandler fluidCapability(@Nullable Direction side) { return side == front() ? null : fluidInput; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EjectorPipeBlockEntity be) {
        if (!(level instanceof ServerLevel world) || level.hasNeighborSignal(pos)) return;
        long t = world.getGameTime() + (pos.asLong() & 7);
        if (t % ITEM_PERIOD == 0) be.tickItems(world);
        if (t % FLUID_PERIOD == 0) be.tickFluid(world);
    }

    private void tickItems(ServerLevel world) {
        Direction front = front();
        if (buffer.getStackInSlot(0).isEmpty()) {
            IItemHandler back = world.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(front.getOpposite()), front);
            if (back != null) {
                for (int s = 0; s < back.getSlots(); s++) {
                    ItemStack taken = back.extractItem(s, ITEMS_PER_SHOT, false);
                    if (!taken.isEmpty()) { buffer.setStackInSlot(0, taken); break; }
                }
            }
        }
        ItemStack stack = buffer.getStackInSlot(0);
        if (stack.isEmpty()) return;
        BlockPos out = worldPosition.relative(front);
        if (world.getBlockState(out).isCollisionShapeFullBlock(world, out)) return;            // blocked nozzle: wait
        ItemStack shot = stack.split(Math.min(stack.getCount(), ITEMS_PER_SHOT));
        buffer.setStackInSlot(0, stack);
        Vec3 dir = Vec3.atLowerCornerOf(front.getNormal());
        Vec3 at = Vec3.atCenterOf(worldPosition).add(dir.scale(0.7));
        ItemEntity entity = new ItemEntity(world, at.x, at.y - 0.1, at.z, shot);
        entity.setDeltaMovement(dir.scale(ITEM_SPEED).add(world.random.triangle(0, 0.03), front == Direction.DOWN ? 0 : 0.06, world.random.triangle(0, 0.03)));
        entity.setDefaultPickUpDelay();
        world.addFreshEntity(entity);
        world.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 3, dir.x * 0.1, dir.y * 0.1, dir.z * 0.1, 0.05);
        world.playSound(null, worldPosition, SoundEvents.DISPENSER_LAUNCH, SoundSource.BLOCKS, 0.6F, 1.3F);
    }

    private void tickFluid(ServerLevel world) {
        Direction front = front();
        if (tank.getFluidAmount() < TANK) {
            IFluidHandler back = world.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(front.getOpposite()), front);
            if (back != null) {
                FluidStack sim = back.drain(TANK - tank.getFluidAmount(), IFluidHandler.FluidAction.SIMULATE);
                if (!sim.isEmpty() && fluidInput.isFluidValid(0, sim) && tank.fill(sim, IFluidHandler.FluidAction.SIMULATE) == sim.getAmount()) {
                    tank.fill(back.drain(sim, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
        if (tank.getFluidAmount() < TANK) return;
        Fluid fluid = tank.getFluid().getFluid();
        FluidState source = fluid.defaultFluidState();
        BlockState block = source.createLegacyBlock();
        BlockPos out = worldPosition.relative(front);
        BlockState there = world.getBlockState(out);
        if (block.isAir() || !there.canBeReplaced(fluid) || !world.getFluidState(out).isEmpty() && world.getFluidState(out).isSource()) return;
        if (world.dimensionType().ultraWarm() && fluid.getFluidType().isVaporizedOnPlacement(world, out, tank.getFluid())) {
            fluid.getFluidType().onVaporize(null, world, out, tank.getFluid());               // water in the Nether: steam, as a bucket
            tank.drain(TANK, IFluidHandler.FluidAction.EXECUTE);
            return;
        }
        if (!world.setBlock(out, block, 11)) return;
        tank.drain(TANK, IFluidHandler.FluidAction.EXECUTE);
        Vec3 at = Vec3.atCenterOf(out);
        world.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 12, 0.3, 0.2, 0.3, 0.2);
        world.playSound(null, out, fluid.getFluidType().getSound(net.neoforged.neoforge.common.SoundActions.BUCKET_EMPTY) != null
            ? fluid.getFluidType().getSound(net.neoforged.neoforge.common.SoundActions.BUCKET_EMPTY) : SoundEvents.BUCKET_EMPTY,
            SoundSource.BLOCKS, 0.8F, 1.1F);
    }

    /** Buffer and tank contents when the block is broken: the items drop, the fluid stays in the world if it can. */
    void dropContents(Level level) {
        ItemStack stack = buffer.getStackInSlot(0);
        if (!stack.isEmpty()) net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, stack);
        buffer.setStackInSlot(0, ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Buffer", buffer.serializeNBT(registries));
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Buffer")) buffer.deserializeNBT(registries, tag.getCompound("Buffer"));
        if (tag.contains("Tank")) tank.readFromNBT(registries, tag.getCompound("Tank"));
    }
}
