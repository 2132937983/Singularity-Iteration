package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.core.api.item.ICutterItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

@SuppressWarnings("null")
public class mio_icif_cutter extends Item implements ICutterItem {

    public mio_icif_cutter(Properties properties) {
        super(properties.durability(59));
    }

    /**
     * Original IC2 design: two cutters cannot combine in crafting for repair.
     * Returns false so the vanilla RepairItemRecipe does not match.
     * This prevents item duplication with Shift quick crafting.
     */
    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }

    /**
     * Original IC2 design: the cutter stays in the grid after crafting and loses 1 durability.
     */
    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    /**
     * Original IC2 design: after crafting, the cutter returns a copy of itself with 1 more point of damage.
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

    /**
     * 0.1.8.0: Shift + use on a cable strips one insulation layer. Vanilla skips the block
     * interaction when a sneaking player holds an item, unless the item returns true here.
     * Before this fix, the strip code of the cable block was never reached.
     */
    @Override
    public boolean doesSneakBypassUse(ItemStack stack, net.minecraft.world.level.LevelReader level,
                                      net.minecraft.core.BlockPos pos, net.minecraft.world.entity.player.Player player) {
        return level.getBlockState(pos).getBlock() instanceof com.miophas.singularity_iteration.common.block.wire.mio_icif_block_wire;
    }
}

