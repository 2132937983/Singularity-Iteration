package com.miophas.singularity_iteration.common.service.registry;

import com.miophas.singularity_iteration.core.api.registry.IMioIcifRegistries;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * mio_icif 注册表 API 实现
 *
 * <p>提供完整的注册表访问功能，包括：
 * <ul>
 *   <li>按 ID 查询方块/物品/流体</li>
 *   <li>按前缀批量查询</li>
 *   <li>按分类查询</li>
 *   <li>检查是否存在</li>
 *   <li>获取 ItemStack/FluidStack</li>
 * </ul>
 */
public class MioIcifRegistriesImpl implements IMioIcifRegistries {

    private static final Logger LOGGER = LoggerFactory.getLogger(MioIcifRegistriesImpl.class);
    private static final String MOD_ID = "mio_icif";

    /**
     * Registry data is published as one immutable snapshot.  The previous
     * implementation updated three mutable maps independently, which meant a
     * concurrent API caller could observe a half-refreshed registry.  More
     * importantly, prefix queries still walked the complete vanilla registry
     * on every call.  A snapshot also gives us a stable read view while the
     * server is building a replacement cache.
     */
    private final Object cacheLock = new Object();
    private volatile RegistrySnapshot snapshot = RegistrySnapshot.empty();

    /**
 * 刷新存（在注册完成后调用）
     */
    public void refreshCache() {
        synchronized (cacheLock) {
            Map<ResourceLocation, Block> blocks = BuiltInRegistries.BLOCK.entrySet().stream()
                .filter(entry -> entry.getKey().location().getNamespace().equals(MOD_ID))
                .collect(Collectors.toMap(
                    entry -> (ResourceLocation) entry.getKey().location(),
                    Map.Entry::getValue
                ));
            Map<ResourceLocation, Item> items = BuiltInRegistries.ITEM.entrySet().stream()
                .filter(entry -> entry.getKey().location().getNamespace().equals(MOD_ID))
                .collect(Collectors.toMap(
                    entry -> (ResourceLocation) entry.getKey().location(),
                    Map.Entry::getValue
                ));
            Map<ResourceLocation, Fluid> fluids = BuiltInRegistries.FLUID.entrySet().stream()
                .filter(entry -> entry.getKey().location().getNamespace().equals(MOD_ID))
                .collect(Collectors.toMap(
                    entry -> (ResourceLocation) entry.getKey().location(),
                    Map.Entry::getValue
                ));

            snapshot = new RegistrySnapshot(
                Map.copyOf(blocks),
                Map.copyOf(items),
                Map.copyOf(fluids),
                buildPrefixIndex(blocks),
                buildPrefixIndex(items),
                true
            );
            LOGGER.info("Registry cache refreshed: {} blocks, {} items, {} fluids",
                blocks.size(), items.size(), fluids.size());
        }
    }

    /**
 * 构造函数：初始化时自动刷新存
     */
    public MioIcifRegistriesImpl() {
 // 延迟初始化，在首次访问时刷新存
    }

    /**
     * 鎳掑姞杞界紦瀛橈細鍦ㄩ�栨�¤�块棶鏃惰嚜鍔ㄥ埛鏂�
     */
    private void ensureCacheInitialized() {
        if (snapshot.initialized()) return;
        refreshCache();
    }

    @Override
    public Block getBlock(String id) {
        if (id == null || id.isEmpty()) return null;
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(MOD_ID, id);
        ensureCacheInitialized();
        return snapshot.blocks().get(loc);
    }

    @Override
    public Block getBlock(ResourceLocation id) {
        if (id == null) return null;
        if (MOD_ID.equals(id.getNamespace())) {
            ensureCacheInitialized();
            return snapshot.blocks().get(id);
        }
        Block block = BuiltInRegistries.BLOCK.get(id);
        return BuiltInRegistries.BLOCK.containsKey(id) ? block : null;
    }

    /**
     * 检查方块是否存在
     */
    public boolean hasBlock(String id) {
        return getBlock(id) != null;
    }

    /**
     * 检查方块是否存在
     */
    public boolean hasBlock(ResourceLocation id) {
        return getBlock(id) != null;
    }

    @Override
    public Collection<Block> getBlocksByPrefix(String prefix) {
        ensureCacheInitialized();
        if (prefix == null) return Collections.emptyList();
        if (prefix.isEmpty()) return snapshot.blocks().values();
        return snapshot.blocksByPrefix().getOrDefault(prefix, Collections.emptyList());
    }

    @Override
    public Collection<Block> getBlocksByCategory(BlockCategory category) {
        if (category == null) return Collections.emptyList();
        // 基于命名约定分类
        // 注意：前缀匹配是近似方法，部分分类可能存在重叠或遗漏
        // 如需精确分类，建议使用 getBlocksByPrefix() 或标签系统
        String prefix = switch (category) {
            case MACHINE -> "";       // 机器无统一前缀（furnace_elc, powder_elc, extractor_elc 等）
            case GENERATOR -> "generator";
            case CABLE -> "wire";
            case STORAGE -> "";       // 储能方块无统一前缀（batbox, cesu, mfe, mfsu）
            case ORE -> "ore";
            case RESOURCE -> "block_"; // 资源方块（block_tin, block_uranium 等）
            case BUILDING -> "fence";  // 建筑方块（block_fence_iron 等）
            case FLUID -> "pipe";
            case CROP -> "rub";       // 作物（rubber wood/leaves/sapling）
            case OTHER -> "";
        };
        
        // 对于 MACHINE 和 STORAGE 分类，使用精确过滤
        if (category == BlockCategory.MACHINE) {
            return getAllBlocks().values().stream()
                .filter(block -> {
                    String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
                    return path.endsWith("_elc") || 
                           path.contains("miner") || 
                           path.contains("compressor") ||
                           path.contains("extractor") ||
                           path.contains("furnace") ||
                           path.contains("macerator") ||
                           path.contains("recycler") ||
                           path.contains("scanner") ||
                           path.contains("sorter") ||
                           path.contains("canner") ||
                           path.contains("centrifuge") ||
                           path.contains("fermenter") ||
                           path.contains("electrolyzer") ||
                           path.contains("harvest") ||
                           path.contains("induction") ||
                           path.contains("matter") ||
                           path.contains("matron") ||
                           path.contains("pump") ||
                           path.contains("replicator") ||
                           path.contains("terra") ||
                           path.contains("washer") ||
                           path.contains("molecular_transformer") ||
                           path.contains("condenser") ||
                           path.contains("block_cutter") ||
                           path.contains("metal_former") ||
                           path.contains("blast_furnace") ||
                           path.contains("lathe") ||
                           path.contains("magnetizer") ||
                           path.contains("barrel") ||
                           path.contains("industrial_workbench") ||
                           path.contains("pattern_storage") ||
                           path.contains("solar_distiller") ||
                           path.contains("item_buffer") ||
                           path.contains("item_distributor") ||
                           path.contains("fluid_distributor") ||
                           path.contains("fluid_regulator") ||
                           path.contains("weighted_fluid_distributor") ||
                           path.contains("future_elc") ||
                           path.contains("teleporter") ||
                           path.contains("nuke") ||
                           path.contains("tesla") ||
                           path.contains("steam_repressurizer");
                })
                .collect(Collectors.toList());
        }
        
        if (category == BlockCategory.STORAGE) {
            return getAllBlocks().values().stream()
                .filter(block -> {
                    String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
                    return path.contains("batbox") || 
                           path.contains("cesu") ||
                           path.contains("mfe") ||
                           path.contains("mfsu") ||
                           path.contains("storage_box") ||
                           path.contains("transformer");
                })
                .collect(Collectors.toList());
        }
        
        // 对于 OTHER 分类，返回空集合（不属于任何明确分类的方块）
        if (category == BlockCategory.OTHER) {
            return Collections.emptyList();
        }
        
        // 对于无前缀的分类，返回空集合而非全部方块
        if (prefix.isEmpty()) {
            return Collections.emptyList();
        }
        return getBlocksByPrefix(prefix);
    }

    @Override
    public Map<ResourceLocation, Block> getAllBlocks() {
        ensureCacheInitialized();
        return snapshot.blocks();
    }

    @Override
    public BlockEntityType<?> getBlockEntityType(String id) {
        if (id == null || id.isEmpty()) return null;
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(MOD_ID, id);
        BlockEntityType<?> type = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(loc);
        return BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(loc) ? type : null;
    }

    @Override
    public BlockEntityType<?> getBlockEntityType(ResourceLocation id) {
        if (id == null) return null;
        BlockEntityType<?> type = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(id);
        return BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(id) ? type : null;
    }

    @Override
    public Item getItem(String id) {
        if (id == null || id.isEmpty()) return null;
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(MOD_ID, id);
        ensureCacheInitialized();
        return snapshot.items().get(loc);
    }

    @Override
    public Item getItem(ResourceLocation id) {
        if (id == null) return null;
        if (MOD_ID.equals(id.getNamespace())) {
            ensureCacheInitialized();
            return snapshot.items().get(id);
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        return BuiltInRegistries.ITEM.containsKey(id) ? item : null;
    }

    /**
     * 检查物品是否存在
     */
    public boolean hasItem(String id) {
        return getItem(id) != null;
    }

    /**
     * 检查物品是否存在
     */
    public boolean hasItem(ResourceLocation id) {
        return getItem(id) != null;
    }

    /**
     * 获取 ItemStack
     *
     * @param id 物品 ID
     * @param count 数量
     * @return ItemStack，如果物品不存在则返回空 ItemStack
     */
    public ItemStack getItemStack(String id, int count) {
        if (count <= 0) return ItemStack.EMPTY;
        Item item = getItem(id);
        if (item == null) {
            LOGGER.warn("Item not found: {}", id);
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, count);
    }

    /**
     * 获取 ItemStack
     *
     * @param id 物品 ID
     * @return ItemStack（数量为 1），如果物品不存在则返回空 ItemStack
     */
    public ItemStack getItemStack(String id) {
        return getItemStack(id, 1);
    }

    @Override
    public Collection<Item> getItemsByPrefix(String prefix) {
        ensureCacheInitialized();
        if (prefix == null) return Collections.emptyList();
        if (prefix.isEmpty()) return snapshot.items().values();
        return snapshot.itemsByPrefix().getOrDefault(prefix, Collections.emptyList());
    }

    @Override
    public Collection<Item> getItemsByCategory(ItemCategory category) {
        if (category == null) return Collections.emptyList();
        String prefix = switch (category) {
            case BATTERY -> "bat";
            case TOOL -> "tool";
            case ARMOR -> "armor";
            case COMPONENT -> "component";
            case CIRCUIT -> "circuit";
            case INGOT -> "ingot";
            case DUST -> "dust";
            case PLATE -> "plate";
            case UPGRADE -> "upgrade";
            case REACTOR -> "reactor";
            case FLUID_CONTAINER -> "cell";
            case SEED -> "seed";
            case FOOD -> "food";
            case OTHER -> "";
        };
        return getItemsByPrefix(prefix);
    }

    @Override
    public Map<ResourceLocation, Item> getAllItems() {
        ensureCacheInitialized();
        return snapshot.items();
    }

    @Override
    public Fluid getFluid(String id) {
        if (id == null || id.isEmpty()) return null;
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(MOD_ID, id);
        ensureCacheInitialized();
        return snapshot.fluids().get(loc);
    }

    @Override
    public Fluid getFluid(ResourceLocation id) {
        if (id == null) return null;
        if (MOD_ID.equals(id.getNamespace())) {
            ensureCacheInitialized();
            return snapshot.fluids().get(id);
        }
        Fluid fluid = BuiltInRegistries.FLUID.get(id);
        return BuiltInRegistries.FLUID.containsKey(id) ? fluid : null;
    }

    /**
     * 检查流体是否存在
     */
    public boolean hasFluid(String id) {
        return getFluid(id) != null;
    }

    /**
     * 获取所有已注册的方块 ID
     */
    public Collection<String> getAllBlockIds() {
        return getAllBlocks().keySet().stream()
            .map(ResourceLocation::getPath)
            .collect(Collectors.toList());
    }

    /**
     * 获取所有已注册的物品 ID
     */
    public Collection<String> getAllItemIds() {
        return getAllItems().keySet().stream()
            .map(ResourceLocation::getPath)
            .collect(Collectors.toList());
    }

    /**
     * 获取所有已注册的流体 ID
     */
    public Collection<String> getAllFluidIds() {
        return getAllFluids().keySet().stream()
            .map(ResourceLocation::getPath)
            .collect(Collectors.toList());
    }

    /**
     * 按关键词搜索方块
     *
     * @param keyword 关键词
     * @return 匹配的方块集合
     */
    public Collection<Block> searchBlocks(String keyword) {
        if (keyword == null || keyword.isEmpty()) return Collections.emptyList();
        String lowerKeyword = keyword.toLowerCase();
        return getAllBlocks().entrySet().stream()
            .filter(entry -> entry.getKey().getPath().contains(lowerKeyword))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    /**
     * 按关键词搜索物品
     *
     * @param keyword 关键词
     * @return 匹配的物品集合
     */
    public Collection<Item> searchItems(String keyword) {
        if (keyword == null || keyword.isEmpty()) return Collections.emptyList();
        String lowerKeyword = keyword.toLowerCase();
        return getAllItems().entrySet().stream()
            .filter(entry -> entry.getKey().getPath().contains(lowerKeyword))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    @Override
    public Map<ResourceLocation, Fluid> getAllFluids() {
        ensureCacheInitialized();
        return snapshot.fluids();
    }

    private static <T> Map<String, List<T>> buildPrefixIndex(Map<ResourceLocation, T> entries) {
        Map<String, List<T>> mutable = new HashMap<>();
        for (Map.Entry<ResourceLocation, T> entry : entries.entrySet()) {
            String path = entry.getKey().getPath();
            for (int end = 1; end <= path.length(); end++) {
                mutable.computeIfAbsent(path.substring(0, end), ignored -> new ArrayList<>()).add(entry.getValue());
            }
        }
        Map<String, List<T>> immutable = new HashMap<>(mutable.size());
        mutable.forEach((prefix, values) -> immutable.put(prefix, List.copyOf(values)));
        return Map.copyOf(immutable);
    }

    private record RegistrySnapshot(
        Map<ResourceLocation, Block> blocks,
        Map<ResourceLocation, Item> items,
        Map<ResourceLocation, Fluid> fluids,
        Map<String, List<Block>> blocksByPrefix,
        Map<String, List<Item>> itemsByPrefix,
        boolean initialized
    ) {
        private static RegistrySnapshot empty() {
            return new RegistrySnapshot(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), false);
        }
    }
}
