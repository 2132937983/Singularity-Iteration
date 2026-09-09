package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_industrial_workbench;
import com.singularity_iteration.mio_icif.Items.Tools.mio_icif_items_tools;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_base_menu;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import java.util.List;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 工业工作台菜单类
 * 
 * 槽位索引说明：
 * 
 * Menu 中的槽位索引：
 * - 0: 主合成输出（使用 craftResult）
 * - 1-9: 3x3 合成网格（使用 craftMatrix）
 * - 10-27: 缓冲存储区（使用 itemHandler 槽位 9-26）
 * - 28: 左侧锤子工具槽（使用 itemHandler 槽位 27）
 * - 29: 左侧锤子输入槽（使用 itemHandler 槽位 28）
 * - 30: 左侧锤子输出槽（使用 itemHandler 槽位 29）
 * - 31: 右侧切割工具槽（使用 itemHandler 槽位 30）
 * - 32: 右侧切割输入槽（使用 itemHandler 槽位 31）
 * - 33: 右侧切割输出槽（使用 itemHandler 槽位 32）
 * 
 * 注意：itemHandler 的槽位布局：
 * - 0-8: 合成网格（由 craftMatrix 管理）
 * - 9-26: 缓冲存储区
 * - 27-32: 工具相关槽位
 */
public class IndustrialWorkbenchMenu extends mio_icif_base_menu {

    // === Menu 槽位索引（这是 Menu 中的全局索引）===
    public static final int MENU_SLOT_OUTPUT = 0;
    public static final int MENU_SLOT_GRID_START = 1;
    public static final int MENU_SLOT_GRID_END = 9;
    public static final int MENU_SLOT_BUFFER_START = 10;
    public static final int MENU_SLOT_BUFFER_END = 27;
    public static final int MENU_SLOT_LEFT_HAMMER_TOOL = 28;
    public static final int MENU_SLOT_LEFT_HAMMER_INPUT = 29;
    public static final int MENU_SLOT_LEFT_HAMMER_OUTPUT = 30;
    public static final int MENU_SLOT_RIGHT_CUTTER_TOOL = 31;
    public static final int MENU_SLOT_RIGHT_CUTTER_INPUT = 32;
    public static final int MENU_SLOT_RIGHT_CUTTER_OUTPUT = 33;
    
    // === itemHandler 槽位索引（这是 ItemStackHandler 的内部索引）===
    public static final int HANDLER_SLOT_GRID_START = 0;
    public static final int HANDLER_SLOT_GRID_END = 8;
    public static final int HANDLER_SLOT_BUFFER_START = 9;
    public static final int HANDLER_SLOT_BUFFER_END = 26;
    public static final int HANDLER_SLOT_LEFT_HAMMER_TOOL = 27;
    public static final int HANDLER_SLOT_LEFT_HAMMER_INPUT = 28;
    public static final int HANDLER_SLOT_LEFT_HAMMER_OUTPUT = 29;
    public static final int HANDLER_SLOT_RIGHT_CUTTER_TOOL = 30;
    public static final int HANDLER_SLOT_RIGHT_CUTTER_INPUT = 31;
    public static final int HANDLER_SLOT_RIGHT_CUTTER_OUTPUT = 32;
    
    // 玩家槽位
    public static final int PLAYER_INVENTORY_START = 34;
    public static final int PLAYER_INVENTORY_END = 61;
    public static final int PLAYER_HOTBAR_START = 61;
    public static final int PLAYER_HOTBAR_END = 70;
    
    public static final int MACHINE_SLOT_COUNT = 34;
    public static final int TOTAL_SLOTS = 70;

    private final ContainerLevelAccess access;
    private final mio_icif_industrial_workbench blockEntity;
    private final CraftingContainer craftMatrix;
    private final ResultContainer craftResult;
    private final ItemStackHandler itemHandler;
    
    // 防止 slotsChanged 递归调用的标记
    private boolean isSyncing = false;

    public IndustrialWorkbenchMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public IndustrialWorkbenchMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_industrial_workbench blockEntity) {
        super(mio_icif_menus.INDUSTRIAL_WORKBENCH_MENU_TYPE.get(), containerId);
        
        this.blockEntity = blockEntity;
        this.access = blockEntity != null 
            ? ContainerLevelAccess.create(playerInventory.player.level(), blockEntity.getBlockPos())
            : ContainerLevelAccess.create(playerInventory.player.level(), playerInventory.player.blockPosition());
        
        // 获取方块实体的物品处理器
        this.itemHandler = blockEntity != null ? blockEntity.getItemHandler() : new ItemStackHandler(33);
        
        // 初始化合成结果容器
        this.craftResult = new ResultContainer();
        
        // 创建合成网格容器 - 直接操作 itemHandler 的 0-8 号槽位
        // 避免使用 TransientCraftingContainer（独立存储），确保 JEI/EMI 配方转移时数据一致
        this.craftMatrix = new HandlerCraftingContainer(this, 3, 3, this.itemHandler, HANDLER_SLOT_GRID_START);
        
        // === 添加槽位 ===
        
        // 标记机器槽位
        markMachineSlots(MACHINE_SLOT_COUNT);
        
        // 1. 主合成输出槽 (Menu 索引 0)
        this.addSlot(new Slot(craftResult, 0, 124, 61) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                if (blockEntity == null) return;
                
                blockEntity.beginBatchUpdate();
                try {
                    ItemStack[] originalItems = new ItemStack[9];
                    for (int i = 0; i < 9; i++) {
                        ItemStack gridStack = itemHandler.getStackInSlot(HANDLER_SLOT_GRID_START + i);
                        originalItems[i] = gridStack.isEmpty() ? ItemStack.EMPTY : gridStack.copy();
                    }
                    
                    for (int i = 0; i < 9; i++) {
                        ItemStack gridStack = itemHandler.getStackInSlot(HANDLER_SLOT_GRID_START + i);
                        if (!gridStack.isEmpty()) {
                            gridStack.shrink(1);
                            itemHandler.setStackInSlot(HANDLER_SLOT_GRID_START + i, gridStack);
                        }
                    }
                    
                    for (int i = 0; i < 9; i++) {
                        ItemStack originalItem = originalItems[i];
                        int gridSlot = HANDLER_SLOT_GRID_START + i;
                        ItemStack currentGridStack = itemHandler.getStackInSlot(gridSlot);
                        
                        if (currentGridStack.isEmpty() && !originalItem.isEmpty()) {
                            refillGridSlotFromBuffer(gridSlot, originalItem);
                        }
                    }
                    
                    syncCraftMatrixFromHandler();
                    blockEntity.updateCraftingResult(craftMatrix);
                    ItemStack newResult = blockEntity.getCraftResult().getItem(0);
                    craftResult.setItem(0, newResult);
                } finally {
                    blockEntity.endBatchUpdate();
                }
                broadcastChanges();
            }
        });

        // 2. 3x3 合成网格 (Menu 索引 1-9)
        // 使用 craftMatrix 作为容器 - HandlerCraftingContainer 已直接操作 itemHandler
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                int posX = 30 + x * 18;
                int posY = 43 + y * 18;
                this.addSlot(new Slot(craftMatrix, x + y * 3, posX, posY));
            }
        }

 // 3. 缓冲存储区 2x9 (Menu 索引 10-27)
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 9; x++) {
                int handlerSlotIndex = HANDLER_SLOT_BUFFER_START + x + y * 9;
                int posX = 8 + x * 18;
                int posY = 106 + y * 18;
                this.addSlot(new SlotItemHandler(itemHandler, handlerSlotIndex, posX, posY));
            }
        }

        // 4. 左侧锤子工具槽 (Menu 索引 28)
        this.addSlot(new SlotItemHandler(itemHandler, HANDLER_SLOT_LEFT_HAMMER_TOOL, 7, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() == mio_icif_items_tools.TOOL_HAMMER.get();
            }
        });

        // 5. 左侧锤子输入槽 (Menu 索引 29)
        this.addSlot(new SlotItemHandler(itemHandler, HANDLER_SLOT_LEFT_HAMMER_INPUT, 25, 17));

        // 6. 左侧锤子输出槽 (Menu 索引 30)
        this.addSlot(new SlotItemHandler(itemHandler, HANDLER_SLOT_LEFT_HAMMER_OUTPUT, 69, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
            
            @Override
            public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
                // 消耗锻造锤耐久和输入材料
                consumeToolCrafting(HANDLER_SLOT_LEFT_HAMMER_TOOL, HANDLER_SLOT_LEFT_HAMMER_INPUT);
            }
        });

        // 7. 右侧切割工具槽 (Menu 索引 31)
        this.addSlot(new SlotItemHandler(itemHandler, HANDLER_SLOT_RIGHT_CUTTER_TOOL, 91, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() == mio_icif_items_tools.TOOL_CUTTER.get();
            }
        });

        // 8. 右侧切割输入槽 (Menu 索引 32)
        this.addSlot(new SlotItemHandler(itemHandler, HANDLER_SLOT_RIGHT_CUTTER_INPUT, 109, 17));

        // 9. 右侧切割输出槽 (Menu 索引 33)
        this.addSlot(new SlotItemHandler(itemHandler, HANDLER_SLOT_RIGHT_CUTTER_OUTPUT, 153, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
            
            @Override
            public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
                // 消耗切割剪耐久和输入材料
                IndustrialWorkbenchMenu.this.consumeToolCrafting(HANDLER_SLOT_RIGHT_CUTTER_TOOL, HANDLER_SLOT_RIGHT_CUTTER_INPUT);
            }
        });

        // 标记玩家槽位
        markPlayerSlots(37);
        
        // 10. 玩家主背包 3x9
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                int slotIndex = x + y * 9 + 9;
                int posX = 8 + x * 18;
                int posY = 146 + y * 18;
                this.addSlot(new Slot(playerInventory, slotIndex, posX, posY));
            }
        }

        // 11. 玩家快捷栏 1x9
        for (int x = 0; x < 9; x++) {
            int posX = 8 + x * 18;
            int posY = 204;
            this.addSlot(new Slot(playerInventory, x, posX, posY));
        }

        // 初始化合成结果
        slotsChanged(craftMatrix);
    }

    @Override
    public void slotsChanged(Container container) {
        if (blockEntity != null && !blockEntity.getLevel().isClientSide() && !isSyncing) {
            // HandlerCraftingContainer 已经直接操作 itemHandler，无需双向同步
            blockEntity.updateCraftingResult(craftMatrix);
            ItemStack result = blockEntity.getCraftResult().getItem(0);
            craftResult.setItem(0, result);
        }
    }

    /**
     * 从 itemHandler 同步合成网格数据到 craftMatrix
     * 仅在初始化或清空网格时使用
     */
    private void syncCraftMatrixFromHandler() {
        isSyncing = true;
        try {
            for (int i = 0; i < 9; i++) {
                // 直接设置到 itemHandler，HandlerCraftingContainer 会读取
                craftMatrix.setItem(i, itemHandler.getStackInSlot(HANDLER_SLOT_GRID_START + i));
            }
        } finally {
            isSyncing = false;
        }
    }

    private void refillGridSlotFromBuffer(int gridSlotIndex, ItemStack originalItem) {
        if (blockEntity == null) return;
        ItemStack existing = itemHandler.getStackInSlot(gridSlotIndex);
        if (!existing.isEmpty()) return;
        if (originalItem.isEmpty()) return;
        
 // 从冲区寻找与原来相同的物品进行补充
        for (int buf = HANDLER_SLOT_BUFFER_START; buf <= HANDLER_SLOT_BUFFER_END; buf++) {
            ItemStack bufStack = itemHandler.getStackInSlot(buf);
            if (bufStack.isEmpty()) continue;
            
            // 只补充与原始物品相同的物品
            if (ItemStack.isSameItemSameComponents(bufStack, originalItem)) {
                ItemStack copy = bufStack.copy();
                copy.setCount(1);
                itemHandler.setStackInSlot(gridSlotIndex, copy);
                bufStack.shrink(1);
                itemHandler.setStackInSlot(buf, bufStack);
                return;
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }

        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();

            if (index == MENU_SLOT_OUTPUT) {
                if (blockEntity == null) return ItemStack.EMPTY;
                itemstack = stackInSlot.copy();

                blockEntity.beginBatchUpdate();
                try {
                    @SuppressWarnings("unused")
                    boolean didCraft = false;
                    while (!stackInSlot.isEmpty()) {
                        if (!this.moveItemStackTo(stackInSlot, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, true)) {
                            break;
                        }
                        if (!stackInSlot.isEmpty()) {
                            slot.setByPlayer(stackInSlot);
                            break;
                        }

                        didCraft = true;

                        ItemStack[] originalItems = new ItemStack[9];
                        for (int j = 0; j < 9; j++) {
                            ItemStack gridStack = itemHandler.getStackInSlot(HANDLER_SLOT_GRID_START + j);
                            originalItems[j] = gridStack.isEmpty() ? ItemStack.EMPTY : gridStack.copy();
                        }

                        for (int i = 0; i < 9; i++) {
                            ItemStack gridStack = itemHandler.getStackInSlot(HANDLER_SLOT_GRID_START + i);
                            if (!gridStack.isEmpty()) {
                                gridStack.shrink(1);
                                itemHandler.setStackInSlot(HANDLER_SLOT_GRID_START + i, gridStack);
                            }
                        }

                        for (int i = 0; i < 9; i++) {
                            ItemStack originalItem = originalItems[i];
                            int gridSlot = HANDLER_SLOT_GRID_START + i;
                            ItemStack currentGridStack = itemHandler.getStackInSlot(gridSlot);
                            
                            if (currentGridStack.isEmpty() && !originalItem.isEmpty()) {
                                refillGridSlotFromBuffer(gridSlot, originalItem);
                            }
                        }

                        syncCraftMatrixFromHandler();
                        blockEntity.updateCraftingResult(craftMatrix);
                        ItemStack newResult = blockEntity.getCraftResult().getItem(0);
                        craftResult.setItem(0, newResult);

                        if (newResult.isEmpty()) {
                            break;
                        }

                        stackInSlot = newResult;
                    }
                } finally {
                    blockEntity.endBatchUpdate();
                }

                broadcastChanges();
                return slot.getItem();
            }

            if (index == MENU_SLOT_LEFT_HAMMER_OUTPUT || index == MENU_SLOT_RIGHT_CUTTER_OUTPUT) {
                itemstack = stackInSlot.copy();
                if (!this.moveItemStackTo(stackInSlot, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
                int toolSlot = index == MENU_SLOT_LEFT_HAMMER_OUTPUT
                    ? HANDLER_SLOT_LEFT_HAMMER_TOOL : HANDLER_SLOT_RIGHT_CUTTER_TOOL;
                int inputSlot = index == MENU_SLOT_LEFT_HAMMER_OUTPUT
                    ? HANDLER_SLOT_LEFT_HAMMER_INPUT : HANDLER_SLOT_RIGHT_CUTTER_INPUT;
                consumeToolCrafting(toolSlot, inputSlot);
                return itemstack;
            }

            itemstack = stackInSlot.copy();

            if (index >= PLAYER_INVENTORY_START) {
                if (!this.moveItemStackTo(stackInSlot, MENU_SLOT_GRID_START, MENU_SLOT_BUFFER_END + 1, false)) {
                    if (index >= PLAYER_HOTBAR_START && index < PLAYER_HOTBAR_END) {
                        if (!this.moveItemStackTo(stackInSlot, PLAYER_INVENTORY_START, PLAYER_HOTBAR_START, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (index >= PLAYER_INVENTORY_START && index < PLAYER_HOTBAR_START) {
                        if (!this.moveItemStackTo(stackInSlot, PLAYER_HOTBAR_START, PLAYER_HOTBAR_END, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            } else if (index >= MENU_SLOT_GRID_START && index <= MENU_SLOT_BUFFER_END) {
                if (!this.moveItemStackTo(stackInSlot, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stackInSlot.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stackInSlot);
        }

        return itemstack;
    }

        /**
     * 消耗工具合成所需的工具和输入材料
     * @param toolSlot 工具槽索引
     * @param inputSlot 输入槽索引
     */
    private void consumeToolCrafting(int toolSlot, int inputSlot) {
        if (blockEntity == null || blockEntity.getLevel() == null || blockEntity.getLevel().isClientSide()) return;

        blockEntity.beginBatchUpdate();
        try {
            ItemStack tool = itemHandler.getStackInSlot(toolSlot);
            ItemStack input = itemHandler.getStackInSlot(inputSlot);

            if (!input.isEmpty()) {
                input.shrink(1);
                itemHandler.setStackInSlot(inputSlot, input);
            }

            if (!tool.isEmpty()) {
                if (tool.hasCraftingRemainingItem()) {
                    ItemStack remainingTool = tool.getCraftingRemainingItem();
                    itemHandler.setStackInSlot(toolSlot, remainingTool);
                } else if (tool.isDamageableItem()) {
                    tool.setDamageValue(tool.getDamageValue() + 1);
                    if (tool.getDamageValue() >= tool.getMaxDamage()) {
                        itemHandler.setStackInSlot(toolSlot, ItemStack.EMPTY);
                    } else {
                        itemHandler.setStackInSlot(toolSlot, tool);
                    }
                }
            }

            if (toolSlot == HANDLER_SLOT_LEFT_HAMMER_TOOL) {
                blockEntity.updateLeftCraftingResult();
            } else if (toolSlot == HANDLER_SLOT_RIGHT_CUTTER_TOOL) {
                blockEntity.updateRightCraftingResult();
            }
        } finally {
            blockEntity.endBatchUpdate();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, pos) ->
            level.getBlockState(pos).getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_industrial_workbench
                && player.distanceToSqr((double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5) <= 64.0,
            true);
    }

    public mio_icif_industrial_workbench getBlockEntity() {
        return blockEntity;
    }

    public CraftingContainer getCraftMatrix() {
        return craftMatrix;
    }

    public ResultContainer getCraftResult() {
        return craftResult;
    }

    @Override
    protected boolean isBattery(ItemStack stack) {
        return false;
    }

    /**
     * ??3x3?????????????????
     */
    public void clearCraftingGrid(Player player) {
        if (blockEntity == null) return;
        blockEntity.clearCraftingGrid(player);
        syncCraftMatrixFromHandler();
        blockEntity.updateCraftingResult(craftMatrix);
        craftResult.setItem(0, blockEntity.getCraftResult().getItem(0));
        broadcastChanges();
    }

    /**
     * 自定义 CraftingContainer，直接操作 ItemStackHandler 的指定槽位范围
     * 解决 JEI/EMI 配方转移时 TransientCraftingContainer 与 itemHandler 数据不一致的问题
     */
    public static class HandlerCraftingContainer implements CraftingContainer {
        private final AbstractContainerMenu menu;
        private final int width;
        private final int height;
        private final ItemStackHandler handler;
        private final int handlerOffset;

        public HandlerCraftingContainer(AbstractContainerMenu menu, int width, int height, ItemStackHandler handler, int handlerOffset) {
            this.menu = menu;
            this.width = width;
            this.height = height;
            this.handler = handler;
            this.handlerOffset = handlerOffset;
        }

        @Override
        public int getContainerSize() {
            return this.width * this.height;
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < getContainerSize(); i++) {
                if (!getItem(i).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return slot >= getContainerSize() ? ItemStack.EMPTY : this.handler.getStackInSlot(this.handlerOffset + slot);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack stack = getItem(slot);
            if (!stack.isEmpty()) {
                this.handler.setStackInSlot(this.handlerOffset + slot, ItemStack.EMPTY);
            }
            return stack;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack stack = this.handler.getStackInSlot(this.handlerOffset + slot);
            if (!stack.isEmpty()) {
                ItemStack result = stack.split(amount);
                this.handler.setStackInSlot(this.handlerOffset + slot, stack);
                this.menu.slotsChanged(this);
                return result;
            }
            return ItemStack.EMPTY;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            this.handler.setStackInSlot(this.handlerOffset + slot, stack);
            this.menu.slotsChanged(this);
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < getContainerSize(); i++) {
                this.handler.setStackInSlot(this.handlerOffset + i, ItemStack.EMPTY);
            }
        }

        @Override
        public int getHeight() {
            return this.height;
        }

        @Override
        public int getWidth() {
            return this.width;
        }

        @Override
        public List<ItemStack> getItems() {
            List<ItemStack> list = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
            for (int i = 0; i < getContainerSize(); i++) {
                list.set(i, getItem(i));
            }
            return list;
        }

        @Override
        public void fillStackedContents(StackedContents contents) {
            for (int i = 0; i < getContainerSize(); i++) {
                contents.accountSimpleStack(getItem(i));
            }
        }
    }
}