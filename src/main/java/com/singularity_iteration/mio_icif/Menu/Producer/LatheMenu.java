package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.mio_icif_lathe;
import com.singularity_iteration.mio_icif.api.item.ILatheItem;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 车床 Menu
 */
@SuppressWarnings("null")
public class LatheMenu extends mio_icif_machine_menu {

    public static final int INPUT_SLOT = 0;  // 车刀
    public static final int TOOL_SLOT = 1;    // 加工�?
    public static final int OUTPUT_SLOT = 2;
    public static final int SLOT_COUNT = 3;

    private final ContainerLevelAccess access;

    public LatheMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public LatheMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_lathe blockEntity) {
        super(mio_icif_menus.LATHE_MENU_TYPE.get(), containerId, SLOT_COUNT,
            playerInventory,
            blockEntity != null ? blockEntity.getItemHandler() : null,
            null, 0, blockEntity);

        this.access = ContainerLevelAccess.create(playerInventory.player.level(),
            blockEntity != null ? blockEntity.getBlockPos() : playerInventory.player.blockPosition());

        // 注册自定义快速移动规则?
        addCustomMoveRule(
            stack -> stack.getItem() instanceof ILatheItem.ILatheTool,
            INPUT_SLOT, INPUT_SLOT + 1);
        addCustomMoveRule(
            stack -> stack.getItem() instanceof ILatheItem,
            TOOL_SLOT, TOOL_SLOT + 1);
    }

    @Override
    protected void addMachineSlots() {
        // 车刀�?(0) - 位置 (10, 30)
        this.addSlot(new SlotItemHandler(itemHandler, 0, 10, 30) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ILatheItem.ILatheTool;
            }
        });

        // 加工件槽 (1) - 位置 (10, 12)
        this.addSlot(new SlotItemHandler(itemHandler, 1, 10, 12) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ILatheItem;
            }
        });

        // 输出�?(2) - 位置 (10, 57)
        this.addSlot(new SlotItemHandler(itemHandler, 2, 10, 57) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });
    }

    @Override
    protected int getInputSlotStart() {
        return INPUT_SLOT;
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

    /**
     * 获取 KU 缓冲像素（供 GUI 渲染�?
     */
    public int getKUBufferPixels() {
        var be = getBlockEntity();
        if (be == null) return 0;
        return be.kUBuffer;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        var be = getBlockEntity();
        if (id >= 0 && id < 5 && be != null) {
            return be.process(id);
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, pos) ->
            level.getBlockState(pos).getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_lathe
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0, true);
    }
}

