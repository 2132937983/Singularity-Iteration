package com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator;

import com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.mio_icif_KineticU_Generator;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 手动动能发电机方块实
 * 继承mio_icif_KineticU_Generator
 *
 * 特点
 * - 玩家右键点击来产生动
 * - 每次点击产生一定量的动
 * - 不需要燃料，但会消耗玩家饥饿
 * - 适合早期游戏或紧急情况使
 */
@SuppressWarnings("null")
public class mio_icif_Manual_KineticU_Generator extends mio_icif_KineticU_Generator {

    // 每次点击产生的动(KU)
    private static final int KINETIC_PER_CLICK = 400;

    // 每次点击消耗的饥饿
    private static final float HUNGER_COST_PER_CLICK = 0.2f;

    // 最小饥饿值要求（6= 3个鸡腿）
    private static final int MIN_HUNGER = 6;

    // 最大转
    private static final int MAX_RPM = 5000;


    /**
     * 构造函数（用于 BlockEntityType.Builder
     * 鎵嬪姩鍙戠數鏈烘病鏈夊姩鑳界紦瀛橈紝鐩存帴杈撳嚭
     */
    public mio_icif_Manual_KineticU_Generator(BlockPos pos, BlockState state) {
        super(mio_icif_block_entities.MANUAL_KINETIC_GENERATOR_ENTITY_TYPE.get(), pos, state, SlotLayout.builder().extra(1).build(), KINETIC_PER_CLICK, KINETIC_PER_CLICK, MAX_RPM);
    }

    /**
     * 完整构造函
     */
    public mio_icif_Manual_KineticU_Generator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, SlotLayout.builder().extra(1).build(), KINETIC_PER_CLICK, KINETIC_PER_CLICK, MAX_RPM);
    }

    /**
     * tick 更新逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_Manual_KineticU_Generator blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 调用父类tick 方法（处理摩擦损失和动能传输
        mio_icif_KineticU_Generator.tick(level, pos, state, blockEntity);
    }

    /**
     * 检查玩家是否有足够的饥饿
     */
    public boolean hasEnoughHunger(Player player) {
        return player.getFoodData().getFoodLevel() >= MIN_HUNGER;
    }

    /**
     * 处理玩家点击
 * 手动发电机直接输出动能，不需要
     * @return 是否成功产生动能
     */
    public boolean onPlayerClick(Player player) {
        if (level == null || level.isClientSide()) {
            return false;
        }

        // 检查玩家饥饿
        if (!hasEnoughHunger(player)) {
            return false;
        }

        // 手摇发电机使用固定的高转速，确保能向任何机器传输动能
        int myRPM = MAX_RPM;
        boolean anyTransferred = false;

        // 向所有六个方向输出动能（每个方向独立发送）
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = worldPosition.relative(direction);
            IMioIcifCapabilities.IKineticStorage adjacentKinetic = level.getCapability(
                IMioIcifCapabilities.KINETIC_STORAGE_BLOCK, adjacentPos, direction.getOpposite());
            if (adjacentKinetic == null) {
                BlockEntity be = level.getBlockEntity(adjacentPos);
                adjacentKinetic = MioIcifAPI.instance().getCapabilities().adaptKineticStorage(be);
            }

            if (adjacentKinetic != null && adjacentKinetic.canReceiveKinetic()) {
                int adjacentRPM = adjacentKinetic.getRPM();
                // 只要有转速差就可以传输（手摇发电机转速固定为 MAX_RPM
                if (myRPM > adjacentRPM) {
                    long maxReceive = adjacentKinetic.getMaxKineticStored() - adjacentKinetic.getKineticStored();
                    long kineticToTransfer = Math.min(KINETIC_PER_CLICK, maxReceive);
                    if (kineticToTransfer > 0) {
                        long received = adjacentKinetic.receiveKinetic(kineticToTransfer, false);
                        if (received > 0) {
                            anyTransferred = true;
                        }
                    }
                }
            }
        }

        // 如果成功传输动能，消耗玩家饥饿
        if (anyTransferred) {
            player.getFoodData().addExhaustion(HUNGER_COST_PER_CLICK);
        }

        // 标记方块实体已更
        setChanged();

        // 只要有动能输出成功，就返true
        return anyTransferred;
    }

    @Override
    public int getFuelBurnTime(ItemStack fuel) {
        // 手动发电机不需要燃
        return 0;
    }

    @Override public int getKineticOutput() { return 0; }
    @Override public boolean isGenerating() { return false; }
    @Override public int getBurnTime() { return 0; }
    @Override public int getBurnDuration() { return 0; }
    @Override public int getKineticGenerationRate() { return KINETIC_PER_CLICK; }
    @Override public int getRotorRPM() { return kineticStorage != null ? kineticStorage.getRPM() : 0; }

    @Override
    public boolean isBurning() {
        // 手动发电机没有燃烧状
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.manual_kinetic_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        // 手动发电机不需GUI，直接返null
        return null;
    }
}