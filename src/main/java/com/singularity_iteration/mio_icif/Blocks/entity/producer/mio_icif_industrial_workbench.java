package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.api.item.IWorkbench;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Items.Tools.mio_icif_items_tools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * 工业工作台方块实体类
 * 功能：
 * 1. 3x3合成网格
 * 2. 左侧工具槽（锻造锤+材料）合成
 * 3. 右侧工具槽（剪线钳+材料）合成
 * 4. 18格存储缓冲区
 */
public class mio_icif_industrial_workbench extends BlockEntity implements MenuProvider, IWorkbench {

    // 槽位定义
    public static final int CRAFTING_GRID_SIZE = 9;  // 3x3合成网格
    public static final int CRAFTING_STORAGE_SIZE = 18; // 18格存储
    public static final int LEFT_TOOL_SLOT = 27;      // 左侧工具槽（锻造锤）
    public static final int LEFT_INPUT_SLOT = 28;     // 左侧输入槽
    public static final int LEFT_OUTPUT_SLOT = 29;    // 左侧输出槽
    public static final int RIGHT_TOOL_SLOT = 30;     // 右侧工具槽（剪线钳）
    public static final int RIGHT_INPUT_SLOT = 31;    // 右侧输入槽
    public static final int RIGHT_OUTPUT_SLOT = 32;   // 右侧输出槽
    public static final int TOTAL_SLOTS = 33;

    // 合成结果容器
    private final ResultContainer craftResult = new ResultContainer();

    private boolean batchUpdating = false;
    private boolean pendingLeftUpdate = false;
    private boolean pendingRightUpdate = false;

    public void beginBatchUpdate() {
        batchUpdating = true;
        pendingLeftUpdate = false;
        pendingRightUpdate = false;
    }

    public void endBatchUpdate() {
        batchUpdating = false;
        if (pendingLeftUpdate) {
            updateLeftCraftingResult();
            pendingLeftUpdate = false;
        }
        if (pendingRightUpdate) {
            updateRightCraftingResult();
            pendingRightUpdate = false;
        }
        setChanged();
    }
    
    // 物品处理器
    private final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            if (batchUpdating) {
                if (slot == LEFT_TOOL_SLOT || slot == LEFT_INPUT_SLOT) {
                    pendingLeftUpdate = true;
                } else if (slot == RIGHT_TOOL_SLOT || slot == RIGHT_INPUT_SLOT) {
                    pendingRightUpdate = true;
                }
                return;
            }
            setChanged();
            if (slot == LEFT_TOOL_SLOT || slot == LEFT_INPUT_SLOT) {
                updateLeftCraftingResult();
            } else if (slot == RIGHT_TOOL_SLOT || slot == RIGHT_INPUT_SLOT) {
                updateRightCraftingResult();
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == LEFT_TOOL_SLOT) {
                // 左侧工具槽只能放锻造锤
                return stack.getItem() == mio_icif_items_tools.TOOL_HAMMER.get();
            }
            if (slot == RIGHT_TOOL_SLOT) {
                // 右侧工具槽只能放剪线钳
                return stack.getItem() == mio_icif_items_tools.TOOL_CUTTER.get();
            }
            return true;
        }
    };

    public mio_icif_industrial_workbench(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.INDUSTRIAL_WORKBENCH_ENTITY_TYPE.get());
    }

    public mio_icif_industrial_workbench(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(type, pos, state);
    }

    /**
     * 更新3x3合成结果
     * 注意：此方法需要在菜单中调用，并传入正确的合成容器
     */
    public void updateCraftingResult(net.minecraft.world.inventory.CraftingContainer craftMatrix) {
        if (level == null || level.isClientSide()) return;

        Optional<RecipeHolder<CraftingRecipe>> recipe = level.getRecipeManager()
            .getRecipeFor(RecipeType.CRAFTING, craftMatrix.asCraftInput(), level);

        if (recipe.isPresent()) {
            ItemStack result = recipe.get().value().assemble(craftMatrix.asCraftInput(), level.registryAccess());
            craftResult.setItem(0, result);
        } else {
            craftResult.setItem(0, ItemStack.EMPTY);
        }
    }

    /**
     * 更新左侧工具合成结果（锻造锤+材料）
     */
    public void updateLeftCraftingResult() {
        if (level == null || level.isClientSide()) return;

        ItemStack tool = itemHandler.getStackInSlot(LEFT_TOOL_SLOT);
        ItemStack input = itemHandler.getStackInSlot(LEFT_INPUT_SLOT);

        if (tool.isEmpty() || input.isEmpty()) {
            itemHandler.setStackInSlot(LEFT_OUTPUT_SLOT, ItemStack.EMPTY);
            return;
        }

        // 检查是否有锻造锤配方
        ItemStack result = getToolCraftingResult(tool, input);
        itemHandler.setStackInSlot(LEFT_OUTPUT_SLOT, result);
    }

    /**
     * 更新右侧工具合成结果（剪线钳+材料）
     */
    public void updateRightCraftingResult() {
        if (level == null || level.isClientSide()) return;

        ItemStack tool = itemHandler.getStackInSlot(RIGHT_TOOL_SLOT);
        ItemStack input = itemHandler.getStackInSlot(RIGHT_INPUT_SLOT);

        if (tool.isEmpty() || input.isEmpty()) {
            itemHandler.setStackInSlot(RIGHT_OUTPUT_SLOT, ItemStack.EMPTY);
            return;
        }

        // 检查是否有剪线钳配方
        ItemStack result = getToolCraftingResult(tool, input);
        itemHandler.setStackInSlot(RIGHT_OUTPUT_SLOT, result);
    }

    /**
     * 获取工具合成结果
     * 使用 Minecraft 配方系统查询工具和材料的合成配方
     */
    private ItemStack getToolCraftingResult(ItemStack tool, ItemStack input) {
        if (level == null) return ItemStack.EMPTY;
        
        // 创建一个2x1 的临时合成容器（工具 + 输入材料）
        TransientCraftingContainer tempCrafting = new TransientCraftingContainer(
            new AbstractContainerMenu(null, -1) {
                @Override
                public ItemStack quickMoveStack(Player player, int index) {
                    return ItemStack.EMPTY;
                }
                @Override
                public boolean stillValid(Player player) {
                    return true;
                }
            },
            2, 1  // 2?行的合成网格
        );
        
        // 放置工具到第一个槽位，输入材料到第二个槽位
        tempCrafting.setItem(0, tool.copy());
        tempCrafting.setItem(1, input.copy());
        
        // 查询无序配方
        Optional<RecipeHolder<CraftingRecipe>> recipe = level.getRecipeManager()
            .getRecipeFor(RecipeType.CRAFTING, tempCrafting.asCraftInput(), level);
        
        if (recipe.isPresent()) {
            return recipe.get().value().assemble(tempCrafting.asCraftInput(), level.registryAccess());
        }
        
        return ItemStack.EMPTY;
    }

    /**
     * 执行3x3合成
     * 注意：此方法应由菜单调用，在调用前应确保craftResult已更新
     */
    public void doCrafting(Player player) {
        if (level == null || level.isClientSide()) return;

        ItemStack result = craftResult.getItem(0);
        if (result.isEmpty()) return;

        // 消耗合成材料
        for (int i = 0; i < CRAFTING_GRID_SIZE; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                stack.shrink(1);
            }
        }

        setChanged();
    }

    /**
     * 清空合成网格，将物品移入存储区或玩家背包
     */
    public void clearCraftingGrid(Player player) {
        for (int i = 0; i < CRAFTING_GRID_SIZE; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                // 先尝试放入存储区
                boolean moved = false;
                for (int j = CRAFTING_GRID_SIZE; j < CRAFTING_GRID_SIZE + CRAFTING_STORAGE_SIZE; j++) {
                    ItemStack storageStack = itemHandler.getStackInSlot(j);
                    if (storageStack.isEmpty()) {
                        itemHandler.setStackInSlot(j, stack.copy());
                        itemHandler.setStackInSlot(i, ItemStack.EMPTY);
                        moved = true;
                        break;
                    } else if (ItemStack.isSameItemSameComponents(storageStack, stack) && 
                               storageStack.getCount() + stack.getCount() <= storageStack.getMaxStackSize()) {
                        storageStack.grow(stack.getCount());
                        itemHandler.setStackInSlot(i, ItemStack.EMPTY);
                        moved = true;
                        break;
                    }
                }
                
                // 如果存储区满了，放入玩家背包
                if (!moved) {
                    if (!player.getInventory().add(stack)) {
                        player.drop(stack, false);
                    }
                    itemHandler.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        }
        setChanged();
    }

    /**
     * 执行左侧工具合成
     */
    public void doLeftCrafting(Player player) {
        if (level == null || level.isClientSide()) return;

        ItemStack result = itemHandler.getStackInSlot(LEFT_OUTPUT_SLOT);
        if (result.isEmpty()) return;

        // 消耗材料和工具耐久
        ItemStack tool = itemHandler.getStackInSlot(LEFT_TOOL_SLOT);
        ItemStack input = itemHandler.getStackInSlot(LEFT_INPUT_SLOT);
        
        input.shrink(1);
        if (tool.isDamageableItem()) {
            tool.setDamageValue(tool.getDamageValue() + 1);
            if (tool.getDamageValue() >= tool.getMaxDamage()) {
                itemHandler.setStackInSlot(LEFT_TOOL_SLOT, ItemStack.EMPTY);
            }
        }

        // 给予结果
        if (!player.getInventory().add(result.copy())) {
            player.drop(result.copy(), false);
        }

        updateLeftCraftingResult();
        setChanged();
    }

    /**
     * 执行右侧工具合成
     */
    public void doRightCrafting(Player player) {
        if (level == null || level.isClientSide()) return;

        ItemStack result = itemHandler.getStackInSlot(RIGHT_OUTPUT_SLOT);
        if (result.isEmpty()) return;

        // 消耗材料和工具耐久
        ItemStack tool = itemHandler.getStackInSlot(RIGHT_TOOL_SLOT);
        ItemStack input = itemHandler.getStackInSlot(RIGHT_INPUT_SLOT);
        
        input.shrink(1);
        if (tool.isDamageableItem()) {
            tool.setDamageValue(tool.getDamageValue() + 1);
            if (tool.getDamageValue() >= tool.getMaxDamage()) {
                itemHandler.setStackInSlot(RIGHT_TOOL_SLOT, ItemStack.EMPTY);
            }
        }

        // 给予结果
        if (!player.getInventory().add(result.copy())) {
            player.drop(result.copy(), false);
        }

        updateRightCraftingResult();
        setChanged();
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public ResultContainer getCraftResult() {
        return craftResult;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.industrial_workbench");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        System.out.println("[IndustrialWorkbenchDebug] createMenu called! this=" + this + ", id=" + id);
        return new com.singularity_iteration.mio_icif.Menu.Producer.IndustrialWorkbenchMenu(id, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", itemHandler.serializeNBT(registries));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
    }
}