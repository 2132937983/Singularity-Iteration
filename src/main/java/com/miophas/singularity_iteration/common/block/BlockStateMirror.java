package com.miophas.singularity_iteration.common.block;

/**
 * A block entity whose setting is also shown by its block state (metal former mode, ...). Called
 * after the block is placed - when an item carrying the block-entity data was used, the loaded
 * setting must be pushed to the freshly placed default state.
 */
public interface BlockStateMirror {
    void mirrorToBlockState();
}
