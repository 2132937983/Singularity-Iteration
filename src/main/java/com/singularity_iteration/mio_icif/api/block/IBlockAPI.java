package com.singularity_iteration.mio_icif.api.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;

/**
 * 方块 API
 *
 * <p>提供与 mio_icif 方块的访问，包括：
 * <ul>
 *   <li>查询模组方块</li>
 *   <li>获取方块属性</li>
 * </ul>
 */
public interface IBlockAPI {

    /**
     * 根据 ID 获取方块
     *
     * @param id 方块 ID
     * @return 方块，如果不存在则返回 null
     */
    Block getBlock(ResourceLocation id);

    /**
     * 获取基础机器外壳方块
     */
    Block getMachineHullBasic();

    /**
     * 获取高级机器外壳方块
     */
    Block getMachineHullAdvanced();

    /**
     * 获取所有注册方块 ID
     */
    Collection<ResourceLocation> getAllBlockIds();
}