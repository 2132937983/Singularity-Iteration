package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.singularity_iteration.mio_icif.Items.Tools.*;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


@SuppressWarnings("null")
public class mio_icif_miner_elc extends mio_icif_producer {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .battery()
        .upgrade(1)
        .output(15)
        .drill()
        .miningPipe()
        .scanner()
        .build();

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_UPGRADE = 1;
    public static final int SLOT_STORAGE_START = 2;
    public static final int SLOT_STORAGE_COUNT = 15;
    public static final int SLOT_STORAGE_END = SLOT_STORAGE_START + SLOT_STORAGE_COUNT; // 17
    public static final int SLOT_DRILL = 17;
    public static final int SLOT_PIPE = 18;
    public static final int SLOT_SCANNER = 19;
    public static final int TOTAL_SLOTS = 20;

    public static final long DEFAULT_CAPACITY = 10000L;    public static final long DEFAULT_MAX_RECEIVE = 128L;   
    public static final long DEFAULT_MAX_EXTRACT = 1600L;     public static final int DEFAULT_WORK_TIME = 20; 
    public static final long DEFAULT_ENERGY_PER_TICK = 0L; 

    public static final int ENERGY_IRON_DRILL_MIN = 450;
    public static final int ENERGY_IRON_DRILL_MAX = 470;
    public static final int ENERGY_DIAMOND_DRILL_MIN = 880;
    public static final int ENERGY_DIAMOND_DRILL_MAX = 900;
    public static final int ENERGY_IRIDIUM_DRILL_MIN = 1500;
    public static final int ENERGY_IRIDIUM_DRILL_MAX = 1600;
    public static final int ENERGY_OD_SCANNER_MIN = 45;
    public static final int ENERGY_OD_SCANNER_MAX = 75;
    public static final int ENERGY_OV_SCANNER_MIN = 165;
    public static final int ENERGY_OV_SCANNER_MAX = 190;

    public static final int SCAN_RADIUS_OD = 3; 
    public static final int SCAN_RADIUS_OV = 6; 

    public static final int DURABILITY_COST_IRON = 1;
    public static final int DURABILITY_COST_DIAMOND = 1;
    public static final int DURABILITY_COST_IRIDIUM = 1;

    private int currentDepth = 0; 
    private BlockPos tipPos = null; 
    private boolean isPaused = false; 
    private List<BlockPos> oresInCurrentLayer = new ArrayList<>(); 
    private int currentOreIndex = 0; 
    @SuppressWarnings("unused")
    private boolean waitingForNextLayer = false; 

    public mio_icif_miner_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.MINER_ELC_ENTITY_TYPE.get());
    }

    public mio_icif_miner_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
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
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_BATTERY -> isBattery(stack);
            case SLOT_UPGRADE -> getItemAPI().isUpgrade(stack);
            case SLOT_DRILL -> isDrill(stack);
            case SLOT_PIPE -> isMiningPipe(stack);
            case SLOT_SCANNER -> isScanner(stack);
            default -> {
                if (slot >= SLOT_STORAGE_START && slot < SLOT_STORAGE_END) {
                    yield true;
                }
                yield false;
            }
        };
    }

    private boolean isDrill(ItemStack stack) {
        return stack.getItem() instanceof mio_icif_iron_driller ||
               stack.getItem() instanceof mio_icif_diamond_driller ||
               stack.getItem() instanceof mio_icif_iridium_driller;
    }

    private boolean isMiningPipe(ItemStack stack) {
        return stack.is(mio_icif_blocks.BLOCK_MINING_PIPE.get().asItem());
    }

    private boolean isScanner(ItemStack stack) {
        return stack.getItem() instanceof mio_icif_od_scanner ||
               stack.getItem() instanceof mio_icif_ov_scanner;
    }

    private DrillType getDrillType() {
        ItemStack drillStack = itemHandler.getStackInSlot(SLOT_DRILL);
        if (drillStack.getItem() instanceof mio_icif_iron_driller) {
            return DrillType.IRON;
        } else if (drillStack.getItem() instanceof mio_icif_diamond_driller) {
            return DrillType.DIAMOND;
        } else if (drillStack.getItem() instanceof mio_icif_iridium_driller) {
            return DrillType.IRIDIUM;
        }
        return DrillType.NONE;
    }

    private ScannerType getScannerType() {
        ItemStack scannerStack = itemHandler.getStackInSlot(SLOT_SCANNER);
        if (scannerStack.getItem() instanceof mio_icif_od_scanner) {
            return ScannerType.OD;
        } else if (scannerStack.getItem() instanceof mio_icif_ov_scanner) {
            return ScannerType.OV;
        }
        return ScannerType.NONE;
    }

    private int getScanRadius() {
        return switch (getScannerType()) {
            case OD -> SCAN_RADIUS_OD;
            case OV -> SCAN_RADIUS_OV;
            default -> 0;
        };
    }

    @Override
    protected int[] getSlotsForDirection(Direction side) {
        int[] slots = new int[TOTAL_SLOTS - 1]; 
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
        return slot >= SLOT_STORAGE_START && slot < SLOT_STORAGE_END;
    }

    @Override
    protected boolean canWork() {
        if (level == null || level.isClientSide) {
            return false;
        }

        DrillType drillType = getDrillType();
        if (drillType == DrillType.NONE) {
            return false;
        }

        ScannerType scannerType = getScannerType();
        if (scannerType == ScannerType.NONE) {
            return false;
        }

        boolean needsPipe = false;
        if (currentDepth == 0 && tipPos == null) {
            needsPipe = true;
        } else if (oresInCurrentLayer.isEmpty() || currentOreIndex >= oresInCurrentLayer.size()) {
            needsPipe = true;
        }
        if (needsPipe) {
            ItemStack pipeStack = itemHandler.getStackInSlot(SLOT_PIPE);
            if (pipeStack.isEmpty()) {
                return false;
            }
        }

        if (isStorageFull()) {
            return false;
        }

        if (tipPos != null && checkAndExtractLayerFluid()) {
            return false;
        }

        return true;
    }

    private boolean isStorageFull() {
        for (int i = SLOT_STORAGE_START; i < SLOT_STORAGE_END; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }

    private long currentEnergyCost = 0;

    private long calculateEnergyCost() {
        if (currentEnergyCost > 0) {
            return currentEnergyCost;
        }

        long drillCost = 0;
        long scannerCost = 0;

        switch (getDrillType()) {
            case IRON -> drillCost = randInclusive(ENERGY_IRON_DRILL_MIN, ENERGY_IRON_DRILL_MAX);
            case DIAMOND -> drillCost = randInclusive(ENERGY_DIAMOND_DRILL_MIN, ENERGY_DIAMOND_DRILL_MAX);
            case IRIDIUM -> drillCost = randInclusive(ENERGY_IRIDIUM_DRILL_MIN, ENERGY_IRIDIUM_DRILL_MAX);
            case NONE -> {}
        }

        switch (getScannerType()) {
            case OD -> scannerCost = randInclusive(ENERGY_OD_SCANNER_MIN, ENERGY_OD_SCANNER_MAX);
            case OV -> scannerCost = randInclusive(ENERGY_OV_SCANNER_MIN, ENERGY_OV_SCANNER_MAX);
            case NONE -> {}
        }

        currentEnergyCost = drillCost + scannerCost;
        return currentEnergyCost;
    }

    private static int randInclusive(int min, int max) {
        if (max <= min) return min;
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private boolean tryConnectPumpForFluidExtraction(net.minecraft.world.level.material.Fluid fluid, BlockPos fluidPos) {
        if (level == null || worldPosition == null) {
            return false;
        }

        BlockPos[] checkPositions = {
            worldPosition.above(),  
            worldPosition.north(),  
            worldPosition.south(),  
            worldPosition.east(),   
            worldPosition.west()   
        };

        for (BlockPos pumpPos : checkPositions) {
            net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(pumpPos);
            
            if (blockEntity instanceof mio_icif_pump_elc pump) {
                if (pump.canAcceptFluid(fluid)) {
                    if (pump.injectFluid(fluid, 1000)) {
                        level.setBlock(fluidPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void resetEnergyCost() {
        currentEnergyCost = 0;
    }

    private boolean checkAndExtractLayerFluid() {
        if (level == null || tipPos == null) {
            return false;
        }

        int radius = getScanRadius();
        if (radius == 0) {
            return false;
        }

        BlockPos center = new BlockPos(worldPosition.getX(), tipPos.getY(), worldPosition.getZ());
        
        boolean hasRemainingFluid = false;
        
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos checkPos = center.offset(x, 0, z);
                BlockState state = level.getBlockState(checkPos);
                FluidState fluidState = state.getFluidState();
                
                if (!fluidState.isEmpty() && fluidState.isSource()) {
                    if (tryConnectPumpForFluidExtraction(fluidState.getType(), checkPos)) {
                        level.setBlock(checkPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                    } else {
                        hasRemainingFluid = true;
                    }
                }
            }
        }
        
        return hasRemainingFluid;
    }

    @Override
    protected void doWork() {
        if (level == null || level.isClientSide) {
            return;
        }

        isWorking = true;

        if (progress >= maxProgress) {
            if (performMining()) {
                progress = 0;
            }
        }
    }

    private boolean performMining() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        if (currentDepth == 0 && tipPos == null) {
            tipPos = worldPosition.below();
            BlockState belowState = level.getBlockState(tipPos);

            if (!belowState.isAir() && !belowState.canBeReplaced()) {
                if (!consumeEnergyForMining()) {
                    return false;
                }
                consumeDrillDurability();
                resetEnergyCost();
            }

            if (!placeFirstMiningTipConsumePipe()) {
                stopWork();
                return true;
            }
            scanCurrentLayer();
            if (checkAndExtractLayerFluid()) {
                stopWork();
                return true;
            }
            return true;
        }

        if (!oresInCurrentLayer.isEmpty() && currentOreIndex < oresInCurrentLayer.size()) {
            BlockPos orePos = oresInCurrentLayer.get(currentOreIndex);
            
            if (!consumeEnergyForMining()) {
                return false;
            }
            
            if (minePathToOre(serverLevel, orePos)) {
                if (!mineBlock(serverLevel, orePos)) {
                    stopWork();
                    return true;
                }
                currentOreIndex++;
                
                consumeDrillDurability();
                
                resetEnergyCost();
            } else {
                stopWork();
                return true;
            }
            return true;
        }

        BlockPos nextPos = tipPos.below();
        BlockState nextState = level.getBlockState(nextPos);
        FluidState fluidState = nextState.getFluidState();


        if (nextState.isAir() || (!fluidState.isEmpty() && !fluidState.isSource())) {
            if (!replaceTipWithPipeConsumeOne()) {
                stopWork();
                return true;
            }
            if (!moveTipTo(nextPos)) {
                stopWork();
                return true;
            }
            currentDepth++;

            scanCurrentLayer();
            currentOreIndex = 0;

            if (checkAndExtractLayerFluid()) {
                stopWork();
                return true;
            }
            return true;
        }

        if (!fluidState.isEmpty() && fluidState.isSource()) {
            if (tryConnectPumpForFluidExtraction(fluidState.getType(), nextPos)) {
                return true;
            }
            stopWork();
            return true;
        }

        if (!canMineBlock(nextState)) {
            stopWork();
            return true;
        }

        if (!consumeEnergyForMining()) {
            return false;
        }

        if (!mineBlock(serverLevel, nextPos)) {
            stopWork();
            return true;
        }

        if (!replaceTipWithPipeConsumeOne()) {
            stopWork();
            return true;
        }

        if (!moveTipTo(nextPos)) {
            stopWork();
            return true;
        }
        currentDepth++;

        scanCurrentLayer();
        currentOreIndex = 0;

        if (checkAndExtractLayerFluid()) {
            stopWork();
            return true;
        }

        consumeDrillDurability();

        resetEnergyCost();
        return true;
    }

    private boolean consumeEnergyForMining() {
        long energyCost = calculateEnergyCost();
        long energyBefore = energyStorage.getAmount();
        if (energyBefore < energyCost) {
            stopWork();
            return false;
        }
        long actuallyExtracted = energyStorage.extract(energyCost, false);
        return actuallyExtracted > 0;
    }

    private boolean placeFirstMiningTipConsumePipe() {
        if (level == null || tipPos == null) {
            return false;
        }

        ItemStack pipeStack = itemHandler.getStackInSlot(SLOT_PIPE);
        if (pipeStack.isEmpty()) {
            return false;
        }

        BlockState currentState = level.getBlockState(tipPos);
        if (currentState.isAir() || currentState.canBeReplaced()) {
            level.setBlock(tipPos, mio_icif_blocks.BLOCK_MINING_TIP.get().defaultBlockState(), 3);
            pipeStack.shrink(1);
            setChanged();
            return true;
        }

        if (currentState.is(Blocks.BEDROCK)) {
            return false;
        }

        FluidState fluidState = currentState.getFluidState();
        if (!fluidState.isEmpty()) {
            return false;
        }

        if (currentState.getDestroySpeed(level, tipPos) < 0) {
            return false;
        }

        if (!canMineBlock(currentState)) {
            return false;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        if (!mineBlock(serverLevel, tipPos)) {
            return false;
        }

        level.setBlock(tipPos, mio_icif_blocks.BLOCK_MINING_TIP.get().defaultBlockState(), 3);
        pipeStack.shrink(1);
        setChanged();
        return true;
    }

    private boolean replaceTipWithPipeConsumeOne() {
        if (level == null || tipPos == null) {
            return false;
        }

        ItemStack pipeStack = itemHandler.getStackInSlot(SLOT_PIPE);
        if (pipeStack.isEmpty()) {
            return false;
        }

        level.setBlock(tipPos, mio_icif_blocks.BLOCK_MINING_PIPE.get().defaultBlockState(), 3);
        pipeStack.shrink(1);
        setChanged();
        return true;
    }


    private boolean moveTipTo(BlockPos newTipPos) {
        if (level == null) return false;

        BlockState stateAt = level.getBlockState(newTipPos);
        if (!(stateAt.isAir() || stateAt.canBeReplaced())) {
            return false;
        }

        level.setBlock(newTipPos, mio_icif_blocks.BLOCK_MINING_TIP.get().defaultBlockState(), 3);
        tipPos = newTipPos;
        setChanged();
        return true;
    }

    private void scanCurrentLayer() {
        oresInCurrentLayer.clear();
        
        if (level == null || tipPos == null) {
            return;
        }

        int radius = getScanRadius();
        if (radius == 0) {
            return;
        }

        BlockPos center = new BlockPos(worldPosition.getX(), tipPos.getY(), worldPosition.getZ());
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos checkPos = center.offset(x, 0, z);
                BlockState state = level.getBlockState(checkPos);
                
                if (isOre(state)) {
                    oresInCurrentLayer.add(checkPos);
                }
            }
        }
    }

    private boolean isOre(BlockState state) {
        // ??��???��?��???�?
        if (state.is(BlockTags.COAL_ORES) ||
            state.is(BlockTags.IRON_ORES) ||
            state.is(BlockTags.COPPER_ORES) ||
            state.is(BlockTags.GOLD_ORES) ||
            state.is(BlockTags.REDSTONE_ORES) ||
            state.is(BlockTags.LAPIS_ORES) ||
            state.is(BlockTags.DIAMOND_ORES) ||
            state.is(BlockTags.EMERALD_ORES)) {
            return true;
        }

        // NeoForge ??�用?��?��???�?
        if (state.is(Tags.Blocks.ORES)) {
            return true;
        }

        // ?��模式???��?���??��??? tags ????????��?��?��?��??
        if (state.is(mio_icif_blocks.BLOCK_ORE_TIN.get()) ||
            state.is(mio_icif_blocks.BLOCK_ORE_URAN.get()) ||
            state.is(mio_icif_blocks.BLOCK_ORE_LEAD.get())) {
            return true;
        }

        return false;
    }

    /**
     * ??��?��?�tip??�矿??��?�间???路�??
     */
    private boolean minePathToOre(ServerLevel serverLevel, BlockPos orePos) {
        // �???�路�?：直?��水平移动作??��??��?�tip?��??��??�?�?
        int dx = orePos.getX() - tipPos.getX();
        int dz = orePos.getZ() - tipPos.getZ();


        // ???水平移�??
        BlockPos currentPos = tipPos;
        while (dx != 0 || dz != 0) {
            if (dx != 0) {
                int step = dx > 0 ? 1 : -1;
                BlockPos nextPos = currentPos.offset(step, 0, 0);
                if (!mineBlockIfPossible(serverLevel, nextPos)) {
                    return false;
                }
                currentPos = nextPos;
                dx -= step;
            } else if (dz != 0) {
                int step = dz > 0 ? 1 : -1;
                BlockPos nextPos = currentPos.offset(0, 0, step);
                if (!mineBlockIfPossible(serverLevel, nextPos)) {
                    return false;
                }
                currentPos = nextPos;
                dz -= step;
            }
        }

        return true;
    }

    /**
     * �???�可以�?��?��?��?��?�方???
     * 跳�??空气?��?�液体方??��?��?��?? true
     */
    private boolean mineBlockIfPossible(ServerLevel serverLevel, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        
        // �???�是空气?��?�直?��跳�??
        if (state.isAir()) {
            return true;
        }
        
        // �???�是液�?��?�跳�?�?????��?��不�?��?��?�液体�?��??路�???��以穿�?液�?��??
        FluidState fluidState = state.getFluidState();
        if (!fluidState.isEmpty()) {
            return true;
        }
        
        // �??��?��?��?��?���?
        if (state.is(Blocks.BEDROCK)) {
            return false;
        }

        // �??��?��?��?��以�?��??
        if (!canMineBlock(state)) {
            return false;
        }

        return mineBlock(serverLevel, pos);
    }

    /**
     * �??��?��?��?��以�?��?�方???
     * ????��?��?��以�?��?��?�U?��????�方??��???���??��岩�?�液体�??
     */
    private boolean canMineBlock(BlockState state) {
        // 不�?��?��?�空气�???��岩�?�液�?
        if (state.isAir() || state.is(Blocks.BEDROCK)) {
            return false;
        }

        FluidState fluidState = state.getFluidState();
        if (!fluidState.isEmpty()) {
            return false;
        }

        // �??��?��??�硬度�??-1.0F 表示不可?��??��??�??��岩�??
        if (state.getDestroySpeed(level, BlockPos.ZERO) < 0) {
            return false;
        }

        // ?��?��?��头类??��???��??��?��?��??
        return switch (getDrillType()) {
            case IRON -> canIronDrillMine(state);
            case DIAMOND, IRIDIUM -> canDiamondDrillMine(state);
            default -> false;
        };
    }

    /**
     * ????��头可以�?��?��???��???
     */
    private boolean canIronDrillMine(BlockState state) {
        // ????��头�?��?��?��?��??�??��?��工�?��???��???
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) {
            return false;
        }
        // ?��以�?��?�任何�?�硬度�???��??��???���????�??��?��工�?��??�?
        return true;
    }

    /**
     * ?��?��/?��?��头可以�?��?��???��???
     */
    private boolean canDiamondDrillMine(BlockState state) {
        // ?��以�?��?�任何�?�硬度�???��???
        return true;
    }

    /**
     * ??��?�方??�并?��?????�落???
     * 使用destroyBlock?��?��?��??�方??��?��?�获??�落??�并存�?��?��?�槽
     */
    private boolean mineBlock(ServerLevel serverLevel, BlockPos pos) {
        BlockState state = level.getBlockState(pos);


        // �???�已经是空气?��?�直?��返回?��?��??
        if (state.isAir()) {
            return true;
        }

        // ???计算?��?�落???
        LootParams.Builder lootBuilder = new LootParams.Builder(serverLevel)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
            .withParameter(LootContextParams.TOOL, getLootToolStack(serverLevel))
            .withOptionalParameter(LootContextParams.BLOCK_STATE, state);

        List<ItemStack> drops = state.getDrops(lootBuilder);

        // �??��??��?�槽?��?��??�足够�??空间存放??�落???
        if (!canStoreDrops(drops)) {
            return false;
        }

        // 使用destroyBlock?��??�方??��?��??不�?�落??��??�???�们?��己收???�?
        // ????��：�?�置????��?��??�落??��??????��??��??
        boolean destroyed = level.destroyBlock(pos, false);

        if (!destroyed) {
            return false;
        }

        // �???�落??�放??��?��?��??
        for (ItemStack drop : drops) {
            insertItemToStorage(drop);
        }

        return true;
    }

    /**
     * �??��??��?�槽?��?��??�足够�??空间存放??�落???
     */
    private boolean canStoreDrops(List<ItemStack> drops) {
        // 模式????��?��?��???��?��?��??�空?��
        for (ItemStack drop : drops) {
            if (!canInsertItem(drop.copy())) {
                return false;
            }
        }
        return true;
    }

    /**
     * �??��?��?��?��以�?��?��?��????��?��?��??
     */
    private boolean canInsertItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }

        int remaining = stack.getCount();

        // ???�??��?��以�??并�?�已??��?��??????��???
        for (int i = SLOT_STORAGE_START; i < SLOT_STORAGE_END; i++) {
            ItemStack slotStack = itemHandler.getStackInSlot(i);
            if (!slotStack.isEmpty() && ItemStack.isSameItem(slotStack, stack)) {
                int canAdd = Math.min(slotStack.getMaxStackSize() - slotStack.getCount(), remaining);
                remaining -= canAdd;
                if (remaining <= 0) {
                    return true;
                }
            }
        }

        // ??��???��空气??
        for (int i = SLOT_STORAGE_START; i < SLOT_STORAGE_END; i++) {
            ItemStack slotStack = itemHandler.getStackInSlot(i);
            if (slotStack.isEmpty()) {
                remaining -= stack.getMaxStackSize();
                if (remaining <= 0) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * ?��??�用于�?�利???计算?��????�工??��???????�H???��于时间?/精�????????等�?��??
     */
    private ItemStack getLootToolStack(ServerLevel serverLevel) {
        ItemStack drill = itemHandler.getStackInSlot(SLOT_DRILL);
        if (!(drill.getItem() instanceof mio_icif_iridium_driller)) {
            return drill;
        }

        // ?��?��头�?��??事件?��??????魔�?��???��??��?�利???计算?��?��??定�?�触??�该事件，�?��?�显式�?��?��??魔�??事件??
        ItemStack tool = drill.copy();
        var enchantmentLookup = serverLevel.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);

        // 读�?�铱?��头模式�??0=?���?, 1=精�????????）�?��?��?��??类�?��??�??��????��??��??
        int mode = 0;
        var customData = tool.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            var tag = customData.copyTag();
            if (tag.contains("iridium_driller_mode")) {
                mode = tag.getInt("iridium_driller_mode");
            }
        }

        net.minecraft.world.item.enchantment.ItemEnchantments.Mutable ench =
            new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(
                tool.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY));

        if (mode == 0) {
            enchantmentLookup.get(net.minecraft.world.item.enchantment.Enchantments.FORTUNE).ifPresent(holder -> ench.set(holder, 3));
        } else {
            enchantmentLookup.get(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH).ifPresent(holder -> ench.set(holder, 1));
        }
        tool.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS, ench.toImmutable());
        return tool;
    }

    /**
     * �???��????��?��?��?�槽
     */
    private boolean insertItemToStorage(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }

        // ???尝�?��??并�?�已??��?��??
        for (int i = SLOT_STORAGE_START; i < SLOT_STORAGE_END; i++) {
            ItemStack slotStack = itemHandler.getStackInSlot(i);
            if (!slotStack.isEmpty() && ItemStack.isSameItem(slotStack, stack)) {
                int canAdd = Math.min(slotStack.getMaxStackSize() - slotStack.getCount(), stack.getCount());
                if (canAdd > 0) {
                    slotStack.grow(canAdd);
                    stack.shrink(canAdd);
                    if (stack.isEmpty()) {
                        setChanged();
                        return true;
                    }
                }
            }
        }

        // ??��?��?�放??�空气?
        for (int i = SLOT_STORAGE_START; i < SLOT_STORAGE_END; i++) {
            ItemStack slotStack = itemHandler.getStackInSlot(i);
            if (slotStack.isEmpty()) {
                itemHandler.setStackInSlot(i, stack.copy());
                setChanged();
                return true;
            }
        }

        // 没�?�空?���?
        return false;
    }

    /**
     * �???�钻头�?��??
     */
    private void consumeDrillDurability() {
        ItemStack drillStack = itemHandler.getStackInSlot(SLOT_DRILL);
        if (drillStack.isEmpty()) {
            return;
        }

        // ?��模式???��头�?��?�电??�工??��?�用??��????��?��?��?�来表现??��??�????
        if (getItemAPI().isElectricTool(drillStack)) {
            getItemAPI().dischargeElectricTool(drillStack, 1, false);
            setChanged();
        }
    }

    @Override
    protected void stopWork() {
        super.stopWork();
    }

    /**
     * 每tick?��?��??��??
     */
        public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_miner_elc blockEntity) {
        mio_icif_producer.tick(level, pos, state, blockEntity);

        if (!level.isClientSide()) {
            blockEntity.chargeTools();
            
            boolean isLit = state.getValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_miner_elc.LIT);
            if (blockEntity.isWorking() != isLit) {
                level.setBlock(pos, state.setValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_miner_elc.LIT, blockEntity.isWorking()), 3);
            }
        }
    }

    private void chargeTools() {
        ItemStack scannerStack = itemHandler.getStackInSlot(SLOT_SCANNER);
        if (!scannerStack.isEmpty() && getItemAPI().isElectricTool(scannerStack)) {
            long availableEnergy = energyStorage.getAmount();
            if (availableEnergy > 0) {
                long scannerMaxEnergy = getItemAPI().getElectricToolMaxEnergy(scannerStack);
                long currentScannerEnergy = getItemAPI().getElectricToolStored(scannerStack);
                long canAdd = Math.min(scannerMaxEnergy - currentScannerEnergy, availableEnergy);
                if (canAdd > 0) {
                    long added = getItemAPI().chargeElectricTool(scannerStack, canAdd, false);
                    if (added > 0) {
                        apiUseEnergy(added, false);
                    }
                }
            }
        }

        ItemStack drillStack = itemHandler.getStackInSlot(SLOT_DRILL);
        if (!drillStack.isEmpty() && getItemAPI().isElectricTool(drillStack)) {
            long availableEnergy = energyStorage.getAmount();
            if (availableEnergy > 0) {
                long drillMaxEnergy = getItemAPI().getElectricToolMaxEnergy(drillStack);
                long currentDrillEnergy = getItemAPI().getElectricToolStored(drillStack);
                long canAdd = Math.min(drillMaxEnergy - currentDrillEnergy, availableEnergy);
                if (canAdd > 0) {
                    long added = getItemAPI().chargeElectricTool(drillStack, canAdd, false);
                    if (added > 0) {
                        apiUseEnergy(added, false);
                    }
                }
            }
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        currentDepth = tag.getInt("CurrentDepth");
        isPaused = tag.getBoolean("IsPaused");
        currentOreIndex = tag.getInt("CurrentOreIndex");
        
        if (tag.contains("TipPosX")) {
            tipPos = new BlockPos(
                tag.getInt("TipPosX"),
                tag.getInt("TipPosY"),
                tag.getInt("TipPosZ")
            );
        }

        // ??�载?��??��?�表
        oresInCurrentLayer.clear();
        int oreCount = tag.getInt("OreCount");
        for (int i = 0; i < oreCount; i++) {
            int x = tag.getInt("Ore" + i + "X");
            int y = tag.getInt("Ore" + i + "Y");
            int z = tag.getInt("Ore" + i + "Z");
            oresInCurrentLayer.add(new BlockPos(x, y, z));
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("CurrentDepth", currentDepth);
        tag.putBoolean("IsPaused", isPaused);
        tag.putInt("CurrentOreIndex", currentOreIndex);
        
        if (tipPos != null) {
            tag.putInt("TipPosX", tipPos.getX());
            tag.putInt("TipPosY", tipPos.getY());
            tag.putInt("TipPosZ", tipPos.getZ());
        }

        // 保�?�矿??��?�表
        tag.putInt("OreCount", oresInCurrentLayer.size());
        for (int i = 0; i < oresInCurrentLayer.size(); i++) {
            BlockPos pos = oresInCurrentLayer.get(i);
            tag.putInt("Ore" + i + "X", pos.getX());
            tag.putInt("Ore" + i + "Y", pos.getY());
            tag.putInt("Ore" + i + "Z", pos.getZ());
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.miner_elc");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Producer.MinerElcMenu(containerId, playerInventory, this);
    }

    // ?��头类??��?�举
    private enum DrillType {
        NONE, IRON, DIAMOND, IRIDIUM
    }

    // ?��??�器类�?��?��??
    private enum ScannerType {
        NONE, OD, OV
    }
}