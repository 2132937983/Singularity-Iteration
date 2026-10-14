// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client settings of the FCS HUD (file {@code mio_icif-client.toml}). Panel positions are the
 * panel centre as a fraction of the screen, so a layout survives a window resize. The HUD layout
 * editor writes them; players do not have to edit numbers.
 */
public final class FcsClientConfig {
    private FcsClientConfig() {}

    public enum Palette {
        /** Green phosphor (thermal sight). */
        GREEN(0x7CFFB2),
        /** Amber (day sight). */
        AMBER(0xFFC24A),
        /** Cyan (digital). */
        CYAN(0x6FE6FF);
        public final int rgb;
        Palette(int rgb) { this.rgb = rgb; }
    }

    public enum Panel {
        STATUS("status", 0.13, 0.20),
        TELEMETRY("telemetry", 0.86, 0.60),
        HOLOMAP("holomap", 0.87, 0.20),
        BALLISTIC("ballistic", 0.62, 0.72),
        BLAST("blast", 0.38, 0.72);
        public final String id;
        public final double defaultX, defaultY;
        ModConfigSpec.DoubleValue x, y;
        Panel(String id, double x, double y) { this.id = id; this.defaultX = x; this.defaultY = y; }
        public double x() { return x == null ? defaultX : x.get(); }
        public double y() { return y == null ? defaultY : y.get(); }
        public void set(double nx, double ny) {
            if (x == null) return;
            x.set(Math.max(0, Math.min(1, nx)));
            y.set(Math.max(0, Math.min(1, ny)));
        }
        public void reset() { set(defaultX, defaultY); }
    }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue HUD_ENABLED;
    public static final ModConfigSpec.EnumValue<Palette> PALETTE;
    public static final ModConfigSpec.BooleanValue ESP_OUTLINES;
    public static final ModConfigSpec.DoubleValue HOLOMAP_SCALE;
    public static final ModConfigSpec.BooleanValue SHOW_COMPASS;
    public static final ModConfigSpec.BooleanValue FLIGHT_CAMERA_ROLL;
    public static final ModConfigSpec.BooleanValue FLIGHT_FOV;
    /** 0.1.8.0: the gauntlet covers the hand in first person (the arm you see) and in third person. */
    public static final ModConfigSpec.BooleanValue HAND_ARMOR_FIRST_PERSON;
    public static final ModConfigSpec.BooleanValue HAND_ARMOR_THIRD_PERSON;
    /** 0.1.9.0: frame core attachments (solar wings, graviton ring, boosters) on worn pieces. */
    public static final ModConfigSpec.BooleanValue CORE_ATTACHMENTS;
    /** 0.1.9.1: quantum suit photon effects, sensor active scan, Wingman effects. */
    public static final ModConfigSpec.BooleanValue PHOTON_FX;
    public static final ModConfigSpec.BooleanValue SCAN_FX;
    public static final ModConfigSpec.BooleanValue WINGMAN_FX;
    /** 0.1.8.3: flight visual effects (vapour cone, wake trails, boom rings) and their strength. */
    public static final ModConfigSpec.BooleanValue VAPOR_EFFECTS;
    public static final ModConfigSpec.DoubleValue VAPOR_INTENSITY;
    /** 0.1.8.3: weapon feedback. */
    public static final ModConfigSpec.BooleanValue MUZZLE_FLASH;
    public static final ModConfigSpec.BooleanValue REDUCED_FLASHES;
    public static final ModConfigSpec.DoubleValue RECOIL_CAMERA;
    public static final ModConfigSpec.BooleanValue BEAM_TRACERS;
    /** 0.1.8.3: jetpack sound. */
    public static final ModConfigSpec.DoubleValue JETPACK_VOLUME;
    public static final ModConfigSpec.BooleanValue JETPACK_CUES;
    /** 0.1.8.5: nano suit HUD and effects. */
    public static final ModConfigSpec.BooleanValue NANO_HUD;
    public static final ModConfigSpec.BooleanValue NANO_REFRACTION;
    public static final ModConfigSpec.DoubleValue NANO_FX_INTENSITY;
    public static final ModConfigSpec.BooleanValue NANO_HF_JITTER;
    /** 0.1.10.0: quantum core HUD, visor boot sequence, action effect strength, decoherence glitch. */
    public static final ModConfigSpec.BooleanValue QUANTUM_HUD;
    public static final ModConfigSpec.BooleanValue VISOR_BOOT;
    public static final ModConfigSpec.DoubleValue QUANTUM_FX_INTENSITY;
    public static final ModConfigSpec.BooleanValue DECOHERENCE_GLITCH;
    /** 0.1.10.2: squad panel, order markers, holo table cells, propulsion effects. */
    public static final ModConfigSpec.BooleanValue SQUAD_HUD;
    public static final ModConfigSpec.BooleanValue ORDER_MARKERS;
    public static final ModConfigSpec.IntValue HOLO_TABLE_CELLS;
    public static final ModConfigSpec.BooleanValue THRUSTER_FX;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("fcs_hud");
        HUD_ENABLED = b.comment("Master switch of the FCS HUD (the HUD key toggles it).").define("hud_enabled", true);
        PALETTE = b.comment("Main HUD colour: GREEN, AMBER or CYAN. Warnings stay yellow and red.").defineEnum("palette", Palette.GREEN);
        ESP_OUTLINES = b.comment("Entity ESP draws a see-through outline around each tagged creature.").define("esp_outlines", true);
        HOLOMAP_SCALE = b.comment("Size of the tactical holomap.").defineInRange("holomap_scale", 1.0, 0.6, 1.8);
        SHOW_COMPASS = b.comment("Show the heading tape (compass) at the top of the HUD.").define("show_compass", true);
        b.pop();
        b.push("flight");
        FLIGHT_CAMERA_ROLL = b.comment("The camera banks in turns in Special Maneuver Mode.").define("camera_roll", true);
        FLIGHT_FOV = b.comment("The field of view widens with speed in Special Maneuver Mode.").define("speed_fov", true);
        VAPOR_EFFECTS = b.comment("Vapour cone, wake trails, sonic boom rings and condensation bursts in Special Maneuver Mode.")
            .define("vapor_effects", true);
        VAPOR_INTENSITY = b.comment("Opacity of the flight vapour effects (1.0 = default).").defineInRange("vapor_intensity", 1.0, 0.25, 1.5);
        THRUSTER_FX = b.comment("Heat haze over the thruster flames (suit and Wingmen), shock diamonds at full burn and on boosts,",
                "and the dust ring when you land from Special Maneuver flight.").define("thruster_fx", true);
        b.pop();
        b.push("weapons");
        MUZZLE_FLASH = b.comment("A short glow at the muzzle when an energy weapon fires.").define("muzzle_flash", true);
        REDUCED_FLASHES = b.comment("Photosensitivity: dims every weapon flash and beam flare to about a third.").define("reduced_flashes", false);
        RECOIL_CAMERA = b.comment("Camera kick when you fire a gun (0 = off, 1 = default, 2 = strong). Visual only, the aim does not move.")
            .defineInRange("recoil_camera", 1.0, 0.0, 2.0);
        BEAM_TRACERS = b.comment("Energy bolts are drawn from the muzzle of the gun, so your own shots are visible in first person.")
            .define("beam_tracers", true);
        b.pop();
        b.push("sound");
        JETPACK_VOLUME = b.comment("Volume of the jetpack and flight-suit thrusters (1.0 = default).").defineInRange("jetpack_volume", 1.0, 0.0, 1.5);
        JETPACK_CUES = b.comment("Ignition and shutdown sounds of the thrusters.").define("jetpack_cues", true);
        b.pop();
        b.push("suit_visuals");
        HAND_ARMOR_FIRST_PERSON = b.comment("Nano and quantum chestplates show their arm armor and hand cover on your first-person arm.")
            .define("first_person_hand_armor", true);
        HAND_ARMOR_THIRD_PERSON = b.comment("Nano and quantum chestplates cover the hands of the worn model (third person, other players).")
            .define("third_person_hand_armor", true);
        CORE_ATTACHMENTS = b.comment("Frame cores show their exterior (solar wings, graviton ring, propulsion boosters) on worn helmets and",
                "chestplates: players, Wingmen, armour stands and holo displays. Each piece also has its own switch.")
            .define("core_attachments", true);
        PHOTON_FX = b.comment("Quantum suit photon effects: the geodesic photon barrier of the deflector and the photon afterglow",
                "(spatial distortion wake and afterimages) of fast Special Maneuver flight.")
            .define("photon_fx", true);
        SCAN_FX = b.comment("Active scan: a scan sphere expands from you every 3 s while an entity sensor unit runs.").define("scan_fx", true);
        WINGMAN_FX = b.comment("Wingman effects: lock-on scan laser and hologram marker, muzzle flashes, thruster boosts, attitude jets, slash arcs.")
            .define("wingman_fx", true);
        b.pop();
        b.push("nano_suit");
        NANO_HUD = b.comment("Nano core HUD around the crosshair (reserve ring, heat meter, burst keys).").define("nano_hud", true);
        NANO_REFRACTION = b.comment("Refraction shader of the optical camouflage and the dash and EMP shock waves.",
                "Turn it off if a shader pack shows black or white shapes; a hologram grid is drawn instead.")
            .define("nano_refraction", true);
        NANO_FX_INTENSITY = b.comment("Strength of the nano burst effects (afterimages, EMP rings, discharge arcs). 1.0 = default.")
            .defineInRange("nano_fx_intensity", 1.0, 0.25, 1.5);
        NANO_HF_JITTER = b.comment("The nanosaber blade trembles in first person while HF sync runs.").define("nano_hf_jitter", true);
        b.pop();
        b.push("quantum_suit");
        QUANTUM_HUD = b.comment("Quantum core HUD around the crosshair (coherence ring, action keys, action timers).").define("quantum_hud", true);
        VISOR_BOOT = b.comment("The FCS visor runs its boot sequence when the HUD comes on (scan line, panels unfold, aim calibration).")
            .define("visor_boot", true);
        QUANTUM_FX_INTENSITY = b.comment("Strength of the quantum action effects (blink, phase shift, force field, time dilation, volley). 1.0 = default.")
            .defineInRange("quantum_fx_intensity", 1.0, 0.25, 1.5);
        DECOHERENCE_GLITCH = b.comment("The HUD glitches (colour split, tearing) while the quantum core decoheres. Off: a calm dim warning.")
            .define("decoherence_glitch", true);
        SQUAD_HUD = b.comment("Squad panel and the edge markers of off-screen Wingmen while the quantum command link works.").define("squad_hud", true);
        ORDER_MARKERS = b.comment("World markers of your Wingmen's orders: hold points, suppression rings, focus brackets.").define("order_markers", true);
        HOLO_TABLE_CELLS = b.comment("Terrain cells per side of the 3D holo table (more = finer, slower).")
            .defineInRange("holo_table_cells", 49, 25, 65);
        b.pop();
        b.push("layout");
        for (Panel panel : Panel.values()) {
            panel.x = b.defineInRange(panel.id + "_x", panel.defaultX, 0.0, 1.0);
            panel.y = b.defineInRange(panel.id + "_y", panel.defaultY, 0.0, 1.0);
        }
        b.pop();
        SPEC = b.build();
    }

    public static boolean loaded() { return SPEC.isLoaded(); }
    public static boolean hudEnabled() { return !loaded() || HUD_ENABLED.get(); }
    public static int primary() { return loaded() ? PALETTE.get().rgb : Palette.GREEN.rgb; }
    public static boolean outlines() { return !loaded() || ESP_OUTLINES.get(); }
    public static float holomapScale() { return loaded() ? HOLOMAP_SCALE.get().floatValue() : 1.0F; }
    public static boolean showCompass() { return !loaded() || SHOW_COMPASS.get(); }
    public static void setShowCompass(boolean on) { if (loaded()) { SHOW_COMPASS.set(on); SPEC.save(); } }
    public static boolean cameraRoll() { return !loaded() || FLIGHT_CAMERA_ROLL.get(); }
    public static boolean speedFov() { return !loaded() || FLIGHT_FOV.get(); }
    public static boolean handArmorFirstPerson() { return !loaded() || HAND_ARMOR_FIRST_PERSON.get(); }
    public static boolean handArmorThirdPerson() { return !loaded() || HAND_ARMOR_THIRD_PERSON.get(); }
    public static void setHandArmor(boolean firstPerson, boolean thirdPerson) {
        if (!loaded()) return;
        HAND_ARMOR_FIRST_PERSON.set(firstPerson);
        HAND_ARMOR_THIRD_PERSON.set(thirdPerson);
        SPEC.save();
    }

    public static boolean coreAttachments() { return !loaded() || CORE_ATTACHMENTS.get(); }
    public static boolean photonFx() { return !loaded() || PHOTON_FX.get(); }
    public static boolean scanFx() { return !loaded() || SCAN_FX.get(); }
    public static boolean wingmanFx() { return !loaded() || WINGMAN_FX.get(); }
    public static void setCoreAttachments(boolean on) {
        if (!loaded()) return;
        CORE_ATTACHMENTS.set(on);
        SPEC.save();
    }

    public static boolean vaporEffects() { return !loaded() || VAPOR_EFFECTS.get(); }
    public static float vaporIntensity() { return loaded() ? VAPOR_INTENSITY.get().floatValue() : 1.0F; }
    public static boolean muzzleFlash() { return !loaded() || MUZZLE_FLASH.get(); }
    /** Scale of weapon flashes and flares: 1, or 0.35 with reduced flashes. */
    public static float flashScale() { return loaded() && REDUCED_FLASHES.get() ? 0.35F : 1.0F; }
    public static float recoilCamera() { return loaded() ? RECOIL_CAMERA.get().floatValue() : 1.0F; }
    public static boolean beamTracers() { return !loaded() || BEAM_TRACERS.get(); }
    public static float jetpackVolume() { return loaded() ? JETPACK_VOLUME.get().floatValue() : 1.0F; }
    public static boolean jetpackCues() { return !loaded() || JETPACK_CUES.get(); }

    public static boolean nanoHud() { return !loaded() || NANO_HUD.get(); }
    public static boolean nanoRefraction() { return !loaded() || NANO_REFRACTION.get(); }
    public static float nanoFx() { return loaded() ? NANO_FX_INTENSITY.get().floatValue() : 1.0F; }
    public static boolean nanoHfJitter() { return !loaded() || NANO_HF_JITTER.get(); }
    public static boolean quantumHud() { return !loaded() || QUANTUM_HUD.get(); }
    public static boolean visorBoot() { return !loaded() || VISOR_BOOT.get(); }
    public static float quantumFx() { return loaded() ? QUANTUM_FX_INTENSITY.get().floatValue() : 1.0F; }
    public static boolean decoherenceGlitch() { return !loaded() || DECOHERENCE_GLITCH.get(); }
    public static boolean squadHud() { return !loaded() || SQUAD_HUD.get(); }
    public static boolean orderMarkers() { return !loaded() || ORDER_MARKERS.get(); }
    /** Odd number of holo table cells per side (25..65). */
    public static int holoTableCells() { return loaded() ? HOLO_TABLE_CELLS.get() | 1 : 49; }
    public static boolean thrusterFx() { return !loaded() || THRUSTER_FX.get(); }

    public static void save() {
        if (loaded()) SPEC.save();
    }
}
