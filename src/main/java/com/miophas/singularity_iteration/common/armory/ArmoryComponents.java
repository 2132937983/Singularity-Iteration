package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item data components of the Armory system. */
public final class ArmoryComponents {
    private ArmoryComponents() {}

    public static final DeferredRegister.DataComponents COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Singularity_Iteration.MOD_ID);

    /** Connector Kit binding on foreign equipment. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ArmoryLink>> LINK =
        COMPONENTS.registerComponentType("armory_link",
            b -> b.persistent(ArmoryLink.CODEC).networkSynchronized(ArmoryLink.STREAM_CODEC));

    /** Armory a remote controller is paired with. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> TARGET =
        COMPONENTS.registerComponentType("armory_target",
            b -> b.persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC));
}
