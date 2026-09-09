package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 作�?�收?��?��?��??��?��?�类
 * 对�?��?��?�IC2?��??��??
 * - ?��??��???���?x3x9�?水平每方???4?���?????��每方????���?
 * - ?��??�方式�?��?�格?��??��?��?�次?���????�?个�?��??
 * - ?��?��?��事件?�达??��??佳收?��?��段�?��??大阶�?
 * - ??��?��????��?�扫???1 EU，�?��???��???0 EU
 * - 产�?��?????：只返回?�产??��?��?��?��?��?��?��??种�?�由作�?��?��?��??
 * - ??��??输出：�?��?�放??�西侧相??�容?���?失败??��?�落为�?��??
 *
 * 槽位?��?��??�?
 * 0-14: ??��??存�?�槽位?15个槽位置??
 * 15: ?��池�??
 * 16: 作�?��????�仪�?
 * 17: ???级槽位?�???��??
 */
@SuppressWarnings("null")
public class mio_icif_harvest_elc extends mio_icif_producer {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .output(15)
        .battery()
        .extra(1)
        .upgrade(1)
        .build();

    // 槽位?��?��??
    public static final int SLOT_STORAGE_START = 0;
    public static final int SLOT_STORAGE_COUNT = 15;  // 15个�?��?�槽
    public static final int SLOT_BATTERY = 15;        // ?��池�??
    public static final int SLOT_ANALYZER = 16;       // 作�?��????�仪�?
    public static final int SLOT_UPGRADE = 17;        // ???级槽位?�???��??
    public static final int TOTAL_SLOTS = 18;

    // 默认??�置
    public static final long DEFAULT_CAPACITY = 10000L;      // ???大�?��??0000EU
    public static final long DEFAULT_MAX_RECEIVE = 32L;      // ???大�?��??2EU/t (LV等级)
    public static final long DEFAULT_MAX_EXTRACT = 0L;
    public static final int DEFAULT_WORK_TIME = 10;          // 0.5秒工作�??�?10 ticks)
    public static final long DEFAULT_ENERGY_PER_TICK = 1L;   // 每次?��??��????? EU
    public static final long ENERGY_PER_HARVEST = 20L;       // 每次??��???��?���????0 EU

    // 工�?��???���?与�?��?�IC2?��??��??x3x9�?
    public static final int HORIZONTAL_RANGE = 4;            // 水平????��4???
    public static final int VERTICAL_RANGE = 1;              // ????��????��1???

    public mio_icif_harvest_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.HARVEST_ELC_ENTITY_TYPE.get());
    }

    public mio_icif_harvest_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            DEFAULT_WORK_TIME,
            LAYOUT,
            DEFAULT_ENERGY_PER_TICK,
            CableTier.LV);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.harvest");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        ContainerData data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> (int) energyStorage.getAmount();
                    case 1 -> (int) energyStorage.getCapacity();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 2;
            }
        };

        return new com.singularity_iteration.mio_icif.Menu.Producer.HarvestElcMenu(
            containerId, playerInventory, this, data);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot >= SLOT_STORAGE_START && slot < SLOT_STORAGE_START + SLOT_STORAGE_COUNT) {
            return false;
        }
        if (slot == SLOT_BATTERY) {
            return isBattery(stack);
        }
        if (slot == SLOT_ANALYZER) {
            return isCropAnalyzer(stack);
        }
        if (slot == SLOT_UPGRADE) {
            return isUpgrade(stack);
        }
        return false;
    }

    private boolean isUpgrade(ItemStack stack) {
        return stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Upgrade.mio_icif_upgrade;
    }

    private boolean isCropAnalyzer(ItemStack stack) {
        return stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Tools.CropAnalyzerItem;
    }

    @SuppressWarnings("unused")
    private boolean hasCropAnalyzer() {
        return isCropAnalyzer(itemHandler.getStackInSlot(SLOT_ANALYZER));
    }

    @Override
    protected int[] getSlotsForDirection(Direction side) {
        // ???级槽不可被管???/漏�?�访?���?与IC2??��??�??���?
        int[] slots = new int[TOTAL_SLOTS - 1]; // ??�除SLOT_UPGRADE
        for (int i = 0; i < SLOT_UPGRADE; i++) {
            slots[i] = i;
        }
        for (int i = SLOT_UPGRADE + 1; i < TOTAL_SLOTS; i++) {
            slots[i - 1] = i;
        }
        return slots;
    }

    @Override
    protected int getBatterySlot() {
        return SLOT_BATTERY;
    }

    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        return slot >= SLOT_STORAGE_START && slot < SLOT_STORAGE_START + SLOT_STORAGE_COUNT;
    }

    @Override
    protected boolean canInsertItem(int slot, ItemStack stack, @Nullable Direction side) {
        if (slot == SLOT_BATTERY) {
            return isBattery(stack);
        }
        if (slot == SLOT_ANALYZER) {
            return isCropAnalyzer(stack);
        }
        if (slot == SLOT_UPGRADE) {
            return false;
        }
        return false;
    }

    @Override
    protected boolean canWork() {
        return energyStorage.getAmount() >= DEFAULT_ENERGY_PER_TICK;
    }

    @Override
    protected void doWork() {
    }

    /**
     * 每tick?��?��??��?? - 对�?��?��?�IC2
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_harvest_elc blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        mio_icif_producer.tick(level, pos, state, blockEntity);

        // ??��?�IC2：�??10 ticks?��??��??�?
        if (level.getGameTime() % 10L != 0L) {
            return;
        }

        // �??��?��?��??�足够�?��?��?��?�扫??��?? EU）?
        if (blockEntity.energyStorage.getAmount() < DEFAULT_ENERGY_PER_TICK) {
            blockEntity.updateWorkingState(level, pos, false);
            return;
        }

        // �???�扫??��?��??
        blockEntity.apiUseEnergy(DEFAULT_ENERGY_PER_TICK, false);

        // ??��?�扫??��?�收???
        boolean harvested = blockEntity.scanAndHarvest(level, pos);

        blockEntity.updateWorkingState(level, pos, harvested);
    }

    /**
     * ?��??�并?��???- 对�?��?��?�IC2?��???
     * 每次?��??��?�个位置，�?��???��?��??��?��?��??
     */
    private boolean scanAndHarvest(Level level, BlockPos center) {
        // ?��??�整个�???��，收?��第�??个找??��????��?��?��??
        // 从中�???��?�扫??��?��?��???��?��??��?�机?��???作�??
        for (int y = -VERTICAL_RANGE; y <= VERTICAL_RANGE; y++) {
            for (int x = 0; x <= HORIZONTAL_RANGE; x++) {
                for (int z = 0; z <= HORIZONTAL_RANGE; z++) {
                    // ?��??��?��?��?�置??��?�对称�?�置�?个象??��??
                    BlockPos[] positions = {
                        center.offset(x, y, z),
                        center.offset(-x, y, z),
                        center.offset(x, y, -z),
                        center.offset(-x, y, -z)
                    };
                    for (BlockPos checkPos : positions) {
                        if (tryHarvestAtPosition(level, checkPos)) {
                            return true; // ??��???��?���?个�?�本次�?��??
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * 尝�?�在???定�?�置?��???
     */
    private boolean tryHarvestAtPosition(Level level, BlockPos pos) {
        BlockState targetState = level.getBlockState(pos);

        // 尝�?�收?��作�?�架上�??作�??
        if (targetState.getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.Crop.mio_icif_crop_stick ||
            targetState.getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.Crop.mio_icif_crop_stick_upgraded) {
            return tryHarvestCropStick(level, pos);
        }

        // 尝�?�收?��?��??��?��??
        return tryHarvestNormalCrop(level, pos, targetState);
    }

    /**
     * 尝�?�收?��作�?�架上�??作�??- 对�?��?��?�IC2
     * 使用performHarvest?��法�?��?��?��?��?��??
     */
    private boolean tryHarvestCropStick(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof com.singularity_iteration.mio_icif.Blocks.entity.crop.mio_icif_crop_entity cropEntity)) {
            return false;
        }

        // �??��?��?��??��?��??
        if (cropEntity.getPlant() == null) {
            return false;
        }

        // 不收?��??????
        if (cropEntity.getPlant().getTypeId().equals("weed")) {
            return false;
        }

        // �??��?��?��?��事件?�使?��canBeHarvested?��法�??达�?��??佳收?��?��段即?���?
        if (!cropEntity.getPlant().canBeHarvested(cropEntity)) {
            return false;
        }

        // �??��?��?��??�足够�?��?��?��?�收?���?0 EU）?
        if (energyStorage.getAmount() < ENERGY_PER_HARVEST) {
            return false;
        }

        // �??��存�?�槽?��?��已满
        if (isStorageFull()) {
            return false;
        }

        // ??��?�收?���?使用performHarvest�??��返回?�产??��?��?��?��?��?��?��??
        List<ItemStack> drops = cropEntity.performHarvest();
        if (drops == null || drops.isEmpty()) {
            return false;
        }

        // �????产�?��?��?��?��?��?�槽??��?��??
        for (ItemStack drop : drops) {
            boolean added = addItemToStorage(drop);
            if (!added) {
                // 存�?�槽充满??，�?�落为�?��??
                dropItem(level, drop);
            }
        }

        // �???�收?��??��??
        apiUseEnergy(ENERGY_PER_HARVEST, false);
        return true;
    }

    /**
     * 尝�?�收?��?��??��?��??
     */
    private boolean tryHarvestNormalCrop(Level level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();

        if (!(block instanceof CropBlock cropBlock)) {
            return false;
        }

        if (!cropBlock.isMaxAge(state)) {
            return false;
        }

        if (energyStorage.getAmount() < ENERGY_PER_HARVEST) {
            return false;
        }

        if (isStorageFull()) {
            return false;
        }

        // ?��??�产??��????��???��式�?�只?��??�产??��?��?��???��种�?��??
        List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(
            state, (net.minecraft.server.level.ServerLevel) level, pos, null);

        // ??�置作�??
        level.setBlock(pos, cropBlock.getStateForAge(0), 3);

        // �????产�??
        for (ItemStack drop : drops) {
            if (!addItemToStorage(drop)) {
                dropItem(level, drop);
            }
        }

        apiUseEnergy(ENERGY_PER_HARVEST, false);
        return true;
    }

    /**
     * �??��存�?�槽?��?��已满
     */
    private boolean isStorageFull() {
        for (int i = SLOT_STORAGE_START; i < SLOT_STORAGE_START + SLOT_STORAGE_COUNT; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }

    /**
     * �???��??添�?��?��?��?��??
     */
    private boolean addItemToStorage(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }

        // ???尝�?��??并�?�已??�槽位?
        for (int i = SLOT_STORAGE_START; i < SLOT_STORAGE_START + SLOT_STORAGE_COUNT; i++) {
            ItemStack existing = itemHandler.getStackInSlot(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)) {
                int space = existing.getMaxStackSize() - existing.getCount();
                if (space > 0) {
                    int toAdd = Math.min(space, stack.getCount());
                    ItemStack newStack = existing.copy();
                    newStack.grow(toAdd);
                    itemHandler.setStackInSlot(i, newStack);
                    stack.shrink(toAdd);
                    if (stack.isEmpty()) {
                        return true;
                    }
                }
            }
        }

        // ??��?��?�放??�空槽位??
        for (int i = SLOT_STORAGE_START; i < SLOT_STORAGE_START + SLOT_STORAGE_COUNT; i++) {
            ItemStack existing = itemHandler.getStackInSlot(i);
            if (existing.isEmpty()) {
                itemHandler.setStackInSlot(i, stack.copy());
                return true;
            }
        }

        return false;
    }

    /**
     * ??�落??��??为�?��?��????��?�IC2?��式�??
     */
    private void dropItem(Level level, ItemStack stack) {
        if (stack.isEmpty()) return;
        net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
            level, getBlockPos().getX() + 0.5, getBlockPos().getY() + 1.0, getBlockPos().getZ() + 0.5, stack);
        level.addFreshEntity(itemEntity);
    }

    private void updateWorkingState(Level level, BlockPos pos, boolean working) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_harvest) {
            boolean currentWorking = state.getValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_harvest.WORKING);
            if (currentWorking != working) {
                level.setBlock(pos, state.setValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_harvest.WORKING, working), 3);
            }
        }
    }

}