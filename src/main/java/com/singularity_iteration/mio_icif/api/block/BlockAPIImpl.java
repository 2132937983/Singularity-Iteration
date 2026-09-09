package com.singularity_iteration.mio_icif.api.block;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * 方块 API 实现
 *
 * <p>所有方块通过 {@link BuiltInRegistries#BLOCK} 按 {@link ResourceLocation} 查找，
 * 不直接引用内部注册表持有者（如 {@code mio_icif_blocks}），避免内部重构影响 API 层。
 */
public class BlockAPIImpl implements IBlockAPI {

    private static final String MOD_ID = "mio_icif";

    private Block getModBlock(String name) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(MOD_ID, name));
    }

    @Override
    public Block getBlock(ResourceLocation id) {
        return BuiltInRegistries.BLOCK.get(id);
    }

    @Override
    public Block getMachineHullBasic() {
        return getModBlock("producer/block_machine_hull_basic");
    }

    @Override
    public Block getMachineHullAdvanced() {
        return getModBlock("producer/block_machine_hull_advanced");
    }

    @Override
    public Collection<ResourceLocation> getAllBlockIds() {
        return BuiltInRegistries.BLOCK.keySet().stream()
            .filter(id -> id.getNamespace().equals(MOD_ID))
            .collect(Collectors.toList());
    }
}

