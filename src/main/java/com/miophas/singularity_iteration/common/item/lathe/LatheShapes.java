package com.miophas.singularity_iteration.common.item.lathe;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The lathe profiles that recipes ask for (IC2 shaped_recipes.ini, itemTurningBlanks@numeric state),
 * each with its name and the item it is made for. Every {@code mio_icif:lathe_state} ingredient of the
 * recipes has its profile here (checked by a GameTest).
 */
public final class LatheShapes {
    private LatheShapes() {}

    /**
     * @param state  the thickness of the 5 positions
     * @param usedIn the id of the item crafted from it
     */
    public record Shape(LatheTurningBlanksItem.LatheMaterial material, int[] state, String usedIn) {
        /** IC2 InternalName style key of the name: {@code ic2.<item>.tooltip.<numeric state in hex>}. */
        public String nameKey() {
            return "ic2." + material.internalName + ".tooltip." + Integer.toHexString(LatheTurningBlanksItem.getNumericState(state));
        }

        public Item usedInItem() {
            return BuiltInRegistries.ITEM.get(ResourceLocation.parse(usedIn));
        }

        /** The profile as written in the tooltips, e.g. 5-4-3-2-1. */
        public String profile() {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < state.length; j++) sb.append(j == 0 ? "" : "-").append(state[j]);
            return sb.toString();
        }
    }

    private static final String MOD = Singularity_Iteration.MOD_ID + ":";

    public static final List<Shape> ALL = List.of(
        new Shape(LatheTurningBlanksItem.LatheMaterial.IRON, new int[]{5, 4, 3, 2, 1}, MOD + "item_tool_diamond_driller"),
        new Shape(LatheTurningBlanksItem.LatheMaterial.IRON, new int[]{4, 3, 4, 3, 4}, MOD + "kugenerator/block_water_kinetic_generator"),
        new Shape(LatheTurningBlanksItem.LatheMaterial.IRON, new int[]{3, 2, 3, 2, 3}, MOD + "kugenerator/block_stirling_kinetic_generator"),
        new Shape(LatheTurningBlanksItem.LatheMaterial.IRON, new int[]{4, 3, 2, 2, 2}, MOD + "item_tool_laser_miner"),
        // not in IC2: the other kinetic generators (the Electric one stays without, it drives the first lathe)
        new Shape(LatheTurningBlanksItem.LatheMaterial.IRON, new int[]{5, 5, 5, 2, 2}, MOD + "kugenerator/block_manual_kinetic_generator"),
        new Shape(LatheTurningBlanksItem.LatheMaterial.IRON, new int[]{5, 3, 3, 3, 5}, MOD + "kugenerator/block_wind_kinetic_generator"),
        new Shape(LatheTurningBlanksItem.LatheMaterial.IRON, new int[]{1, 3, 5, 3, 1}, MOD + "producer/block_steam_kinetic_generator"));

    /** The named profile this workpiece has, if any. */
    public static Optional<Shape> of(ItemStack stack) {
        if (!(stack.getItem() instanceof LatheTurningBlanksItem blank)) return Optional.empty();
        int[] state = blank.getCurrentState(stack);
        for (Shape shape : ALL) {
            if (shape.material() == blank.getMaterial() && Arrays.equals(shape.state(), state)) return Optional.of(shape);
        }
        return Optional.empty();
    }

    /** The named profiles this workpiece can still be cut to (cutting only thins a position). */
    public static List<Shape> reachable(ItemStack stack) {
        List<Shape> list = new ArrayList<>();
        if (!(stack.getItem() instanceof LatheTurningBlanksItem blank)) return list;
        int[] state = blank.getCurrentState(stack);
        for (Shape shape : ALL) {
            if (shape.material() != blank.getMaterial() || Arrays.equals(shape.state(), state)) continue;
            boolean reachable = true;
            for (int j = 0; j < state.length && reachable; j++) reachable = shape.state()[j] <= state[j];
            if (reachable) list.add(shape);
        }
        return list;
    }

    /** Cuts from the workpiece's state to the shape. */
    public static int cutsTo(ItemStack stack, Shape shape) {
        if (!(stack.getItem() instanceof LatheTurningBlanksItem blank)) return 0;
        int[] state = blank.getCurrentState(stack);
        int cuts = 0;
        for (int j = 0; j < state.length; j++) cuts += state[j] - shape.state()[j];
        return cuts;
    }
}
