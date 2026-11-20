package com.miophas.singularity_iteration.common.menu.fusion;

import com.miophas.singularity_iteration.common.blockentity.fusion.FusionItemPortBlockEntity;
import com.miophas.singularity_iteration.common.blockentity.fusion.FusionItemPortBlockEntity.Kind;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_base_menu;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.97: the screen of the fusion reactor's fuel injection port and disruption mitigation port
 * ({@link FusionItemPortBlockEntity}): the port's slots in one row and the player's inventory. Each port has its own menu type
 * (the client builds the slots before it knows the block). Values: linked, the controller's deuterium units or coolant cells,
 * its tritium units.
 */
public class FusionItemPortMenu extends mio_icif_base_menu {
    public static final int W = 176, H = 168, SLOT_Y = 30, INV_Y = 86, HOTBAR_Y = 144, VALUES = 3;
    private static final int[] FUEL_X = {14, 32, 70, 88, 126, 144}, COOLANT_X = {52, 70, 88, 106};

    private final Kind kind;
    private final @Nullable FusionItemPortBlockEntity port;
    private final ContainerLevelAccess access;
    private final int[] values = new int[VALUES];

    /** The client's fuel port menu. */
    public static FusionItemPortMenu fuel(int id, Inventory inventory) { return new FusionItemPortMenu(id, inventory, null, Kind.FUEL); }

    /** The client's mitigation port menu. */
    public static FusionItemPortMenu coolant(int id, Inventory inventory) { return new FusionItemPortMenu(id, inventory, null, Kind.COOLANT); }

    public FusionItemPortMenu(int id, Inventory inventory, FusionItemPortBlockEntity port) {
        this(id, inventory, port, port.kind());
    }

    private FusionItemPortMenu(int id, Inventory inventory, @Nullable FusionItemPortBlockEntity port, Kind kind) {
        super(kind == Kind.FUEL ? mio_icif_menus.FUSION_FUEL_PORT_MENU_TYPE.get() : mio_icif_menus.FUSION_COOLANT_PORT_MENU_TYPE.get(), id,
            kind.slots, kind.slots);
        this.kind = kind;
        this.port = port;
        this.access = port == null || port.getLevel() == null ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(port.getLevel(), port.getBlockPos());
        IItemHandler items = port != null ? port.getItems() : new ItemStackHandler(kind.slots) {
            @Override public boolean isItemValid(int slot, ItemStack stack) { return FusionItemPortBlockEntity.accepts(kind, slot, stack); }
        };
        int[] xs = slotX(kind);
        for (int i = 0; i < kind.slots; i++) addSlot(new SlotItemHandler(items, i, xs[i], SLOT_Y));
        addPlayerInventory(inventory, INV_Y, HOTBAR_Y);
        addDataSlots(new ContainerData() {
            @Override public int get(int i) { return values[i]; }
            @Override public void set(int i, int value) { values[i] = value; }
            @Override public int getCount() { return VALUES; }
        });
    }

    /** The x of each port slot's item (the frame is one pixel further out). */
    public static int[] slotX(Kind kind) {
        return (kind == Kind.FUEL ? FUEL_X : COOLANT_X).clone();
    }

    public Kind kind() { return kind; }

    public boolean linked() { return values[0] != 0; }

    /** The controller's deuterium units (fuel port) or the coolant cells in its mitigation slot (mitigation port). */
    public int first() { return values[1]; }

    /** The controller's tritium units (fuel port). */
    public int second() { return values[2]; }

    @Override
    public void broadcastChanges() {
        if (port != null) System.arraycopy(port.screenValues(), 0, values, 0, VALUES);
        super.broadcastChanges();
    }

    @Override
    protected boolean isBattery(ItemStack stack) {
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return port == null || !port.isRemoved() && stillValid(access, player, port.getBlockState().getBlock());
    }
}
