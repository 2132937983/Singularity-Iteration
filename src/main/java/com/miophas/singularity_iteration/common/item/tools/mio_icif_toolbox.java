package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.core.api.util.IBoxable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 工具箱物品。
 * 对齐 IC2 1.12.2 的 ItemToolbox / HandHeldToolbox / SlotBoxable：
 * 右键打开手持容器 GUI，只有实现 {@link IBoxable} 的工具类物品可以收纳。
 * 数据保存在物品的 CUSTOM_DATA NBT 中（与 {@link AbstractElectricTool} 的手持物品栏格式一致）。
 */
public class mio_icif_toolbox extends Item {

    /** 对齐 IC2: ItemToolbox 构造 HandHeldToolbox(player, stack, 9)，共 9 格 */
    public static final int INVENTORY_SIZE = 9;

    public mio_icif_toolbox(Properties properties) {
        super(properties.stacksTo(1));
    }

    /**
     * 对齐 IC2: SlotBoxable.mayPlace / HandHeldToolbox.isItemValid
     * 通过 ItemWrapper.canBeStoredInToolbox -> item instanceof IBoxable 判断。
     * 工具箱物品本身未实现 IBoxable，因此无法收纳工具箱（含防自嵌套）。
     */
    public static boolean canBeStored(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() instanceof IBoxable boxable && boxable.canBeStoredInToolbox(stack);
    }

    /** 对齐 IC2: ItemToolbox.use - 打开手持容器 GUI */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.openMenu(new Provider(stack), buf -> buf.writeVarInt(INVENTORY_SIZE));
        }
        return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
    }

    public static class Provider implements MenuProvider {
        private final ItemStack containerStack;

        public Provider(ItemStack containerStack) {
            this.containerStack = containerStack;
        }

        @Override
        public Component getDisplayName() {
            return containerStack.getHoverName();
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new com.miophas.singularity_iteration.common.menu.tool.mio_icif_toolbox_menu(
                containerId, playerInventory, containerStack, INVENTORY_SIZE);
        }
    }
}
