package com.singularity_iteration.mio_icif.Blocks.entity.checker;

import com.singularity_iteration.mio_icif.Blocks.entity.*;
import com.singularity_iteration.mio_icif.energy.CustomEUEnergyStorage;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.energy.grid.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 电压检测器方块实体
 * 继承自能量方块，可以连接到电力网络
 * 检测电网中的实际电压、输入电能并在准心指向时显示
 */
@SuppressWarnings("null")
public class mio_icif_checker extends mio_icif_Energy_Block {

    // 当前检测到的实际电压
    private long detectedVoltage = 0;
    // 检测到的电压等级
    private CableTier detectedTier = CableTier.LV;
    // 当前tick输入的电力
    private long inputEnergy = 0;
    // 上一tick输入的电能（用于显示）
    private long lastInputEnergy = 0;

    // 能量存储配置 - 检测器容量小，不存储能量
    // 通过 getEnergyStorageCapability 重写来实现输入检测
    private static final long CAPACITY = 0;
    private static final long MAX_RECEIVE = 0;
    private static final long MAX_EXTRACT = 0;
    private static final CableTier TIER = CableTier.IV;

    // NBT 键名
    private static final String NBT_DETECTED_VOLTAGE = "DetectedVoltage";
    private static final String NBT_DETECTED_TIER = "DetectedTier";
    private static final String NBT_INPUT_ENERGY = "InputEnergy";

    public mio_icif_checker(BlockPos pos, BlockState state) {
        this(pos, state, null);
    }

    public mio_icif_checker(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type != null ? type : mio_icif_block_entities.CHECKER_ENTITY_TYPE.get(),
              CAPACITY, MAX_RECEIVE, MAX_EXTRACT, TIER);
    }

    /**
     * 每tick更新逻辑
     * 检测周围电网的电压和输入电力
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_checker blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        blockEntity.detectVoltage();
        blockEntity.updateInputEnergy();
    }

    /**
     * 更新输入电能显示
     * 将当前tick的输入电能保存到lastInputEnergy，并重置计数据
     */
    private void updateInputEnergy() {
        if (level == null || level.isClientSide()) return;

        // 更新上一tick的输入电力
        if (inputEnergy != lastInputEnergy) {
            lastInputEnergy = inputEnergy;
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }

        // 重置当前tick输入电能计数
        inputEnergy = 0;
    }

    /**
     * 检测周围电网的电压（实际电压）
     * 使用BFS扫描整个电网，计算所有发电机的总输出电力?
     */
    private void detectVoltage() {
        if (level == null || level.isClientSide()) return;

        // 使用BFS扫描整个电网
        long totalPower = scanNetworkBFS();

        // 根据总电压确定电压等级
        CableTier tier = getTierFromVoltage(totalPower);

        // 更新检测到的电压
        if (totalPower != this.detectedVoltage || tier != this.detectedTier) {
            this.detectedVoltage = totalPower;
            this.detectedTier = tier;
            setChanged();
            // 同步数据到客户端
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * 使用BFS（广度优先搜索）扫描整个电网
     * 从检测器开始，找到所有连接的电线和发电机
     * @return 总发电电力
     */
    private long scanNetworkBFS() {
        if (level == null) return 0;
        long total = 0;
        // Check adjacent tiles for energy sources via new grid system
        for (Direction dir : Direction.values()) {
            BlockPos adjPos = worldPosition.relative(dir);
            IEnergyTile tile = EnergyNetGlobal.getTile(level, adjPos);
            if (tile instanceof IEnergySource) {
                total += (long) ((IEnergySource) tile).getOfferedEnergy();
            }
        }
        return total;
    }

    /**
     * 根据电压获取等级
     */
    private CableTier getTierFromVoltage(long voltage) {
        if (voltage <= CableTier.LV.getPowerRating()) return CableTier.LV;
        if (voltage <= CableTier.MV.getPowerRating()) return CableTier.MV;
        if (voltage <= CableTier.HV.getPowerRating()) return CableTier.HV;
        if (voltage <= CableTier.EV.getPowerRating()) return CableTier.EV;
        return CableTier.IV;
    }

    /**
     * 获取检测到的电压（实际电压）
     */
    public long getDetectedVoltage() {
        return detectedVoltage;
    }

    /**
     * 获取检测到的电压等级
     */
    public CableTier getDetectedTier() {
        return detectedTier;
    }

    /**
     * 获取输入的电能（上一tick）
     */
    public long getInputEnergy() {
        return lastInputEnergy;
    }

    /**
     * 获取格式化的电压显示文本（返回翻译键）
     */
    public String getVoltageDisplayText() {
        if (detectedVoltage <= 0) {
            return "gui.mio_icif.checker.no_voltage";
        }
        return "gui.mio_icif.checker.voltage_display";
    }

    /**
     * 获取格式化的输入电能显示文本（返回翻译键）
     */
    public String getInputEnergyDisplayText() {
        if (lastInputEnergy <= 0) {
            return "gui.mio_icif.checker.no_input";
        }
        return "gui.mio_icif.checker.input_energy_display";
    }

    /**
     * 根据索引获取 CableTier
     */
    private CableTier getTierByIndex(int index) {
        return switch (index) {
            case 0 -> CableTier.LV;
            case 1 -> CableTier.MV;
            case 2 -> CableTier.HV;
            case 3 -> CableTier.EV;
            case 4 -> CableTier.IV;
            case 5 -> CableTier.LuV;
            case 6 -> CableTier.ZPMV;
            case 7 -> CableTier.UV;
            case 8 -> CableTier.UHV;
            case 9 -> CableTier.UEV;
            case 10 -> CableTier.UIV;
            case 11 -> CableTier.UXV;
            case 12 -> CableTier.OpV;
            case 13 -> CableTier.MAX;
            default -> CableTier.LV;
        };
    }

    // ==================== 数据同步 ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong(NBT_DETECTED_VOLTAGE, detectedVoltage);
        tag.putInt(NBT_DETECTED_TIER, detectedTier.tierIndex);
        tag.putLong(NBT_INPUT_ENERGY, lastInputEnergy);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(NBT_DETECTED_VOLTAGE)) {
            detectedVoltage = tag.getLong(NBT_DETECTED_VOLTAGE);
        }
        if (tag.contains(NBT_DETECTED_TIER)) {
            int tierIndex = tag.getInt(NBT_DETECTED_TIER);
            detectedTier = getTierByIndex(tierIndex);
        }
        if (tag.contains(NBT_INPUT_ENERGY)) {
            lastInputEnergy = tag.getLong(NBT_INPUT_ENERGY);
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putLong(NBT_DETECTED_VOLTAGE, detectedVoltage);
        tag.putInt(NBT_DETECTED_TIER, detectedTier.tierIndex);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (tag.contains(NBT_DETECTED_VOLTAGE)) {
            detectedVoltage = tag.getLong(NBT_DETECTED_VOLTAGE);
        }
        if (tag.contains(NBT_DETECTED_TIER)) {
            int tierIndex = tag.getInt(NBT_DETECTED_TIER);
            detectedTier = getTierByIndex(tierIndex);
        }
    }

    /**
     * 提供自定义能量存储
     * 用于检测输入电能，但不实际存储能量
     */
    @Override
    public CustomEUEnergyStorage getEnergyStorageCapability(@Nullable Direction side) {
        return null;
    }
}