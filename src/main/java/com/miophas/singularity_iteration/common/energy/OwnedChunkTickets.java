// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.energy;

import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_chunk_loader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Paid tickets belong to a dimension and block position, never the global /forceload set. */
public final class OwnedChunkTickets {
    private static final java.util.Set<MinecraftServer> STOPPING = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
    public static final TicketController CONTROLLER = new TicketController(ResourceLocation.fromNamespaceAndPath("mio_icif", "paid_chunk_loader"), (level, helper) -> {
        for (var entry : helper.getBlockTickets().entrySet()) {
            // Explicitly registered persistent owners are the only chunks loaded by this validation.
            var owner = entry.getKey();
            if (level.getBlockEntity(owner) instanceof mio_icif_chunk_loader loader)
                loader.scexValidateSavedTickets(helper, entry.getValue());
            else helper.removeAllTickets(owner);
        }
        for (var owner : helper.getEntityTickets().keySet()) helper.removeAllTickets(owner);
    });
    private OwnedChunkTickets() { }
    public static void install(IEventBus bus) {
        bus.addListener((RegisterTicketControllersEvent event) -> event.register(CONTROLLER));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> STOPPING.add(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> STOPPING.remove(event.getServer()));
    }
    public static boolean stopping(MinecraftServer server) { return STOPPING.contains(server); }
}

