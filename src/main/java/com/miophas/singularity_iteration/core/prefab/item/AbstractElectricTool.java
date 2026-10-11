package com.miophas.singularity_iteration.core.prefab.item;

import com.miophas.singularity_iteration.core.api.item.IElectricToolItem;
import com.miophas.singularity_iteration.core.runtime.energy.EnergyPackCharging;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * Base class for electric tools. It gives complete default energy storage and energy use.
 *
 * <p>An addon developer extends this class and sets the constructor parameters.
 * This makes a fully working electric tool. No access to mio_icif internal classes is necessary.
 *
 * <h3>Energy storage</h3>
 * <p>The tool uses the shared DataComponent energy storage of {@link AbstractBattery} ({@code mio_icif:battery_energy}).
 * It does not use the durability system.
 *
 * <h3>Example</h3>
 * <pre>{@code
 * public class MyElectricDrill extends AbstractElectricTool {
 *     public MyElectricDrill() {
 *         super(new Properties(), 30000, 0, 128, 100, 3);
 *     }
 * }
 * }</pre>
 *
 * <h3>Features that you get automatically</h3>
 * <ul>
 *   <li>Energy storage (on the shared DataComponent)</li>
 *   <li>Energy bar rendering (green to red gradient)</li>
 *   <li>Energy tooltip</li>
 *   <li>Shift + right-click charges equipment</li>
 *   <li>Charger compatibility (through the IBatteryItem interface)</li>
 *   <li>No vanilla durability loss</li>
 *   <li>Energy use and energy checks</li>
 *   <li>Save and load of a hand-held inventory</li>
 * </ul>
 */
public class AbstractElectricTool extends AbstractBattery implements IElectricToolItem {

    private final long energyPerUse;
    private final int toolTier;

    /**
     * Makes an electric tool (not stackable).
     *
     * @param properties    the item properties
     * @param maxEnergy     the maximum energy capacity (EU)
     * @param initialEnergy the initial energy (EU)
     * @param chargeRate    the charge rate (EU/tick)
     * @param energyPerUse  the energy used for each use (EU)
     * @param toolTier      the tool tier
     */
    protected AbstractElectricTool(Properties properties, long maxEnergy, long initialEnergy,
                                   long chargeRate, long energyPerUse, int toolTier) {
        super(properties, maxEnergy, initialEnergy, chargeRate);
        this.energyPerUse = energyPerUse;
        this.toolTier = toolTier;
    }

    // ==================== Equivalent of IC2 chargeFromArmor ====================

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        EnergyPackCharging.trackHolder(stack, entity);
    }

    /**
     * In original IC2 1.12.2, tools call {@code chargeFromArmor()} first in {@code ElectricItemManager.use()}.
     * Before a tool reads or uses energy, it takes energy from the worn charging pack.
     * In this project, tools use different patterns ("read getEnergy to check, then consumeEnergy").
     * Thus, this method pulls energy at the read point. Every use path reads the energy first.
     *
     * <p>Side effect: each call can move energy from the worn armor into {@code stack}.
     */
    @Override
    public long getEnergy(ItemStack stack) {
        EnergyPackCharging.pullFromArmor(stack);
        return super.getEnergy(stack);
    }

    // ==================== IElectricToolItem implementation ====================

    @Override
    public long getEnergyPerUse() {
        return energyPerUse;
    }

    @Override
    public int getToolTier() {
        return toolTier;
    }

    @Override
    public boolean hasEnoughEnergy(ItemStack stack) {
        return getEnergy(stack) >= energyPerUse;
    }

    // ==================== Energy use ====================

    /**
     * Checks if the tool has enough energy for the given amount.
     *
     * @param stack  the item stack
     * @param amount the necessary energy (EU)
     * @return true if the stored energy is at least {@code amount}; false if {@code amount} is negative
     */
    public boolean hasEnoughEnergy(ItemStack stack, long amount) {
        return amount >= 0 && getEnergy(stack) >= amount;
    }

    /**
     * Uses the energy for one use ({@link #getEnergyPerUse()}).
     *
     * @param stack the item stack
     * @return true if the energy was used
     */
    public boolean consumeEnergy(ItemStack stack) {
        return consumeEnergy(stack, energyPerUse);
    }

    /**
     * Uses the given amount of energy.
     *
     * @param stack  the item stack
     * @param amount the amount to use (EU)
     * @return true if the energy was used
     */
    public boolean consumeEnergy(ItemStack stack, long amount) {
        return com.miophas.singularity_iteration.core.api.item.BatteryTransfer.consume(stack, this, amount);
    }

    /**
     * Gets the remaining energy as a fraction (0.0 to 1.0).
     */
    public float getDurabilityPercent(ItemStack stack) {
        long max = getMaxEnergy();
        return max <= 0 ? 0F : (float) getEnergy(stack) / max;   // no NaN for a zero capacity
    }

    /**
     * Gets the number of uses left. Returns 0 if the energy per use is 0 or less.
     */
    public long getRemainingUses(ItemStack stack) {
        return energyPerUse <= 0 ? 0 : getEnergy(stack) / energyPerUse;
    }

    /**
     * Checks if the tool can be used (it has energy).
     */
    public boolean canUse(ItemStack stack) {
        return !isEmpty(stack);
    }

    // ==================== Prevent vanilla durability loss ====================

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, java.util.function.Consumer<Item> onBroken) {
        return 0;
    }

    // ==================== Hand-held inventory serialization ====================

    /**
     * Saves the hand-held inventory to the CUSTOM_DATA of the item.
     * Use it for electric tools with a built-in inventory, for example the drill.
     * Empty and null entries are not saved. Does nothing if {@code containerStack} is empty.
     *
     * @param containerStack the container item stack
     * @param inventory      the inventory array
     * @param registries     the registry access
     */
    public static void saveHandHeldInventory(ItemStack containerStack, ItemStack[] inventory, net.minecraft.core.HolderLookup.Provider registries) {
        if (containerStack.isEmpty()) return;

        CompoundTag rootTag;
        CustomData customData = containerStack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            rootTag = customData.copyTag();
        } else {
            rootTag = new CompoundTag();
        }

        ListTag contentList = new ListTag();
        for (int i = 0; i < inventory.length; i++) {
            if (inventory[i] != null && !inventory[i].isEmpty()) {
                CompoundTag slotNbt = new CompoundTag();
                slotNbt.putInt("Slot", i);
                Tag itemTag = inventory[i].save(registries);
                if (itemTag instanceof CompoundTag compound) {
                    slotNbt.put("Item", compound);
                }
                contentList.add(slotNbt);
            }
        }
        rootTag.put("HandHeldItems", contentList);

        containerStack.set(DataComponents.CUSTOM_DATA, CustomData.of(rootTag));
    }

    /**
     * Loads the hand-held inventory from the CUSTOM_DATA of the item.
     * First fills the array with ItemStack.EMPTY. Slots out of range are ignored.
     *
     * @param containerStack the container item stack
     * @param inventory      the inventory array (output parameter)
     * @param registries     the registry access
     */
    public static void loadHandHeldInventory(ItemStack containerStack, ItemStack[] inventory, net.minecraft.core.HolderLookup.Provider registries) {
        java.util.Arrays.fill(inventory, ItemStack.EMPTY);
        if (containerStack.isEmpty()) return;

        CustomData customData = containerStack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return;

        CompoundTag rootTag = customData.copyTag();
        if (!rootTag.contains("HandHeldItems", Tag.TAG_LIST)) return;

        ListTag contentList = rootTag.getList("HandHeldItems", Tag.TAG_COMPOUND);
        for (int i = 0; i < contentList.size(); i++) {
            CompoundTag slotNbt = contentList.getCompound(i);
            if (!slotNbt.contains("Slot", Tag.TAG_BYTE) && !slotNbt.contains("Slot", Tag.TAG_INT)) continue;
            int slot = slotNbt.contains("Slot", Tag.TAG_BYTE) ? Byte.toUnsignedInt(slotNbt.getByte("Slot")) : slotNbt.getInt("Slot");
            if (slot >= 0 && slot < inventory.length) {
                inventory[slot] = ItemStack.parseOptional(registries, slotNbt.getCompound("Item"));
            }
        }
    }
}