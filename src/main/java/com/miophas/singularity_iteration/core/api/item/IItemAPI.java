package com.miophas.singularity_iteration.core.api.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;

/**
 * Item API.
 *
 * <p>Provides an interface for querying mio_icif items and related
 * functionality, including:
 * <ul>
 *   <li>Querying mod items</li>
 *   <li>Checking item types (electric tools, batteries, armor, etc.)</li>
 *   <li>Managing custom durability systems</li>
 *   <li>Battery charge and discharge operations</li>
 *   <li>Fluid cell operations (fill, drain, capacity query)</li>
 *   <li>Jetpack mode and thrust control</li>
 *   <li>Armor feature toggling</li>
 *   <li>Upgrade plugin direction and effects</li>
 * </ul>
 *
 * <h3>Fluid Cell API (read this if you develop an addon)</h3>
 * <p>The fluid cell API gives full support for fluid container operations:
 * <ul>
 *   <li>{@link #isFluidCell(ItemStack)} - checks if the item is a fluid cell</li>
 *   <li>{@link #getFluidCellCapacity(ItemStack)} - gets the cell capacity</li>
 *   <li>{@link #getFluidCellContent(ItemStack)} - gets the fluid that the cell holds now</li>
 *   <li>{@link #canFluidCellHold(ItemStack, Fluid)} - checks if the cell can hold the given fluid</li>
 *   <li>{@link #fillFluidCell(ItemStack, FluidStack, boolean)} - fills fluid into the cell</li>
 *   <li>{@link #drainFluidCell(ItemStack, int, boolean)} - drains fluid from the cell</li>
 *   <li>{@link #getFluidCellEmptyContainer(ItemStack)} - gets the empty container after a drain</li>
 *   <li>{@link #getFluidCellFilledContainer(ItemStack, Fluid)} - gets the full container after a fill</li>
 * </ul>
 * <p>An item that implements {@link IFluidCellItem} can:
 * <ul>
 *   <li>Interact with fluid systems of other mods through the Forge fluid capability</li>
 *   <li>Move fluid directly when the player right-clicks a tank</li>
 *   <li>Put out or pick up fluid through a dispenser</li>
 *   <li>Be processed automatically in the canning machine (no hard-coded mapping is necessary)</li>
 *   <li>Hold a partial fill (not only a fixed 1000 mB)</li>
 * </ul>
 */
public interface IItemAPI {

    /**
     * Get an item by its ID.
     *
     * @param id the item ID
     * @return the item, or null if it does not exist
     */
    Item getItem(ResourceLocation id);

    /**
     * Check whether an item is an electric tool.
     *
     * @param stack the item stack
     * @return true if it is an electric tool
     */
    boolean isElectricTool(ItemStack stack);

    /**
     * Check whether an item is a battery.
     *
     * @param stack the item stack
     * @return true if it is a battery
     */
    boolean isBattery(ItemStack stack);

    /**
     * Check whether an item is electric armor.
     *
     * @param stack the item stack
     * @return true if it is electric armor
     */
    boolean isElectricArmor(ItemStack stack);

    /**
     * Check whether an item is a reactor component.
     *
     * @param stack the item stack
     * @return true if it is a reactor component
     */
    boolean isReactorComponent(ItemStack stack);

    /**
     * Check whether an item is an upgrade plugin.
     *
     * @param stack the item stack
     * @return true if it is an upgrade plugin
     */
    boolean isUpgrade(ItemStack stack);

    /**
     * Check whether an item can be processed by a lathe.
     *
     * @param stack the item stack
     * @return true if it is a lathe-processable item
     */
    boolean isLatheItem(ItemStack stack);

    /**
     * Check whether an item uses a custom durability system.
     *
     * @param stack the item stack
     * @return true if it uses custom durability
     */
    boolean hasCustomDamage(ItemStack stack);

    /**
     * Get the current custom durability damage value of an item.
     *
     * @param stack the item stack
     * @return the current damage value
     */
    int getCustomDamage(ItemStack stack);

    /**
     * Get the maximum custom durability of an item.
     *
     * @param stack the item stack
     * @return the maximum durability
     */
    int getMaxCustomDamage(ItemStack stack);

    /**
     * Get the maximum energy capacity (EU) of a battery item.
     *
     * @param stack the battery item stack
     * @return the maximum capacity, or 0 for non-battery items
     */
    long getBatteryCapacity(ItemStack stack);

    /**
     * Get the currently stored energy (EU) in a battery item.
     *
     * @param stack the battery item stack
     * @return the currently stored energy, or 0 for non-battery items
     */
    long getBatteryStored(ItemStack stack);

    /**
     * Charge a battery item.
     *
     * @param stack the battery item stack
     * @param amount the amount of energy to charge (EU)
     * @param simulate if true, do not actually modify the item
     * @return the amount of energy actually charged
     */
    long chargeBattery(ItemStack stack, long amount, boolean simulate);

    /**
     * Discharge a battery item.
     *
     * @param stack the battery item stack
     * @param amount the amount of energy to discharge (EU)
     * @param simulate if true, do not actually modify the item
     * @return the amount of energy actually discharged
     */
    long dischargeBattery(ItemStack stack, long amount, boolean simulate);

    // ========== Electric tool API ==========

    /**
     * Get the maximum energy capacity (EU) of an electric tool.
     *
     * @param stack the tool item stack
     * @return the maximum capacity, or 0 if the item is not an electric tool
     */
    long getElectricToolMaxEnergy(ItemStack stack);

    /**
     * Get the energy (EU) that an electric tool stores now.
     *
     * @param stack the tool item stack
     * @return the current energy, or 0 if the item is not an electric tool
     */
    long getElectricToolStored(ItemStack stack);

    /**
     * Get the energy (EU) that an electric tool uses for each use.
     *
     * @param stack the tool item stack
     * @return the energy for each use, or 0 if the item is not an electric tool
     */
    long getElectricToolEnergyPerUse(ItemStack stack);

    /**
     * Get the tier of an electric tool.
     *
     * @param stack the tool item stack
     * @return the tool tier, or 0 if the item is not an electric tool
     */
    int getElectricToolTier(ItemStack stack);

    /**
     * Charge an electric tool.
     *
     * @param stack the tool item stack
     * @param amount the amount of energy to charge (EU)
     * @param simulate if true, do not change the item
     * @return the energy that was actually charged (EU)
     */
    long chargeElectricTool(ItemStack stack, long amount, boolean simulate);

    /**
     * Discharge an electric tool.
     *
     * @param stack the tool item stack
     * @param amount the amount of energy to discharge (EU)
     * @param simulate if true, do not change the item
     * @return the energy that was actually discharged (EU)
     */
    long dischargeElectricTool(ItemStack stack, long amount, boolean simulate);

    /**
     * Check if an electric tool has sufficient energy for one use.
     *
     * @param stack the tool item stack
     * @return true if the energy is sufficient
     */
    boolean hasEnoughToolEnergy(ItemStack stack);

    // ========== Electric armor API ==========

    /**
     * Get the maximum energy capacity (EU) of electric armor.
     *
     * @param stack the armor item stack
     * @return the maximum capacity, or 0 if the item is not electric armor
     */
    long getElectricArmorMaxEnergy(ItemStack stack);

    /**
     * Get the energy (EU) that electric armor stores now.
     *
     * @param stack the armor item stack
     * @return the current energy, or 0 if the item is not electric armor
     */
    long getElectricArmorStored(ItemStack stack);

    /**
     * Get the energy (EU) that electric armor uses each tick.
     *
     * @param stack the armor item stack
     * @return the energy for each tick, or 0 if the item is not electric armor
     */
    long getElectricArmorEnergyPerTick(ItemStack stack);

    /**
     * Get the tier of electric armor.
     *
     * @param stack the armor item stack
     * @return the armor tier, or 0 if the item is not electric armor
     */
    int getElectricArmorTier(ItemStack stack);

    /**
     * Charge electric armor.
     *
     * @param stack the armor item stack
     * @param amount the amount of energy to charge (EU)
     * @param simulate if true, do not change the item
     * @return the energy that was actually charged (EU)
     */
    long chargeElectricArmor(ItemStack stack, long amount, boolean simulate);

    /**
     * Discharge electric armor.
     *
     * @param stack the armor item stack
     * @param amount the amount of energy to discharge (EU)
     * @param simulate if true, do not change the item
     * @return the energy that was actually discharged (EU)
     */
    long dischargeElectricArmor(ItemStack stack, long amount, boolean simulate);

    /**
     * Check if electric armor has sufficient energy.
     *
     * @param stack the armor item stack
     * @return true if the energy is sufficient
     */
    boolean hasEnoughArmorEnergy(ItemStack stack);

    /**
     * Use energy from electric armor.
     *
     * @param stack the armor item stack
     * @param amount the amount of energy to use (EU)
     * @return true if the energy was used
     */
    boolean consumeArmorEnergy(ItemStack stack, long amount);

    // ========== Reactor component API ==========

    /**
     * Get the type ID of a reactor component.
     *
     * @param stack the item stack
     * @return the type ID, or null if the item is not a reactor component
     */
    String getReactorComponentType(ItemStack stack);

    // ========== Upgrade plugin API ==========

    /**
     * Get the type of an upgrade plugin.
     *
     * @param stack the item stack
     * @return the upgrade type, or null if the item is not an upgrade plugin
     */
    String getUpgradeType(ItemStack stack);

    /**
     * Check if an upgrade plugin is directional.
     *
     * @param stack the item stack
     * @return true if it is directional; false if the item is not an upgrade plugin
     */
    boolean isUpgradeDirectional(ItemStack stack);

    /**
     * Get the direction of an upgrade plugin.
     *
     * @param stack the upgrade plugin item stack
     * @return the direction, or null if no direction is set or the upgrade is not directional
     */
    net.minecraft.core.Direction getUpgradeDirection(ItemStack stack);

    /**
     * Get the IDs of all mod items.
     *
     * @return the item IDs in the mio_icif namespace
     */
    Collection<ResourceLocation> getAllItemIds();

    // ========== Battery charge and discharge rate API ==========

    /**
     * Get the charge rate of a battery.
     *
     * @param stack the battery item stack
     * @return the charge rate (EU/tick), or 0 if the item is not a battery
     */
    long getChargeRate(ItemStack stack);

    /**
     * Get the discharge rate of a battery.
     *
     * @param stack the battery item stack
     * @return the discharge rate (EU/tick), or 0 if the item is not a battery
     */
    long getDischargeRate(ItemStack stack);

    /**
     * Check if a battery is fully charged.
     *
     * @param stack the battery item stack
     * @return true if the battery is full
     */
    boolean isBatteryFull(ItemStack stack);

    /**
     * Check if a battery is empty.
     *
     * @param stack the battery item stack
     * @return true if the stored energy is 0
     */
    boolean isBatteryEmpty(ItemStack stack);

    // ========== Upgrade plugin effect API ==========

    /**
     * Get the speed bonus of an upgrade plugin.
     *
     * @param stack the upgrade plugin item stack
     * @return the speed multiplier (a lower value is faster)
     */
    float getUpgradeSpeedBonus(ItemStack stack);

    /**
     * Get the energy bonus of an upgrade plugin.
     *
     * @param stack the upgrade plugin item stack
     * @return the energy use multiplier
     */
    float getUpgradeEnergyBonus(ItemStack stack);

    /**
     * Get the tier of an upgrade plugin.
     *
     * @param stack the upgrade plugin item stack
     * @return the upgrade tier (1-4, for LV-MV-HV-EV)
     */
    int getUpgradeTier(ItemStack stack);

    // ========== Item type query API (new types) ==========

    /**
     * Check if an item is a weapon.
     *
     * @param stack the item stack
     * @return true if it is a weapon
     */
    boolean isWeapon(ItemStack stack);

    /**
     * Check if an item is a solar helmet.
     *
     * @param stack the item stack
     * @return true if it is a solar helmet
     */
    boolean isSolarHelmet(ItemStack stack);

    /**
     * Check if an item is a jetpack.
     *
     * @param stack the item stack
     * @return true if it is a jetpack
     */
    boolean isJetpack(ItemStack stack);

    /**
     * Check if an item is a fluid cell.
     *
     * @param stack the item stack
     * @return true if it is a fluid cell
     */
    boolean isFluidCell(ItemStack stack);

    /**
     * Get the base damage of a weapon.
     *
     * @param stack the weapon item stack
     * @return the damage value, or 0 if the item is not a weapon
     */
    float getWeaponDamage(ItemStack stack);

    /**
     * Get the effective range of a weapon, in blocks.
     *
     * @param stack the weapon item stack
     * @return the range, or 0 if the item is not a weapon
     */
    float getWeaponRange(ItemStack stack);

    /**
     * Get the energy that a weapon uses for each shot.
     *
     * @param stack the weapon item stack
     * @return the energy for each shot (EU), or 0 if the item is not a weapon
     */
    long getWeaponEnergyPerShot(ItemStack stack);

    /**
     * Get the generation rate of a solar helmet.
     *
     * @param stack the solar helmet item stack
     * @return the energy made each tick (EU), or 0 if the item is not a solar helmet
     */
    long getSolarGenerationRate(ItemStack stack);

    /**
     * Get the thrust of a jetpack.
     *
     * @param stack the jetpack item stack
     * @return the thrust value, or 0 if the item is not a jetpack
     */
    float getJetpackThrust(ItemStack stack);

    /**
     * Get the energy that a jetpack uses each tick while it flies.
     *
     * @param stack the jetpack item stack
     * @return the flight energy for each tick (EU), or 0 if the item is not a jetpack
     */
    long getJetpackEnergyPerTick(ItemStack stack);

    /**
     * Get the current mode of a jetpack.
     *
     * @param stack the jetpack item stack
     * @return the current mode, or OFF if the item is not a jetpack
     */
    IJetpackItem.JetpackMode getJetpackMode(ItemStack stack);

    /**
     * Set the mode of a jetpack.
     *
     * @param stack the jetpack item stack
     * @param mode  the new mode
     */
    void setJetpackMode(ItemStack stack, IJetpackItem.JetpackMode mode);

    /**
     * Get the maximum capacity of a fluid cell.
     *
     * @param stack the fluid cell item stack
     * @return the capacity (mB), or 0 if the item is not a fluid cell
     */
    int getFluidCellCapacity(ItemStack stack);

    /**
     * Get the fluid that a fluid cell holds now.
     *
     * @param stack the fluid cell item stack
     * @return the current fluid stack, or an empty stack if the item is not a fluid cell
     */
    net.neoforged.neoforge.fluids.FluidStack getFluidCellContent(ItemStack stack);

    /**
     * Check if a fluid cell can hold the given fluid.
     *
     * @param stack the fluid cell item stack
     * @param fluid the fluid type
     * @return true if the cell can hold the fluid
     */
    boolean canFluidCellHold(ItemStack stack, net.minecraft.world.level.material.Fluid fluid);

    /**
     * Fill fluid into a fluid cell.
     *
     * @param stack the fluid cell item stack
     * @param fluid the fluid to fill
     * @param simulate if true, do not change the item
     * @return the amount that was actually filled (mB)
     */
    int fillFluidCell(ItemStack stack, net.neoforged.neoforge.fluids.FluidStack fluid, boolean simulate);

    /**
     * Drain fluid from a fluid cell.
     *
     * @param stack the fluid cell item stack
     * @param amount the amount to drain (mB)
     * @param simulate if true, do not change the item
     * @return the fluid stack that was actually drained
     */
    net.neoforged.neoforge.fluids.FluidStack drainFluidCell(ItemStack stack, int amount, boolean simulate);

    /**
     * Get the empty container that is left after a fluid cell is drained.
     *
     * @param stack the fluid cell item stack
     * @return the empty container, or an empty stack if the item is not a fluid cell
     */
    ItemStack getFluidCellEmptyContainer(ItemStack stack);

    /**
     * Get the full container after a fluid cell is filled.
     *
     * @param stack the fluid cell item stack
     * @param fluid the fluid to fill
     * @return the full container, or an empty stack if the item is not a fluid cell
     */
    ItemStack getFluidCellFilledContainer(ItemStack stack, net.minecraft.world.level.material.Fluid fluid);

    /**
     * Check if an item is a dynamic fluid cell (it accepts any fluid and partial fills).
     *
     * @param stack the item stack
     * @return true if it is a dynamic fluid cell
     */
    boolean isDynamicFluidCell(ItemStack stack);

    /**
     * Make a dynamic fluid cell and fill it with the given fluid.
     *
     * @param fluid the fluid to fill
     * @param amount the fill amount (mB)
     * @return the filled dynamic cell, or an empty stack if this fails
     */
    ItemStack createDynamicFluidCell(net.minecraft.world.level.material.Fluid fluid, int amount);

    // ========== Armor feature API ==========

    /**
     * Get the feature list of electric armor.
     *
     * @param stack the armor item stack
     * @return the feature information list, or an empty list if the item is not electric armor
     */
    java.util.List<ArmorFeatureInfo> getArmorFeatures(ItemStack stack);

    /**
     * Toggle the state of a <b>toggle-type</b> feature of electric armor.
     *
     * <p>This has an effect only on a feature where {@link ArmorFeatureInfo#isMode()} is
     * {@code false} and that the item declares in
     * {@link IElectricArmorItem#getFeatures(ItemStack)}.
     * For a mode-type feature, use {@link #cycleArmorFeature(ItemStack, String)}.
     *
     * @param stack the armor item stack
     * @param featureKey the feature key
     * @return the state after the toggle (the current state if nothing changed)
     */
    boolean toggleArmorFeature(ItemStack stack, String featureKey);

    /**
     * Read the enabled state of a toggle-type feature of electric armor.
     *
     * @param stack the armor item stack
     * @param featureKey the feature key
     * @return true if enabled; {@code true} also for a feature that is not declared or is mode-type
     */
    boolean isArmorFeatureEnabled(ItemStack stack, String featureKey);

    /**
     * Set the enabled state of a toggle-type feature of electric armor.
     *
     * @param stack the armor item stack
     * @param featureKey the feature key
     * @param enabled true to enable the feature
     */
    void setArmorFeatureEnabled(ItemStack stack, String featureKey, boolean enabled);

    /**
     * Change a <b>mode-type</b> feature of electric armor to its next mode.
     *
     * <p>The item must declare a feature where {@link ArmorFeatureInfo#isMode()} is {@code true}.
     * The item must also implement {@link IArmorModeFeatureItem}
     * ({@link IJetpackItem} has a default implementation).
     *
     * @param stack the armor item stack
     * @param featureKey the feature key
     * @return true if the mode changed
     */
    boolean cycleArmorFeature(ItemStack stack, String featureKey);

    /**
     * Get the information that an item declares for a feature key.
     *
     * @param stack the armor item stack
     * @param featureKey the feature key
     * @return the feature information, or {@code null} if the item does not declare it
     */
    ArmorFeatureInfo getArmorFeatureInfo(ItemStack stack, String featureKey);

    /**
     * Get the energy that electric armor uses for each point of damage.
     *
     * @param stack the armor item stack
     * @return the energy for each point of damage (EU), or 0 if the item is not electric armor
     */
    long getEnergyPerDamage(ItemStack stack);

    /**
     * Get the damage absorption ratio of electric armor in the given equipment slot.
     *
     * @param stack the armor item stack
     * @param slot the equipment slot
     * @return the absorption ratio (0.0 to 1.0 or more), or 0 if the item is not electric armor
     */
    float getDamageAbsorptionRatio(ItemStack stack, EquipmentSlot slot);

    // ========== Energy set API (mainly for the creative mode tab) ==========

    /**
     * Set the energy value of a battery. This does not check the maximum.
     *
     * @param stack the battery item stack
     * @param energy the energy value (EU)
     */
    void setBatteryEnergy(ItemStack stack, long energy);

    /**
     * Set the energy value of an electric tool. This does not check the maximum.
     *
     * @param stack the tool item stack
     * @param energy the energy value (EU)
     */
    void setElectricToolEnergy(ItemStack stack, long energy);
}