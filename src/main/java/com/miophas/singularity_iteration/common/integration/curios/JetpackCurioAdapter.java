package com.miophas.singularity_iteration.common.integration.curios;

import com.miophas.singularity_iteration.core.api.item.IJetpackTicker;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * 喷气背包的 Curios 适配器。
 * <p>对齐原 jetpack 类的 curioTick/canEquip 行为：
 * 仅允许放入 "back" 槽位，且胸甲槽未穿同款喷气背包时才执行飞行 tick。
 * 此类仅在 Curios 已安装时加载（由 {@code JetpackCuriosRegistrar} 反射调用）。
 */
public class JetpackCurioAdapter implements ICurioItem {

    private final Item jetpackItem;
    private final IJetpackTicker ticker;

    public JetpackCurioAdapter(Item jetpackItem, IJetpackTicker ticker) {
        this.jetpackItem = jetpackItem;
        this.ticker = ticker;
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player)) return;
        if (!slotContext.identifier().equals("back")) return;

        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chestStack.isEmpty() && chestStack.getItem() == jetpackItem) return;

        ticker.tickJetpack(player, stack, player.level());
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return slotContext.identifier().equals("back");
    }
}
