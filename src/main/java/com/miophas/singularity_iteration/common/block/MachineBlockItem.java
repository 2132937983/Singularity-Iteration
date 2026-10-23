package com.miophas.singularity_iteration.common.block;

import com.miophas.singularity_iteration.common.item.RecoveredPartsItem;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/**
 * Block item of the SI blocks (0.1.9.1). A machine picked up with the SI wrench keeps its
 * inventory, upgrade plugins included, in its item data. When such a machine is a crafting
 * material (for example a macerator into an advanced macerator, a solar panel into the next
 * tier), its items come back as the crafting remainder: one stack as it is, several stacks in a
 * {@link RecoveredPartsItem Recovered Parts Case}. The remainder path is the vanilla one, so it
 * works in the crafting table, the Crafter, the batch crafter and in AE2 molecular assemblers.
 */
public class MachineBlockItem extends BlockItem {
    public MachineBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return MachineContents.has(stack) || super.hasCraftingRemainingItem(stack);
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        List<ItemStack> contents = MachineContents.of(stack, null);
        if (contents.isEmpty()) return super.getCraftingRemainingItem(stack);
        return contents.size() == 1 ? contents.get(0) : RecoveredPartsItem.pack(contents);
    }

    /**
     * 0.1.10.15: a cable broken with a wrench used to drop with {@code {id, energy}} block-entity data
     * and never stacked with crafted cables. Such items lose the {@code energy} when they load (save,
     * chest, network), so stacks already in the world merge again. A painted cable keeps its colour.
     */
    @Override
    public void verifyComponentsAfterLoad(ItemStack stack) {
        super.verifyComponentsAfterLoad(stack);
        if (!(getBlock() instanceof com.miophas.singularity_iteration.common.block.wire.mio_icif_block_wire)) return;
        var data = stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        if (data == null || !data.contains("energy")) return;
        var tag = data.copyTag();
        tag.remove("energy");
        if (tag.contains("CableColor")) stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        else stack.remove(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (!MachineContents.has(stack)) return;
        List<ItemStack> contents = MachineContents.of(stack, context.registries());
        if (contents.isEmpty()) return;
        tooltip.add(Component.translatable("tooltip.mio_icif.machine_contents", contents.size()).withStyle(ChatFormatting.AQUA));
        RecoveredPartsItem.listContents(contents, tooltip, 4);
        tooltip.add(Component.translatable("tooltip.mio_icif.machine_contents.returned").withStyle(ChatFormatting.DARK_GRAY));
    }
}
