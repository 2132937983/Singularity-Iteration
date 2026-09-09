package com.singularity_iteration.mio_icif.integration;

import com.singularity_iteration.mio_icif.Singularity_Iteration;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.fml.ModList;

/**
 * Curios API 集成类
 * 仅在 Curios API 已安装时加载和注册饰品
 * 
 * 此类不直接引用任何 Curios 相关的类，所有引用都通过反射延迟加载，
 * 确保在 Curios 未安装时不会触发 ClassNotFoundException。
 */
@SuppressWarnings("null")
public class CuriosIntegration {

    public static final DeferredRegister.Items TRINKET_ITEMS = DeferredRegister.createItems(Singularity_Iteration.MOD_ID);

    // 使用延迟初始化的 DeferredItem，避免在类加载时引用 Curios 类
    public static final DeferredItem<Item> TRINKET_FIREPROOF_NECKLACE = registerTrinket("item_trinket_fire_proof_necklace", 
        "com.singularity_iteration.mio_icif.integration.curios.ElectricFireProofNecklaceCurio");

    public static final DeferredItem<Item> TRINKET_ENERGY_CRYSTAL_BELT = registerTrinket("item_trinket_energy_crystal_belt", 
        "com.singularity_iteration.mio_icif.integration.curios.EnergyCrystalBeltCurio");

    public static final DeferredItem<Item> TRINKET_LAPORTON_CRYSTAL_BELT = registerTrinket("item_trinket_lapotron_crystal_belt", 
        "com.singularity_iteration.mio_icif.integration.curios.LapotronCrystalBeltCurio");

    public static final DeferredItem<Item> TRINKET_LIFE_SUPPORT_RING = registerTrinket("item_trinket_life_support_ring", 
        "com.singularity_iteration.mio_icif.integration.curios.ElectricLifeSupportRingCurio");

    public static final DeferredItem<Item> TRINKET_FLIGHT_RING = registerTrinket("item_trinket_flight_ring", 
        "com.singularity_iteration.mio_icif.integration.curios.ElectricFlightRingCurio");

    /**
     * 通过反射创建饰品实例，避免在类加载时引用 Curios 类
     */
    private static DeferredItem<Item> registerTrinket(String name, String className) {
        return TRINKET_ITEMS.register(name, () -> {
            try {
                Class<?> clazz = Class.forName(className);
                return (Item) clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create trinket: " + className, e);
            }
        });
    }

    /**
     * 检查 Curios API 是否已加载
     */
    public static boolean isCuriosLoaded() {
        return ModList.get().isLoaded("curios");
    }

    /**
     * 注册饰品 - 仅在 Curios API 已安装时调用
     */
    public static void register(IEventBus eventBus) {
        if (isCuriosLoaded()) {
            TRINKET_ITEMS.register(eventBus);
        }
    }
}