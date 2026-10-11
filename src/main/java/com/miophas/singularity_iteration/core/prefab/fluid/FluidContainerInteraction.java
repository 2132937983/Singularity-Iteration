package com.miophas.singularity_iteration.core.prefab.fluid;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.Nullable;

/**
 * General tank interaction when a player right-clicks a machine with a fluid container in hand.
 *
 * <p>The behavior is the same as the original IC2 {@code ic2.core.item.ItemFluidCell#interactWithTank}:
 * <ul>
 *   <li>The transfer uses the fluid capabilities ({@code Capabilities.FluidHandler.BLOCK} / {@code .ITEM}).
 *       The container kind is not important. Buckets, water and distilled water cells, and fluid containers
 *       of other mods all use the same path.</li>
 *   <li>It <b>first</b> tries "container to tank" (the same drain-source-first order as Forge
 *       {@code FluidUtil.tryFluidTransfer}). If that fails, it tries "tank to container".
 *       Thus an empty container always drains the tank.</li>
 *   <li><b>There is no sneak toggle</b>: the original IC2 also selects the direction automatically.
 *       The direction depends on what the container and the tank can accept.</li>
 *   <li>It accepts a full stack of containers: one right-click repeats the transfer until it cannot continue
 *       or the hand is empty.</li>
 * </ul>
 *
 * <p>The {@link IFluidHandler} that you give must enforce the direction roles of the tanks
 * (input only, output only, both) by its structure (see {@link FluidTankGroup}).
 * This class does no additional index checks.
 */
public final class FluidContainerInteraction {

    private FluidContainerInteraction() { }

    /**
     * Does one two-way transfer between the fluid container in the player's hand and the tank.
     * Call it on the server side.
     *
     * @param player      the player (server side)
     * @param hand        the hand that holds the container
     * @param tankHandler the tank handler that the machine exposes; can be null (then the method returns false)
     * @return true if fluid was really transferred
     */
    public static boolean interact(Player player, InteractionHand hand, @Nullable IFluidHandler tankHandler) {
        if (tankHandler == null) return false;

        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) return false;

        boolean single = stack.getCount() == 1;
        boolean changed = false;

        while (true) {
            ItemStack working = single ? stack : stack.copyWithCount(1);
            IFluidHandlerItem itemHandler = working.getCapability(Capabilities.FluidHandler.ITEM);
            if (itemHandler == null) break;

            if (!tryFillTank(itemHandler, tankHandler) && !tryDrainFromTank(itemHandler, tankHandler)) break;

            ItemStack container = itemHandler.getContainer();
            if (single) {
                player.setItemInHand(hand, container);
                return true;
            }

            stack.shrink(1);
            changed = true;
            if (!container.isEmpty() && !player.getInventory().add(container)) {
                player.drop(container, false);
            }
            if (stack.isEmpty()) break;
        }

        return changed;
    }

    /** Container to tank. Transfers exactly the amount that the tank can accept, so the container does not lose more fluid than the tank receives. */
    private static boolean tryFillTank(IFluidHandlerItem item, IFluidHandler tank) {
        FluidStack available = item.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        if (available.isEmpty()) return false;

        int accepted = tank.fill(available, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return false;

        FluidStack drained = item.drain(available.copyWithAmount(accepted), IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) return false;

        return tank.fill(drained, IFluidHandler.FluidAction.EXECUTE) == drained.getAmount();
    }

    /** Tank to container. Transfers exactly the amount that the container can hold, so the tank does not lose more fluid than the container receives. */
    private static boolean tryDrainFromTank(IFluidHandlerItem item, IFluidHandler tank) {
        FluidStack available = tank.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        if (available.isEmpty()) return false;

        int accepted = item.fill(available, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return false;

        FluidStack drained = tank.drain(available.copyWithAmount(accepted), IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) return false;

        return item.fill(drained, IFluidHandler.FluidAction.EXECUTE) > 0;
    }
}
