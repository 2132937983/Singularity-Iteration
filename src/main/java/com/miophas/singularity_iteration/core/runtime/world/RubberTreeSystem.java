package com.miophas.singularity_iteration.core.runtime.world;

import com.miophas.singularity_iteration.core.api.block.IRubberWood;
import com.miophas.singularity_iteration.core.api.item.IElectricToolItem;
import com.miophas.singularity_iteration.core.api.item.ITreeTapItem;
import com.miophas.singularity_iteration.core.api.world.RubberHarvestResult;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 橡胶树体系核心逻辑。
 *
 * <p>集中实现树脂采集与再生的规则，供橡胶木方块与 {@code IRubberTreeAPI} 共用，
 * 与具体注册表内容（树脂物品、方块/物品类）解耦：树脂物品由调用方以 {@link Item} 传入。
 *
 * <p>所有修改世界的方法仅在服务端执行，客户端调用视为无操作。
 */
@SuppressWarnings("null")
public final class RubberTreeSystem {

    /** 橡胶木每次随机刻再生树脂的概率。 */
    public static final float RESIN_REGROW_CHANCE = 0.05F;

    private RubberTreeSystem() {
    }

    // ==================== 判定 ====================

    /**
     * 判断物品堆是否为树液采集器。
     */
    public static boolean isTreeTap(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof ITreeTapItem;
    }

    /**
     * 判断物品堆是否为电动树液采集器。
     */
    public static boolean isElectricTreeTap(ItemStack stack) {
        return stack != null && !stack.isEmpty()
            && stack.getItem() instanceof ITreeTapItem
            && stack.getItem() instanceof IElectricToolItem;
    }

    /**
     * 获取方块状态对应的橡胶木接口。
     *
     * @return 非橡胶木返回 {@code null}
     */
    public static IRubberWood rubberWoodOf(BlockState state) {
        return state != null && state.getBlock() instanceof IRubberWood wood ? wood : null;
    }

    /**
     * 判断方块状态是否为橡胶木。
     */
    public static boolean isRubberWood(BlockState state) {
        return rubberWoodOf(state) != null;
    }

    /**
     * 判断橡胶木当前是否含有树脂。
     */
    public static boolean hasResin(BlockState state) {
        IRubberWood wood = rubberWoodOf(state);
        return wood != null && wood.hasResin(state);
    }

    /**
     * 判断橡胶木是否具备采集条件。
     */
    public static boolean isTappable(BlockState state) {
        IRubberWood wood = rubberWoodOf(state);
        return wood != null && wood.isTappable(state);
    }

    /**
     * 判断橡胶木当前是否可被采集（具备采集条件且含有树脂）。
     */
    public static boolean canHarvest(BlockState state) {
        IRubberWood wood = rubberWoodOf(state);
        return wood != null && wood.isTappable(state) && wood.hasResin(state);
    }

    /**
     * 判断指定工具是否可用于采集该橡胶木。
     *
     * <p>电动工具需要能量充足。
     */
    public static boolean canUseTapOn(BlockState state, ItemStack tool) {
        if (!canHarvest(state) || !isTreeTap(tool)) {
            return false;
        }
        if (tool.getItem() instanceof IElectricToolItem electricTool) {
            return electricTool.hasEnoughEnergy(tool);
        }
        return true;
    }

    // ==================== 数量 ====================

    /**
     * 根据采集器参数掷出本次可获得的树脂数量。
     */
    public static int rollResinCount(ITreeTapItem tap, RandomSource random) {
        if (tap == null || random == null) {
            return 0;
        }
        int min = Math.max(0, tap.getResinDropMin());
        int max = Math.max(min, tap.getResinDropMax());
        return max == min ? min : min + random.nextInt(max - min + 1);
    }

    // ==================== 采集 ====================

    /**
     * 执行一次树脂采集。
     *
     * <p>消耗采集器耐久或电能、掉落树脂并将方块置为不含树脂状态。
     *
     * @param resinItem 树脂物品
     * @return 采集结果
     */
    public static RubberHarvestResult harvest(Level level, BlockPos pos, BlockState state,
                                              Player player, InteractionHand hand,
                                              ItemStack tool, Item resinItem) {
        if (level == null || level.isClientSide || player == null || pos == null || resinItem == null) {
            return RubberHarvestResult.NONE;
        }
        IRubberWood wood = rubberWoodOf(state);
        if (wood == null || !wood.isTappable(state) || !wood.hasResin(state)) {
            return RubberHarvestResult.NONE;
        }
        if (tool == null || !(tool.getItem() instanceof ITreeTapItem tap)) {
            return RubberHarvestResult.NONE;
        }

        boolean electric = false;
        if (tool.getItem() instanceof IElectricToolItem electricTool) {
            if (!electricTool.hasEnoughEnergy(tool)) {
                return RubberHarvestResult.NONE;
            }
            if (!player.isCreative()) {
                electricTool.extractEnergy(tool, com.miophas.singularity_iteration.core.api.item.EnergySaving.apply(tool, electricTool.getEnergyPerUse()));
            }
            electric = true;
        } else {
            int cost = tap.getDurabilityCost();
            if (cost > 0) {
                EquipmentSlot slot = hand == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
                tool.hurtAndBreak(cost, player, slot);
            }
        }

        int count = rollResinCount(tap, level.getRandom());
        dropResin(level, pos, player, resinItem, count);
        level.setBlock(pos, wood.withResin(state, false), 3);

        if (electric && hand != null) {
            // 同步能量变化到玩家手中
            player.setItemInHand(hand, tool);
        }
        return RubberHarvestResult.success(count);
    }

    /**
     * 在方块与玩家之间掉落指定数量的树脂。
     */
    public static void dropResin(Level level, BlockPos pos, Player player, Item resinItem, int count) {
        if (level == null || pos == null || player == null || resinItem == null || count <= 0) {
            return;
        }
        ItemStack resinStack = new ItemStack(resinItem, count);

        double midX = (pos.getX() + 0.5 + player.getX()) / 2.0;
        double midZ = (pos.getZ() + 0.5 + player.getZ()) / 2.0;
        double midY = (pos.getY() + 0.5 + player.getY()) / 2.0 + 1.0;

        ItemEntity itemEntity = new ItemEntity(level, midX, midY, midZ, resinStack);
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);
    }

    // ==================== 再生 ====================

    /**
     * 判断橡胶木当前是否可以自然再生树脂。
     */
    public static boolean canRegrowResin(BlockState state) {
        IRubberWood wood = rubberWoodOf(state);
        return wood != null && wood.canRegrowResin(state);
    }

    /**
     * 尝试让橡胶木再生树脂（随机刻逻辑）。
     *
     * @return 本次是否再生了树脂
     */
    public static boolean tryRegrowResin(Level level, BlockPos pos, BlockState state, RandomSource random) {
        if (level == null || level.isClientSide || pos == null || random == null) {
            return false;
        }
        IRubberWood wood = rubberWoodOf(state);
        if (wood == null || !wood.canRegrowResin(state)) {
            return false;
        }
        if (random.nextFloat() >= RESIN_REGROW_CHANCE) {
            return false;
        }
        level.setBlock(pos, wood.withResin(state, true), 3);
        return true;
    }
}
