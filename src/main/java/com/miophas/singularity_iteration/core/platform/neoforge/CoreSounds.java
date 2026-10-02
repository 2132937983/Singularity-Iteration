package com.miophas.singularity_iteration.core.platform.neoforge;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CoreSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
        DeferredRegister.create(Registries.SOUND_EVENT, "mio_icif");

    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_WORK = SOUND_EVENTS.register("machine.work",
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("mio_icif", "machine.work")));

    public static final DeferredHolder<SoundEvent, SoundEvent> CABLE_BREAK = SOUND_EVENTS.register("cable.break",
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("mio_icif", "cable.break")));

    /** One-shot "operation complete" cue (pneumatic release + two-tone chime), rate-limited per machine. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_COMPLETE = SOUND_EVENTS.register("machine.complete",
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("mio_icif", "machine.complete")));

    private CoreSounds() {}

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}