package com.singularity_iteration.mio_icif.Menu.Generator;

import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_base_menu;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.singularity_iteration.mio_icif.Blocks.entity.reactor.mio_icif_reactor_nuke;
import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_nuclear_material;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class NukeMenu extends mio_icif_base_menu {

    @SuppressWarnings("unused")
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final Container container;
    private final mio_icif_reactor_nuke blockEntity;

    public NukeMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public NukeMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_reactor_nuke blockEntity) {
        super(mio_icif_menus.NUKE_MENU_TYPE.get(), containerId, 9, 9);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(),
            blockEntity != null ? blockEntity.getBlockPos() : playerInventory.player.blockPosition());
        this.blockEntity = blockEntity;
        this.container = blockEntity != null ? blockEntity : new SimpleContainer(9);
        this.data = new ContainerData() {
            private final int[] data = new int[2];

            @Override
            public int get(int index) {
                return data[index];
            }

            @Override
            public void set(int index, int value) {
                data[index] = value;
            }

            @Override
            public int getCount() {
                return data.length;
            }
        };

        this.addDataSlots(data);

        int[][] tntSlotPositions = {
            {52, 8}, {26, 35}, {26, 89}, {52, 116},
            {106, 116}, {133, 89}, {133, 35}, {106, 8}
        };

        for (int i = 0; i < 8; i++) {
            final int slotIndex = i;
            this.addSlot(new Slot(container, i, tntSlotPositions[i][0], tntSlotPositions[i][1]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    if (blockEntity != null) {
                        return blockEntity.canPlaceItem(slotIndex, stack);
                    }
                    return false;
                }
            });
        }

        this.addSlot(new Slot(container, 8, 79, 62) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (blockEntity != null) {
                    return blockEntity.canPlaceItem(8, stack);
                }
                return false;
            }
        });

        addPlayerInventoryCustom(playerInventory, 8, 195);
    }

    public float getExplosionPower() {
        return data.get(0);
    }

    public int getExplosionPowerProgress() {
        int maxPower = data.get(1);
        return maxPower > 0 ? Math.min(24, data.get(0) * 24 / maxPower) : 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public void broadcastChanges() {
        if (blockEntity != null) {
            float power = blockEntity.calculateExplosionPower();
            this.data.set(0, (int) power);
            this.data.set(1, 1000);
        }
        super.broadcastChanges();
    }

    @Override
    protected boolean isBattery(ItemStack stack) {
        return false;
    }

    @Override
    protected boolean isChargeable(ItemStack stack) {
        return false;
    }

    @Override
    protected int getBatterySlotIndex() {
        return -1;
    }

    @Override
    protected int getChargeSlotIndex() {
        return -1;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemStack1 = slot.getItem();
            itemstack = itemStack1.copy();

            if (index < 9) {
                if (!this.moveItemStackTo(itemStack1, 9, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                boolean moved = false;

                if (isIC_TNT(itemStack1)) {
                    if (this.moveItemStackTo(itemStack1, 0, 8, false)) {
                        moved = true;
                    }
                } else if (isNuclearMaterial(itemStack1)) {
                    if (this.moveItemStackTo(itemStack1, 8, 9, false)) {
                        moved = true;
                    }
                }

                if (!moved) {
                    if (index >= 9 && index < 36) {
                        if (!this.moveItemStackTo(itemStack1, 36, 45, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (index >= 36 && index < 45) {
                        if (!this.moveItemStackTo(itemStack1, 9, 36, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else {
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

    private boolean isIC_TNT(ItemStack stack) {
        return stack.is(mio_icif_blocks.IC_TNT.get().asItem());
    }

    private boolean isNuclearMaterial(ItemStack stack) {
        return stack.getItem() instanceof mio_icif_nuclear_material;
    }

    private void addPlayerInventoryCustom(Inventory playerInventory, int startX, int startY) {
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, startX + j * 18, startY - 58 + i * 18));
            }
        }

        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, startX + i * 18, startY));
        }
    }
}


