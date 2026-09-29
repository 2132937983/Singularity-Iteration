package com.miophas.singularity_iteration.common.integration.curios;

import com.miophas.singularity_iteration.common.item.armor.mio_icif_items_armors;
import com.miophas.singularity_iteration.core.api.item.IJetpackTicker;
import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * 喷气背包 Curios 行为注册器。
 * <p>仅在 Curios 已安装时由 {@code CuriosIntegration} 通过反射加载并调用，
 * 因此本类可直接引用 Curios API。
 * 在 common setup 阶段为喷气背包物品附加 {@link JetpackCurioAdapter}，
 * 使其可作为 "back" 槽位饰品工作，而物品类本身不依赖 Curios。
 */
public class JetpackCuriosRegistrar {

    public static void register() {
        registerJetpackCurio(mio_icif_items_armors.ARMOR_JETPACK_ELECTRIC.get());
        registerJetpackCurio(mio_icif_items_armors.ARMOR_ADVANCED_JETPACK.get());
    }

    private static void registerJetpackCurio(Item item) {
        if (item instanceof IJetpackTicker ticker) {
            CuriosApi.registerCurio(item, new JetpackCurioAdapter(item, ticker));
        }
    }
}
