package com.miophas.singularity_iteration.common.blockentity.pipe;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.api.energy.grid.*;
import com.miophas.singularity_iteration.core.runtime.energy.grid.*;
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
    public int getCurrentTransferRate() { return currentTransferRate; }
    @Override protected int extractionLimit() {
        currentTransferRate = Math.min(64, BASE_TRANSFER_RATE + (int)(Math.max(0,storedEnergy) / 2));
        return currentTransferRate;
    }
    @Override protected int extractionCooldown() { return currentTransferRate > 1 ? TRANSFER_COOLDOWN : BASE_TRANSFER_COOLDOWN; }
    @Override protected void extracted(int count) {
        storedEnergy = Math.max(0, storedEnergy - Math.max(0,count-1) * 2L);
        dirty();
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
        if (!Double.isFinite(amount) || amount <= 0.0D) return amount;
        long requested = Math.min((long) Math.floor(amount), MAX_RECEIVE);
        long accepted = Math.min(requested, Math.max(0L, ENERGY_CAPACITY - storedEnergy));
        storedEnergy += accepted;
        // Return both capacity overflow and any fractional EU the long buffer cannot hold.
        return amount - accepted;
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
            currentTransferRate = Math.clamp(tag.getInt("current_transfer_rate"),1,64);
        }
        if (tag.contains("stored_energy", CompoundTag.TAG_LONG)) {
            storedEnergy = Math.clamp(tag.getLong("stored_energy"),0,ENERGY_CAPACITY);
        }
    }

}
