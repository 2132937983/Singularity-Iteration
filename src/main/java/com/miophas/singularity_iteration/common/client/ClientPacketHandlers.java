package com.miophas.singularity_iteration.common.client;

import com.miophas.singularity_iteration.common.menu.tool.mio_icif_od_scanner_menu;
import com.miophas.singularity_iteration.core.api.network.INetworkClientTileEntityEventListener;
import com.miophas.singularity_iteration.core.api.network.INetworkItemEventListener;
import com.miophas.singularity_iteration.core.api.network.INetworkUpdateListener;
import com.miophas.singularity_iteration.common.network.ItemEventPacket;
import com.miophas.singularity_iteration.common.network.ODScannerResultPacket;
import com.miophas.singularity_iteration.common.network.TileEntityEventPacket;
import com.miophas.singularity_iteration.common.network.TileEntityFieldUpdatePacket;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public class ClientPacketHandlers {

    public static void handleNuclearExplosionAnimation(double centerX, double centerY, double centerZ, int explosionRadius) {
        NuclearExplosionAnimationHandler.startAnimation(centerX, centerY, centerZ, explosionRadius);
    }

    public static void handleODScannerResult(ODScannerResultPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.containerMenu instanceof mio_icif_od_scanner_menu menu) {
            menu.updateScanResults(packet.scanResults());
        }
    }

    public static void handleItemEvent(ItemEventPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        // The slot index comes from the wire; reject malformed/stale packets before
        // Inventory#getItem can throw for a negative or out-of-range index.
        if (packet.slotIndex() < 0 || packet.slotIndex() >= mc.player.getInventory().getContainerSize()) return;
        ItemStack stack = mc.player.getInventory().getItem(packet.slotIndex());
        if (stack.isEmpty()) return;
        if (stack.getItem() instanceof INetworkItemEventListener listener) {
            listener.onNetworkEvent(stack, mc.player, packet.eventId());
        }
    }

    public static void handleTileEntityEvent(TileEntityEventPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        var be = mc.level.getBlockEntity(packet.pos());
        if (be instanceof INetworkClientTileEntityEventListener listener) {
            listener.onNetworkEvent(mc.player, packet.eventId());
        }
    }

    public static void handleTileEntityFieldUpdate(TileEntityFieldUpdatePacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        var be = mc.level.getBlockEntity(packet.pos());
        if (be instanceof INetworkUpdateListener listener) {
            listener.onNetworkUpdate(packet.fieldName(), packet.fieldValue());
        }
    }
}
