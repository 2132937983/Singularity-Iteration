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
        BLAST("blast", 0.50, 0.26),
        FLIGHT("flight", 0.36, 0.52);
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
    public static final ModConfigSpec.BooleanValue FLIGHT_CAMERA_ROLL;
    public static final ModConfigSpec.BooleanValue FLIGHT_FOV;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("fcs_hud");
        HUD_ENABLED = b.comment("Master switch of the FCS HUD (the HUD key toggles it).").define("hud_enabled", true);
        PALETTE = b.comment("Main HUD colour: GREEN, AMBER or CYAN. Warnings stay yellow and red.").defineEnum("palette", Palette.GREEN);
        ESP_OUTLINES = b.comment("Entity ESP draws a see-through outline around each tagged creature.").define("esp_outlines", true);
        HOLOMAP_SCALE = b.comment("Size of the tactical holomap.").defineInRange("holomap_scale", 1.0, 0.6, 1.8);
        b.pop();
        b.push("flight");
        FLIGHT_CAMERA_ROLL = b.comment("The camera banks in turns during Viltrum flight.").define("camera_roll", true);
        FLIGHT_FOV = b.comment("The field of view widens with speed during Viltrum flight.").define("speed_fov", true);
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
    public static boolean cameraRoll() { return !loaded() || FLIGHT_CAMERA_ROLL.get(); }
    public static boolean speedFov() { return !loaded() || FLIGHT_FOV.get(); }

    public static void save() {
        if (loaded()) SPEC.save();
    }
}
