package com.miophas.singularity_iteration.core.prefab.inventory;

import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;

/** Content-specific legacy slot defaults; addons can override MachineItemHandler's protected validators. */
public final class SlotItemPredicates {
    private static volatile Predicate<ItemStack> reactor = stack -> false;
    private static volatile Predicate<ItemStack> pellet = stack -> false;
    private static volatile Predicate<ItemStack> turbine = stack -> false;
    private SlotItemPredicates() {}

    public static void configure(Predicate<ItemStack> reactorItems, Predicate<ItemStack> pellets, Predicate<ItemStack> turbines) {
        reactor = Objects.requireNonNull(reactorItems);
        pellet = Objects.requireNonNull(pellets);
        turbine = Objects.requireNonNull(turbines);
    }

    static boolean reactor(ItemStack stack) { return reactor.test(stack); }
    static boolean pellet(ItemStack stack) { return pellet.test(stack); }
    static boolean turbine(ItemStack stack) { return turbine.test(stack); }
}
