package com.singularity_iteration.mio_icif.api.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;

import java.util.Collection;
import java.util.Map;

/**
 * mio_icif 注册表 API
 *
 * <p>提供对 mio_icif 内部注册表的访问，让附属模组可以查询和引用本模组的内容。
 *
 * <p>使用示例：
 * <pre>{@code
 * IMioIcifRegistries registries = MioIcifAPI.instance().getRegistries();
 *
 * // 获取所有机器外壳
 * Block basicHull = registries.getBlock("machine_hull_basic");
 * Block advancedHull = registries.getBlock("machine_hull_advanced");
 *
 * // 获取所有电缆
 * Collection<Block> cables = registries.getBlocksByPrefix("cable");
 *
 * // 获取所有电路板
 * Collection<Item> circuits = registries.getItemsByPrefix("circuit");
 *
 * // 获取所有电池
 * Collection<Item> batteries = registries.getItemsByCategory(ItemCategory.BATTERY);
 * }</pre>
 */
public interface IMioIcifRegistries {

    // ========== 方块注册表 ==========

    /**
     * 根据 ID 获取方块
     *
     * @param id 方块 ID（不含命名空间）
     * @return 方块，如果不存在则返回 null
     */
    Block getBlock(String id);

    /**
     * 根据 ResourceLocation 获取方块
     *
     * @param id 完整方块 ID
     * @return 方块，如果不存在则返回 null
     */
    Block getBlock(ResourceLocation id);

    /**
     * 根据前缀获取方块集合
     *
     * @param prefix ID 前缀
     * @return 方块集合
     */
    Collection<Block> getBlocksByPrefix(String prefix);

    /**
     * 根据分类获取方块集合
     *
     * <p>分类基于方块 ID 的路径部分进行匹配。例如：
     * <ul>
     *   <li>{@code MACHINE} - 匹配包含 "compressor", "extractor", "macerator" 等机器关键字的 ID</li>
     *   <li>{@code STORAGE} - 匹配包含 "energy_storage", "battery" 等关键字的 ID</li>
     *   <li>{@code CABLE} - 匹配包含 "cable" 的 ID</li>
     * </ul>
     * 附属模组注册的方块不会自动归类，除非其 ID 包含对应的关键字。
     *
     * @param category 方块分类
     * @return 方块集合
     */
    Collection<Block> getBlocksByCategory(BlockCategory category);

    /**
     * 获取所有注册方块
     *
     * @return 所有方块 ID 到方块的映射
     */
    Map<ResourceLocation, Block> getAllBlocks();

    // ========== 方块实体类型注册表 ==========

    /**
     * 根据 ID 获取方块实体类型
     *
     * @param id 方块实体类型 ID（不含命名空间）
     * @return 方块实体类型，如果不存在则返回 null
     */
    BlockEntityType<?> getBlockEntityType(String id);

    /**
     * 根据 ResourceLocation 获取方块实体类型
     *
     * @param id 完整方块实体类型 ID
     * @return 方块实体类型，如果不存在则返回 null
     */
    BlockEntityType<?> getBlockEntityType(ResourceLocation id);

    // ========== 物品注册表 ==========

    /**
     * 根据 ID 获取物品
     *
     * @param id 物品 ID（不含命名空间）
     * @return 物品，如果不存在则返回 null
     */
    Item getItem(String id);

    /**
     * 根据 ResourceLocation 获取物品
     *
     * @param id 完整物品 ID
     * @return 物品，如果不存在则返回 null
     */
    Item getItem(ResourceLocation id);

    /**
     * 根据前缀获取物品集合
     *
     * @param prefix ID 前缀
     * @return 物品集合
     */
    Collection<Item> getItemsByPrefix(String prefix);

    /**
     * 根据分类获取物品集合
     *
     * <p>分类基于物品 ID 的路径部分进行匹配。例如：
     * <ul>
     *   <li>{@code BATTERY} - 匹配包含 "battery", "energy_pack" 等关键字的 ID</li>
     *   <li>{@code CIRCUIT} - 匹配包含 "circuit" 的 ID</li>
     *   <li>{@code UPGRADE} - 匹配包含 "upgrade" 的 ID</li>
     * </ul>
     * 附属模组注册的物品不会自动归类，除非其 ID 包含对应的关键字。
     *
     * @param category 物品分类
     * @return 物品集合
     */
    Collection<Item> getItemsByCategory(ItemCategory category);

    /**
     * 获取所有注册物品
     *
     * @return 所有物品 ID 到物品的映射
     */
    Map<ResourceLocation, Item> getAllItems();

    // ========== 流体注册表 ==========

    /**
     * 根据 ID 获取流体
     *
     * @param id 流体 ID（不含命名空间）
     * @return 流体，如果不存在则返回 null
     */
    Fluid getFluid(String id);

    /**
     * 根据 ResourceLocation 获取流体
     *
     * @param id 完整流体 ID
     * @return 流体，如果不存在则返回 null
     */
    Fluid getFluid(ResourceLocation id);

    /**
     * 获取所有注册流体
     *
     * @return 所有流体 ID 到流体的映射
     */
    Map<ResourceLocation, Fluid> getAllFluids();

    // ========== 分类枚举 ==========

    enum BlockCategory {
        MACHINE,        // 机器
        GENERATOR,      // 发电机
        CABLE,          // 电缆
        STORAGE,        // 储能方块
        ORE,            // 矿石
        RESOURCE,       // 资源方块
        BUILDING,       // 建筑方块
        FLUID,          // 流体相关
        CROP,           // 作物
        OTHER           // 其他
    }

    enum ItemCategory {
        BATTERY,        // 电池
        TOOL,           // 工具
        ARMOR,          // 护甲
        COMPONENT,      // 组件
        CIRCUIT,        // 电路板
        INGOT,          // 锭
        DUST,           // 粉
        PLATE,          // 板
        UPGRADE,        // 升级
        REACTOR,        // 反应堆组件
        FLUID_CONTAINER,// 流体容器
        SEED,           // 种子
        FOOD,           // 食物
        OTHER           // 其他
    }

    // ========== 实用方法 ==========

    /**
     * 检查方块是否存在
     *
     * @param id 方块 ID（不含命名空间）
     * @return true 如果方块存在
     */
    boolean hasBlock(String id);

    /**
     * 检查方块是否存在
     *
     * @param id 完整方块 ID
     * @return true 如果方块存在
     */
    boolean hasBlock(ResourceLocation id);

    /**
     * 检查物品是否存在
     *
     * @param id 物品 ID（不含命名空间）
     * @return true 如果物品存在
     */
    boolean hasItem(String id);

    /**
     * 检查物品是否存在
     *
     * @param id 完整物品 ID
     * @return true 如果物品存在
     */
    boolean hasItem(ResourceLocation id);

    /**
     * 获取 ItemStack
     *
     * @param id 物品 ID（不含命名空间）
     * @param count 数量
     * @return ItemStack，如果物品不存在则返回 ItemStack.EMPTY
     */
    net.minecraft.world.item.ItemStack getItemStack(String id, int count);

    /**
     * 获取 ItemStack（默认数量为 1）
     *
     * @param id 物品 ID（不含命名空间）
     * @return ItemStack，如果物品不存在则返回 ItemStack.EMPTY
     */
    default net.minecraft.world.item.ItemStack getItemStack(String id) {
        return getItemStack(id, 1);
    }

    /**
     * 获取所有方块 ID 列表
     *
     * @return 所有方块 ID（不含命名空间）
     */
    Collection<String> getAllBlockIds();

    /**
     * 获取所有物品 ID 列表
     *
     * @return 所有物品 ID（不含命名空间）
     */
    Collection<String> getAllItemIds();

    /**
     * 获取所有流体 ID 列表
     *
     * @return 所有流体 ID（不含命名空间）
     */
    Collection<String> getAllFluidIds();

    /**
     * 搜索包含关键字的方块
     *
     * @param keyword 搜索关键字
     * @return 匹配的方块集合
     */
    Collection<Block> searchBlocks(String keyword);

    /**
     * 搜索包含关键字的物品
     *
     * @param keyword 搜索关键字
     * @return 匹配的物品集合
     */
    Collection<Item> searchItems(String keyword);
}