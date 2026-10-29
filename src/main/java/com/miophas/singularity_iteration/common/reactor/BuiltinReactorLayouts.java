// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.reactor;

import com.miophas.singularity_iteration.core.api.reactor.ReactorLayout;
import com.miophas.singularity_iteration.core.api.reactor.ReactorLayoutRegistry;
import net.minecraft.resources.ResourceLocation;

/**
 * 0.1.10.19: the reactor layouts of Singularity Iteration in the JEI category "Reactor Layouts" (IC2 Classic
 * shows its reactor designs in JEI the same way). Each layout was run through the reactor model
 * ({@link ReactorSimulation#fullRun}, see {@code verify-si1119}); the comment gives the result with the
 * default explosion settings. Three are the tutorials of the guide page "Reactor Simulator"; the last two
 * show how a design fails.
 */
public final class BuiltinReactorLayouts {
    private BuiltinReactorLayouts() {}

    private static final String IV = "iridium_vent";

    public static void install() {
        // 5 EU/t, 0 chambers: steady, blast radius 12
        add("starter", "_,vent,_/vent,uranium_simple,vent/_,vent,_");
        // 20 EU/t, 0 chambers: each vent at its limit of 6 HU/s; blast radius 16
        add("dual_rod", "_,vent,_/vent,uranium_dual,vent/_,vent,_");
        // 60 EU/t, 0 chambers: one quad rod, four iridium vents with 24 HU/s each; blast radius 24
        add("quad_compact", "_,IV,_/IV,uranium_quad,IV/_,IV,_");
        // 120 EU/t, 6 chambers: six dual rods, basic vents only (no vent touches two rods); blast radius 56
        add("dual_rods_six", "_,_,vent,_,_,_,_,vent,_/_,vent,uranium_dual,vent,vent,_,vent,uranium_dual,vent/"
            + "_,vent,vent,vent,uranium_dual,vent,vent,vent,_/vent,uranium_dual,vent,vent,vent,vent,uranium_dual,vent,_/"
            + "_,vent,vent,uranium_dual,vent,_,vent,_,_/_,_,_,vent,_,_,_,_,_");
        // 300 EU/t, 4 chambers: guide tutorial 3; blast radius 88
        add("high_output", "IV,uranium_quad,IV,_,IV,uranium_quad,IV/_,IV,_,IV,_,IV,_/IV,IV,IV,uranium_quad,IV,IV,IV/"
            + "IV,uranium_quad,IV,IV,IV,uranium_quad,IV/_,IV,_,_,_,IV,_/_,_,_,_,_,_,_");
        // the same with containment plating in the free slots: blast power 44 -> 1.2 (radius 3), hull 18,500
        add("high_output_plated", "IV,uranium_quad,IV,P,IV,uranium_quad,IV/P,IV,P,IV,P,IV,P/IV,IV,IV,uranium_quad,IV,IV,IV/"
            + "IV,uranium_quad,IV,IV,IV,uranium_quad,IV/P,IV,P,P,P,IV,P/P,P,P,P,P,P,P");
        // 660 EU/t, 6 chambers: eleven quad rods, each vent touches one rod; blast at the power limit (radius 90)
        add("max_output", "IV,uranium_quad,IV,IV,_,IV,uranium_quad,IV,IV/IV,IV,IV,uranium_quad,IV,IV,IV,IV,uranium_quad/"
            + "uranium_quad,IV,IV,IV,IV,uranium_quad,IV,IV,IV/IV,IV,uranium_quad,IV,IV,IV,IV,uranium_quad,IV/"
            + "_,IV,IV,IV,uranium_quad,IV,IV,IV,_/IV,uranium_quad,IV,_,IV,IV,uranium_quad,IV,_");
        // failure: 96 HU/s into three basic vents; they melt after 39 s, meltdown after 143 s
        add("failure_vents", "vent,uranium_quad,vent/_,vent,_/_,_,_");
        // failure that looks safe for 100 s: the reflector adds pulses (160 HU/s, 108 vented); the vents
        // store the rest, melt after 199 s, meltdown after 280 s
        add("failure_reflector", "_,IV,_/iridium_reflector,uranium_quad,IV/_,IV,_");
        // 0.1.10.37: Steam Reactor layouts (run as steam reactors). 960 mB/s: each Advanced Steam Vent gets 6 HU/s and settles
        // at 350 HU (efficiency 0.25: 6 units of 40 mB); about 24 EU/t of Steam Turbine Generators
        steam("steam_starter", "_,steam_vent_advanced,_/steam_vent_advanced,uranium_dual,steam_vent_advanced/_,steam_vent_advanced,_");
        // 3,840 mB/s: 96 HU/s into four Overclocked Steam Vents, 24 each, settling at 700 HU; about 96 EU/t of turbines
        steam("steam_quad", "_,steam_vent_overclocked,_/steam_vent_overclocked,uranium_quad,steam_vent_overclocked/_,steam_vent_overclocked,_");
    }

    private static void steam(String name, String preset) {
        ReactorLayoutRegistry.register(ReactorLayout.steam(ResourceLocation.fromNamespaceAndPath("mio_icif", name), preset, null,
            "reactor_layout.mio_icif." + name + ".note"));
    }

    private static void add(String name, String preset) {
        ReactorLayoutRegistry.register(ReactorLayout.of(ResourceLocation.fromNamespaceAndPath("mio_icif", name), expand(preset),
            null, "reactor_layout.mio_icif." + name + ".note"));
    }

    /** Short cells of this class: IV = iridium vent, P = containment plating. */
    static String expand(String preset) {
        StringBuilder out = new StringBuilder();
        for (String row : preset.split("/")) {
            if (!out.isEmpty()) out.append('/');
            String[] cells = row.split(",", -1);
            for (int i = 0; i < cells.length; i++) {
                if (i > 0) out.append(',');
                out.append(switch (cells[i]) {
                    case "IV" -> IV;
                    case "P" -> "explosive_plate";
                    default -> cells[i];
                });
            }
        }
        return out.toString();
    }
}
