package com.miophas.singularity_iteration.common.client.integration.jei;

import com.miophas.singularity_iteration.common.client.integration.AdvancedMinerFilters;
import com.miophas.singularity_iteration.common.client.screen.mio_icif_gui_advanced_miner_elc;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 高级采矿机过滤槽的 JEI 幽灵拖拽处理器。
 *
 * <p>允许把 JEI 物品面板里的方块直接拖到采矿机的过滤幽灵槽上，作为黑白名单过滤配置，
 * 且不会消耗物品（幽灵槽只记录标记）。与点击槽位设置等价，都会同步到服务端。
 * 0.1.11.209: 槽位换算与同步在 {@link AdvancedMinerFilters}，与 EMI 插件共用。
 */
@SuppressWarnings("null")
public class AdvancedMinerGhostHandler implements IGhostIngredientHandler<mio_icif_gui_advanced_miner_elc> {

    @Override
    public <I> List<Target<I>> getTargetsTyped(mio_icif_gui_advanced_miner_elc gui, ITypedIngredient<I> ingredient, boolean doStart) {
        // 仅接受物品（方块物品同属物品栈）
        if (ingredient.getType() != VanillaTypes.ITEM_STACK) {
            return List.of();
        }
        List<Target<I>> targets = new ArrayList<>();
        for (AdvancedMinerFilters.Target filter : AdvancedMinerFilters.targets(gui)) {
            targets.add(new Target<I>() {
                @Override
                public Rect2i getArea() {
                    return new Rect2i(filter.x(), filter.y(), 16, 16);
                }

                @Override
                public void accept(I ingredient) {
                    if (ingredient instanceof ItemStack stack) AdvancedMinerFilters.set(gui, filter, stack);
                }
            });
        }
        return targets;
    }

    @Override
    public void onComplete() {
    }
}
