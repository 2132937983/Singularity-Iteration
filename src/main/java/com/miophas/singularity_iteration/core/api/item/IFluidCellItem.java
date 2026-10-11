package com.miophas.singularity_iteration.core.api.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Interface for fluid cell items.
 *
 * <p>It describes items that can store fluid (for example, fluid cells and buckets).
 * Items that implement this interface can store and move a specific type of fluid.
 *
 * <h3>IC2-style cell rules (addon developers must read this)</h3>
 * <p>The implementations of this interface (the cells under {@code mio_icif_cells}) follow the rules of the original IC2 fluid cells.
 * They behave like buckets. Do not treat them as a normal {@code ItemStack} container:
 * <ul>
 *   <li><b>Fixed capacity</b>: each cell has a fixed capacity (usually 1000 mB, the same as a vanilla bucket; some cells have 2000 mB).
 *   {@link #getCapacity()} returns the capacity of <b>one</b> cell, not the total capacity of the stack.</li>
 *   <li><b>Empty cell return</b>: when a cell is used in a crafting recipe, {@code .craftRemainder(CELL_EMPTY)} returns an empty cell.
 *       This is the same as a vanilla bucket, which returns an empty bucket after crafting. If a custom addon cell is used in crafting,
 *       it must also set craftRemainder. If not, the cell is used up and not returned.</li>
 *   <li><b>One item for each fluid type</b>: cells for different fluids are different items (water cell, lava cell, biogas cell and so on).
 *       Do not mix them. Cells of the same fluid stack normally.</li>
 *   <li><b>One fluid only</b>: a cell holds only one fluid at a time. If {@code fill} gets a fluid that is different from the stored fluid
 *       and the cell is not empty, it must refuse (return 0). Mixing is not permitted.</li>
 *   <li><b>Unit</b>: all capacity and flow methods use millibuckets (mB).</li>
 *   <li><b>Empty/full checks</b>: {@link #isEmpty(ItemStack)} / {@link #isFull(ItemStack)} are helper checks that compare the capacity
 *       with the current amount. Do not assume that a cell is always at full capacity.</li>
 * </ul>
 */
public interface IFluidCellItem {

    /**
     * Returns the maximum capacity of one fluid cell (mB).
     *
     * @return the capacity (millibuckets)
     */
    int getCapacity();

    /**
     * Returns the fluid that is stored in the item now.
     *
     * @param stack the item stack
     * @return the current fluid stack (can be empty)
     */
    FluidStack getFluid(ItemStack stack);

    /**
     * Returns true if the cell can store the given fluid.
     *
     * @param fluid the fluid
     * @return true if the cell can store it
     */
    boolean canHoldFluid(Fluid fluid);

    /**
     * Fills the item with fluid.
     *
     * @param stack     the item stack
     * @param fluid     the fluid stack to fill
     * @param simulate  if true, only simulate (do not fill)
     * @return the amount that was filled (mB)
     */
    int fill(ItemStack stack, FluidStack fluid, boolean simulate);

    /**
     * Drains fluid from the item.
     *
     * @param stack     the item stack
     * @param amount    the amount to drain (mB)
     * @param simulate  if true, only simulate (do not drain)
     * @return the fluid stack that was drained
     */
    FluidStack drain(ItemStack stack, int amount, boolean simulate);

    /**
     * Returns the maximum amount that can be drained.
     *
     * @param stack the item stack
     * @return the maximum amount to drain (mB)
     */
    default int getMaxDrain(ItemStack stack) {
        return getFluid(stack).getAmount();
    }

    /**
     * Returns true if the fluid cell is empty.
     *
     * @param stack the item stack
     * @return true if the cell contains no fluid
     */
    default boolean isEmpty(ItemStack stack) {
        return getFluid(stack).isEmpty();
    }

    /**
     * Returns true if the fluid cell is full.
     *
     * @param stack the item stack
     * @return true if the fluid amount is at the capacity or more
     */
    default boolean isFull(ItemStack stack) {
        return getFluid(stack).getAmount() >= getCapacity();
    }

    /**
     * Returns the empty container item that this cell becomes after it is drained.
     * <p>For example, a water cell becomes an empty cell, and a filled bucket becomes an empty bucket.
     * The default implementation returns {@link ItemStack#EMPTY}. This means that no container is returned.
     *
     * @param stack the current full cell item stack
     * @return the empty container item stack after the drain
     */
    default ItemStack getEmptyContainer(ItemStack stack) {
        return ItemStack.EMPTY;
    }

    /**
     * Returns the full container item that this empty cell becomes after it is filled with the given fluid.
     * <p>For example, an empty cell filled with water becomes a water cell, and an empty bucket becomes a water bucket.
     * The default implementation returns {@link ItemStack#EMPTY}. This means that the cell cannot be filled.
     *
     * @param stack the current empty cell item stack
     * @param fluid the fluid type to fill
     * @return the full container item stack after the fill
     */
    default ItemStack getFilledContainer(ItemStack stack, Fluid fluid) {
        return ItemStack.EMPTY;
    }
}