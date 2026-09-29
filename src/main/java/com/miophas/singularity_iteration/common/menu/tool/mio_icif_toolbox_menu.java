package com.miophas.singularity_iteration.common.menu.tool;

import com.miophas.singularity_iteration.common.item.tools.mio_icif_toolbox;
import com.miophas.singularity_iteration.core.prefab.item.AbstractElectricTool;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 工具箱容器菜单。
 * 对齐 IC2 1.12.2 的 ContainerToolbox：
 * 上方为一排 {@code SlotBoxable}（仅收纳实现 IBoxable 的工具），
 * 下方为玩家背包。关闭界面时将内容持久化到工具箱物品的 NBT。
 */
public class mio_icif_toolbox_menu extends AbstractContainerMenu {

    private final ItemStack containerStack;
    private final ItemStack[] inventory;
    private final int inventorySize;
    @Nullable
    private final Player player;

    /** 客户端构造 */
    public mio_icif_toolbox_menu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, ItemStack.EMPTY, extraData.readVarInt());
    }

    /** 服务端构造 */
    public mio_icif_toolbox_menu(int containerId, Inventory playerInventory, ItemStack containerStack, int inventorySize) {
        super(getMenuType(), containerId);
        this.containerStack = containerStack;
        this.inventorySize = inventorySize;
        this.inventory = new ItemStack[inventorySize];
        this.player = playerInventory.player;

        if (!containerStack.isEmpty()) {
            AbstractElectricTool.loadHandHeldInventory(containerStack, inventory, playerInventory.player.level().registryAccess());
        }

        // 对齐 IC2: ContainerToolbox - 一排 SlotBoxable，y=41
        for (int i = 0; i < inventorySize; i++) {
            final int slot = i;
            this.addSlot(new Slot(new ToolboxInventory(), slot, 8 + i * 18, 41) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return mio_icif_toolbox.canBeStored(stack);
                }
            });
        }

        // 对齐 IC2: ContainerToolbox - 玩家背包 y=84 / 快捷栏 y=142
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    private static MenuType<mio_icif_toolbox_menu> getMenuType() {
        return mio_icif_tool_menus.TOOLBOX_MENU.get();
    }

    public ItemStack getContainerStack() {
        return containerStack;
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

        if (index < inventorySize) {
            // 工具箱 -> 玩家背包
            if (!this.moveItemStackTo(slotStack, inventorySize, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // 玩家背包 -> 工具箱（moveItemStackTo 内部按 mayPlace 过滤）
            if (!this.moveItemStackTo(slotStack, 0, inventorySize, false)) {
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
    public void removed(Player player) {
        // 对齐 IC2: HandHeldInventory.onGuiClosed - 关闭时持久化
        if (!containerStack.isEmpty()) {
            AbstractElectricTool.saveHandHeldInventory(containerStack, inventory, player.level().registryAccess());
        }
        super.removed(player);
    }

    /**
     * 包装 inventory[] 数组的内部容器，
     * 对齐 IC2 的 HandHeldInventory（每次变更即标记，关闭/保存时写回 NBT）。
     */
    private class ToolboxInventory implements net.minecraft.world.Container {
        @Override
        public int getContainerSize() { return inventorySize; }

        @Override
        public boolean isEmpty() {
            for (ItemStack stack : inventory) {
                if (stack != null && !stack.isEmpty()) return false;
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            if (slot >= 0 && slot < inventory.length && inventory[slot] != null) {
                return inventory[slot];
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            if (slot >= 0 && slot < inventory.length && inventory[slot] != null && !inventory[slot].isEmpty()) {
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
                return result == null ? ItemStack.EMPTY : result;
            }
            return ItemStack.EMPTY;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (slot >= 0 && slot < inventory.length) {
                inventory[slot] = stack;
            }
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
}
