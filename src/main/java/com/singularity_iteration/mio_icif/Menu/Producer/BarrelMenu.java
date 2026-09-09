package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_barrel_entity;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class BarrelMenu extends AbstractContainerMenu {

    public static final int SLOT_RAW_MATERIAL = 0;
    public static final int SLOT_HOPS = 1;
    public static final int SLOT_WATER = 2;
    public static final int SLOT_COUNT = 3;

    private final IItemHandler itemHandler;
    private final ContainerData data;
    @Nullable
    private final mio_icif_barrel_entity blockEntity;

    public BarrelMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public BarrelMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.BARREL_MENU_TYPE.get(), containerId);
        this.itemHandler = itemHandler != null ? itemHandler : new ItemStackHandler(SLOT_COUNT);
        this.data = data != null ? data : new SimpleContainerData(2);
        this.blockEntity = null;

        addMachineSlots();
        addPlayerInventory(playerInventory);
        addDataSlots(this.data);
    }

    public BarrelMenu(int containerId, Inventory playerInventory, mio_icif_barrel_entity blockEntity) {
        super(mio_icif_menus.BARREL_MENU_TYPE.get(), containerId);
        this.itemHandler = blockEntity.getItemHandler();
        this.data = blockEntity.getContainerData();
        this.blockEntity = blockEntity;

        addMachineSlots();
        addPlayerInventory(playerInventory);
        addDataSlots(this.data);
    }

    private void addMachineSlots() {
        // 原材料槽：小麦或甘蔗 (52, 37)
        this.addSlot(new SlotItemHandler(itemHandler, SLOT_RAW_MATERIAL, 52, 37) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.WHEAT) || stack.is(Items.SUGAR_CANE);
            }
        });

        // 啤酒花槽 (80, 37)
        this.addSlot(new SlotItemHandler(itemHandler, SLOT_HOPS, 80, 37) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal.HOPS.get());
            }
        });

        // 水桶�?(108, 37)
        this.addSlot(new SlotItemHandler(itemHandler, SLOT_WATER, 108, 37) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.WATER_BUCKET);
            }

            @Override
            public boolean mayPickup(Player player) {
                // 如果槽位里是水桶，不允许直接取出（必须通过酿造消耗）
                ItemStack stack = getItem();
                if (stack.is(Items.WATER_BUCKET)) {
                    return false;
                }
                return super.mayPickup(player);
            }
        });
    }

    private void addPlayerInventory(Inventory playerInventory) {
        // 玩家背包 (imageHeight=166, 背包Y=166-82=84, 快捷栏Y=166-24=142)
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        net.minecraft.world.inventory.Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemStack1 = slot.getItem();
            itemstack = itemStack1.copy();

            if (index < SLOT_COUNT) {
                // 从机器槽位移到玩家背�?
                if (!this.moveItemStackTo(itemStack1, SLOT_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 从玩家背包移到机器槽位?
                boolean moved = false;

                // 尝试放入水桶�?
                if (itemStack1.is(Items.WATER_BUCKET)) {
                    if (this.moveItemStackTo(itemStack1, SLOT_WATER, SLOT_WATER + 1, false)) {
                        moved = true;
                    }
                }

                // 尝试放入啤酒花槽
                if (!moved && itemStack1.is(com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal.HOPS.get())) {
                    if (this.moveItemStackTo(itemStack1, SLOT_HOPS, SLOT_HOPS + 1, false)) {
                        moved = true;
                    }
                }

                // 尝试放入原材料槽
                if (!moved && (itemStack1.is(Items.WHEAT) || itemStack1.is(Items.SUGAR_CANE))) {
                    if (this.moveItemStackTo(itemStack1, SLOT_RAW_MATERIAL, SLOT_RAW_MATERIAL + 1, false)) {
                        moved = true;
                    }
                }

                if (!moved) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemStack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setByPlayer(itemStack1);
            }
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null) return true;
        return blockEntity.stillValid(player);
    }

    public int getProgress() {
        return data.get(0);
    }

    public int getMaxProgress() {
        return data.get(1);
    }

    @Nullable
    public mio_icif_barrel_entity getBlockEntity() {
        return blockEntity;
    }
}

