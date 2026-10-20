package com.miophas.singularity_iteration.common.blockentity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * One conversion a machine performs in code rather than through a JSON recipe (the Steam Generator
 * boiling water, the Oil Rig drilling crude oil). Such machines publish theirs from a static
 * {@code conversions()} built from the same constants their tick logic uses, so recipe viewers show
 * what the machine really does and cannot drift from it. See {@link MachineConversions#all()}.
 *
 * @param machines     the machine first (it names the conversion), then other blocks it runs in
 * @param fluidInputs  one entry per input slot, each listing interchangeable fluids
 * @param itemInputs   one entry per input slot, each listing interchangeable items
 * @param fluidOutputs fluids produced
 * @param itemOutputs  items produced
 * @param notes        the conditions and costs that are not ingredients (heat, EU, chance)
 * @param ticks        how long one conversion takes, 0 when it has no fixed time
 */
public record MachineConversion(List<ItemLike> machines, List<List<FluidStack>> fluidInputs,
                                List<List<ItemStack>> itemInputs, List<FluidStack> fluidOutputs,
                                List<ItemStack> itemOutputs, List<Component> notes, int ticks) {

    /** The machine the conversion belongs to. */
    public ItemLike machine() {
        return machines.get(0);
    }

    public static Builder of(ItemLike machine, ItemLike... alsoIn) {
        return new Builder(machine, alsoIn);
    }

    /** A number for a note: "1,000,000", "2.25". */
    public static String number(double n) {
        return String.format(Locale.ROOT, "%,.2f", n).replaceAll("\\.?0+$", "");
    }

    /** Ticks as seconds for a note: "2.25 s". */
    public static String seconds(int ticks) {
        return number(ticks / 20.0) + " s";
    }

    /** A chance (0..1) as a percentage for a note: "80%". */
    public static String percent(double chance) {
        return number(chance * 100) + "%";
    }

    public static final class Builder {
        private final List<ItemLike> machines = new ArrayList<>();
        private final List<List<FluidStack>> fluidInputs = new ArrayList<>();
        private final List<List<ItemStack>> itemInputs = new ArrayList<>();
        private final List<FluidStack> fluidOutputs = new ArrayList<>();
        private final List<ItemStack> itemOutputs = new ArrayList<>();
        private final List<Component> notes = new ArrayList<>();
        private int ticks;

        private Builder(ItemLike machine, ItemLike... alsoIn) {
            machines.add(machine);
            machines.addAll(Arrays.asList(alsoIn));
        }

        /** An input slot taking {@code amount} of any of {@code fluids}. */
        public Builder fluidIn(int amount, Fluid... fluids) {
            fluidInputs.add(Arrays.stream(fluids).map(fluid -> new FluidStack(fluid, amount)).toList());
            return this;
        }

        /** An input slot taking any of {@code stacks}. */
        public Builder itemIn(ItemStack... stacks) {
            itemInputs.add(List.of(stacks));
            return this;
        }

        public Builder fluidOut(Fluid fluid, int amount) {
            fluidOutputs.add(new FluidStack(fluid, amount));
            return this;
        }

        public Builder itemOut(ItemStack stack) {
            itemOutputs.add(stack);
            return this;
        }

        /** One conversion takes {@code ticks}. */
        public Builder time(int ticks) {
            this.ticks = ticks;
            return this;
        }

        /** A note line; numbers are formatted with {@link #number}. */
        public Builder note(String key, Object... args) {
            Object[] shown = Arrays.stream(args).map(arg -> arg instanceof Number n ? number(n.doubleValue()) : arg).toArray();
            notes.add(Component.translatable(key, shown));
            return this;
        }

        public MachineConversion build() {
            return new MachineConversion(List.copyOf(machines), List.copyOf(fluidInputs), List.copyOf(itemInputs),
                List.copyOf(fluidOutputs), List.copyOf(itemOutputs), List.copyOf(notes), ticks);
        }
    }
}
