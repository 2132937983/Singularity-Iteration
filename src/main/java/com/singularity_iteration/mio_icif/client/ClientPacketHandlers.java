package com.singularity_iteration.mio_icif.client;

import com.singularity_iteration.mio_icif.Menu.Tool.mio_icif_od_scanner_menu;
import com.singularity_iteration.mio_icif.api.network.INetworkClientTileEntityEventListener;
import com.singularity_iteration.mio_icif.api.network.INetworkItemEventListener;
import com.singularity_iteration.mio_icif.api.network.INetworkUpdateListener;
import com.singularity_iteration.mio_icif.network.ItemEventPacket;
import com.singularity_iteration.mio_icif.network.ODScannerResultPacket;
import com.singularity_iteration.mio_icif.network.TileEntityEventPacket;
import com.singularity_iteration.mio_icif.network.TileEntityFieldUpdatePacket;

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