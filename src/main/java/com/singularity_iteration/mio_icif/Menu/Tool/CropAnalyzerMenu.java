package com.singularity_iteration.mio_icif.Menu.Tool;

import com.singularity_iteration.mio_icif.Items.Crop.CropSeedItem;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.Items.Tools.CropAnalyzerItem;
import com.singularity_iteration.mio_icif.Items.Tools.mio_icif_tool_elc;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class CropAnalyzerMenu extends AbstractContainerMenu {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_BATTERY = 2;

    private final ItemStack containerStack;
    private final ItemStack[] inventory = new ItemStack[3];
    @Nullable
    private final Player player;

    public CropAnalyzerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        super(getMenuType(), containerId);
        this.containerStack = ItemStack.EMPTY;
        this.player = null;
        initSlots();
        addPlayerInventorySlots(playerInventory);
    }

    public CropAnalyzerMenu(int containerId, Inventory playerInventory, ItemStack containerStack) {
        super(getMenuType(), containerId);
        this.containerStack = containerStack;
        this.player = playerInventory.player;
        initSlots();
        addPlayerInventorySlots(playerInventory);

        if (!containerStack.isEmpty()) {
            mio_icif_tool_elc.loadHandHeldInventory(containerStack, inventory, player.level().registryAccess());
        }
    }

    private void initSlots() {
        for (int i = 0; i < 3; i++) {
            inventory[i] = ItemStack.EMPTY;
        }
        this.addSlot(new Slot(new CropAnalyzerInventory(), SLOT_INPUT, 8, 7));
        this.addSlot(new Slot(new CropAnalyzerInventory(), SLOT_OUTPUT, 41, 7) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new Slot(new CropAnalyzerInventory(), SLOT_BATTERY, 152, 7) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return MioIcifAPI.instance().getItemAPI().isBattery(stack);
            }
        });
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        int xStart = 8;
        int yStart = 141;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                    xStart + col * 18, yStart + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col,
                xStart + col * 18, 199));
        }
    }

    private static MenuType<CropAnalyzerMenu> getMenuType() {
        return mio_icif_tool_menus.CROP_ANALYZER_MENU.get();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }

        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack slotStack = slot.getItem();
        ItemStack result = slotStack.copy();

        if (index < 3) {
            if (!this.moveItemStackTo(slotStack, 3, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (MioIcifAPI.instance().getItemAPI().isBattery(slotStack)) {
                if (!this.moveItemStackTo(slotStack, SLOT_BATTERY, SLOT_BATTERY + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotStack.getItem() instanceof CropSeedItem) {
                if (!this.moveItemStackTo(slotStack, SLOT_INPUT, SLOT_INPUT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (slotStack.getCount() == result.getCount()) {
            return ItemStack.EMPTY;
        }

        if (slotStack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setByPlayer(slotStack);
        }
        return result;
    }

    @Override
    public void broadcastChanges() {
        tryChargeFromBattery();
        tryScan();
        super.broadcastChanges();
    }

    @Override
    public void removed(Player player) {
        if (!containerStack.isEmpty()) {
            mio_icif_tool_elc.saveHandHeldInventory(containerStack, inventory, player.level().registryAccess());
        }
        super.removed(player);
    }

    private void tryChargeFromBattery() {
        ItemStack battery = inventory[SLOT_BATTERY];
        if (battery.isEmpty()) return;
        if (!(containerStack.getItem() instanceof CropAnalyzerItem analyzer)) return;
        var api = MioIcifAPI.instance().getItemAPI();
        if (!api.isBattery(battery)) return;

        long maxEnergy = analyzer.getMaxEnergy();
        long currentEnergy = analyzer.getEnergy(containerStack);
        if (currentEnergy >= maxEnergy) return;

        long transferRate = analyzer.getChargeRate();
        long needed = Math.min(transferRate, maxEnergy - currentEnergy);
        long available = api.getBatteryStored(battery);
        long toTransfer = Math.min(needed, available);

        if (toTransfer > 0) {
            api.dischargeBattery(battery, toTransfer, false);
            analyzer.addEnergy(containerStack, toTransfer);
        }
    }

    public void tryScan() {
        ItemStack input = inventory[SLOT_INPUT];
        ItemStack output = inventory[SLOT_OUTPUT];

        if (input.isEmpty() && output.isEmpty()) return;

        if (!output.isEmpty() && input.isEmpty()) {
            if (output.getItem() instanceof CropSeedItem) {
                int level = CropSeedItem.getScanLevel(output);
                if (level < 4) {
                    inventory[SLOT_INPUT] = output;
                    inventory[SLOT_OUTPUT] = ItemStack.EMPTY;
                    input = inventory[SLOT_INPUT];
                } else {
                    return;
                }
            } else {
                return;
            }
        }

        if (!output.isEmpty()) return;
        if (input.isEmpty()) return;
        if (!(input.getItem() instanceof CropSeedItem)) return;

        int level = CropSeedItem.getScanLevel(input);

        if (level < 4) {
            int need = CropAnalyzerItem.energyForLevel(level);
            boolean consumed = false;

            if (containerStack.getItem() instanceof CropAnalyzerItem analyzer) {
                consumed = analyzer.consumeEnergy(containerStack, need);
            }

            if (!consumed) return;

            CropSeedItem.incrementScanLevel(input);
        }

        inventory[SLOT_OUTPUT] = input.copy();
        inventory[SLOT_INPUT] = ItemStack.EMPTY;
    }

    public ItemStack getOutputStack() {
        return inventory[SLOT_OUTPUT];
    }

    private class CropAnalyzerInventory implements net.minecraft.world.Container {
        @Override
        public int getContainerSize() { return 3; }

        @Override
        public boolean isEmpty() {
            for (ItemStack stack : inventory) {
                if (!stack.isEmpty()) return false;
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            if (slot >= 0 && slot < inventory.length) return inventory[slot];
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            if (slot >= 0 && slot < inventory.length && !inventory[slot].isEmpty()) {
                ItemStack result = inventory[slot].split(amount);
                if (inventory[slot].isEmpty()) inventory[slot] = ItemStack.EMPTY;
                return result;
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            if (slot >= 0 && slot < inventory.length) {
                ItemStack result = inventory[slot];
                inventory[slot] = ItemStack.EMPTY;
                return result;
            }
            return ItemStack.EMPTY;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (slot >= 0 && slot < inventory.length) inventory[slot] = stack;
        }

        @Override
        public void setChanged() {}

        @Override
        public void clearContent() {
            for (int i = 0; i < inventory.length; i++) inventory[i] = ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) { return true; }
    }

    public static class Provider implements MenuProvider {
        private final ItemStack containerStack;

        public Provider(ItemStack containerStack, net.minecraft.world.InteractionHand hand) {
            this.containerStack = containerStack;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("gui.mio_icif.crop_analyzer.title");
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new CropAnalyzerMenu(containerId, playerInventory, containerStack);
        }
    }
}