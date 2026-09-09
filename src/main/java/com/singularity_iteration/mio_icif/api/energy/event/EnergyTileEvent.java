package com.singularity_iteration.mio_icif.api.energy.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * 能量方块事件的基类。
 * <p>
 * 当能量方块需要被注册到或从电网中注销时，应通过 NeoForge 事件总线发布此事件。
 * 这替代了 IC2 1.12.2 中的 {@code EnergyTileLoadEvent} 和 {@code EnergyTileUnloadEvent}。
 * <p>
 * 使用方式：在方块实体的 {@code onLoad()} 中发布 {@link EnergyTileRegisterEvent}，
 * 在 {@code onChunkUnload()} 或 {@code setRemoved()} 中发布 {@link EnergyTileUnregisterEvent}。
 * <p>
 * <b>关于事件总线选择：</b>虽然此事件继承自 {@link LevelEvent}，
 * 但 NeoForge 1.21 的全局事件总线（{@code NeoForge.EVENT_BUS}）会正确处理
 * {@code LevelEvent} 的分发。监听器（如 {@code GridEventHandler}）通过
 * {@code event.getLevel() instanceof ServerLevel} 过滤非服务端维度，
 * 因此使用全局总线是安全且正确的做法。
 */
public abstract class EnergyTileEvent extends LevelEvent {
    private final BlockPos pos;
    private final IEnergyTileMarker tile;

    public EnergyTileEvent(Level level, BlockPos pos, IEnergyTileMarker tile) {
        super(level);
        this.pos = pos;
        this.tile = tile;
    }

    /**
     * 获取能量方块的位置。
     */
    public BlockPos getPos() {
        return pos;
    }

    /**
     * 获取能量方块标记接口。
     * 实现此接口的方块实体将被电网系统识别为能量方块。
     */
    public IEnergyTileMarker getTile() {
        return tile;
    }

    /**
     * 能量方块标记接口。
     * 任何需要参与电网的能量方块实体都应实现此接口。
     * 这是一个轻量级标记接口，不包含任何方法。
     */
    public interface IEnergyTileMarker {
    }

    /**
     * 能量方块注册事件。
     * <p>
     * 在方块实体加载完成时发布，通知电网系统将此方块添加为能量节点。
     */
    public static class EnergyTileRegisterEvent extends EnergyTileEvent {
        public EnergyTileRegisterEvent(Level level, BlockPos pos, IEnergyTileMarker tile) {
            super(level, pos, tile);
        }

        /**
         * 发布注册事件。便捷方法，等价于 {@code NeoForge.EVENT_BUS.post(event)}。
         */
        public static void post(Level level, BlockPos pos, IEnergyTileMarker tile) {
            NeoForge.EVENT_BUS.post(new EnergyTileRegisterEvent(level, pos, tile));
        }
    }

    /**
     * 能量方块注销事件。
     * <p>
     * 在方块实体卸载前发布，通知电网系统将此方块从能量节点中移除。
     */
    public static class EnergyTileUnregisterEvent extends EnergyTileEvent {
        public EnergyTileUnregisterEvent(Level level, BlockPos pos, IEnergyTileMarker tile) {
            super(level, pos, tile);
        }

        /**
         * 发布注销事件。便捷方法，等价于 {@code NeoForge.EVENT_BUS.post(event)}。
         */
        public static void post(Level level, BlockPos pos, IEnergyTileMarker tile) {
            NeoForge.EVENT_BUS.post(new EnergyTileUnregisterEvent(level, pos, tile));
        }
    }
}