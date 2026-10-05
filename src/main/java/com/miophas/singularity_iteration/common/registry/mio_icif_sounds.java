package com.miophas.singularity_iteration.common.registry;

import com.miophas.singularity_iteration.common.Singularity_Iteration;

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

    public static final DeferredHolder<SoundEvent, SoundEvent> NUCLEAR_EXPLOSION = sound("nuclear.explosion");

    public static final DeferredHolder<SoundEvent, SoundEvent> TOWER_LOCK = sound("tower.lock");
    public static final DeferredHolder<SoundEvent, SoundEvent> TOWER_CHARGE = sound("tower.charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> TOWER_FIRE = sound("tower.fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> TOWER_HIT = sound("tower.hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_CLICK = sound("ui.click");
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_TOGGLE = sound("ui.toggle");
    public static final DeferredHolder<SoundEvent, SoundEvent> METAL_FORMER_SWITCH = sound("machine.metal_former.switch");
    public static final DeferredHolder<SoundEvent, SoundEvent> LASER_SHOT = sound("tool.laser.shot");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFLE_SHOT = sound("weapon.rifle.shot");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLASMA_SHOT = sound("weapon.plasma.shot");
    public static final DeferredHolder<SoundEvent, SoundEvent> TACHYON_SHOT = sound("weapon.tachyon.shot");
    public static final DeferredHolder<SoundEvent, SoundEvent> TACHYON_HIT = sound("weapon.tachyon.hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENERGY_HIT = sound("weapon.energy.hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> AIR_CANNON_CHARGE = sound("weapon.air_cannon.charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> AIR_CANNON_BLAST = sound("weapon.air_cannon.blast");
    public static final DeferredHolder<SoundEvent, SoundEvent> AIR_CANNON_HIT = sound("weapon.air_cannon.hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROCKET_LAUNCH = sound("weapon.rocket.launch");
    public static final DeferredHolder<SoundEvent, SoundEvent> TERMINAL_AMBIENT = sound("terminal.ambient");

    // Running-machine loops (played client-side by MachineSoundManager while lit/active)
    public static final DeferredHolder<SoundEvent, SoundEvent> LOOP_MOTOR = sound("machine.loop.motor");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOOP_PRESS = sound("machine.loop.press");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOOP_CRUSHER = sound("machine.loop.crusher");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOOP_CENTRIFUGE = sound("machine.loop.centrifuge");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOOP_SAW = sound("machine.loop.saw");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOOP_FURNACE = sound("machine.loop.furnace");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOOP_FLUID = sound("machine.loop.fluid");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOOP_ELECTRIC = sound("machine.loop.electric");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOOP_GENERATOR = sound("machine.loop.generator");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOOP_TURBINE = sound("machine.loop.turbine");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOOP_REACTOR = sound("machine.loop.reactor");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOOP_DRILL = sound("machine.loop.drill");

    // Armory suit-up sequence (original synthesized effects, tools/armory_sfx.py)
    public static final DeferredHolder<SoundEvent, SoundEvent> ARMORY_JET_LOOP = sound("armory.jet_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARMORY_LAUNCH = sound("armory.launch");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARMORY_FLYBY = sound("armory.flyby");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARMORY_LATCH = sound("armory.latch");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARMORY_LOCK = sound("armory.lock");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARMORY_HISS = sound("armory.hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARMORY_UNLOCK = sound("armory.unlock");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARMORY_PURGE = sound("armory.purge");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARMORY_COMPLETE = sound("armory.complete");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARMORY_TURBINE_LOOP = sound("armory.turbine_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARMORY_ALARM = sound("armory.alarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARMORY_HATCH = sound("armory.hatch");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARMORY_BRAKE = sound("armory.brake");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARMORY_PULSE = sound("armory.pulse");

    // 0.1.7.34 Special Maneuver Mode and FCS cues (original synthesized effects, tools/gen_sfx_034.py)
    public static final DeferredHolder<SoundEvent, SoundEvent> MANEUVER_BOOM = sound("maneuver.boom");
    public static final DeferredHolder<SoundEvent, SoundEvent> MANEUVER_WIND = sound("maneuver.wind");
    public static final DeferredHolder<SoundEvent, SoundEvent> MANEUVER_TAKEOFF = sound("maneuver.takeoff");
    public static final DeferredHolder<SoundEvent, SoundEvent> FCS_ECHO = sound("fcs.echo");
    public static final DeferredHolder<SoundEvent, SoundEvent> FCS_LOCK = sound("fcs.lock");
    public static final DeferredHolder<SoundEvent, SoundEvent> FCS_BLAST_BEEP = sound("fcs.blast_beep");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, name)));
    }

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