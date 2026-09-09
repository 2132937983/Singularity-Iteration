package com.singularity_iteration.mio_icif;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings({"null", "deprecation"}) public class mio_icif_sounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, Singularity_Iteration.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_DEMOLISH = SOUND_EVENTS.register("machine.demolish",
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "machine.demolish")));

    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_WORK = SOUND_EVENTS.register("machine.work",
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "machine.work")));

    public static final DeferredHolder<SoundEvent, SoundEvent> CABLE_BREAK = SOUND_EVENTS.register("cable.break",
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "cable.break")));

    private static SoundType MACHINE_SOUND_TYPE;

    public static SoundType getMachineSoundType() {
        if (MACHINE_SOUND_TYPE == null) {
            MACHINE_SOUND_TYPE = new SoundType(1.0F, 1.0F,
                SoundType.METAL.getBreakSound(),
                SoundType.METAL.getStepSound(),
                SoundType.METAL.getPlaceSound(),
                SoundType.METAL.getHitSound(),
                SoundType.METAL.getFallSound());
        }
        return MACHINE_SOUND_TYPE;
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}

