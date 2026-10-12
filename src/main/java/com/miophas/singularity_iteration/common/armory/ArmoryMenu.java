package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.common.menu.base.mio_icif_machine_menu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Armory GUI: six suit rows (name + helmet, chestplate, leggings, boots, main hand,
 * off hand), battery slot, energy and owner read-out.
 */
@SuppressWarnings("null")
public class ArmoryMenu extends mio_icif_machine_menu {
    public static final int DATA_COUNT = 12;
    public static final int GRID_X = 66, GRID_Y = 19, ROW_H = 18;
    public static final int BATTERY_X = 8, BATTERY_Y = 130;
    public static final int GUI_HEIGHT = 232;

    @Nullable private final BlockPos pos;

    public ArmoryMenu(int containerId, Inventory inventory, @Nullable RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, null, null, buf != null ? buf.readBlockPos() : null);
    }

    public ArmoryMenu(int containerId, Inventory inventory, @Nullable mio_icif_armory armory, @Nullable ContainerData data) {
        this(containerId, inventory, armory, data, armory != null ? armory.getBlockPos() : null);
    }

    private ArmoryMenu(int containerId, Inventory inventory, @Nullable mio_icif_armory armory, @Nullable ContainerData data,
                       @Nullable BlockPos pos) {
        super(ArmoryRegistry.ARMORY_MENU.get(), containerId, mio_icif_armory.SLOT_COUNT, inventory,
            armory != null ? armory.getItemHandler() : null, data, DATA_COUNT, armory);
        this.pos = pos;
    }

    @Override
    protected void addMachineSlots() {
        addBatterySlot(mio_icif_armory.BATTERY_SLOT, BATTERY_X, BATTERY_Y);
        for (int set = 0; set < mio_icif_armory.SETS; set++) {
            for (int col = 0; col < mio_icif_armory.PIECES; col++) {
                ArmoryPiece piece = ArmoryPiece.COLUMNS[col];
                int index = mio_icif_armory.slotOf(set, piece);
                var slot = new SlotItemHandler(itemHandler, index, GRID_X + col * 18, GRID_Y + set * ROW_H) {
                    @Override public boolean mayPlace(ItemStack stack) { return ArmoryRules.fits(stack, piece); }
                    @Override public int getMaxStackSize() { return 1; }
                    @Override public int getMaxStackSize(ItemStack stack) { return 1; }
                };
                slot.setBackground(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS, emptyIcon(piece));
                addSlot(slot);
            }
        }
    }

    /** Vanilla empty-slot silhouettes so each column reads at a glance. */
    private static net.minecraft.resources.ResourceLocation emptyIcon(ArmoryPiece piece) {
        return switch (piece) {
            case HEAD -> net.minecraft.world.inventory.InventoryMenu.EMPTY_ARMOR_SLOT_HELMET;
            case CHEST -> net.minecraft.world.inventory.InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE;
            case LEGS -> net.minecraft.world.inventory.InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS;
            case FEET -> net.minecraft.world.inventory.InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS;
            case OFFHAND -> net.minecraft.world.inventory.InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD;
            case MAINHAND -> net.minecraft.resources.ResourceLocation.withDefaultNamespace("item/empty_slot_sword");
        };
    }

    /**
     * 0.1.8.0: Shift + click from the player inventory fills a suit slot first. Before, an electric
     * armor piece went to the battery slot (the generic machine menu tries chargeable items there
     * first). Order: an empty matching slot of a suit that already has other pieces (complete the
     * suit), then the first empty matching slot of any suit, then the generic rules (battery slot).
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index >= mio_icif_armory.SLOT_COUNT && index < slots.size()) {
            var source = slots.get(index);
            if (source.hasItem()) {
                ItemStack stack = source.getItem();
                int target = suitSlotFor(stack);
                if (target >= 0) {
                    ItemStack copy = stack.copy();
                    if (moveItemStackTo(stack, target, target + 1, false)) {
                        if (stack.isEmpty()) source.setByPlayer(ItemStack.EMPTY); else source.setChanged();
                        source.onTake(player, stack);
                        return copy;
                    }
                }
            }
        }
        return super.quickMoveStack(player, index);
    }

    /** Menu slot index of the best empty suit slot for {@code stack}, or -1. */
    private int suitSlotFor(ItemStack stack) {
        int firstFree = -1;
        for (int set = 0; set < mio_icif_armory.SETS; set++) {
            boolean partial = false;
            int free = -1;
            for (ArmoryPiece piece : ArmoryPiece.COLUMNS) {
                int index = mio_icif_armory.slotOf(set, piece);
                var slot = slots.get(index);
                if (slot.hasItem()) partial = true;
                else if (free < 0 && ArmoryRules.fits(stack, piece)) free = index;
            }
            if (free < 0) continue;
            if (partial) return free;
            if (firstFree < 0) firstFree = free;
        }
        return firstFree;
    }

    @Override protected int getPlayerInventoryY() { return 150; }
    @Override protected int getPlayerHotbarY() { return 208; }
    @Override protected int getBatterySlotIndex() { return mio_icif_armory.BATTERY_SLOT; }
    @Override protected int getDataSlotCount() { return DATA_COUNT; }

    @Nullable public BlockPos pos() { return pos; }

    public long energy() {
        return (data.get(0) & 0xFFFFL) | ((long) (data.get(1) & 0xFFFF) << 16) | ((long) data.get(2) << 32);
    }
    public boolean bound() { return data.get(3) != 0; }
    public boolean busy() { return data.get(4) != 0; }
    public mio_icif_armory.Automation automation() {
        return mio_icif_armory.Automation.values()[Math.floorMod(data.get(5), mio_icif_armory.Automation.values().length)];
    }
    public int mana() { return data.get(6); }
    public int maintenanceState() { return data.get(7); }
    public int etaTicks() { return data.get(8); }
    public int pendingPieces() { return data.get(9); }
    public long euNeeded() { return data.get(10) & 0xFFFFFFFFL; }
    public int manaNeeded() { return data.get(11); }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity instanceof mio_icif_armory armory && !armory.mayAccess(player)) return false;
        return super.stillValid(player);
    }
}
