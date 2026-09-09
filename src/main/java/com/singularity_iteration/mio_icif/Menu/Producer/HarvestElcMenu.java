package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_harvest_elc;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_base_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class HarvestElcMenu extends mio_icif_base_menu {

    public static final int SLOT_STORAGE_START = 0;
    public static final int SLOT_STORAGE_COUNT = 15;
    public static final int SLOT_BATTERY = 15;
    public static final int SLOT_ANALYZER = 16;
    public static final int SLOT_UPGRADE = 17;
    public static final int TOTAL_SLOTS = 18;

    private final ContainerData data;
    private final IItemHandler itemHandler;

    public HarvestElcMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, new SimpleContainerData(2));
    }

    public HarvestElcMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_harvest_elc blockEntity, ContainerData data) {
        super(mio_icif_menus.HARVEST_ELC_MENU_TYPE.get(), containerId, TOTAL_SLOTS, TOTAL_SLOTS);
        this.itemHandler = blockEntity != null ? blockEntity.getItemHandler() : new ItemStackHandler(TOTAL_SLOTS);
        this.data = data;
        this.addDataSlots(this.data);

        // 存存储?(0-14) - 15个槽位?行�?列，第一个在(44, 22)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 5; col++) {
                int slotIndex = row * 5 + col;
                this.addSlot(new SlotItemHandler(itemHandler, slotIndex, 44 + col * 18, 22 + row * 18));
            }
        }

        // 电池�?(15) - 位置 (152, 58)
        this.addSlot(new SlotItemHandler(itemHandler, SLOT_BATTERY, 152, 58) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Normal.mio_icif_bat
                    || stack.getItem() == net.minecraft.world.item.Items.REDSTONE;
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                if (stack.getItem() == net.minecraft.world.item.Items.REDSTONE) return 64;
                return 1;
            }
        });

        // 作物分析仪槽 (16) - 位置 (15, 40)
        this.addSlot(new SlotItemHandler(itemHandler, SLOT_ANALYZER, 15, 40));

        addUpgradeSlot(itemHandler, SLOT_UPGRADE, 80, 80);

        // 玩家物品栈?
        addPlayerInventory(playerInventory, 110, 168);
    }

    @Override
    protected boolean isBattery(ItemStack stack) {
        return MioIcifAPI.instance().getItemAPI().isBattery(stack)
            || stack.getItem() == net.minecraft.world.item.Items.REDSTONE;
    }

    @Override
    protected int getBatterySlotIndex() {
        return SLOT_BATTERY;
    }

    @Override
    protected int getUpgradeSlotStart() {
        return SLOT_UPGRADE;
    }

    @Override
    protected int getUpgradeSlotCount() {
        return 1;
    }

    public int getEnergy() {
        return this.data.get(0);
    }

    public int getMaxEnergy() {
        return this.data.get(1);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
    }
}