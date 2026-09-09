package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_nuclear_reactor_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.reactor.mio_icif_reactor_chamber;
import com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_condensator;
import com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_redstone_condensator;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 红石核反应堆冷却液注入器
 *
 * 特点功能：
 * - 继承 producer 超类
 * - 最大箱子大小，接受不超过 54 个物品（最大箱子大小）
 * - 无需延迟修复核反应堆中的红石冷凝器模块时不会引起过热
 * - 允许最大电流为 128 EU/t 中级电力
 * - 模组效率更低需要 15% 为每次时刻，即超过最大热量 3000 时修复
 * - 需要1000 EU 每次需要红石能量来源为修复冷凝器时模组补偿
 * - 需要 reactor 中的某些字符来判断是否需要修复冷凝器
 */
@SuppressWarnings("null")
public class mio_icif_redstone_reactor_coolant_injector extends mio_icif_producer {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .extra(53)
        .battery()
        .build();

    // 槽位数量 - 54 = 4个最大箱子大小
    public static final int SLOT_COUNT = 54;
    // 电池槽索引为53：使用多槽位排列，第一个槽位置为电池槽位
    public static final int BATTERY_SLOT = 53;

    // 默认配置 - MV 级中级电力
    public static final long DEFAULT_CAPACITY = 10000L;
    public static final long DEFAULT_MAX_RECEIVE = 128L; // MV 级最大输入128 EU/t
    public static final long DEFAULT_MAX_EXTRACT = 0L; // 不输出电力
    public static final int DEFAULT_WORK_TIME = 1; // 无延迟，每 tick 都可以工作
    public static final long DEFAULT_ENERGY_PER_OPERATION = 1000L; // 每次修复操作需要1000 EU

    // 红石冷凝器模块最大热量存储
    public static final int REDSTONE_CONDENSATOR_MAX_HEAT = 20000;
    // 触发修复的最低余量低于 15%即3000时
    public static final int REPAIR_THRESHOLD = 3000;

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) energyStorage.getAmount();
                case 1 -> (int) energyStorage.getCapacity();
                case 2 -> getRedstoneBlockCount();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() { return 3; }
    };

    /**
     * 构造 BlockEntityType.Builder 注册用参数构造函数
     * 注册时会调用此构造函数
     */
    public mio_icif_redstone_reactor_coolant_injector(BlockPos pos, BlockState state) {
        this(pos, state, null);
    }

    public mio_icif_redstone_reactor_coolant_injector(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type != null ? type : mio_icif_block_entities.REDSTONE_REACTOR_COOLANT_INJECTOR_ENTITY_TYPE.get(),
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            DEFAULT_WORK_TIME,
            LAYOUT,
            DEFAULT_ENERGY_PER_OPERATION,
            CableTier.MV);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == BATTERY_SLOT) {
            // 电池槽：接受任何可充电的电池类物品
            return isBattery(stack);
        } else {
            // 所有槽位置接受红石块
            return stack.is(Items.REDSTONE_BLOCK);
        }
    }

    /**
     * 获取特定方向可访问的槽位
     * 槽位索引 0-52 为对应红石块槽位，53 为电池槽
     * @param side 方向
     * @return 可访问的槽位索引数组
     */
    @Override
    protected int[] getSlotsForDirection(Direction side) {
        // 任何方向所有槽位都可以被访问
        int[] slots = new int[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) {
            slots[i] = i;
        }
        return slots;
    }

    /**
     * 检查指定槽位是否可以特定方向提取
     * @param slot 槽位索引
     * @param side 方向
     * @return 是否可以提取
     */
    @Override
    protected int getBatterySlot() {
        return BATTERY_SLOT;
    }

    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        // 除电池槽外的所有槽位时可以提取的，只有非电池槽位但非红石块时可以提取，实际上不允许提取红石块
        return slot != BATTERY_SLOT;
    }

    /**
     * 每tick执行工作核心逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_redstone_reactor_coolant_injector blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 调用父类tick方法处理升级和充放电等逻辑
        mio_icif_producer.tick(level, pos, state, blockEntity);

        // 尝试修复操作无需冷却延迟，直接尝试修复
        blockEntity.tryRepairCondensators();
    }

    /**
     * 尝试修复核反应堆中的红石冷凝器模块
     */
    private void tryRepairCondensators() {
        // 获取正确的朝向位置
        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
        BlockPos targetPos = worldPosition.relative(facing);

        mio_icif_nuclear_reactor_generator reactor = findReactor(level, targetPos);

        if (reactor == null) {
            return;
        }

        // 检查反应堆物品中的项目位置寻找需要修复的红石冷凝器
        // 对比IC2：每次尝试修复至少一个电容，只有耐久度低于3000时修复
        boolean repaired = false;
        for (int i = 0; i < reactor.getReactorItems().size(); i++) {
            ItemStack stack = reactor.getItem(i);

            // 检查是否是红石冷凝器模块
            if (stack.getItem() instanceof mio_icif_redstone_condensator condensator) {
                int storedHeat = condensator.getStoredHeat(stack);
                int currentDurability = REDSTONE_CONDENSATOR_MAX_HEAT - storedHeat;

                // 只有在低于指定修复余量低于3000时
                if (currentDurability < REPAIR_THRESHOLD) {
                    // 尝试执行修复，每次尝试修复一个电容
                    if (attemptRepair(reactor, i, stack, condensator)) {
                        repaired = true;
                        break; // 每tick只修复一个电容即可
                    }
                }
            }
        }

        if (repaired) {
            setChanged();
        }
    }

    /**
     * 查找反应堆是否连接相关位置
     * @param level 世界
     * @param pos 查找位置
     * @return 反应堆，如果没有找到则返回 null
     */
    @Nullable
    private mio_icif_nuclear_reactor_generator findReactor(Level level, BlockPos pos) {
        if (level == null) {
            return null;
        }

        BlockEntity be = level.getBlockEntity(pos);

        // 检查是否直接是反应堆
        if (be instanceof mio_icif_nuclear_reactor_generator reactor) {
            return reactor;
        }

        // 检查是否是反应室并获取连接的反应堆
        if (be instanceof mio_icif_reactor_chamber chamber) {
            return chamber.getConnectedReactor();
        }

        return null;
    }

    /**
     * 尝试修复特定位置的冷凝器
     * @param reactor 反应堆方块
     * @param slotIndex 槽位索引
     * @param stack 组件组件
     * @param condensator 组件组件
     * @return 是否成功修复
     */
    private boolean attemptRepair(mio_icif_nuclear_reactor_generator reactor, int slotIndex,
                                   ItemStack stack, mio_icif_condensator condensator) {
        // 检查是否有足够能量进行操作
        if (energyStorage.getAmount() < DEFAULT_ENERGY_PER_OPERATION) {
            return false;
        }

        // 检查是否有红石块
        int redstoneBlockSlot = findRedstoneBlock();
        if (redstoneBlockSlot == -1) {
            return false;
        }

        // 消耗能量
        if (!consumeEnergy()) {
            return false;
        }

        // 消耗一个红石块
        ItemStack redstoneBlock = itemHandler.getStackInSlot(redstoneBlockSlot);
        redstoneBlock.shrink(1);
        if (redstoneBlock.isEmpty()) {
            itemHandler.setStackInSlot(redstoneBlockSlot, ItemStack.EMPTY);
        }

        // 修复冷凝器模块，将热量设置为 0
        condensator.setStoredHeat(stack, 0);

        // 重新设置反应堆中的物品
        reactor.setItem(slotIndex, stack);

        return true;
    }

    /**
     * 查找红石块所在的第一个槽位
     * @return 槽位的索引，没有找到则返回 -1
     */
    private int findRedstoneBlock() {
        for (int i = 0; i < BATTERY_SLOT; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.is(Items.REDSTONE_BLOCK) && !stack.isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected boolean canWork() {
        // 该机器不使用进度工作模式，而是执行修复动作
        // 因此返回 false，不使用进度系统
        return false;
    }

    @Override
    protected void doWork() {
        // 不使用进度工作模式
    }

    @Override
    protected boolean shouldResetProgress() {
        // 不使用进度模式
        return false;
    }

    /**
     * 获取所有红石块的数量
     * @return 红石块的数量
     */
    public int getRedstoneBlockCount() {
        int count = 0;
        for (int i = 0; i < BATTERY_SLOT; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.is(Items.REDSTONE_BLOCK)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
    }

    // ==================== MenuProvider 接口实现 ===================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.redstone_reactor_coolant_injector");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Producer.RedstoneReactorCoolantInjectorMenu(containerId, playerInventory, this);
    }

    public ContainerData getContainerData() { return containerData; }
}