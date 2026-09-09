package com.singularity_iteration.mio_icif.api.internal.crop;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.api.crop.PlantType;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * 植物注册管理器（内部实现）
 * 管理所有植物类型的注册和查询
 *
 * <p><strong>这是内部实现类，不应被 Addon 直接访问。</strong>
 * 请通过 ICropAPI 提供的公共接口进行植物注册操作。
 */
public class PlantRegistry {

    // 单例实例
    public static final PlantRegistry instance = new PlantRegistry();

    // 存储所有注册的植物类型（线程安全）
    private final Map<String, PlantType> plants = new java.util.concurrent.ConcurrentHashMap<>();

    // 存储基础种子（用于从原版种子转换，线程安全）
    private final List<BaseSeed> baseSeeds = new java.util.concurrent.CopyOnWriteArrayList<>();

    private PlantRegistry() {}

    /**
     * 注册植物类型
     */
    public void registerPlant(PlantType plantType) {
        String key = plantType.getModId() + ":" + plantType.getTypeId();
        if (plants.containsKey(key)) {
            Singularity_Iteration.LOGGER.warn("Plant {} is already registered, overwriting!", key);
        }
        plants.put(key, plantType);
        Singularity_Iteration.LOGGER.info("Registered plant: {}", key);
    }

    /**
     * 根据ID获取植物类型
     */
    public PlantType getPlant(String modId, String typeId) {
        return plants.get(modId + ":" + typeId);
    }

    /**
     * 获取所有注册的植物
     */
    public Collection<PlantType> getAllPlants() {
        return Collections.unmodifiableCollection(plants.values());
    }

    /**
     * 注册基础种子
     * 用于将原版种子转换为作物系统
     */
    public void registerBaseSeed(ItemStack seed, PlantType plantType, int stage, int growthSpeed, int yield, int resilience, int weedResistance) {
        baseSeeds.add(new BaseSeed(seed, plantType, stage, growthSpeed, yield, resilience, weedResistance));
    }

    /**
     * 注册基础种子（向后兼容，weedResistance 默认为 0）
     */
    public void registerBaseSeed(ItemStack seed, PlantType plantType, int stage, int growthSpeed, int yield, int resilience) {
        registerBaseSeed(seed, plantType, stage, growthSpeed, yield, resilience, 0);
    }

    /**
     * 根据种子查找对应的基础种子信息
     * 
     * <p>注意：返回的 BaseSeed 是不可变对象，可以安全地持有引用。
     */
    public BaseSeed getBaseSeed(ItemStack seed) {
        if (seed.isEmpty()) return null;

        for (BaseSeed baseSeed : baseSeeds) {
            // 使用 ItemStack.matches 比较物品逻辑相等性
            // 而非引用比较（==），确保不同实例但相同物品能正确匹配
            if (ItemStack.matches(baseSeed.seed, seed)) {
                return baseSeed;
            }
        }
        return null;
    }

    /**
     * 获取所有注册的基础种子（返回防御性副本）
     * 
     * @return 不可修改的基础种子列表
     */
    public List<BaseSeed> getAllBaseSeeds() {
        return Collections.unmodifiableList(new ArrayList<>(baseSeeds));
    }

    /**
     * 检查物品是否是基础种子
     */
    public boolean isBaseSeed(ItemStack seed) {
        return getBaseSeed(seed) != null;
    }

    /**
     * 基础种子信息
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
         * 向后兼容构造函数
         */
        public BaseSeed(ItemStack seed, PlantType plantType, int stage, int growthSpeed, int yield, int resilience) {
            this(seed, plantType, stage, growthSpeed, yield, resilience, 0);
        }
    }
}