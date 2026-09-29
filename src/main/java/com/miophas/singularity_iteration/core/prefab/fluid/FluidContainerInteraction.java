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
 * 手持流体容器右击机器时的通用储罐交互。
 *
 * <p>语义对齐原版 IC2 {@code ic2.core.item.ItemFluidCell#interactWithTank}：
 * <ul>
 *   <li>通过流体能力（{@code Capabilities.FluidHandler.BLOCK} / {@code .ITEM}）传输，
 *       不关心容器具体种类 —— 水桶、水/蒸馏水单元、其它模组的流体容器走同一条路径。</li>
 *   <li><b>先</b>尝试"容器 → 储罐"（对齐 Forge {@code FluidUtil.tryFluidTransfer} 的
 *       drain-source-first 顺序），失败再尝试"储罐 → 容器"。因此空容器天然表现为抽取。</li>
 *   <li><b>没有蹲下开关</b>：IC2 原版就是双向自动判定，方向由容器/储罐各自能否接受决定。</li>
 *   <li>支持整叠容器：一次右击循环处理，直到无法继续或手上用完。</li>
 * </ul>
 *
 * <p>储罐的方向角色（只进 / 只出 / 双向）应由传入的 {@link IFluidHandler}
 * 结构性保证（见 {@link FluidTankGroup}），本类不做额外索引判断。
 */
public final class FluidContainerInteraction {

    private FluidContainerInteraction() { }

    /**
     * 让玩家手上的流体容器与储罐做一次双向传输。
     *
     * @param player      玩家（服务端）
     * @param hand        持容器的手
     * @param tankHandler 机器暴露的储罐处理器，可为 null
     * @return 是否真的发生了流体转移
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

    /** 容器 → 储罐。按储罐实际可接收量精确转移，避免消耗多于被接收的流体。 */
    private static boolean tryFillTank(IFluidHandlerItem item, IFluidHandler tank) {
        FluidStack available = item.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        if (available.isEmpty()) return false;

        int accepted = tank.fill(available, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return false;

        FluidStack drained = item.drain(available.copyWithAmount(accepted), IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) return false;

        return tank.fill(drained, IFluidHandler.FluidAction.EXECUTE) == drained.getAmount();
    }

    /** 储罐 → 容器。按容器实际可容纳量精确转移，避免抽走多于被接收的流体。 */
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
