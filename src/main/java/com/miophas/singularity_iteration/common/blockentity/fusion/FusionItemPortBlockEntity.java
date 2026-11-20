package com.miophas.singularity_iteration.common.blockentity.fusion;

import com.miophas.singularity_iteration.common.item.normal.mio_icif_normal;
import com.miophas.singularity_iteration.common.menu.fusion.FusionItemPortMenu;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.97: the fuel injection port and the disruption mitigation port as access ports with slots of their own and a screen,
 * like the fission reactor's fluid port. Pipes and hoppers (and players) fill them; nothing but the port's own items goes in, so
 * automation can never put a fuel rod or a coolant cell into the blanket:
 * <ul>
 *   <li>{@link Kind#FUEL}: two slots of deuterium fuel rods, two of tritium fuel rods, and two that give the empty fuel rods back
 *       (the only slots a pipe or hopper can take from).</li>
 *   <li>{@link Kind#COOLANT}: four slots of reactor coolant cells (10k, 30k, 60k) for the disruption mitigation.</li>
 * </ul>
 * The controller the port serves calls {@link #serve} every tick: whenever its own deuterium, tritium or mitigation slot is
 * empty it takes one rod or cell from the port, and it hands its empty fuel rods to the port's output slots. Items stay in the
 * port while the structure is not formed.
 */
public class FusionItemPortBlockEntity extends BlockEntity implements MenuProvider {
    /** Which port this is (from its block). */
    public enum Kind {
        FUEL(6), COOLANT(4);

        public final int slots;

        Kind(int slots) { this.slots = slots; }
    }

    /** The fuel port's slots: deuterium 0-1, tritium 2-3, the empty fuel rods 4-5. */
    public static final int DEUTERIUM_0 = 0, TRITIUM_0 = 2, EMPTY_0 = 4;

    private final Kind kind;
    private final ItemStackHandler items;
    /** What pipes and hoppers see: the inputs take only their own items, only the empty-rod outputs give. */
    private final IItemHandler automation = new IItemHandler() {
        @Override public int getSlots() { return items.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return items.insertItem(slot, stack, simulate); }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return isOutput(slot) ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return items.isItemValid(slot, stack); }
    };

    public FusionItemPortBlockEntity(BlockPos pos, BlockState state) {
        super(mio_icif_block_entities.FUSION_ITEM_PORT_ENTITY_TYPE.get(), pos, state);
        kind = kindOf(state);
        items = new ItemStackHandler(kind.slots) {
            @Override public boolean isItemValid(int slot, ItemStack stack) { return accepts(kind, slot, stack); }
            @Override protected void onContentsChanged(int slot) { setChanged(); }
        };
    }

    public static Kind kindOf(BlockState state) {
        return state.is(mio_icif_blocks.FUSION_MITIGATION_PORT.get()) ? Kind.COOLANT : Kind.FUEL;
    }

    /** What a slot takes from a player, a pipe or a hopper (the empty-rod outputs take nothing: the controller fills them). */
    public static boolean accepts(Kind kind, int slot, ItemStack stack) {
        if (stack.isEmpty() || slot < 0 || slot >= kind.slots) return false;
        if (kind == Kind.COOLANT) return FusionControllerBlockEntity.isCoolant(stack);
        if (slot < TRITIUM_0) return stack.is(mio_icif_normal.DEUTERIUM_CELL.get());
        if (slot < EMPTY_0) return stack.is(mio_icif_normal.TRITIUM_CELL.get());
        return false;
    }

    public static boolean isOutput(Kind kind, int slot) {
        return kind == Kind.FUEL && slot >= EMPTY_0 && slot < kind.slots;
    }

    private boolean isOutput(int slot) { return isOutput(kind, slot); }

    public Kind kind() { return kind; }

    /** The port's own slots (the menu's). */
    public IItemHandlerModifiable getItems() { return items; }

    /** For pipes and hoppers. */
    public IItemHandler automation() { return automation; }

    // ------------------------------------------------------------------ the controller's side

    /**
     * Called by the controller this port serves, every tick: one rod or cell into each of the controller's slots it feeds that
     * is empty, and the controller's empty fuel rods into the output slots (as many as fit).
     */
    void serve(IItemHandlerModifiable controller) {
        if (kind == Kind.COOLANT) {
            feed(controller, FusionControllerBlockEntity.SLOT_MITIGATION, 0, kind.slots);
            return;
        }
        feed(controller, FusionControllerBlockEntity.SLOT_DEUTERIUM, DEUTERIUM_0, TRITIUM_0);
        feed(controller, FusionControllerBlockEntity.SLOT_TRITIUM, TRITIUM_0, EMPTY_0);
        takeEmpty(controller, FusionControllerBlockEntity.SLOT_EMPTY_CELL);
        takeEmpty(controller, FusionControllerBlockEntity.SLOT_EMPTY_ROD);
    }

    private void feed(IItemHandlerModifiable controller, int target, int from, int to) {
        if (!controller.getStackInSlot(target).isEmpty()) return;
        for (int i = from; i < to; i++) {
            ItemStack stack = items.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            if (controller.insertItem(target, stack.copyWithCount(1), false).isEmpty()) items.extractItem(i, 1, false);
            return;
        }
    }

    private void takeEmpty(IItemHandlerModifiable controller, int source) {
        ItemStack rods = controller.getStackInSlot(source).copy();
        if (rods.isEmpty()) return;
        int moved = 0;
        for (int i = EMPTY_0; i < kind.slots && moved < rods.getCount(); i++) {
            ItemStack there = items.getStackInSlot(i);
            if (!there.isEmpty() && !ItemStack.isSameItemSameComponents(there, rods)) continue;
            int has = there.isEmpty() ? 0 : there.getCount();
            int n = Math.min(rods.getMaxStackSize() - has, rods.getCount() - moved);
            if (n <= 0) continue;
            items.setStackInSlot(i, rods.copyWithCount(has + n));
            moved += n;
        }
        if (moved > 0) controller.extractItem(source, moved, false);
    }

    /** The controller this port serves (formed), if any. */
    public @Nullable FusionControllerBlockEntity controller() {
        return level == null || level.isClientSide ? null : FusionPortRegistry.controller(level, worldPosition);
    }

    /**
     * For the screen: {linked (0 or 1), the controller's deuterium units or the coolant cells in its mitigation slot, its
     * tritium units}.
     */
    public int[] screenValues() {
        FusionControllerBlockEntity c = controller();
        if (c == null) return new int[]{0, 0, 0};
        if (kind == Kind.COOLANT) return new int[]{1, c.getItems().getStackInSlot(FusionControllerBlockEntity.SLOT_MITIGATION).getCount(), 0};
        return new int[]{1, (int) c.deuterium(), (int) c.tritium()};
    }

    /** Everything in the port (dropped when it is broken). */
    public List<ItemStack> contents() {
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < items.getSlots(); i++)
            if (!items.getStackInSlot(i).isEmpty()) out.add(items.getStackInSlot(i).copy());
        return out;
    }

    // ------------------------------------------------------------------ menu and saves

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new FusionItemPortMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Items")) {
            items.deserializeNBT(registries, tag.getCompound("Items"));
            if (items.getSlots() != kind.slots) items.setSize(kind.slots);
        }
    }
}
