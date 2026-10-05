// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.guide;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;

/**
 * In-game manual through GuideME (optional dependency, 0.1.7.33).
 *
 * <p>The guide is data driven: {@code assets/mio_icif/guideme_guides/manual.json} defines it and
 * the pages live in {@code assets/mio_icif/guides/mio_icif/manual/}. SI has no compile-time
 * reference to GuideME: this class finds GuideME's guide item and its {@code guideme:guide_id}
 * component through the registries, so SI runs unchanged without GuideME.
 */
public final class GuideBook {
    private GuideBook() {}

    public static final ResourceLocation GUIDE_ID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "manual");
    private static final ResourceLocation GUIDE_ITEM = ResourceLocation.fromNamespaceAndPath("guideme", "guide");
    private static final ResourceLocation GUIDE_COMPONENT = ResourceLocation.fromNamespaceAndPath("guideme", "guide_id");

    public static boolean available() {
        return ModList.get() != null && ModList.get().isLoaded("guideme");
    }

    /** The manual as an item stack, or EMPTY without GuideME. */
    @SuppressWarnings("unchecked")
    public static ItemStack create() {
        if (!available()) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(GUIDE_ITEM);
        DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(GUIDE_COMPONENT);
        if (item == Items.AIR || type == null) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item);
        try {
            stack.set((DataComponentType<ResourceLocation>) type, GUIDE_ID);
        } catch (ClassCastException e) {
            return ItemStack.EMPTY;
        }
        return stack;
    }

    public static void addToTab(CreativeModeTab.Output output) {
        ItemStack stack = create();
        if (!stack.isEmpty()) output.accept(stack);
    }
}
