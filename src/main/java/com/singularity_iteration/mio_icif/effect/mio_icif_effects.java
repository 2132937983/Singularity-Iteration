package com.singularity_iteration.mio_icif.effect;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("null")
public class mio_icif_effects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, Singularity_Iteration.MOD_ID);

    public static final DeferredHolder<MobEffect, RadiationEffect> RADIATION =
            MOB_EFFECTS.register("radiation", RadiationEffect::new);

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}


