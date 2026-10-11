package com.miophas.singularity_iteration.common.item.reactor;

import com.miophas.singularity_iteration.common.item.component.FuelRodDurability;
import com.miophas.singularity_iteration.common.item.component.ReactorComponentData;
import com.miophas.singularity_iteration.common.registry.mio_icif_data_components;
import com.miophas.singularity_iteration.core.api.reactor.IBaseReactorComponent;
import com.miophas.singularity_iteration.core.api.reactor.ReactorComponentType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for all reactor components.
 *
 * Features:
 * - Tracks durability (the service life of the component).
 * - Fuel rods, heat vents, heat exchangers, coolant cells and neutron reflectors extend this class.
 * - Holds the neutron pulse properties and the heat properties of the component.
 * - Can store durability in a DataComponent instead of the vanilla item damage value.
 *   Such items have no vanilla durability and can stack to 64.
 */
@SuppressWarnings("null")
public class mio_icif_reactor extends Item implements IBaseReactorComponent {
    
    // Maximum durability (service life) of the component.
    protected final int maxDurability;

    // True: store durability in the FuelRodDurability DataComponent (fuel rods use this).
    protected final boolean useDataComponentDurability;

    // True: store state in the ReactorComponentData DataComponent (heat vents and heat exchangers use this).
    protected final boolean useReactorComponentData;
    
    // ==================== Component type ====================
    
    // Type of the reactor component.
    protected final ReactorComponentType componentType;
    
    // ==================== Neutron pulse properties ====================
    
    // Number of neutron pulses that this component sends to adjacent components (fuel rods use this).
    protected final int neutronPulseOutput;
    
    // True: the component accepts neutron pulses.
    protected final boolean canReceiveNeutronPulse;
    
    // True: the component is MOX fuel. MOX fuel makes more power when the reactor heat is high.
    protected final boolean isMoxFuel;
    
    // ==================== Heat properties ====================
    
    // Heat that the component makes. A positive value adds heat. A negative value (heat vents) removes heat.
    protected final int heatOutput;
    
    // Maximum heat that the component can store (heat vents and coolant cells use this).
    protected final int maxHeatStorage;
    
    // Heat transfer efficiency in percent, 0 to 100 (heat exchangers use this).
    protected final int heatTransferEfficiency;
    
    // ==================== Constructors ====================
    
    /**
     * Creates a component of type OTHER.
     * @param properties item properties
     * @param maxDurability maximum durability
     */
    public mio_icif_reactor(Properties properties, int maxDurability) {
        this(properties, maxDurability, ReactorComponentType.OTHER);
    }
    
    /**
     * Creates a component of the given type that uses vanilla item damage for durability.
     * @param properties item properties
     * @param maxDurability maximum durability
     * @param componentType type of the reactor component
     */
    public mio_icif_reactor(Properties properties, int maxDurability, ReactorComponentType componentType) {
        super(properties.durability(maxDurability));
        this.maxDurability = maxDurability;
        this.componentType = componentType;
        this.useDataComponentDurability = false;
        this.useReactorComponentData = false;
        
        // No neutron pulse behavior by default.
        this.neutronPulseOutput = 0;
        this.canReceiveNeutronPulse = false;
        this.isMoxFuel = false;
        
        // No heat behavior by default.
        this.heatOutput = 0;
        this.maxHeatStorage = 0;
        this.heatTransferEfficiency = 0;
    }

    /**
     * Creates a component that can store durability or state in a DataComponent.
     * Fuel rods use FuelRodDurability. Other components use ReactorComponentData.
     * If one of the flags is true, the item has no vanilla durability and stacks to 64.
     * @param properties item properties
     * @param maxDurability maximum durability
     * @param componentType type of the reactor component
     * @param useDataComponentDurability true to store durability in FuelRodDurability (fuel rods)
     * @param useReactorComponentData true to store state in ReactorComponentData (heat vents and heat exchangers)
     */
    protected mio_icif_reactor(Properties properties, int maxDurability, ReactorComponentType componentType, boolean useDataComponentDurability, boolean useReactorComponentData) {
        super((useDataComponentDurability || useReactorComponentData) ? properties.stacksTo(64) : properties.durability(maxDurability));
        this.maxDurability = maxDurability;
        this.componentType = componentType;
        this.useDataComponentDurability = useDataComponentDurability;
        this.useReactorComponentData = useReactorComponentData;
        
        // No neutron pulse behavior by default.
        this.neutronPulseOutput = 0;
        this.canReceiveNeutronPulse = false;
        this.isMoxFuel = false;
        
        // No heat behavior by default.
        this.heatOutput = 0;
        this.maxHeatStorage = 0;
        this.heatTransferEfficiency = 0;
    }
    
    /**
     * Creates a component with all neutron pulse and heat properties (for fuel rods and similar components).
     * @param properties item properties
     * @param maxDurability maximum durability
     * @param componentType type of the reactor component
     * @param neutronPulseOutput number of neutron pulses sent to adjacent components
     * @param canReceiveNeutronPulse true if the component accepts neutron pulses
     * @param isMoxFuel true if the component is MOX fuel
     * @param heatOutput heat that the component makes
     * @param maxHeatStorage maximum heat that the component can store
     * @param heatTransferEfficiency heat transfer efficiency in percent, 0 to 100
     */
    public mio_icif_reactor(Properties properties, int maxDurability, 
                            ReactorComponentType componentType,
                            int neutronPulseOutput, boolean canReceiveNeutronPulse, boolean isMoxFuel,
                            int heatOutput, int maxHeatStorage, int heatTransferEfficiency) {
        super(properties.durability(maxDurability));
        this.maxDurability = maxDurability;
        this.componentType = componentType;
        this.useDataComponentDurability = false;
        this.useReactorComponentData = false;
        this.neutronPulseOutput = neutronPulseOutput;
        this.canReceiveNeutronPulse = canReceiveNeutronPulse;
        this.isMoxFuel = isMoxFuel;
        this.heatOutput = heatOutput;
        this.maxHeatStorage = maxHeatStorage;
        this.heatTransferEfficiency = heatTransferEfficiency;
    }
    
    /**
     * Returns the maximum durability.
     */
    public int getMaxDurability() {
        return maxDurability;
    }
    
    // ==================== Component type methods ====================
    
    /**
     * Returns the type of the reactor component.
     */
    @Override
    public ReactorComponentType getComponentType() {
        return componentType;
    }
    
    /**
     * Returns true if the component is a fuel rod.
     */
    @Override
    public boolean isFuelRod() {
        return componentType == ReactorComponentType.FUEL_ROD;
    }
    
    /**
     * Returns true if the component is a heat sink.
     */
    public boolean isHeatSink() {
        return componentType == ReactorComponentType.HEAT_SINK;
    }
    
    /**
     * Returns true if the component is a cooler.
     */
    public boolean isCooler() {
        return componentType == ReactorComponentType.COOLER;
    }
    
    /**
     * Returns true if the component is a neutron reflector.
     */
    public boolean isNeutronReflector() {
        return componentType == ReactorComponentType.NEUTRON_REFLECTOR;
    }
    
    /**
     * Returns the number of fuel cells in the component (single, dual or quad).
     * The default is 1 (single). Multi-cell fuel rods override this method.
     *
     * @return number of cells (1 = single, 2 = dual, 4 = quad)
     */
    public int getNumberOfCells() {
        return 1;
    }
    
    // ==================== Neutron pulse methods ====================
    
    /**
     * Returns the number of neutron pulses that this component sends to adjacent components.
     */
    @Override
    public int getNeutronPulseOutput() {
        return neutronPulseOutput;
    }
    
    /**
     * Returns true if the component accepts neutron pulses.
     */
    public boolean canReceiveNeutronPulse() {
        return canReceiveNeutronPulse;
    }
    
    /**
     * Returns true if the component is MOX fuel. MOX fuel makes more power when the reactor heat is high.
     */
    @Override
    public boolean isMoxFuel() {
        return isMoxFuel;
    }
    
    /**
     * Called when the component receives neutron pulses.
     * Subclasses override this method for special behavior.
     * For example, MOX fuel makes more power when the reactor heat is high.
     * @param stack the component item stack
     * @param pulseCount number of neutron pulses received
     * @param reactorHeat current reactor heat
     * @param maxReactorHeat maximum reactor heat
     * @return additional power that the component makes, in EU/tick
     */
    public int onReceiveNeutronPulse(ItemStack stack, int pulseCount, int reactorHeat, int maxReactorHeat) {
        // The default makes no additional power. Subclasses can override this.
        return 0;
    }
    
    // ==================== Heat methods ====================
    
    /**
     * Returns the heat that the component makes. A positive value adds heat. A negative value (heat vents) removes heat.
     */
    @Override
    public int getHeatOutput() {
        return heatOutput;
    }
    
    /**
     * Returns the maximum heat that the component can store (heat vents and coolant cells use this).
     */
    @Override
    public int getMaxHeatStorage() {
        return maxHeatStorage;
    }
    
    /**
     * Returns the heat transfer efficiency in percent, 0 to 100 (heat exchangers use this).
     */
    @Override
    public int getHeatTransferEfficiency() {
        return heatTransferEfficiency;
    }
    
    /**
     * Absorbs heat into the component (heat vents and coolant cells use this).
     * @param stack the component item stack
     * @param heatToAbsorb heat to absorb
     * @return heat that the component actually absorbed
     */
    public int absorbHeat(ItemStack stack, int heatToAbsorb) {
        // The default absorbs no heat.
        // Heat vents and coolant cells override this method to absorb heat.
        return 0;
    }
    
    /**
     * Returns the heat that the component stores now.
     * @param stack the component item stack
     * @return stored heat
     */
    public int getStoredHeat(ItemStack stack) {
        // The default is 0. Components that store heat (heat vents and heat exchangers) override this method.
        return 0;
    }
    
    /**
     * Sets the stored heat of the component.
     * @param stack the component item stack
     * @param heat new stored heat
     */
    public void setStoredHeat(ItemStack stack, int heat) {
        // The default does nothing. Components that store heat (heat vents and heat exchangers) override this method.
    }
    
    /**
     * Transfers heat to adjacent components (heat exchangers use this).
     * @param stack the component item stack
     * @param heatToTransfer heat to transfer
     * @return heat that the component actually transfers
     */
    public int transferHeatToAdjacent(ItemStack stack, int heatToTransfer) {
        // Scale the heat by the heat transfer efficiency (percent).
        int actualTransfer = (int) (heatToTransfer * heatTransferEfficiency / 100.0);
        return actualTransfer;
    }
    
    /**
     * Returns the remaining durability from the active durability storage.
     */
    public int getCurrentDurability(ItemStack stack) {
        if (useDataComponentDurability) {
            FuelRodDurability data = stack.get(mio_icif_data_components.FUEL_ROD_DURABILITY.get());
            return data != null ? data.remainingUses() : maxDurability;
        }
        if (useReactorComponentData) {
            ReactorComponentData data = stack.get(mio_icif_data_components.REACTOR_COMPONENT_DATA.get());
            return data != null ? data.getRemaining() : maxDurability;
        }
        return stack.getMaxDamage() - stack.getDamageValue();
    }
    
    /**
     * Applies damage to the component.
     * @param stack the component item stack
     * @param amount damage to apply
     * @return true if the component is depleted after the damage
     */
    public boolean damageItem(ItemStack stack, int amount) {
        if (useDataComponentDurability) {
            FuelRodDurability data = stack.get(mio_icif_data_components.FUEL_ROD_DURABILITY.get());
            if (data == null) {
                return true;
            }
            int newRemaining = data.remainingUses() - amount;
            if (newRemaining <= 0) {
                stack.set(mio_icif_data_components.FUEL_ROD_DURABILITY.get(), data.withRemainingUses(0));
                return true;
            } else {
                stack.set(mio_icif_data_components.FUEL_ROD_DURABILITY.get(), data.withRemainingUses(newRemaining));
                return false;
            }
        }
        if (useReactorComponentData) {
            ReactorComponentData data = stack.get(mio_icif_data_components.REACTOR_COMPONENT_DATA.get());
            if (data == null) {
                return true;
            }
            int newStored = data.storedValue() + amount;
            if (newStored >= data.maxValue()) {
                stack.set(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(), data.withStoredValue(data.maxValue()));
                return true;
            } else {
                stack.set(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(), data.withStoredValue(newStored));
                return false;
            }
        }
        int newDamage = stack.getDamageValue() + amount;
        if (newDamage >= stack.getMaxDamage()) {
            stack.setDamageValue(stack.getMaxDamage());
            return true;
        } else {
            stack.setDamageValue(newDamage);
            return false;
        }
    }
    
    /**
     * Returns true if the component is depleted.
     */
    @Override
    public boolean isDepleted(ItemStack stack) {
        if (useDataComponentDurability) {
            FuelRodDurability data = stack.get(mio_icif_data_components.FUEL_ROD_DURABILITY.get());
            return data == null || data.isDepleted();
        }
        if (useReactorComponentData) {
            ReactorComponentData data = stack.get(mio_icif_data_components.REACTOR_COMPONENT_DATA.get());
            return data == null || data.isDepleted();
        }
        return stack.getDamageValue() >= stack.getMaxDamage();
    }

    /**
     * Returns the item that replaces this component when it is depleted (for example, a depleted fuel rod).
     * Subclasses override this method to return their depleted item.
     * @return the depleted item, or null if there is no depleted item
     */
    @Override
    @Nullable
    public Item getDepletedItem() {
        return null;
    }

    /**
     * Returns true if the component has melted (heat vents and coolant cells use this).
     * The default treats a depleted component as melted.
     * @param stack the component item stack
     * @return true if the component has melted
     */
    public boolean isMelted(ItemStack stack) {
        return isDepleted(stack);
    }

    /**
     * Returns true if the component must melt now (heat vents and coolant cells use this).
     * The default returns false. The component never melts.
     * @param stack the component item stack
     * @return true if the component must melt
     */
    public boolean shouldMelt(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        if (useDataComponentDurability) {
            return stack.get(mio_icif_data_components.FUEL_ROD_DURABILITY.get()) != null;
        }
        if (useReactorComponentData) {
            return stack.get(mio_icif_data_components.REACTOR_COMPONENT_DATA.get()) != null;
        }
        return true;
    }
    
    @Override
    public int getBarWidth(ItemStack stack) {
        if (useDataComponentDurability) {
            FuelRodDurability data = stack.get(mio_icif_data_components.FUEL_ROD_DURABILITY.get());
            if (data == null || data.maxUses() <= 0) return 13;
            return Math.round(13.0F * data.getDurabilityRatio());
        }
        if (useReactorComponentData) {
            ReactorComponentData data = stack.get(mio_icif_data_components.REACTOR_COMPONENT_DATA.get());
            if (data == null || data.maxValue() <= 0) return 13;
            return Math.round(13.0F * data.getDurabilityRatio());
        }
        if (stack == null || stack.isEmpty()) {
            return 13;
        }
        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return 13;
        }
        return Math.round(13.0F - (float)stack.getDamageValue() * 13.0F / (float)maxDamage);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        if (useDataComponentDurability) {
            FuelRodDurability data = stack.get(mio_icif_data_components.FUEL_ROD_DURABILITY.get());
            if (data == null) return 0x00FF00;
            float ratio = data.getDurabilityRatio();
            if (ratio > 0.6F) {
                return 0x00FF00;
            } else if (ratio > 0.3F) {
                return 0xFFFF00;
            } else {
                return 0xFF0000;
            }
        }
        if (useReactorComponentData) {
            ReactorComponentData data = stack.get(mio_icif_data_components.REACTOR_COMPONENT_DATA.get());
            if (data == null) return 0x00FF00;
            float ratio = data.getDurabilityRatio();
            if (ratio > 0.6F) {
                return 0x00FF00;
            } else if (ratio > 0.3F) {
                return 0xFFFF00;
            } else {
                return 0xFF0000;
            }
        }
        if (stack == null || stack.isEmpty()) {
            return 0x00FF00;
        }
        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return 0x00FF00;
        }
        float durabilityRatio = 1.0F - (float)stack.getDamageValue() / (float)maxDamage;
        if (durabilityRatio > 0.6F) {
            return 0x00FF00;
        } else if (durabilityRatio > 0.3F) {
            return 0xFFFF00;
        } else {
            return 0xFF0000;
        }
    }
}

