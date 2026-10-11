package com.miophas.singularity_iteration.core.api.machine;

/**
 * API-level slot layout interface.
 * It keeps addon developers independent of the internal SlotLayout implementation.
 */
public interface ISlotLayout {

    /**
     * Gets the number of slots of the given type.
     * @param type the slot type
     * @return the number of slots
     */
    int getCount(ISlotType type);
    
    /**
     * Gets the slot indices of the given type.
     * @param type the slot type
     * @return the array of slot indices
     */
    int[] getSlotsOfType(ISlotType type);
    
    /**
     * Gets the type of the given slot.
     * @param slotIndex the slot index
     * @return the slot type
     */
    ISlotType getType(int slotIndex);
    
    /**
     * Gets the total number of slots.
     * @return the total number of slots
     */
    int getTotalCount();
    
    /**
     * Gets the indices of the input slots.
     * @return the array of input slot indices
     */
    int[] getInputSlots();
    
    /**
     * Gets the indices of the output slots.
     * @return the array of output slot indices
     */
    int[] getOutputSlots();
    
    /**
     * Gets the indices of the battery slots.
     * @return the array of battery slot indices
     */
    int[] getBatterySlots();
    
    /**
     * Gets the indices of the upgrade slots.
     * @return the array of upgrade slot indices
     */
    int[] getUpgradeSlots();
    
    /**
     * Gets the number of input slots.
     * @return the number of input slots
     */
    int getInputCount();
    
    /**
     * Gets the number of output slots.
     * @return the number of output slots
     */
    int getOutputCount();
    
    /**
     * Gets the number of battery slots.
     * @return the number of battery slots
     */
    int getBatteryCount();
    
    /**
     * Gets the number of upgrade slots.
     * @return the number of upgrade slots
     */
    int getUpgradeCount();

    /**
     * Checks if the given slot has the given type.
     */
    default boolean isType(int slot, ISlotType type) {
        for (int s : getSlotsOfType(type)) {
            if (s == slot) return true;
        }
        return false;
    }

    /**
     * Gets the index of the first slot of the given type.
     * @return the start index, or -1 if no slot has this type
     */
    default int getStart(ISlotType type) {
        int[] slots = getSlotsOfType(type);
        return slots.length > 0 ? slots[0] : -1;
    }

    /**
     * Gets the API-level type of the given slot (the same as the internal getInternalSlotType).
     */
    default ISlotType getSlotType(int slotIndex) {
        return getType(slotIndex);
    }

    /**
     * Builder for a slot layout.
     */
    interface Builder {
        Builder input(int count);
        Builder output(int count);
        Builder battery();
        Builder fluidInput(int count);
        Builder fluidOutput(int count);
        Builder upgrade(int count);
        Builder extra(int count);
        Builder rotor();
        Builder turbine();
        Builder reactor(int count);
        Builder coil();
        Builder coil(int count);
        Builder heating();
        Builder fuel();
        Builder fuel(int count);
        Builder memory();
        Builder tool();
        Builder nuclear(int count);
        Builder drill();
        Builder scanner();
        Builder miningPipe();
        Builder filter(int count);
        Builder heatConductor();
        Builder heatConductor(int count);
        Builder rtgPellet();
        Builder rtgPellet(int count);
        ISlotLayout build();
    }
}