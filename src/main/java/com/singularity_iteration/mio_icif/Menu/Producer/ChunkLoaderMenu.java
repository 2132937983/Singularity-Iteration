package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_chunk_loader;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class ChunkLoaderMenu extends mio_icif_machine_menu {

    public static final int BATTERY_SLOT = 0;
    public static final int UPGRADE_SLOT_START = 1;
    public static final int UPGRADE_SLOT_COUNT = 4;
    public static final int SLOT_COUNT = 5;

    public static final int GUI_HEIGHT = 250;

    @Nullable
    private net.minecraft.core.BlockPos blockPos;

    public ChunkLoaderMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        this(containerId, playerInventory, null, null);
        if (data != null) {
            this.blockPos = data.readBlockPos();
        }
    }

    public ChunkLoaderMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public ChunkLoaderMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_chunk_loader blockEntity) {
        this(containerId, playerInventory, blockEntity, blockEntity != null ? blockEntity.getContainerData() : null);
    }

    public ChunkLoaderMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_chunk_loader blockEntity, @Nullable ContainerData data) {
        super(mio_icif_menus.CHUNK_LOADER_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
            blockEntity != null ? blockEntity.getItemHandler() : null, data, 8, blockEntity);
        if (blockEntity != null) {
            this.blockPos = blockEntity.getBlockPos();
        }
    }

    @Nullable
    public net.minecraft.core.BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, BATTERY_SLOT, 8, 143) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isBattery(stack);
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                return 1;
            }
        });

        for (int i = 0; i < UPGRADE_SLOT_COUNT; i++) {
            addUpgradeSlot(itemHandler, UPGRADE_SLOT_START + i, 8, 44 + i * 18);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack1 = slot.getItem();
            itemstack = itemStack1.copy();

            if (index < SLOT_COUNT) {
                if (!this.moveItemStackTo(itemStack1, this.playerInventoryStart, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= this.playerInventoryStart) {
                boolean moved = false;

                if (isBattery(itemStack1)) {
                    if (this.moveItemStackTo(itemStack1, BATTERY_SLOT, BATTERY_SLOT + 1, false)) {
                        moved = true;
                    }
                }

                if (!moved && isUpgrade(itemStack1)) {
                    if (this.moveItemStackTo(itemStack1, UPGRADE_SLOT_START, UPGRADE_SLOT_START + UPGRADE_SLOT_COUNT, false)) {
                        moved = true;
                    }
                }

                if (!moved) {
                    if (!this.moveItemStackTo(itemStack1, this.playerInventoryStart, this.slots.size(), true)) {
                        return ItemStack.EMPTY;
                    }
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
    protected int getPlayerInventoryY() { return 168; }

    @Override
    protected int getPlayerHotbarY() { return 226; }

    @Override
    protected int getBatterySlotIndex() { return BATTERY_SLOT; }

    @Override
    protected int getUpgradeSlotStart() { return UPGRADE_SLOT_START; }

    @Override
    protected int getUpgradeSlotCount() { return UPGRADE_SLOT_COUNT; }

    public int getEnergy() { return data.get(0); }
    public int getMaxEnergy() { return data.get(1); }
    public boolean isActive() { return data.get(2) == 1; }
    public int getLoadedChunkCount() { return data.get(3); }
    public int getMaxChunks() { return data.get(4); }

    public boolean isChunkLoaded(int dx, int dz) {
        int radius = 4;
        int idx = (dx + radius) * 9 + (dz + radius);
        int wordIndex = idx / 32;
        int bitIndex = idx % 32;
        if (wordIndex > 2 || data.getCount() <= 5 + wordIndex) return false;
        return (data.get(5 + wordIndex) & (1 << bitIndex)) != 0;
    }

    public net.minecraft.world.level.ChunkPos getSelfChunkPos() {
        if (blockPos == null) return new net.minecraft.world.level.ChunkPos(0, 0);
        return new net.minecraft.world.level.ChunkPos(blockPos.getX() >> 4, blockPos.getZ() >> 4);
    }
}