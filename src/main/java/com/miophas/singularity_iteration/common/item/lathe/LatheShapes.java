package com.miophas.singularity_iteration.common.item.lathe;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The lathe profiles that recipes ask for (IC2 shaped_recipes.ini, itemTurningBlanks@numeric state),
 * each with its name and the item it is made for. Every {@code mio_icif:lathe_state} ingredient of the
 * recipes has its profile here (checked by a GameTest). Add-ons name their own profiles through the
 * recipe API ({@code registerLatheShape}), which adds to {@link #all()}.
 */
public final class LatheShapes {
    private LatheShapes() {}

    /**
     * @param state  the thickness of the 5 positions
     * @param usedIn the id of the item crafted from it
     * @param id     the id an add-on registered it under; null for the built-in IC2 shapes
     */
    public record Shape(LatheTurningBlanksItem.LatheMaterial material, int[] state, String usedIn, @org.jetbrains.annotations.Nullable ResourceLocation id) {
        public Shape(LatheTurningBlanksItem.LatheMaterial material, int[] state, String usedIn) {
            this(material, state, usedIn, null);
        }

        /**
         * The name key: IC2 InternalName style {@code ic2.<item>.tooltip.<numeric state in hex>} for the
         * built-in shapes, {@code lathe_shape.<namespace>.<path>} for registered ones.
         */
        public String nameKey() {
            if (id != null) return "lathe_shape." + id.getNamespace() + "." + id.getPath().replace('/', '.');
            return "ic2." + material.internalName + ".tooltip." + Integer.toHexString(LatheTurningBlanksItem.getNumericState(state));
        }

        /** The name; a registered shape without a translation shows its profile. */
        public Component name() {
            return id == null ? Component.translatable(nameKey()) : Component.translatableWithFallback(nameKey(), profile());
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

    private static final List<Shape> REGISTERED = new CopyOnWriteArrayList<>();

    /** The built-in shapes ({@link #ALL}) and the ones add-ons registered. */
    public static List<Shape> all() {
        if (REGISTERED.isEmpty()) return ALL;
        List<Shape> all = new ArrayList<>(ALL);
        all.addAll(REGISTERED);
        return all;
    }

    /**
     * Names a profile for add-ons. The profile has 5 positions of 1 to the material's width (3 wood,
     * 5 iron); a profile already named for that material is refused.
     *
     * @return the reason it was refused, or empty when it was added
     */
    public static Optional<String> register(Shape shape) {
        if (shape.id() == null || shape.state() == null || shape.state().length != 5) return Optional.of("a profile has 5 positions");
        for (int thickness : shape.state()) {
            if (thickness < 1 || thickness > shape.material().width) {
                return Optional.of("each position is 1 to " + shape.material().width + " for " + shape.material().name);
            }
        }
        if (!BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(shape.usedIn()))) return Optional.of("no item " + shape.usedIn());
        synchronized (REGISTERED) {
            for (Shape known : all()) {
                if (known.material() == shape.material() && Arrays.equals(known.state(), shape.state())) {
                    return Optional.of("profile " + shape.profile() + " is already " + (known.id() == null ? known.nameKey() : known.id()));
                }
            }
            REGISTERED.add(new Shape(shape.material(), shape.state().clone(), shape.usedIn(), shape.id()));
        }
        return Optional.empty();
    }

    /** The named profile this workpiece has, if any. */
    public static Optional<Shape> of(ItemStack stack) {
        if (!(stack.getItem() instanceof LatheTurningBlanksItem blank)) return Optional.empty();
        int[] state = blank.getCurrentState(stack);
        for (Shape shape : all()) {
            if (shape.material() == blank.getMaterial() && Arrays.equals(shape.state(), state)) return Optional.of(shape);
        }
        return Optional.empty();
    }

    /** The named profiles this workpiece can still be cut to (cutting only thins a position). */
    public static List<Shape> reachable(ItemStack stack) {
        List<Shape> list = new ArrayList<>();
        if (!(stack.getItem() instanceof LatheTurningBlanksItem blank)) return list;
        int[] state = blank.getCurrentState(stack);
        for (Shape shape : all()) {
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
