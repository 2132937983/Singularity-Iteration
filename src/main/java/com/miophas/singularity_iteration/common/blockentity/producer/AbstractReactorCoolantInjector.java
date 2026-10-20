package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.block.producer.mio_icif_block_reactor_coolant_injector;
import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_nuclear_reactor_generator;
import com.miophas.singularity_iteration.common.blockentity.reactor.mio_icif_reactor_chamber;
import com.miophas.singularity_iteration.common.item.reactor.mio_icif_condensator;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.api.upgrade.tile.UpgradableProperty;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Containers;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * IC2 1.12 {@code TileEntityAbstractRCI} (2.8.221): the Redstone (RSH) and Lapis (LZH) Coolant Injectors.
 * <ul>
 *   <li>The reactor, or a chamber of it, touches the injector. IC2 looks only behind the front (the front
 *       faces whoever placed it); here the back comes first, then the front, then the sides, so it works
 *       whichever face touches the reactor. Any of the six faces can be the front.</li>
 *   <li>48,000 EU buffer, MV (128 EU per packet). Energy enters on every face that does not touch a reactor
 *       part (see {@link ReactorInjectorFaces}).</li>
 *   <li>9 coolant slots (redstone blocks or lapis blocks) and 4 upgrade slots, which take the upgrades for
 *       item consuming machines only (the pulling upgrade).</li>
 *   <li>Every tick, with coolant, at least {@link #ENERGY_PER_OPERATION} EU and a reactor, each condensator
 *       of its kind in the reactor that is more than 85% full of heat takes one block and 1,000 EU and is
 *       emptied. Like IC2 the block is taken before the energy: when the EU runs out between two
 *       condensators of one tick, that block is spent without a refill. With a feed rate above 1 the
 *       reactor also lets it refill between the feeds of one operation ({@link #refillBetweenFeeds}).</li>
 * </ul>
 * Injectors saved before 0.1.10.14 had 53 coolant slots and a battery slot: their blocks go into the
 * coolant slots, the rest stays as a reserve that is used first and dropped when the block is broken.
 */
@SuppressWarnings("null")
public abstract class AbstractReactorCoolantInjector extends AbstractProcessingMachineBlockEntity {

    public static final int INPUT_SLOT_COUNT = 9;
    public static final int UPGRADE_SLOT_START = 9;
    public static final int UPGRADE_SLOT_COUNT = 4;
    public static final int SLOT_COUNT = 13;
    public static final long CAPACITY = 48_000L;
    public static final long MAX_RECEIVE = 128L;
    public static final long ENERGY_PER_OPERATION = 1_000L;
    /** A condensator is refilled when its heat is above this share of its capacity (IC2 durability > 0.85). */
    public static final double REFILL_ABOVE = 0.85D;
    public static final int DATA_COUNT = 4;

    private static final SlotLayout LAYOUT = SlotLayout.builder().input(INPUT_SLOT_COUNT).upgrade(UPGRADE_SLOT_COUNT).build();
    private static final String RESERVE_KEY = "rci_reserve";

    private final Item coolant;
    private final Item condensator;
    /** Items of an injector saved before 0.1.10.14 that the 9 coolant slots could not take. */
    private final List<ItemStack> reserve = new ArrayList<>();
    /** Closed energy faces of the last check (-1 before the first one). */
    private int closedFaces = -1;

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.min(Integer.MAX_VALUE, energyStorage.getAmount());
                case 1 -> (int) Math.min(Integer.MAX_VALUE, energyStorage.getCapacity());
                case 2 -> getCoolantCount();
                case 3 -> getReserveCoolantCount();
                default -> 0;
            };
        }

        @Override public void set(int index, int value) {}
        @Override public int getCount() { return DATA_COUNT; }
    };

    /**
     * @param coolant     the block item it consumes (IC2 RSH: redstone block, LZH: lapis block)
     * @param condensator the condensator it refills
     */
    protected AbstractReactorCoolantInjector(BlockEntityType<?> type, BlockPos pos, BlockState state, Item coolant, Item condensator) {
        super(pos, state, type, CAPACITY, MAX_RECEIVE, 0L, 1, LAYOUT, ENERGY_PER_OPERATION, CableTier.MV);
        this.coolant = coolant;
        this.condensator = condensator;
    }

    public Item getCoolant() { return coolant; }
    public Item getCondensator() { return condensator; }
    public ContainerData getContainerData() { return containerData; }

    // ==================== slots ====================

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot < INPUT_SLOT_COUNT) return stack.is(coolant);
        if (slot < UPGRADE_SLOT_START + UPGRADE_SLOT_COUNT) return acceptsUpgrade(slot, stack);
        return false;
    }

    /** IC2 {@code InvSlotUpgrade.accepts}: only upgrades for item consuming machines (the pulling upgrade). */
    @Override
    public boolean acceptsUpgrade(int slot, ItemStack stack) {
        return stack.getItem() instanceof com.miophas.singularity_iteration.common.item.upgrade.mio_icif_upgrade upgrade
            && upgrade.isSuitableFor(stack, getUpgradableProperties());
    }

    /** IC2: {@code EnumSet.of(UpgradableProperty.ItemConsuming)}. */
    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.ITEM_CONSUMING);
    }

    @Override
    protected int[] getSlotsForDirection(Direction side) {
        return getInputSlots();
    }

    @Override
    protected int[] getInputSlots() {
        int[] slots = new int[INPUT_SLOT_COUNT];
        for (int i = 0; i < INPUT_SLOT_COUNT; i++) slots[i] = i;
        return slots;
    }

    @Override
    protected int[] getOutputSlots() {
        return new int[0];
    }

    /** IC2 input slots are {@code Access.I}: automation inserts coolant but does not take it out. */
    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        return false;
    }

    @Override
    protected int getBatterySlot() {
        return -1;
    }

    // ==================== work ====================

    public static void tick(Level level, BlockPos pos, BlockState state, AbstractReactorCoolantInjector injector) {
        if (level.isClientSide()) return;
        AbstractProcessingMachineBlockEntity.tick(level, pos, state, injector);
        if (injector.closedFaces < 0 || level.getGameTime() % 20 == 0) {
            injector.closedFaces = ReactorInjectorFaces.refresh(injector, injector.closedFaces);
        }
        injector.inject();
    }

    /** IC2 {@code updateEntityServer}: refills the condensators of its reactor. */
    private void inject() {
        mio_icif_nuclear_reactor_generator reactor =
            hasCoolant() && apiGetStoredEnergy() >= ENERGY_PER_OPERATION ? cachedReactor() : null;
        setActive(reactor != null);
        if (reactor == null || !(reactor.getReactorItemHandler() instanceof IItemHandlerModifiable parts)) return;
        refill(parts.getSlots(), parts::getStackInSlot, (i, stack) -> parts.setStackInSlot(i, stack));
    }

    /**
     * Called by the reactor between two feeds of one operation: with a feed rate above 1 it runs several
     * cycles at once on item copies, and the injector gets its turn between them as it does between
     * two IC2 cycles. Without this a condensator could fill up inside one operation.
     */
    public void refillBetweenFeeds(ItemStack[] parts) {
        if (!hasCoolant() || apiGetStoredEnergy() < ENERGY_PER_OPERATION) return;
        refill(parts.length, i -> parts[i], (i, stack) -> parts[i] = stack);
    }

    /** True for a condensator of this injector's kind that is more than 85% full of heat. */
    public boolean needsRefill(ItemStack part) {
        return part.is(condensator) && part.getItem() instanceof mio_icif_condensator cell
            && (double) cell.getStoredHeat(part) / cell.getMaxHeatStorage() > REFILL_ABOVE;
    }

    /** Each condensator that needs it takes 1 coolant and 1,000 EU (coolant first, as in IC2) and is emptied. */
    private void refill(int size, java.util.function.IntFunction<ItemStack> get, java.util.function.BiConsumer<Integer, ItemStack> set) {
        boolean changed = false;
        for (int i = 0; i < size; i++) {
            ItemStack part = get.apply(i);
            if (!needsRefill(part)) continue;
            if (!consumeCoolant()) break;
            changed = true;
            if (apiUseEnergy(ENERGY_PER_OPERATION, true) < ENERGY_PER_OPERATION || !apiConsumeEnergy(ENERGY_PER_OPERATION)) continue;
            ItemStack refilled = part.copy();
            ((mio_icif_condensator) refilled.getItem()).setStoredHeat(refilled, 0);
            set.accept(i, refilled);
        }
        if (changed) setChanged();
    }

    /** The face that faces whoever placed it. */
    public Direction getFront() {
        BlockState state = getBlockState();
        if (state.hasProperty(mio_icif_block_reactor_coolant_injector.FACING)) return state.getValue(mio_icif_block_reactor_coolant_injector.FACING);
        return Direction.NORTH;
    }

    @Nullable private mio_icif_nuclear_reactor_generator lastReactor;
    private long reactorCheckedAt = Long.MIN_VALUE;

    /** {@link #reactor()} looked up again once a second, or at once when the last one is gone. */
    @Nullable
    private mio_icif_nuclear_reactor_generator cachedReactor() {
        long now = level == null ? 0 : level.getGameTime();
        if (lastReactor == null || lastReactor.isRemoved() || now - reactorCheckedAt >= 20) {
            lastReactor = reactor();
            reactorCheckedAt = now;
        }
        return lastReactor;
    }

    /**
     * The reactor it serves: the reactor, or a chamber of it, on an adjacent face. IC2 looks only behind
     * the front ({@code getFacing().getOpposite()}); here the back comes first, then the front, then the
     * other faces, so an injector also works with its front (or a side) against the reactor.
     */
    @Nullable
    public mio_icif_nuclear_reactor_generator reactor() {
        if (level == null) return null;
        Direction front = getFront();
        mio_icif_nuclear_reactor_generator found = reactorOn(front.getOpposite());
        if (found == null) found = reactorOn(front);
        for (Direction side : Direction.values()) {
            if (found != null) break;
            if (side.getAxis() != front.getAxis()) found = reactorOn(side);
        }
        return found;
    }

    @Nullable
    private mio_icif_nuclear_reactor_generator reactorOn(Direction side) {
        BlockPos at = worldPosition.relative(side);
        if (!level.isLoaded(at)) return null;
        BlockEntity be = level.getBlockEntity(at);
        if (be instanceof mio_icif_nuclear_reactor_generator reactor) return reactor;
        if (be instanceof mio_icif_reactor_chamber chamber) return chamber.getConnectedReactor();
        return null;
    }

    private void setActive(boolean active) {
        if (level == null) return;
        BlockState state = getBlockState();
        if (state.hasProperty(mio_icif_block_reactor_coolant_injector.ACTIVE)
                && state.getValue(mio_icif_block_reactor_coolant_injector.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(mio_icif_block_reactor_coolant_injector.ACTIVE, active), 3);
        }
        isWorking = active;
    }

    private boolean hasCoolant() {
        return getReserveCoolantCount() > 0 || getCoolantCount() > 0;
    }

    /** IC2 {@code inputSlot.consume(1)}; the reserve of an old injector goes first. */
    private boolean consumeCoolant() {
        for (int i = 0; i < reserve.size(); i++) {
            ItemStack stack = reserve.get(i);
            if (!stack.is(coolant)) continue;
            stack.shrink(1);
            if (stack.isEmpty()) reserve.remove(i);
            return true;
        }
        for (int i = 0; i < INPUT_SLOT_COUNT; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.is(coolant)) continue;
            itemHandler.setStackInSlot(i, stack.copyWithCount(stack.getCount() - 1));
            return true;
        }
        return false;
    }

    /** Coolant blocks in the 9 coolant slots. */
    public int getCoolantCount() {
        int count = 0;
        for (int i = 0; i < INPUT_SLOT_COUNT; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.is(coolant)) count += stack.getCount();
        }
        return count;
    }

    /** Coolant blocks in the reserve of an injector saved before 0.1.10.14. */
    public int getReserveCoolantCount() {
        int count = 0;
        for (ItemStack stack : reserve) if (stack.is(coolant)) count += stack.getCount();
        return count;
    }

    /** The reserve (copies), dropped with the block. */
    public List<ItemStack> getReserve() {
        return reserve.stream().map(ItemStack::copy).toList();
    }

    /** Drops the reserve (the block is being removed; the shared machine removal spills the slots). */
    public void dropReserve(Level level, BlockPos pos) {
        for (ItemStack stack : reserve) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
        reserve.clear();
    }

    /** The reserve is part of the contents (the SI wrench carries both in the item). */
    @Override
    public void clearContent() {
        super.clearContent();
        reserve.clear();
    }

    /** It has no recipe progress: the work is {@link #inject()}. */
    @Override protected boolean canWork() { return false; }
    @Override protected void doWork() {}
    @Override protected boolean shouldResetProgress() { return false; }

    /** No electrical contact with the reactor (its packets would blow up this MV machine). */
    @Override
    public boolean canConnect(@Nullable Direction side) {
        return !ReactorInjectorFaces.closed(this, side) && super.canConnect(side);
    }

    // ==================== save ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!reserve.isEmpty()) {
            ListTag list = new ListTag();
            for (ItemStack stack : reserve) list.add(stack.save(registries));
            tag.put(RESERVE_KEY, list);
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        reserve.clear();
        for (Tag entry : tag.getList(RESERVE_KEY, Tag.TAG_COMPOUND)) {
            ItemStack.parse(registries, entry).filter(stack -> !stack.isEmpty()).ifPresent(reserve::add);
        }
        // an inventory larger than the 13 slots is one saved before 0.1.10.14 (54 slots)
        CompoundTag inventory = tag.getCompound("inventory");
        if (inventory.getInt("Size") > SLOT_COUNT) migrateOldInventory(inventory, registries);
    }

    /** Before 0.1.10.14: 53 coolant slots and a battery slot (54). */
    private void migrateOldInventory(CompoundTag inventory, HolderLookup.Provider registries) {
        List<ItemStack> old = new ArrayList<>();
        for (Tag entry : inventory.getList("Items", Tag.TAG_COMPOUND)) {
            if (entry instanceof CompoundTag item) {
                ItemStack.parse(registries, item.getCompound("Item")).filter(stack -> !stack.isEmpty()).ifPresent(old::add);
            }
        }
        for (int i = 0; i < itemHandler.getSlots(); i++) itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        for (ItemStack stack : old) {
            ItemStack left = stack.copy();
            for (int i = 0; i < INPUT_SLOT_COUNT && !left.isEmpty() && left.is(coolant); i++) {
                ItemStack in = itemHandler.getStackInSlot(i);
                if (in.isEmpty()) {
                    itemHandler.setStackInSlot(i, left.copy());
                    left = ItemStack.EMPTY;
                } else if (ItemStack.isSameItemSameComponents(in, left) && in.getCount() < in.getMaxStackSize()) {
                    int moved = Math.min(left.getCount(), in.getMaxStackSize() - in.getCount());
                    itemHandler.setStackInSlot(i, in.copyWithCount(in.getCount() + moved));
                    left.shrink(moved);
                }
            }
            if (!left.isEmpty()) reserve.add(left);
        }
    }
}
