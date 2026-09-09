package com.singularity_iteration.mio_icif.Blocks.entity.generator;

import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.MachineItemHandler;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;

/**
 * 流体反应堆处理器
 *
 * 功能描述：
 * - 管理流体反应堆的液体槽（输入槽和输出槽）
 * - 处理冷却液加热成热冷却液的逻辑
 * - 只产生热量，不发电
 * - 管理液体单元槽：2个输入槽（放入冷却液单元）+ 2个输出槽（输出热冷却液单元）
 */
@SuppressWarnings("null")
public class mio_icif_fluid_reactor_handler implements net.neoforged.neoforge.fluids.capability.IFluidHandler {

    // 液体槽容量（单位：毫桶）
    public static final int FLUID_CAPACITY = 10000;

    // 输入液体槽 - 存放冷却液
    private final FluidTank inputTank;

    // 输出液体槽 - 存放热冷却液
    private final FluidTank outputTank;

    // 物品槽索引
    public static final int INPUT_CELL_SLOT_1 = 0;  // 输入冷却液单元槽1
    public static final int OUTPUT_EMPTY_SLOT_1 = 1; // 输出热冷却液单元槽1
    public static final int INPUT_CELL_SLOT_2 = 2;  // 输入冷却液单元槽2
    public static final int OUTPUT_EMPTY_SLOT_2 = 3; // 输出热冷却液单元槽2
    public static final int TOTAL_ITEM_SLOTS = 4;

    // 物品槽处理器
    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .extra(2)
        .output(2)
        .build();

    private final MachineItemHandler itemHandler;

    // 当前热量产生速率
    private int currentHeatGeneration = 0;

    // 热量转换效率（每 tick 多少热量可以转换多少液体）
    public static final int HEAT_PER_MB = 1; // 1 HU 热量可以加热 1 mB 冷却液
    public mio_icif_fluid_reactor_handler() {
        // 初始化输入槽（冷却液）

        this.inputTank = new FluidTank(FLUID_CAPACITY) {
            @Override
            public boolean isFluidValid(FluidStack stack) {
                // 只允许冷却液进入输入槽
            return isCoolant(stack);
            }
        };

        // 初始化输出槽（热冷却液）
        this.outputTank = new FluidTank(FLUID_CAPACITY) {
            @Override
            public boolean isFluidValid(FluidStack stack) {
                // 只允许热冷却液进入输出槽
                return isHotCoolant(stack);
            }
        };

        // 初始化物品槽处理器
    this.itemHandler = new MachineItemHandler(LAYOUT) {
            @Override
            protected void onContentsChanged(int slot) {
                onItemSlotChanged(slot);
            }
        };
        this.itemHandler.setValidator((slot, stack, slotType) -> mio_icif_fluid_reactor_handler.this.canInsertItem(slot, stack));
    }

    /**
     * 检查物品是否可以放入指定槽
     */
    private boolean canInsertItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) return true;

        return switch (slot) {
            case INPUT_CELL_SLOT_1, INPUT_CELL_SLOT_2 -> isCoolantCell(stack);
            case OUTPUT_EMPTY_SLOT_1, OUTPUT_EMPTY_SLOT_2 -> false; // 输出槽不允许手动放入
            default -> false;
        };
    }

    /**
     * 检查是否为冷却液单元
     */
    private boolean isCoolantCell(ItemStack stack) {
        return mio_icif_cells.isCellContainingFluid(stack, mio_icif_fluids.COOLANT.get());
    }

    private boolean isEmptyCell(ItemStack stack) {
        return mio_icif_cells.isEmptyCell(stack);
    }

    /**
     * 槽位内容变化时的处理
     * 注意：实际的液体单元处理在 tick() 方法中进行，以避免递归问题
     */
    private void onItemSlotChanged(int slot) {
        // 槽位变化标记，将在下一 tick 时处理
    // 实际的液体单元处理在 tick() 方法中调用 processFluidCells()
    }

    /**
     * 处理液体单元的填充和清空
     */
    private void processFluidCells() {
        // 处理输入槽的冷却液单元 -> 将冷却液转移到输入液体槽，输出空单元
        processInputCell(INPUT_CELL_SLOT_1, OUTPUT_EMPTY_SLOT_1);
        processInputCell(INPUT_CELL_SLOT_2, OUTPUT_EMPTY_SLOT_2);

        // 处理输出槽的热冷却液单元 -> 将热冷却液填充到热冷却液单元
    processOutputCell(OUTPUT_EMPTY_SLOT_1);
        processOutputCell(OUTPUT_EMPTY_SLOT_2);
    }

    /**
     * 处理输入槽的冷却液单元
     * @param inputSlot 输入槽索引
     * @param outputSlot 输出槽索引（用于输出空单元）
     */
    private void processInputCell(int inputSlot, int outputSlot) {
        ItemStack inputStack = itemHandler.getStackInSlot(inputSlot);

        // 检查输入槽是否有冷却液单元
        if (!isCoolantCell(inputStack)) {
            return;
        }

        // 检查输入液体槽是否有足够空间（每个单元提供1000mB）

        int spaceAvailable = inputTank.getCapacity() - inputTank.getFluidAmount();
        if (spaceAvailable < 1000) {
            return; // 没有足够空间
        }

        // 检查输出槽是否可以接受空单元
    ItemStack outputStack = itemHandler.getStackInSlot(outputSlot);
        ItemStack emptyCell = new ItemStack(com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells.CELL_EMPTY.get());

        if (outputStack.isEmpty()) {
            // 输出槽为空，可以直接放入
        } else if (!outputStack.is(emptyCell.getItem())) {
            return; // 输出槽有其他物品
        } else if (outputStack.getCount() >= outputStack.getMaxStackSize()) {
            return; // 输出槽已满
    }

        // 执行转移：冷却液单元 -> 空单元 + 冷却液
    // 向输入液体槽添加冷却液
    FluidStack coolant = new FluidStack(mio_icif_fluids.COOLANT.get(), 1000);
        int filled = inputTank.fill(coolant, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);

        if (filled < 1000) {
            return; // 无法填充足够的冷却液
        }

        // 实际执行
        inputTank.fill(coolant, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);

        // 消耗一个冷却液单元
        inputStack.shrink(1);
        if (inputStack.isEmpty()) {
            itemHandler.setStackInSlot(inputSlot, ItemStack.EMPTY);
        }

        // 输出空单元
    if (outputStack.isEmpty()) {
            itemHandler.setStackInSlot(outputSlot, emptyCell);
        } else {
            outputStack.grow(1);
        }
    }

    /**
     * 处理输出槽的空单元，填充热冷却液
     * @param outputSlot 输出槽索引
     */
    private void processOutputCell(int outputSlot) {
        ItemStack outputStack = itemHandler.getStackInSlot(outputSlot);

        // 检查输出槽是否有空单元
        if (!isEmptyCell(outputStack)) {
            return;
        }

        // 检查输出液体槽是否有足够的热冷却液（每个单元需要1000mB）
    FluidStack hotCoolantInTank = outputTank.getFluid();
        if (hotCoolantInTank.isEmpty() || hotCoolantInTank.getAmount() < 1000) {
            return; // 没有足够的热冷却液
    }

        // 检查输入槽是否有空间放置热冷却液单元
    int inputSlot = (outputSlot == OUTPUT_EMPTY_SLOT_1) ? INPUT_CELL_SLOT_1 : INPUT_CELL_SLOT_2;
        ItemStack inputStack = itemHandler.getStackInSlot(inputSlot);
        ItemStack hotCoolantCell = new ItemStack(com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells.CELL_HOTCOOLANT.get());

        if (inputStack.isEmpty()) {
            // 输入槽为空，可以直接放入
        } else if (!inputStack.is(hotCoolantCell.getItem())) {
            return; // 输入槽有其他物品
        } else if (inputStack.getCount() >= inputStack.getMaxStackSize()) {
            return; // 输入槽已满
    }

        // 执行转移：空单元 + 热冷却液 -> 热冷却液单元
        // 从输出液体槽消耗热冷却液
    FluidStack toDrain = new FluidStack(mio_icif_fluids.HOTCOOLANT.get(), 1000);
        FluidStack drained = outputTank.drain(toDrain, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);

        if (drained.isEmpty() || drained.getAmount() < 1000) {
            return; // 无法抽取足够的热冷却液
    }

        // 消耗一个空单元
        outputStack.shrink(1);
        if (outputStack.isEmpty()) {
            itemHandler.setStackInSlot(outputSlot, ItemStack.EMPTY);
        }

        // 输出热冷却液单元到输入槽（因为输入槽对应的是该液体槽的输出输入）
    if (inputStack.isEmpty()) {
            itemHandler.setStackInSlot(inputSlot, hotCoolantCell);
        } else {
            inputStack.grow(1);
        }
    }

    /**
     * 检查是否为冷却液
     */
    private boolean isCoolant(FluidStack stack) {
        if (stack.isEmpty()) return true; // 空流体视为有效（允许槽位为空）
    return stack.getFluid() == mio_icif_fluids.COOLANT.get();
    }

    /**
     * 检查是否为热冷却液
     */
    private boolean isHotCoolant(FluidStack stack) {
        if (stack.isEmpty()) return true;
        return stack.getFluid() == mio_icif_fluids.HOTCOOLANT.get();
    }

    /**
     * tick 更新逻辑
     * @param heatGenerated tick 产生的热量
     * @return 实际消耗的热量（用于加热冷却液）
     */
    public int tick(int heatGenerated) {
        this.currentHeatGeneration = heatGenerated;

        // tick 处理液体单元（将冷却液单元转换为液体，或将热冷却液填充到空单元）
        processFluidCells();

        if (heatGenerated <= 0) {
            return 0;
        }

        // 计算可以加热多少冷却液
    int maxConvertible = heatGenerated / HEAT_PER_MB;
        if (maxConvertible <= 0) {
            return 0;
        }

        // 检查输入槽是否有足够的冷却液
    FluidStack inputFluid = inputTank.getFluid();
        if (inputFluid.isEmpty() || inputFluid.getAmount() <= 0) {
            return 0; // 没有冷却液可以加热
    }

        // 实际可以转换的量（受限于输入量、输出槽空间和热量）
        int actualConvert = Math.min(maxConvertible, inputFluid.getAmount());

        // 检查输出槽空间
        int outputSpace = outputTank.getCapacity() - outputTank.getFluidAmount();
        actualConvert = Math.min(actualConvert, outputSpace);

        if (actualConvert <= 0) {
            return 0;
        }

        // 执行转换：从输入槽取出冷却液，向输出槽放入热冷却液
    inputTank.drain(actualConvert, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);

        // 向输出槽添加热冷却液
        FluidStack hotCoolant = new FluidStack(mio_icif_fluids.HOTCOOLANT.get(), actualConvert);
        outputTank.fill(hotCoolant, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);

        // 返回实际消耗的热量
        return actualConvert * HEAT_PER_MB;
    }

    /**
     * 获取输入液体槽
     */
    public FluidTank getInputTank() {
        return inputTank;
    }

    /**
     * 获取输出液体槽
     */
    public FluidTank getOutputTank() {
        return outputTank;
    }

    /**
     * 获取当前热量产生速率
     */
    public int getCurrentHeatGeneration() {
        return currentHeatGeneration;
    }

    /**
     * 获取输入槽液体量
     */
    public int getInputFluidAmount() {
        return inputTank.getFluidAmount();
    }

    /**
     * 获取输出槽液体量
     */
    public int getOutputFluidAmount() {
        return outputTank.getFluidAmount();
    }

    /**
     * 获取输入槽容量
     */
    public int getInputCapacity() {
        return inputTank.getCapacity();
    }

    /**
     * 获取输出槽容量
     */
    public int getOutputCapacity() {
        return outputTank.getCapacity();
    }

    /**
     * 获取物品槽处理器
     */
    public MachineItemHandler getItemHandler() {
        return itemHandler;
    }

    /**
     * 获取指定槽位的物品
     */
    public ItemStack getStackInSlot(int slot) {
        return itemHandler.getStackInSlot(slot);
    }

    /**
     * 设置指定槽位的物品
     */
    public void setStackInSlot(int slot, ItemStack stack) {
        itemHandler.setStackInSlot(slot, stack);
    }

    /**
     * 保存数据到NBT
     */
    public void saveToNBT(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("InputTank", inputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("OutputTank", outputTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("CurrentHeatGen", currentHeatGeneration);
        tag.put("ItemHandler", itemHandler.serializeNBT(registries));
    }

    /**
     * 从NBT 加载数据
     */
    public void loadFromNBT(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("InputTank")) {
            inputTank.readFromNBT(registries, tag.getCompound("InputTank"));
        }
        if (tag.contains("OutputTank")) {
            outputTank.readFromNBT(registries, tag.getCompound("OutputTank"));
        }
        if (tag.contains("CurrentHeatGen")) {
            currentHeatGeneration = tag.getInt("CurrentHeatGen");
        }
        if (tag.contains("ItemHandler")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("ItemHandler"));
        }
    }

    // ==================== IFluidHandler 接口实现 ====================

    @Override
    public int getTanks() {
        return 2; // 输入槽和输出槽
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        return switch (tank) {
            case 0 -> inputTank.getFluid();
            case 1 -> outputTank.getFluid();
            default -> FluidStack.EMPTY;
        };
    }

    @Override
    public int getTankCapacity(int tank) {
        return FLUID_CAPACITY;
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return switch (tank) {
            case 0 -> isCoolant(stack);
            case 1 -> isHotCoolant(stack);
            default -> false;
        };
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        // 只能填充到输入槽（冷却液）
    if (resource.isEmpty() || !isCoolant(resource)) {
            return 0;
        }
        return inputTank.fill(resource, action);
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        // 优先从输出槽（热冷却液）抽取
        if (!resource.isEmpty() && isHotCoolant(resource)) {
            FluidStack fromOutput = outputTank.drain(resource, action);
            if (!fromOutput.isEmpty()) {
                return fromOutput;
            }
        }
        // 如果输出槽没有，尝试从输入槽（冷却液）抽取
    return inputTank.drain(resource, action);
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        // 优先从输出槽（热冷却液）抽取
        FluidStack fromOutput = outputTank.drain(maxDrain, action);
        if (!fromOutput.isEmpty()) {
            return fromOutput;
        }
        // 如果输出槽没有，尝试从输入槽（冷却液）抽取
    return inputTank.drain(maxDrain, action);
    }
}
