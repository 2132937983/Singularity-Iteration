// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.grid;

import com.miophas.singularity_iteration.core.api.energy.grid.IEnergyEmitter;
import com.miophas.singularity_iteration.core.api.energy.grid.IEnergySink;
import com.miophas.singularity_iteration.core.api.energy.grid.ILocatableTile;

import com.miophas.singularity_iteration.core.api.energy.storage.EUApi;
import com.miophas.singularity_iteration.core.api.energy.storage.ILongEnergyStorage;
import com.miophas.singularity_iteration.core.platform.neoforge.energy.EnergyCompatibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** Live sided capability adapter; never retains a capability beyond a transaction. */
public final class FECompatTile implements IEnergySink, ILocatableTile, IEnergyStorage {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(FECompatTile.class);
    /** 与电线共用同一个 JVM 参数：-Dsingularity_iteration.wireConnectDebug=true */
    private static final boolean DEBUG =
        Boolean.parseBoolean(System.getProperty("singularity_iteration.wireConnectDebug", "false"));
    private static final java.util.Set<String> DEBUG_ONCE =
        java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    private final Level world;
    private final BlockPos pos;
    private final Direction input;
    private boolean registered;

    /** direction is the existing SI caller's direction from its wire to the receiver. */
    public FECompatTile(Level world, BlockPos pos, Direction direction) {
        this.world = java.util.Objects.requireNonNull(world);
        this.pos = pos.immutable();
        this.input = java.util.Objects.requireNonNull(direction).getOpposite();
    }

    /** 仅用于排查"连上了却充不进电"：打印该接触点每层能力的可用情况。 */
    private void debugLayers(String phase, int amount, boolean simulate) {
        if (!DEBUG) return;
        String key = phase + "|" + pos + "|" + input + "|" + simulate;
        if (DEBUG_ONCE.size() > 512) {
            DEBUG_ONCE.clear();
        }
        if (!DEBUG_ONCE.add(key)) return;
        try {
            var eu = world.getCapability(EUApi.SIDED, pos, input);
            var wide = world.getCapability(ILongEnergyStorage.BLOCK, pos, input);
            var fe = world.getCapability(Capabilities.EnergyStorage.BLOCK, pos, input);
            var compat = EnergyCompatibility.get().findEuStorage(world, pos, input);
            var be = world.getBlockEntity(pos);
            LOG.warn("[FECompat-DEBUG] {} {} (side={}, 请求={} FE, simulate={})"
                    + " | EUApi={} ILong={} FE={}(canReceive={}) 兼容层={}(canReceive={}) | BE={}",
                phase, pos, input, amount, simulate,
                eu != null, wide != null,
                fe != null, fe != null && fe.canReceive(),
                compat != null, compat != null && compat.canReceive(),
                be == null ? "null" : be.getClass().getName());
        } catch (RuntimeException error) {
            LOG.warn("[FECompat-DEBUG] {} {} 层诊断失败: {}", phase, pos, error.toString());
        }
    }
    public boolean loaded() {
        if (!(world instanceof ServerLevel level) || !level.getServer().isSameThread()) return false;
        return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null
            && level.shouldTickBlocksAt(net.minecraft.world.level.ChunkPos.asLong(pos));
    }
    // Call only after the public entry has checked thread/chunk availability.
    private IEnergyStorage capabilityWhenLoaded() {
        var eu = world.getCapability(EUApi.SIDED, pos, input);
        if (eu != null) return eu;
        var wide = world.getCapability(ILongEnergyStorage.BLOCK, pos, input);
        if (wide != null) return wide;
        var fe = world.getCapability(Capabilities.EnergyStorage.BLOCK, pos, input);
        if (fe != null) return fe;
        return EnergyCompatibility.get().findEuStorage(world, pos, input);
    }
    private IEnergySink ae2WhenLoaded() {
        return EnergyCompatibility.get().findNetworkSink(world, pos);
    }
    public boolean isValid() {
        if (!loaded()) return false;
        boolean valid = capabilityWhenLoaded() != null || ae2WhenLoaded() != null;
        return valid && loaded();
    }
    public boolean isRegistered() { return registered; }
    public void setRegistered(boolean value) { registered = value; }
    public Direction inputSide() { return input; }
    @Override public Level getWorld() { return world; }
    @Override public BlockPos getPos() { return pos; }
    @Override public boolean acceptsEnergyFrom(IEnergyEmitter source, Direction side) { return side == input && canReceive(); }
    @Override public double getDemandedEnergy() { return receiveEnergy(Integer.MAX_VALUE, true) / 4.0; }
    @Override public int getSinkTier() { return 14; }
    @Override public double injectEnergy(Direction side, double amount, double voltage) {
        if (side != input || !Double.isFinite(amount) || amount <= 0) return amount;
        int offer = (int) Math.min(Integer.MAX_VALUE, Math.floor(amount * 4.0));
        return amount - receiveEnergy(offer, false) / 4.0;
    }
    @Override public int receiveEnergy(int amount, boolean simulate) {
        if (amount <= 0 || !loaded()) return 0;
        var sink = ae2WhenLoaded();
        if (sink != null) {
            int offered = (int)Math.min(amount, Math.floor(Math.max(0, sink.getDemandedEnergy()) * 4.0));
            if (!loaded()) return 0;
            if (simulate || offered == 0) return offered;
            double rejected = sink.injectEnergy(input, offered / 4.0, offered / 4.0);
            if (!Double.isFinite(rejected) || rejected < 0 || rejected > offered / 4.0)
                throw new IllegalStateException("Invalid AE2 energy receipt at " + pos);
            // FE has integer granularity. Round the debit upwards, never create EU by truncation.
            return Math.min(offered, (int)Math.ceil((offered / 4.0 - rejected) * 4.0));
        }

        // 逐层尝试并回退：某些模组（如 Modern Industrialization）同时暴露
        // 本模组能力与 FE 适配层，而 FE 适配在某个面可能拒收（canReceive=false
        // 或报价为 0）。旧实现只认"第一个非 null 的能力"，于是表现为
        // "电线连上了、机器也在输入面，却永远充不进电"。某层拿不到能量时
        // 必须继续尝试后续层。逐层惰性查询，避免为常见路径付出额外开销。
        int accepted = receiveThrough(world.getCapability(EUApi.SIDED, pos, input), amount, simulate);
        if (accepted >= 0) return accepted;
        accepted = receiveThrough(world.getCapability(ILongEnergyStorage.BLOCK, pos, input), amount, simulate);
        if (accepted >= 0) return accepted;
        accepted = receiveThrough(world.getCapability(Capabilities.EnergyStorage.BLOCK, pos, input), amount, simulate);
        if (accepted >= 0) return accepted;
        accepted = receiveThrough(EnergyCompatibility.get().findEuStorage(world, pos, input), amount, simulate);
        if (accepted >= 0) return accepted;

        debugLayers("所有能量层均未接收", amount, simulate);
        return 0;
    }

    /**
     * @return 该层实际接收量；返回 -1 表示该层不存在、不可接收或本层拿不到能量，
     *         调用方应继续尝试下一层。
     */
    private int receiveThrough(IEnergyStorage target, int amount, boolean simulate) {
        // Providers and policy callbacks are foreign code; they can unload the target.
        if (target == null || !loaded() || !target.canReceive() || !loaded()) return -1;
        int accepted = target.receiveEnergy(amount, simulate);
        if (accepted < 0 || accepted > amount) throw new IllegalStateException("Invalid external FE receipt at " + pos);
        // 该层收不下（含模拟报价为 0）时继续尝试下一层。
        return accepted > 0 ? accepted : -1;
    }

    /** 任一层可接收即为可接收，避免被优先级最高但当前拒收的层掩盖。 */
    private boolean anyLayerCanReceive() {
        if (capabilityCanReceive(world.getCapability(EUApi.SIDED, pos, input))) return true;
        if (capabilityCanReceive(world.getCapability(ILongEnergyStorage.BLOCK, pos, input))) return true;
        if (capabilityCanReceive(world.getCapability(Capabilities.EnergyStorage.BLOCK, pos, input))) return true;
        return capabilityCanReceive(EnergyCompatibility.get().findEuStorage(world, pos, input));
    }

    private boolean capabilityCanReceive(IEnergyStorage target) {
        return target != null && loaded() && target.canReceive() && loaded();
    }
    @Override public int extractEnergy(int amount, boolean simulate) {
        if (amount <= 0 || !loaded()) return 0;
        var target = capabilityWhenLoaded();
        if (target == null || !loaded() || !target.canExtract() || !loaded()) return 0;
        int extracted = target.extractEnergy(amount, simulate);
        if (extracted < 0 || extracted > amount) throw new IllegalStateException("Invalid external FE extraction at " + pos);
        return extracted;
    }
    @Override public int getEnergyStored() {
        if (!loaded()) return 0;
        var target = capabilityWhenLoaded();
        return target == null || !loaded() ? 0 : target.getEnergyStored();
    }
    @Override public int getMaxEnergyStored() {
        if (!loaded()) return 0;
        var target = capabilityWhenLoaded();
        return target == null || !loaded() ? 0 : target.getMaxEnergyStored();
    }
    @Override public boolean canReceive() {
        if (!loaded()) return false;
        var sink = ae2WhenLoaded();
        if (sink != null) return sink.getDemandedEnergy() > 0 && loaded();
        if (anyLayerCanReceive()) return true;
        debugLayers("canReceive=false", 0, true);
        return false;
    }
    @Override public boolean canExtract() {
        if (!loaded()) return false;
        var target = capabilityWhenLoaded();
        return target != null && loaded() && target.canExtract();
    }
}
