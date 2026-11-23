// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.builder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 0.1.11.187: the Infinite Armory Material Interface, a creative block (no recipe, as the Infinite Generator): it has every
 * building material in any amount for the Blueprint Builders within {@link BlueprintBuilderBlockEntity#INTERFACE_RANGE}
 * blocks. No slots, no screen, no item handler (pipes and hoppers neither fill nor empty it); what the drones bring back
 * is gone. A plain copy of the item is given (no components), as the normal interface gives only plain stacks.
 */
public class InfiniteMaterialInterfaceBlockEntity extends BlockEntity implements MaterialSource {
    public InfiniteMaterialInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(BuilderRegistry.INFINITE_MATERIAL_INTERFACE_ENTITY.get(), pos, state);
    }

    @Override
    public long count(Item item) { return item == null || item == Items.AIR ? 0 : Long.MAX_VALUE; }

    @Override
    public ItemStack extract(Item item, int max) {
        return item == null || item == Items.AIR || max <= 0 ? ItemStack.EMPTY : new ItemStack(item, max);
    }

    @Override
    public ItemStack insert(ItemStack stack) { return ItemStack.EMPTY; }
}
