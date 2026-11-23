package com.miophas.singularity_iteration.common.blockentity.batbox;

import com.miophas.singularity_iteration.common.block.mio_icif_entity_block;
import com.miophas.singularity_iteration.common.menu.storage.TesuControllerMenu;
import com.miophas.singularity_iteration.common.multiblock.TesuStructure;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.api.machine.IMultiblockStructure;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.MultiblockEnergyCore;
import com.miophas.singularity_iteration.core.prefab.blockentity.MultiblockEnergyPart;
import com.miophas.singularity_iteration.core.runtime.energy.DirectItemCharging;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.158: 227,000,000,000 EU (189 cells); a wireless output port in a port slot is bound too (the field of {@link TesuWireless}).
 *
 * <p>0.1.11.168: the structure is a 7 x 7 x 13 box now (still 189 cells, the same capacity).
 *
 * <p>0.1.11.157: the TESU (太级储电罐) controller: the core of the 7 x 7 x 13 storage cylinder, 200,000,000,000 EU. It works as the
 * GESU core does: it stores, and its ports move the energy in and out (input ports at any voltage, output ports at the voltage
 * set on each). Unlike the GESU core it stands in the outer ring, so it takes and gives nothing on its own faces: a cable on
 * it would go round the ports.
 *
 * <p>It checks its structure ({@link TesuStructure}) every second; when formed it binds the ports in the port slots, when broken
 * it lets them go. A check that meets an unloaded chunk changes nothing. A charge slot and a discharge slot as on the GESU.
 */
@SuppressWarnings("null")
public class TesuControllerBlockEntity extends MultiblockEnergyCore implements Container,
        com.miophas.singularity_iteration.core.api.machine.IStorageMember {

    public static final long CAPACITY = 227_000_000_000L;   // 0.1.11.158: 189 cells (was 200B with 81)
    public static final int CHARGE_SLOT = 0, DISCHARGE_SLOT = 1;
    /** Ticks between structure checks. */
    public static final int CHECK_TICKS = 20;
    private static final ICableTier MAX_TIER = MioIcifAPI.instance().getEnergyNetAPI().getCableTier("max");

    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private final List<BlockPos> inputs = new ArrayList<>(), outputs = new ArrayList<>(), wireless = new ArrayList<>();
    private int checkIn;
    /** The first wrong position of the last failed check (for the screen), and what belongs there. */
    @Nullable private BlockPos wrongAt;
    @Nullable private TesuStructure.Part expected;

    public TesuControllerBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.TESU_CONTROLLER.get(), CAPACITY, 0, 0, MAX_TIER);
    }

    @Override public BlockEntity storageOwner() { return this; }

    public static void tick(Level level, BlockPos pos, BlockState state, TesuControllerBlockEntity be) {
        if (level.isClientSide()) return;
        AbstractEnergyBlockEntity.tick(level, pos, state, be);
        be.tickItems();
        if (--be.checkIn <= 0) {
            be.checkIn = CHECK_TICKS;
            be.check();
        }
    }

    private Direction facing() {
        BlockState s = getBlockState();
        return s.hasProperty(mio_icif_entity_block.FACING) ? s.getValue(mio_icif_entity_block.FACING) : Direction.NORTH;
    }

    /** Checks the structure now and binds or lets go of the ports. */
    public void check() {
        if (level == null || level.isClientSide()) return;
        TesuStructure.Check c = TesuStructure.get().check(level, worldPosition, facing());
        if (c.unloaded()) return;
        wrongAt = c.wrongAt();
        expected = c.expected();
        if (c.formed()) form(c.inputs(), c.outputs(), c.wireless());
        else if (isStructureComplete() || !inputs.isEmpty() || !outputs.isEmpty() || !wireless.isEmpty()) breakUp();
    }

    private void form(List<BlockPos> in, List<BlockPos> out, List<BlockPos> wl) {
        List<BlockPos> gone = new ArrayList<>(inputs);
        gone.addAll(outputs);
        gone.addAll(wireless);
        gone.removeAll(in);
        gone.removeAll(out);
        gone.removeAll(wl);
        for (BlockPos p : gone) detach(p);
        inputs.clear();
        inputs.addAll(in);
        outputs.clear();
        outputs.addAll(out);
        wireless.clear();
        wireless.addAll(wl);
        for (BlockPos p : inputs) bind(p);
        for (BlockPos p : outputs) bind(p);
        for (BlockPos p : wireless) bind(p);
        setStructureComplete(true);
        setChanged();
    }

    private void breakUp() {
        for (BlockPos p : inputs) detach(p);
        for (BlockPos p : outputs) detach(p);
        for (BlockPos p : wireless) detach(p);
        inputs.clear();
        outputs.clear();
        wireless.clear();
        setStructureComplete(false);
        setChanged();
    }

    private void bind(BlockPos p) {
        if (level.isLoaded(p) && level.getBlockEntity(p) instanceof TesuWirelessPortBlockEntity port) { port.bind(worldPosition); return; }
        if (level.isLoaded(p) && level.getBlockEntity(p) instanceof MultiblockEnergyPart part
                && (!part.isStructureCompleted() || !worldPosition.equals(part.getCorePosition()))) {
            part.setCorePosition(worldPosition);
            part.setStructureCompleted(true);
            part.setChanged();
        }
    }

    private void detach(BlockPos p) {
        if (level.isLoaded(p) && level.getBlockEntity(p) instanceof TesuWirelessPortBlockEntity port) { port.bind(null); return; }
        if (level.isLoaded(p) && level.getBlockEntity(p) instanceof MultiblockEnergyPart part && worldPosition.equals(part.getCorePosition()))
            part.detachFromCore();
    }

    /** The block is gone: let every port go. */
    public void released() { breakUp(); }

    @Override public void onMultiblockFormed(IMultiblockStructure structure) { check(); }
    @Override public void onMultiblockBroken() { breakUp(); }

    // ---- readings ------------------------------------------------------------------------------------------------------

    public int inputCount() { return inputs.size(); }
    public int outputCount() { return outputs.size(); }
    public int wirelessCount() { return wireless.size(); }
    @Nullable public BlockPos wrongAt() { return wrongAt; }
    @Nullable public TesuStructure.Part expected() { return expected; }

    /** EU a tick the input ports can take in (each a MAX packet). */
    public long inputRate() { return inputs.size() * MAX_TIER.getPowerRating(); }

    /** EU a tick the output ports can send (each one packet of its voltage). */
    public long outputRate() {
        long sum = 0;
        if (level == null) return 0;
        for (BlockPos p : outputs)
            if (level.isLoaded(p) && level.getBlockEntity(p) instanceof TesuOutputPortBlockEntity port) sum += port.rate();
        return sum;
    }

    // ---- no energy through the controller's own faces ---------------------------------------------------------------

    @Override public double getDemandedEnergy() { return 0.0D; }
    @Override public double injectEnergy(Direction direction, double amount, double voltage) { return amount; }

    // ---- items (as the GESU core) -----------------------------------------------------------------------------------

    public static long itemRate() { return MioIcifAPI.instance().getEnergyNetAPI().getCableTier("luv").getPowerRating(); }

    public void tickItems() {
        var storage = energyStorage;
        boolean changed = false;
        ItemStack charge = items.get(CHARGE_SLOT);
        if (!charge.isEmpty() && storage.getAmount() > 0) {
            long moved = DirectItemCharging.charge(charge, Math.min(itemRate(), storage.getAmount()));
            if (moved > 0) { storage.setEnergy(storage.getAmount() - moved); changed = true; }
        }
        ItemStack discharge = items.get(DISCHARGE_SLOT);
        long room = storage.getCapacity() - storage.getAmount();
        if (!discharge.isEmpty() && room > 0) {
            long moved = DirectItemCharging.discharge(discharge, Math.min(itemRate(), room));
            if (moved > 0) { storage.setEnergy(storage.getAmount() + moved); changed = true; }
        }
        if (changed) setChanged();
    }

    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return slot >= 0 && slot < items.size() ? items.get(slot) : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack out = ContainerHelper.removeItem(items, slot, amount);
        if (!out.isEmpty()) setChanged();
        return out;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= items.size()) return;
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == CHARGE_SLOT ? DirectItemCharging.canCharge(stack) : slot == DISCHARGE_SLOT && DirectItemCharging.canDischarge(stack);
    }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { items.clear(); setChanged(); }

    @Override
    public Component getDisplayName() { return Component.translatable("container.mio_icif.tesu_controller"); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new TesuControllerMenu(containerId, playerInventory, this);
    }

    // ---- save --------------------------------------------------------------------------------------------------------

    private static ListTag positions(List<BlockPos> list) {
        ListTag tag = new ListTag();
        for (BlockPos p : list) tag.add(NbtUtils.writeBlockPos(p));
        return tag;
    }

    private static void positions(CompoundTag tag, String key, List<BlockPos> into) {
        into.clear();
        for (Tag t : tag.getList(key, Tag.TAG_INT_ARRAY)) {
            if (t instanceof net.minecraft.nbt.IntArrayTag a && a.size() == 3) into.add(new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("TesuInputs", positions(inputs));
        tag.put("TesuOutputs", positions(outputs));
        tag.put("TesuWireless", positions(wireless));
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        positions(tag, "TesuInputs", inputs);
        positions(tag, "TesuOutputs", outputs);
        positions(tag, "TesuWireless", wireless);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        checkIn = 0;   // check the world on the first tick
    }
}
