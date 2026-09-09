package com.singularity_iteration.mio_icif.Menu.Tool;

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


import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("null")
public class mio_icif_od_scanner_menu extends AbstractContainerMenu {

    private Map<String, Integer> scanResults;

    public mio_icif_od_scanner_menu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        super(getMenuType(), containerId);
        this.scanResults = readScanResultsFromBuffer(extraData);
        addPlayerInventorySlots(playerInventory);
    }

    public mio_icif_od_scanner_menu(int containerId, Inventory playerInventory, Map<String, Integer> scanResults) {
        super(getMenuType(), containerId);
        this.scanResults = scanResults != null ? scanResults : Map.of();
        addPlayerInventorySlots(playerInventory);
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                    8 + col * 18, 149 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col,
                8 + col * 18, 207));
        }
    }

    private static Map<String, Integer> readScanResultsFromBuffer(FriendlyByteBuf buf) {
        if (buf == null) {
            return Map.of();
        }
        Map<String, Integer> results = new HashMap<>();
        int size = buf.readVarInt();
        for (int i = 0; i < size; i++) {
            String key = buf.readUtf();
            int value = buf.readVarInt();
            results.put(key, value);
        }
        return results;
    }

    public void writeScanResultsToBuffer(FriendlyByteBuf buf) {
        buf.writeVarInt(scanResults.size());
        scanResults.forEach((key, value) -> {
            buf.writeUtf(key);
            buf.writeVarInt(value);
        });
    }

    public Map<String, Integer> getScanResults() {
        return scanResults;
    }

    private Runnable onScanResultsUpdated;

    public void updateScanResults(Map<String, Integer> newResults) {
        this.scanResults = newResults != null ? newResults : Map.of();
        if (onScanResultsUpdated != null) {
            onScanResultsUpdated.run();
        }
    }

    public void setOnScanResultsUpdated(Runnable callback) {
        this.onScanResultsUpdated = callback;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private static MenuType<mio_icif_od_scanner_menu> getMenuType() {
        return mio_icif_tool_menus.OD_SCANNER_MENU.get();
    }

    public static class Provider implements MenuProvider {
        private final Map<String, Integer> scanResults;

        public Provider(Map<String, Integer> scanResults) {
            this.scanResults = scanResults != null ? scanResults : Map.of();
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("gui.mio_icif.od_scanner.title");
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new mio_icif_od_scanner_menu(containerId, playerInventory, scanResults);
        }

        public void writeExtraData(FriendlyByteBuf buf) {
            buf.writeVarInt(scanResults.size());
            scanResults.forEach((key, value) -> {
                buf.writeUtf(key);
                buf.writeVarInt(value);
            });
        }
    }
}

