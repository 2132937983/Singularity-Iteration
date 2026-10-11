package com.miophas.singularity_iteration.core.api.crop;

import net.minecraft.world.item.ItemStack;

import java.util.Collection;

/**
 * Crop API.
 *
 * <p>Gives access to the mio_icif crop breeding system. It can:
 * <ul>
 *   <li>query plant types</li>
 *   <li>register custom plants</li>
 *   <li>get plant properties</li>
 * </ul>
 */
public interface ICropAPI {

    /**
     * Registers a custom plant type.
     *
     * @param plantType the plant type
     */
    void registerPlant(PlantType plantType);

    /**
     * Gets a plant type by its ID.
     *
     * @param modId the mod ID
     * @param typeId the type ID
     * @return the plant type, or null if it does not exist
     */
    PlantType getPlant(String modId, String typeId);

    /**
     * Gets all registered plant types.
     */
    Collection<PlantType> getAllPlants();

    /**
     * Checks if the item is a base seed. You can plant a base seed on crop sticks.
     *
     * @param stack the item stack
     * @return true if the item is a base seed
     */
    boolean isBaseSeed(ItemStack stack);

    /**
     * Registers a base seed.
     *
     * @param seed the seed item
     * @param plantType the related plant type
     */
    void registerBaseSeed(ItemStack seed, PlantType plantType);

    /**
     * Registers a base seed with custom properties.
     *
     * @param seed the seed item
     * @param plantType the related plant type
     * @param growthRate the growth rate
     * @param gain the yield
     * @param resistance the resistance
     * @param weedEx the weed resistance (not used in this version; kept for future extension)
     */
    void registerBaseSeed(ItemStack seed, PlantType plantType, int growthRate, int gain, int resistance, int weedEx);
}