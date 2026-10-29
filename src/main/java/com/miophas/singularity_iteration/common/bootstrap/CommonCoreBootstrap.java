package com.miophas.singularity_iteration.common.bootstrap;

import com.miophas.singularity_iteration.common.integration.CommonEnergyCompatibility;
import com.miophas.singularity_iteration.common.item.normal.mio_icif_normal;
import com.miophas.singularity_iteration.common.item.reactor.mio_icif_reactor;
import com.miophas.singularity_iteration.common.item.resource.mio_icif_resources;
import com.miophas.singularity_iteration.common.menu.storage.BatBoxMenu;
import com.miophas.singularity_iteration.core.platform.neoforge.CoreDataComponents;
import com.miophas.singularity_iteration.core.platform.neoforge.CoreSounds;
import com.miophas.singularity_iteration.core.platform.neoforge.energy.EnergyCompatibility;
import com.miophas.singularity_iteration.core.prefab.blockentity.MachinePresentation;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotItemPredicates;
import net.neoforged.bus.api.IEventBus;

/** The composition root: core never loads or discovers this class itself. */
public final class CommonCoreBootstrap {
    private CommonCoreBootstrap() {}

    public static void install(IEventBus modEventBus) {
        CommonApiServices.install();
        com.miophas.singularity_iteration.common.energy.BuiltinEnergyProfiles.install();
        com.miophas.singularity_iteration.common.energy.BuiltinIndependentSubstitutions.install();
        com.miophas.singularity_iteration.common.multiblock.BuiltinMultiblockControllers.install();
        // 0.1.10.19: data for the JEI categories "Reactor Layouts" / "Multiblock Structures" and the guide
        com.miophas.singularity_iteration.common.reactor.BuiltinReactorLayouts.install();
        com.miophas.singularity_iteration.common.multiblock.BuiltinStructureBlueprints.install();
        // 0.1.10.37: data for the JEI category "Nuclear Fuel Breeding & Decay", from the reactor's own tables
        for (var change : com.miophas.singularity_iteration.common.reactor.ReactorInventory.transformations())
            com.miophas.singularity_iteration.core.api.reactor.ReactorTransformationRegistry.register(change);
        // 0.1.10.26 (X3): the exchange API for pack makers (core.api.exchange)
        com.miophas.singularity_iteration.core.api.exchange.ExchangeApi.install(new com.miophas.singularity_iteration.common.exchange.ExchangeApiImpl());
        CoreDataComponents.DATA_COMPONENTS.register(modEventBus);
        CoreSounds.register(modEventBus);
        EnergyCompatibility.install(new CommonEnergyCompatibility());
        // Host machines get their running sound from the client-side loop manager
        // (common.client.sound.MachineSoundManager, driven by the ACTIVE block state), so the
        // server no longer broadcasts a work sound every 40 ticks per machine. Addon machines can
        // still return their own sound from getWorkSound().
        MachinePresentation.configure(BatBoxMenu::new, () -> null, () -> CoreSounds.CABLE_BREAK.get());
        SlotItemPredicates.configure(stack -> stack.getItem() instanceof mio_icif_reactor,
            stack -> stack.is(mio_icif_resources.RTG_PELLET.get()),
            stack -> stack.is(mio_icif_normal.STEAM_TURBIN.get()));
    }
}