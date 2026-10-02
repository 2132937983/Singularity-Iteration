// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.tuning;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * An item with player-adjustable sliders (Shift + right-click opens the tuning screen). Values live
 * in the stack's custom data under {@code SiTuning}; the server clamps every change to the spec.
 */
public interface ITunableItem {
    String TAG = "SiTuning";

    /** One slider: integer value in [min, max] moved in {@code step}s; {@code unit} is appended in the UI. */
    record Spec(String key, int min, int max, int step, int def, String unit) {
        public int clamp(int v) {
            int c = Math.max(min, Math.min(max, v));
            return min + Math.round((c - min) / (float) step) * step;
        }
        public Component label() { return Component.translatable("tuning.mio_icif." + key); }
        public Component hint() { return Component.translatable("tuning.mio_icif." + key + ".hint"); }
    }

    List<Spec> tuningSpecs(ItemStack stack);

    /** Lines shown under the sliders (cost, effective stats...), computed from the current values. */
    List<Component> tuningSummary(ItemStack stack);

    default Component tuningTitle(ItemStack stack) { return stack.getHoverName(); }

    static int get(ItemStack stack, Spec spec) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return spec.def();
        CompoundTag tag = data.copyTag().getCompound(TAG);
        return tag.contains(spec.key()) ? spec.clamp(tag.getInt(spec.key())) : spec.def();
    }

    static void set(ItemStack stack, Spec spec, int value) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag tag = root.getCompound(TAG);
        tag.putInt(spec.key(), spec.clamp(value));
        root.put(TAG, tag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    default Spec spec(ItemStack stack, String key) {
        for (Spec s : tuningSpecs(stack)) if (s.key().equals(key)) return s;
        return null;
    }

    default int tuning(ItemStack stack, String key) {
        Spec s = spec(stack, key);
        return s == null ? 0 : get(stack, s);
    }
}
