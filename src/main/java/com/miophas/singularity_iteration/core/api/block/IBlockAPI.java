package com.miophas.singularity_iteration.core.api.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;

/**
 * Block API.
 *
 * <p>It gives access to mio_icif blocks, including:
 * <ul>
 *   <li>queries for mod blocks</li>
 *   <li>block properties</li>
 * </ul>
 */
public interface IBlockAPI {

    /**
     * Gets a block from its ID.
     *
     * @param id the block ID
     * @return the block, or null if it does not exist
     */
    Block getBlock(ResourceLocation id);

    /**
     * Gets the basic machine casing block.
     */
    Block getMachineHullBasic();

    /**
     * Gets the advanced machine casing block.
     */
    Block getMachineHullAdvanced();

    /**
     * Gets the IDs of all registered blocks.
     */
    Collection<ResourceLocation> getAllBlockIds();
}