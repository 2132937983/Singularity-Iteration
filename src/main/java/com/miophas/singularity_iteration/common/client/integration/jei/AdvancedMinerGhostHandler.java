package com.miophas.singularity_iteration.common.client.integration.jei;

import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_advanced_miner_elc;
import com.miophas.singularity_iteration.common.client.screen.mio_icif_gui_advanced_miner_elc;
import com.miophas.singularity_iteration.common.menu.producer.AdvancedMinerElcMenu;
import com.miophas.singularity_iteration.common.menu.slot.PhantomSlot;
import com.miophas.singularity_iteration.common.network.mio_icif_Network;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 高级采矿机过滤槽的 JEI 幽灵拖拽处理器。
 *
 * <p>允许把 JEI 物品面板里的方块直接拖到采矿机的过滤幽灵槽上，作为黑白名单过滤配置，
 * 且不会消耗物品（幽灵槽只记录标记）。与点击槽位设置等价，都会同步到服务端。
 */
@SuppressWarnings("null")
public class AdvancedMinerGhostHandler implements IGhostIngredientHandler<mio_icif_gui_advanced_miner_elc> {

    @Override
    public <I> List<Target<I>> getTargetsTyped(mio_icif_gui_advanced_miner_elc gui, ITypedIngredient<I> ingredient, boolean doStart) {
        // 仅接受物品（方块物品同属物品栈）
        if (ingredient.getType() != VanillaTypes.ITEM_STACK) {
            return List.of();
        }

        int guiLeft = gui.getGuiLeft();
        int guiTop = gui.getGuiTop();

        List<Target<I>> targets = new ArrayList<>();
        for (Slot slot : gui.getMenu().slots) {
            if (!(slot instanceof PhantomSlot)) {
                continue;
            }
            int filterIndex = filterIndexFromSlot(slot);
            if (filterIndex < 0) {
                continue;
            }
            targets.add(new Target<I>() {
                @Override
                public Rect2i getArea() {
                    return new Rect2i(guiLeft + slot.x, guiTop + slot.y, 16, 16);
                }

                @Override
                public void accept(I ingredient) {
                    if (!(ingredient instanceof ItemStack stack) || stack.isEmpty()) {
                        return;
                    }
                    ItemStack filter = stack.copyWithCount(1);
                    // 客户端立即刷新显示（幽灵槽只存标记，不消耗物品）
                    slot.set(filter);
                    // 同步到服务端
                    mio_icif_advanced_miner_elc miner = gui.getMenu().getMinerBlockEntity();
                    if (miner != null) {
                        mio_icif_Network.sendMinerFilter(miner.getBlockPos(), filterIndex, filter);
                    }
                }
            });
        }
        return targets;
    }

    @Override
    public void onComplete() {
    }

    /**
     * 根据幽灵槽在 GUI 中的固定网格坐标换算过滤索引（0..FILTER_COUNT-1）。
     * 过滤槽按 {@code 18} 像素网格从 (FILTER_START_X, FILTER_START_Y) 排列，因此可由坐标反推，
     * 比依赖 {@code Slot.index}（会被 {@code addSlot} 覆写为列表位置）更可靠。
     */
    private static int filterIndexFromSlot(Slot slot) {
        int col = (slot.x - AdvancedMinerElcMenu.FILTER_START_X) / 18;
        int row = (slot.y - AdvancedMinerElcMenu.FILTER_START_Y) / 18;
        if (col < 0 || col >= AdvancedMinerElcMenu.FILTER_COLS
                || row < 0 || row >= AdvancedMinerElcMenu.FILTER_ROWS) {
            return -1;
        }
        return row * AdvancedMinerElcMenu.FILTER_COLS + col;
    }
}
