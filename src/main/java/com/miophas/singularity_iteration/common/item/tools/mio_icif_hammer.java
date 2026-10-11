package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.core.api.item.IHammerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

@SuppressWarnings("null")
public class mio_icif_hammer extends Item implements IHammerItem {

    public mio_icif_hammer(Properties properties) {
        super(properties.durability(250));
    }

    /**
     * Original IC2 design: two forge hammers cannot be combined in crafting to repair them.
     * Return false so that the vanilla RepairItemRecipe does not match.
     * This prevents item duplication during shift-click crafting.
     */
    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }

    /**
     * Original IC2 design: the forge hammer stays in the grid after crafting and loses 1 durability.
     */
    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    /**
     * This follows the original IC2 design. After crafting, the forge hammer returns a copy of itself. The copy has 1 less durability.
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

