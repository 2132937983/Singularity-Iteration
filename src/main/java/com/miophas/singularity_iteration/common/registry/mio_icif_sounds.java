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
    // 0.1.8.0 (tools/sfx_180.py): heavy mechanical lock when a piece docks; machine drive loops
    public static final DeferredHolder<SoundEvent, SoundEvent> ARMORY_SEAL = sound("armory.seal");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOOP_UU_FABRICATOR = sound("machine.loop.uu_fabricator");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOOP_KINETIC_DRIVE = sound("machine.loop.kinetic_drive");

    // 0.1.8.1 (tools/sfx_181.py): loops for machines that were silent (see tools/sound_audit_181.py)
    public static final DeferredHolder<SoundEvent, SoundEvent> LOOP_COMPRESSOR = sound("machine.loop.compressor");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOOP_SERVO = sound("machine.loop.servo");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOOP_DISCHARGE = sound("machine.loop.discharge");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOOP_BUBBLER = sound("machine.loop.bubbler");
    // 0.1.8.1 (tools/sfx_181.py): jetpack thrusters, played by JetpackSoundClient
    public static final DeferredHolder<SoundEvent, SoundEvent> JETPACK_ELECTRIC_LOOP = sound("jetpack.electric.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> JETPACK_ADVANCED_LOOP = sound("jetpack.advanced.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> JETPACK_IONIC_LOOP = sound("jetpack.ionic.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> JETPACK_IGNITE = sound("jetpack.ignite");
    public static final DeferredHolder<SoundEvent, SoundEvent> JETPACK_SHUTDOWN = sound("jetpack.shutdown");

    // 0.1.7.34 Special Maneuver Mode and FCS cues (original synthesized effects, tools/gen_sfx_034.py)
    public static final DeferredHolder<SoundEvent, SoundEvent> MANEUVER_BOOM = sound("maneuver.boom");
    public static final DeferredHolder<SoundEvent, SoundEvent> MANEUVER_WIND = sound("maneuver.wind");
    public static final DeferredHolder<SoundEvent, SoundEvent> MANEUVER_TAKEOFF = sound("maneuver.takeoff");
    public static final DeferredHolder<SoundEvent, SoundEvent> FCS_ECHO = sound("fcs.echo");
    public static final DeferredHolder<SoundEvent, SoundEvent> FCS_LOCK = sound("fcs.lock");
    public static final DeferredHolder<SoundEvent, SoundEvent> FCS_BLAST_BEEP = sound("fcs.blast_beep");

    // 0.1.8.5 nano suit units (original synthesized effects, tools/sfx_185.py)
    public static final DeferredHolder<SoundEvent, SoundEvent> NANO_DASH = sound("nano.dash");
    public static final DeferredHolder<SoundEvent, SoundEvent> NANO_EMP = sound("nano.emp");
    public static final DeferredHolder<SoundEvent, SoundEvent> NANO_CAMO_ON = sound("nano.camo_on");
    public static final DeferredHolder<SoundEvent, SoundEvent> NANO_CAMO_OFF = sound("nano.camo_off");
    public static final DeferredHolder<SoundEvent, SoundEvent> NANO_OVERHEAT = sound("nano.overheat");
    public static final DeferredHolder<SoundEvent, SoundEvent> NANO_DENIED = sound("nano.denied");
    public static final DeferredHolder<SoundEvent, SoundEvent> NANO_HF_SYNC = sound("nano.hf_sync");
    // 0.1.8.6 nano suit phase 2 (tools/sfx_186.py)
    public static final DeferredHolder<SoundEvent, SoundEvent> NANO_MESH_ON = sound("nano.mesh_on");
    public static final DeferredHolder<SoundEvent, SoundEvent> NANO_MESH_HIT = sound("nano.mesh_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> NANO_RECON = sound("nano.recon");
    public static final DeferredHolder<SoundEvent, SoundEvent> NANO_REBOUND = sound("nano.rebound");
    public static final DeferredHolder<SoundEvent, SoundEvent> NANO_REPAIR = sound("nano.repair");
    // 0.1.9.0 frame cores (tools/sfx_190.py)
    public static final DeferredHolder<SoundEvent, SoundEvent> CORE_DISCHARGE = sound("core.discharge");
    public static final DeferredHolder<SoundEvent, SoundEvent> CORE_INSTALL = sound("core.install");
    /** 0.1.9.1: Wingman thruster boost, Wingman target lock, deflector photon barrier, sensor active scan. */
    public static final DeferredHolder<SoundEvent, SoundEvent> WINGMAN_BOOST = sound("wingman.boost");
    public static final DeferredHolder<SoundEvent, SoundEvent> WINGMAN_LOCK = sound("wingman.lock");
    public static final DeferredHolder<SoundEvent, SoundEvent> PHOTON_BARRIER = sound("suit.photon_barrier");
    /** 0.1.10.0 quantum actions, decoherence and the visor boot (tools/sfx_1100.py). */
    public static final DeferredHolder<SoundEvent, SoundEvent> QUANTUM_BLINK = sound("suit.quantum_blink");
    public static final DeferredHolder<SoundEvent, SoundEvent> PHASE_ON = sound("suit.phase_on");
    public static final DeferredHolder<SoundEvent, SoundEvent> PHASE_OFF = sound("suit.phase_off");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIELD_ON = sound("suit.field_on");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIELD_OFF = sound("suit.field_off");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIELD_CATCH = sound("suit.field_catch");
    public static final DeferredHolder<SoundEvent, SoundEvent> DILATION_ON = sound("suit.dilation_on");
    public static final DeferredHolder<SoundEvent, SoundEvent> DILATION_OFF = sound("suit.dilation_off");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOLLEY_ORDER = sound("suit.volley_order");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOLLEY_IMPACT = sound("suit.volley_impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> DECOHERENCE = sound("suit.decoherence");
    public static final DeferredHolder<SoundEvent, SoundEvent> VISOR_BOOT = sound("suit.visor_boot");
    /** 0.1.10.1 entanglement links, chain combos, the resonance burst (tools/sfx_1101.py). */
    public static final DeferredHolder<SoundEvent, SoundEvent> LINK_ON = sound("suit.link_on");
    public static final DeferredHolder<SoundEvent, SoundEvent> LINK_OFF = sound("suit.link_off");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMBO = sound("suit.combo");
    public static final DeferredHolder<SoundEvent, SoundEvent> RESONANCE_BURST = sound("suit.resonance_burst");
    /** 0.1.10.2 quantum command, holo table and propulsion (tools/sfx_1102.py). */
    public static final DeferredHolder<SoundEvent, SoundEvent> WHEEL_OPEN = sound("command.wheel_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> WHEEL_SELECT = sound("command.wheel_select");
    public static final DeferredHolder<SoundEvent, SoundEvent> ORDER_ACK = sound("command.order_ack");
    public static final DeferredHolder<SoundEvent, SoundEvent> QUANTUM_RECALL = sound("command.quantum_recall");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOLO_OPEN = sound("command.holo_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOLO_CLOSE = sound("command.holo_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> THRUSTER_LAND = sound("suit.thruster_land");

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