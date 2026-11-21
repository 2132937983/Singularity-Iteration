package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.core.prefab.inventory.SlotType;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.151: slot silhouettes, as vanilla's empty armour slots show what goes there: an outline of the item in vanilla's dark
 * grey, GUI sprites under textures/gui/sprites/slot made by tools/slot_silhouettes_111151.py from the mod's own item textures.
 * {@link #sprite} names the silhouette of an item (or null: the screen falls back to the faint item), {@link #forType} the item a
 * typed machine slot takes. No client classes: the data is checked on the test server too.
 */
public final class SlotSilhouettes {
    private SlotSilhouettes() {}

    /** Every silhouette (a sprite mio_icif:slot/<name>). */
    public static final List<String> NAMES = List.of("battery", "cell", "vent", "coil", "rotor", "turbine", "heat_conductor", "drill",
        "scanner", "memory", "rtg_pellet", "fuel_rod", "tin_can", "cut_blade", "coolant_cell", "deuterium_rod", "tritium_rod",
        "neon_pellet");

    /** Item id -> silhouette. */
    private static final Map<String, String> ITEMS = Map.ofEntries(
        Map.entry("mio_icif:reactor/item_reactor_vent", "vent"),
        Map.entry("mio_icif:resource/item_coil", "coil"),
        Map.entry("mio_icif:normal/item_steam_turbine_blade", "turbine"),
        Map.entry("mio_icif:resource/item_heatconductor", "heat_conductor"),
        Map.entry("mio_icif:item_tool_iron_driller", "drill"),
        Map.entry("mio_icif:item_tool_diamond_driller", "drill"),
        Map.entry("mio_icif:item_tool_iridium_driller", "drill"),
        Map.entry("mio_icif:item_tool_od_scanner", "scanner"),
        Map.entry("mio_icif:item_tool_ov_scanner", "scanner"),
        Map.entry("mio_icif:resource/item_crystal_memory", "memory"),
        Map.entry("mio_icif:resource/item_rtg_pellet", "rtg_pellet"),
        Map.entry("mio_icif:normal/item_fuel_rod", "fuel_rod"),
        Map.entry("mio_icif:normal/item_tin_empty_can", "tin_can"),
        Map.entry("mio_icif:reactor/item_reactor_collant_simple", "coolant_cell"),
        Map.entry("mio_icif:reactor/item_reactor_collant_triple", "coolant_cell"),
        Map.entry("mio_icif:reactor/item_reactor_coolant_six", "coolant_cell"),
        Map.entry("mio_icif:normal/item_deuterium_cell", "deuterium_rod"),
        Map.entry("mio_icif:normal/item_tritium_cell", "tritium_rod"),
        Map.entry("mio_icif:normal/item_frozen_neon_pellet", "neon_pellet"));

    /** Item id prefixes -> silhouette (every cell, battery level, rotor and cutting blade looks alike). */
    private static final Map<String, String> PREFIXES = Map.of(
        "mio_icif:cell/item_cell_", "cell",
        "mio_icif:normal/item_chargebat_", "battery",
        "mio_icif:rotors/", "rotor");

    /** The item a typed machine slot takes (what its silhouette shows), or empty for a slot that takes anything (or gives). */
    public static ItemStack forType(@Nullable SlotType type) {
        if (type == null) return ItemStack.EMPTY;
        return item(switch (type) {
            case BATTERY -> "mio_icif:normal/item_chargebat_lev0";
            case FLUID_INPUT -> "mio_icif:cell/item_cell_empty";
            case ROTOR -> "mio_icif:rotors/item_iron_rotor";
            case TURBINE -> "mio_icif:normal/item_steam_turbine_blade";
            case COIL -> "mio_icif:resource/item_coil";
            case HEAT_CONDUCTOR -> "mio_icif:resource/item_heatconductor";
            case DRILL -> "mio_icif:item_tool_iron_driller";
            case SCANNER -> "mio_icif:item_tool_od_scanner";
            case MEMORY -> "mio_icif:resource/item_crystal_memory";
            case RTG_PELLET -> "mio_icif:resource/item_rtg_pellet";
            default -> null;
        });
    }

    /** The silhouette sprite of an item (mio_icif:slot/<name>), or null when it has none. */
    public static @Nullable ResourceLocation sprite(ItemStack stack) {
        String name = name(stack);
        return name == null ? null : ResourceLocation.fromNamespaceAndPath("mio_icif", "slot/" + name);
    }

    /** The silhouette name of an item, or null. */
    public static @Nullable String name(ItemStack stack) {
        if (stack.isEmpty()) return null;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String name = ITEMS.get(id);
        if (name != null) return name;
        if (id.endsWith("_cut_blade") && id.startsWith("mio_icif:")) return "cut_blade";
        for (var entry : PREFIXES.entrySet()) if (id.startsWith(entry.getKey())) return entry.getValue();
        return null;
    }

    /** The item ids the table names (for the test). */
    public static java.util.Set<String> itemIds() { return ITEMS.keySet(); }

    private static ItemStack item(@Nullable String id) {
        if (id == null) return ItemStack.EMPTY;
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        return item == net.minecraft.world.item.Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }
}
