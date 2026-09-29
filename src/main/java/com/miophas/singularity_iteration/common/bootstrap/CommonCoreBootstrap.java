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
        CoreDataComponents.DATA_COMPONENTS.register(modEventBus);
        CoreSounds.register(modEventBus);
        EnergyCompatibility.install(new CommonEnergyCompatibility());
        MachinePresentation.configure(BatBoxMenu::new, () -> CoreSounds.MACHINE_WORK.get(), () -> CoreSounds.CABLE_BREAK.get());
        SlotItemPredicates.configure(stack -> stack.getItem() instanceof mio_icif_reactor,
            stack -> stack.is(mio_icif_resources.RTG_PELLET.get()),
            stack -> stack.is(mio_icif_normal.STEAM_TURBIN.get()));
    }
}