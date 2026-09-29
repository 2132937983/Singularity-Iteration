package com.miophas.singularity_iteration.common.item.reactor;

import com.miophas.singularity_iteration.core.api.reactor.ReactorComponentType;
import net.minecraft.world.item.ItemStack;

/**
 * 加热元件（对齐 IC2 1.12.2 {@code ItemReactorHeatpack(1000, 1)}）。
 *
 * <p>行为：
 * <ul>
 *   <li>只要外壳堆温低于 1000 HU，就向四邻可储热的元件注入热量（每个元件每循环 1 HU）；
 *       用于 MOX 增殖等需要主动抬升堆温的场景。</li>
 *   <li>本体不储热、不参与换热，也不影响发电。</li>
 *   <li>爆炸威力加成 +0.1（IC2 {@code influenceExplosion = size / 10}）。</li>
 * </ul>
 * 具体数值与注入逻辑在 {@code ReactorCycle.Profile.heatpack} 与 {@code ReactorInventory} 中维护。
 */
@SuppressWarnings("null")
public class mio_icif_heatpack extends mio_icif_reactor {

    public mio_icif_heatpack(Properties properties) {
        super(properties, 0, ReactorComponentType.OTHER, false, true);
    }

    /** 加热元件没有耐久概念，不显示耐久条。 */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }
}
