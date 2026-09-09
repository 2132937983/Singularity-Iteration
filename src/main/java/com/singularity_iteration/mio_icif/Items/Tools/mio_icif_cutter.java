package com.singularity_iteration.mio_icif.Items.Tools;

import com.singularity_iteration.mio_icif.api.item.ICutterItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

@SuppressWarnings("null")
public class mio_icif_cutter extends Item implements ICutterItem {

    public mio_icif_cutter(Properties properties) {
        super(properties.durability(59));
    }

    /**
     * 原版IC2设计：切割剪不可通过两个合成修复�?
     * 返回false以防止原版RepairItemRecipe匹配方?
     * 避免按Shift快速合成时物品复制的问题�?
     */
    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }

    /**
     * 原版IC2设计：切割剪在合成中使用时保留并扣除1点耐久�?
     */
    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    /**
     * 原版IC2设计：切割剪在合成后返回耐久-1的副本身?
     */
    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        ItemStack ret = stack.copy();
        ret.setDamageValue(ret.getDamageValue() + 1);
        if (ret.getDamageValue() >= ret.getMaxDamage()) {
            return ItemStack.EMPTY;
        }
        return ret;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }
}

