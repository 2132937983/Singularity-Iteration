package com.singularity_iteration.mio_icif.Blocks.entity.generator;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Block;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.EUApi;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.IEUEnergyStorage;
import com.singularity_iteration.mio_icif.api.machine.IGeneratorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 量子发电机 (Quantum Generator)
 *
 * 与太阳能发电机不同，量子发电机不需要阳光，可以全天候发电
 * 可以调节发电功率和电压等级
 *
 * 默认配置 (参考ASP):
 * - 发电功率: 可配置，默认 512 EU/t
 * - 电压等级: 可配置，默认 HV (3)
 */
@SuppressWarnings("null")
public class mio_icif_QuantumGenerator extends mio_icif_Energy_Block implements IGeneratorBlock {

    // 默认配置 (参考ASP)
    public static final int DEFAULT_PRODUCTION = 512;
    public static final int DEFAULT_TIER = 3; // HV
    public static final long DEFAULT_CAPACITY = 100000000L; // 1亿 EU

    // 当前配置
    private int production;
    private int tier;
    private boolean active = false;

    public mio_icif_QuantumGenerator(BlockPos pos, BlockState state) {
        this(pos, state, null);
    }

    public mio_icif_QuantumGenerator(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, 
              type != null ? type : mio_icif_block_entities.QUANTUM_GENERATOR_ENTITY_TYPE.get(),
              DEFAULT_CAPACITY, 0, getCableTierFromIndexStatic(DEFAULT_TIER - 1).powerRating,
              getCableTierFromIndexStatic(DEFAULT_TIER - 1));
        this.production = DEFAULT_PRODUCTION;
        this.tier = DEFAULT_TIER;
        setAsPowerSource(production);
    }

    /**
     * 静态方法：根据索引获取 CableTier
     */
    private static CableTier getCableTierFromIndexStatic(int index) {
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

    /**
     * 根据索引获取 CableTier
     */
    private CableTier getCableTierFromIndex(int index) {
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

    /**
     * 每 tick 更新逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_QuantumGenerator blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 如果激活且未满，则发电
        if (blockEntity.active) {
            long energyToGenerate = Math.min(blockEntity.production,
                blockEntity.energyStorage.getCapacity() - blockEntity.energyStorage.getAmount());
            if (energyToGenerate > 0) {
                blockEntity.apiGenerateEnergy(energyToGenerate, false);
            }
        }

        // 分配能量到相邻方块
        blockEntity.distributeEnergy();

        blockEntity.setChanged();
    }

    /**
     * 向相邻方块实体分配能量
     * 发电机从六面输出能量
     */
    protected void distributeEnergy() {
        if (energyStorage.getAmount() <= 0) {
            return;
        }
        
        // 向六个方向输出能量
        for (Direction direction : Direction.values()) {
            outputEnergyToDirection(direction);
        }
    }

    /**
     * 向指定方向输出能量
     */
    private void outputEnergyToDirection(Direction direction) {
        BlockPos adjacentPos = worldPosition.relative(direction);

        try {
            IEUEnergyStorage adjacentStorage = level.getCapability(EUApi.SIDED, adjacentPos, direction.getOpposite());
            if (adjacentStorage != null) {
                long energyToTransfer = Math.min(energyStorage.getAmount(), energyStorage.getMaxExtract());
                if (energyToTransfer > 0) {
                    long energyReceived = adjacentStorage.receive(energyToTransfer, false);
                    if (energyReceived > 0) {
                        energyStorage.extract(energyReceived, false);
                        setChanged();
                    }
                }
            }
        } catch (Exception e) {
            // 忽略能量传输异常
        }
    }

    /**
     * 设置发电功率
     */
    public void setProduction(int production) {
        this.production = Math.max(0, production);
        setChanged();
    }

    /**
     * 获取发电功率
     */
    public int getProduction() {
        return production;
    }

    /**
     * 设置电压等级
     */
    public void setTier(int tier) {
        this.tier = Math.max(1, Math.min(14, tier));
        CableTier cableTier = getCableTierFromIndex(this.tier - 1);
        this.energyStorage.setMaxExtract(cableTier.powerRating);
        setChanged();
    }

    /**
     * 获取电压等级
     */
    public int getTier() {
        return tier;
    }

    /**
     * 设置激活状态
     */
    public void setActive(boolean active) {
        this.active = active;
        setChanged();
    }

    /**
     * 是否激活
     */
    public boolean isActive() {
        return active;
    }

    // ==================== NBT 数据保存 ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Production", production);
        tag.putInt("Tier", tier);
        tag.putBoolean("Active", active);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        production = tag.getInt("Production");
        tier = tag.getInt("Tier");
        active = tag.getBoolean("Active");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("Production", production);
        tag.putInt("Tier", tier);
        tag.putBoolean("Active", active);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        production = tag.getInt("Production");
        tier = tag.getInt("Tier");
        active = tag.getBoolean("Active");
    }

    // ==================== MenuProvider 接口实现 ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.quantum_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Generator.QuantumGeneratorMenu(containerId, playerInventory, this);
    }

    // ==================== IGeneratorBlock ?𦻖?藁摰䂿緵 ====================

    @Override
    public boolean isBurning() {
        return active;
    }

    @Override
    public int getBurnTime() {
        return 0;
    }

    @Override
    public int getMaxBurnTime() {
        return 0;
    }

    @Override
    public ItemStack getFuelSlotItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getChargeSlotItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public net.neoforged.neoforge.items.IItemHandler getItemHandler() {
        return null;
    }

    @Override
    public boolean isGenerating() {
        return active;
    }

    @Override
    public long getGenerationRate() {
        return production;
    }

    @Override
    public CableTier getCableTier() {
        return getCableTierFromIndex(this.tier - 1);
    }

    @Override
    public long getPowerOutput() {
        return production;
    }

    // ==================== IBurnControl ?𦻖?藁摰䂿緵 ====================

    @Override
    public int getDefaultBurnTime() {
        return 0;
    }

    @Override
    public void setBurnTime(int ticks) {
        // Not applicable for quantum generator
    }
}