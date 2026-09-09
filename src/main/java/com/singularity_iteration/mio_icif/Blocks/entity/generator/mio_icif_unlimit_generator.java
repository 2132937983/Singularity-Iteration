package com.singularity_iteration.mio_icif.Blocks.entity.generator;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Generator;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.EUApi;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.IEUEnergyStorage;
import com.singularity_iteration.mio_icif.integration.mi.EnergyBridge;
import com.singularity_iteration.mio_icif.integration.mi.MICompat;
import com.singularity_iteration.mio_icif.integration.gt.GTCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 无限发电机 - 创造模式发电机
 * 对齐 IC2 原版 TileEntityCreativeGenerator：
 * - getSourceTier() = 1 (LV)
 * - getOfferedEnergy() = POSITIVE_INFINITY
 * - sendMultipleEnergyPackets() = true, 10 packets/tick
 * - 可以无限输出能量，不消耗自身存储的能量
 * - 不需要燃料，始终处于发电状态
 */
@SuppressWarnings("null")
public class mio_icif_unlimit_generator extends mio_icif_Energy_Generator {

    // 能量容量
    public static final long CAPACITY = 100000L;
    // 能量生成速率 - 创造模式无限
    public static final long ENERGY_GENERATION_RATE = 128000000L;
    // 最大输出速率
    public static final long MAX_EXTRACT = 128000000L;

    /**
     * 构造函数（用于BlockEntityType.Builder）
     */
    public mio_icif_unlimit_generator(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.UNLIMIT_GENERATOR_ENTITY_TYPE.get(),
            SlotLayout.builder().extra(1).build(), ENERGY_GENERATION_RATE, CAPACITY, 0, MAX_EXTRACT, CableTier.LV);
    }

    /**
     * 每tick更新逻辑
     * 无限发电机不需要燃料，始终生成能量
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_unlimit_generator blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 保持能量存储始终满
        if (blockEntity.energyStorage.getAmount() < CAPACITY) {
            blockEntity.apiGenerateEnergy(CAPACITY - blockEntity.energyStorage.getAmount(), false);
        }

        // 给充电槽中的物品充电
        blockEntity.chargeItems();

        // 分配能量到相邻方块（不消耗自身能量）
        blockEntity.distributeEnergyUnlimited();

        // 标记方块实体已更新
        blockEntity.setChanged();
    }

    /**
     * 无限能量分配 - 不消耗自身存储的能量
     */
    protected void distributeEnergyUnlimited() {
        // 向六个方向输出能量，但始终从"无限"能量源输出，不消耗存储
        for (Direction direction : Direction.values()) {
            outputUnlimitedEnergyToDirection(direction);
        }
    }

    /**
     * 向指定方向输出无限能量
     * 模拟从无限能量源输出，不减少自身存储
     */
    private void outputUnlimitedEnergyToDirection(Direction direction) {
        BlockPos adjacentPos = worldPosition.relative(direction);

        try {
            IEUEnergyStorage adjacentStorage = level.getCapability(EUApi.SIDED, adjacentPos, direction.getOpposite());

            if (adjacentStorage == null && MICompat.isMILoaded()) {
                Object miStorage = MICompat.getMIStorage(level, adjacentPos, direction.getOpposite());
                if (miStorage != null) {
                    adjacentStorage = MICompat.wrapMIStorage(miStorage);
                }
            }

            if (adjacentStorage != null) {
                long energyToTransfer = Math.min(ENERGY_GENERATION_RATE, adjacentStorage.getCapacity() - adjacentStorage.getAmount());
                if (energyToTransfer > 0) {
                    adjacentStorage.receive(energyToTransfer, false);
                }
                return;
            }

            if (GTCompat.isGTLoaded()) {
                Object gtContainer = GTCompat.getGTEnergyContainer(level, adjacentPos, direction.getOpposite());
                if (gtContainer != null && GTCompat.inputsEnergy(gtContainer, direction.getOpposite())) {
                    long energyToTransfer = ENERGY_GENERATION_RATE;

                    long[] voltageAndAmperage = GTCompat.calculateGTVoltageAndAmperage(energyToTransfer, CableTier.IV);
                    long voltage = voltageAndAmperage[0];
                    long amperage = voltageAndAmperage[1];

                    GTCompat.acceptEnergyFromNetwork(gtContainer, direction.getOpposite(), voltage, amperage);
                    return;
                }
            }

            EnergyBridge.pushToStorage(level, adjacentPos, direction, ENERGY_GENERATION_RATE);
        } catch (Exception e) {
            // 安全降级：不崩溃，只跳过本次输出
        }
    }

    /**
     * 给充电槽中的物品充电
     * 从无限能量源充电，不消耗自身存储
     */
    @Override
    @SuppressWarnings("deprecation")
    protected void chargeItems() {
        // 检查是否有充电槽
        if (itemHandler.getSlots() <= CHARGE_SLOT) {
            return;
        }

        ItemStack chargeStack = itemHandler.getStackInSlot(CHARGE_SLOT);
        if (chargeStack.isEmpty()) {
            return;
        }

        // 检查物品是否是电池类
        if (chargeStack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Normal.mio_icif_bat battery) { // NOPMD
            // 获取电池当前能量、最大能量容量和充电速率
            long currentEnergy = battery.getEnergy(chargeStack);
            long batteryMaxEnergy = battery.getMaxEnergy();
            long batteryChargeRate = battery.getChargeRate();

            // 检查电池是否已充满
            if (currentEnergy >= batteryMaxEnergy) {
                return;
            }

            // 计算可以充电的能量（从无限源）
            long energyToCharge = Math.min(batteryChargeRate, batteryMaxEnergy - currentEnergy);

            // 给电池充电（不消耗自身能量）
            if (energyToCharge > 0) {
                battery.addEnergy(chargeStack, energyToCharge);
            }
        }
    }

    /**
     * 无限发电机不需要燃料，始终返回0
     */
    @Override
    public int getFuelBurnTime(ItemStack fuel) {
        return 0;
    }

    /**
     * 始终处于"燃烧"状态（发电中）
     */
    @Override
    public boolean isBurning() {
        return true;
    }
}