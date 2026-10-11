package com.miophas.singularity_iteration.core.runtime.crop;


import com.miophas.singularity_iteration.core.api.crop.PlantType;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * Plant registry manager (internal implementation).
 * It manages the registration and lookup of all plant types.
 *
 * <p><strong>This is an internal implementation class. Addons must not access it directly.</strong>
 * Use the public interface of ICropAPI to register plants.
 */
public class PlantRegistry {

    // Singleton instance
    public static final PlantRegistry instance = new PlantRegistry();

    // All registered plant types, keyed by "modId:typeId" (thread-safe)
    private final Map<String, PlantType> plants = new java.util.concurrent.ConcurrentHashMap<>();

    // Base seeds, used to convert vanilla seeds (thread-safe)
    private final List<BaseSeed> baseSeeds = new java.util.concurrent.CopyOnWriteArrayList<>();

    private PlantRegistry() {}

    /**
     * Registers a plant type.
     * If the key "modId:typeId" is already registered, the new plant type replaces the old one and a warning is logged.
     */
    public void registerPlant(PlantType plantType) {
        String key = plantType.getModId() + ":" + plantType.getTypeId();
        if (plants.containsKey(key)) {
            org.slf4j.LoggerFactory.getLogger(PlantRegistry.class).warn("Plant {} is already registered, overwriting!", key);
        }
        plants.put(key, plantType);
        org.slf4j.LoggerFactory.getLogger(PlantRegistry.class).info("Registered plant: {}", key);
    }

    /**
     * Gets a plant type by its ID.
     * Returns null if no plant type has this ID.
     */
    public PlantType getPlant(String modId, String typeId) {
        return plants.get(modId + ":" + typeId);
    }

    /**
     * Gets all registered plants as an unmodifiable view.
     */
    public Collection<PlantType> getAllPlants() {
        return Collections.unmodifiableCollection(plants.values());
    }

    /**
     * Registers a base seed.
     * Use it to convert a vanilla seed into the crop system.
     */
    public void registerBaseSeed(ItemStack seed, PlantType plantType, int stage, int growthSpeed, int yield, int resilience, int weedResistance) {
        baseSeeds.add(new BaseSeed(seed, plantType, stage, growthSpeed, yield, resilience, weedResistance));
    }

    /**
     * Registers a base seed (for backward compatibility; weedResistance is 0).
     */
    public void registerBaseSeed(ItemStack seed, PlantType plantType, int stage, int growthSpeed, int yield, int resilience) {
        registerBaseSeed(seed, plantType, stage, growthSpeed, yield, resilience, 0);
    }

    /**
     * Finds the base seed data for the given seed.
     * The stack count is ignored. Returns null if the stack is empty or no match exists.
     * 
     * <p>Note: the returned BaseSeed has final fields, so it is safe to keep a reference to it.
     */
    public BaseSeed getBaseSeed(ItemStack seed) {
        if (seed.isEmpty()) return null;

        for (BaseSeed baseSeed : baseSeeds) {
            // A registered base seed describes the item and its components, not
            // the amount in the caller's hand.  ItemStack.matches also compares
            // the stack count, which made a normal 16-seed stack fail lookup.
            if (ItemStack.isSameItemSameComponents(baseSeed.seed, seed)) {
                return baseSeed;
            }
        }
        return null;
    }

    /**
     * Gets all registered base seeds (as a defensive copy).
     * 
     * @return an unmodifiable list of base seeds
     */
    public List<BaseSeed> getAllBaseSeeds() {
        return Collections.unmodifiableList(new ArrayList<>(baseSeeds));
    }

    /**
     * Checks if the item is a base seed.
     */
    public boolean isBaseSeed(ItemStack seed) {
        return getBaseSeed(seed) != null;
    }

    /**
     * Base seed data.
     */
    public static class BaseSeed {
        public final ItemStack seed;
        public final PlantType plantType;
        public final int stage;
        public final int growthSpeed;
        public final int yield;
        public final int resilience;
        public final int weedResistance;

        public BaseSeed(ItemStack seed, PlantType plantType, int stage, int growthSpeed, int yield, int resilience, int weedResistance) {
            this.seed = seed.copy();
            this.plantType = plantType;
            this.stage = stage;
            this.growthSpeed = growthSpeed;
            this.yield = yield;
            this.resilience = resilience;
            this.weedResistance = weedResistance;
        }

        /**
         * Constructor for backward compatibility (weedResistance is 0).
         */
        public BaseSeed(ItemStack seed, PlantType plantType, int stage, int growthSpeed, int yield, int resilience) {
            this(seed, plantType, stage, growthSpeed, yield, resilience, 0);
        }
    }
}
