package com.miophas.singularity_iteration.common.blockentity.producer;

import com.mojang.authlib.GameProfile;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.item.tools.*;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.runtime.processing.PendingDrops;
import com.miophas.singularity_iteration.core.runtime.processing.MiningLoot;
import com.miophas.singularity_iteration.core.runtime.processing.MiningRoute;
import com.miophas.singularity_iteration.core.runtime.processing.MiningPayment;
import com.miophas.singularity_iteration.core.runtime.processing.PipeAdvance;
import com.miophas.singularity_iteration.core.runtime.processing.MiningPlacement;
import com.miophas.singularity_iteration.core.runtime.processing.MachineActionOwner;
import com.miophas.singularity_iteration.core.runtime.processing.RecipeSlots;
import com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;


@SuppressWarnings("null")
public class mio_icif_miner_elc extends AbstractProcessingMachineBlockEntity implements MiningRangeProvider {
    private static final String ACTION_OWNER_KEY = "scex_machine_action_owner";
    private static final GameProfile LEGACY_ACTOR = new GameProfile(
        UUID.nameUUIDFromBytes("mio_icif:automated_miner".getBytes(StandardCharsets.UTF_8)), "[SI Miner]");
    private final MiningRoute scexRoute = new MiningRoute(() -> ContainerToTank.markUnsaved(this));
    private final MiningPayment scexMiningPayment = new MiningPayment(() -> ContainerToTank.markUnsaved(this));
    private final PipeAdvance scexPipeAdvance = new PipeAdvance(() -> ContainerToTank.markUnsaved(this));
    private boolean scexLayerReady;
    private final PendingDrops scexPendingDrops = new PendingDrops(() -> ContainerToTank.markUnsaved(this));
    private MachineActionOwner actionOwner = MachineActionOwner.legacy(LEGACY_ACTOR);


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
    // 已同步到客户端的工作半径，用于在扫描器变化时触发一次方块实体同步
    private int scexSyncedRangeRadius = -1;
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

    /** 客户端渲染工作范围边界时使用的水平半径，与扫描器决定的实际工作范围一致。 */
    @Override
    public int getMiningRangeRadius() {
        return getScanRadius();
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
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread()
                || !actionOwner.canAct()
                || scexPendingDrops.isBusy() || !scexPendingDrops.isEmpty() || scexMiningPayment.isBusy()
                || scexPipeAdvance.isBusy() || scexRoute.invalid() || scexPipeAdvance.uncertain()) return false;
        if (!scexRoute.belongsTo(worldPosition, level.getMinBuildHeight(), level.getMaxBuildHeight())
                || !scexPipeAdvance.belongsTo(worldPosition, level.getMinBuildHeight(), level.getMaxBuildHeight())) return false;
        if (scexPipeAdvance.active()) return true;
        if (getDrillType() == DrillType.NONE || getScannerType() == ScannerType.NONE) return false;
        if (scexRoute.active()) return true;
        boolean needsPipe = tipPos == null || scexLayerReady && currentOreIndex >= oresInCurrentLayer.size();
        return !needsPipe || isMiningPipe(itemHandler.getStackInSlot(SLOT_PIPE));
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
        ContainerToTank.markUnsaved(this);
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

        if (!(level instanceof ServerLevel server) || !available(server, fluidPos)) return false;
        for (BlockPos pumpPos : checkPositions) {
            if (!available(server, pumpPos)) continue;
            net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(pumpPos);

            if (blockEntity instanceof mio_icif_pump_elc pump && pump.tryCollectForMiner(this, fluidPos)) {
                return true;
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
                if (!(level instanceof ServerLevel server) || !available(server, checkPos)) return true;
                BlockState state = level.getBlockState(checkPos);
                FluidState fluidState = state.getFluidState();

                if (!fluidState.isEmpty() && fluidState.isSource()) {
                    if (!tryConnectPumpForFluidExtraction(fluidState.getType(), checkPos)) {
                        hasRemainingFluid = true;
                    }
                }
            }
        }

        return hasRemainingFluid;
    }

    @Override
    protected void onTick() {
        if (actionOwner.canAct()) flushPendingLoot();
        syncRangeRadiusToClient();
    }

    /**
     * 扫描器决定的工作半径发生变化时，向客户端同步一次方块实体数据，
     * 以便客户端工作范围渲染能即时更新（客户端物品栏不随 GUI 槽位同步）。
     */
    private void syncRangeRadiusToClient() {
        int radius = getScanRadius();
        if (radius == scexSyncedRangeRadius) return;
        scexSyncedRangeRadius = radius;
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /**
     * 默认实现返回 null，会导致 {@link #syncRangeRadiusToClient()} 触发的方块更新
     * 无法携带方块实体数据发往客户端（表现为只有重新进入存档才刷新）。
     */
    @Nullable
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void doWork() {
        if (!canWork()) { stopWork(); return; }
        isWorking = true;
        if (progress < maxProgress) return;
        if (performMining()) progress = 0;
        else stopWork();
        ContainerToTank.markUnsaved(this);
    }

    @Override
    protected void updateProgress() {
        if (isWorking) progress = (int) Math.min(Math.max(0, maxProgress), (long) Math.max(0, progress) + Math.max(1, getProgressPerTick()));
    }

    private boolean performMining() {
        if (!(level instanceof ServerLevel server)) return false;
        if (scexPipeAdvance.active()) return resumePipeAdvance(server);
        if (scexRoute.active()) return advanceRoute(server);
        if (tipPos == null) {
            var first = worldPosition.below();
            if (!available(server, first)) return false;
            if (!scexRoute.begin(worldPosition, first, calculateEnergyCost())) return false;
            return advanceRoute(server);
        }
        if (!available(server, tipPos) || !server.getBlockState(tipPos).is(mio_icif_blocks.BLOCK_MINING_TIP.get())) return false;
        if (!scexLayerReady && !scanCurrentLayer()) return false;
        if (checkAndExtractLayerFluid()) return false;
        while (currentOreIndex < oresInCurrentLayer.size()) {
            var ore = oresInCurrentLayer.get(currentOreIndex);
            if (!available(server, ore)) return false;
            if (!isOre(server.getBlockState(ore))) { currentOreIndex++; ContainerToTank.markUnsaved(this); continue; }
            if (!scexRoute.begin(tipPos, ore, calculateEnergyCost())) return false;
            return advanceRoute(server);
        }
        var next = tipPos.below();
        if (!available(server, next) || !isMiningPipe(itemHandler.getStackInSlot(SLOT_PIPE))) return false;
        if (!scexRoute.begin(tipPos, next, calculateEnergyCost())) return false;
        return advanceRoute(server);
    }

    private boolean available(ServerLevel server, BlockPos pos) {
        return !server.isOutsideBuildHeight(pos) && server.getWorldBorder().isWithinBounds(pos)
            && server.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private boolean advanceRoute(ServerLevel server) {
        if (!scexRoute.active() || scexRoute.invalid()) return false;
        int examined = 0;
        while (!scexRoute.complete() && examined++ < 2 * MiningRoute.RADIUS && scexPendingDrops.isEmpty()) {
            var next = scexRoute.next();
            if (!available(server, next)) return false;
            var state = server.getBlockState(next);
            boolean descending = next.getX() == worldPosition.getX() && next.getZ() == worldPosition.getZ()
                && (tipPos == null || next.getY() < tipPos.getY());
            if (state.isAir() || !descending && !state.getFluidState().isEmpty()) {
                scexRoute.advance(next); continue;
            }
            if (!state.getFluidState().isEmpty()) {
                // Pump owns protection checks, successful source pickup and storage; never delete the source here.
                if (state.getFluidState().isSource()) tryConnectPumpForFluidExtraction(state.getFluidState().getType(), next);
                return false;
            }
            if (!canMineBlock(state, next)) return false;
            var lootTool = getLootToolStack(server).copy();
            Runnable account = () -> { scexRoute.markPaid(); scexRoute.advance(next); };
            boolean removed;
            if (scexRoute.paid()) {
                removed = MiningLoot.capture(server, next, lootTool, actionOwner, scexPendingDrops, this::canStoreDrops, null, account);
            } else {
                long toolCost = getItemAPI().isElectricTool(itemHandler.getStackInSlot(SLOT_DRILL)) ? 1 : 0;
                removed = scexMiningPayment.attempt(energyStorage, itemHandler, SLOT_DRILL, getItemAPI(), scexRoute.cost(), toolCost,
                    payment -> MiningLoot.capture(server, next, lootTool, actionOwner, scexPendingDrops, this::canStoreDrops, payment, account));
            }
            if (!removed) return false;
            flushPendingLoot();
        }
        if (!scexRoute.complete() || !scexPendingDrops.isEmpty()) return false;
        var target = scexRoute.target();
        boolean descending = target.getX() == worldPosition.getX() && target.getZ() == worldPosition.getZ()
            && (tipPos == null || target.getY() < tipPos.getY());
        if (descending) {
            var pipe = itemHandler.getStackInSlot(SLOT_PIPE);
            if (!isMiningPipe(pipe) || !scexPipeAdvance.begin(itemHandler, SLOT_PIPE, pipe.copyWithCount(1), tipPos, target)) return false;
            return resumePipeAdvance(server);
        }
        scexRoute.finish(); currentOreIndex++; resetEnergyCost(); ContainerToTank.markUnsaved(this);
        return true;
    }

    private boolean resumePipeAdvance(ServerLevel server) {
        if (!scexPipeAdvance.belongsTo(worldPosition, server.getMinBuildHeight(), server.getMaxBuildHeight())) return false;
        if (!scexPipeAdvance.advance(new PipeAdvance.WorldAccess() {
            public PipeAdvance.Outcome placeTip(BlockPos target) {
                if (!available(server, target)) return PipeAdvance.Outcome.RETRY;
                var before = server.getBlockState(target);
                if (!before.isAir()) return PipeAdvance.Outcome.RETRY;
                return MiningPlacement.replace(server, target, before, mio_icif_blocks.BLOCK_MINING_TIP.get().defaultBlockState(),
                    scexPipeAdvance.reserved(), actionOwner);
            }
            public PipeAdvance.Outcome replaceOldTip(BlockPos previous) {
                if (!available(server, previous)) return PipeAdvance.Outcome.RETRY;
                var before = server.getBlockState(previous);
                if (!before.is(mio_icif_blocks.BLOCK_MINING_TIP.get())) return PipeAdvance.Outcome.RETRY;
                return MiningPlacement.replace(server, previous, before, mio_icif_blocks.BLOCK_MINING_PIPE.get().defaultBlockState(),
                    ItemStack.EMPTY, actionOwner);
            }
        })) return false;
        tipPos = scexPipeAdvance.target();
        currentDepth = Math.max(0, worldPosition.getY() - tipPos.getY() - 1);
        if (scexRoute.active() && scexRoute.complete()) scexRoute.finish();
        scexPipeAdvance.finish(); resetEnergyCost();
        currentOreIndex = 0; oresInCurrentLayer.clear(); scexLayerReady = false;
        ContainerToTank.markUnsaved(this); return true;
    }










    private boolean scanCurrentLayer() {
        if (!(level instanceof ServerLevel server) || tipPos == null) return false;
        int radius = getScanRadius();
        var found = new ArrayList<BlockPos>();
        var center = new BlockPos(worldPosition.getX(), tipPos.getY(), worldPosition.getZ());
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            var candidate = center.offset(x, 0, z);
            if (!server.getWorldBorder().isWithinBounds(candidate)) continue;
            if (!available(server, candidate)) return false;
            if (!candidate.equals(tipPos) && isOre(server.getBlockState(candidate))) found.add(candidate);
        }
        oresInCurrentLayer = found; currentOreIndex = 0; scexLayerReady = true;
        ContainerToTank.markUnsaved(this); return true;
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


    /**
     * �???�可以�?��?��?��?��?�方???
     * 跳�??空气?��?�液体方??��?��?��?? true
     */


    /**
     * �??��?��?��?��以�?��?�方???
     * ????��?��?��以�?��?��?�U?��????�方??��???���??��岩�?�液体�??
     */
    private boolean canMineBlock(BlockState state, BlockPos pos) {
        // 不�?��?��?�空气�???��岩�?�液�?
        if (state.isAir() || state.is(Blocks.BEDROCK)) {
            return false;
        }

        FluidState fluidState = state.getFluidState();
        if (!fluidState.isEmpty()) {
            return false;
        }

        // �??��?��??�硬度�??-1.0F 表示不可?��??��??�??��岩�??
        if (state.getDestroySpeed(level, pos) < 0) {
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


    private int[] lootOutputSlots() {
        return java.util.stream.IntStream.range(SLOT_STORAGE_START, SLOT_STORAGE_END).toArray();
    }

    private void flushPendingLoot() {
        if (!scexPendingDrops.isEmpty()) scexPendingDrops.commitOwned(itemHandler, lootOutputSlots());
    }

    /**
     * �??��??��?�槽?��?��??�足够�??空间存放??�落???
     */
    private boolean canStoreDrops(List<ItemStack> drops) {
        return drops.isEmpty() || RecipeSlots.outputs(itemHandler, lootOutputSlots(), drops).isPresent();
    }

    /**
     * �??��?��?��?��以�?��?��?��????��?��?��??
     */


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


    /**
     * �???�钻头�?��??
     */


    @Override
    protected void stopWork() {
        super.stopWork();
    }

    /**
     * 每tick?��?��??��??
     */
        public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_miner_elc blockEntity) {
        if (!blockEntity.actionOwner.canAct()) { blockEntity.stopWork(); return; }
        AbstractProcessingMachineBlockEntity.tick(level, pos, state, blockEntity);

        if (!level.isClientSide()) {
            blockEntity.chargeTools();

            boolean isLit = state.getValue(com.miophas.singularity_iteration.common.block.producer.mio_icif_block_miner_elc.LIT);
            if (blockEntity.isWorking() != isLit) {
                level.setBlock(pos, state.setValue(com.miophas.singularity_iteration.common.block.producer.mio_icif_block_miner_elc.LIT, blockEntity.isWorking()), 3);
            }
        }
    }

    private void chargeTools() {
        if (scexPendingDrops.isBusy() || scexMiningPayment.isBusy() || scexPipeAdvance.isBusy()) return;
        com.miophas.singularity_iteration.core.runtime.energy.ToolEnergy.charge(itemHandler, SLOT_SCANNER, energyStorage, getItemAPI());
        com.miophas.singularity_iteration.core.runtime.energy.ToolEnergy.charge(itemHandler, SLOT_DRILL, energyStorage, getItemAPI());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        actionOwner = MachineActionOwner.load(tag, ACTION_OWNER_KEY, LEGACY_ACTOR);
        scexPendingDrops.load(registries, tag.getList("scex_pending_mining_drops", net.minecraft.nbt.Tag.TAG_COMPOUND));
        scexRoute.load(tag.getCompound("scex_mining_route"));
        scexMiningPayment.load(tag.getCompound("scex_mining_payment"));
        scexPipeAdvance.load(tag.getCompound("scex_pipe_advance"), registries);
        currentEnergyCost = Math.max(0, tag.getLong("scex_mining_cost"));
        scexLayerReady = tag.getBoolean("scex_layer_ready");
        currentDepth = Math.max(0, tag.getInt("CurrentDepth"));
        isPaused = tag.getBoolean("IsPaused");
        currentOreIndex = Math.max(0, tag.getInt("CurrentOreIndex"));
        tipPos = null;

        if (tag.contains("TipPosX")) {
            tipPos = new BlockPos(
                tag.getInt("TipPosX"),
                tag.getInt("TipPosY"),
                tag.getInt("TipPosZ")
            );
        }

        // ??�载?��??��?�表
        oresInCurrentLayer.clear();
        int oreCount = Math.clamp(tag.getInt("OreCount"), 0, (2 * MiningRoute.RADIUS + 1) * (2 * MiningRoute.RADIUS + 1));
        var unique = new java.util.HashSet<BlockPos>();
        for (int i = 0; i < oreCount; i++) {
            int x = tag.getInt("Ore" + i + "X");
            int y = tag.getInt("Ore" + i + "Y");
            int z = tag.getInt("Ore" + i + "Z");
            var candidate = new BlockPos(x, y, z);
            if (tipPos == null || y != tipPos.getY() || Math.abs((long) x - worldPosition.getX()) > MiningRoute.RADIUS
                    || Math.abs((long) z - worldPosition.getZ()) > MiningRoute.RADIUS || !unique.add(candidate)) {
                oresInCurrentLayer.clear(); scexLayerReady = false; break;
            }
            oresInCurrentLayer.add(candidate);
        }
        currentOreIndex = Math.min(currentOreIndex, oresInCurrentLayer.size());
        if (tipPos != null && (tipPos.getX() != worldPosition.getX() || tipPos.getZ() != worldPosition.getZ())) {
            tipPos = null; oresInCurrentLayer.clear(); currentOreIndex = 0; scexLayerReady = false;
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(ACTION_OWNER_KEY, actionOwner.save());
        tag.put("scex_pending_mining_drops", scexPendingDrops.save(registries));
        tag.put("scex_mining_route", scexRoute.save());
        tag.put("scex_mining_payment", scexMiningPayment.save());
        tag.put("scex_pipe_advance", scexPipeAdvance.save(registries));
        tag.putLong("scex_mining_cost", currentEnergyCost);
        tag.putBoolean("scex_layer_ready", scexLayerReady);
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
        return new com.miophas.singularity_iteration.common.menu.producer.MinerElcMenu(containerId, playerInventory, this);
    }

    public MachineActionOwner getActionOwner() { return actionOwner; }
    public void setActionOwnerFromPlacer(@Nullable LivingEntity placer) {
        actionOwner = MachineActionOwner.fromPlacer(placer);
        ContainerToTank.markUnsaved(this);
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
