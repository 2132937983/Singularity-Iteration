// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.blockentity.producer;

import com.mojang.authlib.GameProfile;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.common.menu.producer.PumpElcMenu;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank;
import com.miophas.singularity_iteration.core.runtime.processing.MachineActionOwner;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * Independent pump. R49 establishes the unupgraded adjacent-water 20 paid steps / next-step output.
 * Bounded connected search, lava, automation and miner requests are SI candidate behavior.
 * The excluded predecessor was archived without inspecting its implementation.
 */
public class mio_icif_pump_elc extends AbstractProcessingMachineBlockEntity {
    public static final int SLOT_BATTERY = 0, SLOT_UPGRADE_START = 1, SLOT_UPGRADE_COUNT = 4,
        SLOT_UPGRADE_END = 5, SLOT_EMPTY_CONTAINER = 5, SLOT_OUTPUT = 6, TOTAL_SLOTS = 7;
    public static final long DEFAULT_CAPACITY = 20, DEFAULT_MAX_RECEIVE = 32, DEFAULT_MAX_EXTRACT = 0,
        DEFAULT_ENERGY_PER_TICK = 1, ENERGY_PER_1000MB = 20;
    // 对齐原版 IC2 TileEntityPump：addTankExtract("fluid", 8000) → 8000 mB（8 桶）
    public static final int DEFAULT_WORK_TIME = 20, FLUID_CAPACITY = 8000, FLUID_PER_OPERATION = 1000;
    public static final int IDLE_RETRY_TICKS = 20;
    private static final String SAVE_KEY = "scex_pump_v1";
    private static final SlotLayout LAYOUT = SlotLayout.builder().battery().upgrade(4).input(1).output(1).build();
    private static final GameProfile LEGACY_ACTOR = new GameProfile(
        UUID.nameUUIDFromBytes("mio_icif:automated_pump".getBytes(StandardCharsets.UTF_8)), "[SI Pump]");
    private final FluidTank tank = new FluidTank(FLUID_CAPACITY) {
        @Override protected void onContentsChanged() { ContainerToTank.markUnsaved(mio_icif_pump_elc.this); }
    };
    private final IFluidHandler fluidPort;
    // 复刻 IC2 PumpUtil.searchFluidSource：一次搜源完成后缓存水源位置与流体类型。
    private BlockPos cachedSource;
    private Fluid cachedFluid = Fluids.EMPTY;
    private long retryAt, lastCompletion = Long.MIN_VALUE;
    private int paidWork;
    private boolean changing;
    private CompoundTag unmappedLegacy, uncertainRemoval;
    private MachineActionOwner actionOwner = MachineActionOwner.legacy(LEGACY_ACTOR);
    private final int[] clientData = new int[7];

    public mio_icif_pump_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.PUMP_ELC_ENTITY_TYPE.get());
    }
    public mio_icif_pump_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type, DEFAULT_CAPACITY, DEFAULT_MAX_RECEIVE, DEFAULT_MAX_EXTRACT,
            DEFAULT_WORK_TIME, LAYOUT, DEFAULT_ENERGY_PER_TICK, CableTier.LV);
        fluidPort = new IFluidHandler() {
            private void check(int index) { if (index != 0) throw new IndexOutOfBoundsException(index); }
            @Override public int getTanks() { return 1; }
            @Override public FluidStack getFluidInTank(int index) { check(index); return tank.getFluid().copy(); }
            @Override public int getTankCapacity(int index) { check(index); return FLUID_CAPACITY; }
            @Override public boolean isFluidValid(int index, FluidStack fluid) { check(index); return false; }
            @Override public int fill(FluidStack fluid, FluidAction action) { return 0; }
            @Override public FluidStack drain(FluidStack fluid, FluidAction action) {
                return mayTransfer() ? tank.drain(fluid, action) : FluidStack.EMPTY;
            }
            @Override public FluidStack drain(int amount, FluidAction action) {
                return mayTransfer() ? tank.drain(amount, action) : FluidStack.EMPTY;
            }
        };
    }
    protected boolean operational() {
        return level instanceof ServerLevel server && server.getServer().isSameThread() && !isRemoved()
            && available(server, worldPosition) && server.getBlockEntity(worldPosition) == this;
    }
    private static boolean available(ServerLevel server, BlockPos pos) {
        return !server.isOutsideBuildHeight(pos) && server.getWorldBorder().isWithinBounds(pos)
            && server.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }
    private boolean mayTransfer() { return actionOwner.canAct() && !changing && !hasHeldState() && operational(); }
    public boolean hasUnmappedLegacy() { return unmappedLegacy != null; }
    public boolean hasUncertainRemoval() { return uncertainRemoval != null; }
    private boolean hasHeldState() { return hasUnmappedLegacy() || hasUncertainRemoval(); }
    protected long currentTick() { return level == null ? 0 : level.getGameTime(); }
    protected Direction getFacing() {
        var property = net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;
        if (getBlockState().hasProperty(property)) return getBlockState().getValue(property);
        var horizontal = net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
        return getBlockState().hasProperty(horizontal) ? getBlockState().getValue(horizontal) : Direction.DOWN;
    }
    protected BlockState sourceState(BlockPos pos) {
        return level instanceof ServerLevel server && available(server, pos) ? server.getBlockState(pos) : Blocks.AIR.defaultBlockState();
    }
    /**
     * 可被桶/单元吸取的流体方块所对应的流体类型（源与流动都算，用于搜索与归类）。
     * 对齐原版 IC2：既覆盖原版水/岩浆，也覆盖任意实现了 {@link BucketPickup} 的流体方块
     * （对应 IC2 的 {@code IFluidBlock} 泛化抽取）。
     * <p>注意：这里只做“类型”判定，<b>是否水源</b>由调用方用 {@code getFluidState().isSource()} 单独判断，
     * 否则流动水会被误判为不可通行，搜索将无法沿水流爬向水源。
     */
    private static Fluid sourceFluid(BlockState state) {
        if (!(state.getBlock() instanceof BucketPickup)) return Fluids.EMPTY;
        var fluidState = state.getFluidState();
        if (fluidState.isEmpty()) return Fluids.EMPTY;
        return fluidState.getType();
    }
    /**
     * 复刻 IC2 {@code PumpUtil.getFlowDecay}：水源/可抽取流体方块 = 0，流动液按液位换算为 1..7，
     * 非流体方块 = -1。
     */
    private int flowDecay(BlockState state, BlockPos pos) {
        if (!(state.getBlock() instanceof BucketPickup)) return -1;
        var fluidState = state.getFluidState();
        if (fluidState.isEmpty()) return -1;
        if (fluidState.isSource()) return 0;
        return Math.max(1, 8 - fluidState.getAmount());
    }
    private int flowDecay(BlockPos pos) {
        return flowDecay(sourceState(pos), pos);
    }
    /**
     * 复刻 IC2 {@code PumpUtil.moveUp}：探测正上方十字（中心 + 四邻）的流体，返回其 decay；
     * 全部不可通行时把 pos 还原到原位置并返回 -1。
     */
    private int moveUp(BlockPos.MutableBlockPos pos) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        int newDecay = flowDecay(pos.set(x, y + 1, z));
        if (newDecay >= 0) return newDecay;
        newDecay = flowDecay(pos.set(x + 1, y + 1, z));
        if (newDecay >= 0) return newDecay;
        newDecay = flowDecay(pos.set(x - 1, y + 1, z));
        if (newDecay >= 0) return newDecay;
        newDecay = flowDecay(pos.set(x, y + 1, z + 1));
        if (newDecay >= 0) return newDecay;
        newDecay = flowDecay(pos.set(x, y + 1, z - 1));
        if (newDecay >= 0) return newDecay;
        pos.set(x, y, z);
        return -1;
    }
    /**
     * 复刻 IC2 {@code PumpUtil.moveSideways}：同层探测 decay 更低的相邻流体；
     * 全部失败时还原 pos 并返回 -1。
     */
    private int moveSideways(BlockPos.MutableBlockPos pos, int decay) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        int newDecay = flowDecay(pos.set(x - 1, y, z));
        if (newDecay >= 0 && newDecay < decay) return newDecay;
        newDecay = flowDecay(pos.set(x, y, z + 1));
        if (newDecay >= 0 && newDecay < decay) return newDecay;
        newDecay = flowDecay(pos.set(x, y, z - 1));
        if (newDecay >= 0 && newDecay < decay) return newDecay;
        newDecay = flowDecay(pos.set(x + 1, y, z));
        if (newDecay >= 0 && newDecay < decay) return newDecay;
        pos.set(x, y, z);
        return -1;
    }
    /**
     * 复刻 IC2 {@code PumpUtil.searchFluidSource}（含 ±2 跳格探测与找不到水源时消解终点周围 5×5 流动液的破坏性收尾）。
     * 返回找到的水源方块坐标，找不到返回 null。
     */
    @Nullable
    private BlockPos ic2SearchFluidSource(BlockPos startPos) {
        if (!(level instanceof ServerLevel server)) return null;
        BlockPos.MutableBlockPos pos = startPos.mutable();
        int decay = flowDecay(pos);
        for (int i = 0; i < 64; i++) {
            int newDecay = moveUp(pos);
            if (newDecay < 0) {
                newDecay = moveSideways(pos, decay);
                if (newDecay < 0) break;
            }
            decay = newDecay;
        }
        java.util.Set<BlockPos> visited = new java.util.HashSet<>(64);
        for (int j = 0; j < 64; j++) {
            visited.add(pos.immutable());
            if (!visited.contains(pos.move(-1, 0, 0).immutable())) {
                int newDecay = flowDecay(pos);
                if (newDecay >= 0) {
                    if (newDecay == 0) return pos.immutable();
                    continue;
                }
            }
            if (!visited.contains(pos.move(1, 0, 1).immutable())) {
                int newDecay = flowDecay(pos);
                if (newDecay >= 0) {
                    if (newDecay == 0) return pos.immutable();
                    continue;
                }
            }
            if (!visited.contains(pos.move(0, 0, -2).immutable())) {
                int newDecay = flowDecay(pos);
                if (newDecay >= 0) {
                    if (newDecay == 0) return pos.immutable();
                    continue;
                }
            }
            if (!visited.contains(pos.move(1, 0, 1).immutable())) {
                int newDecay = flowDecay(pos);
                if (newDecay >= 0) {
                    if (newDecay == 0) return pos.immutable();
                    continue;
                }
            }
            pos.move(-1, 0, 0);
        }
        for (int ix = -2; ix <= 2; ix++) {
            for (int iz = -2; iz <= 2; iz++) {
                BlockPos cPos = pos.offset(ix, 0, iz);
                if (!available(server, cPos)) continue;
                BlockState state = server.getBlockState(cPos);
                int d = flowDecay(state, cPos);
                if (d < 0) continue;
                if (d == 0) return cPos;
                if (d >= 1 && d < 7 && state.getBlock() instanceof LiquidBlock) {
                    int amount = state.getFluidState().getAmount();
                    server.setBlock(cPos, state.setValue(LiquidBlock.LEVEL, Math.max(1, amount - 1)), 3);
                } else {
                    server.removeBlock(cPos, false);
                }
            }
        }
        return null;
    }
    private void forgetSearch(boolean idle) {
        cachedSource = null; cachedFluid = Fluids.EMPTY;
        retryAt = idle ? currentTick() + IDLE_RETRY_TICKS + Math.floorMod(worldPosition.asLong(), 5) : 0;
    }
    private boolean findSource() {
        if (cachedSource != null) {
            BlockState state = sourceState(cachedSource);
            Fluid fluid = sourceFluid(state);
            if (fluid != Fluids.EMPTY && state.getFluidState().isSource()) {
                cachedFluid = fluid;
                return true;
            }
            forgetSearch(false);
        }
        if (currentTick() < retryAt) return false;
        BlockPos found = ic2SearchFluidSource(worldPosition.relative(getFacing()));
        if (found == null) { forgetSearch(true); return false; }
        Fluid fluid = sourceFluid(sourceState(found));
        if (fluid == Fluids.EMPTY) { forgetSearch(true); return false; }
        cachedSource = found;
        cachedFluid = fluid;
        return true;
    }
    private boolean room(Fluid fluid, int amount) {
        return fluid != Fluids.EMPTY && amount > 0 && amount <= FLUID_CAPACITY
            && tank.fill(new FluidStack(fluid, amount), IFluidHandler.FluidAction.SIMULATE) == amount;
    }
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_pump_elc pump) {
        if (!pump.operational() || pump.hasHeldState() || !pump.actionOwner.canAct()) return;
        AbstractProcessingMachineBlockEntity.tick(level, pos, state, pump);
        pump.setLit(pump.isWorking);
    }
    @Override protected void tickProduction() {
        stopWork();
        if (!mayTransfer() || lastCompletion == currentTick()
                || tank.getFluidAmount() > FLUID_CAPACITY - FLUID_PER_OPERATION) return;
        long cost = getEffectiveEnergyPerTick();
        if (paidWork < DEFAULT_WORK_TIME && (cost <= 0 || energyStorage.consumeEnergyInternal(cost, true) != cost)) return;
        // 对齐原版 IC2：优先从正面相邻的流体容器抽取，其次才退回液源搜索。
        boolean handlerSource = frontHandlerDrainable();
        if (!handlerSource) {
            if (!findSource()) return;
            if (!room(cachedFluid, FLUID_PER_OPERATION)) { forgetSearch(true); return; }
        }
        if (paidWork >= DEFAULT_WORK_TIME) {
            boolean done = handlerSource && collectFromFrontHandler();
            if (!done) {
                // 正面容器缺失或抽取失败时回退液源搜索，避免卡在容器分支上永不搜源。
                done = findSource() && room(cachedFluid, FLUID_PER_OPERATION)
                    && collect(cachedSource, cachedFluid);
            }
            forgetSearch(!done); return;
        }
        changing = true;
        try {
            if (energyStorage.consumeEnergyInternal(cost, false) != cost) return;
            paidWork += Math.min(DEFAULT_WORK_TIME - paidWork, Math.max(1, getProgressPerTick()));
            progress = paidWork; isWorking = true; ContainerToTank.markUnsaved(this);
        } finally { changing = false; }
    }
    @Override protected boolean canWork() {
        return mayTransfer() && (room(cachedFluid, FLUID_PER_OPERATION) || frontHandlerDrainable());
    }
    /** 正面相邻方块的流体处理器（对齐 IC2 抽取正面流体容器）。 */
    @Nullable
    private IFluidHandler frontFluidHandler() {
        if (!(level instanceof ServerLevel server) || !mayTransfer()) return null;
        Direction facing = getFacing();
        BlockPos pos = worldPosition.relative(facing);
        if (!available(server, pos)) return null;
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, facing.getOpposite());
    }
    /** 正面容器是否有可被完整承认（≤1000 mB）的流体。 */
    private boolean frontHandlerDrainable() {
        IFluidHandler front = frontFluidHandler();
        if (front == null) return false;
        FluidStack offered = front.drain(FLUID_PER_OPERATION, IFluidHandler.FluidAction.SIMULATE);
        return !offered.isEmpty() && room(offered.getFluid(), offered.getAmount());
    }
    /** 抽取正面流体容器：按原版语义最多取 1000 mB，实际取到量写入自有储罐。 */
    private boolean collectFromFrontHandler() {
        if (!mayTransfer() || paidWork < DEFAULT_WORK_TIME) return false;
        IFluidHandler front = frontFluidHandler();
        if (front == null) return false;
        FluidStack offered = front.drain(FLUID_PER_OPERATION, IFluidHandler.FluidAction.SIMULATE);
        if (offered.isEmpty() || !room(offered.getFluid(), offered.getAmount())) return false;
        changing = true;
        try {
            FluidStack drained = front.drain(offered, IFluidHandler.FluidAction.EXECUTE);
            if (drained.isEmpty()) return false;
            if (tank.fill(drained, IFluidHandler.FluidAction.EXECUTE) != drained.getAmount())
                throw new IllegalStateException("Owned pump output admission changed during tank drain");
            paidWork -= DEFAULT_WORK_TIME; progress = paidWork; uncertainRemoval = null;
            lastCompletion = currentTick(); ContainerToTank.markUnsaved(this); return true;
        } finally { changing = false; }
    }
    @Override protected void doWork() { tickProduction(); }
    @Override protected void updateProgress() { progress = paidWork; }
    @Override protected boolean shouldResetProgress() { return false; }
    @Override protected void checkInputChanged() { /* Container changes cannot erase paid work. */ }

    /** The pump owns removal and output together; callers must never remove the source again. */
    public boolean tryCollectForMiner(mio_icif_miner_elc miner, BlockPos pos) {
        if (!mayTransfer() || !(level instanceof ServerLevel server) || miner.getLevel() != server
                || !actionOwner.canShareAutomationWith(miner.getActionOwner())
                || !available(server, miner.getBlockPos()) || server.getBlockEntity(miner.getBlockPos()) != miner
                || worldPosition.distManhattan(miner.getBlockPos()) != 1 || !canWorkRedstone()
                || lastCompletion == currentTick() || paidWork > 0 && paidWork < DEFAULT_WORK_TIME) return false;
        var state = sourceState(pos); var fluid = sourceFluid(state);
        if (!state.getFluidState().isSource() || !room(fluid, FLUID_PER_OPERATION)) return false;
        if (paidWork == 0) {
            if (energyStorage.consumeEnergyInternal(ENERGY_PER_1000MB, true) != ENERGY_PER_1000MB) return false;
            changing = true;
            try {
                if (energyStorage.consumeEnergyInternal(ENERGY_PER_1000MB, false) != ENERGY_PER_1000MB) return false;
                paidWork = DEFAULT_WORK_TIME; progress = paidWork; ContainerToTank.markUnsaved(this);
            } finally { changing = false; }
        }
        boolean result = collect(pos, fluid); forgetSearch(!result); return result;
    }
    private boolean collect(BlockPos pos, Fluid fluid) {
        if (!mayTransfer() || paidWork < DEFAULT_WORK_TIME || !room(fluid, FLUID_PER_OPERATION)) return false;
        var expected = sourceState(pos);
        if (sourceFluid(expected) != fluid || !expected.getFluidState().isSource()) return false;
        changing = true;
        try {
            if (!removeSource(pos, expected, fluid)) return false;
            // Owned tank and accounting have no externally callable mutation window here.
            if (tank.fill(new FluidStack(fluid, FLUID_PER_OPERATION), IFluidHandler.FluidAction.EXECUTE) != FLUID_PER_OPERATION)
                throw new IllegalStateException("Owned pump output admission changed during removal");
            paidWork -= DEFAULT_WORK_TIME; progress = paidWork; uncertainRemoval = null;
            lastCompletion = currentTick(); ContainerToTank.markUnsaved(this); return true;
        } finally { changing = false; }
    }
    /** Vanilla liquid pickup plus the public protection event; no IC2 types or internals. */
    protected boolean removeSource(BlockPos pos, BlockState expected, Fluid fluid) {
        if (!(level instanceof ServerLevel server) || !operational() || sourceState(pos) != expected) return false;
        var actor = FakePlayerFactory.get(server, actionOwner.actorProfile());
        var oldHand = actor.getMainHandItem().copy(); var oldPosition = actor.position();
        try {
            actor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
            actor.setPos(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
            var event = new BlockEvent.BreakEvent(server, pos, expected, actor); NeoForge.EVENT_BUS.post(event);
            if (event.isCanceled() || !operational() || sourceState(pos) != expected || !room(fluid, FLUID_PER_OPERATION)) return false;
            uncertainRemoval = new CompoundTag(); uncertainRemoval.putLong("Position", pos.asLong());
            uncertainRemoval.putString("Fluid", BuiltInRegistries.FLUID.getKey(fluid).toString());
            ContainerToTank.markUnsaved(this);
            var result = ((BucketPickup) expected.getBlock()).pickupBlock(actor, server, pos, expected);
            // 1.21.1 LiquidBlock returns a bucket even if its setBlock call returns false.
            // Require a loaded, observably changed source before publishing owned fluid.
            if (result.getItem() instanceof BucketItem bucket && bucket.content == fluid && result.getCount() == 1) {
                return available(server, pos) && sourceState(pos) != expected;
            }
            if (result.isEmpty() && available(server, pos) && sourceState(pos) == expected) uncertainRemoval = null;
            ContainerToTank.markUnsaved(this); return false;
        } finally {
            actor.setItemInHand(InteractionHand.MAIN_HAND, oldHand);
            actor.setPos(oldPosition.x, oldPosition.y, oldPosition.z);
        }
    }
    /** Compatibility entry for already supplied material. World callers use tryCollectForMiner. */
    public boolean injectFluid(Fluid fluid, int amount) {
        if (!mayTransfer() || !room(fluid, amount)) return false;
        long cost = ((long) amount * ENERGY_PER_1000MB + FLUID_PER_OPERATION - 1) / FLUID_PER_OPERATION;
        if (energyStorage.consumeEnergyInternal(cost, true) != cost) return false;
        changing = true;
        try {
            if (energyStorage.consumeEnergyInternal(cost, false) != cost) return false;
            int accepted = tank.fill(new FluidStack(fluid, amount), IFluidHandler.FluidAction.EXECUTE);
            if (accepted != amount) throw new IllegalStateException("Owned pump injection changed");
            ContainerToTank.markUnsaved(this); return true;
        } finally { changing = false; }
    }
    public boolean canAcceptFluid(Fluid fluid) { return mayTransfer() && room(fluid, FLUID_PER_OPERATION); }
    @Override protected int getBatterySlot() { return SLOT_BATTERY; }
    @Override protected int[] getSlotsForDirection(Direction side) { return new int[]{SLOT_EMPTY_CONTAINER, SLOT_OUTPUT}; }
    @Override protected boolean canExtractItem(int slot, @Nullable Direction side) { return slot == SLOT_OUTPUT; }
    @Override public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (slot == SLOT_EMPTY_CONTAINER) return stack.is(Items.BUCKET) || mio_icif_cells.isEmptyCell(stack);
        if (slot == SLOT_BATTERY) return isBattery(stack);
        if (slot >= SLOT_UPGRADE_START && slot < SLOT_UPGRADE_END) {
            var type = getItemAPI().getUpgradeType(stack); return type != null && !type.isEmpty();
        }
        return false;
    }
    @Override protected void onTick() {
        if (!mayTransfer() || tank.getFluidAmount() < FLUID_PER_OPERATION) return;
        var input = itemHandler.getStackInSlot(SLOT_EMPTY_CONTAINER);
        if (input.isEmpty()) return;
        var fluid = tank.getFluid().getFluid(); ItemStack filled;
        if (input.is(Items.BUCKET)) filled = new ItemStack(fluid.getBucket());
        else if (mio_icif_cells.isEmptyCell(input)) filled = mio_icif_cells.getFilledCellForFluidStack(fluid);
        else return;
        if (filled.isEmpty() || filled.is(Items.BUCKET)) return;
        var content = input.is(Items.BUCKET) ? new FluidStack(fluid, FLUID_PER_OPERATION) : mio_icif_cells.getCellFluid(filled.copyWithCount(1));
        if (content.getFluid() != fluid || content.getAmount() <= 0) return;
        changing = true;
        try { ContainerToTank.drainToContainer(itemHandler, SLOT_EMPTY_CONTAINER, SLOT_OUTPUT, tank, content, filled); }
        finally { changing = false; }
    }
    @Override protected void handleAutomationUpgrades() {
        if (actionOwner.canAct() && !hasHeldState() && !changing) super.handleAutomationUpgrades();
    }
    public IFluidHandler getFluidHandler() { return fluidPort; }
    @Override public IFluidHandler getFluidHandlerCapability(@Nullable Direction side) { return fluidPort; }
    public int getFluidAmount() { return tank.getFluidAmount(); }
    public int getFluidCapacity() { return FLUID_CAPACITY; }
    public FluidStack getFluid() { return tank.getFluid().copy(); }
    public int getFluidProgress() { return getFluidAmount() * 100 / FLUID_CAPACITY; }
    public String getFluidTypeName() { return tank.isEmpty() ? "" : tank.getFluid().getHoverName().getString(); }
    public MachineActionOwner getActionOwner() { return actionOwner; }
    public void setActionOwnerFromPlacer(@Nullable LivingEntity placer) {
        actionOwner = MachineActionOwner.fromPlacer(placer);
        ContainerToTank.markUnsaved(this);
    }
    @Override public Component getDisplayName() { return Component.translatable("container.mio_icif.pump_elc"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new PumpElcMenu(id, inventory, this); }
    public ContainerData getContainerData() {
        return new ContainerData() {
            private void check(int i) { if (i < 0 || i >= 7) throw new IndexOutOfBoundsException(i); }
            @Override public int getCount() { return 7; }
            @Override public int get(int i) {
                check(i); if (level != null && level.isClientSide()) return clientData[i];
                return switch (i) {
                    case 0 -> paidWork; case 1 -> DEFAULT_WORK_TIME;
                    case 2 -> (int) Math.min(Integer.MAX_VALUE, energyStorage.getAmount());
                    case 3 -> (int) Math.min(Integer.MAX_VALUE, energyStorage.getCapacity());
                    case 4 -> getFluidAmount(); case 5 -> FLUID_CAPACITY;
                    default -> tank.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(tank.getFluid().getFluid());
                };
            }
            @Override public void set(int i, int value) { check(i); clientData[i] = value; }
        };
    }
    @Override public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        var own = new CompoundTag(); own.putInt("PaidWork", paidWork);
        own.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
        own.put("ActionOwner", actionOwner.save());
        if (unmappedLegacy != null) own.put("UnmappedLegacy", unmappedLegacy.copy());
        if (uncertainRemoval != null) own.put("UncertainRemoval", uncertainRemoval.copy());
        tag.put(SAVE_KEY, own);
    }
    @Override public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (changing) throw new IllegalStateException("Cannot load an active pump transaction");
        super.loadAdditional(tag, registries);
        paidWork = 0; tank.setFluid(FluidStack.EMPTY); unmappedLegacy = null; uncertainRemoval = null;
        forgetSearch(false); lastCompletion = Long.MIN_VALUE;
        if (tag.contains(SAVE_KEY, Tag.TAG_COMPOUND)) {
            var own = tag.getCompound(SAVE_KEY);
            actionOwner = MachineActionOwner.load(own, "ActionOwner", LEGACY_ACTOR);
            if (!own.contains("PaidWork", Tag.TAG_INT) || own.getInt("PaidWork") < 0 || own.getInt("PaidWork") > DEFAULT_WORK_TIME
                    || !own.contains("Tank", Tag.TAG_COMPOUND)
                    || own.contains("UnmappedLegacy") && !own.contains("UnmappedLegacy", Tag.TAG_COMPOUND)
                    || own.contains("UncertainRemoval") && !own.contains("UncertainRemoval", Tag.TAG_COMPOUND)) unmappedLegacy = tag.copy();
            else {
                paidWork = own.getInt("PaidWork"); tank.readFromNBT(registries, own.getCompound("Tank"));
                boolean invalidTank = (!own.getCompound("Tank").isEmpty() && tank.isEmpty()) || tank.getFluidAmount() > FLUID_CAPACITY;
                if (invalidTank) unmappedLegacy = tag.copy();
                else if (own.contains("UnmappedLegacy", Tag.TAG_COMPOUND)) unmappedLegacy = own.getCompound("UnmappedLegacy").copy();
                if (own.contains("UncertainRemoval", Tag.TAG_COMPOUND)) uncertainRemoval = own.getCompound("UncertainRemoval").copy();
            }
        } else {
            actionOwner = MachineActionOwner.legacy(LEGACY_ACTOR);
            if (!tag.isEmpty()) unmappedLegacy = tag.copy();
        }
        isWorking = false; progress = paidWork;
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = super.getUpdateTag(registries); saveAdditional(tag, registries); return tag;
    }
}
