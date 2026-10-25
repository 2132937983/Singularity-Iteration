// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.event;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.util.mio_icif_tags;
import com.miophas.singularity_iteration.core.api.item.IWrenchItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 0.1.10.19: a wrench (an SI wrench or any {@code c:tools/wrench}) is a correct tool for SI machines and
 * cables, so breaking one with a wrench (a plain left-click, or FTB Ultimine and other area tools that
 * break each block with the player's tool) drops the machine itself with its settings.
 *
 * <p>Up to 0.1.10.18 the wrench only dismantled with a right-click. A machine block needs the correct
 * tool for drops (pickaxe), so a wrench break - and every block of an FTB Ultimine selection made with a
 * wrench - dropped nothing: the machine was gone. A pickaxe still drops the salvage of the machine
 * (its hull, see {@code MachineSalvage}); the inventory spills on every removal. The SI manual wrench
 * wears by one per machine mined ({@code mio_icif_wrench#mineBlock}; a right-click dismantle costs 10).
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class WrenchHarvest {
    private WrenchHarvest() { }

    /** Mining speed of a wrench on an SI machine or cable (a diamond pickaxe has 8). */
    public static final float WRENCH_SPEED = 8.0F;

    /** An SI wrench or a wrench of another mod. */
    public static boolean isWrench(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof IWrenchItem || stack.is(Tags.Items.TOOLS_WRENCH));
    }

    /** An SI machine or cable (the common {@code c:machine} / {@code c:cable} tags hold other mods' blocks too). */
    public static boolean wrenchable(BlockState state) {
        return (state.is(mio_icif_tags.MACHINE) || state.is(mio_icif_tags.CABLE))
            && BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals(Singularity_Iteration.MOD_ID);
    }

    @SubscribeEvent
    static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        if (!event.canHarvest() && wrenchable(event.getTargetBlock()) && isWrench(event.getEntity().getMainHandItem())) {
            event.setCanHarvest(true);
        }
    }

    @SubscribeEvent
    static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (wrenchable(event.getState()) && isWrench(event.getEntity().getMainHandItem())) {
            event.setNewSpeed(Math.max(event.getNewSpeed(), WRENCH_SPEED));
        }
    }
}
