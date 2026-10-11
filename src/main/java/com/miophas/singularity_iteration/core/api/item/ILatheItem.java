package com.miophas.singularity_iteration.core.api.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Interface for items that a lathe can process.
 * A lathe can process items that implement this interface. The item gives different products as its state changes.
 * Based on ILatheItem of IC2.
 */
public interface ILatheItem {
    /**
     * Returns the width of the item (a lathe processing parameter).
     * This is the number of faces that the lathe must turn when it processes the item.
     */
    int getWidth(ItemStack stack);

    /**
     * Returns the current lathe processing state.
     * The returned array gives the state of each face (0 = not processed, 1 = processed).
     * The index range is 0 to getWidth()-1.
     */
    int[] getCurrentState(ItemStack stack);

    /**
     * Sets the lathe processing state at the given position.
     *
     * @param position the face index (0-4)
     * @param value the state value
     */
    void setState(ItemStack stack, int position, int value);

    /**
     * Returns the output item after lathe processing at the given position.
     *
     * @param position the face index (0-4)
     */
    ItemStack getOutputItem(ItemStack stack, int position);

    /**
     * Returns the output chance after lathe processing at the given position (0.0 to 1.0).
     *
     * @param position the face index (0-4)
     */
    float getOutputChance(ItemStack stack, int position);

    /**
     * Returns the texture path of the item.
     */
    ResourceLocation getTexture(ItemStack stack);

    /**
     * Returns the hardness of the item (a lathe processing parameter).
     */
    int getHardness(ItemStack stack);

    /**
     * Interface for lathe tools.
     */
    interface ILatheTool extends ICustomDamageItem {
        /**
         * Returns the hardness of the tool.
         */
        int getHardness(ItemStack stack);
    }
}