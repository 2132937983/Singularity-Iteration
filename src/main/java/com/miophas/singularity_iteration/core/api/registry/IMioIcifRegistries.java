package com.miophas.singularity_iteration.core.api.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;

import java.util.Collection;
import java.util.Map;

/**
 * Registry API of mio_icif.
 *
 * <p>Gives access to the internal registries of mio_icif. Addon mods use it to find and refer to the content of this mod.
 *
 * <p>Example:
 * <pre>{@code
 * IMioIcifRegistries registries = MioIcifAPI.instance().getRegistries();
 *
 * // Get the machine hulls
 * Block basicHull = registries.getBlock("machine_hull_basic");
 * Block advancedHull = registries.getBlock("machine_hull_advanced");
 *
 * // Get all cables
 * Collection<Block> cables = registries.getBlocksByPrefix("cable");
 *
 * // Get all circuit boards
 * Collection<Item> circuits = registries.getItemsByPrefix("circuit");
 *
 * // Get all batteries
 * Collection<Item> batteries = registries.getItemsByCategory(ItemCategory.BATTERY);
 * }</pre>
 */
public interface IMioIcifRegistries {

    // ========== Block registry ==========

    /**
     * Returns the block with the given ID.
     *
     * @param id the block ID (without namespace)
     * @return the block, or null if it does not exist
     */
    Block getBlock(String id);

    /**
     * Returns the block with the given ResourceLocation.
     *
     * @param id the full block ID
     * @return the block, or null if it does not exist
     */
    Block getBlock(ResourceLocation id);

    /**
     * Returns the blocks whose ID starts with the given prefix.
     *
     * @param prefix the ID prefix
     * @return the blocks
     */
    Collection<Block> getBlocksByPrefix(String prefix);

    /**
     * Returns the blocks of a category.
     *
     * <p>The category match uses the path part of the block ID. For example:
     * <ul>
     *   <li>{@code MACHINE} - matches IDs that contain machine keywords such as "compressor", "extractor", "macerator"</li>
     *   <li>{@code STORAGE} - matches IDs that contain keywords such as "energy_storage", "battery"</li>
     *   <li>{@code CABLE} - matches IDs that contain "cable"</li>
     * </ul>
     * Blocks that addon mods register do not get a category automatically. They get one only when their ID contains the related keyword.
     *
     * @param category the block category
     * @return the blocks
     */
    Collection<Block> getBlocksByCategory(BlockCategory category);

    /**
     * Returns all registered blocks.
     *
     * @return a map from each block ID to its block
     */
    Map<ResourceLocation, Block> getAllBlocks();

    // ========== Block entity type registry ==========

    /**
     * Returns the block entity type with the given ID.
     *
     * @param id the block entity type ID (without namespace)
     * @return the block entity type, or null if it does not exist
     */
    BlockEntityType<?> getBlockEntityType(String id);

    /**
     * Returns the block entity type with the given ResourceLocation.
     *
     * @param id the full block entity type ID
     * @return the block entity type, or null if it does not exist
     */
    BlockEntityType<?> getBlockEntityType(ResourceLocation id);

    // ========== Item registry ==========

    /**
     * Returns the item with the given ID.
     *
     * @param id the item ID (without namespace)
     * @return the item, or null if it does not exist
     */
    Item getItem(String id);

    /**
     * Returns the item with the given ResourceLocation.
     *
     * @param id the full item ID
     * @return the item, or null if it does not exist
     */
    Item getItem(ResourceLocation id);

    /**
     * Returns the items whose ID starts with the given prefix.
     *
     * @param prefix the ID prefix
     * @return the items
     */
    Collection<Item> getItemsByPrefix(String prefix);

    /**
     * Returns the items of a category.
     *
     * <p>The category match uses the path part of the item ID. For example:
     * <ul>
     *   <li>{@code BATTERY} - matches IDs that contain keywords such as "battery", "energy_pack"</li>
     *   <li>{@code CIRCUIT} - matches IDs that contain "circuit"</li>
     *   <li>{@code UPGRADE} - matches IDs that contain "upgrade"</li>
     * </ul>
     * Items that addon mods register do not get a category automatically. They get one only when their ID contains the related keyword.
     *
     * @param category the item category
     * @return the items
     */
    Collection<Item> getItemsByCategory(ItemCategory category);

    /**
     * Returns all registered items.
     *
     * @return a map from each item ID to its item
     */
    Map<ResourceLocation, Item> getAllItems();

    // ========== Fluid registry ==========

    /**
     * Returns the fluid with the given ID.
     *
     * @param id the fluid ID (without namespace)
     * @return the fluid, or null if it does not exist
     */
    Fluid getFluid(String id);

    /**
     * Returns the fluid with the given ResourceLocation.
     *
     * @param id the full fluid ID
     * @return the fluid, or null if it does not exist
     */
    Fluid getFluid(ResourceLocation id);

    /**
     * Returns all registered fluids.
     *
     * @return a map from each fluid ID to its fluid
     */
    Map<ResourceLocation, Fluid> getAllFluids();

    // ========== Category enums ==========

    enum BlockCategory {
        MACHINE,        // Machines
        GENERATOR,      // Generators
        CABLE,          // Cables
        STORAGE,        // Energy storage blocks
        ORE,            // Ores
        RESOURCE,       // Resource (storage) blocks
        BUILDING,       // Building blocks
        FLUID,          // Fluid-related blocks
        CROP,           // Crops
        OTHER           // Other
    }

    enum ItemCategory {
        BATTERY,        // Batteries
        TOOL,           // Tools
        ARMOR,          // Armor
        COMPONENT,      // Components
        CIRCUIT,        // Circuit boards
        INGOT,          // Ingots
        DUST,           // Dusts
        PLATE,          // Plates
        UPGRADE,        // Upgrades
        REACTOR,        // Reactor components
        FLUID_CONTAINER,// Fluid containers
        SEED,           // Seeds
        FOOD,           // Food
        OTHER           // Other
    }

    // ========== Utility methods ==========

    /**
     * Returns true if the block exists.
     *
     * @param id the block ID (without namespace)
     * @return true if the block exists
     */
    boolean hasBlock(String id);

    /**
     * Returns true if the block exists.
     *
     * @param id the full block ID
     * @return true if the block exists
     */
    boolean hasBlock(ResourceLocation id);

    /**
     * Returns true if the item exists.
     *
     * @param id the item ID (without namespace)
     * @return true if the item exists
     */
    boolean hasItem(String id);

    /**
     * Returns true if the item exists.
     *
     * @param id the full item ID
     * @return true if the item exists
     */
    boolean hasItem(ResourceLocation id);

    /**
     * Makes a new ItemStack.
     *
     * @param id the item ID (without namespace)
     * @param count the stack size
     * @return the ItemStack, or ItemStack.EMPTY if the item does not exist
     */
    net.minecraft.world.item.ItemStack getItemStack(String id, int count);

    /**
     * Makes a new ItemStack with a stack size of 1.
     *
     * @param id the item ID (without namespace)
     * @return the ItemStack, or ItemStack.EMPTY if the item does not exist
     */
    default net.minecraft.world.item.ItemStack getItemStack(String id) {
        return getItemStack(id, 1);
    }

    /**
     * Returns the IDs of all blocks.
     *
     * @return all block IDs (without namespace)
     */
    Collection<String> getAllBlockIds();

    /**
     * Returns the IDs of all items.
     *
     * @return all item IDs (without namespace)
     */
    Collection<String> getAllItemIds();

    /**
     * Returns the IDs of all fluids.
     *
     * @return all fluid IDs (without namespace)
     */
    Collection<String> getAllFluidIds();

    /**
     * Finds the blocks whose ID contains the keyword.
     *
     * @param keyword the search keyword
     * @return the matching blocks
     */
    Collection<Block> searchBlocks(String keyword);

    /**
     * Finds the items whose ID contains the keyword.
     *
     * @param keyword the search keyword
     * @return the matching items
     */
    Collection<Item> searchItems(String keyword);
}