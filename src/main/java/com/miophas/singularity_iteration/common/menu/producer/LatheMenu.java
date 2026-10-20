package com.miophas.singularity_iteration.common.menu.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.blockentity.kuentity.mio_icif_lathe;
import com.miophas.singularity_iteration.core.api.item.ILatheItem;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_machine_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Lathe menu, aligned with IC2 ContainerLathe: workpiece (10, 12), tool (10, 30), output (10, 57),
 * player inventory for a 166 px high GUI. The five cut buttons arrive as menu button clicks 0-4.
 */
@SuppressWarnings("null")
public class LatheMenu extends mio_icif_machine_menu {

    // Menu slot indices (equal to the block entity's handler slots)
    public static final int TOOL_SLOT = mio_icif_lathe.TOOL_SLOT;
    public static final int LATHE_SLOT = mio_icif_lathe.LATHE_SLOT;
    public static final int OUTPUT_SLOT = mio_icif_lathe.OUTPUT_SLOT;
    public static final int SLOT_COUNT = mio_icif_lathe.TOTAL_SLOTS;

    public LatheMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public LatheMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_lathe blockEntity) {
        super(mio_icif_menus.LATHE_MENU_TYPE.get(), containerId, SLOT_COUNT,
            playerInventory,
            blockEntity != null ? blockEntity.getItemHandler() : null,
            null, 0, blockEntity);

        addCustomMoveRule(stack -> stack.getItem() instanceof ILatheItem.ILatheTool, TOOL_SLOT, TOOL_SLOT + 1);
        addCustomMoveRule(stack -> stack.getItem() instanceof ILatheItem, LATHE_SLOT, LATHE_SLOT + 1);
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, TOOL_SLOT, 10, 30) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ILatheItem.ILatheTool;
            }
        });

        this.addSlot(new SlotItemHandler(itemHandler, LATHE_SLOT, 10, 12) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ILatheItem;
            }
        });

        this.addSlot(new SlotItemHandler(itemHandler, OUTPUT_SLOT, 10, 57) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });
    }

    @Override
    protected int getInputSlotStart() {
        return TOOL_SLOT;
    }

    @Override
    protected int getInputSlotCount() {
        return 2;
    }

    @Override
    protected int getOutputSlotStart() {
        return OUTPUT_SLOT;
    }

    @Override
    protected int getOutputSlotCount() {
        return 1;
    }

    @Override
    protected int getBatterySlotIndex() {
        return -1;
    }

    @Override
    protected int getUpgradeSlotStart() {
        return -1;
    }

    @Override
    protected int getUpgradeSlotCount() {
        return 0;
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        return stack.getItem() instanceof ILatheItem.ILatheTool || stack.getItem() instanceof ILatheItem;
    }

    @Override
    public mio_icif_lathe getBlockEntity() {
        return this.blockEntity instanceof mio_icif_lathe be ? be : null;
    }

    /** IC2 onNetworkEvent: button {@code id} (0-4) cuts that position (server side). */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        var be = getBlockEntity();
        return be != null && !player.level().isClientSide() && be.process(id);
    }
}
