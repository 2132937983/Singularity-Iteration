package com.miophas.singularity_iteration.core.api.item;

import net.minecraft.world.item.ItemStack;

/**
 * Battery function interface (simplified mio_icif API).
 * All battery items that can store electric energy must implement this interface.
 *
 * <h3>Relation to {@link com.miophas.singularity_iteration.core.api.item.electric.IElectricItem}</h3>
 * <p>This interface and {@code IElectricItem} are two parallel battery item APIs. They serve different cases:
 * <ul>
 *   <li><b>{@code IBatteryItem}</b> (this interface) — the simplified internal mio_icif API.
 *       The method signatures are short (for example {@link #getMaxEnergy()} does not need an ItemStack).
 *       Machines can call it directly. All batteries, electric tools and electric armor of mio_icif
 *       implement this interface through their base classes ({@code mio_icif_bat}, {@code mio_icif_tool_elc},
 *       {@code mio_icif_armor_elc}).</li>
 *   <li><b>{@code IElectricItem}</b> — the IC2-compatible API.
 *       It follows the IC2 1.12.2 {@code IElectricItem} specification. All methods need an
 *       {@code ItemStack} parameter. Use it together with {@code IElectricItemManager}.
 *       Use it for add-on mods that must work with the original IC2 tools and batteries.</li>
 * </ul>
 *
 * <p><b>Which one to use:</b>
 * <ul>
 *   <li>If the item is used only in mio_icif machines, implement {@code IBatteryItem}.</li>
 *   <li>If the item must be compatible with original IC2 (for example the IC2 charging bench or the electric tool manager), implement {@code IElectricItem}.</li>
 *   <li>An item can implement both. They do not conflict.</li>
 * </ul>
 */
public interface IBatteryItem {

    /**
     * Gets the maximum energy capacity (EU).
     */
    long getMaxEnergy();

    /**
     * Gets the maximum energy capacity of the given item stack (EU).
     * <p>By default this method calls {@link #getMaxEnergy()} with no arguments. This is correct for batteries with a fixed capacity.
     * Items with a variable capacity (for example from enchantments, NBT or upgrades) must override this method
     * and return the actual capacity for {@code stack}.
     *
     * @param stack the item stack
     * @return the maximum energy capacity (EU)
     */
    default long getMaxEnergy(ItemStack stack) {
        return getMaxEnergy();
    }

    /**
     * Gets the energy that is stored now (EU).
     */
    long getEnergy(ItemStack stack);

    /**
     * Adds energy (EU).
     * @return the energy that was added (EU)
     */
    long addEnergy(ItemStack stack, long amount);

    /**
     * Extracts energy (EU).
     * @return the energy that was extracted (EU)
     */
    long extractEnergy(ItemStack stack, long amount);

    /**
     * Checks if the item is full.
     */
    boolean isFull(ItemStack stack);

    /**
     * Checks if the item is empty.
     */
    boolean isEmpty(ItemStack stack);

    /**
     * Gets the charge rate (EU/tick). The default is 0.
     */
    default long getChargeRate(ItemStack stack) {
        return 0;
    }

    /**
     * Sets the energy value. This method does not check the maximum.
     * <p>Use it mainly where the energy must be set directly, for example on the creative mode tab.
     * The default implementation does nothing. Items that support this operation must override this method.
     *
     * @param stack  the item stack
     * @param energy the energy value to set (EU)
     */
    default void setEnergy(ItemStack stack, long energy) {
        // Default implementation: do nothing.
    }
}