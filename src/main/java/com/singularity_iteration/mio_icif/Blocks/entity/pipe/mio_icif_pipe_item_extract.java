package com.singularity_iteration.mio_icif.Blocks.entity.pipe;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.energy.grid.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 电力驱动的物品输入管道方块实
 *
 * 设计参考：抽水管道（流体抽取管道）的电力系
 * - 未连接电网时：极慢速度（每16 tick传输1个物品）
 * - 连接电网时：根据电网电流大小提升速度
 * - 速度无上限，完全由电力决
 *
 * 电流越大，每次从容器中抽取和传输的物品数量就越多
 */
@SuppressWarnings("null")
public class mio_icif_pipe_item_extract extends mio_icif_pipe_item implements IEnergySink {

    // 使用与运输管道相同的类型，确保可以互相连
    public static final String PIPE_TYPE = "item";

    // ========== 电力相关常量 ==========
    // 未通电时的基础传输速率（每次传输的物品数量
    public static final int BASE_TRANSFER_RATE = 1;
    // 未通电时的基础冷却时间 - 20 tick才传个物
    public static final int BASE_TRANSFER_COOLDOWN = 20;
    // 最大传输速率（通电时）- 无上限，根据电力自动调整
    public static final int MAX_TRANSFER_RATE = Integer.MAX_VALUE;
    // 每提个物品传输速率需要的EU/tick
    // 目标28EU/t 达到 64个物tick
    // 计算方式28EU/t ÷ (64-1)个物= 128/63 2.03
    // 2.0，确28EU/t可以达到64个物tick
    public static final double EU_PER_ITEM = 2.0;
    // 最大EU消- 达到64个物品需(64-1)*2 = 126 EU/tick
    public static final int MAX_EU_CONSUMPTION = 128;
    // 能量存储容量 - 存储tick的能量消
    public static final long ENERGY_CAPACITY = 128;
    // 最大接收能- MV级为128 EU/tick
    public static final long MAX_RECEIVE = 128;

    // 当前实际传输速率（根据电力动态计算）
    private int currentTransferRate = BASE_TRANSFER_RATE;
    // 能量存储
    private long storedEnergy = 0;
    // 是否已注册到电网
    private boolean energyRegistered = false;

    public mio_icif_pipe_item_extract(BlockPos pos, BlockState state) {
        this(mio_icif_block_entities.PIPE_ITEM_INPUT_ENTITY_TYPE.get(), pos, state);
    }

    public mio_icif_pipe_item_extract(@Nullable BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type != null ? type : mio_icif_block_entities.PIPE_ITEM_INPUT_ENTITY_TYPE.get(), pos, state, PipeMode.INPUT);
    }

    // ========== 电网注册 ==========

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide() && !energyRegistered) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new EnergyTileLoadEvent(this, level));
            energyRegistered = true;
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide() && energyRegistered) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this, level));
            energyRegistered = false;
        }
        super.setRemoved();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (level != null && !level.isClientSide() && !energyRegistered) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new EnergyTileLoadEvent(this, level));
            energyRegistered = true;
        }
    }

    // ========== 传输逻辑 ==========

    /**
     * 更新电力状
     * 根据存储的能量计算当前传输速率
     * 每tick消耗能量来维持速度
     */
    private void updatePowerState() {
        if (storedEnergy > 0) {
            // 有能量存储，根据能量计算速度
            // 计算可提升的速率 = 可用能量 / 每物品所需能量
            int rateBoost = (int) (storedEnergy / EU_PER_ITEM);
            // 限制在基础速率和最大速率之间
            currentTransferRate = Math.min(BASE_TRANSFER_RATE + rateBoost, MAX_TRANSFER_RATE);

            // 消耗能量来维持当前速率
            int rateAboveBase = currentTransferRate - BASE_TRANSFER_RATE;
            if (rateAboveBase > 0) {
                long energyConsumed = (long) (rateAboveBase * EU_PER_ITEM);
                storedEnergy = Math.max(0, storedEnergy - energyConsumed);
            }
        } else {
            // 无能量，使用基础速率
            currentTransferRate = BASE_TRANSFER_RATE;
        }
    }

    /**
     * 获取当前传输速率
     */
    public int getCurrentTransferRate() {
        return currentTransferRate;
    }

    @Override
    protected void doTransfer() {
        if (level == null || level.isClientSide()) return;

        // 更新电力状
        updatePowerState();

        // 减少冷却计数
        if (transferCooldown > 0) {
            transferCooldown--;
            return;
        }

        // 执行传输
        boolean transferred = extractAndTransferItem();
        if (transferred) {
            // 根据是否有电力决定冷却时
            // 有电力时使用快速冷8 tick)，无电力时使用慢速冷20 tick)
            if (storedEnergy > 0) {
                transferCooldown = TRANSFER_COOLDOWN; // 8 tick
            } else {
                transferCooldown = BASE_TRANSFER_COOLDOWN; // 20 tick
            }
        }
    }

    /**
     * 从容器提取物品并传输到运输管
     * 根据电力决定每次提取和传输的物品数量
     */
    @Override
    protected boolean extractAndTransferItem() {
        // 收集所有源和需求方
        List<ItemSource> containerSources = new ArrayList<>();
        List<mio_icif_pipe_item> pipeNeighbors = new ArrayList<>();

        for (Direction dir : Direction.values()) {
            if (!isConnected(dir)) continue;

            BlockPos adjacentPos = worldPosition.relative(dir);

            // 检查是否是其他管道
            if (level.getBlockEntity(adjacentPos) instanceof mio_icif_pipe_item otherPipe) {
                pipeNeighbors.add(otherPipe);
                continue;
            }

            // 获取容器的能
            IItemHandler handler = level.getCapability(
                Capabilities.ItemHandler.BLOCK, adjacentPos, dir.getOpposite()
            );

            if (handler == null) continue;

            // 收集容器作为
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack available = handler.extractItem(slot, currentTransferRate, true);
                if (!available.isEmpty() && available.getCount() > 0) {
                    containerSources.add(new ItemSource(adjacentPos, dir, handler, slot, available, false));
                    break;
                }
            }
        }

        // 输入型：从容器提取，输出给运输型管道
        if (!containerSources.isEmpty()) {
            // 使用访问记录防止循环
            Set<BlockPos> visited = new HashSet<>();
            visited.add(worldPosition);

            // 首先检查是否有可接收的运输型管
            List<PipeTarget> acceptingPipes = new ArrayList<>();
            for (mio_icif_pipe_item pipe : pipeNeighbors) {
                // 管道需要能接受物品（运输型
                if (!pipe.canInsert() || visited.contains(pipe.getBlockPos())) continue;
                if (pipe.isProcessing()) continue;

                // 计算从当前管道指向目标管道的方向，目标管道收到物品时来源方向是反方向
                Direction toPipeDir = Direction.fromDelta(
                    pipe.getBlockPos().getX() - worldPosition.getX(),
                    pipe.getBlockPos().getY() - worldPosition.getY(),
                    pipe.getBlockPos().getZ() - worldPosition.getZ()
                );
                IItemHandler pipeHandler = pipe.getItemHandlerCapability(toPipeDir != null ? toPipeDir.getOpposite() : null);
                if (pipeHandler == null) continue;

                // 测试是否可以插入
                ItemStack testStack = new ItemStack(net.minecraft.world.item.Items.STONE, 1);
                ItemStack remainingTest = pipeHandler.insertItem(0, testStack, true);
                if (!remainingTest.isEmpty()) continue;

                acceptingPipes.add(new PipeTarget(pipe, pipeHandler, toPipeDir));
            }

            // 没有可接收的管道，不提取物品
            if (acceptingPipes.isEmpty()) {
                return false;
            }

            // 使用轮询方式选择源容
            int sourceIndex = inputSourceIndex % containerSources.size();
            ItemSource source = containerSources.get(sourceIndex);
            inputSourceIndex = (inputSourceIndex + 1) % containerSources.size();

            // 根据当前速率决定提取数量
            int extractAmount = Math.min(currentTransferRate, source.item.getCount());
            ItemStack extracted = source.handler.extractItem(source.slot, extractAmount, false);
            if (extracted.isEmpty()) {
                return false;
            }

            // 使用轮询方式选择目标管道
            int targetIndex = roundRobinIndex % acceptingPipes.size();
            PipeTarget target = acceptingPipes.get(targetIndex);
            roundRobinIndex = (roundRobinIndex + 1) % acceptingPipes.size();

            // 尝试插入物品
            target.pipe.setProcessing(true);
            try {
                ItemStack toInsert = extracted.copy();
                ItemStack notInserted = target.handler.insertItem(0, toInsert, false);
                
                if (notInserted.isEmpty()) {
                    // 全部插入成功
                    return true;
                } else {
                    // 部分插入成功，剩余物品需要处
 // 由于输入管道不应该存物品，尝试将剩余物品返回给源容
                    int insertedCount = extracted.getCount() - notInserted.getCount();
                    if (insertedCount > 0) {
                        // 至少插入了一部分，算作成
                        // 将剩余物品尝试返回给源容
                        ItemStack returned = source.handler.insertItem(source.slot, notInserted, false);
                        if (!returned.isEmpty()) {
                            // 如果源容器无法接受剩余物品，掉落它们
                            double x = worldPosition.getX() + 0.5;
                            double y = worldPosition.getY() + 0.5;
                            double z = worldPosition.getZ() + 0.5;
                            net.minecraft.world.entity.item.ItemEntity itemEntity = 
                                new net.minecraft.world.entity.item.ItemEntity(level, x, y, z, returned);
                            level.addFreshEntity(itemEntity);
                        }
                        return true;
                    }
                    return false;
                }
            } finally {
                target.pipe.setProcessing(false);
            }
        }

        return false;
    }

    // ========== IEnergySink 接口实现 ==========

    @Override
    public double getDemandedEnergy() {
        // 始终请求能量直到存储
        long spaceAvailable = ENERGY_CAPACITY - storedEnergy;
        if (spaceAvailable <= 0) return 0;
        return Math.min(spaceAvailable, MAX_EU_CONSUMPTION);
    }

    @Override
    public double injectEnergy(Direction directionFrom, double amount, double voltage) {
        // 接收能量并存
        double canReceive = Math.min(amount, MAX_RECEIVE);
        double overflow = amount - canReceive;
        storedEnergy += canReceive;
        if (storedEnergy > ENERGY_CAPACITY) {
            overflow += storedEnergy - ENERGY_CAPACITY;
            storedEnergy = ENERGY_CAPACITY;
        }
        return overflow;
    }

    @Override
    public int getSinkTier() {
        // 返回 MV 电压等级 (tier 2 = 128 EU/t)
        return EnergyNetGlobal.cableTierToSourceTier(CableTier.MV);
    }

    @Override
    public boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction direction) {
        // 接受来自任何方向的能
        return true;
    }

    @Override
    public String getPipeTypeString() {
        return PIPE_TYPE;
    }

    // ========== NBT 保存/加载 ==========

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("current_transfer_rate", currentTransferRate);
        tag.putLong("stored_energy", storedEnergy);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("current_transfer_rate", CompoundTag.TAG_INT)) {
            currentTransferRate = tag.getInt("current_transfer_rate");
        }
        if (tag.contains("stored_energy", CompoundTag.TAG_LONG)) {
            storedEnergy = tag.getLong("stored_energy");
        }
    }

    // ========== 辅助==========

    @SuppressWarnings("unused")
    private static class ItemSource {
        final BlockPos pos;
        final Direction direction;
        final IItemHandler handler;
        final int slot;
        final ItemStack item;
        final boolean isPipe;

        ItemSource(BlockPos pos, Direction direction, IItemHandler handler, int slot, ItemStack item, boolean isPipe) {
            this.pos = pos;
            this.direction = direction;
            this.handler = handler;
            this.slot = slot;
            this.item = item;
            this.isPipe = isPipe;
        }
    }

    @SuppressWarnings("unused")
    private static class PipeTarget {
        final mio_icif_pipe_item pipe;
        final IItemHandler handler;
        final Direction direction;

        PipeTarget(mio_icif_pipe_item pipe, IItemHandler handler, Direction direction) {
            this.pipe = pipe;
            this.handler = handler;
            this.direction = direction;
        }
    }
}

