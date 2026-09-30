package com.miophas.singularity_iteration.core.api.item;

import net.minecraft.world.item.ItemStack;

/**
 * 模式型装备特性接口。
 *
 * <p>当 {@link ArmorFeatureInfo#isMode()} 为 {@code true} 时，该特性由"当前模式"描述，
 * 通过本接口切换到下一个模式（例如喷气背包的 悬停 / 飞行 切换）。
 *
 * <p>声明方式：
 * <ul>
 *   <li>物品实现 {@link IElectricArmorItem#getFeatures(ItemStack)} 并返回
 *       {@code isMode=true} 的 {@link ArmorFeatureInfo}（携带当前模式名称）；</li>
 *   <li>同时实现本接口以提供切换逻辑。</li>
 * </ul>
 *
 * <p>{@link IJetpackItem} 已继承本接口并提供默认实现，因此喷气背包无需额外代码。
 * GUI / 命令 / Tooltip 面板都会通过本接口统一驱动模式切换，不再识别具体物品类型。
 */
public interface IArmorModeFeatureItem {

    /**
     * 将指定模式特性切换到下一个模式。
     *
     * <p>实现应就地修改 {@code stack} 的模式状态；随后的
     * {@link IElectricArmorItem#getFeatures(ItemStack)} 应能反映新模式名称。
     *
     * @param stack      物品堆
     * @param featureKey 模式特性键
     * @return 是否发生了切换
     */
    boolean cycleMode(ItemStack stack, String featureKey);
}
