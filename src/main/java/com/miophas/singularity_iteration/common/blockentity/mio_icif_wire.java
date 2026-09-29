package com.miophas.singularity_iteration.common.blockentity;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractWireBlockEntity;

import com.miophas.singularity_iteration.common.block.wire.mio_icif_block_wire;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.api.energy.grid.*;
import com.miophas.singularity_iteration.core.runtime.energy.grid.*;
import com.miophas.singularity_iteration.common.integration.ae2.AE2Compat;
import com.miophas.singularity_iteration.common.integration.ae2.Ae2EnergySink;
import com.miophas.singularity_iteration.common.integration.mi.EnergyBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Wire block entity implementing IC2-style IEnergyConductor.
 * Registers to the energy grid via EnergyTileLoadEvent/EnergyTileUnloadEvent.
 * Uses EnergyBridge from the integration layer to push energy to adjacent
 * non-IEnergyTile storages (e.g. Modern Industrialization machines).
 *
 * <h3>电线参数说明</h3>
 * <p>所有电气参数均从 {@link CableTier} 读取，在构造时一次性确定并存储在 final 字段中。</p>
 * <p><b>修改电线参数只需修改 {@link CableTier} 中的实例定义即可。</b></p>
 * <ul>
 *   <li>绝缘电线：按材质和层数吸收电压；超过绝缘阈值的实际包仍可能触电</li>
 *   <li>自定义参数：使用全参数构造函数直接传入自定义值</li>
 * </ul>
 */
@SuppressWarnings("null")
public class mio_icif_wire extends AbstractWireBlockEntity implements com.miophas.singularity_iteration.core.api.energy.IColoredEnergyTile {
    private net.minecraft.world.item.DyeColor cableColor = net.minecraft.world.item.DyeColor.BLACK;
    @Override public net.minecraft.world.item.DyeColor getEnergyColor(Direction side) {
        return cableColor == net.minecraft.world.item.DyeColor.BLACK ? null : cableColor;
    }
    public net.minecraft.world.item.DyeColor getCableColor() { return cableColor; }
    public boolean canColor(net.minecraft.world.item.DyeColor color) {
        return color != cableColor && !getBlockState().getValue(mio_icif_block_wire.FOAMLOGGED)
            && (insulated || getCableTier() == CableTier.IV);
    }
    public void setCableColor(net.minecraft.world.item.DyeColor color) {
        if (!canColor(color)) return;
        cableColor = color;
        setChanged();
        if (level != null && !level.isClientSide) {
            com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy.conductorColorChanged(this);
            // Re-evaluate this wire too: notifying neighbours alone is not
            // sufficient when none of their block states changes in response.
            var state = getBlockState();
            if (state.getBlock() instanceof mio_icif_block_wire block)
                block.neighborChanged(state, level, worldPosition, block, worldPosition, false);
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        refreshClientModel();
    }
    private void refreshClientModel() {
        if (level == null || !level.isClientSide) return;
        requestModelDataUpdate();
        // Tint comes from BE data, not BlockState. A packet with the same state
        // must still invalidate the compiled section after the new tint arrives.
        var state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
    }
    public void changeInsulation(mio_icif_block_wire replacement) {
        if (level == null || level.isClientSide || replacement == null) return;
        var saved = saveWithoutMetadata(level.registryAccess());
        if (replacement.getInsulationLayers() == 0) saved.remove("CableColor");
        var state = replacement.defaultBlockState();
        for (var property : getBlockState().getProperties()) state = copyProperty(state, getBlockState(), property);
        level.setBlockAndUpdate(worldPosition, state);
        if (level.getBlockEntity(worldPosition) instanceof mio_icif_wire wire) {
            wire.loadAdditional(saved, level.registryAccess()); wire.setChanged();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }
    private static <T extends Comparable<T>> BlockState copyProperty(BlockState target, BlockState source,
            net.minecraft.world.level.block.state.properties.Property<T> property) {
        return target.hasProperty(property) ? target.setValue(property, source.getValue(property)) : target;
    }

    private static final Direction[] ALL_DIRECTIONS = Direction.values();

    // ============ 字段 ============

    private static final int FE_COMPAT_SCAN_INTERVAL = 20;

    private int ticksUntilFECompatScan = 0;

    // Waterlogged wires can receive one entityInside callback per entity. Keep
    // the bounded flood-fill and entity sweep once per wire/tick instead of
    // repeating both operations for every callback.
    private long waterConnectivityCacheTick;
    private Set<BlockPos> waterConnectivityCache;
    private long waterDamageTick = Long.MIN_VALUE;

    private final EnergyBridge energyBridge = new EnergyBridge();
    private final EnumSet<Direction> blockedDirections = EnumSet.noneOf(Direction.class);

    private static final String TAG_DISGUISED_BLOCK = "DisguisedBlockId";
    private ResourceLocation disguisedBlockId = null;

    public ResourceLocation getDisguisedBlockId() {
        return disguisedBlockId;
    }

    public void setDisguisedBlockId(ResourceLocation blockId) {
        this.disguisedBlockId = blockId;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void clearDisguise() {
        this.disguisedBlockId = null;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean hasDisguise() {
        return disguisedBlockId != null;
    }

    public Block getDisguisedBlock() {
        if (disguisedBlockId == null) {
            return null;
        }
        Block block = BuiltInRegistries.BLOCK.get(disguisedBlockId);
        if (block == Blocks.AIR) {
            return null;
        }
        return block;
    }

    /** 相邻FE机器的电网代理，key为电线指向邻居的方向 */
    private final Map<Direction, FECompatTile> feCompatTiles = new HashMap<>();
    /** 相邻AE2能源接收器的电网代理，key为电线指向邻居的方向 */
    private final Map<Direction, Ae2EnergySink> ae2Sinks = new HashMap<>();

    // ============ 类型注册表 ============

    /** 普通电线注册表：CableTier → BlockEntityType */
    private static final Map<CableTier, BlockEntityType<?>> TYPE_REGISTRY = new HashMap<>();
    /** 绝缘电线注册表（与普通电线分开，避免键冲突） */
    private static final Map<CableTier, BlockEntityType<?>> ISOLATION_TYPE_REGISTRY = new HashMap<>();

    // ============ 注册方法 ============

    /**
     * 注册电线方块实体类型（在 BlockEntityType 构建时由注册代码调用）
     */
    public static void registerWireType(CableTier tier, BlockEntityType<?> type) {
        TYPE_REGISTRY.put(tier, type);
    }

    /**
     * 注册绝缘电线方块实体类型（在 BlockEntityType 构建时由注册代码调用）
     */
    public static void registerIsolationWireType(CableTier tier, BlockEntityType<?> type) {
        ISOLATION_TYPE_REGISTRY.put(tier, type);
    }

    /** 内部查找 type：优先查普通表，再查绝缘表 */
    protected static BlockEntityType<?> lookupWireType(CableTier tier) {
        BlockEntityType<?> type = TYPE_REGISTRY.get(tier);
        if (type != null) return type;
        return ISOLATION_TYPE_REGISTRY.get(tier);
    }

    /** 绝缘电线查找 type：优先查绝缘表，再查普通表 */
    protected static BlockEntityType<?> lookupIsolationWireType(CableTier tier) {
        BlockEntityType<?> type = ISOLATION_TYPE_REGISTRY.get(tier);
        if (type != null) return type;
        return TYPE_REGISTRY.get(tier);
    }

    // ============ 构造函数 ============

    /**
     * 便捷构造器：创建普通（非绝缘）电线，使用默认电气参数
     */
    public mio_icif_wire(BlockPos pos, BlockState state, CableTier cableTier) {
        this(pos, state, lookupWireType(cableTier), cableTier, false);
    }

    /**
     * 带明确 type 的内部构造器（供子类及方块类使用）
     */
    public mio_icif_wire(BlockPos pos, BlockState state, BlockEntityType<?> type, CableTier cableTier) {
        this(pos, state, type, cableTier, false);
    }

    /**
     * 便捷构造器：根据电压等级与绝缘标记创建电线，自动查找对应的 BlockEntityType
     *
     * @param insulated 是否为绝缘电线（绝缘电线触电伤害为 0，绝缘吸收阈值更高，传导损耗更低）
     */
    public mio_icif_wire(BlockPos pos, BlockState state, CableTier cableTier, boolean insulated) {
        this(pos, state, insulated ? lookupIsolationWireType(cableTier) : lookupWireType(cableTier), cableTier, insulated);
    }

    /**
     * 标准构造器：根据电压等级与绝缘标记自动计算所有电气参数
     *
     * @param insulated 是否为绝缘电线（绝缘电线触电伤害为 0，绝缘吸收阈值更高，传导损耗更低）
     */
    protected mio_icif_wire(BlockPos pos, BlockState state, BlockEntityType<?> type, CableTier cableTier, boolean insulated) {
        this(pos, state, type, cableTier, insulated,
             -1, -1, -1, -1, -1);
    }

    /**
     * 全参数构造器：允许子类完全自定义所有电气参数
     *
     * <p>传入 {@code -1} 表示使用基于电压等级的默认值。子类可以传入任意正值来自定义。</p>
     *
     * @param pos                    方块位置
     * @param state                  方块状态
     * @param type                   方块实体类型
     * @param cableTier              电压等级
     * @param insulated              是否绝缘（绝缘电线触电伤害自动为 0）
     * @param conductorBreakdownEnergy   导体熔毁能量（-1 = 从 CableTier 读取）
     * @param insulationBreakdownEnergy  绝缘层熔毁能量（-1 = 从 CableTier 读取）
     * @param insulationEnergyAbsorption 绝缘吸收阈值（-1 = 从 CableTier 读取）
     * @param conductionLoss         传导损耗（-1 = 从 CableTier 读取）
     * @param electricDamage         触电伤害（-1 = 根据 insulated 自动决定）
     */
    protected mio_icif_wire(BlockPos pos, BlockState state, BlockEntityType<?> type,
                           CableTier cableTier, boolean insulated,
                           double conductorBreakdownEnergy,
                           double insulationBreakdownEnergy,
                           double insulationEnergyAbsorption,
                           double conductionLoss,
                           float electricDamage) {
        super(pos, state, type, cableTier, insulated,
             conductorBreakdownEnergy,
             insulationBreakdownEnergy,
             insulationEnergyAbsorption >= 0 ? insulationEnergyAbsorption
                 : com.miophas.singularity_iteration.core.api.energy.CableInsulation.absorption(cableTier,
                     state.getBlock() instanceof mio_icif_block_wire wire ? wire.getInsulationLayers() : insulated ? 1 : 0),
             conductionLoss,
             electricDamage);
    }

    // ============ 过载处理 ============

    @Override
    public void removeInsulation() {
        if (level != null && !level.isClientSide && getBlockState().getBlock() instanceof mio_icif_block_wire wire
                && wire.getBareCounterpart() != null) {
            spawnBurnoutEffects(level, worldPosition);
            changeInsulation(wire.getBareCounterpart());
        }
    }

    @Override
    public void removeConductor() {
        if (level != null && !level.isClientSide) {
            spawnBurnoutEffects(level, worldPosition);
            level.destroyBlock(worldPosition, false);
        }
    }

    // ============ IEnergyEmitter / IEnergyAcceptor ============

    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor acceptor, Direction direction) {
        return !energyStorage.scexNetworkControlled() && !blockedDirections.contains(direction)
            && com.miophas.singularity_iteration.core.api.energy.IColoredEnergyTile.connects(this, acceptor, direction);
    }

    @Override
    public boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction direction) {
        return !energyStorage.scexNetworkControlled() && !blockedDirections.contains(direction)
            && com.miophas.singularity_iteration.core.api.energy.IColoredEnergyTile.connects(this, emitter, direction);
    }

    // ============ IEnergySink override for bridge ============

    @Override
    public double getDemandedEnergy() {
        if (energyStorage.scexNetworkControlled()) return 0.0D;
        if (!energyBridge.hasAdjacentCompatSinks()) return 0.0D;
        long spaceAvailable = getEffectiveCapacity() - energyStorage.getAmount();
        if (spaceAvailable <= 0) return 0.0D;
        return spaceAvailable;
    }

    @Override
    public double injectEnergy(Direction direction, double amount, double voltage) {
        if (energyStorage.scexNetworkControlled()) return amount;
        if (!energyBridge.hasAdjacentCompatSinks()) return amount;
        long spaceAvailable = getEffectiveCapacity() - energyStorage.getAmount();
        long accepted = Math.min((long) amount, spaceAvailable);
        energyStorage.setEnergy(energyStorage.getAmount() + accepted);
        return amount - accepted;
    }

    // ============ Server Tick ============

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_wire blockEntity) {
        if (level.isClientSide) return;

        blockEntity.tickPoweredState();

        // Native committed paths own all electrical damage, including wet cables.
        if (blockEntity.energyStorage.scexNetworkControlled()) return;

        if (state.getBlock() instanceof mio_icif_block_wire) {
            blockEntity.tickElectricCheck(level, pos, state);
        }

        blockEntity.energyBridge.tick(level, pos, blockEntity.getCableTier(), blockEntity.energyStorage);

        blockEntity.ticksUntilFECompatScan--;
        if (blockEntity.ticksUntilFECompatScan <= 0) {
            blockEntity.ticksUntilFECompatScan = FE_COMPAT_SCAN_INTERVAL;
            blockEntity.updateFECompatTiles(level, pos);
        }
    }

    /** Returns null unless the topology snapshot was built in this game tick. */
    public Set<BlockPos> getWaterConnectivityCache(long gameTime) {
        return waterConnectivityCacheTick == gameTime ? waterConnectivityCache : null;
    }

    public void cacheWaterConnectivity(long gameTime, Set<BlockPos> connectedBlocks) {
        waterConnectivityCacheTick = gameTime;
        waterConnectivityCache = connectedBlocks == null || connectedBlocks.isEmpty()
            ? Set.of()
            : Set.copyOf(connectedBlocks);
    }

    /** Only the first water collision in a game tick performs the entity sweep. */
    public boolean claimWaterDamageTick(long gameTime) {
        if (waterDamageTick == gameTime) return false;
        waterDamageTick = gameTime;
        return true;
    }

    // ============ FE兼容代理管理 ============

    /**
     * 扫描相邻方块，管理FE机器的电网代理tile。
     * 每tick调用，但只在有变化时才注册/注销。
     */
    private void updateFECompatTiles(Level level, BlockPos pos) {
        for (Direction dir : ALL_DIRECTIONS) {
            if (blockedDirections.contains(dir)) {
                // Direction filters can change while a proxy is registered.
                // Drop both compatibility views before skipping the probe.
                unregisterFeCompatTile(dir);
                unregisterAe2Sink(dir);
                continue;
            }

            BlockPos neighborPos = pos.relative(dir);
            if (!level.isLoaded(neighborPos)) {
                // A proxy retains the foreign endpoint and the energy-net
                // registration.  Do not let an unloaded chunk keep either
                // object alive until the next capability refresh.
                unregisterFeCompatTile(dir);
                unregisterAe2Sink(dir);
                continue;
            }

            if (level.getBlockEntity(neighborPos) instanceof IEnergyTile) {
                // Native grid endpoints supersede both compatibility views.
                // A replacement can still expose FE, so validity alone cannot
                // distinguish it from the proxy's former foreign neighbour.
                unregisterFeCompatTile(dir);
                unregisterAe2Sink(dir);
                continue;
            }

            if (AE2Compat.isAE2Loaded() && AE2Compat.isAe2NetworkBlock(level, neighborPos)) {
                Ae2EnergySink ae2Sink = ae2Sinks.get(dir);
                if (ae2Sink == null) {
                    ae2Sink = new Ae2EnergySink(level, neighborPos);
                    ae2Sinks.put(dir, ae2Sink);
                    EnergyNetGlobal.addTile(ae2Sink, level, neighborPos);
                    ae2Sink.setRegistered(true);
                } else if (!ae2Sink.isValid()) {
                    unregisterAe2Sink(dir);
                }
                unregisterFeCompatTile(dir);
                continue;
            }

            // A neighbour may switch from AE2 to FE (or to no capability).
            // Removing only the map entry leaves the old proxy registered in
            // the energy net and makes it retain this wire indefinitely.
            unregisterAe2Sink(dir);

            boolean isFEMachine = EnergyBridge.hasCompatEnergyStorage(level, neighborPos, dir);
            if (isFEMachine) {
                FECompatTile compat = feCompatTiles.get(dir);
                if (compat == null) {
                    compat = new FECompatTile(level, neighborPos, dir);
                    feCompatTiles.put(dir, compat);
                    EnergyNetGlobal.addTile(compat, level, neighborPos);
                    compat.setRegistered(true);
                } else if (!compat.isValid()) {
                    unregisterFeCompatTile(dir);
                }
            } else {
                // The endpoint can remain loaded while its capability is
                // removed or replaced.  Remove the old proxy immediately;
                // the final sweep is only a fallback for stale registrations.
                unregisterFeCompatTile(dir);
            }
        }

        feCompatTiles.entrySet().removeIf(entry -> {
            FECompatTile compat = entry.getValue();
            if (!compat.isValid()) {
                if (compat.isRegistered()) {
                    EnergyNetGlobal.removeTile(compat);
                    compat.setRegistered(false);
                }
                return true;
            }
            return false;
        });

        ae2Sinks.entrySet().removeIf(entry -> {
            Ae2EnergySink sink = entry.getValue();
            if (!sink.isValid()) {
                if (sink.isRegistered()) {
                    EnergyNetGlobal.removeTile(sink);
                    sink.setRegistered(false);
                }
                return true;
            }
            return false;
        });
    }

    private void unregisterFeCompatTile(Direction direction) {
        FECompatTile compat = feCompatTiles.remove(direction);
        if (compat != null && compat.isRegistered()) {
            EnergyNetGlobal.removeTile(compat);
            compat.setRegistered(false);
        }
    }

    private void unregisterAe2Sink(Direction direction) {
        Ae2EnergySink sink = ae2Sinks.remove(direction);
        if (sink != null && sink.isRegistered()) {
            EnergyNetGlobal.removeTile(sink);
            sink.setRegistered(false);
        }
    }

    private void unregisterAllCompatTiles() {
        for (Direction direction : ALL_DIRECTIONS) {
            unregisterFeCompatTile(direction);
            unregisterAe2Sink(direction);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            unregisterAllCompatTiles();
        }
        super.setRemoved();
    }

    // ============ 方向控制 ============

    @Override
    protected boolean isWaterlogged(BlockState state) {
        return state.getValue(mio_icif_block_wire.WATERLOGGED);
    }

    public boolean isDirectionBlocked(Direction dir) {
        return blockedDirections.contains(dir);
    }

    public void blockDirection(Direction dir) {
        if (blockedDirections.add(dir)) {
            setChanged();
            com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy.conductorPortsChanged(this);
            refreshRegistration();
        }
    }

    public void unblockDirection(Direction dir) {
        if (blockedDirections.remove(dir)) {
            setChanged();
            com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy.conductorPortsChanged(this);
            refreshRegistration();
        }
    }

    // ============ NBT ============

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        var previousColor = cableColor;
        var previousDisguise = disguisedBlockId;
        super.loadAdditional(tag, registries);
        cableColor = net.minecraft.world.item.DyeColor.byId(tag.contains("CableColor") ? tag.getInt("CableColor") : 15);
        blockedDirections.clear();
        if (tag.contains("BlockedDirections", Tag.TAG_INT)) {
            int bits = tag.getInt("BlockedDirections");
            for (Direction dir : ALL_DIRECTIONS) {
                if ((bits & (1 << dir.get3DDataValue())) != 0) {
                    blockedDirections.add(dir);
                }
            }
        }
        if (tag.contains(TAG_DISGUISED_BLOCK)) {
            String idStr = tag.getString(TAG_DISGUISED_BLOCK);
            ResourceLocation id = ResourceLocation.tryParse(idStr);
            this.disguisedBlockId = id;
        } else {
            this.disguisedBlockId = null;
        }
        // Live NBT reload and ordinary saved-world construction share this path.
        com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy.conductorColorChanged(this);
        // Both ordinary BE packets and initial chunk update tags load here.
        if (previousColor != cableColor || !java.util.Objects.equals(previousDisguise, disguisedBlockId))
            refreshClientModel();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("CableColor", cableColor.getId());
        int bits = 0;
        for (Direction dir : blockedDirections) {
            bits |= (1 << dir.get3DDataValue());
        }
        tag.putInt("BlockedDirections", bits);
        if (disguisedBlockId != null) {
            tag.putString(TAG_DISGUISED_BLOCK, disguisedBlockId.toString());
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("CableColor", cableColor.getId());
        int blocked = 0;
        for (Direction direction : blockedDirections) blocked |= 1 << direction.get3DDataValue();
        tag.putInt("BlockedDirections", blocked);
        if (disguisedBlockId != null) {
            tag.putString(TAG_DISGUISED_BLOCK, disguisedBlockId.toString());
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        cableColor = net.minecraft.world.item.DyeColor.byId(tag.contains("CableColor") ? tag.getInt("CableColor") : 15);
        if (tag.contains(TAG_DISGUISED_BLOCK)) {
            String idStr = tag.getString(TAG_DISGUISED_BLOCK);
            this.disguisedBlockId = ResourceLocation.tryParse(idStr);
        } else {
            this.disguisedBlockId = null;
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
